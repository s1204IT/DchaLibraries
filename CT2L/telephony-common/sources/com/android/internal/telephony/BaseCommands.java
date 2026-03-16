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
    protected Registrant mCatCallSetUpResultRegistrant;
    protected Registrant mCatCallSetUpStatusRegistrant;
    protected Registrant mCatCcAlphaRegistrant;
    protected Registrant mCatEventRegistrant;
    protected Registrant mCatProCmdRegistrant;
    protected Registrant mCatSendSmResultRegistrant;
    protected Registrant mCatSendSmStatusRegistrant;
    protected Registrant mCatSendUssdResultRegistrant;
    protected Registrant mCatSessionEndRegistrant;
    protected Registrant mCdmaSmsRegistrant;
    protected int mCdmaSubscription;
    protected Context mContext;
    protected Registrant mEmergencyCallbackModeRegistrant;
    protected Registrant mGsmBroadcastSmsRegistrant;
    protected Registrant mGsmSmsRegistrant;
    protected Registrant mIccSmsFullRegistrant;
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
    protected RegistrantList mSimHotPlugRegistrants = new RegistrantList();
    protected int mRilVersion = -1;
    protected int mSupportedRaf = 1;
    protected boolean mIccStatusChaned = false;

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
        if (this.mIccStatusChaned) {
            r.notifyRegistrant();
        }
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
        if (this.mGsmSmsRegistrant != null && this.mGsmSmsRegistrant.getHandler() == h) {
            this.mGsmSmsRegistrant.clear();
            this.mGsmSmsRegistrant = null;
        }
    }

    @Override
    public void setOnNewCdmaSms(Handler h, int what, Object obj) {
        this.mCdmaSmsRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnNewCdmaSms(Handler h) {
        if (this.mCdmaSmsRegistrant != null && this.mCdmaSmsRegistrant.getHandler() == h) {
            this.mCdmaSmsRegistrant.clear();
            this.mCdmaSmsRegistrant = null;
        }
    }

    @Override
    public void setOnNewGsmBroadcastSms(Handler h, int what, Object obj) {
        this.mGsmBroadcastSmsRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnNewGsmBroadcastSms(Handler h) {
        if (this.mGsmBroadcastSmsRegistrant != null && this.mGsmBroadcastSmsRegistrant.getHandler() == h) {
            this.mGsmBroadcastSmsRegistrant.clear();
            this.mGsmBroadcastSmsRegistrant = null;
        }
    }

    @Override
    public void setOnSmsOnSim(Handler h, int what, Object obj) {
        this.mSmsOnSimRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnSmsOnSim(Handler h) {
        if (this.mSmsOnSimRegistrant != null && this.mSmsOnSimRegistrant.getHandler() == h) {
            this.mSmsOnSimRegistrant.clear();
            this.mSmsOnSimRegistrant = null;
        }
    }

    @Override
    public void setOnSmsStatus(Handler h, int what, Object obj) {
        this.mSmsStatusRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnSmsStatus(Handler h) {
        if (this.mSmsStatusRegistrant != null && this.mSmsStatusRegistrant.getHandler() == h) {
            this.mSmsStatusRegistrant.clear();
            this.mSmsStatusRegistrant = null;
        }
    }

    @Override
    public void setOnSignalStrengthUpdate(Handler h, int what, Object obj) {
        this.mSignalStrengthRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnSignalStrengthUpdate(Handler h) {
        if (this.mSignalStrengthRegistrant != null && this.mSignalStrengthRegistrant.getHandler() == h) {
            this.mSignalStrengthRegistrant.clear();
            this.mSignalStrengthRegistrant = null;
        }
    }

    @Override
    public void setOnNITZTime(Handler h, int what, Object obj) {
        this.mNITZTimeRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnNITZTime(Handler h) {
        if (this.mNITZTimeRegistrant != null && this.mNITZTimeRegistrant.getHandler() == h) {
            this.mNITZTimeRegistrant.clear();
            this.mNITZTimeRegistrant = null;
        }
    }

    @Override
    public void setOnUSSD(Handler h, int what, Object obj) {
        this.mUSSDRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnUSSD(Handler h) {
        if (this.mUSSDRegistrant != null && this.mUSSDRegistrant.getHandler() == h) {
            this.mUSSDRegistrant.clear();
            this.mUSSDRegistrant = null;
        }
    }

    @Override
    public void setOnSuppServiceNotification(Handler h, int what, Object obj) {
        this.mSsnRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnSuppServiceNotification(Handler h) {
        if (this.mSsnRegistrant != null && this.mSsnRegistrant.getHandler() == h) {
            this.mSsnRegistrant.clear();
            this.mSsnRegistrant = null;
        }
    }

    @Override
    public void setOnCatSessionEnd(Handler h, int what, Object obj) {
        this.mCatSessionEndRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnCatSessionEnd(Handler h) {
        if (this.mCatSessionEndRegistrant != null && this.mCatSessionEndRegistrant.getHandler() == h) {
            this.mCatSessionEndRegistrant.clear();
            this.mCatSessionEndRegistrant = null;
        }
    }

    @Override
    public void setOnCatProactiveCmd(Handler h, int what, Object obj) {
        this.mCatProCmdRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnCatProactiveCmd(Handler h) {
        if (this.mCatProCmdRegistrant != null && this.mCatProCmdRegistrant.getHandler() == h) {
            this.mCatProCmdRegistrant.clear();
            this.mCatProCmdRegistrant = null;
        }
    }

    @Override
    public void setOnCatEvent(Handler h, int what, Object obj) {
        this.mCatEventRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnCatEvent(Handler h) {
        if (this.mCatEventRegistrant != null && this.mCatEventRegistrant.getHandler() == h) {
            this.mCatEventRegistrant.clear();
            this.mCatEventRegistrant = null;
        }
    }

    @Override
    public void setOnCatCallSetUp(Handler h, int what, Object obj) {
        this.mCatCallSetUpRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnCatCallSetUp(Handler h) {
        if (this.mCatCallSetUpRegistrant != null && this.mCatCallSetUpRegistrant.getHandler() == h) {
            this.mCatCallSetUpRegistrant.clear();
            this.mCatCallSetUpRegistrant = null;
        }
    }

    @Override
    public void setOnCatCallSetUpStatus(Handler h, int what, Object obj) {
        this.mCatCallSetUpStatusRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnCatCallSetUpStatus(Handler h) {
        this.mCatCallSetUpStatusRegistrant.clear();
    }

    @Override
    public void setOnCatCallSetUpResult(Handler h, int what, Object obj) {
        this.mCatCallSetUpResultRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnCatCallSetUpResult(Handler h) {
        this.mCatCallSetUpResultRegistrant.clear();
    }

    @Override
    public void setOnCatSendSmStatus(Handler h, int what, Object obj) {
        this.mCatSendSmStatusRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnCatSendSmStatus(Handler h) {
        this.mCatSendSmStatusRegistrant.clear();
    }

    @Override
    public void setOnCatSendSmResult(Handler h, int what, Object obj) {
        this.mCatSendSmResultRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnCatSendSmResult(Handler h) {
        this.mCatSendSmResultRegistrant.clear();
    }

    @Override
    public void setOnCatSendUssdResult(Handler h, int what, Object obj) {
        this.mCatSendUssdResultRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnCatSendUssdResult(Handler h) {
        this.mCatSendUssdResultRegistrant.clear();
    }

    @Override
    public void setOnIccSmsFull(Handler h, int what, Object obj) {
        this.mIccSmsFullRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unSetOnIccSmsFull(Handler h) {
        if (this.mIccSmsFullRegistrant != null && this.mIccSmsFullRegistrant.getHandler() == h) {
            this.mIccSmsFullRegistrant.clear();
            this.mIccSmsFullRegistrant = null;
        }
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
        if (this.mRingRegistrant != null && this.mRingRegistrant.getHandler() == h) {
            this.mRingRegistrant.clear();
            this.mRingRegistrant = null;
        }
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
        if (this.mRestrictedStateRegistrant != null && this.mRestrictedStateRegistrant.getHandler() != h) {
            this.mRestrictedStateRegistrant.clear();
            this.mRestrictedStateRegistrant = null;
        }
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
        if (this.mUnsolOemHookRawRegistrant != null && this.mUnsolOemHookRawRegistrant.getHandler() == h) {
            this.mUnsolOemHookRawRegistrant.clear();
            this.mUnsolOemHookRawRegistrant = null;
        }
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
        if (this.mRilVersion != -1) {
            r.notifyRegistrant(new AsyncResult((Object) null, new Integer(this.mRilVersion), (Throwable) null));
        }
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
            if (oldState != this.mState) {
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
    public void registerForSimHotPlug(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mSimHotPlugRegistrants.add(r);
    }

    @Override
    public void unregisterForSimHotPlug(Handler h) {
        this.mSimHotPlugRegistrants.remove(h);
    }

    @Override
    public void testingEmergencyCall() {
    }

    @Override
    public int getRilVersion() {
        return this.mRilVersion;
    }

    @Override
    public int getSupportedRadioAccessFamily() {
        return this.mSupportedRaf;
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
    public void getSIMLockInfo(Message result) {
    }
}
