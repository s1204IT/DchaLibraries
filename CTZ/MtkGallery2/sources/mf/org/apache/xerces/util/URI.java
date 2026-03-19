package mf.org.apache.xerces.util;

import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;

public class URI implements Serializable {
    private static final int ASCII_ALPHA_CHARACTERS = 16;
    private static final int ASCII_DIGIT_CHARACTERS = 32;
    private static final int ASCII_HEX_CHARACTERS = 64;
    private static boolean DEBUG = false;
    private static final int MARK_CHARACTERS = 2;
    private static final int MASK_ALPHA_NUMERIC = 48;
    private static final int MASK_PATH_CHARACTER = 178;
    private static final int MASK_SCHEME_CHARACTER = 52;
    private static final int MASK_UNRESERVED_MASK = 50;
    private static final int MASK_URI_CHARACTER = 51;
    private static final int MASK_USERINFO_CHARACTER = 58;
    private static final int PATH_CHARACTERS = 128;
    private static final int RESERVED_CHARACTERS = 1;
    private static final int SCHEME_CHARACTERS = 4;
    private static final int USERINFO_CHARACTERS = 8;
    private static final byte[] fgLookupTable = new byte[128];
    static final long serialVersionUID = 1601921774685357214L;
    private String m_fragment;
    private String m_host;
    private String m_path;
    private int m_port;
    private String m_queryString;
    private String m_regAuthority;
    private String m_scheme;
    private String m_userinfo;

    public static class MalformedURIException extends IOException {
        static final long serialVersionUID = -6695054834342951930L;

        public MalformedURIException() {
        }

        public MalformedURIException(String p_msg) {
            super(p_msg);
        }
    }

    static {
        for (int i = 48; i <= 57; i++) {
            byte[] bArr = fgLookupTable;
            bArr[i] = (byte) (bArr[i] | 96);
        }
        for (int i2 = 65; i2 <= 70; i2++) {
            byte[] bArr2 = fgLookupTable;
            bArr2[i2] = (byte) (bArr2[i2] | 80);
            byte[] bArr3 = fgLookupTable;
            int i3 = i2 + 32;
            bArr3[i3] = (byte) (bArr3[i3] | 80);
        }
        for (int i4 = 71; i4 <= 90; i4++) {
            byte[] bArr4 = fgLookupTable;
            bArr4[i4] = (byte) (bArr4[i4] | 16);
            byte[] bArr5 = fgLookupTable;
            int i5 = i4 + 32;
            bArr5[i5] = (byte) (bArr5[i5] | 16);
        }
        byte[] bArr6 = fgLookupTable;
        bArr6[59] = (byte) (bArr6[59] | 1);
        byte[] bArr7 = fgLookupTable;
        bArr7[47] = (byte) (bArr7[47] | 1);
        byte[] bArr8 = fgLookupTable;
        bArr8[63] = (byte) (bArr8[63] | 1);
        byte[] bArr9 = fgLookupTable;
        bArr9[MASK_USERINFO_CHARACTER] = (byte) (bArr9[MASK_USERINFO_CHARACTER] | 1);
        byte[] bArr10 = fgLookupTable;
        bArr10[64] = (byte) (bArr10[64] | 1);
        byte[] bArr11 = fgLookupTable;
        bArr11[38] = (byte) (bArr11[38] | 1);
        byte[] bArr12 = fgLookupTable;
        bArr12[61] = (byte) (bArr12[61] | 1);
        byte[] bArr13 = fgLookupTable;
        bArr13[43] = (byte) (bArr13[43] | 1);
        byte[] bArr14 = fgLookupTable;
        bArr14[36] = (byte) (bArr14[36] | 1);
        byte[] bArr15 = fgLookupTable;
        bArr15[44] = (byte) (bArr15[44] | 1);
        byte[] bArr16 = fgLookupTable;
        bArr16[91] = (byte) (bArr16[91] | 1);
        byte[] bArr17 = fgLookupTable;
        bArr17[93] = (byte) (bArr17[93] | 1);
        byte[] bArr18 = fgLookupTable;
        bArr18[45] = (byte) (bArr18[45] | 2);
        byte[] bArr19 = fgLookupTable;
        bArr19[95] = (byte) (bArr19[95] | 2);
        byte[] bArr20 = fgLookupTable;
        bArr20[46] = (byte) (bArr20[46] | 2);
        byte[] bArr21 = fgLookupTable;
        bArr21[33] = (byte) (bArr21[33] | 2);
        byte[] bArr22 = fgLookupTable;
        bArr22[126] = (byte) (bArr22[126] | 2);
        byte[] bArr23 = fgLookupTable;
        bArr23[42] = (byte) (bArr23[42] | 2);
        byte[] bArr24 = fgLookupTable;
        bArr24[39] = (byte) (bArr24[39] | 2);
        byte[] bArr25 = fgLookupTable;
        bArr25[40] = (byte) (bArr25[40] | 2);
        byte[] bArr26 = fgLookupTable;
        bArr26[41] = (byte) (bArr26[41] | 2);
        byte[] bArr27 = fgLookupTable;
        bArr27[43] = (byte) (bArr27[43] | 4);
        byte[] bArr28 = fgLookupTable;
        bArr28[45] = (byte) (bArr28[45] | 4);
        byte[] bArr29 = fgLookupTable;
        bArr29[46] = (byte) (bArr29[46] | 4);
        byte[] bArr30 = fgLookupTable;
        bArr30[59] = (byte) (bArr30[59] | 8);
        byte[] bArr31 = fgLookupTable;
        bArr31[MASK_USERINFO_CHARACTER] = (byte) (bArr31[MASK_USERINFO_CHARACTER] | 8);
        byte[] bArr32 = fgLookupTable;
        bArr32[38] = (byte) (bArr32[38] | 8);
        byte[] bArr33 = fgLookupTable;
        bArr33[61] = (byte) (bArr33[61] | 8);
        byte[] bArr34 = fgLookupTable;
        bArr34[43] = (byte) (bArr34[43] | 8);
        byte[] bArr35 = fgLookupTable;
        bArr35[36] = (byte) (bArr35[36] | 8);
        byte[] bArr36 = fgLookupTable;
        bArr36[44] = (byte) (bArr36[44] | 8);
        byte[] bArr37 = fgLookupTable;
        bArr37[59] = (byte) (bArr37[59] | 128);
        byte[] bArr38 = fgLookupTable;
        bArr38[47] = (byte) (bArr38[47] | 128);
        byte[] bArr39 = fgLookupTable;
        bArr39[MASK_USERINFO_CHARACTER] = (byte) (bArr39[MASK_USERINFO_CHARACTER] | 128);
        byte[] bArr40 = fgLookupTable;
        bArr40[64] = (byte) (bArr40[64] | 128);
        byte[] bArr41 = fgLookupTable;
        bArr41[38] = (byte) (bArr41[38] | 128);
        byte[] bArr42 = fgLookupTable;
        bArr42[61] = (byte) (bArr42[61] | 128);
        byte[] bArr43 = fgLookupTable;
        bArr43[43] = (byte) (bArr43[43] | 128);
        byte[] bArr44 = fgLookupTable;
        bArr44[36] = (byte) (bArr44[36] | 128);
        byte[] bArr45 = fgLookupTable;
        bArr45[44] = (byte) (128 | bArr45[44]);
        DEBUG = false;
    }

