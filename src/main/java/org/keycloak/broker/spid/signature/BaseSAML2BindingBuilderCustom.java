package org.keycloak.broker.spid.signature;

import org.jboss.logging.Logger;
import org.keycloak.common.util.HtmlUtils;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.saml.RandomSecret;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.StringUtil;
import org.keycloak.saml.processing.core.util.XMLEncryptionUtil;
import org.keycloak.saml.processing.web.util.PostBindingUtil;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.URI;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class BaseSAML2BindingBuilderCustom<T extends BaseSAML2BindingBuilderCustom> {
    protected static final Logger logger = Logger.getLogger(BaseSAML2BindingBuilderCustom.class);
    protected String signingKeyName;
    protected KeyPair signingKeyPair;
    protected X509Certificate signingCertificate;
    protected boolean sign;
    protected boolean signAssertions;
    protected SignatureAlgorithm signatureAlgorithm;
    protected String relayState;
    protected int encryptionKeySize;
    protected PublicKey encryptionPublicKey;
    protected String encryptionAlgorithm;
    protected boolean encrypt;
    protected String canonicalizationMethodType;
    protected String keyEncryptionAlgorithm;
    protected String keyEncryptionDigestMethod;
    protected String keyEncryptionMgfAlgorithm;

    public BaseSAML2BindingBuilderCustom() {
        this.signatureAlgorithm = SignatureAlgorithm.RSA_SHA1;
        this.encryptionKeySize = 128;
        this.encryptionAlgorithm = "AES";
        this.canonicalizationMethodType = "http://www.w3.org/2001/10/xml-exc-c14n#";
    }

    public T canonicalizationMethod(String method) {
        this.canonicalizationMethodType = method;
        return (T)this;
    }

    public T signDocument() {
        this.sign = true;
        return (T)this;
    }

    public T signAssertions() {
        this.signAssertions = true;
        return (T)this;
    }

    public T signWith(String signingKeyName, KeyPair keyPair) {
        this.signingKeyName = signingKeyName;
        this.signingKeyPair = keyPair;
        return (T)this;
    }

    public T signWith(String signingKeyName, PrivateKey privateKey, PublicKey publicKey) {
        this.signingKeyName = signingKeyName;
        this.signingKeyPair = new KeyPair(publicKey, privateKey);
        return (T)this;
    }

    public T signWith(String signingKeyName, KeyPair keyPair, X509Certificate cert) {
        this.signingKeyName = signingKeyName;
        this.signingKeyPair = keyPair;
        this.signingCertificate = cert;
        return (T)this;
    }

    public T signWith(String signingKeyName, PrivateKey privateKey, PublicKey publicKey, X509Certificate cert) {
        this.signingKeyName = signingKeyName;
        this.signingKeyPair = new KeyPair(publicKey, privateKey);
        this.signingCertificate = cert;
        return (T)this;
    }

    public T signatureAlgorithm(SignatureAlgorithm alg) {
        this.signatureAlgorithm = alg;
        return (T)this;
    }

    public T encrypt(PublicKey publicKey) {
        this.encrypt = true;
        this.encryptionPublicKey = publicKey;
        return (T)this;
    }

    public T encryptionAlgorithm(String alg) {
        this.encryptionAlgorithm = alg;
        return (T)this;
    }

    public T encryptionKeySize(int size) {
        this.encryptionKeySize = size;
        return (T)this;
    }

    public T keyEncryptionAlgorithm(String keyEncryptionAlgorithm) {
        this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
        return (T)this;
    }

    public T keyEncryptionDigestMethod(String keyEncryptionDigestMethod) {
        this.keyEncryptionDigestMethod = keyEncryptionDigestMethod;
        return (T)this;
    }

    public T keyEncryptionMgfAlgorithm(String keyEncryptionMgfAlgorithm) {
        this.keyEncryptionMgfAlgorithm = keyEncryptionMgfAlgorithm;
        return (T)this;
    }

    public T relayState(String relayState) {
        this.relayState = relayState;
        return (T)this;
    }

    public BaseSAML2BindingBuilderCustom.BaseRedirectBindingBuilder redirectBinding(Document document) throws ProcessingException {
        return new BaseSAML2BindingBuilderCustom.BaseRedirectBindingBuilder(this, document);
    }

    public BaseSAML2BindingBuilderCustom<T>.BasePostBindingBuilder postBinding(Document document) throws ProcessingException {
        return new BaseSAML2BindingBuilderCustom.BasePostBindingBuilder(this, document);
    }

    public BaseSAML2BindingBuilderCustom.BaseSoapBindingBuilder soapBinding(Document document) throws ProcessingException {
        return new BaseSAML2BindingBuilderCustom.BaseSoapBindingBuilder(this, document);
    }

    public String getSAMLNSPrefix(Document samlResponseDocument) {
        Node assertionElement = samlResponseDocument.getDocumentElement().getElementsByTagNameNS(JBossSAMLURIConstants.ASSERTION_NSURI.get(), JBossSAMLConstants.ASSERTION.get()).item(0);
        if (assertionElement == null) {
            throw new IllegalStateException("Unable to find assertion in saml response document");
        } else {
            return assertionElement.getPrefix();
        }
    }

    public void encryptDocument(Document samlDocument) throws ProcessingException {
        String samlNSPrefix = this.getSAMLNSPrefix(samlDocument);

        try {
            QName encryptedAssertionElementQName = new QName(JBossSAMLURIConstants.ASSERTION_NSURI.get(), JBossSAMLConstants.ENCRYPTED_ASSERTION.get(), samlNSPrefix);
            byte[] secret = RandomSecret.createRandomSecret(this.encryptionKeySize / 8);
            SecretKey secretKey = new SecretKeySpec(secret, this.encryptionAlgorithm);
            XMLEncryptionUtil.encryptElement(new QName(JBossSAMLURIConstants.ASSERTION_NSURI.get(), JBossSAMLConstants.ASSERTION.get(), samlNSPrefix), samlDocument, this.encryptionPublicKey, secretKey, this.encryptionKeySize, encryptedAssertionElementQName, true, this.keyEncryptionAlgorithm, this.keyEncryptionDigestMethod, this.keyEncryptionMgfAlgorithm);
        } catch (Exception e) {
            throw new ProcessingException("failed to encrypt", e);
        }
    }

    public void signDocument(Document samlDocument) throws ProcessingException {
        String signatureMethod = this.signatureAlgorithm.getXmlSignatureMethod();
        String signatureDigestMethod = this.signatureAlgorithm.getXmlSignatureDigestMethod();
        SAML2SignatureCustom samlSignature = new SAML2SignatureCustom();
        if (signatureMethod != null) {
            samlSignature.setSignatureMethod(signatureMethod);
        }

        if (signatureDigestMethod != null) {
            samlSignature.setDigestMethod(signatureDigestMethod);
        }

        Node nextSibling = samlSignature.getNextSiblingOfIssuer(samlDocument);
        samlSignature.setNextSibling(nextSibling);
        if (this.signingCertificate != null) {
            samlSignature.setX509Certificate(this.signingCertificate);
        }

        samlSignature.signSAMLDocument(samlDocument, this.signingKeyName, this.signingKeyPair, this.canonicalizationMethodType);
    }

    public void signAssertion(Document samlDocument) throws ProcessingException {
        Element originalAssertionElement = DocumentUtil.getChildElement(samlDocument.getDocumentElement(), new QName(JBossSAMLURIConstants.ASSERTION_NSURI.get(), JBossSAMLConstants.ASSERTION.get()));
        if (originalAssertionElement != null) {
            Node clonedAssertionElement = originalAssertionElement.cloneNode(true);

            Document temporaryDocument;
            try {
                temporaryDocument = DocumentUtil.createDocument();
            } catch (ConfigurationException e) {
                throw new ProcessingException(e);
            }

            temporaryDocument.adoptNode(clonedAssertionElement);
            temporaryDocument.appendChild(clonedAssertionElement);
            this.signDocument(temporaryDocument);
            samlDocument.adoptNode(clonedAssertionElement);
            Element parentNode = (Element)originalAssertionElement.getParentNode();
            parentNode.replaceChild(clonedAssertionElement, originalAssertionElement);
        }
    }

    public String buildHtmlPostResponse(Document responseDoc, String actionUrl, boolean asRequest) throws ProcessingException, ConfigurationException, IOException {
        return this.buildHtml(getSAMLResponse(responseDoc), actionUrl, asRequest);
    }

    public static String getSAMLResponse(Document responseDoc) throws ProcessingException, ConfigurationException, IOException {
        byte[] responseBytes = DocumentUtil.getDocumentAsString(responseDoc).getBytes(GeneralConstants.SAML_CHARSET);
        return PostBindingUtil.base64Encode(new String(responseBytes, GeneralConstants.SAML_CHARSET));
    }

    public String buildHtml(String samlResponse, String actionUrl, boolean asRequest) {
        String key = "SAMLResponse";
        if (asRequest) {
            key = "SAMLRequest";
        }

        Map<String, String> inputTypes = new HashMap();
        inputTypes.put(key, samlResponse);
        if (StringUtil.isNotNull(this.relayState)) {
            inputTypes.put("RelayState", this.relayState);
        }

        return this.buildHtmlForm(actionUrl, inputTypes);
    }

    public String buildHtmlForm(String actionUrl, Map<String, String> inputTypes) {
        StringBuilder builder = new StringBuilder();
        builder.append("<HTML>").append("<HEAD>").append("<TITLE>Authentication Redirect</TITLE>").append("</HEAD>").append("<BODY Onload=\"document.forms[0].submit()\">").append("<FORM METHOD=\"POST\" ACTION=\"").append(HtmlUtils.escapeAttribute(actionUrl)).append("\">");
        builder.append("<p>Redirecting, please wait.</p>");

        for(String key : inputTypes.keySet()) {
            builder.append("<INPUT TYPE=\"HIDDEN\" NAME=\"").append(key).append("\"").append(" VALUE=\"").append(HtmlUtils.escapeAttribute((String)inputTypes.get(key))).append("\"/>");
        }

        builder.append("<NOSCRIPT>").append("<P>JavaScript is disabled. We strongly recommend to enable it. Click the button below to continue.</P>").append("<INPUT TYPE=\"SUBMIT\" VALUE=\"CONTINUE\" />").append("</NOSCRIPT>").append("</FORM></BODY></HTML>");
        return builder.toString();
    }

    public String base64Encoded(Document document) throws ConfigurationException, ProcessingException, IOException {
        String documentAsString = org.keycloak.saml.processing.core.saml.v2.util.DocumentUtil.getDocumentAsString(document);
        logger.debugv("saml document: {0}", documentAsString);
        byte[] responseBytes = documentAsString.getBytes(GeneralConstants.SAML_CHARSET);
        return RedirectBindingUtil.deflateBase64Encode(responseBytes);
    }

    public URI generateRedirectUri(String samlParameterName, String redirectUri, Document document) throws ConfigurationException, ProcessingException, IOException {
        KeycloakUriBuilder builder = KeycloakUriBuilder.fromUri(redirectUri);
        int pos = builder.getQuery() == null ? 0 : builder.getQuery().length();
        builder.queryParam(samlParameterName, new Object[]{this.base64Encoded(document)});
        if (this.relayState != null) {
            builder.queryParam("RelayState", new Object[]{this.relayState});
        }

        if (this.sign) {
            builder.queryParam("SigAlg", new Object[]{this.signatureAlgorithm.getXmlSignatureMethod()});
            URI uri = builder.build(new Object[0]);
            String rawQuery = uri.getRawQuery();
            if (pos > 0) {
                rawQuery = rawQuery.substring(pos + 1);
            }

            Signature signature = this.signatureAlgorithm.createSignature();
            byte[] sig = new byte[0];

            try {
                signature.initSign(this.signingKeyPair.getPrivate());
                signature.update(rawQuery.getBytes(GeneralConstants.SAML_CHARSET));
                sig = signature.sign();
            } catch (SignatureException | InvalidKeyException e) {
                throw new ProcessingException(e);
            }

            String encodedSig = RedirectBindingUtil.base64Encode(sig);
            builder.queryParam("Signature", new Object[]{encodedSig});
        }

        return builder.build(new Object[0]);
    }

    public class BasePostBindingBuilder {
        protected Document document;
        protected BaseSAML2BindingBuilderCustom builder;

        public BasePostBindingBuilder(BaseSAML2BindingBuilderCustom builder, Document document) throws ProcessingException {
            this.builder = builder;
            this.document = document;
            if (builder.signAssertions) {
                builder.signAssertion(document);
            }

            if (builder.encrypt) {
                builder.encryptDocument(document);
            }

            if (builder.sign) {
                builder.signDocument(document);
            }

        }

        public String encoded() throws ProcessingException, ConfigurationException, IOException {
            byte[] responseBytes = org.keycloak.saml.processing.core.saml.v2.util.DocumentUtil.getDocumentAsString(this.document).getBytes(GeneralConstants.SAML_CHARSET);
            return PostBindingUtil.base64Encode(new String(responseBytes, GeneralConstants.SAML_CHARSET));
        }

        public Document getDocument() {
            return this.document;
        }

        public String getHtmlResponse(String actionUrl) throws ProcessingException, ConfigurationException, IOException {
            String str = this.builder.buildHtmlPostResponse(this.document, actionUrl, false);
            return str;
        }

        public String getHtmlRequest(String actionUrl) throws ProcessingException, ConfigurationException, IOException {
            String str = this.builder.buildHtmlPostResponse(this.document, actionUrl, true);
            return str;
        }

        public String getRelayState() {
            return BaseSAML2BindingBuilderCustom.this.relayState;
        }
    }

    public static class BaseRedirectBindingBuilder {
        protected Document document;
        protected BaseSAML2BindingBuilderCustom builder;

        public BaseRedirectBindingBuilder(BaseSAML2BindingBuilderCustom builder, Document document) throws ProcessingException {
            this.builder = builder;
            this.document = document;
            if (builder.encrypt) {
                builder.encryptDocument(document);
            }

            if (builder.signAssertions) {
                builder.signAssertion(document);
            }

        }

        public Document getDocument() {
            return this.document;
        }

        public URI generateURI(String redirectUri, boolean asRequest) throws ConfigurationException, ProcessingException, IOException {
            String samlParameterName = "SAMLResponse";
            if (asRequest) {
                samlParameterName = "SAMLRequest";
            }

            return this.builder.generateRedirectUri(samlParameterName, redirectUri, this.document);
        }

        public URI requestURI(String actionUrl) throws ConfigurationException, ProcessingException, IOException {
            return this.builder.generateRedirectUri("SAMLRequest", actionUrl, this.document);
        }

        public URI responseURI(String actionUrl) throws ConfigurationException, ProcessingException, IOException {
            return this.builder.generateRedirectUri("SAMLResponse", actionUrl, this.document);
        }
    }

    public static class BaseSoapBindingBuilder {
        protected Document document;
        protected BaseSAML2BindingBuilderCustom builder;

        public BaseSoapBindingBuilder(BaseSAML2BindingBuilderCustom builder, Document document) throws ProcessingException {
            this.builder = builder;
            this.document = document;
            if (builder.signAssertions) {
                builder.signAssertion(document);
            }

            if (builder.encrypt) {
                builder.encryptDocument(document);
            }

            if (builder.sign) {
                builder.signDocument(document);
            }

        }

        public Document getDocument() {
            return this.document;
        }
    }
}
