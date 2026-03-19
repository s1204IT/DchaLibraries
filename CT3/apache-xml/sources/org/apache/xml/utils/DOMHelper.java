package org.apache.xml.utils;

import java.util.Hashtable;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.ref.DTMNodeProxy;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;
import org.apache.xml.serializer.SerializerConstants;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class DOMHelper {
    protected static final NSInfo m_NSInfoUnProcWithXMLNS = new NSInfo(false, true);
    protected static final NSInfo m_NSInfoUnProcWithoutXMLNS = new NSInfo(false, false);
    protected static final NSInfo m_NSInfoUnProcNoAncestorXMLNS = new NSInfo(false, false, 2);
    protected static final NSInfo m_NSInfoNullWithXMLNS = new NSInfo(true, true);
    protected static final NSInfo m_NSInfoNullWithoutXMLNS = new NSInfo(true, false);
    protected static final NSInfo m_NSInfoNullNoAncestorXMLNS = new NSInfo(true, false, 2);
    Hashtable m_NSInfos = new Hashtable();
    protected Vector m_candidateNoAncestorXMLNS = new Vector();
    protected Document m_DOMFactory = null;

    public static Document createDocument(boolean isSecureProcessing) {
        try {
            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
            dfactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
            Document outNode = docBuilder.newDocument();
            return outNode;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(XMLMessages.createXMLMessage(XMLErrorResources.ER_CREATEDOCUMENT_NOT_SUPPORTED, null));
        }
    }

    public static Document createDocument() {
        return createDocument(false);
    }

    public boolean shouldStripSourceNode(Node textNode) throws TransformerException {
        return false;
    }

    public String getUniqueID(Node node) {
        return "N" + Integer.toHexString(node.hashCode()).toUpperCase();
    }

    public static boolean isNodeAfter(Node node1, Node node2) {
        if (node1 == node2 || isNodeTheSame(node1, node2)) {
            return true;
        }
        Node parent1 = getParentOfNode(node1);
        Node parent2 = getParentOfNode(node2);
        if (parent1 == parent2 || isNodeTheSame(parent1, parent2)) {
            if (parent1 == null) {
                return true;
            }
            boolean isNodeAfter = isNodeAfterSibling(parent1, node1, node2);
            return isNodeAfter;
        }
        int nParents1 = 2;
        int nParents2 = 2;
        while (parent1 != null) {
            nParents1++;
            parent1 = getParentOfNode(parent1);
        }
        while (parent2 != null) {
            nParents2++;
            parent2 = getParentOfNode(parent2);
        }
        Node startNode1 = node1;
        Node startNode2 = node2;
        if (nParents1 < nParents2) {
            int adjust = nParents2 - nParents1;
            for (int i = 0; i < adjust; i++) {
                startNode2 = getParentOfNode(startNode2);
            }
        } else if (nParents1 > nParents2) {
            int adjust2 = nParents1 - nParents2;
            for (int i2 = 0; i2 < adjust2; i2++) {
                startNode1 = getParentOfNode(startNode1);
            }
        }
        Node prevChild1 = null;
        Node node = null;
        while (startNode1 != null) {
            if (startNode1 == startNode2 || isNodeTheSame(startNode1, startNode2)) {
                if (prevChild1 == null) {
                    return nParents1 < nParents2;
                }
                boolean isNodeAfter2 = isNodeAfterSibling(startNode1, prevChild1, node);
                return isNodeAfter2;
            }
            prevChild1 = startNode1;
            startNode1 = getParentOfNode(startNode1);
            node = startNode2;
            startNode2 = getParentOfNode(startNode2);
        }
        return true;
    }

    public static boolean isNodeTheSame(Node node, Node node2) {
        return ((node instanceof DTMNodeProxy) && (node2 instanceof DTMNodeProxy)) ? node.equals(node2) : node == node2;
    }

    private static boolean isNodeAfterSibling(Node parent, Node child1, Node child2) {
        short child1type = child1.getNodeType();
        short child2type = child2.getNodeType();
        if (2 != child1type && 2 == child2type) {
            return false;
        }
        if (2 == child1type && 2 != child2type) {
            return true;
        }
        if (2 == child1type) {
            NamedNodeMap children = parent.getAttributes();
            int nNodes = children.getLength();
            boolean found1 = false;
            boolean found2 = false;
            for (int i = 0; i < nNodes; i++) {
                Node child = children.item(i);
                if (child1 == child || isNodeTheSame(child1, child)) {
                    if (found2) {
                        return false;
                    }
                    found1 = true;
                } else if (child2 == child || isNodeTheSame(child2, child)) {
                    if (found1) {
                        return true;
                    }
                    found2 = true;
                }
            }
            return false;
        }
        boolean found12 = false;
        boolean found22 = false;
        for (Node child3 = parent.getFirstChild(); child3 != null; child3 = child3.getNextSibling()) {
            if (child1 == child3 || isNodeTheSame(child1, child3)) {
                if (found22) {
                    return false;
                }
                found12 = true;
            } else if (child2 == child3 || isNodeTheSame(child2, child3)) {
                if (found12) {
                    return true;
                }
                found22 = true;
            }
        }
        return false;
    }

    public short getLevel(Node n) {
        short level = 1;
        while (true) {
            n = getParentOfNode(n);
            if (n != null) {
                level = (short) (level + 1);
            } else {
                return level;
            }
        }
    }

    public String getNamespaceForPrefix(String prefix, Element namespaceContext) {
        String declname;
        Attr attr;
        if (prefix.equals("xml")) {
            return "http://www.w3.org/XML/1998/namespace";
        }
        if (prefix.equals("xmlns")) {
            return SerializerConstants.XMLNS_URI;
        }
        if (prefix == "") {
            declname = "xmlns";
        } else {
            declname = org.apache.xalan.templates.Constants.ATTRNAME_XMLNS + prefix;
        }
        for (Node parent = namespaceContext; parent != null; parent = getParentOfNode(parent)) {
            int type = parent.getNodeType();
            if (type != 1 && type != 5) {
                return null;
            }
            if (type == 1 && (attr = ((Element) parent).getAttributeNode(declname)) != null) {
                String namespace = attr.getNodeValue();
                return namespace;
            }
        }
        return null;
    }

    public String getNamespaceOfNode(Node n) {
        boolean hasProcessedNS;
        NSInfo nsInfo;
        String prefix;
        short ntype = n.getNodeType();
        if (2 != ntype) {
            Object nsObj = this.m_NSInfos.get(n);
            nsInfo = nsObj == null ? null : (NSInfo) nsObj;
            hasProcessedNS = nsInfo == null ? false : nsInfo.m_hasProcessedNS;
        } else {
            hasProcessedNS = false;
            nsInfo = null;
        }
        if (hasProcessedNS) {
            return nsInfo.m_namespace;
        }
        String namespaceOfPrefix = null;
        String nodeName = n.getNodeName();
        int indexOfNSSep = nodeName.indexOf(58);
        if (2 == ntype) {
            if (indexOfNSSep <= 0) {
                return null;
            }
            prefix = nodeName.substring(0, indexOfNSSep);
        } else {
            prefix = indexOfNSSep >= 0 ? nodeName.substring(0, indexOfNSSep) : "";
        }
        boolean ancestorsHaveXMLNS = false;
        boolean nHasXMLNS = false;
        if (prefix.equals("xml")) {
            namespaceOfPrefix = "http://www.w3.org/XML/1998/namespace";
        } else {
            Node parent = n;
            while (parent != null && namespaceOfPrefix == null && (nsInfo == null || nsInfo.m_ancestorHasXMLNSAttrs != 2)) {
                int parentType = parent.getNodeType();
                if (nsInfo == null || nsInfo.m_hasXMLNSAttrs) {
                    boolean elementHasXMLNS = false;
                    if (parentType == 1) {
                        NamedNodeMap nnm = parent.getAttributes();
                        int i = 0;
                        while (true) {
                            if (i >= nnm.getLength()) {
                                break;
                            }
                            Node attr = nnm.item(i);
                            String aname = attr.getNodeName();
                            if (aname.charAt(0) == 'x') {
                                boolean isPrefix = aname.startsWith(org.apache.xalan.templates.Constants.ATTRNAME_XMLNS);
                                if (aname.equals("xmlns") || isPrefix) {
                                    if (n == parent) {
                                        nHasXMLNS = true;
                                    }
                                    elementHasXMLNS = true;
                                    ancestorsHaveXMLNS = true;
                                    String p = isPrefix ? aname.substring(6) : "";
                                    if (p.equals(prefix)) {
                                        namespaceOfPrefix = attr.getNodeValue();
                                        break;
                                    }
                                }
                            }
                            i++;
                        }
                    }
                    if (2 != parentType && nsInfo == null && n != parent) {
                        nsInfo = elementHasXMLNS ? m_NSInfoUnProcWithXMLNS : m_NSInfoUnProcWithoutXMLNS;
                        this.m_NSInfos.put(parent, nsInfo);
                    }
                }
                if (2 == parentType) {
                    parent = getParentOfNode(parent);
                } else {
                    this.m_candidateNoAncestorXMLNS.addElement(parent);
                    this.m_candidateNoAncestorXMLNS.addElement(nsInfo);
                    parent = parent.getParentNode();
                }
                if (parent != null) {
                    Object nsObj2 = this.m_NSInfos.get(parent);
                    nsInfo = nsObj2 == null ? null : (NSInfo) nsObj2;
                }
            }
            int nCandidates = this.m_candidateNoAncestorXMLNS.size();
            if (nCandidates > 0) {
                if (!ancestorsHaveXMLNS && parent == null) {
                    for (int i2 = 0; i2 < nCandidates; i2 += 2) {
                        Object candidateInfo = this.m_candidateNoAncestorXMLNS.elementAt(i2 + 1);
                        if (candidateInfo == m_NSInfoUnProcWithoutXMLNS) {
                            this.m_NSInfos.put(this.m_candidateNoAncestorXMLNS.elementAt(i2), m_NSInfoUnProcNoAncestorXMLNS);
                        } else if (candidateInfo == m_NSInfoNullWithoutXMLNS) {
                            this.m_NSInfos.put(this.m_candidateNoAncestorXMLNS.elementAt(i2), m_NSInfoNullNoAncestorXMLNS);
                        }
                    }
                }
                this.m_candidateNoAncestorXMLNS.removeAllElements();
            }
        }
        if (2 != ntype) {
            if (namespaceOfPrefix == null) {
                if (ancestorsHaveXMLNS) {
                    if (nHasXMLNS) {
                        this.m_NSInfos.put(n, m_NSInfoNullWithXMLNS);
                        return namespaceOfPrefix;
                    }
                    this.m_NSInfos.put(n, m_NSInfoNullWithoutXMLNS);
                    return namespaceOfPrefix;
                }
                this.m_NSInfos.put(n, m_NSInfoNullNoAncestorXMLNS);
                return namespaceOfPrefix;
            }
            this.m_NSInfos.put(n, new NSInfo(namespaceOfPrefix, nHasXMLNS));
            return namespaceOfPrefix;
        }
        return namespaceOfPrefix;
    }

    public String getLocalNameOfNode(Node n) {
        String qname = n.getNodeName();
        int index = qname.indexOf(58);
        return index < 0 ? qname : qname.substring(index + 1);
    }

    public String getExpandedElementName(Element elem) {
        String namespace = getNamespaceOfNode(elem);
        if (namespace != null) {
            return namespace + ":" + getLocalNameOfNode(elem);
        }
        return getLocalNameOfNode(elem);
    }

    public String getExpandedAttributeName(Attr attr) {
        String namespace = getNamespaceOfNode(attr);
        if (namespace != null) {
            return namespace + ":" + getLocalNameOfNode(attr);
        }
        return getLocalNameOfNode(attr);
    }

    public boolean isIgnorableWhitespace(Text node) {
        return false;
    }

    public Node getRoot(Node node) {
        Node root = null;
        while (node != null) {
            root = node;
            node = getParentOfNode(node);
        }
        return root;
    }

    public Node getRootNode(Node n) {
        int nt = n.getNodeType();
        return (9 == nt || 11 == nt) ? n : n.getOwnerDocument();
    }

    public boolean isNamespaceNode(Node n) {
        if (2 == n.getNodeType()) {
            String attrName = n.getNodeName();
            if (attrName.startsWith(org.apache.xalan.templates.Constants.ATTRNAME_XMLNS)) {
                return true;
            }
            return attrName.equals("xmlns");
        }
        return false;
    }

    public static Node getParentOfNode(Node node) throws RuntimeException {
        short nodeType = node.getNodeType();
        if (2 == nodeType) {
            Document doc = node.getOwnerDocument();
            DOMImplementation impl = doc.getImplementation();
            if (impl != null && impl.hasFeature("Core", "2.0")) {
                Node parent = ((Attr) node).getOwnerElement();
                return parent;
            }
            Element rootElem = doc.getDocumentElement();
            if (rootElem == null) {
                throw new RuntimeException(XMLMessages.createXMLMessage(XMLErrorResources.ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT, null));
            }
            Node parent2 = locateAttrParent(rootElem, node);
            return parent2;
        }
        Node parent3 = node.getParentNode();
        return parent3;
    }

    public Element getElementByID(String id, Document doc) {
        return null;
    }

    public String getUnparsedEntityURI(String name, Document doc) {
        NamedNodeMap entities;
        Entity entity;
        DocumentType doctype = doc.getDoctype();
        if (doctype == null || (entities = doctype.getEntities()) == null || (entity = (Entity) entities.getNamedItem(name)) == null) {
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

    private static Node locateAttrParent(Element elem, Node attr) {
        Node parent = null;
        Attr check = elem.getAttributeNode(attr.getNodeName());
        if (check == attr) {
            parent = elem;
        }
        if (parent == null) {
            for (Node node = elem.getFirstChild(); node != null && (1 != node.getNodeType() || (parent = locateAttrParent((Element) node, attr)) == null); node = node.getNextSibling()) {
            }
        }
        return parent;
    }

    public void setDOMFactory(Document domFactory) {
        this.m_DOMFactory = domFactory;
    }

    public Document getDOMFactory() {
        if (this.m_DOMFactory == null) {
            this.m_DOMFactory = createDocument();
        }
        return this.m_DOMFactory;
    }

    public static String getNodeData(Node node) {
        FastStringBuffer buf = StringBufferPool.get();
        try {
            getNodeData(node, buf);
            String s = buf.length() > 0 ? buf.toString() : "";
            return s;
        } finally {
            StringBufferPool.free(buf);
        }
    }

    public static void getNodeData(Node node, FastStringBuffer buf) {
        switch (node.getNodeType()) {
            case 1:
            case 9:
            case 11:
                for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
                    getNodeData(child, buf);
                }
                break;
            case 2:
                buf.append(node.getNodeValue());
                break;
            case 3:
            case 4:
                buf.append(node.getNodeValue());
                break;
        }
    }
}
