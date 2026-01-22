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
 * ContactPerson for SPID aggregated entity.
 * This represents the organization that is aggregated by the aggregator.
 */
public class SpidAggregatedContactType extends ContactType {

    public static final String XMLNS_NS = "http://www.w3.org/2000/xmlns/";
    public static final String SPID_METADATA_EXTENSIONS_NS = "https://spid.gov.it/saml-extensions";

    protected Document doc;

    public static Optional<SpidAggregatedContactType> build(final SpidClientConfig clientConfig) throws ConfigurationException {
        String company = clientConfig.getAggregatedCompany();
        String ipaCode = clientConfig.getAggregatedIpaCode();
        
        // Fallback to contact.other if aggregated is not configured
        if (StringUtil.isNullOrEmpty(company)) {
            company = clientConfig.getOtherContactCompany();
        }
        if (StringUtil.isNullOrEmpty(ipaCode)) {
            ipaCode = clientConfig.getIpaCode();
        }
        
        // At least company or ipaCode should be present (from aggregated or contact.other)
        if (StringUtil.isNullOrEmpty(company) && StringUtil.isNullOrEmpty(ipaCode)) {
            return Optional.empty();
        }
        return Optional.of(new SpidAggregatedContactType(clientConfig));
    }

    protected SpidAggregatedContactType(final SpidClientConfig clientConfig) throws ConfigurationException {
        super(ContactTypeType.OTHER);
        
        this.setExtensions(new ExtensionsType());
        doc = DocumentUtil.createDocument();
        
        // Check if this is for collaudo (fictional values for testing)
        boolean isCollaudo = clientConfig.isCollaudo();
        boolean isPrivate = clientConfig.isAggregatedPrivate();
        
        if (isCollaudo) {
            // According to Avviso SPID n°22 v3.0, for collaudo:
            // - Company: "Organizzazione fittizia per il collaudo"
            // - IPACode: "__aggrsint" (double underscore) for PA
            // - VATNumber: "__aggrsint" (double underscore) for private entities
            this.setCompany("Organizzazione fittizia per il collaudo");
            
            // Add qualifier based on whether aggregated entity is private or public
            if (isPrivate) {
                addQualifier("spid:Private");
                // For private entities in collaudo, add VATNumber with fictional value
                addExtensionElement("spid:VATNumber", "__aggrsint");
            } else {
                addQualifier("spid:Public");
                // For public entities in collaudo, add IPACode with fictional value
                addExtensionElement("spid:IPACode", "__aggrsint");
            }
        } else {
            // For production, use real values from configuration
            // Set company from client config (aggregated company)
            String company = clientConfig.getAggregatedCompany();
            if (!StringUtil.isNullOrEmpty(company)) {
                this.setCompany(company);
            } else {
                // Fallback to contact.other.company if aggregated.company is not set
                company = clientConfig.getOtherContactCompany();
                if (!StringUtil.isNullOrEmpty(company)) {
                    this.setCompany(company);
                }
            }
            
            // Add qualifier based on whether aggregated entity is private or public
            if (isPrivate) {
                addQualifier("spid:Private");
            } else {
                addQualifier("spid:Public");
            }
            
            // Add IPACode if available (from aggregated.ipaCode or contact.other.ipaCode)
            String ipaCode = clientConfig.getAggregatedIpaCode();
            if (StringUtil.isNullOrEmpty(ipaCode)) {
                ipaCode = clientConfig.getIpaCode();
            }
            if (!StringUtil.isNullOrEmpty(ipaCode)) {
                addExtensionElement("spid:IPACode", ipaCode);
            }
            
            // Add VATNumber and FiscalCode if available (for private entities)
            // These come from aggregated entity configuration, not from client contact.other
            if (isPrivate) {
                String vatNumber = clientConfig.getAggregatedVatNumber();
                if (!StringUtil.isNullOrEmpty(vatNumber)) {
                    addExtensionElement("spid:VATNumber", vatNumber);
                }
                
                String fiscalCode = clientConfig.getAggregatedFiscalCode();
                if (!StringUtil.isNullOrEmpty(fiscalCode)) {
                    addExtensionElement("spid:FiscalCode", fiscalCode);
                }
            }
        }
        
        // Add email and phone from contact.other (if available)
        // These are the contact details for the aggregated entity
        // Note: According to Avviso n°22, for collaudo these are typically not present
        // but we keep them for flexibility
        String email = clientConfig.getOtherContactEmail();
        if (!StringUtil.isNullOrEmpty(email)) {
            this.addEmailAddress(email);
        }
        
        String phone = clientConfig.getOtherContactPhone();
        if (!StringUtil.isNullOrEmpty(phone)) {
            this.addTelephone(phone);
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
