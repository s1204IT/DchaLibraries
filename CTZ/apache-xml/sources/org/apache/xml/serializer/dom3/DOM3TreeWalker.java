package org.apache.xml.serializer.dom3;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import org.apache.xalan.templates.Constants;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.SerializationHandler;
import org.apache.xml.serializer.SerializerConstants;
import org.apache.xml.serializer.utils.MsgKey;
import org.apache.xml.serializer.utils.Utils;
import org.apache.xml.serializer.utils.XML11Char;
import org.apache.xml.serializer.utils.XMLChar;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.ls.LSSerializerFilter;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.LocatorImpl;

final class DOM3TreeWalker {
    private static final int CANONICAL = 1;
    private static final int CDATA = 2;
    private static final int CHARNORMALIZE = 4;
    private static final int COMMENTS = 8;
    private static final int DISCARDDEFAULT = 32768;
    private static final int DTNORMALIZE = 16;
    private static final int ELEM_CONTENT_WHITESPACE = 32;
    private static final int ENTITIES = 64;
    private static final int IGNORE_CHAR_DENORMALIZE = 131072;
    private static final int INFOSET = 128;
    private static final int NAMESPACEDECLS = 512;
    private static final int NAMESPACES = 256;
    private static final int NORMALIZECHARS = 1024;
    private static final int PRETTY_PRINT = 65536;
    private static final int SCHEMAVALIDATE = 8192;
    private static final int SPLITCDATA = 2048;
    private static final int VALIDATE = 4096;
    private static final int WELLFORMED = 16384;
    private static final int XMLDECL = 262144;
    private static final String XMLNS_PREFIX = "xmlns";
    private static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";
    private static final String XML_PREFIX = "xml";
    private static final String XML_URI = "http://www.w3.org/XML/1998/namespace";
    private static final Hashtable s_propKeys = new Hashtable();
    private Properties fDOMConfigProperties;
    private DOMErrorHandler fErrorHandler;
    private LSSerializerFilter fFilter;
    private LexicalHandler fLexicalHandler;
    private String fNewLine;
    private SerializationHandler fSerializer;
    private int fWhatToShowFilter;
    private LocatorImpl fLocator = new LocatorImpl();
    private boolean fInEntityRef = false;
    private String fXMLVersion = null;
    private boolean fIsXMLVersion11 = false;
    private boolean fIsLevel3DOM = false;
    private int fFeatures = 0;
    boolean fNextIsRaw = false;
    private int fElementDepth = 0;
    protected NamespaceSupport fNSBinder = new NamespaceSupport();
    protected NamespaceSupport fLocalNSBinder = new NamespaceSupport();

    DOM3TreeWalker(SerializationHandler serializationHandler, DOMErrorHandler dOMErrorHandler, LSSerializerFilter lSSerializerFilter, String str) {
        this.fSerializer = null;
        this.fErrorHandler = null;
        this.fFilter = null;
        this.fLexicalHandler = null;
        this.fNewLine = null;
        this.fDOMConfigProperties = null;
        this.fSerializer = serializationHandler;
        this.fErrorHandler = dOMErrorHandler;
        this.fFilter = lSSerializerFilter;
        this.fLexicalHandler = null;
        this.fNewLine = str;
        this.fDOMConfigProperties = this.fSerializer.getOutputFormat();
        this.fSerializer.setDocumentLocator(this.fLocator);
        initProperties(this.fDOMConfigProperties);
        try {
            this.fLocator.setSystemId(System.getProperty("user.dir") + File.separator + "dummy.xsl");
        } catch (SecurityException e) {
        }
    }

    public void traverse(Node node) throws SAXException {
        this.fSerializer.startDocument();
        if (node.getNodeType() != 9) {
            Document ownerDocument = node.getOwnerDocument();
            if (ownerDocument != null && ownerDocument.getImplementation().hasFeature("Core", "3.0")) {
                this.fIsLevel3DOM = true;
            }
        } else if (((Document) node).getImplementation().hasFeature("Core", "3.0")) {
            this.fIsLevel3DOM = true;
        }
        if (this.fSerializer instanceof LexicalHandler) {
            this.fLexicalHandler = this.fSerializer;
        }
        if (this.fFilter != null) {
            this.fWhatToShowFilter = this.fFilter.getWhatToShow();
        }
        Node parentNode = node;
        while (parentNode != null) {
            startNode(parentNode);
            Node firstChild = parentNode.getFirstChild();
            while (firstChild == null) {
                endNode(parentNode);
                if (node.equals(parentNode)) {
                    break;
                }
                firstChild = parentNode.getNextSibling();
                if (firstChild == null && ((parentNode = parentNode.getParentNode()) == null || node.equals(parentNode))) {
                    if (parentNode != null) {
                        endNode(parentNode);
                    }
                    parentNode = null;
                }
            }
            parentNode = firstChild;
        }
        this.fSerializer.endDocument();
    }

