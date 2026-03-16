package com.android.bluetooth.avrcp;

import android.content.Context;
import android.media.AudioManager;
import android.media.RemoteController;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.AbstractionLayer;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.hfp.BluetoothCmeError;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public final class Avrcp {
    private static final int AVRCP_BASE_VOLUME_STEP = 1;
    private static final int AVRCP_MAX_VOL = 127;
    private static final int AVRC_RSP_ACCEPT = 9;
    private static final int AVRC_RSP_CHANGED = 13;
    private static final int AVRC_RSP_IMPL_STBL = 12;
    private static final int AVRC_RSP_INTERIM = 15;
    private static final int AVRC_RSP_IN_TRANS = 11;
    private static final int AVRC_RSP_NOT_IMPL = 8;
    private static final int AVRC_RSP_REJ = 10;
    private static final int BASE_SKIP_AMOUNT = 2000;
    public static final int BTRC_FEAT_ABSOLUTE_VOLUME = 2;
    public static final int BTRC_FEAT_BROWSE = 4;
    public static final int BTRC_FEAT_METADATA = 1;
    private static final int BUTTON_TIMEOUT_TIME = 2000;
    private static final int CMD_TIMEOUT_DELAY = 2000;
    private static final boolean DEBUG = false;
    static final int EVT_APP_SETTINGS_CHANGED = 8;
    static final int EVT_BATT_STATUS_CHANGED = 6;
    static final int EVT_PLAY_POS_CHANGED = 5;
    static final int EVT_PLAY_STATUS_CHANGED = 1;
    static final int EVT_SYSTEM_STATUS_CHANGED = 7;
    static final int EVT_TRACK_CHANGED = 2;
    static final int EVT_TRACK_REACHED_END = 3;
    static final int EVT_TRACK_REACHED_START = 4;
    private static final int KEY_STATE_PRESS = 1;
    private static final int KEY_STATE_RELEASE = 0;
    private static final int MAX_ERROR_RETRY_TIMES = 3;
    private static final long MAX_MULTIPLIER_VALUE = 128;
    static final int MEDIA_ATTR_ALBUM = 3;
    static final int MEDIA_ATTR_ARTIST = 2;
    static final int MEDIA_ATTR_GENRE = 6;
    static final int MEDIA_ATTR_NUM_TRACKS = 5;
    static final int MEDIA_ATTR_PLAYING_TIME = 7;
    static final int MEDIA_ATTR_TITLE = 1;
    static final int MEDIA_ATTR_TRACK_NUM = 4;
    private static final int MESSAGE_ABS_VOL_TIMEOUT = 9;
    private static final int MESSAGE_ADJUST_VOLUME = 7;
    private static final int MESSAGE_CHANGE_PLAY_POS = 12;
    private static final int MESSAGE_FAST_FORWARD = 10;
    private static final int MESSAGE_GET_ELEM_ATTRS = 3;
    private static final int MESSAGE_GET_PLAY_STATUS = 2;
    private static final int MESSAGE_GET_RC_FEATURES = 1;
    private static final int MESSAGE_PLAY_INTERVAL_TIMEOUT = 5;
    private static final int MESSAGE_REGISTER_NOTIFICATION = 4;
    private static final int MESSAGE_REWIND = 11;
    private static final int MESSAGE_SET_A2DP_AUDIO_STATE = 13;
    private static final int MESSAGE_SET_ABSOLUTE_VOLUME = 8;
    private static final int MESSAGE_VOLUME_CHANGED = 6;
    private static final int MSG_SET_GENERATION_ID = 104;
    private static final int MSG_SET_METADATA = 101;
    private static final int MSG_SET_TRANSPORT_CONTROLS = 102;
    private static final int MSG_UPDATE_STATE = 100;
    static final int NOTIFICATION_TYPE_CHANGED = 1;
    static final int NOTIFICATION_TYPE_INTERIM = 0;
    static final int PLAYSTATUS_ERROR = 255;
    static final int PLAYSTATUS_FWD_SEEK = 3;
    static final int PLAYSTATUS_PAUSED = 2;
    static final int PLAYSTATUS_PLAYING = 1;
    static final int PLAYSTATUS_REV_SEEK = 4;
    static final int PLAYSTATUS_STOPPED = 0;
    private static final int SKIP_DOUBLE_INTERVAL = 3000;
    private static final int SKIP_PERIOD = 400;
    private static final String TAG = "Avrcp";
    static final int TRACK_ID_SIZE = 8;
    private final AudioManager mAudioManager;
    private final int mAudioStreamMax;
    private Context mContext;
    private AvrcpMessageHandler mHandler;
    private long mNextPosMs;
    private long mPrevPosMs;
    private RemoteController mRemoteController;
    private RemoteControllerWeak mRemoteControllerCb;
    private int mSkipAmount;
    private long mSkipStartTime;
    private int mTransportControlFlags;
    private final int mVolumeStep;
    private Metadata mMetadata = new Metadata();
    private int mCurrentPlayState = 0;
    private int mPlayStatusChangedNT = 1;
    private int mTrackChangedNT = 1;
    private long mTrackNumber = -1;
    private long mCurrentPosMs = 0;
    private long mPlayStartTimeMs = -1;
    private long mSongLengthMs = 0;
    private long mPlaybackIntervalMs = 0;
    private int mPlayPosChangedNT = 1;
    private int mFeatures = 0;
    private int mAbsoluteVolume = -1;
    private int mLastSetVolume = -1;
    private int mLastDirection = 0;
    private boolean mVolCmdInProgress = DEBUG;
    private int mAbsVolRetryTimes = 0;

    private static native void classInitNative();

    private native void cleanupNative();

    private native boolean getElementAttrRspNative(byte b, int[] iArr, String[] strArr);

    private native boolean getPlayStatusRspNative(int i, int i2, int i3);

    private native void initNative();

    private native boolean registerNotificationRspPlayPosNative(int i, int i2);

    private native boolean registerNotificationRspPlayStatusNative(int i, int i2);

    private native boolean registerNotificationRspTrackChangeNative(int i, byte[] bArr);

    private native boolean sendPassThroughCommandNative(int i, int i2);

    private native boolean setVolumeNative(int i);

    static int access$1712(Avrcp x0, int x1) {
        int i = x0.mAbsVolRetryTimes + x1;
        x0.mAbsVolRetryTimes = i;
        return i;
    }

    static {
        classInitNative();
    }

    private Avrcp(Context context) {
        this.mContext = context;
        initNative();
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mAudioStreamMax = this.mAudioManager.getStreamMaxVolume(3);
        this.mVolumeStep = Math.max(1, AVRCP_MAX_VOL / this.mAudioStreamMax);
    }

    private void start() {
        HandlerThread thread = new HandlerThread("BluetoothAvrcpHandler");
        thread.start();
        Looper looper = thread.getLooper();
        this.mHandler = new AvrcpMessageHandler(looper);
        this.mRemoteControllerCb = new RemoteControllerWeak(this.mHandler);
        this.mRemoteController = new RemoteController(this.mContext, this.mRemoteControllerCb);
        this.mAudioManager.registerRemoteController(this.mRemoteController);
        this.mRemoteController.setSynchronizationMode(1);
    }

    public static Avrcp make(Context context) {
        Avrcp ar = new Avrcp(context);
        ar.start();
        return ar;
    }

    public void doQuit() {
        this.mHandler.removeCallbacksAndMessages(null);
        Looper looper = this.mHandler.getLooper();
        if (looper != null) {
            looper.quit();
        }
        this.mAudioManager.unregisterRemoteController(this.mRemoteController);
    }

    public void cleanup() {
        cleanupNative();
    }

    private static class RemoteControllerWeak implements RemoteController.OnClientUpdateListener {
        private final WeakReference<Handler> mLocalHandler;

        public RemoteControllerWeak(Handler handler) {
            this.mLocalHandler = new WeakReference<>(handler);
        }

        @Override
        public void onClientChange(boolean clearing) {
            Handler handler = this.mLocalHandler.get();
            if (handler != null) {
                handler.obtainMessage(Avrcp.MSG_SET_GENERATION_ID, 0, clearing ? 1 : 0, null).sendToTarget();
            }
        }

        @Override
        public void onClientPlaybackStateUpdate(int state) {
            Handler handler = this.mLocalHandler.get();
            if (handler != null) {
                handler.obtainMessage(100, 0, state, new Long(-1L)).sendToTarget();
            }
        }

        @Override
        public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs, float speed) {
            Handler handler = this.mLocalHandler.get();
            if (handler != null) {
                handler.obtainMessage(100, 0, state, new Long(currentPosMs)).sendToTarget();
            }
        }

        @Override
        public void onClientTransportControlUpdate(int transportControlFlags) {
            Handler handler = this.mLocalHandler.get();
            if (handler != null) {
                handler.obtainMessage(Avrcp.MSG_SET_TRANSPORT_CONTROLS, 0, transportControlFlags).sendToTarget();
            }
        }

        @Override
        public void onClientMetadataUpdate(RemoteController.MetadataEditor metadataEditor) {
            Handler handler = this.mLocalHandler.get();
            if (handler != null) {
                handler.obtainMessage(Avrcp.MSG_SET_METADATA, 0, 0, metadataEditor).sendToTarget();
            }
        }
    }

    private final class AvrcpMessageHandler extends Handler {
        private AvrcpMessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            int skipAmount;
            switch (msg.what) {
                case 1:
                    String address = (String) msg.obj;
                    Avrcp.this.mFeatures = msg.arg1;
                    Avrcp.this.mAudioManager.avrcpSupportsAbsoluteVolume(address, Avrcp.this.isAbsoluteVolumeSupported());
                    break;
                case 2:
                    Avrcp.this.getPlayStatusRspNative(Avrcp.this.convertPlayStateToPlayStatus(Avrcp.this.mCurrentPlayState), (int) Avrcp.this.mSongLengthMs, (int) Avrcp.this.getPlayPosition());
                    break;
                case 3:
                    int i = (byte) msg.arg1;
                    ArrayList<Integer> attrList = (ArrayList) msg.obj;
                    int[] attrIds = new int[i];
                    String[] textArray = new String[i];
                    for (int i2 = 0; i2 < i; i2++) {
                        attrIds[i2] = attrList.get(i2).intValue();
                        textArray[i2] = Avrcp.this.getAttributeString(attrIds[i2]);
                    }
                    Avrcp.this.getElementAttrRspNative(i, attrIds, textArray);
                    break;
                case 4:
                    Avrcp.this.processRegisterNotification(msg.arg1, msg.arg2);
                    break;
                case 5:
                    Avrcp.this.mPlayPosChangedNT = 1;
                    Avrcp.this.registerNotificationRspPlayPosNative(Avrcp.this.mPlayPosChangedNT, (int) Avrcp.this.getPlayPosition());
                    break;
                case 6:
                    if (msg.arg2 == 9 || msg.arg2 == 10) {
                        if (!Avrcp.this.mVolCmdInProgress) {
                            Log.e(Avrcp.TAG, "Unsolicited response, ignored");
                        } else {
                            removeMessages(9);
                            Avrcp.this.mVolCmdInProgress = Avrcp.DEBUG;
                            Avrcp.this.mAbsVolRetryTimes = 0;
                        }
                    }
                    if (Avrcp.this.mAbsoluteVolume != msg.arg1 && (msg.arg2 == 9 || msg.arg2 == 13 || msg.arg2 == 15)) {
                        byte absVol = (byte) (((byte) msg.arg1) & 127);
                        Avrcp.this.notifyVolumeChanged(absVol);
                        Avrcp.this.mAbsoluteVolume = absVol;
                        long pecentVolChanged = (((long) absVol) * 100) / 127;
                        Log.e(Avrcp.TAG, "percent volume changed: " + pecentVolChanged + "%");
                    } else if (msg.arg2 == 10) {
                        Log.e(Avrcp.TAG, "setAbsoluteVolume call rejected");
                    }
                    break;
                case AbstractionLayer.BT_STATUS_PARM_INVALID:
                    if (!Avrcp.this.mVolCmdInProgress) {
                        if (Avrcp.this.mAbsoluteVolume != -1 && (msg.arg1 == -1 || msg.arg1 == 1)) {
                            int setVol = Math.min(Avrcp.AVRCP_MAX_VOL, Math.max(0, Avrcp.this.mAbsoluteVolume + (msg.arg1 * Avrcp.this.mVolumeStep)));
                            if (Avrcp.this.setVolumeNative(setVol)) {
                                sendMessageDelayed(obtainMessage(9), 2000L);
                                Avrcp.this.mVolCmdInProgress = true;
                                Avrcp.this.mLastDirection = msg.arg1;
                                Avrcp.this.mLastSetVolume = setVol;
                            }
                        } else {
                            Log.e(Avrcp.TAG, "Unknown direction in MESSAGE_ADJUST_VOLUME");
                        }
                    }
                    break;
                case 8:
                    if (!Avrcp.this.mVolCmdInProgress && Avrcp.this.setVolumeNative(msg.arg1)) {
                        sendMessageDelayed(obtainMessage(9), 2000L);
                        Avrcp.this.mVolCmdInProgress = true;
                        Avrcp.this.mLastSetVolume = msg.arg1;
                        break;
                    }
                    break;
                case AbstractionLayer.BT_STATUS_AUTH_FAILURE:
                    Avrcp.this.mVolCmdInProgress = Avrcp.DEBUG;
                    if (Avrcp.this.mAbsVolRetryTimes >= 3) {
                        Avrcp.this.mAbsVolRetryTimes = 0;
                    } else {
                        Avrcp.access$1712(Avrcp.this, 1);
                        if (Avrcp.this.setVolumeNative(Avrcp.this.mLastSetVolume)) {
                            sendMessageDelayed(obtainMessage(9), 2000L);
                            Avrcp.this.mVolCmdInProgress = true;
                        }
                    }
                    break;
                case 10:
                case 11:
                    if (msg.what == 10) {
                        if ((Avrcp.this.mTransportControlFlags & 64) != 0) {
                            int keyState = msg.arg1 == 1 ? 0 : 1;
                            KeyEvent keyEvent = new KeyEvent(keyState, 90);
                            Avrcp.this.mRemoteController.sendMediaKeyEvent(keyEvent);
                        }
                    } else if ((Avrcp.this.mTransportControlFlags & 2) != 0) {
                        int keyState2 = msg.arg1 == 1 ? 0 : 1;
                        KeyEvent keyEvent2 = new KeyEvent(keyState2, 89);
                        Avrcp.this.mRemoteController.sendMediaKeyEvent(keyEvent2);
                    }
                    if (msg.what == 10) {
                        removeMessages(10);
                        skipAmount = 2000;
                    } else {
                        removeMessages(11);
                        skipAmount = -2000;
                    }
                    if (hasMessages(12) && skipAmount != Avrcp.this.mSkipAmount) {
                        Log.w(Avrcp.TAG, "missing release button event:" + Avrcp.this.mSkipAmount);
                    }
                    if (!hasMessages(12) || skipAmount != Avrcp.this.mSkipAmount) {
                        Avrcp.this.mSkipStartTime = SystemClock.elapsedRealtime();
                    }
                    removeMessages(12);
                    if (msg.arg1 == 1) {
                        Avrcp.this.mSkipAmount = skipAmount;
                        Avrcp.this.changePositionBy(Avrcp.this.mSkipAmount * Avrcp.this.getSkipMultiplier());
                        Message posMsg = obtainMessage(12);
                        posMsg.arg1 = 1;
                        sendMessageDelayed(posMsg, 400L);
                    }
                    break;
                case 12:
                    Avrcp.this.changePositionBy(Avrcp.this.mSkipAmount * Avrcp.this.getSkipMultiplier());
                    if (msg.arg1 * 400 < 2000) {
                        Message posMsg2 = obtainMessage(12);
                        posMsg2.arg1 = msg.arg1 + 1;
                        sendMessageDelayed(posMsg2, 400L);
                    }
                    break;
                case BluetoothCmeError.SIM_FAILURE:
                    Avrcp.this.updateA2dpAudioState(msg.arg1);
                    break;
                case 100:
                    Avrcp.this.updatePlayPauseState(msg.arg2, ((Long) msg.obj).longValue());
                    break;
                case Avrcp.MSG_SET_METADATA:
                    Avrcp.this.updateMetadata((RemoteController.MetadataEditor) msg.obj);
                    break;
                case Avrcp.MSG_SET_TRANSPORT_CONTROLS:
                    Avrcp.this.updateTransportControls(msg.arg2);
                    break;
            }
        }
    }

    private void updateA2dpAudioState(int state) {
        boolean isPlaying = state == 10 ? true : DEBUG;
        if (isPlaying != isPlayingState(this.mCurrentPlayState)) {
            if (!isPlaying || this.mAudioManager.isMusicActive()) {
                updatePlayPauseState(isPlaying ? 3 : 2, -1L);
            }
        }
    }

    private void updatePlayPauseState(int state, long currentPosMs) {
        boolean oldPosValid = this.mCurrentPosMs != -9216204211029966080L ? true : DEBUG;
        int oldPlayStatus = convertPlayStateToPlayStatus(this.mCurrentPlayState);
        int newPlayStatus = convertPlayStateToPlayStatus(state);
        if (this.mCurrentPlayState == 3 && this.mCurrentPlayState != state && oldPosValid) {
            this.mCurrentPosMs = getPlayPosition();
        }
        if (currentPosMs != -1) {
            this.mCurrentPosMs = currentPosMs;
        }
        if (state == 3 && (currentPosMs != -1 || this.mCurrentPlayState != 3)) {
            this.mPlayStartTimeMs = SystemClock.elapsedRealtime();
        }
        this.mCurrentPlayState = state;
        boolean newPosValid = this.mCurrentPosMs != -9216204211029966080L ? true : DEBUG;
        long playPosition = getPlayPosition();
        this.mHandler.removeMessages(5);
        if (this.mPlayPosChangedNT == 0 && (oldPlayStatus != newPlayStatus || oldPosValid != newPosValid || (newPosValid && (playPosition >= this.mNextPosMs || playPosition <= this.mPrevPosMs)))) {
            this.mPlayPosChangedNT = 1;
            registerNotificationRspPlayPosNative(this.mPlayPosChangedNT, (int) playPosition);
        }
        if (this.mPlayPosChangedNT == 0 && newPosValid && state == 3) {
            Message msg = this.mHandler.obtainMessage(5);
            this.mHandler.sendMessageDelayed(msg, this.mNextPosMs - playPosition);
        }
        if (this.mPlayStatusChangedNT == 0 && oldPlayStatus != newPlayStatus) {
            this.mPlayStatusChangedNT = 1;
            registerNotificationRspPlayStatusNative(this.mPlayStatusChangedNT, newPlayStatus);
        }
    }

    private void updateTransportControls(int transportControlFlags) {
        this.mTransportControlFlags = transportControlFlags;
    }

    class Metadata {
        private String artist = null;
        private String trackTitle = null;
        private String albumTitle = null;

        public Metadata() {
        }

        public String toString() {
            return "Metadata[artist=" + this.artist + " trackTitle=" + this.trackTitle + " albumTitle=" + this.albumTitle + "]";
        }
    }

    private void updateMetadata(RemoteController.MetadataEditor data) {
        String oldMetadata = this.mMetadata.toString();
        this.mMetadata.artist = data.getString(2, null);
        this.mMetadata.trackTitle = data.getString(7, null);
        this.mMetadata.albumTitle = data.getString(1, null);
        if (!oldMetadata.equals(this.mMetadata.toString())) {
            this.mTrackNumber++;
            if (this.mTrackChangedNT == 0) {
                this.mTrackChangedNT = 1;
                sendTrackChangedRsp();
            }
            if (this.mCurrentPosMs != -9216204211029966080L) {
                this.mCurrentPosMs = 0L;
                if (this.mCurrentPlayState == 3) {
                    this.mPlayStartTimeMs = SystemClock.elapsedRealtime();
                }
            }
            if (this.mPlayPosChangedNT == 0) {
                this.mPlayPosChangedNT = 1;
                registerNotificationRspPlayPosNative(this.mPlayPosChangedNT, (int) getPlayPosition());
                this.mHandler.removeMessages(5);
            }
        }
        this.mSongLengthMs = data.getLong(9, -1L);
    }

    private void getRcFeatures(byte[] address, int features) {
        Message msg = this.mHandler.obtainMessage(1, features, 0, Utils.getAddressStringFromByte(address));
        this.mHandler.sendMessage(msg);
    }

    private void getPlayStatus() {
        Message msg = this.mHandler.obtainMessage(2);
        this.mHandler.sendMessage(msg);
    }

    private void getElementAttr(byte numAttr, int[] attrs) {
        ArrayList<Integer> attrList = new ArrayList<>();
        for (int i = 0; i < numAttr; i++) {
            attrList.add(Integer.valueOf(attrs[i]));
        }
        Message msg = this.mHandler.obtainMessage(3, numAttr, 0, attrList);
        this.mHandler.sendMessage(msg);
    }

    private void registerNotification(int eventId, int param) {
        Message msg = this.mHandler.obtainMessage(4, eventId, param);
        this.mHandler.sendMessage(msg);
    }

    private void processRegisterNotification(int eventId, int param) {
        switch (eventId) {
            case 1:
                this.mPlayStatusChangedNT = 0;
                registerNotificationRspPlayStatusNative(this.mPlayStatusChangedNT, convertPlayStateToPlayStatus(this.mCurrentPlayState));
                break;
            case 2:
                this.mTrackChangedNT = 0;
                sendTrackChangedRsp();
                break;
            case 5:
                long songPosition = getPlayPosition();
                this.mPlayPosChangedNT = 0;
                this.mPlaybackIntervalMs = ((long) param) * 1000;
                if (this.mCurrentPosMs != -9216204211029966080L) {
                    this.mNextPosMs = this.mPlaybackIntervalMs + songPosition;
                    this.mPrevPosMs = songPosition - this.mPlaybackIntervalMs;
                    if (this.mCurrentPlayState == 3) {
                        Message msg = this.mHandler.obtainMessage(5);
                        this.mHandler.sendMessageDelayed(msg, this.mPlaybackIntervalMs);
                    }
                }
                registerNotificationRspPlayPosNative(this.mPlayPosChangedNT, (int) songPosition);
                break;
        }
    }

    private void handlePassthroughCmd(int id, int keyState) {
        switch (id) {
            case 72:
                rewind(keyState);
                break;
            case 73:
                fastForward(keyState);
                break;
        }
    }

    private void fastForward(int keyState) {
        Message msg = this.mHandler.obtainMessage(10, keyState, 0);
        this.mHandler.sendMessage(msg);
    }

    private void rewind(int keyState) {
        Message msg = this.mHandler.obtainMessage(11, keyState, 0);
        this.mHandler.sendMessage(msg);
    }

    private void changePositionBy(long amount) {
        long currentPosMs = getPlayPosition();
        if (currentPosMs != -1) {
            long newPosMs = Math.max(0L, currentPosMs + amount);
            this.mRemoteController.seekTo(newPosMs);
        }
    }

    private int getSkipMultiplier() {
        long currentTime = SystemClock.elapsedRealtime();
        long multi = (long) Math.pow(2.0d, (currentTime - this.mSkipStartTime) / 3000);
        return (int) Math.min(MAX_MULTIPLIER_VALUE, multi);
    }

    private void sendTrackChangedRsp() {
        byte[] track = new byte[8];
        for (int i = 0; i < 8; i++) {
            track[i] = (byte) (this.mTrackNumber >> (56 - (i * 8)));
        }
        registerNotificationRspTrackChangeNative(this.mTrackChangedNT, track);
    }

    private long getPlayPosition() {
        if (this.mCurrentPosMs == -9216204211029966080L) {
            return -1L;
        }
        if (this.mCurrentPlayState == 3) {
            long songPosition = (SystemClock.elapsedRealtime() - this.mPlayStartTimeMs) + this.mCurrentPosMs;
            return songPosition;
        }
        long songPosition2 = this.mCurrentPosMs;
        return songPosition2;
    }

    private String getAttributeString(int attrId) {
        String attrStr = null;
        switch (attrId) {
            case 1:
                attrStr = this.mMetadata.trackTitle;
                break;
            case 2:
                attrStr = this.mMetadata.artist;
                break;
            case 3:
                attrStr = this.mMetadata.albumTitle;
                break;
            case AbstractionLayer.BT_STATUS_PARM_INVALID:
                if (this.mSongLengthMs != 0) {
                    attrStr = Long.toString(this.mSongLengthMs);
                }
                break;
        }
        if (attrStr == null) {
            return new String();
        }
        return attrStr;
    }

    private int convertPlayStateToPlayStatus(int playState) {
        switch (playState) {
            case 0:
            case 1:
                return 0;
            case 2:
                return 2;
            case 3:
            case 8:
                return 1;
            case 4:
            case 6:
                return 3;
            case 5:
            case AbstractionLayer.BT_STATUS_PARM_INVALID:
                return 4;
            case AbstractionLayer.BT_STATUS_AUTH_FAILURE:
                return PLAYSTATUS_ERROR;
            default:
                return PLAYSTATUS_ERROR;
        }
    }

    private boolean isPlayingState(int playState) {
        switch (playState) {
            case 3:
            case 8:
                return true;
            default:
                return DEBUG;
        }
    }

    public boolean isAbsoluteVolumeSupported() {
        if ((this.mFeatures & 2) != 0) {
            return true;
        }
        return DEBUG;
    }

    public void adjustVolume(int direction) {
        Message msg = this.mHandler.obtainMessage(7, direction, 0);
        this.mHandler.sendMessage(msg);
    }

    public void setAbsoluteVolume(int volume) {
        int avrcpVolume = convertToAvrcpVolume(volume);
        int avrcpVolume2 = Math.min(AVRCP_MAX_VOL, Math.max(0, avrcpVolume));
        this.mHandler.removeMessages(7);
        Message msg = this.mHandler.obtainMessage(8, avrcpVolume2, 0);
        this.mHandler.sendMessage(msg);
    }

    private void volumeChangeCallback(int volume, int ctype) {
        Message msg = this.mHandler.obtainMessage(6, volume, ctype);
        this.mHandler.sendMessage(msg);
    }

    private void notifyVolumeChanged(int volume) {
        this.mAudioManager.setStreamVolume(3, convertToAudioStreamVolume(volume), 65);
    }

    private int convertToAudioStreamVolume(int volume) {
        return (int) Math.round((((double) volume) * ((double) this.mAudioStreamMax)) / 127.0d);
    }

    private int convertToAvrcpVolume(int volume) {
        return (int) Math.ceil((((double) volume) * 127.0d) / ((double) this.mAudioStreamMax));
    }

    public void setA2dpAudioState(int state) {
        Message msg = this.mHandler.obtainMessage(13, state, 0);
        this.mHandler.sendMessage(msg);
    }

    public void dump(StringBuilder sb) {
        sb.append("AVRCP:\n");
        ProfileService.println(sb, "mMetadata: " + this.mMetadata);
        ProfileService.println(sb, "mTransportControlFlags: " + this.mTransportControlFlags);
        ProfileService.println(sb, "mCurrentPlayState: " + this.mCurrentPlayState);
        ProfileService.println(sb, "mPlayStatusChangedNT: " + this.mPlayStatusChangedNT);
        ProfileService.println(sb, "mTrackChangedNT: " + this.mTrackChangedNT);
        ProfileService.println(sb, "mTrackNumber: " + this.mTrackNumber);
        ProfileService.println(sb, "mCurrentPosMs: " + this.mCurrentPosMs);
        ProfileService.println(sb, "mPlayStartTimeMs: " + this.mPlayStartTimeMs);
        ProfileService.println(sb, "mSongLengthMs: " + this.mSongLengthMs);
        ProfileService.println(sb, "mPlaybackIntervalMs: " + this.mPlaybackIntervalMs);
        ProfileService.println(sb, "mPlayPosChangedNT: " + this.mPlayPosChangedNT);
        ProfileService.println(sb, "mNextPosMs: " + this.mNextPosMs);
        ProfileService.println(sb, "mPrevPosMs: " + this.mPrevPosMs);
        ProfileService.println(sb, "mSkipStartTime: " + this.mSkipStartTime);
        ProfileService.println(sb, "mFeatures: " + this.mFeatures);
        ProfileService.println(sb, "mAbsoluteVolume: " + this.mAbsoluteVolume);
        ProfileService.println(sb, "mLastSetVolume: " + this.mLastSetVolume);
        ProfileService.println(sb, "mLastDirection: " + this.mLastDirection);
        ProfileService.println(sb, "mVolumeStep: " + this.mVolumeStep);
        ProfileService.println(sb, "mAudioStreamMax: " + this.mAudioStreamMax);
        ProfileService.println(sb, "mVolCmdInProgress: " + this.mVolCmdInProgress);
        ProfileService.println(sb, "mAbsVolRetryTimes: " + this.mAbsVolRetryTimes);
        ProfileService.println(sb, "mSkipAmount: " + this.mSkipAmount);
    }
}
