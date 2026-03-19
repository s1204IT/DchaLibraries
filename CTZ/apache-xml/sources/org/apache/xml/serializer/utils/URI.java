package org.apache.xml.serializer.utils;

import java.io.IOException;
import org.apache.xalan.templates.Constants;
import org.apache.xpath.compiler.PsuedoNames;

final class URI {
    private static boolean DEBUG = false;
    private static final String MARK_CHARACTERS = "-_.!~*'() ";
    private static final String RESERVED_CHARACTERS = ";/?:@&=+$,";
    private static final String SCHEME_CHARACTERS = "+-.";
    private static final String USERINFO_CHARACTERS = ";:&=+$,";
    private String m_fragment;
    private String m_host;
    private String m_path;
    private int m_port;
    private String m_queryString;
    private String m_scheme;
    private String m_userinfo;

    public static class MalformedURIException extends IOException {
        public MalformedURIException() {
        }

        public MalformedURIException(String str) {
            super(str);
        }
    }

    public URI() {
        this.m_scheme = null;
        this.m_userinfo = null;
        this.m_host = null;
        this.m_port = -1;
        this.m_path = null;
        this.m_queryString = null;
        this.m_fragment = null;
    }

    public URI(URI uri) {
        this.m_scheme = null;
        this.m_userinfo = null;
        this.m_host = null;
        this.m_port = -1;
        this.m_path = null;
        this.m_queryString = null;
        this.m_fragment = null;
        initialize(uri);
    }

    public URI(String str) throws MalformedURIException {
        this((URI) null, str);
    }

    public URI(URI uri, String str) throws MalformedURIException {
        this.m_scheme = null;
        this.m_userinfo = null;
        this.m_host = null;
        this.m_port = -1;
        this.m_path = null;
        this.m_queryString = null;
        this.m_fragment = null;
        initialize(uri, str);
    }

    public URI(String str, String str2) throws MalformedURIException {
        this.m_scheme = null;
        this.m_userinfo = null;
        this.m_host = null;
        this.m_port = -1;
        this.m_path = null;
        this.m_queryString = null;
        this.m_fragment = null;
        if (str == null || str.trim().length() == 0) {
            throw new MalformedURIException("Cannot construct URI with null/empty scheme!");
        }
        if (str2 == null || str2.trim().length() == 0) {
            throw new MalformedURIException("Cannot construct URI with null/empty scheme-specific part!");
        }
        setScheme(str);
        setPath(str2);
    }

    public URI(String str, String str2, String str3, String str4, String str5) throws MalformedURIException {
        this(str, null, str2, -1, str3, str4, str5);
    }

    public URI(String str, String str2, String str3, int i, String str4, String str5, String str6) throws MalformedURIException {
        this.m_scheme = null;
        this.m_userinfo = null;
        this.m_host = null;
        this.m_port = -1;
        this.m_path = null;
        this.m_queryString = null;
        this.m_fragment = null;
        if (str == null || str.trim().length() == 0) {
            throw new MalformedURIException(Utils.messages.createMessage("ER_SCHEME_REQUIRED", null));
        }
        if (str3 == null) {
            if (str2 != null) {
                throw new MalformedURIException(Utils.messages.createMessage("ER_NO_USERINFO_IF_NO_HOST", null));
            }
            if (i != -1) {
                throw new MalformedURIException(Utils.messages.createMessage("ER_NO_PORT_IF_NO_HOST", null));
            }
        }
        if (str4 != null) {
            if (str4.indexOf(63) == -1 || str5 == null) {
                if (str4.indexOf(35) != -1 && str6 != null) {
                    throw new MalformedURIException(Utils.messages.createMessage("ER_NO_FRAGMENT_STRING_IN_PATH", null));
                }
            } else {
                throw new MalformedURIException(Utils.messages.createMessage("ER_NO_QUERY_STRING_IN_PATH", null));
            }
        }
        setScheme(str);
        setHost(str3);
        setPort(i);
        setUserinfo(str2);
        setPath(str4);
        setQueryString(str5);
        setFragment(str6);
    }

