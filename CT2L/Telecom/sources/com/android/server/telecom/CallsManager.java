package com.android.server.telecom;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Trace;
import android.provider.Settings;
import android.telecom.AudioState;
import android.telecom.CallState;
import android.telecom.Conference;
import android.telecom.DisconnectCause;
import android.telecom.GatewayInfo;
import android.telecom.ParcelableConference;
import android.telecom.ParcelableConnection;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.Dsds;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.telecom.Call;
import com.android.server.telecom.InCallTonePlayer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CallsManager extends Call.ListenerBase {
    private final CallAudioManager mCallAudioManager;
    private final CallLogManager mCallLogManager;
    private final ConnectionServiceRepository mConnectionServiceRepository;
    private final Context mContext;
    private final DtmfLocalTonePlayer mDtmfLocalTonePlayer;
    private Call mForegroundCall;
    private final HeadsetMediaButton mHeadsetMediaButton;
    private final InCallController mInCallController;
    private final InCallWakeLockController mInCallWakeLockController;
    private final MissedCallNotifier mMissedCallNotifier;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;
    private final PhoneStateBroadcaster mPhoneStateBroadcaster;
    private final ProximitySensorManager mProximitySensorManager;
    private final Ringer mRinger;
    private Runnable mStopTone;
    private final TtyManager mTtyManager;
    private final WiredHeadsetManager mWiredHeadsetManager;
    private static CallsManager sInstance = null;
    private static final int[] OUTGOING_CALL_STATES = {1, 2, 3};
    protected static final int[] LIVE_CALL_STATES = {1, 2, 3, 5};
    private final Set<Call> mCalls = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));
    private final Set<CallsManagerListener> mListeners = Collections.newSetFromMap(new ConcurrentHashMap(16, 0.9f, 1));
    private final Set<Call> mLocallyDisconnectingCalls = new HashSet();
    private final Set<Call> mPendingCallsToDisconnect = new HashSet();
    private final Handler mHandler = new Handler();
    private boolean mCanAddCall = true;

    interface CallsManagerListener {
        void onAudioStateChanged(AudioState audioState, AudioState audioState2);

        void onCallAdded(Call call);

        void onCallRemoved(Call call);

        void onCallStateChanged(Call call, int i, int i2);

        void onCanAddCallChanged(boolean z);

        void onConnectionServiceChanged(Call call, ConnectionServiceWrapper connectionServiceWrapper, ConnectionServiceWrapper connectionServiceWrapper2);

        void onForegroundCallChanged(Call call, Call call2);

        void onIncomingCallAnswered(Call call);

        void onIncomingCallRejected(Call call, boolean z, String str);

        void onIsConferencedChanged(Call call);

        void onIsVoipAudioModeChanged(Call call);

        void onRingbackRequested(Call call, boolean z);

        void onVideoStateChanged(Call call);
    }

    @Override
    public void onCallerDisplayNameChanged(Call call) {
        super.onCallerDisplayNameChanged(call);
    }

    @Override
    public void onCallerInfoChanged(Call call) {
        super.onCallerInfoChanged(call);
    }

    @Override
    public void onCannedSmsResponsesLoaded(Call call) {
        super.onCannedSmsResponsesLoaded(call);
    }

    @Override
    public void onConferenceableCallsChanged(Call call) {
        super.onConferenceableCallsChanged(call);
    }

    @Override
    public void onConnectionCapabilitiesChanged(Call call) {
        super.onConnectionCapabilitiesChanged(call);
    }

    @Override
    public void onConnectionManagerPhoneAccountChanged(Call call) {
        super.onConnectionManagerPhoneAccountChanged(call);
    }

    @Override
    public void onHandleChanged(Call call) {
        super.onHandleChanged(call);
    }

    @Override
    public void onStatusHintsChanged(Call call) {
        super.onStatusHintsChanged(call);
    }

    @Override
    public void onTargetPhoneAccountChanged(Call call) {
        super.onTargetPhoneAccountChanged(call);
    }

    @Override
    public void onVideoCallProviderChanged(Call call) {
        super.onVideoCallProviderChanged(call);
    }

    static CallsManager getInstance() {
        return sInstance;
    }

    static void initialize(CallsManager callsManager) {
        sInstance = callsManager;
    }

    CallsManager(Context context, MissedCallNotifier missedCallNotifier, PhoneAccountRegistrar phoneAccountRegistrar) {
        this.mContext = context;
        this.mPhoneAccountRegistrar = phoneAccountRegistrar;
        this.mMissedCallNotifier = missedCallNotifier;
        StatusBarNotifier statusBarNotifier = new StatusBarNotifier(context, this);
        this.mWiredHeadsetManager = new WiredHeadsetManager(context);
        this.mCallAudioManager = new CallAudioManager(context, statusBarNotifier, this.mWiredHeadsetManager);
        InCallTonePlayer.Factory factory = new InCallTonePlayer.Factory(this.mCallAudioManager);
        this.mRinger = new Ringer(this.mCallAudioManager, this, factory, context);
        this.mHeadsetMediaButton = new HeadsetMediaButton(context, this);
        this.mTtyManager = new TtyManager(context, this.mWiredHeadsetManager);
        this.mProximitySensorManager = new ProximitySensorManager(context);
        this.mPhoneStateBroadcaster = new PhoneStateBroadcaster();
        this.mCallLogManager = new CallLogManager(context);
        this.mInCallController = new InCallController(context);
        this.mDtmfLocalTonePlayer = new DtmfLocalTonePlayer(context);
        this.mConnectionServiceRepository = new ConnectionServiceRepository(this.mPhoneAccountRegistrar, context);
        this.mInCallWakeLockController = new InCallWakeLockController(context, this);
        this.mListeners.add(statusBarNotifier);
        this.mListeners.add(this.mCallLogManager);
        this.mListeners.add(this.mPhoneStateBroadcaster);
        this.mListeners.add(this.mInCallController);
        this.mListeners.add(this.mRinger);
        this.mListeners.add(new RingbackPlayer(this, factory));
        this.mListeners.add(new InCallToneMonitor(factory, this));
        this.mListeners.add(this.mCallAudioManager);
        this.mListeners.add(missedCallNotifier);
        this.mListeners.add(this.mDtmfLocalTonePlayer);
        this.mListeners.add(this.mHeadsetMediaButton);
        this.mListeners.add(RespondViaSmsManager.getInstance());
        this.mListeners.add(this.mProximitySensorManager);
    }

    @Override
    public void onSuccessfulOutgoingCall(Call call, int i) {
        Log.v(this, "onSuccessfulOutgoingCall, %s", call);
        setCallState(call, i);
        if (!this.mCalls.contains(call)) {
            addCall(call);
        }
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onConnectionServiceChanged(call, null, call.getConnectionService());
        }
        markCallAsDialing(call);
    }

    @Override
    public void onFailedOutgoingCall(Call call, DisconnectCause disconnectCause) {
        Log.v(this, "onFailedOutgoingCall, call: %s", call);
        markCallAsRemoved(call);
    }

    @Override
    public void onSuccessfulIncomingCall(Call call) {
        Log.d(this, "onSuccessfulIncomingCall", new Object[0]);
        setCallState(call, 4);
        if (hasMaximumRingingCalls()) {
            call.reject(false, null);
            this.mMissedCallNotifier.showMissedCallNotification(call);
            this.mCallLogManager.logCall(call, 3);
            return;
        }
        addCall(call);
    }

    @Override
    public void onFailedIncomingCall(Call call) {
        setCallState(call, 7);
        call.removeListener(this);
    }

    @Override
    public void onSuccessfulUnknownCall(Call call, int i) {
        setCallState(call, i);
        Log.i(this, "onSuccessfulUnknownCall for call %s", call);
        addCall(call);
    }

    @Override
    public void onFailedUnknownCall(Call call) {
        Log.i(this, "onFailedUnknownCall for call %s", call);
        setCallState(call, 7);
        call.removeListener(this);
    }

    @Override
    public void onRingbackRequested(Call call, boolean z) {
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onRingbackRequested(call, z);
        }
    }

    @Override
    public void onPostDialWait(Call call, String str) {
        this.mInCallController.onPostDialWait(call, str);
    }

    @Override
    public void onPostDialChar(final Call call, char c) {
        if (PhoneNumberUtils.is12Key(c)) {
            if (this.mStopTone != null) {
                this.mHandler.removeCallbacks(this.mStopTone);
            }
            this.mDtmfLocalTonePlayer.playTone(call, c);
            this.mStopTone = new Runnable() {
                @Override
                public void run() {
                    CallsManager.this.mDtmfLocalTonePlayer.stopTone(call);
                }
            };
            this.mHandler.postDelayed(this.mStopTone, Timeouts.getDelayBetweenDtmfTonesMillis(this.mContext.getContentResolver()));
            return;
        }
        if (c == 0 || c == ';' || c == ',') {
            if (this.mStopTone != null) {
                this.mHandler.removeCallbacks(this.mStopTone);
            }
            this.mDtmfLocalTonePlayer.stopTone(call);
            return;
        }
        Log.w(this, "onPostDialChar: invalid value %d", Character.valueOf(c));
    }

    @Override
    public void onParentChanged(Call call) {
        updateCallsManagerState();
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onIsConferencedChanged(call);
        }
    }

    @Override
    public void onChildrenChanged(Call call) {
        updateCallsManagerState();
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onIsConferencedChanged(call);
        }
    }

    @Override
    public void onIsVoipAudioModeChanged(Call call) {
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onIsVoipAudioModeChanged(call);
        }
    }

    @Override
    public void onVideoStateChanged(Call call) {
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onVideoStateChanged(call);
        }
    }

    @Override
    public boolean onCanceledViaNewOutgoingCallBroadcast(final Call call) {
        this.mPendingCallsToDisconnect.add(call);
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (CallsManager.this.mPendingCallsToDisconnect.remove(call)) {
                    Log.i(this, "Delayed disconnection of call: %s", call);
                    call.disconnect();
                }
            }
        }, Timeouts.getNewOutgoingCallCancelMillis(this.mContext.getContentResolver()));
        return true;
    }

    Collection<Call> getCalls() {
        return Collections.unmodifiableCollection(this.mCalls);
    }

    Call getForegroundCall() {
        return this.mForegroundCall;
    }

    Ringer getRinger() {
        return this.mRinger;
    }

    InCallController getInCallController() {
        return this.mInCallController;
    }

    boolean hasEmergencyCall() {
        Iterator<Call> it = this.mCalls.iterator();
        while (it.hasNext()) {
            if (it.next().isEmergencyCall()) {
                return true;
            }
        }
        return false;
    }

    boolean hasVideoCall() {
        Iterator<Call> it = this.mCalls.iterator();
        while (it.hasNext()) {
            if (it.next().getVideoState() != 0) {
                return true;
            }
        }
        return false;
    }

    AudioState getAudioState() {
        return this.mCallAudioManager.getAudioState();
    }

    boolean isTtySupported() {
        return this.mTtyManager.isTtySupported();
    }

    int getCurrentTtyMode() {
        return this.mTtyManager.getCurrentTtyMode();
    }

    void addListener(CallsManagerListener callsManagerListener) {
        this.mListeners.add(callsManagerListener);
    }

    void removeListener(CallsManagerListener callsManagerListener) {
        this.mListeners.remove(callsManagerListener);
    }

    void processIncomingCallIntent(PhoneAccountHandle phoneAccountHandle, Bundle bundle) {
        Log.d(this, "processIncomingCallIntent", new Object[0]);
        Call call = new Call(this.mContext, this.mConnectionServiceRepository, (Uri) bundle.getParcelable("incoming_number"), null, null, phoneAccountHandle, true, false);
        call.setExtras(bundle);
        call.addListener(this);
        call.startCreateConnection(this.mPhoneAccountRegistrar);
    }

    void addNewUnknownCall(PhoneAccountHandle phoneAccountHandle, Bundle bundle) {
        Uri uri = (Uri) bundle.getParcelable("android.telecom.extra.UNKNOWN_CALL_HANDLE");
        Log.i(this, "addNewUnknownCall with handle: %s", Log.pii(uri));
        Call call = new Call(this.mContext, this.mConnectionServiceRepository, uri, null, null, phoneAccountHandle, true, false);
        call.setIsUnknown(true);
        call.setExtras(bundle);
        call.addListener(this);
        call.startCreateConnection(this.mPhoneAccountRegistrar);
    }

    private Call getNewOutgoingCall(Uri uri) {
        Call call = null;
        for (Call call2 : this.mPendingCallsToDisconnect) {
            if (call == null && Objects.equals(call2.getHandle(), uri)) {
                this.mPendingCallsToDisconnect.remove(call2);
                Log.i(this, "Reusing disconnected call %s", call2);
            } else {
                call2.disconnect();
                call2 = call;
            }
            call = call2;
        }
        return call != null ? call : new Call(this.mContext, this.mConnectionServiceRepository, uri, null, null, null, false, false);
    }

    Call startOutgoingCall(Uri uri, PhoneAccountHandle phoneAccountHandle, Bundle bundle, int i) {
        PhoneAccountHandle defaultOutgoingPhoneAccount;
        PhoneAccountHandle targetPhoneAccount;
        Call newOutgoingCall = getNewOutgoingCall(uri);
        List<PhoneAccountHandle> callCapablePhoneAccounts = this.mPhoneAccountRegistrar.getCallCapablePhoneAccounts(uri.getScheme());
        Log.v(this, "startOutgoingCall found accounts = " + callCapablePhoneAccounts, new Object[0]);
        PhoneAccountHandle targetPhoneAccount2 = (this.mForegroundCall == null || this.mForegroundCall.getTargetPhoneAccount() == null) ? phoneAccountHandle : this.mForegroundCall.getTargetPhoneAccount();
        PhoneAccountHandle phoneAccountHandle2 = (targetPhoneAccount2 == null || callCapablePhoneAccounts.contains(targetPhoneAccount2)) ? targetPhoneAccount2 : null;
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        if (phoneAccountHandle2 == null && telephonyManager.getMultiSimConfiguration() == TelephonyManager.MultiSimVariants.DSDS) {
            Call firstCallWithState = getFirstCallWithState(newOutgoingCall, LIVE_CALL_STATES);
            if (firstCallWithState == null) {
                firstCallWithState = getFirstCallWithState(newOutgoingCall, 6);
            }
            if (firstCallWithState != null && (targetPhoneAccount = firstCallWithState.getTargetPhoneAccount()) != null) {
                phoneAccountHandle2 = targetPhoneAccount;
            }
            if (phoneAccountHandle2 == null && i == 3 && Dsds.hasTwoIcc()) {
                int[] subId = SubscriptionManager.getSubId(Dsds.isSim2Master() ? PhoneConstants.SimId.SIM2.ordinal() : PhoneConstants.SimId.SIM1.ordinal());
                if (subId != null && SubscriptionManager.isValidSubscriptionId(subId[0])) {
                    Iterator<PhoneAccountHandle> it = callCapablePhoneAccounts.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        PhoneAccountHandle next = it.next();
                        if (TelephonyUtil.isPstnComponentName(next.getComponentName())) {
                            try {
                                if (subId[0] == Integer.parseInt(next.getId().replaceAll("\\D+", ""))) {
                                    phoneAccountHandle2 = next;
                                    break;
                                }
                            } catch (NumberFormatException e) {
                                Log.w(this, "number format is wrong. " + e.getMessage(), new Object[0]);
                            }
                        }
                    }
                }
            }
        }
        if (phoneAccountHandle2 != null || (defaultOutgoingPhoneAccount = this.mPhoneAccountRegistrar.getDefaultOutgoingPhoneAccount(uri.getScheme())) == null) {
            defaultOutgoingPhoneAccount = phoneAccountHandle2;
        }
        newOutgoingCall.setTargetPhoneAccount(defaultOutgoingPhoneAccount);
        boolean zShouldProcessAsEmergency = TelephonyUtil.shouldProcessAsEmergency(this.mContext, newOutgoingCall.getHandle());
        boolean zIsPotentialInCallMMICode = isPotentialInCallMMICode(uri);
        if (!zIsPotentialInCallMMICode && !makeRoomForOutgoingCall(newOutgoingCall, zShouldProcessAsEmergency)) {
            Log.i(this, "No remaining room for outgoing call: %s", newOutgoingCall);
            if (this.mCalls.contains(newOutgoingCall)) {
                newOutgoingCall.disconnect();
            }
            return null;
        }
        boolean z = defaultOutgoingPhoneAccount == null && callCapablePhoneAccounts.size() > 1 && !zShouldProcessAsEmergency;
        if (z) {
            newOutgoingCall.setState(2);
            bundle.putParcelableList("selectPhoneAccountAccounts", callCapablePhoneAccounts);
        } else {
            newOutgoingCall.setState(1);
        }
        newOutgoingCall.setExtras(bundle);
        if ((isPotentialMMICode(uri) || zIsPotentialInCallMMICode) && !z) {
            newOutgoingCall.addListener(this);
        } else if (!this.mCalls.contains(newOutgoingCall)) {
            addCall(newOutgoingCall);
        }
        return newOutgoingCall;
    }

    void placeOutgoingCall(Call call, Uri uri, GatewayInfo gatewayInfo, boolean z, int i) {
        if (call == null) {
            Log.i(this, "Canceling unknown call.", new Object[0]);
            return;
        }
        Uri gatewayAddress = gatewayInfo == null ? uri : gatewayInfo.getGatewayAddress();
        if (gatewayInfo == null) {
            Log.i(this, "Creating a new outgoing call with handle: %s", Log.piiHandle(gatewayAddress));
        } else {
            Log.i(this, "Creating a new outgoing call with gateway handle: %s, original handle: %s", Log.pii(gatewayAddress), Log.pii(uri));
        }
        call.setHandle(gatewayAddress);
        call.setGatewayInfo(gatewayInfo);
        call.setStartWithSpeakerphoneOn(z);
        call.setVideoState(i);
        boolean zShouldProcessAsEmergency = TelephonyUtil.shouldProcessAsEmergency(this.mContext, call.getHandle());
        if (zShouldProcessAsEmergency) {
            call.setTargetPhoneAccount(null);
        }
        if (call.getTargetPhoneAccount() != null || zShouldProcessAsEmergency) {
            call.startCreateConnection(this.mPhoneAccountRegistrar);
        }
    }

    void conference(Call call, Call call2) {
        call.conferenceWith(call2);
    }

    void answerCall(Call call, int i) {
        if (!this.mCalls.contains(call)) {
            Log.i(this, "Request to answer a non-existent call %s", call);
            return;
        }
        if (this.mForegroundCall != null && this.mForegroundCall != call && (this.mForegroundCall.isActive() || this.mForegroundCall.getState() == 3)) {
            if ((this.mForegroundCall.getConnectionCapabilities() & 1) == 0) {
                if (this.mForegroundCall.getConnectionService() != call.getConnectionService()) {
                    this.mForegroundCall.disconnect();
                }
            } else {
                Call heldCall = getHeldCall();
                if (heldCall != null) {
                    Log.v(this, "Disconnecting held call %s before holding active call.", heldCall);
                    heldCall.disconnect();
                }
                Log.v(this, "Holding active/dialing call %s before answering incoming call %s.", this.mForegroundCall, call);
                this.mForegroundCall.hold();
            }
        }
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onIncomingCallAnswered(call);
        }
        call.answer(i);
    }

    void rejectCall(Call call, boolean z, String str) {
        if (!this.mCalls.contains(call)) {
            Log.i(this, "Request to reject a non-existent call %s", call);
            return;
        }
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onIncomingCallRejected(call, z, str);
        }
        call.reject(z, str);
    }

    void playDtmfTone(Call call, char c) {
        if (!this.mCalls.contains(call)) {
            Log.i(this, "Request to play DTMF in a non-existent call %s", call);
        } else {
            call.playDtmfTone(c);
            this.mDtmfLocalTonePlayer.playTone(call, c);
        }
    }

    void stopDtmfTone(Call call) {
        if (!this.mCalls.contains(call)) {
            Log.i(this, "Request to stop DTMF in a non-existent call %s", call);
        } else {
            call.stopDtmfTone();
            this.mDtmfLocalTonePlayer.stopTone(call);
        }
    }

    void postDialContinue(Call call, boolean z) {
        if (!this.mCalls.contains(call)) {
            Log.i(this, "Request to continue post-dial string in a non-existent call %s", call);
        } else {
            call.postDialContinue(z);
        }
    }

    void disconnectCall(Call call) {
        Log.v(this, "disconnectCall %s", call);
        if (!this.mCalls.contains(call)) {
            Log.w(this, "Unknown call (%s) asked to disconnect", call);
        } else {
            this.mLocallyDisconnectingCalls.add(call);
            call.disconnect();
        }
    }

    void disconnectAllCalls() {
        Log.v(this, "disconnectAllCalls", new Object[0]);
        Iterator<Call> it = this.mCalls.iterator();
        while (it.hasNext()) {
            disconnectCall(it.next());
        }
    }

    void holdCall(Call call) {
        if (!this.mCalls.contains(call)) {
            Log.w(this, "Unknown call (%s) asked to be put on hold", call);
        } else {
            Log.d(this, "Putting call on hold: (%s)", call);
            call.hold();
        }
    }

    void unholdCall(Call call) {
        if (!this.mCalls.contains(call)) {
            Log.w(this, "Unknown call (%s) asked to be removed from hold", call);
            return;
        }
        Log.d(this, "unholding call: (%s)", call);
        for (Call call2 : this.mCalls) {
            if (call2 != null && call2.isAlive() && call2 != call && call2.getParentCall() == null) {
                call2.hold();
            }
        }
        call.unhold();
    }

    void mute(boolean z) {
        this.mCallAudioManager.mute(z);
    }

    boolean extraVolume(boolean z) {
        return this.mCallAudioManager.extraVolume(z);
    }

    boolean record() {
        return this.mCallAudioManager.record();
    }

    void setAudioRoute(int i) {
        this.mCallAudioManager.setAudioRoute(i);
    }

    void turnOnProximitySensor() {
        this.mProximitySensorManager.turnOn();
    }

    void turnOffProximitySensor(boolean z) {
        this.mProximitySensorManager.turnOff(z);
    }

    void phoneAccountSelected(Call call, PhoneAccountHandle phoneAccountHandle, boolean z) {
        if (!this.mCalls.contains(call)) {
            Log.i(this, "Attempted to add account to unknown call %s", call);
            return;
        }
        call.setTargetPhoneAccount(phoneAccountHandle);
        if (makeRoomForOutgoingCall(call, false)) {
            call.startCreateConnection(this.mPhoneAccountRegistrar);
        } else {
            call.disconnect();
        }
        if (z) {
            this.mPhoneAccountRegistrar.setUserSelectedOutgoingPhoneAccount(phoneAccountHandle);
        }
    }

    void onAudioStateChanged(AudioState audioState, AudioState audioState2) {
        Log.v(this, "onAudioStateChanged, audioState: %s -> %s", audioState, audioState2);
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onAudioStateChanged(audioState, audioState2);
        }
    }

    void markCallAsRinging(Call call) {
        setCallState(call, 4);
    }

    void markCallAsDialing(Call call) {
        setCallState(call, 3);
    }

    void markCallAsActive(Call call) {
        setCallState(call, 5);
        if (call.getStartWithSpeakerphoneOn()) {
            setAudioRoute(8);
        }
    }

    void markCallAsOnHold(Call call) {
        setCallState(call, 6);
    }

    void markCallAsDisconnected(Call call, DisconnectCause disconnectCause) {
        call.setDisconnectCause(disconnectCause);
        setCallState(call, 7);
    }

    void markCallAsRemoved(Call call) {
        removeCall(call);
        if (this.mLocallyDisconnectingCalls.contains(call)) {
            this.mLocallyDisconnectingCalls.remove(call);
            if (this.mForegroundCall != null && this.mForegroundCall.getState() == 6) {
                this.mForegroundCall.unhold();
            }
        }
    }

    void handleConnectionServiceDeath(ConnectionServiceWrapper connectionServiceWrapper) {
        if (connectionServiceWrapper != null) {
            for (Call call : this.mCalls) {
                if (call.getConnectionService() == connectionServiceWrapper) {
                    if (call.getState() != 7) {
                        markCallAsDisconnected(call, new DisconnectCause(1));
                    }
                    markCallAsRemoved(call);
                }
            }
        }
    }

    boolean hasAnyCalls() {
        return !this.mCalls.isEmpty();
    }

    boolean hasRingingCall() {
        return getFirstCallWithState(4) != null;
    }

    boolean onMediaButton(int i) {
        if (hasAnyCalls()) {
            if (1 == i) {
                Call firstCallWithState = getFirstCallWithState(4);
                if (firstCallWithState == null) {
                    this.mCallAudioManager.toggleMute();
                    return true;
                }
                firstCallWithState.answer(firstCallWithState.getVideoState());
                return true;
            }
            if (2 == i) {
                Log.d(this, "handleHeadsetHook: longpress -> hangup", new Object[0]);
                Call firstCallWithState2 = getFirstCallWithState(4, 3, 5, 6);
                if (firstCallWithState2 != null) {
                    firstCallWithState2.disconnect();
                    return true;
                }
            }
        }
        return false;
    }

    boolean canAddCall() {
        if (!(Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0)) {
            Log.d("CallsManager", "Device not provisioned, canAddCall is false.", new Object[0]);
            return false;
        }
        if (getFirstCallWithState(OUTGOING_CALL_STATES) != null) {
            return false;
        }
        int i = 0;
        for (Call call : this.mCalls) {
            if (call.isEmergencyCall()) {
                return false;
            }
            int i2 = call.getParentCall() == null ? i + 1 : i;
            if (i2 >= 2) {
                return false;
            }
            i = i2;
        }
        return true;
    }

    Call getRingingCall() {
        return getFirstCallWithState(4);
    }

    Call getActiveCall() {
        return getFirstCallWithState(5);
    }

    Call getDialingCall() {
        return getFirstCallWithState(3);
    }

    Call getHeldCall() {
        return getFirstCallWithState(6);
    }

    int getNumHeldCalls() {
        int i = 0;
        Iterator<Call> it = this.mCalls.iterator();
        while (true) {
            int i2 = i;
            if (it.hasNext()) {
                Call next = it.next();
                if (next.getParentCall() == null && next.getState() == 6) {
                    i2++;
                }
                i = i2;
            } else {
                return i2;
            }
        }
    }

    Call getFirstCallWithState(int... iArr) {
        return getFirstCallWithState(null, iArr);
    }

    Call getFirstCallWithState(Call call, int... iArr) {
        for (int i : iArr) {
            if (this.mForegroundCall != null && this.mForegroundCall.getState() == i) {
                return this.mForegroundCall;
            }
            for (Call call2 : this.mCalls) {
                if (!Objects.equals(call, call2) && call2.getParentCall() == null && i == call2.getState()) {
                    return call2;
                }
            }
        }
        return null;
    }

    Call createConferenceCall(PhoneAccountHandle phoneAccountHandle, ParcelableConference parcelableConference) {
        Call call = new Call(this.mContext, this.mConnectionServiceRepository, null, null, null, phoneAccountHandle, false, true, parcelableConference.getConnectTimeMillis() == Conference.CONNECT_TIME_NOT_SPECIFIED ? 0L : parcelableConference.getConnectTimeMillis());
        setCallState(call, Call.getStateFromConnectionState(parcelableConference.getState()));
        call.setConnectionCapabilities(parcelableConference.getConnectionCapabilities());
        call.addListener(this);
        addCall(call);
        return call;
    }

    int getCallState() {
        return this.mPhoneStateBroadcaster.getCallState();
    }

    PhoneAccountRegistrar getPhoneAccountRegistrar() {
        return this.mPhoneAccountRegistrar;
    }

    MissedCallNotifier getMissedCallNotifier() {
        return this.mMissedCallNotifier;
    }

    private void addCall(Call call) {
        Trace.beginSection("addCall");
        Log.v(this, "addCall(%s)", call);
        call.addListener(this);
        this.mCalls.add(call);
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onCallAdded(call);
        }
        updateCallsManagerState();
        Trace.endSection();
    }

    private void removeCall(Call call) {
        boolean z = true;
        Trace.beginSection("removeCall");
        Log.v(this, "removeCall(%s)", call);
        call.setParentCall(null);
        call.removeListener(this);
        call.clearConnectionService();
        if (this.mCalls.contains(call)) {
            this.mCalls.remove(call);
        } else {
            z = false;
        }
        if (z) {
            Iterator<CallsManagerListener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onCallRemoved(call);
            }
            updateCallsManagerState();
        }
        Trace.endSection();
    }

    private boolean isValidNewState(int i, int i2) {
        if (i != 5 || i2 != 4) {
            return true;
        }
        Log.w("Discard invalid call state change. %s -> %s", CallState.toString(i), CallState.toString(i2));
        return false;
    }

    private void setCallState(Call call, int i) {
        if (call != null) {
            int state = call.getState();
            Log.i(this, "setCallState %s -> %s, call: %s", CallState.toString(state), CallState.toString(i), call);
            if (i != state && isValidNewState(state, i)) {
                if (CallRecorder.recorderOn && i != 5) {
                    record();
                }
                call.setState(i);
                Trace.beginSection("onCallStateChanged");
                if (this.mCalls.contains(call)) {
                    Iterator<CallsManagerListener> it = this.mListeners.iterator();
                    while (it.hasNext()) {
                        it.next().onCallStateChanged(call, state, i);
                    }
                    updateCallsManagerState();
                }
                Trace.endSection();
            }
        }
    }

    private void updateForegroundCall() {
        Call next;
        Trace.beginSection("updateForegroundCall");
        Call call = null;
        Iterator<Call> it = this.mCalls.iterator();
        while (true) {
            if (!it.hasNext()) {
                next = call;
                break;
            }
            next = it.next();
            if (next.getParentCall() == null) {
                if (next.isActive()) {
                    break;
                }
                if (!next.isAlive() && next.getState() != 4) {
                    next = call;
                }
                call = next;
            }
        }
        if (next != this.mForegroundCall) {
            Log.v(this, "Updating foreground call, %s -> %s.", this.mForegroundCall, next);
            Call call2 = this.mForegroundCall;
            this.mForegroundCall = next;
            Iterator<CallsManagerListener> it2 = this.mListeners.iterator();
            while (it2.hasNext()) {
                it2.next().onForegroundCallChanged(call2, this.mForegroundCall);
            }
        }
        Trace.endSection();
    }

    private void updateCanAddCall() {
        boolean zCanAddCall = canAddCall();
        if (zCanAddCall != this.mCanAddCall) {
            this.mCanAddCall = zCanAddCall;
            Iterator<CallsManagerListener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onCanAddCallChanged(this.mCanAddCall);
            }
        }
    }

    private void updateCallsManagerState() {
        updateForegroundCall();
        updateCanAddCall();
    }

    private boolean isPotentialMMICode(Uri uri) {
        return (uri == null || uri.getSchemeSpecificPart() == null || !uri.getSchemeSpecificPart().contains("#")) ? false : true;
    }

    private boolean isPotentialInCallMMICode(Uri uri) {
        if (uri == null || uri.getSchemeSpecificPart() == null || !uri.getScheme().equals("tel")) {
            return false;
        }
        String schemeSpecificPart = uri.getSchemeSpecificPart();
        return schemeSpecificPart.equals("0") || (schemeSpecificPart.startsWith("1") && schemeSpecificPart.length() <= 2) || ((schemeSpecificPart.startsWith("2") && schemeSpecificPart.length() <= 2) || schemeSpecificPart.equals("3") || schemeSpecificPart.equals("4") || schemeSpecificPart.equals("5"));
    }

    private int getNumCallsWithState(int... iArr) {
        int i;
        int i2 = 0;
        int length = iArr.length;
        int i3 = 0;
        while (i3 < length) {
            int i4 = iArr[i3];
            Iterator<Call> it = this.mCalls.iterator();
            while (true) {
                i = i2;
                if (it.hasNext()) {
                    Call next = it.next();
                    if (next.getParentCall() == null && next.getState() == i4) {
                        i++;
                    }
                    i2 = i;
                }
            }
            i3++;
            i2 = i;
        }
        return i2;
    }

    private boolean hasMaximumLiveCalls() {
        return 1 <= getNumCallsWithState(LIVE_CALL_STATES);
    }

    private boolean hasMaximumHoldingCalls() {
        return 1 <= getNumCallsWithState(6);
    }

    private boolean hasMaximumRingingCalls() {
        return 1 <= getNumCallsWithState(4);
    }

    private boolean hasMaximumOutgoingCalls() {
        return 1 <= getNumCallsWithState(OUTGOING_CALL_STATES);
    }

    private boolean makeRoomForOutgoingCall(Call call, boolean z) {
        if (!hasMaximumLiveCalls()) {
            return true;
        }
        Call firstCallWithState = getFirstCallWithState(call, LIVE_CALL_STATES);
        Log.i(this, "makeRoomForOutgoingCall call = " + call + " livecall = " + firstCallWithState, new Object[0]);
        if (call == firstCallWithState) {
            return true;
        }
        if (hasMaximumOutgoingCalls()) {
            if (z) {
                Call firstCallWithState2 = getFirstCallWithState(OUTGOING_CALL_STATES);
                if (!firstCallWithState2.isEmergencyCall()) {
                    firstCallWithState2.disconnect();
                    return true;
                }
            }
            return false;
        }
        if (hasMaximumHoldingCalls()) {
            if (!z) {
                return false;
            }
            firstCallWithState.disconnect();
            return true;
        }
        if (Objects.equals(firstCallWithState.getTargetPhoneAccount(), call.getTargetPhoneAccount()) || call.getTargetPhoneAccount() == null) {
            return true;
        }
        if (!firstCallWithState.can(1)) {
            return false;
        }
        firstCallWithState.hold();
        return true;
    }

    Call createCallForExistingConnection(String str, ParcelableConnection parcelableConnection) {
        Call call = new Call(this.mContext, this.mConnectionServiceRepository, parcelableConnection.getHandle(), null, null, parcelableConnection.getPhoneAccount(), false, false);
        setCallState(call, Call.getStateFromConnectionState(parcelableConnection.getState()));
        call.setConnectionCapabilities(parcelableConnection.getConnectionCapabilities());
        call.setCallerDisplayName(parcelableConnection.getCallerDisplayName(), parcelableConnection.getCallerDisplayNamePresentation());
        call.addListener(this);
        addCall(call);
        return call;
    }

    public void dump(IndentingPrintWriter indentingPrintWriter) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", "CallsManager");
        if (this.mCalls != null) {
            indentingPrintWriter.println("mCalls: ");
            indentingPrintWriter.increaseIndent();
            Iterator<Call> it = this.mCalls.iterator();
            while (it.hasNext()) {
                indentingPrintWriter.println(it.next());
            }
            indentingPrintWriter.decreaseIndent();
        }
        indentingPrintWriter.println("mForegroundCall: " + (this.mForegroundCall == null ? "none" : this.mForegroundCall));
        if (this.mCallAudioManager != null) {
            indentingPrintWriter.println("mCallAudioManager:");
            indentingPrintWriter.increaseIndent();
            this.mCallAudioManager.dump(indentingPrintWriter);
            indentingPrintWriter.decreaseIndent();
        }
        if (this.mTtyManager != null) {
            indentingPrintWriter.println("mTtyManager:");
            indentingPrintWriter.increaseIndent();
            this.mTtyManager.dump(indentingPrintWriter);
            indentingPrintWriter.decreaseIndent();
        }
        if (this.mInCallController != null) {
            indentingPrintWriter.println("mInCallController:");
            indentingPrintWriter.increaseIndent();
            this.mInCallController.dump(indentingPrintWriter);
            indentingPrintWriter.decreaseIndent();
        }
        if (this.mConnectionServiceRepository != null) {
            indentingPrintWriter.println("mConnectionServiceRepository:");
            indentingPrintWriter.increaseIndent();
            this.mConnectionServiceRepository.dump(indentingPrintWriter);
            indentingPrintWriter.decreaseIndent();
        }
    }
}
