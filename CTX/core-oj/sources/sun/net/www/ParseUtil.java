package sun.net.www;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.BitSet;
import sun.nio.cs.ThreadLocalCoders;
import sun.util.locale.LanguageTag;

public class ParseUtil {
    static final boolean $assertionsDisabled = false;
    private static final long H_ALPHA;
    private static final long H_ALPHANUM;
    private static final long H_DASH;
    private static final long H_DIGIT = 0;
    private static final long H_ESCAPED = 0;
    private static final long H_HEX;
    private static final long H_LOWALPHA;
    private static final long H_MARK;
    private static final long H_PATH;
    private static final long H_PCHAR;
    private static final long H_REG_NAME;
    private static final long H_RESERVED;
    private static final long H_SERVER;
    private static final long H_UNRESERVED;
    private static final long H_UPALPHA;
    private static final long H_URIC;
    private static final long H_USERINFO;
    private static final long L_ALPHA = 0;
    private static final long L_ALPHANUM;
    private static final long L_DASH;
    private static final long L_DIGIT;
    private static final long L_ESCAPED = 1;
    private static final long L_HEX;
    private static final long L_LOWALPHA = 0;
    private static final long L_MARK;
    private static final long L_PATH;
    private static final long L_PCHAR;
    private static final long L_REG_NAME;
    private static final long L_RESERVED;
    private static final long L_SERVER;
    private static final long L_UNRESERVED;
    private static final long L_UPALPHA = 0;
    private static final long L_URIC;
    private static final long L_USERINFO;
    static BitSet encodedInPath = new BitSet(256);
    private static final char[] hexDigits;

    static {
        encodedInPath.set(61);
        encodedInPath.set(59);
        encodedInPath.set(63);
        encodedInPath.set(47);
        encodedInPath.set(35);
        encodedInPath.set(32);
        encodedInPath.set(60);
        encodedInPath.set(62);
        encodedInPath.set(37);
        encodedInPath.set(34);
        encodedInPath.set(123);
        encodedInPath.set(125);
        encodedInPath.set(124);
        encodedInPath.set(92);
        encodedInPath.set(94);
        encodedInPath.set(91);
        encodedInPath.set(93);
        encodedInPath.set(96);
        for (int i = 0; i < 32; i++) {
            encodedInPath.set(i);
        }
        encodedInPath.set(127);
        hexDigits = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        L_DIGIT = lowMask('0', '9');
        L_HEX = L_DIGIT;
        H_HEX = highMask('A', 'F') | highMask('a', 'f');
        H_UPALPHA = highMask('A', 'Z');
        H_LOWALPHA = highMask('a', 'z');
        H_ALPHA = H_LOWALPHA | H_UPALPHA;
        L_ALPHANUM = L_DIGIT | 0;
        H_ALPHANUM = H_ALPHA | 0;
        L_MARK = lowMask("-_.!~*'()");
        H_MARK = highMask("-_.!~*'()");
        L_UNRESERVED = L_ALPHANUM | L_MARK;
        H_UNRESERVED = H_ALPHANUM | H_MARK;
        L_RESERVED = lowMask(";/?:@&=+$,[]");
        H_RESERVED = highMask(";/?:@&=+$,[]");
        L_DASH = lowMask(LanguageTag.SEP);
        H_DASH = highMask(LanguageTag.SEP);
        L_URIC = L_RESERVED | L_UNRESERVED | L_ESCAPED;
        H_URIC = H_RESERVED | H_UNRESERVED | 0;
        L_PCHAR = L_UNRESERVED | L_ESCAPED | lowMask(":@&=+$,");
        H_PCHAR = H_UNRESERVED | 0 | highMask(":@&=+$,");
        L_PATH = L_PCHAR | lowMask(";/");
        H_PATH = H_PCHAR | highMask(";/");
        L_USERINFO = L_UNRESERVED | L_ESCAPED | lowMask(";:&=+$,");
        H_USERINFO = H_UNRESERVED | 0 | highMask(";:&=+$,");
        L_REG_NAME = L_UNRESERVED | L_ESCAPED | lowMask("$,;:@&=+");
        H_REG_NAME = H_UNRESERVED | 0 | highMask("$,;:@&=+");
        L_SERVER = L_USERINFO | L_ALPHANUM | L_DASH | lowMask(".:@[]");
        H_SERVER = H_USERINFO | H_ALPHANUM | H_DASH | highMask(".:@[]");
    }

    public static String encodePath(String str) {
        return encodePath(str, true);
    }

