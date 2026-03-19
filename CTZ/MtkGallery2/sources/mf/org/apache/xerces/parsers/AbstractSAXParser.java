package mf.org.apache.xerces.parsers;

import java.io.CharConversionException;
import java.io.IOException;
import java.util.Locale;
import mf.org.apache.xerces.impl.Constants;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.util.EntityResolver2Wrapper;
import mf.org.apache.xerces.util.EntityResolverWrapper;
import mf.org.apache.xerces.util.ErrorHandlerWrapper;
import mf.org.apache.xerces.util.SAXMessageFormatter;
import mf.org.apache.xerces.util.SymbolHash;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xni.Augmentations;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLAttributes;
import mf.org.apache.xerces.xni.XMLLocator;
import mf.org.apache.xerces.xni.XMLResourceIdentifier;
import mf.org.apache.xerces.xni.XMLString;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.parser.XMLConfigurationException;
import mf.org.apache.xerces.xni.parser.XMLEntityResolver;
import mf.org.apache.xerces.xni.parser.XMLErrorHandler;
import mf.org.apache.xerces.xni.parser.XMLInputSource;
import mf.org.apache.xerces.xni.parser.XMLParseException;
import mf.org.apache.xerces.xni.parser.XMLParserConfiguration;
import mf.org.apache.xerces.xs.AttributePSVI;
import mf.org.apache.xerces.xs.ElementPSVI;
import mf.org.apache.xerces.xs.PSVIProvider;
import org.xml.sax.AttributeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.DocumentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.Attributes2;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.EntityResolver2;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.ext.Locator2;
import org.xml.sax.ext.Locator2Impl;

public abstract class AbstractSAXParser extends AbstractXMLDocumentParser implements PSVIProvider, Parser, XMLReader {
    protected static final String ALLOW_UE_AND_NOTATION_EVENTS = "http://xml.org/sax/features/allow-dtd-events-after-endDTD";
    private final AttributesProxy fAttributesProxy;
    private Augmentations fAugmentations;
    protected ContentHandler fContentHandler;
    protected DTDHandler fDTDHandler;
    protected DeclHandler fDeclHandler;
    protected SymbolHash fDeclaredAttrs;
    protected DocumentHandler fDocumentHandler;
    protected LexicalHandler fLexicalHandler;
    protected boolean fLexicalHandlerParameterEntities;
    protected NamespaceContext fNamespaceContext;
    protected boolean fNamespacePrefixes;
    protected boolean fNamespaces;
    protected boolean fParseInProgress;
    protected final QName fQName;
    protected boolean fResolveDTDURIs;
    protected boolean fStandalone;
    protected boolean fUseEntityResolver2;
    protected String fVersion;
    protected boolean fXMLNSURIs;
    protected static final String NAMESPACES = "http://xml.org/sax/features/namespaces";
    protected static final String STRING_INTERNING = "http://xml.org/sax/features/string-interning";
    private static final String[] RECOGNIZED_FEATURES = {NAMESPACES, STRING_INTERNING};
    protected static final String LEXICAL_HANDLER = "http://xml.org/sax/properties/lexical-handler";
    protected static final String DECLARATION_HANDLER = "http://xml.org/sax/properties/declaration-handler";
    protected static final String DOM_NODE = "http://xml.org/sax/properties/dom-node";
    private static final String[] RECOGNIZED_PROPERTIES = {LEXICAL_HANDLER, DECLARATION_HANDLER, DOM_NODE};

    protected AbstractSAXParser(XMLParserConfiguration config) {
        super(config);
        this.fNamespacePrefixes = false;
        this.fLexicalHandlerParameterEntities = true;
        this.fResolveDTDURIs = true;
        this.fUseEntityResolver2 = true;
        this.fXMLNSURIs = false;
        this.fQName = new QName();
        this.fParseInProgress = false;
        this.fAttributesProxy = new AttributesProxy();
        this.fAugmentations = null;
        this.fDeclaredAttrs = null;
        config.addRecognizedFeatures(RECOGNIZED_FEATURES);
        config.addRecognizedProperties(RECOGNIZED_PROPERTIES);
        try {
            config.setFeature(ALLOW_UE_AND_NOTATION_EVENTS, false);
        } catch (XMLConfigurationException e) {
        }
    }

    @Override
    public void startDocument(XMLLocator locator, String encoding, NamespaceContext namespaceContext, Augmentations augs) throws XNIException {
        this.fNamespaceContext = namespaceContext;
        try {
            if (this.fDocumentHandler != null) {
                if (locator != null) {
                    this.fDocumentHandler.setDocumentLocator(new LocatorProxy(locator));
                }
                if (this.fDocumentHandler != null) {
                    this.fDocumentHandler.startDocument();
                }
            }
            if (this.fContentHandler != null) {
                if (locator != null) {
                    this.fContentHandler.setDocumentLocator(new LocatorProxy(locator));
                }
                if (this.fContentHandler != null) {
                    this.fContentHandler.startDocument();
                }
            }
        } catch (SAXException e) {
            throw new XNIException(e);
        }
    }

