package mf.org.apache.xml.serialize;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Hashtable;
import java.util.Vector;
import mf.org.apache.xerces.dom.DOMErrorImpl;
import mf.org.apache.xerces.dom.DOMLocatorImpl;
import mf.org.apache.xerces.dom.DOMMessageFormatter;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.w3c.dom.DOMError;
import mf.org.w3c.dom.DOMErrorHandler;
import mf.org.w3c.dom.Document;
import mf.org.w3c.dom.DocumentFragment;
import mf.org.w3c.dom.Element;
import mf.org.w3c.dom.Node;
import mf.org.w3c.dom.ls.LSException;
import mf.org.w3c.dom.ls.LSSerializerFilter;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.DocumentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;

public abstract class BaseMarkupSerializer implements Serializer, ContentHandler, DTDHandler, DocumentHandler, DeclHandler, LexicalHandler {
    protected String _docTypePublicId;
    protected String _docTypeSystemId;
    private int _elementStateCount;
    protected EncodingInfo _encodingInfo;
    protected OutputFormat _format;
    protected boolean _indenting;
    private OutputStream _output;
    private Vector _preRoot;
    protected Hashtable _prefixes;
    private boolean _prepared;
    protected Printer _printer;
    protected boolean _started;
    private Writer _writer;
    protected DOMErrorHandler fDOMErrorHandler;
    protected LSSerializerFilter fDOMFilter;
    protected short features = -1;
    protected final DOMErrorImpl fDOMError = new DOMErrorImpl();
    protected final StringBuffer fStrBuffer = new StringBuffer(40);
    protected Node fCurrentNode = null;
    private ElementState[] _elementStates = new ElementState[10];

    protected abstract String getEntityRef(int i);

    protected abstract void serializeElement(Element element) throws IOException;

    protected BaseMarkupSerializer(OutputFormat format) {
        for (int i = 0; i < this._elementStates.length; i++) {
            this._elementStates[i] = new ElementState();
        }
        this._format = format;
    }

    @Override
    public ContentHandler asContentHandler() throws IOException {
        prepare();
        return this;
    }

