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
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import org.apache.xml.serializer.utils.WrappedRuntimeException;

public final class Encodings {
    static final String DEFAULT_MIME_ENCODING = "UTF-8";
    private static final String ENCODINGS_FILE = SerializerBase.PKG_PATH + "/Encodings.properties";
    private static final Hashtable _encodingTableKeyJava = new Hashtable();
    private static final Hashtable _encodingTableKeyMime = new Hashtable();
    private static final EncodingInfo[] _encodings = loadEncodingInfo();

    static Writer getWriter(OutputStream output, String encoding) throws UnsupportedEncodingException {
        for (int i = 0; i < _encodings.length; i++) {
            if (_encodings[i].name.equalsIgnoreCase(encoding)) {
                try {
                    String javaName = _encodings[i].javaName;
                    OutputStreamWriter osw = new OutputStreamWriter(output, javaName);
                    return osw;
                } catch (UnsupportedEncodingException e) {
                } catch (IllegalArgumentException e2) {
                }
            }
        }
        try {
            return new OutputStreamWriter(output, encoding);
        } catch (IllegalArgumentException e3) {
            throw new UnsupportedEncodingException(encoding);
        }
    }

    static EncodingInfo getEncodingInfo(String encoding) {
        String normalizedEncoding = toUpperCaseFast(encoding);
        EncodingInfo ei = (EncodingInfo) _encodingTableKeyJava.get(normalizedEncoding);
        if (ei == null) {
            ei = (EncodingInfo) _encodingTableKeyMime.get(normalizedEncoding);
        }
        if (ei == null) {
            return new EncodingInfo(null, null, (char) 0);
        }
        return ei;
    }

    public static boolean isRecognizedEncoding(String encoding) {
        String normalizedEncoding = encoding.toUpperCase();
        EncodingInfo ei = (EncodingInfo) _encodingTableKeyJava.get(normalizedEncoding);
        if (ei == null) {
            ei = (EncodingInfo) _encodingTableKeyMime.get(normalizedEncoding);
        }
        if (ei != null) {
            return true;
        }
        return false;
    }

    private static String toUpperCaseFast(String s) {
        boolean different = false;
        int mx = s.length();
        char[] chars = new char[mx];
        for (int i = 0; i < mx; i++) {
            char ch = s.charAt(i);
            if ('a' <= ch && ch <= 'z') {
                ch = (char) (ch - ' ');
                different = true;
            }
            chars[i] = ch;
        }
        if (different) {
            String upper = String.valueOf(chars);
            return upper;
        }
        return s;
    }

    static String getMimeEncoding(String encoding) {
        if (encoding != null) {
            return convertJava2MimeEncoding(encoding);
        }
        try {
            String encoding2 = System.getProperty("file.encoding", "UTF8");
            if (encoding2 == null) {
                return DEFAULT_MIME_ENCODING;
            }
            String jencoding = (encoding2.equalsIgnoreCase("Cp1252") || encoding2.equalsIgnoreCase("ISO8859_1") || encoding2.equalsIgnoreCase("8859_1") || encoding2.equalsIgnoreCase("UTF8")) ? DEFAULT_MIME_ENCODING : convertJava2MimeEncoding(encoding2);
            return jencoding != null ? jencoding : DEFAULT_MIME_ENCODING;
        } catch (SecurityException e) {
            return DEFAULT_MIME_ENCODING;
        }
    }

    private static String convertJava2MimeEncoding(String encoding) {
        EncodingInfo enc = (EncodingInfo) _encodingTableKeyJava.get(toUpperCaseFast(encoding));
        if (enc != null) {
            return enc.name;
        }
        return encoding;
    }

    public static String convertMime2JavaEncoding(String encoding) {
        for (int i = 0; i < _encodings.length; i++) {
            if (_encodings[i].name.equalsIgnoreCase(encoding)) {
                return _encodings[i].javaName;
            }
        }
        return encoding;
    }

    private static EncodingInfo[] loadEncodingInfo() {
        char cIntValue;
        try {
            SecuritySupport ss = SecuritySupport.getInstance();
            InputStream is = ss.getResourceAsStream(ObjectFactory.findClassLoader(), ENCODINGS_FILE);
            Properties props = new Properties();
            if (is != null) {
                props.load(is);
                is.close();
            }
            int totalEntries = props.size();
            List encodingInfo_list = new ArrayList();
            Enumeration keys = props.keys();
            for (int i = 0; i < totalEntries; i++) {
                String javaName = (String) keys.nextElement();
                String val = props.getProperty(javaName);
                int len = lengthOfMimeNames(val);
                if (len != 0) {
                    try {
                        String highVal = val.substring(len).trim();
                        cIntValue = (char) Integer.decode(highVal).intValue();
                    } catch (NumberFormatException e) {
                        cIntValue = 0;
                    }
                    String mimeNames = val.substring(0, len);
                    StringTokenizer st = new StringTokenizer(mimeNames, ",");
                    boolean first = true;
                    while (st.hasMoreTokens()) {
                        String mimeName = st.nextToken();
                        EncodingInfo ei = new EncodingInfo(mimeName, javaName, cIntValue);
                        encodingInfo_list.add(ei);
                        _encodingTableKeyMime.put(mimeName.toUpperCase(), ei);
                        if (first) {
                            _encodingTableKeyJava.put(javaName.toUpperCase(), ei);
                        }
                        first = false;
                    }
                }
            }
            EncodingInfo[] ret_ei = new EncodingInfo[encodingInfo_list.size()];
            encodingInfo_list.toArray(ret_ei);
            return ret_ei;
        } catch (MalformedURLException mue) {
            throw new WrappedRuntimeException(mue);
        } catch (IOException ioe) {
            throw new WrappedRuntimeException(ioe);
        }
    }

    private static int lengthOfMimeNames(String val) {
        int len = val.indexOf(32);
        if (len < 0) {
            return val.length();
        }
        return len;
    }

    static boolean isHighUTF16Surrogate(char ch) {
        return 55296 <= ch && ch <= 56319;
    }

    static boolean isLowUTF16Surrogate(char ch) {
        return 56320 <= ch && ch <= 57343;
    }

    static int toCodePoint(char highSurrogate, char lowSurrogate) {
        int codePoint = ((highSurrogate - 55296) << 10) + (lowSurrogate - 56320) + 65536;
        return codePoint;
    }

    static int toCodePoint(char ch) {
        return ch;
    }

    public static char getHighChar(String encoding) {
        String normalizedEncoding = toUpperCaseFast(encoding);
        EncodingInfo ei = (EncodingInfo) _encodingTableKeyJava.get(normalizedEncoding);
        if (ei == null) {
            ei = (EncodingInfo) _encodingTableKeyMime.get(normalizedEncoding);
        }
        if (ei != null) {
            return ei.getHighChar();
        }
        return (char) 0;
    }
}
