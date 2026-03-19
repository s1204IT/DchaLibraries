package com.android.server.audio;

import android.app.AppOpsManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusInfo;
import android.media.IAudioFocusDispatcher;
import android.media.audiopolicy.IAudioPolicyCallback;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.server.SystemService;
import com.android.server.audio.AudioEventLogger;
import com.android.server.slice.SliceClientPermissions;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;

public class MediaFocusControl implements PlayerFocusEnforcer {
    static final boolean DEBUG;
    static final int DUCKING_IN_APP_SDK_LEVEL = 25;
    static final boolean ENFORCE_DUCKING = true;
    static final boolean ENFORCE_DUCKING_FOR_NEW = true;
    static final boolean ENFORCE_MUTING_FOR_RING_OR_CALL = true;
    private static final int MAX_STACK_SIZE = 100;
    private static final int RING_CALL_MUTING_ENFORCEMENT_DELAY_MS = 100;
    private static final String TAG = "MediaFocusControl";
    private static final int[] USAGES_TO_MUTE_IN_RING_OR_CALL;
    private static final Object mAudioFocusLock;
    private static final AudioEventLogger mEventLogger;
    private final AppOpsManager mAppOps;
    private final Context mContext;

    @GuardedBy("mExtFocusChangeLock")
    private long mExtFocusChangeCounter;
    private PlayerFocusEnforcer mFocusEnforcer;
    private boolean mRingOrCallActive = false;
    private final Object mExtFocusChangeLock = new Object();
    private final Stack<FocusRequester> mFocusStack = new Stack<>();
    private boolean mNotifyFocusOwnerOnDuck = true;
    private ArrayList<IAudioPolicyCallback> mFocusFollowers = new ArrayList<>();
    private IAudioPolicyCallback mFocusPolicy = null;
    private HashMap<String, FocusRequester> mFocusOwnersForFocusPolicy = new HashMap<>();

    static {
        DEBUG = Log.isLoggable(TAG, 3) || !"user".equals(Build.TYPE);
        mAudioFocusLock = new Object();
        mEventLogger = new AudioEventLogger(50, "focus commands as seen by MediaFocusControl");
        USAGES_TO_MUTE_IN_RING_OR_CALL = new int[]{1, 14};
    }

    protected MediaFocusControl(Context context, PlayerFocusEnforcer playerFocusEnforcer) {
        this.mContext = context;
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mFocusEnforcer = playerFocusEnforcer;
    }

    protected void dump(PrintWriter printWriter) {
        printWriter.println("\nMediaFocusControl dump time: " + DateFormat.getTimeInstance().format(new Date()));
        dumpFocusStack(printWriter);
        printWriter.println("\n");
        mEventLogger.dump(printWriter);
    }

    @Override
    public boolean duckPlayers(FocusRequester focusRequester, FocusRequester focusRequester2, boolean z) {
        return this.mFocusEnforcer.duckPlayers(focusRequester, focusRequester2, z);
    }

    @Override
    public void unduckPlayers(FocusRequester focusRequester) {
        this.mFocusEnforcer.unduckPlayers(focusRequester);
    }

    @Override
    public void mutePlayersForCall(int[] iArr) {
        this.mFocusEnforcer.mutePlayersForCall(iArr);
    }

    @Override
    public void unmutePlayersForCall() {
        this.mFocusEnforcer.unmutePlayersForCall();
    }

    protected void discardAudioFocusOwner() {
        synchronized (mAudioFocusLock) {
            if (!this.mFocusStack.empty()) {
                FocusRequester focusRequesterPop = this.mFocusStack.pop();
                focusRequesterPop.handleFocusLoss(-1, null, false);
                focusRequesterPop.release();
            }
        }
    }

    @GuardedBy("mAudioFocusLock")
    private void notifyTopOfAudioFocusStack() {
        if (!this.mFocusStack.empty() && canReassignAudioFocus()) {
            this.mFocusStack.peek().handleFocusGain(1);
        }
    }

