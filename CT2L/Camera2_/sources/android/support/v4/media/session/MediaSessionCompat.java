package android.support.v4.media.session;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
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
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.VolumeProviderCompat;
import android.support.v4.media.session.IMediaSession;
import android.support.v4.media.session.MediaSessionCompatApi14;
import android.support.v4.media.session.MediaSessionCompatApi21;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.view.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class MediaSessionCompat {
    public static final int FLAG_HANDLES_MEDIA_BUTTONS = 1;
    public static final int FLAG_HANDLES_TRANSPORT_CONTROLS = 2;
    private final ArrayList<OnActiveChangeListener> mActiveListeners = new ArrayList<>();
    private final MediaControllerCompat mController;
    private final MediaSessionImpl mImpl;

    interface MediaSessionImpl {
        Object getMediaSession();

        Object getRemoteControlClient();

        Token getSessionToken();

        boolean isActive();

        void release();

        void sendSessionEvent(String str, Bundle bundle);

        void setActive(boolean z);

        void setCallback(Callback callback, Handler handler);

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

        void setSessionActivity(PendingIntent pendingIntent);
    }

    public interface OnActiveChangeListener {
        void onActiveChanged();
    }

    public MediaSessionCompat(Context context, String tag, ComponentName mediaButtonEventReceiver, PendingIntent mbrIntent) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (TextUtils.isEmpty(tag)) {
            throw new IllegalArgumentException("tag must not be null or empty");
        }
        if (Build.VERSION.SDK_INT >= 21) {
            this.mImpl = new MediaSessionImplApi21(context, tag);
            this.mImpl.setMediaButtonReceiver(mbrIntent);
        } else {
            this.mImpl = new MediaSessionImplBase(context, tag, mediaButtonEventReceiver, mbrIntent);
        }
        this.mController = new MediaControllerCompat(context, this);
    }

    private MediaSessionCompat(Context context, MediaSessionImpl impl) {
        this.mImpl = impl;
        this.mController = new MediaControllerCompat(context, this);
    }

    public void setCallback(Callback callback) {
        setCallback(callback, null);
    }

    public void setCallback(Callback callback, Handler handler) {
        MediaSessionImpl mediaSessionImpl = this.mImpl;
        if (handler == null) {
            handler = new Handler();
        }
        mediaSessionImpl.setCallback(callback, handler);
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
        for (OnActiveChangeListener listener : this.mActiveListeners) {
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

    public void setExtras(Bundle extras) {
        this.mImpl.setExtras(extras);
    }

    public Object getMediaSession() {
        return this.mImpl.getMediaSession();
    }

    public Object getRemoteControlClient() {
        return this.mImpl.getRemoteControlClient();
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

    public static MediaSessionCompat obtain(Context context, Object mediaSession) {
        return new MediaSessionCompat(context, new MediaSessionImplApi21(mediaSession));
    }

    public static abstract class Callback {
        final Object mCallbackObj;

        public Callback() {
            if (Build.VERSION.SDK_INT >= 21) {
                this.mCallbackObj = MediaSessionCompatApi21.createCallback(new StubApi21());
            } else {
                this.mCallbackObj = null;
            }
        }

        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
        }

        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            return false;
        }

        public void onPlay() {
        }

        public void onPlayFromMediaId(String mediaId, Bundle extras) {
        }

        public void onPlayFromSearch(String query, Bundle extras) {
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

        public void onCustomAction(String action, Bundle extras) {
        }

        private class StubApi21 implements MediaSessionCompatApi21.Callback {
            private StubApi21() {
            }

            @Override
            public void onCommand(String command, Bundle extras, ResultReceiver cb) {
                Callback.this.onCommand(command, extras, cb);
            }

            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                return Callback.this.onMediaButtonEvent(mediaButtonIntent);
            }

            @Override
            public void onPlay() {
                Callback.this.onPlay();
            }

            @Override
            public void onPlayFromMediaId(String mediaId, Bundle extras) {
                Callback.this.onPlayFromMediaId(mediaId, extras);
            }

            @Override
            public void onPlayFromSearch(String search, Bundle extras) {
                Callback.this.onPlayFromSearch(search, extras);
            }

            @Override
            public void onSkipToQueueItem(long id) {
                Callback.this.onSkipToQueueItem(id);
            }

            @Override
            public void onPause() {
                Callback.this.onPause();
            }

            @Override
            public void onSkipToNext() {
                Callback.this.onSkipToNext();
            }

            @Override
            public void onSkipToPrevious() {
                Callback.this.onSkipToPrevious();
            }

            @Override
            public void onFastForward() {
                Callback.this.onFastForward();
            }

            @Override
            public void onRewind() {
                Callback.this.onRewind();
            }

            @Override
            public void onStop() {
                Callback.this.onStop();
            }

            @Override
            public void onSeekTo(long pos) {
                Callback.this.onSeekTo(pos);
            }

            @Override
            public void onSetRating(Object ratingObj) {
                Callback.this.onSetRating(RatingCompat.fromRating(ratingObj));
            }

            @Override
            public void onCustomAction(String action, Bundle extras) {
                Callback.this.onCustomAction(action, extras);
            }
        }
    }

    public static final class Token implements Parcelable {
        public static final Parcelable.Creator<Token> CREATOR = new Parcelable.Creator<Token>() {
            @Override
            public Token createFromParcel(Parcel in) {
                Object strongBinder;
                if (Build.VERSION.SDK_INT >= 21) {
                    strongBinder = in.readParcelable(null);
                } else {
                    strongBinder = in.readStrongBinder();
                }
                return new Token(strongBinder);
            }

            @Override
            public Token[] newArray(int size) {
                return new Token[size];
            }
        };
        private final Object mInner;

        Token(Object inner) {
            this.mInner = inner;
        }

        public static Token fromToken(Object token) {
            if (token == null || Build.VERSION.SDK_INT < 21) {
                return null;
            }
            return new Token(MediaSessionCompatApi21.verifyToken(token));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            if (Build.VERSION.SDK_INT >= 21) {
                dest.writeParcelable((Parcelable) this.mInner, flags);
            } else {
                dest.writeStrongBinder((IBinder) this.mInner);
            }
        }

        public Object getToken() {
            return this.mInner;
        }
    }

    public static final class QueueItem implements Parcelable {
        public static final Parcelable.Creator<QueueItem> CREATOR = new Parcelable.Creator<QueueItem>() {
            @Override
            public QueueItem createFromParcel(Parcel p) {
                return new QueueItem(p);
            }

            @Override
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

        private QueueItem(Parcel in) {
            this.mDescription = MediaDescriptionCompat.CREATOR.createFromParcel(in);
            this.mId = in.readLong();
        }

        public MediaDescriptionCompat getDescription() {
            return this.mDescription;
        }

        public long getQueueId() {
            return this.mId;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            this.mDescription.writeToParcel(dest, flags);
            dest.writeLong(this.mId);
        }

        @Override
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

        public static QueueItem obtain(Object queueItem) {
            Object descriptionObj = MediaSessionCompatApi21.QueueItem.getDescription(queueItem);
            MediaDescriptionCompat description = MediaDescriptionCompat.fromMediaDescription(descriptionObj);
            long id = MediaSessionCompatApi21.QueueItem.getQueueId(queueItem);
            return new QueueItem(queueItem, description, id);
        }

        public String toString() {
            return "MediaSession.QueueItem {Description=" + this.mDescription + ", Id=" + this.mId + " }";
        }
    }

    static final class ResultReceiverWrapper implements Parcelable {
        public static final Parcelable.Creator<ResultReceiverWrapper> CREATOR = new Parcelable.Creator<ResultReceiverWrapper>() {
            @Override
            public ResultReceiverWrapper createFromParcel(Parcel p) {
                return new ResultReceiverWrapper(p);
            }

            @Override
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

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            this.mResultReceiver.writeToParcel(dest, flags);
        }
    }

    static class MediaSessionImplBase implements MediaSessionImpl {
        private final AudioManager mAudioManager;
        private Callback mCallback;
        private final ComponentName mComponentName;
        private final Context mContext;
        private Bundle mExtras;
        private int mFlags;
        private final MessageHandler mHandler;
        private int mLocalStream;
        private final PendingIntent mMediaButtonEventReceiver;
        private MediaMetadataCompat mMetadata;
        private final String mPackageName;
        private List<QueueItem> mQueue;
        private CharSequence mQueueTitle;
        private int mRatingType;
        private final Object mRccObj;
        private PendingIntent mSessionActivity;
        private PlaybackStateCompat mState;
        private final MediaSessionStub mStub;
        private final String mTag;
        private final Token mToken;
        private VolumeProviderCompat mVolumeProvider;
        private int mVolumeType;
        private final Object mLock = new Object();
        private final RemoteCallbackList<IMediaControllerCallback> mControllerCallbacks = new RemoteCallbackList<>();
        private boolean mDestroyed = false;
        private boolean mIsActive = false;
        private boolean mIsRccRegistered = false;
        private boolean mIsMbrRegistered = false;
        private VolumeProviderCompat.Callback mVolumeCallback = new VolumeProviderCompat.Callback() {
            @Override
            public void onVolumeChanged(VolumeProviderCompat volumeProvider) {
                if (MediaSessionImplBase.this.mVolumeProvider == volumeProvider) {
                    ParcelableVolumeInfo info = new ParcelableVolumeInfo(MediaSessionImplBase.this.mVolumeType, MediaSessionImplBase.this.mLocalStream, volumeProvider.getVolumeControl(), volumeProvider.getMaxVolume(), volumeProvider.getCurrentVolume());
                    MediaSessionImplBase.this.sendVolumeInfoChanged(info);
                }
            }
        };

        public MediaSessionImplBase(Context context, String tag, ComponentName mbrComponent, PendingIntent mbr) {
            if (mbrComponent == null) {
                throw new IllegalArgumentException("MediaButtonReceiver component may not be null.");
            }
            if (mbr == null) {
                Intent mediaButtonIntent = new Intent("android.intent.action.MEDIA_BUTTON");
                mediaButtonIntent.setComponent(mbrComponent);
                mbr = PendingIntent.getBroadcast(context, 0, mediaButtonIntent, 0);
            }
            this.mContext = context;
            this.mPackageName = context.getPackageName();
            this.mAudioManager = (AudioManager) context.getSystemService("audio");
            this.mTag = tag;
            this.mComponentName = mbrComponent;
            this.mMediaButtonEventReceiver = mbr;
            this.mStub = new MediaSessionStub();
            this.mToken = new Token(this.mStub);
            this.mHandler = new MessageHandler(Looper.myLooper());
            this.mRatingType = 0;
            this.mVolumeType = 1;
            this.mLocalStream = 3;
            if (Build.VERSION.SDK_INT >= 14) {
                this.mRccObj = MediaSessionCompatApi14.createRemoteControlClient(mbr);
            } else {
                this.mRccObj = null;
            }
        }

        @Override
        public void setCallback(final Callback callback, Handler handler) {
            if (callback != this.mCallback) {
                if (callback == null || Build.VERSION.SDK_INT < 18) {
                    if (Build.VERSION.SDK_INT >= 18) {
                        MediaSessionCompatApi18.setOnPlaybackPositionUpdateListener(this.mRccObj, null);
                    }
                    if (Build.VERSION.SDK_INT >= 19) {
                        MediaSessionCompatApi19.setOnMetadataUpdateListener(this.mRccObj, null);
                    }
                } else {
                    if (handler == null) {
                        new Handler();
                    }
                    MediaSessionCompatApi14.Callback cb14 = new MediaSessionCompatApi14.Callback() {
                        @Override
                        public void onStop() {
                            callback.onStop();
                        }

                        @Override
                        public void onSkipToPrevious() {
                            callback.onSkipToPrevious();
                        }

                        @Override
                        public void onSkipToNext() {
                            callback.onSkipToNext();
                        }

                        @Override
                        public void onSetRating(Object ratingObj) {
                            callback.onSetRating(RatingCompat.fromRating(ratingObj));
                        }

                        @Override
                        public void onSeekTo(long pos) {
                            callback.onSeekTo(pos);
                        }

                        @Override
                        public void onRewind() {
                            callback.onRewind();
                        }

                        @Override
                        public void onPlay() {
                            callback.onPlay();
                        }

                        @Override
                        public void onPause() {
                            callback.onPause();
                        }

                        @Override
                        public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                            return callback.onMediaButtonEvent(mediaButtonIntent);
                        }

                        @Override
                        public void onFastForward() {
                            callback.onFastForward();
                        }

                        @Override
                        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
                            callback.onCommand(command, extras, cb);
                        }
                    };
                    if (Build.VERSION.SDK_INT >= 18) {
                        Object onPositionUpdateObj = MediaSessionCompatApi18.createPlaybackPositionUpdateListener(cb14);
                        MediaSessionCompatApi18.setOnPlaybackPositionUpdateListener(this.mRccObj, onPositionUpdateObj);
                    }
                    if (Build.VERSION.SDK_INT >= 19) {
                        Object onMetadataUpdateObj = MediaSessionCompatApi19.createMetadataUpdateListener(cb14);
                        MediaSessionCompatApi19.setOnMetadataUpdateListener(this.mRccObj, onMetadataUpdateObj);
                    }
                }
                this.mCallback = callback;
            }
        }

        @Override
        public void setFlags(int flags) {
            synchronized (this.mLock) {
                this.mFlags = flags;
            }
            update();
        }

        @Override
        public void setPlaybackToLocal(int stream) {
            if (this.mVolumeProvider != null) {
                this.mVolumeProvider.setCallback(null);
            }
            this.mVolumeType = 1;
            ParcelableVolumeInfo info = new ParcelableVolumeInfo(this.mVolumeType, this.mLocalStream, 2, this.mAudioManager.getStreamMaxVolume(this.mLocalStream), this.mAudioManager.getStreamVolume(this.mLocalStream));
            sendVolumeInfoChanged(info);
        }

        @Override
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

        @Override
        public void setActive(boolean active) {
            if (active != this.mIsActive) {
                this.mIsActive = active;
                if (update()) {
                    setMetadata(this.mMetadata);
                    setPlaybackState(this.mState);
                }
            }
        }

        @Override
        public boolean isActive() {
            return this.mIsActive;
        }

        @Override
        public void sendSessionEvent(String event, Bundle extras) {
            sendEvent(event, extras);
        }

        @Override
        public void release() {
            this.mIsActive = false;
            this.mDestroyed = true;
            update();
            sendSessionDestroyed();
        }

        @Override
        public Token getSessionToken() {
            return this.mToken;
        }

        @Override
        public void setPlaybackState(PlaybackStateCompat state) {
            synchronized (this.mLock) {
                this.mState = state;
            }
            sendState(state);
            if (this.mIsActive) {
                if (state == null) {
                    if (Build.VERSION.SDK_INT >= 14) {
                        MediaSessionCompatApi14.setState(this.mRccObj, 0);
                    }
                } else if (Build.VERSION.SDK_INT >= 18) {
                    MediaSessionCompatApi18.setState(this.mRccObj, state.getState(), state.getPosition(), state.getPlaybackSpeed(), state.getLastPositionUpdateTime());
                } else if (Build.VERSION.SDK_INT >= 14) {
                    MediaSessionCompatApi14.setState(this.mRccObj, state.getState());
                }
            }
        }

        @Override
        public void setMetadata(MediaMetadataCompat metadata) {
            synchronized (this.mLock) {
                this.mMetadata = metadata;
            }
            sendMetadata(metadata);
            if (this.mIsActive) {
                if (Build.VERSION.SDK_INT >= 19) {
                    boolean canRate = (this.mState == null || (this.mState.getActions() & 128) == 0) ? false : true;
                    MediaSessionCompatApi19.setMetadata(this.mRccObj, metadata != null ? metadata.getBundle() : null, canRate);
                } else if (Build.VERSION.SDK_INT >= 14) {
                    MediaSessionCompatApi14.setMetadata(this.mRccObj, metadata != null ? metadata.getBundle() : null);
                }
            }
        }

        @Override
        public void setSessionActivity(PendingIntent pi) {
            synchronized (this.mLock) {
                this.mSessionActivity = pi;
            }
        }

        @Override
        public void setMediaButtonReceiver(PendingIntent mbr) {
        }

        @Override
        public void setQueue(List<QueueItem> queue) {
            this.mQueue = queue;
            sendQueue(queue);
        }

        @Override
        public void setQueueTitle(CharSequence title) {
            this.mQueueTitle = title;
            sendQueueTitle(title);
        }

        @Override
        public Object getMediaSession() {
            return null;
        }

        @Override
        public Object getRemoteControlClient() {
            return this.mRccObj;
        }

        @Override
        public void setRatingType(int type) {
            this.mRatingType = type;
        }

        @Override
        public void setExtras(Bundle extras) {
            this.mExtras = extras;
        }

        private boolean update() {
            if (this.mIsActive) {
                if (Build.VERSION.SDK_INT >= 8) {
                    if (!this.mIsMbrRegistered && (this.mFlags & 1) != 0) {
                        if (Build.VERSION.SDK_INT >= 18) {
                            MediaSessionCompatApi18.registerMediaButtonEventReceiver(this.mContext, this.mMediaButtonEventReceiver);
                        } else {
                            MediaSessionCompatApi8.registerMediaButtonEventReceiver(this.mContext, this.mComponentName);
                        }
                        this.mIsMbrRegistered = true;
                    } else if (this.mIsMbrRegistered && (this.mFlags & 1) == 0) {
                        if (Build.VERSION.SDK_INT >= 18) {
                            MediaSessionCompatApi18.unregisterMediaButtonEventReceiver(this.mContext, this.mMediaButtonEventReceiver);
                        } else {
                            MediaSessionCompatApi8.unregisterMediaButtonEventReceiver(this.mContext, this.mComponentName);
                        }
                        this.mIsMbrRegistered = false;
                    }
                }
                if (Build.VERSION.SDK_INT < 14) {
                    return false;
                }
                if (!this.mIsRccRegistered && (this.mFlags & 2) != 0) {
                    MediaSessionCompatApi14.registerRemoteControlClient(this.mContext, this.mRccObj);
                    this.mIsRccRegistered = true;
                    return true;
                }
                if (!this.mIsRccRegistered || (this.mFlags & 2) != 0) {
                    return false;
                }
                MediaSessionCompatApi14.unregisterRemoteControlClient(this.mContext, this.mRccObj);
                this.mIsRccRegistered = false;
                return false;
            }
            if (this.mIsMbrRegistered) {
                if (Build.VERSION.SDK_INT >= 18) {
                    MediaSessionCompatApi18.unregisterMediaButtonEventReceiver(this.mContext, this.mMediaButtonEventReceiver);
                } else {
                    MediaSessionCompatApi8.unregisterMediaButtonEventReceiver(this.mContext, this.mComponentName);
                }
                this.mIsMbrRegistered = false;
            }
            if (!this.mIsRccRegistered) {
                return false;
            }
            MediaSessionCompatApi14.unregisterRemoteControlClient(this.mContext, this.mRccObj);
            this.mIsRccRegistered = false;
            return false;
        }

        private void adjustVolume(int direction, int flags) {
            if (this.mVolumeType == 2) {
                if (this.mVolumeProvider != null) {
                    this.mVolumeProvider.onAdjustVolume(direction);
                    return;
                }
                return;
            }
            this.mAudioManager.adjustStreamVolume(direction, this.mLocalStream, flags);
        }

        private void setVolumeTo(int value, int flags) {
            if (this.mVolumeType == 2) {
                if (this.mVolumeProvider != null) {
                    this.mVolumeProvider.onSetVolumeTo(value);
                    return;
                }
                return;
            }
            this.mAudioManager.setStreamVolume(this.mLocalStream, value, flags);
        }

        private PlaybackStateCompat getStateWithUpdatedPosition() {
            PlaybackStateCompat state;
            long duration = -1;
            synchronized (this.mLock) {
                state = this.mState;
                if (this.mMetadata != null && this.mMetadata.containsKey(MediaMetadataCompat.METADATA_KEY_DURATION)) {
                    duration = this.mMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
                }
            }
            PlaybackStateCompat result = null;
            if (state != null && (state.getState() == 3 || state.getState() == 4 || state.getState() == 5)) {
                long updateTime = state.getLastPositionUpdateTime();
                long currentTime = SystemClock.elapsedRealtime();
                if (updateTime > 0) {
                    long position = ((long) (state.getPlaybackSpeed() * (currentTime - updateTime))) + state.getPosition();
                    if (duration >= 0 && position > duration) {
                        position = duration;
                    } else if (position < 0) {
                        position = 0;
                    }
                    PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder(state);
                    builder.setState(state.getState(), position, state.getPlaybackSpeed(), currentTime);
                    result = builder.build();
                }
            }
            return result == null ? state : result;
        }

        private void sendVolumeInfoChanged(ParcelableVolumeInfo info) {
            int size = this.mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = (IMediaControllerCallback) this.mControllerCallbacks.getBroadcastItem(i);
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
                IMediaControllerCallback cb = (IMediaControllerCallback) this.mControllerCallbacks.getBroadcastItem(i);
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
                IMediaControllerCallback cb = (IMediaControllerCallback) this.mControllerCallbacks.getBroadcastItem(i);
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
                IMediaControllerCallback cb = (IMediaControllerCallback) this.mControllerCallbacks.getBroadcastItem(i);
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
                IMediaControllerCallback cb = (IMediaControllerCallback) this.mControllerCallbacks.getBroadcastItem(i);
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
                IMediaControllerCallback cb = (IMediaControllerCallback) this.mControllerCallbacks.getBroadcastItem(i);
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
                IMediaControllerCallback cb = (IMediaControllerCallback) this.mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onQueueTitleChanged(queueTitle);
                } catch (RemoteException e) {
                }
            }
            this.mControllerCallbacks.finishBroadcast();
        }

        class MediaSessionStub extends IMediaSession.Stub {
            MediaSessionStub() {
            }

            @Override
            public void sendCommand(String command, Bundle args, ResultReceiverWrapper cb) {
                MediaSessionImplBase.this.mHandler.post(15, new Command(command, args, cb.mResultReceiver));
            }

            @Override
            public boolean sendMediaButton(KeyEvent mediaButton) {
                boolean handlesMediaButtons = (MediaSessionImplBase.this.mFlags & 1) != 0;
                if (handlesMediaButtons) {
                    MediaSessionImplBase.this.mHandler.post(14, mediaButton);
                }
                return handlesMediaButtons;
            }

            @Override
            public void registerCallbackListener(IMediaControllerCallback cb) {
                if (!MediaSessionImplBase.this.mDestroyed) {
                    MediaSessionImplBase.this.mControllerCallbacks.register(cb);
                } else {
                    try {
                        cb.onSessionDestroyed();
                    } catch (Exception e) {
                    }
                }
            }

            @Override
            public void unregisterCallbackListener(IMediaControllerCallback cb) {
                MediaSessionImplBase.this.mControllerCallbacks.unregister(cb);
            }

            @Override
            public String getPackageName() {
                return MediaSessionImplBase.this.mPackageName;
            }

            @Override
            public String getTag() {
                return MediaSessionImplBase.this.mTag;
            }

            @Override
            public PendingIntent getLaunchPendingIntent() {
                PendingIntent pendingIntent;
                synchronized (MediaSessionImplBase.this.mLock) {
                    pendingIntent = MediaSessionImplBase.this.mSessionActivity;
                }
                return pendingIntent;
            }

            @Override
            public long getFlags() {
                long j;
                synchronized (MediaSessionImplBase.this.mLock) {
                    j = MediaSessionImplBase.this.mFlags;
                }
                return j;
            }

            @Override
            public ParcelableVolumeInfo getVolumeAttributes() {
                int volumeType;
                int stream;
                int controlType;
                int max;
                int current;
                synchronized (MediaSessionImplBase.this.mLock) {
                    volumeType = MediaSessionImplBase.this.mVolumeType;
                    stream = MediaSessionImplBase.this.mLocalStream;
                    VolumeProviderCompat vp = MediaSessionImplBase.this.mVolumeProvider;
                    if (volumeType == 2) {
                        controlType = vp.getVolumeControl();
                        max = vp.getMaxVolume();
                        current = vp.getCurrentVolume();
                    } else {
                        controlType = 2;
                        max = MediaSessionImplBase.this.mAudioManager.getStreamMaxVolume(stream);
                        current = MediaSessionImplBase.this.mAudioManager.getStreamVolume(stream);
                    }
                }
                return new ParcelableVolumeInfo(volumeType, stream, controlType, max, current);
            }

            @Override
            public void adjustVolume(int direction, int flags, String packageName) {
                MediaSessionImplBase.this.adjustVolume(direction, flags);
            }

            @Override
            public void setVolumeTo(int value, int flags, String packageName) {
                MediaSessionImplBase.this.setVolumeTo(value, flags);
            }

            @Override
            public void play() throws RemoteException {
                MediaSessionImplBase.this.mHandler.post(1);
            }

            @Override
            public void playFromMediaId(String mediaId, Bundle extras) throws RemoteException {
                MediaSessionImplBase.this.mHandler.post(2, mediaId, extras);
            }

            @Override
            public void playFromSearch(String query, Bundle extras) throws RemoteException {
                MediaSessionImplBase.this.mHandler.post(3, query, extras);
            }

            @Override
            public void skipToQueueItem(long id) {
                MediaSessionImplBase.this.mHandler.post(4, Long.valueOf(id));
            }

            @Override
            public void pause() throws RemoteException {
                MediaSessionImplBase.this.mHandler.post(5);
            }

            @Override
            public void stop() throws RemoteException {
                MediaSessionImplBase.this.mHandler.post(6);
            }

            @Override
            public void next() throws RemoteException {
                MediaSessionImplBase.this.mHandler.post(7);
            }

            @Override
            public void previous() throws RemoteException {
                MediaSessionImplBase.this.mHandler.post(8);
            }

            @Override
            public void fastForward() throws RemoteException {
                MediaSessionImplBase.this.mHandler.post(9);
            }

            @Override
            public void rewind() throws RemoteException {
                MediaSessionImplBase.this.mHandler.post(10);
            }

            @Override
            public void seekTo(long pos) throws RemoteException {
                MediaSessionImplBase.this.mHandler.post(11, Long.valueOf(pos));
            }

            @Override
            public void rate(RatingCompat rating) throws RemoteException {
                MediaSessionImplBase.this.mHandler.post(12, rating);
            }

            @Override
            public void sendCustomAction(String action, Bundle args) throws RemoteException {
                MediaSessionImplBase.this.mHandler.post(13, action, args);
            }

            @Override
            public MediaMetadataCompat getMetadata() {
                return MediaSessionImplBase.this.mMetadata;
            }

            @Override
            public PlaybackStateCompat getPlaybackState() {
                return MediaSessionImplBase.this.getStateWithUpdatedPosition();
            }

            @Override
            public List<QueueItem> getQueue() {
                List<QueueItem> list;
                synchronized (MediaSessionImplBase.this.mLock) {
                    list = MediaSessionImplBase.this.mQueue;
                }
                return list;
            }

            @Override
            public CharSequence getQueueTitle() {
                return MediaSessionImplBase.this.mQueueTitle;
            }

            @Override
            public Bundle getExtras() {
                Bundle bundle;
                synchronized (MediaSessionImplBase.this.mLock) {
                    bundle = MediaSessionImplBase.this.mExtras;
                }
                return bundle;
            }

            @Override
            public int getRatingType() {
                return MediaSessionImplBase.this.mRatingType;
            }

            @Override
            public boolean isTransportControlEnabled() {
                return (MediaSessionImplBase.this.mFlags & 2) != 0;
            }
        }

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

        private class MessageHandler extends Handler {
            private static final int MSG_ADJUST_VOLUME = 16;
            private static final int MSG_COMMAND = 15;
            private static final int MSG_CUSTOM_ACTION = 13;
            private static final int MSG_FAST_FORWARD = 9;
            private static final int MSG_MEDIA_BUTTON = 14;
            private static final int MSG_NEXT = 7;
            private static final int MSG_PAUSE = 5;
            private static final int MSG_PLAY = 1;
            private static final int MSG_PLAY_MEDIA_ID = 2;
            private static final int MSG_PLAY_SEARCH = 3;
            private static final int MSG_PREVIOUS = 8;
            private static final int MSG_RATE = 12;
            private static final int MSG_REWIND = 10;
            private static final int MSG_SEEK_TO = 11;
            private static final int MSG_SET_VOLUME = 17;
            private static final int MSG_SKIP_TO_ITEM = 4;
            private static final int MSG_STOP = 6;

            public MessageHandler(Looper looper) {
                super(looper);
            }

            public void post(int what, Object obj, Bundle bundle) {
                Message msg = obtainMessage(what, obj);
                msg.setData(bundle);
                msg.sendToTarget();
            }

            public void post(int what, Object obj) {
                obtainMessage(what, obj).sendToTarget();
            }

            public void post(int what) {
                post(what, null);
            }

            public void post(int what, Object obj, int arg1) {
                obtainMessage(what, arg1, 0, obj).sendToTarget();
            }

            @Override
            public void handleMessage(Message msg) {
                if (MediaSessionImplBase.this.mCallback != null) {
                    switch (msg.what) {
                        case 1:
                            MediaSessionImplBase.this.mCallback.onPlay();
                            break;
                        case 2:
                            MediaSessionImplBase.this.mCallback.onPlayFromMediaId((String) msg.obj, msg.getData());
                            break;
                        case 3:
                            MediaSessionImplBase.this.mCallback.onPlayFromSearch((String) msg.obj, msg.getData());
                            break;
                        case 4:
                            MediaSessionImplBase.this.mCallback.onSkipToQueueItem(((Long) msg.obj).longValue());
                            break;
                        case 5:
                            MediaSessionImplBase.this.mCallback.onPause();
                            break;
                        case 6:
                            MediaSessionImplBase.this.mCallback.onStop();
                            break;
                        case 7:
                            MediaSessionImplBase.this.mCallback.onSkipToNext();
                            break;
                        case 8:
                            MediaSessionImplBase.this.mCallback.onSkipToPrevious();
                            break;
                        case 9:
                            MediaSessionImplBase.this.mCallback.onFastForward();
                            break;
                        case 10:
                            MediaSessionImplBase.this.mCallback.onRewind();
                            break;
                        case 11:
                            MediaSessionImplBase.this.mCallback.onSeekTo(((Long) msg.obj).longValue());
                            break;
                        case 12:
                            MediaSessionImplBase.this.mCallback.onSetRating((RatingCompat) msg.obj);
                            break;
                        case 13:
                            MediaSessionImplBase.this.mCallback.onCustomAction((String) msg.obj, msg.getData());
                            break;
                        case MSG_MEDIA_BUTTON:
                            MediaSessionImplBase.this.mCallback.onMediaButtonEvent((Intent) msg.obj);
                            break;
                        case 15:
                            Command cmd = (Command) msg.obj;
                            MediaSessionImplBase.this.mCallback.onCommand(cmd.command, cmd.extras, cmd.stub);
                            break;
                        case 16:
                            MediaSessionImplBase.this.adjustVolume(((Integer) msg.obj).intValue(), 0);
                            break;
                        case MSG_SET_VOLUME:
                            MediaSessionImplBase.this.setVolumeTo(((Integer) msg.obj).intValue(), 0);
                            break;
                    }
                }
            }
        }
    }

    static class MediaSessionImplApi21 implements MediaSessionImpl {
        private PendingIntent mMediaButtonIntent;
        private final Object mSessionObj;
        private final Token mToken;

        public MediaSessionImplApi21(Context context, String tag) {
            this.mSessionObj = MediaSessionCompatApi21.createSession(context, tag);
            this.mToken = new Token(MediaSessionCompatApi21.getSessionToken(this.mSessionObj));
        }

        public MediaSessionImplApi21(Object mediaSession) {
            this.mSessionObj = MediaSessionCompatApi21.verifySession(mediaSession);
            this.mToken = new Token(MediaSessionCompatApi21.getSessionToken(this.mSessionObj));
        }

        @Override
        public void setCallback(Callback callback, Handler handler) {
            MediaSessionCompatApi21.setCallback(this.mSessionObj, callback.mCallbackObj, handler);
        }

        @Override
        public void setFlags(int flags) {
            MediaSessionCompatApi21.setFlags(this.mSessionObj, flags);
        }

        @Override
        public void setPlaybackToLocal(int stream) {
            MediaSessionCompatApi21.setPlaybackToLocal(this.mSessionObj, stream);
        }

        @Override
        public void setPlaybackToRemote(VolumeProviderCompat volumeProvider) {
            MediaSessionCompatApi21.setPlaybackToRemote(this.mSessionObj, volumeProvider.getVolumeProvider());
        }

        @Override
        public void setActive(boolean active) {
            MediaSessionCompatApi21.setActive(this.mSessionObj, active);
        }

        @Override
        public boolean isActive() {
            return MediaSessionCompatApi21.isActive(this.mSessionObj);
        }

        @Override
        public void sendSessionEvent(String event, Bundle extras) {
            MediaSessionCompatApi21.sendSessionEvent(this.mSessionObj, event, extras);
        }

        @Override
        public void release() {
            MediaSessionCompatApi21.release(this.mSessionObj);
        }

        @Override
        public Token getSessionToken() {
            return this.mToken;
        }

        @Override
        public void setPlaybackState(PlaybackStateCompat state) {
            MediaSessionCompatApi21.setPlaybackState(this.mSessionObj, state.getPlaybackState());
        }

        @Override
        public void setMetadata(MediaMetadataCompat metadata) {
            MediaSessionCompatApi21.setMetadata(this.mSessionObj, metadata.getMediaMetadata());
        }

        @Override
        public void setSessionActivity(PendingIntent pi) {
            MediaSessionCompatApi21.setSessionActivity(this.mSessionObj, pi);
        }

        @Override
        public void setMediaButtonReceiver(PendingIntent mbr) {
            this.mMediaButtonIntent = mbr;
            MediaSessionCompatApi21.setMediaButtonReceiver(this.mSessionObj, mbr);
        }

        @Override
        public void setQueue(List<QueueItem> queue) {
            List<Object> queueObjs = null;
            if (queue != null) {
                queueObjs = new ArrayList<>();
                for (QueueItem item : queue) {
                    queueObjs.add(item.getQueueItem());
                }
            }
            MediaSessionCompatApi21.setQueue(this.mSessionObj, queueObjs);
        }

        @Override
        public void setQueueTitle(CharSequence title) {
            MediaSessionCompatApi21.setQueueTitle(this.mSessionObj, title);
        }

        @Override
        public void setRatingType(int type) {
            if (Build.VERSION.SDK_INT >= 22) {
                MediaSessionCompatApi22.setRatingType(this.mSessionObj, type);
            }
        }

        @Override
        public void setExtras(Bundle extras) {
            MediaSessionCompatApi21.setExtras(this.mSessionObj, extras);
        }

        @Override
        public Object getMediaSession() {
            return this.mSessionObj;
        }

        @Override
        public Object getRemoteControlClient() {
            return null;
        }
    }
}
