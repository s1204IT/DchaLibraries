package android.net;

import android.text.format.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;

public class UrlQuerySanitizer {
    private boolean mAllowUnregisteredParamaters;
    private boolean mPreferFirstRepeatedParameter;
    private static final ValueSanitizer sAllIllegal = new IllegalCharacterValueSanitizer(0);
    private static final ValueSanitizer sAllButNulLegal = new IllegalCharacterValueSanitizer(IllegalCharacterValueSanitizer.ALL_BUT_NUL_LEGAL);
    private static final ValueSanitizer sAllButWhitespaceLegal = new IllegalCharacterValueSanitizer(IllegalCharacterValueSanitizer.ALL_BUT_WHITESPACE_LEGAL);
    private static final ValueSanitizer sURLLegal = new IllegalCharacterValueSanitizer(404);
    private static final ValueSanitizer sUrlAndSpaceLegal = new IllegalCharacterValueSanitizer(405);
    private static final ValueSanitizer sAmpLegal = new IllegalCharacterValueSanitizer(128);
    private static final ValueSanitizer sAmpAndSpaceLegal = new IllegalCharacterValueSanitizer(129);
    private static final ValueSanitizer sSpaceLegal = new IllegalCharacterValueSanitizer(1);
    private static final ValueSanitizer sAllButNulAndAngleBracketsLegal = new IllegalCharacterValueSanitizer(1439);
    private final HashMap<String, ValueSanitizer> mSanitizers = new HashMap<>();
    private final HashMap<String, String> mEntries = new HashMap<>();
    private final ArrayList<ParameterValuePair> mEntriesList = new ArrayList<>();
    private ValueSanitizer mUnregisteredParameterValueSanitizer = getAllIllegal();

    public interface ValueSanitizer {
        String sanitize(String str);
    }

    public class ParameterValuePair {
        public String mParameter;
        public String mValue;

        public ParameterValuePair(String str, String str2) {
            this.mParameter = str;
            this.mValue = str2;
        }
    }

    public static class IllegalCharacterValueSanitizer implements ValueSanitizer {
        public static final int ALL_BUT_NUL_AND_ANGLE_BRACKETS_LEGAL = 1439;
        public static final int ALL_BUT_NUL_LEGAL = 1535;
        public static final int ALL_BUT_WHITESPACE_LEGAL = 1532;
        public static final int ALL_ILLEGAL = 0;
        public static final int ALL_OK = 2047;
        public static final int ALL_WHITESPACE_OK = 3;
        public static final int AMP_AND_SPACE_LEGAL = 129;
        public static final int AMP_LEGAL = 128;
        public static final int AMP_OK = 128;
        public static final int DQUOTE_OK = 8;
        public static final int GT_OK = 64;
        public static final int LT_OK = 32;
        public static final int NON_7_BIT_ASCII_OK = 4;
        public static final int NUL_OK = 512;
        public static final int OTHER_WHITESPACE_OK = 2;
        public static final int PCT_OK = 256;
        public static final int SCRIPT_URL_OK = 1024;
        public static final int SPACE_LEGAL = 1;
        public static final int SPACE_OK = 1;
        public static final int SQUOTE_OK = 16;
        public static final int URL_AND_SPACE_LEGAL = 405;
        public static final int URL_LEGAL = 404;
        private int mFlags;
        private static final String JAVASCRIPT_PREFIX = "javascript:";
        private static final String VBSCRIPT_PREFIX = "vbscript:";
        private static final int MIN_SCRIPT_PREFIX_LENGTH = Math.min(JAVASCRIPT_PREFIX.length(), VBSCRIPT_PREFIX.length());

        public IllegalCharacterValueSanitizer(int i) {
            this.mFlags = i;
        }

        @Override
        public String sanitize(String str) {
            if (str == null) {
                return null;
            }
            int length = str.length();
            if ((this.mFlags & 1024) != 0 && length >= MIN_SCRIPT_PREFIX_LENGTH) {
                String lowerCase = str.toLowerCase(Locale.ROOT);
                if (lowerCase.startsWith(JAVASCRIPT_PREFIX) || lowerCase.startsWith(VBSCRIPT_PREFIX)) {
                    return "";
                }
            }
            if ((this.mFlags & 3) == 0) {
                str = trimWhitespace(str);
                length = str.length();
            }
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                char cCharAt = str.charAt(i);
                if (!characterIsLegal(cCharAt)) {
                    if ((this.mFlags & 1) != 0) {
                        cCharAt = ' ';
                    } else {
                        cCharAt = '_';
                    }
                }
                sb.append(cCharAt);
            }
            return sb.toString();
        }

