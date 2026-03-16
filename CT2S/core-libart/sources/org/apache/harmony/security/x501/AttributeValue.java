package org.apache.harmony.security.x501;

import dalvik.bytecode.Opcodes;
import java.io.IOException;
import java.util.Collection;
import org.apache.harmony.security.asn1.ASN1SetOf;
import org.apache.harmony.security.asn1.ASN1StringType;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.DerInputStream;
import org.apache.harmony.security.utils.ObjectIdentifier;

public final class AttributeValue {
    public byte[] bytes;
    public byte[] encoded;
    public final String escapedString;
    private boolean hasConsecutiveSpaces;
    public boolean hasQE;
    private String hexString;
    public final String rawString;
    private String rfc2253String;
    private final int tag;
    public boolean wasEncoded = false;

    public AttributeValue(String parsedString, boolean hasQorE, ObjectIdentifier oid) {
        int tag;
        this.hasQE = hasQorE;
        this.rawString = parsedString;
        this.escapedString = makeEscaped(this.rawString);
        if (oid == AttributeTypeAndValue.EMAILADDRESS || oid == AttributeTypeAndValue.DC) {
            tag = ASN1StringType.IA5STRING.id;
        } else if (isPrintableString(this.rawString)) {
            tag = ASN1StringType.PRINTABLESTRING.id;
        } else {
            tag = ASN1StringType.UTF8STRING.id;
        }
        this.tag = tag;
    }

    public AttributeValue(String hexString, byte[] encoded) {
        this.hexString = hexString;
        this.encoded = encoded;
        try {
            DerInputStream in = new DerInputStream(encoded);
            this.tag = in.tag;
            if (DirectoryString.ASN1.checkTag(this.tag)) {
                this.rawString = (String) DirectoryString.ASN1.decode(in);
                this.escapedString = makeEscaped(this.rawString);
            } else {
                this.rawString = hexString;
                this.escapedString = hexString;
            }
        } catch (IOException e) {
            IllegalArgumentException iae = new IllegalArgumentException();
            iae.initCause(e);
            throw iae;
        }
    }

    public AttributeValue(String rawString, byte[] encoded, int tag) {
        this.encoded = encoded;
        this.tag = tag;
        if (rawString == null) {
            this.rawString = getHexString();
            this.escapedString = this.hexString;
        } else {
            this.rawString = rawString;
            this.escapedString = makeEscaped(rawString);
        }
    }

