package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;
import android.security.keystore.KeyProperties;
import java.util.Objects;

public final class BluetoothCodecConfig implements Parcelable {
    public static final int BITS_PER_SAMPLE_16 = 1;
    public static final int BITS_PER_SAMPLE_24 = 2;
    public static final int BITS_PER_SAMPLE_32 = 4;
    public static final int BITS_PER_SAMPLE_NONE = 0;
    public static final int CHANNEL_MODE_MONO = 1;
    public static final int CHANNEL_MODE_NONE = 0;
    public static final int CHANNEL_MODE_STEREO = 2;
    public static final int CODEC_PRIORITY_DEFAULT = 0;
    public static final int CODEC_PRIORITY_DISABLED = -1;
    public static final int CODEC_PRIORITY_HIGHEST = 1000000;
    public static final Parcelable.Creator<BluetoothCodecConfig> CREATOR = new Parcelable.Creator<BluetoothCodecConfig>() {
        @Override
        public BluetoothCodecConfig createFromParcel(Parcel parcel) {
            return new BluetoothCodecConfig(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readLong(), parcel.readLong(), parcel.readLong(), parcel.readLong());
        }

        @Override
        public BluetoothCodecConfig[] newArray(int i) {
            return new BluetoothCodecConfig[i];
        }
    };
    public static final int SAMPLE_RATE_176400 = 16;
    public static final int SAMPLE_RATE_192000 = 32;
    public static final int SAMPLE_RATE_44100 = 1;
    public static final int SAMPLE_RATE_48000 = 2;
    public static final int SAMPLE_RATE_88200 = 4;
    public static final int SAMPLE_RATE_96000 = 8;
    public static final int SAMPLE_RATE_NONE = 0;
    public static final int SOURCE_CODEC_TYPE_AAC = 1;
    public static final int SOURCE_CODEC_TYPE_APTX = 2;
    public static final int SOURCE_CODEC_TYPE_APTX_HD = 3;
    public static final int SOURCE_CODEC_TYPE_INVALID = 1000000;
    public static final int SOURCE_CODEC_TYPE_LDAC = 4;
    public static final int SOURCE_CODEC_TYPE_MAX = 5;
    public static final int SOURCE_CODEC_TYPE_SBC = 0;
    private final int mBitsPerSample;
    private final int mChannelMode;
    private int mCodecPriority;
    private final long mCodecSpecific1;
    private final long mCodecSpecific2;
    private final long mCodecSpecific3;
    private final long mCodecSpecific4;
    private final int mCodecType;
    private final int mSampleRate;

