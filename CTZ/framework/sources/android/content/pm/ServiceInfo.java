package android.content.pm;

import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Printer;

public class ServiceInfo extends ComponentInfo implements Parcelable {
    public static final Parcelable.Creator<ServiceInfo> CREATOR = new Parcelable.Creator<ServiceInfo>() {
        @Override
        public ServiceInfo createFromParcel(Parcel parcel) {
            return new ServiceInfo(parcel);
        }

        @Override
        public ServiceInfo[] newArray(int i) {
            return new ServiceInfo[i];
        }
    };
    public static final int FLAG_EXTERNAL_SERVICE = 4;
    public static final int FLAG_ISOLATED_PROCESS = 2;
    public static final int FLAG_SINGLE_USER = 1073741824;
    public static final int FLAG_STOP_WITH_TASK = 1;
    public static final int FLAG_VISIBLE_TO_INSTANT_APP = 1048576;
    public int flags;
    public String permission;

    public ServiceInfo() {
    }

    public ServiceInfo(ServiceInfo serviceInfo) {
        super(serviceInfo);
        this.permission = serviceInfo.permission;
        this.flags = serviceInfo.flags;
    }

    public void dump(Printer printer, String str) {
        dump(printer, str, 3);
    }

    void dump(Printer printer, String str, int i) {
        super.dumpFront(printer, str);
        printer.println(str + "permission=" + this.permission);
        printer.println(str + "flags=0x" + Integer.toHexString(this.flags));
        super.dumpBack(printer, str, i);
    }

    public String toString() {
        return "ServiceInfo{" + Integer.toHexString(System.identityHashCode(this)) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.name + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeString(this.permission);
        parcel.writeInt(this.flags);
    }

    private ServiceInfo(Parcel parcel) {
        super(parcel);
        this.permission = parcel.readString();
        this.flags = parcel.readInt();
    }
}