    @Override
    public void xmlDecl(String version, String encoding, String standalone, Augmentations augs) throws XNIException {
        this.fVersion = version;
        this.fStandalone = "yes".equals(standalone);
    }

    @Override
    public void doctypeDecl(String rootElement, String publicId, String systemId, Augmentations augs) throws XNIException {
        this.fInDTD = true;
        try {
            if (this.fLexicalHandler != null) {
                this.fLexicalHandler.startDTD(rootElement, publicId, systemId);
            }
            if (this.fDeclHandler != null) {
                this.fDeclaredAttrs = new SymbolHash();
            }
        } catch (SAXException e) {
            throw new XNIException(e);
        }
    }

    @Override
    public void startGeneralEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs) throws XNIException {
        if (augs != null) {
            try {
                if (Boolean.TRUE.equals(augs.getItem(Constants.ENTITY_SKIPPED))) {
                    if (this.fContentHandler != null) {
                        this.fContentHandler.skippedEntity(name);
                    }
                } else if (this.fLexicalHandler != null) {
                    this.fLexicalHandler.startEntity(name);
                }
            } catch (SAXException e) {
                throw new XNIException(e);
            }
        }
    }

    @Override
    public void endGeneralEntity(String name, Augmentations augs) throws XNIException {
        if (augs != null) {
            try {
                if (Boolean.TRUE.equals(augs.getItem(Constants.ENTITY_SKIPPED))) {
                    return;
                }
            } catch (SAXException e) {
                throw new XNIException(e);
            }
        }
        if (this.fLexicalHandler != null) {
            this.fLexicalHandler.endEntity(name);
        }
    }

    @Override
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        try {
            if (this.fDocumentHandler != null) {
                this.fAttributesProxy.setAttributes(attributes);
                this.fDocumentHandler.startElement(element.rawname, this.fAttributesProxy);
            }
            if (this.fContentHandler != null) {
                if (this.fNamespaces) {
                    startNamespaceMapping();
                    int len = attributes.getLength();
                    if (!this.fNamespacePrefixes) {
                        for (int i = len - 1; i >= 0; i--) {
                            attributes.getName(i, this.fQName);
                            if (this.fQName.prefix == XMLSymbols.PREFIX_XMLNS || this.fQName.rawname == XMLSymbols.PREFIX_XMLNS) {
                                attributes.removeAttributeAt(i);
                            }
                        }
                    } else if (!this.fXMLNSURIs) {
                        for (int i2 = len - 1; i2 >= 0; i2--) {
                            attributes.getName(i2, this.fQName);
                            if (this.fQName.prefix == XMLSymbols.PREFIX_XMLNS || this.fQName.rawname == XMLSymbols.PREFIX_XMLNS) {
                                this.fQName.prefix = "";
                                this.fQName.uri = "";
                                this.fQName.localpart = "";
                                attributes.setName(i2, this.fQName);
                            }
                        }
                    }
                }
                this.fAugmentations = augs;
                String uri = element.uri != null ? element.uri : "";
                String localpart = this.fNamespaces ? element.localpart : "";
                this.fAttributesProxy.setAttributes(attributes);
                this.fContentHandler.startElement(uri, localpart, element.rawname, this.fAttributesProxy);
            }
        } catch (SAXException e) {
            throw new XNIException(e);
        }
    }

    @Override
    public void characters(XMLString text, Augmentations augs) throws XNIException {
        if (text.length == 0) {
            return;
        }
        try {
            if (this.fDocumentHandler != null) {
                this.fDocumentHandler.characters(text.ch, text.offset, text.length);
            }
            if (this.fContentHandler != null) {
                this.fContentHandler.characters(text.ch, text.offset, text.length);
            }
        } catch (SAXException e) {
            throw new XNIException(e);
        }
    }

    @Override
    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
        try {
            if (this.fDocumentHandler != null) {
                this.fDocumentHandler.ignorableWhitespace(text.ch, text.offset, text.length);
            }
            if (this.fContentHandler != null) {
                this.fContentHandler.ignorableWhitespace(text.ch, text.offset, text.length);
            }
        } catch (SAXException e) {
            throw new XNIException(e);
        }
    }

    @Override
    public void endElement(QName element, Augmentations augs) throws XNIException {
        try {
            if (this.fDocumentHandler != null) {
                this.fDocumentHandler.endElement(element.rawname);
            }
            if (this.fContentHandler != null) {
                this.fAugmentations = augs;
                String uri = element.uri != null ? element.uri : "";
                String localpart = this.fNamespaces ? element.localpart : "";
                this.fContentHandler.endElement(uri, localpart, element.rawname);
                if (this.fNamespaces) {
                    endNamespaceMapping();
                }
            }
        } catch (SAXException e) {
            throw new XNIException(e);
        }
    }

    @Override
    public void startCDATA(Augmentations augs) throws XNIException {
        try {
            if (this.fLexicalHandler != null) {
                this.fLexicalHandler.startCDATA();
            }
        } catch (SAXException e) {
            throw new XNIException(e);
        }
    }

    @Override
    public void endCDATA(Augmentations augs) throws XNIException {
        try {
            if (this.fLexicalHandler != null) {
                this.fLexicalHandler.endCDATA();
            }
        } catch (SAXException e) {
            throw new XNIException(e);
        }
    }

    @Override
    public void comment(XMLString text, Augmentations augs) throws XNIException {
        try {
            if (this.fLexicalHandler != null) {
                this.fLexicalHandler.comment(text.ch, 0, text.length);
            }
        } catch (SAXException e) {
            throw new XNIException(e);
        }
    }

    @Override
    public void processingInstruction(String target, XMLString data, Augmentations augs) throws XNIException {
        try {
            if (this.fDocumentHandler != null) {
                this.fDocumentHandler.processingInstruction(target, data.toString());
            }
            if (this.fContentHandler != null) {
                this.fContentHandler.processingInstruction(target, data.toString());
            }
        } catch (SAXException e) {
            throw new XNIException(e);
        }
    }

    @Override
    public void endDocument(Augmentations augs) throws XNIException {
        try {
            if (this.fDocumentHandler != null) {
                this.fDocumentHandler.endDocument();
            }
            if (this.fContentHandler != null) {
                this.fContentHandler.endDocument();
            }
        } catch (SAXException e) {
            throw new XNIException(e);
        }
    }

    @Override
    public void startExternalSubset(XMLResourceIdentifier identifier, Augmentations augs) throws XNIException {
        startParameterEntity("[dtd]", null, null, augs);
    }

    @Override
    public void endExternalSubset(Augmentations augs) throws XNIException {
        endParameterEntity("[dtd]", augs);
    }

    @Override
    public void startParameterEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs) throws XNIException {
        if (augs != null) {
            try {
                if (Boolean.TRUE.equals(augs.getItem(Constants.ENTITY_SKIPPED))) {
                    if (this.fContentHandler != null) {
                        this.fContentHandler.skippedEntity(name);
                    }
                } else if (this.fLexicalHandler != null && this.fLexicalHandlerParameterEntities) {
                    this.fLexicalHandler.startEntity(name);
                }
            } catch (SAXException e) {
                throw new XNIException(e);
            }
        }
    }

    @Override
    public void endParameterEntity(String name, Augmentations augs) throws XNIException {
        if (augs != null) {
            try {
                if (Boolean.TRUE.equals(augs.getItem(Constants.ENTITY_SKIPPED))) {
                    return;
                }
            } catch (SAXException e) {
                throw new XNIException(e);
            }
        }
        if (this.fLexicalHandler != null && this.fLexicalHandlerParameterEntities) {
            this.fLexicalHandler.endEntity(name);
        }
    }

    @Override
    public void elementDecl(String name, String contentModel, Augmentations augs) throws XNIException {
        try {
            if (this.fDeclHandler != null) {
                this.fDeclHandler.elementDecl(name, contentModel);
            }
        } catch (SAXException e) {
            throw new XNIException(e);
        }
    }

    @Override
    public void attributeDecl(String elementName, String attributeName, String type, String[] enumeration, String defaultType, XMLString defaultValue, XMLString nonNormalizedDefaultValue, Augmentations augs) throws XNIException {
        try {
            if (this.fDeclHandler != null) {
                StringBuffer stringBuffer = new StringBuffer(elementName);
                stringBuffer.append('<');
                stringBuffer.append(attributeName);
                String elemAttr = stringBuffer.toString();
                if (this.fDeclaredAttrs.get(elemAttr) != null) {
                    return;
                }
                this.fDeclaredAttrs.put(elemAttr, Boolean.TRUE);
                if (type.equals(SchemaSymbols.ATTVAL_NOTATION) || type.equals("ENUMERATION")) {
                    StringBuffer str = new StringBuffer();
                    if (type.equals(SchemaSymbols.ATTVAL_NOTATION)) {
                        str.append(type);
                        str.append(" (");
                    } else {
                        str.append('(');
                    }
                    for (int i = 0; i < enumeration.length; i++) {
                        str.append(enumeration[i]);
                        if (i < enumeration.length - 1) {
                            str.append('|');
                        }
                    }
                    str.append(')');
                    type = str.toString();
                }
                String value = defaultValue == null ? null : defaultValue.toString();
                this.fDeclHandler.attributeDecl(elementName, attributeName, type, defaultType, value);
            }
        } catch (SAXException e) {
            throw new XNIException(e);
        }
    }

    @Override
    public void internalEntityDecl(String name, XMLString text, XMLString nonNormalizedText, Augmentations augs) throws XNIException {
        try {
            if (this.fDeclHandler != null) {
                this.fDeclHandler.internalEntityDecl(name, text.toString());
            }
        } catch (SAXException e) {
            throw new XNIException(e);
        }
    }

    @Override
    public void externalEntityDecl(String name, XMLResourceIdentifier identifier, Augmentations augs) throws XNIException {
        try {
            if (this.fDeclHandler != null) {
                String publicId = identifier.getPublicId();
                String systemId = this.fResolveDTDURIs ? identifier.getExpandedSystemId() : identifier.getLiteralSystemId();
                this.fDeclHandler.externalEntityDecl(name, publicId, systemId);
            }
        } catch (SAXException e) {
            throw new XNIException(e);
        }
    }

    @Override
    public void unparsedEntityDecl(String name, XMLResourceIdentifier identifier, String notation, Augmentations augs) throws XNIException {
        try {
            if (this.fDTDHandler != null) {
                String publicId = identifier.getPublicId();
                String systemId = this.fResolveDTDURIs ? identifier.getExpandedSystemId() : identifier.getLiteralSystemId();
                this.fDTDHandler.unparsedEntityDecl(name, publicId, systemId, notation);
            }
        } catch (SAXException e) {
            throw new XNIException(e);
        }
    }

    @Override
    public void notationDecl(String name, XMLResourceIdentifier identifier, Augmentations augs) throws XNIException {
        try {
            if (this.fDTDHandler != null) {
                String publicId = identifier.getPublicId();
                String systemId = this.fResolveDTDURIs ? identifier.getExpandedSystemId() : identifier.getLiteralSystemId();
                this.fDTDHandler.notationDecl(name, publicId, systemId);
            }
        } catch (SAXException e) {
            throw new XNIException(e);
        }
    }

    @Override
    public void endDTD(Augmentations augs) throws XNIException {
        this.fInDTD = false;
        try {
            if (this.fLexicalHandler != null) {
                this.fLexicalHandler.endDTD();
            }
            if (this.fDeclaredAttrs != null) {
                this.fDeclaredAttrs.clear();
            }
        } catch (SAXException e) {
            throw new XNIException(e);
        }
    }

    public void parse(String systemId) throws Exception {
        XMLInputSource source = new XMLInputSource(null, systemId, null);
        try {
            parse(source);
        } catch (XMLParseException e) {
            Exception ex = e.getException();
            if (ex == null || (ex instanceof CharConversionException)) {
                Locator2Impl locatorImpl = new Locator2Impl();
                locatorImpl.setXMLVersion(this.fVersion);
                locatorImpl.setPublicId(e.getPublicId());
                locatorImpl.setSystemId(e.getExpandedSystemId());
                locatorImpl.setLineNumber(e.getLineNumber());
                locatorImpl.setColumnNumber(e.getColumnNumber());
                if (ex == null) {
                    throw new SAXParseException(e.getMessage(), locatorImpl);
                }
                throw new SAXParseException(e.getMessage(), locatorImpl, ex);
            }
            if (ex instanceof SAXException) {
                throw ex;
            }
            if (ex instanceof IOException) {
                throw ex;
            }
            throw new SAXException(ex);
        } catch (XNIException e2) {
            Exception ex2 = e2.getException();
            if (ex2 == null) {
                throw new SAXException(e2.getMessage());
            }
            if (ex2 instanceof SAXException) {
                throw ex2;
            }
            if (ex2 instanceof IOException) {
                throw ex2;
            }
            throw new SAXException(ex2);
        }
    }

    public void parse(InputSource inputSource) throws Exception {
        try {
            XMLInputSource xmlInputSource = new XMLInputSource(inputSource.getPublicId(), inputSource.getSystemId(), null);
            xmlInputSource.setByteStream(inputSource.getByteStream());
            xmlInputSource.setCharacterStream(inputSource.getCharacterStream());
            xmlInputSource.setEncoding(inputSource.getEncoding());
            parse(xmlInputSource);
        } catch (XMLParseException e) {
            Exception ex = e.getException();
            if (ex == null || (ex instanceof CharConversionException)) {
                Locator2Impl locatorImpl = new Locator2Impl();
                locatorImpl.setXMLVersion(this.fVersion);
                locatorImpl.setPublicId(e.getPublicId());
                locatorImpl.setSystemId(e.getExpandedSystemId());
                locatorImpl.setLineNumber(e.getLineNumber());
                locatorImpl.setColumnNumber(e.getColumnNumber());
                if (ex == null) {
                    throw new SAXParseException(e.getMessage(), locatorImpl);
                }
                throw new SAXParseException(e.getMessage(), locatorImpl, ex);
            }
            if (ex instanceof SAXException) {
                throw ex;
            }
            if (ex instanceof IOException) {
                throw ex;
            }
            throw new SAXException(ex);
        } catch (XNIException e2) {
            Exception ex2 = e2.getException();
            if (ex2 == null) {
                throw new SAXException(e2.getMessage());
            }
            if (ex2 instanceof SAXException) {
                throw ex2;
            }
            if (ex2 instanceof IOException) {
                throw ex2;
            }
            throw new SAXException(ex2);
        }
    }

    @Override
    public void setEntityResolver(EntityResolver resolver) {
        try {
            ?? r0 = (XMLEntityResolver) this.fConfiguration.getProperty("http://apache.org/xml/properties/internal/entity-resolver");
            if (this.fUseEntityResolver2 && (resolver instanceof EntityResolver2)) {
                if (r0 instanceof EntityResolver2Wrapper) {
                    r0.setEntityResolver((EntityResolver2) resolver);
                } else {
                    this.fConfiguration.setProperty("http://apache.org/xml/properties/internal/entity-resolver", new EntityResolver2Wrapper((EntityResolver2) resolver));
                }
            } else if (r0 instanceof EntityResolverWrapper) {
                r0.setEntityResolver(resolver);
            } else {
                this.fConfiguration.setProperty("http://apache.org/xml/properties/internal/entity-resolver", new EntityResolverWrapper(resolver));
            }
        } catch (XMLConfigurationException e) {
        }
    }

    @Override
    public EntityResolver getEntityResolver() {
        EntityResolver entityResolver = null;
        try {
            ?? r1 = (XMLEntityResolver) this.fConfiguration.getProperty("http://apache.org/xml/properties/internal/entity-resolver");
            if (r1 != 0) {
                if (r1 instanceof EntityResolverWrapper) {
                    entityResolver = r1.getEntityResolver();
                } else if (r1 instanceof EntityResolver2Wrapper) {
                    entityResolver = r1.getEntityResolver();
                }
            }
        } catch (XMLConfigurationException e) {
        }
        return entityResolver;
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        try {
            ?? r0 = (XMLErrorHandler) this.fConfiguration.getProperty("http://apache.org/xml/properties/internal/error-handler");
            if (r0 instanceof ErrorHandlerWrapper) {
                r0.setErrorHandler(errorHandler);
            } else {
                this.fConfiguration.setProperty("http://apache.org/xml/properties/internal/error-handler", new ErrorHandlerWrapper(errorHandler));
            }
        } catch (XMLConfigurationException e) {
        }
    }

    @Override
    public ErrorHandler getErrorHandler() {
        try {
            ?? r1 = (XMLErrorHandler) this.fConfiguration.getProperty("http://apache.org/xml/properties/internal/error-handler");
            if (r1 == 0 || !(r1 instanceof ErrorHandlerWrapper)) {
                return null;
            }
            ErrorHandler errorHandler = r1.getErrorHandler();
            return errorHandler;
        } catch (XMLConfigurationException e) {
            return null;
        }
    }

    @Override
    public void setLocale(Locale locale) throws SAXException {
        this.fConfiguration.setLocale(locale);
    }

    @Override
    public void setDTDHandler(DTDHandler dtdHandler) {
        this.fDTDHandler = dtdHandler;
    }

    @Override
    public void setDocumentHandler(DocumentHandler documentHandler) {
        this.fDocumentHandler = documentHandler;
    }

    @Override
    public void setContentHandler(ContentHandler contentHandler) {
        this.fContentHandler = contentHandler;
    }

    @Override
    public ContentHandler getContentHandler() {
        return this.fContentHandler;
    }

    @Override
    public DTDHandler getDTDHandler() {
        return this.fDTDHandler;
    }

    public void setFeature(String featureId, boolean state) throws SAXNotRecognizedException, SAXNotSupportedException {
        try {
            if (featureId.startsWith(Constants.SAX_FEATURE_PREFIX)) {
                int suffixLength = featureId.length() - Constants.SAX_FEATURE_PREFIX.length();
                if (suffixLength == "namespaces".length() && featureId.endsWith("namespaces")) {
                    this.fConfiguration.setFeature(featureId, state);
                    this.fNamespaces = state;
                    return;
                }
                if (suffixLength == Constants.NAMESPACE_PREFIXES_FEATURE.length() && featureId.endsWith(Constants.NAMESPACE_PREFIXES_FEATURE)) {
                    this.fNamespacePrefixes = state;
                    return;
                }
                if (suffixLength == Constants.STRING_INTERNING_FEATURE.length() && featureId.endsWith(Constants.STRING_INTERNING_FEATURE)) {
                    if (!state) {
                        throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "false-not-supported", new Object[]{featureId}));
                    }
                    return;
                }
                if (suffixLength == Constants.LEXICAL_HANDLER_PARAMETER_ENTITIES_FEATURE.length() && featureId.endsWith(Constants.LEXICAL_HANDLER_PARAMETER_ENTITIES_FEATURE)) {
                    this.fLexicalHandlerParameterEntities = state;
                    return;
                }
                if (suffixLength == Constants.RESOLVE_DTD_URIS_FEATURE.length() && featureId.endsWith(Constants.RESOLVE_DTD_URIS_FEATURE)) {
                    this.fResolveDTDURIs = state;
                    return;
                }
                if (suffixLength == Constants.UNICODE_NORMALIZATION_CHECKING_FEATURE.length() && featureId.endsWith(Constants.UNICODE_NORMALIZATION_CHECKING_FEATURE)) {
                    if (state) {
                        throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "true-not-supported", new Object[]{featureId}));
                    }
                    return;
                }
                if (suffixLength == Constants.XMLNS_URIS_FEATURE.length() && featureId.endsWith(Constants.XMLNS_URIS_FEATURE)) {
                    this.fXMLNSURIs = state;
                    return;
                }
                if (suffixLength == Constants.USE_ENTITY_RESOLVER2_FEATURE.length() && featureId.endsWith(Constants.USE_ENTITY_RESOLVER2_FEATURE)) {
                    if (state != this.fUseEntityResolver2) {
                        this.fUseEntityResolver2 = state;
                        setEntityResolver(getEntityResolver());
                        return;
                    }
                    return;
                }
                if ((suffixLength == Constants.IS_STANDALONE_FEATURE.length() && featureId.endsWith(Constants.IS_STANDALONE_FEATURE)) || ((suffixLength == Constants.USE_ATTRIBUTES2_FEATURE.length() && featureId.endsWith(Constants.USE_ATTRIBUTES2_FEATURE)) || ((suffixLength == Constants.USE_LOCATOR2_FEATURE.length() && featureId.endsWith(Constants.USE_LOCATOR2_FEATURE)) || (suffixLength == Constants.XML_11_FEATURE.length() && featureId.endsWith(Constants.XML_11_FEATURE))))) {
                    throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "feature-read-only", new Object[]{featureId}));
                }
            }
            this.fConfiguration.setFeature(featureId, state);
        } catch (XMLConfigurationException e) {
            String identifier = e.getIdentifier();
            if (e.getType() == 0) {
                throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "feature-not-recognized", new Object[]{identifier}));
            }
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "feature-not-supported", new Object[]{identifier}));
        }
    }

    public boolean getFeature(String featureId) throws SAXNotRecognizedException, SAXNotSupportedException {
        try {
            if (featureId.startsWith(Constants.SAX_FEATURE_PREFIX)) {
                int suffixLength = featureId.length() - Constants.SAX_FEATURE_PREFIX.length();
                if (suffixLength == Constants.NAMESPACE_PREFIXES_FEATURE.length() && featureId.endsWith(Constants.NAMESPACE_PREFIXES_FEATURE)) {
                    return this.fNamespacePrefixes;
                }
                if (suffixLength == Constants.STRING_INTERNING_FEATURE.length() && featureId.endsWith(Constants.STRING_INTERNING_FEATURE)) {
                    return true;
                }
                if (suffixLength == Constants.IS_STANDALONE_FEATURE.length() && featureId.endsWith(Constants.IS_STANDALONE_FEATURE)) {
                    return this.fStandalone;
                }
                if (suffixLength == Constants.XML_11_FEATURE.length() && featureId.endsWith(Constants.XML_11_FEATURE)) {
                    return this.fConfiguration instanceof XML11Configurable;
                }
                if (suffixLength == Constants.LEXICAL_HANDLER_PARAMETER_ENTITIES_FEATURE.length() && featureId.endsWith(Constants.LEXICAL_HANDLER_PARAMETER_ENTITIES_FEATURE)) {
                    return this.fLexicalHandlerParameterEntities;
                }
                if (suffixLength == Constants.RESOLVE_DTD_URIS_FEATURE.length() && featureId.endsWith(Constants.RESOLVE_DTD_URIS_FEATURE)) {
                    return this.fResolveDTDURIs;
                }
                if (suffixLength == Constants.XMLNS_URIS_FEATURE.length() && featureId.endsWith(Constants.XMLNS_URIS_FEATURE)) {
                    return this.fXMLNSURIs;
                }
                if (suffixLength == Constants.UNICODE_NORMALIZATION_CHECKING_FEATURE.length() && featureId.endsWith(Constants.UNICODE_NORMALIZATION_CHECKING_FEATURE)) {
                    return false;
                }
                if (suffixLength == Constants.USE_ENTITY_RESOLVER2_FEATURE.length() && featureId.endsWith(Constants.USE_ENTITY_RESOLVER2_FEATURE)) {
                    return this.fUseEntityResolver2;
                }
                if ((suffixLength == Constants.USE_ATTRIBUTES2_FEATURE.length() && featureId.endsWith(Constants.USE_ATTRIBUTES2_FEATURE)) || (suffixLength == Constants.USE_LOCATOR2_FEATURE.length() && featureId.endsWith(Constants.USE_LOCATOR2_FEATURE))) {
                    return true;
                }
            }
            return this.fConfiguration.getFeature(featureId);
        } catch (XMLConfigurationException e) {
            String identifier = e.getIdentifier();
            if (e.getType() == 0) {
                throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "feature-not-recognized", new Object[]{identifier}));
            }
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "feature-not-supported", new Object[]{identifier}));
        }
    }

    public void setProperty(String propertyId, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        try {
        } catch (XMLConfigurationException e) {
            String identifier = e.getIdentifier();
            if (e.getType() != 0) {
            }
        }
        if (propertyId.startsWith(Constants.SAX_PROPERTY_PREFIX)) {
            int suffixLength = propertyId.length() - Constants.SAX_PROPERTY_PREFIX.length();
            if (suffixLength == Constants.LEXICAL_HANDLER_PROPERTY.length() && propertyId.endsWith(Constants.LEXICAL_HANDLER_PROPERTY)) {
                try {
                    setLexicalHandler((LexicalHandler) value);
                    return;
                } catch (ClassCastException e2) {
                    throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "incompatible-class", new Object[]{propertyId, "org.xml.sax.ext.LexicalHandler"}));
                }
            } else if (suffixLength == Constants.DECLARATION_HANDLER_PROPERTY.length() && propertyId.endsWith(Constants.DECLARATION_HANDLER_PROPERTY)) {
                try {
                    setDeclHandler((DeclHandler) value);
                    return;
                } catch (ClassCastException e3) {
                    throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "incompatible-class", new Object[]{propertyId, "org.xml.sax.ext.DeclHandler"}));
                }
            } else if ((suffixLength == Constants.DOM_NODE_PROPERTY.length() && propertyId.endsWith(Constants.DOM_NODE_PROPERTY)) || (suffixLength == Constants.DOCUMENT_XML_VERSION_PROPERTY.length() && propertyId.endsWith(Constants.DOCUMENT_XML_VERSION_PROPERTY))) {
                throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "property-read-only", new Object[]{propertyId}));
            }
            String identifier2 = e.getIdentifier();
            if (e.getType() != 0) {
                throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "property-not-recognized", new Object[]{identifier2}));
            }
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "property-not-supported", new Object[]{identifier2}));
        }
        this.fConfiguration.setProperty(propertyId, value);
    }

    public Object getProperty(String propertyId) throws SAXNotRecognizedException, SAXNotSupportedException {
        try {
            if (propertyId.startsWith(Constants.SAX_PROPERTY_PREFIX)) {
                int suffixLength = propertyId.length() - Constants.SAX_PROPERTY_PREFIX.length();
                if (suffixLength == Constants.DOCUMENT_XML_VERSION_PROPERTY.length() && propertyId.endsWith(Constants.DOCUMENT_XML_VERSION_PROPERTY)) {
                    return this.fVersion;
                }
                if (suffixLength == Constants.LEXICAL_HANDLER_PROPERTY.length() && propertyId.endsWith(Constants.LEXICAL_HANDLER_PROPERTY)) {
                    return getLexicalHandler();
                }
                if (suffixLength == Constants.DECLARATION_HANDLER_PROPERTY.length() && propertyId.endsWith(Constants.DECLARATION_HANDLER_PROPERTY)) {
                    return getDeclHandler();
                }
                if (suffixLength == Constants.DOM_NODE_PROPERTY.length() && propertyId.endsWith(Constants.DOM_NODE_PROPERTY)) {
                    throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "dom-node-read-not-supported", null));
                }
            }
            return this.fConfiguration.getProperty(propertyId);
        } catch (XMLConfigurationException e) {
            String identifier = e.getIdentifier();
            if (e.getType() == 0) {
                throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "property-not-recognized", new Object[]{identifier}));
            }
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "property-not-supported", new Object[]{identifier}));
        }
    }

    protected void setDeclHandler(DeclHandler handler) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (this.fParseInProgress) {
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "property-not-parsing-supported", new Object[]{DECLARATION_HANDLER}));
        }
        this.fDeclHandler = handler;
    }

    protected DeclHandler getDeclHandler() throws SAXNotRecognizedException, SAXNotSupportedException {
        return this.fDeclHandler;
    }

    protected void setLexicalHandler(LexicalHandler handler) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (this.fParseInProgress) {
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "property-not-parsing-supported", new Object[]{LEXICAL_HANDLER}));
        }
        this.fLexicalHandler = handler;
    }

    protected LexicalHandler getLexicalHandler() throws SAXNotRecognizedException, SAXNotSupportedException {
        return this.fLexicalHandler;
    }

    protected final void startNamespaceMapping() throws SAXException {
        int count = this.fNamespaceContext.getDeclaredPrefixCount();
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                String prefix = this.fNamespaceContext.getDeclaredPrefixAt(i);
                String uri = this.fNamespaceContext.getURI(prefix);
                this.fContentHandler.startPrefixMapping(prefix, uri == null ? "" : uri);
            }
        }
    }

    protected final void endNamespaceMapping() throws SAXException {
        int count = this.fNamespaceContext.getDeclaredPrefixCount();
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                this.fContentHandler.endPrefixMapping(this.fNamespaceContext.getDeclaredPrefixAt(i));
            }
        }
    }

    @Override
    public void reset() throws XNIException {
        super.reset();
        this.fInDTD = false;
        this.fVersion = "1.0";
        this.fStandalone = false;
        this.fNamespaces = this.fConfiguration.getFeature(NAMESPACES);
        this.fAugmentations = null;
        this.fDeclaredAttrs = null;
    }

    protected static final class LocatorProxy implements Locator2 {
        protected XMLLocator fLocator;

        public LocatorProxy(XMLLocator locator) {
            this.fLocator = locator;
        }

        @Override
        public String getPublicId() {
            return this.fLocator.getPublicId();
        }

        @Override
        public String getSystemId() {
            return this.fLocator.getExpandedSystemId();
        }

        @Override
        public int getLineNumber() {
            return this.fLocator.getLineNumber();
        }

        @Override
        public int getColumnNumber() {
            return this.fLocator.getColumnNumber();
        }

        @Override
        public String getXMLVersion() {
            return this.fLocator.getXMLVersion();
        }

        @Override
        public String getEncoding() {
            return this.fLocator.getEncoding();
        }
    }

    protected static final class AttributesProxy implements AttributeList, Attributes2 {
        protected XMLAttributes fAttributes;

        protected AttributesProxy() {
        }

        public void setAttributes(XMLAttributes attributes) {
            this.fAttributes = attributes;
        }

        @Override
        public int getLength() {
            return this.fAttributes.getLength();
        }

        @Override
        public String getName(int i) {
            return this.fAttributes.getQName(i);
        }

        @Override
        public String getQName(int index) {
            return this.fAttributes.getQName(index);
        }

        @Override
        public String getURI(int index) {
            String uri = this.fAttributes.getURI(index);
            return uri != null ? uri : "";
        }

        @Override
        public String getLocalName(int index) {
            return this.fAttributes.getLocalName(index);
        }

        @Override
        public String getType(int i) {
            return this.fAttributes.getType(i);
        }

        @Override
        public String getType(String name) {
            return this.fAttributes.getType(name);
        }

        @Override
        public String getType(String uri, String localName) {
            return uri.length() == 0 ? this.fAttributes.getType(null, localName) : this.fAttributes.getType(uri, localName);
        }

        @Override
        public String getValue(int i) {
            return this.fAttributes.getValue(i);
        }

        @Override
        public String getValue(String name) {
            return this.fAttributes.getValue(name);
        }

        @Override
        public String getValue(String uri, String localName) {
            return uri.length() == 0 ? this.fAttributes.getValue(null, localName) : this.fAttributes.getValue(uri, localName);
        }

        @Override
        public int getIndex(String qName) {
            return this.fAttributes.getIndex(qName);
        }

        @Override
        public int getIndex(String uri, String localPart) {
            return uri.length() == 0 ? this.fAttributes.getIndex(null, localPart) : this.fAttributes.getIndex(uri, localPart);
        }

        @Override
        public boolean isDeclared(int index) {
            if (index < 0 || index >= this.fAttributes.getLength()) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            return Boolean.TRUE.equals(this.fAttributes.getAugmentations(index).getItem(Constants.ATTRIBUTE_DECLARED));
        }

        @Override
        public boolean isDeclared(String qName) {
            int index = getIndex(qName);
            if (index == -1) {
                throw new IllegalArgumentException(qName);
            }
            return Boolean.TRUE.equals(this.fAttributes.getAugmentations(index).getItem(Constants.ATTRIBUTE_DECLARED));
        }

        @Override
        public boolean isDeclared(String uri, String localName) {
            int index = getIndex(uri, localName);
            if (index == -1) {
                throw new IllegalArgumentException(localName);
            }
            return Boolean.TRUE.equals(this.fAttributes.getAugmentations(index).getItem(Constants.ATTRIBUTE_DECLARED));
        }

        @Override
        public boolean isSpecified(int index) {
            if (index < 0 || index >= this.fAttributes.getLength()) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            return this.fAttributes.isSpecified(index);
        }

        @Override
        public boolean isSpecified(String qName) {
            int index = getIndex(qName);
            if (index == -1) {
                throw new IllegalArgumentException(qName);
            }
            return this.fAttributes.isSpecified(index);
        }

        @Override
        public boolean isSpecified(String uri, String localName) {
            int index = getIndex(uri, localName);
            if (index == -1) {
                throw new IllegalArgumentException(localName);
            }
            return this.fAttributes.isSpecified(index);
        }
    }

    @Override
    public ElementPSVI getElementPSVI() {
        if (this.fAugmentations != null) {
            return (ElementPSVI) this.fAugmentations.getItem(Constants.ELEMENT_PSVI);
        }
        return null;
    }

    @Override
    public AttributePSVI getAttributePSVI(int index) {
        return (AttributePSVI) this.fAttributesProxy.fAttributes.getAugmentations(index).getItem(Constants.ATTRIBUTE_PSVI);
    }

    @Override
    public AttributePSVI getAttributePSVIByName(String uri, String localname) {
        return (AttributePSVI) this.fAttributesProxy.fAttributes.getAugmentations(uri, localname).getItem(Constants.ATTRIBUTE_PSVI);
    }
}
