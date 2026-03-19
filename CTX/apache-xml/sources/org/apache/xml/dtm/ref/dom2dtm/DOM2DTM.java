package org.apache.xml.dtm.ref.dom2dtm;

import java.util.Vector;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.dom.DOMSource;
import org.apache.xalan.templates.Constants;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.dtm.DTMWSFilter;
import org.apache.xml.dtm.ref.DTMDefaultBaseIterators;
import org.apache.xml.dtm.ref.DTMManagerDefault;
import org.apache.xml.dtm.ref.ExpandedNameTable;
import org.apache.xml.dtm.ref.IncrementalSAXSource;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;
import org.apache.xml.utils.FastStringBuffer;
import org.apache.xml.utils.QName;
import org.apache.xml.utils.StringBufferPool;
import org.apache.xml.utils.SuballocatedIntVector;
import org.apache.xml.utils.TreeWalker;
import org.apache.xml.utils.XMLCharacterRecognizer;
import org.apache.xml.utils.XMLString;
import org.apache.xml.utils.XMLStringFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;

public class DOM2DTM extends DTMDefaultBaseIterators {
    static final boolean JJK_DEBUG = false;
    static final boolean JJK_NEWCODE = true;
    static final String NAMESPACE_DECL_NS = "http://www.w3.org/XML/1998/namespace";
    private int m_last_kid;
    private int m_last_parent;
    protected Vector m_nodes;
    private transient boolean m_nodesAreProcessed;
    private transient Node m_pos;
    boolean m_processedFirstElement;
    private transient Node m_root;
    TreeWalker m_walker;

    public interface CharacterNodeHandler {
        void characters(Node node) throws SAXException;
    }

    public DOM2DTM(DTMManager dTMManager, DOMSource dOMSource, int i, DTMWSFilter dTMWSFilter, XMLStringFactory xMLStringFactory, boolean z) {
        int length;
        super(dTMManager, dOMSource, i, dTMWSFilter, xMLStringFactory, z);
        this.m_last_parent = 0;
        this.m_last_kid = -1;
        this.m_processedFirstElement = false;
        this.m_nodes = new Vector();
        this.m_walker = new TreeWalker(null);
        Node node = dOMSource.getNode();
        this.m_root = node;
        this.m_pos = node;
        this.m_last_kid = -1;
        this.m_last_parent = -1;
        this.m_last_kid = addNode(this.m_root, this.m_last_parent, this.m_last_kid, -1);
        if (1 == this.m_root.getNodeType()) {
            NamedNodeMap attributes = this.m_root.getAttributes();
            if (attributes != null) {
                length = attributes.getLength();
            } else {
                length = 0;
            }
            if (length > 0) {
                int iAddNode = -1;
                for (int i2 = 0; i2 < length; i2++) {
                    iAddNode = addNode(attributes.item(i2), 0, iAddNode, -1);
                    this.m_firstch.setElementAt(-1, iAddNode);
                }
                this.m_nextsib.setElementAt(-1, iAddNode);
            }
        }
        this.m_nodesAreProcessed = false;
    }

