package com.mediatek.vcalendar.valuetype;

import com.mediatek.vcalendar.component.VCalendar;
import com.mediatek.vcalendar.property.Version;
import com.mediatek.vcalendar.utils.LogUtil;
import java.util.Locale;

public final class Recur {
    private static final String BASE_RULE = "FREQ=?;WKST=SU";
    private static final String BYDAY = "BYDAY";
    private static final String BYMONTHDAY = "BYMONTHDAY";
    private static final String COLON = ":";
    private static final String DAILY = "DAILY";
    private static final String EQUEL = "=";
    private static final String FREQ = "FREQ";
    private static final String MONTHLY = "MONTHLY";
    private static final int RRULE_SEGMENTS_COUNT_WITH_EXTRA = 3;
    private static final String SEMICOLON = ";";
    private static final String TAG = "Recur";
    private static final String WEEKLY = "WEEKLY";
    private static final String YEARLY = "YEARLY";

    private Recur() {
    }

    public static String updateRRuleToRfc5545Version(String str) {
        String str2;
        String str3;
        String upperCase = str.toUpperCase(Locale.US);
        LogUtil.d(TAG, "updateRRuleToNewVersion(): the rRuleString: " + str);
        if (VCalendar.getVCalendarVersion().contains(Version.VERSION20) || upperCase.contains(FREQ)) {
            LogUtil.d(TAG, "updateRRuleToNewVersion(): the rRuleString: " + str + " is already in version 2.0");
            return upperCase;
        }
        char cCharAt = upperCase.charAt(0);
        String[] strArrSplit = upperCase.split(" ");
        String str4 = null;
        boolean z = true;
        if (cCharAt == 'D') {
            str2 = DAILY;
        } else {
            if (cCharAt == 'M') {
                str4 = MONTHLY;
                String str5 = strArrSplit[2];
                String str6 = strArrSplit[1];
                if (str6 != null && !str6.contains("+")) {
                    str5 = strArrSplit[1];
                    z = false;
                }
                str3 = str5;
            } else if (cCharAt == 'W') {
                str4 = WEEKLY;
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
                str2 = YEARLY;
            } else {
                LogUtil.w(TAG, "can not parse mRRule=" + upperCase);
                str3 = null;
            }
            if (str4 != null) {
                upperCase = BASE_RULE.replace("?", str4);
                if (str3 != null) {
                    upperCase = z ? upperCase + ";BYDAY=" + str3 : upperCase + ";BYMONTHDAY=" + str3;
                }
                LogUtil.i(LogUtil.TAG, "updateRRuleToNewVersion(): setVCalendar()-->> mRRule=" + upperCase);
            }
            LogUtil.d(TAG, "updateRRuleToNewVersion(): Version1.0: \"" + str + "\"~~ Version2.0: \"" + upperCase + "\"");
            return upperCase;
        }
        str4 = str2;
        str3 = null;
        if (str4 != null) {
        }
        LogUtil.d(TAG, "updateRRuleToNewVersion(): Version1.0: \"" + str + "\"~~ Version2.0: \"" + upperCase + "\"");
        return upperCase;
    }
}
