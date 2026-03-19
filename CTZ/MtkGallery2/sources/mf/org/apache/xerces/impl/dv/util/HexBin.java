package mf.org.apache.xerces.impl.dv.util;

public final class HexBin {
    private static final int BASELENGTH = 128;
    private static final int LOOKUPLENGTH = 16;
    private static final byte[] hexNumberTable = new byte[128];
    private static final char[] lookUpHexAlphabet = new char[16];

    static {
        for (int i = 0; i < 128; i++) {
            hexNumberTable[i] = -1;
        }
        for (int i2 = 57; i2 >= 48; i2--) {
            hexNumberTable[i2] = (byte) (i2 - 48);
        }
        for (int i3 = 70; i3 >= 65; i3--) {
            hexNumberTable[i3] = (byte) ((i3 - 65) + 10);
        }
        for (int i4 = 102; i4 >= 97; i4--) {
            hexNumberTable[i4] = (byte) ((i4 - 97) + 10);
        }
        for (int i5 = 0; i5 < 10; i5++) {
            lookUpHexAlphabet[i5] = (char) (48 + i5);
        }
        for (int i6 = 10; i6 <= 15; i6++) {
            lookUpHexAlphabet[i6] = (char) ((65 + i6) - 10);
        }
    }

    public static String encode(byte[] bArr) {
        if (bArr == 0) {
            return null;
        }
        int lengthData = bArr.length;
        int lengthEncode = lengthData * 2;
        char[] encodedData = new char[lengthEncode];
        for (int i = 0; i < lengthData; i++) {
            int i2 = bArr[i];
            if (i2 < 0) {
                i2 += 256;
            }
            encodedData[i * 2] = lookUpHexAlphabet[i2 >> 4];
            encodedData[(i * 2) + 1] = lookUpHexAlphabet[i2 & 15];
        }
        return new String(encodedData);
    }

    public static byte[] decode(String encoded) {
        if (encoded == null) {
            return null;
        }
        int lengthData = encoded.length();
        if (lengthData % 2 != 0) {
            return null;
        }
        char[] binaryData = encoded.toCharArray();
        int lengthDecode = lengthData / 2;
        byte[] decodedData = new byte[lengthDecode];
        for (int i = 0; i < lengthDecode; i++) {
            char tempChar = binaryData[i * 2];
            byte temp1 = tempChar < 128 ? hexNumberTable[tempChar] : (byte) -1;
            if (temp1 == -1) {
                return null;
            }
            char tempChar2 = binaryData[(i * 2) + 1];
            byte temp2 = tempChar2 < 128 ? hexNumberTable[tempChar2] : (byte) -1;
            if (temp2 == -1) {
                return null;
            }
            decodedData[i] = (byte) ((temp1 << 4) | temp2);
        }
        return decodedData;
    }
}
