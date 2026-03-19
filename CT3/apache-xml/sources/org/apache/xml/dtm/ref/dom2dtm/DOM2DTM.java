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

    public DOM2DTM(DTMManager mgr, DOMSource domSource, int dtmIdentity, DTMWSFilter whiteSpaceFilter, XMLStringFactory xstringfactory, boolean doIndexing) {
        super(mgr, domSource, dtmIdentity, whiteSpaceFilter, xstringfactory, doIndexing);
        this.m_last_parent = 0;
        this.m_last_kid = -1;
        this.m_processedFirstElement = false;
        this.m_nodes = new Vector();
        this.m_walker = new TreeWalker(null);
        Node node = domSource.getNode();
        this.m_root = node;
        this.m_pos = node;
        this.m_last_kid = -1;
        this.m_last_parent = -1;
        this.m_last_kid = addNode(this.m_root, this.m_last_parent, this.m_last_kid, -1);
        if (1 == this.m_root.getNodeType()) {
            NamedNodeMap attrs = this.m_root.getAttributes();
            int attrsize = attrs == null ? 0 : attrs.getLength();
            if (attrsize > 0) {
                int attrIndex = -1;
                for (int i = 0; i < attrsize; i++) {
                    attrIndex = addNode(attrs.item(i), 0, attrIndex, -1);
                    this.m_firstch.setElementAt(-1, attrIndex);
                }
                this.m_nextsib.setElementAt(-1, attrIndex);
            }
        }
        this.m_nodesAreProcessed = false;
    }

    protected int addNode(Node node, int parentIndex, int previousSibling, int forceNodeType) {
        int type;
        String localName;
        int expandedNameID;
        int nodeIndex = this.m_nodes.size();
        if (this.m_dtmIdent.size() == (nodeIndex >>> 16)) {
            try {
                if (this.m_mgr == null) {
                    throw new ClassCastException();
                }
                DTMManagerDefault mgrD = (DTMManagerDefault) this.m_mgr;
                int id = mgrD.getFirstFreeDTMID();
                mgrD.addDTM(this, id, nodeIndex);
                this.m_dtmIdent.addElement(id << 16);
            } catch (ClassCastException e) {
                error(XMLMessages.createXMLMessage(XMLErrorResources.ER_NO_DTMIDS_AVAIL, null));
            }
        }
        this.m_size++;
        if (-1 == forceNodeType) {
            type = node.getNodeType();
        } else {
            type = forceNodeType;
        }
        if (2 == type) {
            String name = node.getNodeName();
            if (name.startsWith(Constants.ATTRNAME_XMLNS) || name.equals("xmlns")) {
                type = 13;
            }
        }
        this.m_nodes.addElement(node);
        this.m_firstch.setElementAt(-2, nodeIndex);
        this.m_nextsib.setElementAt(-2, nodeIndex);
        this.m_prevsib.setElementAt(previousSibling, nodeIndex);
        this.m_parent.setElementAt(parentIndex, nodeIndex);
        if (-1 != parentIndex && type != 2 && type != 13 && -2 == this.m_firstch.elementAt(parentIndex)) {
            this.m_firstch.setElementAt(nodeIndex, parentIndex);
        }
        String nsURI = node.getNamespaceURI();
        if (type == 7) {
            localName = node.getNodeName();
        } else {
            localName = node.getLocalName();
        }
        if ((type == 1 || type == 2) && localName == null) {
            localName = node.getNodeName();
        }
        ExpandedNameTable exnt = this.m_expandedNameTable;
        if (node.getLocalName() != null || type == 1 || type == 2) {
        }
        if (localName != null) {
            expandedNameID = exnt.getExpandedTypeID(nsURI, localName, type);
        } else {
            expandedNameID = exnt.getExpandedTypeID(type);
        }
        this.m_exptype.setElementAt(expandedNameID, nodeIndex);
        indexNode(expandedNameID, nodeIndex);
        if (-1 != previousSibling) {
            this.m_nextsib.setElementAt(nodeIndex, previousSibling);
        }
        if (type == 13) {
            declareNamespaceInContext(parentIndex, nodeIndex);
        }
        return nodeIndex;
    }

    @Override
    public int getNumberOfNodes() {
        return this.m_nodes.size();
    }

    @Override
    protected boolean nextNode() {
        boolean shouldStrip;
        if (this.m_nodesAreProcessed) {
            return false;
        }
        Node pos = this.m_pos;
        Node next = null;
        int nexttype = -1;
        do {
            if (pos.hasChildNodes()) {
                next = pos.getFirstChild();
                if (next != null && 10 == next.getNodeType()) {
                    next = next.getNextSibling();
                }
                if (5 != pos.getNodeType()) {
                    this.m_last_parent = this.m_last_kid;
                    this.m_last_kid = -1;
                    if (this.m_wsfilter != null) {
                        short wsv = this.m_wsfilter.getShouldStripSpace(makeNodeHandle(this.m_last_parent), this);
                        if (3 == wsv) {
                            shouldStrip = getShouldStripWhitespace();
                        } else {
                            shouldStrip = 2 == wsv;
                        }
                        pushShouldStripWhitespace(shouldStrip);
                    }
                }
            } else {
                if (this.m_last_kid != -1 && this.m_firstch.elementAt(this.m_last_kid) == -2) {
                    this.m_firstch.setElementAt(-1, this.m_last_kid);
                }
                while (this.m_last_parent != -1) {
                    next = pos.getNextSibling();
                    if (next != null && 10 == next.getNodeType()) {
                        next = next.getNextSibling();
                    }
                    if (next != null) {
                        break;
                    }
                    pos = pos.getParentNode();
                    if (pos == null) {
                    }
                    if (pos == null || 5 != pos.getNodeType()) {
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
                    next = null;
                }
            }
            if (next != null) {
                nexttype = next.getNodeType();
            }
            if (5 == nexttype) {
                pos = next;
            }
        } while (5 == nexttype);
        if (next == null) {
            this.m_nextsib.setElementAt(-1, 0);
            this.m_nodesAreProcessed = true;
            this.m_pos = null;
            return false;
        }
        boolean suppressNode = false;
        Node lastTextNode = null;
        int nexttype2 = next.getNodeType();
        if (3 == nexttype2 || 4 == nexttype2) {
            suppressNode = this.m_wsfilter != null ? getShouldStripWhitespace() : false;
            Node n = next;
            while (n != null) {
                lastTextNode = n;
                if (3 == n.getNodeType()) {
                    nexttype2 = 3;
                }
                suppressNode &= XMLCharacterRecognizer.isWhiteSpace(n.getNodeValue());
                n = logicalNextDOMTextNode(n);
            }
        } else if (7 == nexttype2) {
            suppressNode = pos.getNodeName().toLowerCase().equals("xml");
        }
        if (!suppressNode) {
            int nextindex = addNode(next, this.m_last_parent, this.m_last_kid, nexttype2);
            this.m_last_kid = nextindex;
            if (1 == nexttype2) {
                int attrIndex = -1;
                NamedNodeMap attrs = next.getAttributes();
                int attrsize = attrs == null ? 0 : attrs.getLength();
                if (attrsize > 0) {
                    for (int i2 = 0; i2 < attrsize; i2++) {
                        attrIndex = addNode(attrs.item(i2), nextindex, attrIndex, -1);
                        this.m_firstch.setElementAt(-1, attrIndex);
                        if (!this.m_processedFirstElement && "xmlns:xml".equals(attrs.item(i2).getNodeName())) {
                            this.m_processedFirstElement = true;
                        }
                    }
                }
                if (!this.m_processedFirstElement) {
                    attrIndex = addNode(new DOM2DTMdefaultNamespaceDeclarationNode((Element) next, "xml", "http://www.w3.org/XML/1998/namespace", makeNodeHandle((attrIndex == -1 ? nextindex : attrIndex) + 1)), nextindex, attrIndex, -1);
                    this.m_firstch.setElementAt(-1, attrIndex);
                    this.m_processedFirstElement = true;
                }
                if (attrIndex != -1) {
                    this.m_nextsib.setElementAt(-1, attrIndex);
                }
            }
        }
        if (3 == nexttype2 || 4 == nexttype2) {
            next = lastTextNode;
        }
        this.m_pos = next;
        return true;
    }

    @Override
    public Node getNode(int nodeHandle) {
        int identity = makeNodeIdentity(nodeHandle);
        return (Node) this.m_nodes.elementAt(identity);
    }

    protected Node lookupNode(int nodeIdentity) {
        return (Node) this.m_nodes.elementAt(nodeIdentity);
    }

    @Override
    protected int getNextNodeIdentity(int identity) {
        int identity2 = identity + 1;
        if (identity2 >= this.m_nodes.size() && !nextNode()) {
            return -1;
        }
        return identity2;
    }

    private int getHandleFromNode(Node node) {
        if (node != null) {
            int len = this.m_nodes.size();
            int i = 0;
            while (true) {
                if (i < len) {
                    if (this.m_nodes.elementAt(i) != node) {
                        i++;
                    } else {
                        return makeNodeHandle(i);
                    }
                } else {
                    boolean isMore = nextNode();
                    len = this.m_nodes.size();
                    if (!isMore && i >= len) {
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
                Node cursor = node;
                while (cursor != null) {
                    if (cursor != this.m_root) {
                        if (cursor.getNodeType() != 2) {
                            cursor = cursor.getParentNode();
                        } else {
                            cursor = ((Attr) cursor).getOwnerElement();
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
    public int getAttributeNode(int nodeHandle, String namespaceURI, String name) {
        int type;
        if (namespaceURI == null) {
            namespaceURI = "";
        }
        if (1 == getNodeType(nodeHandle)) {
            int identity = makeNodeIdentity(nodeHandle);
            while (true) {
                identity = getNextNodeIdentity(identity);
                if (-1 == identity || !((type = _type(identity)) == 2 || type == 13)) {
                    break;
                }
                Node node = lookupNode(identity);
                String nodeuri = node.getNamespaceURI();
                if (nodeuri == null) {
                    nodeuri = "";
                }
                String nodelocalname = node.getLocalName();
                if (nodeuri.equals(namespaceURI) && name.equals(nodelocalname)) {
                    return makeNodeHandle(identity);
                }
            }
        }
        return -1;
    }

    @Override
    public XMLString getStringValue(int nodeHandle) {
        int type = getNodeType(nodeHandle);
        Node node = getNode(nodeHandle);
        if (1 == type || 9 == type || 11 == type) {
            FastStringBuffer buf = StringBufferPool.get();
            try {
                getNodeData(node, buf);
                String s = buf.length() > 0 ? buf.toString() : "";
                StringBufferPool.free(buf);
                return this.m_xstrf.newstr(s);
            } catch (Throwable th) {
                StringBufferPool.free(buf);
                throw th;
            }
        }
        if (3 == type || 4 == type) {
            FastStringBuffer buf2 = StringBufferPool.get();
            while (node != null) {
                buf2.append(node.getNodeValue());
                node = logicalNextDOMTextNode(node);
            }
            String s2 = buf2.length() > 0 ? buf2.toString() : "";
            StringBufferPool.free(buf2);
            return this.m_xstrf.newstr(s2);
        }
        return this.m_xstrf.newstr(node.getNodeValue());
    }

    public boolean isWhitespace(int nodeHandle) {
        int type = getNodeType(nodeHandle);
        Node node = getNode(nodeHandle);
        if (3 != type && 4 != type) {
            return false;
        }
        FastStringBuffer buf = StringBufferPool.get();
        while (node != null) {
            buf.append(node.getNodeValue());
            node = logicalNextDOMTextNode(node);
        }
        boolean b = buf.isWhitespace(0, buf.length());
        StringBufferPool.free(buf);
        return b;
    }

    protected static void getNodeData(Node node, FastStringBuffer buf) {
        switch (node.getNodeType()) {
            case 1:
            case 9:
            case 11:
                for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
                    getNodeData(child, buf);
                }
                break;
            case 2:
            case 3:
            case 4:
                buf.append(node.getNodeValue());
                break;
        }
    }

    @Override
    public String getNodeName(int nodeHandle) {
        Node node = getNode(nodeHandle);
        return node.getNodeName();
    }

    @Override
    public String getNodeNameX(int nodeHandle) {
        short type = getNodeType(nodeHandle);
        switch (type) {
            case 1:
            case 2:
            case 5:
            case 7:
                Node node = getNode(nodeHandle);
                return node.getNodeName();
            case 13:
                Node node2 = getNode(nodeHandle);
                String name = node2.getNodeName();
                if (name.startsWith(Constants.ATTRNAME_XMLNS)) {
                    return QName.getLocalPart(name);
                }
                if (name.equals("xmlns")) {
                    return "";
                }
                return name;
            default:
                return "";
        }
    }

    @Override
    public String getLocalName(int nodeHandle) {
        int id = makeNodeIdentity(nodeHandle);
        if (-1 == id) {
            return null;
        }
        Node newnode = (Node) this.m_nodes.elementAt(id);
        String newname = newnode.getLocalName();
        if (newname == null) {
            String qname = newnode.getNodeName();
            if ('#' == qname.charAt(0)) {
                return "";
            }
            int index = qname.indexOf(58);
            return index < 0 ? qname : qname.substring(index + 1);
        }
        return newname;
    }

    @Override
    public String getPrefix(int nodeHandle) {
        short type = getNodeType(nodeHandle);
        switch (type) {
            case 1:
            case 2:
                Node node = getNode(nodeHandle);
                String qname = node.getNodeName();
                int index = qname.indexOf(58);
                if (index < 0) {
                    return "";
                }
                String prefix = qname.substring(0, index);
                return prefix;
            case 13:
                Node node2 = getNode(nodeHandle);
                String qname2 = node2.getNodeName();
                int index2 = qname2.indexOf(58);
                if (index2 < 0) {
                    return "";
                }
                String prefix2 = qname2.substring(index2 + 1);
                return prefix2;
            default:
                return "";
        }
    }

    @Override
    public String getNamespaceURI(int nodeHandle) {
        int id = makeNodeIdentity(nodeHandle);
        if (id == -1) {
            return null;
        }
        Node node = (Node) this.m_nodes.elementAt(id);
        return node.getNamespaceURI();
    }

    private Node logicalNextDOMTextNode(Node n) {
        int ntype;
        Node p = n.getNextSibling();
        if (p == null) {
            for (Node n2 = n.getParentNode(); n2 != null && 5 == n2.getNodeType(); n2 = n2.getParentNode()) {
                p = n2.getNextSibling();
                if (p != null) {
                    break;
                }
            }
        }
        Node n3 = p;
        while (n3 != null && 5 == n3.getNodeType()) {
            if (n3.hasChildNodes()) {
                n3 = n3.getFirstChild();
            } else {
                n3 = n3.getNextSibling();
            }
        }
        if (n3 != null && 3 != (ntype = n3.getNodeType()) && 4 != ntype) {
            return null;
        }
        return n3;
    }

    @Override
    public String getNodeValue(int nodeHandle) {
        int type = -1 != _exptype(makeNodeIdentity(nodeHandle)) ? getNodeType(nodeHandle) : -1;
        if (3 != type && 4 != type) {
            return getNode(nodeHandle).getNodeValue();
        }
        Node node = getNode(nodeHandle);
        Node n = logicalNextDOMTextNode(node);
        if (n == null) {
            return node.getNodeValue();
        }
        FastStringBuffer buf = StringBufferPool.get();
        buf.append(node.getNodeValue());
        while (n != null) {
            buf.append(n.getNodeValue());
            n = logicalNextDOMTextNode(n);
        }
        String s = buf.length() > 0 ? buf.toString() : "";
        StringBufferPool.free(buf);
        return s;
    }

    @Override
    public String getDocumentTypeDeclarationSystemIdentifier() {
        Document doc;
        DocumentType dtd;
        if (this.m_root.getNodeType() == 9) {
            doc = (Document) this.m_root;
        } else {
            doc = this.m_root.getOwnerDocument();
        }
        if (doc == null || (dtd = doc.getDoctype()) == null) {
            return null;
        }
        return dtd.getSystemId();
    }

    @Override
    public String getDocumentTypeDeclarationPublicIdentifier() {
        Document doc;
        DocumentType dtd;
        if (this.m_root.getNodeType() == 9) {
            doc = (Document) this.m_root;
        } else {
            doc = this.m_root.getOwnerDocument();
        }
        if (doc == null || (dtd = doc.getDoctype()) == null) {
            return null;
        }
        return dtd.getPublicId();
    }

    @Override
    public int getElementById(String elementId) {
        Node elem;
        Node node;
        Document doc = this.m_root.getNodeType() == 9 ? (Document) this.m_root : this.m_root.getOwnerDocument();
        if (doc == null || (elem = doc.getElementById(elementId)) == null) {
            return -1;
        }
        int elemHandle = getHandleFromNode(elem);
        if (-1 == elemHandle) {
            int identity = this.m_nodes.size() - 1;
            do {
                identity = getNextNodeIdentity(identity);
                if (-1 != identity) {
                    node = getNode(identity);
                } else {
                    return elemHandle;
                }
            } while (node != elem);
            return getHandleFromNode(elem);
        }
        return elemHandle;
    }

    @Override
    public String getUnparsedEntityURI(String name) {
        DocumentType doctype;
        NamedNodeMap entities;
        Entity entity;
        Document doc = this.m_root.getNodeType() == 9 ? (Document) this.m_root : this.m_root.getOwnerDocument();
        if (doc == null || (doctype = doc.getDoctype()) == null || (entities = doctype.getEntities()) == null || (entity = (Entity) entities.getNamedItem(name)) == null) {
            return "";
        }
        String notationName = entity.getNotationName();
        if (notationName == null) {
            return "";
        }
        String url = entity.getSystemId();
        if (url == null) {
            return entity.getPublicId();
        }
        return url;
    }

    @Override
    public boolean isAttributeSpecified(int attributeHandle) {
        int type = getNodeType(attributeHandle);
        if (2 == type) {
            Attr attr = (Attr) getNode(attributeHandle);
            return attr.getSpecified();
        }
        return false;
    }

    public void setIncrementalSAXSource(IncrementalSAXSource source) {
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

    private static boolean isSpace(char ch) {
        return XMLCharacterRecognizer.isWhiteSpace(ch);
    }

    @Override
    public void dispatchCharactersEvents(int nodeHandle, ContentHandler ch, boolean normalize) throws SAXException {
        if (normalize) {
            XMLString str = getStringValue(nodeHandle);
            str.fixWhiteSpace(true, true, false).dispatchCharactersEvents(ch);
            return;
        }
        int type = getNodeType(nodeHandle);
        Node node = getNode(nodeHandle);
        dispatchNodeData(node, ch, 0);
        if (3 != type && 4 != type) {
            return;
        }
        while (true) {
            node = logicalNextDOMTextNode(node);
            if (node == null) {
                return;
            } else {
                dispatchNodeData(node, ch, 0);
            }
        }
    }

    protected static void dispatchNodeData(Node node, ContentHandler ch, int depth) throws SAXException {
        switch (node.getNodeType()) {
            case 1:
            case 9:
            case 11:
                for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
                    dispatchNodeData(child, ch, depth + 1);
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
                if (depth != 0) {
                    return;
                }
                break;
        }
        String str = node.getNodeValue();
        if (ch instanceof CharacterNodeHandler) {
            ((CharacterNodeHandler) ch).characters(node);
        } else {
            ch.characters(str.toCharArray(), 0, str.length());
        }
    }

    @Override
    public void dispatchToEvents(int nodeHandle, ContentHandler ch) throws SAXException {
        TreeWalker treeWalker = this.m_walker;
        ContentHandler prevCH = treeWalker.getContentHandler();
        if (prevCH != null) {
            treeWalker = new TreeWalker(null);
        }
        treeWalker.setContentHandler(ch);
        try {
            Node node = getNode(nodeHandle);
            treeWalker.traverseFragment(node);
        } finally {
            treeWalker.setContentHandler(null);
        }
    }

    @Override
    public void setProperty(String property, Object value) {
    }

    @Override
    public SourceLocator getSourceLocatorFor(int node) {
        return null;
    }
}
