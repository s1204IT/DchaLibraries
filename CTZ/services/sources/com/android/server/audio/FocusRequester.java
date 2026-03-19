package com.android.server.audio;

import android.media.AudioAttributes;
import android.media.AudioFocusInfo;
import android.media.IAudioFocusDispatcher;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.server.audio.MediaFocusControl;
import java.io.PrintWriter;
import java.util.NoSuchElementException;

public class FocusRequester {
    private static final boolean DEBUG;
    private static final String TAG = "MediaFocusControl";
    private final AudioAttributes mAttributes;
    private final int mCallingUid;
    private final String mClientId;
    private MediaFocusControl.AudioFocusDeathHandler mDeathHandler;
    private final MediaFocusControl mFocusController;
    private IAudioFocusDispatcher mFocusDispatcher;
    private final int mFocusGainRequest;
    private int mFocusLossReceived = 0;
    private boolean mFocusLossWasNotified = true;
    private final int mGrantFlags;
    private final String mPackageName;
    private final int mSdkTarget;
    private final IBinder mSourceRef;

    static {
        DEBUG = Log.isLoggable(TAG, 3) || !"user".equals(Build.TYPE);
    }

    FocusRequester(AudioAttributes audioAttributes, int i, int i2, IAudioFocusDispatcher iAudioFocusDispatcher, IBinder iBinder, String str, MediaFocusControl.AudioFocusDeathHandler audioFocusDeathHandler, String str2, int i3, MediaFocusControl mediaFocusControl, int i4) {
        this.mAttributes = audioAttributes;
        this.mFocusDispatcher = iAudioFocusDispatcher;
        this.mSourceRef = iBinder;
        this.mClientId = str;
        this.mDeathHandler = audioFocusDeathHandler;
        this.mPackageName = str2;
        this.mCallingUid = i3;
        this.mFocusGainRequest = i;
        this.mGrantFlags = i2;
        this.mFocusController = mediaFocusControl;
        this.mSdkTarget = i4;
    }

    FocusRequester(AudioFocusInfo audioFocusInfo, IAudioFocusDispatcher iAudioFocusDispatcher, IBinder iBinder, MediaFocusControl.AudioFocusDeathHandler audioFocusDeathHandler, MediaFocusControl mediaFocusControl) {
        this.mAttributes = audioFocusInfo.getAttributes();
        this.mClientId = audioFocusInfo.getClientId();
        this.mPackageName = audioFocusInfo.getPackageName();
        this.mCallingUid = audioFocusInfo.getClientUid();
        this.mFocusGainRequest = audioFocusInfo.getGainRequest();
        this.mGrantFlags = audioFocusInfo.getFlags();
        this.mSdkTarget = audioFocusInfo.getSdkTarget();
        this.mFocusDispatcher = iAudioFocusDispatcher;
        this.mSourceRef = iBinder;
        this.mDeathHandler = audioFocusDeathHandler;
        this.mFocusController = mediaFocusControl;
    }

    boolean hasSameClient(String str) {
        try {
            return this.mClientId.compareTo(str) == 0;
        } catch (NullPointerException e) {
            return false;
        }
    }

    boolean isLockedFocusOwner() {
        return (this.mGrantFlags & 4) != 0;
    }

    boolean hasSameBinder(IBinder iBinder) {
        return this.mSourceRef != null && this.mSourceRef.equals(iBinder);
    }

    boolean hasSameDispatcher(IAudioFocusDispatcher iAudioFocusDispatcher) {
        return this.mFocusDispatcher != null && this.mFocusDispatcher.equals(iAudioFocusDispatcher);
    }

    boolean hasSamePackage(String str) {
        try {
            return this.mPackageName.compareTo(str) == 0;
        } catch (NullPointerException e) {
            return false;
        }
    }

    boolean hasSameUid(int i) {
        return this.mCallingUid == i;
    }

    int getClientUid() {
        return this.mCallingUid;
    }

    String getClientId() {
        return this.mClientId;
    }

    int getGainRequest() {
        return this.mFocusGainRequest;
    }

    int getGrantFlags() {
        return this.mGrantFlags;
    }

    AudioAttributes getAudioAttributes() {
        return this.mAttributes;
    }

