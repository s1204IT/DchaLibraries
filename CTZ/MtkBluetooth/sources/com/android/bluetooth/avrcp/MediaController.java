package com.android.bluetooth.avrcp;

import android.app.PendingIntent;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.Rating;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.view.KeyEvent;
import java.util.List;

public class MediaController {
    public android.media.session.MediaController mDelegate;
    public TransportControls mTransportControls = new TransportControls();
    public MediaController.TransportControls mTransportDelegate;

    public static abstract class Callback extends MediaController.Callback {
    }

    public MediaController(android.media.session.MediaController mediaController) {
        this.mDelegate = mediaController;
        this.mTransportDelegate = mediaController.getTransportControls();
    }

    public MediaController(Context context, MediaSession.Token token) {
        this.mDelegate = new android.media.session.MediaController(context, token);
        this.mTransportDelegate = this.mDelegate.getTransportControls();
    }

    public android.media.session.MediaController getWrappedInstance() {
        return this.mDelegate;
    }

    public TransportControls getTransportControls() {
        return this.mTransportControls;
    }

    public boolean dispatchMediaButtonEvent(KeyEvent keyEvent) {
        return this.mDelegate.dispatchMediaButtonEvent(keyEvent);
    }

    public PlaybackState getPlaybackState() {
        return this.mDelegate.getPlaybackState();
    }

    public MediaMetadata getMetadata() {
        return this.mDelegate.getMetadata();
    }

    public List<MediaSession.QueueItem> getQueue() {
        return this.mDelegate.getQueue();
    }

    public CharSequence getQueueTitle() {
        return this.mDelegate.getQueueTitle();
    }

    public Bundle getExtras() {
        return this.mDelegate.getExtras();
    }

    public int getRatingType() {
        return this.mDelegate.getRatingType();
    }

    public long getFlags() {
        return this.mDelegate.getFlags();
    }

    public MediaController.PlaybackInfo getPlaybackInfo() {
        return this.mDelegate.getPlaybackInfo();
    }

    public PendingIntent getSessionActivity() {
        return this.mDelegate.getSessionActivity();
    }

    public MediaSession.Token getSessionToken() {
        return this.mDelegate.getSessionToken();
    }

    public void setVolumeTo(int i, int i2) {
        this.mDelegate.setVolumeTo(i, i2);
    }

    public void adjustVolume(int i, int i2) {
        this.mDelegate.adjustVolume(i, i2);
    }

    public void registerCallback(Callback callback) {
        this.mDelegate.registerCallback(callback);
    }

    public void registerCallback(Callback callback, Handler handler) {
        this.mDelegate.registerCallback(callback, handler);
    }

    public void unregisterCallback(Callback callback) {
        this.mDelegate.unregisterCallback(callback);
    }

    public void sendCommand(String str, Bundle bundle, ResultReceiver resultReceiver) {
        this.mDelegate.sendCommand(str, bundle, resultReceiver);
    }

    public String getPackageName() {
        return this.mDelegate.getPackageName();
    }

    public String getTag() {
        return this.mDelegate.getTag();
    }

    public boolean controlsSameSession(MediaController mediaController) {
        return this.mDelegate.controlsSameSession(mediaController.getWrappedInstance());
    }

    public boolean controlsSameSession(android.media.session.MediaController mediaController) {
        return this.mDelegate.controlsSameSession(mediaController);
    }

    public boolean equals(Object obj) {
        if (obj instanceof android.media.session.MediaController) {
            return this.mDelegate.equals(obj);
        }
        if (obj instanceof MediaController) {
            return this.mDelegate.equals(obj.mDelegate);
        }
        return false;
    }

    public String toString() {
        MediaMetadata metadata = getMetadata();
        return "MediaController (" + getPackageName() + "@" + Integer.toHexString(this.mDelegate.hashCode()) + ") " + (metadata == null ? null : metadata.getDescription());
    }

    public class TransportControls {
        public TransportControls() {
        }

        public void prepare() {
            MediaController.this.mTransportDelegate.prepare();
        }

        public void prepareFromMediaId(String str, Bundle bundle) {
            MediaController.this.mTransportDelegate.prepareFromMediaId(str, bundle);
        }

        public void prepareFromSearch(String str, Bundle bundle) {
            MediaController.this.mTransportDelegate.prepareFromSearch(str, bundle);
        }

        public void prepareFromUri(Uri uri, Bundle bundle) {
            MediaController.this.mTransportDelegate.prepareFromUri(uri, bundle);
        }

        public void play() {
            MediaController.this.mTransportDelegate.play();
        }

        public void playFromMediaId(String str, Bundle bundle) {
            MediaController.this.mTransportDelegate.playFromMediaId(str, bundle);
        }

        public void playFromSearch(String str, Bundle bundle) {
            MediaController.this.mTransportDelegate.playFromSearch(str, bundle);
        }

        public void playFromUri(Uri uri, Bundle bundle) {
            MediaController.this.mTransportDelegate.playFromUri(uri, bundle);
        }

        public void skipToQueueItem(long j) {
            MediaController.this.mTransportDelegate.skipToQueueItem(j);
        }

        public void pause() {
            MediaController.this.mTransportDelegate.pause();
        }

        public void stop() {
            MediaController.this.mTransportDelegate.stop();
        }

        public void seekTo(long j) {
            MediaController.this.mTransportDelegate.seekTo(j);
        }

        public void fastForward() {
            MediaController.this.mTransportDelegate.fastForward();
        }

        public void skipToNext() {
            MediaController.this.mTransportDelegate.skipToNext();
        }

        public void rewind() {
            MediaController.this.mTransportDelegate.rewind();
        }

        public void skipToPrevious() {
            MediaController.this.mTransportDelegate.skipToPrevious();
        }

        public void setRating(Rating rating) {
            MediaController.this.mTransportDelegate.setRating(rating);
        }

        public void sendCustomAction(PlaybackState.CustomAction customAction, Bundle bundle) {
            MediaController.this.mTransportDelegate.sendCustomAction(customAction, bundle);
        }

        public void sendCustomAction(String str, Bundle bundle) {
            MediaController.this.mTransportDelegate.sendCustomAction(str, bundle);
        }
    }
}
