package com.android.bluetooth.avrcp;

import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.support.annotation.GuardedBy;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.android.bluetooth.Utils;
import com.android.bluetooth.avrcp.MediaController;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

class MediaPlayerWrapper {
    private static final long PLAYSTATE_BOUNCE_IGNORE_PERIOD = 500;
    private static final String TAG = "NewAvrcpMediaPlayerWrapper";
    private Looper mLooper;
    private MediaController mMediaController;
    private String mPackageName;
    private static final boolean DEBUG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    static boolean sTesting = false;

    @GuardedBy("mCallbackLock")
    private MediaControllerListener mControllerCallbacks = null;
    private final Object mCallbackLock = new Object();
    private Callback mRegisteredCallback = null;
    private MediaData mCurrentData = new MediaData(null, null, null);

    public interface Callback {
        void mediaUpdatedCallback(MediaData mediaData);
    }

    protected MediaPlayerWrapper() {
    }

    boolean isReady() {
        if (getPlaybackState() == null) {
            d("isReady(): PlaybackState is null");
            return false;
        }
        if (getMetadata() == null) {
            d("isReady(): Metadata is null");
            return false;
        }
        return true;
    }

    static MediaPlayerWrapper wrap(MediaController mediaController, Looper looper) {
        MediaPlayerWrapper mediaPlayerWrapper;
        if (mediaController == null || looper == null) {
            e("MediaPlayerWrapper.wrap(): Null parameter - Controller: " + mediaController + " | Looper: " + looper);
            return null;
        }
        if (mediaController.getPackageName().equals("com.google.android.music")) {
            Log.v(TAG, "Creating compatibility wrapper for Google Play Music");
            mediaPlayerWrapper = new GPMWrapper();
        } else {
            mediaPlayerWrapper = new MediaPlayerWrapper();
        }
        mediaPlayerWrapper.mMediaController = mediaController;
        mediaPlayerWrapper.mPackageName = mediaController.getPackageName();
        mediaPlayerWrapper.mLooper = looper;
        mediaPlayerWrapper.mCurrentData.queue = Util.toMetadataList(mediaPlayerWrapper.getQueue());
        mediaPlayerWrapper.mCurrentData.metadata = Util.toMetadata(mediaPlayerWrapper.getMetadata());
        mediaPlayerWrapper.mCurrentData.state = mediaPlayerWrapper.getPlaybackState();
        return mediaPlayerWrapper;
    }

    void cleanup() {
        unregisterCallback();
        this.mMediaController = null;
        this.mLooper = null;
    }

    String getPackageName() {
        return this.mPackageName;
    }

    protected List<MediaSession.QueueItem> getQueue() {
        return this.mMediaController.getQueue();
    }

    protected MediaMetadata getMetadata() {
        return this.mMediaController.getMetadata();
    }

    Metadata getCurrentMetadata() {
        if (getActiveQueueID() != -1 && !Utils.isPtsTestMode()) {
            for (Metadata metadata : getCurrentQueue()) {
                if (metadata.mediaId.equals(Util.NOW_PLAYING_PREFIX + getActiveQueueID())) {
                    d("getCurrentMetadata: Using playlist data: " + metadata.toString());
                    return metadata.m8clone();
                }
            }
        }
        return Util.toMetadata(getMetadata());
    }

    PlaybackState getPlaybackState() {
        return this.mMediaController.getPlaybackState();
    }

    long getActiveQueueID() {
        if (this.mMediaController.getPlaybackState() == null) {
            return -1L;
        }
        return this.mMediaController.getPlaybackState().getActiveQueueItemId();
    }

    List<Metadata> getCurrentQueue() {
        return this.mCurrentData.queue;
    }

    MediaData getCurrentMediaData() {
        return new MediaData(getCurrentMetadata(), getPlaybackState(), getCurrentQueue());
    }

    void playItemFromQueue(long j) {
        if (getQueue() == null) {
            Log.w(TAG, "playItemFromQueue: Trying to play item for player that has no queue: " + this.mPackageName);
            return;
        }
        this.mMediaController.getTransportControls().skipToQueueItem(j);
    }

    boolean isShuffleSupported() {
        return false;
    }

    boolean isRepeatSupported() {
        return false;
    }

    void toggleShuffle(boolean z) {
    }

    void toggleRepeat(boolean z) {
    }

