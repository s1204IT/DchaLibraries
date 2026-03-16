package com.android.phone;

import android.net.Uri;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyCapabilities;
import com.android.phone.PhoneUtils;
import com.android.phone.common.CallLogAsync;

class CallLogger {
    private static final boolean DBG;
    private static final String LOG_TAG = CallLogger.class.getSimpleName();
    private PhoneGlobals mApplication;
    private CallLogAsync mCallLog;

    static {
        DBG = SystemProperties.getInt("ro.debuggable", 0) == 1;
    }

    public CallLogger(PhoneGlobals application, CallLogAsync callLogAsync) {
        this.mApplication = application;
        this.mCallLog = callLogAsync;
    }

    public void logCall(Connection c, int callLogType) {
        String number = c.getAddress();
        long date = c.getCreateTime();
        long duration = c.getDurationMillis();
        Phone phone = c.getCall().getPhone();
        CallerInfo ci = getCallerInfoFromConnection(c);
        String logNumber = getLogNumber(c, ci);
        if (DBG) {
            log("- onDisconnect(): logNumber set to:" + PhoneUtils.toLogSafePhoneNumber(logNumber) + ", number set to: " + PhoneUtils.toLogSafePhoneNumber(number));
        }
        int presentation = getPresentation(c, ci);
        boolean isOtaspNumber = TelephonyCapabilities.supportsOtasp(phone) && phone.isOtaSpNumber(number);
        if (!isOtaspNumber) {
            logCall(ci, logNumber, presentation, callLogType, date, duration);
        }
    }

    public void logCall(Connection c) {
        int callLogType = 1;
        int cause = c.getDisconnectCause();
        if (c.isIncoming()) {
            if (cause == 1) {
                callLogType = 3;
            }
        } else {
            callLogType = 2;
        }
        log("- callLogType: " + callLogType + ", UserData: " + c.getUserData());
        logCall(c, callLogType);
    }

    public void logCall(CallerInfo ci, String number, int presentation, int callType, long start, long duration) {
    }

    private CallerInfo getCallerInfoFromConnection(Connection conn) {
        Object o = conn.getUserData();
        if (o == null || (o instanceof CallerInfo)) {
            CallerInfo ci = (CallerInfo) o;
            return ci;
        }
        if (o instanceof Uri) {
            CallerInfo ci2 = CallerInfo.getCallerInfo(this.mApplication.getApplicationContext(), (Uri) o);
            return ci2;
        }
        CallerInfo ci3 = ((PhoneUtils.CallerInfoToken) o).currentInfo;
        return ci3;
    }

    private String getLogNumber(Connection conn, CallerInfo callerInfo) {
        String number;
        if (conn.isIncoming()) {
            number = conn.getAddress();
        } else if (callerInfo == null || TextUtils.isEmpty(callerInfo.phoneNumber) || callerInfo.isEmergencyNumber() || callerInfo.isVoiceMailNumber()) {
            if (conn.getCall().getPhone().getPhoneType() == 2) {
                number = conn.getOrigDialString();
            } else {
                number = conn.getAddress();
            }
        } else {
            number = callerInfo.phoneNumber;
        }
        if (number == null) {
            return null;
        }
        int presentation = conn.getNumberPresentation();
        PhoneUtils.modifyForSpecialCnapCases(this.mApplication, callerInfo, number, presentation);
        if (!PhoneNumberUtils.isUriNumber(number)) {
            number = PhoneNumberUtils.stripSeparators(number);
        }
        log("getLogNumber: " + number);
        return number;
    }

    private int getPresentation(Connection conn, CallerInfo callerInfo) {
        int presentation;
        if (callerInfo == null) {
            presentation = conn.getNumberPresentation();
        } else {
            presentation = callerInfo.numberPresentation;
            if (DBG) {
                log("- getPresentation(): ignoring connection's presentation: " + conn.getNumberPresentation());
            }
        }
        if (DBG) {
            log("- getPresentation: presentation: " + presentation);
        }
        return presentation;
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
