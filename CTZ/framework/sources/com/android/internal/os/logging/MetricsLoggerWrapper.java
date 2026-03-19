package com.android.internal.os.logging;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Pair;
import android.util.StatsLog;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto;

public class MetricsLoggerWrapper {
    private static final int METRIC_VALUE_DISMISSED_BY_DRAG = 1;
    private static final int METRIC_VALUE_DISMISSED_BY_TAP = 0;

    public static void logPictureInPictureDismissByTap(Context context, Pair<ComponentName, Integer> pair) {
        MetricsLogger.action(context, 822, 0);
        StatsLog.write(52, getUid(context, pair.first, pair.second.intValue()), pair.first.flattenToString(), 4);
    }

    public static void logPictureInPictureDismissByDrag(Context context, Pair<ComponentName, Integer> pair) {
        MetricsLogger.action(context, 822, 1);
        StatsLog.write(52, getUid(context, pair.first, pair.second.intValue()), pair.first.flattenToString(), 4);
    }

    public static void logPictureInPictureMinimize(Context context, boolean z, Pair<ComponentName, Integer> pair) {
        MetricsLogger.action(context, 821, z);
        StatsLog.write(52, getUid(context, pair.first, pair.second.intValue()), pair.first.flattenToString(), 3);
    }

    private static int getUid(Context context, ComponentName componentName, int i) {
        try {
            return context.getPackageManager().getApplicationInfoAsUser(componentName.getPackageName(), 0, i).uid;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    public static void logPictureInPictureMenuVisible(Context context, boolean z) {
        MetricsLogger.visibility(context, 823, z);
    }

    public static void logPictureInPictureEnter(Context context, int i, String str, boolean z) {
        MetricsLogger.action(context, MetricsProto.MetricsEvent.ACTION_PICTURE_IN_PICTURE_ENTERED, z);
        StatsLog.write(52, i, str, 1);
    }

    public static void logPictureInPictureFullScreen(Context context, int i, String str) {
        MetricsLogger.action(context, MetricsProto.MetricsEvent.ACTION_PICTURE_IN_PICTURE_EXPANDED_TO_FULLSCREEN);
        StatsLog.write(52, i, str, 2);
    }

    public static void logAppOverlayEnter(int i, String str, boolean z, int i2, boolean z2) {
        if (z) {
            if (i2 != 2038) {
                StatsLog.write(59, i, str, true, 1);
            } else if (!z2) {
                StatsLog.write(59, i, str, false, 1);
            }
        }
    }

    public static void logAppOverlayExit(int i, String str, boolean z, int i2, boolean z2) {
        if (z) {
            if (i2 != 2038) {
                StatsLog.write(59, i, str, true, 2);
            } else if (!z2) {
                StatsLog.write(59, i, str, false, 2);
            }
        }
    }
}
