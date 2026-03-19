package org.apache.xml.utils;

import java.util.Hashtable;
import java.util.Vector;
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

    public static Document createDocument(boolean z) {
        try {
            DocumentBuilderFactory documentBuilderFactoryNewInstance = DocumentBuilderFactory.newInstance();
            documentBuilderFactoryNewInstance.setNamespaceAware(true);
            return documentBuilderFactoryNewInstance.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(XMLMessages.createXMLMessage(XMLErrorResources.ER_CREATEDOCUMENT_NOT_SUPPORTED, null));
        }
    }

    public static Document createDocument() {
        return createDocument(false);
    }

    public boolean shouldStripSourceNode(Node node) throws TransformerException {
        return false;
    }

    public String getUniqueID(Node node) {
        return "N" + Integer.toHexString(node.hashCode()).toUpperCase();
    }

    public static boolean isNodeAfter(Node node, Node node2) {
        if (node == node2 || isNodeTheSame(node, node2)) {
            return true;
        }
        Node parentOfNode = getParentOfNode(node);
        Node parentOfNode2 = getParentOfNode(node2);
        if (parentOfNode == parentOfNode2 || isNodeTheSame(parentOfNode, parentOfNode2)) {
            if (parentOfNode == null) {
                return true;
            }
            return isNodeAfterSibling(parentOfNode, node, node2);
        }
        int i = 2;
        int i2 = 2;
        while (parentOfNode != null) {
            i2++;
            parentOfNode = getParentOfNode(parentOfNode);
        }
        while (parentOfNode2 != null) {
            i++;
            parentOfNode2 = getParentOfNode(parentOfNode2);
        }
        if (i2 < i) {
            int i3 = i - i2;
            Node parentOfNode3 = node2;
            for (int i4 = 0; i4 < i3; i4++) {
                parentOfNode3 = getParentOfNode(parentOfNode3);
            }
            node2 = parentOfNode3;
        } else if (i2 > i) {
            int i5 = i2 - i;
            Node parentOfNode4 = node;
            for (int i6 = 0; i6 < i5; i6++) {
                parentOfNode4 = getParentOfNode(parentOfNode4);
            }
            node = parentOfNode4;
        }
        Node node3 = null;
        Node parentOfNode5 = node;
        Node node4 = null;
        while (parentOfNode5 != null) {
            if (parentOfNode5 == node2 || isNodeTheSame(parentOfNode5, node2)) {
                if (node4 == null) {
                    if (i2 < i) {
                        return true;
                    }
                    return false;
                }
                return isNodeAfterSibling(parentOfNode5, node4, node3);
            }
            Node node5 = parentOfNode5;
            parentOfNode5 = getParentOfNode(parentOfNode5);
            node4 = node5;
            node3 = node2;
            node2 = getParentOfNode(node2);
        }
        return true;
    }

    public static boolean isNodeTheSame(Node node, Node node2) {
        if ((node instanceof DTMNodeProxy) && (node2 instanceof DTMNodeProxy)) {
            return ((DTMNodeProxy) node).equals(node2);
        }
        return node == node2;
    }

    private static boolean isNodeAfterSibling(Node node, Node node2, Node node3) {
        short nodeType = node2.getNodeType();
        short nodeType2 = node3.getNodeType();
        if (2 == nodeType || 2 != nodeType2) {
            if (2 == nodeType && 2 != nodeType2) {
                return true;
            }
            if (2 == nodeType) {
                NamedNodeMap attributes = node.getAttributes();
                int length = attributes.getLength();
                boolean z = false;
                boolean z2 = false;
                for (int i = 0; i < length; i++) {
                    Node nodeItem = attributes.item(i);
                    if (node2 == nodeItem || isNodeTheSame(node2, nodeItem)) {
                        if (z) {
                            break;
                        }
                        z2 = true;
                    } else if (node3 == nodeItem || isNodeTheSame(node3, nodeItem)) {
                        if (z2) {
                            return true;
                        }
                        z = true;
                    }
                }
                return false;
            }
            boolean z3 = false;
            boolean z4 = false;
            for (Node firstChild = node.getFirstChild(); firstChild != null; firstChild = firstChild.getNextSibling()) {
                if (node2 == firstChild || isNodeTheSame(node2, firstChild)) {
                    if (z3) {
                        break;
                    }
                    z4 = true;
                } else if (node3 == firstChild || isNodeTheSame(node3, firstChild)) {
                    if (z4) {
                        return true;
                    }
                    z3 = true;
                }
            }
        }
        return false;
    }

    public short getLevel(Node node) {
        short s = 1;
        while (true) {
            node = getParentOfNode(node);
            if (node != null) {
                s = (short) (s + 1);
            } else {
                return s;
            }
        }
    }

    public String getNamespaceForPrefix(String str, Element element) {
        String str2;
        Attr attributeNode;
        if (str.equals("xml")) {
            return "http://www.w3.org/XML/1998/namespace";
        }
        if (str.equals("xmlns")) {
            return SerializerConstants.XMLNS_URI;
        }
        if (str == "") {
            str2 = "xmlns";
        } else {
            str2 = org.apache.xalan.templates.Constants.ATTRNAME_XMLNS + str;
        }
        while (element != 0) {
            short nodeType = element.getNodeType();
            if (nodeType != 1 && nodeType != 5) {
                break;
            }
            if (nodeType == 1 && (attributeNode = ((Element) element).getAttributeNode(str2)) != null) {
                return attributeNode.getNodeValue();
            }
            element = getParentOfNode(element);
        }
        return null;
    }

    public String getNamespaceOfNode(Node node) {
        NSInfo nSInfo;
        boolean z;
        String strSubstring;
        boolean z2;
        String str;
        int i;
        Node parentNode;
        short nodeType = node.getNodeType();
        int i2 = 2;
        int i3 = 0;
        if (2 != nodeType) {
            Object obj = this.m_NSInfos.get(node);
            if (obj != null) {
                nSInfo = (NSInfo) obj;
            } else {
                nSInfo = null;
            }
            if (nSInfo != null) {
                z = nSInfo.m_hasProcessedNS;
            } else {
                z = false;
            }
        } else {
            nSInfo = null;
            z = false;
        }
        if (z) {
            return nSInfo.m_namespace;
        }
        String nodeName = node.getNodeName();
        int iIndexOf = nodeName.indexOf(58);
        if (2 == nodeType) {
            if (iIndexOf <= 0) {
                return null;
            }
            strSubstring = nodeName.substring(0, iIndexOf);
        } else {
            strSubstring = iIndexOf >= 0 ? nodeName.substring(0, iIndexOf) : "";
        }
        if (strSubstring.equals("xml")) {
            str = "http://www.w3.org/XML/1998/namespace";
            z2 = false;
        } else {
            String nodeValue = null;
            int i4 = 0;
            z2 = false;
            NSInfo nSInfo2 = nSInfo;
            Node node2 = node;
            while (node2 != null && nodeValue == null && (nSInfo2 == null || nSInfo2.m_ancestorHasXMLNSAttrs != i2)) {
                short nodeType2 = node2.getNodeType();
                if (nSInfo2 == null || nSInfo2.m_hasXMLNSAttrs) {
                    if (nodeType2 == 1) {
                        NamedNodeMap attributes = node2.getAttributes();
                        int i5 = i4;
                        boolean z3 = z2;
                        int i6 = i3;
                        int i7 = i6;
                        while (true) {
                            if (i6 < attributes.getLength()) {
                                Node nodeItem = attributes.item(i6);
                                String nodeName2 = nodeItem.getNodeName();
                                if (nodeName2.charAt(i3) == 'x') {
                                    boolean zStartsWith = nodeName2.startsWith(org.apache.xalan.templates.Constants.ATTRNAME_XMLNS);
                                    if (nodeName2.equals("xmlns") || zStartsWith) {
                                        if (node == node2) {
                                            z3 = true;
                                        }
                                        if (!(zStartsWith ? nodeName2.substring(6) : "").equals(strSubstring)) {
                                            i7 = 1;
                                            i5 = 1;
                                        } else {
                                            nodeValue = nodeItem.getNodeValue();
                                            z2 = z3;
                                            i4 = 1;
                                            i = 1;
                                            break;
                                        }
                                    }
                                }
                                i6++;
                                i3 = 0;
                            } else {
                                i = i7;
                                i4 = i5;
                                z2 = z3;
                                break;
                            }
                        }
                    } else {
                        i = 0;
                    }
                    if (2 != nodeType2 && nSInfo2 == null && node != node2) {
                        NSInfo nSInfo3 = i != 0 ? m_NSInfoUnProcWithXMLNS : m_NSInfoUnProcWithoutXMLNS;
                        this.m_NSInfos.put(node2, nSInfo3);
                        nSInfo2 = nSInfo3;
                    }
                }
                if (2 == nodeType2) {
                    parentNode = getParentOfNode(node2);
                } else {
                    this.m_candidateNoAncestorXMLNS.addElement(node2);
                    this.m_candidateNoAncestorXMLNS.addElement(nSInfo2);
                    parentNode = node2.getParentNode();
                }
                node2 = parentNode;
                if (node2 != null) {
                    Object obj2 = this.m_NSInfos.get(node2);
                    if (obj2 != null) {
                        nSInfo2 = (NSInfo) obj2;
                    } else {
                        nSInfo2 = null;
                    }
                }
                i2 = 2;
                i3 = 0;
            }
            int size = this.m_candidateNoAncestorXMLNS.size();
            if (size > 0) {
                if (i4 == 0 && node2 == null) {
                    for (int i8 = 0; i8 < size; i8 += 2) {
                        Object objElementAt = this.m_candidateNoAncestorXMLNS.elementAt(i8 + 1);
                        if (objElementAt == m_NSInfoUnProcWithoutXMLNS) {
                            this.m_NSInfos.put(this.m_candidateNoAncestorXMLNS.elementAt(i8), m_NSInfoUnProcNoAncestorXMLNS);
                        } else if (objElementAt == m_NSInfoNullWithoutXMLNS) {
                            this.m_NSInfos.put(this.m_candidateNoAncestorXMLNS.elementAt(i8), m_NSInfoNullNoAncestorXMLNS);
                        }
                    }
                }
                this.m_candidateNoAncestorXMLNS.removeAllElements();
            }
            str = nodeValue;
            i3 = i4;
        }
        if (2 != nodeType) {
            if (str != null) {
                this.m_NSInfos.put(node, new NSInfo(str, z2));
            } else if (i3 == 0) {
                this.m_NSInfos.put(node, m_NSInfoNullNoAncestorXMLNS);
            } else if (z2) {
                this.m_NSInfos.put(node, m_NSInfoNullWithXMLNS);
            } else {
                this.m_NSInfos.put(node, m_NSInfoNullWithoutXMLNS);
            }
        }
        return str;
    }

    public String getLocalNameOfNode(Node node) {
        String nodeName = node.getNodeName();
        int iIndexOf = nodeName.indexOf(58);
        return iIndexOf < 0 ? nodeName : nodeName.substring(iIndexOf + 1);
    }

    public String getExpandedElementName(Element element) {
        String namespaceOfNode = getNamespaceOfNode(element);
        if (namespaceOfNode != null) {
            return namespaceOfNode + ":" + getLocalNameOfNode(element);
        }
        return getLocalNameOfNode(element);
    }

    public String getExpandedAttributeName(Attr attr) {
        String namespaceOfNode = getNamespaceOfNode(attr);
        if (namespaceOfNode != null) {
            return namespaceOfNode + ":" + getLocalNameOfNode(attr);
        }
        return getLocalNameOfNode(attr);
    }

    public boolean isIgnorableWhitespace(Text text) {
        return false;
    }

    public Node getRoot(Node node) {
        Node node2 = null;
        while (true) {
            Node node3 = node2;
            node2 = node;
            if (node2 == null) {
                return node3;
            }
            node = getParentOfNode(node2);
        }
    }

    public Node getRootNode(Node node) {
        short nodeType = node.getNodeType();
        if (9 == nodeType || 11 == nodeType) {
            return node;
        }
        return node.getOwnerDocument();
    }

    public boolean isNamespaceNode(Node node) {
        if (2 != node.getNodeType()) {
            return false;
        }
        String nodeName = node.getNodeName();
        return nodeName.startsWith(org.apache.xalan.templates.Constants.ATTRNAME_XMLNS) || nodeName.equals("xmlns");
    }

    public static Node getParentOfNode(Node node) throws RuntimeException {
        if (2 == node.getNodeType()) {
            Document ownerDocument = node.getOwnerDocument();
            DOMImplementation implementation = ownerDocument.getImplementation();
            if (implementation != null && implementation.hasFeature("Core", "2.0")) {
                return ((Attr) node).getOwnerElement();
            }
            Element documentElement = ownerDocument.getDocumentElement();
            if (documentElement == null) {
                throw new RuntimeException(XMLMessages.createXMLMessage(XMLErrorResources.ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT, null));
            }
            return locateAttrParent(documentElement, node);
        }
        return node.getParentNode();
    }

    public Element getElementByID(String str, Document document) {
        return null;
    }

    public String getUnparsedEntityURI(String str, Document document) {
        NamedNodeMap entities;
        Entity entity;
        DocumentType doctype = document.getDoctype();
        if (doctype == null || (entities = doctype.getEntities()) == null || (entity = (Entity) entities.getNamedItem(str)) == null || entity.getNotationName() == null) {
            return "";
        }
        String systemId = entity.getSystemId();
        if (systemId == null) {
            return entity.getPublicId();
        }
        return systemId;
    }

    private static Node locateAttrParent(Element element, Node node) {
        Node nodeLocateAttrParent = element.getAttributeNode(node.getNodeName()) == node ? element : null;
        if (nodeLocateAttrParent == null) {
            for (Node firstChild = element.getFirstChild(); firstChild != null && (1 != firstChild.getNodeType() || (nodeLocateAttrParent = locateAttrParent((Element) firstChild, node)) == null); firstChild = firstChild.getNextSibling()) {
            }
        }
        return nodeLocateAttrParent;
    }

    public void setDOMFactory(Document document) {
        this.m_DOMFactory = document;
    }

    public Document getDOMFactory() {
        if (this.m_DOMFactory == null) {
            this.m_DOMFactory = createDocument();
        }
        return this.m_DOMFactory;
    }

    public static String getNodeData(Node node) {
        FastStringBuffer fastStringBuffer = StringBufferPool.get();
        try {
            getNodeData(node, fastStringBuffer);
            return fastStringBuffer.length() > 0 ? fastStringBuffer.toString() : "";
        } finally {
            StringBufferPool.free(fastStringBuffer);
        }
    }

    public static void getNodeData(Node node, FastStringBuffer fastStringBuffer) {
        short nodeType = node.getNodeType();
        if (nodeType != 7) {
            if (nodeType != 9 && nodeType != 11) {
                switch (nodeType) {
                    case 2:
                        fastStringBuffer.append(node.getNodeValue());
                        break;
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
}