    public BluetoothCodecConfig(int i, int i2, int i3, int i4, int i5, long j, long j2, long j3, long j4) {
        this.mCodecType = i;
        this.mCodecPriority = i2;
        this.mSampleRate = i3;
        this.mBitsPerSample = i4;
        this.mChannelMode = i5;
        this.mCodecSpecific1 = j;
        this.mCodecSpecific2 = j2;
        this.mCodecSpecific3 = j3;
        this.mCodecSpecific4 = j4;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof BluetoothCodecConfig)) {
            return false;
        }
        BluetoothCodecConfig bluetoothCodecConfig = (BluetoothCodecConfig) obj;
        return bluetoothCodecConfig.mCodecType == this.mCodecType && bluetoothCodecConfig.mCodecPriority == this.mCodecPriority && bluetoothCodecConfig.mSampleRate == this.mSampleRate && bluetoothCodecConfig.mBitsPerSample == this.mBitsPerSample && bluetoothCodecConfig.mChannelMode == this.mChannelMode && bluetoothCodecConfig.mCodecSpecific1 == this.mCodecSpecific1 && bluetoothCodecConfig.mCodecSpecific2 == this.mCodecSpecific2 && bluetoothCodecConfig.mCodecSpecific3 == this.mCodecSpecific3 && bluetoothCodecConfig.mCodecSpecific4 == this.mCodecSpecific4;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mCodecType), Integer.valueOf(this.mCodecPriority), Integer.valueOf(this.mSampleRate), Integer.valueOf(this.mBitsPerSample), Integer.valueOf(this.mChannelMode), Long.valueOf(this.mCodecSpecific1), Long.valueOf(this.mCodecSpecific2), Long.valueOf(this.mCodecSpecific3), Long.valueOf(this.mCodecSpecific4));
    }

    public boolean isValid() {
        return (this.mSampleRate == 0 || this.mBitsPerSample == 0 || this.mChannelMode == 0) ? false : true;
    }

    private static String appendCapabilityToString(String str, String str2) {
        if (str == null) {
            return str2;
        }
        return str + "|" + str2;
    }

    public String toString() {
        String strAppendCapabilityToString;
        String strAppendCapabilityToString2;
        if (this.mSampleRate == 0) {
            strAppendCapabilityToString = appendCapabilityToString(null, KeyProperties.DIGEST_NONE);
        } else {
            strAppendCapabilityToString = null;
        }
        if ((this.mSampleRate & 1) != 0) {
            strAppendCapabilityToString = appendCapabilityToString(strAppendCapabilityToString, "44100");
        }
        if ((this.mSampleRate & 2) != 0) {
            strAppendCapabilityToString = appendCapabilityToString(strAppendCapabilityToString, "48000");
        }
        if ((this.mSampleRate & 4) != 0) {
            strAppendCapabilityToString = appendCapabilityToString(strAppendCapabilityToString, "88200");
        }
        if ((this.mSampleRate & 8) != 0) {
            strAppendCapabilityToString = appendCapabilityToString(strAppendCapabilityToString, "96000");
        }
        if ((this.mSampleRate & 16) != 0) {
            strAppendCapabilityToString = appendCapabilityToString(strAppendCapabilityToString, "176400");
        }
        if ((this.mSampleRate & 32) != 0) {
            strAppendCapabilityToString = appendCapabilityToString(strAppendCapabilityToString, "192000");
        }
        if (this.mBitsPerSample == 0) {
            strAppendCapabilityToString2 = appendCapabilityToString(null, KeyProperties.DIGEST_NONE);
        } else {
            strAppendCapabilityToString2 = null;
        }
        if ((this.mBitsPerSample & 1) != 0) {
            strAppendCapabilityToString2 = appendCapabilityToString(strAppendCapabilityToString2, "16");
        }
        if ((this.mBitsPerSample & 2) != 0) {
            strAppendCapabilityToString2 = appendCapabilityToString(strAppendCapabilityToString2, "24");
        }
        if ((this.mBitsPerSample & 4) != 0) {
            strAppendCapabilityToString2 = appendCapabilityToString(strAppendCapabilityToString2, "32");
        }
        String strAppendCapabilityToString3 = this.mChannelMode == 0 ? appendCapabilityToString(null, KeyProperties.DIGEST_NONE) : null;
        if ((this.mChannelMode & 1) != 0) {
            strAppendCapabilityToString3 = appendCapabilityToString(strAppendCapabilityToString3, "MONO");
        }
        if ((this.mChannelMode & 2) != 0) {
            strAppendCapabilityToString3 = appendCapabilityToString(strAppendCapabilityToString3, "STEREO");
        }
        return "{codecName:" + getCodecName() + ",mCodecType:" + this.mCodecType + ",mCodecPriority:" + this.mCodecPriority + ",mSampleRate:" + String.format("0x%x", Integer.valueOf(this.mSampleRate)) + "(" + strAppendCapabilityToString + "),mBitsPerSample:" + String.format("0x%x", Integer.valueOf(this.mBitsPerSample)) + "(" + strAppendCapabilityToString2 + "),mChannelMode:" + String.format("0x%x", Integer.valueOf(this.mChannelMode)) + "(" + strAppendCapabilityToString3 + "),mCodecSpecific1:" + this.mCodecSpecific1 + ",mCodecSpecific2:" + this.mCodecSpecific2 + ",mCodecSpecific3:" + this.mCodecSpecific3 + ",mCodecSpecific4:" + this.mCodecSpecific4 + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mCodecType);
        parcel.writeInt(this.mCodecPriority);
        parcel.writeInt(this.mSampleRate);
        parcel.writeInt(this.mBitsPerSample);
        parcel.writeInt(this.mChannelMode);
        parcel.writeLong(this.mCodecSpecific1);
        parcel.writeLong(this.mCodecSpecific2);
        parcel.writeLong(this.mCodecSpecific3);
        parcel.writeLong(this.mCodecSpecific4);
    }

    public String getCodecName() {
        int i = this.mCodecType;
        if (i != 1000000) {
            switch (i) {
                case 0:
                    return "SBC";
                case 1:
                    return "AAC";
                case 2:
                    return "aptX";
                case 3:
                    return "aptX HD";
                case 4:
                    return "LDAC";
                default:
                    return "UNKNOWN CODEC(" + this.mCodecType + ")";
            }
        }
        return "INVALID CODEC";
    }

    public int getCodecType() {
        return this.mCodecType;
    }

    public boolean isMandatoryCodec() {
        return this.mCodecType == 0;
    }

    public int getCodecPriority() {
        return this.mCodecPriority;
    }

    public void setCodecPriority(int i) {
        this.mCodecPriority = i;
    }

    public int getSampleRate() {
        return this.mSampleRate;
    }

    public int getBitsPerSample() {
        return this.mBitsPerSample;
    }

    public int getChannelMode() {
        return this.mChannelMode;
    }

    public long getCodecSpecific1() {
        return this.mCodecSpecific1;
    }

    public long getCodecSpecific2() {
        return this.mCodecSpecific2;
    }

    public long getCodecSpecific3() {
        return this.mCodecSpecific3;
    }

    public long getCodecSpecific4() {
        return this.mCodecSpecific4;
    }

    public boolean sameAudioFeedingParameters(BluetoothCodecConfig bluetoothCodecConfig) {
        return bluetoothCodecConfig != null && bluetoothCodecConfig.mSampleRate == this.mSampleRate && bluetoothCodecConfig.mBitsPerSample == this.mBitsPerSample && bluetoothCodecConfig.mChannelMode == this.mChannelMode;
    }
}
