package android.service.euicc;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.euicc.DownloadableSubscription;
import java.util.Arrays;
import java.util.List;

@SystemApi
public final class GetDefaultDownloadableSubscriptionListResult implements Parcelable {
    public static final Parcelable.Creator<GetDefaultDownloadableSubscriptionListResult> CREATOR = new Parcelable.Creator<GetDefaultDownloadableSubscriptionListResult>() {
        @Override
        public GetDefaultDownloadableSubscriptionListResult createFromParcel(Parcel parcel) {
            return new GetDefaultDownloadableSubscriptionListResult(parcel);
        }

        @Override
        public GetDefaultDownloadableSubscriptionListResult[] newArray(int i) {
            return new GetDefaultDownloadableSubscriptionListResult[i];
        }
    };
    private final DownloadableSubscription[] mSubscriptions;

    @Deprecated
    public final int result;

    public int getResult() {
        return this.result;
    }

    public List<DownloadableSubscription> getDownloadableSubscriptions() {
        if (this.mSubscriptions == null) {
            return null;
        }
        return Arrays.asList(this.mSubscriptions);
    }

    public GetDefaultDownloadableSubscriptionListResult(int i, DownloadableSubscription[] downloadableSubscriptionArr) {
        this.result = i;
        if (this.result == 0) {
            this.mSubscriptions = downloadableSubscriptionArr;
        } else {
            if (downloadableSubscriptionArr != null) {
                throw new IllegalArgumentException("Error result with non-null subscriptions: " + i);
            }
            this.mSubscriptions = null;
        }
    }

    private GetDefaultDownloadableSubscriptionListResult(Parcel parcel) {
        this.result = parcel.readInt();
        this.mSubscriptions = (DownloadableSubscription[]) parcel.createTypedArray(DownloadableSubscription.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.result);
        parcel.writeTypedArray(this.mSubscriptions, i);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
