package mf.org.apache.xml.serialize;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Method;
import mf.org.apache.xerces.util.EncodingMap;

public class EncodingInfo {
    String ianaName;
    String javaName;
    int lastPrintable;
    private Object[] fArgsForMethod = null;
    Object fCharsetEncoder = null;
    Object fCharToByteConverter = null;
    boolean fHaveTriedCToB = false;
    boolean fHaveTriedCharsetEncoder = false;

    public EncodingInfo(String ianaName, String javaName, int lastPrintable) {
        this.ianaName = ianaName;
        this.javaName = EncodingMap.getIANA2JavaMapping(ianaName);
        this.lastPrintable = lastPrintable;
    }

    public Writer getWriter(OutputStream output) throws UnsupportedEncodingException {
        if (this.javaName != null) {
            return new OutputStreamWriter(output, this.javaName);
        }
        this.javaName = EncodingMap.getIANA2JavaMapping(this.ianaName);
        if (this.javaName == null) {
            return new OutputStreamWriter(output, "UTF8");
        }
        return new OutputStreamWriter(output, this.javaName);
    }

    public boolean isPrintable(char ch) {
        if (ch <= this.lastPrintable) {
            return true;
        }
        return isPrintable0(ch);
    }

    private boolean isPrintable0(char ch) {
        if (this.fCharsetEncoder == null && CharsetMethods.fgNIOCharsetAvailable && !this.fHaveTriedCharsetEncoder) {
            if (this.fArgsForMethod == null) {
                this.fArgsForMethod = new Object[1];
            }
            try {
                this.fArgsForMethod[0] = this.javaName;
                Object charset = CharsetMethods.fgCharsetForNameMethod.invoke(null, this.fArgsForMethod);
                if (!((Boolean) CharsetMethods.fgCharsetCanEncodeMethod.invoke(charset, null)).booleanValue()) {
                    this.fHaveTriedCharsetEncoder = true;
                } else {
                    this.fCharsetEncoder = CharsetMethods.fgCharsetNewEncoderMethod.invoke(charset, null);
                }
            } catch (Exception e) {
                this.fHaveTriedCharsetEncoder = true;
            }
        }
        if (this.fCharsetEncoder != null) {
            try {
                this.fArgsForMethod[0] = new Character(ch);
                return ((Boolean) CharsetMethods.fgCharsetEncoderCanEncodeMethod.invoke(this.fCharsetEncoder, this.fArgsForMethod)).booleanValue();
            } catch (Exception e2) {
                this.fCharsetEncoder = null;
                this.fHaveTriedCharsetEncoder = false;
            }
        }
        if (this.fCharToByteConverter == null) {
            if (this.fHaveTriedCToB || !CharToByteConverterMethods.fgConvertersAvailable) {
                return false;
            }
            if (this.fArgsForMethod == null) {
                this.fArgsForMethod = new Object[1];
            }
            try {
                this.fArgsForMethod[0] = this.javaName;
                this.fCharToByteConverter = CharToByteConverterMethods.fgGetConverterMethod.invoke(null, this.fArgsForMethod);
            } catch (Exception e3) {
                this.fHaveTriedCToB = true;
                return false;
            }
        }
        try {
            this.fArgsForMethod[0] = new Character(ch);
            return ((Boolean) CharToByteConverterMethods.fgCanConvertMethod.invoke(this.fCharToByteConverter, this.fArgsForMethod)).booleanValue();
        } catch (Exception e4) {
            this.fCharToByteConverter = null;
            this.fHaveTriedCToB = false;
            return false;
        }
    }

    public static void testJavaEncodingName(String name) throws UnsupportedEncodingException {
        byte[] bTest = {118, 97, 108, 105, 100};
        new String(bTest, name);
    }

    static class CharsetMethods {
        private static Method fgCharsetCanEncodeMethod;
        private static Method fgCharsetEncoderCanEncodeMethod;
        private static Method fgCharsetForNameMethod;
        private static Method fgCharsetNewEncoderMethod;
        private static boolean fgNIOCharsetAvailable;

        static {
            fgCharsetForNameMethod = null;
            fgCharsetCanEncodeMethod = null;
            fgCharsetNewEncoderMethod = null;
            fgCharsetEncoderCanEncodeMethod = null;
            fgNIOCharsetAvailable = false;
            try {
                Class<?> cls = Class.forName("java.nio.charset.Charset");
                Class<?> cls2 = Class.forName("java.nio.charset.CharsetEncoder");
                fgCharsetForNameMethod = cls.getMethod("forName", String.class);
                fgCharsetCanEncodeMethod = cls.getMethod("canEncode", new Class[0]);
                fgCharsetNewEncoderMethod = cls.getMethod("newEncoder", new Class[0]);
                fgCharsetEncoderCanEncodeMethod = cls2.getMethod("canEncode", Character.TYPE);
                fgNIOCharsetAvailable = true;
            } catch (Exception e) {
                fgCharsetForNameMethod = null;
                fgCharsetCanEncodeMethod = null;
                fgCharsetEncoderCanEncodeMethod = null;
                fgCharsetNewEncoderMethod = null;
                fgNIOCharsetAvailable = false;
            }
        }
    }

    static class CharToByteConverterMethods {
        private static Method fgCanConvertMethod;
        private static boolean fgConvertersAvailable;
        private static Method fgGetConverterMethod;

        static {
            fgGetConverterMethod = null;
            fgCanConvertMethod = null;
            fgConvertersAvailable = false;
            try {
                Class<?> cls = Class.forName("sun.io.CharToByteConverter");
                fgGetConverterMethod = cls.getMethod("getConverter", String.class);
                fgCanConvertMethod = cls.getMethod("canConvert", Character.TYPE);
                fgConvertersAvailable = true;
            } catch (Exception e) {
                fgGetConverterMethod = null;
                fgCanConvertMethod = null;
                fgConvertersAvailable = false;
            }
        }
    }
}
