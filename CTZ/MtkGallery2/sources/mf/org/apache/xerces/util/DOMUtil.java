package mf.org.apache.xerces.util;

import java.lang.reflect.Method;
import java.util.Hashtable;
import mf.org.apache.xerces.dom.AttrImpl;
import mf.org.apache.xerces.dom.DocumentImpl;
import mf.org.apache.xerces.impl.xs.opti.ElementImpl;
import mf.org.apache.xerces.impl.xs.opti.NodeImpl;
import mf.org.w3c.dom.Attr;
import mf.org.w3c.dom.DOMException;
import mf.org.w3c.dom.Document;
import mf.org.w3c.dom.Element;
import mf.org.w3c.dom.NamedNodeMap;
import mf.org.w3c.dom.Node;
import mf.org.w3c.dom.ls.LSException;

public class DOMUtil {
    protected DOMUtil() {
    }

    public static void copyInto(Node src, Node dest) throws DOMException {
        Node node;
        Document factory;
        Document factory2 = dest.getOwnerDocument();
        boolean domimpl = factory2 instanceof DocumentImpl;
        Node place = src;
        Node parent = src;
        Node dest2 = dest;
        while (place != null) {
            int type = place.getNodeType();
            switch (type) {
                case 1:
                    Element element = factory2.createElement(place.getNodeName());
                    NamedNodeMap attrs = place.getAttributes();
                    int attrCount = attrs.getLength();
                    int i = 0;
                    while (i < attrCount) {
                        Attr attr = (Attr) attrs.item(i);
                        String attrName = attr.getNodeName();
                        String attrValue = attr.getNodeValue();
                        element.setAttribute(attrName, attrValue);
                        if (!domimpl || attr.getSpecified()) {
                            factory = factory2;
                        } else {
                            factory = factory2;
                            ((AttrImpl) element.getAttributeNode(attrName)).setSpecified(false);
                        }
                        i++;
                        factory2 = factory;
                    }
                    node = element;
                    break;
                case 2:
                case 6:
                default:
                    throw new IllegalArgumentException("can't copy node type, " + type + " (" + place.getNodeName() + ')');
                case 3:
                    node = factory2.createTextNode(place.getNodeValue());
                    break;
                case 4:
                    node = factory2.createCDATASection(place.getNodeValue());
                    break;
                case 5:
                    node = factory2.createEntityReference(place.getNodeName());
                    break;
                case 7:
                    node = factory2.createProcessingInstruction(place.getNodeName(), place.getNodeValue());
                    break;
                case 8:
                    node = factory2.createComment(place.getNodeValue());
                    break;
            }
            dest2.appendChild(node);
            if (place.hasChildNodes()) {
                parent = place;
                place = place.getFirstChild();
                dest2 = node;
            } else {
                place = place.getNextSibling();
                while (place == null && parent != src) {
                    place = parent.getNextSibling();
                    parent = parent.getParentNode();
                    dest2 = dest2.getParentNode();
                }
            }
        }
    }

