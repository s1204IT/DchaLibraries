package android.telecom;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class GatewayInfo implements Parcelable {
    public static final Parcelable.Creator<GatewayInfo> CREATOR = new Parcelable.Creator<GatewayInfo>() {
        @Override
        public GatewayInfo createFromParcel(Parcel source) {
            String gatewayPackageName = source.readString();
            Uri gatewayUri = Uri.CREATOR.createFromParcel(source);
            Uri originalAddress = Uri.CREATOR.createFromParcel(source);
            return new GatewayInfo(gatewayPackageName, gatewayUri, originalAddress);
        }

        @Override
        public GatewayInfo[] newArray(int size) {
            return new GatewayInfo[size];
        }
    };
    private final Uri mGatewayAddress;
    private final String mGatewayProviderPackageName;
    private final Uri mOriginalAddress;

    public GatewayInfo(String packageName, Uri gatewayUri, Uri originalAddress) {
        this.mGatewayProviderPackageName = packageName;
        this.mGatewayAddress = gatewayUri;
        this.mOriginalAddress = originalAddress;
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
    public void writeToParcel(Parcel destination, int flags) {
        destination.writeString(this.mGatewayProviderPackageName);
        this.mGatewayAddress.writeToParcel(destination, 0);
        this.mOriginalAddress.writeToParcel(destination, 0);
    }
}