    public URI() {
        this.m_scheme = null;
        this.m_userinfo = null;
        this.m_host = null;
        this.m_port = -1;
        this.m_regAuthority = null;
        this.m_path = null;
        this.m_queryString = null;
        this.m_fragment = null;
    }

    public URI(URI p_other) {
        this.m_scheme = null;
        this.m_userinfo = null;
        this.m_host = null;
        this.m_port = -1;
        this.m_regAuthority = null;
        this.m_path = null;
        this.m_queryString = null;
        this.m_fragment = null;
        initialize(p_other);
    }

    public URI(String p_uriSpec) throws MalformedURIException {
        this((URI) null, p_uriSpec);
    }

    public URI(String p_uriSpec, boolean allowNonAbsoluteURI) throws MalformedURIException {
        this(null, p_uriSpec, allowNonAbsoluteURI);
    }

    public URI(URI p_base, String p_uriSpec) throws MalformedURIException {
        this.m_scheme = null;
        this.m_userinfo = null;
        this.m_host = null;
        this.m_port = -1;
        this.m_regAuthority = null;
        this.m_path = null;
        this.m_queryString = null;
        this.m_fragment = null;
        initialize(p_base, p_uriSpec);
    }

    public URI(URI p_base, String p_uriSpec, boolean allowNonAbsoluteURI) throws MalformedURIException {
        this.m_scheme = null;
        this.m_userinfo = null;
        this.m_host = null;
        this.m_port = -1;
        this.m_regAuthority = null;
        this.m_path = null;
        this.m_queryString = null;
        this.m_fragment = null;
        initialize(p_base, p_uriSpec, allowNonAbsoluteURI);
    }

    public URI(String p_scheme, String p_schemeSpecificPart) throws MalformedURIException {
        this.m_scheme = null;
        this.m_userinfo = null;
        this.m_host = null;
        this.m_port = -1;
        this.m_regAuthority = null;
        this.m_path = null;
        this.m_queryString = null;
        this.m_fragment = null;
        if (p_scheme == null || p_scheme.trim().length() == 0) {
            throw new MalformedURIException("Cannot construct URI with null/empty scheme!");
        }
        if (p_schemeSpecificPart == null || p_schemeSpecificPart.trim().length() == 0) {
            throw new MalformedURIException("Cannot construct URI with null/empty scheme-specific part!");
        }
        setScheme(p_scheme);
        setPath(p_schemeSpecificPart);
    }

    public URI(String p_scheme, String p_host, String p_path, String p_queryString, String p_fragment) throws MalformedURIException {
        this(p_scheme, null, p_host, -1, p_path, p_queryString, p_fragment);
    }

