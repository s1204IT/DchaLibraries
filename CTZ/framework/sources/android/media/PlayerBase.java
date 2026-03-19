package android.media;

import android.app.ActivityThread;
import android.content.Context;
import android.media.IAudioService;
import android.media.IPlayer;
import android.media.VolumeShaper;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IAppOpsCallback;
import com.android.internal.app.IAppOpsService;
import java.lang.ref.WeakReference;
import java.util.Objects;

public abstract class PlayerBase {
    private static final boolean DEBUG;
    private static final boolean DEBUG_APP_OPS = !"user".equals(Build.TYPE);
    private static final String TAG = "PlayerBase";
    private static IAudioService sService;
    private IAppOpsService mAppOps;
    private IAppOpsCallback mAppOpsCallback;
    protected AudioAttributes mAttributes;
    private final int mImplType;

    @GuardedBy("mLock")
    private int mState;
    protected float mLeftVolume = 1.0f;
    protected float mRightVolume = 1.0f;
    protected float mAuxEffectSendLevel = 0.0f;
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private boolean mHasAppOpsPlayAudio = true;
    private int mPlayerIId = 0;

    @GuardedBy("mLock")
    private int mStartDelayMs = 0;

    @GuardedBy("mLock")
    private float mPanMultiplierL = 1.0f;

    @GuardedBy("mLock")
    private float mPanMultiplierR = 1.0f;

    abstract int playerApplyVolumeShaper(VolumeShaper.Configuration configuration, VolumeShaper.Operation operation);

    abstract VolumeShaper.State playerGetVolumeShaperState(int i);

    abstract void playerPause();

    abstract int playerSetAuxEffectSendLevel(boolean z, float f);

    abstract void playerSetVolume(boolean z, float f, float f2);

    abstract void playerStart();

    abstract void playerStop();

    static {
        DEBUG = DEBUG_APP_OPS;
    }

    PlayerBase(AudioAttributes audioAttributes, int i) {
        if (audioAttributes == null) {
            throw new IllegalArgumentException("Illegal null AudioAttributes");
        }
        this.mAttributes = audioAttributes;
        this.mImplType = i;
        this.mState = 1;
    }

    protected void baseRegisterPlayer() {
        int iTrackPlayer;
        this.mAppOps = IAppOpsService.Stub.asInterface(ServiceManager.getService(Context.APP_OPS_SERVICE));
        updateAppOpsPlayAudio();
        this.mAppOpsCallback = new IAppOpsCallbackWrapper(this);
        try {
            this.mAppOps.startWatchingMode(28, ActivityThread.currentPackageName(), this.mAppOpsCallback);
        } catch (RemoteException e) {
            Log.e(TAG, "Error registering appOps callback", e);
            this.mHasAppOpsPlayAudio = false;
        }
        try {
            iTrackPlayer = getService().trackPlayer(new PlayerIdCard(this.mImplType, this.mAttributes, new IPlayerWrapper(this)));
        } catch (RemoteException e2) {
            Log.e(TAG, "Error talking to audio service, player will not be tracked", e2);
            iTrackPlayer = -1;
        }
        this.mPlayerIId = iTrackPlayer;
    }

    void baseUpdateAudioAttributes(AudioAttributes audioAttributes) {
        if (audioAttributes == null) {
            throw new IllegalArgumentException("Illegal null AudioAttributes");
        }
        try {
            getService().playerAttributes(this.mPlayerIId, audioAttributes);
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to audio service, STARTED state will not be tracked", e);
        }
        synchronized (this.mLock) {
            boolean z = this.mAttributes != audioAttributes;
            this.mAttributes = audioAttributes;
            updateAppOpsPlayAudio_sync(z);
        }
    }

    private void updateState(int i) {
        int i2;
        synchronized (this.mLock) {
            this.mState = i;
            i2 = this.mPlayerIId;
        }
        try {
            getService().playerEvent(i2, i);
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to audio service, " + AudioPlaybackConfiguration.toLogFriendlyPlayerState(i) + " state will not be tracked for piid=" + i2, e);
        }
    }

    void baseStart() {
        if (DEBUG) {
            Log.v(TAG, "baseStart() piid=" + this.mPlayerIId);
        }
        updateState(2);
        synchronized (this.mLock) {
            if (isRestricted_sync()) {
                playerSetVolume(true, 0.0f, 0.0f);
            }
        }
    }

    void baseSetStartDelayMs(int i) {
        synchronized (this.mLock) {
            this.mStartDelayMs = Math.max(i, 0);
        }
    }

    protected int getStartDelayMs() {
        int i;
        synchronized (this.mLock) {
            i = this.mStartDelayMs;
        }
        return i;
    }

    void basePause() {
        if (DEBUG) {
            Log.v(TAG, "basePause() piid=" + this.mPlayerIId);
        }
        updateState(3);
    }

