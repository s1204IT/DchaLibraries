package com.mediatek.internal.telephony.imsphone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.radio.V1_0.LastCallFailCause;
import android.hardware.radio.V1_0.RadioError;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telecom.ConferenceParticipant;
import android.telecom.VideoProfile;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsSuppServiceNotification;
import android.telephony.ims.feature.ImsFeature;
import android.text.TextUtils;
import com.android.ims.ImsCall;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.ims.ImsUtInterface;
import com.android.ims.internal.IImsCallSession;
import com.android.internal.os.SomeArgs;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.SettingsObserver;
import com.android.internal.telephony.TelephonyDevController;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneCall;
import com.android.internal.telephony.imsphone.ImsPhoneCallTracker;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.android.internal.telephony.imsphone.ImsPullCall;
import com.mediatek.ims.MtkImsCall;
import com.mediatek.ims.MtkImsConnectionStateListener;
import com.mediatek.ims.internal.MtkImsManager;
import com.mediatek.internal.telephony.MtkCallFailCause;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.MtkHardwareConfig;
import com.mediatek.internal.telephony.MtkPhoneNumberUtils;
import com.mediatek.internal.telephony.OpTelephonyCustomizationUtils;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import com.mediatek.internal.telephony.digits.DigitsUtil;
import com.mediatek.internal.telephony.imsphone.MtkImsPhone;
import com.mediatek.internal.telephony.imsphone.op.OpCommonImsPhoneCallTracker;
import com.mediatek.internal.telephony.imsphone.op.OpImsPhoneCallTracker;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import mediatek.telecom.FormattedLog;

public class MtkImsPhoneCallTracker extends ImsPhoneCallTracker implements ImsPullCall {
    private static final int EVENT_RETRY_DATA_ENABLED_CHANGED = 30;
    private static final int EVENT_ROAMING_OFF = 28;
    private static final int EVENT_ROAMING_ON = 27;
    private static final int EVENT_ROAMING_SETTING_CHANGE = 29;
    public static final int IMS_SESSION_MODIFY_OPERATION_FLAG = 32768;
    private static final int IMS_VIDEO_CALL = 21;
    private static final int IMS_VIDEO_CONF = 23;
    private static final int IMS_VIDEO_CONF_PARTS = 25;
    private static final int IMS_VOICE_CALL = 20;
    private static final int IMS_VOICE_CONF = 22;
    private static final int IMS_VOICE_CONF_PARTS = 24;
    private static final int INVALID_CALL_MODE = 255;
    static final String LOG_TAG = "MtkImsPhoneCallTracker";
    private static final String PROP_FORCE_DEBUG_KEY = "persist.vendor.log.tel_dbg";
    private static final boolean SENLOG = TextUtils.equals(Build.TYPE, DataSubConstants.REASON_MOBILE_DATA_ENABLE_USER);
    private static final boolean TELDBG;
    private RegistrantList mCallsDisconnectedDuringSrvccRegistrants;
    private boolean mDialAsECC;
    private DigitsUtil mDigitsUtil;
    private boolean mHasPendingResumeRequest;
    private boolean mIgnoreDataRoaming;
    private int mImsRegistrationErrorCode;
    private MtkImsConnectionStateListener mImsStateListener;
    private ImsCall.Listener mImsUssdListener;
    private BroadcastReceiver mIndicationReceiver;
    private boolean mIsDataRoaming;
    private boolean mIsDataRoamingSettingEnabled;
    private boolean mIsImsEccSupported;
    private boolean mIsOnCallResumed;
    private int mLastDataEnabledReason;
    private ImsCall.Listener mMtkImsCallListener;
    protected final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangedListener;
    private OpCommonImsPhoneCallTracker mOpCommonImsPhoneCallTracker;
    private OpImsPhoneCallTracker mOpImsPhoneCallTracker;
    private final SettingsObserver mSettingsObserver;
    private SubscriptionManager mSubscriptionManager;
    TelephonyDevController mTelDevController;
    private int mWifiPdnOOSState;

    static {
        TELDBG = SystemProperties.getInt(PROP_FORCE_DEBUG_KEY, 0) == 1;
    }

    private boolean hasC2kOverImsModem() {
        return (this.mTelDevController == null || this.mTelDevController.getModem(0) == null || !((MtkHardwareConfig) this.mTelDevController.getModem(0)).hasC2kOverImsModem()) ? false : true;
    }

    public boolean isSupportImsEcc() {
        return this.mIsImsEccSupported;
    }

    public MtkImsPhoneCallTracker(ImsPhone imsPhone) {
        super(imsPhone);
        this.mRingingCall = new MtkImsPhoneCall(this, "RG");
        this.mForegroundCall = new MtkImsPhoneCall(this, "FG");
        this.mBackgroundCall = new MtkImsPhoneCall(this, "BG");
        this.mHandoverCall = new MtkImsPhoneCall(this, "HO");
        this.mDialAsECC = false;
        this.mHasPendingResumeRequest = false;
        this.mIsOnCallResumed = false;
        this.mIsImsEccSupported = false;
        this.mWifiPdnOOSState = 2;
        this.mOnSubscriptionsChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
            @Override
            public void onSubscriptionsChanged() {
                MtkImsPhoneCallTracker.this.log("SubscriptionListener.onSubscriptionInfoChanged");
                if (SubscriptionManager.isValidSubscriptionId(MtkImsPhoneCallTracker.this.mPhone.getSubId())) {
                    MtkImsPhoneCallTracker.this.registerSettingsObserver();
                }
            }
        };
        this.mIsDataRoaming = false;
        this.mIsDataRoamingSettingEnabled = false;
        this.mIgnoreDataRoaming = false;
        this.mCallsDisconnectedDuringSrvccRegistrants = new RegistrantList();
        this.mTelDevController = TelephonyDevController.getInstance();
        this.mImsStateListener = new MtkImsConnectionStateListener() {
            public void onImsEmergencyCapabilityChanged(boolean z) {
                MtkImsPhoneCallTracker.this.log("onImsEmergencyCapabilityChanged: " + z);
                MtkImsPhoneCallTracker.this.mPhone.onFeatureCapabilityChanged();
                MtkImsPhoneCallTracker.this.mIsImsEccSupported = z;
                ((MtkImsPhone) MtkImsPhoneCallTracker.this.mPhone).updateIsEmergencyOnly();
            }

            public void onWifiPdnOOSStateChanged(int i) {
                MtkImsPhoneCallTracker.this.log("onWifiPdnOOSStateChanged: " + i);
                MtkImsPhoneCallTracker.this.mWifiPdnOOSState = i;
            }

            public void onCapabilitiesStatusChanged(ImsFeature.Capabilities capabilities) {
                MtkImsPhoneCallTracker.this.log("onCapabilitiesStatusChanged: " + capabilities);
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = capabilities;
                MtkImsPhoneCallTracker.this.removeMessages(26);
                MtkImsPhoneCallTracker.this.obtainMessage(26, someArgsObtain).sendToTarget();
                MtkImsPhoneCallTracker.this.checkRttCallType();
            }

            public void onImsConnected(int i) {
                MtkImsPhoneCallTracker.this.log("onImsConnected imsRadioTech=" + i);
                MtkImsPhoneCallTracker.this.mPhone.setServiceState(0);
                MtkImsPhoneCallTracker.this.mPhone.setImsRegistered(true);
                MtkImsPhoneCallTracker.this.mMetrics.writeOnImsConnectionState(MtkImsPhoneCallTracker.this.mPhone.getPhoneId(), 1, (ImsReasonInfo) null);
            }

            public void onImsProgressing(int i) {
                MtkImsPhoneCallTracker.this.log("onImsProgressing imsRadioTech=" + i);
                MtkImsPhoneCallTracker.this.mPhone.setServiceState(1);
                MtkImsPhoneCallTracker.this.mPhone.setImsRegistered(false);
                MtkImsPhoneCallTracker.this.mMetrics.writeOnImsConnectionState(MtkImsPhoneCallTracker.this.mPhone.getPhoneId(), 2, (ImsReasonInfo) null);
            }

            public void onImsDisconnected(ImsReasonInfo imsReasonInfo) {
                MtkImsPhoneCallTracker.this.log("onImsDisconnected imsReasonInfo=" + imsReasonInfo);
                MtkImsPhoneCallTracker.this.mPhone.setServiceState(1);
                MtkImsPhoneCallTracker.this.mPhone.setImsRegistered(false);
                MtkImsPhoneCallTracker.this.mPhone.processDisconnectReason(imsReasonInfo);
                MtkImsPhoneCallTracker.this.mMetrics.writeOnImsConnectionState(MtkImsPhoneCallTracker.this.mPhone.getPhoneId(), 3, imsReasonInfo);
            }
        };
        this.mMtkImsCallListener = new MtkImsCall.Listener() {
            public void onCallProgressing(ImsCall imsCall) {
                ((ImsPhoneCallTracker) MtkImsPhoneCallTracker.this).mImsCallListener.onCallProgressing(imsCall);
                ImsPhoneConnection imsPhoneConnectionFindConnection = MtkImsPhoneCallTracker.this.findConnection(imsCall);
                if (imsPhoneConnectionFindConnection != null) {
                    imsPhoneConnectionFindConnection.onConnectionEvent("mediatek.telecom.event.EVENT_CALL_ALERTING_NOTIFICATION", (Bundle) null);
                }
            }

            public void onCallStarted(ImsCall imsCall) {
                ((ImsPhoneCallTracker) MtkImsPhoneCallTracker.this).mImsCallListener.onCallStarted(imsCall);
            }

            public void onCallUpdated(ImsCall imsCall) {
                ((ImsPhoneCallTracker) MtkImsPhoneCallTracker.this).mImsCallListener.onCallUpdated(imsCall);
            }

            public void onCallStartFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
                ((ImsPhoneCallTracker) MtkImsPhoneCallTracker.this).mImsCallListener.onCallStartFailed(imsCall, imsReasonInfo);
                if (MtkImsPhoneCallTracker.this.mBackgroundCall.getState() == Call.State.HOLDING) {
                    MtkImsPhoneCallTracker.this.log("auto resume holding call");
                    MtkImsPhoneCallTracker.this.sendEmptyMessage(19);
                }
            }

