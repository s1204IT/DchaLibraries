package com.android.server.usb.descriptors;

import android.hardware.usb.UsbDevice;
import android.util.Log;
import java.util.ArrayList;

public final class UsbDescriptorParser {
    private static final boolean DEBUG = false;
    private static final int DESCRIPTORS_ALLOC_SIZE = 128;
    private static final float IN_HEADSET_TRIGGER = 0.75f;
    private static final float OUT_HEADSET_TRIGGER = 0.75f;
    private static final String TAG = "UsbDescriptorParser";
    private int mACInterfacesSpec;
    private UsbConfigDescriptor mCurConfigDescriptor;
    private UsbInterfaceDescriptor mCurInterfaceDescriptor;
    private final ArrayList<UsbDescriptor> mDescriptors;
    private final String mDeviceAddr;
    private UsbDeviceDescriptor mDeviceDescriptor;

    private native String getDescriptorString_native(String str, int i);

    private native byte[] getRawDescriptors_native(String str);

    public UsbDescriptorParser(String str, ArrayList<UsbDescriptor> arrayList) {
        this.mACInterfacesSpec = 256;
        this.mDeviceAddr = str;
        this.mDescriptors = arrayList;
        this.mDeviceDescriptor = (UsbDeviceDescriptor) arrayList.get(0);
    }

    public UsbDescriptorParser(String str, byte[] bArr) throws UsbDescriptorsStreamFormatException {
        this.mACInterfacesSpec = 256;
        this.mDeviceAddr = str;
        this.mDescriptors = new ArrayList<>(128);
        parseDescriptors(bArr);
    }

    public String getDeviceAddr() {
        return this.mDeviceAddr;
    }

    public int getUsbSpec() {
        if (this.mDeviceDescriptor != null) {
            return this.mDeviceDescriptor.getSpec();
        }
        throw new IllegalArgumentException();
    }

    public void setACInterfaceSpec(int i) {
        this.mACInterfacesSpec = i;
    }

    public int getACInterfaceSpec() {
        return this.mACInterfacesSpec;
    }

    private class UsbDescriptorsStreamFormatException extends Exception {
        String mMessage;

        UsbDescriptorsStreamFormatException(String str) {
            this.mMessage = str;
        }

        @Override
        public String toString() {
            return "Descriptor Stream Format Exception: " + this.mMessage;
        }
    }

    private UsbDescriptor allocDescriptor(ByteStream byteStream) throws UsbDescriptorsStreamFormatException {
        UsbDescriptor usbInterfaceAssoc;
        byteStream.resetReadCount();
        int unsignedByte = byteStream.getUnsignedByte();
        byte b = byteStream.getByte();
        switch (b) {
            case 1:
                UsbDeviceDescriptor usbDeviceDescriptor = new UsbDeviceDescriptor(unsignedByte, b);
                this.mDeviceDescriptor = usbDeviceDescriptor;
                usbInterfaceAssoc = usbDeviceDescriptor;
                break;
            case 2:
                UsbConfigDescriptor usbConfigDescriptor = new UsbConfigDescriptor(unsignedByte, b);
                this.mCurConfigDescriptor = usbConfigDescriptor;
                if (this.mDeviceDescriptor != null) {
                    this.mDeviceDescriptor.addConfigDescriptor(this.mCurConfigDescriptor);
                    usbInterfaceAssoc = usbConfigDescriptor;
                } else {
                    Log.e(TAG, "Config Descriptor found with no associated Device Descriptor!");
                    throw new UsbDescriptorsStreamFormatException("Config Descriptor found with no associated Device Descriptor!");
                }
                break;
            case 4:
                UsbInterfaceDescriptor usbInterfaceDescriptor = new UsbInterfaceDescriptor(unsignedByte, b);
                this.mCurInterfaceDescriptor = usbInterfaceDescriptor;
                if (this.mCurConfigDescriptor != null) {
                    this.mCurConfigDescriptor.addInterfaceDescriptor(this.mCurInterfaceDescriptor);
                    usbInterfaceAssoc = usbInterfaceDescriptor;
                } else {
                    Log.e(TAG, "Interface Descriptor found with no associated Config Descriptor!");
                    throw new UsbDescriptorsStreamFormatException("Interface Descriptor found with no associated Config Descriptor!");
                }
                break;
            case 5:
                UsbEndpointDescriptor usbEndpointDescriptor = new UsbEndpointDescriptor(unsignedByte, b);
                if (this.mCurInterfaceDescriptor != null) {
                    this.mCurInterfaceDescriptor.addEndpointDescriptor(usbEndpointDescriptor);
                    usbInterfaceAssoc = usbEndpointDescriptor;
                } else {
                    Log.e(TAG, "Endpoint Descriptor found with no associated Interface Descriptor!");
                    throw new UsbDescriptorsStreamFormatException("Endpoint Descriptor found with no associated Interface Descriptor!");
                }
                break;
            case 11:
                usbInterfaceAssoc = new UsbInterfaceAssoc(unsignedByte, b);
                break;
            case 33:
                usbInterfaceAssoc = new UsbHIDDescriptor(unsignedByte, b);
                break;
            case 36:
                usbInterfaceAssoc = UsbACInterface.allocDescriptor(this, byteStream, unsignedByte, b);
                break;
            case 37:
                usbInterfaceAssoc = UsbACEndpoint.allocDescriptor(this, unsignedByte, b);
                break;
            default:
                usbInterfaceAssoc = null;
                break;
        }
        if (usbInterfaceAssoc == null) {
            Log.i(TAG, "Unknown Descriptor len: " + unsignedByte + " type:0x" + Integer.toHexString(b));
            return new UsbUnknown(unsignedByte, b);
        }
        return usbInterfaceAssoc;
    }