    void baseStop() {
        if (DEBUG) {
            Log.v(TAG, "baseStop() piid=" + this.mPlayerIId);
        }
        updateState(4);
    }

    void baseSetPan(float f) {
        float fMin = Math.min(Math.max(-1.0f, f), 1.0f);
        synchronized (this.mLock) {
            try {
                if (fMin >= 0.0f) {
                    this.mPanMultiplierL = 1.0f - fMin;
                    this.mPanMultiplierR = 1.0f;
                } else {
                    this.mPanMultiplierL = 1.0f;
                    this.mPanMultiplierR = 1.0f + fMin;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        baseSetVolume(this.mLeftVolume, this.mRightVolume);
    }

    void baseSetVolume(float f, float f2) {
        boolean zIsRestricted_sync;
        synchronized (this.mLock) {
            this.mLeftVolume = f;
            this.mRightVolume = f2;
            zIsRestricted_sync = isRestricted_sync();
        }
        playerSetVolume(zIsRestricted_sync, f * this.mPanMultiplierL, f2 * this.mPanMultiplierR);
    }

    int baseSetAuxEffectSendLevel(float f) {
        synchronized (this.mLock) {
            this.mAuxEffectSendLevel = f;
            if (isRestricted_sync()) {
                return 0;
            }
            return playerSetAuxEffectSendLevel(false, f);
        }
    }

    void baseRelease() {
        boolean z;
        if (DEBUG) {
            Log.v(TAG, "baseRelease() piid=" + this.mPlayerIId + " state=" + this.mState);
        }
        synchronized (this.mLock) {
            if (this.mState != 0) {
                z = true;
                this.mState = 0;
            } else {
                z = false;
            }
        }
        if (z) {
            try {
                getService().releasePlayer(this.mPlayerIId);
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to audio service, the player will still be tracked", e);
            }
        }
        try {
            if (this.mAppOps != null) {
                this.mAppOps.stopWatchingMode(this.mAppOpsCallback);
            }
        } catch (Exception e2) {
        }
    }

    private void updateAppOpsPlayAudio() {
        synchronized (this.mLock) {
            updateAppOpsPlayAudio_sync(false);
        }
    }

    void updateAppOpsPlayAudio_sync(boolean z) {
        int iCheckAudioOperation;
        boolean z2 = this.mHasAppOpsPlayAudio;
        try {
            if (this.mAppOps != null) {
                iCheckAudioOperation = this.mAppOps.checkAudioOperation(28, this.mAttributes.getUsage(), Process.myUid(), ActivityThread.currentPackageName());
            } else {
                iCheckAudioOperation = 1;
            }
            this.mHasAppOpsPlayAudio = iCheckAudioOperation == 0;
        } catch (RemoteException e) {
            this.mHasAppOpsPlayAudio = false;
        }
        try {
            if (z2 != this.mHasAppOpsPlayAudio || z) {
                getService().playerHasOpPlayAudio(this.mPlayerIId, this.mHasAppOpsPlayAudio);
                if (!isRestricted_sync()) {
                    if (DEBUG_APP_OPS) {
                        Log.v(TAG, "updateAppOpsPlayAudio: unmuting player, vol=" + this.mLeftVolume + "/" + this.mRightVolume);
                    }
                    playerSetVolume(false, this.mLeftVolume * this.mPanMultiplierL, this.mRightVolume * this.mPanMultiplierR);
                    playerSetAuxEffectSendLevel(false, this.mAuxEffectSendLevel);
                    return;
                }
                if (DEBUG_APP_OPS) {
                    Log.v(TAG, "updateAppOpsPlayAudio: muting player");
                }
                playerSetVolume(true, 0.0f, 0.0f);
                playerSetAuxEffectSendLevel(true, 0.0f);
            }
        } catch (Exception e2) {
        }
    }

    boolean isRestricted_sync() {
        boolean zIsCameraSoundForced;
        if (this.mHasAppOpsPlayAudio || (this.mAttributes.getAllFlags() & 64) != 0) {
            return false;
        }
        if ((this.mAttributes.getAllFlags() & 1) != 0 && this.mAttributes.getUsage() == 13) {
            try {
                zIsCameraSoundForced = getService().isCameraSoundForced();
            } catch (RemoteException e) {
                Log.e(TAG, "Cannot access AudioService in isRestricted_sync()");
                zIsCameraSoundForced = false;
            } catch (NullPointerException e2) {
                Log.e(TAG, "Null AudioService in isRestricted_sync()");
                zIsCameraSoundForced = false;
            }
            if (zIsCameraSoundForced) {
                return false;
            }
        }
        return true;
    }

    private static IAudioService getService() {
        if (sService != null) {
            return sService;
        }
        sService = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
        return sService;
    }

    public void setStartDelayMs(int i) {
        baseSetStartDelayMs(i);
    }

    private static class IAppOpsCallbackWrapper extends IAppOpsCallback.Stub {
        private final WeakReference<PlayerBase> mWeakPB;

        public IAppOpsCallbackWrapper(PlayerBase playerBase) {
            this.mWeakPB = new WeakReference<>(playerBase);
        }

        @Override
        public void opChanged(int i, int i2, String str) {
            if (i == 28) {
                if (PlayerBase.DEBUG_APP_OPS) {
                    Log.v(PlayerBase.TAG, "opChanged: op=PLAY_AUDIO pack=" + str);
                }
                PlayerBase playerBase = this.mWeakPB.get();
                if (playerBase != null) {
                    playerBase.updateAppOpsPlayAudio();
                }
            }
        }
    }

    private static class IPlayerWrapper extends IPlayer.Stub {
        private final WeakReference<PlayerBase> mWeakPB;

        public IPlayerWrapper(PlayerBase playerBase) {
            this.mWeakPB = new WeakReference<>(playerBase);
        }

        @Override
        public void start() {
            PlayerBase playerBase = this.mWeakPB.get();
            if (playerBase != null) {
                playerBase.playerStart();
            }
        }

        @Override
        public void pause() {
            PlayerBase playerBase = this.mWeakPB.get();
            if (playerBase != null) {
                playerBase.playerPause();
            }
        }

        @Override
        public void stop() {
            PlayerBase playerBase = this.mWeakPB.get();
            if (playerBase != null) {
                playerBase.playerStop();
            }
        }

        @Override
        public void setVolume(float f) {
            PlayerBase playerBase = this.mWeakPB.get();
            if (playerBase != null) {
                playerBase.baseSetVolume(f, f);
            }
        }

        @Override
        public void setPan(float f) {
            PlayerBase playerBase = this.mWeakPB.get();
            if (playerBase != null) {
                playerBase.baseSetPan(f);
            }
        }

        @Override
        public void setStartDelayMs(int i) {
            PlayerBase playerBase = this.mWeakPB.get();
            if (playerBase != null) {
                playerBase.baseSetStartDelayMs(i);
            }
        }

        @Override
        public void applyVolumeShaper(VolumeShaper.Configuration configuration, VolumeShaper.Operation operation) {
            PlayerBase playerBase = this.mWeakPB.get();
            if (playerBase != null) {
                playerBase.playerApplyVolumeShaper(configuration, operation);
            }
        }
    }

    public static class PlayerIdCard implements Parcelable {
        public static final int AUDIO_ATTRIBUTES_DEFINED = 1;
        public static final int AUDIO_ATTRIBUTES_NONE = 0;
        public static final Parcelable.Creator<PlayerIdCard> CREATOR = new Parcelable.Creator<PlayerIdCard>() {
            @Override
            public PlayerIdCard createFromParcel(Parcel parcel) {
                return new PlayerIdCard(parcel);
            }

            @Override
            public PlayerIdCard[] newArray(int i) {
                return new PlayerIdCard[i];
            }
        };
        public final AudioAttributes mAttributes;
        public final IPlayer mIPlayer;
        public final int mPlayerType;

        PlayerIdCard(int i, AudioAttributes audioAttributes, IPlayer iPlayer) {
            this.mPlayerType = i;
            this.mAttributes = audioAttributes;
            this.mIPlayer = iPlayer;
        }

        public int hashCode() {
            return Objects.hash(Integer.valueOf(this.mPlayerType));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.mPlayerType);
            this.mAttributes.writeToParcel(parcel, 0);
            parcel.writeStrongBinder(this.mIPlayer == null ? null : this.mIPlayer.asBinder());
        }

        private PlayerIdCard(Parcel parcel) {
            this.mPlayerType = parcel.readInt();
            this.mAttributes = AudioAttributes.CREATOR.createFromParcel(parcel);
            IBinder strongBinder = parcel.readStrongBinder();
            this.mIPlayer = strongBinder == null ? null : IPlayer.Stub.asInterface(strongBinder);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || !(obj instanceof PlayerIdCard)) {
                return false;
            }
            PlayerIdCard playerIdCard = (PlayerIdCard) obj;
            if (this.mPlayerType == playerIdCard.mPlayerType && this.mAttributes.equals(playerIdCard.mAttributes)) {
                return true;
            }
            return false;
        }
    }

    public static void deprecateStreamTypeForPlayback(int i, String str, String str2) throws IllegalArgumentException {
        if (i == 10) {
            throw new IllegalArgumentException("Use of STREAM_ACCESSIBILITY is reserved for volume control");
        }
        Log.w(str, "Use of stream types is deprecated for operations other than volume control");
        Log.w(str, "See the documentation of " + str2 + " for what to use instead with android.media.AudioAttributes to qualify your playback use case");
    }
}
