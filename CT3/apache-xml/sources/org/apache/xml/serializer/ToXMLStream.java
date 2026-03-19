package org.apache.xml.serializer;

import java.io.IOException;
import java.io.Writer;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import org.apache.xml.serializer.utils.MsgKey;
import org.apache.xml.serializer.utils.Utils;
import org.xml.sax.SAXException;

public class ToXMLStream extends ToStream {
    private CharInfo m_xmlcharInfo = CharInfo.getCharInfo(CharInfo.XML_ENTITIES_RESOURCE, "xml");

    public ToXMLStream() {
        this.m_charInfo = this.m_xmlcharInfo;
        initCDATA();
        this.m_prefixMap = new NamespaceMappings();
    }

    public void CopyFrom(ToXMLStream xmlListener) {
        setWriter(xmlListener.m_writer);
        String encoding = xmlListener.getEncoding();
        setEncoding(encoding);
        setOmitXMLDeclaration(xmlListener.getOmitXMLDeclaration());
        this.m_ispreserve = xmlListener.m_ispreserve;
        this.m_preserves = xmlListener.m_preserves;
        this.m_isprevtext = xmlListener.m_isprevtext;
        this.m_doIndent = xmlListener.m_doIndent;
        setIndentAmount(xmlListener.getIndentAmount());
        this.m_startNewLine = xmlListener.m_startNewLine;
        this.m_needToOutputDocTypeDecl = xmlListener.m_needToOutputDocTypeDecl;
        setDoctypeSystem(xmlListener.getDoctypeSystem());
        setDoctypePublic(xmlListener.getDoctypePublic());
        setStandalone(xmlListener.getStandalone());
        setMediaType(xmlListener.getMediaType());
        this.m_encodingInfo = xmlListener.m_encodingInfo;
        this.m_spaceBeforeClose = xmlListener.m_spaceBeforeClose;
        this.m_cdataStartCalled = xmlListener.m_cdataStartCalled;
    }

