package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;

public final class NetworkScanRequest implements Parcelable {
    public static final Parcelable.Creator<NetworkScanRequest> CREATOR = new Parcelable.Creator<NetworkScanRequest>() {
        @Override
        public NetworkScanRequest createFromParcel(Parcel parcel) {
            return new NetworkScanRequest(parcel);
        }

        @Override
        public NetworkScanRequest[] newArray(int i) {
            return new NetworkScanRequest[i];
        }
    };
    public static final int MAX_BANDS = 8;
    public static final int MAX_CHANNELS = 32;
    public static final int MAX_INCREMENTAL_PERIODICITY_SEC = 10;
    public static final int MAX_MCC_MNC_LIST_SIZE = 20;
    public static final int MAX_RADIO_ACCESS_NETWORKS = 8;
    public static final int MAX_SEARCH_MAX_SEC = 3600;
    public static final int MAX_SEARCH_PERIODICITY_SEC = 300;
    public static final int MIN_INCREMENTAL_PERIODICITY_SEC = 1;
    public static final int MIN_SEARCH_MAX_SEC = 60;
    public static final int MIN_SEARCH_PERIODICITY_SEC = 5;
    public static final int SCAN_TYPE_ONE_SHOT = 0;
    public static final int SCAN_TYPE_PERIODIC = 1;
    private boolean mIncrementalResults;
    private int mIncrementalResultsPeriodicity;
    private int mMaxSearchTime;
    private ArrayList<String> mMccMncs;
    private int mScanType;
    private int mSearchPeriodicity;
    private RadioAccessSpecifier[] mSpecifiers;

    @Retention(RetentionPolicy.SOURCE)
    public @interface ScanType {
    }

    public NetworkScanRequest(int i, RadioAccessSpecifier[] radioAccessSpecifierArr, int i2, int i3, boolean z, int i4, ArrayList<String> arrayList) {
        this.mScanType = i;
        if (radioAccessSpecifierArr != null) {
            this.mSpecifiers = (RadioAccessSpecifier[]) radioAccessSpecifierArr.clone();
        } else {
            this.mSpecifiers = null;
        }
        this.mSearchPeriodicity = i2;
        this.mMaxSearchTime = i3;
        this.mIncrementalResults = z;
        this.mIncrementalResultsPeriodicity = i4;
        if (arrayList != null) {
            this.mMccMncs = (ArrayList) arrayList.clone();
        } else {
            this.mMccMncs = new ArrayList<>();
        }
    }

    public int getScanType() {
        return this.mScanType;
    }

    public int getSearchPeriodicity() {
        return this.mSearchPeriodicity;
    }

    public int getMaxSearchTime() {
        return this.mMaxSearchTime;
    }

    public boolean getIncrementalResults() {
        return this.mIncrementalResults;
    }

    public int getIncrementalResultsPeriodicity() {
        return this.mIncrementalResultsPeriodicity;
    }

    public RadioAccessSpecifier[] getSpecifiers() {
        if (this.mSpecifiers == null) {
            return null;
        }
        return (RadioAccessSpecifier[]) this.mSpecifiers.clone();
    }

    public ArrayList<String> getPlmns() {
        return (ArrayList) this.mMccMncs.clone();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mScanType);
        parcel.writeParcelableArray(this.mSpecifiers, i);
        parcel.writeInt(this.mSearchPeriodicity);
        parcel.writeInt(this.mMaxSearchTime);
        parcel.writeBoolean(this.mIncrementalResults);
        parcel.writeInt(this.mIncrementalResultsPeriodicity);
        parcel.writeStringList(this.mMccMncs);
    }

    private NetworkScanRequest(Parcel parcel) {
        this.mScanType = parcel.readInt();
        this.mSpecifiers = (RadioAccessSpecifier[]) parcel.readParcelableArray(Object.class.getClassLoader(), RadioAccessSpecifier.class);
        this.mSearchPeriodicity = parcel.readInt();
        this.mMaxSearchTime = parcel.readInt();
        this.mIncrementalResults = parcel.readBoolean();
        this.mIncrementalResultsPeriodicity = parcel.readInt();
        this.mMccMncs = new ArrayList<>();
        parcel.readStringList(this.mMccMncs);
    }

    public boolean equals(Object obj) {
        try {
            NetworkScanRequest networkScanRequest = (NetworkScanRequest) obj;
            return obj != null && this.mScanType == networkScanRequest.mScanType && Arrays.equals(this.mSpecifiers, networkScanRequest.mSpecifiers) && this.mSearchPeriodicity == networkScanRequest.mSearchPeriodicity && this.mMaxSearchTime == networkScanRequest.mMaxSearchTime && this.mIncrementalResults == networkScanRequest.mIncrementalResults && this.mIncrementalResultsPeriodicity == networkScanRequest.mIncrementalResultsPeriodicity && this.mMccMncs != null && this.mMccMncs.equals(networkScanRequest.mMccMncs);
        } catch (ClassCastException e) {
            return false;
        }
    }

    public int hashCode() {
        return (this.mScanType * 31) + (Arrays.hashCode(this.mSpecifiers) * 37) + (this.mSearchPeriodicity * 41) + (this.mMaxSearchTime * 43) + ((!this.mIncrementalResults ? 0 : 1) * 47) + (this.mIncrementalResultsPeriodicity * 53) + (this.mMccMncs.hashCode() * 59);
    }
}
