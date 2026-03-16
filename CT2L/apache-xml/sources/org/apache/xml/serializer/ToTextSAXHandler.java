package org.apache.xml.serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

public final class ToTextSAXHandler extends ToSAXHandler {
    @Override
    public void endElement(String elemName) throws SAXException {
        if (this.m_tracer != null) {
            super.fireEndElem(elemName);
        }
    }

    @Override
    public void endElement(String arg0, String arg1, String arg2) throws SAXException {
        if (this.m_tracer != null) {
            super.fireEndElem(arg2);
        }
    }

    public ToTextSAXHandler(ContentHandler hdlr, LexicalHandler lex, String encoding) {
        super(hdlr, lex, encoding);
    }

    public ToTextSAXHandler(ContentHandler handler, String encoding) {
        super(handler, encoding);
    }

    @Override
    public void comment(char[] ch, int start, int length) throws SAXException {
        if (this.m_tracer != null) {
            super.fireCommentEvent(ch, start, length);
        }
    }

    @Override
    public void comment(String data) throws SAXException {
        int length = data.length();
        if (length > this.m_charsBuff.length) {
            this.m_charsBuff = new char[(length * 2) + 1];
        }
        data.getChars(0, length, this.m_charsBuff, 0);
        comment(this.m_charsBuff, 0, length);
    }

    @Override
    public Properties getOutputFormat() {
        return null;
    }

    @Override
    public OutputStream getOutputStream() {
        return null;
    }

    @Override
    public Writer getWriter() {
        return null;
    }

    public void indent(int n) throws SAXException {
    }

    @Override
    public boolean reset() {
        return false;
    }

    @Override
    public void serialize(Node node) throws IOException {
    }

    @Override
    public boolean setEscaping(boolean escape) {
        return false;
    }

    @Override
    public void setIndent(boolean indent) {
    }

    @Override
    public void setOutputFormat(Properties format) {
    }

    @Override
    public void setOutputStream(OutputStream output) {
    }

    @Override
    public void setWriter(Writer writer) {
    }

    @Override
    public void addAttribute(String uri, String localName, String rawName, String type, String value, boolean XSLAttribute) {
    }

    @Override
    public void attributeDecl(String arg0, String arg1, String arg2, String arg3, String arg4) throws SAXException {
    }

    @Override
    public void elementDecl(String arg0, String arg1) throws SAXException {
    }

    @Override
    public void externalEntityDecl(String arg0, String arg1, String arg2) throws SAXException {
    }

    @Override
    public void internalEntityDecl(String arg0, String arg1) throws SAXException {
    }

    @Override
    public void endPrefixMapping(String arg0) throws SAXException {
    }

    @Override
    public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException {
    }

    @Override
    public void processingInstruction(String arg0, String arg1) throws SAXException {
        if (this.m_tracer != null) {
            super.fireEscapingEvent(arg0, arg1);
        }
    }

    @Override
    public void setDocumentLocator(Locator arg0) {
    }

    @Override
    public void skippedEntity(String arg0) throws SAXException {
    }

    @Override
    public void startElement(String arg0, String arg1, String arg2, Attributes arg3) throws SAXException {
        flushPending();
        super.startElement(arg0, arg1, arg2, arg3);
    }

    @Override
    public void endCDATA() throws SAXException {
    }

    @Override
    public void endDTD() throws SAXException {
    }

    @Override
    public void startCDATA() throws SAXException {
    }

    @Override
    public void startEntity(String arg0) throws SAXException {
    }

    @Override
    public void startElement(String elementNamespaceURI, String elementLocalName, String elementName) throws SAXException {
        super.startElement(elementNamespaceURI, elementLocalName, elementName);
    }

    @Override
    public void startElement(String elementName) throws SAXException {
        super.startElement(elementName);
    }

    @Override
    public void endDocument() throws SAXException {
        flushPending();
        this.m_saxHandler.endDocument();
        if (this.m_tracer != null) {
            super.fireEndDoc();
        }
    }

    @Override
    public void characters(String characters) throws SAXException {
        int length = characters.length();
        if (length > this.m_charsBuff.length) {
            this.m_charsBuff = new char[(length * 2) + 1];
        }
        characters.getChars(0, length, this.m_charsBuff, 0);
        this.m_saxHandler.characters(this.m_charsBuff, 0, length);
    }

    @Override
    public void characters(char[] characters, int offset, int length) throws SAXException {
        this.m_saxHandler.characters(characters, offset, length);
        if (this.m_tracer != null) {
            super.fireCharEvent(characters, offset, length);
        }
    }

    @Override
    public void addAttribute(String name, String value) {
    }

    @Override
    public boolean startPrefixMapping(String prefix, String uri, boolean shouldFlush) throws SAXException {
        return false;
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }

    @Override
    public void namespaceAfterStartElement(String prefix, String uri) throws SAXException {
    }
}
