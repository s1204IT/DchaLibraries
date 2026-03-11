package com.android.systemui.statusbar.phone;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlarmManager;
import android.app.SynchronousUserSwitchObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.hardware.display.WifiDisplayStatus;
import android.media.AudioManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.android.internal.telephony.IccCardConstants;
import com.android.systemui.qs.tiles.DndTile;
import com.android.systemui.qs.tiles.RotationLockTile;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.DataSaverController;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.UserInfoController;

public class PhoneStatusBarPolicy implements BluetoothController.Callback, RotationLockController.RotationLockControllerCallback, DataSaverController.Listener {
    private static final boolean DEBUG = Log.isLoggable("PhoneStatusBarPolicy", 3);
    private final AlarmManager mAlarmManager;
    private BluetoothController mBluetooth;
    private final CastController mCast;
    private final Context mContext;
    private boolean mCurrentUserSetup;
    private final DataSaverController mDataSaver;
    private final HotspotController mHotspot;
    private final StatusBarIconController mIconController;
    private final RotationLockController mRotationLockController;
    private final String mSlotAlarmClock;
    private final String mSlotBluetooth;
    private final String mSlotCast;
    private final String mSlotDataSaver;
    private final String mSlotHeadset;
    private final String mSlotHotspot;
    private final String mSlotManagedProfile;
    private final String mSlotRotate;
    private final String mSlotTty;
    private final String mSlotVolume;
    private final String mSlotZen;
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private final UserInfoController mUserInfoController;
    private final UserManager mUserManager;
    private boolean mVolumeVisible;
    private int mZen;
    private boolean mZenVisible;
    private final Handler mHandler = new Handler();
    IccCardConstants.State mSimState = IccCardConstants.State.READY;
    private boolean mManagedProfileFocused = false;
    private boolean mManagedProfileIconVisible = false;
    private boolean mManagedProfileInQuietMode = false;
    private final SynchronousUserSwitchObserver mUserSwitchListener = new SynchronousUserSwitchObserver() {
        public void onUserSwitching(int newUserId) throws RemoteException {
            PhoneStatusBarPolicy.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    PhoneStatusBarPolicy.this.mUserInfoController.reloadUserInfo();
                }
            });
        }

        public void onUserSwitchComplete(final int newUserId) throws RemoteException {
            PhoneStatusBarPolicy.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    PhoneStatusBarPolicy.this.updateAlarm();
                    PhoneStatusBarPolicy.this.profileChanged(newUserId);
                    PhoneStatusBarPolicy.this.updateQuietState();
                    PhoneStatusBarPolicy.this.updateManagedProfile();
                }
            });
        }

        public void onForegroundProfileSwitch(final int newProfileId) {
            PhoneStatusBarPolicy.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    PhoneStatusBarPolicy.this.profileChanged(newProfileId);
                }
            });
        }
    };
    private final HotspotController.Callback mHotspotCallback = new HotspotController.Callback() {
        @Override
        public void onHotspotChanged(boolean enabled) {
            PhoneStatusBarPolicy.this.mIconController.setIconVisibility(PhoneStatusBarPolicy.this.mSlotHotspot, enabled);
        }
    };
    private final CastController.Callback mCastCallback = new CastController.Callback() {
        @Override
        public void onCastDevicesChanged() {
            PhoneStatusBarPolicy.this.updateCast();
        }

        @Override
        public void onWfdStatusChanged(WifiDisplayStatus status, boolean sinkMode) {
        }

        @Override
        public void onWifiP2pDeviceChanged(WifiP2pDevice device) {
        }
    };
    BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.app.action.NEXT_ALARM_CLOCK_CHANGED")) {
                PhoneStatusBarPolicy.this.updateAlarm();
                return;
            }
            if (action.equals("android.media.RINGER_MODE_CHANGED") || action.equals("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION")) {
                PhoneStatusBarPolicy.this.updateVolumeZen();
                return;
            }
            if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                PhoneStatusBarPolicy.this.updateSimState(intent);
                return;
            }
            if (action.equals("android.telecom.action.CURRENT_TTY_MODE_CHANGED")) {
                PhoneStatusBarPolicy.this.updateTTY(intent);
                return;
            }
            if (action.equals("android.intent.action.MANAGED_PROFILE_AVAILABLE") || action.equals("android.intent.action.MANAGED_PROFILE_UNAVAILABLE") || action.equals("android.intent.action.MANAGED_PROFILE_REMOVED")) {
                PhoneStatusBarPolicy.this.updateQuietState();
                PhoneStatusBarPolicy.this.updateManagedProfile();
            } else if (action.equals("android.intent.action.HEADSET_PLUG")) {
                PhoneStatusBarPolicy.this.updateHeadsetPlug(intent);
            } else {
                if (!action.equals("android.intent.action.USER_SWITCHED")) {
                    return;
                }
                PhoneStatusBarPolicy.this.updateAlarm();
                int newUserId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                PhoneStatusBarPolicy.this.registerAlarmClockChanged(newUserId, true);
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
            if (!action.equals("android.app.action.NEXT_ALARM_CLOCK_CHANGED")) {
                return;
            }
            PhoneStatusBarPolicy.this.updateAlarm();
        }
    };

    public PhoneStatusBarPolicy(Context context, StatusBarIconController iconController, CastController cast, HotspotController hotspot, UserInfoController userInfoController, BluetoothController bluetooth, RotationLockController rotationLockController, DataSaverController dataSaver) {
        this.mContext = context;
        this.mIconController = iconController;
        this.mCast = cast;
        this.mHotspot = hotspot;
        this.mBluetooth = bluetooth;
        this.mBluetooth.addStateChangedCallback(this);
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mUserInfoController = userInfoController;
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mRotationLockController = rotationLockController;
        this.mDataSaver = dataSaver;
        this.mSlotCast = context.getString(R.string.config_feedbackIntentNameKey);
        this.mSlotHotspot = context.getString(R.string.config_defaultAssistant);
        this.mSlotBluetooth = context.getString(R.string.config_defaultDialer);
        this.mSlotTty = context.getString(R.string.config_defaultCallRedirection);
        this.mSlotZen = context.getString(R.string.config_systemGallery);
        this.mSlotVolume = context.getString(R.string.config_systemAutomotiveProjection);
        this.mSlotAlarmClock = context.getString(R.string.config_systemWellbeing);
        this.mSlotManagedProfile = context.getString(R.string.config_helpPackageNameValue);
        this.mSlotRotate = context.getString(R.string.paste_as_plain_text);
        this.mSlotHeadset = context.getString(R.string.autofill);
        this.mSlotDataSaver = context.getString(R.string.config_helpPackageNameKey);
        this.mRotationLockController.addRotationLockControllerCallback(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.media.RINGER_MODE_CHANGED");
        filter.addAction("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION");
        filter.addAction("android.intent.action.HEADSET_PLUG");
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("android.telecom.action.CURRENT_TTY_MODE_CHANGED");
        filter.addAction("android.intent.action.MANAGED_PROFILE_AVAILABLE");
        filter.addAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
        filter.addAction("android.intent.action.USER_SWITCHED");
        filter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
        this.mContext.registerReceiver(this.mIntentReceiver, filter, null, this.mHandler);
        registerAlarmClockChanged(0, false);
        try {
            ActivityManagerNative.getDefault().registerUserSwitchObserver(this.mUserSwitchListener);
        } catch (RemoteException e) {
        }
        this.mIconController.setIcon(this.mSlotTty, com.android.systemui.R.drawable.stat_sys_tty_mode, null);
        this.mIconController.setIconVisibility(this.mSlotTty, false);
        updateBluetooth();
        this.mIconController.setIcon(this.mSlotAlarmClock, com.android.systemui.R.drawable.stat_sys_alarm, null);
        this.mIconController.setIconVisibility(this.mSlotAlarmClock, false);
        this.mIconController.setIcon(this.mSlotZen, com.android.systemui.R.drawable.stat_sys_zen_important, null);
        this.mIconController.setIconVisibility(this.mSlotZen, false);
        this.mIconController.setIcon(this.mSlotVolume, com.android.systemui.R.drawable.stat_sys_ringer_vibrate, null);
        this.mIconController.setIconVisibility(this.mSlotVolume, false);
        updateVolumeZen();
        if (this.mCast != null) {
            this.mIconController.setIcon(this.mSlotCast, com.android.systemui.R.drawable.stat_sys_cast, null);
            this.mIconController.setIconVisibility(this.mSlotCast, false);
            this.mCast.addCallback(this.mCastCallback);
        }
        this.mIconController.setIcon(this.mSlotHotspot, com.android.systemui.R.drawable.stat_sys_hotspot, this.mContext.getString(com.android.systemui.R.string.accessibility_status_bar_hotspot));
        this.mIconController.setIconVisibility(this.mSlotHotspot, this.mHotspot.isHotspotEnabled());
        this.mHotspot.addCallback(this.mHotspotCallback);
        this.mIconController.setIcon(this.mSlotManagedProfile, com.android.systemui.R.drawable.stat_sys_managed_profile_status, this.mContext.getString(com.android.systemui.R.string.accessibility_managed_profile));
        this.mIconController.setIconVisibility(this.mSlotManagedProfile, this.mManagedProfileIconVisible);
        this.mIconController.setIcon(this.mSlotDataSaver, com.android.systemui.R.drawable.stat_sys_data_saver, context.getString(com.android.systemui.R.string.accessibility_data_saver_on));
        this.mIconController.setIconVisibility(this.mSlotDataSaver, false);
        this.mDataSaver.addListener(this);
    }

    public void setStatusBarKeyguardViewManager(StatusBarKeyguardViewManager statusBarKeyguardViewManager) {
        this.mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
    }

    public void setZenMode(int zen) {
        this.mZen = zen;
        updateVolumeZen();
    }

    public void updateAlarm() {
        AlarmManager.AlarmClockInfo alarm = this.mAlarmManager.getNextAlarmClock(-2);
        boolean hasAlarm = alarm != null && alarm.getTriggerTime() > 0;
        boolean zenNone = this.mZen == 2;
        this.mIconController.setIcon(this.mSlotAlarmClock, zenNone ? com.android.systemui.R.drawable.stat_sys_alarm_dim : com.android.systemui.R.drawable.stat_sys_alarm, null);
        StatusBarIconController statusBarIconController = this.mIconController;
        String str = this.mSlotAlarmClock;
        if (!this.mCurrentUserSetup) {
            hasAlarm = false;
        }
        statusBarIconController.setIconVisibility(str, hasAlarm);
    }

    public final void updateSimState(Intent intent) {
        String stateExtra = intent.getStringExtra("ss");
        if ("ABSENT".equals(stateExtra)) {
            this.mSimState = IccCardConstants.State.ABSENT;
            return;
        }
        if ("CARD_IO_ERROR".equals(stateExtra)) {
            this.mSimState = IccCardConstants.State.CARD_IO_ERROR;
            return;
        }
        if ("READY".equals(stateExtra)) {
            this.mSimState = IccCardConstants.State.READY;
            return;
        }
        if ("LOCKED".equals(stateExtra)) {
            String lockedReason = intent.getStringExtra("reason");
            if ("PIN".equals(lockedReason)) {
                this.mSimState = IccCardConstants.State.PIN_REQUIRED;
                return;
            } else if ("PUK".equals(lockedReason)) {
                this.mSimState = IccCardConstants.State.PUK_REQUIRED;
                return;
            } else {
                this.mSimState = IccCardConstants.State.NETWORK_LOCKED;
                return;
            }
        }
        this.mSimState = IccCardConstants.State.UNKNOWN;
    }

    public final void updateVolumeZen() {
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
        boolean zenVisible = false;
        int zenIconId = 0;
        String zenDescription = null;
        boolean volumeVisible = false;
        int volumeIconId = 0;
        String volumeDescription = null;
        if (DndTile.isVisible(this.mContext) || DndTile.isCombinedIcon(this.mContext)) {
            zenVisible = this.mZen != 0;
            zenIconId = this.mZen == 2 ? com.android.systemui.R.drawable.stat_sys_dnd_total_silence : com.android.systemui.R.drawable.stat_sys_dnd;
            zenDescription = this.mContext.getString(com.android.systemui.R.string.quick_settings_dnd_label);
        } else if (this.mZen == 2) {
            zenVisible = true;
            zenIconId = com.android.systemui.R.drawable.stat_sys_zen_none;
            zenDescription = this.mContext.getString(com.android.systemui.R.string.interruption_level_none);
        } else if (this.mZen == 1) {
            zenVisible = true;
            zenIconId = com.android.systemui.R.drawable.stat_sys_zen_important;
            zenDescription = this.mContext.getString(com.android.systemui.R.string.interruption_level_priority);
        }
        if (DndTile.isVisible(this.mContext) && !DndTile.isCombinedIcon(this.mContext) && audioManager.getRingerModeInternal() == 0) {
            volumeVisible = true;
            volumeIconId = com.android.systemui.R.drawable.stat_sys_ringer_silent;
            volumeDescription = this.mContext.getString(com.android.systemui.R.string.accessibility_ringer_silent);
        } else if (this.mZen != 2 && this.mZen != 3 && audioManager.getRingerModeInternal() == 1) {
            volumeVisible = true;
            volumeIconId = com.android.systemui.R.drawable.stat_sys_ringer_vibrate;
            volumeDescription = this.mContext.getString(com.android.systemui.R.string.accessibility_ringer_vibrate);
        }
        if (zenVisible) {
            this.mIconController.setIcon(this.mSlotZen, zenIconId, zenDescription);
        }
        if (zenVisible != this.mZenVisible) {
            this.mIconController.setIconVisibility(this.mSlotZen, zenVisible);
            this.mZenVisible = zenVisible;
        }
        if (volumeVisible) {
            this.mIconController.setIcon(this.mSlotVolume, volumeIconId, volumeDescription);
        }
        if (volumeVisible != this.mVolumeVisible) {
            this.mIconController.setIconVisibility(this.mSlotVolume, volumeVisible);
            this.mVolumeVisible = volumeVisible;
        }
        updateAlarm();
    }

    @Override
    public void onBluetoothDevicesChanged() {
        updateBluetooth();
    }

    @Override
    public void onBluetoothStateChange(boolean enabled) {
        updateBluetooth();
    }

    private final void updateBluetooth() {
        int iconId = com.android.systemui.R.drawable.stat_sys_data_bluetooth;
        String contentDescription = this.mContext.getString(com.android.systemui.R.string.accessibility_quick_settings_bluetooth_on);
        boolean bluetoothEnabled = false;
        if (this.mBluetooth != null) {
            bluetoothEnabled = this.mBluetooth.isBluetoothEnabled();
            if (this.mBluetooth.isBluetoothConnected()) {
                iconId = com.android.systemui.R.drawable.stat_sys_data_bluetooth_connected;
                contentDescription = this.mContext.getString(com.android.systemui.R.string.accessibility_bluetooth_connected);
            }
        }
        this.mIconController.setIcon(this.mSlotBluetooth, iconId, contentDescription);
        this.mIconController.setIconVisibility(this.mSlotBluetooth, bluetoothEnabled);
    }

    protected void updateTTY(Intent intent) {
        int currentTtyMode = intent.getIntExtra("android.telecom.intent.extra.CURRENT_TTY_MODE", 0);
        boolean enabled = currentTtyMode != 0;
        if (DEBUG) {
            Log.v("PhoneStatusBarPolicy", "updateTTY: enabled: " + enabled);
        }
        if (enabled) {
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

    public void updateCast() {
        boolean isCasting = false;
        for (CastController.CastDevice device : this.mCast.getCastDevices()) {
            if (device.state == 1 || device.state == 2) {
                isCasting = true;
                break;
            }
        }
        if (DEBUG) {
            Log.v("PhoneStatusBarPolicy", "updateCast: isCasting: " + isCasting);
        }
        this.mHandler.removeCallbacks(this.mRemoveCastIconRunnable);
        if (isCasting) {
            this.mIconController.setIcon(this.mSlotCast, com.android.systemui.R.drawable.stat_sys_cast, this.mContext.getString(com.android.systemui.R.string.accessibility_casting));
            this.mIconController.setIconVisibility(this.mSlotCast, true);
        } else {
            if (DEBUG) {
                Log.v("PhoneStatusBarPolicy", "updateCast: hiding icon in 3 sec...");
            }
            this.mHandler.postDelayed(this.mRemoveCastIconRunnable, 3000L);
        }
    }

    public void updateQuietState() {
        this.mManagedProfileInQuietMode = false;
        int currentUserId = ActivityManager.getCurrentUser();
        for (UserInfo ui : this.mUserManager.getEnabledProfiles(currentUserId)) {
            if (ui.isManagedProfile() && ui.isQuietModeEnabled()) {
                this.mManagedProfileInQuietMode = true;
                return;
            }
        }
    }

    public void profileChanged(int userId) {
        UserInfo user = null;
        if (userId == -2) {
            try {
                user = ActivityManagerNative.getDefault().getCurrentUser();
            } catch (RemoteException e) {
            }
        } else {
            user = this.mUserManager.getUserInfo(userId);
        }
        this.mManagedProfileFocused = user != null ? user.isManagedProfile() : false;
        if (DEBUG) {
            Log.v("PhoneStatusBarPolicy", "profileChanged: mManagedProfileFocused: " + this.mManagedProfileFocused);
        }
    }

    public void updateManagedProfile() {
        boolean showIcon;
        if (DEBUG) {
            Log.v("PhoneStatusBarPolicy", "updateManagedProfile: mManagedProfileFocused: " + this.mManagedProfileFocused);
        }
        if (this.mManagedProfileFocused && !this.mStatusBarKeyguardViewManager.isShowing()) {
            showIcon = true;
            this.mIconController.setIcon(this.mSlotManagedProfile, com.android.systemui.R.drawable.stat_sys_managed_profile_status, this.mContext.getString(com.android.systemui.R.string.accessibility_managed_profile));
        } else if (this.mManagedProfileInQuietMode) {
            showIcon = true;
            this.mIconController.setIcon(this.mSlotManagedProfile, com.android.systemui.R.drawable.stat_sys_managed_profile_status_off, this.mContext.getString(com.android.systemui.R.string.accessibility_managed_profile));
        } else {
            showIcon = false;
        }
        if (this.mManagedProfileIconVisible == showIcon) {
            return;
        }
        this.mIconController.setIconVisibility(this.mSlotManagedProfile, showIcon);
        this.mManagedProfileIconVisible = showIcon;
    }

    public void appTransitionStarting(long startTime, long duration) {
        updateManagedProfile();
    }

    public void notifyKeyguardShowingChanged() {
        updateManagedProfile();
    }

    public void setCurrentUserSetup(boolean userSetup) {
        if (this.mCurrentUserSetup == userSetup) {
            return;
        }
        this.mCurrentUserSetup = userSetup;
        updateAlarm();
        updateQuietState();
    }

    @Override
    public void onRotationLockStateChanged(boolean rotationLocked, boolean affordanceVisible) {
        boolean portrait = RotationLockTile.isCurrentOrientationLockPortrait(this.mRotationLockController, this.mContext);
        if (rotationLocked) {
            if (portrait) {
                this.mIconController.setIcon(this.mSlotRotate, com.android.systemui.R.drawable.stat_sys_rotate_portrait, this.mContext.getString(com.android.systemui.R.string.accessibility_rotation_lock_on_portrait));
            } else {
                this.mIconController.setIcon(this.mSlotRotate, com.android.systemui.R.drawable.stat_sys_rotate_landscape, this.mContext.getString(com.android.systemui.R.string.accessibility_rotation_lock_on_landscape));
            }
            this.mIconController.setIconVisibility(this.mSlotRotate, true);
            return;
        }
        this.mIconController.setIconVisibility(this.mSlotRotate, false);
    }

    public void updateHeadsetPlug(Intent intent) {
        int i;
        boolean connected = intent.getIntExtra("state", 0) != 0;
        boolean hasMic = intent.getIntExtra("microphone", 0) != 0;
        Log.d("PhoneStatusBarPolicy", "updateHeadsetPlug connected:" + connected + ",hasMic:" + hasMic);
        if (connected) {
            Context context = this.mContext;
            if (hasMic) {
                i = com.android.systemui.R.string.accessibility_status_bar_headset;
            } else {
                i = com.android.systemui.R.string.accessibility_status_bar_headphones;
            }
            String contentDescription = context.getString(i);
            this.mIconController.setIcon(this.mSlotHeadset, hasMic ? com.android.systemui.R.drawable.ic_headset_mic : com.android.systemui.R.drawable.ic_headset, contentDescription);
            this.mIconController.setIconVisibility(this.mSlotHeadset, true);
            return;
        }
        this.mIconController.setIconVisibility(this.mSlotHeadset, false);
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        this.mIconController.setIconVisibility(this.mSlotDataSaver, isDataSaving);
    }

    public void registerAlarmClockChanged(int newUserId, boolean userSwitch) {
        if (userSwitch) {
            this.mContext.unregisterReceiver(this.mAlarmIntentReceiver);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
        Log.d("PhoneStatusBarPolicy", "registerAlarmClockChanged:" + newUserId);
        UserHandle newUserHandle = new UserHandle(newUserId);
        this.mContext.registerReceiverAsUser(this.mAlarmIntentReceiver, newUserHandle, filter, null, this.mHandler);
    }
}
