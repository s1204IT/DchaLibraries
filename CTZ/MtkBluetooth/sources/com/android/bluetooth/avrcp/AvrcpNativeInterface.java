package com.android.bluetooth.avrcp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.SystemProperties;
import android.util.Log;
import com.android.bluetooth.avrcp.MediaPlayerList;
import com.android.bluetooth.avrcp.MediaPlayerSettings;
import java.util.List;

public class AvrcpNativeInterface {
    private static final boolean DEBUG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    private static final String TAG = "NewAvrcpNativeInterface";
    private static AvrcpNativeInterface sInstance;
    private AvrcpTargetService mAvrcpService;

    private static native void classInitNative();

    private native void cleanupNative();

    private native boolean connectDeviceNative(String str);

    private native boolean disconnectDeviceNative(String str);

    private native void getAppSettingChangeRspNative(byte b, byte[] bArr);

    private native void getFolderItemsResponseNative(String str, List<ListItem> list);

    private native void getListPlayerAttributeRspNative(byte b, byte[] bArr);

    private native void getListPlayerAttributeValuesRspNative(byte b, byte[] bArr);

    private native void getPlayerAttributeTextRspNative(int i, byte[] bArr, int i2, String[] strArr);

    private native void getPlayerAttributeTextValueRspNative(int i, byte[] bArr, int i2, String[] strArr);

    private native void getPlayerAttributeValueRspNative(byte b, byte[] bArr);

    private native void initNative();

    private native void sendFolderUpdateNative(boolean z, boolean z2, boolean z3);

    private native void sendMediaUpdateNative(boolean z, boolean z2, boolean z3);

    private native void sendPlayerAppSettingsUpdateNative(boolean z);

    private native void sendVolumeChangedNative(int i);

    private native void setBrowsedPlayerResponseNative(int i, boolean z, String str, int i2);

    private native void setPlayerAppSettingRspNative(int i);

    static {
        classInitNative();
    }

    static AvrcpNativeInterface getInterface() {
        if (sInstance == null) {
            sInstance = new AvrcpNativeInterface();
        }
        return sInstance;
    }

    void init(AvrcpTargetService avrcpTargetService) {
        d("Init AvrcpNativeInterface");
        this.mAvrcpService = avrcpTargetService;
        initNative();
    }

    void cleanup() {
        d("Cleanup AvrcpNativeInterface");
        this.mAvrcpService = null;
        cleanupNative();
    }

    Metadata getCurrentSongInfo() {
        d("getCurrentSongInfo");
        if (this.mAvrcpService == null) {
            Log.w(TAG, "getCurrentSongInfo(): AvrcpTargetService is null");
            return null;
        }
        return this.mAvrcpService.getCurrentSongInfo();
    }

    PlayStatus getPlayStatus() {
        d("getPlayStatus");
        if (this.mAvrcpService == null) {
            Log.w(TAG, "getPlayStatus(): AvrcpTargetService is null");
            return null;
        }
        PlayStatus playState = this.mAvrcpService.getPlayState();
        playState.state = (byte) -1;
        return playState;
    }

    void sendMediaKeyEvent(int i, boolean z) {
        d("sendMediaKeyEvent: keyEvent=" + i + " pushed=" + z);
        if (this.mAvrcpService == null) {
            Log.w(TAG, "sendMediaKeyEvent(): AvrcpTargetService is null");
        } else {
            this.mAvrcpService.sendMediaKeyEvent(i, z);
        }
    }

    String getCurrentMediaId() {
        d("getCurrentMediaId");
        if (this.mAvrcpService == null) {
            Log.w(TAG, "getMediaPlayerList(): AvrcpTargetService is null");
            return "";
        }
        return this.mAvrcpService.getCurrentMediaId();
    }

    List<Metadata> getNowPlayingList() {
        d("getNowPlayingList");
        if (this.mAvrcpService == null) {
            Log.w(TAG, "getMediaPlayerList(): AvrcpTargetService is null");
            return null;
        }
        return this.mAvrcpService.getNowPlayingList();
    }

    int getCurrentPlayerId() {
        d("getCurrentPlayerId");
        if (this.mAvrcpService == null) {
            Log.w(TAG, "getMediaPlayerList(): AvrcpTargetService is null");
            return -1;
        }
        return this.mAvrcpService.getCurrentPlayerId();
    }

