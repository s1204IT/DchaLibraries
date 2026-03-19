package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;

public class WifiWakeReasonAndCounts implements Parcelable {
    public static final Parcelable.Creator<WifiWakeReasonAndCounts> CREATOR = new Parcelable.Creator<WifiWakeReasonAndCounts>() {
        @Override
        public WifiWakeReasonAndCounts createFromParcel(Parcel parcel) {
            WifiWakeReasonAndCounts wifiWakeReasonAndCounts = new WifiWakeReasonAndCounts();
            wifiWakeReasonAndCounts.totalCmdEventWake = parcel.readInt();
            wifiWakeReasonAndCounts.totalDriverFwLocalWake = parcel.readInt();
            wifiWakeReasonAndCounts.totalRxDataWake = parcel.readInt();
            wifiWakeReasonAndCounts.rxUnicast = parcel.readInt();
            wifiWakeReasonAndCounts.rxMulticast = parcel.readInt();
            wifiWakeReasonAndCounts.rxBroadcast = parcel.readInt();
            wifiWakeReasonAndCounts.icmp = parcel.readInt();
            wifiWakeReasonAndCounts.icmp6 = parcel.readInt();
            wifiWakeReasonAndCounts.icmp6Ra = parcel.readInt();
            wifiWakeReasonAndCounts.icmp6Na = parcel.readInt();
            wifiWakeReasonAndCounts.icmp6Ns = parcel.readInt();
            wifiWakeReasonAndCounts.ipv4RxMulticast = parcel.readInt();
            wifiWakeReasonAndCounts.ipv6Multicast = parcel.readInt();
            wifiWakeReasonAndCounts.otherRxMulticast = parcel.readInt();
            parcel.readIntArray(wifiWakeReasonAndCounts.cmdEventWakeCntArray);
            parcel.readIntArray(wifiWakeReasonAndCounts.driverFWLocalWakeCntArray);
            return wifiWakeReasonAndCounts;
        }

        @Override
        public WifiWakeReasonAndCounts[] newArray(int i) {
            return new WifiWakeReasonAndCounts[i];
        }
    };
    private static final String TAG = "WifiWakeReasonAndCounts";
    public int[] cmdEventWakeCntArray;
    public int[] driverFWLocalWakeCntArray;
    public int icmp;
    public int icmp6;
    public int icmp6Na;
    public int icmp6Ns;
    public int icmp6Ra;
    public int ipv4RxMulticast;
    public int ipv6Multicast;
    public int otherRxMulticast;
    public int rxBroadcast;
    public int rxMulticast;
    public int rxUnicast;
    public int totalCmdEventWake;
    public int totalDriverFwLocalWake;
    public int totalRxDataWake;

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(" totalCmdEventWake ");
        stringBuffer.append(this.totalCmdEventWake);
        stringBuffer.append(" totalDriverFwLocalWake ");
        stringBuffer.append(this.totalDriverFwLocalWake);
        stringBuffer.append(" totalRxDataWake ");
        stringBuffer.append(this.totalRxDataWake);
        stringBuffer.append(" rxUnicast ");
        stringBuffer.append(this.rxUnicast);
        stringBuffer.append(" rxMulticast ");
        stringBuffer.append(this.rxMulticast);
        stringBuffer.append(" rxBroadcast ");
        stringBuffer.append(this.rxBroadcast);
        stringBuffer.append(" icmp ");
        stringBuffer.append(this.icmp);
        stringBuffer.append(" icmp6 ");
        stringBuffer.append(this.icmp6);
        stringBuffer.append(" icmp6Ra ");
        stringBuffer.append(this.icmp6Ra);
        stringBuffer.append(" icmp6Na ");
        stringBuffer.append(this.icmp6Na);
        stringBuffer.append(" icmp6Ns ");
        stringBuffer.append(this.icmp6Ns);
        stringBuffer.append(" ipv4RxMulticast ");
        stringBuffer.append(this.ipv4RxMulticast);
        stringBuffer.append(" ipv6Multicast ");
        stringBuffer.append(this.ipv6Multicast);
        stringBuffer.append(" otherRxMulticast ");
        stringBuffer.append(this.otherRxMulticast);
        for (int i = 0; i < this.cmdEventWakeCntArray.length; i++) {
            stringBuffer.append(" cmdEventWakeCntArray[" + i + "] " + this.cmdEventWakeCntArray[i]);
        }
        for (int i2 = 0; i2 < this.driverFWLocalWakeCntArray.length; i2++) {
            stringBuffer.append(" driverFWLocalWakeCntArray[" + i2 + "] " + this.driverFWLocalWakeCntArray[i2]);
        }
        return stringBuffer.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.totalCmdEventWake);
        parcel.writeInt(this.totalDriverFwLocalWake);
        parcel.writeInt(this.totalRxDataWake);
        parcel.writeInt(this.rxUnicast);
        parcel.writeInt(this.rxMulticast);
        parcel.writeInt(this.rxBroadcast);
        parcel.writeInt(this.icmp);
        parcel.writeInt(this.icmp6);
        parcel.writeInt(this.icmp6Ra);
        parcel.writeInt(this.icmp6Na);
        parcel.writeInt(this.icmp6Ns);
        parcel.writeInt(this.ipv4RxMulticast);
        parcel.writeInt(this.ipv6Multicast);
        parcel.writeInt(this.otherRxMulticast);
        parcel.writeIntArray(this.cmdEventWakeCntArray);
        parcel.writeIntArray(this.driverFWLocalWakeCntArray);
    }
}
