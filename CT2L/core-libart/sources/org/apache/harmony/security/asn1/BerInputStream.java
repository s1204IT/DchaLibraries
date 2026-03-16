package org.apache.harmony.security.asn1;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.sql.Types;
import java.util.ArrayList;

public class BerInputStream {
    private static final int BUF_INCREASE_SIZE = 16384;
    protected static final int INDEFINIT_LENGTH = -1;
    protected byte[] buffer;
    public int choiceIndex;
    public Object content;
    protected int contentOffset;
    private final InputStream in;
    protected boolean isIndefinedLength;
    protected boolean isVerify;
    protected int length;
    protected int offset;
    public int oidElement;
    private Object[][] pool;
    public int tag;
    protected int tagOffset;
    public int[] times;

    public BerInputStream(byte[] encoded) throws IOException {
        this(encoded, 0, encoded.length);
    }

    public BerInputStream(byte[] encoded, int offset, int expectedLength) throws IOException {
        this.offset = 0;
        this.in = null;
        this.buffer = encoded;
        this.offset = offset;
        next();
        if (this.length != -1 && offset + expectedLength != this.offset + this.length) {
            throw new ASN1Exception("Wrong content length");
        }
    }

    public BerInputStream(InputStream in) throws IOException {
        this(in, 16384);
    }

    public BerInputStream(InputStream in, int initialSize) throws IOException {
        this.offset = 0;
        this.in = in;
        this.buffer = new byte[initialSize];
        next();
        if (this.length != -1) {
            if (this.buffer.length < this.length + this.offset) {
                byte[] newBuffer = new byte[this.length + this.offset];
                System.arraycopy(this.buffer, 0, newBuffer, 0, this.offset);
                this.buffer = newBuffer;
                return;
            }
            return;
        }
        this.isIndefinedLength = true;
        throw new ASN1Exception("Decoding indefinite length encoding is not supported");
    }

    public final void reset(byte[] encoded) throws IOException {
        this.buffer = encoded;
        next();
    }

    public int next() throws IOException {
        this.tagOffset = this.offset;
        this.tag = read();
        this.length = read();
        if (this.length != 128) {
            if ((this.length & 128) != 0) {
                int numOctets = this.length & 127;
                if (numOctets > 5) {
                    throw new ASN1Exception("Too long encoding at [" + this.tagOffset + "]");
                }
                this.length = read();
                for (int i = 1; i < numOctets; i++) {
                    int ch = read();
                    this.length = (this.length << 8) + ch;
                }
                if (this.length > 16777215) {
                    throw new ASN1Exception("Too long encoding at [" + this.tagOffset + "]");
                }
            }
        } else {
            this.length = -1;
        }
        this.contentOffset = this.offset;
        return this.tag;
    }

    public static int getLength(byte[] encoding) {
        int length = encoding[1] & Character.DIRECTIONALITY_UNDEFINED;
        int numOctets = 0;
        if ((length & 128) != 0) {
            numOctets = length & 127;
            length = encoding[2] & Character.DIRECTIONALITY_UNDEFINED;
            for (int i = 3; i < numOctets + 2; i++) {
                length = (length << 8) + (encoding[i] & Character.DIRECTIONALITY_UNDEFINED);
            }
        }
        return numOctets + 2 + length;
    }

    public void readBitString() throws IOException {
        if (this.tag == 3) {
            if (this.length == 0) {
                throw new ASN1Exception("ASN.1 Bitstring: wrong length. Tag at [" + this.tagOffset + "]");
            }
            readContent();
            if (this.buffer[this.contentOffset] > 7) {
                throw new ASN1Exception("ASN.1 Bitstring: wrong content at [" + this.contentOffset + "]. A number of unused bits MUST be in range 0 to 7");
            }
            if (this.length == 1 && this.buffer[this.contentOffset] != 0) {
                throw new ASN1Exception("ASN.1 Bitstring: wrong content at [" + this.contentOffset + "]. For empty string unused bits MUST be 0");
            }
            return;
        }
        if (this.tag == 35) {
            throw new ASN1Exception("Decoding constructed ASN.1 bitstring  type is not provided");
        }
        throw expected("bitstring");
    }

