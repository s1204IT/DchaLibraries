package com.android.bluetooth.avrcp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothAvrcpTarget;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.UserManager;
import android.util.Log;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.Utils;
import com.android.bluetooth.a2dp.A2dpService;
import com.android.bluetooth.avrcp.MediaPlayerList;
import com.android.bluetooth.avrcp.MediaPlayerSettings;
import com.android.bluetooth.btservice.MetricsLogger;
import com.android.bluetooth.btservice.ProfileService;
import java.util.List;
import java.util.Objects;

public class AvrcpTargetService extends ProfileService {
    private static final String AVRCP_ENABLE_PROPERTY = "persist.bluetooth.enablenewavrcp";
    private static final int AVRCP_MAX_VOL = 127;
    private static final String TAG = "NewAvrcpTargetService";
    private AudioManager mAudioManager;
    private MediaData mCurrentData;
    private MediaPlayerList mMediaPlayerList;
    private MediaPlayerSettings mMediaPlayerSettings;
    private AvrcpNativeInterface mNativeInterface;
    private AvrcpBroadcastReceiver mReceiver;
    private AvrcpVolumeManager mVolumeManager;
    private static final boolean DEBUG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    private static int sDeviceMaxVolume = 0;
    private static AvrcpTargetService sInstance = null;

    class ListCallback implements MediaPlayerList.MediaUpdateCallback, MediaPlayerList.FolderUpdateCallback, MediaPlayerSettings.PlayerSettingsUpdateCallback {
        ListCallback() {
        }

        @Override
        public void run(MediaData mediaData) {
            boolean z = !Objects.equals(AvrcpTargetService.this.mCurrentData.metadata, mediaData.metadata);
            boolean z2 = !MediaPlayerWrapper.playstateEquals(AvrcpTargetService.this.mCurrentData.state, mediaData.state);
            boolean z3 = !Objects.equals(AvrcpTargetService.this.mCurrentData.queue, mediaData.queue);
            if (AvrcpTargetService.DEBUG) {
                Log.d(AvrcpTargetService.TAG, "onMediaUpdated: track_changed=" + z + " state=" + z2 + " queue=" + z3);
            }
            AvrcpTargetService.this.mCurrentData = mediaData;
            if (AvrcpTargetService.this.mNativeInterface == null) {
                return;
            }
            AvrcpTargetService.this.mNativeInterface.sendMediaUpdate(z, z2, z3);
        }

        @Override
        public void run(boolean z, boolean z2, boolean z3) {
            AvrcpTargetService.this.mNativeInterface.sendFolderUpdate(z, z2, z3);
        }

        @Override
        public void run(boolean z) {
            if (AvrcpTargetService.this.mNativeInterface == null) {
                return;
            }
            AvrcpTargetService.this.mNativeInterface.sendPlayerAppSettingsUpdate(z);
        }
    }

