package org.apache.xml.dtm.ref;

import org.apache.xml.dtm.DTM;
import org.apache.xml.utils.NodeConsumer;
import org.apache.xml.utils.XMLString;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

public class DTMTreeWalker {
    private ContentHandler m_contentHandler;
    protected DTM m_dtm;
    boolean nextIsRaw;

    public void setDTM(DTM dtm) {
        this.m_dtm = dtm;
    }

    public ContentHandler getcontentHandler() {
        return this.m_contentHandler;
    }

    public void setcontentHandler(ContentHandler contentHandler) {
        this.m_contentHandler = contentHandler;
    }

    public DTMTreeWalker() {
        this.m_contentHandler = null;
        this.nextIsRaw = false;
    }

    public DTMTreeWalker(ContentHandler contentHandler, DTM dtm) {
        this.m_contentHandler = null;
        this.nextIsRaw = false;
        this.m_contentHandler = contentHandler;
        this.m_dtm = dtm;
    }

    public void traverse(int i) throws SAXException {
        int parent = i;
        while (-1 != parent) {
            startNode(parent);
            int firstChild = this.m_dtm.getFirstChild(parent);
            while (-1 == firstChild) {
                endNode(parent);
                if (i == parent) {
                    break;
                }
                firstChild = this.m_dtm.getNextSibling(parent);
                if (-1 == firstChild && (-1 == (parent = this.m_dtm.getParent(parent)) || i == parent)) {
                    if (-1 != parent) {
                        endNode(parent);
                    }
                    parent = -1;
                }
            }
            parent = firstChild;
        }
    }

    public void traverse(int i, int i2) throws SAXException {
        while (-1 != i) {
            startNode(i);
            int firstChild = this.m_dtm.getFirstChild(i);
            while (-1 == firstChild) {
                endNode(i);
                if (-1 != i2 && i2 == i) {
                    break;
                }
                firstChild = this.m_dtm.getNextSibling(i);
                if (-1 == firstChild && (-1 == (i = this.m_dtm.getParent(i)) || (-1 != i2 && i2 == i))) {
                    i = -1;
                    break;
                }
            }
            i = firstChild;
        }
    }

    private final void dispatachChars(int i) throws SAXException {
        this.m_dtm.dispatchCharactersEvents(i, this.m_contentHandler, false);
    }

    protected void startNode(int i) throws SAXException {
        boolean z = this.m_contentHandler instanceof NodeConsumer;
        switch (this.m_dtm.getNodeType(i)) {
            case 1:
                DTM dtm = this.m_dtm;
                int firstNamespaceNode = dtm.getFirstNamespaceNode(i, true);
                while (-1 != firstNamespaceNode) {
                    this.m_contentHandler.startPrefixMapping(dtm.getNodeNameX(firstNamespaceNode), dtm.getNodeValue(firstNamespaceNode));
                    firstNamespaceNode = dtm.getNextNamespaceNode(i, firstNamespaceNode, true);
                }
                String namespaceURI = dtm.getNamespaceURI(i);
                if (namespaceURI == null) {
                    namespaceURI = "";
                }
                AttributesImpl attributesImpl = new AttributesImpl();
                for (int firstAttribute = dtm.getFirstAttribute(i); firstAttribute != -1; firstAttribute = dtm.getNextAttribute(firstAttribute)) {
                    attributesImpl.addAttribute(dtm.getNamespaceURI(firstAttribute), dtm.getLocalName(firstAttribute), dtm.getNodeName(firstAttribute), "CDATA", dtm.getNodeValue(firstAttribute));
                }
                this.m_contentHandler.startElement(namespaceURI, this.m_dtm.getLocalName(i), this.m_dtm.getNodeName(i), attributesImpl);
                break;
            case 3:
                if (this.nextIsRaw) {
                    this.nextIsRaw = false;
                    this.m_contentHandler.processingInstruction("javax.xml.transform.disable-output-escaping", "");
                    dispatachChars(i);
                    this.m_contentHandler.processingInstruction("javax.xml.transform.enable-output-escaping", "");
                } else {
                    dispatachChars(i);
                }
                break;
            case 4:
                boolean z2 = this.m_contentHandler instanceof LexicalHandler;
                LexicalHandler lexicalHandler = z2 ? (LexicalHandler) this.m_contentHandler : null;
                if (z2) {
                    lexicalHandler.startCDATA();
                }
                dispatachChars(i);
                if (z2) {
                    lexicalHandler.endCDATA();
                }
                break;
            case 5:
                if (this.m_contentHandler instanceof LexicalHandler) {
                    ((LexicalHandler) this.m_contentHandler).startEntity(this.m_dtm.getNodeName(i));
                }
                break;
            case 7:
                String nodeName = this.m_dtm.getNodeName(i);
                if (nodeName.equals("xslt-next-is-raw")) {
                    this.nextIsRaw = true;
                } else {
                    this.m_contentHandler.processingInstruction(nodeName, this.m_dtm.getNodeValue(i));
                }
                break;
            case 8:
                XMLString stringValue = this.m_dtm.getStringValue(i);
                if (this.m_contentHandler instanceof LexicalHandler) {
                    stringValue.dispatchAsComment((LexicalHandler) this.m_contentHandler);
                }
                break;
            case 9:
                this.m_contentHandler.startDocument();
                break;
        }
    }

    protected void endNode(int i) throws SAXException {
        short nodeType = this.m_dtm.getNodeType(i);
        if (nodeType != 1) {
            if (nodeType == 9) {
                this.m_contentHandler.endDocument();
                return;
            }
            switch (nodeType) {
                case 5:
                    if (this.m_contentHandler instanceof LexicalHandler) {
                        ((LexicalHandler) this.m_contentHandler).endEntity(this.m_dtm.getNodeName(i));
                    }
                    break;
            }
            return;
        }
        String namespaceURI = this.m_dtm.getNamespaceURI(i);
        if (namespaceURI == null) {
            namespaceURI = "";
        }
        this.m_contentHandler.endElement(namespaceURI, this.m_dtm.getLocalName(i), this.m_dtm.getNodeName(i));
        int firstNamespaceNode = this.m_dtm.getFirstNamespaceNode(i, true);
        while (-1 != firstNamespaceNode) {
            this.m_contentHandler.endPrefixMapping(this.m_dtm.getNodeNameX(firstNamespaceNode));
            firstNamespaceNode = this.m_dtm.getNextNamespaceNode(i, firstNamespaceNode, true);
        }
    }
}
