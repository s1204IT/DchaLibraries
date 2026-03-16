package org.apache.xml.serializer.utils;

import java.io.File;
import javax.xml.transform.TransformerException;
import org.apache.xml.serializer.utils.URI;
import org.apache.xpath.compiler.PsuedoNames;

public final class SystemIDResolver {
    public static String getAbsoluteURIFromRelative(String localPath) {
        String urlString;
        if (localPath == null || localPath.length() == 0) {
            return "";
        }
        String absolutePath = localPath;
        if (!isAbsolutePath(localPath)) {
            try {
                absolutePath = getAbsolutePathFromRelativePath(localPath);
            } catch (SecurityException e) {
                return "file:" + localPath;
            }
        }
        if (absolutePath != null) {
            if (absolutePath.startsWith(File.separator)) {
                urlString = "file://" + absolutePath;
            } else {
                urlString = "file:///" + absolutePath;
            }
        } else {
            urlString = "file:" + localPath;
        }
        return replaceChars(urlString);
    }

    private static String getAbsolutePathFromRelativePath(String relativePath) {
        return new File(relativePath).getAbsolutePath();
    }

    public static boolean isAbsoluteURI(String systemId) {
        if (isWindowsAbsolutePath(systemId)) {
            return false;
        }
        int fragmentIndex = systemId.indexOf(35);
        int queryIndex = systemId.indexOf(63);
        int slashIndex = systemId.indexOf(47);
        int colonIndex = systemId.indexOf(58);
        int index = systemId.length() - 1;
        if (fragmentIndex > 0) {
            index = fragmentIndex;
        }
        if (queryIndex > 0 && queryIndex < index) {
            index = queryIndex;
        }
        if (slashIndex > 0 && slashIndex < index) {
            index = slashIndex;
        }
        return colonIndex > 0 && colonIndex < index;
    }

    public static boolean isAbsolutePath(String systemId) {
        if (systemId == null) {
            return false;
        }
        File file = new File(systemId);
        return file.isAbsolute();
    }

    private static boolean isWindowsAbsolutePath(String systemId) {
        if (isAbsolutePath(systemId) && systemId.length() > 2 && systemId.charAt(1) == ':' && Character.isLetter(systemId.charAt(0))) {
            return systemId.charAt(2) == '\\' || systemId.charAt(2) == '/';
        }
        return false;
    }

    private static String replaceChars(String str) {
        StringBuffer buf = new StringBuffer(str);
        int length = buf.length();
        int i = 0;
        while (i < length) {
            char currentChar = buf.charAt(i);
            if (currentChar == ' ') {
                buf.setCharAt(i, '%');
                buf.insert(i + 1, "20");
                length += 2;
                i += 2;
            } else if (currentChar == '\\') {
                buf.setCharAt(i, '/');
            }
            i++;
        }
        return buf.toString();
    }

    public static String getAbsoluteURI(String systemId) {
        int secondColonIndex;
        String absoluteURI = systemId;
        if (isAbsoluteURI(systemId)) {
            if (systemId.startsWith("file:")) {
                String str = systemId.substring(5);
                if (str != null && str.startsWith(PsuedoNames.PSEUDONAME_ROOT)) {
                    if ((str.startsWith("///") || !str.startsWith("//")) && (secondColonIndex = systemId.indexOf(58, 5)) > 0) {
                        String localPath = systemId.substring(secondColonIndex - 1);
                        try {
                            if (!isAbsolutePath(localPath)) {
                                absoluteURI = systemId.substring(0, secondColonIndex - 1) + getAbsolutePathFromRelativePath(localPath);
                            }
                        } catch (SecurityException e) {
                            return systemId;
                        }
                    }
                    return replaceChars(absoluteURI);
                }
                return getAbsoluteURIFromRelative(systemId.substring(5));
            }
            return systemId;
        }
        return getAbsoluteURIFromRelative(systemId);
    }

    public static String getAbsoluteURI(String urlString, String base) throws TransformerException {
        if (base == null) {
            return getAbsoluteURI(urlString);
        }
        String absoluteBase = getAbsoluteURI(base);
        try {
            URI baseURI = new URI(absoluteBase);
            URI uri = new URI(baseURI, urlString);
            return replaceChars(uri.toString());
        } catch (URI.MalformedURIException mue) {
            throw new TransformerException(mue);
        }
    }
}