    int getSdkTarget() {
        return this.mSdkTarget;
    }

    private static String focusChangeToString(int i) {
        switch (i) {
            case -3:
                return "LOSS_TRANSIENT_CAN_DUCK";
            case -2:
                return "LOSS_TRANSIENT";
            case -1:
                return "LOSS";
            case 0:
                return "none";
            case 1:
                return "GAIN";
            case 2:
                return "GAIN_TRANSIENT";
            case 3:
                return "GAIN_TRANSIENT_MAY_DUCK";
            case 4:
                return "GAIN_TRANSIENT_EXCLUSIVE";
            default:
                return "[invalid focus change" + i + "]";
        }
    }

    private String focusGainToString() {
        return focusChangeToString(this.mFocusGainRequest);
    }

    private String focusLossToString() {
        return focusChangeToString(this.mFocusLossReceived);
    }

    private static String flagsToString(int i) {
        String str = new String();
        if ((i & 1) != 0) {
            str = str + "DELAY_OK";
        }
        if ((i & 4) != 0) {
            if (!str.isEmpty()) {
                str = str + "|";
            }
            str = str + "LOCK";
        }
        if ((i & 2) != 0) {
            if (!str.isEmpty()) {
                str = str + "|";
            }
            return str + "PAUSES_ON_DUCKABLE_LOSS";
        }
        return str;
    }

    void dump(PrintWriter printWriter) {
        printWriter.println("  source:" + this.mSourceRef + " -- pack: " + this.mPackageName + " -- client: " + this.mClientId + " -- gain: " + focusGainToString() + " -- flags: " + flagsToString(this.mGrantFlags) + " -- loss: " + focusLossToString() + " -- notified: " + this.mFocusLossWasNotified + " -- uid: " + this.mCallingUid + " -- attr: " + this.mAttributes + " -- sdk:" + this.mSdkTarget);
    }

    void release() {
        IBinder iBinder = this.mSourceRef;
        MediaFocusControl.AudioFocusDeathHandler audioFocusDeathHandler = this.mDeathHandler;
        if (iBinder != null && audioFocusDeathHandler != null) {
            try {
                iBinder.unlinkToDeath(audioFocusDeathHandler, 0);
            } catch (NoSuchElementException e) {
            }
        }
        this.mDeathHandler = null;
        this.mFocusDispatcher = null;
    }

    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    private int focusLossForGainRequest(int r4) {
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.audio.FocusRequester.focusLossForGainRequest(int):int");
    }

    @GuardedBy("MediaFocusControl.mAudioFocusLock")
    boolean handleFocusLossFromGain(int i, FocusRequester focusRequester, boolean z) {
        int iFocusLossForGainRequest = focusLossForGainRequest(i);
        handleFocusLoss(iFocusLossForGainRequest, focusRequester, z);
        return iFocusLossForGainRequest == -1;
    }

    @GuardedBy("MediaFocusControl.mAudioFocusLock")
    void handleFocusGain(int i) {
        try {
            this.mFocusLossReceived = 0;
            this.mFocusController.notifyExtPolicyFocusGrant_syncAf(toAudioFocusInfo(), 1);
            IAudioFocusDispatcher iAudioFocusDispatcher = this.mFocusDispatcher;
            if (iAudioFocusDispatcher != null) {
                if (DEBUG) {
                    Log.v(TAG, "dispatching " + focusChangeToString(i) + " to " + this.mClientId);
                }
                if (this.mFocusLossWasNotified) {
                    iAudioFocusDispatcher.dispatchAudioFocusChange(i, this.mClientId);
                }
            }
            this.mFocusController.unduckPlayers(this);
        } catch (RemoteException e) {
            Log.e(TAG, "Failure to signal gain of audio focus due to: ", e);
        }
    }

    @GuardedBy("MediaFocusControl.mAudioFocusLock")
    void handleFocusGainFromRequest(int i) {
        if (i == 1) {
            this.mFocusController.unduckPlayers(this);
        }
    }

