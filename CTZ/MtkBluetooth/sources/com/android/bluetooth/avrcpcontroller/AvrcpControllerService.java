package com.android.bluetooth.avrcpcontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAvrcpPlayerSettings;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothAvrcpController;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import com.android.bluetooth.Utils;
import com.android.bluetooth.a2dpsink.A2dpSinkStateMachine;
import com.android.bluetooth.btservice.ProfileService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class AvrcpControllerService extends ProfileService {
    public static final String ACTION_BROWSE_CONNECTION_STATE_CHANGED = "android.bluetooth.avrcp-controller.profile.action.BROWSE_CONNECTION_STATE_CHANGED";
    public static final String ACTION_FOLDER_LIST = "android.bluetooth.avrcp-controller.profile.action.FOLDER_LIST";
    public static final String ACTION_TRACK_EVENT = "android.bluetooth.avrcp-controller.profile.action.TRACK_EVENT";
    public static final int BROWSE_SCOPE_NOW_PLAYING = 3;
    public static final int BROWSE_SCOPE_PLAYER_LIST = 0;
    public static final int BROWSE_SCOPE_SEARCH = 2;
    public static final int BROWSE_SCOPE_VFS = 1;
    static final boolean DBG = false;
    private static final byte[] EMPTY_UID = {0, 0, 0, 0, 0, 0, 0, 0};
    public static final String EXTRA_FOLDER_BT_ID = "com.android.bluetooth.avrcp-controller.EXTRA_FOLDER_BT_ID";
    public static final String EXTRA_FOLDER_ID = "com.android.bluetooth.avrcp.EXTRA_FOLDER_ID";
    public static final String EXTRA_FOLDER_LIST = "android.bluetooth.avrcp-controller.profile.extra.FOLDER_LIST";
    public static final String EXTRA_METADATA = "android.bluetooth.avrcp-controller.profile.extra.METADATA";
    public static final String EXTRA_PLAYBACK = "android.bluetooth.avrcp-controller.profile.extra.PLAYBACK";
    public static final int FOLDER_NAVIGATION_DIRECTION_DOWN = 1;
    public static final int FOLDER_NAVIGATION_DIRECTION_UP = 0;
    private static final int JNI_AVRC_INV_RANGE = 11;
    private static final int JNI_AVRC_STS_NO_ERROR = 4;
    private static final int JNI_FOLDER_TYPE_ALBUMS = 2;
    private static final int JNI_FOLDER_TYPE_ARTISTS = 3;
    private static final int JNI_FOLDER_TYPE_GENRES = 4;
    private static final int JNI_FOLDER_TYPE_PLAYLISTS = 5;
    private static final int JNI_FOLDER_TYPE_TITLES = 1;
    private static final int JNI_FOLDER_TYPE_YEARS = 6;
    private static final int JNI_MEDIA_ATTR_ID_ALBUM = 3;
    private static final int JNI_MEDIA_ATTR_ID_ARTIST = 2;
    private static final int JNI_MEDIA_ATTR_ID_GENRE = 6;
    private static final int JNI_MEDIA_ATTR_ID_INVALID = -1;
    private static final int JNI_MEDIA_ATTR_ID_NUM_TRACKS = 5;
    private static final int JNI_MEDIA_ATTR_ID_PLAYING_TIME = 7;
    private static final int JNI_MEDIA_ATTR_ID_TITLE = 1;
    private static final int JNI_MEDIA_ATTR_ID_TRACK_NUM = 4;
    private static final byte JNI_PLAY_STATUS_ERROR = -1;
    private static final byte JNI_PLAY_STATUS_FWD_SEEK = 3;
    private static final byte JNI_PLAY_STATUS_PAUSED = 2;
    private static final byte JNI_PLAY_STATUS_PLAYING = 1;
    private static final byte JNI_PLAY_STATUS_REV_SEEK = 4;
    private static final byte JNI_PLAY_STATUS_STOPPED = 0;
    public static final int KEY_STATE_PRESSED = 0;
    public static final int KEY_STATE_RELEASED = 1;
    public static final String MEDIA_ITEM_UID_KEY = "media-item-uid-key";
    public static final int PASS_THRU_CMD_ID_BACKWARD = 76;
    public static final int PASS_THRU_CMD_ID_FF = 73;
    public static final int PASS_THRU_CMD_ID_FORWARD = 75;
    public static final int PASS_THRU_CMD_ID_NEXT_GRP = 0;
    public static final int PASS_THRU_CMD_ID_PAUSE = 70;
    public static final int PASS_THRU_CMD_ID_PLAY = 68;
    public static final int PASS_THRU_CMD_ID_PREV_GRP = 1;
    public static final int PASS_THRU_CMD_ID_REWIND = 72;
    public static final int PASS_THRU_CMD_ID_STOP = 69;
    public static final int PASS_THRU_CMD_ID_VOL_DOWN = 66;
    public static final int PASS_THRU_CMD_ID_VOL_UP = 65;
    static final String TAG = "AvrcpControllerService";
    static final boolean VDBG = false;
    private static AvrcpControllerService sAvrcpControllerService;
    private AvrcpControllerStateMachine mAvrcpCtSm;
    private BluetoothDevice mConnectedDevice = null;
    private boolean mBrowseConnected = false;
    private String mCurrentBrowseFolderUID = null;

    static native void changeFolderPathNative(byte[] bArr, byte b, byte[] bArr2);

    private static native void classInitNative();

    private native void cleanupNative();

    static native void getFolderListNative(byte[] bArr, int i, int i2);

    static native void getNowPlayingListNative(byte[] bArr, int i, int i2);

    static native void getPlaybackStateNative(byte[] bArr);

    static native void getPlayerListNative(byte[] bArr, int i, int i2);

    private native void initNative();

    static native void playItemNative(byte[] bArr, byte b, byte[] bArr2, int i);

    static native void sendAbsVolRspNative(byte[] bArr, int i, int i2);

    static native boolean sendGroupNavigationCommandNative(byte[] bArr, int i, int i2);

    static native boolean sendPassThroughCommandNative(byte[] bArr, int i, int i2);

    static native void sendRegisterAbsVolRspNative(byte[] bArr, byte b, int i, int i2);

    static native void setAddressedPlayerNative(byte[] bArr, int i);

    static native void setBrowsedPlayerNative(byte[] bArr, int i);

    static native void setPlayerApplicationSettingValuesNative(byte[] bArr, byte b, byte[] bArr2, byte[] bArr3);

    static {
        classInitNative();
    }

    public AvrcpControllerService() {
        initNative();
    }

    @Override
    protected ProfileService.IProfileServiceBinder initBinder() {
        return new BluetoothAvrcpControllerBinder(this);
    }

    @Override
    protected boolean start() {
        new HandlerThread("BluetoothAvrcpHandler").start();
        this.mAvrcpCtSm = new AvrcpControllerStateMachine(this);
        this.mAvrcpCtSm.start();
        setAvrcpControllerService(this);
        return true;
    }

    @Override
    protected boolean stop() {
        setAvrcpControllerService(null);
        if (this.mAvrcpCtSm != null) {
            this.mAvrcpCtSm.doQuit();
            return true;
        }
        return true;
    }

    public static synchronized AvrcpControllerService getAvrcpControllerService() {
        if (sAvrcpControllerService == null) {
            Log.w(TAG, "getAvrcpControllerService(): service is null");
            return null;
        }
        if (!sAvrcpControllerService.isAvailable()) {
            Log.w(TAG, "getAvrcpControllerService(): service is not available ");
            return null;
        }
        return sAvrcpControllerService;
    }

    private static synchronized void setAvrcpControllerService(AvrcpControllerService avrcpControllerService) {
        sAvrcpControllerService = avrcpControllerService;
    }

    public synchronized List<BluetoothDevice> getConnectedDevices() {
        ArrayList arrayList;
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        arrayList = new ArrayList();
        if (this.mConnectedDevice != null) {
            arrayList.add(this.mConnectedDevice);
        }
        return arrayList;
    }

    public synchronized List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        ArrayList arrayList;
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        arrayList = new ArrayList();
        for (int i : iArr) {
            if (i == 2 && this.mConnectedDevice != null) {
                arrayList.add(this.mConnectedDevice);
            }
        }
        return arrayList;
    }

    public synchronized int getConnectionState(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return this.mConnectedDevice != null ? 2 : 0;
    }

    public synchronized void sendGroupNavigationCmd(BluetoothDevice bluetoothDevice, int i, int i2) {
        Log.v(TAG, "sendGroupNavigationCmd keyCode: " + i + " keyState: " + i2);
        if (bluetoothDevice == null) {
            Log.e(TAG, "sendGroupNavigationCmd device is null");
        }
        if (!bluetoothDevice.equals(this.mConnectedDevice)) {
            Log.e(TAG, " Device does not match " + bluetoothDevice + " connected " + this.mConnectedDevice);
            return;
        }
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        this.mAvrcpCtSm.sendMessage(this.mAvrcpCtSm.obtainMessage(3, i, i2, bluetoothDevice));
    }

    public synchronized void sendPassThroughCmd(BluetoothDevice bluetoothDevice, int i, int i2) {
        Log.v(TAG, "sendPassThroughCmd keyCode: " + i + " keyState: " + i2);
        if (bluetoothDevice == null) {
            Log.e(TAG, "sendPassThroughCmd Device is null");
            return;
        }
        if (!bluetoothDevice.equals(this.mConnectedDevice)) {
            Log.w(TAG, " Device does not match device " + bluetoothDevice + " conn " + this.mConnectedDevice);
            return;
        }
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        this.mAvrcpCtSm.sendMessage(this.mAvrcpCtSm.obtainMessage(1, i, i2, bluetoothDevice));
    }

    public void startAvrcpUpdates() {
        this.mAvrcpCtSm.obtainMessage(202).sendToTarget();
    }

    public void stopAvrcpUpdates() {
        this.mAvrcpCtSm.obtainMessage(201).sendToTarget();
    }

    public synchronized MediaMetadata getMetaData(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (bluetoothDevice == null) {
            Log.e(TAG, "getMetadata device is null");
            return null;
        }
        if (!bluetoothDevice.equals(this.mConnectedDevice)) {
            return null;
        }
        return this.mAvrcpCtSm.getCurrentMetaData();
    }

    public PlaybackState getPlaybackState(BluetoothDevice bluetoothDevice) {
        return getPlaybackState(bluetoothDevice, true);
    }

    public synchronized PlaybackState getPlaybackState(BluetoothDevice bluetoothDevice, boolean z) {
        if (bluetoothDevice == null) {
            Log.e(TAG, "getPlaybackState device is null");
            return null;
        }
        if (!bluetoothDevice.equals(this.mConnectedDevice)) {
            Log.e(TAG, "Device " + bluetoothDevice + " does not match connected deivce " + this.mConnectedDevice);
            return null;
        }
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return this.mAvrcpCtSm.getCurrentPlayBackState(z);
    }

    public synchronized BluetoothAvrcpPlayerSettings getPlayerSettings(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            Log.e(TAG, "getPlayerSettings device is null");
            return null;
        }
        if (!bluetoothDevice.equals(this.mConnectedDevice)) {
            Log.e(TAG, "device " + bluetoothDevice + " does not match connected device " + this.mConnectedDevice);
            return null;
        }
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return null;
    }

    public boolean setPlayerApplicationSetting(BluetoothAvrcpPlayerSettings bluetoothAvrcpPlayerSettings) {
        return false;
    }

    public synchronized boolean getChildren(BluetoothDevice bluetoothDevice, String str, int i, int i2) {
        if (bluetoothDevice == null) {
            Log.e(TAG, "getChildren device is null");
            return false;
        }
        if (!bluetoothDevice.equals(this.mConnectedDevice)) {
            Log.e(TAG, "getChildren device " + bluetoothDevice + " does not match " + this.mConnectedDevice);
            return false;
        }
        if (!this.mBrowseConnected) {
            Log.e(TAG, "getChildren browse not yet connected");
            return false;
        }
        if (!this.mAvrcpCtSm.isConnected()) {
            return false;
        }
        this.mAvrcpCtSm.getChildren(str, i, i2);
        return true;
    }

    public synchronized boolean getNowPlayingList(BluetoothDevice bluetoothDevice, String str, int i, int i2) {
        if (bluetoothDevice == null) {
            Log.e(TAG, "getNowPlayingList device is null");
            return false;
        }
        if (!bluetoothDevice.equals(this.mConnectedDevice)) {
            Log.e(TAG, "getNowPlayingList device " + bluetoothDevice + " does not match " + this.mConnectedDevice);
            return false;
        }
        if (!this.mBrowseConnected) {
            Log.e(TAG, "getNowPlayingList browse not yet connected");
            return false;
        }
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        this.mAvrcpCtSm.sendMessage(this.mAvrcpCtSm.obtainMessage(5, i, i2, str));
        return true;
    }

    public synchronized boolean getFolderList(BluetoothDevice bluetoothDevice, String str, int i, int i2) {
        if (bluetoothDevice == null) {
            Log.e(TAG, "getFolderListing device is null");
            return false;
        }
        if (!bluetoothDevice.equals(this.mConnectedDevice)) {
            Log.e(TAG, "getFolderListing device " + bluetoothDevice + " does not match " + this.mConnectedDevice);
            return false;
        }
        if (!this.mBrowseConnected) {
            Log.e(TAG, "getFolderListing browse not yet connected");
            return false;
        }
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        this.mAvrcpCtSm.sendMessage(this.mAvrcpCtSm.obtainMessage(6, i, i2, str));
        return true;
    }

    public synchronized boolean getPlayerList(BluetoothDevice bluetoothDevice, int i, int i2) {
        if (bluetoothDevice == null) {
            Log.e(TAG, "getPlayerList device is null");
            return false;
        }
        if (!bluetoothDevice.equals(this.mConnectedDevice)) {
            Log.e(TAG, "getPlayerList device " + bluetoothDevice + " does not match " + this.mConnectedDevice);
            return false;
        }
        if (!this.mBrowseConnected) {
            Log.e(TAG, "getPlayerList browse not yet connected");
            return false;
        }
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        this.mAvrcpCtSm.sendMessage(this.mAvrcpCtSm.obtainMessage(7, i, i2));
        return true;
    }

    public synchronized boolean changeFolderPath(BluetoothDevice bluetoothDevice, int i, String str, String str2) {
        if (bluetoothDevice == null) {
            Log.e(TAG, "changeFolderPath device is null");
            return false;
        }
        if (!bluetoothDevice.equals(this.mConnectedDevice)) {
            Log.e(TAG, "changeFolderPath device " + bluetoothDevice + " does not match " + this.mConnectedDevice);
            return false;
        }
        if (!this.mBrowseConnected) {
            Log.e(TAG, "changeFolderPath browse not yet connected");
            return false;
        }
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_FOLDER_ID, str2);
        bundle.putString(EXTRA_FOLDER_BT_ID, str);
        this.mAvrcpCtSm.sendMessage(this.mAvrcpCtSm.obtainMessage(8, i, 0, bundle));
        return true;
    }

    public synchronized boolean setBrowsedPlayer(BluetoothDevice bluetoothDevice, int i, String str) {
        if (bluetoothDevice == null) {
            Log.e(TAG, "setBrowsedPlayer device is null");
            return false;
        }
        if (!bluetoothDevice.equals(this.mConnectedDevice)) {
            Log.e(TAG, "changeFolderPath device " + bluetoothDevice + " does not match " + this.mConnectedDevice);
            return false;
        }
        if (!this.mBrowseConnected) {
            Log.e(TAG, "setBrowsedPlayer browse not yet connected");
            return false;
        }
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        this.mAvrcpCtSm.sendMessage(this.mAvrcpCtSm.obtainMessage(10, i, 0, str));
        return true;
    }

    public synchronized void fetchAttrAndPlayItem(BluetoothDevice bluetoothDevice, String str) {
        if (bluetoothDevice == null) {
            Log.e(TAG, "fetchAttrAndPlayItem device is null");
            return;
        }
        if (!bluetoothDevice.equals(this.mConnectedDevice)) {
            Log.e(TAG, "fetchAttrAndPlayItem device " + bluetoothDevice + " does not match " + this.mConnectedDevice);
            return;
        }
        if (!this.mBrowseConnected) {
            Log.e(TAG, "fetchAttrAndPlayItem browse not yet connected");
        } else {
            this.mAvrcpCtSm.fetchAttrAndPlayItem(str);
        }
    }

    private static class BluetoothAvrcpControllerBinder extends IBluetoothAvrcpController.Stub implements ProfileService.IProfileServiceBinder {
        private AvrcpControllerService mService;

        private AvrcpControllerService getService() {
            if (!Utils.checkCaller()) {
                Log.w(AvrcpControllerService.TAG, "AVRCP call not allowed for non-active user");
                return null;
            }
            if (this.mService == null || !this.mService.isAvailable()) {
                return null;
            }
            return this.mService;
        }

        BluetoothAvrcpControllerBinder(AvrcpControllerService avrcpControllerService) {
            this.mService = avrcpControllerService;
        }

        @Override
        public void cleanup() {
            this.mService = null;
        }

        public List<BluetoothDevice> getConnectedDevices() {
            AvrcpControllerService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getConnectedDevices();
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
            AvrcpControllerService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getDevicesMatchingConnectionStates(iArr);
        }

        public int getConnectionState(BluetoothDevice bluetoothDevice) {
            AvrcpControllerService service = getService();
            if (service == null) {
                return 0;
            }
            if (bluetoothDevice == null) {
                throw new IllegalStateException("Device cannot be null!");
            }
            return service.getConnectionState(bluetoothDevice);
        }

        public void sendGroupNavigationCmd(BluetoothDevice bluetoothDevice, int i, int i2) {
            Log.v(AvrcpControllerService.TAG, "Binder Call: sendGroupNavigationCmd");
            AvrcpControllerService service = getService();
            if (service == null) {
                return;
            }
            if (bluetoothDevice == null) {
                throw new IllegalStateException("Device cannot be null!");
            }
            service.sendGroupNavigationCmd(bluetoothDevice, i, i2);
        }

        public BluetoothAvrcpPlayerSettings getPlayerSettings(BluetoothDevice bluetoothDevice) {
            Log.v(AvrcpControllerService.TAG, "Binder Call: getPlayerApplicationSetting ");
            AvrcpControllerService service = getService();
            if (service == null) {
                return null;
            }
            if (bluetoothDevice == null) {
                throw new IllegalStateException("Device cannot be null!");
            }
            return service.getPlayerSettings(bluetoothDevice);
        }

        public boolean setPlayerApplicationSetting(BluetoothAvrcpPlayerSettings bluetoothAvrcpPlayerSettings) {
            Log.v(AvrcpControllerService.TAG, "Binder Call: setPlayerApplicationSetting ");
            AvrcpControllerService service = getService();
            if (service == null) {
                return false;
            }
            return service.setPlayerApplicationSetting(bluetoothAvrcpPlayerSettings);
        }
    }

    private void handlePassthroughRsp(int i, int i2, byte[] bArr) {
        Log.d(TAG, "passthrough response received as: key: " + i + " state: " + i2 + "address:" + bArr);
    }

    private void handleGroupNavigationRsp(int i, int i2) {
        Log.d(TAG, "group navigation response received as: key: " + i + " state: " + i2);
    }

    private synchronized void onConnectionStateChanged(boolean z, boolean z2, byte[] bArr) {
        BluetoothDevice remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(bArr);
        Log.d(TAG, "onConnectionStateChanged " + z + " " + z2 + remoteDevice + " conn device " + this.mConnectedDevice);
        if (remoteDevice == null) {
            Log.e(TAG, "onConnectionStateChanged Device is null");
            return;
        }
        int i = remoteDevice.equals(this.mConnectedDevice) ? 2 : 0;
        int i2 = z ? 2 : 0;
        if (z && i == 0) {
            if (this.mConnectedDevice != null) {
                Log.d(TAG, "A Connection already exists, returning");
                return;
            } else {
                this.mConnectedDevice = remoteDevice;
                this.mAvrcpCtSm.sendMessage(this.mAvrcpCtSm.obtainMessage(A2dpSinkStateMachine.EVENT_AVRCP_CT_PAUSE, i2, i, remoteDevice));
            }
        } else if (!z && i == 2) {
            this.mConnectedDevice = null;
            this.mAvrcpCtSm.sendMessage(this.mAvrcpCtSm.obtainMessage(A2dpSinkStateMachine.EVENT_AVRCP_CT_PAUSE, i2, i, remoteDevice));
        }
        if (z && z2) {
            this.mBrowseConnected = true;
            Message messageObtainMessage = this.mAvrcpCtSm.obtainMessage(A2dpSinkStateMachine.EVENT_AVRCP_TG_PLAY);
            messageObtainMessage.arg1 = 1;
            messageObtainMessage.obj = remoteDevice;
            this.mAvrcpCtSm.sendMessage(messageObtainMessage);
        }
    }

    private void getRcFeatures(byte[] bArr, int i) {
        this.mAvrcpCtSm.sendMessage(this.mAvrcpCtSm.obtainMessage(A2dpSinkStateMachine.EVENT_AVRCP_CT_PLAY, i, 0, BluetoothAdapter.getDefaultAdapter().getRemoteDevice(bArr)));
    }

    private void setPlayerAppSettingRsp(byte[] bArr, byte b) {
    }

    private synchronized void handleRegisterNotificationAbsVol(byte[] bArr, byte b) {
        Log.d(TAG, "handleRegisterNotificationAbsVol ");
        BluetoothDevice remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(bArr);
        if (remoteDevice != null && !remoteDevice.equals(this.mConnectedDevice)) {
            Log.e(TAG, "handleRegisterNotificationAbsVol device not found " + bArr);
            return;
        }
        this.mAvrcpCtSm.sendMessage(this.mAvrcpCtSm.obtainMessage(104, b, 0));
    }

    private synchronized void handleSetAbsVolume(byte[] bArr, byte b, byte b2) {
        Log.d(TAG, "handleSetAbsVolume ");
        BluetoothDevice remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(bArr);
        if (remoteDevice != null && !remoteDevice.equals(this.mConnectedDevice)) {
            Log.e(TAG, "handleSetAbsVolume device not found " + bArr);
            return;
        }
        this.mAvrcpCtSm.sendMessage(this.mAvrcpCtSm.obtainMessage(103, b, b2));
    }

    private synchronized void onTrackChanged(byte[] bArr, byte b, int[] iArr, String[] strArr) {
        BluetoothDevice remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(bArr);
        if (remoteDevice != null && !remoteDevice.equals(this.mConnectedDevice)) {
            Log.e(TAG, "onTrackChanged device not found " + bArr);
            return;
        }
        ArrayList arrayList = new ArrayList();
        for (int i : iArr) {
            arrayList.add(Integer.valueOf(i));
        }
        this.mAvrcpCtSm.sendMessage(this.mAvrcpCtSm.obtainMessage(105, new TrackInfo(arrayList, Arrays.asList(strArr))));
    }

    private synchronized void onPlayPositionChanged(byte[] bArr, int i, int i2) {
        BluetoothDevice remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(bArr);
        if (remoteDevice != null && !remoteDevice.equals(this.mConnectedDevice)) {
            Log.e(TAG, "onPlayPositionChanged not found device not found " + bArr);
            return;
        }
        this.mAvrcpCtSm.sendMessage(this.mAvrcpCtSm.obtainMessage(106, i, i2));
    }

    private synchronized void onPlayStatusChanged(byte[] bArr, byte b) {
        BluetoothDevice remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(bArr);
        if (remoteDevice != null && !remoteDevice.equals(this.mConnectedDevice)) {
            Log.e(TAG, "onPlayStatusChanged not found device not found " + bArr);
            return;
        }
        int i = 4;
        switch (b) {
            case 0:
                i = 1;
                break;
            case 1:
                i = 3;
                break;
            case 2:
                i = 2;
                break;
            case 3:
            case 4:
                break;
            default:
                i = 0;
                break;
        }
        this.mAvrcpCtSm.sendMessage(this.mAvrcpCtSm.obtainMessage(107, i));
    }

    private synchronized void handlePlayerAppSetting(byte[] bArr, byte[] bArr2, int i) {
        BluetoothDevice remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(bArr);
        if (remoteDevice != null && !remoteDevice.equals(this.mConnectedDevice)) {
            Log.e(TAG, "handlePlayerAppSetting not found device not found " + bArr);
            return;
        }
        PlayerApplicationSettings.makeSupportedSettings(bArr2);
    }

    private synchronized void onPlayerAppSettingChanged(byte[] bArr, byte[] bArr2, int i) {
        BluetoothDevice remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(bArr);
        if (remoteDevice != null && !remoteDevice.equals(this.mConnectedDevice)) {
            Log.e(TAG, "onPlayerAppSettingChanged not found device not found " + bArr);
            return;
        }
        PlayerApplicationSettings.makeSettings(bArr2);
    }

    void handleGetFolderItemsRsp(int i, MediaBrowser.MediaItem[] mediaItemArr) {
        if (i == 11) {
            Log.w(TAG, "Sending out of range message.");
            this.mAvrcpCtSm.sendMessage(this.mAvrcpCtSm.obtainMessage(110));
            return;
        }
        for (MediaBrowser.MediaItem mediaItem : mediaItemArr) {
        }
        ArrayList arrayList = new ArrayList();
        for (MediaBrowser.MediaItem mediaItem2 : mediaItemArr) {
            arrayList.add(mediaItem2);
        }
        this.mAvrcpCtSm.sendMessage(this.mAvrcpCtSm.obtainMessage(109, arrayList));
    }

    void handleGetPlayerItemsRsp(AvrcpPlayer[] avrcpPlayerArr) {
        for (AvrcpPlayer avrcpPlayer : avrcpPlayerArr) {
        }
        ArrayList arrayList = new ArrayList();
        for (AvrcpPlayer avrcpPlayer2 : avrcpPlayerArr) {
            arrayList.add(avrcpPlayer2);
        }
        this.mAvrcpCtSm.sendMessage(this.mAvrcpCtSm.obtainMessage(111, arrayList));
    }

    MediaBrowser.MediaItem createFromNativeMediaItem(byte[] bArr, int i, String str, int[] iArr, String[] strArr) {
        MediaDescription.Builder builder = new MediaDescription.Builder();
        Bundle bundle = new Bundle();
        bundle.putString(MEDIA_ITEM_UID_KEY, byteUIDToHexString(bArr));
        builder.setExtras(bundle);
        builder.setMediaId(UUID.randomUUID().toString());
        builder.setTitle(str);
        return new MediaBrowser.MediaItem(builder.build(), 2);
    }

    MediaBrowser.MediaItem createFromNativeFolderItem(byte[] bArr, int i, String str, int i2) {
        MediaDescription.Builder builder = new MediaDescription.Builder();
        Bundle bundle = new Bundle();
        bundle.putString(MEDIA_ITEM_UID_KEY, byteUIDToHexString(bArr));
        builder.setExtras(bundle);
        builder.setMediaId(UUID.randomUUID().toString());
        builder.setTitle(str);
        return new MediaBrowser.MediaItem(builder.build(), 1);
    }

    AvrcpPlayer createFromNativePlayerItem(int i, String str, byte[] bArr, int i2, int i3) {
        return new AvrcpPlayer(i, str, 0, i2, i3);
    }

    private void handleChangeFolderRsp(int i) {
        this.mAvrcpCtSm.sendMessage(this.mAvrcpCtSm.obtainMessage(112, i));
    }

    private void handleSetBrowsedPlayerRsp(int i, int i2) {
        this.mAvrcpCtSm.sendMessage(this.mAvrcpCtSm.obtainMessage(113, i, i2));
    }

    private void handleSetAddressedPlayerRsp(int i) {
        this.mAvrcpCtSm.sendMessage(this.mAvrcpCtSm.obtainMessage(114));
    }

    @Override
    public void dump(StringBuilder sb) {
        super.dump(sb);
        this.mAvrcpCtSm.dump(sb);
    }

    public static String byteUIDToHexString(byte[] bArr) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bArr) {
            sb.append(String.format("%02X", Byte.valueOf(b)));
        }
        return sb.toString();
    }

    public static byte[] hexStringToByteUID(String str) {
        if (str == null) {
            Log.e(TAG, "Null hex string.");
            return EMPTY_UID;
        }
        if (str.length() % 2 == 1) {
            Log.e(TAG, "Odd length hex string " + str);
            return EMPTY_UID;
        }
        int length = str.length();
        byte[] bArr = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            bArr[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + Character.digit(str.charAt(i + 1), 16));
        }
        return bArr;
    }
}
