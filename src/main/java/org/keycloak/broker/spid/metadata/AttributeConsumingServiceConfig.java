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

import java.util.List;

/**
 * Configuration class for AttributeConsumingService.
 * Used to deserialize JSON configuration from client attributes.
 */
public class AttributeConsumingServiceConfig {
    private Integer index;
    private String serviceName;
    private String serviceDescription;
    private List<String> requestedAttributes;
    
    public AttributeConsumingServiceConfig() {
    }
    
    public AttributeConsumingServiceConfig(Integer index, String serviceName, String serviceDescription, List<String> requestedAttributes) {
        this.index = index;
        this.serviceName = serviceName;
        this.serviceDescription = serviceDescription;
        this.requestedAttributes = requestedAttributes;
    }
    
    public Integer getIndex() {
        return index;
    }
    
    public void setIndex(Integer index) {
        this.index = index;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getServiceDescription() {
        return serviceDescription;
    }
    
    public void setServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription;
    }
    
    public List<String> getRequestedAttributes() {
        return requestedAttributes;
    }
    
    public void setRequestedAttributes(List<String> requestedAttributes) {
        this.requestedAttributes = requestedAttributes;
    }
}
