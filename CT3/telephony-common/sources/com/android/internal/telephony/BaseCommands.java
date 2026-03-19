package com.android.internal.telephony;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.telephony.Rlog;
import android.telephony.SmsParameters;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CommandsInterface;
import com.mediatek.common.telephony.gsm.PBEntry;
import com.mediatek.internal.telephony.FemtoCellInfo;
import com.mediatek.internal.telephony.SrvccCallContext;
import com.mediatek.internal.telephony.uicc.PhbEntry;

public abstract class BaseCommands implements CommandsInterface {
    protected Registrant mBipProCmdRegistrant;
    protected int mBipPsType;
    protected Registrant mCDMACardEsnMeidRegistrant;
    protected Registrant mCallRelatedSuppSvcRegistrant;
    protected Registrant mCatCallSetUpRegistrant;
    protected Registrant mCatCcAlphaRegistrant;
    protected Registrant mCatEventRegistrant;
    protected Registrant mCatProCmdRegistrant;
    protected Registrant mCatSessionEndRegistrant;
    protected Registrant mCdmaSmsRegistrant;
    protected int mCdmaSubscription;
    protected Context mContext;
    protected Registrant mEfCspPlmnModeBitRegistrant;
    protected Registrant mEmergencyCallbackModeRegistrant;
    protected Registrant mEtwsNotificationRegistrant;
    protected Registrant mGsmBroadcastSmsRegistrant;
    protected Registrant mGsmSmsRegistrant;
    protected Registrant mIccSmsFullRegistrant;
    protected Registrant mIncomingCallIndicationRegistrant;
    protected Registrant mLceInfoRegistrant;
    protected Registrant mMeSmsFullRegistrant;
    protected Registrant mNITZTimeRegistrant;
    protected int mPhoneType;
    protected int mPreferredNetworkType;
    RadioCapability mRadioCapability;
    protected Registrant mRegistrationSuspendedRegistrant;
    protected Registrant mRestrictedStateRegistrant;
    protected Registrant mRingRegistrant;
    protected Registrant mSignalStrengthRegistrant;
    protected Registrant mSmsOnSimRegistrant;
    protected Registrant mSmsStatusRegistrant;
    protected Registrant mSpeechCodecInfoRegistrant;
    protected Registrant mSsRegistrant;
    protected Registrant mSsnRegistrant;
    protected Registrant mStkCallCtrlRegistrant;
    protected Registrant mStkEvdlCallRegistrant;
    protected Registrant mStkSetupMenuResetRegistrant;
    protected int mStkSwitchMode;
    protected Registrant mUSSDRegistrant;
    protected Registrant mUnsolOemHookRawRegistrant;
    protected CommandsInterface.RadioState mState = CommandsInterface.RadioState.RADIO_UNAVAILABLE;
    protected Object mStateMonitor = new Object();
    protected RegistrantList mRadioStateChangedRegistrants = new RegistrantList();
    protected RegistrantList mOnRegistrants = new RegistrantList();
    protected RegistrantList mAvailRegistrants = new RegistrantList();
    protected RegistrantList mOffOrNotAvailRegistrants = new RegistrantList();
    protected RegistrantList mNotAvailRegistrants = new RegistrantList();
    protected RegistrantList mCallStateRegistrants = new RegistrantList();
    protected RegistrantList mVoiceNetworkStateRegistrants = new RegistrantList();
    protected RegistrantList mDataNetworkStateRegistrants = new RegistrantList();
    protected RegistrantList mVoiceRadioTechChangedRegistrants = new RegistrantList();
    protected RegistrantList mImsNetworkStateChangedRegistrants = new RegistrantList();
    protected RegistrantList mIccStatusChangedRegistrants = new RegistrantList();
    protected RegistrantList mVoicePrivacyOnRegistrants = new RegistrantList();
    protected RegistrantList mVoicePrivacyOffRegistrants = new RegistrantList();
    protected RegistrantList mOtaProvisionRegistrants = new RegistrantList();
    protected RegistrantList mCallWaitingInfoRegistrants = new RegistrantList();
    protected RegistrantList mDisplayInfoRegistrants = new RegistrantList();
    protected RegistrantList mSignalInfoRegistrants = new RegistrantList();
    protected RegistrantList mNumberInfoRegistrants = new RegistrantList();
    protected RegistrantList mRedirNumInfoRegistrants = new RegistrantList();
    protected RegistrantList mLineControlInfoRegistrants = new RegistrantList();
    protected RegistrantList mT53ClirInfoRegistrants = new RegistrantList();
    protected RegistrantList mT53AudCntrlInfoRegistrants = new RegistrantList();
    protected RegistrantList mRingbackToneRegistrants = new RegistrantList();
    protected RegistrantList mResendIncallMuteRegistrants = new RegistrantList();
    protected RegistrantList mCdmaSubscriptionChangedRegistrants = new RegistrantList();
    protected RegistrantList mCdmaPrlChangedRegistrants = new RegistrantList();
    protected RegistrantList mExitEmergencyCallbackModeRegistrants = new RegistrantList();
    protected RegistrantList mRilConnectedRegistrants = new RegistrantList();
    protected RegistrantList mIccRefreshRegistrants = new RegistrantList();
    protected RegistrantList mRilCellInfoListRegistrants = new RegistrantList();
    protected RegistrantList mSubscriptionStatusRegistrants = new RegistrantList();
    protected RegistrantList mSrvccStateRegistrants = new RegistrantList();
    protected RegistrantList mHardwareConfigChangeRegistrants = new RegistrantList();
    protected RegistrantList mPhoneRadioCapabilityChangedRegistrants = new RegistrantList();
    protected RegistrantList mSessionChangedRegistrants = new RegistrantList();
    protected RegistrantList mCallForwardingInfoRegistrants = new RegistrantList();
    protected RegistrantList mCipherIndicationRegistrant = new RegistrantList();
    protected RegistrantList mVtStatusInfoRegistrants = new RegistrantList();
    protected RegistrantList mVtRingRegistrants = new RegistrantList();
    protected RegistrantList mCallRedialStateRegistrants = new RegistrantList();
    protected RegistrantList mRemoveRestrictEutranRegistrants = new RegistrantList();
    protected RegistrantList mResetAttachApnRegistrants = new RegistrantList();
    protected RegistrantList mMelockRegistrants = new RegistrantList();
    protected RegistrantList mPhbReadyRegistrants = new RegistrantList();
    protected boolean mIsCatchPhbStatus = false;
    protected RegistrantList mEconfSrvccRegistrants = new RegistrantList();
    protected RegistrantList mEconfResultRegistrants = new RegistrantList();
    protected RegistrantList mCallInfoRegistrants = new RegistrantList();
    protected RegistrantList mAttachApnChangedRegistrants = new RegistrantList();
    protected int mRilVersion = -1;
    protected RegistrantList mFemtoCellInfoRegistrants = new RegistrantList();
    protected RegistrantList mNeighboringInfoRegistrants = new RegistrantList();
    protected RegistrantList mNetworkInfoRegistrants = new RegistrantList();
    protected RegistrantList mNetworkExistRegistrants = new RegistrantList();
    protected RegistrantList mPlmnChangeNotificationRegistrant = new RegistrantList();
    protected Object mEmsrReturnValue = null;
    protected Object mEcopsReturnValue = null;
    protected Object mWPMonitor = new Object();
    protected RegistrantList mImsEnableRegistrants = new RegistrantList();
    protected RegistrantList mImsDisableRegistrants = new RegistrantList();
    protected RegistrantList mImsRegistrationInfoRegistrants = new RegistrantList();
    protected RegistrantList mDedicateBearerActivatedRegistrant = new RegistrantList();
    protected RegistrantList mDedicateBearerModifiedRegistrant = new RegistrantList();
    protected RegistrantList mDedicateBearerDeactivatedRegistrant = new RegistrantList();
    protected RegistrantList mPsNetworkStateRegistrants = new RegistrantList();
    protected RegistrantList mImeiLockRegistrant = new RegistrantList();
    protected RegistrantList mInvalidSimInfoRegistrant = new RegistrantList();
    protected RegistrantList mGetAvailableNetworkDoneRegistrant = new RegistrantList();
    protected Object mCfuReturnValue = null;
    protected boolean mIsSmsSimFull = false;
    protected boolean mIsSmsReady = false;
    protected Object mEspOrMeid = null;
    protected RegistrantList mSmsReadyRegistrants = new RegistrantList();
    protected RegistrantList mEpsNetworkFeatureSupportRegistrants = new RegistrantList();
    protected RegistrantList mEpsNetworkFeatureInfoRegistrants = new RegistrantList();
    protected RegistrantList mSrvccHandoverInfoIndicationRegistrants = new RegistrantList();
    protected RegistrantList mSsacBarringInfoRegistrants = new RegistrantList();
    protected RegistrantList mEmergencyBearerSupportInfoRegistrants = new RegistrantList();
    protected RegistrantList mAbnormalEventRegistrant = new RegistrantList();
    protected RegistrantList mImsiRefreshDoneRegistrant = new RegistrantList();
    protected RegistrantList mLteAccessStratumStateRegistrants = new RegistrantList();
    protected RegistrantList mModulationRegistrants = new RegistrantList();
    protected RegistrantList mNetworkEventRegistrants = new RegistrantList();
    protected RegistrantList mAcceptedRegistrant = new RegistrantList();
    protected RegistrantList mGmssRatChangedRegistrant = new RegistrantList();
    protected int[] mNewVoiceTech = {-1};
    protected RegistrantList mPcoStatusRegistrant = new RegistrantList();
    protected RegistrantList mSimMissing = new RegistrantList();
    protected RegistrantList mSimRecovery = new RegistrantList();
    protected RegistrantList mVirtualSimOn = new RegistrantList();
    protected RegistrantList mVirtualSimOff = new RegistrantList();
    protected RegistrantList mSimPlugOutRegistrants = new RegistrantList();
    protected RegistrantList mSimPlugInRegistrants = new RegistrantList();
    protected RegistrantList mTrayPlugInRegistrants = new RegistrantList();
    protected RegistrantList mCdmaCardTypeRegistrants = new RegistrantList();
    protected RegistrantList mCommonSlotNoChangedRegistrants = new RegistrantList();
    protected RegistrantList mDataAllowedRegistrants = new RegistrantList();
    protected Object mCdmaCardTypeValue = null;

