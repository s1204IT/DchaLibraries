package android.support.v4.media.session;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.Rating;
import android.media.RemoteControlClient;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.BadParcelableException;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.v4.app.BundleCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.MediaSessionManager;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.SessionToken2;
import android.support.v4.media.VolumeProviderCompat;
import android.support.v4.media.session.IMediaSession;
import android.support.v4.media.session.MediaSessionCompatApi21;
import android.support.v4.media.session.MediaSessionCompatApi23;
import android.support.v4.media.session.MediaSessionCompatApi24;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import com.android.launcher3.InstallShortcutReceiver;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
/* loaded from: classes.dex */
public class MediaSessionCompat {
    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public static final String ACTION_ARGUMENT_CAPTIONING_ENABLED = "android.support.v4.media.session.action.ARGUMENT_CAPTIONING_ENABLED";
    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public static final String ACTION_ARGUMENT_EXTRAS = "android.support.v4.media.session.action.ARGUMENT_EXTRAS";
    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public static final String ACTION_ARGUMENT_MEDIA_ID = "android.support.v4.media.session.action.ARGUMENT_MEDIA_ID";
    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public static final String ACTION_ARGUMENT_QUERY = "android.support.v4.media.session.action.ARGUMENT_QUERY";
    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public static final String ACTION_ARGUMENT_RATING = "android.support.v4.media.session.action.ARGUMENT_RATING";
    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public static final String ACTION_ARGUMENT_REPEAT_MODE = "android.support.v4.media.session.action.ARGUMENT_REPEAT_MODE";
    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public static final String ACTION_ARGUMENT_SHUFFLE_MODE = "android.support.v4.media.session.action.ARGUMENT_SHUFFLE_MODE";
    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public static final String ACTION_ARGUMENT_URI = "android.support.v4.media.session.action.ARGUMENT_URI";
    public static final String ACTION_FLAG_AS_INAPPROPRIATE = "android.support.v4.media.session.action.FLAG_AS_INAPPROPRIATE";
    public static final String ACTION_FOLLOW = "android.support.v4.media.session.action.FOLLOW";
    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public static final String ACTION_PLAY_FROM_URI = "android.support.v4.media.session.action.PLAY_FROM_URI";
    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public static final String ACTION_PREPARE = "android.support.v4.media.session.action.PREPARE";
    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public static final String ACTION_PREPARE_FROM_MEDIA_ID = "android.support.v4.media.session.action.PREPARE_FROM_MEDIA_ID";
    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public static final String ACTION_PREPARE_FROM_SEARCH = "android.support.v4.media.session.action.PREPARE_FROM_SEARCH";
    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public static final String ACTION_PREPARE_FROM_URI = "android.support.v4.media.session.action.PREPARE_FROM_URI";
    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public static final String ACTION_SET_CAPTIONING_ENABLED = "android.support.v4.media.session.action.SET_CAPTIONING_ENABLED";
    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public static final String ACTION_SET_RATING = "android.support.v4.media.session.action.SET_RATING";
    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public static final String ACTION_SET_REPEAT_MODE = "android.support.v4.media.session.action.SET_REPEAT_MODE";
    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public static final String ACTION_SET_SHUFFLE_MODE = "android.support.v4.media.session.action.SET_SHUFFLE_MODE";
    public static final String ACTION_SKIP_AD = "android.support.v4.media.session.action.SKIP_AD";
    public static final String ACTION_UNFOLLOW = "android.support.v4.media.session.action.UNFOLLOW";
    public static final String ARGUMENT_MEDIA_ATTRIBUTE = "android.support.v4.media.session.ARGUMENT_MEDIA_ATTRIBUTE";
    public static final String ARGUMENT_MEDIA_ATTRIBUTE_VALUE = "android.support.v4.media.session.ARGUMENT_MEDIA_ATTRIBUTE_VALUE";
    private static final String DATA_CALLING_PACKAGE = "data_calling_pkg";
    private static final String DATA_CALLING_PID = "data_calling_pid";
    private static final String DATA_CALLING_UID = "data_calling_uid";
    private static final String DATA_EXTRAS = "data_extras";
    public static final int FLAG_HANDLES_MEDIA_BUTTONS = 1;
    public static final int FLAG_HANDLES_QUEUE_COMMANDS = 4;
    public static final int FLAG_HANDLES_TRANSPORT_CONTROLS = 2;
    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public static final String KEY_EXTRA_BINDER = "android.support.v4.media.session.EXTRA_BINDER";
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    public static final String KEY_SESSION_TOKEN2 = "android.support.v4.media.session.SESSION_TOKEN2";
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    public static final String KEY_TOKEN = "android.support.v4.media.session.TOKEN";
    private static final int MAX_BITMAP_SIZE_IN_DP = 320;
    public static final int MEDIA_ATTRIBUTE_ALBUM = 1;
    public static final int MEDIA_ATTRIBUTE_ARTIST = 0;
    public static final int MEDIA_ATTRIBUTE_PLAYLIST = 2;
    static final String TAG = "MediaSessionCompat";
    static int sMaxBitmapSize;
    private final ArrayList<OnActiveChangeListener> mActiveListeners;
    private final MediaControllerCompat mController;
    private final MediaSessionImpl mImpl;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface MediaSessionImpl {
        String getCallingPackage();

        MediaSessionManager.RemoteUserInfo getCurrentControllerInfo();

        Object getMediaSession();

        PlaybackStateCompat getPlaybackState();

        Object getRemoteControlClient();

        Token getSessionToken();

        boolean isActive();

        void release();

        void sendSessionEvent(String str, Bundle bundle);

        void setActive(boolean z);

        void setCallback(Callback callback, Handler handler);

        void setCaptioningEnabled(boolean z);

        void setExtras(Bundle bundle);

        void setFlags(int i);

        void setMediaButtonReceiver(PendingIntent pendingIntent);

        void setMetadata(MediaMetadataCompat mediaMetadataCompat);

        void setPlaybackState(PlaybackStateCompat playbackStateCompat);

        void setPlaybackToLocal(int i);

        void setPlaybackToRemote(VolumeProviderCompat volumeProviderCompat);

        void setQueue(List<QueueItem> list);

        void setQueueTitle(CharSequence charSequence);

        void setRatingType(int i);

        void setRepeatMode(int i);

        void setSessionActivity(PendingIntent pendingIntent);

        void setShuffleMode(int i);
    }

    /* loaded from: classes.dex */
    public interface OnActiveChangeListener {
        void onActiveChanged();
    }

    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    /* loaded from: classes.dex */
    public @interface SessionFlags {
    }

    public MediaSessionCompat(Context context, String tag) {
        this(context, tag, null, null);
    }

    public MediaSessionCompat(Context context, String tag, ComponentName mbrComponent, PendingIntent mbrIntent) {
        this(context, tag, mbrComponent, mbrIntent, null);
    }

    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    public MediaSessionCompat(Context context, String tag, SessionToken2 token2) {
        this(context, tag, null, null, token2);
    }

    private MediaSessionCompat(Context context, String tag, ComponentName mbrComponent, PendingIntent mbrIntent, SessionToken2 token2) {
        this.mActiveListeners = new ArrayList<>();
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (TextUtils.isEmpty(tag)) {
            throw new IllegalArgumentException("tag must not be null or empty");
        }
        if (mbrComponent == null && (mbrComponent = MediaButtonReceiver.getMediaButtonReceiverComponent(context)) == null) {
            Log.w(TAG, "Couldn't find a unique registered media button receiver in the given context.");
        }
        if (mbrComponent != null && mbrIntent == null) {
            Intent mediaButtonIntent = new Intent("android.intent.action.MEDIA_BUTTON");
            mediaButtonIntent.setComponent(mbrComponent);
            mbrIntent = PendingIntent.getBroadcast(context, 0, mediaButtonIntent, 0);
        }
        if (Build.VERSION.SDK_INT >= 28) {
            this.mImpl = new MediaSessionImplApi28(context, tag, token2);
            setCallback(new Callback() { // from class: android.support.v4.media.session.MediaSessionCompat.1
            });
            this.mImpl.setMediaButtonReceiver(mbrIntent);
        } else if (Build.VERSION.SDK_INT >= 21) {
            this.mImpl = new MediaSessionImplApi21(context, tag, token2);
            setCallback(new Callback() { // from class: android.support.v4.media.session.MediaSessionCompat.2
            });
            this.mImpl.setMediaButtonReceiver(mbrIntent);
        } else if (Build.VERSION.SDK_INT >= 19) {
            this.mImpl = new MediaSessionImplApi19(context, tag, mbrComponent, mbrIntent);
        } else if (Build.VERSION.SDK_INT >= 18) {
            this.mImpl = new MediaSessionImplApi18(context, tag, mbrComponent, mbrIntent);
        } else {
            this.mImpl = new MediaSessionImplBase(context, tag, mbrComponent, mbrIntent);
        }
        this.mController = new MediaControllerCompat(context, this);
        if (sMaxBitmapSize == 0) {
            sMaxBitmapSize = (int) (TypedValue.applyDimension(1, 320.0f, context.getResources().getDisplayMetrics()) + 0.5f);
        }
    }

    private MediaSessionCompat(Context context, MediaSessionImpl impl) {
        this.mActiveListeners = new ArrayList<>();
        this.mImpl = impl;
        if (Build.VERSION.SDK_INT >= 21 && !MediaSessionCompatApi21.hasCallback(impl.getMediaSession())) {
            setCallback(new Callback() { // from class: android.support.v4.media.session.MediaSessionCompat.3
            });
        }
        this.mController = new MediaControllerCompat(context, this);
    }

    public void setCallback(Callback callback) {
        setCallback(callback, null);
    }

    public void setCallback(Callback callback, Handler handler) {
        this.mImpl.setCallback(callback, handler != null ? handler : new Handler());
    }

    public void setSessionActivity(PendingIntent pi) {
        this.mImpl.setSessionActivity(pi);
    }

    public void setMediaButtonReceiver(PendingIntent mbr) {
        this.mImpl.setMediaButtonReceiver(mbr);
    }

    public void setFlags(int flags) {
        this.mImpl.setFlags(flags);
    }

    public void setPlaybackToLocal(int stream) {
        this.mImpl.setPlaybackToLocal(stream);
    }

    public void setPlaybackToRemote(VolumeProviderCompat volumeProvider) {
        if (volumeProvider == null) {
            throw new IllegalArgumentException("volumeProvider may not be null!");
        }
        this.mImpl.setPlaybackToRemote(volumeProvider);
    }

    public void setActive(boolean active) {
        this.mImpl.setActive(active);
        Iterator<OnActiveChangeListener> it = this.mActiveListeners.iterator();
        while (it.hasNext()) {
            OnActiveChangeListener listener = it.next();
            listener.onActiveChanged();
        }
    }

    public boolean isActive() {
        return this.mImpl.isActive();
    }

    public void sendSessionEvent(String event, Bundle extras) {
        if (TextUtils.isEmpty(event)) {
            throw new IllegalArgumentException("event cannot be null or empty");
        }
        this.mImpl.sendSessionEvent(event, extras);
    }

    public void release() {
        this.mImpl.release();
    }

    public Token getSessionToken() {
        return this.mImpl.getSessionToken();
    }

    public MediaControllerCompat getController() {
        return this.mController;
    }

