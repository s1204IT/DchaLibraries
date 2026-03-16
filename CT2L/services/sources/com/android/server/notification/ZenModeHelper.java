package com.android.server.notification;

import android.R;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.ContentObserver;
import android.media.AudioManagerInternal;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.telecom.TelecomManager;
import android.util.Log;
import android.util.Slog;
import com.android.server.LocalServices;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Objects;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class ZenModeHelper implements AudioManagerInternal.RingerModeDelegate {
    private final AppOpsManager mAppOps;
    private AudioManagerInternal mAudioManager;
    private ZenModeConfig mConfig;
    private final Context mContext;
    private final ZenModeConfig mDefaultConfig;
    private ComponentName mDefaultPhoneApp;
    private boolean mEffectsSuppressed;
    private final H mHandler;
    private final SettingsObserver mSettingsObserver;
    private int mZenMode;
    private static final String TAG = "ZenModeHelper";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private final ArrayList<Callback> mCallbacks = new ArrayList<>();
    private int mPreviousRingerMode = -1;

    public ZenModeHelper(Context context, Looper looper) {
        this.mContext = context;
        this.mHandler = new H(looper);
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        this.mDefaultConfig = readDefaultConfig(context.getResources());
        this.mConfig = this.mDefaultConfig;
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mSettingsObserver.observe();
    }

    public static ZenModeConfig readDefaultConfig(Resources resources) {
        XmlResourceParser parser = null;
        try {
            parser = resources.getXml(R.bool.config_showDefaultHome);
            while (parser.next() != 1) {
                ZenModeConfig config = ZenModeConfig.readXml(parser);
                if (config != null) {
                    return config;
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error reading default zen mode config from resource", e);
        } finally {
            IoUtils.closeQuietly(parser);
        }
        return new ZenModeConfig();
    }

    public void addCallback(Callback callback) {
        this.mCallbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        this.mCallbacks.remove(callback);
    }

    public void onSystemReady() {
        this.mAudioManager = (AudioManagerInternal) LocalServices.getService(AudioManagerInternal.class);
        if (this.mAudioManager != null) {
            this.mAudioManager.setRingerModeDelegate(this);
        }
    }

    public int getZenModeListenerInterruptionFilter() {
        switch (this.mZenMode) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            default:
                return 0;
        }
    }

    private static int zenModeFromListenerInterruptionFilter(int listenerInterruptionFilter, int defValue) {
        switch (listenerInterruptionFilter) {
            case 1:
                return 0;
            case 2:
                return 1;
            case 3:
                return 2;
            default:
                return defValue;
        }
    }

    public void requestFromListener(ComponentName name, int interruptionFilter) {
        int newZen = zenModeFromListenerInterruptionFilter(interruptionFilter, -1);
        if (newZen != -1) {
            setZenMode(newZen, "listener:" + (name != null ? name.flattenToShortString() : null));
        }
    }

    public void setEffectsSuppressed(boolean effectsSuppressed) {
        if (this.mEffectsSuppressed != effectsSuppressed) {
            this.mEffectsSuppressed = effectsSuppressed;
            applyRestrictions();
        }
    }

    public boolean shouldIntercept(NotificationRecord record) {
        if (isSystem(record)) {
            return false;
        }
        switch (this.mZenMode) {
            case 1:
                if (!isAlarm(record)) {
                    if (record.getPackagePriority() == 2) {
                        ZenLog.traceNotIntercepted(record, "priorityApp");
                    } else if (isCall(record)) {
                        if (!this.mConfig.allowCalls) {
                            ZenLog.traceIntercepted(record, "!allowCalls");
                        }
                    } else if (isMessage(record)) {
                        if (!this.mConfig.allowMessages) {
                            ZenLog.traceIntercepted(record, "!allowMessages");
                        }
                    } else if (isEvent(record)) {
                        if (!this.mConfig.allowEvents) {
                            ZenLog.traceIntercepted(record, "!allowEvents");
                        }
                    } else {
                        ZenLog.traceIntercepted(record, "!priority");
                    }
                }
                break;
            case 2:
                ZenLog.traceIntercepted(record, "none");
                break;
        }
        return false;
    }

    private boolean shouldInterceptAudience(NotificationRecord record) {
        if (audienceMatches(record.getContactAffinity())) {
            return false;
        }
        ZenLog.traceIntercepted(record, "!audienceMatches");
        return true;
    }

    public int getZenMode() {
        return this.mZenMode;
    }

    public void setZenMode(int zenMode, String reason) {
        setZenMode(zenMode, reason, true);
    }

    private void setZenMode(int zenMode, String reason, boolean setRingerMode) {
        ZenLog.traceSetZenMode(zenMode, reason);
        if (this.mZenMode != zenMode) {
            ZenLog.traceUpdateZenMode(this.mZenMode, zenMode);
            this.mZenMode = zenMode;
            Settings.Global.putInt(this.mContext.getContentResolver(), "zen_mode", this.mZenMode);
            if (setRingerMode) {
                applyZenToRingerMode();
            }
            applyRestrictions();
            this.mHandler.postDispatchOnZenModeChanged();
        }
    }

    public void readZenModeFromSetting() {
        int newMode = Settings.Global.getInt(this.mContext.getContentResolver(), "zen_mode", 0);
        setZenMode(newMode, "setting");
    }

    private void applyRestrictions() {
        boolean zen = this.mZenMode != 0;
        boolean muteNotifications = this.mEffectsSuppressed;
        applyRestrictions(muteNotifications, 5);
        boolean muteCalls = (zen && !this.mConfig.allowCalls) || this.mEffectsSuppressed;
        applyRestrictions(muteCalls, 6);
        boolean muteAlarms = this.mZenMode == 2;
        applyRestrictions(muteAlarms, 4);
    }

    private void applyRestrictions(boolean mute, int usage) {
        this.mAppOps.setRestriction(3, usage, mute ? 1 : 0, null);
        this.mAppOps.setRestriction(28, usage, mute ? 1 : 0, null);
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mZenMode=");
        pw.println(Settings.Global.zenModeToString(this.mZenMode));
        pw.print(prefix);
        pw.print("mConfig=");
        pw.println(this.mConfig);
        pw.print(prefix);
        pw.print("mDefaultConfig=");
        pw.println(this.mDefaultConfig);
        pw.print(prefix);
        pw.print("mPreviousRingerMode=");
        pw.println(this.mPreviousRingerMode);
        pw.print(prefix);
        pw.print("mDefaultPhoneApp=");
        pw.println(this.mDefaultPhoneApp);
        pw.print(prefix);
        pw.print("mEffectsSuppressed=");
        pw.println(this.mEffectsSuppressed);
    }

    public void readXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        ZenModeConfig config = ZenModeConfig.readXml(parser);
        if (config != null) {
            setConfig(config);
        }
    }

    public void writeXml(XmlSerializer out) throws IOException {
        this.mConfig.writeXml(out);
    }

    public ZenModeConfig getConfig() {
        return this.mConfig;
    }

    public boolean setConfig(ZenModeConfig config) {
        if (config == null || !config.isValid()) {
            return false;
        }
        if (config.equals(this.mConfig)) {
            return true;
        }
        ZenLog.traceConfig(this.mConfig, config);
        this.mConfig = config;
        dispatchOnConfigChanged();
        String val = Integer.toString(this.mConfig.hashCode());
        Settings.Global.putString(this.mContext.getContentResolver(), "zen_mode_config_etag", val);
        applyRestrictions();
        return true;
    }

    private void applyZenToRingerMode() {
        if (this.mAudioManager != null) {
            int ringerModeInternal = this.mAudioManager.getRingerModeInternal();
            int newRingerModeInternal = ringerModeInternal;
            switch (this.mZenMode) {
                case 0:
                case 1:
                    if (ringerModeInternal == 0) {
                        newRingerModeInternal = this.mPreviousRingerMode != -1 ? this.mPreviousRingerMode : 2;
                        this.mPreviousRingerMode = -1;
                    }
                    break;
                case 2:
                    if (ringerModeInternal != 0) {
                        this.mPreviousRingerMode = ringerModeInternal;
                        newRingerModeInternal = 0;
                    }
                    break;
            }
            if (newRingerModeInternal != -1) {
                this.mAudioManager.setRingerModeInternal(newRingerModeInternal, TAG);
            }
        }
    }

    public int onSetRingerModeInternal(int ringerModeOld, int ringerModeNew, String caller, int ringerModeExternal) {
        boolean isChange = ringerModeOld != ringerModeNew;
        int ringerModeExternalOut = ringerModeNew;
        int newZen = -1;
        switch (ringerModeNew) {
            case 0:
                if (isChange && this.mZenMode != 2) {
                    newZen = 2;
                }
                break;
            case 1:
            case 2:
                if (isChange && ringerModeOld == 0 && this.mZenMode == 2) {
                    newZen = 0;
                } else if (this.mZenMode != 0) {
                    ringerModeExternalOut = 0;
                }
                break;
        }
        if (newZen != -1) {
            setZenMode(newZen, "ringerModeInternal", false);
        }
        if (isChange || newZen != -1 || ringerModeExternal != ringerModeExternalOut) {
            ZenLog.traceSetRingerModeInternal(ringerModeOld, ringerModeNew, caller, ringerModeExternal, ringerModeExternalOut);
        }
        return ringerModeExternalOut;
    }

    public int onSetRingerModeExternal(int ringerModeOld, int ringerModeNew, String caller, int ringerModeInternal) {
        int ringerModeInternalOut = ringerModeNew;
        boolean isChange = ringerModeOld != ringerModeNew;
        boolean isVibrate = ringerModeInternal == 1;
        int newZen = -1;
        switch (ringerModeNew) {
            case 0:
                if (isChange) {
                    if (this.mZenMode == 0) {
                        newZen = 1;
                    }
                    ringerModeInternalOut = !isVibrate ? 2 : 1;
                } else {
                    ringerModeInternalOut = ringerModeInternal;
                }
                break;
            case 1:
            case 2:
                if (this.mZenMode != 0) {
                    newZen = 0;
                }
                break;
        }
        if (newZen != -1) {
            setZenMode(newZen, "ringerModeExternal", false);
        }
        ZenLog.traceSetRingerModeExternal(ringerModeOld, ringerModeNew, caller, ringerModeInternal, ringerModeInternalOut);
        return ringerModeInternalOut;
    }

    private void dispatchOnConfigChanged() {
        for (Callback callback : this.mCallbacks) {
            callback.onConfigChanged();
        }
    }

    private void dispatchOnZenModeChanged() {
        for (Callback callback : this.mCallbacks) {
            callback.onZenModeChanged();
        }
    }

    private static boolean isSystem(NotificationRecord record) {
        return record.isCategory("sys");
    }

    private static boolean isAlarm(NotificationRecord record) {
        return record.isCategory("alarm") || record.isAudioStream(4) || record.isAudioAttributesUsage(4);
    }

    private static boolean isEvent(NotificationRecord record) {
        return record.isCategory("event");
    }

    public boolean isCall(NotificationRecord record) {
        return record != null && (isDefaultPhoneApp(record.sbn.getPackageName()) || record.isCategory("call"));
    }

    private boolean isDefaultPhoneApp(String pkg) {
        if (this.mDefaultPhoneApp == null) {
            TelecomManager telecomm = (TelecomManager) this.mContext.getSystemService("telecom");
            this.mDefaultPhoneApp = telecomm != null ? telecomm.getDefaultPhoneApp() : null;
            if (DEBUG) {
                Slog.d(TAG, "Default phone app: " + this.mDefaultPhoneApp);
            }
        }
        return (pkg == null || this.mDefaultPhoneApp == null || !pkg.equals(this.mDefaultPhoneApp.getPackageName())) ? false : true;
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
        return record.isCategory("msg") || isDefaultMessagingApp(record);
    }

    public boolean matchesCallFilter(UserHandle userHandle, Bundle extras, ValidateNotificationPeople validator, int contactsTimeoutMs, float timeoutAffinity) {
        int zen = this.mZenMode;
        if (zen == 2) {
            return false;
        }
        if (zen == 1) {
            if (!this.mConfig.allowCalls) {
                return false;
            }
            if (validator != null) {
                float contactAffinity = validator.getContactAffinity(userHandle, extras, contactsTimeoutMs, timeoutAffinity);
                return audienceMatches(contactAffinity);
            }
        }
        return true;
    }

    public String toString() {
        return TAG;
    }

    private boolean audienceMatches(float contactAffinity) {
        switch (this.mConfig.allowFrom) {
            case 0:
                break;
            case 1:
                if (contactAffinity < 0.5f) {
                }
                break;
            case 2:
                if (contactAffinity < 1.0f) {
                }
                break;
            default:
                Slog.w(TAG, "Encountered unknown source: " + this.mConfig.allowFrom);
                break;
        }
        return true;
    }

    private class SettingsObserver extends ContentObserver {
        private final Uri ZEN_MODE;

        public SettingsObserver(Handler handler) {
            super(handler);
            this.ZEN_MODE = Settings.Global.getUriFor("zen_mode");
        }

        public void observe() {
            ContentResolver resolver = ZenModeHelper.this.mContext.getContentResolver();
            resolver.registerContentObserver(this.ZEN_MODE, false, this);
            update(null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            update(uri);
        }

        public void update(Uri uri) {
            if (this.ZEN_MODE.equals(uri)) {
                ZenModeHelper.this.readZenModeFromSetting();
            }
        }
    }

    private class H extends Handler {
        private static final int MSG_DISPATCH = 1;

        private H(Looper looper) {
            super(looper);
        }

        private void postDispatchOnZenModeChanged() {
            removeMessages(1);
            sendEmptyMessage(1);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ZenModeHelper.this.dispatchOnZenModeChanged();
                    break;
            }
        }
    }

    public static class Callback {
        void onConfigChanged() {
        }

        void onZenModeChanged() {
        }
    }
}
