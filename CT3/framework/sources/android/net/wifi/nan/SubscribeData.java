package android.net.wifi.nan;

import android.net.wifi.nan.TlvBufferUtils;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

public class SubscribeData implements Parcelable {
    public static final Parcelable.Creator<SubscribeData> CREATOR = new Parcelable.Creator<SubscribeData>() {
        @Override
        public SubscribeData[] newArray(int size) {
            return new SubscribeData[size];
        }

        @Override
        public SubscribeData createFromParcel(Parcel in) {
            String serviceName = in.readString();
            int ssiLength = in.readInt();
            byte[] ssi = new byte[ssiLength];
            if (ssiLength != 0) {
                in.readByteArray(ssi);
            }
            int txFilterLength = in.readInt();
            byte[] txFilter = new byte[txFilterLength];
            if (txFilterLength != 0) {
                in.readByteArray(txFilter);
            }
            int rxFilterLength = in.readInt();
            byte[] rxFilter = new byte[rxFilterLength];
            if (rxFilterLength != 0) {
                in.readByteArray(rxFilter);
            }
            return new SubscribeData(serviceName, ssi, ssiLength, txFilter, txFilterLength, rxFilter, rxFilterLength, null);
        }
    };
    public final byte[] mRxFilter;
    public final int mRxFilterLength;
    public final String mServiceName;
    public final byte[] mServiceSpecificInfo;
    public final int mServiceSpecificInfoLength;
    public final byte[] mTxFilter;
    public final int mTxFilterLength;

    SubscribeData(String serviceName, byte[] serviceSpecificInfo, int serviceSpecificInfoLength, byte[] txFilter, int txFilterLength, byte[] rxFilter, int rxFilterLength, SubscribeData subscribeData) {
        this(serviceName, serviceSpecificInfo, serviceSpecificInfoLength, txFilter, txFilterLength, rxFilter, rxFilterLength);
    }

    private SubscribeData(String serviceName, byte[] serviceSpecificInfo, int serviceSpecificInfoLength, byte[] txFilter, int txFilterLength, byte[] rxFilter, int rxFilterLength) {
        this.mServiceName = serviceName;
        this.mServiceSpecificInfoLength = serviceSpecificInfoLength;
        this.mServiceSpecificInfo = serviceSpecificInfo;
        this.mTxFilterLength = txFilterLength;
        this.mTxFilter = txFilter;
        this.mRxFilterLength = rxFilterLength;
        this.mRxFilter = rxFilter;
    }

    public String toString() {
        return "SubscribeData [mServiceName='" + this.mServiceName + "', mServiceSpecificInfo='" + new String(this.mServiceSpecificInfo, 0, this.mServiceSpecificInfoLength) + "', mTxFilter=" + new TlvBufferUtils.TlvIterable(0, 1, this.mTxFilter, this.mTxFilterLength).toString() + ", mRxFilter=" + new TlvBufferUtils.TlvIterable(0, 1, this.mRxFilter, this.mRxFilterLength).toString() + "']";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mServiceName);
        dest.writeInt(this.mServiceSpecificInfoLength);
        if (this.mServiceSpecificInfoLength != 0) {
            dest.writeByteArray(this.mServiceSpecificInfo, 0, this.mServiceSpecificInfoLength);
        }
        dest.writeInt(this.mTxFilterLength);
        if (this.mTxFilterLength != 0) {
            dest.writeByteArray(this.mTxFilter, 0, this.mTxFilterLength);
        }
        dest.writeInt(this.mRxFilterLength);
        if (this.mRxFilterLength == 0) {
            return;
        }
        dest.writeByteArray(this.mRxFilter, 0, this.mRxFilterLength);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SubscribeData) || !this.mServiceName.equals(obj.mServiceName) || this.mServiceSpecificInfoLength != obj.mServiceSpecificInfoLength || this.mTxFilterLength != obj.mTxFilterLength || this.mRxFilterLength != obj.mRxFilterLength) {
            return false;
        }
        if (this.mServiceSpecificInfo != null && obj.mServiceSpecificInfo != null) {
            for (int i = 0; i < this.mServiceSpecificInfoLength; i++) {
                if (this.mServiceSpecificInfo[i] != obj.mServiceSpecificInfo[i]) {
                    return false;
                }
            }
        } else if (this.mServiceSpecificInfoLength != 0) {
            return false;
        }
        if (this.mTxFilter != null && obj.mTxFilter != null) {
            for (int i2 = 0; i2 < this.mTxFilterLength; i2++) {
                if (this.mTxFilter[i2] != obj.mTxFilter[i2]) {
                    return false;
                }
            }
        } else if (this.mTxFilterLength != 0) {
            return false;
        }
        if (this.mRxFilter != null && obj.mRxFilter != null) {
            for (int i3 = 0; i3 < this.mRxFilterLength; i3++) {
                if (this.mRxFilter[i3] != obj.mRxFilter[i3]) {
                    return false;
                }
            }
        } else if (this.mRxFilterLength != 0) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int result = this.mServiceName.hashCode() + 527;
        return (((((((((((result * 31) + this.mServiceSpecificInfoLength) * 31) + Arrays.hashCode(this.mServiceSpecificInfo)) * 31) + this.mTxFilterLength) * 31) + Arrays.hashCode(this.mTxFilter)) * 31) + this.mRxFilterLength) * 31) + Arrays.hashCode(this.mRxFilter);
    }

    public static final class Builder {
        private int mRxFilterLength;
        private String mServiceName;
        private int mServiceSpecificInfoLength;
        private int mTxFilterLength;
        private byte[] mServiceSpecificInfo = new byte[0];
        private byte[] mTxFilter = new byte[0];
        private byte[] mRxFilter = new byte[0];

        public Builder setServiceName(String serviceName) {
            this.mServiceName = serviceName;
            return this;
        }

        public Builder setServiceSpecificInfo(byte[] serviceSpecificInfo, int serviceSpecificInfoLength) {
            this.mServiceSpecificInfoLength = serviceSpecificInfoLength;
            this.mServiceSpecificInfo = serviceSpecificInfo;
            return this;
        }

        public Builder setServiceSpecificInfo(String serviceSpecificInfoStr) {
            this.mServiceSpecificInfoLength = serviceSpecificInfoStr.length();
            this.mServiceSpecificInfo = serviceSpecificInfoStr.getBytes();
            return this;
        }

        public Builder setTxFilter(byte[] txFilter, int txFilterLength) {
            this.mTxFilter = txFilter;
            this.mTxFilterLength = txFilterLength;
            return this;
        }

        public Builder setRxFilter(byte[] rxFilter, int rxFilterLength) {
            this.mRxFilter = rxFilter;
            this.mRxFilterLength = rxFilterLength;
            return this;
        }

        public SubscribeData build() {
            return new SubscribeData(this.mServiceName, this.mServiceSpecificInfo, this.mServiceSpecificInfoLength, this.mTxFilter, this.mTxFilterLength, this.mRxFilter, this.mRxFilterLength, null);
        }
    }
}
