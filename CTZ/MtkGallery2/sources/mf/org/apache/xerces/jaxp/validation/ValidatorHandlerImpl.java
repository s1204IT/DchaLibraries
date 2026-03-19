package mf.org.apache.xerces.jaxp.validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import mf.javax.xml.parsers.FactoryConfigurationError;
import mf.javax.xml.parsers.SAXParserFactory;
import mf.javax.xml.transform.Result;
import mf.javax.xml.transform.Source;
import mf.javax.xml.transform.sax.SAXResult;
import mf.javax.xml.transform.sax.SAXSource;
import mf.javax.xml.validation.TypeInfoProvider;
import mf.javax.xml.validation.ValidatorHandler;
import mf.org.apache.xerces.impl.Constants;
import mf.org.apache.xerces.impl.XMLEntityManager;
import mf.org.apache.xerces.impl.XMLErrorReporter;
import mf.org.apache.xerces.impl.dv.XSSimpleType;
import mf.org.apache.xerces.impl.validation.EntityState;
import mf.org.apache.xerces.impl.validation.ValidationManager;
import mf.org.apache.xerces.impl.xs.XMLSchemaValidator;
import mf.org.apache.xerces.parsers.SAXParser;
import mf.org.apache.xerces.util.AttributesProxy;
import mf.org.apache.xerces.util.SAXLocatorWrapper;
import mf.org.apache.xerces.util.SAXMessageFormatter;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.URI;
import mf.org.apache.xerces.util.XMLAttributesImpl;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xni.Augmentations;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLAttributes;
import mf.org.apache.xerces.xni.XMLDocumentHandler;
import mf.org.apache.xerces.xni.XMLLocator;
import mf.org.apache.xerces.xni.XMLResourceIdentifier;
import mf.org.apache.xerces.xni.XMLString;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.parser.XMLConfigurationException;
import mf.org.apache.xerces.xni.parser.XMLDocumentSource;
import mf.org.apache.xerces.xni.parser.XMLParseException;
import mf.org.apache.xerces.xs.AttributePSVI;
import mf.org.apache.xerces.xs.ElementPSVI;
import mf.org.apache.xerces.xs.ItemPSVI;
import mf.org.apache.xerces.xs.PSVIProvider;
import mf.org.apache.xerces.xs.XSTypeDefinition;
import mf.org.w3c.dom.TypeInfo;
import mf.org.w3c.dom.ls.LSInput;
import mf.org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.Attributes2;
import org.xml.sax.ext.EntityResolver2;
import org.xml.sax.ext.LexicalHandler;

final class ValidatorHandlerImpl extends ValidatorHandler implements EntityState, ValidatorHelper, XMLDocumentHandler, PSVIProvider, DTDHandler {
    private static final String ERROR_REPORTER = "http://apache.org/xml/properties/internal/error-reporter";
    private static final String LEXICAL_HANDLER = "http://xml.org/sax/properties/lexical-handler";
    private static final String NAMESPACE_CONTEXT = "http://apache.org/xml/properties/internal/namespace-context";
    private static final String NAMESPACE_PREFIXES = "http://xml.org/sax/features/namespace-prefixes";
    private static final String SCHEMA_VALIDATOR = "http://apache.org/xml/properties/internal/validator/schema";
    private static final String SECURITY_MANAGER = "http://apache.org/xml/properties/security-manager";
    private static final String STRINGS_INTERNED = "http://apache.org/xml/features/internal/strings-interned";
    private static final String STRING_INTERNING = "http://xml.org/sax/features/string-interning";
    private static final String SYMBOL_TABLE = "http://apache.org/xml/properties/internal/symbol-table";
    private static final String VALIDATION_MANAGER = "http://apache.org/xml/properties/internal/validation-manager";
    private final AttributesProxy fAttrAdapter;
    private final QName fAttributeQName;
    private final XMLAttributesImpl fAttributes;
    private final XMLSchemaValidatorComponentManager fComponentManager;
    private ContentHandler fContentHandler;
    private final QName fElementQName;
    private final XMLErrorReporter fErrorReporter;
    private final NamespaceContext fNamespaceContext;
    private boolean fNeedPushNSContext;
    private final ResolutionForwarder fResolutionForwarder;
    private final SAXLocatorWrapper fSAXLocatorWrapper;
    private final XMLSchemaValidator fSchemaValidator;
    private boolean fStringsInternalized;
    private final SymbolTable fSymbolTable;
    private final XMLString fTempString;
    private final XMLSchemaTypeInfoProvider fTypeInfoProvider;
    private HashMap fUnparsedEntities;
    private final ValidationManager fValidationManager;

