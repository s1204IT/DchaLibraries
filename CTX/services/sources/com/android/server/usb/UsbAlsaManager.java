package com.android.server.usb;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.usb.UsbDevice;
import android.media.IAudioService;
import android.os.Bundle;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Slog;
import com.android.internal.alsa.AlsaCardsParser;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.usb.descriptors.UsbDescriptorParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import libcore.io.IoUtils;

public final class UsbAlsaManager {
    private static final String ALSA_DIRECTORY = "/dev/snd/";
    private static final boolean DEBUG = false;
    private static final String TAG = UsbAlsaManager.class.getSimpleName();
    private IAudioService mAudioService;
    private final Context mContext;
    private final boolean mHasMidiFeature;
    private UsbAlsaDevice mSelectedDevice;
    private final AlsaCardsParser mCardsParser = new AlsaCardsParser();
    private final ArrayList<UsbAlsaDevice> mAlsaDevices = new ArrayList<>();
    private final HashMap<String, UsbMidiDevice> mMidiDevices = new HashMap<>();
    private UsbMidiDevice mPeripheralMidiDevice = null;

    UsbAlsaManager(Context context) {
        this.mContext = context;
        this.mHasMidiFeature = context.getPackageManager().hasSystemFeature("android.software.midi");
    }

    public void systemReady() {
        this.mAudioService = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
    }

    private synchronized void selectAlsaDevice(UsbAlsaDevice usbAlsaDevice) {
        if (this.mSelectedDevice != null) {
            deselectAlsaDevice();
        }
        if (Settings.Secure.getInt(this.mContext.getContentResolver(), "usb_audio_automatic_routing_disabled", 0) != 0) {
            return;
        }
        this.mSelectedDevice = usbAlsaDevice;
        usbAlsaDevice.start();
    }

    private synchronized void deselectAlsaDevice() {
        if (this.mSelectedDevice != null) {
            this.mSelectedDevice.stop();
            this.mSelectedDevice = null;
        }
    }

    private int getAlsaDeviceListIndexFor(String str) {
        for (int i = 0; i < this.mAlsaDevices.size(); i++) {
            if (this.mAlsaDevices.get(i).getDeviceAddress().equals(str)) {
                return i;
            }
        }
        return -1;
    }

    private UsbAlsaDevice removeAlsaDeviceFromList(String str) {
        int alsaDeviceListIndexFor = getAlsaDeviceListIndexFor(str);
        if (alsaDeviceListIndexFor > -1) {
            return this.mAlsaDevices.remove(alsaDeviceListIndexFor);
        }
        return null;
    }

    UsbAlsaDevice selectDefaultDevice() {
        if (this.mAlsaDevices.size() > 0) {
            UsbAlsaDevice usbAlsaDevice = this.mAlsaDevices.get(0);
            if (usbAlsaDevice != null) {
                selectAlsaDevice(usbAlsaDevice);
            }
            return usbAlsaDevice;
        }
        return null;
    }

