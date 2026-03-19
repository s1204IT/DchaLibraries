package com.android.server;

import android.R;
import android.app.ActivityManagerNative;
import android.app.IApplicationThread;
import android.app.IUiModeManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.dreams.Sandman;
import android.util.Slog;
import com.android.internal.app.DisableCarModeActivity;
import com.android.server.pm.PackageManagerService;
import com.android.server.twilight.TwilightListener;
import com.android.server.twilight.TwilightManager;
import com.android.server.twilight.TwilightState;
import java.io.FileDescriptor;
import java.io.PrintWriter;

final class UiModeManagerService extends SystemService {
    private static final boolean ENABLE_LAUNCH_DESK_DOCK_APP = true;
    private static final boolean LOG = false;
    private static final String TAG = UiModeManager.class.getSimpleName();
    private final BroadcastReceiver mBatteryReceiver;
    private int mCarModeEnableFlags;
    private boolean mCarModeEnabled;
    private boolean mCarModeKeepsScreenOn;
    private boolean mCharging;
    private boolean mComputedNightMode;
    private Configuration mConfiguration;
    int mCurUiMode;
    private int mDefaultUiModeType;
    private boolean mDeskModeKeepsScreenOn;
    private final BroadcastReceiver mDockModeReceiver;
    private int mDockState;
    private boolean mEnableCarDockLaunch;
    private final Handler mHandler;
    private boolean mHoldingConfiguration;
    private int mLastBroadcastState;
    final Object mLock;
    private int mNightMode;
    private boolean mNightModeLocked;
    private NotificationManager mNotificationManager;
    private final BroadcastReceiver mResultReceiver;
    private final IBinder mService;
    private int mSetUiMode;
    private StatusBarManager mStatusBarManager;
    boolean mSystemReady;
    private boolean mTelevision;
    private final TwilightListener mTwilightListener;
    private TwilightManager mTwilightManager;
    private boolean mUiModeLocked;
    private PowerManager.WakeLock mWakeLock;
    private boolean mWatch;

