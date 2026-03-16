package com.android.systemui.statusbar.phone;

import android.app.AlarmManager;
import android.app.StatusBarManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import com.android.internal.telephony.IccCardConstants;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.HotspotController;

public class PhoneStatusBarPolicy {
    private static final boolean DEBUG = Log.isLoggable("PhoneStatusBarPolicy", 3);
    private final CastController mCast;
    private final Context mContext;
    private final HotspotController mHotspot;
    private final StatusBarManager mService;
    private Toast mToast;
    private boolean mVolumeVisible;
    private int mZen;
    private boolean mZenVisible;
    private final Handler mHandler = new Handler();
    IccCardConstants.State mSimState = IccCardConstants.State.READY;
    private boolean mBluetoothEnabled = false;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.app.action.NEXT_ALARM_CLOCK_CHANGED")) {
                PhoneStatusBarPolicy.this.updateAlarm();
                return;
            }
            if (action.equals("android.intent.action.SYNC_STATE_CHANGED")) {
                PhoneStatusBarPolicy.this.updateSyncState(intent);
                return;
            }
            if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED") || action.equals("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")) {
                PhoneStatusBarPolicy.this.updateBluetooth();
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
            } else if (action.equals("android.intent.action.USER_SWITCHED")) {
                PhoneStatusBarPolicy.this.updateAlarm();
            } else if (action.equals("jp.panasonic.sanyo.ts.tablet.CHANGE_PEN_MODE")) {
                PhoneStatusBarPolicy.this.updateTouchMode(intent);
            }
        }
    };
    private Runnable mRemoveCastIconRunnable = new Runnable() {
        @Override
        public void run() {
            if (PhoneStatusBarPolicy.DEBUG) {
                Log.v("PhoneStatusBarPolicy", "updateCast: hiding icon NOW");
            }
            PhoneStatusBarPolicy.this.mService.setIconVisibility("cast", false);
        }
    };
    private final HotspotController.Callback mHotspotCallback = new HotspotController.Callback() {
        @Override
        public void onHotspotChanged(boolean enabled) {
            PhoneStatusBarPolicy.this.mService.setIconVisibility("hotspot", enabled);
        }
    };
    private final CastController.Callback mCastCallback = new CastController.Callback() {
        @Override
        public void onCastDevicesChanged() {
            PhoneStatusBarPolicy.this.updateCast();
        }
    };

    public PhoneStatusBarPolicy(Context context, CastController cast, HotspotController hotspot) {
        this.mContext = context;
        this.mCast = cast;
        this.mHotspot = hotspot;
        this.mService = (StatusBarManager) context.getSystemService("statusbar");
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
        filter.addAction("android.intent.action.SYNC_STATE_CHANGED");
        filter.addAction("android.media.RINGER_MODE_CHANGED");
        filter.addAction("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION");
        filter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        filter.addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED");
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("android.telecom.action.CURRENT_TTY_MODE_CHANGED");
        filter.addAction("android.intent.action.USER_SWITCHED");
        filter.addAction("jp.panasonic.sanyo.ts.tablet.CHANGE_PEN_MODE");
        this.mContext.registerReceiver(this.mIntentReceiver, filter, null, this.mHandler);
        this.mService.setIcon("tty", R.drawable.stat_sys_tty_mode, 0, null);
        this.mService.setIconVisibility("tty", false);
        this.mService.setIcon("cdma_eri", R.drawable.stat_sys_roaming_cdma_0, 0, null);
        this.mService.setIconVisibility("cdma_eri", false);
        updateBluetooth();
        this.mService.setIcon("alarm_clock", R.drawable.stat_sys_alarm, 0, null);
        this.mService.setIconVisibility("alarm_clock", false);
        this.mService.setIcon("sync_active", R.drawable.stat_sys_sync, 0, null);
        this.mService.setIconVisibility("sync_active", false);
        this.mService.setIcon("zen", R.drawable.stat_sys_zen_important, 0, null);
        this.mService.setIconVisibility("zen", false);
        this.mService.setIcon("volume", R.drawable.stat_sys_ringer_vibrate, 0, null);
        this.mService.setIconVisibility("volume", false);
        updateVolumeZen();
        this.mService.setIcon("cast", R.drawable.stat_sys_cast, 0, null);
        this.mService.setIconVisibility("cast", false);
        this.mCast.addCallback(this.mCastCallback);
        this.mService.setIcon("hotspot", R.drawable.stat_sys_hotspot, 0, null);
        this.mService.setIconVisibility("hotspot", this.mHotspot.isHotspotEnabled());
        this.mHotspot.addCallback(this.mHotspotCallback);
        this.mService.setIcon("touch", R.drawable.stat_sys_pen_mode, 0, null);
        this.mService.setIconVisibility("touch", Settings.System.getInt(this.mContext.getContentResolver(), "pen_mode", 0) == 1);
    }

    public void setZenMode(int zen) {
        this.mZen = zen;
        updateVolumeZen();
    }

    private void updateAlarm() {
        AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        boolean alarmSet = alarmManager.getNextAlarmClock(-2) != null;
        this.mService.setIconVisibility("alarm_clock", alarmSet);
    }

    private final void updateSyncState(Intent intent) {
    }

    private final void updateSimState(Intent intent) {
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

    private final void updateVolumeZen() {
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
        boolean zenVisible = false;
        int zenIconId = 0;
        String zenDescription = null;
        boolean volumeVisible = false;
        int volumeIconId = 0;
        String volumeDescription = null;
        if (this.mZen == 2) {
            zenVisible = true;
            zenIconId = R.drawable.stat_sys_zen_none;
            zenDescription = this.mContext.getString(R.string.zen_no_interruptions);
        } else if (this.mZen == 1) {
            zenVisible = true;
            zenIconId = R.drawable.stat_sys_zen_important;
            zenDescription = this.mContext.getString(R.string.zen_important_interruptions);
        }
        if (this.mZen != 2 && audioManager.getRingerModeInternal() == 1) {
            volumeVisible = true;
            volumeIconId = R.drawable.stat_sys_ringer_vibrate;
            volumeDescription = this.mContext.getString(R.string.accessibility_ringer_vibrate);
        }
        if (zenVisible) {
            this.mService.setIcon("zen", zenIconId, 0, zenDescription);
        }
        if (zenVisible != this.mZenVisible) {
            this.mService.setIconVisibility("zen", zenVisible);
            this.mZenVisible = zenVisible;
        }
        if (volumeVisible) {
            this.mService.setIcon("volume", volumeIconId, 0, volumeDescription);
        }
        if (volumeVisible != this.mVolumeVisible) {
            this.mService.setIconVisibility("volume", volumeVisible);
            this.mVolumeVisible = volumeVisible;
        }
    }

    private final void updateBluetooth() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        int iconId = R.drawable.stat_sys_data_bluetooth;
        String contentDescription = this.mContext.getString(R.string.accessibility_bluetooth_disconnected);
        if (adapter != null) {
            this.mBluetoothEnabled = adapter.getState() == 12;
            if (adapter.getConnectionState() == 2) {
                iconId = R.drawable.stat_sys_data_bluetooth_connected;
                contentDescription = this.mContext.getString(R.string.accessibility_bluetooth_connected);
            }
        } else {
            this.mBluetoothEnabled = false;
        }
        this.mService.setIcon("bluetooth", iconId, 0, contentDescription);
        this.mService.setIconVisibility("bluetooth", this.mBluetoothEnabled);
    }

    private final void updateTTY(Intent intent) {
        int currentTtyMode = intent.getIntExtra("android.telecom.intent.extra.CURRENT_TTY_MODE", 0);
        boolean enabled = currentTtyMode != 0;
        if (DEBUG) {
            Log.v("PhoneStatusBarPolicy", "updateTTY: enabled: " + enabled);
        }
        if (enabled) {
            if (DEBUG) {
                Log.v("PhoneStatusBarPolicy", "updateTTY: set TTY on");
            }
            this.mService.setIcon("tty", R.drawable.stat_sys_tty_mode, 0, this.mContext.getString(R.string.accessibility_tty_enabled));
            this.mService.setIconVisibility("tty", true);
            return;
        }
        if (DEBUG) {
            Log.v("PhoneStatusBarPolicy", "updateTTY: set TTY off");
        }
        this.mService.setIconVisibility("tty", false);
    }

    private void updateCast() {
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
            this.mService.setIcon("cast", R.drawable.stat_sys_cast, 0, this.mContext.getString(R.string.accessibility_casting));
            this.mService.setIconVisibility("cast", true);
        } else {
            if (DEBUG) {
                Log.v("PhoneStatusBarPolicy", "updateCast: hiding icon in 3 sec...");
            }
            this.mHandler.postDelayed(this.mRemoveCastIconRunnable, 3000L);
        }
    }

    private final void updateTouchMode(Intent intent) {
        boolean touch = intent.getBooleanExtra("pen_mode", false);
        this.mService.setIconVisibility("touch", touch);
        if (this.mToast != null) {
            this.mToast.cancel();
        }
        this.mToast = Toast.makeText(this.mContext, touch ? R.string.touch_panel_mode_off : R.string.touch_panel_mode_on, 3000);
        this.mToast.show();
    }
}
