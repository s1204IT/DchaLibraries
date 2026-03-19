package org.apache.xml.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import org.apache.xml.serializer.utils.WrappedRuntimeException;

public final class Encodings {
    static final String DEFAULT_MIME_ENCODING = "UTF-8";
    private static final String ENCODINGS_FILE = SerializerBase.PKG_PATH + "/Encodings.properties";
    private static final Hashtable _encodingTableKeyJava = new Hashtable();
    private static final Hashtable _encodingTableKeyMime = new Hashtable();
    private static final EncodingInfo[] _encodings = loadEncodingInfo();

    static Writer getWriter(OutputStream outputStream, String str) throws UnsupportedEncodingException {
        for (int i = 0; i < _encodings.length; i++) {
            if (_encodings[i].name.equalsIgnoreCase(str)) {
                try {
                    return new OutputStreamWriter(outputStream, _encodings[i].javaName);
                } catch (UnsupportedEncodingException e) {
                } catch (IllegalArgumentException e2) {
                }
            }
        }
        try {
            return new OutputStreamWriter(outputStream, str);
        } catch (IllegalArgumentException e3) {
            throw new UnsupportedEncodingException(str);
        }
    }

    static EncodingInfo getEncodingInfo(String str) {
        String upperCaseFast = toUpperCaseFast(str);
        EncodingInfo encodingInfo = (EncodingInfo) _encodingTableKeyJava.get(upperCaseFast);
        if (encodingInfo == null) {
            encodingInfo = (EncodingInfo) _encodingTableKeyMime.get(upperCaseFast);
        }
        if (encodingInfo == null) {
            return new EncodingInfo(null, null, (char) 0);
        }
        return encodingInfo;
    }

    public static boolean isRecognizedEncoding(String str) {
        String upperCase = str.toUpperCase();
        EncodingInfo encodingInfo = (EncodingInfo) _encodingTableKeyJava.get(upperCase);
        if (encodingInfo == null) {
            encodingInfo = (EncodingInfo) _encodingTableKeyMime.get(upperCase);
        }
        if (encodingInfo != null) {
            return true;
        }
        return false;
    }

    private static String toUpperCaseFast(String str) {
        int length = str.length();
        char[] cArr = new char[length];
        boolean z = false;
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if ('a' <= cCharAt && cCharAt <= 'z') {
                cCharAt = (char) (cCharAt - ' ');
                z = true;
            }
            cArr[i] = cCharAt;
        }
        if (z) {
            return String.valueOf(cArr);
        }
        return str;
    }

    static String getMimeEncoding(String str) {
        String strConvertJava2MimeEncoding;
        if (str == null) {
            try {
                String property = System.getProperty("file.encoding", "UTF8");
                if (property != null) {
                    if (property.equalsIgnoreCase("Cp1252") || property.equalsIgnoreCase("ISO8859_1") || property.equalsIgnoreCase("8859_1") || property.equalsIgnoreCase("UTF8")) {
                        strConvertJava2MimeEncoding = DEFAULT_MIME_ENCODING;
                    } else {
                        strConvertJava2MimeEncoding = convertJava2MimeEncoding(property);
                    }
                    if (strConvertJava2MimeEncoding == null) {
                        strConvertJava2MimeEncoding = DEFAULT_MIME_ENCODING;
                    }
                    return strConvertJava2MimeEncoding;
                }
                return DEFAULT_MIME_ENCODING;
            } catch (SecurityException e) {
                return DEFAULT_MIME_ENCODING;
            }
        }
        return convertJava2MimeEncoding(str);
    }

    private static String convertJava2MimeEncoding(String str) {
        EncodingInfo encodingInfo = (EncodingInfo) _encodingTableKeyJava.get(toUpperCaseFast(str));
        if (encodingInfo != null) {
            return encodingInfo.name;
        }
        return str;
    }

    public static String convertMime2JavaEncoding(String str) {
        for (int i = 0; i < _encodings.length; i++) {
            if (_encodings[i].name.equalsIgnoreCase(str)) {
                return _encodings[i].javaName;
            }
        }
        return str;
    }

    private static EncodingInfo[] loadEncodingInfo() {
        char cIntValue;
        try {
            InputStream resourceAsStream = SecuritySupport.getInstance().getResourceAsStream(ObjectFactory.findClassLoader(), ENCODINGS_FILE);
            Properties properties = new Properties();
            if (resourceAsStream != null) {
                properties.load(resourceAsStream);
                resourceAsStream.close();
            }
            int size = properties.size();
            ArrayList arrayList = new ArrayList();
            Enumeration enumerationKeys = properties.keys();
            for (int i = 0; i < size; i++) {
                String str = (String) enumerationKeys.nextElement();
                String property = properties.getProperty(str);
                int iLengthOfMimeNames = lengthOfMimeNames(property);
                if (iLengthOfMimeNames != 0) {
                    try {
                        cIntValue = (char) Integer.decode(property.substring(iLengthOfMimeNames).trim()).intValue();
                    } catch (NumberFormatException e) {
                        cIntValue = 0;
                    }
                    StringTokenizer stringTokenizer = new StringTokenizer(property.substring(0, iLengthOfMimeNames), ",");
                    boolean z = true;
                    while (stringTokenizer.hasMoreTokens()) {
                        String strNextToken = stringTokenizer.nextToken();
                        EncodingInfo encodingInfo = new EncodingInfo(strNextToken, str, cIntValue);
                        arrayList.add(encodingInfo);
                        _encodingTableKeyMime.put(strNextToken.toUpperCase(), encodingInfo);
                        if (z) {
                            _encodingTableKeyJava.put(str.toUpperCase(), encodingInfo);
                        }
                        z = false;
                    }
                }
            }
            EncodingInfo[] encodingInfoArr = new EncodingInfo[arrayList.size()];
            arrayList.toArray(encodingInfoArr);
            return encodingInfoArr;
        } catch (MalformedURLException e2) {
            throw new WrappedRuntimeException(e2);
        } catch (IOException e3) {
            throw new WrappedRuntimeException(e3);
        }
    }

    private static int lengthOfMimeNames(String str) {
        int iIndexOf = str.indexOf(32);
        if (iIndexOf < 0) {
            return str.length();
        }
        return iIndexOf;
    }

    static boolean isHighUTF16Surrogate(char c) {
        return 55296 <= c && c <= 56319;
    }

    static boolean isLowUTF16Surrogate(char c) {
        return 56320 <= c && c <= 57343;
    }

    static int toCodePoint(char c, char c2) {
        return ((c - 55296) << 10) + (c2 - 56320) + 65536;
    }

    static int toCodePoint(char c) {
        return c;
    }

    public static char getHighChar(String str) {
        String upperCaseFast = toUpperCaseFast(str);
        EncodingInfo encodingInfo = (EncodingInfo) _encodingTableKeyJava.get(upperCaseFast);
        if (encodingInfo == null) {
            encodingInfo = (EncodingInfo) _encodingTableKeyMime.get(upperCaseFast);
        }
        if (encodingInfo != null) {
            return encodingInfo.getHighChar();
        }
        return (char) 0;
    }
}
