package com.android.documentsui;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.android.documentsui.base.SharedMinimal;
import com.android.internal.logging.MetricsLogger;

public final class ScopedAccessMetrics {
    public static void logInvalidScopedAccessRequest(Context context, String str) {
        byte b;
        int iHashCode = str.hashCode();
        if (iHashCode != -349446562) {
            if (iHashCode != 467452175) {
                b = (iHashCode == 1400555035 && str.equals("docsui_scoped_directory_access_invalid_dir")) ? (byte) 1 : (byte) -1;
            } else if (str.equals("docsui_scoped_directory_access_invalid_args")) {
                b = 0;
            }
        } else if (str.equals("docsui_scoped_directory_access_error")) {
            b = 2;
        }
        switch (b) {
            case 0:
            case 1:
            case 2:
                logCount(context, str);
                break;
            default:
                Log.wtf("ScopedAccessMetrics", "invalid InvalidScopedAccess: " + str);
                break;
        }
    }

    public static void logValidScopedAccessRequest(Activity activity, String str, int i) {
        int i2;
        if ("ROOT_DIRECTORY".equals(str)) {
            i2 = -2;
        } else {
            int i3 = 0;
            while (true) {
                if (i3 < Environment.STANDARD_DIRECTORIES.length) {
                    if (!Environment.STANDARD_DIRECTORIES[i3].equals(str)) {
                        i3++;
                    } else {
                        i2 = i3;
                        break;
                    }
                } else {
                    i2 = -1;
                    break;
                }
            }
        }
        String callingPackage = activity.getCallingPackage();
        switch (i) {
            case 0:
                MetricsLogger.action(activity, 331, callingPackage);
                MetricsLogger.action(activity, 330, i2);
                break;
            case 1:
                MetricsLogger.action(activity, 328, callingPackage);
                MetricsLogger.action(activity, 326, i2);
                break;
            case 2:
                MetricsLogger.action(activity, 329, callingPackage);
                MetricsLogger.action(activity, 327, i2);
                break;
            case 3:
                MetricsLogger.action(activity, 356, callingPackage);
                MetricsLogger.action(activity, 355, i2);
                break;
            case 4:
                MetricsLogger.action(activity, 354, callingPackage);
                MetricsLogger.action(activity, 353, i2);
                break;
            default:
                Log.wtf("ScopedAccessMetrics", "invalid ScopedAccessGrant: " + i);
                break;
        }
    }

    private static void logCount(Context context, String str) {
        if (SharedMinimal.DEBUG) {
            Log.d("ScopedAccessMetrics", str + ": 1");
        }
        MetricsLogger.count(context, str, 1);
    }
}
