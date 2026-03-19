package com.android.server.notification;

import android.R;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.telecom.TelecomManager;
import android.util.ArrayMap;
import android.util.Slog;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Objects;

public class ZenModeFiltering {
    private static final boolean DEBUG = ZenModeHelper.DEBUG;
    static final RepeatCallers REPEAT_CALLERS = new RepeatCallers(null);
    private static final String TAG = "ZenModeHelper";
    private final Context mContext;
    private ComponentName mDefaultPhoneApp;

    public ZenModeFiltering(Context context) {
        this.mContext = context;
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mDefaultPhoneApp=");
        pw.println(this.mDefaultPhoneApp);
        pw.print(prefix);
        pw.print("RepeatCallers.mThresholdMinutes=");
        pw.println(REPEAT_CALLERS.mThresholdMinutes);
        synchronized (REPEAT_CALLERS) {
            if (!REPEAT_CALLERS.mCalls.isEmpty()) {
                pw.print(prefix);
                pw.println("RepeatCallers.mCalls=");
                for (int i = 0; i < REPEAT_CALLERS.mCalls.size(); i++) {
                    pw.print(prefix);
                    pw.print("  ");
                    pw.print((String) REPEAT_CALLERS.mCalls.keyAt(i));
                    pw.print(" at ");
                    pw.println(ts(((Long) REPEAT_CALLERS.mCalls.valueAt(i)).longValue()));
                }
            }
        }
    }

    private static String ts(long time) {
        return new Date(time) + " (" + time + ")";
    }

    public static boolean matchesCallFilter(Context context, int zen, ZenModeConfig config, UserHandle userHandle, Bundle extras, ValidateNotificationPeople validator, int contactsTimeoutMs, float timeoutAffinity) {
        if (zen == 2 || zen == 3) {
            return false;
        }
        if (zen != 1 || (config.allowRepeatCallers && REPEAT_CALLERS.isRepeat(context, extras))) {
            return true;
        }
        if (!config.allowCalls) {
            return false;
        }
        if (validator != null) {
            float contactAffinity = validator.getContactAffinity(userHandle, extras, contactsTimeoutMs, timeoutAffinity);
            return audienceMatches(config.allowCallsFrom, contactAffinity);
        }
        return true;
    }

    private static Bundle extras(NotificationRecord record) {
        if (record == null || record.sbn == null || record.sbn.getNotification() == null) {
            return null;
        }
        return record.sbn.getNotification().extras;
    }

    public boolean shouldIntercept(int zen, ZenModeConfig config, NotificationRecord record) {
        if (isSystem(record)) {
            return false;
        }
        switch (zen) {
            case 1:
                if (!isAlarm(record)) {
                    if (record.getPackagePriority() == 2) {
                        ZenLog.traceNotIntercepted(record, "priorityApp");
                    } else if (isCall(record)) {
                        if (config.allowRepeatCallers && REPEAT_CALLERS.isRepeat(this.mContext, extras(record))) {
                            ZenLog.traceNotIntercepted(record, "repeatCaller");
                        } else if (!config.allowCalls) {
                            ZenLog.traceIntercepted(record, "!allowCalls");
                        }
                    } else if (isMessage(record)) {
                        if (!config.allowMessages) {
                            ZenLog.traceIntercepted(record, "!allowMessages");
                        }
                    } else if (isEvent(record)) {
                        if (!config.allowEvents) {
                            ZenLog.traceIntercepted(record, "!allowEvents");
                        }
                    } else if (isReminder(record)) {
                        if (!config.allowReminders) {
                            ZenLog.traceIntercepted(record, "!allowReminders");
                        }
                    } else {
                        ZenLog.traceIntercepted(record, "!priority");
                    }
                    break;
                }
                break;
            case 2:
                ZenLog.traceIntercepted(record, "none");
                break;
            case 3:
                if (!isAlarm(record)) {
                    ZenLog.traceIntercepted(record, "alarmsOnly");
                    break;
                }
                break;
        }
        return false;
    }

