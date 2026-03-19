package sun.net.www.protocol.jar;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import sun.net.www.ParseUtil;

public class Handler extends URLStreamHandler {
    private static final String separator = "!/";

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return new JarURLConnection(url, this);
    }

    private static int indexOfBangSlash(String str) {
        int length = str.length();
        while (true) {
            int iLastIndexOf = str.lastIndexOf(33, length);
            if (iLastIndexOf == -1) {
                return -1;
            }
            if (iLastIndexOf != str.length() - 1) {
                int i = iLastIndexOf + 1;
                if (str.charAt(i) == '/') {
                    return i;
                }
            }
            length = iLastIndexOf - 1;
        }
    }

    @Override
    protected boolean sameFile(URL url, URL url2) {
        if (!url.getProtocol().equals("jar") || !url2.getProtocol().equals("jar")) {
            return false;
        }
        String file = url.getFile();
        String file2 = url2.getFile();
        int iIndexOf = file.indexOf(separator);
        int iIndexOf2 = file2.indexOf(separator);
        if (iIndexOf == -1 || iIndexOf2 == -1) {
            return super.sameFile(url, url2);
        }
        if (!file.substring(iIndexOf + 2).equals(file2.substring(iIndexOf2 + 2))) {
            return false;
        }
        try {
            return super.sameFile(new URL(file.substring(0, iIndexOf)), new URL(file2.substring(0, iIndexOf2)));
        } catch (MalformedURLException e) {
            return super.sameFile(url, url2);
        }
    }

    @Override
    protected int hashCode(URL url) {
        int iHashCode;
        int iHashCode2;
        String protocol = url.getProtocol();
        if (protocol != null) {
            iHashCode = protocol.hashCode() + 0;
        } else {
            iHashCode = 0;
        }
        String file = url.getFile();
        int iIndexOf = file.indexOf(separator);
        if (iIndexOf == -1) {
            return iHashCode + file.hashCode();
        }
        String strSubstring = file.substring(0, iIndexOf);
        try {
            iHashCode2 = iHashCode + new URL(strSubstring).hashCode();
        } catch (MalformedURLException e) {
            iHashCode2 = iHashCode + strSubstring.hashCode();
        }
        return iHashCode2 + file.substring(iIndexOf + 2).hashCode();
    }

    @Override
    protected void parseURL(URL url, String str, int i, int i2) {
        String str2;
        boolean zEqualsIgnoreCase;
        String str3;
        String absoluteSpec;
        int iIndexOf = str.indexOf(35, i2);
        boolean z = iIndexOf == i;
        if (iIndexOf > -1) {
            String strSubstring = str.substring(iIndexOf + 1, str.length());
            file = z ? url.getFile() : null;
            str2 = strSubstring;
        } else {
            str2 = null;
        }
        if (str.length() >= 4) {
            zEqualsIgnoreCase = str.substring(0, 4).equalsIgnoreCase("jar:");
        } else {
            zEqualsIgnoreCase = false;
        }
        String strSubstring2 = str.substring(i, i2);
        if (zEqualsIgnoreCase) {
            absoluteSpec = parseAbsoluteSpec(strSubstring2);
        } else if (!z) {
            String contextSpec = parseContextSpec(url, strSubstring2);
            int iIndexOfBangSlash = indexOfBangSlash(contextSpec);
            absoluteSpec = contextSpec.substring(0, iIndexOfBangSlash) + new ParseUtil().canonizeString(contextSpec.substring(iIndexOfBangSlash));
        } else {
            str3 = file;
            setURL(url, "jar", "", -1, str3, str2);
        }
        str3 = absoluteSpec;
        setURL(url, "jar", "", -1, str3, str2);
    }

    private String parseAbsoluteSpec(String str) {
        int iIndexOfBangSlash = indexOfBangSlash(str);
        if (iIndexOfBangSlash == -1) {
            throw new NullPointerException("no !/ in spec");
        }
        try {
            new URL(str.substring(0, iIndexOfBangSlash - 1));
            return str;
        } catch (MalformedURLException e) {
            throw new NullPointerException("invalid url: " + str + " (" + ((Object) e) + ")");
        }
    }

    private String parseContextSpec(URL url, String str) {
        String file = url.getFile();
        if (str.startsWith("/")) {
            int iIndexOfBangSlash = indexOfBangSlash(file);
            if (iIndexOfBangSlash == -1) {
                throw new NullPointerException("malformed context url:" + ((Object) url) + ": no !/");
            }
            file = file.substring(0, iIndexOfBangSlash);
        }
        if (!file.endsWith("/") && !str.startsWith("/")) {
            int iLastIndexOf = file.lastIndexOf(47);
            if (iLastIndexOf == -1) {
                throw new NullPointerException("malformed context url:" + ((Object) url));
            }
            file = file.substring(0, iLastIndexOf + 1);
        }
        return file + str;
    }
}
