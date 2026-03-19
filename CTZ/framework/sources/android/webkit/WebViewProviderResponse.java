package android.webkit;

import android.content.pm.PackageInfo;
import android.os.Parcel;
import android.os.Parcelable;

public final class WebViewProviderResponse implements Parcelable {
    public static final Parcelable.Creator<WebViewProviderResponse> CREATOR = new Parcelable.Creator<WebViewProviderResponse>() {
        @Override
        public WebViewProviderResponse createFromParcel(Parcel parcel) {
            return new WebViewProviderResponse(parcel);
        }

        @Override
        public WebViewProviderResponse[] newArray(int i) {
            return new WebViewProviderResponse[i];
        }
    };
    public final PackageInfo packageInfo;
    public final int status;

    public WebViewProviderResponse(PackageInfo packageInfo, int i) {
        this.packageInfo = packageInfo;
        this.status = i;
    }

    private WebViewProviderResponse(Parcel parcel) {
        this.packageInfo = (PackageInfo) parcel.readTypedObject(PackageInfo.CREATOR);
        this.status = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedObject(this.packageInfo, i);
        parcel.writeInt(this.status);
    }
}
