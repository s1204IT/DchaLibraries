package com.mediatek.internal.telephony;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Process;
import android.telephony.Rlog;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.DriverCall;
import com.android.internal.telephony.PhoneConstants;

public final class MtkGsmCdmaCallTrackerHelper {
    protected static final int EVENT_CALL_STATE_CHANGE = 2;
    protected static final int EVENT_CALL_WAITING_INFO_CDMA = 15;
    protected static final int EVENT_CONFERENCE_RESULT = 11;
    protected static final int EVENT_DIAL_CALL_RESULT = 1002;
    protected static final int EVENT_ECT_RESULT = 13;
    protected static final int EVENT_EXIT_ECM_RESPONSE_CDMA = 14;
    protected static final int EVENT_GET_LAST_CALL_FAIL_CAUSE = 5;
    protected static final int EVENT_HANG_UP_RESULT = 1003;
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
    static final String LOG_TAG = "GsmCallTkrHlpr";
    private Context mContext;
    private MtkGsmCdmaCallTracker mMtkTracker;
    private boolean mContainForwardingAddress = false;
    private String mForwardingAddress = null;
    private int mForwardingAddressCallId = 0;

    public MtkGsmCdmaCallTrackerHelper(Context context, MtkGsmCdmaCallTracker mtkGsmCdmaCallTracker) {
        this.mContext = context;
        this.mMtkTracker = mtkGsmCdmaCallTracker;
    }

    void logD(String str) {
        Rlog.d(LOG_TAG, str + " (slot " + this.mMtkTracker.mPhone.getPhoneId() + ")");
    }

    void logI(String str) {
        Rlog.i(LOG_TAG, str + " (slot " + this.mMtkTracker.mPhone.getPhoneId() + ")");
    }

    void logW(String str) {
        Rlog.w(LOG_TAG, str + " (slot " + this.mMtkTracker.mPhone.getPhoneId() + ")");
    }

    public void LogerMessage(int i) {
        switch (i) {
            case 1:
                logD("handle EVENT_POLL_CALLS_RESULT");
                break;
            case 2:
                logD("handle EVENT_CALL_STATE_CHANGE");
                break;
            case 3:
                logD("handle EVENT_REPOLL_AFTER_DELAY");
                break;
            case 4:
                logD("handle EVENT_OPERATION_COMPLETE");
                break;
            case 5:
                logD("handle EVENT_GET_LAST_CALL_FAIL_CAUSE");
                break;
            default:
                switch (i) {
                    case 8:
                        logD("handle EVENT_SWITCH_RESULT");
                        break;
                    case 9:
                        logD("handle EVENT_RADIO_AVAILABLE");
                        break;
                    case 10:
                        logD("handle EVENT_RADIO_NOT_AVAILABLE");
                        break;
                    case 11:
                        logD("handle EVENT_CONFERENCE_RESULT");
                        break;
                    case 12:
                        logD("handle EVENT_SEPARATE_RESULT");
                        break;
                    case 13:
                        logD("handle EVENT_ECT_RESULT");
                        break;
                    default:
                        switch (i) {
                            case 1000:
                                logD("handle EVENT_INCOMING_CALL_INDICATION");
                                break;
                            case 1001:
                                logD("handle EVENT_RADIO_OFF_OR_NOT_AVAILABLE");
                                break;
                            case 1002:
                                logD("handle EVENT_DIAL_CALL_RESULT");
                                break;
                            case 1003:
                                logD("handle EVENT_HANG_UP_RESULT");
                                break;
                            default:
                                logD("handle XXXXX");
                                break;
                        }
                        break;
                }
                break;
        }
    }

    public void LogState() {
        int maxConnections = this.mMtkTracker.getMaxConnections();
        int i = 0;
        for (int i2 = 0; i2 < maxConnections; i2++) {
            if (this.mMtkTracker.mConnections[i2] != null) {
                i++;
                logI("* conn id " + (this.mMtkTracker.mConnections[i2].mIndex + 1) + " existed");
            }
        }
        logI("* GsmCT has " + i + " connection");
    }

    public int getCurrentTotalConnections() {
        int i = 0;
        for (int i2 = 0; i2 < this.mMtkTracker.getMaxConnections(); i2++) {
            if (this.mMtkTracker.mConnections[i2] != null) {
                i++;
            }
        }
        return i;
    }

    public void CallIndicationProcess(AsyncResult asyncResult) {
        String[] strArr = (String[]) asyncResult.result;
        int i = 0;
        int i2 = Integer.parseInt(strArr[0]);
        Integer.parseInt(strArr[3]);
        int i3 = Integer.parseInt(strArr[4]);
        logD("CallIndicationProcess 0 callId " + i2 + " seqNumber " + i3);
        this.mForwardingAddress = null;
        if (strArr[5] != null && strArr[5].length() > 0) {
            this.mContainForwardingAddress = false;
            this.mForwardingAddress = strArr[5];
            this.mForwardingAddressCallId = i2;
            logD("EAIC message contains forwarding address - " + this.mForwardingAddress + "," + i2);
        }
        if (this.mMtkTracker.mState == PhoneConstants.State.RINGING) {
            i = 1;
        }
        if (i == 0) {
            int iMyPid = Process.myPid();
            Process.setThreadPriority(iMyPid, -10);
            logD("Adjust the priority of process - " + iMyPid + " to " + Process.getThreadPriority(iMyPid));
            if (this.mForwardingAddress != null) {
                this.mContainForwardingAddress = true;
            }
            this.mMtkTracker.mMtkCi.setCallIndication(i, i2, i3, null);
        }
        if (i == 1) {
            DriverCall driverCall = new DriverCall();
            driverCall.isMT = true;
            driverCall.index = i2;
            driverCall.state = DriverCall.State.WAITING;
            this.mMtkTracker.mMtkCi.setCallIndication(i, i2, i3, null);
            driverCall.isVoice = true;
            driverCall.number = strArr[1];
            driverCall.numberPresentation = 1;
            driverCall.TOA = Integer.parseInt(strArr[2]);
            driverCall.number = MtkPhoneNumberUtils.stringFromStringAndTOA(driverCall.number, driverCall.TOA);
            new MtkGsmCdmaConnection(this.mMtkTracker.mPhone, driverCall, this.mMtkTracker, i2).onReplaceDisconnect(1);
        }
    }

    public void CallIndicationEnd() {
        int iMyPid = Process.myPid();
        if (Process.getThreadPriority(iMyPid) != 0) {
            Process.setThreadPriority(iMyPid, 0);
            logD("Current priority = " + Process.getThreadPriority(iMyPid));
        }
    }

    public void clearForwardingAddressVariables(int i) {
        if (this.mContainForwardingAddress && this.mForwardingAddressCallId == i + 1) {
            this.mContainForwardingAddress = false;
            this.mForwardingAddress = null;
            this.mForwardingAddressCallId = 0;
        }
    }

    public void setForwardingAddressToConnection(int i, Connection connection) {
        if (this.mContainForwardingAddress && this.mForwardingAddress != null && this.mForwardingAddressCallId == i + 1) {
            MtkGsmCdmaConnection mtkGsmCdmaConnection = (MtkGsmCdmaConnection) connection;
            mtkGsmCdmaConnection.setForwardingAddress(this.mForwardingAddress);
            logD("Store forwarding address - " + this.mForwardingAddress);
            logD("Get forwarding address - " + mtkGsmCdmaConnection.getForwardingAddress());
            clearForwardingAddressVariables(i);
        }
    }
}