    public URI(String p_scheme, String p_userinfo, String p_host, int p_port, String p_path, String p_queryString, String p_fragment) throws MalformedURIException {
        this.m_scheme = null;
        this.m_userinfo = null;
        this.m_host = null;
        this.m_port = -1;
        this.m_regAuthority = null;
        this.m_path = null;
        this.m_queryString = null;
        this.m_fragment = null;
        if (p_scheme == null || p_scheme.trim().length() == 0) {
            throw new MalformedURIException("Scheme is required!");
        }
        if (p_host == null) {
            if (p_userinfo != null) {
                throw new MalformedURIException("Userinfo may not be specified if host is not specified!");
            }
            if (p_port != -1) {
                throw new MalformedURIException("Port may not be specified if host is not specified!");
            }
        }
        if (p_path != null) {
            if (p_path.indexOf(63) == -1 || p_queryString == null) {
                if (p_path.indexOf(35) != -1 && p_fragment != null) {
                    throw new MalformedURIException("Fragment cannot be specified in both the path and fragment!");
                }
            } else {
                throw new MalformedURIException("Query string cannot be specified in path and query string!");
            }
        }
        setScheme(p_scheme);
        setHost(p_host);
        setPort(p_port);
        setUserinfo(p_userinfo);
        setPath(p_path);
        setQueryString(p_queryString);
        setFragment(p_fragment);
    }

    private void initialize(URI p_other) {
        this.m_scheme = p_other.getScheme();
        this.m_userinfo = p_other.getUserinfo();
        this.m_host = p_other.getHost();
        this.m_port = p_other.getPort();
        this.m_regAuthority = p_other.getRegBasedAuthority();
        this.m_path = p_other.getPath();
        this.m_queryString = p_other.getQueryString();
        this.m_fragment = p_other.getFragment();
    }

    private void initialize(URI p_base, String p_uriSpec, boolean allowNonAbsoluteURI) throws MalformedURIException {
        int uriSpecLen = p_uriSpec != null ? p_uriSpec.length() : 0;
        if (p_base == null && uriSpecLen == 0) {
            if (allowNonAbsoluteURI) {
                this.m_path = "";
                return;
            }
            throw new MalformedURIException("Cannot initialize URI with empty parameters.");
        }
        if (uriSpecLen == 0) {
            initialize(p_base);
            return;
        }
        int index = 0;
        int colonIdx = p_uriSpec.indexOf(MASK_USERINFO_CHARACTER);
        if (colonIdx != -1) {
            int searchFrom = colonIdx - 1;
            int slashIdx = p_uriSpec.lastIndexOf(47, searchFrom);
            int queryIdx = p_uriSpec.lastIndexOf(63, searchFrom);
            int fragmentIdx = p_uriSpec.lastIndexOf(35, searchFrom);
            if (colonIdx == 0 || slashIdx != -1 || queryIdx != -1 || fragmentIdx != -1) {
                if (colonIdx == 0 || (p_base == null && fragmentIdx != 0 && !allowNonAbsoluteURI)) {
                    throw new MalformedURIException("No scheme found in URI.");
                }
            } else {
                initializeScheme(p_uriSpec);
                index = this.m_scheme.length() + 1;
                if (colonIdx == uriSpecLen - 1 || p_uriSpec.charAt(colonIdx + 1) == '#') {
                    throw new MalformedURIException("Scheme specific part cannot be empty.");
                }
            }
        } else if (p_base == null && p_uriSpec.indexOf(35) != 0 && !allowNonAbsoluteURI) {
            throw new MalformedURIException("No scheme found in URI.");
        }
        if (index + 1 < uriSpecLen && p_uriSpec.charAt(index) == '/' && p_uriSpec.charAt(index + 1) == '/') {
            index += 2;
            while (index < uriSpecLen) {
                char testChar = p_uriSpec.charAt(index);
                if (testChar == '/' || testChar == '?' || testChar == '#') {
                    break;
                } else {
                    index++;
                }
            }
            if (index > index) {
                if (!initializeAuthority(p_uriSpec.substring(index, index))) {
                    index -= 2;
                }
            } else {
                this.m_host = "";
            }
        }
        initializePath(p_uriSpec, index);
        if (p_base != null) {
            absolutize(p_base);
        }
    }

    private void initialize(URI p_base, String p_uriSpec) throws MalformedURIException {
        int uriSpecLen = p_uriSpec != null ? p_uriSpec.length() : 0;
        if (p_base == null && uriSpecLen == 0) {
            throw new MalformedURIException("Cannot initialize URI with empty parameters.");
        }
        if (uriSpecLen == 0) {
            initialize(p_base);
            return;
        }
        int index = 0;
        int colonIdx = p_uriSpec.indexOf(MASK_USERINFO_CHARACTER);
        if (colonIdx != -1) {
            int searchFrom = colonIdx - 1;
            int slashIdx = p_uriSpec.lastIndexOf(47, searchFrom);
            int queryIdx = p_uriSpec.lastIndexOf(63, searchFrom);
            int fragmentIdx = p_uriSpec.lastIndexOf(35, searchFrom);
            if (colonIdx == 0 || slashIdx != -1 || queryIdx != -1 || fragmentIdx != -1) {
                if (colonIdx == 0 || (p_base == null && fragmentIdx != 0)) {
                    throw new MalformedURIException("No scheme found in URI.");
                }
            } else {
                initializeScheme(p_uriSpec);
                index = this.m_scheme.length() + 1;
                if (colonIdx == uriSpecLen - 1 || p_uriSpec.charAt(colonIdx + 1) == '#') {
                    throw new MalformedURIException("Scheme specific part cannot be empty.");
                }
            }
        } else if (p_base == null && p_uriSpec.indexOf(35) != 0) {
            throw new MalformedURIException("No scheme found in URI.");
        }
        if (index + 1 < uriSpecLen && p_uriSpec.charAt(index) == '/' && p_uriSpec.charAt(index + 1) == '/') {
            index += 2;
            while (index < uriSpecLen) {
                char testChar = p_uriSpec.charAt(index);
                if (testChar == '/' || testChar == '?' || testChar == '#') {
                    break;
                } else {
                    index++;
                }
            }
            if (index > index) {
                if (!initializeAuthority(p_uriSpec.substring(index, index))) {
                    index -= 2;
                }
            } else {
                this.m_host = "";
            }
        }
        initializePath(p_uriSpec, index);
        if (p_base != null) {
            absolutize(p_base);
        }
    }

