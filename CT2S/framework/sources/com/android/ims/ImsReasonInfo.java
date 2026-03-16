package com.android.ims;

import android.os.Parcel;
import android.os.Parcelable;

public class ImsReasonInfo implements Parcelable {
    public static final int CODE_BLACKLISTED_CALL_ID = 506;
    public static final int CODE_ECBM_NOT_SUPPORTED = 901;
    public static final int CODE_LOCAL_CALL_BUSY = 142;
    public static final int CODE_LOCAL_CALL_CS_RETRY_REQUIRED = 146;
    public static final int CODE_LOCAL_CALL_DECLINE = 143;
    public static final int CODE_LOCAL_CALL_EXCEEDED = 141;
    public static final int CODE_LOCAL_CALL_RESOURCE_RESERVATION_FAILED = 145;
    public static final int CODE_LOCAL_CALL_TERMINATED = 148;
    public static final int CODE_LOCAL_CALL_VCC_ON_PROGRESSING = 144;
    public static final int CODE_LOCAL_CALL_VOLTE_RETRY_REQUIRED = 147;
    public static final int CODE_LOCAL_ILLEGAL_ARGUMENT = 101;
    public static final int CODE_LOCAL_ILLEGAL_STATE = 102;
    public static final int CODE_LOCAL_IMS_SERVICE_DOWN = 106;
    public static final int CODE_LOCAL_INTERNAL_ERROR = 103;
    public static final int CODE_LOCAL_LOW_BATTERY = 112;
    public static final int CODE_LOCAL_NETWORK_IP_CHANGED = 124;
    public static final int CODE_LOCAL_NETWORK_NO_LTE_COVERAGE = 122;
    public static final int CODE_LOCAL_NETWORK_NO_SERVICE = 121;
    public static final int CODE_LOCAL_NETWORK_ROAMING = 123;
    public static final int CODE_LOCAL_NOT_REGISTERED = 132;
    public static final int CODE_LOCAL_NO_PENDING_CALL = 107;
    public static final int CODE_LOCAL_POWER_OFF = 111;
    public static final int CODE_LOCAL_SERVICE_UNAVAILABLE = 131;
    public static final int CODE_LOW_BATTERY = 505;
    public static final int CODE_MEDIA_INIT_FAILED = 401;
    public static final int CODE_MEDIA_NOT_ACCEPTABLE = 403;
    public static final int CODE_MEDIA_NO_DATA = 402;
    public static final int CODE_MEDIA_UNSPECIFIED = 404;
    public static final int CODE_SIP_BAD_ADDRESS = 337;
    public static final int CODE_SIP_BAD_REQUEST = 331;
    public static final int CODE_SIP_BUSY = 338;
    public static final int CODE_SIP_CLIENT_ERROR = 342;
    public static final int CODE_SIP_FORBIDDEN = 332;
    public static final int CODE_SIP_GLOBAL_ERROR = 362;
    public static final int CODE_SIP_NOT_ACCEPTABLE = 340;
    public static final int CODE_SIP_NOT_FOUND = 333;
    public static final int CODE_SIP_NOT_REACHABLE = 341;
    public static final int CODE_SIP_NOT_SUPPORTED = 334;
    public static final int CODE_SIP_REDIRECTED = 321;
    public static final int CODE_SIP_REQUEST_CANCELLED = 339;
    public static final int CODE_SIP_REQUEST_TIMEOUT = 335;
    public static final int CODE_SIP_SERVER_ERROR = 354;
    public static final int CODE_SIP_SERVER_INTERNAL_ERROR = 351;
    public static final int CODE_SIP_SERVER_TIMEOUT = 353;
    public static final int CODE_SIP_SERVICE_UNAVAILABLE = 352;
    public static final int CODE_SIP_TEMPRARILY_UNAVAILABLE = 336;
    public static final int CODE_SIP_USER_REJECTED = 361;
    public static final int CODE_SMS_NOT_SUPPORTED = 1001;
    public static final int CODE_TIMEOUT_1XX_WAITING = 201;
    public static final int CODE_TIMEOUT_NO_ANSWER = 202;
    public static final int CODE_TIMEOUT_NO_ANSWER_CALL_UPDATE = 203;
    public static final int CODE_UNSPECIFIED = 0;
    public static final int CODE_USER_DECLINE = 504;
    public static final int CODE_USER_IGNORE = 503;
    public static final int CODE_USER_NOANSWER = 502;
    public static final int CODE_USER_TERMINATED = 501;
    public static final int CODE_USER_TERMINATED_BY_REMOTE = 510;
    public static final int CODE_UT_CB_PASSWORD_MISMATCH = 821;
    public static final int CODE_UT_NETWORK_ERROR = 804;
    public static final int CODE_UT_NOT_SUPPORTED = 801;
    public static final int CODE_UT_OPERATION_NOT_ALLOWED = 803;
    public static final int CODE_UT_SERVICE_UNAVAILABLE = 802;
    public static final Parcelable.Creator<ImsReasonInfo> CREATOR = new Parcelable.Creator<ImsReasonInfo>() {
        @Override
        public ImsReasonInfo createFromParcel(Parcel in) {
            return new ImsReasonInfo(in);
        }

        @Override
        public ImsReasonInfo[] newArray(int size) {
            return new ImsReasonInfo[size];
        }
    };
    public static final int EXTRA_CODE_CALL_RETRY_BY_SETTINGS = 3;
    public static final int EXTRA_CODE_CALL_RETRY_NORMAL = 1;
    public static final int EXTRA_CODE_CALL_RETRY_SILENT_REDIAL = 2;
    public static final String EXTRA_MSG_SERVICE_NOT_AUTHORIZED = "Forbidden. Not Authorized for Service";
    public static final int TYPE_LOCAL = 1;
    public static final int TYPE_MEDIA = 4;
    public static final int TYPE_STATUSCODE = 3;
    public static final int TYPE_TIMEOUT = 2;
    public static final int TYPE_UNSPECIFIED = 0;
    public static final int TYPE_USER = 5;
    public static final int TYPE_UT = 8;
    public int mCode;
    public int mExtraCode;
    public String mExtraMessage;
    public int mReasonType;

    public ImsReasonInfo() {
        this.mReasonType = 0;
        this.mCode = 0;
        this.mExtraCode = 0;
        this.mExtraMessage = null;
    }

    public ImsReasonInfo(Parcel in) {
        readFromParcel(in);
    }

    public ImsReasonInfo(int code, int extraCode) {
        this.mReasonType = code / 100;
        this.mCode = code;
        this.mExtraCode = extraCode;
        this.mExtraMessage = null;
    }

    public ImsReasonInfo(int code, int extraCode, String extraMessage) {
        this.mReasonType = code / 100;
        this.mCode = code;
        this.mExtraCode = extraCode;
        this.mExtraMessage = extraMessage;
    }

    public int getCode() {
        return this.mCode;
    }

    public int getExtraCode() {
        return this.mExtraCode;
    }

    public String getExtraMessage() {
        return this.mExtraMessage;
    }

    public int getReasonType() {
        return this.mReasonType;
    }

    public String toString() {
        return "ImsReasonInfo :: {" + this.mReasonType + ", " + this.mCode + ", " + this.mExtraCode + ", " + this.mExtraMessage + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.mReasonType);
        out.writeInt(this.mCode);
        out.writeInt(this.mExtraCode);
        out.writeString(this.mExtraMessage);
    }

    private void readFromParcel(Parcel in) {
        this.mReasonType = in.readInt();
        this.mCode = in.readInt();
        this.mExtraCode = in.readInt();
        this.mExtraMessage = in.readString();
    }
}