    public ValidatorHandlerImpl(XSGrammarPoolContainer grammarContainer) {
        this(new XMLSchemaValidatorComponentManager(grammarContainer));
        this.fComponentManager.addRecognizedFeatures(new String[]{NAMESPACE_PREFIXES});
        this.fComponentManager.setFeature(NAMESPACE_PREFIXES, false);
        setErrorHandler(null);
        setResourceResolver(null);
    }

    public ValidatorHandlerImpl(XMLSchemaValidatorComponentManager componentManager) {
        this.fSAXLocatorWrapper = new SAXLocatorWrapper();
        this.fNeedPushNSContext = true;
        this.fUnparsedEntities = null;
        this.fStringsInternalized = false;
        this.fElementQName = new QName();
        this.fAttributeQName = new QName();
        this.fAttributes = new XMLAttributesImpl();
        this.fAttrAdapter = new AttributesProxy(this.fAttributes);
        this.fTempString = new XMLString();
        this.fContentHandler = null;
        this.fTypeInfoProvider = new XMLSchemaTypeInfoProvider(this, null);
        this.fResolutionForwarder = new ResolutionForwarder(null);
        this.fComponentManager = componentManager;
        this.fErrorReporter = (XMLErrorReporter) this.fComponentManager.getProperty("http://apache.org/xml/properties/internal/error-reporter");
        this.fNamespaceContext = (NamespaceContext) this.fComponentManager.getProperty(NAMESPACE_CONTEXT);
        this.fSchemaValidator = (XMLSchemaValidator) this.fComponentManager.getProperty(SCHEMA_VALIDATOR);
        this.fSymbolTable = (SymbolTable) this.fComponentManager.getProperty("http://apache.org/xml/properties/internal/symbol-table");
        this.fValidationManager = (ValidationManager) this.fComponentManager.getProperty(VALIDATION_MANAGER);
    }

    @Override
    public void setContentHandler(ContentHandler receiver) {
        this.fContentHandler = receiver;
    }

    @Override
    public ContentHandler getContentHandler() {
        return this.fContentHandler;
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.fComponentManager.setErrorHandler(errorHandler);
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return this.fComponentManager.getErrorHandler();
    }

    @Override
    public void setResourceResolver(LSResourceResolver resourceResolver) {
        this.fComponentManager.setResourceResolver(resourceResolver);
    }

    @Override
    public LSResourceResolver getResourceResolver() {
        return this.fComponentManager.getResourceResolver();
    }

    @Override
    public TypeInfoProvider getTypeInfoProvider() {
        return this.fTypeInfoProvider;
    }