    public void absolutize(URI p_base) {
        int segIndex;
        if (this.m_path.length() == 0 && this.m_scheme == null && this.m_host == null && this.m_regAuthority == null) {
            this.m_scheme = p_base.getScheme();
            this.m_userinfo = p_base.getUserinfo();
            this.m_host = p_base.getHost();
            this.m_port = p_base.getPort();
            this.m_regAuthority = p_base.getRegBasedAuthority();
            this.m_path = p_base.getPath();
            if (this.m_queryString == null) {
                this.m_queryString = p_base.getQueryString();
                if (this.m_fragment == null) {
                    this.m_fragment = p_base.getFragment();
                    return;
                }
                return;
            }
            return;
        }
        if (this.m_scheme == null) {
            this.m_scheme = p_base.getScheme();
            if (this.m_host == null && this.m_regAuthority == null) {
                this.m_userinfo = p_base.getUserinfo();
                this.m_host = p_base.getHost();
                this.m_port = p_base.getPort();
                this.m_regAuthority = p_base.getRegBasedAuthority();
                if (this.m_path.length() > 0 && this.m_path.startsWith("/")) {
                    return;
                }
                String path = "";
                String basePath = p_base.getPath();
                if (basePath != null && basePath.length() > 0) {
                    int lastSlash = basePath.lastIndexOf(47);
                    if (lastSlash != -1) {
                        path = basePath.substring(0, lastSlash + 1);
                    }
                } else if (this.m_path.length() > 0) {
                    path = "/";
                }
                String path2 = path.concat(this.m_path);
                while (true) {
                    int index = path2.indexOf("/./");
                    if (index == -1) {
                        break;
                    } else {
                        path2 = path2.substring(0, index + 1).concat(path2.substring(index + 3));
                    }
                }
                if (path2.endsWith("/.")) {
                    path2 = path2.substring(0, path2.length() - 1);
                }
                int index2 = 1;
                while (true) {
                    int index3 = path2.indexOf("/../", index2);
                    if (index3 <= 0) {
                        break;
                    }
                    String tempString = path2.substring(0, path2.indexOf("/../"));
                    int segIndex2 = tempString.lastIndexOf(47);
                    if (segIndex2 != -1) {
                        if (!tempString.substring(segIndex2).equals("..")) {
                            path2 = path2.substring(0, segIndex2 + 1).concat(path2.substring(index3 + 4));
                            index2 = segIndex2;
                        } else {
                            index2 = index3 + 4;
                        }
                    } else {
                        index2 = index3 + 4;
                    }
                }
                if (path2.endsWith("/..") && (segIndex = path2.substring(0, path2.length() - 3).lastIndexOf(47)) != -1) {
                    path2 = path2.substring(0, segIndex + 1);
                }
                this.m_path = path2;
            }
        }
    }

    private void initializeScheme(String p_uriSpec) throws MalformedURIException {
        int uriSpecLen = p_uriSpec.length();
        int index = 0;
        while (index < uriSpecLen) {
            char testChar = p_uriSpec.charAt(index);
            if (testChar == MASK_USERINFO_CHARACTER || testChar == '/' || testChar == '?' || testChar == '#') {
                break;
            } else {
                index++;
            }
        }
        String scheme = p_uriSpec.substring(0, index);
        if (scheme.length() == 0) {
            throw new MalformedURIException("No scheme found in URI.");
        }
        setScheme(scheme);
    }

    private boolean initializeAuthority(String p_uriSpec) {
        int index = 0;
        int end = p_uriSpec.length();
        String userinfo = null;
        if (p_uriSpec.indexOf(64, 0) != -1) {
            while (index < end) {
                char testChar = p_uriSpec.charAt(index);
                if (testChar == '@') {
                    break;
                }
                index++;
            }
            userinfo = p_uriSpec.substring(0, index);
            index++;
        }
        int start = index;
        boolean hasPort = false;
        if (index < end) {
            if (p_uriSpec.charAt(start) == '[') {
                int bracketIndex = p_uriSpec.indexOf(93, start);
                int index2 = bracketIndex != -1 ? bracketIndex : end;
                if (index2 + 1 < end && p_uriSpec.charAt(index2 + 1) == MASK_USERINFO_CHARACTER) {
                    index = index2 + 1;
                    hasPort = true;
                } else {
                    index = end;
                }
            } else {
                int colonIndex = p_uriSpec.lastIndexOf(MASK_USERINFO_CHARACTER, end);
                index = colonIndex > start ? colonIndex : end;
                hasPort = index != end;
            }
        }
        String host = p_uriSpec.substring(start, index);
        int port = -1;
        if (host.length() > 0 && hasPort) {
            int index3 = index + 1;
            while (index3 < end) {
                index3++;
            }
            String portStr = p_uriSpec.substring(index3, index3);
            if (portStr.length() > 0) {
                try {
                    port = Integer.parseInt(portStr);
                    if (port == -1) {
                        port--;
                    }
                } catch (NumberFormatException e) {
                    port = -2;
                }
            }
        }
        if (isValidServerBasedAuthority(host, port, userinfo)) {
            this.m_host = host;
            this.m_port = port;
            this.m_userinfo = userinfo;
            return true;
        }
        if (!isValidRegistryBasedAuthority(p_uriSpec)) {
            return false;
        }
        this.m_regAuthority = p_uriSpec;
        return true;
    }

