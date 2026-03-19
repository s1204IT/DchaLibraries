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
import android.net.Uri;
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
import android.view.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.List;

public final class MediaSession {
    public static final int FLAG_EXCLUSIVE_GLOBAL_PRIORITY = 65536;
    public static final int FLAG_HANDLES_MEDIA_BUTTONS = 1;
    public static final int FLAG_HANDLES_TRANSPORT_CONTROLS = 2;
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

    public MediaSession(Context context, String tag) {
        this(context, tag, UserHandle.myUserId());
    }

    public MediaSession(Context context, String tag, int userId) {
        this.mLock = new Object();
        this.mActive = false;
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null.");
        }
        if (TextUtils.isEmpty(tag)) {
            throw new IllegalArgumentException("tag cannot be null or empty");
        }
        this.mMaxBitmapSize = context.getResources().getDimensionPixelSize(17104916);
        this.mCbStub = new CallbackStub(this);
        MediaSessionManager manager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        try {
            this.mBinder = manager.createSession(this.mCbStub, tag, userId);
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
            if (callback == null) {
                if (this.mCallback != null) {
                    this.mCallback.mCallback.mSession = null;
                }
                this.mCallback = null;
                return;
            }
            if (this.mCallback != null) {
                this.mCallback.mCallback.mSession = null;
            }
            if (handler == null) {
                handler = new Handler();
            }
            callback.mSession = this;
            CallbackMessageHandler msgHandler = new CallbackMessageHandler(handler.getLooper(), callback);
            this.mCallback = msgHandler;
        }
    }

    public void setSessionActivity(PendingIntent pi) {
        try {
            this.mBinder.setLaunchPendingIntent(pi);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Failure in setLaunchPendingIntent.", e);
        }
    }

    public void setMediaButtonReceiver(PendingIntent mbr) {
        try {
            this.mBinder.setMediaButtonReceiver(mbr);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Failure in setMediaButtonReceiver.", e);
        }
    }

    public void setFlags(int flags) {
        try {
            this.mBinder.setFlags(flags);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Failure in setFlags.", e);
        }
    }

    public void setPlaybackToLocal(AudioAttributes attributes) {
        if (attributes == null) {
            throw new IllegalArgumentException("Attributes cannot be null for local playback.");
        }
        try {
            this.mBinder.setPlaybackToLocal(attributes);
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

    public void setActive(boolean active) {
        if (this.mActive == active) {
            return;
        }
        try {
            this.mBinder.setActive(active);
            this.mActive = active;
        } catch (RemoteException e) {
            Log.wtf(TAG, "Failure in setActive.", e);
        }
    }

    public boolean isActive() {
        return this.mActive;
    }

    public void sendSessionEvent(String event, Bundle extras) {
        if (TextUtils.isEmpty(event)) {
            throw new IllegalArgumentException("event cannot be null or empty");
        }
        try {
            this.mBinder.sendEvent(event, extras);
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

    public void setPlaybackState(PlaybackState state) {
        this.mPlaybackState = state;
        try {
            this.mBinder.setPlaybackState(state);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Dead object in setPlaybackState.", e);
        }
    }

    public void setMetadata(MediaMetadata metadata) {
        if (metadata != null) {
            metadata = new MediaMetadata.Builder(metadata, this.mMaxBitmapSize).build();
        }
        try {
            this.mBinder.setMetadata(metadata);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Dead object in setPlaybackState.", e);
        }
    }

    public void setQueue(List<QueueItem> queue) {
        try {
            this.mBinder.setQueue(queue != null ? new ParceledListSlice(queue) : null);
        } catch (RemoteException e) {
            Log.wtf("Dead object in setQueue.", e);
        }
    }

    public void setQueueTitle(CharSequence title) {
        try {
            this.mBinder.setQueueTitle(title);
        } catch (RemoteException e) {
            Log.wtf("Dead object in setQueueTitle.", e);
        }
    }

    public void setRatingType(int type) {
        try {
            this.mBinder.setRatingType(type);
        } catch (RemoteException e) {
            Log.e(TAG, "Error in setRatingType.", e);
        }
    }

    public void setExtras(Bundle extras) {
        try {
            this.mBinder.setExtras(extras);
        } catch (RemoteException e) {
            Log.wtf("Dead object in setExtras.", e);
        }
    }

    public void notifyRemoteVolumeChanged(VolumeProvider provider) {
        synchronized (this.mLock) {
            if (provider != null) {
                if (provider == this.mVolumeProvider) {
                    try {
                        this.mBinder.setCurrentVolume(provider.getCurrentVolume());
                        return;
                    } catch (RemoteException e) {
                        Log.e(TAG, "Error in notifyVolumeChanged", e);
                        return;
                    }
                }
                Log.w(TAG, "Received update from stale volume provider");
                return;
            }
            Log.w(TAG, "Received update from stale volume provider");
            return;
        }
    }

    public String getCallingPackage() {
        try {
            return this.mBinder.getCallingPackage();
        } catch (RemoteException e) {
            Log.wtf(TAG, "Dead object in getCallingPackage.", e);
            return null;
        }
    }

    private void dispatchPrepare() {
        postToCallback(3);
    }

    private void dispatchPrepareFromMediaId(String mediaId, Bundle extras) {
        postToCallback(4, mediaId, extras);
    }

    private void dispatchPrepareFromSearch(String query, Bundle extras) {
        postToCallback(5, query, extras);
    }

    private void dispatchPrepareFromUri(Uri uri, Bundle extras) {
        postToCallback(6, uri, extras);
    }

    private void dispatchPlay() {
        postToCallback(7);
    }

    private void dispatchPlayFromMediaId(String mediaId, Bundle extras) {
        postToCallback(8, mediaId, extras);
    }

    private void dispatchPlayFromSearch(String query, Bundle extras) {
        postToCallback(9, query, extras);
    }

    private void dispatchPlayFromUri(Uri uri, Bundle extras) {
        postToCallback(10, uri, extras);
    }

    private void dispatchSkipToItem(long id) {
        postToCallback(11, Long.valueOf(id));
    }

    private void dispatchPause() {
        postToCallback(12);
    }

    private void dispatchStop() {
        postToCallback(13);
    }

    private void dispatchNext() {
        postToCallback(14);
    }

    private void dispatchPrevious() {
        postToCallback(15);
    }

    private void dispatchFastForward() {
        postToCallback(16);
    }

    private void dispatchRewind() {
        postToCallback(17);
    }

    private void dispatchSeekTo(long pos) {
        postToCallback(18, Long.valueOf(pos));
    }

    private void dispatchRate(Rating rating) {
        postToCallback(19, rating);
    }

    private void dispatchCustomAction(String action, Bundle args) {
        postToCallback(20, action, args);
    }

    private void dispatchMediaButton(Intent mediaButtonIntent) {
        postToCallback(2, mediaButtonIntent);
    }

    private void dispatchAdjustVolume(int direction) {
        postToCallback(21, Integer.valueOf(direction));
    }

    private void dispatchSetVolumeTo(int volume) {
        postToCallback(22, Integer.valueOf(volume));
    }

    private void postToCallback(int what) {
        postToCallback(what, null);
    }

    private void postCommand(String command, Bundle args, ResultReceiver resultCb) {
        Command cmd = new Command(command, args, resultCb);
        postToCallback(1, cmd);
    }

    private void postToCallback(int what, Object obj) {
        postToCallback(what, obj, null);
    }

    private void postToCallback(int what, Object obj, Bundle extras) {
        synchronized (this.mLock) {
            if (this.mCallback != null) {
                this.mCallback.post(what, obj, extras);
            }
        }
    }

    public static boolean isActiveState(int state) {
        switch (state) {
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
            public Token createFromParcel(Parcel in) {
                return new Token(ISessionController.Stub.asInterface(in.readStrongBinder()));
            }

            @Override
            public Token[] newArray(int size) {
                return new Token[size];
            }
        };
        private ISessionController mBinder;

        public Token(ISessionController binder) {
            this.mBinder = binder;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeStrongBinder(this.mBinder.asBinder());
        }

        public int hashCode() {
            int result = (this.mBinder == null ? 0 : this.mBinder.asBinder().hashCode()) + 31;
            return result;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Token other = (Token) obj;
            if (this.mBinder == null) {
                if (other.mBinder != null) {
                    return false;
                }
            } else if (!this.mBinder.asBinder().equals(other.mBinder.asBinder())) {
                return false;
            }
            return true;
        }

        ISessionController getBinder() {
            return this.mBinder;
        }
    }

    public static abstract class Callback {
        private MediaSession mSession;

        public void onCommand(String command, Bundle args, ResultReceiver cb) {
        }

        public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
            KeyEvent ke;
            if (this.mSession != null && Intent.ACTION_MEDIA_BUTTON.equals(mediaButtonIntent.getAction()) && (ke = (KeyEvent) mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)) != null && ke.getAction() == 0) {
                PlaybackState state = this.mSession.mPlaybackState;
                long validActions = state == null ? 0L : state.getActions();
                switch (ke.getKeyCode()) {
                    case 79:
                    case 85:
                        boolean isPlaying = state != null && state.getState() == 3;
                        boolean canPlay = (516 & validActions) != 0;
                        boolean canPause = (514 & validActions) != 0;
                        if (isPlaying && canPause) {
                            onPause();
                            return true;
                        }
                        if (!isPlaying && canPlay) {
                            onPlay();
                            return true;
                        }
                        break;
                    case 86:
                        if ((1 & validActions) != 0) {
                            onStop();
                            return true;
                        }
                        break;
                    case 87:
                        if ((32 & validActions) != 0) {
                            onSkipToNext();
                            return true;
                        }
                        break;
                    case 88:
                        if ((16 & validActions) != 0) {
                            onSkipToPrevious();
                            return true;
                        }
                        break;
                    case 89:
                        if ((8 & validActions) != 0) {
                            onRewind();
                            return true;
                        }
                        break;
                    case 90:
                        if ((64 & validActions) != 0) {
                            onFastForward();
                            return true;
                        }
                        break;
                    case 126:
                        if ((4 & validActions) != 0) {
                            onPlay();
                            return true;
                        }
                        break;
                    case 127:
                        if ((2 & validActions) != 0) {
                            onPause();
                            return true;
                        }
                        break;
                }
            }
            return false;
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

        public void onPlayFromSearch(String query, Bundle extras) {
        }

        public void onPlayFromMediaId(String mediaId, Bundle extras) {
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

        public void onSetRating(Rating rating) {
        }

        public void onCustomAction(String action, Bundle extras) {
        }
    }

    public static class CallbackStub extends ISessionCallback.Stub {
        private WeakReference<MediaSession> mMediaSession;

        public CallbackStub(MediaSession session) {
            this.mMediaSession = new WeakReference<>(session);
        }

        @Override
        public void onCommand(String command, Bundle args, ResultReceiver cb) {
            MediaSession session = this.mMediaSession.get();
            if (session == null) {
                return;
            }
            session.postCommand(command, args, cb);
        }

        @Override
        public void onMediaButton(Intent mediaButtonIntent, int sequenceNumber, ResultReceiver cb) {
            MediaSession session = this.mMediaSession.get();
            if (session != null) {
                try {
                    session.dispatchMediaButton(mediaButtonIntent);
                } finally {
                    if (cb != null) {
                        cb.send(sequenceNumber, null);
                    }
                }
            }
        }

        @Override
        public void onPrepare() {
            MediaSession session = this.mMediaSession.get();
            if (session == null) {
                return;
            }
            session.dispatchPrepare();
        }

        @Override
        public void onPrepareFromMediaId(String mediaId, Bundle extras) {
            MediaSession session = this.mMediaSession.get();
            if (session == null) {
                return;
            }
            session.dispatchPrepareFromMediaId(mediaId, extras);
        }

        @Override
        public void onPrepareFromSearch(String query, Bundle extras) {
            MediaSession session = this.mMediaSession.get();
            if (session == null) {
                return;
            }
            session.dispatchPrepareFromSearch(query, extras);
        }

        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {
            MediaSession session = this.mMediaSession.get();
            if (session == null) {
                return;
            }
            session.dispatchPrepareFromUri(uri, extras);
        }

        @Override
        public void onPlay() {
            MediaSession session = this.mMediaSession.get();
            if (session == null) {
                return;
            }
            session.dispatchPlay();
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            MediaSession session = this.mMediaSession.get();
            if (session == null) {
                return;
            }
            session.dispatchPlayFromMediaId(mediaId, extras);
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            MediaSession session = this.mMediaSession.get();
            if (session == null) {
                return;
            }
            session.dispatchPlayFromSearch(query, extras);
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            MediaSession session = this.mMediaSession.get();
            if (session == null) {
                return;
            }
            session.dispatchPlayFromUri(uri, extras);
        }

        @Override
        public void onSkipToTrack(long id) {
            MediaSession session = this.mMediaSession.get();
            if (session == null) {
                return;
            }
            session.dispatchSkipToItem(id);
        }

        @Override
        public void onPause() {
            MediaSession session = this.mMediaSession.get();
            if (session == null) {
                return;
            }
            session.dispatchPause();
        }

        @Override
        public void onStop() {
            MediaSession session = this.mMediaSession.get();
            if (session == null) {
                return;
            }
            session.dispatchStop();
        }

        @Override
        public void onNext() {
            MediaSession session = this.mMediaSession.get();
            if (session == null) {
                return;
            }
            session.dispatchNext();
        }

        @Override
        public void onPrevious() {
            MediaSession session = this.mMediaSession.get();
            if (session == null) {
                return;
            }
            session.dispatchPrevious();
        }

        @Override
        public void onFastForward() {
            MediaSession session = this.mMediaSession.get();
            if (session == null) {
                return;
            }
            session.dispatchFastForward();
        }

        @Override
        public void onRewind() {
            MediaSession session = this.mMediaSession.get();
            if (session == null) {
                return;
            }
            session.dispatchRewind();
        }

        @Override
        public void onSeekTo(long pos) {
            MediaSession session = this.mMediaSession.get();
            if (session == null) {
                return;
            }
            session.dispatchSeekTo(pos);
        }

        @Override
        public void onRate(Rating rating) {
            MediaSession session = this.mMediaSession.get();
            if (session == null) {
                return;
            }
            session.dispatchRate(rating);
        }

        @Override
        public void onCustomAction(String action, Bundle args) {
            MediaSession session = this.mMediaSession.get();
            if (session == null) {
                return;
            }
            session.dispatchCustomAction(action, args);
        }

        @Override
        public void onAdjustVolume(int direction) {
            MediaSession session = this.mMediaSession.get();
            if (session == null) {
                return;
            }
            session.dispatchAdjustVolume(direction);
        }

        @Override
        public void onSetVolumeTo(int value) {
            MediaSession session = this.mMediaSession.get();
            if (session == null) {
                return;
            }
            session.dispatchSetVolumeTo(value);
        }
    }

    public static final class QueueItem implements Parcelable {
        public static final Parcelable.Creator<QueueItem> CREATOR = new Parcelable.Creator<QueueItem>() {
            @Override
            public QueueItem createFromParcel(Parcel p) {
                return new QueueItem(p, (QueueItem) null);
            }

            @Override
            public QueueItem[] newArray(int size) {
                return new QueueItem[size];
            }
        };
        public static final int UNKNOWN_ID = -1;
        private final MediaDescription mDescription;
        private final long mId;

        QueueItem(Parcel in, QueueItem queueItem) {
            this(in);
        }

        public QueueItem(MediaDescription description, long id) {
            if (description == null) {
                throw new IllegalArgumentException("Description cannot be null.");
            }
            if (id == -1) {
                throw new IllegalArgumentException("Id cannot be QueueItem.UNKNOWN_ID");
            }
            this.mDescription = description;
            this.mId = id;
        }

        private QueueItem(Parcel in) {
            this.mDescription = MediaDescription.CREATOR.createFromParcel(in);
            this.mId = in.readLong();
        }

        public MediaDescription getDescription() {
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

        public String toString() {
            return "MediaSession.QueueItem {Description=" + this.mDescription + ", Id=" + this.mId + " }";
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

        public CallbackMessageHandler(Looper looper, Callback callback) {
            super(looper, null, true);
            this.mCallback = callback;
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
            VolumeProvider vp;
            VolumeProvider vp2;
            switch (msg.what) {
                case 1:
                    Command cmd = (Command) msg.obj;
                    this.mCallback.onCommand(cmd.command, cmd.extras, cmd.stub);
                    return;
                case 2:
                    this.mCallback.onMediaButtonEvent((Intent) msg.obj);
                    return;
                case 3:
                    this.mCallback.onPrepare();
                    return;
                case 4:
                    this.mCallback.onPrepareFromMediaId((String) msg.obj, msg.getData());
                    return;
                case 5:
                    this.mCallback.onPrepareFromSearch((String) msg.obj, msg.getData());
                    return;
                case 6:
                    this.mCallback.onPrepareFromUri((Uri) msg.obj, msg.getData());
                    return;
                case 7:
                    this.mCallback.onPlay();
                    return;
                case 8:
                    this.mCallback.onPlayFromMediaId((String) msg.obj, msg.getData());
                    return;
                case 9:
                    this.mCallback.onPlayFromSearch((String) msg.obj, msg.getData());
                    return;
                case 10:
                    this.mCallback.onPlayFromUri((Uri) msg.obj, msg.getData());
                    return;
                case 11:
                    this.mCallback.onSkipToQueueItem(((Long) msg.obj).longValue());
                    return;
                case 12:
                    this.mCallback.onPause();
                    return;
                case 13:
                    this.mCallback.onStop();
                    return;
                case 14:
                    this.mCallback.onSkipToNext();
                    return;
                case 15:
                    this.mCallback.onSkipToPrevious();
                    return;
                case 16:
                    this.mCallback.onFastForward();
                    return;
                case 17:
                    this.mCallback.onRewind();
                    return;
                case 18:
                    this.mCallback.onSeekTo(((Long) msg.obj).longValue());
                    return;
                case 19:
                    this.mCallback.onSetRating((Rating) msg.obj);
                    return;
                case 20:
                    this.mCallback.onCustomAction((String) msg.obj, msg.getData());
                    return;
                case 21:
                    synchronized (MediaSession.this.mLock) {
                        vp2 = MediaSession.this.mVolumeProvider;
                    }
                    if (vp2 == null) {
                        return;
                    }
                    vp2.onAdjustVolume(((Integer) msg.obj).intValue());
                    return;
                case 22:
                    synchronized (MediaSession.this.mLock) {
                        vp = MediaSession.this.mVolumeProvider;
                    }
                    if (vp == null) {
                        return;
                    }
                    vp.onSetVolumeTo(((Integer) msg.obj).intValue());
                    return;
                default:
                    return;
            }
        }
    }
}
