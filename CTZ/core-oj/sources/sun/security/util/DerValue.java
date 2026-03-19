package sun.security.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Date;
import sun.misc.IOUtils;

public class DerValue {
    public static final byte TAG_APPLICATION = 64;
    public static final byte TAG_CONTEXT = -128;
    public static final byte TAG_PRIVATE = -64;
    public static final byte TAG_UNIVERSAL = 0;
    public static final byte tag_BMPString = 30;
    public static final byte tag_BitString = 3;
    public static final byte tag_Boolean = 1;
    public static final byte tag_Enumerated = 10;
    public static final byte tag_GeneralString = 27;
    public static final byte tag_GeneralizedTime = 24;
    public static final byte tag_IA5String = 22;
    public static final byte tag_Integer = 2;
    public static final byte tag_Null = 5;
    public static final byte tag_ObjectId = 6;
    public static final byte tag_OctetString = 4;
    public static final byte tag_PrintableString = 19;
    public static final byte tag_Sequence = 48;
    public static final byte tag_SequenceOf = 48;
    public static final byte tag_Set = 49;
    public static final byte tag_SetOf = 49;
    public static final byte tag_T61String = 20;
    public static final byte tag_UTF8String = 12;
    public static final byte tag_UniversalString = 28;
    public static final byte tag_UtcTime = 23;
    protected DerInputBuffer buffer;
    public final DerInputStream data;
    private int length;
    private byte[] originalEncodedForm;
    public byte tag;

    public boolean isUniversal() {
        return (this.tag & TAG_PRIVATE) == 0;
    }

    public boolean isApplication() {
        return (this.tag & TAG_PRIVATE) == 64;
    }

    public boolean isContextSpecific() {
        return (this.tag & TAG_PRIVATE) == 128;
    }

    public boolean isContextSpecific(byte b) {
        return isContextSpecific() && (this.tag & 31) == b;
    }

    boolean isPrivate() {
        return (this.tag & TAG_PRIVATE) == 192;
    }

    public boolean isConstructed() {
        return (this.tag & 32) == 32;
    }

    public boolean isConstructed(byte b) {
        return isConstructed() && (this.tag & 31) == b;
    }

    public DerValue(String str) throws IOException {
        boolean z = false;
        int i = 0;
        while (true) {
            if (i < str.length()) {
                if (!isPrintableStringChar(str.charAt(i))) {
                    break;
                } else {
                    i++;
                }
            } else {
                z = true;
                break;
            }
        }
        this.data = init(z ? (byte) 19 : (byte) 12, str);
    }

    public DerValue(byte b, String str) throws IOException {
        this.data = init(b, str);
    }

    public DerValue(byte b, byte[] bArr) {
        this.tag = b;
        this.buffer = new DerInputBuffer((byte[]) bArr.clone());
        this.length = bArr.length;
        this.data = new DerInputStream(this.buffer);
        this.data.mark(Integer.MAX_VALUE);
    }

    DerValue(DerInputBuffer derInputBuffer, boolean z) throws IOException {
        int pos = derInputBuffer.getPos();
        this.tag = (byte) derInputBuffer.read();
        byte b = (byte) derInputBuffer.read();
        this.length = DerInputStream.getLength(b, derInputBuffer);
        if (this.length == -1) {
            DerInputBuffer derInputBufferDup = derInputBuffer.dup();
            int iAvailable = derInputBufferDup.available();
            byte[] bArr = new byte[iAvailable + 2];
            bArr[0] = this.tag;
            bArr[1] = b;
            DataInputStream dataInputStream = new DataInputStream(derInputBufferDup);
            dataInputStream.readFully(bArr, 2, iAvailable);
            dataInputStream.close();
            DerInputBuffer derInputBuffer2 = new DerInputBuffer(new DerIndefLenConverter().convert(bArr));
            if (this.tag != derInputBuffer2.read()) {
                throw new IOException("Indefinite length encoding not supported");
            }
            this.length = DerInputStream.getLength(derInputBuffer2);
            this.buffer = derInputBuffer2.dup();
            this.buffer.truncate(this.length);
            this.data = new DerInputStream(this.buffer);
            derInputBuffer.skip(this.length + 2);
        } else {
            this.buffer = derInputBuffer.dup();
            this.buffer.truncate(this.length);
            this.data = new DerInputStream(this.buffer);
            derInputBuffer.skip(this.length);
        }
        if (z) {
            this.originalEncodedForm = derInputBuffer.getSlice(pos, derInputBuffer.getPos() - pos);
        }
    }

