package com.android.internal.telephony.imsphone;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.net.LinkProperties;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.telephony.CellLocation;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import com.android.ims.ImsCallForwardInfo;
import com.android.ims.ImsEcbm;
import com.android.ims.ImsEcbmStateListener;
import com.android.ims.ImsException;
import com.android.ims.ImsReasonInfo;
import com.android.ims.ImsSsInfo;
import com.android.ims.ImsUtInterface;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CallTracker;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccPhoneBookInterfaceManager;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneNotifier;
import com.android.internal.telephony.PhoneSubInfo;
import com.android.internal.telephony.RadioNVItems;
import com.android.internal.telephony.UUSInfo;
import com.android.internal.telephony.cdma.CDMAPhone;
import com.android.internal.telephony.gsm.CallFailCause;
import com.android.internal.telephony.gsm.GSMPhone;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import java.util.ArrayList;
import java.util.List;

public class ImsPhone extends ImsPhoneBase {
    static final int CANCEL_ECM_TIMER = 1;
    public static final String CS_FALLBACK = "cs_fallback";
    private static final boolean DBG = true;
    private static final int DEFAULT_ECM_EXIT_TIMER_VALUE = 300000;
    protected static final int EVENT_GET_CALL_BARRING_DONE = 38;
    protected static final int EVENT_GET_CALL_WAITING_DONE = 40;
    protected static final int EVENT_SET_CALL_BARRING_DONE = 37;
    protected static final int EVENT_SET_CALL_WAITING_DONE = 39;
    private static final String LOG_TAG = "ImsPhone";
    static final int RESTART_ECM_TIMER = 0;
    private static final boolean VDBG = false;
    ImsPhoneCallTracker mCT;
    PhoneBase mDefaultPhone;
    private Registrant mEcmExitRespRegistrant;
    private Runnable mExitEcmRunnable;
    ImsEcbmStateListener mImsEcbmStateListener;
    private boolean mImsRegistered;
    private final RegistrantList mImsStateChangedRegistrants;
    protected boolean mIsPhoneInEcmState;
    private String mLastDialString;
    ArrayList<ImsPhoneMmiCode> mPendingMMIs;
    Registrant mPostDialHandler;
    ServiceState mSS;
    private final RegistrantList mSilentRedialRegistrants;
    PowerManager.WakeLock mWakeLock;

    @Override
    public void activateCellBroadcastSms(int i, Message message) {
        super.activateCellBroadcastSms(i, message);
    }

    @Override
    public Connection dial(String str, UUSInfo uUSInfo, int i) throws CallStateException {
        return super.dial(str, uUSInfo, i);
    }

    @Override
    public boolean disableDataConnectivity() {
        return super.disableDataConnectivity();
    }

    @Override
    public void disableLocationUpdates() {
        super.disableLocationUpdates();
    }

    @Override
    public boolean enableDataConnectivity() {
        return super.enableDataConnectivity();
    }

    @Override
    public void enableLocationUpdates() {
        super.enableLocationUpdates();
    }

    @Override
    public List getAllCellInfo() {
        return super.getAllCellInfo();
    }

    @Override
    public void getAvailableNetworks(Message message) {
        super.getAvailableNetworks(message);
    }

    @Override
    public boolean getCallForwardingIndicator() {
        return super.getCallForwardingIndicator();
    }

    @Override
    public void getCellBroadcastSmsConfig(Message message) {
        super.getCellBroadcastSmsConfig(message);
    }

    @Override
    public CellLocation getCellLocation() {
        return super.getCellLocation();
    }

    @Override
    public List getCurrentDataConnectionList() {
        return super.getCurrentDataConnectionList();
    }

    @Override
    public Phone.DataActivityState getDataActivityState() {
        return super.getDataActivityState();
    }

    @Override
    public void getDataCallList(Message message) {
        super.getDataCallList(message);
    }

    @Override
    public PhoneConstants.DataState getDataConnectionState() {
        return super.getDataConnectionState();
    }

    @Override
    public PhoneConstants.DataState getDataConnectionState(String str) {
        return super.getDataConnectionState(str);
    }

    @Override
    public boolean getDataEnabled() {
        return super.getDataEnabled();
    }

    @Override
    public boolean getDataRoamingEnabled() {
        return super.getDataRoamingEnabled();
    }

    @Override
    public String getDeviceId() {
        return super.getDeviceId();
    }

    @Override
    public String getDeviceSvn() {
        return super.getDeviceSvn();
    }

    @Override
    public String getEsn() {
        return super.getEsn();
    }

    @Override
    public String getGroupIdLevel1() {
        return super.getGroupIdLevel1();
    }

    @Override
    public IccCard getIccCard() {
        return super.getIccCard();
    }

    @Override
    public IccFileHandler getIccFileHandler() {
        return super.getIccFileHandler();
    }

