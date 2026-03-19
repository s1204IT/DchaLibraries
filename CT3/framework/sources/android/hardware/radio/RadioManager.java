package android.hardware.radio;

import android.content.Context;
import android.hardware.radio.RadioTuner;
import android.net.ProxyInfo;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;
import java.util.List;

public class RadioManager {
    public static final int BAND_AM = 0;
    public static final int BAND_AM_HD = 3;
    public static final int BAND_FM = 1;
    public static final int BAND_FM_HD = 2;
    public static final int CLASS_AM_FM = 0;
    public static final int CLASS_DT = 2;
    public static final int CLASS_SAT = 1;
    public static final int REGION_ITU_1 = 0;
    public static final int REGION_ITU_2 = 1;
    public static final int REGION_JAPAN = 3;
    public static final int REGION_KOREA = 4;
    public static final int REGION_OIRT = 2;
    public static final int STATUS_BAD_VALUE = -22;
    public static final int STATUS_DEAD_OBJECT = -32;
    public static final int STATUS_ERROR = Integer.MIN_VALUE;
    public static final int STATUS_INVALID_OPERATION = -38;
    public static final int STATUS_NO_INIT = -19;
    public static final int STATUS_OK = 0;
    public static final int STATUS_PERMISSION_DENIED = -1;
    public static final int STATUS_TIMED_OUT = -110;
    private final Context mContext;

    public native int listModules(List<ModuleProperties> list);

    public static class ModuleProperties implements Parcelable {
        public static final Parcelable.Creator<ModuleProperties> CREATOR = new Parcelable.Creator<ModuleProperties>() {
            @Override
            public ModuleProperties createFromParcel(Parcel in) {
                return new ModuleProperties(in, null);
            }

            @Override
            public ModuleProperties[] newArray(int size) {
                return new ModuleProperties[size];
            }
        };
        private final BandDescriptor[] mBands;
        private final int mClassId;
        private final int mId;
        private final String mImplementor;
        private final boolean mIsCaptureSupported;
        private final int mNumAudioSources;
        private final int mNumTuners;
        private final String mProduct;
        private final String mSerial;
        private final String mVersion;

        ModuleProperties(Parcel in, ModuleProperties moduleProperties) {
            this(in);
        }

        ModuleProperties(int id, int classId, String implementor, String product, String version, String serial, int numTuners, int numAudioSources, boolean isCaptureSupported, BandDescriptor[] bands) {
            this.mId = id;
            this.mClassId = classId;
            this.mImplementor = implementor;
            this.mProduct = product;
            this.mVersion = version;
            this.mSerial = serial;
            this.mNumTuners = numTuners;
            this.mNumAudioSources = numAudioSources;
            this.mIsCaptureSupported = isCaptureSupported;
            this.mBands = bands;
        }

        public int getId() {
            return this.mId;
        }

        public int getClassId() {
            return this.mClassId;
        }

        public String getImplementor() {
            return this.mImplementor;
        }

        public String getProduct() {
            return this.mProduct;
        }

        public String getVersion() {
            return this.mVersion;
        }

        public String getSerial() {
            return this.mSerial;
        }

        public int getNumTuners() {
            return this.mNumTuners;
        }

        public int getNumAudioSources() {
            return this.mNumAudioSources;
        }

        public boolean isCaptureSupported() {
            return this.mIsCaptureSupported;
        }

        public BandDescriptor[] getBands() {
            return this.mBands;
        }