        private String trimWhitespace(String str) {
            int length = str.length() - 1;
            int i = 0;
            while (i <= length && isWhitespace(str.charAt(i))) {
                i++;
            }
            int i2 = length;
            while (i2 >= i && isWhitespace(str.charAt(i2))) {
                i2--;
            }
            if (i == 0 && i2 == length) {
                return str;
            }
            return str.substring(i, i2 + 1);
        }

        private boolean isWhitespace(char c) {
            if (c != ' ') {
                switch (c) {
                    case '\t':
                    case '\n':
                    case 11:
                    case '\f':
                    case '\r':
                        return true;
                    default:
                        return false;
                }
            }
            return true;
        }

        private boolean characterIsLegal(char c) {
            if (c == 0) {
                return (this.mFlags & 512) != 0;
            }
            if (c == ' ') {
                return (this.mFlags & 1) != 0;
            }
            if (c == '\"') {
                return (this.mFlags & 8) != 0;
            }
            if (c == '<') {
                return (this.mFlags & 32) != 0;
            }
            if (c == '>') {
                return (this.mFlags & 64) != 0;
            }
            switch (c) {
                case '\t':
                case '\n':
                case 11:
                case '\f':
                case '\r':
                    if ((this.mFlags & 2) != 0) {
                    }
                    break;
                default:
                    switch (c) {
                        case '%':
                            if ((this.mFlags & 256) != 0) {
                            }
                            break;
                        case '&':
                            if ((this.mFlags & 128) != 0) {
                            }
                            break;
                        case '\'':
                            if ((this.mFlags & 16) != 0) {
                            }
                            break;
                        default:
                            if ((c >= ' ' && c < 127) || (c >= 128 && (this.mFlags & 4) != 0)) {
                                break;
                            }
                            break;
                    }
                    break;
            }
            return true;
        }
    }

    public ValueSanitizer getUnregisteredParameterValueSanitizer() {
        return this.mUnregisteredParameterValueSanitizer;
    }

    public void setUnregisteredParameterValueSanitizer(ValueSanitizer valueSanitizer) {
        this.mUnregisteredParameterValueSanitizer = valueSanitizer;
    }

    public static final ValueSanitizer getAllIllegal() {
        return sAllIllegal;
    }

    public static final ValueSanitizer getAllButNulLegal() {
        return sAllButNulLegal;
    }

    public static final ValueSanitizer getAllButWhitespaceLegal() {
        return sAllButWhitespaceLegal;
    }

    public static final ValueSanitizer getUrlLegal() {
        return sURLLegal;
    }

    public static final ValueSanitizer getUrlAndSpaceLegal() {
        return sUrlAndSpaceLegal;
    }

    public static final ValueSanitizer getAmpLegal() {
        return sAmpLegal;
    }

    public static final ValueSanitizer getAmpAndSpaceLegal() {
        return sAmpAndSpaceLegal;
    }

    public static final ValueSanitizer getSpaceLegal() {
        return sSpaceLegal;
    }

    public static final ValueSanitizer getAllButNulAndAngleBracketsLegal() {
        return sAllButNulAndAngleBracketsLegal;
    }

    public UrlQuerySanitizer() {
    }

    public UrlQuerySanitizer(String str) {
        setAllowUnregisteredParamaters(true);
        parseUrl(str);
    }

    public void parseUrl(String str) {
        String strSubstring;
        int iIndexOf = str.indexOf(63);
        if (iIndexOf >= 0) {
            strSubstring = str.substring(iIndexOf + 1);
        } else {
            strSubstring = "";
        }
        parseQuery(strSubstring);
    }

    public void parseQuery(String str) {
        clear();
        StringTokenizer stringTokenizer = new StringTokenizer(str, "&");
        while (stringTokenizer.hasMoreElements()) {
            String strNextToken = stringTokenizer.nextToken();
            if (strNextToken.length() > 0) {
                int iIndexOf = strNextToken.indexOf(61);
                if (iIndexOf < 0) {
                    parseEntry(strNextToken, "");
                } else {
                    parseEntry(strNextToken.substring(0, iIndexOf), strNextToken.substring(iIndexOf + 1));
                }
            }
        }
    }

    public Set<String> getParameterSet() {
        return this.mEntries.keySet();
    }

    public List<ParameterValuePair> getParameterList() {
        return this.mEntriesList;
    }

    public boolean hasParameter(String str) {
        return this.mEntries.containsKey(str);
    }

    public String getValue(String str) {
        return this.mEntries.get(str);
    }

    public void registerParameter(String str, ValueSanitizer valueSanitizer) {
        if (valueSanitizer == null) {
            this.mSanitizers.remove(str);
        }
        this.mSanitizers.put(str, valueSanitizer);
    }

    public void registerParameters(String[] strArr, ValueSanitizer valueSanitizer) {
        for (String str : strArr) {
            this.mSanitizers.put(str, valueSanitizer);
        }
    }

    public void setAllowUnregisteredParamaters(boolean z) {
        this.mAllowUnregisteredParamaters = z;
    }

    public boolean getAllowUnregisteredParamaters() {
        return this.mAllowUnregisteredParamaters;
    }

    public void setPreferFirstRepeatedParameter(boolean z) {
        this.mPreferFirstRepeatedParameter = z;
    }

    public boolean getPreferFirstRepeatedParameter() {
        return this.mPreferFirstRepeatedParameter;
    }

    protected void parseEntry(String str, String str2) {
        String strUnescape = unescape(str);
        ValueSanitizer effectiveValueSanitizer = getEffectiveValueSanitizer(strUnescape);
        if (effectiveValueSanitizer == null) {
            return;
        }
        addSanitizedEntry(strUnescape, effectiveValueSanitizer.sanitize(unescape(str2)));
    }

    protected void addSanitizedEntry(String str, String str2) {
        this.mEntriesList.add(new ParameterValuePair(str, str2));
        if (this.mPreferFirstRepeatedParameter && this.mEntries.containsKey(str)) {
            return;
        }
        this.mEntries.put(str, str2);
    }

    public ValueSanitizer getValueSanitizer(String str) {
        return this.mSanitizers.get(str);
    }

    public ValueSanitizer getEffectiveValueSanitizer(String str) {
        ValueSanitizer valueSanitizer = getValueSanitizer(str);
        if (valueSanitizer == null && this.mAllowUnregisteredParamaters) {
            return getUnregisteredParameterValueSanitizer();
        }
        return valueSanitizer;
    }

    public String unescape(String str) {
        int i;
        int iIndexOf = str.indexOf(37);
        if (iIndexOf < 0 && (iIndexOf = str.indexOf(43)) < 0) {
            return str;
        }
        int length = str.length();
        StringBuilder sb = new StringBuilder(length);
        sb.append(str.substring(0, iIndexOf));
        while (iIndexOf < length) {
            char cCharAt = str.charAt(iIndexOf);
            if (cCharAt == '+') {
                cCharAt = ' ';
            } else if (cCharAt == '%' && (i = iIndexOf + 2) < length) {
                char cCharAt2 = str.charAt(iIndexOf + 1);
                char cCharAt3 = str.charAt(i);
                if (isHexDigit(cCharAt2) && isHexDigit(cCharAt3)) {
                    cCharAt = (char) ((decodeHexDigit(cCharAt2) * 16) + decodeHexDigit(cCharAt3));
                    iIndexOf = i;
                }
            }
            sb.append(cCharAt);
            iIndexOf++;
        }
        return sb.toString();
    }

    protected boolean isHexDigit(char c) {
        return decodeHexDigit(c) >= 0;
    }

    protected int decodeHexDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'F') {
            return (c - DateFormat.CAPITAL_AM_PM) + 10;
        }
        if (c >= 'a' && c <= 'f') {
            return (c - DateFormat.AM_PM) + 10;
        }
        return -1;
    }

    protected void clear() {
        this.mEntries.clear();
        this.mEntriesList.clear();
    }
}
