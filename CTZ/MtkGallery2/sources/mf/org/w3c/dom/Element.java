package mf.org.w3c.dom;

public interface Element extends Node {
    String getAttribute(String str);

    String getAttributeNS(String str, String str2) throws DOMException;

    Attr getAttributeNode(String str);

    Attr getAttributeNodeNS(String str, String str2) throws DOMException;

    String getTagName();

    Attr removeAttributeNode(Attr attr) throws DOMException;

    void setAttribute(String str, String str2) throws DOMException;

    void setAttributeNS(String str, String str2, String str3) throws DOMException;

    Attr setAttributeNode(Attr attr) throws DOMException;

    Attr setAttributeNodeNS(Attr attr) throws DOMException;
}
