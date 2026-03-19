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
    public boolean setEscaping(boolean escape) throws SAXException {
        return this.m_handler.setEscaping(escape);
    }

    @Override
    public void setOutputFormat(Properties format) {
        this.m_handler.setOutputFormat(format);
    }

    @Override
    public void setOutputStream(OutputStream output) {
        this.m_handler.setOutputStream(output);
    }

    @Override
    public void setWriter(Writer writer) {
        this.m_handler.setWriter(writer);
    }

    @Override
    public void addAttribute(String uri, String localName, String rawName, String type, String value, boolean XSLAttribute) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.addAttribute(uri, localName, rawName, type, value, XSLAttribute);
    }

    @Override
    public void addAttribute(String rawName, String value) {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.addAttribute(rawName, value);
    }

    @Override
    public void addUniqueAttribute(String rawName, String value, int flags) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.addUniqueAttribute(rawName, value, flags);
    }

    @Override
    public void characters(String chars) throws SAXException {
        int length = chars.length();
        if (length > this.m_charsBuff.length) {
            this.m_charsBuff = new char[(length * 2) + 1];
        }
        chars.getChars(0, length, this.m_charsBuff, 0);
        characters(this.m_charsBuff, 0, length);
    }

    @Override
    public void endElement(String elementName) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.endElement(elementName);
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        startPrefixMapping(prefix, uri, true);
    }

    @Override
    public void namespaceAfterStartElement(String prefix, String uri) throws SAXException {
        if (this.m_firstTagNotEmitted && this.m_firstElementURI == null && this.m_firstElementName != null) {
            String prefix1 = getPrefixPart(this.m_firstElementName);
            if (prefix1 == null && "".equals(prefix)) {
                this.m_firstElementURI = uri;
            }
        }
        startPrefixMapping(prefix, uri, false);
    }

    @Override
    public boolean startPrefixMapping(String prefix, String uri, boolean shouldFlush) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            if (this.m_firstElementName != null && shouldFlush) {
                flush();
                boolean pushed = this.m_handler.startPrefixMapping(prefix, uri, shouldFlush);
                return pushed;
            }
            if (this.m_namespacePrefix == null) {
                this.m_namespacePrefix = new Vector();
                this.m_namespaceURI = new Vector();
            }
            this.m_namespacePrefix.addElement(prefix);
            this.m_namespaceURI.addElement(uri);
            if (this.m_firstElementURI != null || !prefix.equals(this.m_firstElementPrefix)) {
                return false;
            }
            this.m_firstElementURI = uri;
            return false;
        }
        boolean pushed2 = this.m_handler.startPrefixMapping(prefix, uri, shouldFlush);
        return pushed2;
    }

    @Override
    public void setVersion(String version) {
        this.m_handler.setVersion(version);
        this.m_setVersion_called = true;
    }

    @Override
    public void startDocument() throws SAXException {
        this.m_needToCallStartDocument = true;
    }

    @Override
    public void startElement(String qName) throws SAXException {
        startElement(null, null, qName, null);
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName) throws SAXException {
        startElement(namespaceURI, localName, qName, null);
    }

    @Override
    public void startElement(String namespaceURI, String localName, String elementName, Attributes atts) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            if (this.m_firstElementName != null) {
                flush();
                this.m_handler.startElement(namespaceURI, localName, elementName, atts);
                return;
            }
            this.m_wrapped_handler_not_initialized = true;
            this.m_firstElementName = elementName;
            this.m_firstElementPrefix = getPrefixPartUnknown(elementName);
            this.m_firstElementURI = namespaceURI;
            this.m_firstElementLocalName = localName;
            if (this.m_tracer != null) {
                firePseudoElement(elementName);
            }
            if (atts != null) {
                super.addAttributes(atts);
            }
            if (atts == null) {
                return;
            }
            flush();
            return;
        }
        this.m_handler.startElement(namespaceURI, localName, elementName, atts);
    }

    @Override
    public void comment(String comment) throws SAXException {
        if (this.m_firstTagNotEmitted && this.m_firstElementName != null) {
            emitFirstTag();
        } else if (this.m_needToCallStartDocument) {
            this.m_handler.startDocument();
            this.m_needToCallStartDocument = false;
        }
        this.m_handler.comment(comment);
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
    public void setDoctype(String system, String pub) {
        this.m_handler.setDoctypePublic(pub);
        this.m_handler.setDoctypeSystem(system);
    }

    @Override
    public void setDoctypePublic(String doctype) {
        this.m_handler.setDoctypePublic(doctype);
        this.m_setDoctypePublic_called = true;
    }

    @Override
    public void setDoctypeSystem(String doctype) {
        this.m_handler.setDoctypeSystem(doctype);
        this.m_setDoctypeSystem_called = true;
    }

    @Override
    public void setEncoding(String encoding) {
        this.m_handler.setEncoding(encoding);
    }

    @Override
    public void setIndent(boolean indent) {
        this.m_handler.setIndent(indent);
    }

    @Override
    public void setIndentAmount(int value) {
        this.m_handler.setIndentAmount(value);
    }

    @Override
    public void setMediaType(String mediaType) {
        this.m_handler.setMediaType(mediaType);
        this.m_setMediaType_called = true;
    }

    @Override
    public void setOmitXMLDeclaration(boolean b) {
        this.m_handler.setOmitXMLDeclaration(b);
    }

    @Override
    public void setStandalone(String standalone) {
        this.m_handler.setStandalone(standalone);
    }

    @Override
    public void attributeDecl(String arg0, String arg1, String arg2, String arg3, String arg4) throws SAXException {
        this.m_handler.attributeDecl(arg0, arg1, arg2, arg3, arg4);
    }

    @Override
    public void elementDecl(String arg0, String arg1) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            emitFirstTag();
        }
        this.m_handler.elementDecl(arg0, arg1);
    }

    @Override
    public void externalEntityDecl(String name, String publicId, String systemId) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.externalEntityDecl(name, publicId, systemId);
    }

    @Override
    public void internalEntityDecl(String arg0, String arg1) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.internalEntityDecl(arg0, arg1);
    }

    @Override
    public void characters(char[] characters, int offset, int length) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.characters(characters, offset, length);
    }

    @Override
    public void endDocument() throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.endDocument();
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
            if (namespaceURI == null && this.m_firstElementURI != null) {
                namespaceURI = this.m_firstElementURI;
            }
            if (localName == null && this.m_firstElementLocalName != null) {
                localName = this.m_firstElementLocalName;
            }
        }
        this.m_handler.endElement(namespaceURI, localName, qName);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        this.m_handler.endPrefixMapping(prefix);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.ignorableWhitespace(ch, start, length);
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.processingInstruction(target, data);
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.m_handler.setDocumentLocator(locator);
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        this.m_handler.skippedEntity(name);
    }

    @Override
    public void comment(char[] ch, int start, int length) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            flush();
        }
        this.m_handler.comment(ch, start, length);
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
    public void endEntity(String name) throws SAXException {
        if (this.m_firstTagNotEmitted) {
            emitFirstTag();
        }
        this.m_handler.endEntity(name);
    }

    @Override
    public void startCDATA() throws SAXException {
        this.m_handler.startCDATA();
    }

    @Override
    public void startDTD(String name, String publicId, String systemId) throws SAXException {
        this.m_handler.startDTD(name, publicId, systemId);
    }

    @Override
    public void startEntity(String name) throws SAXException {
        this.m_handler.startEntity(name);
    }

    private void initStreamOutput() throws SAXException {
        boolean firstElementIsHTML = isFirstElemHTML();
        if (firstElementIsHTML) {
            SerializationHandler oldHandler = this.m_handler;
            Properties htmlProperties = OutputPropertiesFactory.getDefaultMethodProperties("html");
            Serializer serializer = SerializerFactory.getSerializer(htmlProperties);
            this.m_handler = (SerializationHandler) serializer;
            Writer writer = oldHandler.getWriter();
            if (writer != null) {
                this.m_handler.setWriter(writer);
            } else {
                OutputStream os = oldHandler.getOutputStream();
                if (os != null) {
                    this.m_handler.setOutputStream(os);
                }
            }
            this.m_handler.setVersion(oldHandler.getVersion());
            this.m_handler.setDoctypeSystem(oldHandler.getDoctypeSystem());
            this.m_handler.setDoctypePublic(oldHandler.getDoctypePublic());
            this.m_handler.setMediaType(oldHandler.getMediaType());
            this.m_handler.setTransformer(oldHandler.getTransformer());
        }
        if (this.m_needToCallStartDocument) {
            this.m_handler.startDocument();
            this.m_needToCallStartDocument = false;
        }
        this.m_wrapped_handler_not_initialized = false;
    }

    private void emitFirstTag() throws SAXException {
        if (this.m_firstElementName == null) {
            return;
        }
        if (this.m_wrapped_handler_not_initialized) {
            initStreamOutput();
            this.m_wrapped_handler_not_initialized = false;
        }
        this.m_handler.startElement(this.m_firstElementURI, null, this.m_firstElementName, this.m_attributes);
        this.m_attributes = null;
        if (this.m_namespacePrefix != null) {
            int n = this.m_namespacePrefix.size();
            for (int i = 0; i < n; i++) {
                String prefix = (String) this.m_namespacePrefix.elementAt(i);
                String uri = (String) this.m_namespaceURI.elementAt(i);
                this.m_handler.startPrefixMapping(prefix, uri, false);
            }
            this.m_namespacePrefix = null;
            this.m_namespaceURI = null;
        }
        this.m_firstTagNotEmitted = false;
    }

    private String getLocalNameUnknown(String value) {
        int idx = value.lastIndexOf(58);
        if (idx >= 0) {
            value = value.substring(idx + 1);
        }
        int idx2 = value.lastIndexOf(64);
        if (idx2 >= 0) {
            return value.substring(idx2 + 1);
        }
        return value;
    }

    private String getPrefixPartUnknown(String qname) {
        int index = qname.indexOf(58);
        return index > 0 ? qname.substring(0, index) : "";
    }

    private boolean isFirstElemHTML() {
        boolean isHTML = getLocalNameUnknown(this.m_firstElementName).equalsIgnoreCase("html");
        if (isHTML && this.m_firstElementURI != null && !"".equals(this.m_firstElementURI)) {
            isHTML = false;
        }
        if (isHTML && this.m_namespacePrefix != null) {
            int max = this.m_namespacePrefix.size();
            for (int i = 0; i < max; i++) {
                String prefix = (String) this.m_namespacePrefix.elementAt(i);
                String uri = (String) this.m_namespaceURI.elementAt(i);
                if (this.m_firstElementPrefix != null && this.m_firstElementPrefix.equals(prefix) && !"".equals(uri)) {
                    return false;
                }
            }
            return isHTML;
        }
        return isHTML;
    }

    @Override
    public DOMSerializer asDOMSerializer() throws IOException {
        return this.m_handler.asDOMSerializer();
    }

    @Override
    public void setCdataSectionElements(Vector URI_and_localNames) {
        this.m_handler.setCdataSectionElements(URI_and_localNames);
    }

    @Override
    public void addAttributes(Attributes atts) throws SAXException {
        this.m_handler.addAttributes(atts);
    }

    @Override
    public NamespaceMappings getNamespaceMappings() {
        if (this.m_handler == null) {
            return null;
        }
        NamespaceMappings mappings = this.m_handler.getNamespaceMappings();
        return mappings;
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
            if (!this.m_needToCallStartDocument) {
                return;
            }
            this.m_handler.startDocument();
            this.m_needToCallStartDocument = false;
        } catch (SAXException e) {
            throw new RuntimeException(e.toString());
        }
    }

    @Override
    public String getPrefix(String namespaceURI) {
        return this.m_handler.getPrefix(namespaceURI);
    }

    @Override
    public void entityReference(String entityName) throws SAXException {
        this.m_handler.entityReference(entityName);
    }

    @Override
    public String getNamespaceURI(String qname, boolean isElement) {
        return this.m_handler.getNamespaceURI(qname, isElement);
    }

    @Override
    public String getNamespaceURIFromPrefix(String prefix) {
        return this.m_handler.getNamespaceURIFromPrefix(prefix);
    }

    @Override
    public void setTransformer(Transformer transformer) {
        this.m_handler.setTransformer(transformer);
        if ((transformer instanceof SerializerTrace) && ((SerializerTrace) transformer).hasTraceListeners()) {
            this.m_tracer = (SerializerTrace) transformer;
        } else {
            this.m_tracer = null;
        }
    }

    @Override
    public Transformer getTransformer() {
        return this.m_handler.getTransformer();
    }

    @Override
    public void setContentHandler(ContentHandler ch) {
        this.m_handler.setContentHandler(ch);
    }

    @Override
    public void setSourceLocator(SourceLocator locator) {
        this.m_handler.setSourceLocator(locator);
    }

    protected void firePseudoElement(String elementName) {
        if (this.m_tracer == null) {
            return;
        }
        StringBuffer sb = new StringBuffer();
        sb.append('<');
        sb.append(elementName);
        char[] ch = sb.toString().toCharArray();
        this.m_tracer.fireGenerateEvent(11, ch, 0, ch.length);
    }

    @Override
    public Object asDOM3Serializer() throws IOException {
        return this.m_handler.asDOM3Serializer();
    }
}
