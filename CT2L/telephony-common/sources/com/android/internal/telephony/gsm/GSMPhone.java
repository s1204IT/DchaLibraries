package com.android.internal.telephony.gsm;

import android.R;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.telephony.CellLocation;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CallTracker;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.Dsds;
import com.android.internal.telephony.IccPhoneBookInterfaceManager;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneNotifier;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.PhoneSubInfo;
import com.android.internal.telephony.RadioNVItems;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.UUSInfo;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.dataconnection.DctController;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.test.SimulatedRadioControl;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccVmNotSupportedException;
import com.android.internal.telephony.uicc.IsimRecords;
import com.android.internal.telephony.uicc.IsimUiccRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class GSMPhone extends PhoneBase {
    public static final String CIPHERING_KEY = "ciphering_key";
    private static final boolean LOCAL_DEBUG = true;
    static final String LOG_TAG = "GSMPhone";
    private static final boolean VDBG = false;
    public static final String VM_NUMBER = "vm_number_key";
    public static final String VM_SIM_IMSI = "vm_sim_imsi_key";
    GsmCallTracker mCT;
    private final RegistrantList mEcmTimerResetRegistrants;
    private String mImei;
    private String mImeiSv;
    private IsimUiccRecords mIsimUiccRecords;
    ArrayList<GsmMmiCode> mPendingMMIs;
    Registrant mPostDialHandler;
    GsmServiceStateTracker mSST;
    SimPhoneBookInterfaceManager mSimPhoneBookIntManager;
    RegistrantList mSsnRegistrants;
    PhoneSubInfo mSubInfo;
    private String mVmNumber;

    private static class Cfu {
        final Message mOnComplete;
        final String mSetCfNumber;

        Cfu(String cfNumber, Message onComplete) {
            this.mSetCfNumber = cfNumber;
            this.mOnComplete = onComplete;
        }
    }

    public GSMPhone(Context context, CommandsInterface ci, PhoneNotifier notifier, boolean unitTestMode) {
        super("GSM", notifier, context, ci, unitTestMode);
        this.mPendingMMIs = new ArrayList<>();
        this.mSsnRegistrants = new RegistrantList();
        this.mEcmTimerResetRegistrants = new RegistrantList();
        if (ci instanceof SimulatedRadioControl) {
            this.mSimulatedRadioControl = (SimulatedRadioControl) ci;
        }
        this.mCi.setPhoneType(1);
        this.mCT = new GsmCallTracker(this);
        this.mSST = new GsmServiceStateTracker(this);
        this.mDcTracker = new DcTracker(this);
        if (!unitTestMode) {
            this.mSimPhoneBookIntManager = new SimPhoneBookInterfaceManager(this);
            this.mSubInfo = new PhoneSubInfo(this);
        }
        this.mCi.registerForAvailable(this, 1, null);
        this.mCi.registerForOffOrNotAvailable(this, 8, null);
        this.mCi.registerForOn(this, 5, null);
        this.mCi.setOnUSSD(this, 7, null);
        this.mCi.setOnSuppServiceNotification(this, 2, null);
        this.mSST.registerForNetworkAttached(this, 19, null);
        this.mCi.setOnSs(this, 36, null);
        setProperties();
    }

    public GSMPhone(Context context, CommandsInterface ci, PhoneNotifier notifier, int phoneId) {
        this(context, ci, notifier, false, phoneId);
    }

    public GSMPhone(Context context, CommandsInterface ci, PhoneNotifier notifier, boolean unitTestMode, int phoneId) {
        super("GSM", notifier, context, ci, unitTestMode, phoneId);
        this.mPendingMMIs = new ArrayList<>();
        this.mSsnRegistrants = new RegistrantList();
        this.mEcmTimerResetRegistrants = new RegistrantList();
        if (ci instanceof SimulatedRadioControl) {
            this.mSimulatedRadioControl = (SimulatedRadioControl) ci;
        }
        this.mCi.setPhoneType(1);
        this.mCT = new GsmCallTracker(this);
        this.mSST = new GsmServiceStateTracker(this);
        this.mDcTracker = new DcTracker(this);
        if (!unitTestMode) {
            this.mSimPhoneBookIntManager = new SimPhoneBookInterfaceManager(this);
            this.mSubInfo = new PhoneSubInfo(this);
        }
        this.mCi.registerForAvailable(this, 1, null);
        this.mCi.registerForOffOrNotAvailable(this, 8, null);
        this.mCi.registerForOn(this, 5, null);
        this.mCi.setOnUSSD(this, 7, null);
        this.mCi.setOnSuppServiceNotification(this, 2, null);
        this.mSST.registerForNetworkAttached(this, 19, null);
        this.mCi.setOnSs(this, 36, null);
        setProperties();
        log("GSMPhone: constructor: sub = " + this.mPhoneId);
        setProperties();
    }

    protected void setProperties() {
        TelephonyManager.from(this.mContext).setPhoneType(getPhoneId(), 1);
    }

    @Override
    public void dispose() {
        synchronized (PhoneProxy.lockForRadioTechnologyChange) {
            super.dispose();
            this.mCi.unregisterForAvailable(this);
            unregisterForSimRecordEvents();
            this.mCi.unregisterForOffOrNotAvailable(this);
            this.mCi.unregisterForOn(this);
            this.mSST.unregisterForNetworkAttached(this);
            this.mCi.unSetOnUSSD(this);
            this.mCi.unSetOnSuppServiceNotification(this);
            this.mCi.unSetOnSs(this);
            this.mPendingMMIs.clear();
            this.mCT.dispose();
            this.mDcTracker.dispose();
            this.mSST.dispose();
            this.mSimPhoneBookIntManager.dispose();
            this.mSubInfo.dispose();
        }
    }

    @Override
    public void removeReferences() {
        Rlog.d(LOG_TAG, "removeReferences");
        this.mSimulatedRadioControl = null;
        this.mSimPhoneBookIntManager = null;
        this.mSubInfo = null;
        this.mCT = null;
        this.mSST = null;
        super.removeReferences();
    }

    protected void finalize() {
        Rlog.d(LOG_TAG, "GSMPhone finalized");
    }

    @Override
    public ServiceState getServiceState() {
        if ((this.mSST == null || this.mSST.mSS.getState() != 0) && this.mImsPhone != null) {
            return ServiceState.mergeServiceStates(this.mSST == null ? new ServiceState() : this.mSST.mSS, this.mImsPhone.getServiceState());
        }
        if (this.mSST != null) {
            return this.mSST.mSS;
        }
        return new ServiceState();
    }

    @Override
    public CellLocation getCellLocation() {
        return this.mSST.getCellLocation();
    }

    @Override
    public PhoneConstants.State getState() {
        PhoneConstants.State imsState;
        return (this.mImsPhone == null || (imsState = this.mImsPhone.getState()) == PhoneConstants.State.IDLE) ? this.mCT.mState : imsState;
    }

    @Override
    public int getPhoneType() {
        return 1;
    }

    @Override
    public ServiceStateTracker getServiceStateTracker() {
        return this.mSST;
    }

    @Override
    public CallTracker getCallTracker() {
        return this.mCT;
    }

    private void updateVoiceMail() {
        int countVoiceMessages = 0;
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            countVoiceMessages = r.getVoiceMessageCount();
        }
        int countVoiceMessagesStored = getStoredVoiceMessageCount();
        if (countVoiceMessages == -1 && countVoiceMessagesStored != 0) {
            countVoiceMessages = countVoiceMessagesStored;
        }
        Rlog.d(LOG_TAG, "updateVoiceMail countVoiceMessages = " + countVoiceMessages + " subId " + getSubId());
        setVoiceMessageCount(countVoiceMessages);
    }

    @Override
    public List<? extends MmiCode> getPendingMmiCodes() {
        return this.mPendingMMIs;
    }

    @Override
    public PhoneConstants.DataState getDataConnectionState(String apnType) {
        PhoneConstants.DataState ret = PhoneConstants.DataState.DISCONNECTED;
        if (this.mSST == null) {
            PhoneConstants.DataState ret2 = PhoneConstants.DataState.DISCONNECTED;
            return ret2;
        }
        if (!apnType.equals("emergency") && this.mSST.getCurrentDataConnectionState() != 0) {
            PhoneConstants.DataState ret3 = PhoneConstants.DataState.DISCONNECTED;
            return ret3;
        }
        if (!this.mDcTracker.isApnTypeEnabled(apnType) || !this.mDcTracker.isApnTypeActive(apnType)) {
            PhoneConstants.DataState ret4 = PhoneConstants.DataState.DISCONNECTED;
            return ret4;
        }
        switch (AnonymousClass1.$SwitchMap$com$android$internal$telephony$DctConstants$State[this.mDcTracker.getState(apnType).ordinal()]) {
            case 1:
            case 2:
            case 3:
                PhoneConstants.DataState ret5 = PhoneConstants.DataState.DISCONNECTED;
                return ret5;
            case 4:
            case 5:
                if ((this.mCT.mState != PhoneConstants.State.IDLE && !this.mSST.isConcurrentVoiceAndDataAllowed()) || this.mDcTracker.isDataBlockedByOther()) {
                    PhoneConstants.DataState ret6 = PhoneConstants.DataState.SUSPENDED;
                    return ret6;
                }
                PhoneConstants.DataState ret7 = PhoneConstants.DataState.CONNECTED;
                return ret7;
            case 6:
            case 7:
                PhoneConstants.DataState ret8 = PhoneConstants.DataState.CONNECTING;
                return ret8;
            default:
                return ret;
        }
    }

    @Override
    public Phone.DataActivityState getDataActivityState() {
        Phone.DataActivityState ret = Phone.DataActivityState.NONE;
        if (this.mSST.getCurrentDataConnectionState() == 0) {
            switch (AnonymousClass1.$SwitchMap$com$android$internal$telephony$DctConstants$Activity[this.mDcTracker.getActivity().ordinal()]) {
                case 1:
                    Phone.DataActivityState ret2 = Phone.DataActivityState.DATAIN;
                    return ret2;
                case 2:
                    Phone.DataActivityState ret3 = Phone.DataActivityState.DATAOUT;
                    return ret3;
                case 3:
                    Phone.DataActivityState ret4 = Phone.DataActivityState.DATAINANDOUT;
                    return ret4;
                case 4:
                    Phone.DataActivityState ret5 = Phone.DataActivityState.DORMANT;
                    return ret5;
                default:
                    Phone.DataActivityState ret6 = Phone.DataActivityState.NONE;
                    return ret6;
            }
        }
        return ret;
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$internal$telephony$DctConstants$Activity = new int[DctConstants.Activity.values().length];
        static final int[] $SwitchMap$com$android$internal$telephony$DctConstants$State;

        static {
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$Activity[DctConstants.Activity.DATAIN.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$Activity[DctConstants.Activity.DATAOUT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$Activity[DctConstants.Activity.DATAINANDOUT.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$Activity[DctConstants.Activity.DORMANT.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            $SwitchMap$com$android$internal$telephony$DctConstants$State = new int[DctConstants.State.values().length];
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.RETRYING.ordinal()] = 1;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.FAILED.ordinal()] = 2;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.IDLE.ordinal()] = 3;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.CONNECTED.ordinal()] = 4;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.DISCONNECTING.ordinal()] = 5;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.CONNECTING.ordinal()] = 6;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.SCANNING.ordinal()] = 7;
            } catch (NoSuchFieldError e11) {
            }
        }
    }

    void notifyPhoneStateChanged() {
        this.mNotifier.notifyPhoneState(this);
    }

    void notifyPreciseCallStateChanged() {
        super.notifyPreciseCallStateChangedP();
    }

    public void notifyNewRingingConnection(Connection c) {
        super.notifyNewRingingConnectionP(c);
    }

    void notifyDisconnect(Connection cn) {
        this.mDisconnectRegistrants.notifyResult(cn);
        this.mNotifier.notifyDisconnectCause(cn.getDisconnectCause(), cn.getPreciseDisconnectCause());
    }

    void notifyUnknownConnection(Connection cn) {
        this.mUnknownConnectionRegistrants.notifyResult(cn);
    }

    void notifySuppServiceFailed(Phone.SuppService code) {
        this.mSuppServiceFailedRegistrants.notifyResult(code);
    }

    void notifyServiceStateChanged(ServiceState ss) {
        super.notifyServiceStateChangedP(ss);
    }

    void notifyLocationChanged() {
        this.mNotifier.notifyCellLocation(this);
    }

    @Override
    public void notifyCallForwardingIndicator() {
        this.mNotifier.notifyCallForwardingChanged(this);
    }

    @Override
    public void setSystemProperty(String property, String value) {
        TelephonyManager.setTelephonyProperty(this.mPhoneId, property, value);
    }

    @Override
    public void registerForSuppServiceNotification(Handler h, int what, Object obj) {
        this.mSsnRegistrants.addUnique(h, what, obj);
        if (this.mSsnRegistrants.size() == 1) {
            this.mCi.setSuppServiceNotifications(true, null);
        }
    }

    @Override
    public void unregisterForSuppServiceNotification(Handler h) {
        this.mSsnRegistrants.remove(h);
        if (this.mSsnRegistrants.size() == 0) {
            this.mCi.setSuppServiceNotifications(false, null);
        }
    }

    @Override
    public void registerForSimRecordsLoaded(Handler h, int what, Object obj) {
        this.mSimRecordsLoadedRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForSimRecordsLoaded(Handler h) {
        this.mSimRecordsLoadedRegistrants.remove(h);
    }

    @Override
    public void acceptCall(int videoState) throws CallStateException {
        ImsPhone imsPhone = this.mImsPhone;
        if (imsPhone != null && imsPhone.getRingingCall().isRinging()) {
            imsPhone.acceptCall(videoState);
        } else {
            this.mCT.acceptCall();
        }
    }

    @Override
    public void rejectCall() throws CallStateException {
        this.mCT.rejectCall();
    }

    @Override
    public void switchHoldingAndActive() throws CallStateException {
        this.mCT.switchWaitingOrHoldingAndActive();
    }

    @Override
    public boolean canConference() {
        boolean canImsConference = false;
        if (this.mImsPhone != null) {
            canImsConference = this.mImsPhone.canConference();
        }
        return this.mCT.canConference() || canImsConference;
    }

    public boolean canDial() {
        return this.mCT.canDial();
    }

    @Override
    public void conference() {
        if (this.mImsPhone != null && this.mImsPhone.canConference()) {
            log("conference() - delegated to IMS phone");
            this.mImsPhone.conference();
        } else {
            this.mCT.conference();
        }
    }

    @Override
    public void clearDisconnected() {
        this.mCT.clearDisconnected();
    }

    @Override
    public boolean canTransfer() {
        return this.mCT.canTransfer();
    }

    @Override
    public void explicitCallTransfer() {
        this.mCT.explicitCallTransfer();
    }

    @Override
    public GsmCall getForegroundCall() {
        return this.mCT.mForegroundCall;
    }

    @Override
    public GsmCall getBackgroundCall() {
        return this.mCT.mBackgroundCall;
    }

    @Override
    public Call getRingingCall() {
        ImsPhone imsPhone = this.mImsPhone;
        if (this.mCT.mRingingCall != null && this.mCT.mRingingCall.isRinging()) {
            return this.mCT.mRingingCall;
        }
        if (imsPhone != null) {
            return imsPhone.getRingingCall();
        }
        return this.mCT.mRingingCall;
    }

    private boolean handleCallDeflectionIncallSupplementaryService(String dialString) {
        if (dialString.length() > 1) {
            return false;
        }
        if (getRingingCall().getState() != Call.State.IDLE) {
            Rlog.d(LOG_TAG, "MmiCode 0: rejectCall");
            try {
                this.mCT.rejectCall();
                return true;
            } catch (CallStateException e) {
                Rlog.d(LOG_TAG, "reject failed", e);
                notifySuppServiceFailed(Phone.SuppService.REJECT);
                return true;
            }
        }
        if (getBackgroundCall().getState() == Call.State.IDLE) {
            return true;
        }
        Rlog.d(LOG_TAG, "MmiCode 0: hangupWaitingOrBackground");
        this.mCT.hangupWaitingOrBackground();
        return true;
    }

    private boolean handleCallWaitingIncallSupplementaryService(String dialString) {
        int len = dialString.length();
        if (len > 2) {
            return false;
        }
        GsmCall call = getForegroundCall();
        try {
            if (len > 1) {
                char ch = dialString.charAt(1);
                int callIndex = ch - '0';
                if (callIndex >= 1 && callIndex <= 7) {
                    Rlog.d(LOG_TAG, "MmiCode 1: hangupConnectionByIndex " + callIndex);
                    this.mCT.hangupConnectionByIndex(call, callIndex);
                }
            } else if (call.getState() != Call.State.IDLE) {
                Rlog.d(LOG_TAG, "MmiCode 1: hangup foreground");
                this.mCT.hangup(call);
            } else {
                Rlog.d(LOG_TAG, "MmiCode 1: switchWaitingOrHoldingAndActive");
                this.mCT.switchWaitingOrHoldingAndActive();
            }
            return true;
        } catch (CallStateException e) {
            Rlog.d(LOG_TAG, "hangup failed", e);
            notifySuppServiceFailed(Phone.SuppService.HANGUP);
            return true;
        }
    }

    private boolean handleCallHoldIncallSupplementaryService(String dialString) {
        int len = dialString.length();
        if (len > 2) {
            return false;
        }
        GsmCall call = getForegroundCall();
        if (len > 1) {
            try {
                char ch = dialString.charAt(1);
                int callIndex = ch - '0';
                GsmConnection conn = this.mCT.getConnectionByIndex(call, callIndex);
                if (conn != null && callIndex >= 1 && callIndex <= 7) {
                    Rlog.d(LOG_TAG, "MmiCode 2: separate call " + callIndex);
                    this.mCT.separate(conn);
                } else {
                    Rlog.d(LOG_TAG, "separate: invalid call index " + callIndex);
                    notifySuppServiceFailed(Phone.SuppService.SEPARATE);
                }
                return true;
            } catch (CallStateException e) {
                Rlog.d(LOG_TAG, "separate failed", e);
                notifySuppServiceFailed(Phone.SuppService.SEPARATE);
                return true;
            }
        }
        try {
            if (getRingingCall().getState() != Call.State.IDLE) {
                Rlog.d(LOG_TAG, "MmiCode 2: accept ringing call");
                this.mCT.acceptCall();
            } else {
                Rlog.d(LOG_TAG, "MmiCode 2: switchWaitingOrHoldingAndActive");
                this.mCT.switchWaitingOrHoldingAndActive();
            }
            return true;
        } catch (CallStateException e2) {
            Rlog.d(LOG_TAG, "switch failed", e2);
            notifySuppServiceFailed(Phone.SuppService.SWITCH);
            return true;
        }
    }

    private boolean handleMultipartyIncallSupplementaryService(String dialString) {
        if (dialString.length() > 1) {
            return false;
        }
        Rlog.d(LOG_TAG, "MmiCode 3: merge calls");
        conference();
        return true;
    }

    private boolean handleEctIncallSupplementaryService(String dialString) {
        int len = dialString.length();
        if (len != 1) {
            return false;
        }
        Rlog.d(LOG_TAG, "MmiCode 4: explicit call transfer");
        explicitCallTransfer();
        return true;
    }

    private boolean handleCcbsIncallSupplementaryService(String dialString) {
        if (dialString.length() > 1) {
            return false;
        }
        Rlog.i(LOG_TAG, "MmiCode 5: CCBS not supported!");
        notifySuppServiceFailed(Phone.SuppService.UNKNOWN);
        return true;
    }

    @Override
    public boolean handleInCallMmiCommands(String dialString) throws CallStateException {
        if (this.mDomainSelection != null && this.mDomainSelection.utInterfaceEnabled()) {
            return this.mImsPhone.handleInCallMmiCommands(dialString);
        }
        if (isInCall() && !TextUtils.isEmpty(dialString)) {
            char ch = dialString.charAt(0);
            switch (ch) {
                case '0':
                    boolean result = handleCallDeflectionIncallSupplementaryService(dialString);
                    return result;
                case CallFailCause.QOS_NOT_AVAIL:
                    boolean result2 = handleCallWaitingIncallSupplementaryService(dialString);
                    return result2;
                case '2':
                    boolean result3 = handleCallHoldIncallSupplementaryService(dialString);
                    return result3;
                case RadioNVItems.RIL_NV_CDMA_PRL_VERSION:
                    boolean result4 = handleMultipartyIncallSupplementaryService(dialString);
                    return result4;
                case RadioNVItems.RIL_NV_CDMA_BC10:
                    boolean result5 = handleEctIncallSupplementaryService(dialString);
                    return result5;
                case RadioNVItems.RIL_NV_CDMA_BC14:
                    boolean result6 = handleCcbsIncallSupplementaryService(dialString);
                    return result6;
                default:
                    return false;
            }
        }
        return false;
    }

    boolean isInCall() {
        Call.State foregroundCallState = getForegroundCall().getState();
        Call.State backgroundCallState = getBackgroundCall().getState();
        Call.State ringingCallState = getRingingCall().getState();
        return foregroundCallState.isAlive() || backgroundCallState.isAlive() || ringingCallState.isAlive();
    }

    @Override
    public Connection dial(String dialString, int videoState) throws CallStateException {
        return dial(dialString, null, videoState);
    }

    @Override
    public Connection dial(String dialString, UUSInfo uusInfo, int videoState) throws CallStateException {
        ImsPhone imsPhone = this.mImsPhone;
        if (this.mDomainSelection != null && this.mDomainSelection.useVoLTE(dialString)) {
            try {
                Rlog.d(LOG_TAG, "Trying IMS PS call");
                return imsPhone.dial(dialString, videoState);
            } catch (CallStateException e) {
                Rlog.d(LOG_TAG, "IMS PS call exception " + e + ", imsPhone =" + imsPhone);
                if (!ImsPhone.CS_FALLBACK.equals(e.getMessage())) {
                    CallStateException ce = new CallStateException(e.getMessage());
                    ce.setStackTrace(e.getStackTrace());
                    throw ce;
                }
            }
        }
        Rlog.d(LOG_TAG, "Trying (non-IMS) CS call");
        return dialInternal(dialString, null, 0);
    }

    @Override
    protected Connection dialInternal(String dialString, UUSInfo uusInfo, int videoState) throws CallStateException {
        String newDialString = PhoneNumberUtils.stripSeparators(dialString);
        if (handleInCallMmiCommands(newDialString)) {
            return null;
        }
        String networkPortion = PhoneNumberUtils.extractNetworkPortionAlt(newDialString);
        GsmMmiCode mmi = GsmMmiCode.newFromDialString(networkPortion, this, this.mUiccApplication.get());
        Rlog.d(LOG_TAG, "dialing w/ mmi '" + mmi + "'...");
        if (mmi == null) {
            return this.mCT.dial(newDialString, uusInfo);
        }
        if (mmi.isTemporaryModeCLIR()) {
            return this.mCT.dial(mmi.mDialingNumber, mmi.getCLIRMode(), uusInfo);
        }
        this.mPendingMMIs.add(mmi);
        this.mMmiRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmi, (Throwable) null));
        mmi.processCode();
        return null;
    }

    @Override
    public boolean handlePinMmi(String dialString) {
        GsmMmiCode mmi = GsmMmiCode.newFromDialString(dialString, this, this.mUiccApplication.get());
        if (mmi == null || !mmi.isPinPukCommand()) {
            return false;
        }
        this.mPendingMMIs.add(mmi);
        this.mMmiRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmi, (Throwable) null));
        mmi.processCode();
        return true;
    }

    @Override
    public void sendUssdResponse(String ussdMessge) {
        GsmMmiCode mmi = GsmMmiCode.newFromUssdUserInput(ussdMessge, this, this.mUiccApplication.get());
        this.mPendingMMIs.add(mmi);
        this.mMmiRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmi, (Throwable) null));
        mmi.sendUssd(ussdMessge);
    }

    @Override
    public void sendDtmf(char c) {
        if (!PhoneNumberUtils.is12Key(c)) {
            Rlog.e(LOG_TAG, "sendDtmf called with invalid character '" + c + "'");
        } else if (this.mCT.mState == PhoneConstants.State.OFFHOOK) {
            this.mCi.sendDtmf(c, null);
        }
    }

    @Override
    public void startDtmf(char c) {
        if (!PhoneNumberUtils.is12Key(c)) {
            Rlog.e(LOG_TAG, "startDtmf called with invalid character '" + c + "'");
        } else {
            this.mCi.startDtmf(c, null);
        }
    }

    @Override
    public void stopDtmf() {
        this.mCi.stopDtmf(null);
    }

    public void sendBurstDtmf(String dtmfString) {
        Rlog.e(LOG_TAG, "[GSMPhone] sendBurstDtmf() is a CDMA method");
    }

    @Override
    public void setRadioPower(boolean power) {
        this.mSST.setRadioPower(power);
    }

    private void storeVoiceMailNumber(String number) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(VM_NUMBER + getPhoneId(), number);
        editor.apply();
        setVmSimImsi(getSubscriberId());
    }

    @Override
    public String getVoiceMailNumber() {
        String[] listArray;
        String[] defaultVMNumberArray;
        IccRecords r = this.mIccRecords.get();
        String number = r != null ? r.getVoiceMailNumber() : "";
        if (TextUtils.isEmpty(number)) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
            number = sp.getString(VM_NUMBER + getPhoneId(), null);
        }
        if (TextUtils.isEmpty(number) && (listArray = getContext().getResources().getStringArray(R.array.config_defaultAmbientContextServices)) != null && listArray.length > 0) {
            for (int i = 0; i < listArray.length; i++) {
                if (!TextUtils.isEmpty(listArray[i]) && (defaultVMNumberArray = listArray[i].split(";")) != null && defaultVMNumberArray.length > 0) {
                    if (defaultVMNumberArray.length == 1) {
                        number = defaultVMNumberArray[0];
                    } else if (defaultVMNumberArray.length == 2 && !TextUtils.isEmpty(defaultVMNumberArray[1]) && defaultVMNumberArray[1].equalsIgnoreCase(getGroupIdLevel1())) {
                        return defaultVMNumberArray[0];
                    }
                }
            }
            return number;
        }
        return number;
    }

    private String getVmSimImsi() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sp.getString(VM_SIM_IMSI + getPhoneId(), null);
    }

    private void setVmSimImsi(String imsi) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(VM_SIM_IMSI + getPhoneId(), imsi);
        editor.apply();
    }

    @Override
    public String getVoiceMailAlphaTag() {
        IccRecords r = this.mIccRecords.get();
        String ret = r != null ? r.getVoiceMailAlphaTag() : "";
        if (ret == null || ret.length() == 0) {
            return this.mContext.getText(R.string.defaultVoiceMailAlphaTag).toString();
        }
        return ret;
    }

    @Override
    public String getDeviceId() {
        return this.mImei;
    }

    @Override
    public String getDeviceSvn() {
        return this.mImeiSv;
    }

    @Override
    public IsimRecords getIsimRecords() {
        return this.mIsimUiccRecords;
    }

    @Override
    public String getImei() {
        return this.mImei;
    }

    @Override
    public String getEsn() {
        Rlog.e(LOG_TAG, "[GSMPhone] getEsn() is a CDMA method");
        return "0";
    }

    @Override
    public String getMeid() {
        Rlog.e(LOG_TAG, "[GSMPhone] getMeid() is a CDMA method");
        return "0";
    }

    @Override
    public String getNai() {
        IccRecords r = this.mUiccController.getIccRecords(this.mPhoneId, 2);
        if (Log.isLoggable(LOG_TAG, 2)) {
            Rlog.v(LOG_TAG, "IccRecords is " + r);
        }
        if (r != null) {
            return r.getNAI();
        }
        return null;
    }

    @Override
    public String getSubscriberId() {
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            return r.getIMSI();
        }
        return null;
    }

    @Override
    public String getGroupIdLevel1() {
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            return r.getGid1();
        }
        return null;
    }

    @Override
    public String getLine1Number() {
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            return r.getMsisdnNumber();
        }
        return null;
    }

    @Override
    public String getMsisdn() {
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            return r.getMsisdnNumber();
        }
        return null;
    }

    @Override
    public String getLine1AlphaTag() {
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            return r.getMsisdnAlphaTag();
        }
        return null;
    }

    @Override
    public boolean setLine1Number(String alphaTag, String number, Message onComplete) {
        IccRecords r = this.mIccRecords.get();
        if (r == null) {
            return false;
        }
        r.setMsisdnNumber(alphaTag, number, onComplete);
        return true;
    }

    @Override
    public void setVoiceMailNumber(String alphaTag, String voiceMailNumber, Message onComplete) {
        this.mVmNumber = voiceMailNumber;
        Message resp = obtainMessage(20, 0, 0, onComplete);
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            r.setVoiceMailNumber(alphaTag, this.mVmNumber, resp);
        }
    }

    private boolean isValidCommandInterfaceCFReason(int commandInterfaceCFReason) {
        switch (commandInterfaceCFReason) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                return true;
            default:
                return false;
        }
    }

    @Override
    public String getSystemProperty(String property, String defValue) {
        if (getUnitTestMode()) {
            return null;
        }
        return TelephonyManager.getTelephonyProperty(this.mPhoneId, property, defValue);
    }

    private boolean isValidCommandInterfaceCFAction(int commandInterfaceCFAction) {
        switch (commandInterfaceCFAction) {
            case 0:
            case 1:
            case 3:
            case 4:
                return true;
            case 2:
            default:
                return false;
        }
    }

    public void updateDataConnectionTracker() {
        ((DcTracker) this.mDcTracker).update();
    }

    protected boolean isCfEnable(int action) {
        return action == 1 || action == 3;
    }

    @Override
    public void getCallForwardingOption(int commandInterfaceCFReason, Message onComplete) {
        Message resp;
        if (this.mDomainSelection != null && this.mDomainSelection.utInterfaceEnabled()) {
            this.mImsPhone.getCallForwardingOption(commandInterfaceCFReason, onComplete);
            return;
        }
        if (isValidCommandInterfaceCFReason(commandInterfaceCFReason)) {
            Rlog.d(LOG_TAG, "requesting call forwarding query.");
            if (commandInterfaceCFReason == 0) {
                resp = obtainMessage(13, onComplete);
            } else {
                resp = onComplete;
            }
            this.mCi.queryCallForwardStatus(commandInterfaceCFReason, 0, null, resp);
        }
    }

    @Override
    public void setCallForwardingOption(int commandInterfaceCFAction, int commandInterfaceCFReason, String dialingNumber, int timerSeconds, Message onComplete) {
        Message resp;
        if (this.mDomainSelection != null && this.mDomainSelection.utInterfaceEnabled()) {
            this.mImsPhone.setCallForwardingOption(commandInterfaceCFAction, commandInterfaceCFReason, dialingNumber, timerSeconds, onComplete);
            return;
        }
        if (isValidCommandInterfaceCFAction(commandInterfaceCFAction) && isValidCommandInterfaceCFReason(commandInterfaceCFReason)) {
            if (commandInterfaceCFReason == 0) {
                Cfu cfu = new Cfu(dialingNumber, onComplete);
                resp = obtainMessage(12, isCfEnable(commandInterfaceCFAction) ? 1 : 0, 0, cfu);
            } else {
                resp = onComplete;
            }
            this.mCi.setCallForward(commandInterfaceCFAction, commandInterfaceCFReason, 1, dialingNumber, timerSeconds, resp);
        }
    }

    @Override
    public void getOutgoingCallerIdDisplay(Message onComplete) {
        this.mCi.getCLIR(onComplete);
    }

    @Override
    public void setOutgoingCallerIdDisplay(int commandInterfaceCLIRMode, Message onComplete) {
        this.mCi.setCLIR(commandInterfaceCLIRMode, obtainMessage(18, commandInterfaceCLIRMode, 0, onComplete));
    }

    @Override
    public void getCallWaiting(Message onComplete) {
        if (this.mDomainSelection != null && !this.mDomainSelection.isLocalCallWaitingEnabled() && this.mDomainSelection.utInterfaceEnabled()) {
            this.mImsPhone.getCallWaiting(onComplete);
        } else {
            this.mCi.queryCallWaiting(0, onComplete);
        }
    }

    @Override
    public void setCallWaiting(boolean enable, Message onComplete) {
        if (this.mDomainSelection != null && !this.mDomainSelection.isLocalCallWaitingEnabled() && this.mDomainSelection.utInterfaceEnabled()) {
            this.mImsPhone.setCallWaiting(enable, onComplete);
        } else {
            this.mCi.setCallWaiting(enable, 1, onComplete);
        }
    }

    @Override
    public void getAvailableNetworks(Message response) {
        this.mCi.getAvailableNetworks(response);
    }

    @Override
    public void getNeighboringCids(Message response) {
        this.mCi.getNeighboringCids(response);
    }

    @Override
    public void setOnPostDialCharacter(Handler h, int what, Object obj) {
        this.mPostDialHandler = new Registrant(h, what, obj);
    }

    @Override
    public void setUiTTYMode(int uiTtyMode, Message onComplete) {
        if (this.mImsPhone != null) {
            this.mImsPhone.setUiTTYMode(uiTtyMode, onComplete);
        }
    }

    @Override
    public void setMute(boolean muted) {
        this.mCT.setMute(muted);
    }

    @Override
    public boolean getMute() {
        return this.mCT.getMute();
    }

    @Override
    public void getDataCallList(Message response) {
        this.mCi.getDataCallList(response);
    }

    @Override
    public void updateServiceLocation() {
        this.mSST.enableSingleLocationUpdate();
    }

    @Override
    public void enableLocationUpdates() {
        this.mSST.enableLocationUpdates();
    }

    @Override
    public void disableLocationUpdates() {
        this.mSST.disableLocationUpdates();
    }

    @Override
    public boolean getDataRoamingEnabled() {
        return this.mDcTracker.getDataOnRoamingEnabled();
    }

    @Override
    public void setDataRoamingEnabled(boolean enable) {
        this.mDcTracker.setDataOnRoamingEnabled(enable);
    }

    @Override
    public boolean getDataEnabled() {
        return this.mDcTracker.getDataEnabled();
    }

    @Override
    public void setDataEnabled(boolean enable) {
        this.mDcTracker.setDataEnabled(enable);
    }

    void onMMIDone(GsmMmiCode mmi) {
        if (this.mPendingMMIs.remove(mmi) || mmi.isUssdRequest() || mmi.isSsInfo()) {
            this.mMmiCompleteRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmi, (Throwable) null));
        }
    }

    private void onNetworkInitiatedUssd(GsmMmiCode mmi) {
        this.mMmiCompleteRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmi, (Throwable) null));
    }

    private void onIncomingUSSD(int ussdMode, String ussdMessage) {
        boolean isUssdRequest = ussdMode == 1;
        boolean isUssdError = (ussdMode == 0 || ussdMode == 1) ? false : true;
        boolean isUssdRelease = ussdMode == 2;
        GsmMmiCode found = null;
        int i = 0;
        int s = this.mPendingMMIs.size();
        while (true) {
            if (i >= s) {
                break;
            }
            if (!this.mPendingMMIs.get(i).isPendingUSSD()) {
                i++;
            } else {
                GsmMmiCode found2 = this.mPendingMMIs.get(i);
                found = found2;
                break;
            }
        }
        if (found != null) {
            if (isUssdRelease) {
                found.onUssdRelease();
                return;
            } else if (isUssdError) {
                found.onUssdFinishedError();
                return;
            } else {
                found.onUssdFinished(ussdMessage, isUssdRequest);
                return;
            }
        }
        if (!isUssdError && ussdMessage != null) {
            GsmMmiCode mmi = GsmMmiCode.newNetworkInitiatedUssd(ussdMessage, isUssdRequest, this, this.mUiccApplication.get());
            onNetworkInitiatedUssd(mmi);
        }
    }

    protected void syncClirSetting() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        int clirSetting = sp.getInt(PhoneBase.CLIR_KEY + getPhoneId(), -1);
        if (clirSetting >= 0) {
            this.mCi.setCLIR(clirSetting, null);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 16:
            case 17:
                super.handleMessage(msg);
                break;
            default:
                if (!this.mIsTheCurrentActivePhone) {
                    Rlog.e(LOG_TAG, "Received message " + msg + "[" + msg.what + "] while being destroyed. Ignoring.");
                    break;
                } else {
                    switch (msg.what) {
                        case 1:
                            this.mCi.getBasebandVersion(obtainMessage(6));
                            this.mCi.getIMEI(obtainMessage(9));
                            this.mCi.getIMEISV(obtainMessage(10));
                            this.mCi.getRadioCapability(obtainMessage(35));
                            break;
                        case 2:
                            AsyncResult ar = (AsyncResult) msg.obj;
                            this.mSsnRegistrants.notifyRegistrants(ar);
                            break;
                        case 3:
                            updateCurrentCarrierInProvider();
                            String imsi = getVmSimImsi();
                            String imsiFromSIM = getSubscriberId();
                            if (imsi != null && imsiFromSIM != null && !imsiFromSIM.equals(imsi)) {
                                storeVoiceMailNumber(null);
                                setVmSimImsi(null);
                            }
                            this.mSimRecordsLoadedRegistrants.notifyRegistrants();
                            updateVoiceMail();
                            break;
                        case 4:
                        case 11:
                        case 14:
                        case 15:
                        case 16:
                        case 17:
                        case 21:
                        case 22:
                        case SmsHeader.ELT_ID_OBJECT_DISTR_INDICATOR:
                        case SmsHeader.ELT_ID_STANDARD_WVG_OBJECT:
                        case SmsHeader.ELT_ID_CHARACTER_SIZE_WVG_OBJECT:
                        case SmsHeader.ELT_ID_EXTENDED_OBJECT_DATA_REQUEST_CMD:
                        case 27:
                        case CallFailCause.STATUS_ENQUIRY:
                        case 31:
                        case 32:
                        case 33:
                        case 34:
                        case 35:
                        default:
                            super.handleMessage(msg);
                            break;
                        case 5:
                            break;
                        case 6:
                            AsyncResult ar2 = (AsyncResult) msg.obj;
                            if (ar2.exception == null) {
                                Rlog.d(LOG_TAG, "Baseband version: " + ar2.result);
                                TelephonyManager.from(this.mContext).setBasebandVersionForPhone(getPhoneId(), (String) ar2.result);
                            }
                            break;
                        case 7:
                            AsyncResult ar3 = (AsyncResult) msg.obj;
                            String[] ussdResult = (String[]) ar3.result;
                            if (ussdResult.length > 1) {
                                try {
                                    onIncomingUSSD(Integer.parseInt(ussdResult[0]), ussdResult[1]);
                                } catch (NumberFormatException e) {
                                    Rlog.w(LOG_TAG, "error parsing USSD");
                                    return;
                                }
                            }
                            break;
                        case 8:
                            for (int i = this.mPendingMMIs.size() - 1; i >= 0; i--) {
                                if (this.mPendingMMIs.get(i).isPendingUSSD()) {
                                    this.mPendingMMIs.get(i).onUssdFinishedError();
                                }
                            }
                            ImsPhone imsPhone = this.mImsPhone;
                            if (imsPhone != null) {
                                imsPhone.getServiceState().setStateOff();
                            }
                            this.mRadioOffOrNotAvailableRegistrants.notifyRegistrants();
                            break;
                        case 9:
                            AsyncResult ar4 = (AsyncResult) msg.obj;
                            if (ar4.exception == null) {
                                this.mImei = (String) ar4.result;
                            }
                            break;
                        case 10:
                            AsyncResult ar5 = (AsyncResult) msg.obj;
                            if (ar5.exception == null) {
                                this.mImeiSv = (String) ar5.result;
                            }
                            break;
                        case 12:
                            AsyncResult ar6 = (AsyncResult) msg.obj;
                            IccRecords r = this.mIccRecords.get();
                            Cfu cfu = (Cfu) ar6.userObj;
                            if (ar6.exception == null && r != null) {
                                r.setVoiceCallForwardingFlag(1, msg.arg1 == 1, cfu.mSetCfNumber);
                            }
                            if (cfu.mOnComplete != null) {
                                AsyncResult.forMessage(cfu.mOnComplete, ar6.result, ar6.exception);
                                cfu.mOnComplete.sendToTarget();
                            }
                            break;
                        case 13:
                            AsyncResult ar7 = (AsyncResult) msg.obj;
                            if (ar7.exception == null) {
                                handleCfuQueryResult((CallForwardInfo[]) ar7.result);
                            }
                            Message onComplete = (Message) ar7.userObj;
                            if (onComplete != null) {
                                AsyncResult.forMessage(onComplete, ar7.result, ar7.exception);
                                onComplete.sendToTarget();
                            }
                            break;
                        case 18:
                            AsyncResult ar8 = (AsyncResult) msg.obj;
                            if (ar8.exception == null) {
                                saveClirSetting(msg.arg1);
                            }
                            Message onComplete2 = (Message) ar8.userObj;
                            if (onComplete2 != null) {
                                AsyncResult.forMessage(onComplete2, ar8.result, ar8.exception);
                                onComplete2.sendToTarget();
                            }
                            break;
                        case 19:
                            syncClirSetting();
                            break;
                        case 20:
                            AsyncResult ar9 = (AsyncResult) msg.obj;
                            if (IccVmNotSupportedException.class.isInstance(ar9.exception)) {
                                storeVoiceMailNumber(this.mVmNumber);
                                ar9.exception = null;
                            }
                            Message onComplete3 = (Message) ar9.userObj;
                            if (onComplete3 != null) {
                                AsyncResult.forMessage(onComplete3, ar9.result, ar9.exception);
                                onComplete3.sendToTarget();
                            }
                            break;
                        case 28:
                            AsyncResult ar10 = (AsyncResult) msg.obj;
                            if (this.mSST.mSS.getIsManualSelection()) {
                                setNetworkSelectionModeAutomatic((Message) ar10.result);
                                Rlog.d(LOG_TAG, "SET_NETWORK_SELECTION_AUTOMATIC: set to automatic");
                            } else {
                                Rlog.d(LOG_TAG, "SET_NETWORK_SELECTION_AUTOMATIC: already automatic, ignore");
                            }
                            break;
                        case 29:
                            AsyncResult ar11 = (AsyncResult) msg.obj;
                            processIccRecordEvents(((Integer) ar11.result).intValue());
                            break;
                        case 36:
                            AsyncResult ar12 = (AsyncResult) msg.obj;
                            Rlog.d(LOG_TAG, "Event EVENT_SS received");
                            GsmMmiCode mmi = new GsmMmiCode(this, this.mUiccApplication.get());
                            mmi.processSsData(ar12);
                            break;
                    }
                }
                break;
        }
    }

    protected UiccCardApplication getUiccCardApplication() {
        return this.mUiccController.getUiccCardApplication(this.mPhoneId, 1);
    }

    @Override
    protected void onUpdateIccAvailability() {
        if (this.mUiccController != null) {
            UiccCardApplication newUiccApplication = this.mUiccController.getUiccCardApplication(this.mPhoneId, 3);
            IsimUiccRecords newIsimUiccRecords = null;
            if (newUiccApplication != null) {
                newIsimUiccRecords = (IsimUiccRecords) newUiccApplication.getIccRecords();
                log("New ISIM application found");
            }
            this.mIsimUiccRecords = newIsimUiccRecords;
            UiccCardApplication newUiccApplication2 = getUiccCardApplication();
            UiccCardApplication app = this.mUiccApplication.get();
            if (app != newUiccApplication2) {
                if (app != null) {
                    log("Removing stale icc objects.");
                    if (this.mIccRecords.get() != null) {
                        unregisterForSimRecordEvents();
                        this.mSimPhoneBookIntManager.updateIccRecords(null);
                    }
                    this.mIccRecords.set(null);
                    this.mUiccApplication.set(null);
                }
                if (newUiccApplication2 != null) {
                    log("New Uicc application found");
                    this.mUiccApplication.set(newUiccApplication2);
                    this.mIccRecords.set(newUiccApplication2.getIccRecords());
                    registerForSimRecordEvents();
                    this.mSimPhoneBookIntManager.updateIccRecords(this.mIccRecords.get());
                }
            }
        }
    }

    private void processIccRecordEvents(int eventCode) {
        switch (eventCode) {
            case 1:
                notifyCallForwardingIndicator();
                break;
        }
    }

    protected boolean isActiveDataPhone() {
        try {
            return getPhoneId() == DctController.getInstance().getDataActivePhoneId();
        } catch (RuntimeException e) {
            log("Get RuntimeException during creating Phones. It's normal!");
            return getPhoneId() == Dsds.getInitialDataAllowSIM();
        }
    }

    public boolean updateCurrentCarrierInProvider() {
        boolean isDataActive = isActiveDataPhone();
        String operatorNumeric = getOperatorNumeric();
        log("updateCurrentCarrierInProvider: getPhoneId() = " + getPhoneId() + " isDataActive = " + isDataActive + " operatorNumeric = " + operatorNumeric);
        if (!TextUtils.isEmpty(operatorNumeric) && isDataActive) {
            try {
                Uri uri = Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, Telephony.Carriers.CURRENT);
                ContentValues map = new ContentValues();
                map.put(Telephony.Carriers.NUMERIC, operatorNumeric);
                this.mContext.getContentResolver().insert(uri, map);
                return true;
            } catch (SQLException e) {
                Rlog.e(LOG_TAG, "Can't store current operator", e);
            }
        }
        return false;
    }

    public void saveClirSetting(int commandInterfaceCLIRMode) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(PhoneBase.CLIR_KEY + getPhoneId(), commandInterfaceCLIRMode);
        if (!editor.commit()) {
            Rlog.e(LOG_TAG, "failed to commit CLIR preference");
        }
    }

    private void handleCfuQueryResult(CallForwardInfo[] infos) {
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            if (infos == null || infos.length == 0) {
                r.setVoiceCallForwardingFlag(1, false, null);
                return;
            }
            int s = infos.length;
            for (int i = 0; i < s; i++) {
                if ((infos[i].serviceClass & 1) != 0) {
                    r.setVoiceCallForwardingFlag(1, infos[i].status == 1, infos[i].number);
                    return;
                }
            }
        }
    }

    @Override
    public PhoneSubInfo getPhoneSubInfo() {
        return this.mSubInfo;
    }

    @Override
    public IccPhoneBookInterfaceManager getIccPhoneBookInterfaceManager() {
        return this.mSimPhoneBookIntManager;
    }

    @Override
    public void activateCellBroadcastSms(int activate, Message response) {
        Rlog.e(LOG_TAG, "[GSMPhone] activateCellBroadcastSms() is obsolete; use SmsManager");
        response.sendToTarget();
    }

    @Override
    public void getCellBroadcastSmsConfig(Message response) {
        Rlog.e(LOG_TAG, "[GSMPhone] getCellBroadcastSmsConfig() is obsolete; use SmsManager");
        response.sendToTarget();
    }

    @Override
    public void setCellBroadcastSmsConfig(int[] configValuesArray, Message response) {
        Rlog.e(LOG_TAG, "[GSMPhone] setCellBroadcastSmsConfig() is obsolete; use SmsManager");
        response.sendToTarget();
    }

    @Override
    public boolean isCspPlmnEnabled() {
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            return r.isCspPlmnEnabled();
        }
        return false;
    }

    boolean isManualNetSelAllowed() {
        int i = Phone.PREFERRED_NT_MODE;
        int subId = getSubId();
        int nwMode = PhoneFactory.calculatePreferredNetworkType(this.mContext, subId);
        Rlog.d(LOG_TAG, "isManualNetSelAllowed in mode = " + nwMode);
        if (isManualSelProhibitedInGlobalMode() && (nwMode == 10 || nwMode == 7)) {
            Rlog.d(LOG_TAG, "Manual selection not supported in mode = " + nwMode);
            return false;
        }
        Rlog.d(LOG_TAG, "Manual selection is supported in mode = " + nwMode);
        return true;
    }

    private boolean isManualSelProhibitedInGlobalMode() {
        String[] configArray;
        boolean isProhibited = false;
        String configString = getContext().getResources().getString(R.string.config_systemDependencyInstaller);
        if (!TextUtils.isEmpty(configString) && (configArray = configString.split(";")) != null && ((configArray.length == 1 && configArray[0].equalsIgnoreCase("true")) || (configArray.length == 2 && !TextUtils.isEmpty(configArray[1]) && configArray[0].equalsIgnoreCase("true") && configArray[1].equalsIgnoreCase(getGroupIdLevel1())))) {
            isProhibited = true;
        }
        Rlog.d(LOG_TAG, "isManualNetSelAllowedInGlobal in current carrier is " + isProhibited);
        return isProhibited;
    }

    private void registerForSimRecordEvents() {
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            r.registerForNetworkSelectionModeAutomatic(this, 28, null);
            r.registerForRecordsEvents(this, 29, null);
            r.registerForRecordsLoaded(this, 3, null);
        }
    }

    private void unregisterForSimRecordEvents() {
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            r.unregisterForNetworkSelectionModeAutomatic(this);
            r.unregisterForRecordsEvents(this);
            r.unregisterForRecordsLoaded(this);
        }
    }

    @Override
    public void exitEmergencyCallbackMode() {
        if (this.mImsPhone != null) {
            this.mImsPhone.exitEmergencyCallbackMode();
        }
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("GSMPhone extends:");
        super.dump(fd, pw, args);
        pw.println(" mCT=" + this.mCT);
        pw.println(" mSST=" + this.mSST);
        pw.println(" mPendingMMIs=" + this.mPendingMMIs);
        pw.println(" mSimPhoneBookIntManager=" + this.mSimPhoneBookIntManager);
        pw.println(" mSubInfo=" + this.mSubInfo);
        pw.println(" mVmNumber=" + this.mVmNumber);
    }

    @Override
    public boolean setOperatorBrandOverride(String brand) {
        UiccCard card;
        boolean status = false;
        if (this.mUiccController != null && (card = this.mUiccController.getUiccCard(getPhoneId())) != null && (status = card.setOperatorBrandOverride(brand))) {
            IccRecords iccRecords = this.mIccRecords.get();
            if (iccRecords != null) {
                TelephonyManager.from(this.mContext).setSimOperatorNameForPhone(getPhoneId(), iccRecords.getServiceProviderName());
            }
            if (this.mSST != null) {
                this.mSST.pollState();
            }
        }
        return status;
    }

    public String getOperatorNumeric() {
        IccRecords r = this.mIccRecords.get();
        if (r == null) {
            return null;
        }
        String operatorNumeric = r.getOperatorNumeric();
        return operatorNumeric;
    }

    public void registerForAllDataDisconnected(Handler h, int what, Object obj) {
        ((DcTracker) this.mDcTracker).registerForAllDataDisconnected(h, what, obj);
    }

    public void unregisterForAllDataDisconnected(Handler h) {
        ((DcTracker) this.mDcTracker).unregisterForAllDataDisconnected(h);
    }

    public void setInternalDataEnabled(boolean enable, Message onCompleteMsg) {
        ((DcTracker) this.mDcTracker).setInternalDataEnabled(enable, onCompleteMsg);
    }

    public boolean setInternalDataEnabledFlag(boolean enable) {
        return ((DcTracker) this.mDcTracker).setInternalDataEnabledFlag(enable);
    }

    public void notifyEcbmTimerReset(Boolean flag) {
        this.mEcmTimerResetRegistrants.notifyResult(flag);
    }

    @Override
    public void registerForEcmTimerReset(Handler h, int what, Object obj) {
        this.mEcmTimerResetRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForEcmTimerReset(Handler h) {
        this.mEcmTimerResetRegistrants.remove(h);
    }

    @Override
    public void setVoiceMessageWaiting(int line, int countWaiting) {
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            r.setVoiceMessageWaiting(line, countWaiting);
        } else {
            log("SIM Records not found, MWI not updated");
        }
    }

    protected void log(String s) {
        Rlog.d(LOG_TAG, "[GSMPhone] " + s);
    }

    @Override
    public Connection dialVT(String dialString) throws CallStateException {
        return null;
    }
}