    public void traverse(Node node, Node node2) throws SAXException {
        this.fSerializer.startDocument();
        if (node.getNodeType() != 9) {
            Document ownerDocument = node.getOwnerDocument();
            if (ownerDocument != null && ownerDocument.getImplementation().hasFeature("Core", "3.0")) {
                this.fIsLevel3DOM = true;
            }
        } else if (((Document) node).getImplementation().hasFeature("Core", "3.0")) {
            this.fIsLevel3DOM = true;
        }
        if (this.fSerializer instanceof LexicalHandler) {
            this.fLexicalHandler = this.fSerializer;
        }
        if (this.fFilter != null) {
            this.fWhatToShowFilter = this.fFilter.getWhatToShow();
        }
        while (node != null) {
            startNode(node);
            Node firstChild = node.getFirstChild();
            while (firstChild == null) {
                endNode(node);
                if (node2 != null && node2.equals(node)) {
                    break;
                }
                firstChild = node.getNextSibling();
                if (firstChild == null && ((node = node.getParentNode()) == null || (node2 != null && node2.equals(node)))) {
                    node = null;
                    break;
                }
            }
            node = firstChild;
        }
        this.fSerializer.endDocument();
    }

    private final void dispatachChars(Node node) throws SAXException {
        if (this.fSerializer != null) {
            this.fSerializer.characters(node);
        } else {
            String data = ((Text) node).getData();
            this.fSerializer.characters(data.toCharArray(), 0, data.length());
        }
    }

    protected void startNode(Node node) throws SAXException {
        if (node instanceof Locator) {
            Locator locator = (Locator) node;
            this.fLocator.setColumnNumber(locator.getColumnNumber());
            this.fLocator.setLineNumber(locator.getLineNumber());
            this.fLocator.setPublicId(locator.getPublicId());
            this.fLocator.setSystemId(locator.getSystemId());
        } else {
            this.fLocator.setColumnNumber(0);
            this.fLocator.setLineNumber(0);
        }
        switch (node.getNodeType()) {
            case 1:
                serializeElement((Element) node, true);
                break;
            case 3:
                serializeText((Text) node);
                break;
            case 4:
                serializeCDATASection((CDATASection) node);
                break;
            case 5:
                serializeEntityReference((EntityReference) node, true);
                break;
            case 7:
                serializePI((ProcessingInstruction) node);
                break;
            case 8:
                serializeComment((Comment) node);
                break;
            case 10:
                serializeDocType((DocumentType) node, true);
                break;
        }
    }

    protected void endNode(Node node) throws SAXException {
        switch (node.getNodeType()) {
            case 1:
                serializeElement((Element) node, false);
                break;
            case 5:
                serializeEntityReference((EntityReference) node, false);
                break;
            case 10:
                serializeDocType((DocumentType) node, false);
                break;
        }
    }

    protected boolean applyFilter(Node node, int i) {
        if (this.fFilter != null && (i & this.fWhatToShowFilter) != 0) {
            switch (this.fFilter.acceptNode(node)) {
                case 2:
                case 3:
                    return false;
                default:
                    return true;
            }
        }
        return true;
    }

    protected void serializeDocType(DocumentType documentType, boolean z) throws SAXException {
        String nodeName = documentType.getNodeName();
        String publicId = documentType.getPublicId();
        String systemId = documentType.getSystemId();
        String internalSubset = documentType.getInternalSubset();
        if (internalSubset != null && !"".equals(internalSubset)) {
            if (z) {
                try {
                    Writer writer = this.fSerializer.getWriter();
                    StringBuffer stringBuffer = new StringBuffer();
                    stringBuffer.append("<!DOCTYPE ");
                    stringBuffer.append(nodeName);
                    if (publicId != null) {
                        stringBuffer.append(" PUBLIC \"");
                        stringBuffer.append(publicId);
                        stringBuffer.append('\"');
                    }
                    if (systemId != null) {
                        if (publicId == null) {
                            stringBuffer.append(" SYSTEM \"");
                        } else {
                            stringBuffer.append(" \"");
                        }
                        stringBuffer.append(systemId);
                        stringBuffer.append('\"');
                    }
                    stringBuffer.append(" [ ");
                    stringBuffer.append(this.fNewLine);
                    stringBuffer.append(internalSubset);
                    stringBuffer.append("]>");
                    stringBuffer.append(new String(this.fNewLine));
                    writer.write(stringBuffer.toString());
                    writer.flush();
                    return;
                } catch (IOException e) {
                    throw new SAXException(Utils.messages.createMessage(MsgKey.ER_WRITING_INTERNAL_SUBSET, null), e);
                }
            }
            return;
        }
        if (z) {
            if (this.fLexicalHandler != null) {
                this.fLexicalHandler.startDTD(nodeName, publicId, systemId);
            }
        } else if (this.fLexicalHandler != null) {
            this.fLexicalHandler.endDTD();
        }
    }

    protected void serializeComment(Comment comment) throws SAXException {
        if ((this.fFeatures & 8) != 0) {
            String data = comment.getData();
            if ((this.fFeatures & 16384) != 0) {
                isCommentWellFormed(data);
            }
            if (this.fLexicalHandler == null || !applyFilter(comment, 128)) {
                return;
            }
            this.fLexicalHandler.comment(data.toCharArray(), 0, data.length());
        }
    }

    protected void serializeElement(Element element, boolean z) throws SAXException {
        if (z) {
            this.fElementDepth++;
            if ((this.fFeatures & 16384) != 0) {
                isElementWellFormed(element);
            }
            if (!applyFilter(element, 1)) {
                return;
            }
            if ((this.fFeatures & 256) != 0) {
                this.fNSBinder.pushContext();
                this.fLocalNSBinder.reset();
                recordLocalNSDecl(element);
                fixupElementNS(element);
            }
            this.fSerializer.startElement(element.getNamespaceURI(), element.getLocalName(), element.getNodeName());
            serializeAttList(element);
            return;
        }
        this.fElementDepth--;
        if (!applyFilter(element, 1)) {
            return;
        }
        this.fSerializer.endElement(element.getNamespaceURI(), element.getLocalName(), element.getNodeName());
        if ((this.fFeatures & 256) != 0) {
            this.fNSBinder.popContext();
        }
    }