        private ModuleProperties(Parcel in) {
            this.mId = in.readInt();
            this.mClassId = in.readInt();
            this.mImplementor = in.readString();
            this.mProduct = in.readString();
            this.mVersion = in.readString();
            this.mSerial = in.readString();
            this.mNumTuners = in.readInt();
            this.mNumAudioSources = in.readInt();
            this.mIsCaptureSupported = in.readInt() == 1;
            Parcelable[] tmp = in.readParcelableArray(BandDescriptor.class.getClassLoader());
            this.mBands = new BandDescriptor[tmp.length];
            for (int i = 0; i < tmp.length; i++) {
                this.mBands[i] = (BandDescriptor) tmp[i];
            }
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mId);
            dest.writeInt(this.mClassId);
            dest.writeString(this.mImplementor);
            dest.writeString(this.mProduct);
            dest.writeString(this.mVersion);
            dest.writeString(this.mSerial);
            dest.writeInt(this.mNumTuners);
            dest.writeInt(this.mNumAudioSources);
            dest.writeInt(this.mIsCaptureSupported ? 1 : 0);
            dest.writeParcelableArray(this.mBands, flags);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "ModuleProperties [mId=" + this.mId + ", mClassId=" + this.mClassId + ", mImplementor=" + this.mImplementor + ", mProduct=" + this.mProduct + ", mVersion=" + this.mVersion + ", mSerial=" + this.mSerial + ", mNumTuners=" + this.mNumTuners + ", mNumAudioSources=" + this.mNumAudioSources + ", mIsCaptureSupported=" + this.mIsCaptureSupported + ", mBands=" + Arrays.toString(this.mBands) + "]";
        }