    private boolean isValidServerBasedAuthority(String host, int port, String userinfo) {
        if (!isWellFormedAddress(host) || port < -1 || port > 65535) {
            return false;
        }
        if (userinfo != null) {
            int index = 0;
            int end = userinfo.length();
            while (index < end) {
                char testChar = userinfo.charAt(index);
                if (testChar == '%') {
                    if (index + 2 >= end || !isHex(userinfo.charAt(index + 1)) || !isHex(userinfo.charAt(index + 2))) {
                        return false;
                    }
                    index += 2;
                } else if (!isUserinfoCharacter(testChar)) {
                    return false;
                }
                index++;
            }
        }
        return true;
    }

    private boolean isValidRegistryBasedAuthority(String authority) {
        int index = 0;
        int end = authority.length();
        while (index < end) {
            char testChar = authority.charAt(index);
            if (testChar == '%') {
                if (index + 2 >= end || !isHex(authority.charAt(index + 1)) || !isHex(authority.charAt(index + 2))) {
                    return false;
                }
                index += 2;
            } else if (!isPathCharacter(testChar)) {
                return false;
            }
            index++;
        }
        return true;
    }

    private void initializePath(String p_uriSpec, int p_nStartIndex) throws MalformedURIException {
        if (p_uriSpec == null) {
            throw new MalformedURIException("Cannot initialize path from null string!");
        }
        int index = p_nStartIndex;
        int end = p_uriSpec.length();
        char testChar = 0;
        if (p_nStartIndex < end) {
            if (getScheme() == null || p_uriSpec.charAt(p_nStartIndex) == '/') {
                while (true) {
                    if (index >= end) {
                        break;
                    }
                    testChar = p_uriSpec.charAt(index);
                    if (testChar == '%') {
                        if (index + 2 >= end || !isHex(p_uriSpec.charAt(index + 1)) || !isHex(p_uriSpec.charAt(index + 2))) {
                            break;
                        } else {
                            index += 2;
                        }
                    } else if (!isPathCharacter(testChar)) {
                        if (testChar != '?' && testChar != '#') {
                            throw new MalformedURIException("Path contains invalid character: " + testChar);
                        }
                    }
                    index++;
                }
            } else {
                while (index < end) {
                    testChar = p_uriSpec.charAt(index);
                    if (testChar == '?' || testChar == '#') {
                        break;
                    }
                    if (testChar == '%') {
                        if (index + 2 >= end || !isHex(p_uriSpec.charAt(index + 1)) || !isHex(p_uriSpec.charAt(index + 2))) {
                            throw new MalformedURIException("Opaque part contains invalid escape sequence!");
                        }
                        index += 2;
                    } else if (!isURICharacter(testChar)) {
                        throw new MalformedURIException("Opaque part contains invalid character: " + testChar);
                    }
                    index++;
                }
            }
        }
        this.m_path = p_uriSpec.substring(p_nStartIndex, index);
        if (testChar == '?') {
            index++;
            while (index < end) {
                testChar = p_uriSpec.charAt(index);
                if (testChar == '#') {
                    break;
                }
                if (testChar == '%') {
                    if (index + 2 >= end || !isHex(p_uriSpec.charAt(index + 1)) || !isHex(p_uriSpec.charAt(index + 2))) {
                        throw new MalformedURIException("Query string contains invalid escape sequence!");
                    }
                    index += 2;
                } else if (!isURICharacter(testChar)) {
                    throw new MalformedURIException("Query string contains invalid character: " + testChar);
                }
                index++;
            }
            this.m_queryString = p_uriSpec.substring(index, index);
        }
        if (testChar == '#') {
            int index2 = index + 1;
            while (index2 < end) {
                char testChar2 = p_uriSpec.charAt(index2);
                if (testChar2 == '%') {
                    if (index2 + 2 >= end || !isHex(p_uriSpec.charAt(index2 + 1)) || !isHex(p_uriSpec.charAt(index2 + 2))) {
                        throw new MalformedURIException("Fragment contains invalid escape sequence!");
                    }
                    index2 += 2;
                } else if (!isURICharacter(testChar2)) {
                    throw new MalformedURIException("Fragment contains invalid character: " + testChar2);
                }
                index2++;
            }
            this.m_fragment = p_uriSpec.substring(index2, index2);
        }
    }

