package org.apache.xml.utils;

import java.io.File;
import org.apache.xml.dtm.ref.dom2dtm.DOM2DTM;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.LocatorImpl;

public class TreeWalker {
    private ContentHandler m_contentHandler;
    protected DOMHelper m_dh;
    private LocatorImpl m_locator = new LocatorImpl();
    boolean nextIsRaw = false;

    public ContentHandler getContentHandler() {
        return this.m_contentHandler;
    }

    public void setContentHandler(ContentHandler contentHandler) {
        this.m_contentHandler = contentHandler;
    }

    public TreeWalker(ContentHandler contentHandler, DOMHelper dOMHelper, String str) {
        this.m_contentHandler = null;
        this.m_contentHandler = contentHandler;
        this.m_contentHandler.setDocumentLocator(this.m_locator);
        if (str != null) {
            this.m_locator.setSystemId(str);
        } else {
            try {
                this.m_locator.setSystemId(System.getProperty("user.dir") + File.separator + "dummy.xsl");
            } catch (SecurityException e) {
            }
        }
        this.m_dh = dOMHelper;
    }

    public TreeWalker(ContentHandler contentHandler, DOMHelper dOMHelper) {
        this.m_contentHandler = null;
        this.m_contentHandler = contentHandler;
        this.m_contentHandler.setDocumentLocator(this.m_locator);
        try {
            this.m_locator.setSystemId(System.getProperty("user.dir") + File.separator + "dummy.xsl");
        } catch (SecurityException e) {
        }
        this.m_dh = dOMHelper;
    }

    public TreeWalker(ContentHandler contentHandler) {
        this.m_contentHandler = null;
        this.m_contentHandler = contentHandler;
        if (this.m_contentHandler != null) {
            this.m_contentHandler.setDocumentLocator(this.m_locator);
        }
        try {
            this.m_locator.setSystemId(System.getProperty("user.dir") + File.separator + "dummy.xsl");
        } catch (SecurityException e) {
        }
        this.m_dh = new DOM2Helper();
    }

    public void traverse(Node node) throws SAXException {
        this.m_contentHandler.startDocument();
        traverseFragment(node);
        this.m_contentHandler.endDocument();
    }

    public void traverseFragment(Node node) throws SAXException {
        Node parentNode = node;
        while (parentNode != null) {
            startNode(parentNode);
            Node firstChild = parentNode.getFirstChild();
            while (firstChild == null) {
                endNode(parentNode);
                if (node.equals(parentNode)) {
                    break;
                }
                firstChild = parentNode.getNextSibling();
                if (firstChild == null && ((parentNode = parentNode.getParentNode()) == null || node.equals(parentNode))) {
                    if (parentNode != null) {
                        endNode(parentNode);
                    }
                    parentNode = null;
                }
            }
            parentNode = firstChild;
        }
    }

    public void traverse(Node node, Node node2) throws SAXException {
        this.m_contentHandler.startDocument();
        while (node != null) {
            startNode(node);
            Node firstChild = node.getFirstChild();
            while (firstChild == null) {
                endNode(node);
                if (node2 != null && node2.equals(node)) {
                    break;
                }
                firstChild = node.getNextSibling();
                if (firstChild == null && ((node = node.getParentNode()) == null || (node2 != null && node2.equals(node)))) {
                    node = null;
                    break;
                }
            }
            node = firstChild;
        }
        this.m_contentHandler.endDocument();
    }

    private final void dispatachChars(Node node) throws SAXException {
        if (this.m_contentHandler instanceof DOM2DTM.CharacterNodeHandler) {
            ((DOM2DTM.CharacterNodeHandler) this.m_contentHandler).characters(node);
        } else {
            String data = ((Text) node).getData();
            this.m_contentHandler.characters(data.toCharArray(), 0, data.length());
        }
    }