    @GuardedBy("mAudioFocusLock")
    private void propagateFocusLossFromGain_syncAf(int i, FocusRequester focusRequester, boolean z) {
        LinkedList linkedList = new LinkedList();
        for (FocusRequester focusRequester2 : this.mFocusStack) {
            if (focusRequester2.handleFocusLossFromGain(i, focusRequester, z)) {
                linkedList.add(focusRequester2.getClientId());
            }
        }
        Iterator it = linkedList.iterator();
        while (it.hasNext()) {
            removeFocusStackEntry((String) it.next(), false, true);
        }
    }

    private void dumpFocusStack(PrintWriter printWriter) {
        printWriter.println("\nAudio Focus stack entries (last is top of stack):");
        synchronized (mAudioFocusLock) {
            Iterator<FocusRequester> it = this.mFocusStack.iterator();
            while (it.hasNext()) {
                it.next().dump(printWriter);
            }
            printWriter.println("\n");
            if (this.mFocusPolicy == null) {
                printWriter.println("No external focus policy\n");
            } else {
                printWriter.println("External focus policy: " + this.mFocusPolicy + ", focus owners:\n");
                dumpExtFocusPolicyFocusOwners(printWriter);
            }
        }
        printWriter.println("\n");
        printWriter.println(" Notify on duck:  " + this.mNotifyFocusOwnerOnDuck + "\n");
        printWriter.println(" In ring or call: " + this.mRingOrCallActive + "\n");
    }

    @GuardedBy("mAudioFocusLock")
    private void removeFocusStackEntry(String str, boolean z, boolean z2) {
        if (!this.mFocusStack.empty() && this.mFocusStack.peek().hasSameClient(str)) {
            FocusRequester focusRequesterPop = this.mFocusStack.pop();
            focusRequesterPop.release();
            if (z2) {
                AudioFocusInfo audioFocusInfo = focusRequesterPop.toAudioFocusInfo();
                audioFocusInfo.clearLossReceived();
                notifyExtPolicyFocusLoss_syncAf(audioFocusInfo, false);
            }
            if (z) {
                notifyTopOfAudioFocusStack();
                return;
            }
            return;
        }
        Iterator<FocusRequester> it = this.mFocusStack.iterator();
        while (it.hasNext()) {
            FocusRequester next = it.next();
            if (next.hasSameClient(str)) {
                Log.i(TAG, "AudioFocus  removeFocusStackEntry(): removing entry for " + str);
                it.remove();
                next.release();
            }
        }
    }

    @GuardedBy("mAudioFocusLock")
    private void removeFocusStackEntryOnDeath(IBinder iBinder) {
        boolean z = !this.mFocusStack.isEmpty() && this.mFocusStack.peek().hasSameBinder(iBinder);
        Iterator<FocusRequester> it = this.mFocusStack.iterator();
        while (it.hasNext()) {
            FocusRequester next = it.next();
            if (next.hasSameBinder(iBinder)) {
                Log.i(TAG, "AudioFocus  removeFocusStackEntryOnDeath(): removing entry for " + iBinder);
                it.remove();
                next.release();
            }
        }
        if (z) {
            notifyTopOfAudioFocusStack();
        }
    }

