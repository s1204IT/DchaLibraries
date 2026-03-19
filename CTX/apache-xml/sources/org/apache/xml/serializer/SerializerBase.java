package org.apache.xml.serializer;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Set;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.Transformer;
import org.apache.xalan.templates.Constants;
import org.apache.xml.serializer.dom3.DOM3SerializerImpl;
import org.apache.xml.serializer.utils.MsgKey;
import org.apache.xml.serializer.utils.Utils;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public abstract class SerializerBase implements SerializationHandler, SerializerConstants {
    public static final String PKG_NAME;
    public static final String PKG_PATH;
    private HashMap m_OutputProps;
    private HashMap m_OutputPropsDefault;
    protected String m_doctypePublic;
    protected String m_doctypeSystem;
    protected String m_mediatype;
    protected NamespaceMappings m_prefixMap;
    protected SourceLocator m_sourceLocator;
    private String m_standalone;
    protected SerializerTrace m_tracer;
    private Transformer m_transformer;
    protected boolean m_needToCallStartDocument = true;
    protected boolean m_cdataTagOpen = false;
    protected AttributesImplSerializer m_attributes = new AttributesImplSerializer();
    protected boolean m_inEntityRef = false;
    protected boolean m_inExternalDTD = false;
    boolean m_needToOutputDocTypeDecl = true;
    protected boolean m_shouldNotWriteXMLHeader = false;
    protected boolean m_standaloneWasSpecified = false;
    protected boolean m_doIndent = false;
    protected int m_indentAmount = 0;
    protected String m_version = null;
    protected Writer m_writer = null;
    protected ElemContext m_elemContext = new ElemContext();
    protected char[] m_charsBuff = new char[60];
    protected char[] m_attrBuff = new char[30];
    protected String m_StringOfCDATASections = null;
    boolean m_docIsEmpty = true;
    protected Hashtable m_CdataElems = null;

    SerializerBase() {
    }

    static {
        String name = SerializerBase.class.getName();
        int iLastIndexOf = name.lastIndexOf(46);
        if (iLastIndexOf < 0) {
            PKG_NAME = "";
        } else {
            PKG_NAME = name.substring(0, iLastIndexOf);
        }
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < PKG_NAME.length(); i++) {
            char cCharAt = PKG_NAME.charAt(i);
            if (cCharAt == '.') {
                stringBuffer.append('/');
            } else {
                stringBuffer.append(cCharAt);
            }
        }
        PKG_PATH = stringBuffer.toString();
    }

    protected void fireEndElem(String str) throws SAXException {
        if (this.m_tracer != null) {
            flushMyWriter();
            this.m_tracer.fireGenerateEvent(4, str, (Attributes) null);
        }
    }

    protected void fireCharEvent(char[] cArr, int i, int i2) throws SAXException {
        if (this.m_tracer != null) {
            flushMyWriter();
            this.m_tracer.fireGenerateEvent(5, cArr, i, i2);
        }
    }

    @Override
    public void comment(String str) throws SAXException {
        this.m_docIsEmpty = false;
        int length = str.length();
        if (length > this.m_charsBuff.length) {
            this.m_charsBuff = new char[(length * 2) + 1];
        }
        str.getChars(0, length, this.m_charsBuff, 0);
        comment(this.m_charsBuff, 0, length);
    }

    protected String patchName(String str) {
        int iLastIndexOf = str.lastIndexOf(58);
        if (iLastIndexOf > 0) {
            int iIndexOf = str.indexOf(58);
            String strSubstring = str.substring(0, iIndexOf);
            String strSubstring2 = str.substring(iLastIndexOf + 1);
            String strLookupNamespace = this.m_prefixMap.lookupNamespace(strSubstring);
            if (strLookupNamespace != null && strLookupNamespace.length() == 0) {
                return strSubstring2;
            }
            if (iIndexOf != iLastIndexOf) {
                return strSubstring + ':' + strSubstring2;
            }
        }
        return str;
    }

    protected static String getLocalName(String str) {
        int iLastIndexOf = str.lastIndexOf(58);
        return iLastIndexOf > 0 ? str.substring(iLastIndexOf + 1) : str;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void addAttribute(String str, String str2, String str3, String str4, String str5, boolean z) throws SAXException {
        if (this.m_elemContext.m_startTagOpen) {
            addAttributeAlways(str, str2, str3, str4, str5, z);
        }
    }

    public boolean addAttributeAlways(String str, String str2, String str3, String str4, String str5, boolean z) {
        int index;
        if (str2 == null || str == null || str.length() == 0) {
            index = this.m_attributes.getIndex(str3);
        } else {
            index = this.m_attributes.getIndex(str, str2);
        }
        if (index >= 0) {
            this.m_attributes.setValue(index, str5);
            return false;
        }
        this.m_attributes.addAttribute(str, str2, str3, str4, str5);
        return true;
    }

    @Override
    public void addAttribute(String str, String str2) {
        if (this.m_elemContext.m_startTagOpen) {
            String strPatchName = patchName(str);
            addAttributeAlways(getNamespaceURI(strPatchName, false), getLocalName(strPatchName), strPatchName, "CDATA", str2, false);
        }
    }

    @Override
    public void addXSLAttribute(String str, String str2, String str3) {
        if (this.m_elemContext.m_startTagOpen) {
            String strPatchName = patchName(str);
            addAttributeAlways(str3, getLocalName(strPatchName), strPatchName, "CDATA", str2, true);
        }
    }

    @Override
    public void addAttributes(Attributes attributes) throws SAXException {
        int length = attributes.getLength();
        for (int i = 0; i < length; i++) {
            String uri = attributes.getURI(i);
            if (uri == null) {
                uri = "";
            }
            addAttributeAlways(uri, attributes.getLocalName(i), attributes.getQName(i), attributes.getType(i), attributes.getValue(i), false);
        }
    }

    @Override
    public ContentHandler asContentHandler() throws IOException {
        return this;
    }

    @Override
    public void endEntity(String str) throws SAXException {
        if (str.equals("[dtd]")) {
            this.m_inExternalDTD = false;
        }
        this.m_inEntityRef = false;
        if (this.m_tracer != null) {
            fireEndEntity(str);
        }
    }

    @Override
    public void close() {
    }

    protected void initCDATA() {
    }

    @Override
    public String getEncoding() {
        return getOutputProperty("encoding");
    }

    @Override
    public void setEncoding(String str) {
        setOutputProperty("encoding", str);
    }

    @Override
    public void setOmitXMLDeclaration(boolean z) {
        setOutputProperty("omit-xml-declaration", z ? "yes" : "no");
    }

    @Override
    public boolean getOmitXMLDeclaration() {
        return this.m_shouldNotWriteXMLHeader;
    }

    @Override
    public String getDoctypePublic() {
        return this.m_doctypePublic;
    }

    @Override
    public void setDoctypePublic(String str) {
        setOutputProperty(Constants.ATTRNAME_OUTPUT_DOCTYPE_PUBLIC, str);
    }

    @Override
    public String getDoctypeSystem() {
        return this.m_doctypeSystem;
    }

    @Override
    public void setDoctypeSystem(String str) {
        setOutputProperty(Constants.ATTRNAME_OUTPUT_DOCTYPE_SYSTEM, str);
    }

    @Override
    public void setDoctype(String str, String str2) {
        setOutputProperty(Constants.ATTRNAME_OUTPUT_DOCTYPE_SYSTEM, str);
        setOutputProperty(Constants.ATTRNAME_OUTPUT_DOCTYPE_PUBLIC, str2);
    }

    @Override
    public void setStandalone(String str) {
        setOutputProperty(Constants.ATTRNAME_OUTPUT_STANDALONE, str);
    }

    protected void setStandaloneInternal(String str) {
        if ("yes".equals(str)) {
            this.m_standalone = "yes";
        } else {
            this.m_standalone = "no";
        }
    }

    @Override
    public String getStandalone() {
        return this.m_standalone;
    }

    @Override
    public boolean getIndent() {
        return this.m_doIndent;
    }

    @Override
    public String getMediaType() {
        return this.m_mediatype;
    }

    @Override
    public String getVersion() {
        return this.m_version;
    }

    @Override
    public void setVersion(String str) {
        setOutputProperty("version", str);
    }

    @Override
    public void setMediaType(String str) {
        setOutputProperty(Constants.ATTRNAME_OUTPUT_MEDIATYPE, str);
    }

    @Override
    public int getIndentAmount() {
        return this.m_indentAmount;
    }

    @Override
    public void setIndentAmount(int i) {
        this.m_indentAmount = i;
    }

    @Override
    public void setIndent(boolean z) {
        setOutputProperty("indent", z ? "yes" : "no");
    }

    @Override
    public void namespaceAfterStartElement(String str, String str2) throws SAXException {
    }

    @Override
    public DOMSerializer asDOMSerializer() throws IOException {
        return this;
    }

    private static final boolean subPartMatch(String str, String str2) {
        return str == str2 || (str != null && str.equals(str2));
    }

    protected static final String getPrefixPart(String str) {
        int iIndexOf = str.indexOf(58);
        if (iIndexOf > 0) {
            return str.substring(0, iIndexOf);
        }
        return null;
    }

    @Override
    public NamespaceMappings getNamespaceMappings() {
        return this.m_prefixMap;
    }

    @Override
    public String getPrefix(String str) {
        return this.m_prefixMap.lookupPrefix(str);
    }

    @Override
    public String getNamespaceURI(String str, boolean z) {
        String strLookupNamespace = "";
        int iLastIndexOf = str.lastIndexOf(58);
        String strSubstring = iLastIndexOf > 0 ? str.substring(0, iLastIndexOf) : "";
        if ((!"".equals(strSubstring) || z) && this.m_prefixMap != null && (strLookupNamespace = this.m_prefixMap.lookupNamespace(strSubstring)) == null && !strSubstring.equals("xmlns")) {
            throw new RuntimeException(Utils.messages.createMessage(MsgKey.ER_NAMESPACE_PREFIX, new Object[]{str.substring(0, iLastIndexOf)}));
        }
        return strLookupNamespace;
    }

    @Override
    public String getNamespaceURIFromPrefix(String str) {
        if (this.m_prefixMap != null) {
            return this.m_prefixMap.lookupNamespace(str);
        }
        return null;
    }

    @Override
    public void entityReference(String str) throws SAXException {
        flushPending();
        startEntity(str);
        endEntity(str);
        if (this.m_tracer != null) {
            fireEntityReference(str);
        }
    }

    @Override
    public void setTransformer(Transformer transformer) {
        this.m_transformer = transformer;
        if ((this.m_transformer instanceof SerializerTrace) && ((SerializerTrace) this.m_transformer).hasTraceListeners()) {
            this.m_tracer = (SerializerTrace) this.m_transformer;
        } else {
            this.m_tracer = null;
        }
    }

    @Override
    public Transformer getTransformer() {
        return this.m_transformer;
    }

    @Override
    public void characters(Node node) throws SAXException {
        flushPending();
        String nodeValue = node.getNodeValue();
        if (nodeValue != null) {
            int length = nodeValue.length();
            if (length > this.m_charsBuff.length) {
                this.m_charsBuff = new char[(length * 2) + 1];
            }
            nodeValue.getChars(0, length, this.m_charsBuff, 0);
            characters(this.m_charsBuff, 0, length);
        }
    }

    @Override
    public void error(SAXParseException sAXParseException) throws SAXException {
    }

    @Override
    public void fatalError(SAXParseException sAXParseException) throws SAXException {
        this.m_elemContext.m_startTagOpen = false;
    }

    @Override
    public void warning(SAXParseException sAXParseException) throws SAXException {
    }

    protected void fireStartEntity(String str) throws SAXException {
        if (this.m_tracer != null) {
            flushMyWriter();
            this.m_tracer.fireGenerateEvent(9, str);
        }
    }

    private void flushMyWriter() {
        if (this.m_writer != null) {
            try {
                this.m_writer.flush();
            } catch (IOException e) {
            }
        }
    }

    protected void fireCDATAEvent(char[] cArr, int i, int i2) throws SAXException {
        if (this.m_tracer != null) {
            flushMyWriter();
            this.m_tracer.fireGenerateEvent(10, cArr, i, i2);
        }
    }

    protected void fireCommentEvent(char[] cArr, int i, int i2) throws SAXException {
        if (this.m_tracer != null) {
            flushMyWriter();
            this.m_tracer.fireGenerateEvent(8, new String(cArr, i, i2));
        }
    }

    public void fireEndEntity(String str) throws SAXException {
        if (this.m_tracer != null) {
            flushMyWriter();
        }
    }

    protected void fireStartDoc() throws SAXException {
        if (this.m_tracer != null) {
            flushMyWriter();
            this.m_tracer.fireGenerateEvent(1);
        }
    }

    protected void fireEndDoc() throws SAXException {
        if (this.m_tracer != null) {
            flushMyWriter();
            this.m_tracer.fireGenerateEvent(2);
        }
    }

    protected void fireStartElem(String str) throws SAXException {
        if (this.m_tracer != null) {
            flushMyWriter();
            this.m_tracer.fireGenerateEvent(3, str, this.m_attributes);
        }
    }

    protected void fireEscapingEvent(String str, String str2) throws SAXException {
        if (this.m_tracer != null) {
            flushMyWriter();
            this.m_tracer.fireGenerateEvent(7, str, str2);
        }
    }

    protected void fireEntityReference(String str) throws SAXException {
        if (this.m_tracer != null) {
            flushMyWriter();
            this.m_tracer.fireGenerateEvent(9, str, (Attributes) null);
        }
    }

    @Override
    public void startDocument() throws SAXException {
        startDocumentInternal();
        this.m_needToCallStartDocument = false;
    }

    protected void startDocumentInternal() throws SAXException {
        if (this.m_tracer != null) {
            fireStartDoc();
        }
    }

    @Override
    public void setSourceLocator(SourceLocator sourceLocator) {
        this.m_sourceLocator = sourceLocator;
    }

    @Override
    public void setNamespaceMappings(NamespaceMappings namespaceMappings) {
        this.m_prefixMap = namespaceMappings;
    }

    @Override
    public boolean reset() {
        resetSerializerBase();
        return true;
    }

    private void resetSerializerBase() {
        this.m_attributes.clear();
        this.m_CdataElems = null;
        this.m_cdataTagOpen = false;
        this.m_docIsEmpty = true;
        this.m_doctypePublic = null;
        this.m_doctypeSystem = null;
        this.m_doIndent = false;
        this.m_elemContext = new ElemContext();
        this.m_indentAmount = 0;
        this.m_inEntityRef = false;
        this.m_inExternalDTD = false;
        this.m_mediatype = null;
        this.m_needToCallStartDocument = true;
        this.m_needToOutputDocTypeDecl = false;
        if (this.m_OutputProps != null) {
            this.m_OutputProps.clear();
        }
        if (this.m_OutputPropsDefault != null) {
            this.m_OutputPropsDefault.clear();
        }
        if (this.m_prefixMap != null) {
            this.m_prefixMap.reset();
        }
        this.m_shouldNotWriteXMLHeader = false;
        this.m_sourceLocator = null;
        this.m_standalone = null;
        this.m_standaloneWasSpecified = false;
        this.m_StringOfCDATASections = null;
        this.m_tracer = null;
        this.m_transformer = null;
        this.m_version = null;
    }

    final boolean inTemporaryOutputState() {
        return getEncoding() == null;
    }

    @Override
    public void addAttribute(String str, String str2, String str3, String str4, String str5) throws SAXException {
        if (this.m_elemContext.m_startTagOpen) {
            addAttributeAlways(str, str2, str3, str4, str5, false);
        }
    }

    @Override
    public void notationDecl(String str, String str2, String str3) throws SAXException {
    }

    @Override
    public void unparsedEntityDecl(String str, String str2, String str3, String str4) throws SAXException {
    }

    @Override
    public void setDTDEntityExpansion(boolean z) {
    }

    void initCdataElems(String str) {
        if (str != null) {
            int length = str.length();
            StringBuffer stringBuffer = new StringBuffer();
            String str2 = null;
            boolean z = false;
            boolean z2 = false;
            for (int i = 0; i < length; i++) {
                char cCharAt = str.charAt(i);
                if (Character.isWhitespace(cCharAt)) {
                    if (z) {
                        stringBuffer.append(cCharAt);
                    } else if (stringBuffer.length() > 0) {
                        String string = stringBuffer.toString();
                        if (!z2) {
                            str2 = "";
                        }
                        addCDATAElement(str2, string);
                        stringBuffer.setLength(0);
                        z2 = false;
                    }
                } else if ('{' == cCharAt) {
                    z = true;
                } else if ('}' == cCharAt) {
                    String string2 = stringBuffer.toString();
                    stringBuffer.setLength(0);
                    str2 = string2;
                    z2 = true;
                    z = false;
                } else {
                    stringBuffer.append(cCharAt);
                }
            }
            if (stringBuffer.length() > 0) {
                String string3 = stringBuffer.toString();
                if (!z2) {
                    str2 = "";
                }
                addCDATAElement(str2, string3);
            }
        }
    }

    private void addCDATAElement(String str, String str2) {
        if (this.m_CdataElems == null) {
            this.m_CdataElems = new Hashtable();
        }
        Hashtable hashtable = (Hashtable) this.m_CdataElems.get(str2);
        if (hashtable == null) {
            hashtable = new Hashtable();
            this.m_CdataElems.put(str2, hashtable);
        }
        hashtable.put(str, str);
    }

    public boolean documentIsEmpty() {
        return this.m_docIsEmpty && this.m_elemContext.m_currentElemDepth == 0;
    }

    protected boolean isCdataSection() {
        if (this.m_StringOfCDATASections != null) {
            if (this.m_elemContext.m_elementLocalName == null) {
                this.m_elemContext.m_elementLocalName = getLocalName(this.m_elemContext.m_elementName);
            }
            if (this.m_elemContext.m_elementURI == null) {
                this.m_elemContext.m_elementURI = getElementURI();
            } else if (this.m_elemContext.m_elementURI.length() == 0) {
                if (this.m_elemContext.m_elementName == null) {
                    this.m_elemContext.m_elementName = this.m_elemContext.m_elementLocalName;
                } else if (this.m_elemContext.m_elementLocalName.length() < this.m_elemContext.m_elementName.length()) {
                    this.m_elemContext.m_elementURI = getElementURI();
                }
            }
            Hashtable hashtable = (Hashtable) this.m_CdataElems.get(this.m_elemContext.m_elementLocalName);
            if (hashtable != null && hashtable.get(this.m_elemContext.m_elementURI) != null) {
                return true;
            }
        }
        return false;
    }

    private String getElementURI() {
        String strLookupNamespace;
        String prefixPart = getPrefixPart(this.m_elemContext.m_elementName);
        if (prefixPart == null) {
            strLookupNamespace = this.m_prefixMap.lookupNamespace("");
        } else {
            strLookupNamespace = this.m_prefixMap.lookupNamespace(prefixPart);
        }
        if (strLookupNamespace == null) {
            return "";
        }
        return strLookupNamespace;
    }

    @Override
    public String getOutputProperty(String str) {
        String outputPropertyNonDefault = getOutputPropertyNonDefault(str);
        if (outputPropertyNonDefault == null) {
            return getOutputPropertyDefault(str);
        }
        return outputPropertyNonDefault;
    }

    public String getOutputPropertyNonDefault(String str) {
        return getProp(str, false);
    }

    @Override
    public Object asDOM3Serializer() throws IOException {
        return new DOM3SerializerImpl(this);
    }

    @Override
    public String getOutputPropertyDefault(String str) {
        return getProp(str, true);
    }

    @Override
    public void setOutputProperty(String str, String str2) {
        setProp(str, str2, false);
    }

    @Override
    public void setOutputPropertyDefault(String str, String str2) {
        setProp(str, str2, true);
    }

    Set getOutputPropDefaultKeys() {
        return this.m_OutputPropsDefault.keySet();
    }

    Set getOutputPropKeys() {
        return this.m_OutputProps.keySet();
    }

    private String getProp(String str, boolean z) {
        if (this.m_OutputProps == null) {
            this.m_OutputProps = new HashMap();
            this.m_OutputPropsDefault = new HashMap();
        }
        if (z) {
            return (String) this.m_OutputPropsDefault.get(str);
        }
        return (String) this.m_OutputProps.get(str);
    }

    void setProp(String str, String str2, boolean z) {
        if (this.m_OutputProps == null) {
            this.m_OutputProps = new HashMap();
            this.m_OutputPropsDefault = new HashMap();
        }
        if (z) {
            this.m_OutputPropsDefault.put(str, str2);
            return;
        }
        if (Constants.ATTRNAME_OUTPUT_CDATA_SECTION_ELEMENTS.equals(str) && str2 != null) {
            initCdataElems(str2);
            String str3 = (String) this.m_OutputProps.get(str);
            if (str3 == null) {
                str2 = str3 + ' ' + str2;
            }
            this.m_OutputProps.put(str, str2);
            return;
        }
        this.m_OutputProps.put(str, str2);
    }

    static char getFirstCharLocName(String str) {
        int iIndexOf = str.indexOf(125);
        if (iIndexOf < 0) {
            return str.charAt(0);
        }
        return str.charAt(iIndexOf + 1);
    }
}
