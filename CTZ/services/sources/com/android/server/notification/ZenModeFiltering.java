package com.android.server.notification;

import android.R;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.os.UserHandle;
import android.service.notification.ZenModeConfig;
import android.telecom.TelecomManager;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.util.NotificationMessagingUtil;
import com.android.server.pm.PackageManagerService;
import java.io.PrintWriter;
import java.util.Date;

public class ZenModeFiltering {
    private static final boolean DEBUG = ZenModeHelper.DEBUG;
    static final RepeatCallers REPEAT_CALLERS = new RepeatCallers();
    private static final String TAG = "ZenModeHelper";
    private final Context mContext;
    private ComponentName mDefaultPhoneApp;
    private final NotificationMessagingUtil mMessagingUtil;

    public ZenModeFiltering(Context context) {
        this.mContext = context;
        this.mMessagingUtil = new NotificationMessagingUtil(this.mContext);
    }

    public ZenModeFiltering(Context context, NotificationMessagingUtil notificationMessagingUtil) {
        this.mContext = context;
        this.mMessagingUtil = notificationMessagingUtil;
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.print("mDefaultPhoneApp=");
        printWriter.println(this.mDefaultPhoneApp);
        printWriter.print(str);
        printWriter.print("RepeatCallers.mThresholdMinutes=");
        printWriter.println(REPEAT_CALLERS.mThresholdMinutes);
        synchronized (REPEAT_CALLERS) {
            if (!REPEAT_CALLERS.mCalls.isEmpty()) {
                printWriter.print(str);
                printWriter.println("RepeatCallers.mCalls=");
                for (int i = 0; i < REPEAT_CALLERS.mCalls.size(); i++) {
                    printWriter.print(str);
                    printWriter.print("  ");
                    printWriter.print((String) REPEAT_CALLERS.mCalls.keyAt(i));
                    printWriter.print(" at ");
                    printWriter.println(ts(((Long) REPEAT_CALLERS.mCalls.valueAt(i)).longValue()));
                }
            }
        }
    }

    private static String ts(long j) {
        return new Date(j) + " (" + j + ")";
    }

    public static boolean matchesCallFilter(Context context, int i, ZenModeConfig zenModeConfig, UserHandle userHandle, Bundle bundle, ValidateNotificationPeople validateNotificationPeople, int i2, float f) {
        if (i == 2 || i == 3) {
            return false;
        }
        if (i != 1 || (zenModeConfig.allowRepeatCallers && REPEAT_CALLERS.isRepeat(context, bundle))) {
            return true;
        }
        if (!zenModeConfig.allowCalls) {
            return false;
        }
        if (validateNotificationPeople != null) {
            return audienceMatches(zenModeConfig.allowCallsFrom, validateNotificationPeople.getContactAffinity(userHandle, bundle, i2, f));
        }
        return true;
    }

    private static Bundle extras(NotificationRecord notificationRecord) {
        if (notificationRecord == null || notificationRecord.sbn == null || notificationRecord.sbn.getNotification() == null) {
            return null;
        }
        return notificationRecord.sbn.getNotification().extras;
    }

    protected void recordCall(NotificationRecord notificationRecord) {
        REPEAT_CALLERS.recordCall(this.mContext, extras(notificationRecord));
    }

    public boolean shouldIntercept(int i, ZenModeConfig zenModeConfig, NotificationRecord notificationRecord) {
        if (i == 0) {
            return false;
        }
        if (NotificationManager.Policy.areAllVisualEffectsSuppressed(zenModeConfig.suppressedVisualEffects) && PackageManagerService.PLATFORM_PACKAGE_NAME.equals(notificationRecord.sbn.getPackageName()) && 48 == notificationRecord.sbn.getId()) {
            ZenLog.traceNotIntercepted(notificationRecord, "systemDndChangedNotification");
            return false;
        }
        switch (i) {
            case 1:
                if (notificationRecord.getPackagePriority() == 2) {
                    ZenLog.traceNotIntercepted(notificationRecord, "priorityApp");
                } else if (isAlarm(notificationRecord)) {
                    if (!zenModeConfig.allowAlarms) {
                        ZenLog.traceIntercepted(notificationRecord, "!allowAlarms");
                    }
                } else if (isCall(notificationRecord)) {
                    if (zenModeConfig.allowRepeatCallers && REPEAT_CALLERS.isRepeat(this.mContext, extras(notificationRecord))) {
                        ZenLog.traceNotIntercepted(notificationRecord, "repeatCaller");
                    } else if (!zenModeConfig.allowCalls) {
                        ZenLog.traceIntercepted(notificationRecord, "!allowCalls");
                    }
                } else if (isMessage(notificationRecord)) {
                    if (!zenModeConfig.allowMessages) {
                        ZenLog.traceIntercepted(notificationRecord, "!allowMessages");
                    }
                } else if (isEvent(notificationRecord)) {
                    if (!zenModeConfig.allowEvents) {
                        ZenLog.traceIntercepted(notificationRecord, "!allowEvents");
                    }
                } else if (isReminder(notificationRecord)) {
                    if (!zenModeConfig.allowReminders) {
                        ZenLog.traceIntercepted(notificationRecord, "!allowReminders");
                    }
                } else if (isMedia(notificationRecord)) {
                    if (!zenModeConfig.allowMedia) {
                        ZenLog.traceIntercepted(notificationRecord, "!allowMedia");
                    }
                } else if (isSystem(notificationRecord)) {
                    if (!zenModeConfig.allowSystem) {
                        ZenLog.traceIntercepted(notificationRecord, "!allowSystem");
                    }
                } else {
                    ZenLog.traceIntercepted(notificationRecord, "!priority");
                }
                break;
            case 2:
                ZenLog.traceIntercepted(notificationRecord, "none");
                break;
            case 3:
                if (!isAlarm(notificationRecord)) {
                    ZenLog.traceIntercepted(notificationRecord, "alarmsOnly");
                    break;
                }
                break;
        }
        return false;
    }

