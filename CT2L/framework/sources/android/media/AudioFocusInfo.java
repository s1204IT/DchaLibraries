package android.media;

import android.media.AudioAttributes;
import android.net.ProxyInfo;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public final class AudioFocusInfo implements Parcelable {
    public static final Parcelable.Creator<AudioFocusInfo> CREATOR = new Parcelable.Creator<AudioFocusInfo>() {
        @Override
        public AudioFocusInfo createFromParcel(Parcel in) {
            return new AudioFocusInfo(AudioAttributes.CREATOR.createFromParcel(in), in.readString(), in.readString(), in.readInt(), in.readInt(), in.readInt());
        }

        @Override
        public AudioFocusInfo[] newArray(int size) {
            return new AudioFocusInfo[size];
        }
    };
    private AudioAttributes mAttributes;
    private String mClientId;
    private int mFlags;
    private int mGainRequest;
    private int mLossReceived;
    private String mPackageName;

    AudioFocusInfo(AudioAttributes aa, String clientId, String packageName, int gainRequest, int lossReceived, int flags) {
        this.mAttributes = aa == null ? new AudioAttributes.Builder().build() : aa;
        this.mClientId = clientId == null ? ProxyInfo.LOCAL_EXCL_LIST : clientId;
        this.mPackageName = packageName == null ? ProxyInfo.LOCAL_EXCL_LIST : packageName;
        this.mGainRequest = gainRequest;
        this.mLossReceived = lossReceived;
        this.mFlags = flags;
    }

    public AudioAttributes getAttributes() {
        return this.mAttributes;
    }

    public String getClientId() {
        return this.mClientId;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public int getGainRequest() {
        return this.mGainRequest;
    }

    public int getLossReceived() {
        return this.mLossReceived;
    }

    void clearLossReceived() {
        this.mLossReceived = 0;
    }

    public int getFlags() {
        return this.mFlags;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        this.mAttributes.writeToParcel(dest, flags);
        dest.writeString(this.mClientId);
        dest.writeString(this.mPackageName);
        dest.writeInt(this.mGainRequest);
        dest.writeInt(this.mLossReceived);
        dest.writeInt(this.mFlags);
    }

    public int hashCode() {
        return Objects.hash(this.mAttributes, this.mClientId, this.mPackageName, Integer.valueOf(this.mGainRequest), Integer.valueOf(this.mFlags));
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            AudioFocusInfo other = (AudioFocusInfo) obj;
            return this.mAttributes.equals(other.mAttributes) && this.mClientId.equals(other.mClientId) && this.mPackageName.equals(other.mPackageName) && this.mGainRequest == other.mGainRequest && this.mLossReceived == other.mLossReceived && this.mFlags == other.mFlags;
        }
        return false;
    }
}