    private void initialize(URI uri) {
        this.m_scheme = uri.getScheme();
        this.m_userinfo = uri.getUserinfo();
        this.m_host = uri.getHost();
        this.m_port = uri.getPort();
        this.m_path = uri.getPath();
        this.m_queryString = uri.getQueryString();
        this.m_fragment = uri.getFragment();
    }

    private void initialize(URI uri, String str) throws MalformedURIException {
        int i;
        int iLastIndexOf;
        int iLastIndexOf2;
        if (uri == null && (str == null || str.trim().length() == 0)) {
            throw new MalformedURIException(Utils.messages.createMessage("ER_CANNOT_INIT_URI_EMPTY_PARMS", null));
        }
        if (str == null || str.trim().length() == 0) {
            initialize(uri);
            return;
        }
        String strTrim = str.trim();
        int length = strTrim.length();
        int iIndexOf = strTrim.indexOf(58);
        if (iIndexOf < 0) {
            if (uri == null) {
                throw new MalformedURIException(Utils.messages.createMessage("ER_NO_SCHEME_IN_URI", new Object[]{strTrim}));
            }
        } else {
            initializeScheme(strTrim);
            strTrim = strTrim.substring(iIndexOf + 1);
            length = strTrim.length();
        }
        if (strTrim.startsWith("//")) {
            i = 2;
            while (i < length) {
                char cCharAt = strTrim.charAt(i);
                if (cCharAt == '/' || cCharAt == '?' || cCharAt == '#') {
                    break;
                } else {
                    i++;
                }
            }
            if (i > 2) {
                initializeAuthority(strTrim.substring(2, i));
            } else {
                this.m_host = "";
            }
        } else {
            i = 0;
        }
        initializePath(strTrim.substring(i));
        if (uri != null) {
            if (this.m_path.length() == 0 && this.m_scheme == null && this.m_host == null) {
                this.m_scheme = uri.getScheme();
                this.m_userinfo = uri.getUserinfo();
                this.m_host = uri.getHost();
                this.m_port = uri.getPort();
                this.m_path = uri.getPath();
                if (this.m_queryString == null) {
                    this.m_queryString = uri.getQueryString();
                    return;
                }
                return;
            }
            if (this.m_scheme == null) {
                this.m_scheme = uri.getScheme();
            }
            if (this.m_host == null) {
                this.m_userinfo = uri.getUserinfo();
                this.m_host = uri.getHost();
                this.m_port = uri.getPort();
                if (this.m_path.length() > 0 && this.m_path.startsWith(PsuedoNames.PSEUDONAME_ROOT)) {
                    return;
                }
                String str2 = new String();
                String path = uri.getPath();
                if (path != null && (iLastIndexOf2 = path.lastIndexOf(47)) != -1) {
                    str2 = path.substring(0, iLastIndexOf2 + 1);
                }
                String strConcat = str2.concat(this.m_path);
                while (true) {
                    int iIndexOf2 = strConcat.indexOf("/./");
                    if (iIndexOf2 == -1) {
                        break;
                    } else {
                        strConcat = strConcat.substring(0, iIndexOf2 + 1).concat(strConcat.substring(iIndexOf2 + 3));
                    }
                }
                if (strConcat.endsWith("/.")) {
                    strConcat = strConcat.substring(0, strConcat.length() - 1);
                }
                while (true) {
                    int iIndexOf3 = strConcat.indexOf("/../");
                    if (iIndexOf3 <= 0) {
                        break;
                    }
                    String strSubstring = strConcat.substring(0, strConcat.indexOf("/../"));
                    int iLastIndexOf3 = strSubstring.lastIndexOf(47);
                    if (iLastIndexOf3 != -1) {
                        int i2 = iLastIndexOf3 + 1;
                        if (!strSubstring.substring(iLastIndexOf3).equals(Constants.ATTRVAL_PARENT)) {
                            strConcat = strConcat.substring(0, i2).concat(strConcat.substring(iIndexOf3 + 4));
                        }
                    }
                }
                if (strConcat.endsWith("/..") && (iLastIndexOf = strConcat.substring(0, strConcat.length() - 3).lastIndexOf(47)) != -1) {
                    strConcat = strConcat.substring(0, iLastIndexOf + 1);
                }
                this.m_path = strConcat;
            }
        }
    }