        public int hashCode() {
            int result = this.mId + 31;
            return (((((((((((((((((result * 31) + this.mClassId) * 31) + (this.mImplementor == null ? 0 : this.mImplementor.hashCode())) * 31) + (this.mProduct == null ? 0 : this.mProduct.hashCode())) * 31) + (this.mVersion == null ? 0 : this.mVersion.hashCode())) * 31) + (this.mSerial == null ? 0 : this.mSerial.hashCode())) * 31) + this.mNumTuners) * 31) + this.mNumAudioSources) * 31) + (this.mIsCaptureSupported ? 1 : 0)) * 31) + Arrays.hashCode(this.mBands);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ModuleProperties)) {
                return false;
            }
            ModuleProperties other = (ModuleProperties) obj;
            if (this.mId != other.getId() || this.mClassId != other.getClassId()) {
                return false;
            }
            if (this.mImplementor == null) {
                if (other.getImplementor() != null) {
                    return false;
                }
            } else if (!this.mImplementor.equals(other.getImplementor())) {
                return false;
            }
            if (this.mProduct == null) {
                if (other.getProduct() != null) {
                    return false;
                }
            } else if (!this.mProduct.equals(other.getProduct())) {
                return false;
            }
            if (this.mVersion == null) {
                if (other.getVersion() != null) {
                    return false;
                }
            } else if (!this.mVersion.equals(other.getVersion())) {
                return false;
            }
            if (this.mSerial == null) {
                if (other.getSerial() != null) {
                    return false;
                }
            } else if (!this.mSerial.equals(other.getSerial())) {
                return false;
            }
            return this.mNumTuners == other.getNumTuners() && this.mNumAudioSources == other.getNumAudioSources() && this.mIsCaptureSupported == other.isCaptureSupported() && Arrays.equals(this.mBands, other.getBands());
        }
    }

    public static class BandDescriptor implements Parcelable {
        public static final Parcelable.Creator<BandDescriptor> CREATOR = new Parcelable.Creator<BandDescriptor>() {
            @Override
            public BandDescriptor createFromParcel(Parcel in) {
                return new BandDescriptor(in, null);
            }

            @Override
            public BandDescriptor[] newArray(int size) {
                return new BandDescriptor[size];
            }
        };
        private final int mLowerLimit;
        private final int mRegion;
        private final int mSpacing;
        private final int mType;
        private final int mUpperLimit;

        BandDescriptor(Parcel in, BandDescriptor bandDescriptor) {
            this(in);
        }

        BandDescriptor(int region, int type, int lowerLimit, int upperLimit, int spacing) {
            this.mRegion = region;
            this.mType = type;
            this.mLowerLimit = lowerLimit;
            this.mUpperLimit = upperLimit;
            this.mSpacing = spacing;
        }

        public int getRegion() {
            return this.mRegion;
        }

        public int getType() {
            return this.mType;
        }

        public int getLowerLimit() {
            return this.mLowerLimit;
        }

        public int getUpperLimit() {
            return this.mUpperLimit;
        }

        public int getSpacing() {
            return this.mSpacing;
        }

        private BandDescriptor(Parcel in) {
            this.mRegion = in.readInt();
            this.mType = in.readInt();
            this.mLowerLimit = in.readInt();
            this.mUpperLimit = in.readInt();
            this.mSpacing = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mRegion);
            dest.writeInt(this.mType);
            dest.writeInt(this.mLowerLimit);
            dest.writeInt(this.mUpperLimit);
            dest.writeInt(this.mSpacing);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "BandDescriptor [mRegion=" + this.mRegion + ", mType=" + this.mType + ", mLowerLimit=" + this.mLowerLimit + ", mUpperLimit=" + this.mUpperLimit + ", mSpacing=" + this.mSpacing + "]";
        }

        public int hashCode() {
            int result = this.mRegion + 31;
            return (((((((result * 31) + this.mType) * 31) + this.mLowerLimit) * 31) + this.mUpperLimit) * 31) + this.mSpacing;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BandDescriptor)) {
                return false;
            }
            BandDescriptor other = (BandDescriptor) obj;
            return this.mRegion == other.getRegion() && this.mType == other.getType() && this.mLowerLimit == other.getLowerLimit() && this.mUpperLimit == other.getUpperLimit() && this.mSpacing == other.getSpacing();
        }
    }

    public static class FmBandDescriptor extends BandDescriptor {
        public static final Parcelable.Creator<FmBandDescriptor> CREATOR = new Parcelable.Creator<FmBandDescriptor>() {
            @Override
            public FmBandDescriptor createFromParcel(Parcel in) {
                return new FmBandDescriptor(in, null);
            }

            @Override
            public FmBandDescriptor[] newArray(int size) {
                return new FmBandDescriptor[size];
            }
        };
        private final boolean mAf;
        private final boolean mEa;
        private final boolean mRds;
        private final boolean mStereo;
        private final boolean mTa;

        FmBandDescriptor(Parcel in, FmBandDescriptor fmBandDescriptor) {
            this(in);
        }

        FmBandDescriptor(int region, int type, int lowerLimit, int upperLimit, int spacing, boolean stereo, boolean rds, boolean ta, boolean af, boolean ea) {
            super(region, type, lowerLimit, upperLimit, spacing);
            this.mStereo = stereo;
            this.mRds = rds;
            this.mTa = ta;
            this.mAf = af;
            this.mEa = ea;
        }

        public boolean isStereoSupported() {
            return this.mStereo;
        }

        public boolean isRdsSupported() {
            return this.mRds;
        }

        public boolean isTaSupported() {
            return this.mTa;
        }

        public boolean isAfSupported() {
            return this.mAf;
        }

        public boolean isEaSupported() {
            return this.mEa;
        }

        private FmBandDescriptor(Parcel in) {
            super(in, null);
            this.mStereo = in.readByte() == 1;
            this.mRds = in.readByte() == 1;
            this.mTa = in.readByte() == 1;
            this.mAf = in.readByte() == 1;
            this.mEa = in.readByte() == 1;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeByte((byte) (this.mStereo ? 1 : 0));
            dest.writeByte((byte) (this.mRds ? 1 : 0));
            dest.writeByte((byte) (this.mTa ? 1 : 0));
            dest.writeByte((byte) (this.mAf ? 1 : 0));
            dest.writeByte((byte) (this.mEa ? 1 : 0));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public String toString() {
            return "FmBandDescriptor [ " + super.toString() + " mStereo=" + this.mStereo + ", mRds=" + this.mRds + ", mTa=" + this.mTa + ", mAf=" + this.mAf + ", mEa =" + this.mEa + "]";
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            return (((((((((result * 31) + (this.mStereo ? 1 : 0)) * 31) + (this.mRds ? 1 : 0)) * 31) + (this.mTa ? 1 : 0)) * 31) + (this.mAf ? 1 : 0)) * 31) + (this.mEa ? 1 : 0);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj) || !(obj instanceof FmBandDescriptor)) {
                return false;
            }
            FmBandDescriptor other = (FmBandDescriptor) obj;
            return this.mStereo == other.isStereoSupported() && this.mRds == other.isRdsSupported() && this.mTa == other.isTaSupported() && this.mAf == other.isAfSupported() && this.mEa == other.isEaSupported();
        }
    }

    public static class AmBandDescriptor extends BandDescriptor {
        public static final Parcelable.Creator<AmBandDescriptor> CREATOR = new Parcelable.Creator<AmBandDescriptor>() {
            @Override
            public AmBandDescriptor createFromParcel(Parcel in) {
                return new AmBandDescriptor(in, null);
            }

            @Override
            public AmBandDescriptor[] newArray(int size) {
                return new AmBandDescriptor[size];
            }
        };
        private final boolean mStereo;

        AmBandDescriptor(Parcel in, AmBandDescriptor amBandDescriptor) {
            this(in);
        }

        AmBandDescriptor(int region, int type, int lowerLimit, int upperLimit, int spacing, boolean stereo) {
            super(region, type, lowerLimit, upperLimit, spacing);
            this.mStereo = stereo;
        }

        public boolean isStereoSupported() {
            return this.mStereo;
        }

        private AmBandDescriptor(Parcel in) {
            super(in, null);
            this.mStereo = in.readByte() == 1;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeByte((byte) (this.mStereo ? 1 : 0));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public String toString() {
            return "AmBandDescriptor [ " + super.toString() + " mStereo=" + this.mStereo + "]";
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            return (result * 31) + (this.mStereo ? 1 : 0);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj) || !(obj instanceof AmBandDescriptor)) {
                return false;
            }
            AmBandDescriptor other = (AmBandDescriptor) obj;
            return this.mStereo == other.isStereoSupported();
        }
    }

    public static class BandConfig implements Parcelable {
        public static final Parcelable.Creator<BandConfig> CREATOR = new Parcelable.Creator<BandConfig>() {
            @Override
            public BandConfig createFromParcel(Parcel in) {
                return new BandConfig(in, null);
            }

            @Override
            public BandConfig[] newArray(int size) {
                return new BandConfig[size];
            }
        };
        final BandDescriptor mDescriptor;

        BandConfig(Parcel in, BandConfig bandConfig) {
            this(in);
        }

        BandConfig(BandDescriptor descriptor) {
            this.mDescriptor = descriptor;
        }

        BandConfig(int region, int type, int lowerLimit, int upperLimit, int spacing) {
            this.mDescriptor = new BandDescriptor(region, type, lowerLimit, upperLimit, spacing);
        }

        private BandConfig(Parcel in) {
            this.mDescriptor = new BandDescriptor(in, null);
        }

        BandDescriptor getDescriptor() {
            return this.mDescriptor;
        }

        public int getRegion() {
            return this.mDescriptor.getRegion();
        }

        public int getType() {
            return this.mDescriptor.getType();
        }

        public int getLowerLimit() {
            return this.mDescriptor.getLowerLimit();
        }

        public int getUpperLimit() {
            return this.mDescriptor.getUpperLimit();
        }

        public int getSpacing() {
            return this.mDescriptor.getSpacing();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            this.mDescriptor.writeToParcel(dest, flags);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "BandConfig [ " + this.mDescriptor.toString() + "]";
        }

        public int hashCode() {
            int result = this.mDescriptor.hashCode() + 31;
            return result;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BandConfig)) {
                return false;
            }
            BandConfig other = (BandConfig) obj;
            return this.mDescriptor == other.getDescriptor();
        }
    }

    public static class FmBandConfig extends BandConfig {
        public static final Parcelable.Creator<FmBandConfig> CREATOR = new Parcelable.Creator<FmBandConfig>() {
            @Override
            public FmBandConfig createFromParcel(Parcel in) {
                return new FmBandConfig(in, null);
            }

            @Override
            public FmBandConfig[] newArray(int size) {
                return new FmBandConfig[size];
            }
        };
        private final boolean mAf;
        private final boolean mEa;
        private final boolean mRds;
        private final boolean mStereo;
        private final boolean mTa;

        FmBandConfig(Parcel in, FmBandConfig fmBandConfig) {
            this(in);
        }

        FmBandConfig(FmBandDescriptor descriptor) {
            super(descriptor);
            this.mStereo = descriptor.isStereoSupported();
            this.mRds = descriptor.isRdsSupported();
            this.mTa = descriptor.isTaSupported();
            this.mAf = descriptor.isAfSupported();
            this.mEa = descriptor.isEaSupported();
        }

        FmBandConfig(int region, int type, int lowerLimit, int upperLimit, int spacing, boolean stereo, boolean rds, boolean ta, boolean af, boolean ea) {
            super(region, type, lowerLimit, upperLimit, spacing);
            this.mStereo = stereo;
            this.mRds = rds;
            this.mTa = ta;
            this.mAf = af;
            this.mEa = ea;
        }

        public boolean getStereo() {
            return this.mStereo;
        }

        public boolean getRds() {
            return this.mRds;
        }

        public boolean getTa() {
            return this.mTa;
        }

        public boolean getAf() {
            return this.mAf;
        }

        public boolean getEa() {
            return this.mEa;
        }

        private FmBandConfig(Parcel in) {
            super(in, null);
            this.mStereo = in.readByte() == 1;
            this.mRds = in.readByte() == 1;
            this.mTa = in.readByte() == 1;
            this.mAf = in.readByte() == 1;
            this.mEa = in.readByte() == 1;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeByte((byte) (this.mStereo ? 1 : 0));
            dest.writeByte((byte) (this.mRds ? 1 : 0));
            dest.writeByte((byte) (this.mTa ? 1 : 0));
            dest.writeByte((byte) (this.mAf ? 1 : 0));
            dest.writeByte((byte) (this.mEa ? 1 : 0));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public String toString() {
            return "FmBandConfig [" + super.toString() + ", mStereo=" + this.mStereo + ", mRds=" + this.mRds + ", mTa=" + this.mTa + ", mAf=" + this.mAf + ", mEa =" + this.mEa + "]";
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            return (((((((((result * 31) + (this.mStereo ? 1 : 0)) * 31) + (this.mRds ? 1 : 0)) * 31) + (this.mTa ? 1 : 0)) * 31) + (this.mAf ? 1 : 0)) * 31) + (this.mEa ? 1 : 0);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj) || !(obj instanceof FmBandConfig)) {
                return false;
            }
            FmBandConfig other = (FmBandConfig) obj;
            return this.mStereo == other.mStereo && this.mRds == other.mRds && this.mTa == other.mTa && this.mAf == other.mAf && this.mEa == other.mEa;
        }

        public static class Builder {
            private boolean mAf;
            private final BandDescriptor mDescriptor;
            private boolean mEa;
            private boolean mRds;
            private boolean mStereo;
            private boolean mTa;

            public Builder(FmBandDescriptor descriptor) {
                this.mDescriptor = new BandDescriptor(descriptor.getRegion(), descriptor.getType(), descriptor.getLowerLimit(), descriptor.getUpperLimit(), descriptor.getSpacing());
                this.mStereo = descriptor.isStereoSupported();
                this.mRds = descriptor.isRdsSupported();
                this.mTa = descriptor.isTaSupported();
                this.mAf = descriptor.isAfSupported();
                this.mEa = descriptor.isEaSupported();
            }

            public Builder(FmBandConfig config) {
                this.mDescriptor = new BandDescriptor(config.getRegion(), config.getType(), config.getLowerLimit(), config.getUpperLimit(), config.getSpacing());
                this.mStereo = config.getStereo();
                this.mRds = config.getRds();
                this.mTa = config.getTa();
                this.mAf = config.getAf();
                this.mEa = config.getEa();
            }

            public FmBandConfig build() {
                FmBandConfig config = new FmBandConfig(this.mDescriptor.getRegion(), this.mDescriptor.getType(), this.mDescriptor.getLowerLimit(), this.mDescriptor.getUpperLimit(), this.mDescriptor.getSpacing(), this.mStereo, this.mRds, this.mTa, this.mAf, this.mEa);
                return config;
            }

            public Builder setStereo(boolean state) {
                this.mStereo = state;
                return this;
            }

            public Builder setRds(boolean state) {
                this.mRds = state;
                return this;
            }

            public Builder setTa(boolean state) {
                this.mTa = state;
                return this;
            }

            public Builder setAf(boolean state) {
                this.mAf = state;
                return this;
            }

            public Builder setEa(boolean state) {
                this.mEa = state;
                return this;
            }
        }
    }

    public static class AmBandConfig extends BandConfig {
        public static final Parcelable.Creator<AmBandConfig> CREATOR = new Parcelable.Creator<AmBandConfig>() {
            @Override
            public AmBandConfig createFromParcel(Parcel in) {
                return new AmBandConfig(in, null);
            }

            @Override
            public AmBandConfig[] newArray(int size) {
                return new AmBandConfig[size];
            }
        };
        private final boolean mStereo;

        AmBandConfig(Parcel in, AmBandConfig amBandConfig) {
            this(in);
        }

        AmBandConfig(AmBandDescriptor descriptor) {
            super(descriptor);
            this.mStereo = descriptor.isStereoSupported();
        }

        AmBandConfig(int region, int type, int lowerLimit, int upperLimit, int spacing, boolean stereo) {
            super(region, type, lowerLimit, upperLimit, spacing);
            this.mStereo = stereo;
        }

        public boolean getStereo() {
            return this.mStereo;
        }

        private AmBandConfig(Parcel in) {
            super(in, null);
            this.mStereo = in.readByte() == 1;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeByte((byte) (this.mStereo ? 1 : 0));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public String toString() {
            return "AmBandConfig [" + super.toString() + ", mStereo=" + this.mStereo + "]";
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            return (result * 31) + (this.mStereo ? 1 : 0);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj) || !(obj instanceof AmBandConfig)) {
                return false;
            }
            AmBandConfig other = (AmBandConfig) obj;
            return this.mStereo == other.getStereo();
        }

        public static class Builder {
            private final BandDescriptor mDescriptor;
            private boolean mStereo;

            public Builder(AmBandDescriptor descriptor) {
                this.mDescriptor = new BandDescriptor(descriptor.getRegion(), descriptor.getType(), descriptor.getLowerLimit(), descriptor.getUpperLimit(), descriptor.getSpacing());
                this.mStereo = descriptor.isStereoSupported();
            }

            public Builder(AmBandConfig config) {
                this.mDescriptor = new BandDescriptor(config.getRegion(), config.getType(), config.getLowerLimit(), config.getUpperLimit(), config.getSpacing());
                this.mStereo = config.getStereo();
            }

            public AmBandConfig build() {
                AmBandConfig config = new AmBandConfig(this.mDescriptor.getRegion(), this.mDescriptor.getType(), this.mDescriptor.getLowerLimit(), this.mDescriptor.getUpperLimit(), this.mDescriptor.getSpacing(), this.mStereo);
                return config;
            }

            public Builder setStereo(boolean state) {
                this.mStereo = state;
                return this;
            }
        }
    }

    public static class ProgramInfo implements Parcelable {
        public static final Parcelable.Creator<ProgramInfo> CREATOR = new Parcelable.Creator<ProgramInfo>() {
            @Override
            public ProgramInfo createFromParcel(Parcel in) {
                return new ProgramInfo(in, null);
            }

            @Override
            public ProgramInfo[] newArray(int size) {
                return new ProgramInfo[size];
            }
        };
        private final int mChannel;
        private final boolean mDigital;
        private final RadioMetadata mMetadata;
        private final int mSignalStrength;
        private final boolean mStereo;
        private final int mSubChannel;
        private final boolean mTuned;

        ProgramInfo(Parcel in, ProgramInfo programInfo) {
            this(in);
        }

        ProgramInfo(int channel, int subChannel, boolean tuned, boolean stereo, boolean digital, int signalStrength, RadioMetadata metadata) {
            this.mChannel = channel;
            this.mSubChannel = subChannel;
            this.mTuned = tuned;
            this.mStereo = stereo;
            this.mDigital = digital;
            this.mSignalStrength = signalStrength;
            this.mMetadata = metadata;
        }

        public int getChannel() {
            return this.mChannel;
        }

        public int getSubChannel() {
            return this.mSubChannel;
        }

        public boolean isTuned() {
            return this.mTuned;
        }

        public boolean isStereo() {
            return this.mStereo;
        }

        public boolean isDigital() {
            return this.mDigital;
        }

        public int getSignalStrength() {
            return this.mSignalStrength;
        }

        public RadioMetadata getMetadata() {
            return this.mMetadata;
        }

        private ProgramInfo(Parcel in) {
            this.mChannel = in.readInt();
            this.mSubChannel = in.readInt();
            this.mTuned = in.readByte() == 1;
            this.mStereo = in.readByte() == 1;
            this.mDigital = in.readByte() == 1;
            this.mSignalStrength = in.readInt();
            if (in.readByte() == 1) {
                this.mMetadata = RadioMetadata.CREATOR.createFromParcel(in);
            } else {
                this.mMetadata = null;
            }
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mChannel);
            dest.writeInt(this.mSubChannel);
            dest.writeByte((byte) (this.mTuned ? 1 : 0));
            dest.writeByte((byte) (this.mStereo ? 1 : 0));
            dest.writeByte((byte) (this.mDigital ? 1 : 0));
            dest.writeInt(this.mSignalStrength);
            if (this.mMetadata == null) {
                dest.writeByte((byte) 0);
            } else {
                dest.writeByte((byte) 1);
                this.mMetadata.writeToParcel(dest, flags);
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "ProgramInfo [mChannel=" + this.mChannel + ", mSubChannel=" + this.mSubChannel + ", mTuned=" + this.mTuned + ", mStereo=" + this.mStereo + ", mDigital=" + this.mDigital + ", mSignalStrength=" + this.mSignalStrength + (this.mMetadata == null ? ProxyInfo.LOCAL_EXCL_LIST : ", mMetadata=" + this.mMetadata.toString()) + "]";
        }

        public int hashCode() {
            int result = this.mChannel + 31;
            return (((((((((((result * 31) + this.mSubChannel) * 31) + (this.mTuned ? 1 : 0)) * 31) + (this.mStereo ? 1 : 0)) * 31) + (this.mDigital ? 1 : 0)) * 31) + this.mSignalStrength) * 31) + (this.mMetadata != null ? this.mMetadata.hashCode() : 0);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ProgramInfo)) {
                return false;
            }
            ProgramInfo other = (ProgramInfo) obj;
            if (this.mChannel != other.getChannel() || this.mSubChannel != other.getSubChannel() || this.mTuned != other.isTuned() || this.mStereo != other.isStereo() || this.mDigital != other.isDigital() || this.mSignalStrength != other.getSignalStrength()) {
                return false;
            }
            if (this.mMetadata == null) {
                if (other.getMetadata() != null) {
                    return false;
                }
            } else if (!this.mMetadata.equals(other.getMetadata())) {
                return false;
            }
            return true;
        }
    }

    public RadioTuner openTuner(int moduleId, BandConfig config, boolean withAudio, RadioTuner.Callback callback, Handler handler) {
        if (callback == null) {
            return null;
        }
        RadioModule module = new RadioModule(moduleId, config, withAudio, callback, handler);
        if (module != null && !module.initCheck()) {
            return null;
        }
        return module;
    }

    public RadioManager(Context context) {
        this.mContext = context;
    }
}
