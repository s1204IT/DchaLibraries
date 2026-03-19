package org.apache.xml.serializer;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;
import org.apache.xalan.templates.Constants;
import org.apache.xml.dtm.DTMFilter;
import org.apache.xml.serializer.utils.Utils;
import org.apache.xpath.axes.WalkerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ToHTMLStream extends ToStream {
    private static final ElemDesc m_dummy;
    static final Trie m_elementFlags = new Trie();
    protected boolean m_inDTD = false;
    private boolean m_inBlockElem = false;
    private final CharInfo m_htmlcharInfo = CharInfo.getCharInfo(CharInfo.HTML_ENTITIES_RESOURCE, "html");
    private boolean m_specialEscapeURLs = true;
    private boolean m_omitMetaTag = false;
    private Trie m_htmlInfo = new Trie(m_elementFlags);

    static {
        initTagReference(m_elementFlags);
        m_dummy = new ElemDesc(8);
    }

    static void initTagReference(Trie trie) {
        trie.put("BASEFONT", new ElemDesc(2));
        trie.put("FRAME", new ElemDesc(10));
        trie.put("FRAMESET", new ElemDesc(8));
        trie.put("NOFRAMES", new ElemDesc(8));
        trie.put("ISINDEX", new ElemDesc(10));
        trie.put("APPLET", new ElemDesc(WalkerFactory.BIT_NAMESPACE));
        trie.put("CENTER", new ElemDesc(8));
        trie.put("DIR", new ElemDesc(8));
        trie.put("MENU", new ElemDesc(8));
        trie.put("TT", new ElemDesc(4096));
        trie.put("I", new ElemDesc(4096));
        trie.put("B", new ElemDesc(4096));
        trie.put("BIG", new ElemDesc(4096));
        trie.put("SMALL", new ElemDesc(4096));
        trie.put("EM", new ElemDesc(WalkerFactory.BIT_ANCESTOR));
        trie.put("STRONG", new ElemDesc(WalkerFactory.BIT_ANCESTOR));
        trie.put("DFN", new ElemDesc(WalkerFactory.BIT_ANCESTOR));
        trie.put("CODE", new ElemDesc(WalkerFactory.BIT_ANCESTOR));
        trie.put("SAMP", new ElemDesc(WalkerFactory.BIT_ANCESTOR));
        trie.put("KBD", new ElemDesc(WalkerFactory.BIT_ANCESTOR));
        trie.put("VAR", new ElemDesc(WalkerFactory.BIT_ANCESTOR));
        trie.put("CITE", new ElemDesc(WalkerFactory.BIT_ANCESTOR));
        trie.put("ABBR", new ElemDesc(WalkerFactory.BIT_ANCESTOR));
        trie.put("ACRONYM", new ElemDesc(WalkerFactory.BIT_ANCESTOR));
        trie.put("SUP", new ElemDesc(98304));
        trie.put("SUB", new ElemDesc(98304));
        trie.put("SPAN", new ElemDesc(98304));
        trie.put("BDO", new ElemDesc(98304));
        trie.put("BR", new ElemDesc(98314));
        trie.put("BODY", new ElemDesc(8));
        trie.put("ADDRESS", new ElemDesc(56));
        trie.put("DIV", new ElemDesc(56));
        trie.put("A", new ElemDesc(WalkerFactory.BIT_ATTRIBUTE));
        trie.put("MAP", new ElemDesc(98312));
        trie.put("AREA", new ElemDesc(10));
        trie.put("LINK", new ElemDesc(131082));
        trie.put("IMG", new ElemDesc(2195458));
        trie.put("OBJECT", new ElemDesc(2326528));
        trie.put("PARAM", new ElemDesc(2));
        trie.put("HR", new ElemDesc(58));
        trie.put("P", new ElemDesc(56));
        trie.put("H1", new ElemDesc(262152));
        trie.put("H2", new ElemDesc(262152));
        trie.put("H3", new ElemDesc(262152));
        trie.put("H4", new ElemDesc(262152));
        trie.put("H5", new ElemDesc(262152));
        trie.put("H6", new ElemDesc(262152));
        trie.put("PRE", new ElemDesc(1048584));
        trie.put("Q", new ElemDesc(98304));
        trie.put("BLOCKQUOTE", new ElemDesc(56));
        trie.put("INS", new ElemDesc(0));
        trie.put("DEL", new ElemDesc(0));
        trie.put("DL", new ElemDesc(56));
        trie.put("DT", new ElemDesc(8));
        trie.put("DD", new ElemDesc(8));
        trie.put("OL", new ElemDesc(524296));
        trie.put("UL", new ElemDesc(524296));
        trie.put("LI", new ElemDesc(8));
        trie.put("FORM", new ElemDesc(8));
        trie.put("LABEL", new ElemDesc(WalkerFactory.BIT_ANCESTOR_OR_SELF));
        trie.put("INPUT", new ElemDesc(18434));
        trie.put("SELECT", new ElemDesc(18432));
        trie.put("OPTGROUP", new ElemDesc(0));
        trie.put("OPTION", new ElemDesc(0));
        trie.put("TEXTAREA", new ElemDesc(18432));
        trie.put("FIELDSET", new ElemDesc(24));
        trie.put("LEGEND", new ElemDesc(0));
        trie.put("BUTTON", new ElemDesc(18432));
        trie.put("TABLE", new ElemDesc(56));
        trie.put("CAPTION", new ElemDesc(8));
        trie.put("THEAD", new ElemDesc(8));
        trie.put("TFOOT", new ElemDesc(8));
        trie.put("TBODY", new ElemDesc(8));
        trie.put("COLGROUP", new ElemDesc(8));
        trie.put("COL", new ElemDesc(10));
        trie.put("TR", new ElemDesc(8));
        trie.put("TH", new ElemDesc(0));
        trie.put("TD", new ElemDesc(0));
        trie.put("HEAD", new ElemDesc(4194312));
        trie.put("TITLE", new ElemDesc(8));
        trie.put("BASE", new ElemDesc(10));
        trie.put("META", new ElemDesc(131082));
        trie.put("STYLE", new ElemDesc(131336));
        trie.put("SCRIPT", new ElemDesc(229632));
        trie.put("NOSCRIPT", new ElemDesc(56));
        trie.put("HTML", new ElemDesc(8388616));
        trie.put("FONT", new ElemDesc(4096));
        trie.put("S", new ElemDesc(4096));
        trie.put("STRIKE", new ElemDesc(4096));
        trie.put("U", new ElemDesc(4096));
        trie.put("NOBR", new ElemDesc(4096));
        trie.put("IFRAME", new ElemDesc(56));
        trie.put("LAYER", new ElemDesc(56));
        trie.put("ILAYER", new ElemDesc(56));
        ElemDesc elemDesc = (ElemDesc) trie.get("a");
        elemDesc.setAttr("HREF", 2);
        elemDesc.setAttr("NAME", 2);
        ElemDesc elemDesc2 = (ElemDesc) trie.get("area");
        elemDesc2.setAttr("HREF", 2);
        elemDesc2.setAttr("NOHREF", 4);
        ((ElemDesc) trie.get("base")).setAttr("HREF", 2);
        ((ElemDesc) trie.get("button")).setAttr("DISABLED", 4);
        ((ElemDesc) trie.get("blockquote")).setAttr("CITE", 2);
        ((ElemDesc) trie.get("del")).setAttr("CITE", 2);
        ((ElemDesc) trie.get("dir")).setAttr("COMPACT", 4);
        ElemDesc elemDesc3 = (ElemDesc) trie.get("div");
        elemDesc3.setAttr("SRC", 2);
        elemDesc3.setAttr("NOWRAP", 4);
        ((ElemDesc) trie.get("dl")).setAttr("COMPACT", 4);
        ((ElemDesc) trie.get("form")).setAttr("ACTION", 2);
        ElemDesc elemDesc4 = (ElemDesc) trie.get("frame");
        elemDesc4.setAttr("SRC", 2);
        elemDesc4.setAttr("LONGDESC", 2);
        elemDesc4.setAttr("NORESIZE", 4);
        ((ElemDesc) trie.get("head")).setAttr("PROFILE", 2);
        ((ElemDesc) trie.get("hr")).setAttr("NOSHADE", 4);
        ElemDesc elemDesc5 = (ElemDesc) trie.get("iframe");
        elemDesc5.setAttr("SRC", 2);
        elemDesc5.setAttr("LONGDESC", 2);
        ((ElemDesc) trie.get("ilayer")).setAttr("SRC", 2);
        ElemDesc elemDesc6 = (ElemDesc) trie.get("img");
        elemDesc6.setAttr("SRC", 2);
        elemDesc6.setAttr("LONGDESC", 2);
        elemDesc6.setAttr("USEMAP", 2);
        elemDesc6.setAttr("ISMAP", 4);
        ElemDesc elemDesc7 = (ElemDesc) trie.get("input");
        elemDesc7.setAttr("SRC", 2);
        elemDesc7.setAttr("USEMAP", 2);
        elemDesc7.setAttr("CHECKED", 4);
        elemDesc7.setAttr("DISABLED", 4);
        elemDesc7.setAttr("ISMAP", 4);
        elemDesc7.setAttr("READONLY", 4);
        ((ElemDesc) trie.get("ins")).setAttr("CITE", 2);
        ((ElemDesc) trie.get("layer")).setAttr("SRC", 2);
        ((ElemDesc) trie.get("link")).setAttr("HREF", 2);
        ((ElemDesc) trie.get("menu")).setAttr("COMPACT", 4);
        ElemDesc elemDesc8 = (ElemDesc) trie.get("object");
        elemDesc8.setAttr("CLASSID", 2);
        elemDesc8.setAttr("CODEBASE", 2);
        elemDesc8.setAttr("DATA", 2);
        elemDesc8.setAttr("ARCHIVE", 2);
        elemDesc8.setAttr("USEMAP", 2);
        elemDesc8.setAttr("DECLARE", 4);
        ((ElemDesc) trie.get("ol")).setAttr("COMPACT", 4);
        ((ElemDesc) trie.get("optgroup")).setAttr("DISABLED", 4);
        ElemDesc elemDesc9 = (ElemDesc) trie.get("option");
        elemDesc9.setAttr("SELECTED", 4);
        elemDesc9.setAttr("DISABLED", 4);
        ((ElemDesc) trie.get("q")).setAttr("CITE", 2);
        ElemDesc elemDesc10 = (ElemDesc) trie.get(Constants.ELEMNAME_SCRIPT_STRING);
        elemDesc10.setAttr("SRC", 2);
        elemDesc10.setAttr("FOR", 2);
        elemDesc10.setAttr("DEFER", 4);
        ElemDesc elemDesc11 = (ElemDesc) trie.get(Constants.ATTRNAME_SELECT);
        elemDesc11.setAttr("DISABLED", 4);
        elemDesc11.setAttr("MULTIPLE", 4);
        ((ElemDesc) trie.get("table")).setAttr("NOWRAP", 4);
        ((ElemDesc) trie.get("td")).setAttr("NOWRAP", 4);
        ElemDesc elemDesc12 = (ElemDesc) trie.get("textarea");
        elemDesc12.setAttr("DISABLED", 4);
        elemDesc12.setAttr("READONLY", 4);
        ((ElemDesc) trie.get("th")).setAttr("NOWRAP", 4);
        ((ElemDesc) trie.get("tr")).setAttr("NOWRAP", 4);
        ((ElemDesc) trie.get("ul")).setAttr("COMPACT", 4);
    }

    public void setSpecialEscapeURLs(boolean z) {
        this.m_specialEscapeURLs = z;
    }

    public void setOmitMetaTag(boolean z) {
        this.m_omitMetaTag = z;
    }

    @Override
    public void setOutputFormat(Properties properties) {
        if (properties.getProperty(OutputPropertiesFactory.S_USE_URL_ESCAPING) != null) {
            this.m_specialEscapeURLs = OutputPropertyUtils.getBooleanProperty(OutputPropertiesFactory.S_USE_URL_ESCAPING, properties);
        }
        if (properties.getProperty(OutputPropertiesFactory.S_OMIT_META_TAG) != null) {
            this.m_omitMetaTag = OutputPropertyUtils.getBooleanProperty(OutputPropertiesFactory.S_OMIT_META_TAG, properties);
        }
        super.setOutputFormat(properties);
    }

    private final boolean getSpecialEscapeURLs() {
        return this.m_specialEscapeURLs;
    }

    private final boolean getOmitMetaTag() {
        return this.m_omitMetaTag;
    }

    public static final ElemDesc getElemDesc(String str) {
        Object obj = m_elementFlags.get(str);
        if (obj != null) {
            return (ElemDesc) obj;
        }
        return m_dummy;
    }

    private ElemDesc getElemDesc2(String str) {
        Object obj = this.m_htmlInfo.get2(str);
        if (obj != null) {
            return (ElemDesc) obj;
        }
        return m_dummy;
    }

    public ToHTMLStream() {
        this.m_doIndent = true;
        this.m_charInfo = this.m_htmlcharInfo;
        this.m_prefixMap = new NamespaceMappings();
    }

    @Override
    protected void startDocumentInternal() throws SAXException {
        super.startDocumentInternal();
        this.m_needToCallStartDocument = false;
        this.m_needToOutputDocTypeDecl = true;
        this.m_startNewLine = false;
        setOmitXMLDeclaration(true);
    }

    private void outputDocTypeDecl(String str) throws SAXException {
        if (true == this.m_needToOutputDocTypeDecl) {
            String doctypeSystem = getDoctypeSystem();
            String doctypePublic = getDoctypePublic();
            if (doctypeSystem != null || doctypePublic != null) {
                Writer writer = this.m_writer;
                try {
                    writer.write("<!DOCTYPE ");
                    writer.write(str);
                    if (doctypePublic != null) {
                        writer.write(" PUBLIC \"");
                        writer.write(doctypePublic);
                        writer.write(34);
                    }
                    if (doctypeSystem != null) {
                        if (doctypePublic == null) {
                            writer.write(" SYSTEM \"");
                        } else {
                            writer.write(" \"");
                        }
                        writer.write(doctypeSystem);
                        writer.write(34);
                    }
                    writer.write(62);
                    outputLineSep();
                } catch (IOException e) {
                    throw new SAXException(e);
                }
            }
        }
        this.m_needToOutputDocTypeDecl = false;
    }

    @Override
    public final void endDocument() throws SAXException {
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

    @Override
    public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
        ElemContext elemContext = this.m_elemContext;
        if (elemContext.m_startTagOpen) {
            closeStartTag();
            elemContext.m_startTagOpen = false;
        } else if (this.m_cdataTagOpen) {
            closeCDATA();
            this.m_cdataTagOpen = false;
        } else if (this.m_needToCallStartDocument) {
            startDocumentInternal();
            this.m_needToCallStartDocument = false;
        }
        if (this.m_needToOutputDocTypeDecl) {
            outputDocTypeDecl((str3 == null || str3.length() == 0) ? str2 : str3);
        }
        if (str != null && str.length() > 0) {
            super.startElement(str, str2, str3, attributes);
            return;
        }
        try {
            ElemDesc elemDesc2 = getElemDesc2(str3);
            int flags = elemDesc2.getFlags();
            boolean z = true;
            if (this.m_doIndent) {
                boolean z2 = (flags & 8) != 0;
                if (this.m_ispreserve) {
                    this.m_ispreserve = false;
                } else if (elemContext.m_elementName != null && (!this.m_inBlockElem || z2)) {
                    this.m_startNewLine = true;
                    indent();
                }
                this.m_inBlockElem = !z2;
            }
            if (attributes != null) {
                addAttributes(attributes);
            }
            this.m_isprevtext = false;
            Writer writer = this.m_writer;
            writer.write(60);
            writer.write(str3);
            if (this.m_tracer != null) {
                firePseudoAttributes();
            }
            if ((flags & 2) != 0) {
                this.m_elemContext = elemContext.push();
                this.m_elemContext.m_elementName = str3;
                this.m_elemContext.m_elementDesc = elemDesc2;
                return;
            }
            ElemContext elemContextPush = elemContext.push(str, str2, str3);
            this.m_elemContext = elemContextPush;
            elemContextPush.m_elementDesc = elemDesc2;
            if ((flags & DTMFilter.SHOW_DOCUMENT) == 0) {
                z = false;
            }
            elemContextPush.m_isRaw = z;
            if ((4194304 & flags) != 0) {
                closeStartTag();
                elemContextPush.m_startTagOpen = false;
                if (!this.m_omitMetaTag) {
                    if (this.m_doIndent) {
                        indent();
                    }
                    writer.write("<META http-equiv=\"Content-Type\" content=\"text/html; charset=");
                    writer.write(Encodings.getMimeEncoding(getEncoding()));
                    writer.write("\">");
                }
            }
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public final void endElement(String str, String str2, String str3) throws SAXException {
        boolean z;
        if (this.m_cdataTagOpen) {
            closeCDATA();
        }
        if (str != null && str.length() > 0) {
            super.endElement(str, str2, str3);
            return;
        }
        try {
            ElemContext elemContext = this.m_elemContext;
            int flags = elemContext.m_elementDesc.getFlags();
            boolean z2 = (flags & 2) != 0;
            if (this.m_doIndent) {
                boolean z3 = (flags & 8) != 0;
                if (this.m_ispreserve) {
                    this.m_ispreserve = false;
                } else {
                    if (this.m_doIndent && (!this.m_inBlockElem || z3)) {
                        this.m_startNewLine = true;
                        z = true;
                    }
                    if (!elemContext.m_startTagOpen && z) {
                        indent(elemContext.m_currentElemDepth - 1);
                    }
                    this.m_inBlockElem = !z3;
                }
                z = false;
                if (!elemContext.m_startTagOpen) {
                    indent(elemContext.m_currentElemDepth - 1);
                }
                this.m_inBlockElem = !z3;
            }
            Writer writer = this.m_writer;
            if (!elemContext.m_startTagOpen) {
                writer.write("</");
                writer.write(str3);
                writer.write(62);
            } else {
                if (this.m_tracer != null) {
                    super.fireStartElem(str3);
                }
                int length = this.m_attributes.getLength();
                if (length > 0) {
                    processAttributes(this.m_writer, length);
                    this.m_attributes.clear();
                }
                if (!z2) {
                    writer.write("></");
                    writer.write(str3);
                    writer.write(62);
                } else {
                    writer.write(62);
                }
            }
            if ((flags & WalkerFactory.BIT_NAMESPACE) != 0) {
                this.m_ispreserve = true;
            }
            this.m_isprevtext = false;
            if (this.m_tracer != null) {
                super.fireEndElem(str3);
            }
            if (z2) {
                this.m_elemContext = elemContext.m_prev;
                return;
            }
            if (!elemContext.m_startTagOpen && this.m_doIndent && !this.m_preserves.isEmpty()) {
                this.m_preserves.pop();
            }
            this.m_elemContext = elemContext.m_prev;
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    protected void processAttribute(Writer writer, String str, String str2, ElemDesc elemDesc) throws IOException {
        writer.write(32);
        if ((str2.length() == 0 || str2.equalsIgnoreCase(str)) && elemDesc != null && elemDesc.isAttrFlagSet(str, 4)) {
            writer.write(str);
            return;
        }
        writer.write(str);
        writer.write("=\"");
        if (elemDesc != null && elemDesc.isAttrFlagSet(str, 2)) {
            writeAttrURI(writer, str2, this.m_specialEscapeURLs);
        } else {
            writeAttrString(writer, str2, getEncoding());
        }
        writer.write(34);
    }

    private boolean isASCIIDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static String makeHHString(int i) {
        String upperCase = Integer.toHexString(i).toUpperCase();
        if (upperCase.length() == 1) {
            return "0" + upperCase;
        }
        return upperCase;
    }

    private boolean isHHSign(String str) {
        try {
            Integer.parseInt(str, 16);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void writeAttrURI(Writer writer, String str, boolean z) throws IOException {
        int length = str.length();
        int i = 1;
        if (length > this.m_attrBuff.length) {
            this.m_attrBuff = new char[(length * 2) + 1];
        }
        int i2 = 0;
        str.getChars(0, length, this.m_attrBuff, 0);
        char[] cArr = this.m_attrBuff;
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        char c = 0;
        while (i3 < length) {
            c = cArr[i3];
            if (c < ' ' || c > '~') {
                if (i4 > 0) {
                    writer.write(cArr, i5, i4);
                    i4 = i2;
                }
                if (z) {
                    if (c <= 127) {
                        writer.write(37);
                        writer.write(makeHHString(c));
                    } else if (c <= 2047) {
                        writer.write(37);
                        writer.write(makeHHString((c >> 6) | 192));
                        writer.write(37);
                        writer.write(makeHHString(128 | (c & '?')));
                    } else if (Encodings.isHighUTF16Surrogate(c)) {
                        int i6 = c & 1023;
                        int i7 = ((i6 & 960) >> 6) + i;
                        i3++;
                        char c2 = cArr[i3];
                        int i8 = c2 & 1023;
                        writer.write(37);
                        writer.write(makeHHString(240 | (i7 >> 2)));
                        writer.write(37);
                        writer.write(makeHHString((((i7 & 3) << 4) & 48) | 128 | ((i6 & 60) >> 2)));
                        writer.write(37);
                        writer.write(makeHHString(((i8 & 960) >> 6) | (((i6 & 3) << 4) & 48) | 128));
                        writer.write(37);
                        writer.write(makeHHString((i8 & 63) | 128));
                        c = c2;
                    } else {
                        writer.write(37);
                        writer.write(makeHHString((c >> '\f') | 224));
                        writer.write(37);
                        writer.write(makeHHString(((c & 4032) >> 6) | 128));
                        writer.write(37);
                        writer.write(makeHHString((c & '?') | 128));
                    }
                } else if (escapingNotNeeded(c)) {
                    writer.write(c);
                } else {
                    writer.write("&#");
                    writer.write(Integer.toString(c));
                    writer.write(59);
                }
                i5 = i3 + 1;
            } else if (c == '\"') {
                if (i4 > 0) {
                    writer.write(cArr, i5, i4);
                    i4 = i2;
                }
                if (z) {
                    writer.write("%22");
                } else {
                    writer.write(SerializerConstants.ENTITY_QUOT);
                }
                i5 = i3 + 1;
            } else if (c == '&') {
                if (i4 > 0) {
                    writer.write(cArr, i5, i4);
                    i4 = i2;
                }
                writer.write(SerializerConstants.ENTITY_AMP);
                i5 = i3 + 1;
            } else {
                i4++;
            }
            i = 1;
            i3++;
            i2 = 0;
        }
        if (i4 > i) {
            if (i5 == 0) {
                writer.write(str);
                return;
            } else {
                writer.write(cArr, i5, i4);
                return;
            }
        }
        if (i4 == i) {
            writer.write(c);
        }
    }

    @Override
    public void writeAttrString(Writer writer, String str, String str2) throws IOException {
        char c;
        int i;
        int i2;
        int length = str.length();
        if (length > this.m_attrBuff.length) {
            this.m_attrBuff = new char[(length * 2) + 1];
        }
        int i3 = 0;
        str.getChars(0, length, this.m_attrBuff, 0);
        char[] cArr = this.m_attrBuff;
        int i4 = 0;
        int i5 = 0;
        char c2 = 0;
        int i6 = 0;
        while (i6 < length) {
            char c3 = cArr[i6];
            if (!escapingNotNeeded(c3) || this.m_charInfo.shouldMapAttrChar(c3)) {
                if ('<' == c3 || '>' == c3) {
                    c = c3;
                    i4++;
                } else if ('&' == c3 && (i2 = i6 + 1) < length && '{' == cArr[i2]) {
                    i4++;
                } else {
                    if (i4 > 0) {
                        writer.write(cArr, i5, i4);
                        i = i3;
                    } else {
                        i = i4;
                    }
                    c = c3;
                    int iAccumDefaultEntity = accumDefaultEntity(writer, c3, i6, cArr, length, false, true);
                    if (i6 != iAccumDefaultEntity) {
                        i6 = iAccumDefaultEntity - 1;
                    } else {
                        if (Encodings.isHighUTF16Surrogate(c)) {
                            writeUTF16Surrogate(c, cArr, i6, length);
                            i6++;
                        }
                        String outputStringForChar = this.m_charInfo.getOutputStringForChar(c);
                        if (outputStringForChar != null) {
                            writer.write(outputStringForChar);
                        } else if (escapingNotNeeded(c)) {
                            writer.write(c);
                        } else {
                            writer.write("&#");
                            writer.write(Integer.toString(c));
                            writer.write(59);
                        }
                    }
                    i5 = i6 + 1;
                    i4 = i;
                }
                i6++;
                c2 = c;
                i3 = 0;
            } else {
                i4++;
            }
            c = c3;
            i6++;
            c2 = c;
            i3 = 0;
        }
        if (i4 <= 1) {
            if (i4 == 1) {
                writer.write(c2);
            }
        } else if (i5 == 0) {
            writer.write(str);
        } else {
            writer.write(cArr, i5, i4);
        }
    }

    @Override
    public final void characters(char[] cArr, int i, int i2) throws SAXException {
        if (this.m_elemContext.m_isRaw) {
            try {
                if (this.m_elemContext.m_startTagOpen) {
                    closeStartTag();
                    this.m_elemContext.m_startTagOpen = false;
                }
                this.m_ispreserve = true;
                writeNormalizedChars(cArr, i, i2, false, this.m_lineSepUse);
                if (this.m_tracer != null) {
                    super.fireCharEvent(cArr, i, i2);
                    return;
                }
                return;
            } catch (IOException e) {
                throw new SAXException(Utils.messages.createMessage("ER_OIERROR", null), e);
            }
        }
        super.characters(cArr, i, i2);
    }

    @Override
    public final void cdata(char[] cArr, int i, int i2) throws SAXException {
        if (this.m_elemContext.m_elementName != null && (this.m_elemContext.m_elementName.equalsIgnoreCase("SCRIPT") || this.m_elemContext.m_elementName.equalsIgnoreCase("STYLE"))) {
            try {
                if (this.m_elemContext.m_startTagOpen) {
                    closeStartTag();
                    this.m_elemContext.m_startTagOpen = false;
                }
                this.m_ispreserve = true;
                if (shouldIndent()) {
                    indent();
                }
                writeNormalizedChars(cArr, i, i2, true, this.m_lineSepUse);
                return;
            } catch (IOException e) {
                throw new SAXException(Utils.messages.createMessage("ER_OIERROR", null), e);
            }
        }
        super.cdata(cArr, i, i2);
    }

    @Override
    public void processingInstruction(String str, String str2) throws SAXException {
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
                } else if (this.m_cdataTagOpen) {
                    closeCDATA();
                } else if (this.m_needToCallStartDocument) {
                    startDocumentInternal();
                }
                if (true == this.m_needToOutputDocTypeDecl) {
                    outputDocTypeDecl("html");
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
                writer.write(str2);
                writer.write(62);
                if (this.m_elemContext.m_currentElemDepth <= 0) {
                    outputLineSep();
                }
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
    public final void entityReference(String str) throws SAXException {
        try {
            Writer writer = this.m_writer;
            writer.write(38);
            writer.write(str);
            writer.write(59);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public final void endElement(String str) throws SAXException {
        endElement(null, null, str);
    }

    @Override
    public void processAttributes(Writer writer, int i) throws SAXException, IOException {
        for (int i2 = 0; i2 < i; i2++) {
            processAttribute(writer, this.m_attributes.getQName(i2), this.m_attributes.getValue(i2), this.m_elemContext.m_elementDesc);
        }
    }

    @Override
    protected void closeStartTag() throws SAXException {
        try {
            if (this.m_tracer != null) {
                super.fireStartElem(this.m_elemContext.m_elementName);
            }
            int length = this.m_attributes.getLength();
            if (length > 0) {
                processAttributes(this.m_writer, length);
                this.m_attributes.clear();
            }
            this.m_writer.write(62);
            if (this.m_CdataElems != null) {
                this.m_elemContext.m_isCdataSection = isCdataSection();
            }
            if (this.m_doIndent) {
                this.m_isprevtext = false;
                this.m_preserves.push(this.m_ispreserve);
            }
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void namespaceAfterStartElement(String str, String str2) throws SAXException {
        if (this.m_elemContext.m_elementURI == null && getPrefixPart(this.m_elemContext.m_elementName) == null && "".equals(str)) {
            this.m_elemContext.m_elementURI = str2;
        }
        startPrefixMapping(str, str2, false);
    }

    @Override
    public void startDTD(String str, String str2, String str3) throws SAXException {
        this.m_inDTD = true;
        super.startDTD(str, str2, str3);
    }

    @Override
    public void endDTD() throws SAXException {
        this.m_inDTD = false;
    }

    @Override
    public void attributeDecl(String str, String str2, String str3, String str4, String str5) throws SAXException {
    }

    @Override
    public void elementDecl(String str, String str2) throws SAXException {
    }

    @Override
    public void internalEntityDecl(String str, String str2) throws SAXException {
    }

    @Override
    public void externalEntityDecl(String str, String str2, String str3) throws SAXException {
    }

    @Override
    public void addUniqueAttribute(String str, String str2, int i) throws SAXException {
        try {
            Writer writer = this.m_writer;
            if ((i & 1) > 0 && this.m_htmlcharInfo.onlyQuotAmpLtGt) {
                writer.write(32);
                writer.write(str);
                writer.write("=\"");
                writer.write(str2);
                writer.write(34);
                return;
            }
            if ((i & 2) > 0 && (str2.length() == 0 || str2.equalsIgnoreCase(str))) {
                writer.write(32);
                writer.write(str);
                return;
            }
            writer.write(32);
            writer.write(str);
            writer.write("=\"");
            if ((i & 4) > 0) {
                writeAttrURI(writer, str2, this.m_specialEscapeURLs);
            } else {
                writeAttrString(writer, str2, getEncoding());
            }
            writer.write(34);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void comment(char[] cArr, int i, int i2) throws SAXException {
        if (this.m_inDTD) {
            return;
        }
        if (this.m_elemContext.m_startTagOpen) {
            closeStartTag();
            this.m_elemContext.m_startTagOpen = false;
        } else if (this.m_cdataTagOpen) {
            closeCDATA();
        } else if (this.m_needToCallStartDocument) {
            startDocumentInternal();
        }
        if (this.m_needToOutputDocTypeDecl) {
            outputDocTypeDecl("html");
        }
        super.comment(cArr, i, i2);
    }

    @Override
    public boolean reset() {
        if (!super.reset()) {
            return false;
        }
        resetToHTMLStream();
        return true;
    }

    private void resetToHTMLStream() {
        this.m_inBlockElem = false;
        this.m_inDTD = false;
        this.m_omitMetaTag = false;
        this.m_specialEscapeURLs = true;
    }

    static class Trie {
        public static final int ALPHA_SIZE = 128;
        final Node m_Root;
        private char[] m_charBuffer;
        private final boolean m_lowerCaseOnly;

        public Trie() {
            this.m_charBuffer = new char[0];
            this.m_Root = new Node();
            this.m_lowerCaseOnly = false;
        }

        public Trie(boolean z) {
            this.m_charBuffer = new char[0];
            this.m_Root = new Node();
            this.m_lowerCaseOnly = z;
        }

        public Object put(String str, Object obj) {
            int length = str.length();
            if (length > this.m_charBuffer.length) {
                this.m_charBuffer = new char[length];
            }
            Node node = this.m_Root;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                Node node2 = node.m_nextChar[Character.toLowerCase(str.charAt(i))];
                if (node2 != null) {
                    i++;
                    node = node2;
                } else {
                    while (i < length) {
                        Node node3 = new Node();
                        if (this.m_lowerCaseOnly) {
                            node.m_nextChar[Character.toLowerCase(str.charAt(i))] = node3;
                        } else {
                            node.m_nextChar[Character.toUpperCase(str.charAt(i))] = node3;
                            node.m_nextChar[Character.toLowerCase(str.charAt(i))] = node3;
                        }
                        i++;
                        node = node3;
                    }
                }
            }
            Object obj2 = node.m_Value;
            node.m_Value = obj;
            return obj2;
        }

        public Object get(String str) {
            Node node;
            int length = str.length();
            if (this.m_charBuffer.length < length) {
                return null;
            }
            Node node2 = this.m_Root;
            switch (length) {
                case 0:
                    return null;
                case 1:
                    char cCharAt = str.charAt(0);
                    if (cCharAt >= 128 || (node = node2.m_nextChar[cCharAt]) == null) {
                        return null;
                    }
                    return node.m_Value;
            }
            for (int i = 0; i < length; i++) {
                char cCharAt2 = str.charAt(i);
                if (128 <= cCharAt2 || (node2 = node2.m_nextChar[cCharAt2]) == null) {
                    return null;
                }
            }
            return node2.m_Value;
        }

        private class Node {
            final Node[] m_nextChar = new Node[128];
            Object m_Value = null;

            Node() {
            }
        }

        public Trie(Trie trie) {
            this.m_charBuffer = new char[0];
            this.m_Root = trie.m_Root;
            this.m_lowerCaseOnly = trie.m_lowerCaseOnly;
            this.m_charBuffer = new char[trie.getLongestKeyLength()];
        }

        public Object get2(String str) {
            Node node;
            int length = str.length();
            if (this.m_charBuffer.length < length) {
                return null;
            }
            Node node2 = this.m_Root;
            switch (length) {
                case 0:
                    return null;
                case 1:
                    char cCharAt = str.charAt(0);
                    if (cCharAt >= 128 || (node = node2.m_nextChar[cCharAt]) == null) {
                        return null;
                    }
                    return node.m_Value;
                default:
                    str.getChars(0, length, this.m_charBuffer, 0);
                    for (int i = 0; i < length; i++) {
                        char c = this.m_charBuffer[i];
                        if (128 <= c || (node2 = node2.m_nextChar[c]) == null) {
                            return null;
                        }
                    }
                    return node2.m_Value;
            }
        }

        public int getLongestKeyLength() {
            return this.m_charBuffer.length;
        }
    }
}