    private void initializeScheme(String str) throws MalformedURIException {
        int length = str.length();
        int i = 0;
        while (i < length) {
            char cCharAt = str.charAt(i);
            if (cCharAt == ':' || cCharAt == '/' || cCharAt == '?' || cCharAt == '#') {
                break;
            } else {
                i++;
            }
        }
        String strSubstring = str.substring(0, i);
        if (strSubstring.length() == 0) {
            throw new MalformedURIException(Utils.messages.createMessage("ER_NO_SCHEME_INURI", null));
        }
        setScheme(strSubstring);
    }

    private void initializeAuthority(String str) throws MalformedURIException {
        String strSubstring;
        int i;
        char cCharAt;
        int i2;
        int length = str.length();
        if (str.indexOf(64, 0) != -1) {
            int i3 = 0;
            cCharAt = 0;
            while (i3 < length) {
                cCharAt = str.charAt(i3);
                if (cCharAt == '@') {
                    break;
                } else {
                    i3++;
                }
            }
            strSubstring = str.substring(0, i3);
            i = i3 + 1;
        } else {
            strSubstring = null;
            i = 0;
            cCharAt = 0;
        }
        char cCharAt2 = cCharAt;
        int i4 = i;
        while (i4 < length && (cCharAt2 = str.charAt(i4)) != ':') {
            i4++;
        }
        String strSubstring2 = str.substring(i, i4);
        if (strSubstring2.length() > 0 && cCharAt2 == ':') {
            int i5 = i4 + 1;
            int i6 = i5;
            while (i6 < length) {
                i6++;
            }
            String strSubstring3 = str.substring(i5, i6);
            if (strSubstring3.length() > 0) {
                for (int i7 = 0; i7 < strSubstring3.length(); i7++) {
                    if (!isDigit(strSubstring3.charAt(i7))) {
                        throw new MalformedURIException(strSubstring3 + " is invalid. Port should only contain digits!");
                    }
                }
                try {
                    i2 = Integer.parseInt(strSubstring3);
                } catch (NumberFormatException e) {
                    i2 = -1;
                }
            }
        } else {
            i2 = -1;
        }
        setHost(strSubstring2);
        setPort(i2);
        setUserinfo(strSubstring);
    }

    private void initializePath(String str) throws MalformedURIException {
        int i;
        if (str == null) {
            throw new MalformedURIException("Cannot initialize path from null string!");
        }
        int length = str.length();
        int i2 = 0;
        char cCharAt = 0;
        while (i2 < length && (cCharAt = str.charAt(i2)) != '?' && cCharAt != '#') {
            if (cCharAt == '%') {
                int i3 = i2 + 2;
                if (i3 >= length || !isHex(str.charAt(i2 + 1)) || !isHex(str.charAt(i3))) {
                    throw new MalformedURIException(Utils.messages.createMessage("ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE", null));
                }
            } else if (!isReservedCharacter(cCharAt) && !isUnreservedCharacter(cCharAt) && '\\' != cCharAt) {
                throw new MalformedURIException(Utils.messages.createMessage("ER_PATH_INVALID_CHAR", new Object[]{String.valueOf(cCharAt)}));
            }
            i2++;
        }
        this.m_path = str.substring(0, i2);
        if (cCharAt == '?') {
            int i4 = i2 + 1;
            i = i4;
            while (i < length) {
                cCharAt = str.charAt(i);
                if (cCharAt == '#') {
                    break;
                }
                if (cCharAt == '%') {
                    int i5 = i + 2;
                    if (i5 >= length || !isHex(str.charAt(i + 1)) || !isHex(str.charAt(i5))) {
                        throw new MalformedURIException("Query string contains invalid escape sequence!");
                    }
                } else if (!isReservedCharacter(cCharAt) && !isUnreservedCharacter(cCharAt)) {
                    throw new MalformedURIException("Query string contains invalid character:" + cCharAt);
                }
                i++;
            }
            this.m_queryString = str.substring(i4, i);
        } else {
            i = i2;
        }
        if (cCharAt == '#') {
            int i6 = i + 1;
            int i7 = i6;
            while (i7 < length) {
                char cCharAt2 = str.charAt(i7);
                if (cCharAt2 == '%') {
                    int i8 = i7 + 2;
                    if (i8 >= length || !isHex(str.charAt(i7 + 1)) || !isHex(str.charAt(i8))) {
                        throw new MalformedURIException("Fragment contains invalid escape sequence!");
                    }
                } else if (!isReservedCharacter(cCharAt2) && !isUnreservedCharacter(cCharAt2)) {
                    throw new MalformedURIException("Fragment contains invalid character:" + cCharAt2);
                }
                i7++;
            }
            this.m_fragment = str.substring(i6, i7);
        }
    }

