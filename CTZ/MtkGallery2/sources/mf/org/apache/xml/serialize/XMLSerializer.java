package mf.org.apache.xml.serialize;

import java.io.IOException;
import java.util.Map;
import mf.org.apache.xerces.dom.DOMMessageFormatter;
import mf.org.apache.xerces.impl.xpath.XPath;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.util.NamespaceSupport;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.w3c.dom.Attr;
import mf.org.w3c.dom.Element;
import mf.org.w3c.dom.NamedNodeMap;
import mf.org.w3c.dom.Node;
import org.xml.sax.AttributeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class XMLSerializer extends BaseMarkupSerializer {
    protected NamespaceSupport fLocalNSBinder;
    protected NamespaceSupport fNSBinder;
    protected boolean fNamespacePrefixes;
    protected boolean fNamespaces;
    private boolean fPreserveSpace;
    protected SymbolTable fSymbolTable;

    public XMLSerializer() {
        super(new OutputFormat("xml", null, false));
        this.fNamespaces = false;
        this.fNamespacePrefixes = true;
    }

    public XMLSerializer(OutputFormat format) {
        super(format != null ? format : new OutputFormat("xml", null, false));
        this.fNamespaces = false;
        this.fNamespacePrefixes = true;
        this._format.setMethod("xml");
    }

    @Override
    public void startElement(String namespaceURI, String localName, String rawName, Attributes attrs) throws SAXException {
        String prefix;
        String prefix2;
        try {
            if (this._printer == null) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "NoWriterSupplied", null);
                throw new IllegalStateException(msg);
            }
            ElementState state = getElementState();
            if (isDocumentState()) {
                if (!this._started) {
                    startDocument((localName == null || localName.length() == 0) ? rawName : localName);
                }
            } else {
                if (state.empty) {
                    this._printer.printText('>');
                }
                if (state.inCData) {
                    this._printer.printText("]]>");
                    state.inCData = false;
                }
                if (this._indenting && !state.preserveSpace && (state.empty || state.afterElement || state.afterComment)) {
                    this._printer.breakLine();
                }
            }
            boolean preserveSpace = state.preserveSpace;
            Attributes attrs2 = extractNamespaces(attrs);
            if (rawName == null || rawName.length() == 0) {
                if (localName == null) {
                    String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "NoName", null);
                    throw new SAXException(msg2);
                }
                if (namespaceURI == null || namespaceURI.equals("") || (prefix = getPrefix(namespaceURI)) == null || prefix.length() <= 0) {
                    rawName = localName;
                } else {
                    rawName = String.valueOf(prefix) + ":" + localName;
                }
            }
            this._printer.printText('<');
            this._printer.printText(rawName);
            this._printer.indent();
            if (attrs2 != null) {
                for (int i = 0; i < attrs2.getLength(); i++) {
                    this._printer.printSpace();
                    String name = attrs2.getQName(i);
                    if (name != null && name.length() == 0) {
                        name = attrs2.getLocalName(i);
                        String attrURI = attrs2.getURI(i);
                        if (attrURI != null && attrURI.length() != 0 && ((namespaceURI == null || namespaceURI.length() == 0 || !attrURI.equals(namespaceURI)) && (prefix2 = getPrefix(attrURI)) != null && prefix2.length() > 0)) {
                            name = String.valueOf(prefix2) + ":" + name;
                        }
                    }
                    String value = attrs2.getValue(i);
                    if (value == null) {
                        value = "";
                    }
                    this._printer.printText(name);
                    this._printer.printText("=\"");
                    printEscaped(value);
                    this._printer.printText('\"');
                    if (name.equals("xml:space")) {
                        if (value.equals(SchemaSymbols.ATTVAL_PRESERVE)) {
                            preserveSpace = true;
                        } else {
                            preserveSpace = this._format.getPreserveSpace();
                        }
                    }
                }
            }
            if (this._prefixes != null) {
                for (Map.Entry entry : this._prefixes.entrySet()) {
                    this._printer.printSpace();
                    String value2 = (String) entry.getKey();
                    String name2 = (String) entry.getValue();
                    if (name2.length() == 0) {
                        this._printer.printText("xmlns=\"");
                        printEscaped(value2);
                        this._printer.printText('\"');
                    } else {
                        this._printer.printText("xmlns:");
                        this._printer.printText(name2);
                        this._printer.printText("=\"");
                        printEscaped(value2);
                        this._printer.printText('\"');
                    }
                }
            }
            ElementState state2 = enterElementState(namespaceURI, localName, rawName, preserveSpace);
            String name3 = (localName == null || localName.length() == 0) ? rawName : String.valueOf(namespaceURI) + "^" + localName;
            state2.doCData = this._format.isCDataElement(name3);
            state2.unescaped = this._format.isNonEscapingElement(name3);
        } catch (IOException except) {
            throw new SAXException(except);
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String rawName) throws SAXException {
        try {
            endElementIO(namespaceURI, localName, rawName);
        } catch (IOException except) {
            throw new SAXException(except);
        }
    }

    public void endElementIO(String namespaceURI, String localName, String rawName) throws IOException {
        this._printer.unindent();
        ElementState state = getElementState();
        if (state.empty) {
            this._printer.printText("/>");
        } else {
            if (state.inCData) {
                this._printer.printText("]]>");
            }
            if (this._indenting && !state.preserveSpace && (state.afterElement || state.afterComment)) {
                this._printer.breakLine();
            }
            this._printer.printText("</");
            this._printer.printText(state.rawName);
            this._printer.printText('>');
        }
        ElementState state2 = leaveElementState();
        state2.afterElement = true;
        state2.afterComment = false;
        state2.empty = false;
        if (isDocumentState()) {
            this._printer.flush();
        }
    }

    @Override
    public void startElement(String tagName, AttributeList attrs) throws SAXException {
        try {
            if (this._printer == null) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "NoWriterSupplied", null);
                throw new IllegalStateException(msg);
            }
            ElementState state = getElementState();
            if (isDocumentState()) {
                if (!this._started) {
                    startDocument(tagName);
                }
            } else {
                if (state.empty) {
                    this._printer.printText('>');
                }
                if (state.inCData) {
                    this._printer.printText("]]>");
                    state.inCData = false;
                }
                if (this._indenting && !state.preserveSpace && (state.empty || state.afterElement || state.afterComment)) {
                    this._printer.breakLine();
                }
            }
            boolean preserveSpace = state.preserveSpace;
            this._printer.printText('<');
            this._printer.printText(tagName);
            this._printer.indent();
            if (attrs != null) {
                for (int i = 0; i < attrs.getLength(); i++) {
                    this._printer.printSpace();
                    String name = attrs.getName(i);
                    String value = attrs.getValue(i);
                    if (value != null) {
                        this._printer.printText(name);
                        this._printer.printText("=\"");
                        printEscaped(value);
                        this._printer.printText('\"');
                    }
                    if (name.equals("xml:space")) {
                        if (value.equals(SchemaSymbols.ATTVAL_PRESERVE)) {
                            preserveSpace = true;
                        } else {
                            preserveSpace = this._format.getPreserveSpace();
                        }
                    }
                }
            }
            ElementState state2 = enterElementState(null, null, tagName, preserveSpace);
            state2.doCData = this._format.isCDataElement(tagName);
            state2.unescaped = this._format.isNonEscapingElement(tagName);
        } catch (IOException except) {
            throw new SAXException(except);
        }
    }

    @Override
    public void endElement(String tagName) throws SAXException {
        endElement(null, null, tagName);
    }

    protected void startDocument(String rootTagName) throws IOException {
        String dtd = this._printer.leaveDTD();
        if (!this._started) {
            if (!this._format.getOmitXMLDeclaration()) {
                StringBuffer buffer = new StringBuffer("<?xml version=\"");
                if (this._format.getVersion() != null) {
                    buffer.append(this._format.getVersion());
                } else {
                    buffer.append("1.0");
                }
                buffer.append('\"');
                String format_encoding = this._format.getEncoding();
                if (format_encoding != null) {
                    buffer.append(" encoding=\"");
                    buffer.append(format_encoding);
                    buffer.append('\"');
                }
                if (this._format.getStandalone() && this._docTypeSystemId == null && this._docTypePublicId == null) {
                    buffer.append(" standalone=\"yes\"");
                }
                buffer.append("?>");
                this._printer.printText(buffer);
                this._printer.breakLine();
            }
            if (!this._format.getOmitDocumentType()) {
                if (this._docTypeSystemId != null) {
                    this._printer.printText("<!DOCTYPE ");
                    this._printer.printText(rootTagName);
                    if (this._docTypePublicId != null) {
                        this._printer.printText(" PUBLIC ");
                        printDoctypeURL(this._docTypePublicId);
                        if (this._indenting) {
                            this._printer.breakLine();
                            for (int i = 0; i < 18 + rootTagName.length(); i++) {
                                this._printer.printText(" ");
                            }
                        } else {
                            this._printer.printText(" ");
                        }
                        printDoctypeURL(this._docTypeSystemId);
                    } else {
                        this._printer.printText(" SYSTEM ");
                        printDoctypeURL(this._docTypeSystemId);
                    }
                    if (dtd != null && dtd.length() > 0) {
                        this._printer.printText(" [");
                        printText(dtd, true, true);
                        this._printer.printText(']');
                    }
                    this._printer.printText(">");
                    this._printer.breakLine();
                } else if (dtd != null && dtd.length() > 0) {
                    this._printer.printText("<!DOCTYPE ");
                    this._printer.printText(rootTagName);
                    this._printer.printText(" [");
                    printText(dtd, true, true);
                    this._printer.printText("]>");
                    this._printer.breakLine();
                }
            }
        }
        this._started = true;
        serializePreRoot();
    }

    @Override
    protected void serializeElement(Element element) throws IOException {
        int i;
        String str;
        ElementState elementState;
        boolean z;
        boolean z2;
        Object obj;
        if (this.fNamespaces) {
            this.fLocalNSBinder.reset();
            this.fNSBinder.pushContext();
        }
        String tagName = element.getTagName();
        ElementState elementState2 = getElementState();
        boolean z3 = false;
        z3 = false;
        if (!isDocumentState()) {
            if (elementState2.empty) {
                this._printer.printText('>');
            }
            if (elementState2.inCData) {
                this._printer.printText("]]>");
                elementState2.inCData = false;
            }
            if (this._indenting && !elementState2.preserveSpace && (elementState2.empty || elementState2.afterElement || elementState2.afterComment)) {
                this._printer.breakLine();
            }
        } else if (!this._started) {
            startDocument(tagName);
        }
        this.fPreserveSpace = elementState2.preserveSpace;
        int length = 0;
        NamedNodeMap attributes = null;
        if (element.hasAttributes()) {
            attributes = element.getAttributes();
            length = attributes.getLength();
        }
        int i2 = 1;
        boolean z4 = 1;
        ?? r9 = 0;
        String str2 = 0;
        if (this.fNamespaces) {
            int i3 = 0;
            while (i3 < length) {
                String str3 = tagName;
                ElementState elementState3 = elementState2;
                boolean z5 = z3 ? 1 : 0;
                int i4 = i2;
                Attr attr = (Attr) attributes.item(i3);
                String namespaceURI = attr.getNamespaceURI();
                if (namespaceURI == null || !namespaceURI.equals(NamespaceContext.XMLNS_URI)) {
                    obj = null;
                } else {
                    String nodeValue = attr.getNodeValue();
                    if (nodeValue == null) {
                        nodeValue = XMLSymbols.EMPTY_STRING;
                    }
                    if (nodeValue.equals(NamespaceContext.XMLNS_URI)) {
                        if (this.fDOMErrorHandler != null) {
                            modifyDOMError(DOMMessageFormatter.formatMessage("http://www.w3.org/TR/1998/REC-xml-19980210", "CantBindXMLNS", null), (short) 2, null, attr);
                            if (!this.fDOMErrorHandler.handleError(this.fDOMError)) {
                                throw new RuntimeException(DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "SerializationStopped", null));
                            }
                        }
                        obj = null;
                    } else {
                        obj = null;
                        String prefix = attr.getPrefix();
                        String strAddSymbol = (prefix == null || prefix.length() == 0) ? XMLSymbols.EMPTY_STRING : this.fSymbolTable.addSymbol(prefix);
                        String strAddSymbol2 = this.fSymbolTable.addSymbol(attr.getLocalName());
                        if (strAddSymbol == XMLSymbols.PREFIX_XMLNS) {
                            String strAddSymbol3 = this.fSymbolTable.addSymbol(nodeValue);
                            if (strAddSymbol3.length() != 0) {
                                this.fNSBinder.declarePrefix(strAddSymbol2, strAddSymbol3);
                            }
                        } else {
                            this.fNSBinder.declarePrefix(XMLSymbols.EMPTY_STRING, this.fSymbolTable.addSymbol(nodeValue));
                        }
                    }
                }
                i3++;
                i2 = i4 == true ? 1 : 0;
                z3 = z5 ? 1 : 0;
                r9 = obj;
                tagName = str3;
                elementState2 = elementState3;
            }
            String namespaceURI2 = element.getNamespaceURI();
            String prefix2 = element.getPrefix();
            if (namespaceURI2 == null || prefix2 == null || namespaceURI2.length() != 0 || prefix2.length() == 0) {
                this._printer.printText('<');
                this._printer.printText(tagName);
                this._printer.indent();
            } else {
                prefix2 = null;
                this._printer.printText('<');
                this._printer.printText(element.getLocalName());
                this._printer.indent();
            }
            if (namespaceURI2 != null) {
                String strAddSymbol4 = this.fSymbolTable.addSymbol(namespaceURI2);
                String strAddSymbol5 = (prefix2 == null || prefix2.length() == 0) ? XMLSymbols.EMPTY_STRING : this.fSymbolTable.addSymbol(prefix2);
                if (this.fNSBinder.getURI(strAddSymbol5) != strAddSymbol4) {
                    if (this.fNamespacePrefixes) {
                        printNamespaceAttr(strAddSymbol5, strAddSymbol4);
                    }
                    this.fLocalNSBinder.declarePrefix(strAddSymbol5, strAddSymbol4);
                    this.fNSBinder.declarePrefix(strAddSymbol5, strAddSymbol4);
                }
            } else {
                if (element.getLocalName() != null) {
                    String uri = this.fNSBinder.getURI(XMLSymbols.EMPTY_STRING);
                    if (uri != null && uri.length() > 0) {
                        if (this.fNamespacePrefixes) {
                            printNamespaceAttr(XMLSymbols.EMPTY_STRING, XMLSymbols.EMPTY_STRING);
                        }
                        this.fLocalNSBinder.declarePrefix(XMLSymbols.EMPTY_STRING, XMLSymbols.EMPTY_STRING);
                        this.fNSBinder.declarePrefix(XMLSymbols.EMPTY_STRING, XMLSymbols.EMPTY_STRING);
                    }
                } else if (this.fDOMErrorHandler != null) {
                    Object[] objArr = new Object[i2];
                    objArr[z3 ? 1 : 0] = element.getNodeName();
                    modifyDOMError(DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NullLocalElementName", objArr), (short) 2, r9, element);
                    if (!this.fDOMErrorHandler.handleError(this.fDOMError)) {
                        throw new RuntimeException(DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "SerializationStopped", r9));
                    }
                }
                i = 0;
                z4 = i2;
                str2 = r9;
                while (i < length) {
                    Attr attr2 = (Attr) attributes.item(i);
                    String value = attr2.getValue();
                    String nodeName = attr2.getNodeName();
                    String namespaceURI3 = attr2.getNamespaceURI();
                    if (namespaceURI3 != null && namespaceURI3.length() == 0) {
                        namespaceURI3 = null;
                        nodeName = attr2.getLocalName();
                    }
                    if (value == null) {
                        value = XMLSymbols.EMPTY_STRING;
                    }
                    if (namespaceURI3 != null) {
                        String prefix3 = attr2.getPrefix();
                        String strAddSymbol6 = prefix3 == null ? XMLSymbols.EMPTY_STRING : this.fSymbolTable.addSymbol(prefix3);
                        String strAddSymbol7 = this.fSymbolTable.addSymbol(attr2.getLocalName());
                        if (namespaceURI3 == null || !namespaceURI3.equals(NamespaceContext.XMLNS_URI)) {
                            String strAddSymbol8 = this.fSymbolTable.addSymbol(namespaceURI3);
                            String uri2 = this.fNSBinder.getURI(strAddSymbol6);
                            if (strAddSymbol6 == XMLSymbols.EMPTY_STRING || uri2 != strAddSymbol8) {
                                String nodeName2 = attr2.getNodeName();
                                String prefix4 = this.fNSBinder.getPrefix(strAddSymbol8);
                                if (prefix4 == null || prefix4 == XMLSymbols.EMPTY_STRING) {
                                    if (strAddSymbol6 == XMLSymbols.EMPTY_STRING || this.fLocalNSBinder.getURI(strAddSymbol6) != null) {
                                        SymbolTable symbolTable = this.fSymbolTable;
                                        str = tagName;
                                        elementState = elementState2;
                                        StringBuilder sb = new StringBuilder("NS");
                                        int i5 = 1 + 1;
                                        sb.append(1);
                                        strAddSymbol6 = symbolTable.addSymbol(sb.toString());
                                        while (this.fLocalNSBinder.getURI(strAddSymbol6) != null) {
                                            strAddSymbol6 = this.fSymbolTable.addSymbol("NS" + i5);
                                            i5++;
                                        }
                                        nodeName = String.valueOf(strAddSymbol6) + ":" + strAddSymbol7;
                                    } else {
                                        str = tagName;
                                        elementState = elementState2;
                                        nodeName = nodeName2;
                                    }
                                    if (this.fNamespacePrefixes) {
                                        printNamespaceAttr(strAddSymbol6, strAddSymbol8);
                                    }
                                    value = this.fSymbolTable.addSymbol(value);
                                    this.fLocalNSBinder.declarePrefix(strAddSymbol6, value);
                                    this.fNSBinder.declarePrefix(strAddSymbol6, strAddSymbol8);
                                    printAttribute(nodeName, value == null ? XMLSymbols.EMPTY_STRING : value, attr2.getSpecified(), attr2);
                                    z = true;
                                } else {
                                    strAddSymbol6 = prefix4;
                                    nodeName = String.valueOf(strAddSymbol6) + ":" + strAddSymbol7;
                                    str = tagName;
                                    elementState = elementState2;
                                    printAttribute(nodeName, value == null ? XMLSymbols.EMPTY_STRING : value, attr2.getSpecified(), attr2);
                                    z = true;
                                }
                            } else {
                                str = tagName;
                                elementState = elementState2;
                                printAttribute(nodeName, value == null ? XMLSymbols.EMPTY_STRING : value, attr2.getSpecified(), attr2);
                                z = true;
                            }
                        } else {
                            String prefix5 = attr2.getPrefix();
                            String strAddSymbol9 = (prefix5 == null || prefix5.length() == 0) ? XMLSymbols.EMPTY_STRING : this.fSymbolTable.addSymbol(prefix5);
                            String strAddSymbol10 = this.fSymbolTable.addSymbol(attr2.getLocalName());
                            if (strAddSymbol9 == XMLSymbols.PREFIX_XMLNS) {
                                String uri3 = this.fLocalNSBinder.getURI(strAddSymbol10);
                                String strAddSymbol11 = this.fSymbolTable.addSymbol(value);
                                if (strAddSymbol11.length() != 0 && uri3 == null) {
                                    if (this.fNamespacePrefixes) {
                                        printNamespaceAttr(strAddSymbol10, strAddSymbol11);
                                    }
                                    this.fLocalNSBinder.declarePrefix(strAddSymbol10, strAddSymbol11);
                                }
                                str = tagName;
                                elementState = elementState2;
                                z = z4 ? 1 : 0;
                            } else {
                                this.fNSBinder.getURI(XMLSymbols.EMPTY_STRING);
                                String uri4 = this.fLocalNSBinder.getURI(XMLSymbols.EMPTY_STRING);
                                String strAddSymbol12 = this.fSymbolTable.addSymbol(value);
                                if (uri4 == null && this.fNamespacePrefixes) {
                                    printNamespaceAttr(XMLSymbols.EMPTY_STRING, strAddSymbol12);
                                }
                                str = tagName;
                                elementState = elementState2;
                                z = z4 ? 1 : 0;
                            }
                        }
                        z2 = false;
                    } else {
                        str = tagName;
                        elementState = elementState2;
                        if (attr2.getLocalName() == null) {
                            if (this.fDOMErrorHandler != null) {
                                z = true;
                                z2 = false;
                                modifyDOMError(DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NullLocalAttrName", new Object[]{attr2.getNodeName()}), (short) 2, null, attr2);
                                if (!this.fDOMErrorHandler.handleError(this.fDOMError)) {
                                    throw new RuntimeException(DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "SerializationStopped", null));
                                }
                            } else {
                                z = true;
                                z2 = false;
                            }
                            printAttribute(nodeName, value, attr2.getSpecified(), attr2);
                        } else {
                            z = true;
                            z2 = false;
                            printAttribute(nodeName, value, attr2.getSpecified(), attr2);
                        }
                    }
                    i++;
                    z4 = z;
                    z3 = z2;
                    tagName = str;
                    elementState2 = elementState;
                    str2 = 0;
                }
            }
            i = 0;
            z4 = i2;
            str2 = r9;
            while (i < length) {
            }
        } else {
            this._printer.printText('<');
            this._printer.printText(tagName);
            this._printer.indent();
            for (int i6 = 0; i6 < length; i6++) {
                Attr attr3 = (Attr) attributes.item(i6);
                String name = attr3.getName();
                String value2 = attr3.getValue();
                if (value2 == null) {
                    value2 = "";
                }
                printAttribute(name, value2, attr3.getSpecified(), attr3);
            }
        }
        if (element.hasChildNodes()) {
            ElementState elementStateEnterElementState = enterElementState(str2, str2, tagName, this.fPreserveSpace);
            elementStateEnterElementState.doCData = this._format.isCDataElement(tagName);
            elementStateEnterElementState.unescaped = this._format.isNonEscapingElement(tagName);
            for (Node firstChild = element.getFirstChild(); firstChild != null; firstChild = firstChild.getNextSibling()) {
                serializeNode(firstChild);
            }
            if (this.fNamespaces) {
                this.fNSBinder.popContext();
            }
            endElementIO(str2, str2, tagName);
            return;
        }
        if (this.fNamespaces) {
            this.fNSBinder.popContext();
        }
        this._printer.unindent();
        this._printer.printText("/>");
        elementState2.afterElement = z4;
        elementState2.afterComment = z3;
        elementState2.empty = z3;
        if (isDocumentState()) {
            this._printer.flush();
        }
    }

    private void printNamespaceAttr(String prefix, String uri) throws IOException {
        this._printer.printSpace();
        if (prefix == XMLSymbols.EMPTY_STRING) {
            this._printer.printText(XMLSymbols.PREFIX_XMLNS);
        } else {
            this._printer.printText("xmlns:" + prefix);
        }
        this._printer.printText("=\"");
        printEscaped(uri);
        this._printer.printText('\"');
    }

    private void printAttribute(String name, String value, boolean isSpecified, Attr attr) throws IOException {
        if (isSpecified || (this.features & 64) == 0) {
            if (this.fDOMFilter != null && (this.fDOMFilter.getWhatToShow() & 2) != 0) {
                short code = this.fDOMFilter.acceptNode(attr);
                switch (code) {
                    case 2:
                    case 3:
                        return;
                }
            }
            this._printer.printSpace();
            this._printer.printText(name);
            this._printer.printText("=\"");
            printEscaped(value);
            this._printer.printText('\"');
        }
        if (name.equals("xml:space")) {
            if (value.equals(SchemaSymbols.ATTVAL_PRESERVE)) {
                this.fPreserveSpace = true;
            } else {
                this.fPreserveSpace = this._format.getPreserveSpace();
            }
        }
    }

    @Override
    protected String getEntityRef(int ch) {
        if (ch == 34) {
            return "quot";
        }
        if (ch == 60) {
            return "lt";
        }
        if (ch == 62) {
            return "gt";
        }
        switch (ch) {
            case XPath.Tokens.EXPRTOKEN_AXISNAME_DESCENDANT_OR_SELF:
                return "amp";
            case XPath.Tokens.EXPRTOKEN_AXISNAME_FOLLOWING:
                return "apos";
            default:
                return null;
        }
    }

    private Attributes extractNamespaces(Attributes attrs) throws SAXException {
        if (attrs == null) {
            return null;
        }
        int length = attrs.getLength();
        AttributesImpl attrsOnly = new AttributesImpl(attrs);
        for (int i = length - 1; i >= 0; i--) {
            String rawName = attrsOnly.getQName(i);
            if (rawName.startsWith("xmlns")) {
                if (rawName.length() == 5) {
                    startPrefixMapping("", attrs.getValue(i));
                    attrsOnly.removeAttribute(i);
                } else if (rawName.charAt(5) == ':') {
                    startPrefixMapping(rawName.substring(6), attrs.getValue(i));
                    attrsOnly.removeAttribute(i);
                }
            }
        }
        return attrsOnly;
    }

    @Override
    protected void printEscaped(String source) throws IOException {
        int length = source.length();
        int i = 0;
        while (i < length) {
            int ch = source.charAt(i);
            if (!XMLChar.isValid(ch)) {
                i++;
                if (i < length) {
                    surrogates(ch, source.charAt(i), false);
                } else {
                    fatalError("The character '" + ((char) ch) + "' is an invalid XML character");
                }
            } else if (ch == 10 || ch == 13 || ch == 9) {
                printHex(ch);
            } else if (ch == 60) {
                this._printer.printText("&lt;");
            } else if (ch == 38) {
                this._printer.printText("&amp;");
            } else if (ch == 34) {
                this._printer.printText("&quot;");
            } else if (ch >= 32 && this._encodingInfo.isPrintable((char) ch)) {
                this._printer.printText((char) ch);
            } else {
                printHex(ch);
            }
            i++;
        }
    }

    protected void printXMLChar(int ch) throws IOException {
        if (ch == 13) {
            printHex(ch);
            return;
        }
        if (ch == 60) {
            this._printer.printText("&lt;");
            return;
        }
        if (ch == 38) {
            this._printer.printText("&amp;");
            return;
        }
        if (ch == 62) {
            this._printer.printText("&gt;");
            return;
        }
        if (ch == 10 || ch == 9 || (ch >= 32 && this._encodingInfo.isPrintable((char) ch))) {
            this._printer.printText((char) ch);
        } else {
            printHex(ch);
        }
    }

    @Override
    protected void printText(String text, boolean preserveSpace, boolean unescaped) throws IOException {
        int length = text.length();
        if (preserveSpace) {
            int index = 0;
            while (index < length) {
                char ch = text.charAt(index);
                if (!XMLChar.isValid(ch)) {
                    index++;
                    if (index < length) {
                        surrogates(ch, text.charAt(index), true);
                    } else {
                        fatalError("The character '" + ch + "' is an invalid XML character");
                    }
                } else if (unescaped) {
                    this._printer.printText(ch);
                } else {
                    printXMLChar(ch);
                }
                index++;
            }
            return;
        }
        int index2 = 0;
        while (index2 < length) {
            char ch2 = text.charAt(index2);
            if (!XMLChar.isValid(ch2)) {
                index2++;
                if (index2 < length) {
                    surrogates(ch2, text.charAt(index2), true);
                } else {
                    fatalError("The character '" + ch2 + "' is an invalid XML character");
                }
            } else if (unescaped) {
                this._printer.printText(ch2);
            } else {
                printXMLChar(ch2);
            }
            index2++;
        }
    }

    @Override
    protected void printText(char[] chars, int start, int start2, boolean preserveSpace, boolean unescaped) throws IOException {
        if (preserveSpace) {
            while (true) {
                int length = start2 - 1;
                if (start2 <= 0) {
                    return;
                }
                int start3 = start + 1;
                char ch = chars[start];
                if (XMLChar.isValid(ch)) {
                    if (unescaped) {
                        this._printer.printText(ch);
                    } else {
                        printXMLChar(ch);
                    }
                    start = start3;
                    start2 = length;
                } else {
                    int length2 = length - 1;
                    if (length > 0) {
                        surrogates(ch, chars[start3], true);
                        start = start3 + 1;
                    } else {
                        fatalError("The character '" + ch + "' is an invalid XML character");
                        start = start3;
                    }
                    start2 = length2;
                }
            }
        } else {
            while (true) {
                int length3 = start2 - 1;
                if (start2 <= 0) {
                    return;
                }
                int start4 = start + 1;
                char ch2 = chars[start];
                if (XMLChar.isValid(ch2)) {
                    if (unescaped) {
                        this._printer.printText(ch2);
                    } else {
                        printXMLChar(ch2);
                    }
                    start = start4;
                    start2 = length3;
                } else {
                    int length4 = length3 - 1;
                    if (length3 > 0) {
                        surrogates(ch2, chars[start4], true);
                        start = start4 + 1;
                    } else {
                        fatalError("The character '" + ch2 + "' is an invalid XML character");
                        start = start4;
                    }
                    start2 = length4;
                }
            }
        }
    }

    @Override
    protected void checkUnboundNamespacePrefixedNode(Node node) throws IOException {
        if (this.fNamespaces) {
            Node child = node.getFirstChild();
            while (child != null) {
                Node next = child.getNextSibling();
                String prefix = child.getPrefix();
                String prefix2 = (prefix == null || prefix.length() == 0) ? XMLSymbols.EMPTY_STRING : this.fSymbolTable.addSymbol(prefix);
                if (this.fNSBinder.getURI(prefix2) == null && prefix2 != null) {
                    fatalError("The replacement text of the entity node '" + node.getNodeName() + "' contains an element node '" + child.getNodeName() + "' with an undeclared prefix '" + prefix2 + "'.");
                }
                if (child.getNodeType() == 1) {
                    NamedNodeMap attrs = child.getAttributes();
                    for (int i = 0; i < attrs.getLength(); i++) {
                        String attrPrefix = attrs.item(i).getPrefix();
                        String attrPrefix2 = (attrPrefix == null || attrPrefix.length() == 0) ? XMLSymbols.EMPTY_STRING : this.fSymbolTable.addSymbol(attrPrefix);
                        if (this.fNSBinder.getURI(attrPrefix2) == null && attrPrefix2 != null) {
                            fatalError("The replacement text of the entity node '" + node.getNodeName() + "' contains an element node '" + child.getNodeName() + "' with an attribute '" + attrs.item(i).getNodeName() + "' an undeclared prefix '" + attrPrefix2 + "'.");
                        }
                    }
                }
                if (child.hasChildNodes()) {
                    checkUnboundNamespacePrefixedNode(child);
                }
                child = next;
            }
        }
    }

    @Override
    public boolean reset() {
        super.reset();
        if (this.fNSBinder != null) {
            this.fNSBinder.reset();
            this.fNSBinder.declarePrefix(XMLSymbols.EMPTY_STRING, XMLSymbols.EMPTY_STRING);
            return true;
        }
        return true;
    }
}
