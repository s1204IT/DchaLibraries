package android.net.wifi.p2p.nsd;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.telephony.IccCardConstants;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WifiP2pServiceResponse implements Parcelable {
    protected byte[] mData;
    protected WifiP2pDevice mDevice;
    protected int mServiceType;
    protected int mStatus;
    protected int mTransId;
    private static int MAX_BUF_SIZE = 1024;
    public static final Parcelable.Creator<WifiP2pServiceResponse> CREATOR = new Parcelable.Creator<WifiP2pServiceResponse>() {
        @Override
        public WifiP2pServiceResponse createFromParcel(Parcel parcel) {
            int i = parcel.readInt();
            int i2 = parcel.readInt();
            int i3 = parcel.readInt();
            byte[] bArr = null;
            WifiP2pDevice wifiP2pDevice = (WifiP2pDevice) parcel.readParcelable(null);
            int i4 = parcel.readInt();
            if (i4 > 0) {
                bArr = new byte[i4];
                parcel.readByteArray(bArr);
            }
            byte[] bArr2 = bArr;
            if (i == 1) {
                return WifiP2pDnsSdServiceResponse.newInstance(i2, i3, wifiP2pDevice, bArr2);
            }
            if (i == 2) {
                return WifiP2pUpnpServiceResponse.newInstance(i2, i3, wifiP2pDevice, bArr2);
            }
            return new WifiP2pServiceResponse(i, i2, i3, wifiP2pDevice, bArr2);
        }

        @Override
        public WifiP2pServiceResponse[] newArray(int i) {
            return new WifiP2pServiceResponse[i];
        }
    };

    public static class Status {
        public static final int BAD_REQUEST = 3;
        public static final int REQUESTED_INFORMATION_NOT_AVAILABLE = 2;
        public static final int SERVICE_PROTOCOL_NOT_AVAILABLE = 1;
        public static final int SUCCESS = 0;

        public static String toString(int i) {
            switch (i) {
                case 0:
                    return "SUCCESS";
                case 1:
                    return "SERVICE_PROTOCOL_NOT_AVAILABLE";
                case 2:
                    return "REQUESTED_INFORMATION_NOT_AVAILABLE";
                case 3:
                    return "BAD_REQUEST";
                default:
                    return IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
            }
        }

        private Status() {
        }
    }

    protected WifiP2pServiceResponse(int i, int i2, int i3, WifiP2pDevice wifiP2pDevice, byte[] bArr) {
        this.mServiceType = i;
        this.mStatus = i2;
        this.mTransId = i3;
        this.mDevice = wifiP2pDevice;
        this.mData = bArr;
    }

    public int getServiceType() {
        return this.mServiceType;
    }

    public int getStatus() {
        return this.mStatus;
    }

    public int getTransactionId() {
        return this.mTransId;
    }

    public byte[] getRawData() {
        return this.mData;
    }

    public WifiP2pDevice getSrcDevice() {
        return this.mDevice;
    }

    public void setSrcDevice(WifiP2pDevice wifiP2pDevice) {
        if (wifiP2pDevice == null) {
            return;
        }
        this.mDevice = wifiP2pDevice;
    }

    public static List<WifiP2pServiceResponse> newInstance(String str, byte[] bArr) {
        WifiP2pServiceResponse wifiP2pServiceResponse;
        ArrayList arrayList = new ArrayList();
        WifiP2pDevice wifiP2pDevice = new WifiP2pDevice();
        wifiP2pDevice.deviceAddress = str;
        if (bArr == null) {
            return null;
        }
        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bArr));
        while (dataInputStream.available() > 0) {
            try {
                int unsignedByte = (dataInputStream.readUnsignedByte() + (dataInputStream.readUnsignedByte() << 8)) - 3;
                int unsignedByte2 = dataInputStream.readUnsignedByte();
                int unsignedByte3 = dataInputStream.readUnsignedByte();
                int unsignedByte4 = dataInputStream.readUnsignedByte();
                if (unsignedByte < 0) {
                    return null;
                }
                if (unsignedByte == 0) {
                    if (unsignedByte4 == 0) {
                        arrayList.add(new WifiP2pServiceResponse(unsignedByte2, unsignedByte4, unsignedByte3, wifiP2pDevice, null));
                    }
                } else if (unsignedByte > MAX_BUF_SIZE) {
                    dataInputStream.skip(unsignedByte);
                } else {
                    byte[] bArr2 = new byte[unsignedByte];
                    dataInputStream.readFully(bArr2);
                    if (unsignedByte2 == 1) {
                        wifiP2pServiceResponse = WifiP2pDnsSdServiceResponse.newInstance(unsignedByte4, unsignedByte3, wifiP2pDevice, bArr2);
                    } else if (unsignedByte2 == 2) {
                        wifiP2pServiceResponse = WifiP2pUpnpServiceResponse.newInstance(unsignedByte4, unsignedByte3, wifiP2pDevice, bArr2);
                    } else {
                        wifiP2pServiceResponse = new WifiP2pServiceResponse(unsignedByte2, unsignedByte4, unsignedByte3, wifiP2pDevice, bArr2);
                    }
                    if (wifiP2pServiceResponse != null && wifiP2pServiceResponse.getStatus() == 0) {
                        arrayList.add(wifiP2pServiceResponse);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (arrayList.size() <= 0) {
                    return null;
                }
                return arrayList;
            }
        }
        return arrayList;
    }

    private static byte[] hexStr2Bin(String str) {
        int length = str.length() / 2;
        byte[] bArr = new byte[str.length() / 2];
        for (int i = 0; i < length; i++) {
            int i2 = i * 2;
            try {
                bArr[i] = (byte) Integer.parseInt(str.substring(i2, i2 + 2), 16);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return bArr;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("serviceType:");
        stringBuffer.append(this.mServiceType);
        stringBuffer.append(" status:");
        stringBuffer.append(Status.toString(this.mStatus));
        stringBuffer.append(" srcAddr:");
        stringBuffer.append(this.mDevice.deviceAddress);
        stringBuffer.append(" data:");
        stringBuffer.append(Arrays.toString(this.mData));
        return stringBuffer.toString();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof WifiP2pServiceResponse)) {
            return false;
        }
        WifiP2pServiceResponse wifiP2pServiceResponse = (WifiP2pServiceResponse) obj;
        return wifiP2pServiceResponse.mServiceType == this.mServiceType && wifiP2pServiceResponse.mStatus == this.mStatus && equals(wifiP2pServiceResponse.mDevice.deviceAddress, this.mDevice.deviceAddress) && Arrays.equals(wifiP2pServiceResponse.mData, this.mData);
    }

    private boolean equals(Object obj, Object obj2) {
        if (obj == null && obj2 == null) {
            return true;
        }
        if (obj != null) {
            return obj.equals(obj2);
        }
        return false;
    }

    public int hashCode() {
        return (31 * (((((((MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + this.mServiceType) * 31) + this.mStatus) * 31) + this.mTransId) * 31) + (this.mDevice.deviceAddress == null ? 0 : this.mDevice.deviceAddress.hashCode()))) + (this.mData != null ? Arrays.hashCode(this.mData) : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mServiceType);
        parcel.writeInt(this.mStatus);
        parcel.writeInt(this.mTransId);
        parcel.writeParcelable(this.mDevice, i);
        if (this.mData == null || this.mData.length == 0) {
            parcel.writeInt(0);
        } else {
            parcel.writeInt(this.mData.length);
            parcel.writeByteArray(this.mData);
        }
    }
}
