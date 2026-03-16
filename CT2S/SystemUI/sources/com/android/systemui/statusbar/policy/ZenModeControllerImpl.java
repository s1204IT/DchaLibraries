package com.android.systemui.statusbar.policy;

import android.app.AlarmManager;
import android.app.INotificationManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.Condition;
import android.service.notification.IConditionListener;
import android.service.notification.ZenModeConfig;
import android.util.Log;
import android.util.Slog;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.statusbar.policy.ZenModeController;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ZenModeControllerImpl implements ZenModeController {
    private static final boolean DEBUG = Log.isLoggable("ZenModeController", 3);
    private final AlarmManager mAlarmManager;
    private final GlobalSetting mConfigSetting;
    private final Context mContext;
    private final GlobalSetting mModeSetting;
    private final INotificationManager mNoMan;
    private boolean mRegistered;
    private boolean mRequesting;
    private final SetupObserver mSetupObserver;
    private int mUserId;
    private final ArrayList<ZenModeController.Callback> mCallbacks = new ArrayList<>();
    private final LinkedHashMap<Uri, Condition> mConditions = new LinkedHashMap<>();
    private final IConditionListener mListener = new IConditionListener.Stub() {
        public void onConditionsReceived(Condition[] conditions) {
            if (ZenModeControllerImpl.DEBUG) {
                Slog.d("ZenModeController", "onConditionsReceived " + (conditions == null ? 0 : conditions.length) + " mRequesting=" + ZenModeControllerImpl.this.mRequesting);
            }
            if (ZenModeControllerImpl.this.mRequesting) {
                ZenModeControllerImpl.this.updateConditions(conditions);
            }
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.app.action.NEXT_ALARM_CLOCK_CHANGED".equals(intent.getAction())) {
                ZenModeControllerImpl.this.fireNextAlarmChanged();
            }
            if ("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED".equals(intent.getAction())) {
                ZenModeControllerImpl.this.fireEffectsSuppressorChanged();
            }
        }
    };

    public ZenModeControllerImpl(Context context, Handler handler) {
        this.mContext = context;
        this.mModeSetting = new GlobalSetting(this.mContext, handler, "zen_mode") {
            @Override
            protected void handleValueChanged(int value) {
                ZenModeControllerImpl.this.fireZenChanged(value);
            }
        };
        this.mConfigSetting = new GlobalSetting(this.mContext, handler, "zen_mode_config_etag") {
            @Override
            protected void handleValueChanged(int value) {
                ZenModeControllerImpl.this.fireExitConditionChanged();
            }
        };
        this.mModeSetting.setListening(true);
        this.mConfigSetting.setListening(true);
        this.mNoMan = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mSetupObserver = new SetupObserver(handler);
        this.mSetupObserver.register();
    }

    @Override
    public void addCallback(ZenModeController.Callback callback) {
        this.mCallbacks.add(callback);
    }

    @Override
    public int getZen() {
        return this.mModeSetting.getValue();
    }

    @Override
    public void setZen(int zen) {
        this.mModeSetting.setValue(zen);
    }

    @Override
    public boolean isZenAvailable() {
        return this.mSetupObserver.isDeviceProvisioned() && this.mSetupObserver.isUserSetup();
    }

    @Override
    public void requestConditions(boolean request) {
        this.mRequesting = request;
        try {
            this.mNoMan.requestZenModeConditions(this.mListener, request ? 1 : 0);
        } catch (RemoteException e) {
        }
        if (!this.mRequesting) {
            this.mConditions.clear();
        }
    }

    @Override
    public void setExitCondition(Condition exitCondition) {
        try {
            this.mNoMan.setZenModeCondition(exitCondition);
        } catch (RemoteException e) {
        }
    }

    @Override
    public Condition getExitCondition() {
        try {
            ZenModeConfig config = this.mNoMan.getZenModeConfig();
            if (config != null) {
                return config.exitCondition;
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    @Override
    public void setUserId(int userId) {
        this.mUserId = userId;
        if (this.mRegistered) {
            this.mContext.unregisterReceiver(this.mReceiver);
        }
        IntentFilter filter = new IntentFilter("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
        filter.addAction("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED");
        this.mContext.registerReceiverAsUser(this.mReceiver, new UserHandle(this.mUserId), filter, null, null);
        this.mRegistered = true;
        this.mSetupObserver.register();
    }

    @Override
    public ComponentName getEffectsSuppressor() {
        return NotificationManager.from(this.mContext).getEffectsSuppressor();
    }

    private void fireNextAlarmChanged() {
        for (ZenModeController.Callback cb : this.mCallbacks) {
            cb.onNextAlarmChanged();
        }
    }

    private void fireEffectsSuppressorChanged() {
        for (ZenModeController.Callback cb : this.mCallbacks) {
            cb.onEffectsSupressorChanged();
        }
    }

    private void fireZenChanged(int zen) {
        for (ZenModeController.Callback cb : this.mCallbacks) {
            cb.onZenChanged(zen);
        }
    }

    private void fireZenAvailableChanged(boolean available) {
        for (ZenModeController.Callback cb : this.mCallbacks) {
            cb.onZenAvailableChanged(available);
        }
    }

    private void fireConditionsChanged(Condition[] conditions) {
        for (ZenModeController.Callback cb : this.mCallbacks) {
            cb.onConditionsChanged(conditions);
        }
    }

    private void fireExitConditionChanged() {
        Condition exitCondition = getExitCondition();
        if (DEBUG) {
            Slog.d("ZenModeController", "exitCondition changed: " + exitCondition);
        }
        for (ZenModeController.Callback cb : this.mCallbacks) {
            cb.onExitConditionChanged(exitCondition);
        }
    }

    private void updateConditions(Condition[] conditions) {
        if (conditions != null && conditions.length != 0) {
            for (Condition c : conditions) {
                if ((c.flags & 1) != 0) {
                    this.mConditions.put(c.id, c);
                }
            }
            fireConditionsChanged((Condition[]) this.mConditions.values().toArray(new Condition[this.mConditions.values().size()]));
        }
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
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.Global.getUriFor("device_provisioned").equals(uri) || Settings.Secure.getUriFor("user_setup_complete").equals(uri)) {
                ZenModeControllerImpl.this.fireZenAvailableChanged(ZenModeControllerImpl.this.isZenAvailable());
            }
        }
    }
}
