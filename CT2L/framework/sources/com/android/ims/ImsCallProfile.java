package com.android.ims;

import android.net.ProxyInfo;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class ImsCallProfile implements Parcelable {
    public static final int CALL_RESTRICT_CAUSE_DISABLED = 2;
    public static final int CALL_RESTRICT_CAUSE_HD = 3;
    public static final int CALL_RESTRICT_CAUSE_NONE = 0;
    public static final int CALL_RESTRICT_CAUSE_RAT = 1;
    public static final int CALL_TYPE_VIDEO_N_VOICE = 3;
    public static final int CALL_TYPE_VOICE = 2;
    public static final int CALL_TYPE_VOICE_N_VIDEO = 1;
    public static final int CALL_TYPE_VS = 8;
    public static final int CALL_TYPE_VS_RX = 10;
    public static final int CALL_TYPE_VS_TX = 9;
    public static final int CALL_TYPE_VT = 4;
    public static final int CALL_TYPE_VT_NODIR = 7;
    public static final int CALL_TYPE_VT_RX = 6;
    public static final int CALL_TYPE_VT_TX = 5;
    public static final Parcelable.Creator<ImsCallProfile> CREATOR = new Parcelable.Creator<ImsCallProfile>() {
        @Override
        public ImsCallProfile createFromParcel(Parcel in) {
            return new ImsCallProfile(in);
        }

        @Override
        public ImsCallProfile[] newArray(int size) {
            return new ImsCallProfile[size];
        }
    };
    public static final int DIALSTRING_NORMAL = 0;
    public static final int DIALSTRING_SS_CONF = 1;
    public static final int DIALSTRING_USSD = 2;
    public static final String EXTRA_CALL_MODE_CHANGEABLE = "call_mode_changeable";
    public static final String EXTRA_CNA = "cna";
    public static final String EXTRA_CNAP = "cnap";
    public static final String EXTRA_CONFERENCE = "conference";
    public static final String EXTRA_CONFERENCE_AVAIL = "conference_avail";
    public static final String EXTRA_DIALSTRING = "dialstring";
    public static final String EXTRA_E_CALL = "e_call";
    public static final String EXTRA_OEM_EXTRAS = "OemCallExtras";
    public static final String EXTRA_OI = "oi";
    public static final String EXTRA_OIR = "oir";
    public static final String EXTRA_REMOTE_URI = "remote_uri";
    public static final String EXTRA_USSD = "ussd";
    public static final String EXTRA_VMS = "vms";
    public static final int OIR_DEFAULT = 0;
    public static final int OIR_PRESENTATION_NOT_RESTRICTED = 2;
    public static final int OIR_PRESENTATION_RESTRICTED = 1;
    public static final int SERVICE_TYPE_EMERGENCY = 2;
    public static final int SERVICE_TYPE_NONE = 0;
    public static final int SERVICE_TYPE_NORMAL = 1;
    private static final String TAG = "ImsCallProfile";
    public Bundle mCallExtras;
    public int mCallType;
    public ImsStreamMediaProfile mMediaProfile;
    public int mRestrictCause;
    public int mServiceType;

    public ImsCallProfile(Parcel in) {
        this.mRestrictCause = 0;
        readFromParcel(in);
    }

    public ImsCallProfile() {
        this.mRestrictCause = 0;
        this.mServiceType = 1;
        this.mCallType = 1;
        this.mCallExtras = new Bundle();
        this.mMediaProfile = new ImsStreamMediaProfile();
    }

    public ImsCallProfile(int serviceType, int callType) {
        this.mRestrictCause = 0;
        this.mServiceType = serviceType;
        this.mCallType = callType;
        this.mCallExtras = new Bundle();
        if (callType == 4) {
            this.mMediaProfile = new ImsStreamMediaProfile(2, 3, 1, 3);
        } else {
            this.mMediaProfile = new ImsStreamMediaProfile();
        }
    }

    public String getCallExtra(String name) {
        return getCallExtra(name, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public String getCallExtra(String name, String defaultValue) {
        return this.mCallExtras == null ? defaultValue : this.mCallExtras.getString(name, defaultValue);
    }

    public boolean getCallExtraBoolean(String name) {
        return getCallExtraBoolean(name, false);
    }

    public boolean getCallExtraBoolean(String name, boolean defaultValue) {
        return this.mCallExtras == null ? defaultValue : this.mCallExtras.getBoolean(name, defaultValue);
    }

    public int getCallExtraInt(String name) {
        return getCallExtraInt(name, -1);
    }

    public int getCallExtraInt(String name, int defaultValue) {
        return this.mCallExtras == null ? defaultValue : this.mCallExtras.getInt(name, defaultValue);
    }

    public void setCallExtra(String name, String value) {
        if (this.mCallExtras != null) {
            this.mCallExtras.putString(name, value);
        }
    }

    public void setCallExtraBoolean(String name, boolean value) {
        if (this.mCallExtras != null) {
            this.mCallExtras.putBoolean(name, value);
        }
    }

    public void setCallExtraInt(String name, int value) {
        if (this.mCallExtras != null) {
            this.mCallExtras.putInt(name, value);
        }
    }

    public void updateCallType(ImsCallProfile profile) {
        this.mCallType = profile.mCallType;
    }

    public void updateCallExtras(ImsCallProfile profile) {
        this.mCallExtras.clear();
        this.mCallExtras = (Bundle) profile.mCallExtras.clone();
    }

    public String toString() {
        return "{ serviceType=" + this.mServiceType + ", callType=" + this.mCallType + ", restrictCause=" + this.mRestrictCause + ", callExtras=" + this.mCallExtras.toString() + ", mediaProfile=" + this.mMediaProfile.toString() + " }";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.mServiceType);
        out.writeInt(this.mCallType);
        out.writeParcelable(this.mCallExtras, 0);
        out.writeParcelable(this.mMediaProfile, 0);
    }

    private void readFromParcel(Parcel in) {
        this.mServiceType = in.readInt();
        this.mCallType = in.readInt();
        this.mCallExtras = (Bundle) in.readParcelable(null);
        this.mMediaProfile = (ImsStreamMediaProfile) in.readParcelable(null);
    }

    public static int getVideoStateFromCallType(int callType) {
        switch (callType) {
            case 2:
            case 3:
            default:
                return 0;
            case 4:
                return 3;
            case 5:
                return 1;
            case 6:
                return 2;
            case 7:
                return 7;
        }
    }

    public static int getCallTypeFromVideoState(int videoState) {
        boolean videoTx = isVideoStateSet(videoState, 1);
        boolean videoRx = isVideoStateSet(videoState, 2);
        boolean isPaused = isVideoStateSet(videoState, 4);
        if (isPaused) {
            return 7;
        }
        if (videoTx && !videoRx) {
            return 5;
        }
        if (videoTx || !videoRx) {
            return (videoTx && videoRx) ? 4 : 2;
        }
        return 6;
    }

    public static int presentationToOIR(int presentation) {
        switch (presentation) {
            case 1:
                return 2;
            case 2:
                return 1;
            default:
                return 0;
        }
    }

    public static int OIRToPresentation(int oir) {
        switch (oir) {
            case 1:
                return 2;
            case 2:
                return 1;
            default:
                return 3;
        }
    }

    private static boolean isVideoStateSet(int videoState, int videoStateToCheck) {
        return (videoState & videoStateToCheck) == videoStateToCheck;
    }
}
