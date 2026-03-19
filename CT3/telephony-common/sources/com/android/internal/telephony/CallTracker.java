package com.android.internal.telephony;

import android.R;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public abstract class CallTracker extends Handler {
    private static final boolean DBG_POLL;
    protected static final int EVENT_CALL_REDIAL_STATE = 1006;
    protected static final int EVENT_CALL_STATE_CHANGE = 2;
    protected static final int EVENT_CALL_WAITING_INFO_CDMA = 15;
    protected static final int EVENT_CDMA_CALL_ACCEPTED = 1005;
    protected static final int EVENT_CONFERENCE_RESULT = 11;
    protected static final int EVENT_DIAL_CALL_RESULT = 1003;
    protected static final int EVENT_ECONF_SRVCC_INDICATION = 1004;
    protected static final int EVENT_ECT_RESULT = 13;
    protected static final int EVENT_EXIT_ECM_RESPONSE_CDMA = 14;
    protected static final int EVENT_GET_LAST_CALL_FAIL_CAUSE = 5;
    protected static final int EVENT_HANG_UP_RESULT = 1002;
    protected static final int EVENT_INCOMING_CALL_INDICATION = 1000;
    protected static final int EVENT_MTK_BASE = 1000;
    protected static final int EVENT_OPERATION_COMPLETE = 4;
    protected static final int EVENT_POLL_CALLS_RESULT = 1;
    protected static final int EVENT_RADIO_AVAILABLE = 9;
    protected static final int EVENT_RADIO_NOT_AVAILABLE = 10;
    protected static final int EVENT_RADIO_OFF_OR_NOT_AVAILABLE = 1001;
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
    protected boolean mNeedWaitImsEConfSrvcc = false;
    protected Connection mImsConfHostConnection = null;

    public abstract PhoneConstants.State getState();

    @Override
    public abstract void handleMessage(Message message);

    protected abstract void handlePollCalls(AsyncResult asyncResult);

    protected abstract void log(String str);

    public abstract void registerForVoiceCallEnded(Handler handler, int i, Object obj);

    public abstract void registerForVoiceCallStarted(Handler handler, int i, Object obj);

    public abstract void unregisterForVoiceCallEnded(Handler handler);

    public abstract void unregisterForVoiceCallStarted(Handler handler);

    static {
        DBG_POLL = SystemProperties.getInt("persist.log.tag.tel_dbg", 0) == 1;
    }

    public enum RedialState {
        REDIAL_NONE,
        REDIAL_TO_MD1,
        REDIAL_TO_MD3,
        REDIAL_CHANGE_GLOBAL_MODE;

        public static RedialState[] valuesCustom() {
            return values();
        }
    }

    public static RedialState redialStateFromInt(int state) {
        switch (state) {
        }
        return RedialState.REDIAL_NONE;
    }

    protected void pollCallsWhenSafe() {
        this.mNeedsPoll = true;
        if (!checkNoOperationsPending()) {
            return;
        }
        this.mLastRelevantPoll = obtainMessage(1);
        this.mCi.getCurrentCalls(this.mLastRelevantPoll);
    }

    protected void pollCallsAfterDelay() {
        Message msg = obtainMessage();
        msg.what = 3;
        sendMessageDelayed(msg, 250L);
    }

    protected boolean isCommandExceptionRadioNotAvailable(Throwable e) {
        return e != null && (e instanceof CommandException) && ((CommandException) e).getCommandError() == CommandException.Error.RADIO_NOT_AVAILABLE;
    }

    protected Connection getHoConnection(DriverCall dc) {
        log("SRVCC: getHoConnection() with dc, number = " + dc.number + " state = " + dc.state);
        if (dc.number != null && !dc.number.isEmpty()) {
            for (Connection hoConn : this.mHandoverConnections) {
                log("getHoConnection - compare number: hoConn= " + hoConn.toString());
                if (hoConn.getAddress() != null && hoConn.getAddress().contains(dc.number)) {
                    log("getHoConnection: Handover connection match found = " + hoConn.toString());
                    return hoConn;
                }
            }
        }
        for (Connection hoConn2 : this.mHandoverConnections) {
            log("getHoConnection: compare state hoConn= " + hoConn2.toString());
            if (hoConn2.getStateBeforeHandover() == Call.stateFromDCState(dc.state)) {
                log("getHoConnection: Handover connection match found = " + hoConn2.toString());
                return hoConn2;
            }
        }
        return null;
    }

    protected void notifySrvccState(Call.SrvccState state, ArrayList<Connection> c) {
        if (state == Call.SrvccState.STARTED && c != null) {
            this.mHandoverConnections.addAll(c);
            for (Connection conn : this.mHandoverConnections) {
                if (conn.isMultiparty() && (conn instanceof ImsPhoneConnection) && ((ImsPhoneConnection) conn).isConferenceHost()) {
                    log("srvcc: mNeedWaitImsEConfSrvcc set True");
                    this.mNeedWaitImsEConfSrvcc = true;
                    this.mImsConfHostConnection = conn;
                }
            }
        } else if (state != Call.SrvccState.COMPLETED) {
            this.mHandoverConnections.clear();
        }
        log("notifySrvccState: mHandoverConnections= " + this.mHandoverConnections.toString());
    }

    protected void handleRadioAvailable() {
        pollCallsWhenSafe();
    }

    protected Message obtainNoPollCompleteMessage(int what) {
        this.mPendingOperations++;
        this.mLastRelevantPoll = null;
        return obtainMessage(what);
    }

    private boolean checkNoOperationsPending() {
        if (DBG_POLL) {
            log("checkNoOperationsPending: pendingOperations=" + this.mPendingOperations);
        }
        return this.mPendingOperations == 0;
    }

    protected String checkForTestEmergencyNumber(String dialString) {
        String testEn = SystemProperties.get("ril.test.emergencynumber");
        if (DBG_POLL) {
            log("checkForTestEmergencyNumber: dialString=" + dialString + " testEn=" + testEn);
        }
        if (!TextUtils.isEmpty(testEn)) {
            String[] values = testEn.split(":");
            log("checkForTestEmergencyNumber: values.length=" + values.length);
            if (values.length == 2 && values[0].equals(PhoneNumberUtils.stripSeparators(dialString))) {
                if (this.mCi != null) {
                    this.mCi.testingEmergencyCall();
                }
                log("checkForTestEmergencyNumber: remap " + dialString + " to " + values[1]);
                return values[1];
            }
            return dialString;
        }
        return dialString;
    }

    protected String convertNumberIfNecessary(Phone phone, String dialNumber) {
        if (dialNumber == null) {
            return dialNumber;
        }
        String[] convertMaps = phone.getContext().getResources().getStringArray(R.array.config_default_vm_number);
        log("convertNumberIfNecessary Roaming convertMaps.length " + convertMaps.length + " dialNumber.length() " + dialNumber.length());
        if (convertMaps.length < 1 || dialNumber.length() < 3) {
            return dialNumber;
        }
        String outNumber = UsimPBMemInfo.STRING_NOT_SET;
        boolean needConvert = false;
        for (String convertMap : convertMaps) {
            log("convertNumberIfNecessary: " + convertMap);
            String[] entry = convertMap.split(":");
            if (entry.length > 1) {
                String[] tmpArray = entry[1].split(",");
                if (!TextUtils.isEmpty(entry[0]) && dialNumber.equals(entry[0])) {
                    if (tmpArray.length < 2 || TextUtils.isEmpty(tmpArray[1])) {
                        if (outNumber.isEmpty()) {
                            needConvert = true;
                        }
                    } else if (compareGid1(phone, tmpArray[1])) {
                        needConvert = true;
                    }
                    if (needConvert) {
                        if (TextUtils.isEmpty(tmpArray[0]) || !tmpArray[0].endsWith("MDN")) {
                            outNumber = tmpArray[0];
                        } else {
                            String mdn = phone.getLine1Number();
                            if (!TextUtils.isEmpty(mdn)) {
                                outNumber = mdn.startsWith("+") ? mdn : tmpArray[0].substring(0, tmpArray[0].length() - 3) + mdn;
                            }
                        }
                        needConvert = false;
                    }
                }
            }
        }
        if (TextUtils.isEmpty(outNumber)) {
            return dialNumber;
        }
        log("convertNumberIfNecessary: convert service number");
        this.mNumberConverted = true;
        return outNumber;
    }

    private boolean compareGid1(Phone phone, String serviceGid1) {
        boolean zEqualsIgnoreCase = false;
        String gid1 = phone.getGroupIdLevel1();
        int gid_length = serviceGid1.length();
        boolean ret = true;
        if (serviceGid1 == null || serviceGid1.equals(UsimPBMemInfo.STRING_NOT_SET)) {
            log("compareGid1 serviceGid is empty, return true");
            return true;
        }
        if (gid1 != null && gid1.length() >= gid_length) {
            zEqualsIgnoreCase = gid1.substring(0, gid_length).equalsIgnoreCase(serviceGid1);
        }
        if (!zEqualsIgnoreCase) {
            log(" gid1 " + gid1 + " serviceGid1 " + serviceGid1);
            ret = false;
        }
        log("compareGid1 is " + (ret ? "Same" : "Different"));
        return ret;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("CallTracker:");
        pw.println(" mPendingOperations=" + this.mPendingOperations);
        pw.println(" mNeedsPoll=" + this.mNeedsPoll);
        pw.println(" mLastRelevantPoll=" + this.mLastRelevantPoll);
    }
}