    public DerValue(byte[] bArr) throws IOException {
        this.data = init(true, (InputStream) new ByteArrayInputStream(bArr));
    }

    public DerValue(byte[] bArr, int i, int i2) throws IOException {
        this.data = init(true, (InputStream) new ByteArrayInputStream(bArr, i, i2));
    }

    public DerValue(InputStream inputStream) throws IOException {
        this.data = init(false, inputStream);
    }

    private DerInputStream init(byte b, String str) throws IOException {
        String str2;
        this.tag = b;
        if (b == 12) {
            str2 = "UTF8";
        } else if (b == 22 || b == 27) {
            str2 = "ASCII";
        } else if (b != 30) {
            switch (b) {
                case 19:
                    break;
                case 20:
                    str2 = "ISO-8859-1";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported DER string type");
            }
        } else {
            str2 = "UnicodeBigUnmarked";
        }
        byte[] bytes = str.getBytes(str2);
        this.length = bytes.length;
        this.buffer = new DerInputBuffer(bytes);
        DerInputStream derInputStream = new DerInputStream(this.buffer);
        derInputStream.mark(Integer.MAX_VALUE);
        return derInputStream;
    }

    private DerInputStream init(boolean z, InputStream inputStream) throws IOException {
        this.tag = (byte) inputStream.read();
        byte b = (byte) inputStream.read();
        this.length = DerInputStream.getLength(b, inputStream);
        if (this.length == -1) {
            int iAvailable = inputStream.available();
            byte[] bArr = new byte[iAvailable + 2];
            bArr[0] = this.tag;
            bArr[1] = b;
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            dataInputStream.readFully(bArr, 2, iAvailable);
            dataInputStream.close();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(new DerIndefLenConverter().convert(bArr));
            if (this.tag != byteArrayInputStream.read()) {
                throw new IOException("Indefinite length encoding not supported");
            }
            this.length = DerInputStream.getLength(byteArrayInputStream);
            inputStream = byteArrayInputStream;
        }
        if (z && inputStream.available() != this.length) {
            throw new IOException("extra data given to DerValue constructor");
        }
        this.buffer = new DerInputBuffer(IOUtils.readFully(inputStream, this.length, true));
        return new DerInputStream(this.buffer);
    }

    public void encode(DerOutputStream derOutputStream) throws IOException {
        derOutputStream.write(this.tag);
        derOutputStream.putLength(this.length);
        if (this.length > 0) {
            byte[] bArr = new byte[this.length];
            synchronized (this.data) {
                this.buffer.reset();
                if (this.buffer.read(bArr) != this.length) {
                    throw new IOException("short DER value read (encode)");
                }
                derOutputStream.write(bArr);
            }
        }
    }

    public final DerInputStream getData() {
        return this.data;
    }

    public final byte getTag() {
        return this.tag;
    }

    public boolean getBoolean() throws IOException {
        if (this.tag != 1) {
            throw new IOException("DerValue.getBoolean, not a BOOLEAN " + ((int) this.tag));
        }
        if (this.length == 1) {
            return this.buffer.read() != 0;
        }
        throw new IOException("DerValue.getBoolean, invalid length " + this.length);
    }

    public ObjectIdentifier getOID() throws IOException {
        if (this.tag != 6) {
            throw new IOException("DerValue.getOID, not an OID " + ((int) this.tag));
        }
        return new ObjectIdentifier(this.buffer);
    }

    private byte[] append(byte[] bArr, byte[] bArr2) {
        if (bArr == null) {
            return bArr2;
        }
        byte[] bArr3 = new byte[bArr.length + bArr2.length];
        System.arraycopy(bArr, 0, bArr3, 0, bArr.length);
        System.arraycopy(bArr2, 0, bArr3, bArr.length, bArr2.length);
        return bArr3;
    }