    List<PlayerInfo> getMediaPlayerList() {
        d("getMediaPlayerList");
        if (this.mAvrcpService == null) {
            Log.w(TAG, "getMediaPlayerList(): AvrcpTargetService is null");
            return null;
        }
        return this.mAvrcpService.getMediaPlayerList();
    }

    void setBrowsedPlayer(int i) {
        d("setBrowsedPlayer: playerId=" + i);
        this.mAvrcpService.getPlayerRoot(i, new MediaPlayerList.GetPlayerRootCallback() {
            @Override
            public final void run(int i2, boolean z, String str, int i3) {
                this.f$0.setBrowsedPlayerResponse(i2, z, str, i3);
            }
        });
    }

    void setBrowsedPlayerResponse(int i, boolean z, String str, int i2) {
        d("setBrowsedPlayerResponse: playerId=" + i + " success=" + z + " rootId=" + str + " numItems=" + i2);
        setBrowsedPlayerResponseNative(i, z, str, i2);
    }

    void getFolderItemsRequest(int i, String str) {
        d("getFolderItemsRequest: playerId=" + i + " mediaId=" + str);
        this.mAvrcpService.getFolderItems(i, str, new MediaPlayerList.GetFolderItemsCallback() {
            @Override
            public final void run(String str2, List list) {
                this.f$0.getFolderItemsResponse(str2, list);
            }
        });
    }

    void getFolderItemsResponse(String str, List<ListItem> list) {
        d("getFolderItemsResponse: parentId=" + str + " items.size=" + list.size());
        getFolderItemsResponseNative(str, list);
    }

    void sendMediaUpdate(boolean z, boolean z2, boolean z3) {
        d("sendMediaUpdate: metadata=" + z + " playStatus=" + z2 + " queue=" + z3);
        sendMediaUpdateNative(z, z2, z3);
    }

    void sendFolderUpdate(boolean z, boolean z2, boolean z3) {
        d("sendFolderUpdate: availablePlayers=" + z + " addressedPlayers=" + z2 + " uids=" + z3);
        sendFolderUpdateNative(z, z2, z3);
    }

    void playItem(int i, boolean z, String str) {
        d("playItem: playerId=" + i + " nowPlaying=" + z + " mediaId" + str);
        if (this.mAvrcpService == null) {
            Log.d(TAG, "playItem: AvrcpTargetService is null");
        } else {
            this.mAvrcpService.playItem(i, z, str);
        }
    }

    boolean connectDevice(String str) {
        d("connectDevice: bdaddr=" + str);
        return connectDeviceNative(str);
    }

    boolean disconnectDevice(String str) {
        d("disconnectDevice: bdaddr=" + str);
        return disconnectDeviceNative(str);
    }

    void setActiveDevice(String str) {
        BluetoothDevice remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(str.toUpperCase());
        d("setActiveDevice: device=" + remoteDevice);
        this.mAvrcpService.setActiveDevice(remoteDevice);
    }

    void deviceConnected(String str, boolean z) {
        BluetoothDevice remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(str.toUpperCase());
        d("deviceConnected: device=" + remoteDevice + " absoluteVolume=" + z);
        if (this.mAvrcpService == null) {
            Log.w(TAG, "deviceConnected: AvrcpTargetService is null");
        } else {
            this.mAvrcpService.deviceConnected(remoteDevice, z);
        }
    }

    void deviceDisconnected(String str) {
        BluetoothDevice remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(str.toUpperCase());
        d("deviceDisconnected: device=" + remoteDevice);
        if (this.mAvrcpService == null) {
            Log.w(TAG, "deviceDisconnected: AvrcpTargetService is null");
        } else {
            this.mAvrcpService.deviceDisconnected(remoteDevice);
        }
    }

    void sendVolumeChanged(int i) {
        d("sendVolumeChanged: volume=" + i);
        sendVolumeChangedNative(i);
    }

    void setVolume(int i) {
        d("setVolume: volume=" + i);
        if (this.mAvrcpService == null) {
            Log.w(TAG, "setVolume: AvrcpTargetService is null");
        } else {
            this.mAvrcpService.setVolume(i);
        }
    }

    void getListPlayerAttributeRequest() {
        d("getListPlayerAttributeRequest");
        this.mAvrcpService.getListPlayerAttribute(new MediaPlayerSettings.GetListPlayerAttributeCallback() {
            @Override
            public final void run(byte b, byte[] bArr) {
                this.f$0.getListPlayerAttributeResponse(b, bArr);
            }
        });
    }

