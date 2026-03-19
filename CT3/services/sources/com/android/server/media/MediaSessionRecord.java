package com.android.server.media;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioManagerInternal;
import android.media.AudioSystem;
import android.media.MediaMetadata;
import android.media.Rating;
import android.media.session.ISession;
import android.media.session.ISessionCallback;
import android.media.session.ISessionController;
import android.media.session.ISessionControllerCallback;
import android.media.session.MediaSession;
import android.media.session.ParcelableVolumeInfo;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;
import android.view.KeyEvent;
import com.android.server.LocalServices;
import com.android.server.pm.PackageManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;

public class MediaSessionRecord implements IBinder.DeathRecipient {
    private static final int ACTIVE_BUFFER = 30000;
    private static final int OPTIMISTIC_VOLUME_TIMEOUT = 1000;
    private static final int UID_NOT_SET = -1;
    private AudioManager mAudioManager;
    private String mCallingPackage;
    private Bundle mExtras;
    private long mFlags;
    private final MessageHandler mHandler;
    private long mLastActiveTime;
    private PendingIntent mLaunchIntent;
    private PendingIntent mMediaButtonReceiver;
    private MediaMetadata mMetadata;
    private final int mOwnerPid;
    private final int mOwnerUid;
    private final String mPackageName;
    private PlaybackState mPlaybackState;
    private ParceledListSlice mQueue;
    private CharSequence mQueueTitle;
    private int mRatingType;
    private final MediaSessionService mService;
    private final SessionCb mSessionCb;
    private final String mTag;
    private final int mUserId;
    private static final String TAG = "MediaSessionRecord";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private final Object mLock = new Object();
    private final ArrayList<ISessionControllerCallback> mControllerCallbacks = new ArrayList<>();
    private int mVolumeType = 1;
    private int mVolumeControlType = 2;
    private int mMaxVolume = 0;
    private int mCurrentVolume = 0;
    private int mOptimisticVolume = -1;
    private boolean mIsActive = false;
    private boolean mDestroyed = false;
    private int mCallingUid = -1;
    private final Runnable mClearOptimisticVolumeRunnable = new Runnable() {
        @Override
        public void run() {
            boolean needUpdate = MediaSessionRecord.this.mOptimisticVolume != MediaSessionRecord.this.mCurrentVolume;
            MediaSessionRecord.this.mOptimisticVolume = -1;
            if (!needUpdate) {
                return;
            }
            MediaSessionRecord.this.pushVolumeUpdate();
        }
    };
    private final ControllerStub mController = new ControllerStub();
    private final SessionStub mSession = new SessionStub(this, null);
    private AudioManagerInternal mAudioManagerInternal = (AudioManagerInternal) LocalServices.getService(AudioManagerInternal.class);
    private AudioAttributes mAudioAttrs = new AudioAttributes.Builder().setUsage(1).build();

    public MediaSessionRecord(int ownerPid, int ownerUid, int userId, String ownerPackageName, ISessionCallback cb, String tag, MediaSessionService service, Handler handler) {
        this.mOwnerPid = ownerPid;
        this.mOwnerUid = ownerUid;
        this.mUserId = userId;
        this.mPackageName = ownerPackageName;
        this.mTag = tag;
        this.mSessionCb = new SessionCb(cb);
        this.mService = service;
        this.mHandler = new MessageHandler(handler.getLooper());
        this.mAudioManager = (AudioManager) service.getContext().getSystemService("audio");
    }

    public ISession getSessionBinder() {
        return this.mSession;
    }

