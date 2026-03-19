package mf.org.apache.xerces.impl.dv.util;

public final class Base64 {
    private static final int BASELENGTH = 128;
    private static final int EIGHTBIT = 8;
    private static final int FOURBYTE = 4;
    private static final int LOOKUPLENGTH = 64;
    private static final char PAD = '=';
    private static final int SIGN = -128;
    private static final int SIXBIT = 6;
    private static final int SIXTEENBIT = 16;
    private static final int TWENTYFOURBITGROUP = 24;
    private static final boolean fDebug = false;
    private static final byte[] base64Alphabet = new byte[128];
    private static final char[] lookUpBase64Alphabet = new char[64];

    static {
        for (int i = 0; i < 128; i++) {
            base64Alphabet[i] = -1;
        }
        for (int i2 = 90; i2 >= 65; i2--) {
            base64Alphabet[i2] = (byte) (i2 - 65);
        }
        for (int i3 = 122; i3 >= 97; i3--) {
            base64Alphabet[i3] = (byte) ((i3 - 97) + 26);
        }
        for (int i4 = 57; i4 >= 48; i4--) {
            base64Alphabet[i4] = (byte) ((i4 - 48) + 52);
        }
        base64Alphabet[43] = 62;
        base64Alphabet[47] = 63;
        for (int i5 = 0; i5 <= 25; i5++) {
            lookUpBase64Alphabet[i5] = (char) (65 + i5);
        }
        int i6 = 26;
        int j = 0;
        while (i6 <= 51) {
            lookUpBase64Alphabet[i6] = (char) (97 + j);
            i6++;
            j++;
        }
        int i7 = 52;
        int j2 = 0;
        while (i7 <= 61) {
            lookUpBase64Alphabet[i7] = (char) (48 + j2);
            i7++;
            j2++;
        }
        lookUpBase64Alphabet[62] = '+';
        lookUpBase64Alphabet[63] = '/';
    }

    protected static boolean isWhiteSpace(char octect) {
        return octect == ' ' || octect == '\r' || octect == '\n' || octect == '\t';
    }

    protected static boolean isPad(char octect) {
        return octect == '=';
    }

    protected static boolean isData(char octect) {
        return octect < 128 && base64Alphabet[octect] != -1;
    }

    protected static boolean isBase64(char octect) {
        return isWhiteSpace(octect) || isPad(octect) || isData(octect);
    }

    public static String encode(byte[] binaryData) {
        byte[] bArr = binaryData;
        if (bArr == null) {
            return null;
        }
        int i = 8;
        int lengthDataBits = bArr.length * 8;
        if (lengthDataBits == 0) {
            return "";
        }
        int fewerThan24bits = lengthDataBits % 24;
        int numberTriplets = lengthDataBits / 24;
        int numberQuartet = fewerThan24bits != 0 ? numberTriplets + 1 : numberTriplets;
        char[] encodedData = new char[numberQuartet * 4];
        int encodedIndex = 0;
        int dataIndex = 0;
        int i2 = 0;
        while (i2 < numberTriplets) {
            int dataIndex2 = dataIndex + 1;
            byte b1 = bArr[dataIndex];
            int dataIndex3 = dataIndex2 + 1;
            byte b2 = bArr[dataIndex2];
            int dataIndex4 = dataIndex3 + 1;
            byte b3 = bArr[dataIndex3];
            int dataIndex5 = b2 & 15;
            byte l = (byte) dataIndex5;
            byte k = (byte) (b1 & 3);
            byte val1 = (byte) ((b1 & (-128)) == 0 ? b1 >> 2 : (b1 >> 2) ^ 192);
            byte val2 = (byte) ((b2 & (-128)) == 0 ? b2 >> 4 : (b2 >> 4) ^ 240);
            byte val3 = (byte) ((b3 & (-128)) == 0 ? b3 >> 6 : (b3 >> 6) ^ 252);
            int encodedIndex2 = encodedIndex + 1;
            encodedData[encodedIndex] = lookUpBase64Alphabet[val1];
            int encodedIndex3 = encodedIndex2 + 1;
            encodedData[encodedIndex2] = lookUpBase64Alphabet[val2 | (k << 4)];
            int encodedIndex4 = encodedIndex3 + 1;
            encodedData[encodedIndex3] = lookUpBase64Alphabet[(l << 2) | val3];
            encodedIndex = encodedIndex4 + 1;
            encodedData[encodedIndex4] = lookUpBase64Alphabet[b3 & 63];
            i2++;
            dataIndex = dataIndex4;
            bArr = binaryData;
            i = 8;
        }
        if (fewerThan24bits == i) {
            byte b12 = bArr[dataIndex];
            byte k2 = (byte) (b12 & 3);
            byte val12 = (byte) ((b12 & (-128)) == 0 ? b12 >> 2 : (b12 >> 2) ^ 192);
            int encodedIndex5 = encodedIndex + 1;
            encodedData[encodedIndex] = lookUpBase64Alphabet[val12];
            int encodedIndex6 = encodedIndex5 + 1;
            encodedData[encodedIndex5] = lookUpBase64Alphabet[k2 << 4];
            int encodedIndex7 = encodedIndex6 + 1;
            encodedData[encodedIndex6] = PAD;
            int encodedIndex8 = encodedIndex7 + 1;
            encodedData[encodedIndex7] = PAD;
        } else if (fewerThan24bits == 16) {
            byte b13 = bArr[dataIndex];
            byte b22 = bArr[dataIndex + 1];
            byte l2 = (byte) (b22 & 15);
            byte k3 = (byte) (b13 & 3);
            byte val13 = (byte) ((b13 & (-128)) == 0 ? b13 >> 2 : (b13 >> 2) ^ 192);
            byte val22 = (byte) ((b22 & (-128)) == 0 ? b22 >> 4 : (b22 >> 4) ^ 240);
            int encodedIndex9 = encodedIndex + 1;
            encodedData[encodedIndex] = lookUpBase64Alphabet[val13];
            int encodedIndex10 = encodedIndex9 + 1;
            encodedData[encodedIndex9] = lookUpBase64Alphabet[val22 | (k3 << 4)];
            int encodedIndex11 = encodedIndex10 + 1;
            encodedData[encodedIndex10] = lookUpBase64Alphabet[l2 << 2];
            int encodedIndex12 = encodedIndex11 + 1;
            encodedData[encodedIndex11] = PAD;
        }
        return new String(encodedData);
    }