    boolean isMetadataSynced() {
        if (getQueue() != null && getActiveQueueID() != -1) {
            MediaSession.QueueItem queueItem = null;
            Iterator<MediaSession.QueueItem> it = getQueue().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                MediaSession.QueueItem next = it.next();
                if (next.getQueueId() == getActiveQueueID()) {
                    queueItem = next;
                    break;
                }
            }
            Metadata metadata = Util.toMetadata(queueItem);
            Metadata metadata2 = Util.toMetadata(getMetadata());
            if (queueItem == null || !metadata.equals(metadata2)) {
                if (DEBUG) {
                    Log.d(TAG, "Metadata currently out of sync for " + this.mPackageName);
                    Log.d(TAG, "  └ Current queueItem: " + metadata);
                    Log.d(TAG, "  └ Current metadata : " + metadata2);
                    return false;
                }
                return false;
            }
            return true;
        }
        return true;
    }

    void registerCallback(Callback callback) {
        if (callback == null) {
            e("Cannot register null callbacks for " + this.mPackageName);
            return;
        }
        synchronized (this.mCallbackLock) {
            this.mRegisteredCallback = callback;
        }
        this.mCurrentData = new MediaData(Util.toMetadata(getMetadata()), getPlaybackState(), Util.toMetadataList(getQueue()));
        this.mControllerCallbacks = new MediaControllerListener(this.mLooper);
    }

    void unregisterCallback() {
        synchronized (this.mCallbackLock) {
            this.mRegisteredCallback = null;
        }
        if (this.mControllerCallbacks == null) {
            return;
        }
        this.mControllerCallbacks.cleanup();
        this.mControllerCallbacks = null;
    }

    void updateMediaController(MediaController mediaController) {
        if (mediaController == this.mMediaController) {
            return;
        }
        synchronized (this.mCallbackLock) {
            if (this.mRegisteredCallback != null && this.mControllerCallbacks != null) {
                this.mControllerCallbacks.cleanup();
                this.mMediaController = mediaController;
                this.mCurrentData = new MediaData(Util.toMetadata(getMetadata()), getPlaybackState(), Util.toMetadataList(getQueue()));
                this.mControllerCallbacks = new MediaControllerListener(this.mLooper);
                d("Controller for " + this.mPackageName + " was updated.");
            }
        }
    }

    private void sendMediaUpdate() {
        MediaData mediaData = new MediaData(Util.toMetadata(getMetadata()), getPlaybackState(), Util.toMetadataList(getQueue()));
        if (mediaData.equals(this.mCurrentData)) {
            Log.v(TAG, "Trying to update with last sent metadata");
            return;
        }
        synchronized (this.mCallbackLock) {
            if (this.mRegisteredCallback == null) {
                Log.e(TAG, this.mPackageName + "Trying to send an update with no registered callback");
                return;
            }
            Log.v(TAG, "trySendMediaUpdate(): Metadata has been updated for " + this.mPackageName);
            this.mRegisteredCallback.mediaUpdatedCallback(mediaData);
            this.mCurrentData = mediaData;
        }
    }

    class TimeoutHandler extends Handler {
        private static final long CALLBACK_TIMEOUT_MS = 2000;
        private static final int MSG_TIMEOUT = 0;

        TimeoutHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                Log.e(MediaPlayerWrapper.TAG, "Timeout while waiting for metadata to sync for " + MediaPlayerWrapper.this.mPackageName);
                Log.e(MediaPlayerWrapper.TAG, "  └ Current Metadata: " + Util.toMetadata(MediaPlayerWrapper.this.getMetadata()));
                Log.e(MediaPlayerWrapper.TAG, "  └ Current Playstate: " + MediaPlayerWrapper.this.getPlaybackState());
                List<Metadata> metadataList = Util.toMetadataList(MediaPlayerWrapper.this.getQueue());
                for (int i = 0; i < metadataList.size(); i++) {
                    Log.e(MediaPlayerWrapper.TAG, "  └ QueueItem(" + i + "): " + metadataList.get(i));
                }
                MediaPlayerWrapper.this.sendMediaUpdate();
                if (MediaPlayerWrapper.sTesting) {
                    Log.wtfStack(MediaPlayerWrapper.TAG, "Crashing the stack");
                    return;
                }
                return;
            }
            Log.wtf(MediaPlayerWrapper.TAG, "Unknown message on timeout handler: " + message.what);
        }
    }

    class MediaControllerListener extends MediaController.Callback {
        private Handler mTimeoutHandler;
        private final Object mTimeoutHandlerLock = new Object();

        MediaControllerListener(Looper looper) {
            synchronized (this.mTimeoutHandlerLock) {
                this.mTimeoutHandler = MediaPlayerWrapper.this.new TimeoutHandler(looper);
                MediaPlayerWrapper.this.mMediaController.registerCallback(this, this.mTimeoutHandler);
            }
        }

        void cleanup() {
            synchronized (this.mTimeoutHandlerLock) {
                MediaPlayerWrapper.this.mMediaController.unregisterCallback(this);
                this.mTimeoutHandler.removeMessages(0);
                this.mTimeoutHandler = null;
            }
        }

        void trySendMediaUpdate() {
            synchronized (this.mTimeoutHandlerLock) {
                if (this.mTimeoutHandler == null) {
                    return;
                }
                this.mTimeoutHandler.removeMessages(0);
                if (!MediaPlayerWrapper.this.isMetadataSynced()) {
                    MediaPlayerWrapper.this.d("trySendMediaUpdate(): Starting media update timeout");
                    this.mTimeoutHandler.sendEmptyMessageDelayed(0, 2000L);
                } else {
                    MediaPlayerWrapper.this.sendMediaUpdate();
                }
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadata mediaMetadata) {
            if (!MediaPlayerWrapper.this.isReady()) {
                Log.v(MediaPlayerWrapper.TAG, "onMetadataChanged(): " + MediaPlayerWrapper.this.mPackageName + " tried to update with no queue");
                return;
            }
            Log.v(MediaPlayerWrapper.TAG, "onMetadataChanged(): " + MediaPlayerWrapper.this.mPackageName + " : " + Util.toMetadata(mediaMetadata));
            if (!Objects.equals(mediaMetadata, MediaPlayerWrapper.this.getMetadata())) {
                MediaPlayerWrapper.e("The callback metadata doesn't match controller metadata");
            }
            if (Objects.equals(mediaMetadata, MediaPlayerWrapper.this.mCurrentData.metadata)) {
                Log.w(MediaPlayerWrapper.TAG, "onMetadataChanged(): " + MediaPlayerWrapper.this.mPackageName + " tried to update with no new data");
                return;
            }
            trySendMediaUpdate();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState playbackState) {
            if (!MediaPlayerWrapper.this.isReady()) {
                Log.v(MediaPlayerWrapper.TAG, "onPlaybackStateChanged(): " + MediaPlayerWrapper.this.mPackageName + " tried to update with no queue");
                return;
            }
            Log.v(MediaPlayerWrapper.TAG, "onPlaybackStateChanged(): " + MediaPlayerWrapper.this.mPackageName + " : " + playbackState.toString());
            if (!MediaPlayerWrapper.playstateEquals(playbackState, MediaPlayerWrapper.this.getPlaybackState())) {
                MediaPlayerWrapper.e("The callback playback state doesn't match the current state");
            }
            if (MediaPlayerWrapper.playstateEquals(playbackState, MediaPlayerWrapper.this.mCurrentData.state)) {
                Log.w(MediaPlayerWrapper.TAG, "onPlaybackStateChanged(): " + MediaPlayerWrapper.this.mPackageName + " tried to update with no new data");
                return;
            }
            if (playbackState.getState() == 0) {
                Log.v(MediaPlayerWrapper.TAG, "Waiting to send update as controller has no playback state");
            } else {
                trySendMediaUpdate();
            }
        }

        @Override
        public void onQueueChanged(List<MediaSession.QueueItem> list) {
            if (!MediaPlayerWrapper.this.isReady()) {
                Log.v(MediaPlayerWrapper.TAG, "onQueueChanged(): " + MediaPlayerWrapper.this.mPackageName + " tried to update with no queue");
                return;
            }
            Log.v(MediaPlayerWrapper.TAG, "onQueueChanged(): " + MediaPlayerWrapper.this.mPackageName);
            if (!Objects.equals(list, MediaPlayerWrapper.this.getQueue())) {
                MediaPlayerWrapper.e("The callback queue isn't the current queue");
            }
            List<Metadata> metadataList = Util.toMetadataList(list);
            if (metadataList.equals(MediaPlayerWrapper.this.mCurrentData.queue)) {
                Log.w(MediaPlayerWrapper.TAG, "onQueueChanged(): " + MediaPlayerWrapper.this.mPackageName + " tried to update with no new data");
                return;
            }
            if (MediaPlayerWrapper.DEBUG) {
                for (int i = 0; i < metadataList.size(); i++) {
                    Log.d(MediaPlayerWrapper.TAG, "  └ QueueItem(" + i + "): " + metadataList.get(i));
                }
            }
            trySendMediaUpdate();
        }

        @Override
        public void onSessionDestroyed() {
            Log.w(MediaPlayerWrapper.TAG, "The session was destroyed " + MediaPlayerWrapper.this.mPackageName);
        }

        @VisibleForTesting
        Handler getTimeoutHandler() {
            return this.mTimeoutHandler;
        }
    }

    static boolean playstateEquals(PlaybackState playbackState, PlaybackState playbackState2) {
        if (playbackState == playbackState2) {
            return true;
        }
        if (playbackState != null && playbackState2 != null && playbackState.getState() == playbackState2.getState() && playbackState.getActiveQueueItemId() == playbackState2.getActiveQueueItemId() && Math.abs(playbackState.getPosition() - playbackState2.getPosition()) < PLAYSTATE_BOUNCE_IGNORE_PERIOD) {
            return true;
        }
        return false;
    }

    private static void e(String str) {
        if (sTesting) {
            Log.wtfStack(TAG, str);
        } else {
            Log.e(TAG, str);
        }
    }

    private void d(String str) {
        if (DEBUG) {
            Log.d(TAG, this.mPackageName + ": " + str);
        }
    }

    @VisibleForTesting
    Handler getTimeoutHandler() {
        if (this.mControllerCallbacks == null) {
            return null;
        }
        return this.mControllerCallbacks.getTimeoutHandler();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.mMediaController.toString() + "\n");
        sb.append("Current Data:\n");
        sb.append("  Song: " + this.mCurrentData.metadata + "\n");
        sb.append("  PlayState: " + this.mCurrentData.state + "\n");
        sb.append("  Queue: size=" + this.mCurrentData.queue.size() + "\n");
        Iterator<Metadata> it = this.mCurrentData.queue.iterator();
        while (it.hasNext()) {
            sb.append("    " + it.next() + "\n");
        }
        return sb.toString();
    }
}
