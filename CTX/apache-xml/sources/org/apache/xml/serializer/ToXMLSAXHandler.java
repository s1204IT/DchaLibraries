package org.apache.xml.serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;
import org.apache.xalan.templates.Constants;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

public final class ToXMLSAXHandler extends ToSAXHandler {
    protected boolean m_escapeSetting;

    public ToXMLSAXHandler() {
        this.m_escapeSetting = true;
        this.m_prefixMap = new NamespaceMappings();
        initCDATA();
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
    public void serialize(Node node) throws IOException {
    }

    @Override
    public boolean setEscaping(boolean z) throws SAXException {
        boolean z2 = this.m_escapeSetting;
        this.m_escapeSetting = z;
        if (z) {
            processingInstruction("javax.xml.transform.enable-output-escaping", "");
        } else {
            processingInstruction("javax.xml.transform.disable-output-escaping", "");
        }
        return z2;
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
    public void endDocument() throws SAXException {
        flushPending();
        this.m_saxHandler.endDocument();
        if (this.m_tracer != null) {
            super.fireEndDoc();
        }
    }

    @Override
    protected void closeStartTag() throws SAXException {
        this.m_elemContext.m_startTagOpen = false;
        String localName = getLocalName(this.m_elemContext.m_elementName);
        String namespaceURI = getNamespaceURI(this.m_elemContext.m_elementName, true);
        if (this.m_needToCallStartDocument) {
            startDocumentInternal();
        }
        this.m_saxHandler.startElement(namespaceURI, localName, this.m_elemContext.m_elementName, this.m_attributes);
        this.m_attributes.clear();
        if (this.m_state != null) {
            this.m_state.setCurrentNode(null);
        }
    }

    @Override
    public void closeCDATA() throws SAXException {
        if (this.m_lexHandler != null && this.m_cdataTagOpen) {
            this.m_lexHandler.endCDATA();
        }
        this.m_cdataTagOpen = false;
    }

    @Override
    public void endElement(String str, String str2, String str3) throws SAXException {
        flushPending();
        if (str == null) {
            if (this.m_elemContext.m_elementURI != null) {
                str = this.m_elemContext.m_elementURI;
            } else {
                str = getNamespaceURI(str3, true);
            }
        }
        if (str2 == null) {
            if (this.m_elemContext.m_elementLocalName != null) {
                str2 = this.m_elemContext.m_elementLocalName;
            } else {
                str2 = getLocalName(str3);
            }
        }
        this.m_saxHandler.endElement(str, str2, str3);
        if (this.m_tracer != null) {
            super.fireEndElem(str3);
        }
        this.m_prefixMap.popNamespaces(this.m_elemContext.m_currentElemDepth, this.m_saxHandler);
        this.m_elemContext = this.m_elemContext.m_prev;
    }

    @Override
    public void endPrefixMapping(String str) throws SAXException {
    }

    @Override
    public void ignorableWhitespace(char[] cArr, int i, int i2) throws SAXException {
        this.m_saxHandler.ignorableWhitespace(cArr, i, i2);
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.m_saxHandler.setDocumentLocator(locator);
    }

    @Override
    public void skippedEntity(String str) throws SAXException {
        this.m_saxHandler.skippedEntity(str);
    }

    @Override
    public void startPrefixMapping(String str, String str2) throws SAXException {
        startPrefixMapping(str, str2, true);
    }

    @Override
    public boolean startPrefixMapping(String str, String str2, boolean z) throws SAXException {
        int i;
        if (z) {
            flushPending();
            i = this.m_elemContext.m_currentElemDepth + 1;
        } else {
            i = this.m_elemContext.m_currentElemDepth;
        }
        boolean zPushNamespace = this.m_prefixMap.pushNamespace(str, str2, i);
        if (zPushNamespace) {
            this.m_saxHandler.startPrefixMapping(str, str2);
            if (getShouldOutputNSAttr()) {
                if ("".equals(str)) {
                    addAttributeAlways(SerializerConstants.XMLNS_URI, "xmlns", "xmlns", "CDATA", str2, false);
                } else if (!"".equals(str2)) {
                    addAttributeAlways(SerializerConstants.XMLNS_URI, str, Constants.ATTRNAME_XMLNS + str, "CDATA", str2, false);
                }
            }
        }
        return zPushNamespace;
    }

    @Override
    public void comment(char[] cArr, int i, int i2) throws SAXException {
        flushPending();
        if (this.m_lexHandler != null) {
            this.m_lexHandler.comment(cArr, i, i2);
        }
        if (this.m_tracer != null) {
            super.fireCommentEvent(cArr, i, i2);
        }
    }

    @Override
    public void endCDATA() throws SAXException {
    }

    @Override
    public void endDTD() throws SAXException {
        if (this.m_lexHandler != null) {
            this.m_lexHandler.endDTD();
        }
    }

    @Override
    public void startEntity(String str) throws SAXException {
        if (this.m_lexHandler != null) {
            this.m_lexHandler.startEntity(str);
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

    public ToXMLSAXHandler(ContentHandler contentHandler, String str) {
        super(contentHandler, str);
        this.m_escapeSetting = true;
        initCDATA();
        this.m_prefixMap = new NamespaceMappings();
    }

    public ToXMLSAXHandler(ContentHandler contentHandler, LexicalHandler lexicalHandler, String str) {
        super(contentHandler, lexicalHandler, str);
        this.m_escapeSetting = true;
        initCDATA();
        this.m_prefixMap = new NamespaceMappings();
    }

    @Override
    public void startElement(String str, String str2, String str3) throws SAXException {
        startElement(str, str2, str3, null);
    }

    @Override
    public void startElement(String str) throws SAXException {
        startElement(null, null, str, null);
    }

    @Override
    public void characters(char[] cArr, int i, int i2) throws SAXException {
        if (this.m_needToCallStartDocument) {
            startDocumentInternal();
            this.m_needToCallStartDocument = false;
        }
        if (this.m_elemContext.m_startTagOpen) {
            closeStartTag();
            this.m_elemContext.m_startTagOpen = false;
        }
        if (this.m_elemContext.m_isCdataSection && !this.m_cdataTagOpen && this.m_lexHandler != null) {
            this.m_lexHandler.startCDATA();
            this.m_cdataTagOpen = true;
        }
        this.m_saxHandler.characters(cArr, i, i2);
        if (this.m_tracer != null) {
            fireCharEvent(cArr, i, i2);
        }
    }

    @Override
    public void endElement(String str) throws SAXException {
        endElement(null, null, str);
    }

    @Override
    public void namespaceAfterStartElement(String str, String str2) throws SAXException {
        startPrefixMapping(str, str2, false);
    }

    @Override
    public void processingInstruction(String str, String str2) throws SAXException {
        flushPending();
        this.m_saxHandler.processingInstruction(str, str2);
        if (this.m_tracer != null) {
            super.fireEscapingEvent(str, str2);
        }
    }

    protected boolean popNamespace(String str) {
        try {
            if (this.m_prefixMap.popNamespace(str)) {
                this.m_saxHandler.endPrefixMapping(str);
                return true;
            }
            return false;
        } catch (SAXException e) {
            return false;
        }
    }

    @Override
    public void startCDATA() throws SAXException {
        if (!this.m_cdataTagOpen) {
            flushPending();
            if (this.m_lexHandler != null) {
                this.m_lexHandler.startCDATA();
                this.m_cdataTagOpen = true;
            }
        }
    }

    @Override
    public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
        flushPending();
        super.startElement(str, str2, str3, attributes);
        if (this.m_needToOutputDocTypeDecl) {
            String doctypeSystem = getDoctypeSystem();
            if (doctypeSystem != null && this.m_lexHandler != null) {
                String doctypePublic = getDoctypePublic();
                if (doctypeSystem != null) {
                    this.m_lexHandler.startDTD(str3, doctypePublic, doctypeSystem);
                }
            }
            this.m_needToOutputDocTypeDecl = false;
        }
        this.m_elemContext = this.m_elemContext.push(str, str2, str3);
        if (str != null) {
            ensurePrefixIsDeclared(str, str3);
        }
        if (attributes != null) {
            addAttributes(attributes);
        }
        this.m_elemContext.m_isCdataSection = isCdataSection();
    }

    private void ensurePrefixIsDeclared(String str, String str2) throws SAXException {
        String str3;
        if (str != null && str.length() > 0) {
            int iIndexOf = str2.indexOf(":");
            boolean z = iIndexOf < 0;
            String strSubstring = z ? "" : str2.substring(0, iIndexOf);
            if (strSubstring != null) {
                String strLookupNamespace = this.m_prefixMap.lookupNamespace(strSubstring);
                if (strLookupNamespace == null || !strLookupNamespace.equals(str)) {
                    startPrefixMapping(strSubstring, str, false);
                    if (getShouldOutputNSAttr()) {
                        String str4 = z ? "xmlns" : strSubstring;
                        if (z) {
                            str3 = "xmlns";
                        } else {
                            str3 = Constants.ATTRNAME_XMLNS + strSubstring;
                        }
                        addAttributeAlways(SerializerConstants.XMLNS_URI, str4, str3, "CDATA", str, false);
                    }
                }
            }
        }
    }

    @Override
    public void addAttribute(String str, String str2, String str3, String str4, String str5, boolean z) throws SAXException {
        if (this.m_elemContext.m_startTagOpen) {
            ensurePrefixIsDeclared(str, str3);
            addAttributeAlways(str, str2, str3, str4, str5, false);
        }
    }

    @Override
    public boolean reset() {
        if (super.reset()) {
            resetToXMLSAXHandler();
            return true;
        }
        return false;
    }

    private void resetToXMLSAXHandler() {
        this.m_escapeSetting = true;
    }
}