    private class AvrcpBroadcastReceiver extends BroadcastReceiver {
        private AvrcpBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals("android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED") || AvrcpTargetService.this.mNativeInterface == null) {
                return;
            }
            AvrcpTargetService.this.mNativeInterface.sendMediaUpdate(false, true, false);
        }
    }

    public static AvrcpTargetService get() {
        return sInstance;
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    protected ProfileService.IProfileServiceBinder initBinder() {
        return new AvrcpTargetBinder(this);
    }

    @Override
    protected void setUserUnlocked(int i) {
        Log.i(TAG, "User unlocked, initializing the service");
        if (!SystemProperties.getBoolean(AVRCP_ENABLE_PROPERTY, true)) {
            Log.w(TAG, "Skipping initialization of the new AVRCP Target Player List");
            sInstance = null;
        } else if (this.mMediaPlayerList != null) {
            this.mMediaPlayerList.init(new ListCallback());
        }
    }

    @Override
    protected boolean start() {
        if (sInstance != null) {
            Log.w(TAG, "The service has already been initialized");
            return true;
        }
        Log.i(TAG, "Starting the AVRCP Target Service");
        this.mCurrentData = new MediaData(null, null, null);
        this.mReceiver = new AvrcpBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED");
        registerReceiver(this.mReceiver, intentFilter);
        if (!SystemProperties.getBoolean(AVRCP_ENABLE_PROPERTY, true)) {
            Log.w(TAG, "Skipping initialization of the new AVRCP Target Service");
            sInstance = null;
            return true;
        }
        this.mAudioManager = (AudioManager) getSystemService("audio");
        sDeviceMaxVolume = this.mAudioManager.getStreamMaxVolume(3);
        this.mMediaPlayerList = new MediaPlayerList(Looper.myLooper(), this);
        this.mMediaPlayerSettings = new MediaPlayerSettings(Looper.myLooper(), this);
        if (UserManager.get(getApplicationContext()).isUserUnlocked()) {
            this.mMediaPlayerList.init(new ListCallback());
            this.mMediaPlayerSettings.init(new ListCallback());
        }
        this.mNativeInterface = AvrcpNativeInterface.getInterface();
        this.mNativeInterface.init(this);
        this.mVolumeManager = new AvrcpVolumeManager(this, this.mAudioManager, this.mNativeInterface);
        sInstance = this;
        return true;
    }

    @Override
    protected boolean stop() {
        Log.i(TAG, "Stopping the AVRCP Target Service");
        if (sInstance == null) {
            Log.w(TAG, "stop() called before start()");
            return true;
        }
        sInstance = null;
        unregisterReceiver(this.mReceiver);
        if (this.mMediaPlayerList != null) {
            this.mMediaPlayerList.cleanup();
        }
        if (this.mMediaPlayerSettings != null) {
            this.mMediaPlayerSettings.cleanup();
        }
        if (this.mNativeInterface != null) {
            this.mNativeInterface.cleanup();
        }
        this.mMediaPlayerList = null;
        this.mMediaPlayerSettings = null;
        this.mNativeInterface = null;
        this.mAudioManager = null;
        this.mReceiver = null;
        return true;
    }

    private void init() {
    }

    void deviceConnected(BluetoothDevice bluetoothDevice, boolean z) {
        Log.i(TAG, "deviceConnected: device=" + bluetoothDevice + " absoluteVolume=" + z);
        this.mVolumeManager.deviceConnected(bluetoothDevice, z);
        MetricsLogger.logProfileConnectionEvent(BluetoothMetricsProto.ProfileId.AVRCP);
    }

    void deviceDisconnected(BluetoothDevice bluetoothDevice) {
        Log.i(TAG, "deviceDisconnected: device=" + bluetoothDevice);
        this.mVolumeManager.deviceDisconnected(bluetoothDevice);
    }

    public void volumeDeviceSwitched(BluetoothDevice bluetoothDevice) {
        if (DEBUG) {
            Log.d(TAG, "volumeDeviceSwitched: device=" + bluetoothDevice);
        }
        this.mVolumeManager.volumeDeviceSwitched(bluetoothDevice);
    }

    public void storeVolumeForDevice(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            return;
        }
        this.mVolumeManager.storeVolumeForDevice(bluetoothDevice);
    }

    public int getRememberedVolumeForDevice(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            return -1;
        }
        return this.mVolumeManager.getVolume(bluetoothDevice, -1);
    }

    void setVolume(int i) {
        int iRound = (int) Math.round((((double) i) * ((double) sDeviceMaxVolume)) / 127.0d);
        if (DEBUG) {
            Log.d(TAG, "SendVolumeChanged: avrcpVolume=" + i + " deviceVolume=" + iRound + " sDeviceMaxVolume=" + sDeviceMaxVolume);
        }
        this.mAudioManager.setStreamVolume(3, iRound, 65);
    }

    public void sendVolumeChanged(int i) {
        int iFloor = (int) Math.floor((((double) i) * 127.0d) / ((double) sDeviceMaxVolume));
        if (iFloor > 127) {
            iFloor = 127;
        }
        if (DEBUG) {
            Log.d(TAG, "SendVolumeChanged: avrcpVolume=" + iFloor + " deviceVolume=" + i + " sDeviceMaxVolume=" + sDeviceMaxVolume);
        }
        this.mNativeInterface.sendVolumeChanged(iFloor);
    }

    Metadata getCurrentSongInfo() {
        return this.mMediaPlayerList.getCurrentSongInfo();
    }

    PlayStatus getPlayState() {
        return PlayStatus.fromPlaybackState(this.mMediaPlayerList.getCurrentPlayStatus(), Long.parseLong(getCurrentSongInfo().duration));
    }

    String getCurrentMediaId() {
        String currentMediaId = this.mMediaPlayerList.getCurrentMediaId();
        if (currentMediaId != null) {
            return currentMediaId;
        }
        Metadata currentSongInfo = getCurrentSongInfo();
        return currentSongInfo != null ? currentSongInfo.mediaId : "error";
    }

    List<Metadata> getNowPlayingList() {
        return this.mMediaPlayerList.getNowPlayingList();
    }

    int getCurrentPlayerId() {
        return this.mMediaPlayerList.getCurrentPlayerId();
    }

    List<PlayerInfo> getMediaPlayerList() {
        return this.mMediaPlayerList.getMediaPlayerList();
    }

    void getPlayerRoot(int i, MediaPlayerList.GetPlayerRootCallback getPlayerRootCallback) {
        this.mMediaPlayerList.getPlayerRoot(i, getPlayerRootCallback);
    }

    void getFolderItems(int i, String str, MediaPlayerList.GetFolderItemsCallback getFolderItemsCallback) {
        this.mMediaPlayerList.getFolderItems(i, str, getFolderItemsCallback);
    }

    void playItem(int i, boolean z, String str) {
        this.mMediaPlayerList.playItem(i, z, str);
    }

    void sendMediaKeyEvent(int i, boolean z) {
        if (DEBUG) {
            Log.d(TAG, "getMediaKeyEvent: event=" + i + " pushed=" + z);
        }
        this.mMediaPlayerList.sendMediaKeyEvent(i, z);
    }

    void setActiveDevice(BluetoothDevice bluetoothDevice) {
        Log.i(TAG, "setActiveDevice: device=" + bluetoothDevice);
        if (bluetoothDevice == null) {
            Log.wtfStack(TAG, "setActiveDevice: could not find device " + bluetoothDevice);
        }
        A2dpService.getA2dpService().setActiveDevice(bluetoothDevice);
    }

    void getListPlayerAttribute(MediaPlayerSettings.GetListPlayerAttributeCallback getListPlayerAttributeCallback) {
        this.mMediaPlayerSettings.getListPlayerAttribute(getListPlayerAttributeCallback);
    }

    void getListPlayerAttributeValues(byte b, MediaPlayerSettings.GetListPlayerAttributeValuesCallback getListPlayerAttributeValuesCallback) {
        this.mMediaPlayerSettings.getListPlayerAttributeValues(b, getListPlayerAttributeValuesCallback);
    }

    void getPlayerAttributeValue(byte b, int[] iArr, MediaPlayerSettings.GetPlayerAttributeValueCallback getPlayerAttributeValueCallback) {
        this.mMediaPlayerSettings.getPlayerAttributeValue(b, iArr, getPlayerAttributeValueCallback);
    }

    void setPlayerAppSetting(byte b, byte[] bArr, byte[] bArr2, MediaPlayerSettings.SetPlayerAppSettingCallback setPlayerAppSettingCallback) {
        this.mMediaPlayerSettings.setPlayerAppSetting(b, bArr, bArr2, setPlayerAppSettingCallback);
    }

    void getPlayerAttributeText(byte b, byte[] bArr, MediaPlayerSettings.GetPlayerAttributeTextCallback getPlayerAttributeTextCallback) {
        this.mMediaPlayerSettings.getPlayerAttributeText(b, bArr, getPlayerAttributeTextCallback);
    }

    void getPlayerAttributeTextValue(byte b, byte b2, byte[] bArr, MediaPlayerSettings.GetPlayerAttributeValueTextCallback getPlayerAttributeValueTextCallback) {
        this.mMediaPlayerSettings.getPlayerAttributeTextValue(b, b2, bArr, getPlayerAttributeValueTextCallback);
    }

    void getAppSettingChange(MediaPlayerSettings.GetAppSettingChangeCallback getAppSettingChangeCallback) {
        this.mMediaPlayerSettings.getAppSettingChange(getAppSettingChangeCallback);
    }

    @Override
    public void dump(StringBuilder sb) {
        sb.append("\nProfile: AvrcpTargetService:\n");
        if (sInstance == null) {
            sb.append("AvrcpTargetService not running");
            return;
        }
        StringBuilder sb2 = new StringBuilder();
        if (this.mMediaPlayerList != null) {
            this.mMediaPlayerList.dump(sb2);
        } else {
            sb2.append("\nMedia Player List is empty\n");
        }
        this.mVolumeManager.dump(sb2);
        sb.append(sb2.toString().replaceAll("(?m)^", "  "));
    }

    private static class AvrcpTargetBinder extends IBluetoothAvrcpTarget.Stub implements ProfileService.IProfileServiceBinder {
        private AvrcpTargetService mService;

        AvrcpTargetBinder(AvrcpTargetService avrcpTargetService) {
            this.mService = avrcpTargetService;
        }

        @Override
        public void cleanup() {
            this.mService = null;
        }

        public void sendVolumeChanged(int i) {
            if (!Utils.checkCaller()) {
                Log.w(AvrcpTargetService.TAG, "sendVolumeChanged not allowed for non-active user");
            } else {
                if (this.mService == null) {
                    return;
                }
                this.mService.sendVolumeChanged(i);
            }
        }
    }
}
