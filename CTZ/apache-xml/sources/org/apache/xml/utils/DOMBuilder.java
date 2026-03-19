package org.apache.xml.utils;

import java.io.Writer;
import java.util.Stack;
import java.util.Vector;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;
import org.apache.xml.serializer.SerializerConstants;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

public class DOMBuilder implements ContentHandler, LexicalHandler {
    protected Node m_currentNode;
    public Document m_doc;
    public DocumentFragment m_docFrag;
    protected Stack m_elemStack;
    protected boolean m_inCData;
    protected Node m_nextSibling;
    protected Vector m_prefixMappings;
    protected Node m_root;

    public DOMBuilder(Document document, Node node) {
        this.m_currentNode = null;
        this.m_root = null;
        this.m_nextSibling = null;
        this.m_docFrag = null;
        this.m_elemStack = new Stack();
        this.m_prefixMappings = new Vector();
        this.m_inCData = false;
        this.m_doc = document;
        this.m_root = node;
        this.m_currentNode = node;
        if (node instanceof Element) {
            this.m_elemStack.push(node);
        }
    }

    public DOMBuilder(Document document, DocumentFragment documentFragment) {
        this.m_currentNode = null;
        this.m_root = null;
        this.m_nextSibling = null;
        this.m_docFrag = null;
        this.m_elemStack = new Stack();
        this.m_prefixMappings = new Vector();
        this.m_inCData = false;
        this.m_doc = document;
        this.m_docFrag = documentFragment;
    }

    public DOMBuilder(Document document) {
        this.m_currentNode = null;
        this.m_root = null;
        this.m_nextSibling = null;
        this.m_docFrag = null;
        this.m_elemStack = new Stack();
        this.m_prefixMappings = new Vector();
        this.m_inCData = false;
        this.m_doc = document;
    }

    public Node getRootDocument() {
        return this.m_docFrag != null ? this.m_docFrag : this.m_doc;
    }

    public Node getRootNode() {
        return this.m_root;
    }

    public Node getCurrentNode() {
        return this.m_currentNode;
    }

    public void setNextSibling(Node node) {
        this.m_nextSibling = node;
    }

    public Node getNextSibling() {
        return this.m_nextSibling;
    }

    public Writer getWriter() {
        return null;
    }

    protected void append(Node node) throws SAXException {
        Node node2 = this.m_currentNode;
        if (node2 != null) {
            if (node2 == this.m_root && this.m_nextSibling != null) {
                node2.insertBefore(node, this.m_nextSibling);
                return;
            } else {
                node2.appendChild(node);
                return;
            }
        }
        if (this.m_docFrag != null) {
            if (this.m_nextSibling != null) {
                this.m_docFrag.insertBefore(node, this.m_nextSibling);
                return;
            } else {
                this.m_docFrag.appendChild(node);
                return;
            }
        }
        short nodeType = node.getNodeType();
        boolean z = true;
        if (nodeType == 3) {
            String nodeValue = node.getNodeValue();
            if (nodeValue != null && nodeValue.trim().length() > 0) {
                throw new SAXException(XMLMessages.createXMLMessage(XMLErrorResources.ER_CANT_OUTPUT_TEXT_BEFORE_DOC, null));
            }
            z = false;
        } else if (nodeType == 1 && this.m_doc.getDocumentElement() != null) {
            throw new SAXException(XMLMessages.createXMLMessage(XMLErrorResources.ER_CANT_HAVE_MORE_THAN_ONE_ROOT, null));
        }
        if (z) {
            if (this.m_nextSibling != null) {
                this.m_doc.insertBefore(node, this.m_nextSibling);
            } else {
                this.m_doc.appendChild(node);
            }
        }
    }

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void startDocument() throws SAXException {
    }

    @Override
    public void endDocument() throws SAXException {
    }

