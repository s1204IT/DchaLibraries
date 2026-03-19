package mf.org.apache.xerces.jaxp;

import java.io.IOException;
import mf.javax.xml.validation.TypeInfoProvider;
import mf.javax.xml.validation.ValidatorHandler;
import mf.org.apache.xerces.dom.DOMInputImpl;
import mf.org.apache.xerces.impl.XMLErrorReporter;
import mf.org.apache.xerces.impl.xs.opti.DefaultXMLDocumentHandler;
import mf.org.apache.xerces.util.AttributesProxy;
import mf.org.apache.xerces.util.AugmentationsImpl;
import mf.org.apache.xerces.util.ErrorHandlerProxy;
import mf.org.apache.xerces.util.ErrorHandlerWrapper;
import mf.org.apache.xerces.util.LocatorProxy;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.XMLResourceIdentifierImpl;
import mf.org.apache.xerces.xni.Augmentations;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLAttributes;
import mf.org.apache.xerces.xni.XMLDocumentHandler;
import mf.org.apache.xerces.xni.XMLLocator;
import mf.org.apache.xerces.xni.XMLString;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.parser.XMLComponent;
import mf.org.apache.xerces.xni.parser.XMLComponentManager;
import mf.org.apache.xerces.xni.parser.XMLConfigurationException;
import mf.org.apache.xerces.xni.parser.XMLEntityResolver;
import mf.org.apache.xerces.xni.parser.XMLErrorHandler;
import mf.org.apache.xerces.xni.parser.XMLInputSource;
import mf.org.w3c.dom.TypeInfo;
import mf.org.w3c.dom.ls.LSInput;
import mf.org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

final class JAXPValidatorComponent extends TeeXMLDocumentFilterImpl implements XMLComponent {
    private static final String ENTITY_MANAGER = "http://apache.org/xml/properties/internal/entity-manager";
    private static final String ERROR_REPORTER = "http://apache.org/xml/properties/internal/error-reporter";
    private static final String SYMBOL_TABLE = "http://apache.org/xml/properties/internal/symbol-table";
    private static final TypeInfoProvider noInfoProvider = new TypeInfoProvider() {
        @Override
        public TypeInfo getElementTypeInfo() {
            return null;
        }

        @Override
        public TypeInfo getAttributeTypeInfo(int index) {
            return null;
        }

        public TypeInfo getAttributeTypeInfo(String attributeQName) {
            return null;
        }

        public TypeInfo getAttributeTypeInfo(String attributeUri, String attributeLocalName) {
            return null;
        }

        @Override
        public boolean isIdAttribute(int index) {
            return false;
        }

        @Override
        public boolean isSpecified(int index) {
            return false;
        }
    };
    private XMLAttributes fCurrentAttributes;
    private Augmentations fCurrentAug;
    private XMLEntityResolver fEntityResolver;
    private XMLErrorReporter fErrorReporter;
    private SymbolTable fSymbolTable;
    private final TypeInfoProvider typeInfoProvider;
    private final ValidatorHandler validator;
    private final XNI2SAX xni2sax = new XNI2SAX(null);
    private final SAX2XNI sax2xni = new SAX2XNI(this, 0 == true ? 1 : 0);

    public JAXPValidatorComponent(ValidatorHandler validatorHandler) {
        this.validator = validatorHandler;
        TypeInfoProvider typeInfoProvider = validatorHandler.getTypeInfoProvider();
        this.typeInfoProvider = typeInfoProvider == null ? noInfoProvider : typeInfoProvider;
        this.xni2sax.setContentHandler(this.validator);
        this.validator.setContentHandler(this.sax2xni);
        setSide(this.xni2sax);
        this.validator.setErrorHandler(new ErrorHandlerProxy() {
            @Override
            protected XMLErrorHandler getErrorHandler() {
                XMLErrorHandler handler = JAXPValidatorComponent.this.fErrorReporter.getErrorHandler();
                return handler != null ? handler : new ErrorHandlerWrapper(DraconianErrorHandler.getInstance());
            }
        });
        this.validator.setResourceResolver(new LSResourceResolver() {
            @Override
            public LSInput resolveResource(String type, String ns, String publicId, String systemId, String baseUri) {
                if (JAXPValidatorComponent.this.fEntityResolver == null) {
                    return null;
                }
                try {
                    XMLInputSource is = JAXPValidatorComponent.this.fEntityResolver.resolveEntity(new XMLResourceIdentifierImpl(publicId, systemId, baseUri, null));
                    if (is == null) {
                        return null;
                    }
                    LSInput di = new DOMInputImpl();
                    di.setBaseURI(is.getBaseSystemId());
                    di.setByteStream(is.getByteStream());
                    di.setCharacterStream(is.getCharacterStream());
                    di.setEncoding(is.getEncoding());
                    di.setPublicId(is.getPublicId());
                    di.setSystemId(is.getSystemId());
                    return di;
                } catch (IOException e) {
                    throw new XNIException(e);
                }
            }
        });
    }