    public static Element getFirstChildElement(Node parent) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == 1) {
                return (Element) child;
            }
        }
        return null;
    }

    public static Element getFirstVisibleChildElement(Node parent) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == 1 && !isHidden(child)) {
                return (Element) child;
            }
        }
        return null;
    }

    public static Element getFirstVisibleChildElement(Node parent, Hashtable hiddenNodes) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == 1 && !isHidden(child, hiddenNodes)) {
                return (Element) child;
            }
        }
        return null;
    }

    public static Element getLastChildElement(Node parent) {
        for (Node child = parent.getLastChild(); child != null; child = child.getPreviousSibling()) {
            if (child.getNodeType() == 1) {
                return (Element) child;
            }
        }
        return null;
    }

    public static Element getLastVisibleChildElement(Node parent) {
        for (Node child = parent.getLastChild(); child != null; child = child.getPreviousSibling()) {
            if (child.getNodeType() == 1 && !isHidden(child)) {
                return (Element) child;
            }
        }
        return null;
    }

    public static Element getLastVisibleChildElement(Node parent, Hashtable hiddenNodes) {
        for (Node child = parent.getLastChild(); child != null; child = child.getPreviousSibling()) {
            if (child.getNodeType() == 1 && !isHidden(child, hiddenNodes)) {
                return (Element) child;
            }
        }
        return null;
    }

    public static Element getNextSiblingElement(Node node) {
        for (Node sibling = node.getNextSibling(); sibling != null; sibling = sibling.getNextSibling()) {
            if (sibling.getNodeType() == 1) {
                return (Element) sibling;
            }
        }
        return null;
    }

    public static Element getNextVisibleSiblingElement(Node node) {
        for (Node sibling = node.getNextSibling(); sibling != null; sibling = sibling.getNextSibling()) {
            if (sibling.getNodeType() == 1 && !isHidden(sibling)) {
                return (Element) sibling;
            }
        }
        return null;
    }

    public static Element getNextVisibleSiblingElement(Node node, Hashtable hiddenNodes) {
        for (Node sibling = node.getNextSibling(); sibling != null; sibling = sibling.getNextSibling()) {
            if (sibling.getNodeType() == 1 && !isHidden(sibling, hiddenNodes)) {
                return (Element) sibling;
            }
        }
        return null;
    }

    public static void setHidden(Node node) {
        if (node instanceof NodeImpl) {
            ((NodeImpl) node).setReadOnly(true, false);
        } else if (node instanceof mf.org.apache.xerces.dom.NodeImpl) {
            node.setReadOnly(true, false);
        }
    }

    public static void setHidden(Node node, Hashtable hiddenNodes) {
        if (node instanceof NodeImpl) {
            node.setReadOnly(true, false);
        } else {
            hiddenNodes.put(node, "");
        }
    }

    public static void setVisible(Node node) {
        if (node instanceof NodeImpl) {
            ((NodeImpl) node).setReadOnly(false, false);
        } else if (node instanceof mf.org.apache.xerces.dom.NodeImpl) {
            node.setReadOnly(false, false);
        }
    }

    public static void setVisible(Node node, Hashtable hiddenNodes) {
        if (node instanceof NodeImpl) {
            node.setReadOnly(false, false);
        } else {
            hiddenNodes.remove(node);
        }
    }

    public static boolean isHidden(Node node) {
        if (node instanceof NodeImpl) {
            return node.getReadOnly();
        }
        if (node instanceof mf.org.apache.xerces.dom.NodeImpl) {
            return node.getReadOnly();
        }
        return false;
    }

    public static boolean isHidden(Node node, Hashtable hiddenNodes) {
        if (node instanceof NodeImpl) {
            return node.getReadOnly();
        }
        return hiddenNodes.containsKey(node);
    }

    public static Element getFirstChildElement(Node parent, String elemName) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == 1 && child.getNodeName().equals(elemName)) {
                return (Element) child;
            }
        }
        return null;
    }

    public static Element getLastChildElement(Node parent, String elemName) {
        for (Node child = parent.getLastChild(); child != null; child = child.getPreviousSibling()) {
            if (child.getNodeType() == 1 && child.getNodeName().equals(elemName)) {
                return (Element) child;
            }
        }
        return null;
    }

    public static Element getNextSiblingElement(Node node, String elemName) {
        for (Node sibling = node.getNextSibling(); sibling != null; sibling = sibling.getNextSibling()) {
            if (sibling.getNodeType() == 1 && sibling.getNodeName().equals(elemName)) {
                return (Element) sibling;
            }
        }
        return null;
    }

    public static Element getFirstChildElementNS(Node parent, String uri, String localpart) {
        String childURI;
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == 1 && (childURI = child.getNamespaceURI()) != null && childURI.equals(uri) && child.getLocalName().equals(localpart)) {
                return (Element) child;
            }
        }
        return null;
    }

    public static Element getLastChildElementNS(Node parent, String uri, String localpart) {
        String childURI;
        for (Node child = parent.getLastChild(); child != null; child = child.getPreviousSibling()) {
            if (child.getNodeType() == 1 && (childURI = child.getNamespaceURI()) != null && childURI.equals(uri) && child.getLocalName().equals(localpart)) {
                return (Element) child;
            }
        }
        return null;
    }

    public static Element getNextSiblingElementNS(Node node, String uri, String localpart) {
        String siblingURI;
        for (Node sibling = node.getNextSibling(); sibling != null; sibling = sibling.getNextSibling()) {
            if (sibling.getNodeType() == 1 && (siblingURI = sibling.getNamespaceURI()) != null && siblingURI.equals(uri) && sibling.getLocalName().equals(localpart)) {
                return (Element) sibling;
            }
        }
        return null;
    }

    public static Element getFirstChildElement(Node parent, String[] elemNames) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == 1) {
                for (String str : elemNames) {
                    if (child.getNodeName().equals(str)) {
                        return (Element) child;
                    }
                }
            }
        }
        return null;
    }

    public static Element getLastChildElement(Node parent, String[] elemNames) {
        for (Node child = parent.getLastChild(); child != null; child = child.getPreviousSibling()) {
            if (child.getNodeType() == 1) {
                for (String str : elemNames) {
                    if (child.getNodeName().equals(str)) {
                        return (Element) child;
                    }
                }
            }
        }
        return null;
    }

    public static Element getNextSiblingElement(Node node, String[] elemNames) {
        for (Node sibling = node.getNextSibling(); sibling != null; sibling = sibling.getNextSibling()) {
            if (sibling.getNodeType() == 1) {
                for (String str : elemNames) {
                    if (sibling.getNodeName().equals(str)) {
                        return (Element) sibling;
                    }
                }
            }
        }
        return null;
    }

    public static Element getFirstChildElementNS(Node parent, String[][] elemNames) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == 1) {
                for (int i = 0; i < elemNames.length; i++) {
                    String uri = child.getNamespaceURI();
                    if (uri != null && uri.equals(elemNames[i][0]) && child.getLocalName().equals(elemNames[i][1])) {
                        return (Element) child;
                    }
                }
            }
        }
        return null;
    }

    public static Element getLastChildElementNS(Node parent, String[][] elemNames) {
        for (Node child = parent.getLastChild(); child != null; child = child.getPreviousSibling()) {
            if (child.getNodeType() == 1) {
                for (int i = 0; i < elemNames.length; i++) {
                    String uri = child.getNamespaceURI();
                    if (uri != null && uri.equals(elemNames[i][0]) && child.getLocalName().equals(elemNames[i][1])) {
                        return (Element) child;
                    }
                }
            }
        }
        return null;
    }

    public static Element getNextSiblingElementNS(Node node, String[][] elemNames) {
        for (Node sibling = node.getNextSibling(); sibling != null; sibling = sibling.getNextSibling()) {
            if (sibling.getNodeType() == 1) {
                for (int i = 0; i < elemNames.length; i++) {
                    String uri = sibling.getNamespaceURI();
                    if (uri != null && uri.equals(elemNames[i][0]) && sibling.getLocalName().equals(elemNames[i][1])) {
                        return (Element) sibling;
                    }
                }
            }
        }
        return null;
    }

    public static Element getFirstChildElement(Node parent, String elemName, String attrName, String attrValue) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == 1) {
                Element element = (Element) child;
                if (element.getNodeName().equals(elemName) && element.getAttribute(attrName).equals(attrValue)) {
                    return element;
                }
            }
        }
        return null;
    }

    public static Element getLastChildElement(Node parent, String elemName, String attrName, String attrValue) {
        for (Node child = parent.getLastChild(); child != null; child = child.getPreviousSibling()) {
            if (child.getNodeType() == 1) {
                Element element = (Element) child;
                if (element.getNodeName().equals(elemName) && element.getAttribute(attrName).equals(attrValue)) {
                    return element;
                }
            }
        }
        return null;
    }

    public static Element getNextSiblingElement(Node node, String elemName, String attrName, String attrValue) {
        for (Node sibling = node.getNextSibling(); sibling != null; sibling = sibling.getNextSibling()) {
            if (sibling.getNodeType() == 1) {
                Element element = (Element) sibling;
                if (element.getNodeName().equals(elemName) && element.getAttribute(attrName).equals(attrValue)) {
                    return element;
                }
            }
        }
        return null;
    }

    public static String getChildText(Node node) {
        if (node == null) {
            return null;
        }
        StringBuffer str = new StringBuffer();
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            short type = child.getNodeType();
            if (type == 3) {
                str.append(child.getNodeValue());
            } else if (type == 4) {
                str.append(getChildText(child));
            }
        }
        return str.toString();
    }

    public static String getName(Node node) {
        return node.getNodeName();
    }

    public static String getLocalName(Node node) {
        String name = node.getLocalName();
        return name != null ? name : node.getNodeName();
    }

    public static Element getParent(Element elem) {
        Node parent = elem.getParentNode();
        if (parent instanceof Element) {
            return (Element) parent;
        }
        return null;
    }

    public static Document getDocument(Node node) {
        return node.getOwnerDocument();
    }

    public static Element getRoot(Document doc) {
        return doc.getDocumentElement();
    }

    public static Attr getAttr(Element elem, String name) {
        return elem.getAttributeNode(name);
    }

    public static Attr getAttrNS(Element elem, String nsUri, String localName) {
        return elem.getAttributeNodeNS(nsUri, localName);
    }

    public static Attr[] getAttrs(Element elem) {
        NamedNodeMap attrMap = elem.getAttributes();
        Attr[] attrArray = new Attr[attrMap.getLength()];
        for (int i = 0; i < attrMap.getLength(); i++) {
            attrArray[i] = (Attr) attrMap.item(i);
        }
        return attrArray;
    }

    public static String getValue(Attr attribute) {
        return attribute.getValue();
    }

    public static String getAttrValue(Element elem, String name) {
        return elem.getAttribute(name);
    }

    public static String getAttrValueNS(Element elem, String nsUri, String localName) {
        return elem.getAttributeNS(nsUri, localName);
    }

    public static String getPrefix(Node node) {
        return node.getPrefix();
    }

    public static String getNamespaceURI(Node node) {
        return node.getNamespaceURI();
    }

    public static String getAnnotation(Node node) {
        if (node instanceof ElementImpl) {
            return node.getAnnotation();
        }
        return null;
    }

    public static String getSyntheticAnnotation(Node node) {
        if (node instanceof ElementImpl) {
            return node.getSyntheticAnnotation();
        }
        return null;
    }

    public static DOMException createDOMException(short code, Throwable cause) {
        DOMException de = new DOMException(code, cause != null ? cause.getMessage() : null);
        if (cause != null && ThrowableMethods.fgThrowableMethodsAvailable) {
            try {
                ThrowableMethods.fgThrowableInitCauseMethod.invoke(de, cause);
            } catch (Exception e) {
            }
        }
        return de;
    }

    public static LSException createLSException(short code, Throwable cause) {
        LSException lse = new LSException(code, cause != null ? cause.getMessage() : null);
        if (cause != null && ThrowableMethods.fgThrowableMethodsAvailable) {
            try {
                ThrowableMethods.fgThrowableInitCauseMethod.invoke(lse, cause);
            } catch (Exception e) {
            }
        }
        return lse;
    }

    static class ThrowableMethods {
        private static Method fgThrowableInitCauseMethod;
        private static boolean fgThrowableMethodsAvailable;

        static {
            fgThrowableInitCauseMethod = null;
            fgThrowableMethodsAvailable = false;
            try {
                fgThrowableInitCauseMethod = Throwable.class.getMethod("initCause", Throwable.class);
                fgThrowableMethodsAvailable = true;
            } catch (Exception e) {
                fgThrowableInitCauseMethod = null;
                fgThrowableMethodsAvailable = false;
            }
        }

        private ThrowableMethods() {
        }
    }
}
