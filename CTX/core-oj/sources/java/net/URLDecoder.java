package java.net;

import java.io.UnsupportedEncodingException;

public class URLDecoder {
    static String dfltEncName = URLEncoder.dfltEncName;

    @Deprecated
    public static String decode(String str) {
        try {
            return decode(str, dfltEncName);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static String decode(String str, String str2) throws UnsupportedEncodingException {
        int length = str.length();
        StringBuffer stringBuffer = new StringBuffer(length > 500 ? length / 2 : length);
        if (str2.length() == 0) {
            throw new UnsupportedEncodingException("URLDecoder: empty string enc parameter");
        }
        byte[] bArr = null;
        int i = 0;
        boolean z = false;
        loop0: while (i < length) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '%') {
                if (bArr == null) {
                    try {
                        bArr = new byte[(length - i) / 3];
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("URLDecoder: Illegal hex characters in escape (%) pattern - " + e.getMessage());
                    }
                }
                int i2 = 0;
                while (true) {
                    int i3 = i + 2;
                    if (i3 >= length || cCharAt != '%') {
                        break;
                    }
                    int i4 = i + 1;
                    if (!isValidHexChar(str.charAt(i4)) || !isValidHexChar(str.charAt(i3))) {
                        break loop0;
                    }
                    int i5 = i + 3;
                    int i6 = Integer.parseInt(str.substring(i4, i5), 16);
                    if (i6 < 0) {
                        throw new IllegalArgumentException("URLDecoder: Illegal hex characters in escape (%) pattern - negative value : " + str.substring(i, i5));
                    }
                    int i7 = i2 + 1;
                    bArr[i2] = (byte) i6;
                    if (i5 < length) {
                        cCharAt = str.charAt(i5);
                    }
                    i2 = i7;
                    i = i5;
                }
            } else if (cCharAt == '+') {
                stringBuffer.append(' ');
                i++;
            } else {
                stringBuffer.append(cCharAt);
                i++;
            }
            z = true;
        }
        return z ? stringBuffer.toString() : str;
    }

    private static boolean isValidHexChar(char c) {
        return ('0' <= c && c <= '9') || ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F');
    }
}
