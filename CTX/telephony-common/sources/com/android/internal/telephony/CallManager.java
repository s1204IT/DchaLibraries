package com.android.internal.telephony;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.sip.SipPhone;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class CallManager {
    private static final boolean DBG = true;
    protected static final int EVENT_CALL_WAITING = 108;
    protected static final int EVENT_CDMA_OTA_STATUS_CHANGE = 111;
    protected static final int EVENT_DISCONNECT = 100;
    protected static final int EVENT_DISPLAY_INFO = 109;
    protected static final int EVENT_ECM_TIMER_RESET = 115;
    protected static final int EVENT_INCOMING_RING = 104;
    protected static final int EVENT_IN_CALL_VOICE_PRIVACY_OFF = 107;
    protected static final int EVENT_IN_CALL_VOICE_PRIVACY_ON = 106;
    protected static final int EVENT_MMI_COMPLETE = 114;
    protected static final int EVENT_MMI_INITIATE = 113;
    protected static final int EVENT_NEW_RINGING_CONNECTION = 102;
    protected static final int EVENT_ONHOLD_TONE = 120;
    protected static final int EVENT_POST_DIAL_CHARACTER = 119;
    protected static final int EVENT_PRECISE_CALL_STATE_CHANGED = 101;
    protected static final int EVENT_RESEND_INCALL_MUTE = 112;
    protected static final int EVENT_RINGBACK_TONE = 105;
    protected static final int EVENT_SERVICE_STATE_CHANGED = 118;
    protected static final int EVENT_SIGNAL_INFO = 110;
    protected static final int EVENT_SUBSCRIPTION_INFO_READY = 116;
    protected static final int EVENT_SUPP_SERVICE_FAILED = 117;
    protected static final int EVENT_TTY_MODE_RECEIVED = 122;
    protected static final int EVENT_UNKNOWN_CONNECTION = 103;
    private static CallManager INSTANCE = null;
    protected static final String LOG_TAG = "CallManager";
    protected static final boolean VDBG = false;
    private final ArrayList<Connection> mEmptyConnections = new ArrayList<>();
    protected final HashMap<Phone, CallManagerHandler> mHandlerMap = new HashMap<>();
    private boolean mSpeedUpAudioForMtCall = false;
    protected Object mRegistrantidentifier = new Object();
    protected final RegistrantList mPreciseCallStateRegistrants = new RegistrantList();
    protected final RegistrantList mNewRingingConnectionRegistrants = new RegistrantList();
    protected final RegistrantList mIncomingRingRegistrants = new RegistrantList();
    protected final RegistrantList mDisconnectRegistrants = new RegistrantList();
    protected final RegistrantList mMmiRegistrants = new RegistrantList();
    protected final RegistrantList mUnknownConnectionRegistrants = new RegistrantList();
    protected final RegistrantList mRingbackToneRegistrants = new RegistrantList();
    protected final RegistrantList mOnHoldToneRegistrants = new RegistrantList();
    protected final RegistrantList mInCallVoicePrivacyOnRegistrants = new RegistrantList();
    protected final RegistrantList mInCallVoicePrivacyOffRegistrants = new RegistrantList();
    protected final RegistrantList mCallWaitingRegistrants = new RegistrantList();
    protected final RegistrantList mDisplayInfoRegistrants = new RegistrantList();
    protected final RegistrantList mSignalInfoRegistrants = new RegistrantList();
    protected final RegistrantList mCdmaOtaStatusChangeRegistrants = new RegistrantList();
    protected final RegistrantList mResendIncallMuteRegistrants = new RegistrantList();
    protected final RegistrantList mMmiInitiateRegistrants = new RegistrantList();
    protected final RegistrantList mMmiCompleteRegistrants = new RegistrantList();
    protected final RegistrantList mEcmTimerResetRegistrants = new RegistrantList();
    protected final RegistrantList mSubscriptionInfoReadyRegistrants = new RegistrantList();
    protected final RegistrantList mSuppServiceFailedRegistrants = new RegistrantList();
    protected final RegistrantList mServiceStateChangedRegistrants = new RegistrantList();
    protected final RegistrantList mPostDialCharacterRegistrants = new RegistrantList();
    protected final RegistrantList mTtyModeReceivedRegistrants = new RegistrantList();
    private final ArrayList<Phone> mPhones = new ArrayList<>();
    private final ArrayList<Call> mRingingCalls = new ArrayList<>();
    private final ArrayList<Call> mBackgroundCalls = new ArrayList<>();
    private final ArrayList<Call> mForegroundCalls = new ArrayList<>();
    private Phone mDefaultPhone = null;

    protected CallManager() {
    }

    public static CallManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = TelephonyComponentFactory.getInstance().makeCallManager();
        }
        return INSTANCE;
    }

    public List<Phone> getAllPhones() {
        return Collections.unmodifiableList(this.mPhones);
    }

    private Phone getPhone(int i) {
        for (Phone phone : this.mPhones) {
            if (phone.getSubId() == i && phone.getPhoneType() != 5) {
                return phone;
            }
        }
        return null;
    }

    public PhoneConstants.State getState() {
        PhoneConstants.State state = PhoneConstants.State.IDLE;
        for (Phone phone : this.mPhones) {
            if (phone.getState() == PhoneConstants.State.RINGING) {
                state = PhoneConstants.State.RINGING;
            } else if (phone.getState() == PhoneConstants.State.OFFHOOK && state == PhoneConstants.State.IDLE) {
                state = PhoneConstants.State.OFFHOOK;
            }
        }
        return state;
    }

    public PhoneConstants.State getState(int i) {
        PhoneConstants.State state = PhoneConstants.State.IDLE;
        for (Phone phone : this.mPhones) {
            if (phone.getSubId() == i) {
                if (phone.getState() == PhoneConstants.State.RINGING) {
                    state = PhoneConstants.State.RINGING;
                } else if (phone.getState() == PhoneConstants.State.OFFHOOK && state == PhoneConstants.State.IDLE) {
                    state = PhoneConstants.State.OFFHOOK;
                }
            }
        }
        return state;
    }

    public int getServiceState() {
        Iterator<Phone> it = this.mPhones.iterator();
        while (it.hasNext()) {
            int state = it.next().getServiceState().getState();
            if (state == 0) {
                return state;
            }
            if (state != 1 && state == 2) {
            }
        }
        return 1;
    }

    public int getServiceState(int i) {
        for (Phone phone : this.mPhones) {
            if (phone.getSubId() == i) {
                int state = phone.getServiceState().getState();
                if (state == 0) {
                    return state;
                }
                if (state != 1 && state == 2) {
                }
            }
        }
        return 1;
    }

    public Phone getPhoneInCall() {
        if (!getFirstActiveRingingCall().isIdle()) {
            return getFirstActiveRingingCall().getPhone();
        }
        if (!getActiveFgCall().isIdle()) {
            return getActiveFgCall().getPhone();
        }
        return getFirstActiveBgCall().getPhone();
    }

    public Phone getPhoneInCall(int i) {
        if (!getFirstActiveRingingCall(i).isIdle()) {
            return getFirstActiveRingingCall(i).getPhone();
        }
        if (!getActiveFgCall(i).isIdle()) {
            return getActiveFgCall(i).getPhone();
        }
        return getFirstActiveBgCall(i).getPhone();
    }

    public boolean registerPhone(Phone phone) {
        if (phone != null && !this.mPhones.contains(phone)) {
            Rlog.d(LOG_TAG, "registerPhone(" + phone.getPhoneName() + " " + phone + ")");
            if (this.mPhones.isEmpty()) {
                this.mDefaultPhone = phone;
            }
            this.mPhones.add(phone);
            this.mRingingCalls.add(phone.getRingingCall());
            this.mBackgroundCalls.add(phone.getBackgroundCall());
            this.mForegroundCalls.add(phone.getForegroundCall());
            registerForPhoneStates(phone);
            return true;
        }
        return false;
    }

    public void unregisterPhone(Phone phone) {
        if (phone != null && this.mPhones.contains(phone)) {
            Rlog.d(LOG_TAG, "unregisterPhone(" + phone.getPhoneName() + " " + phone + ")");
            Phone imsPhone = phone.getImsPhone();
            if (imsPhone != null) {
                unregisterPhone(imsPhone);
            }
            this.mPhones.remove(phone);
            this.mRingingCalls.remove(phone.getRingingCall());
            this.mBackgroundCalls.remove(phone.getBackgroundCall());
            this.mForegroundCalls.remove(phone.getForegroundCall());
            unregisterForPhoneStates(phone);
            if (phone == this.mDefaultPhone) {
                if (this.mPhones.isEmpty()) {
                    this.mDefaultPhone = null;
                } else {
                    this.mDefaultPhone = this.mPhones.get(0);
                }
            }
        }
    }

    public Phone getDefaultPhone() {
        return this.mDefaultPhone;
    }

    public Phone getFgPhone() {
        return getActiveFgCall().getPhone();
    }

    public Phone getFgPhone(int i) {
        return getActiveFgCall(i).getPhone();
    }

    public Phone getBgPhone() {
        return getFirstActiveBgCall().getPhone();
    }

    public Phone getBgPhone(int i) {
        return getFirstActiveBgCall(i).getPhone();
    }

    public Phone getRingingPhone() {
        return getFirstActiveRingingCall().getPhone();
    }

    public Phone getRingingPhone(int i) {
        return getFirstActiveRingingCall(i).getPhone();
    }

    private Context getContext() {
        Phone defaultPhone = getDefaultPhone();
        if (defaultPhone == null) {
            return null;
        }
        return defaultPhone.getContext();
    }

    public Object getRegistrantIdentifier() {
        return this.mRegistrantidentifier;
    }

    protected void registerForPhoneStates(Phone phone) {
        if (this.mHandlerMap.get(phone) != null) {
            Rlog.d(LOG_TAG, "This phone has already been registered.");
            return;
        }
        CallManagerHandler callManagerHandler = new CallManagerHandler();
        this.mHandlerMap.put(phone, callManagerHandler);
        phone.registerForPreciseCallStateChanged(callManagerHandler, 101, this.mRegistrantidentifier);
        phone.registerForDisconnect(callManagerHandler, 100, this.mRegistrantidentifier);
        phone.registerForNewRingingConnection(callManagerHandler, 102, this.mRegistrantidentifier);
        phone.registerForUnknownConnection(callManagerHandler, EVENT_UNKNOWN_CONNECTION, this.mRegistrantidentifier);
        phone.registerForIncomingRing(callManagerHandler, EVENT_INCOMING_RING, this.mRegistrantidentifier);
        phone.registerForRingbackTone(callManagerHandler, EVENT_RINGBACK_TONE, this.mRegistrantidentifier);
        phone.registerForInCallVoicePrivacyOn(callManagerHandler, 106, this.mRegistrantidentifier);
        phone.registerForInCallVoicePrivacyOff(callManagerHandler, EVENT_IN_CALL_VOICE_PRIVACY_OFF, this.mRegistrantidentifier);
        phone.registerForDisplayInfo(callManagerHandler, EVENT_DISPLAY_INFO, this.mRegistrantidentifier);
        phone.registerForSignalInfo(callManagerHandler, EVENT_SIGNAL_INFO, this.mRegistrantidentifier);
        phone.registerForResendIncallMute(callManagerHandler, 112, this.mRegistrantidentifier);
        phone.registerForMmiInitiate(callManagerHandler, 113, this.mRegistrantidentifier);
        phone.registerForMmiComplete(callManagerHandler, 114, this.mRegistrantidentifier);
        phone.registerForSuppServiceFailed(callManagerHandler, 117, this.mRegistrantidentifier);
        phone.registerForServiceStateChanged(callManagerHandler, 118, this.mRegistrantidentifier);
        phone.setOnPostDialCharacter(callManagerHandler, 119, null);
        phone.registerForCdmaOtaStatusChange(callManagerHandler, 111, null);
        phone.registerForSubscriptionInfoReady(callManagerHandler, 116, null);
        phone.registerForCallWaiting(callManagerHandler, EVENT_CALL_WAITING, null);
        phone.registerForEcmTimerReset(callManagerHandler, 115, null);
        phone.registerForOnHoldTone(callManagerHandler, 120, null);
        phone.registerForSuppServiceFailed(callManagerHandler, 117, null);
        phone.registerForTtyModeReceived(callManagerHandler, 122, null);
    }

    private void unregisterForPhoneStates(Phone phone) {
        CallManagerHandler callManagerHandler = this.mHandlerMap.get(phone);
        if (callManagerHandler == null) {
            Rlog.e(LOG_TAG, "Could not find Phone handler for unregistration");
            return;
        }
        this.mHandlerMap.remove(phone);
        phone.unregisterForPreciseCallStateChanged(callManagerHandler);
        phone.unregisterForDisconnect(callManagerHandler);
        phone.unregisterForNewRingingConnection(callManagerHandler);
        phone.unregisterForUnknownConnection(callManagerHandler);
        phone.unregisterForIncomingRing(callManagerHandler);
        phone.unregisterForRingbackTone(callManagerHandler);
        phone.unregisterForInCallVoicePrivacyOn(callManagerHandler);
        phone.unregisterForInCallVoicePrivacyOff(callManagerHandler);
        phone.unregisterForDisplayInfo(callManagerHandler);
        phone.unregisterForSignalInfo(callManagerHandler);
        phone.unregisterForResendIncallMute(callManagerHandler);
        phone.unregisterForMmiInitiate(callManagerHandler);
        phone.unregisterForMmiComplete(callManagerHandler);
        phone.unregisterForSuppServiceFailed(callManagerHandler);
        phone.unregisterForServiceStateChanged(callManagerHandler);
        phone.unregisterForTtyModeReceived(callManagerHandler);
        phone.setOnPostDialCharacter(null, 119, null);
        phone.unregisterForCdmaOtaStatusChange(callManagerHandler);
        phone.unregisterForSubscriptionInfoReady(callManagerHandler);
        phone.unregisterForCallWaiting(callManagerHandler);
        phone.unregisterForEcmTimerReset(callManagerHandler);
        phone.unregisterForOnHoldTone(callManagerHandler);
        phone.unregisterForSuppServiceFailed(callManagerHandler);
    }

    public void acceptCall(Call call) throws CallStateException {
        Phone phone = call.getPhone();
        if (hasActiveFgCall()) {
            Phone phone2 = getActiveFgCall().getPhone();
            boolean z = true;
            boolean z2 = !phone2.getBackgroundCall().isIdle();
            if (phone2 != phone) {
                z = false;
            }
            if (z && z2) {
                getActiveFgCall().hangup();
            } else if (!z && !z2) {
                phone2.switchHoldingAndActive();
            } else if (!z && z2) {
                getActiveFgCall().hangup();
            }
        }
        phone.acceptCall(0);
    }

    public void rejectCall(Call call) throws CallStateException {
        call.getPhone().rejectCall();
    }

    public void switchHoldingAndActive(Call call) throws CallStateException {
        Phone phone;
        if (hasActiveFgCall()) {
            phone = getActiveFgCall().getPhone();
        } else {
            phone = null;
        }
        Phone phone2 = call != null ? call.getPhone() : null;
        if (phone != null) {
            phone.switchHoldingAndActive();
        }
        if (phone2 != null && phone2 != phone) {
            phone2.switchHoldingAndActive();
        }
    }

    public void hangupForegroundResumeBackground(Call call) throws CallStateException {
        if (hasActiveFgCall()) {
            Phone fgPhone = getFgPhone();
            if (call != null) {
                if (fgPhone == call.getPhone()) {
                    getActiveFgCall().hangup();
                } else {
                    getActiveFgCall().hangup();
                    switchHoldingAndActive(call);
                }
            }
        }
    }

    public boolean canConference(Call call) {
        Phone phone;
        if (hasActiveFgCall()) {
            phone = getActiveFgCall().getPhone();
        } else {
            phone = null;
        }
        return (call != null ? call.getPhone() : null).getClass().equals(phone.getClass());
    }

    public boolean canConference(Call call, int i) {
        Phone phone;
        if (hasActiveFgCall(i)) {
            phone = getActiveFgCall(i).getPhone();
        } else {
            phone = null;
        }
        return (call != null ? call.getPhone() : null).getClass().equals(phone.getClass());
    }

    public void conference(Call call) throws CallStateException {
        Phone fgPhone = getFgPhone(call.getPhone().getSubId());
        if (fgPhone != null) {
            if (fgPhone instanceof SipPhone) {
                ((SipPhone) fgPhone).conference(call);
                return;
            } else {
                if (canConference(call)) {
                    fgPhone.conference();
                    return;
                }
                throw new CallStateException("Can't conference foreground and selected background call");
            }
        }
        Rlog.d(LOG_TAG, "conference: fgPhone=null");
    }

    public Connection dial(Phone phone, String str, int i) throws CallStateException {
        int subId = phone.getSubId();
        if (!canDial(phone)) {
            if (phone.handleInCallMmiCommands(PhoneNumberUtils.stripSeparators(str))) {
                return null;
            }
            throw new CallStateException("cannot dial in current state");
        }
        if (hasActiveFgCall(subId)) {
            Phone phone2 = getActiveFgCall(subId).getPhone();
            boolean z = !phone2.getBackgroundCall().isIdle();
            StringBuilder sb = new StringBuilder();
            sb.append("hasBgCall: ");
            sb.append(z);
            sb.append(" sameChannel:");
            sb.append(phone2 == phone);
            Rlog.d(LOG_TAG, sb.toString());
            Phone imsPhone = phone.getImsPhone();
            if (phone2 != phone && (imsPhone == null || imsPhone != phone2)) {
                if (z) {
                    Rlog.d(LOG_TAG, "Hangup");
                    getActiveFgCall(subId).hangup();
                } else {
                    Rlog.d(LOG_TAG, "Switch");
                    phone2.switchHoldingAndActive();
                }
            }
        }
        return phone.dial(str, new PhoneInternalInterface.DialArgs.Builder().setVideoState(i).build());
    }

    public Connection dial(Phone phone, String str, UUSInfo uUSInfo, int i) throws CallStateException {
        return phone.dial(str, new PhoneInternalInterface.DialArgs.Builder().setUusInfo(uUSInfo).setVideoState(i).build());
    }

    public void clearDisconnected() {
        Iterator<Phone> it = this.mPhones.iterator();
        while (it.hasNext()) {
            it.next().clearDisconnected();
        }
    }

    public void clearDisconnected(int i) {
        for (Phone phone : this.mPhones) {
            if (phone.getSubId() == i) {
                phone.clearDisconnected();
            }
        }
    }

    private boolean canDial(Phone phone) {
        int state = phone.getServiceState().getState();
        int subId = phone.getSubId();
        boolean zHasActiveRingingCall = hasActiveRingingCall();
        Call.State activeFgCallState = getActiveFgCallState(subId);
        boolean z = (state == 3 || zHasActiveRingingCall || (activeFgCallState != Call.State.ACTIVE && activeFgCallState != Call.State.IDLE && activeFgCallState != Call.State.DISCONNECTED && activeFgCallState != Call.State.ALERTING)) ? false : true;
        if (!z) {
            Rlog.d(LOG_TAG, "canDial serviceState=" + state + " hasRingingCall=" + zHasActiveRingingCall + " fgCallState=" + activeFgCallState);
        }
        return z;
    }

    public boolean canTransfer(Call call) {
        Phone phone;
        if (hasActiveFgCall()) {
            phone = getActiveFgCall().getPhone();
        } else {
            phone = null;
        }
        return (call != null ? call.getPhone() : null) == phone && phone.canTransfer();
    }

    public boolean canTransfer(Call call, int i) {
        Phone phone;
        if (hasActiveFgCall(i)) {
            phone = getActiveFgCall(i).getPhone();
        } else {
            phone = null;
        }
        return (call != null ? call.getPhone() : null) == phone && phone.canTransfer();
    }

    public void explicitCallTransfer(Call call) throws CallStateException {
        if (canTransfer(call)) {
            call.getPhone().explicitCallTransfer();
        }
    }

    public List<? extends MmiCode> getPendingMmiCodes(Phone phone) {
        Rlog.e(LOG_TAG, "getPendingMmiCodes not implemented");
        return null;
    }

    public boolean sendUssdResponse(Phone phone, String str) {
        Rlog.e(LOG_TAG, "sendUssdResponse not implemented");
        return false;
    }

    public void setMute(boolean z) {
        if (hasActiveFgCall()) {
            getActiveFgCall().getPhone().setMute(z);
        }
    }

    public boolean getMute() {
        if (hasActiveFgCall()) {
            return getActiveFgCall().getPhone().getMute();
        }
        if (hasActiveBgCall()) {
            return getFirstActiveBgCall().getPhone().getMute();
        }
        return false;
    }

    public void setEchoSuppressionEnabled() {
        if (hasActiveFgCall()) {
            getActiveFgCall().getPhone().setEchoSuppressionEnabled();
        }
    }

    public boolean sendDtmf(char c) {
        if (hasActiveFgCall()) {
            getActiveFgCall().getPhone().sendDtmf(c);
            return true;
        }
        return false;
    }

    public boolean startDtmf(char c) {
        if (hasActiveFgCall()) {
            getActiveFgCall().getPhone().startDtmf(c);
            return true;
        }
        return false;
    }

    public void stopDtmf() {
        if (hasActiveFgCall()) {
            getFgPhone().stopDtmf();
        }
    }

    public boolean sendBurstDtmf(String str, int i, int i2, Message message) {
        if (hasActiveFgCall()) {
            getActiveFgCall().getPhone().sendBurstDtmf(str, i, i2, message);
            return true;
        }
        return false;
    }

    public void registerForDisconnect(Handler handler, int i, Object obj) {
        this.mDisconnectRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForDisconnect(Handler handler) {
        this.mDisconnectRegistrants.remove(handler);
    }

    public void registerForPreciseCallStateChanged(Handler handler, int i, Object obj) {
        this.mPreciseCallStateRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForPreciseCallStateChanged(Handler handler) {
        this.mPreciseCallStateRegistrants.remove(handler);
    }

    public void registerForUnknownConnection(Handler handler, int i, Object obj) {
        this.mUnknownConnectionRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForUnknownConnection(Handler handler) {
        this.mUnknownConnectionRegistrants.remove(handler);
    }

    public void registerForNewRingingConnection(Handler handler, int i, Object obj) {
        this.mNewRingingConnectionRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForNewRingingConnection(Handler handler) {
        this.mNewRingingConnectionRegistrants.remove(handler);
    }

    public void registerForIncomingRing(Handler handler, int i, Object obj) {
        this.mIncomingRingRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForIncomingRing(Handler handler) {
        this.mIncomingRingRegistrants.remove(handler);
    }

    public void registerForRingbackTone(Handler handler, int i, Object obj) {
        this.mRingbackToneRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForRingbackTone(Handler handler) {
        this.mRingbackToneRegistrants.remove(handler);
    }

    public void registerForOnHoldTone(Handler handler, int i, Object obj) {
        this.mOnHoldToneRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForOnHoldTone(Handler handler) {
        this.mOnHoldToneRegistrants.remove(handler);
    }

    public void registerForResendIncallMute(Handler handler, int i, Object obj) {
        this.mResendIncallMuteRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForResendIncallMute(Handler handler) {
        this.mResendIncallMuteRegistrants.remove(handler);
    }

    public void registerForMmiInitiate(Handler handler, int i, Object obj) {
        this.mMmiInitiateRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForMmiInitiate(Handler handler) {
        this.mMmiInitiateRegistrants.remove(handler);
    }

    public void registerForMmiComplete(Handler handler, int i, Object obj) {
        Rlog.d(LOG_TAG, "registerForMmiComplete");
        this.mMmiCompleteRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForMmiComplete(Handler handler) {
        this.mMmiCompleteRegistrants.remove(handler);
    }

    public void registerForEcmTimerReset(Handler handler, int i, Object obj) {
        this.mEcmTimerResetRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForEcmTimerReset(Handler handler) {
        this.mEcmTimerResetRegistrants.remove(handler);
    }

    public void registerForServiceStateChanged(Handler handler, int i, Object obj) {
        this.mServiceStateChangedRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForServiceStateChanged(Handler handler) {
        this.mServiceStateChangedRegistrants.remove(handler);
    }

    public void registerForSuppServiceFailed(Handler handler, int i, Object obj) {
        this.mSuppServiceFailedRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForSuppServiceFailed(Handler handler) {
        this.mSuppServiceFailedRegistrants.remove(handler);
    }

    public void registerForInCallVoicePrivacyOn(Handler handler, int i, Object obj) {
        this.mInCallVoicePrivacyOnRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForInCallVoicePrivacyOn(Handler handler) {
        this.mInCallVoicePrivacyOnRegistrants.remove(handler);
    }

    public void registerForInCallVoicePrivacyOff(Handler handler, int i, Object obj) {
        this.mInCallVoicePrivacyOffRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForInCallVoicePrivacyOff(Handler handler) {
        this.mInCallVoicePrivacyOffRegistrants.remove(handler);
    }

    public void registerForCallWaiting(Handler handler, int i, Object obj) {
        this.mCallWaitingRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForCallWaiting(Handler handler) {
        this.mCallWaitingRegistrants.remove(handler);
    }

    public void registerForSignalInfo(Handler handler, int i, Object obj) {
        this.mSignalInfoRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForSignalInfo(Handler handler) {
        this.mSignalInfoRegistrants.remove(handler);
    }

    public void registerForDisplayInfo(Handler handler, int i, Object obj) {
        this.mDisplayInfoRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForDisplayInfo(Handler handler) {
        this.mDisplayInfoRegistrants.remove(handler);
    }

    public void registerForCdmaOtaStatusChange(Handler handler, int i, Object obj) {
        this.mCdmaOtaStatusChangeRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForCdmaOtaStatusChange(Handler handler) {
        this.mCdmaOtaStatusChangeRegistrants.remove(handler);
    }

    public void registerForSubscriptionInfoReady(Handler handler, int i, Object obj) {
        this.mSubscriptionInfoReadyRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForSubscriptionInfoReady(Handler handler) {
        this.mSubscriptionInfoReadyRegistrants.remove(handler);
    }

    public void registerForPostDialCharacter(Handler handler, int i, Object obj) {
        this.mPostDialCharacterRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForPostDialCharacter(Handler handler) {
        this.mPostDialCharacterRegistrants.remove(handler);
    }

    public void registerForTtyModeReceived(Handler handler, int i, Object obj) {
        this.mTtyModeReceivedRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForTtyModeReceived(Handler handler) {
        this.mTtyModeReceivedRegistrants.remove(handler);
    }

    public List<Call> getRingingCalls() {
        return Collections.unmodifiableList(this.mRingingCalls);
    }

    public List<Call> getForegroundCalls() {
        return Collections.unmodifiableList(this.mForegroundCalls);
    }

    public List<Call> getBackgroundCalls() {
        return Collections.unmodifiableList(this.mBackgroundCalls);
    }

    public boolean hasActiveFgCall() {
        return getFirstActiveCall(this.mForegroundCalls) != null;
    }

    public boolean hasActiveFgCall(int i) {
        return getFirstActiveCall(this.mForegroundCalls, i) != null;
    }

    public boolean hasActiveBgCall() {
        return getFirstActiveCall(this.mBackgroundCalls) != null;
    }

    public boolean hasActiveBgCall(int i) {
        return getFirstActiveCall(this.mBackgroundCalls, i) != null;
    }

    public boolean hasActiveRingingCall() {
        return getFirstActiveCall(this.mRingingCalls) != null;
    }

    public boolean hasActiveRingingCall(int i) {
        return getFirstActiveCall(this.mRingingCalls, i) != null;
    }

    public Call getActiveFgCall() {
        Call firstNonIdleCall = getFirstNonIdleCall(this.mForegroundCalls);
        if (firstNonIdleCall == null) {
            if (this.mDefaultPhone == null) {
                return null;
            }
            return this.mDefaultPhone.getForegroundCall();
        }
        return firstNonIdleCall;
    }

    public Call getActiveFgCall(int i) {
        Call foregroundCall;
        Call firstNonIdleCall = getFirstNonIdleCall(this.mForegroundCalls, i);
        if (firstNonIdleCall == null) {
            Phone phone = getPhone(i);
            if (phone == null) {
                foregroundCall = null;
            } else {
                foregroundCall = phone.getForegroundCall();
            }
            return foregroundCall;
        }
        return firstNonIdleCall;
    }

    private Call getFirstNonIdleCall(List<Call> list) {
        Call call = null;
        for (Call call2 : list) {
            if (!call2.isIdle()) {
                return call2;
            }
            if (call2.getState() != Call.State.IDLE && call == null) {
                call = call2;
            }
        }
        return call;
    }

    private Call getFirstNonIdleCall(List<Call> list, int i) {
        Call call = null;
        for (Call call2 : list) {
            if (call2.getPhone().getSubId() == i || (call2.getPhone() instanceof SipPhone)) {
                if (!call2.isIdle()) {
                    return call2;
                }
                if (call2.getState() != Call.State.IDLE && call == null) {
                    call = call2;
                }
            }
        }
        return call;
    }

    public Call getFirstActiveBgCall() {
        Call firstNonIdleCall = getFirstNonIdleCall(this.mBackgroundCalls);
        if (firstNonIdleCall == null) {
            if (this.mDefaultPhone == null) {
                return null;
            }
            return this.mDefaultPhone.getBackgroundCall();
        }
        return firstNonIdleCall;
    }

    public Call getFirstActiveBgCall(int i) {
        Phone phone = getPhone(i);
        if (hasMoreThanOneHoldingCall(i)) {
            return phone.getBackgroundCall();
        }
        Call firstNonIdleCall = getFirstNonIdleCall(this.mBackgroundCalls, i);
        if (firstNonIdleCall == null) {
            if (phone == null) {
                return null;
            }
            return phone.getBackgroundCall();
        }
        return firstNonIdleCall;
    }

    public Call getFirstActiveRingingCall() {
        Call firstNonIdleCall = getFirstNonIdleCall(this.mRingingCalls);
        if (firstNonIdleCall == null) {
            if (this.mDefaultPhone == null) {
                return null;
            }
            return this.mDefaultPhone.getRingingCall();
        }
        return firstNonIdleCall;
    }

    public Call getFirstActiveRingingCall(int i) {
        Phone phone = getPhone(i);
        Call firstNonIdleCall = getFirstNonIdleCall(this.mRingingCalls, i);
        if (firstNonIdleCall == null) {
            if (phone == null) {
                return null;
            }
            return phone.getRingingCall();
        }
        return firstNonIdleCall;
    }

    public Call.State getActiveFgCallState() {
        Call activeFgCall = getActiveFgCall();
        if (activeFgCall != null) {
            return activeFgCall.getState();
        }
        return Call.State.IDLE;
    }

    public Call.State getActiveFgCallState(int i) {
        Call activeFgCall = getActiveFgCall(i);
        if (activeFgCall != null) {
            return activeFgCall.getState();
        }
        return Call.State.IDLE;
    }

    public List<Connection> getFgCallConnections() {
        Call activeFgCall = getActiveFgCall();
        if (activeFgCall != null) {
            return activeFgCall.getConnections();
        }
        return this.mEmptyConnections;
    }

    public List<Connection> getFgCallConnections(int i) {
        Call activeFgCall = getActiveFgCall(i);
        if (activeFgCall != null) {
            return activeFgCall.getConnections();
        }
        return this.mEmptyConnections;
    }

    public List<Connection> getBgCallConnections() {
        Call firstActiveBgCall = getFirstActiveBgCall();
        if (firstActiveBgCall != null) {
            return firstActiveBgCall.getConnections();
        }
        return this.mEmptyConnections;
    }

    public List<Connection> getBgCallConnections(int i) {
        Call firstActiveBgCall = getFirstActiveBgCall(i);
        if (firstActiveBgCall != null) {
            return firstActiveBgCall.getConnections();
        }
        return this.mEmptyConnections;
    }

    public Connection getFgCallLatestConnection() {
        Call activeFgCall = getActiveFgCall();
        if (activeFgCall != null) {
            return activeFgCall.getLatestConnection();
        }
        return null;
    }

    public Connection getFgCallLatestConnection(int i) {
        Call activeFgCall = getActiveFgCall(i);
        if (activeFgCall != null) {
            return activeFgCall.getLatestConnection();
        }
        return null;
    }

    public boolean hasDisconnectedFgCall() {
        return getFirstCallOfState(this.mForegroundCalls, Call.State.DISCONNECTED) != null;
    }

    public boolean hasDisconnectedFgCall(int i) {
        return getFirstCallOfState(this.mForegroundCalls, Call.State.DISCONNECTED, i) != null;
    }

    public boolean hasDisconnectedBgCall() {
        return getFirstCallOfState(this.mBackgroundCalls, Call.State.DISCONNECTED) != null;
    }

    public boolean hasDisconnectedBgCall(int i) {
        return getFirstCallOfState(this.mBackgroundCalls, Call.State.DISCONNECTED, i) != null;
    }

    private Call getFirstActiveCall(ArrayList<Call> arrayList) {
        for (Call call : arrayList) {
            if (!call.isIdle()) {
                return call;
            }
        }
        return null;
    }

    private Call getFirstActiveCall(ArrayList<Call> arrayList, int i) {
        for (Call call : arrayList) {
            if (!call.isIdle() && (call.getPhone().getSubId() == i || (call.getPhone() instanceof SipPhone))) {
                return call;
            }
        }
        return null;
    }

    private Call getFirstCallOfState(ArrayList<Call> arrayList, Call.State state) {
        for (Call call : arrayList) {
            if (call.getState() == state) {
                return call;
            }
        }
        return null;
    }

    private Call getFirstCallOfState(ArrayList<Call> arrayList, Call.State state, int i) {
        for (Call call : arrayList) {
            if (call.getState() == state || call.getPhone().getSubId() == i || (call.getPhone() instanceof SipPhone)) {
                return call;
            }
        }
        return null;
    }

    protected boolean hasMoreThanOneRingingCall() {
        Iterator<Call> it = this.mRingingCalls.iterator();
        int i = 0;
        while (it.hasNext()) {
            if (it.next().getState().isRinging() && (i = i + 1) > 1) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMoreThanOneRingingCall(int i) {
        int i2 = 0;
        for (Call call : this.mRingingCalls) {
            if (call.getState().isRinging() && (call.getPhone().getSubId() == i || (call.getPhone() instanceof SipPhone))) {
                i2++;
                if (i2 > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasMoreThanOneHoldingCall(int i) {
        int i2 = 0;
        for (Call call : this.mBackgroundCalls) {
            if (call.getState() == Call.State.HOLDING && (call.getPhone().getSubId() == i || (call.getPhone() instanceof SipPhone))) {
                i2++;
                if (i2 > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    protected class CallManagerHandler extends Handler {
        protected CallManagerHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 100:
                    CallManager.this.mDisconnectRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    break;
                case 101:
                    CallManager.this.mPreciseCallStateRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    break;
                case 102:
                    Connection connection = (Connection) ((AsyncResult) message.obj).result;
                    if (CallManager.this.getActiveFgCallState(connection.getCall().getPhone().getSubId()).isDialing() || CallManager.this.hasMoreThanOneRingingCall()) {
                        try {
                            Rlog.d(CallManager.LOG_TAG, "silently drop incoming call: " + connection.getCall());
                            connection.getCall().hangup();
                        } catch (CallStateException e) {
                            Rlog.w(CallManager.LOG_TAG, "new ringing connection", e);
                            return;
                        }
                    } else {
                        CallManager.this.mNewRingingConnectionRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    }
                    break;
                case CallManager.EVENT_UNKNOWN_CONNECTION:
                    CallManager.this.mUnknownConnectionRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    break;
                case CallManager.EVENT_INCOMING_RING:
                    if (!CallManager.this.hasActiveFgCall()) {
                        CallManager.this.mIncomingRingRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    }
                    break;
                case CallManager.EVENT_RINGBACK_TONE:
                    CallManager.this.mRingbackToneRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    break;
                case 106:
                    CallManager.this.mInCallVoicePrivacyOnRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    break;
                case CallManager.EVENT_IN_CALL_VOICE_PRIVACY_OFF:
                    CallManager.this.mInCallVoicePrivacyOffRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    break;
                case CallManager.EVENT_CALL_WAITING:
                    CallManager.this.mCallWaitingRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    break;
                case CallManager.EVENT_DISPLAY_INFO:
                    CallManager.this.mDisplayInfoRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    break;
                case CallManager.EVENT_SIGNAL_INFO:
                    CallManager.this.mSignalInfoRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    break;
                case 111:
                    CallManager.this.mCdmaOtaStatusChangeRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    break;
                case 112:
                    CallManager.this.mResendIncallMuteRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    break;
                case 113:
                    CallManager.this.mMmiInitiateRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    break;
                case 114:
                    Rlog.d(CallManager.LOG_TAG, "CallManager: handleMessage (EVENT_MMI_COMPLETE)");
                    CallManager.this.mMmiCompleteRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    break;
                case 115:
                    CallManager.this.mEcmTimerResetRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    break;
                case 116:
                    CallManager.this.mSubscriptionInfoReadyRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    break;
                case 117:
                    CallManager.this.mSuppServiceFailedRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    break;
                case 118:
                    CallManager.this.mServiceStateChangedRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    break;
                case 119:
                    for (int i = 0; i < CallManager.this.mPostDialCharacterRegistrants.size(); i++) {
                        Message messageMessageForRegistrant = ((Registrant) CallManager.this.mPostDialCharacterRegistrants.get(i)).messageForRegistrant();
                        messageMessageForRegistrant.obj = message.obj;
                        messageMessageForRegistrant.arg1 = message.arg1;
                        messageMessageForRegistrant.sendToTarget();
                    }
                    break;
                case 120:
                    CallManager.this.mOnHoldToneRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    break;
                case 122:
                    CallManager.this.mTtyModeReceivedRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    break;
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            sb.append("CallManager {");
            sb.append("\nstate = " + getState(i));
            Call activeFgCall = getActiveFgCall(i);
            if (activeFgCall != null) {
                sb.append("\n- Foreground: " + getActiveFgCallState(i));
                sb.append(" from " + activeFgCall.getPhone());
                sb.append("\n  Conn: ");
                sb.append(getFgCallConnections(i));
            }
            Call firstActiveBgCall = getFirstActiveBgCall(i);
            if (firstActiveBgCall != null) {
                sb.append("\n- Background: " + firstActiveBgCall.getState());
                sb.append(" from " + firstActiveBgCall.getPhone());
                sb.append("\n  Conn: ");
                sb.append(getBgCallConnections(i));
            }
            Call firstActiveRingingCall = getFirstActiveRingingCall(i);
            if (firstActiveRingingCall != null) {
                sb.append("\n- Ringing: " + firstActiveRingingCall.getState());
                sb.append(" from " + firstActiveRingingCall.getPhone());
            }
        }
        for (Phone phone : getAllPhones()) {
            if (phone != null) {
                sb.append("\nPhone: " + phone + ", name = " + phone.getPhoneName() + ", state = " + phone.getState());
                Call foregroundCall = phone.getForegroundCall();
                if (foregroundCall != null) {
                    sb.append("\n- Foreground: ");
                    sb.append(foregroundCall);
                }
                Call backgroundCall = phone.getBackgroundCall();
                if (backgroundCall != null) {
                    sb.append(" Background: ");
                    sb.append(backgroundCall);
                }
                Call ringingCall = phone.getRingingCall();
                if (ringingCall != null) {
                    sb.append(" Ringing: ");
                    sb.append(ringingCall);
                }
            }
        }
        sb.append("\n}");
        return sb.toString();
    }
}
