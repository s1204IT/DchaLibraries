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
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;
import android.view.KeyEvent;
import com.android.server.LocalServices;
import com.android.server.slice.SliceClientPermissions;
import java.io.PrintWriter;
import java.util.ArrayList;

public class MediaSessionRecord implements IBinder.DeathRecipient {
    private static final boolean DEBUG;
    private static final int OPTIMISTIC_VOLUME_TIMEOUT = 1000;
    private static final String TAG = "MediaSessionRecord";
    private AudioManager mAudioManager;
    private final Context mContext;
    private Bundle mExtras;
    private long mFlags;
    private final MessageHandler mHandler;
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
    private final Object mLock = new Object();
    private final ArrayList<ISessionControllerCallbackHolder> mControllerCallbackHolders = new ArrayList<>();
    private int mVolumeType = 1;
    private int mVolumeControlType = 2;
    private int mMaxVolume = 0;
    private int mCurrentVolume = 0;
    private int mOptimisticVolume = -1;
    private boolean mIsActive = false;
    private boolean mDestroyed = false;
    private final Runnable mClearOptimisticVolumeRunnable = new Runnable() {
        @Override
        public void run() {
            boolean z = MediaSessionRecord.this.mOptimisticVolume != MediaSessionRecord.this.mCurrentVolume;
            MediaSessionRecord.this.mOptimisticVolume = -1;
            if (z) {
                MediaSessionRecord.this.pushVolumeUpdate();
            }
        }
    };
    private final ControllerStub mController = new ControllerStub();
    private final SessionStub mSession = new SessionStub();
    private AudioManagerInternal mAudioManagerInternal = (AudioManagerInternal) LocalServices.getService(AudioManagerInternal.class);
    private AudioAttributes mAudioAttrs = new AudioAttributes.Builder().setUsage(1).build();

    static {
        DEBUG = Log.isLoggable(TAG, 3) || !"user".equals(Build.TYPE);
    }

