package com.android.internal.telephony;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CommandsInterface;

public abstract class BaseCommands implements CommandsInterface {
    protected Registrant mCatCallSetUpRegistrant;
    protected Registrant mCatCcAlphaRegistrant;
    protected Registrant mCatEventRegistrant;
    protected Registrant mCatProCmdRegistrant;
    protected Registrant mCatSessionEndRegistrant;
    protected Registrant mCdmaSmsRegistrant;
    public int mCdmaSubscription;
    protected Context mContext;
    public Registrant mEmergencyCallbackModeRegistrant;
    protected Registrant mGsmBroadcastSmsRegistrant;
    protected Registrant mGsmSmsRegistrant;
    public Registrant mIccSmsFullRegistrant;
    protected Registrant mNITZTimeRegistrant;
    protected int mPhoneType;
    protected int mPreferredNetworkType;
    protected Registrant mRestrictedStateRegistrant;
    protected Registrant mRingRegistrant;
    protected Registrant mSignalStrengthRegistrant;
    protected Registrant mSmsOnSimRegistrant;
    protected Registrant mSmsStatusRegistrant;
    protected Registrant mSsRegistrant;
    protected Registrant mSsnRegistrant;
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
    protected RegistrantList mNetworkStateRegistrants = new RegistrantList();
    protected RegistrantList mDataCallListChangedRegistrants = new RegistrantList();
    protected RegistrantList mVoiceRadioTechChangedRegistrants = new RegistrantList();
    protected RegistrantList mImsNetworkStateChangedRegistrants = new RegistrantList();
    protected RegistrantList mIccStatusChangedRegistrants = new RegistrantList();
    protected RegistrantList mIccSlotStatusChangedRegistrants = new RegistrantList();
    public RegistrantList mVoicePrivacyOnRegistrants = new RegistrantList();
    public RegistrantList mVoicePrivacyOffRegistrants = new RegistrantList();
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
    protected RegistrantList mPcoDataRegistrants = new RegistrantList();
    protected RegistrantList mCarrierInfoForImsiEncryptionRegistrants = new RegistrantList();
    protected RegistrantList mRilNetworkScanResultRegistrants = new RegistrantList();
    protected RegistrantList mModemResetRegistrants = new RegistrantList();
    protected RegistrantList mNattKeepaliveStatusRegistrants = new RegistrantList();
    protected RegistrantList mPhysicalChannelConfigurationRegistrants = new RegistrantList();
    protected RegistrantList mLceInfoRegistrants = new RegistrantList();
    protected int mRilVersion = -1;
    protected int mNewVoiceTech = -1;

    public BaseCommands(Context context) {
        this.mContext = context;
    }

    @Override
    public CommandsInterface.RadioState getRadioState() {
        return this.mState;
    }

    @Override
    public void registerForRadioStateChanged(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        synchronized (this.mStateMonitor) {
            this.mRadioStateChangedRegistrants.add(registrant);
            registrant.notifyRegistrant();
        }
    }

    @Override
    public void unregisterForRadioStateChanged(Handler handler) {
        synchronized (this.mStateMonitor) {
            this.mRadioStateChangedRegistrants.remove(handler);
        }
    }

