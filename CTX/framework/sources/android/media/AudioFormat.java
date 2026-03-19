package android.media;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Objects;

public final class AudioFormat implements Parcelable {
    public static final int AUDIO_FORMAT_HAS_PROPERTY_CHANNEL_INDEX_MASK = 8;
    public static final int AUDIO_FORMAT_HAS_PROPERTY_CHANNEL_MASK = 4;
    public static final int AUDIO_FORMAT_HAS_PROPERTY_ENCODING = 1;
    public static final int AUDIO_FORMAT_HAS_PROPERTY_NONE = 0;
    public static final int AUDIO_FORMAT_HAS_PROPERTY_SAMPLE_RATE = 2;

    @Deprecated
    public static final int CHANNEL_CONFIGURATION_DEFAULT = 1;

    @Deprecated
    public static final int CHANNEL_CONFIGURATION_INVALID = 0;

    @Deprecated
    public static final int CHANNEL_CONFIGURATION_MONO = 2;

    @Deprecated
    public static final int CHANNEL_CONFIGURATION_STEREO = 3;
    public static final int CHANNEL_INVALID = 0;
    public static final int CHANNEL_IN_BACK = 32;
    public static final int CHANNEL_IN_BACK_PROCESSED = 512;
    public static final int CHANNEL_IN_DEFAULT = 1;
    public static final int CHANNEL_IN_FRONT = 16;
    public static final int CHANNEL_IN_FRONT_BACK = 48;
    public static final int CHANNEL_IN_FRONT_PROCESSED = 256;
    public static final int CHANNEL_IN_LEFT = 4;
    public static final int CHANNEL_IN_LEFT_PROCESSED = 64;
    public static final int CHANNEL_IN_MONO = 16;
    public static final int CHANNEL_IN_PRESSURE = 1024;
    public static final int CHANNEL_IN_RIGHT = 8;
    public static final int CHANNEL_IN_RIGHT_PROCESSED = 128;
    public static final int CHANNEL_IN_STEREO = 12;
    public static final int CHANNEL_IN_VOICE_DNLINK = 32768;
    public static final int CHANNEL_IN_VOICE_UPLINK = 16384;
    public static final int CHANNEL_IN_X_AXIS = 2048;
    public static final int CHANNEL_IN_Y_AXIS = 4096;
    public static final int CHANNEL_IN_Z_AXIS = 8192;
    public static final int CHANNEL_OUT_5POINT1 = 252;
    public static final int CHANNEL_OUT_5POINT1_SIDE = 6204;

