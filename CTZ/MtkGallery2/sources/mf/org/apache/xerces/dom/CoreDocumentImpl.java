package mf.org.apache.xerces.dom;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.WeakHashMap;
import mf.org.apache.xerces.dom.ParentNode;
import mf.org.apache.xerces.util.URI;
import mf.org.apache.xerces.util.XML11Char;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.w3c.dom.Attr;
import mf.org.w3c.dom.CDATASection;
import mf.org.w3c.dom.Comment;
import mf.org.w3c.dom.DOMConfiguration;
import mf.org.w3c.dom.DOMException;
import mf.org.w3c.dom.DOMImplementation;
import mf.org.w3c.dom.Document;
import mf.org.w3c.dom.DocumentFragment;
import mf.org.w3c.dom.DocumentType;
import mf.org.w3c.dom.Element;
import mf.org.w3c.dom.Entity;
import mf.org.w3c.dom.EntityReference;
import mf.org.w3c.dom.NamedNodeMap;
import mf.org.w3c.dom.Node;
import mf.org.w3c.dom.NodeList;
import mf.org.w3c.dom.Notation;
import mf.org.w3c.dom.ProcessingInstruction;
import mf.org.w3c.dom.Text;
import mf.org.w3c.dom.UserDataHandler;
import mf.org.w3c.dom.events.Event;
import mf.org.w3c.dom.events.EventListener;
import mf.org.w3c.dom.ls.DOMImplementationLS;
import mf.org.w3c.dom.ls.LSSerializer;

public class CoreDocumentImpl extends ParentNode implements Document {
    private static final int[] kidOK = new int[13];
    static final long serialVersionUID = 0;
    protected String actualEncoding;
    protected boolean allowGrammarAccess;
    protected int changes;
    protected ElementImpl docElement;
    protected DocumentTypeImpl docType;
    private int documentNumber;
    transient DOMNormalizer domNormalizer;
    protected String encoding;
    protected boolean errorChecking;
    transient DOMConfigurationImpl fConfiguration;
    protected String fDocumentURI;
    transient NodeListCache fFreeNLCache;
    transient Object fXPathEvaluator;
    protected Hashtable identifiers;
    private int nodeCounter;
    private Map nodeTable;
    protected boolean standalone;
    protected Map userData;
    protected String version;
    private boolean xml11Version;
    protected boolean xmlVersionChanged;

    static {
        kidOK[9] = 1410;
        int[] iArr = kidOK;
        int[] iArr2 = kidOK;
        int[] iArr3 = kidOK;
        kidOK[1] = 442;
        iArr3[5] = 442;
        iArr2[6] = 442;
        iArr[11] = 442;
        kidOK[2] = 40;
        int[] iArr4 = kidOK;
        int[] iArr5 = kidOK;
        int[] iArr6 = kidOK;
        int[] iArr7 = kidOK;
        int[] iArr8 = kidOK;
        kidOK[12] = 0;
        iArr8[4] = 0;
        iArr7[3] = 0;
        iArr6[8] = 0;
        iArr5[7] = 0;
        iArr4[10] = 0;
    }

    public CoreDocumentImpl() {
        this(false);
    }

    public CoreDocumentImpl(boolean grammarAccess) {
        super(null);
        this.domNormalizer = null;
        this.fConfiguration = null;
        this.fXPathEvaluator = null;
        this.changes = 0;
        this.errorChecking = true;
        this.xmlVersionChanged = false;
        this.documentNumber = 0;
        this.nodeCounter = 0;
        this.xml11Version = false;
        this.ownerDocument = this;
        this.allowGrammarAccess = grammarAccess;
    }

    public CoreDocumentImpl(DocumentType doctype) {
        this(doctype, false);
    }

