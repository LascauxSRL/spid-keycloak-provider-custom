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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Default AttributeConsumingService configurations for SPID.
 * These are the standard service configurations that should be included in metadata.
 */
public class DefaultAttributeConsumingServices {
    
    /**
     * Get all default AttributeConsumingService configurations.
     * These include all standard SPID service combinations (index 0-30) 
     * plus eIDAS service sets (index 99-100).
     * 
     * @return List of default AttributeConsumingServiceConfig
     */
    public static List<AttributeConsumingServiceConfig> getAllDefaultServices() {
        List<AttributeConsumingServiceConfig> services = new ArrayList<>();
        
        // Index 0: default
        services.add(new AttributeConsumingServiceConfig(0, "default", "", 
            Arrays.asList("familyName", "name", "spidCode", "fiscalNumber")));
        
        // Index 1: default,profile
        services.add(new AttributeConsumingServiceConfig(1, "default,profile", "", 
            Arrays.asList("placeOfBirth", "gender", "idCard", "familyName", "name", 
                "spidCode", "dateOfBirth", "countyOfBirth", "fiscalNumber", "expirationDate")));
        
        // Index 2: default,email
        services.add(new AttributeConsumingServiceConfig(2, "default,email", "", 
            Arrays.asList("digitalAddress", "familyName", "name", "spidCode", 
                "fiscalNumber", "email")));
        
        // Index 3: default,phone
        services.add(new AttributeConsumingServiceConfig(3, "default,phone", "", 
            Arrays.asList("mobilePhone", "familyName", "name", "spidCode", "fiscalNumber")));
        
        // Index 4: default,address
        services.add(new AttributeConsumingServiceConfig(4, "default,address", "", 
            Arrays.asList("domicilePostalCode", "domicileProvince", "address", "familyName", 
                "name", "domicileMunicipality", "spidCode", "domicileNation", 
                "domicileStreetAddress", "fiscalNumber")));
        
        // Index 5: default,professional
        services.add(new AttributeConsumingServiceConfig(5, "default,professional", "", 
            Arrays.asList("ivaCode", "familyName", "companyName", "name", 
                "companyFiscalNumber", "spidCode", "registeredOffice", "fiscalNumber")));
        
        // Index 6: default,profile,email
        services.add(new AttributeConsumingServiceConfig(6, "default,profile,email", "", 
            Arrays.asList("placeOfBirth", "gender", "idCard", "digitalAddress", "familyName", 
                "name", "spidCode", "dateOfBirth", "countyOfBirth", "fiscalNumber", 
                "email", "expirationDate")));
        
        // Index 7: default,profile,phone
        services.add(new AttributeConsumingServiceConfig(7, "default,profile,phone", "", 
            Arrays.asList("placeOfBirth", "gender", "mobilePhone", "idCard", "familyName", 
                "name", "spidCode", "dateOfBirth", "countyOfBirth", "fiscalNumber", 
                "expirationDate")));
        
        // Index 8: default,profile,address
        services.add(new AttributeConsumingServiceConfig(8, "default,profile,address", "", 
            Arrays.asList("domicilePostalCode", "placeOfBirth", "address", "gender", 
                "idCard", "domicileMunicipality", "spidCode", "dateOfBirth", 
                "domicileStreetAddress", "domicileProvince", "familyName", "name", 
                "domicileNation", "countyOfBirth", "fiscalNumber", "expirationDate")));
        
        // Index 9: default,profile,professional
        services.add(new AttributeConsumingServiceConfig(9, "default,profile,professional", "", 
            Arrays.asList("ivaCode", "placeOfBirth", "gender", "idCard", "companyName", 
                "companyFiscalNumber", "spidCode", "dateOfBirth", "familyName", "name", 
                "countyOfBirth", "registeredOffice", "fiscalNumber", "expirationDate")));
        
        // Index 10: default,email,phone
        services.add(new AttributeConsumingServiceConfig(10, "default,email,phone", "", 
            Arrays.asList("mobilePhone", "digitalAddress", "familyName", "name", 
                "spidCode", "fiscalNumber", "email")));
        
        // Index 11: default,email,address
        services.add(new AttributeConsumingServiceConfig(11, "default,email,address", "", 
            Arrays.asList("domicilePostalCode", "domicileProvince", "address", 
                "digitalAddress", "familyName", "name", "domicileMunicipality", "spidCode", 
                "domicileNation", "domicileStreetAddress", "fiscalNumber", "email")));
        
        // Index 12: default,email,professional
        services.add(new AttributeConsumingServiceConfig(12, "default,email,professional", "", 
            Arrays.asList("ivaCode", "digitalAddress", "familyName", "companyName", "name", 
                "companyFiscalNumber", "spidCode", "registeredOffice", "fiscalNumber", 
                "email")));
        
        // Index 13: default,phone,address
        services.add(new AttributeConsumingServiceConfig(13, "default,phone,address", "", 
            Arrays.asList("domicilePostalCode", "domicileProvince", "address", "mobilePhone", 
                "familyName", "name", "domicileMunicipality", "spidCode", "domicileNation", 
                "domicileStreetAddress", "fiscalNumber")));
        
        // Index 14: default,phone,professional
        services.add(new AttributeConsumingServiceConfig(14, "default,phone,professional", "", 
            Arrays.asList("ivaCode", "mobilePhone", "familyName", "companyName", "name", 
                "companyFiscalNumber", "spidCode", "registeredOffice", "fiscalNumber")));
        
        // Index 15: default,address,professional
        services.add(new AttributeConsumingServiceConfig(15, "default,address,professional", "", 
            Arrays.asList("domicilePostalCode", "ivaCode", "address", "companyName", 
                "domicileMunicipality", "companyFiscalNumber", "spidCode", 
                "domicileStreetAddress", "domicileProvince", "familyName", "name", 
                "domicileNation", "registeredOffice", "fiscalNumber")));
        
        // Index 16: default,profile,email,phone
        services.add(new AttributeConsumingServiceConfig(16, "default,profile,email,phone", "", 
            Arrays.asList("placeOfBirth", "gender", "idCard", "spidCode", "dateOfBirth", 
                "mobilePhone", "digitalAddress", "familyName", "name", "countyOfBirth", 
                "fiscalNumber", "email", "expirationDate")));
        
        // Index 17: default,profile,email,address
        services.add(new AttributeConsumingServiceConfig(17, "default,profile,email,address", "", 
            Arrays.asList("domicilePostalCode", "placeOfBirth", "address", "gender", "idCard", 
                "domicileMunicipality", "spidCode", "dateOfBirth", "domicileStreetAddress", 
                "domicileProvince", "digitalAddress", "familyName", "name", "domicileNation", 
                "countyOfBirth", "fiscalNumber", "email", "expirationDate")));
        
        // Index 18: default,profile,email,professional
        services.add(new AttributeConsumingServiceConfig(18, "default,profile,email,professional", "", 
            Arrays.asList("ivaCode", "placeOfBirth", "gender", "idCard", "companyName", 
                "companyFiscalNumber", "spidCode", "dateOfBirth", "digitalAddress", "familyName", 
                "name", "countyOfBirth", "registeredOffice", "fiscalNumber", "email", 
                "expirationDate")));
        
        // Index 19: default,profile,phone,address
        services.add(new AttributeConsumingServiceConfig(19, "default,profile,phone,address", "", 
            Arrays.asList("domicilePostalCode", "placeOfBirth", "address", "gender", "idCard", 
                "domicileMunicipality", "spidCode", "dateOfBirth", "domicileStreetAddress", 
                "domicileProvince", "mobilePhone", "familyName", "name", "domicileNation", 
                "countyOfBirth", "fiscalNumber", "expirationDate")));
        
        // Index 20: default,profile,phone,professional
        services.add(new AttributeConsumingServiceConfig(20, "default,profile,phone,professional", "", 
            Arrays.asList("ivaCode", "placeOfBirth", "gender", "idCard", "companyName", 
                "companyFiscalNumber", "spidCode", "dateOfBirth", "mobilePhone", "familyName", 
                "name", "countyOfBirth", "registeredOffice", "fiscalNumber", "expirationDate")));
        
        // Index 21: default,profile,address,professional
        services.add(new AttributeConsumingServiceConfig(21, "default,profile,address,professional", "", 
            Arrays.asList("domicilePostalCode", "ivaCode", "placeOfBirth", "address", "gender", 
                "idCard", "companyName", "domicileMunicipality", "companyFiscalNumber", "spidCode", 
                "dateOfBirth", "domicileStreetAddress", "domicileProvince", "familyName", "name", 
                "domicileNation", "countyOfBirth", "registeredOffice", "fiscalNumber", 
                "expirationDate")));
        
        // Index 22: default,email,phone,address
        services.add(new AttributeConsumingServiceConfig(22, "default,email,phone,address", "", 
            Arrays.asList("domicilePostalCode", "address", "domicileMunicipality", "spidCode", 
                "domicileStreetAddress", "domicileProvince", "mobilePhone", "digitalAddress", 
                "familyName", "name", "domicileNation", "fiscalNumber", "email")));
        
        // Index 23: default,email,phone,professional
        services.add(new AttributeConsumingServiceConfig(23, "default,email,phone,professional", "", 
            Arrays.asList("ivaCode", "mobilePhone", "digitalAddress", "familyName", "companyName", 
                "name", "companyFiscalNumber", "spidCode", "registeredOffice", "fiscalNumber", 
                "email")));
        
        // Index 24: default,email,address,professional
        services.add(new AttributeConsumingServiceConfig(24, "default,email,address,professional", "", 
            Arrays.asList("domicilePostalCode", "ivaCode", "address", "companyName", 
                "domicileMunicipality", "companyFiscalNumber", "spidCode", "domicileStreetAddress", 
                "domicileProvince", "digitalAddress", "familyName", "name", "domicileNation", 
                "registeredOffice", "fiscalNumber", "email")));
        
        // Index 25: default,profile,email,phone,address
        services.add(new AttributeConsumingServiceConfig(25, "default,profile,email,phone,address", "", 
            Arrays.asList("domicilePostalCode", "placeOfBirth", "address", "gender", "idCard", 
                "domicileMunicipality", "spidCode", "dateOfBirth", "domicileStreetAddress", 
                "domicileProvince", "mobilePhone", "digitalAddress", "familyName", "name", 
                "domicileNation", "countyOfBirth", "fiscalNumber", "email", "expirationDate")));
        
        // Index 26: default,profile,email,phone,professional
        services.add(new AttributeConsumingServiceConfig(26, "default,profile,email,phone,professional", "", 
            Arrays.asList("ivaCode", "placeOfBirth", "gender", "idCard", "companyName", 
                "companyFiscalNumber", "spidCode", "dateOfBirth", "mobilePhone", "digitalAddress", 
                "familyName", "name", "countyOfBirth", "registeredOffice", "fiscalNumber", 
                "email", "expirationDate")));
        
        // Index 27: default,profile,email,address,professional
        services.add(new AttributeConsumingServiceConfig(27, "default,profile,email,address,professional", "", 
            Arrays.asList("domicilePostalCode", "ivaCode", "placeOfBirth", "address", "gender", 
                "idCard", "companyName", "domicileMunicipality", "companyFiscalNumber", "spidCode", 
                "dateOfBirth", "domicileStreetAddress", "domicileProvince", "digitalAddress", 
                "familyName", "name", "domicileNation", "countyOfBirth", "registeredOffice", 
                "fiscalNumber", "email", "expirationDate")));
        
        // Index 28: default,profile,phone,address,professional
        services.add(new AttributeConsumingServiceConfig(28, "default,profile,phone,address,professional", "", 
            Arrays.asList("domicilePostalCode", "ivaCode", "placeOfBirth", "address", "gender", 
                "idCard", "companyName", "domicileMunicipality", "companyFiscalNumber", "spidCode", 
                "dateOfBirth", "domicileStreetAddress", "domicileProvince", "mobilePhone", 
                "familyName", "name", "domicileNation", "countyOfBirth", "registeredOffice", 
                "fiscalNumber", "expirationDate")));
        
        // Index 29: default,email,phone,address,professional
        services.add(new AttributeConsumingServiceConfig(29, "default,email,phone,address,professional", "", 
            Arrays.asList("domicilePostalCode", "ivaCode", "address", "companyName", 
                "domicileMunicipality", "companyFiscalNumber", "spidCode", "domicileStreetAddress", 
                "domicileProvince", "mobilePhone", "digitalAddress", "familyName", "name", 
                "domicileNation", "registeredOffice", "fiscalNumber", "email")));
        
        // Index 30: default,profile,email,phone,address,professional
        services.add(new AttributeConsumingServiceConfig(30, "default,profile,email,phone,address,professional", "", 
            Arrays.asList("domicilePostalCode", "ivaCode", "placeOfBirth", "address", "gender", 
                "idCard", "companyName", "domicileMunicipality", "companyFiscalNumber", "spidCode", 
                "dateOfBirth", "domicileStreetAddress", "domicileProvince", "mobilePhone", 
                "digitalAddress", "familyName", "name", "domicileNation", "countyOfBirth", 
                "registeredOffice", "fiscalNumber", "email", "expirationDate")));
        
        // Index 99: eIDAS Natural Person Minimum Attribute Set
        services.add(new AttributeConsumingServiceConfig(99, "eIDAS Natural Person Minimum Attribute Set", "", 
            Arrays.asList("familyName", "name", "dateOfBirth", "spidCode")));
        
        // Index 100: eIDAS Natural Person Full Attribute Set
        services.add(new AttributeConsumingServiceConfig(100, "eIDAS Natural Person Full Attribute Set", "", 
            Arrays.asList("placeOfBirth", "address", "gender", "familyName", "name", 
                "spidCode", "dateOfBirth")));
        
        return services;
    }
}
