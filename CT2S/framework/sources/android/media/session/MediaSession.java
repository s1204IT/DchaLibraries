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
import com.android.internal.R;
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
        this.mMaxBitmapSize = context.getResources().getDimensionPixelSize(R.dimen.config_mediaMetadataBitmapMaxSize);
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
        if (this.mActive != active) {
            try {
                this.mBinder.setActive(active);
                this.mActive = active;
            } catch (RemoteException e) {
                Log.wtf(TAG, "Failure in setActive.", e);
            }
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
            this.mBinder.setQueue(queue == null ? null : new ParceledListSlice(queue));
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
                if (provider != this.mVolumeProvider) {
                    Log.w(TAG, "Received update from stale volume provider");
                } else {
                    try {
                        this.mBinder.setCurrentVolume(provider.getCurrentVolume());
                    } catch (RemoteException e) {
                        Log.e(TAG, "Error in notifyVolumeChanged", e);
                    }
                }
            }
        }
    }

    private void dispatchPlay() {
        postToCallback(1);
    }

    private void dispatchPlayFromMediaId(String mediaId, Bundle extras) {
        postToCallback(2, mediaId, extras);
    }

    private void dispatchPlayFromSearch(String query, Bundle extras) {
        postToCallback(3, query, extras);
    }

    private void dispatchSkipToItem(long id) {
        postToCallback(4, Long.valueOf(id));
    }

    private void dispatchPause() {
        postToCallback(5);
    }

    private void dispatchStop() {
        postToCallback(6);
    }

    private void dispatchNext() {
        postToCallback(7);
    }

    private void dispatchPrevious() {
        postToCallback(8);
    }

    private void dispatchFastForward() {
        postToCallback(9);
    }

    private void dispatchRewind() {
        postToCallback(10);
    }

    private void dispatchSeekTo(long pos) {
        postToCallback(11, Long.valueOf(pos));
    }

    private void dispatchRate(Rating rating) {
        postToCallback(12, rating);
    }

    private void dispatchCustomAction(String action, Bundle args) {
        postToCallback(13, action, args);
    }

    private void dispatchMediaButton(Intent mediaButtonIntent) {
        postToCallback(14, mediaButtonIntent);
    }

    private void dispatchAdjustVolume(int direction) {
        postToCallback(16, Integer.valueOf(direction));
    }

    private void dispatchSetVolumeTo(int volume) {
        postToCallback(17, Integer.valueOf(volume));
    }

    private void postToCallback(int what) {
        postToCallback(what, null);
    }

    private void postCommand(String command, Bundle args, ResultReceiver resultCb) {
        Command cmd = new Command(command, args, resultCb);
        postToCallback(15, cmd);
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
            if (obj != null && getClass() == obj.getClass()) {
                Token other = (Token) obj;
                return this.mBinder == null ? other.mBinder == null : this.mBinder.asBinder().equals(other.mBinder.asBinder());
            }
            return false;
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
            if (this.mSession == null || !Intent.ACTION_MEDIA_BUTTON.equals(mediaButtonIntent.getAction()) || (ke = (KeyEvent) mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)) == null || ke.getAction() != 0) {
                return false;
            }
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
                    if (isPlaying || !canPlay) {
                        return false;
                    }
                    onPlay();
                    return true;
                case 86:
                    if ((1 & validActions) == 0) {
                        return false;
                    }
                    onStop();
                    return true;
                case 87:
                    if ((32 & validActions) == 0) {
                        return false;
                    }
                    onSkipToNext();
                    return true;
                case 88:
                    if ((16 & validActions) == 0) {
                        return false;
                    }
                    onSkipToPrevious();
                    return true;
                case 89:
                    if ((8 & validActions) == 0) {
                        return false;
                    }
                    onRewind();
                    return true;
                case 90:
                    if ((64 & validActions) == 0) {
                        return false;
                    }
                    onFastForward();
                    return true;
                case 126:
                    if ((4 & validActions) == 0) {
                        return false;
                    }
                    onPlay();
                    return true;
                case 127:
                    if ((2 & validActions) == 0) {
                        return false;
                    }
                    onPause();
                    return true;
                default:
                    return false;
            }
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
            if (session != null) {
                session.postCommand(command, args, cb);
            }
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
        public void onPlay() {
            MediaSession session = this.mMediaSession.get();
            if (session != null) {
                session.dispatchPlay();
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            MediaSession session = this.mMediaSession.get();
            if (session != null) {
                session.dispatchPlayFromMediaId(mediaId, extras);
            }
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            MediaSession session = this.mMediaSession.get();
            if (session != null) {
                session.dispatchPlayFromSearch(query, extras);
            }
        }

        @Override
        public void onSkipToTrack(long id) {
            MediaSession session = this.mMediaSession.get();
            if (session != null) {
                session.dispatchSkipToItem(id);
            }
        }

        @Override
        public void onPause() {
            MediaSession session = this.mMediaSession.get();
            if (session != null) {
                session.dispatchPause();
            }
        }

        @Override
        public void onStop() {
            MediaSession session = this.mMediaSession.get();
            if (session != null) {
                session.dispatchStop();
            }
        }

        @Override
        public void onNext() {
            MediaSession session = this.mMediaSession.get();
            if (session != null) {
                session.dispatchNext();
            }
        }

        @Override
        public void onPrevious() {
            MediaSession session = this.mMediaSession.get();
            if (session != null) {
                session.dispatchPrevious();
            }
        }

        @Override
        public void onFastForward() {
            MediaSession session = this.mMediaSession.get();
            if (session != null) {
                session.dispatchFastForward();
            }
        }

        @Override
        public void onRewind() {
            MediaSession session = this.mMediaSession.get();
            if (session != null) {
                session.dispatchRewind();
            }
        }

        @Override
        public void onSeekTo(long pos) {
            MediaSession session = this.mMediaSession.get();
            if (session != null) {
                session.dispatchSeekTo(pos);
            }
        }

        @Override
        public void onRate(Rating rating) {
            MediaSession session = this.mMediaSession.get();
            if (session != null) {
                session.dispatchRate(rating);
            }
        }

        @Override
        public void onCustomAction(String action, Bundle args) {
            MediaSession session = this.mMediaSession.get();
            if (session != null) {
                session.dispatchCustomAction(action, args);
            }
        }

        @Override
        public void onAdjustVolume(int direction) {
            MediaSession session = this.mMediaSession.get();
            if (session != null) {
                session.dispatchAdjustVolume(direction);
            }
        }

        @Override
        public void onSetVolumeTo(int value) {
            MediaSession session = this.mMediaSession.get();
            if (session != null) {
                session.dispatchSetVolumeTo(value);
            }
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
        private final MediaDescription mDescription;
        private final long mId;

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
                    this.mCallback.onPlay();
                    return;
                case 2:
                    this.mCallback.onPlayFromMediaId((String) msg.obj, msg.getData());
                    return;
                case 3:
                    this.mCallback.onPlayFromSearch((String) msg.obj, msg.getData());
                    return;
                case 4:
                    this.mCallback.onSkipToQueueItem(((Long) msg.obj).longValue());
                    return;
                case 5:
                    this.mCallback.onPause();
                    return;
                case 6:
                    this.mCallback.onStop();
                    return;
                case 7:
                    this.mCallback.onSkipToNext();
                    return;
                case 8:
                    this.mCallback.onSkipToPrevious();
                    return;
                case 9:
                    this.mCallback.onFastForward();
                    return;
                case 10:
                    this.mCallback.onRewind();
                    return;
                case 11:
                    this.mCallback.onSeekTo(((Long) msg.obj).longValue());
                    return;
                case 12:
                    this.mCallback.onSetRating((Rating) msg.obj);
                    return;
                case 13:
                    this.mCallback.onCustomAction((String) msg.obj, msg.getData());
                    return;
                case 14:
                    this.mCallback.onMediaButtonEvent((Intent) msg.obj);
                    return;
                case 15:
                    Command cmd = (Command) msg.obj;
                    this.mCallback.onCommand(cmd.command, cmd.extras, cmd.stub);
                    return;
                case 16:
                    synchronized (MediaSession.this.mLock) {
                        vp2 = MediaSession.this.mVolumeProvider;
                        break;
                    }
                    if (vp2 != null) {
                        vp2.onAdjustVolume(((Integer) msg.obj).intValue());
                        return;
                    }
                    return;
                case 17:
                    synchronized (MediaSession.this.mLock) {
                        vp = MediaSession.this.mVolumeProvider;
                        break;
                    }
                    if (vp != null) {
                        vp.onSetVolumeTo(((Integer) msg.obj).intValue());
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }
}
