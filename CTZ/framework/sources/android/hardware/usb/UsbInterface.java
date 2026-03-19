package android.hardware.usb;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.Preconditions;

public class UsbInterface implements Parcelable {
    public static final Parcelable.Creator<UsbInterface> CREATOR = new Parcelable.Creator<UsbInterface>() {
        @Override
        public UsbInterface createFromParcel(Parcel parcel) {
            int i = parcel.readInt();
            int i2 = parcel.readInt();
            String string = parcel.readString();
            int i3 = parcel.readInt();
            int i4 = parcel.readInt();
            int i5 = parcel.readInt();
            Parcelable[] parcelableArray = parcel.readParcelableArray(UsbEndpoint.class.getClassLoader());
            UsbInterface usbInterface = new UsbInterface(i, i2, string, i3, i4, i5);
            usbInterface.setEndpoints(parcelableArray);
            return usbInterface;
        }

        @Override
        public UsbInterface[] newArray(int i) {
            return new UsbInterface[i];
        }
    };
    private final int mAlternateSetting;
    private final int mClass;
    private Parcelable[] mEndpoints;
    private final int mId;
    private final String mName;
    private final int mProtocol;
    private final int mSubclass;

    public UsbInterface(int i, int i2, String str, int i3, int i4, int i5) {
        this.mId = i;
        this.mAlternateSetting = i2;
        this.mName = str;
        this.mClass = i3;
        this.mSubclass = i4;
        this.mProtocol = i5;
    }

    public int getId() {
        return this.mId;
    }

    public int getAlternateSetting() {
        return this.mAlternateSetting;
    }

    public String getName() {
        return this.mName;
    }

    public int getInterfaceClass() {
        return this.mClass;
    }

    public int getInterfaceSubclass() {
        return this.mSubclass;
    }

    public int getInterfaceProtocol() {
        return this.mProtocol;
    }

    public int getEndpointCount() {
        return this.mEndpoints.length;
    }

    public UsbEndpoint getEndpoint(int i) {
        return (UsbEndpoint) this.mEndpoints[i];
    }

    public void setEndpoints(Parcelable[] parcelableArr) {
        this.mEndpoints = (Parcelable[]) Preconditions.checkArrayElementsNotNull(parcelableArr, "endpoints");
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("UsbInterface[mId=" + this.mId + ",mAlternateSetting=" + this.mAlternateSetting + ",mName=" + this.mName + ",mClass=" + this.mClass + ",mSubclass=" + this.mSubclass + ",mProtocol=" + this.mProtocol + ",mEndpoints=[");
        for (int i = 0; i < this.mEndpoints.length; i++) {
            sb.append("\n");
            sb.append(this.mEndpoints[i].toString());
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
        parcel.writeInt(this.mId);
        parcel.writeInt(this.mAlternateSetting);
        parcel.writeString(this.mName);
        parcel.writeInt(this.mClass);
        parcel.writeInt(this.mSubclass);
        parcel.writeInt(this.mProtocol);
        parcel.writeParcelableArray(this.mEndpoints, 0);
    }
}
