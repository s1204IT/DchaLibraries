package mf.org.apache.xerces.parsers;

import com.mediatek.plugin.preload.SoOperater;
import java.util.Locale;
import java.util.Stack;
import mf.org.apache.xerces.dom.AttrImpl;
import mf.org.apache.xerces.dom.CoreDocumentImpl;
import mf.org.apache.xerces.dom.DOMErrorImpl;
import mf.org.apache.xerces.dom.DOMMessageFormatter;
import mf.org.apache.xerces.dom.DeferredDocumentImpl;
import mf.org.apache.xerces.dom.DocumentImpl;
import mf.org.apache.xerces.dom.DocumentTypeImpl;
import mf.org.apache.xerces.dom.ElementDefinitionImpl;
import mf.org.apache.xerces.dom.ElementImpl;
import mf.org.apache.xerces.dom.ElementNSImpl;
import mf.org.apache.xerces.dom.EntityImpl;
import mf.org.apache.xerces.dom.EntityReferenceImpl;
import mf.org.apache.xerces.dom.NodeImpl;
import mf.org.apache.xerces.dom.NotationImpl;
import mf.org.apache.xerces.dom.PSVIAttrNSImpl;
import mf.org.apache.xerces.dom.PSVIDocumentImpl;
import mf.org.apache.xerces.dom.PSVIElementNSImpl;
import mf.org.apache.xerces.dom.TextImpl;
import mf.org.apache.xerces.impl.Constants;
import mf.org.apache.xerces.impl.dv.XSSimpleType;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.util.DOMErrorHandlerWrapper;
import mf.org.apache.xerces.xni.Augmentations;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLAttributes;
import mf.org.apache.xerces.xni.XMLLocator;
import mf.org.apache.xerces.xni.XMLResourceIdentifier;
import mf.org.apache.xerces.xni.XMLString;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.parser.XMLParserConfiguration;
import mf.org.apache.xerces.xs.AttributePSVI;
import mf.org.apache.xerces.xs.ElementPSVI;
import mf.org.apache.xerces.xs.XSTypeDefinition;
import mf.org.w3c.dom.Attr;
import mf.org.w3c.dom.CDATASection;
import mf.org.w3c.dom.Comment;
import mf.org.w3c.dom.Document;
import mf.org.w3c.dom.DocumentType;
import mf.org.w3c.dom.Element;
import mf.org.w3c.dom.EntityReference;
import mf.org.w3c.dom.NamedNodeMap;
import mf.org.w3c.dom.Node;
import mf.org.w3c.dom.NodeList;
import mf.org.w3c.dom.ProcessingInstruction;
import mf.org.w3c.dom.Text;
import mf.org.w3c.dom.ls.LSParserFilter;

public class AbstractDOMParser extends AbstractXMLDocumentParser {
    protected static final String CORE_DOCUMENT_CLASS_NAME = "mf.org.apache.xerces.dom.CoreDocumentImpl";
    private static final boolean DEBUG_BASEURI = false;
    private static final boolean DEBUG_EVENTS = false;
    protected static final String DEFAULT_DOCUMENT_CLASS_NAME = "mf.org.apache.xerces.dom.DocumentImpl";
    protected static final String PSVI_DOCUMENT_CLASS_NAME = "mf.org.apache.xerces.dom.PSVIDocumentImpl";
    private final QName fAttrQName;
    protected final Stack fBaseURIStack;
    protected boolean fCreateCDATANodes;
    protected boolean fCreateEntityRefNodes;
    protected CDATASection fCurrentCDATASection;
    protected int fCurrentCDATASectionIndex;
    protected EntityImpl fCurrentEntityDecl;
    protected Node fCurrentNode;
    protected int fCurrentNodeIndex;
    protected LSParserFilter fDOMFilter;
    protected boolean fDeferNodeExpansion;
    protected DeferredDocumentImpl fDeferredDocumentImpl;
    protected int fDeferredEntityDecl;
    protected Document fDocument;
    protected String fDocumentClassName;
    protected CoreDocumentImpl fDocumentImpl;
    protected int fDocumentIndex;
    protected DocumentType fDocumentType;
    protected int fDocumentTypeIndex;
    protected DOMErrorHandlerWrapper fErrorHandler;
    protected boolean fFilterReject;
    protected boolean fFirstChunk;
    protected boolean fInCDATASection;
    protected boolean fInDTD;
    protected boolean fInDTDExternalSubset;
    protected boolean fInEntityRef;
    protected boolean fIncludeComments;
    protected boolean fIncludeIgnorableWhitespace;
    protected StringBuffer fInternalSubset;
    private XMLLocator fLocator;
    protected boolean fNamespaceAware;
    protected int fRejectedElementDepth;
    protected Node fRoot;
    protected Stack fSkippedElemStack;
    protected boolean fStorePSVI;
    protected final StringBuffer fStringBuffer;
    protected static final String NAMESPACES = "http://xml.org/sax/features/namespaces";
    protected static final String CREATE_ENTITY_REF_NODES = "http://apache.org/xml/features/dom/create-entity-ref-nodes";
    protected static final String INCLUDE_COMMENTS_FEATURE = "http://apache.org/xml/features/include-comments";
    protected static final String CREATE_CDATA_NODES_FEATURE = "http://apache.org/xml/features/create-cdata-nodes";
    protected static final String INCLUDE_IGNORABLE_WHITESPACE = "http://apache.org/xml/features/dom/include-ignorable-whitespace";
    protected static final String DEFER_NODE_EXPANSION = "http://apache.org/xml/features/dom/defer-node-expansion";
    private static final String[] RECOGNIZED_FEATURES = {NAMESPACES, CREATE_ENTITY_REF_NODES, INCLUDE_COMMENTS_FEATURE, CREATE_CDATA_NODES_FEATURE, INCLUDE_IGNORABLE_WHITESPACE, DEFER_NODE_EXPANSION};
    protected static final String DOCUMENT_CLASS_NAME = "http://apache.org/xml/properties/dom/document-class-name";
    protected static final String CURRENT_ELEMENT_NODE = "http://apache.org/xml/properties/dom/current-element-node";
    private static final String[] RECOGNIZED_PROPERTIES = {DOCUMENT_CLASS_NAME, CURRENT_ELEMENT_NODE};

    static final class Abort extends RuntimeException {
        static final Abort INSTANCE = new Abort();
        private static final long serialVersionUID = 1687848994976808490L;

        private Abort() {
        }

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    protected AbstractDOMParser(XMLParserConfiguration config) {
        super(config);
        this.fErrorHandler = null;
        this.fStringBuffer = new StringBuffer(50);
        this.fFirstChunk = false;
        this.fFilterReject = false;
        this.fBaseURIStack = new Stack();
        this.fRejectedElementDepth = 0;
        this.fSkippedElemStack = null;
        this.fInEntityRef = false;
        this.fAttrQName = new QName();
        this.fDOMFilter = null;
        this.fConfiguration.addRecognizedFeatures(RECOGNIZED_FEATURES);
        this.fConfiguration.setFeature(CREATE_ENTITY_REF_NODES, true);
        this.fConfiguration.setFeature(INCLUDE_IGNORABLE_WHITESPACE, true);
        this.fConfiguration.setFeature(DEFER_NODE_EXPANSION, true);
        this.fConfiguration.setFeature(INCLUDE_COMMENTS_FEATURE, true);
        this.fConfiguration.setFeature(CREATE_CDATA_NODES_FEATURE, true);
        this.fConfiguration.addRecognizedProperties(RECOGNIZED_PROPERTIES);
        this.fConfiguration.setProperty(DOCUMENT_CLASS_NAME, DEFAULT_DOCUMENT_CLASS_NAME);
    }

    protected String getDocumentClassName() {
        return this.fDocumentClassName;
    }

