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

package org.keycloak.broker.spid;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProvider.AuthenticationCallback;
import org.keycloak.dom.saml.v2.protocol.RequestAbstractType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.web.util.PostBindingUtil;
import org.keycloak.saml.validators.DestinationValidator;
import org.keycloak.services.resource.RealmResourceProvider;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Optional;

/**
 * Generic logout endpoint for SPID identity providers.
 * This endpoint identifies the IdP from the logout request/response Issuer
 * and delegates to the appropriate SpidSAMLEndpoint.
 */
public class SpidLogoutEndpointResourceProvider implements RealmResourceProvider {

    protected static final Logger logger = Logger.getLogger(SpidLogoutEndpointResourceProvider.class);

    private final KeycloakSession session;

    public SpidLogoutEndpointResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {
    }

    @GET
    @Path("endpoint")
    public Response redirectBinding(@QueryParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest,
                                    @QueryParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse,
                                    @QueryParam(GeneralConstants.RELAY_STATE) String relayState) {
        return handleLogout(samlRequest, samlResponse, relayState, true);
    }

    @POST
    @Path("endpoint")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postBinding(@FormParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest,
                                @FormParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse,
                                @FormParam(GeneralConstants.RELAY_STATE) String relayState) {
        return handleLogout(samlRequest, samlResponse, relayState, false);
    }

    private Response handleLogout(String samlRequest, String samlResponse, String relayState, boolean isRedirect) {
        try {
            RealmModel realm = session.getContext().getRealm();
            
            // Extract the IdP entityId from the SAML request/response
            String idpEntityId = extractIdpEntityId(samlRequest, samlResponse, isRedirect);
            
            if (idpEntityId == null) {
                logger.error("Could not extract IdP entityId from logout request/response");
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid SAML logout request/response: missing Issuer")
                    .build();
            }
            
            // Find the IdentityProviderModel matching this entityId
            Optional<IdentityProviderModel> idpModelOpt = realm.getIdentityProvidersStream()
                .filter(idp -> idp.getProviderId().equals(SpidIdentityProviderFactory.PROVIDER_ID) && idp.isEnabled())
                .filter(idp -> {
                    SpidIdentityProviderConfig config = new SpidIdentityProviderConfig(idp);
                    return idpEntityId.equals(config.getIdpEntityId());
                })
                .findFirst();
            
            if (!idpModelOpt.isPresent()) {
                logger.errorf("No SPID identity provider found with entityId: %s", idpEntityId);
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("No SPID identity provider found for entityId: " + idpEntityId)
                    .build();
            }
            
            IdentityProviderModel idpModel = idpModelOpt.get();
            
            // Create the appropriate SpidIdentityProvider and SpidSAMLEndpoint
            SpidIdentityProviderFactory factory = new SpidIdentityProviderFactory();
            SpidIdentityProvider provider = factory.create(session, idpModel);
            
            // Create a dummy callback (not used for logout)
            // For logout, we don't need a real callback, but SpidSAMLEndpoint requires one
            AuthenticationCallback callback = new AuthenticationCallback() {
                @Override
                public Response authenticated(org.keycloak.broker.provider.BrokeredIdentityContext context) {
                    // Should not be called during logout
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                }
                @Override
                public org.keycloak.sessions.AuthenticationSessionModel getAndVerifyAuthenticationSession(String encodedState) {
                    return null;
                }
                @Override
                public Response cancelled(IdentityProviderModel idp) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
                @Override
                public Response error(String idp) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
                @Override
                public Response retryLogin(IdentityProvider<?> idp, org.keycloak.sessions.AuthenticationSessionModel authSession) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            };
            
            // Use the default validator from the factory
            // The validation will be handled in SpidSAMLEndpoint.handleSamlResponse
            // which checks if the destination matches the generic logout endpoint
            DestinationValidator destinationValidator = DestinationValidator.forProtocolMap(null);
            
            SpidSAMLEndpoint endpoint = new SpidSAMLEndpoint(
                session,
                provider,
                provider.getConfig(),
                callback,
                destinationValidator
            );
            
            // Delegate to the appropriate endpoint method
            if (isRedirect) {
                return endpoint.redirectBinding(samlRequest, samlResponse, relayState);
            } else {
                return endpoint.postBinding(samlRequest, samlResponse, relayState);
            }
            
        } catch (Exception e) {
            logger.error("Error handling generic SPID logout", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error processing logout request: " + e.getMessage())
                .build();
        }
    }

    /**
     * Extract the IdP entityId from a SAML logout request or response.
     */
    private String extractIdpEntityId(String samlRequest, String samlResponse, boolean isRedirect) {
        try {
            SAMLDocumentHolder holder = null;
            
            if (samlRequest != null) {
                holder = isRedirect 
                    ? SAMLRequestParser.parseRequestRedirectBinding(samlRequest)
                    : SAMLRequestParser.parseRequestPostBinding(samlRequest);
            } else if (samlResponse != null) {
                holder = isRedirect
                    ? SAMLRequestParser.parseResponseRedirectBinding(samlResponse)
                    : SAMLRequestParser.parseResponseDocument(PostBindingUtil.base64Decode(samlResponse));
            }
            
            if (holder != null && holder.getSamlObject() instanceof RequestAbstractType) {
                RequestAbstractType request = (RequestAbstractType) holder.getSamlObject();
                if (request.getIssuer() != null && request.getIssuer().getValue() != null) {
                    return request.getIssuer().getValue();
                }
            } else if (holder != null && holder.getSamlObject() instanceof StatusResponseType) {
                StatusResponseType response = (StatusResponseType) holder.getSamlObject();
                if (response.getIssuer() != null && response.getIssuer().getValue() != null) {
                    return response.getIssuer().getValue();
                }
            }
        } catch (Exception e) {
            logger.warnf("Failed to extract IdP entityId from SAML message: %s", e.getMessage());
        }
        
        return null;
    }
}
