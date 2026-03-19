package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;

public class PasspointManagementObjectDefinition implements Parcelable {
    public static final Parcelable.Creator<PasspointManagementObjectDefinition> CREATOR = new Parcelable.Creator<PasspointManagementObjectDefinition>() {
        @Override
        public PasspointManagementObjectDefinition createFromParcel(Parcel parcel) {
            return new PasspointManagementObjectDefinition(parcel.readString(), parcel.readString(), parcel.readString());
        }

        @Override
        public PasspointManagementObjectDefinition[] newArray(int i) {
            return new PasspointManagementObjectDefinition[i];
        }
    };
    private final String mBaseUri;
    private final String mMoTree;
    private final String mUrn;

    public PasspointManagementObjectDefinition(String str, String str2, String str3) {
        this.mBaseUri = str;
        this.mUrn = str2;
        this.mMoTree = str3;
    }

    public String getBaseUri() {
        return this.mBaseUri;
    }

    public String getUrn() {
        return this.mUrn;
    }

    public String getMoTree() {
        return this.mMoTree;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mBaseUri);
        parcel.writeString(this.mUrn);
        parcel.writeString(this.mMoTree);
    }
}