    public UiModeManagerService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mDockState = 0;
        this.mLastBroadcastState = 0;
        this.mNightMode = 1;
        this.mCarModeEnabled = false;
        this.mCharging = false;
        this.mEnableCarDockLaunch = true;
        this.mUiModeLocked = false;
        this.mNightModeLocked = false;
        this.mCurUiMode = 0;
        this.mSetUiMode = 0;
        this.mHoldingConfiguration = false;
        this.mConfiguration = new Configuration();
        this.mHandler = new Handler();
        this.mResultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (getResultCode() != -1) {
                    return;
                }
                int enableFlags = intent.getIntExtra("enableFlags", 0);
                int disableFlags = intent.getIntExtra("disableFlags", 0);
                synchronized (UiModeManagerService.this.mLock) {
                    UiModeManagerService.this.updateAfterBroadcastLocked(intent.getAction(), enableFlags, disableFlags);
                }
            }
        };
        this.mDockModeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                int state = intent.getIntExtra("android.intent.extra.DOCK_STATE", 0);
                UiModeManagerService.this.updateDockState(state);
            }
        };
        this.mBatteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                UiModeManagerService.this.mCharging = intent.getIntExtra("plugged", 0) != 0;
                synchronized (UiModeManagerService.this.mLock) {
                    if (UiModeManagerService.this.mSystemReady) {
                        UiModeManagerService.this.updateLocked(0, 0);
                    }
                }
            }
        };
        this.mTwilightListener = new TwilightListener() {
            @Override
            public void onTwilightStateChanged() {
                UiModeManagerService.this.updateTwilight();
            }
        };
        this.mService = new IUiModeManager.Stub() {
            public void enableCarMode(int flags) {
                if (isUiModeLocked()) {
                    Slog.e(UiModeManagerService.TAG, "enableCarMode while UI mode is locked");
                    return;
                }
                long ident = Binder.clearCallingIdentity();
                try {
                    synchronized (UiModeManagerService.this.mLock) {
                        UiModeManagerService.this.setCarModeLocked(true, flags);
                        if (UiModeManagerService.this.mSystemReady) {
                            UiModeManagerService.this.updateLocked(flags, 0);
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }

            public void disableCarMode(int flags) {
                if (isUiModeLocked()) {
                    Slog.e(UiModeManagerService.TAG, "disableCarMode while UI mode is locked");
                    return;
                }
                long ident = Binder.clearCallingIdentity();
                try {
                    synchronized (UiModeManagerService.this.mLock) {
                        UiModeManagerService.this.setCarModeLocked(false, 0);
                        if (UiModeManagerService.this.mSystemReady) {
                            UiModeManagerService.this.updateLocked(0, flags);
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }

            public int getCurrentModeType() {
                int i;
                long ident = Binder.clearCallingIdentity();
                try {
                    synchronized (UiModeManagerService.this.mLock) {
                        i = UiModeManagerService.this.mCurUiMode & 15;
                    }
                    return i;
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }

            public void setNightMode(int mode) {
                if (isNightModeLocked() && UiModeManagerService.this.getContext().checkCallingOrSelfPermission("android.permission.MODIFY_DAY_NIGHT_MODE") != 0) {
                    Slog.e(UiModeManagerService.TAG, "Night mode locked, requires MODIFY_DAY_NIGHT_MODE permission");
                    return;
                }
                switch (mode) {
                    case 0:
                    case 1:
                    case 2:
                        long ident = Binder.clearCallingIdentity();
                        try {
                            synchronized (UiModeManagerService.this.mLock) {
                                if (UiModeManagerService.this.mNightMode != mode) {
                                    Settings.Secure.putInt(UiModeManagerService.this.getContext().getContentResolver(), "ui_night_mode", mode);
                                    UiModeManagerService.this.mNightMode = mode;
                                    UiModeManagerService.this.updateLocked(0, 0);
                                }
                                break;
                            }
                            return;
                        } finally {
                            Binder.restoreCallingIdentity(ident);
                        }
                    default:
                        throw new IllegalArgumentException("Unknown mode: " + mode);
                }
            }

            public int getNightMode() {
                int i;
                synchronized (UiModeManagerService.this.mLock) {
                    i = UiModeManagerService.this.mNightMode;
                }
                return i;
            }

            public boolean isUiModeLocked() {
                boolean z;
                synchronized (UiModeManagerService.this.mLock) {
                    z = UiModeManagerService.this.mUiModeLocked;
                }
                return z;
            }

            public boolean isNightModeLocked() {
                boolean z;
                synchronized (UiModeManagerService.this.mLock) {
                    z = UiModeManagerService.this.mNightModeLocked;
                }
                return z;
            }

            protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
                if (UiModeManagerService.this.getContext().checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                    pw.println("Permission Denial: can't dump uimode service from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
                } else {
                    UiModeManagerService.this.dumpImpl(pw);
                }
            }
        };
    }

    private static Intent buildHomeIntent(String category) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory(category);
        intent.setFlags(270532608);
        return intent;
    }

    @Override
    public void onStart() {
        Context context = getContext();
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mWakeLock = powerManager.newWakeLock(26, TAG);
        context.registerReceiver(this.mDockModeReceiver, new IntentFilter("android.intent.action.DOCK_EVENT"));
        context.registerReceiver(this.mBatteryReceiver, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        this.mConfiguration.setToDefaults();
        Resources res = context.getResources();
        this.mDefaultUiModeType = res.getInteger(R.integer.config_chooser_max_targets_per_row);
        this.mCarModeKeepsScreenOn = res.getInteger(R.integer.config_carDockRotation) == 1;
        this.mDeskModeKeepsScreenOn = res.getInteger(R.integer.config_cameraPrivacyLightAlsAveragingIntervalMillis) == 1;
        this.mEnableCarDockLaunch = res.getBoolean(R.^attr-private.dialogTitleIconsDecorLayout);
        this.mUiModeLocked = res.getBoolean(R.^attr-private.disableChildrenWhenDisabled);
        this.mNightModeLocked = res.getBoolean(R.^attr-private.dotActivatedColor);
        PackageManager pm = context.getPackageManager();
        this.mTelevision = pm.hasSystemFeature("android.hardware.type.television") ? true : pm.hasSystemFeature("android.software.leanback");
        this.mWatch = pm.hasSystemFeature("android.hardware.type.watch");
        int defaultNightMode = res.getInteger(R.integer.config_criticalBatteryWarningLevel);
        this.mNightMode = Settings.Secure.getInt(context.getContentResolver(), "ui_night_mode", defaultNightMode);
        synchronized (this) {
            updateConfigurationLocked();
            sendConfigurationLocked();
        }
        publishBinderService("uimode", this.mService);
    }

    void dumpImpl(PrintWriter pw) {
        synchronized (this.mLock) {
            pw.println("Current UI Mode Service state:");
            pw.print("  mDockState=");
            pw.print(this.mDockState);
            pw.print(" mLastBroadcastState=");
            pw.println(this.mLastBroadcastState);
            pw.print("  mNightMode=");
            pw.print(this.mNightMode);
            pw.print(" mNightModeLocked=");
            pw.print(this.mNightModeLocked);
            pw.print(" mCarModeEnabled=");
            pw.print(this.mCarModeEnabled);
            pw.print(" mComputedNightMode=");
            pw.print(this.mComputedNightMode);
            pw.print(" mCarModeEnableFlags=");
            pw.print(this.mCarModeEnableFlags);
            pw.print(" mEnableCarDockLaunch=");
            pw.println(this.mEnableCarDockLaunch);
            pw.print("  mCurUiMode=0x");
            pw.print(Integer.toHexString(this.mCurUiMode));
            pw.print(" mUiModeLocked=");
            pw.print(this.mUiModeLocked);
            pw.print(" mSetUiMode=0x");
            pw.println(Integer.toHexString(this.mSetUiMode));
            pw.print("  mHoldingConfiguration=");
            pw.print(this.mHoldingConfiguration);
            pw.print(" mSystemReady=");
            pw.println(this.mSystemReady);
            if (this.mTwilightManager != null) {
                pw.print("  mTwilightService.getCurrentState()=");
                pw.println(this.mTwilightManager.getCurrentState());
            }
        }
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase != 500) {
            return;
        }
        synchronized (this.mLock) {
            this.mTwilightManager = (TwilightManager) getLocalService(TwilightManager.class);
            if (this.mTwilightManager != null) {
                this.mTwilightManager.registerListener(this.mTwilightListener, this.mHandler);
            }
            this.mSystemReady = true;
            this.mCarModeEnabled = this.mDockState == 2;
            updateComputedNightModeLocked();
            updateLocked(0, 0);
        }
    }

    void setCarModeLocked(boolean enabled, int flags) {
        if (this.mCarModeEnabled != enabled) {
            this.mCarModeEnabled = enabled;
        }
        this.mCarModeEnableFlags = flags;
    }

    private void updateDockState(int newState) {
        synchronized (this.mLock) {
            if (newState != this.mDockState) {
                this.mDockState = newState;
                setCarModeLocked(this.mDockState == 2, 0);
                if (this.mSystemReady) {
                    updateLocked(1, 0);
                }
            }
        }
    }

    private static boolean isDeskDockState(int state) {
        switch (state) {
            case 1:
            case 3:
            case 4:
                return true;
            case 2:
            default:
                return false;
        }
    }

    private void updateConfigurationLocked() {
        int uiMode;
        int uiMode2 = this.mDefaultUiModeType;
        if (!this.mUiModeLocked) {
            if (this.mTelevision) {
                uiMode2 = 4;
            } else if (this.mWatch) {
                uiMode2 = 6;
            } else if (this.mCarModeEnabled) {
                uiMode2 = 3;
            } else if (isDeskDockState(this.mDockState)) {
                uiMode2 = 2;
            }
        }
        if (this.mNightMode == 0) {
            updateComputedNightModeLocked();
            uiMode = uiMode2 | (this.mComputedNightMode ? 32 : 16);
        } else {
            uiMode = uiMode2 | (this.mNightMode << 4);
        }
        this.mCurUiMode = uiMode;
        if (this.mHoldingConfiguration) {
            return;
        }
        this.mConfiguration.uiMode = uiMode;
    }

    private void sendConfigurationLocked() {
        if (this.mSetUiMode == this.mConfiguration.uiMode) {
            return;
        }
        this.mSetUiMode = this.mConfiguration.uiMode;
        try {
            ActivityManagerNative.getDefault().updateConfiguration(this.mConfiguration);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failure communicating with activity manager", e);
        }
    }

    void updateLocked(int enableFlags, int disableFlags) {
        boolean keepScreenOn;
        String action = null;
        String oldAction = null;
        if (this.mLastBroadcastState == 2) {
            adjustStatusBarCarModeLocked();
            oldAction = UiModeManager.ACTION_EXIT_CAR_MODE;
        } else if (isDeskDockState(this.mLastBroadcastState)) {
            oldAction = UiModeManager.ACTION_EXIT_DESK_MODE;
        }
        if (this.mCarModeEnabled) {
            if (this.mLastBroadcastState != 2) {
                adjustStatusBarCarModeLocked();
                if (oldAction != null) {
                    getContext().sendBroadcastAsUser(new Intent(oldAction), UserHandle.ALL);
                }
                this.mLastBroadcastState = 2;
                action = UiModeManager.ACTION_ENTER_CAR_MODE;
            }
        } else if (isDeskDockState(this.mDockState)) {
            if (!isDeskDockState(this.mLastBroadcastState)) {
                if (oldAction != null) {
                    getContext().sendBroadcastAsUser(new Intent(oldAction), UserHandle.ALL);
                }
                this.mLastBroadcastState = this.mDockState;
                action = UiModeManager.ACTION_ENTER_DESK_MODE;
            }
        } else {
            this.mLastBroadcastState = 0;
            action = oldAction;
        }
        if (action != null) {
            Intent intent = new Intent(action);
            intent.putExtra("enableFlags", enableFlags);
            intent.putExtra("disableFlags", disableFlags);
            getContext().sendOrderedBroadcastAsUser(intent, UserHandle.CURRENT, null, this.mResultReceiver, null, -1, null, null);
            this.mHoldingConfiguration = true;
            updateConfigurationLocked();
        } else {
            String category = null;
            if (this.mCarModeEnabled) {
                if (this.mEnableCarDockLaunch && (enableFlags & 1) != 0) {
                    category = "android.intent.category.CAR_DOCK";
                }
            } else if (isDeskDockState(this.mDockState)) {
                if ((enableFlags & 1) != 0) {
                    category = "android.intent.category.DESK_DOCK";
                }
            } else if ((disableFlags & 1) != 0) {
                category = "android.intent.category.HOME";
            }
            sendConfigurationAndStartDreamOrDockAppLocked(category);
        }
        if (!this.mCharging) {
            keepScreenOn = false;
        } else if (this.mCarModeEnabled && this.mCarModeKeepsScreenOn && (this.mCarModeEnableFlags & 2) == 0) {
            keepScreenOn = true;
        } else {
            keepScreenOn = this.mCurUiMode == 2 ? this.mDeskModeKeepsScreenOn : false;
        }
        if (keepScreenOn == this.mWakeLock.isHeld()) {
            return;
        }
        if (keepScreenOn) {
            this.mWakeLock.acquire();
        } else {
            this.mWakeLock.release();
        }
    }

    private void updateAfterBroadcastLocked(String action, int enableFlags, int disableFlags) {
        String category = null;
        if (UiModeManager.ACTION_ENTER_CAR_MODE.equals(action)) {
            if (this.mEnableCarDockLaunch && (enableFlags & 1) != 0) {
                category = "android.intent.category.CAR_DOCK";
            }
        } else if (UiModeManager.ACTION_ENTER_DESK_MODE.equals(action)) {
            if ((enableFlags & 1) != 0) {
                category = "android.intent.category.DESK_DOCK";
            }
        } else if ((disableFlags & 1) != 0) {
            category = "android.intent.category.HOME";
        }
        sendConfigurationAndStartDreamOrDockAppLocked(category);
    }

    private void sendConfigurationAndStartDreamOrDockAppLocked(String category) {
        this.mHoldingConfiguration = false;
        updateConfigurationLocked();
        boolean dockAppStarted = false;
        if (category != null) {
            Intent homeIntent = buildHomeIntent(category);
            if (Sandman.shouldStartDockApp(getContext(), homeIntent)) {
                try {
                    int result = ActivityManagerNative.getDefault().startActivityWithConfig((IApplicationThread) null, (String) null, homeIntent, (String) null, (IBinder) null, (String) null, 0, 0, this.mConfiguration, (Bundle) null, -2);
                    if (result >= 0) {
                        dockAppStarted = true;
                    } else if (result != -1) {
                        Slog.e(TAG, "Could not start dock app: " + homeIntent + ", startActivityWithConfig result " + result);
                    }
                } catch (RemoteException ex) {
                    Slog.e(TAG, "Could not start dock app: " + homeIntent, ex);
                }
            }
        }
        sendConfigurationLocked();
        if (category == null || dockAppStarted) {
            return;
        }
        Sandman.startDreamWhenDockedIfAppropriate(getContext());
    }

    private void adjustStatusBarCarModeLocked() {
        Context context = getContext();
        if (this.mStatusBarManager == null) {
            this.mStatusBarManager = (StatusBarManager) context.getSystemService("statusbar");
        }
        if (this.mStatusBarManager != null) {
            this.mStatusBarManager.disable(this.mCarModeEnabled ? PackageManagerService.DumpState.DUMP_FROZEN : 0);
        }
        if (this.mNotificationManager == null) {
            this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
        }
        if (this.mNotificationManager != null) {
            if (!this.mCarModeEnabled) {
                this.mNotificationManager.cancelAsUser(null, R.string.global_action_power_off, UserHandle.ALL);
                return;
            }
            Intent carModeOffIntent = new Intent(context, (Class<?>) DisableCarModeActivity.class);
            Notification.Builder n = new Notification.Builder(context).setSmallIcon(R.drawable.list_selector_background_default).setDefaults(4).setOngoing(true).setWhen(0L).setColor(context.getColor(R.color.system_accent3_600)).setContentTitle(context.getString(R.string.global_action_power_off)).setContentText(context.getString(R.string.global_action_power_options)).setContentIntent(PendingIntent.getActivityAsUser(context, 0, carModeOffIntent, 0, null, UserHandle.CURRENT));
            this.mNotificationManager.notifyAsUser(null, R.string.global_action_power_off, n.build(), UserHandle.ALL);
        }
    }

    void updateTwilight() {
        synchronized (this.mLock) {
            if (this.mNightMode == 0) {
                updateComputedNightModeLocked();
                updateLocked(0, 0);
            }
        }
    }

    private void updateComputedNightModeLocked() {
        TwilightState state;
        if (this.mTwilightManager == null || (state = this.mTwilightManager.getCurrentState()) == null) {
            return;
        }
        this.mComputedNightMode = state.isNight();
    }
}