    public String getScheme() {
        return this.m_scheme;
    }

    public String getSchemeSpecificPart() {
        StringBuffer stringBuffer = new StringBuffer();
        if (this.m_userinfo != null || this.m_host != null || this.m_port != -1) {
            stringBuffer.append("//");
        }
        if (this.m_userinfo != null) {
            stringBuffer.append(this.m_userinfo);
            stringBuffer.append('@');
        }
        if (this.m_host != null) {
            stringBuffer.append(this.m_host);
        }
        if (this.m_port != -1) {
            stringBuffer.append(':');
            stringBuffer.append(this.m_port);
        }
        if (this.m_path != null) {
            stringBuffer.append(this.m_path);
        }
        if (this.m_queryString != null) {
            stringBuffer.append('?');
            stringBuffer.append(this.m_queryString);
        }
        if (this.m_fragment != null) {
            stringBuffer.append('#');
            stringBuffer.append(this.m_fragment);
        }
        return stringBuffer.toString();
    }

    public String getUserinfo() {
        return this.m_userinfo;
    }

    public String getHost() {
        return this.m_host;
    }

    public int getPort() {
        return this.m_port;
    }

    public String getPath(boolean z, boolean z2) {
        StringBuffer stringBuffer = new StringBuffer(this.m_path);
        if (z && this.m_queryString != null) {
            stringBuffer.append('?');
            stringBuffer.append(this.m_queryString);
        }
        if (z2 && this.m_fragment != null) {
            stringBuffer.append('#');
            stringBuffer.append(this.m_fragment);
        }
        return stringBuffer.toString();
    }

    public String getPath() {
        return this.m_path;
    }

    public String getQueryString() {
        return this.m_queryString;
    }

    public String getFragment() {
        return this.m_fragment;
    }

    public void setScheme(String str) throws MalformedURIException {
        if (str == null) {
            throw new MalformedURIException(Utils.messages.createMessage("ER_SCHEME_FROM_NULL_STRING", null));
        }
        if (!isConformantSchemeName(str)) {
            throw new MalformedURIException(Utils.messages.createMessage("ER_SCHEME_NOT_CONFORMANT", null));
        }
        this.m_scheme = str.toLowerCase();
    }

    public void setUserinfo(String str) throws MalformedURIException {
        if (str == null) {
            this.m_userinfo = null;
        } else {
            if (this.m_host == null) {
                throw new MalformedURIException("Userinfo cannot be set when host is null!");
            }
            int length = str.length();
            for (int i = 0; i < length; i++) {
                char cCharAt = str.charAt(i);
                if (cCharAt == '%') {
                    int i2 = i + 2;
                    if (i2 >= length || !isHex(str.charAt(i + 1)) || !isHex(str.charAt(i2))) {
                        throw new MalformedURIException("Userinfo contains invalid escape sequence!");
                    }
                } else if (!isUnreservedCharacter(cCharAt) && USERINFO_CHARACTERS.indexOf(cCharAt) == -1) {
                    throw new MalformedURIException("Userinfo contains invalid character:" + cCharAt);
                }
            }
        }
        this.m_userinfo = str;
    }