    @Deprecated
    public static final int CHANNEL_OUT_7POINT1 = 1020;
    public static final int CHANNEL_OUT_7POINT1_SURROUND = 6396;
    public static final int CHANNEL_OUT_BACK_CENTER = 1024;
    public static final int CHANNEL_OUT_BACK_LEFT = 64;
    public static final int CHANNEL_OUT_BACK_RIGHT = 128;
    public static final int CHANNEL_OUT_DEFAULT = 1;
    public static final int CHANNEL_OUT_FRONT_CENTER = 16;
    public static final int CHANNEL_OUT_FRONT_LEFT = 4;
    public static final int CHANNEL_OUT_FRONT_LEFT_OF_CENTER = 256;
    public static final int CHANNEL_OUT_FRONT_RIGHT = 8;
    public static final int CHANNEL_OUT_FRONT_RIGHT_OF_CENTER = 512;
    public static final int CHANNEL_OUT_LOW_FREQUENCY = 32;
    public static final int CHANNEL_OUT_MONO = 4;
    public static final int CHANNEL_OUT_QUAD = 204;
    public static final int CHANNEL_OUT_QUAD_SIDE = 6156;
    public static final int CHANNEL_OUT_SIDE_LEFT = 2048;
    public static final int CHANNEL_OUT_SIDE_RIGHT = 4096;
    public static final int CHANNEL_OUT_STEREO = 12;
    public static final int CHANNEL_OUT_SURROUND = 1052;
    public static final int CHANNEL_OUT_TOP_BACK_CENTER = 262144;
    public static final int CHANNEL_OUT_TOP_BACK_LEFT = 131072;
    public static final int CHANNEL_OUT_TOP_BACK_RIGHT = 524288;
    public static final int CHANNEL_OUT_TOP_CENTER = 8192;
    public static final int CHANNEL_OUT_TOP_FRONT_CENTER = 32768;
    public static final int CHANNEL_OUT_TOP_FRONT_LEFT = 16384;
    public static final int CHANNEL_OUT_TOP_FRONT_RIGHT = 65536;
    public static final int ENCODING_AAC_ELD = 15;
    public static final int ENCODING_AAC_HE_V1 = 11;
    public static final int ENCODING_AAC_HE_V2 = 12;
    public static final int ENCODING_AAC_LC = 10;
    public static final int ENCODING_AAC_XHE = 16;
    public static final int ENCODING_AC3 = 5;
    public static final int ENCODING_AC4 = 17;
    public static final int ENCODING_DEFAULT = 1;
    public static final int ENCODING_DOLBY_TRUEHD = 14;
    public static final int ENCODING_DTS = 7;
    public static final int ENCODING_DTS_HD = 8;
    public static final int ENCODING_E_AC3 = 6;
    public static final int ENCODING_E_AC3_JOC = 18;
    public static final int ENCODING_IEC61937 = 13;
    public static final int ENCODING_INVALID = 0;
    public static final int ENCODING_MP3 = 9;
    public static final int ENCODING_PCM_16BIT = 2;
    public static final int ENCODING_PCM_8BIT = 3;
    public static final int ENCODING_PCM_FLOAT = 4;
    public static final int SAMPLE_RATE_HZ_MAX = 192000;
    public static final int SAMPLE_RATE_HZ_MIN = 4000;
    public static final int SAMPLE_RATE_UNSPECIFIED = 0;
    private int mChannelIndexMask;
    private int mChannelMask;
    private int mEncoding;
    private int mPropertySetMask;
    private int mSampleRate;
    public static final Parcelable.Creator<AudioFormat> CREATOR = new Parcelable.Creator<AudioFormat>() {
        @Override
        public AudioFormat createFromParcel(Parcel parcel) {
            return new AudioFormat(parcel);
        }

        @Override
        public AudioFormat[] newArray(int i) {
            return new AudioFormat[i];
        }
    };
    public static final int[] SURROUND_SOUND_ENCODING = {5, 6, 7, 8, 10, 14, 18};

    @Retention(RetentionPolicy.SOURCE)
    public @interface Encoding {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SurroundSoundEncoding {
    }

    public static String toLogFriendlyEncoding(int i) {
        if (i == 0) {
            return "ENCODING_INVALID";
        }
        switch (i) {
            case 2:
                return "ENCODING_PCM_16BIT";
            case 3:
                return "ENCODING_PCM_8BIT";
            case 4:
                return "ENCODING_PCM_FLOAT";
            case 5:
                return "ENCODING_AC3";
            case 6:
                return "ENCODING_E_AC3";
            case 7:
                return "ENCODING_DTS";
            case 8:
                return "ENCODING_DTS_HD";
            case 9:
                return "ENCODING_MP3";
            case 10:
                return "ENCODING_AAC_LC";
            case 11:
                return "ENCODING_AAC_HE_V1";
            case 12:
                return "ENCODING_AAC_HE_V2";
            case 13:
                return "ENCODING_IEC61937";
            case 14:
                return "ENCODING_DOLBY_TRUEHD";
            case 15:
                return "ENCODING_AAC_ELD";
            case 16:
                return "ENCODING_AAC_XHE";
            case 17:
                return "ENCODING_AC4";
            default:
                return "invalid encoding " + i;
        }
    }

    public static int inChannelMaskFromOutChannelMask(int i) throws IllegalArgumentException {
        if (i == 1) {
            throw new IllegalArgumentException("Illegal CHANNEL_OUT_DEFAULT channel mask for input.");
        }
        switch (channelCountFromOutChannelMask(i)) {
            case 1:
                return 16;
            case 2:
                return 12;
            default:
                throw new IllegalArgumentException("Unsupported channel configuration for input.");
        }
    }

    public static int channelCountFromInChannelMask(int i) {
        return Integer.bitCount(i);
    }

    public static int channelCountFromOutChannelMask(int i) {
        return Integer.bitCount(i);
    }

    public static int convertChannelOutMaskToNativeMask(int i) {
        return i >> 2;
    }

    public static int convertNativeChannelMaskToOutMask(int i) {
        return i << 2;
    }

    public static int getBytesPerSample(int i) {
        if (i != 13) {
            switch (i) {
                case 1:
                case 2:
                    return 2;
                case 3:
                    return 1;
                case 4:
                    return 4;
                default:
                    throw new IllegalArgumentException("Bad audio format " + i);
            }
        }
        return 2;
    }

