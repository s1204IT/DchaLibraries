package com.mediatek.vcalendar.valuetype;

import com.mediatek.vcalendar.component.VCalendar;
import com.mediatek.vcalendar.utils.LogUtil;
import java.util.Locale;

public final class Recur {
    public static String updateRRuleToRfc5545Version(String str) {
        String str2;
        String str3;
        String upperCase = str.toUpperCase(Locale.US);
        LogUtil.d("Recur", "updateRRuleToNewVersion(): the rRuleString: " + str);
        if (VCalendar.getVCalendarVersion().contains("2.0") || upperCase.contains("FREQ")) {
            LogUtil.d("Recur", "updateRRuleToNewVersion(): the rRuleString: " + str + " is already in version 2.0");
            return upperCase;
        }
        char cCharAt = upperCase.charAt(0);
        String[] strArrSplit = upperCase.split(" ");
        String str4 = null;
        boolean z = true;
        if (cCharAt == 'D') {
            str2 = "DAILY";
        } else {
            if (cCharAt == 'M') {
                str4 = "MONTHLY";
                String str5 = strArrSplit[2];
                String str6 = strArrSplit[1];
                if (str6 != null && !str6.contains("+")) {
                    str5 = strArrSplit[1];
                    z = false;
                }
                str3 = str5;
            } else if (cCharAt == 'W') {
                str4 = "WEEKLY";
                str3 = strArrSplit[1];
                int length = strArrSplit.length;
                if (length >= 3) {
                    StringBuffer stringBuffer = new StringBuffer();
                    for (int i = 2; i < length - 1; i++) {
                        stringBuffer.append(",");
                        stringBuffer.append(strArrSplit[i]);
                    }
                    str3 = str3 + stringBuffer.toString();
                }
            } else if (cCharAt == 'Y') {
                str2 = "YEARLY";
            } else {
                LogUtil.w("Recur", "can not parse mRRule=" + upperCase);
                str3 = null;
            }
            if (str4 != null) {
                upperCase = "FREQ=?;WKST=SU".replace("?", str4);
                if (str3 != null) {
                    upperCase = z ? upperCase + ";BYDAY=" + str3 : upperCase + ";BYMONTHDAY=" + str3;
                }
                LogUtil.i("vCalendar---", "updateRRuleToNewVersion(): setVCalendar()-->> mRRule=" + upperCase);
            }
            LogUtil.d("Recur", "updateRRuleToNewVersion(): Version1.0: \"" + str + "\"~~ Version2.0: \"" + upperCase + "\"");
            return upperCase;
        }
        str4 = str2;
        str3 = null;
        if (str4 != null) {
        }
        LogUtil.d("Recur", "updateRRuleToNewVersion(): Version1.0: \"" + str + "\"~~ Version2.0: \"" + upperCase + "\"");
        return upperCase;
    }
}
