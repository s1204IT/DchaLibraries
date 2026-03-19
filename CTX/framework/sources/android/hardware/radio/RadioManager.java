package android.hardware.radio;

import android.annotation.SystemApi;
import android.content.Context;
import android.hardware.radio.Announcement;
import android.hardware.radio.IAnnouncementListener;
import android.hardware.radio.IRadioService;
import android.hardware.radio.ProgramSelector;
import android.hardware.radio.RadioTuner;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telecom.Logging.Session;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

@SystemApi
public class RadioManager {
    public static final int BAND_AM = 0;
    public static final int BAND_AM_HD = 3;
    public static final int BAND_FM = 1;
    public static final int BAND_FM_HD = 2;
    public static final int BAND_INVALID = -1;
    public static final int CLASS_AM_FM = 0;
    public static final int CLASS_DT = 2;
    public static final int CLASS_SAT = 1;
    public static final int CONFIG_DAB_DAB_LINKING = 6;
    public static final int CONFIG_DAB_DAB_SOFT_LINKING = 8;
    public static final int CONFIG_DAB_FM_LINKING = 7;
    public static final int CONFIG_DAB_FM_SOFT_LINKING = 9;
    public static final int CONFIG_FORCE_ANALOG = 2;
    public static final int CONFIG_FORCE_DIGITAL = 3;
    public static final int CONFIG_FORCE_MONO = 1;
    public static final int CONFIG_RDS_AF = 4;
    public static final int CONFIG_RDS_REG = 5;
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
    private static final String TAG = "BroadcastRadio.manager";
    private final Context mContext;
    private final Map<Announcement.OnListUpdatedListener, ICloseHandle> mAnnouncementListeners = new HashMap();
    private final IRadioService mService = IRadioService.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.RADIO_SERVICE));

    @Retention(RetentionPolicy.SOURCE)
    public @interface Band {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ConfigFlag {
    }

    private native int nativeListModules(List<ModuleProperties> list);

    public static class ModuleProperties implements Parcelable {
        public static final Parcelable.Creator<ModuleProperties> CREATOR = new Parcelable.Creator<ModuleProperties>() {
            @Override
            public ModuleProperties createFromParcel(Parcel parcel) {
                return new ModuleProperties(parcel, null);
            }

            @Override
            public ModuleProperties[] newArray(int i) {
                return new ModuleProperties[i];
            }
        };
        private final BandDescriptor[] mBands;
        private final int mClassId;
        private final Map<String, Integer> mDabFrequencyTable;
        private final int mId;
        private final String mImplementor;
        private final boolean mIsBgScanSupported;
        private final boolean mIsCaptureSupported;
        private final boolean mIsInitializationRequired;
        private final int mNumAudioSources;
        private final int mNumTuners;
        private final String mProduct;
        private final String mSerial;
        private final String mServiceName;
        private final Set<Integer> mSupportedIdentifierTypes;
        private final Set<Integer> mSupportedProgramTypes;
        private final Map<String, String> mVendorInfo;
        private final String mVersion;

        ModuleProperties(Parcel parcel, AnonymousClass1 anonymousClass1) {
            this(parcel);
        }

        public ModuleProperties(int i, String str, int i2, String str2, String str3, String str4, String str5, int i3, int i4, boolean z, boolean z2, BandDescriptor[] bandDescriptorArr, boolean z3, int[] iArr, int[] iArr2, Map<String, Integer> map, Map<String, String> map2) {
            this.mId = i;
            this.mServiceName = TextUtils.isEmpty(str) ? PhoneConstants.APN_TYPE_DEFAULT : str;
            this.mClassId = i2;
            this.mImplementor = str2;
            this.mProduct = str3;
            this.mVersion = str4;
            this.mSerial = str5;
            this.mNumTuners = i3;
            this.mNumAudioSources = i4;
            this.mIsInitializationRequired = z;
            this.mIsCaptureSupported = z2;
            this.mBands = bandDescriptorArr;
            this.mIsBgScanSupported = z3;
            this.mSupportedProgramTypes = arrayToSet(iArr);
            this.mSupportedIdentifierTypes = arrayToSet(iArr2);
            if (map != null) {
                for (Map.Entry<String, Integer> entry : map.entrySet()) {
                    Objects.requireNonNull(entry.getKey());
                    Objects.requireNonNull(entry.getValue());
                }
            }
            this.mDabFrequencyTable = map;
            this.mVendorInfo = map2 == null ? new HashMap<>() : map2;
        }

        private static Set<Integer> arrayToSet(int[] iArr) {
            return (Set) Arrays.stream(iArr).boxed().collect(Collectors.toSet());
        }

        private static int[] setToArray(Set<Integer> set) {
            return set.stream().mapToInt(new ToIntFunction() {
                @Override
                public final int applyAsInt(Object obj) {
                    return ((Integer) obj).intValue();
                }
            }).toArray();
        }

        public int getId() {
            return this.mId;
        }

        public String getServiceName() {
            return this.mServiceName;
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

        public boolean isInitializationRequired() {
            return this.mIsInitializationRequired;
        }

        public boolean isCaptureSupported() {
            return this.mIsCaptureSupported;
        }

        public boolean isBackgroundScanningSupported() {
            return this.mIsBgScanSupported;
        }

        public boolean isProgramTypeSupported(int i) {
            return this.mSupportedProgramTypes.contains(Integer.valueOf(i));
        }

        public boolean isProgramIdentifierSupported(int i) {
            return this.mSupportedIdentifierTypes.contains(Integer.valueOf(i));
        }

        public Map<String, Integer> getDabFrequencyTable() {
            return this.mDabFrequencyTable;
        }

        public Map<String, String> getVendorInfo() {
            return this.mVendorInfo;
        }

        public BandDescriptor[] getBands() {
            return this.mBands;
        }

        private ModuleProperties(Parcel parcel) {
            this.mId = parcel.readInt();
            String string = parcel.readString();
            this.mServiceName = TextUtils.isEmpty(string) ? PhoneConstants.APN_TYPE_DEFAULT : string;
            this.mClassId = parcel.readInt();
            this.mImplementor = parcel.readString();
            this.mProduct = parcel.readString();
            this.mVersion = parcel.readString();
            this.mSerial = parcel.readString();
            this.mNumTuners = parcel.readInt();
            this.mNumAudioSources = parcel.readInt();
            this.mIsInitializationRequired = parcel.readInt() == 1;
            this.mIsCaptureSupported = parcel.readInt() == 1;
            Parcelable[] parcelableArray = parcel.readParcelableArray(BandDescriptor.class.getClassLoader());
            this.mBands = new BandDescriptor[parcelableArray.length];
            for (int i = 0; i < parcelableArray.length; i++) {
                this.mBands[i] = (BandDescriptor) parcelableArray[i];
            }
            this.mIsBgScanSupported = parcel.readInt() == 1;
            this.mSupportedProgramTypes = arrayToSet(parcel.createIntArray());
            this.mSupportedIdentifierTypes = arrayToSet(parcel.createIntArray());
            this.mDabFrequencyTable = Utils.readStringIntMap(parcel);
            this.mVendorInfo = Utils.readStringMap(parcel);
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.mId);
            parcel.writeString(this.mServiceName);
            parcel.writeInt(this.mClassId);
            parcel.writeString(this.mImplementor);
            parcel.writeString(this.mProduct);
            parcel.writeString(this.mVersion);
            parcel.writeString(this.mSerial);
            parcel.writeInt(this.mNumTuners);
            parcel.writeInt(this.mNumAudioSources);
            parcel.writeInt(this.mIsInitializationRequired ? 1 : 0);
            parcel.writeInt(this.mIsCaptureSupported ? 1 : 0);
            parcel.writeParcelableArray(this.mBands, i);
            parcel.writeInt(this.mIsBgScanSupported ? 1 : 0);
            parcel.writeIntArray(setToArray(this.mSupportedProgramTypes));
            parcel.writeIntArray(setToArray(this.mSupportedIdentifierTypes));
            Utils.writeStringIntMap(parcel, this.mDabFrequencyTable);
            Utils.writeStringMap(parcel, this.mVendorInfo);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "ModuleProperties [mId=" + this.mId + ", mServiceName=" + this.mServiceName + ", mClassId=" + this.mClassId + ", mImplementor=" + this.mImplementor + ", mProduct=" + this.mProduct + ", mVersion=" + this.mVersion + ", mSerial=" + this.mSerial + ", mNumTuners=" + this.mNumTuners + ", mNumAudioSources=" + this.mNumAudioSources + ", mIsInitializationRequired=" + this.mIsInitializationRequired + ", mIsCaptureSupported=" + this.mIsCaptureSupported + ", mIsBgScanSupported=" + this.mIsBgScanSupported + ", mBands=" + Arrays.toString(this.mBands) + "]";
        }

        public int hashCode() {
            return Objects.hash(Integer.valueOf(this.mId), this.mServiceName, Integer.valueOf(this.mClassId), this.mImplementor, this.mProduct, this.mVersion, this.mSerial, Integer.valueOf(this.mNumTuners), Integer.valueOf(this.mNumAudioSources), Boolean.valueOf(this.mIsInitializationRequired), Boolean.valueOf(this.mIsCaptureSupported), this.mBands, Boolean.valueOf(this.mIsBgScanSupported), this.mDabFrequencyTable, this.mVendorInfo);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ModuleProperties)) {
                return false;
            }
            ModuleProperties moduleProperties = (ModuleProperties) obj;
            return this.mId == moduleProperties.getId() && TextUtils.equals(this.mServiceName, moduleProperties.mServiceName) && this.mClassId == moduleProperties.mClassId && Objects.equals(this.mImplementor, moduleProperties.mImplementor) && Objects.equals(this.mProduct, moduleProperties.mProduct) && Objects.equals(this.mVersion, moduleProperties.mVersion) && Objects.equals(this.mSerial, moduleProperties.mSerial) && this.mNumTuners == moduleProperties.mNumTuners && this.mNumAudioSources == moduleProperties.mNumAudioSources && this.mIsInitializationRequired == moduleProperties.mIsInitializationRequired && this.mIsCaptureSupported == moduleProperties.mIsCaptureSupported && Objects.equals(this.mBands, moduleProperties.mBands) && this.mIsBgScanSupported == moduleProperties.mIsBgScanSupported && Objects.equals(this.mDabFrequencyTable, moduleProperties.mDabFrequencyTable) && Objects.equals(this.mVendorInfo, moduleProperties.mVendorInfo);
        }
    }

    public static class BandDescriptor implements Parcelable {
        public static final Parcelable.Creator<BandDescriptor> CREATOR = new Parcelable.Creator<BandDescriptor>() {
            @Override
            public BandDescriptor createFromParcel(Parcel parcel) {
                int iLookupTypeFromParcel = BandDescriptor.lookupTypeFromParcel(parcel);
                AnonymousClass1 anonymousClass1 = null;
                switch (iLookupTypeFromParcel) {
                    case 0:
                    case 3:
                        return new AmBandDescriptor(parcel, anonymousClass1);
                    case 1:
                    case 2:
                        return new FmBandDescriptor(parcel, anonymousClass1);
                    default:
                        throw new IllegalArgumentException("Unsupported band: " + iLookupTypeFromParcel);
                }
            }

            @Override
            public BandDescriptor[] newArray(int i) {
                return new BandDescriptor[i];
            }
        };
        private final int mLowerLimit;
        private final int mRegion;
        private final int mSpacing;
        private final int mType;
        private final int mUpperLimit;

        BandDescriptor(Parcel parcel, AnonymousClass1 anonymousClass1) {
            this(parcel);
        }

        BandDescriptor(int i, int i2, int i3, int i4, int i5) {
            if (i2 != 0 && i2 != 1 && i2 != 2 && i2 != 3) {
                throw new IllegalArgumentException("Unsupported band: " + i2);
            }
            this.mRegion = i;
            this.mType = i2;
            this.mLowerLimit = i3;
            this.mUpperLimit = i4;
            this.mSpacing = i5;
        }

        public int getRegion() {
            return this.mRegion;
        }

        public int getType() {
            return this.mType;
        }

        public boolean isAmBand() {
            return this.mType == 0 || this.mType == 3;
        }

        public boolean isFmBand() {
            return this.mType == 1 || this.mType == 2;
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

        private BandDescriptor(Parcel parcel) {
            this.mRegion = parcel.readInt();
            this.mType = parcel.readInt();
            this.mLowerLimit = parcel.readInt();
            this.mUpperLimit = parcel.readInt();
            this.mSpacing = parcel.readInt();
        }

        private static int lookupTypeFromParcel(Parcel parcel) {
            int iDataPosition = parcel.dataPosition();
            parcel.readInt();
            int i = parcel.readInt();
            parcel.setDataPosition(iDataPosition);
            return i;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.mRegion);
            parcel.writeInt(this.mType);
            parcel.writeInt(this.mLowerLimit);
            parcel.writeInt(this.mUpperLimit);
            parcel.writeInt(this.mSpacing);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "BandDescriptor [mRegion=" + this.mRegion + ", mType=" + this.mType + ", mLowerLimit=" + this.mLowerLimit + ", mUpperLimit=" + this.mUpperLimit + ", mSpacing=" + this.mSpacing + "]";
        }

        public int hashCode() {
            return (31 * (((((((this.mRegion + 31) * 31) + this.mType) * 31) + this.mLowerLimit) * 31) + this.mUpperLimit)) + this.mSpacing;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BandDescriptor)) {
                return false;
            }
            BandDescriptor bandDescriptor = (BandDescriptor) obj;
            return this.mRegion == bandDescriptor.getRegion() && this.mType == bandDescriptor.getType() && this.mLowerLimit == bandDescriptor.getLowerLimit() && this.mUpperLimit == bandDescriptor.getUpperLimit() && this.mSpacing == bandDescriptor.getSpacing();
        }
    }

    public static class FmBandDescriptor extends BandDescriptor {
        public static final Parcelable.Creator<FmBandDescriptor> CREATOR = new Parcelable.Creator<FmBandDescriptor>() {
            @Override
            public FmBandDescriptor createFromParcel(Parcel parcel) {
                return new FmBandDescriptor(parcel, null);
            }

            @Override
            public FmBandDescriptor[] newArray(int i) {
                return new FmBandDescriptor[i];
            }
        };
        private final boolean mAf;
        private final boolean mEa;
        private final boolean mRds;
        private final boolean mStereo;
        private final boolean mTa;

        FmBandDescriptor(Parcel parcel, AnonymousClass1 anonymousClass1) {
            this(parcel);
        }

        public FmBandDescriptor(int i, int i2, int i3, int i4, int i5, boolean z, boolean z2, boolean z3, boolean z4, boolean z5) {
            super(i, i2, i3, i4, i5);
            this.mStereo = z;
            this.mRds = z2;
            this.mTa = z3;
            this.mAf = z4;
            this.mEa = z5;
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

        private FmBandDescriptor(Parcel parcel) {
            super(parcel, null);
            this.mStereo = parcel.readByte() == 1;
            this.mRds = parcel.readByte() == 1;
            this.mTa = parcel.readByte() == 1;
            this.mAf = parcel.readByte() == 1;
            this.mEa = parcel.readByte() == 1;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeByte(this.mStereo ? (byte) 1 : (byte) 0);
            parcel.writeByte(this.mRds ? (byte) 1 : (byte) 0);
            parcel.writeByte(this.mTa ? (byte) 1 : (byte) 0);
            parcel.writeByte(this.mAf ? (byte) 1 : (byte) 0);
            parcel.writeByte(this.mEa ? (byte) 1 : (byte) 0);
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
            return (31 * ((((((((super.hashCode() * 31) + (this.mStereo ? 1 : 0)) * 31) + (this.mRds ? 1 : 0)) * 31) + (this.mTa ? 1 : 0)) * 31) + (this.mAf ? 1 : 0))) + (this.mEa ? 1 : 0);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj) || !(obj instanceof FmBandDescriptor)) {
                return false;
            }
            FmBandDescriptor fmBandDescriptor = (FmBandDescriptor) obj;
            return this.mStereo == fmBandDescriptor.isStereoSupported() && this.mRds == fmBandDescriptor.isRdsSupported() && this.mTa == fmBandDescriptor.isTaSupported() && this.mAf == fmBandDescriptor.isAfSupported() && this.mEa == fmBandDescriptor.isEaSupported();
        }
    }

    public static class AmBandDescriptor extends BandDescriptor {
        public static final Parcelable.Creator<AmBandDescriptor> CREATOR = new Parcelable.Creator<AmBandDescriptor>() {
            @Override
            public AmBandDescriptor createFromParcel(Parcel parcel) {
                return new AmBandDescriptor(parcel, null);
            }

            @Override
            public AmBandDescriptor[] newArray(int i) {
                return new AmBandDescriptor[i];
            }
        };
        private final boolean mStereo;

        AmBandDescriptor(Parcel parcel, AnonymousClass1 anonymousClass1) {
            this(parcel);
        }

        public AmBandDescriptor(int i, int i2, int i3, int i4, int i5, boolean z) {
            super(i, i2, i3, i4, i5);
            this.mStereo = z;
        }

        public boolean isStereoSupported() {
            return this.mStereo;
        }

        private AmBandDescriptor(Parcel parcel) {
            super(parcel, null);
            this.mStereo = parcel.readByte() == 1;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeByte(this.mStereo ? (byte) 1 : (byte) 0);
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
            return (31 * super.hashCode()) + (this.mStereo ? 1 : 0);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            return super.equals(obj) && (obj instanceof AmBandDescriptor) && this.mStereo == ((AmBandDescriptor) obj).isStereoSupported();
        }
    }

    public static class BandConfig implements Parcelable {
        public static final Parcelable.Creator<BandConfig> CREATOR = new Parcelable.Creator<BandConfig>() {
            @Override
            public BandConfig createFromParcel(Parcel parcel) {
                int iLookupTypeFromParcel = BandDescriptor.lookupTypeFromParcel(parcel);
                AnonymousClass1 anonymousClass1 = null;
                switch (iLookupTypeFromParcel) {
                    case 0:
                    case 3:
                        return new AmBandConfig(parcel, anonymousClass1);
                    case 1:
                    case 2:
                        return new FmBandConfig(parcel, anonymousClass1);
                    default:
                        throw new IllegalArgumentException("Unsupported band: " + iLookupTypeFromParcel);
                }
            }

            @Override
            public BandConfig[] newArray(int i) {
                return new BandConfig[i];
            }
        };
        final BandDescriptor mDescriptor;

        BandConfig(Parcel parcel, AnonymousClass1 anonymousClass1) {
            this(parcel);
        }

        BandConfig(BandDescriptor bandDescriptor) {
            this.mDescriptor = (BandDescriptor) Objects.requireNonNull(bandDescriptor);
        }

        BandConfig(int i, int i2, int i3, int i4, int i5) {
            this.mDescriptor = new BandDescriptor(i, i2, i3, i4, i5);
        }

        private BandConfig(Parcel parcel) {
            this.mDescriptor = new BandDescriptor(parcel, null);
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
        public void writeToParcel(Parcel parcel, int i) {
            this.mDescriptor.writeToParcel(parcel, i);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "BandConfig [ " + this.mDescriptor.toString() + "]";
        }

        public int hashCode() {
            return 31 + this.mDescriptor.hashCode();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BandConfig)) {
                return false;
            }
            BandDescriptor descriptor = ((BandConfig) obj).getDescriptor();
            if ((this.mDescriptor == null) != (descriptor == null)) {
                return false;
            }
            return this.mDescriptor == null || this.mDescriptor.equals(descriptor);
        }
    }

    public static class FmBandConfig extends BandConfig {
        public static final Parcelable.Creator<FmBandConfig> CREATOR = new Parcelable.Creator<FmBandConfig>() {
            @Override
            public FmBandConfig createFromParcel(Parcel parcel) {
                return new FmBandConfig(parcel, null);
            }

            @Override
            public FmBandConfig[] newArray(int i) {
                return new FmBandConfig[i];
            }
        };
        private final boolean mAf;
        private final boolean mEa;
        private final boolean mRds;
        private final boolean mStereo;
        private final boolean mTa;

        FmBandConfig(Parcel parcel, AnonymousClass1 anonymousClass1) {
            this(parcel);
        }

        public FmBandConfig(FmBandDescriptor fmBandDescriptor) {
            super(fmBandDescriptor);
            this.mStereo = fmBandDescriptor.isStereoSupported();
            this.mRds = fmBandDescriptor.isRdsSupported();
            this.mTa = fmBandDescriptor.isTaSupported();
            this.mAf = fmBandDescriptor.isAfSupported();
            this.mEa = fmBandDescriptor.isEaSupported();
        }

        FmBandConfig(int i, int i2, int i3, int i4, int i5, boolean z, boolean z2, boolean z3, boolean z4, boolean z5) {
            super(i, i2, i3, i4, i5);
            this.mStereo = z;
            this.mRds = z2;
            this.mTa = z3;
            this.mAf = z4;
            this.mEa = z5;
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

        private FmBandConfig(Parcel parcel) {
            super(parcel, null);
            this.mStereo = parcel.readByte() == 1;
            this.mRds = parcel.readByte() == 1;
            this.mTa = parcel.readByte() == 1;
            this.mAf = parcel.readByte() == 1;
            this.mEa = parcel.readByte() == 1;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeByte(this.mStereo ? (byte) 1 : (byte) 0);
            parcel.writeByte(this.mRds ? (byte) 1 : (byte) 0);
            parcel.writeByte(this.mTa ? (byte) 1 : (byte) 0);
            parcel.writeByte(this.mAf ? (byte) 1 : (byte) 0);
            parcel.writeByte(this.mEa ? (byte) 1 : (byte) 0);
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
            return (31 * ((((((((super.hashCode() * 31) + (this.mStereo ? 1 : 0)) * 31) + (this.mRds ? 1 : 0)) * 31) + (this.mTa ? 1 : 0)) * 31) + (this.mAf ? 1 : 0))) + (this.mEa ? 1 : 0);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj) || !(obj instanceof FmBandConfig)) {
                return false;
            }
            FmBandConfig fmBandConfig = (FmBandConfig) obj;
            return this.mStereo == fmBandConfig.mStereo && this.mRds == fmBandConfig.mRds && this.mTa == fmBandConfig.mTa && this.mAf == fmBandConfig.mAf && this.mEa == fmBandConfig.mEa;
        }

        public static class Builder {
            private boolean mAf;
            private final BandDescriptor mDescriptor;
            private boolean mEa;
            private boolean mRds;
            private boolean mStereo;
            private boolean mTa;

            public Builder(FmBandDescriptor fmBandDescriptor) {
                this.mDescriptor = new BandDescriptor(fmBandDescriptor.getRegion(), fmBandDescriptor.getType(), fmBandDescriptor.getLowerLimit(), fmBandDescriptor.getUpperLimit(), fmBandDescriptor.getSpacing());
                this.mStereo = fmBandDescriptor.isStereoSupported();
                this.mRds = fmBandDescriptor.isRdsSupported();
                this.mTa = fmBandDescriptor.isTaSupported();
                this.mAf = fmBandDescriptor.isAfSupported();
                this.mEa = fmBandDescriptor.isEaSupported();
            }

            public Builder(FmBandConfig fmBandConfig) {
                this.mDescriptor = new BandDescriptor(fmBandConfig.getRegion(), fmBandConfig.getType(), fmBandConfig.getLowerLimit(), fmBandConfig.getUpperLimit(), fmBandConfig.getSpacing());
                this.mStereo = fmBandConfig.getStereo();
                this.mRds = fmBandConfig.getRds();
                this.mTa = fmBandConfig.getTa();
                this.mAf = fmBandConfig.getAf();
                this.mEa = fmBandConfig.getEa();
            }

            public FmBandConfig build() {
                return new FmBandConfig(this.mDescriptor.getRegion(), this.mDescriptor.getType(), this.mDescriptor.getLowerLimit(), this.mDescriptor.getUpperLimit(), this.mDescriptor.getSpacing(), this.mStereo, this.mRds, this.mTa, this.mAf, this.mEa);
            }

            public Builder setStereo(boolean z) {
                this.mStereo = z;
                return this;
            }

            public Builder setRds(boolean z) {
                this.mRds = z;
                return this;
            }

            public Builder setTa(boolean z) {
                this.mTa = z;
                return this;
            }

            public Builder setAf(boolean z) {
                this.mAf = z;
                return this;
            }

            public Builder setEa(boolean z) {
                this.mEa = z;
                return this;
            }
        }
    }

    public static class AmBandConfig extends BandConfig {
        public static final Parcelable.Creator<AmBandConfig> CREATOR = new Parcelable.Creator<AmBandConfig>() {
            @Override
            public AmBandConfig createFromParcel(Parcel parcel) {
                return new AmBandConfig(parcel, null);
            }

            @Override
            public AmBandConfig[] newArray(int i) {
                return new AmBandConfig[i];
            }
        };
        private final boolean mStereo;

        AmBandConfig(Parcel parcel, AnonymousClass1 anonymousClass1) {
            this(parcel);
        }

        public AmBandConfig(AmBandDescriptor amBandDescriptor) {
            super(amBandDescriptor);
            this.mStereo = amBandDescriptor.isStereoSupported();
        }

        AmBandConfig(int i, int i2, int i3, int i4, int i5, boolean z) {
            super(i, i2, i3, i4, i5);
            this.mStereo = z;
        }

        public boolean getStereo() {
            return this.mStereo;
        }

        private AmBandConfig(Parcel parcel) {
            super(parcel, null);
            this.mStereo = parcel.readByte() == 1;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeByte(this.mStereo ? (byte) 1 : (byte) 0);
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
            return (31 * super.hashCode()) + (this.mStereo ? 1 : 0);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            return super.equals(obj) && (obj instanceof AmBandConfig) && this.mStereo == ((AmBandConfig) obj).getStereo();
        }

        public static class Builder {
            private final BandDescriptor mDescriptor;
            private boolean mStereo;

            public Builder(AmBandDescriptor amBandDescriptor) {
                this.mDescriptor = new BandDescriptor(amBandDescriptor.getRegion(), amBandDescriptor.getType(), amBandDescriptor.getLowerLimit(), amBandDescriptor.getUpperLimit(), amBandDescriptor.getSpacing());
                this.mStereo = amBandDescriptor.isStereoSupported();
            }

            public Builder(AmBandConfig amBandConfig) {
                this.mDescriptor = new BandDescriptor(amBandConfig.getRegion(), amBandConfig.getType(), amBandConfig.getLowerLimit(), amBandConfig.getUpperLimit(), amBandConfig.getSpacing());
                this.mStereo = amBandConfig.getStereo();
            }

            public AmBandConfig build() {
                return new AmBandConfig(this.mDescriptor.getRegion(), this.mDescriptor.getType(), this.mDescriptor.getLowerLimit(), this.mDescriptor.getUpperLimit(), this.mDescriptor.getSpacing(), this.mStereo);
            }

            public Builder setStereo(boolean z) {
                this.mStereo = z;
                return this;
            }
        }
    }

    public static class ProgramInfo implements Parcelable {
        public static final Parcelable.Creator<ProgramInfo> CREATOR = new Parcelable.Creator<ProgramInfo>() {
            @Override
            public ProgramInfo createFromParcel(Parcel parcel) {
                return new ProgramInfo(parcel, null);
            }

            @Override
            public ProgramInfo[] newArray(int i) {
                return new ProgramInfo[i];
            }
        };
        private static final int FLAG_LIVE = 1;
        private static final int FLAG_MUTED = 2;
        private static final int FLAG_STEREO = 32;
        private static final int FLAG_TRAFFIC_ANNOUNCEMENT = 8;
        private static final int FLAG_TRAFFIC_PROGRAM = 4;
        private static final int FLAG_TUNED = 16;
        private final int mInfoFlags;
        private final ProgramSelector.Identifier mLogicallyTunedTo;
        private final RadioMetadata mMetadata;
        private final ProgramSelector.Identifier mPhysicallyTunedTo;
        private final Collection<ProgramSelector.Identifier> mRelatedContent;
        private final ProgramSelector mSelector;
        private final int mSignalQuality;
        private final Map<String, String> mVendorInfo;

        ProgramInfo(Parcel parcel, AnonymousClass1 anonymousClass1) {
            this(parcel);
        }

        public ProgramInfo(ProgramSelector programSelector, ProgramSelector.Identifier identifier, ProgramSelector.Identifier identifier2, Collection<ProgramSelector.Identifier> collection, int i, int i2, RadioMetadata radioMetadata, Map<String, String> map) {
            this.mSelector = (ProgramSelector) Objects.requireNonNull(programSelector);
            this.mLogicallyTunedTo = identifier;
            this.mPhysicallyTunedTo = identifier2;
            if (collection == null) {
                this.mRelatedContent = Collections.emptyList();
            } else {
                Preconditions.checkCollectionElementsNotNull(collection, "relatedContent");
                this.mRelatedContent = collection;
            }
            this.mInfoFlags = i;
            this.mSignalQuality = i2;
            this.mMetadata = radioMetadata;
            this.mVendorInfo = map == null ? new HashMap<>() : map;
        }

        public ProgramSelector getSelector() {
            return this.mSelector;
        }

        public ProgramSelector.Identifier getLogicallyTunedTo() {
            return this.mLogicallyTunedTo;
        }

        public ProgramSelector.Identifier getPhysicallyTunedTo() {
            return this.mPhysicallyTunedTo;
        }

        public Collection<ProgramSelector.Identifier> getRelatedContent() {
            return this.mRelatedContent;
        }

        @Deprecated
        public int getChannel() {
            try {
                return (int) this.mSelector.getFirstId(1);
            } catch (IllegalArgumentException e) {
                Log.w(RadioManager.TAG, "Not an AM/FM program");
                return 0;
            }
        }

        @Deprecated
        public int getSubChannel() {
            try {
                return ((int) this.mSelector.getFirstId(4)) + 1;
            } catch (IllegalArgumentException e) {
                return 0;
            }
        }

        public boolean isTuned() {
            return (this.mInfoFlags & 16) != 0;
        }

        public boolean isStereo() {
            return (this.mInfoFlags & 32) != 0;
        }

        @Deprecated
        public boolean isDigital() {
            ProgramSelector.Identifier primaryId = this.mLogicallyTunedTo;
            if (primaryId == null) {
                primaryId = this.mSelector.getPrimaryId();
            }
            int type = primaryId.getType();
            return (type == 1 || type == 2) ? false : true;
        }

        public boolean isLive() {
            return (this.mInfoFlags & 1) != 0;
        }

        public boolean isMuted() {
            return (this.mInfoFlags & 2) != 0;
        }

        public boolean isTrafficProgram() {
            return (this.mInfoFlags & 4) != 0;
        }

        public boolean isTrafficAnnouncementActive() {
            return (this.mInfoFlags & 8) != 0;
        }

        public int getSignalStrength() {
            return this.mSignalQuality;
        }

        public RadioMetadata getMetadata() {
            return this.mMetadata;
        }

        public Map<String, String> getVendorInfo() {
            return this.mVendorInfo;
        }

        private ProgramInfo(Parcel parcel) {
            this.mSelector = (ProgramSelector) Objects.requireNonNull((ProgramSelector) parcel.readTypedObject(ProgramSelector.CREATOR));
            this.mLogicallyTunedTo = (ProgramSelector.Identifier) parcel.readTypedObject(ProgramSelector.Identifier.CREATOR);
            this.mPhysicallyTunedTo = (ProgramSelector.Identifier) parcel.readTypedObject(ProgramSelector.Identifier.CREATOR);
            this.mRelatedContent = parcel.createTypedArrayList(ProgramSelector.Identifier.CREATOR);
            this.mInfoFlags = parcel.readInt();
            this.mSignalQuality = parcel.readInt();
            this.mMetadata = (RadioMetadata) parcel.readTypedObject(RadioMetadata.CREATOR);
            this.mVendorInfo = Utils.readStringMap(parcel);
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeTypedObject(this.mSelector, i);
            parcel.writeTypedObject(this.mLogicallyTunedTo, i);
            parcel.writeTypedObject(this.mPhysicallyTunedTo, i);
            Utils.writeTypedCollection(parcel, this.mRelatedContent);
            parcel.writeInt(this.mInfoFlags);
            parcel.writeInt(this.mSignalQuality);
            parcel.writeTypedObject(this.mMetadata, i);
            Utils.writeStringMap(parcel, this.mVendorInfo);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "ProgramInfo [selector=" + this.mSelector + ", logicallyTunedTo=" + Objects.toString(this.mLogicallyTunedTo) + ", physicallyTunedTo=" + Objects.toString(this.mPhysicallyTunedTo) + ", relatedContent=" + this.mRelatedContent.size() + ", infoFlags=" + this.mInfoFlags + ", mSignalQuality=" + this.mSignalQuality + ", mMetadata=" + Objects.toString(this.mMetadata) + "]";
        }

        public int hashCode() {
            return Objects.hash(this.mSelector, this.mLogicallyTunedTo, this.mPhysicallyTunedTo, this.mRelatedContent, Integer.valueOf(this.mInfoFlags), Integer.valueOf(this.mSignalQuality), this.mMetadata, this.mVendorInfo);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ProgramInfo)) {
                return false;
            }
            ProgramInfo programInfo = (ProgramInfo) obj;
            return Objects.equals(this.mSelector, programInfo.mSelector) && Objects.equals(this.mLogicallyTunedTo, programInfo.mLogicallyTunedTo) && Objects.equals(this.mPhysicallyTunedTo, programInfo.mPhysicallyTunedTo) && Objects.equals(this.mRelatedContent, programInfo.mRelatedContent) && this.mInfoFlags == programInfo.mInfoFlags && this.mSignalQuality == programInfo.mSignalQuality && Objects.equals(this.mMetadata, programInfo.mMetadata) && Objects.equals(this.mVendorInfo, programInfo.mVendorInfo);
        }
    }

    public int listModules(List<ModuleProperties> list) {
        if (list == null) {
            Log.e(TAG, "the output list must not be empty");
            return -22;
        }
        Log.d(TAG, "Listing available tuners...");
        try {
            List<ModuleProperties> listListModules = this.mService.listModules();
            if (listListModules == null) {
                Log.e(TAG, "Returned list was a null");
                return Integer.MIN_VALUE;
            }
            list.addAll(listListModules);
            return 0;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed listing available tuners", e);
            return -32;
        }
    }

    public RadioTuner openTuner(int i, BandConfig bandConfig, boolean z, RadioTuner.Callback callback, Handler handler) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be empty");
        }
        Log.d(TAG, "Opening tuner " + i + Session.TRUNCATE_STRING);
        TunerCallbackAdapter tunerCallbackAdapter = new TunerCallbackAdapter(callback, handler);
        try {
            ITuner iTunerOpenTuner = this.mService.openTuner(i, bandConfig, z, tunerCallbackAdapter);
            if (iTunerOpenTuner == null) {
                Log.e(TAG, "Failed to open tuner");
                return null;
            }
            return new TunerAdapter(iTunerOpenTuner, tunerCallbackAdapter, bandConfig != null ? bandConfig.getType() : -1);
        } catch (RemoteException | IllegalArgumentException e) {
            Log.e(TAG, "Failed to open tuner", e);
            return null;
        }
    }

    public void addAnnouncementListener(Set<Integer> set, Announcement.OnListUpdatedListener onListUpdatedListener) {
        addAnnouncementListener(new Executor() {
            @Override
            public final void execute(Runnable runnable) {
                runnable.run();
            }
        }, set, onListUpdatedListener);
    }

    public void addAnnouncementListener(Executor executor, Set<Integer> set, Announcement.OnListUpdatedListener onListUpdatedListener) {
        ICloseHandle iCloseHandleAddAnnouncementListener;
        Objects.requireNonNull(executor);
        Objects.requireNonNull(onListUpdatedListener);
        int[] array = set.stream().mapToInt(new ToIntFunction() {
            @Override
            public final int applyAsInt(Object obj) {
                return ((Integer) obj).intValue();
            }
        }).toArray();
        AnonymousClass1 anonymousClass1 = new AnonymousClass1(executor, onListUpdatedListener);
        synchronized (this.mAnnouncementListeners) {
            try {
                iCloseHandleAddAnnouncementListener = this.mService.addAnnouncementListener(array, anonymousClass1);
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
                iCloseHandleAddAnnouncementListener = null;
            }
            Objects.requireNonNull(iCloseHandleAddAnnouncementListener);
            ICloseHandle iCloseHandlePut = this.mAnnouncementListeners.put(onListUpdatedListener, iCloseHandleAddAnnouncementListener);
            if (iCloseHandlePut != null) {
                Utils.close(iCloseHandlePut);
            }
        }
    }

    class AnonymousClass1 extends IAnnouncementListener.Stub {
        final Executor val$executor;
        final Announcement.OnListUpdatedListener val$listener;

        AnonymousClass1(Executor executor, Announcement.OnListUpdatedListener onListUpdatedListener) {
            this.val$executor = executor;
            this.val$listener = onListUpdatedListener;
        }

        @Override
        public void onListUpdated(final List<Announcement> list) {
            Executor executor = this.val$executor;
            final Announcement.OnListUpdatedListener onListUpdatedListener = this.val$listener;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    onListUpdatedListener.onListUpdated(list);
                }
            });
        }
    }

    public void removeAnnouncementListener(Announcement.OnListUpdatedListener onListUpdatedListener) {
        Objects.requireNonNull(onListUpdatedListener);
        synchronized (this.mAnnouncementListeners) {
            ICloseHandle iCloseHandleRemove = this.mAnnouncementListeners.remove(onListUpdatedListener);
            if (iCloseHandleRemove != null) {
                Utils.close(iCloseHandleRemove);
            }
        }
    }

    public RadioManager(Context context) throws ServiceManager.ServiceNotFoundException {
        this.mContext = context;
    }
}