    public static boolean isValidEncoding(int i) {
        switch (i) {
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 15:
            case 16:
            case 17:
            case 18:
                return true;
            case 14:
            default:
                return false;
        }
    }

    public static boolean isPublicEncoding(int i) {
        switch (i) {
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 15:
            case 16:
            case 17:
            case 18:
                return true;
            case 14:
            default:
                return false;
        }
    }

    public static boolean isEncodingLinearPcm(int i) {
        switch (i) {
            case 1:
            case 2:
            case 3:
            case 4:
                return true;
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 15:
            case 16:
            case 17:
            case 18:
                return false;
            case 14:
            default:
                throw new IllegalArgumentException("Bad audio format " + i);
        }
    }

    public static boolean isEncodingLinearFrames(int i) {
        switch (i) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 13:
                return true;
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 15:
            case 16:
            case 17:
            case 18:
                return false;
            case 14:
            default:
                throw new IllegalArgumentException("Bad audio format " + i);
        }
    }

    public static int[] filterPublicFormats(int[] iArr) {
        if (iArr == null) {
            return null;
        }
        int[] iArrCopyOf = Arrays.copyOf(iArr, iArr.length);
        int i = 0;
        for (int i2 = 0; i2 < iArrCopyOf.length; i2++) {
            if (isPublicEncoding(iArrCopyOf[i2])) {
                if (i != i2) {
                    iArrCopyOf[i] = iArrCopyOf[i2];
                }
                i++;
            }
        }
        return Arrays.copyOf(iArrCopyOf, i);
    }

    public AudioFormat() {
        throw new UnsupportedOperationException("There is no valid usage of this constructor");
    }

    private AudioFormat(int i) {
    }

    private AudioFormat(int i, int i2, int i3, int i4) {
        this.mEncoding = i;
        this.mSampleRate = i2;
        this.mChannelMask = i3;
        this.mChannelIndexMask = i4;
        this.mPropertySetMask = 15;
    }

    public int getEncoding() {
        if ((this.mPropertySetMask & 1) == 0) {
            return 0;
        }
        return this.mEncoding;
    }

    public int getSampleRate() {
        return this.mSampleRate;
    }

    public int getChannelMask() {
        if ((this.mPropertySetMask & 4) == 0) {
            return 0;
        }
        return this.mChannelMask;
    }

    public int getChannelIndexMask() {
        if ((this.mPropertySetMask & 8) == 0) {
            return 0;
        }
        return this.mChannelIndexMask;
    }

    public int getChannelCount() {
        int iBitCount = Integer.bitCount(getChannelIndexMask());
        int iChannelCountFromOutChannelMask = channelCountFromOutChannelMask(getChannelMask());
        if (iChannelCountFromOutChannelMask == 0) {
            return iBitCount;
        }
        if (iChannelCountFromOutChannelMask != iBitCount && iBitCount != 0) {
            return 0;
        }
        return iChannelCountFromOutChannelMask;
    }

    public int getPropertySetMask() {
        return this.mPropertySetMask;
    }

    public String toLogFriendlyString() {
        return String.format("%dch %dHz %s", Integer.valueOf(getChannelCount()), Integer.valueOf(this.mSampleRate), toLogFriendlyEncoding(this.mEncoding));
    }

    public static class Builder {
        private int mChannelIndexMask;
        private int mChannelMask;
        private int mEncoding;
        private int mPropertySetMask;
        private int mSampleRate;

        public Builder() {
            this.mEncoding = 0;
            this.mSampleRate = 0;
            this.mChannelMask = 0;
            this.mChannelIndexMask = 0;
            this.mPropertySetMask = 0;
        }

        public Builder(AudioFormat audioFormat) {
            this.mEncoding = 0;
            this.mSampleRate = 0;
            this.mChannelMask = 0;
            this.mChannelIndexMask = 0;
            this.mPropertySetMask = 0;
            this.mEncoding = audioFormat.mEncoding;
            this.mSampleRate = audioFormat.mSampleRate;
            this.mChannelMask = audioFormat.mChannelMask;
            this.mChannelIndexMask = audioFormat.mChannelIndexMask;
            this.mPropertySetMask = audioFormat.mPropertySetMask;
        }

