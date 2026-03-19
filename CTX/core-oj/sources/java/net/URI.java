package java.net;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.text.Normalizer;
import sun.nio.cs.ThreadLocalCoders;
import sun.util.locale.BaseLocale;
import sun.util.locale.LanguageTag;

public final class URI implements Comparable<URI>, Serializable {
    static final boolean $assertionsDisabled = false;
    private static final long H_DIGIT = 0;
    private static final long H_ESCAPED = 0;
    private static final long L_ALPHA = 0;
    private static final long L_LOWALPHA = 0;
    private static final long L_UPALPHA = 0;
    static final long serialVersionUID = -6052424284110960213L;
    private transient String authority;
    private volatile transient String decodedAuthority;
    private volatile transient String decodedFragment;
    private volatile transient String decodedPath;
    private volatile transient String decodedQuery;
    private volatile transient String decodedSchemeSpecificPart;
    private volatile transient String decodedUserInfo;
    private transient String fragment;
    private volatile transient int hash;
    private transient String host;
    private transient String path;
    private transient int port;
    private transient String query;
    private transient String scheme;
    private volatile transient String schemeSpecificPart;
    private volatile String string;
    private transient String userInfo;
    private static final long L_DIGIT = lowMask('0', '9');
    private static final long H_UPALPHA = highMask('A', 'Z');
    private static final long H_LOWALPHA = highMask('a', 'z');
    private static final long H_ALPHA = H_LOWALPHA | H_UPALPHA;
    private static final long L_ALPHANUM = L_DIGIT | 0;
    private static final long H_ALPHANUM = H_ALPHA | 0;
    private static final long L_HEX = L_DIGIT;
    private static final long H_HEX = highMask('A', 'F') | highMask('a', 'f');
    private static final long L_MARK = lowMask("-_.!~*'()");
    private static final long H_MARK = highMask("-_.!~*'()");
    private static final long L_UNRESERVED = L_ALPHANUM | L_MARK;
    private static final long H_UNRESERVED = H_ALPHANUM | H_MARK;
    private static final long L_RESERVED = lowMask(";/?:@&=+$,[]");
    private static final long H_RESERVED = highMask(";/?:@&=+$,[]");
    private static final long L_ESCAPED = 1;
    private static final long L_URIC = (L_RESERVED | L_UNRESERVED) | L_ESCAPED;
    private static final long H_URIC = (H_RESERVED | H_UNRESERVED) | 0;
    private static final long L_PCHAR = (L_UNRESERVED | L_ESCAPED) | lowMask(":@&=+$,");
    private static final long H_PCHAR = (H_UNRESERVED | 0) | highMask(":@&=+$,");
    private static final long L_PATH = L_PCHAR | lowMask(";/");
    private static final long H_PATH = H_PCHAR | highMask(";/");
    private static final long L_DASH = lowMask(LanguageTag.SEP);
    private static final long H_DASH = highMask(LanguageTag.SEP);
    private static final long L_UNDERSCORE = lowMask(BaseLocale.SEP);
    private static final long H_UNDERSCORE = highMask(BaseLocale.SEP);
    private static final long L_DOT = lowMask(".");
    private static final long H_DOT = highMask(".");
    private static final long L_USERINFO = (L_UNRESERVED | L_ESCAPED) | lowMask(";:&=+$,");
    private static final long H_USERINFO = (H_UNRESERVED | 0) | highMask(";:&=+$,");
    private static final long L_REG_NAME = (L_UNRESERVED | L_ESCAPED) | lowMask("$,;:@&=+");
    private static final long H_REG_NAME = (H_UNRESERVED | 0) | highMask("$,;:@&=+");
    private static final long L_SERVER = ((L_USERINFO | L_ALPHANUM) | L_DASH) | lowMask(".:@[]");
    private static final long H_SERVER = ((H_USERINFO | H_ALPHANUM) | H_DASH) | highMask(".:@[]");
    private static final long L_SERVER_PERCENT = L_SERVER | lowMask("%");
    private static final long H_SERVER_PERCENT = H_SERVER | highMask("%");
    private static final long L_LEFT_BRACKET = lowMask("[");
    private static final long H_LEFT_BRACKET = highMask("[");
    private static final long L_SCHEME = (L_DIGIT | 0) | lowMask("+-.");
    private static final long H_SCHEME = (H_ALPHA | 0) | highMask("+-.");
    private static final long L_URIC_NO_SLASH = (L_UNRESERVED | L_ESCAPED) | lowMask(";?:@&=+$,");
    private static final long H_URIC_NO_SLASH = (H_UNRESERVED | 0) | highMask(";?:@&=+$,");
    private static final char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private URI() {
        this.port = -1;
        this.decodedUserInfo = null;
        this.decodedAuthority = null;
        this.decodedPath = null;
        this.decodedQuery = null;
        this.decodedFragment = null;
        this.decodedSchemeSpecificPart = null;
    }

    public URI(String str) throws URISyntaxException {
        this.port = -1;
        this.decodedUserInfo = null;
        this.decodedAuthority = null;
        this.decodedPath = null;
        this.decodedQuery = null;
        this.decodedFragment = null;
        this.decodedSchemeSpecificPart = null;
        new Parser(str).parse($assertionsDisabled);
    }

    public URI(String str, String str2, String str3, int i, String str4, String str5, String str6) throws URISyntaxException {
        this.port = -1;
        this.decodedUserInfo = null;
        this.decodedAuthority = null;
        this.decodedPath = null;
        this.decodedQuery = null;
        this.decodedFragment = null;
        this.decodedSchemeSpecificPart = null;
        String string = toString(str, null, null, str2, str3, i, str4, str5, str6);
        checkPath(string, str, str4);
        new Parser(string).parse(true);
    }

    public URI(String str, String str2, String str3, String str4, String str5) throws URISyntaxException {
        this.port = -1;
        this.decodedUserInfo = null;
        this.decodedAuthority = null;
        this.decodedPath = null;
        this.decodedQuery = null;
        this.decodedFragment = null;
        this.decodedSchemeSpecificPart = null;
        String string = toString(str, null, str2, null, null, -1, str3, str4, str5);
        checkPath(string, str, str3);
        new Parser(string).parse($assertionsDisabled);
    }

