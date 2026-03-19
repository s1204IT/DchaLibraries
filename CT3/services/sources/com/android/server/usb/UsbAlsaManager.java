package com.android.server.usb;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.media.IAudioService;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Slog;
import com.android.internal.alsa.AlsaCardsParser;
import com.android.internal.alsa.AlsaDevicesParser;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.audio.AudioService;
import com.android.server.pm.PackageManagerService;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import libcore.io.IoUtils;

public final class UsbAlsaManager {
    private static final String ALSA_DIRECTORY = "/dev/snd/";
    private static final boolean DEBUG = true;
    private static final String TAG = UsbAlsaManager.class.getSimpleName();
    private IAudioService mAudioService;
    private final Context mContext;
    private final boolean mHasMidiFeature;
    private final AlsaCardsParser mCardsParser = new AlsaCardsParser();
    private final AlsaDevicesParser mDevicesParser = new AlsaDevicesParser();
    private final HashMap<UsbDevice, UsbAudioDevice> mAudioDevices = new HashMap<>();
    private final HashMap<UsbDevice, UsbMidiDevice> mMidiDevices = new HashMap<>();
    private final HashMap<String, AlsaDevice> mAlsaDevices = new HashMap<>();
    private UsbAudioDevice mAccessoryAudioDevice = null;
    private UsbMidiDevice mPeripheralMidiDevice = null;
    private final FileObserver mAlsaObserver = new FileObserver(ALSA_DIRECTORY, 768) {
        @Override
        public void onEvent(int event, String path) {
            switch (event) {
                case 256:
                    UsbAlsaManager.this.alsaFileAdded(path);
                    break;
                case 512:
                    UsbAlsaManager.this.alsaFileRemoved(path);
                    break;
            }
        }
    };

    private final class AlsaDevice {
        public static final int TYPE_CAPTURE = 2;
        public static final int TYPE_MIDI = 3;
        public static final int TYPE_PLAYBACK = 1;
        public static final int TYPE_UNKNOWN = 0;
        public int mCard;
        public int mDevice;
        public int mType;

