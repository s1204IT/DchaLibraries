package com.android.internal.telephony;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.PhoneConstants;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public abstract class CallTracker extends Handler {
    private static final boolean DBG_POLL = false;
    protected static final int EVENT_CALL_STATE_CHANGE = 2;
    protected static final int EVENT_CALL_WAITING_INFO_CDMA = 15;
    protected static final int EVENT_CONFERENCE_RESULT = 11;
    protected static final int EVENT_ECT_RESULT = 13;
    protected static final int EVENT_EXIT_ECM_RESPONSE_CDMA = 14;
    protected static final int EVENT_GET_LAST_CALL_FAIL_CAUSE = 5;
    protected static final int EVENT_OPERATION_COMPLETE = 4;
    protected static final int EVENT_POLL_CALLS_RESULT = 1;
    protected static final int EVENT_RADIO_AVAILABLE = 9;
    protected static final int EVENT_RADIO_NOT_AVAILABLE = 10;
    protected static final int EVENT_REPOLL_AFTER_DELAY = 3;
    protected static final int EVENT_SEPARATE_RESULT = 12;
    protected static final int EVENT_SWITCH_RESULT = 8;
    protected static final int EVENT_THREE_WAY_DIAL_BLANK_FLASH = 20;
    protected static final int EVENT_THREE_WAY_DIAL_L2_RESULT_CDMA = 16;
    static final int POLL_DELAY_MSEC = 250;
    public CommandsInterface mCi;
    protected Message mLastRelevantPoll;
    protected boolean mNeedsPoll;
    protected int mPendingOperations;
    protected ArrayList<Connection> mHandoverConnections = new ArrayList<>();
    protected boolean mNumberConverted = false;
    private final int VALID_COMPARE_LENGTH = 3;

    public abstract PhoneConstants.State getState();

    @Override
    public abstract void handleMessage(Message message);

    protected abstract void handlePollCalls(AsyncResult asyncResult);

    protected abstract void log(String str);

    public abstract void registerForVoiceCallEnded(Handler handler, int i, Object obj);

    public abstract void registerForVoiceCallStarted(Handler handler, int i, Object obj);

    public abstract void unregisterForVoiceCallEnded(Handler handler);

    public abstract void unregisterForVoiceCallStarted(Handler handler);

    protected void pollCallsWhenSafe() {
        this.mNeedsPoll = true;
        if (checkNoOperationsPending()) {
            this.mLastRelevantPoll = obtainMessage(1);
            this.mCi.getCurrentCalls(this.mLastRelevantPoll);
        }
    }

    protected void pollCallsAfterDelay() {
        Message messageObtainMessage = obtainMessage();
        messageObtainMessage.what = 3;
        sendMessageDelayed(messageObtainMessage, 250L);
    }

    protected boolean isCommandExceptionRadioNotAvailable(Throwable th) {
        return th != null && (th instanceof CommandException) && ((CommandException) th).getCommandError() == CommandException.Error.RADIO_NOT_AVAILABLE;
    }

    protected Connection getHoConnection(DriverCall driverCall) {
        for (Connection connection : this.mHandoverConnections) {
            log("getHoConnection - compare number: hoConn= " + connection.toString());
            if (connection.getAddress() != null && connection.getAddress().contains(driverCall.number)) {
                log("getHoConnection: Handover connection match found = " + connection.toString());
                return connection;
            }
        }
        for (Connection connection2 : this.mHandoverConnections) {
            log("getHoConnection: compare state hoConn= " + connection2.toString());
            if (connection2.getStateBeforeHandover() == Call.stateFromDCState(driverCall.state)) {
                log("getHoConnection: Handover connection match found = " + connection2.toString());
                return connection2;
            }
        }
        return null;
    }

    protected void notifySrvccState(Call.SrvccState srvccState, ArrayList<Connection> arrayList) {
        if (srvccState == Call.SrvccState.STARTED && arrayList != null) {
            this.mHandoverConnections.addAll(arrayList);
        } else if (srvccState != Call.SrvccState.COMPLETED) {
            this.mHandoverConnections.clear();
        }
        log("notifySrvccState: mHandoverConnections= " + this.mHandoverConnections.toString());
    }

    protected void handleRadioAvailable() {
        pollCallsWhenSafe();
    }

    protected Message obtainNoPollCompleteMessage(int i) {
        this.mPendingOperations++;
        this.mLastRelevantPoll = null;
        return obtainMessage(i);
    }

    private boolean checkNoOperationsPending() {
        return this.mPendingOperations == 0;
    }

    protected String checkForTestEmergencyNumber(String str) {
        String str2 = SystemProperties.get("ril.test.emergencynumber");
        if (!TextUtils.isEmpty(str2)) {
            String[] strArrSplit = str2.split(":");
            log("checkForTestEmergencyNumber: values.length=" + strArrSplit.length);
            if (strArrSplit.length == 2 && strArrSplit[0].equals(PhoneNumberUtils.stripSeparators(str))) {
                if (this.mCi != null) {
                    this.mCi.testingEmergencyCall();
                }
                log("checkForTestEmergencyNumber: remap " + str + " to " + strArrSplit[1]);
                return strArrSplit[1];
            }
            return str;
        }
        return str;
    }

    protected String convertNumberIfNecessary(Phone phone, String str) {
        if (str == null) {
            return str;
        }
        String[] stringArray = null;
        PersistableBundle config = ((CarrierConfigManager) phone.getContext().getSystemService("carrier_config")).getConfig();
        if (config != null) {
            stringArray = config.getStringArray("dial_string_replace_string_array");
        }
        if (stringArray == null) {
            log("convertNumberIfNecessary convertMaps is null");
            return str;
        }
        log("convertNumberIfNecessary Roaming convertMaps.length " + stringArray.length + " dialNumber.length() " + str.length());
        if (stringArray.length < 1 || str.length() < 3) {
            return str;
        }
        String str2 = "";
        int length = stringArray.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            String str3 = stringArray[i];
            log("convertNumberIfNecessary: " + str3);
            String[] strArrSplit = str3.split(":");
            if (strArrSplit != null && strArrSplit.length > 1) {
                String str4 = strArrSplit[0];
                String str5 = strArrSplit[1];
                if (!TextUtils.isEmpty(str4) && str.equals(str4)) {
                    if (!TextUtils.isEmpty(str5) && str5.endsWith("MDN")) {
                        String line1Number = phone.getLine1Number();
                        if (!TextUtils.isEmpty(line1Number)) {
                            if (!line1Number.startsWith("+")) {
                                line1Number = str5.substring(0, str5.length() - 3) + line1Number;
                            }
                            str2 = line1Number;
                        }
                    } else {
                        str2 = str5;
                    }
                }
            }
            i++;
        }
        if (!TextUtils.isEmpty(str2)) {
            log("convertNumberIfNecessary: convert service number");
            this.mNumberConverted = true;
            return str2;
        }
        return str;
    }

    private boolean compareGid1(Phone phone, String str) {
        String groupIdLevel1 = phone.getGroupIdLevel1();
        int length = str.length();
        boolean z = true;
        if (str == null || str.equals("")) {
            log("compareGid1 serviceGid is empty, return true");
            return true;
        }
        if (groupIdLevel1 == null || groupIdLevel1.length() < length || !groupIdLevel1.substring(0, length).equalsIgnoreCase(str)) {
            log(" gid1 " + groupIdLevel1 + " serviceGid1 " + str);
            z = false;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("compareGid1 is ");
        sb.append(z ? "Same" : "Different");
        log(sb.toString());
        return z;
    }

    public void cleanupCalls() {
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("CallTracker:");
        printWriter.println(" mPendingOperations=" + this.mPendingOperations);
        printWriter.println(" mNeedsPoll=" + this.mNeedsPoll);
        printWriter.println(" mLastRelevantPoll=" + this.mLastRelevantPoll);
    }
}