    public void readEnumerated() throws IOException {
        if (this.tag != 10) {
            throw expected("enumerated");
        }
        if (this.length == 0) {
            throw new ASN1Exception("ASN.1 enumerated: wrong length for identifier at [" + this.tagOffset + "]");
        }
        readContent();
        if (this.length > 1) {
            int bits = this.buffer[this.contentOffset] & Character.DIRECTIONALITY_UNDEFINED;
            if (this.buffer[this.contentOffset + 1] < 0) {
                bits += 256;
            }
            if (bits == 0 || bits == 511) {
                throw new ASN1Exception("ASN.1 enumerated: wrong content at [" + this.contentOffset + "]. An integer MUST be encoded in minimum number of octets");
            }
        }
    }

    public void readBoolean() throws IOException {
        if (this.tag != 1) {
            throw expected("boolean");
        }
        if (this.length != 1) {
            throw new ASN1Exception("Wrong length for ASN.1 boolean at [" + this.tagOffset + "]");
        }
        readContent();
    }

    public void readGeneralizedTime() throws IOException {
        byte char14;
        if (this.tag == 24) {
            readContent();
            if (this.buffer[this.offset - 1] != 90) {
                throw new ASN1Exception("ASN.1 GeneralizedTime: encoded format is not implemented");
            }
            if (this.length != 15 && (this.length < 17 || this.length > 19)) {
                throw new ASN1Exception("ASN.1 GeneralizedTime wrongly encoded at [" + this.contentOffset + "]");
            }
            if (this.length > 16 && (char14 = this.buffer[this.contentOffset + 14]) != 46 && char14 != 44) {
                throw new ASN1Exception("ASN.1 GeneralizedTime wrongly encoded at [" + this.contentOffset + "]");
            }
            if (this.times == null) {
                this.times = new int[7];
            }
            this.times[0] = strToInt(this.contentOffset, 4);
            this.times[1] = strToInt(this.contentOffset + 4, 2);
            this.times[2] = strToInt(this.contentOffset + 6, 2);
            this.times[3] = strToInt(this.contentOffset + 8, 2);
            this.times[4] = strToInt(this.contentOffset + 10, 2);
            this.times[5] = strToInt(this.contentOffset + 12, 2);
            if (this.length > 16) {
                this.times[6] = strToInt(this.contentOffset + 15, this.length - 16);
                if (this.length == 17) {
                    this.times[6] = this.times[6] * 100;
                    return;
                } else {
                    if (this.length == 18) {
                        this.times[6] = this.times[6] * 10;
                        return;
                    }
                    return;
                }
            }
            return;
        }
        if (this.tag == 56) {
            throw new ASN1Exception("Decoding constructed ASN.1 GeneralizedTime type is not supported");
        }
        throw expected("GeneralizedTime");
    }

    public void readUTCTime() throws IOException {
        if (this.tag == 23) {
            switch (this.length) {
                case 11:
                case 13:
                    readContent();
                    if (this.buffer[this.offset - 1] != 90) {
                        throw new ASN1Exception("ASN.1 UTCTime wrongly encoded at [" + this.contentOffset + ']');
                    }
                    if (this.times == null) {
                        this.times = new int[7];
                    }
                    this.times[0] = strToInt(this.contentOffset, 2);
                    if (this.times[0] > 49) {
                        int[] iArr = this.times;
                        iArr[0] = iArr[0] + 1900;
                    } else {
                        int[] iArr2 = this.times;
                        iArr2[0] = iArr2[0] + Types.JAVA_OBJECT;
                    }
                    this.times[1] = strToInt(this.contentOffset + 2, 2);
                    this.times[2] = strToInt(this.contentOffset + 4, 2);
                    this.times[3] = strToInt(this.contentOffset + 6, 2);
                    this.times[4] = strToInt(this.contentOffset + 8, 2);
                    if (this.length == 13) {
                        this.times[5] = strToInt(this.contentOffset + 10, 2);
                        return;
                    }
                    return;
                case 12:
                case 14:
                case 16:
                default:
                    throw new ASN1Exception("ASN.1 UTCTime: wrong length, identifier at " + this.tagOffset);
                case 15:
                case 17:
                    throw new ASN1Exception("ASN.1 UTCTime: local time format is not supported");
            }
        }
        if (this.tag == 55) {
            throw new ASN1Exception("Decoding constructed ASN.1 UTCTime type is not supported");
        }
        throw expected("UTCTime");
    }

    private int strToInt(int off, int count) throws ASN1Exception {
        int result = 0;
        int end = off + count;
        for (int i = off; i < end; i++) {
            int c = this.buffer[i] - 48;
            if (c < 0 || c > 9) {
                throw new ASN1Exception("Time encoding has invalid char");
            }
            result = (result * 10) + c;
        }
        return result;
    }