    protected void serializeAttList(Element element) throws SAXException {
        String typeName;
        boolean z;
        boolean z2;
        String str;
        boolean z3;
        String strSubstring;
        NamedNodeMap attributes = element.getAttributes();
        int length = attributes.getLength();
        for (int i = 0; i < length; i++) {
            Node nodeItem = attributes.item(i);
            String localName = nodeItem.getLocalName();
            String nodeName = nodeItem.getNodeName();
            String prefix = nodeItem.getPrefix() == null ? "" : nodeItem.getPrefix();
            String nodeValue = nodeItem.getNodeValue();
            if (this.fIsLevel3DOM) {
                typeName = ((Attr) nodeItem).getSchemaTypeInfo().getTypeName();
            } else {
                typeName = null;
            }
            if (typeName == null) {
                typeName = "CDATA";
            }
            String str2 = typeName;
            String namespaceURI = nodeItem.getNamespaceURI();
            if (namespaceURI != null && namespaceURI.length() == 0) {
                nodeName = nodeItem.getLocalName();
                namespaceURI = null;
            }
            boolean specified = ((Attr) nodeItem).getSpecified();
            boolean z4 = nodeName.equals("xmlns") || nodeName.startsWith(Constants.ATTRNAME_XMLNS);
            if ((this.fFeatures & 16384) != 0) {
                isAttributeWellFormed(nodeItem);
            }
            if ((this.fFeatures & 256) != 0 && !z4) {
                if (namespaceURI != null) {
                    if (prefix == null) {
                        prefix = "";
                    }
                    String prefix2 = this.fNSBinder.getPrefix(namespaceURI);
                    String uri = this.fNSBinder.getURI(prefix);
                    if ("".equals(prefix) || "".equals(prefix2) || !prefix.equals(prefix2)) {
                        if (prefix2 == null || "".equals(prefix2)) {
                            if (prefix != null && !"".equals(prefix) && uri == null) {
                                if ((this.fFeatures & 512) != 0) {
                                    this.fSerializer.addAttribute("http://www.w3.org/2000/xmlns/", prefix, Constants.ATTRNAME_XMLNS + prefix, "CDATA", namespaceURI);
                                    this.fNSBinder.declarePrefix(prefix, namespaceURI);
                                    this.fLocalNSBinder.declarePrefix(prefix, namespaceURI);
                                }
                            } else {
                                String str3 = "NS1";
                                int i2 = 2;
                                while (this.fLocalNSBinder.getURI(str3) != null) {
                                    str3 = "NS" + i2;
                                    i2++;
                                }
                                String str4 = str3 + ":" + localName;
                                if ((this.fFeatures & 512) != 0) {
                                    this.fSerializer.addAttribute("http://www.w3.org/2000/xmlns/", str3, Constants.ATTRNAME_XMLNS + str3, "CDATA", namespaceURI);
                                    this.fNSBinder.declarePrefix(str3, namespaceURI);
                                    this.fLocalNSBinder.declarePrefix(str3, namespaceURI);
                                }
                                nodeName = str4;
                            }
                        } else {
                            nodeName = prefix2.length() > 0 ? prefix2 + ":" + localName : localName;
                        }
                        str = nodeName;
                        z = true;
                        z2 = false;
                    }
                } else {
                    if (localName == null) {
                        z = true;
                        z2 = false;
                        String strCreateMessage = Utils.messages.createMessage(MsgKey.ER_NULL_LOCAL_ELEMENT_NAME, new Object[]{nodeName});
                        if (this.fErrorHandler != null) {
                            this.fErrorHandler.handleError(new DOMErrorImpl((short) 2, strCreateMessage, MsgKey.ER_NULL_LOCAL_ELEMENT_NAME, null, null, null));
                        }
                    }
                    str = nodeName;
                }
            } else {
                z = true;
                z2 = false;
                str = nodeName;
            }
            if (((this.fFeatures & 32768) == 0 || !specified) && (this.fFeatures & 32768) != 0) {
                z3 = z2;
                z = z3;
            } else {
                z3 = z;
            }
            if (z && this.fFilter != null && (this.fFilter.getWhatToShow() & 2) != 0 && !z4) {
                switch (this.fFilter.acceptNode(nodeItem)) {
                    case 2:
                    case 3:
                        z3 = z2;
                        break;
                }
            }
            if (z3 && z4) {
                if ((this.fFeatures & 512) != 0 && localName != null && !"".equals(localName)) {
                    this.fSerializer.addAttribute(namespaceURI, localName, str, str2, nodeValue);
                }
            } else if (z3 && !z4) {
                if ((this.fFeatures & 512) != 0 && namespaceURI != null) {
                    this.fSerializer.addAttribute(namespaceURI, localName, str, str2, nodeValue);
                } else {
                    this.fSerializer.addAttribute("", localName, str, str2, nodeValue);
                }
            }
            if (z4 && (this.fFeatures & 512) != 0) {
                int iIndexOf = str.indexOf(":");
                if (iIndexOf < 0) {
                    strSubstring = "";
                } else {
                    strSubstring = str.substring(iIndexOf + 1);
                }
                if (!"".equals(strSubstring)) {
                    this.fSerializer.namespaceAfterStartElement(strSubstring, nodeValue);
                }
            }
        }
    }