    public URI(String str, String str2, String str3, String str4) throws URISyntaxException {
        this(str, null, str2, -1, str3, null, str4);
    }

    public URI(String str, String str2, String str3) throws URISyntaxException {
        this.port = -1;
        this.decodedUserInfo = null;
        this.decodedAuthority = null;
        this.decodedPath = null;
        this.decodedQuery = null;
        this.decodedFragment = null;
        this.decodedSchemeSpecificPart = null;
        new Parser(toString(str, str2, null, null, null, -1, null, null, str3)).parse($assertionsDisabled);
    }

    public static URI create(String str) {
        try {
            return new URI(str);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public URI parseServerAuthority() throws URISyntaxException {
        if (this.host != null || this.authority == null) {
            return this;
        }
        defineString();
        new Parser(this.string).parse(true);
        return this;
    }

    public URI normalize() {
        return normalize(this);
    }

    public URI resolve(URI uri) {
        return resolve(this, uri);
    }

    public URI resolve(String str) {
        return resolve(create(str));
    }

    public URI relativize(URI uri) {
        return relativize(this, uri);
    }

    public URL toURL() throws MalformedURLException {
        if (!isAbsolute()) {
            throw new IllegalArgumentException("URI is not absolute");
        }
        return new URL(toString());
    }

    public String getScheme() {
        return this.scheme;
    }

    public boolean isAbsolute() {
        if (this.scheme != null) {
            return true;
        }
        return $assertionsDisabled;
    }

    public boolean isOpaque() {
        if (this.path == null) {
            return true;
        }
        return $assertionsDisabled;
    }

    public String getRawSchemeSpecificPart() {
        defineSchemeSpecificPart();
        return this.schemeSpecificPart;
    }

    public String getSchemeSpecificPart() {
        if (this.decodedSchemeSpecificPart == null) {
            this.decodedSchemeSpecificPart = decode(getRawSchemeSpecificPart());
        }
        return this.decodedSchemeSpecificPart;
    }

    public String getRawAuthority() {
        return this.authority;
    }

    public String getAuthority() {
        if (this.decodedAuthority == null) {
            this.decodedAuthority = decode(this.authority);
        }
        return this.decodedAuthority;
    }

    public String getRawUserInfo() {
        return this.userInfo;
    }

    public String getUserInfo() {
        if (this.decodedUserInfo == null && this.userInfo != null) {
            this.decodedUserInfo = decode(this.userInfo);
        }
        return this.decodedUserInfo;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getRawPath() {
        return this.path;
    }

    public String getPath() {
        if (this.decodedPath == null && this.path != null) {
            this.decodedPath = decode(this.path);
        }
        return this.decodedPath;
    }

    public String getRawQuery() {
        return this.query;
    }

    public String getQuery() {
        if (this.decodedQuery == null && this.query != null) {
            this.decodedQuery = decode(this.query);
        }
        return this.decodedQuery;
    }

    public String getRawFragment() {
        return this.fragment;
    }

    public String getFragment() {
        if (this.decodedFragment == null && this.fragment != null) {
            this.decodedFragment = decode(this.fragment);
        }
        return this.decodedFragment;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof URI)) {
            return $assertionsDisabled;
        }
        URI uri = (URI) obj;
        if (isOpaque() != uri.isOpaque() || !equalIgnoringCase(this.scheme, uri.scheme) || !equal(this.fragment, uri.fragment)) {
            return $assertionsDisabled;
        }
        if (isOpaque()) {
            return equal(this.schemeSpecificPart, uri.schemeSpecificPart);
        }
        if (!equal(this.path, uri.path) || !equal(this.query, uri.query)) {
            return $assertionsDisabled;
        }
        if (this.authority == uri.authority) {
            return true;
        }
        if (this.host != null) {
            if (!equal(this.userInfo, uri.userInfo) || !equalIgnoringCase(this.host, uri.host) || this.port != uri.port) {
                return $assertionsDisabled;
            }
        } else if (this.authority != null) {
            if (!equal(this.authority, uri.authority)) {
                return $assertionsDisabled;
            }
        } else if (this.authority != uri.authority) {
            return $assertionsDisabled;
        }
        return true;
    }

    public int hashCode() {
        int iHash;
        if (this.hash != 0) {
            return this.hash;
        }
        int iHash2 = hash(hashIgnoringCase(0, this.scheme), this.fragment);
        if (isOpaque()) {
            iHash = hash(iHash2, this.schemeSpecificPart);
        } else {
            int iHash3 = hash(hash(iHash2, this.path), this.query);
            if (this.host != null) {
                iHash = hashIgnoringCase(hash(iHash3, this.userInfo), this.host) + (1949 * this.port);
            } else {
                iHash = hash(iHash3, this.authority);
            }
        }
        this.hash = iHash;
        return iHash;
    }

    @Override
    public int compareTo(URI uri) {
        int iCompareIgnoringCase = compareIgnoringCase(this.scheme, uri.scheme);
        if (iCompareIgnoringCase != 0) {
            return iCompareIgnoringCase;
        }
        if (isOpaque()) {
            if (uri.isOpaque()) {
                int iCompare = compare(this.schemeSpecificPart, uri.schemeSpecificPart);
                if (iCompare != 0) {
                    return iCompare;
                }
                return compare(this.fragment, uri.fragment);
            }
            return 1;
        }
        if (uri.isOpaque()) {
            return -1;
        }
        if (this.host != null && uri.host != null) {
            int iCompare2 = compare(this.userInfo, uri.userInfo);
            if (iCompare2 != 0) {
                return iCompare2;
            }
            int iCompareIgnoringCase2 = compareIgnoringCase(this.host, uri.host);
            if (iCompareIgnoringCase2 != 0) {
                return iCompareIgnoringCase2;
            }
            int i = this.port - uri.port;
            if (i != 0) {
                return i;
            }
        } else {
            int iCompare3 = compare(this.authority, uri.authority);
            if (iCompare3 != 0) {
                return iCompare3;
            }
        }
        int iCompare4 = compare(this.path, uri.path);
        if (iCompare4 != 0) {
            return iCompare4;
        }
        int iCompare5 = compare(this.query, uri.query);
        return iCompare5 != 0 ? iCompare5 : compare(this.fragment, uri.fragment);
    }

    public String toString() {
        defineString();
        return this.string;
    }

    public String toASCIIString() {
        defineString();
        return encode(this.string);
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        defineString();
        objectOutputStream.defaultWriteObject();
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        this.port = -1;
        objectInputStream.defaultReadObject();
        try {
            new Parser(this.string).parse($assertionsDisabled);
        } catch (URISyntaxException e) {
            InvalidObjectException invalidObjectException = new InvalidObjectException("Invalid URI");
            invalidObjectException.initCause(e);
            throw invalidObjectException;
        }
    }

    private static int toLower(char c) {
        if (c >= 'A' && c <= 'Z') {
            return c + ' ';
        }
        return c;
    }

    private static int toUpper(char c) {
        if (c >= 'a' && c <= 'z') {
            return c - ' ';
        }
        return c;
    }

    private static boolean equal(String str, String str2) {
        if (str == str2) {
            return true;
        }
        if (str == null || str2 == null || str.length() != str2.length()) {
            return $assertionsDisabled;
        }
        if (str.indexOf(37) < 0) {
            return str.equals(str2);
        }
        int length = str.length();
        int i = 0;
        while (i < length) {
            char cCharAt = str.charAt(i);
            char cCharAt2 = str2.charAt(i);
            if (cCharAt != '%') {
                if (cCharAt != cCharAt2) {
                    return $assertionsDisabled;
                }
                i++;
            } else {
                if (cCharAt2 != '%') {
                    return $assertionsDisabled;
                }
                int i2 = i + 1;
                if (toLower(str.charAt(i2)) != toLower(str2.charAt(i2))) {
                    return $assertionsDisabled;
                }
                int i3 = i2 + 1;
                if (toLower(str.charAt(i3)) != toLower(str2.charAt(i3))) {
                    return $assertionsDisabled;
                }
                i = i3 + 1;
            }
        }
        return true;
    }

    private static boolean equalIgnoringCase(String str, String str2) {
        int length;
        if (str == str2) {
            return true;
        }
        if (str == null || str2 == null || str2.length() != (length = str.length())) {
            return $assertionsDisabled;
        }
        for (int i = 0; i < length; i++) {
            if (toLower(str.charAt(i)) != toLower(str2.charAt(i))) {
                return $assertionsDisabled;
            }
        }
        return true;
    }

    private static int hash(int i, String str) {
        return str == null ? i : str.indexOf(37) < 0 ? (i * 127) + str.hashCode() : normalizedHash(i, str);
    }

    private static int normalizedHash(int i, String str) {
        int i2 = 0;
        int upper = 0;
        while (i2 < str.length()) {
            char cCharAt = str.charAt(i2);
            upper = (upper * 31) + cCharAt;
            if (cCharAt == '%') {
                for (int i3 = i2 + 1; i3 < i2 + 3; i3++) {
                    upper = (upper * 31) + toUpper(str.charAt(i3));
                }
                i2 += 2;
            }
            i2++;
        }
        return (i * 127) + upper;
    }

    private static int hashIgnoringCase(int i, String str) {
        if (str == null) {
            return i;
        }
        int length = str.length();
        for (int i2 = 0; i2 < length; i2++) {
            i = toLower(str.charAt(i2)) + (31 * i);
        }
        return i;
    }

    private static int compare(String str, String str2) {
        if (str == str2) {
            return 0;
        }
        if (str != null) {
            if (str2 != null) {
                return str.compareTo(str2);
            }
            return 1;
        }
        return -1;
    }

    private static int compareIgnoringCase(String str, String str2) {
        if (str == str2) {
            return 0;
        }
        if (str != null) {
            if (str2 != null) {
                int length = str.length();
                int length2 = str2.length();
                int i = length < length2 ? length : length2;
                for (int i2 = 0; i2 < i; i2++) {
                    int lower = toLower(str.charAt(i2)) - toLower(str2.charAt(i2));
                    if (lower != 0) {
                        return lower;
                    }
                }
                return length - length2;
            }
            return 1;
        }
        return -1;
    }

    private static void checkPath(String str, String str2, String str3) throws URISyntaxException {
        if (str2 != null && str3 != null && str3.length() > 0 && str3.charAt(0) != '/') {
            throw new URISyntaxException(str, "Relative path in absolute URI");
        }
    }

    private void appendAuthority(StringBuffer stringBuffer, String str, String str2, String str3, int i) {
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
                String strSubstring = "";
                if (iIndexOf != -1 && str.indexOf(":") != -1) {
                    if (iIndexOf != str.length()) {
                        int i2 = iIndexOf + 1;
                        strSubstring = str.substring(0, i2);
                        str = str.substring(i2);
                    } else {
                        strSubstring = str;
                        str = "";
                    }
                }
                stringBuffer.append(strSubstring);
                stringBuffer.append(quote(str, L_REG_NAME | L_SERVER, H_REG_NAME | H_SERVER));
                return;
            }
            stringBuffer.append(quote(str, L_REG_NAME | L_SERVER, H_REG_NAME | H_SERVER));
        }
    }

    private void appendSchemeSpecificPart(StringBuffer stringBuffer, String str, String str2, String str3, String str4, int i, String str5, String str6) {
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

    private void appendFragment(StringBuffer stringBuffer, String str) {
        if (str != null) {
            stringBuffer.append('#');
            stringBuffer.append(quote(str, L_URIC, H_URIC));
        }
    }

    private String toString(String str, String str2, String str3, String str4, String str5, int i, String str6, String str7, String str8) {
        StringBuffer stringBuffer = new StringBuffer();
        if (str != null) {
            stringBuffer.append(str);
            stringBuffer.append(':');
        }
        appendSchemeSpecificPart(stringBuffer, str2, str3, str4, str5, i, str6, str7);
        appendFragment(stringBuffer, str8);
        return stringBuffer.toString();
    }

    private void defineSchemeSpecificPart() {
        if (this.schemeSpecificPart != null) {
            return;
        }
        StringBuffer stringBuffer = new StringBuffer();
        appendSchemeSpecificPart(stringBuffer, null, getAuthority(), getUserInfo(), this.host, this.port, getPath(), getQuery());
        if (stringBuffer.length() == 0) {
            return;
        }
        this.schemeSpecificPart = stringBuffer.toString();
    }

    private void defineString() {
        if (this.string != null) {
            return;
        }
        StringBuffer stringBuffer = new StringBuffer();
        if (this.scheme != null) {
            stringBuffer.append(this.scheme);
            stringBuffer.append(':');
        }
        if (isOpaque()) {
            stringBuffer.append(this.schemeSpecificPart);
        } else {
            if (this.host != null) {
                stringBuffer.append("//");
                if (this.userInfo != null) {
                    stringBuffer.append(this.userInfo);
                    stringBuffer.append('@');
                }
                boolean z = (this.host.indexOf(58) < 0 || this.host.startsWith("[") || this.host.endsWith("]")) ? $assertionsDisabled : true;
                if (z) {
                    stringBuffer.append('[');
                }
                stringBuffer.append(this.host);
                if (z) {
                    stringBuffer.append(']');
                }
                if (this.port != -1) {
                    stringBuffer.append(':');
                    stringBuffer.append(this.port);
                }
            } else if (this.authority != null) {
                stringBuffer.append("//");
                stringBuffer.append(this.authority);
            }
            if (this.path != null) {
                stringBuffer.append(this.path);
            }
            if (this.query != null) {
                stringBuffer.append('?');
                stringBuffer.append(this.query);
            }
        }
        if (this.fragment != null) {
            stringBuffer.append('#');
            stringBuffer.append(this.fragment);
        }
        this.string = stringBuffer.toString();
    }

    private static String resolvePath(String str, String str2, boolean z) {
        int iLastIndexOf = str.lastIndexOf(47);
        int length = str2.length();
        String string = "";
        if (length == 0) {
            if (iLastIndexOf >= 0) {
                string = str.substring(0, iLastIndexOf + 1);
            }
        } else {
            StringBuffer stringBuffer = new StringBuffer(str.length() + length);
            if (iLastIndexOf >= 0) {
                stringBuffer.append(str.substring(0, iLastIndexOf + 1));
            }
            stringBuffer.append(str2);
            string = stringBuffer.toString();
        }
        return normalize(string, true);
    }

    private static URI resolve(URI uri, URI uri2) {
        if (uri2.isOpaque() || uri.isOpaque()) {
            return uri2;
        }
        if (uri2.scheme == null && uri2.authority == null && uri2.path.equals("") && uri2.fragment != null && uri2.query == null) {
            if (uri.fragment != null && uri2.fragment.equals(uri.fragment)) {
                return uri;
            }
            URI uri3 = new URI();
            uri3.scheme = uri.scheme;
            uri3.authority = uri.authority;
            uri3.userInfo = uri.userInfo;
            uri3.host = uri.host;
            uri3.port = uri.port;
            uri3.path = uri.path;
            uri3.fragment = uri2.fragment;
            uri3.query = uri.query;
            return uri3;
        }
        if (uri2.scheme != null) {
            return uri2;
        }
        URI uri4 = new URI();
        uri4.scheme = uri.scheme;
        uri4.query = uri2.query;
        uri4.fragment = uri2.fragment;
        if (uri2.authority == null) {
            uri4.authority = uri.authority;
            uri4.host = uri.host;
            uri4.userInfo = uri.userInfo;
            uri4.port = uri.port;
            if (uri2.path == null || uri2.path.isEmpty()) {
                uri4.path = uri.path;
                uri4.query = uri2.query != null ? uri2.query : uri.query;
            } else if (uri2.path.length() > 0 && uri2.path.charAt(0) == '/') {
                uri4.path = normalize(uri2.path, true);
            } else {
                uri4.path = resolvePath(uri.path, uri2.path, uri.isAbsolute());
            }
        } else {
            uri4.authority = uri2.authority;
            uri4.host = uri2.host;
            uri4.userInfo = uri2.userInfo;
            uri4.host = uri2.host;
            uri4.port = uri2.port;
            uri4.path = uri2.path;
        }
        return uri4;
    }

    private static URI normalize(URI uri) {
        String strNormalize;
        if (uri.isOpaque() || uri.path == null || uri.path.length() == 0 || (strNormalize = normalize(uri.path)) == uri.path) {
            return uri;
        }
        URI uri2 = new URI();
        uri2.scheme = uri.scheme;
        uri2.fragment = uri.fragment;
        uri2.authority = uri.authority;
        uri2.userInfo = uri.userInfo;
        uri2.host = uri.host;
        uri2.port = uri.port;
        uri2.path = strNormalize;
        uri2.query = uri.query;
        return uri2;
    }

    private static URI relativize(URI uri, URI uri2) {
        if (uri2.isOpaque() || uri.isOpaque() || !equalIgnoringCase(uri.scheme, uri2.scheme) || !equal(uri.authority, uri2.authority)) {
            return uri2;
        }
        String strNormalize = normalize(uri.path);
        String strNormalize2 = normalize(uri2.path);
        if (!strNormalize.equals(strNormalize2)) {
            if (strNormalize.indexOf(47) != -1) {
                strNormalize = strNormalize.substring(0, strNormalize.lastIndexOf(47) + 1);
            }
            if (!strNormalize2.startsWith(strNormalize)) {
                return uri2;
            }
        }
        URI uri3 = new URI();
        uri3.path = strNormalize2.substring(strNormalize.length());
        uri3.query = uri2.query;
        uri3.fragment = uri2.fragment;
        return uri3;
    }

    private static int needsNormalization(String str) {
        int i;
        boolean z = true;
        int length = str.length() - 1;
        int i2 = 0;
        while (i2 <= length && str.charAt(i2) == '/') {
            i2++;
        }
        if (i2 <= 1) {
            i = 0;
        } else {
            z = false;
            i = 0;
        }
        while (i2 <= length) {
            if (str.charAt(i2) == '.') {
                if (i2 != length) {
                    int i3 = i2 + 1;
                    if (str.charAt(i3) == '/' || (str.charAt(i3) == '.' && (i3 == length || str.charAt(i2 + 2) == '/'))) {
                        z = false;
                    }
                }
            }
            i++;
            while (true) {
                if (i2 <= length) {
                    int i4 = i2 + 1;
                    if (str.charAt(i2) != '/') {
                        i2 = i4;
                    } else {
                        i2 = i4;
                        while (i2 <= length && str.charAt(i2) == '/') {
                            i2++;
                            z = false;
                        }
                    }
                }
            }
        }
        if (z) {
            return -1;
        }
        return i;
    }

    private static void split(char[] cArr, int[] iArr) {
        int length = cArr.length - 1;
        int i = 0;
        while (i <= length && cArr[i] == '/') {
            cArr[i] = 0;
            i++;
        }
        int i2 = 0;
        while (i <= length) {
            int i3 = i2 + 1;
            iArr[i2] = i;
            i++;
            while (true) {
                if (i <= length) {
                    int i4 = i + 1;
                    if (cArr[i] == '/') {
                        cArr[i4 - 1] = 0;
                        while (true) {
                            i = i4;
                            if (i > length || cArr[i] != '/') {
                                break;
                            }
                            i4 = i + 1;
                            cArr[i] = 0;
                        }
                    } else {
                        i = i4;
                    }
                }
            }
            i2 = i3;
        }
        if (i2 != iArr.length) {
            throw new InternalError();
        }
    }

    private static int join(char[] cArr, int[] iArr) {
        int i;
        int i2 = 1;
        int length = cArr.length - 1;
        if (cArr[0] == 0) {
            cArr[0] = '/';
        } else {
            i2 = 0;
        }
        for (int i3 : iArr) {
            if (i3 != -1) {
                if (i2 == i3) {
                    while (i2 <= length && cArr[i2] != 0) {
                        i2++;
                    }
                    if (i2 <= length) {
                        i = i2 + 1;
                        cArr[i2] = '/';
                        i2 = i;
                    }
                } else if (i2 < i3) {
                    while (i3 <= length && cArr[i3] != 0) {
                        cArr[i2] = cArr[i3];
                        i2++;
                        i3++;
                    }
                    if (i3 <= length) {
                        i = i2 + 1;
                        cArr[i2] = '/';
                        i2 = i;
                    }
                } else {
                    throw new InternalError();
                }
            }
        }
        return i2;
    }

    private static void removeDots(char[] cArr, int[] iArr, boolean z) {
        char c;
        int length = iArr.length;
        int length2 = cArr.length - 1;
        int i = 0;
        while (i < length) {
            do {
                int i2 = iArr[i];
                if (cArr[i2] == '.') {
                    if (i2 != length2) {
                        int i3 = i2 + 1;
                        if (cArr[i3] != 0) {
                            if (cArr[i3] == '.' && (i3 == length2 || cArr[i2 + 2] == 0)) {
                                c = 2;
                                break;
                            }
                            i++;
                        }
                    }
                    c = 1;
                    break;
                }
                i++;
            } while (i < length);
            c = 0;
            if (i <= length && c != 0) {
                if (c == 1) {
                    iArr[i] = -1;
                } else {
                    int i4 = i - 1;
                    while (i4 >= 0 && iArr[i4] == -1) {
                        i4--;
                    }
                    if (i4 >= 0) {
                        int i5 = iArr[i4];
                        if (cArr[i5] != '.' || cArr[i5 + 1] != '.' || cArr[i5 + 2] != 0) {
                            iArr[i] = -1;
                            iArr[i4] = -1;
                        }
                    } else if (z) {
                        iArr[i] = -1;
                    }
                }
                i++;
            } else {
                return;
            }
        }
    }

    private static void maybeAddLeadingDot(char[] cArr, int[] iArr) {
        if (cArr[0] == 0) {
            return;
        }
        int length = iArr.length;
        int i = 0;
        while (i < length && iArr[i] < 0) {
            i++;
        }
        if (i >= length || i == 0) {
            return;
        }
        int i2 = iArr[i];
        while (i2 < cArr.length && cArr[i2] != ':' && cArr[i2] != 0) {
            i2++;
        }
        if (i2 < cArr.length && cArr[i2] != 0) {
            cArr[0] = '.';
            cArr[1] = 0;
            iArr[0] = 0;
        }
    }

    private static String normalize(String str) {
        return normalize(str, $assertionsDisabled);
    }

    private static String normalize(String str, boolean z) {
        int iNeedsNormalization = needsNormalization(str);
        if (iNeedsNormalization < 0) {
            return str;
        }
        char[] charArray = str.toCharArray();
        int[] iArr = new int[iNeedsNormalization];
        split(charArray, iArr);
        removeDots(charArray, iArr, z);
        maybeAddLeadingDot(charArray, iArr);
        String str2 = new String(charArray, 0, join(charArray, iArr));
        if (str2.equals(str)) {
            return str;
        }
        return str2;
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

    private static long lowMask(char c, char c2) {
        int iMax = Math.max(Math.min((int) c2, 63), 0);
        long j = 0;
        for (int iMax2 = Math.max(Math.min((int) c, 63), 0); iMax2 <= iMax; iMax2++) {
            j |= L_ESCAPED << iMax2;
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

    private static boolean match(char c, long j, long j2) {
        if (c == 0) {
            return $assertionsDisabled;
        }
        if (c < '@') {
            if (((L_ESCAPED << c) & j) == 0) {
                return $assertionsDisabled;
            }
            return true;
        }
        if (c >= 128 || ((L_ESCAPED << (c - 64)) & j2) == 0) {
            return $assertionsDisabled;
        }
        return true;
    }

    private static void appendEscape(StringBuffer stringBuffer, byte b) {
        stringBuffer.append('%');
        stringBuffer.append(hexDigits[(b >> 4) & 15]);
        stringBuffer.append(hexDigits[(b >> 0) & 15]);
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

    private static String quote(String str, long j, long j2) {
        str.length();
        boolean z = (L_ESCAPED & j) != 0;
        StringBuffer stringBuffer = null;
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt < 128) {
                if (!match(cCharAt, j, j2)) {
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

    private static String encode(String str) {
        ByteBuffer byteBufferEncode;
        int length = str.length();
        if (length == 0) {
            return str;
        }
        int i = 0;
        while (str.charAt(i) < 128) {
            i++;
            if (i >= length) {
                return str;
            }
        }
        try {
            byteBufferEncode = ThreadLocalCoders.encoderFor("UTF-8").encode(CharBuffer.wrap(Normalizer.normalize(str, Normalizer.Form.NFC)));
        } catch (CharacterCodingException e) {
            byteBufferEncode = null;
        }
        StringBuffer stringBuffer = new StringBuffer();
        while (byteBufferEncode.hasRemaining()) {
            int i2 = byteBufferEncode.get() & Character.DIRECTIONALITY_UNDEFINED;
            if (i2 >= 128) {
                appendEscape(stringBuffer, (byte) i2);
            } else {
                stringBuffer.append((char) i2);
            }
        }
        return stringBuffer.toString();
    }

    private static int decode(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return (c - 'a') + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return (c - 'A') + 10;
        }
        return -1;
    }

    private static byte decode(char c, char c2) {
        return (byte) (((decode(c) & 15) << 4) | ((decode(c2) & 15) << 0));
    }

    private static String decode(String str) {
        int length;
        if (str == null || (length = str.length()) == 0 || str.indexOf(37) < 0) {
            return str;
        }
        StringBuffer stringBuffer = new StringBuffer(length);
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(length);
        CharBuffer charBufferAllocate = CharBuffer.allocate(length);
        CharsetDecoder charsetDecoderOnUnmappableCharacter = ThreadLocalCoders.decoderFor("UTF-8").onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
        boolean z = false;
        char cCharAt = str.charAt(0);
        int i = 0;
        while (i < length) {
            if (cCharAt != '[') {
                if (z && cCharAt == ']') {
                    z = false;
                }
            } else {
                z = true;
            }
            if (cCharAt != '%' || z) {
                stringBuffer.append(cCharAt);
                i++;
                if (i >= length) {
                    break;
                }
                cCharAt = str.charAt(i);
            } else {
                byteBufferAllocate.clear();
                do {
                    int i2 = i + 1;
                    char cCharAt2 = str.charAt(i2);
                    int i3 = i2 + 1;
                    byteBufferAllocate.put(decode(cCharAt2, str.charAt(i3)));
                    i = i3 + 1;
                    if (i >= length) {
                        break;
                    }
                    cCharAt = str.charAt(i);
                } while (cCharAt == '%');
                byteBufferAllocate.flip();
                charBufferAllocate.clear();
                charsetDecoderOnUnmappableCharacter.reset();
                charsetDecoderOnUnmappableCharacter.decode(byteBufferAllocate, charBufferAllocate, true);
                charsetDecoderOnUnmappableCharacter.flush(charBufferAllocate);
                stringBuffer.append(charBufferAllocate.flip().toString());
            }
        }
        return stringBuffer.toString();
    }

    private class Parser {
        private String input;
        private boolean requireServerAuthority = URI.$assertionsDisabled;
        private int ipv6byteCount = 0;

        Parser(String str) {
            this.input = str;
            URI.this.string = str;
        }

        private void fail(String str) throws URISyntaxException {
            throw new URISyntaxException(this.input, str);
        }

        private void fail(String str, int i) throws URISyntaxException {
            throw new URISyntaxException(this.input, str, i);
        }

        private void failExpecting(String str, int i) throws URISyntaxException {
            fail("Expected " + str, i);
        }

        private void failExpecting(String str, String str2, int i) throws URISyntaxException {
            fail("Expected " + str + " following " + str2, i);
        }

        private String substring(int i, int i2) {
            return this.input.substring(i, i2);
        }

        private char charAt(int i) {
            return this.input.charAt(i);
        }

        private boolean at(int i, int i2, char c) {
            if (i >= i2 || charAt(i) != c) {
                return URI.$assertionsDisabled;
            }
            return true;
        }

        private boolean at(int i, int i2, String str) {
            int length = str.length();
            if (length > i2 - i) {
                return URI.$assertionsDisabled;
            }
            int i3 = i;
            int i4 = 0;
            while (i4 < length) {
                int i5 = i3 + 1;
                if (charAt(i3) != str.charAt(i4)) {
                    break;
                }
                i4++;
                i3 = i5;
            }
            if (i4 == length) {
                return true;
            }
            return URI.$assertionsDisabled;
        }

        private int scan(int i, int i2, char c) {
            if (i < i2 && charAt(i) == c) {
                return i + 1;
            }
            return i;
        }

        private int scan(int i, int i2, String str, String str2) {
            while (i < i2) {
                char cCharAt = charAt(i);
                if (str.indexOf(cCharAt) >= 0) {
                    return -1;
                }
                if (str2.indexOf(cCharAt) >= 0) {
                    break;
                }
                i++;
            }
            return i;
        }

        private int scanEscape(int i, int i2, char c) throws URISyntaxException {
            if (c == '%') {
                int i3 = i + 3;
                if (i3 <= i2 && URI.match(charAt(i + 1), URI.L_HEX, URI.H_HEX) && URI.match(charAt(i + 2), URI.L_HEX, URI.H_HEX)) {
                    return i3;
                }
                fail("Malformed escape pair", i);
            } else if (c > 128 && !Character.isSpaceChar(c) && !Character.isISOControl(c)) {
                return i + 1;
            }
            return i;
        }

        private int scan(int i, int i2, long j, long j2) throws URISyntaxException {
            int iScanEscape;
            while (i < i2) {
                char cCharAt = charAt(i);
                if (URI.match(cCharAt, j, j2)) {
                    i++;
                } else {
                    if ((URI.L_ESCAPED & j) == 0 || (iScanEscape = scanEscape(i, i2, cCharAt)) <= i) {
                        break;
                    }
                    i = iScanEscape;
                }
            }
            return i;
        }

        private void checkChars(int i, int i2, long j, long j2, String str) throws URISyntaxException {
            int iScan = scan(i, i2, j, j2);
            if (iScan < i2) {
                fail("Illegal character in " + str, iScan);
            }
        }

        private void checkChar(int i, long j, long j2, String str) throws URISyntaxException {
            checkChars(i, i + 1, j, j2, str);
        }

        void parse(boolean z) throws URISyntaxException {
            int hierarchical;
            int i;
            this.requireServerAuthority = z;
            int length = this.input.length();
            int iScan = scan(0, length, "/?#", ":");
            if (iScan >= 0 && at(iScan, length, ':')) {
                if (iScan == 0) {
                    failExpecting("scheme name", 0);
                }
                checkChar(0, 0L, URI.H_ALPHA, "scheme name");
                checkChars(1, iScan, URI.L_SCHEME, URI.H_SCHEME, "scheme name");
                URI.this.scheme = substring(0, iScan);
                i = iScan + 1;
                if (at(i, length, '/')) {
                    hierarchical = parseHierarchical(i, length);
                } else {
                    hierarchical = scan(i, length, "", "#");
                    if (hierarchical <= i) {
                        failExpecting("scheme-specific part", i);
                    }
                    checkChars(i, hierarchical, URI.L_URIC, URI.H_URIC, "opaque part");
                }
            } else {
                hierarchical = parseHierarchical(0, length);
                i = 0;
            }
            URI.this.schemeSpecificPart = substring(i, hierarchical);
            if (at(hierarchical, length, '#')) {
                int i2 = hierarchical + 1;
                checkChars(i2, length, URI.L_URIC, URI.H_URIC, "fragment");
                URI.this.fragment = substring(i2, length);
                hierarchical = length;
            }
            if (hierarchical < length) {
                fail("end of URI", hierarchical);
            }
        }

        private int parseHierarchical(int i, int i2) throws URISyntaxException {
            if (at(i, i2, '/') && at(i + 1, i2, '/')) {
                i += 2;
                int iScan = scan(i, i2, "", "/?#");
                if (iScan > i) {
                    i = parseAuthority(i, iScan);
                } else if (iScan >= i2) {
                    failExpecting("authority", i);
                }
            }
            int iScan2 = scan(i, i2, "", "?#");
            checkChars(i, iScan2, URI.L_PATH, URI.H_PATH, "path");
            URI.this.path = substring(i, iScan2);
            if (at(iScan2, i2, '?')) {
                int i3 = iScan2 + 1;
                int iScan3 = scan(i3, i2, "", "#");
                checkChars(i3, iScan3, URI.L_URIC, URI.H_URIC, "query");
                URI.this.query = substring(i3, iScan3);
                return iScan3;
            }
            return iScan2;
        }

        private int parseAuthority(int i, int i2) throws URISyntaxException {
            int server;
            int iScan = scan(i, i2, "", "]");
            boolean z = URI.$assertionsDisabled;
            boolean z2 = iScan <= i ? scan(i, i2, URI.L_SERVER, URI.H_SERVER) == i2 : scan(i, i2, URI.L_SERVER_PERCENT, URI.H_SERVER_PERCENT) == i2;
            if (scan(i, i2, URI.L_REG_NAME, URI.H_REG_NAME) == i2) {
                z = true;
            }
            if (z && !z2) {
                URI.this.authority = substring(i, i2);
                return i2;
            }
            URISyntaxException uRISyntaxException = null;
            if (z2) {
                try {
                    server = parseServer(i, i2);
                    if (server < i2) {
                        failExpecting("end of authority", server);
                    }
                    URI.this.authority = substring(i, i2);
                } catch (URISyntaxException e) {
                    URI.this.userInfo = null;
                    URI.this.host = null;
                    URI.this.port = -1;
                    if (this.requireServerAuthority) {
                        throw e;
                    }
                    uRISyntaxException = e;
                    server = i;
                }
            } else {
                server = i;
            }
            if (server < i2) {
                if (z) {
                    URI.this.authority = substring(i, i2);
                } else {
                    if (uRISyntaxException != null) {
                        throw uRISyntaxException;
                    }
                    fail("Illegal character in authority", server);
                }
            }
            return i2;
        }

        private int parseServer(int i, int i2) throws URISyntaxException {
            int hostname;
            int iScan;
            int iScan2 = scan(i, i2, "/?#", "@");
            if (iScan2 >= i && at(iScan2, i2, '@')) {
                checkChars(i, iScan2, URI.L_USERINFO, URI.H_USERINFO, "user info");
                URI.this.userInfo = substring(i, iScan2);
                i = iScan2 + 1;
            }
            if (at(i, i2, '[')) {
                hostname = i + 1;
                int iScan3 = scan(hostname, i2, "/?#", "]");
                if (iScan3 > hostname && at(iScan3, i2, ']')) {
                    int iScan4 = scan(hostname, iScan3, "", "%");
                    if (iScan4 > hostname) {
                        parseIPv6Reference(hostname, iScan4);
                        int i3 = iScan4 + 1;
                        if (i3 == iScan3) {
                            fail("scope id expected");
                        }
                        checkChars(i3, iScan3, URI.L_ALPHANUM, URI.H_ALPHANUM, "scope id");
                    } else {
                        parseIPv6Reference(hostname, iScan3);
                    }
                    int i4 = iScan3 + 1;
                    URI.this.host = substring(hostname - 1, i4);
                    hostname = i4;
                } else {
                    failExpecting("closing bracket for IPv6 address", iScan3);
                }
            } else {
                int iPv4Address = parseIPv4Address(i, i2);
                if (iPv4Address <= i) {
                    hostname = parseHostname(i, i2);
                } else {
                    hostname = iPv4Address;
                }
            }
            if (at(hostname, i2, ':') && (iScan = scan((hostname = hostname + 1), i2, "", "/")) > hostname) {
                checkChars(hostname, iScan, URI.L_DIGIT, 0L, "port number");
                try {
                    URI.this.port = Integer.parseInt(substring(hostname, iScan));
                } catch (NumberFormatException e) {
                    fail("Malformed port number", hostname);
                }
                hostname = iScan;
            }
            if (hostname < i2) {
                failExpecting("port number", hostname);
            }
            return hostname;
        }

        private int scanByte(int i, int i2) throws URISyntaxException {
            int iScan = scan(i, i2, URI.L_DIGIT, 0L);
            return (iScan > i && Integer.parseInt(substring(i, iScan)) > 255) ? i : iScan;
        }

        private int scanIPv4Address(int i, int i2, boolean z) throws URISyntaxException {
            int iScan = scan(i, i2, URI.L_DIGIT | URI.L_DOT, 0 | URI.H_DOT);
            if (iScan <= i || (z && iScan != i2)) {
                return -1;
            }
            int iScanByte = scanByte(i, iScan);
            if (iScanByte > i) {
                int iScan2 = scan(iScanByte, iScan, '.');
                if (iScan2 > iScanByte) {
                    iScanByte = scanByte(iScan2, iScan);
                    if (iScanByte > iScan2) {
                        iScan2 = scan(iScanByte, iScan, '.');
                        if (iScan2 > iScanByte) {
                            iScanByte = scanByte(iScan2, iScan);
                            if (iScanByte > iScan2) {
                                int iScan3 = scan(iScanByte, iScan, '.');
                                if (iScan3 > iScanByte) {
                                    iScanByte = scanByte(iScan3, iScan);
                                    if (iScanByte > iScan3 && iScanByte >= iScan) {
                                        return iScanByte;
                                    }
                                } else {
                                    iScanByte = iScan3;
                                }
                            }
                        } else {
                            iScanByte = iScan2;
                        }
                    }
                }
            }
            fail("Malformed IPv4 address", iScanByte);
            return -1;
        }

        private int takeIPv4Address(int i, int i2, String str) throws URISyntaxException {
            int iScanIPv4Address = scanIPv4Address(i, i2, true);
            if (iScanIPv4Address <= i) {
                failExpecting(str, i);
            }
            return iScanIPv4Address;
        }

        private int parseIPv4Address(int i, int i2) {
            try {
                int iScanIPv4Address = scanIPv4Address(i, i2, URI.$assertionsDisabled);
                if (iScanIPv4Address > i && iScanIPv4Address < i2 && charAt(iScanIPv4Address) != ':') {
                    iScanIPv4Address = -1;
                }
                if (iScanIPv4Address > i) {
                    URI.this.host = substring(i, iScanIPv4Address);
                }
                return iScanIPv4Address;
            } catch (NumberFormatException e) {
                return -1;
            } catch (URISyntaxException e2) {
                return -1;
            }
        }

        private int parseHostname(int i, int i2) throws URISyntaxException {
            int iScan;
            int i3 = -1;
            int i4 = i;
            while (true) {
                iScan = scan(i4, i2, URI.L_ALPHANUM, URI.H_ALPHANUM);
                if (iScan > i4) {
                    if (iScan > i4) {
                        int iScan2 = scan(iScan, i2, URI.L_ALPHANUM | URI.L_DASH | URI.L_UNDERSCORE, URI.H_ALPHANUM | URI.H_DASH | URI.H_UNDERSCORE);
                        if (iScan2 > iScan) {
                            int i5 = iScan2 - 1;
                            if (charAt(i5) == '-') {
                                fail("Illegal character in hostname", i5);
                            }
                            iScan = iScan2;
                        }
                    } else {
                        iScan = i4;
                    }
                    int iScan3 = scan(iScan, i2, '.');
                    if (iScan3 <= iScan) {
                        break;
                    }
                    if (iScan3 < i2) {
                        i3 = i4;
                        i4 = iScan3;
                    } else {
                        iScan = iScan3;
                        break;
                    }
                } else {
                    iScan = i4;
                    i4 = i3;
                    break;
                }
            }
            if (iScan < i2 && !at(iScan, i2, ':')) {
                fail("Illegal character in hostname", iScan);
            }
            if (i4 < 0) {
                failExpecting("hostname", i);
            }
            if (i4 > i && !URI.match(charAt(i4), 0L, URI.H_ALPHA)) {
                fail("Illegal character in hostname", i4);
            }
            URI.this.host = substring(i, iScan);
            return iScan;
        }

        private int parseIPv6Reference(int i, int i2) throws URISyntaxException {
            int iScanHexSeq = scanHexSeq(i, i2);
            boolean z = URI.$assertionsDisabled;
            if (iScanHexSeq > i) {
                if (at(iScanHexSeq, i2, "::")) {
                    iScanHexSeq = scanHexPost(iScanHexSeq + 2, i2);
                    z = true;
                } else if (at(iScanHexSeq, i2, ':')) {
                    iScanHexSeq = takeIPv4Address(iScanHexSeq + 1, i2, "IPv4 address");
                    this.ipv6byteCount += 4;
                }
            } else if (at(i, i2, "::")) {
                iScanHexSeq = scanHexPost(i + 2, i2);
                z = true;
            } else {
                iScanHexSeq = i;
            }
            if (iScanHexSeq < i2) {
                fail("Malformed IPv6 address", i);
            }
            if (this.ipv6byteCount > 16) {
                fail("IPv6 address too long", i);
            }
            if (!z && this.ipv6byteCount < 16) {
                fail("IPv6 address too short", i);
            }
            if (z && this.ipv6byteCount == 16) {
                fail("Malformed IPv6 address", i);
            }
            return iScanHexSeq;
        }

        private int scanHexPost(int i, int i2) throws URISyntaxException {
            if (i == i2) {
                return i;
            }
            int iScanHexSeq = scanHexSeq(i, i2);
            if (iScanHexSeq > i) {
                if (at(iScanHexSeq, i2, ':')) {
                    int iTakeIPv4Address = takeIPv4Address(iScanHexSeq + 1, i2, "hex digits or IPv4 address");
                    this.ipv6byteCount += 4;
                    return iTakeIPv4Address;
                }
                return iScanHexSeq;
            }
            int iTakeIPv4Address2 = takeIPv4Address(i, i2, "hex digits or IPv4 address");
            this.ipv6byteCount += 4;
            return iTakeIPv4Address2;
        }

        private int scanHexSeq(int i, int i2) throws URISyntaxException {
            int iScan = scan(i, i2, URI.L_HEX, URI.H_HEX);
            if (iScan <= i || at(iScan, i2, '.')) {
                return -1;
            }
            if (iScan > i + 4) {
                fail("IPv6 hexadecimal digit sequence too long", i);
            }
            this.ipv6byteCount += 2;
            while (iScan < i2 && at(iScan, i2, ':')) {
                int i3 = iScan + 1;
                if (!at(i3, i2, ':')) {
                    iScan = scan(i3, i2, URI.L_HEX, URI.H_HEX);
                    if (iScan <= i3) {
                        failExpecting("digits for an IPv6 address", i3);
                    }
                    if (at(iScan, i2, '.')) {
                        return i3 - 1;
                    }
                    if (iScan > i3 + 4) {
                        fail("IPv6 hexadecimal digit sequence too long", i3);
                    }
                    this.ipv6byteCount += 2;
                } else {
                    return iScan;
                }
            }
            return iScan;
        }
    }
}
