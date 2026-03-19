package android.media;

import android.app.ActivityThread;
import android.content.Context;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.app.IAppOpsCallback;
import com.android.internal.app.IAppOpsService;

public abstract class PlayerBase {
    private final IAppOpsService mAppOps;
    private final IAppOpsCallback mAppOpsCallback;
    protected AudioAttributes mAttributes;
    private boolean mHasAppOpsPlayAudio;
    protected float mLeftVolume = 1.0f;
    protected float mRightVolume = 1.0f;
    protected float mAuxEffectSendLevel = 0.0f;
    private final Object mAppOpsLock = new Object();

    abstract int playerSetAuxEffectSendLevel(float f);

    abstract void playerSetVolume(float f, float f2);

    PlayerBase(AudioAttributes attr) {
        this.mHasAppOpsPlayAudio = true;
        if (attr == null) {
            throw new IllegalArgumentException("Illegal null AudioAttributes");
        }
        this.mAttributes = attr;
        IBinder b = ServiceManager.getService(Context.APP_OPS_SERVICE);
        this.mAppOps = IAppOpsService.Stub.asInterface(b);
        updateAppOpsPlayAudio_sync();
        this.mAppOpsCallback = new IAppOpsCallback.Stub() {
            public void opChanged(int op, int uid, String packageName) {
                synchronized (PlayerBase.this.mAppOpsLock) {
                    if (op == 28) {
                        PlayerBase.this.updateAppOpsPlayAudio_sync();
                    }
                }
            }
        };
        try {
            this.mAppOps.startWatchingMode(28, ActivityThread.currentPackageName(), this.mAppOpsCallback);
        } catch (RemoteException e) {
            this.mHasAppOpsPlayAudio = false;
        }
    }

    void baseUpdateAudioAttributes(AudioAttributes attr) {
        if (attr == null) {
            throw new IllegalArgumentException("Illegal null AudioAttributes");
        }
        synchronized (this.mAppOpsLock) {
            this.mAttributes = attr;
            updateAppOpsPlayAudio_sync();
        }
    }

    void baseStart() {
        synchronized (this.mAppOpsLock) {
            if (isRestricted_sync()) {
                playerSetVolume(0.0f, 0.0f);
            }
        }
    }

    void baseSetVolume(float leftVolume, float rightVolume) {
        synchronized (this.mAppOpsLock) {
            this.mLeftVolume = leftVolume;
            this.mRightVolume = rightVolume;
            if (isRestricted_sync()) {
                return;
            }
            playerSetVolume(leftVolume, rightVolume);
        }
    }

    int baseSetAuxEffectSendLevel(float level) {
        synchronized (this.mAppOpsLock) {
            this.mAuxEffectSendLevel = level;
            if (isRestricted_sync()) {
                return 0;
            }
            return playerSetAuxEffectSendLevel(level);
        }
    }

    void baseRelease() {
        try {
            this.mAppOps.stopWatchingMode(this.mAppOpsCallback);
        } catch (RemoteException e) {
        }
    }

    void updateAppOpsPlayAudio_sync() {
        boolean oldHasAppOpsPlayAudio = this.mHasAppOpsPlayAudio;
        try {
            int mode = this.mAppOps.checkAudioOperation(28, this.mAttributes.getUsage(), Process.myUid(), ActivityThread.currentPackageName());
            this.mHasAppOpsPlayAudio = mode == 0;
        } catch (RemoteException e) {
            this.mHasAppOpsPlayAudio = false;
        }
        try {
            if (oldHasAppOpsPlayAudio == this.mHasAppOpsPlayAudio) {
                return;
            }
            if (this.mHasAppOpsPlayAudio) {
                playerSetVolume(this.mLeftVolume, this.mRightVolume);
                playerSetAuxEffectSendLevel(this.mAuxEffectSendLevel);
            } else {
                playerSetVolume(0.0f, 0.0f);
                playerSetAuxEffectSendLevel(0.0f);
            }
        } catch (Exception e2) {
        }
    }

    boolean isRestricted_sync() {
        return (this.mAttributes.getAllFlags() & 64) == 0 && !this.mHasAppOpsPlayAudio;
    }
}
