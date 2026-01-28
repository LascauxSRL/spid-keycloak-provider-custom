/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.broker.spid.metadata;

import org.jboss.logging.Logger;

import org.keycloak.broker.spid.SpidIdentityProviderConfig;
import org.keycloak.broker.spid.metadata.extensions.SpidBillingContactType;
import org.keycloak.broker.spid.metadata.extensions.SpidOrganizationType;
import org.keycloak.broker.spid.metadata.extensions.SpidOtherContactType;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyUse;
import org.keycloak.dom.saml.v2.metadata.AttributeConsumingServiceType;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.RequestedAttributeType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.IndexedEndpointType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.metadata.LocalizedNameType;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.models.KeyManager;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.ClientModel;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.saml.SPMetadataDescriptor;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.processing.core.saml.v2.writers.SAMLMetadataWriter;
import org.keycloak.saml.processing.api.saml.v2.sig.SAML2Signature;
import org.keycloak.protocol.saml.SamlService;
import org.keycloak.protocol.saml.mappers.SamlMetadataDescriptorUpdater;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.util.JsonSerialization;

import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.broker.spid.SpidIdentityProvider;
import org.keycloak.broker.spid.SpidIdentityProviderFactory;

public class SpidSpMetadataResourceProvider implements RealmResourceProvider {
    protected static final Logger logger = Logger.getLogger(SpidSpMetadataResourceProvider.class);

    private KeycloakSession session;

