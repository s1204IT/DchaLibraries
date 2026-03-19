package sun.security.util;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import sun.net.www.ParseUtil;

public class PropertyExpander {

    public static class ExpandException extends GeneralSecurityException {
        private static final long serialVersionUID = -7941948581406161702L;

        public ExpandException(String str) {
            super(str);
        }
    }

    public static String expand(String str) throws ExpandException {
        return expand(str, false);
    }

    public static String expand(String str, boolean z) throws ExpandException {
        int i;
        if (str == null) {
            return null;
        }
        int i2 = 0;
        int iIndexOf = str.indexOf("${", 0);
        if (iIndexOf == -1) {
            return str;
        }
        StringBuffer stringBuffer = new StringBuffer(str.length());
        int length = str.length();
        while (true) {
            if (iIndexOf >= length) {
                break;
            }
            if (iIndexOf > i2) {
                stringBuffer.append(str.substring(i2, iIndexOf));
            }
            int i3 = iIndexOf + 2;
            if (i3 < length && str.charAt(i3) == '{') {
                int iIndexOf2 = str.indexOf("}}", i3);
                if (iIndexOf2 == -1 || iIndexOf2 + 2 == length) {
                    break;
                }
                i = iIndexOf2 + 1;
                stringBuffer.append(str.substring(iIndexOf, i + 1));
                i2 = i + 1;
                iIndexOf = str.indexOf("${", i2);
                if (iIndexOf != -1) {
                }
            } else {
                int i4 = i3;
                while (i4 < length && str.charAt(i4) != '}') {
                    i4++;
                }
                if (i4 == length) {
                    stringBuffer.append(str.substring(iIndexOf, i4));
                    break;
                }
                String strSubstring = str.substring(i3, i4);
                if (strSubstring.equals("/")) {
                    stringBuffer.append(File.separatorChar);
                } else {
                    String property = System.getProperty(strSubstring);
                    if (property != null) {
                        if (z) {
                            try {
                                if (stringBuffer.length() > 0 || !new URI(property).isAbsolute()) {
                                    property = ParseUtil.encodePath(property);
                                }
                            } catch (URISyntaxException e) {
                                property = ParseUtil.encodePath(property);
                            }
                        }
                        stringBuffer.append(property);
                    } else {
                        throw new ExpandException("unable to expand property " + strSubstring);
                    }
                }
                i = i4;
                i2 = i + 1;
                iIndexOf = str.indexOf("${", i2);
                if (iIndexOf != -1) {
                    if (i2 < length) {
                        stringBuffer.append(str.substring(i2, length));
                    }
                }
            }
        }
        return stringBuffer.toString();
    }
}
