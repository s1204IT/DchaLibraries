package android.net.lowpan;

import android.net.lowpan.LowpanIdentity;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.HexDump;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.TreeSet;

public class LowpanBeaconInfo implements Parcelable {
    public static final Parcelable.Creator<LowpanBeaconInfo> CREATOR = new Parcelable.Creator<LowpanBeaconInfo>() {
        @Override
        public LowpanBeaconInfo createFromParcel(Parcel parcel) {
            Builder builder = new Builder();
            builder.setLowpanIdentity(LowpanIdentity.CREATOR.createFromParcel(parcel));
            builder.setRssi(parcel.readInt());
            builder.setLqi(parcel.readInt());
            builder.setBeaconAddress(parcel.createByteArray());
            for (int i = parcel.readInt(); i > 0; i--) {
                builder.setFlag(parcel.readInt());
            }
            return builder.build();
        }

        @Override
        public LowpanBeaconInfo[] newArray(int i) {
            return new LowpanBeaconInfo[i];
        }
    };
    public static final int FLAG_CAN_ASSIST = 1;
    public static final int UNKNOWN_LQI = 0;
    public static final int UNKNOWN_RSSI = Integer.MAX_VALUE;
    private byte[] mBeaconAddress;
    private final TreeSet<Integer> mFlags;
    private LowpanIdentity mIdentity;
    private int mLqi;
    private int mRssi;

    public static class Builder {
        final LowpanIdentity.Builder mIdentityBuilder = new LowpanIdentity.Builder();
        final LowpanBeaconInfo mBeaconInfo = new LowpanBeaconInfo();

        public Builder setLowpanIdentity(LowpanIdentity lowpanIdentity) {
            this.mIdentityBuilder.setLowpanIdentity(lowpanIdentity);
            return this;
        }

        public Builder setName(String str) {
            this.mIdentityBuilder.setName(str);
            return this;
        }

        public Builder setXpanid(byte[] bArr) {
            this.mIdentityBuilder.setXpanid(bArr);
            return this;
        }

        public Builder setPanid(int i) {
            this.mIdentityBuilder.setPanid(i);
            return this;
        }

        public Builder setChannel(int i) {
            this.mIdentityBuilder.setChannel(i);
            return this;
        }

        public Builder setType(String str) {
            this.mIdentityBuilder.setType(str);
            return this;
        }

        public Builder setRssi(int i) {
            this.mBeaconInfo.mRssi = i;
            return this;
        }

        public Builder setLqi(int i) {
            this.mBeaconInfo.mLqi = i;
            return this;
        }

        public Builder setBeaconAddress(byte[] bArr) {
            this.mBeaconInfo.mBeaconAddress = bArr != null ? (byte[]) bArr.clone() : null;
            return this;
        }

        public Builder setFlag(int i) {
            this.mBeaconInfo.mFlags.add(Integer.valueOf(i));
            return this;
        }

        public Builder setFlags(Collection<Integer> collection) {
            this.mBeaconInfo.mFlags.addAll(collection);
            return this;
        }

        public LowpanBeaconInfo build() {
            this.mBeaconInfo.mIdentity = this.mIdentityBuilder.build();
            if (this.mBeaconInfo.mBeaconAddress == null) {
                this.mBeaconInfo.mBeaconAddress = new byte[0];
            }
            return this.mBeaconInfo;
        }
    }

    private LowpanBeaconInfo() {
        this.mRssi = Integer.MAX_VALUE;
        this.mLqi = 0;
        this.mBeaconAddress = null;
        this.mFlags = new TreeSet<>();
    }

    public LowpanIdentity getLowpanIdentity() {
        return this.mIdentity;
    }

    public int getRssi() {
        return this.mRssi;
    }

    public int getLqi() {
        return this.mLqi;
    }

    public byte[] getBeaconAddress() {
        return (byte[]) this.mBeaconAddress.clone();
    }

    public Collection<Integer> getFlags() {
        return (Collection) this.mFlags.clone();
    }

    public boolean isFlagSet(int i) {
        return this.mFlags.contains(Integer.valueOf(i));
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(this.mIdentity.toString());
        if (this.mRssi != Integer.MAX_VALUE) {
            stringBuffer.append(", RSSI:");
            stringBuffer.append(this.mRssi);
            stringBuffer.append("dBm");
        }
        if (this.mLqi != 0) {
            stringBuffer.append(", LQI:");
            stringBuffer.append(this.mLqi);
        }
        if (this.mBeaconAddress.length > 0) {
            stringBuffer.append(", BeaconAddress:");
            stringBuffer.append(HexDump.toHexString(this.mBeaconAddress));
        }
        for (Integer num : this.mFlags) {
            if (num.intValue() == 1) {
                stringBuffer.append(", CAN_ASSIST");
            } else {
                stringBuffer.append(", FLAG_");
                stringBuffer.append(Integer.toHexString(num.intValue()));
            }
        }
        return stringBuffer.toString();
    }

    public int hashCode() {
        return Objects.hash(this.mIdentity, Integer.valueOf(this.mRssi), Integer.valueOf(this.mLqi), Integer.valueOf(Arrays.hashCode(this.mBeaconAddress)), this.mFlags);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof LowpanBeaconInfo)) {
            return false;
        }
        LowpanBeaconInfo lowpanBeaconInfo = (LowpanBeaconInfo) obj;
        return this.mIdentity.equals(lowpanBeaconInfo.mIdentity) && Arrays.equals(this.mBeaconAddress, lowpanBeaconInfo.mBeaconAddress) && this.mRssi == lowpanBeaconInfo.mRssi && this.mLqi == lowpanBeaconInfo.mLqi && this.mFlags.equals(lowpanBeaconInfo.mFlags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        this.mIdentity.writeToParcel(parcel, i);
        parcel.writeInt(this.mRssi);
        parcel.writeInt(this.mLqi);
        parcel.writeByteArray(this.mBeaconAddress);
        parcel.writeInt(this.mFlags.size());
        Iterator<Integer> it = this.mFlags.iterator();
        while (it.hasNext()) {
            parcel.writeInt(it.next().intValue());
        }
    }
}