    @Override
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        this.fCurrentAttributes = attributes;
        this.fCurrentAug = augs;
        this.xni2sax.startElement(element, attributes, null);
        this.fCurrentAttributes = null;
    }

    @Override
    public void endElement(QName element, Augmentations augs) throws XNIException {
        this.fCurrentAug = augs;
        this.xni2sax.endElement(element, null);
    }

    @Override
    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        startElement(element, attributes, augs);
        endElement(element, augs);
    }

    @Override
    public void characters(XMLString text, Augmentations augs) throws XNIException {
        this.fCurrentAug = augs;
        this.xni2sax.characters(text, null);
    }

    @Override
    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
        this.fCurrentAug = augs;
        this.xni2sax.ignorableWhitespace(text, null);
    }

    @Override
    public void reset(XMLComponentManager componentManager) throws XMLConfigurationException {
        this.fSymbolTable = (SymbolTable) componentManager.getProperty("http://apache.org/xml/properties/internal/symbol-table");
        this.fErrorReporter = (XMLErrorReporter) componentManager.getProperty("http://apache.org/xml/properties/internal/error-reporter");
        try {
            this.fEntityResolver = (XMLEntityResolver) componentManager.getProperty(ENTITY_MANAGER);
        } catch (XMLConfigurationException e) {
            this.fEntityResolver = null;
        }
    }

    private final class SAX2XNI extends DefaultHandler {
        private final Augmentations fAugmentations;
        private final QName fQName;

        private SAX2XNI() {
            this.fAugmentations = new AugmentationsImpl();
            this.fQName = new QName();
        }

        SAX2XNI(JAXPValidatorComponent jAXPValidatorComponent, SAX2XNI sax2xni) {
            this();
        }

        @Override
        public void characters(char[] ch, int start, int len) throws SAXException {
            try {
                handler().characters(new XMLString(ch, start, len), aug());
            } catch (XNIException e) {
                throw toSAXException(e);
            }
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int len) throws SAXException {
            try {
                handler().ignorableWhitespace(new XMLString(ch, start, len), aug());
            } catch (XNIException e) {
                throw toSAXException(e);
            }
        }

        @Override
        public void startElement(String uri, String localName, String qname, Attributes atts) throws SAXException {
            try {
                JAXPValidatorComponent.this.updateAttributes(atts);
                handler().startElement(toQName(uri, localName, qname), JAXPValidatorComponent.this.fCurrentAttributes, elementAug());
            } catch (XNIException e) {
                throw toSAXException(e);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qname) throws SAXException {
            try {
                handler().endElement(toQName(uri, localName, qname), aug());
            } catch (XNIException e) {
                throw toSAXException(e);
            }
        }

        private Augmentations elementAug() {
            Augmentations aug = aug();
            return aug;
        }

        private Augmentations aug() {
            if (JAXPValidatorComponent.this.fCurrentAug != null) {
                Augmentations r = JAXPValidatorComponent.this.fCurrentAug;
                JAXPValidatorComponent.this.fCurrentAug = null;
                return r;
            }
            Augmentations r2 = this.fAugmentations;
            r2.removeAllItems();
            return this.fAugmentations;
        }

        private XMLDocumentHandler handler() {
            return JAXPValidatorComponent.this.getDocumentHandler();
        }

        private SAXException toSAXException(XNIException xe) {
            Exception e = xe.getException();
            if (e == null) {
                e = xe;
            }
            return e instanceof SAXException ? e : new SAXException(e);
        }

        private QName toQName(String uri, String localName, String qname) {
            String prefix = null;
            int idx = qname.indexOf(58);
            if (idx > 0) {
                prefix = JAXPValidatorComponent.this.symbolize(qname.substring(0, idx));
            }
            this.fQName.setValues(prefix, JAXPValidatorComponent.this.symbolize(localName), JAXPValidatorComponent.this.symbolize(qname), JAXPValidatorComponent.this.symbolize(uri));
            return this.fQName;
        }
    }

    private static final class XNI2SAX extends DefaultXMLDocumentHandler {
        private final AttributesProxy fAttributesProxy;
        private ContentHandler fContentHandler;
        protected NamespaceContext fNamespaceContext;
        private String fVersion;

        private XNI2SAX() {
            this.fAttributesProxy = new AttributesProxy(null);
        }

        XNI2SAX(XNI2SAX xni2sax) {
            this();
        }

        public void setContentHandler(ContentHandler handler) {
            this.fContentHandler = handler;
        }

        public ContentHandler getContentHandler() {
            return this.fContentHandler;
        }

        @Override
        public void xmlDecl(String version, String encoding, String standalone, Augmentations augs) throws XNIException {
            this.fVersion = version;
        }

        @Override
        public void startDocument(XMLLocator locator, String encoding, NamespaceContext namespaceContext, Augmentations augs) throws XNIException {
            this.fNamespaceContext = namespaceContext;
            this.fContentHandler.setDocumentLocator(new LocatorProxy(locator));
            try {
                this.fContentHandler.startDocument();
            } catch (SAXException e) {
                throw new XNIException(e);
            }
        }

        @Override
        public void endDocument(Augmentations augs) throws XNIException {
            try {
                this.fContentHandler.endDocument();
            } catch (SAXException e) {
                throw new XNIException(e);
            }
        }

        @Override
        public void processingInstruction(String target, XMLString data, Augmentations augs) throws XNIException {
            try {
                this.fContentHandler.processingInstruction(target, data.toString());
            } catch (SAXException e) {
                throw new XNIException(e);
            }
        }

        @Override
        public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
            try {
                int count = this.fNamespaceContext.getDeclaredPrefixCount();
                if (count > 0) {
                    for (int i = 0; i < count; i++) {
                        String prefix = this.fNamespaceContext.getDeclaredPrefixAt(i);
                        String uri = this.fNamespaceContext.getURI(prefix);
                        this.fContentHandler.startPrefixMapping(prefix, uri == null ? "" : uri);
                    }
                }
                String uri2 = element.uri != null ? element.uri : "";
                String localpart = element.localpart;
                this.fAttributesProxy.setAttributes(attributes);
                this.fContentHandler.startElement(uri2, localpart, element.rawname, this.fAttributesProxy);
            } catch (SAXException e) {
                throw new XNIException(e);
            }
        }

        @Override
        public void endElement(QName element, Augmentations augs) throws XNIException {
            try {
                String uri = element.uri != null ? element.uri : "";
                String localpart = element.localpart;
                this.fContentHandler.endElement(uri, localpart, element.rawname);
                int count = this.fNamespaceContext.getDeclaredPrefixCount();
                if (count > 0) {
                    for (int i = 0; i < count; i++) {
                        this.fContentHandler.endPrefixMapping(this.fNamespaceContext.getDeclaredPrefixAt(i));
                    }
                }
            } catch (SAXException e) {
                throw new XNIException(e);
            }
        }

        @Override
        public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
            startElement(element, attributes, augs);
            endElement(element, augs);
        }

        @Override
        public void characters(XMLString text, Augmentations augs) throws XNIException {
            try {
                this.fContentHandler.characters(text.ch, text.offset, text.length);
            } catch (SAXException e) {
                throw new XNIException(e);
            }
        }

        @Override
        public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
            try {
                this.fContentHandler.ignorableWhitespace(text.ch, text.offset, text.length);
            } catch (SAXException e) {
                throw new XNIException(e);
            }
        }
    }

    private static final class DraconianErrorHandler implements ErrorHandler {
        private static final DraconianErrorHandler ERROR_HANDLER_INSTANCE = new DraconianErrorHandler();

        private DraconianErrorHandler() {
        }

        public static DraconianErrorHandler getInstance() {
            return ERROR_HANDLER_INSTANCE;
        }

        @Override
        public void warning(SAXParseException e) throws SAXException {
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            throw e;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            throw e;
        }
    }

    private void updateAttributes(Attributes atts) {
        String prefix;
        int len = atts.getLength();
        for (int i = 0; i < len; i++) {
            String aqn = atts.getQName(i);
            int j = this.fCurrentAttributes.getIndex(aqn);
            String av = atts.getValue(i);
            if (j == -1) {
                int idx = aqn.indexOf(58);
                if (idx < 0) {
                    prefix = null;
                } else {
                    prefix = symbolize(aqn.substring(0, idx));
                }
                this.fCurrentAttributes.addAttribute(new QName(prefix, symbolize(atts.getLocalName(i)), symbolize(aqn), symbolize(atts.getURI(i))), atts.getType(i), av);
            } else if (!av.equals(this.fCurrentAttributes.getValue(j))) {
                this.fCurrentAttributes.setValue(j, av);
            }
        }
    }

    private String symbolize(String s) {
        return this.fSymbolTable.addSymbol(s);
    }

    @Override
    public String[] getRecognizedFeatures() {
        return null;
    }

    @Override
    public void setFeature(String featureId, boolean state) throws XMLConfigurationException {
    }

    @Override
    public String[] getRecognizedProperties() {
        return new String[]{ENTITY_MANAGER, "http://apache.org/xml/properties/internal/error-reporter", "http://apache.org/xml/properties/internal/symbol-table"};
    }

    @Override
    public void setProperty(String propertyId, Object value) throws XMLConfigurationException {
    }

    @Override
    public Boolean getFeatureDefault(String featureId) {
        return null;
    }

    @Override
    public Object getPropertyDefault(String propertyId) {
        return null;
    }
}