    protected void startNode(Node node) throws SAXException {
        if (this.m_contentHandler instanceof NodeConsumer) {
            ((NodeConsumer) this.m_contentHandler).setOriginatingNode(node);
        }
        if (node instanceof Locator) {
            Locator locator = (Locator) node;
            this.m_locator.setColumnNumber(locator.getColumnNumber());
            this.m_locator.setLineNumber(locator.getLineNumber());
            this.m_locator.setPublicId(locator.getPublicId());
            this.m_locator.setSystemId(locator.getSystemId());
        } else {
            this.m_locator.setColumnNumber(0);
            this.m_locator.setLineNumber(0);
        }
        switch (node.getNodeType()) {
            case 1:
                NamedNodeMap attributes = ((Element) node).getAttributes();
                int length = attributes.getLength();
                for (int i = 0; i < length; i++) {
                    Node nodeItem = attributes.item(i);
                    String nodeName = nodeItem.getNodeName();
                    if (nodeName.equals("xmlns") || nodeName.startsWith(org.apache.xalan.templates.Constants.ATTRNAME_XMLNS)) {
                        int iIndexOf = nodeName.indexOf(":");
                        this.m_contentHandler.startPrefixMapping(iIndexOf < 0 ? "" : nodeName.substring(iIndexOf + 1), nodeItem.getNodeValue());
                    }
                }
                String namespaceOfNode = this.m_dh.getNamespaceOfNode(node);
                if (namespaceOfNode == null) {
                    namespaceOfNode = "";
                }
                this.m_contentHandler.startElement(namespaceOfNode, this.m_dh.getLocalNameOfNode(node), node.getNodeName(), new AttList(attributes, this.m_dh));
                break;
            case 3:
                if (this.nextIsRaw) {
                    this.nextIsRaw = false;
                    this.m_contentHandler.processingInstruction("javax.xml.transform.disable-output-escaping", "");
                    dispatachChars(node);
                    this.m_contentHandler.processingInstruction("javax.xml.transform.enable-output-escaping", "");
                } else {
                    dispatachChars(node);
                }
                break;
            case 4:
                boolean z = this.m_contentHandler instanceof LexicalHandler;
                LexicalHandler lexicalHandler = z ? (LexicalHandler) this.m_contentHandler : null;
                if (z) {
                    lexicalHandler.startCDATA();
                }
                dispatachChars(node);
                if (z) {
                    lexicalHandler.endCDATA();
                }
                break;
            case 5:
                EntityReference entityReference = (EntityReference) node;
                if (this.m_contentHandler instanceof LexicalHandler) {
                    ((LexicalHandler) this.m_contentHandler).startEntity(entityReference.getNodeName());
                }
                break;
            case 7:
                ProcessingInstruction processingInstruction = (ProcessingInstruction) node;
                if (processingInstruction.getNodeName().equals("xslt-next-is-raw")) {
                    this.nextIsRaw = true;
                } else {
                    this.m_contentHandler.processingInstruction(processingInstruction.getNodeName(), processingInstruction.getData());
                }
                break;
            case 8:
                String data = ((Comment) node).getData();
                if (this.m_contentHandler instanceof LexicalHandler) {
                    ((LexicalHandler) this.m_contentHandler).comment(data.toCharArray(), 0, data.length());
                }
                break;
        }
    }

    protected void endNode(Node node) throws SAXException {
        short nodeType = node.getNodeType();
        if (nodeType != 1) {
            if (nodeType != 9) {
                switch (nodeType) {
                    case 5:
                        EntityReference entityReference = (EntityReference) node;
                        if (this.m_contentHandler instanceof LexicalHandler) {
                            ((LexicalHandler) this.m_contentHandler).endEntity(entityReference.getNodeName());
                        }
                        break;
                }
                return;
            }
            return;
        }
        String namespaceOfNode = this.m_dh.getNamespaceOfNode(node);
        if (namespaceOfNode == null) {
            namespaceOfNode = "";
        }
        this.m_contentHandler.endElement(namespaceOfNode, this.m_dh.getLocalNameOfNode(node), node.getNodeName());
        NamedNodeMap attributes = ((Element) node).getAttributes();
        int length = attributes.getLength();
        for (int i = 0; i < length; i++) {
            String nodeName = attributes.item(i).getNodeName();
            if (nodeName.equals("xmlns") || nodeName.startsWith(org.apache.xalan.templates.Constants.ATTRNAME_XMLNS)) {
                int iIndexOf = nodeName.indexOf(":");
                this.m_contentHandler.endPrefixMapping(iIndexOf < 0 ? "" : nodeName.substring(iIndexOf + 1));
            }
        }
    }
}