    public UsbDeviceDescriptor getDeviceDescriptor() {
        return this.mDeviceDescriptor;
    }

    public UsbInterfaceDescriptor getCurInterface() {
        return this.mCurInterfaceDescriptor;
    }

    public void parseDescriptors(byte[] bArr) throws UsbDescriptorsStreamFormatException {
        ByteStream byteStream = new ByteStream(bArr);
        while (byteStream.available() > 0) {
            UsbDescriptor usbDescriptorAllocDescriptor = null;
            try {
                usbDescriptorAllocDescriptor = allocDescriptor(byteStream);
            } catch (Exception e) {
                Log.e(TAG, "Exception allocating USB descriptor.", e);
            }
            if (usbDescriptorAllocDescriptor != null) {
                try {
                    try {
                        usbDescriptorAllocDescriptor.parseRawDescriptors(byteStream);
                        usbDescriptorAllocDescriptor.postParse(byteStream);
                    } catch (Exception e2) {
                        Log.e(TAG, "Exception parsing USB descriptors.", e2);
                        usbDescriptorAllocDescriptor.setStatus(4);
                    }
                } finally {
                    this.mDescriptors.add(usbDescriptorAllocDescriptor);
                }
            }
        }
    }

    public byte[] getRawDescriptors() {
        return getRawDescriptors_native(this.mDeviceAddr);
    }

    public String getDescriptorString(int i) {
        return getDescriptorString_native(this.mDeviceAddr, i);
    }

    public int getParsingSpec() {
        if (this.mDeviceDescriptor != null) {
            return this.mDeviceDescriptor.getSpec();
        }
        return 0;
    }

    public ArrayList<UsbDescriptor> getDescriptors() {
        return this.mDescriptors;
    }

    public UsbDevice toAndroidUsbDevice() {
        if (this.mDeviceDescriptor == null) {
            Log.e(TAG, "toAndroidUsbDevice() ERROR - No Device Descriptor");
            return null;
        }
        UsbDevice android2 = this.mDeviceDescriptor.toAndroid(this);
        if (android2 == null) {
            Log.e(TAG, "toAndroidUsbDevice() ERROR Creating Device");
        }
        return android2;
    }

    public ArrayList<UsbDescriptor> getDescriptors(byte b) {
        ArrayList<UsbDescriptor> arrayList = new ArrayList<>();
        for (UsbDescriptor usbDescriptor : this.mDescriptors) {
            if (usbDescriptor.getType() == b) {
                arrayList.add(usbDescriptor);
            }
        }
        return arrayList;
    }

    public ArrayList<UsbDescriptor> getInterfaceDescriptorsForClass(int i) {
        ArrayList<UsbDescriptor> arrayList = new ArrayList<>();
        for (UsbDescriptor usbDescriptor : this.mDescriptors) {
            if (usbDescriptor.getType() == 4) {
                if (usbDescriptor instanceof UsbInterfaceDescriptor) {
                    if (((UsbInterfaceDescriptor) usbDescriptor).getUsbClass() == i) {
                        arrayList.add(usbDescriptor);
                    }
                } else {
                    Log.w(TAG, "Unrecognized Interface l: " + usbDescriptor.getLength() + " t:0x" + Integer.toHexString(usbDescriptor.getType()));
                }
            }
        }
        return arrayList;
    }

    public ArrayList<UsbDescriptor> getACInterfaceDescriptors(byte b, int i) {
        ArrayList<UsbDescriptor> arrayList = new ArrayList<>();
        for (UsbDescriptor usbDescriptor : this.mDescriptors) {
            if (usbDescriptor.getType() == 36) {
                if (usbDescriptor instanceof UsbACInterface) {
                    UsbACInterface usbACInterface = (UsbACInterface) usbDescriptor;
                    if (usbACInterface.getSubtype() == b && usbACInterface.getSubclass() == i) {
                        arrayList.add(usbDescriptor);
                    }
                } else {
                    Log.w(TAG, "Unrecognized Audio Interface l: " + usbDescriptor.getLength() + " t:0x" + Integer.toHexString(usbDescriptor.getType()));
                }
            }
        }
        return arrayList;
    }

