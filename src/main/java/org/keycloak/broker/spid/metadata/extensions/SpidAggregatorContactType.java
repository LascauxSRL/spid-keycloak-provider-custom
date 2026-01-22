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

package org.keycloak.broker.spid.metadata.extensions;

import org.keycloak.broker.spid.SpidIdentityProviderConfig;
import org.keycloak.broker.spid.metadata.SpidClientConfig;
import org.keycloak.dom.saml.v2.metadata.ContactType;
import org.keycloak.dom.saml.v2.metadata.ContactTypeType;
import org.keycloak.dom.saml.v2.metadata.ExtensionsType;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.StringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Optional;

/**
 * ContactPerson for SPID aggregator entity.
 * This represents the organization that provides aggregation services.
 * For client-specific metadata, uses data from client configuration.
 */
public class SpidAggregatorContactType extends ContactType {

    public static final String XMLNS_NS = "http://www.w3.org/2000/xmlns/";
    public static final String SPID_METADATA_EXTENSIONS_NS = "https://spid.gov.it/saml-extensions";

    protected Document doc;

    /**
     * Build aggregator ContactPerson from client config.
     * Used for client-specific metadata where aggregator data comes from client configuration.
     * @param clientConfig Client config (for aggregator organization data)
     * @param isAggregatedPrivate Whether the aggregated entity is private (determines qualifier)
     * @return Optional ContactPerson
     */
    public static Optional<SpidAggregatorContactType> build(final SpidClientConfig clientConfig, boolean isAggregatedPrivate) throws ConfigurationException {
        if (StringUtil.isNullOrEmpty(clientConfig.getOtherContactCompany()) &&
            StringUtil.isNullOrEmpty(clientConfig.getOtherContactEmail()) &&
            StringUtil.isNullOrEmpty(clientConfig.getOtherContactPhone())) {
            return Optional.empty();
        }
        return Optional.of(new SpidAggregatorContactType(clientConfig, isAggregatedPrivate));
    }
    
    /**
     * Build aggregator ContactPerson from identity provider config.
     * Used for backward compatibility with non-client-specific metadata.
     * @param config Identity provider config (for aggregator organization data)
     * @param isAggregatedPrivate Whether the aggregated entity is private (determines qualifier)
     * @return Optional ContactPerson
     */
    public static Optional<SpidAggregatorContactType> build(final SpidIdentityProviderConfig config, boolean isAggregatedPrivate) throws ConfigurationException {
        if (StringUtil.isNullOrEmpty(config.getOtherContactCompany()) &&
            StringUtil.isNullOrEmpty(config.getOtherContactEmail()) &&
            StringUtil.isNullOrEmpty(config.getOtherContactPhone())) {
            return Optional.empty();
        }
        return Optional.of(new SpidAggregatorContactType(config, isAggregatedPrivate));
    }
    
    /**
     * Build aggregator ContactPerson (legacy method for backward compatibility).
     * Assumes aggregated entity is public by default.
     */
    public static Optional<SpidAggregatorContactType> build(final SpidIdentityProviderConfig config) throws ConfigurationException {
        return build(config, false);
    }

    /**
     * Constructor from client config (for client-specific metadata).
     */
    protected SpidAggregatorContactType(final SpidClientConfig clientConfig, boolean isAggregatedPrivate) throws ConfigurationException {
        super(ContactTypeType.OTHER);
        
        // Set company, email, phone from client config
        if (!StringUtil.isNullOrEmpty(clientConfig.getOtherContactCompany())) {
            this.setCompany(clientConfig.getOtherContactCompany());
        }
        if (!StringUtil.isNullOrEmpty(clientConfig.getOtherContactEmail())) {
            this.addEmailAddress(clientConfig.getOtherContactEmail());
        }
        if (!StringUtil.isNullOrEmpty(clientConfig.getOtherContactPhone())) {
            this.addTelephone(clientConfig.getOtherContactPhone());
        }
        
        this.setExtensions(new ExtensionsType());
        doc = DocumentUtil.createDocument();
        
        // Add qualifier based on whether aggregated entity is private or public
        if (isAggregatedPrivate) {
            addQualifier("spid:PrivateServicesFullAggregator");
        } else {
            addQualifier("spid:PublicServicesFullAggregator");
        }
        
        // Add IPACode if available (for public SP)
        if (!clientConfig.isSpPrivate() && !StringUtil.isNullOrEmpty(clientConfig.getIpaCode())) {
            addExtensionElement("spid:IPACode", clientConfig.getIpaCode());
        }
        
        // Add VATNumber if available (for private SP)
        if (clientConfig.isSpPrivate() && !StringUtil.isNullOrEmpty(clientConfig.getVatNumber())) {
            addExtensionElement("spid:VATNumber", clientConfig.getVatNumber());
        }
        
        // Note: spid:entityType attribute will be added to ContactPerson element during XML serialization
    }

    /**
     * Constructor from identity provider config (for backward compatibility).
     */
    protected SpidAggregatorContactType(final SpidIdentityProviderConfig config, boolean isAggregatedPrivate) throws ConfigurationException {
        super(ContactTypeType.OTHER);
        
        // Set company, email, phone from organization config
        if (!StringUtil.isNullOrEmpty(config.getOtherContactCompany())) {
            this.setCompany(config.getOtherContactCompany());
        }
        if (!StringUtil.isNullOrEmpty(config.getOtherContactEmail())) {
            this.addEmailAddress(config.getOtherContactEmail());
        }
        if (!StringUtil.isNullOrEmpty(config.getOtherContactPhone())) {
            this.addTelephone(config.getOtherContactPhone());
        }
        
        this.setExtensions(new ExtensionsType());
        doc = DocumentUtil.createDocument();
        
        // Add qualifier based on whether aggregated entity is private or public
        if (isAggregatedPrivate) {
            addQualifier("spid:PrivateServicesFullAggregator");
        } else {
            addQualifier("spid:PublicServicesFullAggregator");
        }
        
        // Add IPACode if available (for public SP)
        if (!config.isSpPrivate() && !StringUtil.isNullOrEmpty(config.getIpaCode())) {
            addExtensionElement("spid:IPACode", config.getIpaCode());
        }
        
        // Add VATNumber if available (for private SP)
        if (config.isSpPrivate() && !StringUtil.isNullOrEmpty(config.getVatNumber())) {
            addExtensionElement("spid:VATNumber", config.getVatNumber());
        }
        
        // Note: spid:entityType attribute will be added to ContactPerson element during XML serialization
    }

    protected void addQualifier(String qualifier) {
        Element spTypeElement = doc.createElementNS(SPID_METADATA_EXTENSIONS_NS, qualifier);
        spTypeElement.setAttributeNS(XMLNS_NS, "xmlns:spid", SPID_METADATA_EXTENSIONS_NS);
        getExtensions().addExtension(spTypeElement);
    }

    protected void addExtensionElement(String name, String value) {
        if (!StringUtil.isNullOrEmpty(value)) {
            Element element = doc.createElementNS(SPID_METADATA_EXTENSIONS_NS, name);
            element.setAttributeNS(XMLNS_NS, "xmlns:spid", SPID_METADATA_EXTENSIONS_NS);
            element.setTextContent(value);
            getExtensions().addExtension(element);
        }
    }
}