    @Override
    public IccPhoneBookInterfaceManager getIccPhoneBookInterfaceManager() {
        return super.getIccPhoneBookInterfaceManager();
    }

    @Override
    public boolean getIccRecordsLoaded() {
        return super.getIccRecordsLoaded();
    }

    @Override
    public String getIccSerialNumber() {
        return super.getIccSerialNumber();
    }

    @Override
    public String getImei() {
        return super.getImei();
    }

    @Override
    public String getLine1AlphaTag() {
        return super.getLine1AlphaTag();
    }

    @Override
    public String getLine1Number() {
        return super.getLine1Number();
    }

    @Override
    public LinkProperties getLinkProperties(String str) {
        return super.getLinkProperties(str);
    }

    @Override
    public String getMeid() {
        return super.getMeid();
    }

    @Override
    public boolean getMessageWaitingIndicator() {
        return super.getMessageWaitingIndicator();
    }

    @Override
    public void getNeighboringCids(Message message) {
        super.getNeighboringCids(message);
    }

    @Override
    public void getOutgoingCallerIdDisplay(Message message) {
        super.getOutgoingCallerIdDisplay(message);
    }

    @Override
    public PhoneSubInfo getPhoneSubInfo() {
        return super.getPhoneSubInfo();
    }

    @Override
    public int getPhoneType() {
        return super.getPhoneType();
    }

    @Override
    public SignalStrength getSignalStrength() {
        return super.getSignalStrength();
    }

    @Override
    public String getSubscriberId() {
        return super.getSubscriberId();
    }

    @Override
    public String getVoiceMailAlphaTag() {
        return super.getVoiceMailAlphaTag();
    }

    @Override
    public String getVoiceMailNumber() {
        return super.getVoiceMailNumber();
    }

    @Override
    public boolean handlePinMmi(String str) {
        return super.handlePinMmi(str);
    }

    @Override
    public boolean isDataConnectivityPossible() {
        return super.isDataConnectivityPossible();
    }

    @Override
    public void migrateFrom(PhoneBase phoneBase) {
        super.migrateFrom(phoneBase);
    }

    @Override
    public boolean needsOtaServiceProvisioning() {
        return super.needsOtaServiceProvisioning();
    }

    @Override
    public void notifyCallForwardingIndicator() {
        super.notifyCallForwardingIndicator();
    }

    @Override
    public void onTtyModeReceived(int i) {
        super.onTtyModeReceived(i);
    }

    @Override
    public void registerForOnHoldTone(Handler handler, int i, Object obj) {
        super.registerForOnHoldTone(handler, i, obj);
    }

    @Override
    public void registerForRingbackTone(Handler handler, int i, Object obj) {
        super.registerForRingbackTone(handler, i, obj);
    }

    @Override
    public void registerForSuppServiceNotification(Handler handler, int i, Object obj) {
        super.registerForSuppServiceNotification(handler, i, obj);
    }

    @Override
    public void registerForTtyModeReceived(Handler handler, int i, Object obj) {
        super.registerForTtyModeReceived(handler, i, obj);
    }

    @Override
    public void saveClirSetting(int i) {
        super.saveClirSetting(i);
    }

    @Override
    public void selectNetworkManually(OperatorInfo operatorInfo, Message message) {
        super.selectNetworkManually(operatorInfo, message);
    }

    @Override
    public void setCellBroadcastSmsConfig(int[] iArr, Message message) {
        super.setCellBroadcastSmsConfig(iArr, message);
    }

    @Override
    public void setDataEnabled(boolean z) {
        super.setDataEnabled(z);
    }

    @Override
    public void setDataRoamingEnabled(boolean z) {
        super.setDataRoamingEnabled(z);
    }

    @Override
    public boolean setLine1Number(String str, String str2, Message message) {
        return super.setLine1Number(str, str2, message);
    }

    @Override
    public void setNetworkSelectionModeAutomatic(Message message) {
        super.setNetworkSelectionModeAutomatic(message);
    }

    @Override
    public void setOutgoingCallerIdDisplay(int i, Message message) {
        super.setOutgoingCallerIdDisplay(i, message);
    }

    @Override
    public void setRadioPower(boolean z) {
        super.setRadioPower(z);
    }

    @Override
    public void setVoiceMailNumber(String str, String str2, Message message) {
        super.setVoiceMailNumber(str, str2, message);
    }

    @Override
    public void unregisterForOnHoldTone(Handler handler) {
        super.unregisterForOnHoldTone(handler);
    }

    @Override
    public void unregisterForRingbackTone(Handler handler) {
        super.unregisterForRingbackTone(handler);
    }

    @Override
    public void unregisterForSuppServiceNotification(Handler handler) {
        super.unregisterForSuppServiceNotification(handler);
    }