    public CoreDocumentImpl(DocumentType doctype, boolean grammarAccess) {
        this(grammarAccess);
        if (doctype != null) {
            try {
                DocumentTypeImpl doctypeImpl = (DocumentTypeImpl) doctype;
                doctypeImpl.ownerDocument = this;
                appendChild(doctype);
            } catch (ClassCastException e) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null);
                throw new DOMException((short) 4, msg);
            }
        }
    }

    @Override
    public final Document getOwnerDocument() {
        return null;
    }

    @Override
    public short getNodeType() {
        return (short) 9;
    }

    @Override
    public String getNodeName() {
        return "#document";
    }

    @Override
    public Node cloneNode(boolean deep) {
        CoreDocumentImpl newdoc = new CoreDocumentImpl();
        callUserDataHandlers(this, newdoc, (short) 1);
        cloneNode(newdoc, deep);
        return newdoc;
    }

    protected void cloneNode(CoreDocumentImpl newdoc, boolean deep) {
        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        if (deep) {
            HashMap reversedIdentifiers = null;
            if (this.identifiers != null) {
                HashMap reversedIdentifiers2 = new HashMap();
                for (Map.Entry entry : this.identifiers.entrySet()) {
                    Object elementId = entry.getKey();
                    Object elementNode = entry.getValue();
                    reversedIdentifiers2.put(elementNode, elementId);
                }
                reversedIdentifiers = reversedIdentifiers2;
            }
            for (ChildNode kid = this.firstChild; kid != null; kid = kid.nextSibling) {
                newdoc.appendChild(newdoc.importNode(kid, true, true, reversedIdentifiers));
            }
        }
        newdoc.allowGrammarAccess = this.allowGrammarAccess;
        newdoc.errorChecking = this.errorChecking;
    }

    @Override
    public Node insertBefore(Node node, Node refChild) throws DOMException {
        int type = node.getNodeType();
        if (this.errorChecking) {
            if (needsSyncChildren()) {
                synchronizeChildren();
            }
            if ((type == 1 && this.docElement != null) || (type == 10 && this.docType != null)) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "HIERARCHY_REQUEST_ERR", null);
                throw new DOMException((short) 3, msg);
            }
        }
        if (node.getOwnerDocument() == null && (node instanceof DocumentTypeImpl)) {
            node.ownerDocument = this;
        }
        super.insertBefore(node, refChild);
        if (type == 1) {
            this.docElement = (ElementImpl) node;
        } else if (type == 10) {
            this.docType = (DocumentTypeImpl) node;
        }
        return node;
    }

    @Override
    public Node removeChild(Node oldChild) throws DOMException {
        super.removeChild(oldChild);
        int type = oldChild.getNodeType();
        if (type == 1) {
            this.docElement = null;
        } else if (type == 10) {
            this.docType = null;
        }
        return oldChild;
    }

    @Override
    public Node replaceChild(Node node, Node oldChild) throws DOMException {
        if (node.getOwnerDocument() == null && (node instanceof DocumentTypeImpl)) {
            node.ownerDocument = this;
        }
        if (this.errorChecking && ((this.docType != null && oldChild.getNodeType() != 10 && node.getNodeType() == 10) || (this.docElement != null && oldChild.getNodeType() != 1 && node.getNodeType() == 1))) {
            throw new DOMException((short) 3, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "HIERARCHY_REQUEST_ERR", null));
        }
        super.replaceChild(node, oldChild);
        int type = oldChild.getNodeType();
        if (type == 1) {
            this.docElement = (ElementImpl) node;
        } else if (type == 10) {
            this.docType = (DocumentTypeImpl) node;
        }
        return oldChild;
    }

    @Override
    public String getTextContent() throws DOMException {
        return null;
    }

    @Override
    public void setTextContent(String textContent) throws DOMException {
    }

    @Override
    public Object getFeature(String feature, String version) {
        boolean anyVersion = version == null || version.length() == 0;
        if (feature.equalsIgnoreCase("+XPath") && (anyVersion || version.equals("3.0"))) {
            if (this.fXPathEvaluator != null) {
                return this.fXPathEvaluator;
            }
            try {
                Class xpathClass = ObjectFactory.findProviderClass("org.apache.xpath.domapi.XPathEvaluatorImpl", ObjectFactory.findClassLoader(), true);
                Constructor xpathClassConstr = xpathClass.getConstructor(Document.class);
                for (Class<?> cls : xpathClass.getInterfaces()) {
                    if (cls.getName().equals("org.w3c.dom.xpath.XPathEvaluator")) {
                        this.fXPathEvaluator = xpathClassConstr.newInstance(this);
                        return this.fXPathEvaluator;
                    }
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }
        return super.getFeature(feature, version);
    }

    @Override
    public Attr createAttribute(String name) throws DOMException {
        if (this.errorChecking && !isXMLName(name, this.xml11Version)) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR", null);
            throw new DOMException((short) 5, msg);
        }
        return new AttrImpl(this, name);
    }

    @Override
    public CDATASection createCDATASection(String data) throws DOMException {
        return new CDATASectionImpl(this, data);
    }

    @Override
    public Comment createComment(String data) {
        return new CommentImpl(this, data);
    }

    public DocumentFragment createDocumentFragment() {
        return new DocumentFragmentImpl(this);
    }

    @Override
    public Element createElement(String tagName) throws DOMException {
        if (this.errorChecking && !isXMLName(tagName, this.xml11Version)) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR", null);
            throw new DOMException((short) 5, msg);
        }
        return new ElementImpl(this, tagName);
    }

    @Override
    public EntityReference createEntityReference(String name) throws DOMException {
        if (this.errorChecking && !isXMLName(name, this.xml11Version)) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR", null);
            throw new DOMException((short) 5, msg);
        }
        return new EntityReferenceImpl(this, name);
    }

    @Override
    public ProcessingInstruction createProcessingInstruction(String target, String data) throws DOMException {
        if (this.errorChecking && !isXMLName(target, this.xml11Version)) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR", null);
            throw new DOMException((short) 5, msg);
        }
        return new ProcessingInstructionImpl(this, target, data);
    }

    @Override
    public Text createTextNode(String data) {
        return new TextImpl(this, data);
    }

    @Override
    public DocumentType getDoctype() {
        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        return this.docType;
    }

    @Override
    public Element getDocumentElement() {
        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        return this.docElement;
    }

    public NodeList getElementsByTagName(String tagname) {
        return new DeepNodeListImpl(this, tagname);
    }

    @Override
    public DOMImplementation getImplementation() {
        return CoreDOMImplementationImpl.getDOMImplementation();
    }

    public void setErrorChecking(boolean check) {
        this.errorChecking = check;
    }

    public void setStrictErrorChecking(boolean check) {
        this.errorChecking = check;
    }

    public boolean getErrorChecking() {
        return this.errorChecking;
    }

    public boolean getStrictErrorChecking() {
        return this.errorChecking;
    }

    @Override
    public String getInputEncoding() {
        return this.actualEncoding;
    }

    public void setInputEncoding(String value) {
        this.actualEncoding = value;
    }

    public void setXmlEncoding(String value) {
        this.encoding = value;
    }

    public void setEncoding(String value) {
        setXmlEncoding(value);
    }

    @Override
    public String getXmlEncoding() {
        return this.encoding;
    }

    public String getEncoding() {
        return getXmlEncoding();
    }

    public void setXmlVersion(String value) {
        if (value.equals("1.0") || value.equals("1.1")) {
            String msg = getXmlVersion();
            if (!msg.equals(value)) {
                this.xmlVersionChanged = true;
                isNormalized(false);
                this.version = value;
            }
            if (getXmlVersion().equals("1.1")) {
                this.xml11Version = true;
                return;
            } else {
                this.xml11Version = false;
                return;
            }
        }
        String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg2);
    }

    public void setVersion(String value) {
        setXmlVersion(value);
    }

    @Override
    public String getXmlVersion() {
        return this.version == null ? "1.0" : this.version;
    }

    public String getVersion() {
        return getXmlVersion();
    }

    public void setXmlStandalone(boolean value) throws DOMException {
        this.standalone = value;
    }

    public void setStandalone(boolean value) {
        setXmlStandalone(value);
    }

    public boolean getXmlStandalone() {
        return this.standalone;
    }

    public boolean getStandalone() {
        return getXmlStandalone();
    }

    @Override
    public String getDocumentURI() {
        return this.fDocumentURI;
    }

    protected boolean canRenameElements(String newNamespaceURI, String newNodeName, ElementImpl el) {
        return true;
    }

    public Node renameNode(Node node, String str, String str2) throws DOMException {
        ?? ReplaceRenameElement;
        if (this.errorChecking && node.getOwnerDocument() != this && node != this) {
            throw new DOMException((short) 4, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
        }
        switch (node.getNodeType()) {
            case 1:
                ?? r0 = (ElementImpl) node;
                if (r0 instanceof ElementNSImpl) {
                    if (canRenameElements(str, str2, r0)) {
                        r0.rename(str, str2);
                        callUserDataHandlers(r0, null, (short) 4);
                        ReplaceRenameElement = r0;
                    } else {
                        ReplaceRenameElement = replaceRenameElement(r0, str, str2);
                    }
                } else if (str == null && canRenameElements(null, str2, r0)) {
                    r0.rename(str2);
                    callUserDataHandlers(r0, null, (short) 4);
                    ReplaceRenameElement = r0;
                } else {
                    ReplaceRenameElement = replaceRenameElement(r0, str, str2);
                }
                renamedElement((Element) node, ReplaceRenameElement);
                return ReplaceRenameElement;
            case 2:
                AttrImpl attrImpl = (AttrImpl) node;
                Element ownerElement = attrImpl.getOwnerElement();
                if (ownerElement != null) {
                    ownerElement.removeAttributeNode(attrImpl);
                }
                if (node instanceof AttrNSImpl) {
                    ((AttrNSImpl) attrImpl).rename(str, str2);
                    if (ownerElement != null) {
                        ownerElement.setAttributeNodeNS(attrImpl);
                    }
                    callUserDataHandlers(attrImpl, null, (short) 4);
                } else if (str == null) {
                    attrImpl.rename(str2);
                    if (ownerElement != null) {
                        ownerElement.setAttributeNode(attrImpl);
                    }
                    callUserDataHandlers(attrImpl, null, (short) 4);
                } else {
                    AttrNSImpl attrNSImpl = (AttrNSImpl) createAttributeNS(str, str2);
                    copyEventListeners(attrImpl, attrNSImpl);
                    Hashtable hashtableRemoveUserDataTable = removeUserDataTable(attrImpl);
                    for (Node firstChild = attrImpl.getFirstChild(); firstChild != null; firstChild = attrImpl.getFirstChild()) {
                        attrImpl.removeChild(firstChild);
                        attrNSImpl.appendChild(firstChild);
                    }
                    setUserDataTable(attrNSImpl, hashtableRemoveUserDataTable);
                    callUserDataHandlers(attrImpl, attrNSImpl, (short) 4);
                    if (ownerElement != null) {
                        ownerElement.setAttributeNode(attrNSImpl);
                    }
                    attrImpl = attrNSImpl;
                }
                renamedAttrNode((Attr) node, attrImpl);
                return attrImpl;
            default:
                throw new DOMException((short) 9, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null));
        }
    }

    private ElementImpl replaceRenameElement(ElementImpl el, String namespaceURI, String name) {
        ElementNSImpl nel = (ElementNSImpl) createElementNS(namespaceURI, name);
        copyEventListeners(el, nel);
        Hashtable data = removeUserDataTable(el);
        Node parent = el.getParentNode();
        Node nextSib = el.getNextSibling();
        if (parent != null) {
            parent.removeChild(el);
        }
        Node child = el.getFirstChild();
        while (child != null) {
            el.removeChild(child);
            nel.appendChild(child);
            child = el.getFirstChild();
        }
        nel.moveSpecifiedAttributes(el);
        setUserDataTable(nel, data);
        callUserDataHandlers(el, nel, (short) 4);
        if (parent != null) {
            parent.insertBefore(nel, nextSib);
        }
        return nel;
    }

    public void normalizeDocument() {
        if (isNormalized() && !isNormalizeDocRequired()) {
            return;
        }
        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        if (this.domNormalizer == null) {
            this.domNormalizer = new DOMNormalizer();
        }
        if (this.fConfiguration == null) {
            this.fConfiguration = new DOMConfigurationImpl();
        } else {
            this.fConfiguration.reset();
        }
        this.domNormalizer.normalizeDocument(this, this.fConfiguration);
        isNormalized(true);
        this.xmlVersionChanged = false;
    }

    public DOMConfiguration getDomConfig() {
        if (this.fConfiguration == null) {
            this.fConfiguration = new DOMConfigurationImpl();
        }
        return this.fConfiguration;
    }

    @Override
    public String getBaseURI() {
        if (this.fDocumentURI != null && this.fDocumentURI.length() != 0) {
            try {
                return new URI(this.fDocumentURI).toString();
            } catch (URI.MalformedURIException e) {
                return null;
            }
        }
        return this.fDocumentURI;
    }

    public void setDocumentURI(String documentURI) {
        this.fDocumentURI = documentURI;
    }

    public boolean getAsync() {
        return false;
    }

    public void setAsync(boolean async) {
        if (async) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
            throw new DOMException((short) 9, msg);
        }
    }

    public void abort() {
    }

    public boolean load(String uri) {
        return false;
    }

    public boolean loadXML(String source) {
        return false;
    }

    public String saveXML(Node node) throws DOMException {
        if (this.errorChecking && node != null && this != node.getOwnerDocument()) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null);
            throw new DOMException((short) 4, msg);
        }
        DOMImplementationLS domImplLS = (DOMImplementationLS) DOMImplementationImpl.getDOMImplementation();
        LSSerializer xmlWriter = domImplLS.createLSSerializer();
        if (node == null) {
            node = this;
        }
        return xmlWriter.writeToString(node);
    }

    void setMutationEvents(boolean set) {
    }

    boolean getMutationEvents() {
        return false;
    }

    public DocumentType createDocumentType(String qualifiedName, String publicID, String systemID) throws DOMException {
        return new DocumentTypeImpl(this, qualifiedName, publicID, systemID);
    }

    public Entity createEntity(String name) throws DOMException {
        if (this.errorChecking && !isXMLName(name, this.xml11Version)) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR", null);
            throw new DOMException((short) 5, msg);
        }
        return new EntityImpl(this, name);
    }

    public Notation createNotation(String name) throws DOMException {
        if (this.errorChecking && !isXMLName(name, this.xml11Version)) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR", null);
            throw new DOMException((short) 5, msg);
        }
        return new NotationImpl(this, name);
    }

    public ElementDefinitionImpl createElementDefinition(String name) throws DOMException {
        if (this.errorChecking && !isXMLName(name, this.xml11Version)) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR", null);
            throw new DOMException((short) 5, msg);
        }
        return new ElementDefinitionImpl(this, name);
    }

    @Override
    protected int getNodeNumber() {
        if (this.documentNumber == 0) {
            CoreDOMImplementationImpl cd = (CoreDOMImplementationImpl) CoreDOMImplementationImpl.getDOMImplementation();
            this.documentNumber = cd.assignDocumentNumber();
        }
        return this.documentNumber;
    }

    protected int getNodeNumber(Node node) {
        if (this.nodeTable == null) {
            this.nodeTable = new WeakHashMap();
            int num = this.nodeCounter - 1;
            this.nodeCounter = num;
            this.nodeTable.put(node, new Integer(num));
            return num;
        }
        Integer n = (Integer) this.nodeTable.get(node);
        if (n != null) {
            return n.intValue();
        }
        int num2 = this.nodeCounter - 1;
        this.nodeCounter = num2;
        this.nodeTable.put(node, new Integer(num2));
        return num2;
    }

    @Override
    public Node importNode(Node source, boolean deep) throws DOMException {
        return importNode(source, deep, false, null);
    }

    private Node importNode(Node node, boolean deep, boolean cloningDoc, HashMap reversedIdentifiers) throws DOMException {
        Element newElement;
        Node newnode;
        Object elementId;
        boolean deep2;
        Hashtable userData = null;
        if (node instanceof NodeImpl) {
            userData = node.getUserDataRecord();
        }
        int type = node.getNodeType();
        switch (type) {
            case 1:
                boolean domLevel20 = node.getOwnerDocument().getImplementation().hasFeature("XML", "2.0");
                if (!domLevel20 || node.getLocalName() == null) {
                    newElement = createElement(node.getNodeName());
                } else {
                    newElement = createElementNS(node.getNamespaceURI(), node.getNodeName());
                }
                NamedNodeMap sourceAttrs = node.getAttributes();
                if (sourceAttrs != null) {
                    int length = sourceAttrs.getLength();
                    for (int index = 0; index < length; index++) {
                        Attr attr = (Attr) sourceAttrs.item(index);
                        if (attr.getSpecified() || cloningDoc) {
                            Attr newAttr = (Attr) importNode(attr, true, cloningDoc, reversedIdentifiers);
                            if (!domLevel20 || attr.getLocalName() == null) {
                                newElement.setAttributeNode(newAttr);
                            } else {
                                newElement.setAttributeNodeNS(newAttr);
                            }
                        }
                    }
                }
                if (reversedIdentifiers != null && (elementId = reversedIdentifiers.get(node)) != null) {
                    if (this.identifiers == null) {
                        this.identifiers = new Hashtable();
                    }
                    this.identifiers.put(elementId, newElement);
                }
                newnode = newElement;
                deep2 = deep;
                if (userData != null) {
                    callUserDataHandlers(node, newnode, (short) 2, userData);
                }
                if (deep2) {
                    for (Node srckid = node.getFirstChild(); srckid != null; srckid = srckid.getNextSibling()) {
                        newnode.appendChild(importNode(srckid, true, cloningDoc, reversedIdentifiers));
                    }
                }
                if (newnode.getNodeType() == 6) {
                    ((NodeImpl) newnode).setReadOnly(true, true);
                }
                return newnode;
            case 2:
                if (!node.getOwnerDocument().getImplementation().hasFeature("XML", "2.0") || node.getLocalName() == null) {
                    newnode = createAttribute(node.getNodeName());
                } else {
                    newnode = createAttributeNS(node.getNamespaceURI(), node.getNodeName());
                }
                if (node instanceof AttrImpl) {
                    if (node.hasStringValue()) {
                        AttrImpl newattr = (AttrImpl) newnode;
                        newattr.setValue(node.getValue());
                        deep2 = false;
                    } else {
                        deep2 = true;
                    }
                } else if (node.getFirstChild() == null) {
                    newnode.setNodeValue(node.getNodeValue());
                    deep2 = false;
                } else {
                    deep2 = true;
                }
                if (userData != null) {
                }
                if (deep2) {
                }
                if (newnode.getNodeType() == 6) {
                }
                return newnode;
            case 3:
                newnode = createTextNode(node.getNodeValue());
                deep2 = deep;
                if (userData != null) {
                }
                if (deep2) {
                }
                if (newnode.getNodeType() == 6) {
                }
                return newnode;
            case 4:
                newnode = createCDATASection(node.getNodeValue());
                deep2 = deep;
                if (userData != null) {
                }
                if (deep2) {
                }
                if (newnode.getNodeType() == 6) {
                }
                return newnode;
            case 5:
                newnode = createEntityReference(node.getNodeName());
                deep2 = false;
                if (userData != null) {
                }
                if (deep2) {
                }
                if (newnode.getNodeType() == 6) {
                }
                return newnode;
            case 6:
                Entity srcentity = (Entity) node;
                EntityImpl newentity = (EntityImpl) createEntity(node.getNodeName());
                newentity.setPublicId(srcentity.getPublicId());
                newentity.setSystemId(srcentity.getSystemId());
                newentity.setNotationName(srcentity.getNotationName());
                newentity.isReadOnly(false);
                newnode = newentity;
                deep2 = deep;
                if (userData != null) {
                }
                if (deep2) {
                }
                if (newnode.getNodeType() == 6) {
                }
                return newnode;
            case 7:
                newnode = createProcessingInstruction(node.getNodeName(), node.getNodeValue());
                deep2 = deep;
                if (userData != null) {
                }
                if (deep2) {
                }
                if (newnode.getNodeType() == 6) {
                }
                return newnode;
            case 8:
                newnode = createComment(node.getNodeValue());
                deep2 = deep;
                if (userData != null) {
                }
                if (deep2) {
                }
                if (newnode.getNodeType() == 6) {
                }
                return newnode;
            case 9:
            default:
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
                throw new DOMException((short) 9, msg);
            case 10:
                if (!cloningDoc) {
                    String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
                    throw new DOMException((short) 9, msg2);
                }
                DocumentType srcdoctype = (DocumentType) node;
                DocumentTypeImpl newdoctype = (DocumentTypeImpl) createDocumentType(srcdoctype.getNodeName(), srcdoctype.getPublicId(), srcdoctype.getSystemId());
                newdoctype.setInternalSubset(srcdoctype.getInternalSubset());
                NamedNodeMap smap = srcdoctype.getEntities();
                NamedNodeMap tmap = newdoctype.getEntities();
                if (smap != null) {
                    for (int i = 0; i < smap.getLength(); i++) {
                        tmap.setNamedItem(importNode(smap.item(i), true, true, reversedIdentifiers));
                    }
                }
                NamedNodeMap smap2 = srcdoctype.getNotations();
                NamedNodeMap tmap2 = newdoctype.getNotations();
                if (smap2 != null) {
                    for (int i2 = 0; i2 < smap2.getLength(); i2++) {
                        tmap2.setNamedItem(importNode(smap2.item(i2), true, true, reversedIdentifiers));
                    }
                }
                newnode = newdoctype;
                deep2 = deep;
                if (userData != null) {
                }
                if (deep2) {
                }
                if (newnode.getNodeType() == 6) {
                }
                return newnode;
            case 11:
                newnode = createDocumentFragment();
                deep2 = deep;
                if (userData != null) {
                }
                if (deep2) {
                }
                if (newnode.getNodeType() == 6) {
                }
                return newnode;
            case 12:
                Notation srcnotation = (Notation) node;
                NotationImpl newnotation = (NotationImpl) createNotation(node.getNodeName());
                newnotation.setPublicId(srcnotation.getPublicId());
                newnotation.setSystemId(srcnotation.getSystemId());
                newnode = newnotation;
                deep2 = deep;
                if (userData != null) {
                }
                if (deep2) {
                }
                if (newnode.getNodeType() == 6) {
                }
                return newnode;
        }
    }

    @Override
    public Node adoptNode(Node source) {
        Hashtable userData;
        try {
            NodeImpl node = (NodeImpl) source;
            if (source == null) {
                return null;
            }
            if (source != null && source.getOwnerDocument() != null) {
                DOMImplementation thisImpl = getImplementation();
                DOMImplementation otherImpl = source.getOwnerDocument().getImplementation();
                if (thisImpl != otherImpl) {
                    if ((thisImpl instanceof DOMImplementationImpl) && (otherImpl instanceof DeferredDOMImplementationImpl)) {
                        undeferChildren(node);
                    } else if (!(thisImpl instanceof DeferredDOMImplementationImpl) || !(otherImpl instanceof DOMImplementationImpl)) {
                        return null;
                    }
                } else if (otherImpl instanceof DeferredDOMImplementationImpl) {
                    undeferChildren(node);
                }
            }
            switch (node.getNodeType()) {
                case 1:
                    userData = node.getUserDataRecord();
                    Node parent = node.getParentNode();
                    if (parent != null) {
                        parent.removeChild(source);
                    }
                    node.setOwnerDocument(this);
                    if (userData != null) {
                        setUserDataTable(node, userData);
                    }
                    ((ElementImpl) node).reconcileDefaultAttributes();
                    break;
                case 2:
                    AttrImpl attr = (AttrImpl) node;
                    if (attr.getOwnerElement() != null) {
                        attr.getOwnerElement().removeAttributeNode(attr);
                    }
                    attr.isSpecified(true);
                    Hashtable userData2 = node.getUserDataRecord();
                    attr.setOwnerDocument(this);
                    if (userData2 != null) {
                        setUserDataTable(node, userData2);
                    }
                    userData = userData2;
                    break;
                case 3:
                case 4:
                case 7:
                case 8:
                case 11:
                default:
                    userData = node.getUserDataRecord();
                    Node parent2 = node.getParentNode();
                    if (parent2 != null) {
                        parent2.removeChild(source);
                    }
                    node.setOwnerDocument(this);
                    if (userData != null) {
                        setUserDataTable(node, userData);
                    }
                    break;
                case 5:
                    userData = node.getUserDataRecord();
                    Node parent3 = node.getParentNode();
                    if (parent3 != null) {
                        parent3.removeChild(source);
                    }
                    while (true) {
                        Node child = node.getFirstChild();
                        if (child != null) {
                            node.removeChild(child);
                        } else {
                            node.setOwnerDocument(this);
                            if (userData != null) {
                                setUserDataTable(node, userData);
                            }
                            if (this.docType != null) {
                                NamedNodeMap entities = this.docType.getEntities();
                                Node entityNode = entities.getNamedItem(node.getNodeName());
                                if (entityNode != null) {
                                    for (Node child2 = entityNode.getFirstChild(); child2 != null; child2 = child2.getNextSibling()) {
                                        Node childClone = child2.cloneNode(true);
                                        node.appendChild(childClone);
                                    }
                                }
                            }
                        }
                        break;
                    }
                    break;
                case 6:
                case 12:
                    String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null);
                    throw new DOMException((short) 7, msg);
                case 9:
                case 10:
                    String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
                    throw new DOMException((short) 9, msg2);
            }
            if (userData != null) {
                callUserDataHandlers(source, null, (short) 5, userData);
            }
            return node;
        } catch (ClassCastException e) {
            return null;
        }
    }

    protected void undeferChildren(Node node) {
        while (node != null) {
            if (((NodeImpl) node).needsSyncData()) {
                ((NodeImpl) node).synchronizeData();
            }
            NamedNodeMap attributes = node.getAttributes();
            if (attributes != null) {
                int length = attributes.getLength();
                for (int i = 0; i < length; i++) {
                    undeferChildren(attributes.item(i));
                }
            }
            Node nextNode = node.getFirstChild();
            while (nextNode == null && !node.equals(node)) {
                nextNode = node.getNextSibling();
                if (nextNode == null && ((node = node.getParentNode()) == null || node.equals(node))) {
                    nextNode = null;
                    break;
                }
            }
            node = nextNode;
        }
    }

    public Element getElementById(String elementId) {
        return getIdentifier(elementId);
    }

    protected final void clearIdentifiers() {
        if (this.identifiers != null) {
            this.identifiers.clear();
        }
    }

    public void putIdentifier(String idName, Element element) {
        if (element == null) {
            removeIdentifier(idName);
            return;
        }
        if (needsSyncData()) {
            synchronizeData();
        }
        if (this.identifiers == null) {
            this.identifiers = new Hashtable();
        }
        this.identifiers.put(idName, element);
    }

    public Element getIdentifier(String idName) {
        Element elem;
        if (needsSyncData()) {
            synchronizeData();
        }
        if (this.identifiers != null && (elem = (Element) this.identifiers.get(idName)) != null) {
            for (Node parent = elem.getParentNode(); parent != null; parent = parent.getParentNode()) {
                if (parent == this) {
                    return elem;
                }
            }
        }
        return null;
    }

    public void removeIdentifier(String idName) {
        if (needsSyncData()) {
            synchronizeData();
        }
        if (this.identifiers == null) {
            return;
        }
        this.identifiers.remove(idName);
    }

    public Enumeration getIdentifiers() {
        if (needsSyncData()) {
            synchronizeData();
        }
        if (this.identifiers == null) {
            this.identifiers = new Hashtable();
        }
        return this.identifiers.keys();
    }

    @Override
    public Element createElementNS(String namespaceURI, String qualifiedName) throws DOMException {
        return new ElementNSImpl(this, namespaceURI, qualifiedName);
    }

    public Element createElementNS(String namespaceURI, String qualifiedName, String localpart) throws DOMException {
        return new ElementNSImpl(this, namespaceURI, qualifiedName, localpart);
    }

    @Override
    public Attr createAttributeNS(String namespaceURI, String qualifiedName) throws DOMException {
        return new AttrNSImpl(this, namespaceURI, qualifiedName);
    }

    public Attr createAttributeNS(String namespaceURI, String qualifiedName, String localpart) throws DOMException {
        return new AttrNSImpl(this, namespaceURI, qualifiedName, localpart);
    }

    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
        return new DeepNodeListImpl(this, namespaceURI, localName);
    }

    public Object clone() throws CloneNotSupportedException {
        CoreDocumentImpl newdoc = (CoreDocumentImpl) super.clone();
        newdoc.docType = null;
        newdoc.docElement = null;
        return newdoc;
    }

    public static final boolean isXMLName(String s, boolean xml11Version) {
        if (s == null) {
            return false;
        }
        if (!xml11Version) {
            return XMLChar.isValidName(s);
        }
        return XML11Char.isXML11ValidName(s);
    }

    public static final boolean isValidQName(String prefix, String local, boolean xml11Version) {
        if (local == null) {
            return false;
        }
        if (!xml11Version) {
            if ((prefix != null && !XMLChar.isValidNCName(prefix)) || !XMLChar.isValidNCName(local)) {
                return false;
            }
            return true;
        }
        if ((prefix != null && !XML11Char.isXML11ValidNCName(prefix)) || !XML11Char.isXML11ValidNCName(local)) {
            return false;
        }
        return true;
    }

    protected boolean isKidOK(Node parent, Node child) {
        return (this.allowGrammarAccess && parent.getNodeType() == 10) ? child.getNodeType() == 1 : (kidOK[parent.getNodeType()] & (1 << child.getNodeType())) != 0;
    }

    @Override
    protected void changed() {
        this.changes++;
    }

    @Override
    protected int changes() {
        return this.changes;
    }

    NodeListCache getNodeListCache(ParentNode owner) {
        if (this.fFreeNLCache == null) {
            return new NodeListCache(owner);
        }
        NodeListCache c = this.fFreeNLCache;
        this.fFreeNLCache = this.fFreeNLCache.next;
        c.fChild = null;
        c.fChildIndex = -1;
        c.fLength = -1;
        if (c.fOwner != null) {
            c.fOwner.fNodeListCache = null;
        }
        c.fOwner = owner;
        return c;
    }

    void freeNodeListCache(NodeListCache c) {
        c.next = this.fFreeNLCache;
        this.fFreeNLCache = c;
    }

    public Object setUserData(Node n, String key, Object data, UserDataHandler handler) {
        Hashtable t;
        Hashtable t2;
        Object o;
        if (data == null) {
            if (this.userData == null || (t2 = (Hashtable) this.userData.get(n)) == null || (o = t2.remove(key)) == null) {
                return null;
            }
            ParentNode.UserDataRecord r = (ParentNode.UserDataRecord) o;
            return r.fData;
        }
        if (this.userData == null) {
            this.userData = new WeakHashMap();
            t = new Hashtable();
            this.userData.put(n, t);
        } else {
            t = (Hashtable) this.userData.get(n);
            if (t == null) {
                t = new Hashtable();
                this.userData.put(n, t);
            }
        }
        Object o2 = t.put(key, new ParentNode.UserDataRecord(data, handler));
        if (o2 == null) {
            return null;
        }
        ParentNode.UserDataRecord r2 = (ParentNode.UserDataRecord) o2;
        return r2.fData;
    }

    public Object getUserData(Node n, String key) {
        Hashtable t;
        Object o;
        if (this.userData == null || (t = (Hashtable) this.userData.get(n)) == null || (o = t.get(key)) == null) {
            return null;
        }
        ParentNode.UserDataRecord r = (ParentNode.UserDataRecord) o;
        return r.fData;
    }

    protected Hashtable getUserDataRecord(Node n) {
        Hashtable t;
        if (this.userData == null || (t = (Hashtable) this.userData.get(n)) == null) {
            return null;
        }
        return t;
    }

    Hashtable removeUserDataTable(Node n) {
        if (this.userData == null) {
            return null;
        }
        return (Hashtable) this.userData.get(n);
    }

    void setUserDataTable(Node n, Hashtable data) {
        if (this.userData == null) {
            this.userData = new WeakHashMap();
        }
        if (data != null) {
            this.userData.put(n, data);
        }
    }

    protected void callUserDataHandlers(Node node, Node c, short operation) {
        Hashtable t;
        if (this.userData == null || !(node instanceof NodeImpl) || (t = node.getUserDataRecord()) == null || t.isEmpty()) {
            return;
        }
        callUserDataHandlers(node, c, operation, t);
    }

    void callUserDataHandlers(Node n, Node c, short operation, Hashtable userData) {
        if (userData == null || userData.isEmpty()) {
            return;
        }
        for (Map.Entry entry : userData.entrySet()) {
            String key = (String) entry.getKey();
            ParentNode.UserDataRecord r = (ParentNode.UserDataRecord) entry.getValue();
            if (r.fHandler != null) {
                r.fHandler.handle(operation, key, r.fData, n, c);
            }
        }
    }

    protected final void checkNamespaceWF(String qname, int colon1, int colon2) {
        if (!this.errorChecking) {
            return;
        }
        if (colon1 == 0 || colon1 == qname.length() - 1 || colon2 != colon1) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NAMESPACE_ERR", null);
            throw new DOMException((short) 14, msg);
        }
    }

    protected final void checkDOMNSErr(String prefix, String namespace) {
        if (this.errorChecking) {
            if (namespace == null) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NAMESPACE_ERR", null);
                throw new DOMException((short) 14, msg);
            }
            if (prefix.equals("xml") && !namespace.equals(NamespaceContext.XML_URI)) {
                String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NAMESPACE_ERR", null);
                throw new DOMException((short) 14, msg2);
            }
            if ((prefix.equals("xmlns") && !namespace.equals(NamespaceContext.XMLNS_URI)) || (!prefix.equals("xmlns") && namespace.equals(NamespaceContext.XMLNS_URI))) {
                String msg3 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NAMESPACE_ERR", null);
                throw new DOMException((short) 14, msg3);
            }
        }
    }

    protected final void checkQName(String prefix, String local) {
        boolean validNCName;
        if (!this.errorChecking) {
            return;
        }
        boolean z = false;
        if (!this.xml11Version) {
            if ((prefix == null || XMLChar.isValidNCName(prefix)) && XMLChar.isValidNCName(local)) {
                z = true;
            }
            validNCName = z;
        } else {
            if ((prefix == null || XML11Char.isXML11ValidNCName(prefix)) && XML11Char.isXML11ValidNCName(local)) {
                z = true;
            }
            validNCName = z;
        }
        if (!validNCName) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR", null);
            throw new DOMException((short) 5, msg);
        }
    }

    boolean isXML11Version() {
        return this.xml11Version;
    }

    boolean isNormalizeDocRequired() {
        return true;
    }

    boolean isXMLVersionChanged() {
        return this.xmlVersionChanged;
    }

    protected void setUserData(NodeImpl n, Object data) {
        setUserData(n, "XERCES1DOMUSERDATA", data, null);
    }

    protected Object getUserData(NodeImpl n) {
        return getUserData(n, "XERCES1DOMUSERDATA");
    }

    protected void addEventListener(NodeImpl node, String type, EventListener listener, boolean useCapture) {
    }

    protected void removeEventListener(NodeImpl node, String type, EventListener listener, boolean useCapture) {
    }

    protected void copyEventListeners(NodeImpl src, NodeImpl tgt) {
    }

    protected boolean dispatchEvent(NodeImpl node, Event event) {
        return false;
    }

    void replacedText(CharacterDataImpl node) {
    }

    void deletedText(CharacterDataImpl node, int offset, int count) {
    }

    void insertedText(CharacterDataImpl node, int offset, int count) {
    }

    void modifyingCharacterData(NodeImpl node, boolean replace) {
    }

    void modifiedCharacterData(NodeImpl node, String oldvalue, String value, boolean replace) {
    }

    void insertingNode(NodeImpl node, boolean replace) {
    }

    void insertedNode(NodeImpl node, NodeImpl newInternal, boolean replace) {
    }

    void removingNode(NodeImpl node, NodeImpl oldChild, boolean replace) {
    }

    void removedNode(NodeImpl node, boolean replace) {
    }

    void replacingNode(NodeImpl node) {
    }

    void replacedNode(NodeImpl node) {
    }

    void replacingData(NodeImpl node) {
    }

    void replacedCharacterData(NodeImpl node, String oldvalue, String value) {
    }

    void modifiedAttrValue(AttrImpl attr, String oldvalue) {
    }

    void setAttrNode(AttrImpl attr, AttrImpl previous) {
    }

    void removedAttrNode(AttrImpl attr, NodeImpl oldOwner, String name) {
    }

    void renamedAttrNode(Attr oldAt, Attr newAt) {
    }

    void renamedElement(Element oldEl, Element newEl) {
    }

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        if (this.userData != null) {
            this.userData = new WeakHashMap(this.userData);
        }
        if (this.nodeTable != null) {
            this.nodeTable = new WeakHashMap(this.nodeTable);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        Map oldUserData = this.userData;
        Map oldNodeTable = this.nodeTable;
        if (oldUserData != null) {
            try {
                this.userData = new Hashtable(oldUserData);
            } catch (Throwable th) {
                this.userData = oldUserData;
                this.nodeTable = oldNodeTable;
                throw th;
            }
        }
        if (oldNodeTable != null) {
            this.nodeTable = new Hashtable(oldNodeTable);
        }
        out.defaultWriteObject();
        this.userData = oldUserData;
        this.nodeTable = oldNodeTable;
    }
}
