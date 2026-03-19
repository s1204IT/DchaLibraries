package com.android.server.usb;

import android.media.IAudioService;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.audio.AudioService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.pm.Settings;

public final class UsbAlsaDevice {
    protected static final boolean DEBUG = false;
    private static final String TAG = "UsbAlsaDevice";
    private IAudioService mAudioService;
    private final int mCardNum;
    private final String mDeviceAddress;
    private final int mDeviceNum;
    private final boolean mHasInput;
    private final boolean mHasOutput;
    private int mInputState;
    private final boolean mIsInputHeadset;
    private final boolean mIsOutputHeadset;
    private UsbAlsaJackDetector mJackDetector;
    private int mOutputState;
    private boolean mSelected = false;
    private String mDeviceName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    private String mDeviceDescription = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;

    public UsbAlsaDevice(IAudioService iAudioService, int i, int i2, String str, boolean z, boolean z2, boolean z3, boolean z4) {
        this.mAudioService = iAudioService;
        this.mCardNum = i;
        this.mDeviceNum = i2;
        this.mDeviceAddress = str;
        this.mHasOutput = z;
        this.mHasInput = z2;
        this.mIsInputHeadset = z3;
        this.mIsOutputHeadset = z4;
    }

    public int getCardNum() {
        return this.mCardNum;
    }

    public int getDeviceNum() {
        return this.mDeviceNum;
    }

    public String getDeviceAddress() {
        return this.mDeviceAddress;
    }

    public String getAlsaCardDeviceString() {
        if (this.mCardNum < 0 || this.mDeviceNum < 0) {
            Slog.e(TAG, "Invalid alsa card or device alsaCard: " + this.mCardNum + " alsaDevice: " + this.mDeviceNum);
            return null;
        }
        return AudioService.makeAlsaAddressString(this.mCardNum, this.mDeviceNum);
    }

    public boolean hasOutput() {
        return this.mHasOutput;
    }

    public boolean hasInput() {
        return this.mHasInput;
    }

    public boolean isInputHeadset() {
        return this.mIsInputHeadset;
    }

    public boolean isOutputHeadset() {
        return this.mIsOutputHeadset;
    }

    private synchronized boolean isInputJackConnected() {
        if (this.mJackDetector == null) {
            return true;
        }
        return this.mJackDetector.isInputJackConnected();
    }

    private synchronized boolean isOutputJackConnected() {
        if (this.mJackDetector == null) {
            return true;
        }
        return this.mJackDetector.isOutputJackConnected();
    }

    private synchronized void startJackDetect() {
        this.mJackDetector = UsbAlsaJackDetector.startJackDetect(this);
    }

    private synchronized void stopJackDetect() {
        if (this.mJackDetector != null) {
            this.mJackDetector.pleaseStop();
        }
        this.mJackDetector = null;
    }

    public synchronized void start() {
        this.mSelected = true;
        this.mInputState = 0;
        this.mOutputState = 0;
        startJackDetect();
        updateWiredDeviceConnectionState(true);
    }

    public synchronized void stop() {
        stopJackDetect();
        updateWiredDeviceConnectionState(false);
        this.mSelected = false;
    }

    public synchronized void updateWiredDeviceConnectionState(boolean z) {
        int i;
        if (!this.mSelected) {
            Slog.e(TAG, "updateWiredDeviceConnectionState on unselected AlsaDevice!");
            return;
        }
        String alsaCardDeviceString = getAlsaCardDeviceString();
        if (alsaCardDeviceString == null) {
            return;
        }
        try {
            if (this.mHasOutput) {
                if (this.mIsOutputHeadset) {
                    i = 67108864;
                } else {
                    i = 16384;
                }
                int i2 = i;
                boolean zIsOutputJackConnected = isOutputJackConnected();
                Slog.i(TAG, "OUTPUT JACK connected: " + zIsOutputJackConnected);
                int i3 = (z && zIsOutputJackConnected) ? 1 : 0;
                if (i3 != this.mOutputState) {
                    this.mOutputState = i3;
                    this.mAudioService.setWiredDeviceConnectionState(i2, i3, alsaCardDeviceString, this.mDeviceName, TAG);
                }
            }
            if (this.mHasInput) {
                int i4 = this.mIsInputHeadset ? -2113929216 : -2147479552;
                boolean zIsInputJackConnected = isInputJackConnected();
                Slog.i(TAG, "INPUT JACK connected: " + zIsInputJackConnected);
                int i5 = (z && zIsInputJackConnected) ? 1 : 0;
                if (i5 != this.mInputState) {
                    this.mInputState = i5;
                    this.mAudioService.setWiredDeviceConnectionState(i4, i5, alsaCardDeviceString, this.mDeviceName, TAG);
                }
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException in setWiredDeviceConnectionState");
        }
    }

    public synchronized String toString() {
        return "UsbAlsaDevice: [card: " + this.mCardNum + ", device: " + this.mDeviceNum + ", name: " + this.mDeviceName + ", hasOutput: " + this.mHasOutput + ", hasInput: " + this.mHasInput + "]";
    }

    public synchronized void dump(DualDumpOutputStream dualDumpOutputStream, String str, long j) {
        long jStart = dualDumpOutputStream.start(str, j);
        dualDumpOutputStream.write("card", 1120986464257L, this.mCardNum);
        dualDumpOutputStream.write("device", 1120986464258L, this.mDeviceNum);
        dualDumpOutputStream.write(Settings.ATTR_NAME, 1138166333443L, this.mDeviceName);
        dualDumpOutputStream.write("has_output", 1133871366148L, this.mHasOutput);
        dualDumpOutputStream.write("has_input", 1133871366149L, this.mHasInput);
        dualDumpOutputStream.write(AudioService.CONNECT_INTENT_KEY_ADDRESS, 1138166333446L, this.mDeviceAddress);
        dualDumpOutputStream.end(jStart);
    }

    synchronized String toShortString() {
        return "[card:" + this.mCardNum + " device:" + this.mDeviceNum + " " + this.mDeviceName + "]";
    }

    synchronized String getDeviceName() {
        return this.mDeviceName;
    }

    synchronized void setDeviceNameAndDescription(String str, String str2) {
        this.mDeviceName = str;
        this.mDeviceDescription = str2;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof UsbAlsaDevice)) {
            return false;
        }
        UsbAlsaDevice usbAlsaDevice = (UsbAlsaDevice) obj;
        return this.mCardNum == usbAlsaDevice.mCardNum && this.mDeviceNum == usbAlsaDevice.mDeviceNum && this.mHasOutput == usbAlsaDevice.mHasOutput && this.mHasInput == usbAlsaDevice.mHasInput && this.mIsInputHeadset == usbAlsaDevice.mIsInputHeadset && this.mIsOutputHeadset == usbAlsaDevice.mIsOutputHeadset;
    }

    public int hashCode() {
        return (31 * (((((((((this.mCardNum + 31) * 31) + this.mDeviceNum) * 31) + (!this.mHasOutput ? 1 : 0)) * 31) + (!this.mHasInput ? 1 : 0)) * 31) + (!this.mIsInputHeadset ? 1 : 0))) + (!this.mIsOutputHeadset ? 1 : 0);
    }
}