    protected int addNode(Node node, int i, int i2, int i3) {
        String localName;
        int expandedTypeID;
        int size = this.m_nodes.size();
        if (this.m_dtmIdent.size() == (size >>> 16)) {
            try {
                if (this.m_mgr == null) {
                    throw new ClassCastException();
                }
                DTMManagerDefault dTMManagerDefault = (DTMManagerDefault) this.m_mgr;
                int firstFreeDTMID = dTMManagerDefault.getFirstFreeDTMID();
                dTMManagerDefault.addDTM(this, firstFreeDTMID, size);
                this.m_dtmIdent.addElement(firstFreeDTMID << 16);
            } catch (ClassCastException e) {
                error(XMLMessages.createXMLMessage(XMLErrorResources.ER_NO_DTMIDS_AVAIL, null));
            }
        }
        this.m_size++;
        if (-1 == i3) {
            i3 = node.getNodeType();
        }
        if (2 == i3) {
            String nodeName = node.getNodeName();
            if (nodeName.startsWith(Constants.ATTRNAME_XMLNS) || nodeName.equals("xmlns")) {
                i3 = 13;
            }
        }
        this.m_nodes.addElement(node);
        this.m_firstch.setElementAt(-2, size);
        this.m_nextsib.setElementAt(-2, size);
        this.m_prevsib.setElementAt(i2, size);
        this.m_parent.setElementAt(i, size);
        if (-1 != i && i3 != 2 && i3 != 13 && -2 == this.m_firstch.elementAt(i)) {
            this.m_firstch.setElementAt(size, i);
        }
        String namespaceURI = node.getNamespaceURI();
        if (i3 == 7) {
            localName = node.getNodeName();
        } else {
            localName = node.getLocalName();
        }
        if ((i3 == 1 || i3 == 2) && localName == null) {
            localName = node.getNodeName();
        }
        ExpandedNameTable expandedNameTable = this.m_expandedNameTable;
        if (node.getLocalName() != null || i3 != 1) {
        }
        if (localName != null) {
            expandedTypeID = expandedNameTable.getExpandedTypeID(namespaceURI, localName, i3);
        } else {
            expandedTypeID = expandedNameTable.getExpandedTypeID(i3);
        }
        this.m_exptype.setElementAt(expandedTypeID, size);
        indexNode(expandedTypeID, size);
        if (-1 != i2) {
            this.m_nextsib.setElementAt(size, i2);
        }
        if (i3 == 13) {
            declareNamespaceInContext(i, size);
        }
        return size;
    }

    @Override
    public int getNumberOfNodes() {
        return this.m_nodes.size();
    }

    @Override
    protected boolean nextNode() {
        boolean zEquals;
        int length;
        int iAddNode;
        boolean shouldStripWhitespace;
        if (this.m_nodesAreProcessed) {
            return false;
        }
        Node parentNode = this.m_pos;
        Node node = null;
        Node nextSibling = null;
        short nodeType = -1;
        do {
            if (parentNode.hasChildNodes()) {
                nextSibling = parentNode.getFirstChild();
                if (nextSibling != null && 10 == nextSibling.getNodeType()) {
                    nextSibling = nextSibling.getNextSibling();
                }
                if (5 != parentNode.getNodeType()) {
                    this.m_last_parent = this.m_last_kid;
                    this.m_last_kid = -1;
                    if (this.m_wsfilter != null) {
                        short shouldStripSpace = this.m_wsfilter.getShouldStripSpace(makeNodeHandle(this.m_last_parent), this);
                        if (3 == shouldStripSpace) {
                            shouldStripWhitespace = getShouldStripWhitespace();
                        } else {
                            shouldStripWhitespace = 2 == shouldStripSpace;
                        }
                        pushShouldStripWhitespace(shouldStripWhitespace);
                    }
                }
            } else {
                if (this.m_last_kid != -1 && this.m_firstch.elementAt(this.m_last_kid) == -2) {
                    this.m_firstch.setElementAt(-1, this.m_last_kid);
                }
                while (this.m_last_parent != -1) {
                    nextSibling = parentNode.getNextSibling();
                    if (nextSibling != null && 10 == nextSibling.getNodeType()) {
                        nextSibling = nextSibling.getNextSibling();
                    }
                    if (nextSibling != null) {
                        break;
                    }
                    parentNode = parentNode.getParentNode();
                    if (parentNode == null || 5 != parentNode.getNodeType()) {
                        popShouldStripWhitespace();
                        if (this.m_last_kid == -1) {
                            this.m_firstch.setElementAt(-1, this.m_last_parent);
                        } else {
                            this.m_nextsib.setElementAt(-1, this.m_last_kid);
                        }
                        SuballocatedIntVector suballocatedIntVector = this.m_parent;
                        int i = this.m_last_parent;
                        this.m_last_kid = i;
                        this.m_last_parent = suballocatedIntVector.elementAt(i);
                    }
                }
                if (this.m_last_parent == -1) {
                    nextSibling = null;
                }
            }
            if (nextSibling != null) {
                nodeType = nextSibling.getNodeType();
            }
            if (5 == nodeType) {
                parentNode = nextSibling;
            }
        } while (5 == nodeType);
        if (nextSibling == null) {
            this.m_nextsib.setElementAt(-1, 0);
            this.m_nodesAreProcessed = true;
            this.m_pos = null;
            return false;
        }
        short nodeType2 = nextSibling.getNodeType();
        if (3 == nodeType2 || 4 == nodeType2) {
            Node node2 = null;
            boolean zIsWhiteSpace = this.m_wsfilter != null && getShouldStripWhitespace();
            Node nodeLogicalNextDOMTextNode = nextSibling;
            while (nodeLogicalNextDOMTextNode != null) {
                if (3 == nodeLogicalNextDOMTextNode.getNodeType()) {
                    nodeType2 = 3;
                }
                zIsWhiteSpace &= XMLCharacterRecognizer.isWhiteSpace(nodeLogicalNextDOMTextNode.getNodeValue());
                node2 = nodeLogicalNextDOMTextNode;
                nodeLogicalNextDOMTextNode = logicalNextDOMTextNode(nodeLogicalNextDOMTextNode);
            }
            zEquals = zIsWhiteSpace;
            node = node2;
        } else {
            zEquals = 7 == nodeType2 ? parentNode.getNodeName().toLowerCase().equals("xml") : false;
        }
        if (!zEquals) {
            int iAddNode2 = addNode(nextSibling, this.m_last_parent, this.m_last_kid, nodeType2);
            this.m_last_kid = iAddNode2;
            if (1 == nodeType2) {
                NamedNodeMap attributes = nextSibling.getAttributes();
                if (attributes != null) {
                    length = attributes.getLength();
                } else {
                    length = 0;
                }
                if (length > 0) {
                    iAddNode = -1;
                    for (int i2 = 0; i2 < length; i2++) {
                        iAddNode = addNode(attributes.item(i2), iAddNode2, iAddNode, -1);
                        this.m_firstch.setElementAt(-1, iAddNode);
                        if (!this.m_processedFirstElement && "xmlns:xml".equals(attributes.item(i2).getNodeName())) {
                            this.m_processedFirstElement = true;
                        }
                    }
                } else {
                    iAddNode = -1;
                }
                if (!this.m_processedFirstElement) {
                    iAddNode = addNode(new DOM2DTMdefaultNamespaceDeclarationNode((Element) nextSibling, "xml", "http://www.w3.org/XML/1998/namespace", makeNodeHandle((iAddNode == -1 ? iAddNode2 : iAddNode) + 1)), iAddNode2, iAddNode, -1);
                    this.m_firstch.setElementAt(-1, iAddNode);
                    this.m_processedFirstElement = true;
                }
                if (iAddNode != -1) {
                    this.m_nextsib.setElementAt(-1, iAddNode);
                }
            }
        }
        if (3 != nodeType2 && 4 != nodeType2) {
            node = nextSibling;
        }
        this.m_pos = node;
        return true;
    }

