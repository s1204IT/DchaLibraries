package com.android.server.media;

import android.R;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioManagerInternal;
import android.media.AudioSystem;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.Rating;
import android.media.session.ISession;
import android.media.session.ISessionCallback;
import android.media.session.ISessionController;
import android.media.session.ISessionControllerCallback;
import android.media.session.MediaSession;
import android.media.session.ParcelableVolumeInfo;
import android.media.session.PlaybackState;
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
import com.android.server.voiceinteraction.SoundTriggerHelper;
import java.io.PrintWriter;
import java.util.ArrayList;

public class MediaSessionRecord implements IBinder.DeathRecipient {
    private static final int ACTIVE_BUFFER = 30000;
    private static final boolean DEBUG = false;
    private static final int OPTIMISTIC_VOLUME_TIMEOUT = 1000;
    private static final String TAG = "MediaSessionRecord";
    private AudioManager mAudioManager;
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
    private final boolean mUseMasterVolume;
    private final int mUserId;
    private final Object mLock = new Object();
    private final ArrayList<ISessionControllerCallback> mControllerCallbacks = new ArrayList<>();
    private int mVolumeType = 1;
    private int mVolumeControlType = 2;
    private int mMaxVolume = 0;
    private int mCurrentVolume = 0;
    private int mOptimisticVolume = -1;
    private boolean mIsActive = DEBUG;
    private boolean mDestroyed = DEBUG;
    private final Runnable mClearOptimisticVolumeRunnable = new Runnable() {
        @Override
        public void run() {
            boolean needUpdate = MediaSessionRecord.this.mOptimisticVolume != MediaSessionRecord.this.mCurrentVolume ? true : MediaSessionRecord.DEBUG;
            MediaSessionRecord.this.mOptimisticVolume = -1;
            if (needUpdate) {
                MediaSessionRecord.this.pushVolumeUpdate();
            }
        }
    };
    private final ControllerStub mController = new ControllerStub();
    private final SessionStub mSession = new SessionStub();
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
        this.mUseMasterVolume = service.getContext().getResources().getBoolean(R.^attr-private.alertDialogCenterButtons);
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
        if ((this.mFlags & ((long) flag)) != 0) {
            return true;
        }
        return DEBUG;
    }

    public int getUserId() {
        return this.mUserId;
    }

    public boolean isSystemPriority() {
        if ((this.mFlags & 65536) != 0) {
            return true;
        }
        return DEBUG;
    }

    public void adjustVolume(int direction, int flags, String packageName, int uid, boolean useSuggested) {
        int previousFlagPlaySound = flags & 4;
        if (isPlaybackActive(DEBUG) || hasFlag(65536)) {
            flags &= -5;
        }
        boolean isMute = direction == -99 ? true : DEBUG;
        if (direction > 1) {
            direction = 1;
        } else if (direction < -1) {
            direction = -1;
        }
        if (this.mVolumeType == 1) {
            if (this.mUseMasterVolume) {
                boolean isMasterMute = this.mAudioManager.isMasterMute();
                if (isMute) {
                    this.mAudioManagerInternal.setMasterMuteForUid(!isMasterMute ? true : DEBUG, flags, packageName, this.mService.mICallback, uid);
                    return;
                } else {
                    this.mAudioManagerInternal.adjustMasterVolumeForUid(direction, flags, packageName, uid);
                    if (isMasterMute) {
                        this.mAudioManagerInternal.setMasterMuteForUid(DEBUG, flags, packageName, this.mService.mICallback, uid);
                        return;
                    }
                    return;
                }
            }
            int stream = AudioAttributes.toLegacyStreamType(this.mAudioAttrs);
            boolean isStreamMute = this.mAudioManager.isStreamMute(stream);
            if (useSuggested) {
                if (AudioSystem.isStreamActive(stream, 0)) {
                    if (isMute) {
                        this.mAudioManager.setStreamMute(stream, !isStreamMute ? true : DEBUG);
                        return;
                    }
                    this.mAudioManagerInternal.adjustSuggestedStreamVolumeForUid(stream, direction, flags, packageName, uid);
                    if (isStreamMute && direction != 0) {
                        this.mAudioManager.setStreamMute(stream, DEBUG);
                        return;
                    }
                    return;
                }
                int flags2 = flags | previousFlagPlaySound;
                boolean isStreamMute2 = this.mAudioManager.isStreamMute(SoundTriggerHelper.STATUS_ERROR);
                if (isMute) {
                    this.mAudioManager.setStreamMute(SoundTriggerHelper.STATUS_ERROR, !isStreamMute2 ? true : DEBUG);
                    return;
                }
                this.mAudioManagerInternal.adjustSuggestedStreamVolumeForUid(SoundTriggerHelper.STATUS_ERROR, direction, flags2, packageName, uid);
                if (isStreamMute2 && direction != 0) {
                    this.mAudioManager.setStreamMute(SoundTriggerHelper.STATUS_ERROR, DEBUG);
                    return;
                }
                return;
            }
            if (isMute) {
                this.mAudioManager.setStreamMute(stream, !isStreamMute ? true : DEBUG);
                return;
            }
            this.mAudioManagerInternal.adjustStreamVolumeForUid(stream, direction, flags, packageName, uid);
            if (isStreamMute && direction != 0) {
                this.mAudioManager.setStreamMute(stream, DEBUG);
                return;
            }
            return;
        }
        if (this.mVolumeControlType != 0) {
            if (isMute) {
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
        }
    }

    public void setVolumeTo(int value, int flags, String packageName, int uid) {
        if (this.mVolumeType == 1) {
            int stream = AudioAttributes.toLegacyStreamType(this.mAudioAttrs);
            this.mAudioManagerInternal.setStreamVolumeForUid(stream, value, flags, packageName, uid);
            return;
        }
        if (this.mVolumeControlType == 2) {
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
        }
    }

    public boolean isActive() {
        if (!this.mIsActive || this.mDestroyed) {
            return DEBUG;
        }
        return true;
    }

    public boolean isPlaybackActive(boolean includeRecentlyActive) {
        int state = this.mPlaybackState == null ? 0 : this.mPlaybackState.getState();
        if (MediaSession.isActiveState(state)) {
            return true;
        }
        if (!includeRecentlyActive) {
            return DEBUG;
        }
        PlaybackState playbackState = this.mPlaybackState;
        if (state != 2) {
            return DEBUG;
        }
        long inactiveTime = SystemClock.uptimeMillis() - this.mLastActiveTime;
        if (inactiveTime < 30000) {
            return true;
        }
        return DEBUG;
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
            if (!this.mDestroyed) {
                this.mDestroyed = true;
                this.mHandler.post(9);
            }
        }
    }

    public ISessionCallback getCallback() {
        return this.mSessionCb.mCb;
    }

    public void sendMediaButton(KeyEvent ke, int sequenceId, ResultReceiver cb) {
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
        pw.println(indent + "state=" + (this.mPlaybackState == null ? null : this.mPlaybackState.toString()));
        pw.println(indent + "audioAttrs=" + this.mAudioAttrs);
        pw.println(indent + "volumeType=" + this.mVolumeType + ", controlType=" + this.mVolumeControlType + ", max=" + this.mMaxVolume + ", current=" + this.mCurrentVolume);
        pw.println(indent + "metadata:" + getShortMetadataString());
        pw.println(indent + "queueTitle=" + ((Object) this.mQueueTitle) + ", size=" + (this.mQueue == null ? 0 : this.mQueue.getList().size()));
    }

    public String toString() {
        return this.mPackageName + "/" + this.mTag;
    }

    private String getShortMetadataString() {
        int fields = this.mMetadata == null ? 0 : this.mMetadata.size();
        MediaDescription description = this.mMetadata == null ? null : this.mMetadata.getDescription();
        return "size=" + fields + ", description=" + description;
    }

    private void pushPlaybackStateUpdate() {
        synchronized (this.mLock) {
            if (!this.mDestroyed) {
                for (int i = this.mControllerCallbacks.size() - 1; i >= 0; i--) {
                    ISessionControllerCallback cb = this.mControllerCallbacks.get(i);
                    try {
                        try {
                            cb.onPlaybackStateChanged(this.mPlaybackState);
                        } catch (DeadObjectException e) {
                            this.mControllerCallbacks.remove(i);
                            Log.w(TAG, "Removed dead callback in pushPlaybackStateUpdate.", e);
                        }
                    } catch (RemoteException e2) {
                        Log.w(TAG, "unexpected exception in pushPlaybackStateUpdate.", e2);
                    }
                }
            }
        }
    }

    private void pushMetadataUpdate() {
        synchronized (this.mLock) {
            if (!this.mDestroyed) {
                for (int i = this.mControllerCallbacks.size() - 1; i >= 0; i--) {
                    ISessionControllerCallback cb = this.mControllerCallbacks.get(i);
                    try {
                        try {
                            cb.onMetadataChanged(this.mMetadata);
                        } catch (DeadObjectException e) {
                            Log.w(TAG, "Removing dead callback in pushMetadataUpdate. ", e);
                            this.mControllerCallbacks.remove(i);
                        }
                    } catch (RemoteException e2) {
                        Log.w(TAG, "unexpected exception in pushMetadataUpdate. ", e2);
                    }
                }
            }
        }
    }

    private void pushQueueUpdate() {
        synchronized (this.mLock) {
            if (!this.mDestroyed) {
                for (int i = this.mControllerCallbacks.size() - 1; i >= 0; i--) {
                    ISessionControllerCallback cb = this.mControllerCallbacks.get(i);
                    try {
                        try {
                            cb.onQueueChanged(this.mQueue);
                        } catch (DeadObjectException e) {
                            this.mControllerCallbacks.remove(i);
                            Log.w(TAG, "Removed dead callback in pushQueueUpdate.", e);
                        }
                    } catch (RemoteException e2) {
                        Log.w(TAG, "unexpected exception in pushQueueUpdate.", e2);
                    }
                }
            }
        }
    }

    private void pushQueueTitleUpdate() {
        synchronized (this.mLock) {
            if (!this.mDestroyed) {
                for (int i = this.mControllerCallbacks.size() - 1; i >= 0; i--) {
                    ISessionControllerCallback cb = this.mControllerCallbacks.get(i);
                    try {
                        try {
                            cb.onQueueTitleChanged(this.mQueueTitle);
                        } catch (DeadObjectException e) {
                            this.mControllerCallbacks.remove(i);
                            Log.w(TAG, "Removed dead callback in pushQueueTitleUpdate.", e);
                        }
                    } catch (RemoteException e2) {
                        Log.w(TAG, "unexpected exception in pushQueueTitleUpdate.", e2);
                    }
                }
            }
        }
    }

    private void pushExtrasUpdate() {
        synchronized (this.mLock) {
            if (!this.mDestroyed) {
                for (int i = this.mControllerCallbacks.size() - 1; i >= 0; i--) {
                    ISessionControllerCallback cb = this.mControllerCallbacks.get(i);
                    try {
                        try {
                            cb.onExtrasChanged(this.mExtras);
                        } catch (DeadObjectException e) {
                            this.mControllerCallbacks.remove(i);
                            Log.w(TAG, "Removed dead callback in pushExtrasUpdate.", e);
                        }
                    } catch (RemoteException e2) {
                        Log.w(TAG, "unexpected exception in pushExtrasUpdate.", e2);
                    }
                }
            }
        }
    }

    private void pushVolumeUpdate() {
        synchronized (this.mLock) {
            if (!this.mDestroyed) {
                ParcelableVolumeInfo info = this.mController.getVolumeAttributes();
                for (int i = this.mControllerCallbacks.size() - 1; i >= 0; i--) {
                    ISessionControllerCallback cb = this.mControllerCallbacks.get(i);
                    try {
                        try {
                            cb.onVolumeInfoChanged(info);
                        } catch (RemoteException e) {
                            Log.w(TAG, "Unexpected exception in pushVolumeUpdate. ", e);
                        }
                    } catch (DeadObjectException e2) {
                        Log.w(TAG, "Removing dead callback in pushVolumeUpdate. ", e2);
                    }
                }
            }
        }
    }

    private void pushEvent(String event, Bundle data) {
        synchronized (this.mLock) {
            if (!this.mDestroyed) {
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
    }

    private void pushSessionDestroyed() {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
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

    private final class SessionStub extends ISession.Stub {
        private SessionStub() {
        }

        public void destroy() {
            MediaSessionRecord.this.mService.destroySession(MediaSessionRecord.this);
        }

        public void sendEvent(String event, Bundle data) {
            MediaSessionRecord.this.mHandler.post(6, event, data == null ? null : new Bundle(data));
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
                MediaMetadata temp = metadata == null ? null : new MediaMetadata.Builder(metadata).build();
                if (temp != null) {
                    temp.size();
                }
                MediaSessionRecord.this.mMetadata = temp;
            }
            MediaSessionRecord.this.mHandler.post(1);
        }

        public void setPlaybackState(PlaybackState state) {
            int oldState = MediaSessionRecord.this.mPlaybackState == null ? 0 : MediaSessionRecord.this.mPlaybackState.getState();
            int newState = state != null ? state.getState() : 0;
            if (MediaSession.isActiveState(oldState) && newState == 2) {
                MediaSessionRecord.this.mLastActiveTime = SystemClock.elapsedRealtime();
            }
            synchronized (MediaSessionRecord.this.mLock) {
                MediaSessionRecord.this.mPlaybackState = state;
            }
            MediaSessionRecord.this.mService.onSessionPlaystateChange(MediaSessionRecord.this, oldState, newState);
            MediaSessionRecord.this.mHandler.post(2);
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
                MediaSessionRecord.this.mExtras = extras == null ? null : new Bundle(extras);
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
                typeChanged = MediaSessionRecord.this.mVolumeType != 2 ? MediaSessionRecord.DEBUG : true;
                MediaSessionRecord.this.mVolumeType = 1;
                if (attributes != null) {
                    MediaSessionRecord.this.mAudioAttrs = attributes;
                } else {
                    Log.e(MediaSessionRecord.TAG, "Received null audio attributes, using existing attributes");
                }
            }
            if (typeChanged) {
                MediaSessionRecord.this.mService.onSessionPlaybackTypeChanged(MediaSessionRecord.this);
            }
        }

        public void setPlaybackToRemote(int control, int max) {
            boolean typeChanged;
            synchronized (MediaSessionRecord.this.mLock) {
                typeChanged = MediaSessionRecord.this.mVolumeType != 1 ? MediaSessionRecord.DEBUG : true;
                MediaSessionRecord.this.mVolumeType = 2;
                MediaSessionRecord.this.mVolumeControlType = control;
                MediaSessionRecord.this.mMaxVolume = max;
            }
            if (typeChanged) {
                MediaSessionRecord.this.mService.onSessionPlaybackTypeChanged(MediaSessionRecord.this);
            }
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
                return MediaSessionRecord.DEBUG;
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
                Slog.e(MediaSessionRecord.TAG, "Remote failure in playUri.", e);
            }
        }

        public void playFromSearch(String query, Bundle extras) {
            try {
                this.mCb.onPlayFromSearch(query, extras);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in playFromSearch.", e);
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
            MediaSessionRecord.this.mSessionCb.sendCommand(command, args, cb);
        }

        public boolean sendMediaButton(KeyEvent mediaButtonIntent) {
            return MediaSessionRecord.this.mSessionCb.sendMediaButton(mediaButtonIntent, 0, null);
        }

        public void registerCallbackListener(ISessionControllerCallback cb) {
            synchronized (MediaSessionRecord.this.mLock) {
                if (!MediaSessionRecord.this.mDestroyed) {
                    if (MediaSessionRecord.this.getControllerCbIndexForCb(cb) < 0) {
                        MediaSessionRecord.this.mControllerCallbacks.add(cb);
                    }
                } else {
                    try {
                        cb.onSessionDestroyed();
                    } catch (Exception e) {
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
            int type;
            int max;
            int current;
            ParcelableVolumeInfo parcelableVolumeInfo;
            synchronized (MediaSessionRecord.this.mLock) {
                if (MediaSessionRecord.this.mVolumeType == 2) {
                    type = MediaSessionRecord.this.mVolumeControlType;
                    max = MediaSessionRecord.this.mMaxVolume;
                    current = MediaSessionRecord.this.mOptimisticVolume != -1 ? MediaSessionRecord.this.mOptimisticVolume : MediaSessionRecord.this.mCurrentVolume;
                } else {
                    int stream = AudioAttributes.toLegacyStreamType(MediaSessionRecord.this.mAudioAttrs);
                    type = 2;
                    max = MediaSessionRecord.this.mAudioManager.getStreamMaxVolume(stream);
                    current = MediaSessionRecord.this.mAudioManager.getStreamVolume(stream);
                }
                parcelableVolumeInfo = new ParcelableVolumeInfo(MediaSessionRecord.this.mVolumeType, MediaSessionRecord.this.mAudioAttrs, type, max, current);
            }
            return parcelableVolumeInfo;
        }

        public void adjustVolume(int direction, int flags, String packageName) {
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                MediaSessionRecord.this.adjustVolume(direction, flags, packageName, uid, MediaSessionRecord.DEBUG);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setVolumeTo(int value, int flags, String packageName) {
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                MediaSessionRecord.this.setVolumeTo(value, flags, packageName, uid);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void play() throws RemoteException {
            MediaSessionRecord.this.mSessionCb.play();
        }

        public void playFromMediaId(String mediaId, Bundle extras) throws RemoteException {
            MediaSessionRecord.this.mSessionCb.playFromMediaId(mediaId, extras);
        }

        public void playFromSearch(String query, Bundle extras) throws RemoteException {
            MediaSessionRecord.this.mSessionCb.playFromSearch(query, extras);
        }

        public void skipToQueueItem(long id) {
            MediaSessionRecord.this.mSessionCb.skipToTrack(id);
        }

        public void pause() throws RemoteException {
            MediaSessionRecord.this.mSessionCb.pause();
        }

        public void stop() throws RemoteException {
            MediaSessionRecord.this.mSessionCb.stop();
        }

        public void next() throws RemoteException {
            MediaSessionRecord.this.mSessionCb.next();
        }

        public void previous() throws RemoteException {
            MediaSessionRecord.this.mSessionCb.previous();
        }

        public void fastForward() throws RemoteException {
            MediaSessionRecord.this.mSessionCb.fastForward();
        }

        public void rewind() throws RemoteException {
            MediaSessionRecord.this.mSessionCb.rewind();
        }

        public void seekTo(long pos) throws RemoteException {
            MediaSessionRecord.this.mSessionCb.seekTo(pos);
        }

        public void rate(Rating rating) throws RemoteException {
            MediaSessionRecord.this.mSessionCb.rate(rating);
        }

        public void sendCustomAction(String action, Bundle args) throws RemoteException {
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
    }
}
