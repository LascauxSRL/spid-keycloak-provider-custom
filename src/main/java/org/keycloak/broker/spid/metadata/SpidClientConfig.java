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

import org.keycloak.models.ClientModel;

/**
 * Helper class to access SPID-specific configuration stored in client attributes.
 * This allows each client to represent a separate aggregated subject (soggetto aggregato).
 */
public class SpidClientConfig {
    
    // Attribute keys for SPID configuration
    public static final String SPID_ENTITY_ID = "spid.entityId";
    public static final String SPID_ORGANIZATION_NAMES = "spid.organizationNames";
    public static final String SPID_ORGANIZATION_DISPLAY_NAMES = "spid.organizationDisplayNames";
    public static final String SPID_ORGANIZATION_URLS = "spid.organizationUrls";
    public static final String SPID_ATTRIBUTE_CONSUMING_SERVICE_INDEX = "spid.attributeConsumingServiceIndex";
    public static final String SPID_ATTRIBUTE_CONSUMING_SERVICE_NAME = "spid.attributeConsumingServiceName";
    public static final String SPID_REQUESTED_ATTRIBUTES = "spid.requestedAttributes";
    public static final String SPID_ATTRIBUTE_CONSUMING_SERVICES = "spid.attributeConsumingServices";
    
    // Aggregated entity attributes (for aggregated ContactPerson)
    public static final String SPID_AGGREGATED_IPA_CODE = "spid.aggregated.ipaCode";
    public static final String SPID_AGGREGATED_COMPANY = "spid.aggregated.company";
    public static final String SPID_AGGREGATED_IS_PRIVATE = "spid.aggregated.isPrivate";
    public static final String SPID_AGGREGATED_VAT_NUMBER = "spid.aggregated.vatNumber";
    public static final String SPID_AGGREGATED_FISCAL_CODE = "spid.aggregated.fiscalCode";
    public static final String SPID_IS_COLLAUDO = "spid.isCollaudo";
    public static final String SPID_IS_TEST_CLIENT = "spid.isTestClient";
    
    // Contact OTHER attributes
    public static final String SPID_CONTACT_OTHER_SP_PRIVATE = "spid.contact.other.isSpPrivate";
    public static final String SPID_CONTACT_OTHER_IPA_CODE = "spid.contact.other.ipaCode";
    public static final String SPID_CONTACT_OTHER_VAT_NUMBER = "spid.contact.other.vatNumber";
    public static final String SPID_CONTACT_OTHER_FISCAL_CODE = "spid.contact.other.fiscalCode";
    public static final String SPID_CONTACT_OTHER_COMPANY = "spid.contact.other.company";
    public static final String SPID_CONTACT_OTHER_PHONE = "spid.contact.other.phone";
    public static final String SPID_CONTACT_OTHER_EMAIL = "spid.contact.other.email";
    
    // Contact BILLING attributes
    public static final String SPID_CONTACT_BILLING_COMPANY = "spid.contact.billing.company";
    public static final String SPID_CONTACT_BILLING_PHONE = "spid.contact.billing.phone";
    public static final String SPID_CONTACT_BILLING_EMAIL = "spid.contact.billing.email";
    public static final String SPID_CONTACT_BILLING_REGISTRY_NAME = "spid.contact.billing.registryName";
    public static final String SPID_CONTACT_BILLING_SITE_ADDRESS = "spid.contact.billing.siteAddress";
    public static final String SPID_CONTACT_BILLING_SITE_NUMBER = "spid.contact.billing.siteNumber";
    public static final String SPID_CONTACT_BILLING_SITE_CITY = "spid.contact.billing.siteCity";
    public static final String SPID_CONTACT_BILLING_SITE_ZIP_CODE = "spid.contact.billing.siteZipCode";
    public static final String SPID_CONTACT_BILLING_SITE_PROVINCE = "spid.contact.billing.siteProvince";
    public static final String SPID_CONTACT_BILLING_SITE_COUNTRY = "spid.contact.billing.siteCountry";
    
    private final ClientModel client;
    
    private SpidClientConfig(ClientModel client) {
        this.client = client;
    }
    
    public static SpidClientConfig from(ClientModel client) {
        return new SpidClientConfig(client);
    }
    
    public boolean isSpidConfigured() {
        // For client-specific metadata, entityId is generated from URL, so we check for organization data instead
        return (getOrganizationNames() != null && !getOrganizationNames().isEmpty()) ||
               (getOrganizationDisplayNames() != null && !getOrganizationDisplayNames().isEmpty());
    }
    
    public String getEntityId() {
        return client.getAttribute(SPID_ENTITY_ID);
    }
    
    public String getOrganizationNames() {
        return client.getAttribute(SPID_ORGANIZATION_NAMES);
    }
    
