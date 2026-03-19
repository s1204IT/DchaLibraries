package com.android.systemui.statusbar.phone;

import android.R;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SynchronousUserSwitchObserver;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.notification.StatusBarNotification;
import android.service.notification.ZenModeConfig;
import android.telecom.TelecomManager;
import android.text.format.DateFormat;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.IccCardConstants;
import com.android.systemui.Dependency;
import com.android.systemui.DockedStackExistsListener;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.qs.tiles.DndTile;
import com.android.systemui.qs.tiles.RotationLockTile;
import com.android.systemui.recents.misc.SysUiTaskStackChangeListener;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.phone.PhoneStatusBarPolicy;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.DataSaverController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.util.NotificationChannels;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class PhoneStatusBarPolicy implements CommandQueue.Callbacks, BluetoothController.Callback, DataSaverController.Listener, DeviceProvisionedController.DeviceProvisionedListener, KeyguardMonitor.Callback, LocationController.LocationChangeCallback, RotationLockController.RotationLockControllerCallback, ZenModeController.Callback {
    private static final boolean DEBUG = Log.isLoggable("PhoneStatusBarPolicy", 3);
    private final AlarmManager mAlarmManager;
    private final Context mContext;
    private boolean mCurrentUserSetup;
    private boolean mDockedStackExists;
    private final StatusBarIconController mIconController;
    private AlarmManager.AlarmClockInfo mNextAlarm;
    private final String mSlotAlarmClock;
    private final String mSlotBluetooth;
    private final String mSlotCast;
    private final String mSlotDataSaver;
    private final String mSlotHeadset;
    private final String mSlotHotspot;
    private final String mSlotLocation;
    private final String mSlotManagedProfile;
    private final String mSlotRotate;
    private final String mSlotTty;
    private final String mSlotVolume;
    private final String mSlotZen;
    private final UserManager mUserManager;
    private boolean mVolumeVisible;
    private boolean mZenVisible;
    private final Handler mHandler = new Handler();
    private final ArraySet<Pair<String, Integer>> mCurrentNotifs = new ArraySet<>();
    private final UiOffloadThread mUiOffloadThread = (UiOffloadThread) Dependency.get(UiOffloadThread.class);
    IccCardConstants.State mSimState = IccCardConstants.State.READY;
    private boolean mManagedProfileIconVisible = false;
    private final SynchronousUserSwitchObserver mUserSwitchListener = new AnonymousClass1();
    private final HotspotController.Callback mHotspotCallback = new HotspotController.Callback() {
        @Override
        public void onHotspotChanged(boolean z, int i) {
            PhoneStatusBarPolicy.this.mIconController.setIconVisibility(PhoneStatusBarPolicy.this.mSlotHotspot, z);
        }
    };
    private final CastController.Callback mCastCallback = new CastController.Callback() {
        @Override
        public void onCastDevicesChanged() {
            PhoneStatusBarPolicy.this.updateCast();
        }
    };
    private final NextAlarmController.NextAlarmChangeCallback mNextAlarmCallback = new NextAlarmController.NextAlarmChangeCallback() {
        @Override
        public void onNextAlarmChanged(AlarmManager.AlarmClockInfo alarmClockInfo) {
            PhoneStatusBarPolicy.this.mNextAlarm = alarmClockInfo;
            PhoneStatusBarPolicy.this.updateAlarm();
        }
    };
    private final SysUiTaskStackChangeListener mTaskListener = new SysUiTaskStackChangeListener() {
        @Override
        public void onTaskStackChanged() {
            PhoneStatusBarPolicy.this.updateForegroundInstantApps();
        }
    };

    @VisibleForTesting
    BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case "android.media.RINGER_MODE_CHANGED":
                case "android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION":
                    PhoneStatusBarPolicy.this.updateVolumeZen();
                    break;
                case "android.intent.action.SIM_STATE_CHANGED":
                    if (!intent.getBooleanExtra("rebroadcastOnUnlock", false)) {
                        PhoneStatusBarPolicy.this.updateSimState(intent);
                        break;
                    }
                    break;
                case "android.telecom.action.CURRENT_TTY_MODE_CHANGED":
                    PhoneStatusBarPolicy.this.updateTTY(intent.getIntExtra("android.telecom.intent.extra.CURRENT_TTY_MODE", 0));
                    break;
                case "android.intent.action.MANAGED_PROFILE_AVAILABLE":
                case "android.intent.action.MANAGED_PROFILE_UNAVAILABLE":
                case "android.intent.action.MANAGED_PROFILE_REMOVED":
                    PhoneStatusBarPolicy.this.updateManagedProfile();
                    break;
                case "android.intent.action.HEADSET_PLUG":
                    PhoneStatusBarPolicy.this.updateHeadsetPlug(intent);
                    break;
                case "android.intent.action.USER_SWITCHED":
                    PhoneStatusBarPolicy.this.updateAlarm();
                    PhoneStatusBarPolicy.this.registerAlarmClockChanged(intent.getIntExtra("android.intent.extra.user_handle", -1), true);
                    break;
                case "mediatek.intent.action.EMBMS_SESSION_STATUS_CHANGED":
                    PhoneStatusBarPolicy.this.updateEmbmsState(intent);
                    break;
            }
        }
    };
    private Runnable mRemoveCastIconRunnable = new Runnable() {
        @Override
        public void run() {
            if (PhoneStatusBarPolicy.DEBUG) {
                Log.v("PhoneStatusBarPolicy", "updateCast: hiding icon NOW");
            }
            PhoneStatusBarPolicy.this.mIconController.setIconVisibility(PhoneStatusBarPolicy.this.mSlotCast, false);
        }
    };
    private BroadcastReceiver mAlarmIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("PhoneStatusBarPolicy", "onReceive:" + action);
            if (action.equals("android.app.action.NEXT_ALARM_CLOCK_CHANGED")) {
                PhoneStatusBarPolicy.this.updateAlarm();
            }
        }
    };
    private final CastController mCast = (CastController) Dependency.get(CastController.class);
    private final HotspotController mHotspot = (HotspotController) Dependency.get(HotspotController.class);
    private BluetoothController mBluetooth = (BluetoothController) Dependency.get(BluetoothController.class);
    private final NextAlarmController mNextAlarmController = (NextAlarmController) Dependency.get(NextAlarmController.class);
    private final UserInfoController mUserInfoController = (UserInfoController) Dependency.get(UserInfoController.class);
    private final RotationLockController mRotationLockController = (RotationLockController) Dependency.get(RotationLockController.class);
    private final DataSaverController mDataSaver = (DataSaverController) Dependency.get(DataSaverController.class);
    private final ZenModeController mZenController = (ZenModeController) Dependency.get(ZenModeController.class);
    private final DeviceProvisionedController mProvisionedController = (DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class);
    private final KeyguardMonitor mKeyguardMonitor = (KeyguardMonitor) Dependency.get(KeyguardMonitor.class);
    private final LocationController mLocationController = (LocationController) Dependency.get(LocationController.class);
    private final String mSlotEmbms = "embms";

    @VisibleForTesting
    public PhoneStatusBarPolicy(Context context, StatusBarIconController statusBarIconController) {
        this.mContext = context;
        this.mIconController = statusBarIconController;
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mSlotCast = context.getString(R.string.mediasize_iso_b8);
        this.mSlotHotspot = context.getString(R.string.mediasize_iso_c4);
        this.mSlotBluetooth = context.getString(R.string.mediasize_iso_b7);
        this.mSlotTty = context.getString(R.string.mediasize_japanese_jis_b3);
        this.mSlotZen = context.getString(R.string.mediasize_japanese_jis_b7);
        this.mSlotVolume = context.getString(R.string.mediasize_japanese_jis_b4);
        this.mSlotAlarmClock = context.getString(R.string.mediasize_iso_b5);
        this.mSlotManagedProfile = context.getString(R.string.mediasize_iso_c7);
        this.mSlotRotate = context.getString(R.string.mediasize_japanese_hagaki);
        this.mSlotHeadset = context.getString(R.string.mediasize_iso_c3);
        this.mSlotDataSaver = context.getString(R.string.mediasize_iso_c10);
        this.mSlotLocation = context.getString(R.string.mediasize_iso_c6);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.media.RINGER_MODE_CHANGED");
        intentFilter.addAction("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION");
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        intentFilter.addAction("android.telecom.action.CURRENT_TTY_MODE_CHANGED");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_AVAILABLE");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
        intentFilter.addAction("mediatek.intent.action.EMBMS_SESSION_STATUS_CHANGED");
        this.mContext.registerReceiver(this.mIntentReceiver, intentFilter, null, this.mHandler);
        registerAlarmClockChanged(0, false);
        try {
            ActivityManager.getService().registerUserSwitchObserver(this.mUserSwitchListener, "PhoneStatusBarPolicy");
        } catch (RemoteException e) {
        }
        updateTTY();
        updateBluetooth();
        this.mIconController.setIcon(this.mSlotEmbms, com.android.systemui.R.drawable.stat_sys_embms, null);
        this.mIconController.setIconVisibility(this.mSlotEmbms, false);
        this.mIconController.setIcon(this.mSlotAlarmClock, com.android.systemui.R.drawable.stat_sys_alarm, null);
        this.mIconController.setIconVisibility(this.mSlotAlarmClock, false);
        this.mIconController.setIcon(this.mSlotZen, com.android.systemui.R.drawable.stat_sys_zen_important, null);
        this.mIconController.setIconVisibility(this.mSlotZen, false);
        this.mIconController.setIcon(this.mSlotVolume, com.android.systemui.R.drawable.stat_sys_ringer_vibrate, null);
        this.mIconController.setIconVisibility(this.mSlotVolume, false);
        updateVolumeZen();
        this.mIconController.setIcon(this.mSlotCast, com.android.systemui.R.drawable.stat_sys_cast, null);
        this.mIconController.setIconVisibility(this.mSlotCast, false);
        this.mIconController.setIcon(this.mSlotHotspot, com.android.systemui.R.drawable.stat_sys_hotspot, this.mContext.getString(com.android.systemui.R.string.accessibility_status_bar_hotspot));
        this.mIconController.setIconVisibility(this.mSlotHotspot, this.mHotspot.isHotspotEnabled());
        this.mIconController.setIcon(this.mSlotManagedProfile, com.android.systemui.R.drawable.stat_sys_managed_profile_status, this.mContext.getString(com.android.systemui.R.string.accessibility_managed_profile));
        this.mIconController.setIconVisibility(this.mSlotManagedProfile, this.mManagedProfileIconVisible);
        this.mIconController.setIcon(this.mSlotDataSaver, com.android.systemui.R.drawable.stat_sys_data_saver, context.getString(com.android.systemui.R.string.accessibility_data_saver_on));
        this.mIconController.setIconVisibility(this.mSlotDataSaver, false);
        this.mRotationLockController.addCallback(this);
        this.mBluetooth.addCallback(this);
        this.mProvisionedController.addCallback(this);
        this.mZenController.addCallback(this);
        this.mCast.addCallback(this.mCastCallback);
        this.mHotspot.addCallback(this.mHotspotCallback);
        this.mNextAlarmController.addCallback(this.mNextAlarmCallback);
        this.mDataSaver.addCallback(this);
        this.mKeyguardMonitor.addCallback(this);
        this.mLocationController.addCallback(this);
        ((CommandQueue) SysUiServiceProvider.getComponent(this.mContext, CommandQueue.class)).addCallbacks(this);
        ActivityManagerWrapper.getInstance().registerTaskStackListener(this.mTaskListener);
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        for (StatusBarNotification statusBarNotification : notificationManager.getActiveNotifications()) {
            if (statusBarNotification.getId() == 7) {
                notificationManager.cancel(statusBarNotification.getTag(), statusBarNotification.getId());
            }
        }
        DockedStackExistsListener.register(new Consumer() {
            @Override
            public final void accept(Object obj) {
                PhoneStatusBarPolicy.lambda$new$0(this.f$0, (Boolean) obj);
            }
        });
    }

    public static void lambda$new$0(PhoneStatusBarPolicy phoneStatusBarPolicy, Boolean bool) {
        phoneStatusBarPolicy.mDockedStackExists = bool.booleanValue();
        phoneStatusBarPolicy.updateForegroundInstantApps();
    }

    @Override
    public void onZenChanged(int i) {
        updateVolumeZen();
    }

    @Override
    public void onConfigChanged(ZenModeConfig zenModeConfig) {
        updateVolumeZen();
    }

    @Override
    public void onLocationActiveChanged(boolean z) {
        updateLocation();
    }

    private void updateLocation() {
        if (this.mLocationController.isLocationActive()) {
            this.mIconController.setIcon(this.mSlotLocation, com.android.systemui.R.drawable.stat_sys_location, this.mContext.getString(com.android.systemui.R.string.accessibility_location_active));
        } else {
            this.mIconController.removeAllIconsForSlot(this.mSlotLocation);
        }
    }

    private void updateAlarm() {
        AlarmManager.AlarmClockInfo nextAlarmClock = this.mAlarmManager.getNextAlarmClock(-2);
        boolean z = false;
        boolean z2 = nextAlarmClock != null && nextAlarmClock.getTriggerTime() > 0;
        this.mIconController.setIcon(this.mSlotAlarmClock, this.mZenController.getZen() == 2 ? com.android.systemui.R.drawable.stat_sys_alarm_dim : com.android.systemui.R.drawable.stat_sys_alarm, buildAlarmContentDescription());
        StatusBarIconController statusBarIconController = this.mIconController;
        String str = this.mSlotAlarmClock;
        if (this.mCurrentUserSetup && z2) {
            z = true;
        }
        statusBarIconController.setIconVisibility(str, z);
    }

    private String buildAlarmContentDescription() {
        if (this.mNextAlarm == null) {
            return this.mContext.getString(com.android.systemui.R.string.status_bar_alarm);
        }
        return formatNextAlarm(this.mNextAlarm, this.mContext);
    }

    private static String formatNextAlarm(AlarmManager.AlarmClockInfo alarmClockInfo, Context context) {
        if (alarmClockInfo == null) {
            return "";
        }
        return context.getString(com.android.systemui.R.string.accessibility_quick_settings_alarm, DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), DateFormat.is24HourFormat(context, ActivityManager.getCurrentUser()) ? "EHm" : "Ehma"), alarmClockInfo.getTriggerTime()).toString());
    }

    private final void updateSimState(Intent intent) {
        String stringExtra = intent.getStringExtra("ss");
        if ("ABSENT".equals(stringExtra)) {
            this.mSimState = IccCardConstants.State.ABSENT;
            return;
        }
        if ("CARD_IO_ERROR".equals(stringExtra)) {
            this.mSimState = IccCardConstants.State.CARD_IO_ERROR;
            return;
        }
        if ("CARD_RESTRICTED".equals(stringExtra)) {
            this.mSimState = IccCardConstants.State.CARD_RESTRICTED;
            return;
        }
        if ("READY".equals(stringExtra)) {
            this.mSimState = IccCardConstants.State.READY;
            return;
        }
        if ("LOCKED".equals(stringExtra)) {
            String stringExtra2 = intent.getStringExtra("reason");
            if ("PIN".equals(stringExtra2)) {
                this.mSimState = IccCardConstants.State.PIN_REQUIRED;
                return;
            } else if ("PUK".equals(stringExtra2)) {
                this.mSimState = IccCardConstants.State.PUK_REQUIRED;
                return;
            } else {
                this.mSimState = IccCardConstants.State.NETWORK_LOCKED;
                return;
            }
        }
        this.mSimState = IccCardConstants.State.UNKNOWN;
    }

    private final void updateVolumeZen() {
        boolean z;
        int i;
        String string;
        int i2;
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
        int zen = this.mZenController.getZen();
        String string2 = null;
        int i3 = 0;
        boolean z2 = true;
        if (DndTile.isVisible(this.mContext) || DndTile.isCombinedIcon(this.mContext)) {
            z = zen != 0;
            String string3 = this.mContext.getString(com.android.systemui.R.string.quick_settings_dnd_label);
            i = com.android.systemui.R.drawable.stat_sys_dnd;
            string = string3;
        } else {
            if (zen == 2) {
                i2 = com.android.systemui.R.drawable.stat_sys_zen_none;
                string = this.mContext.getString(com.android.systemui.R.string.interruption_level_none);
            } else if (zen == 1) {
                i2 = com.android.systemui.R.drawable.stat_sys_zen_important;
                string = this.mContext.getString(com.android.systemui.R.string.interruption_level_priority);
            } else {
                string = null;
                z = false;
                i = 0;
            }
            i = i2;
            z = true;
        }
        if (!ZenModeConfig.isZenOverridingRinger(zen, this.mZenController.getConfig())) {
            if (audioManager.getRingerModeInternal() == 1) {
                i3 = com.android.systemui.R.drawable.stat_sys_ringer_vibrate;
                string2 = this.mContext.getString(com.android.systemui.R.string.accessibility_ringer_vibrate);
            } else if (audioManager.getRingerModeInternal() == 0) {
                i3 = com.android.systemui.R.drawable.stat_sys_ringer_silent;
                string2 = this.mContext.getString(com.android.systemui.R.string.accessibility_ringer_silent);
            }
        } else {
            z2 = false;
        }
        if (z) {
            this.mIconController.setIcon(this.mSlotZen, i, string);
        }
        if (z != this.mZenVisible) {
            this.mIconController.setIconVisibility(this.mSlotZen, z);
            this.mZenVisible = z;
        }
        if (z2) {
            this.mIconController.setIcon(this.mSlotVolume, i3, string2);
        }
        if (z2 != this.mVolumeVisible) {
            this.mIconController.setIconVisibility(this.mSlotVolume, z2);
            this.mVolumeVisible = z2;
        }
        updateAlarm();
    }

    @Override
    public void onBluetoothDevicesChanged() {
        updateBluetooth();
    }

    @Override
    public void onBluetoothStateChange(boolean z) {
        updateBluetooth();
    }

    private final void updateBluetooth() {
        boolean zIsBluetoothEnabled;
        String string;
        int i;
        String string2 = this.mContext.getString(com.android.systemui.R.string.accessibility_quick_settings_bluetooth_on);
        if (this.mBluetooth != null && this.mBluetooth.isBluetoothConnected()) {
            i = com.android.systemui.R.drawable.stat_sys_data_bluetooth_connected;
            string = this.mContext.getString(com.android.systemui.R.string.accessibility_bluetooth_connected);
            zIsBluetoothEnabled = this.mBluetooth.isBluetoothEnabled();
        } else {
            zIsBluetoothEnabled = false;
            string = string2;
            i = com.android.systemui.R.drawable.stat_sys_data_bluetooth;
        }
        this.mIconController.setIcon(this.mSlotBluetooth, i, string);
        this.mIconController.setIconVisibility(this.mSlotBluetooth, zIsBluetoothEnabled);
    }

    private final void updateTTY() {
        TelecomManager telecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
        if (telecomManager == null) {
            updateTTY(0);
        } else {
            updateTTY(telecomManager.getCurrentTtyMode());
        }
    }

    private final void updateTTY(int i) {
        boolean z = i != 0;
        if (DEBUG) {
            Log.v("PhoneStatusBarPolicy", "updateTTY: enabled: " + z);
        }
        if (z) {
            if (DEBUG) {
                Log.v("PhoneStatusBarPolicy", "updateTTY: set TTY on");
            }
            this.mIconController.setIcon(this.mSlotTty, com.android.systemui.R.drawable.stat_sys_tty_mode, this.mContext.getString(com.android.systemui.R.string.accessibility_tty_enabled));
            this.mIconController.setIconVisibility(this.mSlotTty, true);
            return;
        }
        if (DEBUG) {
            Log.v("PhoneStatusBarPolicy", "updateTTY: set TTY off");
        }
        this.mIconController.setIconVisibility(this.mSlotTty, false);
    }

    private void updateCast() {
        boolean z;
        for (CastController.CastDevice castDevice : this.mCast.getCastDevices()) {
            if (castDevice.state == 1 || castDevice.state == 2) {
                z = true;
                break;
            }
        }
        z = false;
        if (DEBUG) {
            Log.v("PhoneStatusBarPolicy", "updateCast: isCasting: " + z);
        }
        this.mHandler.removeCallbacks(this.mRemoveCastIconRunnable);
        if (z) {
            this.mIconController.setIcon(this.mSlotCast, com.android.systemui.R.drawable.stat_sys_cast, this.mContext.getString(com.android.systemui.R.string.accessibility_casting));
            this.mIconController.setIconVisibility(this.mSlotCast, true);
        } else {
            if (DEBUG) {
                Log.v("PhoneStatusBarPolicy", "updateCast: hiding icon in 3 sec...");
            }
            this.mHandler.postDelayed(this.mRemoveCastIconRunnable, 3000L);
        }
    }

    protected void updateEmbmsState(Intent intent) {
        int intExtra = intent.getIntExtra("isActived", 0);
        if (DEBUG) {
            Log.d("PhoneStatusBarPolicy", "updateEmbmsState  active = " + intExtra);
        }
        if (intExtra == 1) {
            this.mIconController.setIcon(this.mSlotEmbms, com.android.systemui.R.drawable.stat_sys_embms, this.mContext.getString(com.android.systemui.R.string.accessibility_embms_enabled));
            this.mIconController.setIconVisibility(this.mSlotEmbms, true);
        } else {
            this.mIconController.setIconVisibility(this.mSlotEmbms, false);
        }
    }

    private void updateManagedProfile() {
        this.mUiOffloadThread.submit(new Runnable() {
            @Override
            public final void run() {
                PhoneStatusBarPolicy.lambda$updateManagedProfile$3(this.f$0);
            }
        });
    }

    public static void lambda$updateManagedProfile$3(final PhoneStatusBarPolicy phoneStatusBarPolicy) {
        try {
            final boolean zIsManagedProfile = phoneStatusBarPolicy.mUserManager.isManagedProfile(ActivityManager.getService().getLastResumedActivityUserId());
            phoneStatusBarPolicy.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    PhoneStatusBarPolicy.lambda$updateManagedProfile$2(this.f$0, zIsManagedProfile);
                }
            });
        } catch (RemoteException e) {
            Log.w("PhoneStatusBarPolicy", "updateManagedProfile: ", e);
        }
    }

    public static void lambda$updateManagedProfile$2(PhoneStatusBarPolicy phoneStatusBarPolicy, boolean z) {
        boolean z2;
        if (z && (!phoneStatusBarPolicy.mKeyguardMonitor.isShowing() || phoneStatusBarPolicy.mKeyguardMonitor.isOccluded())) {
            z2 = true;
            phoneStatusBarPolicy.mIconController.setIcon(phoneStatusBarPolicy.mSlotManagedProfile, com.android.systemui.R.drawable.stat_sys_managed_profile_status, phoneStatusBarPolicy.mContext.getString(com.android.systemui.R.string.accessibility_managed_profile));
        } else {
            z2 = false;
        }
        if (phoneStatusBarPolicy.mManagedProfileIconVisible != z2) {
            phoneStatusBarPolicy.mIconController.setIconVisibility(phoneStatusBarPolicy.mSlotManagedProfile, z2);
            phoneStatusBarPolicy.mManagedProfileIconVisible = z2;
        }
    }

    private void updateForegroundInstantApps() {
        final NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        final ArraySet arraySet = new ArraySet((ArraySet) this.mCurrentNotifs);
        final IPackageManager packageManager = AppGlobals.getPackageManager();
        this.mCurrentNotifs.clear();
        this.mUiOffloadThread.submit(new Runnable() {
            @Override
            public final void run() {
                PhoneStatusBarPolicy.lambda$updateForegroundInstantApps$5(this.f$0, arraySet, notificationManager, packageManager);
            }
        });
    }

    public static void lambda$updateForegroundInstantApps$5(PhoneStatusBarPolicy phoneStatusBarPolicy, ArraySet arraySet, final NotificationManager notificationManager, IPackageManager iPackageManager) {
        int windowingMode;
        try {
            ActivityManager.StackInfo focusedStackInfo = ActivityManager.getService().getFocusedStackInfo();
            if (focusedStackInfo != null && ((windowingMode = focusedStackInfo.configuration.windowConfiguration.getWindowingMode()) == 1 || windowingMode == 4)) {
                phoneStatusBarPolicy.checkStack(focusedStackInfo, arraySet, notificationManager, iPackageManager);
            }
            if (phoneStatusBarPolicy.mDockedStackExists) {
                phoneStatusBarPolicy.checkStack(3, 0, arraySet, notificationManager, iPackageManager);
            }
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
        arraySet.forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                Pair pair = (Pair) obj;
                notificationManager.cancelAsUser((String) pair.first, 7, new UserHandle(((Integer) pair.second).intValue()));
            }
        });
    }

    private void checkStack(int i, int i2, ArraySet<Pair<String, Integer>> arraySet, NotificationManager notificationManager, IPackageManager iPackageManager) {
        try {
            checkStack(ActivityManager.getService().getStackInfo(i, i2), arraySet, notificationManager, iPackageManager);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    private void checkStack(ActivityManager.StackInfo stackInfo, ArraySet<Pair<String, Integer>> arraySet, NotificationManager notificationManager, IPackageManager iPackageManager) {
        if (stackInfo == null) {
            return;
        }
        try {
            if (stackInfo.topActivity == null) {
                return;
            }
            String packageName = stackInfo.topActivity.getPackageName();
            if (!hasNotif(arraySet, packageName, stackInfo.userId)) {
                ApplicationInfo applicationInfo = iPackageManager.getApplicationInfo(packageName, 8192, stackInfo.userId);
                if (applicationInfo.isInstantApp()) {
                    postEphemeralNotif(packageName, stackInfo.userId, applicationInfo, notificationManager, stackInfo.taskIds[stackInfo.taskIds.length - 1]);
                }
            }
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    private void postEphemeralNotif(String str, int i, ApplicationInfo applicationInfo, NotificationManager notificationManager, int i2) {
        ComponentName instantAppInstallerComponent;
        Bundle bundle = new Bundle();
        bundle.putString("android.substName", this.mContext.getString(com.android.systemui.R.string.instant_apps));
        this.mCurrentNotifs.add(new Pair<>(str, Integer.valueOf(i)));
        String string = this.mContext.getString(com.android.systemui.R.string.instant_apps_message);
        PendingIntent activity = BenesseExtension.getDchaState() != 0 ? null : PendingIntent.getActivity(this.mContext, 0, new Intent("android.settings.APPLICATION_DETAILS_SETTINGS").setData(Uri.fromParts("package", str, null)), 67108864);
        Notification.Action actionBuild = new Notification.Action.Builder((Icon) null, this.mContext.getString(com.android.systemui.R.string.app_info), activity).build();
        Intent taskIntent = getTaskIntent(i2, i);
        Notification.Builder builder = new Notification.Builder(this.mContext, NotificationChannels.GENERAL);
        if (taskIntent != null && taskIntent.isWebIntent()) {
            taskIntent.setComponent(null).setPackage(null).addFlags(512).addFlags(268435456);
            PendingIntent activity2 = PendingIntent.getActivity(this.mContext, 0, taskIntent, 67108864);
            try {
                instantAppInstallerComponent = AppGlobals.getPackageManager().getInstantAppInstallerComponent();
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
                instantAppInstallerComponent = null;
            }
            builder.addAction(new Notification.Action.Builder((Icon) null, this.mContext.getString(com.android.systemui.R.string.go_to_web), PendingIntent.getActivity(this.mContext, 0, new Intent().setComponent(instantAppInstallerComponent).setAction("android.intent.action.VIEW").addCategory("android.intent.category.BROWSABLE").addCategory("unique:" + System.currentTimeMillis()).putExtra("android.intent.extra.PACKAGE_NAME", applicationInfo.packageName).putExtra("android.intent.extra.VERSION_CODE", applicationInfo.versionCode & Integer.MAX_VALUE).putExtra("android.intent.extra.LONG_VERSION_CODE", applicationInfo.versionCode).putExtra("android.intent.extra.EPHEMERAL_FAILURE", activity2).putExtra("android.intent.extra.INSTANT_APP_FAILURE", activity2), 67108864)).build());
        }
        notificationManager.notifyAsUser(str, 7, builder.addExtras(bundle).addAction(actionBuild).setContentIntent(activity).setColor(this.mContext.getColor(com.android.systemui.R.color.instant_apps_color)).setContentTitle(applicationInfo.loadLabel(this.mContext.getPackageManager())).setLargeIcon(Icon.createWithResource(str, applicationInfo.icon)).setSmallIcon(Icon.createWithResource(this.mContext.getPackageName(), com.android.systemui.R.drawable.instant_icon)).setContentText(string).setOngoing(true).build(), new UserHandle(i));
    }

    private Intent getTaskIntent(int i, int i2) {
        try {
            List list = ActivityManager.getService().getRecentTasks(5, 0, i2).getList();
            for (int i3 = 0; i3 < list.size(); i3++) {
                if (((ActivityManager.RecentTaskInfo) list.get(i3)).id == i) {
                    return ((ActivityManager.RecentTaskInfo) list.get(i3)).baseIntent;
                }
            }
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    private boolean hasNotif(ArraySet<Pair<String, Integer>> arraySet, String str, int i) {
        Pair<String, Integer> pair = new Pair<>(str, Integer.valueOf(i));
        if (arraySet.remove(pair)) {
            this.mCurrentNotifs.add(pair);
            return true;
        }
        return false;
    }

    class AnonymousClass1 extends SynchronousUserSwitchObserver {
        AnonymousClass1() {
        }

        public void onUserSwitching(int i) throws RemoteException {
            PhoneStatusBarPolicy.this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    PhoneStatusBarPolicy.this.mUserInfoController.reloadUserInfo();
                }
            });
        }

        public void onUserSwitchComplete(int i) throws RemoteException {
            PhoneStatusBarPolicy.this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    PhoneStatusBarPolicy.AnonymousClass1.lambda$onUserSwitchComplete$1(this.f$0);
                }
            });
        }

        public static void lambda$onUserSwitchComplete$1(AnonymousClass1 anonymousClass1) {
            PhoneStatusBarPolicy.this.updateAlarm();
            PhoneStatusBarPolicy.this.updateManagedProfile();
            PhoneStatusBarPolicy.this.updateForegroundInstantApps();
        }
    }

    @Override
    public void appTransitionStarting(long j, long j2, boolean z) {
        updateManagedProfile();
        updateForegroundInstantApps();
    }

    @Override
    public void onKeyguardShowingChanged() {
        updateManagedProfile();
        updateForegroundInstantApps();
    }

    @Override
    public void onUserSetupChanged() {
        boolean zIsUserSetup = this.mProvisionedController.isUserSetup(this.mProvisionedController.getCurrentUser());
        if (this.mCurrentUserSetup == zIsUserSetup) {
            return;
        }
        this.mCurrentUserSetup = zIsUserSetup;
        updateAlarm();
    }

    @Override
    public void preloadRecentApps() {
        updateForegroundInstantApps();
    }

    @Override
    public void onRotationLockStateChanged(boolean z, boolean z2) {
        boolean zIsCurrentOrientationLockPortrait = RotationLockTile.isCurrentOrientationLockPortrait(this.mRotationLockController, this.mContext);
        if (z) {
            if (zIsCurrentOrientationLockPortrait) {
                this.mIconController.setIcon(this.mSlotRotate, com.android.systemui.R.drawable.stat_sys_rotate_portrait, this.mContext.getString(com.android.systemui.R.string.accessibility_rotation_lock_on_portrait));
            } else {
                this.mIconController.setIcon(this.mSlotRotate, com.android.systemui.R.drawable.stat_sys_rotate_landscape, this.mContext.getString(com.android.systemui.R.string.accessibility_rotation_lock_on_landscape));
            }
            this.mIconController.setIconVisibility(this.mSlotRotate, true);
            return;
        }
        this.mIconController.setIconVisibility(this.mSlotRotate, false);
    }

    private void updateHeadsetPlug(Intent intent) {
        int i;
        boolean z = intent.getIntExtra("state", 0) != 0;
        boolean z2 = intent.getIntExtra("microphone", 0) != 0;
        Log.d("PhoneStatusBarPolicy", "updateHeadsetPlug connected:" + z + ",hasMic:" + z2);
        if (z) {
            Context context = this.mContext;
            if (z2) {
                i = com.android.systemui.R.string.accessibility_status_bar_headset;
            } else {
                i = com.android.systemui.R.string.accessibility_status_bar_headphones;
            }
            this.mIconController.setIcon(this.mSlotHeadset, z2 ? com.android.systemui.R.drawable.ic_headset_mic : com.android.systemui.R.drawable.ic_headset, context.getString(i));
            this.mIconController.setIconVisibility(this.mSlotHeadset, true);
            return;
        }
        this.mIconController.setIconVisibility(this.mSlotHeadset, false);
    }

    @Override
    public void onDataSaverChanged(boolean z) {
        this.mIconController.setIconVisibility(this.mSlotDataSaver, z);
    }

    private void registerAlarmClockChanged(int i, boolean z) {
        if (z) {
            this.mContext.unregisterReceiver(this.mAlarmIntentReceiver);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
        Log.d("PhoneStatusBarPolicy", "registerAlarmClockChanged:" + i);
        this.mContext.registerReceiverAsUser(this.mAlarmIntentReceiver, new UserHandle(i), intentFilter, null, this.mHandler);
    }
}
