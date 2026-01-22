package org.keycloak.broker.spid.signature;

import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.saml.profile.util.Soap;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.w3c.dom.Document;

import java.io.IOException;
import java.net.URI;

public class JaxrsSAML2BindingBuilderCustom extends BaseSAML2BindingBuilderCustom<JaxrsSAML2BindingBuilderCustom> {

    private final KeycloakSession session;

    public JaxrsSAML2BindingBuilderCustom(KeycloakSession session) {
        this.session = session;
    }

    public JaxrsSAML2BindingBuilderCustom.RedirectBindingBuilder redirectBinding(Document document) throws ProcessingException {
        return new JaxrsSAML2BindingBuilderCustom.RedirectBindingBuilder(this, document);
    }

    public JaxrsSAML2BindingBuilderCustom.PostBindingBuilder postBinding(Document document) throws ProcessingException {
        return new JaxrsSAML2BindingBuilderCustom.PostBindingBuilder(this, document);
    }

    public JaxrsSAML2BindingBuilderCustom.SoapBindingBuilder soapBinding(Document document) throws ProcessingException {
        return new JaxrsSAML2BindingBuilderCustom.SoapBindingBuilder(this, document);
    }

    public class PostBindingBuilder extends BaseSAML2BindingBuilderCustom<JaxrsSAML2BindingBuilderCustom>.BasePostBindingBuilder {
        public PostBindingBuilder(JaxrsSAML2BindingBuilderCustom builder, Document document) throws ProcessingException {
            super(builder, document);
        }

        public Response request(String actionUrl) throws ConfigurationException, ProcessingException, IOException {
            return this.createResponse(actionUrl, "SAMLRequest");
        }

        public Response response(String actionUrl) throws ConfigurationException, ProcessingException, IOException {
            return this.createResponse(actionUrl, "SAMLResponse");
        }

        private Response createResponse(String actionUrl, String key) throws ProcessingException, ConfigurationException, IOException {
            MultivaluedMap<String, String> formData = new MultivaluedHashMap();
            formData.add("url", actionUrl);
            formData.add(key, BaseSAML2BindingBuilder.getSAMLResponse(this.document));
            if (this.getRelayState() != null) {
                formData.add("RelayState", this.getRelayState());
            }

            return ((LoginFormsProvider)JaxrsSAML2BindingBuilderCustom.this.session.getProvider(LoginFormsProvider.class)).setFormData(formData).createSamlPostForm();
        }
    }

    public static class RedirectBindingBuilder extends BaseSAML2BindingBuilderCustom.BaseRedirectBindingBuilder {
        public RedirectBindingBuilder(JaxrsSAML2BindingBuilderCustom builder, Document document) throws ProcessingException {
            super(builder, document);
        }

        public Response response(String redirectUri) throws ProcessingException, ConfigurationException, IOException {
            return this.response(redirectUri, false);
        }

        public Response request(String redirect) throws ProcessingException, ConfigurationException, IOException {
            return this.response(redirect, true);
        }

        private Response response(String redirectUri, boolean asRequest) throws ProcessingException, ConfigurationException, IOException {
            URI uri = this.generateURI(redirectUri, asRequest);
            JaxrsSAML2BindingBuilderCustom.logger.tracef("redirect-binding uri: %s", uri);
            CacheControl cacheControl = new CacheControl();
            cacheControl.setNoCache(true);
            return Response.status(302).location(uri).header("Pragma", "no-cache").header("Cache-Control", "no-cache, no-store").build();
        }
    }

    public static class SoapBindingBuilder extends BaseSAML2BindingBuilderCustom.BaseSoapBindingBuilder {
        public SoapBindingBuilder(JaxrsSAML2BindingBuilderCustom builder, Document document) throws ProcessingException {
            super(builder, document);
        }

        public Response response() throws ConfigurationException, ProcessingException, IOException {
            try {
                Soap.SoapMessageBuilder messageBuilder = Soap.createMessage();
                messageBuilder.addToBody(this.document);
                return messageBuilder.build();
            } catch (Exception e) {
                throw new RuntimeException("Error while creating SAML response.", e);
            }
        }
    }
}