    public byte[] getOctetString() throws IOException {
        if (this.tag != 4 && !isConstructed((byte) 4)) {
            throw new IOException("DerValue.getOctetString, not an Octet String: " + ((int) this.tag));
        }
        byte[] bArrAppend = new byte[this.length];
        if (this.length == 0) {
            return bArrAppend;
        }
        if (this.buffer.read(bArrAppend) != this.length) {
            throw new IOException("short read on DerValue buffer");
        }
        if (isConstructed()) {
            DerInputStream derInputStream = new DerInputStream(bArrAppend);
            bArrAppend = null;
            while (derInputStream.available() != 0) {
                bArrAppend = append(bArrAppend, derInputStream.getOctetString());
            }
        }
        return bArrAppend;
    }

    public int getInteger() throws IOException {
        if (this.tag != 2) {
            throw new IOException("DerValue.getInteger, not an int " + ((int) this.tag));
        }
        return this.buffer.getInteger(this.data.available());
    }

    public BigInteger getBigInteger() throws IOException {
        if (this.tag != 2) {
            throw new IOException("DerValue.getBigInteger, not an int " + ((int) this.tag));
        }
        return this.buffer.getBigInteger(this.data.available(), false);
    }

    public BigInteger getPositiveBigInteger() throws IOException {
        if (this.tag != 2) {
            throw new IOException("DerValue.getBigInteger, not an int " + ((int) this.tag));
        }
        return this.buffer.getBigInteger(this.data.available(), true);
    }

    public int getEnumerated() throws IOException {
        if (this.tag != 10) {
            throw new IOException("DerValue.getEnumerated, incorrect tag: " + ((int) this.tag));
        }
        return this.buffer.getInteger(this.data.available());
    }

    public byte[] getBitString() throws IOException {
        if (this.tag != 3) {
            throw new IOException("DerValue.getBitString, not a bit string " + ((int) this.tag));
        }
        return this.buffer.getBitString();
    }

    public BitArray getUnalignedBitString() throws IOException {
        if (this.tag != 3) {
            throw new IOException("DerValue.getBitString, not a bit string " + ((int) this.tag));
        }
        return this.buffer.getUnalignedBitString();
    }

    public String getAsString() throws IOException {
        if (this.tag == 12) {
            return getUTF8String();
        }
        if (this.tag == 19) {
            return getPrintableString();
        }
        if (this.tag == 20) {
            return getT61String();
        }
        if (this.tag == 22) {
            return getIA5String();
        }
        if (this.tag == 30) {
            return getBMPString();
        }
        if (this.tag == 27) {
            return getGeneralString();
        }
        return null;
    }

    public byte[] getBitString(boolean z) throws IOException {
        if (!z && this.tag != 3) {
            throw new IOException("DerValue.getBitString, not a bit string " + ((int) this.tag));
        }
        return this.buffer.getBitString();
    }

    public BitArray getUnalignedBitString(boolean z) throws IOException {
        if (!z && this.tag != 3) {
            throw new IOException("DerValue.getBitString, not a bit string " + ((int) this.tag));
        }
        return this.buffer.getUnalignedBitString();
    }

    public byte[] getDataBytes() throws IOException {
        byte[] bArr = new byte[this.length];
        synchronized (this.data) {
            this.data.reset();
            this.data.getBytes(bArr);
        }
        return bArr;
    }

    public String getPrintableString() throws IOException {
        if (this.tag != 19) {
            throw new IOException("DerValue.getPrintableString, not a string " + ((int) this.tag));
        }
        return new String(getDataBytes(), "ASCII");
    }

    public String getT61String() throws IOException {
        if (this.tag != 20) {
            throw new IOException("DerValue.getT61String, not T61 " + ((int) this.tag));
        }
        return new String(getDataBytes(), "ISO-8859-1");
    }

    public String getIA5String() throws IOException {
        if (this.tag != 22) {
            throw new IOException("DerValue.getIA5String, not IA5 " + ((int) this.tag));
        }
        return new String(getDataBytes(), "ASCII");
    }

