package mf.org.w3c.dom;

public interface Document extends Node {
    Node adoptNode(Node node) throws DOMException;

    Attr createAttribute(String str) throws DOMException;

    Attr createAttributeNS(String str, String str2) throws DOMException;

    CDATASection createCDATASection(String str) throws DOMException;

    Comment createComment(String str);

    Element createElement(String str) throws DOMException;

    Element createElementNS(String str, String str2) throws DOMException;

    EntityReference createEntityReference(String str) throws DOMException;

    ProcessingInstruction createProcessingInstruction(String str, String str2) throws DOMException;

    Text createTextNode(String str);

    DocumentType getDoctype();

    Element getDocumentElement();

    String getDocumentURI();

    DOMImplementation getImplementation();

    String getInputEncoding();

    String getXmlEncoding();

    String getXmlVersion();

    Node importNode(Node node, boolean z) throws DOMException;
}
