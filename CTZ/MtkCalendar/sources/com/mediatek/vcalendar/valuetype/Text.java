package com.mediatek.vcalendar.valuetype;

import com.mediatek.vcalendar.utils.LogUtil;

public final class Text {
    public static String decode(String str, String str2, String str3) {
        if (str3.equals("QUOTED-PRINTABLE")) {
            str = Charset.decodeQuotedPrintable(str, str2);
        }
        if (str3.equals("BASE64")) {
            LogUtil.e("Text", "decode: we do not support the BASE64 text decode");
        }
        return str;
    }

    public static String decode(String str, String str2) {
        return decode(str, "UTF-8", str2);
    }

    public static String encoding(String str, String str2, String str3) {
        if (str3.equals("QUOTED-PRINTABLE")) {
            str = Charset.encodeQuotedPrintable(str, str2);
        }
        if (str3.equals("BASE64")) {
            LogUtil.e("Text", "encoding(): not support the BASE64 text decode");
        }
        return str;
    }

    public static String encoding(String str, String str2) {
        return encoding(str, "UTF-8", str2);
    }
}
