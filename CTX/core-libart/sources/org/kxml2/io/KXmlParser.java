package org.kxml2.io;

import android.icu.impl.PatternTokenizer;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import libcore.internal.StringPool;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class KXmlParser implements XmlPullParser, Closeable {
    private static final char[] ANY;
    private static final int ATTLISTDECL = 13;
    private static final char[] COMMENT_DOUBLE_DASH;
    private static final Map<String, String> DEFAULT_ENTITIES = new HashMap();
    private static final char[] DOUBLE_QUOTE;
    private static final int ELEMENTDECL = 11;
    private static final char[] EMPTY;
    private static final char[] END_CDATA;
    private static final char[] END_COMMENT;
    private static final char[] END_PROCESSING_INSTRUCTION;
    private static final int ENTITYDECL = 12;
    private static final String FEATURE_RELAXED = "http://xmlpull.org/v1/doc/features.html#relaxed";
    private static final char[] FIXED;
    private static final String ILLEGAL_TYPE = "Wrong event type";
    private static final char[] IMPLIED;
    private static final char[] NDATA;
    private static final char[] NOTATION;
    private static final int NOTATIONDECL = 14;
    private static final int PARAMETER_ENTITY_REF = 15;
    private static final String PROPERTY_LOCATION = "http://xmlpull.org/v1/doc/properties.html#location";
    private static final String PROPERTY_XMLDECL_STANDALONE = "http://xmlpull.org/v1/doc/properties.html#xmldecl-standalone";
    private static final String PROPERTY_XMLDECL_VERSION = "http://xmlpull.org/v1/doc/properties.html#xmldecl-version";
    private static final char[] PUBLIC;
    private static final char[] REQUIRED;
    private static final char[] SINGLE_QUOTE;
    private static final char[] START_ATTLIST;
    private static final char[] START_CDATA;
    private static final char[] START_COMMENT;
    private static final char[] START_DOCTYPE;
    private static final char[] START_ELEMENT;
    private static final char[] START_ENTITY;
    private static final char[] START_NOTATION;
    private static final char[] START_PROCESSING_INSTRUCTION;
    private static final char[] SYSTEM;
    private static final String UNEXPECTED_EOF = "Unexpected EOF";
    private static final int XML_DECLARATION = 998;
    private int attributeCount;
    private StringBuilder bufferCapture;
    private int bufferStartColumn;
    private int bufferStartLine;
    private Map<String, Map<String, String>> defaultAttributes;
    private boolean degenerated;
    private int depth;
    private Map<String, char[]> documentEntities;
    private String encoding;
    private String error;
    private boolean isWhitespace;
    private boolean keepNamespaceAttributes;
    private String location;
    private String name;
    private String namespace;
    private ContentSource nextContentSource;
    private boolean parsedTopLevelStartTag;
    private String prefix;
    private boolean processDocDecl;
    private boolean processNsp;
    private String publicId;
    private Reader reader;
    private boolean relaxed;
    private String rootElementName;
    private Boolean standalone;
    private String systemId;
    private String text;
    private int type;
    private boolean unresolved;
    private String version;
    private String[] elementStack = new String[16];
    private String[] nspStack = new String[8];
    private int[] nspCounts = new int[4];
    private char[] buffer = new char[8192];
    private int position = 0;
    private int limit = 0;
    private String[] attributes = new String[16];
    public final StringPool stringPool = new StringPool();

    enum ValueContext {
        ATTRIBUTE,
        TEXT,
        ENTITY_DECLARATION
    }

    static {
        DEFAULT_ENTITIES.put("lt", "<");
        DEFAULT_ENTITIES.put("gt", ">");
        DEFAULT_ENTITIES.put("amp", "&");
        DEFAULT_ENTITIES.put("apos", "'");
        DEFAULT_ENTITIES.put("quot", "\"");
        START_COMMENT = new char[]{'<', '!', '-', '-'};
        END_COMMENT = new char[]{'-', '-', '>'};
        COMMENT_DOUBLE_DASH = new char[]{'-', '-'};
        START_CDATA = new char[]{'<', '!', '[', 'C', 'D', 'A', 'T', 'A', '['};
        END_CDATA = new char[]{']', ']', '>'};
        START_PROCESSING_INSTRUCTION = new char[]{'<', '?'};
        END_PROCESSING_INSTRUCTION = new char[]{'?', '>'};
        START_DOCTYPE = new char[]{'<', '!', 'D', 'O', 'C', 'T', 'Y', 'P', 'E'};
        SYSTEM = new char[]{'S', 'Y', 'S', 'T', 'E', 'M'};
        PUBLIC = new char[]{'P', 'U', 'B', 'L', 'I', 'C'};
        START_ELEMENT = new char[]{'<', '!', 'E', 'L', 'E', 'M', 'E', 'N', 'T'};
        START_ATTLIST = new char[]{'<', '!', 'A', 'T', 'T', 'L', 'I', 'S', 'T'};
        START_ENTITY = new char[]{'<', '!', 'E', 'N', 'T', 'I', 'T', 'Y'};
        START_NOTATION = new char[]{'<', '!', 'N', 'O', 'T', 'A', 'T', 'I', 'O', 'N'};
        EMPTY = new char[]{'E', 'M', 'P', 'T', 'Y'};
        ANY = new char[]{'A', 'N', 'Y'};
        NDATA = new char[]{'N', 'D', 'A', 'T', 'A'};
        NOTATION = new char[]{'N', 'O', 'T', 'A', 'T', 'I', 'O', 'N'};
        REQUIRED = new char[]{'R', 'E', 'Q', 'U', 'I', 'R', 'E', 'D'};
        IMPLIED = new char[]{'I', 'M', 'P', 'L', 'I', 'E', 'D'};
        FIXED = new char[]{'F', 'I', 'X', 'E', 'D'};
        SINGLE_QUOTE = new char[]{PatternTokenizer.SINGLE_QUOTE};
        DOUBLE_QUOTE = new char[]{'\"'};
    }

    public void keepNamespaceAttributes() {
        this.keepNamespaceAttributes = true;
    }

    private boolean adjustNsp() throws XmlPullParserException {
        String strSubstring;
        int i = 0;
        boolean z = false;
        while (i < (this.attributeCount << 2)) {
            String str = this.attributes[i + 2];
            int iIndexOf = str.indexOf(58);
            if (iIndexOf != -1) {
                String strSubstring2 = str.substring(0, iIndexOf);
                strSubstring = str.substring(iIndexOf + 1);
                str = strSubstring2;
            } else if (!str.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
                i += 4;
            } else {
                strSubstring = null;
            }
            if (!str.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
                z = true;
            } else {
                int[] iArr = this.nspCounts;
                int i2 = this.depth;
                int i3 = iArr[i2];
                iArr[i2] = i3 + 1;
                int i4 = i3 << 1;
                this.nspStack = ensureCapacity(this.nspStack, i4 + 2);
                this.nspStack[i4] = strSubstring;
                int i5 = i + 3;
                this.nspStack[i4 + 1] = this.attributes[i5];
                if (strSubstring != null && this.attributes[i5].isEmpty()) {
                    checkRelaxed("illegal empty namespace");
                }
                if (this.keepNamespaceAttributes) {
                    this.attributes[i] = XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
                    z = true;
                } else {
                    String[] strArr = this.attributes;
                    int i6 = this.attributeCount - 1;
                    this.attributeCount = i6;
                    System.arraycopy(this.attributes, i + 4, strArr, i, (i6 << 2) - i);
                    i -= 4;
                }
            }
            i += 4;
        }
        if (z) {
            for (int i7 = (this.attributeCount << 2) - 4; i7 >= 0; i7 -= 4) {
                int i8 = i7 + 2;
                String str2 = this.attributes[i8];
                int iIndexOf2 = str2.indexOf(58);
                if (iIndexOf2 == 0 && !this.relaxed) {
                    throw new RuntimeException("illegal attribute name: " + str2 + " at " + this);
                }
                if (iIndexOf2 != -1) {
                    String strSubstring3 = str2.substring(0, iIndexOf2);
                    String strSubstring4 = str2.substring(iIndexOf2 + 1);
                    String namespace = getNamespace(strSubstring3);
                    if (namespace == null && !this.relaxed) {
                        throw new RuntimeException("Undefined Prefix: " + strSubstring3 + " in " + this);
                    }
                    this.attributes[i7] = namespace;
                    this.attributes[i7 + 1] = strSubstring3;
                    this.attributes[i8] = strSubstring4;
                }
            }
        }
        int iIndexOf3 = this.name.indexOf(58);
        if (iIndexOf3 == 0) {
            checkRelaxed("illegal tag name: " + this.name);
        }
        if (iIndexOf3 != -1) {
            this.prefix = this.name.substring(0, iIndexOf3);
            this.name = this.name.substring(iIndexOf3 + 1);
        }
        this.namespace = getNamespace(this.prefix);
        if (this.namespace == null) {
            if (this.prefix != null) {
                checkRelaxed("undefined prefix: " + this.prefix);
            }
            this.namespace = "";
        }
        return z;
    }

    private String[] ensureCapacity(String[] strArr, int i) {
        if (strArr.length >= i) {
            return strArr;
        }
        String[] strArr2 = new String[i + 16];
        System.arraycopy(strArr, 0, strArr2, 0, strArr.length);
        return strArr2;
    }

    private void checkRelaxed(String str) throws XmlPullParserException {
        if (!this.relaxed) {
            throw new XmlPullParserException(str, this, null);
        }
        if (this.error == null) {
            this.error = "Error: " + str;
        }
    }

    @Override
    public int next() throws XmlPullParserException, IOException {
        return next(false);
    }

    @Override
    public int nextToken() throws XmlPullParserException, IOException {
        return next(true);
    }

    private int next(boolean z) throws XmlPullParserException, IOException {
        if (this.reader == null) {
            throw new XmlPullParserException("setInput() must be called first.", this, null);
        }
        if (this.type == 3) {
            this.depth--;
        }
        if (this.degenerated) {
            this.degenerated = false;
            this.type = 3;
            return this.type;
        }
        if (this.error != null) {
            if (z) {
                this.text = this.error;
                this.type = 9;
                this.error = null;
                return this.type;
            }
            this.error = null;
        }
        this.type = peekType(false);
        if (this.type == XML_DECLARATION) {
            readXmlDeclaration();
            this.type = peekType(false);
        }
        this.text = null;
        this.isWhitespace = true;
        this.prefix = null;
        this.name = null;
        this.namespace = null;
        this.attributeCount = -1;
        boolean z2 = !z;
        while (true) {
            switch (this.type) {
                case 1:
                    return this.type;
                case 2:
                    parseStartTag(false, z2);
                    return this.type;
                case 3:
                    readEndTag();
                    return this.type;
                case 4:
                    this.text = readValue('<', !z, z2, ValueContext.TEXT);
                    if (this.depth == 0 && this.isWhitespace) {
                        this.type = 7;
                    }
                    if (this.depth == 0 || (this.type != 6 && this.type != 4 && this.type != 5)) {
                        if (!z) {
                            return this.type;
                        }
                        if (this.type == 7) {
                            this.text = null;
                        }
                        int iPeekType = peekType(false);
                        if (this.text != null && !this.text.isEmpty() && iPeekType < 4) {
                            this.type = 4;
                            return this.type;
                        }
                        this.type = iPeekType;
                    }
                    break;
                case 5:
                    read(START_CDATA);
                    this.text = readUntil(END_CDATA, true);
                    if (this.depth == 0) {
                    }
                    if (!z) {
                    }
                    break;
                case 6:
                    if (z) {
                        StringBuilder sb = new StringBuilder();
                        readEntity(sb, true, z2, ValueContext.TEXT);
                        this.text = sb.toString();
                    }
                    if (this.depth == 0) {
                    }
                    if (!z) {
                    }
                    break;
                case 7:
                default:
                    throw new XmlPullParserException("Unexpected token", this, null);
                case 8:
                    read(START_PROCESSING_INSTRUCTION);
                    String until = readUntil(END_PROCESSING_INSTRUCTION, z);
                    if (z) {
                        this.text = until;
                    }
                    if (this.depth == 0) {
                    }
                    if (!z) {
                    }
                    break;
                case 9:
                    String comment = readComment(z);
                    if (z) {
                        this.text = comment;
                    }
                    if (this.depth == 0) {
                    }
                    if (!z) {
                    }
                    break;
                case 10:
                    readDoctype(z);
                    if (this.parsedTopLevelStartTag) {
                        throw new XmlPullParserException("Unexpected token", this, null);
                    }
                    if (this.depth == 0) {
                    }
                    if (!z) {
                    }
                    break;
            }
        }
    }

    private String readUntil(char[] cArr, boolean z) throws XmlPullParserException, IOException {
        StringBuilder sb;
        int i = this.position;
        if (z && this.text != null) {
            sb = new StringBuilder();
            sb.append(this.text);
        } else {
            sb = null;
        }
        while (true) {
            if (this.position + cArr.length > this.limit) {
                if (i < this.position && z) {
                    if (sb == null) {
                        sb = new StringBuilder();
                    }
                    sb.append(this.buffer, i, this.position - i);
                }
                if (!fillBuffer(cArr.length)) {
                    checkRelaxed(UNEXPECTED_EOF);
                    this.type = 9;
                    return null;
                }
                i = this.position;
            }
            for (int i2 = 0; i2 < cArr.length; i2++) {
                if (this.buffer[this.position + i2] != cArr[i2]) {
                    break;
                }
            }
            int i3 = this.position;
            this.position += cArr.length;
            if (!z) {
                return null;
            }
            if (sb == null) {
                return this.stringPool.get(this.buffer, i, i3 - i);
            }
            sb.append(this.buffer, i, i3 - i);
            return sb.toString();
            this.position++;
        }
    }

    private void readXmlDeclaration() throws XmlPullParserException, IOException {
        if (this.bufferStartLine != 0 || this.bufferStartColumn != 0 || this.position != 0) {
            checkRelaxed("processing instructions must not start with xml");
        }
        read(START_PROCESSING_INSTRUCTION);
        parseStartTag(true, true);
        int i = 2;
        if (this.attributeCount < 1 || !OutputKeys.VERSION.equals(this.attributes[2])) {
            checkRelaxed("version expected");
        }
        this.version = this.attributes[3];
        if (1 < this.attributeCount && OutputKeys.ENCODING.equals(this.attributes[6])) {
            this.encoding = this.attributes[7];
        } else {
            i = 1;
        }
        if (i < this.attributeCount) {
            int i2 = 4 * i;
            if (OutputKeys.STANDALONE.equals(this.attributes[i2 + 2])) {
                String str = this.attributes[3 + i2];
                if ("yes".equals(str)) {
                    this.standalone = Boolean.TRUE;
                } else if ("no".equals(str)) {
                    this.standalone = Boolean.FALSE;
                } else {
                    checkRelaxed("illegal standalone value: " + str);
                }
                i++;
            }
        }
        if (i != this.attributeCount) {
            checkRelaxed("unexpected attributes in XML declaration");
        }
        this.isWhitespace = true;
        this.text = null;
    }

    private String readComment(boolean z) throws XmlPullParserException, IOException {
        read(START_COMMENT);
        if (this.relaxed) {
            return readUntil(END_COMMENT, z);
        }
        String until = readUntil(COMMENT_DOUBLE_DASH, z);
        if (peekCharacter() != 62) {
            throw new XmlPullParserException("Comments may not contain --", this, null);
        }
        this.position++;
        return until;
    }

    private void readDoctype(boolean z) throws XmlPullParserException, IOException {
        int i;
        read(START_DOCTYPE);
        if (z) {
            this.bufferCapture = new StringBuilder();
            i = this.position;
        } else {
            i = -1;
        }
        try {
            skip();
            this.rootElementName = readName();
            readExternalId(true, true);
            skip();
            if (peekCharacter() == 91) {
                readInternalSubset();
            }
            skip();
            read('>');
            skip();
        } finally {
            if (z) {
                this.bufferCapture.append(this.buffer, 0, this.position);
                this.bufferCapture.delete(0, i);
                this.text = this.bufferCapture.toString();
                this.bufferCapture = null;
            }
        }
    }

    private boolean readExternalId(boolean z, boolean z2) throws XmlPullParserException, IOException {
        int iPeekCharacter;
        skip();
        int iPeekCharacter2 = peekCharacter();
        if (iPeekCharacter2 == 83) {
            read(SYSTEM);
        } else {
            if (iPeekCharacter2 != 80) {
                return false;
            }
            read(PUBLIC);
            skip();
            if (z2) {
                this.publicId = readQuotedId(true);
            } else {
                readQuotedId(false);
            }
        }
        skip();
        if (!z && (iPeekCharacter = peekCharacter()) != 34 && iPeekCharacter != 39) {
            return true;
        }
        if (z2) {
            this.systemId = readQuotedId(true);
        } else {
            readQuotedId(false);
        }
        return true;
    }

    private String readQuotedId(boolean z) throws XmlPullParserException, IOException {
        char[] cArr;
        int iPeekCharacter = peekCharacter();
        if (iPeekCharacter == 34) {
            cArr = DOUBLE_QUOTE;
        } else if (iPeekCharacter == 39) {
            cArr = SINGLE_QUOTE;
        } else {
            throw new XmlPullParserException("Expected a quoted string", this, null);
        }
        this.position++;
        return readUntil(cArr, z);
    }

    private void readInternalSubset() throws XmlPullParserException, IOException {
        read('[');
        while (true) {
            skip();
            if (peekCharacter() == 93) {
                this.position++;
                return;
            }
            switch (peekType(true)) {
                case 8:
                    read(START_PROCESSING_INSTRUCTION);
                    readUntil(END_PROCESSING_INSTRUCTION, false);
                    break;
                case 9:
                    readComment(false);
                    break;
                case 10:
                default:
                    throw new XmlPullParserException("Unexpected token", this, null);
                case 11:
                    readElementDeclaration();
                    break;
                case 12:
                    readEntityDeclaration();
                    break;
                case 13:
                    readAttributeListDeclaration();
                    break;
                case 14:
                    readNotationDeclaration();
                    break;
                case 15:
                    throw new XmlPullParserException("Parameter entity references are not supported", this, null);
            }
        }
    }

    private void readElementDeclaration() throws XmlPullParserException, IOException {
        read(START_ELEMENT);
        skip();
        readName();
        readContentSpec();
        skip();
        read('>');
    }

    private void readContentSpec() throws XmlPullParserException, IOException {
        skip();
        int iPeekCharacter = peekCharacter();
        int i = 0;
        if (iPeekCharacter == 40) {
            do {
                if (iPeekCharacter == 40) {
                    i++;
                } else if (iPeekCharacter == 41) {
                    i--;
                } else if (iPeekCharacter == -1) {
                    throw new XmlPullParserException("Unterminated element content spec", this, null);
                }
                this.position++;
                iPeekCharacter = peekCharacter();
            } while (i > 0);
            if (iPeekCharacter == 42 || iPeekCharacter == 63 || iPeekCharacter == 43) {
                this.position++;
                return;
            }
            return;
        }
        if (iPeekCharacter == EMPTY[0]) {
            read(EMPTY);
        } else {
            if (iPeekCharacter == ANY[0]) {
                read(ANY);
                return;
            }
            throw new XmlPullParserException("Expected element content spec", this, null);
        }
    }

    private void readAttributeListDeclaration() throws XmlPullParserException, IOException {
        read(START_ATTLIST);
        skip();
        String name = readName();
        while (true) {
            skip();
            if (peekCharacter() == 62) {
                this.position++;
                return;
            }
            String name2 = readName();
            skip();
            if (this.position + 1 >= this.limit && !fillBuffer(2)) {
                throw new XmlPullParserException("Malformed attribute list", this, null);
            }
            if (this.buffer[this.position] == NOTATION[0] && this.buffer[this.position + 1] == NOTATION[1]) {
                read(NOTATION);
                skip();
            }
            if (peekCharacter() == 40) {
                this.position++;
                while (true) {
                    skip();
                    readName();
                    skip();
                    int iPeekCharacter = peekCharacter();
                    if (iPeekCharacter == 41) {
                        this.position++;
                        break;
                    } else if (iPeekCharacter == 124) {
                        this.position++;
                    } else {
                        throw new XmlPullParserException("Malformed attribute type", this, null);
                    }
                }
            } else {
                readName();
            }
            skip();
            int iPeekCharacter2 = peekCharacter();
            if (iPeekCharacter2 == 35) {
                this.position++;
                int iPeekCharacter3 = peekCharacter();
                if (iPeekCharacter3 == 82) {
                    read(REQUIRED);
                } else if (iPeekCharacter3 == 73) {
                    read(IMPLIED);
                } else if (iPeekCharacter3 == 70) {
                    read(FIXED);
                } else {
                    throw new XmlPullParserException("Malformed attribute type", this, null);
                }
                skip();
                iPeekCharacter2 = peekCharacter();
            }
            if (iPeekCharacter2 == 34 || iPeekCharacter2 == 39) {
                this.position++;
                String value = readValue((char) iPeekCharacter2, true, true, ValueContext.ATTRIBUTE);
                if (peekCharacter() == iPeekCharacter2) {
                    this.position++;
                }
                defineAttributeDefault(name, name2, value);
            }
        }
    }

    private void defineAttributeDefault(String str, String str2, String str3) {
        if (this.defaultAttributes == null) {
            this.defaultAttributes = new HashMap();
        }
        Map<String, String> map = this.defaultAttributes.get(str);
        if (map == null) {
            map = new HashMap<>();
            this.defaultAttributes.put(str, map);
        }
        map.put(str2, str3);
    }

    private void readEntityDeclaration() throws XmlPullParserException, IOException {
        boolean z;
        String value;
        read(START_ENTITY);
        skip();
        if (peekCharacter() == 37) {
            this.position++;
            skip();
            z = false;
        } else {
            z = true;
        }
        String name = readName();
        skip();
        int iPeekCharacter = peekCharacter();
        if (iPeekCharacter == 34 || iPeekCharacter == 39) {
            this.position++;
            value = readValue((char) iPeekCharacter, true, false, ValueContext.ENTITY_DECLARATION);
            if (peekCharacter() == iPeekCharacter) {
                this.position++;
            }
        } else if (readExternalId(true, false)) {
            skip();
            if (peekCharacter() == NDATA[0]) {
                read(NDATA);
                skip();
                readName();
            }
            value = "";
        } else {
            throw new XmlPullParserException("Expected entity value or external ID", this, null);
        }
        if (z && this.processDocDecl) {
            if (this.documentEntities == null) {
                this.documentEntities = new HashMap();
            }
            this.documentEntities.put(name, value.toCharArray());
        }
        skip();
        read('>');
    }

    private void readNotationDeclaration() throws XmlPullParserException, IOException {
        read(START_NOTATION);
        skip();
        readName();
        if (!readExternalId(false, false)) {
            throw new XmlPullParserException("Expected external ID or public ID for notation", this, null);
        }
        skip();
        read('>');
    }

    private void readEndTag() throws XmlPullParserException, IOException {
        read('<');
        read('/');
        this.name = readName();
        skip();
        read('>');
        int i = (this.depth - 1) * 4;
        if (this.depth == 0) {
            checkRelaxed("read end tag " + this.name + " with no tags open");
            this.type = 9;
            return;
        }
        int i2 = i + 3;
        if (this.name.equals(this.elementStack[i2])) {
            this.namespace = this.elementStack[i];
            this.prefix = this.elementStack[i + 1];
            this.name = this.elementStack[i + 2];
        } else if (!this.relaxed) {
            throw new XmlPullParserException("expected: /" + this.elementStack[i2] + " read: " + this.name, this, null);
        }
    }

    private int peekType(boolean z) throws XmlPullParserException, IOException {
        if (this.position >= this.limit && !fillBuffer(1)) {
            return 1;
        }
        char c = this.buffer[this.position];
        if (c != '<') {
            switch (c) {
                case '%':
                    return z ? 15 : 4;
                case '&':
                    return 6;
                default:
                    return 4;
            }
        }
        if (this.position + 3 >= this.limit && !fillBuffer(4)) {
            throw new XmlPullParserException("Dangling <", this, null);
        }
        char c2 = this.buffer[this.position + 1];
        if (c2 != '!') {
            if (c2 == '/') {
                return 3;
            }
            if (c2 != '?') {
                return 2;
            }
            if (this.position + 5 >= this.limit && !fillBuffer(6)) {
                return 8;
            }
            if (this.buffer[this.position + 2] != 'x' && this.buffer[this.position + 2] != 'X') {
                return 8;
            }
            if (this.buffer[this.position + 3] != 'm' && this.buffer[this.position + 3] != 'M') {
                return 8;
            }
            if ((this.buffer[this.position + 4] == 'l' || this.buffer[this.position + 4] == 'L') && this.buffer[this.position + 5] == ' ') {
                return XML_DECLARATION;
            }
            return 8;
        }
        char c3 = this.buffer[this.position + 2];
        if (c3 == '-') {
            return 9;
        }
        if (c3 == 'A') {
            return 13;
        }
        if (c3 == 'N') {
            return 14;
        }
        if (c3 == '[') {
            return 5;
        }
        switch (c3) {
            case 'D':
                return 10;
            case 'E':
                char c4 = this.buffer[this.position + 3];
                if (c4 == 'L') {
                    return 11;
                }
                if (c4 == 'N') {
                    return 12;
                }
                break;
        }
        throw new XmlPullParserException("Unexpected <!", this, null);
    }

    private void parseStartTag(boolean z, boolean z2) throws XmlPullParserException, IOException {
        if (!z) {
            read('<');
        }
        this.name = readName();
        this.attributeCount = 0;
        while (true) {
            skip();
            if (this.position >= this.limit && !fillBuffer(1)) {
                checkRelaxed(UNEXPECTED_EOF);
                return;
            }
            char c = this.buffer[this.position];
            if (z) {
                if (c == '?') {
                    this.position++;
                    read('>');
                    return;
                }
            } else {
                if (c == '/') {
                    this.degenerated = true;
                    this.position++;
                    skip();
                    read('>');
                    break;
                }
                if (c == '>') {
                    this.position++;
                    break;
                }
            }
            String name = readName();
            int i = this.attributeCount;
            this.attributeCount = i + 1;
            int i2 = i * 4;
            this.attributes = ensureCapacity(this.attributes, i2 + 4);
            this.attributes[i2] = "";
            this.attributes[i2 + 1] = null;
            this.attributes[i2 + 2] = name;
            skip();
            if (this.position >= this.limit && !fillBuffer(1)) {
                checkRelaxed(UNEXPECTED_EOF);
                return;
            }
            if (this.buffer[this.position] == '=') {
                this.position++;
                skip();
                if (this.position >= this.limit && !fillBuffer(1)) {
                    checkRelaxed(UNEXPECTED_EOF);
                    return;
                }
                char c2 = this.buffer[this.position];
                if (c2 == '\'' || c2 == '\"') {
                    this.position++;
                } else {
                    if (!this.relaxed) {
                        throw new XmlPullParserException("attr value delimiter missing!", this, null);
                    }
                    c2 = ' ';
                }
                this.attributes[i2 + 3] = readValue(c2, true, z2, ValueContext.ATTRIBUTE);
                if (c2 != ' ' && peekCharacter() == c2) {
                    this.position++;
                }
            } else if (this.relaxed) {
                this.attributes[i2 + 3] = name;
            } else {
                checkRelaxed("Attr.value missing f. " + name);
                this.attributes[i2 + 3] = name;
            }
        }
    }

    private void readEntity(StringBuilder sb, boolean z, boolean z2, ValueContext valueContext) throws XmlPullParserException, IOException {
        int i;
        char[] cArr;
        int length = sb.length();
        char[] cArr2 = this.buffer;
        int i2 = this.position;
        this.position = i2 + 1;
        if (cArr2[i2] != '&') {
            throw new AssertionError();
        }
        sb.append('&');
        while (true) {
            int iPeekCharacter = peekCharacter();
            if (iPeekCharacter == 59) {
                sb.append(';');
                this.position++;
                String strSubstring = sb.substring(length + 1, sb.length() - 1);
                if (z) {
                    this.name = strSubstring;
                }
                if (strSubstring.startsWith("#")) {
                    try {
                        if (strSubstring.startsWith("#x")) {
                            i = Integer.parseInt(strSubstring.substring(2), 16);
                        } else {
                            i = Integer.parseInt(strSubstring.substring(1));
                        }
                        sb.delete(length, sb.length());
                        sb.appendCodePoint(i);
                        this.unresolved = false;
                        return;
                    } catch (NumberFormatException e) {
                        throw new XmlPullParserException("Invalid character reference: &" + strSubstring);
                    } catch (IllegalArgumentException e2) {
                        throw new XmlPullParserException("Invalid character reference: &" + strSubstring);
                    }
                }
                if (valueContext == ValueContext.ENTITY_DECLARATION) {
                    return;
                }
                String str = DEFAULT_ENTITIES.get(strSubstring);
                if (str != null) {
                    sb.delete(length, sb.length());
                    this.unresolved = false;
                    sb.append(str);
                    return;
                }
                if (this.documentEntities != null && (cArr = this.documentEntities.get(strSubstring)) != null) {
                    sb.delete(length, sb.length());
                    this.unresolved = false;
                    if (this.processDocDecl) {
                        pushContentSource(cArr);
                        return;
                    } else {
                        sb.append(cArr);
                        return;
                    }
                }
                if (this.systemId != null) {
                    sb.delete(length, sb.length());
                    return;
                }
                this.unresolved = true;
                if (z2) {
                    checkRelaxed("unresolved: &" + strSubstring + ";");
                    return;
                }
                return;
            }
            if (iPeekCharacter >= 128 || ((iPeekCharacter >= 48 && iPeekCharacter <= 57) || ((iPeekCharacter >= 97 && iPeekCharacter <= 122) || ((iPeekCharacter >= 65 && iPeekCharacter <= 90) || iPeekCharacter == 95 || iPeekCharacter == 45 || iPeekCharacter == 35)))) {
                this.position++;
                sb.append((char) iPeekCharacter);
            } else if (this.relaxed) {
                return;
            } else {
                throw new XmlPullParserException("unterminated entity ref", this, null);
            }
        }
    }

    private String readValue(char c, boolean z, boolean z2, ValueContext valueContext) throws XmlPullParserException, IOException {
        StringBuilder sb;
        int i = this.position;
        if (valueContext != ValueContext.TEXT || this.text == null) {
            sb = null;
        } else {
            sb = new StringBuilder();
            sb.append(this.text);
        }
        while (true) {
            if (this.position >= this.limit) {
                if (i < this.position) {
                    if (sb == null) {
                        sb = new StringBuilder();
                    }
                    sb.append(this.buffer, i, this.position - i);
                }
                if (!fillBuffer(1)) {
                    return sb != null ? sb.toString() : "";
                }
                i = this.position;
            }
            char c2 = this.buffer[this.position];
            if (c2 == c || ((c == ' ' && (c2 <= ' ' || c2 == '>')) || (c2 == '&' && !z))) {
                break;
            }
            if (c2 == '\r' || ((c2 == '\n' && valueContext == ValueContext.ATTRIBUTE) || c2 == '&' || c2 == '<' || ((c2 == ']' && valueContext == ValueContext.TEXT) || (c2 == '%' && valueContext == ValueContext.ENTITY_DECLARATION)))) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append(this.buffer, i, this.position - i);
                if (c2 == '\r') {
                    if ((this.position + 1 < this.limit || fillBuffer(2)) && this.buffer[this.position + 1] == '\n') {
                        this.position++;
                    }
                    c2 = valueContext == ValueContext.ATTRIBUTE ? ' ' : '\n';
                } else if (c2 == '\n') {
                    c2 = ' ';
                } else if (c2 == '&') {
                    this.isWhitespace = false;
                    readEntity(sb, false, z2, valueContext);
                    i = this.position;
                } else if (c2 == '<') {
                    if (valueContext == ValueContext.ATTRIBUTE) {
                        checkRelaxed("Illegal: \"<\" inside attribute value");
                    }
                    this.isWhitespace = false;
                } else {
                    if (c2 != ']') {
                        if (c2 == '%') {
                            throw new XmlPullParserException("This parser doesn't support parameter entities", this, null);
                        }
                        throw new AssertionError();
                    }
                    if ((this.position + 2 < this.limit || fillBuffer(3)) && this.buffer[this.position + 1] == ']' && this.buffer[this.position + 2] == '>') {
                        checkRelaxed("Illegal: \"]]>\" outside CDATA section");
                    }
                    this.isWhitespace = false;
                }
                this.position++;
                sb.append(c2);
                i = this.position;
            } else {
                this.isWhitespace &= c2 <= ' ';
                this.position++;
            }
        }
    }

    private void read(char c) throws XmlPullParserException, IOException {
        int iPeekCharacter = peekCharacter();
        if (iPeekCharacter != c) {
            checkRelaxed("expected: '" + c + "' actual: '" + ((char) iPeekCharacter) + "'");
            if (iPeekCharacter == -1) {
                return;
            }
        }
        this.position++;
    }

    private void read(char[] cArr) throws XmlPullParserException, IOException {
        if (this.position + cArr.length > this.limit && !fillBuffer(cArr.length)) {
            checkRelaxed("expected: '" + new String(cArr) + "' but was EOF");
            return;
        }
        for (int i = 0; i < cArr.length; i++) {
            if (this.buffer[this.position + i] != cArr[i]) {
                checkRelaxed("expected: \"" + new String(cArr) + "\" but was \"" + new String(this.buffer, this.position, cArr.length) + "...\"");
            }
        }
        this.position += cArr.length;
    }

    private int peekCharacter() throws XmlPullParserException, IOException {
        if (this.position < this.limit || fillBuffer(1)) {
            return this.buffer[this.position];
        }
        return -1;
    }

    private boolean fillBuffer(int i) throws XmlPullParserException, IOException {
        while (this.nextContentSource != null) {
            if (this.position < this.limit) {
                throw new XmlPullParserException("Unbalanced entity!", this, null);
            }
            popContentSource();
            if (this.limit - this.position >= i) {
                return true;
            }
        }
        for (int i2 = 0; i2 < this.position; i2++) {
            if (this.buffer[i2] == '\n') {
                this.bufferStartLine++;
                this.bufferStartColumn = 0;
            } else {
                this.bufferStartColumn++;
            }
        }
        if (this.bufferCapture != null) {
            this.bufferCapture.append(this.buffer, 0, this.position);
        }
        if (this.limit != this.position) {
            this.limit -= this.position;
            System.arraycopy(this.buffer, this.position, this.buffer, 0, this.limit);
        } else {
            this.limit = 0;
        }
        this.position = 0;
        do {
            int i3 = this.reader.read(this.buffer, this.limit, this.buffer.length - this.limit);
            if (i3 == -1) {
                return false;
            }
            this.limit += i3;
        } while (this.limit < i);
        return true;
    }

    private String readName() throws XmlPullParserException, IOException {
        if (this.position >= this.limit && !fillBuffer(1)) {
            checkRelaxed("name expected");
            return "";
        }
        int i = this.position;
        StringBuilder sb = null;
        char c = this.buffer[this.position];
        if ((c >= 'a' && c <= 'z') || ((c >= 'A' && c <= 'Z') || c == '_' || c == ':' || c >= 192 || this.relaxed)) {
            this.position++;
            while (true) {
                if (this.position >= this.limit) {
                    if (sb == null) {
                        sb = new StringBuilder();
                    }
                    sb.append(this.buffer, i, this.position - i);
                    if (!fillBuffer(1)) {
                        return sb.toString();
                    }
                    i = this.position;
                }
                char c2 = this.buffer[this.position];
                if ((c2 >= 'a' && c2 <= 'z') || ((c2 >= 'A' && c2 <= 'Z') || ((c2 >= '0' && c2 <= '9') || c2 == '_' || c2 == '-' || c2 == ':' || c2 == '.' || c2 >= 183))) {
                    this.position++;
                } else {
                    if (sb == null) {
                        return this.stringPool.get(this.buffer, i, this.position - i);
                    }
                    sb.append(this.buffer, i, this.position - i);
                    return sb.toString();
                }
            }
        } else {
            checkRelaxed("name expected");
            return "";
        }
    }

    private void skip() throws XmlPullParserException, IOException {
        while (true) {
            if ((this.position < this.limit || fillBuffer(1)) && this.buffer[this.position] <= ' ') {
                this.position++;
            } else {
                return;
            }
        }
    }

    @Override
    public void setInput(Reader reader) throws XmlPullParserException {
        this.reader = reader;
        this.type = 0;
        this.parsedTopLevelStartTag = false;
        this.name = null;
        this.namespace = null;
        this.degenerated = false;
        this.attributeCount = -1;
        this.encoding = null;
        this.version = null;
        this.standalone = null;
        if (reader == null) {
            return;
        }
        this.position = 0;
        this.limit = 0;
        this.bufferStartLine = 0;
        this.bufferStartColumn = 0;
        this.depth = 0;
        this.documentEntities = null;
    }

    @Override
    public void setInput(InputStream inputStream, String str) throws XmlPullParserException {
        int i;
        this.position = 0;
        this.limit = 0;
        boolean z = str == null;
        if (inputStream == null) {
            throw new IllegalArgumentException("is == null");
        }
        if (z) {
            int i2 = 0;
            while (this.limit < 4 && (i = inputStream.read()) != -1) {
                try {
                    i2 = (i2 << 8) | i;
                    char[] cArr = this.buffer;
                    int i3 = this.limit;
                    this.limit = i3 + 1;
                    cArr[i3] = (char) i;
                } catch (Exception e) {
                    throw new XmlPullParserException("Invalid stream or encoding: " + e, this, e);
                }
            }
            if (this.limit == 4) {
                if (i2 == -131072) {
                    str = "UTF-32LE";
                    this.limit = 0;
                } else if (i2 != 60) {
                    if (i2 == 65279) {
                        str = "UTF-32BE";
                        this.limit = 0;
                    } else if (i2 == 3932223) {
                        str = "UTF-16BE";
                        this.buffer[0] = '<';
                        this.buffer[1] = '?';
                        this.limit = 2;
                    } else if (i2 == 1006632960) {
                        str = "UTF-32LE";
                        this.buffer[0] = '<';
                        this.limit = 1;
                    } else if (i2 == 1006649088) {
                        str = "UTF-16LE";
                        this.buffer[0] = '<';
                        this.buffer[1] = '?';
                        this.limit = 2;
                    } else if (i2 != 1010792557) {
                        int i4 = (-65536) & i2;
                        if (i4 == -16842752) {
                            str = "UTF-16BE";
                            this.buffer[0] = (char) ((this.buffer[2] << '\b') | this.buffer[3]);
                            this.limit = 1;
                        } else if (i4 == -131072) {
                            str = "UTF-16LE";
                            this.buffer[0] = (char) ((this.buffer[3] << '\b') | this.buffer[2]);
                            this.limit = 1;
                        } else if ((i2 & (-256)) == -272908544) {
                            str = "UTF-8";
                            this.buffer[0] = this.buffer[3];
                            this.limit = 1;
                        }
                    } else {
                        while (true) {
                            int i5 = inputStream.read();
                            if (i5 == -1) {
                                break;
                            }
                            char[] cArr2 = this.buffer;
                            int i6 = this.limit;
                            this.limit = i6 + 1;
                            cArr2[i6] = (char) i5;
                            if (i5 == 62) {
                                String str2 = new String(this.buffer, 0, this.limit);
                                int iIndexOf = str2.indexOf(OutputKeys.ENCODING);
                                if (iIndexOf != -1) {
                                    while (str2.charAt(iIndexOf) != '\"' && str2.charAt(iIndexOf) != '\'') {
                                        iIndexOf++;
                                    }
                                    int i7 = iIndexOf + 1;
                                    str = str2.substring(i7, str2.indexOf(str2.charAt(iIndexOf), i7));
                                }
                            }
                        }
                    }
                } else {
                    str = "UTF-32BE";
                    this.buffer[0] = '<';
                    this.limit = 1;
                }
            }
        }
        if (str == null) {
            str = "UTF-8";
        }
        int i8 = this.limit;
        setInput(new InputStreamReader(inputStream, str));
        this.encoding = str;
        this.limit = i8;
        if (!z && peekCharacter() == 65279) {
            this.limit--;
            System.arraycopy(this.buffer, 1, this.buffer, 0, this.limit);
        }
    }

    @Override
    public void close() throws IOException {
        if (this.reader != null) {
            this.reader.close();
        }
    }

    @Override
    public boolean getFeature(String str) {
        if (XmlPullParser.FEATURE_PROCESS_NAMESPACES.equals(str)) {
            return this.processNsp;
        }
        if (FEATURE_RELAXED.equals(str)) {
            return this.relaxed;
        }
        if (XmlPullParser.FEATURE_PROCESS_DOCDECL.equals(str)) {
            return this.processDocDecl;
        }
        return false;
    }

    @Override
    public String getInputEncoding() {
        return this.encoding;
    }

    @Override
    public void defineEntityReplacementText(String str, String str2) throws XmlPullParserException {
        if (this.processDocDecl) {
            throw new IllegalStateException("Entity replacement text may not be defined with DOCTYPE processing enabled.");
        }
        if (this.reader == null) {
            throw new IllegalStateException("Entity replacement text must be defined after setInput()");
        }
        if (this.documentEntities == null) {
            this.documentEntities = new HashMap();
        }
        this.documentEntities.put(str, str2.toCharArray());
    }

    @Override
    public Object getProperty(String str) {
        if (str.equals(PROPERTY_XMLDECL_VERSION)) {
            return this.version;
        }
        if (str.equals(PROPERTY_XMLDECL_STANDALONE)) {
            return this.standalone;
        }
        if (str.equals(PROPERTY_LOCATION)) {
            return this.location != null ? this.location : this.reader.toString();
        }
        return null;
    }

    public String getRootElementName() {
        return this.rootElementName;
    }

    public String getSystemId() {
        return this.systemId;
    }

    public String getPublicId() {
        return this.publicId;
    }

    @Override
    public int getNamespaceCount(int i) {
        if (i > this.depth) {
            throw new IndexOutOfBoundsException();
        }
        return this.nspCounts[i];
    }

    @Override
    public String getNamespacePrefix(int i) {
        return this.nspStack[i * 2];
    }

    @Override
    public String getNamespaceUri(int i) {
        return this.nspStack[(i * 2) + 1];
    }

    @Override
    public String getNamespace(String str) {
        if (XMLConstants.XML_NS_PREFIX.equals(str)) {
            return "http://www.w3.org/XML/1998/namespace";
        }
        if (XMLConstants.XMLNS_ATTRIBUTE.equals(str)) {
            return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
        }
        for (int namespaceCount = (getNamespaceCount(this.depth) << 1) - 2; namespaceCount >= 0; namespaceCount -= 2) {
            if (str == null) {
                if (this.nspStack[namespaceCount] == null) {
                    return this.nspStack[namespaceCount + 1];
                }
            } else if (str.equals(this.nspStack[namespaceCount])) {
                return this.nspStack[namespaceCount + 1];
            }
        }
        return null;
    }

    @Override
    public int getDepth() {
        return this.depth;
    }

    @Override
    public String getPositionDescription() {
        StringBuilder sb = new StringBuilder(this.type < TYPES.length ? TYPES[this.type] : "unknown");
        sb.append(' ');
        if (this.type == 2 || this.type == 3) {
            if (this.degenerated) {
                sb.append("(empty) ");
            }
            sb.append('<');
            if (this.type == 3) {
                sb.append('/');
            }
            if (this.prefix != null) {
                sb.append("{" + this.namespace + "}" + this.prefix + ":");
            }
            sb.append(this.name);
            int i = this.attributeCount * 4;
            for (int i2 = 0; i2 < i; i2 += 4) {
                sb.append(' ');
                int i3 = i2 + 1;
                if (this.attributes[i3] != null) {
                    sb.append("{" + this.attributes[i2] + "}" + this.attributes[i3] + ":");
                }
                sb.append(this.attributes[i2 + 2] + "='" + this.attributes[i2 + 3] + "'");
            }
            sb.append('>');
        } else if (this.type != 7) {
            if (this.type != 4) {
                sb.append(getText());
            } else if (this.isWhitespace) {
                sb.append("(whitespace)");
            } else {
                String text = getText();
                if (text.length() > 16) {
                    text = text.substring(0, 16) + "...";
                }
                sb.append(text);
            }
        }
        sb.append("@" + getLineNumber() + ":" + getColumnNumber());
        if (this.location != null) {
            sb.append(" in ");
            sb.append(this.location);
        } else if (this.reader != null) {
            sb.append(" in ");
            sb.append(this.reader.toString());
        }
        return sb.toString();
    }

    @Override
    public int getLineNumber() {
        int i = this.bufferStartLine;
        for (int i2 = 0; i2 < this.position; i2++) {
            if (this.buffer[i2] == '\n') {
                i++;
            }
        }
        return i + 1;
    }

    @Override
    public int getColumnNumber() {
        int i = this.bufferStartColumn;
        for (int i2 = 0; i2 < this.position; i2++) {
            if (this.buffer[i2] == '\n') {
                i = 0;
            } else {
                i++;
            }
        }
        return i + 1;
    }

    @Override
    public boolean isWhitespace() throws XmlPullParserException {
        if (this.type != 4 && this.type != 7 && this.type != 5) {
            throw new XmlPullParserException(ILLEGAL_TYPE, this, null);
        }
        return this.isWhitespace;
    }

    @Override
    public String getText() {
        if (this.type < 4) {
            return null;
        }
        if (this.type == 6 && this.unresolved) {
            return null;
        }
        if (this.text == null) {
            return "";
        }
        return this.text;
    }

    @Override
    public char[] getTextCharacters(int[] iArr) {
        String text = getText();
        if (text == null) {
            iArr[0] = -1;
            iArr[1] = -1;
            return null;
        }
        char[] charArray = text.toCharArray();
        iArr[0] = 0;
        iArr[1] = charArray.length;
        return charArray;
    }

    @Override
    public String getNamespace() {
        return this.namespace;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getPrefix() {
        return this.prefix;
    }

    @Override
    public boolean isEmptyElementTag() throws XmlPullParserException {
        if (this.type != 2) {
            throw new XmlPullParserException(ILLEGAL_TYPE, this, null);
        }
        return this.degenerated;
    }

    @Override
    public int getAttributeCount() {
        return this.attributeCount;
    }

    @Override
    public String getAttributeType(int i) {
        return "CDATA";
    }

    @Override
    public boolean isAttributeDefault(int i) {
        return false;
    }

    @Override
    public String getAttributeNamespace(int i) {
        if (i >= this.attributeCount) {
            throw new IndexOutOfBoundsException();
        }
        return this.attributes[i * 4];
    }

    @Override
    public String getAttributeName(int i) {
        if (i >= this.attributeCount) {
            throw new IndexOutOfBoundsException();
        }
        return this.attributes[(i * 4) + 2];
    }

    @Override
    public String getAttributePrefix(int i) {
        if (i >= this.attributeCount) {
            throw new IndexOutOfBoundsException();
        }
        return this.attributes[(i * 4) + 1];
    }

    @Override
    public String getAttributeValue(int i) {
        if (i >= this.attributeCount) {
            throw new IndexOutOfBoundsException();
        }
        return this.attributes[(i * 4) + 3];
    }

    @Override
    public String getAttributeValue(String str, String str2) {
        for (int i = (this.attributeCount * 4) - 4; i >= 0; i -= 4) {
            if (this.attributes[i + 2].equals(str2) && (str == null || this.attributes[i].equals(str))) {
                return this.attributes[i + 3];
            }
        }
        return null;
    }

    @Override
    public int getEventType() throws XmlPullParserException {
        return this.type;
    }

    @Override
    public int nextTag() throws XmlPullParserException, IOException {
        next();
        if (this.type == 4 && this.isWhitespace) {
            next();
        }
        if (this.type != 3 && this.type != 2) {
            throw new XmlPullParserException("unexpected type", this, null);
        }
        return this.type;
    }

    @Override
    public void require(int i, String str, String str2) throws XmlPullParserException, IOException {
        if (i != this.type || ((str != null && !str.equals(getNamespace())) || (str2 != null && !str2.equals(getName())))) {
            throw new XmlPullParserException("expected: " + TYPES[i] + " {" + str + "}" + str2, this, null);
        }
    }

    @Override
    public String nextText() throws XmlPullParserException, IOException {
        String text;
        if (this.type != 2) {
            throw new XmlPullParserException("precondition: START_TAG", this, null);
        }
        next();
        if (this.type == 4) {
            text = getText();
            next();
        } else {
            text = "";
        }
        if (this.type != 3) {
            throw new XmlPullParserException("END_TAG expected", this, null);
        }
        return text;
    }

    @Override
    public void setFeature(String str, boolean z) throws XmlPullParserException {
        if (XmlPullParser.FEATURE_PROCESS_NAMESPACES.equals(str)) {
            this.processNsp = z;
            return;
        }
        if (XmlPullParser.FEATURE_PROCESS_DOCDECL.equals(str)) {
            this.processDocDecl = z;
        } else {
            if (FEATURE_RELAXED.equals(str)) {
                this.relaxed = z;
                return;
            }
            throw new XmlPullParserException("unsupported feature: " + str, this, null);
        }
    }

    @Override
    public void setProperty(String str, Object obj) throws XmlPullParserException {
        if (str.equals(PROPERTY_LOCATION)) {
            this.location = String.valueOf(obj);
            return;
        }
        throw new XmlPullParserException("unsupported property: " + str);
    }

    static class ContentSource {
        private final char[] buffer;
        private final int limit;
        private final ContentSource next;
        private final int position;

        ContentSource(ContentSource contentSource, char[] cArr, int i, int i2) {
            this.next = contentSource;
            this.buffer = cArr;
            this.position = i;
            this.limit = i2;
        }
    }

    private void pushContentSource(char[] cArr) {
        this.nextContentSource = new ContentSource(this.nextContentSource, this.buffer, this.position, this.limit);
        this.buffer = cArr;
        this.position = 0;
        this.limit = cArr.length;
    }

    private void popContentSource() {
        this.buffer = this.nextContentSource.buffer;
        this.position = this.nextContentSource.position;
        this.limit = this.nextContentSource.limit;
        this.nextContentSource = this.nextContentSource.next;
    }
}
