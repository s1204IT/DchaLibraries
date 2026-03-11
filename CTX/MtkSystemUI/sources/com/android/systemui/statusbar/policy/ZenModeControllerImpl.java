package com.android.systemui.statusbar.policy;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.notification.Condition;
import android.service.notification.ZenModeConfig;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.util.Utils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Consumer;

public class ZenModeControllerImpl extends CurrentUserTracker implements ZenModeController {
    private static final boolean DEBUG = Log.isLoggable("ZenModeController", 3);
    private final AlarmManager mAlarmManager;
    private final ArrayList<ZenModeController.Callback> mCallbacks;
    private final LinkedHashMap<Uri, Condition> mConditions;
    private ZenModeConfig mConfig;
    private final GlobalSetting mConfigSetting;
    private final Context mContext;
    private final GlobalSetting mModeSetting;
    private final NotificationManager mNoMan;
    private final BroadcastReceiver mReceiver;
    private boolean mRegistered;
    private final SetupObserver mSetupObserver;
    private int mUserId;
    private final UserManager mUserManager;
    private int mZenMode;

    public ZenModeControllerImpl(Context context, Handler handler) {
        super(context);
        this.mCallbacks = new ArrayList<>();
        this.mConditions = new LinkedHashMap<>();
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if ("android.app.action.NEXT_ALARM_CLOCK_CHANGED".equals(intent.getAction())) {
                    ZenModeControllerImpl.this.fireNextAlarmChanged();
                }
                if ("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED".equals(intent.getAction())) {
                    ZenModeControllerImpl.this.fireEffectsSuppressorChanged();
                }
            }
        };
        this.mContext = context;
        this.mModeSetting = new GlobalSetting(this.mContext, handler, "zen_mode") {
            @Override
            protected void handleValueChanged(int i) {
                ZenModeControllerImpl.this.updateZenMode(i);
                ZenModeControllerImpl.this.fireZenChanged(i);
            }
        };
        this.mConfigSetting = new GlobalSetting(this.mContext, handler, "zen_mode_config_etag") {
            @Override
            protected void handleValueChanged(int i) {
                ZenModeControllerImpl.this.updateZenModeConfig();
            }
        };
        this.mNoMan = (NotificationManager) context.getSystemService("notification");
        this.mConfig = this.mNoMan.getZenModeConfig();
        this.mModeSetting.setListening(true);
        updateZenMode(this.mModeSetting.getValue());
        this.mConfigSetting.setListening(true);
        updateZenModeConfig();
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mSetupObserver = new SetupObserver(handler);
        this.mSetupObserver.register();
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
        startTracking();
    }

    @Override
    public boolean isVolumeRestricted() {
        return this.mUserManager.hasUserRestriction("no_adjust_volume", new UserHandle(this.mUserId));
    }

    @Override
    public boolean areNotificationsHiddenInShade() {
        return (this.mZenMode == 0 || (this.mConfig.suppressedVisualEffects & 256) == 0) ? false : true;
    }

    @Override
    public void addCallback(ZenModeController.Callback callback) {
        if (callback == null) {
            Slog.e("ZenModeController", "Attempted to add a null callback.");
        } else {
            this.mCallbacks.add(callback);
        }
    }

    @Override
    public void removeCallback(ZenModeController.Callback callback) {
        this.mCallbacks.remove(callback);
    }

    @Override
    public int getZen() {
        return this.mZenMode;
    }

    @Override
    public void setZen(int i, Uri uri, String str) {
        this.mNoMan.setZenMode(i, uri, str);
    }

    public boolean isZenAvailable() {
        return this.mSetupObserver.isDeviceProvisioned() && this.mSetupObserver.isUserSetup();
    }

    @Override
    public ZenModeConfig.ZenRule getManualRule() {
        if (this.mConfig == null) {
            return null;
        }
        return this.mConfig.manualRule;
    }

    @Override
    public ZenModeConfig getConfig() {
        return this.mConfig;
    }

    @Override
    public long getNextAlarm() {
        AlarmManager.AlarmClockInfo nextAlarmClock = this.mAlarmManager.getNextAlarmClock(this.mUserId);
        if (nextAlarmClock != null) {
            return nextAlarmClock.getTriggerTime();
        }
        return 0L;
    }

    @Override
    public void onUserSwitched(int i) {
        this.mUserId = i;
        if (this.mRegistered) {
            this.mContext.unregisterReceiver(this.mReceiver);
        }
        IntentFilter intentFilter = new IntentFilter("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
        intentFilter.addAction("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED");
        this.mContext.registerReceiverAsUser(this.mReceiver, new UserHandle(this.mUserId), intentFilter, null, null);
        this.mRegistered = true;
        this.mSetupObserver.register();
    }

    public void fireNextAlarmChanged() {
        Utils.safeForeach(this.mCallbacks, new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((ZenModeController.Callback) obj).onNextAlarmChanged();
            }
        });
    }

    public void fireEffectsSuppressorChanged() {
        Utils.safeForeach(this.mCallbacks, new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((ZenModeController.Callback) obj).onEffectsSupressorChanged();
            }
        });
    }

    public void fireZenChanged(final int i) {
        Utils.safeForeach(this.mCallbacks, new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((ZenModeController.Callback) obj).onZenChanged(i);
            }
        });
    }

    public void fireZenAvailableChanged(final boolean z) {
        Utils.safeForeach(this.mCallbacks, new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((ZenModeController.Callback) obj).onZenAvailableChanged(z);
            }
        });
    }

    private void fireManualRuleChanged(final ZenModeConfig.ZenRule zenRule) {
        Utils.safeForeach(this.mCallbacks, new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((ZenModeController.Callback) obj).onManualRuleChanged(zenRule);
            }
        });
    }

    @VisibleForTesting
    protected void fireConfigChanged(final ZenModeConfig zenModeConfig) {
        Utils.safeForeach(this.mCallbacks, new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((ZenModeController.Callback) obj).onConfigChanged(zenModeConfig);
            }
        });
    }

    @VisibleForTesting
    protected void updateZenMode(int i) {
        this.mZenMode = i;
    }

    @VisibleForTesting
    protected void updateZenModeConfig() {
        ZenModeConfig zenModeConfig = this.mNoMan.getZenModeConfig();
        if (Objects.equals(zenModeConfig, this.mConfig)) {
            return;
        }
        ZenModeConfig.ZenRule zenRule = this.mConfig != null ? this.mConfig.manualRule : null;
        this.mConfig = zenModeConfig;
        fireConfigChanged(zenModeConfig);
        ZenModeConfig.ZenRule zenRule2 = zenModeConfig != null ? zenModeConfig.manualRule : null;
        if (Objects.equals(zenRule, zenRule2)) {
            return;
        }
        fireManualRuleChanged(zenRule2);
    }

    private final class SetupObserver extends ContentObserver {
        private boolean mRegistered;
        private final ContentResolver mResolver;

        public SetupObserver(Handler handler) {
            super(handler);
            this.mResolver = ZenModeControllerImpl.this.mContext.getContentResolver();
        }

        public boolean isUserSetup() {
            return Settings.Secure.getIntForUser(this.mResolver, "user_setup_complete", 0, ZenModeControllerImpl.this.mUserId) != 0;
        }

        public boolean isDeviceProvisioned() {
            return Settings.Global.getInt(this.mResolver, "device_provisioned", 0) != 0;
        }

        public void register() {
            if (this.mRegistered) {
                this.mResolver.unregisterContentObserver(this);
            }
            this.mResolver.registerContentObserver(Settings.Global.getUriFor("device_provisioned"), false, this);
            this.mResolver.registerContentObserver(Settings.Secure.getUriFor("user_setup_complete"), false, this, ZenModeControllerImpl.this.mUserId);
            ZenModeControllerImpl.this.fireZenAvailableChanged(ZenModeControllerImpl.this.isZenAvailable());
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            if (Settings.Global.getUriFor("device_provisioned").equals(uri) || Settings.Secure.getUriFor("user_setup_complete").equals(uri)) {
                ZenModeControllerImpl.this.fireZenAvailableChanged(ZenModeControllerImpl.this.isZenAvailable());
            }
        }
    }
}