    public static byte[] decode(String encoded) {
        if (encoded == null) {
            return null;
        }
        char[] base64Data = encoded.toCharArray();
        int len = removeWhiteSpace(base64Data);
        if (len % 4 != 0) {
            return null;
        }
        int numberQuadruple = len / 4;
        if (numberQuadruple == 0) {
            return new byte[0];
        }
        int i = 0;
        int encodedIndex = 0;
        int dataIndex = 0;
        byte[] decodedData = new byte[numberQuadruple * 3];
        while (i < numberQuadruple - 1) {
            int len2 = len;
            int dataIndex2 = dataIndex + 1;
            char d1 = base64Data[dataIndex];
            if (!isData(d1)) {
                return null;
            }
            int dataIndex3 = dataIndex2 + 1;
            char c = base64Data[dataIndex2];
            if (!isData(c)) {
                return null;
            }
            int dataIndex4 = dataIndex3 + 1;
            char c2 = base64Data[dataIndex3];
            if (!isData(c2)) {
                return null;
            }
            dataIndex = dataIndex4 + 1;
            char c3 = base64Data[dataIndex4];
            if (!isData(c3)) {
                return null;
            }
            byte b1 = base64Alphabet[d1];
            byte b2 = base64Alphabet[c];
            byte b3 = base64Alphabet[c2];
            byte b4 = base64Alphabet[c3];
            int encodedIndex2 = encodedIndex + 1;
            decodedData[encodedIndex] = (byte) ((b1 << 2) | (b2 >> 4));
            int encodedIndex3 = encodedIndex2 + 1;
            decodedData[encodedIndex2] = (byte) (((b2 & 15) << 4) | ((b3 >> 2) & 15));
            encodedIndex = encodedIndex3 + 1;
            decodedData[encodedIndex3] = (byte) ((b3 << 6) | b4);
            i++;
            len = len2;
        }
        int dataIndex5 = dataIndex + 1;
        char d12 = base64Data[dataIndex];
        if (!isData(d12)) {
            return null;
        }
        int dataIndex6 = dataIndex5 + 1;
        char c4 = base64Data[dataIndex5];
        if (!isData(c4)) {
            return null;
        }
        byte b12 = base64Alphabet[d12];
        byte b22 = base64Alphabet[c4];
        int dataIndex7 = dataIndex6 + 1;
        char d3 = base64Data[dataIndex6];
        int i2 = dataIndex7 + 1;
        char d4 = base64Data[dataIndex7];
        if (isData(d3) && isData(d4)) {
            byte b32 = base64Alphabet[d3];
            byte b42 = base64Alphabet[d4];
            int encodedIndex4 = encodedIndex + 1;
            decodedData[encodedIndex] = (byte) ((b12 << 2) | (b22 >> 4));
            int encodedIndex5 = encodedIndex4 + 1;
            decodedData[encodedIndex4] = (byte) (((b22 & 15) << 4) | ((b32 >> 2) & 15));
            int i3 = encodedIndex5 + 1;
            decodedData[encodedIndex5] = (byte) ((b32 << 6) | b42);
            return decodedData;
        }
        if (isPad(d3) && isPad(d4)) {
            if ((b22 & 15) != 0) {
                return null;
            }
            byte[] tmp = new byte[(i * 3) + 1];
            System.arraycopy(decodedData, 0, tmp, 0, i * 3);
            tmp[encodedIndex] = (byte) ((b12 << 2) | (b22 >> 4));
            return tmp;
        }
        if (!isPad(d3) && isPad(d4)) {
            byte b33 = base64Alphabet[d3];
            if ((b33 & 3) != 0) {
                return null;
            }
            byte[] tmp2 = new byte[(i * 3) + 2];
            System.arraycopy(decodedData, 0, tmp2, 0, i * 3);
            tmp2[encodedIndex] = (byte) ((b12 << 2) | (b22 >> 4));
            tmp2[encodedIndex + 1] = (byte) (((b22 & 15) << 4) | ((b33 >> 2) & 15));
            return tmp2;
        }
        return null;
    }

    protected static int removeWhiteSpace(char[] data) {
        if (data == null) {
            return 0;
        }
        int newSize = 0;
        int len = data.length;
        for (int i = 0; i < len; i++) {
            if (!isWhiteSpace(data[i])) {
                data[newSize] = data[i];
                newSize++;
            }
        }
        return newSize;
    }
}