    @Override
    public void setOutputByteStream(OutputStream output) {
        if (output == null) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "ArgumentIsNull", new Object[]{"output"});
            throw new NullPointerException(msg);
        }
        this._output = output;
        this._writer = null;
        reset();
    }

    @Override
    public void setOutputCharStream(Writer writer) {
        if (writer == null) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "ArgumentIsNull", new Object[]{"writer"});
            throw new NullPointerException(msg);
        }
        this._writer = writer;
        this._output = null;
        reset();
    }

    public boolean reset() {
        if (this._elementStateCount > 1) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "ResetInMiddle", null);
            throw new IllegalStateException(msg);
        }
        this._prepared = false;
        this.fCurrentNode = null;
        this.fStrBuffer.setLength(0);
        return true;
    }

    protected void cleanup() {
        this.fCurrentNode = null;
    }

    protected void prepare() throws IOException {
        if (this._prepared) {
            return;
        }
        if (this._writer == null && this._output == null) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "NoWriterSupplied", null);
            throw new IOException(msg);
        }
        this._encodingInfo = this._format.getEncodingInfo();
        if (this._output != null) {
            this._writer = this._encodingInfo.getWriter(this._output);
        }
        if (this._format.getIndenting()) {
            this._indenting = true;
            this._printer = new IndentPrinter(this._writer, this._format);
        } else {
            this._indenting = false;
            this._printer = new Printer(this._writer, this._format);
        }
        this._elementStateCount = 0;
        ElementState state = this._elementStates[0];
        state.namespaceURI = null;
        state.localName = null;
        state.rawName = null;
        state.preserveSpace = this._format.getPreserveSpace();
        state.empty = true;
        state.afterElement = false;
        state.afterComment = false;
        state.inCData = false;
        state.doCData = false;
        state.prefixes = null;
        this._docTypePublicId = this._format.getDoctypePublic();
        this._docTypeSystemId = this._format.getDoctypeSystem();
        this._started = false;
        this._prepared = true;
    }

    public void serialize(Element elem) throws IOException {
        reset();
        prepare();
        serializeNode(elem);
        cleanup();
        this._printer.flush();
        if (this._printer.getException() != null) {
            throw this._printer.getException();
        }
    }

    public void serialize(DocumentFragment frag) throws IOException {
        reset();
        prepare();
        serializeNode(frag);
        cleanup();
        this._printer.flush();
        if (this._printer.getException() != null) {
            throw this._printer.getException();
        }
    }

    public void serialize(Document doc) throws IOException {
        reset();
        prepare();
        serializeNode(doc);
        serializePreRoot();
        cleanup();
        this._printer.flush();
        if (this._printer.getException() != null) {
            throw this._printer.getException();
        }
    }

    @Override
    public void startDocument() throws SAXException {
        try {
            prepare();
        } catch (IOException except) {
            throw new SAXException(except.toString());
        }
    }

    @Override
    public void characters(char[] chars, int start, int length) throws SAXException {
        try {
            ElementState state = content();
            if (!state.inCData && !state.doCData) {
                if (state.preserveSpace) {
                    int saveIndent = this._printer.getNextIndent();
                    this._printer.setNextIndent(0);
                    printText(chars, start, length, true, state.unescaped);
                    this._printer.setNextIndent(saveIndent);
                    return;
                }
                printText(chars, start, length, false, state.unescaped);
                return;
            }
            if (!state.inCData) {
                this._printer.printText("<![CDATA[");
                state.inCData = true;
            }
            int saveIndent2 = this._printer.getNextIndent();
            this._printer.setNextIndent(0);
            int end = start + length;
            int index = start;
            while (index < end) {
                char ch = chars[index];
                if (ch == ']' && index + 2 < end && chars[index + 1] == ']' && chars[index + 2] == '>') {
                    this._printer.printText("]]]]><![CDATA[>");
                    index += 2;
                } else if (!XMLChar.isValid(ch)) {
                    index++;
                    if (index < end) {
                        surrogates(ch, chars[index], true);
                    } else {
                        fatalError("The character '" + ch + "' is an invalid XML character");
                    }
                } else if ((ch >= ' ' && this._encodingInfo.isPrintable(ch) && ch != 127) || ch == '\n' || ch == '\r' || ch == '\t') {
                    this._printer.printText(ch);
                } else {
                    this._printer.printText("]]>&#x");
                    this._printer.printText(Integer.toHexString(ch));
                    this._printer.printText(";<![CDATA[");
                }
                index++;
            }
            this._printer.setNextIndent(saveIndent2);
        } catch (IOException except) {
            throw new SAXException(except);
        }
    }

    @Override
    public void ignorableWhitespace(char[] chars, int start, int length) throws SAXException {
        try {
            content();
            if (this._indenting) {
                this._printer.setThisIndent(0);
                int i = start;
                while (true) {
                    int length2 = length - 1;
                    if (length > 0) {
                        try {
                            this._printer.printText(chars[i]);
                            i++;
                            length = length2;
                        } catch (IOException e) {
                            except = e;
                        }
                    } else {
                        return;
                    }
                }
            } else {
                return;
            }
        } catch (IOException e2) {
            except = e2;
        }
        throw new SAXException(except);
    }

    @Override
    public final void processingInstruction(String target, String code) throws SAXException {
        try {
            processingInstructionIO(target, code);
        } catch (IOException except) {
            throw new SAXException(except);
        }
    }

    public void processingInstructionIO(String target, String code) throws IOException {
        ElementState state = content();
        int index = target.indexOf("?>");
        if (index >= 0) {
            StringBuffer stringBuffer = this.fStrBuffer;
            stringBuffer.append("<?");
            stringBuffer.append(target.substring(0, index));
        } else {
            StringBuffer stringBuffer2 = this.fStrBuffer;
            stringBuffer2.append("<?");
            stringBuffer2.append(target);
        }
        if (code != null) {
            this.fStrBuffer.append(' ');
            int index2 = code.indexOf("?>");
            if (index2 >= 0) {
                this.fStrBuffer.append(code.substring(0, index2));
            } else {
                this.fStrBuffer.append(code);
            }
        }
        this.fStrBuffer.append("?>");
        if (isDocumentState()) {
            if (this._preRoot == null) {
                this._preRoot = new Vector();
            }
            this._preRoot.addElement(this.fStrBuffer.toString());
        } else {
            this._printer.indent();
            printText(this.fStrBuffer.toString(), true, true);
            this._printer.unindent();
            if (this._indenting) {
                state.afterElement = true;
            }
        }
        this.fStrBuffer.setLength(0);
    }

    @Override
    public void comment(char[] chars, int start, int length) throws SAXException {
        try {
            comment(new String(chars, start, length));
        } catch (IOException except) {
            throw new SAXException(except);
        }
    }

    public void comment(String text) throws IOException {
        if (this._format.getOmitComments()) {
            return;
        }
        ElementState state = content();
        int index = text.indexOf("-->");
        if (index >= 0) {
            StringBuffer stringBuffer = this.fStrBuffer;
            stringBuffer.append("<!--");
            stringBuffer.append(text.substring(0, index));
            stringBuffer.append("-->");
        } else {
            StringBuffer stringBuffer2 = this.fStrBuffer;
            stringBuffer2.append("<!--");
            stringBuffer2.append(text);
            stringBuffer2.append("-->");
        }
        if (isDocumentState()) {
            if (this._preRoot == null) {
                this._preRoot = new Vector();
            }
            this._preRoot.addElement(this.fStrBuffer.toString());
        } else {
            if (this._indenting && !state.preserveSpace) {
                this._printer.breakLine();
            }
            this._printer.indent();
            printText(this.fStrBuffer.toString(), true, true);
            this._printer.unindent();
            if (this._indenting) {
                state.afterElement = true;
            }
        }
        this.fStrBuffer.setLength(0);
        state.afterComment = true;
        state.afterElement = false;
    }

    @Override
    public void startCDATA() {
        ElementState state = getElementState();
        state.doCData = true;
    }

    @Override
    public void endCDATA() {
        ElementState state = getElementState();
        state.doCData = false;
    }

    @Override
    public void endDocument() throws SAXException {
        try {
            serializePreRoot();
            this._printer.flush();
        } catch (IOException except) {
            throw new SAXException(except);
        }
    }

    @Override
    public void startEntity(String name) {
    }

    @Override
    public void endEntity(String name) {
    }

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        try {
            endCDATA();
            content();
            this._printer.printText('&');
            this._printer.printText(name);
            this._printer.printText(';');
        } catch (IOException except) {
            throw new SAXException(except);
        }
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (this._prefixes == null) {
            this._prefixes = new Hashtable();
        }
        this._prefixes.put(uri, prefix == null ? "" : prefix);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
    }

    @Override
    public final void startDTD(String name, String publicId, String systemId) throws SAXException {
        try {
            this._printer.enterDTD();
            this._docTypePublicId = publicId;
            this._docTypeSystemId = systemId;
        } catch (IOException except) {
            throw new SAXException(except);
        }
    }

    @Override
    public void endDTD() {
    }

    @Override
    public void elementDecl(String name, String model) throws SAXException {
        try {
            this._printer.enterDTD();
            this._printer.printText("<!ELEMENT ");
            this._printer.printText(name);
            this._printer.printText(' ');
            this._printer.printText(model);
            this._printer.printText('>');
            if (this._indenting) {
                this._printer.breakLine();
            }
        } catch (IOException except) {
            throw new SAXException(except);
        }
    }

    @Override
    public void attributeDecl(String eName, String aName, String type, String valueDefault, String value) throws SAXException {
        try {
            this._printer.enterDTD();
            this._printer.printText("<!ATTLIST ");
            this._printer.printText(eName);
            this._printer.printText(' ');
            this._printer.printText(aName);
            this._printer.printText(' ');
            this._printer.printText(type);
            if (valueDefault != null) {
                this._printer.printText(' ');
                this._printer.printText(valueDefault);
            }
            if (value != null) {
                this._printer.printText(" \"");
                printEscaped(value);
                this._printer.printText('\"');
            }
            this._printer.printText('>');
            if (this._indenting) {
                this._printer.breakLine();
            }
        } catch (IOException except) {
            throw new SAXException(except);
        }
    }

    @Override
    public void internalEntityDecl(String name, String value) throws SAXException {
        try {
            this._printer.enterDTD();
            this._printer.printText("<!ENTITY ");
            this._printer.printText(name);
            this._printer.printText(" \"");
            printEscaped(value);
            this._printer.printText("\">");
            if (this._indenting) {
                this._printer.breakLine();
            }
        } catch (IOException except) {
            throw new SAXException(except);
        }
    }

    @Override
    public void externalEntityDecl(String name, String publicId, String systemId) throws SAXException {
        try {
            this._printer.enterDTD();
            unparsedEntityDecl(name, publicId, systemId, null);
        } catch (IOException except) {
            throw new SAXException(except);
        }
    }

    @Override
    public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName) throws SAXException {
        try {
            this._printer.enterDTD();
            if (publicId == null) {
                this._printer.printText("<!ENTITY ");
                this._printer.printText(name);
                this._printer.printText(" SYSTEM ");
                printDoctypeURL(systemId);
            } else {
                this._printer.printText("<!ENTITY ");
                this._printer.printText(name);
                this._printer.printText(" PUBLIC ");
                printDoctypeURL(publicId);
                this._printer.printText(' ');
                printDoctypeURL(systemId);
            }
            if (notationName != null) {
                this._printer.printText(" NDATA ");
                this._printer.printText(notationName);
            }
            this._printer.printText('>');
            if (this._indenting) {
                this._printer.breakLine();
            }
        } catch (IOException except) {
            throw new SAXException(except);
        }
    }

    @Override
    public void notationDecl(String name, String publicId, String systemId) throws SAXException {
        try {
            this._printer.enterDTD();
            if (publicId != null) {
                this._printer.printText("<!NOTATION ");
                this._printer.printText(name);
                this._printer.printText(" PUBLIC ");
                printDoctypeURL(publicId);
                if (systemId != null) {
                    this._printer.printText(' ');
                    printDoctypeURL(systemId);
                }
            } else {
                this._printer.printText("<!NOTATION ");
                this._printer.printText(name);
                this._printer.printText(" SYSTEM ");
                printDoctypeURL(systemId);
            }
            this._printer.printText('>');
            if (this._indenting) {
                this._printer.breakLine();
            }
        } catch (IOException except) {
            throw new SAXException(except);
        }
    }

    protected void serializeNode(mf.org.w3c.dom.Node r10) throws java.io.IOException {
        r9.fCurrentNode = r10;
        switch (r10.getNodeType()) {
            case 1:
                if (r9.fDOMFilter != null && (r9.fDOMFilter.getWhatToShow() & 1) != 0) {
                    r0 = r9.fDOMFilter.acceptNode(r10);
                    switch (r0) {
                        case 3:
                            r1 = r10.getFirstChild();
                            while (r1 != null) {
                                serializeNode(r1);
                                r1 = r1.getNextSibling();
                            }
                            break;
                    }
                    return;
                }
                serializeElement((mf.org.w3c.dom.Element) r10);
                return;
            case 2:
            case 6:
            case 10:
            default:
                return;
            case 3:
                r0 = r10.getNodeValue();
                if (r0 != null) {
                    if (r9.fDOMFilter == null || (r9.fDOMFilter.getWhatToShow() & 4) == 0) {
                        if (!r9._indenting || getElementState().preserveSpace || r0.replace('\n', ' ').trim().length() != 0) {
                            characters(r0);
                            return;
                        } else {
                            return;
                        }
                    } else {
                        r1 = r9.fDOMFilter.acceptNode(r10);
                        switch (r1) {
                            case 2:
                            case 3:
                                break;
                            default:
                                characters(r0);
                                break;
                        }
                        return;
                    }
                } else {
                    return;
                }
            case 4:
                r0 = r10.getNodeValue();
                if ((r9.features & 8) != 0) {
                    if (r0 != null) {
                        if (r9.fDOMFilter != null && (r9.fDOMFilter.getWhatToShow() & 8) != 0) {
                            r1 = r9.fDOMFilter.acceptNode(r10);
                            switch (r1) {
                            }
                            return;
                        }
                        startCDATA();
                        characters(r0);
                        endCDATA();
                        return;
                    } else {
                        return;
                    }
                } else {
                    characters(r0);
                    return;
                }
            case 5:
                endCDATA();
                content();
                if ((r9.features & 4) != 0 || r10.getFirstChild() == null) {
                    if (r9.fDOMFilter != null && (r9.fDOMFilter.getWhatToShow() & 16) != 0) {
                        r0 = r9.fDOMFilter.acceptNode(r10);
                        switch (r0) {
                            case 3:
                                r1 = r10.getFirstChild();
                                while (r1 != null) {
                                    serializeNode(r1);
                                    r1 = r1.getNextSibling();
                                }
                                break;
                        }
                        return;
                    }
                    checkUnboundNamespacePrefixedNode(r10);
                    r9._printer.printText("&");
                    r9._printer.printText(r10.getNodeName());
                    r9._printer.printText(";");
                    return;
                } else {
                    r0 = r10.getFirstChild();
                    while (r0 != null) {
                        serializeNode(r0);
                        r0 = r0.getNextSibling();
                    }
                    return;
                }
            case 7:
                if (r9.fDOMFilter != null && (r9.fDOMFilter.getWhatToShow() & 64) != 0) {
                    r0 = r9.fDOMFilter.acceptNode(r10);
                    switch (r0) {
                    }
                    return;
                }
                processingInstructionIO(r10.getNodeName(), r10.getNodeValue());
                return;
            case 8:
                if (r9._format.getOmitComments() || (r0 = r10.getNodeValue()) == null) {
                    return;
                } else {
                    if (r9.fDOMFilter != null && (r9.fDOMFilter.getWhatToShow() & 128) != 0) {
                        r1 = r9.fDOMFilter.acceptNode(r10);
                        switch (r1) {
                        }
                        return;
                    }
                    comment(r0);
                    return;
                }
            case 9:
                r0 = ((mf.org.w3c.dom.Document) r10).getDoctype();
                if (r0 != null) {
                    try {
                        r9._printer.enterDTD();
                        r9._docTypePublicId = r0.getPublicId();
                        r9._docTypeSystemId = r0.getSystemId();
                        r1 = r0.getInternalSubset();
                        if (r1 != null && r1.length() > 0) {
                            r9._printer.printText(r1);
                        }
                        endDTD();
                    } catch (java.lang.NoSuchMethodError e) {
                        r2 = r0.getClass();
                        r3 = null;
                        r5 = null;
                        try {
                            r6 = r2.getMethod("getPublicId", null);
                            if (r6.getReturnType().equals(java.lang.String.class)) {
                                r3 = (java.lang.String) r6.invoke(r0, null);
                            }
                        } catch (java.lang.Exception e) {
                        }
                        try {
                            r6 = r2.getMethod("getSystemId", null);
                            if (r6.getReturnType().equals(java.lang.String.class)) {
                                r5 = (java.lang.String) r6.invoke(r0, null);
                            }
                        } catch (java.lang.Exception e) {
                        }
                        r9._printer.enterDTD();
                        r9._docTypePublicId = r3;
                        r9._docTypeSystemId = r5;
                        endDTD();
                    }
                }
            case 11:
        }
        r0 = r10.getFirstChild();
        while (r0 != null) {
            serializeNode(r0);
            r0 = r0.getNextSibling();
        }
        return;
    }

    protected ElementState content() throws IOException {
        ElementState state = getElementState();
        if (!isDocumentState()) {
            if (state.inCData && !state.doCData) {
                this._printer.printText("]]>");
                state.inCData = false;
            }
            if (state.empty) {
                this._printer.printText('>');
                state.empty = false;
            }
            state.afterElement = false;
            state.afterComment = false;
        }
        return state;
    }

    protected void characters(String text) throws IOException {
        ElementState state = content();
        if (state.inCData || state.doCData) {
            if (!state.inCData) {
                this._printer.printText("<![CDATA[");
                state.inCData = true;
            }
            int saveIndent = this._printer.getNextIndent();
            this._printer.setNextIndent(0);
            printCDATAText(text);
            this._printer.setNextIndent(saveIndent);
            return;
        }
        if (state.preserveSpace) {
            int saveIndent2 = this._printer.getNextIndent();
            this._printer.setNextIndent(0);
            printText(text, true, state.unescaped);
            this._printer.setNextIndent(saveIndent2);
            return;
        }
        printText(text, false, state.unescaped);
    }

    protected void serializePreRoot() throws IOException {
        if (this._preRoot != null) {
            for (int i = 0; i < this._preRoot.size(); i++) {
                printText((String) this._preRoot.elementAt(i), true, true);
                if (this._indenting) {
                    this._printer.breakLine();
                }
            }
            this._preRoot.removeAllElements();
        }
    }

    protected void printCDATAText(String text) throws IOException {
        int length = text.length();
        int index = 0;
        while (index < length) {
            char ch = text.charAt(index);
            if (ch == ']' && index + 2 < length && text.charAt(index + 1) == ']' && text.charAt(index + 2) == '>') {
                if (this.fDOMErrorHandler != null) {
                    if ((this.features & 16) == 0) {
                        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "EndingCDATA", null);
                        if ((this.features & 2) != 0) {
                            modifyDOMError(msg, (short) 3, "wf-invalid-character", this.fCurrentNode);
                            this.fDOMErrorHandler.handleError(this.fDOMError);
                            throw new LSException((short) 82, msg);
                        }
                        modifyDOMError(msg, (short) 2, "cdata-section-not-splitted", this.fCurrentNode);
                        if (!this.fDOMErrorHandler.handleError(this.fDOMError)) {
                            throw new LSException((short) 82, msg);
                        }
                    } else {
                        modifyDOMError(DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "SplittingCDATA", null), (short) 1, null, this.fCurrentNode);
                        this.fDOMErrorHandler.handleError(this.fDOMError);
                    }
                }
                this._printer.printText("]]]]><![CDATA[>");
                index += 2;
            } else if (!XMLChar.isValid(ch)) {
                index++;
                if (index < length) {
                    surrogates(ch, text.charAt(index), true);
                } else {
                    fatalError("The character '" + ch + "' is an invalid XML character");
                }
            } else if ((ch >= ' ' && this._encodingInfo.isPrintable(ch) && ch != 127) || ch == '\n' || ch == '\r' || ch == '\t') {
                this._printer.printText(ch);
            } else {
                this._printer.printText("]]>&#x");
                this._printer.printText(Integer.toHexString(ch));
                this._printer.printText(";<![CDATA[");
            }
            index++;
        }
    }

    protected void surrogates(int high, int low, boolean inContent) throws IOException {
        if (XMLChar.isHighSurrogate(high)) {
            if (!XMLChar.isLowSurrogate(low)) {
                fatalError("The character '" + ((char) low) + "' is an invalid XML character");
                return;
            }
            int supplemental = XMLChar.supplemental((char) high, (char) low);
            if (!XMLChar.isValid(supplemental)) {
                fatalError("The character '" + ((char) supplemental) + "' is an invalid XML character");
                return;
            }
            if (inContent && content().inCData) {
                this._printer.printText("]]>&#x");
                this._printer.printText(Integer.toHexString(supplemental));
                this._printer.printText(";<![CDATA[");
                return;
            }
            printHex(supplemental);
            return;
        }
        fatalError("The character '" + ((char) high) + "' is an invalid XML character");
    }

    protected void printText(char[] chars, int start, int length, boolean preserveSpace, boolean unescaped) throws IOException {
        if (preserveSpace) {
            while (true) {
                int length2 = length - 1;
                if (length > 0) {
                    char ch = chars[start];
                    start++;
                    if (ch == '\n' || ch == '\r' || unescaped) {
                        this._printer.printText(ch);
                    } else {
                        printEscaped(ch);
                    }
                    length = length2;
                } else {
                    return;
                }
            }
        } else {
            while (true) {
                int length3 = length - 1;
                if (length > 0) {
                    char ch2 = chars[start];
                    start++;
                    if (ch2 == ' ' || ch2 == '\f' || ch2 == '\t' || ch2 == '\n' || ch2 == '\r') {
                        this._printer.printSpace();
                    } else if (unescaped) {
                        this._printer.printText(ch2);
                    } else {
                        printEscaped(ch2);
                    }
                    length = length3;
                } else {
                    return;
                }
            }
        }
    }

    protected void printText(String text, boolean preserveSpace, boolean unescaped) throws IOException {
        if (preserveSpace) {
            for (int index = 0; index < text.length(); index++) {
                char ch = text.charAt(index);
                if (ch == '\n' || ch == '\r' || unescaped) {
                    this._printer.printText(ch);
                } else {
                    printEscaped(ch);
                }
            }
            return;
        }
        for (int index2 = 0; index2 < text.length(); index2++) {
            char ch2 = text.charAt(index2);
            if (ch2 == ' ' || ch2 == '\f' || ch2 == '\t' || ch2 == '\n' || ch2 == '\r') {
                this._printer.printSpace();
            } else if (unescaped) {
                this._printer.printText(ch2);
            } else {
                printEscaped(ch2);
            }
        }
    }

    protected void printDoctypeURL(String url) throws IOException {
        this._printer.printText('\"');
        for (int i = 0; i < url.length(); i++) {
            if (url.charAt(i) == '\"' || url.charAt(i) < ' ' || url.charAt(i) > 127) {
                this._printer.printText('%');
                this._printer.printText(Integer.toHexString(url.charAt(i)));
            } else {
                this._printer.printText(url.charAt(i));
            }
        }
        this._printer.printText('\"');
    }

    protected void printEscaped(int ch) throws IOException {
        String charRef = getEntityRef(ch);
        if (charRef != null) {
            this._printer.printText('&');
            this._printer.printText(charRef);
            this._printer.printText(';');
        } else {
            if ((ch >= 32 && this._encodingInfo.isPrintable((char) ch) && ch != 127) || ch == 10 || ch == 13 || ch == 9) {
                if (ch >= 65536) {
                    this._printer.printText((char) (((ch - 65536) >> 10) + 55296));
                    this._printer.printText((char) (((ch - 65536) & 1023) + 56320));
                    return;
                } else {
                    this._printer.printText((char) ch);
                    return;
                }
            }
            printHex(ch);
        }
    }

    final void printHex(int ch) throws IOException {
        this._printer.printText("&#x");
        this._printer.printText(Integer.toHexString(ch));
        this._printer.printText(';');
    }

    protected void printEscaped(String source) throws IOException {
        int i = 0;
        while (i < source.length()) {
            int ch = source.charAt(i);
            if ((ch & 64512) == 55296 && i + 1 < source.length()) {
                int lowch = source.charAt(i + 1);
                if ((64512 & lowch) == 56320) {
                    ch = ((65536 + ((ch - 55296) << 10)) + lowch) - 56320;
                    i++;
                }
            }
            printEscaped(ch);
            i++;
        }
    }

    protected ElementState getElementState() {
        return this._elementStates[this._elementStateCount];
    }

    protected ElementState enterElementState(String namespaceURI, String localName, String rawName, boolean preserveSpace) {
        if (this._elementStateCount + 1 == this._elementStates.length) {
            ElementState[] newStates = new ElementState[this._elementStates.length + 10];
            for (int i = 0; i < this._elementStates.length; i++) {
                newStates[i] = this._elementStates[i];
            }
            for (int i2 = this._elementStates.length; i2 < newStates.length; i2++) {
                newStates[i2] = new ElementState();
            }
            this._elementStates = newStates;
        }
        this._elementStateCount++;
        ElementState state = this._elementStates[this._elementStateCount];
        state.namespaceURI = namespaceURI;
        state.localName = localName;
        state.rawName = rawName;
        state.preserveSpace = preserveSpace;
        state.empty = true;
        state.afterElement = false;
        state.afterComment = false;
        state.inCData = false;
        state.doCData = false;
        state.unescaped = false;
        state.prefixes = this._prefixes;
        this._prefixes = null;
        return state;
    }

    protected ElementState leaveElementState() {
        if (this._elementStateCount > 0) {
            this._prefixes = null;
            this._elementStateCount--;
            return this._elementStates[this._elementStateCount];
        }
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "Internal", null);
        throw new IllegalStateException(msg);
    }

    protected boolean isDocumentState() {
        return this._elementStateCount == 0;
    }

    final void clearDocumentState() {
        this._elementStateCount = 0;
    }

    protected String getPrefix(String namespaceURI) {
        String prefix;
        String prefix2;
        if (this._prefixes != null && (prefix2 = (String) this._prefixes.get(namespaceURI)) != null) {
            return prefix2;
        }
        if (this._elementStateCount == 0) {
            return null;
        }
        for (int i = this._elementStateCount; i > 0; i--) {
            if (this._elementStates[i].prefixes != null && (prefix = (String) this._elementStates[i].prefixes.get(namespaceURI)) != null) {
                return prefix;
            }
        }
        return null;
    }

    protected DOMError modifyDOMError(String message, short severity, String type, Node node) {
        this.fDOMError.reset();
        this.fDOMError.fMessage = message;
        this.fDOMError.fType = type;
        this.fDOMError.fSeverity = severity;
        this.fDOMError.fLocator = new DOMLocatorImpl(-1, -1, -1, node, null);
        return this.fDOMError;
    }

    protected void fatalError(String message) throws IOException {
        if (this.fDOMErrorHandler != null) {
            modifyDOMError(message, (short) 3, null, this.fCurrentNode);
            this.fDOMErrorHandler.handleError(this.fDOMError);
            return;
        }
        throw new IOException(message);
    }

    protected void checkUnboundNamespacePrefixedNode(Node node) throws IOException {
    }
}