    public void setHost(String str) throws MalformedURIException {
        if (str == null || str.trim().length() == 0) {
            this.m_host = str;
            this.m_userinfo = null;
            this.m_port = -1;
        } else if (!isWellFormedAddress(str)) {
            throw new MalformedURIException(Utils.messages.createMessage("ER_HOST_ADDRESS_NOT_WELLFORMED", null));
        }
        this.m_host = str;
    }

    public void setPort(int i) throws MalformedURIException {
        if (i >= 0 && i <= 65535) {
            if (this.m_host == null) {
                throw new MalformedURIException(Utils.messages.createMessage("ER_PORT_WHEN_HOST_NULL", null));
            }
        } else if (i != -1) {
            throw new MalformedURIException(Utils.messages.createMessage("ER_INVALID_PORT", null));
        }
        this.m_port = i;
    }

    public void setPath(String str) throws MalformedURIException {
        if (str == null) {
            this.m_path = null;
            this.m_queryString = null;
            this.m_fragment = null;
            return;
        }
        initializePath(str);
    }

    public void appendPath(String str) throws MalformedURIException {
        if (str == null || str.trim().length() == 0) {
            return;
        }
        if (!isURIString(str)) {
            throw new MalformedURIException(Utils.messages.createMessage("ER_PATH_INVALID_CHAR", new Object[]{str}));
        }
        if (this.m_path == null || this.m_path.trim().length() == 0) {
            if (str.startsWith(PsuedoNames.PSEUDONAME_ROOT)) {
                this.m_path = str;
                return;
            }
            this.m_path = PsuedoNames.PSEUDONAME_ROOT + str;
            return;
        }
        if (this.m_path.endsWith(PsuedoNames.PSEUDONAME_ROOT)) {
            if (str.startsWith(PsuedoNames.PSEUDONAME_ROOT)) {
                this.m_path = this.m_path.concat(str.substring(1));
                return;
            } else {
                this.m_path = this.m_path.concat(str);
                return;
            }
        }
        if (str.startsWith(PsuedoNames.PSEUDONAME_ROOT)) {
            this.m_path = this.m_path.concat(str);
            return;
        }
        this.m_path = this.m_path.concat(PsuedoNames.PSEUDONAME_ROOT + str);
    }

    public void setQueryString(String str) throws MalformedURIException {
        if (str == null) {
            this.m_queryString = null;
            return;
        }
        if (!isGenericURI()) {
            throw new MalformedURIException("Query string can only be set for a generic URI!");
        }
        if (getPath() == null) {
            throw new MalformedURIException("Query string cannot be set when path is null!");
        }
        if (!isURIString(str)) {
            throw new MalformedURIException("Query string contains invalid character!");
        }
        this.m_queryString = str;
    }

    public void setFragment(String str) throws MalformedURIException {
        if (str == null) {
            this.m_fragment = null;
            return;
        }
        if (!isGenericURI()) {
            throw new MalformedURIException(Utils.messages.createMessage("ER_FRAG_FOR_GENERIC_URI", null));
        }
        if (getPath() == null) {
            throw new MalformedURIException(Utils.messages.createMessage("ER_FRAG_WHEN_PATH_NULL", null));
        }
        if (!isURIString(str)) {
            throw new MalformedURIException(Utils.messages.createMessage("ER_FRAG_INVALID_CHAR", null));
        }
        this.m_fragment = str;
    }

