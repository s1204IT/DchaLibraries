package com.android.server.usb.descriptors;

import android.util.Log;
import com.android.server.usb.descriptors.report.ReportCanvas;
import com.android.server.usb.descriptors.report.UsbStrings;

public abstract class UsbACInterface extends UsbDescriptor {
    public static final byte ACI_CLOCK_MULTIPLIER = 12;
    public static final byte ACI_CLOCK_SELECTOR = 11;
    public static final byte ACI_CLOCK_SOURCE = 10;
    public static final byte ACI_EXTENSION_UNIT = 8;
    public static final byte ACI_FEATURE_UNIT = 6;
    public static final byte ACI_HEADER = 1;
    public static final byte ACI_INPUT_TERMINAL = 2;
    public static final byte ACI_MIXER_UNIT = 4;
    public static final byte ACI_OUTPUT_TERMINAL = 3;
    public static final byte ACI_PROCESSING_UNIT = 7;
    public static final byte ACI_SAMPLE_RATE_CONVERTER = 13;
    public static final byte ACI_SELECTOR_UNIT = 5;
    public static final byte ACI_UNDEFINED = 0;
    public static final byte ASI_FORMAT_SPECIFIC = 3;
    public static final byte ASI_FORMAT_TYPE = 2;
    public static final byte ASI_GENERAL = 1;
    public static final byte ASI_UNDEFINED = 0;
    public static final int FORMAT_III_IEC1937AC3 = 8193;
    public static final int FORMAT_III_IEC1937_MPEG1_Layer1 = 8194;
    public static final int FORMAT_III_IEC1937_MPEG1_Layer2 = 8195;
    public static final int FORMAT_III_IEC1937_MPEG2_EXT = 8196;
    public static final int FORMAT_III_IEC1937_MPEG2_Layer1LS = 8197;
    public static final int FORMAT_III_UNDEFINED = 8192;
    public static final int FORMAT_II_AC3 = 4098;
    public static final int FORMAT_II_MPEG = 4097;
    public static final int FORMAT_II_UNDEFINED = 4096;
    public static final int FORMAT_I_ALAW = 4;
    public static final int FORMAT_I_IEEE_FLOAT = 3;
    public static final int FORMAT_I_MULAW = 5;
    public static final int FORMAT_I_PCM = 1;
    public static final int FORMAT_I_PCM8 = 2;
    public static final int FORMAT_I_UNDEFINED = 0;
    public static final byte MSI_ELEMENT = 4;
    public static final byte MSI_HEADER = 1;
    public static final byte MSI_IN_JACK = 2;
    public static final byte MSI_OUT_JACK = 3;
    public static final byte MSI_UNDEFINED = 0;
    private static final String TAG = "UsbACInterface";
    protected final int mSubclass;
    protected final byte mSubtype;

    public UsbACInterface(int i, byte b, byte b2, int i2) {
        super(i, b);
        this.mSubtype = b2;
        this.mSubclass = i2;
    }

    public byte getSubtype() {
        return this.mSubtype;
    }

    public int getSubclass() {
        return this.mSubclass;
    }

    private static UsbDescriptor allocAudioControlDescriptor(UsbDescriptorParser usbDescriptorParser, ByteStream byteStream, int i, byte b, byte b2, int i2) {
        switch (b2) {
            case 1:
                int iUnpackUsbShort = byteStream.unpackUsbShort();
                usbDescriptorParser.setACInterfaceSpec(iUnpackUsbShort);
                if (iUnpackUsbShort == 512) {
                    return new Usb20ACHeader(i, b, b2, i2, iUnpackUsbShort);
                }
                return new Usb10ACHeader(i, b, b2, i2, iUnpackUsbShort);
            case 2:
                if (usbDescriptorParser.getACInterfaceSpec() == 512) {
                    return new Usb20ACInputTerminal(i, b, b2, i2);
                }
                return new Usb10ACInputTerminal(i, b, b2, i2);
            case 3:
                if (usbDescriptorParser.getACInterfaceSpec() == 512) {
                    return new Usb20ACOutputTerminal(i, b, b2, i2);
                }
                return new Usb10ACOutputTerminal(i, b, b2, i2);
            case 4:
                if (usbDescriptorParser.getACInterfaceSpec() == 512) {
                    return new Usb20ACMixerUnit(i, b, b2, i2);
                }
                return new Usb10ACMixerUnit(i, b, b2, i2);
            case 5:
                return new UsbACSelectorUnit(i, b, b2, i2);
            case 6:
                return new UsbACFeatureUnit(i, b, b2, i2);
            default:
                Log.w(TAG, "Unknown Audio Class Interface subtype:0x" + Integer.toHexString(b2));
                return new UsbACInterfaceUnparsed(i, b, b2, i2);
        }
    }

    private static UsbDescriptor allocAudioStreamingDescriptor(UsbDescriptorParser usbDescriptorParser, ByteStream byteStream, int i, byte b, byte b2, int i2) {
        int aCInterfaceSpec = usbDescriptorParser.getACInterfaceSpec();
        switch (b2) {
            case 1:
                if (aCInterfaceSpec == 512) {
                    return new Usb20ASGeneral(i, b, b2, i2);
                }
                return new Usb10ASGeneral(i, b, b2, i2);
            case 2:
                return UsbASFormat.allocDescriptor(usbDescriptorParser, byteStream, i, b, b2, i2);
            default:
                Log.w(TAG, "Unknown Audio Streaming Interface subtype:0x" + Integer.toHexString(b2));
                return null;
        }
    }

    private static UsbDescriptor allocMidiStreamingDescriptor(int i, byte b, byte b2, int i2) {
        switch (b2) {
            case 1:
                return new UsbMSMidiHeader(i, b, b2, i2);
            case 2:
                return new UsbMSMidiInputJack(i, b, b2, i2);
            case 3:
                return new UsbMSMidiOutputJack(i, b, b2, i2);
            default:
                Log.w(TAG, "Unknown MIDI Streaming Interface subtype:0x" + Integer.toHexString(b2));
                return null;
        }
    }

    public static UsbDescriptor allocDescriptor(UsbDescriptorParser usbDescriptorParser, ByteStream byteStream, int i, byte b) {
        byte b2 = byteStream.getByte();
        int usbSubclass = usbDescriptorParser.getCurInterface().getUsbSubclass();
        switch (usbSubclass) {
            case 1:
                return allocAudioControlDescriptor(usbDescriptorParser, byteStream, i, b, b2, usbSubclass);
            case 2:
                return allocAudioStreamingDescriptor(usbDescriptorParser, byteStream, i, b, b2, usbSubclass);
            case 3:
                return allocMidiStreamingDescriptor(i, b, b2, usbSubclass);
            default:
                Log.w(TAG, "Unknown Audio Class Interface Subclass: 0x" + Integer.toHexString(usbSubclass));
                return null;
        }
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        super.report(reportCanvas);
        int subclass = getSubclass();
        String aCInterfaceSubclassName = UsbStrings.getACInterfaceSubclassName(subclass);
        byte subtype = getSubtype();
        String aCControlInterfaceName = UsbStrings.getACControlInterfaceName(subtype);
        reportCanvas.openList();
        reportCanvas.writeListItem("Subclass: " + ReportCanvas.getHexString(subclass) + " " + aCInterfaceSubclassName);
        reportCanvas.writeListItem("Subtype: " + ReportCanvas.getHexString(subtype) + " " + aCControlInterfaceName);
        reportCanvas.closeList();
    }
}
