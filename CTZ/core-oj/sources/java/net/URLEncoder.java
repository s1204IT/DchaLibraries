package java.net;

import java.io.CharArrayWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.security.AccessController;
import java.util.BitSet;
import sun.security.action.GetPropertyAction;

public class URLEncoder {
    static final int caseDiff = 32;
    static String dfltEncName;
    static BitSet dontNeedEncoding = new BitSet(256);

    static {
        dfltEncName = null;
        for (int i = 97; i <= 122; i++) {
            dontNeedEncoding.set(i);
        }
        for (int i2 = 65; i2 <= 90; i2++) {
            dontNeedEncoding.set(i2);
        }
        for (int i3 = 48; i3 <= 57; i3++) {
            dontNeedEncoding.set(i3);
        }
        dontNeedEncoding.set(32);
        dontNeedEncoding.set(45);
        dontNeedEncoding.set(95);
        dontNeedEncoding.set(46);
        dontNeedEncoding.set(42);
        dfltEncName = (String) AccessController.doPrivileged(new GetPropertyAction("file.encoding"));
    }

    private URLEncoder() {
    }

    @Deprecated
    public static String encode(String str) {
        try {
            return encode(str, dfltEncName);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static String encode(String str, String str2) throws UnsupportedEncodingException {
        BitSet bitSet;
        int i;
        char cCharAt;
        StringBuffer stringBuffer = new StringBuffer(str.length());
        CharArrayWriter charArrayWriter = new CharArrayWriter();
        if (str2 == null) {
            throw new NullPointerException("charsetName");
        }
        try {
            Charset charsetForName = Charset.forName(str2);
            int i2 = 0;
            boolean z = false;
            while (i2 < str.length()) {
                char cCharAt2 = str.charAt(i2);
                if (dontNeedEncoding.get(cCharAt2)) {
                    if (cCharAt2 == ' ') {
                        cCharAt2 = '+';
                        z = true;
                    }
                    stringBuffer.append(cCharAt2);
                    i2++;
                } else {
                    do {
                        charArrayWriter.write(cCharAt2);
                        if (cCharAt2 >= 55296 && cCharAt2 <= 56319 && (i = i2 + 1) < str.length() && (cCharAt = str.charAt(i)) >= 56320 && cCharAt <= 57343) {
                            charArrayWriter.write(cCharAt);
                            i2 = i;
                        }
                        i2++;
                        if (i2 >= str.length()) {
                            break;
                        }
                        bitSet = dontNeedEncoding;
                        cCharAt2 = str.charAt(i2);
                    } while (!bitSet.get(cCharAt2));
                    charArrayWriter.flush();
                    byte[] bytes = new String(charArrayWriter.toCharArray()).getBytes(charsetForName);
                    for (int i3 = 0; i3 < bytes.length; i3++) {
                        stringBuffer.append('%');
                        char cForDigit = Character.forDigit((bytes[i3] >> 4) & 15, 16);
                        if (Character.isLetter(cForDigit)) {
                            cForDigit = (char) (cForDigit - ' ');
                        }
                        stringBuffer.append(cForDigit);
                        char cForDigit2 = Character.forDigit(bytes[i3] & 15, 16);
                        if (Character.isLetter(cForDigit2)) {
                            cForDigit2 = (char) (cForDigit2 - ' ');
                        }
                        stringBuffer.append(cForDigit2);
                    }
                    charArrayWriter.reset();
                    z = true;
                }
            }
            return z ? stringBuffer.toString() : str;
        } catch (IllegalCharsetNameException e) {
            throw new UnsupportedEncodingException(str2);
        } catch (UnsupportedCharsetException e2) {
            throw new UnsupportedEncodingException(str2);
        }
    }
}