    public SpidSpMetadataResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return this;
    }

    /**
     * Generate SPID metadata for a specific client (soggetto aggregato) - Public entities.
     * 
     * @param clientId The client ID representing the aggregated subject
     * @return SPID metadata XML for the specified client
     */
    @GET
    @Path("/pub-ag-full/clients/{client_id}")
    @Produces("text/xml; charset=utf-8")
    public Response getPublicClientMetadata(@PathParam("client_id") String clientId) {
        return getClientMetadata(clientId, false);
    }
    
    /**
     * Generate SPID metadata for a specific client (soggetto aggregato) - Private entities.
     * 
     * @param clientId The client ID representing the aggregated subject
     * @return SPID metadata XML for the specified client
     */
    @GET
    @Path("/priv-ag-full/clients/{client_id}")
    @Produces("text/xml; charset=utf-8")
    public Response getPrivateClientMetadata(@PathParam("client_id") String clientId) {
        return getClientMetadata(clientId, true);
    }
    
    /**
     * Generate SPID metadata for a specific client (soggetto aggregato).
     * 
     * @param clientId The client ID representing the aggregated subject
     * @param isPrivate Whether the aggregated entity is private
     * @return SPID metadata XML for the specified client
     */
    private Response getClientMetadata(String clientId, boolean isPrivate) {
        try {
            RealmModel realm = session.getContext().getRealm();
            ClientModel client = session.clients().getClientByClientId(realm, clientId);
            
            if (client == null || !client.isEnabled()) {
                logger.warnf("Client not found or disabled: %s", clientId);
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Client not found or disabled: " + clientId)
                    .build();
            }
            
            SpidClientConfig clientConfig = SpidClientConfig.from(client);
            
            // Verify that the client entity type matches the endpoint called
            boolean clientIsPrivate = clientConfig.isAggregatedPrivate();
            if (clientIsPrivate != isPrivate) {
                String entityType = isPrivate ? "private" : "public";
                String clientType = clientIsPrivate ? "private" : "public";
                logger.warnf("Client %s is configured as %s entity but called from %s endpoint", 
                    clientId, clientType, entityType);
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(String.format("Client is configured as %s entity but endpoint is for %s entities. " +
                        "Use /%s-ag-full/clients/%s instead.", 
                        clientType, entityType, clientType.substring(0, 4), clientId))
                    .build();
            }
            
            // Generate metadata for this specific client
            // EntityId will be generated from the metadata URL
            UriInfo uriInfo = session.getContext().getUri();
            return generateMetadataForClient(client, clientConfig, uriInfo, isPrivate);
            
        } catch (Exception e) {
            logger.warn("Failed to export SAML SP Metadata for client!", e);
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Generate aggregated SPID metadata for all SPID identity providers in the realm.
     * Maintains backward compatibility with existing implementation.
     * 
     * @return SPID metadata XML aggregating all SPID identity providers
     */
    @GET
    @Produces("text/xml; charset=utf-8")
    public Response get() {
        try
        {
            // Retrieve all enabled SPID Identity Providers for this realms
            RealmModel realm = session.getContext().getRealm();
            List<IdentityProviderModel> lstSpidIdentityProviders = realm.getIdentityProvidersStream()
                .filter(t -> t.getProviderId().equals(SpidIdentityProviderFactory.PROVIDER_ID) &&
                    t.isEnabled())
                .sorted((o1,o2)-> o1.getAlias().compareTo(o2.getAlias()))
                .collect(Collectors.toList());

            if (lstSpidIdentityProviders.size() == 0)
                throw new Exception("No SPID providers found!");

            // Create an instance of the first SPID Identity Provider in alphabetical order
            SpidIdentityProviderFactory providerFactory = new SpidIdentityProviderFactory();
            SpidIdentityProvider firstSpidProvider = providerFactory.create(session, lstSpidIdentityProviders.get(0));

            // Retrieve the context URI
            UriInfo uriInfo = session.getContext().getUri();

            //
            URI authnBinding = JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri();

            if (firstSpidProvider.getConfig().isPostBindingAuthnRequest()) {
                authnBinding = JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.getUri();
            }

            URI endpoint = uriInfo.getBaseUriBuilder()
                    .path("realms").path(realm.getName())
                    .path("broker")
                    .path(firstSpidProvider.getConfig().getAlias())
                    .path("endpoint")
                    .build();

            boolean wantAuthnRequestsSigned = firstSpidProvider.getConfig().isWantAuthnRequestsSigned();
            boolean wantAssertionsSigned = firstSpidProvider.getConfig().isWantAssertionsSigned();
            boolean wantAssertionsEncrypted = firstSpidProvider.getConfig().isWantAssertionsEncrypted();
            String configEntityId = firstSpidProvider.getConfig().getEntityId();
            String entityId = getEntityId(configEntityId, uriInfo, realm);
            String nameIDPolicyFormat = firstSpidProvider.getConfig().getNameIDPolicyFormat();
            int attributeConsumingServiceIndex = firstSpidProvider.getConfig().getAttributeConsumingServiceIndex() != null ? firstSpidProvider.getConfig().getAttributeConsumingServiceIndex(): 0;
            String attributeConsumingServiceName = firstSpidProvider.getConfig().getAttributeConsumingServiceName();
            String[] attributeConsumingServiceNames = attributeConsumingServiceName != null ? attributeConsumingServiceName.split(","): null;

            List<KeyDescriptorType> signingKeys = new LinkedList<>();
            List<KeyDescriptorType> encryptionKeys = new LinkedList<>();

            session.keys().getKeysStream(realm, KeyUse.SIG, Algorithm.RS256)
                    .filter(Objects::nonNull)
                    .filter(key -> key.getCertificate() != null)
                    .sorted(SamlService::compareKeys)
                    .forEach(key -> {
                        try {
                            Element element = SPMetadataDescriptor
                                    .buildKeyInfoElement(key.getKid(), PemUtils.encodeCertificate(key.getCertificate()));                            
                            signingKeys.add(SPMetadataDescriptor.buildKeyDescriptorType(element, KeyTypes.SIGNING, null));
                            if (key.getStatus() == KeyStatus.ACTIVE) {
                                encryptionKeys.add(SPMetadataDescriptor.buildKeyDescriptorType(element, KeyTypes.ENCRYPTION, null));
                            }
                        } catch (ParserConfigurationException e) {
                            logger.warn("Failed to export SAML SP Metadata!", e);
                            throw new RuntimeException(e);
                        }
                    });

            EntityDescriptorType entityDescriptor = SPMetadataDescriptor.buildSPDescriptor(
                authnBinding, authnBinding, endpoint, endpoint,
                wantAuthnRequestsSigned, wantAssertionsSigned, wantAssertionsEncrypted,
                entityId, nameIDPolicyFormat, signingKeys, encryptionKeys);
            
            // Create the AttributeConsumingService
            AttributeConsumingServiceType attributeConsumingService = new AttributeConsumingServiceType(attributeConsumingServiceIndex);
            attributeConsumingService.setIsDefault(true);

            if (attributeConsumingServiceNames != null && attributeConsumingServiceNames.length > 0)
            {
                for (String attributeConsumingServiceNameStr: attributeConsumingServiceNames)
                {
                    String currentLocale = realm.getDefaultLocale() == null ? "en": realm.getDefaultLocale();

                    String[] parsedName = attributeConsumingServiceNameStr.split("\\|", 2);
                    String serviceNameLocale = parsedName.length >= 2 ? parsedName[0]: currentLocale;

                    LocalizedNameType attributeConsumingServiceNameElement = new LocalizedNameType(serviceNameLocale);
                    attributeConsumingServiceNameElement.setValue(parsedName.length >= 2 ? parsedName[1]: attributeConsumingServiceNameStr);
                    attributeConsumingService.addServiceName(attributeConsumingServiceNameElement);
                }
            }
    
            // Look for the SP descriptor and add the attribute consuming service
            for (EntityDescriptorType.EDTChoiceType choiceType: entityDescriptor.getChoiceType()) {
                List<EntityDescriptorType.EDTDescriptorChoiceType> descriptors = choiceType.getDescriptors();

                if (descriptors != null) {
                    for (EntityDescriptorType.EDTDescriptorChoiceType descriptor: descriptors) {
                        if (descriptor.getSpDescriptor() != null) {
                            descriptor.getSpDescriptor().addAttributeConsumerService(attributeConsumingService);
                        }
                    }
                }
            }
            
            // Add the attribute mappers
            realm.getIdentityProviderMappersByAliasStream(firstSpidProvider.getConfig().getAlias())
                .forEach(mapper -> {
                    IdentityProviderMapper target = (IdentityProviderMapper) session.getKeycloakSessionFactory().getProviderFactory(IdentityProviderMapper.class, mapper.getIdentityProviderMapper());
                    if (target instanceof SamlMetadataDescriptorUpdater)
                    {
                        SamlMetadataDescriptorUpdater metadataAttrProvider = (SamlMetadataDescriptorUpdater)target;
                        metadataAttrProvider.updateMetadata(mapper, entityDescriptor);
                    }
                });
				

			// Additional EntityDescriptor customizations
            customizeEntityDescriptor(entityDescriptor, firstSpidProvider.getConfig());

            // Additional SPSSODescriptor customizations
            List<URI> assertionEndpoints = lstSpidIdentityProviders.stream()
                    .map(t -> uriInfo.getBaseUriBuilder()
                        .path("realms").path(realm.getName())
                        .path("broker")
                        .path(t.getAlias())
                        .path("endpoint")
                    .build()).collect(Collectors.toList());

            List<URI> logoutEndpoints = lstSpidIdentityProviders.stream()
                .map(t -> uriInfo.getBaseUriBuilder()
                    .path("realms").path(realm.getName())
                    .path("broker")
                    .path(t.getAlias())
                    .path("endpoint")
                    .build()).collect(Collectors.toList());

            for (EntityDescriptorType.EDTChoiceType choiceType: entityDescriptor.getChoiceType()) {
                List<EntityDescriptorType.EDTDescriptorChoiceType> descriptors = choiceType.getDescriptors();
    
                if (descriptors != null) {
                    for (EntityDescriptorType.EDTDescriptorChoiceType descriptor: descriptors) {
                        SPSSODescriptorType spDescriptor = descriptor.getSpDescriptor();
                        
                        if (spDescriptor != null) {
                            customizeSpDescriptor(spDescriptor,
                                authnBinding, authnBinding,
                                assertionEndpoints, logoutEndpoints);
                        }
                    }
                }
            }

            String descriptor = writeEntityDescriptorWithConsistentID(entityDescriptor);

            // Metadata signing
            if (firstSpidProvider.getConfig().isSignSpMetadata())
            {
                KeyManager.ActiveRsaKey activeKey = session.keys().getActiveRsaKey(realm);
                String keyName = firstSpidProvider.getConfig().getXmlSigKeyInfoKeyNameTransformer().getKeyName(activeKey.getKid(), activeKey.getCertificate());
                KeyPair keyPair = new KeyPair(activeKey.getPublicKey(), activeKey.getPrivateKey());

                Document metadataDocument = DocumentUtil.getDocument(descriptor);
                SAML2Signature signatureHelper = new SAML2Signature();
                signatureHelper.setSignatureMethod(firstSpidProvider.getSignatureAlgorithm().getXmlSignatureMethod());
                signatureHelper.setDigestMethod(firstSpidProvider.getSignatureAlgorithm().getXmlSignatureDigestMethod());

                Node nextSibling = metadataDocument.getDocumentElement().getFirstChild();
                signatureHelper.setNextSibling(nextSibling);

                signatureHelper.signSAMLDocument(metadataDocument, keyName, keyPair, CanonicalizationMethod.EXCLUSIVE);

                descriptor = DocumentUtil.getDocumentAsString(metadataDocument);
            }

            return Response.ok(descriptor, MediaType.APPLICATION_XML_TYPE).build();
        } catch (Exception e) {
            logger.warn("Failed to export SAML SP Metadata!", e);
            throw new RuntimeException(e);
        }
    }

    private String writeEntityDescriptorWithConsistentID(final EntityDescriptorType entityDescriptor) throws ProcessingException {
        // Update ID with hash of content so multiple metadata request give back same xml if configuration is same.
        entityDescriptor.setID("ID_"); // Set to fixed value before hashing
        String data = entityDescriptorAsString(entityDescriptor);
        String hash = md5hex(data);
        entityDescriptor.setID("ID_" + hash); // Update to hashed value ID
        return entityDescriptorAsString(entityDescriptor);
    }

    private String entityDescriptorAsString(final EntityDescriptorType entityDescriptor) throws ProcessingException {
        StringWriter sw = new StringWriter();
        XMLStreamWriter writer = StaxUtil.getXMLStreamWriter(sw);
        SAMLMetadataWriter metadataWriter = new SAMLMetadataWriter(writer);
        metadataWriter.writeEntityDescriptor(entityDescriptor);
        String xml = sw.toString();
        
        // Add spid:entityType attributes to aggregator and aggregated ContactPerson elements
        // This is done via post-processing since ContactType doesn't support custom attributes directly
        xml = addEntityTypeAttributesToContactPerson(xml);
        
        return xml;
    }
    
    /**
     * Add spid:entityType attributes to ContactPerson elements in the XML.
     * This is a post-processing step since Keycloak's ContactType doesn't support custom attributes.
     */
    private String addEntityTypeAttributesToContactPerson(String xml) {
        try {
            // Parse XML
            Document doc = DocumentUtil.getDocument(xml);
            String spidNs = "https://spid.gov.it/saml-extensions";
            
            // Find all ContactPerson elements
            NodeList contactPersons = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:metadata", "ContactPerson");
            
            for (int i = 0; i < contactPersons.getLength(); i++) {
                Element contactPerson = (Element) contactPersons.item(i);
                
                // Skip if entityType is already set
                if (contactPerson.hasAttributeNS(spidNs, "entityType")) {
                    continue;
                }
                
                // Check if this ContactPerson has PublicServicesFullAggregator or PrivateServicesFullAggregator (aggregator)
                NodeList publicServicesFullAggregator = contactPerson.getElementsByTagNameNS(spidNs, "PublicServicesFullAggregator");
                NodeList privateServicesFullAggregator = contactPerson.getElementsByTagNameNS(spidNs, "PrivateServicesFullAggregator");
                if (publicServicesFullAggregator.getLength() > 0 || privateServicesFullAggregator.getLength() > 0) {
                    // This is an aggregator ContactPerson
                    contactPerson.setAttributeNS(spidNs, "spid:entityType", "spid:aggregator");
                } else {
                    // Check if this ContactPerson is the aggregated one
                    // The aggregated ContactPerson is identified by:
                    // 1. Having Public or Private in Extensions
                    // 2. NOT having PublicServicesFullAggregator/PrivateServicesFullAggregator (already checked above)
                    // 3. Having characteristic elements: IPACode, VATNumber, or FiscalCode
                    //    OR having Company (aggregated ContactPerson always has Company)
                    NodeList publicElement = contactPerson.getElementsByTagNameNS(spidNs, "Public");
                    NodeList privateElement = contactPerson.getElementsByTagNameNS(spidNs, "Private");
                    NodeList ipaCodeElement = contactPerson.getElementsByTagNameNS(spidNs, "IPACode");
                    NodeList vatNumberElement = contactPerson.getElementsByTagNameNS(spidNs, "VATNumber");
                    NodeList fiscalCodeElement = contactPerson.getElementsByTagNameNS(spidNs, "FiscalCode");
                    
                    // Check if it has Public or Private
                    boolean hasPublicOrPrivate = publicElement.getLength() > 0 || privateElement.getLength() > 0;
                    
                    if (hasPublicOrPrivate) {
                        // This could be either:
                        // - Normal ContactPerson OTHER (from SpidOtherContactType) - typically doesn't have VATNumber/FiscalCode/IPACode
                        // - Normal ContactPerson BILLING (from SpidBillingContactType) - has contactType="billing"
                        // - Aggregated ContactPerson (from SpidAggregatedContactType) - has Public/Private AND (IPACode OR VATNumber OR FiscalCode)
                        
                        // Check contactType attribute
                        String contactType = contactPerson.getAttribute("contactType");
                        
                        // BILLING ContactPerson is never aggregated
                        if ("billing".equals(contactType)) {
                            continue;
                        }
                        
                        // Aggregated ContactPerson is identified by having:
                        // - Public or Private qualifier (already checked)
                        // - AND one of: IPACode, VATNumber, or FiscalCode
                        // These elements are characteristic of aggregated entities
                        boolean hasAggregatedIndicators = ipaCodeElement.getLength() > 0 
                            || vatNumberElement.getLength() > 0 
                            || fiscalCodeElement.getLength() > 0;
                        
                        if (hasAggregatedIndicators) {
                            // This is an aggregated ContactPerson
                            contactPerson.setAttributeNS(spidNs, "spid:entityType", "spid:aggregated");
                        }
                    }
                }
            }
            
            // Convert back to string
            return DocumentUtil.getDocumentAsString(doc);
        } catch (Exception e) {
            logger.warnf("Failed to add entityType attributes to ContactPerson: %s", e.getMessage());
            return xml; // Return original XML if processing fails
        }
    }

    private String getEntityId(String configEntityId, UriInfo uriInfo, RealmModel realm) {
        if (configEntityId == null || configEntityId.isEmpty())
            return UriBuilder.fromUri(uriInfo.getBaseUri()).path("realms").path(realm.getName()).build().toString();
        else
            return configEntityId;
    }

    private static void customizeEntityDescriptor(EntityDescriptorType entityDescriptor,
        SpidIdentityProviderConfig config)
        throws ConfigurationException
    {
        // Organization
        SpidOrganizationType.build(config).ifPresent(entityDescriptor::setOrganization);

        // ContactPerson type=OTHER
        SpidOtherContactType.build(config).ifPresent(entityDescriptor::addContactPerson);

        // ContactPerson type=BILLING
        SpidBillingContactType.build(config).ifPresent(entityDescriptor::addContactPerson);
    }
    
    /**
     * Customize EntityDescriptor for aggregated clients.
     * This method adds Organization and BILLING contact, but NOT the OTHER contact,
     * which is replaced by the aggregated ContactPerson that includes all contact data.
     * 
     * @param entityDescriptor The entity descriptor to customize
     * @param clientConfig Client config (for Organization data - aggregated entity)
     * @param idpConfig Identity provider config (for BILLING contact)
     */
    private static void customizeEntityDescriptorForAggregatedClient(EntityDescriptorType entityDescriptor,
        SpidClientConfig clientConfig, SpidIdentityProviderConfig idpConfig)
        throws ConfigurationException
    {
        // Organization: use data from client configuration (aggregated entity)
        // Convert client config to provider config format for Organization
        SpidClientConfig.SpidIdentityProviderConfigAdapter clientProviderConfig = clientConfig.toProviderConfig();
        SpidOrganizationType.build(clientProviderConfig).ifPresent(entityDescriptor::setOrganization);
        
        // Note: ContactPerson type=OTHER is NOT added here because it will be replaced
        // by the aggregated ContactPerson which includes all contact data (email, phone, etc.)
        // Note: ContactPerson type=BILLING is NOT added for client-specific metadata
    }
    
    /**
     * Add aggregator ContactPerson to entity descriptor.
     * This represents the organization that provides aggregation services.
     * Uses data from identity provider configuration (not from client).
     * @param entityDescriptor The entity descriptor to add the contact to
     * @param idpConfig Identity provider config (for aggregator organization data)
     * @param isAggregatedPrivate Whether the aggregated entity is private (determines qualifier)
     */
    private static void addAggregatorContactPerson(EntityDescriptorType entityDescriptor,
        SpidIdentityProviderConfig idpConfig, boolean isAggregatedPrivate) {
        try {
            org.keycloak.broker.spid.metadata.extensions.SpidAggregatorContactType.build(idpConfig, isAggregatedPrivate)
                .ifPresent(entityDescriptor::addContactPerson);
        } catch (ConfigurationException e) {
            logger.warnf("Failed to create aggregator ContactPerson: %s", e.getMessage());
        }
    }
    
    /**
     * Add aggregated ContactPerson to entity descriptor.
     * This represents the organization that is aggregated by the aggregator.
     */
    private static void addAggregatedContactPerson(EntityDescriptorType entityDescriptor,
        SpidClientConfig clientConfig) {
        try {
            org.keycloak.broker.spid.metadata.extensions.SpidAggregatedContactType.build(clientConfig)
                .ifPresent(entityDescriptor::addContactPerson);
        } catch (ConfigurationException e) {
            logger.warnf("Failed to create aggregated ContactPerson: %s", e.getMessage());
        }
    }

    private static void customizeSpDescriptor(SPSSODescriptorType spDescriptor,
        URI loginBinding, URI logoutBinding, 
        List<URI> assertionEndpoints, List<URI> logoutEndpoints)
    {
        // Remove any existing SingleLogoutService endpoints
        List<EndpointType> lstSingleLogoutService = spDescriptor.getSingleLogoutService();
        for (int i = lstSingleLogoutService.size() - 1; i >= 0; --i)
            spDescriptor.removeSingleLogoutService(lstSingleLogoutService.get(i));

        // Add the new SingleLogoutService endpoints
        for (URI logoutEndpoint: logoutEndpoints)
            spDescriptor.addSingleLogoutService(new EndpointType(logoutBinding, logoutEndpoint));

        // Remove any existing AssertionConsumerService endpoints
        List<IndexedEndpointType> lstAssertionConsumerService = spDescriptor.getAssertionConsumerService();
        for (int i = lstAssertionConsumerService.size() - 1; i >= 0; --i)
            spDescriptor.removeAssertionConsumerService(lstAssertionConsumerService.get(i));

        // Add the new AssertionConsumerService endpoints
        int assertionEndpointIndex = 0;
        for (URI assertionEndpoint: assertionEndpoints)
        {
            IndexedEndpointType assertionConsumerEndpoint = new IndexedEndpointType(loginBinding, assertionEndpoint);
            if (assertionEndpointIndex == 0) assertionConsumerEndpoint.setIsDefault(true);
            assertionConsumerEndpoint.setIndex(assertionEndpointIndex);

            spDescriptor.addAssertionConsumerService(assertionConsumerEndpoint);
            assertionEndpointIndex++;
        }
    }

    private static String md5hex(String data)
    {
        try {
            byte[] bytes = MessageDigest.getInstance("MD5").digest(data.getBytes(StandardCharsets.UTF_8));
            return new BigInteger(1, bytes).toString(16);
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Default SPID requested attributes list.
     * These are the standard SPID attributes that should be included in metadata by default.
     */
    private static final String[] DEFAULT_SPID_REQUESTED_ATTRIBUTES = {
        "name", "ivaCode", "countyOfBirth", "digitalAddress", "address", "companyName",
        "gender", "familyName", "registeredOffice", "email", "dateOfBirth",
        "fiscalNumber", "mobilePhone", "placeOfBirth", "spidCode"
    };

    /**
     * Create a single AttributeConsumingService using legacy configuration format.
     * 
     * @param index The index for the service
     * @param serviceNames Array of service names (format: "locale|Name")
     * @param clientConfig The SPID client configuration
     * @param realm The realm model
     * @return Created AttributeConsumingService
     */
    private static AttributeConsumingServiceType createSingleAttributeConsumingService(
            int index, String[] serviceNames, SpidClientConfig clientConfig, RealmModel realm) {
        AttributeConsumingServiceType service = new AttributeConsumingServiceType(index);
        service.setIsDefault(true);

        if (serviceNames != null && serviceNames.length > 0) {
            for (String serviceNameStr: serviceNames) {
                String currentLocale = realm.getDefaultLocale() == null ? "en": realm.getDefaultLocale();

                String[] parsedName = serviceNameStr.split("\\|", 2);
                String serviceNameLocale = parsedName.length >= 2 ? parsedName[0]: currentLocale;

                LocalizedNameType serviceNameElement = new LocalizedNameType(serviceNameLocale);
                serviceNameElement.setValue(parsedName.length >= 2 ? parsedName[1]: serviceNameStr);
                service.addServiceName(serviceNameElement);
            }
        }
        
        // Add RequestedAttributes from client configuration or use defaults
        addRequestedAttributes(service, clientConfig);
        
        return service;
    }
    
    /**
     * Create an AttributeConsumingService from JSON configuration.
     * 
     * @param config The AttributeConsumingServiceConfig from JSON
     * @param realm The realm model
     * @return Created AttributeConsumingService
     */
    private static AttributeConsumingServiceType createAttributeConsumingService(
            AttributeConsumingServiceConfig config, RealmModel realm) {
        int index = config.getIndex() != null ? config.getIndex() : 0;
        AttributeConsumingServiceType service = new AttributeConsumingServiceType(index);
        
        // Set as default only if index is 0 (first service)
        service.setIsDefault(index == 0);
        
        // Add service name
        // ServiceName should be a single value (e.g., "default,profile" is one service name, not two)
        if (config.getServiceName() != null && !config.getServiceName().trim().isEmpty()) {
            String currentLocale = realm.getDefaultLocale() == null ? "it": realm.getDefaultLocale();
            LocalizedNameType serviceNameElement = new LocalizedNameType(currentLocale);
            serviceNameElement.setValue(config.getServiceName().trim());
            service.addServiceName(serviceNameElement);
        }
        
        // Note: ServiceDescription is optional in SAML metadata but not directly supported by Keycloak's AttributeConsumingServiceType
        // The serviceDescription field from JSON config is stored but not added to metadata
        // This is acceptable as ServiceDescription is optional per SAML 2.0 specification
        
        // Add requested attributes
        if (config.getRequestedAttributes() != null && !config.getRequestedAttributes().isEmpty()) {
            for (String attrName : config.getRequestedAttributes()) {
                if (attrName != null && !attrName.trim().isEmpty()) {
                    RequestedAttributeType requestedAttribute = new RequestedAttributeType(attrName.trim());
                    requestedAttribute.setNameFormat(URI.create(JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC.get()).toString());
                    requestedAttribute.setFriendlyName(attrName.trim());
                    service.addRequestedAttribute(requestedAttribute);
                }
            }
        }
        
        return service;
    }
    
    /**
     * Add RequestedAttribute elements to AttributeConsumingService.
     * Uses client configuration if available, otherwise uses default SPID attributes.
     * 
     * @param attributeConsumingService The AttributeConsumingService to add attributes to
     * @param clientConfig The SPID client configuration
     */
    private static void addRequestedAttributes(AttributeConsumingServiceType attributeConsumingService, 
                                               SpidClientConfig clientConfig) {
        String requestedAttributesStr = clientConfig.getRequestedAttributes();
        String[] requestedAttributeNames;
        
        if (requestedAttributesStr != null && !requestedAttributesStr.trim().isEmpty()) {
            // Use configured attributes (comma-separated list)
            requestedAttributeNames = requestedAttributesStr.split(",");
            for (int i = 0; i < requestedAttributeNames.length; i++) {
                requestedAttributeNames[i] = requestedAttributeNames[i].trim();
            }
        } else {
            // Use default SPID attributes
            requestedAttributeNames = DEFAULT_SPID_REQUESTED_ATTRIBUTES;
        }
        
        // Add each requested attribute
        for (String attrName : requestedAttributeNames) {
            if (attrName != null && !attrName.isEmpty()) {
                RequestedAttributeType requestedAttribute = new RequestedAttributeType(attrName);
                requestedAttribute.setNameFormat(URI.create(JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC.get()).toString());
                requestedAttribute.setFriendlyName(attrName);
                attributeConsumingService.addRequestedAttribute(requestedAttribute);
            }
        }
    }

    /**
     * Generate SPID metadata for a specific client.
     * 
     * @param client The client representing the aggregated subject
     * @param clientConfig The SPID configuration for the client
     * @param uriInfo URI information for generating entityId from metadata URL
     * @param isPrivate Whether the aggregated entity is private
     * @return SPID metadata XML
     */
    private Response generateMetadataForClient(ClientModel client, SpidClientConfig clientConfig, 
            UriInfo uriInfo, boolean isPrivate)
            throws ProcessingException, ConfigurationException, ParserConfigurationException, ParsingException {
        RealmModel realm = session.getContext().getRealm();
        
        // Get SPID identity provider configuration (shared across all clients)
        List<IdentityProviderModel> lstSpidIdentityProviders = realm.getIdentityProvidersStream()
            .filter(t -> t.getProviderId().equals(SpidIdentityProviderFactory.PROVIDER_ID) &&
                t.isEnabled())
            .sorted((o1,o2)-> o1.getAlias().compareTo(o2.getAlias()))
            .collect(Collectors.toList());

        if (lstSpidIdentityProviders.size() == 0) {
            throw new RuntimeException("No SPID providers found!");
        }

        // Create an instance of the first SPID Identity Provider for shared config
        SpidIdentityProviderFactory providerFactory = new SpidIdentityProviderFactory();
        SpidIdentityProvider firstSpidProvider = providerFactory.create(session, lstSpidIdentityProviders.get(0));
        
        // Determine binding from identity provider config
        URI authnBinding = JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri();
        if (firstSpidProvider.getConfig().isPostBindingAuthnRequest()) {
            authnBinding = JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.getUri();
        }

        // Build endpoint URL (same as aggregated metadata, clientId is handled via SAML parameters)
        URI endpoint = uriInfo.getBaseUriBuilder()
                .path("realms").path(realm.getName())
                .path("broker")
                .path(firstSpidProvider.getConfig().getAlias())
                .path("endpoint")
                .build();

        // Get configuration from identity provider (shared settings)
        boolean wantAuthnRequestsSigned = firstSpidProvider.getConfig().isWantAuthnRequestsSigned();
        boolean wantAssertionsSigned = firstSpidProvider.getConfig().isWantAssertionsSigned();
        boolean wantAssertionsEncrypted = firstSpidProvider.getConfig().isWantAssertionsEncrypted();
        String nameIDPolicyFormat = firstSpidProvider.getConfig().getNameIDPolicyFormat();
        
        // Generate entity ID from the metadata URL
        // For client-specific metadata, entityId is the URL where the metadata is served
        String pathSegment = isPrivate ? "priv-ag-full" : "pub-ag-full";
        String entityId = UriBuilder.fromUri(uriInfo.getBaseUri())
            .path("realms").path(realm.getName())
            .path(SpidSpMetadataResourceProviderFactory.ID)
            .path(pathSegment)
            .path("clients")
            .path(client.getClientId())
            .build().toString();
        
        // Get AttributeConsumingService configuration from client
        // Default to 0 to match production metadata behavior
        int attributeConsumingServiceIndex = clientConfig.getAttributeConsumingServiceIndex() != null 
            ? clientConfig.getAttributeConsumingServiceIndex() : 0;
        String attributeConsumingServiceName = clientConfig.getAttributeConsumingServiceName();
        String[] attributeConsumingServiceNames = attributeConsumingServiceName != null 
            ? attributeConsumingServiceName.split(",") : null;

        // Get signing and encryption keys from realm
        List<KeyDescriptorType> signingKeys = new LinkedList<>();
        List<KeyDescriptorType> encryptionKeys = new LinkedList<>();

        session.keys().getKeysStream(realm, KeyUse.SIG, Algorithm.RS256)
                .filter(Objects::nonNull)
                .filter(key -> key.getCertificate() != null)
                .sorted(SamlService::compareKeys)
                .forEach(key -> {
                    try {
                        Element element = SPMetadataDescriptor
                                .buildKeyInfoElement(key.getKid(), PemUtils.encodeCertificate(key.getCertificate()));                            
                        signingKeys.add(SPMetadataDescriptor.buildKeyDescriptorType(element, KeyTypes.SIGNING, null));
                        if (key.getStatus() == KeyStatus.ACTIVE) {
                            encryptionKeys.add(SPMetadataDescriptor.buildKeyDescriptorType(element, KeyTypes.ENCRYPTION, null));
                        }
                    } catch (ParserConfigurationException e) {
                        logger.warn("Failed to export SAML SP Metadata!", e);
                        throw new RuntimeException(e);
                    }
                });

        // Build entity descriptor
        EntityDescriptorType entityDescriptor = SPMetadataDescriptor.buildSPDescriptor(
            authnBinding, authnBinding, endpoint, endpoint,
            wantAuthnRequestsSigned, wantAssertionsSigned, wantAssertionsEncrypted,
            entityId, nameIDPolicyFormat, signingKeys, encryptionKeys);
        
        // Create AttributeConsumingService(s)
        List<AttributeConsumingServiceType> attributeConsumingServices = new LinkedList<>();

        // If the client is marked as test-only, expose only a single default
        // AttributeConsumingService instead of the full set.
        if (clientConfig.isTestClient()) {
            AttributeConsumingServiceType service = createSingleAttributeConsumingService(
                    attributeConsumingServiceIndex,
                    attributeConsumingServiceNames,
                    clientConfig,
                    realm);
            attributeConsumingServices.add(service);
        } else {
            // Standard behaviour: possibly multiple services via JSON or defaults
            String attributeConsumingServicesJson = clientConfig.getAttributeConsumingServices();

            if (attributeConsumingServicesJson != null && !attributeConsumingServicesJson.trim().isEmpty()) {
                // Parse JSON array of AttributeConsumingService configurations
                try {
                    AttributeConsumingServiceConfig[] servicesConfig = JsonSerialization.readValue(
                        attributeConsumingServicesJson, 
                        AttributeConsumingServiceConfig[].class
                    );
                    
                    for (AttributeConsumingServiceConfig serviceConfig : servicesConfig) {
                        AttributeConsumingServiceType service = createAttributeConsumingService(
                            serviceConfig, realm
                        );
                        attributeConsumingServices.add(service);
                    }
                } catch (Exception e) {
                    logger.warnf("Failed to parse spid.attributeConsumingServices JSON: %s", e.getMessage());
                    // Fall back to all default services
                    List<AttributeConsumingServiceConfig> defaultServices = DefaultAttributeConsumingServices.getAllDefaultServices();
                    for (AttributeConsumingServiceConfig serviceConfig : defaultServices) {
                        AttributeConsumingServiceType service = createAttributeConsumingService(serviceConfig, realm);
                        attributeConsumingServices.add(service);
                    }
                }
            } else {
                // Use all default AttributeConsumingServices (index 0-30, 99-100)
                // This ensures all possible service combinations are available in metadata
                List<AttributeConsumingServiceConfig> defaultServices = DefaultAttributeConsumingServices.getAllDefaultServices();
                for (AttributeConsumingServiceConfig serviceConfig : defaultServices) {
                    AttributeConsumingServiceType service = createAttributeConsumingService(serviceConfig, realm);
                    attributeConsumingServices.add(service);
                }
            }
        }
    
        // Look for the SP descriptor and add all attribute consuming services
        for (EntityDescriptorType.EDTChoiceType choiceType: entityDescriptor.getChoiceType()) {
            List<EntityDescriptorType.EDTDescriptorChoiceType> descriptors = choiceType.getDescriptors();

            if (descriptors != null) {
                for (EntityDescriptorType.EDTDescriptorChoiceType descriptor: descriptors) {
                    if (descriptor.getSpDescriptor() != null) {
                        for (AttributeConsumingServiceType service : attributeConsumingServices) {
                            descriptor.getSpDescriptor().addAttributeConsumerService(service);
                        }
                    }
                }
            }
        }
        
        // Add the attribute mappers from identity provider (if any)
        realm.getIdentityProviderMappersByAliasStream(firstSpidProvider.getConfig().getAlias())
            .forEach(mapper -> {
                IdentityProviderMapper target = (IdentityProviderMapper) session.getKeycloakSessionFactory()
                    .getProviderFactory(IdentityProviderMapper.class, mapper.getIdentityProviderMapper());
                if (target instanceof SamlMetadataDescriptorUpdater) {
                    SamlMetadataDescriptorUpdater metadataAttrProvider = (SamlMetadataDescriptorUpdater)target;
                    metadataAttrProvider.updateMetadata(mapper, entityDescriptor);
                }
            });

        // Additional EntityDescriptor customizations
        // Organization must use data from client configuration (aggregated entity)
        // For aggregated clients, we only add Organization and BILLING contact
        // The OTHER contact is replaced by the aggregated ContactPerson which includes all contact data
        customizeEntityDescriptorForAggregatedClient(entityDescriptor, clientConfig, firstSpidProvider.getConfig());
        
        // Add aggregator and aggregated ContactPerson for client-specific metadata
        // Aggregator ContactPerson uses data from identity provider (not from client)
        // Aggregated ContactPerson uses data from client configuration
        // Check if aggregated entity is private to determine the correct qualifier
        boolean isAggregatedPrivate = clientConfig.isAggregatedPrivate();
        addAggregatorContactPerson(entityDescriptor, firstSpidProvider.getConfig(), isAggregatedPrivate);
        addAggregatedContactPerson(entityDescriptor, clientConfig);

        // Additional SPSSODescriptor customizations
        // Use similar logic to the original get() method, but for client-specific metadata
        // the endpoints must include the client path segment (/clients/{clientId})
        // so that responses are sent to the dedicated client endpoint.
        logger.debugf("Found %d SPID identity providers for client %s", lstSpidIdentityProviders.size(), client.getClientId());
        
        List<URI> assertionEndpoints = lstSpidIdentityProviders.stream()
                .map(t -> {
                    URI assertionEndpoint = uriInfo.getBaseUriBuilder()
                        .path("realms").path(realm.getName())
                        .path("broker")
                        .path(t.getAlias())
                        .path("endpoint")
                        .path("clients")
                        .path(client.getClientId())
                        .build();
                    logger.debugf("Adding AssertionConsumerService endpoint for IDP %s: %s", t.getAlias(), assertionEndpoint);
                    return assertionEndpoint;
                })
                .collect(Collectors.toList());

        List<URI> logoutEndpoints = lstSpidIdentityProviders.stream()
            .map(t -> {
                URI logoutEndpoint = uriInfo.getBaseUriBuilder()
                    .path("realms").path(realm.getName())
                    .path("broker")
                    .path(t.getAlias())
                    .path("endpoint")
                    .path("clients")
                    .path(client.getClientId())
                    .build();
                logger.debugf("Adding SingleLogoutService endpoint for IDP %s: %s", t.getAlias(), logoutEndpoint);
                return logoutEndpoint;
            })
            .collect(Collectors.toList());

        for (EntityDescriptorType.EDTChoiceType choiceType: entityDescriptor.getChoiceType()) {
            List<EntityDescriptorType.EDTDescriptorChoiceType> descriptors = choiceType.getDescriptors();
    
            if (descriptors != null) {
                for (EntityDescriptorType.EDTDescriptorChoiceType descriptor: descriptors) {
                    SPSSODescriptorType spDescriptor = descriptor.getSpDescriptor();
                    
                    if (spDescriptor != null) {
                        customizeSpDescriptor(spDescriptor,
                            authnBinding, authnBinding,
                            assertionEndpoints, logoutEndpoints);
                    }
                }
            }
        }

        String descriptor = writeEntityDescriptorWithConsistentID(entityDescriptor);

        // Metadata signing (use identity provider config for signing settings)
        if (firstSpidProvider.getConfig().isSignSpMetadata()) {
            KeyManager.ActiveRsaKey activeKey = session.keys().getActiveRsaKey(realm);
            String keyName = firstSpidProvider.getConfig().getXmlSigKeyInfoKeyNameTransformer()
                .getKeyName(activeKey.getKid(), activeKey.getCertificate());
            KeyPair keyPair = new KeyPair(activeKey.getPublicKey(), activeKey.getPrivateKey());

            Document metadataDocument = DocumentUtil.getDocument(descriptor);
            SAML2Signature signatureHelper = new SAML2Signature();
            signatureHelper.setSignatureMethod(firstSpidProvider.getSignatureAlgorithm().getXmlSignatureMethod());
            signatureHelper.setDigestMethod(firstSpidProvider.getSignatureAlgorithm().getXmlSignatureDigestMethod());

            Node nextSibling = metadataDocument.getDocumentElement().getFirstChild();
            signatureHelper.setNextSibling(nextSibling);

            signatureHelper.signSAMLDocument(metadataDocument, keyName, keyPair, CanonicalizationMethod.EXCLUSIVE);

            descriptor = DocumentUtil.getDocumentAsString(metadataDocument);
        }

        return Response.ok(descriptor, MediaType.APPLICATION_XML_TYPE).build();
    }

    @Override
    public void close() {
    }

    public static URI getMetadataURL(KeycloakSession session) {
        UriInfo uriInfo = session.getContext().getUri();
        return uriInfo.getBaseUriBuilder()
                    .path("realms").path(session.getContext().getRealm().getName())
                    .path(SpidSpMetadataResourceProviderFactory.ID)
                    .build();
    }
    
    public static URI getClientMetadataURL(KeycloakSession session, String clientId) {
        UriInfo uriInfo = session.getContext().getUri();
        return uriInfo.getBaseUriBuilder()
                    .path("realms").path(session.getContext().getRealm().getName())
                    .path(SpidSpMetadataResourceProviderFactory.ID)
                    .path("clients")
                    .path(clientId)
                    .build();
    }
}
