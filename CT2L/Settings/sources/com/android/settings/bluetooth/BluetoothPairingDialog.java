package com.android.settings.bluetooth;

import android.app.NotificationManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.TextWatcher;
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
import java.util.Locale;

public final class BluetoothPairingDialog extends AlertActivity implements DialogInterface.OnClickListener, TextWatcher, CompoundButton.OnCheckedChangeListener {
    private LocalBluetoothManager mBluetoothManager;
    private CachedBluetoothDeviceManager mCachedDeviceManager;
    private BluetoothDevice mDevice;
    private Button mOkButton;
    private String mPairingKey;
    private EditText mPairingView;
    private PowerManager.WakeLock mPartialWakeLock;
    private boolean mRecRegistered = false;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.bluetooth.device.action.BOND_STATE_CHANGED".equals(action)) {
                int bondState = intent.getIntExtra("android.bluetooth.device.extra.BOND_STATE", Integer.MIN_VALUE);
                if (bondState == 12 || bondState == 10) {
                    BluetoothPairingDialog.this.dismiss();
                    return;
                }
                return;
            }
            if ("android.bluetooth.device.action.PAIRING_CANCEL".equals(action)) {
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                if (device == null || device.equals(BluetoothPairingDialog.this.mDevice)) {
                    BluetoothPairingDialog.this.dismiss();
                }
            }
        }
    };
    private PowerManager.WakeLock mScreenWakeLock;
    private int mType;
    private PowerManager.WakeLock mWakeLock;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        PowerManager pm = (PowerManager) getSystemService("power");
        this.mWakeLock = pm.newWakeLock(805306394, "BluetoothPairingDialog");
        this.mPartialWakeLock = pm.newWakeLock(1, "BluetoothPairingDialog");
        this.mScreenWakeLock = pm.newWakeLock(10, "BluetoothPairingDialog");
        this.mWakeLock.acquire();
        if (!intent.getAction().equals("android.bluetooth.device.action.PAIRING_REQUEST")) {
            Log.e("BluetoothPairingDialog", "Error: this activity may be started only with intent android.bluetooth.device.action.PAIRING_REQUEST");
            finish();
            return;
        }
        this.mBluetoothManager = LocalBluetoothManager.getInstance(this);
        if (this.mBluetoothManager == null) {
            Log.e("BluetoothPairingDialog", "Error: BluetoothAdapter not supported by system");
            finish();
            return;
        }
        this.mCachedDeviceManager = this.mBluetoothManager.getCachedDeviceManager();
        this.mDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
        this.mType = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_VARIANT", Integer.MIN_VALUE);
        if (this.mWakeLock.isHeld()) {
            this.mPartialWakeLock.acquire();
            this.mScreenWakeLock.acquire();
        }
        switch (this.mType) {
            case 0:
            case 1:
                createUserEntryDialog();
                break;
            case 2:
                int passkey = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", Integer.MIN_VALUE);
                if (passkey == Integer.MIN_VALUE) {
                    Log.e("BluetoothPairingDialog", "Invalid Confirmation Passkey received, not showing any dialog");
                    finish();
                    return;
                } else {
                    this.mPairingKey = String.format(Locale.US, "%06d", Integer.valueOf(passkey));
                    createConfirmationDialog();
                }
                break;
            case 3:
            case 6:
                createConsentDialog();
                break;
            case 4:
            case 5:
                int pairingKey = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", Integer.MIN_VALUE);
                if (pairingKey == Integer.MIN_VALUE) {
                    Log.e("BluetoothPairingDialog", "Invalid Confirmation Passkey or PIN received, not showing any dialog");
                    return;
                }
                if (this.mType == 4) {
                    this.mPairingKey = String.format("%06d", Integer.valueOf(pairingKey));
                } else {
                    this.mPairingKey = String.format("%04d", Integer.valueOf(pairingKey));
                }
                createDisplayPasskeyOrPinDialog();
                break;
                break;
            default:
                if (this.mScreenWakeLock.isHeld()) {
                    this.mScreenWakeLock.release();
                }
                Log.e("BluetoothPairingDialog", "Incorrect pairing type received, not showing any dialog");
                break;
        }
        this.mRecRegistered = true;
        registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.device.action.PAIRING_CANCEL"));
        registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.device.action.BOND_STATE_CHANGED"));
        registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.device.action.PAIRING_REQUEST"));
    }

    private void createUserEntryDialog() {
        AlertController.AlertParams p = this.mAlertParams;
        p.mTitle = getString(R.string.bluetooth_pairing_request);
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
        int messageId1;
        int messageId2;
        int maxLength;
        View view = getLayoutInflater().inflate(R.layout.bluetooth_pin_entry, (ViewGroup) null);
        TextView messageViewCaption = (TextView) view.findViewById(R.id.message_caption);
        TextView messageViewContent = (TextView) view.findViewById(R.id.message_subhead);
        TextView messageView2 = (TextView) view.findViewById(R.id.message_below_pin);
        CheckBox alphanumericPin = (CheckBox) view.findViewById(R.id.alphanumeric_pin);
        this.mPairingView = (EditText) view.findViewById(R.id.text);
        this.mPairingView.addTextChangedListener(this);
        alphanumericPin.setOnCheckedChangeListener(this);
        switch (this.mType) {
            case 0:
                messageId1 = R.string.bluetooth_enter_pin_msg;
                messageId2 = R.string.bluetooth_enter_pin_other_device;
                maxLength = 16;
                break;
            case 1:
                messageId1 = R.string.bluetooth_enter_pin_msg;
                messageId2 = R.string.bluetooth_enter_passkey_other_device;
                maxLength = 6;
                alphanumericPin.setVisibility(8);
                break;
            default:
                Log.e("BluetoothPairingDialog", "Incorrect pairing type for createPinEntryView: " + this.mType);
                return null;
        }
        messageViewCaption.setText(messageId1);
        messageViewContent.setText(this.mCachedDeviceManager.getName(this.mDevice));
        messageView2.setText(messageId2);
        this.mPairingView.setInputType(2);
        this.mPairingView.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
        return view;
    }

    private View createView() {
        String messageCaption;
        View view = getLayoutInflater().inflate(R.layout.bluetooth_pin_confirm, (ViewGroup) null);
        String name = Html.escapeHtml(this.mCachedDeviceManager.getName(this.mDevice));
        TextView messageViewCaption = (TextView) view.findViewById(R.id.message_caption);
        TextView messageViewContent = (TextView) view.findViewById(R.id.message_subhead);
        TextView pairingViewCaption = (TextView) view.findViewById(R.id.pairing_caption);
        TextView pairingViewContent = (TextView) view.findViewById(R.id.pairing_subhead);
        TextView messagePairing = (TextView) view.findViewById(R.id.pairing_code_message);
        String pairingContent = null;
        switch (this.mType) {
            case 2:
                messageCaption = getString(R.string.bluetooth_enter_pin_msg);
                pairingContent = this.mPairingKey;
                if (messageViewCaption != null) {
                    messageViewCaption.setText(messageCaption);
                    messageViewContent.setText(name);
                }
                if (pairingContent != null) {
                    pairingViewCaption.setVisibility(0);
                    pairingViewContent.setVisibility(0);
                    pairingViewContent.setText(pairingContent);
                    return view;
                }
                return view;
            case 3:
            case 6:
                messagePairing.setVisibility(0);
                messageCaption = getString(R.string.bluetooth_enter_pin_msg);
                if (messageViewCaption != null) {
                }
                if (pairingContent != null) {
                }
                break;
            case 4:
            case 5:
                messagePairing.setVisibility(0);
                messageCaption = getString(R.string.bluetooth_enter_pin_msg);
                pairingContent = this.mPairingKey;
                if (messageViewCaption != null) {
                }
                if (pairingContent != null) {
                }
                break;
            default:
                Log.e("BluetoothPairingDialog", "Incorrect pairing type received, not creating view");
                return null;
        }
    }

    private void createConfirmationDialog() {
        AlertController.AlertParams p = this.mAlertParams;
        p.mTitle = getString(R.string.bluetooth_pairing_request);
        p.mView = createView();
        p.mPositiveButtonText = getString(R.string.bluetooth_pairing_accept);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(R.string.bluetooth_pairing_decline);
        p.mNegativeButtonListener = this;
        setupAlert();
    }

    private void createConsentDialog() {
        AlertController.AlertParams p = this.mAlertParams;
        p.mTitle = getString(R.string.bluetooth_pairing_request);
        p.mView = createView();
        p.mPositiveButtonText = getString(R.string.bluetooth_pairing_accept);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(R.string.bluetooth_pairing_decline);
        p.mNegativeButtonListener = this;
        setupAlert();
    }

    private void createDisplayPasskeyOrPinDialog() {
        AlertController.AlertParams p = this.mAlertParams;
        p.mTitle = getString(R.string.bluetooth_pairing_request);
        p.mView = createView();
        p.mNegativeButtonText = getString(android.R.string.cancel);
        p.mNegativeButtonListener = this;
        setupAlert();
        if (this.mType == 4) {
            this.mDevice.setPairingConfirmation(true);
        } else if (this.mType == 5) {
            byte[] pinBytes = BluetoothDevice.convertPinToBytes(this.mPairingKey);
            this.mDevice.setPin(pinBytes);
        }
    }

    protected void onDestroy() {
        if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        if (this.mPartialWakeLock.isHeld()) {
            this.mPartialWakeLock.release();
        }
        if (this.mScreenWakeLock.isHeld()) {
            this.mScreenWakeLock.release();
        }
        super.onDestroy();
        if (this.mReceiver != null && this.mRecRegistered) {
            unregisterReceiver(this.mReceiver);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (this.mOkButton != null) {
            this.mOkButton.setEnabled(s.length() > 0);
        }
    }

    private void onPair(String value) {
        switch (this.mType) {
            case 0:
                byte[] pinBytes = BluetoothDevice.convertPinToBytes(value);
                if (pinBytes != null) {
                    this.mDevice.setPin(pinBytes);
                }
                break;
            case 1:
                int passkey = Integer.parseInt(value);
                this.mDevice.setPasskey(passkey);
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
                Log.e("BluetoothPairingDialog", "Incorrect pairing type received");
                break;
        }
    }

    private void onCancel() {
        destroyNotification();
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
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            this.mPairingView.setInputType(1);
        } else {
            this.mPairingView.setInputType(2);
        }
    }

    private void destroyNotification() {
        NotificationManager manager = (NotificationManager) getSystemService("notification");
        manager.cancel(android.R.drawable.stat_sys_data_bluetooth);
    }
}