    @Override
    public Node getNode(int i) {
        return (Node) this.m_nodes.elementAt(makeNodeIdentity(i));
    }

    protected Node lookupNode(int i) {
        return (Node) this.m_nodes.elementAt(i);
    }

    @Override
    protected int getNextNodeIdentity(int i) {
        int i2 = i + 1;
        if (i2 >= this.m_nodes.size() && !nextNode()) {
            return -1;
        }
        return i2;
    }

    private int getHandleFromNode(Node node) {
        if (node != null) {
            int size = this.m_nodes.size();
            int i = 0;
            while (true) {
                if (i < size) {
                    if (this.m_nodes.elementAt(i) != node) {
                        i++;
                    } else {
                        return makeNodeHandle(i);
                    }
                } else {
                    boolean zNextNode = nextNode();
                    int size2 = this.m_nodes.size();
                    if (zNextNode || i < size2) {
                        size = size2;
                    } else {
                        return -1;
                    }
                }
            }
        } else {
            return -1;
        }
    }

    public int getHandleOfNode(Node node) {
        if (node != null) {
            if (this.m_root == node || ((this.m_root.getNodeType() == 9 && this.m_root == node.getOwnerDocument()) || (this.m_root.getNodeType() != 9 && this.m_root.getOwnerDocument() == node.getOwnerDocument()))) {
                Node parentNode = node;
                while (parentNode != null) {
                    if (parentNode != this.m_root) {
                        if (parentNode.getNodeType() != 2) {
                            parentNode = parentNode.getParentNode();
                        } else {
                            parentNode = ((Attr) parentNode).getOwnerElement();
                        }
                    } else {
                        return getHandleFromNode(node);
                    }
                }
                return -1;
            }
            return -1;
        }
        return -1;
    }