    private static boolean shouldInterceptAudience(int i, NotificationRecord notificationRecord) {
        if (!audienceMatches(i, notificationRecord.getContactAffinity())) {
            ZenLog.traceIntercepted(notificationRecord, "!audienceMatches");
            return true;
        }
        return false;
    }

    protected static boolean isAlarm(NotificationRecord notificationRecord) {
        return notificationRecord.isCategory("alarm") || notificationRecord.isAudioAttributesUsage(4);
    }

    private static boolean isEvent(NotificationRecord notificationRecord) {
        return notificationRecord.isCategory("event");
    }

    private static boolean isReminder(NotificationRecord notificationRecord) {
        return notificationRecord.isCategory("reminder");
    }

    public boolean isCall(NotificationRecord notificationRecord) {
        return notificationRecord != null && (isDefaultPhoneApp(notificationRecord.sbn.getPackageName()) || notificationRecord.isCategory("call"));
    }

    public boolean isMedia(NotificationRecord notificationRecord) {
        AudioAttributes audioAttributes = notificationRecord.getAudioAttributes();
        return audioAttributes != null && AudioAttributes.SUPPRESSIBLE_USAGES.get(audioAttributes.getUsage()) == 5;
    }

    public boolean isSystem(NotificationRecord notificationRecord) {
        AudioAttributes audioAttributes = notificationRecord.getAudioAttributes();
        return audioAttributes != null && AudioAttributes.SUPPRESSIBLE_USAGES.get(audioAttributes.getUsage()) == 6;
    }

    private boolean isDefaultPhoneApp(String str) {
        if (this.mDefaultPhoneApp == null) {
            TelecomManager telecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
            this.mDefaultPhoneApp = telecomManager != null ? telecomManager.getDefaultPhoneApp() : null;
            if (DEBUG) {
                Slog.d(TAG, "Default phone app: " + this.mDefaultPhoneApp);
            }
        }
        return (str == null || this.mDefaultPhoneApp == null || !str.equals(this.mDefaultPhoneApp.getPackageName())) ? false : true;
    }

    protected boolean isMessage(NotificationRecord notificationRecord) {
        return this.mMessagingUtil.isMessaging(notificationRecord.sbn);
    }

    private static boolean audienceMatches(int i, float f) {
        switch (i) {
            case 0:
                break;
            case 1:
                if (f >= 0.5f) {
                }
                break;
            case 2:
                if (f >= 1.0f) {
                }
                break;
            default:
                Slog.w(TAG, "Encountered unknown source: " + i);
                break;
        }
        return true;
    }

    private static class RepeatCallers {
        private final ArrayMap<String, Long> mCalls;
        private int mThresholdMinutes;

        private RepeatCallers() {
            this.mCalls = new ArrayMap<>();
        }

        private synchronized void recordCall(Context context, Bundle bundle) {
            setThresholdMinutes(context);
            if (this.mThresholdMinutes > 0 && bundle != null) {
                String strPeopleString = peopleString(bundle);
                if (strPeopleString == null) {
                    return;
                }
                long jCurrentTimeMillis = System.currentTimeMillis();
                cleanUp(this.mCalls, jCurrentTimeMillis);
                this.mCalls.put(strPeopleString, Long.valueOf(jCurrentTimeMillis));
            }
        }

        private synchronized boolean isRepeat(Context context, Bundle bundle) {
            setThresholdMinutes(context);
            if (this.mThresholdMinutes > 0 && bundle != null) {
                String strPeopleString = peopleString(bundle);
                if (strPeopleString == null) {
                    return false;
                }
                cleanUp(this.mCalls, System.currentTimeMillis());
                return this.mCalls.containsKey(strPeopleString);
            }
            return false;
        }

        private synchronized void cleanUp(ArrayMap<String, Long> arrayMap, long j) {
            for (int size = arrayMap.size() - 1; size >= 0; size--) {
                long jLongValue = this.mCalls.valueAt(size).longValue();
                if (jLongValue > j || j - jLongValue > this.mThresholdMinutes * 1000 * 60) {
                    arrayMap.removeAt(size);
                }
            }
        }

        private void setThresholdMinutes(Context context) {
            if (this.mThresholdMinutes <= 0) {
                this.mThresholdMinutes = context.getResources().getInteger(R.integer.config_mediaRouter_builtInSpeakerSuitability);
            }
        }

        private static String peopleString(Bundle bundle) {
            String[] extraPeople = ValidateNotificationPeople.getExtraPeople(bundle);
            if (extraPeople == null || extraPeople.length == 0) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (String str : extraPeople) {
                if (str != null) {
                    String strTrim = str.trim();
                    if (!strTrim.isEmpty()) {
                        if (sb.length() > 0) {
                            sb.append('|');
                        }
                        sb.append(strTrim);
                    }
                }
            }
            if (sb.length() == 0) {
                return null;
            }
            return sb.toString();
        }
    }
}
