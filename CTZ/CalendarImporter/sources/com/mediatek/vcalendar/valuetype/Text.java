package com.mediatek.vcalendar.valuetype;

import com.mediatek.vcalendar.parameter.Encoding;
import com.mediatek.vcalendar.utils.LogUtil;

public final class Text {
    private static final String TAG = "Text";

    private Text() {
    }

    public static String decode(String str, String str2, String str3) {
        if (str3.equals(Encoding.QUOTED_PRINTABLE)) {
            str = Charset.decodeQuotedPrintable(str, str2);
        }
        if (str3.equals(Encoding.BASE64)) {
            LogUtil.e(TAG, "decode: we do not support the BASE64 text decode");
        }
        return str;
    }

    public static String decode(String str, String str2) {
        return decode(str, Charset.UTF8, str2);
    }

    public static String encoding(String str, String str2, String str3) {
        if (str3.equals(Encoding.QUOTED_PRINTABLE)) {
            str = Charset.encodeQuotedPrintable(str, str2);
        }
        if (str3.equals(Encoding.BASE64)) {
            LogUtil.e(TAG, "encoding(): not support the BASE64 text decode");
        }
        return str;
    }

    public static String encoding(String str, String str2) {
        return encoding(str, Charset.UTF8, str2);
    }
}