    public String getScheme() {
        return this.m_scheme;
    }

    public String getSchemeSpecificPart() {
        StringBuffer schemespec = new StringBuffer();
        if (this.m_host != null || this.m_regAuthority != null) {
            schemespec.append("//");
            if (this.m_host != null) {
                if (this.m_userinfo != null) {
                    schemespec.append(this.m_userinfo);
                    schemespec.append('@');
                }
                schemespec.append(this.m_host);
                if (this.m_port != -1) {
                    schemespec.append(':');
                    schemespec.append(this.m_port);
                }
            } else {
                schemespec.append(this.m_regAuthority);
            }
        }
        if (this.m_path != null) {
            schemespec.append(this.m_path);
        }
        if (this.m_queryString != null) {
            schemespec.append('?');
            schemespec.append(this.m_queryString);
        }
        if (this.m_fragment != null) {
            schemespec.append('#');
            schemespec.append(this.m_fragment);
        }
        return schemespec.toString();
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

    public String getRegBasedAuthority() {
        return this.m_regAuthority;
    }

    public String getAuthority() {
        StringBuffer authority = new StringBuffer();
        if (this.m_host != null || this.m_regAuthority != null) {
            authority.append("//");
            if (this.m_host != null) {
                if (this.m_userinfo != null) {
                    authority.append(this.m_userinfo);
                    authority.append('@');
                }
                authority.append(this.m_host);
                if (this.m_port != -1) {
                    authority.append(':');
                    authority.append(this.m_port);
                }
            } else {
                authority.append(this.m_regAuthority);
            }
        }
        return authority.toString();
    }

    public String getPath(boolean p_includeQueryString, boolean p_includeFragment) {
        StringBuffer pathString = new StringBuffer(this.m_path);
        if (p_includeQueryString && this.m_queryString != null) {
            pathString.append('?');
            pathString.append(this.m_queryString);
        }
        if (p_includeFragment && this.m_fragment != null) {
            pathString.append('#');
            pathString.append(this.m_fragment);
        }
        return pathString.toString();
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

    public void setScheme(String p_scheme) throws MalformedURIException {
        if (p_scheme == null) {
            throw new MalformedURIException("Cannot set scheme from null string!");
        }
        if (!isConformantSchemeName(p_scheme)) {
            throw new MalformedURIException("The scheme is not conformant.");
        }
        this.m_scheme = p_scheme.toLowerCase(Locale.ENGLISH);
    }

    public void setUserinfo(String p_userinfo) throws MalformedURIException {
        if (p_userinfo == null) {
            this.m_userinfo = null;
            return;
        }
        if (this.m_host == null) {
            throw new MalformedURIException("Userinfo cannot be set when host is null!");
        }
        int end = p_userinfo.length();
        for (int index = 0; index < end; index++) {
            char testChar = p_userinfo.charAt(index);
            if (testChar == '%') {
                if (index + 2 >= end || !isHex(p_userinfo.charAt(index + 1)) || !isHex(p_userinfo.charAt(index + 2))) {
                    throw new MalformedURIException("Userinfo contains invalid escape sequence!");
                }
            } else if (!isUserinfoCharacter(testChar)) {
                throw new MalformedURIException("Userinfo contains invalid character:" + testChar);
            }
        }
        this.m_userinfo = p_userinfo;
    }

    public void setHost(String p_host) throws MalformedURIException {
        if (p_host == null || p_host.length() == 0) {
            if (p_host != null) {
                this.m_regAuthority = null;
            }
            this.m_host = p_host;
            this.m_userinfo = null;
            this.m_port = -1;
            return;
        }
        if (!isWellFormedAddress(p_host)) {
            throw new MalformedURIException("Host is not a well formed address!");
        }
        this.m_host = p_host;
        this.m_regAuthority = null;
    }

    public void setPort(int p_port) throws MalformedURIException {
        if (p_port >= 0 && p_port <= 65535) {
            if (this.m_host == null) {
                throw new MalformedURIException("Port cannot be set when host is null!");
            }
        } else if (p_port != -1) {
            throw new MalformedURIException("Invalid port number!");
        }
        this.m_port = p_port;
    }

    public void setRegBasedAuthority(String authority) throws MalformedURIException {
        if (authority == null) {
            this.m_regAuthority = null;
            return;
        }
        if (authority.length() < 1 || !isValidRegistryBasedAuthority(authority) || authority.indexOf(47) != -1) {
            throw new MalformedURIException("Registry based authority is not well formed.");
        }
        this.m_regAuthority = authority;
        this.m_host = null;
        this.m_userinfo = null;
        this.m_port = -1;
    }

    public void setPath(String p_path) throws MalformedURIException {
        if (p_path == null) {
            this.m_path = null;
            this.m_queryString = null;
            this.m_fragment = null;
            return;
        }
        initializePath(p_path, 0);
    }

    public void appendPath(String p_addToPath) throws MalformedURIException {
        if (p_addToPath == null || p_addToPath.trim().length() == 0) {
            return;
        }
        if (!isURIString(p_addToPath)) {
            throw new MalformedURIException("Path contains invalid character!");
        }
        if (this.m_path == null || this.m_path.trim().length() == 0) {
            if (p_addToPath.startsWith("/")) {
                this.m_path = p_addToPath;
                return;
            }
            this.m_path = "/" + p_addToPath;
            return;
        }
        if (this.m_path.endsWith("/")) {
            if (p_addToPath.startsWith("/")) {
                this.m_path = this.m_path.concat(p_addToPath.substring(1));
                return;
            } else {
                this.m_path = this.m_path.concat(p_addToPath);
                return;
            }
        }
        if (p_addToPath.startsWith("/")) {
            this.m_path = this.m_path.concat(p_addToPath);
            return;
        }
        this.m_path = this.m_path.concat("/" + p_addToPath);
    }

    public void setQueryString(String p_queryString) throws MalformedURIException {
        if (p_queryString == null) {
            this.m_queryString = null;
            return;
        }
        if (!isGenericURI()) {
            throw new MalformedURIException("Query string can only be set for a generic URI!");
        }
        if (getPath() == null) {
            throw new MalformedURIException("Query string cannot be set when path is null!");
        }
        if (!isURIString(p_queryString)) {
            throw new MalformedURIException("Query string contains invalid character!");
        }
        this.m_queryString = p_queryString;
    }

    public void setFragment(String p_fragment) throws MalformedURIException {
        if (p_fragment == null) {
            this.m_fragment = null;
            return;
        }
        if (!isGenericURI()) {
            throw new MalformedURIException("Fragment can only be set for a generic URI!");
        }
        if (getPath() == null) {
            throw new MalformedURIException("Fragment cannot be set when path is null!");
        }
        if (!isURIString(p_fragment)) {
            throw new MalformedURIException("Fragment contains invalid character!");
        }
        this.m_fragment = p_fragment;
    }

    public boolean equals(Object obj) {
        if (obj instanceof URI) {
            if ((this.m_scheme == null && obj.m_scheme == null) || (this.m_scheme != null && obj.m_scheme != null && this.m_scheme.equals(obj.m_scheme))) {
                if ((this.m_userinfo == null && obj.m_userinfo == null) || (this.m_userinfo != null && obj.m_userinfo != null && this.m_userinfo.equals(obj.m_userinfo))) {
                    if (((this.m_host == null && obj.m_host == null) || (this.m_host != null && obj.m_host != null && this.m_host.equals(obj.m_host))) && this.m_port == obj.m_port) {
                        if ((this.m_path == null && obj.m_path == null) || (this.m_path != null && obj.m_path != null && this.m_path.equals(obj.m_path))) {
                            if ((this.m_queryString == null && obj.m_queryString == null) || (this.m_queryString != null && obj.m_queryString != null && this.m_queryString.equals(obj.m_queryString))) {
                                if (this.m_fragment != null || obj.m_fragment != null) {
                                    if (this.m_fragment != null && obj.m_fragment != null && this.m_fragment.equals(obj.m_fragment)) {
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
        return false;
    }

    public String toString() {
        StringBuffer uriSpecString = new StringBuffer();
        if (this.m_scheme != null) {
            uriSpecString.append(this.m_scheme);
            uriSpecString.append(':');
        }
        uriSpecString.append(getSchemeSpecificPart());
        return uriSpecString.toString();
    }

    public boolean isGenericURI() {
        return this.m_host != null;
    }

    public boolean isAbsoluteURI() {
        return this.m_scheme != null;
    }

    public static boolean isConformantSchemeName(String p_scheme) {
        if (p_scheme == null || p_scheme.trim().length() == 0 || !isAlpha(p_scheme.charAt(0))) {
            return false;
        }
        int schemeLength = p_scheme.length();
        for (int i = 1; i < schemeLength; i++) {
            char testChar = p_scheme.charAt(i);
            if (!isSchemeCharacter(testChar)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isWellFormedAddress(String address) {
        int addrLength;
        if (address == null || (addrLength = address.length()) == 0) {
            return false;
        }
        if (address.startsWith("[")) {
            return isWellFormedIPv6Reference(address);
        }
        if (address.startsWith(".") || address.startsWith("-") || address.endsWith("-")) {
            return false;
        }
        int index = address.lastIndexOf(46);
        if (address.endsWith(".")) {
            index = address.substring(0, index).lastIndexOf(46);
        }
        if (index + 1 < addrLength && isDigit(address.charAt(index + 1))) {
            return isWellFormedIPv4Address(address);
        }
        if (addrLength > 255) {
            return false;
        }
        int labelCharCount = 0;
        for (int i = 0; i < addrLength; i++) {
            char testChar = address.charAt(i);
            if (testChar == '.') {
                if (!isAlphanum(address.charAt(i - 1))) {
                    return false;
                }
                if (i + 1 < addrLength && !isAlphanum(address.charAt(i + 1))) {
                    return false;
                }
                labelCharCount = 0;
            } else if ((!isAlphanum(testChar) && testChar != '-') || (labelCharCount = labelCharCount + 1) > 63) {
                return false;
            }
        }
        return true;
    }

    public static boolean isWellFormedIPv4Address(String address) {
        int addrLength = address.length();
        int numDots = 0;
        int numDigits = 0;
        for (int i = 0; i < addrLength; i++) {
            char testChar = address.charAt(i);
            if (testChar == '.') {
                if ((i > 0 && !isDigit(address.charAt(i - 1))) || (i + 1 < addrLength && !isDigit(address.charAt(i + 1)))) {
                    return false;
                }
                numDigits = 0;
                numDots++;
                if (numDots > 3) {
                    return false;
                }
            } else {
                if (!isDigit(testChar) || (numDigits = numDigits + 1) > 3) {
                    return false;
                }
                if (numDigits == 3) {
                    char first = address.charAt(i - 2);
                    char second = address.charAt(i - 1);
                    if (first >= MASK_UNRESERVED_MASK && (first != MASK_UNRESERVED_MASK || (second >= '5' && (second != '5' || testChar > '5')))) {
                        return false;
                    }
                } else {
                    continue;
                }
            }
        }
        return numDots == 3;
    }

    public static boolean isWellFormedIPv6Reference(String address) {
        int[] counter;
        int index;
        int addrLength = address.length();
        int end = addrLength - 1;
        if (addrLength <= 2 || address.charAt(0) != '[' || address.charAt(end) != ']' || (index = scanHexSequence(address, 1, end, (counter = new int[1]))) == -1) {
            return false;
        }
        if (index == end) {
            return counter[0] == 8;
        }
        if (index + 1 >= end || address.charAt(index) != MASK_USERINFO_CHARACTER) {
            return false;
        }
        if (address.charAt(index + 1) == MASK_USERINFO_CHARACTER) {
            int i = counter[0] + 1;
            counter[0] = i;
            if (i > 8) {
                return false;
            }
            int index2 = index + 2;
            if (index2 == end) {
                return true;
            }
            int prevCount = counter[0];
            int index3 = scanHexSequence(address, index2, end, counter);
            if (index3 != end) {
                if (index3 != -1) {
                    if (!isWellFormedIPv4Address(address.substring(counter[0] > prevCount ? index3 + 1 : index3, end))) {
                    }
                }
                return false;
            }
            return true;
        }
        int prevCount2 = counter[0];
        return prevCount2 == 6 && isWellFormedIPv4Address(address.substring(index + 1, end));
    }

    private static int scanHexSequence(String address, int index, int end, int[] counter) {
        int numDigits = 0;
        while (index < end) {
            char testChar = address.charAt(index);
            if (testChar == MASK_USERINFO_CHARACTER) {
                if (numDigits > 0) {
                    int i = counter[0] + 1;
                    counter[0] = i;
                    if (i > 8) {
                        return -1;
                    }
                }
                if (numDigits == 0 || (index + 1 < end && address.charAt(index + 1) == MASK_USERINFO_CHARACTER)) {
                    return index;
                }
                numDigits = 0;
            } else {
                if (!isHex(testChar)) {
                    if (testChar != '.' || numDigits >= 4 || numDigits <= 0 || counter[0] > 6) {
                        return -1;
                    }
                    int back = (index - numDigits) - 1;
                    return back >= index ? back : back + 1;
                }
                numDigits++;
                if (numDigits > 4) {
                    return -1;
                }
            }
            index++;
        }
        if (numDigits <= 0) {
            return -1;
        }
        int i2 = counter[0] + 1;
        counter[0] = i2;
        if (i2 <= 8) {
            return end;
        }
        return -1;
    }

    private static boolean isDigit(char p_char) {
        return p_char >= '0' && p_char <= '9';
    }

    private static boolean isHex(char p_char) {
        return p_char <= 'f' && (fgLookupTable[p_char] & 64) != 0;
    }

    private static boolean isAlpha(char p_char) {
        if (p_char < 'a' || p_char > 'z') {
            return p_char >= 'A' && p_char <= 'Z';
        }
        return true;
    }

    private static boolean isAlphanum(char p_char) {
        return p_char <= 'z' && (fgLookupTable[p_char] & 48) != 0;
    }

    private static boolean isReservedCharacter(char p_char) {
        return p_char <= ']' && (fgLookupTable[p_char] & 1) != 0;
    }

    private static boolean isUnreservedCharacter(char p_char) {
        return p_char <= '~' && (fgLookupTable[p_char] & 50) != 0;
    }

    private static boolean isURICharacter(char p_char) {
        return p_char <= '~' && (fgLookupTable[p_char] & 51) != 0;
    }

    private static boolean isSchemeCharacter(char p_char) {
        return p_char <= 'z' && (fgLookupTable[p_char] & 52) != 0;
    }

    private static boolean isUserinfoCharacter(char p_char) {
        return p_char <= 'z' && (fgLookupTable[p_char] & 58) != 0;
    }

    private static boolean isPathCharacter(char p_char) {
        return p_char <= '~' && (fgLookupTable[p_char] & 178) != 0;
    }

    private static boolean isURIString(String p_uric) {
        if (p_uric == null) {
            return false;
        }
        int end = p_uric.length();
        int i = 0;
        while (i < end) {
            char testChar = p_uric.charAt(i);
            if (testChar == '%') {
                if (i + 2 >= end || !isHex(p_uric.charAt(i + 1)) || !isHex(p_uric.charAt(i + 2))) {
                    return false;
                }
                i += 2;
            } else if (!isURICharacter(testChar)) {
                return false;
            }
            i++;
        }
        return true;
    }
}