    private static boolean shouldInterceptAudience(int source, NotificationRecord record) {
        if (!audienceMatches(source, record.getContactAffinity())) {
            ZenLog.traceIntercepted(record, "!audienceMatches");
            return true;
        }
        return false;
    }

    private static boolean isSystem(NotificationRecord record) {
        return record.isCategory("sys");
    }

    private static boolean isAlarm(NotificationRecord record) {
        if (record.isCategory("alarm") || record.isAudioStream(4)) {
            return true;
        }
        return record.isAudioAttributesUsage(4);
    }

    private static boolean isEvent(NotificationRecord record) {
        return record.isCategory("event");
    }

    private static boolean isReminder(NotificationRecord record) {
        return record.isCategory("reminder");
    }

    public boolean isCall(NotificationRecord record) {
        if (record == null) {
            return false;
        }
        if (isDefaultPhoneApp(record.sbn.getPackageName())) {
            return true;
        }
        return record.isCategory("call");
    }

    private boolean isDefaultPhoneApp(String pkg) {
        if (this.mDefaultPhoneApp == null) {
            TelecomManager telecomm = (TelecomManager) this.mContext.getSystemService("telecom");
            this.mDefaultPhoneApp = telecomm != null ? telecomm.getDefaultPhoneApp() : null;
            if (DEBUG) {
                Slog.d(TAG, "Default phone app: " + this.mDefaultPhoneApp);
            }
        }
        if (pkg == null || this.mDefaultPhoneApp == null) {
            return false;
        }
        return pkg.equals(this.mDefaultPhoneApp.getPackageName());
    }

    private boolean isDefaultMessagingApp(NotificationRecord record) {
        int userId = record.getUserId();
        if (userId == -10000 || userId == -1) {
            return false;
        }
        String defaultApp = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "sms_default_application", userId);
        return Objects.equals(defaultApp, record.sbn.getPackageName());
    }

    private boolean isMessage(NotificationRecord record) {
        if (record.isCategory("msg")) {
            return true;
        }
        return isDefaultMessagingApp(record);
    }

    private static boolean audienceMatches(int source, float contactAffinity) {
        switch (source) {
            case 0:
                break;
            case 1:
                if (contactAffinity < 0.5f) {
                    break;
                }
                break;
            case 2:
                if (contactAffinity < 1.0f) {
                    break;
                }
                break;
            default:
                Slog.w(TAG, "Encountered unknown source: " + source);
                break;
        }
        return true;
    }

    private static class RepeatCallers {
        private final ArrayMap<String, Long> mCalls;
        private int mThresholdMinutes;

        RepeatCallers(RepeatCallers repeatCallers) {
            this();
        }

        private RepeatCallers() {
            this.mCalls = new ArrayMap<>();
        }

        private synchronized boolean isRepeat(Context context, Bundle extras) {
            if (this.mThresholdMinutes <= 0) {
                this.mThresholdMinutes = context.getResources().getInteger(R.integer.config_dreamsBatteryLevelDrainCutoff);
            }
            if (this.mThresholdMinutes <= 0 || extras == null) {
                return false;
            }
            String peopleString = peopleString(extras);
            if (peopleString == null) {
                return false;
            }
            long now = System.currentTimeMillis();
            int N = this.mCalls.size();
            for (int i = N - 1; i >= 0; i--) {
                long time = this.mCalls.valueAt(i).longValue();
                if (time > now || now - time > this.mThresholdMinutes * 1000 * 60) {
                    this.mCalls.removeAt(i);
                }
            }
            boolean isRepeat = this.mCalls.containsKey(peopleString);
            this.mCalls.put(peopleString, Long.valueOf(now));
            return isRepeat;
        }

        private static String peopleString(Bundle extras) {
            String[] extraPeople = ValidateNotificationPeople.getExtraPeople(extras);
            if (extraPeople == null || extraPeople.length == 0) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (String extraPerson : extraPeople) {
                if (extraPerson != null) {
                    String extraPerson2 = extraPerson.trim();
                    if (!extraPerson2.isEmpty()) {
                        if (sb.length() > 0) {
                            sb.append('|');
                        }
                        sb.append(extraPerson2);
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