    public static String encodePath(String str, boolean z) {
        char[] cArr = new char[(str.length() * 2) + 16];
        char[] charArray = str.toCharArray();
        int length = str.length();
        char[] cArr2 = cArr;
        int iEscape = 0;
        for (int i = 0; i < length; i++) {
            char c = charArray[i];
            if ((!z && c == '/') || (z && c == File.separatorChar)) {
                cArr2[iEscape] = '/';
                iEscape++;
            } else if (c <= 127) {
                if ((c < 'a' || c > 'z') && ((c < 'A' || c > 'Z') && ((c < '0' || c > '9') && encodedInPath.get(c)))) {
                    iEscape = escape(cArr2, c, iEscape);
                } else {
                    int i2 = iEscape + 1;
                    cArr2[iEscape] = c;
                    iEscape = i2;
                }
            } else if (c > 2047) {
                iEscape = escape(cArr2, (char) (((c >> 0) & 63) | 128), escape(cArr2, (char) (((c >> 6) & 63) | 128), escape(cArr2, (char) (224 | ((c >> '\f') & 15)), iEscape)));
            } else {
                iEscape = escape(cArr2, (char) (((c >> 0) & 63) | 128), escape(cArr2, (char) (192 | ((c >> 6) & 31)), iEscape));
            }
            if (iEscape + 9 > cArr2.length) {
                int length2 = (cArr2.length * 2) + 16;
                if (length2 < 0) {
                    length2 = Integer.MAX_VALUE;
                }
                char[] cArr3 = new char[length2];
                System.arraycopy((Object) cArr2, 0, (Object) cArr3, 0, iEscape);
                cArr2 = cArr3;
            }
        }
        return new String(cArr2, 0, iEscape);
    }

    private static int escape(char[] cArr, char c, int i) {
        int i2 = i + 1;
        cArr[i] = '%';
        int i3 = i2 + 1;
        cArr[i2] = Character.forDigit((c >> 4) & 15, 16);
        int i4 = i3 + 1;
        cArr[i3] = Character.forDigit(c & 15, 16);
        return i4;
    }

    private static byte unescape(String str, int i) {
        return (byte) Integer.parseInt(str.substring(i + 1, i + 3), 16);
    }

    public static String decode(String str) {
        int length = str.length();
        if (length == 0 || str.indexOf(37) < 0) {
            return str;
        }
        StringBuilder sb = new StringBuilder(length);
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(length);
        CharBuffer charBufferAllocate = CharBuffer.allocate(length);
        CharsetDecoder charsetDecoderOnUnmappableCharacter = ThreadLocalCoders.decoderFor("UTF-8").onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        int i = 0;
        char cCharAt = str.charAt(0);
        while (i < length) {
            if (cCharAt != '%') {
                sb.append(cCharAt);
                i++;
                if (i >= length) {
                    break;
                }
                cCharAt = str.charAt(i);
            } else {
                byteBufferAllocate.clear();
                do {
                    try {
                        byteBufferAllocate.put(unescape(str, i));
                        i += 3;
                        if (i >= length) {
                            break;
                        }
                        cCharAt = str.charAt(i);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException();
                    }
                } while (cCharAt == '%');
                byteBufferAllocate.flip();
                charBufferAllocate.clear();
                charsetDecoderOnUnmappableCharacter.reset();
                if (charsetDecoderOnUnmappableCharacter.decode(byteBufferAllocate, charBufferAllocate, true).isError()) {
                    throw new IllegalArgumentException("Error decoding percent encoded characters");
                }
                if (charsetDecoderOnUnmappableCharacter.flush(charBufferAllocate).isError()) {
                    throw new IllegalArgumentException("Error decoding percent encoded characters");
                }
                sb.append(charBufferAllocate.flip().toString());
            }
        }
        return sb.toString();
    }

    public String canonizeString(String str) {
        str.length();
        while (true) {
            int iIndexOf = str.indexOf("/../");
            if (iIndexOf < 0) {
                break;
            }
            int iLastIndexOf = str.lastIndexOf(47, iIndexOf - 1);
            if (iLastIndexOf >= 0) {
                str = str.substring(0, iLastIndexOf) + str.substring(iIndexOf + 3);
            } else {
                str = str.substring(iIndexOf + 3);
            }
        }
        while (true) {
            int iIndexOf2 = str.indexOf("/./");
            if (iIndexOf2 < 0) {
                break;
            }
            str = str.substring(0, iIndexOf2) + str.substring(iIndexOf2 + 2);
        }
        while (str.endsWith("/..")) {
            int iIndexOf3 = str.indexOf("/..");
            int iLastIndexOf2 = str.lastIndexOf(47, iIndexOf3 - 1);
            if (iLastIndexOf2 >= 0) {
                str = str.substring(0, iLastIndexOf2 + 1);
            } else {
                str = str.substring(0, iIndexOf3);
            }
        }
        if (str.endsWith("/.")) {
            return str.substring(0, str.length() - 1);
        }
        return str;
    }

