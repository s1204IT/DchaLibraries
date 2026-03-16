package android.os;

import android.os.Parcelable;

public class BatteryProperties implements Parcelable {
    public static final Parcelable.Creator<BatteryProperties> CREATOR = new Parcelable.Creator<BatteryProperties>() {
        @Override
        public BatteryProperties createFromParcel(Parcel p) {
            return new BatteryProperties(p);
        }

        @Override
        public BatteryProperties[] newArray(int size) {
            return new BatteryProperties[size];
        }
    };
    public int batteryHealth;
    public int batteryLevel;
    public boolean batteryPresent;
    public int batteryStatus;
    public String batteryTechnology;
    public int batteryTemperature;
    public int batteryVoltage;
    public boolean chargerAcOnline;
    public boolean chargerUsbOnline;
    public boolean chargerWirelessOnline;

    public BatteryProperties() {
    }

    public void set(BatteryProperties other) {
        this.chargerAcOnline = other.chargerAcOnline;
        this.chargerUsbOnline = other.chargerUsbOnline;
        this.chargerWirelessOnline = other.chargerWirelessOnline;
        this.batteryStatus = other.batteryStatus;
        this.batteryHealth = other.batteryHealth;
        this.batteryPresent = other.batteryPresent;
        this.batteryLevel = other.batteryLevel;
        this.batteryVoltage = other.batteryVoltage;
        this.batteryTemperature = other.batteryTemperature;
        this.batteryTechnology = other.batteryTechnology;
    }

    private BatteryProperties(Parcel p) {
        this.chargerAcOnline = p.readInt() == 1;
        this.chargerUsbOnline = p.readInt() == 1;
        this.chargerWirelessOnline = p.readInt() == 1;
        this.batteryStatus = p.readInt();
        this.batteryHealth = p.readInt();
        this.batteryPresent = p.readInt() == 1;
        this.batteryLevel = p.readInt();
        this.batteryVoltage = p.readInt();
        this.batteryTemperature = p.readInt();
        this.batteryTechnology = p.readString();
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        p.writeInt(this.chargerAcOnline ? 1 : 0);
        p.writeInt(this.chargerUsbOnline ? 1 : 0);
        p.writeInt(this.chargerWirelessOnline ? 1 : 0);
        p.writeInt(this.batteryStatus);
        p.writeInt(this.batteryHealth);
        p.writeInt(this.batteryPresent ? 1 : 0);
        p.writeInt(this.batteryLevel);
        p.writeInt(this.batteryVoltage);
        p.writeInt(this.batteryTemperature);
        p.writeString(this.batteryTechnology);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
