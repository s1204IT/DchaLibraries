package android.net.wifi.p2p.nsd;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Locale;

public class WifiP2pServiceRequest implements Parcelable {
    public static final Parcelable.Creator<WifiP2pServiceRequest> CREATOR = new Parcelable.Creator<WifiP2pServiceRequest>() {
        @Override
        public WifiP2pServiceRequest createFromParcel(Parcel in) {
            int servType = in.readInt();
            int length = in.readInt();
            int transId = in.readInt();
            String query = in.readString();
            return new WifiP2pServiceRequest(servType, length, transId, query, null);
        }

        @Override
        public WifiP2pServiceRequest[] newArray(int size) {
            return new WifiP2pServiceRequest[size];
        }
    };
    private int mLength;
    private int mProtocolType;
    private String mQuery;
    private int mTransId;

    WifiP2pServiceRequest(int serviceType, int length, int transId, String query, WifiP2pServiceRequest wifiP2pServiceRequest) {
        this(serviceType, length, transId, query);
    }

    protected WifiP2pServiceRequest(int protocolType, String query) {
        validateQuery(query);
        this.mProtocolType = protocolType;
        this.mQuery = query;
        if (query != null) {
            this.mLength = (query.length() / 2) + 2;
        } else {
            this.mLength = 2;
        }
    }

    private WifiP2pServiceRequest(int serviceType, int length, int transId, String query) {
        this.mProtocolType = serviceType;
        this.mLength = length;
        this.mTransId = transId;
        this.mQuery = query;
    }

    public int getTransactionId() {
        return this.mTransId;
    }

    public void setTransactionId(int id) {
        this.mTransId = id;
    }

    public String getSupplicantQuery() {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format(Locale.US, "%02x", Integer.valueOf(this.mLength & 255)));
        sb.append(String.format(Locale.US, "%02x", Integer.valueOf((this.mLength >> 8) & 255)));
        sb.append(String.format(Locale.US, "%02x", Integer.valueOf(this.mProtocolType)));
        sb.append(String.format(Locale.US, "%02x", Integer.valueOf(this.mTransId)));
        if (this.mQuery != null) {
            sb.append(this.mQuery);
        }
        return sb.toString();
    }

    private void validateQuery(String query) {
        if (query == null) {
            return;
        }
        if (query.length() % 2 == 1) {
            throw new IllegalArgumentException("query size is invalid. query=" + query);
        }
        if (query.length() / 2 > 65535) {
            throw new IllegalArgumentException("query size is too large. len=" + query.length());
        }
        String query2 = query.toLowerCase(Locale.ROOT);
        char[] chars = query2.toCharArray();
        for (char c : chars) {
            if ((c < '0' || c > '9') && (c < 'a' || c > 'f')) {
                throw new IllegalArgumentException("query should be hex string. query=" + query2);
            }
        }
    }

    public static WifiP2pServiceRequest newInstance(int protocolType, String queryData) {
        return new WifiP2pServiceRequest(protocolType, queryData);
    }

    public static WifiP2pServiceRequest newInstance(int protocolType) {
        return new WifiP2pServiceRequest(protocolType, null);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof WifiP2pServiceRequest) || obj.mProtocolType != this.mProtocolType || obj.mLength != this.mLength) {
            return false;
        }
        if (obj.mQuery == null && this.mQuery == null) {
            return true;
        }
        if (obj.mQuery != null) {
            return obj.mQuery.equals(this.mQuery);
        }
        return false;
    }

    public int hashCode() {
        int result = this.mProtocolType + 527;
        return (((result * 31) + this.mLength) * 31) + (this.mQuery == null ? 0 : this.mQuery.hashCode());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mProtocolType);
        dest.writeInt(this.mLength);
        dest.writeInt(this.mTransId);
        dest.writeString(this.mQuery);
    }
}