    @Override
    public int getAttributeNode(int i, String str, String str2) {
        short s_type;
        if (str == null) {
            str = "";
        }
        if (1 == getNodeType(i)) {
            int iMakeNodeIdentity = makeNodeIdentity(i);
            while (true) {
                iMakeNodeIdentity = getNextNodeIdentity(iMakeNodeIdentity);
                if (-1 == iMakeNodeIdentity || !((s_type = _type(iMakeNodeIdentity)) == 2 || s_type == 13)) {
                    break;
                }
                Node nodeLookupNode = lookupNode(iMakeNodeIdentity);
                String namespaceURI = nodeLookupNode.getNamespaceURI();
                if (namespaceURI == null) {
                    namespaceURI = "";
                }
                String localName = nodeLookupNode.getLocalName();
                if (namespaceURI.equals(str) && str2.equals(localName)) {
                    return makeNodeHandle(iMakeNodeIdentity);
                }
            }
        }
        return -1;
    }

    @Override
    public XMLString getStringValue(int i) {
        short nodeType = getNodeType(i);
        Node node = getNode(i);
        if (1 == nodeType || 9 == nodeType || 11 == nodeType) {
            FastStringBuffer fastStringBuffer = StringBufferPool.get();
            try {
                getNodeData(node, fastStringBuffer);
                String string = fastStringBuffer.length() > 0 ? fastStringBuffer.toString() : "";
                StringBufferPool.free(fastStringBuffer);
                return this.m_xstrf.newstr(string);
            } catch (Throwable th) {
                StringBufferPool.free(fastStringBuffer);
                throw th;
            }
        }
        if (3 == nodeType || 4 == nodeType) {
            FastStringBuffer fastStringBuffer2 = StringBufferPool.get();
            while (node != null) {
                fastStringBuffer2.append(node.getNodeValue());
                node = logicalNextDOMTextNode(node);
            }
            String string2 = fastStringBuffer2.length() > 0 ? fastStringBuffer2.toString() : "";
            StringBufferPool.free(fastStringBuffer2);
            return this.m_xstrf.newstr(string2);
        }
        return this.m_xstrf.newstr(node.getNodeValue());
    }

    public boolean isWhitespace(int i) {
        short nodeType = getNodeType(i);
        Node node = getNode(i);
        if (3 != nodeType && 4 != nodeType) {
            return false;
        }
        FastStringBuffer fastStringBuffer = StringBufferPool.get();
        while (node != null) {
            fastStringBuffer.append(node.getNodeValue());
            node = logicalNextDOMTextNode(node);
        }
        boolean zIsWhitespace = fastStringBuffer.isWhitespace(0, fastStringBuffer.length());
        StringBufferPool.free(fastStringBuffer);
        return zIsWhitespace;
    }

    protected static void getNodeData(Node node, FastStringBuffer fastStringBuffer) {
        short nodeType = node.getNodeType();
        if (nodeType != 7) {
            if (nodeType != 9 && nodeType != 11) {
                switch (nodeType) {
                    case 2:
                    case 3:
                    case 4:
                        fastStringBuffer.append(node.getNodeValue());
                        break;
                }
                return;
            }
            for (Node firstChild = node.getFirstChild(); firstChild != null; firstChild = firstChild.getNextSibling()) {
                getNodeData(firstChild, fastStringBuffer);
            }
        }
    }

    @Override
    public String getNodeName(int i) {
        return getNode(i).getNodeName();
    }

    @Override
    public String getNodeNameX(int i) {
        short nodeType = getNodeType(i);
        if (nodeType != 5 && nodeType != 7) {
            if (nodeType == 13) {
                String nodeName = getNode(i).getNodeName();
                if (nodeName.startsWith(Constants.ATTRNAME_XMLNS)) {
                    return QName.getLocalPart(nodeName);
                }
                if (nodeName.equals("xmlns")) {
                    return "";
                }
                return nodeName;
            }
            switch (nodeType) {
                case 1:
                case 2:
                    break;
                default:
                    return "";
            }
        }
        return getNode(i).getNodeName();
    }

