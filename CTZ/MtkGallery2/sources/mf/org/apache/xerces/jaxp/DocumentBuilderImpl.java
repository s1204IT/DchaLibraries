package mf.org.apache.xerces.jaxp;

import java.util.Hashtable;
import java.util.Map;
import mf.javax.xml.parsers.DocumentBuilder;
import mf.javax.xml.validation.Schema;
import mf.org.apache.xerces.dom.DOMImplementationImpl;
import mf.org.apache.xerces.dom.DOMMessageFormatter;
import mf.org.apache.xerces.dom.DocumentImpl;
import mf.org.apache.xerces.impl.validation.ValidationManager;
import mf.org.apache.xerces.impl.xs.XMLSchemaValidator;
import mf.org.apache.xerces.jaxp.validation.XSGrammarPoolContainer;
import mf.org.apache.xerces.parsers.DOMParser;
import mf.org.apache.xerces.util.SecurityManager;
import mf.org.apache.xerces.xni.XMLDocumentHandler;
import mf.org.apache.xerces.xni.parser.XMLComponent;
import mf.org.apache.xerces.xni.parser.XMLComponentManager;
import mf.org.apache.xerces.xni.parser.XMLConfigurationException;
import mf.org.apache.xerces.xni.parser.XMLDocumentSource;
import mf.org.apache.xerces.xni.parser.XMLParserConfiguration;
import mf.org.w3c.dom.DOMImplementation;
import mf.org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class DocumentBuilderImpl extends DocumentBuilder implements JAXPConstants {
    private static final String CREATE_CDATA_NODES_FEATURE = "http://apache.org/xml/features/create-cdata-nodes";
    private static final String CREATE_ENTITY_REF_NODES_FEATURE = "http://apache.org/xml/features/dom/create-entity-ref-nodes";
    private static final String INCLUDE_COMMENTS_FEATURE = "http://apache.org/xml/features/include-comments";
    private static final String INCLUDE_IGNORABLE_WHITESPACE = "http://apache.org/xml/features/dom/include-ignorable-whitespace";
    private static final String NAMESPACES_FEATURE = "http://xml.org/sax/features/namespaces";
    private static final String SECURITY_MANAGER = "http://apache.org/xml/properties/security-manager";
    private static final String VALIDATION_FEATURE = "http://xml.org/sax/features/validation";
    private static final String XINCLUDE_FEATURE = "http://apache.org/xml/features/xinclude";
    private static final String XMLSCHEMA_VALIDATION_FEATURE = "http://apache.org/xml/features/validation/schema";
    private final DOMParser domParser;
    private final EntityResolver fInitEntityResolver;
    private final ErrorHandler fInitErrorHandler;
    private final ValidationManager fSchemaValidationManager;
    private final XMLComponent fSchemaValidator;
    private final XMLComponentManager fSchemaValidatorComponentManager;
    private final UnparsedEntityHandler fUnparsedEntityHandler;
    private final Schema grammar;

    DocumentBuilderImpl(DocumentBuilderFactoryImpl dbf, Hashtable dbfAttrs, Hashtable features) throws SAXNotRecognizedException, SAXNotSupportedException {
        this(dbf, dbfAttrs, features, false);
    }

    DocumentBuilderImpl(DocumentBuilderFactoryImpl dbf, Hashtable dbfAttrs, Hashtable features, boolean secureProcessing) throws SAXNotRecognizedException, SAXNotSupportedException {
        XMLComponent validatorComponent;
        this.domParser = new DOMParser();
        if (dbf.isValidating()) {
            this.fInitErrorHandler = new DefaultValidationErrorHandler();
            setErrorHandler(this.fInitErrorHandler);
        } else {
            this.fInitErrorHandler = this.domParser.getErrorHandler();
        }
        this.domParser.setFeature(VALIDATION_FEATURE, dbf.isValidating());
        this.domParser.setFeature(NAMESPACES_FEATURE, dbf.isNamespaceAware());
        this.domParser.setFeature(INCLUDE_IGNORABLE_WHITESPACE, !dbf.isIgnoringElementContentWhitespace());
        this.domParser.setFeature(CREATE_ENTITY_REF_NODES_FEATURE, !dbf.isExpandEntityReferences());
        this.domParser.setFeature(INCLUDE_COMMENTS_FEATURE, !dbf.isIgnoringComments());
        this.domParser.setFeature(CREATE_CDATA_NODES_FEATURE, !dbf.isCoalescing());
        if (dbf.isXIncludeAware()) {
            this.domParser.setFeature(XINCLUDE_FEATURE, true);
        }
        if (secureProcessing) {
            this.domParser.setProperty(SECURITY_MANAGER, new SecurityManager());
        }
        this.grammar = dbf.getSchema();
        if (this.grammar != null) {
            XMLParserConfiguration config = this.domParser.getXMLParserConfiguration();
            if (this.grammar instanceof XSGrammarPoolContainer) {
                validatorComponent = new XMLSchemaValidator();
                this.fSchemaValidationManager = new ValidationManager();
                this.fUnparsedEntityHandler = new UnparsedEntityHandler(this.fSchemaValidationManager);
                config.setDTDHandler(this.fUnparsedEntityHandler);
                this.fUnparsedEntityHandler.setDTDHandler(this.domParser);
                this.domParser.setDTDSource(this.fUnparsedEntityHandler);
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
            ((XMLDocumentSource) validatorComponent).setDocumentHandler(this.domParser);
            this.domParser.setDocumentSource((XMLDocumentSource) validatorComponent);
            this.fSchemaValidator = validatorComponent;
        } else {
            this.fSchemaValidationManager = null;
            this.fUnparsedEntityHandler = null;
            this.fSchemaValidatorComponentManager = null;
            this.fSchemaValidator = null;
        }
        setFeatures(features);
        setDocumentBuilderFactoryAttributes(dbfAttrs);
        this.fInitEntityResolver = this.domParser.getEntityResolver();
    }

    private void setFeatures(Hashtable features) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (features != null) {
            for (Map.Entry entry : features.entrySet()) {
                String feature = (String) entry.getKey();
                boolean value = ((Boolean) entry.getValue()).booleanValue();
                this.domParser.setFeature(feature, value);
            }
        }
    }

    private void setDocumentBuilderFactoryAttributes(Hashtable dbfAttrs) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (dbfAttrs == null) {
            return;
        }
        for (Map.Entry entry : dbfAttrs.entrySet()) {
            String name = (String) entry.getKey();
            ?? value = entry.getValue();
            if (value instanceof Boolean) {
                this.domParser.setFeature(name, value.booleanValue());
            } else if (JAXPConstants.JAXP_SCHEMA_LANGUAGE.equals(name)) {
                if ("http://www.w3.org/2001/XMLSchema".equals(value) && isValidating()) {
                    this.domParser.setFeature(XMLSCHEMA_VALIDATION_FEATURE, true);
                    this.domParser.setProperty(JAXPConstants.JAXP_SCHEMA_LANGUAGE, "http://www.w3.org/2001/XMLSchema");
                }
            } else if (JAXPConstants.JAXP_SCHEMA_SOURCE.equals(name)) {
                if (isValidating()) {
                    String value2 = (String) dbfAttrs.get(JAXPConstants.JAXP_SCHEMA_LANGUAGE);
                    if (value2 != null && "http://www.w3.org/2001/XMLSchema".equals(value2)) {
                        this.domParser.setProperty(name, value);
                    } else {
                        throw new IllegalArgumentException(DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "jaxp-order-not-supported", new Object[]{JAXPConstants.JAXP_SCHEMA_LANGUAGE, JAXPConstants.JAXP_SCHEMA_SOURCE}));
                    }
                } else {
                    continue;
                }
            } else {
                this.domParser.setProperty(name, value);
            }
        }
    }

    @Override
    public Document newDocument() {
        return new DocumentImpl();
    }

    @Override
    public DOMImplementation getDOMImplementation() {
        return DOMImplementationImpl.getDOMImplementation();
    }

    @Override
    public Document parse(InputSource is) throws Exception {
        if (is == null) {
            throw new IllegalArgumentException(DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "jaxp-null-input-source", null));
        }
        if (this.fSchemaValidator != null) {
            if (this.fSchemaValidationManager != null) {
                this.fSchemaValidationManager.reset();
                this.fUnparsedEntityHandler.reset();
            }
            resetSchemaValidator();
        }
        this.domParser.parse(is);
        Document doc = this.domParser.getDocument();
        this.domParser.dropDocumentReferences();
        return doc;
    }

    @Override
    public boolean isNamespaceAware() {
        try {
            return this.domParser.getFeature(NAMESPACES_FEATURE);
        } catch (SAXException x) {
            throw new IllegalStateException(x.getMessage());
        }
    }

    @Override
    public boolean isValidating() {
        try {
            return this.domParser.getFeature(VALIDATION_FEATURE);
        } catch (SAXException x) {
            throw new IllegalStateException(x.getMessage());
        }
    }

    @Override
    public boolean isXIncludeAware() {
        try {
            return this.domParser.getFeature(XINCLUDE_FEATURE);
        } catch (SAXException e) {
            return false;
        }
    }

    @Override
    public void setEntityResolver(EntityResolver er) {
        this.domParser.setEntityResolver(er);
    }

    @Override
    public void setErrorHandler(ErrorHandler eh) {
        this.domParser.setErrorHandler(eh);
    }

    @Override
    public Schema getSchema() {
        return this.grammar;
    }

    @Override
    public void reset() {
        if (this.domParser.getErrorHandler() != this.fInitErrorHandler) {
            this.domParser.setErrorHandler(this.fInitErrorHandler);
        }
        if (this.domParser.getEntityResolver() != this.fInitEntityResolver) {
            this.domParser.setEntityResolver(this.fInitEntityResolver);
        }
    }

    DOMParser getDOMParser() {
        return this.domParser;
    }

    private void resetSchemaValidator() throws SAXException {
        try {
            this.fSchemaValidator.reset(this.fSchemaValidatorComponentManager);
        } catch (XMLConfigurationException e) {
            throw new SAXException(e);
        }
    }
}
