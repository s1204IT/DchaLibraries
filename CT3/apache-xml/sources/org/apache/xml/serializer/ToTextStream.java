package org.apache.xml.serializer;

import java.io.IOException;
import java.io.Writer;
import org.apache.xml.serializer.utils.MsgKey;
import org.apache.xml.serializer.utils.Utils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ToTextStream extends ToStream {
    @Override
    protected void startDocumentInternal() throws SAXException {
        super.startDocumentInternal();
        this.m_needToCallStartDocument = false;
    }

    @Override
    public void endDocument() throws SAXException {
        flushPending();
        flushWriter();
        if (this.m_tracer == null) {
            return;
        }
        super.fireEndDoc();
    }

    @Override
    public void startElement(String namespaceURI, String localName, String name, Attributes atts) throws SAXException {
        if (this.m_tracer != null) {
            super.fireStartElem(name);
            firePseudoAttributes();
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String name) throws SAXException {
        if (this.m_tracer == null) {
            return;
        }
        super.fireEndElem(name);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        flushPending();
        try {
            if (inTemporaryOutputState()) {
                this.m_writer.write(ch, start, length);
            } else {
                writeNormalizedChars(ch, start, length, this.m_lineSepUse);
            }
            if (this.m_tracer == null) {
                return;
            }
            super.fireCharEvent(ch, start, length);
        } catch (IOException ioe) {
            throw new SAXException(ioe);
        }
    }

    @Override
    public void charactersRaw(char[] ch, int start, int length) throws SAXException {
        try {
            writeNormalizedChars(ch, start, length, this.m_lineSepUse);
        } catch (IOException ioe) {
            throw new SAXException(ioe);
        }
    }

    void writeNormalizedChars(char[] ch, int start, int length, boolean useLineSep) throws SAXException, IOException {
        String encoding = getEncoding();
        Writer writer = this.m_writer;
        int end = start + length;
        int i = start;
        while (i < end) {
            char c = ch[i];
            if ('\n' == c && useLineSep) {
                writer.write(this.m_lineSep, 0, this.m_lineSepLen);
            } else if (this.m_encodingInfo.isInEncoding(c)) {
                writer.write(c);
            } else if (Encodings.isHighUTF16Surrogate(c)) {
                int codePoint = writeUTF16Surrogate(c, ch, i, end);
                if (codePoint != 0) {
                    String integralValue = Integer.toString(codePoint);
                    String msg = Utils.messages.createMessage(MsgKey.ER_ILLEGAL_CHARACTER, new Object[]{integralValue, encoding});
                    System.err.println(msg);
                }
                i++;
            } else if (encoding != null) {
                writer.write(38);
                writer.write(35);
                writer.write(Integer.toString(c));
                writer.write(59);
                String integralValue2 = Integer.toString(c);
                String msg2 = Utils.messages.createMessage(MsgKey.ER_ILLEGAL_CHARACTER, new Object[]{integralValue2, encoding});
                System.err.println(msg2);
            } else {
                writer.write(c);
            }
            i++;
        }
    }

    @Override
    public void cdata(char[] ch, int start, int length) throws SAXException {
        try {
            writeNormalizedChars(ch, start, length, this.m_lineSepUse);
            if (this.m_tracer == null) {
                return;
            }
            super.fireCDATAEvent(ch, start, length);
        } catch (IOException ioe) {
            throw new SAXException(ioe);
        }
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        try {
            writeNormalizedChars(ch, start, length, this.m_lineSepUse);
        } catch (IOException ioe) {
            throw new SAXException(ioe);
        }
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        flushPending();
        if (this.m_tracer == null) {
            return;
        }
        super.fireEscapingEvent(target, data);
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
    public void comment(char[] ch, int start, int length) throws SAXException {
        flushPending();
        if (this.m_tracer == null) {
            return;
        }
        super.fireCommentEvent(ch, start, length);
    }

    @Override
    public void entityReference(String name) throws SAXException {
        if (this.m_tracer == null) {
            return;
        }
        super.fireEntityReference(name);
    }

    @Override
    public void addAttribute(String uri, String localName, String rawName, String type, String value, boolean XSLAttribute) {
    }

    @Override
    public void endCDATA() throws SAXException {
    }

    @Override
    public void endElement(String elemName) throws SAXException {
        if (this.m_tracer == null) {
            return;
        }
        super.fireEndElem(elemName);
    }

    @Override
    public void startElement(String elementNamespaceURI, String elementLocalName, String elementName) throws SAXException {
        if (this.m_needToCallStartDocument) {
            startDocumentInternal();
        }
        if (this.m_tracer != null) {
            super.fireStartElem(elementName);
            firePseudoAttributes();
        }
    }

    @Override
    public void characters(String characters) throws SAXException {
        int length = characters.length();
        if (length > this.m_charsBuff.length) {
            this.m_charsBuff = new char[(length * 2) + 1];
        }
        characters.getChars(0, length, this.m_charsBuff, 0);
        characters(this.m_charsBuff, 0, length);
    }

    @Override
    public void addAttribute(String name, String value) {
    }

    @Override
    public void addUniqueAttribute(String qName, String value, int flags) throws SAXException {
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

    @Override
    public void flushPending() throws SAXException {
        if (!this.m_needToCallStartDocument) {
            return;
        }
        startDocumentInternal();
        this.m_needToCallStartDocument = false;
    }
}
