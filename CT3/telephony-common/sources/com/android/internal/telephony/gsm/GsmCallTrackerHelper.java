package com.android.internal.telephony.gsm;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Process;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.DriverCall;
import com.android.internal.telephony.GsmCdmaCall;
import com.android.internal.telephony.GsmCdmaCallTracker;
import com.android.internal.telephony.GsmCdmaConnection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;

public final class GsmCallTrackerHelper {
    protected static final int EVENT_CALL_INFO_INDICATION = 1007;
    protected static final int EVENT_CALL_STATE_CHANGE = 2;
    protected static final int EVENT_CALL_WAITING_INFO_CDMA = 15;
    protected static final int EVENT_CONFERENCE_RESULT = 11;
    protected static final int EVENT_DIAL_CALL_RESULT = 1003;
    protected static final int EVENT_ECONF_RESULT_INDICATION = 1005;
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
    protected static final int EVENT_RETRIEVE_HELD_CALL_RESULT = 1006;
    protected static final int EVENT_SEPARATE_RESULT = 12;
    protected static final int EVENT_SWITCH_RESULT = 8;
    protected static final int EVENT_THREE_WAY_DIAL_BLANK_FLASH = 20;
    protected static final int EVENT_THREE_WAY_DIAL_L2_RESULT_CDMA = 16;
    static final String LOG_TAG = "GsmCallTrackerHelper";
    private static final boolean MTK_SWITCH_ANTENNA_SUPPORT = SystemProperties.get("ro.mtk_switch_antenna").equals("1");
    private Context mContext;
    private GsmCdmaCallTracker mTracker;
    private boolean hasPendingHangupRequest = false;
    private int pendingHangupRequest = 0;
    private int pendingMTCallId = 0;
    private int pendingMTSeqNum = 0;
    private boolean mContainForwardingAddress = false;
    private String mForwardingAddress = null;
    private int mForwardingAddressCallId = 0;

    public GsmCallTrackerHelper(Context context, GsmCdmaCallTracker tracker) {
        this.mContext = context;
        this.mTracker = tracker;
    }

    void logD(String msg) {
        Rlog.d(LOG_TAG, msg + " (slot " + this.mTracker.mPhone.getPhoneId() + ")");
    }

    void logI(String msg) {
        Rlog.i(LOG_TAG, msg + " (slot " + this.mTracker.mPhone.getPhoneId() + ")");
    }

    void logW(String msg) {
        Rlog.w(LOG_TAG, msg + " (slot " + this.mTracker.mPhone.getPhoneId() + ")");
    }

    public void LogerMessage(int msgType) {
        switch (msgType) {
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
            case 1000:
                logD("handle EVENT_INCOMING_CALL_INDICATION");
                break;
            case 1001:
                logD("handle EVENT_RADIO_OFF_OR_NOT_AVAILABLE");
                break;
            case 1002:
                logD("handle EVENT_HANG_UP_RESULT");
                break;
            case 1003:
                logD("handle EVENT_DIAL_CALL_RESULT");
                break;
            default:
                logD("handle XXXXX");
                break;
        }
    }

    public void LogState() {
        int count = 0;
        for (int i = 0; i < 19; i++) {
            if (this.mTracker.mConnections[i] != null) {
                int callId = this.mTracker.mConnections[i].mIndex + 1;
                count++;
                logI("* conn id " + callId + " existed");
            }
        }
        logI("* GsmCT has " + count + " connection");
    }

