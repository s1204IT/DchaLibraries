package com.android.future.usb;

public class UsbAccessory {
    private final String mDescription;
    private final String mManufacturer;
    private final String mModel;
    private final String mSerial;
    private final String mUri;
    private final String mVersion;

    UsbAccessory(android.hardware.usb.UsbAccessory usbAccessory) {
        this.mManufacturer = usbAccessory.getManufacturer();
        this.mModel = usbAccessory.getModel();
        this.mDescription = usbAccessory.getDescription();
        this.mVersion = usbAccessory.getVersion();
        this.mUri = usbAccessory.getUri();
        this.mSerial = usbAccessory.getSerial();
    }

    public String getManufacturer() {
        return this.mManufacturer;
    }

    public String getModel() {
        return this.mModel;
    }

    public String getDescription() {
        return this.mDescription;
    }

    public String getVersion() {
        return this.mVersion;
    }

    public String getUri() {
        return this.mUri;
    }

    public String getSerial() {
        return this.mSerial;
    }

    private static boolean compare(String str, String str2) {
        if (str == null) {
            return str2 == null;
        }
        return str.equals(str2);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof UsbAccessory)) {
            return false;
        }
        UsbAccessory usbAccessory = (UsbAccessory) obj;
        return compare(this.mManufacturer, usbAccessory.getManufacturer()) && compare(this.mModel, usbAccessory.getModel()) && compare(this.mDescription, usbAccessory.getDescription()) && compare(this.mVersion, usbAccessory.getVersion()) && compare(this.mUri, usbAccessory.getUri()) && compare(this.mSerial, usbAccessory.getSerial());
    }

    public int hashCode() {
        int iHashCode;
        if (this.mManufacturer != null) {
            iHashCode = this.mManufacturer.hashCode();
        } else {
            iHashCode = 0;
        }
        return ((((iHashCode ^ (this.mModel == null ? 0 : this.mModel.hashCode())) ^ (this.mDescription == null ? 0 : this.mDescription.hashCode())) ^ (this.mVersion == null ? 0 : this.mVersion.hashCode())) ^ (this.mUri == null ? 0 : this.mUri.hashCode())) ^ (this.mSerial != null ? this.mSerial.hashCode() : 0);
    }

    public String toString() {
        return "UsbAccessory[mManufacturer=" + this.mManufacturer + ", mModel=" + this.mModel + ", mDescription=" + this.mDescription + ", mVersion=" + this.mVersion + ", mUri=" + this.mUri + ", mSerial=" + this.mSerial + "]";
    }
}
