package com.android.settingslib;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeviceInfoUtils {
    private static String readLine(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }

    public static String getFormattedKernelVersion() {
        try {
            return formatKernelVersion(readLine("/proc/version"));
        } catch (IOException e) {
            Log.e("DeviceInfoUtils", "IO Exception when getting kernel version for Device Info screen", e);
            return "Unavailable";
        }
    }

    public static String formatKernelVersion(String rawKernelVersion) {
        Matcher m = Pattern.compile("Linux version (\\S+) \\((\\S+?)\\) (?:\\(gcc.+? \\)) (#\\d+) (?:.*?)?((Sun|Mon|Tue|Wed|Thu|Fri|Sat).+)").matcher(rawKernelVersion);
        if (!m.matches()) {
            Log.e("DeviceInfoUtils", "Regex did not match on /proc/version: " + rawKernelVersion);
            return "Unavailable";
        }
        if (m.groupCount() >= 4) {
            return m.group(1) + "\n" + m.group(2) + " " + m.group(3) + "\n" + m.group(4);
        }
        Log.e("DeviceInfoUtils", "Regex match on /proc/version only returned " + m.groupCount() + " groups");
        return "Unavailable";
    }

    public static String getMsvSuffix() {
        try {
            String msv = readLine("/sys/board_properties/soc/msv");
            if (Long.parseLong(msv, 16) == 0) {
                return " (ENGINEERING)";
            }
            return "";
        } catch (IOException | NumberFormatException e) {
            return "";
        }
    }

    public static String getFeedbackReporterPackage(Context context) {
        String feedbackReporter = context.getResources().getString(R$string.oem_preferred_feedback_reporter);
        if (TextUtils.isEmpty(feedbackReporter)) {
            return feedbackReporter;
        }
        Intent intent = new Intent("android.intent.action.BUG_REPORT");
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolvedPackages = pm.queryIntentActivities(intent, 64);
        for (ResolveInfo info : resolvedPackages) {
            if (info.activityInfo != null && !TextUtils.isEmpty(info.activityInfo.packageName)) {
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(info.activityInfo.packageName, 0);
                    if ((ai.flags & 1) != 0 && TextUtils.equals(info.activityInfo.packageName, feedbackReporter)) {
                        return feedbackReporter;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        }
        return null;
    }

    public static String getSecurityPatch() {
        String patch = Build.VERSION.SECURITY_PATCH;
        if (!"".equals(patch)) {
            try {
                SimpleDateFormat template = new SimpleDateFormat("yyyy-MM-dd");
                Date patchDate = template.parse(patch);
                String format = DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMMyyyy");
                return DateFormat.format(format, patchDate).toString();
            } catch (ParseException e) {
                return patch;
            }
        }
        return null;
    }
}