    public MediaSessionRecord(int i, int i2, int i3, String str, ISessionCallback iSessionCallback, String str2, MediaSessionService mediaSessionService, Looper looper) {
        this.mOwnerPid = i;
        this.mOwnerUid = i2;
        this.mUserId = i3;
        this.mPackageName = str;
        this.mTag = str2;
        this.mSessionCb = new SessionCb(iSessionCallback);
        this.mService = mediaSessionService;
        this.mContext = this.mService.getContext();
        this.mHandler = new MessageHandler(looper);
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
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

    public boolean hasFlag(int i) {
        return (this.mFlags & ((long) i)) != 0;
    }

    public int getUid() {
        return this.mOwnerUid;
    }

    public int getUserId() {
        return this.mUserId;
    }

    public boolean isSystemPriority() {
        return (this.mFlags & 65536) != 0;
    }

    public void adjustVolume(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, boolean z, int i3, int i4, boolean z2) {
        int i5;
        int i6 = i4 & 4;
        if (isPlaybackActive() || hasFlag(65536)) {
            i5 = i4 & (-5);
        } else {
            i5 = i4;
        }
        if (this.mVolumeType == 1) {
            postAdjustLocalVolume(AudioAttributes.toLegacyStreamType(this.mAudioAttrs), i3, i5, str, i2, z2, i6);
            return;
        }
        if (this.mVolumeControlType == 0) {
            return;
        }
        if (i3 == 101 || i3 == -100 || i3 == 100) {
            Log.w(TAG, "Muting remote playback is not supported");
            return;
        }
        this.mSessionCb.adjustVolume(str, i, i2, iSessionControllerCallback, z, i3);
        int i7 = this.mOptimisticVolume < 0 ? this.mCurrentVolume : this.mOptimisticVolume;
        this.mOptimisticVolume = i7 + i3;
        this.mOptimisticVolume = Math.max(0, Math.min(this.mOptimisticVolume, this.mMaxVolume));
        this.mHandler.removeCallbacks(this.mClearOptimisticVolumeRunnable);
        this.mHandler.postDelayed(this.mClearOptimisticVolumeRunnable, 1000L);
        if (i7 != this.mOptimisticVolume) {
            pushVolumeUpdate();
        }
        this.mService.notifyRemoteVolumeChanged(i5, this);
        if (DEBUG) {
            Log.d(TAG, "Adjusted optimistic volume to " + this.mOptimisticVolume + " max is " + this.mMaxVolume);
        }
    }

    private void setVolumeTo(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, int i3, int i4) {
        if (this.mVolumeType == 1) {
            this.mAudioManagerInternal.setStreamVolumeForUid(AudioAttributes.toLegacyStreamType(this.mAudioAttrs), i3, i4, str, i2);
            return;
        }
        if (this.mVolumeControlType != 2) {
            return;
        }
        int iMax = Math.max(0, Math.min(i3, this.mMaxVolume));
        this.mSessionCb.setVolumeTo(str, i, i2, iSessionControllerCallback, iMax);
        int i5 = this.mOptimisticVolume < 0 ? this.mCurrentVolume : this.mOptimisticVolume;
        this.mOptimisticVolume = Math.max(0, Math.min(iMax, this.mMaxVolume));
        this.mHandler.removeCallbacks(this.mClearOptimisticVolumeRunnable);
        this.mHandler.postDelayed(this.mClearOptimisticVolumeRunnable, 1000L);
        if (i5 != this.mOptimisticVolume) {
            pushVolumeUpdate();
        }
        this.mService.notifyRemoteVolumeChanged(i4, this);
        if (DEBUG) {
            Log.d(TAG, "Set optimistic volume to " + this.mOptimisticVolume + " max is " + this.mMaxVolume);
        }
    }

    public boolean isActive() {
        return this.mIsActive && !this.mDestroyed;
    }

    public PlaybackState getPlaybackState() {
        return this.mPlaybackState;
    }

    public boolean isPlaybackActive() {
        return MediaSession.isActiveState(this.mPlaybackState == null ? 0 : this.mPlaybackState.getState());
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

    public void sendMediaButton(String str, int i, int i2, boolean z, KeyEvent keyEvent, int i3, ResultReceiver resultReceiver) {
        this.mSessionCb.sendMediaButton(str, i, i2, z, keyEvent, i3, resultReceiver);
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.println(str + this.mTag + " " + this);
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append("  ");
        String string = sb.toString();
        printWriter.println(string + "ownerPid=" + this.mOwnerPid + ", ownerUid=" + this.mOwnerUid + ", userId=" + this.mUserId);
        StringBuilder sb2 = new StringBuilder();
        sb2.append(string);
        sb2.append("package=");
        sb2.append(this.mPackageName);
        printWriter.println(sb2.toString());
        printWriter.println(string + "launchIntent=" + this.mLaunchIntent);
        printWriter.println(string + "mediaButtonReceiver=" + this.mMediaButtonReceiver);
        printWriter.println(string + "active=" + this.mIsActive);
        printWriter.println(string + "flags=" + this.mFlags);
        printWriter.println(string + "rating type=" + this.mRatingType);
        printWriter.println(string + "controllers: " + this.mControllerCallbackHolders.size());
        StringBuilder sb3 = new StringBuilder();
        sb3.append(string);
        sb3.append("state=");
        sb3.append(this.mPlaybackState == null ? null : this.mPlaybackState.toString());
        printWriter.println(sb3.toString());
        printWriter.println(string + "audioAttrs=" + this.mAudioAttrs);
        printWriter.println(string + "volumeType=" + this.mVolumeType + ", controlType=" + this.mVolumeControlType + ", max=" + this.mMaxVolume + ", current=" + this.mCurrentVolume);
        StringBuilder sb4 = new StringBuilder();
        sb4.append(string);
        sb4.append("metadata:");
        sb4.append(getShortMetadataString());
        printWriter.println(sb4.toString());
        StringBuilder sb5 = new StringBuilder();
        sb5.append(string);
        sb5.append("queueTitle=");
        sb5.append((Object) this.mQueueTitle);
        sb5.append(", size=");
        sb5.append(this.mQueue == null ? 0 : this.mQueue.getList().size());
        printWriter.println(sb5.toString());
    }

    public String toString() {
        return this.mPackageName + SliceClientPermissions.SliceAuthority.DELIMITER + this.mTag + " (userId=" + this.mUserId + ")";
    }

    private void postAdjustLocalVolume(final int i, final int i2, final int i3, final String str, final int i4, final boolean z, final int i5) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!z) {
                        MediaSessionRecord.this.mAudioManagerInternal.adjustStreamVolumeForUid(i, i2, i3, str, i4);
                    } else if (AudioSystem.isStreamActive(i, 0)) {
                        MediaSessionRecord.this.mAudioManagerInternal.adjustSuggestedStreamVolumeForUid(i, i2, i3, str, i4);
                    } else {
                        MediaSessionRecord.this.mAudioManagerInternal.adjustSuggestedStreamVolumeForUid(Integer.MIN_VALUE, i2, i5 | i3, str, i4);
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(MediaSessionRecord.TAG, "Cannot adjust volume: direction=" + i2 + ", stream=" + i + ", flags=" + i3 + ", packageName=" + str + ", uid=" + i4 + ", useSuggested=" + z + ", previousFlagPlaySound=" + i5, e);
                }
            }
        });
    }

    private String getShortMetadataString() {
        return "size=" + (this.mMetadata == null ? 0 : this.mMetadata.size()) + ", description=" + (this.mMetadata == null ? null : this.mMetadata.getDescription());
    }

    private void logCallbackException(String str, ISessionControllerCallbackHolder iSessionControllerCallbackHolder, Exception exc) {
        Log.v(TAG, str + ", this=" + this + ", callback package=" + iSessionControllerCallbackHolder.mPackageName + ", exception=" + exc);
    }

    private void pushPlaybackStateUpdate() {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                return;
            }
            for (int size = this.mControllerCallbackHolders.size() - 1; size >= 0; size--) {
                ISessionControllerCallbackHolder iSessionControllerCallbackHolder = this.mControllerCallbackHolders.get(size);
                try {
                    iSessionControllerCallbackHolder.mCallback.onPlaybackStateChanged(this.mPlaybackState);
                } catch (DeadObjectException e) {
                    this.mControllerCallbackHolders.remove(size);
                    logCallbackException("Removed dead callback in pushPlaybackStateUpdate", iSessionControllerCallbackHolder, e);
                } catch (RemoteException e2) {
                    logCallbackException("unexpected exception in pushPlaybackStateUpdate", iSessionControllerCallbackHolder, e2);
                }
            }
        }
    }

    private void pushMetadataUpdate() {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                return;
            }
            for (int size = this.mControllerCallbackHolders.size() - 1; size >= 0; size--) {
                ISessionControllerCallbackHolder iSessionControllerCallbackHolder = this.mControllerCallbackHolders.get(size);
                try {
                    iSessionControllerCallbackHolder.mCallback.onMetadataChanged(this.mMetadata);
                } catch (DeadObjectException e) {
                    logCallbackException("Removing dead callback in pushMetadataUpdate", iSessionControllerCallbackHolder, e);
                    this.mControllerCallbackHolders.remove(size);
                } catch (RemoteException e2) {
                    logCallbackException("unexpected exception in pushMetadataUpdate", iSessionControllerCallbackHolder, e2);
                }
            }
        }
    }

    private void pushQueueUpdate() {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                return;
            }
            for (int size = this.mControllerCallbackHolders.size() - 1; size >= 0; size--) {
                ISessionControllerCallbackHolder iSessionControllerCallbackHolder = this.mControllerCallbackHolders.get(size);
                try {
                    iSessionControllerCallbackHolder.mCallback.onQueueChanged(this.mQueue);
                } catch (DeadObjectException e) {
                    this.mControllerCallbackHolders.remove(size);
                    logCallbackException("Removed dead callback in pushQueueUpdate", iSessionControllerCallbackHolder, e);
                } catch (RemoteException e2) {
                    logCallbackException("unexpected exception in pushQueueUpdate", iSessionControllerCallbackHolder, e2);
                }
            }
        }
    }

    private void pushQueueTitleUpdate() {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                return;
            }
            for (int size = this.mControllerCallbackHolders.size() - 1; size >= 0; size--) {
                ISessionControllerCallbackHolder iSessionControllerCallbackHolder = this.mControllerCallbackHolders.get(size);
                try {
                    iSessionControllerCallbackHolder.mCallback.onQueueTitleChanged(this.mQueueTitle);
                } catch (DeadObjectException e) {
                    this.mControllerCallbackHolders.remove(size);
                    logCallbackException("Removed dead callback in pushQueueTitleUpdate", iSessionControllerCallbackHolder, e);
                } catch (RemoteException e2) {
                    logCallbackException("unexpected exception in pushQueueTitleUpdate", iSessionControllerCallbackHolder, e2);
                }
            }
        }
    }

    private void pushExtrasUpdate() {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                return;
            }
            for (int size = this.mControllerCallbackHolders.size() - 1; size >= 0; size--) {
                ISessionControllerCallbackHolder iSessionControllerCallbackHolder = this.mControllerCallbackHolders.get(size);
                try {
                    iSessionControllerCallbackHolder.mCallback.onExtrasChanged(this.mExtras);
                } catch (DeadObjectException e) {
                    this.mControllerCallbackHolders.remove(size);
                    logCallbackException("Removed dead callback in pushExtrasUpdate", iSessionControllerCallbackHolder, e);
                } catch (RemoteException e2) {
                    logCallbackException("unexpected exception in pushExtrasUpdate", iSessionControllerCallbackHolder, e2);
                }
            }
        }
    }

    private void pushVolumeUpdate() {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                return;
            }
            ParcelableVolumeInfo volumeAttributes = this.mController.getVolumeAttributes();
            for (int size = this.mControllerCallbackHolders.size() - 1; size >= 0; size--) {
                ISessionControllerCallbackHolder iSessionControllerCallbackHolder = this.mControllerCallbackHolders.get(size);
                try {
                    try {
                        iSessionControllerCallbackHolder.mCallback.onVolumeInfoChanged(volumeAttributes);
                    } catch (RemoteException e) {
                        logCallbackException("Unexpected exception in pushVolumeUpdate", iSessionControllerCallbackHolder, e);
                    }
                } catch (DeadObjectException e2) {
                    this.mControllerCallbackHolders.remove(size);
                    logCallbackException("Removing dead callback in pushVolumeUpdate", iSessionControllerCallbackHolder, e2);
                }
            }
        }
    }

    private void pushEvent(String str, Bundle bundle) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                return;
            }
            for (int size = this.mControllerCallbackHolders.size() - 1; size >= 0; size--) {
                ISessionControllerCallbackHolder iSessionControllerCallbackHolder = this.mControllerCallbackHolders.get(size);
                try {
                    iSessionControllerCallbackHolder.mCallback.onEvent(str, bundle);
                } catch (DeadObjectException e) {
                    this.mControllerCallbackHolders.remove(size);
                    logCallbackException("Removing dead callback in pushEvent", iSessionControllerCallbackHolder, e);
                } catch (RemoteException e2) {
                    logCallbackException("unexpected exception in pushEvent", iSessionControllerCallbackHolder, e2);
                }
            }
        }
    }

    private void pushSessionDestroyed() {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                for (int size = this.mControllerCallbackHolders.size() - 1; size >= 0; size--) {
                    ISessionControllerCallbackHolder iSessionControllerCallbackHolder = this.mControllerCallbackHolders.get(size);
                    try {
                        try {
                            iSessionControllerCallbackHolder.mCallback.onSessionDestroyed();
                        } catch (RemoteException e) {
                            logCallbackException("unexpected exception in pushEvent", iSessionControllerCallbackHolder, e);
                        }
                    } catch (DeadObjectException e2) {
                        logCallbackException("Removing dead callback in pushEvent", iSessionControllerCallbackHolder, e2);
                        this.mControllerCallbackHolders.remove(size);
                    }
                }
                this.mControllerCallbackHolders.clear();
            }
        }
    }

    private PlaybackState getStateWithUpdatedPosition() {
        PlaybackState playbackState;
        long j;
        synchronized (this.mLock) {
            playbackState = this.mPlaybackState;
            if (this.mMetadata != null && this.mMetadata.containsKey("android.media.metadata.DURATION")) {
                j = this.mMetadata.getLong("android.media.metadata.DURATION");
            } else {
                j = -1;
            }
        }
        PlaybackState playbackStateBuild = null;
        if (playbackState != null && (playbackState.getState() == 3 || playbackState.getState() == 4 || playbackState.getState() == 5)) {
            long lastPositionUpdateTime = playbackState.getLastPositionUpdateTime();
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            if (lastPositionUpdateTime > 0) {
                long playbackSpeed = ((long) (playbackState.getPlaybackSpeed() * (jElapsedRealtime - lastPositionUpdateTime))) + playbackState.getPosition();
                long j2 = (j < 0 || playbackSpeed <= j) ? playbackSpeed < 0 ? 0L : playbackSpeed : j;
                PlaybackState.Builder builder = new PlaybackState.Builder(playbackState);
                builder.setState(playbackState.getState(), j2, playbackState.getPlaybackSpeed(), jElapsedRealtime);
                playbackStateBuild = builder.build();
            }
        }
        return playbackStateBuild == null ? playbackState : playbackStateBuild;
    }

    private int getControllerHolderIndexForCb(ISessionControllerCallback iSessionControllerCallback) {
        IBinder iBinderAsBinder = iSessionControllerCallback.asBinder();
        for (int size = this.mControllerCallbackHolders.size() - 1; size >= 0; size--) {
            if (iBinderAsBinder.equals(this.mControllerCallbackHolders.get(size).mCallback.asBinder())) {
                return size;
            }
        }
        return -1;
    }

    private final class SessionStub extends ISession.Stub {
        private SessionStub() {
        }

        public void destroy() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                MediaSessionRecord.this.mService.destroySession(MediaSessionRecord.this);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void sendEvent(String str, Bundle bundle) {
            MediaSessionRecord.this.mHandler.post(6, str, bundle == null ? null : new Bundle(bundle));
        }

        public ISessionController getController() {
            return MediaSessionRecord.this.mController;
        }

        public void setActive(boolean z) {
            MediaSessionRecord.this.mIsActive = z;
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                MediaSessionRecord.this.mService.updateSession(MediaSessionRecord.this);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                MediaSessionRecord.this.mHandler.post(7);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        }

        public void setFlags(int i) {
            int i2 = 65536 & i;
            if (i2 != 0) {
                MediaSessionRecord.this.mService.enforcePhoneStatePermission(getCallingPid(), getCallingUid());
            }
            MediaSessionRecord.this.mFlags = i;
            if (i2 != 0) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    MediaSessionRecord.this.mService.setGlobalPrioritySession(MediaSessionRecord.this);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            MediaSessionRecord.this.mHandler.post(7);
        }

        public void setMediaButtonReceiver(PendingIntent pendingIntent) {
            MediaSessionRecord.this.mMediaButtonReceiver = pendingIntent;
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                MediaSessionRecord.this.mService.onMediaButtonReceiverChanged(MediaSessionRecord.this);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setLaunchPendingIntent(PendingIntent pendingIntent) {
            MediaSessionRecord.this.mLaunchIntent = pendingIntent;
        }

        public void setMetadata(MediaMetadata mediaMetadata) {
            if (MediaSessionRecord.DEBUG) {
                Log.d(MediaSessionRecord.TAG, "setMetadata from u/pid:" + Binder.getCallingUid() + SliceClientPermissions.SliceAuthority.DELIMITER + Binder.getCallingPid());
            }
            synchronized (MediaSessionRecord.this.mLock) {
                MediaMetadata mediaMetadataBuild = mediaMetadata == null ? null : new MediaMetadata.Builder(mediaMetadata).build();
                if (mediaMetadataBuild != null) {
                    mediaMetadataBuild.size();
                }
                MediaSessionRecord.this.mMetadata = mediaMetadataBuild;
            }
            MediaSessionRecord.this.mHandler.post(1);
        }

        public void setPlaybackState(PlaybackState playbackState) {
            int state;
            if (MediaSessionRecord.this.mPlaybackState != null) {
                state = MediaSessionRecord.this.mPlaybackState.getState();
            } else {
                state = 0;
            }
            int state2 = playbackState != null ? playbackState.getState() : 0;
            if (MediaSessionRecord.DEBUG) {
                Log.d(MediaSessionRecord.TAG, "Playback state changed from: " + state + " to:" + state2 + "from Source:" + ("setPlaybackState from u/pid:" + Binder.getCallingUid() + SliceClientPermissions.SliceAuthority.DELIMITER + Binder.getCallingPid()));
            }
            synchronized (MediaSessionRecord.this.mLock) {
                MediaSessionRecord.this.mPlaybackState = playbackState;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                MediaSessionRecord.this.mService.onSessionPlaystateChanged(MediaSessionRecord.this, state, state2);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                MediaSessionRecord.this.mHandler.post(2);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        }

        public void setQueue(ParceledListSlice parceledListSlice) {
            synchronized (MediaSessionRecord.this.mLock) {
                MediaSessionRecord.this.mQueue = parceledListSlice;
            }
            MediaSessionRecord.this.mHandler.post(3);
        }

        public void setQueueTitle(CharSequence charSequence) {
            MediaSessionRecord.this.mQueueTitle = charSequence;
            MediaSessionRecord.this.mHandler.post(4);
        }

        public void setExtras(Bundle bundle) {
            synchronized (MediaSessionRecord.this.mLock) {
                MediaSessionRecord.this.mExtras = bundle == null ? null : new Bundle(bundle);
            }
            MediaSessionRecord.this.mHandler.post(5);
        }

        public void setRatingType(int i) {
            MediaSessionRecord.this.mRatingType = i;
        }

        public void setCurrentVolume(int i) {
            MediaSessionRecord.this.mCurrentVolume = i;
            MediaSessionRecord.this.mHandler.post(8);
        }

        public void setPlaybackToLocal(AudioAttributes audioAttributes) {
            boolean z;
            synchronized (MediaSessionRecord.this.mLock) {
                if (MediaSessionRecord.this.mVolumeType != 2) {
                    z = false;
                } else {
                    z = true;
                }
                MediaSessionRecord.this.mVolumeType = 1;
                if (audioAttributes != null) {
                    MediaSessionRecord.this.mAudioAttrs = audioAttributes;
                } else {
                    Log.e(MediaSessionRecord.TAG, "Received null audio attributes, using existing attributes");
                }
            }
            if (z) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    MediaSessionRecord.this.mService.onSessionPlaybackTypeChanged(MediaSessionRecord.this);
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    MediaSessionRecord.this.mHandler.post(8);
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
            }
        }

        public void setPlaybackToRemote(int i, int i2) {
            boolean z;
            synchronized (MediaSessionRecord.this.mLock) {
                z = true;
                if (MediaSessionRecord.this.mVolumeType != 1) {
                    z = false;
                }
                MediaSessionRecord.this.mVolumeType = 2;
                MediaSessionRecord.this.mVolumeControlType = i;
                MediaSessionRecord.this.mMaxVolume = i2;
            }
            if (z) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    MediaSessionRecord.this.mService.onSessionPlaybackTypeChanged(MediaSessionRecord.this);
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    MediaSessionRecord.this.mHandler.post(8);
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
            }
        }
    }

    class SessionCb {
        private final ISessionCallback mCb;

        public SessionCb(ISessionCallback iSessionCallback) {
            this.mCb = iSessionCallback;
        }

        public boolean sendMediaButton(String str, int i, int i2, boolean z, KeyEvent keyEvent, int i3, ResultReceiver resultReceiver) {
            try {
                if (z) {
                    this.mCb.onMediaButton(MediaSessionRecord.this.mContext.getPackageName(), Process.myPid(), 1000, createMediaButtonIntent(keyEvent), i3, resultReceiver);
                    return true;
                }
                this.mCb.onMediaButton(str, i, i2, createMediaButtonIntent(keyEvent), i3, resultReceiver);
                return true;
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in sendMediaRequest.", e);
                return false;
            }
        }

        public boolean sendMediaButton(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, boolean z, KeyEvent keyEvent) {
            try {
                if (z) {
                    this.mCb.onMediaButton(MediaSessionRecord.this.mContext.getPackageName(), Process.myPid(), 1000, createMediaButtonIntent(keyEvent), 0, (ResultReceiver) null);
                    return true;
                }
                this.mCb.onMediaButtonFromController(str, i, i2, iSessionControllerCallback, createMediaButtonIntent(keyEvent));
                return true;
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in sendMediaRequest.", e);
                return false;
            }
        }

        public void sendCommand(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle, ResultReceiver resultReceiver) {
            try {
                this.mCb.onCommand(str, i, i2, iSessionControllerCallback, str2, bundle, resultReceiver);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in sendCommand.", e);
            }
        }

        public void sendCustomAction(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) {
            try {
                this.mCb.onCustomAction(str, i, i2, iSessionControllerCallback, str2, bundle);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in sendCustomAction.", e);
            }
        }

        public void prepare(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) {
            try {
                this.mCb.onPrepare(str, i, i2, iSessionControllerCallback);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in prepare.", e);
            }
        }

        public void prepareFromMediaId(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) {
            try {
                this.mCb.onPrepareFromMediaId(str, i, i2, iSessionControllerCallback, str2, bundle);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in prepareFromMediaId.", e);
            }
        }

        public void prepareFromSearch(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) {
            try {
                this.mCb.onPrepareFromSearch(str, i, i2, iSessionControllerCallback, str2, bundle);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in prepareFromSearch.", e);
            }
        }

        public void prepareFromUri(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, Uri uri, Bundle bundle) {
            try {
                this.mCb.onPrepareFromUri(str, i, i2, iSessionControllerCallback, uri, bundle);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in prepareFromUri.", e);
            }
        }

        public void play(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) {
            try {
                this.mCb.onPlay(str, i, i2, iSessionControllerCallback);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in play.", e);
            }
        }

        public void playFromMediaId(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) {
            try {
                this.mCb.onPlayFromMediaId(str, i, i2, iSessionControllerCallback, str2, bundle);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in playFromMediaId.", e);
            }
        }

        public void playFromSearch(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) {
            try {
                this.mCb.onPlayFromSearch(str, i, i2, iSessionControllerCallback, str2, bundle);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in playFromSearch.", e);
            }
        }

        public void playFromUri(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, Uri uri, Bundle bundle) {
            try {
                this.mCb.onPlayFromUri(str, i, i2, iSessionControllerCallback, uri, bundle);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in playFromUri.", e);
            }
        }

        public void skipToTrack(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, long j) {
            try {
                this.mCb.onSkipToTrack(str, i, i2, iSessionControllerCallback, j);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in skipToTrack", e);
            }
        }

        public void pause(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) {
            try {
                this.mCb.onPause(str, i, i2, iSessionControllerCallback);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in pause.", e);
            }
        }

        public void stop(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) {
            try {
                this.mCb.onStop(str, i, i2, iSessionControllerCallback);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in stop.", e);
            }
        }

        public void next(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) {
            try {
                this.mCb.onNext(str, i, i2, iSessionControllerCallback);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in next.", e);
            }
        }

        public void previous(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) {
            try {
                this.mCb.onPrevious(str, i, i2, iSessionControllerCallback);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in previous.", e);
            }
        }

        public void fastForward(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) {
            try {
                this.mCb.onFastForward(str, i, i2, iSessionControllerCallback);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in fastForward.", e);
            }
        }

        public void rewind(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) {
            try {
                this.mCb.onRewind(str, i, i2, iSessionControllerCallback);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in rewind.", e);
            }
        }

        public void seekTo(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, long j) {
            try {
                this.mCb.onSeekTo(str, i, i2, iSessionControllerCallback, j);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in seekTo.", e);
            }
        }

        public void rate(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, Rating rating) {
            try {
                this.mCb.onRate(str, i, i2, iSessionControllerCallback, rating);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in rate.", e);
            }
        }

        public void adjustVolume(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, boolean z, int i3) {
            try {
                if (z) {
                    this.mCb.onAdjustVolume(MediaSessionRecord.this.mContext.getPackageName(), Process.myPid(), 1000, (ISessionControllerCallback) null, i3);
                } else {
                    this.mCb.onAdjustVolume(str, i, i2, iSessionControllerCallback, i3);
                }
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in adjustVolume.", e);
            }
        }

        public void setVolumeTo(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, int i3) {
            try {
                this.mCb.onSetVolumeTo(str, i, i2, iSessionControllerCallback, i3);
            } catch (RemoteException e) {
                Slog.e(MediaSessionRecord.TAG, "Remote failure in setVolumeTo.", e);
            }
        }

        private Intent createMediaButtonIntent(KeyEvent keyEvent) {
            Intent intent = new Intent("android.intent.action.MEDIA_BUTTON");
            intent.putExtra("android.intent.extra.KEY_EVENT", keyEvent);
            return intent;
        }
    }

    class ControllerStub extends ISessionController.Stub {
        ControllerStub() {
        }

        public void sendCommand(String str, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle, ResultReceiver resultReceiver) {
            MediaSessionRecord.this.mSessionCb.sendCommand(str, Binder.getCallingPid(), Binder.getCallingUid(), iSessionControllerCallback, str2, bundle, resultReceiver);
        }

        public boolean sendMediaButton(String str, ISessionControllerCallback iSessionControllerCallback, boolean z, KeyEvent keyEvent) {
            return MediaSessionRecord.this.mSessionCb.sendMediaButton(str, Binder.getCallingPid(), Binder.getCallingUid(), iSessionControllerCallback, z, keyEvent);
        }

        public void registerCallbackListener(String str, ISessionControllerCallback iSessionControllerCallback) {
            synchronized (MediaSessionRecord.this.mLock) {
                if (!MediaSessionRecord.this.mDestroyed) {
                    if (MediaSessionRecord.this.getControllerHolderIndexForCb(iSessionControllerCallback) < 0) {
                        MediaSessionRecord.this.mControllerCallbackHolders.add(MediaSessionRecord.this.new ISessionControllerCallbackHolder(iSessionControllerCallback, str, Binder.getCallingUid()));
                        if (MediaSessionRecord.DEBUG) {
                            Log.d(MediaSessionRecord.TAG, "registering controller callback " + iSessionControllerCallback + " from controller" + str);
                        }
                    }
                    return;
                }
                try {
                    iSessionControllerCallback.onSessionDestroyed();
                } catch (Exception e) {
                }
            }
        }

        public void unregisterCallbackListener(ISessionControllerCallback iSessionControllerCallback) {
            synchronized (MediaSessionRecord.this.mLock) {
                int controllerHolderIndexForCb = MediaSessionRecord.this.getControllerHolderIndexForCb(iSessionControllerCallback);
                if (controllerHolderIndexForCb != -1) {
                    MediaSessionRecord.this.mControllerCallbackHolders.remove(controllerHolderIndexForCb);
                }
                if (MediaSessionRecord.DEBUG) {
                    Log.d(MediaSessionRecord.TAG, "unregistering callback " + iSessionControllerCallback.asBinder());
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
                    return new ParcelableVolumeInfo(MediaSessionRecord.this.mVolumeType, MediaSessionRecord.this.mAudioAttrs, MediaSessionRecord.this.mVolumeControlType, MediaSessionRecord.this.mMaxVolume, MediaSessionRecord.this.mOptimisticVolume != -1 ? MediaSessionRecord.this.mOptimisticVolume : MediaSessionRecord.this.mCurrentVolume);
                }
                int i = MediaSessionRecord.this.mVolumeType;
                AudioAttributes audioAttributes = MediaSessionRecord.this.mAudioAttrs;
                int legacyStreamType = AudioAttributes.toLegacyStreamType(audioAttributes);
                return new ParcelableVolumeInfo(i, audioAttributes, 2, MediaSessionRecord.this.mAudioManager.getStreamMaxVolume(legacyStreamType), MediaSessionRecord.this.mAudioManager.getStreamVolume(legacyStreamType));
            }
        }

        public void adjustVolume(String str, ISessionControllerCallback iSessionControllerCallback, boolean z, int i, int i2) {
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                MediaSessionRecord.this.adjustVolume(str, callingPid, callingUid, iSessionControllerCallback, z, i, i2, false);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setVolumeTo(String str, ISessionControllerCallback iSessionControllerCallback, int i, int i2) {
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                MediaSessionRecord.this.setVolumeTo(str, callingPid, callingUid, iSessionControllerCallback, i, i2);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void prepare(String str, ISessionControllerCallback iSessionControllerCallback) {
            MediaSessionRecord.this.mSessionCb.prepare(str, Binder.getCallingPid(), Binder.getCallingUid(), iSessionControllerCallback);
        }

        public void prepareFromMediaId(String str, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) {
            MediaSessionRecord.this.mSessionCb.prepareFromMediaId(str, Binder.getCallingPid(), Binder.getCallingUid(), iSessionControllerCallback, str2, bundle);
        }

        public void prepareFromSearch(String str, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) {
            MediaSessionRecord.this.mSessionCb.prepareFromSearch(str, Binder.getCallingPid(), Binder.getCallingUid(), iSessionControllerCallback, str2, bundle);
        }

        public void prepareFromUri(String str, ISessionControllerCallback iSessionControllerCallback, Uri uri, Bundle bundle) {
            MediaSessionRecord.this.mSessionCb.prepareFromUri(str, Binder.getCallingPid(), Binder.getCallingUid(), iSessionControllerCallback, uri, bundle);
        }

        public void play(String str, ISessionControllerCallback iSessionControllerCallback) {
            MediaSessionRecord.this.mSessionCb.play(str, Binder.getCallingPid(), Binder.getCallingUid(), iSessionControllerCallback);
        }

        public void playFromMediaId(String str, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) {
            MediaSessionRecord.this.mSessionCb.playFromMediaId(str, Binder.getCallingPid(), Binder.getCallingUid(), iSessionControllerCallback, str2, bundle);
        }

        public void playFromSearch(String str, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) {
            MediaSessionRecord.this.mSessionCb.playFromSearch(str, Binder.getCallingPid(), Binder.getCallingUid(), iSessionControllerCallback, str2, bundle);
        }

        public void playFromUri(String str, ISessionControllerCallback iSessionControllerCallback, Uri uri, Bundle bundle) {
            MediaSessionRecord.this.mSessionCb.playFromUri(str, Binder.getCallingPid(), Binder.getCallingUid(), iSessionControllerCallback, uri, bundle);
        }

        public void skipToQueueItem(String str, ISessionControllerCallback iSessionControllerCallback, long j) {
            MediaSessionRecord.this.mSessionCb.skipToTrack(str, Binder.getCallingPid(), Binder.getCallingUid(), iSessionControllerCallback, j);
        }

        public void pause(String str, ISessionControllerCallback iSessionControllerCallback) {
            MediaSessionRecord.this.mSessionCb.pause(str, Binder.getCallingPid(), Binder.getCallingUid(), iSessionControllerCallback);
        }

        public void stop(String str, ISessionControllerCallback iSessionControllerCallback) {
            MediaSessionRecord.this.mSessionCb.stop(str, Binder.getCallingPid(), Binder.getCallingUid(), iSessionControllerCallback);
        }

        public void next(String str, ISessionControllerCallback iSessionControllerCallback) {
            MediaSessionRecord.this.mSessionCb.next(str, Binder.getCallingPid(), Binder.getCallingUid(), iSessionControllerCallback);
        }

        public void previous(String str, ISessionControllerCallback iSessionControllerCallback) {
            MediaSessionRecord.this.mSessionCb.previous(str, Binder.getCallingPid(), Binder.getCallingUid(), iSessionControllerCallback);
        }

        public void fastForward(String str, ISessionControllerCallback iSessionControllerCallback) {
            MediaSessionRecord.this.mSessionCb.fastForward(str, Binder.getCallingPid(), Binder.getCallingUid(), iSessionControllerCallback);
        }

        public void rewind(String str, ISessionControllerCallback iSessionControllerCallback) {
            MediaSessionRecord.this.mSessionCb.rewind(str, Binder.getCallingPid(), Binder.getCallingUid(), iSessionControllerCallback);
        }

        public void seekTo(String str, ISessionControllerCallback iSessionControllerCallback, long j) {
            MediaSessionRecord.this.mSessionCb.seekTo(str, Binder.getCallingPid(), Binder.getCallingUid(), iSessionControllerCallback, j);
        }

        public void rate(String str, ISessionControllerCallback iSessionControllerCallback, Rating rating) {
            MediaSessionRecord.this.mSessionCb.rate(str, Binder.getCallingPid(), Binder.getCallingUid(), iSessionControllerCallback, rating);
        }

        public void sendCustomAction(String str, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) {
            MediaSessionRecord.this.mSessionCb.sendCustomAction(str, Binder.getCallingPid(), Binder.getCallingUid(), iSessionControllerCallback, str2, bundle);
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

    private class ISessionControllerCallbackHolder {
        private final ISessionControllerCallback mCallback;
        private final String mPackageName;
        private final int mUid;

        ISessionControllerCallbackHolder(ISessionControllerCallback iSessionControllerCallback, String str, int i) {
            this.mCallback = iSessionControllerCallback;
            this.mPackageName = str;
            this.mUid = i;
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
        public void handleMessage(Message message) {
            switch (message.what) {
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
                    MediaSessionRecord.this.pushEvent((String) message.obj, message.getData());
                    break;
                case 8:
                    MediaSessionRecord.this.pushVolumeUpdate();
                    break;
                case 9:
                    MediaSessionRecord.this.pushSessionDestroyed();
                    break;
            }
        }

        public void post(int i) {
            post(i, null);
        }

        public void post(int i, Object obj) {
            obtainMessage(i, obj).sendToTarget();
        }

        public void post(int i, Object obj, Bundle bundle) {
            Message messageObtainMessage = obtainMessage(i, obj);
            messageObtainMessage.setData(bundle);
            messageObtainMessage.sendToTarget();
        }
    }
}