    public void setPlaybackState(PlaybackStateCompat state) {
        this.mImpl.setPlaybackState(state);
    }

    public void setMetadata(MediaMetadataCompat metadata) {
        this.mImpl.setMetadata(metadata);
    }

    public void setQueue(List<QueueItem> queue) {
        this.mImpl.setQueue(queue);
    }

    public void setQueueTitle(CharSequence title) {
        this.mImpl.setQueueTitle(title);
    }

    public void setRatingType(int type) {
        this.mImpl.setRatingType(type);
    }

    public void setCaptioningEnabled(boolean enabled) {
        this.mImpl.setCaptioningEnabled(enabled);
    }

    public void setRepeatMode(int repeatMode) {
        this.mImpl.setRepeatMode(repeatMode);
    }

    public void setShuffleMode(int shuffleMode) {
        this.mImpl.setShuffleMode(shuffleMode);
    }

    public void setExtras(Bundle extras) {
        this.mImpl.setExtras(extras);
    }

    public Object getMediaSession() {
        return this.mImpl.getMediaSession();
    }

    public Object getRemoteControlClient() {
        return this.mImpl.getRemoteControlClient();
    }

    @NonNull
    public final MediaSessionManager.RemoteUserInfo getCurrentControllerInfo() {
        return this.mImpl.getCurrentControllerInfo();
    }

    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    public String getCallingPackage() {
        return this.mImpl.getCallingPackage();
    }

