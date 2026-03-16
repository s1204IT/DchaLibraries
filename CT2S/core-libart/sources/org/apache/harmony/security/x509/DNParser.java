package org.apache.harmony.security.x509;

import dalvik.bytecode.Opcodes;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.harmony.security.utils.ObjectIdentifier;
import org.apache.harmony.security.x501.AttributeTypeAndValue;
import org.apache.harmony.security.x501.AttributeValue;

public final class DNParser {
    private int beg;
    private final char[] chars;
    private byte[] encoded;
    private int end;
    private boolean hasQE;
    private int pos;

    public DNParser(String dn) throws IOException {
        this.chars = dn.toCharArray();
    }

    private String nextAT() throws IOException {
        this.hasQE = false;
        while (this.pos < this.chars.length && this.chars[this.pos] == ' ') {
            this.pos++;
        }
        if (this.pos == this.chars.length) {
            return null;
        }
        this.beg = this.pos;
        this.pos++;
        while (this.pos < this.chars.length && this.chars[this.pos] != '=' && this.chars[this.pos] != ' ') {
            this.pos++;
        }
        if (this.pos >= this.chars.length) {
            throw new IOException("Invalid distinguished name string");
        }
        this.end = this.pos;
        if (this.chars[this.pos] == ' ') {
            while (this.pos < this.chars.length && this.chars[this.pos] != '=' && this.chars[this.pos] == ' ') {
                this.pos++;
            }
            if (this.chars[this.pos] != '=' || this.pos == this.chars.length) {
                throw new IOException("Invalid distinguished name string");
            }
        }
        this.pos++;
        while (this.pos < this.chars.length && this.chars[this.pos] == ' ') {
            this.pos++;
        }
        if (this.end - this.beg > 4 && this.chars[this.beg + 3] == '.' && ((this.chars[this.beg] == 'O' || this.chars[this.beg] == 'o') && ((this.chars[this.beg + 1] == 'I' || this.chars[this.beg + 1] == 'i') && (this.chars[this.beg + 2] == 'D' || this.chars[this.beg + 2] == 'd')))) {
            this.beg += 4;
        }
        return new String(this.chars, this.beg, this.end - this.beg);
    }

    private String quotedAV() throws IOException {
        this.pos++;
        this.beg = this.pos;
        this.end = this.beg;
        while (this.pos != this.chars.length) {
            if (this.chars[this.pos] == '\"') {
                this.pos++;
                while (this.pos < this.chars.length && this.chars[this.pos] == ' ') {
                    this.pos++;
                }
                return new String(this.chars, this.beg, this.end - this.beg);
            }
            if (this.chars[this.pos] == '\\') {
                this.chars[this.end] = getEscaped();
            } else {
                this.chars[this.end] = this.chars[this.pos];
            }
            this.pos++;
            this.end++;
        }
        throw new IOException("Invalid distinguished name string");
    }

    private String hexAV() throws IOException {
        int hexLen;
        if (this.pos + 4 >= this.chars.length) {
            throw new IOException("Invalid distinguished name string");
        }
        this.beg = this.pos;
        this.pos++;
        while (this.pos != this.chars.length && this.chars[this.pos] != '+' && this.chars[this.pos] != ',' && this.chars[this.pos] != ';') {
            if (this.chars[this.pos] == ' ') {
                this.end = this.pos;
                this.pos++;
                while (this.pos < this.chars.length && this.chars[this.pos] == ' ') {
                    this.pos++;
                }
                hexLen = this.end - this.beg;
                if (hexLen >= 5 || (hexLen & 1) == 0) {
                    throw new IOException("Invalid distinguished name string");
                }
                this.encoded = new byte[hexLen / 2];
                int p = this.beg + 1;
                for (int i = 0; i < this.encoded.length; i++) {
                    this.encoded[i] = (byte) getByte(p);
                    p += 2;
                }
                return new String(this.chars, this.beg, hexLen);
            }
            if (this.chars[this.pos] >= 'A' && this.chars[this.pos] <= 'F') {
                char[] cArr = this.chars;
                int i2 = this.pos;
                cArr[i2] = (char) (cArr[i2] + ' ');
            }
            this.pos++;
        }
        this.end = this.pos;
        hexLen = this.end - this.beg;
        if (hexLen >= 5) {
        }
        throw new IOException("Invalid distinguished name string");
    }

    private String escapedAV() throws IOException {
        this.beg = this.pos;
        this.end = this.pos;
        while (this.pos < this.chars.length) {
            switch (this.chars[this.pos]) {
                case ' ':
                    int cur = this.end;
                    this.pos++;
                    char[] cArr = this.chars;
                    int i = this.end;
                    this.end = i + 1;
                    cArr[i] = ' ';
                    while (this.pos < this.chars.length && this.chars[this.pos] == ' ') {
                        char[] cArr2 = this.chars;
                        int i2 = this.end;
                        this.end = i2 + 1;
                        cArr2[i2] = ' ';
                        this.pos++;
                    }
                    if (this.pos == this.chars.length || this.chars[this.pos] == ',' || this.chars[this.pos] == '+' || this.chars[this.pos] == ';') {
                        return new String(this.chars, this.beg, cur - this.beg);
                    }
                    break;
                case Opcodes.OP_PACKED_SWITCH:
                case ',':
                case Opcodes.OP_IF_GEZ:
                    return new String(this.chars, this.beg, this.end - this.beg);
                case '\\':
                    char[] cArr3 = this.chars;
                    int i3 = this.end;
                    this.end = i3 + 1;
                    cArr3[i3] = getEscaped();
                    this.pos++;
                    break;
                default:
                    char[] cArr4 = this.chars;
                    int i4 = this.end;
                    this.end = i4 + 1;
                    cArr4[i4] = this.chars[this.pos];
                    this.pos++;
                    break;
            }
        }
        return new String(this.chars, this.beg, this.end - this.beg);
    }

