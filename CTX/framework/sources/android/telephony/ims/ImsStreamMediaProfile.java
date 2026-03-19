package android.telephony.ims;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

@SystemApi
public final class ImsStreamMediaProfile implements Parcelable {
    public static final int AUDIO_QUALITY_AMR = 1;
    public static final int AUDIO_QUALITY_AMR_WB = 2;
    public static final int AUDIO_QUALITY_EVRC = 4;
    public static final int AUDIO_QUALITY_EVRC_B = 5;
    public static final int AUDIO_QUALITY_EVRC_NW = 7;
    public static final int AUDIO_QUALITY_EVRC_WB = 6;
    public static final int AUDIO_QUALITY_EVS_FB = 20;
    public static final int AUDIO_QUALITY_EVS_NB = 17;
    public static final int AUDIO_QUALITY_EVS_SWB = 19;
    public static final int AUDIO_QUALITY_EVS_WB = 18;
    public static final int AUDIO_QUALITY_G711A = 13;
    public static final int AUDIO_QUALITY_G711AB = 15;
    public static final int AUDIO_QUALITY_G711U = 11;
    public static final int AUDIO_QUALITY_G722 = 14;
    public static final int AUDIO_QUALITY_G723 = 12;
    public static final int AUDIO_QUALITY_G729 = 16;
    public static final int AUDIO_QUALITY_GSM_EFR = 8;
    public static final int AUDIO_QUALITY_GSM_FR = 9;
    public static final int AUDIO_QUALITY_GSM_HR = 10;
    public static final int AUDIO_QUALITY_NONE = 0;
    public static final int AUDIO_QUALITY_QCELP13K = 3;
    public static final Parcelable.Creator<ImsStreamMediaProfile> CREATOR = new Parcelable.Creator<ImsStreamMediaProfile>() {
        @Override
        public ImsStreamMediaProfile createFromParcel(Parcel parcel) {
            return new ImsStreamMediaProfile(parcel);
        }

        @Override
        public ImsStreamMediaProfile[] newArray(int i) {
            return new ImsStreamMediaProfile[i];
        }
    };
    public static final int DIRECTION_INACTIVE = 0;
    public static final int DIRECTION_INVALID = -1;
    public static final int DIRECTION_RECEIVE = 1;
    public static final int DIRECTION_SEND = 2;
    public static final int DIRECTION_SEND_RECEIVE = 3;
    public static final int RTT_MODE_DISABLED = 0;
    public static final int RTT_MODE_FULL = 1;
    private static final String TAG = "ImsStreamMediaProfile";
    public static final int VIDEO_QUALITY_NONE = 0;
    public static final int VIDEO_QUALITY_QCIF = 1;
    public static final int VIDEO_QUALITY_QVGA_LANDSCAPE = 2;
    public static final int VIDEO_QUALITY_QVGA_PORTRAIT = 4;
    public static final int VIDEO_QUALITY_VGA_LANDSCAPE = 8;
    public static final int VIDEO_QUALITY_VGA_PORTRAIT = 16;
    public int mAudioDirection;
    public int mAudioQuality;
    public int mRttMode;
    public int mVideoDirection;
    public int mVideoQuality;

    public ImsStreamMediaProfile(Parcel parcel) {
        readFromParcel(parcel);
    }

    public ImsStreamMediaProfile(int i, int i2, int i3, int i4, int i5) {
        this.mAudioQuality = i;
        this.mAudioDirection = i2;
        this.mVideoQuality = i3;
        this.mVideoDirection = i4;
        this.mRttMode = i5;
    }

    public ImsStreamMediaProfile() {
        this.mAudioQuality = 0;
        this.mAudioDirection = 3;
        this.mVideoQuality = 0;
        this.mVideoDirection = -1;
        this.mRttMode = 0;
    }

    public ImsStreamMediaProfile(int i, int i2, int i3, int i4) {
        this.mAudioQuality = i;
        this.mAudioDirection = i2;
        this.mVideoQuality = i3;
        this.mVideoDirection = i4;
    }

    public ImsStreamMediaProfile(int i) {
        this.mRttMode = i;
    }

    public void copyFrom(ImsStreamMediaProfile imsStreamMediaProfile) {
        this.mAudioQuality = imsStreamMediaProfile.mAudioQuality;
        this.mAudioDirection = imsStreamMediaProfile.mAudioDirection;
        this.mVideoQuality = imsStreamMediaProfile.mVideoQuality;
        this.mVideoDirection = imsStreamMediaProfile.mVideoDirection;
        this.mRttMode = imsStreamMediaProfile.mRttMode;
    }

    public String toString() {
        return "{ audioQuality=" + this.mAudioQuality + ", audioDirection=" + this.mAudioDirection + ", videoQuality=" + this.mVideoQuality + ", videoDirection=" + this.mVideoDirection + ", rttMode=" + this.mRttMode + " }";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mAudioQuality);
        parcel.writeInt(this.mAudioDirection);
        parcel.writeInt(this.mVideoQuality);
        parcel.writeInt(this.mVideoDirection);
        parcel.writeInt(this.mRttMode);
    }

    private void readFromParcel(Parcel parcel) {
        this.mAudioQuality = parcel.readInt();
        this.mAudioDirection = parcel.readInt();
        this.mVideoQuality = parcel.readInt();
        this.mVideoDirection = parcel.readInt();
        this.mRttMode = parcel.readInt();
    }

    public boolean isRttCall() {
        return this.mRttMode == 1;
    }

    public void setRttMode(int i) {
        this.mRttMode = i;
    }

    public int getAudioQuality() {
        return this.mAudioQuality;
    }

    public int getAudioDirection() {
        return this.mAudioDirection;
    }

    public int getVideoQuality() {
        return this.mVideoQuality;
    }

    public int getVideoDirection() {
        return this.mVideoDirection;
    }

    public int getRttMode() {
        return this.mRttMode;
    }
}