    void getListPlayerAttributeResponse(byte b, byte[] bArr) {
        d("getListPlayerAttributeResponse attr");
        getListPlayerAttributeRspNative(b, bArr);
    }

    void getListPlayerAttributeValuesRequest(byte b) {
        d("getListPlayerAttributeValuesRequest attr = " + ((int) b));
        this.mAvrcpService.getListPlayerAttributeValues(b, new MediaPlayerSettings.GetListPlayerAttributeValuesCallback() {
            @Override
            public final void run(byte b2, byte[] bArr) {
                this.f$0.getListPlayerAttributeValuesResponse(b2, bArr);
            }
        });
    }

    void getListPlayerAttributeValuesResponse(byte b, byte[] bArr) {
        d("getListPlayerAttributeValuesResponse");
        getListPlayerAttributeValuesRspNative(b, bArr);
    }

    void getPlayerAttributeValueRequest(byte b, int[] iArr) {
        d("getPlayerAttributeValueRequest");
        this.mAvrcpService.getPlayerAttributeValue(b, iArr, new MediaPlayerSettings.GetPlayerAttributeValueCallback() {
            @Override
            public final void run(byte b2, byte[] bArr) {
                this.f$0.getPlayerAttributeValueResponse(b2, bArr);
            }
        });
    }

    void getPlayerAttributeValueResponse(byte b, byte[] bArr) {
        d("getPlayerAttributeValueResponse");
        getPlayerAttributeValueRspNative(b, bArr);
    }

    void setPlayerAppSettingRequest(byte b, byte[] bArr, byte[] bArr2) {
        d("setPlayerAppSettingRequest");
        this.mAvrcpService.setPlayerAppSetting(b, bArr, bArr2, new MediaPlayerSettings.SetPlayerAppSettingCallback() {
            @Override
            public final void run(int i) {
                this.f$0.setPlayerAppSettingResponse(i);
            }
        });
    }

    void setPlayerAppSettingResponse(int i) {
        d("setPlayerAppSettingResponse attr_status = " + i);
        setPlayerAppSettingRspNative(i);
    }

    void getPlayerAttributeTextRequest(byte b, byte[] bArr) {
        d("getPlayerAttributeTextRequest");
        this.mAvrcpService.getPlayerAttributeText(b, bArr, new MediaPlayerSettings.GetPlayerAttributeTextCallback() {
            @Override
            public final void run(int i, byte[] bArr2, int i2, String[] strArr) {
                this.f$0.getPlayerAttributeTextResponse(i, bArr2, i2, strArr);
            }
        });
    }

    void getPlayerAttributeTextResponse(int i, byte[] bArr, int i2, String[] strArr) {
        d("getPlayerAttributeTextResponse");
        getPlayerAttributeTextRspNative(i, bArr, i2, strArr);
    }

    void getPlayerAttributeValueTextRequest(byte b, byte b2, byte[] bArr) {
        d("getPlayerAttributeValueTextRequest");
        this.mAvrcpService.getPlayerAttributeTextValue(b, b2, bArr, new MediaPlayerSettings.GetPlayerAttributeValueTextCallback() {
            @Override
            public final void run(int i, byte[] bArr2, int i2, String[] strArr) {
                this.f$0.getPlayerAttributeTextValueResponse(i, bArr2, i2, strArr);
            }
        });
    }

    void getPlayerAttributeTextValueResponse(int i, byte[] bArr, int i2, String[] strArr) {
        d("getPlayerAttributeTextValueResponse");
        getPlayerAttributeTextValueRspNative(i, bArr, i2, strArr);
    }

    void sendPlayerAppSettingsUpdate(boolean z) {
        d("sendPlayerAppSettingsUpdate playerSettings= " + z);
        sendPlayerAppSettingsUpdateNative(z);
    }

    void getAppSettingChangeRequest() {
        d("getAppSettingChangeRequest");
        this.mAvrcpService.getAppSettingChange(new MediaPlayerSettings.GetAppSettingChangeCallback() {
            @Override
            public final void run(byte b, byte[] bArr) {
                this.f$0.getAppSettingChangeResponse(b, bArr);
            }
        });
    }

    void getAppSettingChangeResponse(byte b, byte[] bArr) {
        d("getAppSettingChangeResponse");
        getAppSettingChangeRspNative(b, bArr);
    }

    private static void d(String str) {
        if (DEBUG) {
            Log.d(TAG, str);
        }
    }
}
