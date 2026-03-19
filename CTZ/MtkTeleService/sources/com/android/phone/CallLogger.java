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
    private static final boolean VDBG = false;
    private PhoneGlobals mApplication;
    private CallLogAsync mCallLog;

    static {
        DBG = SystemProperties.getInt("ro.debuggable", 0) == 1;
    }

    public CallLogger(PhoneGlobals phoneGlobals, CallLogAsync callLogAsync) {
        this.mApplication = phoneGlobals;
        this.mCallLog = callLogAsync;
    }

    public void logCall(Connection connection, int i) {
        String address = connection.getAddress();
        long createTime = connection.getCreateTime();
        long durationMillis = connection.getDurationMillis();
        Phone phone = connection.getCall().getPhone();
        CallerInfo callerInfoFromConnection = getCallerInfoFromConnection(connection);
        String logNumber = getLogNumber(connection, callerInfoFromConnection);
        if (DBG) {
            log("- onDisconnect(): logNumber set to:" + PhoneUtils.toLogSafePhoneNumber(logNumber) + ", number set to: " + PhoneUtils.toLogSafePhoneNumber(address));
        }
        int presentation = getPresentation(connection, callerInfoFromConnection);
        if (!(TelephonyCapabilities.supportsOtasp(phone) && phone.isOtaSpNumber(address))) {
            logCall(callerInfoFromConnection, logNumber, presentation, i, createTime, durationMillis);
        }
    }

    public void logCall(Connection connection) {
        int i;
        int disconnectCause = connection.getDisconnectCause();
        if (connection.isIncoming()) {
            i = 1;
            if (disconnectCause == 1) {
                i = 3;
            }
        } else {
            i = 2;
        }
        logCall(connection, i);
    }

    public void logCall(CallerInfo callerInfo, String str, int i, int i2, long j, long j2) {
    }

    private CallerInfo getCallerInfoFromConnection(Connection connection) {
        Object userData = connection.getUserData();
        if (userData == null || (userData instanceof CallerInfo)) {
            return (CallerInfo) userData;
        }
        if (userData instanceof Uri) {
            return CallerInfo.getCallerInfo(this.mApplication.getApplicationContext(), (Uri) userData);
        }
        return ((PhoneUtils.CallerInfoToken) userData).currentInfo;
    }

    private String getLogNumber(Connection connection, CallerInfo callerInfo) {
        String address;
        if (connection.isIncoming()) {
            address = connection.getAddress();
        } else if (callerInfo == null || TextUtils.isEmpty(callerInfo.phoneNumber) || callerInfo.isEmergencyNumber() || callerInfo.isVoiceMailNumber()) {
            if (connection.getCall().getPhone().getPhoneType() == 2) {
                address = connection.getOrigDialString();
            } else {
                address = connection.getAddress();
            }
        } else {
            address = callerInfo.phoneNumber;
        }
        if (address == null) {
            return null;
        }
        PhoneUtils.modifyForSpecialCnapCases(this.mApplication, callerInfo, address, connection.getNumberPresentation());
        if (!PhoneNumberUtils.isUriNumber(address)) {
            return PhoneNumberUtils.stripSeparators(address);
        }
        return address;
    }

    private int getPresentation(Connection connection, CallerInfo callerInfo) {
        int numberPresentation;
        if (callerInfo == null) {
            numberPresentation = connection.getNumberPresentation();
        } else {
            int i = callerInfo.numberPresentation;
            if (DBG) {
                log("- getPresentation(): ignoring connection's presentation: " + connection.getNumberPresentation());
            }
            numberPresentation = i;
        }
        if (DBG) {
            log("- getPresentation: presentation: " + numberPresentation);
        }
        return numberPresentation;
    }

    private void log(String str) {
        Log.d(LOG_TAG, str);
    }
}