    @Override
    public void unregisterForTtyModeReceived(Handler handler) {
        super.unregisterForTtyModeReceived(handler);
    }

    @Override
    public void updateServiceLocation() {
        super.updateServiceLocation();
    }

    private static class Cf {
        final boolean mIsCfu;
        final Message mOnComplete;
        final String mSetCfNumber;

        Cf(String cfNumber, boolean isCfu, Message onComplete) {
            this.mSetCfNumber = cfNumber;
            this.mIsCfu = isCfu;
            this.mOnComplete = onComplete;
        }
    }

    ImsPhone(Context context, PhoneNotifier notifier, Phone defaultPhone) {
        super(LOG_TAG, context, notifier);
        this.mPendingMMIs = new ArrayList<>();
        this.mSS = new ServiceState();
        this.mSilentRedialRegistrants = new RegistrantList();
        this.mImsRegistered = false;
        this.mImsStateChangedRegistrants = new RegistrantList();
        this.mExitEcmRunnable = new Runnable() {
            @Override
            public void run() {
                ImsPhone.this.exitEmergencyCallbackMode();
            }
        };
        this.mImsEcbmStateListener = new ImsEcbmStateListener() {
            public void onECBMEntered() {
                Rlog.d(ImsPhone.LOG_TAG, "onECBMEntered");
                ImsPhone.this.handleEnterEmergencyCallbackMode();
            }

            public void onECBMExited() {
                Rlog.d(ImsPhone.LOG_TAG, "onECBMExited");
                ImsPhone.this.handleExitEmergencyCallbackMode();
            }
        };
        this.mDefaultPhone = (PhoneBase) defaultPhone;
        this.mCT = new ImsPhoneCallTracker(this);
        this.mSS.setStateOff();
        this.mPhoneId = this.mDefaultPhone.getPhoneId();
        this.mIsPhoneInEcmState = SystemProperties.getBoolean("ril.cdma.inecmmode", false);
        PowerManager pm = (PowerManager) context.getSystemService("power");
        this.mWakeLock = pm.newWakeLock(1, LOG_TAG);
        this.mWakeLock.setReferenceCounted(false);
    }

    public void updateParentPhone(PhoneBase parentPhone) {
        this.mDefaultPhone = parentPhone;
        this.mPhoneId = this.mDefaultPhone.getPhoneId();
    }

    @Override
    public void dispose() {
        Rlog.d(LOG_TAG, "dispose");
        this.mPendingMMIs.clear();
        this.mCT.dispose();
    }

    @Override
    public void removeReferences() {
        Rlog.d(LOG_TAG, "removeReferences");
        super.removeReferences();
        this.mCT = null;
        this.mSS = null;
    }

    @Override
    public ServiceState getServiceState() {
        return this.mSS;
    }

    void setServiceState(int state) {
        this.mSS.setState(state);
    }

    @Override
    public CallTracker getCallTracker() {
        return this.mCT;
    }

    @Override
    public List<? extends ImsPhoneMmiCode> getPendingMmiCodes() {
        return this.mPendingMMIs;
    }

    @Override
    public void acceptCall(int videoState) throws CallStateException {
        this.mCT.acceptCall(videoState);
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
        return this.mCT.canConference();
    }

    @Override
    public boolean canDial() {
        return this.mCT.canDial();
    }

