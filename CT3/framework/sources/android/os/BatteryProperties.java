package android.os;

import android.os.Parcelable;

public class BatteryProperties implements Parcelable {
    public static final Parcelable.Creator<BatteryProperties> CREATOR = new Parcelable.Creator<BatteryProperties>() {
        @Override
        public BatteryProperties createFromParcel(Parcel p) {
            return new BatteryProperties(p, null);
        }

        @Override
        public BatteryProperties[] newArray(int size) {
            return new BatteryProperties[size];
        }
    };
    public int adjustPower;
    public int batteryChargeCounter;
    public int batteryCurrentNow;
    public int batteryHealth;
    public int batteryLevel;
    public int batteryLevel_smb;
    public boolean batteryPresent;
    public boolean batteryPresent_smb;
    public int batteryStatus;
    public int batteryStatus_smb;
    public String batteryTechnology;
    public int batteryTemperature;
    public int batteryVoltage;
    public boolean chargerAcOnline;
    public boolean chargerUsbOnline;
    public boolean chargerWirelessOnline;
    public int maxChargingCurrent;
    public int maxChargingVoltage;

    BatteryProperties(Parcel p, BatteryProperties batteryProperties) {
        this(p);
    }

    public BatteryProperties() {
    }

    public void set(BatteryProperties other) {
        this.chargerAcOnline = other.chargerAcOnline;
        this.chargerUsbOnline = other.chargerUsbOnline;
        this.chargerWirelessOnline = other.chargerWirelessOnline;
        this.maxChargingCurrent = other.maxChargingCurrent;
        this.maxChargingVoltage = other.maxChargingVoltage;
        this.batteryStatus = other.batteryStatus;
        this.batteryHealth = other.batteryHealth;
        this.batteryPresent = other.batteryPresent;
        this.batteryLevel = other.batteryLevel;
        this.batteryVoltage = other.batteryVoltage;
        this.batteryTemperature = other.batteryTemperature;
        this.batteryStatus_smb = other.batteryStatus_smb;
        this.batteryPresent_smb = other.batteryPresent_smb;
        this.batteryLevel_smb = other.batteryLevel_smb;
        this.batteryCurrentNow = other.batteryCurrentNow;
        this.batteryChargeCounter = other.batteryChargeCounter;
        this.adjustPower = other.adjustPower;
        this.batteryTechnology = other.batteryTechnology;
    }

    private BatteryProperties(Parcel p) {
        this.chargerAcOnline = p.readInt() == 1;
        this.chargerUsbOnline = p.readInt() == 1;
        this.chargerWirelessOnline = p.readInt() == 1;
        this.maxChargingCurrent = p.readInt();
        this.maxChargingVoltage = p.readInt();
        this.batteryStatus = p.readInt();
        this.batteryHealth = p.readInt();
        this.batteryPresent = p.readInt() == 1;
        this.batteryLevel = p.readInt();
        this.batteryVoltage = p.readInt();
        this.batteryTemperature = p.readInt();
        this.batteryStatus_smb = p.readInt();
        this.batteryPresent_smb = p.readInt() == 1;
        this.batteryLevel_smb = p.readInt();
        this.batteryCurrentNow = p.readInt();
        this.batteryChargeCounter = p.readInt();
        this.adjustPower = p.readInt();
        this.batteryTechnology = p.readString();
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        p.writeInt(this.chargerAcOnline ? 1 : 0);
        p.writeInt(this.chargerUsbOnline ? 1 : 0);
        p.writeInt(this.chargerWirelessOnline ? 1 : 0);
        p.writeInt(this.maxChargingCurrent);
        p.writeInt(this.maxChargingVoltage);
        p.writeInt(this.batteryStatus);
        p.writeInt(this.batteryHealth);
        p.writeInt(this.batteryPresent ? 1 : 0);
        p.writeInt(this.batteryLevel);
        p.writeInt(this.batteryVoltage);
        p.writeInt(this.batteryTemperature);
        p.writeInt(this.batteryStatus_smb);
        p.writeInt(this.batteryPresent_smb ? 1 : 0);
        p.writeInt(this.batteryLevel_smb);
        p.writeInt(this.batteryCurrentNow);
        p.writeInt(this.batteryChargeCounter);
        p.writeInt(this.adjustPower);
        p.writeString(this.batteryTechnology);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