    @Override
    public String getLocalName(int i) {
        int iMakeNodeIdentity = makeNodeIdentity(i);
        if (-1 == iMakeNodeIdentity) {
            return null;
        }
        Node node = (Node) this.m_nodes.elementAt(iMakeNodeIdentity);
        String localName = node.getLocalName();
        if (localName == null) {
            String nodeName = node.getNodeName();
            if ('#' == nodeName.charAt(0)) {
                return "";
            }
            int iIndexOf = nodeName.indexOf(58);
            if (iIndexOf >= 0) {
                nodeName = nodeName.substring(iIndexOf + 1);
            }
            return nodeName;
        }
        return localName;
    }

    @Override
    public String getPrefix(int i) {
        short nodeType = getNodeType(i);
        if (nodeType == 13) {
            String nodeName = getNode(i).getNodeName();
            int iIndexOf = nodeName.indexOf(58);
            return iIndexOf < 0 ? "" : nodeName.substring(iIndexOf + 1);
        }
        switch (nodeType) {
            case 1:
            case 2:
                String nodeName2 = getNode(i).getNodeName();
                int iIndexOf2 = nodeName2.indexOf(58);
                return iIndexOf2 < 0 ? "" : nodeName2.substring(0, iIndexOf2);
            default:
                return "";
        }
    }

    @Override
    public String getNamespaceURI(int i) {
        int iMakeNodeIdentity = makeNodeIdentity(i);
        if (iMakeNodeIdentity == -1) {
            return null;
        }
        return ((Node) this.m_nodes.elementAt(iMakeNodeIdentity)).getNamespaceURI();
    }

    private Node logicalNextDOMTextNode(Node node) {
        short nodeType;
        Node nextSibling = node.getNextSibling();
        if (nextSibling == null) {
            for (Node parentNode = node.getParentNode(); parentNode != null && 5 == parentNode.getNodeType(); parentNode = parentNode.getParentNode()) {
                nextSibling = parentNode.getNextSibling();
                if (nextSibling != null) {
                    break;
                }
            }
        }
        while (nextSibling != null && 5 == nextSibling.getNodeType()) {
            if (nextSibling.hasChildNodes()) {
                nextSibling = nextSibling.getFirstChild();
            } else {
                nextSibling = nextSibling.getNextSibling();
            }
        }
        if (nextSibling != null && 3 != (nodeType = nextSibling.getNodeType()) && 4 != nodeType) {
            return null;
        }
        return nextSibling;
    }

    @Override
    public String getNodeValue(int i) {
        short nodeType = -1 != _exptype(makeNodeIdentity(i)) ? getNodeType(i) : (short) -1;
        if (3 != nodeType && 4 != nodeType) {
            return getNode(i).getNodeValue();
        }
        Node node = getNode(i);
        Node nodeLogicalNextDOMTextNode = logicalNextDOMTextNode(node);
        if (nodeLogicalNextDOMTextNode == null) {
            return node.getNodeValue();
        }
        FastStringBuffer fastStringBuffer = StringBufferPool.get();
        fastStringBuffer.append(node.getNodeValue());
        while (nodeLogicalNextDOMTextNode != null) {
            fastStringBuffer.append(nodeLogicalNextDOMTextNode.getNodeValue());
            nodeLogicalNextDOMTextNode = logicalNextDOMTextNode(nodeLogicalNextDOMTextNode);
        }
        String string = fastStringBuffer.length() > 0 ? fastStringBuffer.toString() : "";
        StringBufferPool.free(fastStringBuffer);
        return string;
    }

    @Override
    public String getDocumentTypeDeclarationSystemIdentifier() {
        Document ownerDocument;
        DocumentType doctype;
        if (this.m_root.getNodeType() == 9) {
            ownerDocument = (Document) this.m_root;
        } else {
            ownerDocument = this.m_root.getOwnerDocument();
        }
        if (ownerDocument != null && (doctype = ownerDocument.getDoctype()) != null) {
            return doctype.getSystemId();
        }
        return null;
    }

    @Override
    public String getDocumentTypeDeclarationPublicIdentifier() {
        Document ownerDocument;
        DocumentType doctype;
        if (this.m_root.getNodeType() == 9) {
            ownerDocument = (Document) this.m_root;
        } else {
            ownerDocument = this.m_root.getOwnerDocument();
        }
        if (ownerDocument != null && (doctype = ownerDocument.getDoctype()) != null) {
            return doctype.getPublicId();
        }
        return null;
    }

