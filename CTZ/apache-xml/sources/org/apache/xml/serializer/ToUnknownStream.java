package org.apache.xml.serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;
import java.util.Vector;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.Transformer;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public final class ToUnknownStream extends SerializerBase {
    private static final String EMPTYSTRING = "";
    private String m_firstElementName;
    private String m_firstElementPrefix;
    private String m_firstElementURI;
    private boolean m_wrapped_handler_not_initialized = false;
    private String m_firstElementLocalName = null;
    private boolean m_firstTagNotEmitted = true;
    private Vector m_namespaceURI = null;
    private Vector m_namespacePrefix = null;
    private boolean m_needToCallStartDocument = false;
    private boolean m_setVersion_called = false;
    private boolean m_setDoctypeSystem_called = false;
    private boolean m_setDoctypePublic_called = false;
    private boolean m_setMediaType_called = false;
    private SerializationHandler m_handler = new ToXMLStream();

    @Override
    public ContentHandler asContentHandler() throws IOException {
        return this;
    }

    @Override
    public void close() {
        this.m_handler.close();
    }

    @Override
    public Properties getOutputFormat() {
        return this.m_handler.getOutputFormat();
    }

    @Override
    public OutputStream getOutputStream() {
        return this.m_handler.getOutputStream();
    }

    @Override
    public Writer getWriter() {
        return this.m_handler.getWriter();
    }

    @Override
    public boolean reset() {
        return this.m_handler.reset();
    }

    @Override
    public void serialize(Node node) throws IOException {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.serialize(node);
    }

    @Override
    public boolean setEscaping(boolean z) throws SAXException {
        return this.m_handler.setEscaping(z);
    }

    @Override
    public void setOutputFormat(Properties properties) {
        this.m_handler.setOutputFormat(properties);
    }

    @Override
    public void setOutputStream(OutputStream outputStream) {
        this.m_handler.setOutputStream(outputStream);
    }

    @Override
    public void setWriter(Writer writer) {
        this.m_handler.setWriter(writer);
    }

    @Override
    public void addAttribute(String str, String str2, String str3, String str4, String str5, boolean z) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.addAttribute(str, str2, str3, str4, str5, z);
    }

    @Override
    public void addAttribute(String str, String str2) {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.addAttribute(str, str2);
    }

    @Override
    public void addUniqueAttribute(String str, String str2, int i) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.addUniqueAttribute(str, str2, i);
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
    public void endElement(String str) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.endElement(str);
    }

    @Override
    public void startPrefixMapping(String str, String str2) throws SAXException {
        startPrefixMapping(str, str2, true);
    }

    @Override
    public void namespaceAfterStartElement(String str, String str2) throws SAXException {
        if (this.m_firstTagNotEmitted && this.m_firstElementURI == null && this.m_firstElementName != null && getPrefixPart(this.m_firstElementName) == null && "".equals(str)) {
            this.m_firstElementURI = str2;
        }
        startPrefixMapping(str, str2, false);
    }

    @Override
    public boolean startPrefixMapping(String str, String str2, boolean z) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            if (this.m_firstElementName != null && z) {
                flush();
                return this.m_handler.startPrefixMapping(str, str2, z);
            }
            if (this.m_namespacePrefix == null) {
                this.m_namespacePrefix = new Vector();
                this.m_namespaceURI = new Vector();
            }
            this.m_namespacePrefix.addElement(str);
            this.m_namespaceURI.addElement(str2);
            if (this.m_firstElementURI == null && str.equals(this.m_firstElementPrefix)) {
                this.m_firstElementURI = str2;
            }
            return false;
        }
        return this.m_handler.startPrefixMapping(str, str2, z);
    }

    @Override
    public void setVersion(String str) {
        this.m_handler.setVersion(str);
        this.m_setVersion_called = true;
    }

    @Override
    public void startDocument() throws SAXException {
        this.m_needToCallStartDocument = true;
    }

    @Override
    public void startElement(String str) throws SAXException {
        startElement(null, null, str, null);
    }

    @Override
    public void startElement(String str, String str2, String str3) throws SAXException {
        startElement(str, str2, str3, null);
    }

    @Override
    public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            if (this.m_firstElementName != null) {
                flush();
                this.m_handler.startElement(str, str2, str3, attributes);
                return;
            }
            this.m_wrapped_handler_not_initialized = true;
            this.m_firstElementName = str3;
            this.m_firstElementPrefix = getPrefixPartUnknown(str3);
            this.m_firstElementURI = str;
            this.m_firstElementLocalName = str2;
            if (this.m_tracer != null) {
                firePseudoElement(str3);
            }
            if (attributes != null) {
                super.addAttributes(attributes);
            }
            if (attributes != null) {
                flush();
                return;
            }
            return;
        }
        this.m_handler.startElement(str, str2, str3, attributes);
    }

    @Override
    public void comment(String str) throws SAXException {
        if (this.m_firstTagNotEmitted && this.m_firstElementName != null) {
            emitFirstTag();
        } else if (this.m_needToCallStartDocument) {
            this.m_handler.startDocument();
            this.m_needToCallStartDocument = false;
        }
        this.m_handler.comment(str);
    }

    @Override
    public String getDoctypePublic() {
        return this.m_handler.getDoctypePublic();
    }

    @Override
    public String getDoctypeSystem() {
        return this.m_handler.getDoctypeSystem();
    }

    @Override
    public String getEncoding() {
        return this.m_handler.getEncoding();
    }

    @Override
    public boolean getIndent() {
        return this.m_handler.getIndent();
    }

    @Override
    public int getIndentAmount() {
        return this.m_handler.getIndentAmount();
    }

    @Override
    public String getMediaType() {
        return this.m_handler.getMediaType();
    }

    @Override
    public boolean getOmitXMLDeclaration() {
        return this.m_handler.getOmitXMLDeclaration();
    }

    @Override
    public String getStandalone() {
        return this.m_handler.getStandalone();
    }

    @Override
    public String getVersion() {
        return this.m_handler.getVersion();
    }

    @Override
    public void setDoctype(String str, String str2) {
        this.m_handler.setDoctypePublic(str2);
        this.m_handler.setDoctypeSystem(str);
    }

    @Override
    public void setDoctypePublic(String str) {
        this.m_handler.setDoctypePublic(str);
        this.m_setDoctypePublic_called = true;
    }

    @Override
    public void setDoctypeSystem(String str) {
        this.m_handler.setDoctypeSystem(str);
        this.m_setDoctypeSystem_called = true;
    }

    @Override
    public void setEncoding(String str) {
        this.m_handler.setEncoding(str);
    }

    @Override
    public void setIndent(boolean z) {
        this.m_handler.setIndent(z);
    }

    @Override
    public void setIndentAmount(int i) {
        this.m_handler.setIndentAmount(i);
    }

    @Override
    public void setMediaType(String str) {
        this.m_handler.setMediaType(str);
        this.m_setMediaType_called = true;
    }

    @Override
    public void setOmitXMLDeclaration(boolean z) {
        this.m_handler.setOmitXMLDeclaration(z);
    }

    @Override
    public void setStandalone(String str) {
        this.m_handler.setStandalone(str);
    }

    @Override
    public void attributeDecl(String str, String str2, String str3, String str4, String str5) throws SAXException {
        this.m_handler.attributeDecl(str, str2, str3, str4, str5);
    }

    @Override
    public void elementDecl(String str, String str2) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            emitFirstTag();
        }
        this.m_handler.elementDecl(str, str2);
    }

    @Override
    public void externalEntityDecl(String str, String str2, String str3) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.externalEntityDecl(str, str2, str3);
    }

    @Override
    public void internalEntityDecl(String str, String str2) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.internalEntityDecl(str, str2);
    }

    @Override
    public void characters(char[] cArr, int i, int i2) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.characters(cArr, i, i2);
    }

    @Override
    public void endDocument() throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.endDocument();
    }

    @Override
    public void endElement(String str, String str2, String str3) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
            if (str == null && this.m_firstElementURI != null) {
                str = this.m_firstElementURI;
            }
            if (str2 == null && this.m_firstElementLocalName != null) {
                str2 = this.m_firstElementLocalName;
            }
        }
        this.m_handler.endElement(str, str2, str3);
    }

    @Override
    public void endPrefixMapping(String str) throws SAXException {
        this.m_handler.endPrefixMapping(str);
    }

    @Override
    public void ignorableWhitespace(char[] cArr, int i, int i2) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.ignorableWhitespace(cArr, i, i2);
    }

    @Override
    public void processingInstruction(String str, String str2) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.processingInstruction(str, str2);
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.m_handler.setDocumentLocator(locator);
    }

    @Override
    public void skippedEntity(String str) throws SAXException {
        this.m_handler.skippedEntity(str);
    }

    @Override
    public void comment(char[] cArr, int i, int i2) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.comment(cArr, i, i2);
    }

    @Override
    public void endCDATA() throws SAXException {
        this.m_handler.endCDATA();
    }

    @Override
    public void endDTD() throws SAXException {
        this.m_handler.endDTD();
    }

    @Override
    public void endEntity(String str) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            emitFirstTag();
        }
        this.m_handler.endEntity(str);
    }

    @Override
    public void startCDATA() throws SAXException {
        this.m_handler.startCDATA();
    }

    @Override
    public void startDTD(String str, String str2, String str3) throws SAXException {
        this.m_handler.startDTD(str, str2, str3);
    }

    @Override
    public void startEntity(String str) throws SAXException {
        this.m_handler.startEntity(str);
    }

    private void initStreamOutput() throws SAXException {
        if (isFirstElemHTML()) {
            SerializationHandler serializationHandler = this.m_handler;
            this.m_handler = (SerializationHandler) SerializerFactory.getSerializer(OutputPropertiesFactory.getDefaultMethodProperties("html"));
            Writer writer = serializationHandler.getWriter();
            if (writer != null) {
                this.m_handler.setWriter(writer);
            } else {
                OutputStream outputStream = serializationHandler.getOutputStream();
                if (outputStream != null) {
                    this.m_handler.setOutputStream(outputStream);
                }
            }
            this.m_handler.setVersion(serializationHandler.getVersion());
            this.m_handler.setDoctypeSystem(serializationHandler.getDoctypeSystem());
            this.m_handler.setDoctypePublic(serializationHandler.getDoctypePublic());
            this.m_handler.setMediaType(serializationHandler.getMediaType());
            this.m_handler.setTransformer(serializationHandler.getTransformer());
        }
        if (this.m_needToCallStartDocument) {
            this.m_handler.startDocument();
            this.m_needToCallStartDocument = false;
        }
        this.m_wrapped_handler_not_initialized = false;
    }

    private void emitFirstTag() throws SAXException {
        if (this.m_firstElementName != null) {
            if (this.m_wrapped_handler_not_initialized) {
                initStreamOutput();
                this.m_wrapped_handler_not_initialized = false;
            }
            this.m_handler.startElement(this.m_firstElementURI, null, this.m_firstElementName, this.m_attributes);
            this.m_attributes = null;
            if (this.m_namespacePrefix != null) {
                int size = this.m_namespacePrefix.size();
                for (int i = 0; i < size; i++) {
                    this.m_handler.startPrefixMapping((String) this.m_namespacePrefix.elementAt(i), (String) this.m_namespaceURI.elementAt(i), false);
                }
                this.m_namespacePrefix = null;
                this.m_namespaceURI = null;
            }
            this.m_firstTagNotEmitted = false;
        }
    }

    private String getLocalNameUnknown(String str) {
        int iLastIndexOf = str.lastIndexOf(58);
        if (iLastIndexOf >= 0) {
            str = str.substring(iLastIndexOf + 1);
        }
        int iLastIndexOf2 = str.lastIndexOf(64);
        if (iLastIndexOf2 >= 0) {
            return str.substring(iLastIndexOf2 + 1);
        }
        return str;
    }

    private String getPrefixPartUnknown(String str) {
        int iIndexOf = str.indexOf(58);
        return iIndexOf > 0 ? str.substring(0, iIndexOf) : "";
    }

    private boolean isFirstElemHTML() {
        boolean zEqualsIgnoreCase = getLocalNameUnknown(this.m_firstElementName).equalsIgnoreCase("html");
        if (zEqualsIgnoreCase && this.m_firstElementURI != null && !"".equals(this.m_firstElementURI)) {
            zEqualsIgnoreCase = false;
        }
        if (zEqualsIgnoreCase && this.m_namespacePrefix != null) {
            int size = this.m_namespacePrefix.size();
            for (int i = 0; i < size; i++) {
                String str = (String) this.m_namespacePrefix.elementAt(i);
                String str2 = (String) this.m_namespaceURI.elementAt(i);
                if (this.m_firstElementPrefix != null && this.m_firstElementPrefix.equals(str) && !"".equals(str2)) {
                    return false;
                }
            }
            return zEqualsIgnoreCase;
        }
        return zEqualsIgnoreCase;
    }

    @Override
    public DOMSerializer asDOMSerializer() throws IOException {
        return this.m_handler.asDOMSerializer();
    }

    @Override
    public void setCdataSectionElements(Vector vector) {
        this.m_handler.setCdataSectionElements(vector);
    }

    @Override
    public void addAttributes(Attributes attributes) throws SAXException {
        this.m_handler.addAttributes(attributes);
    }

    @Override
    public NamespaceMappings getNamespaceMappings() {
        if (this.m_handler != null) {
            return this.m_handler.getNamespaceMappings();
        }
        return null;
    }

    @Override
    public void flushPending() throws SAXException {
        flush();
        this.m_handler.flushPending();
    }

    private void flush() {
        try {
            if (this.m_firstTagNotEmitted) {
                emitFirstTag();
            }
            if (this.m_needToCallStartDocument) {
                this.m_handler.startDocument();
                this.m_needToCallStartDocument = false;
            }
        } catch (SAXException e) {
            throw new RuntimeException(e.toString());
        }
    }

    @Override
    public String getPrefix(String str) {
        return this.m_handler.getPrefix(str);
    }

    @Override
    public void entityReference(String str) throws SAXException {
        this.m_handler.entityReference(str);
    }

    @Override
    public String getNamespaceURI(String str, boolean z) {
        return this.m_handler.getNamespaceURI(str, z);
    }

    @Override
    public String getNamespaceURIFromPrefix(String str) {
        return this.m_handler.getNamespaceURIFromPrefix(str);
    }

    @Override
    public void setTransformer(Transformer transformer) {
        this.m_handler.setTransformer(transformer);
        if (transformer instanceof SerializerTrace) {
            SerializerTrace serializerTrace = (SerializerTrace) transformer;
            if (serializerTrace.hasTraceListeners()) {
                this.m_tracer = serializerTrace;
                return;
            }
        }
        this.m_tracer = null;
    }

    @Override
    public Transformer getTransformer() {
        return this.m_handler.getTransformer();
    }

    @Override
    public void setContentHandler(ContentHandler contentHandler) {
        this.m_handler.setContentHandler(contentHandler);
    }

    @Override
    public void setSourceLocator(SourceLocator sourceLocator) {
        this.m_handler.setSourceLocator(sourceLocator);
    }

    protected void firePseudoElement(String str) {
        if (this.m_tracer != null) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append('<');
            stringBuffer.append(str);
            char[] charArray = stringBuffer.toString().toCharArray();
            this.m_tracer.fireGenerateEvent(11, charArray, 0, charArray.length);
        }
    }

    @Override
    public Object asDOM3Serializer() throws IOException {
        return this.m_handler.asDOM3Serializer();
    }
}
