package com.mediatek.bluetooth.avrcp;

import android.os.RemoteCallbackList;
import android.os.RemoteException;
import com.android.music.MediaPlaybackService;
import com.android.music.MusicLogUtils;
import com.mediatek.bluetooth.avrcp.IBTAvrcpMusic;

public class ServiceAvrcpStub extends IBTAvrcpMusic.Stub {
    private MediaPlaybackService mService;
    private int mRepeatMode = 1;
    private int mShuffleMode = 1;
    final RemoteCallbackList<IBTAvrcpMusicCallback> mAvrcpCallbacksList = new RemoteCallbackList<>();
    protected boolean bPlaybackFlag = false;
    protected boolean bTrackchangeFlag = false;
    protected boolean bTrackReachStartFlag = false;
    protected boolean bTrackReachEndFlag = false;
    protected boolean bTrackPosChangedFlag = false;
    protected boolean bTrackAppSettingChangedFlag = false;
    protected boolean bTrackNowPlayingChangedFlag = false;

    public ServiceAvrcpStub(MediaPlaybackService mediaPlaybackService) {
        this.mService = mediaPlaybackService;
    }

    @Override
    public void registerCallback(IBTAvrcpMusicCallback iBTAvrcpMusicCallback) {
        MusicLogUtils.d("SERVICE_AVRCP_STUB", "[AVRCP] ServiceAvrcpStub. registerCallback");
        if (iBTAvrcpMusicCallback != null) {
            this.mAvrcpCallbacksList.register(iBTAvrcpMusicCallback);
        }
        getRepeatMode();
        getShuffleMode();
    }

    @Override
    public void unregisterCallback(IBTAvrcpMusicCallback iBTAvrcpMusicCallback) {
        MusicLogUtils.d("SERVICE_AVRCP_STUB", "[AVRCP] ServiceAvrcpStub. unregisterCallback");
        if (iBTAvrcpMusicCallback != null) {
            this.mAvrcpCallbacksList.unregister(iBTAvrcpMusicCallback);
        }
    }

    @Override
    public boolean regNotificationEvent(byte b, int i) {
        if (b != 9) {
            switch (b) {
                case 1:
                    this.bPlaybackFlag = true;
                    MusicLogUtils.v("SERVICE_AVRCP_STUB", "[AVRCP] bPlaybackFlag flag is " + this.bPlaybackFlag);
                    return true;
                case 2:
                    this.bTrackchangeFlag = true;
                    MusicLogUtils.v("SERVICE_AVRCP_STUB", "[AVRCP] bTrackchange flag is " + this.bTrackchangeFlag);
                    return this.bTrackchangeFlag;
                default:
                    MusicLogUtils.e("SERVICE_AVRCP_STUB", "[AVRCP] MusicApp doesn't support eventId:" + ((int) b));
                    return false;
            }
        }
        this.bTrackNowPlayingChangedFlag = true;
        return true;
    }

    @Override
    public boolean setPlayerApplicationSettingValue(byte b, byte b2) {
        return false;
    }

    @Override
    public byte[] getCapabilities() {
        return null;
    }

    @Override
    public void stop() {
        this.mService.stop();
    }

    @Override
    public void pause() {
        this.mService.pause();
    }

    @Override
    public void resume() {
    }

    @Override
    public void play() {
        this.mService.play();
    }

    @Override
    public void prev() {
        this.mService.prev();
    }

    @Override
    public void next() {
        this.mService.gotoNext(true);
    }

    @Override
    public void nextGroup() {
        this.mService.gotoNext(true);
    }

    @Override
    public void prevGroup() {
        this.mService.prev();
    }

    @Override
    public byte getPlayStatus() {
        if (true == this.mService.isPlaying()) {
            return (byte) 1;
        }
        if (!this.mService.isCursorNull()) {
            return (byte) 2;
        }
        return (byte) 0;
    }

    @Override
    public long getAudioId() {
        return this.mService.getAudioId();
    }

    @Override
    public String getTrackName() {
        return this.mService.getTrackName();
    }

    @Override
    public String getAlbumName() {
        return this.mService.getAlbumName();
    }

    @Override
    public long getAlbumId() {
        return this.mService.getAlbumId();
    }

    @Override
    public String getArtistName() {
        return this.mService.getArtistName();
    }

    @Override
    public long position() {
        return this.mService.position();
    }

    @Override
    public long duration() {
        return this.mService.duration();
    }

    @Override
    public boolean setEqualizeMode(int i) {
        return false;
    }

    @Override
    public int getEqualizeMode() {
        return 0;
    }

    @Override
    public boolean setShuffleMode(int i) {
        int i2 = 0;
        switch (i) {
            case 1:
                break;
            case 2:
                i2 = 1;
                break;
            default:
                return false;
        }
        MusicLogUtils.d("SERVICE_AVRCP_STUB", "[AVRCP] setShuffleMode music_mode:" + i2);
        this.mService.setShuffleMode(i2);
        return true;
    }