    @Override
    public void registerForImsNetworkStateChanged(Handler handler, int i, Object obj) {
        this.mImsNetworkStateChangedRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForImsNetworkStateChanged(Handler handler) {
        this.mImsNetworkStateChangedRegistrants.remove(handler);
    }

    @Override
    public void registerForOn(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        synchronized (this.mStateMonitor) {
            this.mOnRegistrants.add(registrant);
            if (this.mState.isOn()) {
                registrant.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
            }
        }
    }

    @Override
    public void unregisterForOn(Handler handler) {
        synchronized (this.mStateMonitor) {
            this.mOnRegistrants.remove(handler);
        }
    }

    @Override
    public void registerForAvailable(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        synchronized (this.mStateMonitor) {
            this.mAvailRegistrants.add(registrant);
            if (this.mState.isAvailable()) {
                registrant.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
            }
        }
    }

    @Override
    public void unregisterForAvailable(Handler handler) {
        synchronized (this.mStateMonitor) {
            this.mAvailRegistrants.remove(handler);
        }
    }

    @Override
    public void registerForNotAvailable(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        synchronized (this.mStateMonitor) {
            this.mNotAvailRegistrants.add(registrant);
            if (!this.mState.isAvailable()) {
                registrant.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
            }
        }
    }

    @Override
    public void unregisterForNotAvailable(Handler handler) {
        synchronized (this.mStateMonitor) {
            this.mNotAvailRegistrants.remove(handler);
        }
    }

    @Override
    public void registerForOffOrNotAvailable(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        synchronized (this.mStateMonitor) {
            this.mOffOrNotAvailRegistrants.add(registrant);
            if (this.mState == CommandsInterface.RadioState.RADIO_OFF || !this.mState.isAvailable()) {
                registrant.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
            }
        }
    }

    @Override
    public void unregisterForOffOrNotAvailable(Handler handler) {
        synchronized (this.mStateMonitor) {
            this.mOffOrNotAvailRegistrants.remove(handler);
        }
    }

    @Override
    public void registerForCallStateChanged(Handler handler, int i, Object obj) {
        this.mCallStateRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForCallStateChanged(Handler handler) {
        this.mCallStateRegistrants.remove(handler);
    }

    @Override
    public void registerForNetworkStateChanged(Handler handler, int i, Object obj) {
        this.mNetworkStateRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForNetworkStateChanged(Handler handler) {
        this.mNetworkStateRegistrants.remove(handler);
    }

    @Override
    public void registerForDataCallListChanged(Handler handler, int i, Object obj) {
        this.mDataCallListChangedRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForDataCallListChanged(Handler handler) {
        this.mDataCallListChangedRegistrants.remove(handler);
    }

    @Override
    public void registerForVoiceRadioTechChanged(Handler handler, int i, Object obj) {
        this.mVoiceRadioTechChangedRegistrants.add(new Registrant(handler, i, obj));
        if (this.mNewVoiceTech != -1) {
            this.mVoiceRadioTechChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, new int[]{this.mNewVoiceTech}, (Throwable) null));
        }
    }

    @Override
    public void unregisterForVoiceRadioTechChanged(Handler handler) {
        this.mVoiceRadioTechChangedRegistrants.remove(handler);
    }

    @Override
    public void registerForIccStatusChanged(Handler handler, int i, Object obj) {
        this.mIccStatusChangedRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForIccStatusChanged(Handler handler) {
        this.mIccStatusChangedRegistrants.remove(handler);
    }

    @Override
    public void registerForIccSlotStatusChanged(Handler handler, int i, Object obj) {
        this.mIccSlotStatusChangedRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForIccSlotStatusChanged(Handler handler) {
        this.mIccSlotStatusChangedRegistrants.remove(handler);
    }

    @Override
    public void setOnNewGsmSms(Handler handler, int i, Object obj) {
        this.mGsmSmsRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unSetOnNewGsmSms(Handler handler) {
        if (this.mGsmSmsRegistrant != null && this.mGsmSmsRegistrant.getHandler() == handler) {
            this.mGsmSmsRegistrant.clear();
            this.mGsmSmsRegistrant = null;
        }
    }

    @Override
    public void setOnNewCdmaSms(Handler handler, int i, Object obj) {
        this.mCdmaSmsRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unSetOnNewCdmaSms(Handler handler) {
        if (this.mCdmaSmsRegistrant != null && this.mCdmaSmsRegistrant.getHandler() == handler) {
            this.mCdmaSmsRegistrant.clear();
            this.mCdmaSmsRegistrant = null;
        }
    }

    @Override
    public void setOnNewGsmBroadcastSms(Handler handler, int i, Object obj) {
        this.mGsmBroadcastSmsRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unSetOnNewGsmBroadcastSms(Handler handler) {
        if (this.mGsmBroadcastSmsRegistrant != null && this.mGsmBroadcastSmsRegistrant.getHandler() == handler) {
            this.mGsmBroadcastSmsRegistrant.clear();
            this.mGsmBroadcastSmsRegistrant = null;
        }
    }

    @Override
    public void setOnSmsOnSim(Handler handler, int i, Object obj) {
        this.mSmsOnSimRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unSetOnSmsOnSim(Handler handler) {
        if (this.mSmsOnSimRegistrant != null && this.mSmsOnSimRegistrant.getHandler() == handler) {
            this.mSmsOnSimRegistrant.clear();
            this.mSmsOnSimRegistrant = null;
        }
    }

    @Override
    public void setOnSmsStatus(Handler handler, int i, Object obj) {
        this.mSmsStatusRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unSetOnSmsStatus(Handler handler) {
        if (this.mSmsStatusRegistrant != null && this.mSmsStatusRegistrant.getHandler() == handler) {
            this.mSmsStatusRegistrant.clear();
            this.mSmsStatusRegistrant = null;
        }
    }

    @Override
    public void setOnSignalStrengthUpdate(Handler handler, int i, Object obj) {
        this.mSignalStrengthRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unSetOnSignalStrengthUpdate(Handler handler) {
        if (this.mSignalStrengthRegistrant != null && this.mSignalStrengthRegistrant.getHandler() == handler) {
            this.mSignalStrengthRegistrant.clear();
            this.mSignalStrengthRegistrant = null;
        }
    }

    @Override
    public void setOnNITZTime(Handler handler, int i, Object obj) {
        this.mNITZTimeRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unSetOnNITZTime(Handler handler) {
        if (this.mNITZTimeRegistrant != null && this.mNITZTimeRegistrant.getHandler() == handler) {
            this.mNITZTimeRegistrant.clear();
            this.mNITZTimeRegistrant = null;
        }
    }

    @Override
    public void setOnUSSD(Handler handler, int i, Object obj) {
        this.mUSSDRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unSetOnUSSD(Handler handler) {
        if (this.mUSSDRegistrant != null && this.mUSSDRegistrant.getHandler() == handler) {
            this.mUSSDRegistrant.clear();
            this.mUSSDRegistrant = null;
        }
    }

    @Override
    public void setOnSuppServiceNotification(Handler handler, int i, Object obj) {
        this.mSsnRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unSetOnSuppServiceNotification(Handler handler) {
        if (this.mSsnRegistrant != null && this.mSsnRegistrant.getHandler() == handler) {
            this.mSsnRegistrant.clear();
            this.mSsnRegistrant = null;
        }
    }

    @Override
    public void setOnCatSessionEnd(Handler handler, int i, Object obj) {
        this.mCatSessionEndRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unSetOnCatSessionEnd(Handler handler) {
        if (this.mCatSessionEndRegistrant != null && this.mCatSessionEndRegistrant.getHandler() == handler) {
            this.mCatSessionEndRegistrant.clear();
            this.mCatSessionEndRegistrant = null;
        }
    }

    @Override
    public void setOnCatProactiveCmd(Handler handler, int i, Object obj) {
        this.mCatProCmdRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unSetOnCatProactiveCmd(Handler handler) {
        if (this.mCatProCmdRegistrant != null && this.mCatProCmdRegistrant.getHandler() == handler) {
            this.mCatProCmdRegistrant.clear();
            this.mCatProCmdRegistrant = null;
        }
    }

    @Override
    public void setOnCatEvent(Handler handler, int i, Object obj) {
        this.mCatEventRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unSetOnCatEvent(Handler handler) {
        if (this.mCatEventRegistrant != null && this.mCatEventRegistrant.getHandler() == handler) {
            this.mCatEventRegistrant.clear();
            this.mCatEventRegistrant = null;
        }
    }

    @Override
    public void setOnCatCallSetUp(Handler handler, int i, Object obj) {
        this.mCatCallSetUpRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unSetOnCatCallSetUp(Handler handler) {
        if (this.mCatCallSetUpRegistrant != null && this.mCatCallSetUpRegistrant.getHandler() == handler) {
            this.mCatCallSetUpRegistrant.clear();
            this.mCatCallSetUpRegistrant = null;
        }
    }

    @Override
    public void setOnIccSmsFull(Handler handler, int i, Object obj) {
        this.mIccSmsFullRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unSetOnIccSmsFull(Handler handler) {
        if (this.mIccSmsFullRegistrant != null && this.mIccSmsFullRegistrant.getHandler() == handler) {
            this.mIccSmsFullRegistrant.clear();
            this.mIccSmsFullRegistrant = null;
        }
    }

    @Override
    public void registerForIccRefresh(Handler handler, int i, Object obj) {
        this.mIccRefreshRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void setOnIccRefresh(Handler handler, int i, Object obj) {
        registerForIccRefresh(handler, i, obj);
    }

    @Override
    public void setEmergencyCallbackMode(Handler handler, int i, Object obj) {
        this.mEmergencyCallbackModeRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unregisterForIccRefresh(Handler handler) {
        this.mIccRefreshRegistrants.remove(handler);
    }

    @Override
    public void unsetOnIccRefresh(Handler handler) {
        unregisterForIccRefresh(handler);
    }

    @Override
    public void setOnCallRing(Handler handler, int i, Object obj) {
        this.mRingRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unSetOnCallRing(Handler handler) {
        if (this.mRingRegistrant != null && this.mRingRegistrant.getHandler() == handler) {
            this.mRingRegistrant.clear();
            this.mRingRegistrant = null;
        }
    }

    @Override
    public void setOnSs(Handler handler, int i, Object obj) {
        this.mSsRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unSetOnSs(Handler handler) {
        this.mSsRegistrant.clear();
    }

    @Override
    public void setOnCatCcAlphaNotify(Handler handler, int i, Object obj) {
        this.mCatCcAlphaRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unSetOnCatCcAlphaNotify(Handler handler) {
        this.mCatCcAlphaRegistrant.clear();
    }

    @Override
    public void registerForInCallVoicePrivacyOn(Handler handler, int i, Object obj) {
        this.mVoicePrivacyOnRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForInCallVoicePrivacyOn(Handler handler) {
        this.mVoicePrivacyOnRegistrants.remove(handler);
    }

    @Override
    public void registerForInCallVoicePrivacyOff(Handler handler, int i, Object obj) {
        this.mVoicePrivacyOffRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForInCallVoicePrivacyOff(Handler handler) {
        this.mVoicePrivacyOffRegistrants.remove(handler);
    }

    @Override
    public void setOnRestrictedStateChanged(Handler handler, int i, Object obj) {
        this.mRestrictedStateRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unSetOnRestrictedStateChanged(Handler handler) {
        if (this.mRestrictedStateRegistrant != null && this.mRestrictedStateRegistrant.getHandler() == handler) {
            this.mRestrictedStateRegistrant.clear();
            this.mRestrictedStateRegistrant = null;
        }
    }

    @Override
    public void registerForDisplayInfo(Handler handler, int i, Object obj) {
        this.mDisplayInfoRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForDisplayInfo(Handler handler) {
        this.mDisplayInfoRegistrants.remove(handler);
    }

    @Override
    public void registerForCallWaitingInfo(Handler handler, int i, Object obj) {
        this.mCallWaitingInfoRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForCallWaitingInfo(Handler handler) {
        this.mCallWaitingInfoRegistrants.remove(handler);
    }

    @Override
    public void registerForSignalInfo(Handler handler, int i, Object obj) {
        this.mSignalInfoRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void setOnUnsolOemHookRaw(Handler handler, int i, Object obj) {
        this.mUnsolOemHookRawRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unSetOnUnsolOemHookRaw(Handler handler) {
        if (this.mUnsolOemHookRawRegistrant != null && this.mUnsolOemHookRawRegistrant.getHandler() == handler) {
            this.mUnsolOemHookRawRegistrant.clear();
            this.mUnsolOemHookRawRegistrant = null;
        }
    }

    @Override
    public void unregisterForSignalInfo(Handler handler) {
        this.mSignalInfoRegistrants.remove(handler);
    }

    @Override
    public void registerForCdmaOtaProvision(Handler handler, int i, Object obj) {
        this.mOtaProvisionRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForCdmaOtaProvision(Handler handler) {
        this.mOtaProvisionRegistrants.remove(handler);
    }

    @Override
    public void registerForNumberInfo(Handler handler, int i, Object obj) {
        this.mNumberInfoRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForNumberInfo(Handler handler) {
        this.mNumberInfoRegistrants.remove(handler);
    }

    @Override
    public void registerForRedirectedNumberInfo(Handler handler, int i, Object obj) {
        this.mRedirNumInfoRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForRedirectedNumberInfo(Handler handler) {
        this.mRedirNumInfoRegistrants.remove(handler);
    }

    @Override
    public void registerForLineControlInfo(Handler handler, int i, Object obj) {
        this.mLineControlInfoRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForLineControlInfo(Handler handler) {
        this.mLineControlInfoRegistrants.remove(handler);
    }

    @Override
    public void registerFoT53ClirlInfo(Handler handler, int i, Object obj) {
        this.mT53ClirInfoRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForT53ClirInfo(Handler handler) {
        this.mT53ClirInfoRegistrants.remove(handler);
    }

    @Override
    public void registerForT53AudioControlInfo(Handler handler, int i, Object obj) {
        this.mT53AudCntrlInfoRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForT53AudioControlInfo(Handler handler) {
        this.mT53AudCntrlInfoRegistrants.remove(handler);
    }

    @Override
    public void registerForRingbackTone(Handler handler, int i, Object obj) {
        this.mRingbackToneRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForRingbackTone(Handler handler) {
        this.mRingbackToneRegistrants.remove(handler);
    }

    @Override
    public void registerForResendIncallMute(Handler handler, int i, Object obj) {
        this.mResendIncallMuteRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForResendIncallMute(Handler handler) {
        this.mResendIncallMuteRegistrants.remove(handler);
    }

    @Override
    public void registerForCdmaSubscriptionChanged(Handler handler, int i, Object obj) {
        this.mCdmaSubscriptionChangedRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForCdmaSubscriptionChanged(Handler handler) {
        this.mCdmaSubscriptionChangedRegistrants.remove(handler);
    }

    @Override
    public void registerForCdmaPrlChanged(Handler handler, int i, Object obj) {
        this.mCdmaPrlChangedRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForCdmaPrlChanged(Handler handler) {
        this.mCdmaPrlChangedRegistrants.remove(handler);
    }

    @Override
    public void registerForExitEmergencyCallbackMode(Handler handler, int i, Object obj) {
        this.mExitEmergencyCallbackModeRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForExitEmergencyCallbackMode(Handler handler) {
        this.mExitEmergencyCallbackModeRegistrants.remove(handler);
    }

    @Override
    public void registerForHardwareConfigChanged(Handler handler, int i, Object obj) {
        this.mHardwareConfigChangeRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForHardwareConfigChanged(Handler handler) {
        this.mHardwareConfigChangeRegistrants.remove(handler);
    }

    @Override
    public void registerForNetworkScanResult(Handler handler, int i, Object obj) {
        this.mRilNetworkScanResultRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForNetworkScanResult(Handler handler) {
        this.mRilNetworkScanResultRegistrants.remove(handler);
    }

    @Override
    public void registerForRilConnected(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        this.mRilConnectedRegistrants.add(registrant);
        if (this.mRilVersion != -1) {
            registrant.notifyRegistrant(new AsyncResult((Object) null, new Integer(this.mRilVersion), (Throwable) null));
        }
    }

    @Override
    public void unregisterForRilConnected(Handler handler) {
        this.mRilConnectedRegistrants.remove(handler);
    }

    @Override
    public void registerForSubscriptionStatusChanged(Handler handler, int i, Object obj) {
        this.mSubscriptionStatusRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForSubscriptionStatusChanged(Handler handler) {
        this.mSubscriptionStatusRegistrants.remove(handler);
    }

    protected void setRadioState(CommandsInterface.RadioState radioState) {
        synchronized (this.mStateMonitor) {
            CommandsInterface.RadioState radioState2 = this.mState;
            this.mState = radioState;
            if (radioState2 == this.mState) {
                return;
            }
            this.mRadioStateChangedRegistrants.notifyRegistrants();
            if (this.mState.isAvailable() && !radioState2.isAvailable()) {
                this.mAvailRegistrants.notifyRegistrants();
            }
            if (!this.mState.isAvailable() && radioState2.isAvailable()) {
                this.mNotAvailRegistrants.notifyRegistrants();
            }
            if (this.mState.isOn() && !radioState2.isOn()) {
                this.mOnRegistrants.notifyRegistrants();
            }
            if ((!this.mState.isOn() || !this.mState.isAvailable()) && radioState2.isOn() && radioState2.isAvailable()) {
                this.mOffOrNotAvailRegistrants.notifyRegistrants();
            }
        }
    }

    @Override
    public int getLteOnCdmaMode() {
        return TelephonyManager.getLteOnCdmaModeStatic();
    }

    @Override
    public void registerForCellInfoList(Handler handler, int i, Object obj) {
        this.mRilCellInfoListRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForCellInfoList(Handler handler) {
        this.mRilCellInfoListRegistrants.remove(handler);
    }

    @Override
    public void registerForPhysicalChannelConfiguration(Handler handler, int i, Object obj) {
        this.mPhysicalChannelConfigurationRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForPhysicalChannelConfiguration(Handler handler) {
        this.mPhysicalChannelConfigurationRegistrants.remove(handler);
    }

    @Override
    public void registerForSrvccStateChanged(Handler handler, int i, Object obj) {
        this.mSrvccStateRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForSrvccStateChanged(Handler handler) {
        this.mSrvccStateRegistrants.remove(handler);
    }

    @Override
    public void testingEmergencyCall() {
    }

    @Override
    public int getRilVersion() {
        return this.mRilVersion;
    }

    @Override
    public void setUiccSubscription(int i, int i2, int i3, int i4, Message message) {
    }

    @Override
    public void setDataAllowed(boolean z, Message message) {
    }

    @Override
    public void requestShutdown(Message message) {
    }

    @Override
    public void getRadioCapability(Message message) {
    }

    @Override
    public void setRadioCapability(RadioCapability radioCapability, Message message) {
    }

    @Override
    public void registerForRadioCapabilityChanged(Handler handler, int i, Object obj) {
        this.mPhoneRadioCapabilityChangedRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForRadioCapabilityChanged(Handler handler) {
        this.mPhoneRadioCapabilityChangedRegistrants.remove(handler);
    }

    @Override
    public void startLceService(int i, boolean z, Message message) {
    }

    @Override
    public void stopLceService(Message message) {
    }

    @Override
    public void pullLceData(Message message) {
    }

    @Override
    public void registerForLceInfo(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        synchronized (this.mStateMonitor) {
            this.mLceInfoRegistrants.add(registrant);
        }
    }

    @Override
    public void unregisterForLceInfo(Handler handler) {
        synchronized (this.mStateMonitor) {
            this.mLceInfoRegistrants.remove(handler);
        }
    }

    @Override
    public void registerForModemReset(Handler handler, int i, Object obj) {
        this.mModemResetRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForModemReset(Handler handler) {
        this.mModemResetRegistrants.remove(handler);
    }

    @Override
    public void registerForPcoData(Handler handler, int i, Object obj) {
        this.mPcoDataRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForPcoData(Handler handler) {
        this.mPcoDataRegistrants.remove(handler);
    }

    @Override
    public void registerForCarrierInfoForImsiEncryption(Handler handler, int i, Object obj) {
        this.mCarrierInfoForImsiEncryptionRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForCarrierInfoForImsiEncryption(Handler handler) {
        this.mCarrierInfoForImsiEncryptionRegistrants.remove(handler);
    }

    @Override
    public void registerForNattKeepaliveStatus(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        synchronized (this.mStateMonitor) {
            this.mNattKeepaliveStatusRegistrants.add(registrant);
        }
    }

    @Override
    public void unregisterForNattKeepaliveStatus(Handler handler) {
        synchronized (this.mStateMonitor) {
            this.mNattKeepaliveStatusRegistrants.remove(handler);
        }
    }
}