    public static URL fileToEncodedURL(File file) throws MalformedURLException {
        String strEncodePath = encodePath(file.getAbsolutePath());
        if (!strEncodePath.startsWith("/")) {
            strEncodePath = "/" + strEncodePath;
        }
        if (!strEncodePath.endsWith("/") && file.isDirectory()) {
            strEncodePath = strEncodePath + "/";
        }
        return new URL("file", "", strEncodePath);
    }

    public static URI toURI(URL url) {
        String protocol = url.getProtocol();
        String authority = url.getAuthority();
        String path = url.getPath();
        String query = url.getQuery();
        String ref = url.getRef();
        if (path != null && !path.startsWith("/")) {
            path = "/" + path;
        }
        if (authority != null && authority.endsWith(":-1")) {
            authority = authority.substring(0, authority.length() - 3);
        }
        try {
            return createURI(protocol, authority, path, query, ref);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static URI createURI(String str, String str2, String str3, String str4, String str5) throws URISyntaxException {
        String string = toString(str, null, str2, null, null, -1, str3, str4, str5);
        checkPath(string, str, str3);
        return new URI(string);
    }

    private static String toString(String str, String str2, String str3, String str4, String str5, int i, String str6, String str7, String str8) {
        StringBuffer stringBuffer = new StringBuffer();
        if (str != null) {
            stringBuffer.append(str);
            stringBuffer.append(':');
        }
        appendSchemeSpecificPart(stringBuffer, str2, str3, str4, str5, i, str6, str7);
        appendFragment(stringBuffer, str8);
        return stringBuffer.toString();
    }

    private static void appendSchemeSpecificPart(StringBuffer stringBuffer, String str, String str2, String str3, String str4, int i, String str5, String str6) {
        String strSubstring;
        if (str != null) {
            if (str.startsWith("//[")) {
                int iIndexOf = str.indexOf("]");
                if (iIndexOf != -1 && str.indexOf(":") != -1) {
                    if (iIndexOf == str.length()) {
                        strSubstring = "";
                    } else {
                        int i2 = iIndexOf + 1;
                        String strSubstring2 = str.substring(0, i2);
                        strSubstring = str.substring(i2);
                        str = strSubstring2;
                    }
                    stringBuffer.append(str);
                    stringBuffer.append(quote(strSubstring, L_URIC, H_URIC));
                    return;
                }
                return;
            }
            stringBuffer.append(quote(str, L_URIC, H_URIC));
            return;
        }
        appendAuthority(stringBuffer, str2, str3, str4, i);
        if (str5 != null) {
            stringBuffer.append(quote(str5, L_PATH, H_PATH));
        }
        if (str6 != null) {
            stringBuffer.append('?');
            stringBuffer.append(quote(str6, L_URIC, H_URIC));
        }
    }

    private static void appendAuthority(StringBuffer stringBuffer, String str, String str2, String str3, int i) {
        String strSubstring;
        boolean z = $assertionsDisabled;
        if (str3 != null) {
            stringBuffer.append("//");
            if (str2 != null) {
                stringBuffer.append(quote(str2, L_USERINFO, H_USERINFO));
                stringBuffer.append('@');
            }
            if (str3.indexOf(58) >= 0 && !str3.startsWith("[") && !str3.endsWith("]")) {
                z = true;
            }
            if (z) {
                stringBuffer.append('[');
            }
            stringBuffer.append(str3);
            if (z) {
                stringBuffer.append(']');
            }
            if (i != -1) {
                stringBuffer.append(':');
                stringBuffer.append(i);
                return;
            }
            return;
        }
        if (str != null) {
            stringBuffer.append("//");
            if (str.startsWith("[")) {
                int iIndexOf = str.indexOf("]");
                if (iIndexOf != -1 && str.indexOf(":") != -1) {
                    if (iIndexOf == str.length()) {
                        strSubstring = "";
                    } else {
                        int i2 = iIndexOf + 1;
                        String strSubstring2 = str.substring(0, i2);
                        strSubstring = str.substring(i2);
                        str = strSubstring2;
                    }
                    stringBuffer.append(str);
                    stringBuffer.append(quote(strSubstring, L_REG_NAME | L_SERVER, H_REG_NAME | H_SERVER));
                    return;
                }
                return;
            }
            stringBuffer.append(quote(str, L_REG_NAME | L_SERVER, H_REG_NAME | H_SERVER));
        }
    }

    private static void appendFragment(StringBuffer stringBuffer, String str) {
        if (str != null) {
            stringBuffer.append('#');
            stringBuffer.append(quote(str, L_URIC, H_URIC));
        }
    }

    private static String quote(String str, long j, long j2) {
        str.length();
        boolean z = (L_ESCAPED & j) != 0;
        StringBuffer stringBuffer = null;
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt < 128) {
                if (!match(cCharAt, j, j2) && !isEscaped(str, i)) {
                    if (stringBuffer == null) {
                        stringBuffer = new StringBuffer();
                        stringBuffer.append(str.substring(0, i));
                    }
                    appendEscape(stringBuffer, (byte) cCharAt);
                } else if (stringBuffer != null) {
                    stringBuffer.append(cCharAt);
                }
            } else if (z && (Character.isSpaceChar(cCharAt) || Character.isISOControl(cCharAt))) {
                if (stringBuffer == null) {
                    stringBuffer = new StringBuffer();
                    stringBuffer.append(str.substring(0, i));
                }
                appendEncoded(stringBuffer, cCharAt);
            } else if (stringBuffer != null) {
                stringBuffer.append(cCharAt);
            }
        }
        return stringBuffer == null ? str : stringBuffer.toString();
    }

