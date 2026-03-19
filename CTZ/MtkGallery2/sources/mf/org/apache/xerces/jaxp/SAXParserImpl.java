package mf.org.apache.xerces.jaxp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import mf.javax.xml.parsers.SAXParser;
import mf.javax.xml.validation.Schema;
import mf.org.apache.xerces.impl.validation.ValidationManager;
import mf.org.apache.xerces.impl.xs.XMLSchemaValidator;
import mf.org.apache.xerces.jaxp.validation.XSGrammarPoolContainer;
import mf.org.apache.xerces.util.SAXMessageFormatter;
import mf.org.apache.xerces.util.SecurityManager;
import mf.org.apache.xerces.xni.XMLDocumentHandler;
import mf.org.apache.xerces.xni.parser.XMLComponent;
import mf.org.apache.xerces.xni.parser.XMLComponentManager;
import mf.org.apache.xerces.xni.parser.XMLConfigurationException;
import mf.org.apache.xerces.xni.parser.XMLDocumentSource;
import mf.org.apache.xerces.xni.parser.XMLParserConfiguration;
import mf.org.apache.xerces.xs.AttributePSVI;
import mf.org.apache.xerces.xs.ElementPSVI;
import mf.org.apache.xerces.xs.PSVIProvider;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.HandlerBase;
import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class SAXParserImpl extends SAXParser implements JAXPConstants, PSVIProvider {
    private static final String NAMESPACES_FEATURE = "http://xml.org/sax/features/namespaces";
    private static final String NAMESPACE_PREFIXES_FEATURE = "http://xml.org/sax/features/namespace-prefixes";
    private static final String SECURITY_MANAGER = "http://apache.org/xml/properties/security-manager";
    private static final String VALIDATION_FEATURE = "http://xml.org/sax/features/validation";
    private static final String XINCLUDE_FEATURE = "http://apache.org/xml/features/xinclude";
    private static final String XMLSCHEMA_VALIDATION_FEATURE = "http://apache.org/xml/features/validation/schema";
    private final EntityResolver fInitEntityResolver;
    private final ErrorHandler fInitErrorHandler;
    private final ValidationManager fSchemaValidationManager;
    private final XMLComponent fSchemaValidator;
    private final XMLComponentManager fSchemaValidatorComponentManager;
    private final UnparsedEntityHandler fUnparsedEntityHandler;
    private final Schema grammar;
    private String schemaLanguage;
    private final JAXPSAXParser xmlReader;

    SAXParserImpl(SAXParserFactoryImpl spf, Hashtable features) throws SAXException {
        this(spf, features, false);
    }

    SAXParserImpl(SAXParserFactoryImpl spf, Hashtable features, boolean secureProcessing) throws SAXException {
        XMLComponent validatorComponent;
        this.schemaLanguage = null;
        this.xmlReader = new JAXPSAXParser(this);
        this.xmlReader.setFeature0(NAMESPACES_FEATURE, spf.isNamespaceAware());
        this.xmlReader.setFeature0(NAMESPACE_PREFIXES_FEATURE, !spf.isNamespaceAware());
        if (spf.isXIncludeAware()) {
            this.xmlReader.setFeature0(XINCLUDE_FEATURE, true);
        }
        if (secureProcessing) {
            this.xmlReader.setProperty0(SECURITY_MANAGER, new SecurityManager());
        }
        setFeatures(features);
        if (spf.isValidating()) {
            this.fInitErrorHandler = new DefaultValidationErrorHandler();
            this.xmlReader.setErrorHandler(this.fInitErrorHandler);
        } else {
            this.fInitErrorHandler = this.xmlReader.getErrorHandler();
        }
        this.xmlReader.setFeature0(VALIDATION_FEATURE, spf.isValidating());
        this.grammar = spf.getSchema();
        if (this.grammar != null) {
            XMLParserConfiguration config = this.xmlReader.getXMLParserConfiguration();
            if (this.grammar instanceof XSGrammarPoolContainer) {
                validatorComponent = new XMLSchemaValidator();
                this.fSchemaValidationManager = new ValidationManager();
                this.fUnparsedEntityHandler = new UnparsedEntityHandler(this.fSchemaValidationManager);
                config.setDTDHandler(this.fUnparsedEntityHandler);
                this.fUnparsedEntityHandler.setDTDHandler(this.xmlReader);
                this.xmlReader.setDTDSource(this.fUnparsedEntityHandler);
                this.fSchemaValidatorComponentManager = new SchemaValidatorConfiguration(config, (XSGrammarPoolContainer) this.grammar, this.fSchemaValidationManager);
            } else {
                XMLComponent validatorComponent2 = new JAXPValidatorComponent(this.grammar.newValidatorHandler());
                this.fSchemaValidationManager = null;
                this.fUnparsedEntityHandler = null;
                this.fSchemaValidatorComponentManager = config;
                validatorComponent = validatorComponent2;
            }
            config.addRecognizedFeatures(validatorComponent.getRecognizedFeatures());
            config.addRecognizedProperties(validatorComponent.getRecognizedProperties());
            config.setDocumentHandler((XMLDocumentHandler) validatorComponent);
            ((XMLDocumentSource) validatorComponent).setDocumentHandler(this.xmlReader);
            this.xmlReader.setDocumentSource((XMLDocumentSource) validatorComponent);
            this.fSchemaValidator = validatorComponent;
        } else {
            this.fSchemaValidationManager = null;
            this.fUnparsedEntityHandler = null;
            this.fSchemaValidatorComponentManager = null;
            this.fSchemaValidator = null;
        }
        this.fInitEntityResolver = this.xmlReader.getEntityResolver();
    }

    private void setFeatures(Hashtable features) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (features != null) {
            for (Map.Entry entry : features.entrySet()) {
                String feature = (String) entry.getKey();
                boolean value = ((Boolean) entry.getValue()).booleanValue();
                this.xmlReader.setFeature0(feature, value);
            }
        }
    }

    @Override
    public Parser getParser() throws SAXException {
        return this.xmlReader;
    }

    @Override
    public XMLReader getXMLReader() {
        return this.xmlReader;
    }

    @Override
    public boolean isNamespaceAware() {
        try {
            return this.xmlReader.getFeature(NAMESPACES_FEATURE);
        } catch (SAXException x) {
            throw new IllegalStateException(x.getMessage());
        }
    }

    @Override
    public boolean isValidating() {
        try {
            return this.xmlReader.getFeature(VALIDATION_FEATURE);
        } catch (SAXException x) {
            throw new IllegalStateException(x.getMessage());
        }
    }

    @Override
    public boolean isXIncludeAware() {
        try {
            return this.xmlReader.getFeature(XINCLUDE_FEATURE);
        } catch (SAXException e) {
            return false;
        }
    }

    @Override
    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        this.xmlReader.setProperty(name, value);
    }

    @Override
    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return this.xmlReader.getProperty(name);
    }

    @Override
    public void parse(InputSource is, DefaultHandler dh) throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException();
        }
        if (dh != null) {
            this.xmlReader.setContentHandler(dh);
            this.xmlReader.setEntityResolver(dh);
            this.xmlReader.setErrorHandler(dh);
            this.xmlReader.setDTDHandler(dh);
            this.xmlReader.setDocumentHandler(null);
        }
        this.xmlReader.parse(is);
    }

    @Override
    public void parse(InputSource is, HandlerBase hb) throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException();
        }
        if (hb != null) {
            this.xmlReader.setDocumentHandler(hb);
            this.xmlReader.setEntityResolver(hb);
            this.xmlReader.setErrorHandler(hb);
            this.xmlReader.setDTDHandler(hb);
            this.xmlReader.setContentHandler(null);
        }
        this.xmlReader.parse(is);
    }

    @Override
    public Schema getSchema() {
        return this.grammar;
    }

    @Override
    public void reset() {
        try {
            this.xmlReader.restoreInitState();
        } catch (SAXException e) {
        }
        this.xmlReader.setContentHandler(null);
        this.xmlReader.setDTDHandler(null);
        if (this.xmlReader.getErrorHandler() != this.fInitErrorHandler) {
            this.xmlReader.setErrorHandler(this.fInitErrorHandler);
        }
        if (this.xmlReader.getEntityResolver() != this.fInitEntityResolver) {
            this.xmlReader.setEntityResolver(this.fInitEntityResolver);
        }
    }

    @Override
    public ElementPSVI getElementPSVI() {
        return this.xmlReader.getElementPSVI();
    }

    @Override
    public AttributePSVI getAttributePSVI(int index) {
        return this.xmlReader.getAttributePSVI(index);
    }

    @Override
    public AttributePSVI getAttributePSVIByName(String uri, String localname) {
        return this.xmlReader.getAttributePSVIByName(uri, localname);
    }

    public static class JAXPSAXParser extends mf.org.apache.xerces.parsers.SAXParser {
        private final HashMap fInitFeatures;
        private final HashMap fInitProperties;
        private final SAXParserImpl fSAXParser;

        public JAXPSAXParser() {
            this(null);
        }

        JAXPSAXParser(SAXParserImpl saxParser) {
            this.fInitFeatures = new HashMap();
            this.fInitProperties = new HashMap();
            this.fSAXParser = saxParser;
        }

        @Override
        public synchronized void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
            SecurityManager securityManager;
            if (name == null) {
                throw new NullPointerException();
            }
            if (name.equals("http://javax.xml.XMLConstants/feature/secure-processing")) {
                if (value) {
                    try {
                        securityManager = new SecurityManager();
                    } catch (SAXNotRecognizedException exc) {
                        if (value) {
                            throw exc;
                        }
                    } catch (SAXNotSupportedException exc2) {
                        if (value) {
                            throw exc2;
                        }
                    }
                } else {
                    securityManager = null;
                }
                setProperty(SAXParserImpl.SECURITY_MANAGER, securityManager);
                return;
            }
            if (!this.fInitFeatures.containsKey(name)) {
                boolean current = super.getFeature(name);
                this.fInitFeatures.put(name, current ? Boolean.TRUE : Boolean.FALSE);
            }
            if (this.fSAXParser != null && this.fSAXParser.fSchemaValidator != null) {
                setSchemaValidatorFeature(name, value);
            }
            super.setFeature(name, value);
        }

        @Override
        public synchronized boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
            if (name == null) {
                throw new NullPointerException();
            }
            if (name.equals("http://javax.xml.XMLConstants/feature/secure-processing")) {
                try {
                    return super.getProperty(SAXParserImpl.SECURITY_MANAGER) != null;
                } catch (SAXException e) {
                    return false;
                }
            }
            return super.getFeature(name);
        }

        @Override
        public synchronized void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
            if (name == null) {
                throw new NullPointerException();
            }
            if (this.fSAXParser != null) {
                if (JAXPConstants.JAXP_SCHEMA_LANGUAGE.equals(name)) {
                    if (this.fSAXParser.grammar != null) {
                        throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "schema-already-specified", new Object[]{name}));
                    }
                    if ("http://www.w3.org/2001/XMLSchema".equals(value)) {
                        if (this.fSAXParser.isValidating()) {
                            this.fSAXParser.schemaLanguage = "http://www.w3.org/2001/XMLSchema";
                            setFeature(SAXParserImpl.XMLSCHEMA_VALIDATION_FEATURE, true);
                            if (!this.fInitProperties.containsKey(JAXPConstants.JAXP_SCHEMA_LANGUAGE)) {
                                this.fInitProperties.put(JAXPConstants.JAXP_SCHEMA_LANGUAGE, super.getProperty(JAXPConstants.JAXP_SCHEMA_LANGUAGE));
                            }
                            super.setProperty(JAXPConstants.JAXP_SCHEMA_LANGUAGE, "http://www.w3.org/2001/XMLSchema");
                        }
                    } else if (value == null) {
                        this.fSAXParser.schemaLanguage = null;
                        setFeature(SAXParserImpl.XMLSCHEMA_VALIDATION_FEATURE, false);
                    } else {
                        throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "schema-not-supported", null));
                    }
                    return;
                }
                if (JAXPConstants.JAXP_SCHEMA_SOURCE.equals(name)) {
                    if (this.fSAXParser.grammar != null) {
                        throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "schema-already-specified", new Object[]{name}));
                    }
                    String val = (String) getProperty(JAXPConstants.JAXP_SCHEMA_LANGUAGE);
                    if (val != null && "http://www.w3.org/2001/XMLSchema".equals(val)) {
                        if (!this.fInitProperties.containsKey(JAXPConstants.JAXP_SCHEMA_SOURCE)) {
                            this.fInitProperties.put(JAXPConstants.JAXP_SCHEMA_SOURCE, super.getProperty(JAXPConstants.JAXP_SCHEMA_SOURCE));
                        }
                        super.setProperty(name, value);
                        return;
                    }
                    throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "jaxp-order-not-supported", new Object[]{JAXPConstants.JAXP_SCHEMA_LANGUAGE, JAXPConstants.JAXP_SCHEMA_SOURCE}));
                }
            }
            if (!this.fInitProperties.containsKey(name)) {
                this.fInitProperties.put(name, super.getProperty(name));
            }
            if (this.fSAXParser != null && this.fSAXParser.fSchemaValidator != null) {
                setSchemaValidatorProperty(name, value);
            }
            super.setProperty(name, value);
        }

        @Override
        public synchronized Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
            if (name == null) {
                throw new NullPointerException();
            }
            if (this.fSAXParser != null && JAXPConstants.JAXP_SCHEMA_LANGUAGE.equals(name)) {
                return this.fSAXParser.schemaLanguage;
            }
            return super.getProperty(name);
        }

        synchronized void restoreInitState() throws SAXNotRecognizedException, SAXNotSupportedException {
            if (!this.fInitFeatures.isEmpty()) {
                for (Map.Entry entry : this.fInitFeatures.entrySet()) {
                    String name = (String) entry.getKey();
                    boolean value = ((Boolean) entry.getValue()).booleanValue();
                    super.setFeature(name, value);
                }
                this.fInitFeatures.clear();
            }
            if (!this.fInitProperties.isEmpty()) {
                for (Map.Entry entry2 : this.fInitProperties.entrySet()) {
                    String name2 = (String) entry2.getKey();
                    Object value2 = entry2.getValue();
                    super.setProperty(name2, value2);
                }
                this.fInitProperties.clear();
            }
        }

        @Override
        public void parse(InputSource inputSource) throws SAXException, IOException {
            if (this.fSAXParser != null && this.fSAXParser.fSchemaValidator != null) {
                if (this.fSAXParser.fSchemaValidationManager != null) {
                    this.fSAXParser.fSchemaValidationManager.reset();
                    this.fSAXParser.fUnparsedEntityHandler.reset();
                }
                resetSchemaValidator();
            }
            super.parse(inputSource);
        }

        @Override
        public void parse(String systemId) throws SAXException, IOException {
            if (this.fSAXParser != null && this.fSAXParser.fSchemaValidator != null) {
                if (this.fSAXParser.fSchemaValidationManager != null) {
                    this.fSAXParser.fSchemaValidationManager.reset();
                    this.fSAXParser.fUnparsedEntityHandler.reset();
                }
                resetSchemaValidator();
            }
            super.parse(systemId);
        }

        XMLParserConfiguration getXMLParserConfiguration() {
            return this.fConfiguration;
        }

        void setFeature0(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
            super.setFeature(name, value);
        }

        boolean getFeature0(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
            return super.getFeature(name);
        }

        void setProperty0(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
            super.setProperty(name, value);
        }

        Object getProperty0(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
            return super.getProperty(name);
        }

        private void setSchemaValidatorFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
            try {
                this.fSAXParser.fSchemaValidator.setFeature(name, value);
            } catch (XMLConfigurationException e) {
                String identifier = e.getIdentifier();
                if (e.getType() == 0) {
                    throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "feature-not-recognized", new Object[]{identifier}));
                }
                throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "feature-not-supported", new Object[]{identifier}));
            }
        }

        private void setSchemaValidatorProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
            try {
                this.fSAXParser.fSchemaValidator.setProperty(name, value);
            } catch (XMLConfigurationException e) {
                String identifier = e.getIdentifier();
                if (e.getType() == 0) {
                    throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "property-not-recognized", new Object[]{identifier}));
                }
                throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "property-not-supported", new Object[]{identifier}));
            }
        }

        private void resetSchemaValidator() throws SAXException {
            try {
                this.fSAXParser.fSchemaValidator.reset(this.fSAXParser.fSchemaValidatorComponentManager);
            } catch (XMLConfigurationException e) {
                throw new SAXException(e);
            }
        }
    }
}
