package gov.nist.javax.sip.address;

import java.io.UnsupportedEncodingException;

public class RFC2396UrlDecoder {
    public static String decode(String str) {
        StringBuffer stringBuffer = new StringBuffer(str.length());
        byte[] bArr = new byte[str.length() / 3];
        int length = str.length();
        int i = 0;
        while (i < length) {
            if (str.charAt(i) == '%') {
                int i2 = 0;
                while (i < length && str.charAt(i) == '%') {
                    if (i + 2 < length) {
                        int i3 = i + 1;
                        i += 3;
                        try {
                            bArr[i2] = (byte) Integer.parseInt(str.substring(i3, i), 16);
                            i2++;
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Illegal hex characters in pattern %" + str.substring(i3, i));
                        }
                    } else {
                        throw new IllegalArgumentException("% character should be followed by 2 hexadecimal characters.");
                    }
                }
                try {
                    stringBuffer.append(new String(bArr, 0, i2, "UTF-8"));
                } catch (UnsupportedEncodingException e2) {
                    throw new RuntimeException("Problem in decodePath: UTF-8 encoding not supported.");
                }
            } else {
                stringBuffer.append(str.charAt(i));
                i++;
            }
        }
        return stringBuffer.toString();
    }
}
