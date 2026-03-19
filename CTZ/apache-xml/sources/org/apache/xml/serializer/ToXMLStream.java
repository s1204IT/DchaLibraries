package org.apache.xml.serializer;

import java.io.IOException;
import java.io.Writer;
import javax.xml.transform.ErrorListener;
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

    public void CopyFrom(ToXMLStream toXMLStream) {
        setWriter(toXMLStream.m_writer);
        setEncoding(toXMLStream.getEncoding());
        setOmitXMLDeclaration(toXMLStream.getOmitXMLDeclaration());
        this.m_ispreserve = toXMLStream.m_ispreserve;
        this.m_preserves = toXMLStream.m_preserves;
        this.m_isprevtext = toXMLStream.m_isprevtext;
        this.m_doIndent = toXMLStream.m_doIndent;
        setIndentAmount(toXMLStream.getIndentAmount());
        this.m_startNewLine = toXMLStream.m_startNewLine;
        this.m_needToOutputDocTypeDecl = toXMLStream.m_needToOutputDocTypeDecl;
        setDoctypeSystem(toXMLStream.getDoctypeSystem());
        setDoctypePublic(toXMLStream.getDoctypePublic());
        setStandalone(toXMLStream.getStandalone());
        setMediaType(toXMLStream.getMediaType());
        this.m_encodingInfo = toXMLStream.m_encodingInfo;
        this.m_spaceBeforeClose = toXMLStream.m_spaceBeforeClose;
        this.m_cdataStartCalled = toXMLStream.m_cdataStartCalled;
    }

    @Override
    public void startDocumentInternal() throws SAXException {
        String str;
        if (this.m_needToCallStartDocument) {
            super.startDocumentInternal();
            this.m_needToCallStartDocument = false;
            if (this.m_inEntityRef) {
                return;
            }
            this.m_needToOutputDocTypeDecl = true;
            this.m_startNewLine = false;
            String xMLVersion = getXMLVersion();
            if (!getOmitXMLDeclaration()) {
                String mimeEncoding = Encodings.getMimeEncoding(getEncoding());
                if (this.m_standaloneWasSpecified) {
                    str = " standalone=\"" + getStandalone() + "\"";
                } else {
                    str = "";
                }
                try {
                    Writer writer = this.m_writer;
                    writer.write("<?xml version=\"");
                    writer.write(xMLVersion);
                    writer.write("\" encoding=\"");
                    writer.write(mimeEncoding);
                    writer.write(34);
                    writer.write(str);
                    writer.write("?>");
                    if (this.m_doIndent) {
                        if (this.m_standaloneWasSpecified || getDoctypePublic() != null || getDoctypeSystem() != null) {
                            writer.write(this.m_lineSep, 0, this.m_lineSepLen);
                        }
                    }
                } catch (IOException e) {
                    throw new SAXException(e);
                }
            }
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
        if (this.m_tracer != null) {
            super.fireEndDoc();
        }
    }

    public void startPreserving() throws SAXException {
        this.m_preserves.push(true);
        this.m_ispreserve = true;
    }

    public void endPreserving() throws SAXException {
        this.m_ispreserve = this.m_preserves.isEmpty() ? false : this.m_preserves.pop();
    }

    @Override
    public void processingInstruction(String str, String str2) throws SAXException {
        if (this.m_inEntityRef) {
            return;
        }
        flushPending();
        if (str.equals("javax.xml.transform.disable-output-escaping")) {
            startNonEscaping();
        } else if (str.equals("javax.xml.transform.enable-output-escaping")) {
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
                writer.write(str);
                if (str2.length() > 0 && !Character.isSpaceChar(str2.charAt(0))) {
                    writer.write(32);
                }
                int iIndexOf = str2.indexOf("?>");
                if (iIndexOf >= 0) {
                    if (iIndexOf > 0) {
                        writer.write(str2.substring(0, iIndexOf));
                    }
                    writer.write("? >");
                    int i = iIndexOf + 2;
                    if (i < str2.length()) {
                        writer.write(str2.substring(i));
                    }
                } else {
                    writer.write(str2);
                }
                writer.write(63);
                writer.write(62);
                this.m_startNewLine = true;
            } catch (IOException e) {
                throw new SAXException(e);
            }
        }
        if (this.m_tracer != null) {
            super.fireEscapingEvent(str, str2);
        }
    }

    @Override
    public void entityReference(String str) throws SAXException {
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
            writer.write(str);
            writer.write(59);
            if (this.m_tracer != null) {
                super.fireEntityReference(str);
            }
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void addUniqueAttribute(String str, String str2, int i) throws SAXException {
        if (this.m_elemContext.m_startTagOpen) {
            try {
                String strPatchName = patchName(str);
                Writer writer = this.m_writer;
                if ((i & 1) > 0 && this.m_xmlcharInfo.onlyQuotAmpLtGt) {
                    writer.write(32);
                    writer.write(strPatchName);
                    writer.write("=\"");
                    writer.write(str2);
                    writer.write(34);
                } else {
                    writer.write(32);
                    writer.write(strPatchName);
                    writer.write("=\"");
                    writeAttrString(writer, str2, getEncoding());
                    writer.write(34);
                }
            } catch (IOException e) {
                throw new SAXException(e);
            }
        }
    }

    @Override
    public void addAttribute(String str, String str2, String str3, String str4, String str5, boolean z) throws SAXException {
        String strEnsureAttributesNamespaceIsDeclared;
        if (this.m_elemContext.m_startTagOpen) {
            if (addAttributeAlways(str, str2, str3, str4, str5, z) && !z && !str3.startsWith("xmlns") && (strEnsureAttributesNamespaceIsDeclared = ensureAttributesNamespaceIsDeclared(str, str2, str3)) != null && str3 != null && !str3.startsWith(strEnsureAttributesNamespaceIsDeclared)) {
                str3 = strEnsureAttributesNamespaceIsDeclared + ":" + str2;
            }
            addAttributeAlways(str, str2, str3, str4, str5, z);
            return;
        }
        String strCreateMessage = Utils.messages.createMessage(MsgKey.ER_ILLEGAL_ATTRIBUTE_POSITION, new Object[]{str2});
        try {
            ErrorListener errorListener = super.getTransformer().getErrorListener();
            if (errorListener != null && this.m_sourceLocator != null) {
                errorListener.warning(new TransformerException(strCreateMessage, this.m_sourceLocator));
            } else {
                System.out.println(strCreateMessage);
            }
        } catch (TransformerException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void endElement(String str) throws SAXException {
        endElement(null, null, str);
    }

    @Override
    public void namespaceAfterStartElement(String str, String str2) throws SAXException {
        if (this.m_elemContext.m_elementURI == null && getPrefixPart(this.m_elemContext.m_elementName) == null && "".equals(str)) {
            this.m_elemContext.m_elementURI = str2;
        }
        startPrefixMapping(str, str2, false);
    }

    protected boolean pushNamespace(String str, String str2) {
        try {
            if (this.m_prefixMap.pushNamespace(str, str2, this.m_elemContext.m_currentElemDepth)) {
                startPrefixMapping(str, str2);
                return true;
            }
            return false;
        } catch (SAXException e) {
            return false;
        }
    }

    @Override
    public boolean reset() {
        if (super.reset()) {
            return true;
        }
        return false;
    }

    private void resetToXMLStream() {
    }

    private String getXMLVersion() {
        String version = getVersion();
        if (version == null || version.equals(SerializerConstants.XMLVERSION10)) {
            return SerializerConstants.XMLVERSION10;
        }
        if (version.equals(SerializerConstants.XMLVERSION11)) {
            return SerializerConstants.XMLVERSION11;
        }
        String strCreateMessage = Utils.messages.createMessage(MsgKey.ER_XML_VERSION_NOT_SUPPORTED, new Object[]{version});
        try {
            ErrorListener errorListener = super.getTransformer().getErrorListener();
            if (errorListener != null && this.m_sourceLocator != null) {
                errorListener.warning(new TransformerException(strCreateMessage, this.m_sourceLocator));
            } else {
                System.out.println(strCreateMessage);
            }
        } catch (Exception e) {
        }
        return SerializerConstants.XMLVERSION10;
    }
}
