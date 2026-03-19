package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.collect.Sets;
import java.util.HashSet;
import java.util.Iterator;

public class InterfaceConfiguration implements Parcelable {
    public static final Parcelable.Creator<InterfaceConfiguration> CREATOR = new Parcelable.Creator<InterfaceConfiguration>() {
        @Override
        public InterfaceConfiguration createFromParcel(Parcel parcel) {
            InterfaceConfiguration interfaceConfiguration = new InterfaceConfiguration();
            interfaceConfiguration.mHwAddr = parcel.readString();
            if (parcel.readByte() == 1) {
                interfaceConfiguration.mAddr = (LinkAddress) parcel.readParcelable(null);
            }
            int i = parcel.readInt();
            for (int i2 = 0; i2 < i; i2++) {
                interfaceConfiguration.mFlags.add(parcel.readString());
            }
            return interfaceConfiguration;
        }

        @Override
        public InterfaceConfiguration[] newArray(int i) {
            return new InterfaceConfiguration[i];
        }
    };
    private static final String FLAG_DOWN = "down";
    private static final String FLAG_UP = "up";
    private LinkAddress mAddr;
    private HashSet<String> mFlags = Sets.newHashSet();
    private String mHwAddr;

    public String toString() {
        return "mHwAddr=" + this.mHwAddr + " mAddr=" + String.valueOf(this.mAddr) + " mFlags=" + getFlags();
    }

    public Iterable<String> getFlags() {
        return this.mFlags;
    }

    public boolean hasFlag(String str) {
        validateFlag(str);
        return this.mFlags.contains(str);
    }

    public void clearFlag(String str) {
        validateFlag(str);
        this.mFlags.remove(str);
    }

    public void setFlag(String str) {
        validateFlag(str);
        this.mFlags.add(str);
    }

    public void setInterfaceUp() {
        this.mFlags.remove(FLAG_DOWN);
        this.mFlags.add(FLAG_UP);
    }

    public void setInterfaceDown() {
        this.mFlags.remove(FLAG_UP);
        this.mFlags.add(FLAG_DOWN);
    }

    public void ignoreInterfaceUpDownStatus() {
        this.mFlags.remove(FLAG_UP);
        this.mFlags.remove(FLAG_DOWN);
    }

    public LinkAddress getLinkAddress() {
        return this.mAddr;
    }

    public void setLinkAddress(LinkAddress linkAddress) {
        this.mAddr = linkAddress;
    }

    public String getHardwareAddress() {
        return this.mHwAddr;
    }

    public void setHardwareAddress(String str) {
        this.mHwAddr = str;
    }

    public boolean isActive() {
        try {
            if (isUp()) {
                for (byte b : this.mAddr.getAddress().getAddress()) {
                    if (b != 0) {
                        return true;
                    }
                }
            }
            return false;
        } catch (NullPointerException e) {
            return false;
        }
    }

    public boolean isUp() {
        return hasFlag(FLAG_UP);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mHwAddr);
        if (this.mAddr != null) {
            parcel.writeByte((byte) 1);
            parcel.writeParcelable(this.mAddr, i);
        } else {
            parcel.writeByte((byte) 0);
        }
        parcel.writeInt(this.mFlags.size());
        Iterator<String> it = this.mFlags.iterator();
        while (it.hasNext()) {
            parcel.writeString(it.next());
        }
    }

    private static void validateFlag(String str) {
        if (str.indexOf(32) >= 0) {
            throw new IllegalArgumentException("flag contains space: " + str);
        }
    }
}
