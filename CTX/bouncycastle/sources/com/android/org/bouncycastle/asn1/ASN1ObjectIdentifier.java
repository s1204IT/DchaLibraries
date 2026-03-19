package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ASN1ObjectIdentifier extends ASN1Primitive {
    private static final long LONG_LIMIT = 72057594037927808L;
    private static final ConcurrentMap<OidHandle, ASN1ObjectIdentifier> pool = new ConcurrentHashMap();
    private byte[] body;
    private final String identifier;

    public static ASN1ObjectIdentifier getInstance(Object obj) {
        if (obj == null || (obj instanceof ASN1ObjectIdentifier)) {
            return (ASN1ObjectIdentifier) obj;
        }
        if (obj instanceof ASN1Encodable) {
            ASN1Encodable aSN1Encodable = (ASN1Encodable) obj;
            if (aSN1Encodable.toASN1Primitive() instanceof ASN1ObjectIdentifier) {
                return (ASN1ObjectIdentifier) aSN1Encodable.toASN1Primitive();
            }
        }
        if (obj instanceof byte[]) {
            try {
                return (ASN1ObjectIdentifier) fromByteArray((byte[]) obj);
            } catch (IOException e) {
                throw new IllegalArgumentException("failed to construct object identifier from byte[]: " + e.getMessage());
            }
        }
        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    public static ASN1ObjectIdentifier getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        ASN1Primitive object = aSN1TaggedObject.getObject();
        if (z || (object instanceof ASN1ObjectIdentifier)) {
            return getInstance(object);
        }
        return fromOctetString(ASN1OctetString.getInstance(aSN1TaggedObject.getObject()).getOctets());
    }

    ASN1ObjectIdentifier(byte[] bArr) {
        StringBuffer stringBuffer = new StringBuffer();
        boolean z = true;
        long j = 0;
        BigInteger bigIntegerShiftLeft = null;
        for (int i = 0; i != bArr.length; i++) {
            int i2 = bArr[i] & 255;
            if (j <= LONG_LIMIT) {
                long j2 = j + ((long) (i2 & 127));
                if ((i2 & 128) == 0) {
                    if (z) {
                        if (j2 < 40) {
                            stringBuffer.append('0');
                        } else if (j2 < 80) {
                            stringBuffer.append('1');
                            j2 -= 40;
                        } else {
                            stringBuffer.append('2');
                            j2 -= 80;
                        }
                        z = false;
                    }
                    stringBuffer.append('.');
                    stringBuffer.append(j2);
                    j = 0;
                } else {
                    j = j2 << 7;
                }
            } else {
                BigInteger bigIntegerOr = (bigIntegerShiftLeft == null ? BigInteger.valueOf(j) : bigIntegerShiftLeft).or(BigInteger.valueOf(i2 & 127));
                if ((i2 & 128) == 0) {
                    if (z) {
                        stringBuffer.append('2');
                        bigIntegerOr = bigIntegerOr.subtract(BigInteger.valueOf(80L));
                        z = false;
                    }
                    stringBuffer.append('.');
                    stringBuffer.append(bigIntegerOr);
                    j = 0;
                    bigIntegerShiftLeft = null;
                } else {
                    bigIntegerShiftLeft = bigIntegerOr.shiftLeft(7);
                }
            }
        }
        this.identifier = stringBuffer.toString().intern();
        this.body = Arrays.clone(bArr);
    }

    public ASN1ObjectIdentifier(String str) {
        if (str == null) {
            throw new IllegalArgumentException("'identifier' cannot be null");
        }
        if (!isValidIdentifier(str)) {
            throw new IllegalArgumentException("string " + str + " not an OID");
        }
        this.identifier = str.intern();
    }

    ASN1ObjectIdentifier(ASN1ObjectIdentifier aSN1ObjectIdentifier, String str) {
        if (!isValidBranchID(str, 0)) {
            throw new IllegalArgumentException("string " + str + " not a valid OID branch");
        }
        this.identifier = aSN1ObjectIdentifier.getId() + "." + str;
    }

    public String getId() {
        return this.identifier;
    }

    public ASN1ObjectIdentifier branch(String str) {
        return new ASN1ObjectIdentifier(this, str);
    }

    public boolean on(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        String id = getId();
        String id2 = aSN1ObjectIdentifier.getId();
        return id.length() > id2.length() && id.charAt(id2.length()) == '.' && id.startsWith(id2);
    }

    private void writeField(ByteArrayOutputStream byteArrayOutputStream, long j) {
        byte[] bArr = new byte[9];
        int i = 8;
        bArr[8] = (byte) (((int) j) & 127);
        while (j >= 128) {
            j >>= 7;
            i--;
            bArr[i] = (byte) ((((int) j) & 127) | 128);
        }
        byteArrayOutputStream.write(bArr, i, 9 - i);
    }

    private void writeField(ByteArrayOutputStream byteArrayOutputStream, BigInteger bigInteger) {
        int iBitLength = (bigInteger.bitLength() + 6) / 7;
        if (iBitLength == 0) {
            byteArrayOutputStream.write(0);
            return;
        }
        byte[] bArr = new byte[iBitLength];
        int i = iBitLength - 1;
        BigInteger bigIntegerShiftRight = bigInteger;
        for (int i2 = i; i2 >= 0; i2--) {
            bArr[i2] = (byte) ((bigIntegerShiftRight.intValue() & 127) | 128);
            bigIntegerShiftRight = bigIntegerShiftRight.shiftRight(7);
        }
        bArr[i] = (byte) (bArr[i] & 127);
        byteArrayOutputStream.write(bArr, 0, bArr.length);
    }

    private void doOutput(ByteArrayOutputStream byteArrayOutputStream) {
        OIDTokenizer oIDTokenizer = new OIDTokenizer(this.identifier);
        int i = Integer.parseInt(oIDTokenizer.nextToken()) * 40;
        String strNextToken = oIDTokenizer.nextToken();
        if (strNextToken.length() <= 18) {
            writeField(byteArrayOutputStream, ((long) i) + Long.parseLong(strNextToken));
        } else {
            writeField(byteArrayOutputStream, new BigInteger(strNextToken).add(BigInteger.valueOf(i)));
        }
        while (oIDTokenizer.hasMoreTokens()) {
            String strNextToken2 = oIDTokenizer.nextToken();
            if (strNextToken2.length() <= 18) {
                writeField(byteArrayOutputStream, Long.parseLong(strNextToken2));
            } else {
                writeField(byteArrayOutputStream, new BigInteger(strNextToken2));
            }
        }
    }

    private synchronized byte[] getBody() {
        if (this.body == null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            doOutput(byteArrayOutputStream);
            this.body = byteArrayOutputStream.toByteArray();
        }
        return this.body;
    }

    @Override
    boolean isConstructed() {
        return false;
    }

    @Override
    int encodedLength() throws IOException {
        int length = getBody().length;
        return 1 + StreamUtil.calculateBodyLength(length) + length;
    }

    @Override
    void encode(ASN1OutputStream aSN1OutputStream) throws IOException {
        byte[] body = getBody();
        aSN1OutputStream.write(6);
        aSN1OutputStream.writeLength(body.length);
        aSN1OutputStream.write(body);
    }

    @Override
    public int hashCode() {
        return this.identifier.hashCode();
    }

    @Override
    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        if (aSN1Primitive == this) {
            return true;
        }
        if (!(aSN1Primitive instanceof ASN1ObjectIdentifier)) {
            return false;
        }
        return this.identifier.equals(((ASN1ObjectIdentifier) aSN1Primitive).identifier);
    }

    public String toString() {
        return getId();
    }

    private static boolean isValidBranchID(String str, int i) {
        boolean z;
        char cCharAt;
        int length = str.length();
        do {
            z = false;
            while (true) {
                length--;
                if (length >= i) {
                    cCharAt = str.charAt(length);
                    if ('0' > cCharAt || cCharAt > '9') {
                        break;
                    }
                    z = true;
                } else {
                    return z;
                }
            }
            if (cCharAt != '.') {
                return false;
            }
        } while (z);
        return false;
    }

    private static boolean isValidIdentifier(String str) {
        char cCharAt;
        if (str.length() < 3 || str.charAt(1) != '.' || (cCharAt = str.charAt(0)) < '0' || cCharAt > '2') {
            return false;
        }
        return isValidBranchID(str, 2);
    }

    public ASN1ObjectIdentifier intern() {
        OidHandle oidHandle = new OidHandle(getBody());
        ASN1ObjectIdentifier aSN1ObjectIdentifier = pool.get(oidHandle);
        if (aSN1ObjectIdentifier != null) {
            return aSN1ObjectIdentifier;
        }
        ASN1ObjectIdentifier aSN1ObjectIdentifierPutIfAbsent = pool.putIfAbsent(oidHandle, this);
        return aSN1ObjectIdentifierPutIfAbsent == null ? this : aSN1ObjectIdentifierPutIfAbsent;
    }

    private static class OidHandle {
        private final byte[] enc;
        private final int key;

        OidHandle(byte[] bArr) {
            this.key = Arrays.hashCode(bArr);
            this.enc = bArr;
        }

        public int hashCode() {
            return this.key;
        }

        public boolean equals(Object obj) {
            if (obj instanceof OidHandle) {
                return Arrays.areEqual(this.enc, ((OidHandle) obj).enc);
            }
            return false;
        }
    }

    static ASN1ObjectIdentifier fromOctetString(byte[] bArr) {
        ASN1ObjectIdentifier aSN1ObjectIdentifier = pool.get(new OidHandle(bArr));
        if (aSN1ObjectIdentifier == null) {
            return new ASN1ObjectIdentifier(bArr);
        }
        return aSN1ObjectIdentifier;
    }
}