    protected void serializePI(ProcessingInstruction processingInstruction) throws SAXException {
        String nodeName = processingInstruction.getNodeName();
        if ((this.fFeatures & 16384) != 0) {
            isPIWellFormed(processingInstruction);
        }
        if (!applyFilter(processingInstruction, 64)) {
            return;
        }
        if (nodeName.equals("xslt-next-is-raw")) {
            this.fNextIsRaw = true;
        } else {
            this.fSerializer.processingInstruction(nodeName, processingInstruction.getData());
        }
    }

    protected void serializeCDATASection(CDATASection cDATASection) throws SAXException {
        if ((this.fFeatures & 16384) != 0) {
            isCDATASectionWellFormed(cDATASection);
        }
        if ((this.fFeatures & 2) != 0) {
            String nodeValue = cDATASection.getNodeValue();
            int iIndexOf = nodeValue.indexOf(SerializerConstants.CDATA_DELIMITER_CLOSE);
            if ((this.fFeatures & 2048) != 0) {
                if (iIndexOf >= 0) {
                    String strSubstring = nodeValue.substring(0, iIndexOf + 2);
                    String strCreateMessage = Utils.messages.createMessage(MsgKey.ER_CDATA_SECTIONS_SPLIT, null);
                    if (this.fErrorHandler != null) {
                        this.fErrorHandler.handleError(new DOMErrorImpl((short) 1, strCreateMessage, MsgKey.ER_CDATA_SECTIONS_SPLIT, null, strSubstring, null));
                    }
                }
            } else if (iIndexOf >= 0) {
                nodeValue.substring(0, iIndexOf + 2);
                String strCreateMessage2 = Utils.messages.createMessage(MsgKey.ER_CDATA_SECTIONS_SPLIT, null);
                if (this.fErrorHandler != null) {
                    this.fErrorHandler.handleError(new DOMErrorImpl((short) 2, strCreateMessage2, MsgKey.ER_CDATA_SECTIONS_SPLIT));
                    return;
                }
                return;
            }
            if (!applyFilter(cDATASection, 8)) {
                return;
            }
            if (this.fLexicalHandler != null) {
                this.fLexicalHandler.startCDATA();
            }
            dispatachChars(cDATASection);
            if (this.fLexicalHandler != null) {
                this.fLexicalHandler.endCDATA();
                return;
            }
            return;
        }
        dispatachChars(cDATASection);
    }

    protected void serializeText(Text text) throws SAXException {
        boolean zIsElementContentWhitespace;
        if (this.fNextIsRaw) {
            this.fNextIsRaw = false;
            this.fSerializer.processingInstruction("javax.xml.transform.disable-output-escaping", "");
            dispatachChars(text);
            this.fSerializer.processingInstruction("javax.xml.transform.enable-output-escaping", "");
            return;
        }
        if ((this.fFeatures & 16384) != 0) {
            isTextWellFormed(text);
        }
        if (this.fIsLevel3DOM) {
            zIsElementContentWhitespace = text.isElementContentWhitespace();
        } else {
            zIsElementContentWhitespace = false;
        }
        boolean z = true;
        if (zIsElementContentWhitespace && (this.fFeatures & 32) == 0) {
            z = false;
        }
        if (applyFilter(text, 4) && z) {
            dispatachChars(text);
        }
    }

    protected void serializeEntityReference(EntityReference entityReference, boolean z) throws SAXException {
        if (z) {
            if ((this.fFeatures & 64) != 0) {
                if ((this.fFeatures & 16384) != 0) {
                    isEntityReferneceWellFormed(entityReference);
                }
                if ((this.fFeatures & 256) != 0) {
                    checkUnboundPrefixInEntRef(entityReference);
                }
            }
            if (this.fLexicalHandler != null) {
                this.fLexicalHandler.startEntity(entityReference.getNodeName());
                return;
            }
            return;
        }
        if (this.fLexicalHandler != null) {
            this.fLexicalHandler.endEntity(entityReference.getNodeName());
        }
    }

    protected boolean isXMLName(String str, boolean z) {
        if (str == null) {
            return false;
        }
        if (!z) {
            return XMLChar.isValidName(str);
        }
        return XML11Char.isXML11ValidName(str);
    }

    protected boolean isValidQName(String str, String str2, boolean z) {
        if (str2 == null) {
            return false;
        }
        if (!z) {
            if ((str != null && !XMLChar.isValidNCName(str)) || !XMLChar.isValidNCName(str2)) {
                return false;
            }
        } else if ((str != null && !XML11Char.isXML11ValidNCName(str)) || !XML11Char.isXML11ValidNCName(str2)) {
            return false;
        }
        return true;
    }

