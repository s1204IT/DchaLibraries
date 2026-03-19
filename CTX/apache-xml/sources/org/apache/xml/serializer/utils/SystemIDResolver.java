package org.apache.xml.serializer.utils;

import java.io.File;
import javax.xml.transform.TransformerException;
import org.apache.xml.serializer.utils.URI;
import org.apache.xpath.compiler.PsuedoNames;

public final class SystemIDResolver {
    public static String getAbsoluteURIFromRelative(String str) {
        String absolutePathFromRelativePath;
        String str2;
        if (str == null || str.length() == 0) {
            return "";
        }
        if (!isAbsolutePath(str)) {
            try {
                absolutePathFromRelativePath = getAbsolutePathFromRelativePath(str);
            } catch (SecurityException e) {
                return "file:" + str;
            }
        } else {
            absolutePathFromRelativePath = str;
        }
        if (absolutePathFromRelativePath != null) {
            if (absolutePathFromRelativePath.startsWith(File.separator)) {
                str2 = "file://" + absolutePathFromRelativePath;
            } else {
                str2 = "file:///" + absolutePathFromRelativePath;
            }
        } else {
            str2 = "file:" + str;
        }
        return replaceChars(str2);
    }

    private static String getAbsolutePathFromRelativePath(String str) {
        return new File(str).getAbsolutePath();
    }

    public static boolean isAbsoluteURI(String str) {
        if (isWindowsAbsolutePath(str)) {
            return false;
        }
        int iIndexOf = str.indexOf(35);
        int iIndexOf2 = str.indexOf(63);
        int iIndexOf3 = str.indexOf(47);
        int iIndexOf4 = str.indexOf(58);
        int length = str.length() - 1;
        if (iIndexOf > 0) {
            length = iIndexOf;
        }
        if (iIndexOf2 > 0 && iIndexOf2 < length) {
            length = iIndexOf2;
        }
        if (iIndexOf3 > 0 && iIndexOf3 < length) {
            length = iIndexOf3;
        }
        return iIndexOf4 > 0 && iIndexOf4 < length;
    }

    public static boolean isAbsolutePath(String str) {
        if (str == null) {
            return false;
        }
        return new File(str).isAbsolute();
    }

    private static boolean isWindowsAbsolutePath(String str) {
        return isAbsolutePath(str) && str.length() > 2 && str.charAt(1) == ':' && Character.isLetter(str.charAt(0)) && (str.charAt(2) == '\\' || str.charAt(2) == '/');
    }

    private static String replaceChars(String str) {
        StringBuffer stringBuffer = new StringBuffer(str);
        int length = stringBuffer.length();
        int i = 0;
        while (i < length) {
            char cCharAt = stringBuffer.charAt(i);
            if (cCharAt == ' ') {
                stringBuffer.setCharAt(i, '%');
                stringBuffer.insert(i + 1, "20");
                length += 2;
                i += 2;
            } else if (cCharAt == '\\') {
                stringBuffer.setCharAt(i, '/');
            }
            i++;
        }
        return stringBuffer.toString();
    }

    public static String getAbsoluteURI(String str) {
        int iIndexOf;
        if (isAbsoluteURI(str)) {
            if (str.startsWith("file:")) {
                String strSubstring = str.substring(5);
                if (strSubstring != null && strSubstring.startsWith(PsuedoNames.PSEUDONAME_ROOT)) {
                    if ((strSubstring.startsWith("///") || !strSubstring.startsWith("//")) && (iIndexOf = str.indexOf(58, 5)) > 0) {
                        int i = iIndexOf - 1;
                        String strSubstring2 = str.substring(i);
                        try {
                            if (!isAbsolutePath(strSubstring2)) {
                                str = str.substring(0, i) + getAbsolutePathFromRelativePath(strSubstring2);
                            }
                        } catch (SecurityException e) {
                            return str;
                        }
                    }
                    return replaceChars(str);
                }
                return getAbsoluteURIFromRelative(str.substring(5));
            }
            return str;
        }
        return getAbsoluteURIFromRelative(str);
    }

    public static String getAbsoluteURI(String str, String str2) throws TransformerException {
        if (str2 == null) {
            return getAbsoluteURI(str);
        }
        try {
            return replaceChars(new URI(new URI(getAbsoluteURI(str2)), str).toString());
        } catch (URI.MalformedURIException e) {
            throw new TransformerException(e);
        }
    }
}
