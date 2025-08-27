package com.android.settingslib;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Build;
import android.system.Os;
import android.system.StructUtsname;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: classes.dex */
public class DeviceInfoUtils {
    private static String readLine(String str) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(str), 256);
        try {
            return bufferedReader.readLine();
        } finally {
            bufferedReader.close();
        }
    }

    public static String getFormattedKernelVersion(Context context) {
        return formatKernelVersion(context, Os.uname());
    }

    static String formatKernelVersion(Context context, StructUtsname structUtsname) {
        if (structUtsname == null) {
            return context.getString(R.string.status_unavailable);
        }
        Matcher matcher = Pattern.compile("(#\\d+) (?:.*?)?((Sun|Mon|Tue|Wed|Thu|Fri|Sat).+)").matcher(structUtsname.version);
        if (!matcher.matches()) {
            Log.e("DeviceInfoUtils", "Regex did not match on uname version " + structUtsname.version);
            return context.getString(R.string.status_unavailable);
        }
        return structUtsname.release + "\n" + matcher.group(1) + " " + matcher.group(2);
    }

    public static String getMsvSuffix() {
        try {
            if (Long.parseLong(readLine("/sys/board_properties/soc/msv"), 16) == 0) {
                return " (ENGINEERING)";
            }
            return "";
        } catch (IOException | NumberFormatException e) {
            return "";
        }
    }

    public static String getFeedbackReporterPackage(Context context) throws Resources.NotFoundException {
        String string = context.getResources().getString(R.string.oem_preferred_feedback_reporter);
        if (TextUtils.isEmpty(string)) {
            return string;
        }
        Intent intent = new Intent("android.intent.action.BUG_REPORT");
        PackageManager packageManager = context.getPackageManager();
        for (ResolveInfo resolveInfo : packageManager.queryIntentActivities(intent, 64)) {
            if (resolveInfo.activityInfo != null && !TextUtils.isEmpty(resolveInfo.activityInfo.packageName)) {
                try {
                    if ((packageManager.getApplicationInfo(resolveInfo.activityInfo.packageName, 0).flags & 1) != 0 && TextUtils.equals(resolveInfo.activityInfo.packageName, string)) {
                        return string;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        }
        return null;
    }

    public static String getSecurityPatch() {
        String str = Build.VERSION.SECURITY_PATCH;
        if (!"".equals(str)) {
            try {
                return DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMMyyyy"), new SimpleDateFormat("yyyy-MM-dd").parse(str)).toString();
            } catch (ParseException e) {
                return str;
            }
        }
        return null;
    }

    public static String getFormattedPhoneNumber(Context context, SubscriptionInfo subscriptionInfo) {
        if (subscriptionInfo != null) {
            String line1Number = ((TelephonyManager) context.getSystemService("phone")).getLine1Number(subscriptionInfo.getSubscriptionId());
            if (!TextUtils.isEmpty(line1Number)) {
                return PhoneNumberUtils.formatNumber(line1Number);
            }
        }
        return null;
    }
}
