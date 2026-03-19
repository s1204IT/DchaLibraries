package com.android.server;

import android.R;
import android.app.ActivityManager;
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
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.dreams.Sandman;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.util.Slog;
import com.android.internal.app.DisableCarModeActivity;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.DumpUtils;
import com.android.server.pm.DumpState;
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
    private final IUiModeManager.Stub mService;
    private int mSetUiMode;
    private StatusBarManager mStatusBarManager;
    boolean mSystemReady;
    private boolean mTelevision;
    private final TwilightListener mTwilightListener;
    private TwilightManager mTwilightManager;
    private boolean mUiModeLocked;
    private boolean mVrHeadset;
    private final IVrStateCallbacks mVrStateCallbacks;
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
                int intExtra = intent.getIntExtra("enableFlags", 0);
                int intExtra2 = intent.getIntExtra("disableFlags", 0);
                synchronized (UiModeManagerService.this.mLock) {
                    UiModeManagerService.this.updateAfterBroadcastLocked(intent.getAction(), intExtra, intExtra2);
                }
            }
        };
        this.mDockModeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                UiModeManagerService.this.updateDockState(intent.getIntExtra("android.intent.extra.DOCK_STATE", 0));
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
            public void onTwilightStateChanged(TwilightState twilightState) {
                synchronized (UiModeManagerService.this.mLock) {
                    if (UiModeManagerService.this.mNightMode == 0) {
                        UiModeManagerService.this.updateComputedNightModeLocked();
                        UiModeManagerService.this.updateLocked(0, 0);
                    }
                }
            }
        };
        this.mVrStateCallbacks = new IVrStateCallbacks.Stub() {
            public void onVrStateChanged(boolean z) {
                synchronized (UiModeManagerService.this.mLock) {
                    UiModeManagerService.this.mVrHeadset = z;
                    if (UiModeManagerService.this.mSystemReady) {
                        UiModeManagerService.this.updateLocked(0, 0);
                    }
                }
            }
        };
        this.mService = new IUiModeManager.Stub() {
            public void enableCarMode(int i) {
                if (isUiModeLocked()) {
                    Slog.e(UiModeManagerService.TAG, "enableCarMode while UI mode is locked");
                    return;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (UiModeManagerService.this.mLock) {
                        UiModeManagerService.this.setCarModeLocked(true, i);
                        if (UiModeManagerService.this.mSystemReady) {
                            UiModeManagerService.this.updateLocked(i, 0);
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public void disableCarMode(int i) {
                if (isUiModeLocked()) {
                    Slog.e(UiModeManagerService.TAG, "disableCarMode while UI mode is locked");
                    return;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (UiModeManagerService.this.mLock) {
                        UiModeManagerService.this.setCarModeLocked(false, 0);
                        if (UiModeManagerService.this.mSystemReady) {
                            UiModeManagerService.this.updateLocked(0, i);
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public int getCurrentModeType() {
                int i;
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (UiModeManagerService.this.mLock) {
                        i = UiModeManagerService.this.mCurUiMode & 15;
                    }
                    return i;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public void setNightMode(int i) {
                if (isNightModeLocked() && UiModeManagerService.this.getContext().checkCallingOrSelfPermission("android.permission.MODIFY_DAY_NIGHT_MODE") != 0) {
                    Slog.e(UiModeManagerService.TAG, "Night mode locked, requires MODIFY_DAY_NIGHT_MODE permission");
                    return;
                }
                switch (i) {
                    case 0:
                    case 1:
                    case 2:
                        long jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            synchronized (UiModeManagerService.this.mLock) {
                                if (UiModeManagerService.this.mNightMode != i) {
                                    Settings.Secure.putInt(UiModeManagerService.this.getContext().getContentResolver(), "ui_night_mode", i);
                                    UiModeManagerService.this.mNightMode = i;
                                    UiModeManagerService.this.updateLocked(0, 0);
                                }
                                break;
                            }
                            return;
                        } finally {
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                        }
                    default:
                        throw new IllegalArgumentException("Unknown mode: " + i);
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

            public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
                new Shell(UiModeManagerService.this.mService).exec(UiModeManagerService.this.mService, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
            }

            protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
                if (DumpUtils.checkDumpPermission(UiModeManagerService.this.getContext(), UiModeManagerService.TAG, printWriter)) {
                    UiModeManagerService.this.dumpImpl(printWriter);
                }
            }
        };
    }

    private static Intent buildHomeIntent(String str) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory(str);
        intent.setFlags(270532608);
        return intent;
    }

    @Override
    public void onStart() {
        Context context = getContext();
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(26, TAG);
        context.registerReceiver(this.mDockModeReceiver, new IntentFilter("android.intent.action.DOCK_EVENT"));
        context.registerReceiver(this.mBatteryReceiver, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        this.mConfiguration.setToDefaults();
        Resources resources = context.getResources();
        this.mDefaultUiModeType = resources.getInteger(R.integer.config_bluetooth_idle_cur_ma);
        this.mCarModeKeepsScreenOn = resources.getInteger(R.integer.config_autoBrightnessBrighteningLightDebounce) == 1;
        this.mDeskModeKeepsScreenOn = resources.getInteger(R.integer.config_bluetooth_rx_cur_ma) == 1;
        this.mEnableCarDockLaunch = resources.getBoolean(R.^attr-private.foregroundInsidePadding);
        this.mUiModeLocked = resources.getBoolean(R.^attr-private.layout_removeBorders);
        this.mNightModeLocked = resources.getBoolean(R.^attr-private.layout_maxHeight);
        PackageManager packageManager = context.getPackageManager();
        this.mTelevision = packageManager.hasSystemFeature("android.hardware.type.television") || packageManager.hasSystemFeature("android.software.leanback");
        this.mWatch = packageManager.hasSystemFeature("android.hardware.type.watch");
        this.mNightMode = Settings.Secure.getInt(context.getContentResolver(), "ui_night_mode", resources.getInteger(R.integer.config_bg_current_drain_media_playback_min_duration));
        SystemServerInitThreadPool.get().submit(new Runnable() {
            @Override
            public final void run() {
                UiModeManagerService.lambda$onStart$0(this.f$0);
            }
        }, TAG + ".onStart");
        publishBinderService("uimode", this.mService);
    }

    public static void lambda$onStart$0(UiModeManagerService uiModeManagerService) {
        synchronized (uiModeManagerService.mLock) {
            uiModeManagerService.updateConfigurationLocked();
            uiModeManagerService.sendConfigurationLocked();
        }
    }

    void dumpImpl(PrintWriter printWriter) {
        synchronized (this.mLock) {
            printWriter.println("Current UI Mode Service state:");
            printWriter.print("  mDockState=");
            printWriter.print(this.mDockState);
            printWriter.print(" mLastBroadcastState=");
            printWriter.println(this.mLastBroadcastState);
            printWriter.print("  mNightMode=");
            printWriter.print(this.mNightMode);
            printWriter.print(" mNightModeLocked=");
            printWriter.print(this.mNightModeLocked);
            printWriter.print(" mCarModeEnabled=");
            printWriter.print(this.mCarModeEnabled);
            printWriter.print(" mComputedNightMode=");
            printWriter.print(this.mComputedNightMode);
            printWriter.print(" mCarModeEnableFlags=");
            printWriter.print(this.mCarModeEnableFlags);
            printWriter.print(" mEnableCarDockLaunch=");
            printWriter.println(this.mEnableCarDockLaunch);
            printWriter.print("  mCurUiMode=0x");
            printWriter.print(Integer.toHexString(this.mCurUiMode));
            printWriter.print(" mUiModeLocked=");
            printWriter.print(this.mUiModeLocked);
            printWriter.print(" mSetUiMode=0x");
            printWriter.println(Integer.toHexString(this.mSetUiMode));
            printWriter.print("  mHoldingConfiguration=");
            printWriter.print(this.mHoldingConfiguration);
            printWriter.print(" mSystemReady=");
            printWriter.println(this.mSystemReady);
            if (this.mTwilightManager != null) {
                printWriter.print("  mTwilightService.getLastTwilightState()=");
                printWriter.println(this.mTwilightManager.getLastTwilightState());
            }
        }
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 500) {
            synchronized (this.mLock) {
                this.mTwilightManager = (TwilightManager) getLocalService(TwilightManager.class);
                boolean z = true;
                this.mSystemReady = true;
                if (this.mDockState != 2) {
                    z = false;
                }
                this.mCarModeEnabled = z;
                updateComputedNightModeLocked();
                registerVrStateListener();
                updateLocked(0, 0);
            }
        }
    }

    void setCarModeLocked(boolean z, int i) {
        if (this.mCarModeEnabled != z) {
            this.mCarModeEnabled = z;
        }
        this.mCarModeEnableFlags = i;
    }

    private void updateDockState(int i) {
        synchronized (this.mLock) {
            if (i != this.mDockState) {
                this.mDockState = i;
                setCarModeLocked(this.mDockState == 2, 0);
                if (this.mSystemReady) {
                    updateLocked(1, 0);
                }
            }
        }
    }

    private static boolean isDeskDockState(int i) {
        if (i != 1) {
            switch (i) {
                case 3:
                case 4:
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    private void updateConfigurationLocked() {
        int i;
        int i2 = this.mDefaultUiModeType;
        if (!this.mUiModeLocked) {
            if (!this.mTelevision) {
                if (this.mWatch) {
                    i2 = 6;
                } else if (this.mCarModeEnabled) {
                    i2 = 3;
                } else if (isDeskDockState(this.mDockState)) {
                    i2 = 2;
                } else if (this.mVrHeadset) {
                    i2 = 7;
                }
            } else {
                i2 = 4;
            }
        }
        if (this.mNightMode == 0) {
            if (this.mTwilightManager != null) {
                this.mTwilightManager.registerListener(this.mTwilightListener, this.mHandler);
            }
            updateComputedNightModeLocked();
            i = i2 | (this.mComputedNightMode ? 32 : 16);
        } else {
            if (this.mTwilightManager != null) {
                this.mTwilightManager.unregisterListener(this.mTwilightListener);
            }
            i = i2 | (this.mNightMode << 4);
        }
        this.mCurUiMode = i;
        if (!this.mHoldingConfiguration) {
            this.mConfiguration.uiMode = i;
        }
    }

    private void sendConfigurationLocked() {
        if (this.mSetUiMode != this.mConfiguration.uiMode) {
            this.mSetUiMode = this.mConfiguration.uiMode;
            try {
                ActivityManager.getService().updateConfiguration(this.mConfiguration);
            } catch (RemoteException e) {
                Slog.w(TAG, "Failure communicating with activity manager", e);
            }
        }
    }

    void updateLocked(int i, int i2) {
        String str;
        String str2 = null;
        if (this.mLastBroadcastState == 2) {
            adjustStatusBarCarModeLocked();
            str = UiModeManager.ACTION_EXIT_CAR_MODE;
        } else if (isDeskDockState(this.mLastBroadcastState)) {
            str = UiModeManager.ACTION_EXIT_DESK_MODE;
        } else {
            str = null;
        }
        if (this.mCarModeEnabled) {
            if (this.mLastBroadcastState != 2) {
                adjustStatusBarCarModeLocked();
                if (str != null) {
                    sendForegroundBroadcastToAllUsers(str);
                }
                this.mLastBroadcastState = 2;
                str = UiModeManager.ACTION_ENTER_CAR_MODE;
            } else {
                str = null;
            }
        } else if (isDeskDockState(this.mDockState)) {
            if (!isDeskDockState(this.mLastBroadcastState)) {
                if (str != null) {
                    sendForegroundBroadcastToAllUsers(str);
                }
                this.mLastBroadcastState = this.mDockState;
                str = UiModeManager.ACTION_ENTER_DESK_MODE;
            }
        } else {
            this.mLastBroadcastState = 0;
        }
        boolean z = true;
        if (str != null) {
            Intent intent = new Intent(str);
            intent.putExtra("enableFlags", i);
            intent.putExtra("disableFlags", i2);
            intent.addFlags(268435456);
            getContext().sendOrderedBroadcastAsUser(intent, UserHandle.CURRENT, null, this.mResultReceiver, null, -1, null, null);
            this.mHoldingConfiguration = true;
            updateConfigurationLocked();
        } else {
            if (this.mCarModeEnabled) {
                if (this.mEnableCarDockLaunch && (i & 1) != 0) {
                    str2 = "android.intent.category.CAR_DOCK";
                }
            } else if (isDeskDockState(this.mDockState)) {
                if ((i & 1) != 0) {
                    str2 = "android.intent.category.DESK_DOCK";
                }
            } else if ((i2 & 1) != 0) {
                str2 = "android.intent.category.HOME";
            }
            sendConfigurationAndStartDreamOrDockAppLocked(str2);
        }
        if (!this.mCharging || ((!this.mCarModeEnabled || !this.mCarModeKeepsScreenOn || (this.mCarModeEnableFlags & 2) != 0) && (this.mCurUiMode != 2 || !this.mDeskModeKeepsScreenOn))) {
            z = false;
        }
        if (z != this.mWakeLock.isHeld()) {
            if (z) {
                this.mWakeLock.acquire();
            } else {
                this.mWakeLock.release();
            }
        }
    }

    private void sendForegroundBroadcastToAllUsers(String str) {
        getContext().sendBroadcastAsUser(new Intent(str).addFlags(268435456), UserHandle.ALL);
    }

    private void updateAfterBroadcastLocked(String str, int i, int i2) {
        String str2;
        if (UiModeManager.ACTION_ENTER_CAR_MODE.equals(str)) {
            if (this.mEnableCarDockLaunch && (i & 1) != 0) {
                str2 = "android.intent.category.CAR_DOCK";
            } else {
                str2 = null;
            }
        } else if (UiModeManager.ACTION_ENTER_DESK_MODE.equals(str)) {
            if ((i & 1) != 0) {
                str2 = "android.intent.category.DESK_DOCK";
            }
        } else if ((i2 & 1) != 0) {
            str2 = "android.intent.category.HOME";
        }
        sendConfigurationAndStartDreamOrDockAppLocked(str2);
    }

    private void sendConfigurationAndStartDreamOrDockAppLocked(String str) {
        boolean z = false;
        this.mHoldingConfiguration = false;
        updateConfigurationLocked();
        if (str != null) {
            Intent intentBuildHomeIntent = buildHomeIntent(str);
            if (Sandman.shouldStartDockApp(getContext(), intentBuildHomeIntent)) {
                try {
                    int iStartActivityWithConfig = ActivityManager.getService().startActivityWithConfig((IApplicationThread) null, (String) null, intentBuildHomeIntent, (String) null, (IBinder) null, (String) null, 0, 0, this.mConfiguration, (Bundle) null, -2);
                    if (ActivityManager.isStartResultSuccessful(iStartActivityWithConfig)) {
                        z = true;
                    } else if (iStartActivityWithConfig != -91) {
                        Slog.e(TAG, "Could not start dock app: " + intentBuildHomeIntent + ", startActivityWithConfig result " + iStartActivityWithConfig);
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "Could not start dock app: " + intentBuildHomeIntent, e);
                }
            }
        }
        sendConfigurationLocked();
        if (str != null && !z) {
            Sandman.startDreamWhenDockedIfAppropriate(getContext());
        }
    }

    private void adjustStatusBarCarModeLocked() {
        int i;
        Context context = getContext();
        if (this.mStatusBarManager == null) {
            this.mStatusBarManager = (StatusBarManager) context.getSystemService("statusbar");
        }
        if (this.mStatusBarManager != null) {
            StatusBarManager statusBarManager = this.mStatusBarManager;
            if (this.mCarModeEnabled) {
                i = DumpState.DUMP_FROZEN;
            } else {
                i = 0;
            }
            statusBarManager.disable(i);
        }
        if (this.mNotificationManager == null) {
            this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
        }
        if (this.mNotificationManager != null) {
            if (this.mCarModeEnabled) {
                this.mNotificationManager.notifyAsUser(null, 10, new Notification.Builder(context, SystemNotificationChannels.CAR_MODE).setSmallIcon(R.drawable.pointer_grab_vector).setDefaults(4).setOngoing(true).setWhen(0L).setColor(context.getColor(R.color.car_colorPrimary)).setContentTitle(context.getString(R.string.accessibility_label_private_profile)).setContentText(context.getString(R.string.accessibility_label_managed_profile)).setContentIntent(PendingIntent.getActivityAsUser(context, 0, new Intent(context, (Class<?>) DisableCarModeActivity.class), 0, null, UserHandle.CURRENT)).build(), UserHandle.ALL);
                return;
            }
            this.mNotificationManager.cancelAsUser(null, 10, UserHandle.ALL);
        }
    }

    private void updateComputedNightModeLocked() {
        TwilightState lastTwilightState;
        if (this.mTwilightManager != null && (lastTwilightState = this.mTwilightManager.getLastTwilightState()) != null) {
            this.mComputedNightMode = lastTwilightState.isNight();
        }
    }

    private void registerVrStateListener() {
        IVrManager iVrManagerAsInterface = IVrManager.Stub.asInterface(ServiceManager.getService("vrmanager"));
        if (iVrManagerAsInterface != null) {
            try {
                iVrManagerAsInterface.registerListener(this.mVrStateCallbacks);
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to register VR mode state listener: " + e);
            }
        }
    }

    private static class Shell extends ShellCommand {
        public static final String NIGHT_MODE_STR_AUTO = "auto";
        public static final String NIGHT_MODE_STR_NO = "no";
        public static final String NIGHT_MODE_STR_UNKNOWN = "unknown";
        public static final String NIGHT_MODE_STR_YES = "yes";
        private final IUiModeManager mInterface;

        Shell(IUiModeManager iUiModeManager) {
            this.mInterface = iUiModeManager;
        }

        public void onHelp() {
            PrintWriter outPrintWriter = getOutPrintWriter();
            outPrintWriter.println("UiModeManager service (uimode) commands:");
            outPrintWriter.println("  help");
            outPrintWriter.println("    Print this help text.");
            outPrintWriter.println("  night [yes|no|auto]");
            outPrintWriter.println("    Set or read night mode.");
        }

        public int onCommand(String str) {
            if (str == null) {
                return handleDefaultCommands(str);
            }
            try {
                if (((str.hashCode() == 104817688 && str.equals("night")) ? (byte) 0 : (byte) -1) == 0) {
                    return handleNightMode();
                }
                return handleDefaultCommands(str);
            } catch (RemoteException e) {
                getErrPrintWriter().println("Remote exception: " + e);
                return -1;
            }
        }

        private int handleNightMode() throws RemoteException {
            PrintWriter errPrintWriter = getErrPrintWriter();
            String nextArg = getNextArg();
            if (nextArg == null) {
                printCurrentNightMode();
                return 0;
            }
            int iStrToNightMode = strToNightMode(nextArg);
            if (iStrToNightMode >= 0) {
                this.mInterface.setNightMode(iStrToNightMode);
                printCurrentNightMode();
                return 0;
            }
            errPrintWriter.println("Error: mode must be 'yes', 'no', or 'auto'");
            return -1;
        }

        private void printCurrentNightMode() throws RemoteException {
            getOutPrintWriter().println("Night mode: " + nightModeToStr(this.mInterface.getNightMode()));
        }

        private static String nightModeToStr(int i) {
            switch (i) {
                case 0:
                    return NIGHT_MODE_STR_AUTO;
                case 1:
                    return NIGHT_MODE_STR_NO;
                case 2:
                    return NIGHT_MODE_STR_YES;
                default:
                    return NIGHT_MODE_STR_UNKNOWN;
            }
        }

        private static int strToNightMode(String str) {
            byte b;
            int iHashCode = str.hashCode();
            if (iHashCode != 3521) {
                if (iHashCode != 119527) {
                    b = (iHashCode == 3005871 && str.equals(NIGHT_MODE_STR_AUTO)) ? (byte) 2 : (byte) -1;
                } else if (str.equals(NIGHT_MODE_STR_YES)) {
                    b = 0;
                }
            } else if (str.equals(NIGHT_MODE_STR_NO)) {
                b = 1;
            }
            switch (b) {
                case 0:
                    return 2;
                case 1:
                    return 1;
                case 2:
                    return 0;
                default:
                    return -1;
            }
        }
    }
}
