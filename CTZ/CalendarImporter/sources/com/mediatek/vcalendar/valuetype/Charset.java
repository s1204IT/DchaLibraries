package com.mediatek.vcalendar.valuetype;

import android.text.TextUtils;
import com.android.common.speech.LoggingEvents;
import com.mediatek.vcalendar.utils.LogUtil;
import com.mediatek.vcalendar.utils.StringUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.apache.james.mime4j.decoder.QuotedPrintableInputStream;

public final class Charset {
    public static final String GB18030 = "GB18030";
    private static final String TAG = "Charset";
    public static final String UTF8 = "UTF-8";
    static char[] sList = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    static StringBuffer sBuf = new StringBuffer();

    private Charset() {
    }

    public static String encoding(String str) {
        return encoding(str, UTF8);
    }

    public static String encoding(String str, String str2) {
        if (StringUtil.isNullOrEmpty(str) || StringUtil.isNullOrEmpty(str2)) {
            return null;
        }
        sBuf.setLength(0);
        try {
            byte[] bytes = str.getBytes(str2);
            for (int i = 0; i < bytes.length; i++) {
                sBuf.append('=');
                sBuf.append(sList[(bytes[i] >> 4) & 15]);
                sBuf.append(sList[bytes[i] & 15]);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return new String(sBuf);
    }

    public static String encodeQuotedPrintable(String str, String str2) {
        byte[] bytes;
        if (TextUtils.isEmpty(str)) {
            return LoggingEvents.EXTRA_CALLING_APP_NAME;
        }
        StringBuilder sb = new StringBuilder();
        try {
            bytes = str.getBytes(str2);
        } catch (UnsupportedEncodingException e) {
            LogUtil.e(TAG, "encodeQuotedPrintable(): Charset " + str2 + " cannot be used. Try default charset");
            bytes = str.getBytes();
        }
        int i = 0;
        int i2 = 0;
        while (i < bytes.length) {
            sb.append(String.format("=%02X", Byte.valueOf(bytes[i])));
            i++;
            i2 += 3;
            if (i2 >= 67) {
                sb.append("=\r\n");
                i2 = 0;
            }
        }
        return sb.toString();
    }

    public static String decoding(String str) {
        return decoding(str, UTF8);
    }

    public static String decoding(String str, String str2) {
        if (StringUtil.isNullOrEmpty(str) || StringUtil.isNullOrEmpty(str2) || str.indexOf("=") != 0 || str.length() % 3 != 0) {
            return null;
        }
        String strReplaceAll = str.replaceAll("=", LoggingEvents.EXTRA_CALLING_APP_NAME);
        if (strReplaceAll.length() % 2 != 0) {
            return null;
        }
        try {
            return new String(hexStringToByteArray(strReplaceAll), str2);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static byte[] hexStringToByteArray(String str) {
        int length = str.length();
        byte[] bArr = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            bArr[i / 2] = (byte) ((toByte(str.charAt(i)) << 4) | toByte(str.charAt(i + 1)));
        }
        return bArr;
    }

    private static int toByte(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'F') {
            return (c - 'A') + 10;
        }
        if (c >= 'a' && c <= 'f') {
            return (c - 'a') + 10;
        }
        throw new RuntimeException("Invalid hex char '" + c + "'");
    }

    public static String decodeQuotedPrintable(String str, String str2) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '_') {
                stringBuffer.append("=20");
            } else {
                stringBuffer.append(cCharAt);
            }
        }
        try {
            return new String(decodeBaseQuotedPrintable(stringBuffer.toString()), str2);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] decodeBaseQuotedPrintable(String str) {
        QuotedPrintableInputStream quotedPrintableInputStream;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            quotedPrintableInputStream = new QuotedPrintableInputStream(new ByteArrayInputStream(str.getBytes("US-ASCII")));
        } catch (IOException e) {
            LogUtil.i(TAG, "decodeBaseQuotedPrintable(): Charset--error.");
        }
        while (true) {
            int i = quotedPrintableInputStream.read();
            if (i == -1) {
                break;
            }
            byteArrayOutputStream.write(i);
            return byteArrayOutputStream.toByteArray();
        }
        quotedPrintableInputStream.close();
        return byteArrayOutputStream.toByteArray();
    }
}
