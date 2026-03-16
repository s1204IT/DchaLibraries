package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public class DERObjectIdentifier extends ASN1Primitive {
    private static final long LONG_LIMIT = 72057594037927808L;
    private static ASN1ObjectIdentifier[][] cache = new ASN1ObjectIdentifier[256][];
    private byte[] body;
    String identifier;

    public static ASN1ObjectIdentifier getInstance(Object obj) {
        if (obj == null || (obj instanceof ASN1ObjectIdentifier)) {
            return (ASN1ObjectIdentifier) obj;
        }
        if (obj instanceof DERObjectIdentifier) {
            return new ASN1ObjectIdentifier(((DERObjectIdentifier) obj).getId());
        }
        if ((obj instanceof ASN1Encodable) && (((ASN1Encodable) obj).toASN1Primitive() instanceof ASN1ObjectIdentifier)) {
            return (ASN1ObjectIdentifier) ((ASN1Encodable) obj).toASN1Primitive();
        }
        if (obj instanceof byte[]) {
            byte[] enc = (byte[]) obj;
            if (enc[0] == 6) {
                try {
                    return (ASN1ObjectIdentifier) fromByteArray(enc);
                } catch (IOException e) {
                    throw new IllegalArgumentException("failed to construct sequence from byte[]: " + e.getMessage());
                }
            }
            return ASN1ObjectIdentifier.fromOctetString((byte[]) obj);
        }
        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    public static ASN1ObjectIdentifier getInstance(ASN1TaggedObject obj, boolean explicit) {
        ASN1Primitive o = obj.getObject();
        return (explicit || (o instanceof DERObjectIdentifier)) ? getInstance(o) : ASN1ObjectIdentifier.fromOctetString(ASN1OctetString.getInstance(obj.getObject()).getOctets());
    }

    DERObjectIdentifier(byte[] bytes) {
        StringBuffer objId = new StringBuffer();
        long value = 0;
        BigInteger bigValue = null;
        boolean first = true;
        for (int i = 0; i != bytes.length; i++) {
            int b = bytes[i] & 255;
            if (value <= LONG_LIMIT) {
                long value2 = value + ((long) (b & 127));
                if ((b & 128) == 0) {
                    if (first) {
                        if (value2 < 40) {
                            objId.append('0');
                        } else if (value2 < 80) {
                            objId.append('1');
                            value2 -= 40;
                        } else {
                            objId.append('2');
                            value2 -= 80;
                        }
                        first = false;
                    }
                    objId.append('.');
                    objId.append(value2);
                    value = 0;
                } else {
                    value = value2 << 7;
                }
            } else {
                BigInteger bigValue2 = (bigValue == null ? BigInteger.valueOf(value) : bigValue).or(BigInteger.valueOf(b & 127));
                if ((b & 128) == 0) {
                    if (first) {
                        objId.append('2');
                        bigValue2 = bigValue2.subtract(BigInteger.valueOf(80L));
                        first = false;
                    }
                    objId.append('.');
                    objId.append(bigValue2);
                    bigValue = null;
                    value = 0;
                } else {
                    bigValue = bigValue2.shiftLeft(7);
                }
            }
        }
        this.identifier = objId.toString().intern();
        this.body = Arrays.clone(bytes);
    }

    public DERObjectIdentifier(String identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("'identifier' cannot be null");
        }
        if (!isValidIdentifier(identifier)) {
            throw new IllegalArgumentException("string " + identifier + " not an OID");
        }
        this.identifier = identifier.intern();
    }

    DERObjectIdentifier(DERObjectIdentifier oid, String branchID) {
        if (!isValidBranchID(branchID, 0)) {
            throw new IllegalArgumentException("string " + branchID + " not a valid OID branch");
        }
        this.identifier = oid.getId() + "." + branchID;
    }

    public String getId() {
        return this.identifier;
    }

    private void writeField(ByteArrayOutputStream out, long fieldValue) {
        byte[] result = new byte[9];
        int pos = 8;
        result[8] = (byte) (((int) fieldValue) & 127);
        while (fieldValue >= 128) {
            fieldValue >>= 7;
            pos--;
            result[pos] = (byte) ((((int) fieldValue) & 127) | 128);
        }
        out.write(result, pos, 9 - pos);
    }

    private void writeField(ByteArrayOutputStream out, BigInteger fieldValue) {
        int byteCount = (fieldValue.bitLength() + 6) / 7;
        if (byteCount == 0) {
            out.write(0);
            return;
        }
        BigInteger tmpValue = fieldValue;
        byte[] tmp = new byte[byteCount];
        for (int i = byteCount - 1; i >= 0; i--) {
            tmp[i] = (byte) ((tmpValue.intValue() & 127) | 128);
            tmpValue = tmpValue.shiftRight(7);
        }
        int i2 = byteCount - 1;
        tmp[i2] = (byte) (tmp[i2] & 127);
        out.write(tmp, 0, tmp.length);
    }

    private void doOutput(ByteArrayOutputStream aOut) {
        OIDTokenizer tok = new OIDTokenizer(this.identifier);
        int first = Integer.parseInt(tok.nextToken()) * 40;
        String secondToken = tok.nextToken();
        if (secondToken.length() <= 18) {
            writeField(aOut, ((long) first) + Long.parseLong(secondToken));
        } else {
            writeField(aOut, new BigInteger(secondToken).add(BigInteger.valueOf(first)));
        }
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            if (token.length() <= 18) {
                writeField(aOut, Long.parseLong(token));
            } else {
                writeField(aOut, new BigInteger(token));
            }
        }
    }

    protected synchronized byte[] getBody() {
        if (this.body == null) {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            doOutput(bOut);
            this.body = bOut.toByteArray();
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
        return StreamUtil.calculateBodyLength(length) + 1 + length;
    }

    @Override
    void encode(ASN1OutputStream out) throws IOException {
        byte[] enc = getBody();
        out.write(6);
        out.writeLength(enc.length);
        out.write(enc);
    }

    @Override
    public int hashCode() {
        return this.identifier.hashCode();
    }

    @Override
    boolean asn1Equals(ASN1Primitive o) {
        if (o instanceof DERObjectIdentifier) {
            return this.identifier.equals(((DERObjectIdentifier) o).identifier);
        }
        return false;
    }

    public String toString() {
        return getId();
    }

    private static boolean isValidBranchID(String branchID, int start) {
        boolean periodAllowed = false;
        int pos = branchID.length();
        while (true) {
            pos--;
            if (pos >= start) {
                char ch = branchID.charAt(pos);
                if ('0' <= ch && ch <= '9') {
                    periodAllowed = true;
                } else {
                    if (ch == '.' && periodAllowed) {
                        periodAllowed = false;
                    }
                    return false;
                }
            } else {
                return periodAllowed;
            }
        }
    }

    private static boolean isValidIdentifier(String identifier) {
        char first;
        if (identifier.length() < 3 || identifier.charAt(1) != '.' || (first = identifier.charAt(0)) < '0' || first > '2') {
            return false;
        }
        return isValidBranchID(identifier, 2);
    }

    static ASN1ObjectIdentifier fromOctetString(byte[] enc) {
        if (enc.length < 3) {
            return new ASN1ObjectIdentifier(enc);
        }
        int idx1 = enc[enc.length - 2] & 255;
        int idx2 = enc[enc.length - 1] & 127;
        synchronized (cache) {
            ASN1ObjectIdentifier[] first = cache[idx1];
            if (first == null) {
                first = new ASN1ObjectIdentifier[128];
                cache[idx1] = first;
            }
            ASN1ObjectIdentifier possibleMatch = first[idx2];
            if (possibleMatch == null) {
                ASN1ObjectIdentifier possibleMatch2 = new ASN1ObjectIdentifier(enc);
                first[idx2] = possibleMatch2;
                return possibleMatch2;
            }
            if (!Arrays.areEqual(enc, possibleMatch.getBody())) {
                int idx12 = (idx1 + 1) & 255;
                ASN1ObjectIdentifier[] first2 = cache[idx12];
                if (first2 == null) {
                    first2 = new ASN1ObjectIdentifier[128];
                    cache[idx12] = first2;
                }
                ASN1ObjectIdentifier possibleMatch3 = first2[idx2];
                if (possibleMatch3 == null) {
                    ASN1ObjectIdentifier possibleMatch4 = new ASN1ObjectIdentifier(enc);
                    first2[idx2] = possibleMatch4;
                    return possibleMatch4;
                }
                if (!Arrays.areEqual(enc, possibleMatch3.getBody())) {
                    int idx22 = (idx2 + 1) & 127;
                    ASN1ObjectIdentifier possibleMatch5 = first2[idx22];
                    if (possibleMatch5 != null) {
                        return !Arrays.areEqual(enc, possibleMatch5.getBody()) ? new ASN1ObjectIdentifier(enc) : possibleMatch5;
                    }
                    ASN1ObjectIdentifier possibleMatch6 = new ASN1ObjectIdentifier(enc);
                    first2[idx22] = possibleMatch6;
                    return possibleMatch6;
                }
                return possibleMatch3;
            }
            return possibleMatch;
        }
    }
}
