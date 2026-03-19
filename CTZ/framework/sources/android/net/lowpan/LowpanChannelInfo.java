package android.net.lowpan;

import android.net.wifi.WifiInfo;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public class LowpanChannelInfo implements Parcelable {
    public static final Parcelable.Creator<LowpanChannelInfo> CREATOR = new Parcelable.Creator<LowpanChannelInfo>() {
        @Override
        public LowpanChannelInfo createFromParcel(Parcel parcel) {
            LowpanChannelInfo lowpanChannelInfo = new LowpanChannelInfo();
            lowpanChannelInfo.mIndex = parcel.readInt();
            lowpanChannelInfo.mName = parcel.readString();
            lowpanChannelInfo.mSpectrumCenterFrequency = parcel.readFloat();
            lowpanChannelInfo.mSpectrumBandwidth = parcel.readFloat();
            lowpanChannelInfo.mMaxTransmitPower = parcel.readInt();
            lowpanChannelInfo.mIsMaskedByRegulatoryDomain = parcel.readBoolean();
            return lowpanChannelInfo;
        }

        @Override
        public LowpanChannelInfo[] newArray(int i) {
            return new LowpanChannelInfo[i];
        }
    };
    public static final float UNKNOWN_BANDWIDTH = 0.0f;
    public static final float UNKNOWN_FREQUENCY = 0.0f;
    public static final int UNKNOWN_POWER = Integer.MAX_VALUE;
    private int mIndex;
    private boolean mIsMaskedByRegulatoryDomain;
    private int mMaxTransmitPower;
    private String mName;
    private float mSpectrumBandwidth;
    private float mSpectrumCenterFrequency;

    public static LowpanChannelInfo getChannelInfoForIeee802154Page0(int i) {
        LowpanChannelInfo lowpanChannelInfo = new LowpanChannelInfo();
        LowpanChannelInfo lowpanChannelInfo2 = null;
        if (i >= 0) {
            if (i == 0) {
                lowpanChannelInfo.mSpectrumCenterFrequency = 8.683E8f;
                lowpanChannelInfo.mSpectrumBandwidth = 600000.0f;
            } else if (i < 11) {
                lowpanChannelInfo.mSpectrumCenterFrequency = 9.04E8f + (2000000.0f * i);
                lowpanChannelInfo.mSpectrumBandwidth = 0.0f;
            } else if (i < 26) {
                lowpanChannelInfo.mSpectrumCenterFrequency = 2.3499999E9f + (5000000.0f * i);
                lowpanChannelInfo.mSpectrumBandwidth = 2000000.0f;
            }
            lowpanChannelInfo2 = lowpanChannelInfo;
        }
        lowpanChannelInfo2.mName = Integer.toString(i);
        return lowpanChannelInfo2;
    }

    private LowpanChannelInfo() {
        this.mIndex = 0;
        this.mName = null;
        this.mSpectrumCenterFrequency = 0.0f;
        this.mSpectrumBandwidth = 0.0f;
        this.mMaxTransmitPower = Integer.MAX_VALUE;
        this.mIsMaskedByRegulatoryDomain = false;
    }

    private LowpanChannelInfo(int i, String str, float f, float f2) {
        this.mIndex = 0;
        this.mName = null;
        this.mSpectrumCenterFrequency = 0.0f;
        this.mSpectrumBandwidth = 0.0f;
        this.mMaxTransmitPower = Integer.MAX_VALUE;
        this.mIsMaskedByRegulatoryDomain = false;
        this.mIndex = i;
        this.mName = str;
        this.mSpectrumCenterFrequency = f;
        this.mSpectrumBandwidth = f2;
    }

    public String getName() {
        return this.mName;
    }

    public int getIndex() {
        return this.mIndex;
    }

    public int getMaxTransmitPower() {
        return this.mMaxTransmitPower;
    }

    public boolean isMaskedByRegulatoryDomain() {
        return this.mIsMaskedByRegulatoryDomain;
    }

    public float getSpectrumCenterFrequency() {
        return this.mSpectrumCenterFrequency;
    }

    public float getSpectrumBandwidth() {
        return this.mSpectrumBandwidth;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("Channel ");
        stringBuffer.append(this.mIndex);
        if (this.mName != null && !this.mName.equals(Integer.toString(this.mIndex))) {
            stringBuffer.append(" (");
            stringBuffer.append(this.mName);
            stringBuffer.append(")");
        }
        if (this.mSpectrumCenterFrequency > 0.0f) {
            if (this.mSpectrumCenterFrequency > 1.0E9f) {
                stringBuffer.append(", SpectrumCenterFrequency: ");
                stringBuffer.append(this.mSpectrumCenterFrequency / 1.0E9f);
                stringBuffer.append("GHz");
            } else if (this.mSpectrumCenterFrequency > 1000000.0f) {
                stringBuffer.append(", SpectrumCenterFrequency: ");
                stringBuffer.append(this.mSpectrumCenterFrequency / 1000000.0f);
                stringBuffer.append(WifiInfo.FREQUENCY_UNITS);
            } else {
                stringBuffer.append(", SpectrumCenterFrequency: ");
                stringBuffer.append(this.mSpectrumCenterFrequency / 1000.0f);
                stringBuffer.append("kHz");
            }
        }
        if (this.mSpectrumBandwidth > 0.0f) {
            if (this.mSpectrumBandwidth > 1.0E9f) {
                stringBuffer.append(", SpectrumBandwidth: ");
                stringBuffer.append(this.mSpectrumBandwidth / 1.0E9f);
                stringBuffer.append("GHz");
            } else if (this.mSpectrumBandwidth > 1000000.0f) {
                stringBuffer.append(", SpectrumBandwidth: ");
                stringBuffer.append(this.mSpectrumBandwidth / 1000000.0f);
                stringBuffer.append(WifiInfo.FREQUENCY_UNITS);
            } else {
                stringBuffer.append(", SpectrumBandwidth: ");
                stringBuffer.append(this.mSpectrumBandwidth / 1000.0f);
                stringBuffer.append("kHz");
            }
        }
        if (this.mMaxTransmitPower != Integer.MAX_VALUE) {
            stringBuffer.append(", MaxTransmitPower: ");
            stringBuffer.append(this.mMaxTransmitPower);
            stringBuffer.append("dBm");
        }
        return stringBuffer.toString();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof LowpanChannelInfo)) {
            return false;
        }
        LowpanChannelInfo lowpanChannelInfo = (LowpanChannelInfo) obj;
        return Objects.equals(this.mName, lowpanChannelInfo.mName) && this.mIndex == lowpanChannelInfo.mIndex && this.mIsMaskedByRegulatoryDomain == lowpanChannelInfo.mIsMaskedByRegulatoryDomain && this.mSpectrumCenterFrequency == lowpanChannelInfo.mSpectrumCenterFrequency && this.mSpectrumBandwidth == lowpanChannelInfo.mSpectrumBandwidth && this.mMaxTransmitPower == lowpanChannelInfo.mMaxTransmitPower;
    }

    public int hashCode() {
        return Objects.hash(this.mName, Integer.valueOf(this.mIndex), Boolean.valueOf(this.mIsMaskedByRegulatoryDomain), Float.valueOf(this.mSpectrumCenterFrequency), Float.valueOf(this.mSpectrumBandwidth), Integer.valueOf(this.mMaxTransmitPower));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mIndex);
        parcel.writeString(this.mName);
        parcel.writeFloat(this.mSpectrumCenterFrequency);
        parcel.writeFloat(this.mSpectrumBandwidth);
        parcel.writeInt(this.mMaxTransmitPower);
        parcel.writeBoolean(this.mIsMaskedByRegulatoryDomain);
    }
}