    protected boolean isWFXMLChar(String str, Character ch) {
        if (str == null || str.length() == 0) {
            return true;
        }
        char[] charArray = str.toCharArray();
        int length = charArray.length;
        if (this.fIsXMLVersion11) {
            int i = 0;
            while (i < length) {
                int i2 = i + 1;
                if (!XML11Char.isXML11Invalid(charArray[i])) {
                    i = i2;
                } else {
                    char c = charArray[i2 - 1];
                    if (XMLChar.isHighSurrogate(c) && i2 < length) {
                        int i3 = i2 + 1;
                        char c2 = charArray[i2];
                        if (XMLChar.isLowSurrogate(c2) && XMLChar.isSupplemental(XMLChar.supplemental(c, c2))) {
                            i = i3;
                        }
                    }
                    new Character(c);
                    return false;
                }
            }
        } else {
            int i4 = 0;
            while (i4 < length) {
                int i5 = i4 + 1;
                if (!XMLChar.isInvalid(charArray[i4])) {
                    i4 = i5;
                } else {
                    char c3 = charArray[i5 - 1];
                    if (XMLChar.isHighSurrogate(c3) && i5 < length) {
                        int i6 = i5 + 1;
                        char c4 = charArray[i5];
                        if (XMLChar.isLowSurrogate(c4) && XMLChar.isSupplemental(XMLChar.supplemental(c3, c4))) {
                            i4 = i6;
                        }
                    }
                    new Character(c3);
                    return false;
                }
            }
        }
        return true;
    }

    protected Character isWFXMLChar(String str) {
        if (str == null || str.length() == 0) {
            return null;
        }
        char[] charArray = str.toCharArray();
        int length = charArray.length;
        int i = 0;
        if (this.fIsXMLVersion11) {
            while (i < length) {
                int i2 = i + 1;
                if (!XML11Char.isXML11Invalid(charArray[i])) {
                    i = i2;
                } else {
                    char c = charArray[i2 - 1];
                    if (XMLChar.isHighSurrogate(c) && i2 < length) {
                        int i3 = i2 + 1;
                        char c2 = charArray[i2];
                        if (XMLChar.isLowSurrogate(c2) && XMLChar.isSupplemental(XMLChar.supplemental(c, c2))) {
                            i = i3;
                        }
                    }
                    return new Character(c);
                }
            }
        } else {
            while (i < length) {
                int i4 = i + 1;
                if (!XMLChar.isInvalid(charArray[i])) {
                    i = i4;
                } else {
                    char c3 = charArray[i4 - 1];
                    if (XMLChar.isHighSurrogate(c3) && i4 < length) {
                        int i5 = i4 + 1;
                        char c4 = charArray[i4];
                        if (XMLChar.isLowSurrogate(c4) && XMLChar.isSupplemental(XMLChar.supplemental(c3, c4))) {
                            i = i5;
                        }
                    }
                    return new Character(c3);
                }
            }
        }
        return null;
    }

    protected void isCommentWellFormed(String str) {
        if (str == null || str.length() == 0) {
            return;
        }
        char[] charArray = str.toCharArray();
        int length = charArray.length;
        if (this.fIsXMLVersion11) {
            int i = 0;
            while (i < length) {
                int i2 = i + 1;
                char c = charArray[i];
                if (XML11Char.isXML11Invalid(c)) {
                    if (XMLChar.isHighSurrogate(c) && i2 < length) {
                        int i3 = i2 + 1;
                        char c2 = charArray[i2];
                        if (XMLChar.isLowSurrogate(c2) && XMLChar.isSupplemental(XMLChar.supplemental(c, c2))) {
                            i = i3;
                        } else {
                            i2 = i3;
                        }
                    }
                    String strCreateMessage = Utils.messages.createMessage(MsgKey.ER_WF_INVALID_CHARACTER_IN_COMMENT, new Object[]{new Character(c)});
                    if (this.fErrorHandler != null) {
                        this.fErrorHandler.handleError(new DOMErrorImpl((short) 3, strCreateMessage, MsgKey.ER_WF_INVALID_CHARACTER, null, null, null));
                    }
                } else if (c == '-' && i2 < length && charArray[i2] == '-') {
                    String strCreateMessage2 = Utils.messages.createMessage(MsgKey.ER_WF_DASH_IN_COMMENT, null);
                    if (this.fErrorHandler != null) {
                        this.fErrorHandler.handleError(new DOMErrorImpl((short) 3, strCreateMessage2, MsgKey.ER_WF_INVALID_CHARACTER, null, null, null));
                    }
                }
                i = i2;
            }
            return;
        }
        int i4 = 0;
        while (i4 < length) {
            int i5 = i4 + 1;
            char c3 = charArray[i4];
            if (XMLChar.isInvalid(c3)) {
                if (XMLChar.isHighSurrogate(c3) && i5 < length) {
                    int i6 = i5 + 1;
                    char c4 = charArray[i5];
                    if (XMLChar.isLowSurrogate(c4) && XMLChar.isSupplemental(XMLChar.supplemental(c3, c4))) {
                        i4 = i6;
                    } else {
                        i5 = i6;
                    }
                }
                String strCreateMessage3 = Utils.messages.createMessage(MsgKey.ER_WF_INVALID_CHARACTER_IN_COMMENT, new Object[]{new Character(c3)});
                if (this.fErrorHandler != null) {
                    this.fErrorHandler.handleError(new DOMErrorImpl((short) 3, strCreateMessage3, MsgKey.ER_WF_INVALID_CHARACTER, null, null, null));
                }
            } else if (c3 == '-' && i5 < length && charArray[i5] == '-') {
                String strCreateMessage4 = Utils.messages.createMessage(MsgKey.ER_WF_DASH_IN_COMMENT, null);
                if (this.fErrorHandler != null) {
                    this.fErrorHandler.handleError(new DOMErrorImpl((short) 3, strCreateMessage4, MsgKey.ER_WF_INVALID_CHARACTER, null, null, null));
                }
            }
            i4 = i5;
        }
    }