    @GuardedBy("mAudioFocusLock")
    private void removeFocusEntryForExtPolicy(IBinder iBinder) {
        if (this.mFocusOwnersForFocusPolicy.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, FocusRequester>> it = this.mFocusOwnersForFocusPolicy.entrySet().iterator();
        while (it.hasNext()) {
            FocusRequester value = it.next().getValue();
            if (value.hasSameBinder(iBinder)) {
                it.remove();
                value.release();
                notifyExtFocusPolicyFocusAbandon_syncAf(value.toAudioFocusInfo());
                return;
            }
        }
    }

    private boolean canReassignAudioFocus() {
        if (!this.mFocusStack.isEmpty() && isLockedFocusOwner(this.mFocusStack.peek())) {
            return false;
        }
        return true;
    }

    private boolean isLockedFocusOwner(FocusRequester focusRequester) {
        return focusRequester.hasSameClient("AudioFocus_For_Phone_Ring_And_Calls") || focusRequester.isLockedFocusOwner();
    }

    @GuardedBy("mAudioFocusLock")
    private int pushBelowLockedFocusOwners(FocusRequester focusRequester) {
        int size = this.mFocusStack.size();
        for (int size2 = this.mFocusStack.size() - 1; size2 >= 0; size2--) {
            if (isLockedFocusOwner(this.mFocusStack.elementAt(size2))) {
                size = size2;
            }
        }
        if (size == this.mFocusStack.size()) {
            Log.e(TAG, "No exclusive focus owner found in propagateFocusLossFromGain_syncAf()", new Exception());
            propagateFocusLossFromGain_syncAf(focusRequester.getGainRequest(), focusRequester, false);
            this.mFocusStack.push(focusRequester);
            return 1;
        }
        this.mFocusStack.insertElementAt(focusRequester, size);
        return 2;
    }

    protected class AudioFocusDeathHandler implements IBinder.DeathRecipient {
        private IBinder mCb;

        AudioFocusDeathHandler(IBinder iBinder) {
            this.mCb = iBinder;
        }

        @Override
        public void binderDied() {
            synchronized (MediaFocusControl.mAudioFocusLock) {
                if (MediaFocusControl.this.mFocusPolicy != null) {
                    MediaFocusControl.this.removeFocusEntryForExtPolicy(this.mCb);
                } else {
                    MediaFocusControl.this.removeFocusStackEntryOnDeath(this.mCb);
                }
            }
        }
    }

    protected void setDuckingInExtPolicyAvailable(boolean z) {
        this.mNotifyFocusOwnerOnDuck = !z;
    }

    boolean mustNotifyFocusOwnerOnDuck() {
        return this.mNotifyFocusOwnerOnDuck;
    }

    void addFocusFollower(IAudioPolicyCallback iAudioPolicyCallback) {
        if (iAudioPolicyCallback == null) {
            return;
        }
        synchronized (mAudioFocusLock) {
            boolean z = false;
            Iterator<IAudioPolicyCallback> it = this.mFocusFollowers.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                } else if (it.next().asBinder().equals(iAudioPolicyCallback.asBinder())) {
                    z = true;
                    break;
                }
            }
            if (z) {
                return;
            }
            this.mFocusFollowers.add(iAudioPolicyCallback);
            notifyExtPolicyCurrentFocusAsync(iAudioPolicyCallback);
        }
    }

    void removeFocusFollower(IAudioPolicyCallback iAudioPolicyCallback) {
        if (iAudioPolicyCallback == null) {
            return;
        }
        synchronized (mAudioFocusLock) {
            Iterator<IAudioPolicyCallback> it = this.mFocusFollowers.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                IAudioPolicyCallback next = it.next();
                if (next.asBinder().equals(iAudioPolicyCallback.asBinder())) {
                    this.mFocusFollowers.remove(next);
                    break;
                }
            }
        }
    }

    void setFocusPolicy(IAudioPolicyCallback iAudioPolicyCallback) {
        if (iAudioPolicyCallback == null) {
            return;
        }
        synchronized (mAudioFocusLock) {
            this.mFocusPolicy = iAudioPolicyCallback;
        }
    }

    void unsetFocusPolicy(IAudioPolicyCallback iAudioPolicyCallback) {
        if (iAudioPolicyCallback == null) {
            return;
        }
        synchronized (mAudioFocusLock) {
            if (this.mFocusPolicy == iAudioPolicyCallback) {
                this.mFocusPolicy = null;
            }
        }
    }

    void notifyExtPolicyCurrentFocusAsync(final IAudioPolicyCallback iAudioPolicyCallback) {
        new Thread() {
            @Override
            public void run() {
                synchronized (MediaFocusControl.mAudioFocusLock) {
                    if (MediaFocusControl.this.mFocusStack.isEmpty()) {
                        return;
                    }
                    try {
                        iAudioPolicyCallback.notifyAudioFocusGrant(((FocusRequester) MediaFocusControl.this.mFocusStack.peek()).toAudioFocusInfo(), 1);
                    } catch (RemoteException e) {
                        Log.e(MediaFocusControl.TAG, "Can't call notifyAudioFocusGrant() on IAudioPolicyCallback " + iAudioPolicyCallback.asBinder(), e);
                    }
                }
            }
        }.start();
    }

    void notifyExtPolicyFocusGrant_syncAf(AudioFocusInfo audioFocusInfo, int i) {
        for (IAudioPolicyCallback iAudioPolicyCallback : this.mFocusFollowers) {
            try {
                iAudioPolicyCallback.notifyAudioFocusGrant(audioFocusInfo, i);
            } catch (RemoteException e) {
                Log.e(TAG, "Can't call notifyAudioFocusGrant() on IAudioPolicyCallback " + iAudioPolicyCallback.asBinder(), e);
            }
        }
    }

    void notifyExtPolicyFocusLoss_syncAf(AudioFocusInfo audioFocusInfo, boolean z) {
        for (IAudioPolicyCallback iAudioPolicyCallback : this.mFocusFollowers) {
            try {
                iAudioPolicyCallback.notifyAudioFocusLoss(audioFocusInfo, z);
            } catch (RemoteException e) {
                Log.e(TAG, "Can't call notifyAudioFocusLoss() on IAudioPolicyCallback " + iAudioPolicyCallback.asBinder(), e);
            }
        }
    }

    boolean notifyExtFocusPolicyFocusRequest_syncAf(AudioFocusInfo audioFocusInfo, IAudioFocusDispatcher iAudioFocusDispatcher, IBinder iBinder) {
        if (this.mFocusPolicy == null) {
            return false;
        }
        if (DEBUG) {
            Log.v(TAG, "notifyExtFocusPolicyFocusRequest client=" + audioFocusInfo.getClientId() + " dispatcher=" + iAudioFocusDispatcher);
        }
        synchronized (this.mExtFocusChangeLock) {
            long j = this.mExtFocusChangeCounter;
            this.mExtFocusChangeCounter = 1 + j;
            audioFocusInfo.setGen(j);
        }
        FocusRequester focusRequester = this.mFocusOwnersForFocusPolicy.get(audioFocusInfo.getClientId());
        if (focusRequester != null) {
            if (!focusRequester.hasSameDispatcher(iAudioFocusDispatcher)) {
                focusRequester.release();
                this.mFocusOwnersForFocusPolicy.put(audioFocusInfo.getClientId(), new FocusRequester(audioFocusInfo, iAudioFocusDispatcher, iBinder, new AudioFocusDeathHandler(iBinder), this));
            }
        } else {
            this.mFocusOwnersForFocusPolicy.put(audioFocusInfo.getClientId(), new FocusRequester(audioFocusInfo, iAudioFocusDispatcher, iBinder, new AudioFocusDeathHandler(iBinder), this));
        }
        try {
            this.mFocusPolicy.notifyAudioFocusRequest(audioFocusInfo, 1);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Can't call notifyAudioFocusRequest() on IAudioPolicyCallback " + this.mFocusPolicy.asBinder(), e);
            return false;
        }
    }

    void setFocusRequestResultFromExtPolicy(AudioFocusInfo audioFocusInfo, int i) {
        synchronized (this.mExtFocusChangeLock) {
            if (audioFocusInfo.getGen() > this.mExtFocusChangeCounter) {
                return;
            }
            FocusRequester focusRequester = this.mFocusOwnersForFocusPolicy.get(audioFocusInfo.getClientId());
            if (focusRequester != null) {
                focusRequester.dispatchFocusResultFromExtPolicy(i);
            }
        }
    }

    boolean notifyExtFocusPolicyFocusAbandon_syncAf(AudioFocusInfo audioFocusInfo) {
        if (this.mFocusPolicy == null) {
            return false;
        }
        FocusRequester focusRequesterRemove = this.mFocusOwnersForFocusPolicy.remove(audioFocusInfo.getClientId());
        if (focusRequesterRemove != null) {
            focusRequesterRemove.release();
        }
        try {
            this.mFocusPolicy.notifyAudioFocusAbandon(audioFocusInfo);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Can't call notifyAudioFocusAbandon() on IAudioPolicyCallback " + this.mFocusPolicy.asBinder(), e);
            return true;
        }
    }

    int dispatchFocusChange(AudioFocusInfo audioFocusInfo, int i) {
        FocusRequester focusRequesterRemove;
        if (DEBUG) {
            Log.v(TAG, "dispatchFocusChange " + i + " to afi client=" + audioFocusInfo.getClientId());
        }
        synchronized (mAudioFocusLock) {
            if (this.mFocusPolicy == null) {
                if (DEBUG) {
                    Log.v(TAG, "> failed: no focus policy");
                }
                return 0;
            }
            if (i == -1) {
                focusRequesterRemove = this.mFocusOwnersForFocusPolicy.remove(audioFocusInfo.getClientId());
            } else {
                focusRequesterRemove = this.mFocusOwnersForFocusPolicy.get(audioFocusInfo.getClientId());
            }
            if (focusRequesterRemove == null) {
                if (DEBUG) {
                    Log.v(TAG, "> failed: no such focus requester known");
                }
                return 0;
            }
            return focusRequesterRemove.dispatchFocusChange(i);
        }
    }

    private void dumpExtFocusPolicyFocusOwners(PrintWriter printWriter) {
        Iterator<Map.Entry<String, FocusRequester>> it = this.mFocusOwnersForFocusPolicy.entrySet().iterator();
        while (it.hasNext()) {
            it.next().getValue().dump(printWriter);
        }
    }

    protected int getCurrentAudioFocus() {
        synchronized (mAudioFocusLock) {
            if (this.mFocusStack.empty()) {
                return 0;
            }
            return this.mFocusStack.peek().getGainRequest();
        }
    }

    protected static int getFocusRampTimeMs(int i, AudioAttributes audioAttributes) {
        switch (audioAttributes.getUsage()) {
            case 1:
            case 14:
                return 1000;
            case 2:
            case 3:
            case 5:
            case 7:
            case 8:
            case 9:
            case 10:
            case 13:
                return SystemService.PHASE_SYSTEM_SERVICES_READY;
            case 4:
            case 6:
            case 11:
            case 12:
            case 16:
                return 700;
            case 15:
            default:
                return 0;
        }
    }

    protected int requestAudioFocus(AudioAttributes audioAttributes, int i, IBinder iBinder, IAudioFocusDispatcher iAudioFocusDispatcher, String str, String str2, int i2, int i3, boolean z) {
        int i4;
        ?? r12;
        AudioFocusInfo audioFocusInfo;
        ?? r18;
        boolean z2;
        mEventLogger.log(new AudioEventLogger.StringEvent("requestAudioFocus() from uid/pid " + Binder.getCallingUid() + SliceClientPermissions.SliceAuthority.DELIMITER + Binder.getCallingPid() + " clientId=" + str + " callingPack=" + str2 + " req=" + i + " flags=0x" + Integer.toHexString(i2) + " sdk=" + i3).printLog(TAG));
        if (!iBinder.pingBinder()) {
            Log.e(TAG, " AudioFocus DOA client for requestAudioFocus(), aborting.");
            return 0;
        }
        if (this.mAppOps.noteOp(32, Binder.getCallingUid(), str2) != 0) {
            return 0;
        }
        synchronized (mAudioFocusLock) {
            if (this.mFocusStack.size() > 100) {
                Log.e(TAG, "Max AudioFocus stack size reached, failing requestAudioFocus()");
                return 0;
            }
            boolean z3 = (!this.mRingOrCallActive) & ("AudioFocus_For_Phone_Ring_And_Calls".compareTo(str) == 0);
            if (z3) {
                this.mRingOrCallActive = true;
            }
            if (this.mFocusPolicy != null) {
                i4 = 100;
                r12 = 0;
                audioFocusInfo = new AudioFocusInfo(audioAttributes, Binder.getCallingUid(), str, str2, i, 0, i2, i3);
            } else {
                i4 = 100;
                r12 = 0;
                audioFocusInfo = null;
            }
            AudioFocusInfo audioFocusInfo2 = audioFocusInfo;
            if (canReassignAudioFocus()) {
                r18 = r12;
            } else {
                if ((i2 & 1) == 0) {
                    return r12;
                }
                r18 = 1;
            }
            if (notifyExtFocusPolicyFocusRequest_syncAf(audioFocusInfo2, iAudioFocusDispatcher, iBinder)) {
                return i4;
            }
            AudioFocusDeathHandler audioFocusDeathHandler = new AudioFocusDeathHandler(iBinder);
            try {
                iBinder.linkToDeath(audioFocusDeathHandler, r12);
                if (!this.mFocusStack.empty() && this.mFocusStack.peek().hasSameClient(str)) {
                    FocusRequester focusRequesterPeek = this.mFocusStack.peek();
                    if (focusRequesterPeek.getGainRequest() == i && focusRequesterPeek.getGrantFlags() == i2) {
                        iBinder.unlinkToDeath(audioFocusDeathHandler, r12);
                        notifyExtPolicyFocusGrant_syncAf(focusRequesterPeek.toAudioFocusInfo(), 1);
                        return 1;
                    }
                    z2 = true;
                    if (r18 == 0) {
                        this.mFocusStack.pop();
                        focusRequesterPeek.release();
                    }
                } else {
                    z2 = true;
                }
                removeFocusStackEntry(str, r12, r12);
                FocusRequester focusRequester = new FocusRequester(audioAttributes, i, i2, iAudioFocusDispatcher, iBinder, str, audioFocusDeathHandler, str2, Binder.getCallingUid(), this, i3);
                if (r18 != 0) {
                    int iPushBelowLockedFocusOwners = pushBelowLockedFocusOwners(focusRequester);
                    if (iPushBelowLockedFocusOwners != 0) {
                        notifyExtPolicyFocusGrant_syncAf(focusRequester.toAudioFocusInfo(), iPushBelowLockedFocusOwners);
                    }
                    return iPushBelowLockedFocusOwners;
                }
                if (!this.mFocusStack.empty()) {
                    propagateFocusLossFromGain_syncAf(i, focusRequester, z);
                }
                this.mFocusStack.push(focusRequester);
                focusRequester.handleFocusGainFromRequest(1);
                notifyExtPolicyFocusGrant_syncAf(focusRequester.toAudioFocusInfo(), 1);
                if (true & z3) {
                    runAudioCheckerForRingOrCallAsync(true);
                }
                return 1;
            } catch (RemoteException e) {
                Log.w(TAG, "AudioFocus  requestAudioFocus() could not link to " + ((Object) iBinder) + " binder death");
                return r12;
            }
        }
    }

    protected int abandonAudioFocus(IAudioFocusDispatcher iAudioFocusDispatcher, String str, AudioAttributes audioAttributes, String str2) {
        mEventLogger.log(new AudioEventLogger.StringEvent("abandonAudioFocus() from uid/pid " + Binder.getCallingUid() + SliceClientPermissions.SliceAuthority.DELIMITER + Binder.getCallingPid() + " clientId=" + str).printLog(TAG));
        try {
        } catch (ConcurrentModificationException e) {
            Log.e(TAG, "FATAL EXCEPTION AudioFocus  abandonAudioFocus() caused " + e);
            e.printStackTrace();
        }
        synchronized (mAudioFocusLock) {
            if (this.mFocusPolicy != null && notifyExtFocusPolicyFocusAbandon_syncAf(new AudioFocusInfo(audioAttributes, Binder.getCallingUid(), str, str2, 0, 0, 0, 0))) {
                return 1;
            }
            boolean z = this.mRingOrCallActive & ("AudioFocus_For_Phone_Ring_And_Calls".compareTo(str) == 0);
            if (z) {
                this.mRingOrCallActive = false;
            }
            removeFocusStackEntry(str, true, true);
            if (true & z) {
                runAudioCheckerForRingOrCallAsync(false);
            }
            return 1;
        }
    }

    protected void unregisterAudioFocusClient(String str) {
        synchronized (mAudioFocusLock) {
            removeFocusStackEntry(str, false, true);
        }
    }

    private void runAudioCheckerForRingOrCallAsync(final boolean z) {
        new Thread() {
            @Override
            public void run() {
                if (z) {
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                synchronized (MediaFocusControl.mAudioFocusLock) {
                    if (MediaFocusControl.this.mRingOrCallActive) {
                        MediaFocusControl.this.mFocusEnforcer.mutePlayersForCall(MediaFocusControl.USAGES_TO_MUTE_IN_RING_OR_CALL);
                    } else {
                        MediaFocusControl.this.mFocusEnforcer.unmutePlayersForCall();
                    }
                }
            }
        }.start();
    }
}
