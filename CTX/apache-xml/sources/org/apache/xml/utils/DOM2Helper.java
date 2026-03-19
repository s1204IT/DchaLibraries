package org.apache.xml.utils;

import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DOM2Helper extends DOMHelper {
    private Document m_doc;

    public void checkNode(Node node) throws TransformerException {
    }

    public boolean supportsSAX() {
        return true;
    }

    public void setDocument(Document document) {
        this.m_doc = document;
    }

    public Document getDocument() {
        return this.m_doc;
    }

    public void parse(InputSource inputSource) throws TransformerException {
        try {
            DocumentBuilderFactory documentBuilderFactoryNewInstance = DocumentBuilderFactory.newInstance();
            documentBuilderFactoryNewInstance.setNamespaceAware(true);
            documentBuilderFactoryNewInstance.setValidating(true);
            DocumentBuilder documentBuilderNewDocumentBuilder = documentBuilderFactoryNewInstance.newDocumentBuilder();
            documentBuilderNewDocumentBuilder.setErrorHandler(new DefaultErrorHandler());
            setDocument(documentBuilderNewDocumentBuilder.parse(inputSource));
        } catch (IOException e) {
            throw new TransformerException(e);
        } catch (ParserConfigurationException e2) {
            throw new TransformerException(e2);
        } catch (SAXException e3) {
            throw new TransformerException(e3);
        }
    }

    @Override
    public Element getElementByID(String str, Document document) {
        return document.getElementById(str);
    }

    public static boolean isNodeAfter(Node node, Node node2) {
        if ((node instanceof DOMOrder) && (node2 instanceof DOMOrder)) {
            return ((DOMOrder) node).getUid() <= ((DOMOrder) node2).getUid();
        }
        return DOMHelper.isNodeAfter(node, node2);
    }

    public static Node getParentOfNode(Node node) {
        Node parentNode = node.getParentNode();
        if (parentNode == null && 2 == node.getNodeType()) {
            return ((Attr) node).getOwnerElement();
        }
        return parentNode;
    }

    @Override
    public String getLocalNameOfNode(Node node) {
        String localName = node.getLocalName();
        return localName == null ? super.getLocalNameOfNode(node) : localName;
    }

    @Override
    public String getNamespaceOfNode(Node node) {
        return node.getNamespaceURI();
    }
}