    @Override
    public int getElementById(String str) {
        Element elementById;
        Document ownerDocument = this.m_root.getNodeType() == 9 ? (Document) this.m_root : this.m_root.getOwnerDocument();
        if (ownerDocument == null || (elementById = ownerDocument.getElementById(str)) == null) {
            return -1;
        }
        int handleFromNode = getHandleFromNode(elementById);
        if (-1 == handleFromNode) {
            int size = this.m_nodes.size() - 1;
            do {
                size = getNextNodeIdentity(size);
                if (-1 == size) {
                    return handleFromNode;
                }
            } while (getNode(size) != elementById);
            return getHandleFromNode(elementById);
        }
        return handleFromNode;
    }

    @Override
    public String getUnparsedEntityURI(String str) {
        DocumentType doctype;
        NamedNodeMap entities;
        Entity entity;
        Document ownerDocument = this.m_root.getNodeType() == 9 ? (Document) this.m_root : this.m_root.getOwnerDocument();
        if (ownerDocument == null || (doctype = ownerDocument.getDoctype()) == null || (entities = doctype.getEntities()) == null || (entity = (Entity) entities.getNamedItem(str)) == null || entity.getNotationName() == null) {
            return "";
        }
        String systemId = entity.getSystemId();
        if (systemId == null) {
            return entity.getPublicId();
        }
        return systemId;
    }

    @Override
    public boolean isAttributeSpecified(int i) {
        if (2 == getNodeType(i)) {
            return ((Attr) getNode(i)).getSpecified();
        }
        return false;
    }

    public void setIncrementalSAXSource(IncrementalSAXSource incrementalSAXSource) {
    }

    @Override
    public ContentHandler getContentHandler() {
        return null;
    }

    @Override
    public LexicalHandler getLexicalHandler() {
        return null;
    }

    @Override
    public EntityResolver getEntityResolver() {
        return null;
    }

    @Override
    public DTDHandler getDTDHandler() {
        return null;
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return null;
    }

    @Override
    public DeclHandler getDeclHandler() {
        return null;
    }

    @Override
    public boolean needsTwoThreads() {
        return false;
    }

    private static boolean isSpace(char c) {
        return XMLCharacterRecognizer.isWhiteSpace(c);
    }

    @Override
    public void dispatchCharactersEvents(int i, ContentHandler contentHandler, boolean z) throws SAXException {
        if (z) {
            getStringValue(i).fixWhiteSpace(true, true, false).dispatchCharactersEvents(contentHandler);
            return;
        }
        short nodeType = getNodeType(i);
        Node node = getNode(i);
        dispatchNodeData(node, contentHandler, 0);
        if (3 != nodeType && 4 != nodeType) {
            return;
        }
        while (true) {
            node = logicalNextDOMTextNode(node);
            if (node != null) {
                dispatchNodeData(node, contentHandler, 0);
            } else {
                return;
            }
        }
    }

    protected static void dispatchNodeData(Node node, ContentHandler contentHandler, int i) throws SAXException {
        switch (node.getNodeType()) {
            case 1:
            case 9:
            case 11:
                for (Node firstChild = node.getFirstChild(); firstChild != null; firstChild = firstChild.getNextSibling()) {
                    dispatchNodeData(firstChild, contentHandler, i + 1);
                }
                return;
            case 2:
            case 3:
            case 4:
                break;
            case 5:
            case 6:
            case 10:
            default:
                return;
            case 7:
            case 8:
                if (i != 0) {
                    return;
                }
                break;
        }
        String nodeValue = node.getNodeValue();
        if (contentHandler instanceof CharacterNodeHandler) {
            ((CharacterNodeHandler) contentHandler).characters(node);
        } else {
            contentHandler.characters(nodeValue.toCharArray(), 0, nodeValue.length());
        }
    }

    @Override
    public void dispatchToEvents(int i, ContentHandler contentHandler) throws SAXException {
        TreeWalker treeWalker = this.m_walker;
        if (treeWalker.getContentHandler() != null) {
            treeWalker = new TreeWalker(null);
        }
        treeWalker.setContentHandler(contentHandler);
        try {
            treeWalker.traverseFragment(getNode(i));
        } finally {
            treeWalker.setContentHandler(null);
        }
    }

    @Override
    public void setProperty(String str, Object obj) {
    }

    @Override
    public SourceLocator getSourceLocatorFor(int i) {
        return null;
    }
}