    public void addOnActiveChangeListener(OnActiveChangeListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener may not be null");
        }
        this.mActiveListeners.add(listener);
    }

    public void removeOnActiveChangeListener(OnActiveChangeListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener may not be null");
        }
        this.mActiveListeners.remove(listener);
    }

    public static MediaSessionCompat fromMediaSession(Context context, Object mediaSession) {
        if (context != null && mediaSession != null && Build.VERSION.SDK_INT >= 21) {
            return new MediaSessionCompat(context, new MediaSessionImplApi21(mediaSession));
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static PlaybackStateCompat getStateWithUpdatedPosition(PlaybackStateCompat state, MediaMetadataCompat metadata) {
        long position;
        if (state == null || state.getPosition() == -1) {
            return state;
        }
        if (state.getState() == 3 || state.getState() == 4 || state.getState() == 5) {
            long updateTime = state.getLastPositionUpdateTime();
            if (updateTime > 0) {
                long currentTime = SystemClock.elapsedRealtime();
                long position2 = (state.getPlaybackSpeed() * ((float) (currentTime - updateTime))) + state.getPosition();
                long duration = -1;
                if (metadata != null && metadata.containsKey("android.media.metadata.DURATION")) {
                    duration = metadata.getLong("android.media.metadata.DURATION");
                }
                long duration2 = duration;
                if (duration2 >= 0 && position2 > duration2) {
                    position = duration2;
                } else if (position2 < 0) {
                    position = 0;
                } else {
                    position = position2;
                }
                return new PlaybackStateCompat.Builder(state).setState(state.getState(), position, state.getPlaybackSpeed(), currentTime).build();
            }
        }
        return state;
    }

    /* loaded from: classes.dex */
    public static abstract class Callback {
        private CallbackHandler mCallbackHandler = null;
        final Object mCallbackObj;
        private boolean mMediaPlayPauseKeyPending;
        private WeakReference<MediaSessionImpl> mSessionImpl;

        public Callback() {
            if (Build.VERSION.SDK_INT >= 24) {
                this.mCallbackObj = MediaSessionCompatApi24.createCallback(new StubApi24());
            } else if (Build.VERSION.SDK_INT >= 23) {
                this.mCallbackObj = MediaSessionCompatApi23.createCallback(new StubApi23());
            } else if (Build.VERSION.SDK_INT >= 21) {
                this.mCallbackObj = MediaSessionCompatApi21.createCallback(new StubApi21());
            } else {
                this.mCallbackObj = null;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setSessionImpl(MediaSessionImpl impl, Handler handler) {
            this.mSessionImpl = new WeakReference<>(impl);
            if (this.mCallbackHandler != null) {
                this.mCallbackHandler.removeCallbacksAndMessages(null);
            }
            this.mCallbackHandler = new CallbackHandler(handler.getLooper());
        }

        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
        }

        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            KeyEvent keyEvent;
            MediaSessionImpl impl = this.mSessionImpl.get();
            if (impl == null || this.mCallbackHandler == null || (keyEvent = (KeyEvent) mediaButtonEvent.getParcelableExtra("android.intent.extra.KEY_EVENT")) == null || keyEvent.getAction() != 0) {
                return false;
            }
            int keyCode = keyEvent.getKeyCode();
            if (keyCode == 79 || keyCode == 85) {
                if (keyEvent.getRepeatCount() > 0) {
                    handleMediaPlayPauseKeySingleTapIfPending();
                } else if (this.mMediaPlayPauseKeyPending) {
                    this.mCallbackHandler.removeMessages(1);
                    this.mMediaPlayPauseKeyPending = false;
                    PlaybackStateCompat state = impl.getPlaybackState();
                    long validActions = state == null ? 0L : state.getActions();
                    if ((32 & validActions) != 0) {
                        onSkipToNext();
                    }
                } else {
                    this.mMediaPlayPauseKeyPending = true;
                    this.mCallbackHandler.sendEmptyMessageDelayed(1, ViewConfiguration.getDoubleTapTimeout());
                }
                return true;
            }
            handleMediaPlayPauseKeySingleTapIfPending();
            return false;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void handleMediaPlayPauseKeySingleTapIfPending() {
            if (!this.mMediaPlayPauseKeyPending) {
                return;
            }
            this.mMediaPlayPauseKeyPending = false;
            this.mCallbackHandler.removeMessages(1);
            MediaSessionImpl impl = this.mSessionImpl.get();
            if (impl == null) {
                return;
            }
            PlaybackStateCompat state = impl.getPlaybackState();
            long validActions = state == null ? 0L : state.getActions();
            boolean isPlaying = state != null && state.getState() == 3;
            boolean canPlay = (516 & validActions) != 0;
            boolean canPause = (514 & validActions) != 0;
            if (isPlaying && canPause) {
                onPause();
            } else if (!isPlaying && canPlay) {
                onPlay();
            }
        }

        public void onPrepare() {
        }

        public void onPrepareFromMediaId(String mediaId, Bundle extras) {
        }

        public void onPrepareFromSearch(String query, Bundle extras) {
        }

        public void onPrepareFromUri(Uri uri, Bundle extras) {
        }

        public void onPlay() {
        }

        public void onPlayFromMediaId(String mediaId, Bundle extras) {
        }

        public void onPlayFromSearch(String query, Bundle extras) {
        }

        public void onPlayFromUri(Uri uri, Bundle extras) {
        }

        public void onSkipToQueueItem(long id) {
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

        public void onSeekTo(long pos) {
        }

        public void onSetRating(RatingCompat rating) {
        }

        public void onSetRating(RatingCompat rating, Bundle extras) {
        }

        public void onSetCaptioningEnabled(boolean enabled) {
        }

        public void onSetRepeatMode(int repeatMode) {
        }

        public void onSetShuffleMode(int shuffleMode) {
        }

        public void onCustomAction(String action, Bundle extras) {
        }

        public void onAddQueueItem(MediaDescriptionCompat description) {
        }

        public void onAddQueueItem(MediaDescriptionCompat description, int index) {
        }

        public void onRemoveQueueItem(MediaDescriptionCompat description) {
        }

        @Deprecated
        public void onRemoveQueueItemAt(int index) {
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public class CallbackHandler extends Handler {
            private static final int MSG_MEDIA_PLAY_PAUSE_KEY_DOUBLE_TAP_TIMEOUT = 1;

            CallbackHandler(Looper looper) {
                super(looper);
            }

            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    Callback.this.handleMediaPlayPauseKeySingleTapIfPending();
                }
            }
        }

        @RequiresApi(21)
        /* loaded from: classes.dex */
        private class StubApi21 implements MediaSessionCompatApi21.Callback {
            StubApi21() {
            }

            @Override // android.support.v4.media.session.MediaSessionCompatApi21.Callback
            public void onCommand(String command, Bundle extras, ResultReceiver cb) {
                try {
                    QueueItem item = null;
                    Bundle bundle = null;
                    item = null;
                    if (command.equals(MediaControllerCompat.COMMAND_GET_EXTRA_BINDER)) {
                        MediaSessionImplApi21 impl = (MediaSessionImplApi21) Callback.this.mSessionImpl.get();
                        if (impl != null) {
                            Bundle result = new Bundle();
                            Token token = impl.getSessionToken();
                            IMediaSession extraBinder = token.getExtraBinder();
                            BundleCompat.putBinder(result, MediaSessionCompat.KEY_EXTRA_BINDER, extraBinder == null ? null : extraBinder.asBinder());
                            SessionToken2 token2 = token.getSessionToken2();
                            if (token2 != null) {
                                bundle = token2.toBundle();
                            }
                            result.putBundle(MediaSessionCompat.KEY_SESSION_TOKEN2, bundle);
                            cb.send(0, result);
                        }
                    } else if (command.equals(MediaControllerCompat.COMMAND_ADD_QUEUE_ITEM)) {
                        extras.setClassLoader(MediaDescriptionCompat.class.getClassLoader());
                        Callback.this.onAddQueueItem((MediaDescriptionCompat) extras.getParcelable(MediaControllerCompat.COMMAND_ARGUMENT_MEDIA_DESCRIPTION));
                    } else if (command.equals(MediaControllerCompat.COMMAND_ADD_QUEUE_ITEM_AT)) {
                        extras.setClassLoader(MediaDescriptionCompat.class.getClassLoader());
                        Callback.this.onAddQueueItem((MediaDescriptionCompat) extras.getParcelable(MediaControllerCompat.COMMAND_ARGUMENT_MEDIA_DESCRIPTION), extras.getInt(MediaControllerCompat.COMMAND_ARGUMENT_INDEX));
                    } else if (command.equals(MediaControllerCompat.COMMAND_REMOVE_QUEUE_ITEM)) {
                        extras.setClassLoader(MediaDescriptionCompat.class.getClassLoader());
                        Callback.this.onRemoveQueueItem((MediaDescriptionCompat) extras.getParcelable(MediaControllerCompat.COMMAND_ARGUMENT_MEDIA_DESCRIPTION));
                    } else if (command.equals(MediaControllerCompat.COMMAND_REMOVE_QUEUE_ITEM_AT)) {
                        MediaSessionImplApi21 impl2 = (MediaSessionImplApi21) Callback.this.mSessionImpl.get();
                        if (impl2 != null && impl2.mQueue != null) {
                            int index = extras.getInt(MediaControllerCompat.COMMAND_ARGUMENT_INDEX, -1);
                            if (index >= 0 && index < impl2.mQueue.size()) {
                                item = (QueueItem) impl2.mQueue.get(index);
                            }
                            if (item != null) {
                                Callback.this.onRemoveQueueItem(item.getDescription());
                            }
                        }
                    } else {
                        Callback.this.onCommand(command, extras, cb);
                    }
                } catch (BadParcelableException e) {
                    Log.e(MediaSessionCompat.TAG, "Could not unparcel the extra data.");
                }
            }

            @Override // android.support.v4.media.session.MediaSessionCompatApi21.Callback
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                return Callback.this.onMediaButtonEvent(mediaButtonIntent);
            }

            @Override // android.support.v4.media.session.MediaSessionCompatApi21.Callback
            public void onPlay() {
                Callback.this.onPlay();
            }

            @Override // android.support.v4.media.session.MediaSessionCompatApi21.Callback
            public void onPlayFromMediaId(String mediaId, Bundle extras) {
                Callback.this.onPlayFromMediaId(mediaId, extras);
            }

            @Override // android.support.v4.media.session.MediaSessionCompatApi21.Callback
            public void onPlayFromSearch(String search, Bundle extras) {
                Callback.this.onPlayFromSearch(search, extras);
            }

            @Override // android.support.v4.media.session.MediaSessionCompatApi21.Callback
            public void onSkipToQueueItem(long id) {
                Callback.this.onSkipToQueueItem(id);
            }

            @Override // android.support.v4.media.session.MediaSessionCompatApi21.Callback
            public void onPause() {
                Callback.this.onPause();
            }

            @Override // android.support.v4.media.session.MediaSessionCompatApi21.Callback
            public void onSkipToNext() {
                Callback.this.onSkipToNext();
            }

            @Override // android.support.v4.media.session.MediaSessionCompatApi21.Callback
            public void onSkipToPrevious() {
                Callback.this.onSkipToPrevious();
            }

            @Override // android.support.v4.media.session.MediaSessionCompatApi21.Callback
            public void onFastForward() {
                Callback.this.onFastForward();
            }

            @Override // android.support.v4.media.session.MediaSessionCompatApi21.Callback
            public void onRewind() {
                Callback.this.onRewind();
            }

            @Override // android.support.v4.media.session.MediaSessionCompatApi21.Callback
            public void onStop() {
                Callback.this.onStop();
            }

            @Override // android.support.v4.media.session.MediaSessionCompatApi21.Callback
            public void onSeekTo(long pos) {
                Callback.this.onSeekTo(pos);
            }

            @Override // android.support.v4.media.session.MediaSessionCompatApi21.Callback
            public void onSetRating(Object ratingObj) {
                Callback.this.onSetRating(RatingCompat.fromRating(ratingObj));
            }

            @Override // android.support.v4.media.session.MediaSessionCompatApi21.Callback
            public void onSetRating(Object ratingObj, Bundle extras) {
                Callback.this.onSetRating(RatingCompat.fromRating(ratingObj), extras);
            }

            @Override // android.support.v4.media.session.MediaSessionCompatApi21.Callback
            public void onCustomAction(String action, Bundle extras) {
                if (action.equals(MediaSessionCompat.ACTION_PLAY_FROM_URI)) {
                    Uri uri = (Uri) extras.getParcelable(MediaSessionCompat.ACTION_ARGUMENT_URI);
                    Bundle bundle = (Bundle) extras.getParcelable(MediaSessionCompat.ACTION_ARGUMENT_EXTRAS);
                    Callback.this.onPlayFromUri(uri, bundle);
                } else if (action.equals(MediaSessionCompat.ACTION_PREPARE)) {
                    Callback.this.onPrepare();
                } else if (action.equals(MediaSessionCompat.ACTION_PREPARE_FROM_MEDIA_ID)) {
                    String mediaId = extras.getString(MediaSessionCompat.ACTION_ARGUMENT_MEDIA_ID);
                    Bundle bundle2 = extras.getBundle(MediaSessionCompat.ACTION_ARGUMENT_EXTRAS);
                    Callback.this.onPrepareFromMediaId(mediaId, bundle2);
                } else if (action.equals(MediaSessionCompat.ACTION_PREPARE_FROM_SEARCH)) {
                    String query = extras.getString(MediaSessionCompat.ACTION_ARGUMENT_QUERY);
                    Bundle bundle3 = extras.getBundle(MediaSessionCompat.ACTION_ARGUMENT_EXTRAS);
                    Callback.this.onPrepareFromSearch(query, bundle3);
                } else if (action.equals(MediaSessionCompat.ACTION_PREPARE_FROM_URI)) {
                    Uri uri2 = (Uri) extras.getParcelable(MediaSessionCompat.ACTION_ARGUMENT_URI);
                    Bundle bundle4 = extras.getBundle(MediaSessionCompat.ACTION_ARGUMENT_EXTRAS);
                    Callback.this.onPrepareFromUri(uri2, bundle4);
                } else if (action.equals(MediaSessionCompat.ACTION_SET_CAPTIONING_ENABLED)) {
                    boolean enabled = extras.getBoolean(MediaSessionCompat.ACTION_ARGUMENT_CAPTIONING_ENABLED);
                    Callback.this.onSetCaptioningEnabled(enabled);
                } else if (action.equals(MediaSessionCompat.ACTION_SET_REPEAT_MODE)) {
                    int repeatMode = extras.getInt(MediaSessionCompat.ACTION_ARGUMENT_REPEAT_MODE);
                    Callback.this.onSetRepeatMode(repeatMode);
                } else if (action.equals(MediaSessionCompat.ACTION_SET_SHUFFLE_MODE)) {
                    int shuffleMode = extras.getInt(MediaSessionCompat.ACTION_ARGUMENT_SHUFFLE_MODE);
                    Callback.this.onSetShuffleMode(shuffleMode);
                } else if (action.equals(MediaSessionCompat.ACTION_SET_RATING)) {
                    extras.setClassLoader(RatingCompat.class.getClassLoader());
                    RatingCompat rating = (RatingCompat) extras.getParcelable(MediaSessionCompat.ACTION_ARGUMENT_RATING);
                    Bundle bundle5 = extras.getBundle(MediaSessionCompat.ACTION_ARGUMENT_EXTRAS);
                    Callback.this.onSetRating(rating, bundle5);
                } else {
                    Callback.this.onCustomAction(action, extras);
                }
            }
        }

        @RequiresApi(23)
        /* loaded from: classes.dex */
        private class StubApi23 extends StubApi21 implements MediaSessionCompatApi23.Callback {
            StubApi23() {
                super();
            }

            @Override // android.support.v4.media.session.MediaSessionCompatApi23.Callback
            public void onPlayFromUri(Uri uri, Bundle extras) {
                Callback.this.onPlayFromUri(uri, extras);
            }
        }

        @RequiresApi(24)
        /* loaded from: classes.dex */
        private class StubApi24 extends StubApi23 implements MediaSessionCompatApi24.Callback {
            StubApi24() {
                super();
            }

            @Override // android.support.v4.media.session.MediaSessionCompatApi24.Callback
            public void onPrepare() {
                Callback.this.onPrepare();
            }

            @Override // android.support.v4.media.session.MediaSessionCompatApi24.Callback
            public void onPrepareFromMediaId(String mediaId, Bundle extras) {
                Callback.this.onPrepareFromMediaId(mediaId, extras);
            }

            @Override // android.support.v4.media.session.MediaSessionCompatApi24.Callback
            public void onPrepareFromSearch(String query, Bundle extras) {
                Callback.this.onPrepareFromSearch(query, extras);
            }

            @Override // android.support.v4.media.session.MediaSessionCompatApi24.Callback
            public void onPrepareFromUri(Uri uri, Bundle extras) {
                Callback.this.onPrepareFromUri(uri, extras);
            }
        }
    }

    /* loaded from: classes.dex */
    public static final class Token implements Parcelable {
        public static final Parcelable.Creator<Token> CREATOR = new Parcelable.Creator<Token>() { // from class: android.support.v4.media.session.MediaSessionCompat.Token.1
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public Token createFromParcel(Parcel in) {
                Object inner;
                if (Build.VERSION.SDK_INT >= 21) {
                    inner = in.readParcelable(null);
                } else {
                    inner = in.readStrongBinder();
                }
                return new Token(inner);
            }

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public Token[] newArray(int size) {
                return new Token[size];
            }
        };
        private IMediaSession mExtraBinder;
        private final Object mInner;
        private SessionToken2 mSessionToken2;

        Token(Object inner) {
            this(inner, null, null);
        }

        Token(Object inner, IMediaSession extraBinder) {
            this(inner, extraBinder, null);
        }

        Token(Object inner, IMediaSession extraBinder, SessionToken2 token2) {
            this.mInner = inner;
            this.mExtraBinder = extraBinder;
            this.mSessionToken2 = token2;
        }

        public static Token fromToken(Object token) {
            return fromToken(token, null);
        }

        @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
        public static Token fromToken(Object token, IMediaSession extraBinder) {
            if (token != null && Build.VERSION.SDK_INT >= 21) {
                return new Token(MediaSessionCompatApi21.verifyToken(token), extraBinder);
            }
            return null;
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            if (Build.VERSION.SDK_INT >= 21) {
                dest.writeParcelable((Parcelable) this.mInner, flags);
            } else {
                dest.writeStrongBinder((IBinder) this.mInner);
            }
        }

        public int hashCode() {
            if (this.mInner == null) {
                return 0;
            }
            return this.mInner.hashCode();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof Token) {
                Token other = (Token) obj;
                if (this.mInner == null) {
                    return other.mInner == null;
                } else if (other.mInner == null) {
                    return false;
                } else {
                    return this.mInner.equals(other.mInner);
                }
            }
            return false;
        }

        public Object getToken() {
            return this.mInner;
        }

        @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
        public IMediaSession getExtraBinder() {
            return this.mExtraBinder;
        }

        @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
        public void setExtraBinder(IMediaSession extraBinder) {
            this.mExtraBinder = extraBinder;
        }

        @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
        public SessionToken2 getSessionToken2() {
            return this.mSessionToken2;
        }

        @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
        public void setSessionToken2(SessionToken2 token2) {
            this.mSessionToken2 = token2;
        }

        @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putParcelable(MediaSessionCompat.KEY_TOKEN, this);
            if (this.mExtraBinder != null) {
                BundleCompat.putBinder(bundle, MediaSessionCompat.KEY_EXTRA_BINDER, this.mExtraBinder.asBinder());
            }
            if (this.mSessionToken2 != null) {
                bundle.putBundle(MediaSessionCompat.KEY_SESSION_TOKEN2, this.mSessionToken2.toBundle());
            }
            return bundle;
        }

        @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
        public static Token fromBundle(Bundle tokenBundle) {
            if (tokenBundle == null) {
                return null;
            }
            IMediaSession extraSession = IMediaSession.Stub.asInterface(BundleCompat.getBinder(tokenBundle, MediaSessionCompat.KEY_EXTRA_BINDER));
            SessionToken2 token2 = SessionToken2.fromBundle(tokenBundle.getBundle(MediaSessionCompat.KEY_SESSION_TOKEN2));
            Token token = (Token) tokenBundle.getParcelable(MediaSessionCompat.KEY_TOKEN);
            if (token == null) {
                return null;
            }
            return new Token(token.mInner, extraSession, token2);
        }
    }

    /* loaded from: classes.dex */
    public static final class QueueItem implements Parcelable {
        public static final Parcelable.Creator<QueueItem> CREATOR = new Parcelable.Creator<QueueItem>() { // from class: android.support.v4.media.session.MediaSessionCompat.QueueItem.1
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public QueueItem createFromParcel(Parcel p) {
                return new QueueItem(p);
            }

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public QueueItem[] newArray(int size) {
                return new QueueItem[size];
            }
        };
        public static final int UNKNOWN_ID = -1;
        private final MediaDescriptionCompat mDescription;
        private final long mId;
        private Object mItem;

        public QueueItem(MediaDescriptionCompat description, long id) {
            this(null, description, id);
        }

        private QueueItem(Object queueItem, MediaDescriptionCompat description, long id) {
            if (description == null) {
                throw new IllegalArgumentException("Description cannot be null.");
            }
            if (id == -1) {
                throw new IllegalArgumentException("Id cannot be QueueItem.UNKNOWN_ID");
            }
            this.mDescription = description;
            this.mId = id;
            this.mItem = queueItem;
        }

        QueueItem(Parcel in) {
            this.mDescription = MediaDescriptionCompat.CREATOR.createFromParcel(in);
            this.mId = in.readLong();
        }

        public MediaDescriptionCompat getDescription() {
            return this.mDescription;
        }

        public long getQueueId() {
            return this.mId;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            this.mDescription.writeToParcel(dest, flags);
            dest.writeLong(this.mId);
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        public Object getQueueItem() {
            if (this.mItem != null || Build.VERSION.SDK_INT < 21) {
                return this.mItem;
            }
            this.mItem = MediaSessionCompatApi21.QueueItem.createItem(this.mDescription.getMediaDescription(), this.mId);
            return this.mItem;
        }

        public static QueueItem fromQueueItem(Object queueItem) {
            if (queueItem == null || Build.VERSION.SDK_INT < 21) {
                return null;
            }
            Object descriptionObj = MediaSessionCompatApi21.QueueItem.getDescription(queueItem);
            MediaDescriptionCompat description = MediaDescriptionCompat.fromMediaDescription(descriptionObj);
            long id = MediaSessionCompatApi21.QueueItem.getQueueId(queueItem);
            return new QueueItem(queueItem, description, id);
        }

        public static List<QueueItem> fromQueueItemList(List<?> itemList) {
            if (itemList == null || Build.VERSION.SDK_INT < 21) {
                return null;
            }
            List<QueueItem> items = new ArrayList<>();
            for (Object itemObj : itemList) {
                items.add(fromQueueItem(itemObj));
            }
            return items;
        }

        public String toString() {
            return "MediaSession.QueueItem {Description=" + this.mDescription + ", Id=" + this.mId + " }";
        }
    }

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    /* loaded from: classes.dex */
    public static final class ResultReceiverWrapper implements Parcelable {
        public static final Parcelable.Creator<ResultReceiverWrapper> CREATOR = new Parcelable.Creator<ResultReceiverWrapper>() { // from class: android.support.v4.media.session.MediaSessionCompat.ResultReceiverWrapper.1
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public ResultReceiverWrapper createFromParcel(Parcel p) {
                return new ResultReceiverWrapper(p);
            }

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public ResultReceiverWrapper[] newArray(int size) {
                return new ResultReceiverWrapper[size];
            }
        };
        private ResultReceiver mResultReceiver;

        public ResultReceiverWrapper(ResultReceiver resultReceiver) {
            this.mResultReceiver = resultReceiver;
        }

        ResultReceiverWrapper(Parcel in) {
            this.mResultReceiver = (ResultReceiver) ResultReceiver.CREATOR.createFromParcel(in);
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            this.mResultReceiver.writeToParcel(dest, flags);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class MediaSessionImplBase implements MediaSessionImpl {
        static final int RCC_PLAYSTATE_NONE = 0;
        final AudioManager mAudioManager;
        volatile Callback mCallback;
        boolean mCaptioningEnabled;
        private final Context mContext;
        Bundle mExtras;
        int mFlags;
        private MessageHandler mHandler;
        int mLocalStream;
        private final ComponentName mMediaButtonReceiverComponentName;
        private final PendingIntent mMediaButtonReceiverIntent;
        MediaMetadataCompat mMetadata;
        final String mPackageName;
        List<QueueItem> mQueue;
        CharSequence mQueueTitle;
        int mRatingType;
        final RemoteControlClient mRcc;
        int mRepeatMode;
        PendingIntent mSessionActivity;
        int mShuffleMode;
        PlaybackStateCompat mState;
        private final MediaSessionStub mStub;
        final String mTag;
        private final Token mToken;
        VolumeProviderCompat mVolumeProvider;
        int mVolumeType;
        final Object mLock = new Object();
        final RemoteCallbackList<IMediaControllerCallback> mControllerCallbacks = new RemoteCallbackList<>();
        boolean mDestroyed = false;
        boolean mIsActive = false;
        private boolean mIsMbrRegistered = false;
        private boolean mIsRccRegistered = false;
        private VolumeProviderCompat.Callback mVolumeCallback = new VolumeProviderCompat.Callback() { // from class: android.support.v4.media.session.MediaSessionCompat.MediaSessionImplBase.1
            @Override // android.support.v4.media.VolumeProviderCompat.Callback
            public void onVolumeChanged(VolumeProviderCompat volumeProvider) {
                if (MediaSessionImplBase.this.mVolumeProvider != volumeProvider) {
                    return;
                }
                ParcelableVolumeInfo info = new ParcelableVolumeInfo(MediaSessionImplBase.this.mVolumeType, MediaSessionImplBase.this.mLocalStream, volumeProvider.getVolumeControl(), volumeProvider.getMaxVolume(), volumeProvider.getCurrentVolume());
                MediaSessionImplBase.this.sendVolumeInfoChanged(info);
            }
        };

        public MediaSessionImplBase(Context context, String tag, ComponentName mbrComponent, PendingIntent mbrIntent) {
            if (mbrComponent == null) {
                throw new IllegalArgumentException("MediaButtonReceiver component may not be null.");
            }
            this.mContext = context;
            this.mPackageName = context.getPackageName();
            this.mAudioManager = (AudioManager) context.getSystemService("audio");
            this.mTag = tag;
            this.mMediaButtonReceiverComponentName = mbrComponent;
            this.mMediaButtonReceiverIntent = mbrIntent;
            this.mStub = new MediaSessionStub();
            this.mToken = new Token(this.mStub);
            this.mRatingType = 0;
            this.mVolumeType = 1;
            this.mLocalStream = 3;
            this.mRcc = new RemoteControlClient(mbrIntent);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setCallback(Callback callback, Handler handler) {
            Handler handler2;
            this.mCallback = callback;
            if (callback != null) {
                if (handler == null) {
                    handler2 = new Handler();
                } else {
                    handler2 = handler;
                }
                synchronized (this.mLock) {
                    if (this.mHandler != null) {
                        this.mHandler.removeCallbacksAndMessages(null);
                    }
                    this.mHandler = new MessageHandler(handler2.getLooper());
                    this.mCallback.setSessionImpl(this, handler2);
                }
            }
        }

        void postToHandler(int what, int arg1, int arg2, Object obj, Bundle extras) {
            synchronized (this.mLock) {
                if (this.mHandler != null) {
                    Message msg = this.mHandler.obtainMessage(what, arg1, arg2, obj);
                    Bundle data = new Bundle();
                    data.putString(MediaSessionCompat.DATA_CALLING_PACKAGE, MediaSessionManager.RemoteUserInfo.LEGACY_CONTROLLER);
                    data.putInt("data_calling_pid", Binder.getCallingPid());
                    data.putInt("data_calling_uid", Binder.getCallingUid());
                    if (extras != null) {
                        data.putBundle(MediaSessionCompat.DATA_EXTRAS, extras);
                    }
                    msg.setData(data);
                    msg.sendToTarget();
                }
            }
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setFlags(int flags) {
            synchronized (this.mLock) {
                this.mFlags = flags;
            }
            update();
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setPlaybackToLocal(int stream) {
            if (this.mVolumeProvider != null) {
                this.mVolumeProvider.setCallback(null);
            }
            this.mLocalStream = stream;
            this.mVolumeType = 1;
            ParcelableVolumeInfo info = new ParcelableVolumeInfo(this.mVolumeType, this.mLocalStream, 2, this.mAudioManager.getStreamMaxVolume(this.mLocalStream), this.mAudioManager.getStreamVolume(this.mLocalStream));
            sendVolumeInfoChanged(info);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setPlaybackToRemote(VolumeProviderCompat volumeProvider) {
            if (volumeProvider == null) {
                throw new IllegalArgumentException("volumeProvider may not be null");
            }
            if (this.mVolumeProvider != null) {
                this.mVolumeProvider.setCallback(null);
            }
            this.mVolumeType = 2;
            this.mVolumeProvider = volumeProvider;
            ParcelableVolumeInfo info = new ParcelableVolumeInfo(this.mVolumeType, this.mLocalStream, this.mVolumeProvider.getVolumeControl(), this.mVolumeProvider.getMaxVolume(), this.mVolumeProvider.getCurrentVolume());
            sendVolumeInfoChanged(info);
            volumeProvider.setCallback(this.mVolumeCallback);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setActive(boolean active) {
            if (active == this.mIsActive) {
                return;
            }
            this.mIsActive = active;
            if (update()) {
                setMetadata(this.mMetadata);
                setPlaybackState(this.mState);
            }
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public boolean isActive() {
            return this.mIsActive;
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void sendSessionEvent(String event, Bundle extras) {
            sendEvent(event, extras);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void release() {
            this.mIsActive = false;
            this.mDestroyed = true;
            update();
            sendSessionDestroyed();
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public Token getSessionToken() {
            return this.mToken;
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setPlaybackState(PlaybackStateCompat state) {
            synchronized (this.mLock) {
                this.mState = state;
            }
            sendState(state);
            if (!this.mIsActive) {
                return;
            }
            if (state == null) {
                this.mRcc.setPlaybackState(0);
                this.mRcc.setTransportControlFlags(0);
                return;
            }
            setRccState(state);
            this.mRcc.setTransportControlFlags(getRccTransportControlFlagsFromActions(state.getActions()));
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public PlaybackStateCompat getPlaybackState() {
            PlaybackStateCompat playbackStateCompat;
            synchronized (this.mLock) {
                playbackStateCompat = this.mState;
            }
            return playbackStateCompat;
        }

        void setRccState(PlaybackStateCompat state) {
            this.mRcc.setPlaybackState(getRccStateFromState(state.getState()));
        }

        int getRccStateFromState(int state) {
            switch (state) {
                case 0:
                    return 0;
                case 1:
                    return 1;
                case 2:
                    return 2;
                case 3:
                    return 3;
                case 4:
                    return 4;
                case 5:
                    return 5;
                case 6:
                case 8:
                    return 8;
                case 7:
                    return 9;
                case 9:
                    return 7;
                case 10:
                case 11:
                    return 6;
                default:
                    return -1;
            }
        }

        int getRccTransportControlFlagsFromActions(long actions) {
            int transportControlFlags = 0;
            if ((1 & actions) != 0) {
                transportControlFlags = 0 | 32;
            }
            if ((2 & actions) != 0) {
                transportControlFlags |= 16;
            }
            if ((4 & actions) != 0) {
                transportControlFlags |= 4;
            }
            if ((8 & actions) != 0) {
                transportControlFlags |= 2;
            }
            if ((16 & actions) != 0) {
                transportControlFlags |= 1;
            }
            if ((32 & actions) != 0) {
                transportControlFlags |= 128;
            }
            if ((64 & actions) != 0) {
                transportControlFlags |= 64;
            }
            if ((512 & actions) != 0) {
                return transportControlFlags | 8;
            }
            return transportControlFlags;
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setMetadata(MediaMetadataCompat metadata) {
            if (metadata != null) {
                metadata = new MediaMetadataCompat.Builder(metadata, MediaSessionCompat.sMaxBitmapSize).build();
            }
            synchronized (this.mLock) {
                this.mMetadata = metadata;
            }
            sendMetadata(metadata);
            if (!this.mIsActive) {
                return;
            }
            RemoteControlClient.MetadataEditor editor = buildRccMetadata(metadata == null ? null : metadata.getBundle());
            editor.apply();
        }

        RemoteControlClient.MetadataEditor buildRccMetadata(Bundle metadata) {
            RemoteControlClient.MetadataEditor editor = this.mRcc.editMetadata(true);
            if (metadata == null) {
                return editor;
            }
            if (metadata.containsKey("android.media.metadata.ART")) {
                Bitmap art = (Bitmap) metadata.getParcelable("android.media.metadata.ART");
                if (art != null) {
                    art = art.copy(art.getConfig(), false);
                }
                editor.putBitmap(100, art);
            } else if (metadata.containsKey("android.media.metadata.ALBUM_ART")) {
                Bitmap art2 = (Bitmap) metadata.getParcelable("android.media.metadata.ALBUM_ART");
                if (art2 != null) {
                    art2 = art2.copy(art2.getConfig(), false);
                }
                editor.putBitmap(100, art2);
            }
            if (metadata.containsKey("android.media.metadata.ALBUM")) {
                editor.putString(1, metadata.getString("android.media.metadata.ALBUM"));
            }
            if (metadata.containsKey("android.media.metadata.ALBUM_ARTIST")) {
                editor.putString(13, metadata.getString("android.media.metadata.ALBUM_ARTIST"));
            }
            if (metadata.containsKey("android.media.metadata.ARTIST")) {
                editor.putString(2, metadata.getString("android.media.metadata.ARTIST"));
            }
            if (metadata.containsKey("android.media.metadata.AUTHOR")) {
                editor.putString(3, metadata.getString("android.media.metadata.AUTHOR"));
            }
            if (metadata.containsKey("android.media.metadata.COMPILATION")) {
                editor.putString(15, metadata.getString("android.media.metadata.COMPILATION"));
            }
            if (metadata.containsKey("android.media.metadata.COMPOSER")) {
                editor.putString(4, metadata.getString("android.media.metadata.COMPOSER"));
            }
            if (metadata.containsKey("android.media.metadata.DATE")) {
                editor.putString(5, metadata.getString("android.media.metadata.DATE"));
            }
            if (metadata.containsKey("android.media.metadata.DISC_NUMBER")) {
                editor.putLong(14, metadata.getLong("android.media.metadata.DISC_NUMBER"));
            }
            if (metadata.containsKey("android.media.metadata.DURATION")) {
                editor.putLong(9, metadata.getLong("android.media.metadata.DURATION"));
            }
            if (metadata.containsKey("android.media.metadata.GENRE")) {
                editor.putString(6, metadata.getString("android.media.metadata.GENRE"));
            }
            if (metadata.containsKey("android.media.metadata.TITLE")) {
                editor.putString(7, metadata.getString("android.media.metadata.TITLE"));
            }
            if (metadata.containsKey("android.media.metadata.TRACK_NUMBER")) {
                editor.putLong(0, metadata.getLong("android.media.metadata.TRACK_NUMBER"));
            }
            if (metadata.containsKey("android.media.metadata.WRITER")) {
                editor.putString(11, metadata.getString("android.media.metadata.WRITER"));
            }
            return editor;
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setSessionActivity(PendingIntent pi) {
            synchronized (this.mLock) {
                this.mSessionActivity = pi;
            }
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setMediaButtonReceiver(PendingIntent mbr) {
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setQueue(List<QueueItem> queue) {
            this.mQueue = queue;
            sendQueue(queue);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setQueueTitle(CharSequence title) {
            this.mQueueTitle = title;
            sendQueueTitle(title);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public Object getMediaSession() {
            return null;
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public Object getRemoteControlClient() {
            return null;
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public String getCallingPackage() {
            return null;
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setRatingType(int type) {
            this.mRatingType = type;
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setCaptioningEnabled(boolean enabled) {
            if (this.mCaptioningEnabled != enabled) {
                this.mCaptioningEnabled = enabled;
                sendCaptioningEnabled(enabled);
            }
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setRepeatMode(int repeatMode) {
            if (this.mRepeatMode != repeatMode) {
                this.mRepeatMode = repeatMode;
                sendRepeatMode(repeatMode);
            }
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setShuffleMode(int shuffleMode) {
            if (this.mShuffleMode != shuffleMode) {
                this.mShuffleMode = shuffleMode;
                sendShuffleMode(shuffleMode);
            }
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setExtras(Bundle extras) {
            this.mExtras = extras;
            sendExtras(extras);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public MediaSessionManager.RemoteUserInfo getCurrentControllerInfo() {
            synchronized (this.mLock) {
                if (this.mHandler != null) {
                    return this.mHandler.getRemoteUserInfo();
                }
                return null;
            }
        }

        boolean update() {
            if (this.mIsActive) {
                if (!this.mIsMbrRegistered && (this.mFlags & 1) != 0) {
                    registerMediaButtonEventReceiver(this.mMediaButtonReceiverIntent, this.mMediaButtonReceiverComponentName);
                    this.mIsMbrRegistered = true;
                } else if (this.mIsMbrRegistered && (this.mFlags & 1) == 0) {
                    unregisterMediaButtonEventReceiver(this.mMediaButtonReceiverIntent, this.mMediaButtonReceiverComponentName);
                    this.mIsMbrRegistered = false;
                }
                if (!this.mIsRccRegistered && (this.mFlags & 2) != 0) {
                    this.mAudioManager.registerRemoteControlClient(this.mRcc);
                    this.mIsRccRegistered = true;
                    return true;
                } else if (!this.mIsRccRegistered || (this.mFlags & 2) != 0) {
                    return false;
                } else {
                    this.mRcc.setPlaybackState(0);
                    this.mAudioManager.unregisterRemoteControlClient(this.mRcc);
                    this.mIsRccRegistered = false;
                    return false;
                }
            }
            if (this.mIsMbrRegistered) {
                unregisterMediaButtonEventReceiver(this.mMediaButtonReceiverIntent, this.mMediaButtonReceiverComponentName);
                this.mIsMbrRegistered = false;
            }
            if (!this.mIsRccRegistered) {
                return false;
            }
            this.mRcc.setPlaybackState(0);
            this.mAudioManager.unregisterRemoteControlClient(this.mRcc);
            this.mIsRccRegistered = false;
            return false;
        }

        void registerMediaButtonEventReceiver(PendingIntent mbrIntent, ComponentName mbrComponent) {
            this.mAudioManager.registerMediaButtonEventReceiver(mbrComponent);
        }

        void unregisterMediaButtonEventReceiver(PendingIntent mbrIntent, ComponentName mbrComponent) {
            this.mAudioManager.unregisterMediaButtonEventReceiver(mbrComponent);
        }

        void adjustVolume(int direction, int flags) {
            if (this.mVolumeType == 2) {
                if (this.mVolumeProvider != null) {
                    this.mVolumeProvider.onAdjustVolume(direction);
                    return;
                }
                return;
            }
            this.mAudioManager.adjustStreamVolume(this.mLocalStream, direction, flags);
        }

        void setVolumeTo(int value, int flags) {
            if (this.mVolumeType == 2) {
                if (this.mVolumeProvider != null) {
                    this.mVolumeProvider.onSetVolumeTo(value);
                    return;
                }
                return;
            }
            this.mAudioManager.setStreamVolume(this.mLocalStream, value, flags);
        }

        void sendVolumeInfoChanged(ParcelableVolumeInfo info) {
            int size = this.mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = this.mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onVolumeInfoChanged(info);
                } catch (RemoteException e) {
                }
            }
            this.mControllerCallbacks.finishBroadcast();
        }

        private void sendSessionDestroyed() {
            int size = this.mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = this.mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onSessionDestroyed();
                } catch (RemoteException e) {
                }
            }
            this.mControllerCallbacks.finishBroadcast();
            this.mControllerCallbacks.kill();
        }

        private void sendEvent(String event, Bundle extras) {
            int size = this.mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = this.mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onEvent(event, extras);
                } catch (RemoteException e) {
                }
            }
            this.mControllerCallbacks.finishBroadcast();
        }

        private void sendState(PlaybackStateCompat state) {
            int size = this.mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = this.mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onPlaybackStateChanged(state);
                } catch (RemoteException e) {
                }
            }
            this.mControllerCallbacks.finishBroadcast();
        }

        private void sendMetadata(MediaMetadataCompat metadata) {
            int size = this.mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = this.mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onMetadataChanged(metadata);
                } catch (RemoteException e) {
                }
            }
            this.mControllerCallbacks.finishBroadcast();
        }

        private void sendQueue(List<QueueItem> queue) {
            int size = this.mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = this.mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onQueueChanged(queue);
                } catch (RemoteException e) {
                }
            }
            this.mControllerCallbacks.finishBroadcast();
        }

        private void sendQueueTitle(CharSequence queueTitle) {
            int size = this.mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = this.mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onQueueTitleChanged(queueTitle);
                } catch (RemoteException e) {
                }
            }
            this.mControllerCallbacks.finishBroadcast();
        }

        private void sendCaptioningEnabled(boolean enabled) {
            int size = this.mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = this.mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onCaptioningEnabledChanged(enabled);
                } catch (RemoteException e) {
                }
            }
            this.mControllerCallbacks.finishBroadcast();
        }

        private void sendRepeatMode(int repeatMode) {
            int size = this.mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = this.mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onRepeatModeChanged(repeatMode);
                } catch (RemoteException e) {
                }
            }
            this.mControllerCallbacks.finishBroadcast();
        }

        private void sendShuffleMode(int shuffleMode) {
            int size = this.mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = this.mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onShuffleModeChanged(shuffleMode);
                } catch (RemoteException e) {
                }
            }
            this.mControllerCallbacks.finishBroadcast();
        }

        private void sendExtras(Bundle extras) {
            int size = this.mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = this.mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onExtrasChanged(extras);
                } catch (RemoteException e) {
                }
            }
            this.mControllerCallbacks.finishBroadcast();
        }

        /* loaded from: classes.dex */
        class MediaSessionStub extends IMediaSession.Stub {
            MediaSessionStub() {
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void sendCommand(String command, Bundle args, ResultReceiverWrapper cb) {
                postToHandler(1, new Command(command, args, cb.mResultReceiver));
            }

            @Override // android.support.v4.media.session.IMediaSession
            public boolean sendMediaButton(KeyEvent mediaButton) {
                boolean handlesMediaButtons = (MediaSessionImplBase.this.mFlags & 1) != 0;
                if (handlesMediaButtons) {
                    postToHandler(21, mediaButton);
                }
                return handlesMediaButtons;
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void registerCallbackListener(IMediaControllerCallback cb) {
                if (MediaSessionImplBase.this.mDestroyed) {
                    try {
                        cb.onSessionDestroyed();
                        return;
                    } catch (Exception e) {
                        return;
                    }
                }
                MediaSessionImplBase.this.mControllerCallbacks.register(cb);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void unregisterCallbackListener(IMediaControllerCallback cb) {
                MediaSessionImplBase.this.mControllerCallbacks.unregister(cb);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public String getPackageName() {
                return MediaSessionImplBase.this.mPackageName;
            }

            @Override // android.support.v4.media.session.IMediaSession
            public String getTag() {
                return MediaSessionImplBase.this.mTag;
            }

            @Override // android.support.v4.media.session.IMediaSession
            public PendingIntent getLaunchPendingIntent() {
                PendingIntent pendingIntent;
                synchronized (MediaSessionImplBase.this.mLock) {
                    pendingIntent = MediaSessionImplBase.this.mSessionActivity;
                }
                return pendingIntent;
            }

            @Override // android.support.v4.media.session.IMediaSession
            public long getFlags() {
                long j;
                synchronized (MediaSessionImplBase.this.mLock) {
                    j = MediaSessionImplBase.this.mFlags;
                }
                return j;
            }

            @Override // android.support.v4.media.session.IMediaSession
            public ParcelableVolumeInfo getVolumeAttributes() {
                boolean z;
                int volumeType;
                int stream;
                VolumeProviderCompat vp;
                int controlType;
                int max;
                int current;
                synchronized (MediaSessionImplBase.this.mLock) {
                    int current2 = 0;
                    try {
                        volumeType = MediaSessionImplBase.this.mVolumeType;
                        try {
                            stream = MediaSessionImplBase.this.mLocalStream;
                            try {
                                vp = MediaSessionImplBase.this.mVolumeProvider;
                                controlType = 2;
                            } catch (Throwable th) {
                                th = th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            z = false;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            }
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        z = false;
                    }
                    try {
                        try {
                            if (volumeType == 2) {
                                controlType = vp.getVolumeControl();
                                int max2 = vp.getMaxVolume();
                                current = vp.getCurrentVolume();
                                current2 = controlType;
                                max = max2;
                            } else {
                                controlType = 2;
                                int max3 = MediaSessionImplBase.this.mAudioManager.getStreamMaxVolume(stream);
                                current2 = 2;
                                max = max3;
                                current = MediaSessionImplBase.this.mAudioManager.getStreamVolume(stream);
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            while (true) {
                                break;
                                break;
                            }
                            throw th;
                        }
                        try {
                            return new ParcelableVolumeInfo(volumeType, stream, current2, max, current);
                        } catch (Throwable th6) {
                            th = th6;
                            while (true) {
                                break;
                                break;
                            }
                            throw th;
                        }
                    } catch (Throwable th7) {
                        th = th7;
                        while (true) {
                            break;
                            break;
                        }
                        throw th;
                    }
                }
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void adjustVolume(int direction, int flags, String packageName) {
                MediaSessionImplBase.this.adjustVolume(direction, flags);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void setVolumeTo(int value, int flags, String packageName) {
                MediaSessionImplBase.this.setVolumeTo(value, flags);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void prepare() throws RemoteException {
                postToHandler(3);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void prepareFromMediaId(String mediaId, Bundle extras) throws RemoteException {
                postToHandler(4, mediaId, extras);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void prepareFromSearch(String query, Bundle extras) throws RemoteException {
                postToHandler(5, query, extras);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void prepareFromUri(Uri uri, Bundle extras) throws RemoteException {
                postToHandler(6, uri, extras);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void play() throws RemoteException {
                postToHandler(7);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void playFromMediaId(String mediaId, Bundle extras) throws RemoteException {
                postToHandler(8, mediaId, extras);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void playFromSearch(String query, Bundle extras) throws RemoteException {
                postToHandler(9, query, extras);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void playFromUri(Uri uri, Bundle extras) throws RemoteException {
                postToHandler(10, uri, extras);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void skipToQueueItem(long id) {
                postToHandler(11, Long.valueOf(id));
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void pause() throws RemoteException {
                postToHandler(12);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void stop() throws RemoteException {
                postToHandler(13);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void next() throws RemoteException {
                postToHandler(14);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void previous() throws RemoteException {
                postToHandler(15);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void fastForward() throws RemoteException {
                postToHandler(16);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void rewind() throws RemoteException {
                postToHandler(17);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void seekTo(long pos) throws RemoteException {
                postToHandler(18, Long.valueOf(pos));
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void rate(RatingCompat rating) throws RemoteException {
                postToHandler(19, rating);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void rateWithExtras(RatingCompat rating, Bundle extras) throws RemoteException {
                postToHandler(31, rating, extras);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void setCaptioningEnabled(boolean enabled) throws RemoteException {
                postToHandler(29, Boolean.valueOf(enabled));
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void setRepeatMode(int repeatMode) throws RemoteException {
                postToHandler(23, repeatMode);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void setShuffleModeEnabledRemoved(boolean enabled) throws RemoteException {
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void setShuffleMode(int shuffleMode) throws RemoteException {
                postToHandler(30, shuffleMode);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void sendCustomAction(String action, Bundle args) throws RemoteException {
                postToHandler(20, action, args);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public MediaMetadataCompat getMetadata() {
                return MediaSessionImplBase.this.mMetadata;
            }

            @Override // android.support.v4.media.session.IMediaSession
            public PlaybackStateCompat getPlaybackState() {
                synchronized (MediaSessionImplBase.this.mLock) {
                    MediaMetadataCompat metadata = null;
                    try {
                        PlaybackStateCompat state = MediaSessionImplBase.this.mState;
                        try {
                            metadata = MediaSessionImplBase.this.mMetadata;
                            return MediaSessionCompat.getStateWithUpdatedPosition(state, metadata);
                        } catch (Throwable th) {
                            th = th;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            }
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                    }
                }
            }

            @Override // android.support.v4.media.session.IMediaSession
            public List<QueueItem> getQueue() {
                List<QueueItem> list;
                synchronized (MediaSessionImplBase.this.mLock) {
                    list = MediaSessionImplBase.this.mQueue;
                }
                return list;
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void addQueueItem(MediaDescriptionCompat description) {
                postToHandler(25, description);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void addQueueItemAt(MediaDescriptionCompat description, int index) {
                postToHandler(26, description, index);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void removeQueueItem(MediaDescriptionCompat description) {
                postToHandler(27, description);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void removeQueueItemAt(int index) {
                postToHandler(28, index);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public CharSequence getQueueTitle() {
                return MediaSessionImplBase.this.mQueueTitle;
            }

            @Override // android.support.v4.media.session.IMediaSession
            public Bundle getExtras() {
                Bundle bundle;
                synchronized (MediaSessionImplBase.this.mLock) {
                    bundle = MediaSessionImplBase.this.mExtras;
                }
                return bundle;
            }

            @Override // android.support.v4.media.session.IMediaSession
            public int getRatingType() {
                return MediaSessionImplBase.this.mRatingType;
            }

            @Override // android.support.v4.media.session.IMediaSession
            public boolean isCaptioningEnabled() {
                return MediaSessionImplBase.this.mCaptioningEnabled;
            }

            @Override // android.support.v4.media.session.IMediaSession
            public int getRepeatMode() {
                return MediaSessionImplBase.this.mRepeatMode;
            }

            @Override // android.support.v4.media.session.IMediaSession
            public boolean isShuffleModeEnabledRemoved() {
                return false;
            }

            @Override // android.support.v4.media.session.IMediaSession
            public int getShuffleMode() {
                return MediaSessionImplBase.this.mShuffleMode;
            }

            @Override // android.support.v4.media.session.IMediaSession
            public boolean isTransportControlEnabled() {
                return (MediaSessionImplBase.this.mFlags & 2) != 0;
            }

            void postToHandler(int what) {
                MediaSessionImplBase.this.postToHandler(what, 0, 0, null, null);
            }

            void postToHandler(int what, int arg1) {
                MediaSessionImplBase.this.postToHandler(what, arg1, 0, null, null);
            }

            void postToHandler(int what, Object obj) {
                MediaSessionImplBase.this.postToHandler(what, 0, 0, obj, null);
            }

            void postToHandler(int what, Object obj, int arg1) {
                MediaSessionImplBase.this.postToHandler(what, arg1, 0, obj, null);
            }

            void postToHandler(int what, Object obj, Bundle extras) {
                MediaSessionImplBase.this.postToHandler(what, 0, 0, obj, extras);
            }
        }

        /* loaded from: classes.dex */
        private static final class Command {
            public final String command;
            public final Bundle extras;
            public final ResultReceiver stub;

            public Command(String command, Bundle extras, ResultReceiver stub) {
                this.command = command;
                this.extras = extras;
                this.stub = stub;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes.dex */
        public class MessageHandler extends Handler {
            private static final int KEYCODE_MEDIA_PAUSE = 127;
            private static final int KEYCODE_MEDIA_PLAY = 126;
            private static final int MSG_ADD_QUEUE_ITEM = 25;
            private static final int MSG_ADD_QUEUE_ITEM_AT = 26;
            private static final int MSG_ADJUST_VOLUME = 2;
            private static final int MSG_COMMAND = 1;
            private static final int MSG_CUSTOM_ACTION = 20;
            private static final int MSG_FAST_FORWARD = 16;
            private static final int MSG_MEDIA_BUTTON = 21;
            private static final int MSG_NEXT = 14;
            private static final int MSG_PAUSE = 12;
            private static final int MSG_PLAY = 7;
            private static final int MSG_PLAY_MEDIA_ID = 8;
            private static final int MSG_PLAY_SEARCH = 9;
            private static final int MSG_PLAY_URI = 10;
            private static final int MSG_PREPARE = 3;
            private static final int MSG_PREPARE_MEDIA_ID = 4;
            private static final int MSG_PREPARE_SEARCH = 5;
            private static final int MSG_PREPARE_URI = 6;
            private static final int MSG_PREVIOUS = 15;
            private static final int MSG_RATE = 19;
            private static final int MSG_RATE_EXTRA = 31;
            private static final int MSG_REMOVE_QUEUE_ITEM = 27;
            private static final int MSG_REMOVE_QUEUE_ITEM_AT = 28;
            private static final int MSG_REWIND = 17;
            private static final int MSG_SEEK_TO = 18;
            private static final int MSG_SET_CAPTIONING_ENABLED = 29;
            private static final int MSG_SET_REPEAT_MODE = 23;
            private static final int MSG_SET_SHUFFLE_MODE = 30;
            private static final int MSG_SET_VOLUME = 22;
            private static final int MSG_SKIP_TO_ITEM = 11;
            private static final int MSG_STOP = 13;
            private MediaSessionManager.RemoteUserInfo mRemoteUserInfo;

            public MessageHandler(Looper looper) {
                super(looper);
            }

            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                QueueItem item;
                Callback cb = MediaSessionImplBase.this.mCallback;
                if (cb == null) {
                    return;
                }
                Bundle data = msg.getData();
                this.mRemoteUserInfo = new MediaSessionManager.RemoteUserInfo(data.getString(MediaSessionCompat.DATA_CALLING_PACKAGE), data.getInt("data_calling_pid"), data.getInt("data_calling_uid"));
                Bundle data2 = data.getBundle(MediaSessionCompat.DATA_EXTRAS);
                try {
                    switch (msg.what) {
                        case 1:
                            Command cmd = (Command) msg.obj;
                            cb.onCommand(cmd.command, cmd.extras, cmd.stub);
                            break;
                        case 2:
                            MediaSessionImplBase.this.adjustVolume(msg.arg1, 0);
                            break;
                        case 3:
                            cb.onPrepare();
                            break;
                        case 4:
                            cb.onPrepareFromMediaId((String) msg.obj, data2);
                            break;
                        case 5:
                            cb.onPrepareFromSearch((String) msg.obj, data2);
                            break;
                        case 6:
                            cb.onPrepareFromUri((Uri) msg.obj, data2);
                            break;
                        case 7:
                            cb.onPlay();
                            break;
                        case 8:
                            cb.onPlayFromMediaId((String) msg.obj, data2);
                            break;
                        case 9:
                            cb.onPlayFromSearch((String) msg.obj, data2);
                            break;
                        case 10:
                            cb.onPlayFromUri((Uri) msg.obj, data2);
                            break;
                        case 11:
                            cb.onSkipToQueueItem(((Long) msg.obj).longValue());
                            break;
                        case 12:
                            cb.onPause();
                            break;
                        case 13:
                            cb.onStop();
                            break;
                        case 14:
                            cb.onSkipToNext();
                            break;
                        case 15:
                            cb.onSkipToPrevious();
                            break;
                        case 16:
                            cb.onFastForward();
                            break;
                        case 17:
                            cb.onRewind();
                            break;
                        case 18:
                            cb.onSeekTo(((Long) msg.obj).longValue());
                            break;
                        case 19:
                            cb.onSetRating((RatingCompat) msg.obj);
                            break;
                        case 20:
                            cb.onCustomAction((String) msg.obj, data2);
                            break;
                        case 21:
                            KeyEvent keyEvent = (KeyEvent) msg.obj;
                            Intent intent = new Intent("android.intent.action.MEDIA_BUTTON");
                            intent.putExtra("android.intent.extra.KEY_EVENT", keyEvent);
                            if (!cb.onMediaButtonEvent(intent)) {
                                onMediaButtonEvent(keyEvent, cb);
                                break;
                            }
                            break;
                        case 22:
                            MediaSessionImplBase.this.setVolumeTo(msg.arg1, 0);
                            break;
                        case 23:
                            cb.onSetRepeatMode(msg.arg1);
                            break;
                        case 25:
                            cb.onAddQueueItem((MediaDescriptionCompat) msg.obj);
                            break;
                        case 26:
                            cb.onAddQueueItem((MediaDescriptionCompat) msg.obj, msg.arg1);
                            break;
                        case 27:
                            cb.onRemoveQueueItem((MediaDescriptionCompat) msg.obj);
                            break;
                        case 28:
                            if (MediaSessionImplBase.this.mQueue != null) {
                                if (msg.arg1 < 0 || msg.arg1 >= MediaSessionImplBase.this.mQueue.size()) {
                                    item = null;
                                } else {
                                    item = MediaSessionImplBase.this.mQueue.get(msg.arg1);
                                }
                                if (item != null) {
                                    cb.onRemoveQueueItem(item.getDescription());
                                }
                                break;
                            }
                            break;
                        case 29:
                            cb.onSetCaptioningEnabled(((Boolean) msg.obj).booleanValue());
                            break;
                        case 30:
                            cb.onSetShuffleMode(msg.arg1);
                            break;
                        case 31:
                            cb.onSetRating((RatingCompat) msg.obj, data2);
                            break;
                    }
                } finally {
                    this.mRemoteUserInfo = null;
                }
            }

            private void onMediaButtonEvent(KeyEvent ke, Callback cb) {
                if (ke == null || ke.getAction() != 0) {
                    return;
                }
                long validActions = MediaSessionImplBase.this.mState == null ? 0L : MediaSessionImplBase.this.mState.getActions();
                int keyCode = ke.getKeyCode();
                if (keyCode != 79) {
                    switch (keyCode) {
                        case InstallShortcutReceiver.NEW_SHORTCUT_STAGGER_DELAY /* 85 */:
                            break;
                        case 86:
                            if ((1 & validActions) != 0) {
                                cb.onStop();
                                return;
                            }
                            return;
                        case 87:
                            if ((32 & validActions) != 0) {
                                cb.onSkipToNext();
                                return;
                            }
                            return;
                        case 88:
                            if ((16 & validActions) != 0) {
                                cb.onSkipToPrevious();
                                return;
                            }
                            return;
                        case 89:
                            if ((8 & validActions) != 0) {
                                cb.onRewind();
                                return;
                            }
                            return;
                        case 90:
                            if ((64 & validActions) != 0) {
                                cb.onFastForward();
                                return;
                            }
                            return;
                        default:
                            switch (keyCode) {
                                case KEYCODE_MEDIA_PLAY /* 126 */:
                                    if ((4 & validActions) != 0) {
                                        cb.onPlay();
                                        return;
                                    }
                                    return;
                                case 127:
                                    if ((2 & validActions) != 0) {
                                        cb.onPause();
                                        return;
                                    }
                                    return;
                                default:
                                    return;
                            }
                    }
                }
                Log.w(MediaSessionCompat.TAG, "KEYCODE_MEDIA_PLAY_PAUSE and KEYCODE_HEADSETHOOK are handled already");
            }

            MediaSessionManager.RemoteUserInfo getRemoteUserInfo() {
                return this.mRemoteUserInfo;
            }
        }
    }

    @RequiresApi(18)
    /* loaded from: classes.dex */
    static class MediaSessionImplApi18 extends MediaSessionImplBase {
        private static boolean sIsMbrPendingIntentSupported = true;

        MediaSessionImplApi18(Context context, String tag, ComponentName mbrComponent, PendingIntent mbrIntent) {
            super(context, tag, mbrComponent, mbrIntent);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImplBase, android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setCallback(Callback callback, Handler handler) {
            super.setCallback(callback, handler);
            if (callback == null) {
                this.mRcc.setPlaybackPositionUpdateListener(null);
                return;
            }
            RemoteControlClient.OnPlaybackPositionUpdateListener listener = new RemoteControlClient.OnPlaybackPositionUpdateListener() { // from class: android.support.v4.media.session.MediaSessionCompat.MediaSessionImplApi18.1
                @Override // android.media.RemoteControlClient.OnPlaybackPositionUpdateListener
                public void onPlaybackPositionUpdate(long newPositionMs) {
                    MediaSessionImplApi18.this.postToHandler(18, -1, -1, Long.valueOf(newPositionMs), null);
                }
            };
            this.mRcc.setPlaybackPositionUpdateListener(listener);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImplBase
        void setRccState(PlaybackStateCompat state) {
            long position = state.getPosition();
            float speed = state.getPlaybackSpeed();
            long updateTime = state.getLastPositionUpdateTime();
            long currTime = SystemClock.elapsedRealtime();
            if (state.getState() == 3 && position > 0) {
                long diff = 0;
                if (updateTime > 0) {
                    diff = currTime - updateTime;
                    if (speed > 0.0f && speed != 1.0f) {
                        diff = ((float) diff) * speed;
                    }
                }
                position += diff;
            }
            this.mRcc.setPlaybackState(getRccStateFromState(state.getState()), position, speed);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImplBase
        int getRccTransportControlFlagsFromActions(long actions) {
            int transportControlFlags = super.getRccTransportControlFlagsFromActions(actions);
            if ((256 & actions) != 0) {
                return transportControlFlags | 256;
            }
            return transportControlFlags;
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImplBase
        void registerMediaButtonEventReceiver(PendingIntent mbrIntent, ComponentName mbrComponent) {
            if (sIsMbrPendingIntentSupported) {
                try {
                    this.mAudioManager.registerMediaButtonEventReceiver(mbrIntent);
                } catch (NullPointerException e) {
                    Log.w(MediaSessionCompat.TAG, "Unable to register media button event receiver with PendingIntent, falling back to ComponentName.");
                    sIsMbrPendingIntentSupported = false;
                }
            }
            if (!sIsMbrPendingIntentSupported) {
                super.registerMediaButtonEventReceiver(mbrIntent, mbrComponent);
            }
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImplBase
        void unregisterMediaButtonEventReceiver(PendingIntent mbrIntent, ComponentName mbrComponent) {
            if (sIsMbrPendingIntentSupported) {
                this.mAudioManager.unregisterMediaButtonEventReceiver(mbrIntent);
            } else {
                super.unregisterMediaButtonEventReceiver(mbrIntent, mbrComponent);
            }
        }
    }

    @RequiresApi(19)
    /* loaded from: classes.dex */
    static class MediaSessionImplApi19 extends MediaSessionImplApi18 {
        MediaSessionImplApi19(Context context, String tag, ComponentName mbrComponent, PendingIntent mbrIntent) {
            super(context, tag, mbrComponent, mbrIntent);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImplApi18, android.support.v4.media.session.MediaSessionCompat.MediaSessionImplBase, android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setCallback(Callback callback, Handler handler) {
            super.setCallback(callback, handler);
            if (callback == null) {
                this.mRcc.setMetadataUpdateListener(null);
                return;
            }
            RemoteControlClient.OnMetadataUpdateListener listener = new RemoteControlClient.OnMetadataUpdateListener() { // from class: android.support.v4.media.session.MediaSessionCompat.MediaSessionImplApi19.1
                @Override // android.media.RemoteControlClient.OnMetadataUpdateListener
                public void onMetadataUpdate(int key, Object newValue) {
                    if (key == 268435457 && (newValue instanceof Rating)) {
                        MediaSessionImplApi19.this.postToHandler(19, -1, -1, RatingCompat.fromRating(newValue), null);
                    }
                }
            };
            this.mRcc.setMetadataUpdateListener(listener);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImplApi18, android.support.v4.media.session.MediaSessionCompat.MediaSessionImplBase
        int getRccTransportControlFlagsFromActions(long actions) {
            int transportControlFlags = super.getRccTransportControlFlagsFromActions(actions);
            if ((128 & actions) != 0) {
                return transportControlFlags | 512;
            }
            return transportControlFlags;
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImplBase
        RemoteControlClient.MetadataEditor buildRccMetadata(Bundle metadata) {
            RemoteControlClient.MetadataEditor editor = super.buildRccMetadata(metadata);
            long actions = this.mState == null ? 0L : this.mState.getActions();
            if ((128 & actions) != 0) {
                editor.addEditableKey(268435457);
            }
            if (metadata == null) {
                return editor;
            }
            if (metadata.containsKey("android.media.metadata.YEAR")) {
                editor.putLong(8, metadata.getLong("android.media.metadata.YEAR"));
            }
            if (metadata.containsKey("android.media.metadata.RATING")) {
                editor.putObject(101, (Object) metadata.getParcelable("android.media.metadata.RATING"));
            }
            if (metadata.containsKey("android.media.metadata.USER_RATING")) {
                editor.putObject(268435457, (Object) metadata.getParcelable("android.media.metadata.USER_RATING"));
            }
            return editor;
        }
    }

    @RequiresApi(21)
    /* loaded from: classes.dex */
    static class MediaSessionImplApi21 implements MediaSessionImpl {
        boolean mCaptioningEnabled;
        private boolean mDestroyed = false;
        private final RemoteCallbackList<IMediaControllerCallback> mExtraControllerCallbacks = new RemoteCallbackList<>();
        private MediaMetadataCompat mMetadata;
        private PlaybackStateCompat mPlaybackState;
        private List<QueueItem> mQueue;
        int mRatingType;
        int mRepeatMode;
        private final Object mSessionObj;
        int mShuffleMode;
        private final Token mToken;

        MediaSessionImplApi21(Context context, String tag, SessionToken2 token2) {
            this.mSessionObj = MediaSessionCompatApi21.createSession(context, tag);
            this.mToken = new Token(MediaSessionCompatApi21.getSessionToken(this.mSessionObj), new ExtraSession(), token2);
        }

        MediaSessionImplApi21(Object mediaSession) {
            this.mSessionObj = MediaSessionCompatApi21.verifySession(mediaSession);
            this.mToken = new Token(MediaSessionCompatApi21.getSessionToken(this.mSessionObj), new ExtraSession());
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setCallback(Callback callback, Handler handler) {
            MediaSessionCompatApi21.setCallback(this.mSessionObj, callback == null ? null : callback.mCallbackObj, handler);
            if (callback != null) {
                callback.setSessionImpl(this, handler);
            }
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setFlags(int flags) {
            MediaSessionCompatApi21.setFlags(this.mSessionObj, flags);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setPlaybackToLocal(int stream) {
            MediaSessionCompatApi21.setPlaybackToLocal(this.mSessionObj, stream);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setPlaybackToRemote(VolumeProviderCompat volumeProvider) {
            MediaSessionCompatApi21.setPlaybackToRemote(this.mSessionObj, volumeProvider.getVolumeProvider());
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setActive(boolean active) {
            MediaSessionCompatApi21.setActive(this.mSessionObj, active);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public boolean isActive() {
            return MediaSessionCompatApi21.isActive(this.mSessionObj);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void sendSessionEvent(String event, Bundle extras) {
            if (Build.VERSION.SDK_INT < 23) {
                int size = this.mExtraControllerCallbacks.beginBroadcast();
                for (int i = size - 1; i >= 0; i--) {
                    IMediaControllerCallback cb = this.mExtraControllerCallbacks.getBroadcastItem(i);
                    try {
                        cb.onEvent(event, extras);
                    } catch (RemoteException e) {
                    }
                }
                this.mExtraControllerCallbacks.finishBroadcast();
            }
            MediaSessionCompatApi21.sendSessionEvent(this.mSessionObj, event, extras);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void release() {
            this.mDestroyed = true;
            MediaSessionCompatApi21.release(this.mSessionObj);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public Token getSessionToken() {
            return this.mToken;
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setPlaybackState(PlaybackStateCompat state) {
            this.mPlaybackState = state;
            int size = this.mExtraControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = this.mExtraControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onPlaybackStateChanged(state);
                } catch (RemoteException e) {
                }
            }
            this.mExtraControllerCallbacks.finishBroadcast();
            MediaSessionCompatApi21.setPlaybackState(this.mSessionObj, state == null ? null : state.getPlaybackState());
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public PlaybackStateCompat getPlaybackState() {
            return this.mPlaybackState;
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setMetadata(MediaMetadataCompat metadata) {
            this.mMetadata = metadata;
            MediaSessionCompatApi21.setMetadata(this.mSessionObj, metadata == null ? null : metadata.getMediaMetadata());
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setSessionActivity(PendingIntent pi) {
            MediaSessionCompatApi21.setSessionActivity(this.mSessionObj, pi);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setMediaButtonReceiver(PendingIntent mbr) {
            MediaSessionCompatApi21.setMediaButtonReceiver(this.mSessionObj, mbr);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setQueue(List<QueueItem> queue) {
            this.mQueue = queue;
            List<Object> queueObjs = null;
            if (queue != null) {
                queueObjs = new ArrayList<>();
                for (QueueItem item : queue) {
                    queueObjs.add(item.getQueueItem());
                }
            }
            MediaSessionCompatApi21.setQueue(this.mSessionObj, queueObjs);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setQueueTitle(CharSequence title) {
            MediaSessionCompatApi21.setQueueTitle(this.mSessionObj, title);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setRatingType(int type) {
            if (Build.VERSION.SDK_INT < 22) {
                this.mRatingType = type;
            } else {
                MediaSessionCompatApi22.setRatingType(this.mSessionObj, type);
            }
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setCaptioningEnabled(boolean enabled) {
            if (this.mCaptioningEnabled != enabled) {
                this.mCaptioningEnabled = enabled;
                int size = this.mExtraControllerCallbacks.beginBroadcast();
                for (int i = size - 1; i >= 0; i--) {
                    IMediaControllerCallback cb = this.mExtraControllerCallbacks.getBroadcastItem(i);
                    try {
                        cb.onCaptioningEnabledChanged(enabled);
                    } catch (RemoteException e) {
                    }
                }
                this.mExtraControllerCallbacks.finishBroadcast();
            }
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setRepeatMode(int repeatMode) {
            if (this.mRepeatMode != repeatMode) {
                this.mRepeatMode = repeatMode;
                int size = this.mExtraControllerCallbacks.beginBroadcast();
                for (int i = size - 1; i >= 0; i--) {
                    IMediaControllerCallback cb = this.mExtraControllerCallbacks.getBroadcastItem(i);
                    try {
                        cb.onRepeatModeChanged(repeatMode);
                    } catch (RemoteException e) {
                    }
                }
                this.mExtraControllerCallbacks.finishBroadcast();
            }
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setShuffleMode(int shuffleMode) {
            if (this.mShuffleMode != shuffleMode) {
                this.mShuffleMode = shuffleMode;
                int size = this.mExtraControllerCallbacks.beginBroadcast();
                for (int i = size - 1; i >= 0; i--) {
                    IMediaControllerCallback cb = this.mExtraControllerCallbacks.getBroadcastItem(i);
                    try {
                        cb.onShuffleModeChanged(shuffleMode);
                    } catch (RemoteException e) {
                    }
                }
                this.mExtraControllerCallbacks.finishBroadcast();
            }
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public void setExtras(Bundle extras) {
            MediaSessionCompatApi21.setExtras(this.mSessionObj, extras);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public Object getMediaSession() {
            return this.mSessionObj;
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public Object getRemoteControlClient() {
            return null;
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public String getCallingPackage() {
            if (Build.VERSION.SDK_INT < 24) {
                return null;
            }
            return MediaSessionCompatApi24.getCallingPackage(this.mSessionObj);
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        public MediaSessionManager.RemoteUserInfo getCurrentControllerInfo() {
            return null;
        }

        /* loaded from: classes.dex */
        class ExtraSession extends IMediaSession.Stub {
            ExtraSession() {
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void sendCommand(String command, Bundle args, ResultReceiverWrapper cb) {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public boolean sendMediaButton(KeyEvent mediaButton) {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void registerCallbackListener(IMediaControllerCallback cb) {
                if (!MediaSessionImplApi21.this.mDestroyed) {
                    MediaSessionImplApi21.this.mExtraControllerCallbacks.register(cb);
                }
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void unregisterCallbackListener(IMediaControllerCallback cb) {
                MediaSessionImplApi21.this.mExtraControllerCallbacks.unregister(cb);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public String getPackageName() {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public String getTag() {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public PendingIntent getLaunchPendingIntent() {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public long getFlags() {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public ParcelableVolumeInfo getVolumeAttributes() {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void adjustVolume(int direction, int flags, String packageName) {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void setVolumeTo(int value, int flags, String packageName) {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void prepare() throws RemoteException {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void prepareFromMediaId(String mediaId, Bundle extras) throws RemoteException {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void prepareFromSearch(String query, Bundle extras) throws RemoteException {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void prepareFromUri(Uri uri, Bundle extras) throws RemoteException {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void play() throws RemoteException {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void playFromMediaId(String mediaId, Bundle extras) throws RemoteException {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void playFromSearch(String query, Bundle extras) throws RemoteException {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void playFromUri(Uri uri, Bundle extras) throws RemoteException {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void skipToQueueItem(long id) {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void pause() throws RemoteException {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void stop() throws RemoteException {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void next() throws RemoteException {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void previous() throws RemoteException {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void fastForward() throws RemoteException {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void rewind() throws RemoteException {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void seekTo(long pos) throws RemoteException {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void rate(RatingCompat rating) throws RemoteException {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void rateWithExtras(RatingCompat rating, Bundle extras) throws RemoteException {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void setCaptioningEnabled(boolean enabled) throws RemoteException {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void setRepeatMode(int repeatMode) throws RemoteException {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void setShuffleModeEnabledRemoved(boolean enabled) throws RemoteException {
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void setShuffleMode(int shuffleMode) throws RemoteException {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void sendCustomAction(String action, Bundle args) throws RemoteException {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public MediaMetadataCompat getMetadata() {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public PlaybackStateCompat getPlaybackState() {
                return MediaSessionCompat.getStateWithUpdatedPosition(MediaSessionImplApi21.this.mPlaybackState, MediaSessionImplApi21.this.mMetadata);
            }

            @Override // android.support.v4.media.session.IMediaSession
            public List<QueueItem> getQueue() {
                return null;
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void addQueueItem(MediaDescriptionCompat descriptionCompat) {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void addQueueItemAt(MediaDescriptionCompat descriptionCompat, int index) {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void removeQueueItem(MediaDescriptionCompat description) {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public void removeQueueItemAt(int index) {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public CharSequence getQueueTitle() {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public Bundle getExtras() {
                throw new AssertionError();
            }

            @Override // android.support.v4.media.session.IMediaSession
            public int getRatingType() {
                return MediaSessionImplApi21.this.mRatingType;
            }

            @Override // android.support.v4.media.session.IMediaSession
            public boolean isCaptioningEnabled() {
                return MediaSessionImplApi21.this.mCaptioningEnabled;
            }

            @Override // android.support.v4.media.session.IMediaSession
            public int getRepeatMode() {
                return MediaSessionImplApi21.this.mRepeatMode;
            }

            @Override // android.support.v4.media.session.IMediaSession
            public boolean isShuffleModeEnabledRemoved() {
                return false;
            }

            @Override // android.support.v4.media.session.IMediaSession
            public int getShuffleMode() {
                return MediaSessionImplApi21.this.mShuffleMode;
            }

            @Override // android.support.v4.media.session.IMediaSession
            public boolean isTransportControlEnabled() {
                throw new AssertionError();
            }
        }
    }

    @RequiresApi(28)
    /* loaded from: classes.dex */
    static class MediaSessionImplApi28 extends MediaSessionImplApi21 {
        private MediaSession mSession;

        MediaSessionImplApi28(Context context, String tag, SessionToken2 token2) {
            super(context, tag, token2);
        }

        MediaSessionImplApi28(Object mediaSession) {
            super(mediaSession);
            this.mSession = (MediaSession) mediaSession;
        }

        @Override // android.support.v4.media.session.MediaSessionCompat.MediaSessionImplApi21, android.support.v4.media.session.MediaSessionCompat.MediaSessionImpl
        @NonNull
        public final MediaSessionManager.RemoteUserInfo getCurrentControllerInfo() {
            MediaSessionManager.RemoteUserInfo info = this.mSession.getCurrentControllerInfo();
            return new MediaSessionManager.RemoteUserInfo(info.getPackageName(), info.getPid(), info.getUid());
        }
    }
}