    void usbDeviceAdded(String str, UsbDevice usbDevice, UsbDescriptorParser usbDescriptorParser) {
        String str2;
        this.mCardsParser.scan();
        AlsaCardsParser.AlsaCardRecord alsaCardRecordFindCardNumFor = this.mCardsParser.findCardNumFor(str);
        if (alsaCardRecordFindCardNumFor == null) {
            return;
        }
        boolean zHasInput = usbDescriptorParser.hasInput();
        boolean zHasOutput = usbDescriptorParser.hasOutput();
        if (zHasInput || zHasOutput) {
            boolean zIsInputHeadset = usbDescriptorParser.isInputHeadset();
            boolean zIsOutputHeadset = usbDescriptorParser.isOutputHeadset();
            if (this.mAudioService == null) {
                Slog.e(TAG, "no AudioService");
                return;
            }
            UsbAlsaDevice usbAlsaDevice = new UsbAlsaDevice(this.mAudioService, alsaCardRecordFindCardNumFor.getCardNum(), 0, str, zHasOutput, zHasInput, zIsInputHeadset, zIsOutputHeadset);
            usbAlsaDevice.setDeviceNameAndDescription(alsaCardRecordFindCardNumFor.getCardName(), alsaCardRecordFindCardNumFor.getCardDescription());
            this.mAlsaDevices.add(0, usbAlsaDevice);
            selectAlsaDevice(usbAlsaDevice);
        }
        if (usbDescriptorParser.hasMIDIInterface() && this.mHasMidiFeature) {
            Bundle bundle = new Bundle();
            String manufacturerName = usbDevice.getManufacturerName();
            String productName = usbDevice.getProductName();
            String version = usbDevice.getVersion();
            if (manufacturerName != null && !manufacturerName.isEmpty()) {
                if (productName != null && !productName.isEmpty()) {
                    str2 = manufacturerName + " " + productName;
                } else {
                    str2 = manufacturerName;
                }
            } else {
                str2 = productName;
            }
            bundle.putString(com.android.server.pm.Settings.ATTR_NAME, str2);
            bundle.putString("manufacturer", manufacturerName);
            bundle.putString("product", productName);
            bundle.putString("version", version);
            bundle.putString("serial_number", usbDevice.getSerialNumber());
            bundle.putInt("alsa_card", alsaCardRecordFindCardNumFor.getCardNum());
            bundle.putInt("alsa_device", 0);
            bundle.putParcelable("usb_device", usbDevice);
            UsbMidiDevice usbMidiDeviceCreate = UsbMidiDevice.create(this.mContext, bundle, alsaCardRecordFindCardNumFor.getCardNum(), 0);
            if (usbMidiDeviceCreate != null) {
                this.mMidiDevices.put(str, usbMidiDeviceCreate);
            }
        }
    }

    synchronized void usbDeviceRemoved(String str) {
        UsbAlsaDevice usbAlsaDeviceRemoveAlsaDeviceFromList = removeAlsaDeviceFromList(str);
        Slog.i(TAG, "USB Audio Device Removed: " + usbAlsaDeviceRemoveAlsaDeviceFromList);
        if (usbAlsaDeviceRemoveAlsaDeviceFromList != null && usbAlsaDeviceRemoveAlsaDeviceFromList == this.mSelectedDevice) {
            deselectAlsaDevice();
            selectDefaultDevice();
        }
        UsbMidiDevice usbMidiDeviceRemove = this.mMidiDevices.remove(str);
        if (usbMidiDeviceRemove != null) {
            Slog.i(TAG, "USB MIDI Device Removed: " + usbMidiDeviceRemove);
            IoUtils.closeQuietly(usbMidiDeviceRemove);
        }
    }

    void setPeripheralMidiState(boolean z, int i, int i2) {
        if (!this.mHasMidiFeature) {
            return;
        }
        if (!z || this.mPeripheralMidiDevice != null) {
            if (!z && this.mPeripheralMidiDevice != null) {
                IoUtils.closeQuietly(this.mPeripheralMidiDevice);
                this.mPeripheralMidiDevice = null;
                return;
            }
            return;
        }
        Bundle bundle = new Bundle();
        Resources resources = this.mContext.getResources();
        bundle.putString(com.android.server.pm.Settings.ATTR_NAME, resources.getString(R.string.miniresolver_call));
        bundle.putString("manufacturer", resources.getString(R.string.mime_type_video_ext));
        bundle.putString("product", resources.getString(R.string.miniresolver_call_in_work));
        bundle.putInt("alsa_card", i);
        bundle.putInt("alsa_device", i2);
        this.mPeripheralMidiDevice = UsbMidiDevice.create(this.mContext, bundle, i, i2);
    }

    public void dump(DualDumpOutputStream dualDumpOutputStream, String str, long j) {
        long jStart = dualDumpOutputStream.start(str, j);
        dualDumpOutputStream.write("cards_parser", 1120986464257L, this.mCardsParser.getScanStatus());
        Iterator<UsbAlsaDevice> it = this.mAlsaDevices.iterator();
        while (it.hasNext()) {
            it.next().dump(dualDumpOutputStream, "alsa_devices", 2246267895810L);
        }
        for (String str2 : this.mMidiDevices.keySet()) {
            this.mMidiDevices.get(str2).dump(str2, dualDumpOutputStream, "midi_devices", 2246267895811L);
        }
        dualDumpOutputStream.end(jStart);
    }
}
