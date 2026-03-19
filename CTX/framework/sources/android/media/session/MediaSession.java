package android.media.session;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.media.AudioAttributes;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.Rating;
import android.media.VolumeProvider;
import android.media.session.ISessionCallback;
import android.media.session.ISessionController;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import com.android.internal.R;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;

public final class MediaSession {
    private static final boolean DEBUG;
    public static final int FLAG_EXCLUSIVE_GLOBAL_PRIORITY = 65536;

    @Deprecated
    public static final int FLAG_HANDLES_MEDIA_BUTTONS = 1;

    @Deprecated
    public static final int FLAG_HANDLES_TRANSPORT_CONTROLS = 2;
    public static final int INVALID_PID = -1;
    public static final int INVALID_UID = -1;
    private static final String TAG = "MediaSession";
    private boolean mActive;
    private final ISession mBinder;
    private CallbackMessageHandler mCallback;
    private final CallbackStub mCbStub;
    private final MediaController mController;
    private final Object mLock;
    private final int mMaxBitmapSize;
    private PlaybackState mPlaybackState;
    private final Token mSessionToken;
    private VolumeProvider mVolumeProvider;

    @Retention(RetentionPolicy.SOURCE)
    public @interface SessionFlags {
    }

    static {
        DEBUG = Log.isLoggable(TAG, 3) || !"user".equals(Build.TYPE);
    }

    public MediaSession(Context context, String str) {
        this(context, str, UserHandle.myUserId());
    }

