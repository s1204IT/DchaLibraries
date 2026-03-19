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
    public void endElement(String str) throws SAXException {
        if (this.m_tracer != null) {
            super.fireEndElem(str);
        }
    }

    @Override
    public void endElement(String str, String str2, String str3) throws SAXException {
        if (this.m_tracer != null) {
            super.fireEndElem(str3);
        }
    }

    public ToTextSAXHandler(ContentHandler contentHandler, LexicalHandler lexicalHandler, String str) {
        super(contentHandler, lexicalHandler, str);
    }

    public ToTextSAXHandler(ContentHandler contentHandler, String str) {
        super(contentHandler, str);
    }

    @Override
    public void comment(char[] cArr, int i, int i2) throws SAXException {
        if (this.m_tracer != null) {
            super.fireCommentEvent(cArr, i, i2);
        }
    }

    @Override
    public void comment(String str) throws SAXException {
        int length = str.length();
        if (length > this.m_charsBuff.length) {
            this.m_charsBuff = new char[(length * 2) + 1];
        }
        str.getChars(0, length, this.m_charsBuff, 0);
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

    public void indent(int i) throws SAXException {
    }

    @Override
    public boolean reset() {
        return false;
    }

    @Override
    public void serialize(Node node) throws IOException {
    }

    @Override
    public boolean setEscaping(boolean z) {
        return false;
    }

    @Override
    public void setIndent(boolean z) {
    }

    @Override
    public void setOutputFormat(Properties properties) {
    }

    @Override
    public void setOutputStream(OutputStream outputStream) {
    }

    @Override
    public void setWriter(Writer writer) {
    }

    @Override
    public void addAttribute(String str, String str2, String str3, String str4, String str5, boolean z) {
    }

    @Override
    public void attributeDecl(String str, String str2, String str3, String str4, String str5) throws SAXException {
    }

    @Override
    public void elementDecl(String str, String str2) throws SAXException {
    }

    @Override
    public void externalEntityDecl(String str, String str2, String str3) throws SAXException {
    }

    @Override
    public void internalEntityDecl(String str, String str2) throws SAXException {
    }

    @Override
    public void endPrefixMapping(String str) throws SAXException {
    }

    @Override
    public void ignorableWhitespace(char[] cArr, int i, int i2) throws SAXException {
    }

    @Override
    public void processingInstruction(String str, String str2) throws SAXException {
        if (this.m_tracer != null) {
            super.fireEscapingEvent(str, str2);
        }
    }

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void skippedEntity(String str) throws SAXException {
    }

    @Override
    public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
        flushPending();
        super.startElement(str, str2, str3, attributes);
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
    public void startEntity(String str) throws SAXException {
    }

    @Override
    public void startElement(String str, String str2, String str3) throws SAXException {
        super.startElement(str, str2, str3);
    }

    @Override
    public void startElement(String str) throws SAXException {
        super.startElement(str);
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
    public void characters(String str) throws SAXException {
        int length = str.length();
        if (length > this.m_charsBuff.length) {
            this.m_charsBuff = new char[(length * 2) + 1];
        }
        str.getChars(0, length, this.m_charsBuff, 0);
        this.m_saxHandler.characters(this.m_charsBuff, 0, length);
    }

    @Override
    public void characters(char[] cArr, int i, int i2) throws SAXException {
        this.m_saxHandler.characters(cArr, i, i2);
        if (this.m_tracer != null) {
            super.fireCharEvent(cArr, i, i2);
        }
    }

    @Override
    public void addAttribute(String str, String str2) {
    }

    @Override
    public boolean startPrefixMapping(String str, String str2, boolean z) throws SAXException {
        return false;
    }

    @Override
    public void startPrefixMapping(String str, String str2) throws SAXException {
    }

    @Override
    public void namespaceAfterStartElement(String str, String str2) throws SAXException {
    }
}
