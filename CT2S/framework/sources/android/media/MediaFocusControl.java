package android.media;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.media.AudioService;
import android.media.PlayerRecord;
import android.media.audiopolicy.IAudioPolicyCallback;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Slog;
import android.view.KeyEvent;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

public class MediaFocusControl implements PendingIntent.OnFinished {
    protected static final boolean DEBUG_RC = false;
    protected static final boolean DEBUG_VOL = false;
    private static final String EXTRA_WAKELOCK_ACQUIRED = "android.media.AudioService.WAKELOCK_ACQUIRED";
    protected static final String IN_VOICE_COMM_FOCUS_ID = "AudioFocus_For_Phone_Ring_And_Calls";
    private static final int MSG_RCC_NEW_PLAYBACK_INFO = 4;
    private static final int MSG_RCC_NEW_PLAYBACK_STATE = 6;
    private static final int MSG_RCC_NEW_VOLUME_OBS = 5;
    private static final int MSG_RCC_SEEK_REQUEST = 7;
    private static final int MSG_RCC_UPDATE_METADATA = 8;
    private static final int MSG_RCDISPLAY_CLEAR = 1;
    private static final int MSG_RCDISPLAY_INIT_INFO = 9;
    private static final int MSG_RCDISPLAY_UPDATE = 2;
    private static final int MSG_REEVALUATE_RCD = 10;
    private static final int MSG_REEVALUATE_REMOTE = 3;
    private static final int MSG_UNREGISTER_MEDIABUTTONINTENT = 11;
    private static final int RCD_REG_FAILURE = 0;
    private static final int RCD_REG_SUCCESS_ENABLED_NOTIF = 2;
    private static final int RCD_REG_SUCCESS_PERMISSION = 1;
    private static final int RC_INFO_ALL = 15;
    private static final int RC_INFO_NONE = 0;
    private static final int SENDMSG_NOOP = 1;
    private static final int SENDMSG_QUEUE = 2;
    private static final int SENDMSG_REPLACE = 0;
    private static final String TAG = "MediaFocusControl";
    private static final int VOICEBUTTON_ACTION_DISCARD_CURRENT_KEY_PRESS = 1;
    private static final int VOICEBUTTON_ACTION_SIMULATE_KEY_PRESS = 3;
    private static final int VOICEBUTTON_ACTION_START_VOICE_INPUT = 2;
    private static final int WAKELOCK_RELEASE_ON_FINISHED = 1980;
    private final AppOpsManager mAppOps;
    private final AudioService mAudioService;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private final MediaEventHandler mEventHandler;
    private boolean mHasRemotePlayback;
    private final KeyguardManager mKeyguardManager;
    private PlayerRecord.RemotePlaybackState mMainRemote;
    private boolean mMainRemoteIsActive;
    private final PowerManager.WakeLock mMediaEventWakeLock;
    private final NotificationListenerObserver mNotifListenerObserver;
    private boolean mVoiceButtonDown;
    private boolean mVoiceButtonHandled;
    private final AudioService.VolumeController mVolumeController;
    private static final Uri ENABLED_NOTIFICATION_LISTENERS_URI = Settings.Secure.getUriFor(Settings.Secure.ENABLED_NOTIFICATION_LISTENERS);
    private static final Object mAudioFocusLock = new Object();
    private static final Object mRingingLock = new Object();
    private boolean mIsRinging = false;
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == 1) {
                synchronized (MediaFocusControl.mRingingLock) {
                    MediaFocusControl.this.mIsRinging = true;
                }
            } else if (state == 2 || state == 0) {
                synchronized (MediaFocusControl.mRingingLock) {
                    MediaFocusControl.this.mIsRinging = false;
                }
            }
        }
    };
    private final Stack<FocusRequester> mFocusStack = new Stack<>();
    private boolean mNotifyFocusOwnerOnDuck = true;
    private ArrayList<IAudioPolicyCallback> mFocusFollowers = new ArrayList<>();
    private final Object mVoiceEventLock = new Object();
    BroadcastReceiver mKeyEventDone = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras;
            if (intent != null && (extras = intent.getExtras()) != null && extras.containsKey(MediaFocusControl.EXTRA_WAKELOCK_ACQUIRED)) {
                MediaFocusControl.this.mMediaEventWakeLock.release();
            }
        }
    };
    private final Object mCurrentRcLock = new Object();
    private IRemoteControlClient mCurrentRcClient = null;
    private PendingIntent mCurrentRcClientIntent = null;
    private int mCurrentRcClientGen = 0;
    private final Stack<PlayerRecord> mPRStack = new Stack<>();
    private ComponentName mMediaReceiverForCalls = null;
    private ArrayList<DisplayInfoForServer> mRcDisplays = new ArrayList<>(1);

    protected MediaFocusControl(Looper looper, Context cntxt, AudioService.VolumeController volumeCtrl, AudioService as) {
        this.mEventHandler = new MediaEventHandler(looper);
        this.mContext = cntxt;
        this.mContentResolver = this.mContext.getContentResolver();
        this.mVolumeController = volumeCtrl;
        this.mAudioService = as;
        PowerManager pm = (PowerManager) this.mContext.getSystemService(Context.POWER_SERVICE);
        this.mMediaEventWakeLock = pm.newWakeLock(1, "handleMediaEvent");
        this.mMainRemote = new PlayerRecord.RemotePlaybackState(-1, AudioService.getMaxStreamVolume(3), AudioService.getMaxStreamVolume(3));
        TelephonyManager tmgr = (TelephonyManager) this.mContext.getSystemService("phone");
        tmgr.listen(this.mPhoneStateListener, 32);
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService(Context.APP_OPS_SERVICE);
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService(Context.KEYGUARD_SERVICE);
        this.mNotifListenerObserver = new NotificationListenerObserver();
        this.mHasRemotePlayback = false;
        this.mMainRemoteIsActive = false;
        PlayerRecord.setMediaFocusControl(this);
        postReevaluateRemote();
    }

    protected void dump(PrintWriter pw) {
        dumpFocusStack(pw);
        dumpRCStack(pw);
        dumpRCCStack(pw);
        dumpRCDList(pw);
    }

    private class NotificationListenerObserver extends ContentObserver {
        NotificationListenerObserver() {
            super(MediaFocusControl.this.mEventHandler);
            MediaFocusControl.this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.ENABLED_NOTIFICATION_LISTENERS), false, this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (MediaFocusControl.ENABLED_NOTIFICATION_LISTENERS_URI.equals(uri) && !selfChange) {
                MediaFocusControl.this.postReevaluateRemoteControlDisplays();
            }
        }
    }

    private int checkRcdRegistrationAuthorization(ComponentName listenerComp) {
        if (this.mContext.checkCallingOrSelfPermission(Manifest.permission.MEDIA_CONTENT_CONTROL) == 0) {
            return 1;
        }
        if (listenerComp != null) {
            long ident = Binder.clearCallingIdentity();
            try {
                int currentUser = ActivityManager.getCurrentUser();
                String enabledNotifListeners = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), Settings.Secure.ENABLED_NOTIFICATION_LISTENERS, currentUser);
                if (enabledNotifListeners != null) {
                    String[] components = enabledNotifListeners.split(":");
                    for (String str : components) {
                        ComponentName component = ComponentName.unflattenFromString(str);
                        if (component != null && listenerComp.equals(component)) {
                            return 2;
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return 0;
    }

    protected boolean registerRemoteController(IRemoteControlDisplay rcd, int w, int h, ComponentName listenerComp) {
        int reg = checkRcdRegistrationAuthorization(listenerComp);
        if (reg != 0) {
            registerRemoteControlDisplay_int(rcd, w, h, listenerComp);
            return true;
        }
        Slog.w(TAG, "Access denied to process: " + Binder.getCallingPid() + ", must have permission " + Manifest.permission.MEDIA_CONTENT_CONTROL + " or be an enabled NotificationListenerService for registerRemoteController");
        return false;
    }

    protected boolean registerRemoteControlDisplay(IRemoteControlDisplay rcd, int w, int h) {
        int reg = checkRcdRegistrationAuthorization(null);
        if (reg != 0) {
            registerRemoteControlDisplay_int(rcd, w, h, null);
            return true;
        }
        Slog.w(TAG, "Access denied to process: " + Binder.getCallingPid() + ", must have permission " + Manifest.permission.MEDIA_CONTENT_CONTROL + " to register IRemoteControlDisplay");
        return false;
    }

    private void postReevaluateRemoteControlDisplays() {
        sendMsg(this.mEventHandler, 10, 2, 0, 0, null, 0);
    }

    private void onReevaluateRemoteControlDisplays() {
        String[] enabledComponents;
        int currentUser = ActivityManager.getCurrentUser();
        String enabledNotifListeners = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), Settings.Secure.ENABLED_NOTIFICATION_LISTENERS, currentUser);
        synchronized (mAudioFocusLock) {
            synchronized (this.mPRStack) {
                if (enabledNotifListeners == null) {
                    enabledComponents = null;
                } else {
                    enabledComponents = enabledNotifListeners.split(":");
                }
                for (DisplayInfoForServer di : this.mRcDisplays) {
                    if (di.mClientNotifListComp != null) {
                        boolean wasEnabled = di.mEnabled;
                        di.mEnabled = isComponentInStringArray(di.mClientNotifListComp, enabledComponents);
                        if (wasEnabled != di.mEnabled) {
                            try {
                                di.mRcDisplay.setEnabled(di.mEnabled);
                                enableRemoteControlDisplayForClient_syncRcStack(di.mRcDisplay, di.mEnabled);
                                if (di.mEnabled) {
                                    sendMsg(this.mEventHandler, 9, 2, di.mArtworkExpectedWidth, di.mArtworkExpectedHeight, di.mRcDisplay, 0);
                                }
                            } catch (RemoteException e) {
                                Log.e(TAG, "Error en/disabling RCD: ", e);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isComponentInStringArray(ComponentName comp, String[] enabledArray) {
        if (enabledArray == null || enabledArray.length == 0) {
            return false;
        }
        String compString = comp.flattenToString();
        for (String str : enabledArray) {
            if (compString.equals(str)) {
                return true;
            }
        }
        return false;
    }

    private static void sendMsg(Handler handler, int msg, int existingMsgPolicy, int arg1, int arg2, Object obj, int delay) {
        if (existingMsgPolicy == 0) {
            handler.removeMessages(msg);
        } else if (existingMsgPolicy == 1 && handler.hasMessages(msg)) {
            return;
        }
        handler.sendMessageDelayed(handler.obtainMessage(msg, arg1, arg2, obj), delay);
    }

    private class MediaEventHandler extends Handler {
        MediaEventHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MediaFocusControl.this.onRcDisplayClear();
                    break;
                case 2:
                    MediaFocusControl.this.onRcDisplayUpdate((PlayerRecord) msg.obj, msg.arg1);
                    break;
                case 3:
                    MediaFocusControl.this.onReevaluateRemote();
                    break;
                case 5:
                    MediaFocusControl.this.onRegisterVolumeObserverForRcc(msg.arg1, (IRemoteVolumeObserver) msg.obj);
                    break;
                case 9:
                    MediaFocusControl.this.onRcDisplayInitInfo((IRemoteControlDisplay) msg.obj, msg.arg1, msg.arg2);
                    break;
                case 10:
                    MediaFocusControl.this.onReevaluateRemoteControlDisplays();
                    break;
                case 11:
                    MediaFocusControl.this.unregisterMediaButtonIntent((PendingIntent) msg.obj);
                    break;
            }
        }
    }

    protected void discardAudioFocusOwner() {
        synchronized (mAudioFocusLock) {
            if (!this.mFocusStack.empty()) {
                FocusRequester exFocusOwner = this.mFocusStack.pop();
                exFocusOwner.handleFocusLoss(-1);
                exFocusOwner.release();
            }
        }
    }

    private void notifyTopOfAudioFocusStack() {
        if (!this.mFocusStack.empty() && canReassignAudioFocus()) {
            this.mFocusStack.peek().handleFocusGain(1);
        }
    }

    private void propagateFocusLossFromGain_syncAf(int focusGain) {
        Iterator<FocusRequester> stackIterator = this.mFocusStack.iterator();
        while (stackIterator.hasNext()) {
            stackIterator.next().handleExternalFocusGain(focusGain);
        }
    }

    private void dumpFocusStack(PrintWriter pw) {
        pw.println("\nAudio Focus stack entries (last is top of stack):");
        synchronized (mAudioFocusLock) {
            Iterator<FocusRequester> stackIterator = this.mFocusStack.iterator();
            while (stackIterator.hasNext()) {
                stackIterator.next().dump(pw);
            }
        }
        pw.println("\n Notify on duck: " + this.mNotifyFocusOwnerOnDuck + "\n");
    }

    private void removeFocusStackEntry(String clientToRemove, boolean signal, boolean notifyFocusFollowers) {
        if (!this.mFocusStack.empty() && this.mFocusStack.peek().hasSameClient(clientToRemove)) {
            FocusRequester fr = this.mFocusStack.pop();
            fr.release();
            if (notifyFocusFollowers) {
                AudioFocusInfo afi = fr.toAudioFocusInfo();
                afi.clearLossReceived();
                notifyExtPolicyFocusLoss_syncAf(afi, false);
            }
            if (signal) {
                notifyTopOfAudioFocusStack();
                return;
            }
            return;
        }
        Iterator<FocusRequester> stackIterator = this.mFocusStack.iterator();
        while (stackIterator.hasNext()) {
            FocusRequester fr2 = stackIterator.next();
            if (fr2.hasSameClient(clientToRemove)) {
                Log.i(TAG, "AudioFocus  removeFocusStackEntry(): removing entry for " + clientToRemove);
                stackIterator.remove();
                fr2.release();
            }
        }
    }

    private void removeFocusStackEntryForClient(IBinder cb) {
        boolean isTopOfStackForClientToRemove = !this.mFocusStack.isEmpty() && this.mFocusStack.peek().hasSameBinder(cb);
        Iterator<FocusRequester> stackIterator = this.mFocusStack.iterator();
        while (stackIterator.hasNext()) {
            FocusRequester fr = stackIterator.next();
            if (fr.hasSameBinder(cb)) {
                Log.i(TAG, "AudioFocus  removeFocusStackEntry(): removing entry for " + cb);
                stackIterator.remove();
            }
        }
        if (isTopOfStackForClientToRemove) {
            notifyTopOfAudioFocusStack();
        }
    }

    private boolean canReassignAudioFocus() {
        return this.mFocusStack.isEmpty() || !isLockedFocusOwner(this.mFocusStack.peek());
    }

    private boolean isLockedFocusOwner(FocusRequester fr) {
        return fr.hasSameClient(IN_VOICE_COMM_FOCUS_ID) || fr.isLockedFocusOwner();
    }

    private int pushBelowLockedFocusOwners(FocusRequester nfr) {
        int lastLockedFocusOwnerIndex = this.mFocusStack.size();
        for (int index = this.mFocusStack.size() - 1; index >= 0; index--) {
            if (isLockedFocusOwner(this.mFocusStack.elementAt(index))) {
                lastLockedFocusOwnerIndex = index;
            }
        }
        if (lastLockedFocusOwnerIndex == this.mFocusStack.size()) {
            Log.e(TAG, "No exclusive focus owner found in propagateFocusLossFromGain_syncAf()", new Exception());
            propagateFocusLossFromGain_syncAf(nfr.getGainRequest());
            this.mFocusStack.push(nfr);
            return 1;
        }
        this.mFocusStack.insertElementAt(nfr, lastLockedFocusOwnerIndex);
        return 2;
    }

    protected class AudioFocusDeathHandler implements IBinder.DeathRecipient {
        private IBinder mCb;

        AudioFocusDeathHandler(IBinder cb) {
            this.mCb = cb;
        }

        @Override
        public void binderDied() {
            synchronized (MediaFocusControl.mAudioFocusLock) {
                Log.w(MediaFocusControl.TAG, "  AudioFocus   audio focus client died");
                MediaFocusControl.this.removeFocusStackEntryForClient(this.mCb);
            }
        }

        public IBinder getBinder() {
            return this.mCb;
        }
    }

    protected void setDuckingInExtPolicyAvailable(boolean available) {
        this.mNotifyFocusOwnerOnDuck = !available;
    }

    boolean mustNotifyFocusOwnerOnDuck() {
        return this.mNotifyFocusOwnerOnDuck;
    }

    void addFocusFollower(IAudioPolicyCallback ff) {
        if (ff != null) {
            synchronized (mAudioFocusLock) {
                boolean found = false;
                Iterator<IAudioPolicyCallback> it = this.mFocusFollowers.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    IAudioPolicyCallback pcb = it.next();
                    if (pcb.asBinder().equals(ff.asBinder())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    this.mFocusFollowers.add(ff);
                }
            }
        }
    }

    void removeFocusFollower(IAudioPolicyCallback ff) {
        if (ff != null) {
            synchronized (mAudioFocusLock) {
                Iterator<IAudioPolicyCallback> it = this.mFocusFollowers.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    IAudioPolicyCallback pcb = it.next();
                    if (pcb.asBinder().equals(ff.asBinder())) {
                        this.mFocusFollowers.remove(pcb);
                        break;
                    }
                }
            }
        }
    }

    void notifyExtPolicyFocusGrant_syncAf(AudioFocusInfo afi, int requestResult) {
        for (IAudioPolicyCallback pcb : this.mFocusFollowers) {
            try {
                pcb.notifyAudioFocusGrant(afi, requestResult);
            } catch (RemoteException e) {
                Log.e(TAG, "Can't call newAudioFocusLoser() on IAudioPolicyCallback " + pcb.asBinder(), e);
            }
        }
    }

    void notifyExtPolicyFocusLoss_syncAf(AudioFocusInfo afi, boolean wasDispatched) {
        for (IAudioPolicyCallback pcb : this.mFocusFollowers) {
            try {
                pcb.notifyAudioFocusLoss(afi, wasDispatched);
            } catch (RemoteException e) {
                Log.e(TAG, "Can't call newAudioFocusLoser() on IAudioPolicyCallback " + pcb.asBinder(), e);
            }
        }
    }

    protected int getCurrentAudioFocus() {
        int gainRequest;
        synchronized (mAudioFocusLock) {
            gainRequest = this.mFocusStack.empty() ? 0 : this.mFocusStack.peek().getGainRequest();
        }
        return gainRequest;
    }

    protected int requestAudioFocus(AudioAttributes aa, int focusChangeHint, IBinder cb, IAudioFocusDispatcher fd, String clientId, String callingPackageName, int flags) {
        Log.i(TAG, " AudioFocus  requestAudioFocus() from " + clientId + " req=" + focusChangeHint + "flags=0x" + Integer.toHexString(flags));
        if (!cb.pingBinder()) {
            Log.e(TAG, " AudioFocus DOA client for requestAudioFocus(), aborting.");
            return 0;
        }
        if (this.mAppOps.noteOp(32, Binder.getCallingUid(), callingPackageName) != 0) {
            return 0;
        }
        synchronized (mAudioFocusLock) {
            boolean focusGrantDelayed = false;
            if (!canReassignAudioFocus()) {
                if ((flags & 1) == 0) {
                    return 0;
                }
                focusGrantDelayed = true;
            }
            AudioFocusDeathHandler afdh = new AudioFocusDeathHandler(cb);
            try {
                cb.linkToDeath(afdh, 0);
                if (!this.mFocusStack.empty() && this.mFocusStack.peek().hasSameClient(clientId)) {
                    FocusRequester fr = this.mFocusStack.peek();
                    if (fr.getGainRequest() == focusChangeHint && fr.getGrantFlags() == flags) {
                        cb.unlinkToDeath(afdh, 0);
                        notifyExtPolicyFocusGrant_syncAf(fr.toAudioFocusInfo(), 1);
                        return 1;
                    }
                    if (!focusGrantDelayed) {
                        this.mFocusStack.pop();
                        fr.release();
                    }
                }
                removeFocusStackEntry(clientId, false, false);
                FocusRequester nfr = new FocusRequester(aa, focusChangeHint, flags, fd, cb, clientId, afdh, callingPackageName, Binder.getCallingUid(), this);
                if (focusGrantDelayed) {
                    int requestResult = pushBelowLockedFocusOwners(nfr);
                    if (requestResult != 0) {
                        notifyExtPolicyFocusGrant_syncAf(nfr.toAudioFocusInfo(), requestResult);
                    }
                    return requestResult;
                }
                if (!this.mFocusStack.empty()) {
                    propagateFocusLossFromGain_syncAf(focusChangeHint);
                }
                this.mFocusStack.push(nfr);
                notifyExtPolicyFocusGrant_syncAf(nfr.toAudioFocusInfo(), 1);
                return 1;
            } catch (RemoteException e) {
                Log.w(TAG, "AudioFocus  requestAudioFocus() could not link to " + cb + " binder death");
                return 0;
            }
        }
    }

    protected int abandonAudioFocus(IAudioFocusDispatcher fl, String clientId, AudioAttributes aa) {
        Log.i(TAG, " AudioFocus  abandonAudioFocus() from " + clientId);
        try {
            synchronized (mAudioFocusLock) {
                removeFocusStackEntry(clientId, true, true);
            }
        } catch (ConcurrentModificationException cme) {
            Log.e(TAG, "FATAL EXCEPTION AudioFocus  abandonAudioFocus() caused " + cme);
            cme.printStackTrace();
        }
        return 1;
    }

    protected void unregisterAudioFocusClient(String clientId) {
        synchronized (mAudioFocusLock) {
            removeFocusStackEntry(clientId, false, true);
        }
    }

    protected void dispatchMediaKeyEvent(KeyEvent keyEvent) {
        filterMediaKeyEvent(keyEvent, false);
    }

    protected void dispatchMediaKeyEventUnderWakelock(KeyEvent keyEvent) {
        filterMediaKeyEvent(keyEvent, true);
    }

    private void filterMediaKeyEvent(KeyEvent keyEvent, boolean needWakeLock) {
        if (!isValidMediaKeyEvent(keyEvent)) {
            Log.e(TAG, "not dispatching invalid media key event " + keyEvent);
            return;
        }
        synchronized (mRingingLock) {
            synchronized (this.mPRStack) {
                if (this.mMediaReceiverForCalls != null && (this.mIsRinging || this.mAudioService.getMode() == 2)) {
                    dispatchMediaKeyEventForCalls(keyEvent, needWakeLock);
                } else if (isValidVoiceInputKeyCode(keyEvent.getKeyCode())) {
                    filterVoiceInputKeyEvent(keyEvent, needWakeLock);
                } else {
                    dispatchMediaKeyEvent(keyEvent, needWakeLock);
                }
            }
        }
    }

    private void dispatchMediaKeyEventForCalls(KeyEvent keyEvent, boolean needWakeLock) {
        Intent keyIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, (Uri) null);
        keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
        keyIntent.setPackage(this.mMediaReceiverForCalls.getPackageName());
        if (needWakeLock) {
            this.mMediaEventWakeLock.acquire();
            keyIntent.putExtra(EXTRA_WAKELOCK_ACQUIRED, WAKELOCK_RELEASE_ON_FINISHED);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.sendOrderedBroadcastAsUser(keyIntent, UserHandle.ALL, null, this.mKeyEventDone, this.mEventHandler, -1, null, null);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void dispatchMediaKeyEvent(KeyEvent keyEvent, boolean needWakeLock) {
        if (needWakeLock) {
            this.mMediaEventWakeLock.acquire();
        }
        Intent keyIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, (Uri) null);
        keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
        synchronized (this.mPRStack) {
            if (!this.mPRStack.empty()) {
                try {
                    this.mPRStack.peek().getMediaButtonIntent().send(this.mContext, needWakeLock ? WAKELOCK_RELEASE_ON_FINISHED : 0, keyIntent, this, this.mEventHandler);
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "Error sending pending intent " + this.mPRStack.peek());
                    e.printStackTrace();
                }
            } else {
                if (needWakeLock) {
                    keyIntent.putExtra(EXTRA_WAKELOCK_ACQUIRED, WAKELOCK_RELEASE_ON_FINISHED);
                }
                long ident = Binder.clearCallingIdentity();
                try {
                    this.mContext.sendOrderedBroadcastAsUser(keyIntent, UserHandle.ALL, null, this.mKeyEventDone, this.mEventHandler, -1, null, null);
                    Binder.restoreCallingIdentity(ident);
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(ident);
                    throw th;
                }
            }
        }
    }

    private void filterVoiceInputKeyEvent(KeyEvent keyEvent, boolean needWakeLock) {
        int voiceButtonAction = 1;
        int keyAction = keyEvent.getAction();
        synchronized (this.mVoiceEventLock) {
            if (keyAction == 0) {
                if (keyEvent.getRepeatCount() == 0) {
                    this.mVoiceButtonDown = true;
                    this.mVoiceButtonHandled = false;
                } else if (this.mVoiceButtonDown && !this.mVoiceButtonHandled && (keyEvent.getFlags() & 128) != 0) {
                    this.mVoiceButtonHandled = true;
                    voiceButtonAction = 2;
                }
            } else if (keyAction == 1 && this.mVoiceButtonDown) {
                this.mVoiceButtonDown = false;
                if (!this.mVoiceButtonHandled && !keyEvent.isCanceled()) {
                    voiceButtonAction = 3;
                }
            }
        }
        switch (voiceButtonAction) {
            case 1:
            default:
                return;
            case 2:
                startVoiceBasedInteractions(needWakeLock);
                return;
            case 3:
                sendSimulatedMediaButtonEvent(keyEvent, needWakeLock);
                return;
        }
    }

    private void sendSimulatedMediaButtonEvent(KeyEvent originalKeyEvent, boolean needWakeLock) {
        KeyEvent keyEvent = KeyEvent.changeAction(originalKeyEvent, 0);
        dispatchMediaKeyEvent(keyEvent, needWakeLock);
        KeyEvent keyEvent2 = KeyEvent.changeAction(originalKeyEvent, 1);
        dispatchMediaKeyEvent(keyEvent2, needWakeLock);
    }

    private static boolean isValidMediaKeyEvent(KeyEvent keyEvent) {
        if (keyEvent == null) {
            return false;
        }
        return KeyEvent.isMediaKey(keyEvent.getKeyCode());
    }

    private static boolean isValidVoiceInputKeyCode(int keyCode) {
        return keyCode == 79;
    }

    private void startVoiceBasedInteractions(boolean needWakeLock) {
        Intent voiceIntent;
        PowerManager pm = (PowerManager) this.mContext.getSystemService(Context.POWER_SERVICE);
        boolean isLocked = this.mKeyguardManager != null && this.mKeyguardManager.isKeyguardLocked();
        if (!isLocked && pm.isScreenOn()) {
            voiceIntent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
            Log.i(TAG, "voice-based interactions: about to use ACTION_WEB_SEARCH");
        } else {
            voiceIntent = new Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE);
            voiceIntent.putExtra(RecognizerIntent.EXTRA_SECURE, isLocked && this.mKeyguardManager.isKeyguardSecure());
            Log.i(TAG, "voice-based interactions: about to use ACTION_VOICE_SEARCH_HANDS_FREE");
        }
        if (needWakeLock) {
            this.mMediaEventWakeLock.acquire();
        }
        long identity = Binder.clearCallingIdentity();
        if (voiceIntent != null) {
            try {
                try {
                    voiceIntent.setFlags(276824064);
                    this.mContext.startActivityAsUser(voiceIntent, UserHandle.CURRENT);
                } catch (ActivityNotFoundException e) {
                    Log.w(TAG, "No activity for search: " + e);
                    Binder.restoreCallingIdentity(identity);
                    if (needWakeLock) {
                        this.mMediaEventWakeLock.release();
                        return;
                    }
                    return;
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
                if (needWakeLock) {
                    this.mMediaEventWakeLock.release();
                }
                throw th;
            }
        }
        Binder.restoreCallingIdentity(identity);
        if (needWakeLock) {
            this.mMediaEventWakeLock.release();
        }
    }

    @Override
    public void onSendFinished(PendingIntent pendingIntent, Intent intent, int resultCode, String resultData, Bundle resultExtras) {
        if (resultCode == WAKELOCK_RELEASE_ON_FINISHED) {
            this.mMediaEventWakeLock.release();
        }
    }

    private void dumpRCStack(PrintWriter pw) {
        pw.println("\nRemote Control stack entries (last is top of stack):");
        synchronized (this.mPRStack) {
            Iterator<PlayerRecord> stackIterator = this.mPRStack.iterator();
            while (stackIterator.hasNext()) {
                stackIterator.next().dump(pw, true);
            }
        }
    }

    private void dumpRCCStack(PrintWriter pw) {
        pw.println("\nRemote Control Client stack entries (last is top of stack):");
        synchronized (this.mPRStack) {
            Iterator<PlayerRecord> stackIterator = this.mPRStack.iterator();
            while (stackIterator.hasNext()) {
                stackIterator.next().dump(pw, false);
            }
            synchronized (this.mCurrentRcLock) {
                pw.println("\nCurrent remote control generation ID = " + this.mCurrentRcClientGen);
            }
        }
        synchronized (this.mMainRemote) {
            pw.println("\nRemote Volume State:");
            pw.println("  has remote: " + this.mHasRemotePlayback);
            pw.println("  is remote active: " + this.mMainRemoteIsActive);
            pw.println("  rccId: " + this.mMainRemote.mRccId);
            pw.println("  volume handling: " + (this.mMainRemote.mVolumeHandling == 0 ? "PLAYBACK_VOLUME_FIXED(0)" : "PLAYBACK_VOLUME_VARIABLE(1)"));
            pw.println("  volume: " + this.mMainRemote.mVolume);
            pw.println("  volume steps: " + this.mMainRemote.mVolumeMax);
        }
    }

    private void dumpRCDList(PrintWriter pw) {
        pw.println("\nRemote Control Display list entries:");
        synchronized (this.mPRStack) {
            for (DisplayInfoForServer di : this.mRcDisplays) {
                pw.println("  IRCD: " + di.mRcDisplay + "  -- w:" + di.mArtworkExpectedWidth + "  -- h:" + di.mArtworkExpectedHeight + "  -- wantsPosSync:" + di.mWantsPositionSync + "  -- " + (di.mEnabled ? "enabled" : "disabled"));
            }
        }
    }

    private boolean pushMediaButtonReceiver_syncPrs(PendingIntent mediaIntent, ComponentName target, IBinder token) {
        if (this.mPRStack.empty()) {
            this.mPRStack.push(new PlayerRecord(mediaIntent, target, token));
            return true;
        }
        if (this.mPRStack.peek().hasMatchingMediaButtonIntent(mediaIntent) || this.mAppOps.noteOp(31, Binder.getCallingUid(), mediaIntent.getCreatorPackage()) != 0) {
            return false;
        }
        this.mPRStack.lastElement();
        int lastPlayingIndex = this.mPRStack.size();
        int inStackIndex = -1;
        try {
            int index = this.mPRStack.size() - 1;
            PlayerRecord prse = null;
            while (index >= 0) {
                try {
                    PlayerRecord prse2 = this.mPRStack.elementAt(index);
                    if (prse2.isPlaybackActive()) {
                        lastPlayingIndex = index;
                    }
                    if (prse2.hasMatchingMediaButtonIntent(mediaIntent)) {
                        inStackIndex = index;
                    }
                    index--;
                    prse = prse2;
                } catch (ArrayIndexOutOfBoundsException e) {
                    e = e;
                    Log.e(TAG, "Wrong index (inStack=" + inStackIndex + " lastPlaying=" + lastPlayingIndex + " size=" + this.mPRStack.size() + " accessing media button stack", e);
                    return false;
                }
            }
            if (inStackIndex == -1) {
                PlayerRecord prse3 = new PlayerRecord(mediaIntent, target, token);
                this.mPRStack.add(lastPlayingIndex, prse3);
                return false;
            }
            if (this.mPRStack.size() <= 1) {
                return false;
            }
            PlayerRecord prse4 = this.mPRStack.elementAt(inStackIndex);
            this.mPRStack.removeElementAt(inStackIndex);
            if (prse4.isPlaybackActive()) {
                this.mPRStack.push(prse4);
                return false;
            }
            if (inStackIndex > lastPlayingIndex) {
                this.mPRStack.add(lastPlayingIndex, prse4);
                return false;
            }
            this.mPRStack.add(lastPlayingIndex - 1, prse4);
            return false;
        } catch (ArrayIndexOutOfBoundsException e2) {
            e = e2;
        }
    }

    private void removeMediaButtonReceiver_syncPrs(PendingIntent pi) {
        try {
            for (int index = this.mPRStack.size() - 1; index >= 0; index--) {
                PlayerRecord prse = this.mPRStack.elementAt(index);
                if (prse.hasMatchingMediaButtonIntent(pi)) {
                    prse.destroy();
                    this.mPRStack.removeElementAt(index);
                    return;
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "Wrong index accessing media button stack, lock error? ", e);
        }
    }

    private boolean isCurrentRcController(PendingIntent pi) {
        return !this.mPRStack.empty() && this.mPRStack.peek().hasMatchingMediaButtonIntent(pi);
    }

    private void setNewRcClientOnDisplays_syncRcsCurrc(int newClientGeneration, PendingIntent newMediaIntent, boolean clearing) {
        synchronized (this.mPRStack) {
            if (this.mRcDisplays.size() > 0) {
                Iterator<DisplayInfoForServer> displayIterator = this.mRcDisplays.iterator();
                while (displayIterator.hasNext()) {
                    DisplayInfoForServer di = displayIterator.next();
                    try {
                        di.mRcDisplay.setCurrentClientId(newClientGeneration, newMediaIntent, clearing);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Dead display in setNewRcClientOnDisplays_syncRcsCurrc()", e);
                        di.release();
                        displayIterator.remove();
                    }
                }
            }
        }
    }

    private void setNewRcClientGenerationOnClients_syncRcsCurrc(int newClientGeneration) {
        Iterator<PlayerRecord> stackIterator = this.mPRStack.iterator();
        while (stackIterator.hasNext()) {
            PlayerRecord se = stackIterator.next();
            if (se != null && se.getRcc() != null) {
                try {
                    se.getRcc().setCurrentClientGenerationId(newClientGeneration);
                } catch (RemoteException e) {
                    Log.w(TAG, "Dead client in setNewRcClientGenerationOnClients_syncRcsCurrc()", e);
                    stackIterator.remove();
                    se.unlinkToRcClientDeath();
                }
            }
        }
    }

    private void setNewRcClient_syncRcsCurrc(int newClientGeneration, PendingIntent newMediaIntent, boolean clearing) {
        setNewRcClientOnDisplays_syncRcsCurrc(newClientGeneration, newMediaIntent, clearing);
        setNewRcClientGenerationOnClients_syncRcsCurrc(newClientGeneration);
    }

    private void onRcDisplayClear() {
        synchronized (this.mPRStack) {
            synchronized (this.mCurrentRcLock) {
                this.mCurrentRcClientGen++;
                setNewRcClient_syncRcsCurrc(this.mCurrentRcClientGen, null, true);
            }
        }
    }

    private void onRcDisplayUpdate(PlayerRecord prse, int flags) {
        synchronized (this.mPRStack) {
            synchronized (this.mCurrentRcLock) {
                if (this.mCurrentRcClient != null && this.mCurrentRcClient.equals(prse.getRcc())) {
                    this.mCurrentRcClientGen++;
                    setNewRcClient_syncRcsCurrc(this.mCurrentRcClientGen, prse.getMediaButtonIntent(), false);
                    try {
                        this.mCurrentRcClient.onInformationRequested(this.mCurrentRcClientGen, flags);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Current valid remote client is dead: " + e);
                        this.mCurrentRcClient = null;
                    }
                }
            }
        }
    }

    private void onRcDisplayInitInfo(IRemoteControlDisplay newRcd, int w, int h) {
        synchronized (this.mPRStack) {
            synchronized (this.mCurrentRcLock) {
                if (this.mCurrentRcClient != null) {
                    try {
                        newRcd.setCurrentClientId(this.mCurrentRcClientGen, this.mCurrentRcClientIntent, false);
                        try {
                            this.mCurrentRcClient.informationRequestForDisplay(newRcd, w, h);
                        } catch (RemoteException e) {
                            Log.e(TAG, "Current valid remote client is dead: ", e);
                            this.mCurrentRcClient = null;
                        }
                    } catch (RemoteException e2) {
                        Log.e(TAG, "Dead display in onRcDisplayInitInfo()", e2);
                    }
                }
            }
        }
    }

    private void clearRemoteControlDisplay_syncPrs() {
        synchronized (this.mCurrentRcLock) {
            this.mCurrentRcClient = null;
        }
        this.mEventHandler.sendMessage(this.mEventHandler.obtainMessage(1));
    }

    private void updateRemoteControlDisplay_syncPrs(int infoChangedFlags) {
        PlayerRecord prse = this.mPRStack.peek();
        int infoFlagsAboutToBeUsed = infoChangedFlags;
        if (prse.getRcc() == null) {
            clearRemoteControlDisplay_syncPrs();
            return;
        }
        synchronized (this.mCurrentRcLock) {
            if (!prse.getRcc().equals(this.mCurrentRcClient)) {
                infoFlagsAboutToBeUsed = 15;
            }
            this.mCurrentRcClient = prse.getRcc();
            this.mCurrentRcClientIntent = prse.getMediaButtonIntent();
        }
        this.mEventHandler.sendMessage(this.mEventHandler.obtainMessage(2, infoFlagsAboutToBeUsed, 0, prse));
    }

    private void checkUpdateRemoteControlDisplay_syncPrs(int infoChangedFlags) {
        if (this.mPRStack.isEmpty()) {
            clearRemoteControlDisplay_syncPrs();
        } else {
            updateRemoteControlDisplay_syncPrs(infoChangedFlags);
        }
    }

    protected void registerMediaButtonIntent(PendingIntent mediaIntent, ComponentName eventReceiver, IBinder token) {
        Log.i(TAG, "  Remote Control   registerMediaButtonIntent() for " + mediaIntent);
        synchronized (this.mPRStack) {
            if (pushMediaButtonReceiver_syncPrs(mediaIntent, eventReceiver, token)) {
                checkUpdateRemoteControlDisplay_syncPrs(15);
            }
        }
    }

    protected void unregisterMediaButtonIntent(PendingIntent mediaIntent) {
        Log.i(TAG, "  Remote Control   unregisterMediaButtonIntent() for " + mediaIntent);
        synchronized (this.mPRStack) {
            boolean topOfStackWillChange = isCurrentRcController(mediaIntent);
            removeMediaButtonReceiver_syncPrs(mediaIntent);
            if (topOfStackWillChange) {
                checkUpdateRemoteControlDisplay_syncPrs(15);
            }
        }
    }

    protected void unregisterMediaButtonIntentAsync(PendingIntent mediaIntent) {
        this.mEventHandler.sendMessage(this.mEventHandler.obtainMessage(11, 0, 0, mediaIntent));
    }

    protected void registerMediaButtonEventReceiverForCalls(ComponentName c) {
        if (this.mContext.checkCallingPermission(Manifest.permission.MODIFY_PHONE_STATE) != 0) {
            Log.e(TAG, "Invalid permissions to register media button receiver for calls");
            return;
        }
        synchronized (this.mPRStack) {
            this.mMediaReceiverForCalls = c;
        }
    }

    protected void unregisterMediaButtonEventReceiverForCalls() {
        if (this.mContext.checkCallingPermission(Manifest.permission.MODIFY_PHONE_STATE) != 0) {
            Log.e(TAG, "Invalid permissions to unregister media button receiver for calls");
            return;
        }
        synchronized (this.mPRStack) {
            this.mMediaReceiverForCalls = null;
        }
    }

    protected int registerRemoteControlClient(PendingIntent mediaIntent, IRemoteControlClient rcClient, String callingPackageName) {
        int rccId = -1;
        synchronized (this.mPRStack) {
            try {
                int index = this.mPRStack.size() - 1;
                while (true) {
                    if (index < 0) {
                        break;
                    }
                    PlayerRecord prse = this.mPRStack.elementAt(index);
                    if (prse.hasMatchingMediaButtonIntent(mediaIntent)) {
                        break;
                    }
                    index--;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "Wrong index accessing RC stack, lock error? ", e);
            }
            if (isCurrentRcController(mediaIntent)) {
                checkUpdateRemoteControlDisplay_syncPrs(15);
            }
        }
        return rccId;
    }

    protected void unregisterRemoteControlClient(PendingIntent mediaIntent, IRemoteControlClient rcClient) {
        synchronized (this.mPRStack) {
            boolean topRccChange = false;
            try {
                int index = this.mPRStack.size() - 1;
                while (true) {
                    if (index < 0) {
                        break;
                    }
                    PlayerRecord prse = this.mPRStack.elementAt(index);
                    if (prse.hasMatchingMediaButtonIntent(mediaIntent) && rcClient.equals(prse.getRcc())) {
                        break;
                    } else {
                        index--;
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "Wrong index accessing RC stack, lock error? ", e);
            }
            if (topRccChange) {
                checkUpdateRemoteControlDisplay_syncPrs(15);
            }
        }
    }

    private class DisplayInfoForServer implements IBinder.DeathRecipient {
        private int mArtworkExpectedHeight;
        private int mArtworkExpectedWidth;
        private ComponentName mClientNotifListComp;
        private final IRemoteControlDisplay mRcDisplay;
        private final IBinder mRcDisplayBinder;
        private boolean mWantsPositionSync = false;
        private boolean mEnabled = true;

        public DisplayInfoForServer(IRemoteControlDisplay rcd, int w, int h) {
            this.mArtworkExpectedWidth = -1;
            this.mArtworkExpectedHeight = -1;
            this.mRcDisplay = rcd;
            this.mRcDisplayBinder = rcd.asBinder();
            this.mArtworkExpectedWidth = w;
            this.mArtworkExpectedHeight = h;
        }

        public boolean init() {
            try {
                this.mRcDisplayBinder.linkToDeath(this, 0);
                return true;
            } catch (RemoteException e) {
                Log.w(MediaFocusControl.TAG, "registerRemoteControlDisplay() has a dead client " + this.mRcDisplayBinder);
                return false;
            }
        }

        public void release() {
            try {
                this.mRcDisplayBinder.unlinkToDeath(this, 0);
            } catch (NoSuchElementException e) {
                Log.e(MediaFocusControl.TAG, "Error in DisplaInfoForServer.relase()", e);
            }
        }

        @Override
        public void binderDied() {
            synchronized (MediaFocusControl.this.mPRStack) {
                Log.w(MediaFocusControl.TAG, "RemoteControl: display " + this.mRcDisplay + " died");
                Iterator<DisplayInfoForServer> displayIterator = MediaFocusControl.this.mRcDisplays.iterator();
                while (displayIterator.hasNext()) {
                    DisplayInfoForServer di = displayIterator.next();
                    if (di.mRcDisplay == this.mRcDisplay) {
                        displayIterator.remove();
                        return;
                    }
                }
            }
        }
    }

    private void plugRemoteControlDisplaysIntoClient_syncPrs(IRemoteControlClient rcc) {
        for (DisplayInfoForServer di : this.mRcDisplays) {
            try {
                rcc.plugRemoteControlDisplay(di.mRcDisplay, di.mArtworkExpectedWidth, di.mArtworkExpectedHeight);
                if (di.mWantsPositionSync) {
                    rcc.setWantsSyncForDisplay(di.mRcDisplay, true);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Error connecting RCD to RCC in RCC registration", e);
            }
        }
    }

    private void enableRemoteControlDisplayForClient_syncRcStack(IRemoteControlDisplay rcd, boolean enabled) {
        for (PlayerRecord prse : this.mPRStack) {
            if (prse.getRcc() != null) {
                try {
                    prse.getRcc().enableRemoteControlDisplay(rcd, enabled);
                } catch (RemoteException e) {
                    Log.e(TAG, "Error connecting RCD to client: ", e);
                }
            }
        }
    }

    private boolean rcDisplayIsPluggedIn_syncRcStack(IRemoteControlDisplay rcd) {
        for (DisplayInfoForServer di : this.mRcDisplays) {
            if (di.mRcDisplay.asBinder().equals(rcd.asBinder())) {
                return true;
            }
        }
        return false;
    }

    private void registerRemoteControlDisplay_int(IRemoteControlDisplay rcd, int w, int h, ComponentName listenerComp) {
        synchronized (mAudioFocusLock) {
            synchronized (this.mPRStack) {
                if (rcd != null) {
                    if (!rcDisplayIsPluggedIn_syncRcStack(rcd)) {
                        DisplayInfoForServer di = new DisplayInfoForServer(rcd, w, h);
                        di.mEnabled = true;
                        di.mClientNotifListComp = listenerComp;
                        if (di.init()) {
                            this.mRcDisplays.add(di);
                            for (PlayerRecord prse : this.mPRStack) {
                                if (prse.getRcc() != null) {
                                    try {
                                        prse.getRcc().plugRemoteControlDisplay(rcd, w, h);
                                    } catch (RemoteException e) {
                                        Log.e(TAG, "Error connecting RCD to client: ", e);
                                    }
                                }
                            }
                            sendMsg(this.mEventHandler, 9, 2, w, h, rcd, 0);
                        }
                    }
                }
            }
        }
    }

    protected void unregisterRemoteControlDisplay(IRemoteControlDisplay rcd) {
        synchronized (this.mPRStack) {
            if (rcd != null) {
                boolean displayWasPluggedIn = false;
                Iterator<DisplayInfoForServer> displayIterator = this.mRcDisplays.iterator();
                while (displayIterator.hasNext() && !displayWasPluggedIn) {
                    DisplayInfoForServer di = displayIterator.next();
                    if (di.mRcDisplay.asBinder().equals(rcd.asBinder())) {
                        displayWasPluggedIn = true;
                        di.release();
                        displayIterator.remove();
                    }
                }
                if (displayWasPluggedIn) {
                    for (PlayerRecord prse : this.mPRStack) {
                        if (prse.getRcc() != null) {
                            try {
                                prse.getRcc().unplugRemoteControlDisplay(rcd);
                            } catch (RemoteException e) {
                                Log.e(TAG, "Error disconnecting remote control display to client: ", e);
                            }
                        }
                    }
                }
            }
        }
    }

    protected void remoteControlDisplayUsesBitmapSize(IRemoteControlDisplay rcd, int w, int h) {
        synchronized (this.mPRStack) {
            Iterator<DisplayInfoForServer> displayIterator = this.mRcDisplays.iterator();
            boolean artworkSizeUpdate = false;
            while (displayIterator.hasNext() && !artworkSizeUpdate) {
                DisplayInfoForServer di = displayIterator.next();
                if (di.mRcDisplay.asBinder().equals(rcd.asBinder()) && (di.mArtworkExpectedWidth != w || di.mArtworkExpectedHeight != h)) {
                    di.mArtworkExpectedWidth = w;
                    di.mArtworkExpectedHeight = h;
                    artworkSizeUpdate = true;
                }
            }
            if (artworkSizeUpdate) {
                for (PlayerRecord prse : this.mPRStack) {
                    if (prse.getRcc() != null) {
                        try {
                            prse.getRcc().setBitmapSizeForDisplay(rcd, w, h);
                        } catch (RemoteException e) {
                            Log.e(TAG, "Error setting bitmap size for RCD on RCC: ", e);
                        }
                    }
                }
            }
        }
    }

    protected void remoteControlDisplayWantsPlaybackPositionSync(IRemoteControlDisplay rcd, boolean wantsSync) {
        synchronized (this.mPRStack) {
            boolean rcdRegistered = false;
            Iterator<DisplayInfoForServer> displayIterator = this.mRcDisplays.iterator();
            while (true) {
                if (!displayIterator.hasNext()) {
                    break;
                }
                DisplayInfoForServer di = displayIterator.next();
                if (di.mRcDisplay.asBinder().equals(rcd.asBinder())) {
                    di.mWantsPositionSync = wantsSync;
                    rcdRegistered = true;
                    break;
                }
            }
            if (rcdRegistered) {
                for (PlayerRecord prse : this.mPRStack) {
                    if (prse.getRcc() != null) {
                        try {
                            prse.getRcc().setWantsSyncForDisplay(rcd, wantsSync);
                        } catch (RemoteException e) {
                            Log.e(TAG, "Error setting position sync flag for RCD on RCC: ", e);
                        }
                    }
                }
            }
        }
    }

    private void onRegisterVolumeObserverForRcc(int rccId, IRemoteVolumeObserver rvo) {
        synchronized (this.mPRStack) {
            try {
                int index = this.mPRStack.size() - 1;
                while (true) {
                    if (index < 0) {
                        break;
                    }
                    PlayerRecord prse = this.mPRStack.elementAt(index);
                    if (prse.getRccId() == rccId) {
                        break;
                    } else {
                        index--;
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "Wrong index accessing media button stack, lock error? ", e);
            }
        }
    }

    protected boolean checkUpdateRemoteStateIfActive(int streamType) {
        synchronized (this.mPRStack) {
            try {
                for (int index = this.mPRStack.size() - 1; index >= 0; index--) {
                    PlayerRecord prse = this.mPRStack.elementAt(index);
                    if (prse.mPlaybackType == 1 && isPlaystateActive(prse.mPlaybackState.mState) && prse.mPlaybackStream == streamType) {
                        synchronized (this.mMainRemote) {
                            this.mMainRemote.mRccId = prse.getRccId();
                            this.mMainRemote.mVolume = prse.mPlaybackVolume;
                            this.mMainRemote.mVolumeMax = prse.mPlaybackVolumeMax;
                            this.mMainRemote.mVolumeHandling = prse.mPlaybackVolumeHandling;
                            this.mMainRemoteIsActive = true;
                        }
                        return true;
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "Wrong index accessing RC stack, lock error? ", e);
            }
            synchronized (this.mMainRemote) {
                this.mMainRemoteIsActive = false;
            }
            return false;
        }
    }

    protected static boolean isPlaystateActive(int playState) {
        switch (playState) {
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                return true;
            default:
                return false;
        }
    }

    private void sendVolumeUpdateToRemote(int rccId, int direction) {
        if (direction != 0) {
            IRemoteVolumeObserver rvo = null;
            synchronized (this.mPRStack) {
                try {
                    int index = this.mPRStack.size() - 1;
                    while (true) {
                        if (index < 0) {
                            break;
                        }
                        PlayerRecord prse = this.mPRStack.elementAt(index);
                        if (prse.getRccId() == rccId) {
                            break;
                        } else {
                            index--;
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.e(TAG, "Wrong index accessing media button stack, lock error? ", e);
                }
            }
            if (rvo != null) {
                try {
                    rvo.dispatchRemoteVolumeUpdate(direction, -1);
                } catch (RemoteException e2) {
                    Log.e(TAG, "Error dispatching relative volume update", e2);
                }
            }
        }
    }

    protected int getRemoteStreamMaxVolume() {
        int i;
        synchronized (this.mMainRemote) {
            i = this.mMainRemote.mRccId == -1 ? 0 : this.mMainRemote.mVolumeMax;
        }
        return i;
    }

    protected int getRemoteStreamVolume() {
        int i;
        synchronized (this.mMainRemote) {
            i = this.mMainRemote.mRccId == -1 ? 0 : this.mMainRemote.mVolume;
        }
        return i;
    }

    protected void setRemoteStreamVolume(int vol) {
        synchronized (this.mMainRemote) {
            if (this.mMainRemote.mRccId != -1) {
                int rccId = this.mMainRemote.mRccId;
                IRemoteVolumeObserver rvo = null;
                synchronized (this.mPRStack) {
                    try {
                        int index = this.mPRStack.size() - 1;
                        while (true) {
                            if (index < 0) {
                                break;
                            }
                            PlayerRecord prse = this.mPRStack.elementAt(index);
                            if (prse.getRccId() == rccId) {
                                break;
                            } else {
                                index--;
                            }
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        Log.e(TAG, "Wrong index accessing media button stack, lock error? ", e);
                    }
                }
                if (rvo != null) {
                    try {
                        rvo.dispatchRemoteVolumeUpdate(0, vol);
                    } catch (RemoteException e2) {
                        Log.e(TAG, "Error dispatching absolute volume update", e2);
                    }
                }
            }
        }
    }

    protected void postReevaluateRemote() {
        sendMsg(this.mEventHandler, 3, 2, 0, 0, null, 0);
    }

    private void onReevaluateRemote() {
    }
}