    @Override
    public void conference() {
        this.mCT.conference();
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
    public ImsPhoneCall getForegroundCall() {
        return this.mCT.mForegroundCall;
    }

    @Override
    public ImsPhoneCall getBackgroundCall() {
        return this.mCT.mBackgroundCall;
    }

    @Override
    public ImsPhoneCall getRingingCall() {
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
        try {
            this.mCT.hangup(getBackgroundCall());
            return true;
        } catch (CallStateException e2) {
            Rlog.d(LOG_TAG, "hangup failed", e2);
            return true;
        }
    }

    private boolean handleCallWaitingIncallSupplementaryService(String dialString) {
        int len = dialString.length();
        if (len > 2) {
            return false;
        }
        ImsPhoneCall call = getForegroundCall();
        try {
            if (len > 1) {
                Rlog.d(LOG_TAG, "not support 1X SEND");
                notifySuppServiceFailed(Phone.SuppService.HANGUP);
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
        getForegroundCall();
        if (len > 1) {
            Rlog.d(LOG_TAG, "separate not supported");
            notifySuppServiceFailed(Phone.SuppService.SEPARATE);
            return true;
        }
        try {
            if (getRingingCall().getState() != Call.State.IDLE) {
                Rlog.d(LOG_TAG, "MmiCode 2: accept ringing call");
                this.mCT.acceptCall(2);
            } else {
                Rlog.d(LOG_TAG, "MmiCode 2: switchWaitingOrHoldingAndActive");
                this.mCT.switchWaitingOrHoldingAndActive();
            }
            return true;
        } catch (CallStateException e) {
            Rlog.d(LOG_TAG, "switch failed", e);
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
        Rlog.d(LOG_TAG, "MmiCode 4: not support explicit call transfer");
        notifySuppServiceFailed(Phone.SuppService.TRANSFER);
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
    public boolean handleInCallMmiCommands(String dialString) {
        if (isInCall() && !TextUtils.isEmpty(dialString)) {
            char ch = dialString.charAt(0);
            switch (ch) {
                case '0':
                    boolean result = handleCallDeflectionIncallSupplementaryService(dialString);
                    break;
                case CallFailCause.QOS_NOT_AVAIL:
                    boolean result2 = handleCallWaitingIncallSupplementaryService(dialString);
                    break;
                case '2':
                    boolean result3 = handleCallHoldIncallSupplementaryService(dialString);
                    break;
                case RadioNVItems.RIL_NV_CDMA_PRL_VERSION:
                    boolean result4 = handleMultipartyIncallSupplementaryService(dialString);
                    break;
                case RadioNVItems.RIL_NV_CDMA_BC10:
                    boolean result5 = handleEctIncallSupplementaryService(dialString);
                    break;
                case RadioNVItems.RIL_NV_CDMA_BC14:
                    boolean result6 = handleCcbsIncallSupplementaryService(dialString);
                    break;
            }
            return false;
        }
        return false;
    }

    @Override
    boolean isInCall() {
        Call.State foregroundCallState = getForegroundCall().getState();
        Call.State backgroundCallState = getBackgroundCall().getState();
        Call.State ringingCallState = getRingingCall().getState();
        return foregroundCallState.isAlive() || backgroundCallState.isAlive() || ringingCallState.isAlive();
    }

    void notifyNewRingingConnection(Connection c) {
        this.mDefaultPhone.notifyNewRingingConnectionP(c);
    }

    @Override
    public Connection dial(String dialString, int videoState) throws CallStateException {
        return dialInternal(dialString, videoState);
    }

    protected Connection dialInternal(String dialString, int videoState) throws CallStateException {
        String newDialString = PhoneNumberUtils.stripSeparators(dialString);
        if (handleInCallMmiCommands(newDialString)) {
            return null;
        }
        if (this.mDefaultPhone.getPhoneType() == 2) {
            return this.mCT.dial(dialString, videoState);
        }
        String networkPortion = PhoneNumberUtils.extractNetworkPortionAlt(newDialString);
        ImsPhoneMmiCode mmi = ImsPhoneMmiCode.newFromDialString(networkPortion, this);
        Rlog.d(LOG_TAG, "dialing w/ mmi '" + mmi + "'...");
        this.mLastDialString = dialString;
        if (mmi == null) {
            return this.mCT.dial(dialString, videoState);
        }
        if (mmi.isTemporaryModeCLIR()) {
            return this.mCT.dial(mmi.getDialingNumber(), mmi.getCLIRMode(), videoState);
        }
        if (!mmi.isSupportedOverImsPhone()) {
            throw new CallStateException(CS_FALLBACK);
        }
        this.mPendingMMIs.add(mmi);
        this.mMmiRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmi, (Throwable) null));
        mmi.processCode();
        return null;
    }

    @Override
    public void sendDtmf(char c) {
        if (!PhoneNumberUtils.is12Key(c)) {
            Rlog.e(LOG_TAG, "sendDtmf called with invalid character '" + c + "'");
        } else if (this.mCT.mState == PhoneConstants.State.OFFHOOK) {
            this.mCT.sendDtmf(c, null);
        }
    }

    @Override
    public void startDtmf(char c) {
        if (!PhoneNumberUtils.is12Key(c) && (c < 'A' || c > 'D')) {
            Rlog.e(LOG_TAG, "startDtmf called with invalid character '" + c + "'");
        } else {
            this.mCT.startDtmf(c);
        }
    }

    @Override
    public void stopDtmf() {
        this.mCT.stopDtmf();
    }

    @Override
    public void setOnPostDialCharacter(Handler h, int what, Object obj) {
        this.mPostDialHandler = new Registrant(h, what, obj);
    }

    void notifyIncomingRing() {
        Rlog.d(LOG_TAG, "notifyIncomingRing");
        AsyncResult ar = new AsyncResult((Object) null, (Object) null, (Throwable) null);
        sendMessage(obtainMessage(14, ar));
    }

    @Override
    public void setMute(boolean muted) {
        this.mCT.setMute(muted);
    }

    @Override
    public void setUiTTYMode(int uiTtyMode, Message onComplete) {
        this.mCT.setUiTTYMode(uiTtyMode, onComplete);
    }

    @Override
    public boolean getMute() {
        return this.mCT.getMute();
    }

    @Override
    public PhoneConstants.State getState() {
        return this.mCT.mState;
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

    private boolean isCfEnable(int action) {
        return action == 1 || action == 3;
    }

    private int getConditionFromCFReason(int reason) {
        switch (reason) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            default:
                return -1;
        }
    }

    private int getCFReasonFromCondition(int condition) {
        switch (condition) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
            default:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
        }
    }

    private int getActionFromCFAction(int action) {
        switch (action) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
            default:
                return -1;
            case 3:
                return 3;
            case 4:
                return 4;
        }
    }

    @Override
    public void getCallForwardingOption(int commandInterfaceCFReason, Message onComplete) {
        Rlog.d(LOG_TAG, "getCallForwardingOption reason=" + commandInterfaceCFReason);
        if (isValidCommandInterfaceCFReason(commandInterfaceCFReason)) {
            Rlog.d(LOG_TAG, "requesting call forwarding query.");
            Message resp = obtainMessage(13, onComplete);
            try {
                ImsUtInterface ut = this.mCT.getUtInterface();
                ut.queryCallForward(getConditionFromCFReason(commandInterfaceCFReason), (String) null, resp);
                return;
            } catch (ImsException e) {
                sendErrorResponse(onComplete, (Throwable) e);
                return;
            }
        }
        if (onComplete != null) {
            sendErrorResponse(onComplete);
        }
    }

    @Override
    public void setCallForwardingOption(int commandInterfaceCFAction, int commandInterfaceCFReason, String dialingNumber, int timerSeconds, Message onComplete) {
        Rlog.d(LOG_TAG, "setCallForwardingOption action=" + commandInterfaceCFAction + ", reason=" + commandInterfaceCFReason);
        if (isValidCommandInterfaceCFAction(commandInterfaceCFAction) && isValidCommandInterfaceCFReason(commandInterfaceCFReason)) {
            Cf cf = new Cf(dialingNumber, commandInterfaceCFReason == 0, onComplete);
            obtainMessage(12, isCfEnable(commandInterfaceCFAction) ? 1 : 0, 0, cf);
            try {
                ImsUtInterface ut = this.mCT.getUtInterface();
                ut.updateCallForward(getActionFromCFAction(commandInterfaceCFAction), getConditionFromCFReason(commandInterfaceCFReason), dialingNumber, timerSeconds, onComplete);
                return;
            } catch (ImsException e) {
                sendErrorResponse(onComplete, (Throwable) e);
                return;
            }
        }
        if (onComplete != null) {
            sendErrorResponse(onComplete);
        }
    }

    @Override
    public void getCallWaiting(Message onComplete) {
        Rlog.d(LOG_TAG, "getCallWaiting");
        Message resp = obtainMessage(40, onComplete);
        try {
            ImsUtInterface ut = this.mCT.getUtInterface();
            ut.queryCallWaiting(resp);
        } catch (ImsException e) {
            sendErrorResponse(onComplete, (Throwable) e);
        }
    }

    @Override
    public void setCallWaiting(boolean enable, Message onComplete) {
        Rlog.d(LOG_TAG, "setCallWaiting enable=" + enable);
        Message resp = obtainMessage(39, onComplete);
        try {
            ImsUtInterface ut = this.mCT.getUtInterface();
            ut.updateCallWaiting(enable, resp);
        } catch (ImsException e) {
            sendErrorResponse(onComplete, (Throwable) e);
        }
    }

    private int getCBTypeFromFacility(String facility) {
        if (CommandsInterface.CB_FACILITY_BAOC.equals(facility)) {
            return 2;
        }
        if (CommandsInterface.CB_FACILITY_BAOIC.equals(facility)) {
            return 3;
        }
        if (CommandsInterface.CB_FACILITY_BAOICxH.equals(facility)) {
            return 4;
        }
        if (CommandsInterface.CB_FACILITY_BAIC.equals(facility)) {
            return 1;
        }
        if (CommandsInterface.CB_FACILITY_BAICr.equals(facility)) {
            return 5;
        }
        if (CommandsInterface.CB_FACILITY_BA_ALL.equals(facility)) {
            return 7;
        }
        if (CommandsInterface.CB_FACILITY_BA_MO.equals(facility)) {
            return 8;
        }
        if (CommandsInterface.CB_FACILITY_BA_MT.equals(facility)) {
            return 9;
        }
        return 0;
    }

    void getCallBarring(String facility, Message onComplete) {
        Rlog.d(LOG_TAG, "getCallBarring facility=" + facility);
        Message resp = obtainMessage(38, onComplete);
        try {
            ImsUtInterface ut = this.mCT.getUtInterface();
            ut.queryCallBarring(getCBTypeFromFacility(facility), resp);
        } catch (ImsException e) {
            sendErrorResponse(onComplete, (Throwable) e);
        }
    }

    void setCallBarring(String facility, boolean lockState, String password, Message onComplete) {
        Rlog.d(LOG_TAG, "setCallBarring facility=" + facility + ", lockState=" + lockState);
        Message resp = obtainMessage(37, onComplete);
        try {
            ImsUtInterface ut = this.mCT.getUtInterface();
            ut.updateCallBarring(getCBTypeFromFacility(facility), lockState, resp, (String[]) null);
        } catch (ImsException e) {
            sendErrorResponse(onComplete, (Throwable) e);
        }
    }

    @Override
    public void sendUssdResponse(String ussdMessge) {
        Rlog.d(LOG_TAG, "sendUssdResponse");
        ImsPhoneMmiCode mmi = ImsPhoneMmiCode.newFromUssdUserInput(ussdMessge, this);
        this.mPendingMMIs.add(mmi);
        this.mMmiRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmi, (Throwable) null));
        mmi.sendUssd(ussdMessge);
    }

    void sendUSSD(String ussdString, Message response) {
        this.mCT.sendUSSD(ussdString, response);
    }

    void cancelUSSD() {
        this.mCT.cancelUSSD();
    }

    void sendErrorResponse(Message onComplete) {
        Rlog.d(LOG_TAG, "sendErrorResponse");
        if (onComplete != null) {
            AsyncResult.forMessage(onComplete, (Object) null, new CommandException(CommandException.Error.GENERIC_FAILURE));
            onComplete.sendToTarget();
        }
    }

    void sendErrorResponse(Message onComplete, Throwable e) {
        Rlog.d(LOG_TAG, "sendErrorResponse");
        if (onComplete != null) {
            AsyncResult.forMessage(onComplete, (Object) null, getCommandException(e));
            onComplete.sendToTarget();
        }
    }

    void sendErrorResponse(Message onComplete, ImsReasonInfo reasonInfo) {
        Rlog.d(LOG_TAG, "sendErrorResponse reasonCode=" + reasonInfo.getCode());
        if (onComplete != null) {
            AsyncResult.forMessage(onComplete, (Object) null, getCommandException(reasonInfo.getCode()));
            onComplete.sendToTarget();
        }
    }

    CommandException getCommandException(int code) {
        Rlog.d(LOG_TAG, "getCommandException code=" + code);
        CommandException.Error error = CommandException.Error.GENERIC_FAILURE;
        switch (code) {
            case 801:
                error = CommandException.Error.REQUEST_NOT_SUPPORTED;
                break;
            case 821:
                error = CommandException.Error.PASSWORD_INCORRECT;
                break;
        }
        return new CommandException(error);
    }

    CommandException getCommandException(Throwable e) {
        if (e instanceof ImsException) {
            CommandException ex = getCommandException(((ImsException) e).getCode());
            return ex;
        }
        Rlog.d(LOG_TAG, "getCommandException generic failure");
        CommandException ex2 = new CommandException(CommandException.Error.GENERIC_FAILURE);
        return ex2;
    }

    private void onNetworkInitiatedUssd(ImsPhoneMmiCode mmi) {
        Rlog.d(LOG_TAG, "onNetworkInitiatedUssd");
        this.mMmiCompleteRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmi, (Throwable) null));
    }

    void onIncomingUSSD(int ussdMode, String ussdMessage) {
        Rlog.d(LOG_TAG, "onIncomingUSSD ussdMode=" + ussdMode);
        boolean isUssdRequest = ussdMode == 1;
        boolean isUssdError = (ussdMode == 0 || ussdMode == 1) ? false : true;
        ImsPhoneMmiCode found = null;
        int i = 0;
        int s = this.mPendingMMIs.size();
        while (true) {
            if (i >= s) {
                break;
            }
            if (!this.mPendingMMIs.get(i).isPendingUSSD()) {
                i++;
            } else {
                ImsPhoneMmiCode found2 = this.mPendingMMIs.get(i);
                found = found2;
                break;
            }
        }
        if (found != null) {
            if (isUssdError) {
                found.onUssdFinishedError();
                return;
            } else {
                found.onUssdFinished(ussdMessage, isUssdRequest);
                return;
            }
        }
        if (!isUssdError && ussdMessage != null) {
            ImsPhoneMmiCode mmi = ImsPhoneMmiCode.newNetworkInitiatedUssd(ussdMessage, isUssdRequest, this);
            onNetworkInitiatedUssd(mmi);
        }
    }

    void onMMIDone(ImsPhoneMmiCode mmi) {
        if (this.mPendingMMIs.remove(mmi) || mmi.isUssdRequest()) {
            this.mMmiCompleteRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmi, (Throwable) null));
        }
    }

    public ArrayList<Connection> getHandoverConnection() {
        ArrayList<Connection> connList = new ArrayList<>();
        connList.addAll(getForegroundCall().mConnections);
        connList.addAll(getBackgroundCall().mConnections);
        connList.addAll(getRingingCall().mConnections);
        if (connList.size() > 0) {
            return connList;
        }
        return null;
    }

    public void notifySrvccState(Call.SrvccState state) {
        this.mCT.notifySrvccState(state);
    }

    void initiateSilentRedial() {
        String result = this.mLastDialString;
        AsyncResult ar = new AsyncResult((Object) null, result, (Throwable) null);
        if (ar != null) {
            this.mSilentRedialRegistrants.notifyRegistrants(ar);
        }
    }

    public void registerForSilentRedial(Handler h, int what, Object obj) {
        this.mSilentRedialRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForSilentRedial(Handler h) {
        this.mSilentRedialRegistrants.remove(h);
    }

    @Override
    public int getSubId() {
        return this.mDefaultPhone.getSubId();
    }

    @Override
    public int getPhoneId() {
        return this.mDefaultPhone.getPhoneId();
    }

    private IccRecords getIccRecords() {
        return this.mDefaultPhone.mIccRecords.get();
    }

    private CallForwardInfo getCallForwardInfo(ImsCallForwardInfo info) {
        CallForwardInfo cfInfo = new CallForwardInfo();
        cfInfo.status = info.mStatus;
        cfInfo.reason = getCFReasonFromCondition(info.mCondition);
        cfInfo.serviceClass = 1;
        cfInfo.toa = info.mToA;
        cfInfo.number = info.mNumber;
        cfInfo.timeSeconds = info.mTimeSeconds;
        return cfInfo;
    }

    private CallForwardInfo[] handleCfQueryResult(ImsCallForwardInfo[] infos) {
        CallForwardInfo[] cfInfos = null;
        if (infos != null && infos.length != 0) {
            cfInfos = new CallForwardInfo[infos.length];
        }
        IccRecords r = getIccRecords();
        if (infos == null || infos.length == 0) {
            if (r != null) {
                r.setVoiceCallForwardingFlag(1, false, null);
            }
        } else {
            int s = infos.length;
            for (int i = 0; i < s; i++) {
                if (infos[i].mCondition == 0 && r != null) {
                    r.setVoiceCallForwardingFlag(1, infos[i].mStatus == 1, infos[i].mNumber);
                }
                cfInfos[i] = getCallForwardInfo(infos[i]);
            }
        }
        return cfInfos;
    }

    private int[] handleCbQueryResult(ImsSsInfo[] infos) {
        int[] cbInfos = {0};
        if (infos[0].mStatus == 1) {
            cbInfos[0] = 1;
        }
        return cbInfos;
    }

    private int[] handleCwQueryResult(ImsSsInfo[] infos) {
        int[] cwInfos = {0, 0};
        if (infos[0].mStatus == 1) {
            cwInfos[0] = 1;
            cwInfos[1] = 1;
        }
        return cwInfos;
    }

    private void sendResponse(Message onComplete, Object result, Throwable e) {
        if (onComplete != null) {
            if (e != null) {
                if (e instanceof ImsException) {
                    ImsException imsEx = (ImsException) e;
                    AsyncResult.forMessage(onComplete, result, imsEx);
                } else {
                    CommandException ex = getCommandException(e);
                    AsyncResult.forMessage(onComplete, result, ex);
                }
            } else {
                AsyncResult.forMessage(onComplete, result, (Throwable) null);
            }
            onComplete.sendToTarget();
        }
    }

    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar = (AsyncResult) msg.obj;
        Rlog.d(LOG_TAG, "handleMessage what=" + msg.what);
        switch (msg.what) {
            case 12:
                IccRecords r = getIccRecords();
                Cf cf = (Cf) ar.userObj;
                if (cf.mIsCfu && ar.exception == null && r != null) {
                    r.setVoiceCallForwardingFlag(1, msg.arg1 == 1, cf.mSetCfNumber);
                }
                sendResponse(cf.mOnComplete, null, ar.exception);
                break;
            case 13:
                CallForwardInfo[] cfInfos = null;
                if (ar.exception == null) {
                    cfInfos = handleCfQueryResult((ImsCallForwardInfo[]) ar.result);
                }
                sendResponse((Message) ar.userObj, cfInfos, ar.exception);
                break;
            case 37:
            case 39:
                sendResponse((Message) ar.userObj, null, ar.exception);
                break;
            case 38:
            case 40:
                int[] ssInfos = null;
                if (ar.exception == null) {
                    if (msg.what == 38) {
                        ssInfos = handleCbQueryResult((ImsSsInfo[]) ar.result);
                    } else if (msg.what == 40) {
                        ssInfos = handleCwQueryResult((ImsSsInfo[]) ar.result);
                    }
                }
                sendResponse((Message) ar.userObj, ssInfos, ar.exception);
                break;
            default:
                super.handleMessage(msg);
                break;
        }
    }

    @Override
    public boolean isInEmergencyCall() {
        return this.mCT.isInEmergencyCall();
    }

    @Override
    public boolean isInEcm() {
        return this.mIsPhoneInEcmState;
    }

    void sendEmergencyCallbackModeChange() {
        Intent intent = new Intent("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        intent.putExtra("phoneinECMState", this.mIsPhoneInEcmState);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, getPhoneId());
        ActivityManagerNative.broadcastStickyIntent(intent, (String) null, -1);
        Rlog.d(LOG_TAG, "sendEmergencyCallbackModeChange");
    }

    @Override
    public void exitEmergencyCallbackMode() {
        if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        Rlog.d(LOG_TAG, "exitEmergencyCallbackMode()");
        try {
            ImsEcbm ecbm = this.mCT.getEcbmInterface();
            ecbm.exitEmergencyCallbackMode();
        } catch (ImsException e) {
            e.printStackTrace();
        }
    }

    private void handleEnterEmergencyCallbackMode() {
        Rlog.d(LOG_TAG, "handleEnterEmergencyCallbackMode,mIsPhoneInEcmState= " + this.mIsPhoneInEcmState);
        if (!this.mIsPhoneInEcmState) {
            this.mIsPhoneInEcmState = true;
            sendEmergencyCallbackModeChange();
            setSystemProperty("ril.cdma.inecmmode", "true");
            long delayInMillis = SystemProperties.getLong("ro.cdma.ecmexittimer", 300000L);
            postDelayed(this.mExitEcmRunnable, delayInMillis);
            this.mWakeLock.acquire();
        }
    }

    private void handleExitEmergencyCallbackMode() {
        Rlog.d(LOG_TAG, "handleExitEmergencyCallbackMode: mIsPhoneInEcmState = " + this.mIsPhoneInEcmState);
        removeCallbacks(this.mExitEcmRunnable);
        if (this.mEcmExitRespRegistrant != null) {
            this.mEcmExitRespRegistrant.notifyResult(Boolean.TRUE);
        }
        if (this.mIsPhoneInEcmState) {
            this.mIsPhoneInEcmState = false;
            setSystemProperty("ril.cdma.inecmmode", "false");
        }
        sendEmergencyCallbackModeChange();
    }

    void handleTimerInEmergencyCallbackMode(int action) {
        switch (action) {
            case 0:
                long delayInMillis = SystemProperties.getLong("ro.cdma.ecmexittimer", 300000L);
                postDelayed(this.mExitEcmRunnable, delayInMillis);
                if (this.mDefaultPhone.getPhoneType() == 1) {
                    ((GSMPhone) this.mDefaultPhone).notifyEcbmTimerReset(Boolean.FALSE);
                } else {
                    ((CDMAPhone) this.mDefaultPhone).notifyEcbmTimerReset(Boolean.FALSE);
                }
                break;
            case 1:
                removeCallbacks(this.mExitEcmRunnable);
                if (this.mDefaultPhone.getPhoneType() == 1) {
                    ((GSMPhone) this.mDefaultPhone).notifyEcbmTimerReset(Boolean.TRUE);
                } else {
                    ((CDMAPhone) this.mDefaultPhone).notifyEcbmTimerReset(Boolean.TRUE);
                }
                break;
            default:
                Rlog.e(LOG_TAG, "handleTimerInEmergencyCallbackMode, unsupported action " + action);
                break;
        }
    }

    @Override
    public void setOnEcbModeExitResponse(Handler h, int what, Object obj) {
        this.mEcmExitRespRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unsetOnEcbModeExitResponse(Handler h) {
        this.mEcmExitRespRegistrant.clear();
    }

    public boolean isVolteEnabled() {
        return this.mCT.isVolteEnabled();
    }

    public boolean isVtEnabled() {
        return this.mCT.isVtEnabled();
    }

    public Phone getDefaultPhone() {
        return this.mDefaultPhone;
    }

    @Override
    public boolean isImsRegistered() {
        return this.mImsRegistered;
    }

    public void setImsRegistered(boolean value) {
        this.mImsRegistered = value;
    }

    public void callEndCleanupHandOverCallIfAny() {
        this.mCT.callEndCleanupHandOverCallIfAny();
    }

    public void registerForImsStateChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mImsStateChangedRegistrants.add(r);
    }

    public void unregisterForImsStateChanged(Handler h) {
        this.mImsStateChangedRegistrants.remove(h);
    }

    public void notifyImsStateChanged() {
        this.mImsStateChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        this.mDefaultPhone.notifyServiceStateChangedP(this.mDefaultPhone.getServiceState());
    }

    @Override
    public void turnOnIms() {
        try {
            this.mCT.turnOnIms();
        } catch (ImsException e) {
        }
    }

    @Override
    public void turnOffIms() {
        try {
            this.mCT.turnOffIms();
        } catch (ImsException e) {
        }
    }
}
