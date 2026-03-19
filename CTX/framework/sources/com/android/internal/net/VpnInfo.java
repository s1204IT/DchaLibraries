package com.android.internal.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateFormat;

public class VpnInfo implements Parcelable {
    public static final Parcelable.Creator<VpnInfo> CREATOR = new Parcelable.Creator<VpnInfo>() {
        @Override
        public VpnInfo createFromParcel(Parcel parcel) {
            VpnInfo vpnInfo = new VpnInfo();
            vpnInfo.ownerUid = parcel.readInt();
            vpnInfo.vpnIface = parcel.readString();
            vpnInfo.primaryUnderlyingIface = parcel.readString();
            return vpnInfo;
        }

        @Override
        public VpnInfo[] newArray(int i) {
            return new VpnInfo[i];
        }
    };
    public int ownerUid;
    public String primaryUnderlyingIface;
    public String vpnIface;

    public String toString() {
        return "VpnInfo{ownerUid=" + this.ownerUid + ", vpnIface='" + this.vpnIface + DateFormat.QUOTE + ", primaryUnderlyingIface='" + this.primaryUnderlyingIface + DateFormat.QUOTE + '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.ownerUid);
        parcel.writeString(this.vpnIface);
        parcel.writeString(this.primaryUnderlyingIface);
    }
}
