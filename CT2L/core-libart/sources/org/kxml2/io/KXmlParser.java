package org.kxml2.io;

import dalvik.bytecode.Opcodes;
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
        SINGLE_QUOTE = new char[]{'\''};
        DOUBLE_QUOTE = new char[]{'\"'};
    }

    public void keepNamespaceAttributes() {
        this.keepNamespaceAttributes = true;
    }

    private boolean adjustNsp() throws XmlPullParserException {
        String prefix;
        String attrName;
        boolean any = false;
        int i = 0;
        while (i < (this.attributeCount << 2)) {
            String attrName2 = this.attributes[i + 2];
            int cut = attrName2.indexOf(58);
            if (cut != -1) {
                prefix = attrName2.substring(0, cut);
                attrName = attrName2.substring(cut + 1);
            } else if (!attrName2.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
                i += 4;
            } else {
                prefix = attrName2;
                attrName = null;
            }
            if (!prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
                any = true;
            } else {
                int[] iArr = this.nspCounts;
                int i2 = this.depth;
                int i3 = iArr[i2];
                iArr[i2] = i3 + 1;
                int j = i3 << 1;
                this.nspStack = ensureCapacity(this.nspStack, j + 2);
                this.nspStack[j] = attrName;
                this.nspStack[j + 1] = this.attributes[i + 3];
                if (attrName != null && this.attributes[i + 3].isEmpty()) {
                    checkRelaxed("illegal empty namespace");
                }
                if (this.keepNamespaceAttributes) {
                    this.attributes[i] = XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
                    any = true;
                } else {
                    String[] strArr = this.attributes;
                    int i4 = this.attributeCount - 1;
                    this.attributeCount = i4;
                    System.arraycopy(this.attributes, i + 4, strArr, i, (i4 << 2) - i);
                    i -= 4;
                }
            }
            i += 4;
        }
        if (any) {
            for (int i5 = (this.attributeCount << 2) - 4; i5 >= 0; i5 -= 4) {
                String attrName3 = this.attributes[i5 + 2];
                int cut2 = attrName3.indexOf(58);
                if (cut2 == 0 && !this.relaxed) {
                    throw new RuntimeException("illegal attribute name: " + attrName3 + " at " + this);
                }
                if (cut2 != -1) {
                    String attrPrefix = attrName3.substring(0, cut2);
                    String attrName4 = attrName3.substring(cut2 + 1);
                    String attrNs = getNamespace(attrPrefix);
                    if (attrNs == null && !this.relaxed) {
                        throw new RuntimeException("Undefined Prefix: " + attrPrefix + " in " + this);
                    }
                    this.attributes[i5] = attrNs;
                    this.attributes[i5 + 1] = attrPrefix;
                    this.attributes[i5 + 2] = attrName4;
                }
            }
        }
        int cut3 = this.name.indexOf(58);
        if (cut3 == 0) {
            checkRelaxed("illegal tag name: " + this.name);
        }
        if (cut3 != -1) {
            this.prefix = this.name.substring(0, cut3);
            this.name = this.name.substring(cut3 + 1);
        }
        this.namespace = getNamespace(this.prefix);
        if (this.namespace == null) {
            if (this.prefix != null) {
                checkRelaxed("undefined prefix: " + this.prefix);
            }
            this.namespace = "";
        }
        return any;
    }

    private String[] ensureCapacity(String[] arr, int required) {
        if (arr.length < required) {
            String[] bigger = new String[required + 16];
            System.arraycopy(arr, 0, bigger, 0, arr.length);
            return bigger;
        }
        return arr;
    }

    private void checkRelaxed(String errorMessage) throws XmlPullParserException {
        if (!this.relaxed) {
            throw new XmlPullParserException(errorMessage, this, null);
        }
        if (this.error == null) {
            this.error = "Error: " + errorMessage;
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

    private int next(boolean justOneToken) throws XmlPullParserException, IOException {
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
            if (justOneToken) {
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
        boolean throwOnResolveFailure = !justOneToken;
        while (true) {
            switch (this.type) {
                case 1:
                    return this.type;
                case 2:
                    parseStartTag(false, throwOnResolveFailure);
                    return this.type;
                case 3:
                    readEndTag();
                    return this.type;
                case 4:
                    break;
                case 5:
                    read(START_CDATA);
                    this.text = readUntil(END_CDATA, true);
                    if (this.depth == 0 || (this.type != 6 && this.type != 4 && this.type != 5)) {
                        if (!justOneToken) {
                            return this.type;
                        }
                        if (this.type == 7) {
                            this.text = null;
                        }
                        int peek = peekType(false);
                        if (this.text != null && !this.text.isEmpty() && peek < 4) {
                            this.type = 4;
                            return this.type;
                        }
                        this.type = peek;
                    }
                    break;
                case 6:
                    if (justOneToken) {
                        StringBuilder entityTextBuilder = new StringBuilder();
                        readEntity(entityTextBuilder, true, throwOnResolveFailure, ValueContext.TEXT);
                        this.text = entityTextBuilder.toString();
                    }
                    if (this.depth == 0) {
                    }
                    if (!justOneToken) {
                    }
                    break;
                case 7:
                default:
                    throw new XmlPullParserException("Unexpected token", this, null);
                case 8:
                    read(START_PROCESSING_INSTRUCTION);
                    String processingInstruction = readUntil(END_PROCESSING_INSTRUCTION, justOneToken);
                    if (justOneToken) {
                        this.text = processingInstruction;
                    }
                    if (this.depth == 0) {
                    }
                    if (!justOneToken) {
                    }
                    break;
                case 9:
                    String commentText = readComment(justOneToken);
                    if (justOneToken) {
                        this.text = commentText;
                    }
                    if (this.depth == 0) {
                    }
                    if (!justOneToken) {
                    }
                    break;
                case 10:
                    readDoctype(justOneToken);
                    if (this.parsedTopLevelStartTag) {
                        throw new XmlPullParserException("Unexpected token", this, null);
                    }
                    if (this.depth == 0) {
                    }
                    if (!justOneToken) {
                    }
                    break;
            }
            this.text = readValue('<', !justOneToken, throwOnResolveFailure, ValueContext.TEXT);
            if (this.depth == 0 && this.isWhitespace) {
                this.type = 7;
            }
            if (this.depth == 0) {
            }
            if (!justOneToken) {
            }
        }
    }

    private String readUntil(char[] delimiter, boolean returnText) throws XmlPullParserException, IOException {
        int start = this.position;
        StringBuilder result = null;
        if (returnText && this.text != null) {
            result = new StringBuilder();
            result.append(this.text);
        }
        while (true) {
            if (this.position + delimiter.length > this.limit) {
                if (start < this.position && returnText) {
                    if (result == null) {
                        result = new StringBuilder();
                    }
                    result.append(this.buffer, start, this.position - start);
                }
                if (!fillBuffer(delimiter.length)) {
                    checkRelaxed(UNEXPECTED_EOF);
                    this.type = 9;
                    return null;
                }
                start = this.position;
            }
            for (int i = 0; i < delimiter.length; i++) {
                if (this.buffer[this.position + i] != delimiter[i]) {
                    break;
                }
            }
            int end = this.position;
            this.position += delimiter.length;
            if (!returnText) {
                return null;
            }
            if (result == null) {
                return this.stringPool.get(this.buffer, start, end - start);
            }
            result.append(this.buffer, start, end - start);
            return result.toString();
            this.position++;
        }
    }

    private void readXmlDeclaration() throws XmlPullParserException, IOException {
        if (this.bufferStartLine != 0 || this.bufferStartColumn != 0 || this.position != 0) {
            checkRelaxed("processing instructions must not start with xml");
        }
        read(START_PROCESSING_INSTRUCTION);
        parseStartTag(true, true);
        if (this.attributeCount < 1 || !OutputKeys.VERSION.equals(this.attributes[2])) {
            checkRelaxed("version expected");
        }
        this.version = this.attributes[3];
        int pos = 1;
        if (1 < this.attributeCount && OutputKeys.ENCODING.equals(this.attributes[6])) {
            this.encoding = this.attributes[7];
            pos = 1 + 1;
        }
        if (pos < this.attributeCount && OutputKeys.STANDALONE.equals(this.attributes[(pos * 4) + 2])) {
            String st = this.attributes[(pos * 4) + 3];
            if ("yes".equals(st)) {
                this.standalone = Boolean.TRUE;
            } else if ("no".equals(st)) {
                this.standalone = Boolean.FALSE;
            } else {
                checkRelaxed("illegal standalone value: " + st);
            }
            pos++;
        }
        if (pos != this.attributeCount) {
            checkRelaxed("unexpected attributes in XML declaration");
        }
        this.isWhitespace = true;
        this.text = null;
    }

    private String readComment(boolean returnText) throws XmlPullParserException, IOException {
        read(START_COMMENT);
        if (this.relaxed) {
            return readUntil(END_COMMENT, returnText);
        }
        String until = readUntil(COMMENT_DOUBLE_DASH, returnText);
        if (peekCharacter() != 62) {
            throw new XmlPullParserException("Comments may not contain --", this, null);
        }
        this.position++;
        return until;
    }

    private void readDoctype(boolean saveDtdText) throws XmlPullParserException, IOException {
        read(START_DOCTYPE);
        int startPosition = -1;
        if (saveDtdText) {
            this.bufferCapture = new StringBuilder();
            startPosition = this.position;
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
        } finally {
            if (saveDtdText) {
                this.bufferCapture.append(this.buffer, 0, this.position);
                this.bufferCapture.delete(0, startPosition);
                this.text = this.bufferCapture.toString();
                this.bufferCapture = null;
            }
        }
    }

    private boolean readExternalId(boolean requireSystemName, boolean assignFields) throws XmlPullParserException, IOException {
        int delimiter;
        skip();
        int c = peekCharacter();
        if (c == 83) {
            read(SYSTEM);
        } else {
            if (c != 80) {
                return false;
            }
            read(PUBLIC);
            skip();
            if (assignFields) {
                this.publicId = readQuotedId(true);
            } else {
                readQuotedId(false);
            }
        }
        skip();
        if (!requireSystemName && (delimiter = peekCharacter()) != 34 && delimiter != 39) {
            return true;
        }
        if (assignFields) {
            this.systemId = readQuotedId(true);
        } else {
            readQuotedId(false);
        }
        return true;
    }

    private String readQuotedId(boolean returnText) throws XmlPullParserException, IOException {
        char[] delimiter;
        int quote = peekCharacter();
        if (quote == 34) {
            delimiter = DOUBLE_QUOTE;
        } else if (quote == 39) {
            delimiter = SINGLE_QUOTE;
        } else {
            throw new XmlPullParserException("Expected a quoted string", this, null);
        }
        this.position++;
        return readUntil(delimiter, returnText);
    }

    private void readInternalSubset() throws XmlPullParserException, IOException {
        read('[');
        while (true) {
            skip();
            if (peekCharacter() == 93) {
                this.position++;
                return;
            }
            int declarationType = peekType(true);
            switch (declarationType) {
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
        int c = peekCharacter();
        if (c == 40) {
            int depth = 0;
            do {
                if (c == 40) {
                    depth++;
                } else if (c == 41) {
                    depth--;
                } else if (c == -1) {
                    throw new XmlPullParserException("Unterminated element content spec", this, null);
                }
                this.position++;
                c = peekCharacter();
            } while (depth > 0);
            if (c == 42 || c == 63 || c == 43) {
                this.position++;
                return;
            }
            return;
        }
        if (c == EMPTY[0]) {
            read(EMPTY);
        } else {
            if (c == ANY[0]) {
                read(ANY);
                return;
            }
            throw new XmlPullParserException("Expected element content spec", this, null);
        }
    }

    private void readAttributeListDeclaration() throws XmlPullParserException, IOException {
        read(START_ATTLIST);
        skip();
        String elementName = readName();
        while (true) {
            skip();
            int c = peekCharacter();
            if (c == 62) {
                this.position++;
                return;
            }
            String attributeName = readName();
            skip();
            if (this.position + 1 >= this.limit && !fillBuffer(2)) {
                throw new XmlPullParserException("Malformed attribute list", this, null);
            }
            if (this.buffer[this.position] == NOTATION[0] && this.buffer[this.position + 1] == NOTATION[1]) {
                read(NOTATION);
                skip();
            }
            int c2 = peekCharacter();
            if (c2 == 40) {
                this.position++;
                while (true) {
                    skip();
                    readName();
                    skip();
                    int c3 = peekCharacter();
                    if (c3 == 41) {
                        this.position++;
                        break;
                    } else if (c3 == 124) {
                        this.position++;
                    } else {
                        throw new XmlPullParserException("Malformed attribute type", this, null);
                    }
                }
            } else {
                readName();
            }
            skip();
            int c4 = peekCharacter();
            if (c4 == 35) {
                this.position++;
                int c5 = peekCharacter();
                if (c5 == 82) {
                    read(REQUIRED);
                } else if (c5 == 73) {
                    read(IMPLIED);
                } else if (c5 == 70) {
                    read(FIXED);
                } else {
                    throw new XmlPullParserException("Malformed attribute type", this, null);
                }
                skip();
                c4 = peekCharacter();
            }
            if (c4 == 34 || c4 == 39) {
                this.position++;
                String value = readValue((char) c4, true, true, ValueContext.ATTRIBUTE);
                if (peekCharacter() == c4) {
                    this.position++;
                }
                defineAttributeDefault(elementName, attributeName, value);
            }
        }
    }

    private void defineAttributeDefault(String elementName, String attributeName, String value) {
        if (this.defaultAttributes == null) {
            this.defaultAttributes = new HashMap();
        }
        Map<String, String> elementAttributes = this.defaultAttributes.get(elementName);
        if (elementAttributes == null) {
            elementAttributes = new HashMap<>();
            this.defaultAttributes.put(elementName, elementAttributes);
        }
        elementAttributes.put(attributeName, value);
    }

    private void readEntityDeclaration() throws XmlPullParserException, IOException {
        String entityValue;
        read(START_ENTITY);
        boolean generalEntity = true;
        skip();
        if (peekCharacter() == 37) {
            generalEntity = false;
            this.position++;
            skip();
        }
        String name = readName();
        skip();
        int quote = peekCharacter();
        if (quote == 34 || quote == 39) {
            this.position++;
            entityValue = readValue((char) quote, true, false, ValueContext.ENTITY_DECLARATION);
            if (peekCharacter() == quote) {
                this.position++;
            }
        } else if (readExternalId(true, false)) {
            entityValue = "";
            skip();
            if (peekCharacter() == NDATA[0]) {
                read(NDATA);
                skip();
                readName();
            }
        } else {
            throw new XmlPullParserException("Expected entity value or external ID", this, null);
        }
        if (generalEntity && this.processDocDecl) {
            if (this.documentEntities == null) {
                this.documentEntities = new HashMap();
            }
            this.documentEntities.put(name, entityValue.toCharArray());
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
        int sp = (this.depth - 1) * 4;
        if (this.depth == 0) {
            checkRelaxed("read end tag " + this.name + " with no tags open");
            this.type = 9;
        } else if (this.name.equals(this.elementStack[sp + 3])) {
            this.namespace = this.elementStack[sp];
            this.prefix = this.elementStack[sp + 1];
            this.name = this.elementStack[sp + 2];
        } else if (!this.relaxed) {
            throw new XmlPullParserException("expected: /" + this.elementStack[sp + 3] + " read: " + this.name, this, null);
        }
    }

    private int peekType(boolean inDeclaration) throws XmlPullParserException, IOException {
        if (this.position >= this.limit && !fillBuffer(1)) {
            return 1;
        }
        switch (this.buffer[this.position]) {
            case Opcodes.OP_FILLED_NEW_ARRAY_RANGE:
                return inDeclaration ? 15 : 4;
            case '&':
                return 6;
            case Opcodes.OP_IF_GTZ:
                if (this.position + 3 >= this.limit && !fillBuffer(4)) {
                    throw new XmlPullParserException("Dangling <", this, null);
                }
                switch (this.buffer[this.position + 1]) {
                    case Opcodes.OP_ARRAY_LENGTH:
                        switch (this.buffer[this.position + 2]) {
                            case Opcodes.OP_CMPL_FLOAT:
                                return 9;
                            case 'A':
                                return 13;
                            case Opcodes.OP_AGET:
                                return 10;
                            case Opcodes.OP_AGET_WIDE:
                                switch (this.buffer[this.position + 3]) {
                                    case Opcodes.OP_APUT_WIDE:
                                        return 11;
                                    case Opcodes.OP_APUT_BOOLEAN:
                                        return 12;
                                }
                            case Opcodes.OP_APUT_BOOLEAN:
                                return 14;
                            case '[':
                                return 5;
                        }
                        throw new XmlPullParserException("Unexpected <!", this, null);
                    case Opcodes.OP_CMPL_DOUBLE:
                        return 3;
                    case '?':
                        if ((this.position + 5 < this.limit || fillBuffer(6)) && ((this.buffer[this.position + 2] == 'x' || this.buffer[this.position + 2] == 'X') && ((this.buffer[this.position + 3] == 'm' || this.buffer[this.position + 3] == 'M') && ((this.buffer[this.position + 4] == 'l' || this.buffer[this.position + 4] == 'L') && this.buffer[this.position + 5] == ' ')))) {
                            return XML_DECLARATION;
                        }
                        return 8;
                    default:
                        return 2;
                }
            default:
                return 4;
        }
    }

    private void parseStartTag(boolean xmldecl, boolean throwOnResolveFailure) throws XmlPullParserException, IOException {
        if (!xmldecl) {
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
            if (xmldecl) {
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
            String attrName = readName();
            int i = this.attributeCount;
            this.attributeCount = i + 1;
            int i2 = i * 4;
            this.attributes = ensureCapacity(this.attributes, i2 + 4);
            this.attributes[i2] = "";
            this.attributes[i2 + 1] = null;
            this.attributes[i2 + 2] = attrName;
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
                char delimiter = this.buffer[this.position];
                if (delimiter == '\'' || delimiter == '\"') {
                    this.position++;
                } else if (this.relaxed) {
                    delimiter = ' ';
                } else {
                    throw new XmlPullParserException("attr value delimiter missing!", this, null);
                }
                this.attributes[i2 + 3] = readValue(delimiter, true, throwOnResolveFailure, ValueContext.ATTRIBUTE);
                if (delimiter != ' ' && peekCharacter() == delimiter) {
                    this.position++;
                }
            } else if (this.relaxed) {
                this.attributes[i2 + 3] = attrName;
            } else {
                checkRelaxed("Attr.value missing f. " + attrName);
                this.attributes[i2 + 3] = attrName;
            }
        }
    }

    private void readEntity(StringBuilder out, boolean isEntityToken, boolean throwOnResolveFailure, ValueContext valueContext) throws XmlPullParserException, IOException {
        char[] resolved;
        int start = out.length();
        char[] cArr = this.buffer;
        int i = this.position;
        this.position = i + 1;
        if (cArr[i] != '&') {
            throw new AssertionError();
        }
        out.append('&');
        while (true) {
            int c = peekCharacter();
            if (c == 59) {
                out.append(';');
                this.position++;
                String code = out.substring(start + 1, out.length() - 1);
                if (isEntityToken) {
                    this.name = code;
                }
                if (code.startsWith("#")) {
                    try {
                        int c2 = code.startsWith("#x") ? Integer.parseInt(code.substring(2), 16) : Integer.parseInt(code.substring(1));
                        out.delete(start, out.length());
                        out.appendCodePoint(c2);
                        this.unresolved = false;
                        return;
                    } catch (NumberFormatException e) {
                        throw new XmlPullParserException("Invalid character reference: &" + code);
                    } catch (IllegalArgumentException e2) {
                        throw new XmlPullParserException("Invalid character reference: &" + code);
                    }
                }
                if (valueContext != ValueContext.ENTITY_DECLARATION) {
                    String defaultEntity = DEFAULT_ENTITIES.get(code);
                    if (defaultEntity != null) {
                        out.delete(start, out.length());
                        this.unresolved = false;
                        out.append(defaultEntity);
                        return;
                    }
                    if (this.documentEntities != null && (resolved = this.documentEntities.get(code)) != null) {
                        out.delete(start, out.length());
                        this.unresolved = false;
                        if (this.processDocDecl) {
                            pushContentSource(resolved);
                            return;
                        } else {
                            out.append(resolved);
                            return;
                        }
                    }
                    if (this.systemId != null) {
                        out.delete(start, out.length());
                        return;
                    }
                    this.unresolved = true;
                    if (throwOnResolveFailure) {
                        checkRelaxed("unresolved: &" + code + ";");
                        return;
                    }
                    return;
                }
                return;
            }
            if (c >= 128 || ((c >= 48 && c <= 57) || ((c >= 97 && c <= 122) || ((c >= 65 && c <= 90) || c == 95 || c == 45 || c == 35)))) {
                this.position++;
                out.append((char) c);
            } else {
                if (!this.relaxed) {
                    throw new XmlPullParserException("unterminated entity ref", this, null);
                }
                return;
            }
        }
    }

    private String readValue(char delimiter, boolean resolveEntities, boolean throwOnResolveFailure, ValueContext valueContext) throws XmlPullParserException, IOException {
        int start = this.position;
        StringBuilder result = null;
        if (valueContext == ValueContext.TEXT && this.text != null) {
            result = new StringBuilder();
            result.append(this.text);
        }
        while (true) {
            if (this.position >= this.limit) {
                if (start < this.position) {
                    if (result == null) {
                        result = new StringBuilder();
                    }
                    result.append(this.buffer, start, this.position - start);
                }
                if (!fillBuffer(1)) {
                    return result != null ? result.toString() : "";
                }
                start = this.position;
            }
            char c = this.buffer[this.position];
            if (c == delimiter || ((delimiter == ' ' && (c <= ' ' || c == '>')) || (c == '&' && !resolveEntities))) {
                break;
            }
            if (c != '\r' && ((c != '\n' || valueContext != ValueContext.ATTRIBUTE) && c != '&' && c != '<' && ((c != ']' || valueContext != ValueContext.TEXT) && (c != '%' || valueContext != ValueContext.ENTITY_DECLARATION)))) {
                this.isWhitespace = (c <= ' ') & this.isWhitespace;
                this.position++;
            } else {
                if (result == null) {
                    result = new StringBuilder();
                }
                result.append(this.buffer, start, this.position - start);
                if (c == '\r') {
                    if ((this.position + 1 < this.limit || fillBuffer(2)) && this.buffer[this.position + 1] == '\n') {
                        this.position++;
                    }
                    c = valueContext == ValueContext.ATTRIBUTE ? ' ' : '\n';
                } else if (c == '\n') {
                    c = ' ';
                } else if (c == '&') {
                    this.isWhitespace = false;
                    readEntity(result, false, throwOnResolveFailure, valueContext);
                    start = this.position;
                } else if (c == '<') {
                    if (valueContext == ValueContext.ATTRIBUTE) {
                        checkRelaxed("Illegal: \"<\" inside attribute value");
                    }
                    this.isWhitespace = false;
                } else if (c == ']') {
                    if ((this.position + 2 < this.limit || fillBuffer(3)) && this.buffer[this.position + 1] == ']' && this.buffer[this.position + 2] == '>') {
                        checkRelaxed("Illegal: \"]]>\" outside CDATA section");
                    }
                    this.isWhitespace = false;
                } else {
                    if (c == '%') {
                        throw new XmlPullParserException("This parser doesn't support parameter entities", this, null);
                    }
                    throw new AssertionError();
                }
                this.position++;
                result.append(c);
                start = this.position;
            }
        }
    }

    private void read(char expected) throws XmlPullParserException, IOException {
        int c = peekCharacter();
        if (c != expected) {
            checkRelaxed("expected: '" + expected + "' actual: '" + ((char) c) + "'");
            if (c == -1) {
                return;
            }
        }
        this.position++;
    }

    private void read(char[] chars) throws XmlPullParserException, IOException {
        if (this.position + chars.length > this.limit && !fillBuffer(chars.length)) {
            checkRelaxed("expected: '" + new String(chars) + "' but was EOF");
            return;
        }
        for (int i = 0; i < chars.length; i++) {
            if (this.buffer[this.position + i] != chars[i]) {
                checkRelaxed("expected: \"" + new String(chars) + "\" but was \"" + new String(this.buffer, this.position, chars.length) + "...\"");
            }
        }
        this.position += chars.length;
    }

    private int peekCharacter() throws XmlPullParserException, IOException {
        if (this.position < this.limit || fillBuffer(1)) {
            return this.buffer[this.position];
        }
        return -1;
    }

    private boolean fillBuffer(int minimum) throws XmlPullParserException, IOException {
        while (this.nextContentSource != null) {
            if (this.position < this.limit) {
                throw new XmlPullParserException("Unbalanced entity!", this, null);
            }
            popContentSource();
            if (this.limit - this.position >= minimum) {
                return true;
            }
        }
        for (int i = 0; i < this.position; i++) {
            if (this.buffer[i] == '\n') {
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
            int total = this.reader.read(this.buffer, this.limit, this.buffer.length - this.limit);
            if (total == -1) {
                return false;
            }
            this.limit += total;
        } while (this.limit < minimum);
        return true;
    }

    private String readName() throws XmlPullParserException, IOException {
        if (this.position >= this.limit && !fillBuffer(1)) {
            checkRelaxed("name expected");
            return "";
        }
        int start = this.position;
        StringBuilder result = null;
        char c = this.buffer[this.position];
        if ((c >= 'a' && c <= 'z') || ((c >= 'A' && c <= 'Z') || c == '_' || c == ':' || c >= 192 || this.relaxed)) {
            this.position++;
            while (true) {
                if (this.position >= this.limit) {
                    if (result == null) {
                        result = new StringBuilder();
                    }
                    result.append(this.buffer, start, this.position - start);
                    if (!fillBuffer(1)) {
                        return result.toString();
                    }
                    start = this.position;
                }
                char c2 = this.buffer[this.position];
                if ((c2 >= 'a' && c2 <= 'z') || ((c2 >= 'A' && c2 <= 'Z') || ((c2 >= '0' && c2 <= '9') || c2 == '_' || c2 == '-' || c2 == ':' || c2 == '.' || c2 >= 183))) {
                    this.position++;
                } else {
                    if (result == null) {
                        return this.stringPool.get(this.buffer, start, this.position - start);
                    }
                    result.append(this.buffer, start, this.position - start);
                    return result.toString();
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
        this.name = null;
        this.namespace = null;
        this.degenerated = false;
        this.attributeCount = -1;
        this.encoding = null;
        this.version = null;
        this.standalone = null;
        if (reader != null) {
            this.position = 0;
            this.limit = 0;
            this.bufferStartLine = 0;
            this.bufferStartColumn = 0;
            this.depth = 0;
            this.documentEntities = null;
        }
    }

    @Override
    public void setInput(InputStream is, String charset) throws XmlPullParserException {
        int i;
        this.position = 0;
        this.limit = 0;
        boolean detectCharset = charset == null;
        if (is == null) {
            throw new IllegalArgumentException("is == null");
        }
        if (detectCharset) {
            int firstFourBytes = 0;
            while (this.limit < 4 && (i = is.read()) != -1) {
                try {
                    firstFourBytes = (firstFourBytes << 8) | i;
                    char[] cArr = this.buffer;
                    int i2 = this.limit;
                    this.limit = i2 + 1;
                    cArr[i2] = (char) i;
                } catch (Exception e) {
                    throw new XmlPullParserException("Invalid stream or encoding: " + e, this, e);
                }
            }
            if (this.limit == 4) {
                switch (firstFourBytes) {
                    case -131072:
                        charset = "UTF-32LE";
                        this.limit = 0;
                        break;
                    case Opcodes.OP_IF_GTZ:
                        charset = "UTF-32BE";
                        this.buffer[0] = '<';
                        this.limit = 1;
                        break;
                    case 65279:
                        charset = "UTF-32BE";
                        this.limit = 0;
                        break;
                    case 3932223:
                        charset = "UTF-16BE";
                        this.buffer[0] = '<';
                        this.buffer[1] = '?';
                        this.limit = 2;
                        break;
                    case 1006632960:
                        charset = "UTF-32LE";
                        this.buffer[0] = '<';
                        this.limit = 1;
                        break;
                    case 1006649088:
                        charset = "UTF-16LE";
                        this.buffer[0] = '<';
                        this.buffer[1] = '?';
                        this.limit = 2;
                        break;
                    case 1010792557:
                        while (true) {
                            int i3 = is.read();
                            if (i3 == -1) {
                                break;
                            } else {
                                char[] cArr2 = this.buffer;
                                int i4 = this.limit;
                                this.limit = i4 + 1;
                                cArr2[i4] = (char) i3;
                                if (i3 == 62) {
                                    String s = new String(this.buffer, 0, this.limit);
                                    int i0 = s.indexOf(OutputKeys.ENCODING);
                                    if (i0 != -1) {
                                        int i02 = i0;
                                        while (s.charAt(i02) != '\"' && s.charAt(i02) != '\'') {
                                            i02++;
                                        }
                                        int i03 = i02 + 1;
                                        char deli = s.charAt(i02);
                                        int i1 = s.indexOf(deli, i03);
                                        charset = s.substring(i03, i1);
                                    }
                                    break;
                                }
                            }
                        }
                        break;
                    default:
                        if (((-65536) & firstFourBytes) == -16842752) {
                            charset = "UTF-16BE";
                            this.buffer[0] = (char) ((this.buffer[2] << '\b') | this.buffer[3]);
                            this.limit = 1;
                        } else if (((-65536) & firstFourBytes) == -131072) {
                            charset = "UTF-16LE";
                            this.buffer[0] = (char) ((this.buffer[3] << '\b') | this.buffer[2]);
                            this.limit = 1;
                        } else if ((firstFourBytes & (-256)) == -272908544) {
                            charset = "UTF-8";
                            this.buffer[0] = this.buffer[3];
                            this.limit = 1;
                        }
                        break;
                }
            }
        }
        if (charset == null) {
            charset = "UTF-8";
        }
        int savedLimit = this.limit;
        setInput(new InputStreamReader(is, charset));
        this.encoding = charset;
        this.limit = savedLimit;
        if (!detectCharset && peekCharacter() == 65279) {
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
    public boolean getFeature(String feature) {
        if (XmlPullParser.FEATURE_PROCESS_NAMESPACES.equals(feature)) {
            return this.processNsp;
        }
        if (FEATURE_RELAXED.equals(feature)) {
            return this.relaxed;
        }
        if (XmlPullParser.FEATURE_PROCESS_DOCDECL.equals(feature)) {
            return this.processDocDecl;
        }
        return false;
    }

    @Override
    public String getInputEncoding() {
        return this.encoding;
    }

    @Override
    public void defineEntityReplacementText(String entity, String value) throws XmlPullParserException {
        if (this.processDocDecl) {
            throw new IllegalStateException("Entity replacement text may not be defined with DOCTYPE processing enabled.");
        }
        if (this.reader == null) {
            throw new IllegalStateException("Entity replacement text must be defined after setInput()");
        }
        if (this.documentEntities == null) {
            this.documentEntities = new HashMap();
        }
        this.documentEntities.put(entity, value.toCharArray());
    }

    @Override
    public Object getProperty(String property) {
        if (property.equals(PROPERTY_XMLDECL_VERSION)) {
            return this.version;
        }
        if (property.equals(PROPERTY_XMLDECL_STANDALONE)) {
            return this.standalone;
        }
        if (property.equals(PROPERTY_LOCATION)) {
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
    public int getNamespaceCount(int depth) {
        if (depth > this.depth) {
            throw new IndexOutOfBoundsException();
        }
        return this.nspCounts[depth];
    }

    @Override
    public String getNamespacePrefix(int pos) {
        return this.nspStack[pos * 2];
    }

    @Override
    public String getNamespaceUri(int pos) {
        return this.nspStack[(pos * 2) + 1];
    }

    @Override
    public String getNamespace(String prefix) {
        if (XMLConstants.XML_NS_PREFIX.equals(prefix)) {
            return "http://www.w3.org/XML/1998/namespace";
        }
        if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
            return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
        }
        for (int i = (getNamespaceCount(this.depth) << 1) - 2; i >= 0; i -= 2) {
            if (prefix == null) {
                if (this.nspStack[i] == null) {
                    return this.nspStack[i + 1];
                }
            } else if (prefix.equals(this.nspStack[i])) {
                return this.nspStack[i + 1];
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
        StringBuilder buf = new StringBuilder(this.type < TYPES.length ? TYPES[this.type] : "unknown");
        buf.append(' ');
        if (this.type == 2 || this.type == 3) {
            if (this.degenerated) {
                buf.append("(empty) ");
            }
            buf.append('<');
            if (this.type == 3) {
                buf.append('/');
            }
            if (this.prefix != null) {
                buf.append("{" + this.namespace + "}" + this.prefix + ":");
            }
            buf.append(this.name);
            int cnt = this.attributeCount * 4;
            for (int i = 0; i < cnt; i += 4) {
                buf.append(' ');
                if (this.attributes[i + 1] != null) {
                    buf.append("{" + this.attributes[i] + "}" + this.attributes[i + 1] + ":");
                }
                buf.append(this.attributes[i + 2] + "='" + this.attributes[i + 3] + "'");
            }
            buf.append('>');
        } else if (this.type != 7) {
            if (this.type != 4) {
                buf.append(getText());
            } else if (this.isWhitespace) {
                buf.append("(whitespace)");
            } else {
                String text = getText();
                if (text.length() > 16) {
                    text = text.substring(0, 16) + "...";
                }
                buf.append(text);
            }
        }
        buf.append("@" + getLineNumber() + ":" + getColumnNumber());
        if (this.location != null) {
            buf.append(" in ");
            buf.append(this.location);
        } else if (this.reader != null) {
            buf.append(" in ");
            buf.append(this.reader.toString());
        }
        return buf.toString();
    }

    @Override
    public int getLineNumber() {
        int result = this.bufferStartLine;
        for (int i = 0; i < this.position; i++) {
            if (this.buffer[i] == '\n') {
                result++;
            }
        }
        return result + 1;
    }

    @Override
    public int getColumnNumber() {
        int result = this.bufferStartColumn;
        for (int i = 0; i < this.position; i++) {
            if (this.buffer[i] == '\n') {
                result = 0;
            } else {
                result++;
            }
        }
        return result + 1;
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
        if (this.type < 4 || (this.type == 6 && this.unresolved)) {
            return null;
        }
        if (this.text == null) {
            return "";
        }
        return this.text;
    }

    @Override
    public char[] getTextCharacters(int[] poslen) {
        String text = getText();
        if (text == null) {
            poslen[0] = -1;
            poslen[1] = -1;
            return null;
        }
        char[] result = text.toCharArray();
        poslen[0] = 0;
        poslen[1] = result.length;
        return result;
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
    public String getAttributeType(int index) {
        return "CDATA";
    }

    @Override
    public boolean isAttributeDefault(int index) {
        return false;
    }

    @Override
    public String getAttributeNamespace(int index) {
        if (index >= this.attributeCount) {
            throw new IndexOutOfBoundsException();
        }
        return this.attributes[index * 4];
    }

    @Override
    public String getAttributeName(int index) {
        if (index >= this.attributeCount) {
            throw new IndexOutOfBoundsException();
        }
        return this.attributes[(index * 4) + 2];
    }

    @Override
    public String getAttributePrefix(int index) {
        if (index >= this.attributeCount) {
            throw new IndexOutOfBoundsException();
        }
        return this.attributes[(index * 4) + 1];
    }

    @Override
    public String getAttributeValue(int index) {
        if (index >= this.attributeCount) {
            throw new IndexOutOfBoundsException();
        }
        return this.attributes[(index * 4) + 3];
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        for (int i = (this.attributeCount * 4) - 4; i >= 0; i -= 4) {
            if (this.attributes[i + 2].equals(name) && (namespace == null || this.attributes[i].equals(namespace))) {
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
    public void require(int type, String namespace, String name) throws XmlPullParserException, IOException {
        if (type != this.type || ((namespace != null && !namespace.equals(getNamespace())) || (name != null && !name.equals(getName())))) {
            throw new XmlPullParserException("expected: " + TYPES[type] + " {" + namespace + "}" + name, this, null);
        }
    }

    @Override
    public String nextText() throws XmlPullParserException, IOException {
        String result;
        if (this.type != 2) {
            throw new XmlPullParserException("precondition: START_TAG", this, null);
        }
        next();
        if (this.type == 4) {
            result = getText();
            next();
        } else {
            result = "";
        }
        if (this.type != 3) {
            throw new XmlPullParserException("END_TAG expected", this, null);
        }
        return result;
    }

    @Override
    public void setFeature(String feature, boolean value) throws XmlPullParserException {
        if (XmlPullParser.FEATURE_PROCESS_NAMESPACES.equals(feature)) {
            this.processNsp = value;
        } else if (XmlPullParser.FEATURE_PROCESS_DOCDECL.equals(feature)) {
            this.processDocDecl = value;
        } else {
            if (FEATURE_RELAXED.equals(feature)) {
                this.relaxed = value;
                return;
            }
            throw new XmlPullParserException("unsupported feature: " + feature, this, null);
        }
    }

    @Override
    public void setProperty(String property, Object value) throws XmlPullParserException {
        if (property.equals(PROPERTY_LOCATION)) {
            this.location = String.valueOf(value);
            return;
        }
        throw new XmlPullParserException("unsupported property: " + property);
    }

    static class ContentSource {
        private final char[] buffer;
        private final int limit;
        private final ContentSource next;
        private final int position;

        ContentSource(ContentSource next, char[] buffer, int position, int limit) {
            this.next = next;
            this.buffer = buffer;
            this.position = position;
            this.limit = limit;
        }
    }

    private void pushContentSource(char[] newBuffer) {
        this.nextContentSource = new ContentSource(this.nextContentSource, this.buffer, this.position, this.limit);
        this.buffer = newBuffer;
        this.position = 0;
        this.limit = newBuffer.length;
    }

    private void popContentSource() {
        this.buffer = this.nextContentSource.buffer;
        this.position = this.nextContentSource.position;
        this.limit = this.nextContentSource.limit;
        this.nextContentSource = this.nextContentSource.next;
    }
}