    protected void setDocumentClassName(String documentClassName) {
        if (documentClassName == null) {
            documentClassName = DEFAULT_DOCUMENT_CLASS_NAME;
        }
        if (!documentClassName.equals(DEFAULT_DOCUMENT_CLASS_NAME) && !documentClassName.equals(PSVI_DOCUMENT_CLASS_NAME)) {
            try {
                Class _class = ObjectFactory.findProviderClass(documentClassName, ObjectFactory.findClassLoader(), true);
                if (!Document.class.isAssignableFrom(_class)) {
                    throw new IllegalArgumentException(DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "InvalidDocumentClassName", new Object[]{documentClassName}));
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "MissingDocumentClassName", new Object[]{documentClassName}));
            }
        }
        this.fDocumentClassName = documentClassName;
        if (!documentClassName.equals(DEFAULT_DOCUMENT_CLASS_NAME)) {
            this.fDeferNodeExpansion = false;
        }
    }

    public Document getDocument() {
        return this.fDocument;
    }

    public final void dropDocumentReferences() {
        this.fDocument = null;
        this.fDocumentImpl = null;
        this.fDeferredDocumentImpl = null;
        this.fDocumentType = null;
        this.fCurrentNode = null;
        this.fCurrentCDATASection = null;
        this.fCurrentEntityDecl = null;
        this.fRoot = null;
    }

    @Override
    public void reset() throws XNIException {
        super.reset();
        this.fCreateEntityRefNodes = this.fConfiguration.getFeature(CREATE_ENTITY_REF_NODES);
        this.fIncludeIgnorableWhitespace = this.fConfiguration.getFeature(INCLUDE_IGNORABLE_WHITESPACE);
        this.fDeferNodeExpansion = this.fConfiguration.getFeature(DEFER_NODE_EXPANSION);
        this.fNamespaceAware = this.fConfiguration.getFeature(NAMESPACES);
        this.fIncludeComments = this.fConfiguration.getFeature(INCLUDE_COMMENTS_FEATURE);
        this.fCreateCDATANodes = this.fConfiguration.getFeature(CREATE_CDATA_NODES_FEATURE);
        setDocumentClassName((String) this.fConfiguration.getProperty(DOCUMENT_CLASS_NAME));
        this.fDocument = null;
        this.fDocumentImpl = null;
        this.fStorePSVI = false;
        this.fDocumentType = null;
        this.fDocumentTypeIndex = -1;
        this.fDeferredDocumentImpl = null;
        this.fCurrentNode = null;
        this.fStringBuffer.setLength(0);
        this.fRoot = null;
        this.fInDTD = false;
        this.fInDTDExternalSubset = false;
        this.fInCDATASection = false;
        this.fFirstChunk = false;
        this.fCurrentCDATASection = null;
        this.fCurrentCDATASectionIndex = -1;
        this.fBaseURIStack.removeAllElements();
    }

    public void setLocale(Locale locale) {
        this.fConfiguration.setLocale(locale);
    }

    @Override
    public void startGeneralEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs) throws XNIException {
        if (!this.fDeferNodeExpansion) {
            if (this.fFilterReject) {
                return;
            }
            setCharacterData(true);
            EntityReference er = this.fDocument.createEntityReference(name);
            if (this.fDocumentImpl != null) {
                EntityReferenceImpl erImpl = (EntityReferenceImpl) er;
                erImpl.setBaseURI(identifier.getExpandedSystemId());
                if (this.fDocumentType != null) {
                    NamedNodeMap entities = this.fDocumentType.getEntities();
                    this.fCurrentEntityDecl = (EntityImpl) entities.getNamedItem(name);
                    if (this.fCurrentEntityDecl != null) {
                        this.fCurrentEntityDecl.setInputEncoding(encoding);
                    }
                }
                erImpl.needsSyncChildren(false);
            }
            this.fInEntityRef = true;
            this.fCurrentNode.appendChild(er);
            this.fCurrentNode = er;
            return;
        }
        int er2 = this.fDeferredDocumentImpl.createDeferredEntityReference(name, identifier.getExpandedSystemId());
        if (this.fDocumentTypeIndex != -1) {
            int node = this.fDeferredDocumentImpl.getLastChild(this.fDocumentTypeIndex, false);
            while (true) {
                if (node == -1) {
                    break;
                }
                short nodeType = this.fDeferredDocumentImpl.getNodeType(node, false);
                if (nodeType == 6) {
                    String nodeName = this.fDeferredDocumentImpl.getNodeName(node, false);
                    if (nodeName.equals(name)) {
                        this.fDeferredEntityDecl = node;
                        this.fDeferredDocumentImpl.setInputEncoding(node, encoding);
                        break;
                    }
                }
                node = this.fDeferredDocumentImpl.getRealPrevSibling(node, false);
            }
        }
        this.fDeferredDocumentImpl.appendChild(this.fCurrentNodeIndex, er2);
        this.fCurrentNodeIndex = er2;
    }

    @Override
    public void textDecl(String version, String encoding, Augmentations augs) throws XNIException {
        if (this.fInDTD) {
            return;
        }
        if (!this.fDeferNodeExpansion) {
            if (this.fCurrentEntityDecl != null && !this.fFilterReject) {
                this.fCurrentEntityDecl.setXmlEncoding(encoding);
                if (version != null) {
                    this.fCurrentEntityDecl.setXmlVersion(version);
                    return;
                }
                return;
            }
            return;
        }
        if (this.fDeferredEntityDecl != -1) {
            this.fDeferredDocumentImpl.setEntityInfo(this.fDeferredEntityDecl, version, encoding);
        }
    }

    @Override
    public void comment(XMLString text, Augmentations augs) throws XNIException {
        if (this.fInDTD) {
            if (this.fInternalSubset != null && !this.fInDTDExternalSubset) {
                this.fInternalSubset.append("<!--");
                if (text.length > 0) {
                    this.fInternalSubset.append(text.ch, text.offset, text.length);
                }
                this.fInternalSubset.append("-->");
                return;
            }
            return;
        }
        if (!this.fIncludeComments || this.fFilterReject) {
            return;
        }
        if (!this.fDeferNodeExpansion) {
            Comment comment = this.fDocument.createComment(text.toString());
            setCharacterData(false);
            this.fCurrentNode.appendChild(comment);
            if (this.fDOMFilter != null && !this.fInEntityRef && (this.fDOMFilter.getWhatToShow() & 128) != 0) {
                short code = this.fDOMFilter.acceptNode(comment);
                switch (code) {
                    case 2:
                    case 3:
                        this.fCurrentNode.removeChild(comment);
                        this.fFirstChunk = true;
                        return;
                    case 4:
                        throw Abort.INSTANCE;
                    default:
                        return;
                }
            }
            return;
        }
        this.fDeferredDocumentImpl.appendChild(this.fCurrentNodeIndex, this.fDeferredDocumentImpl.createDeferredComment(text.toString()));
    }

    @Override
    public void processingInstruction(String target, XMLString data, Augmentations augs) throws XNIException {
        if (this.fInDTD) {
            if (this.fInternalSubset != null && !this.fInDTDExternalSubset) {
                this.fInternalSubset.append("<?");
                this.fInternalSubset.append(target);
                if (data.length > 0) {
                    StringBuffer stringBuffer = this.fInternalSubset;
                    stringBuffer.append(' ');
                    stringBuffer.append(data.ch, data.offset, data.length);
                }
                this.fInternalSubset.append("?>");
                return;
            }
            return;
        }
        if (!this.fDeferNodeExpansion) {
            if (this.fFilterReject) {
                return;
            }
            ProcessingInstruction pi = this.fDocument.createProcessingInstruction(target, data.toString());
            setCharacterData(false);
            this.fCurrentNode.appendChild(pi);
            if (this.fDOMFilter != null && !this.fInEntityRef && (this.fDOMFilter.getWhatToShow() & 64) != 0) {
                short code = this.fDOMFilter.acceptNode(pi);
                switch (code) {
                    case 2:
                    case 3:
                        this.fCurrentNode.removeChild(pi);
                        this.fFirstChunk = true;
                        return;
                    case 4:
                        throw Abort.INSTANCE;
                    default:
                        return;
                }
            }
            return;
        }
        this.fDeferredDocumentImpl.appendChild(this.fCurrentNodeIndex, this.fDeferredDocumentImpl.createDeferredProcessingInstruction(target, data.toString()));
    }

    @Override
    public void startDocument(XMLLocator locator, String encoding, NamespaceContext namespaceContext, Augmentations augs) throws XNIException {
        this.fLocator = locator;
        if (!this.fDeferNodeExpansion) {
            if (this.fDocumentClassName.equals(DEFAULT_DOCUMENT_CLASS_NAME)) {
                this.fDocument = new DocumentImpl();
                this.fDocumentImpl = (CoreDocumentImpl) this.fDocument;
                this.fDocumentImpl.setStrictErrorChecking(false);
                this.fDocumentImpl.setInputEncoding(encoding);
                this.fDocumentImpl.setDocumentURI(locator.getExpandedSystemId());
            } else if (this.fDocumentClassName.equals(PSVI_DOCUMENT_CLASS_NAME)) {
                this.fDocument = new PSVIDocumentImpl();
                this.fDocumentImpl = (CoreDocumentImpl) this.fDocument;
                this.fStorePSVI = true;
                this.fDocumentImpl.setStrictErrorChecking(false);
                this.fDocumentImpl.setInputEncoding(encoding);
                this.fDocumentImpl.setDocumentURI(locator.getExpandedSystemId());
            } else {
                try {
                    ClassLoader cl = ObjectFactory.findClassLoader();
                    Class<?> clsFindProviderClass = ObjectFactory.findProviderClass(this.fDocumentClassName, cl, true);
                    this.fDocument = (Document) clsFindProviderClass.newInstance();
                    Class defaultDocClass = ObjectFactory.findProviderClass(CORE_DOCUMENT_CLASS_NAME, cl, true);
                    if (defaultDocClass.isAssignableFrom(clsFindProviderClass)) {
                        this.fDocumentImpl = (CoreDocumentImpl) this.fDocument;
                        Class psviDocClass = ObjectFactory.findProviderClass(PSVI_DOCUMENT_CLASS_NAME, cl, true);
                        if (psviDocClass.isAssignableFrom(clsFindProviderClass)) {
                            this.fStorePSVI = true;
                        }
                        this.fDocumentImpl.setStrictErrorChecking(false);
                        this.fDocumentImpl.setInputEncoding(encoding);
                        if (locator != null) {
                            this.fDocumentImpl.setDocumentURI(locator.getExpandedSystemId());
                        }
                    }
                } catch (ClassNotFoundException e) {
                } catch (Exception e2) {
                    throw new RuntimeException(DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "CannotCreateDocumentClass", new Object[]{this.fDocumentClassName}));
                }
            }
            this.fCurrentNode = this.fDocument;
            return;
        }
        this.fDeferredDocumentImpl = new DeferredDocumentImpl(this.fNamespaceAware);
        this.fDocument = this.fDeferredDocumentImpl;
        this.fDocumentIndex = this.fDeferredDocumentImpl.createDeferredDocument();
        this.fDeferredDocumentImpl.setInputEncoding(encoding);
        this.fDeferredDocumentImpl.setDocumentURI(locator.getExpandedSystemId());
        this.fCurrentNodeIndex = this.fDocumentIndex;
    }

    @Override
    public void xmlDecl(String version, String encoding, String standalone, Augmentations augs) throws XNIException {
        if (!this.fDeferNodeExpansion) {
            if (this.fDocumentImpl != null) {
                if (version != null) {
                    this.fDocumentImpl.setXmlVersion(version);
                }
                this.fDocumentImpl.setXmlEncoding(encoding);
                this.fDocumentImpl.setXmlStandalone("yes".equals(standalone));
                return;
            }
            return;
        }
        if (version != null) {
            this.fDeferredDocumentImpl.setXmlVersion(version);
        }
        this.fDeferredDocumentImpl.setXmlEncoding(encoding);
        this.fDeferredDocumentImpl.setXmlStandalone("yes".equals(standalone));
    }

    @Override
    public void doctypeDecl(String rootElement, String publicId, String systemId, Augmentations augs) throws XNIException {
        if (!this.fDeferNodeExpansion) {
            if (this.fDocumentImpl != null) {
                this.fDocumentType = this.fDocumentImpl.createDocumentType(rootElement, publicId, systemId);
                this.fCurrentNode.appendChild(this.fDocumentType);
                return;
            }
            return;
        }
        this.fDocumentTypeIndex = this.fDeferredDocumentImpl.createDeferredDocumentType(rootElement, publicId, systemId);
        this.fDeferredDocumentImpl.appendChild(this.fCurrentNodeIndex, this.fDocumentTypeIndex);
    }

    @Override
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        ElementPSVI elementPSVI;
        int attrCount;
        boolean z;
        Augmentations augmentations = augs;
        if (!this.fDeferNodeExpansion) {
            boolean z2 = true;
            if (this.fFilterReject) {
                this.fRejectedElementDepth++;
                return;
            }
            Element el = createElementNode(element);
            int attrCount2 = attributes.getLength();
            boolean seenSchemaDefault = false;
            int i = 0;
            while (i < attrCount2) {
                attributes.getName(i, this.fAttrQName);
                Attr attr = createAttrNode(this.fAttrQName);
                String attrValue = attributes.getValue(i);
                AttributePSVI attrPSVI = (AttributePSVI) attributes.getAugmentations(i).getItem(Constants.ATTRIBUTE_PSVI);
                if (this.fStorePSVI && attrPSVI != null) {
                    ((PSVIAttrNSImpl) attr).setPSVI(attrPSVI);
                }
                attr.setValue(attrValue);
                boolean specified = attributes.isSpecified(i);
                if (!specified && (seenSchemaDefault || (this.fAttrQName.uri != null && this.fAttrQName.uri != NamespaceContext.XMLNS_URI && this.fAttrQName.prefix == null))) {
                    el.setAttributeNodeNS(attr);
                    seenSchemaDefault = true;
                } else {
                    el.setAttributeNode(attr);
                }
                if (this.fDocumentImpl == null) {
                    attrCount = attrCount2;
                    z = z2;
                } else {
                    AttrImpl attrImpl = (AttrImpl) attr;
                    Object type = null;
                    boolean id = false;
                    if (attrPSVI != null && this.fNamespaceAware) {
                        Object type2 = attrPSVI.getMemberTypeDefinition();
                        if (type2 == null) {
                            Object type3 = attrPSVI.getTypeDefinition();
                            if (type3 != null) {
                                id = ((XSSimpleType) type3).isIDType();
                                attrImpl.setType(type3);
                            }
                        } else {
                            id = ((XSSimpleType) type2).isIDType();
                            attrImpl.setType(type2);
                        }
                        attrCount = attrCount2;
                    } else {
                        attrCount = attrCount2;
                        boolean isDeclared = Boolean.TRUE.equals(attributes.getAugmentations(i).getItem(Constants.ATTRIBUTE_DECLARED));
                        if (isDeclared) {
                            type = attributes.getType(i);
                            id = SchemaSymbols.ATTVAL_ID.equals(type);
                        }
                        attrImpl.setType(type);
                    }
                    if (id) {
                        z = true;
                        ((ElementImpl) el).setIdAttributeNode(attr, true);
                    } else {
                        z = true;
                    }
                    attrImpl.setSpecified(specified);
                }
                i++;
                z2 = z;
                attrCount2 = attrCount;
                augmentations = augs;
            }
            setCharacterData(false);
            if (augmentations != null && (elementPSVI = (ElementPSVI) augmentations.getItem(Constants.ELEMENT_PSVI)) != null && this.fNamespaceAware) {
                XSTypeDefinition type4 = elementPSVI.getMemberTypeDefinition();
                if (type4 == null) {
                    type4 = elementPSVI.getTypeDefinition();
                }
                ((ElementNSImpl) el).setType(type4);
            }
            if (this.fDOMFilter != null && !this.fInEntityRef) {
                if (this.fRoot == null) {
                    this.fRoot = el;
                } else {
                    short code = this.fDOMFilter.startElement(el);
                    switch (code) {
                        case 2:
                            this.fFilterReject = z2;
                            this.fRejectedElementDepth = 0;
                            return;
                        case 3:
                            this.fFirstChunk = z2;
                            this.fSkippedElemStack.push(Boolean.TRUE);
                            return;
                        case 4:
                            throw Abort.INSTANCE;
                        default:
                            if (!this.fSkippedElemStack.isEmpty()) {
                                this.fSkippedElemStack.push(Boolean.FALSE);
                            }
                            break;
                    }
                }
            }
            this.fCurrentNode.appendChild(el);
            this.fCurrentNode = el;
            return;
        }
        int el2 = this.fDeferredDocumentImpl.createDeferredElement(this.fNamespaceAware ? element.uri : null, element.rawname);
        int attrCount3 = attributes.getLength();
        int i2 = attrCount3 - 1;
        Object type5 = null;
        int i3 = i2;
        while (i3 >= 0) {
            AttributePSVI attrPSVI2 = (AttributePSVI) attributes.getAugmentations(i3).getItem(Constants.ATTRIBUTE_PSVI);
            boolean id2 = false;
            if (attrPSVI2 != null && this.fNamespaceAware) {
                type5 = attrPSVI2.getMemberTypeDefinition();
                if (type5 == null) {
                    type5 = attrPSVI2.getTypeDefinition();
                    if (type5 != null) {
                        id2 = ((XSSimpleType) type5).isIDType();
                    }
                } else {
                    id2 = ((XSSimpleType) type5).isIDType();
                }
            } else {
                boolean isDeclared2 = Boolean.TRUE.equals(attributes.getAugmentations(i3).getItem(Constants.ATTRIBUTE_DECLARED));
                if (isDeclared2) {
                    type5 = attributes.getType(i3);
                    id2 = SchemaSymbols.ATTVAL_ID.equals(type5);
                }
            }
            Object type6 = type5;
            this.fDeferredDocumentImpl.setDeferredAttribute(el2, attributes.getQName(i3), attributes.getURI(i3), attributes.getValue(i3), attributes.isSpecified(i3), id2, type6);
            i3--;
            type5 = type6;
        }
        this.fDeferredDocumentImpl.appendChild(this.fCurrentNodeIndex, el2);
        this.fCurrentNodeIndex = el2;
    }

    @Override
    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        startElement(element, attributes, augs);
        endElement(element, augs);
    }

    @Override
    public void characters(XMLString text, Augmentations augs) throws XNIException {
        if (!this.fDeferNodeExpansion) {
            if (this.fFilterReject) {
                return;
            }
            if (this.fInCDATASection && this.fCreateCDATANodes) {
                if (this.fCurrentCDATASection == null) {
                    this.fCurrentCDATASection = this.fDocument.createCDATASection(text.toString());
                    this.fCurrentNode.appendChild(this.fCurrentCDATASection);
                    this.fCurrentNode = this.fCurrentCDATASection;
                    return;
                }
                this.fCurrentCDATASection.appendData(text.toString());
                return;
            }
            if (this.fInDTD || text.length == 0) {
                return;
            }
            Node child = this.fCurrentNode.getLastChild();
            if (child != null && child.getNodeType() == 3) {
                if (this.fFirstChunk) {
                    if (this.fDocumentImpl != null) {
                        this.fStringBuffer.append(((TextImpl) child).removeData());
                    } else {
                        this.fStringBuffer.append(((Text) child).getData());
                        ((Text) child).setNodeValue(null);
                    }
                    this.fFirstChunk = false;
                }
                if (text.length > 0) {
                    this.fStringBuffer.append(text.ch, text.offset, text.length);
                    return;
                }
                return;
            }
            this.fFirstChunk = true;
            Text textNode = this.fDocument.createTextNode(text.toString());
            this.fCurrentNode.appendChild(textNode);
            return;
        }
        if (this.fInCDATASection && this.fCreateCDATANodes) {
            if (this.fCurrentCDATASectionIndex == -1) {
                int cs = this.fDeferredDocumentImpl.createDeferredCDATASection(text.toString());
                this.fDeferredDocumentImpl.appendChild(this.fCurrentNodeIndex, cs);
                this.fCurrentCDATASectionIndex = cs;
                this.fCurrentNodeIndex = cs;
                return;
            }
            int txt = this.fDeferredDocumentImpl.createDeferredTextNode(text.toString(), false);
            this.fDeferredDocumentImpl.appendChild(this.fCurrentNodeIndex, txt);
            return;
        }
        if (this.fInDTD || text.length == 0) {
            return;
        }
        String value = text.toString();
        int txt2 = this.fDeferredDocumentImpl.createDeferredTextNode(value, false);
        this.fDeferredDocumentImpl.appendChild(this.fCurrentNodeIndex, txt2);
    }

    @Override
    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
        if (!this.fIncludeIgnorableWhitespace || this.fFilterReject) {
            return;
        }
        if (!this.fDeferNodeExpansion) {
            Node child = this.fCurrentNode.getLastChild();
            if (child != null && child.getNodeType() == 3) {
                ((Text) child).appendData(text.toString());
                return;
            }
            Text textNode = this.fDocument.createTextNode(text.toString());
            if (this.fDocumentImpl != null) {
                TextImpl textNodeImpl = (TextImpl) textNode;
                textNodeImpl.setIgnorableWhitespace(true);
            }
            this.fCurrentNode.appendChild(textNode);
            return;
        }
        int txt = this.fDeferredDocumentImpl.createDeferredTextNode(text.toString(), true);
        this.fDeferredDocumentImpl.appendChild(this.fCurrentNodeIndex, txt);
    }

    @Override
    public void endElement(QName element, Augmentations augs) throws XNIException {
        ElementPSVI elementPSVI;
        ElementPSVI elementPSVI2;
        if (!this.fDeferNodeExpansion) {
            if (augs != null && this.fDocumentImpl != null && ((this.fNamespaceAware || this.fStorePSVI) && (elementPSVI2 = (ElementPSVI) augs.getItem(Constants.ELEMENT_PSVI)) != null)) {
                if (this.fNamespaceAware) {
                    XSTypeDefinition type = elementPSVI2.getMemberTypeDefinition();
                    if (type == null) {
                        type = elementPSVI2.getTypeDefinition();
                    }
                    ((ElementNSImpl) this.fCurrentNode).setType(type);
                }
                if (this.fStorePSVI) {
                    ((PSVIElementNSImpl) this.fCurrentNode).setPSVI(elementPSVI2);
                }
            }
            if (this.fDOMFilter != null) {
                if (this.fFilterReject) {
                    int i = this.fRejectedElementDepth;
                    this.fRejectedElementDepth = i - 1;
                    if (i == 0) {
                        this.fFilterReject = false;
                        return;
                    }
                    return;
                }
                if (!this.fSkippedElemStack.isEmpty() && this.fSkippedElemStack.pop() == Boolean.TRUE) {
                    return;
                }
                setCharacterData(false);
                if (this.fCurrentNode != this.fRoot && !this.fInEntityRef && (this.fDOMFilter.getWhatToShow() & 1) != 0) {
                    short code = this.fDOMFilter.acceptNode(this.fCurrentNode);
                    switch (code) {
                        case 2:
                            Node parent = this.fCurrentNode.getParentNode();
                            parent.removeChild(this.fCurrentNode);
                            this.fCurrentNode = parent;
                            return;
                        case 3:
                            this.fFirstChunk = true;
                            Node parent2 = this.fCurrentNode.getParentNode();
                            NodeList ls = this.fCurrentNode.getChildNodes();
                            int length = ls.getLength();
                            for (int i2 = 0; i2 < length; i2++) {
                                parent2.appendChild(ls.item(0));
                            }
                            parent2.removeChild(this.fCurrentNode);
                            this.fCurrentNode = parent2;
                            return;
                        case 4:
                            throw Abort.INSTANCE;
                    }
                }
                this.fCurrentNode = this.fCurrentNode.getParentNode();
                return;
            }
            setCharacterData(false);
            this.fCurrentNode = this.fCurrentNode.getParentNode();
            return;
        }
        if (augs != null && (elementPSVI = (ElementPSVI) augs.getItem(Constants.ELEMENT_PSVI)) != null) {
            XSTypeDefinition type2 = elementPSVI.getMemberTypeDefinition();
            if (type2 == null) {
                type2 = elementPSVI.getTypeDefinition();
            }
            this.fDeferredDocumentImpl.setTypeInfo(this.fCurrentNodeIndex, type2);
        }
        this.fCurrentNodeIndex = this.fDeferredDocumentImpl.getParentNode(this.fCurrentNodeIndex, false);
    }

    @Override
    public void startCDATA(Augmentations augs) throws XNIException {
        this.fInCDATASection = true;
        if (!this.fDeferNodeExpansion && !this.fFilterReject && this.fCreateCDATANodes) {
            setCharacterData(false);
        }
    }

    @Override
    public void endCDATA(Augmentations augs) throws XNIException {
        this.fInCDATASection = false;
        if (!this.fDeferNodeExpansion) {
            if (!this.fFilterReject && this.fCurrentCDATASection != null) {
                if (this.fDOMFilter != null && !this.fInEntityRef && (this.fDOMFilter.getWhatToShow() & 8) != 0) {
                    short code = this.fDOMFilter.acceptNode(this.fCurrentCDATASection);
                    switch (code) {
                        case 2:
                        case 3:
                            Node parent = this.fCurrentNode.getParentNode();
                            parent.removeChild(this.fCurrentCDATASection);
                            this.fCurrentNode = parent;
                            return;
                        case 4:
                            throw Abort.INSTANCE;
                    }
                }
                this.fCurrentNode = this.fCurrentNode.getParentNode();
                this.fCurrentCDATASection = null;
                return;
            }
            return;
        }
        if (this.fCurrentCDATASectionIndex != -1) {
            this.fCurrentNodeIndex = this.fDeferredDocumentImpl.getParentNode(this.fCurrentNodeIndex, false);
            this.fCurrentCDATASectionIndex = -1;
        }
    }

    @Override
    public void endDocument(Augmentations augs) throws XNIException {
        if (!this.fDeferNodeExpansion) {
            if (this.fDocumentImpl != null) {
                if (this.fLocator != null) {
                    this.fDocumentImpl.setInputEncoding(this.fLocator.getEncoding());
                }
                this.fDocumentImpl.setStrictErrorChecking(true);
            }
            this.fCurrentNode = null;
            return;
        }
        if (this.fLocator != null) {
            this.fDeferredDocumentImpl.setInputEncoding(this.fLocator.getEncoding());
        }
        this.fCurrentNodeIndex = -1;
    }

    @Override
    public void endGeneralEntity(String name, Augmentations augs) throws XNIException {
        if (!this.fDeferNodeExpansion) {
            if (this.fFilterReject) {
                return;
            }
            setCharacterData(true);
            if (this.fDocumentType != null) {
                NamedNodeMap entities = this.fDocumentType.getEntities();
                this.fCurrentEntityDecl = (EntityImpl) entities.getNamedItem(name);
                if (this.fCurrentEntityDecl != null) {
                    if (this.fCurrentEntityDecl != null && this.fCurrentEntityDecl.getFirstChild() == null) {
                        this.fCurrentEntityDecl.setReadOnly(false, true);
                        for (Node child = this.fCurrentNode.getFirstChild(); child != null; child = child.getNextSibling()) {
                            Node copy = child.cloneNode(true);
                            this.fCurrentEntityDecl.appendChild(copy);
                        }
                        this.fCurrentEntityDecl.setReadOnly(true, true);
                    }
                    this.fCurrentEntityDecl = null;
                }
            }
            this.fInEntityRef = false;
            boolean removeEntityRef = false;
            if (this.fCreateEntityRefNodes) {
                if (this.fDocumentImpl != null) {
                    ((NodeImpl) this.fCurrentNode).setReadOnly(true, true);
                }
                if (this.fDOMFilter != null && (this.fDOMFilter.getWhatToShow() & 16) != 0) {
                    short code = this.fDOMFilter.acceptNode(this.fCurrentNode);
                    switch (code) {
                        case 2:
                            Node parent = this.fCurrentNode.getParentNode();
                            parent.removeChild(this.fCurrentNode);
                            this.fCurrentNode = parent;
                            return;
                        case 3:
                            this.fFirstChunk = true;
                            removeEntityRef = true;
                            break;
                        case 4:
                            throw Abort.INSTANCE;
                        default:
                            this.fCurrentNode = this.fCurrentNode.getParentNode();
                            break;
                    }
                } else {
                    this.fCurrentNode = this.fCurrentNode.getParentNode();
                }
            }
            if (!this.fCreateEntityRefNodes || removeEntityRef) {
                NodeList children = this.fCurrentNode.getChildNodes();
                Node parent2 = this.fCurrentNode.getParentNode();
                int length = children.getLength();
                if (length > 0) {
                    Node node = this.fCurrentNode.getPreviousSibling();
                    Node child2 = children.item(0);
                    if (node != null && node.getNodeType() == 3 && child2.getNodeType() == 3) {
                        ((Text) node).appendData(child2.getNodeValue());
                        this.fCurrentNode.removeChild(child2);
                    } else {
                        Node node2 = parent2.insertBefore(child2, this.fCurrentNode);
                        handleBaseURI(node2);
                    }
                    for (int i = 1; i < length; i++) {
                        Node node3 = parent2.insertBefore(children.item(0), this.fCurrentNode);
                        handleBaseURI(node3);
                    }
                }
                parent2.removeChild(this.fCurrentNode);
                this.fCurrentNode = parent2;
                return;
            }
            return;
        }
        if (this.fDocumentTypeIndex != -1) {
            int node4 = this.fDeferredDocumentImpl.getLastChild(this.fDocumentTypeIndex, false);
            while (true) {
                if (node4 == -1) {
                    break;
                }
                short nodeType = this.fDeferredDocumentImpl.getNodeType(node4, false);
                if (nodeType == 6) {
                    String nodeName = this.fDeferredDocumentImpl.getNodeName(node4, false);
                    if (nodeName.equals(name)) {
                        this.fDeferredEntityDecl = node4;
                        break;
                    }
                }
                node4 = this.fDeferredDocumentImpl.getRealPrevSibling(node4, false);
            }
        }
        int node5 = this.fDeferredEntityDecl;
        if (node5 != -1 && this.fDeferredDocumentImpl.getLastChild(this.fDeferredEntityDecl, false) == -1) {
            int prevIndex = -1;
            int childIndex = this.fDeferredDocumentImpl.getLastChild(this.fCurrentNodeIndex, false);
            while (childIndex != -1) {
                int cloneIndex = this.fDeferredDocumentImpl.cloneNode(childIndex, true);
                this.fDeferredDocumentImpl.insertBefore(this.fDeferredEntityDecl, cloneIndex, prevIndex);
                prevIndex = cloneIndex;
                childIndex = this.fDeferredDocumentImpl.getRealPrevSibling(childIndex, false);
            }
        }
        if (this.fCreateEntityRefNodes) {
            this.fCurrentNodeIndex = this.fDeferredDocumentImpl.getParentNode(this.fCurrentNodeIndex, false);
        } else {
            int childIndex2 = this.fDeferredDocumentImpl.getLastChild(this.fCurrentNodeIndex, false);
            int parentIndex = this.fDeferredDocumentImpl.getParentNode(this.fCurrentNodeIndex, false);
            int prevIndex2 = this.fCurrentNodeIndex;
            while (childIndex2 != -1) {
                handleBaseURI(childIndex2);
                int sibling = this.fDeferredDocumentImpl.getRealPrevSibling(childIndex2, false);
                this.fDeferredDocumentImpl.insertBefore(parentIndex, childIndex2, prevIndex2);
                prevIndex2 = childIndex2;
                childIndex2 = sibling;
            }
            if (childIndex2 != -1) {
                this.fDeferredDocumentImpl.setAsLastChild(parentIndex, childIndex2);
            } else {
                int sibling2 = this.fDeferredDocumentImpl.getRealPrevSibling(prevIndex2, false);
                this.fDeferredDocumentImpl.setAsLastChild(parentIndex, sibling2);
            }
            this.fCurrentNodeIndex = parentIndex;
        }
        this.fDeferredEntityDecl = -1;
    }

    protected final void handleBaseURI(Node node) {
        String baseURI;
        if (this.fDocumentImpl != null) {
            short nodeType = node.getNodeType();
            if (nodeType == 1) {
                if (this.fNamespaceAware) {
                    if (((Element) node).getAttributeNodeNS("http://www.w3.org/XML/1998/namespace", "base") != null) {
                        return;
                    }
                } else if (((Element) node).getAttributeNode("xml:base") != null) {
                    return;
                }
                String baseURI2 = ((EntityReferenceImpl) this.fCurrentNode).getBaseURI();
                if (baseURI2 != null && !baseURI2.equals(this.fDocumentImpl.getDocumentURI())) {
                    if (this.fNamespaceAware) {
                        ((Element) node).setAttributeNS("http://www.w3.org/XML/1998/namespace", "xml:base", baseURI2);
                        return;
                    } else {
                        ((Element) node).setAttribute("xml:base", baseURI2);
                        return;
                    }
                }
                return;
            }
            if (nodeType == 7 && (baseURI = ((EntityReferenceImpl) this.fCurrentNode).getBaseURI()) != null && this.fErrorHandler != null) {
                DOMErrorImpl error = new DOMErrorImpl();
                error.fType = "pi-base-uri-not-preserved";
                error.fRelatedData = baseURI;
                error.fSeverity = (short) 1;
                this.fErrorHandler.getErrorHandler().handleError(error);
            }
        }
    }

    protected final void handleBaseURI(int node) {
        short nodeType = this.fDeferredDocumentImpl.getNodeType(node, false);
        if (nodeType == 1) {
            String baseURI = this.fDeferredDocumentImpl.getNodeValueString(this.fCurrentNodeIndex, false);
            if (baseURI == null) {
                baseURI = this.fDeferredDocumentImpl.getDeferredEntityBaseURI(this.fDeferredEntityDecl);
            }
            if (baseURI != null && !baseURI.equals(this.fDeferredDocumentImpl.getDocumentURI())) {
                this.fDeferredDocumentImpl.setDeferredAttribute(node, "xml:base", "http://www.w3.org/XML/1998/namespace", baseURI, true);
                return;
            }
            return;
        }
        if (nodeType == 7) {
            String baseURI2 = this.fDeferredDocumentImpl.getNodeValueString(this.fCurrentNodeIndex, false);
            if (baseURI2 == null) {
                baseURI2 = this.fDeferredDocumentImpl.getDeferredEntityBaseURI(this.fDeferredEntityDecl);
            }
            if (baseURI2 != null && this.fErrorHandler != null) {
                DOMErrorImpl error = new DOMErrorImpl();
                error.fType = "pi-base-uri-not-preserved";
                error.fRelatedData = baseURI2;
                error.fSeverity = (short) 1;
                this.fErrorHandler.getErrorHandler().handleError(error);
            }
        }
    }

    @Override
    public void startDTD(XMLLocator locator, Augmentations augs) throws XNIException {
        this.fInDTD = true;
        if (locator != null) {
            this.fBaseURIStack.push(locator.getBaseSystemId());
        }
        if (this.fDeferNodeExpansion || this.fDocumentImpl != null) {
            this.fInternalSubset = new StringBuffer(SoOperater.STEP);
        }
    }

    @Override
    public void endDTD(Augmentations augs) throws XNIException {
        this.fInDTD = false;
        if (!this.fBaseURIStack.isEmpty()) {
            this.fBaseURIStack.pop();
        }
        String internalSubset = (this.fInternalSubset == null || this.fInternalSubset.length() <= 0) ? null : this.fInternalSubset.toString();
        if (this.fDeferNodeExpansion) {
            if (internalSubset != null) {
                this.fDeferredDocumentImpl.setInternalSubset(this.fDocumentTypeIndex, internalSubset);
            }
        } else if (this.fDocumentImpl != null && internalSubset != null) {
            ((DocumentTypeImpl) this.fDocumentType).setInternalSubset(internalSubset);
        }
    }

    @Override
    public void startConditional(short type, Augmentations augs) throws XNIException {
    }

    @Override
    public void endConditional(Augmentations augs) throws XNIException {
    }

    @Override
    public void startExternalSubset(XMLResourceIdentifier identifier, Augmentations augs) throws XNIException {
        this.fBaseURIStack.push(identifier.getBaseSystemId());
        this.fInDTDExternalSubset = true;
    }

    @Override
    public void endExternalSubset(Augmentations augs) throws XNIException {
        this.fInDTDExternalSubset = false;
        this.fBaseURIStack.pop();
    }

    @Override
    public void internalEntityDecl(String name, XMLString text, XMLString nonNormalizedText, Augmentations augs) throws XNIException {
        if (this.fInternalSubset != null && !this.fInDTDExternalSubset) {
            this.fInternalSubset.append("<!ENTITY ");
            if (name.startsWith("%")) {
                this.fInternalSubset.append("% ");
                this.fInternalSubset.append(name.substring(1));
            } else {
                this.fInternalSubset.append(name);
            }
            this.fInternalSubset.append(' ');
            String value = nonNormalizedText.toString();
            boolean singleQuote = value.indexOf(39) == -1;
            this.fInternalSubset.append(singleQuote ? '\'' : '\"');
            this.fInternalSubset.append(value);
            this.fInternalSubset.append(singleQuote ? '\'' : '\"');
            this.fInternalSubset.append(">\n");
        }
        if (name.startsWith("%")) {
            return;
        }
        if (this.fDocumentType != null) {
            NamedNodeMap entities = this.fDocumentType.getEntities();
            if (((EntityImpl) entities.getNamedItem(name)) == null) {
                EntityImpl entity = (EntityImpl) this.fDocumentImpl.createEntity(name);
                entity.setBaseURI((String) this.fBaseURIStack.peek());
                entities.setNamedItem(entity);
            }
        }
        if (this.fDocumentTypeIndex != -1) {
            boolean found = false;
            int node = this.fDeferredDocumentImpl.getLastChild(this.fDocumentTypeIndex, false);
            while (true) {
                if (node == -1) {
                    break;
                }
                short nodeType = this.fDeferredDocumentImpl.getNodeType(node, false);
                if (nodeType == 6) {
                    String nodeName = this.fDeferredDocumentImpl.getNodeName(node, false);
                    if (nodeName.equals(name)) {
                        found = true;
                        break;
                    }
                }
                node = this.fDeferredDocumentImpl.getRealPrevSibling(node, false);
            }
            if (!found) {
                int entityIndex = this.fDeferredDocumentImpl.createDeferredEntity(name, null, null, null, (String) this.fBaseURIStack.peek());
                this.fDeferredDocumentImpl.appendChild(this.fDocumentTypeIndex, entityIndex);
            }
        }
    }

    @Override
    public void externalEntityDecl(String name, XMLResourceIdentifier identifier, Augmentations augs) throws XNIException {
        String publicId = identifier.getPublicId();
        String literalSystemId = identifier.getLiteralSystemId();
        if (this.fInternalSubset != null && !this.fInDTDExternalSubset) {
            this.fInternalSubset.append("<!ENTITY ");
            if (name.startsWith("%")) {
                this.fInternalSubset.append("% ");
                this.fInternalSubset.append(name.substring(1));
            } else {
                this.fInternalSubset.append(name);
            }
            this.fInternalSubset.append(' ');
            if (publicId != null) {
                this.fInternalSubset.append("PUBLIC '");
                this.fInternalSubset.append(publicId);
                this.fInternalSubset.append("' '");
            } else {
                this.fInternalSubset.append("SYSTEM '");
            }
            this.fInternalSubset.append(literalSystemId);
            this.fInternalSubset.append("'>\n");
        }
        if (name.startsWith("%")) {
            return;
        }
        if (this.fDocumentType != null) {
            NamedNodeMap entities = this.fDocumentType.getEntities();
            if (((EntityImpl) entities.getNamedItem(name)) == null) {
                EntityImpl entity = (EntityImpl) this.fDocumentImpl.createEntity(name);
                entity.setPublicId(publicId);
                entity.setSystemId(literalSystemId);
                entity.setBaseURI(identifier.getBaseSystemId());
                entities.setNamedItem(entity);
            }
        }
        if (this.fDocumentTypeIndex != -1) {
            boolean found = false;
            int nodeIndex = this.fDeferredDocumentImpl.getLastChild(this.fDocumentTypeIndex, false);
            int nodeIndex2 = nodeIndex;
            while (true) {
                if (nodeIndex2 == -1) {
                    break;
                }
                short nodeType = this.fDeferredDocumentImpl.getNodeType(nodeIndex2, false);
                if (nodeType == 6) {
                    String nodeName = this.fDeferredDocumentImpl.getNodeName(nodeIndex2, false);
                    if (nodeName.equals(name)) {
                        found = true;
                        break;
                    }
                }
                nodeIndex2 = this.fDeferredDocumentImpl.getRealPrevSibling(nodeIndex2, false);
            }
            if (!found) {
                int entityIndex = this.fDeferredDocumentImpl.createDeferredEntity(name, publicId, literalSystemId, null, identifier.getBaseSystemId());
                this.fDeferredDocumentImpl.appendChild(this.fDocumentTypeIndex, entityIndex);
            }
        }
    }

    @Override
    public void startParameterEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs) throws XNIException {
        if (augs != null && this.fInternalSubset != null && !this.fInDTDExternalSubset && Boolean.TRUE.equals(augs.getItem(Constants.ENTITY_SKIPPED))) {
            StringBuffer stringBuffer = this.fInternalSubset;
            stringBuffer.append(name);
            stringBuffer.append(";\n");
        }
        this.fBaseURIStack.push(identifier.getExpandedSystemId());
    }

    @Override
    public void endParameterEntity(String name, Augmentations augs) throws XNIException {
        this.fBaseURIStack.pop();
    }

    @Override
    public void unparsedEntityDecl(String name, XMLResourceIdentifier identifier, String notation, Augmentations augs) throws XNIException {
        String publicId = identifier.getPublicId();
        String literalSystemId = identifier.getLiteralSystemId();
        if (this.fInternalSubset != null && !this.fInDTDExternalSubset) {
            this.fInternalSubset.append("<!ENTITY ");
            this.fInternalSubset.append(name);
            this.fInternalSubset.append(' ');
            if (publicId != null) {
                this.fInternalSubset.append("PUBLIC '");
                this.fInternalSubset.append(publicId);
                if (literalSystemId != null) {
                    this.fInternalSubset.append("' '");
                    this.fInternalSubset.append(literalSystemId);
                }
            } else {
                this.fInternalSubset.append("SYSTEM '");
                this.fInternalSubset.append(literalSystemId);
            }
            this.fInternalSubset.append("' NDATA ");
            this.fInternalSubset.append(notation);
            this.fInternalSubset.append(">\n");
        }
        if (this.fDocumentType != null) {
            NamedNodeMap entities = this.fDocumentType.getEntities();
            if (((EntityImpl) entities.getNamedItem(name)) == null) {
                EntityImpl entity = (EntityImpl) this.fDocumentImpl.createEntity(name);
                entity.setPublicId(publicId);
                entity.setSystemId(literalSystemId);
                entity.setNotationName(notation);
                entity.setBaseURI(identifier.getBaseSystemId());
                entities.setNamedItem(entity);
            }
        }
        if (this.fDocumentTypeIndex != -1) {
            boolean found = false;
            int nodeIndex = this.fDeferredDocumentImpl.getLastChild(this.fDocumentTypeIndex, false);
            int nodeIndex2 = nodeIndex;
            while (true) {
                if (nodeIndex2 == -1) {
                    break;
                }
                short nodeType = this.fDeferredDocumentImpl.getNodeType(nodeIndex2, false);
                if (nodeType == 6) {
                    String nodeName = this.fDeferredDocumentImpl.getNodeName(nodeIndex2, false);
                    if (nodeName.equals(name)) {
                        found = true;
                        break;
                    }
                }
                nodeIndex2 = this.fDeferredDocumentImpl.getRealPrevSibling(nodeIndex2, false);
            }
            if (!found) {
                int entityIndex = this.fDeferredDocumentImpl.createDeferredEntity(name, publicId, literalSystemId, notation, identifier.getBaseSystemId());
                this.fDeferredDocumentImpl.appendChild(this.fDocumentTypeIndex, entityIndex);
            }
        }
    }

    @Override
    public void notationDecl(String name, XMLResourceIdentifier identifier, Augmentations augs) throws XNIException {
        String publicId = identifier.getPublicId();
        String literalSystemId = identifier.getLiteralSystemId();
        if (this.fInternalSubset != null && !this.fInDTDExternalSubset) {
            this.fInternalSubset.append("<!NOTATION ");
            this.fInternalSubset.append(name);
            if (publicId != null) {
                this.fInternalSubset.append(" PUBLIC '");
                this.fInternalSubset.append(publicId);
                if (literalSystemId != null) {
                    this.fInternalSubset.append("' '");
                    this.fInternalSubset.append(literalSystemId);
                }
            } else {
                this.fInternalSubset.append(" SYSTEM '");
                this.fInternalSubset.append(literalSystemId);
            }
            this.fInternalSubset.append("'>\n");
        }
        if (this.fDocumentImpl != null && this.fDocumentType != null) {
            NamedNodeMap notations = this.fDocumentType.getNotations();
            if (notations.getNamedItem(name) == null) {
                NotationImpl notation = (NotationImpl) this.fDocumentImpl.createNotation(name);
                notation.setPublicId(publicId);
                notation.setSystemId(literalSystemId);
                notation.setBaseURI(identifier.getBaseSystemId());
                notations.setNamedItem(notation);
            }
        }
        if (this.fDocumentTypeIndex != -1) {
            boolean found = false;
            int nodeIndex = this.fDeferredDocumentImpl.getLastChild(this.fDocumentTypeIndex, false);
            while (true) {
                if (nodeIndex == -1) {
                    break;
                }
                short nodeType = this.fDeferredDocumentImpl.getNodeType(nodeIndex, false);
                if (nodeType == 12) {
                    String nodeName = this.fDeferredDocumentImpl.getNodeName(nodeIndex, false);
                    if (nodeName.equals(name)) {
                        found = true;
                        break;
                    }
                }
                nodeIndex = this.fDeferredDocumentImpl.getPrevSibling(nodeIndex, false);
            }
            if (!found) {
                int notationIndex = this.fDeferredDocumentImpl.createDeferredNotation(name, publicId, literalSystemId, identifier.getBaseSystemId());
                this.fDeferredDocumentImpl.appendChild(this.fDocumentTypeIndex, notationIndex);
            }
        }
    }

    @Override
    public void ignoredCharacters(XMLString text, Augmentations augs) throws XNIException {
    }

    @Override
    public void elementDecl(String name, String contentModel, Augmentations augs) throws XNIException {
        if (this.fInternalSubset != null && !this.fInDTDExternalSubset) {
            this.fInternalSubset.append("<!ELEMENT ");
            this.fInternalSubset.append(name);
            this.fInternalSubset.append(' ');
            this.fInternalSubset.append(contentModel);
            this.fInternalSubset.append(">\n");
        }
    }

    @Override
    public void attributeDecl(String elementName, String attributeName, String type, String[] enumeration, String defaultType, XMLString defaultValue, XMLString nonNormalizedDefaultValue, Augmentations augs) throws XNIException {
        AttrImpl attr;
        if (this.fInternalSubset != null && !this.fInDTDExternalSubset) {
            this.fInternalSubset.append("<!ATTLIST ");
            this.fInternalSubset.append(elementName);
            this.fInternalSubset.append(' ');
            this.fInternalSubset.append(attributeName);
            this.fInternalSubset.append(' ');
            if (type.equals("ENUMERATION")) {
                this.fInternalSubset.append('(');
                for (int i = 0; i < enumeration.length; i++) {
                    if (i > 0) {
                        this.fInternalSubset.append('|');
                    }
                    this.fInternalSubset.append(enumeration[i]);
                }
                this.fInternalSubset.append(')');
            } else {
                this.fInternalSubset.append(type);
            }
            if (defaultType != null) {
                this.fInternalSubset.append(' ');
                this.fInternalSubset.append(defaultType);
            }
            if (defaultValue != null) {
                this.fInternalSubset.append(" '");
                for (int i2 = 0; i2 < defaultValue.length; i2++) {
                    char c = defaultValue.ch[defaultValue.offset + i2];
                    if (c == '\'') {
                        this.fInternalSubset.append("&apos;");
                    } else {
                        this.fInternalSubset.append(c);
                    }
                }
                this.fInternalSubset.append('\'');
            }
            this.fInternalSubset.append(">\n");
        }
        if (this.fDeferredDocumentImpl != null) {
            if (defaultValue != null) {
                int elementDefIndex = this.fDeferredDocumentImpl.lookupElementDefinition(elementName);
                if (elementDefIndex == -1) {
                    elementDefIndex = this.fDeferredDocumentImpl.createDeferredElementDefinition(elementName);
                    this.fDeferredDocumentImpl.appendChild(this.fDocumentTypeIndex, elementDefIndex);
                }
                String namespaceURI = null;
                if (this.fNamespaceAware) {
                    if (attributeName.startsWith("xmlns:") || attributeName.equals("xmlns")) {
                        namespaceURI = NamespaceContext.XMLNS_URI;
                    } else if (attributeName.startsWith("xml:")) {
                        namespaceURI = NamespaceContext.XML_URI;
                    }
                }
                int attrIndex = this.fDeferredDocumentImpl.createDeferredAttribute(attributeName, namespaceURI, defaultValue.toString(), false);
                if (SchemaSymbols.ATTVAL_ID.equals(type)) {
                    this.fDeferredDocumentImpl.setIdAttribute(attrIndex);
                }
                this.fDeferredDocumentImpl.appendChild(elementDefIndex, attrIndex);
                return;
            }
            return;
        }
        if (this.fDocumentImpl != null && defaultValue != null) {
            NamedNodeMap elements = ((DocumentTypeImpl) this.fDocumentType).getElements();
            ElementDefinitionImpl elementDef = (ElementDefinitionImpl) elements.getNamedItem(elementName);
            if (elementDef == null) {
                elementDef = this.fDocumentImpl.createElementDefinition(elementName);
                ((DocumentTypeImpl) this.fDocumentType).getElements().setNamedItem(elementDef);
            }
            boolean nsEnabled = this.fNamespaceAware;
            if (nsEnabled) {
                String namespaceURI2 = null;
                if (attributeName.startsWith("xmlns:") || attributeName.equals("xmlns")) {
                    namespaceURI2 = NamespaceContext.XMLNS_URI;
                } else if (attributeName.startsWith("xml:")) {
                    namespaceURI2 = NamespaceContext.XML_URI;
                }
                attr = (AttrImpl) this.fDocumentImpl.createAttributeNS(namespaceURI2, attributeName);
            } else {
                attr = (AttrImpl) this.fDocumentImpl.createAttribute(attributeName);
            }
            attr.setValue(defaultValue.toString());
            attr.setSpecified(false);
            attr.setIdAttribute(SchemaSymbols.ATTVAL_ID.equals(type));
            if (nsEnabled) {
                elementDef.getAttributes().setNamedItemNS(attr);
            } else {
                elementDef.getAttributes().setNamedItem(attr);
            }
        }
    }

    @Override
    public void startAttlist(String elementName, Augmentations augs) throws XNIException {
    }

    @Override
    public void endAttlist(Augmentations augs) throws XNIException {
    }

    protected Element createElementNode(QName element) {
        if (this.fNamespaceAware) {
            if (this.fDocumentImpl != null) {
                Element el = this.fDocumentImpl.createElementNS(element.uri, element.rawname, element.localpart);
                return el;
            }
            Element el2 = this.fDocument.createElementNS(element.uri, element.rawname);
            return el2;
        }
        Element el3 = this.fDocument.createElement(element.rawname);
        return el3;
    }

    protected Attr createAttrNode(QName attrQName) {
        if (this.fNamespaceAware) {
            if (this.fDocumentImpl != null) {
                Attr attr = this.fDocumentImpl.createAttributeNS(attrQName.uri, attrQName.rawname, attrQName.localpart);
                return attr;
            }
            Attr attr2 = this.fDocument.createAttributeNS(attrQName.uri, attrQName.rawname);
            return attr2;
        }
        Attr attr3 = this.fDocument.createAttribute(attrQName.rawname);
        return attr3;
    }

    protected void setCharacterData(boolean sawChars) {
        this.fFirstChunk = sawChars;
        Node child = this.fCurrentNode.getLastChild();
        if (child != null) {
            if (this.fStringBuffer.length() > 0) {
                if (child.getNodeType() == 3) {
                    if (this.fDocumentImpl != null) {
                        ((TextImpl) child).replaceData(this.fStringBuffer.toString());
                    } else {
                        ((Text) child).setData(this.fStringBuffer.toString());
                    }
                }
                this.fStringBuffer.setLength(0);
            }
            if (this.fDOMFilter != null && !this.fInEntityRef && child.getNodeType() == 3 && (this.fDOMFilter.getWhatToShow() & 4) != 0) {
                short code = this.fDOMFilter.acceptNode(child);
                switch (code) {
                    case 2:
                    case 3:
                        this.fCurrentNode.removeChild(child);
                        return;
                    case 4:
                        throw Abort.INSTANCE;
                    default:
                        return;
                }
            }
        }
    }

    public void abort() {
        throw Abort.INSTANCE;
    }
}