    public boolean ForceReleaseConnection(GsmCdmaCall call, GsmCdmaCall hangupCall) {
        if (call.mState == Call.State.DISCONNECTING) {
            for (int i = 0; i < call.mConnections.size(); i++) {
                GsmCdmaConnection cn = (GsmCdmaConnection) call.mConnections.get(i);
                this.mTracker.mCi.forceReleaseCall(cn.mIndex + 1, this.mTracker.obtainCompleteMessage());
            }
            if (call == hangupCall) {
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean ForceReleaseAllConnection(GsmCdmaCall call) {
        boolean forceReleaseFg = ForceReleaseConnection(this.mTracker.mForegroundCall, call);
        boolean forceReleaseBg = ForceReleaseConnection(this.mTracker.mBackgroundCall, call);
        boolean forceReleaseRing = ForceReleaseConnection(this.mTracker.mRingingCall, call);
        if (forceReleaseFg || forceReleaseBg || forceReleaseRing) {
            logD("hangup(GsmCdmaCall)Hang up disconnecting call, return directly");
            return true;
        }
        return false;
    }

    public boolean ForceReleaseNotRingingConnection(GsmCdmaCall call) {
        boolean forceReleaseFg = ForceReleaseConnection(this.mTracker.mForegroundCall, call);
        boolean forceReleaseBg = ForceReleaseConnection(this.mTracker.mBackgroundCall, call);
        if (!forceReleaseFg && !forceReleaseBg) {
            return true;
        }
        logD("hangup(GsmCdmaCall)Hang up disconnecting call, return directly");
        return true;
    }

    public boolean hangupConnectionByIndex(GsmCdmaCall c, int index) {
        int count = c.mConnections.size();
        for (int i = 0; i < count; i++) {
            GsmCdmaConnection cn = (GsmCdmaConnection) c.mConnections.get(i);
            if (cn.getState() == Call.State.DISCONNECTED) {
                logD("hangupConnectionByIndex: hangup a DISCONNECTED conn");
            } else {
                try {
                    if (cn.getGsmCdmaIndex() == index) {
                        this.mTracker.mCi.hangupConnection(index, this.mTracker.obtainCompleteMessage());
                        return true;
                    }
                    continue;
                } catch (CallStateException e) {
                    logW("GsmCallTracker hangupConnectionByIndex() on absent connection ");
                }
            }
        }
        return false;
    }

    public boolean hangupFgConnectionByIndex(int index) {
        return hangupConnectionByIndex(this.mTracker.mForegroundCall, index);
    }

    public boolean hangupBgConnectionByIndex(int index) {
        return hangupConnectionByIndex(this.mTracker.mBackgroundCall, index);
    }

    public boolean hangupRingingConnectionByIndex(int index) {
        return hangupConnectionByIndex(this.mTracker.mRingingCall, index);
    }

    public int getCurrentTotalConnections() {
        int count = 0;
        for (int i = 0; i < 19; i++) {
            if (this.mTracker.mConnections[i] != null) {
                count++;
            }
        }
        return count;
    }

    public void PendingHangupRequestUpdate() {
        logD("updatePendingHangupRequest - " + this.mTracker.mHangupPendingMO + this.hasPendingHangupRequest + this.pendingHangupRequest);
        if (!this.mTracker.mHangupPendingMO || !this.hasPendingHangupRequest) {
            return;
        }
        this.pendingHangupRequest--;
        if (this.pendingHangupRequest != 0) {
            return;
        }
        this.hasPendingHangupRequest = false;
    }

    public void PendingHangupRequestInc() {
        this.hasPendingHangupRequest = true;
        this.pendingHangupRequest++;
    }

    public void PendingHangupRequestDec() {
        if (!this.hasPendingHangupRequest) {
            return;
        }
        this.pendingHangupRequest--;
        if (this.pendingHangupRequest != 0) {
            return;
        }
        this.hasPendingHangupRequest = false;
    }

    public void PendingHangupRequestReset() {
        this.hasPendingHangupRequest = false;
        this.pendingHangupRequest = 0;
    }

    public boolean hasPendingHangupRequest() {
        return this.hasPendingHangupRequest;
    }

    public int CallIndicationGetId() {
        return this.pendingMTCallId;
    }

    public int CallIndicationGetSeqNo() {
        return this.pendingMTSeqNum;
    }

    public void CallIndicationProcess(AsyncResult ar) {
        int mode = 0;
        String[] incomingCallInfo = (String[]) ar.result;
        int callId = Integer.parseInt(incomingCallInfo[0]);
        int callMode = Integer.parseInt(incomingCallInfo[3]);
        int seqNumber = Integer.parseInt(incomingCallInfo[4]);
        logD("CallIndicationProcess 0 pendingMTCallId " + this.pendingMTCallId + " pendingMTSeqNum " + this.pendingMTSeqNum);
        String tbcwMode = this.mTracker.mPhone.getSystemProperty("persist.radio.terminal-based.cw", "disabled_tbcw");
        Call.State fgState = this.mTracker.mForegroundCall == null ? Call.State.IDLE : this.mTracker.mForegroundCall.getState();
        Call.State bgState = this.mTracker.mBackgroundCall == null ? Call.State.IDLE : this.mTracker.mBackgroundCall.getState();
        logD("PROPERTY_TERMINAL_BASED_CALL_WAITING_MODE = " + tbcwMode + " , ForgroundCall State = " + fgState + " , BackgroundCall State = " + bgState);
        if ("enabled_tbcw_off".equals(tbcwMode) && (fgState == Call.State.ACTIVE || bgState == Call.State.HOLDING)) {
            logD("PROPERTY_TERMINAL_BASED_CALL_WAITING_MODE = TERMINAL_BASED_CALL_WAITING_ENABLED_OFF. Reject the call as UDUB ");
            this.mTracker.mCi.setCallIndication(1, callId, seqNumber, null);
            return;
        }
        this.mForwardingAddress = null;
        if (incomingCallInfo[5] != null && incomingCallInfo[5].length() > 0) {
            this.mContainForwardingAddress = false;
            this.mForwardingAddress = incomingCallInfo[5];
            this.mForwardingAddressCallId = callId;
            logD("EAIC message contains forwarding address - " + this.mForwardingAddress + "," + callId);
        }
        if (this.mTracker.mState == PhoneConstants.State.RINGING) {
            mode = 1;
        } else if (this.mTracker.mState == PhoneConstants.State.OFFHOOK) {
            if (callMode == 10) {
                int i = 0;
                while (true) {
                    if (i >= 19) {
                        break;
                    }
                    if (this.mTracker.mConnections[i] == null) {
                        i++;
                    } else {
                        mode = 1;
                        break;
                    }
                }
            } else if (callMode == 0) {
                int i2 = 0;
                while (true) {
                    if (i2 < 19) {
                        Connection cn = this.mTracker.mConnections[i2];
                        if (cn == null || !cn.isVideo()) {
                            i2++;
                        } else {
                            mode = 1;
                            break;
                        }
                    } else {
                        break;
                    }
                }
            } else {
                mode = 1;
            }
        }
        if (mode == 0) {
            this.pendingMTCallId = callId;
            this.pendingMTSeqNum = seqNumber;
            this.mTracker.mVoiceCallIncomingIndicationRegistrants.notifyRegistrants();
            logD("notify mVoiceCallIncomingIndicationRegistrants " + this.pendingMTCallId + " " + this.pendingMTSeqNum);
        }
        if (mode != 1) {
            return;
        }
        DriverCall dc = new DriverCall();
        dc.isMT = true;
        dc.index = callId;
        dc.state = DriverCall.State.WAITING;
        this.mTracker.mCi.setCallIndication(mode, callId, seqNumber, null);
        if (callMode == 0) {
            dc.isVoice = true;
            dc.isVideo = false;
        } else if (callMode == 10) {
            dc.isVoice = false;
            dc.isVideo = true;
        } else {
            dc.isVoice = false;
            dc.isVideo = false;
        }
        dc.number = incomingCallInfo[1];
        dc.numberPresentation = 1;
        dc.TOA = Integer.parseInt(incomingCallInfo[2]);
        dc.number = PhoneNumberUtils.stringFromStringAndTOA(dc.number, dc.TOA);
        new GsmCdmaConnection(this.mTracker.mPhone, dc, this.mTracker, callId).onReplaceDisconnect(1);
    }

    public void CallIndicationResponse(boolean accept) {
        int mode;
        logD("setIncomingCallIndicationResponse 0 pendingMTCallId " + this.pendingMTCallId + " pendingMTSeqNum " + this.pendingMTSeqNum);
        if (accept) {
            int pid = Process.myPid();
            mode = 0;
            Process.setThreadPriority(pid, -10);
            logD("Adjust the priority of process - " + pid + " to " + Process.getThreadPriority(pid));
            if (this.mForwardingAddress != null) {
                this.mContainForwardingAddress = true;
            }
        } else {
            mode = 1;
        }
        this.mTracker.mCi.setCallIndication(mode, this.pendingMTCallId, this.pendingMTSeqNum, null);
        this.pendingMTCallId = 0;
        this.pendingMTSeqNum = 0;
    }

    public void CallIndicationEnd() {
        int pid = Process.myPid();
        if (Process.getThreadPriority(pid) == 0) {
            return;
        }
        Process.setThreadPriority(pid, 0);
        logD("Current priority = " + Process.getThreadPriority(pid));
    }

    public void clearForwardingAddressVariables(int index) {
        if (!this.mContainForwardingAddress || this.mForwardingAddressCallId != index + 1) {
            return;
        }
        this.mContainForwardingAddress = false;
        this.mForwardingAddress = null;
        this.mForwardingAddressCallId = 0;
    }

    public void setForwardingAddressToConnection(int index, Connection conn) {
        if (!this.mContainForwardingAddress || this.mForwardingAddress == null || this.mForwardingAddressCallId != index + 1) {
            return;
        }
        conn.setForwardingAddress(this.mForwardingAddress);
        logD("Store forwarding address - " + this.mForwardingAddress);
        logD("Get forwarding address - " + conn.getForwardingAddress());
        clearForwardingAddressVariables(index);
    }

    private boolean shouldNotifyMtCall() {
        if (MTK_SWITCH_ANTENNA_SUPPORT) {
            logD("shouldNotifyMtCall, mTracker.mPhone:" + this.mTracker.mPhone);
            Phone[] phones = PhoneFactory.getPhones();
            for (Phone phone : phones) {
                logD("phone:" + phone + ", state:" + phone.getState());
                if (phone.getState() != PhoneConstants.State.IDLE && phone != this.mTracker.mPhone) {
                    logD("shouldNotifyMtCall, another phone active, phone:" + phone + ", state:" + phone.getState());
                    return false;
                }
            }
            return true;
        }
        return true;
    }
}
