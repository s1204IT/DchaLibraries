package com.android.settings.notification;

import android.app.ActivityManager;
import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.content.Context;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.util.Log;
import com.android.settings.R;

/* loaded from: classes.dex */
public class ZenModeBackend {
    protected static final String ZEN_MODE_FROM_ANYONE = "zen_mode_from_anyone";
    protected static final String ZEN_MODE_FROM_CONTACTS = "zen_mode_from_contacts";
    protected static final String ZEN_MODE_FROM_NONE = "zen_mode_from_none";
    protected static final String ZEN_MODE_FROM_STARRED = "zen_mode_from_starred";
    private static ZenModeBackend sInstance;
    private String TAG = "ZenModeSettingsBackend";
    private final Context mContext;
    private final NotificationManager mNotificationManager;
    protected NotificationManager.Policy mPolicy;
    protected int mZenMode;

    public static ZenModeBackend getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ZenModeBackend(context);
        }
        return sInstance;
    }

    public ZenModeBackend(Context context) {
        this.mContext = context;
        this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
        updateZenMode();
        updatePolicy();
    }

    protected void updatePolicy() {
        if (this.mNotificationManager != null) {
            this.mPolicy = this.mNotificationManager.getNotificationPolicy();
        }
    }

    protected void updateZenMode() {
        this.mZenMode = Settings.Global.getInt(this.mContext.getContentResolver(), "zen_mode", this.mZenMode);
    }

    protected boolean setZenRule(String str, AutomaticZenRule automaticZenRule) {
        return NotificationManager.from(this.mContext).updateAutomaticZenRule(str, automaticZenRule);
    }

    protected void setZenMode(int i) {
        NotificationManager.from(this.mContext).setZenMode(i, null, this.TAG);
        this.mZenMode = getZenMode();
    }

    protected void setZenModeForDuration(int i) {
        this.mNotificationManager.setZenMode(1, ZenModeConfig.toTimeCondition(this.mContext, i, ActivityManager.getCurrentUser(), true).id, this.TAG);
        this.mZenMode = getZenMode();
    }

    protected int getZenMode() {
        this.mZenMode = Settings.Global.getInt(this.mContext.getContentResolver(), "zen_mode", this.mZenMode);
        return this.mZenMode;
    }

    protected boolean isVisualEffectSuppressed(int i) {
        return (i & this.mPolicy.suppressedVisualEffects) != 0;
    }

    protected boolean isPriorityCategoryEnabled(int i) {
        return (i & this.mPolicy.priorityCategories) != 0;
    }

    protected int getNewPriorityCategories(boolean z, int i) {
        int i2 = this.mPolicy.priorityCategories;
        if (z) {
            return i2 | i;
        }
        return (~i) & i2;
    }

    protected int getPriorityCallSenders() {
        if (isPriorityCategoryEnabled(8)) {
            return this.mPolicy.priorityCallSenders;
        }
        return -1;
    }

    protected int getPriorityMessageSenders() {
        if (isPriorityCategoryEnabled(4)) {
            return this.mPolicy.priorityMessageSenders;
        }
        return -1;
    }

    protected void saveVisualEffectsPolicy(int i, boolean z) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "zen_settings_updated", 1);
        savePolicy(this.mPolicy.priorityCategories, this.mPolicy.priorityCallSenders, this.mPolicy.priorityMessageSenders, getNewSuppressedEffects(z, i));
    }

    protected void saveSoundPolicy(int i, boolean z) {
        savePolicy(getNewPriorityCategories(z, i), this.mPolicy.priorityCallSenders, this.mPolicy.priorityMessageSenders, this.mPolicy.suppressedVisualEffects);
    }

    protected void savePolicy(int i, int i2, int i3, int i4) {
        this.mPolicy = new NotificationManager.Policy(i, i2, i3, i4);
        this.mNotificationManager.setNotificationPolicy(this.mPolicy);
    }

    private int getNewSuppressedEffects(boolean z, int i) {
        int i2;
        int i3 = this.mPolicy.suppressedVisualEffects;
        if (z) {
            i2 = i3 | i;
        } else {
            i2 = (~i) & i3;
        }
        return clearDeprecatedEffects(i2);
    }

    private int clearDeprecatedEffects(int i) {
        return i & (-4);
    }

    protected void saveSenders(int i, int i2) {
        int priorityCallSenders = getPriorityCallSenders();
        int priorityMessageSenders = getPriorityMessageSenders();
        int prioritySenders = getPrioritySenders(i);
        boolean z = i2 != -1;
        if (i2 == -1) {
            i2 = prioritySenders;
        }
        String str = "";
        if (i == 8) {
            str = "Calls";
            priorityCallSenders = i2;
        }
        if (i == 4) {
            str = "Messages";
            priorityMessageSenders = i2;
        }
        savePolicy(getNewPriorityCategories(z, i), priorityCallSenders, priorityMessageSenders, this.mPolicy.suppressedVisualEffects);
        if (ZenModeSettingsBase.DEBUG) {
            Log.d(this.TAG, "onPrefChange allow" + str + "=" + z + " allow" + str + "From=" + ZenModeConfig.sourceToString(i2));
        }
    }

    private int getPrioritySenders(int i) {
        if (i == 8) {
            return getPriorityCallSenders();
        }
        if (i == 4) {
            return getPriorityMessageSenders();
        }
        return -1;
    }

    protected static String getKeyFromSetting(int i) {
        switch (i) {
            case 0:
                return ZEN_MODE_FROM_ANYONE;
            case 1:
                return ZEN_MODE_FROM_CONTACTS;
            case 2:
                return ZEN_MODE_FROM_STARRED;
            default:
                return ZEN_MODE_FROM_NONE;
        }
    }

    protected int getContactsSummary(int i) {
        int priorityCallSenders = -1;
        if (i == -1) {
            return R.string.zen_mode_from_none;
        }
        if (i == 4) {
            if (isPriorityCategoryEnabled(i)) {
                priorityCallSenders = getPriorityMessageSenders();
            }
        } else if (i == 8 && isPriorityCategoryEnabled(i)) {
            priorityCallSenders = getPriorityCallSenders();
        }
        switch (priorityCallSenders) {
        }
        return R.string.zen_mode_from_none;
    }

    /* JADX WARN: Removed duplicated region for block: B:23:0x0045  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    protected static int getSettingFromPrefKey(String str) {
        char c;
        int iHashCode = str.hashCode();
        if (iHashCode != -946901971) {
            if (iHashCode != -423126328) {
                if (iHashCode != 187510959) {
                    c = (iHashCode == 462773226 && str.equals(ZEN_MODE_FROM_STARRED)) ? (char) 2 : (char) 65535;
                } else if (str.equals(ZEN_MODE_FROM_ANYONE)) {
                    c = 0;
                }
            } else if (str.equals(ZEN_MODE_FROM_CONTACTS)) {
                c = 1;
            }
        } else if (str.equals(ZEN_MODE_FROM_NONE)) {
            c = 3;
        }
        switch (c) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                return -1;
        }
    }

    public boolean removeZenRule(String str) {
        return NotificationManager.from(this.mContext).removeAutomaticZenRule(str);
    }

    protected String addZenRule(AutomaticZenRule automaticZenRule) {
        try {
            String strAddAutomaticZenRule = NotificationManager.from(this.mContext).addAutomaticZenRule(automaticZenRule);
            NotificationManager.from(this.mContext).getAutomaticZenRule(strAddAutomaticZenRule);
            return strAddAutomaticZenRule;
        } catch (Exception e) {
            return null;
        }
    }
}