    @Override
    public int getShuffleMode() {
        this.mShuffleMode = this.mService.getShuffleMode() + 1;
        return this.mShuffleMode;
    }

    @Override
    public boolean setRepeatMode(int i) {
        int i2;
        switch (i) {
            case 1:
                i2 = 0;
                break;
            case 2:
                i2 = 1;
                break;
            case 3:
                i2 = 2;
                break;
            default:
                return false;
        }
        MusicLogUtils.d("SERVICE_AVRCP_STUB", String.format("[AVRCP] setRepeatMode musid_mode:%d", Integer.valueOf(i2)));
        this.mService.setRepeatMode(i2);
        return true;
    }

    @Override
    public int getRepeatMode() {
        this.mRepeatMode = this.mService.getRepeatMode() + 1;
        return this.mRepeatMode;
    }

    @Override
    public boolean setScanMode(int i) {
        return false;
    }

    @Override
    public int getScanMode() {
        return 0;
    }

    @Override
    public boolean informDisplayableCharacterSet(int i) {
        if (i == 106) {
            return true;
        }
        return false;
    }

    @Override
    public boolean informBatteryStatusOfCT() {
        return true;
    }

    @Override
    public void enqueue(long[] jArr, int i) {
        this.mService.enqueue(jArr, i);
    }

    @Override
    public long[] getNowPlaying() {
        return this.mService.getQueue();
    }

    @Override
    public String getNowPlayingItemName(long j) {
        return null;
    }

    @Override
    public void open(long[] jArr, int i) {
        this.mService.open(jArr, i);
    }

    @Override
    public int getQueuePosition() {
        return this.mService.getQueuePosition();
    }

    @Override
    public void setQueuePosition(int i) {
        this.mService.setQueuePosition(i);
    }

    public void notifyBTAvrcp(String str) {
        MusicLogUtils.v("SERVICE_AVRCP_STUB", "[AVRCP] notifyBTAvrcp " + str);
        if ("com.android.music.playstatechanged".equals(str)) {
            notifyPlaybackStatus(getPlayStatus());
        }
        if ("com.android.music.playbackcomplete".equals(str)) {
            notifyTrackChanged();
        }
        if ("com.android.music.queuechanged".equals(str)) {
            notifyTrackChanged();
            notifyNowPlayingContentChanged();
        }
        if ("com.android.music.metachanged".equals(str)) {
            notifyTrackChanged();
        }
    }

    protected void notifyPlaybackStatus(byte b) {
        this.bPlaybackFlag = false;
        int iBeginBroadcast = this.mAvrcpCallbacksList.beginBroadcast();
        MusicLogUtils.d("SERVICE_AVRCP_STUB", "[AVRCP] notifyPlaybackStatus " + ((int) b) + " N= " + iBeginBroadcast);
        for (int i = 0; i < iBeginBroadcast; i++) {
            try {
                ((IBTAvrcpMusicCallback) this.mAvrcpCallbacksList.getBroadcastItem(i)).notifyPlaybackStatus(b);
            } catch (RemoteException e) {
                MusicLogUtils.e("SERVICE_AVRCP_STUB", "Error:" + e);
            }
        }
        this.mAvrcpCallbacksList.finishBroadcast();
    }

    protected void notifyTrackChanged() {
        this.bTrackchangeFlag = false;
        int iBeginBroadcast = this.mAvrcpCallbacksList.beginBroadcast();
        MusicLogUtils.d("SERVICE_AVRCP_STUB", "[AVRCP] notifyTrackChanged  N= " + iBeginBroadcast);
        for (int i = 0; i < iBeginBroadcast; i++) {
            try {
                ((IBTAvrcpMusicCallback) this.mAvrcpCallbacksList.getBroadcastItem(i)).notifyTrackChanged(getAudioId());
            } catch (RemoteException e) {
                MusicLogUtils.e("SERVICE_AVRCP_STUB", "Error:" + e);
            }
        }
        this.mAvrcpCallbacksList.finishBroadcast();
    }

    protected void notifyNowPlayingContentChanged() {
        MusicLogUtils.v("SERVICE_AVRCP_STUB", "[AVRCP] notifyNowPlayingContentChanged ");
        if (this.mAvrcpCallbacksList == null) {
            return;
        }
        this.bTrackNowPlayingChangedFlag = false;
        int iBeginBroadcast = this.mAvrcpCallbacksList.beginBroadcast();
        MusicLogUtils.d("SERVICE_AVRCP_STUB", "[AVRCP] notifyNowPlayingContentChanged  N= " + iBeginBroadcast);
        for (int i = 0; i < iBeginBroadcast; i++) {
            try {
                ((IBTAvrcpMusicCallback) this.mAvrcpCallbacksList.getBroadcastItem(i)).notifyNowPlayingContentChanged();
            } catch (RemoteException e) {
                MusicLogUtils.e("SERVICE_AVRCP_STUB", "Error:" + e);
            }
        }
        this.mAvrcpCallbacksList.finishBroadcast();
    }
}