    public boolean equals(Object obj) {
        if (obj instanceof URI) {
            URI uri = (URI) obj;
            if ((this.m_scheme == null && uri.m_scheme == null) || (this.m_scheme != null && uri.m_scheme != null && this.m_scheme.equals(uri.m_scheme))) {
                if ((this.m_userinfo == null && uri.m_userinfo == null) || (this.m_userinfo != null && uri.m_userinfo != null && this.m_userinfo.equals(uri.m_userinfo))) {
                    if (((this.m_host == null && uri.m_host == null) || (this.m_host != null && uri.m_host != null && this.m_host.equals(uri.m_host))) && this.m_port == uri.m_port) {
                        if ((this.m_path == null && uri.m_path == null) || (this.m_path != null && uri.m_path != null && this.m_path.equals(uri.m_path))) {
                            if (!(this.m_queryString == null && uri.m_queryString == null) && (this.m_queryString == null || uri.m_queryString == null || !this.m_queryString.equals(uri.m_queryString))) {
                                return false;
                            }
                            if (this.m_fragment != null || uri.m_fragment != null) {
                                if (this.m_fragment != null && uri.m_fragment != null && this.m_fragment.equals(uri.m_fragment)) {
                                    return true;
                                }
                                return false;
                            }
                            return true;
                        }
                        return false;
                    }
                    return false;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        if (this.m_scheme != null) {
            stringBuffer.append(this.m_scheme);
            stringBuffer.append(':');
        }
        stringBuffer.append(getSchemeSpecificPart());
        return stringBuffer.toString();
    }

    public boolean isGenericURI() {
        return this.m_host != null;
    }

    public static boolean isConformantSchemeName(String str) {
        if (str == null || str.trim().length() == 0 || !isAlpha(str.charAt(0))) {
            return false;
        }
        for (int i = 1; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (!isAlphanum(cCharAt) && SCHEME_CHARACTERS.indexOf(cCharAt) == -1) {
                return false;
            }
        }
        return true;
    }

    public static boolean isWellFormedAddress(String str) {
        String strTrim;
        int length;
        int i;
        if (str == null || (length = (strTrim = str.trim()).length()) == 0 || length > 255 || strTrim.startsWith(Constants.ATTRVAL_THIS) || strTrim.startsWith("-")) {
            return false;
        }
        int iLastIndexOf = strTrim.lastIndexOf(46);
        if (strTrim.endsWith(Constants.ATTRVAL_THIS)) {
            iLastIndexOf = strTrim.substring(0, iLastIndexOf).lastIndexOf(46);
        }
        int i2 = iLastIndexOf + 1;
        if (i2 < length && isDigit(str.charAt(i2))) {
            int i3 = 0;
            for (int i4 = 0; i4 < length; i4++) {
                char cCharAt = strTrim.charAt(i4);
                if (cCharAt == '.') {
                    if (!isDigit(strTrim.charAt(i4 - 1)) || ((i = i4 + 1) < length && !isDigit(strTrim.charAt(i)))) {
                        return false;
                    }
                    i3++;
                } else if (!isDigit(cCharAt)) {
                    return false;
                }
            }
            if (i3 != 3) {
                return false;
            }
        } else {
            for (int i5 = 0; i5 < length; i5++) {
                char cCharAt2 = strTrim.charAt(i5);
                if (cCharAt2 == '.') {
                    if (!isAlphanum(strTrim.charAt(i5 - 1))) {
                        return false;
                    }
                    int i6 = i5 + 1;
                    if (i6 < length && !isAlphanum(strTrim.charAt(i6))) {
                        return false;
                    }
                } else if (!isAlphanum(cCharAt2) && cCharAt2 != '-') {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isHex(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isAlphanum(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private static boolean isReservedCharacter(char c) {
        return RESERVED_CHARACTERS.indexOf(c) != -1;
    }

    private static boolean isUnreservedCharacter(char c) {
        return isAlphanum(c) || MARK_CHARACTERS.indexOf(c) != -1;
    }

    private static boolean isURIString(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        int i = 0;
        while (i < length) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '%') {
                int i2 = i + 2;
                if (i2 >= length || !isHex(str.charAt(i + 1)) || !isHex(str.charAt(i2))) {
                    return false;
                }
                i = i2;
            } else if (!isReservedCharacter(cCharAt) && !isUnreservedCharacter(cCharAt)) {
                return false;
            }
            i++;
        }
        return true;
    }
}
