package android.telephony.ims;

import android.annotation.SystemApi;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.telecom.VideoProfile;
import android.util.Log;

@SystemApi
public final class ImsCallProfile implements Parcelable {
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
        public ImsCallProfile createFromParcel(Parcel parcel) {
            return new ImsCallProfile(parcel);
        }

        @Override
        public ImsCallProfile[] newArray(int i) {
            return new ImsCallProfile[i];
        }
    };
    public static final int DIALSTRING_NORMAL = 0;
    public static final int DIALSTRING_SS_CONF = 1;
    public static final int DIALSTRING_USSD = 2;
    public static final String EXTRA_ADDITIONAL_CALL_INFO = "AdditionalCallInfo";
    public static final String EXTRA_CALL_MODE_CHANGEABLE = "call_mode_changeable";
    public static final String EXTRA_CALL_RAT_TYPE = "CallRadioTech";
    public static final String EXTRA_CALL_RAT_TYPE_ALT = "callRadioTech";
    public static final String EXTRA_CHILD_NUMBER = "ChildNum";
    public static final String EXTRA_CNA = "cna";
    public static final String EXTRA_CNAP = "cnap";
    public static final String EXTRA_CODEC = "Codec";
    public static final String EXTRA_CONFERENCE = "conference";
    public static final String EXTRA_CONFERENCE_AVAIL = "conference_avail";
    public static final String EXTRA_DIALSTRING = "dialstring";
    public static final String EXTRA_DISPLAY_TEXT = "DisplayText";
    public static final String EXTRA_E_CALL = "e_call";
    public static final String EXTRA_IS_CALL_PULL = "CallPull";
    public static final String EXTRA_OEM_EXTRAS = "OemCallExtras";
    public static final String EXTRA_OI = "oi";
    public static final String EXTRA_OIR = "oir";
    public static final String EXTRA_REMOTE_URI = "remote_uri";
    public static final String EXTRA_USSD = "ussd";
    public static final String EXTRA_VMS = "vms";
    public static final int OIR_DEFAULT = 0;
    public static final int OIR_PRESENTATION_NOT_RESTRICTED = 2;
    public static final int OIR_PRESENTATION_PAYPHONE = 4;
    public static final int OIR_PRESENTATION_RESTRICTED = 1;
    public static final int OIR_PRESENTATION_UNKNOWN = 3;
    public static final int SERVICE_TYPE_EMERGENCY = 2;
    public static final int SERVICE_TYPE_NONE = 0;
    public static final int SERVICE_TYPE_NORMAL = 1;
    private static final String TAG = "ImsCallProfile";
    public Bundle mCallExtras;
    public int mCallType;
    public ImsStreamMediaProfile mMediaProfile;
    public int mRestrictCause;
    public int mServiceType;

    public ImsCallProfile(Parcel parcel) {
        this.mRestrictCause = 0;
        readFromParcel(parcel);
    }

    public ImsCallProfile() {
        this.mRestrictCause = 0;
        this.mServiceType = 1;
        this.mCallType = 1;
        this.mCallExtras = new Bundle();
        this.mMediaProfile = new ImsStreamMediaProfile();
    }

    public ImsCallProfile(int i, int i2) {
        this.mRestrictCause = 0;
        this.mServiceType = i;
        this.mCallType = i2;
        this.mCallExtras = new Bundle();
        this.mMediaProfile = new ImsStreamMediaProfile();
    }

    public ImsCallProfile(int i, int i2, Bundle bundle, ImsStreamMediaProfile imsStreamMediaProfile) {
        this.mRestrictCause = 0;
        this.mServiceType = i;
        this.mCallType = i2;
        this.mCallExtras = bundle;
        this.mMediaProfile = imsStreamMediaProfile;
    }

    public String getCallExtra(String str) {
        return getCallExtra(str, "");
    }

    public String getCallExtra(String str, String str2) {
        if (this.mCallExtras == null) {
            return str2;
        }
        return this.mCallExtras.getString(str, str2);
    }

    public boolean getCallExtraBoolean(String str) {
        return getCallExtraBoolean(str, false);
    }

    public boolean getCallExtraBoolean(String str, boolean z) {
        if (this.mCallExtras == null) {
            return z;
        }
        return this.mCallExtras.getBoolean(str, z);
    }

    public int getCallExtraInt(String str) {
        return getCallExtraInt(str, -1);
    }

    public int getCallExtraInt(String str, int i) {
        if (this.mCallExtras == null) {
            return i;
        }
        return this.mCallExtras.getInt(str, i);
    }

    public void setCallExtra(String str, String str2) {
        if (this.mCallExtras != null) {
            this.mCallExtras.putString(str, str2);
        }
    }

    public void setCallExtraBoolean(String str, boolean z) {
        if (this.mCallExtras != null) {
            this.mCallExtras.putBoolean(str, z);
        }
    }

    public void setCallExtraInt(String str, int i) {
        if (this.mCallExtras != null) {
            this.mCallExtras.putInt(str, i);
        }
    }

    public void updateCallType(ImsCallProfile imsCallProfile) {
        this.mCallType = imsCallProfile.mCallType;
    }

    public void updateCallExtras(ImsCallProfile imsCallProfile) {
        this.mCallExtras.clear();
        this.mCallExtras = (Bundle) imsCallProfile.mCallExtras.clone();
    }

    public void updateMediaProfile(ImsCallProfile imsCallProfile) {
        this.mMediaProfile = imsCallProfile.mMediaProfile;
    }

    public String toString() {
        return "{ serviceType=" + this.mServiceType + ", callType=" + this.mCallType + ", restrictCause=" + this.mRestrictCause + ", mediaProfile=" + this.mMediaProfile.toString() + " }";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        Bundle bundleMaybeCleanseExtras = maybeCleanseExtras(this.mCallExtras);
        parcel.writeInt(this.mServiceType);
        parcel.writeInt(this.mCallType);
        parcel.writeInt(this.mRestrictCause);
        parcel.writeBundle(bundleMaybeCleanseExtras);
        parcel.writeParcelable(this.mMediaProfile, 0);
    }

    private void readFromParcel(Parcel parcel) {
        this.mServiceType = parcel.readInt();
        this.mCallType = parcel.readInt();
        this.mRestrictCause = parcel.readInt();
        this.mCallExtras = parcel.readBundle();
        this.mMediaProfile = (ImsStreamMediaProfile) parcel.readParcelable(ImsStreamMediaProfile.class.getClassLoader());
    }

    public int getServiceType() {
        return this.mServiceType;
    }

    public int getCallType() {
        return this.mCallType;
    }

    public int getRestrictCause() {
        return this.mRestrictCause;
    }

    public Bundle getCallExtras() {
        return this.mCallExtras;
    }

    public ImsStreamMediaProfile getMediaProfile() {
        return this.mMediaProfile;
    }

    public static int getVideoStateFromImsCallProfile(ImsCallProfile imsCallProfile) {
        int videoStateFromCallType = getVideoStateFromCallType(imsCallProfile.mCallType);
        if (imsCallProfile.isVideoPaused() && !VideoProfile.isAudioOnly(videoStateFromCallType)) {
            return videoStateFromCallType | 4;
        }
        return videoStateFromCallType & (-5);
    }

    public static int getVideoStateFromCallType(int i) {
        if (i == 2) {
            return 0;
        }
        switch (i) {
            case 4:
                return 3;
            case 5:
                return 1;
            case 6:
                return 2;
            case 7:
                return 7;
            default:
                return 0;
        }
    }

    public static int getCallTypeFromVideoState(int i) {
        boolean zIsVideoStateSet = isVideoStateSet(i, 1);
        boolean zIsVideoStateSet2 = isVideoStateSet(i, 2);
        if (isVideoStateSet(i, 4)) {
            return 7;
        }
        if (zIsVideoStateSet && !zIsVideoStateSet2) {
            return 5;
        }
        if (!zIsVideoStateSet && zIsVideoStateSet2) {
            return 6;
        }
        if (!zIsVideoStateSet || !zIsVideoStateSet2) {
            return 2;
        }
        return 4;
    }

    public static int presentationToOIR(int i) {
        switch (i) {
            case 1:
                return 2;
            case 2:
                return 1;
            case 3:
                return 3;
            case 4:
                return 4;
            default:
                return 0;
        }
    }

    public static int presentationToOir(int i) {
        return presentationToOIR(i);
    }

    public static int OIRToPresentation(int i) {
        switch (i) {
        }
        return 3;
    }

    public boolean isVideoPaused() {
        return this.mMediaProfile.mVideoDirection == 0;
    }

    public boolean isVideoCall() {
        return VideoProfile.isVideo(getVideoStateFromCallType(this.mCallType));
    }

    private Bundle maybeCleanseExtras(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        int size = bundle.size();
        Bundle bundleFilterValues = bundle.filterValues();
        int size2 = bundleFilterValues.size();
        if (size != size2) {
            Log.i(TAG, "maybeCleanseExtras: " + (size - size2) + " extra values were removed - only primitive types and system parcelables are permitted.");
        }
        return bundleFilterValues;
    }

    private static boolean isVideoStateSet(int i, int i2) {
        return (i & i2) == i2;
    }
}