        public AlsaDevice(int type, int card, int device) {
            this.mType = type;
            this.mCard = card;
            this.mDevice = device;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof AlsaDevice)) {
                return false;
            }
            AlsaDevice other = (AlsaDevice) obj;
            return this.mType == other.mType && this.mCard == other.mCard && this.mDevice == other.mDevice;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("AlsaDevice: [card: ").append(this.mCard);
            sb.append(", device: ").append(this.mDevice);
            sb.append(", type: ").append(this.mType);
            sb.append("]");
            return sb.toString();
        }
    }

    UsbAlsaManager(Context context) {
        this.mContext = context;
        this.mHasMidiFeature = context.getPackageManager().hasSystemFeature("android.software.midi");
        this.mCardsParser.scan();
    }

    public void systemReady() {
        this.mAudioService = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
        this.mAlsaObserver.startWatching();
        File[] files = new File(ALSA_DIRECTORY).listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            alsaFileAdded(file.getName());
        }
    }

    private void notifyDeviceState(UsbAudioDevice audioDevice, boolean enabled) {
        Slog.d(TAG, "notifyDeviceState " + enabled + " " + audioDevice);
        if (this.mAudioService == null) {
            Slog.e(TAG, "no AudioService");
            return;
        }
        int isDisabled = Settings.Secure.getInt(this.mContext.getContentResolver(), "usb_audio_automatic_routing_disabled", 0);
        if (isDisabled != 0) {
            return;
        }
        int state = enabled ? 1 : 0;
        int alsaCard = audioDevice.mCard;
        int alsaDevice = audioDevice.mDevice;
        if (alsaCard < 0 || alsaDevice < 0) {
            Slog.e(TAG, "Invalid alsa card or device alsaCard: " + alsaCard + " alsaDevice: " + alsaDevice);
            return;
        }
        String address = AudioService.makeAlsaAddressString(alsaCard, alsaDevice);
        try {
            if (audioDevice.mHasPlayback) {
                int device = audioDevice == this.mAccessoryAudioDevice ? PackageManagerService.DumpState.DUMP_PREFERRED_XML : PackageManagerService.DumpState.DUMP_KEYSETS;
                Slog.i(TAG, "pre-call device:0x" + Integer.toHexString(device) + " addr:" + address + " name:" + audioDevice.mDeviceName);
                this.mAudioService.setWiredDeviceConnectionState(device, state, address, audioDevice.mDeviceName, TAG);
            }
            if (audioDevice.mHasCapture) {
                this.mAudioService.setWiredDeviceConnectionState(audioDevice == this.mAccessoryAudioDevice ? -2147481600 : -2147479552, state, address, audioDevice.mDeviceName, TAG);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException in setWiredDeviceConnectionState");
        }
    }

    private AlsaDevice waitForAlsaDevice(int card, int device, int type) {
        Slog.e(TAG, "waitForAlsaDevice(c:" + card + " d:" + device + ")");
        AlsaDevice testDevice = new AlsaDevice(type, card, device);
        synchronized (this.mAlsaDevices) {
            long timeout = SystemClock.elapsedRealtime() + 2500;
            while (!this.mAlsaDevices.values().contains(testDevice)) {
                long waitTime = timeout - SystemClock.elapsedRealtime();
                if (waitTime > 0) {
                    try {
                        this.mAlsaDevices.wait(waitTime);
                    } catch (InterruptedException e) {
                        Slog.d(TAG, "usb: InterruptedException while waiting for ALSA file.");
                    }
                }
                if (timeout <= SystemClock.elapsedRealtime()) {
                    Slog.e(TAG, "waitForAlsaDevice failed for " + testDevice);
                    return null;
                }
            }
            return testDevice;
        }
    }

    private void alsaFileAdded(String name) {
        int type = 0;
        if (name.startsWith("pcmC")) {
            if (name.endsWith("p")) {
                type = 1;
            } else if (name.endsWith("c")) {
                type = 2;
            }
        } else if (name.startsWith("midiC")) {
            type = 3;
        }
        if (type == 0) {
            return;
        }
        try {
            int c_index = name.indexOf(67);
            int d_index = name.indexOf(68);
            int end = name.length();
            if (type == 1 || type == 2) {
                end--;
            }
            int card = Integer.parseInt(name.substring(c_index + 1, d_index));
            int device = Integer.parseInt(name.substring(d_index + 1, end));
            synchronized (this.mAlsaDevices) {
                if (this.mAlsaDevices.get(name) == null) {
                    AlsaDevice alsaDevice = new AlsaDevice(type, card, device);
                    Slog.d(TAG, "Adding ALSA device " + alsaDevice);
                    this.mAlsaDevices.put(name, alsaDevice);
                    this.mAlsaDevices.notifyAll();
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "Could not parse ALSA file name " + name, e);
        }
    }

    private void alsaFileRemoved(String path) {
        synchronized (this.mAlsaDevices) {
            AlsaDevice device = this.mAlsaDevices.remove(path);
            if (device != null) {
                Slog.d(TAG, "ALSA device removed: " + device);
            }
        }
    }

    UsbAudioDevice selectAudioCard(int card) {
        Slog.d(TAG, "selectAudioCard() card:" + card + " isCardUsb(): " + this.mCardsParser.isCardUsb(card));
        if (!this.mCardsParser.isCardUsb(card)) {
            return null;
        }
        this.mDevicesParser.scan();
        int device = this.mDevicesParser.getDefaultDeviceNum(card);
        boolean hasPlayback = this.mDevicesParser.hasPlaybackDevices(card);
        boolean hasCapture = this.mDevicesParser.hasCaptureDevices(card);
        Slog.d(TAG, "usb: hasPlayback:" + hasPlayback + " hasCapture:" + hasCapture);
        int deviceClass = (this.mCardsParser.isCardUsb(card) ? 2 : 1) | Integer.MIN_VALUE;
        if (hasPlayback && waitForAlsaDevice(card, device, 1) == null) {
            return null;
        }
        if (hasCapture && waitForAlsaDevice(card, device, 2) == null) {
            return null;
        }
        UsbAudioDevice audioDevice = new UsbAudioDevice(card, device, hasPlayback, hasCapture, deviceClass);
        AlsaCardsParser.AlsaCardRecord cardRecord = this.mCardsParser.getCardRecordFor(card);
        audioDevice.mDeviceName = cardRecord.mCardName;
        audioDevice.mDeviceDescription = cardRecord.mCardDescription;
        notifyDeviceState(audioDevice, true);
        return audioDevice;
    }

    UsbAudioDevice selectDefaultDevice() {
        Slog.d(TAG, "UsbAudioManager.selectDefaultDevice()");
        return selectAudioCard(this.mCardsParser.getDefaultCard());
    }

    void usbDeviceAdded(UsbDevice usbDevice) {
        String name;
        Slog.d(TAG, "deviceAdded(): " + usbDevice.getManufacturerName() + " nm:" + usbDevice.getProductName());
        boolean isAudioDevice = false;
        int interfaceCount = usbDevice.getInterfaceCount();
        for (int ntrfaceIndex = 0; !isAudioDevice && ntrfaceIndex < interfaceCount; ntrfaceIndex++) {
            UsbInterface ntrface = usbDevice.getInterface(ntrfaceIndex);
            if (ntrface.getInterfaceClass() == 1) {
                isAudioDevice = true;
            }
        }
        Slog.d(TAG, "  isAudioDevice: " + isAudioDevice);
        if (!isAudioDevice) {
            return;
        }
        int addedCard = this.mCardsParser.getDefaultUsbCard();
        Slog.d(TAG, "  mCardsParser.isCardUsb(" + addedCard + ") = " + this.mCardsParser.isCardUsb(addedCard));
        if (this.mCardsParser.isCardUsb(addedCard)) {
            UsbAudioDevice audioDevice = selectAudioCard(addedCard);
            if (audioDevice != null) {
                this.mAudioDevices.put(usbDevice, audioDevice);
                Slog.i(TAG, "USB Audio Device Added: " + audioDevice);
            }
            boolean hasMidi = this.mDevicesParser.hasMIDIDevices(addedCard);
            if (hasMidi && this.mHasMidiFeature) {
                int device = this.mDevicesParser.getDefaultDeviceNum(addedCard);
                AlsaDevice alsaDevice = waitForAlsaDevice(addedCard, device, 3);
                if (alsaDevice != null) {
                    Bundle properties = new Bundle();
                    String manufacturer = usbDevice.getManufacturerName();
                    String product = usbDevice.getProductName();
                    String version = usbDevice.getVersion();
                    if (manufacturer == null || manufacturer.isEmpty()) {
                        name = product;
                    } else if (product == null || product.isEmpty()) {
                        name = manufacturer;
                    } else {
                        name = manufacturer + " " + product;
                    }
                    properties.putString("name", name);
                    properties.putString("manufacturer", manufacturer);
                    properties.putString("product", product);
                    properties.putString("version", version);
                    properties.putString("serial_number", usbDevice.getSerialNumber());
                    properties.putInt("alsa_card", alsaDevice.mCard);
                    properties.putInt("alsa_device", alsaDevice.mDevice);
                    properties.putParcelable("usb_device", usbDevice);
                    UsbMidiDevice usbMidiDevice = UsbMidiDevice.create(this.mContext, properties, alsaDevice.mCard, alsaDevice.mDevice);
                    if (usbMidiDevice != null) {
                        this.mMidiDevices.put(usbDevice, usbMidiDevice);
                    }
                }
            }
        }
        Slog.d(TAG, "deviceAdded() - done");
    }

    void usbDeviceRemoved(UsbDevice usbDevice) {
        Slog.d(TAG, "deviceRemoved(): " + usbDevice.getManufacturerName() + " " + usbDevice.getProductName());
        UsbAudioDevice audioDevice = this.mAudioDevices.remove(usbDevice);
        Slog.i(TAG, "USB Audio Device Removed: " + audioDevice);
        if (audioDevice != null && (audioDevice.mHasPlayback || audioDevice.mHasCapture)) {
            notifyDeviceState(audioDevice, false);
            selectDefaultDevice();
        }
        UsbMidiDevice usbMidiDevice = this.mMidiDevices.remove(usbDevice);
        if (usbMidiDevice == null) {
            return;
        }
        IoUtils.closeQuietly(usbMidiDevice);
    }

    void setAccessoryAudioState(boolean enabled, int card, int device) {
        Slog.d(TAG, "setAccessoryAudioState " + enabled + " " + card + " " + device);
        if (enabled) {
            this.mAccessoryAudioDevice = new UsbAudioDevice(card, device, true, false, 2);
            notifyDeviceState(this.mAccessoryAudioDevice, true);
        } else {
            if (this.mAccessoryAudioDevice == null) {
                return;
            }
            notifyDeviceState(this.mAccessoryAudioDevice, false);
            this.mAccessoryAudioDevice = null;
        }
    }

    void setPeripheralMidiState(boolean enabled, int card, int device) {
        if (!this.mHasMidiFeature) {
            return;
        }
        if (enabled && this.mPeripheralMidiDevice == null) {
            Bundle properties = new Bundle();
            Resources r = this.mContext.getResources();
            properties.putString("name", r.getString(R.string.lockscreen_storage_locked));
            properties.putString("manufacturer", r.getString(R.string.lockscreen_too_many_failed_attempts_countdown));
            properties.putString("product", r.getString(R.string.lockscreen_too_many_failed_attempts_dialog_message));
            properties.putInt("alsa_card", card);
            properties.putInt("alsa_device", device);
            this.mPeripheralMidiDevice = UsbMidiDevice.create(this.mContext, properties, card, device);
            return;
        }
        if (enabled || this.mPeripheralMidiDevice == null) {
            return;
        }
        IoUtils.closeQuietly(this.mPeripheralMidiDevice);
        this.mPeripheralMidiDevice = null;
    }

    public ArrayList<UsbAudioDevice> getConnectedDevices() {
        ArrayList<UsbAudioDevice> devices = new ArrayList<>(this.mAudioDevices.size());
        for (Map.Entry<UsbDevice, UsbAudioDevice> entry : this.mAudioDevices.entrySet()) {
            devices.add(entry.getValue());
        }
        return devices;
    }

    public void dump(IndentingPrintWriter pw) {
        pw.println("USB Audio Devices:");
        for (UsbDevice device : this.mAudioDevices.keySet()) {
            pw.println("  " + device.getDeviceName() + ": " + this.mAudioDevices.get(device));
        }
        pw.println("USB MIDI Devices:");
        for (UsbDevice device2 : this.mMidiDevices.keySet()) {
            pw.println("  " + device2.getDeviceName() + ": " + this.mMidiDevices.get(device2));
        }
    }

    public void logDevicesList(String title) {
        for (Map.Entry<UsbDevice, UsbAudioDevice> entry : this.mAudioDevices.entrySet()) {
            Slog.i(TAG, "UsbDevice-------------------");
            Slog.i(TAG, "" + (entry != null ? entry.getKey() : "[none]"));
            Slog.i(TAG, "UsbAudioDevice--------------");
            Slog.i(TAG, "" + entry.getValue());
        }
    }

    public void logDevices(String title) {
        Slog.i(TAG, title);
        for (Map.Entry<UsbDevice, UsbAudioDevice> entry : this.mAudioDevices.entrySet()) {
            Slog.i(TAG, entry.getValue().toShortString());
        }
    }
}