    public boolean hasInput() {
        for (UsbDescriptor usbDescriptor : getACInterfaceDescriptors((byte) 2, 1)) {
            if (usbDescriptor instanceof UsbACTerminal) {
                int terminalType = ((UsbACTerminal) usbDescriptor).getTerminalType() & (-256);
                if (terminalType != 256 && terminalType != 768) {
                    return true;
                }
            } else {
                Log.w(TAG, "Undefined Audio Input terminal l: " + usbDescriptor.getLength() + " t:0x" + Integer.toHexString(usbDescriptor.getType()));
            }
        }
        return false;
    }

    public boolean hasOutput() {
        for (UsbDescriptor usbDescriptor : getACInterfaceDescriptors((byte) 3, 1)) {
            if (usbDescriptor instanceof UsbACTerminal) {
                int terminalType = ((UsbACTerminal) usbDescriptor).getTerminalType() & (-256);
                if (terminalType != 256 && terminalType != 512) {
                    return true;
                }
            } else {
                Log.w(TAG, "Undefined Audio Input terminal l: " + usbDescriptor.getLength() + " t:0x" + Integer.toHexString(usbDescriptor.getType()));
            }
        }
        return false;
    }

    public boolean hasMic() {
        for (UsbDescriptor usbDescriptor : getACInterfaceDescriptors((byte) 2, 1)) {
            if (usbDescriptor instanceof UsbACTerminal) {
                UsbACTerminal usbACTerminal = (UsbACTerminal) usbDescriptor;
                if (usbACTerminal.getTerminalType() == 513 || usbACTerminal.getTerminalType() == 1026 || usbACTerminal.getTerminalType() == 1024 || usbACTerminal.getTerminalType() == 1539) {
                    return true;
                }
            } else {
                Log.w(TAG, "Undefined Audio Input terminal l: " + usbDescriptor.getLength() + " t:0x" + Integer.toHexString(usbDescriptor.getType()));
            }
        }
        return false;
    }

    public boolean hasSpeaker() {
        for (UsbDescriptor usbDescriptor : getACInterfaceDescriptors((byte) 3, 1)) {
            if (usbDescriptor instanceof UsbACTerminal) {
                UsbACTerminal usbACTerminal = (UsbACTerminal) usbDescriptor;
                if (usbACTerminal.getTerminalType() == 769 || usbACTerminal.getTerminalType() == 770 || usbACTerminal.getTerminalType() == 1026) {
                    return true;
                }
            } else {
                Log.w(TAG, "Undefined Audio Output terminal l: " + usbDescriptor.getLength() + " t:0x" + Integer.toHexString(usbDescriptor.getType()));
            }
        }
        return false;
    }

    public boolean hasAudioInterface() {
        return true ^ getInterfaceDescriptorsForClass(1).isEmpty();
    }

    public boolean hasHIDInterface() {
        return !getInterfaceDescriptorsForClass(3).isEmpty();
    }

    public boolean hasStorageInterface() {
        return !getInterfaceDescriptorsForClass(8).isEmpty();
    }

    public boolean hasMIDIInterface() {
        for (UsbDescriptor usbDescriptor : getInterfaceDescriptorsForClass(1)) {
            if (usbDescriptor instanceof UsbInterfaceDescriptor) {
                if (((UsbInterfaceDescriptor) usbDescriptor).getUsbSubclass() == 3) {
                    return true;
                }
            } else {
                Log.w(TAG, "Undefined Audio Class Interface l: " + usbDescriptor.getLength() + " t:0x" + Integer.toHexString(usbDescriptor.getType()));
            }
        }
        return false;
    }

    public float getInputHeadsetProbability() {
        float f = 0.0f;
        if (hasMIDIInterface()) {
            return 0.0f;
        }
        boolean zHasMic = hasMic();
        boolean zHasSpeaker = hasSpeaker();
        if (zHasMic && zHasSpeaker) {
            f = 0.75f;
        }
        if (zHasMic && hasHIDInterface()) {
            return f + 0.25f;
        }
        return f;
    }

    public boolean isInputHeadset() {
        return getInputHeadsetProbability() >= 0.75f;
    }

    public float getOutputHeadsetProbability() {
        if (hasMIDIInterface()) {
            return 0.0f;
        }
        boolean z = false;
        for (UsbDescriptor usbDescriptor : getACInterfaceDescriptors((byte) 3, 1)) {
            if (usbDescriptor instanceof UsbACTerminal) {
                UsbACTerminal usbACTerminal = (UsbACTerminal) usbDescriptor;
                if (usbACTerminal.getTerminalType() == 769 || usbACTerminal.getTerminalType() == 770 || usbACTerminal.getTerminalType() == 1026) {
                    z = true;
                    break;
                }
            } else {
                Log.w(TAG, "Undefined Audio Output terminal l: " + usbDescriptor.getLength() + " t:0x" + Integer.toHexString(usbDescriptor.getType()));
            }
        }
        float f = z ? 0.75f : 0.0f;
        if (z && hasHIDInterface()) {
            return f + 0.25f;
        }
        return f;
    }

    public boolean isOutputHeadset() {
        return getOutputHeadsetProbability() >= 0.75f;
    }
}