        public AudioFormat build() {
            AudioFormat audioFormat = new AudioFormat(1980);
            audioFormat.mEncoding = this.mEncoding;
            audioFormat.mSampleRate = this.mSampleRate;
            audioFormat.mChannelMask = this.mChannelMask;
            audioFormat.mChannelIndexMask = this.mChannelIndexMask;
            audioFormat.mPropertySetMask = this.mPropertySetMask;
            return audioFormat;
        }

        public Builder setEncoding(int i) throws IllegalArgumentException {
            switch (i) {
                case 1:
                    this.mEncoding = 2;
                    break;
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 15:
                case 16:
                case 17:
                case 18:
                    this.mEncoding = i;
                    break;
                case 14:
                default:
                    throw new IllegalArgumentException("Invalid encoding " + i);
            }
            this.mPropertySetMask |= 1;
            return this;
        }

        public Builder setChannelMask(int i) {
            if (i == 0) {
                throw new IllegalArgumentException("Invalid zero channel mask");
            }
            if (this.mChannelIndexMask != 0 && Integer.bitCount(i) != Integer.bitCount(this.mChannelIndexMask)) {
                throw new IllegalArgumentException("Mismatched channel count for mask " + Integer.toHexString(i).toUpperCase());
            }
            this.mChannelMask = i;
            this.mPropertySetMask |= 4;
            return this;
        }

        public Builder setChannelIndexMask(int i) {
            if (i == 0) {
                throw new IllegalArgumentException("Invalid zero channel index mask");
            }
            if (this.mChannelMask != 0 && Integer.bitCount(i) != Integer.bitCount(this.mChannelMask)) {
                throw new IllegalArgumentException("Mismatched channel count for index mask " + Integer.toHexString(i).toUpperCase());
            }
            this.mChannelIndexMask = i;
            this.mPropertySetMask |= 8;
            return this;
        }

        public Builder setSampleRate(int i) throws IllegalArgumentException {
            if ((i < 4000 || i > 192000) && i != 0) {
                throw new IllegalArgumentException("Invalid sample rate " + i);
            }
            this.mSampleRate = i;
            this.mPropertySetMask |= 2;
            return this;
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AudioFormat audioFormat = (AudioFormat) obj;
        if (this.mPropertySetMask == audioFormat.mPropertySetMask) {
            if (((this.mPropertySetMask & 1) == 0 || this.mEncoding == audioFormat.mEncoding) && (((this.mPropertySetMask & 2) == 0 || this.mSampleRate == audioFormat.mSampleRate) && (((this.mPropertySetMask & 4) == 0 || this.mChannelMask == audioFormat.mChannelMask) && ((this.mPropertySetMask & 8) == 0 || this.mChannelIndexMask == audioFormat.mChannelIndexMask)))) {
                return true;
            }
            return false;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mPropertySetMask), Integer.valueOf(this.mSampleRate), Integer.valueOf(this.mEncoding), Integer.valueOf(this.mChannelMask), Integer.valueOf(this.mChannelIndexMask));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mPropertySetMask);
        parcel.writeInt(this.mEncoding);
        parcel.writeInt(this.mSampleRate);
        parcel.writeInt(this.mChannelMask);
        parcel.writeInt(this.mChannelIndexMask);
    }

    private AudioFormat(Parcel parcel) {
        this.mPropertySetMask = parcel.readInt();
        this.mEncoding = parcel.readInt();
        this.mSampleRate = parcel.readInt();
        this.mChannelMask = parcel.readInt();
        this.mChannelIndexMask = parcel.readInt();
    }

    public String toString() {
        return new String("AudioFormat: props=" + this.mPropertySetMask + " enc=" + this.mEncoding + " chan=0x" + Integer.toHexString(this.mChannelMask).toUpperCase() + " chan_index=0x" + Integer.toHexString(this.mChannelIndexMask).toUpperCase() + " rate=" + this.mSampleRate);
    }

    public static String toDisplayName(int i) {
        if (i == 10) {
            return "AAC";
        }
        if (i == 14) {
            return "Dolby TrueHD";
        }
        if (i != 18) {
            switch (i) {
                case 5:
                    return "Dolby Digital (AC3)";
                case 6:
                    return "Dolby Digital Plus (E_AC3)";
                case 7:
                    return "DTS";
                case 8:
                    return "DTS HD";
                default:
                    return "Unknown surround sound format";
            }
        }
        return "Dolby Atmos";
    }
}
