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
        if (this.m_tracer != null) {
            super.fireEndDoc();
        }
    }

    @Override
    public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
        if (this.m_tracer != null) {
            super.fireStartElem(str3);
            firePseudoAttributes();
        }
    }

    @Override
    public void endElement(String str, String str2, String str3) throws SAXException {
        if (this.m_tracer != null) {
            super.fireEndElem(str3);
        }
    }

    @Override
    public void characters(char[] cArr, int i, int i2) throws SAXException {
        flushPending();
        try {
            if (inTemporaryOutputState()) {
                this.m_writer.write(cArr, i, i2);
            } else {
                writeNormalizedChars(cArr, i, i2, this.m_lineSepUse);
            }
            if (this.m_tracer != null) {
                super.fireCharEvent(cArr, i, i2);
            }
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void charactersRaw(char[] cArr, int i, int i2) throws SAXException {
        try {
            writeNormalizedChars(cArr, i, i2, this.m_lineSepUse);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    void writeNormalizedChars(char[] cArr, int i, int i2, boolean z) throws SAXException, IOException {
        String encoding = getEncoding();
        Writer writer = this.m_writer;
        int i3 = i2 + i;
        while (i < i3) {
            char c = cArr[i];
            if ('\n' == c && z) {
                writer.write(this.m_lineSep, 0, this.m_lineSepLen);
            } else if (this.m_encodingInfo.isInEncoding(c)) {
                writer.write(c);
            } else if (Encodings.isHighUTF16Surrogate(c)) {
                int iWriteUTF16Surrogate = writeUTF16Surrogate(c, cArr, i, i3);
                if (iWriteUTF16Surrogate != 0) {
                    System.err.println(Utils.messages.createMessage(MsgKey.ER_ILLEGAL_CHARACTER, new Object[]{Integer.toString(iWriteUTF16Surrogate), encoding}));
                }
                i++;
            } else if (encoding != null) {
                writer.write(38);
                writer.write(35);
                writer.write(Integer.toString(c));
                writer.write(59);
                System.err.println(Utils.messages.createMessage(MsgKey.ER_ILLEGAL_CHARACTER, new Object[]{Integer.toString(c), encoding}));
            } else {
                writer.write(c);
            }
            i++;
        }
    }

    @Override
    public void cdata(char[] cArr, int i, int i2) throws SAXException {
        try {
            writeNormalizedChars(cArr, i, i2, this.m_lineSepUse);
            if (this.m_tracer != null) {
                super.fireCDATAEvent(cArr, i, i2);
            }
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void ignorableWhitespace(char[] cArr, int i, int i2) throws SAXException {
        try {
            writeNormalizedChars(cArr, i, i2, this.m_lineSepUse);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void processingInstruction(String str, String str2) throws SAXException {
        flushPending();
        if (this.m_tracer != null) {
            super.fireEscapingEvent(str, str2);
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
    public void comment(char[] cArr, int i, int i2) throws SAXException {
        flushPending();
        if (this.m_tracer != null) {
            super.fireCommentEvent(cArr, i, i2);
        }
    }

    @Override
    public void entityReference(String str) throws SAXException {
        if (this.m_tracer != null) {
            super.fireEntityReference(str);
        }
    }

    @Override
    public void addAttribute(String str, String str2, String str3, String str4, String str5, boolean z) {
    }

    @Override
    public void endCDATA() throws SAXException {
    }

    @Override
    public void endElement(String str) throws SAXException {
        if (this.m_tracer != null) {
            super.fireEndElem(str);
        }
    }

    @Override
    public void startElement(String str, String str2, String str3) throws SAXException {
        if (this.m_needToCallStartDocument) {
            startDocumentInternal();
        }
        if (this.m_tracer != null) {
            super.fireStartElem(str3);
            firePseudoAttributes();
        }
    }

    @Override
    public void characters(String str) throws SAXException {
        int length = str.length();
        if (length > this.m_charsBuff.length) {
            this.m_charsBuff = new char[(length * 2) + 1];
        }
        str.getChars(0, length, this.m_charsBuff, 0);
        characters(this.m_charsBuff, 0, length);
    }

    @Override
    public void addAttribute(String str, String str2) {
    }

    @Override
    public void addUniqueAttribute(String str, String str2, int i) throws SAXException {
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

    @Override
    public void flushPending() throws SAXException {
        if (this.m_needToCallStartDocument) {
            startDocumentInternal();
            this.m_needToCallStartDocument = false;
        }
    }
}
