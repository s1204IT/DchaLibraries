package android.webkit;

import android.annotation.SystemApi;
import android.content.pm.Signature;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

@SystemApi
public final class WebViewProviderInfo implements Parcelable {
    public static final Parcelable.Creator<WebViewProviderInfo> CREATOR = new Parcelable.Creator<WebViewProviderInfo>() {
        @Override
        public WebViewProviderInfo createFromParcel(Parcel parcel) {
            return new WebViewProviderInfo(parcel);
        }

        @Override
        public WebViewProviderInfo[] newArray(int i) {
            return new WebViewProviderInfo[i];
        }
    };
    public final boolean availableByDefault;
    public final String description;
    public final boolean isFallback;
    public final String packageName;
    public final Signature[] signatures;

    public WebViewProviderInfo(String str, String str2, boolean z, boolean z2, String[] strArr) {
        this.packageName = str;
        this.description = str2;
        this.availableByDefault = z;
        this.isFallback = z2;
        if (strArr == null) {
            this.signatures = new Signature[0];
            return;
        }
        this.signatures = new Signature[strArr.length];
        for (int i = 0; i < strArr.length; i++) {
            this.signatures[i] = new Signature(Base64.decode(strArr[i], 0));
        }
    }

    private WebViewProviderInfo(Parcel parcel) {
        this.packageName = parcel.readString();
        this.description = parcel.readString();
        this.availableByDefault = parcel.readInt() > 0;
        this.isFallback = parcel.readInt() > 0;
        this.signatures = (Signature[]) parcel.createTypedArray(Signature.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.packageName);
        parcel.writeString(this.description);
        parcel.writeInt(this.availableByDefault ? 1 : 0);
        parcel.writeInt(this.isFallback ? 1 : 0);
        parcel.writeTypedArray(this.signatures, 0);
    }
}
