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

    public void setDocument(Document doc) {
        this.m_doc = doc;
    }

    public Document getDocument() {
        return this.m_doc;
    }

    public void parse(InputSource source) throws TransformerException {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            builderFactory.setValidating(true);
            DocumentBuilder parser = builderFactory.newDocumentBuilder();
            parser.setErrorHandler(new DefaultErrorHandler());
            setDocument(parser.parse(source));
        } catch (IOException ioe) {
            throw new TransformerException(ioe);
        } catch (ParserConfigurationException pce) {
            throw new TransformerException(pce);
        } catch (SAXException se) {
            throw new TransformerException(se);
        }
    }

    @Override
    public Element getElementByID(String id, Document doc) {
        return doc.getElementById(id);
    }

    public static boolean isNodeAfter(Node node1, Node node2) {
        if ((node1 instanceof DOMOrder) && (node2 instanceof DOMOrder)) {
            int index1 = ((DOMOrder) node1).getUid();
            int index2 = ((DOMOrder) node2).getUid();
            return index1 <= index2;
        }
        return DOMHelper.isNodeAfter(node1, node2);
    }

    public static Node getParentOfNode(Node node) {
        Node parent = node.getParentNode();
        if (parent == null && 2 == node.getNodeType()) {
            return ((Attr) node).getOwnerElement();
        }
        return parent;
    }

    @Override
    public String getLocalNameOfNode(Node n) {
        String name = n.getLocalName();
        return name == null ? super.getLocalNameOfNode(n) : name;
    }

    @Override
    public String getNamespaceOfNode(Node n) {
        return n.getNamespaceURI();
    }
}
