package com.android.internal.telephony.imsphone;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.provider.Settings;
import android.telecom.ConferenceParticipant;
import android.telecom.Connection;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.ims.ImsCall;
import com.android.ims.ImsCallProfile;
import com.android.ims.ImsConfigListener;
import com.android.ims.ImsConnectionStateListener;
import com.android.ims.ImsEcbm;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.ims.ImsMultiEndpoint;
import com.android.ims.ImsReasonInfo;
import com.android.ims.ImsSuppServiceNotification;
import com.android.ims.ImsUtInterface;
import com.android.ims.internal.IImsVideoCallProvider;
import com.android.ims.internal.ImsVideoCallProviderWrapper;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallFailCause;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CallTracker;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.TelephonyEventLog;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.google.android.mms.pdu.CharacterSets;
import com.google.android.mms.pdu.PduHeaders;
import com.mediatek.internal.telephony.ConferenceCallMessageHandler;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import com.mediatek.telecom.FormattedLog;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class ImsPhoneCallTracker extends CallTracker implements ImsPullCall {
    private static final boolean DBG = true;
    private static final int EVENT_DIAL_PENDINGMO = 20;
    private static final int EVENT_EXIT_ECBM_BEFORE_PENDINGMO = 21;
    private static final int EVENT_HANGUP_PENDINGMO = 18;
    private static final int EVENT_RESUME_BACKGROUND = 19;
    private static final int IMS_VIDEO_CALL = 21;
    private static final int IMS_VIDEO_CONF = 23;
    private static final int IMS_VIDEO_CONF_PARTS = 25;
    private static final int IMS_VOICE_CALL = 20;
    private static final int IMS_VOICE_CONF = 22;
    private static final int IMS_VOICE_CONF_PARTS = 24;
    private static final int INVALID_CALL_MODE = 255;
    static final String LOG_TAG = "ImsPhoneCallTracker";
    static final int MAX_CONNECTIONS = 7;
    static final int MAX_CONNECTIONS_PER_CALL = 5;
    private static final int TIMEOUT_HANGUP_PENDINGMO = 500;
    private static final boolean VERBOSE_STATE_LOGGING = false;
    private boolean mAllowEmergencyVideoCalls;
    private TelephonyEventLog mEventLog;
    private ImsManager mImsManager;
    private int mImsRegistrationErrorCode;
    private int mPendingCallVideoState;
    private Bundle mPendingIntentExtras;
    private ImsPhoneConnection mPendingMO;
    ImsPhone mPhone;
    private int pendingCallClirMode;
    private boolean[] mImsFeatureEnabled = {false, false, false, false, false, false};
    private final String[] mImsFeatureStrings = {TelephonyEventLog.DATA_KEY_VOLTE, TelephonyEventLog.DATA_KEY_VILTE, TelephonyEventLog.DATA_KEY_VOWIFI, TelephonyEventLog.DATA_KEY_VIWIFI, TelephonyEventLog.DATA_KEY_UTLTE, TelephonyEventLog.DATA_KEY_UTWIFI};
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int subId;
            if (!intent.getAction().equals("com.android.ims.IMS_INCOMING_CALL")) {
                if (intent.getAction().equals("android.telephony.action.CARRIER_CONFIG_CHANGED") && (subId = intent.getIntExtra("subscription", -1)) == ImsPhoneCallTracker.this.mPhone.getSubId()) {
                    ImsPhoneCallTracker.this.mAllowEmergencyVideoCalls = ImsPhoneCallTracker.this.isEmergencyVtCallAllowed(subId);
                    ImsPhoneCallTracker.this.log("onReceive : Updating mAllowEmergencyVideoCalls = " + ImsPhoneCallTracker.this.mAllowEmergencyVideoCalls);
                    return;
                }
                return;
            }
            ImsPhoneCallTracker.this.log("onReceive : incoming call intent");
            if (ImsPhoneCallTracker.this.mImsManager != null && ImsPhoneCallTracker.this.mServiceId >= 0) {
                try {
                    boolean isUssd = intent.getBooleanExtra("android:ussd", false);
                    if (isUssd) {
                        ImsPhoneCallTracker.this.log("onReceive : USSD");
                        ImsPhoneCallTracker.this.mUssdSession = ImsPhoneCallTracker.this.mImsManager.takeCall(ImsPhoneCallTracker.this.mServiceId, intent, ImsPhoneCallTracker.this.mImsUssdListener);
                        if (ImsPhoneCallTracker.this.mUssdSession != null) {
                            ImsPhoneCallTracker.this.mUssdSession.accept(2);
                            return;
                        }
                        return;
                    }
                    boolean isUnknown = intent.getBooleanExtra("android:isUnknown", false);
                    ImsPhoneCallTracker.this.log("onReceive : isUnknown = " + isUnknown + " fg = " + ImsPhoneCallTracker.this.mForegroundCall.getState() + " bg = " + ImsPhoneCallTracker.this.mBackgroundCall.getState());
                    ImsCall imsCall = ImsPhoneCallTracker.this.mImsManager.takeCall(ImsPhoneCallTracker.this.mServiceId, intent, ImsPhoneCallTracker.this.mImsCallListener);
                    ImsPhoneConnection conn = new ImsPhoneConnection(ImsPhoneCallTracker.this.mPhone, imsCall, ImsPhoneCallTracker.this, isUnknown ? ImsPhoneCallTracker.this.mForegroundCall : ImsPhoneCallTracker.this.mRingingCall, isUnknown);
                    ImsPhoneCallTracker.this.addConnection(conn);
                    ImsPhoneCallTracker.this.setVideoCallProvider(conn, imsCall);
                    ImsPhoneCallTracker.this.logDebugMessagesWithDumpFormat("CC", conn, UsimPBMemInfo.STRING_NOT_SET);
                    ImsPhoneCallTracker.this.mEventLog.writeOnImsCallReceive(imsCall.getSession());
                    if (isUnknown) {
                        ImsPhoneCallTracker.this.mPhone.notifyUnknownConnection(conn);
                    } else {
                        if (ImsPhoneCallTracker.this.mForegroundCall.getState() != Call.State.IDLE || ImsPhoneCallTracker.this.mBackgroundCall.getState() != Call.State.IDLE) {
                            conn.update(imsCall, Call.State.WAITING);
                        }
                        ImsPhoneCallTracker.this.mPhone.notifyNewRingingConnection(conn);
                        ImsPhoneCallTracker.this.mPhone.notifyIncomingRing();
                    }
                    ImsPhoneCallTracker.this.updatePhoneState();
                    ImsPhoneCallTracker.this.mPhone.notifyPreciseCallStateChanged();
                } catch (ImsException e) {
                    ImsPhoneCallTracker.this.loge("onReceive : exception " + e);
                } catch (RemoteException e2) {
                }
            }
        }
    };
    private ArrayList<ImsPhoneConnection> mConnections = new ArrayList<>();
    private RegistrantList mVoiceCallEndedRegistrants = new RegistrantList();
    private RegistrantList mVoiceCallStartedRegistrants = new RegistrantList();
    public ImsPhoneCall mRingingCall = new ImsPhoneCall(this, ImsPhoneCall.CONTEXT_RINGING);
    public ImsPhoneCall mForegroundCall = new ImsPhoneCall(this, ImsPhoneCall.CONTEXT_FOREGROUND);
    public ImsPhoneCall mBackgroundCall = new ImsPhoneCall(this, ImsPhoneCall.CONTEXT_BACKGROUND);
    public ImsPhoneCall mHandoverCall = new ImsPhoneCall(this, ImsPhoneCall.CONTEXT_HANDOVER);
    private int mClirMode = 0;
    private Object mSyncHold = new Object();
    private ImsCall mUssdSession = null;
    private Message mPendingUssd = null;
    private boolean mDesiredMute = false;
    private boolean mOnHoldToneStarted = false;
    private int mOnHoldToneId = -1;
    private PhoneConstants.State mState = PhoneConstants.State.IDLE;
    private int mServiceId = -1;
    private Call.SrvccState mSrvccState = Call.SrvccState.NONE;
    private boolean mIsInEmergencyCall = false;
    private boolean pendingCallInEcm = false;
    private boolean mSwitchingFgAndBgCalls = false;
    private ImsCall mCallExpectedToResume = null;
    private boolean mDialAsECC = false;
    private boolean mHasPendingResumeRequest = false;
    private final Object mSyncLock = new Object();
    private ImsCall.Listener mImsCallListener = new ImsCall.Listener() {
        public void onCallProgressing(ImsCall imsCall) {
            ImsPhoneCallTracker.this.log("onCallProgressing");
            ImsPhoneCallTracker.this.mPendingMO = null;
            ImsPhoneCallTracker.this.processCallStateChange(imsCall, Call.State.ALERTING, 0);
            ImsPhoneCallTracker.this.mEventLog.writeOnImsCallProgressing(imsCall.getCallSession());
        }

        public void onCallStarted(ImsCall imsCall) {
            ImsPhoneCallTracker.this.log("onCallStarted");
            ImsPhoneCallTracker.this.mPendingMO = null;
            ImsPhoneCallTracker.this.processCallStateChange(imsCall, Call.State.ACTIVE, 0);
            ImsPhoneCallTracker.this.mEventLog.writeOnImsCallStarted(imsCall.getCallSession());
        }

        public void onCallUpdated(ImsCall imsCall) {
            ImsPhoneConnection conn;
            ImsPhoneCallTracker.this.log("onCallUpdated");
            if (imsCall == null || (conn = ImsPhoneCallTracker.this.findConnection(imsCall)) == null) {
                return;
            }
            ImsPhoneCallTracker.this.processCallStateChange(imsCall, conn.getCall().mState, 0, true);
            ImsPhoneCallTracker.this.mEventLog.writeImsCallState(imsCall.getCallSession(), conn.getCall().mState);
        }

        public void onCallStartFailed(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            ImsPhoneCallTracker.this.log("onCallStartFailed reasonCode=" + reasonInfo.getCode());
            if (ImsPhoneCallTracker.this.mPendingMO == null) {
                return;
            }
            if (reasonInfo.getCode() == 146 && ImsPhoneCallTracker.this.mBackgroundCall.getState() == Call.State.IDLE && ImsPhoneCallTracker.this.mRingingCall.getState() == Call.State.IDLE) {
                ImsPhoneCallTracker.this.mForegroundCall.detach(ImsPhoneCallTracker.this.mPendingMO);
                ImsPhoneCallTracker.this.removeConnection(ImsPhoneCallTracker.this.mPendingMO);
                ImsPhoneCallTracker.this.mPendingMO.finalize();
                ImsPhoneCallTracker.this.mPendingMO = null;
                ImsPhoneCallTracker.this.mPhone.initiateSilentRedial();
                return;
            }
            ImsPhoneCallTracker.this.mPendingMO = null;
            int cause = ImsPhoneCallTracker.this.getDisconnectCauseFromReasonInfo(reasonInfo);
            if (cause == 380) {
                ImsPhoneCallTracker.this.mDialAsECC = true;
            }
            ImsPhoneConnection conn = ImsPhoneCallTracker.this.findConnection(imsCall);
            if (conn != null) {
                conn.setVendorDisconnectCause(reasonInfo.getExtraMessage());
            }
            ImsPhoneCallTracker.this.processCallStateChange(imsCall, Call.State.DISCONNECTED, cause);
            ImsPhoneCallTracker.this.mEventLog.writeOnImsCallStartFailed(imsCall.getCallSession(), reasonInfo);
        }

        public void onCallTerminated(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            ImsPhoneCallTracker.this.log("onCallTerminated reasonCode=" + reasonInfo.getCode());
            ImsPhoneCallTracker.this.mForegroundCall.getState();
            int cause = ImsPhoneCallTracker.this.getDisconnectCauseFromReasonInfo(reasonInfo);
            ImsPhoneConnection conn = ImsPhoneCallTracker.this.findConnection(imsCall);
            ImsPhoneCallTracker.this.log("cause = " + cause + " conn = " + conn);
            if (ImsPhoneCallTracker.this.mOnHoldToneId == System.identityHashCode(conn)) {
                if (conn != null && ImsPhoneCallTracker.this.mOnHoldToneStarted) {
                    ImsPhoneCallTracker.this.mPhone.stopOnHoldTone(conn);
                }
                ImsPhoneCallTracker.this.mOnHoldToneStarted = false;
                ImsPhoneCallTracker.this.mOnHoldToneId = -1;
            }
            if (conn != null && conn.isIncoming() && conn.getConnectTime() == 0) {
                if (cause == 2) {
                    cause = 1;
                } else {
                    cause = 16;
                }
                ImsPhoneCallTracker.this.log("Incoming connection of 0 connect time detected - translated cause = " + cause);
            }
            if (cause == 36 && conn != null && conn.getImsCall().isMerged()) {
                cause = 45;
            }
            if (cause == 380) {
                ImsPhoneCallTracker.this.mDialAsECC = true;
            }
            if (conn != null && ImsPhoneCallTracker.this.isVendorDisconnectCauseNeeded(reasonInfo)) {
                conn.setVendorDisconnectCause(reasonInfo.getExtraMessage());
            }
            ImsPhoneCallTracker.this.mEventLog.writeOnImsCallTerminated(imsCall.getCallSession(), reasonInfo);
            ImsPhoneCallTracker.this.processCallStateChange(imsCall, Call.State.DISCONNECTED, cause);
            if (ImsPhoneCallTracker.this.mForegroundCall.getState() != Call.State.ACTIVE) {
                if (ImsPhoneCallTracker.this.mRingingCall.getState().isRinging()) {
                    ImsPhoneCallTracker.this.mPendingMO = null;
                } else if (ImsPhoneCallTracker.this.mPendingMO != null) {
                    ImsPhoneCallTracker.this.sendEmptyMessage(20);
                }
            }
            if (!ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls) {
                return;
            }
            ImsPhoneCallTracker.this.log("onCallTerminated: Call terminated in the midst of Switching Fg and Bg calls.");
            if (imsCall == ImsPhoneCallTracker.this.mCallExpectedToResume) {
                ImsPhoneCallTracker.this.log("onCallTerminated: switching " + ImsPhoneCallTracker.this.mForegroundCall + " with " + ImsPhoneCallTracker.this.mBackgroundCall);
                ImsPhoneCallTracker.this.mForegroundCall.switchWith(ImsPhoneCallTracker.this.mBackgroundCall);
            }
            if (ImsPhoneCallTracker.this.mForegroundCall.getState() != Call.State.HOLDING) {
                return;
            }
            ImsPhoneCallTracker.this.sendEmptyMessage(19);
            ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls = false;
            ImsPhoneCallTracker.this.mCallExpectedToResume = null;
        }

        public void onCallHeld(ImsCall imsCall) {
            if (ImsPhoneCallTracker.this.mForegroundCall.getImsCall() == imsCall) {
                ImsPhoneCallTracker.this.log("onCallHeld (fg) " + imsCall);
            } else if (ImsPhoneCallTracker.this.mBackgroundCall.getImsCall() == imsCall) {
                ImsPhoneCallTracker.this.log("onCallHeld (bg) " + imsCall);
            }
            synchronized (ImsPhoneCallTracker.this.mSyncHold) {
                Call.State oldState = ImsPhoneCallTracker.this.mBackgroundCall.getState();
                ImsPhoneCallTracker.this.processCallStateChange(imsCall, Call.State.HOLDING, 0);
                if (oldState == Call.State.ACTIVE) {
                    if (ImsPhoneCallTracker.this.mForegroundCall.getState() == Call.State.HOLDING || ImsPhoneCallTracker.this.mRingingCall.getState() == Call.State.WAITING) {
                        ImsPhoneCallTracker.this.sendEmptyMessage(19);
                    } else {
                        if (ImsPhoneCallTracker.this.mPendingMO != null) {
                            ImsPhoneCallTracker.this.dialPendingMO();
                        }
                        ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls = false;
                    }
                } else if (oldState == Call.State.IDLE && ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls && ImsPhoneCallTracker.this.mForegroundCall.getState() == Call.State.HOLDING) {
                    ImsPhoneCallTracker.this.sendEmptyMessage(19);
                    ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls = false;
                    ImsPhoneCallTracker.this.mCallExpectedToResume = null;
                }
            }
            ImsPhoneCallTracker.this.mEventLog.writeOnImsCallHeld(imsCall.getCallSession());
        }

        public void onCallHoldFailed(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            ImsPhoneCallTracker.this.log("onCallHoldFailed reasonCode=" + reasonInfo.getCode());
            synchronized (ImsPhoneCallTracker.this.mSyncHold) {
                Call.State bgState = ImsPhoneCallTracker.this.mBackgroundCall.getState();
                if (reasonInfo.getCode() == 148) {
                    if (ImsPhoneCallTracker.this.mForegroundCall.getState() == Call.State.HOLDING || ImsPhoneCallTracker.this.mRingingCall.getState() == Call.State.WAITING) {
                        ImsPhoneCallTracker.this.log("onCallHoldFailed resume background");
                        ImsPhoneCallTracker.this.sendEmptyMessage(19);
                    } else if (ImsPhoneCallTracker.this.mPendingMO != null) {
                        ImsPhoneCallTracker.this.dialPendingMO();
                    }
                } else if (bgState == Call.State.ACTIVE) {
                    ImsPhoneCallTracker.this.mForegroundCall.switchWith(ImsPhoneCallTracker.this.mBackgroundCall);
                    if (ImsPhoneCallTracker.this.mPendingMO != null) {
                        ImsPhoneCallTracker.this.mPendingMO.setDisconnectCause(36);
                        ImsPhoneCallTracker.this.sendEmptyMessageDelayed(18, 500L);
                    }
                }
                ImsPhoneCallTracker.this.mPhone.notifySuppServiceFailed(PhoneInternalInterface.SuppService.HOLD);
            }
            ImsPhoneCallTracker.this.mEventLog.writeOnImsCallHoldFailed(imsCall.getCallSession(), reasonInfo);
        }

        public void onCallResumed(ImsCall imsCall) {
            ImsPhoneCallTracker.this.log("onCallResumed");
            if (ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls) {
                if (imsCall != ImsPhoneCallTracker.this.mCallExpectedToResume) {
                    ImsPhoneCallTracker.this.log("onCallResumed : switching " + ImsPhoneCallTracker.this.mForegroundCall + " with " + ImsPhoneCallTracker.this.mBackgroundCall);
                    ImsPhoneCallTracker.this.mForegroundCall.switchWith(ImsPhoneCallTracker.this.mBackgroundCall);
                } else {
                    ImsPhoneCallTracker.this.log("onCallResumed : expected call resumed.");
                }
                ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls = false;
                ImsPhoneCallTracker.this.mCallExpectedToResume = null;
            }
            ImsPhoneCallTracker.this.mHasPendingResumeRequest = false;
            ImsPhoneCallTracker.this.processCallStateChange(imsCall, Call.State.ACTIVE, 0);
            ImsPhoneCallTracker.this.mEventLog.writeOnImsCallResumed(imsCall.getCallSession());
        }

        public void onCallResumeFailed(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            ImsPhoneCallTracker.this.log("onCallResumeFailed");
            if (ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls) {
                if (imsCall == ImsPhoneCallTracker.this.mCallExpectedToResume) {
                    ImsPhoneCallTracker.this.log("onCallResumeFailed : switching " + ImsPhoneCallTracker.this.mForegroundCall + " with " + ImsPhoneCallTracker.this.mBackgroundCall);
                    ImsPhoneCallTracker.this.mForegroundCall.switchWith(ImsPhoneCallTracker.this.mBackgroundCall);
                    if (ImsPhoneCallTracker.this.mForegroundCall.getState() == Call.State.HOLDING) {
                        ImsPhoneCallTracker.this.sendEmptyMessage(19);
                    }
                }
                ImsPhoneCallTracker.this.mCallExpectedToResume = null;
                ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls = false;
            }
            ImsPhoneCallTracker.this.mHasPendingResumeRequest = false;
            ImsPhoneCallTracker.this.mPhone.notifySuppServiceFailed(PhoneInternalInterface.SuppService.RESUME);
            ImsPhoneCallTracker.this.mEventLog.writeOnImsCallResumeFailed(imsCall.getCallSession(), reasonInfo);
        }

        public void onCallResumeReceived(ImsCall imsCall) {
            ImsPhoneCallTracker.this.log("onCallResumeReceived");
            ImsPhoneConnection conn = ImsPhoneCallTracker.this.findConnection(imsCall);
            if (conn != null && ImsPhoneCallTracker.this.mOnHoldToneStarted) {
                ImsPhoneCallTracker.this.mPhone.stopOnHoldTone(conn);
                ImsPhoneCallTracker.this.mOnHoldToneStarted = false;
            }
            if (conn != null) {
                conn.notifyRemoteHeld(false);
            }
            SuppServiceNotification supp = new SuppServiceNotification();
            supp.notificationType = 1;
            supp.code = 3;
            ImsPhoneCallTracker.this.mPhone.notifySuppSvcNotification(supp);
            ImsPhoneCallTracker.this.mEventLog.writeOnImsCallResumeReceived(imsCall.getCallSession());
        }

        public void onCallHoldReceived(ImsCall imsCall) {
            ImsPhoneCallTracker.this.log("onCallHoldReceived");
            ImsPhoneConnection conn = ImsPhoneCallTracker.this.findConnection(imsCall);
            if (conn != null && conn.getState() == Call.State.ACTIVE && !ImsPhoneCallTracker.this.mOnHoldToneStarted && ImsPhoneCall.isLocalTone(imsCall)) {
                ImsPhoneCallTracker.this.mPhone.startOnHoldTone(conn);
                ImsPhoneCallTracker.this.mOnHoldToneStarted = true;
                ImsPhoneCallTracker.this.mOnHoldToneId = System.identityHashCode(conn);
            }
            if (conn != null) {
                conn.notifyRemoteHeld(true);
            }
            SuppServiceNotification supp = new SuppServiceNotification();
            supp.notificationType = 1;
            supp.code = 2;
            ImsPhoneCallTracker.this.mPhone.notifySuppSvcNotification(supp);
            ImsPhoneCallTracker.this.mEventLog.writeOnImsCallHoldReceived(imsCall.getCallSession());
        }

        public void onCallSuppServiceReceived(ImsCall call, ImsSuppServiceNotification suppServiceInfo) {
            ImsPhoneCallTracker.this.log("onCallSuppServiceReceived: suppServiceInfo=" + suppServiceInfo);
            SuppServiceNotification supp = new SuppServiceNotification();
            supp.notificationType = suppServiceInfo.notificationType;
            supp.code = suppServiceInfo.code;
            supp.index = suppServiceInfo.index;
            supp.number = suppServiceInfo.number;
            supp.history = suppServiceInfo.history;
            ImsPhoneCallTracker.this.mPhone.notifySuppSvcNotification(supp);
        }

        public void onCallMerged(ImsCall call, ImsCall peerCall, boolean swapCalls) {
            FormattedLog formattedLog;
            ImsPhoneCallTracker.this.log("onCallMerged");
            ImsPhoneCall foregroundImsPhoneCall = ImsPhoneCallTracker.this.findConnection(call).getCall();
            ImsPhoneConnection peerConnection = ImsPhoneCallTracker.this.findConnection(peerCall);
            ImsPhoneCall call2 = peerConnection == null ? null : peerConnection.getCall();
            if (swapCalls) {
                ImsPhoneCallTracker.this.switchAfterConferenceSuccess();
            }
            foregroundImsPhoneCall.merge(call2, Call.State.ACTIVE);
            try {
                ImsPhoneConnection conn = ImsPhoneCallTracker.this.findConnection(call);
                ImsPhoneCallTracker.this.log("onCallMerged: ImsPhoneConnection=" + conn);
                ImsPhoneCallTracker.this.log("onCallMerged: CurrentVideoProvider=" + conn.getVideoProvider());
                ImsPhoneCallTracker.this.setVideoCallProvider(conn, call);
                ImsPhoneCallTracker.this.log("onCallMerged: CurrentVideoProvider=" + conn.getVideoProvider());
            } catch (Exception e) {
                ImsPhoneCallTracker.this.loge("onCallMerged: exception " + e);
            }
            ImsPhoneCallTracker.this.processCallStateChange(ImsPhoneCallTracker.this.mForegroundCall.getImsCall(), Call.State.ACTIVE, 0);
            if (peerConnection != null) {
                ImsPhoneCallTracker.this.processCallStateChange(ImsPhoneCallTracker.this.mBackgroundCall.getImsCall(), Call.State.HOLDING, 0);
            }
            if (call.isMergeRequestedByConf()) {
                ImsPhoneCallTracker.this.log("onCallMerged :: Merge requested by existing conference.");
                call.resetIsMergeRequestedByConf(false);
            } else {
                ImsPhoneCallTracker.this.log("onCallMerged :: calling onMultipartyStateChanged()");
                onMultipartyStateChanged(call, true);
            }
            ImsPhoneCallTracker.this.logState();
            ImsPhoneConnection hostConn = ImsPhoneCallTracker.this.findConnection(call);
            if (hostConn == null || (formattedLog = new FormattedLog.Builder().setCategory("CC").setServiceName("ImsPhone").setOpType(FormattedLog.OpType.DUMP).setCallNumber(hostConn.getAddress()).setCallId(ImsPhoneCallTracker.this.getConnectionCallId(hostConn)).setStatusInfo("state", ConferenceCallMessageHandler.STATUS_DISCONNECTED).setStatusInfo("isConfCall", "No").setStatusInfo("isConfChildCall", "No").setStatusInfo("parent", hostConn.getParentCallName()).buildDumpInfo()) == null) {
                return;
            }
            ImsPhoneCallTracker.this.log(formattedLog.toString());
        }

        public void onCallMergeFailed(ImsCall call, ImsReasonInfo reasonInfo) {
            ImsPhoneCallTracker.this.log("onCallMergeFailed reasonInfo=" + reasonInfo);
            ImsPhoneCallTracker.this.mPhone.notifySuppServiceFailed(PhoneInternalInterface.SuppService.CONFERENCE);
            ImsPhoneConnection conn = ImsPhoneCallTracker.this.findConnection(call);
            if (conn == null) {
                return;
            }
            conn.onConferenceMergeFailed();
        }

        public void onConferenceParticipantsStateChanged(ImsCall call, List<ConferenceParticipant> participants) {
            ImsPhoneCallTracker.this.log("onConferenceParticipantsStateChanged");
            ImsPhoneConnection conn = ImsPhoneCallTracker.this.findConnection(call);
            if (conn == null) {
                return;
            }
            conn.updateConferenceParticipants(participants);
        }

        public void onCallSessionTtyModeReceived(ImsCall call, int mode) {
            ImsPhoneCallTracker.this.mPhone.onTtyModeReceived(mode);
        }

        public void onCallHandover(ImsCall imsCall, int srcAccessTech, int targetAccessTech, ImsReasonInfo reasonInfo) {
            ImsPhoneCallTracker.this.log("onCallHandover ::  srcAccessTech=" + srcAccessTech + ", targetAccessTech=" + targetAccessTech + ", reasonInfo=" + reasonInfo);
            ImsPhoneCallTracker.this.mEventLog.writeOnImsCallHandover(imsCall.getCallSession(), srcAccessTech, targetAccessTech, reasonInfo);
        }

        public void onCallHandoverFailed(ImsCall imsCall, int srcAccessTech, int targetAccessTech, ImsReasonInfo reasonInfo) {
            ImsPhoneCallTracker.this.log("onCallHandoverFailed :: srcAccessTech=" + srcAccessTech + ", targetAccessTech=" + targetAccessTech + ", reasonInfo=" + reasonInfo);
            ImsPhoneCallTracker.this.mEventLog.writeOnImsCallHandoverFailed(imsCall.getCallSession(), srcAccessTech, targetAccessTech, reasonInfo);
        }

        public void onMultipartyStateChanged(ImsCall imsCall, boolean isMultiParty) {
            ImsPhoneCallTracker.this.log("onMultipartyStateChanged to " + (isMultiParty ? "Y" : "N"));
            ImsPhoneConnection conn = ImsPhoneCallTracker.this.findConnection(imsCall);
            if (conn == null) {
                return;
            }
            conn.updateMultipartyState(isMultiParty);
        }

        public void onCallInviteParticipantsRequestDelivered(ImsCall call) {
            ImsPhoneCallTracker.this.log("onCallInviteParticipantsRequestDelivered");
            ImsPhoneConnection conn = ImsPhoneCallTracker.this.findConnection(call);
            if (conn == null) {
                return;
            }
            conn.notifyConferenceParticipantsInvited(true);
        }

        public void onCallInviteParticipantsRequestFailed(ImsCall call, ImsReasonInfo reasonInfo) {
            ImsPhoneCallTracker.this.log("onCallInviteParticipantsRequestFailed reasonCode=" + reasonInfo.getCode());
            ImsPhoneConnection conn = ImsPhoneCallTracker.this.findConnection(call);
            if (conn == null) {
                return;
            }
            conn.notifyConferenceParticipantsInvited(false);
        }

        public void onCallTransferred(ImsCall imsCall) {
            ImsPhoneCallTracker.this.log("onCallTransferred");
        }

        public void onCallTransferFailed(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            ImsPhoneCallTracker.this.log("onCallTransferFailed");
            ImsPhoneCallTracker.this.mPhone.notifySuppServiceFailed(PhoneInternalInterface.SuppService.TRANSFER);
        }
    };
    private ImsCall.Listener mImsUssdListener = new ImsCall.Listener() {
        public void onCallStarted(ImsCall imsCall) {
            ImsPhoneCallTracker.this.log("mImsUssdListener onCallStarted");
            if (imsCall != ImsPhoneCallTracker.this.mUssdSession || ImsPhoneCallTracker.this.mPendingUssd == null) {
                return;
            }
            AsyncResult.forMessage(ImsPhoneCallTracker.this.mPendingUssd);
            ImsPhoneCallTracker.this.mPendingUssd.sendToTarget();
            ImsPhoneCallTracker.this.mPendingUssd = null;
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
                    if (call == ImsPhoneCallTracker.this.mUssdSession) {
                        ImsPhoneCallTracker.this.mUssdSession = null;
                        call.close();
                    }
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
            ImsPhoneCallTracker.this.log("onImsConnected");
            ImsPhoneCallTracker.this.mPhone.setServiceState(0);
            ImsPhoneCallTracker.this.mPhone.setImsRegistered(true);
            ImsPhoneCallTracker.this.mEventLog.writeOnImsConnectionState(1, null);
        }

        public void onImsDisconnected(ImsReasonInfo imsReasonInfo) {
            ImsPhoneCallTracker.this.log("onImsDisconnected imsReasonInfo=" + imsReasonInfo);
            ImsPhoneCallTracker.this.mPhone.setServiceState(1);
            ImsPhoneCallTracker.this.mPhone.setImsRegistered(false);
            ImsPhoneCallTracker.this.mPhone.processDisconnectReason(imsReasonInfo);
            ImsPhoneCallTracker.this.mEventLog.writeOnImsConnectionState(3, imsReasonInfo);
            if (imsReasonInfo == null || imsReasonInfo.getExtraMessage() == null || imsReasonInfo.getExtraMessage().equals(UsimPBMemInfo.STRING_NOT_SET)) {
                return;
            }
            ImsPhoneCallTracker.this.mImsRegistrationErrorCode = Integer.parseInt(imsReasonInfo.getExtraMessage());
        }

        public void onImsProgressing() {
            ImsPhoneCallTracker.this.log("onImsProgressing");
            ImsPhoneCallTracker.this.mPhone.setServiceState(1);
            ImsPhoneCallTracker.this.mPhone.setImsRegistered(false);
            ImsPhoneCallTracker.this.mEventLog.writeOnImsConnectionState(2, null);
        }

        public void onImsResumed() {
            ImsPhoneCallTracker.this.log("onImsResumed");
            ImsPhoneCallTracker.this.mPhone.setServiceState(0);
            ImsPhoneCallTracker.this.mEventLog.writeOnImsConnectionState(4, null);
        }

        public void onImsSuspended() {
            ImsPhoneCallTracker.this.log("onImsSuspended");
            ImsPhoneCallTracker.this.mPhone.setServiceState(1);
            ImsPhoneCallTracker.this.mEventLog.writeOnImsConnectionState(5, null);
        }

        public void onFeatureCapabilityChanged(int serviceClass, int[] enabledFeatures, int[] disabledFeatures) {
            if (serviceClass == 1) {
                boolean tmpIsVideoCallEnabled = ImsPhoneCallTracker.this.isVideoCallEnabled();
                StringBuilder sb = new StringBuilder(120);
                sb.append("onFeatureCapabilityChanged: ");
                for (int i = 0; i <= 5 && i < enabledFeatures.length; i++) {
                    if (enabledFeatures[i] == i) {
                        sb.append(ImsPhoneCallTracker.this.mImsFeatureStrings[i]);
                        sb.append(":true ");
                        ImsPhoneCallTracker.this.mImsFeatureEnabled[i] = true;
                    } else if (enabledFeatures[i] == -1) {
                        sb.append(ImsPhoneCallTracker.this.mImsFeatureStrings[i]);
                        sb.append(":false ");
                        ImsPhoneCallTracker.this.mImsFeatureEnabled[i] = false;
                    } else {
                        ImsPhoneCallTracker.this.loge("onFeatureCapabilityChanged(" + i + ", " + ImsPhoneCallTracker.this.mImsFeatureStrings[i] + "): unexpectedValue=" + enabledFeatures[i]);
                    }
                }
                ImsPhoneCallTracker.this.log(sb.toString());
                if (tmpIsVideoCallEnabled != ImsPhoneCallTracker.this.isVideoCallEnabled()) {
                    ImsPhoneCallTracker.this.mPhone.notifyForVideoCapabilityChanged(ImsPhoneCallTracker.this.isVideoCallEnabled());
                }
                ImsPhoneCallTracker.this.log("onFeatureCapabilityChanged: isVolteEnabled=" + ImsPhoneCallTracker.this.isVolteEnabled() + ", isVideoCallEnabled=" + ImsPhoneCallTracker.this.isVideoCallEnabled() + ", isVowifiEnabled=" + ImsPhoneCallTracker.this.isVowifiEnabled() + ", isUtEnabled=" + ImsPhoneCallTracker.this.isUtEnabled());
                for (ImsPhoneConnection connection : ImsPhoneCallTracker.this.mConnections) {
                    connection.updateWifiState();
                }
                ImsPhoneCallTracker.this.mPhone.onFeatureCapabilityChanged();
                ImsPhoneCallTracker.this.mEventLog.writeOnImsCapabilities(ImsPhoneCallTracker.this.mImsFeatureEnabled);
                ImsPhoneCallTracker.this.broadcastImsStatusChange();
            }
        }

        public void onVoiceMessageCountChanged(int count) {
            ImsPhoneCallTracker.this.log("onVoiceMessageCountChanged :: count=" + count);
            ImsPhoneCallTracker.this.mPhone.mDefaultPhone.setVoiceMessageCount(count);
        }
    };
    private ImsConfigListener.Stub mImsConfigListener = new ImsConfigListener.Stub() {
        public void onGetFeatureResponse(int feature, int network, int value, int status) {
        }

        public void onSetFeatureResponse(int feature, int network, int value, int status) {
            ImsPhoneCallTracker.this.mEventLog.writeImsSetFeatureValue(feature, network, value, status);
        }

        public void onGetVideoQuality(int status, int quality) {
        }

        public void onSetVideoQuality(int status) {
        }
    };
    private BroadcastReceiver mIndicationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals("com.android.ims.IMS_INCOMING_CALL_INDICATION")) {
                return;
            }
            ImsPhoneCallTracker.this.log("onReceive : indication call intent");
            if (ImsPhoneCallTracker.this.mImsManager == null) {
                ImsPhoneCallTracker.this.log("no ims manager");
                return;
            }
            boolean isAllow = true;
            int serviceId = intent.getIntExtra("android:imsServiceId", -1);
            if (serviceId != ImsPhoneCallTracker.this.mServiceId) {
                return;
            }
            String callWaitingSetting = TelephonyManager.getTelephonyProperty(ImsPhoneCallTracker.this.mPhone.getPhoneId(), "persist.radio.terminal-based.cw", "disabled_tbcw");
            if (callWaitingSetting.equals("enabled_tbcw_off") && ImsPhoneCallTracker.this.mForegroundCall != null && ImsPhoneCallTracker.this.mForegroundCall.getState() == Call.State.ACTIVE) {
                ImsPhoneCallTracker.this.log("PROPERTY_TERMINAL_BASED_CALL_WAITING_MODE = TERMINAL_BASED_CALL_WAITING_ENABLED_OFF. Reject the call as UDUB ");
                isAllow = false;
            }
            if (ImsPhoneCallTracker.this.isEccExist()) {
                ImsPhoneCallTracker.this.log("there is an ECC call, dis-allow this incoming call!");
                isAllow = false;
            }
            if (ImsPhoneCallTracker.this.hasVideoCallRestriction(context, intent)) {
                isAllow = false;
                ImsPhoneCallTracker.this.addCallLog(context, intent);
            }
            ImsPhoneCallTracker.this.log("setCallIndication : serviceId = " + serviceId + ", intent = " + intent + ", isAllow = " + isAllow);
            try {
                ImsPhoneCallTracker.this.mImsManager.setCallIndication(ImsPhoneCallTracker.this.mServiceId, intent, isAllow);
            } catch (ImsException e) {
                ImsPhoneCallTracker.this.loge("setCallIndication ImsException " + e);
            }
        }
    };

    public ImsPhoneCallTracker(ImsPhone phone) {
        this.mAllowEmergencyVideoCalls = false;
        this.mPhone = phone;
        this.mEventLog = new TelephonyEventLog(this.mPhone.getPhoneId());
        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction("com.android.ims.IMS_INCOMING_CALL");
        intentfilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        this.mPhone.getContext().registerReceiver(this.mReceiver, intentfilter);
        this.mAllowEmergencyVideoCalls = isEmergencyVtCallAllowed(this.mPhone.getSubId());
        registerIndicationReceiver();
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
        synchronized (this.mSyncLock) {
            this.mImsManager = ImsManager.getInstance(this.mPhone.getContext(), this.mPhone.getPhoneId());
            try {
                this.mServiceId = this.mImsManager.open(1, createIncomingCallPendingIntent(), this.mImsConnectionStateListener);
                this.mImsManager.setImsConfigListener(this.mImsConfigListener);
                getEcbmInterface().setEcbmStateListener(this.mPhone.getImsEcbmStateListener());
                if (this.mPhone.isInEcm()) {
                    this.mPhone.exitEmergencyCallbackMode();
                }
                int mPreferredTtyMode = Settings.Secure.getInt(this.mPhone.getContext().getContentResolver(), "preferred_tty_mode", 0);
                this.mImsManager.setUiTTYMode(this.mPhone.getContext(), this.mServiceId, mPreferredTtyMode, (Message) null);
            } catch (ImsException e) {
                loge("getImsService: " + e);
                this.mImsManager = null;
            }
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
        unregisterIndicationReceiver();
        synchronized (this.mSyncLock) {
            if (this.mImsManager != null && this.mServiceId != -1) {
                try {
                    this.mImsManager.close(this.mServiceId);
                } catch (ImsException e) {
                    loge("getImsService: " + e);
                }
                this.mServiceId = -1;
                this.mImsManager = null;
            }
        }
        this.mPhone.setServiceState(1);
        this.mPhone.setImsRegistered(false);
        for (int i = 0; i <= 3; i++) {
            this.mImsFeatureEnabled[i] = false;
        }
        this.mPhone.onFeatureCapabilityChanged();
        broadcastImsStatusChange();
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

    public Connection dial(String dialString, int videoState, Bundle intentExtras) throws CallStateException {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mPhone.getContext());
        int oirMode = sp.getInt(Phone.CLIR_KEY + this.mPhone.getPhoneId(), 0);
        return dial(dialString, oirMode, videoState, intentExtras);
    }

    synchronized Connection dial(String dialString, int clirMode, int videoState, Bundle intentExtras) throws CallStateException {
        boolean isPhoneInEcmMode = isPhoneInEcbMode();
        boolean isEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(this.mPhone.getSubId(), dialString);
        log("dial clirMode=" + clirMode);
        clearDisconnected();
        if (this.mImsManager == null) {
            throw new CallStateException("service not available");
        }
        if (this.mHandoverCall.mConnections.size() > 0) {
            log("SRVCC: there are connections during handover, trigger CSFB!");
            throw new CallStateException(Phone.CS_FALLBACK);
        }
        if (this.mPhone != null && this.mPhone.getDefaultPhone() != null) {
            Phone defaultPhone = this.mPhone.getDefaultPhone();
            if (defaultPhone.getState() != PhoneConstants.State.IDLE && getState() == PhoneConstants.State.IDLE) {
                log("There are CS connections, trigger CSFB!");
                throw new CallStateException(Phone.CS_FALLBACK);
            }
        }
        if (!canDial()) {
            throw new CallStateException("cannot dial in current state");
        }
        if (isPhoneInEcmMode && isEmergencyNumber) {
            handleEcmTimer(1);
        }
        if (isEmergencyNumber && VideoProfile.isVideo(videoState) && !this.mAllowEmergencyVideoCalls) {
            loge("dial: carrier does not support video emergency calls; downgrade to audio-only");
            videoState = 0;
        }
        boolean holdBeforeDial = false;
        if (this.mForegroundCall.getState() == Call.State.ACTIVE) {
            if (this.mBackgroundCall.getState() != Call.State.IDLE) {
                throw new CallStateException("cannot dial in current state");
            }
            holdBeforeDial = true;
            this.mPendingCallVideoState = videoState;
            this.mPendingIntentExtras = intentExtras;
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
                this.mPendingMO = new ImsPhoneConnection(this.mPhone, checkForTestEmergencyNumber(dialString), this, this.mForegroundCall, isEmergencyNumber);
                this.mPendingMO.setVideoState(videoState);
            } else {
                this.mPendingMO = new ImsPhoneConnection(this.mPhone, checkForTestEmergencyNumber(dialString), this, this.mForegroundCall, isEmergencyNumber);
                this.mPendingMO.setVideoState(videoState);
            }
        }
        addConnection(this.mPendingMO);
        log("IMS: dial() holdBeforeDial = " + holdBeforeDial + " isPhoneInEcmMode = " + isPhoneInEcmMode + " isEmergencyNumber = " + isEmergencyNumber);
        logDebugMessagesWithOpFormat("CC", "Dial", this.mPendingMO, UsimPBMemInfo.STRING_NOT_SET);
        logDebugMessagesWithDumpFormat("CC", this.mPendingMO, UsimPBMemInfo.STRING_NOT_SET);
        if (!holdBeforeDial) {
            if (!isPhoneInEcmMode || (isPhoneInEcmMode && isEmergencyNumber)) {
                dialInternal(this.mPendingMO, clirMode, videoState, intentExtras);
            } else {
                try {
                    getEcbmInterface().exitEmergencyCallbackMode();
                    this.mPhone.setOnEcbModeExitResponse(this, 14, null);
                    this.pendingCallClirMode = clirMode;
                    this.mPendingCallVideoState = videoState;
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

    private boolean isEmergencyVtCallAllowed(int subId) {
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        if (carrierConfigManager == null) {
            loge("isEmergencyVideoCallsSupported: No carrier config service found.");
            return false;
        }
        PersistableBundle carrierConfig = carrierConfigManager.getConfigForSubId(subId);
        if (carrierConfig == null) {
            loge("isEmergencyVideoCallsSupported: Empty carrier config.");
            return false;
        }
        return carrierConfig.getBoolean("allow_emergency_video_calls_bool");
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

    private void dialInternal(ImsPhoneConnection conn, int clirMode, int videoState, Bundle intentExtras) {
        String[] callees;
        if (conn == null) {
            return;
        }
        if (conn.getConfDialStrings() == null && (conn.getAddress() == null || conn.getAddress().length() == 0 || conn.getAddress().indexOf(78) >= 0)) {
            conn.setDisconnectCause(7);
            sendEmptyMessageDelayed(18, 500L);
            return;
        }
        setMute(false);
        int serviceType = PhoneNumberUtils.isEmergencyNumber(this.mPhone.getSubId(), conn.getAddress()) ? 2 : 1;
        if (serviceType == 2 && PhoneNumberUtils.isSpecialEmergencyNumber(conn.getAddress())) {
            serviceType = 1;
        }
        if (this.mDialAsECC) {
            serviceType = 2;
            log("Dial as ECC: conn.getAddress(): " + conn.getAddress());
            this.mDialAsECC = false;
        }
        int callType = ImsCallProfile.getCallTypeFromVideoState(videoState);
        conn.setVideoState(videoState);
        try {
            if (conn.getConfDialStrings() != null) {
                ArrayList<String> dialStrings = conn.getConfDialStrings();
                callees = (String[]) dialStrings.toArray(new String[dialStrings.size()]);
            } else {
                String[] callees2 = {conn.getAddress()};
                callees = callees2;
            }
            ImsCallProfile profile = this.mImsManager.createCallProfile(this.mServiceId, serviceType, callType);
            profile.setCallExtraInt("oir", clirMode);
            if (conn.getConfDialStrings() != null) {
                profile.setCallExtraBoolean("conference", true);
            }
            if (intentExtras != null) {
                if (intentExtras.containsKey("android.telecom.extra.CALL_SUBJECT")) {
                    intentExtras.putString("DisplayText", cleanseInstantLetteringMessage(intentExtras.getString("android.telecom.extra.CALL_SUBJECT")));
                }
                profile.mCallExtras.putBundle("OemCallExtras", intentExtras);
            }
            ImsCall imsCall = this.mImsManager.makeCall(this.mServiceId, profile, callees, this.mImsCallListener);
            conn.setImsCall(imsCall);
            if (callees != null && callees.length > 0) {
                this.mEventLog.writeOnImsCallStart(imsCall.getSession(), callees[0]);
            }
            setVideoCallProvider(conn, imsCall);
        } catch (RemoteException e) {
        } catch (ImsException e2) {
            loge("dialInternal : " + e2);
            conn.setDisconnectCause(36);
            sendEmptyMessageDelayed(18, 500L);
        }
    }

    public void acceptCall(int videoState) throws CallStateException {
        if (this.mSrvccState == Call.SrvccState.STARTED || this.mSrvccState == Call.SrvccState.COMPLETED) {
            throw new CallStateException(2, "cannot accept call: SRVCC");
        }
        logDebugMessagesWithOpFormat("CC", "Answer", this.mRingingCall.getFirstConnection(), UsimPBMemInfo.STRING_NOT_SET);
        log("acceptCall");
        if (this.mForegroundCall.getState().isAlive() && this.mBackgroundCall.getState().isAlive()) {
            throw new CallStateException("cannot accept call");
        }
        if (this.mRingingCall.getState() == Call.State.WAITING && this.mForegroundCall.getState().isAlive()) {
            setMute(false);
            this.mPendingCallVideoState = videoState;
            switchWaitingOrHoldingAndActive();
        } else {
            if (this.mRingingCall.getState().isRinging()) {
                log("acceptCall: incoming...");
                setMute(false);
                try {
                    ImsCall imsCall = this.mRingingCall.getImsCall();
                    if (imsCall != null) {
                        imsCall.accept(ImsCallProfile.getCallTypeFromVideoState(videoState));
                        this.mEventLog.writeOnImsCallAccept(imsCall.getSession());
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

    public void rejectCall() throws CallStateException {
        logDebugMessagesWithOpFormat("CC", "Reject", this.mRingingCall.getFirstConnection(), UsimPBMemInfo.STRING_NOT_SET);
        log("rejectCall");
        if (this.mRingingCall.getState().isRinging()) {
            hangup(this.mRingingCall);
            return;
        }
        throw new CallStateException("phone not ringing");
    }

    private void switchAfterConferenceSuccess() {
        log("switchAfterConferenceSuccess fg =" + this.mForegroundCall.getState() + ", bg = " + this.mBackgroundCall.getState());
        if (this.mBackgroundCall.getState() != Call.State.HOLDING) {
            return;
        }
        log("switchAfterConferenceSuccess");
        this.mForegroundCall.switchWith(this.mBackgroundCall);
    }

    public void switchWaitingOrHoldingAndActive() throws CallStateException {
        ImsPhoneConnection conn;
        String msg;
        if (this.mSrvccState == Call.SrvccState.STARTED || this.mSrvccState == Call.SrvccState.COMPLETED) {
            throw new CallStateException(2, "cannot hold/unhold call: SRVCC");
        }
        log("switchWaitingOrHoldingAndActive");
        if (this.mForegroundCall.getState() == Call.State.ACTIVE) {
            conn = this.mForegroundCall.getFirstConnection();
            if (this.mBackgroundCall.getState().isAlive()) {
                msg = "switch with background connection:" + this.mBackgroundCall.getFirstConnection();
            } else {
                msg = "hold to background";
            }
        } else {
            conn = this.mBackgroundCall.getFirstConnection();
            msg = "unhold to foreground";
        }
        logDebugMessagesWithOpFormat("CC", "Swap", conn, msg);
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
                this.mEventLog.writeOnImsCallHold(imsCall.getSession());
                if (this.mCallExpectedToResume != null) {
                    return;
                }
                this.mSwitchingFgAndBgCalls = false;
                return;
            } catch (ImsException e) {
                this.mForegroundCall.switchWith(this.mBackgroundCall);
                this.mSwitchingFgAndBgCalls = false;
                this.mCallExpectedToResume = null;
                throw new CallStateException(e.getMessage());
            }
        }
        if (this.mBackgroundCall.getState() != Call.State.HOLDING) {
            return;
        }
        resumeWaitingOrHolding();
    }

    public void conference() {
        long conferenceConnectTime;
        logDebugMessagesWithOpFormat("CC", "Conference", this.mForegroundCall.getFirstConnection(), " merge with " + this.mBackgroundCall.getFirstConnection());
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
        long foregroundConnectTime = this.mForegroundCall.getEarliestConnectTime();
        long backgroundConnectTime = this.mBackgroundCall.getEarliestConnectTime();
        if (foregroundConnectTime > 0 && backgroundConnectTime > 0) {
            conferenceConnectTime = Math.min(this.mForegroundCall.getEarliestConnectTime(), this.mBackgroundCall.getEarliestConnectTime());
            log("conference - using connect time = " + conferenceConnectTime);
        } else if (foregroundConnectTime > 0) {
            log("conference - bg call connect time is 0; using fg = " + foregroundConnectTime);
            conferenceConnectTime = foregroundConnectTime;
        } else {
            log("conference - fg call connect time is 0; using bg = " + backgroundConnectTime);
            conferenceConnectTime = backgroundConnectTime;
        }
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

    public void explicitCallTransfer() {
        log("explicitCallTransfer");
        ImsCall fgImsCall = this.mForegroundCall.getImsCall();
        if (fgImsCall == null) {
            log("explicitCallTransfer no foreground ims call");
            return;
        }
        ImsCall bgImsCall = this.mBackgroundCall.getImsCall();
        if (bgImsCall == null) {
            log("explicitCallTransfer no background ims call");
            return;
        }
        if (this.mForegroundCall.getState() != Call.State.ACTIVE || this.mBackgroundCall.getState() != Call.State.HOLDING) {
            log("annot transfer call");
            return;
        }
        try {
            fgImsCall.explicitCallTransfer();
        } catch (ImsException e) {
            log("explicitCallTransfer " + e.getMessage());
        }
    }

    public void unattendedCallTransfer(String number, int type) {
        log("unattendedCallTransfer number : " + number + ", type : " + type);
        ImsCall fgImsCall = this.mForegroundCall.getImsCall();
        if (fgImsCall == null) {
            log("explicitCallTransfer no foreground ims call");
            return;
        }
        try {
            fgImsCall.unattendedCallTransfer(number, type);
        } catch (ImsException e) {
            log("explicitCallTransfer " + e.getMessage());
        }
    }

    public void clearDisconnected() {
        log("clearDisconnected");
        internalClearDisconnected();
        updatePhoneState();
        this.mPhone.notifyPreciseCallStateChanged();
    }

    public boolean canConference() {
        return this.mForegroundCall.getState() == Call.State.ACTIVE && this.mBackgroundCall.getState() == Call.State.HOLDING && !this.mBackgroundCall.isFull() && !this.mForegroundCall.isFull();
    }

    public boolean canDial() {
        int serviceState = this.mPhone.getServiceState().getState();
        String disableCall = SystemProperties.get("ro.telephony.disable-call", "false");
        boolean ret = (serviceState == 3 || this.mPendingMO != null || this.mRingingCall.isRinging() || disableCall.equals("true")) ? false : (this.mForegroundCall.getState().isAlive() && this.mBackgroundCall.getState().isAlive()) ? false : true;
        log("IMS: canDial() serviceState = " + serviceState + ", disableCall = " + disableCall + ", mPendingMO = " + this.mPendingMO + ", Is mRingingCall ringing = " + this.mRingingCall.isRinging() + ", Is mForegroundCall alive = " + this.mForegroundCall.getState().isAlive() + ", Is mBackgroundCall alive = " + this.mBackgroundCall.getState().isAlive());
        return ret;
    }

    public boolean canTransfer() {
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
        if (this.mState == oldState) {
            return;
        }
        this.mPhone.notifyPhoneStateChanged();
        this.mEventLog.writePhoneState(this.mState);
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

    public void setUiTTYMode(int uiTtyMode, Message onComplete) {
        try {
            this.mImsManager.setUiTTYMode(this.mPhone.getContext(), this.mServiceId, uiTtyMode, onComplete);
        } catch (ImsException e) {
            loge("setTTYMode : " + e);
            this.mPhone.sendErrorResponse(onComplete, e);
        }
    }

    public void setMute(boolean mute) {
        this.mDesiredMute = mute;
        this.mForegroundCall.setMute(mute);
    }

    public boolean getMute() {
        return this.mDesiredMute;
    }

    public void sendDtmf(char c, Message result) {
        log("sendDtmf");
        ImsCall imscall = this.mForegroundCall.getImsCall();
        if (imscall == null) {
            return;
        }
        imscall.sendDtmf(c, result);
    }

    public void startDtmf(char c) {
        log("startDtmf");
        ImsCall imscall = this.mForegroundCall.getImsCall();
        if (imscall != null) {
            imscall.startDtmf(c);
        } else {
            loge("startDtmf : no foreground call");
        }
    }

    public void stopDtmf() {
        log("stopDtmf");
        ImsCall imscall = this.mForegroundCall.getImsCall();
        if (imscall != null) {
            imscall.stopDtmf();
        } else {
            loge("stopDtmf : no foreground call");
        }
    }

    public void hangup(ImsPhoneConnection conn) throws CallStateException {
        log("hangup connection");
        if (conn.getOwner() != this) {
            throw new CallStateException("ImsPhoneConnection " + conn + "does not belong to ImsPhoneCallTracker " + this);
        }
        hangup(conn.getCall());
    }

    public void hangup(ImsPhoneCall call) throws CallStateException {
        if (this.mSrvccState == Call.SrvccState.STARTED || this.mSrvccState == Call.SrvccState.COMPLETED) {
            throw new CallStateException(2, "cannot hangup call: SRVCC");
        }
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
                    this.mEventLog.writeOnImsCallReject(imsCall.getSession());
                } else {
                    imsCall.terminate(501);
                    this.mEventLog.writeOnImsCallTerminate(imsCall.getSession());
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
        if (this.mHandoverCall.mConnections.size() <= 0) {
            return;
        }
        log("callEndCleanupHandOverCallIfAny, mHandoverCall.mConnections=" + this.mHandoverCall.mConnections);
        for (Connection conn : this.mHandoverCall.mConnections) {
            log("SRVCC: remove connection=" + conn);
            removeConnection((ImsPhoneConnection) conn);
        }
        this.mHandoverCall.mConnections.clear();
        this.mState = PhoneConstants.State.IDLE;
        this.mSrvccState = Call.SrvccState.NONE;
        if (this.mPhone == null || this.mPhone.mDefaultPhone == null || this.mPhone.mDefaultPhone.getState() != PhoneConstants.State.IDLE) {
            return;
        }
        log("SRVCC: notify ImsPhone state as idle.");
        this.mPhone.notifyPhoneStateChanged();
    }

    void resumeWaitingOrHolding() throws CallStateException {
        log("resumeWaitingOrHolding");
        try {
            if (this.mForegroundCall.getState().isAlive()) {
                ImsCall imsCall = this.mForegroundCall.getImsCall();
                if (imsCall == null) {
                    return;
                }
                imsCall.resume();
                this.mEventLog.writeOnImsCallResume(imsCall.getSession());
                return;
            }
            if (this.mRingingCall.getState() == Call.State.WAITING) {
                if (this.mHasPendingResumeRequest) {
                    log("there is a pending resume background request, ignore accept()!");
                    return;
                }
                ImsCall imsCall2 = this.mRingingCall.getImsCall();
                if (imsCall2 == null) {
                    return;
                }
                imsCall2.accept(ImsCallProfile.getCallTypeFromVideoState(this.mPendingCallVideoState));
                this.mEventLog.writeOnImsCallAccept(imsCall2.getSession());
                return;
            }
            ImsCall imsCall3 = this.mBackgroundCall.getImsCall();
            if (imsCall3 == null) {
                return;
            }
            imsCall3.resume();
            this.mHasPendingResumeRequest = true;
            log("turn on the resuem pending request lock!");
            this.mEventLog.writeOnImsCallResume(imsCall3.getSession());
        } catch (ImsException e) {
            throw new CallStateException(e.getMessage());
        }
    }

    public void sendUSSD(String ussdString, Message response) {
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
                this.mPendingUssd = response;
                this.mUssdSession = this.mImsManager.makeCall(this.mServiceId, profile, callees, this.mImsUssdListener);
            }
        } catch (ImsException e) {
            loge("sendUSSD : " + e);
            this.mPhone.sendErrorResponse(response, e);
        }
    }

    public void cancelUSSD() {
        if (this.mUssdSession == null) {
            return;
        }
        try {
            this.mUssdSession.terminate(501);
        } catch (ImsException e) {
        }
    }

    private synchronized ImsPhoneConnection findConnection(ImsCall imsCall) {
        for (ImsPhoneConnection conn : this.mConnections) {
            if (conn.getImsCall() == imsCall) {
                return conn;
            }
        }
        return null;
    }

    private synchronized void removeConnection(ImsPhoneConnection conn) {
        this.mConnections.remove(conn);
        if (this.mIsInEmergencyCall) {
            boolean isEmergencyCallInList = false;
            Iterator imsPhoneConnection$iterator = this.mConnections.iterator();
            while (true) {
                if (!imsPhoneConnection$iterator.hasNext()) {
                    break;
                }
                ImsPhoneConnection imsPhoneConnection = (ImsPhoneConnection) imsPhoneConnection$iterator.next();
                if (imsPhoneConnection != null && imsPhoneConnection.isEmergency()) {
                    isEmergencyCallInList = true;
                    break;
                }
            }
            if (!isEmergencyCallInList) {
                this.mIsInEmergencyCall = false;
                this.mPhone.sendEmergencyCallStateChange(false);
            }
        }
    }

    private synchronized void addConnection(ImsPhoneConnection conn) {
        this.mConnections.add(conn);
        if (conn.isEmergency()) {
            this.mIsInEmergencyCall = true;
            this.mPhone.sendEmergencyCallStateChange(true);
        }
    }

    private void processCallStateChange(ImsCall imsCall, Call.State state, int cause) {
        log("processCallStateChange " + imsCall + " state=" + state + " cause=" + cause);
        processCallStateChange(imsCall, state, cause, false);
    }

    private void processCallStateChange(ImsCall imsCall, Call.State state, int cause, boolean ignoreState) {
        ImsPhoneConnection conn;
        log("processCallStateChange state=" + state + " cause=" + cause + " ignoreState=" + ignoreState);
        if (imsCall == null || (conn = findConnection(imsCall)) == null) {
            return;
        }
        conn.updateMediaCapabilities(imsCall);
        if (ignoreState) {
            conn.updateAddressDisplay(imsCall);
            conn.updateExtras(imsCall);
            maybeSetVideoCallProvider(conn, imsCall);
            return;
        }
        boolean changed = conn.update(imsCall, state);
        if (state == Call.State.DISCONNECTED) {
            if (conn.onDisconnect(cause)) {
                changed = true;
            }
            conn.getCall().detach(conn);
            removeConnection(conn);
        }
        logDebugMessagesWithDumpFormat("CC", conn, UsimPBMemInfo.STRING_NOT_SET);
        if (!changed || conn.getCall() == this.mHandoverCall) {
            return;
        }
        updatePhoneState();
        this.mPhone.notifyPreciseCallStateChanged();
    }

    private void maybeSetVideoCallProvider(ImsPhoneConnection conn, ImsCall imsCall) {
        Connection.VideoProvider connVideoProvider = conn.getVideoProvider();
        if (connVideoProvider != null || imsCall.getCallSession().getVideoCallProvider() == null) {
            return;
        }
        try {
            setVideoCallProvider(conn, imsCall);
        } catch (RemoteException e) {
            loge("maybeSetVideoCallProvider: exception " + e);
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
            case CharacterSets.ISO_8859_16:
                return 17;
            case 143:
                return 16;
            case PduHeaders.DATE_SENT:
            case 202:
            case 203:
            case 335:
                return 13;
            case CallFailCause.FDN_BLOCKED:
                return 21;
            case 321:
            case 331:
            case 332:
            case 340:
            case 361:
            case 362:
                return 12;
            case 329:
                return CallFailCause.IMS_EMERGENCY_REREG;
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
            case 905:
                return 400;
            case 906:
                return 401;
            case 907:
                return 402;
            case 908:
                return 403;
            case 1500:
                return 404;
            case 1501:
                return 405;
            default:
                return 36;
        }
    }

    private boolean isPhoneInEcbMode() {
        if (this.mPhone == null) {
            log("get isPhoneInEcbMode failed: mPhone = null");
            return false;
        }
        return Boolean.parseBoolean(TelephonyManager.getTelephonyProperty(this.mPhone.getPhoneId(), "ril.cdma.inecmmode", "false"));
    }

    private void dialPendingMO() {
        boolean isPhoneInEcmMode = isPhoneInEcbMode();
        boolean isEmergencyNumber = this.mPendingMO.isEmergency();
        if (!isPhoneInEcmMode || (isPhoneInEcmMode && isEmergencyNumber)) {
            sendEmptyMessage(20);
        } else {
            sendEmptyMessage(21);
        }
    }

    public ImsUtInterface getUtInterface() throws ImsException {
        if (this.mImsManager == null) {
            throw new ImsException("no ims manager", 0);
        }
        ImsUtInterface ut = this.mImsManager.getSupplementaryServiceConfiguration(this.mServiceId);
        return ut;
    }

    private void transferHandoverConnections(ImsPhoneCall call) {
        if (call.mConnections != null) {
            for (com.android.internal.telephony.Connection c : call.mConnections) {
                c.mPreHandoverState = call.mState;
                log("Connection state before handover is " + c.getStateBeforeHandover());
                c.mPreMultipartyState = c.isMultiparty();
                c.mPreMultipartyHostState = c instanceof ImsPhoneConnection ? ((ImsPhoneConnection) c).isConferenceHost() : false;
                log("SRVCC: Connection isMultiparty is " + c.mPreMultipartyState + "and isConfHost is " + c.mPreMultipartyHostState + " before handover");
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
            for (com.android.internal.telephony.Connection c2 : this.mHandoverCall.mConnections) {
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
        call.resetRingbackTone();
    }

    void notifySrvccState(Call.SrvccState state) {
        log("notifySrvccState state=" + state);
        this.mSrvccState = state;
        if (this.mSrvccState != Call.SrvccState.COMPLETED) {
            return;
        }
        transferHandoverConnections(this.mForegroundCall);
        transferHandoverConnections(this.mBackgroundCall);
        transferHandoverConnections(this.mRingingCall);
        if (this.mPendingMO != null) {
            log("SRVCC: reset mPendingMO");
            removeConnection(this.mPendingMO);
            this.mPendingMO = null;
        }
        updatePhoneState();
    }

    @Override
    public void handleMessage(Message msg) {
        log("handleMessage what=" + msg.what);
        switch (msg.what) {
            case 14:
                if (this.pendingCallInEcm) {
                    dialInternal(this.mPendingMO, this.pendingCallClirMode, this.mPendingCallVideoState, this.mPendingIntentExtras);
                    this.mPendingIntentExtras = null;
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
                this.mPendingIntentExtras = null;
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
                if (this.mPendingMO != null && this.mPendingMO.getImsCall() == null) {
                    dialInternal(this.mPendingMO, this.mClirMode, this.mPendingCallVideoState, this.mPendingIntentExtras);
                    this.mPendingIntentExtras = null;
                    break;
                }
                break;
            case 21:
                if (this.mPendingMO != null) {
                    try {
                        getEcbmInterface().exitEmergencyCallbackMode();
                        this.mPhone.setOnEcbModeExitResponse(this, 14, null);
                        this.pendingCallClirMode = this.mClirMode;
                        this.pendingCallInEcm = true;
                    } catch (ImsException e2) {
                        e2.printStackTrace();
                        this.mPendingMO.setDisconnectCause(36);
                        sendEmptyMessageDelayed(18, 500L);
                        return;
                    }
                }
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

    void logState() {
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
        for (int i = 0; i < this.mImsFeatureEnabled.length; i++) {
            pw.println(" " + this.mImsFeatureStrings[i] + ": " + (this.mImsFeatureEnabled[i] ? "enabled" : "disabled"));
        }
        pw.flush();
        pw.println("++++++++++++++++++++++++++++++++");
        try {
            if (this.mImsManager != null) {
                this.mImsManager.dump(fd, pw, args);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.mConnections == null || this.mConnections.size() <= 0) {
            return;
        }
        pw.println("mConnections:");
        for (int i2 = 0; i2 < this.mConnections.size(); i2++) {
            pw.println("  [" + i2 + "]: " + this.mConnections.get(i2));
        }
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

    ImsMultiEndpoint getMultiEndpointInterface() throws ImsException {
        if (this.mImsManager == null) {
            throw new ImsException("no ims manager", 0);
        }
        ImsMultiEndpoint multiendpoint = this.mImsManager.getMultiEndpointInterface(this.mServiceId);
        return multiendpoint;
    }

    public boolean isInEmergencyCall() {
        return this.mIsInEmergencyCall;
    }

    public boolean isVolteEnabled() {
        return this.mImsFeatureEnabled[0];
    }

    public boolean isVowifiEnabled() {
        return this.mImsFeatureEnabled[2];
    }

    public boolean isVideoCallEnabled() {
        if (this.mImsFeatureEnabled[1]) {
            return true;
        }
        return this.mImsFeatureEnabled[3];
    }

    @Override
    public PhoneConstants.State getState() {
        return this.mState;
    }

    private void setVideoCallProvider(ImsPhoneConnection conn, ImsCall imsCall) throws RemoteException {
        IImsVideoCallProvider imsVideoCallProvider = imsCall.getCallSession().getVideoCallProvider();
        if (imsVideoCallProvider == null) {
            return;
        }
        ImsVideoCallProviderWrapper imsVideoCallProviderWrapper = new ImsVideoCallProviderWrapper(imsVideoCallProvider);
        conn.setVideoProvider(imsVideoCallProviderWrapper);
    }

    public boolean isUtEnabled() {
        if (this.mImsFeatureEnabled[4]) {
            return true;
        }
        return this.mImsFeatureEnabled[5];
    }

    private String cleanseInstantLetteringMessage(String callSubject) {
        CarrierConfigManager configMgr;
        PersistableBundle carrierConfig;
        if (TextUtils.isEmpty(callSubject) || (configMgr = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config")) == null || (carrierConfig = configMgr.getConfigForSubId(this.mPhone.getSubId())) == null) {
            return callSubject;
        }
        String invalidCharacters = carrierConfig.getString("carrier_instant_lettering_invalid_chars_string");
        if (!TextUtils.isEmpty(invalidCharacters)) {
            callSubject = callSubject.replaceAll(invalidCharacters, UsimPBMemInfo.STRING_NOT_SET);
        }
        String escapedCharacters = carrierConfig.getString("carrier_instant_lettering_escaped_chars_string");
        if (!TextUtils.isEmpty(escapedCharacters)) {
            return escapeChars(escapedCharacters, callSubject);
        }
        return callSubject;
    }

    private String escapeChars(String toEscape, String source) {
        StringBuilder escaped = new StringBuilder();
        for (char c : source.toCharArray()) {
            if (toEscape.contains(Character.toString(c))) {
                escaped.append("\\");
            }
            escaped.append(c);
        }
        return escaped.toString();
    }

    @Override
    public void pullExternalCall(String number, int videoState) {
        Bundle extras = new Bundle();
        extras.putBoolean("CallPull", true);
        try {
            com.android.internal.telephony.Connection connection = dial(number, videoState, extras);
            this.mPhone.notifyUnknownConnection(connection);
        } catch (CallStateException e) {
            loge("pullExternalCall failed - " + e);
        }
    }

    private void registerIndicationReceiver() {
        log("registerIndicationReceiver");
        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction("com.android.ims.IMS_INCOMING_CALL_INDICATION");
        this.mPhone.getContext().registerReceiver(this.mIndicationReceiver, intentfilter);
    }

    private void unregisterIndicationReceiver() {
        log("unregisterIndicationReceiver");
        this.mPhone.getContext().unregisterReceiver(this.mIndicationReceiver);
    }

    private boolean isEccExist() {
        ImsCall imsCall;
        ImsCallProfile callProfile;
        ImsPhoneCall[] allCalls = {this.mForegroundCall, this.mBackgroundCall, this.mRingingCall, this.mHandoverCall};
        for (int i = 0; i < allCalls.length; i++) {
            if (allCalls[i].getState().isAlive() && (imsCall = allCalls[i].getImsCall()) != null && (callProfile = imsCall.getCallProfile()) != null && callProfile.mServiceType == 2) {
                return true;
            }
        }
        log("isEccExist(): no ECC!");
        return false;
    }

    private boolean hasVideoCallRestriction(Context context, Intent intent) {
        if (this.mPhone == null || !this.mPhone.isFeatureSupported(Phone.FeatureType.VIDEO_RESTRICTION)) {
            return false;
        }
        if (this.mForegroundCall.isIdle() && this.mBackgroundCall.isIdle()) {
            return false;
        }
        boolean hasVideoCall = false;
        ImsPhoneConnection fgConn = this.mForegroundCall.getFirstConnection();
        ImsPhoneConnection bgConn = this.mBackgroundCall.getFirstConnection();
        if (fgConn != null) {
            hasVideoCall = VideoProfile.isVideo(fgConn.getVideoState());
        }
        if (bgConn != null) {
            hasVideoCall |= VideoProfile.isVideo(bgConn.getVideoState());
        }
        boolean incomingVideoCall = isIncomingVideoCall(intent);
        return hasVideoCall | incomingVideoCall;
    }

    private void addCallLog(Context context, Intent intent) {
        int presentationMode;
        PhoneAccountHandle phoneAccountHandle = null;
        TelecomManager telecomManager = TelecomManager.from(context);
        Iterator<PhoneAccountHandle> phoneAccounts = telecomManager.getCallCapablePhoneAccounts().listIterator();
        while (true) {
            if (!phoneAccounts.hasNext()) {
                break;
            }
            PhoneAccountHandle handle = phoneAccounts.next();
            String id = handle.getId();
            if (id != null && id.equals(this.mPhone.getIccSerialNumber())) {
                log("iccid matches");
                phoneAccountHandle = handle;
                break;
            }
        }
        String number = intent.getStringExtra("android:imsDialString");
        if (number == null) {
            number = UsimPBMemInfo.STRING_NOT_SET;
        }
        if (number == null || number.equals(UsimPBMemInfo.STRING_NOT_SET)) {
            presentationMode = 2;
        } else {
            presentationMode = 1;
        }
        boolean isVideoIncoming = isIncomingVideoCall(intent);
        int features = 0;
        if (isVideoIncoming) {
            features = 1;
        }
        CallLog.Calls.addCall(null, context, number, presentationMode, 3, features, phoneAccountHandle, new Date().getTime(), 0, new Long(0L));
    }

    private boolean isIncomingVideoCall(Intent intent) {
        if (intent == null) {
            return false;
        }
        int callMode = intent.getIntExtra("android:imsCallMode", 0);
        return callMode == 21 || callMode == 23 || callMode == 25;
    }

    com.android.internal.telephony.Connection dial(List<String> numbers, int videoState) throws CallStateException {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mPhone.getContext());
        int oirMode = sp.getInt(Phone.CLIR_KEY + this.mPhone.getPhoneId(), 0);
        return dial(numbers, oirMode, videoState);
    }

    synchronized com.android.internal.telephony.Connection dial(List<String> numbers, int clirMode, int videoState) throws CallStateException {
        log("dial clirMode=" + clirMode);
        clearDisconnected();
        if (this.mImsManager == null) {
            throw new CallStateException("service not available");
        }
        if (!canDial()) {
            throw new CallStateException("cannot dial in current state");
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
            }
            this.mPendingMO = new ImsPhoneConnection((Phone) this.mPhone, UsimPBMemInfo.STRING_NOT_SET, this, this.mForegroundCall, false);
            ArrayList<String> dialStrings = new ArrayList<>();
            for (String str : numbers) {
                dialStrings.add(PhoneNumberUtils.extractNetworkPortionAlt(str));
            }
            this.mPendingMO.setConfDialStrings(dialStrings);
        }
        addConnection(this.mPendingMO);
        StringBuilder sb = new StringBuilder();
        for (String number : numbers) {
            sb.append(number);
            sb.append(", ");
        }
        logDebugMessagesWithOpFormat("CC", "DialConf", this.mPendingMO, " numbers=" + sb.toString());
        logDebugMessagesWithDumpFormat("CC", this.mPendingMO, UsimPBMemInfo.STRING_NOT_SET);
        if (!holdBeforeDial) {
            dialInternal(this.mPendingMO, clirMode, videoState, null);
        }
        updatePhoneState();
        this.mPhone.notifyPreciseCallStateChanged();
        return this.mPendingMO;
    }

    void hangupAll() throws CallStateException {
        log("hangupAll");
        if (this.mImsManager == null) {
            throw new CallStateException("No ImsManager Instance");
        }
        try {
            this.mImsManager.hangupAllCall();
            if (!this.mRingingCall.isIdle()) {
                this.mRingingCall.onHangupLocal();
            }
            if (!this.mForegroundCall.isIdle()) {
                this.mForegroundCall.onHangupLocal();
            }
            if (this.mBackgroundCall.isIdle()) {
                return;
            }
            this.mBackgroundCall.onHangupLocal();
        } catch (ImsException e) {
            throw new CallStateException(e.getMessage());
        }
    }

    private void broadcastImsStatusChange() {
        if (this.mPhone == null) {
            return;
        }
        Intent intent = new Intent("com.android.ims.IMS_STATE_CHANGED");
        int serviceState = this.mPhone.getServiceState().getState();
        int errorCode = this.mImsRegistrationErrorCode;
        boolean[] enabledFeatures = this.mImsFeatureEnabled;
        log("broadcastImsStateChange state= " + serviceState + " errorCode= " + errorCode + " enabledFeatures= " + enabledFeatures);
        intent.putExtra("android:regState", serviceState);
        if (serviceState != 0 && errorCode > 0) {
            intent.putExtra("android:regError", errorCode);
        }
        intent.putExtra("android:enablecap", enabledFeatures);
        intent.putExtra("android:phone_id", this.mPhone.getPhoneId());
        this.mPhone.getContext().sendBroadcast(intent);
    }

    void logDebugMessagesWithOpFormat(String category, String action, ImsPhoneConnection conn, String msg) {
        FormattedLog formattedLog;
        if (category == null || action == null || conn == null || (formattedLog = new FormattedLog.Builder().setCategory(category).setServiceName("ImsPhone").setOpType(FormattedLog.OpType.OPERATION).setActionName(action).setCallNumber(getCallNumber(conn)).setCallId(getConnectionCallId(conn)).setExtraMessage(msg).buildDebugMsg()) == null) {
            return;
        }
        log(formattedLog.toString());
    }

    void logDebugMessagesWithDumpFormat(String category, ImsPhoneConnection conn, String msg) {
        if (category == null || conn == null) {
            return;
        }
        FormattedLog formattedLog = new FormattedLog.Builder().setCategory("CC").setServiceName("ImsPhone").setOpType(FormattedLog.OpType.DUMP).setCallNumber(getCallNumber(conn)).setCallId(getConnectionCallId(conn)).setExtraMessage(msg).setStatusInfo("state", conn.getState().toString()).setStatusInfo("isConfCall", conn.isMultiparty() ? "Yes" : "No").setStatusInfo("isConfChildCall", "No").setStatusInfo("parent", conn.getParentCallName()).buildDumpInfo();
        if (formattedLog != null) {
            log(formattedLog.toString());
        }
    }

    private String getConnectionCallId(ImsPhoneConnection conn) {
        if (conn == null) {
            return UsimPBMemInfo.STRING_NOT_SET;
        }
        int callId = conn.getCallId();
        if (callId == -1 && (callId = conn.getCallIdBeforeDisconnected()) == -1) {
            return UsimPBMemInfo.STRING_NOT_SET;
        }
        return String.valueOf(callId);
    }

    private String getCallNumber(ImsPhoneConnection conn) {
        if (conn == null) {
            return null;
        }
        if (conn.isMultiparty()) {
            return "conferenceCall";
        }
        return conn.getAddress();
    }

    void unhold(ImsPhoneConnection conn) throws CallStateException {
        log("unhold connection");
        if (conn.getOwner() != this) {
            throw new CallStateException("ImsPhoneConnection " + conn + "does not belong to ImsPhoneCallTracker " + this);
        }
        unhold(conn.getCall());
    }

    private void unhold(ImsPhoneCall call) throws CallStateException {
        log("unhold call");
        if (call.getConnections().size() == 0) {
            throw new CallStateException("no connections");
        }
        try {
            if (call == this.mBackgroundCall) {
                log("unhold call: it is bg call, swap fg and bg");
                this.mSwitchingFgAndBgCalls = true;
                this.mCallExpectedToResume = this.mBackgroundCall.getImsCall();
                this.mForegroundCall.switchWith(this.mBackgroundCall);
            } else if (call != this.mForegroundCall) {
                log("unhold call which is neither background nor foreground call");
                return;
            }
            if (!this.mForegroundCall.getState().isAlive()) {
                return;
            }
            log("unhold call: foreground call is alive; try to resume it");
            ImsCall imsCall = this.mForegroundCall.getImsCall();
            if (imsCall == null) {
                return;
            }
            imsCall.resume();
        } catch (ImsException e) {
            throw new CallStateException(e.getMessage());
        }
    }

    private boolean isVendorDisconnectCauseNeeded(ImsReasonInfo reasonInfo) {
        if (reasonInfo == null) {
            return false;
        }
        int errorCode = reasonInfo.getCode();
        String errorMsg = reasonInfo.getExtraMessage();
        if (errorMsg == null) {
            log("isVendorDisconnectCauseNeeded = no due to empty errorMsg");
            return false;
        }
        if (errorCode == 1500 || errorCode == 1501) {
            log("isVendorDisconnectCauseNeeded = yes, OP07 503/403 cases");
            return true;
        }
        log("isVendorDisconnectCauseNeeded = no, no matched case");
        return false;
    }
}