    public void readInteger() throws IOException {
        if (this.tag != 2) {
            throw expected("integer");
        }
        if (this.length < 1) {
            throw new ASN1Exception("Wrong length for ASN.1 integer at [" + this.tagOffset + "]");
        }
        readContent();
        if (this.length > 1) {
            byte firstByte = this.buffer[this.offset - this.length];
            byte secondByte = (byte) (this.buffer[(this.offset - this.length) + 1] & Byte.MIN_VALUE);
            if ((firstByte == 0 && secondByte == 0) || (firstByte == -1 && secondByte == -128)) {
                throw new ASN1Exception("Wrong content for ASN.1 integer at [" + (this.offset - this.length) + "]. An integer MUST be encoded in minimum number of octets");
            }
        }
    }

    public void readOctetString() throws IOException {
        if (this.tag == 4) {
            readContent();
        } else {
            if (this.tag == 36) {
                throw new ASN1Exception("Decoding constructed ASN.1 octet string type is not supported");
            }
            throw expected("octetstring");
        }
    }

    private ASN1Exception expected(String what) throws ASN1Exception {
        throw new ASN1Exception("ASN.1 " + what + " identifier expected at [" + this.tagOffset + "], got " + Integer.toHexString(this.tag));
    }

    public void readOID() throws IOException {
        if (this.tag != 6) {
            throw expected("OID");
        }
        if (this.length < 1) {
            throw new ASN1Exception("Wrong length for ASN.1 object identifier at [" + this.tagOffset + "]");
        }
        readContent();
        if ((this.buffer[this.offset - 1] & Byte.MIN_VALUE) != 0) {
            throw new ASN1Exception("Wrong encoding at [" + (this.offset - 1) + "]");
        }
        this.oidElement = 1;
        int i = 0;
        while (i < this.length) {
            while ((this.buffer[this.contentOffset + i] & Byte.MIN_VALUE) == 128) {
                i++;
            }
            i++;
            this.oidElement++;
        }
    }

    public void readSequence(ASN1Sequence sequence) throws IOException {
        if (this.tag != 48) {
            throw expected("sequence");
        }
        int begOffset = this.offset;
        int endOffset = begOffset + this.length;
        ASN1Type[] type = sequence.type;
        int i = 0;
        if (this.isVerify) {
            while (this.offset < endOffset && i < type.length) {
                next();
                while (!type[i].checkTag(this.tag)) {
                    if (!sequence.OPTIONAL[i] || i == type.length - 1) {
                        throw new ASN1Exception("ASN.1 Sequence: mandatory value is missing at [" + this.tagOffset + "]");
                    }
                    i++;
                }
                type[i].decode(this);
                i++;
            }
            while (i < type.length) {
                if (sequence.OPTIONAL[i]) {
                    i++;
                } else {
                    throw new ASN1Exception("ASN.1 Sequence: mandatory value is missing at [" + this.tagOffset + "]");
                }
            }
        } else {
            int seqTagOffset = this.tagOffset;
            Object[] values = new Object[type.length];
            while (this.offset < endOffset && i < type.length) {
                next();
                while (!type[i].checkTag(this.tag)) {
                    if (!sequence.OPTIONAL[i] || i == type.length - 1) {
                        throw new ASN1Exception("ASN.1 Sequence: mandatory value is missing at [" + this.tagOffset + "]");
                    }
                    if (sequence.DEFAULT[i] != null) {
                        values[i] = sequence.DEFAULT[i];
                    }
                    i++;
                }
                values[i] = type[i].decode(this);
                i++;
            }
            while (i < type.length) {
                if (!sequence.OPTIONAL[i]) {
                    throw new ASN1Exception("ASN.1 Sequence: mandatory value is missing at [" + this.tagOffset + "]");
                }
                if (sequence.DEFAULT[i] != null) {
                    values[i] = sequence.DEFAULT[i];
                }
                i++;
            }
            this.content = values;
            this.tagOffset = seqTagOffset;
        }
        if (this.offset != endOffset) {
            throw new ASN1Exception("Wrong encoding at [" + begOffset + "]. Content's length and encoded length are not the same");
        }
    }

    public void readSequenceOf(ASN1SequenceOf sequenceOf) throws IOException {
        if (this.tag != 48) {
            throw expected("sequenceOf");
        }
        decodeValueCollection(sequenceOf);
    }

    public void readSet(ASN1Set set) throws IOException {
        if (this.tag != 49) {
            throw expected("set");
        }
        throw new ASN1Exception("Decoding ASN.1 Set type is not supported");
    }

