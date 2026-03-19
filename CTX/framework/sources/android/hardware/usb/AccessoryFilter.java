package android.hardware.usb;

import android.media.midi.MidiDeviceInfo;
import com.android.internal.util.dump.DualDumpOutputStream;
import java.io.IOException;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class AccessoryFilter {
    public final String mManufacturer;
    public final String mModel;
    public final String mVersion;

    public AccessoryFilter(String str, String str2, String str3) {
        this.mManufacturer = str;
        this.mModel = str2;
        this.mVersion = str3;
    }

    public AccessoryFilter(UsbAccessory usbAccessory) {
        this.mManufacturer = usbAccessory.getManufacturer();
        this.mModel = usbAccessory.getModel();
        this.mVersion = usbAccessory.getVersion();
    }

    public static AccessoryFilter read(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int attributeCount = xmlPullParser.getAttributeCount();
        String str = null;
        String str2 = null;
        String str3 = null;
        for (int i = 0; i < attributeCount; i++) {
            String attributeName = xmlPullParser.getAttributeName(i);
            String attributeValue = xmlPullParser.getAttributeValue(i);
            if (MidiDeviceInfo.PROPERTY_MANUFACTURER.equals(attributeName)) {
                str = attributeValue;
            } else if ("model".equals(attributeName)) {
                str2 = attributeValue;
            } else if ("version".equals(attributeName)) {
                str3 = attributeValue;
            }
        }
        return new AccessoryFilter(str, str2, str3);
    }

    public void write(XmlSerializer xmlSerializer) throws IOException {
        xmlSerializer.startTag(null, "usb-accessory");
        if (this.mManufacturer != null) {
            xmlSerializer.attribute(null, MidiDeviceInfo.PROPERTY_MANUFACTURER, this.mManufacturer);
        }
        if (this.mModel != null) {
            xmlSerializer.attribute(null, "model", this.mModel);
        }
        if (this.mVersion != null) {
            xmlSerializer.attribute(null, "version", this.mVersion);
        }
        xmlSerializer.endTag(null, "usb-accessory");
    }

    public boolean matches(UsbAccessory usbAccessory) {
        if (this.mManufacturer != null && !usbAccessory.getManufacturer().equals(this.mManufacturer)) {
            return false;
        }
        if (this.mModel == null || usbAccessory.getModel().equals(this.mModel)) {
            return this.mVersion == null || usbAccessory.getVersion().equals(this.mVersion);
        }
        return false;
    }

    public boolean contains(AccessoryFilter accessoryFilter) {
        if (this.mManufacturer != null && !Objects.equals(accessoryFilter.mManufacturer, this.mManufacturer)) {
            return false;
        }
        if (this.mModel == null || Objects.equals(accessoryFilter.mModel, this.mModel)) {
            return this.mVersion == null || Objects.equals(accessoryFilter.mVersion, this.mVersion);
        }
        return false;
    }

    public boolean equals(Object obj) {
        if (this.mManufacturer == null || this.mModel == null || this.mVersion == null) {
            return false;
        }
        if (obj instanceof AccessoryFilter) {
            AccessoryFilter accessoryFilter = (AccessoryFilter) obj;
            return this.mManufacturer.equals(accessoryFilter.mManufacturer) && this.mModel.equals(accessoryFilter.mModel) && this.mVersion.equals(accessoryFilter.mVersion);
        }
        if (!(obj instanceof UsbAccessory)) {
            return false;
        }
        UsbAccessory usbAccessory = (UsbAccessory) obj;
        return this.mManufacturer.equals(usbAccessory.getManufacturer()) && this.mModel.equals(usbAccessory.getModel()) && this.mVersion.equals(usbAccessory.getVersion());
    }

    public int hashCode() {
        int iHashCode;
        if (this.mManufacturer != null) {
            iHashCode = this.mManufacturer.hashCode();
        } else {
            iHashCode = 0;
        }
        return (iHashCode ^ (this.mModel == null ? 0 : this.mModel.hashCode())) ^ (this.mVersion != null ? this.mVersion.hashCode() : 0);
    }

    public String toString() {
        return "AccessoryFilter[mManufacturer=\"" + this.mManufacturer + "\", mModel=\"" + this.mModel + "\", mVersion=\"" + this.mVersion + "\"]";
    }

    public void dump(DualDumpOutputStream dualDumpOutputStream, String str, long j) {
        long jStart = dualDumpOutputStream.start(str, j);
        dualDumpOutputStream.write(MidiDeviceInfo.PROPERTY_MANUFACTURER, 1138166333441L, this.mManufacturer);
        dualDumpOutputStream.write("model", 1138166333442L, this.mModel);
        dualDumpOutputStream.write("version", 1138166333443L, this.mVersion);
        dualDumpOutputStream.end(jStart);
    }
}
