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

    public DOMBuilder(Document doc, Node node) {
        this.m_currentNode = null;
        this.m_root = null;
        this.m_nextSibling = null;
        this.m_docFrag = null;
        this.m_elemStack = new Stack();
        this.m_prefixMappings = new Vector();
        this.m_inCData = false;
        this.m_doc = doc;
        this.m_root = node;
        this.m_currentNode = node;
        if (node instanceof Element) {
            this.m_elemStack.push(node);
        }
    }

    public DOMBuilder(Document doc, DocumentFragment docFrag) {
        this.m_currentNode = null;
        this.m_root = null;
        this.m_nextSibling = null;
        this.m_docFrag = null;
        this.m_elemStack = new Stack();
        this.m_prefixMappings = new Vector();
        this.m_inCData = false;
        this.m_doc = doc;
        this.m_docFrag = docFrag;
    }

    public DOMBuilder(Document doc) {
        this.m_currentNode = null;
        this.m_root = null;
        this.m_nextSibling = null;
        this.m_docFrag = null;
        this.m_elemStack = new Stack();
        this.m_prefixMappings = new Vector();
        this.m_inCData = false;
        this.m_doc = doc;
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

    public void setNextSibling(Node nextSibling) {
        this.m_nextSibling = nextSibling;
    }

    public Node getNextSibling() {
        return this.m_nextSibling;
    }

    public Writer getWriter() {
        return null;
    }

    protected void append(Node newNode) throws SAXException {
        Node currentNode = this.m_currentNode;
        if (currentNode != null) {
            if (currentNode == this.m_root && this.m_nextSibling != null) {
                currentNode.insertBefore(newNode, this.m_nextSibling);
                return;
            } else {
                currentNode.appendChild(newNode);
                return;
            }
        }
        if (this.m_docFrag != null) {
            if (this.m_nextSibling != null) {
                this.m_docFrag.insertBefore(newNode, this.m_nextSibling);
                return;
            } else {
                this.m_docFrag.appendChild(newNode);
                return;
            }
        }
        boolean ok = true;
        short type = newNode.getNodeType();
        if (type == 3) {
            String data = newNode.getNodeValue();
            if (data != null && data.trim().length() > 0) {
                throw new SAXException(XMLMessages.createXMLMessage(XMLErrorResources.ER_CANT_OUTPUT_TEXT_BEFORE_DOC, null));
            }
            ok = false;
        } else if (type == 1 && this.m_doc.getDocumentElement() != null) {
            throw new SAXException(XMLMessages.createXMLMessage(XMLErrorResources.ER_CANT_HAVE_MORE_THAN_ONE_ROOT, null));
        }
        if (ok) {
            if (this.m_nextSibling != null) {
                this.m_doc.insertBefore(newNode, this.m_nextSibling);
            } else {
                this.m_doc.appendChild(newNode);
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
    public void startElement(String ns, String localName, String name, Attributes atts) throws SAXException {
        Element elem;
        if (ns == null || ns.length() == 0) {
            elem = this.m_doc.createElementNS(null, name);
        } else {
            elem = this.m_doc.createElementNS(ns, name);
        }
        append(elem);
        try {
            int nAtts = atts.getLength();
            if (nAtts != 0) {
                for (int i = 0; i < nAtts; i++) {
                    if (atts.getType(i).equalsIgnoreCase("ID")) {
                        setIDAttribute(atts.getValue(i), elem);
                    }
                    String attrNS = atts.getURI(i);
                    if ("".equals(attrNS)) {
                        attrNS = null;
                    }
                    String attrQName = atts.getQName(i);
                    if (attrQName.startsWith(org.apache.xalan.templates.Constants.ATTRNAME_XMLNS) || attrQName.equals("xmlns")) {
                        attrNS = SerializerConstants.XMLNS_URI;
                    }
                    elem.setAttributeNS(attrNS, attrQName, atts.getValue(i));
                }
            }
            int nDecls = this.m_prefixMappings.size();
            for (int i2 = 0; i2 < nDecls; i2 += 2) {
                String prefix = (String) this.m_prefixMappings.elementAt(i2);
                if (prefix != null) {
                    String declURL = (String) this.m_prefixMappings.elementAt(i2 + 1);
                    elem.setAttributeNS(SerializerConstants.XMLNS_URI, prefix, declURL);
                }
            }
            this.m_prefixMappings.clear();
            this.m_elemStack.push(elem);
            this.m_currentNode = elem;
        } catch (Exception de) {
            throw new SAXException(de);
        }
    }

    @Override
    public void endElement(String ns, String localName, String name) throws SAXException {
        this.m_elemStack.pop();
        this.m_currentNode = this.m_elemStack.isEmpty() ? null : (Node) this.m_elemStack.peek();
    }

    public void setIDAttribute(String id, Element elem) {
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (!isOutsideDocElem() || !XMLCharacterRecognizer.isWhiteSpace(ch, start, length)) {
            if (this.m_inCData) {
                cdata(ch, start, length);
                return;
            }
            String s = new String(ch, start, length);
            Node childNode = this.m_currentNode != null ? this.m_currentNode.getLastChild() : null;
            if (childNode != null && childNode.getNodeType() == 3) {
                ((Text) childNode).appendData(s);
            } else {
                Text text = this.m_doc.createTextNode(s);
                append(text);
            }
        }
    }

    public void charactersRaw(char[] ch, int start, int length) throws SAXException {
        if (!isOutsideDocElem() || !XMLCharacterRecognizer.isWhiteSpace(ch, start, length)) {
            String s = new String(ch, start, length);
            append(this.m_doc.createProcessingInstruction("xslt-next-is-raw", "formatter-to-dom"));
            append(this.m_doc.createTextNode(s));
        }
    }

    @Override
    public void startEntity(String name) throws SAXException {
    }

    @Override
    public void endEntity(String name) throws SAXException {
    }

    public void entityReference(String name) throws SAXException {
        append(this.m_doc.createEntityReference(name));
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if (!isOutsideDocElem()) {
            String s = new String(ch, start, length);
            append(this.m_doc.createTextNode(s));
        }
    }

    private boolean isOutsideDocElem() {
        return this.m_docFrag == null && this.m_elemStack.size() == 0 && (this.m_currentNode == null || this.m_currentNode.getNodeType() == 9);
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        append(this.m_doc.createProcessingInstruction(target, data));
    }

    @Override
    public void comment(char[] ch, int start, int length) throws SAXException {
        append(this.m_doc.createComment(new String(ch, start, length)));
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

    public void cdata(char[] ch, int start, int length) throws SAXException {
        if (!isOutsideDocElem() || !XMLCharacterRecognizer.isWhiteSpace(ch, start, length)) {
            String s = new String(ch, start, length);
            CDATASection section = (CDATASection) this.m_currentNode.getLastChild();
            section.appendData(s);
        }
    }

    @Override
    public void startDTD(String name, String publicId, String systemId) throws SAXException {
    }

    @Override
    public void endDTD() throws SAXException {
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        String prefix2;
        if (prefix == null || prefix.equals("")) {
            prefix2 = "xmlns";
        } else {
            prefix2 = org.apache.xalan.templates.Constants.ATTRNAME_XMLNS + prefix;
        }
        this.m_prefixMappings.addElement(prefix2);
        this.m_prefixMappings.addElement(uri);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
    }
}
