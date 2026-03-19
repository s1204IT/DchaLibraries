package org.apache.xml.serializer;

import java.util.Vector;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;

public abstract class ToSAXHandler extends SerializerBase {
    protected LexicalHandler m_lexHandler;
    protected ContentHandler m_saxHandler;
    private boolean m_shouldGenerateNSAttribute = true;
    protected TransformStateSetter m_state = null;

    public ToSAXHandler() {
    }

    public ToSAXHandler(ContentHandler contentHandler, LexicalHandler lexicalHandler, String str) {
        setContentHandler(contentHandler);
        setLexHandler(lexicalHandler);
        setEncoding(str);
    }

    public ToSAXHandler(ContentHandler contentHandler, String str) {
        setContentHandler(contentHandler);
        setEncoding(str);
    }

    @Override
    protected void startDocumentInternal() throws SAXException {
        if (this.m_needToCallStartDocument) {
            super.startDocumentInternal();
            this.m_saxHandler.startDocument();
            this.m_needToCallStartDocument = false;
        }
    }

    @Override
    public void startDTD(String str, String str2, String str3) throws SAXException {
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
    public void comment(String str) throws SAXException {
        flushPending();
        if (this.m_lexHandler != null) {
            int length = str.length();
            if (length > this.m_charsBuff.length) {
                this.m_charsBuff = new char[(length * 2) + 1];
            }
            str.getChars(0, length, this.m_charsBuff, 0);
            this.m_lexHandler.comment(this.m_charsBuff, 0, length);
            if (this.m_tracer != null) {
                super.fireCommentEvent(this.m_charsBuff, 0, length);
            }
        }
    }

    @Override
    public void processingInstruction(String str, String str2) throws SAXException {
    }

    protected void closeStartTag() throws SAXException {
    }

    protected void closeCDATA() throws SAXException {
    }

    @Override
    public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
        if (this.m_state != null) {
            this.m_state.resetState(getTransformer());
        }
        if (this.m_tracer != null) {
            super.fireStartElem(str3);
        }
    }

    public void setLexHandler(LexicalHandler lexicalHandler) {
        this.m_lexHandler = lexicalHandler;
    }

    @Override
    public void setContentHandler(ContentHandler contentHandler) {
        this.m_saxHandler = contentHandler;
        if (this.m_lexHandler == null && (contentHandler instanceof LexicalHandler)) {
            this.m_lexHandler = (LexicalHandler) contentHandler;
        }
    }

    @Override
    public void setCdataSectionElements(Vector vector) {
    }

    public void setShouldOutputNSAttr(boolean z) {
        this.m_shouldGenerateNSAttribute = z;
    }

    boolean getShouldOutputNSAttr() {
        return this.m_shouldGenerateNSAttribute;
    }

    @Override
    public void flushPending() throws SAXException {
        if (this.m_needToCallStartDocument) {
            startDocumentInternal();
            this.m_needToCallStartDocument = false;
        }
        if (this.m_elemContext.m_startTagOpen) {
            closeStartTag();
            this.m_elemContext.m_startTagOpen = false;
        }
        if (this.m_cdataTagOpen) {
            closeCDATA();
            this.m_cdataTagOpen = false;
        }
    }

    public void setTransformState(TransformStateSetter transformStateSetter) {
        this.m_state = transformStateSetter;
    }

    @Override
    public void startElement(String str, String str2, String str3) throws SAXException {
        if (this.m_state != null) {
            this.m_state.resetState(getTransformer());
        }
        if (this.m_tracer != null) {
            super.fireStartElem(str3);
        }
    }

    @Override
    public void startElement(String str) throws SAXException {
        if (this.m_state != null) {
            this.m_state.resetState(getTransformer());
        }
        if (this.m_tracer != null) {
            super.fireStartElem(str);
        }
    }

    @Override
    public void characters(Node node) throws SAXException {
        if (this.m_state != null) {
            this.m_state.setCurrentNode(node);
        }
        String nodeValue = node.getNodeValue();
        if (nodeValue != null) {
            characters(nodeValue);
        }
    }

    @Override
    public void fatalError(SAXParseException sAXParseException) throws SAXException {
        super.fatalError(sAXParseException);
        this.m_needToCallStartDocument = false;
        if (this.m_saxHandler instanceof ErrorHandler) {
            ((ErrorHandler) this.m_saxHandler).fatalError(sAXParseException);
        }
    }

    @Override
    public void error(SAXParseException sAXParseException) throws SAXException {
        super.error(sAXParseException);
        if (this.m_saxHandler instanceof ErrorHandler) {
            ((ErrorHandler) this.m_saxHandler).error(sAXParseException);
        }
    }

    @Override
    public void warning(SAXParseException sAXParseException) throws SAXException {
        super.warning(sAXParseException);
        if (this.m_saxHandler instanceof ErrorHandler) {
            ((ErrorHandler) this.m_saxHandler).warning(sAXParseException);
        }
    }

    @Override
    public boolean reset() {
        if (super.reset()) {
            resetToSAXHandler();
            return true;
        }
        return false;
    }

    private void resetToSAXHandler() {
        this.m_lexHandler = null;
        this.m_saxHandler = null;
        this.m_state = null;
        this.m_shouldGenerateNSAttribute = false;
    }

    @Override
    public void addUniqueAttribute(String str, String str2, int i) throws SAXException {
        addAttribute(str, str2);
    }
}