    public void readSetOf(ASN1SetOf setOf) throws IOException {
        if (this.tag != 49) {
            throw expected("setOf");
        }
        decodeValueCollection(setOf);
    }

    private void decodeValueCollection(ASN1ValueCollection collection) throws IOException {
        int begOffset = this.offset;
        int endOffset = begOffset + this.length;
        ASN1Type type = collection.type;
        if (this.isVerify) {
            while (endOffset > this.offset) {
                next();
                type.decode(this);
            }
        } else {
            int seqTagOffset = this.tagOffset;
            ArrayList<Object> values = new ArrayList<>();
            while (endOffset > this.offset) {
                next();
                values.add(type.decode(this));
            }
            values.trimToSize();
            this.content = values;
            this.tagOffset = seqTagOffset;
        }
        if (this.offset != endOffset) {
            throw new ASN1Exception("Wrong encoding at [" + begOffset + "]. Content's length and encoded length are not the same");
        }
    }

    public void readString(ASN1StringType type) throws IOException {
        if (this.tag == type.id) {
            readContent();
        } else {
            if (this.tag == type.constrId) {
                throw new ASN1Exception("Decoding constructed ASN.1 string type is not provided");
            }
            throw expected("string");
        }
    }

    public byte[] getEncoded() {
        byte[] encoded = new byte[this.offset - this.tagOffset];
        System.arraycopy(this.buffer, this.tagOffset, encoded, 0, encoded.length);
        return encoded;
    }

    public final byte[] getBuffer() {
        return this.buffer;
    }

    public final int getLength() {
        return this.length;
    }

    public final int getOffset() {
        return this.offset;
    }

    public final int getEndOffset() {
        return this.offset + this.length;
    }

    public final int getTagOffset() {
        return this.tagOffset;
    }

    public final void setVerify() {
        this.isVerify = true;
    }

    protected int read() throws IOException {
        if (this.offset == this.buffer.length) {
            throw new ASN1Exception("Unexpected end of encoding");
        }
        if (this.in == null) {
            byte[] bArr = this.buffer;
            int i = this.offset;
            this.offset = i + 1;
            return bArr[i] & Character.DIRECTIONALITY_UNDEFINED;
        }
        int octet = this.in.read();
        if (octet == -1) {
            throw new ASN1Exception("Unexpected end of encoding");
        }
        byte[] bArr2 = this.buffer;
        int i2 = this.offset;
        this.offset = i2 + 1;
        bArr2[i2] = (byte) octet;
        return octet;
    }

    public void readContent() throws IOException {
        if (this.offset + this.length > this.buffer.length) {
            throw new ASN1Exception("Unexpected end of encoding");
        }
        if (this.in == null) {
            this.offset += this.length;
            return;
        }
        int bytesRead = this.in.read(this.buffer, this.offset, this.length);
        if (bytesRead != this.length) {
            int c = bytesRead;
            while (c >= 1 && bytesRead <= this.length) {
                c = this.in.read(this.buffer, this.offset + bytesRead, this.length - bytesRead);
                bytesRead += c;
                if (bytesRead == this.length) {
                }
            }
            throw new ASN1Exception("Failed to read encoded content");
        }
        this.offset += this.length;
    }

    public void compactBuffer() {
        if (this.offset != this.buffer.length) {
            byte[] newBuffer = new byte[this.offset];
            System.arraycopy(this.buffer, 0, newBuffer, 0, this.offset);
            this.buffer = newBuffer;
        }
    }

    public void put(Object key, Object entry) {
        if (this.pool == null) {
            this.pool = (Object[][]) Array.newInstance((Class<?>) Object.class, 2, 10);
        }
        int i = 0;
        while (i < this.pool[0].length && this.pool[0][i] != null) {
            if (this.pool[0][i] != key) {
                i++;
            } else {
                this.pool[1][i] = entry;
                return;
            }
        }
        if (i == this.pool[0].length) {
            Object[][] newPool = (Object[][]) Array.newInstance((Class<?>) Object.class, this.pool[0].length * 2, 2);
            System.arraycopy(this.pool[0], 0, newPool[0], 0, this.pool[0].length);
            System.arraycopy(this.pool[1], 0, newPool[1], 0, this.pool[0].length);
            this.pool = newPool;
            return;
        }
        this.pool[0][i] = key;
        this.pool[1][i] = entry;
    }

    public Object get(Object key) {
        if (this.pool == null) {
            return null;
        }
        for (int i = 0; i < this.pool[0].length; i++) {
            if (this.pool[0][i] == key) {
                return this.pool[1][i];
            }
        }
        return null;
    }
}
