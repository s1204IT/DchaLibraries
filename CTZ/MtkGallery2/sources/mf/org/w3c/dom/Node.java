package mf.org.w3c.dom;

public interface Node {
    Node appendChild(Node node) throws DOMException;

    Node cloneNode(boolean z);

    short compareDocumentPosition(Node node) throws DOMException;

    NamedNodeMap getAttributes();

    String getBaseURI();

    NodeList getChildNodes();

    Node getFirstChild();

    Node getLastChild();

    String getLocalName();

    String getNamespaceURI();

    Node getNextSibling();

    String getNodeName();

    short getNodeType();

    String getNodeValue() throws DOMException;

    Document getOwnerDocument();

    Node getParentNode();

    String getPrefix();

    Node getPreviousSibling();

    boolean hasAttributes();

    boolean hasChildNodes();

    Node insertBefore(Node node, Node node2) throws DOMException;

    boolean isEqualNode(Node node);

    boolean isSameNode(Node node);

    void normalize();

    Node removeChild(Node node) throws DOMException;

    Node replaceChild(Node node, Node node2) throws DOMException;

    void setNodeValue(String str) throws DOMException;

    void setPrefix(String str) throws DOMException;
}
