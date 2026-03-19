package android.service.euicc;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.euicc.DownloadableSubscription;

@SystemApi
public final class GetDownloadableSubscriptionMetadataResult implements Parcelable {
    public static final Parcelable.Creator<GetDownloadableSubscriptionMetadataResult> CREATOR = new Parcelable.Creator<GetDownloadableSubscriptionMetadataResult>() {
        @Override
        public GetDownloadableSubscriptionMetadataResult createFromParcel(Parcel parcel) {
            return new GetDownloadableSubscriptionMetadataResult(parcel);
        }

        @Override
        public GetDownloadableSubscriptionMetadataResult[] newArray(int i) {
            return new GetDownloadableSubscriptionMetadataResult[i];
        }
    };
    private final DownloadableSubscription mSubscription;

    @Deprecated
    public final int result;

    public int getResult() {
        return this.result;
    }

    public DownloadableSubscription getDownloadableSubscription() {
        return this.mSubscription;
    }

    public GetDownloadableSubscriptionMetadataResult(int i, DownloadableSubscription downloadableSubscription) {
        this.result = i;
        if (this.result == 0) {
            this.mSubscription = downloadableSubscription;
        } else {
            if (downloadableSubscription != null) {
                throw new IllegalArgumentException("Error result with non-null subscription: " + i);
            }
            this.mSubscription = null;
        }
    }

    private GetDownloadableSubscriptionMetadataResult(Parcel parcel) {
        this.result = parcel.readInt();
        this.mSubscription = (DownloadableSubscription) parcel.readTypedObject(DownloadableSubscription.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.result);
        parcel.writeTypedObject(this.mSubscription, i);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
