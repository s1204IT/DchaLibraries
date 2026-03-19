package android.hardware.usb;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.Preconditions;

public class UsbDevice implements Parcelable {
    public static final Parcelable.Creator<UsbDevice> CREATOR = new Parcelable.Creator<UsbDevice>() {
        @Override
        public UsbDevice createFromParcel(Parcel parcel) {
            String string = parcel.readString();
            int i = parcel.readInt();
            int i2 = parcel.readInt();
            int i3 = parcel.readInt();
            int i4 = parcel.readInt();
            int i5 = parcel.readInt();
            String string2 = parcel.readString();
            String string3 = parcel.readString();
            String string4 = parcel.readString();
            String string5 = parcel.readString();
            Parcelable[] parcelableArray = parcel.readParcelableArray(UsbInterface.class.getClassLoader());
            UsbDevice usbDevice = new UsbDevice(string, i, i2, i3, i4, i5, string2, string3, string4, string5);
            usbDevice.setConfigurations(parcelableArray);
            return usbDevice;
        }

        @Override
        public UsbDevice[] newArray(int i) {
            return new UsbDevice[i];
        }
    };
    private static final boolean DEBUG = false;
    private static final String TAG = "UsbDevice";
    private final int mClass;
    private Parcelable[] mConfigurations;
    private UsbInterface[] mInterfaces;
    private final String mManufacturerName;
    private final String mName;
    private final int mProductId;
    private final String mProductName;
    private final int mProtocol;
    private final String mSerialNumber;
    private final int mSubclass;
    private final int mVendorId;
    private final String mVersion;

    private static native int native_get_device_id(String str);

    private static native String native_get_device_name(int i);

    public UsbDevice(String str, int i, int i2, int i3, int i4, int i5, String str2, String str3, String str4, String str5) {
        this.mName = (String) Preconditions.checkNotNull(str);
        this.mVendorId = i;
        this.mProductId = i2;
        this.mClass = i3;
        this.mSubclass = i4;
        this.mProtocol = i5;
        this.mManufacturerName = str2;
        this.mProductName = str3;
        this.mVersion = (String) Preconditions.checkStringNotEmpty(str4);
        this.mSerialNumber = str5;
    }

    public String getDeviceName() {
        return this.mName;
    }

    public String getManufacturerName() {
        return this.mManufacturerName;
    }

    public String getProductName() {
        return this.mProductName;
    }

    public String getVersion() {
        return this.mVersion;
    }

    public String getSerialNumber() {
        return this.mSerialNumber;
    }

    public int getDeviceId() {
        return getDeviceId(this.mName);
    }

    public int getVendorId() {
        return this.mVendorId;
    }

    public int getProductId() {
        return this.mProductId;
    }

    public int getDeviceClass() {
        return this.mClass;
    }

    public int getDeviceSubclass() {
        return this.mSubclass;
    }

    public int getDeviceProtocol() {
        return this.mProtocol;
    }

    public int getConfigurationCount() {
        return this.mConfigurations.length;
    }

    public UsbConfiguration getConfiguration(int i) {
        return (UsbConfiguration) this.mConfigurations[i];
    }

    private UsbInterface[] getInterfaceList() {
        if (this.mInterfaces == null) {
            int length = this.mConfigurations.length;
            int interfaceCount = 0;
            for (int i = 0; i < length; i++) {
                interfaceCount += ((UsbConfiguration) this.mConfigurations[i]).getInterfaceCount();
            }
            this.mInterfaces = new UsbInterface[interfaceCount];
            int i2 = 0;
            int i3 = 0;
            while (i2 < length) {
                UsbConfiguration usbConfiguration = (UsbConfiguration) this.mConfigurations[i2];
                int interfaceCount2 = usbConfiguration.getInterfaceCount();
                int i4 = i3;
                int i5 = 0;
                while (i5 < interfaceCount2) {
                    this.mInterfaces[i4] = usbConfiguration.getInterface(i5);
                    i5++;
                    i4++;
                }
                i2++;
                i3 = i4;
            }
        }
        return this.mInterfaces;
    }

    public int getInterfaceCount() {
        return getInterfaceList().length;
    }

    public UsbInterface getInterface(int i) {
        return getInterfaceList()[i];
    }

    public void setConfigurations(Parcelable[] parcelableArr) {
        this.mConfigurations = (Parcelable[]) Preconditions.checkArrayElementsNotNull(parcelableArr, "configuration");
    }

    public boolean equals(Object obj) {
        if (obj instanceof UsbDevice) {
            return ((UsbDevice) obj).mName.equals(this.mName);
        }
        if (obj instanceof String) {
            return ((String) obj).equals(this.mName);
        }
        return false;
    }

    public int hashCode() {
        return this.mName.hashCode();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("UsbDevice[mName=" + this.mName + ",mVendorId=" + this.mVendorId + ",mProductId=" + this.mProductId + ",mClass=" + this.mClass + ",mSubclass=" + this.mSubclass + ",mProtocol=" + this.mProtocol + ",mManufacturerName=" + this.mManufacturerName + ",mProductName=" + this.mProductName + ",mVersion=" + this.mVersion + ",mSerialNumber=" + this.mSerialNumber + ",mConfigurations=[");
        for (int i = 0; i < this.mConfigurations.length; i++) {
            sb.append("\n");
            sb.append(this.mConfigurations[i].toString());
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mName);
        parcel.writeInt(this.mVendorId);
        parcel.writeInt(this.mProductId);
        parcel.writeInt(this.mClass);
        parcel.writeInt(this.mSubclass);
        parcel.writeInt(this.mProtocol);
        parcel.writeString(this.mManufacturerName);
        parcel.writeString(this.mProductName);
        parcel.writeString(this.mVersion);
        parcel.writeString(this.mSerialNumber);
        parcel.writeParcelableArray(this.mConfigurations, 0);
    }

    public static int getDeviceId(String str) {
        return native_get_device_id(str);
    }

    public static String getDeviceName(int i) {
        return native_get_device_name(i);
    }
}
