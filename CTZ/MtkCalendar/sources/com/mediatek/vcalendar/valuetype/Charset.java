package com.mediatek.vcalendar.valuetype;

import android.text.TextUtils;
import com.mediatek.vcalendar.utils.LogUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.apache.james.mime4j.decoder.QuotedPrintableInputStream;

public final class Charset {
    static char[] sList = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    static StringBuffer sBuf = new StringBuffer();

    public static String encodeQuotedPrintable(String str, String str2) {
        byte[] bytes;
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try {
            bytes = str.getBytes(str2);
        } catch (UnsupportedEncodingException e) {
            LogUtil.e("Charset", "encodeQuotedPrintable(): Charset " + str2 + " cannot be used. Try default charset");
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
            LogUtil.i("Charset", "decodeBaseQuotedPrintable(): Charset--error.");
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