    @Override
    public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
        Element elementCreateElementNS;
        if (str == null || str.length() == 0) {
            elementCreateElementNS = this.m_doc.createElementNS(null, str3);
        } else {
            elementCreateElementNS = this.m_doc.createElementNS(str, str3);
        }
        append(elementCreateElementNS);
        try {
            int length = attributes.getLength();
            if (length != 0) {
                for (int i = 0; i < length; i++) {
                    if (attributes.getType(i).equalsIgnoreCase("ID")) {
                        setIDAttribute(attributes.getValue(i), elementCreateElementNS);
                    }
                    String uri = attributes.getURI(i);
                    if ("".equals(uri)) {
                        uri = null;
                    }
                    String qName = attributes.getQName(i);
                    if (qName.startsWith(org.apache.xalan.templates.Constants.ATTRNAME_XMLNS) || qName.equals("xmlns")) {
                        uri = SerializerConstants.XMLNS_URI;
                    }
                    elementCreateElementNS.setAttributeNS(uri, qName, attributes.getValue(i));
                }
            }
            int size = this.m_prefixMappings.size();
            for (int i2 = 0; i2 < size; i2 += 2) {
                String str4 = (String) this.m_prefixMappings.elementAt(i2);
                if (str4 != null) {
                    elementCreateElementNS.setAttributeNS(SerializerConstants.XMLNS_URI, str4, (String) this.m_prefixMappings.elementAt(i2 + 1));
                }
            }
            this.m_prefixMappings.clear();
            this.m_elemStack.push(elementCreateElementNS);
            this.m_currentNode = elementCreateElementNS;
        } catch (Exception e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void endElement(String str, String str2, String str3) throws SAXException {
        this.m_elemStack.pop();
        this.m_currentNode = this.m_elemStack.isEmpty() ? null : (Node) this.m_elemStack.peek();
    }

    public void setIDAttribute(String str, Element element) {
    }

    @Override
    public void characters(char[] cArr, int i, int i2) throws SAXException {
        if (isOutsideDocElem() && XMLCharacterRecognizer.isWhiteSpace(cArr, i, i2)) {
            return;
        }
        if (this.m_inCData) {
            cdata(cArr, i, i2);
            return;
        }
        String str = new String(cArr, i, i2);
        Node lastChild = this.m_currentNode != null ? this.m_currentNode.getLastChild() : null;
        if (lastChild != null && lastChild.getNodeType() == 3) {
            ((Text) lastChild).appendData(str);
        } else {
            append(this.m_doc.createTextNode(str));
        }
    }

    public void charactersRaw(char[] cArr, int i, int i2) throws SAXException {
        if (isOutsideDocElem() && XMLCharacterRecognizer.isWhiteSpace(cArr, i, i2)) {
            return;
        }
        String str = new String(cArr, i, i2);
        append(this.m_doc.createProcessingInstruction("xslt-next-is-raw", "formatter-to-dom"));
        append(this.m_doc.createTextNode(str));
    }

    @Override
    public void startEntity(String str) throws SAXException {
    }

    @Override
    public void endEntity(String str) throws SAXException {
    }

    public void entityReference(String str) throws SAXException {
        append(this.m_doc.createEntityReference(str));
    }

    @Override
    public void ignorableWhitespace(char[] cArr, int i, int i2) throws SAXException {
        if (isOutsideDocElem()) {
            return;
        }
        append(this.m_doc.createTextNode(new String(cArr, i, i2)));
    }

    private boolean isOutsideDocElem() {
        return this.m_docFrag == null && this.m_elemStack.size() == 0 && (this.m_currentNode == null || this.m_currentNode.getNodeType() == 9);
    }

    @Override
    public void processingInstruction(String str, String str2) throws SAXException {
        append(this.m_doc.createProcessingInstruction(str, str2));
    }

    @Override
    public void comment(char[] cArr, int i, int i2) throws SAXException {
        append(this.m_doc.createComment(new String(cArr, i, i2)));
    }

    @Override
    public void startCDATA() throws SAXException {
        this.m_inCData = true;
        append(this.m_doc.createCDATASection(""));
    }

    @Override
    public void endCDATA() throws SAXException {
        this.m_inCData = false;
    }

    public void cdata(char[] cArr, int i, int i2) throws SAXException {
        if (isOutsideDocElem() && XMLCharacterRecognizer.isWhiteSpace(cArr, i, i2)) {
            return;
        }
        ((CDATASection) this.m_currentNode.getLastChild()).appendData(new String(cArr, i, i2));
    }

    @Override
    public void startDTD(String str, String str2, String str3) throws SAXException {
    }

    @Override
    public void endDTD() throws SAXException {
    }

    @Override
    public void startPrefixMapping(String str, String str2) throws SAXException {
        String str3;
        if (str == null || str.equals("")) {
            str3 = "xmlns";
        } else {
            str3 = org.apache.xalan.templates.Constants.ATTRNAME_XMLNS + str;
        }
        this.m_prefixMappings.addElement(str3);
        this.m_prefixMappings.addElement(str2);
    }

    @Override
    public void endPrefixMapping(String str) throws SAXException {
    }

    @Override
    public void skippedEntity(String str) throws SAXException {
    }
}
