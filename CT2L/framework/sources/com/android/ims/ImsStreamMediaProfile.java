package com.android.ims;

import android.os.Parcel;
import android.os.Parcelable;

public class ImsStreamMediaProfile implements Parcelable {
    public static final int AUDIO_QUALITY_AMR = 1;
    public static final int AUDIO_QUALITY_AMR_WB = 2;
    public static final int AUDIO_QUALITY_EVRC = 4;
    public static final int AUDIO_QUALITY_EVRC_B = 5;
    public static final int AUDIO_QUALITY_EVRC_NW = 7;
    public static final int AUDIO_QUALITY_EVRC_WB = 6;
    public static final int AUDIO_QUALITY_GSM_EFR = 8;
    public static final int AUDIO_QUALITY_GSM_FR = 9;
    public static final int AUDIO_QUALITY_GSM_HR = 10;
    public static final int AUDIO_QUALITY_NONE = 0;
    public static final int AUDIO_QUALITY_QCELP13K = 3;
    public static final Parcelable.Creator<ImsStreamMediaProfile> CREATOR = new Parcelable.Creator<ImsStreamMediaProfile>() {
        @Override
        public ImsStreamMediaProfile createFromParcel(Parcel in) {
            return new ImsStreamMediaProfile(in);
        }

        @Override
        public ImsStreamMediaProfile[] newArray(int size) {
            return new ImsStreamMediaProfile[size];
        }
    };
    public static final int DIRECTION_INACTIVE = 0;
    public static final int DIRECTION_INVALID = -1;
    public static final int DIRECTION_RECEIVE = 1;
    public static final int DIRECTION_SEND = 2;
    public static final int DIRECTION_SEND_RECEIVE = 3;
    private static final String TAG = "ImsStreamMediaProfile";
    public static final int VIDEO_QUALITY_NONE = 0;
    public static final int VIDEO_QUALITY_QCIF = 1;
    public static final int VIDEO_QUALITY_QVGA_LANDSCAPE = 2;
    public static final int VIDEO_QUALITY_QVGA_PORTRAIT = 4;
    public static final int VIDEO_QUALITY_VGA_LANDSCAPE = 8;
    public static final int VIDEO_QUALITY_VGA_PORTRAIT = 16;
    public int mAudioDirection;
    public int mAudioQuality;
    public int mVideoDirection;
    public int mVideoQuality;

    public ImsStreamMediaProfile(Parcel in) {
        readFromParcel(in);
    }

    public ImsStreamMediaProfile() {
        this.mAudioQuality = 2;
        this.mAudioDirection = 3;
        this.mVideoQuality = 0;
        this.mVideoDirection = -1;
    }

    public ImsStreamMediaProfile(int audioQuality, int audioDirection, int videoQuality, int videoDirection) {
        this.mAudioQuality = audioQuality;
        this.mAudioDirection = audioDirection;
        this.mVideoQuality = videoQuality;
        this.mVideoDirection = videoDirection;
    }

    public void copyFrom(ImsStreamMediaProfile profile) {
        this.mAudioQuality = profile.mAudioQuality;
        this.mAudioDirection = profile.mAudioDirection;
        this.mVideoQuality = profile.mVideoQuality;
        this.mVideoDirection = profile.mVideoDirection;
    }

    public String toString() {
        return "{ audioQuality=" + this.mAudioQuality + ", audioDirection=" + this.mAudioDirection + ", videoQuality=" + this.mVideoQuality + ", videoDirection=" + this.mVideoDirection + " }";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.mAudioQuality);
        out.writeInt(this.mAudioDirection);
        out.writeInt(this.mVideoQuality);
        out.writeInt(this.mVideoDirection);
    }

    private void readFromParcel(Parcel in) {
        this.mAudioQuality = in.readInt();
        this.mAudioDirection = in.readInt();
        this.mVideoQuality = in.readInt();
        this.mVideoDirection = in.readInt();
    }
}
