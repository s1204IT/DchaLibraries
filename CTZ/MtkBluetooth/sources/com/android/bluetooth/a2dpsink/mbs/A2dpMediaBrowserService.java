package com.android.bluetooth.a2dpsink.mbs;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.service.media.MediaBrowserService;
import android.util.Log;
import android.util.Pair;
import com.android.bluetooth.R;
import com.android.bluetooth.a2dpsink.A2dpSinkService;
import com.android.bluetooth.avrcpcontroller.AvrcpControllerService;
import com.android.bluetooth.avrcpcontroller.BrowseTree;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class A2dpMediaBrowserService extends MediaBrowserService {
    private static final String CUSTOM_ACTION_GET_PLAY_STATUS_NATIVE = "com.android.bluetooth.a2dpsink.mbs.CUSTOM_ACTION_GET_PLAY_STATUS_NATIVE";
    private static final String CUSTOM_ACTION_VOL_DN = "com.android.bluetooth.a2dpsink.mbs.CUSTOM_ACTION_VOL_DN";
    private static final String CUSTOM_ACTION_VOL_UP = "com.android.bluetooth.a2dpsink.mbs.CUSTOM_ACTION_VOL_UP";
    private static final boolean DBG = false;
    private static final int MSG_AVRCP_GET_PLAY_STATUS_NATIVE = 6;
    private static final int MSG_AVRCP_PASSTHRU = 5;
    private static final int MSG_DEVICE_BROWSE_CONNECT = 7;
    private static final int MSG_DEVICE_BROWSE_DISCONNECT = 8;
    private static final int MSG_DEVICE_CONNECT = 2;
    private static final int MSG_DEVICE_DISCONNECT = 0;
    private static final int MSG_FOLDER_LIST = 9;
    private static final int MSG_TRACK = 4;
    private static final float PLAYBACK_SPEED = 1.0f;
    private static final String TAG = "A2dpMediaBrowserService";
    private static final String UNKNOWN_BT_AUDIO = "__UNKNOWN_BT_AUDIO__";
    private static final boolean VDBG = false;
    private MediaMetadata mA2dpMetadata;
    private Handler mAvrcpCommandQueue;
    private AvrcpControllerService mAvrcpCtrlSrvc;
    private MediaSession mSession;
    private boolean mBrowseConnected = false;
    private BluetoothDevice mA2dpDevice = null;
    private A2dpSinkService mA2dpSinkService = null;
    private final Map<String, MediaBrowserService.Result<List<MediaBrowser.MediaItem>>> mParentIdToRequestMap = new HashMap();
    private List<MediaBrowser.MediaItem> mNowPlayingList = null;
    private long mTransportControlFlags = 54;
    private MediaSession.Callback mSessionCallbacks = new MediaSession.Callback() {
        @Override
        public void onPlay() {
            A2dpMediaBrowserService.this.mAvrcpCommandQueue.obtainMessage(5, 68).sendToTarget();
        }

        @Override
        public void onPause() {
            A2dpMediaBrowserService.this.mAvrcpCommandQueue.obtainMessage(5, 70).sendToTarget();
        }

        @Override
        public void onSkipToNext() {
            A2dpMediaBrowserService.this.mAvrcpCommandQueue.obtainMessage(5, 75).sendToTarget();
        }

        @Override
        public void onSkipToPrevious() {
            A2dpMediaBrowserService.this.mAvrcpCommandQueue.obtainMessage(5, 76).sendToTarget();
        }

        @Override
        public void onStop() {
            A2dpMediaBrowserService.this.mAvrcpCommandQueue.obtainMessage(5, 69).sendToTarget();
        }

        @Override
        public void onPrepare() {
            if (A2dpMediaBrowserService.this.mA2dpSinkService != null) {
                A2dpMediaBrowserService.this.mA2dpSinkService.requestAudioFocus(A2dpMediaBrowserService.this.mA2dpDevice, true);
            }
        }

        @Override
        public void onRewind() {
            A2dpMediaBrowserService.this.mAvrcpCommandQueue.obtainMessage(5, 72).sendToTarget();
        }

        @Override
        public void onFastForward() {
            A2dpMediaBrowserService.this.mAvrcpCommandQueue.obtainMessage(5, 73).sendToTarget();
        }

        @Override
        public void onPlayFromMediaId(String str, Bundle bundle) {
            synchronized (A2dpMediaBrowserService.this) {
                A2dpMediaBrowserService.this.mAvrcpCtrlSrvc.fetchAttrAndPlayItem(A2dpMediaBrowserService.this.mA2dpDevice, str);
                A2dpMediaBrowserService.this.mAvrcpCtrlSrvc.startAvrcpUpdates();
            }
        }

        @Override
        public void onCustomAction(String str, Bundle bundle) {
            if (A2dpMediaBrowserService.CUSTOM_ACTION_VOL_UP.equals(str)) {
                A2dpMediaBrowserService.this.mAvrcpCommandQueue.obtainMessage(5, 65).sendToTarget();
                return;
            }
            if (A2dpMediaBrowserService.CUSTOM_ACTION_VOL_DN.equals(str)) {
                A2dpMediaBrowserService.this.mAvrcpCommandQueue.obtainMessage(5, 66).sendToTarget();
                return;
            }
            if (A2dpMediaBrowserService.CUSTOM_ACTION_GET_PLAY_STATUS_NATIVE.equals(str)) {
                A2dpMediaBrowserService.this.mAvrcpCommandQueue.obtainMessage(6).sendToTarget();
                return;
            }
            Log.w(A2dpMediaBrowserService.TAG, "Custom action " + str + " not supported.");
        }
    };
    private BroadcastReceiver mBtReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            int intExtra = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
            if ("android.bluetooth.avrcp-controller.profile.action.CONNECTION_STATE_CHANGED".equals(action)) {
                if (intExtra == 2) {
                    A2dpMediaBrowserService.this.mAvrcpCommandQueue.obtainMessage(2, bluetoothDevice).sendToTarget();
                    return;
                } else {
                    if (intExtra == 0) {
                        A2dpMediaBrowserService.this.mAvrcpCommandQueue.obtainMessage(0, bluetoothDevice).sendToTarget();
                        if (A2dpMediaBrowserService.this.mSession.isActive()) {
                            A2dpMediaBrowserService.this.mSession.setActive(false);
                            return;
                        }
                        return;
                    }
                    return;
                }
            }
            if (AvrcpControllerService.ACTION_BROWSE_CONNECTION_STATE_CHANGED.equals(action)) {
                if (intExtra == 2) {
                    A2dpMediaBrowserService.this.mAvrcpCommandQueue.obtainMessage(7, bluetoothDevice).sendToTarget();
                    return;
                } else {
                    if (intExtra == 0) {
                        A2dpMediaBrowserService.this.mAvrcpCommandQueue.obtainMessage(8, bluetoothDevice).sendToTarget();
                        return;
                    }
                    return;
                }
            }
            if (AvrcpControllerService.ACTION_TRACK_EVENT.equals(action)) {
                A2dpMediaBrowserService.this.mAvrcpCommandQueue.obtainMessage(4, new Pair((PlaybackState) intent.getParcelableExtra(AvrcpControllerService.EXTRA_PLAYBACK), (MediaMetadata) intent.getParcelableExtra(AvrcpControllerService.EXTRA_METADATA))).sendToTarget();
            } else if (AvrcpControllerService.ACTION_FOLDER_LIST.equals(action)) {
                A2dpMediaBrowserService.this.mAvrcpCommandQueue.obtainMessage(9, intent).sendToTarget();
            }
        }
    };

    private static final class AvrcpCommandQueueHandler extends Handler {
        WeakReference<A2dpMediaBrowserService> mInst;

        AvrcpCommandQueueHandler(Looper looper, A2dpMediaBrowserService a2dpMediaBrowserService) {
            super(looper);
            this.mInst = new WeakReference<>(a2dpMediaBrowserService);
        }

        @Override
        public void handleMessage(Message message) {
            A2dpMediaBrowserService a2dpMediaBrowserService = this.mInst.get();
            if (a2dpMediaBrowserService == null) {
                Log.e(A2dpMediaBrowserService.TAG, "Parent class has died; aborting.");
            }
            int i = message.what;
            if (i == 0) {
                a2dpMediaBrowserService.msgDeviceDisconnect((BluetoothDevice) message.obj);
                return;
            }
            if (i == 2) {
                a2dpMediaBrowserService.msgDeviceConnect((BluetoothDevice) message.obj);
                return;
            }
            switch (i) {
                case 4:
                    Pair pair = (Pair) message.obj;
                    a2dpMediaBrowserService.msgTrack((PlaybackState) pair.first, (MediaMetadata) pair.second);
                    break;
                case 5:
                    a2dpMediaBrowserService.msgPassThru(((Integer) message.obj).intValue());
                    break;
                case 6:
                    a2dpMediaBrowserService.msgGetPlayStatusNative();
                    break;
                case 7:
                    a2dpMediaBrowserService.msgDeviceBrowseConnect((BluetoothDevice) message.obj);
                    break;
                case 8:
                    a2dpMediaBrowserService.msgDeviceBrowseDisconnect((BluetoothDevice) message.obj);
                    break;
                case 9:
                    a2dpMediaBrowserService.msgFolderList((Intent) message.obj);
                    break;
                default:
                    Log.e(A2dpMediaBrowserService.TAG, "Message not handled " + message);
                    break;
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mSession = new MediaSession(this, TAG);
        setSessionToken(this.mSession.getSessionToken());
        this.mSession.setCallback(this.mSessionCallbacks);
        this.mSession.setFlags(3);
        this.mSession.setActive(true);
        this.mAvrcpCommandQueue = new AvrcpCommandQueueHandler(Looper.getMainLooper(), this);
        refreshInitialPlayingState();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.avrcp-controller.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction(AvrcpControllerService.ACTION_BROWSE_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(AvrcpControllerService.ACTION_TRACK_EVENT);
        intentFilter.addAction(AvrcpControllerService.ACTION_FOLDER_LIST);
        registerReceiver(this.mBtReceiver, intentFilter);
        synchronized (this) {
            this.mParentIdToRequestMap.clear();
        }
    }

    @Override
    public void onDestroy() {
        this.mSession.release();
        unregisterReceiver(this.mBtReceiver);
        super.onDestroy();
    }

    @Override
    public MediaBrowserService.BrowserRoot onGetRoot(String str, int i, Bundle bundle) {
        return new MediaBrowserService.BrowserRoot(BrowseTree.ROOT, null);
    }

    @Override
    public synchronized void onLoadChildren(String str, MediaBrowserService.Result<List<MediaBrowser.MediaItem>> result) {
        if (this.mAvrcpCtrlSrvc == null) {
            Log.w(TAG, "AVRCP not yet connected.");
            result.sendResult(Collections.emptyList());
        } else {
            if (!this.mAvrcpCtrlSrvc.getChildren(this.mA2dpDevice, str, 0, 255)) {
                result.sendResult(Collections.emptyList());
                return;
            }
            synchronized (this) {
                this.mParentIdToRequestMap.put(str, result);
                result.detach();
            }
        }
    }

    @Override
    public void onLoadItem(String str, MediaBrowserService.Result<MediaBrowser.MediaItem> result) {
    }

    private synchronized void msgDeviceConnect(BluetoothDevice bluetoothDevice) {
        this.mA2dpDevice = bluetoothDevice;
        this.mAvrcpCtrlSrvc = AvrcpControllerService.getAvrcpControllerService();
        if (this.mAvrcpCtrlSrvc == null) {
            Log.e(TAG, "!!!AVRCP Controller cannot be null");
        } else {
            refreshInitialPlayingState();
        }
    }

    private synchronized void refreshInitialPlayingState() {
        if (this.mA2dpDevice == null) {
            return;
        }
        List<BluetoothDevice> connectedDevices = this.mAvrcpCtrlSrvc.getConnectedDevices();
        if (connectedDevices.size() == 0) {
            Log.w(TAG, "No devices connected yet");
            return;
        }
        if (this.mA2dpDevice != null && !this.mA2dpDevice.equals(connectedDevices.get(0))) {
            Log.w(TAG, "A2dp device : " + this.mA2dpDevice + " avrcp device " + connectedDevices.get(0));
            return;
        }
        this.mA2dpDevice = connectedDevices.get(0);
        this.mA2dpSinkService = A2dpSinkService.getA2dpSinkService();
        PlaybackState playbackStateBuild = new PlaybackState.Builder(this.mAvrcpCtrlSrvc.getPlaybackState(this.mA2dpDevice)).setActions(this.mTransportControlFlags).build();
        this.mAvrcpCtrlSrvc.getMetaData(this.mA2dpDevice);
        this.mSession.setMetadata(this.mAvrcpCtrlSrvc.getMetaData(this.mA2dpDevice));
        this.mSession.setPlaybackState(playbackStateBuild);
    }

    private void msgDeviceDisconnect(BluetoothDevice bluetoothDevice) {
        if (this.mA2dpDevice == null) {
            Log.w(TAG, "Already disconnected - nothing to do here.");
            return;
        }
        if (!this.mA2dpDevice.equals(bluetoothDevice)) {
            Log.e(TAG, "Not the right device to disconnect current " + this.mA2dpDevice + " dc " + bluetoothDevice);
            return;
        }
        this.mSession.setPlaybackState(new PlaybackState.Builder().setState(7, -1L, PLAYBACK_SPEED).setActions(this.mTransportControlFlags).setErrorMessage(getString(R.string.bluetooth_disconnected)).build());
        this.mA2dpDevice = null;
        this.mBrowseConnected = false;
        notifyChildrenChanged(BrowseTree.ROOT);
    }

    private void msgTrack(PlaybackState playbackState, MediaMetadata mediaMetadata) {
        MediaController controller = this.mSession.getController();
        PlaybackState playbackState2 = controller.getPlaybackState();
        MediaMetadata metadata = controller.getMetadata();
        if (playbackState2 != null) {
            Log.d(TAG, "prevPS " + playbackState2);
        }
        if (metadata != null) {
            metadata.getString("android.media.metadata.TITLE");
            metadata.getLong("android.media.metadata.DURATION");
        }
        if (mediaMetadata != null) {
            this.mSession.setMetadata(mediaMetadata);
        }
        if (playbackState != null) {
            PlaybackState playbackStateBuild = new PlaybackState.Builder(playbackState).setActions(this.mTransportControlFlags).build();
            this.mSession.setPlaybackState(playbackStateBuild);
            if (playbackStateBuild.getState() == 3 && !this.mSession.isActive()) {
                this.mSession.setActive(true);
            }
        }
    }

    private synchronized void msgPassThru(int i) {
        if (this.mA2dpDevice == null) {
            Log.w(TAG, "Already disconnected ignoring.");
        } else {
            this.mAvrcpCtrlSrvc.sendPassThroughCmd(this.mA2dpDevice, i, 0);
            this.mAvrcpCtrlSrvc.sendPassThroughCmd(this.mA2dpDevice, i, 1);
        }
    }

    private synchronized void msgGetPlayStatusNative() {
        if (this.mA2dpDevice == null) {
            Log.w(TAG, "Already disconnected ignoring.");
        } else {
            this.mAvrcpCtrlSrvc.getPlaybackState(this.mA2dpDevice, false);
        }
    }

    private void msgDeviceBrowseConnect(BluetoothDevice bluetoothDevice) {
        if (!bluetoothDevice.equals(this.mA2dpDevice)) {
            Log.e(TAG, "Browse connected over different device a2dp " + this.mA2dpDevice + " browse " + bluetoothDevice);
            return;
        }
        this.mBrowseConnected = true;
        notifyChildrenChanged(BrowseTree.ROOT);
    }

    private void msgFolderList(Intent intent) {
        ArrayList parcelableArrayListExtra = intent.getParcelableArrayListExtra(AvrcpControllerService.EXTRA_FOLDER_LIST);
        ArrayList arrayList = new ArrayList();
        Iterator it = parcelableArrayListExtra.iterator();
        while (it.hasNext()) {
            arrayList.add((MediaBrowser.MediaItem) ((Parcelable) it.next()));
        }
        String stringExtra = intent.getStringExtra(AvrcpControllerService.EXTRA_FOLDER_ID);
        synchronized (this) {
            MediaBrowserService.Result<List<MediaBrowser.MediaItem>> resultRemove = this.mParentIdToRequestMap.remove(stringExtra);
            if (resultRemove == null) {
                Log.w(TAG, "Request no longer exists, notifying that children changed.");
                notifyChildrenChanged(stringExtra);
            } else {
                resultRemove.sendResult(arrayList);
            }
        }
    }

    private void msgDeviceBrowseDisconnect(BluetoothDevice bluetoothDevice) {
        if (!bluetoothDevice.equals(this.mA2dpDevice)) {
            Log.w(TAG, "Browse disconnecting from different device a2dp " + this.mA2dpDevice + " browse " + bluetoothDevice);
            return;
        }
        this.mBrowseConnected = false;
    }
}
