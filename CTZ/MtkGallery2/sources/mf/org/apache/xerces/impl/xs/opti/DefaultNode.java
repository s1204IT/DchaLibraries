package mf.org.apache.xerces.impl.xs.opti;

import mf.org.w3c.dom.DOMException;
import mf.org.w3c.dom.Document;
import mf.org.w3c.dom.NamedNodeMap;
import mf.org.w3c.dom.Node;
import mf.org.w3c.dom.NodeList;
import mf.org.w3c.dom.UserDataHandler;

public class DefaultNode implements Node {
    @Override
    public String getNodeName() {
        return null;
    }

    @Override
    public String getNodeValue() throws DOMException {
        return null;
    }

    @Override
    public short getNodeType() {
        return (short) -1;
    }

    @Override
    public Node getParentNode() {
        return null;
    }

    @Override
    public NodeList getChildNodes() {
        return null;
    }

    @Override
    public Node getFirstChild() {
        return null;
    }

    @Override
    public Node getLastChild() {
        return null;
    }

    @Override
    public Node getPreviousSibling() {
        return null;
    }

    @Override
    public Node getNextSibling() {
        return null;
    }

    @Override
    public NamedNodeMap getAttributes() {
        return null;
    }

    @Override
    public Document getOwnerDocument() {
        return null;
    }

    @Override
    public boolean hasChildNodes() {
        return false;
    }

    @Override
    public Node cloneNode(boolean deep) {
        return null;
    }

    @Override
    public void normalize() {
    }

    public boolean isSupported(String feature, String version) {
        return false;
    }

    @Override
    public String getNamespaceURI() {
        return null;
    }

    @Override
    public String getPrefix() {
        return null;
    }

    @Override
    public String getLocalName() {
        return null;
    }

    @Override
    public String getBaseURI() {
        return null;
    }

    @Override
    public boolean hasAttributes() {
        return false;
    }

    @Override
    public void setNodeValue(String nodeValue) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public Node insertBefore(Node newChild, Node refChild) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public Node removeChild(Node oldChild) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public Node appendChild(Node newChild) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public void setPrefix(String prefix) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public short compareDocumentPosition(Node other) {
        throw new DOMException((short) 9, "Method not supported");
    }

    public String getTextContent() throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    public void setTextContent(String textContent) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public boolean isSameNode(Node other) {
        throw new DOMException((short) 9, "Method not supported");
    }

    public String lookupPrefix(String namespaceURI) {
        throw new DOMException((short) 9, "Method not supported");
    }

    public boolean isDefaultNamespace(String namespaceURI) {
        throw new DOMException((short) 9, "Method not supported");
    }

    public String lookupNamespaceURI(String prefix) {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public boolean isEqualNode(Node arg) {
        throw new DOMException((short) 9, "Method not supported");
    }

    public Object getFeature(String feature, String version) {
        return null;
    }

    public Object setUserData(String key, Object data, UserDataHandler handler) {
        throw new DOMException((short) 9, "Method not supported");
    }

    public Object getUserData(String key) {
        return null;
    }
}
