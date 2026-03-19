package android.webkit;

import android.content.ClipDescription;
import android.net.ParseException;
import android.net.Uri;
import android.net.WebAddress;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class URLUtil {
    static final String ASSET_BASE = "file:///android_asset/";
    static final String CONTENT_BASE = "content:";
    private static final Pattern CONTENT_DISPOSITION_PATTERN = Pattern.compile("attachment;\\s*filename\\s*=\\s*(\"?)([^\"]*)\\1\\s*$", 2);
    static final String FILE_BASE = "file:";
    private static final String LOGTAG = "webkit";
    static final String PROXY_BASE = "file:///cookieless_proxy/";
    static final String RESOURCE_BASE = "file:///android_res/";
    private static final boolean TRACE = false;

    public static String guessUrl(String str) {
        String strSubstring;
        if (str.length() == 0 || str.startsWith("about:") || str.startsWith("data:") || str.startsWith(FILE_BASE) || str.startsWith("javascript:")) {
            return str;
        }
        if (str.endsWith(".")) {
            strSubstring = str.substring(0, str.length() - 1);
        } else {
            strSubstring = str;
        }
        try {
            WebAddress webAddress = new WebAddress(strSubstring);
            if (webAddress.getHost().indexOf(46) == -1) {
                webAddress.setHost("www." + webAddress.getHost() + ".com");
            }
            return webAddress.toString();
        } catch (ParseException e) {
            return str;
        }
    }

    public static String composeSearchUrl(String str, String str2, String str3) {
        int iIndexOf = str2.indexOf(str3);
        if (iIndexOf < 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(str2.substring(0, iIndexOf));
        try {
            sb.append(URLEncoder.encode(str, "utf-8"));
            sb.append(str2.substring(iIndexOf + str3.length()));
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static byte[] decode(byte[] bArr) throws IllegalArgumentException {
        if (bArr.length == 0) {
            return new byte[0];
        }
        byte[] bArr2 = new byte[bArr.length];
        int i = 0;
        int i2 = 0;
        while (i < bArr.length) {
            byte hex = bArr[i];
            if (hex == 37) {
                if (bArr.length - i > 2) {
                    int hex2 = parseHex(bArr[i + 1]) * 16;
                    i += 2;
                    hex = (byte) (hex2 + parseHex(bArr[i]));
                } else {
                    throw new IllegalArgumentException("Invalid format");
                }
            }
            bArr2[i2] = hex;
            i++;
            i2++;
        }
        byte[] bArr3 = new byte[i2];
        System.arraycopy(bArr2, 0, bArr3, 0, i2);
        return bArr3;
    }

    static boolean verifyURLEncoding(String str) {
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int iIndexOf = str.indexOf(37);
        while (iIndexOf >= 0 && iIndexOf < length) {
            if (iIndexOf >= length - 2) {
                return false;
            }
            int i = iIndexOf + 1;
            try {
                parseHex((byte) str.charAt(i));
                int i2 = i + 1;
                parseHex((byte) str.charAt(i2));
                iIndexOf = str.indexOf(37, i2 + 1);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return true;
    }

    private static int parseHex(byte b) {
        if (b >= 48 && b <= 57) {
            return b - 48;
        }
        if (b >= 65 && b <= 70) {
            return (b - 65) + 10;
        }
        if (b >= 97 && b <= 102) {
            return (b - 97) + 10;
        }
        throw new IllegalArgumentException("Invalid hex char '" + ((int) b) + "'");
    }

    public static boolean isAssetUrl(String str) {
        return str != null && str.startsWith(ASSET_BASE);
    }

    public static boolean isResourceUrl(String str) {
        return str != null && str.startsWith(RESOURCE_BASE);
    }

    @Deprecated
    public static boolean isCookielessProxyUrl(String str) {
        return str != null && str.startsWith(PROXY_BASE);
    }

    public static boolean isFileUrl(String str) {
        return (str == null || !str.startsWith(FILE_BASE) || str.startsWith(ASSET_BASE) || str.startsWith(PROXY_BASE)) ? false : true;
    }

    public static boolean isAboutUrl(String str) {
        return str != null && str.startsWith("about:");
    }

    public static boolean isDataUrl(String str) {
        return str != null && str.startsWith("data:");
    }

    public static boolean isJavaScriptUrl(String str) {
        return str != null && str.startsWith("javascript:");
    }

    public static boolean isHttpUrl(String str) {
        return str != null && str.length() > 6 && str.substring(0, 7).equalsIgnoreCase("http://");
    }

    public static boolean isHttpsUrl(String str) {
        return str != null && str.length() > 7 && str.substring(0, 8).equalsIgnoreCase("https://");
    }

    public static boolean isNetworkUrl(String str) {
        if (str == null || str.length() == 0) {
            return false;
        }
        return isHttpUrl(str) || isHttpsUrl(str);
    }

    public static boolean isContentUrl(String str) {
        return str != null && str.startsWith(CONTENT_BASE);
    }

    public static boolean isValidUrl(String str) {
        if (str == null || str.length() == 0) {
            return false;
        }
        return isAssetUrl(str) || isResourceUrl(str) || isFileUrl(str) || isAboutUrl(str) || isHttpUrl(str) || isHttpsUrl(str) || isJavaScriptUrl(str) || isContentUrl(str);
    }

    public static String stripAnchor(String str) {
        int iIndexOf = str.indexOf(35);
        if (iIndexOf != -1) {
            return str.substring(0, iIndexOf);
        }
        return str;
    }

    public static final String guessFileName(String str, String str2, String str3) {
        String strSubstring;
        String strDecode;
        int iLastIndexOf;
        int iLastIndexOf2;
        String strSubstring2 = null;
        if (str2 != null) {
            strSubstring = parseContentDisposition(str2);
            if (strSubstring != null && (iLastIndexOf2 = strSubstring.lastIndexOf(47) + 1) > 0) {
                strSubstring = strSubstring.substring(iLastIndexOf2);
            }
        } else {
            strSubstring = null;
        }
        if (strSubstring == null && (strDecode = Uri.decode(str)) != null) {
            int iIndexOf = strDecode.indexOf(63);
            if (iIndexOf > 0) {
                strDecode = strDecode.substring(0, iIndexOf);
            }
            if (!strDecode.endsWith("/") && (iLastIndexOf = strDecode.lastIndexOf(47) + 1) > 0) {
                strSubstring = strDecode.substring(iLastIndexOf);
            }
        }
        if (strSubstring == null) {
            strSubstring = "downloadfile";
        }
        int iIndexOf2 = strSubstring.indexOf(46);
        if (iIndexOf2 < 0) {
            if (str3 != null && (strSubstring2 = MimeTypeMap.getSingleton().getExtensionFromMimeType(str3)) != null) {
                strSubstring2 = "." + strSubstring2;
            }
            if (strSubstring2 == null) {
                if (str3 != null && str3.toLowerCase(Locale.ROOT).startsWith("text/")) {
                    if (str3.equalsIgnoreCase(ClipDescription.MIMETYPE_TEXT_HTML)) {
                        strSubstring2 = ".html";
                    } else {
                        strSubstring2 = ".txt";
                    }
                } else {
                    strSubstring2 = ".bin";
                }
            }
        } else {
            if (str3 != null) {
                String mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(strSubstring.substring(strSubstring.lastIndexOf(46) + 1));
                if (mimeTypeFromExtension != null && !mimeTypeFromExtension.equalsIgnoreCase(str3) && (strSubstring2 = MimeTypeMap.getSingleton().getExtensionFromMimeType(str3)) != null) {
                    strSubstring2 = "." + strSubstring2;
                }
            }
            if (strSubstring2 == null) {
                strSubstring2 = strSubstring.substring(iIndexOf2);
            }
            strSubstring = strSubstring.substring(0, iIndexOf2);
        }
        return strSubstring + strSubstring2;
    }

    static String parseContentDisposition(String str) {
        try {
            Matcher matcher = CONTENT_DISPOSITION_PATTERN.matcher(str);
            if (matcher.find()) {
                return matcher.group(2);
            }
            return null;
        } catch (IllegalStateException e) {
            return null;
        }
    }
}