    @GuardedBy("MediaFocusControl.mAudioFocusLock")
    void handleFocusLoss(int i, FocusRequester focusRequester, boolean z) {
        boolean zDuckPlayers;
        try {
            if (i != this.mFocusLossReceived) {
                this.mFocusLossReceived = i;
                this.mFocusLossWasNotified = false;
                if (!this.mFocusController.mustNotifyFocusOwnerOnDuck() && this.mFocusLossReceived == -3 && (this.mGrantFlags & 2) == 0) {
                    if (DEBUG) {
                        Log.v(TAG, "NOT dispatching " + focusChangeToString(this.mFocusLossReceived) + " to " + this.mClientId + ", to be handled externally");
                    }
                    this.mFocusController.notifyExtPolicyFocusLoss_syncAf(toAudioFocusInfo(), false);
                    return;
                }
                if (i == -3 && focusRequester != null && focusRequester.mCallingUid != this.mCallingUid) {
                    if (!z && (this.mGrantFlags & 2) != 0) {
                        Log.v(TAG, "not ducking uid " + this.mCallingUid + " - flags");
                    } else if (!z && getSdkTarget() <= 25) {
                        Log.v(TAG, "not ducking uid " + this.mCallingUid + " - old SDK");
                    } else {
                        zDuckPlayers = this.mFocusController.duckPlayers(focusRequester, this, z);
                    }
                    zDuckPlayers = false;
                } else {
                    zDuckPlayers = false;
                }
                if (zDuckPlayers) {
                    if (DEBUG) {
                        Log.v(TAG, "NOT dispatching " + focusChangeToString(this.mFocusLossReceived) + " to " + this.mClientId + ", ducking implemented by framework");
                    }
                    this.mFocusController.notifyExtPolicyFocusLoss_syncAf(toAudioFocusInfo(), false);
                    return;
                }
                IAudioFocusDispatcher iAudioFocusDispatcher = this.mFocusDispatcher;
                if (iAudioFocusDispatcher != null) {
                    if (DEBUG) {
                        Log.v(TAG, "dispatching " + focusChangeToString(this.mFocusLossReceived) + " to " + this.mClientId);
                    }
                    this.mFocusController.notifyExtPolicyFocusLoss_syncAf(toAudioFocusInfo(), true);
                    this.mFocusLossWasNotified = true;
                    iAudioFocusDispatcher.dispatchAudioFocusChange(this.mFocusLossReceived, this.mClientId);
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failure to signal loss of audio focus due to:", e);
        }
    }

    int dispatchFocusChange(int i) {
        if (this.mFocusDispatcher == null) {
            if (MediaFocusControl.DEBUG) {
                Log.e(TAG, "dispatchFocusChange: no focus dispatcher");
            }
            return 0;
        }
        if (i == 0) {
            if (MediaFocusControl.DEBUG) {
                Log.v(TAG, "dispatchFocusChange: AUDIOFOCUS_NONE");
            }
            return 0;
        }
        if ((i == 3 || i == 4 || i == 2 || i == 1) && this.mFocusGainRequest != i) {
            Log.w(TAG, "focus gain was requested with " + this.mFocusGainRequest + ", dispatching " + i);
        } else if (i == -3 || i == -2 || i == -1) {
            this.mFocusLossReceived = i;
        }
        try {
            this.mFocusDispatcher.dispatchAudioFocusChange(i, this.mClientId);
            return 1;
        } catch (RemoteException e) {
            Log.e(TAG, "dispatchFocusChange: error talking to focus listener " + this.mClientId, e);
            return 0;
        }
    }

    void dispatchFocusResultFromExtPolicy(int i) {
        if (this.mFocusDispatcher == null && MediaFocusControl.DEBUG) {
            Log.e(TAG, "dispatchFocusResultFromExtPolicy: no focus dispatcher");
        }
        if (DEBUG) {
            Log.v(TAG, "dispatching result" + i + " to " + this.mClientId);
        }
        try {
            this.mFocusDispatcher.dispatchFocusResultFromExtPolicy(i, this.mClientId);
        } catch (RemoteException e) {
            Log.e(TAG, "dispatchFocusResultFromExtPolicy: error talking to focus listener" + this.mClientId, e);
        }
    }

    AudioFocusInfo toAudioFocusInfo() {
        return new AudioFocusInfo(this.mAttributes, this.mCallingUid, this.mClientId, this.mPackageName, this.mFocusGainRequest, this.mFocusLossReceived, this.mGrantFlags, this.mSdkTarget);
    }
}