    public String getBMPString() throws IOException {
        if (this.tag != 30) {
            throw new IOException("DerValue.getBMPString, not BMP " + ((int) this.tag));
        }
        return new String(getDataBytes(), "UnicodeBigUnmarked");
    }

    public String getUTF8String() throws IOException {
        if (this.tag != 12) {
            throw new IOException("DerValue.getUTF8String, not UTF-8 " + ((int) this.tag));
        }
        return new String(getDataBytes(), "UTF8");
    }

    public String getGeneralString() throws IOException {
        if (this.tag != 27) {
            throw new IOException("DerValue.getGeneralString, not GeneralString " + ((int) this.tag));
        }
        return new String(getDataBytes(), "ASCII");
    }

    public Date getUTCTime() throws IOException {
        if (this.tag != 23) {
            throw new IOException("DerValue.getUTCTime, not a UtcTime: " + ((int) this.tag));
        }
        return this.buffer.getUTCTime(this.data.available());
    }

    public Date getGeneralizedTime() throws IOException {
        if (this.tag != 24) {
            throw new IOException("DerValue.getGeneralizedTime, not a GeneralizedTime: " + ((int) this.tag));
        }
        return this.buffer.getGeneralizedTime(this.data.available());
    }

    public boolean equals(Object obj) {
        if (obj instanceof DerValue) {
            return equals((DerValue) obj);
        }
        return false;
    }

    public boolean equals(DerValue derValue) {
        if (this == derValue) {
            return true;
        }
        if (this.tag != derValue.tag) {
            return false;
        }
        if (this.data == derValue.data) {
            return true;
        }
        if (System.identityHashCode(this.data) > System.identityHashCode(derValue.data)) {
            return doEquals(this, derValue);
        }
        return doEquals(derValue, this);
    }

    private static boolean doEquals(DerValue derValue, DerValue derValue2) {
        boolean zEquals;
        synchronized (derValue.data) {
            synchronized (derValue2.data) {
                derValue.data.reset();
                derValue2.data.reset();
                zEquals = derValue.buffer.equals(derValue2.buffer);
            }
        }
        return zEquals;
    }

    public String toString() {
        try {
            String asString = getAsString();
            if (asString != null) {
                return "\"" + asString + "\"";
            }
            if (this.tag == 5) {
                return "[DerValue, null]";
            }
            if (this.tag == 6) {
                return "OID." + ((Object) getOID());
            }
            return "[DerValue, tag = " + ((int) this.tag) + ", length = " + this.length + "]";
        } catch (IOException e) {
            throw new IllegalArgumentException("misformatted DER value");
        }
    }

    public byte[] getOriginalEncodedForm() {
        if (this.originalEncodedForm != null) {
            return (byte[]) this.originalEncodedForm.clone();
        }
        return null;
    }

    public byte[] toByteArray() throws IOException {
        DerOutputStream derOutputStream = new DerOutputStream();
        encode(derOutputStream);
        this.data.reset();
        return derOutputStream.toByteArray();
    }

    public DerInputStream toDerInputStream() throws IOException {
        if (this.tag == 48 || this.tag == 49) {
            return new DerInputStream(this.buffer);
        }
        throw new IOException("toDerInputStream rejects tag type " + ((int) this.tag));
    }

    public int length() {
        return this.length;
    }

    public static boolean isPrintableStringChar(char r2) {
        if ((r2 < 'a' || r2 > 'z') && ((r2 < 'A' || r2 > 'Z') && ((r2 < '0' || r2 > '9') && r2 != ' ' && r2 != ':' && r2 != '=' && r2 != '?'))) {
            switch (r2) {
                default:
                    switch (r2) {
                    }
                    return true;
                case '\'':
                case '(':
                case ')':
                    return true;
            }
        }
        return true;
    }

    public static byte createTag(byte b, boolean z, byte b2) {
        byte b3 = (byte) (b | b2);
        if (z) {
            return (byte) (b3 | 32);
        }
        return b3;
    }

    public void resetTag(byte b) {
        this.tag = b;
    }

    public int hashCode() {
        return toString().hashCode();
    }
}