    private static boolean isEscaped(String str, int i) {
        int i2;
        if (str != null && str.length() > (i2 = i + 2) && str.charAt(i) == '%' && match(str.charAt(i + 1), L_HEX, H_HEX) && match(str.charAt(i2), L_HEX, H_HEX)) {
            return true;
        }
        return $assertionsDisabled;
    }

    private static void appendEncoded(StringBuffer stringBuffer, char c) {
        ByteBuffer byteBufferEncode;
        try {
            byteBufferEncode = ThreadLocalCoders.encoderFor("UTF-8").encode(CharBuffer.wrap("" + c));
        } catch (CharacterCodingException e) {
            byteBufferEncode = null;
        }
        while (byteBufferEncode.hasRemaining()) {
            int i = byteBufferEncode.get() & Character.DIRECTIONALITY_UNDEFINED;
            if (i >= 128) {
                appendEscape(stringBuffer, (byte) i);
            } else {
                stringBuffer.append((char) i);
            }
        }
    }

    private static void appendEscape(StringBuffer stringBuffer, byte b) {
        stringBuffer.append('%');
        stringBuffer.append(hexDigits[(b >> 4) & 15]);
        stringBuffer.append(hexDigits[(b >> 0) & 15]);
    }

    private static boolean match(char c, long j, long j2) {
        if (c < '@') {
            if (((L_ESCAPED << c) & j) != 0) {
                return true;
            }
            return $assertionsDisabled;
        }
        if (c >= 128 || ((L_ESCAPED << (c - 64)) & j2) == 0) {
            return $assertionsDisabled;
        }
        return true;
    }

    private static void checkPath(String str, String str2, String str3) throws URISyntaxException {
        if (str2 != null && str3 != null && str3.length() > 0 && str3.charAt(0) != '/') {
            throw new URISyntaxException(str, "Relative path in absolute URI");
        }
    }

    private static long lowMask(char c, char c2) {
        int iMax = Math.max(Math.min((int) c2, 63), 0);
        long j = 0;
        for (int iMax2 = Math.max(Math.min((int) c, 63), 0); iMax2 <= iMax; iMax2++) {
            j |= L_ESCAPED << iMax2;
        }
        return j;
    }

    private static long lowMask(String str) {
        int length = str.length();
        long j = 0;
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt < '@') {
                j |= L_ESCAPED << cCharAt;
            }
        }
        return j;
    }

    private static long highMask(char c, char c2) {
        int iMax = Math.max(Math.min((int) c2, 127), 64) - 64;
        long j = 0;
        for (int iMax2 = Math.max(Math.min((int) c, 127), 64) - 64; iMax2 <= iMax; iMax2++) {
            j |= L_ESCAPED << iMax2;
        }
        return j;
    }

    private static long highMask(String str) {
        int length = str.length();
        long j = 0;
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt >= '@' && cCharAt < 128) {
                j |= L_ESCAPED << (cCharAt - '@');
            }
        }
        return j;
    }
}