    @Override
    public void startDocumentInternal() throws SAXException {
        String standalone;
        if (!this.m_needToCallStartDocument) {
            return;
        }
        super.startDocumentInternal();
        this.m_needToCallStartDocument = false;
        if (this.m_inEntityRef) {
            return;
        }
        this.m_needToOutputDocTypeDecl = true;
        this.m_startNewLine = false;
        String version = getXMLVersion();
        if (getOmitXMLDeclaration()) {
            return;
        }
        String encoding = Encodings.getMimeEncoding(getEncoding());
        if (this.m_standaloneWasSpecified) {
            standalone = " standalone=\"" + getStandalone() + "\"";
        } else {
            standalone = "";
        }
        try {
            Writer writer = this.m_writer;
            writer.write("<?xml version=\"");
            writer.write(version);
            writer.write("\" encoding=\"");
            writer.write(encoding);
            writer.write(34);
            writer.write(standalone);
            writer.write("?>");
            if (!this.m_doIndent) {
                return;
            }
            if (!this.m_standaloneWasSpecified && getDoctypePublic() == null && getDoctypeSystem() == null) {
                return;
            }
            writer.write(this.m_lineSep, 0, this.m_lineSepLen);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void endDocument() throws SAXException {
        flushPending();
        if (this.m_doIndent && !this.m_isprevtext) {
            try {
                outputLineSep();
            } catch (IOException e) {
                throw new SAXException(e);
            }
        }
        flushWriter();
        if (this.m_tracer == null) {
            return;
        }
        super.fireEndDoc();
    }

    public void startPreserving() throws SAXException {
        this.m_preserves.push(true);
        this.m_ispreserve = true;
    }

    public void endPreserving() throws SAXException {
        this.m_ispreserve = this.m_preserves.isEmpty() ? false : this.m_preserves.pop();
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        if (this.m_inEntityRef) {
            return;
        }
        flushPending();
        if (target.equals("javax.xml.transform.disable-output-escaping")) {
            startNonEscaping();
        } else if (target.equals("javax.xml.transform.enable-output-escaping")) {
            endNonEscaping();
        } else {
            try {
                if (this.m_elemContext.m_startTagOpen) {
                    closeStartTag();
                    this.m_elemContext.m_startTagOpen = false;
                } else if (this.m_needToCallStartDocument) {
                    startDocumentInternal();
                }
                if (shouldIndent()) {
                    indent();
                }
                Writer writer = this.m_writer;
                writer.write("<?");
                writer.write(target);
                if (data.length() > 0 && !Character.isSpaceChar(data.charAt(0))) {
                    writer.write(32);
                }
                int indexOfQLT = data.indexOf("?>");
                if (indexOfQLT >= 0) {
                    if (indexOfQLT > 0) {
                        writer.write(data.substring(0, indexOfQLT));
                    }
                    writer.write("? >");
                    if (indexOfQLT + 2 < data.length()) {
                        writer.write(data.substring(indexOfQLT + 2));
                    }
                } else {
                    writer.write(data);
                }
                writer.write(63);
                writer.write(62);
                this.m_startNewLine = true;
            } catch (IOException e) {
                throw new SAXException(e);
            }
        }
        if (this.m_tracer == null) {
            return;
        }
        super.fireEscapingEvent(target, data);
    }

    @Override
    public void entityReference(String name) throws SAXException {
        if (this.m_elemContext.m_startTagOpen) {
            closeStartTag();
            this.m_elemContext.m_startTagOpen = false;
        }
        try {
            if (shouldIndent()) {
                indent();
            }
            Writer writer = this.m_writer;
            writer.write(38);
            writer.write(name);
            writer.write(59);
            if (this.m_tracer == null) {
                return;
            }
            super.fireEntityReference(name);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void addUniqueAttribute(String name, String value, int flags) throws SAXException {
        if (!this.m_elemContext.m_startTagOpen) {
            return;
        }
        try {
            String patchedName = patchName(name);
            Writer writer = this.m_writer;
            if ((flags & 1) > 0 && this.m_xmlcharInfo.onlyQuotAmpLtGt) {
                writer.write(32);
                writer.write(patchedName);
                writer.write("=\"");
                writer.write(value);
                writer.write(34);
            } else {
                writer.write(32);
                writer.write(patchedName);
                writer.write("=\"");
                writeAttrString(writer, value, getEncoding());
                writer.write(34);
            }
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void addAttribute(String uri, String localName, String rawName, String type, String value, boolean xslAttribute) throws SAXException {
        String prefixUsed;
        if (this.m_elemContext.m_startTagOpen) {
            boolean was_added = addAttributeAlways(uri, localName, rawName, type, value, xslAttribute);
            if (was_added && !xslAttribute && !rawName.startsWith("xmlns") && (prefixUsed = ensureAttributesNamespaceIsDeclared(uri, localName, rawName)) != null && rawName != null && !rawName.startsWith(prefixUsed)) {
                rawName = prefixUsed + ":" + localName;
            }
            addAttributeAlways(uri, localName, rawName, type, value, xslAttribute);
            return;
        }
        String msg = Utils.messages.createMessage(MsgKey.ER_ILLEGAL_ATTRIBUTE_POSITION, new Object[]{localName});
        try {
            Transformer tran = super.getTransformer();
            ErrorListener errHandler = tran.getErrorListener();
            if (errHandler != null && this.m_sourceLocator != null) {
                errHandler.warning(new TransformerException(msg, this.m_sourceLocator));
            } else {
                System.out.println(msg);
            }
        } catch (TransformerException e) {
            SAXException se = new SAXException(e);
            throw se;
        }
    }

    @Override
    public void endElement(String elemName) throws SAXException {
        endElement(null, null, elemName);
    }

    @Override
    public void namespaceAfterStartElement(String prefix, String uri) throws SAXException {
        if (this.m_elemContext.m_elementURI == null) {
            String prefix1 = getPrefixPart(this.m_elemContext.m_elementName);
            if (prefix1 == null && "".equals(prefix)) {
                this.m_elemContext.m_elementURI = uri;
            }
        }
        startPrefixMapping(prefix, uri, false);
    }

    protected boolean pushNamespace(String prefix, String uri) {
        try {
            if (this.m_prefixMap.pushNamespace(prefix, uri, this.m_elemContext.m_currentElemDepth)) {
                startPrefixMapping(prefix, uri);
                return true;
            }
            return false;
        } catch (SAXException e) {
            return false;
        }
    }

    @Override
    public boolean reset() {
        if (!super.reset()) {
            return false;
        }
        return true;
    }

    private void resetToXMLStream() {
    }

    private String getXMLVersion() {
        String xmlVersion = getVersion();
        if (xmlVersion == null || xmlVersion.equals(SerializerConstants.XMLVERSION10)) {
            return SerializerConstants.XMLVERSION10;
        }
        if (xmlVersion.equals(SerializerConstants.XMLVERSION11)) {
            return SerializerConstants.XMLVERSION11;
        }
        String msg = Utils.messages.createMessage(MsgKey.ER_XML_VERSION_NOT_SUPPORTED, new Object[]{xmlVersion});
        try {
            Transformer tran = super.getTransformer();
            ErrorListener errHandler = tran.getErrorListener();
            if (errHandler != null && this.m_sourceLocator != null) {
                errHandler.warning(new TransformerException(msg, this.m_sourceLocator));
            } else {
                System.out.println(msg);
            }
        } catch (Exception e) {
        }
        return SerializerConstants.XMLVERSION10;
    }
}
