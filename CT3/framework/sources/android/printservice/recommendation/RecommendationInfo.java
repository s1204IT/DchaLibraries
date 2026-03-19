package android.printservice.recommendation;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.Preconditions;

public final class RecommendationInfo implements Parcelable {
    public static final Parcelable.Creator<RecommendationInfo> CREATOR = new Parcelable.Creator<RecommendationInfo>() {
        @Override
        public RecommendationInfo createFromParcel(Parcel in) {
            return new RecommendationInfo(in, null);
        }

        @Override
        public RecommendationInfo[] newArray(int size) {
            return new RecommendationInfo[size];
        }
    };
    private final CharSequence mName;
    private final int mNumDiscoveredPrinters;
    private final CharSequence mPackageName;
    private final boolean mRecommendsMultiVendorService;

    RecommendationInfo(Parcel parcel, RecommendationInfo recommendationInfo) {
        this(parcel);
    }

    public RecommendationInfo(CharSequence packageName, CharSequence name, int numDiscoveredPrinters, boolean recommendsMultiVendorService) {
        this.mPackageName = Preconditions.checkStringNotEmpty(packageName);
        this.mName = Preconditions.checkStringNotEmpty(name);
        this.mNumDiscoveredPrinters = Preconditions.checkArgumentNonnegative(numDiscoveredPrinters);
        this.mRecommendsMultiVendorService = recommendsMultiVendorService;
    }

    private RecommendationInfo(Parcel parcel) {
        this(parcel.readCharSequence(), parcel.readCharSequence(), parcel.readInt(), parcel.readByte() != 0);
    }

    public CharSequence getPackageName() {
        return this.mPackageName;
    }

    public boolean recommendsMultiVendorService() {
        return this.mRecommendsMultiVendorService;
    }

    public int getNumDiscoveredPrinters() {
        return this.mNumDiscoveredPrinters;
    }

    public CharSequence getName() {
        return this.mName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeCharSequence(this.mPackageName);
        dest.writeCharSequence(this.mName);
        dest.writeInt(this.mNumDiscoveredPrinters);
        dest.writeByte((byte) (this.mRecommendsMultiVendorService ? 1 : 0));
    }
}