    public ISessionController getControllerBinder() {
        return this.mController;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public String getTag() {
        return this.mTag;
    }

    public PendingIntent getMediaButtonReceiver() {
        return this.mMediaButtonReceiver;
    }

    public long getFlags() {
        return this.mFlags;
    }

    public boolean hasFlag(int flag) {
        return (this.mFlags & ((long) flag)) != 0;
    }

    public int getUserId() {
        return this.mUserId;
    }

    public boolean isSystemPriority() {
        return (this.mFlags & 65536) != 0;
    }

    public void adjustVolume(int direction, int flags, String packageName, int uid, boolean useSuggested) {
        int previousFlagPlaySound = flags & 4;
        if (isPlaybackActive(false) || hasFlag(PackageManagerService.DumpState.DUMP_INSTALLS)) {
            flags &= -5;
        }
        if (this.mVolumeType == 1) {
            int stream = AudioAttributes.toLegacyStreamType(this.mAudioAttrs);
            postAdjustLocalVolume(stream, direction, flags, packageName, uid, useSuggested, previousFlagPlaySound);
            return;
        }
        if (this.mVolumeControlType == 0) {
            return;
        }
        if (direction == 101 || direction == -100 || direction == 100) {
            Log.w(TAG, "Muting remote playback is not supported");
            return;
        }
        this.mSessionCb.adjustVolume(direction);
        int volumeBefore = this.mOptimisticVolume < 0 ? this.mCurrentVolume : this.mOptimisticVolume;
        this.mOptimisticVolume = volumeBefore + direction;
        this.mOptimisticVolume = Math.max(0, Math.min(this.mOptimisticVolume, this.mMaxVolume));
        this.mHandler.removeCallbacks(this.mClearOptimisticVolumeRunnable);
        this.mHandler.postDelayed(this.mClearOptimisticVolumeRunnable, 1000L);
        if (volumeBefore != this.mOptimisticVolume) {
            pushVolumeUpdate();
        }
        this.mService.notifyRemoteVolumeChanged(flags, this);
        if (!DEBUG) {
            return;
        }
        Log.d(TAG, "Adjusted optimistic volume to " + this.mOptimisticVolume + " max is " + this.mMaxVolume);
    }

    public void setVolumeTo(int value, int flags, String packageName, int uid) {
        if (this.mVolumeType == 1) {
            int stream = AudioAttributes.toLegacyStreamType(this.mAudioAttrs);
            this.mAudioManagerInternal.setStreamVolumeForUid(stream, value, flags, packageName, uid);
            return;
        }
        if (this.mVolumeControlType != 2) {
            return;
        }
        int value2 = Math.max(0, Math.min(value, this.mMaxVolume));
        this.mSessionCb.setVolumeTo(value2);
        int volumeBefore = this.mOptimisticVolume < 0 ? this.mCurrentVolume : this.mOptimisticVolume;
        this.mOptimisticVolume = Math.max(0, Math.min(value2, this.mMaxVolume));
        this.mHandler.removeCallbacks(this.mClearOptimisticVolumeRunnable);
        this.mHandler.postDelayed(this.mClearOptimisticVolumeRunnable, 1000L);
        if (volumeBefore != this.mOptimisticVolume) {
            pushVolumeUpdate();
        }
        this.mService.notifyRemoteVolumeChanged(flags, this);
        if (!DEBUG) {
            return;
        }
        Log.d(TAG, "Set optimistic volume to " + this.mOptimisticVolume + " max is " + this.mMaxVolume);
    }

    public boolean isActive() {
        return this.mIsActive && !this.mDestroyed;
    }

    public boolean isPlaybackActive(boolean includeRecentlyActive) {
        int state = this.mPlaybackState == null ? 0 : this.mPlaybackState.getState();
        if (MediaSession.isActiveState(state)) {
            return true;
        }
        if (includeRecentlyActive && state == 2) {
            long inactiveTime = SystemClock.uptimeMillis() - this.mLastActiveTime;
            if (inactiveTime < 30000) {
                return true;
            }
        }
        return false;
    }

    public int getPlaybackType() {
        return this.mVolumeType;
    }

    public AudioAttributes getAudioAttributes() {
        return this.mAudioAttrs;
    }

    public int getVolumeControl() {
        return this.mVolumeControlType;
    }

    public int getMaxVolume() {
        return this.mMaxVolume;
    }

    public int getCurrentVolume() {
        return this.mCurrentVolume;
    }

    public int getOptimisticVolume() {
        return this.mOptimisticVolume;
    }

    public boolean isTransportControlEnabled() {
        return hasFlag(2);
    }

    @Override
    public void binderDied() {
        this.mService.sessionDied(this);
    }

    public void onDestroy() {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                return;
            }
            this.mDestroyed = true;
            this.mHandler.post(9);
        }
    }

    public ISessionCallback getCallback() {
        return this.mSessionCb.mCb;
    }

    public void sendMediaButton(KeyEvent ke, int sequenceId, ResultReceiver cb, int uid, String packageName) {
        updateCallingPackage(uid, packageName);
        this.mSessionCb.sendMediaButton(ke, sequenceId, cb);
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.println(prefix + this.mTag + " " + this);
        String indent = prefix + "  ";
        pw.println(indent + "ownerPid=" + this.mOwnerPid + ", ownerUid=" + this.mOwnerUid + ", userId=" + this.mUserId);
        pw.println(indent + "package=" + this.mPackageName);
        pw.println(indent + "launchIntent=" + this.mLaunchIntent);
        pw.println(indent + "mediaButtonReceiver=" + this.mMediaButtonReceiver);
        pw.println(indent + "active=" + this.mIsActive);
        pw.println(indent + "flags=" + this.mFlags);
        pw.println(indent + "rating type=" + this.mRatingType);
        pw.println(indent + "controllers: " + this.mControllerCallbacks.size());
        pw.println(indent + "state=" + (this.mPlaybackState != null ? this.mPlaybackState.toString() : null));
        pw.println(indent + "audioAttrs=" + this.mAudioAttrs);
        pw.println(indent + "volumeType=" + this.mVolumeType + ", controlType=" + this.mVolumeControlType + ", max=" + this.mMaxVolume + ", current=" + this.mCurrentVolume);
        pw.println(indent + "metadata:" + getShortMetadataString());
        pw.println(indent + "queueTitle=" + this.mQueueTitle + ", size=" + (this.mQueue == null ? 0 : this.mQueue.getList().size()));
    }

    public String toString() {
        return this.mPackageName + "/" + this.mTag;
    }

    private void postAdjustLocalVolume(final int stream, final int direction, final int flags, final String packageName, final int uid, final boolean useSuggested, final int previousFlagPlaySound) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (useSuggested) {
                    if (AudioSystem.isStreamActive(stream, 0)) {
                        MediaSessionRecord.this.mAudioManagerInternal.adjustSuggestedStreamVolumeForUid(stream, direction, flags, packageName, uid);
                        return;
                    }
                    MediaSessionRecord.this.mAudioManagerInternal.adjustSuggestedStreamVolumeForUid(Integer.MIN_VALUE, direction, previousFlagPlaySound | flags, packageName, uid);
                    return;
                }
                MediaSessionRecord.this.mAudioManagerInternal.adjustStreamVolumeForUid(stream, direction, flags, packageName, uid);
            }
        });
    }

    private String getShortMetadataString() {
        int fields = this.mMetadata == null ? 0 : this.mMetadata.size();
        return "size=" + fields + ", description=" + (this.mMetadata == null ? null : this.mMetadata.getDescription());
    }

    private void pushPlaybackStateUpdate() {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                return;
            }
            for (int i = this.mControllerCallbacks.size() - 1; i >= 0; i--) {
                ISessionControllerCallback cb = this.mControllerCallbacks.get(i);
                try {
                    cb.onPlaybackStateChanged(this.mPlaybackState);
                } catch (DeadObjectException e) {
                    this.mControllerCallbacks.remove(i);
                    Log.w(TAG, "Removed dead callback in pushPlaybackStateUpdate.", e);
                } catch (RemoteException e2) {
                    Log.w(TAG, "unexpected exception in pushPlaybackStateUpdate.", e2);
                }
            }
        }
    }

    private void pushMetadataUpdate() {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                return;
            }
            for (int i = this.mControllerCallbacks.size() - 1; i >= 0; i--) {
                ISessionControllerCallback cb = this.mControllerCallbacks.get(i);
                try {
                    cb.onMetadataChanged(this.mMetadata);
                } catch (DeadObjectException e) {
                    Log.w(TAG, "Removing dead callback in pushMetadataUpdate. ", e);
                    this.mControllerCallbacks.remove(i);
                } catch (RemoteException e2) {
                    Log.w(TAG, "unexpected exception in pushMetadataUpdate. ", e2);
                }
            }
        }
    }

    private void pushQueueUpdate() {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                return;
            }
            for (int i = this.mControllerCallbacks.size() - 1; i >= 0; i--) {
                ISessionControllerCallback cb = this.mControllerCallbacks.get(i);
                try {
                    cb.onQueueChanged(this.mQueue);
                } catch (DeadObjectException e) {
                    this.mControllerCallbacks.remove(i);
                    Log.w(TAG, "Removed dead callback in pushQueueUpdate.", e);
                } catch (RemoteException e2) {
                    Log.w(TAG, "unexpected exception in pushQueueUpdate.", e2);
                }
            }
        }
    }

    private void pushQueueTitleUpdate() {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                return;
            }
            for (int i = this.mControllerCallbacks.size() - 1; i >= 0; i--) {
                ISessionControllerCallback cb = this.mControllerCallbacks.get(i);
                try {
                    cb.onQueueTitleChanged(this.mQueueTitle);
                } catch (DeadObjectException e) {
                    this.mControllerCallbacks.remove(i);
                    Log.w(TAG, "Removed dead callback in pushQueueTitleUpdate.", e);
                } catch (RemoteException e2) {
                    Log.w(TAG, "unexpected exception in pushQueueTitleUpdate.", e2);
                }
            }
        }
    }

    private void pushExtrasUpdate() {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                return;
            }
            for (int i = this.mControllerCallbacks.size() - 1; i >= 0; i--) {
                ISessionControllerCallback cb = this.mControllerCallbacks.get(i);
                try {
                    cb.onExtrasChanged(this.mExtras);
                } catch (DeadObjectException e) {
                    this.mControllerCallbacks.remove(i);
                    Log.w(TAG, "Removed dead callback in pushExtrasUpdate.", e);
                } catch (RemoteException e2) {
                    Log.w(TAG, "unexpected exception in pushExtrasUpdate.", e2);
                }
            }
        }
    }

    private void pushVolumeUpdate() {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                return;
            }
            ParcelableVolumeInfo info = this.mController.getVolumeAttributes();
            for (int i = this.mControllerCallbacks.size() - 1; i >= 0; i--) {
                ISessionControllerCallback cb = this.mControllerCallbacks.get(i);
                try {
                    try {
                        cb.onVolumeInfoChanged(info);
                    } catch (DeadObjectException e) {
                        Log.w(TAG, "Removing dead callback in pushVolumeUpdate. ", e);
                    }
                } catch (RemoteException e2) {
                    Log.w(TAG, "Unexpected exception in pushVolumeUpdate. ", e2);
                }
            }
        }
    }

    private void pushEvent(String event, Bundle data) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                return;
            }
            for (int i = this.mControllerCallbacks.size() - 1; i >= 0; i--) {
                ISessionControllerCallback cb = this.mControllerCallbacks.get(i);
                try {
                    cb.onEvent(event, data);
                } catch (DeadObjectException e) {
                    Log.w(TAG, "Removing dead callback in pushEvent.", e);
                    this.mControllerCallbacks.remove(i);
                } catch (RemoteException e2) {
                    Log.w(TAG, "unexpected exception in pushEvent.", e2);
                }
            }
        }
    }

    private void pushSessionDestroyed() {
        synchronized (this.mLock) {
            if (!this.mDestroyed) {
                return;
            }
            for (int i = this.mControllerCallbacks.size() - 1; i >= 0; i--) {
                ISessionControllerCallback cb = this.mControllerCallbacks.get(i);
                try {
                    cb.onSessionDestroyed();
                } catch (DeadObjectException e) {
                    Log.w(TAG, "Removing dead callback in pushEvent.", e);
                    this.mControllerCallbacks.remove(i);
                } catch (RemoteException e2) {
                    Log.w(TAG, "unexpected exception in pushEvent.", e2);
                }
            }
            this.mControllerCallbacks.clear();
        }
    }

    private PlaybackState getStateWithUpdatedPosition() {
        PlaybackState state;
        long duration = -1;
        synchronized (this.mLock) {
            state = this.mPlaybackState;
            if (this.mMetadata != null && this.mMetadata.containsKey("android.media.metadata.DURATION")) {
                duration = this.mMetadata.getLong("android.media.metadata.DURATION");
            }
        }
        PlaybackState result = null;
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
                PlaybackState.Builder builder = new PlaybackState.Builder(state);
                builder.setState(state.getState(), position, state.getPlaybackSpeed(), currentTime);
                result = builder.build();
            }
        }
        return result == null ? state : result;
    }

    private int getControllerCbIndexForCb(ISessionControllerCallback cb) {
        IBinder binder = cb.asBinder();
        for (int i = this.mControllerCallbacks.size() - 1; i >= 0; i--) {
            if (binder.equals(this.mControllerCallbacks.get(i).asBinder())) {
                return i;
            }
        }
        return -1;
    }

    private void updateCallingPackage() {
        updateCallingPackage(-1, null);
    }

    private void updateCallingPackage(int uid, String packageName) {
        if (uid == -1) {
            uid = Binder.getCallingUid();
        }
        synchronized (this.mLock) {
            if (this.mCallingUid == -1 || this.mCallingUid != uid) {
                this.mCallingUid = uid;
                this.mCallingPackage = packageName;
                if (this.mCallingPackage != null) {
                    return;
                }
                Context context = this.mService.getContext();
                if (context == null) {
                    return;
                }
                String[] packages = context.getPackageManager().getPackagesForUid(uid);
                if (packages != null && packages.length > 0) {
                    this.mCallingPackage = packages[0];
                }
            }
        }
    }

    private final class SessionStub extends ISession.Stub {
        SessionStub(MediaSessionRecord this$0, SessionStub sessionStub) {
            this();
        }

        private SessionStub() {
        }

        public void destroy() {
            MediaSessionRecord.this.mService.destroySession(MediaSessionRecord.this);
        }

        public void sendEvent(String event, Bundle data) {
            MediaSessionRecord.this.mHandler.post(6, event, data != null ? new Bundle(data) : null);
        }

        public ISessionController getController() {
            return MediaSessionRecord.this.mController;
        }

        public void setActive(boolean active) {
            MediaSessionRecord.this.mIsActive = active;
            MediaSessionRecord.this.mService.updateSession(MediaSessionRecord.this);
            MediaSessionRecord.this.mHandler.post(7);
        }

        public void setFlags(int flags) {
            if ((65536 & flags) != 0) {
                int pid = getCallingPid();
                int uid = getCallingUid();
                MediaSessionRecord.this.mService.enforcePhoneStatePermission(pid, uid);
            }
            MediaSessionRecord.this.mFlags = flags;
            MediaSessionRecord.this.mHandler.post(7);
        }

        public void setMediaButtonReceiver(PendingIntent pi) {
            MediaSessionRecord.this.mMediaButtonReceiver = pi;
        }

        public void setLaunchPendingIntent(PendingIntent pi) {
            MediaSessionRecord.this.mLaunchIntent = pi;
        }

        public void setMetadata(MediaMetadata metadata) {
            synchronized (MediaSessionRecord.this.mLock) {
                MediaMetadata mediaMetadataBuild = metadata == null ? null : new MediaMetadata.Builder(metadata).build();
                if (mediaMetadataBuild != null) {
                    mediaMetadataBuild.size();
                }
                MediaSessionRecord.this.mMetadata = mediaMetadataBuild;
            }
            MediaSessionRecord.this.mHandler.post(1);
        }

        public void setPlaybackState(PlaybackState state) {
            int oldState = MediaSessionRecord.this.mPlaybackState == null ? 0 : MediaSessionRecord.this.mPlaybackState.getState();
            int newState = state == null ? 0 : state.getState();
            if (MediaSession.isActiveState(oldState) && newState == 2) {
                MediaSessionRecord.this.mLastActiveTime = SystemClock.elapsedRealtime();
            }
            synchronized (MediaSessionRecord.this.mLock) {
                MediaSessionRecord.this.mPlaybackState = state;
            }
            MediaSessionRecord.this.mHandler.post(2);
            MediaSessionRecord.this.mHandler.post(10, null, oldState, newState);
        }

        public void setQueue(ParceledListSlice queue) {
            synchronized (MediaSessionRecord.this.mLock) {
                MediaSessionRecord.this.mQueue = queue;
            }
            MediaSessionRecord.this.mHandler.post(3);
        }

        public void setQueueTitle(CharSequence title) {
            MediaSessionRecord.this.mQueueTitle = title;
            MediaSessionRecord.this.mHandler.post(4);
        }

        public void setExtras(Bundle extras) {
            synchronized (MediaSessionRecord.this.mLock) {
                MediaSessionRecord.this.mExtras = extras != null ? new Bundle(extras) : null;
            }
            MediaSessionRecord.this.mHandler.post(5);
        }

        public void setRatingType(int type) {
            MediaSessionRecord.this.mRatingType = type;
        }

        public void setCurrentVolume(int volume) {
            MediaSessionRecord.this.mCurrentVolume = volume;
            MediaSessionRecord.this.mHandler.post(8);
        }

        public void setPlaybackToLocal(AudioAttributes attributes) {
            boolean typeChanged;
            synchronized (MediaSessionRecord.this.mLock) {
                typeChanged = MediaSessionRecord.this.mVolumeType == 2;
                MediaSessionRecord.this.mVolumeType = 1;
                if (attributes != null) {
                    MediaSessionRecord.this.mAudioAttrs = attributes;
                } else {
                    Log.e(MediaSessionRecord.TAG, "Received null audio attributes, using existing attributes");
                }
            }
            if (!typeChanged) {
                return;
            }
            MediaSessionRecord.this.mService.onSessionPlaybackTypeChanged(MediaSessionRecord.this);
            MediaSessionRecord.this.mHandler.post(8);
        }

        public void setPlaybackToRemote(int control, int max) {
            boolean typeChanged;
            synchronized (MediaSessionRecord.this.mLock) {
                typeChanged = MediaSessionRecord.this.mVolumeType == 1;
                MediaSessionRecord.this.mVolumeType = 2;
                MediaSessionRecord.this.mVolumeControlType = control;
                MediaSessionRecord.this.mMaxVolume = max;
            }
            if (!typeChanged) {
                return;
            }
            MediaSessionRecord.this.mService.onSessionPlaybackTypeChanged(MediaSessionRecord.this);
            MediaSessionRecord.this.mHandler.post(8);
        }

        public String getCallingPackage() {
            return MediaSessionRecord.this.mCallingPackage;
        }
    }

    class SessionCb {
        private final ISessionCallback mCb;

        public SessionCb(ISessionCallback cb) {
            this.mCb = cb;
        }

        public boolean sendMediaButton(KeyEvent keyEvent, int sequenceId, ResultReceiver cb) {
            Intent mediaButtonIntent = new Intent("android.intent.action.MEDIA_BUTTON");
            mediaButtonIntent.putExtra("android.intent.extra.KEY_EVENT", keyEvent);
            try {
                this.mCb.onMediaButton(mediaButtonIntent, sequenceId, cb);
                return true;
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in sendMediaRequest.", e);
                return false;
            }
        }

        public void sendCommand(String command, Bundle args, ResultReceiver cb) {
            try {
                this.mCb.onCommand(command, args, cb);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in sendCommand.", e);
            }
        }

        public void sendCustomAction(String action, Bundle args) {
            try {
                this.mCb.onCustomAction(action, args);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in sendCustomAction.", e);
            }
        }

        public void prepare() {
            try {
                this.mCb.onPrepare();
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in prepare.", e);
            }
        }

        public void prepareFromMediaId(String mediaId, Bundle extras) {
            try {
                this.mCb.onPrepareFromMediaId(mediaId, extras);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in prepareFromMediaId.", e);
            }
        }

        public void prepareFromSearch(String query, Bundle extras) {
            try {
                this.mCb.onPrepareFromSearch(query, extras);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in prepareFromSearch.", e);
            }
        }

        public void prepareFromUri(Uri uri, Bundle extras) {
            try {
                this.mCb.onPrepareFromUri(uri, extras);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in prepareFromUri.", e);
            }
        }

        public void play() {
            try {
                this.mCb.onPlay();
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in play.", e);
            }
        }

        public void playFromMediaId(String mediaId, Bundle extras) {
            try {
                this.mCb.onPlayFromMediaId(mediaId, extras);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in playFromMediaId.", e);
            }
        }

        public void playFromSearch(String query, Bundle extras) {
            try {
                this.mCb.onPlayFromSearch(query, extras);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in playFromSearch.", e);
            }
        }

        public void playFromUri(Uri uri, Bundle extras) {
            try {
                this.mCb.onPlayFromUri(uri, extras);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in playFromUri.", e);
            }
        }

        public void skipToTrack(long id) {
            try {
                this.mCb.onSkipToTrack(id);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in skipToTrack", e);
            }
        }

        public void pause() {
            try {
                this.mCb.onPause();
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in pause.", e);
            }
        }

        public void stop() {
            try {
                this.mCb.onStop();
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in stop.", e);
            }
        }

        public void next() {
            try {
                this.mCb.onNext();
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in next.", e);
            }
        }

        public void previous() {
            try {
                this.mCb.onPrevious();
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in previous.", e);
            }
        }

        public void fastForward() {
            try {
                this.mCb.onFastForward();
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in fastForward.", e);
            }
        }

        public void rewind() {
            try {
                this.mCb.onRewind();
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in rewind.", e);
            }
        }

        public void seekTo(long pos) {
            try {
                this.mCb.onSeekTo(pos);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in seekTo.", e);
            }
        }

        public void rate(Rating rating) {
            try {
                this.mCb.onRate(rating);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in rate.", e);
            }
        }

        public void adjustVolume(int direction) {
            try {
                this.mCb.onAdjustVolume(direction);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in adjustVolume.", e);
            }
        }

        public void setVolumeTo(int value) {
            try {
                this.mCb.onSetVolumeTo(value);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in setVolumeTo.", e);
            }
        }
    }

    class ControllerStub extends ISessionController.Stub {
        ControllerStub() {
        }

        public void sendCommand(String command, Bundle args, ResultReceiver cb) throws RemoteException {
            MediaSessionRecord.this.updateCallingPackage();
            MediaSessionRecord.this.mSessionCb.sendCommand(command, args, cb);
        }

        public boolean sendMediaButton(KeyEvent mediaButtonIntent) {
            MediaSessionRecord.this.updateCallingPackage();
            return MediaSessionRecord.this.mSessionCb.sendMediaButton(mediaButtonIntent, 0, null);
        }

        public void registerCallbackListener(ISessionControllerCallback cb) {
            synchronized (MediaSessionRecord.this.mLock) {
                if (MediaSessionRecord.this.mDestroyed) {
                    try {
                        cb.onSessionDestroyed();
                    } catch (Exception e) {
                    }
                    return;
                }
                if (MediaSessionRecord.this.getControllerCbIndexForCb(cb) < 0) {
                    MediaSessionRecord.this.mControllerCallbacks.add(cb);
                    if (MediaSessionRecord.DEBUG) {
                        Log.d(MediaSessionRecord.TAG, "registering controller callback " + cb);
                    }
                }
            }
        }

        public void unregisterCallbackListener(ISessionControllerCallback cb) throws RemoteException {
            synchronized (MediaSessionRecord.this.mLock) {
                int index = MediaSessionRecord.this.getControllerCbIndexForCb(cb);
                if (index != -1) {
                    MediaSessionRecord.this.mControllerCallbacks.remove(index);
                }
                if (MediaSessionRecord.DEBUG) {
                    Log.d(MediaSessionRecord.TAG, "unregistering callback " + cb + ". index=" + index);
                }
            }
        }

        public String getPackageName() {
            return MediaSessionRecord.this.mPackageName;
        }

        public String getTag() {
            return MediaSessionRecord.this.mTag;
        }

        public PendingIntent getLaunchPendingIntent() {
            return MediaSessionRecord.this.mLaunchIntent;
        }

        public long getFlags() {
            return MediaSessionRecord.this.mFlags;
        }

        public ParcelableVolumeInfo getVolumeAttributes() {
            synchronized (MediaSessionRecord.this.mLock) {
                if (MediaSessionRecord.this.mVolumeType == 2) {
                    int current = MediaSessionRecord.this.mOptimisticVolume != -1 ? MediaSessionRecord.this.mOptimisticVolume : MediaSessionRecord.this.mCurrentVolume;
                    return new ParcelableVolumeInfo(MediaSessionRecord.this.mVolumeType, MediaSessionRecord.this.mAudioAttrs, MediaSessionRecord.this.mVolumeControlType, MediaSessionRecord.this.mMaxVolume, current);
                }
                int volumeType = MediaSessionRecord.this.mVolumeType;
                AudioAttributes attributes = MediaSessionRecord.this.mAudioAttrs;
                int stream = AudioAttributes.toLegacyStreamType(attributes);
                int max = MediaSessionRecord.this.mAudioManager.getStreamMaxVolume(stream);
                int current2 = MediaSessionRecord.this.mAudioManager.getStreamVolume(stream);
                return new ParcelableVolumeInfo(volumeType, attributes, 2, max, current2);
            }
        }

        public void adjustVolume(int direction, int flags, String packageName) {
            MediaSessionRecord.this.updateCallingPackage();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                MediaSessionRecord.this.adjustVolume(direction, flags, packageName, uid, false);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setVolumeTo(int value, int flags, String packageName) {
            MediaSessionRecord.this.updateCallingPackage();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                MediaSessionRecord.this.setVolumeTo(value, flags, packageName, uid);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void prepare() throws RemoteException {
            MediaSessionRecord.this.updateCallingPackage();
            MediaSessionRecord.this.mSessionCb.prepare();
        }

        public void prepareFromMediaId(String mediaId, Bundle extras) throws RemoteException {
            MediaSessionRecord.this.updateCallingPackage();
            MediaSessionRecord.this.mSessionCb.prepareFromMediaId(mediaId, extras);
        }

        public void prepareFromSearch(String query, Bundle extras) throws RemoteException {
            MediaSessionRecord.this.updateCallingPackage();
            MediaSessionRecord.this.mSessionCb.prepareFromSearch(query, extras);
        }

        public void prepareFromUri(Uri uri, Bundle extras) throws RemoteException {
            MediaSessionRecord.this.updateCallingPackage();
            MediaSessionRecord.this.mSessionCb.prepareFromUri(uri, extras);
        }

        public void play() throws RemoteException {
            MediaSessionRecord.this.updateCallingPackage();
            MediaSessionRecord.this.mSessionCb.play();
        }

        public void playFromMediaId(String mediaId, Bundle extras) throws RemoteException {
            MediaSessionRecord.this.updateCallingPackage();
            MediaSessionRecord.this.mSessionCb.playFromMediaId(mediaId, extras);
        }

        public void playFromSearch(String query, Bundle extras) throws RemoteException {
            MediaSessionRecord.this.updateCallingPackage();
            MediaSessionRecord.this.mSessionCb.playFromSearch(query, extras);
        }

        public void playFromUri(Uri uri, Bundle extras) throws RemoteException {
            MediaSessionRecord.this.updateCallingPackage();
            MediaSessionRecord.this.mSessionCb.playFromUri(uri, extras);
        }

        public void skipToQueueItem(long id) {
            MediaSessionRecord.this.updateCallingPackage();
            MediaSessionRecord.this.mSessionCb.skipToTrack(id);
        }

        public void pause() throws RemoteException {
            MediaSessionRecord.this.updateCallingPackage();
            MediaSessionRecord.this.mSessionCb.pause();
        }

        public void stop() throws RemoteException {
            MediaSessionRecord.this.updateCallingPackage();
            MediaSessionRecord.this.mSessionCb.stop();
        }

        public void next() throws RemoteException {
            MediaSessionRecord.this.updateCallingPackage();
            MediaSessionRecord.this.mSessionCb.next();
        }

        public void previous() throws RemoteException {
            MediaSessionRecord.this.updateCallingPackage();
            MediaSessionRecord.this.mSessionCb.previous();
        }

        public void fastForward() throws RemoteException {
            MediaSessionRecord.this.updateCallingPackage();
            MediaSessionRecord.this.mSessionCb.fastForward();
        }

        public void rewind() throws RemoteException {
            MediaSessionRecord.this.updateCallingPackage();
            MediaSessionRecord.this.mSessionCb.rewind();
        }

        public void seekTo(long pos) throws RemoteException {
            MediaSessionRecord.this.updateCallingPackage();
            MediaSessionRecord.this.mSessionCb.seekTo(pos);
        }

        public void rate(Rating rating) throws RemoteException {
            MediaSessionRecord.this.updateCallingPackage();
            MediaSessionRecord.this.mSessionCb.rate(rating);
        }

        public void sendCustomAction(String action, Bundle args) throws RemoteException {
            MediaSessionRecord.this.updateCallingPackage();
            MediaSessionRecord.this.mSessionCb.sendCustomAction(action, args);
        }

        public MediaMetadata getMetadata() {
            MediaMetadata mediaMetadata;
            synchronized (MediaSessionRecord.this.mLock) {
                mediaMetadata = MediaSessionRecord.this.mMetadata;
            }
            return mediaMetadata;
        }

        public PlaybackState getPlaybackState() {
            return MediaSessionRecord.this.getStateWithUpdatedPosition();
        }

        public ParceledListSlice getQueue() {
            ParceledListSlice parceledListSlice;
            synchronized (MediaSessionRecord.this.mLock) {
                parceledListSlice = MediaSessionRecord.this.mQueue;
            }
            return parceledListSlice;
        }

        public CharSequence getQueueTitle() {
            return MediaSessionRecord.this.mQueueTitle;
        }

        public Bundle getExtras() {
            Bundle bundle;
            synchronized (MediaSessionRecord.this.mLock) {
                bundle = MediaSessionRecord.this.mExtras;
            }
            return bundle;
        }

        public int getRatingType() {
            return MediaSessionRecord.this.mRatingType;
        }

        public boolean isTransportControlEnabled() {
            return MediaSessionRecord.this.isTransportControlEnabled();
        }
    }

    private class MessageHandler extends Handler {
        private static final int MSG_ACTIVE_SESSION_CHANGED = 10;
        private static final int MSG_DESTROYED = 9;
        private static final int MSG_SEND_EVENT = 6;
        private static final int MSG_UPDATE_EXTRAS = 5;
        private static final int MSG_UPDATE_METADATA = 1;
        private static final int MSG_UPDATE_PLAYBACK_STATE = 2;
        private static final int MSG_UPDATE_QUEUE = 3;
        private static final int MSG_UPDATE_QUEUE_TITLE = 4;
        private static final int MSG_UPDATE_SESSION_STATE = 7;
        private static final int MSG_UPDATE_VOLUME = 8;

        public MessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MediaSessionRecord.this.pushMetadataUpdate();
                    break;
                case 2:
                    MediaSessionRecord.this.pushPlaybackStateUpdate();
                    break;
                case 3:
                    MediaSessionRecord.this.pushQueueUpdate();
                    break;
                case 4:
                    MediaSessionRecord.this.pushQueueTitleUpdate();
                    break;
                case 5:
                    MediaSessionRecord.this.pushExtrasUpdate();
                    break;
                case 6:
                    MediaSessionRecord.this.pushEvent((String) msg.obj, msg.getData());
                    break;
                case 8:
                    MediaSessionRecord.this.pushVolumeUpdate();
                    break;
                case 9:
                    MediaSessionRecord.this.pushSessionDestroyed();
                    break;
                case 10:
                    MediaSessionRecord.this.mService.onSessionPlaystateChange(MediaSessionRecord.this, msg.arg1, msg.arg2);
                    break;
            }
        }

        public void post(int what) {
            post(what, null);
        }

        public void post(int what, Object obj) {
            obtainMessage(what, obj).sendToTarget();
        }

        public void post(int what, Object obj, Bundle data) {
            Message msg = obtainMessage(what, obj);
            msg.setData(data);
            msg.sendToTarget();
        }

        public void post(int what, Object obj, int arg1, int arg2) {
            Message msg = obtainMessage(what, arg1, arg2, obj);
            msg.sendToTarget();
        }
    }
}