    private static boolean isPrintableString(String str) {
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch != ' ' && ((ch < '\'' || ch > ')') && ((ch < '+' || ch > ':') && ch != '=' && ch != '?' && ((ch < 'A' || ch > 'Z') && (ch < 'a' || ch > 'z'))))) {
                return false;
            }
        }
        return true;
    }

    public int getTag() {
        return this.tag;
    }

    public String getHexString() {
        if (this.hexString == null) {
            if (!this.wasEncoded) {
                if (this.tag == ASN1StringType.IA5STRING.id) {
                    this.encoded = ASN1StringType.IA5STRING.encode(this.rawString);
                } else if (this.tag == ASN1StringType.PRINTABLESTRING.id) {
                    this.encoded = ASN1StringType.PRINTABLESTRING.encode(this.rawString);
                } else {
                    this.encoded = ASN1StringType.UTF8STRING.encode(this.rawString);
                }
                this.wasEncoded = true;
            }
            StringBuilder buf = new StringBuilder((this.encoded.length * 2) + 1);
            buf.append('#');
            for (int i = 0; i < this.encoded.length; i++) {
                int c = (this.encoded[i] >> 4) & 15;
                if (c < 10) {
                    buf.append((char) (c + 48));
                } else {
                    buf.append((char) (c + 87));
                }
                int c2 = this.encoded[i] & 15;
                if (c2 < 10) {
                    buf.append((char) (c2 + 48));
                } else {
                    buf.append((char) (c2 + 87));
                }
            }
            this.hexString = buf.toString();
        }
        return this.hexString;
    }

    public Collection<?> getValues(ASN1Type type) throws IOException {
        return (Collection) new ASN1SetOf(type).decode(this.encoded);
    }

    public void appendQEString(StringBuilder sb) {
        sb.append('\"');
        if (this.hasQE) {
            for (int i = 0; i < this.rawString.length(); i++) {
                char c = this.rawString.charAt(i);
                if (c == '\"' || c == '\\') {
                    sb.append('\\');
                }
                sb.append(c);
            }
        } else {
            sb.append(this.rawString);
        }
        sb.append('\"');
    }

    private String makeEscaped(String name) {
        int length = name.length();
        if (length != 0) {
            StringBuilder buf = new StringBuilder(length * 2);
            boolean escapeSpaces = false;
            int index = 0;
            while (index < length) {
                char ch = name.charAt(index);
                switch (ch) {
                    case ' ':
                        if (index < length - 1) {
                            boolean nextIsSpace = name.charAt(index + 1) == ' ';
                            escapeSpaces = escapeSpaces || nextIsSpace || index == 0;
                            this.hasConsecutiveSpaces |= nextIsSpace;
                        } else {
                            escapeSpaces = true;
                        }
                        if (escapeSpaces) {
                            buf.append('\\');
                        }
                        buf.append(' ');
                        break;
                    case '\"':
                    case '\\':
                        this.hasQE = true;
                        buf.append('\\');
                        buf.append(ch);
                        break;
                    case '#':
                    case Opcodes.OP_PACKED_SWITCH:
                    case ',':
                    case Opcodes.OP_IF_GEZ:
                    case Opcodes.OP_IF_GTZ:
                    case Opcodes.OP_IF_LEZ:
                    case '>':
                        buf.append('\\');
                        buf.append(ch);
                        break;
                    default:
                        buf.append(ch);
                        break;
                }
                if (escapeSpaces && ch != ' ') {
                    escapeSpaces = false;
                }
                index++;
            }
            return buf.toString();
        }
        return name;
    }

    public String makeCanonical() {
        int length = this.rawString.length();
        if (length == 0) {
            return this.rawString;
        }
        StringBuilder buf = new StringBuilder(length * 2);
        int index = 0;
        if (this.rawString.charAt(0) == '#') {
            buf.append('\\');
            buf.append('#');
            index = 0 + 1;
        }
        while (index < length) {
            char ch = this.rawString.charAt(index);
            switch (ch) {
                case ' ':
                    int bufLength = buf.length();
                    if (bufLength == 0) {
                        continue;
                    } else if (buf.charAt(bufLength - 1) != ' ') {
                        buf.append(' ');
                    }
                    index++;
                    break;
                case '\"':
                case Opcodes.OP_PACKED_SWITCH:
                case ',':
                case Opcodes.OP_IF_GEZ:
                case Opcodes.OP_IF_GTZ:
                case '>':
                case '\\':
                    buf.append('\\');
                    break;
            }
            buf.append(ch);
            index++;
        }
        int bufLength2 = buf.length() - 1;
        while (bufLength2 > -1 && buf.charAt(bufLength2) == ' ') {
            bufLength2--;
        }
        buf.setLength(bufLength2 + 1);
        return buf.toString();
    }

    public String getRFC2253String() {
        if (!this.hasConsecutiveSpaces) {
            return this.escapedString;
        }
        if (this.rfc2253String == null) {
            int lastIndex = this.escapedString.length() - 2;
            for (int i = lastIndex; i > 0; i -= 2) {
                if (this.escapedString.charAt(i) == '\\' && this.escapedString.charAt(i + 1) == ' ') {
                    lastIndex = i - 1;
                }
            }
            boolean beginning = true;
            StringBuilder sb = new StringBuilder(this.escapedString.length());
            int i2 = 0;
            while (i2 < this.escapedString.length()) {
                char ch = this.escapedString.charAt(i2);
                if (ch != '\\') {
                    sb.append(ch);
                    beginning = false;
                } else {
                    char nextCh = this.escapedString.charAt(i2 + 1);
                    if (nextCh == ' ') {
                        if (beginning || i2 > lastIndex) {
                            sb.append(ch);
                        }
                        sb.append(nextCh);
                    } else {
                        sb.append(ch);
                        sb.append(nextCh);
                        beginning = false;
                    }
                    i2++;
                }
                i2++;
            }
            this.rfc2253String = sb.toString();
        }
        return this.rfc2253String;
    }
}
