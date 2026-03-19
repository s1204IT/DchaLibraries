package com.mediatek.internal.telephony.test;

import android.content.Context;
import android.hardware.radio.V1_0.DataRegStateResult;
import android.hardware.radio.V1_0.SetupDataCallResult;
import android.hardware.radio.V1_0.VoiceRegStateResult;
import android.net.KeepalivePacketData;
import android.net.LinkProperties;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.SystemClock;
import android.os.WorkSource;
import android.service.carrier.CarrierIdentifier;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.Rlog;
import android.telephony.SignalStrength;
import android.telephony.data.DataProfile;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.BaseCommands;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.LastCallFailCause;
import com.android.internal.telephony.RadioCapability;
import com.android.internal.telephony.SmsResponse;
import com.android.internal.telephony.UUSInfo;
import com.android.internal.telephony.cdma.CdmaSmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.IccSlotStatus;
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
    public boolean mCssSupported;
    private int mDataRadioTech;
    private int mDataRegState;
    private boolean mDcSuccess;
    public int mDefaultRoamingIndicator;
    private final AtomicInteger mGetDataRegistrationStateCallCount;
    private final AtomicInteger mGetOperatorCallCount;
    private final AtomicInteger mGetVoiceRegistrationStateCallCount;
    HandlerThread mHandlerThread;
    private IccCardStatus mIccCardStatus;
    private IccIoResult mIccIoResultForApduLogicalChannel;
    private IccSlotStatus mIccSlotStatus;
    private String mImei;
    private String mImeiSv;
    private int[] mImsRegState;
    private boolean mIsRadioPowerFailResponse;
    public int mMaxDataCalls;
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
    public int mReasonForDenial;
    public int mRoamingIndicator;
    private SetupDataCallResult mSetupDataCallResult;
    private SignalStrength mSignalStrength;
    boolean mSimFdnEnabled;
    SimFdnState mSimFdnEnabledState;
    boolean mSimLockEnabled;
    SimLockState mSimLockedState;
    boolean mSsnNotifyOn;
    public int mSystemIsInPrl;
    private int mVoiceRadioTech;
    private int mVoiceRegState;
    SimulatedGsmCallState simulatedCallState;
    private static final SimLockState INITIAL_LOCK_STATE = SimLockState.NONE;
    private static final SimFdnState INITIAL_FDN_STATE = SimFdnState.NONE;

    private enum SimFdnState {
        NONE,
        REQUIRE_PIN2,
        REQUIRE_PUK2,
        SIM_PERM_LOCKED
    }

    private enum SimLockState {
        NONE,
        REQUIRE_PIN,
        REQUIRE_PUK,
        SIM_PERM_LOCKED
    }

    public SimulatedCommands() {
        boolean z;
        super((Context) null);
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
        this.mIsRadioPowerFailResponse = false;
        this.mGetVoiceRegistrationStateCallCount = new AtomicInteger(0);
        this.mGetDataRegistrationStateCallCount = new AtomicInteger(0);
        this.mGetOperatorCallCount = new AtomicInteger(0);
        this.getNetworkSelectionModeCallCount = new AtomicInteger(0);
        this.mAllowed = new AtomicBoolean(false);
        this.mHandlerThread = new HandlerThread(LOG_TAG);
        this.mHandlerThread.start();
        this.simulatedCallState = new SimulatedGsmCallState(this.mHandlerThread.getLooper());
        setRadioState(CommandsInterface.RadioState.RADIO_ON);
        this.mSimLockedState = INITIAL_LOCK_STATE;
        if (this.mSimLockedState == SimLockState.NONE) {
            z = false;
        } else {
            z = true;
        }
        this.mSimLockEnabled = z;
        this.mPinCode = "1234";
        this.mSimFdnEnabledState = INITIAL_FDN_STATE;
        this.mSimFdnEnabled = this.mSimFdnEnabledState != SimFdnState.NONE;
        this.mPin2Code = DEFAULT_SIM_PIN2_CODE;
    }

    public void dispose() {
        if (this.mHandlerThread != null) {
            this.mHandlerThread.quit();
        }
    }

    private void log(String str) {
        Rlog.d(LOG_TAG, str);
    }

    public void getIccCardStatus(Message message) {
        SimulatedCommandsVerifier.getInstance().getIccCardStatus(message);
        if (this.mIccCardStatus != null) {
            resultSuccess(message, this.mIccCardStatus);
        } else {
            resultFail(message, null, new RuntimeException("IccCardStatus not set"));
        }
    }

    public void setIccSlotStatus(IccSlotStatus iccSlotStatus) {
        this.mIccSlotStatus = iccSlotStatus;
    }

    public void getIccSlotsStatus(Message message) {
        SimulatedCommandsVerifier.getInstance().getIccSlotsStatus(message);
        if (this.mIccSlotStatus != null) {
            resultSuccess(message, this.mIccSlotStatus);
        } else {
            resultFail(message, null, new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED));
        }
    }

    public void setLogicalToPhysicalSlotMapping(int[] iArr, Message message) {
        unimplemented(message);
    }

    public void supplyIccPin(String str, Message message) {
        if (this.mSimLockedState != SimLockState.REQUIRE_PIN) {
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPin: wrong state, state=" + this.mSimLockedState);
            resultFail(message, null, new CommandException(CommandException.Error.PASSWORD_INCORRECT));
            return;
        }
        if (str != null && str.equals(this.mPinCode)) {
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPin: success!");
            this.mPinUnlockAttempts = 0;
            this.mSimLockedState = SimLockState.NONE;
            this.mIccStatusChangedRegistrants.notifyRegistrants();
            resultSuccess(message, null);
            return;
        }
        if (message != null) {
            this.mPinUnlockAttempts++;
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPin: failed! attempt=" + this.mPinUnlockAttempts);
            if (this.mPinUnlockAttempts >= 5) {
                Rlog.i(LOG_TAG, "[SimCmd] supplyIccPin: set state to REQUIRE_PUK");
                this.mSimLockedState = SimLockState.REQUIRE_PUK;
            }
            resultFail(message, null, new CommandException(CommandException.Error.PASSWORD_INCORRECT));
        }
    }

    public void supplyIccPuk(String str, String str2, Message message) {
        if (this.mSimLockedState != SimLockState.REQUIRE_PUK) {
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPuk: wrong state, state=" + this.mSimLockedState);
            resultFail(message, null, new CommandException(CommandException.Error.PASSWORD_INCORRECT));
            return;
        }
        if (str != null && str.equals(SIM_PUK_CODE)) {
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPuk: success!");
            this.mSimLockedState = SimLockState.NONE;
            this.mPukUnlockAttempts = 0;
            this.mIccStatusChangedRegistrants.notifyRegistrants();
            resultSuccess(message, null);
            return;
        }
        if (message != null) {
            this.mPukUnlockAttempts++;
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPuk: failed! attempt=" + this.mPukUnlockAttempts);
            if (this.mPukUnlockAttempts >= 10) {
                Rlog.i(LOG_TAG, "[SimCmd] supplyIccPuk: set state to SIM_PERM_LOCKED");
                this.mSimLockedState = SimLockState.SIM_PERM_LOCKED;
            }
            resultFail(message, null, new CommandException(CommandException.Error.PASSWORD_INCORRECT));
        }
    }

    public void supplyIccPin2(String str, Message message) {
        if (this.mSimFdnEnabledState != SimFdnState.REQUIRE_PIN2) {
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPin2: wrong state, state=" + this.mSimFdnEnabledState);
            resultFail(message, null, new CommandException(CommandException.Error.PASSWORD_INCORRECT));
            return;
        }
        if (str != null && str.equals(this.mPin2Code)) {
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPin2: success!");
            this.mPin2UnlockAttempts = 0;
            this.mSimFdnEnabledState = SimFdnState.NONE;
            resultSuccess(message, null);
            return;
        }
        if (message != null) {
            this.mPin2UnlockAttempts++;
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPin2: failed! attempt=" + this.mPin2UnlockAttempts);
            if (this.mPin2UnlockAttempts >= 5) {
                Rlog.i(LOG_TAG, "[SimCmd] supplyIccPin2: set state to REQUIRE_PUK2");
                this.mSimFdnEnabledState = SimFdnState.REQUIRE_PUK2;
            }
            resultFail(message, null, new CommandException(CommandException.Error.PASSWORD_INCORRECT));
        }
    }

    public void supplyIccPuk2(String str, String str2, Message message) {
        if (this.mSimFdnEnabledState != SimFdnState.REQUIRE_PUK2) {
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPuk2: wrong state, state=" + this.mSimLockedState);
            resultFail(message, null, new CommandException(CommandException.Error.PASSWORD_INCORRECT));
            return;
        }
        if (str != null && str.equals(SIM_PUK2_CODE)) {
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPuk2: success!");
            this.mSimFdnEnabledState = SimFdnState.NONE;
            this.mPuk2UnlockAttempts = 0;
            resultSuccess(message, null);
            return;
        }
        if (message != null) {
            this.mPuk2UnlockAttempts++;
            Rlog.i(LOG_TAG, "[SimCmd] supplyIccPuk2: failed! attempt=" + this.mPuk2UnlockAttempts);
            if (this.mPuk2UnlockAttempts >= 10) {
                Rlog.i(LOG_TAG, "[SimCmd] supplyIccPuk2: set state to SIM_PERM_LOCKED");
                this.mSimFdnEnabledState = SimFdnState.SIM_PERM_LOCKED;
            }
            resultFail(message, null, new CommandException(CommandException.Error.PASSWORD_INCORRECT));
        }
    }

    public void changeIccPin(String str, String str2, Message message) {
        if (str != null && str.equals(this.mPinCode)) {
            this.mPinCode = str2;
            resultSuccess(message, null);
        } else {
            Rlog.i(LOG_TAG, "[SimCmd] changeIccPin: pin failed!");
            resultFail(message, null, new CommandException(CommandException.Error.PASSWORD_INCORRECT));
        }
    }

    public void changeIccPin2(String str, String str2, Message message) {
        if (str != null && str.equals(this.mPin2Code)) {
            this.mPin2Code = str2;
            resultSuccess(message, null);
        } else {
            Rlog.i(LOG_TAG, "[SimCmd] changeIccPin2: pin2 failed!");
            resultFail(message, null, new CommandException(CommandException.Error.PASSWORD_INCORRECT));
        }
    }

    public void changeBarringPassword(String str, String str2, String str3, Message message) {
        unimplemented(message);
    }

    public void setSuppServiceNotifications(boolean z, Message message) {
        resultSuccess(message, null);
        if (z && this.mSsnNotifyOn) {
            Rlog.w(LOG_TAG, "Supp Service Notifications already enabled!");
        }
        this.mSsnNotifyOn = z;
    }

    public void queryFacilityLock(String str, String str2, int i, Message message) {
        queryFacilityLockForApp(str, str2, i, null, message);
    }

    public void queryFacilityLockForApp(String str, String str2, int i, String str3, Message message) {
        if (str != null && str.equals("SC")) {
            if (message != null) {
                int[] iArr = {this.mSimLockEnabled ? 1 : 0};
                StringBuilder sb = new StringBuilder();
                sb.append("[SimCmd] queryFacilityLock: SIM is ");
                sb.append(iArr[0] == 0 ? "unlocked" : "locked");
                Rlog.i(LOG_TAG, sb.toString());
                resultSuccess(message, iArr);
                return;
            }
            return;
        }
        if (str != null && str.equals("FD")) {
            if (message != null) {
                int[] iArr2 = {this.mSimFdnEnabled ? 1 : 0};
                StringBuilder sb2 = new StringBuilder();
                sb2.append("[SimCmd] queryFacilityLock: FDN is ");
                sb2.append(iArr2[0] == 0 ? "disabled" : "enabled");
                Rlog.i(LOG_TAG, sb2.toString());
                resultSuccess(message, iArr2);
                return;
            }
            return;
        }
        unimplemented(message);
    }

    public void setFacilityLock(String str, boolean z, String str2, int i, Message message) {
        setFacilityLockForApp(str, z, str2, i, null, message);
    }

    public void setFacilityLockForApp(String str, boolean z, String str2, int i, String str3, Message message) {
        if (str != null && str.equals("SC")) {
            if (str2 != null && str2.equals(this.mPinCode)) {
                Rlog.i(LOG_TAG, "[SimCmd] setFacilityLock: pin is valid");
                this.mSimLockEnabled = z;
                resultSuccess(message, null);
                return;
            } else {
                Rlog.i(LOG_TAG, "[SimCmd] setFacilityLock: pin failed!");
                resultFail(message, null, new CommandException(CommandException.Error.GENERIC_FAILURE));
                return;
            }
        }
        if (str != null && str.equals("FD")) {
            if (str2 != null && str2.equals(this.mPin2Code)) {
                Rlog.i(LOG_TAG, "[SimCmd] setFacilityLock: pin2 is valid");
                this.mSimFdnEnabled = z;
                resultSuccess(message, null);
                return;
            } else {
                Rlog.i(LOG_TAG, "[SimCmd] setFacilityLock: pin2 failed!");
                resultFail(message, null, new CommandException(CommandException.Error.GENERIC_FAILURE));
                return;
            }
        }
        unimplemented(message);
    }

    public void supplyNetworkDepersonalization(String str, Message message) {
        unimplemented(message);
    }

    public void getCurrentCalls(Message message) {
        SimulatedCommandsVerifier.getInstance().getCurrentCalls(message);
        if (this.mState == CommandsInterface.RadioState.RADIO_ON && !isSimLocked()) {
            resultSuccess(message, this.simulatedCallState.getDriverCalls());
        } else {
            resultFail(message, null, new CommandException(CommandException.Error.RADIO_NOT_AVAILABLE));
        }
    }

    @Deprecated
    public void getPDPContextList(Message message) {
        getDataCallList(message);
    }

    public void getDataCallList(Message message) {
        resultSuccess(message, new ArrayList(0));
    }

    public void dial(String str, int i, Message message) {
        SimulatedCommandsVerifier.getInstance().dial(str, i, message);
        this.simulatedCallState.onDial(str);
        resultSuccess(message, null);
    }

    public void dial(String str, int i, UUSInfo uUSInfo, Message message) {
        SimulatedCommandsVerifier.getInstance().dial(str, i, uUSInfo, message);
        this.simulatedCallState.onDial(str);
        resultSuccess(message, null);
    }

    public void getIMSI(Message message) {
        getIMSIForApp(null, message);
    }

    public void getIMSIForApp(String str, Message message) {
        resultSuccess(message, FAKE_IMEI);
    }

    public void setIMEI(String str) {
        this.mImei = str;
    }

    public void getIMEI(Message message) {
        SimulatedCommandsVerifier.getInstance().getIMEI(message);
        resultSuccess(message, this.mImei != null ? this.mImei : FAKE_IMEI);
    }

    public void setIMEISV(String str) {
        this.mImeiSv = str;
    }

    public void getIMEISV(Message message) {
        SimulatedCommandsVerifier.getInstance().getIMEISV(message);
        resultSuccess(message, this.mImeiSv != null ? this.mImeiSv : FAKE_IMEISV);
    }

    public void hangupConnection(int i, Message message) {
        if (!this.simulatedCallState.onChld('1', (char) (48 + i))) {
            Rlog.i("GSM", "[SimCmd] hangupConnection: resultFail");
            resultFail(message, null, new RuntimeException("Hangup Error"));
        } else {
            Rlog.i("GSM", "[SimCmd] hangupConnection: resultSuccess");
            resultSuccess(message, null);
        }
    }

    public void hangupWaitingOrBackground(Message message) {
        if (!this.simulatedCallState.onChld('0', (char) 0)) {
            resultFail(message, null, new RuntimeException("Hangup Error"));
        } else {
            resultSuccess(message, null);
        }
    }

    public void hangupForegroundResumeBackground(Message message) {
        if (!this.simulatedCallState.onChld('1', (char) 0)) {
            resultFail(message, null, new RuntimeException("Hangup Error"));
        } else {
            resultSuccess(message, null);
        }
    }

    public void switchWaitingOrHoldingAndActive(Message message) {
        if (!this.simulatedCallState.onChld('2', (char) 0)) {
            resultFail(message, null, new RuntimeException("Hangup Error"));
        } else {
            resultSuccess(message, null);
        }
    }

    public void conference(Message message) {
        if (!this.simulatedCallState.onChld('3', (char) 0)) {
            resultFail(message, null, new RuntimeException("Hangup Error"));
        } else {
            resultSuccess(message, null);
        }
    }

    public void explicitCallTransfer(Message message) {
        if (!this.simulatedCallState.onChld('4', (char) 0)) {
            resultFail(message, null, new RuntimeException("Hangup Error"));
        } else {
            resultSuccess(message, null);
        }
    }

    public void separateConnection(int i, Message message) {
        if (!this.simulatedCallState.onChld('2', (char) (i + 48))) {
            resultFail(message, null, new RuntimeException("Hangup Error"));
        } else {
            resultSuccess(message, null);
        }
    }

    public void acceptCall(Message message) {
        SimulatedCommandsVerifier.getInstance().acceptCall(message);
        if (!this.simulatedCallState.onAnswer()) {
            resultFail(message, null, new RuntimeException("Hangup Error"));
        } else {
            resultSuccess(message, null);
        }
    }

    public void rejectCall(Message message) {
        if (!this.simulatedCallState.onChld('0', (char) 0)) {
            resultFail(message, null, new RuntimeException("Hangup Error"));
        } else {
            resultSuccess(message, null);
        }
    }

    public void getLastCallFailCause(Message message) {
        LastCallFailCause lastCallFailCause = new LastCallFailCause();
        lastCallFailCause.causeCode = this.mNextCallFailCause;
        resultSuccess(message, lastCallFailCause);
    }

    @Deprecated
    public void getLastPdpFailCause(Message message) {
        unimplemented(message);
    }

    public void getLastDataCallFailCause(Message message) {
        unimplemented(message);
    }

    public void setMute(boolean z, Message message) {
        unimplemented(message);
    }

    public void getMute(Message message) {
        unimplemented(message);
    }

    public void setSignalStrength(SignalStrength signalStrength) {
        this.mSignalStrength = signalStrength;
    }

    public void getSignalStrength(Message message) {
        if (this.mSignalStrength == null) {
            this.mSignalStrength = new SignalStrength(20, 0, -1, -1, -1, -1, -1, 99, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        }
        resultSuccess(message, this.mSignalStrength);
    }

    public void setBandMode(int i, Message message) {
        resultSuccess(message, null);
    }

    public void queryAvailableBandMode(Message message) {
        resultSuccess(message, new int[]{4, 2, 3, 4});
    }

    public void sendTerminalResponse(String str, Message message) {
        resultSuccess(message, null);
    }

    public void sendEnvelope(String str, Message message) {
        resultSuccess(message, null);
    }

    public void sendEnvelopeWithStatus(String str, Message message) {
        resultSuccess(message, null);
    }

    public void handleCallSetupRequestFromSim(boolean z, Message message) {
        resultSuccess(message, null);
    }

    public void setVoiceRadioTech(int i) {
        this.mVoiceRadioTech = i;
    }

    public void setVoiceRegState(int i) {
        this.mVoiceRegState = i;
    }

    public void getVoiceRegistrationState(Message message) {
        this.mGetVoiceRegistrationStateCallCount.incrementAndGet();
        VoiceRegStateResult voiceRegStateResult = new VoiceRegStateResult();
        voiceRegStateResult.regState = this.mVoiceRegState;
        voiceRegStateResult.rat = this.mVoiceRadioTech;
        voiceRegStateResult.cssSupported = this.mCssSupported;
        voiceRegStateResult.roamingIndicator = this.mRoamingIndicator;
        voiceRegStateResult.systemIsInPrl = this.mSystemIsInPrl;
        voiceRegStateResult.defaultRoamingIndicator = this.mDefaultRoamingIndicator;
        voiceRegStateResult.reasonForDenial = this.mReasonForDenial;
        resultSuccess(message, voiceRegStateResult);
    }

    @VisibleForTesting
    public int getGetVoiceRegistrationStateCallCount() {
        return this.mGetVoiceRegistrationStateCallCount.get();
    }

    public void setDataRadioTech(int i) {
        this.mDataRadioTech = i;
    }

    public void setDataRegState(int i) {
        this.mDataRegState = i;
    }

    public void getDataRegistrationState(Message message) {
        this.mGetDataRegistrationStateCallCount.incrementAndGet();
        DataRegStateResult dataRegStateResult = new DataRegStateResult();
        dataRegStateResult.regState = this.mDataRegState;
        dataRegStateResult.rat = this.mDataRadioTech;
        dataRegStateResult.maxDataCalls = this.mMaxDataCalls;
        dataRegStateResult.reasonDataDenied = this.mReasonForDenial;
        resultSuccess(message, dataRegStateResult);
    }

    @VisibleForTesting
    public int getGetDataRegistrationStateCallCount() {
        return this.mGetDataRegistrationStateCallCount.get();
    }

    public void getOperator(Message message) {
        this.mGetOperatorCallCount.incrementAndGet();
        resultSuccess(message, new String[]{FAKE_LONG_NAME, FAKE_SHORT_NAME, FAKE_MCC_MNC});
    }

    @VisibleForTesting
    public int getGetOperatorCallCount() {
        this.mGetOperatorCallCount.get();
        return this.mGetOperatorCallCount.get();
    }

    public void sendDtmf(char c, Message message) {
        resultSuccess(message, null);
    }

    public void startDtmf(char c, Message message) {
        resultSuccess(message, null);
    }

    public void stopDtmf(Message message) {
        resultSuccess(message, null);
    }

    public void sendBurstDtmf(String str, int i, int i2, Message message) {
        SimulatedCommandsVerifier.getInstance().sendBurstDtmf(str, i, i2, message);
        resultSuccess(message, null);
    }

    public void sendSMS(String str, String str2, Message message) {
        SimulatedCommandsVerifier.getInstance().sendSMS(str, str2, message);
        resultSuccess(message, new SmsResponse(0, (String) null, 0));
    }

    public void sendSMSExpectMore(String str, String str2, Message message) {
        unimplemented(message);
    }

    public void deleteSmsOnSim(int i, Message message) {
        Rlog.d(LOG_TAG, "Delete message at index " + i);
        unimplemented(message);
    }

    public void deleteSmsOnRuim(int i, Message message) {
        Rlog.d(LOG_TAG, "Delete RUIM message at index " + i);
        unimplemented(message);
    }

    public void writeSmsToSim(int i, String str, String str2, Message message) {
        Rlog.d(LOG_TAG, "Write SMS to SIM with status " + i);
        unimplemented(message);
    }

    public void writeSmsToRuim(int i, String str, Message message) {
        Rlog.d(LOG_TAG, "Write SMS to RUIM with status " + i);
        unimplemented(message);
    }

    public void setDataCallResult(boolean z, SetupDataCallResult setupDataCallResult) {
        this.mSetupDataCallResult = setupDataCallResult;
        this.mDcSuccess = z;
    }

    public void triggerNITZupdate(String str) {
        if (str != null) {
            this.mNITZTimeRegistrant.notifyRegistrant(new AsyncResult((Object) null, new Object[]{str, Long.valueOf(SystemClock.elapsedRealtime())}, (Throwable) null));
        }
    }

    public void setupDataCall(int i, DataProfile dataProfile, boolean z, boolean z2, int i2, LinkProperties linkProperties, Message message) {
        SimulatedCommandsVerifier.getInstance().setupDataCall(i, dataProfile, z, z2, i2, linkProperties, message);
        if (this.mSetupDataCallResult == null) {
            try {
                this.mSetupDataCallResult = new SetupDataCallResult();
                this.mSetupDataCallResult.status = 0;
                this.mSetupDataCallResult.suggestedRetryTime = -1;
                this.mSetupDataCallResult.cid = 1;
                this.mSetupDataCallResult.active = 2;
                this.mSetupDataCallResult.type = "IP";
                this.mSetupDataCallResult.ifname = "rmnet_data7";
                this.mSetupDataCallResult.addresses = "12.34.56.78";
                this.mSetupDataCallResult.dnses = "98.76.54.32";
                this.mSetupDataCallResult.gateways = "11.22.33.44";
                this.mSetupDataCallResult.pcscf = "";
                this.mSetupDataCallResult.mtu = 1440;
            } catch (Exception e) {
            }
        }
        if (this.mDcSuccess) {
            resultSuccess(message, this.mSetupDataCallResult);
        } else {
            resultFail(message, this.mSetupDataCallResult, new RuntimeException("Setup data call failed!"));
        }
    }

    public void deactivateDataCall(int i, int i2, Message message) {
        SimulatedCommandsVerifier.getInstance().deactivateDataCall(i, i2, message);
        resultSuccess(message, null);
    }

    public void setPreferredNetworkType(int i, Message message) {
        SimulatedCommandsVerifier.getInstance().setPreferredNetworkType(i, message);
        this.mNetworkType = i;
        resultSuccess(message, null);
    }

    public void getPreferredNetworkType(Message message) {
        SimulatedCommandsVerifier.getInstance().getPreferredNetworkType(message);
        resultSuccess(message, new int[]{this.mNetworkType});
    }

    public void getNeighboringCids(Message message, WorkSource workSource) {
        int[] iArr = new int[7];
        iArr[0] = 6;
        for (int i = 1; i < 7; i++) {
            iArr[i] = i;
        }
        resultSuccess(message, iArr);
    }

    public void setLocationUpdates(boolean z, Message message) {
        SimulatedCommandsVerifier.getInstance().setLocationUpdates(z, message);
        resultSuccess(message, null);
    }

    public void getSmscAddress(Message message) {
        unimplemented(message);
    }

    public void setSmscAddress(String str, Message message) {
        unimplemented(message);
    }

    public void reportSmsMemoryStatus(boolean z, Message message) {
        resultSuccess(message, null);
        SimulatedCommandsVerifier.getInstance().reportSmsMemoryStatus(z, message);
    }

    public void reportStkServiceIsRunning(Message message) {
        resultSuccess(message, null);
    }

    public void getCdmaSubscriptionSource(Message message) {
        unimplemented(message);
    }

    private boolean isSimLocked() {
        if (this.mSimLockedState != SimLockState.NONE) {
            return true;
        }
        return false;
    }

    public void setRadioPower(boolean z, Message message) {
        if (this.mIsRadioPowerFailResponse) {
            resultFail(message, null, new RuntimeException("setRadioPower failed!"));
            return;
        }
        if (z) {
            setRadioState(CommandsInterface.RadioState.RADIO_ON);
        } else {
            setRadioState(CommandsInterface.RadioState.RADIO_OFF);
        }
        resultSuccess(message, null);
    }

    public void acknowledgeLastIncomingGsmSms(boolean z, int i, Message message) {
        unimplemented(message);
        SimulatedCommandsVerifier.getInstance().acknowledgeLastIncomingGsmSms(z, i, message);
    }

    public void acknowledgeLastIncomingCdmaSms(boolean z, int i, Message message) {
        unimplemented(message);
    }

    public void acknowledgeIncomingGsmSmsWithPdu(boolean z, String str, Message message) {
        unimplemented(message);
    }

    public void iccIO(int i, int i2, String str, int i3, int i4, int i5, String str2, String str3, Message message) {
        iccIOForApp(i, i2, str, i3, i4, i5, str2, str3, null, message);
    }

    public void iccIOForApp(int i, int i2, String str, int i3, int i4, int i5, String str2, String str3, String str4, Message message) {
        unimplemented(message);
    }

    public void queryCLIP(Message message) {
        unimplemented(message);
    }

    public void getCLIR(Message message) {
        unimplemented(message);
    }

    public void setCLIR(int i, Message message) {
        unimplemented(message);
    }

    public void queryCallWaiting(int i, Message message) {
        unimplemented(message);
    }

    public void setCallWaiting(boolean z, int i, Message message) {
        unimplemented(message);
    }

    public void setCallForward(int i, int i2, int i3, String str, int i4, Message message) {
        SimulatedCommandsVerifier.getInstance().setCallForward(i, i2, i3, str, i4, message);
        resultSuccess(message, null);
    }

    public void queryCallForwardStatus(int i, int i2, String str, Message message) {
        SimulatedCommandsVerifier.getInstance().queryCallForwardStatus(i, i2, str, message);
        resultSuccess(message, null);
    }

    public void setNetworkSelectionModeAutomatic(Message message) {
        unimplemented(message);
    }

    public void exitEmergencyCallbackMode(Message message) {
        unimplemented(message);
    }

    public void setNetworkSelectionModeManual(String str, Message message) {
        unimplemented(message);
    }

    public void getNetworkSelectionMode(Message message) {
        SimulatedCommandsVerifier.getInstance().getNetworkSelectionMode(message);
        this.getNetworkSelectionModeCallCount.incrementAndGet();
        resultSuccess(message, new int[]{0});
    }

    @VisibleForTesting
    public int getGetNetworkSelectionModeCallCount() {
        return this.getNetworkSelectionModeCallCount.get();
    }

    public void getAvailableNetworks(Message message) {
        unimplemented(message);
    }

    public void startNetworkScan(NetworkScanRequest networkScanRequest, Message message) {
        unimplemented(message);
    }

    public void stopNetworkScan(Message message) {
        unimplemented(message);
    }

    public void getBasebandVersion(Message message) {
        SimulatedCommandsVerifier.getInstance().getBasebandVersion(message);
        resultSuccess(message, LOG_TAG);
    }

    public void triggerIncomingStkCcAlpha(String str) {
        if (this.mCatCcAlphaRegistrant != null) {
            this.mCatCcAlphaRegistrant.notifyResult(str);
        }
    }

    public void sendStkCcAplha(String str) {
        triggerIncomingStkCcAlpha(str);
    }

    @Override
    public void triggerIncomingUssd(String str, String str2) {
        if (this.mUSSDRegistrant != null) {
            this.mUSSDRegistrant.notifyResult(new String[]{str, str2});
        }
    }

    public void sendUSSD(String str, Message message) {
        if (str.equals("#646#")) {
            resultSuccess(message, null);
            triggerIncomingUssd("0", "You have NNN minutes remaining.");
        } else {
            resultSuccess(message, null);
            triggerIncomingUssd("0", "All Done");
        }
    }

    public void cancelPendingUssd(Message message) {
        resultSuccess(message, null);
    }

    public void resetRadio(Message message) {
        unimplemented(message);
    }

    public void invokeOemRilRequestRaw(byte[] bArr, Message message) {
        if (message != null) {
            AsyncResult.forMessage(message).result = bArr;
            message.sendToTarget();
        }
    }

    public void setCarrierInfoForImsiEncryption(ImsiEncryptionInfo imsiEncryptionInfo, Message message) {
        if (message != null) {
            AsyncResult.forMessage(message).result = imsiEncryptionInfo;
            message.sendToTarget();
        }
    }

    public void invokeOemRilRequestStrings(String[] strArr, Message message) {
        if (message != null) {
            AsyncResult.forMessage(message).result = strArr;
            message.sendToTarget();
        }
    }

    @Override
    public void triggerRing(String str) {
        this.simulatedCallState.triggerRing(str);
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
    public void setAutoProgressConnectingCall(boolean z) {
        this.simulatedCallState.setAutoProgressConnectingCall(z);
    }

    @Override
    public void setNextDialFailImmediately(boolean z) {
        this.simulatedCallState.setNextDialFailImmediately(z);
    }

    @Override
    public void setNextCallFailCause(int i) {
        this.mNextCallFailCause = i;
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
    public void triggerSsn(int i, int i2) {
        SuppServiceNotification suppServiceNotification = new SuppServiceNotification();
        suppServiceNotification.notificationType = i;
        suppServiceNotification.code = i2;
        this.mSsnRegistrant.notifyRegistrant(new AsyncResult((Object) null, suppServiceNotification, (Throwable) null));
    }

    @Override
    public void shutdown() {
        setRadioState(CommandsInterface.RadioState.RADIO_UNAVAILABLE);
        Looper looper = this.mHandlerThread.getLooper();
        if (looper != null) {
            looper.quit();
        }
    }

    @Override
    public void triggerHangupAll() {
        this.simulatedCallState.triggerHangupAll();
        this.mCallStateRegistrants.notifyRegistrants();
    }

    @Override
    public void triggerIncomingSMS(String str) {
    }

    @Override
    public void pauseResponses() {
        this.mPausedResponseCount++;
    }

    @Override
    public void resumeResponses() {
        this.mPausedResponseCount--;
        if (this.mPausedResponseCount == 0) {
            int size = this.mPausedResponses.size();
            for (int i = 0; i < size; i++) {
                this.mPausedResponses.get(i).sendToTarget();
            }
            this.mPausedResponses.clear();
            return;
        }
        Rlog.e("GSM", "SimulatedCommands.resumeResponses < 0");
    }

    private void unimplemented(Message message) {
        if (message != null) {
            AsyncResult.forMessage(message).exception = new RuntimeException("Unimplemented");
            if (this.mPausedResponseCount > 0) {
                this.mPausedResponses.add(message);
            } else {
                message.sendToTarget();
            }
        }
    }

    private void resultSuccess(Message message, Object obj) {
        if (message != null) {
            AsyncResult.forMessage(message).result = obj;
            if (this.mPausedResponseCount > 0) {
                this.mPausedResponses.add(message);
            } else {
                message.sendToTarget();
            }
        }
    }

    private void resultFail(Message message, Object obj, Throwable th) {
        if (message != null) {
            AsyncResult.forMessage(message, obj, th);
            if (this.mPausedResponseCount > 0) {
                this.mPausedResponses.add(message);
            } else {
                message.sendToTarget();
            }
        }
    }

    public void getDeviceIdentity(Message message) {
        SimulatedCommandsVerifier.getInstance().getDeviceIdentity(message);
        resultSuccess(message, new String[]{FAKE_IMEI, FAKE_IMEISV, "1234", "1234"});
    }

    public void getCDMASubscription(Message message) {
        resultSuccess(message, new String[]{"123", "456", "789", "234", "345"});
    }

    public void setCdmaSubscriptionSource(int i, Message message) {
        unimplemented(message);
    }

    public void queryCdmaRoamingPreference(Message message) {
        unimplemented(message);
    }

    public void setCdmaRoamingPreference(int i, Message message) {
        unimplemented(message);
    }

    public void setPhoneType(int i) {
    }

    public void getPreferredVoicePrivacy(Message message) {
        unimplemented(message);
    }

    public void setPreferredVoicePrivacy(boolean z, Message message) {
        unimplemented(message);
    }

    public void setTTYMode(int i, Message message) {
        Rlog.w(LOG_TAG, "Not implemented in SimulatedCommands");
        unimplemented(message);
    }

    public void queryTTYMode(Message message) {
        unimplemented(message);
    }

    public void sendCDMAFeatureCode(String str, Message message) {
        unimplemented(message);
    }

    public void sendCdmaSms(byte[] bArr, Message message) {
        SimulatedCommandsVerifier.getInstance().sendCdmaSms(bArr, message);
        resultSuccess(message, null);
    }

    public void setCdmaBroadcastActivation(boolean z, Message message) {
        unimplemented(message);
    }

    public void getCdmaBroadcastConfig(Message message) {
        unimplemented(message);
    }

    public void setCdmaBroadcastConfig(CdmaSmsBroadcastConfigInfo[] cdmaSmsBroadcastConfigInfoArr, Message message) {
        unimplemented(message);
    }

    public void forceDataDormancy(Message message) {
        unimplemented(message);
    }

    public void setGsmBroadcastActivation(boolean z, Message message) {
        unimplemented(message);
    }

    public void setGsmBroadcastConfig(SmsBroadcastConfigInfo[] smsBroadcastConfigInfoArr, Message message) {
        unimplemented(message);
    }

    public void getGsmBroadcastConfig(Message message) {
        unimplemented(message);
    }

    public void supplyIccPinForApp(String str, String str2, Message message) {
        SimulatedCommandsVerifier.getInstance().supplyIccPinForApp(str, str2, message);
        if (this.mPinCode != null && this.mPinCode.equals(str)) {
            resultSuccess(message, null);
            return;
        }
        Rlog.i(LOG_TAG, "[SimCmd] supplyIccPinForApp: pin failed!");
        Throwable commandException = new CommandException(CommandException.Error.PASSWORD_INCORRECT);
        int[] iArr = new int[1];
        int i = this.mPin1attemptsRemaining - 1;
        this.mPin1attemptsRemaining = i;
        iArr[0] = i < 0 ? 0 : this.mPin1attemptsRemaining;
        resultFail(message, iArr, commandException);
    }

    public void supplyIccPukForApp(String str, String str2, String str3, Message message) {
        unimplemented(message);
    }

    public void supplyIccPin2ForApp(String str, String str2, Message message) {
        unimplemented(message);
    }

    public void supplyIccPuk2ForApp(String str, String str2, String str3, Message message) {
        unimplemented(message);
    }

    public void changeIccPinForApp(String str, String str2, String str3, Message message) {
        SimulatedCommandsVerifier.getInstance().changeIccPinForApp(str, str2, str3, message);
        changeIccPin(str, str2, message);
    }

    public void changeIccPin2ForApp(String str, String str2, String str3, Message message) {
        unimplemented(message);
    }

    public void requestIccSimAuthentication(int i, String str, String str2, Message message) {
        unimplemented(message);
    }

    public void getVoiceRadioTechnology(Message message) {
        SimulatedCommandsVerifier.getInstance().getVoiceRadioTechnology(message);
        resultSuccess(message, new int[]{this.mVoiceRadioTech});
    }

    public void setCellInfoList(List<CellInfo> list) {
        this.mCellInfoList = list;
    }

    public void getCellInfoList(Message message, WorkSource workSource) {
        if (this.mCellInfoList == null) {
            Parcel parcelObtain = Parcel.obtain();
            parcelObtain.writeInt(1);
            parcelObtain.writeInt(1);
            parcelObtain.writeInt(2);
            parcelObtain.writeLong(1453510289108L);
            parcelObtain.writeInt(310);
            parcelObtain.writeInt(android.hardware.radio.V1_0.LastCallFailCause.ACCESS_CLASS_BLOCKED);
            parcelObtain.writeInt(123);
            parcelObtain.writeInt(456);
            parcelObtain.writeInt(99);
            parcelObtain.writeInt(3);
            parcelObtain.setDataPosition(0);
            new ArrayList().add((CellInfoGsm) CellInfoGsm.CREATOR.createFromParcel(parcelObtain));
        }
        resultSuccess(message, this.mCellInfoList);
    }

    public int getRilVersion() {
        return 11;
    }

    public void setCellInfoListRate(int i, Message message, WorkSource workSource) {
        unimplemented(message);
    }

    public void setInitialAttachApn(DataProfile dataProfile, boolean z, Message message) {
    }

    public void setDataProfile(DataProfile[] dataProfileArr, boolean z, Message message) {
    }

    public void setImsRegistrationState(int[] iArr) {
        this.mImsRegState = iArr;
    }

    public void getImsRegistrationState(Message message) {
        if (this.mImsRegState == null) {
            this.mImsRegState = new int[]{1, 0};
        }
        resultSuccess(message, this.mImsRegState);
    }

    public void sendImsCdmaSms(byte[] bArr, int i, int i2, Message message) {
        SimulatedCommandsVerifier.getInstance().sendImsCdmaSms(bArr, i, i2, message);
        resultSuccess(message, new SmsResponse(0, (String) null, 0));
    }

    public void sendImsGsmSms(String str, String str2, int i, int i2, Message message) {
        SimulatedCommandsVerifier.getInstance().sendImsGsmSms(str, str2, i, i2, message);
        resultSuccess(message, new SmsResponse(0, (String) null, 0));
    }

    public void iccOpenLogicalChannel(String str, int i, Message message) {
        SimulatedCommandsVerifier.getInstance().iccOpenLogicalChannel(str, i, message);
        resultSuccess(message, new int[]{this.mChannelId});
    }

    public void iccCloseLogicalChannel(int i, Message message) {
        unimplemented(message);
    }

    public void iccTransmitApduLogicalChannel(int i, int i2, int i3, int i4, int i5, int i6, String str, Message message) {
        SimulatedCommandsVerifier.getInstance().iccTransmitApduLogicalChannel(i, i2, i3, i4, i5, i6, str, message);
        if (this.mIccIoResultForApduLogicalChannel != null) {
            resultSuccess(message, this.mIccIoResultForApduLogicalChannel);
        } else {
            resultFail(message, null, new RuntimeException("IccIoResult not set"));
        }
    }

    public void iccTransmitApduBasicChannel(int i, int i2, int i3, int i4, int i5, String str, Message message) {
        unimplemented(message);
    }

    public void nvReadItem(int i, Message message) {
        unimplemented(message);
    }

    public void nvWriteItem(int i, String str, Message message) {
        unimplemented(message);
    }

    public void nvWriteCdmaPrl(byte[] bArr, Message message) {
        unimplemented(message);
    }

    public void nvResetConfig(int i, Message message) {
        unimplemented(message);
    }

    public void getHardwareConfig(Message message) {
        unimplemented(message);
    }

    public void requestShutdown(Message message) {
        setRadioState(CommandsInterface.RadioState.RADIO_UNAVAILABLE);
    }

    public void startLceService(int i, boolean z, Message message) {
        SimulatedCommandsVerifier.getInstance().startLceService(i, z, message);
    }

    public void stopLceService(Message message) {
        unimplemented(message);
    }

    public void pullLceData(Message message) {
        unimplemented(message);
    }

    public void registerForLceInfo(Handler handler, int i, Object obj) {
        SimulatedCommandsVerifier.getInstance().registerForLceInfo(handler, i, obj);
    }

    public void unregisterForLceInfo(Handler handler) {
        SimulatedCommandsVerifier.getInstance().unregisterForLceInfo(handler);
    }

    public void getModemActivityInfo(Message message) {
        unimplemented(message);
    }

    public void setAllowedCarriers(List<CarrierIdentifier> list, Message message) {
        unimplemented(message);
    }

    public void getAllowedCarriers(Message message) {
        unimplemented(message);
    }

    public void getRadioCapability(Message message) {
        SimulatedCommandsVerifier.getInstance().getRadioCapability(message);
        resultSuccess(message, new RadioCapability(0, 0, 0, 65535, (String) null, 0));
    }

    public void notifySmsStatus(Object obj) {
        if (this.mSmsStatusRegistrant != null) {
            this.mSmsStatusRegistrant.notifyRegistrant(new AsyncResult((Object) null, obj, (Throwable) null));
        }
    }

    public void notifyGsmBroadcastSms(Object obj) {
        if (this.mGsmBroadcastSmsRegistrant != null) {
            this.mGsmBroadcastSmsRegistrant.notifyRegistrant(new AsyncResult((Object) null, obj, (Throwable) null));
        }
    }

    public void notifyIccSmsFull() {
        if (this.mIccSmsFullRegistrant != null) {
            this.mIccSmsFullRegistrant.notifyRegistrant();
        }
    }

    public void notifyEmergencyCallbackMode() {
        if (this.mEmergencyCallbackModeRegistrant != null) {
            this.mEmergencyCallbackModeRegistrant.notifyRegistrant();
        }
    }

    public void setEmergencyCallbackMode(Handler handler, int i, Object obj) {
        SimulatedCommandsVerifier.getInstance().setEmergencyCallbackMode(handler, i, obj);
        super.setEmergencyCallbackMode(handler, i, obj);
    }

    public void notifyExitEmergencyCallbackMode() {
        if (this.mExitEmergencyCallbackModeRegistrants != null) {
            this.mExitEmergencyCallbackModeRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    public void notifyImsNetworkStateChanged() {
        if (this.mImsNetworkStateChangedRegistrants != null) {
            this.mImsNetworkStateChangedRegistrants.notifyRegistrants();
        }
    }

    public void notifyModemReset() {
        if (this.mModemResetRegistrants != null) {
            this.mModemResetRegistrants.notifyRegistrants(new AsyncResult((Object) null, "Test", (Throwable) null));
        }
    }

    public void registerForExitEmergencyCallbackMode(Handler handler, int i, Object obj) {
        SimulatedCommandsVerifier.getInstance().registerForExitEmergencyCallbackMode(handler, i, obj);
        super.registerForExitEmergencyCallbackMode(handler, i, obj);
    }

    public void notifyRadioOn() {
        this.mOnRegistrants.notifyRegistrants();
    }

    @VisibleForTesting
    public void notifyNetworkStateChanged() {
        this.mNetworkStateRegistrants.notifyRegistrants();
    }

    @VisibleForTesting
    public void notifyOtaProvisionStatusChanged() {
        if (this.mOtaProvisionRegistrants != null) {
            this.mOtaProvisionRegistrants.notifyRegistrants(new AsyncResult((Object) null, new int[]{8}, (Throwable) null));
        }
    }

    public void notifySignalStrength() {
        if (this.mSignalStrength == null) {
            this.mSignalStrength = new SignalStrength(20, 0, -1, -1, -1, -1, -1, 99, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        }
        if (this.mSignalStrengthRegistrant != null) {
            this.mSignalStrengthRegistrant.notifyRegistrant(new AsyncResult((Object) null, this.mSignalStrength, (Throwable) null));
        }
    }

    public void setIccCardStatus(IccCardStatus iccCardStatus) {
        this.mIccCardStatus = iccCardStatus;
    }

    public void setIccIoResultForApduLogicalChannel(IccIoResult iccIoResult) {
        this.mIccIoResultForApduLogicalChannel = iccIoResult;
    }

    public void setOpenChannelId(int i) {
        this.mChannelId = i;
    }

    public void setPin1RemainingAttempt(int i) {
        this.mPin1attemptsRemaining = i;
    }

    public void setDataAllowed(boolean z, Message message) {
        log("setDataAllowed = " + z);
        this.mAllowed.set(z);
        resultSuccess(message, null);
    }

    @VisibleForTesting
    public boolean isDataAllowed() {
        return this.mAllowed.get();
    }

    public void registerForPcoData(Handler handler, int i, Object obj) {
    }

    public void unregisterForPcoData(Handler handler) {
    }

    public void registerForModemReset(Handler handler, int i, Object obj) {
        SimulatedCommandsVerifier.getInstance().registerForModemReset(handler, i, obj);
        super.registerForModemReset(handler, i, obj);
    }

    public void sendDeviceState(int i, boolean z, Message message) {
        SimulatedCommandsVerifier.getInstance().sendDeviceState(i, z, message);
        resultSuccess(message, null);
    }

    public void setUnsolResponseFilter(int i, Message message) {
        SimulatedCommandsVerifier.getInstance().setUnsolResponseFilter(i, message);
        resultSuccess(message, null);
    }

    public void setSignalStrengthReportingCriteria(int i, int i2, int[] iArr, int i3, Message message) {
    }

    public void setLinkCapacityReportingCriteria(int i, int i2, int i3, int[] iArr, int[] iArr2, int i4, Message message) {
    }

    public void setSimCardPower(int i, Message message) {
    }

    @VisibleForTesting
    public void triggerRestrictedStateChanged(int i) {
        if (this.mRestrictedStateRegistrant != null) {
            this.mRestrictedStateRegistrant.notifyRegistrant(new AsyncResult((Object) null, Integer.valueOf(i), (Throwable) null));
        }
    }

    public void setOnRestrictedStateChanged(Handler handler, int i, Object obj) {
        super.setOnRestrictedStateChanged(handler, i, obj);
        SimulatedCommandsVerifier.getInstance().setOnRestrictedStateChanged(handler, i, obj);
    }

    public void setRadioPowerFailResponse(boolean z) {
        this.mIsRadioPowerFailResponse = z;
    }

    public void registerForIccRefresh(Handler handler, int i, Object obj) {
        super.registerForIccRefresh(handler, i, obj);
        SimulatedCommandsVerifier.getInstance().registerForIccRefresh(handler, i, obj);
    }

    public void unregisterForIccRefresh(Handler handler) {
        super.unregisterForIccRefresh(handler);
        SimulatedCommandsVerifier.getInstance().unregisterForIccRefresh(handler);
    }

    public void registerForNattKeepaliveStatus(Handler handler, int i, Object obj) {
        SimulatedCommandsVerifier.getInstance().registerForNattKeepaliveStatus(handler, i, obj);
    }

    public void unregisterForNattKeepaliveStatus(Handler handler) {
        SimulatedCommandsVerifier.getInstance().unregisterForNattKeepaliveStatus(handler);
    }

    public void startNattKeepalive(int i, KeepalivePacketData keepalivePacketData, int i2, Message message) {
        SimulatedCommandsVerifier.getInstance().startNattKeepalive(i, keepalivePacketData, i2, message);
    }

    public void stopNattKeepalive(int i, Message message) {
        SimulatedCommandsVerifier.getInstance().stopNattKeepalive(i, message);
    }
}
