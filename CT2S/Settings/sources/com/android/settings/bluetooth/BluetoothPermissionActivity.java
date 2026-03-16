package com.android.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.Preference;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;

public class BluetoothPermissionActivity extends AlertActivity implements DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener {
    private BluetoothDevice mDevice;
    private Button mOkButton;
    private PowerManager.WakeLock mPartialWakeLock;
    private PowerManager.WakeLock mScreenWakeLock;
    private View mView;
    private PowerManager.WakeLock mWakeLock;
    private TextView messageView;
    private String mReturnPackage = null;
    private String mReturnClass = null;
    private int mRequestType = 0;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.bluetooth.device.action.CONNECTION_ACCESS_CANCEL")) {
                int requestType = intent.getIntExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 2);
                if (requestType == BluetoothPermissionActivity.this.mRequestType) {
                    BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    if (BluetoothPermissionActivity.this.mDevice.equals(device)) {
                        BluetoothPermissionActivity.this.dismissDialog();
                    }
                }
            }
        }
    };
    private boolean mReceiverRegistered = false;

    private void dismissDialog() {
        dismiss();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PowerManager pm = (PowerManager) getSystemService("power");
        this.mWakeLock = pm.newWakeLock(805306394, "BluetoothPermissionActivity");
        this.mPartialWakeLock = pm.newWakeLock(1, "BluetoothPermissionActivity");
        this.mScreenWakeLock = pm.newWakeLock(10, "BluetoothPermissionActivity");
        this.mWakeLock.acquire();
        Intent i = getIntent();
        String action = i.getAction();
        if (!action.equals("android.bluetooth.device.action.CONNECTION_ACCESS_REQUEST")) {
            Log.e("BluetoothPermissionActivity", "Error: this activity may be started only with intent ACTION_CONNECTION_ACCESS_REQUEST");
            finish();
            return;
        }
        if (this.mWakeLock.isHeld()) {
            this.mPartialWakeLock.acquire();
            this.mScreenWakeLock.acquire();
        }
        this.mDevice = (BluetoothDevice) i.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
        this.mReturnPackage = i.getStringExtra("android.bluetooth.device.extra.PACKAGE_NAME");
        this.mReturnClass = i.getStringExtra("android.bluetooth.device.extra.CLASS_NAME");
        this.mRequestType = i.getIntExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 2);
        Log.i("BluetoothPermissionActivity", "onCreate() Request type: " + this.mRequestType);
        if (this.mRequestType == 1) {
            showDialog(getString(R.string.bluetooth_connection_permission_request), this.mRequestType);
        } else if (this.mRequestType == 2) {
            showDialog(getString(R.string.bluetooth_phonebook_request), this.mRequestType);
        } else if (this.mRequestType == 3) {
            showDialog(getString(R.string.bluetooth_map_request), this.mRequestType);
        } else {
            Log.e("BluetoothPermissionActivity", "Error: bad request type: " + this.mRequestType);
            finish();
            return;
        }
        registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.device.action.CONNECTION_ACCESS_CANCEL"));
        this.mReceiverRegistered = true;
    }

    private void showDialog(String title, int requestType) {
        AlertController.AlertParams p = this.mAlertParams;
        p.mTitle = title;
        Log.i("BluetoothPermissionActivity", "showDialog() Request type: " + this.mRequestType + " this: " + this);
        switch (requestType) {
            case 1:
                p.mView = createConnectionDialogView();
                break;
            case 2:
                p.mView = createPhonebookDialogView();
                break;
            case 3:
                p.mView = createMapDialogView();
                break;
        }
        p.mPositiveButtonText = getString(R.string.yes);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(R.string.no);
        p.mNegativeButtonListener = this;
        this.mOkButton = this.mAlert.getButton(-1);
        setupAlert();
    }

    public void onBackPressed() {
        Log.i("BluetoothPermissionActivity", "Back button pressed! ignoring");
    }

    private String createRemoteName() {
        String mRemoteName = this.mDevice != null ? this.mDevice.getAliasName() : null;
        if (mRemoteName == null) {
            return getString(R.string.unknown);
        }
        return mRemoteName;
    }

    private View createConnectionDialogView() {
        String mRemoteName = createRemoteName();
        this.mView = getLayoutInflater().inflate(R.layout.bluetooth_access, (ViewGroup) null);
        this.messageView = (TextView) this.mView.findViewById(R.id.message);
        this.messageView.setText(getString(R.string.bluetooth_connection_dialog_text, new Object[]{mRemoteName}));
        return this.mView;
    }

    private View createPhonebookDialogView() {
        String mRemoteName = createRemoteName();
        this.mView = getLayoutInflater().inflate(R.layout.bluetooth_access, (ViewGroup) null);
        this.messageView = (TextView) this.mView.findViewById(R.id.message);
        this.messageView.setText(getString(R.string.bluetooth_pb_acceptance_dialog_text, new Object[]{mRemoteName, mRemoteName}));
        return this.mView;
    }

    private View createMapDialogView() {
        String mRemoteName = createRemoteName();
        this.mView = getLayoutInflater().inflate(R.layout.bluetooth_access, (ViewGroup) null);
        this.messageView = (TextView) this.mView.findViewById(R.id.message);
        this.messageView.setText(getString(R.string.bluetooth_map_acceptance_dialog_text, new Object[]{mRemoteName, mRemoteName}));
        return this.mView;
    }

    private void onPositive() {
        Log.d("BluetoothPermissionActivity", "onPositive");
        sendReplyIntentToReceiver(true, true);
        finish();
    }

    private void onNegative() {
        Log.d("BluetoothPermissionActivity", "onNegative");
        boolean always = true;
        if (this.mRequestType == 3) {
            LocalBluetoothManager bluetoothManager = LocalBluetoothManager.getInstance(this);
            CachedBluetoothDeviceManager cachedDeviceManager = bluetoothManager.getCachedDeviceManager();
            CachedBluetoothDevice cachedDevice = cachedDeviceManager.findDevice(this.mDevice);
            if (cachedDevice == null) {
                cachedDevice = cachedDeviceManager.addDevice(bluetoothManager.getBluetoothAdapter(), bluetoothManager.getProfileManager(), this.mDevice);
            }
            always = cachedDevice.checkAndIncreaseMessageRejectionCount();
        }
        sendReplyIntentToReceiver(false, always);
    }

    private void sendReplyIntentToReceiver(boolean allowed, boolean always) {
        Intent intent = new Intent("android.bluetooth.device.action.CONNECTION_ACCESS_REPLY");
        if (this.mReturnPackage != null && this.mReturnClass != null) {
            intent.setClassName(this.mReturnPackage, this.mReturnClass);
        }
        Log.i("BluetoothPermissionActivity", "sendReplyIntentToReceiver() Request type: " + this.mRequestType + " mReturnPackage" + this.mReturnPackage + " mReturnClass" + this.mReturnClass);
        intent.putExtra("android.bluetooth.device.extra.CONNECTION_ACCESS_RESULT", allowed ? 1 : 2);
        intent.putExtra("android.bluetooth.device.extra.ALWAYS_ALLOWED", always);
        intent.putExtra("android.bluetooth.device.extra.DEVICE", this.mDevice);
        intent.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", this.mRequestType);
        sendBroadcast(intent, "android.permission.BLUETOOTH_ADMIN");
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case -2:
                onNegative();
                break;
            case -1:
                onPositive();
                break;
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        if (this.mPartialWakeLock.isHeld()) {
            this.mPartialWakeLock.release();
        }
        if (this.mScreenWakeLock.isHeld()) {
            this.mScreenWakeLock.release();
        }
        if (this.mReceiverRegistered) {
            unregisterReceiver(this.mReceiver);
            this.mReceiverRegistered = false;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }
}