    @Override
    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException(JAXPValidationMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "FeatureNameNull", null));
        }
        if (STRINGS_INTERNED.equals(name)) {
            return this.fStringsInternalized;
        }
        try {
            return this.fComponentManager.getFeature(name);
        } catch (XMLConfigurationException e) {
            String identifier = e.getIdentifier();
            if (e.getType() == 0) {
                throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "feature-not-recognized", new Object[]{identifier}));
            }
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "feature-not-supported", new Object[]{identifier}));
        }
    }

    @Override
    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException(JAXPValidationMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "FeatureNameNull", null));
        }
        if (STRINGS_INTERNED.equals(name)) {
            this.fStringsInternalized = value;
            return;
        }
        try {
            this.fComponentManager.setFeature(name, value);
        } catch (XMLConfigurationException e) {
            String identifier = e.getIdentifier();
            if (e.getType() == 0) {
                throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "feature-not-recognized", new Object[]{identifier}));
            }
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "feature-not-supported", new Object[]{identifier}));
        }
    }

    @Override
    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException(JAXPValidationMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "ProperyNameNull", null));
        }
        try {
            return this.fComponentManager.getProperty(name);
        } catch (XMLConfigurationException e) {
            String identifier = e.getIdentifier();
            if (e.getType() == 0) {
                throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "property-not-recognized", new Object[]{identifier}));
            }
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "property-not-supported", new Object[]{identifier}));
        }
    }

    @Override
    public void setProperty(String name, Object object) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException(JAXPValidationMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "ProperyNameNull", null));
        }
        try {
            this.fComponentManager.setProperty(name, object);
        } catch (XMLConfigurationException e) {
            String identifier = e.getIdentifier();
            if (e.getType() == 0) {
                throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "property-not-recognized", new Object[]{identifier}));
            }
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "property-not-supported", new Object[]{identifier}));
        }
    }

    @Override
    public boolean isEntityDeclared(String name) {
        return false;
    }

    @Override
    public boolean isEntityUnparsed(String name) {
        if (this.fUnparsedEntities != null) {
            return this.fUnparsedEntities.containsKey(name);
        }
        return false;
    }

    @Override
    public void startDocument(XMLLocator locator, String encoding, NamespaceContext namespaceContext, Augmentations augs) throws XNIException {
        if (this.fContentHandler != null) {
            try {
                this.fContentHandler.startDocument();
            } catch (SAXException e) {
                throw new XNIException(e);
            }
        }
    }

    @Override
    public void xmlDecl(String version, String encoding, String standalone, Augmentations augs) throws XNIException {
    }

    @Override
    public void doctypeDecl(String rootElement, String publicId, String systemId, Augmentations augs) throws XNIException {
    }

    @Override
    public void comment(XMLString text, Augmentations augs) throws XNIException {
    }

    @Override
    public void processingInstruction(String target, XMLString data, Augmentations augs) throws XNIException {
        if (this.fContentHandler != null) {
            try {
                this.fContentHandler.processingInstruction(target, data.toString());
            } catch (SAXException e) {
                throw new XNIException(e);
            }
        }
    }

    @Override
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        try {
            if (this.fContentHandler != null) {
                try {
                    this.fTypeInfoProvider.beginStartElement(augs, attributes);
                    this.fContentHandler.startElement(element.uri != null ? element.uri : XMLSymbols.EMPTY_STRING, element.localpart, element.rawname, this.fAttrAdapter);
                } catch (SAXException e) {
                    throw new XNIException(e);
                }
            }
        } finally {
            this.fTypeInfoProvider.finishStartElement();
        }
    }

    @Override
    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        startElement(element, attributes, augs);
        endElement(element, augs);
    }

    @Override
    public void startGeneralEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs) throws XNIException {
    }

    @Override
    public void textDecl(String version, String encoding, Augmentations augs) throws XNIException {
    }

    @Override
    public void endGeneralEntity(String name, Augmentations augs) throws XNIException {
    }

    @Override
    public void characters(XMLString text, Augmentations augs) throws XNIException {
        if (this.fContentHandler == null || text.length == 0) {
            return;
        }
        try {
            this.fContentHandler.characters(text.ch, text.offset, text.length);
        } catch (SAXException e) {
            throw new XNIException(e);
        }
    }

    @Override
    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
        if (this.fContentHandler != null) {
            try {
                this.fContentHandler.ignorableWhitespace(text.ch, text.offset, text.length);
            } catch (SAXException e) {
                throw new XNIException(e);
            }
        }
    }

    @Override
    public void endElement(QName element, Augmentations augs) throws XNIException {
        try {
            if (this.fContentHandler != null) {
                try {
                    this.fTypeInfoProvider.beginEndElement(augs);
                    this.fContentHandler.endElement(element.uri != null ? element.uri : XMLSymbols.EMPTY_STRING, element.localpart, element.rawname);
                } catch (SAXException e) {
                    throw new XNIException(e);
                }
            }
        } finally {
            this.fTypeInfoProvider.finishEndElement();
        }
    }

    @Override
    public void startCDATA(Augmentations augs) throws XNIException {
    }

    @Override
    public void endCDATA(Augmentations augs) throws XNIException {
    }

    @Override
    public void endDocument(Augmentations augs) throws XNIException {
        if (this.fContentHandler != null) {
            try {
                this.fContentHandler.endDocument();
            } catch (SAXException e) {
                throw new XNIException(e);
            }
        }
    }

    @Override
    public void setDocumentSource(XMLDocumentSource source) {
    }

    @Override
    public XMLDocumentSource getDocumentSource() {
        return this.fSchemaValidator;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.fSAXLocatorWrapper.setLocator(locator);
        if (this.fContentHandler != null) {
            this.fContentHandler.setDocumentLocator(locator);
        }
    }

    @Override
    public void startDocument() throws SAXException {
        this.fComponentManager.reset();
        this.fSchemaValidator.setDocumentHandler(this);
        this.fValidationManager.setEntityState(this);
        this.fTypeInfoProvider.finishStartElement();
        this.fNeedPushNSContext = true;
        if (this.fUnparsedEntities != null && !this.fUnparsedEntities.isEmpty()) {
            this.fUnparsedEntities.clear();
        }
        this.fErrorReporter.setDocumentLocator(this.fSAXLocatorWrapper);
        try {
            this.fSchemaValidator.startDocument(this.fSAXLocatorWrapper, this.fSAXLocatorWrapper.getEncoding(), this.fNamespaceContext, null);
        } catch (XMLParseException e) {
            throw Util.toSAXParseException(e);
        } catch (XNIException e2) {
            throw Util.toSAXException(e2);
        }
    }

    @Override
    public void endDocument() throws SAXException {
        this.fSAXLocatorWrapper.setLocator(null);
        try {
            this.fSchemaValidator.endDocument(null);
        } catch (XMLParseException e) {
            throw Util.toSAXParseException(e);
        } catch (XNIException e2) {
            throw Util.toSAXException(e2);
        }
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        String prefixSymbol;
        String uriSymbol = null;
        if (!this.fStringsInternalized) {
            prefixSymbol = prefix != null ? this.fSymbolTable.addSymbol(prefix) : XMLSymbols.EMPTY_STRING;
            if (uri != null && uri.length() > 0) {
                uriSymbol = this.fSymbolTable.addSymbol(uri);
            }
        } else {
            prefixSymbol = prefix != null ? prefix : XMLSymbols.EMPTY_STRING;
            if (uri != null && uri.length() > 0) {
                uriSymbol = uri;
            }
        }
        if (this.fNeedPushNSContext) {
            this.fNeedPushNSContext = false;
            this.fNamespaceContext.pushContext();
        }
        this.fNamespaceContext.declarePrefix(prefixSymbol, uriSymbol);
        if (this.fContentHandler != null) {
            this.fContentHandler.startPrefixMapping(prefix, uri);
        }
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        if (this.fContentHandler != null) {
            this.fContentHandler.endPrefixMapping(prefix);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (this.fNeedPushNSContext) {
            this.fNamespaceContext.pushContext();
        }
        this.fNeedPushNSContext = true;
        fillQName(this.fElementQName, uri, localName, qName);
        if (atts instanceof Attributes2) {
            fillXMLAttributes2((Attributes2) atts);
        } else {
            fillXMLAttributes(atts);
        }
        try {
            this.fSchemaValidator.startElement(this.fElementQName, this.fAttributes, null);
        } catch (XMLParseException e) {
            throw Util.toSAXParseException(e);
        } catch (XNIException e2) {
            throw Util.toSAXException(e2);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        fillQName(this.fElementQName, uri, localName, qName);
        try {
            try {
                this.fSchemaValidator.endElement(this.fElementQName, null);
            } catch (XMLParseException e) {
                throw Util.toSAXParseException(e);
            } catch (XNIException e2) {
                throw Util.toSAXException(e2);
            }
        } finally {
            this.fNamespaceContext.popContext();
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        try {
            this.fTempString.setValues(ch, start, length);
            this.fSchemaValidator.characters(this.fTempString, null);
        } catch (XMLParseException e) {
            throw Util.toSAXParseException(e);
        } catch (XNIException e2) {
            throw Util.toSAXException(e2);
        }
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        try {
            this.fTempString.setValues(ch, start, length);
            this.fSchemaValidator.ignorableWhitespace(this.fTempString, null);
        } catch (XMLParseException e) {
            throw Util.toSAXParseException(e);
        } catch (XNIException e2) {
            throw Util.toSAXException(e2);
        }
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        if (this.fContentHandler != null) {
            this.fContentHandler.processingInstruction(target, data);
        }
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        if (this.fContentHandler != null) {
            this.fContentHandler.skippedEntity(name);
        }
    }

    @Override
    public void notationDecl(String name, String publicId, String systemId) throws SAXException {
    }

    @Override
    public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName) throws SAXException {
        if (this.fUnparsedEntities == null) {
            this.fUnparsedEntities = new HashMap();
        }
        this.fUnparsedEntities.put(name, name);
    }

    @Override
    public void validate(Source source, Result result) throws SAXException, IOException {
        Object securityManager;
        if ((result instanceof SAXResult) || result == null) {
            SAXSource saxSource = (SAXSource) source;
            SAXResult saxResult = (SAXResult) result;
            LexicalHandler lh = null;
            if (result != null) {
                ContentHandler ch = saxResult.getHandler();
                lh = saxResult.getLexicalHandler();
                if (lh == null && (ch instanceof LexicalHandler)) {
                    lh = (LexicalHandler) ch;
                }
                setContentHandler(ch);
            }
            XMLReader reader = null;
            try {
                reader = saxSource.getXMLReader();
                if (reader == null) {
                    SAXParserFactory spf = SAXParserFactory.newInstance();
                    spf.setNamespaceAware(true);
                    try {
                        reader = spf.newSAXParser().getXMLReader();
                        if ((reader instanceof SAXParser) && (securityManager = this.fComponentManager.getProperty(SECURITY_MANAGER)) != null) {
                            try {
                                reader.setProperty(SECURITY_MANAGER, securityManager);
                            } catch (SAXException e) {
                            }
                        }
                    } catch (Exception e2) {
                        throw new FactoryConfigurationError(e2);
                    }
                }
                try {
                    this.fStringsInternalized = reader.getFeature(STRING_INTERNING);
                } catch (SAXException e3) {
                    this.fStringsInternalized = false;
                }
                ErrorHandler errorHandler = this.fComponentManager.getErrorHandler();
                reader.setErrorHandler(errorHandler != null ? errorHandler : DraconianErrorHandler.getInstance());
                reader.setEntityResolver(this.fResolutionForwarder);
                this.fResolutionForwarder.setEntityResolver(this.fComponentManager.getResourceResolver());
                reader.setContentHandler(this);
                reader.setDTDHandler(this);
                try {
                    reader.setProperty(LEXICAL_HANDLER, lh);
                } catch (SAXException e4) {
                }
                InputSource is = saxSource.getInputSource();
                reader.parse(is);
                setContentHandler(null);
                if (reader != null) {
                    try {
                        reader.setContentHandler(null);
                        reader.setDTDHandler(null);
                        reader.setErrorHandler(null);
                        reader.setEntityResolver(null);
                        this.fResolutionForwarder.setEntityResolver(null);
                        reader.setProperty(LEXICAL_HANDLER, null);
                        return;
                    } catch (Exception e5) {
                        return;
                    }
                }
                return;
            } catch (Throwable th) {
                setContentHandler(null);
                if (reader != null) {
                    try {
                        reader.setContentHandler(null);
                        reader.setDTDHandler(null);
                        reader.setErrorHandler(null);
                        reader.setEntityResolver(null);
                        this.fResolutionForwarder.setEntityResolver(null);
                        reader.setProperty(LEXICAL_HANDLER, null);
                    } catch (Exception e6) {
                    }
                }
                throw th;
            }
        }
        throw new IllegalArgumentException(JAXPValidationMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "SourceResultMismatch", new Object[]{source.getClass().getName(), result.getClass().getName()}));
    }

    @Override
    public ElementPSVI getElementPSVI() {
        return this.fTypeInfoProvider.getElementPSVI();
    }

    @Override
    public AttributePSVI getAttributePSVI(int index) {
        return this.fTypeInfoProvider.getAttributePSVI(index);
    }

    @Override
    public AttributePSVI getAttributePSVIByName(String uri, String localname) {
        return this.fTypeInfoProvider.getAttributePSVIByName(uri, localname);
    }

    private void fillQName(QName toFill, String uri, String localpart, String raw) {
        if (!this.fStringsInternalized) {
            uri = (uri == null || uri.length() <= 0) ? null : this.fSymbolTable.addSymbol(uri);
            localpart = localpart != null ? this.fSymbolTable.addSymbol(localpart) : XMLSymbols.EMPTY_STRING;
            raw = raw != null ? this.fSymbolTable.addSymbol(raw) : XMLSymbols.EMPTY_STRING;
        } else {
            if (uri != null && uri.length() == 0) {
                uri = null;
            }
            if (localpart == null) {
                localpart = XMLSymbols.EMPTY_STRING;
            }
            if (raw == null) {
                raw = XMLSymbols.EMPTY_STRING;
            }
        }
        String prefix = XMLSymbols.EMPTY_STRING;
        int prefixIdx = raw.indexOf(58);
        if (prefixIdx != -1) {
            prefix = this.fSymbolTable.addSymbol(raw.substring(0, prefixIdx));
        }
        toFill.setValues(prefix, localpart, raw, uri);
    }

    private void fillXMLAttributes(Attributes att) {
        this.fAttributes.removeAllAttributes();
        int len = att.getLength();
        for (int i = 0; i < len; i++) {
            fillXMLAttribute(att, i);
            this.fAttributes.setSpecified(i, true);
        }
    }

    private void fillXMLAttributes2(Attributes2 att) {
        this.fAttributes.removeAllAttributes();
        int len = att.getLength();
        for (int i = 0; i < len; i++) {
            fillXMLAttribute(att, i);
            this.fAttributes.setSpecified(i, att.isSpecified(i));
            if (att.isDeclared(i)) {
                this.fAttributes.getAugmentations(i).putItem(Constants.ATTRIBUTE_DECLARED, Boolean.TRUE);
            }
        }
    }

    private void fillXMLAttribute(Attributes att, int index) {
        fillQName(this.fAttributeQName, att.getURI(index), att.getLocalName(index), att.getQName(index));
        String type = att.getType(index);
        this.fAttributes.addAttributeNS(this.fAttributeQName, type != null ? type : XMLSymbols.fCDATASymbol, att.getValue(index));
    }

    private class XMLSchemaTypeInfoProvider extends TypeInfoProvider {
        private XMLAttributes fAttributes;
        private Augmentations fElementAugs;
        private boolean fInEndElement;
        private boolean fInStartElement;

        private XMLSchemaTypeInfoProvider() {
            this.fInStartElement = false;
            this.fInEndElement = false;
        }

        XMLSchemaTypeInfoProvider(ValidatorHandlerImpl validatorHandlerImpl, XMLSchemaTypeInfoProvider xMLSchemaTypeInfoProvider) {
            this();
        }

        void beginStartElement(Augmentations elementAugs, XMLAttributes attributes) {
            this.fInStartElement = true;
            this.fElementAugs = elementAugs;
            this.fAttributes = attributes;
        }

        void finishStartElement() {
            this.fInStartElement = false;
            this.fElementAugs = null;
            this.fAttributes = null;
        }

        void beginEndElement(Augmentations elementAugs) {
            this.fInEndElement = true;
            this.fElementAugs = elementAugs;
        }

        void finishEndElement() {
            this.fInEndElement = false;
            this.fElementAugs = null;
        }

        private void checkStateAttribute() {
            if (!this.fInStartElement) {
                throw new IllegalStateException(JAXPValidationMessageFormatter.formatMessage(ValidatorHandlerImpl.this.fComponentManager.getLocale(), "TypeInfoProviderIllegalStateAttribute", null));
            }
        }

        private void checkStateElement() {
            if (!this.fInStartElement && !this.fInEndElement) {
                throw new IllegalStateException(JAXPValidationMessageFormatter.formatMessage(ValidatorHandlerImpl.this.fComponentManager.getLocale(), "TypeInfoProviderIllegalStateElement", null));
            }
        }

        @Override
        public TypeInfo getAttributeTypeInfo(int index) {
            checkStateAttribute();
            return getAttributeType(index);
        }

        private TypeInfo getAttributeType(int index) {
            checkStateAttribute();
            if (index < 0 || this.fAttributes.getLength() <= index) {
                throw new IndexOutOfBoundsException(Integer.toString(index));
            }
            Augmentations augs = this.fAttributes.getAugmentations(index);
            if (augs == null) {
                return null;
            }
            AttributePSVI psvi = (AttributePSVI) augs.getItem(Constants.ATTRIBUTE_PSVI);
            return getTypeInfoFromPSVI(psvi);
        }

        public TypeInfo getAttributeTypeInfo(String attributeUri, String attributeLocalName) {
            checkStateAttribute();
            return getAttributeTypeInfo(this.fAttributes.getIndex(attributeUri, attributeLocalName));
        }

        public TypeInfo getAttributeTypeInfo(String attributeQName) {
            checkStateAttribute();
            return getAttributeTypeInfo(this.fAttributes.getIndex(attributeQName));
        }

        @Override
        public TypeInfo getElementTypeInfo() {
            checkStateElement();
            if (this.fElementAugs == null) {
                return null;
            }
            ElementPSVI psvi = (ElementPSVI) this.fElementAugs.getItem(Constants.ELEMENT_PSVI);
            return getTypeInfoFromPSVI(psvi);
        }

        private TypeInfo getTypeInfoFromPSVI(ItemPSVI psvi) {
            XSTypeDefinition t;
            if (psvi == null) {
                return null;
            }
            if (psvi.getValidity() == 2 && (t = psvi.getMemberTypeDefinition()) != null) {
                if (t instanceof TypeInfo) {
                    return (TypeInfo) t;
                }
                return null;
            }
            XSTypeDefinition t2 = psvi.getTypeDefinition();
            if (t2 == null || !(t2 instanceof TypeInfo)) {
                return null;
            }
            return (TypeInfo) t2;
        }

        @Override
        public boolean isIdAttribute(int index) {
            checkStateAttribute();
            XSSimpleType type = (XSSimpleType) getAttributeType(index);
            if (type == null) {
                return false;
            }
            return type.isIDType();
        }

        @Override
        public boolean isSpecified(int index) {
            checkStateAttribute();
            return this.fAttributes.isSpecified(index);
        }

        ElementPSVI getElementPSVI() {
            if (this.fElementAugs != null) {
                return (ElementPSVI) this.fElementAugs.getItem(Constants.ELEMENT_PSVI);
            }
            return null;
        }

        AttributePSVI getAttributePSVI(int index) {
            Augmentations augs;
            if (this.fAttributes != null && (augs = this.fAttributes.getAugmentations(index)) != null) {
                return (AttributePSVI) augs.getItem(Constants.ATTRIBUTE_PSVI);
            }
            return null;
        }

        AttributePSVI getAttributePSVIByName(String uri, String localname) {
            Augmentations augs;
            if (this.fAttributes != null && (augs = this.fAttributes.getAugmentations(uri, localname)) != null) {
                return (AttributePSVI) augs.getItem(Constants.ATTRIBUTE_PSVI);
            }
            return null;
        }
    }

    static final class ResolutionForwarder implements EntityResolver2 {
        private static final String XML_TYPE = "http://www.w3.org/TR/REC-xml";
        protected LSResourceResolver fEntityResolver;

        public ResolutionForwarder() {
        }

        public ResolutionForwarder(LSResourceResolver entityResolver) {
            setEntityResolver(entityResolver);
        }

        public void setEntityResolver(LSResourceResolver entityResolver) {
            this.fEntityResolver = entityResolver;
        }

        public LSResourceResolver getEntityResolver() {
            return this.fEntityResolver;
        }

        @Override
        public InputSource getExternalSubset(String name, String baseURI) throws SAXException, IOException {
            return null;
        }

        @Override
        public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId) throws SAXException, IOException {
            LSInput lsInput;
            if (this.fEntityResolver != null && (lsInput = this.fEntityResolver.resolveResource("http://www.w3.org/TR/REC-xml", null, publicId, systemId, baseURI)) != null) {
                String pubId = lsInput.getPublicId();
                String sysId = lsInput.getSystemId();
                String baseSystemId = lsInput.getBaseURI();
                Reader charStream = lsInput.getCharacterStream();
                InputStream byteStream = lsInput.getByteStream();
                String data = lsInput.getStringData();
                String encoding = lsInput.getEncoding();
                InputSource inputSource = new InputSource();
                inputSource.setPublicId(pubId);
                inputSource.setSystemId(baseSystemId != null ? resolveSystemId(sysId, baseSystemId) : sysId);
                if (charStream != null) {
                    inputSource.setCharacterStream(charStream);
                } else if (byteStream != null) {
                    inputSource.setByteStream(byteStream);
                } else if (data != null && data.length() != 0) {
                    inputSource.setCharacterStream(new StringReader(data));
                }
                inputSource.setEncoding(encoding);
                return inputSource;
            }
            return null;
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            return resolveEntity(null, publicId, null, systemId);
        }

        private String resolveSystemId(String systemId, String baseURI) {
            try {
                return XMLEntityManager.expandSystemId(systemId, baseURI, false);
            } catch (URI.MalformedURIException e) {
                return systemId;
            }
        }
    }
}
