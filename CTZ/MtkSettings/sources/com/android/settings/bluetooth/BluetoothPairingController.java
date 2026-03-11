package com.android.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.util.Log;
import android.widget.CompoundButton;
import com.android.settings.R;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import com.android.settingslib.wifi.AccessPoint;
import java.util.Locale;

public class BluetoothPairingController implements CompoundButton.OnCheckedChangeListener {
    private LocalBluetoothManager mBluetoothManager;
    private BluetoothDevice mDevice;
    private String mDeviceName;
    private int mPasskey;
    private String mPasskeyFormatted;
    private boolean mPbapAllowed;
    private LocalBluetoothProfile mPbapClientProfile;
    private int mType;
    private String mUserInput;

    public BluetoothPairingController(Intent intent, Context context) {
        this.mBluetoothManager = Utils.getLocalBtManager(context);
        this.mDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
        if (this.mBluetoothManager == null) {
            throw new IllegalStateException("Could not obtain LocalBluetoothManager");
        }
        if (this.mDevice == null) {
            throw new IllegalStateException("Could not find BluetoothDevice");
        }
        this.mType = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_VARIANT", AccessPoint.UNREACHABLE_RSSI);
        this.mPasskey = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", AccessPoint.UNREACHABLE_RSSI);
        this.mDeviceName = this.mBluetoothManager.getCachedDeviceManager().getName(this.mDevice);
        this.mPbapClientProfile = this.mBluetoothManager.getProfileManager().getPbapClientProfile();
        this.mPasskeyFormatted = formatKey(this.mPasskey);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
        if (z) {
            this.mPbapAllowed = true;
        } else {
            this.mPbapAllowed = false;
        }
    }

    public void onDialogPositiveClick(BluetoothPairingDialogFragment bluetoothPairingDialogFragment) {
        if (getDialogType() == 0) {
            if (this.mPbapAllowed) {
                this.mDevice.setPhonebookAccessPermission(1);
            } else {
                this.mDevice.setPhonebookAccessPermission(2);
            }
            onPair(this.mUserInput);
            return;
        }
        onPair(null);
    }

    public void onDialogNegativeClick(BluetoothPairingDialogFragment bluetoothPairingDialogFragment) {
        this.mDevice.setPhonebookAccessPermission(2);
        onCancel();
    }

    public int getDialogType() {
        switch (this.mType) {
            case 0:
            case 1:
            case 7:
                return 0;
            case 2:
            case 3:
            case 6:
                return 1;
            case 4:
            case 5:
                return 2;
            default:
                return -1;
        }
    }

    public String getDeviceName() {
        return this.mDeviceName;
    }

    public boolean isProfileReady() {
        return this.mPbapClientProfile != null && this.mPbapClientProfile.isProfileReady();
    }

    public boolean getContactSharingState() {
        switch (this.mDevice.getPhonebookAccessPermission()) {
            case 1:
                return true;
            case 2:
                return false;
            default:
                return this.mDevice.getBluetoothClass().getDeviceClass() == 1032;
        }
    }

    public void setContactSharingState() {
        if (this.mDevice.getPhonebookAccessPermission() != 1 && this.mDevice.getPhonebookAccessPermission() != 2) {
            if (this.mDevice.getBluetoothClass().getDeviceClass() == 1032) {
                onCheckedChanged(null, true);
            } else {
                onCheckedChanged(null, false);
            }
        }
    }

    public boolean isPasskeyValid(Editable editable) {
        boolean z = this.mType == 7;
        return (editable.length() >= 16 && z) || (editable.length() > 0 && !z);
    }

    public int getDeviceVariantMessageId() {
        int i = this.mType;
        if (i != 7) {
            switch (i) {
                case 0:
                    return R.string.bluetooth_enter_pin_other_device;
                case 1:
                    return R.string.bluetooth_enter_passkey_other_device;
                default:
                    return -1;
            }
        }
        return R.string.bluetooth_enter_pin_other_device;
    }

    public int getDeviceVariantMessageHintId() {
        int i = this.mType;
        if (i == 7) {
            return R.string.bluetooth_pin_values_hint_16_digits;
        }
        switch (i) {
            case 0:
            case 1:
                return R.string.bluetooth_pin_values_hint;
            default:
                return -1;
        }
    }

    public int getDeviceMaxPasskeyLength() {
        int i = this.mType;
        if (i != 7) {
            switch (i) {
                case 0:
                    return 16;
                case 1:
                    return 6;
                default:
                    return 0;
            }
        }
        return 16;
    }

    public boolean pairingCodeIsAlphanumeric() {
        return this.mType != 1;
    }

    protected void notifyDialogDisplayed() {
        if (this.mType == 4) {
            this.mDevice.setPairingConfirmation(true);
        } else if (this.mType == 5) {
            this.mDevice.setPin(BluetoothDevice.convertPinToBytes(this.mPasskeyFormatted));
        }
    }

    public boolean isDisplayPairingKeyVariant() {
        switch (this.mType) {
            case 4:
            case 5:
            case 6:
                return true;
            default:
                return false;
        }
    }

    public boolean hasPairingContent() {
        int i = this.mType;
        if (i != 2) {
            switch (i) {
                case 4:
                case 5:
                    return true;
                default:
                    return false;
            }
        }
        return true;
    }

    public String getPairingContent() {
        if (hasPairingContent()) {
            return this.mPasskeyFormatted;
        }
        return null;
    }

    protected void updateUserInput(String str) {
        this.mUserInput = str;
    }

    private String formatKey(int i) {
        int i2 = this.mType;
        if (i2 != 2) {
            switch (i2) {
                case 4:
                    break;
                case 5:
                    return String.format("%04d", Integer.valueOf(i));
                default:
                    return null;
            }
        }
        return String.format(Locale.US, "%06d", Integer.valueOf(i));
    }

    private void onPair(String str) {
        Log.d("BTPairingController", "Pairing dialog accepted");
        switch (this.mType) {
            case 0:
            case 7:
                byte[] bArrConvertPinToBytes = BluetoothDevice.convertPinToBytes(str);
                if (bArrConvertPinToBytes != null) {
                    this.mDevice.setPin(bArrConvertPinToBytes);
                    break;
                }
                break;
            case 1:
                this.mDevice.setPasskey(Integer.parseInt(str));
                break;
            case 2:
            case 3:
                this.mDevice.setPairingConfirmation(true);
                break;
            case 4:
            case 5:
                break;
            case 6:
                this.mDevice.setRemoteOutOfBandData();
                break;
            default:
                Log.e("BTPairingController", "Incorrect pairing type received");
                break;
        }
    }

    public void onCancel() {
        Log.d("BTPairingController", "Pairing dialog canceled");
        this.mDevice.cancelPairingUserInput();
    }

    public boolean deviceEquals(BluetoothDevice bluetoothDevice) {
        return this.mDevice == bluetoothDevice;
    }
}
