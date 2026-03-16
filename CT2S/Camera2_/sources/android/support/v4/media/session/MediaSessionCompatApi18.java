package android.support.v4.media.session;

import android.app.PendingIntent;
import android.content.Context;
import android.media.AudioManager;
import android.media.RemoteControlClient;
import android.os.SystemClock;
import android.support.v4.media.session.MediaSessionCompatApi14;

public class MediaSessionCompatApi18 {
    public static Object createPlaybackPositionUpdateListener(MediaSessionCompatApi14.Callback callback) {
        return new OnPlaybackPositionUpdateListener(callback);
    }

    public static void registerMediaButtonEventReceiver(Context context, PendingIntent pi) {
        AudioManager am = (AudioManager) context.getSystemService("audio");
        am.registerMediaButtonEventReceiver(pi);
    }

    public static void unregisterMediaButtonEventReceiver(Context context, PendingIntent pi) {
        AudioManager am = (AudioManager) context.getSystemService("audio");
        am.unregisterMediaButtonEventReceiver(pi);
    }

    public static void setState(Object rccObj, int state, long position, float speed, long updateTime) {
        long currTime = SystemClock.elapsedRealtime();
        if (state == 3 && position > 0) {
            long diff = 0;
            if (updateTime > 0) {
                diff = currTime - updateTime;
                if (speed > 0.0f && speed != 1.0f) {
                    diff = (long) (diff * speed);
                }
            }
            position += diff;
        }
        ((RemoteControlClient) rccObj).setPlaybackState(MediaSessionCompatApi14.getRccStateFromState(state), position, speed);
    }

    public static void setOnPlaybackPositionUpdateListener(Object rccObj, Object onPositionUpdateObj) {
        ((RemoteControlClient) rccObj).setPlaybackPositionUpdateListener((RemoteControlClient.OnPlaybackPositionUpdateListener) onPositionUpdateObj);
    }

    static class OnPlaybackPositionUpdateListener<T extends MediaSessionCompatApi14.Callback> implements RemoteControlClient.OnPlaybackPositionUpdateListener {
        protected final T mCallback;

        public OnPlaybackPositionUpdateListener(T callback) {
            this.mCallback = callback;
        }

        @Override
        public void onPlaybackPositionUpdate(long newPositionMs) {
            this.mCallback.onSeekTo(newPositionMs);
        }
    }
}