    public MediaSession(Context context, String str, int i) {
        this.mLock = new Object();
        this.mActive = false;
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null.");
        }
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("tag cannot be null or empty");
        }
        this.mMaxBitmapSize = context.getResources().getDimensionPixelSize(R.dimen.config_mediaMetadataBitmapMaxSize);
        this.mCbStub = new CallbackStub(this);
        try {
            this.mBinder = ((MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE)).createSession(this.mCbStub, str, i);
            this.mSessionToken = new Token(this.mBinder.getController());
            this.mController = new MediaController(context, this.mSessionToken);
        } catch (RemoteException e) {
            throw new RuntimeException("Remote error creating session.", e);
        }
    }

    public void setCallback(Callback callback) {
        setCallback(callback, null);
    }

    public void setCallback(Callback callback, Handler handler) {
        synchronized (this.mLock) {
            if (this.mCallback != null) {
                this.mCallback.mCallback.mSession = null;
                this.mCallback.removeCallbacksAndMessages(null);
            }
            if (callback == null) {
                this.mCallback = null;
                return;
            }
            if (handler == null) {
                handler = new Handler();
            }
            callback.mSession = this;
            this.mCallback = new CallbackMessageHandler(handler.getLooper(), callback);
        }
    }

    public void setSessionActivity(PendingIntent pendingIntent) {
        try {
            this.mBinder.setLaunchPendingIntent(pendingIntent);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Failure in setLaunchPendingIntent.", e);
        }
    }

    public void setMediaButtonReceiver(PendingIntent pendingIntent) {
        try {
            this.mBinder.setMediaButtonReceiver(pendingIntent);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Failure in setMediaButtonReceiver.", e);
        }
    }

    public void setFlags(int i) {
        try {
            this.mBinder.setFlags(i);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Failure in setFlags.", e);
        }
    }

    public void setPlaybackToLocal(AudioAttributes audioAttributes) {
        if (audioAttributes == null) {
            throw new IllegalArgumentException("Attributes cannot be null for local playback.");
        }
        try {
            this.mBinder.setPlaybackToLocal(audioAttributes);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Failure in setPlaybackToLocal.", e);
        }
    }

    public void setPlaybackToRemote(VolumeProvider volumeProvider) {
        if (volumeProvider == null) {
            throw new IllegalArgumentException("volumeProvider may not be null!");
        }
        synchronized (this.mLock) {
            this.mVolumeProvider = volumeProvider;
        }
        volumeProvider.setCallback(new VolumeProvider.Callback() {
            @Override
            public void onVolumeChanged(VolumeProvider volumeProvider2) {
                MediaSession.this.notifyRemoteVolumeChanged(volumeProvider2);
            }
        });
        try {
            this.mBinder.setPlaybackToRemote(volumeProvider.getVolumeControl(), volumeProvider.getMaxVolume());
            this.mBinder.setCurrentVolume(volumeProvider.getCurrentVolume());
        } catch (RemoteException e) {
            Log.wtf(TAG, "Failure in setPlaybackToRemote.", e);
        }
    }

    public void setActive(boolean z) {
        if (this.mActive == z) {
            return;
        }
        try {
            this.mBinder.setActive(z);
            this.mActive = z;
        } catch (RemoteException e) {
            Log.wtf(TAG, "Failure in setActive.", e);
        }
    }

    public boolean isActive() {
        return this.mActive;
    }

    public void sendSessionEvent(String str, Bundle bundle) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("event cannot be null or empty");
        }
        try {
            this.mBinder.sendEvent(str, bundle);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Error sending event", e);
        }
    }

    public void release() {
        try {
            this.mBinder.destroy();
        } catch (RemoteException e) {
            Log.wtf(TAG, "Error releasing session: ", e);
        }
    }

    public Token getSessionToken() {
        return this.mSessionToken;
    }

    public MediaController getController() {
        return this.mController;
    }

    public void setPlaybackState(PlaybackState playbackState) {
        this.mPlaybackState = playbackState;
        if (DEBUG) {
            Log.d(TAG, "setPlaybackState");
        }
        try {
            this.mBinder.setPlaybackState(playbackState);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Dead object in setPlaybackState.", e);
        }
    }

    public void setMetadata(MediaMetadata mediaMetadata) {
        if (DEBUG) {
            Log.d(TAG, "setMetadata");
        }
        if (mediaMetadata != null) {
            mediaMetadata = new MediaMetadata.Builder(mediaMetadata, this.mMaxBitmapSize).build();
        }
        try {
            this.mBinder.setMetadata(mediaMetadata);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Dead object in setPlaybackState.", e);
        }
    }

    public void setQueue(List<QueueItem> list) {
        try {
            this.mBinder.setQueue(list == null ? null : new ParceledListSlice(list));
        } catch (RemoteException e) {
            Log.wtf("Dead object in setQueue.", e);
        }
    }

    public void setQueueTitle(CharSequence charSequence) {
        try {
            this.mBinder.setQueueTitle(charSequence);
        } catch (RemoteException e) {
            Log.wtf("Dead object in setQueueTitle.", e);
        }
    }

    public void setRatingType(int i) {
        try {
            this.mBinder.setRatingType(i);
        } catch (RemoteException e) {
            Log.e(TAG, "Error in setRatingType.", e);
        }
    }

    public void setExtras(Bundle bundle) {
        try {
            this.mBinder.setExtras(bundle);
        } catch (RemoteException e) {
            Log.wtf("Dead object in setExtras.", e);
        }
    }

    public final MediaSessionManager.RemoteUserInfo getCurrentControllerInfo() {
        if (this.mCallback == null || this.mCallback.mCurrentControllerInfo == null) {
            throw new IllegalStateException("This should be called inside of MediaSession.Callback methods");
        }
        return this.mCallback.mCurrentControllerInfo;
    }

    public void notifyRemoteVolumeChanged(VolumeProvider volumeProvider) {
        synchronized (this.mLock) {
            if (volumeProvider != null) {
                if (volumeProvider == this.mVolumeProvider) {
                    try {
                        this.mBinder.setCurrentVolume(volumeProvider.getCurrentVolume());
                        return;
                    } catch (RemoteException e) {
                        Log.e(TAG, "Error in notifyVolumeChanged", e);
                        return;
                    }
                }
            }
            Log.w(TAG, "Received update from stale volume provider");
        }
    }

    public String getCallingPackage() {
        if (this.mCallback == null || this.mCallback.mCurrentControllerInfo == null) {
            return null;
        }
        return this.mCallback.mCurrentControllerInfo.getPackageName();
    }

    private void dispatchPrepare(MediaSessionManager.RemoteUserInfo remoteUserInfo) {
        postToCallback(remoteUserInfo, 3, null, null);
    }

    private void dispatchPrepareFromMediaId(MediaSessionManager.RemoteUserInfo remoteUserInfo, String str, Bundle bundle) {
        postToCallback(remoteUserInfo, 4, str, bundle);
    }

    private void dispatchPrepareFromSearch(MediaSessionManager.RemoteUserInfo remoteUserInfo, String str, Bundle bundle) {
        postToCallback(remoteUserInfo, 5, str, bundle);
    }

    private void dispatchPrepareFromUri(MediaSessionManager.RemoteUserInfo remoteUserInfo, Uri uri, Bundle bundle) {
        postToCallback(remoteUserInfo, 6, uri, bundle);
    }

    private void dispatchPlay(MediaSessionManager.RemoteUserInfo remoteUserInfo) {
        postToCallback(remoteUserInfo, 7, null, null);
    }

    private void dispatchPlayFromMediaId(MediaSessionManager.RemoteUserInfo remoteUserInfo, String str, Bundle bundle) {
        postToCallback(remoteUserInfo, 8, str, bundle);
    }

    private void dispatchPlayFromSearch(MediaSessionManager.RemoteUserInfo remoteUserInfo, String str, Bundle bundle) {
        postToCallback(remoteUserInfo, 9, str, bundle);
    }

    private void dispatchPlayFromUri(MediaSessionManager.RemoteUserInfo remoteUserInfo, Uri uri, Bundle bundle) {
        postToCallback(remoteUserInfo, 10, uri, bundle);
    }

    private void dispatchSkipToItem(MediaSessionManager.RemoteUserInfo remoteUserInfo, long j) {
        postToCallback(remoteUserInfo, 11, Long.valueOf(j), null);
    }

    private void dispatchPause(MediaSessionManager.RemoteUserInfo remoteUserInfo) {
        postToCallback(remoteUserInfo, 12, null, null);
    }

    private void dispatchStop(MediaSessionManager.RemoteUserInfo remoteUserInfo) {
        postToCallback(remoteUserInfo, 13, null, null);
    }

    private void dispatchNext(MediaSessionManager.RemoteUserInfo remoteUserInfo) {
        postToCallback(remoteUserInfo, 14, null, null);
    }

    private void dispatchPrevious(MediaSessionManager.RemoteUserInfo remoteUserInfo) {
        postToCallback(remoteUserInfo, 15, null, null);
    }

    private void dispatchFastForward(MediaSessionManager.RemoteUserInfo remoteUserInfo) {
        postToCallback(remoteUserInfo, 16, null, null);
    }

    private void dispatchRewind(MediaSessionManager.RemoteUserInfo remoteUserInfo) {
        postToCallback(remoteUserInfo, 17, null, null);
    }

    private void dispatchSeekTo(MediaSessionManager.RemoteUserInfo remoteUserInfo, long j) {
        postToCallback(remoteUserInfo, 18, Long.valueOf(j), null);
    }

    private void dispatchRate(MediaSessionManager.RemoteUserInfo remoteUserInfo, Rating rating) {
        postToCallback(remoteUserInfo, 19, rating, null);
    }

    private void dispatchCustomAction(MediaSessionManager.RemoteUserInfo remoteUserInfo, String str, Bundle bundle) {
        postToCallback(remoteUserInfo, 20, str, bundle);
    }

    private void dispatchMediaButton(MediaSessionManager.RemoteUserInfo remoteUserInfo, Intent intent) {
        postToCallback(remoteUserInfo, 2, intent, null);
    }

    private void dispatchMediaButtonDelayed(MediaSessionManager.RemoteUserInfo remoteUserInfo, Intent intent, long j) {
        postToCallbackDelayed(remoteUserInfo, 23, intent, null, j);
    }

    private void dispatchAdjustVolume(MediaSessionManager.RemoteUserInfo remoteUserInfo, int i) {
        postToCallback(remoteUserInfo, 21, Integer.valueOf(i), null);
    }

    private void dispatchSetVolumeTo(MediaSessionManager.RemoteUserInfo remoteUserInfo, int i) {
        postToCallback(remoteUserInfo, 22, Integer.valueOf(i), null);
    }

    private void dispatchCommand(MediaSessionManager.RemoteUserInfo remoteUserInfo, String str, Bundle bundle, ResultReceiver resultReceiver) {
        postToCallback(remoteUserInfo, 1, new Command(str, bundle, resultReceiver), null);
    }

    private void postToCallback(MediaSessionManager.RemoteUserInfo remoteUserInfo, int i, Object obj, Bundle bundle) {
        postToCallbackDelayed(remoteUserInfo, i, obj, bundle, 0L);
    }

    private void postToCallbackDelayed(MediaSessionManager.RemoteUserInfo remoteUserInfo, int i, Object obj, Bundle bundle, long j) {
        synchronized (this.mLock) {
            if (this.mCallback != null) {
                this.mCallback.post(remoteUserInfo, i, obj, bundle, j);
            }
        }
    }

    public static boolean isActiveState(int i) {
        switch (i) {
            case 3:
            case 4:
            case 5:
            case 6:
            case 8:
            case 9:
            case 10:
                return true;
            case 7:
            default:
                return false;
        }
    }

    public static final class Token implements Parcelable {
        public static final Parcelable.Creator<Token> CREATOR = new Parcelable.Creator<Token>() {
            @Override
            public Token createFromParcel(Parcel parcel) {
                return new Token(ISessionController.Stub.asInterface(parcel.readStrongBinder()));
            }

            @Override
            public Token[] newArray(int i) {
                return new Token[i];
            }
        };
        private ISessionController mBinder;

        public Token(ISessionController iSessionController) {
            this.mBinder = iSessionController;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeStrongBinder(this.mBinder.asBinder());
        }

        public int hashCode() {
            return 31 + (this.mBinder == null ? 0 : this.mBinder.asBinder().hashCode());
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Token token = (Token) obj;
            if (this.mBinder == null) {
                if (token.mBinder != null) {
                    return false;
                }
            } else if (!this.mBinder.asBinder().equals(token.mBinder.asBinder())) {
                return false;
            }
            return true;
        }

        ISessionController getBinder() {
            return this.mBinder;
        }
    }

    public static abstract class Callback {
        private CallbackMessageHandler mHandler;
        private boolean mMediaPlayPauseKeyPending;
        private MediaSession mSession;

        public void onCommand(String str, Bundle bundle, ResultReceiver resultReceiver) {
        }

        public boolean onMediaButtonEvent(Intent intent) {
            KeyEvent keyEvent;
            long actions;
            if (this.mSession != null && this.mHandler != null && Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction()) && (keyEvent = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)) != null && keyEvent.getAction() == 0) {
                PlaybackState playbackState = this.mSession.mPlaybackState;
                if (playbackState != null) {
                    actions = playbackState.getActions();
                } else {
                    actions = 0;
                }
                int keyCode = keyEvent.getKeyCode();
                if (keyCode == 79 || keyCode == 85) {
                    if (keyEvent.getRepeatCount() > 0) {
                        handleMediaPlayPauseKeySingleTapIfPending();
                    } else if (this.mMediaPlayPauseKeyPending) {
                        this.mHandler.removeMessages(23);
                        this.mMediaPlayPauseKeyPending = false;
                        if ((actions & 32) != 0) {
                            onSkipToNext();
                        }
                    } else {
                        this.mMediaPlayPauseKeyPending = true;
                        this.mSession.dispatchMediaButtonDelayed(this.mSession.getCurrentControllerInfo(), intent, ViewConfiguration.getDoubleTapTimeout());
                    }
                    return true;
                }
                handleMediaPlayPauseKeySingleTapIfPending();
                int keyCode2 = keyEvent.getKeyCode();
                switch (keyCode2) {
                    case 86:
                        if ((actions & 1) != 0) {
                            onStop();
                            return true;
                        }
                        break;
                    case 87:
                        if ((actions & 32) != 0) {
                            onSkipToNext();
                            return true;
                        }
                        break;
                    case 88:
                        if ((actions & 16) != 0) {
                            onSkipToPrevious();
                            return true;
                        }
                        break;
                    case 89:
                        if ((actions & 8) != 0) {
                            onRewind();
                            return true;
                        }
                        break;
                    case 90:
                        if ((actions & 64) != 0) {
                            onFastForward();
                            return true;
                        }
                        break;
                    default:
                        switch (keyCode2) {
                            case 126:
                                if ((actions & 4) != 0) {
                                    onPlay();
                                    return true;
                                }
                                break;
                            case 127:
                                if ((actions & 2) != 0) {
                                    onPause();
                                    return true;
                                }
                                break;
                        }
                        break;
                }
            }
            return false;
        }

        private void handleMediaPlayPauseKeySingleTapIfPending() {
            long actions;
            if (!this.mMediaPlayPauseKeyPending) {
                return;
            }
            boolean z = false;
            this.mMediaPlayPauseKeyPending = false;
            this.mHandler.removeMessages(23);
            PlaybackState playbackState = this.mSession.mPlaybackState;
            if (playbackState != null) {
                actions = playbackState.getActions();
            } else {
                actions = 0;
            }
            boolean z2 = playbackState != null && playbackState.getState() == 3;
            boolean z3 = (516 & actions) != 0;
            if ((actions & 514) != 0) {
                z = true;
            }
            if (z2 && z) {
                onPause();
            } else if (!z2 && z3) {
                onPlay();
            }
        }

        public void onPrepare() {
        }

        public void onPrepareFromMediaId(String str, Bundle bundle) {
        }

        public void onPrepareFromSearch(String str, Bundle bundle) {
        }

        public void onPrepareFromUri(Uri uri, Bundle bundle) {
        }

        public void onPlay() {
        }

        public void onPlayFromSearch(String str, Bundle bundle) {
        }

        public void onPlayFromMediaId(String str, Bundle bundle) {
        }

        public void onPlayFromUri(Uri uri, Bundle bundle) {
        }

        public void onSkipToQueueItem(long j) {
        }

        public void onPause() {
        }

        public void onSkipToNext() {
        }

        public void onSkipToPrevious() {
        }

        public void onFastForward() {
        }

        public void onRewind() {
        }

        public void onStop() {
        }

        public void onSeekTo(long j) {
        }

        public void onSetRating(Rating rating) {
        }

        public void onCustomAction(String str, Bundle bundle) {
        }
    }

    public static class CallbackStub extends ISessionCallback.Stub {
        private WeakReference<MediaSession> mMediaSession;

        public CallbackStub(MediaSession mediaSession) {
            this.mMediaSession = new WeakReference<>(mediaSession);
        }

        private static MediaSessionManager.RemoteUserInfo createRemoteUserInfo(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) {
            return new MediaSessionManager.RemoteUserInfo(str, i, i2, iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
        }

        @Override
        public void onCommand(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle, ResultReceiver resultReceiver) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchCommand(createRemoteUserInfo(str, i, i2, iSessionControllerCallback), str2, bundle, resultReceiver);
            }
        }

        @Override
        public void onMediaButton(String str, int i, int i2, Intent intent, int i3, ResultReceiver resultReceiver) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                try {
                    mediaSession.dispatchMediaButton(createRemoteUserInfo(str, i, i2, null), intent);
                } finally {
                    if (resultReceiver != null) {
                        resultReceiver.send(i3, null);
                    }
                }
            }
        }

        @Override
        public void onMediaButtonFromController(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, Intent intent) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchMediaButton(createRemoteUserInfo(str, i, i2, iSessionControllerCallback), intent);
            }
        }

        @Override
        public void onPrepare(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchPrepare(createRemoteUserInfo(str, i, i2, iSessionControllerCallback));
            }
        }

        @Override
        public void onPrepareFromMediaId(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchPrepareFromMediaId(createRemoteUserInfo(str, i, i2, iSessionControllerCallback), str2, bundle);
            }
        }

        @Override
        public void onPrepareFromSearch(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchPrepareFromSearch(createRemoteUserInfo(str, i, i2, iSessionControllerCallback), str2, bundle);
            }
        }

        @Override
        public void onPrepareFromUri(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, Uri uri, Bundle bundle) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchPrepareFromUri(createRemoteUserInfo(str, i, i2, iSessionControllerCallback), uri, bundle);
            }
        }

        @Override
        public void onPlay(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchPlay(createRemoteUserInfo(str, i, i2, iSessionControllerCallback));
            }
        }

        @Override
        public void onPlayFromMediaId(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchPlayFromMediaId(createRemoteUserInfo(str, i, i2, iSessionControllerCallback), str2, bundle);
            }
        }

        @Override
        public void onPlayFromSearch(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchPlayFromSearch(createRemoteUserInfo(str, i, i2, iSessionControllerCallback), str2, bundle);
            }
        }

        @Override
        public void onPlayFromUri(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, Uri uri, Bundle bundle) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchPlayFromUri(createRemoteUserInfo(str, i, i2, iSessionControllerCallback), uri, bundle);
            }
        }

        @Override
        public void onSkipToTrack(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, long j) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchSkipToItem(createRemoteUserInfo(str, i, i2, iSessionControllerCallback), j);
            }
        }

        @Override
        public void onPause(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchPause(createRemoteUserInfo(str, i, i2, iSessionControllerCallback));
            }
        }

        @Override
        public void onStop(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchStop(createRemoteUserInfo(str, i, i2, iSessionControllerCallback));
            }
        }

        @Override
        public void onNext(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchNext(createRemoteUserInfo(str, i, i2, iSessionControllerCallback));
            }
        }

        @Override
        public void onPrevious(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchPrevious(createRemoteUserInfo(str, i, i2, iSessionControllerCallback));
            }
        }

        @Override
        public void onFastForward(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchFastForward(createRemoteUserInfo(str, i, i2, iSessionControllerCallback));
            }
        }

        @Override
        public void onRewind(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchRewind(createRemoteUserInfo(str, i, i2, iSessionControllerCallback));
            }
        }

        @Override
        public void onSeekTo(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, long j) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchSeekTo(createRemoteUserInfo(str, i, i2, iSessionControllerCallback), j);
            }
        }

        @Override
        public void onRate(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, Rating rating) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchRate(createRemoteUserInfo(str, i, i2, iSessionControllerCallback), rating);
            }
        }

        @Override
        public void onCustomAction(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchCustomAction(createRemoteUserInfo(str, i, i2, iSessionControllerCallback), str2, bundle);
            }
        }

        @Override
        public void onAdjustVolume(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, int i3) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchAdjustVolume(createRemoteUserInfo(str, i, i2, iSessionControllerCallback), i3);
            }
        }

        @Override
        public void onSetVolumeTo(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, int i3) {
            MediaSession mediaSession = this.mMediaSession.get();
            if (mediaSession != null) {
                mediaSession.dispatchSetVolumeTo(createRemoteUserInfo(str, i, i2, iSessionControllerCallback), i3);
            }
        }
    }

    public static final class QueueItem implements Parcelable {
        public static final Parcelable.Creator<QueueItem> CREATOR = new Parcelable.Creator<QueueItem>() {
            @Override
            public QueueItem createFromParcel(Parcel parcel) {
                return new QueueItem(parcel);
            }

            @Override
            public QueueItem[] newArray(int i) {
                return new QueueItem[i];
            }
        };
        public static final int UNKNOWN_ID = -1;
        private final MediaDescription mDescription;
        private final long mId;

        public QueueItem(MediaDescription mediaDescription, long j) {
            if (mediaDescription == null) {
                throw new IllegalArgumentException("Description cannot be null.");
            }
            if (j == -1) {
                throw new IllegalArgumentException("Id cannot be QueueItem.UNKNOWN_ID");
            }
            this.mDescription = mediaDescription;
            this.mId = j;
        }

        private QueueItem(Parcel parcel) {
            this.mDescription = MediaDescription.CREATOR.createFromParcel(parcel);
            this.mId = parcel.readLong();
        }

        public MediaDescription getDescription() {
            return this.mDescription;
        }

        public long getQueueId() {
            return this.mId;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            this.mDescription.writeToParcel(parcel, i);
            parcel.writeLong(this.mId);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "MediaSession.QueueItem {Description=" + this.mDescription + ", Id=" + this.mId + " }";
        }

        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof QueueItem)) {
                return false;
            }
            QueueItem queueItem = (QueueItem) obj;
            if (this.mId != queueItem.mId || !Objects.equals(this.mDescription, queueItem.mDescription)) {
                return false;
            }
            return true;
        }
    }

    private static final class Command {
        public final String command;
        public final Bundle extras;
        public final ResultReceiver stub;

        public Command(String str, Bundle bundle, ResultReceiver resultReceiver) {
            this.command = str;
            this.extras = bundle;
            this.stub = resultReceiver;
        }
    }

    private class CallbackMessageHandler extends Handler {
        private static final int MSG_ADJUST_VOLUME = 21;
        private static final int MSG_COMMAND = 1;
        private static final int MSG_CUSTOM_ACTION = 20;
        private static final int MSG_FAST_FORWARD = 16;
        private static final int MSG_MEDIA_BUTTON = 2;
        private static final int MSG_NEXT = 14;
        private static final int MSG_PAUSE = 12;
        private static final int MSG_PLAY = 7;
        private static final int MSG_PLAY_MEDIA_ID = 8;
        private static final int MSG_PLAY_PAUSE_KEY_DOUBLE_TAP_TIMEOUT = 23;
        private static final int MSG_PLAY_SEARCH = 9;
        private static final int MSG_PLAY_URI = 10;
        private static final int MSG_PREPARE = 3;
        private static final int MSG_PREPARE_MEDIA_ID = 4;
        private static final int MSG_PREPARE_SEARCH = 5;
        private static final int MSG_PREPARE_URI = 6;
        private static final int MSG_PREVIOUS = 15;
        private static final int MSG_RATE = 19;
        private static final int MSG_REWIND = 17;
        private static final int MSG_SEEK_TO = 18;
        private static final int MSG_SET_VOLUME = 22;
        private static final int MSG_SKIP_TO_ITEM = 11;
        private static final int MSG_STOP = 13;
        private Callback mCallback;
        private MediaSessionManager.RemoteUserInfo mCurrentControllerInfo;

        public CallbackMessageHandler(Looper looper, Callback callback) {
            super(looper, null, true);
            this.mCallback = callback;
            this.mCallback.mHandler = this;
        }

        public void post(MediaSessionManager.RemoteUserInfo remoteUserInfo, int i, Object obj, Bundle bundle, long j) {
            Message messageObtainMessage = obtainMessage(i, Pair.create(remoteUserInfo, obj));
            messageObtainMessage.setData(bundle);
            if (j > 0) {
                sendMessageDelayed(messageObtainMessage, j);
            } else {
                sendMessage(messageObtainMessage);
            }
        }

        @Override
        public void handleMessage(Message message) {
            VolumeProvider volumeProvider;
            VolumeProvider volumeProvider2;
            this.mCurrentControllerInfo = (MediaSessionManager.RemoteUserInfo) ((Pair) message.obj).first;
            S s = ((Pair) message.obj).second;
            switch (message.what) {
                case 1:
                    Command command = (Command) s;
                    this.mCallback.onCommand(command.command, command.extras, command.stub);
                    break;
                case 2:
                    this.mCallback.onMediaButtonEvent((Intent) s);
                    break;
                case 3:
                    this.mCallback.onPrepare();
                    break;
                case 4:
                    this.mCallback.onPrepareFromMediaId((String) s, message.getData());
                    break;
                case 5:
                    this.mCallback.onPrepareFromSearch((String) s, message.getData());
                    break;
                case 6:
                    this.mCallback.onPrepareFromUri((Uri) s, message.getData());
                    break;
                case 7:
                    this.mCallback.onPlay();
                    break;
                case 8:
                    this.mCallback.onPlayFromMediaId((String) s, message.getData());
                    break;
                case 9:
                    this.mCallback.onPlayFromSearch((String) s, message.getData());
                    break;
                case 10:
                    this.mCallback.onPlayFromUri((Uri) s, message.getData());
                    break;
                case 11:
                    this.mCallback.onSkipToQueueItem(((Long) s).longValue());
                    break;
                case 12:
                    this.mCallback.onPause();
                    break;
                case 13:
                    this.mCallback.onStop();
                    break;
                case 14:
                    this.mCallback.onSkipToNext();
                    break;
                case 15:
                    this.mCallback.onSkipToPrevious();
                    break;
                case 16:
                    this.mCallback.onFastForward();
                    break;
                case 17:
                    this.mCallback.onRewind();
                    break;
                case 18:
                    this.mCallback.onSeekTo(((Long) s).longValue());
                    break;
                case 19:
                    this.mCallback.onSetRating((Rating) s);
                    break;
                case 20:
                    this.mCallback.onCustomAction((String) s, message.getData());
                    break;
                case 21:
                    synchronized (MediaSession.this.mLock) {
                        volumeProvider = MediaSession.this.mVolumeProvider;
                        break;
                    }
                    if (volumeProvider != null) {
                        volumeProvider.onAdjustVolume(((Integer) s).intValue());
                    }
                    break;
                case 22:
                    synchronized (MediaSession.this.mLock) {
                        volumeProvider2 = MediaSession.this.mVolumeProvider;
                        break;
                    }
                    if (volumeProvider2 != null) {
                        volumeProvider2.onSetVolumeTo(((Integer) s).intValue());
                    }
                    break;
                case 23:
                    this.mCallback.handleMediaPlayPauseKeySingleTapIfPending();
                    break;
            }
            this.mCurrentControllerInfo = null;
        }
    }
}
