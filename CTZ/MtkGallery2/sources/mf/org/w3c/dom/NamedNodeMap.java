package mf.org.w3c.dom;

public interface NamedNodeMap {
    int getLength();

    Node getNamedItem(String str);

    Node getNamedItemNS(String str, String str2) throws DOMException;

    Node item(int i);

    Node setNamedItem(Node node) throws DOMException;

    Node setNamedItemNS(Node node) throws DOMException;
}
