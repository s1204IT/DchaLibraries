package android.telephony.data;

import android.net.LinkAddress;
import android.os.Parcel;
import android.os.Parcelable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DataCallResponse implements Parcelable {
    public static final Parcelable.Creator<DataCallResponse> CREATOR = new Parcelable.Creator<DataCallResponse>() {
        @Override
        public DataCallResponse createFromParcel(Parcel parcel) {
            return new DataCallResponse(parcel);
        }

        @Override
        public DataCallResponse[] newArray(int i) {
            return new DataCallResponse[i];
        }
    };
    private final int mActive;
    private final List<LinkAddress> mAddresses;
    private final int mCid;
    private final List<InetAddress> mDnses;
    private final List<InetAddress> mGateways;
    private final String mIfname;
    private final int mMtu;
    private final List<String> mPcscfs;
    private final int mStatus;
    private final int mSuggestedRetryTime;
    private final String mType;

    public DataCallResponse(int i, int i2, int i3, int i4, String str, String str2, List<LinkAddress> list, List<InetAddress> list2, List<InetAddress> list3, List<String> list4, int i5) {
        this.mStatus = i;
        this.mSuggestedRetryTime = i2;
        this.mCid = i3;
        this.mActive = i4;
        this.mType = str == null ? "" : str;
        this.mIfname = str2 == null ? "" : str2;
        this.mAddresses = list == null ? new ArrayList<>() : list;
        this.mDnses = list2 == null ? new ArrayList<>() : list2;
        this.mGateways = list3 == null ? new ArrayList<>() : list3;
        this.mPcscfs = list4 == null ? new ArrayList<>() : list4;
        this.mMtu = i5;
    }

    public DataCallResponse(Parcel parcel) {
        this.mStatus = parcel.readInt();
        this.mSuggestedRetryTime = parcel.readInt();
        this.mCid = parcel.readInt();
        this.mActive = parcel.readInt();
        this.mType = parcel.readString();
        this.mIfname = parcel.readString();
        this.mAddresses = new ArrayList();
        parcel.readList(this.mAddresses, LinkAddress.class.getClassLoader());
        this.mDnses = new ArrayList();
        parcel.readList(this.mDnses, InetAddress.class.getClassLoader());
        this.mGateways = new ArrayList();
        parcel.readList(this.mGateways, InetAddress.class.getClassLoader());
        this.mPcscfs = new ArrayList();
        parcel.readList(this.mPcscfs, InetAddress.class.getClassLoader());
        this.mMtu = parcel.readInt();
    }

    public int getStatus() {
        return this.mStatus;
    }

    public int getSuggestedRetryTime() {
        return this.mSuggestedRetryTime;
    }

    public int getCallId() {
        return this.mCid;
    }

    public int getActive() {
        return this.mActive;
    }

    public String getType() {
        return this.mType;
    }

    public String getIfname() {
        return this.mIfname;
    }

    public List<LinkAddress> getAddresses() {
        return this.mAddresses;
    }

    public List<InetAddress> getDnses() {
        return this.mDnses;
    }

    public List<InetAddress> getGateways() {
        return this.mGateways;
    }

    public List<String> getPcscfs() {
        return this.mPcscfs;
    }

    public int getMtu() {
        return this.mMtu;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("DataCallResponse: {");
        stringBuffer.append(" status=");
        stringBuffer.append(this.mStatus);
        stringBuffer.append(" retry=");
        stringBuffer.append(this.mSuggestedRetryTime);
        stringBuffer.append(" cid=");
        stringBuffer.append(this.mCid);
        stringBuffer.append(" active=");
        stringBuffer.append(this.mActive);
        stringBuffer.append(" type=");
        stringBuffer.append(this.mType);
        stringBuffer.append(" ifname=");
        stringBuffer.append(this.mIfname);
        stringBuffer.append(" addresses=");
        stringBuffer.append(this.mAddresses);
        stringBuffer.append(" dnses=");
        stringBuffer.append(this.mDnses);
        stringBuffer.append(" gateways=");
        stringBuffer.append(this.mGateways);
        stringBuffer.append(" pcscf=");
        stringBuffer.append(this.mPcscfs);
        stringBuffer.append(" mtu=");
        stringBuffer.append(this.mMtu);
        stringBuffer.append("}");
        return stringBuffer.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof DataCallResponse)) {
            return false;
        }
        DataCallResponse dataCallResponse = (DataCallResponse) obj;
        if (this.mStatus == dataCallResponse.mStatus && this.mSuggestedRetryTime == dataCallResponse.mSuggestedRetryTime && this.mCid == dataCallResponse.mCid && this.mActive == dataCallResponse.mActive && this.mType.equals(dataCallResponse.mType) && this.mIfname.equals(dataCallResponse.mIfname) && this.mAddresses.size() == dataCallResponse.mAddresses.size() && this.mAddresses.containsAll(dataCallResponse.mAddresses) && this.mDnses.size() == dataCallResponse.mDnses.size() && this.mDnses.containsAll(dataCallResponse.mDnses) && this.mGateways.size() == dataCallResponse.mGateways.size() && this.mGateways.containsAll(dataCallResponse.mGateways) && this.mPcscfs.size() == dataCallResponse.mPcscfs.size() && this.mPcscfs.containsAll(dataCallResponse.mPcscfs) && this.mMtu == dataCallResponse.mMtu) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mStatus), Integer.valueOf(this.mSuggestedRetryTime), Integer.valueOf(this.mCid), Integer.valueOf(this.mActive), this.mType, this.mIfname, this.mAddresses, this.mDnses, this.mGateways, this.mPcscfs, Integer.valueOf(this.mMtu));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mStatus);
        parcel.writeInt(this.mSuggestedRetryTime);
        parcel.writeInt(this.mCid);
        parcel.writeInt(this.mActive);
        parcel.writeString(this.mType);
        parcel.writeString(this.mIfname);
        parcel.writeList(this.mAddresses);
        parcel.writeList(this.mDnses);
        parcel.writeList(this.mGateways);
        parcel.writeList(this.mPcscfs);
        parcel.writeInt(this.mMtu);
    }
}
