package android.net.wifi.aware;

import android.net.wifi.aware.TlvBufferUtils;
import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import libcore.util.HexEncoding;

public final class SubscribeConfig implements Parcelable {
    public static final Parcelable.Creator<SubscribeConfig> CREATOR = new Parcelable.Creator<SubscribeConfig>() {
        @Override
        public SubscribeConfig[] newArray(int i) {
            return new SubscribeConfig[i];
        }

        @Override
        public SubscribeConfig createFromParcel(Parcel parcel) {
            byte[] bArrCreateByteArray = parcel.createByteArray();
            byte[] bArrCreateByteArray2 = parcel.createByteArray();
            byte[] bArrCreateByteArray3 = parcel.createByteArray();
            int i = parcel.readInt();
            int i2 = parcel.readInt();
            boolean z = parcel.readInt() != 0;
            int i3 = parcel.readInt();
            return new SubscribeConfig(bArrCreateByteArray, bArrCreateByteArray2, bArrCreateByteArray3, i, i2, z, parcel.readInt() != 0, i3, parcel.readInt() != 0, parcel.readInt());
        }
    };
    public static final int SUBSCRIBE_TYPE_ACTIVE = 1;
    public static final int SUBSCRIBE_TYPE_PASSIVE = 0;
    public final boolean mEnableTerminateNotification;
    public final byte[] mMatchFilter;
    public final int mMaxDistanceMm;
    public final boolean mMaxDistanceMmSet;
    public final int mMinDistanceMm;
    public final boolean mMinDistanceMmSet;
    public final byte[] mServiceName;
    public final byte[] mServiceSpecificInfo;
    public final int mSubscribeType;
    public final int mTtlSec;

    @Retention(RetentionPolicy.SOURCE)
    public @interface SubscribeTypes {
    }

