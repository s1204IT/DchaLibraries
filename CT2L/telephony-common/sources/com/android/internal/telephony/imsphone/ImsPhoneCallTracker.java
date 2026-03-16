package com.android.internal.telephony.imsphone;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telecom.ConferenceParticipant;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import com.android.ims.ImsCall;
import com.android.ims.ImsCallProfile;
import com.android.ims.ImsConnectionStateListener;
import com.android.ims.ImsEcbm;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.ims.ImsReasonInfo;
import com.android.ims.ImsUtInterface;
import com.android.ims.internal.IImsVideoCallProvider;
import com.android.ims.internal.ImsVideoCallProviderWrapper;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CallTracker;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ImsPhoneCallTracker extends CallTracker {
    private static final boolean DBG = true;
    private static final int EVENT_DIAL_PENDINGMO = 20;
    private static final int EVENT_HANGUP_PENDINGMO = 18;
    private static final int EVENT_RESUME_BACKGROUND = 19;
    static final String LOG_TAG = "ImsPhoneCallTracker";
    static final int MAX_CONNECTIONS = 7;
    static final int MAX_CONNECTIONS_PER_CALL = 5;
    private static final int TIMEOUT_HANGUP_PENDINGMO = 500;
    private ImsManager mImsManager;
    private ImsPhoneConnection mPendingMO;
    ImsPhone mPhone;
    private int pendingCallClirMode;
    private int pendingCallVideoState;
    private boolean mIsVolteEnabled = false;
    private boolean mIsVtEnabled = false;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.android.ims.IMS_INCOMING_CALL")) {
                ImsPhoneCallTracker.this.log("onReceive : incoming call intent");
                if (ImsPhoneCallTracker.this.mImsManager != null && ImsPhoneCallTracker.this.mServiceId >= 0) {
                    try {
                        boolean isUssd = intent.getBooleanExtra("android:ussd", false);
                        if (!isUssd) {
                            ImsCall imsCall = ImsPhoneCallTracker.this.mImsManager.takeCall(ImsPhoneCallTracker.this.mServiceId, intent, ImsPhoneCallTracker.this.mImsCallListener);
                            ImsPhoneConnection conn = new ImsPhoneConnection(ImsPhoneCallTracker.this.mPhone.getContext(), imsCall, ImsPhoneCallTracker.this, ImsPhoneCallTracker.this.mRingingCall);
                            ImsPhoneCallTracker.this.addConnection(conn);
                            IImsVideoCallProvider imsVideoCallProvider = imsCall.getCallSession().getVideoCallProvider();
                            if (imsVideoCallProvider != null) {
                                ImsVideoCallProviderWrapper imsVideoCallProviderWrapper = new ImsVideoCallProviderWrapper(imsVideoCallProvider);
                                conn.setVideoProvider(imsVideoCallProviderWrapper);
                            }
                            if (ImsPhoneCallTracker.this.mForegroundCall.getState() != Call.State.IDLE || ImsPhoneCallTracker.this.mBackgroundCall.getState() != Call.State.IDLE) {
                                conn.update(imsCall, Call.State.WAITING);
                            }
                            ImsPhoneCallTracker.this.mPhone.notifyNewRingingConnection(conn);
                            ImsPhoneCallTracker.this.mPhone.notifyIncomingRing();
                            ImsPhoneCallTracker.this.updatePhoneState();
                            ImsPhoneCallTracker.this.mPhone.notifyPreciseCallStateChanged();
                            return;
                        }
                        ImsPhoneCallTracker.this.log("onReceive : USSD");
                        ImsPhoneCallTracker.this.mUssdSession = ImsPhoneCallTracker.this.mImsManager.takeCall(ImsPhoneCallTracker.this.mServiceId, intent, ImsPhoneCallTracker.this.mImsUssdListener);
                        if (ImsPhoneCallTracker.this.mUssdSession != null) {
                            ImsPhoneCallTracker.this.mUssdSession.accept(2);
                        }
                    } catch (ImsException e) {
                        ImsPhoneCallTracker.this.loge("onReceive : exception " + e);
                    } catch (RemoteException e2) {
                    }
                }
            }
        }
    };
    private ArrayList<ImsPhoneConnection> mConnections = new ArrayList<>();
    private RegistrantList mVoiceCallEndedRegistrants = new RegistrantList();
    private RegistrantList mVoiceCallStartedRegistrants = new RegistrantList();
    ImsPhoneCall mRingingCall = new ImsPhoneCall(this);
    ImsPhoneCall mForegroundCall = new ImsPhoneCall(this);
    ImsPhoneCall mBackgroundCall = new ImsPhoneCall(this);
    ImsPhoneCall mHandoverCall = new ImsPhoneCall(this);
    private int mClirMode = 0;
    private Object mSyncHold = new Object();
    private ImsCall mUssdSession = null;
    private Message mPendingUssd = null;
    private boolean mDesiredMute = false;
    private boolean mOnHoldToneStarted = false;
    PhoneConstants.State mState = PhoneConstants.State.IDLE;
    private int mServiceId = -1;
    private Call.SrvccState mSrvccState = Call.SrvccState.NONE;
    private boolean mIsInEmergencyCall = false;
    private boolean pendingCallInEcm = false;
    private boolean mSwitchingFgAndBgCalls = false;
    private ImsCall mCallExpectedToResume = null;
    private ImsCall.Listener mImsCallListener = new ImsCall.Listener() {
        public void onCallProgressing(ImsCall imsCall) {
            ImsPhoneCallTracker.this.log("onCallProgressing");
            ImsPhoneCallTracker.this.processCallStateChange(imsCall, Call.State.ALERTING, 0);
        }

        public void onCallStarted(ImsCall imsCall) {
            ImsPhoneCallTracker.this.log("onCallStarted");
            ImsPhoneCallTracker.this.mPendingMO = null;
            ImsPhoneCallTracker.this.processCallStateChange(imsCall, Call.State.ACTIVE, 0);
        }

        public void onCallStartFailed(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            ImsPhoneCallTracker.this.log("onCallStartFailed reasonCode=" + reasonInfo.getCode());
            if (ImsPhoneCallTracker.this.mPendingMO != null) {
                if (reasonInfo.getCode() != 146 || ImsPhoneCallTracker.this.mBackgroundCall.getState() != Call.State.IDLE || ImsPhoneCallTracker.this.mRingingCall.getState() != Call.State.IDLE) {
                    int cause = ImsPhoneCallTracker.this.getDisconnectCauseFromReasonInfo(reasonInfo);
                    ImsPhoneCallTracker.this.processCallStateChange(imsCall, Call.State.DISCONNECTED, cause);
                    ImsPhoneCallTracker.this.mPendingMO = null;
                } else {
                    ImsPhoneCallTracker.this.mForegroundCall.detach(ImsPhoneCallTracker.this.mPendingMO);
                    ImsPhoneCallTracker.this.removeConnection(ImsPhoneCallTracker.this.mPendingMO);
                    ImsPhoneCallTracker.this.mPendingMO.finalize();
                    ImsPhoneCallTracker.this.mPendingMO = null;
                    ImsPhoneCallTracker.this.mPhone.initiateSilentRedial();
                }
            }
        }

        public void onCallTerminated(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            ImsPhoneCallTracker.this.log("onCallTerminated reasonCode=" + reasonInfo.getCode());
            ImsPhoneCallTracker.this.mForegroundCall.getState();
            int cause = ImsPhoneCallTracker.this.getDisconnectCauseFromReasonInfo(reasonInfo);
            ImsPhoneConnection conn = ImsPhoneCallTracker.this.findConnection(imsCall);
            ImsPhoneCallTracker.this.log("cause = " + cause + " conn = " + conn);
            if (conn != null && conn.isIncoming() && conn.getConnectTime() == 0) {
                if (cause == 2) {
                    cause = 1;
                }
                ImsPhoneCallTracker.this.log("Incoming connection of 0 connect time detected - translated cause = " + cause);
            }
            if (cause == 2 && conn != null && conn.getImsCall().isMerged()) {
                cause = 45;
            }
            ImsPhoneCallTracker.this.processCallStateChange(imsCall, Call.State.DISCONNECTED, cause);
        }

        public void onCallHeld(ImsCall imsCall) {
            ImsPhoneCallTracker.this.log("onCallHeld");
            synchronized (ImsPhoneCallTracker.this.mSyncHold) {
                Call.State oldState = ImsPhoneCallTracker.this.mBackgroundCall.getState();
                ImsPhoneCallTracker.this.processCallStateChange(imsCall, Call.State.HOLDING, 0);
                if (oldState == Call.State.ACTIVE) {
                    if (ImsPhoneCallTracker.this.mForegroundCall.getState() != Call.State.HOLDING && ImsPhoneCallTracker.this.mRingingCall.getState() != Call.State.WAITING) {
                        if (ImsPhoneCallTracker.this.mPendingMO != null) {
                            ImsPhoneCallTracker.this.sendEmptyMessage(20);
                        }
                        ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls = false;
                    } else {
                        ImsPhoneCallTracker.this.sendEmptyMessage(19);
                    }
                }
            }
        }

        public void onCallHoldFailed(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            ImsPhoneCallTracker.this.log("onCallHoldFailed reasonCode=" + reasonInfo.getCode());
            synchronized (ImsPhoneCallTracker.this.mSyncHold) {
                Call.State bgState = ImsPhoneCallTracker.this.mBackgroundCall.getState();
                if (reasonInfo.getCode() == 148) {
                    if (ImsPhoneCallTracker.this.mPendingMO != null) {
                        ImsPhoneCallTracker.this.sendEmptyMessage(20);
                    }
                } else if (bgState == Call.State.ACTIVE) {
                    ImsPhoneCallTracker.this.mForegroundCall.switchWith(ImsPhoneCallTracker.this.mBackgroundCall);
                    if (ImsPhoneCallTracker.this.mPendingMO != null) {
                        ImsPhoneCallTracker.this.mPendingMO.setDisconnectCause(36);
                        ImsPhoneCallTracker.this.sendEmptyMessageDelayed(18, 500L);
                    }
                }
            }
        }

        public void onCallResumed(ImsCall imsCall) {
            ImsPhoneCallTracker.this.log("onCallResumed");
            if (ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls && imsCall != ImsPhoneCallTracker.this.mCallExpectedToResume) {
                ImsPhoneCallTracker.this.mForegroundCall.switchWith(ImsPhoneCallTracker.this.mBackgroundCall);
                ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls = false;
                ImsPhoneCallTracker.this.mCallExpectedToResume = null;
            }
            ImsPhoneCallTracker.this.processCallStateChange(imsCall, Call.State.ACTIVE, 0);
        }

        public void onCallResumeFailed(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            if (ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls && imsCall == ImsPhoneCallTracker.this.mCallExpectedToResume) {
                ImsPhoneCallTracker.this.mForegroundCall.switchWith(ImsPhoneCallTracker.this.mBackgroundCall);
                ImsPhoneCallTracker.this.mCallExpectedToResume = null;
                ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls = false;
            }
            ImsPhoneCallTracker.this.mPhone.notifySuppServiceFailed(Phone.SuppService.RESUME);
        }

        public void onCallResumeReceived(ImsCall imsCall) {
            ImsPhoneCallTracker.this.log("onCallResumeReceived");
            if (ImsPhoneCallTracker.this.mOnHoldToneStarted) {
                ImsPhoneCallTracker.this.mPhone.stopOnHoldTone();
                ImsPhoneCallTracker.this.mOnHoldToneStarted = false;
            }
        }

        public void onCallHoldReceived(ImsCall imsCall) {
            ImsPhoneCallTracker.this.log("onCallHoldReceived");
            ImsPhoneConnection conn = ImsPhoneCallTracker.this.findConnection(imsCall);
            if (conn != null && conn.getState() == Call.State.ACTIVE && !ImsPhoneCallTracker.this.mOnHoldToneStarted && ImsPhoneCall.isLocalTone(imsCall)) {
                ImsPhoneCallTracker.this.mPhone.startOnHoldTone();
                ImsPhoneCallTracker.this.mOnHoldToneStarted = true;
            }
        }

        public void onCallMerged(ImsCall call, boolean swapCalls) {
            ImsPhoneCallTracker.this.log("onCallMerged");
            ImsPhoneCallTracker.this.mForegroundCall.merge(ImsPhoneCallTracker.this.mBackgroundCall, ImsPhoneCallTracker.this.mForegroundCall.getState());
            if (swapCalls) {
                try {
                    ImsPhoneCallTracker.this.switchWaitingOrHoldingAndActive();
                } catch (CallStateException e) {
                    ImsPhoneCallTracker.this.loge("Failed swap fg and bg calls on merge exception=" + e);
                }
            }
            ImsPhoneCallTracker.this.updatePhoneState();
            ImsPhoneCallTracker.this.mPhone.notifyPreciseCallStateChanged();
        }

        public void onCallMergeFailed(ImsCall call, ImsReasonInfo reasonInfo) {
            ImsPhoneCallTracker.this.log("onCallMergeFailed reasonInfo=" + reasonInfo);
            ImsPhoneCallTracker.this.mPhone.notifySuppServiceFailed(Phone.SuppService.CONFERENCE);
        }

        public void onConferenceParticipantsStateChanged(ImsCall call, List<ConferenceParticipant> participants) {
            ImsPhoneCallTracker.this.log("onConferenceParticipantsStateChanged");
            ImsPhoneConnection conn = ImsPhoneCallTracker.this.findConnection(call);
            if (conn != null) {
                conn.updateConferenceParticipants(participants);
            }
        }

        public void onCallSessionTtyModeReceived(ImsCall call, int mode) {
            ImsPhoneCallTracker.this.mPhone.onTtyModeReceived(mode);
        }

        public void onCallUpdated(ImsCall imsCall) {
            ImsPhoneCallTracker.this.log("onCallUpdated");
            ImsPhoneConnection conn = ImsPhoneCallTracker.this.findConnection(imsCall);
            if (conn != null) {
                Call.State state = conn.getCall().getState();
                ImsPhoneCallTracker.this.processCallStateChange(imsCall, state, 0);
            }
        }
    };
    private ImsCall.Listener mImsUssdListener = new ImsCall.Listener() {
        public void onCallStarted(ImsCall imsCall) {
            ImsPhoneCallTracker.this.log("mImsUssdListener onCallStarted");
            if (imsCall == ImsPhoneCallTracker.this.mUssdSession && ImsPhoneCallTracker.this.mPendingUssd != null) {
                AsyncResult.forMessage(ImsPhoneCallTracker.this.mPendingUssd);
                ImsPhoneCallTracker.this.mPendingUssd.sendToTarget();
                ImsPhoneCallTracker.this.mPendingUssd = null;
            }
        }

        public void onCallStartFailed(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            ImsPhoneCallTracker.this.log("mImsUssdListener onCallStartFailed reasonCode=" + reasonInfo.getCode());
            onCallTerminated(imsCall, reasonInfo);
        }

        public void onCallTerminated(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            ImsPhoneCallTracker.this.log("mImsUssdListener onCallTerminated reasonCode=" + reasonInfo.getCode());
            if (imsCall == ImsPhoneCallTracker.this.mUssdSession) {
                ImsPhoneCallTracker.this.mUssdSession = null;
                if (ImsPhoneCallTracker.this.mPendingUssd != null) {
                    CommandException ex = new CommandException(CommandException.Error.GENERIC_FAILURE);
                    AsyncResult.forMessage(ImsPhoneCallTracker.this.mPendingUssd, (Object) null, ex);
                    ImsPhoneCallTracker.this.mPendingUssd.sendToTarget();
                    ImsPhoneCallTracker.this.mPendingUssd = null;
                }
            }
            imsCall.close();
        }

        public void onCallUssdMessageReceived(ImsCall call, int mode, String ussdMessage) {
            ImsPhoneCallTracker.this.log("mImsUssdListener onCallUssdMessageReceived mode=" + mode);
            int ussdMode = -1;
            switch (mode) {
                case 0:
                    ussdMode = 0;
                    break;
                case 1:
                    ussdMode = 1;
                    break;
            }
            ImsPhoneCallTracker.this.mPhone.onIncomingUSSD(ussdMode, ussdMessage);
        }
    };
    private ImsConnectionStateListener mImsConnectionStateListener = new ImsConnectionStateListener() {
        public void onImsConnected() {
            int oldState = ImsPhoneCallTracker.this.mPhone.getServiceState().getState();
            ImsPhoneCallTracker.this.log("onImsConnected oldState=" + oldState);
            if (oldState != 0) {
                ImsPhoneCallTracker.this.mPhone.setServiceState(0);
                ImsPhoneCallTracker.this.mPhone.setImsRegistered(true);
                ImsPhoneCallTracker.this.mPhone.notifyImsStateChanged();
            }
        }

        public void onImsDisconnected() {
            int oldState = ImsPhoneCallTracker.this.mPhone.getServiceState().getState();
            ImsPhoneCallTracker.this.log("onImsDisconnected oldSate=" + oldState);
            if (oldState != 1) {
                ImsPhoneCallTracker.this.mPhone.setServiceState(1);
                ImsPhoneCallTracker.this.mPhone.setImsRegistered(false);
                ImsPhoneCallTracker.this.mPhone.notifyImsStateChanged();
            }
        }

        public void onImsResumed() {
            ImsPhoneCallTracker.this.log("onImsResumed");
            ImsPhoneCallTracker.this.mPhone.setServiceState(0);
        }

        public void onImsSuspended() {
            ImsPhoneCallTracker.this.log("onImsSuspended");
            ImsPhoneCallTracker.this.mPhone.setServiceState(1);
        }

        public void onFeatureCapabilityChanged(int serviceClass, int[] enabledFeatures, int[] disabledFeatures) {
            if (serviceClass == 1) {
                if (enabledFeatures[0] == 0) {
                    ImsPhoneCallTracker.this.mIsVolteEnabled = true;
                }
                if (enabledFeatures[1] == 1) {
                    ImsPhoneCallTracker.this.mIsVtEnabled = true;
                }
                if (disabledFeatures[0] == 0) {
                    ImsPhoneCallTracker.this.mIsVolteEnabled = false;
                }
                if (disabledFeatures[1] == 1) {
                    ImsPhoneCallTracker.this.mIsVtEnabled = false;
                }
            }
            ImsPhoneCallTracker.this.log("onFeatureCapabilityChanged, mIsVolteEnabled = " + ImsPhoneCallTracker.this.mIsVolteEnabled + " mIsVtEnabled = " + ImsPhoneCallTracker.this.mIsVtEnabled);
        }
    };

    ImsPhoneCallTracker(ImsPhone phone) {
        this.mPhone = phone;
        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction("com.android.ims.IMS_INCOMING_CALL");
        this.mPhone.getContext().registerReceiver(this.mReceiver, intentfilter);
        Thread t = new Thread() {
            @Override
            public void run() {
                ImsPhoneCallTracker.this.getImsService();
            }
        };
        t.start();
    }

    private PendingIntent createIncomingCallPendingIntent() {
        Intent intent = new Intent("com.android.ims.IMS_INCOMING_CALL");
        intent.addFlags(268435456);
        return PendingIntent.getBroadcast(this.mPhone.getContext(), 0, intent, 134217728);
    }

    private void getImsService() {
        log("getImsService");
        this.mImsManager = ImsManager.getInstance(this.mPhone.getContext(), this.mPhone.getPhoneId());
        try {
            this.mServiceId = this.mImsManager.open(1, createIncomingCallPendingIntent(), this.mImsConnectionStateListener);
            int mPreferredTtyMode = Settings.Secure.getInt(this.mPhone.getContext().getContentResolver(), "preferred_tty_mode", 0);
            this.mImsManager.setUiTTYMode(this.mPhone.getContext(), this.mServiceId, mPreferredTtyMode, (Message) null);
            this.mImsManager.getSmsInterface(this.mServiceId);
        } catch (ImsException e) {
            loge("getImsService: " + e);
            this.mImsManager = null;
        }
    }

    public void dispose() {
        log("dispose");
        this.mRingingCall.dispose();
        this.mBackgroundCall.dispose();
        this.mForegroundCall.dispose();
        this.mHandoverCall.dispose();
        clearDisconnected();
        this.mPhone.getContext().unregisterReceiver(this.mReceiver);
    }

    protected void finalize() {
        log("ImsPhoneCallTracker finalized");
    }

    @Override
    public void registerForVoiceCallStarted(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mVoiceCallStartedRegistrants.add(r);
    }

    @Override
    public void unregisterForVoiceCallStarted(Handler h) {
        this.mVoiceCallStartedRegistrants.remove(h);
    }

    @Override
    public void registerForVoiceCallEnded(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mVoiceCallEndedRegistrants.add(r);
    }

    @Override
    public void unregisterForVoiceCallEnded(Handler h) {
        this.mVoiceCallEndedRegistrants.remove(h);
    }

    Connection dial(String dialString, int videoState) throws CallStateException {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mPhone.getContext());
        int oirMode = sp.getInt(PhoneBase.CLIR_KEY, 0);
        return dial(dialString, oirMode, videoState);
    }

    synchronized Connection dial(String dialString, int clirMode, int videoState) throws CallStateException {
        boolean isPhoneInEcmMode = SystemProperties.getBoolean("ril.cdma.inecmmode", false);
        boolean isEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(dialString);
        log("dial clirMode=" + clirMode);
        clearDisconnected();
        if (this.mImsManager == null) {
            throw new CallStateException("service not available");
        }
        if (!canDial()) {
            throw new CallStateException("cannot dial in current state");
        }
        if (isPhoneInEcmMode && isEmergencyNumber) {
            handleEcmTimer(1);
        }
        boolean holdBeforeDial = false;
        if (this.mForegroundCall.getState() == Call.State.ACTIVE) {
            if (this.mBackgroundCall.getState() != Call.State.IDLE) {
                throw new CallStateException("cannot dial in current state");
            }
            holdBeforeDial = true;
            switchWaitingOrHoldingAndActive();
        }
        Call.State state = Call.State.IDLE;
        Call.State state2 = Call.State.IDLE;
        this.mClirMode = clirMode;
        synchronized (this.mSyncHold) {
            if (holdBeforeDial) {
                Call.State fgState = this.mForegroundCall.getState();
                Call.State bgState = this.mBackgroundCall.getState();
                if (fgState == Call.State.ACTIVE) {
                    throw new CallStateException("cannot dial in current state");
                }
                if (bgState == Call.State.HOLDING) {
                    holdBeforeDial = false;
                }
                this.mPendingMO = new ImsPhoneConnection(this.mPhone.getContext(), checkForTestEmergencyNumber(dialString), this, this.mForegroundCall);
            } else {
                this.mPendingMO = new ImsPhoneConnection(this.mPhone.getContext(), checkForTestEmergencyNumber(dialString), this, this.mForegroundCall);
            }
        }
        addConnection(this.mPendingMO);
        if (!holdBeforeDial) {
            if (!isPhoneInEcmMode || (isPhoneInEcmMode && isEmergencyNumber)) {
                dialInternal(this.mPendingMO, clirMode, videoState);
            } else {
                try {
                    getEcbmInterface().exitEmergencyCallbackMode();
                    this.mPhone.setOnEcbModeExitResponse(this, 14, null);
                    this.pendingCallClirMode = clirMode;
                    this.pendingCallVideoState = videoState;
                    this.pendingCallInEcm = true;
                } catch (ImsException e) {
                    e.printStackTrace();
                    throw new CallStateException("service not available");
                }
            }
        }
        updatePhoneState();
        this.mPhone.notifyPreciseCallStateChanged();
        return this.mPendingMO;
    }

    private void handleEcmTimer(int action) {
        this.mPhone.handleTimerInEmergencyCallbackMode(action);
        switch (action) {
            case 0:
            case 1:
                break;
            default:
                log("handleEcmTimer, unsupported action " + action);
                break;
        }
    }

    private void dialInternal(ImsPhoneConnection conn, int clirMode, int videoState) {
        if (conn != null) {
            if (conn.getAddress() == null || conn.getAddress().length() == 0 || conn.getAddress().indexOf(78) >= 0) {
                conn.setDisconnectCause(7);
                sendEmptyMessageDelayed(18, 500L);
                return;
            }
            setMute(false);
            int serviceType = PhoneNumberUtils.isEmergencyNumber(conn.getAddress()) ? 2 : 1;
            int callType = ImsCallProfile.getCallTypeFromVideoState(videoState);
            conn.setVideoState(videoState);
            try {
                String[] callees = {conn.getAddress()};
                ImsCallProfile profile = this.mImsManager.createCallProfile(this.mServiceId, serviceType, callType);
                profile.setCallExtraInt("oir", clirMode);
                ImsCall imsCall = this.mImsManager.makeCall(this.mServiceId, profile, callees, this.mImsCallListener);
                conn.setImsCall(imsCall);
                IImsVideoCallProvider imsVideoCallProvider = imsCall.getCallSession().getVideoCallProvider();
                if (imsVideoCallProvider != null) {
                    ImsVideoCallProviderWrapper imsVideoCallProviderWrapper = new ImsVideoCallProviderWrapper(imsVideoCallProvider);
                    conn.setVideoProvider(imsVideoCallProviderWrapper);
                }
            } catch (RemoteException e) {
            } catch (ImsException e2) {
                loge("dialInternal : " + e2);
                conn.setDisconnectCause(36);
                sendEmptyMessageDelayed(18, 500L);
            }
        }
    }

    void acceptCall(int videoState) throws CallStateException {
        log("acceptCall");
        if (this.mForegroundCall.getState().isAlive() && this.mBackgroundCall.getState().isAlive()) {
            throw new CallStateException("cannot accept call");
        }
        if (this.mRingingCall.getState() == Call.State.WAITING && this.mForegroundCall.getState().isAlive()) {
            setMute(false);
            switchWaitingOrHoldingAndActive();
        } else {
            if (this.mRingingCall.getState().isRinging()) {
                log("acceptCall: incoming...");
                setMute(false);
                try {
                    ImsCall imsCall = this.mRingingCall.getImsCall();
                    if (imsCall != null) {
                        imsCall.accept(ImsCallProfile.getCallTypeFromVideoState(videoState));
                        return;
                    }
                    throw new CallStateException("no valid ims call");
                } catch (ImsException e) {
                    throw new CallStateException("cannot accept call");
                }
            }
            throw new CallStateException("phone not ringing");
        }
    }

    void rejectCall() throws CallStateException {
        log("rejectCall");
        if (this.mRingingCall.getState().isRinging()) {
            hangup(this.mRingingCall);
            return;
        }
        throw new CallStateException("phone not ringing");
    }

    void switchWaitingOrHoldingAndActive() throws CallStateException {
        log("switchWaitingOrHoldingAndActive");
        if (this.mRingingCall.getState() == Call.State.INCOMING) {
            throw new CallStateException("cannot be in the incoming state");
        }
        if (this.mForegroundCall.getState() == Call.State.ACTIVE) {
            ImsCall imsCall = this.mForegroundCall.getImsCall();
            if (imsCall == null) {
                throw new CallStateException("no ims call");
            }
            this.mSwitchingFgAndBgCalls = true;
            this.mCallExpectedToResume = this.mBackgroundCall.getImsCall();
            this.mForegroundCall.switchWith(this.mBackgroundCall);
            try {
                imsCall.hold();
                return;
            } catch (ImsException e) {
                this.mForegroundCall.switchWith(this.mBackgroundCall);
                throw new CallStateException(e.getMessage());
            }
        }
        if (this.mBackgroundCall.getState() == Call.State.HOLDING) {
            resumeWaitingOrHolding();
        }
    }

    void conference() {
        log("conference");
        ImsCall fgImsCall = this.mForegroundCall.getImsCall();
        if (fgImsCall == null) {
            log("conference no foreground ims call");
            return;
        }
        ImsCall bgImsCall = this.mBackgroundCall.getImsCall();
        if (bgImsCall == null) {
            log("conference no background ims call");
            return;
        }
        long conferenceConnectTime = Math.min(this.mForegroundCall.getEarliestConnectTime(), this.mBackgroundCall.getEarliestConnectTime());
        ImsPhoneConnection foregroundConnection = this.mForegroundCall.getFirstConnection();
        if (foregroundConnection != null) {
            foregroundConnection.setConferenceConnectTime(conferenceConnectTime);
        }
        try {
            fgImsCall.merge(bgImsCall);
        } catch (ImsException e) {
            log("conference " + e.getMessage());
        }
    }

    void explicitCallTransfer() {
    }

    void clearDisconnected() {
        log("clearDisconnected");
        internalClearDisconnected();
        updatePhoneState();
        this.mPhone.notifyPreciseCallStateChanged();
    }

    boolean canConference() {
        return this.mForegroundCall.getState() == Call.State.ACTIVE && this.mBackgroundCall.getState() == Call.State.HOLDING && !this.mBackgroundCall.isFull() && !this.mForegroundCall.isFull();
    }

    boolean canDial() {
        int serviceState = this.mPhone.getServiceState().getState();
        String disableCall = SystemProperties.get("ro.telephony.disable-call", "false");
        return (serviceState == 3 || this.mPendingMO != null || this.mRingingCall.isRinging() || disableCall.equals("true") || (this.mForegroundCall.getState().isAlive() && this.mBackgroundCall.getState().isAlive())) ? false : true;
    }

    boolean canTransfer() {
        return this.mForegroundCall.getState() == Call.State.ACTIVE && this.mBackgroundCall.getState() == Call.State.HOLDING;
    }

    private void internalClearDisconnected() {
        this.mRingingCall.clearDisconnected();
        this.mForegroundCall.clearDisconnected();
        this.mBackgroundCall.clearDisconnected();
        this.mHandoverCall.clearDisconnected();
    }

    private void updatePhoneState() {
        PhoneConstants.State oldState = this.mState;
        if (this.mRingingCall.isRinging()) {
            this.mState = PhoneConstants.State.RINGING;
        } else if (this.mPendingMO != null || !this.mForegroundCall.isIdle() || !this.mBackgroundCall.isIdle()) {
            this.mState = PhoneConstants.State.OFFHOOK;
        } else {
            this.mState = PhoneConstants.State.IDLE;
        }
        if (this.mState == PhoneConstants.State.IDLE && oldState != this.mState) {
            this.mVoiceCallEndedRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        } else if (oldState == PhoneConstants.State.IDLE && oldState != this.mState) {
            this.mVoiceCallStartedRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
        log("updatePhoneState oldState=" + oldState + ", newState=" + this.mState);
        if (this.mState != oldState) {
            this.mPhone.notifyPhoneStateChanged();
        }
    }

    private void handleRadioNotAvailable() {
        pollCallsWhenSafe();
    }

    private void dumpState() {
        log("Phone State:" + this.mState);
        log("Ringing call: " + this.mRingingCall.toString());
        List<Connection> connections = this.mRingingCall.getConnections();
        int s = connections.size();
        for (int i = 0; i < s; i++) {
            log(connections.get(i).toString());
        }
        log("Foreground call: " + this.mForegroundCall.toString());
        List<Connection> connections2 = this.mForegroundCall.getConnections();
        int s2 = connections2.size();
        for (int i2 = 0; i2 < s2; i2++) {
            log(connections2.get(i2).toString());
        }
        log("Background call: " + this.mBackgroundCall.toString());
        List<Connection> connections3 = this.mBackgroundCall.getConnections();
        int s3 = connections3.size();
        for (int i3 = 0; i3 < s3; i3++) {
            log(connections3.get(i3).toString());
        }
    }

    void setUiTTYMode(int uiTtyMode, Message onComplete) {
        try {
            this.mImsManager.setUiTTYMode(this.mPhone.getContext(), this.mServiceId, uiTtyMode, onComplete);
        } catch (ImsException e) {
            loge("setTTYMode : " + e);
            this.mPhone.sendErrorResponse(onComplete, e);
        }
    }

    void setMute(boolean mute) {
        this.mDesiredMute = mute;
        this.mForegroundCall.setMute(mute);
    }

    boolean getMute() {
        return this.mDesiredMute;
    }

    void sendDtmf(char c, Message result) {
        log("sendDtmf");
        ImsCall imscall = this.mForegroundCall.getImsCall();
        if (imscall != null) {
            imscall.sendDtmf(c, result);
        }
    }

    void startDtmf(char c) {
        log("startDtmf");
        ImsCall imscall = this.mForegroundCall.getImsCall();
        if (imscall != null) {
            imscall.startDtmf(c);
        } else {
            loge("startDtmf : no foreground call");
        }
    }

    void stopDtmf() {
        log("stopDtmf");
        ImsCall imscall = this.mForegroundCall.getImsCall();
        if (imscall != null) {
            imscall.stopDtmf();
        } else {
            loge("stopDtmf : no foreground call");
        }
    }

    void hangup(ImsPhoneConnection conn) throws CallStateException {
        log("hangup connection");
        if (conn.getOwner() != this) {
            throw new CallStateException("ImsPhoneConnection " + conn + "does not belong to ImsPhoneCallTracker " + this);
        }
        hangup(conn.getCall());
    }

    void hangup(ImsPhoneCall call) throws CallStateException {
        log("hangup call");
        if (call.getConnections().size() == 0) {
            throw new CallStateException("no connections");
        }
        ImsCall imsCall = call.getImsCall();
        boolean rejectCall = false;
        if (call == this.mRingingCall) {
            log("(ringing) hangup incoming");
            rejectCall = true;
        } else if (call == this.mForegroundCall) {
            if (call.isDialingOrAlerting()) {
                log("(foregnd) hangup dialing or alerting...");
            } else {
                log("(foregnd) hangup foreground");
            }
        } else if (call == this.mBackgroundCall) {
            log("(backgnd) hangup waiting or background");
        } else {
            throw new CallStateException("ImsPhoneCall " + call + "does not belong to ImsPhoneCallTracker " + this);
        }
        call.onHangupLocal();
        try {
            if (imsCall != null) {
                if (rejectCall) {
                    imsCall.reject(504);
                } else {
                    imsCall.terminate(501);
                }
            } else if (this.mPendingMO != null && call == this.mForegroundCall) {
                this.mPendingMO.update(null, Call.State.DISCONNECTED);
                this.mPendingMO.onDisconnect();
                removeConnection(this.mPendingMO);
                this.mPendingMO = null;
                updatePhoneState();
                removeMessages(20);
            }
            this.mPhone.notifyPreciseCallStateChanged();
        } catch (ImsException e) {
            throw new CallStateException(e.getMessage());
        }
    }

    void callEndCleanupHandOverCallIfAny() {
        if (this.mHandoverCall.mConnections.size() > 0) {
            log("callEndCleanupHandOverCallIfAny, mHandoverCall.mConnections=" + this.mHandoverCall.mConnections);
            this.mHandoverCall.mConnections.clear();
            this.mState = PhoneConstants.State.IDLE;
        }
    }

    void resumeWaitingOrHolding() throws CallStateException {
        log("resumeWaitingOrHolding");
        try {
            if (this.mForegroundCall.getState().isAlive()) {
                ImsCall imsCall = this.mForegroundCall.getImsCall();
                if (imsCall != null) {
                    imsCall.resume();
                    return;
                }
                return;
            }
            if (this.mRingingCall.getState() == Call.State.WAITING) {
                ImsCall imsCall2 = this.mRingingCall.getImsCall();
                if (imsCall2 != null) {
                    imsCall2.accept(2);
                    return;
                }
                return;
            }
            ImsCall imsCall3 = this.mBackgroundCall.getImsCall();
            if (imsCall3 != null) {
                imsCall3.resume();
            }
        } catch (ImsException e) {
            throw new CallStateException(e.getMessage());
        }
    }

    void sendUSSD(String ussdString, Message response) {
        log("sendUSSD");
        try {
            if (this.mUssdSession != null) {
                this.mUssdSession.sendUssd(ussdString);
                AsyncResult.forMessage(response, (Object) null, (Throwable) null);
                response.sendToTarget();
            } else {
                String[] callees = {ussdString};
                ImsCallProfile profile = this.mImsManager.createCallProfile(this.mServiceId, 1, 2);
                profile.setCallExtraInt("dialstring", 2);
                this.mUssdSession = this.mImsManager.makeCall(this.mServiceId, profile, callees, this.mImsUssdListener);
            }
        } catch (ImsException e) {
            loge("sendUSSD : " + e);
            this.mPhone.sendErrorResponse(response, e);
        }
    }

    void cancelUSSD() {
        if (this.mUssdSession != null) {
            try {
                this.mUssdSession.terminate(501);
            } catch (ImsException e) {
            }
        }
    }

    private synchronized ImsPhoneConnection findConnection(ImsCall imsCall) {
        ImsPhoneConnection conn;
        Iterator<ImsPhoneConnection> it = this.mConnections.iterator();
        while (true) {
            if (!it.hasNext()) {
                conn = null;
                break;
            }
            conn = it.next();
            if (conn.getImsCall() == imsCall) {
                break;
            }
        }
        return conn;
    }

    private synchronized void removeConnection(ImsPhoneConnection conn) {
        this.mConnections.remove(conn);
    }

    private synchronized void addConnection(ImsPhoneConnection conn) {
        this.mConnections.add(conn);
    }

    private void processCallStateChange(ImsCall imsCall, Call.State state, int cause) {
        ImsPhoneConnection conn;
        log("processCallStateChange " + imsCall + " state=" + state + " cause=" + cause);
        if (imsCall != null && (conn = findConnection(imsCall)) != null) {
            boolean changed = conn.update(imsCall, state);
            if (state == Call.State.DISCONNECTED) {
                changed = conn.onDisconnect(cause) || changed;
                conn.getCall().detach(conn);
                removeConnection(conn);
            }
            if (changed && conn.getCall() != this.mHandoverCall) {
                updatePhoneState();
                this.mPhone.notifyPreciseCallStateChanged();
            }
        }
    }

    private int getDisconnectCauseFromReasonInfo(ImsReasonInfo reasonInfo) {
        int code = reasonInfo.getCode();
        switch (code) {
            case 106:
            case 121:
            case 122:
            case 123:
            case 124:
            case 131:
            case 132:
            case 144:
                return 18;
            case 111:
            case 112:
                return 17;
            case 143:
                return 16;
            case 201:
            case 202:
            case 203:
            case 335:
                return 13;
            case 321:
            case 331:
            case 332:
            case 340:
            case 361:
            case 362:
                return 12;
            case 333:
            case 352:
            case 354:
                return 9;
            case 337:
            case 341:
                return 8;
            case 338:
                return 4;
            case 501:
                return 3;
            case 510:
                return 2;
            default:
                return 36;
        }
    }

    ImsUtInterface getUtInterface() throws ImsException {
        if (this.mImsManager == null) {
            throw new ImsException("no ims manager", 0);
        }
        ImsUtInterface ut = this.mImsManager.getSupplementaryServiceConfiguration(this.mServiceId);
        return ut;
    }

    private void transferHandoverConnections(ImsPhoneCall call) {
        if (call.mConnections != null) {
            for (Connection c : call.mConnections) {
                c.mPreHandoverState = call.mState;
                log("Connection state before handover is " + c.getStateBeforeHandover());
            }
        }
        if (this.mHandoverCall.mConnections == null) {
            this.mHandoverCall.mConnections = call.mConnections;
        } else {
            this.mHandoverCall.mConnections.addAll(call.mConnections);
        }
        if (this.mHandoverCall.mConnections != null) {
            if (call.getImsCall() != null) {
                call.getImsCall().close();
            }
            for (Connection c2 : this.mHandoverCall.mConnections) {
                ((ImsPhoneConnection) c2).changeParent(this.mHandoverCall);
                ((ImsPhoneConnection) c2).releaseWakeLock();
            }
        }
        if (call.getState().isAlive()) {
            log("Call is alive and state is " + call.mState);
            this.mHandoverCall.mState = call.mState;
        }
        call.mConnections.clear();
        call.mState = Call.State.IDLE;
    }

    void notifySrvccState(Call.SrvccState state) {
        log("notifySrvccState state=" + state);
        this.mSrvccState = state;
        if (this.mSrvccState == Call.SrvccState.COMPLETED) {
            transferHandoverConnections(this.mForegroundCall);
            transferHandoverConnections(this.mBackgroundCall);
            transferHandoverConnections(this.mRingingCall);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        log("handleMessage what=" + msg.what);
        switch (msg.what) {
            case 14:
                if (this.pendingCallInEcm) {
                    dialInternal(this.mPendingMO, this.pendingCallClirMode, this.pendingCallVideoState);
                    this.pendingCallInEcm = false;
                }
                this.mPhone.unsetOnEcbModeExitResponse(this);
                break;
            case 18:
                if (this.mPendingMO != null) {
                    this.mPendingMO.onDisconnect();
                    removeConnection(this.mPendingMO);
                    this.mPendingMO = null;
                }
                updatePhoneState();
                this.mPhone.notifyPreciseCallStateChanged();
                break;
            case 19:
                try {
                    resumeWaitingOrHolding();
                } catch (CallStateException e) {
                    loge("handleMessage EVENT_RESUME_BACKGROUND exception=" + e);
                    return;
                }
                break;
            case 20:
                dialInternal(this.mPendingMO, this.mClirMode, 0);
                break;
        }
    }

    @Override
    protected void log(String msg) {
        Rlog.d(LOG_TAG, "[ImsPhoneCallTracker] " + msg);
    }

    protected void loge(String msg) {
        Rlog.e(LOG_TAG, "[ImsPhoneCallTracker] " + msg);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("ImsPhoneCallTracker extends:");
        super.dump(fd, pw, args);
        pw.println(" mVoiceCallEndedRegistrants=" + this.mVoiceCallEndedRegistrants);
        pw.println(" mVoiceCallStartedRegistrants=" + this.mVoiceCallStartedRegistrants);
        pw.println(" mRingingCall=" + this.mRingingCall);
        pw.println(" mForegroundCall=" + this.mForegroundCall);
        pw.println(" mBackgroundCall=" + this.mBackgroundCall);
        pw.println(" mHandoverCall=" + this.mHandoverCall);
        pw.println(" mPendingMO=" + this.mPendingMO);
        pw.println(" mPhone=" + this.mPhone);
        pw.println(" mDesiredMute=" + this.mDesiredMute);
        pw.println(" mState=" + this.mState);
    }

    @Override
    protected void handlePollCalls(AsyncResult ar) {
    }

    ImsEcbm getEcbmInterface() throws ImsException {
        if (this.mImsManager == null) {
            throw new ImsException("no ims manager", 0);
        }
        ImsEcbm ecbm = this.mImsManager.getEcbmInterface(this.mServiceId);
        return ecbm;
    }

    public boolean isInEmergencyCall() {
        return this.mIsInEmergencyCall;
    }

    public boolean isVolteEnabled() {
        return this.mIsVolteEnabled;
    }

    public boolean isVtEnabled() {
        return this.mIsVtEnabled;
    }

    @Override
    public PhoneConstants.State getState() {
        return this.mState;
    }

    public void turnOnIms() throws ImsException {
        if (this.mImsManager == null) {
            throw new ImsException("no ims manager", 0);
        }
        this.mImsManager.turnOnIms();
    }

    public void turnOffIms() throws ImsException {
        if (this.mImsManager == null) {
            throw new ImsException("no ims manager", 0);
        }
        this.mImsManager.turnOffIms();
    }
}