    public BaseCommands(Context context) {
        this.mContext = context;
    }

    @Override
    public CommandsInterface.RadioState getRadioState() {
        return this.mState;
    }

    @Override
    public void registerForRadioStateChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        synchronized (this.mStateMonitor) {
            this.mRadioStateChangedRegistrants.add(r);
            r.notifyRegistrant();
        }
    }

    @Override
    public void unregisterForRadioStateChanged(Handler h) {
        synchronized (this.mStateMonitor) {
            this.mRadioStateChangedRegistrants.remove(h);
        }
    }

    @Override
    public void registerForImsNetworkStateChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mImsNetworkStateChangedRegistrants.add(r);
    }

    @Override
    public void unregisterForImsNetworkStateChanged(Handler h) {
        this.mImsNetworkStateChangedRegistrants.remove(h);
    }

    @Override
    public void registerForOn(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        synchronized (this.mStateMonitor) {
            this.mOnRegistrants.add(r);
            if (this.mState.isOn()) {
                r.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
            }
        }
    }

    @Override
    public void unregisterForOn(Handler h) {
        synchronized (this.mStateMonitor) {
            this.mOnRegistrants.remove(h);
        }
    }

    @Override
    public void registerForAvailable(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        synchronized (this.mStateMonitor) {
            this.mAvailRegistrants.add(r);
            if (this.mState.isAvailable()) {
                r.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
            }
        }
    }

    @Override
    public void unregisterForAvailable(Handler h) {
        synchronized (this.mStateMonitor) {
            this.mAvailRegistrants.remove(h);
        }
    }

    @Override
    public void registerForNotAvailable(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        synchronized (this.mStateMonitor) {
            this.mNotAvailRegistrants.add(r);
            if (!this.mState.isAvailable()) {
                r.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
            }
        }
    }

    @Override
    public void unregisterForNotAvailable(Handler h) {
        synchronized (this.mStateMonitor) {
            this.mNotAvailRegistrants.remove(h);
        }
    }

    @Override
    public void registerForOffOrNotAvailable(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        synchronized (this.mStateMonitor) {
            this.mOffOrNotAvailRegistrants.add(r);
            if (this.mState == CommandsInterface.RadioState.RADIO_OFF || !this.mState.isAvailable()) {
                r.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
            }
        }
    }

    @Override
    public void unregisterForOffOrNotAvailable(Handler h) {
        synchronized (this.mStateMonitor) {
            this.mOffOrNotAvailRegistrants.remove(h);
        }
    }

    @Override
    public void registerForCallStateChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mCallStateRegistrants.add(r);
    }

    @Override
    public void unregisterForCallStateChanged(Handler h) {
        this.mCallStateRegistrants.remove(h);
    }

    @Override
    public void registerForVoiceNetworkStateChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mVoiceNetworkStateRegistrants.add(r);
    }

    @Override
    public void unregisterForVoiceNetworkStateChanged(Handler h) {
        this.mVoiceNetworkStateRegistrants.remove(h);
    }

    @Override
    public void registerForDataNetworkStateChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mDataNetworkStateRegistrants.add(r);
    }

    @Override
    public void unregisterForDataNetworkStateChanged(Handler h) {
        this.mDataNetworkStateRegistrants.remove(h);
    }

    @Override
    public void registerForVoiceRadioTechChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        if (this.mNewVoiceTech[0] != -1) {
            r.notifyRegistrant(new AsyncResult((Object) null, this.mNewVoiceTech, (Throwable) null));
        }
        this.mVoiceRadioTechChangedRegistrants.add(r);
    }

    @Override
    public void unregisterForVoiceRadioTechChanged(Handler h) {
        this.mVoiceRadioTechChangedRegistrants.remove(h);
    }

    @Override
    public void registerForIccStatusChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mIccStatusChangedRegistrants.add(r);
    }

    @Override
    public void unregisterForIccStatusChanged(Handler h) {
        this.mIccStatusChangedRegistrants.remove(h);
    }

    @Override
    public void setOnNewGsmSms(Handler h, int what, Object obj) {
        this.mGsmSmsRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnNewGsmSms(Handler h) {
        if (this.mGsmSmsRegistrant == null || this.mGsmSmsRegistrant.getHandler() != h) {
            return;
        }
        this.mGsmSmsRegistrant.clear();
        this.mGsmSmsRegistrant = null;
    }

    @Override
    public void setOnNewCdmaSms(Handler h, int what, Object obj) {
        this.mCdmaSmsRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnNewCdmaSms(Handler h) {
        if (this.mCdmaSmsRegistrant == null || this.mCdmaSmsRegistrant.getHandler() != h) {
            return;
        }
        this.mCdmaSmsRegistrant.clear();
        this.mCdmaSmsRegistrant = null;
    }

    @Override
    public void setOnNewGsmBroadcastSms(Handler h, int what, Object obj) {
        this.mGsmBroadcastSmsRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnNewGsmBroadcastSms(Handler h) {
        if (this.mGsmBroadcastSmsRegistrant == null || this.mGsmBroadcastSmsRegistrant.getHandler() != h) {
            return;
        }
        this.mGsmBroadcastSmsRegistrant.clear();
        this.mGsmBroadcastSmsRegistrant = null;
    }

    @Override
    public void setOnSmsOnSim(Handler h, int what, Object obj) {
        this.mSmsOnSimRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnSmsOnSim(Handler h) {
        if (this.mSmsOnSimRegistrant == null || this.mSmsOnSimRegistrant.getHandler() != h) {
            return;
        }
        this.mSmsOnSimRegistrant.clear();
        this.mSmsOnSimRegistrant = null;
    }

    @Override
    public void setOnSmsStatus(Handler h, int what, Object obj) {
        this.mSmsStatusRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnSmsStatus(Handler h) {
        if (this.mSmsStatusRegistrant == null || this.mSmsStatusRegistrant.getHandler() != h) {
            return;
        }
        this.mSmsStatusRegistrant.clear();
        this.mSmsStatusRegistrant = null;
    }

    @Override
    public void setOnSignalStrengthUpdate(Handler h, int what, Object obj) {
        this.mSignalStrengthRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnSignalStrengthUpdate(Handler h) {
        if (this.mSignalStrengthRegistrant == null || this.mSignalStrengthRegistrant.getHandler() != h) {
            return;
        }
        this.mSignalStrengthRegistrant.clear();
        this.mSignalStrengthRegistrant = null;
    }

    @Override
    public void setOnNITZTime(Handler h, int what, Object obj) {
        this.mNITZTimeRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnNITZTime(Handler h) {
        if (this.mNITZTimeRegistrant == null || this.mNITZTimeRegistrant.getHandler() != h) {
            return;
        }
        this.mNITZTimeRegistrant.clear();
        this.mNITZTimeRegistrant = null;
    }

    @Override
    public void setOnUSSD(Handler h, int what, Object obj) {
        this.mUSSDRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnUSSD(Handler h) {
        if (this.mUSSDRegistrant == null || this.mUSSDRegistrant.getHandler() != h) {
            return;
        }
        this.mUSSDRegistrant.clear();
        this.mUSSDRegistrant = null;
    }

    @Override
    public void setOnSuppServiceNotification(Handler h, int what, Object obj) {
        this.mSsnRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnSuppServiceNotification(Handler h) {
        if (this.mSsnRegistrant == null || this.mSsnRegistrant.getHandler() != h) {
            return;
        }
        this.mSsnRegistrant.clear();
        this.mSsnRegistrant = null;
    }

    @Override
    public void setOnCatSessionEnd(Handler h, int what, Object obj) {
        this.mCatSessionEndRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnCatSessionEnd(Handler h) {
        if (this.mCatSessionEndRegistrant == null || this.mCatSessionEndRegistrant.getHandler() != h) {
            return;
        }
        this.mCatSessionEndRegistrant.clear();
        this.mCatSessionEndRegistrant = null;
    }

    @Override
    public void setOnCatProactiveCmd(Handler h, int what, Object obj) {
        this.mCatProCmdRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnCatProactiveCmd(Handler h) {
        if (this.mCatProCmdRegistrant == null || this.mCatProCmdRegistrant.getHandler() != h) {
            return;
        }
        this.mCatProCmdRegistrant.clear();
        this.mCatProCmdRegistrant = null;
    }

    @Override
    public void setOnBipProactiveCmd(Handler h, int what, Object obj) {
        this.mBipProCmdRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnBipProactiveCmd(Handler h) {
        if (this.mBipProCmdRegistrant == null || this.mBipProCmdRegistrant.getHandler() != h) {
            return;
        }
        this.mBipProCmdRegistrant.clear();
        this.mBipProCmdRegistrant = null;
    }

    @Override
    public void setOnCatEvent(Handler h, int what, Object obj) {
        this.mCatEventRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnCatEvent(Handler h) {
        if (this.mCatEventRegistrant == null || this.mCatEventRegistrant.getHandler() != h) {
            return;
        }
        this.mCatEventRegistrant.clear();
        this.mCatEventRegistrant = null;
    }

    @Override
    public void setOnCatCallSetUp(Handler h, int what, Object obj) {
        this.mCatCallSetUpRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnCatCallSetUp(Handler h) {
        if (this.mCatCallSetUpRegistrant == null || this.mCatCallSetUpRegistrant.getHandler() != h) {
            return;
        }
        this.mCatCallSetUpRegistrant.clear();
        this.mCatCallSetUpRegistrant = null;
    }

    @Override
    public void setOnIccSmsFull(Handler h, int what, Object obj) {
        this.mIccSmsFullRegistrant = new Registrant(h, what, obj);
        if (!this.mIsSmsSimFull) {
            return;
        }
        this.mIccSmsFullRegistrant.notifyRegistrant();
        this.mIsSmsSimFull = false;
    }

    @Override
    public void unSetOnIccSmsFull(Handler h) {
        if (this.mIccSmsFullRegistrant == null || this.mIccSmsFullRegistrant.getHandler() != h) {
            return;
        }
        this.mIccSmsFullRegistrant.clear();
        this.mIccSmsFullRegistrant = null;
    }

    @Override
    public void registerForIccRefresh(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mIccRefreshRegistrants.add(r);
    }

    @Override
    public void setOnIccRefresh(Handler h, int what, Object obj) {
        registerForIccRefresh(h, what, obj);
    }

    @Override
    public void setEmergencyCallbackMode(Handler h, int what, Object obj) {
        this.mEmergencyCallbackModeRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unregisterForIccRefresh(Handler h) {
        this.mIccRefreshRegistrants.remove(h);
    }

    @Override
    public void unsetOnIccRefresh(Handler h) {
        unregisterForIccRefresh(h);
    }

    @Override
    public void setOnCallRing(Handler h, int what, Object obj) {
        this.mRingRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnCallRing(Handler h) {
        if (this.mRingRegistrant == null || this.mRingRegistrant.getHandler() != h) {
            return;
        }
        this.mRingRegistrant.clear();
        this.mRingRegistrant = null;
    }

    @Override
    public void setOnSs(Handler h, int what, Object obj) {
        this.mSsRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnSs(Handler h) {
        this.mSsRegistrant.clear();
    }

    @Override
    public void setOnCatCcAlphaNotify(Handler h, int what, Object obj) {
        this.mCatCcAlphaRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnCatCcAlphaNotify(Handler h) {
        this.mCatCcAlphaRegistrant.clear();
    }

    @Override
    public void setStkEvdlCallByAP(int enabled, Message response) {
    }

    @Override
    public void setOnStkEvdlCall(Handler h, int what, Object obj) {
        this.mStkEvdlCallRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnStkEvdlCall(Handler h) {
        this.mStkEvdlCallRegistrant.clear();
    }

    @Override
    public void setOnStkSetupMenuReset(Handler h, int what, Object obj) {
        this.mStkSetupMenuResetRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnStkSetupMenuReset(Handler h) {
        this.mStkSetupMenuResetRegistrant.clear();
    }

    @Override
    public void setOnStkCallCtrl(Handler h, int what, Object obj) {
        this.mStkCallCtrlRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnStkCallCtrl(Handler h) {
        this.mStkCallCtrlRegistrant.clear();
    }

    @Override
    public void registerForInCallVoicePrivacyOn(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mVoicePrivacyOnRegistrants.add(r);
    }

    @Override
    public void unregisterForInCallVoicePrivacyOn(Handler h) {
        this.mVoicePrivacyOnRegistrants.remove(h);
    }

    @Override
    public void registerForInCallVoicePrivacyOff(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mVoicePrivacyOffRegistrants.add(r);
    }

    @Override
    public void unregisterForInCallVoicePrivacyOff(Handler h) {
        this.mVoicePrivacyOffRegistrants.remove(h);
    }

    @Override
    public void setOnRestrictedStateChanged(Handler h, int what, Object obj) {
        this.mRestrictedStateRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnRestrictedStateChanged(Handler h) {
        if (this.mRestrictedStateRegistrant == null || this.mRestrictedStateRegistrant.getHandler() != h) {
            return;
        }
        this.mRestrictedStateRegistrant.clear();
        this.mRestrictedStateRegistrant = null;
    }

    @Override
    public void registerForDisplayInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mDisplayInfoRegistrants.add(r);
    }

    @Override
    public void unregisterForDisplayInfo(Handler h) {
        this.mDisplayInfoRegistrants.remove(h);
    }

    @Override
    public void registerForCallWaitingInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mCallWaitingInfoRegistrants.add(r);
    }

    @Override
    public void unregisterForCallWaitingInfo(Handler h) {
        this.mCallWaitingInfoRegistrants.remove(h);
    }

    @Override
    public void registerForSignalInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mSignalInfoRegistrants.add(r);
    }

    @Override
    public void setOnUnsolOemHookRaw(Handler h, int what, Object obj) {
        this.mUnsolOemHookRawRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnUnsolOemHookRaw(Handler h) {
        if (this.mUnsolOemHookRawRegistrant == null || this.mUnsolOemHookRawRegistrant.getHandler() != h) {
            return;
        }
        this.mUnsolOemHookRawRegistrant.clear();
        this.mUnsolOemHookRawRegistrant = null;
    }

    @Override
    public void unregisterForSignalInfo(Handler h) {
        this.mSignalInfoRegistrants.remove(h);
    }

    @Override
    public void registerForCdmaOtaProvision(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mOtaProvisionRegistrants.add(r);
    }

    @Override
    public void unregisterForCdmaOtaProvision(Handler h) {
        this.mOtaProvisionRegistrants.remove(h);
    }

    @Override
    public void registerForNumberInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mNumberInfoRegistrants.add(r);
    }

    @Override
    public void unregisterForNumberInfo(Handler h) {
        this.mNumberInfoRegistrants.remove(h);
    }

    @Override
    public void registerForRedirectedNumberInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mRedirNumInfoRegistrants.add(r);
    }

    @Override
    public void unregisterForRedirectedNumberInfo(Handler h) {
        this.mRedirNumInfoRegistrants.remove(h);
    }

    @Override
    public void registerForLineControlInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mLineControlInfoRegistrants.add(r);
    }

    @Override
    public void unregisterForLineControlInfo(Handler h) {
        this.mLineControlInfoRegistrants.remove(h);
    }

    @Override
    public void registerFoT53ClirlInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mT53ClirInfoRegistrants.add(r);
    }

    @Override
    public void unregisterForT53ClirInfo(Handler h) {
        this.mT53ClirInfoRegistrants.remove(h);
    }

    @Override
    public void registerForT53AudioControlInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mT53AudCntrlInfoRegistrants.add(r);
    }

    @Override
    public void unregisterForT53AudioControlInfo(Handler h) {
        this.mT53AudCntrlInfoRegistrants.remove(h);
    }

    @Override
    public void registerForRingbackTone(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mRingbackToneRegistrants.add(r);
    }

    @Override
    public void unregisterForRingbackTone(Handler h) {
        this.mRingbackToneRegistrants.remove(h);
    }

    @Override
    public void registerForResendIncallMute(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mResendIncallMuteRegistrants.add(r);
    }

    @Override
    public void unregisterForResendIncallMute(Handler h) {
        this.mResendIncallMuteRegistrants.remove(h);
    }

    @Override
    public void registerForCdmaSubscriptionChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mCdmaSubscriptionChangedRegistrants.add(r);
    }

    @Override
    public void unregisterForCdmaSubscriptionChanged(Handler h) {
        this.mCdmaSubscriptionChangedRegistrants.remove(h);
    }

    @Override
    public void registerForCdmaPrlChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mCdmaPrlChangedRegistrants.add(r);
    }

    @Override
    public void unregisterForCdmaPrlChanged(Handler h) {
        this.mCdmaPrlChangedRegistrants.remove(h);
    }

    @Override
    public void registerForExitEmergencyCallbackMode(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mExitEmergencyCallbackModeRegistrants.add(r);
    }

    @Override
    public void unregisterForExitEmergencyCallbackMode(Handler h) {
        this.mExitEmergencyCallbackModeRegistrants.remove(h);
    }

    @Override
    public void registerForHardwareConfigChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mHardwareConfigChangeRegistrants.add(r);
    }

    @Override
    public void unregisterForHardwareConfigChanged(Handler h) {
        this.mHardwareConfigChangeRegistrants.remove(h);
    }

    @Override
    public void registerForRilConnected(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mRilConnectedRegistrants.add(r);
        if (this.mRilVersion == -1) {
            return;
        }
        r.notifyRegistrant(new AsyncResult((Object) null, new Integer(this.mRilVersion), (Throwable) null));
    }

    @Override
    public void unregisterForRilConnected(Handler h) {
        this.mRilConnectedRegistrants.remove(h);
    }

    @Override
    public void registerForSubscriptionStatusChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mSubscriptionStatusRegistrants.add(r);
    }

    @Override
    public void unregisterForSubscriptionStatusChanged(Handler h) {
        this.mSubscriptionStatusRegistrants.remove(h);
    }

    protected void setRadioState(CommandsInterface.RadioState newState) {
        synchronized (this.mStateMonitor) {
            CommandsInterface.RadioState oldState = this.mState;
            this.mState = newState;
            if (oldState == this.mState) {
                return;
            }
            this.mRadioStateChangedRegistrants.notifyRegistrants();
            if (this.mState.isAvailable() && !oldState.isAvailable()) {
                this.mAvailRegistrants.notifyRegistrants();
                onRadioAvailable();
            }
            if (!this.mState.isAvailable() && oldState.isAvailable()) {
                this.mNotAvailRegistrants.notifyRegistrants();
            }
            if (this.mState.isOn() && !oldState.isOn()) {
                this.mOnRegistrants.notifyRegistrants();
            }
            if ((!this.mState.isOn() || !this.mState.isAvailable()) && oldState.isOn() && oldState.isAvailable()) {
                this.mOffOrNotAvailRegistrants.notifyRegistrants();
            }
        }
    }

    @Override
    public void setModemPower(boolean power, Message response) {
    }

    protected void onRadioAvailable() {
    }

    @Override
    public int getLteOnCdmaMode() {
        return TelephonyManager.getLteOnCdmaModeStatic();
    }

    @Override
    public void registerForCellInfoList(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mRilCellInfoListRegistrants.add(r);
    }

    @Override
    public void unregisterForCellInfoList(Handler h) {
        this.mRilCellInfoListRegistrants.remove(h);
    }

    @Override
    public void registerForSrvccStateChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mSrvccStateRegistrants.add(r);
    }

    @Override
    public void unregisterForSrvccStateChanged(Handler h) {
        this.mSrvccStateRegistrants.remove(h);
    }

    @Override
    public void testingEmergencyCall() {
    }

    @Override
    public int getRilVersion() {
        return this.mRilVersion;
    }

    @Override
    public void setUiccSubscription(int slotId, int appIndex, int subId, int subStatus, Message response) {
    }

    @Override
    public void setDataAllowed(boolean allowed, Message response) {
    }

    @Override
    public void requestShutdown(Message result) {
    }

    @Override
    public void getRadioCapability(Message result) {
    }

    @Override
    public void setRadioCapability(RadioCapability rc, Message response) {
    }

    @Override
    public void registerForRadioCapabilityChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mPhoneRadioCapabilityChangedRegistrants.add(r);
    }

    @Override
    public void unregisterForRadioCapabilityChanged(Handler h) {
        this.mPhoneRadioCapabilityChangedRegistrants.remove(h);
    }

    @Override
    public void startLceService(int reportIntervalMs, boolean pullMode, Message result) {
    }

    @Override
    public void stopLceService(Message result) {
    }

    @Override
    public void pullLceData(Message result) {
    }

    @Override
    public void registerForLceInfo(Handler h, int what, Object obj) {
        this.mLceInfoRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unregisterForLceInfo(Handler h) {
        if (this.mLceInfoRegistrant == null || this.mLceInfoRegistrant.getHandler() != h) {
            return;
        }
        this.mLceInfoRegistrant.clear();
        this.mLceInfoRegistrant = null;
    }

    @Override
    public void setOnIncomingCallIndication(Handler h, int what, Object obj) {
        this.mIncomingCallIndicationRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unsetOnIncomingCallIndication(Handler h) {
        this.mIncomingCallIndicationRegistrant.clear();
    }

    @Override
    public void registerForCipherIndication(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mCipherIndicationRegistrant.add(r);
    }

    @Override
    public void unregisterForCipherIndication(Handler h) {
        this.mCipherIndicationRegistrant.remove(h);
    }

    @Override
    public void setOnSpeechCodecInfo(Handler h, int what, Object obj) {
        this.mSpeechCodecInfoRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnSpeechCodecInfo(Handler h) {
        if (this.mSpeechCodecInfoRegistrant == null || this.mSpeechCodecInfoRegistrant.getHandler() != h) {
            return;
        }
        this.mSpeechCodecInfoRegistrant.clear();
        this.mSpeechCodecInfoRegistrant = null;
    }

    @Override
    public void registerForVtStatusInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mVtStatusInfoRegistrants.add(r);
    }

    @Override
    public void unregisterForVtStatusInfo(Handler h) {
        this.mVtStatusInfoRegistrants.remove(h);
    }

    @Override
    public void registerForVtRingInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mVtRingRegistrants.add(r);
    }

    @Override
    public void unregisterForVtRingInfo(Handler h) {
        this.mVtRingRegistrants.remove(h);
    }

    @Override
    public void registerForCallRedialState(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mCallRedialStateRegistrants.add(r);
    }

    @Override
    public void unregisterForCallRedialState(Handler h) {
        this.mCallRedialStateRegistrants.remove(h);
    }

    @Override
    public void setDataOnToMD(boolean enable, Message result) {
    }

    @Override
    public void setRemoveRestrictEutranMode(boolean enable, Message result) {
    }

    @Override
    public void registerForRemoveRestrictEutran(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mRemoveRestrictEutranRegistrants.add(r);
    }

    @Override
    public void unregisterForRemoveRestrictEutran(Handler h) {
        this.mRemoveRestrictEutranRegistrants.remove(h);
    }

    @Override
    public void setInitialAttachApn(String apn, String protocol, int authType, String username, String password, Object obj, Message result) {
    }

    @Override
    public void registerForResetAttachApn(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mResetAttachApnRegistrants.add(r);
    }

    @Override
    public void unregisterForResetAttachApn(Handler h) {
        this.mResetAttachApnRegistrants.remove(h);
    }

    @Override
    public void registerForAttachApnChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mAttachApnChangedRegistrants.add(r);
    }

    @Override
    public void unregisterForAttachApnChanged(Handler h) {
        this.mAttachApnChangedRegistrants.remove(h);
    }

    @Override
    public void setupDataCall(int radioTechnology, int profile, String apn, String user, String password, int authType, String protocol, Message result) {
    }

    @Override
    public void setupDataCall(int radioTechnology, int profile, String apn, String user, String password, int authType, String protocol, int interfaceId, Message result) {
    }

    @Override
    public void syncApnTable(String index, String apnClass, String apn, String apnType, String apnBearer, String apnEnable, String apnTime, String maxConn, String maxConnTime, String waitTime, String throttlingTime, String inactiveTimer, Message result) {
    }

    @Override
    public void syncDataSettingsToMd(boolean dataSetting, boolean dataRoamingSetting, Message result) {
    }

    @Override
    public void setTrm(int mode, Message result) {
    }

    @Override
    public void registerForCallForwardingInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mCallForwardingInfoRegistrants.add(r);
        if (this.mCfuReturnValue == null) {
            return;
        }
        r.notifyRegistrant(new AsyncResult((Object) null, this.mCfuReturnValue, (Throwable) null));
    }

    @Override
    public void unregisterForCallForwardingInfo(Handler h) {
        this.mCallForwardingInfoRegistrants.remove(h);
    }

    @Override
    public void setOnCallRelatedSuppSvc(Handler h, int what, Object obj) {
        this.mCallRelatedSuppSvcRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnCallRelatedSuppSvc(Handler h) {
        this.mCallRelatedSuppSvcRegistrant.clear();
    }

    @Override
    public void hangupAll(Message result) {
    }

    @Override
    public void forceReleaseCall(int index, Message response) {
    }

    @Override
    public void setCallIndication(int mode, int callId, int seqNumber, Message response) {
    }

    @Override
    public void emergencyDial(String address, int clirMode, UUSInfo uusInfo, Message result) {
    }

    @Override
    public void setEccServiceCategory(int serviceCategory) {
    }

    @Override
    public void setSpeechCodecInfo(boolean enable, Message response) {
    }

    @Override
    public void vtDial(String address, int clirMode, UUSInfo uusInfo, Message result) {
    }

    @Override
    public void acceptVtCallWithVoiceOnly(int callId, Message result) {
    }

    @Override
    public void replaceVtCall(int index, Message result) {
    }

    @Override
    public void sendCNAPSS(String cnapssString, Message response) {
    }

    @Override
    public void setCLIP(boolean enable, Message response) {
    }

    @Override
    public void openIccApplication(int application, Message response) {
    }

    @Override
    public void getIccApplicationStatus(int sessionId, Message result) {
    }

    @Override
    public void registerForSessionChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mSessionChangedRegistrants.add(r);
    }

    @Override
    public void unregisterForSessionChanged(Handler h) {
        this.mSessionChangedRegistrants.remove(h);
    }

    @Override
    public void queryNetworkLock(int categrory, Message response) {
    }

    @Override
    public void setNetworkLock(int catagory, int lockop, String password, String data_imsi, String gid1, String gid2, Message response) {
    }

    @Override
    public void doGeneralSimAuthentication(int sessionId, int mode, int tag, String param1, String param2, Message response) {
    }

    @Override
    public void registerForSimMissing(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mSimMissing.add(r);
    }

    @Override
    public void unregisterForSimMissing(Handler h) {
        this.mSimMissing.remove(h);
    }

    @Override
    public void registerForSimRecovery(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mSimRecovery.add(r);
    }

    @Override
    public void unregisterForSimRecovery(Handler h) {
        this.mSimRecovery.remove(h);
    }

    @Override
    public void registerForVirtualSimOn(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mVirtualSimOn.add(r);
    }

    @Override
    public void unregisterForVirtualSimOn(Handler h) {
        this.mVirtualSimOn.remove(h);
    }

    @Override
    public void registerForVirtualSimOff(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mVirtualSimOff.add(r);
    }

    @Override
    public void unregisterForVirtualSimOff(Handler h) {
        this.mVirtualSimOff.remove(h);
    }

    @Override
    public void registerForSimPlugOut(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mSimPlugOutRegistrants.add(r);
    }

    @Override
    public void unregisterForSimPlugOut(Handler h) {
        this.mSimPlugOutRegistrants.remove(h);
    }

    @Override
    public void registerForSimPlugIn(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mSimPlugInRegistrants.add(r);
    }

    @Override
    public void unregisterForSimPlugIn(Handler h) {
        this.mSimPlugInRegistrants.remove(h);
    }

    @Override
    public void registerForTrayPlugIn(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mTrayPlugInRegistrants.add(r);
    }

    @Override
    public void unregisterForTrayPlugIn(Handler h) {
        this.mTrayPlugInRegistrants.remove(h);
    }

    @Override
    public void registerForCommonSlotNoChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mCommonSlotNoChangedRegistrants.add(r);
    }

    @Override
    public void unregisterForCommonSlotNoChanged(Handler h) {
        this.mCommonSlotNoChangedRegistrants.remove(h);
    }

    @Override
    public void registerSetDataAllowed(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mDataAllowedRegistrants.add(r);
    }

    @Override
    public void unregisterSetDataAllowed(Handler h) {
        this.mDataAllowedRegistrants.remove(h);
    }

    @Override
    public void sendBTSIMProfile(int nAction, int nType, String strData, Message response) {
    }

    @Override
    public void registerForEfCspPlmnModeBitChanged(Handler h, int what, Object obj) {
        this.mEfCspPlmnModeBitRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unregisterForEfCspPlmnModeBitChanged(Handler h) {
        this.mEfCspPlmnModeBitRegistrant.clear();
    }

    @Override
    public void queryPhbStorageInfo(int type, Message response) {
    }

    @Override
    public void writePhbEntry(PhbEntry entry, Message result) {
    }

    @Override
    public void ReadPhbEntry(int type, int bIndex, int eIndex, Message response) {
    }

    @Override
    public void registerForPhbReady(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        Rlog.d("RILJ", "call registerForPhbReady Handler : " + h);
        this.mPhbReadyRegistrants.add(r);
    }

    @Override
    public void unregisterForPhbReady(Handler h) {
        this.mPhbReadyRegistrants.remove(h);
    }

    @Override
    public void queryUPBCapability(Message response) {
    }

    @Override
    public void editUPBEntry(int entryType, int adnIndex, int entryIndex, String strVal, String tonForNum, String aasAnrIndex, Message response) {
    }

    @Override
    public void editUPBEntry(int entryType, int adnIndex, int entryIndex, String strVal, String tonForNum, Message response) {
    }

    @Override
    public void deleteUPBEntry(int entryType, int adnIndex, int entryIndex, Message response) {
    }

    @Override
    public void readUPBGasList(int startIndex, int endIndex, Message response) {
    }

    @Override
    public void readUPBGrpEntry(int adnIndex, Message response) {
    }

    @Override
    public void writeUPBGrpEntry(int adnIndex, int[] grpIds, Message response) {
    }

    @Override
    public void getPhoneBookStringsLength(Message result) {
    }

    @Override
    public void getPhoneBookMemStorage(Message result) {
    }

    @Override
    public void setPhoneBookMemStorage(String storage, String password, Message result) {
    }

    @Override
    public void readPhoneBookEntryExt(int index1, int index2, Message result) {
    }

    @Override
    public void writePhoneBookEntryExt(PBEntry entry, Message result) {
    }

    @Override
    public void queryUPBAvailable(int eftype, int fileIndex, Message response) {
    }

    @Override
    public void readUPBEmailEntry(int adnIndex, int fileIndex, Message response) {
    }

    @Override
    public void readUPBSneEntry(int adnIndex, int fileIndex, Message response) {
    }

    @Override
    public void readUPBAnrEntry(int adnIndex, int fileIndex, Message response) {
    }

    @Override
    public void readUPBAasList(int startIndex, int endIndex, Message response) {
    }

    @Override
    public void setLteAccessStratumReport(boolean enable, Message result) {
    }

    @Override
    public void setLteUplinkDataTransfer(int state, int interfaceId, Message result) {
    }

    @Override
    public void registerForLteAccessStratumState(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mLteAccessStratumStateRegistrants.add(r);
    }

    @Override
    public void unregisterForLteAccessStratumState(Handler h) {
        this.mLteAccessStratumStateRegistrants.remove(h);
    }

    @Override
    public void registerForSmsReady(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mSmsReadyRegistrants.add(r);
        if (!this.mIsSmsReady) {
            return;
        }
        r.notifyRegistrant();
    }

    @Override
    public void unregisterForSmsReady(Handler h) {
        this.mSmsReadyRegistrants.remove(h);
    }

    @Override
    public void setOnMeSmsFull(Handler h, int what, Object obj) {
        this.mMeSmsFullRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnMeSmsFull(Handler h) {
        this.mMeSmsFullRegistrant.clear();
    }

    @Override
    public void getSmsParameters(Message response) {
    }

    @Override
    public void setSmsParameters(SmsParameters params, Message response) {
    }

    @Override
    public void setEtws(int mode, Message result) {
    }

    @Override
    public void setOnEtwsNotification(Handler h, int what, Object obj) {
        this.mEtwsNotificationRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnEtwsNotification(Handler h) {
        this.mEtwsNotificationRegistrant.clear();
    }

    @Override
    public void setCellBroadcastChannelConfigInfo(String config, int cb_set_type, Message response) {
    }

    @Override
    public void setCellBroadcastLanguageConfigInfo(String config, Message response) {
    }

    @Override
    public void queryCellBroadcastConfigInfo(Message response) {
    }

    @Override
    public void removeCellBroadcastMsg(int channelId, int serialId, Message response) {
    }

    @Override
    public void getSmsSimMemoryStatus(Message result) {
    }

    @Override
    public void setCDMACardInitalEsnMeid(Handler h, int what, Object obj) {
        this.mCDMACardEsnMeidRegistrant = new Registrant(h, what, obj);
        if (this.mEspOrMeid == null) {
            return;
        }
        this.mCDMACardEsnMeidRegistrant.notifyRegistrant(new AsyncResult((Object) null, this.mEspOrMeid, (Throwable) null));
    }

    @Override
    public void unSetCDMACardInitalEsnMeid(Handler h) {
        this.mCDMACardEsnMeidRegistrant.clear();
    }

    @Override
    public void registerForNeighboringInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mNeighboringInfoRegistrants.add(r);
    }

    @Override
    public void unregisterForNeighboringInfo(Handler h) {
        this.mNeighboringInfoRegistrants.remove(h);
    }

    @Override
    public void registerForNetworkInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mNetworkInfoRegistrants.add(r);
    }

    @Override
    public void unregisterForNetworkInfo(Handler h) {
        this.mNetworkInfoRegistrants.remove(h);
    }

    @Override
    public void setInvalidSimInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mInvalidSimInfoRegistrant.add(r);
    }

    @Override
    public void unSetInvalidSimInfo(Handler h) {
        this.mInvalidSimInfoRegistrant.remove(h);
    }

    @Override
    public void registerForIMEILock(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mImeiLockRegistrant.add(r);
    }

    @Override
    public void unregisterForIMEILock(Handler h) {
        this.mImeiLockRegistrant.remove(h);
    }

    @Override
    public void setNetworkSelectionModeManualWithAct(String operatorNumeric, String act, Message result) {
    }

    @Override
    public void setNetworkSelectionModeSemiAutomatic(String operatorNumeric, String act, Message response) {
    }

    @Override
    public void cancelAvailableNetworks(Message response) {
    }

    @Override
    public void registerForGetAvailableNetworksDone(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mGetAvailableNetworkDoneRegistrant.add(r);
    }

    @Override
    public void unregisterForGetAvailableNetworksDone(Handler h) {
        this.mGetAvailableNetworkDoneRegistrant.remove(h);
    }

    @Override
    public void getPOLCapabilty(Message response) {
    }

    @Override
    public void getCurrentPOLList(Message response) {
    }

    @Override
    public void setPOLEntry(int index, String numeric, int nAct, Message response) {
    }

    @Override
    public void getFemtoCellList(String operatorNumeric, int rat, Message response) {
    }

    @Override
    public void abortFemtoCellList(Message response) {
    }

    @Override
    public void selectFemtoCell(FemtoCellInfo femtocell, Message response) {
    }

    @Override
    public void registerForFemtoCellInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mFemtoCellInfoRegistrants.add(r);
    }

    @Override
    public void registerForPsNetworkStateChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mPsNetworkStateRegistrants.add(r);
    }

    @Override
    public void unregisterForPsNetworkStateChanged(Handler h) {
        this.mPsNetworkStateRegistrants.remove(h);
    }

    @Override
    public boolean isGettingAvailableNetworks() {
        return false;
    }

    @Override
    public void unregisterForFemtoCellInfo(Handler h) {
        this.mFemtoCellInfoRegistrants.remove(h);
    }

    @Override
    public void registerForImsEnable(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mImsEnableRegistrants.add(r);
    }

    @Override
    public void unregisterForImsEnable(Handler h) {
        this.mImsEnableRegistrants.remove(h);
    }

    @Override
    public void registerForImsDisable(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mImsDisableRegistrants.add(r);
    }

    @Override
    public void unregisterForImsDisable(Handler h) {
        this.mImsDisableRegistrants.remove(h);
    }

    @Override
    public void registerForImsRegistrationInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mImsRegistrationInfoRegistrants.add(r);
    }

    @Override
    public void unregisterForImsRegistrationInfo(Handler h) {
        this.mImsRegistrationInfoRegistrants.remove(h);
    }

    @Override
    public void setIMSEnabled(boolean enable, Message response) {
    }

    @Override
    public void registerForImsDisableDone(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForImsDisableDone(Handler h) {
    }

    @Override
    public void setOnPlmnChangeNotification(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        synchronized (this.mWPMonitor) {
            this.mPlmnChangeNotificationRegistrant.add(r);
            if (this.mEcopsReturnValue != null) {
                r.notifyRegistrant(new AsyncResult((Object) null, this.mEcopsReturnValue, (Throwable) null));
                this.mEcopsReturnValue = null;
            }
        }
    }

    @Override
    public void unSetOnPlmnChangeNotification(Handler h) {
        synchronized (this.mWPMonitor) {
            this.mPlmnChangeNotificationRegistrant.remove(h);
        }
    }

    @Override
    public void setOnRegistrationSuspended(Handler h, int what, Object obj) {
        synchronized (this.mWPMonitor) {
            this.mRegistrationSuspendedRegistrant = new Registrant(h, what, obj);
            if (this.mEmsrReturnValue != null) {
                this.mRegistrationSuspendedRegistrant.notifyRegistrant(new AsyncResult((Object) null, this.mEmsrReturnValue, (Throwable) null));
                this.mEmsrReturnValue = null;
            }
        }
    }

    @Override
    public void unSetOnRegistrationSuspended(Handler h) {
        synchronized (this.mWPMonitor) {
            this.mRegistrationSuspendedRegistrant.clear();
        }
    }

    @Override
    public void registerForMelockChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mMelockRegistrants.add(r);
    }

    @Override
    public void unregisterForMelockChanged(Handler h) {
        this.mMelockRegistrants.remove(h);
    }

    @Override
    public void setFDMode(int mode, int parameter1, int parameter2, Message response) {
    }

    public void registerForEpsNetworkFeatureSupport(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mEpsNetworkFeatureSupportRegistrants.add(r);
    }

    public void unregisterForEpsNetworkFeatureSupport(Handler h) {
        this.mEpsNetworkFeatureSupportRegistrants.remove(h);
    }

    @Override
    public void registerForEconfSrvcc(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mEconfSrvccRegistrants.add(r);
    }

    @Override
    public void unregisterForEconfSrvcc(Handler h) {
        this.mEconfSrvccRegistrants.remove(h);
    }

    @Override
    public void registerForEconfResult(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mEconfResultRegistrants.add(r);
    }

    @Override
    public void unregisterForEconfResult(Handler h) {
        this.mEconfResultRegistrants.remove(h);
    }

    @Override
    public void registerForCallInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mCallInfoRegistrants.add(r);
    }

    @Override
    public void unregisterForCallInfo(Handler h) {
        this.mCallInfoRegistrants.remove(h);
    }

    @Override
    public void addConferenceMember(int confCallId, String address, int callIdToAdd, Message response) {
    }

    @Override
    public void removeConferenceMember(int confCallId, String address, int callIdToRemove, Message response) {
    }

    @Override
    public void resumeCall(int callIdToResume, Message response) {
    }

    @Override
    public void holdCall(int callIdToHold, Message response) {
    }

    public void registerForEpsNetworkFeatureInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mEpsNetworkFeatureInfoRegistrants.add(r);
    }

    public void unregisterForEpsNetworkFeatureInfo(Handler h) {
        this.mEpsNetworkFeatureInfoRegistrants.remove(h);
    }

    public void registerForSsacBarringInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mSsacBarringInfoRegistrants.add(r);
    }

    public void unregisterForSsacBarringInfo(Handler h) {
        this.mSsacBarringInfoRegistrants.remove(h);
    }

    public void registerForSrvccHandoverInfoIndication(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mSrvccHandoverInfoIndicationRegistrants.add(r);
    }

    public void unregisterForSrvccHandoverInfoIndication(Handler h) {
        this.mSrvccHandoverInfoIndicationRegistrants.remove(h);
    }

    public void registerForEmergencyBearerSupportInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mEmergencyBearerSupportInfoRegistrants.add(r);
    }

    public void unregisterForEmergencyBearerSupportInfo(Handler h) {
        this.mEmergencyBearerSupportInfoRegistrants.remove(h);
    }

    @Override
    public void sendScreenState(boolean on) {
    }

    @Override
    public void setDataCentric(boolean enable, Message response) {
    }

    @Override
    public void setImsCallStatus(boolean existed, Message response) {
    }

    public void setSrvccCallContextTransfer(int numberOfCall, SrvccCallContext[] callList) {
    }

    @Override
    public void updateImsRegistrationStatus(int regState, int regType, int reason) {
    }

    @Override
    public void registerForAbnormalEvent(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mAbnormalEventRegistrant.add(r);
    }

    @Override
    public void unregisterForAbnormalEvent(Handler h) {
        this.mAbnormalEventRegistrant.remove(h);
    }

    @Override
    public int getDisplayState() {
        return 0;
    }

    @Override
    public String lookupOperatorNameFromNetwork(long subId, String numeric, boolean desireLongName) {
        return null;
    }

    @Override
    public void conferenceDial(String[] participants, int clirMode, boolean isVideoCall, Message result) {
    }

    @Override
    public void registerForImsiRefreshDone(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mImsiRefreshDoneRegistrant.add(r);
    }

    @Override
    public void unregisterForImsiRefreshDone(Handler h) {
        this.mImsiRefreshDoneRegistrant.remove(h);
    }

    @Override
    public RadioCapability getBootupRadioCapability() {
        Rlog.d("RILJ", "getBootupRadioCapability: " + this.mRadioCapability);
        return this.mRadioCapability;
    }

    @Override
    public void setRegistrationSuspendEnabled(int enabled, Message response) {
    }

    @Override
    public void setResumeRegistration(int sessionId, Message response) {
    }

    @Override
    public void enableMd3Sleep(int enable) {
    }

    @Override
    public void registerForNetworkExsit(Handler h, int what, Object obj) {
        Rlog.d("RILJ", "registerForNetworkExsit h=" + h + " w=" + what);
        Registrant r = new Registrant(h, what, obj);
        this.mNetworkExistRegistrants.add(r);
    }

    @Override
    public void unregisterForNetworkExsit(Handler h) {
        Rlog.d("RILJ", "registerForNetworkExsit");
        this.mNetworkExistRegistrants.remove(h);
    }

    @Override
    public void registerForModulation(Handler h, int what, Object obj) {
        Rlog.d("RILJ", "registerForModulation h=" + h + " w=" + what);
        Registrant r = new Registrant(h, what, obj);
        this.mModulationRegistrants.add(r);
    }

    @Override
    public void unregisterForModulation(Handler h) {
        Rlog.d("RILJ", "unregisterForModulation");
        this.mModulationRegistrants.remove(h);
    }

    @Override
    public void registerForNetworkEvent(Handler h, int what, Object obj) {
        Rlog.d("RILJ", "registerForNetworkEvent h=" + h + " w=" + what);
        Registrant r = new Registrant(h, what, obj);
        this.mNetworkEventRegistrants.add(r);
    }

    @Override
    public void unregisterForNetworkEvent(Handler h) {
        Rlog.d("RILJ", "registerForNetworkEvent");
        this.mNetworkEventRegistrants.remove(h);
    }

    @Override
    public void registerForCallAccepted(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mAcceptedRegistrant.add(r);
    }

    @Override
    public void unregisterForCallAccepted(Handler h) {
        this.mAcceptedRegistrant.remove(h);
    }

    @Override
    public void setSimPower(int mode, Message result) {
    }

    @Override
    public void triggerModeSwitchByEcc(int mode, Message response) {
    }

    @Override
    public void setBandMode(int[] bandMode, Message response) {
    }

    @Override
    public void getCOLP(Message response) {
    }

    @Override
    public void setCOLP(boolean enable, Message response) {
    }

    @Override
    public void getCOLR(Message response) {
    }

    @Override
    public void iccGetATR(Message result) {
    }

    @Override
    public void iccOpenChannelWithSw(String AID, Message result) {
    }

    @Override
    public void storeModemType(int modemType, Message response) {
    }

    @Override
    public void reloadModemType(int modemType, Message response) {
    }

    @Override
    public void queryModemType(Message response) {
    }

    @Override
    public void syncApnTableToRds(String[] apnlist, Message response) {
    }

    @Override
    public void registerForPcoStatus(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mPcoStatusRegistrant.add(r);
    }

    @Override
    public void unregisterForPcoStatus(Handler h) {
        this.mPcoStatusRegistrant.remove(h);
    }

    @Override
    public void registerForGmssRatChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mGmssRatChangedRegistrant.add(r);
    }

    @Override
    public void enablePseudoBSMonitor(boolean reportOn, int reportRateInSeconds, Message response) {
    }

    @Override
    public void disablePseudoBSMonitor(Message response) {
    }

    @Override
    public void queryPseudoBSRecords(Message response) {
    }
}
