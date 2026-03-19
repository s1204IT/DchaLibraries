package com.android.server.notification;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.service.notification.Condition;
import android.service.notification.IConditionProvider;
import android.service.notification.ZenModeConfig;
import android.util.Slog;
import com.android.server.UiModeManagerService;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ZenLog {
    private static final boolean DEBUG = Build.IS_DEBUGGABLE;
    private static final SimpleDateFormat FORMAT;
    private static final String[] MSGS;
    private static final int SIZE;
    private static final String TAG = "ZenLog";
    private static final long[] TIMES;
    private static final int[] TYPES;
    private static final int TYPE_ALLOW_DISABLE = 2;
    private static final int TYPE_CONFIG = 11;
    private static final int TYPE_DISABLE_EFFECTS = 13;
    private static final int TYPE_DOWNTIME = 5;
    private static final int TYPE_EXIT_CONDITION = 8;
    private static final int TYPE_INTERCEPTED = 1;
    private static final int TYPE_LISTENER_HINTS_CHANGED = 15;
    private static final int TYPE_NOT_INTERCEPTED = 12;
    private static final int TYPE_SET_NOTIFICATION_POLICY = 16;
    private static final int TYPE_SET_RINGER_MODE_EXTERNAL = 3;
    private static final int TYPE_SET_RINGER_MODE_INTERNAL = 4;
    private static final int TYPE_SET_ZEN_MODE = 6;
    private static final int TYPE_SUBSCRIBE = 9;
    private static final int TYPE_SUPPRESSOR_CHANGED = 14;
    private static final int TYPE_UNSUBSCRIBE = 10;
    private static final int TYPE_UPDATE_ZEN_MODE = 7;
    private static int sNext;
    private static int sSize;

    static {
        SIZE = Build.IS_DEBUGGABLE ? 100 : 20;
        TIMES = new long[SIZE];
        TYPES = new int[SIZE];
        MSGS = new String[SIZE];
        FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    }

    public static void traceIntercepted(NotificationRecord notificationRecord, String str) {
        if (notificationRecord == null || !notificationRecord.isIntercepted()) {
            append(1, notificationRecord.getKey() + "," + str);
        }
    }

    public static void traceNotIntercepted(NotificationRecord notificationRecord, String str) {
        if (notificationRecord == null || !notificationRecord.isUpdate) {
            append(12, notificationRecord.getKey() + "," + str);
        }
    }

    public static void traceSetRingerModeExternal(int i, int i2, String str, int i3, int i4) {
        append(3, str + ",e:" + ringerModeToString(i) + "->" + ringerModeToString(i2) + ",i:" + ringerModeToString(i3) + "->" + ringerModeToString(i4));
    }

    public static void traceSetRingerModeInternal(int i, int i2, String str, int i3, int i4) {
        append(4, str + ",i:" + ringerModeToString(i) + "->" + ringerModeToString(i2) + ",e:" + ringerModeToString(i3) + "->" + ringerModeToString(i4));
    }

    public static void traceDowntimeAutotrigger(String str) {
        append(5, str);
    }

    public static void traceSetZenMode(int i, String str) {
        append(6, zenModeToString(i) + "," + str);
    }

    public static void traceUpdateZenMode(int i, int i2) {
        append(7, zenModeToString(i) + " -> " + zenModeToString(i2));
    }

    public static void traceExitCondition(Condition condition, ComponentName componentName, String str) {
        append(8, condition + "," + componentToString(componentName) + "," + str);
    }

    public static void traceSetNotificationPolicy(String str, int i, NotificationManager.Policy policy) {
        append(16, "pkg=" + str + " targetSdk=" + i + " NotificationPolicy=" + policy.toString());
    }

    public static void traceSubscribe(Uri uri, IConditionProvider iConditionProvider, RemoteException remoteException) {
        append(9, uri + "," + subscribeResult(iConditionProvider, remoteException));
    }

    public static void traceUnsubscribe(Uri uri, IConditionProvider iConditionProvider, RemoteException remoteException) {
        append(10, uri + "," + subscribeResult(iConditionProvider, remoteException));
    }

    public static void traceConfig(String str, ZenModeConfig zenModeConfig, ZenModeConfig zenModeConfig2) {
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append(",");
        sb.append(zenModeConfig2 != null ? zenModeConfig2.toString() : null);
        sb.append(",");
        sb.append(ZenModeConfig.diff(zenModeConfig, zenModeConfig2));
        append(11, sb.toString());
    }

    public static void traceDisableEffects(NotificationRecord notificationRecord, String str) {
        append(13, notificationRecord.getKey() + "," + str);
    }

    public static void traceEffectsSuppressorChanged(List<ComponentName> list, List<ComponentName> list2, long j) {
        append(14, "suppressed effects:" + j + "," + componentListToString(list) + "->" + componentListToString(list2));
    }

    public static void traceListenerHintsChanged(int i, int i2, int i3) {
        append(15, hintsToString(i) + "->" + hintsToString(i2) + ",listeners=" + i3);
    }

    private static String subscribeResult(IConditionProvider iConditionProvider, RemoteException remoteException) {
        return iConditionProvider == null ? "no provider" : remoteException != null ? remoteException.getMessage() : "ok";
    }

    private static String typeToString(int i) {
        switch (i) {
            case 1:
                return "intercepted";
            case 2:
                return "allow_disable";
            case 3:
                return "set_ringer_mode_external";
            case 4:
                return "set_ringer_mode_internal";
            case 5:
                return "downtime";
            case 6:
                return "set_zen_mode";
            case 7:
                return "update_zen_mode";
            case 8:
                return "exit_condition";
            case 9:
                return "subscribe";
            case 10:
                return "unsubscribe";
            case 11:
                return "config";
            case 12:
                return "not_intercepted";
            case 13:
                return "disable_effects";
            case 14:
                return "suppressor_changed";
            case 15:
                return "listener_hints_changed";
            case 16:
                return "set_notification_policy";
            default:
                return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
        }
    }

    private static String ringerModeToString(int i) {
        switch (i) {
            case 0:
                return "silent";
            case 1:
                return "vibrate";
            case 2:
                return "normal";
            default:
                return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
        }
    }

    private static String zenModeToString(int i) {
        switch (i) {
            case 0:
                return "off";
            case 1:
                return "important_interruptions";
            case 2:
                return "no_interruptions";
            case 3:
                return "alarms";
            default:
                return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
        }
    }

    private static String hintsToString(int i) {
        if (i != 4) {
            switch (i) {
                case 0:
                    return "none";
                case 1:
                    return "disable_effects";
                case 2:
                    return "disable_notification_effects";
                default:
                    return Integer.toString(i);
            }
        }
        return "disable_call_effects";
    }

    private static String componentToString(ComponentName componentName) {
        if (componentName != null) {
            return componentName.toShortString();
        }
        return null;
    }

    private static String componentListToString(List<ComponentName> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(componentToString(list.get(i)));
        }
        return sb.toString();
    }

    private static void append(int i, String str) {
        synchronized (MSGS) {
            TIMES[sNext] = System.currentTimeMillis();
            TYPES[sNext] = i;
            MSGS[sNext] = str;
            sNext = (sNext + 1) % SIZE;
            if (sSize < SIZE) {
                sSize++;
            }
        }
        if (DEBUG) {
            Slog.d(TAG, typeToString(i) + ": " + str);
        }
    }

    public static void dump(PrintWriter printWriter, String str) {
        synchronized (MSGS) {
            int i = ((sNext - sSize) + SIZE) % SIZE;
            for (int i2 = 0; i2 < sSize; i2++) {
                int i3 = (i + i2) % SIZE;
                printWriter.print(str);
                printWriter.print(FORMAT.format(new Date(TIMES[i3])));
                printWriter.print(' ');
                printWriter.print(typeToString(TYPES[i3]));
                printWriter.print(": ");
                printWriter.println(MSGS[i3]);
            }
        }
    }
}