            public void onCallTerminated(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
                ((ImsPhoneCallTracker) MtkImsPhoneCallTracker.this).mImsCallListener.onCallTerminated(imsCall, imsReasonInfo);
            }

            public void onCallHeld(ImsCall imsCall) {
                ((ImsPhoneCallTracker) MtkImsPhoneCallTracker.this).mImsCallListener.onCallHeld(imsCall);
            }

            public void onCallHoldFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
                MtkImsPhoneCallTracker.this.log("onCallHoldFailed reasonCode=" + imsReasonInfo.getCode());
                synchronized (MtkImsPhoneCallTracker.this.mSyncHold) {
                    Call.State state = MtkImsPhoneCallTracker.this.mBackgroundCall.getState();
                    if (imsReasonInfo.getCode() == 148) {
                        if (MtkImsPhoneCallTracker.this.mForegroundCall.getState() == Call.State.HOLDING || MtkImsPhoneCallTracker.this.mRingingCall.getState() == Call.State.WAITING) {
                            MtkImsPhoneCallTracker.this.log("onCallHoldFailed resume background");
                            MtkImsPhoneCallTracker.this.sendEmptyMessage(19);
                        } else if (MtkImsPhoneCallTracker.this.mPendingMO != null) {
                            MtkImsPhoneCallTracker.this.dialPendingMO();
                        }
                    } else if (state == Call.State.ACTIVE || state == Call.State.DISCONNECTING) {
                        MtkImsPhoneCallTracker.this.mForegroundCall.switchWith(MtkImsPhoneCallTracker.this.mBackgroundCall);
                        if (MtkImsPhoneCallTracker.this.mPendingMO != null) {
                            MtkImsPhoneCallTracker.this.mPendingMO.setDisconnectCause(36);
                            MtkImsPhoneCallTracker.this.sendEmptyMessageDelayed(18, 500L);
                        }
                    }
                    MtkImsPhoneCallTracker.this.mPhone.notifySuppServiceFailed(PhoneInternalInterface.SuppService.HOLD);
                }
                if (MtkImsPhoneCallTracker.this.mSwitchingFgAndBgCalls) {
                    MtkImsPhoneCallTracker.this.mSwitchingFgAndBgCalls = false;
                    MtkImsPhoneCallTracker.this.mCallExpectedToResume = null;
                }
                MtkImsPhoneCallTracker.this.mMetrics.writeOnImsCallHoldFailed(MtkImsPhoneCallTracker.this.mPhone.getPhoneId(), imsCall.getCallSession(), imsReasonInfo);
            }

            public void onCallResumed(ImsCall imsCall) {
                MtkImsPhoneCallTracker.this.log("onCallResumed");
                MtkImsPhoneCallTracker.this.mIsOnCallResumed = true;
                if (MtkImsPhoneCallTracker.this.mSwitchingFgAndBgCalls) {
                    if (imsCall == MtkImsPhoneCallTracker.this.mCallExpectedToResume) {
                        MtkImsPhoneCallTracker.this.log("onCallResumed : expected call resumed.");
                    } else {
                        MtkImsPhoneCallTracker.this.log("onCallResumed : switching " + MtkImsPhoneCallTracker.this.mForegroundCall + " with " + MtkImsPhoneCallTracker.this.mBackgroundCall);
                        MtkImsPhoneCallTracker.this.mForegroundCall.switchWith(MtkImsPhoneCallTracker.this.mBackgroundCall);
                    }
                    MtkImsPhoneCallTracker.this.mSwitchingFgAndBgCalls = false;
                    MtkImsPhoneCallTracker.this.mCallExpectedToResume = null;
                }
                MtkImsPhoneCallTracker.this.mHasPendingResumeRequest = false;
                MtkImsPhoneCallTracker.this.processCallStateChange(imsCall, Call.State.ACTIVE, 0);
                MtkImsPhoneCallTracker.this.mMetrics.writeOnImsCallResumed(MtkImsPhoneCallTracker.this.mPhone.getPhoneId(), imsCall.getCallSession());
                MtkImsPhoneCallTracker.this.mIsOnCallResumed = false;
            }

            public void onCallResumeFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
                MtkImsPhoneCallTracker.this.log("onCallResumeFailed");
                if (MtkImsPhoneCallTracker.this.mSwitchingFgAndBgCalls) {
                    if (imsCall == MtkImsPhoneCallTracker.this.mCallExpectedToResume) {
                        MtkImsPhoneCallTracker.this.log("onCallResumeFailed : switching " + MtkImsPhoneCallTracker.this.mForegroundCall + " with " + MtkImsPhoneCallTracker.this.mBackgroundCall);
                        MtkImsPhoneCallTracker.this.mForegroundCall.switchWith(MtkImsPhoneCallTracker.this.mBackgroundCall);
                        if (MtkImsPhoneCallTracker.this.mForegroundCall.getState() == Call.State.HOLDING) {
                            MtkImsPhoneCallTracker.this.sendEmptyMessage(19);
                        }
                    }
                    MtkImsPhoneCallTracker.this.mCallExpectedToResume = null;
                    MtkImsPhoneCallTracker.this.mSwitchingFgAndBgCalls = false;
                }
                MtkImsPhoneCallTracker.this.mHasPendingResumeRequest = false;
                MtkImsPhoneCallTracker.this.mPhone.notifySuppServiceFailed(PhoneInternalInterface.SuppService.RESUME);
                MtkImsPhoneCallTracker.this.mMetrics.writeOnImsCallResumeFailed(MtkImsPhoneCallTracker.this.mPhone.getPhoneId(), imsCall.getCallSession(), imsReasonInfo);
            }

            public void onCallResumeReceived(ImsCall imsCall) {
                ((ImsPhoneCallTracker) MtkImsPhoneCallTracker.this).mImsCallListener.onCallResumeReceived(imsCall);
            }

            public void onCallHoldReceived(ImsCall imsCall) {
                ((ImsPhoneCallTracker) MtkImsPhoneCallTracker.this).mImsCallListener.onCallHoldReceived(imsCall);
            }

            public void onCallSuppServiceReceived(ImsCall imsCall, ImsSuppServiceNotification imsSuppServiceNotification) {
                ((ImsPhoneCallTracker) MtkImsPhoneCallTracker.this).mImsCallListener.onCallSuppServiceReceived(imsCall, imsSuppServiceNotification);
            }

            public void onCallMerged(ImsCall imsCall, ImsCall imsCall2, boolean z) {
                ((ImsPhoneCallTracker) MtkImsPhoneCallTracker.this).mImsCallListener.onCallMerged(imsCall, imsCall2, z);
                ImsPhoneConnection imsPhoneConnectionFindConnection = MtkImsPhoneCallTracker.this.findConnection(imsCall);
                if (imsPhoneConnectionFindConnection != null && (imsPhoneConnectionFindConnection instanceof MtkImsPhoneConnection)) {
                    MtkImsPhoneConnection mtkImsPhoneConnection = (MtkImsPhoneConnection) imsPhoneConnectionFindConnection;
                    FormattedLog formattedLogBuildDumpInfo = new FormattedLog.Builder().setCategory("CC").setServiceName("ImsPhone").setOpType(FormattedLog.OpType.DUMP).setCallNumber(imsPhoneConnectionFindConnection.getAddress()).setCallId(MtkImsPhoneCallTracker.this.getConnectionCallId(mtkImsPhoneConnection)).setStatusInfo("state", "disconnected").setStatusInfo("isConfCall", "No").setStatusInfo("isConfChildCall", "No").setStatusInfo("parent", mtkImsPhoneConnection.getParentCallName()).buildDumpInfo();
                    if (formattedLogBuildDumpInfo != null) {
                        MtkImsPhoneCallTracker.this.log(formattedLogBuildDumpInfo.toString());
                    }
                }
            }

            public void onCallMergeFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
                ((ImsPhoneCallTracker) MtkImsPhoneCallTracker.this).mImsCallListener.onCallMergeFailed(imsCall, imsReasonInfo);
            }

            public void onConferenceParticipantsStateChanged(ImsCall imsCall, List<ConferenceParticipant> list) {
                ((ImsPhoneCallTracker) MtkImsPhoneCallTracker.this).mImsCallListener.onConferenceParticipantsStateChanged(imsCall, list);
            }

            public void onCallSessionTtyModeReceived(ImsCall imsCall, int i) {
                ((ImsPhoneCallTracker) MtkImsPhoneCallTracker.this).mImsCallListener.onCallSessionTtyModeReceived(imsCall, i);
            }

            public void onCallHandover(ImsCall imsCall, int i, int i2, ImsReasonInfo imsReasonInfo) {
                ((ImsPhoneCallTracker) MtkImsPhoneCallTracker.this).mImsCallListener.onCallHandover(imsCall, i, i2, imsReasonInfo);
            }

            public void onCallHandoverFailed(ImsCall imsCall, int i, int i2, ImsReasonInfo imsReasonInfo) {
                ((ImsPhoneCallTracker) MtkImsPhoneCallTracker.this).mImsCallListener.onCallHandoverFailed(imsCall, i, i2, imsReasonInfo);
            }

            public void onMultipartyStateChanged(ImsCall imsCall, boolean z) {
                ((ImsPhoneCallTracker) MtkImsPhoneCallTracker.this).mImsCallListener.onMultipartyStateChanged(imsCall, z);
            }

            public void onCallInviteParticipantsRequestDelivered(ImsCall imsCall) {
                MtkImsPhoneCallTracker.this.log("onCallInviteParticipantsRequestDelivered");
                ImsPhoneConnection imsPhoneConnectionFindConnection = MtkImsPhoneCallTracker.this.findConnection(imsCall);
                if (imsPhoneConnectionFindConnection != null && (imsPhoneConnectionFindConnection instanceof MtkImsPhoneConnection)) {
                    ((MtkImsPhoneConnection) imsPhoneConnectionFindConnection).notifyConferenceParticipantsInvited(true);
                }
            }

            public void onCallInviteParticipantsRequestFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
                MtkImsPhoneCallTracker.this.log("onCallInviteParticipantsRequestFailed reasonCode=" + imsReasonInfo.getCode());
                ImsPhoneConnection imsPhoneConnectionFindConnection = MtkImsPhoneCallTracker.this.findConnection(imsCall);
                if (imsPhoneConnectionFindConnection != null && (imsPhoneConnectionFindConnection instanceof MtkImsPhoneConnection)) {
                    ((MtkImsPhoneConnection) imsPhoneConnectionFindConnection).notifyConferenceParticipantsInvited(false);
                }
            }

            public void onCallTransferred(ImsCall imsCall) {
                MtkImsPhoneCallTracker.this.log("onCallTransferred");
            }

            public void onCallTransferFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
                MtkImsPhoneCallTracker.this.log("onCallTransferFailed");
                MtkImsPhoneCallTracker.this.mPhone.notifySuppServiceFailed(PhoneInternalInterface.SuppService.TRANSFER);
            }

            public void onTextCapabilityChanged(ImsCall imsCall, int i, int i2, int i3, int i4) {
                MtkImsPhoneCallTracker.this.mOpCommonImsPhoneCallTracker.onTextCapabilityChanged(MtkImsPhoneCallTracker.this.findConnection(imsCall), i, i2, i3, i4);
                MtkImsPhoneCallTracker.this.checkRttCallType();
            }

            public void onRttEventReceived(ImsCall imsCall, int i) {
                MtkImsPhoneCallTracker.this.mOpCommonImsPhoneCallTracker.onRttEventReceived(MtkImsPhoneCallTracker.this.findConnection(imsCall), i);
            }

            public void onRttModifyResponseReceived(ImsCall imsCall, int i) {
                MtkImsPhoneCallTracker.this.log("onRttModifyResponseReceived : " + i);
                ImsPhoneConnection imsPhoneConnectionFindConnection = MtkImsPhoneCallTracker.this.findConnection(imsCall);
                if (imsPhoneConnectionFindConnection != null) {
                    if (i == 1) {
                        imsPhoneConnectionFindConnection.onRttModifyResponseReceived(i);
                        MtkImsPhoneCallTracker.this.processRttModifySuccessCase(imsCall, i, imsPhoneConnectionFindConnection);
                        MtkImsPhoneCallTracker.this.checkRttCallType();
                        imsPhoneConnectionFindConnection.startRttTextProcessing();
                        return;
                    }
                    MtkImsPhoneCallTracker.this.processRttModifyFailCase(imsCall, i, imsPhoneConnectionFindConnection);
                }
            }

            public void onRttMessageReceived(ImsCall imsCall, String str) {
                MtkImsPhoneCallTracker.this.log("onRttMessageReceived : " + str);
                ((ImsPhoneCallTracker) MtkImsPhoneCallTracker.this).mImsCallListener.onRttMessageReceived(imsCall, str);
            }

            public void onRttModifyRequestReceived(ImsCall imsCall) {
                MtkImsPhoneCallTracker.this.log("onRttModifyRequestReceived");
                ((ImsPhoneCallTracker) MtkImsPhoneCallTracker.this).mImsCallListener.onRttModifyRequestReceived(imsCall);
            }

            public void onCallDeviceSwitched(ImsCall imsCall) {
                MtkImsPhoneCallTracker.this.log("onCallDeviceSwitched");
                ImsPhoneConnection imsPhoneConnectionFindConnection = MtkImsPhoneCallTracker.this.findConnection(imsCall);
                if (imsPhoneConnectionFindConnection != null && (imsPhoneConnectionFindConnection instanceof MtkImsPhoneConnection)) {
                    ((MtkImsPhoneConnection) imsPhoneConnectionFindConnection).notifyDeviceSwitched(true);
                }
            }

            public void onCallDeviceSwitchFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
                MtkImsPhoneCallTracker.this.log("onCallDeviceSwitchFailed");
                ImsPhoneConnection imsPhoneConnectionFindConnection = MtkImsPhoneCallTracker.this.findConnection(imsCall);
                if (imsPhoneConnectionFindConnection != null && (imsPhoneConnectionFindConnection instanceof MtkImsPhoneConnection)) {
                    ((MtkImsPhoneConnection) imsPhoneConnectionFindConnection).notifyDeviceSwitched(false);
                }
            }
        };
        this.mImsUssdListener = new ImsCall.Listener() {
            public void onCallStarted(ImsCall imsCall) {
                MtkImsPhoneCallTracker.this.log("mImsUssdListener onCallStarted");
                if (imsCall == MtkImsPhoneCallTracker.this.mUssdSession && MtkImsPhoneCallTracker.this.mPendingUssd != null) {
                    AsyncResult.forMessage(MtkImsPhoneCallTracker.this.mPendingUssd);
                    MtkImsPhoneCallTracker.this.mPendingUssd.sendToTarget();
                    MtkImsPhoneCallTracker.this.mPendingUssd = null;
                }
            }

            public void onCallStartFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
                MtkImsPhoneCallTracker.this.log("mImsUssdListener onCallStartFailed reasonCode=" + imsReasonInfo.getCode());
                onCallTerminated(imsCall, imsReasonInfo);
            }

            public void onCallTerminated(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
                MtkImsPhoneCallTracker.this.log("mImsUssdListener onCallTerminated reasonCode=" + imsReasonInfo.getCode());
                MtkImsPhoneCallTracker.this.removeMessages(25);
                if (imsCall == MtkImsPhoneCallTracker.this.mUssdSession) {
                    MtkImsPhoneCallTracker.this.mUssdSession = null;
                    if (MtkImsPhoneCallTracker.this.mPendingUssd != null) {
                        AsyncResult.forMessage(MtkImsPhoneCallTracker.this.mPendingUssd, (Object) null, new CommandException(CommandException.Error.GENERIC_FAILURE));
                        MtkImsPhoneCallTracker.this.mPendingUssd.sendToTarget();
                        MtkImsPhoneCallTracker.this.mPendingUssd = null;
                    }
                }
                imsCall.close();
            }

            public void onCallUssdMessageReceived(ImsCall imsCall, int i, String str) {
                int i2;
                MtkImsPhoneCallTracker.this.log("mImsUssdListener onCallUssdMessageReceived mode=" + i);
                switch (i) {
                    case 0:
                        if (imsCall == MtkImsPhoneCallTracker.this.mUssdSession) {
                            MtkImsPhoneCallTracker.this.mUssdSession = null;
                            imsCall.close();
                        }
                        i2 = 0;
                        break;
                    case 1:
                        i2 = 1;
                        break;
                    default:
                        if (imsCall == MtkImsPhoneCallTracker.this.mUssdSession) {
                            MtkImsPhoneCallTracker.this.log("invalid mode: " + i + ", clear ussi session");
                            MtkImsPhoneCallTracker.this.mUssdSession = null;
                            imsCall.close();
                        }
                        i2 = -1;
                        break;
                }
                ((MtkImsPhone) MtkImsPhoneCallTracker.this.mPhone).onIncomingUSSD(i2, str);
            }
        };
        this.mIndicationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("com.android.ims.IMS_INCOMING_CALL_INDICATION")) {
                    MtkImsPhoneCallTracker.this.log("onReceive : indication call intent");
                    if (MtkImsPhoneCallTracker.this.mImsManager == null) {
                        MtkImsPhoneCallTracker.this.log("no ims manager");
                        return;
                    }
                    if (intent.getIntExtra("android:phoneId", -1) != MtkImsPhoneCallTracker.this.mPhone.getPhoneId()) {
                        return;
                    }
                    boolean z = true;
                    if (TelephonyManager.getTelephonyProperty(MtkImsPhoneCallTracker.this.mPhone.getPhoneId(), "persist.vendor.radio.terminal-based.cw", "disabled_tbcw").equals("enabled_tbcw_off") && MtkImsPhoneCallTracker.this.mPhone.isInCall()) {
                        MtkImsPhoneCallTracker.this.log("PROPERTY_TBCW_MODE = TBCW_OFF. Reject the call as UDUB ");
                        z = false;
                    }
                    if (MtkImsPhoneCallTracker.this.isEccExist()) {
                        MtkImsPhoneCallTracker.this.log("there is an ECC call, dis-allow this incoming call!");
                        z = false;
                    }
                    if (MtkImsPhoneCallTracker.this.hasVideoCallRestriction(context, intent)) {
                        z = false;
                    }
                    MtkImsPhoneCallTracker.this.log("setCallIndication : intent = " + intent + ", isAllow = " + z);
                    try {
                        if (MtkImsPhoneCallTracker.this.mImsManager instanceof MtkImsManager) {
                            MtkImsPhoneCallTracker.this.mImsManager.setCallIndication(MtkImsPhoneCallTracker.this.mPhone.getPhoneId(), intent, z);
                        }
                    } catch (ImsException e) {
                        MtkImsPhoneCallTracker.this.loge("setCallIndication ImsException " + e);
                    }
                }
            }
        };
        this.mOpCommonImsPhoneCallTracker = OpTelephonyCustomizationUtils.getOpCommonInstance().makeOpCommonImsPhoneCallTracker();
        this.mOpImsPhoneCallTracker = OpTelephonyCustomizationUtils.getOpFactory(this.mPhone.getContext()).makeOpImsPhoneCallTracker();
        this.mDigitsUtil = OpTelephonyCustomizationUtils.getOpFactory(this.mPhone.getContext()).makeDigitsUtil();
        registerIndicationReceiver();
        this.mSettingsObserver = new SettingsObserver(this.mPhone.getContext(), this);
        registerSettingsObserver();
        this.mSubscriptionManager = SubscriptionManager.from(this.mPhone.getContext());
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        this.mPhone.getDefaultPhone().getServiceStateTracker().registerForDataRoamingOn(this, 27, (Object) null);
        this.mPhone.getDefaultPhone().getServiceStateTracker().registerForDataRoamingOff(this, 28, (Object) null, true);
        this.mOpCommonImsPhoneCallTracker.initRtt(this.mPhone);
    }

    public void dispose() {
        log("dispose");
        this.mRingingCall.dispose();
        this.mBackgroundCall.dispose();
        this.mForegroundCall.dispose();
        this.mHandoverCall.dispose();
        clearDisconnected();
        if (this.mUtInterface != null) {
            this.mUtInterface.unregisterForSuppServiceIndication(this);
        }
        this.mPhone.getContext().unregisterReceiver(this.mReceiver);
        unregisterIndicationReceiver();
        this.mPhone.setServiceState(1);
        this.mPhone.setImsRegistered(false);
        resetImsCapabilities();
        this.mPhone.onFeatureCapabilityChanged();
        this.mPhone.getDefaultPhone().unregisterForDataEnabledChanged(this);
        this.mImsManagerConnector.disconnect();
        this.mPhone.getDefaultPhone().getServiceStateTracker().unregisterForDataRoamingOn(this);
        this.mPhone.getDefaultPhone().getServiceStateTracker().unregisterForDataRoamingOff(this);
        this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        this.mSettingsObserver.unobserve();
        this.mOpCommonImsPhoneCallTracker.disposeRtt(this.mPhone, this.mForegroundCall, this.mSrvccState);
        if (this.mImsManager != null) {
            try {
                this.mImsManager.removeImsConnectionStateListener(this.mImsStateListener);
            } catch (ImsException e) {
                loge("dispose() : removeRegistrationListener failed: " + e);
            }
        }
    }

    public synchronized Connection dial(String str, ImsPhone.ImsDialArgs imsDialArgs) throws CallStateException {
        if (this.mSrvccState == Call.SrvccState.STARTED || this.mSrvccState == Call.SrvccState.COMPLETED) {
            throw new CallStateException(3, "cannot dial call: SRVCC");
        }
        return super.dial(str, imsDialArgs);
    }

    protected void dialInternal(ImsPhoneConnection imsPhoneConnection, int i, int i2, Bundle bundle) {
        boolean zIsEmergencyNumber;
        String[] strArr;
        if (imsPhoneConnection == null) {
            return;
        }
        if ((!(imsPhoneConnection instanceof MtkImsPhoneConnection) || ((MtkImsPhoneConnection) imsPhoneConnection).getConfDialStrings() == null) && (imsPhoneConnection.getAddress() == null || imsPhoneConnection.getAddress().length() == 0 || imsPhoneConnection.getAddress().indexOf(78) >= 0)) {
            imsPhoneConnection.setDisconnectCause(7);
            sendEmptyMessageDelayed(18, 500L);
            return;
        }
        setMute(false);
        if (hasC2kOverImsModem() && (!TelephonyManager.getDefault().hasIccCard(this.mPhone.getPhoneId()) || this.mPhone.getServiceState().getState() != 0)) {
            zIsEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(imsPhoneConnection.getAddress());
        } else {
            zIsEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(this.mPhone.getSubId(), imsPhoneConnection.getAddress());
        }
        int i3 = zIsEmergencyNumber ? 2 : 1;
        if (i3 == 2 && MtkPhoneNumberUtils.isSpecialEmergencyNumber(this.mPhone.getSubId(), imsPhoneConnection.getAddress())) {
            i3 = 1;
        }
        if (this.mDialAsECC) {
            log("Dial as ECC: conn.getAddress(): " + imsPhoneConnection.getAddress());
            this.mDialAsECC = false;
            i3 = 2;
        }
        int callTypeFromVideoState = ImsCallProfile.getCallTypeFromVideoState(i2);
        imsPhoneConnection.setVideoState(i2);
        try {
            if ((imsPhoneConnection instanceof MtkImsPhoneConnection) && ((MtkImsPhoneConnection) imsPhoneConnection).getConfDialStrings() != null) {
                ArrayList<String> confDialStrings = ((MtkImsPhoneConnection) imsPhoneConnection).getConfDialStrings();
                strArr = (String[]) confDialStrings.toArray(new String[confDialStrings.size()]);
            } else {
                strArr = new String[]{imsPhoneConnection.getAddress()};
            }
            ImsCallProfile imsCallProfileCreateCallProfile = this.mImsManager.createCallProfile(i3, callTypeFromVideoState);
            imsCallProfileCreateCallProfile.setCallExtraInt("oir", i);
            if ((imsPhoneConnection instanceof MtkImsPhoneConnection) && ((MtkImsPhoneConnection) imsPhoneConnection).getConfDialStrings() != null) {
                imsCallProfileCreateCallProfile.setCallExtraBoolean("conference", true);
            }
            if (bundle != null) {
                if (bundle.containsKey("android.telecom.extra.CALL_SUBJECT")) {
                    bundle.putString("DisplayText", cleanseInstantLetteringMessage(bundle.getString("android.telecom.extra.CALL_SUBJECT")));
                }
                if (imsPhoneConnection.hasRttTextStream()) {
                    imsCallProfileCreateCallProfile.mMediaProfile.mRttMode = 1;
                }
                if (bundle.containsKey("CallPull")) {
                    imsCallProfileCreateCallProfile.mCallExtras.putBoolean("CallPull", bundle.getBoolean("CallPull"));
                    int i4 = bundle.getInt("android.telephony.ImsExternalCallTracker.extra.EXTERNAL_CALL_ID");
                    imsPhoneConnection.setIsPulledCall(true);
                    imsPhoneConnection.setPulledDialogId(i4);
                }
                imsCallProfileCreateCallProfile.mCallExtras.putBundle("OemCallExtras", bundle);
                this.mOpCommonImsPhoneCallTracker.setRttMode(bundle, imsCallProfileCreateCallProfile);
                this.mDigitsUtil.putDialFrom(bundle, imsCallProfileCreateCallProfile);
            }
            if (strArr != null && strArr.length == 1 && !imsCallProfileCreateCallProfile.getCallExtraBoolean("conference")) {
                imsCallProfileCreateCallProfile.setCallExtra("oi", strArr[0]);
            }
            ImsCall imsCallMakeCall = this.mImsManager.makeCall(imsCallProfileCreateCallProfile, strArr, this.mMtkImsCallListener);
            imsPhoneConnection.setImsCall(imsCallMakeCall);
            this.mMetrics.writeOnImsCallStart(this.mPhone.getPhoneId(), imsCallMakeCall.getSession());
            setVideoCallProvider(imsPhoneConnection, imsCallMakeCall);
            imsPhoneConnection.setAllowAddCallDuringVideoCall(this.mAllowAddCallDuringVideoCall);
        } catch (ImsException e) {
            loge("dialInternal : " + e);
            imsPhoneConnection.setDisconnectCause(36);
            sendEmptyMessageDelayed(18, 500L);
            retryGetImsService();
        } catch (RemoteException e2) {
        }
    }

    public void acceptCall(int i) throws CallStateException {
        if (this.mSrvccState == Call.SrvccState.STARTED || this.mSrvccState == Call.SrvccState.COMPLETED) {
            throw new CallStateException(3, "cannot accept call: SRVCC");
        }
        if (!isDataAvailableForViLTE()) {
            i = 0;
            log("Data is off, answer as voice call");
        }
        logDebugMessagesWithOpFormat("CC", "Answer", this.mRingingCall.getFirstConnection(), "");
        super.acceptCall(i);
    }

    public void rejectCall() throws CallStateException {
        logDebugMessagesWithOpFormat("CC", "Reject", this.mRingingCall.getFirstConnection(), "");
        super.rejectCall();
    }

    public void switchWaitingOrHoldingAndActive() throws CallStateException {
        ImsPhoneConnection firstConnection;
        String str;
        if (this.mSrvccState == Call.SrvccState.STARTED || this.mSrvccState == Call.SrvccState.COMPLETED) {
            throw new CallStateException(3, "cannot hold/unhold call: SRVCC");
        }
        if (this.mForegroundCall.getState() == Call.State.ACTIVE) {
            firstConnection = this.mForegroundCall.getFirstConnection();
            if (this.mBackgroundCall.getState().isAlive()) {
                str = "switch with background connection:" + this.mBackgroundCall.getFirstConnection();
            } else {
                str = "hold to background";
            }
        } else {
            firstConnection = this.mBackgroundCall.getFirstConnection();
            str = "unhold to foreground";
        }
        logDebugMessagesWithOpFormat("CC", "Swap", firstConnection, str);
        super.switchWaitingOrHoldingAndActive();
    }

    public void conference() {
        logDebugMessagesWithOpFormat("CC", "Conference", this.mForegroundCall.getFirstConnection(), " merge with " + this.mBackgroundCall.getFirstConnection());
        if (this.mOpCommonImsPhoneCallTracker.isRttCallInvolved(this.mForegroundCall.getImsCall(), this.mBackgroundCall.getImsCall()) && !this.mOpCommonImsPhoneCallTracker.isAllowMergeRttCallToVoiceOnly()) {
            return;
        }
        super.conference();
    }

    public void explicitCallTransfer() {
        log("explicitCallTransfer");
        MtkImsCall imsCall = this.mForegroundCall.getImsCall();
        if (imsCall == null) {
            log("explicitCallTransfer no foreground ims call");
            return;
        }
        if (this.mBackgroundCall.getImsCall() == null) {
            log("explicitCallTransfer no background ims call");
            return;
        }
        if (this.mForegroundCall.getState() != Call.State.ACTIVE || this.mBackgroundCall.getState() != Call.State.HOLDING) {
            log("annot transfer call");
            return;
        }
        try {
            imsCall.explicitCallTransfer();
        } catch (ImsException e) {
            log("explicitCallTransfer " + e.getMessage());
        }
    }

    public void unattendedCallTransfer(String str, int i) {
        log("unattendedCallTransfer number : " + sensitiveEncode(str) + ", type : " + i);
        MtkImsCall imsCall = this.mForegroundCall.getImsCall();
        if (imsCall == null) {
            log("explicitCallTransfer no foreground ims call");
            return;
        }
        try {
            imsCall.unattendedCallTransfer(str, i);
        } catch (ImsException e) {
            log("explicitCallTransfer " + e.getMessage());
        }
    }

    public void deviceSwitch(String str, String str2) {
        log("deviceSwitch number : " + sensitiveEncode(str) + ", deviceId : " + str2);
        MtkImsCall imsCall = this.mForegroundCall.getImsCall();
        if (imsCall == null) {
            log("deviceSwitch no foreground ims call");
            return;
        }
        try {
            imsCall.deviceSwitch(str, str2);
        } catch (ImsException e) {
            log("deviceSwitch " + e.getMessage());
        }
    }

    public void cancelDeviceSwitch() {
        log("cancelDeviceSwitch");
        MtkImsCall imsCall = this.mForegroundCall.getImsCall();
        if (imsCall == null) {
            log("cancelDeviceSwitch no foreground ims call");
            return;
        }
        try {
            imsCall.cancelDeviceSwitch();
        } catch (ImsException e) {
            log("cancelDeviceSwitch " + e.getMessage());
        }
    }

    public boolean canDial() {
        log("IMS: canDial() serviceState = " + this.mPhone.getServiceState().getState() + ", disableCall = " + SystemProperties.get("ro.telephony.disable-call", "false") + ", mPendingMO = " + this.mPendingMO + ", Is mRingingCall ringing = " + this.mRingingCall.isRinging() + ", Is mForegroundCall alive = " + this.mForegroundCall.getState().isAlive() + ", Is mBackgroundCall alive = " + this.mBackgroundCall.getState().isAlive());
        if (this.mPhone != null && (this.mPhone.getDefaultPhone() instanceof MtkGsmCdmaPhone) && this.mPhone.getDefaultPhone().shouldProcessSelfActivation()) {
            log("IMS: canDial(), bypass dial for self activation");
            return true;
        }
        return super.canDial();
    }

    public void hangup(ImsPhoneCall imsPhoneCall) throws CallStateException {
        if (this.mSrvccState == Call.SrvccState.STARTED || this.mSrvccState == Call.SrvccState.COMPLETED) {
            throw new CallStateException(3, "cannot hangup call: SRVCC");
        }
        super.hangup(imsPhoneCall);
    }

    protected void callEndCleanupHandOverCallIfAny() {
        if (this.mHandoverCall.mConnections.size() > 0) {
            log("callEndCleanupHandOverCallIfAny, mHandoverCall.mConnections=" + this.mHandoverCall.mConnections);
            for (Connection connection : this.mHandoverCall.mConnections) {
                log("SRVCC: remove connection=" + connection);
                removeConnection((ImsPhoneConnection) connection);
            }
            this.mHandoverCall.mConnections.clear();
            this.mConnections.clear();
            this.mState = PhoneConstants.State.IDLE;
            if (this.mPhone != null && this.mPhone.mDefaultPhone != null && this.mPhone.mDefaultPhone.getState() == PhoneConstants.State.IDLE) {
                log("SRVCC: notify ImsPhone state as idle.");
                this.mPhone.notifyPhoneStateChanged();
                this.mCallsDisconnectedDuringSrvccRegistrants.notifyRegistrants(getCallStateChangeAsyncResult());
            }
        }
    }

    public void sendUSSD(String str, Message message) {
        log("sendUSSD");
        try {
            if (this.mUssdSession != null) {
                this.mUssdSession.sendUssd(str);
                AsyncResult.forMessage(message, (Object) null, (Throwable) null);
                message.sendToTarget();
            } else {
                if (this.mImsManager == null) {
                    this.mPhone.sendErrorResponse(message, getImsManagerIsNullException());
                    return;
                }
                String[] strArr = {str};
                ImsCallProfile imsCallProfileCreateCallProfile = this.mImsManager.createCallProfile(1, 2);
                imsCallProfileCreateCallProfile.setCallExtraInt("dialstring", 2);
                this.mDigitsUtil.putDialFrom(OpTelephonyCustomizationUtils.getOpFactory(this.mPhone.getContext()).makeDigitsUssdManager().getUssdExtra(), imsCallProfileCreateCallProfile);
                this.mPendingUssd = message;
                this.mUssdSession = this.mImsManager.makeCall(imsCallProfileCreateCallProfile, strArr, this.mImsUssdListener);
            }
        } catch (ImsException e) {
            loge("sendUSSD : " + e);
            this.mPhone.sendErrorResponse(message, e);
            retryGetImsService();
        }
    }

    protected synchronized void addConnection(ImsPhoneConnection imsPhoneConnection) {
        super.addConnection(imsPhoneConnection);
        if (imsPhoneConnection.isEmergency()) {
            this.mOpCommonImsPhoneCallTracker.stopRttEmcGuardTimer();
        }
    }

    protected void processCallStateChange(ImsCall imsCall, Call.State state, int i, boolean z) {
        super.processCallStateChange(imsCall, state, i, z);
        logDebugMessagesWithDumpFormat("CC", findConnection(imsCall), "");
        if (imsCall.isVideoCall() && !this.mIsDataEnabled) {
            log("ImsCall updated to video call but data off, retry onDataEnabledChanged");
            sendEmptyMessage(30);
        }
    }

    public int getDisconnectCauseFromReasonInfo(ImsReasonInfo imsReasonInfo, Call.State state) {
        int iMaybeRemapReasonCode = maybeRemapReasonCode(imsReasonInfo);
        switch (iMaybeRemapReasonCode) {
            case 1600:
                return 1500;
            case 1601:
                return 1501;
            case 1602:
                return 1502;
            case 1603:
                return 1503;
            case 1604:
                return 1504;
            case 1605:
                return 1505;
            case 1606:
                return 1506;
            case 1607:
                return 1507;
            case 1608:
                return 1508;
            case 1609:
                return 1509;
            case 1610:
                return 1510;
            case 1611:
                return 1511;
            case 1612:
                return 1512;
            case 1613:
                return 1513;
            case 1614:
                return 1514;
            case 1615:
                return 1515;
            case 1616:
                return 1516;
            case 1617:
                return 1517;
            case 1618:
                return 1518;
            case 1619:
                return 1519;
            case 1620:
                return 1520;
            case 1621:
                return 1521;
            case 1622:
                return 1522;
            case 1623:
                return 1523;
            case 1624:
                return 1524;
            case 1625:
                return 1525;
            case 1626:
                return 1526;
            case 1627:
                return 1527;
            case 1628:
                return 1528;
            case 1629:
                return 1529;
            case 1630:
                return 1530;
            case 1631:
                return 1531;
            case 1632:
                return 1532;
            case 1633:
                return 1533;
            case 1634:
                return 1534;
            case 1635:
                return 1535;
            case 1636:
                return 1536;
            case 1637:
                return 1537;
            case 1638:
                return 1538;
            case 1639:
                return 1539;
            default:
                switch (iMaybeRemapReasonCode) {
                    case LastCallFailCause.OEM_CAUSE_1:
                        return MtkCallFailCause.IMS_EMERGENCY_REREG;
                    case LastCallFailCause.OEM_CAUSE_2:
                        return 1045;
                    case LastCallFailCause.OEM_CAUSE_3:
                        return 1046;
                    case LastCallFailCause.OEM_CAUSE_4:
                        return 1042;
                    case LastCallFailCause.OEM_CAUSE_5:
                        return 1043;
                    default:
                        switch (iMaybeRemapReasonCode) {
                            case LastCallFailCause.OEM_CAUSE_11:
                                return 400;
                            case LastCallFailCause.OEM_CAUSE_12:
                                return 401;
                            case LastCallFailCause.OEM_CAUSE_13:
                                return 402;
                            case LastCallFailCause.OEM_CAUSE_14:
                                return 403;
                            default:
                                return super.getDisconnectCauseFromReasonInfo(imsReasonInfo, state);
                        }
                }
        }
    }

    protected void notifySrvccState(Call.SrvccState srvccState) {
        if (srvccState == Call.SrvccState.COMPLETED) {
            this.mOpCommonImsPhoneCallTracker.sendRttSrvccOrCsfbEvent(this.mForegroundCall);
            this.mOpCommonImsPhoneCallTracker.sendRttSrvccOrCsfbEvent(this.mBackgroundCall);
            this.mOpCommonImsPhoneCallTracker.sendRttSrvccOrCsfbEvent(this.mRingingCall);
        }
        super.notifySrvccState(srvccState);
        if (this.mSrvccState == Call.SrvccState.COMPLETED) {
            checkRttCallType();
        }
    }

    protected void releasePendingMOIfRequired() {
        if (this.mPendingMO == null) {
            return;
        }
        this.mPendingMO.setDisconnectCause(36);
        sendEmptyMessageDelayed(18, 500L);
    }

    protected void transferHandoverConnections(ImsPhoneCall imsPhoneCall) {
        log("transferHandoverConnections mSrvccState:" + this.mSrvccState);
        if (this.mSrvccState == Call.SrvccState.COMPLETED && imsPhoneCall.mConnections != null) {
            for (Connection connection : imsPhoneCall.mConnections) {
                if (this.mOnHoldToneStarted && connection != null && this.mOnHoldToneId == System.identityHashCode(connection)) {
                    log("transferHandoverConnections reset the hold tone.");
                    this.mPhone.stopOnHoldTone(connection);
                    this.mOnHoldToneStarted = false;
                    this.mOnHoldToneId = -1;
                }
            }
        }
        super.transferHandoverConnections(imsPhoneCall);
    }

    public void handleMessage(Message message) {
        log("handleMessage what=" + message.what);
        int i = message.what;
        if (i == 20) {
            if (this.mPendingMO != null && this.mPendingMO.getImsCall() == null) {
                dialInternal(this.mPendingMO, this.mClirMode, this.mPendingCallVideoState, this.mPendingIntentExtras);
                this.mPendingIntentExtras = null;
            }
            return;
        }
        switch (i) {
            case 27:
                onDataRoamingOn();
                break;
            case 28:
                onDataRoamingOff();
                break;
            case 29:
                onRoamingSettingsChanged();
                break;
            case 30:
                onDataEnabledChanged(this.mIsDataEnabled, this.mLastDataEnabledReason);
                break;
            default:
                super.handleMessage(message);
                break;
        }
    }

    private boolean hasVideoCallRestriction(Context context, Intent intent) {
        MtkImsPhone mtkImsPhone = (MtkImsPhone) this.mPhone;
        if (mtkImsPhone == null || !mtkImsPhone.isFeatureSupported(MtkImsPhone.FeatureType.VIDEO_RESTRICTION)) {
            return false;
        }
        if (this.mForegroundCall.isIdle() && this.mBackgroundCall.isIdle()) {
            return false;
        }
        ImsPhoneConnection firstConnection = this.mForegroundCall.getFirstConnection();
        ImsPhoneConnection firstConnection2 = this.mBackgroundCall.getFirstConnection();
        boolean zIsVideo = firstConnection != null ? false | VideoProfile.isVideo(firstConnection.getVideoState()) : false;
        if (firstConnection2 != null) {
            zIsVideo |= VideoProfile.isVideo(firstConnection2.getVideoState());
        }
        return isIncomingVideoCall(intent) | zIsVideo;
    }

    private boolean isIncomingVideoCall(Intent intent) {
        if (intent == null) {
            return false;
        }
        int intExtra = intent.getIntExtra("android:imsCallMode", 0);
        if (intExtra != 21 && intExtra != 23 && intExtra != 25) {
            return false;
        }
        return true;
    }

    private void registerIndicationReceiver() {
        log("registerIndicationReceiver");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.android.ims.IMS_INCOMING_CALL_INDICATION");
        this.mPhone.getContext().registerReceiver(this.mIndicationReceiver, intentFilter);
    }

    private void unregisterIndicationReceiver() {
        log("unregisterIndicationReceiver");
        this.mPhone.getContext().unregisterReceiver(this.mIndicationReceiver);
    }

    private boolean isEccExist() {
        ImsCall imsCall;
        ImsCallProfile callProfile;
        ImsPhoneCall[] imsPhoneCallArr = {this.mForegroundCall, this.mBackgroundCall, this.mRingingCall, this.mHandoverCall};
        for (int i = 0; i < imsPhoneCallArr.length; i++) {
            if (imsPhoneCallArr[i].getState().isAlive() && (imsCall = imsPhoneCallArr[i].getImsCall()) != null && (callProfile = imsCall.getCallProfile()) != null && callProfile.mServiceType == 2) {
                return true;
            }
        }
        log("isEccExist(): no ECC!");
        return false;
    }

    Connection dial(List<String> list, int i) throws CallStateException {
        return dial(list, PreferenceManager.getDefaultSharedPreferences(this.mPhone.getContext()).getInt("clir_key" + this.mPhone.getPhoneId(), 0), i);
    }

    synchronized Connection dial(List<String> list, int i, int i2) throws CallStateException {
        boolean z;
        MtkImsPhoneConnection mtkImsPhoneConnection;
        log("dial clirMode=" + i);
        clearDisconnected();
        if (this.mImsManager == null) {
            throw new CallStateException("service not available");
        }
        if (!canDial()) {
            throw new CallStateException("cannot dial in current state");
        }
        if (this.mForegroundCall.getState() == Call.State.ACTIVE) {
            if (this.mBackgroundCall.getState() != Call.State.IDLE) {
                throw new CallStateException("cannot dial in current state");
            }
            z = true;
            switchWaitingOrHoldingAndActive();
        } else {
            z = false;
        }
        Call.State state = Call.State.IDLE;
        Call.State state2 = Call.State.IDLE;
        this.mClirMode = i;
        synchronized (this.mSyncHold) {
            if (z) {
                try {
                    Call.State state3 = this.mForegroundCall.getState();
                    Call.State state4 = this.mBackgroundCall.getState();
                    if (state3 == Call.State.ACTIVE) {
                        throw new CallStateException("cannot dial in current state");
                    }
                    if (state4 == Call.State.HOLDING) {
                        z = false;
                    }
                } finally {
                }
            }
            mtkImsPhoneConnection = new MtkImsPhoneConnection((Phone) this.mPhone, "", (ImsPhoneCallTracker) this, this.mForegroundCall, false);
            this.mPendingMO = mtkImsPhoneConnection;
            ArrayList<String> arrayList = new ArrayList<>();
            Iterator<String> it = list.iterator();
            while (it.hasNext()) {
                arrayList.add(PhoneNumberUtils.extractNetworkPortionAlt(it.next()));
            }
            ((MtkImsPhoneConnection) this.mPendingMO).setConfDialStrings(arrayList);
        }
        addConnection(this.mPendingMO);
        StringBuilder sb = new StringBuilder();
        Iterator<String> it2 = list.iterator();
        while (it2.hasNext()) {
            sb.append(it2.next());
            sb.append(", ");
        }
        logDebugMessagesWithOpFormat("CC", "DialConf", this.mPendingMO, " numbers=" + sb.toString());
        logDebugMessagesWithDumpFormat("CC", this.mPendingMO, "");
        if (!z) {
            dialInternal(this.mPendingMO, i, i2, null);
        }
        updatePhoneState();
        this.mPhone.notifyPreciseCallStateChanged();
        return mtkImsPhoneConnection;
    }

    void hangupAll() throws CallStateException {
        log("hangupAll");
        if (this.mImsManager == null || !(this.mImsManager instanceof MtkImsManager)) {
            throw new CallStateException("No MtkImsManager Instance");
        }
        try {
            this.mImsManager.hangupAllCall(this.mPhone.getPhoneId());
            if (!this.mRingingCall.isIdle()) {
                setCallTerminationFlag(this.mRingingCall);
                this.mRingingCall.onHangupLocal();
            }
            if (!this.mForegroundCall.isIdle()) {
                setCallTerminationFlag(this.mForegroundCall);
                this.mForegroundCall.onHangupLocal();
            }
            if (!this.mBackgroundCall.isIdle()) {
                setCallTerminationFlag(this.mBackgroundCall);
                this.mBackgroundCall.onHangupLocal();
            }
        } catch (ImsException e) {
            throw new CallStateException(e.getMessage());
        }
    }

    private void setCallTerminationFlag(ImsPhoneCall imsPhoneCall) {
        log("setCallTerminationFlag");
        MtkImsCall imsCall = imsPhoneCall.getImsCall();
        if (imsCall == null) {
            log("setCallTerminationFlag " + imsPhoneCall + " no ims call");
            return;
        }
        imsCall.setTerminationRequestFlag(true);
    }

    protected void logDebugMessagesWithOpFormat(String str, String str2, ImsPhoneConnection imsPhoneConnection, String str3) {
        FormattedLog formattedLogBuildDebugMsg;
        if (str != null && str2 != null && imsPhoneConnection != null && (formattedLogBuildDebugMsg = new FormattedLog.Builder().setCategory(str).setServiceName("ImsPhone").setOpType(FormattedLog.OpType.OPERATION).setActionName(str2).setCallNumber(getCallNumber(imsPhoneConnection)).setCallId(getConnectionCallId((MtkImsPhoneConnection) imsPhoneConnection)).setExtraMessage(str3).buildDebugMsg()) != null) {
            if (!SENLOG || TELDBG) {
                log(formattedLogBuildDebugMsg.toString());
            }
        }
    }

    protected void logDebugMessagesWithDumpFormat(String str, ImsPhoneConnection imsPhoneConnection, String str2) {
        if (str == null || imsPhoneConnection == null || !(imsPhoneConnection instanceof MtkImsPhoneConnection)) {
            return;
        }
        MtkImsPhoneConnection mtkImsPhoneConnection = (MtkImsPhoneConnection) imsPhoneConnection;
        FormattedLog formattedLogBuildDumpInfo = new FormattedLog.Builder().setCategory("CC").setServiceName("ImsPhone").setOpType(FormattedLog.OpType.DUMP).setCallNumber(getCallNumber(imsPhoneConnection)).setCallId(getConnectionCallId(mtkImsPhoneConnection)).setExtraMessage(str2).setStatusInfo("state", imsPhoneConnection.getState().toString()).setStatusInfo("isConfCall", imsPhoneConnection.isMultiparty() ? "Yes" : "No").setStatusInfo("isConfChildCall", "No").setStatusInfo("parent", mtkImsPhoneConnection.getParentCallName()).buildDumpInfo();
        if (formattedLogBuildDumpInfo != null) {
            if (!SENLOG || TELDBG) {
                log(formattedLogBuildDumpInfo.toString());
            }
        }
    }

    private String getConnectionCallId(MtkImsPhoneConnection mtkImsPhoneConnection) {
        if (mtkImsPhoneConnection == null) {
            return "";
        }
        int callId = mtkImsPhoneConnection.getCallId();
        if (callId == -1 && (callId = mtkImsPhoneConnection.getCallIdBeforeDisconnected()) == -1) {
            return "";
        }
        return String.valueOf(callId);
    }

    private String getCallNumber(ImsPhoneConnection imsPhoneConnection) {
        if (imsPhoneConnection == null) {
            return null;
        }
        if (imsPhoneConnection.isMultiparty()) {
            return "conferenceCall";
        }
        return imsPhoneConnection.getAddress();
    }

    void unhold(ImsPhoneConnection imsPhoneConnection) throws CallStateException {
        log("unhold connection, is in switching? : " + this.mSwitchingFgAndBgCalls);
        if (imsPhoneConnection.getOwner() != this) {
            throw new CallStateException("ImsPhoneConnection " + imsPhoneConnection + "does not belong to MtkImsPhoneCallTracker " + this);
        }
        if (!this.mSwitchingFgAndBgCalls) {
            unhold(imsPhoneConnection.getCall());
        }
    }

    private void unhold(ImsPhoneCall imsPhoneCall) throws CallStateException {
        log("unhold call, is in switching? : " + this.mSwitchingFgAndBgCalls);
        if (imsPhoneCall.getConnections().size() == 0) {
            throw new CallStateException("no connections");
        }
        if (this.mIsOnCallResumed) {
            log("unhold call: drop unhold, an call is processing onCallResumed");
            return;
        }
        try {
            if (imsPhoneCall == this.mBackgroundCall) {
                log("unhold call: it is bg call, swap fg and bg");
                this.mSwitchingFgAndBgCalls = true;
                this.mCallExpectedToResume = this.mBackgroundCall.getImsCall();
                this.mForegroundCall.switchWith(this.mBackgroundCall);
            } else if (imsPhoneCall != this.mForegroundCall) {
                log("unhold call which is neither background nor foreground call");
                return;
            }
            if (this.mForegroundCall.getState().isAlive()) {
                log("unhold call: foreground call is alive; try to resume it");
                ImsCall imsCall = this.mForegroundCall.getImsCall();
                if (imsCall != null) {
                    imsCall.resume();
                }
            }
        } catch (ImsException e) {
            throw new CallStateException(e.getMessage());
        }
    }

    private boolean isVendorDisconnectCauseNeeded(ImsReasonInfo imsReasonInfo) {
        if (imsReasonInfo == null) {
            return false;
        }
        imsReasonInfo.getCode();
        if (imsReasonInfo.getExtraMessage() == null) {
            log("isVendorDisconnectCauseNeeded = no due to empty errorMsg");
            return false;
        }
        log("isVendorDisconnectCauseNeeded = no, no matched case");
        return false;
    }

    public ImsUtInterface getUtInterface() throws ImsException {
        if (this.mImsManager == null) {
            throw getImsManagerIsNullException();
        }
        return this.mImsManager.getSupplementaryServiceConfiguration();
    }

    protected ImsPhoneConnection makeImsPhoneConnectionForMO(String str, boolean z) {
        return new MtkImsPhoneConnection((Phone) this.mPhone, checkForTestEmergencyNumber(str), (ImsPhoneCallTracker) this, this.mForegroundCall, z);
    }

    protected ImsPhoneConnection makeImsPhoneConnectionForMT(ImsCall imsCall, boolean z) {
        return new MtkImsPhoneConnection((Phone) this.mPhone, imsCall, (ImsPhoneCallTracker) this, z ? this.mForegroundCall : this.mRingingCall, z);
    }

    protected ImsCall takeCall(IImsCallSession iImsCallSession, Bundle bundle) throws ImsException {
        return this.mImsManager.takeCall(iImsCallSession, bundle, this.mMtkImsCallListener);
    }

    protected void mtkNotifyRemoteHeld(ImsPhoneConnection imsPhoneConnection, boolean z) {
        if (imsPhoneConnection != null && (imsPhoneConnection instanceof MtkImsPhoneConnection)) {
            ((MtkImsPhoneConnection) imsPhoneConnection).notifyRemoteHeld(z);
        }
    }

    protected boolean isEmergencyNumber(String str) {
        if (hasC2kOverImsModem() && (!TelephonyManager.getDefault().hasIccCard(this.mPhone.getPhoneId()) || this.mPhone.getServiceState().getState() != 0)) {
            return PhoneNumberUtils.isEmergencyNumber(str);
        }
        return PhoneNumberUtils.isEmergencyNumber(this.mPhone.getSubId(), str);
    }

    protected void checkforCsfb() throws CallStateException {
        if (this.mHandoverCall.mConnections.size() > 0) {
            log("SRVCC: there are connections during handover, trigger CSFB!");
            throw new CallStateException("cs_fallback");
        }
        if (this.mPhone != null && this.mPhone.getDefaultPhone() != null && this.mPhone.getDefaultPhone().getState() != PhoneConstants.State.IDLE && getState() == PhoneConstants.State.IDLE) {
            log("There are CS connections, trigger CSFB!");
            throw new CallStateException("cs_fallback");
        }
    }

    protected void resetFlagWhenSwitchFailed() {
        this.mSwitchingFgAndBgCalls = false;
        this.mCallExpectedToResume = null;
    }

    protected void setPendingResumeRequest(boolean z) {
        if (z) {
            log("turn on the resuem pending request lock!");
        }
        this.mHasPendingResumeRequest = z;
    }

    protected boolean hasPendingResumeRequest() {
        if (this.mHasPendingResumeRequest) {
            log("there is a pending resume background request, ignore accept()!");
        }
        return this.mHasPendingResumeRequest;
    }

    protected boolean canDailOnCallTerminated() {
        return (this.mPendingMO == null || hasMessages(18)) ? false : true;
    }

    protected void setRedialAsEcc(int i) {
        if (i == 380) {
            this.mDialAsECC = true;
        }
    }

    protected void setVendorDisconnectCause(ImsPhoneConnection imsPhoneConnection, ImsReasonInfo imsReasonInfo) {
        if (imsPhoneConnection instanceof MtkImsPhoneConnection) {
            ((MtkImsPhoneConnection) imsPhoneConnection).setVendorDisconnectCause(imsReasonInfo.getExtraMessage());
        }
    }

    protected int updateDisconnectCause(int i, ImsPhoneConnection imsPhoneConnection) {
        if (i == 36 && imsPhoneConnection != null && imsPhoneConnection.getImsCall().isMerged()) {
            return 45;
        }
        return i;
    }

    protected void setMultiPartyState(Connection connection) {
        if (connection instanceof MtkImsPhoneConnection) {
            MtkImsPhoneConnection mtkImsPhoneConnection = (MtkImsPhoneConnection) connection;
            mtkImsPhoneConnection.mWasMultiparty = connection.isMultiparty();
            mtkImsPhoneConnection.mWasPreMultipartyHost = connection.isConferenceHost();
            log("SRVCC: Connection isMultiparty is " + mtkImsPhoneConnection.mWasMultiparty + "and isConfHost is " + mtkImsPhoneConnection.mWasPreMultipartyHost + " before handover");
        }
    }

    protected void resetRingBackTone(ImsPhoneCall imsPhoneCall) {
        if (imsPhoneCall instanceof MtkImsPhoneCall) {
            ((MtkImsPhoneCall) imsPhoneCall).resetRingbackTone();
        }
    }

    protected void updateForSrvccCompleted() {
        if (this.mPendingMO != null) {
            log("SRVCC: reset mPendingMO");
            removeConnection(this.mPendingMO);
            this.mPendingMO = null;
        }
        updatePhoneState();
        this.mSrvccState = Call.SrvccState.NONE;
    }

    public void cancelUSSD(Message message) {
        if (this.mUssdSession == null) {
            return;
        }
        this.mPendingUssd = message;
        try {
            this.mUssdSession.terminate(RadioError.OEM_ERROR_1);
        } catch (ImsException e) {
        }
    }

    protected AsyncResult getCallStateChangeAsyncResult() {
        return new AsyncResult((Object) null, this.mSrvccState, (Throwable) null);
    }

    protected void checkIncomingRtt(Intent intent, ImsCall imsCall, ImsPhoneConnection imsPhoneConnection) {
        this.mOpCommonImsPhoneCallTracker.checkIncomingRtt(intent, imsCall, imsPhoneConnection);
    }

    protected void processRttModifyFailCase(ImsCall imsCall, int i, ImsPhoneConnection imsPhoneConnection) {
        this.mOpCommonImsPhoneCallTracker.processRttModifyFailCase(imsCall, i, imsPhoneConnection);
    }

    protected void checkRttCallType() {
        this.mOpCommonImsPhoneCallTracker.checkRttCallType(this.mPhone, this.mForegroundCall, this.mSrvccState);
    }

    protected void processRttModifySuccessCase(ImsCall imsCall, int i, ImsPhoneConnection imsPhoneConnection) {
        this.mOpCommonImsPhoneCallTracker.processRttModifySuccessCase(imsCall, i, imsPhoneConnection);
    }

    protected void startRttEmcGuardTimer() {
        this.mOpCommonImsPhoneCallTracker.startRttEmcGuardTimer(this.mPhone);
    }

    protected void startListeningForCalls() throws ImsException {
        super.startListeningForCalls();
        try {
            this.mImsManager.addImsConnectionStateListener(this.mImsStateListener);
            log("startListeningForCalls() : register ims succeed, " + this.mImsStateListener);
        } catch (ImsException e) {
            log("startListeningForCalls() : register ims fail!");
        }
    }

    protected void modifyVideoCall(ImsCall imsCall, int i) {
        ImsPhoneConnection imsPhoneConnectionFindConnection = findConnection(imsCall);
        int i2 = i | 32768;
        if (imsPhoneConnectionFindConnection != null) {
            int videoState = imsPhoneConnectionFindConnection.getVideoState();
            if (imsPhoneConnectionFindConnection.getVideoProvider() != null) {
                imsPhoneConnectionFindConnection.getVideoProvider().onSendSessionModifyRequest(new VideoProfile(videoState), new VideoProfile(i2));
            }
        }
    }

    protected void switchWfcModeIfRequired(ImsManager imsManager, boolean z, boolean z2) {
        this.mOpImsPhoneCallTracker.switchWfcModeIfRequired(imsManager, z, z2);
    }

    protected String getVtInterface() {
        String str = new String("vt_data0" + String.valueOf(this.mPhone.getSubId()));
        log("getVtInterface(): " + str);
        return str;
    }

    private String sensitiveEncode(String str) {
        if (!SENLOG || TELDBG) {
            return str;
        }
        return "[hidden]";
    }

    protected boolean isCarrierPauseAllowed(ImsCall imsCall) {
        if (imsCall != null && imsCall.getState() == 3) {
            return false;
        }
        return true;
    }

    public boolean isWifiPdnOutOfService() {
        return this.mWifiPdnOOSState == 1 || this.mWifiPdnOOSState == 0;
    }

    public void registerForCallsDisconnectedDuringSrvcc(Handler handler, int i, Object obj) {
        this.mCallsDisconnectedDuringSrvccRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForCallsDisconnectedDuringSrvcc(Handler handler) {
        this.mCallsDisconnectedDuringSrvccRegistrants.remove(handler);
    }

    public void registerSettingsObserver() {
        this.mSettingsObserver.unobserve();
        String string = "";
        if (TelephonyManager.getDefault().getSimCount() > 1) {
            string = Integer.toString(this.mPhone.getDefaultPhone().getSubId());
        }
        this.mSettingsObserver.observe(Settings.Global.getUriFor("data_roaming" + string), 29);
    }

    protected void onDataRoamingOn() {
        log("onDataRoamingOn");
        if (this.mIsDataRoaming) {
            log("onDataRoamingOn: device already in roaming. ignored the update.");
            return;
        }
        this.mIsDataRoaming = this.mPhone.getDefaultPhone().getServiceState().getDataRoaming();
        ImsManager.getInstance(this.mPhone.getContext(), this.mPhone.getPhoneId()).setDataRoaming(this.mIsDataRoaming);
        if (this.mIsDataRoamingSettingEnabled) {
            log("onDataRoamingOn: setup data on roaming");
            onDataRoamingEnabledChanged(true);
        } else {
            log("onDataRoamingOn: Tear down data connection on roaming.");
            onDataRoamingEnabledChanged(false);
        }
    }

    protected void onDataRoamingOff() {
        log("onDataRoamingOff");
        if (!this.mIsDataRoaming) {
            log("onDataRoamingOff: device already not roaming. ignored the update.");
            return;
        }
        this.mIsDataRoaming = this.mPhone.getDefaultPhone().getServiceState().getDataRoaming();
        ImsManager.getInstance(this.mPhone.getContext(), this.mPhone.getPhoneId()).setDataRoaming(this.mIsDataRoaming);
        if (!this.mIsDataRoamingSettingEnabled) {
            onDataRoamingEnabledChanged(true);
        }
    }

    protected void onRoamingSettingsChanged() {
        log("onRoamingSettingsChanged");
        this.mIsDataRoamingSettingEnabled = this.mPhone.getDefaultPhone().mDcTracker.getDataRoamingEnabled();
        ImsManager.getInstance(this.mPhone.getContext(), this.mPhone.getPhoneId()).setDataRoamingSettingsEnabled(this.mIsDataRoamingSettingEnabled);
        log("onRoamingSettingsChanged: mIsDataRoaming = " + this.mIsDataRoaming + ", mIsDataRoamingSettingEnabled = " + this.mIsDataRoamingSettingEnabled);
        if (!this.mIsDataRoaming) {
            log("onRoamingSettingsChanged: device is not roaming. ignored the request.");
        } else if (this.mIsDataRoamingSettingEnabled) {
            log("onRoamingSettingsChanged: setup data on roaming");
            onDataRoamingEnabledChanged(true);
        } else {
            log("onRoamingSettingsChanged: Tear down data connection on roaming.");
            onDataRoamingEnabledChanged(false);
        }
    }

    private void onDataRoamingEnabledChanged(boolean z) {
        log("onDataRoamingEnabledChanged: enabled=" + z);
        if (!this.mIsViLteDataMetered) {
            StringBuilder sb = new StringBuilder();
            sb.append("onDataRoamingEnabledChanged: Ignore data ");
            sb.append(z ? "enabled" : "disabled");
            sb.append(" - carrier policy indicates that data is not metered for ViLTE calls.");
            log(sb.toString());
            return;
        }
        if (this.mIgnoreDataRoaming) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("onDataRoaming: Ignore data ");
            sb2.append(z ? "enabled" : "disabled");
            sb2.append(" - carrier policy indicates that ignore data roaming");
            log(sb2.toString());
            return;
        }
        if (z && !this.mIsDataEnabled) {
            log("onDataRoamingEnabledChanged: Ignore on when data off");
            return;
        }
        Iterator it = this.mConnections.iterator();
        while (it.hasNext()) {
            ((ImsPhoneConnection) it.next()).handleDataEnabledChange(z);
        }
        maybeNotifyDataDisabled(z, 1406);
        handleDataEnabledChange(z, 1406);
        if (!this.mShouldUpdateImsConfigOnDisconnect) {
            ImsManager.updateImsServiceConfig(this.mPhone.getContext(), this.mPhone.getPhoneId(), true);
        }
    }

    protected boolean isRoamingOnAndRoamingSettingOff() {
        return (!this.mIsDataRoaming || this.mIsDataRoamingSettingEnabled || this.mIgnoreDataRoaming) ? false : true;
    }

    protected void onDataEnabledChanged(boolean z, int i) {
        this.mLastDataEnabledReason = i;
        super.onDataEnabledChanged(z, i);
    }

    protected void cacheCarrierConfiguration(int i) {
        super.cacheCarrierConfiguration(i);
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        if (carrierConfigManager == null) {
            loge("cacheCarrierConfiguration: No carrier config service found.");
            return;
        }
        PersistableBundle configForSubId = carrierConfigManager.getConfigForSubId(i);
        if (configForSubId == null) {
            loge("cacheCarrierConfiguration: Empty carrier config.");
        } else {
            this.mIgnoreDataRoaming = configForSubId.getBoolean("mtk_ignore_data_roaming_for_video_calls");
        }
    }

    protected boolean isDataAvailableForViLTE() {
        return !this.mIsViLteDataMetered || (this.mIsDataEnabled && !isRoamingOnAndRoamingSettingOff());
    }
}
