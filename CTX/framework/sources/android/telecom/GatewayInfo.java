package android.telecom;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class GatewayInfo implements Parcelable {
    public static final Parcelable.Creator<GatewayInfo> CREATOR = new Parcelable.Creator<GatewayInfo>() {
        @Override
        public GatewayInfo createFromParcel(Parcel parcel) {
            return new GatewayInfo(parcel.readString(), Uri.CREATOR.createFromParcel(parcel), Uri.CREATOR.createFromParcel(parcel));
        }

        @Override
        public GatewayInfo[] newArray(int i) {
            return new GatewayInfo[i];
        }
    };
    private final Uri mGatewayAddress;
    private final String mGatewayProviderPackageName;
    private final Uri mOriginalAddress;

    public GatewayInfo(String str, Uri uri, Uri uri2) {
        this.mGatewayProviderPackageName = str;
        this.mGatewayAddress = uri;
        this.mOriginalAddress = uri2;
    }

    public String getGatewayProviderPackageName() {
        return this.mGatewayProviderPackageName;
    }

    public Uri getGatewayAddress() {
        return this.mGatewayAddress;
    }

    public Uri getOriginalAddress() {
        return this.mOriginalAddress;
    }

    public boolean isEmpty() {
        return TextUtils.isEmpty(this.mGatewayProviderPackageName) || this.mGatewayAddress == null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mGatewayProviderPackageName);
        this.mGatewayAddress.writeToParcel(parcel, 0);
        this.mOriginalAddress.writeToParcel(parcel, 0);
    }
}