    public SubscribeConfig(byte[] bArr, byte[] bArr2, byte[] bArr3, int i, int i2, boolean z, boolean z2, int i3, boolean z3, int i4) {
        this.mServiceName = bArr;
        this.mServiceSpecificInfo = bArr2;
        this.mMatchFilter = bArr3;
        this.mSubscribeType = i;
        this.mTtlSec = i2;
        this.mEnableTerminateNotification = z;
        this.mMinDistanceMm = i3;
        this.mMinDistanceMmSet = z2;
        this.mMaxDistanceMm = i4;
        this.mMaxDistanceMmSet = z3;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SubscribeConfig [mServiceName='");
        sb.append(this.mServiceName == null ? "<null>" : String.valueOf(HexEncoding.encode(this.mServiceName)));
        sb.append(", mServiceName.length=");
        sb.append(this.mServiceName == null ? 0 : this.mServiceName.length);
        sb.append(", mServiceSpecificInfo='");
        sb.append(this.mServiceSpecificInfo == null ? "<null>" : String.valueOf(HexEncoding.encode(this.mServiceSpecificInfo)));
        sb.append(", mServiceSpecificInfo.length=");
        sb.append(this.mServiceSpecificInfo == null ? 0 : this.mServiceSpecificInfo.length);
        sb.append(", mMatchFilter=");
        sb.append(new TlvBufferUtils.TlvIterable(0, 1, this.mMatchFilter).toString());
        sb.append(", mMatchFilter.length=");
        sb.append(this.mMatchFilter != null ? this.mMatchFilter.length : 0);
        sb.append(", mSubscribeType=");
        sb.append(this.mSubscribeType);
        sb.append(", mTtlSec=");
        sb.append(this.mTtlSec);
        sb.append(", mEnableTerminateNotification=");
        sb.append(this.mEnableTerminateNotification);
        sb.append(", mMinDistanceMm=");
        sb.append(this.mMinDistanceMm);
        sb.append(", mMinDistanceMmSet=");
        sb.append(this.mMinDistanceMmSet);
        sb.append(", mMaxDistanceMm=");
        sb.append(this.mMaxDistanceMm);
        sb.append(", mMaxDistanceMmSet=");
        sb.append(this.mMaxDistanceMmSet);
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByteArray(this.mServiceName);
        parcel.writeByteArray(this.mServiceSpecificInfo);
        parcel.writeByteArray(this.mMatchFilter);
        parcel.writeInt(this.mSubscribeType);
        parcel.writeInt(this.mTtlSec);
        parcel.writeInt(this.mEnableTerminateNotification ? 1 : 0);
        parcel.writeInt(this.mMinDistanceMm);
        parcel.writeInt(this.mMinDistanceMmSet ? 1 : 0);
        parcel.writeInt(this.mMaxDistanceMm);
        parcel.writeInt(this.mMaxDistanceMmSet ? 1 : 0);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SubscribeConfig)) {
            return false;
        }
        SubscribeConfig subscribeConfig = (SubscribeConfig) obj;
        if (!Arrays.equals(this.mServiceName, subscribeConfig.mServiceName) || !Arrays.equals(this.mServiceSpecificInfo, subscribeConfig.mServiceSpecificInfo) || !Arrays.equals(this.mMatchFilter, subscribeConfig.mMatchFilter) || this.mSubscribeType != subscribeConfig.mSubscribeType || this.mTtlSec != subscribeConfig.mTtlSec || this.mEnableTerminateNotification != subscribeConfig.mEnableTerminateNotification || this.mMinDistanceMmSet != subscribeConfig.mMinDistanceMmSet || this.mMaxDistanceMmSet != subscribeConfig.mMaxDistanceMmSet) {
            return false;
        }
        if (!this.mMinDistanceMmSet || this.mMinDistanceMm == subscribeConfig.mMinDistanceMm) {
            return !this.mMaxDistanceMmSet || this.mMaxDistanceMm == subscribeConfig.mMaxDistanceMm;
        }
        return false;
    }

    public int hashCode() {
        int iHash = Objects.hash(this.mServiceName, this.mServiceSpecificInfo, this.mMatchFilter, Integer.valueOf(this.mSubscribeType), Integer.valueOf(this.mTtlSec), Boolean.valueOf(this.mEnableTerminateNotification), Boolean.valueOf(this.mMinDistanceMmSet), Boolean.valueOf(this.mMaxDistanceMmSet));
        if (this.mMinDistanceMmSet) {
            iHash = Objects.hash(Integer.valueOf(iHash), Integer.valueOf(this.mMinDistanceMm));
        }
        return this.mMaxDistanceMmSet ? Objects.hash(Integer.valueOf(iHash), Integer.valueOf(this.mMaxDistanceMm)) : iHash;
    }

    public void assertValid(Characteristics characteristics, boolean z) throws IllegalArgumentException {
        WifiAwareUtils.validateServiceName(this.mServiceName);
        if (!TlvBufferUtils.isValid(this.mMatchFilter, 0, 1)) {
            throw new IllegalArgumentException("Invalid matchFilter configuration - LV fields do not match up to length");
        }
        if (this.mSubscribeType < 0 || this.mSubscribeType > 1) {
            throw new IllegalArgumentException("Invalid subscribeType - " + this.mSubscribeType);
        }
        if (this.mTtlSec < 0) {
            throw new IllegalArgumentException("Invalid ttlSec - must be non-negative");
        }
        if (characteristics != null) {
            int maxServiceNameLength = characteristics.getMaxServiceNameLength();
            if (maxServiceNameLength != 0 && this.mServiceName.length > maxServiceNameLength) {
                throw new IllegalArgumentException("Service name longer than supported by device characteristics");
            }
            int maxServiceSpecificInfoLength = characteristics.getMaxServiceSpecificInfoLength();
            if (maxServiceSpecificInfoLength != 0 && this.mServiceSpecificInfo != null && this.mServiceSpecificInfo.length > maxServiceSpecificInfoLength) {
                throw new IllegalArgumentException("Service specific info longer than supported by device characteristics");
            }
            int maxMatchFilterLength = characteristics.getMaxMatchFilterLength();
            if (maxMatchFilterLength != 0 && this.mMatchFilter != null && this.mMatchFilter.length > maxMatchFilterLength) {
                throw new IllegalArgumentException("Match filter longer than supported by device characteristics");
            }
        }
        if (this.mMinDistanceMmSet && this.mMinDistanceMm < 0) {
            throw new IllegalArgumentException("Minimum distance must be non-negative");
        }
        if (this.mMaxDistanceMmSet && this.mMaxDistanceMm < 0) {
            throw new IllegalArgumentException("Maximum distance must be non-negative");
        }
        if (this.mMinDistanceMmSet && this.mMaxDistanceMmSet && this.mMaxDistanceMm <= this.mMinDistanceMm) {
            throw new IllegalArgumentException("Maximum distance must be greater than minimum distance");
        }
        if (z) {
            return;
        }
        if (this.mMinDistanceMmSet || this.mMaxDistanceMmSet) {
            throw new IllegalArgumentException("Ranging is not supported");
        }
    }

    public static final class Builder {
        private byte[] mMatchFilter;
        private int mMaxDistanceMm;
        private int mMinDistanceMm;
        private byte[] mServiceName;
        private byte[] mServiceSpecificInfo;
        private int mSubscribeType = 0;
        private int mTtlSec = 0;
        private boolean mEnableTerminateNotification = true;
        private boolean mMinDistanceMmSet = false;
        private boolean mMaxDistanceMmSet = false;

        public Builder setServiceName(String str) {
            if (str == null) {
                throw new IllegalArgumentException("Invalid service name - must be non-null");
            }
            this.mServiceName = str.getBytes(StandardCharsets.UTF_8);
            return this;
        }

        public Builder setServiceSpecificInfo(byte[] bArr) {
            this.mServiceSpecificInfo = bArr;
            return this;
        }

        public Builder setMatchFilter(List<byte[]> list) {
            this.mMatchFilter = new TlvBufferUtils.TlvConstructor(0, 1).allocateAndPut(list).getArray();
            return this;
        }

        public Builder setSubscribeType(int i) {
            if (i < 0 || i > 1) {
                throw new IllegalArgumentException("Invalid subscribeType - " + i);
            }
            this.mSubscribeType = i;
            return this;
        }

        public Builder setTtlSec(int i) {
            if (i < 0) {
                throw new IllegalArgumentException("Invalid ttlSec - must be non-negative");
            }
            this.mTtlSec = i;
            return this;
        }

        public Builder setTerminateNotificationEnabled(boolean z) {
            this.mEnableTerminateNotification = z;
            return this;
        }

        public Builder setMinDistanceMm(int i) {
            this.mMinDistanceMm = i;
            this.mMinDistanceMmSet = true;
            return this;
        }

        public Builder setMaxDistanceMm(int i) {
            this.mMaxDistanceMm = i;
            this.mMaxDistanceMmSet = true;
            return this;
        }

        public SubscribeConfig build() {
            return new SubscribeConfig(this.mServiceName, this.mServiceSpecificInfo, this.mMatchFilter, this.mSubscribeType, this.mTtlSec, this.mEnableTerminateNotification, this.mMinDistanceMmSet, this.mMinDistanceMm, this.mMaxDistanceMmSet, this.mMaxDistanceMm);
        }
    }
}
