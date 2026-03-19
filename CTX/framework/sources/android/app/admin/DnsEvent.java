package android.app.admin;

import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;
import android.security.keystore.KeyProperties;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DnsEvent extends NetworkEvent implements Parcelable {
    public static final Parcelable.Creator<DnsEvent> CREATOR = new Parcelable.Creator<DnsEvent>() {
        @Override
        public DnsEvent createFromParcel(Parcel parcel) {
            if (parcel.readInt() != 1) {
                return null;
            }
            return new DnsEvent(parcel);
        }

        @Override
        public DnsEvent[] newArray(int i) {
            return new DnsEvent[i];
        }
    };
    private final String mHostname;
    private final String[] mIpAddresses;
    private final int mIpAddressesCount;

    public DnsEvent(String str, String[] strArr, int i, String str2, long j) {
        super(str2, j);
        this.mHostname = str;
        this.mIpAddresses = strArr;
        this.mIpAddressesCount = i;
    }

    private DnsEvent(Parcel parcel) {
        this.mHostname = parcel.readString();
        this.mIpAddresses = parcel.createStringArray();
        this.mIpAddressesCount = parcel.readInt();
        this.mPackageName = parcel.readString();
        this.mTimestamp = parcel.readLong();
        this.mId = parcel.readLong();
    }

    public String getHostname() {
        return this.mHostname;
    }

    public List<InetAddress> getInetAddresses() {
        if (this.mIpAddresses == null || this.mIpAddresses.length == 0) {
            return Collections.emptyList();
        }
        ArrayList arrayList = new ArrayList(this.mIpAddresses.length);
        for (String str : this.mIpAddresses) {
            try {
                arrayList.add(InetAddress.getByName(str));
            } catch (UnknownHostException e) {
            }
        }
        return arrayList;
    }

    public int getTotalResolvedAddressCount() {
        return this.mIpAddressesCount;
    }

    public String toString() {
        Object[] objArr = new Object[6];
        objArr[0] = Long.valueOf(this.mId);
        objArr[1] = this.mHostname;
        objArr[2] = this.mIpAddresses == null ? KeyProperties.DIGEST_NONE : String.join(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER, this.mIpAddresses);
        objArr[3] = Integer.valueOf(this.mIpAddressesCount);
        objArr[4] = Long.valueOf(this.mTimestamp);
        objArr[5] = this.mPackageName;
        return String.format("DnsEvent(%d, %s, %s, %d, %d, %s)", objArr);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(1);
        parcel.writeString(this.mHostname);
        parcel.writeStringArray(this.mIpAddresses);
        parcel.writeInt(this.mIpAddressesCount);
        parcel.writeString(this.mPackageName);
        parcel.writeLong(this.mTimestamp);
        parcel.writeLong(this.mId);
    }
}
