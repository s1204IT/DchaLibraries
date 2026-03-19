package com.android.internal.telephony.test;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.SystemClock;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.Rlog;
import android.telephony.SignalStrength;
import com.android.internal.telephony.BaseCommands;
import com.android.internal.telephony.CallFailCause;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.LastCallFailCause;
import com.android.internal.telephony.RadioCapability;
import com.android.internal.telephony.SmsResponse;
import com.android.internal.telephony.UUSInfo;
import com.android.internal.telephony.cdma.CdmaSmsBroadcastConfigInfo;
import com.android.internal.telephony.dataconnection.DataCallResponse;
import com.android.internal.telephony.dataconnection.DataProfile;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccIoResult;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulatedCommands extends BaseCommands implements CommandsInterface, SimulatedRadioControl {
    public static final int DEFAULT_PIN1_ATTEMPT = 5;
    public static final int DEFAULT_PIN2_ATTEMPT = 5;
    public static final String DEFAULT_SIM_PIN2_CODE = "5678";
    public static final String DEFAULT_SIM_PIN_CODE = "1234";
    public static final String FAKE_ESN = "1234";
    public static final String FAKE_IMEI = "012345678901234";
    public static final String FAKE_IMEISV = "99";
    public static final String FAKE_LONG_NAME = "Fake long name";
    public static final String FAKE_MCC_MNC = "310260";
    public static final String FAKE_MEID = "1234";
    public static final String FAKE_SHORT_NAME = "Fake short name";
    private static final String LOG_TAG = "SimulatedCommands";
    private static final String SIM_PUK2_CODE = "87654321";
    private static final String SIM_PUK_CODE = "12345678";
    private final AtomicInteger getNetworkSelectionModeCallCount;
    private AtomicBoolean mAllowed;
    private List<CellInfo> mCellInfoList;
    private int mChannelId;
    private int mDataRadioTech;
    private int mDataRegState;
    private DataCallResponse mDcResponse;
    private boolean mDcSuccess;
    private final AtomicInteger mGetDataRegistrationStateCallCount;
    private final AtomicInteger mGetOperatorCallCount;
    private final AtomicInteger mGetVoiceRegistrationStateCallCount;
    HandlerThread mHandlerThread;
    private IccCardStatus mIccCardStatus;
    private IccIoResult mIccIoResultForApduLogicalChannel;
    private String mImei;
    private String mImeiSv;
    private int[] mImsRegState;
    int mNetworkType;
    int mNextCallFailCause;
    int mPausedResponseCount;
    ArrayList<Message> mPausedResponses;
    int mPin1attemptsRemaining;
    String mPin2Code;
    int mPin2UnlockAttempts;
    String mPinCode;
    int mPinUnlockAttempts;
    int mPuk2UnlockAttempts;
    int mPukUnlockAttempts;
    private SignalStrength mSignalStrength;
    boolean mSimFdnEnabled;
    SimFdnState mSimFdnEnabledState;
    boolean mSimLockEnabled;
    SimLockState mSimLockedState;
    boolean mSsnNotifyOn;
    private int mVoiceRadioTech;
    private int mVoiceRegState;
    SimulatedGsmCallState simulatedCallState;
    private static final SimLockState INITIAL_LOCK_STATE = SimLockState.NONE;
    private static final SimFdnState INITIAL_FDN_STATE = SimFdnState.NONE;

    private enum SimLockState {
        NONE,
        REQUIRE_PIN,
        REQUIRE_PUK,
        SIM_PERM_LOCKED;

        public static SimLockState[] valuesCustom() {
            return values();
        }
    }

    private enum SimFdnState {
        NONE,
        REQUIRE_PIN2,
        REQUIRE_PUK2,
        SIM_PERM_LOCKED;

        public static SimFdnState[] valuesCustom() {
            return values();
        }
    }

    public SimulatedCommands() {
        super(null);
        this.mPin1attemptsRemaining = 5;
        this.mSsnNotifyOn = false;
        this.mVoiceRegState = 1;
        this.mVoiceRadioTech = 3;
        this.mDataRegState = 1;
        this.mDataRadioTech = 3;
        this.mChannelId = -1;
        this.mPausedResponses = new ArrayList<>();
        this.mNextCallFailCause = 16;
        this.mDcSuccess = true;
        this.mGetVoiceRegistrationStateCallCount = new AtomicInteger(0);
        this.mGetDataRegistrationStateCallCount = new AtomicInteger(0);
        this.mGetOperatorCallCount = new AtomicInteger(0);
        this.getNetworkSelectionModeCallCount = new AtomicInteger(0);
        this.mAllowed = new AtomicBoolean(false);
        this.mHandlerThread = new HandlerThread(LOG_TAG);
        this.mHandlerThread.start();
        Looper looper = this.mHandlerThread.getLooper();
        this.simulatedCallState = new SimulatedGsmCallState(looper);
        setRadioState(CommandsInterface.RadioState.RADIO_ON);
        this.mSimLockedState = INITIAL_LOCK_STATE;
        this.mSimLockEnabled = this.mSimLockedState != SimLockState.NONE;
        this.mPinCode = "1234";
        this.mSimFdnEnabledState = INITIAL_FDN_STATE;
        this.mSimFdnEnabled = this.mSimFdnEnabledState != SimFdnState.NONE;
        this.mPin2Code = DEFAULT_SIM_PIN2_CODE;
    }

    private void log(String str) {
        Rlog.d(LOG_TAG, str);
    }

    @Override
    public void getIccCardStatus(Message result) {
        if (this.mIccCardStatus != null) {
            resultSuccess(result, this.mIccCardStatus);
        } else {
            resultFail(result, null, new RuntimeException("IccCardStatus not set"));
        }
    }

    @Override
    public void supplyIccPin(String pin, Message result) {
        if (this.mSimLockedState != SimLockState.REQUIRE_PIN) {
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPin: wrong state, state=" + this.mSimLockedState);
            CommandException ex = new CommandException(CommandException.Error.PASSWORD_INCORRECT);
            resultFail(result, null, ex);
            return;
        }
        if (pin != null && pin.equals(this.mPinCode)) {
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPin: success!");
            this.mPinUnlockAttempts = 0;
            this.mSimLockedState = SimLockState.NONE;
            this.mIccStatusChangedRegistrants.notifyRegistrants();
            resultSuccess(result, null);
            return;
        }
        if (result == null) {
            return;
        }
        this.mPinUnlockAttempts++;
        Rlog.i(LOG_TAG, "[SimCmd] supplyIccPin: failed! attempt=" + this.mPinUnlockAttempts);
        if (this.mPinUnlockAttempts >= 5) {
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPin: set state to REQUIRE_PUK");
            this.mSimLockedState = SimLockState.REQUIRE_PUK;
        }
        CommandException ex2 = new CommandException(CommandException.Error.PASSWORD_INCORRECT);
        resultFail(result, null, ex2);
    }

    @Override
    public void supplyIccPuk(String puk, String newPin, Message result) {
        if (this.mSimLockedState != SimLockState.REQUIRE_PUK) {
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPuk: wrong state, state=" + this.mSimLockedState);
            CommandException ex = new CommandException(CommandException.Error.PASSWORD_INCORRECT);
            resultFail(result, null, ex);
            return;
        }
        if (puk != null && puk.equals(SIM_PUK_CODE)) {
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPuk: success!");
            this.mSimLockedState = SimLockState.NONE;
            this.mPukUnlockAttempts = 0;
            this.mIccStatusChangedRegistrants.notifyRegistrants();
            resultSuccess(result, null);
            return;
        }
        if (result == null) {
            return;
        }
        this.mPukUnlockAttempts++;
        Rlog.i(LOG_TAG, "[SimCmd] supplyIccPuk: failed! attempt=" + this.mPukUnlockAttempts);
        if (this.mPukUnlockAttempts >= 10) {
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPuk: set state to SIM_PERM_LOCKED");
            this.mSimLockedState = SimLockState.SIM_PERM_LOCKED;
        }
        CommandException ex2 = new CommandException(CommandException.Error.PASSWORD_INCORRECT);
        resultFail(result, null, ex2);
    }

    @Override
    public void supplyIccPin2(String pin2, Message result) {
        if (this.mSimFdnEnabledState != SimFdnState.REQUIRE_PIN2) {
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPin2: wrong state, state=" + this.mSimFdnEnabledState);
            CommandException ex = new CommandException(CommandException.Error.PASSWORD_INCORRECT);
            resultFail(result, null, ex);
            return;
        }
        if (pin2 != null && pin2.equals(this.mPin2Code)) {
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPin2: success!");
            this.mPin2UnlockAttempts = 0;
            this.mSimFdnEnabledState = SimFdnState.NONE;
            resultSuccess(result, null);
            return;
        }
        if (result == null) {
            return;
        }
        this.mPin2UnlockAttempts++;
        Rlog.i(LOG_TAG, "[SimCmd] supplyIccPin2: failed! attempt=" + this.mPin2UnlockAttempts);
        if (this.mPin2UnlockAttempts >= 5) {
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPin2: set state to REQUIRE_PUK2");
            this.mSimFdnEnabledState = SimFdnState.REQUIRE_PUK2;
        }
        CommandException ex2 = new CommandException(CommandException.Error.PASSWORD_INCORRECT);
        resultFail(result, null, ex2);
    }

    @Override
    public void supplyIccPuk2(String puk2, String newPin2, Message result) {
        if (this.mSimFdnEnabledState != SimFdnState.REQUIRE_PUK2) {
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPuk2: wrong state, state=" + this.mSimLockedState);
            CommandException ex = new CommandException(CommandException.Error.PASSWORD_INCORRECT);
            resultFail(result, null, ex);
            return;
        }
        if (puk2 != null && puk2.equals(SIM_PUK2_CODE)) {
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPuk2: success!");
            this.mSimFdnEnabledState = SimFdnState.NONE;
            this.mPuk2UnlockAttempts = 0;
            resultSuccess(result, null);
            return;
        }
        if (result == null) {
            return;
        }
        this.mPuk2UnlockAttempts++;
        Rlog.i(LOG_TAG, "[SimCmd] supplyIccPuk2: failed! attempt=" + this.mPuk2UnlockAttempts);
        if (this.mPuk2UnlockAttempts >= 10) {
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPuk2: set state to SIM_PERM_LOCKED");
            this.mSimFdnEnabledState = SimFdnState.SIM_PERM_LOCKED;
        }
        CommandException ex2 = new CommandException(CommandException.Error.PASSWORD_INCORRECT);
        resultFail(result, null, ex2);
    }

    @Override
    public void changeIccPin(String oldPin, String newPin, Message result) {
        if (oldPin != null && oldPin.equals(this.mPinCode)) {
            this.mPinCode = newPin;
            resultSuccess(result, null);
        } else {
            Rlog.i(LOG_TAG, "[SimCmd] changeIccPin: pin failed!");
            CommandException ex = new CommandException(CommandException.Error.PASSWORD_INCORRECT);
            resultFail(result, null, ex);
        }
    }

    @Override
    public void changeIccPin2(String oldPin2, String newPin2, Message result) {
        if (oldPin2 != null && oldPin2.equals(this.mPin2Code)) {
            this.mPin2Code = newPin2;
            resultSuccess(result, null);
        } else {
            Rlog.i(LOG_TAG, "[SimCmd] changeIccPin2: pin2 failed!");
            CommandException ex = new CommandException(CommandException.Error.PASSWORD_INCORRECT);
            resultFail(result, null, ex);
        }
    }

    @Override
    public void changeBarringPassword(String facility, String oldPwd, String newPwd, Message result) {
        unimplemented(result);
    }

    @Override
    public void setSuppServiceNotifications(boolean enable, Message result) {
        resultSuccess(result, null);
        if (enable && this.mSsnNotifyOn) {
            Rlog.w(LOG_TAG, "Supp Service Notifications already enabled!");
        }
        this.mSsnNotifyOn = enable;
    }

    @Override
    public void queryFacilityLock(String facility, String pin, int serviceClass, Message result) {
        queryFacilityLockForApp(facility, pin, serviceClass, null, result);
    }

    @Override
    public void queryFacilityLockForApp(String facility, String pin, int serviceClass, String appId, Message result) {
        if (facility != null && facility.equals(CommandsInterface.CB_FACILITY_BA_SIM)) {
            if (result != null) {
                int[] r = new int[1];
                r[0] = this.mSimLockEnabled ? 1 : 0;
                Rlog.i(LOG_TAG, "[SimCmd] queryFacilityLock: SIM is " + (r[0] == 0 ? "unlocked" : "locked"));
                resultSuccess(result, r);
                return;
            }
            return;
        }
        if (facility != null && facility.equals(CommandsInterface.CB_FACILITY_BA_FD)) {
            if (result != null) {
                int[] r2 = new int[1];
                r2[0] = this.mSimFdnEnabled ? 1 : 0;
                Rlog.i(LOG_TAG, "[SimCmd] queryFacilityLock: FDN is " + (r2[0] == 0 ? "disabled" : "enabled"));
                resultSuccess(result, r2);
                return;
            }
            return;
        }
        unimplemented(result);
    }

    @Override
    public void setFacilityLock(String facility, boolean lockEnabled, String pin, int serviceClass, Message result) {
        setFacilityLockForApp(facility, lockEnabled, pin, serviceClass, null, result);
    }

    @Override
    public void setFacilityLockForApp(String facility, boolean lockEnabled, String pin, int serviceClass, String appId, Message result) {
        if (facility != null && facility.equals(CommandsInterface.CB_FACILITY_BA_SIM)) {
            if (pin != null && pin.equals(this.mPinCode)) {
                Rlog.i(LOG_TAG, "[SimCmd] setFacilityLock: pin is valid");
                this.mSimLockEnabled = lockEnabled;
                resultSuccess(result, null);
                return;
            } else {
                Rlog.i(LOG_TAG, "[SimCmd] setFacilityLock: pin failed!");
                CommandException ex = new CommandException(CommandException.Error.GENERIC_FAILURE);
                resultFail(result, null, ex);
                return;
            }
        }
        if (facility != null && facility.equals(CommandsInterface.CB_FACILITY_BA_FD)) {
            if (pin != null && pin.equals(this.mPin2Code)) {
                Rlog.i(LOG_TAG, "[SimCmd] setFacilityLock: pin2 is valid");
                this.mSimFdnEnabled = lockEnabled;
                resultSuccess(result, null);
                return;
            } else {
                Rlog.i(LOG_TAG, "[SimCmd] setFacilityLock: pin2 failed!");
                CommandException ex2 = new CommandException(CommandException.Error.GENERIC_FAILURE);
                resultFail(result, null, ex2);
                return;
            }
        }
        unimplemented(result);
    }

    @Override
    public void supplyNetworkDepersonalization(String netpin, Message result) {
        unimplemented(result);
    }

    @Override
    public void getCurrentCalls(Message result) {
        SimulatedCommandsVerifier.getInstance().getCurrentCalls(result);
        if (this.mState == CommandsInterface.RadioState.RADIO_ON && !isSimLocked()) {
            resultSuccess(result, this.simulatedCallState.getDriverCalls());
        } else {
            resultFail(result, null, new CommandException(CommandException.Error.RADIO_NOT_AVAILABLE));
        }
    }

    @Override
    @Deprecated
    public void getPDPContextList(Message result) {
        getDataCallList(result);
    }

    @Override
    public void getDataCallList(Message result) {
        resultSuccess(result, new ArrayList(0));
    }

    @Override
    public void dial(String address, int clirMode, Message result) {
        SimulatedCommandsVerifier.getInstance().dial(address, clirMode, result);
        this.simulatedCallState.onDial(address);
        resultSuccess(result, null);
    }

    @Override
    public void dial(String address, int clirMode, UUSInfo uusInfo, Message result) {
        SimulatedCommandsVerifier.getInstance().dial(address, clirMode, uusInfo, result);
        this.simulatedCallState.onDial(address);
        resultSuccess(result, null);
    }

    @Override
    public void getIMSI(Message result) {
        getIMSIForApp(null, result);
    }

    @Override
    public void getIMSIForApp(String aid, Message result) {
        resultSuccess(result, FAKE_IMEI);
    }

    public void setIMEI(String imei) {
        this.mImei = imei;
    }

    @Override
    public void getIMEI(Message result) {
        SimulatedCommandsVerifier.getInstance().getIMEI(result);
        resultSuccess(result, this.mImei != null ? this.mImei : FAKE_IMEI);
    }

    public void setIMEISV(String imeisv) {
        this.mImeiSv = imeisv;
    }

    @Override
    public void getIMEISV(Message result) {
        SimulatedCommandsVerifier.getInstance().getIMEISV(result);
        resultSuccess(result, this.mImeiSv != null ? this.mImeiSv : FAKE_IMEISV);
    }

    @Override
    public void hangupConnection(int gsmIndex, Message result) {
        boolean success = this.simulatedCallState.onChld('1', (char) (gsmIndex + 48));
        if (!success) {
            Rlog.i("GSM", "[SimCmd] hangupConnection: resultFail");
            resultFail(result, null, new RuntimeException("Hangup Error"));
        } else {
            Rlog.i("GSM", "[SimCmd] hangupConnection: resultSuccess");
            resultSuccess(result, null);
        }
    }

    @Override
    public void hangupWaitingOrBackground(Message result) {
        boolean success = this.simulatedCallState.onChld('0', (char) 0);
        if (!success) {
            resultFail(result, null, new RuntimeException("Hangup Error"));
        } else {
            resultSuccess(result, null);
        }
    }

    @Override
    public void hangupForegroundResumeBackground(Message result) {
        boolean success = this.simulatedCallState.onChld('1', (char) 0);
        if (!success) {
            resultFail(result, null, new RuntimeException("Hangup Error"));
        } else {
            resultSuccess(result, null);
        }
    }

    @Override
    public void switchWaitingOrHoldingAndActive(Message result) {
        boolean success = this.simulatedCallState.onChld('2', (char) 0);
        if (!success) {
            resultFail(result, null, new RuntimeException("Hangup Error"));
        } else {
            resultSuccess(result, null);
        }
    }

    @Override
    public void conference(Message result) {
        boolean success = this.simulatedCallState.onChld('3', (char) 0);
        if (!success) {
            resultFail(result, null, new RuntimeException("Hangup Error"));
        } else {
            resultSuccess(result, null);
        }
    }

    @Override
    public void explicitCallTransfer(Message result) {
        boolean success = this.simulatedCallState.onChld('4', (char) 0);
        if (!success) {
            resultFail(result, null, new RuntimeException("Hangup Error"));
        } else {
            resultSuccess(result, null);
        }
    }

    @Override
    public void separateConnection(int gsmIndex, Message result) {
        char ch = (char) (gsmIndex + 48);
        boolean success = this.simulatedCallState.onChld('2', ch);
        if (!success) {
            resultFail(result, null, new RuntimeException("Hangup Error"));
        } else {
            resultSuccess(result, null);
        }
    }

    @Override
    public void acceptCall(Message result) {
        SimulatedCommandsVerifier.getInstance().acceptCall(result);
        boolean success = this.simulatedCallState.onAnswer();
        if (!success) {
            resultFail(result, null, new RuntimeException("Hangup Error"));
        } else {
            resultSuccess(result, null);
        }
    }

    @Override
    public void rejectCall(Message result) {
        boolean success = this.simulatedCallState.onChld('0', (char) 0);
        if (!success) {
            resultFail(result, null, new RuntimeException("Hangup Error"));
        } else {
            resultSuccess(result, null);
        }
    }

    @Override
    public void getLastCallFailCause(Message result) {
        LastCallFailCause mFailCause = new LastCallFailCause();
        mFailCause.causeCode = this.mNextCallFailCause;
        resultSuccess(result, mFailCause);
    }

    @Override
    @Deprecated
    public void getLastPdpFailCause(Message result) {
        unimplemented(result);
    }

    @Override
    public void getLastDataCallFailCause(Message result) {
        unimplemented(result);
    }

    @Override
    public void setMute(boolean enableMute, Message result) {
        unimplemented(result);
    }

    @Override
    public void getMute(Message result) {
        unimplemented(result);
    }

    public void setSignalStrength(SignalStrength signalStrength) {
        this.mSignalStrength = signalStrength;
    }

    @Override
    public void getSignalStrength(Message result) {
        if (this.mSignalStrength == null) {
            this.mSignalStrength = new SignalStrength(20, 0, -1, -1, -1, -1, -1, 99, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
        }
        resultSuccess(result, this.mSignalStrength);
    }

    @Override
    public void setBandMode(int bandMode, Message result) {
        resultSuccess(result, null);
    }

    @Override
    public void queryAvailableBandMode(Message result) {
        int[] ret = {4, 2, 3, 4};
        resultSuccess(result, ret);
    }

    @Override
    public void sendTerminalResponse(String contents, Message response) {
        resultSuccess(response, null);
    }

    @Override
    public void sendEnvelope(String contents, Message response) {
        resultSuccess(response, null);
    }

    @Override
    public void sendEnvelopeWithStatus(String contents, Message response) {
        resultSuccess(response, null);
    }

    @Override
    public void handleCallSetupRequestFromSim(boolean accept, int resCode, Message response) {
        resultSuccess(response, null);
    }

    public void setVoiceRadioTech(int voiceRadioTech) {
        this.mVoiceRadioTech = voiceRadioTech;
    }

    public void setVoiceRegState(int voiceRegState) {
        this.mVoiceRegState = voiceRegState;
    }

    @Override
    public void getVoiceRegistrationState(Message result) {
        this.mGetVoiceRegistrationStateCallCount.incrementAndGet();
        String[] ret = new String[14];
        ret[0] = Integer.toString(this.mVoiceRegState);
        ret[3] = Integer.toString(this.mVoiceRadioTech);
        resultSuccess(result, ret);
    }

    public int getGetVoiceRegistrationStateCallCount() {
        return this.mGetVoiceRegistrationStateCallCount.get();
    }

    public void setDataRadioTech(int radioTech) {
        this.mDataRadioTech = radioTech;
    }

    public void setDataRegState(int dataRegState) {
        this.mDataRegState = dataRegState;
    }

    @Override
    public void getDataRegistrationState(Message result) {
        this.mGetDataRegistrationStateCallCount.incrementAndGet();
        String[] ret = new String[11];
        ret[0] = Integer.toString(this.mDataRegState);
        ret[3] = Integer.toString(this.mDataRadioTech);
        resultSuccess(result, ret);
    }

    public int getGetDataRegistrationStateCallCount() {
        return this.mGetDataRegistrationStateCallCount.get();
    }

    @Override
    public void getOperator(Message result) {
        this.mGetOperatorCallCount.incrementAndGet();
        String[] ret = {FAKE_LONG_NAME, FAKE_SHORT_NAME, FAKE_MCC_MNC};
        resultSuccess(result, ret);
    }

    public int getGetOperatorCallCount() {
        this.mGetOperatorCallCount.get();
        return this.mGetOperatorCallCount.get();
    }

    @Override
    public void sendDtmf(char c, Message result) {
        resultSuccess(result, null);
    }

    @Override
    public void startDtmf(char c, Message result) {
        resultSuccess(result, null);
    }

    @Override
    public void stopDtmf(Message result) {
        resultSuccess(result, null);
    }

    @Override
    public void sendBurstDtmf(String dtmfString, int on, int off, Message result) {
        SimulatedCommandsVerifier.getInstance().sendBurstDtmf(dtmfString, on, off, result);
        resultSuccess(result, null);
    }

    @Override
    public void sendSMS(String smscPDU, String pdu, Message result) {
        SimulatedCommandsVerifier.getInstance().sendSMS(smscPDU, pdu, result);
        resultSuccess(result, new SmsResponse(0, null, 0));
    }

    @Override
    public void sendSMSExpectMore(String smscPDU, String pdu, Message result) {
        unimplemented(result);
    }

    @Override
    public void deleteSmsOnSim(int index, Message response) {
        Rlog.d(LOG_TAG, "Delete message at index " + index);
        unimplemented(response);
    }

    @Override
    public void deleteSmsOnRuim(int index, Message response) {
        Rlog.d(LOG_TAG, "Delete RUIM message at index " + index);
        unimplemented(response);
    }

    @Override
    public void writeSmsToSim(int status, String smsc, String pdu, Message response) {
        Rlog.d(LOG_TAG, "Write SMS to SIM with status " + status);
        unimplemented(response);
    }

    @Override
    public void writeSmsToRuim(int status, String pdu, Message response) {
        Rlog.d(LOG_TAG, "Write SMS to RUIM with status " + status);
        unimplemented(response);
    }

    public void setDataCallResponse(boolean success, DataCallResponse dcResponse) {
        this.mDcResponse = dcResponse;
        this.mDcSuccess = success;
    }

    public void triggerNITZupdate(String NITZStr) {
        if (NITZStr == null) {
            return;
        }
        this.mNITZTimeRegistrant.notifyRegistrant(new AsyncResult((Object) null, new Object[]{NITZStr, Long.valueOf(SystemClock.elapsedRealtime())}, (Throwable) null));
    }

    @Override
    public void setupDataCall(int radioTechnology, int profile, String apn, String user, String password, int authType, String protocol, Message result) {
        SimulatedCommandsVerifier.getInstance().setupDataCall(radioTechnology, profile, apn, user, password, authType, protocol, result);
        if (this.mDcResponse == null) {
            this.mDcResponse = new DataCallResponse();
            this.mDcResponse.version = 11;
            this.mDcResponse.status = 0;
            this.mDcResponse.suggestedRetryTime = -1;
            this.mDcResponse.cid = 1;
            this.mDcResponse.active = 2;
            this.mDcResponse.type = "IP";
            this.mDcResponse.ifname = "rmnet_data7";
            this.mDcResponse.mtu = 1440;
            this.mDcResponse.addresses = new String[]{"12.34.56.78"};
            this.mDcResponse.dnses = new String[]{"98.76.54.32"};
            this.mDcResponse.gateways = new String[]{"11.22.33.44"};
            this.mDcResponse.pcscf = new String[0];
        }
        if (this.mDcSuccess) {
            resultSuccess(result, this.mDcResponse);
        } else {
            resultFail(result, this.mDcResponse, new RuntimeException("Setup data call failed!"));
        }
    }

    @Override
    public void deactivateDataCall(int cid, int reason, Message result) {
        SimulatedCommandsVerifier.getInstance().deactivateDataCall(cid, reason, result);
        resultSuccess(result, null);
    }

    @Override
    public void setPreferredNetworkType(int networkType, Message result) {
        SimulatedCommandsVerifier.getInstance().setPreferredNetworkType(networkType, result);
        this.mNetworkType = networkType;
        resultSuccess(result, null);
    }

    @Override
    public void getPreferredNetworkType(Message result) {
        SimulatedCommandsVerifier.getInstance().getPreferredNetworkType(result);
        int[] ret = {this.mNetworkType};
        resultSuccess(result, ret);
    }

    @Override
    public void getNeighboringCids(Message result) {
        int[] ret = new int[7];
        ret[0] = 6;
        for (int i = 1; i < 7; i++) {
            ret[i] = i;
        }
        resultSuccess(result, ret);
    }

    @Override
    public void setLocationUpdates(boolean enable, Message response) {
        SimulatedCommandsVerifier.getInstance().setLocationUpdates(enable, response);
        resultSuccess(response, null);
    }

    @Override
    public void getSmscAddress(Message result) {
        unimplemented(result);
    }

    @Override
    public void setSmscAddress(String address, Message result) {
        unimplemented(result);
    }

    @Override
    public void reportSmsMemoryStatus(boolean available, Message result) {
        resultSuccess(result, null);
        SimulatedCommandsVerifier.getInstance().reportSmsMemoryStatus(available, result);
    }

    @Override
    public void reportStkServiceIsRunning(Message result) {
        resultSuccess(result, null);
    }

    @Override
    public void getCdmaSubscriptionSource(Message result) {
        unimplemented(result);
    }

    private boolean isSimLocked() {
        if (this.mSimLockedState != SimLockState.NONE) {
            return true;
        }
        return false;
    }

    @Override
    public void setRadioPower(boolean on, Message result) {
        if (on) {
            setRadioState(CommandsInterface.RadioState.RADIO_ON);
        } else {
            setRadioState(CommandsInterface.RadioState.RADIO_OFF);
        }
    }

    @Override
    public void acknowledgeLastIncomingGsmSms(boolean success, int cause, Message result) {
        unimplemented(result);
        SimulatedCommandsVerifier.getInstance().acknowledgeLastIncomingGsmSms(success, cause, result);
    }

    @Override
    public void acknowledgeLastIncomingCdmaSms(boolean success, int cause, Message result) {
        unimplemented(result);
    }

    @Override
    public void acknowledgeIncomingGsmSmsWithPdu(boolean success, String ackPdu, Message result) {
        unimplemented(result);
    }

    @Override
    public void iccIO(int command, int fileid, String path, int p1, int p2, int p3, String data, String pin2, Message response) {
        iccIOForApp(command, fileid, path, p1, p2, p3, data, pin2, null, response);
    }

    @Override
    public void iccIOForApp(int command, int fileid, String path, int p1, int p2, int p3, String data, String pin2, String aid, Message result) {
        unimplemented(result);
    }

    @Override
    public void queryCLIP(Message response) {
        unimplemented(response);
    }

    @Override
    public void getCLIR(Message result) {
        unimplemented(result);
    }

    @Override
    public void setCLIR(int clirMode, Message result) {
        unimplemented(result);
    }

    @Override
    public void queryCallWaiting(int serviceClass, Message response) {
        unimplemented(response);
    }

    @Override
    public void setCallWaiting(boolean enable, int serviceClass, Message response) {
        unimplemented(response);
    }

    @Override
    public void setCallForward(int action, int cfReason, int serviceClass, String number, int timeSeconds, Message result) {
        SimulatedCommandsVerifier.getInstance().setCallForward(action, cfReason, serviceClass, number, timeSeconds, result);
        resultSuccess(result, null);
    }

    @Override
    public void queryCallForwardStatus(int cfReason, int serviceClass, String number, Message result) {
        SimulatedCommandsVerifier.getInstance().queryCallForwardStatus(cfReason, serviceClass, number, result);
        resultSuccess(result, null);
    }

    @Override
    public void setNetworkSelectionModeAutomatic(Message result) {
        unimplemented(result);
    }

    @Override
    public void exitEmergencyCallbackMode(Message result) {
        unimplemented(result);
    }

    @Override
    public void setNetworkSelectionModeManual(String operatorNumeric, Message result) {
        unimplemented(result);
    }

    @Override
    public void getNetworkSelectionMode(Message result) {
        SimulatedCommandsVerifier.getInstance().getNetworkSelectionMode(result);
        this.getNetworkSelectionModeCallCount.incrementAndGet();
        int[] ret = {0};
        resultSuccess(result, ret);
    }

    public int getGetNetworkSelectionModeCallCount() {
        return this.getNetworkSelectionModeCallCount.get();
    }

    @Override
    public void getAvailableNetworks(Message result) {
        unimplemented(result);
    }

    @Override
    public void getBasebandVersion(Message result) {
        SimulatedCommandsVerifier.getInstance().getBasebandVersion(result);
        resultSuccess(result, LOG_TAG);
    }

    public void triggerIncomingStkCcAlpha(String alphaString) {
        if (this.mCatCcAlphaRegistrant == null) {
            return;
        }
        this.mCatCcAlphaRegistrant.notifyResult(alphaString);
    }

    public void sendStkCcAplha(String alphaString) {
        triggerIncomingStkCcAlpha(alphaString);
    }

    @Override
    public void triggerIncomingUssd(String statusCode, String message) {
        if (this.mUSSDRegistrant == null) {
            return;
        }
        String[] result = {statusCode, message};
        this.mUSSDRegistrant.notifyResult(result);
    }

    @Override
    public void sendUSSD(String ussdString, Message result) {
        if (ussdString.equals("#646#")) {
            resultSuccess(result, null);
            triggerIncomingUssd("0", "You have NNN minutes remaining.");
        } else {
            resultSuccess(result, null);
            triggerIncomingUssd("0", "All Done");
        }
    }

    @Override
    public void cancelPendingUssd(Message response) {
        resultSuccess(response, null);
    }

    @Override
    public void resetRadio(Message result) {
        unimplemented(result);
    }

    @Override
    public void invokeOemRilRequestRaw(byte[] data, Message response) {
        if (response == null) {
            return;
        }
        AsyncResult.forMessage(response).result = data;
        response.sendToTarget();
    }

    @Override
    public void invokeOemRilRequestStrings(String[] strings, Message response) {
        if (response == null) {
            return;
        }
        AsyncResult.forMessage(response).result = strings;
        response.sendToTarget();
    }

    @Override
    public void triggerRing(String number) {
        this.simulatedCallState.triggerRing(number);
        this.mCallStateRegistrants.notifyRegistrants();
    }

    @Override
    public void progressConnectingCallState() {
        this.simulatedCallState.progressConnectingCallState();
        this.mCallStateRegistrants.notifyRegistrants();
    }

    @Override
    public void progressConnectingToActive() {
        this.simulatedCallState.progressConnectingToActive();
        this.mCallStateRegistrants.notifyRegistrants();
    }

    @Override
    public void setAutoProgressConnectingCall(boolean b) {
        this.simulatedCallState.setAutoProgressConnectingCall(b);
    }

    @Override
    public void setNextDialFailImmediately(boolean b) {
        this.simulatedCallState.setNextDialFailImmediately(b);
    }

    @Override
    public void setNextCallFailCause(int gsmCause) {
        this.mNextCallFailCause = gsmCause;
    }

    @Override
    public void triggerHangupForeground() {
        this.simulatedCallState.triggerHangupForeground();
        this.mCallStateRegistrants.notifyRegistrants();
    }

    @Override
    public void triggerHangupBackground() {
        this.simulatedCallState.triggerHangupBackground();
        this.mCallStateRegistrants.notifyRegistrants();
    }

    @Override
    public void triggerSsn(int type, int code) {
        SuppServiceNotification not = new SuppServiceNotification();
        not.notificationType = type;
        not.code = code;
        this.mSsnRegistrant.notifyRegistrant(new AsyncResult((Object) null, not, (Throwable) null));
    }

    @Override
    public void shutdown() {
        setRadioState(CommandsInterface.RadioState.RADIO_UNAVAILABLE);
        Looper looper = this.mHandlerThread.getLooper();
        if (looper == null) {
            return;
        }
        looper.quit();
    }

    @Override
    public void triggerHangupAll() {
        this.simulatedCallState.triggerHangupAll();
        this.mCallStateRegistrants.notifyRegistrants();
    }

    @Override
    public void triggerIncomingSMS(String message) {
    }

    @Override
    public void pauseResponses() {
        this.mPausedResponseCount++;
    }

    @Override
    public void resumeResponses() {
        this.mPausedResponseCount--;
        if (this.mPausedResponseCount == 0) {
            int s = this.mPausedResponses.size();
            for (int i = 0; i < s; i++) {
                this.mPausedResponses.get(i).sendToTarget();
            }
            this.mPausedResponses.clear();
            return;
        }
        Rlog.e("GSM", "SimulatedCommands.resumeResponses < 0");
    }

    private void unimplemented(Message result) {
        if (result == null) {
            return;
        }
        AsyncResult.forMessage(result).exception = new RuntimeException("Unimplemented");
        if (this.mPausedResponseCount > 0) {
            this.mPausedResponses.add(result);
        } else {
            result.sendToTarget();
        }
    }

    private void resultSuccess(Message result, Object ret) {
        if (result == null) {
            return;
        }
        AsyncResult.forMessage(result).result = ret;
        if (this.mPausedResponseCount > 0) {
            this.mPausedResponses.add(result);
        } else {
            result.sendToTarget();
        }
    }

    private void resultFail(Message result, Object ret, Throwable tr) {
        if (result == null) {
            return;
        }
        AsyncResult.forMessage(result, ret, tr);
        if (this.mPausedResponseCount > 0) {
            this.mPausedResponses.add(result);
        } else {
            result.sendToTarget();
        }
    }

    @Override
    public void getDeviceIdentity(Message response) {
        SimulatedCommandsVerifier.getInstance().getDeviceIdentity(response);
        resultSuccess(response, new String[]{FAKE_IMEI, FAKE_IMEISV, "1234", "1234"});
    }

    @Override
    public void getCDMASubscription(Message result) {
        String[] ret = {"123", "456", "789", "234", "345"};
        resultSuccess(result, ret);
    }

    @Override
    public void setCdmaSubscriptionSource(int cdmaSubscriptionType, Message response) {
        unimplemented(response);
    }

    @Override
    public void queryCdmaRoamingPreference(Message response) {
        unimplemented(response);
    }

    @Override
    public void setCdmaRoamingPreference(int cdmaRoamingType, Message response) {
        unimplemented(response);
    }

    @Override
    public void setPhoneType(int phoneType) {
    }

    @Override
    public void getPreferredVoicePrivacy(Message result) {
        unimplemented(result);
    }

    @Override
    public void setPreferredVoicePrivacy(boolean enable, Message result) {
        unimplemented(result);
    }

    @Override
    public void setTTYMode(int ttyMode, Message response) {
        Rlog.w(LOG_TAG, "Not implemented in SimulatedCommands");
        unimplemented(response);
    }

    @Override
    public void queryTTYMode(Message response) {
        unimplemented(response);
    }

    @Override
    public void sendCDMAFeatureCode(String FeatureCode, Message response) {
        unimplemented(response);
    }

    @Override
    public void sendCdmaSms(byte[] pdu, Message response) {
        SimulatedCommandsVerifier.getInstance().sendCdmaSms(pdu, response);
        resultSuccess(response, null);
    }

    @Override
    public void setCdmaBroadcastActivation(boolean activate, Message response) {
        unimplemented(response);
    }

    @Override
    public void getCdmaBroadcastConfig(Message response) {
        unimplemented(response);
    }

    @Override
    public void setCdmaBroadcastConfig(CdmaSmsBroadcastConfigInfo[] configs, Message response) {
        unimplemented(response);
    }

    public void forceDataDormancy(Message response) {
        unimplemented(response);
    }

    @Override
    public void setGsmBroadcastActivation(boolean activate, Message response) {
        unimplemented(response);
    }

    @Override
    public void setGsmBroadcastConfig(SmsBroadcastConfigInfo[] config, Message response) {
        unimplemented(response);
    }

    @Override
    public void getGsmBroadcastConfig(Message response) {
        unimplemented(response);
    }

    @Override
    public void supplyIccPinForApp(String pin, String aid, Message response) {
        SimulatedCommandsVerifier.getInstance().supplyIccPinForApp(pin, aid, response);
        if (this.mPinCode != null && this.mPinCode.equals(pin)) {
            resultSuccess(response, null);
            return;
        }
        Rlog.i(LOG_TAG, "[SimCmd] supplyIccPinForApp: pin failed!");
        Throwable ex = new CommandException(CommandException.Error.PASSWORD_INCORRECT);
        int[] iArr = new int[1];
        int i = this.mPin1attemptsRemaining - 1;
        this.mPin1attemptsRemaining = i;
        iArr[0] = i < 0 ? 0 : this.mPin1attemptsRemaining;
        resultFail(response, iArr, ex);
    }

    @Override
    public void supplyIccPukForApp(String puk, String newPin, String aid, Message response) {
        unimplemented(response);
    }

    @Override
    public void supplyIccPin2ForApp(String pin2, String aid, Message response) {
        unimplemented(response);
    }

    @Override
    public void supplyIccPuk2ForApp(String puk2, String newPin2, String aid, Message response) {
        unimplemented(response);
    }

    @Override
    public void changeIccPinForApp(String oldPin, String newPin, String aidPtr, Message response) {
        SimulatedCommandsVerifier.getInstance().changeIccPinForApp(oldPin, newPin, aidPtr, response);
        changeIccPin(oldPin, newPin, response);
    }

    @Override
    public void changeIccPin2ForApp(String oldPin2, String newPin2, String aidPtr, Message response) {
        unimplemented(response);
    }

    @Override
    public void requestIsimAuthentication(String nonce, Message response) {
        unimplemented(response);
    }

    @Override
    public void requestIccSimAuthentication(int authContext, String data, String aid, Message response) {
        unimplemented(response);
    }

    @Override
    public void getVoiceRadioTechnology(Message response) {
        SimulatedCommandsVerifier.getInstance().getVoiceRadioTechnology(response);
        int[] ret = {this.mVoiceRadioTech};
        resultSuccess(response, ret);
    }

    public void setCellInfoList(List<CellInfo> list) {
        this.mCellInfoList = list;
    }

    @Override
    public void getCellInfoList(Message response) {
        if (this.mCellInfoList == null) {
            Parcel p = Parcel.obtain();
            p.writeInt(1);
            p.writeInt(1);
            p.writeInt(2);
            p.writeLong(1453510289108L);
            p.writeInt(310);
            p.writeInt(260);
            p.writeInt(123);
            p.writeInt(456);
            p.writeInt(99);
            p.writeInt(3);
            p.setDataPosition(0);
            CellInfo cellInfo = (CellInfoGsm) CellInfoGsm.CREATOR.createFromParcel(p);
            ArrayList<CellInfo> mCellInfoList = new ArrayList<>();
            mCellInfoList.add(cellInfo);
        }
        resultSuccess(response, this.mCellInfoList);
    }

    @Override
    public int getRilVersion() {
        return 11;
    }

    @Override
    public void setCellInfoListRate(int rateInMillis, Message response) {
        unimplemented(response);
    }

    @Override
    public void setInitialAttachApn(String apn, String protocol, int authType, String username, String password, Message result) {
    }

    @Override
    public void setDataProfile(DataProfile[] dps, Message result) {
    }

    public void setImsRegistrationState(int[] regState) {
        this.mImsRegState = regState;
    }

    @Override
    public void getImsRegistrationState(Message response) {
        if (this.mImsRegState == null) {
            this.mImsRegState = new int[]{1, 0};
        }
        resultSuccess(response, this.mImsRegState);
    }

    @Override
    public void sendImsCdmaSms(byte[] pdu, int retry, int messageRef, Message response) {
        SimulatedCommandsVerifier.getInstance().sendImsCdmaSms(pdu, retry, messageRef, response);
        resultSuccess(response, new SmsResponse(0, null, 0));
    }

    @Override
    public void sendImsGsmSms(String smscPDU, String pdu, int retry, int messageRef, Message response) {
        SimulatedCommandsVerifier.getInstance().sendImsGsmSms(smscPDU, pdu, retry, messageRef, response);
        resultSuccess(response, new SmsResponse(0, null, 0));
    }

    @Override
    public void iccOpenLogicalChannel(String AID, Message response) {
        SimulatedCommandsVerifier.getInstance().iccOpenLogicalChannel(AID, response);
        resultSuccess(response, new int[]{this.mChannelId});
    }

    @Override
    public void iccCloseLogicalChannel(int channel, Message response) {
        unimplemented(response);
    }

    @Override
    public void iccTransmitApduLogicalChannel(int channel, int cla, int instruction, int p1, int p2, int p3, String data, Message response) {
        SimulatedCommandsVerifier.getInstance().iccTransmitApduLogicalChannel(channel, cla, instruction, p1, p2, p3, data, response);
        if (this.mIccIoResultForApduLogicalChannel != null) {
            resultSuccess(response, this.mIccIoResultForApduLogicalChannel);
        } else {
            resultFail(response, null, new RuntimeException("IccIoResult not set"));
        }
    }

    @Override
    public void iccTransmitApduBasicChannel(int cla, int instruction, int p1, int p2, int p3, String data, Message response) {
        unimplemented(response);
    }

    @Override
    public void nvReadItem(int itemID, Message response) {
        unimplemented(response);
    }

    @Override
    public void nvWriteItem(int itemID, String itemValue, Message response) {
        unimplemented(response);
    }

    @Override
    public void nvWriteCdmaPrl(byte[] preferredRoamingList, Message response) {
        unimplemented(response);
    }

    @Override
    public void nvResetConfig(int resetType, Message response) {
        unimplemented(response);
    }

    @Override
    public void getHardwareConfig(Message result) {
        unimplemented(result);
    }

    @Override
    public void requestShutdown(Message result) {
        setRadioState(CommandsInterface.RadioState.RADIO_UNAVAILABLE);
    }

    @Override
    public void startLceService(int report_interval_ms, boolean pullMode, Message result) {
        SimulatedCommandsVerifier.getInstance().startLceService(report_interval_ms, pullMode, result);
        unimplemented(result);
    }

    @Override
    public void stopLceService(Message result) {
        unimplemented(result);
    }

    @Override
    public void pullLceData(Message result) {
        unimplemented(result);
    }

    @Override
    public void getModemActivityInfo(Message result) {
        unimplemented(result);
    }

    @Override
    public void setTrm(int mode, Message result) {
    }

    @Override
    public void getRadioCapability(Message result) {
        SimulatedCommandsVerifier.getInstance().getRadioCapability(result);
        resultSuccess(result, new RadioCapability(0, 0, 0, CallFailCause.ERROR_UNSPECIFIED, null, 0));
    }

    public void notifySmsStatus(Object result) {
        if (this.mSmsStatusRegistrant == null) {
            return;
        }
        this.mSmsStatusRegistrant.notifyRegistrant(new AsyncResult((Object) null, result, (Throwable) null));
    }

    public void notifyGsmBroadcastSms(Object result) {
        if (this.mGsmBroadcastSmsRegistrant == null) {
            return;
        }
        this.mGsmBroadcastSmsRegistrant.notifyRegistrant(new AsyncResult((Object) null, result, (Throwable) null));
    }

    public void notifyIccSmsFull() {
        if (this.mIccSmsFullRegistrant == null) {
            return;
        }
        this.mIccSmsFullRegistrant.notifyRegistrant();
    }

    public void notifyEmergencyCallbackMode() {
        if (this.mEmergencyCallbackModeRegistrant == null) {
            return;
        }
        this.mEmergencyCallbackModeRegistrant.notifyRegistrant();
    }

    @Override
    public void setEmergencyCallbackMode(Handler h, int what, Object obj) {
        SimulatedCommandsVerifier.getInstance().setEmergencyCallbackMode(h, what, obj);
        super.setEmergencyCallbackMode(h, what, obj);
    }

    public void notifyExitEmergencyCallbackMode() {
        if (this.mExitEmergencyCallbackModeRegistrants == null) {
            return;
        }
        this.mExitEmergencyCallbackModeRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
    }

    public void notifyImsNetworkStateChanged() {
        if (this.mImsNetworkStateChangedRegistrants == null) {
            return;
        }
        this.mImsNetworkStateChangedRegistrants.notifyRegistrants();
    }

    @Override
    public void registerForExitEmergencyCallbackMode(Handler h, int what, Object obj) {
        SimulatedCommandsVerifier.getInstance().registerForExitEmergencyCallbackMode(h, what, obj);
        super.registerForExitEmergencyCallbackMode(h, what, obj);
    }

    public void notifyRadioOn() {
        this.mOnRegistrants.notifyRegistrants();
    }

    public void notifyVoiceNetworkStateChanged() {
        this.mVoiceNetworkStateRegistrants.notifyRegistrants();
    }

    public void notifyOtaProvisionStatusChanged() {
        if (this.mOtaProvisionRegistrants == null) {
            return;
        }
        int[] ret = {8};
        this.mOtaProvisionRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
    }

    public void notifySignalStrength() {
        if (this.mSignalStrength == null) {
            this.mSignalStrength = new SignalStrength(20, 0, -1, -1, -1, -1, -1, 99, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
        }
        if (this.mSignalStrengthRegistrant == null) {
            return;
        }
        this.mSignalStrengthRegistrant.notifyRegistrant(new AsyncResult((Object) null, this.mSignalStrength, (Throwable) null));
    }

    public void setIccCardStatus(IccCardStatus iccCardStatus) {
        this.mIccCardStatus = iccCardStatus;
    }

    public void setIccIoResultForApduLogicalChannel(IccIoResult iccIoResult) {
        this.mIccIoResultForApduLogicalChannel = iccIoResult;
    }

    public void setOpenChannelId(int channelId) {
        this.mChannelId = channelId;
    }

    public void setPin1RemainingAttempt(int pin1attemptsRemaining) {
        this.mPin1attemptsRemaining = pin1attemptsRemaining;
    }

    @Override
    public void setDataAllowed(boolean allowed, Message result) {
        log("setDataAllowed = " + allowed);
        this.mAllowed.set(allowed);
        resultSuccess(result, null);
    }

    public boolean isDataAllowed() {
        return this.mAllowed.get();
    }

    @Override
    public void changeBarringPassword(String facility, String oldPwd, String newPwd, String newCfm, Message result) {
    }

    @Override
    public void ReadPhbEntry(int type, int bIndex, int eIndex, Message response) {
    }
}
