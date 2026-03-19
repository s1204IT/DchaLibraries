package sun.net.util;

public class IPAddressUtil {
    private static final int INADDR16SZ = 16;
    private static final int INADDR4SZ = 4;
    private static final int INT16SZ = 2;

    public static byte[] textToNumericFormatV4(String str) {
        byte[] bArr = new byte[4];
        int length = str.length();
        if (length == 0 || length > 15) {
            return null;
        }
        long j = 0;
        boolean z = true;
        int i = 0;
        for (int i2 = 0; i2 < length; i2++) {
            char cCharAt = str.charAt(i2);
            if (cCharAt == '.') {
                if (z || j < 0 || j > 255 || i == 3) {
                    return null;
                }
                bArr[i] = (byte) (j & 255);
                i++;
                j = 0;
                z = true;
            } else {
                int iDigit = Character.digit(cCharAt, 10);
                if (iDigit < 0) {
                    return null;
                }
                j = (j * 10) + ((long) iDigit);
                z = false;
            }
        }
        if (z || j < 0 || j >= (1 << ((4 - i) * 8))) {
            return null;
        }
        switch (i) {
            case 0:
            case 1:
            case 2:
                return null;
            case 3:
                bArr[3] = (byte) ((j >> 0) & 255);
            default:
                return bArr;
        }
    }

    public static byte[] textToNumericFormatV6(String str) {
        int i;
        byte[] bArrTextToNumericFormatV4;
        if (str.length() >= 2) {
            char[] charArray = str.toCharArray();
            byte[] bArr = new byte[16];
            int length = charArray.length;
            int iIndexOf = str.indexOf("%");
            if (iIndexOf != length - 1) {
                if (iIndexOf != -1) {
                    length = iIndexOf;
                }
                if (charArray[0] != ':') {
                    i = 0;
                } else {
                    if (charArray[1] != ':') {
                        return null;
                    }
                    i = 1;
                }
                int i2 = 0;
                boolean z = false;
                int i3 = 0;
                int i4 = -1;
                int i5 = i;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    int i6 = i + 1;
                    char c = charArray[i];
                    int iDigit = Character.digit(c, 16);
                    if (iDigit != -1) {
                        i2 = (i2 << 4) | iDigit;
                        if (i2 > 65535) {
                            return null;
                        }
                        i = i6;
                        z = true;
                    } else if (c == ':') {
                        if (z) {
                            if (i6 == length || i3 + 2 > 16) {
                                return null;
                            }
                            int i7 = i3 + 1;
                            bArr[i3] = (byte) ((i2 >> 8) & 255);
                            i3 = i7 + 1;
                            bArr[i7] = (byte) (i2 & 255);
                            i = i6;
                            i5 = i;
                            i2 = 0;
                            z = false;
                        } else {
                            if (i4 != -1) {
                                return null;
                            }
                            i = i6;
                            i5 = i;
                            i4 = i3;
                        }
                    } else {
                        if (c != '.' || i3 + 4 > 16) {
                            return null;
                        }
                        String strSubstring = str.substring(i5, length);
                        int i8 = 0;
                        int i9 = 0;
                        while (true) {
                            int iIndexOf2 = strSubstring.indexOf(46, i8);
                            if (iIndexOf2 == -1) {
                                break;
                            }
                            i9++;
                            i8 = iIndexOf2 + 1;
                        }
                        if (i9 != 3 || (bArrTextToNumericFormatV4 = textToNumericFormatV4(strSubstring)) == null) {
                            return null;
                        }
                        int i10 = 0;
                        while (i10 < 4) {
                            bArr[i3] = bArrTextToNumericFormatV4[i10];
                            i10++;
                            i3++;
                        }
                        z = false;
                    }
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static boolean isIPv4LiteralAddress(String str) {
        return textToNumericFormatV4(str) != null;
    }

    public static boolean isIPv6LiteralAddress(String str) {
        return textToNumericFormatV6(str) != null;
    }

    public static byte[] convertFromIPv4MappedAddress(byte[] bArr) {
        if (isIPv4MappedAddress(bArr)) {
            byte[] bArr2 = new byte[4];
            System.arraycopy(bArr, 12, bArr2, 0, 4);
            return bArr2;
        }
        return null;
    }

    private static boolean isIPv4MappedAddress(byte[] bArr) {
        return bArr.length >= 16 && bArr[0] == 0 && bArr[1] == 0 && bArr[2] == 0 && bArr[3] == 0 && bArr[4] == 0 && bArr[5] == 0 && bArr[6] == 0 && bArr[7] == 0 && bArr[8] == 0 && bArr[9] == 0 && bArr[10] == -1 && bArr[11] == -1;
    }
}
