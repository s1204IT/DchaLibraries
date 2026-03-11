package com.android.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.EventLog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BluetoothPairingDialog extends AlertActivity implements CompoundButton.OnCheckedChangeListener, DialogInterface.OnClickListener, TextWatcher {
    private static boolean sReceiverRegistered = false;
    private LocalBluetoothManager mBluetoothManager;
    private CachedBluetoothDeviceManager mCachedDeviceManager;
    private BluetoothDevice mDevice;
    private Button mOkButton;
    private String mPairingKey;
    private EditText mPairingView;
    private LocalBluetoothProfile mPbapClientProfile;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.bluetooth.device.action.BOND_STATE_CHANGED".equals(action)) {
                int bondState = intent.getIntExtra("android.bluetooth.device.extra.BOND_STATE", Integer.MIN_VALUE);
                if (bondState != 12 && bondState != 10) {
                    return;
                }
                BluetoothPairingDialog.this.dismiss();
                return;
            }
            if (!"android.bluetooth.device.action.PAIRING_CANCEL".equals(action)) {
                return;
            }
            BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            if (device != null && !device.equals(BluetoothPairingDialog.this.mDevice)) {
                return;
            }
            BluetoothPairingDialog.this.dismiss();
        }
    };
    private int mType;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (!intent.getAction().equals("android.bluetooth.device.action.PAIRING_REQUEST")) {
            Log.e("BluetoothPairingDialog", "Error: this activity may be started only with intent android.bluetooth.device.action.PAIRING_REQUEST");
            finish();
            return;
        }
        this.mBluetoothManager = Utils.getLocalBtManager(this);
        if (this.mBluetoothManager == null) {
            Log.e("BluetoothPairingDialog", "Error: BluetoothAdapter not supported by system");
            finish();
            return;
        }
        this.mCachedDeviceManager = this.mBluetoothManager.getCachedDeviceManager();
        this.mPbapClientProfile = this.mBluetoothManager.getProfileManager().getPbapClientProfile();
        this.mDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
        this.mType = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_VARIANT", Integer.MIN_VALUE);
        switch (this.mType) {
            case DefaultWfcSettingsExt.RESUME:
            case DefaultWfcSettingsExt.PAUSE:
            case 7:
                createUserEntryDialog();
                break;
            case DefaultWfcSettingsExt.CREATE:
                int passkey = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", Integer.MIN_VALUE);
                if (passkey == Integer.MIN_VALUE) {
                    Log.e("BluetoothPairingDialog", "Invalid Confirmation Passkey received, not showing any dialog");
                    return;
                } else {
                    this.mPairingKey = String.format(Locale.US, "%06d", Integer.valueOf(passkey));
                    createConfirmationDialog();
                }
                break;
            case DefaultWfcSettingsExt.DESTROY:
            case 6:
                createConsentDialog();
                break;
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
            case 5:
                int pairingKey = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", Integer.MIN_VALUE);
                if (pairingKey == Integer.MIN_VALUE) {
                    Log.e("BluetoothPairingDialog", "Invalid Confirmation Passkey or PIN received, not showing any dialog");
                    return;
                }
                if (this.mType == 4) {
                    this.mPairingKey = String.format(Locale.US, "%06d", Integer.valueOf(pairingKey));
                } else {
                    this.mPairingKey = String.format(Locale.US, "%04d", Integer.valueOf(pairingKey));
                }
                createDisplayPasskeyOrPinDialog();
                break;
                break;
            default:
                Log.e("BluetoothPairingDialog", "Incorrect pairing type received, not showing any dialog");
                break;
        }
        registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.device.action.PAIRING_CANCEL"));
        registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.device.action.BOND_STATE_CHANGED"));
        sReceiverRegistered = true;
    }

    private void createUserEntryDialog() {
        AlertController.AlertParams p = this.mAlertParams;
        p.mTitle = getString(R.string.bluetooth_pairing_request, new Object[]{this.mCachedDeviceManager.getName(this.mDevice)});
        p.mView = createPinEntryView();
        p.mPositiveButtonText = getString(android.R.string.ok);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(android.R.string.cancel);
        p.mNegativeButtonListener = this;
        setupAlert();
        this.mOkButton = this.mAlert.getButton(-1);
        this.mOkButton.setEnabled(false);
    }

    private View createPinEntryView() {
        int messageId;
        int maxLength;
        View view = getLayoutInflater().inflate(R.layout.bluetooth_pin_entry, (ViewGroup) null);
        TextView messageViewCaptionHint = (TextView) view.findViewById(R.id.pin_values_hint);
        TextView messageView2 = (TextView) view.findViewById(R.id.message_below_pin);
        CheckBox alphanumericPin = (CheckBox) view.findViewById(R.id.alphanumeric_pin);
        CheckBox contactSharing = (CheckBox) view.findViewById(R.id.phonebook_sharing_message_entry_pin);
        contactSharing.setText(getString(R.string.bluetooth_pairing_shares_phonebook, new Object[]{this.mCachedDeviceManager.getName(this.mDevice)}));
        if (this.mPbapClientProfile != null && this.mPbapClientProfile.isProfileReady()) {
            contactSharing.setVisibility(8);
        }
        if (this.mDevice.getPhonebookAccessPermission() == 1) {
            contactSharing.setChecked(true);
        } else if (this.mDevice.getPhonebookAccessPermission() == 2) {
            contactSharing.setChecked(false);
        } else if (this.mDevice.getBluetoothClass().getDeviceClass() == 1032) {
            contactSharing.setChecked(false);
            this.mDevice.setPhonebookAccessPermission(2);
            EventLog.writeEvent(1397638484, "73173182", -1, "");
        } else {
            contactSharing.setChecked(false);
            this.mDevice.setPhonebookAccessPermission(2);
        }
        contactSharing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    BluetoothPairingDialog.this.mDevice.setPhonebookAccessPermission(1);
                } else {
                    BluetoothPairingDialog.this.mDevice.setPhonebookAccessPermission(2);
                }
            }
        });
        this.mPairingView = (EditText) view.findViewById(R.id.text);
        this.mPairingView.addTextChangedListener(this);
        alphanumericPin.setOnCheckedChangeListener(this);
        int messageIdHint = R.string.bluetooth_pin_values_hint;
        switch (this.mType) {
            case DefaultWfcSettingsExt.RESUME:
                messageId = R.string.bluetooth_enter_pin_other_device;
                maxLength = 16;
                messageViewCaptionHint.setText(messageIdHint);
                messageView2.setText(messageId);
                this.mPairingView.setInputType(2);
                this.mPairingView.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
                return view;
            case DefaultWfcSettingsExt.PAUSE:
                messageId = R.string.bluetooth_enter_passkey_other_device;
                maxLength = 6;
                alphanumericPin.setVisibility(8);
                messageViewCaptionHint.setText(messageIdHint);
                messageView2.setText(messageId);
                this.mPairingView.setInputType(2);
                this.mPairingView.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
                return view;
            case 7:
                messageIdHint = R.string.bluetooth_pin_values_hint_16_digits;
                messageId = R.string.bluetooth_enter_pin_other_device;
                maxLength = 16;
                messageViewCaptionHint.setText(messageIdHint);
                messageView2.setText(messageId);
                this.mPairingView.setInputType(2);
                this.mPairingView.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
                return view;
            default:
                Log.e("BluetoothPairingDialog", "Incorrect pairing type for createPinEntryView: " + this.mType);
                return null;
        }
    }

    private View createView() {
        View view = getLayoutInflater().inflate(R.layout.bluetooth_pin_confirm, (ViewGroup) null);
        TextView pairingViewCaption = (TextView) view.findViewById(R.id.pairing_caption);
        TextView pairingViewContent = (TextView) view.findViewById(R.id.pairing_subhead);
        TextView messagePairing = (TextView) view.findViewById(R.id.pairing_code_message);
        CheckBox contactSharing = (CheckBox) view.findViewById(R.id.phonebook_sharing_message_confirm_pin);
        contactSharing.setText(getString(R.string.bluetooth_pairing_shares_phonebook, new Object[]{this.mCachedDeviceManager.getName(this.mDevice)}));
        if (this.mPbapClientProfile != null && this.mPbapClientProfile.isProfileReady()) {
            contactSharing.setVisibility(8);
        }
        if (this.mDevice.getPhonebookAccessPermission() == 1) {
            contactSharing.setChecked(true);
        } else if (this.mDevice.getPhonebookAccessPermission() == 2) {
            contactSharing.setChecked(false);
        } else if (this.mDevice.getBluetoothClass().getDeviceClass() == 1032) {
            contactSharing.setChecked(true);
            this.mDevice.setPhonebookAccessPermission(1);
        } else {
            contactSharing.setChecked(false);
            this.mDevice.setPhonebookAccessPermission(2);
        }
        contactSharing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    BluetoothPairingDialog.this.mDevice.setPhonebookAccessPermission(1);
                } else {
                    BluetoothPairingDialog.this.mDevice.setPhonebookAccessPermission(2);
                }
            }
        });
        String pairingContent = null;
        switch (this.mType) {
            case DefaultWfcSettingsExt.CREATE:
                pairingContent = this.mPairingKey;
                if (pairingContent != null) {
                    pairingViewCaption.setVisibility(0);
                    pairingViewContent.setVisibility(0);
                    pairingViewContent.setText(pairingContent);
                }
                return view;
            case DefaultWfcSettingsExt.DESTROY:
            case 6:
                messagePairing.setVisibility(0);
                if (pairingContent != null) {
                }
                return view;
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
            case 5:
                messagePairing.setVisibility(0);
                pairingContent = this.mPairingKey;
                if (pairingContent != null) {
                }
                return view;
            default:
                Log.e("BluetoothPairingDialog", "Incorrect pairing type received, not creating view");
                return null;
        }
    }

    private void createConfirmationDialog() {
        AlertController.AlertParams p = this.mAlertParams;
        p.mTitle = getString(R.string.bluetooth_pairing_request, new Object[]{this.mCachedDeviceManager.getName(this.mDevice)});
        p.mView = createView();
        p.mPositiveButtonText = getString(R.string.bluetooth_pairing_accept);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(R.string.bluetooth_pairing_decline);
        p.mNegativeButtonListener = this;
        setupAlert();
    }

    private void createConsentDialog() {
        AlertController.AlertParams p = this.mAlertParams;
        p.mTitle = getString(R.string.bluetooth_pairing_request, new Object[]{this.mCachedDeviceManager.getName(this.mDevice)});
        p.mView = createView();
        p.mPositiveButtonText = getString(R.string.bluetooth_pairing_accept);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(R.string.bluetooth_pairing_decline);
        p.mNegativeButtonListener = this;
        setupAlert();
    }

    private void createDisplayPasskeyOrPinDialog() {
        AlertController.AlertParams p = this.mAlertParams;
        p.mTitle = getString(R.string.bluetooth_pairing_request, new Object[]{this.mCachedDeviceManager.getName(this.mDevice)});
        p.mView = createView();
        p.mNegativeButtonText = getString(android.R.string.cancel);
        p.mNegativeButtonListener = this;
        setupAlert();
        if (this.mType == 4) {
            this.mDevice.setPairingConfirmation(true);
        } else {
            if (this.mType != 5) {
                return;
            }
            byte[] pinBytes = BluetoothDevice.convertPinToBytes(this.mPairingKey);
            this.mDevice.setPin(pinBytes);
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        if (!sReceiverRegistered) {
            return;
        }
        unregisterReceiver(this.mReceiver);
        sReceiverRegistered = false;
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (this.mOkButton == null) {
            return;
        }
        if (this.mType == 7) {
            this.mOkButton.setEnabled(s.length() >= 16);
        } else {
            this.mOkButton.setEnabled(s.length() > 0);
        }
    }

    private void onPair(String value) {
        switch (this.mType) {
            case DefaultWfcSettingsExt.RESUME:
            case 7:
                byte[] pinBytes = BluetoothDevice.convertPinToBytes(value);
                if (pinBytes != null) {
                    this.mDevice.setPin(pinBytes);
                    break;
                }
                break;
            case DefaultWfcSettingsExt.PAUSE:
                int passkey = Integer.parseInt(value);
                this.mDevice.setPasskey(passkey);
                break;
            case DefaultWfcSettingsExt.CREATE:
            case DefaultWfcSettingsExt.DESTROY:
                this.mDevice.setPairingConfirmation(true);
                break;
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
            case 5:
                break;
            case 6:
                this.mDevice.setRemoteOutOfBandData();
                break;
            default:
                Log.e("BluetoothPairingDialog", "Incorrect pairing type received");
                break;
        }
    }

    private void onCancel() {
        this.mDevice.cancelPairingUserInput();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 4) {
            onCancel();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case -1:
                if (this.mPairingView != null) {
                    onPair(this.mPairingView.getText().toString());
                } else {
                    onPair(null);
                }
                break;
            default:
                onCancel();
                break;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (this.mPairingView.getInputType() == 2) {
            return;
        }
        String pinCodeStr = s.toString();
        Log.d("BluetoothPairingDialog", "onTextChanged " + pinCodeStr);
        String str = stringFilter(pinCodeStr);
        if (pinCodeStr.equals(str)) {
            return;
        }
        this.mPairingView.setText(str);
        this.mPairingView.setSelection(str.length());
    }

    private String stringFilter(String text) {
        Pattern pattern = Pattern.compile("[^\\x20-\\x7e]");
        Matcher matcher = pattern.matcher(text);
        return matcher.replaceAll("").trim();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            this.mPairingView.setInputType(1);
        } else {
            this.mPairingView.setInputType(2);
        }
    }

    public void onBackPressed() {
        try {
            super.onBackPressed();
        } catch (IllegalStateException e) {
            Log.e("BluetoothPairingDialog", e.getMessage());
        }
    }
}