    private char getEscaped() throws IOException {
        this.pos++;
        if (this.pos == this.chars.length) {
            throw new IOException("Invalid distinguished name string");
        }
        char ch = this.chars[this.pos];
        switch (ch) {
            case ' ':
            case '#':
            case Opcodes.OP_FILLED_NEW_ARRAY_RANGE:
            case '*':
            case Opcodes.OP_PACKED_SWITCH:
            case ',':
            case Opcodes.OP_IF_GEZ:
            case Opcodes.OP_IF_GTZ:
            case Opcodes.OP_IF_LEZ:
            case '>':
            case Opcodes.OP_IPUT_SHORT:
                return ch;
            case '\"':
            case '\\':
                this.hasQE = true;
                return ch;
            default:
                return getUTF8();
        }
    }

    protected char getUTF8() throws IOException {
        int count;
        int res;
        int res2 = getByte(this.pos);
        this.pos++;
        if (res2 < 128) {
            return (char) res2;
        }
        if (res2 < 192 || res2 > 247) {
            return '?';
        }
        if (res2 <= 223) {
            count = 1;
            res = res2 & 31;
        } else if (res2 <= 239) {
            count = 2;
            res = res2 & 15;
        } else {
            count = 3;
            res = res2 & 7;
        }
        for (int i = 0; i < count; i++) {
            this.pos++;
            if (this.pos == this.chars.length || this.chars[this.pos] != '\\') {
                return '?';
            }
            this.pos++;
            int b = getByte(this.pos);
            this.pos++;
            if ((b & 192) != 128) {
                return '?';
            }
            res = (res << 6) + (b & 63);
        }
        return (char) res;
    }

    private int getByte(int position) throws IOException {
        int b1;
        int b2;
        if (position + 1 >= this.chars.length) {
            throw new IOException("Invalid distinguished name string");
        }
        char c = this.chars[position];
        if (c >= '0' && c <= '9') {
            b1 = c - '0';
        } else if (c >= 'a' && c <= 'f') {
            b1 = c - 'W';
        } else if (c >= 'A' && c <= 'F') {
            b1 = c - '7';
        } else {
            throw new IOException("Invalid distinguished name string");
        }
        char c2 = this.chars[position + 1];
        if (c2 >= '0' && c2 <= '9') {
            b2 = c2 - '0';
        } else if (c2 >= 'a' && c2 <= 'f') {
            b2 = c2 - 'W';
        } else if (c2 >= 'A' && c2 <= 'F') {
            b2 = c2 - '7';
        } else {
            throw new IOException("Invalid distinguished name string");
        }
        return (b1 << 4) + b2;
    }

    public List<List<AttributeTypeAndValue>> parse() throws IOException {
        List<List<AttributeTypeAndValue>> list = new ArrayList<>();
        String attType = nextAT();
        if (attType != null) {
            ObjectIdentifier oid = AttributeTypeAndValue.getObjectIdentifier(attType);
            List<AttributeTypeAndValue> atav = new ArrayList<>();
            while (true) {
                if (this.pos == this.chars.length) {
                    atav.add(new AttributeTypeAndValue(oid, new AttributeValue("", false, oid)));
                    list.add(0, atav);
                } else {
                    switch (this.chars[this.pos]) {
                        case '\"':
                            atav.add(new AttributeTypeAndValue(oid, new AttributeValue(quotedAV(), this.hasQE, oid)));
                            break;
                        case '#':
                            atav.add(new AttributeTypeAndValue(oid, new AttributeValue(hexAV(), this.encoded)));
                            break;
                        case Opcodes.OP_PACKED_SWITCH:
                        case ',':
                        case Opcodes.OP_IF_GEZ:
                            atav.add(new AttributeTypeAndValue(oid, new AttributeValue("", false, oid)));
                            break;
                        default:
                            atav.add(new AttributeTypeAndValue(oid, new AttributeValue(escapedAV(), this.hasQE, oid)));
                            break;
                    }
                    if (this.pos >= this.chars.length) {
                        list.add(0, atav);
                    } else {
                        if (this.chars[this.pos] == ',' || this.chars[this.pos] == ';') {
                            list.add(0, atav);
                            atav = new ArrayList<>();
                        } else if (this.chars[this.pos] != '+') {
                            throw new IOException("Invalid distinguished name string");
                        }
                        this.pos++;
                        String attType2 = nextAT();
                        if (attType2 == null) {
                            throw new IOException("Invalid distinguished name string");
                        }
                        oid = AttributeTypeAndValue.getObjectIdentifier(attType2);
                    }
                }
            }
        }
        return list;
    }
}