    protected void isElementWellFormed(Node node) {
        boolean zIsXMLName;
        if ((this.fFeatures & 256) != 0) {
            zIsXMLName = isValidQName(node.getPrefix(), node.getLocalName(), this.fIsXMLVersion11);
        } else {
            zIsXMLName = isXMLName(node.getNodeName(), this.fIsXMLVersion11);
        }
        if (!zIsXMLName) {
            String strCreateMessage = Utils.messages.createMessage(MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME, new Object[]{"Element", node.getNodeName()});
            if (this.fErrorHandler != null) {
                this.fErrorHandler.handleError(new DOMErrorImpl((short) 3, strCreateMessage, MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME, null, null, null));
            }
        }
    }

    protected void isAttributeWellFormed(Node node) {
        boolean zIsXMLName;
        if ((this.fFeatures & 256) != 0) {
            zIsXMLName = isValidQName(node.getPrefix(), node.getLocalName(), this.fIsXMLVersion11);
        } else {
            zIsXMLName = isXMLName(node.getNodeName(), this.fIsXMLVersion11);
        }
        if (!zIsXMLName) {
            String strCreateMessage = Utils.messages.createMessage(MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME, new Object[]{"Attr", node.getNodeName()});
            if (this.fErrorHandler != null) {
                this.fErrorHandler.handleError(new DOMErrorImpl((short) 3, strCreateMessage, MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME, null, null, null));
            }
        }
        if (node.getNodeValue().indexOf(60) >= 0) {
            String strCreateMessage2 = Utils.messages.createMessage(MsgKey.ER_WF_LT_IN_ATTVAL, new Object[]{((Attr) node).getOwnerElement().getNodeName(), node.getNodeName()});
            if (this.fErrorHandler != null) {
                this.fErrorHandler.handleError(new DOMErrorImpl((short) 3, strCreateMessage2, MsgKey.ER_WF_LT_IN_ATTVAL, null, null, null));
            }
        }
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node nodeItem = childNodes.item(i);
            if (nodeItem != null) {
                short nodeType = nodeItem.getNodeType();
                if (nodeType == 3) {
                    isTextWellFormed((Text) nodeItem);
                } else if (nodeType == 5) {
                    isEntityReferneceWellFormed((EntityReference) nodeItem);
                }
            }
        }
    }

    protected void isPIWellFormed(ProcessingInstruction processingInstruction) {
        if (!isXMLName(processingInstruction.getNodeName(), this.fIsXMLVersion11)) {
            String strCreateMessage = Utils.messages.createMessage(MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME, new Object[]{"ProcessingInstruction", processingInstruction.getTarget()});
            if (this.fErrorHandler != null) {
                this.fErrorHandler.handleError(new DOMErrorImpl((short) 3, strCreateMessage, MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME, null, null, null));
            }
        }
        Character chIsWFXMLChar = isWFXMLChar(processingInstruction.getData());
        if (chIsWFXMLChar != null) {
            String strCreateMessage2 = Utils.messages.createMessage(MsgKey.ER_WF_INVALID_CHARACTER_IN_PI, new Object[]{Integer.toHexString(Character.getNumericValue(chIsWFXMLChar.charValue()))});
            if (this.fErrorHandler != null) {
                this.fErrorHandler.handleError(new DOMErrorImpl((short) 3, strCreateMessage2, MsgKey.ER_WF_INVALID_CHARACTER, null, null, null));
            }
        }
    }

    protected void isCDATASectionWellFormed(CDATASection cDATASection) {
        Character chIsWFXMLChar = isWFXMLChar(cDATASection.getData());
        if (chIsWFXMLChar != null) {
            String strCreateMessage = Utils.messages.createMessage(MsgKey.ER_WF_INVALID_CHARACTER_IN_CDATA, new Object[]{Integer.toHexString(Character.getNumericValue(chIsWFXMLChar.charValue()))});
            if (this.fErrorHandler != null) {
                this.fErrorHandler.handleError(new DOMErrorImpl((short) 3, strCreateMessage, MsgKey.ER_WF_INVALID_CHARACTER, null, null, null));
            }
        }
    }

    protected void isTextWellFormed(Text text) {
        Character chIsWFXMLChar = isWFXMLChar(text.getData());
        if (chIsWFXMLChar != null) {
            String strCreateMessage = Utils.messages.createMessage(MsgKey.ER_WF_INVALID_CHARACTER_IN_TEXT, new Object[]{Integer.toHexString(Character.getNumericValue(chIsWFXMLChar.charValue()))});
            if (this.fErrorHandler != null) {
                this.fErrorHandler.handleError(new DOMErrorImpl((short) 3, strCreateMessage, MsgKey.ER_WF_INVALID_CHARACTER, null, null, null));
            }
        }
    }

    protected void isEntityReferneceWellFormed(EntityReference entityReference) {
        String namespaceURI;
        if (!isXMLName(entityReference.getNodeName(), this.fIsXMLVersion11)) {
            String strCreateMessage = Utils.messages.createMessage(MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME, new Object[]{"EntityReference", entityReference.getNodeName()});
            if (this.fErrorHandler != null) {
                this.fErrorHandler.handleError(new DOMErrorImpl((short) 3, strCreateMessage, MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME, null, null, null));
            }
        }
        Node parentNode = entityReference.getParentNode();
        DocumentType doctype = entityReference.getOwnerDocument().getDoctype();
        if (doctype != null) {
            NamedNodeMap entities = doctype.getEntities();
            for (int i = 0; i < entities.getLength(); i++) {
                Entity entity = (Entity) entities.item(i);
                String nodeName = entityReference.getNodeName() == null ? "" : entityReference.getNodeName();
                if (entityReference.getNamespaceURI() == null) {
                    namespaceURI = "";
                } else {
                    namespaceURI = entityReference.getNamespaceURI();
                }
                String nodeName2 = entity.getNodeName() == null ? "" : entity.getNodeName();
                String namespaceURI2 = entity.getNamespaceURI() == null ? "" : entity.getNamespaceURI();
                if (parentNode.getNodeType() == 1 && namespaceURI2.equals(namespaceURI) && nodeName2.equals(nodeName) && entity.getNotationName() != null) {
                    String strCreateMessage2 = Utils.messages.createMessage(MsgKey.ER_WF_REF_TO_UNPARSED_ENT, new Object[]{entityReference.getNodeName()});
                    if (this.fErrorHandler != null) {
                        this.fErrorHandler.handleError(new DOMErrorImpl((short) 3, strCreateMessage2, MsgKey.ER_WF_REF_TO_UNPARSED_ENT, null, null, null));
                    }
                }
                if (parentNode.getNodeType() == 2 && namespaceURI2.equals(namespaceURI) && nodeName2.equals(nodeName) && (entity.getPublicId() != null || entity.getSystemId() != null || entity.getNotationName() != null)) {
                    String strCreateMessage3 = Utils.messages.createMessage(MsgKey.ER_WF_REF_TO_EXTERNAL_ENT, new Object[]{entityReference.getNodeName()});
                    if (this.fErrorHandler != null) {
                        this.fErrorHandler.handleError(new DOMErrorImpl((short) 3, strCreateMessage3, MsgKey.ER_WF_REF_TO_EXTERNAL_ENT, null, null, null));
                    }
                }
            }
        }
    }

    protected void checkUnboundPrefixInEntRef(Node node) {
        Node firstChild = node.getFirstChild();
        while (firstChild != null) {
            Node nextSibling = firstChild.getNextSibling();
            if (firstChild.getNodeType() == 1) {
                String prefix = firstChild.getPrefix();
                if (prefix != null && this.fNSBinder.getURI(prefix) == null) {
                    String strCreateMessage = Utils.messages.createMessage("unbound-prefix-in-entity-reference", new Object[]{node.getNodeName(), firstChild.getNodeName(), prefix});
                    if (this.fErrorHandler != null) {
                        this.fErrorHandler.handleError(new DOMErrorImpl((short) 3, strCreateMessage, "unbound-prefix-in-entity-reference", null, null, null));
                    }
                }
                NamedNodeMap attributes = firstChild.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    String prefix2 = attributes.item(i).getPrefix();
                    if (prefix2 != null && this.fNSBinder.getURI(prefix2) == null) {
                        String strCreateMessage2 = Utils.messages.createMessage("unbound-prefix-in-entity-reference", new Object[]{node.getNodeName(), firstChild.getNodeName(), attributes.item(i)});
                        if (this.fErrorHandler != null) {
                            this.fErrorHandler.handleError(new DOMErrorImpl((short) 3, strCreateMessage2, "unbound-prefix-in-entity-reference", null, null, null));
                        }
                    }
                }
            }
            if (firstChild.hasChildNodes()) {
                checkUnboundPrefixInEntRef(firstChild);
            }
            firstChild = nextSibling;
        }
    }

    protected void recordLocalNSDecl(Node node) {
        NamedNodeMap attributes = ((Element) node).getAttributes();
        int length = attributes.getLength();
        for (int i = 0; i < length; i++) {
            Node nodeItem = attributes.item(i);
            String localName = nodeItem.getLocalName();
            String prefix = nodeItem.getPrefix();
            String nodeValue = nodeItem.getNodeValue();
            String namespaceURI = nodeItem.getNamespaceURI();
            if (localName == null || "xmlns".equals(localName)) {
                localName = "";
            }
            if (prefix == null) {
                prefix = "";
            }
            if (nodeValue == null) {
                nodeValue = "";
            }
            if (namespaceURI == null) {
                namespaceURI = "";
            }
            if ("http://www.w3.org/2000/xmlns/".equals(namespaceURI)) {
                if ("http://www.w3.org/2000/xmlns/".equals(nodeValue)) {
                    String strCreateMessage = Utils.messages.createMessage(MsgKey.ER_NS_PREFIX_CANNOT_BE_BOUND, new Object[]{prefix, "http://www.w3.org/2000/xmlns/"});
                    if (this.fErrorHandler != null) {
                        this.fErrorHandler.handleError(new DOMErrorImpl((short) 2, strCreateMessage, MsgKey.ER_NS_PREFIX_CANNOT_BE_BOUND, null, null, null));
                    }
                } else if ("xmlns".equals(prefix)) {
                    if (nodeValue.length() != 0) {
                        this.fNSBinder.declarePrefix(localName, nodeValue);
                    }
                } else {
                    this.fNSBinder.declarePrefix("", nodeValue);
                }
            }
        }
    }

    protected void fixupElementNS(Node node) throws SAXException {
        Element element = (Element) node;
        String namespaceURI = element.getNamespaceURI();
        String prefix = element.getPrefix();
        String localName = element.getLocalName();
        if (namespaceURI != null) {
            if (prefix == null) {
                prefix = "";
            }
            String uri = this.fNSBinder.getURI(prefix);
            if (uri == null || !uri.equals(namespaceURI)) {
                if ((this.fFeatures & 512) != 0) {
                    if ("".equals(prefix) || "".equals(namespaceURI)) {
                        element.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", namespaceURI);
                    } else {
                        element.setAttributeNS("http://www.w3.org/2000/xmlns/", Constants.ATTRNAME_XMLNS + prefix, namespaceURI);
                    }
                }
                this.fLocalNSBinder.declarePrefix(prefix, namespaceURI);
                this.fNSBinder.declarePrefix(prefix, namespaceURI);
                return;
            }
            return;
        }
        if (localName == null || "".equals(localName)) {
            String strCreateMessage = Utils.messages.createMessage(MsgKey.ER_NULL_LOCAL_ELEMENT_NAME, new Object[]{node.getNodeName()});
            if (this.fErrorHandler != null) {
                this.fErrorHandler.handleError(new DOMErrorImpl((short) 2, strCreateMessage, MsgKey.ER_NULL_LOCAL_ELEMENT_NAME, null, null, null));
                return;
            }
            return;
        }
        String uri2 = this.fNSBinder.getURI("");
        if (uri2 != null && uri2.length() > 0) {
            element.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", "");
            this.fLocalNSBinder.declarePrefix("", "");
            this.fNSBinder.declarePrefix("", "");
        }
    }

    static {
        s_propKeys.put("{http://www.w3.org/TR/DOM-Level-3-LS}cdata-sections", new Integer(2));
        s_propKeys.put("{http://www.w3.org/TR/DOM-Level-3-LS}comments", new Integer(8));
        s_propKeys.put("{http://www.w3.org/TR/DOM-Level-3-LS}element-content-whitespace", new Integer(32));
        s_propKeys.put("{http://www.w3.org/TR/DOM-Level-3-LS}entities", new Integer(64));
        s_propKeys.put("{http://www.w3.org/TR/DOM-Level-3-LS}namespaces", new Integer(256));
        s_propKeys.put("{http://www.w3.org/TR/DOM-Level-3-LS}namespace-declarations", new Integer(512));
        s_propKeys.put("{http://www.w3.org/TR/DOM-Level-3-LS}split-cdata-sections", new Integer(2048));
        s_propKeys.put("{http://www.w3.org/TR/DOM-Level-3-LS}well-formed", new Integer(16384));
        s_propKeys.put("{http://www.w3.org/TR/DOM-Level-3-LS}discard-default-content", new Integer(32768));
        s_propKeys.put("{http://www.w3.org/TR/DOM-Level-3-LS}format-pretty-print", "");
        s_propKeys.put("omit-xml-declaration", "");
        s_propKeys.put("{http://xml.apache.org/xerces-2j}xml-version", "");
        s_propKeys.put("encoding", "");
        s_propKeys.put("{http://xml.apache.org/xerces-2j}entities", "");
    }

    protected void initProperties(Properties properties) {
        Enumeration enumerationKeys = properties.keys();
        while (enumerationKeys.hasMoreElements()) {
            String str = (String) enumerationKeys.nextElement();
            Object obj = s_propKeys.get(str);
            if (obj != null) {
                if (obj instanceof Integer) {
                    int iIntValue = ((Integer) obj).intValue();
                    if (properties.getProperty(str).endsWith("yes")) {
                        this.fFeatures |= iIntValue;
                    } else {
                        this.fFeatures &= ~iIntValue;
                    }
                } else if ("{http://www.w3.org/TR/DOM-Level-3-LS}format-pretty-print".equals(str)) {
                    if (properties.getProperty(str).endsWith("yes")) {
                        this.fSerializer.setIndent(true);
                        this.fSerializer.setIndentAmount(3);
                    } else {
                        this.fSerializer.setIndent(false);
                    }
                } else if ("omit-xml-declaration".equals(str)) {
                    if (properties.getProperty(str).endsWith("yes")) {
                        this.fSerializer.setOmitXMLDeclaration(true);
                    } else {
                        this.fSerializer.setOmitXMLDeclaration(false);
                    }
                } else if ("{http://xml.apache.org/xerces-2j}xml-version".equals(str)) {
                    String property = properties.getProperty(str);
                    if (SerializerConstants.XMLVERSION11.equals(property)) {
                        this.fIsXMLVersion11 = true;
                        this.fSerializer.setVersion(property);
                    } else {
                        this.fSerializer.setVersion(SerializerConstants.XMLVERSION10);
                    }
                } else if ("encoding".equals(str)) {
                    String property2 = properties.getProperty(str);
                    if (property2 != null) {
                        this.fSerializer.setEncoding(property2);
                    }
                } else if ("{http://xml.apache.org/xerces-2j}entities".equals(str)) {
                    if (properties.getProperty(str).endsWith("yes")) {
                        this.fSerializer.setDTDEntityExpansion(false);
                    } else {
                        this.fSerializer.setDTDEntityExpansion(true);
                    }
                }
            }
        }
        if (this.fNewLine != null) {
            this.fSerializer.setOutputProperty(OutputPropertiesFactory.S_KEY_LINE_SEPARATOR, this.fNewLine);
        }
    }
}
