package android.os;

import android.os.Parcelable;

public class BatteryProperties implements Parcelable {
    public static final Parcelable.Creator<BatteryProperties> CREATOR = new Parcelable.Creator<BatteryProperties>() {
        @Override
        public BatteryProperties createFromParcel(Parcel parcel) {
            return new BatteryProperties(parcel);
        }

        @Override
        public BatteryProperties[] newArray(int i) {
            return new BatteryProperties[i];
        }
    };
    public int batteryChargeCounter;
    public int batteryFullCharge;
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
    public int maxChargingCurrent;
    public int maxChargingVoltage;

    public BatteryProperties() {
    }

    public void set(BatteryProperties batteryProperties) {
        this.chargerAcOnline = batteryProperties.chargerAcOnline;
        this.chargerUsbOnline = batteryProperties.chargerUsbOnline;
        this.chargerWirelessOnline = batteryProperties.chargerWirelessOnline;
        this.maxChargingCurrent = batteryProperties.maxChargingCurrent;
        this.maxChargingVoltage = batteryProperties.maxChargingVoltage;
        this.batteryStatus = batteryProperties.batteryStatus;
        this.batteryHealth = batteryProperties.batteryHealth;
        this.batteryPresent = batteryProperties.batteryPresent;
        this.batteryLevel = batteryProperties.batteryLevel;
        this.batteryVoltage = batteryProperties.batteryVoltage;
        this.batteryTemperature = batteryProperties.batteryTemperature;
        this.batteryFullCharge = batteryProperties.batteryFullCharge;
        this.batteryChargeCounter = batteryProperties.batteryChargeCounter;
        this.batteryTechnology = batteryProperties.batteryTechnology;
    }

    private BatteryProperties(Parcel parcel) {
        this.chargerAcOnline = parcel.readInt() == 1;
        this.chargerUsbOnline = parcel.readInt() == 1;
        this.chargerWirelessOnline = parcel.readInt() == 1;
        this.maxChargingCurrent = parcel.readInt();
        this.maxChargingVoltage = parcel.readInt();
        this.batteryStatus = parcel.readInt();
        this.batteryHealth = parcel.readInt();
        this.batteryPresent = parcel.readInt() == 1;
        this.batteryLevel = parcel.readInt();
        this.batteryVoltage = parcel.readInt();
        this.batteryTemperature = parcel.readInt();
        this.batteryFullCharge = parcel.readInt();
        this.batteryChargeCounter = parcel.readInt();
        this.batteryTechnology = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.chargerAcOnline ? 1 : 0);
        parcel.writeInt(this.chargerUsbOnline ? 1 : 0);
        parcel.writeInt(this.chargerWirelessOnline ? 1 : 0);
        parcel.writeInt(this.maxChargingCurrent);
        parcel.writeInt(this.maxChargingVoltage);
        parcel.writeInt(this.batteryStatus);
        parcel.writeInt(this.batteryHealth);
        parcel.writeInt(this.batteryPresent ? 1 : 0);
        parcel.writeInt(this.batteryLevel);
        parcel.writeInt(this.batteryVoltage);
        parcel.writeInt(this.batteryTemperature);
        parcel.writeInt(this.batteryFullCharge);
        parcel.writeInt(this.batteryChargeCounter);
        parcel.writeString(this.batteryTechnology);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