    public String getOrganizationDisplayNames() {
        return client.getAttribute(SPID_ORGANIZATION_DISPLAY_NAMES);
    }
    
    public String getOrganizationUrls() {
        return client.getAttribute(SPID_ORGANIZATION_URLS);
    }
    
    public Integer getAttributeConsumingServiceIndex() {
        String index = client.getAttribute(SPID_ATTRIBUTE_CONSUMING_SERVICE_INDEX);
        if (index != null && !index.isEmpty()) {
            try {
                return Integer.parseInt(index);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    public String getAttributeConsumingServiceName() {
        return client.getAttribute(SPID_ATTRIBUTE_CONSUMING_SERVICE_NAME);
    }
    
    /**
     * Get requested attributes list from client configuration.
     * Format: comma-separated list of attribute names (e.g., "fiscalNumber,name,familyName")
     * or empty/null to use default SPID attributes.
     * 
     * @return List of requested attribute names, or null if not configured
     */
    public String getRequestedAttributes() {
        return client.getAttribute(SPID_REQUESTED_ATTRIBUTES);
    }
    
    /**
     * Get multiple AttributeConsumingService configurations from client.
     * Format: JSON array of AttributeConsumingService objects.
     * 
     * @return JSON string with AttributeConsumingService configurations, or null if not configured
     */
    public String getAttributeConsumingServices() {
        return client.getAttribute(SPID_ATTRIBUTE_CONSUMING_SERVICES);
    }
    
    // Contact OTHER getters
    public boolean isSpPrivate() {
        String value = client.getAttribute(SPID_CONTACT_OTHER_SP_PRIVATE);
        return Boolean.parseBoolean(value);
    }
    
    public String getIpaCode() {
        return client.getAttribute(SPID_CONTACT_OTHER_IPA_CODE);
    }
    
    public String getVatNumber() {
        return client.getAttribute(SPID_CONTACT_OTHER_VAT_NUMBER);
    }
    
    public String getFiscalCode() {
        return client.getAttribute(SPID_CONTACT_OTHER_FISCAL_CODE);
    }
    
    public String getOtherContactCompany() {
        return client.getAttribute(SPID_CONTACT_OTHER_COMPANY);
    }
    
    public String getOtherContactPhone() {
        return client.getAttribute(SPID_CONTACT_OTHER_PHONE);
    }
    
    public String getOtherContactEmail() {
        return client.getAttribute(SPID_CONTACT_OTHER_EMAIL);
    }
    
    // Aggregated entity getters
    public String getAggregatedIpaCode() {
        return client.getAttribute(SPID_AGGREGATED_IPA_CODE);
    }
    
    public String getAggregatedCompany() {
        return client.getAttribute(SPID_AGGREGATED_COMPANY);
    }
    
    public boolean isAggregatedPrivate() {
        String value = client.getAttribute(SPID_AGGREGATED_IS_PRIVATE);
        return Boolean.parseBoolean(value);
    }
    
    public String getAggregatedVatNumber() {
        return client.getAttribute(SPID_AGGREGATED_VAT_NUMBER);
    }
    
    public String getAggregatedFiscalCode() {
        return client.getAttribute(SPID_AGGREGATED_FISCAL_CODE);
    }
    
    public boolean isCollaudo() {
        String value = client.getAttribute(SPID_IS_COLLAUDO);
        return Boolean.parseBoolean(value);
    }

    /**
     * Indicates whether this client is a test-only SPID client.
     * When true, metadata generation will expose only a single
     * default AttributeConsumingService instead of the full set.
     */
    public boolean isTestClient() {
        String value = client.getAttribute(SPID_IS_TEST_CLIENT);
        return Boolean.parseBoolean(value);
    }
    
    // Contact BILLING getters
    public String getBillingContactCompany() {
        return client.getAttribute(SPID_CONTACT_BILLING_COMPANY);
    }
    
    public String getBillingContactPhone() {
        return client.getAttribute(SPID_CONTACT_BILLING_PHONE);
    }
    
    public String getBillingContactEmail() {
        return client.getAttribute(SPID_CONTACT_BILLING_EMAIL);
    }
    
    public String getBillingContactRegistryName() {
        return client.getAttribute(SPID_CONTACT_BILLING_REGISTRY_NAME);
    }
    
    public String getBillingContactSiteAddress() {
        return client.getAttribute(SPID_CONTACT_BILLING_SITE_ADDRESS);
    }
    
    public String getBillingContactSiteNumber() {
        return client.getAttribute(SPID_CONTACT_BILLING_SITE_NUMBER);
    }
    
    public String getBillingContactSiteCity() {
        return client.getAttribute(SPID_CONTACT_BILLING_SITE_CITY);
    }
    
    public String getBillingContactSiteZipCode() {
        return client.getAttribute(SPID_CONTACT_BILLING_SITE_ZIP_CODE);
    }
    
    public String getBillingContactSiteProvince() {
        return client.getAttribute(SPID_CONTACT_BILLING_SITE_PROVINCE);
    }
    
    public String getBillingContactSiteCountry() {
        return client.getAttribute(SPID_CONTACT_BILLING_SITE_COUNTRY);
    }
    
    /**
     * Creates a SpidIdentityProviderConfig-like object from client attributes.
     * This allows reusing existing metadata generation code.
     */
    public SpidIdentityProviderConfigAdapter toProviderConfig() {
        return new SpidIdentityProviderConfigAdapter(this);
    }
    
    /**
     * Adapter class to make SpidClientConfig compatible with SpidIdentityProviderConfig
     * for metadata generation purposes.
     */
    public static class SpidIdentityProviderConfigAdapter extends org.keycloak.broker.spid.SpidIdentityProviderConfig {
        @SuppressWarnings("unused")
        private final SpidClientConfig clientConfig;
        
        public SpidIdentityProviderConfigAdapter(SpidClientConfig clientConfig) {
            super();
            this.clientConfig = clientConfig;
            
            // Map client attributes to provider config
            if (clientConfig.getEntityId() != null) {
                setEntityId(clientConfig.getEntityId());
            }
            if (clientConfig.getOrganizationNames() != null) {
                setOrganizationNames(clientConfig.getOrganizationNames());
            }
            if (clientConfig.getOrganizationDisplayNames() != null) {
                setOrganizationDisplayNames(clientConfig.getOrganizationDisplayNames());
            }
            if (clientConfig.getOrganizationUrls() != null) {
                setOrganizationUrls(clientConfig.getOrganizationUrls());
            }
            if (clientConfig.getAttributeConsumingServiceIndex() != null) {
                setAttributeConsumingServiceIndex(clientConfig.getAttributeConsumingServiceIndex());
            }
            if (clientConfig.getAttributeConsumingServiceName() != null) {
                setAttributeConsumingServiceName(clientConfig.getAttributeConsumingServiceName());
            }
            
            // Contact OTHER
            setSpPrivate(clientConfig.isSpPrivate());
            if (clientConfig.getIpaCode() != null) {
                setIpaCode(clientConfig.getIpaCode());
            }
            if (clientConfig.getVatNumber() != null) {
                setVatNumber(clientConfig.getVatNumber());
            }
            if (clientConfig.getFiscalCode() != null) {
                setFiscalCode(clientConfig.getFiscalCode());
            }
            if (clientConfig.getOtherContactCompany() != null) {
                setOtherContactCompany(clientConfig.getOtherContactCompany());
            }
            if (clientConfig.getOtherContactPhone() != null) {
                setOtherContactPhone(clientConfig.getOtherContactPhone());
            }
            if (clientConfig.getOtherContactEmail() != null) {
                setOtherContactEmail(clientConfig.getOtherContactEmail());
            }
            
            // Contact BILLING
            if (clientConfig.getBillingContactCompany() != null) {
                setBillingContactCompany(clientConfig.getBillingContactCompany());
            }
            if (clientConfig.getBillingContactPhone() != null) {
                setBillingContactPhone(clientConfig.getBillingContactPhone());
            }
            if (clientConfig.getBillingContactEmail() != null) {
                setBillingContactEmail(clientConfig.getBillingContactEmail());
            }
            if (clientConfig.getBillingContactRegistryName() != null) {
                setBillingContactRegistryName(clientConfig.getBillingContactRegistryName());
            }
            if (clientConfig.getBillingContactSiteAddress() != null) {
                setBillingContactSiteAddress(clientConfig.getBillingContactSiteAddress());
            }
            if (clientConfig.getBillingContactSiteNumber() != null) {
                setBillingContactSiteNumber(clientConfig.getBillingContactSiteNumber());
            }
            if (clientConfig.getBillingContactSiteCity() != null) {
                setBillingContactSiteCity(clientConfig.getBillingContactSiteCity());
            }
            if (clientConfig.getBillingContactSiteZipCode() != null) {
                setBillingContactSiteZipCode(clientConfig.getBillingContactSiteZipCode());
            }
            if (clientConfig.getBillingContactSiteProvince() != null) {
                setBillingContactSiteProvince(clientConfig.getBillingContactSiteProvince());
            }
            if (clientConfig.getBillingContactSiteCountry() != null) {
                setBillingContactSiteCountry(clientConfig.getBillingContactSiteCountry());
            }
        }
    }
}
