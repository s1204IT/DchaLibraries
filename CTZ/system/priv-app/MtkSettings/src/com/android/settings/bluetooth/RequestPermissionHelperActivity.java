package com.android.settings.bluetooth;

import android.app.admin.DevicePolicyManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.util.Log;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

/* loaded from: classes.dex */
public class RequestPermissionHelperActivity extends AlertActivity implements DialogInterface.OnClickListener {
    private CharSequence mAppLabel;
    private LocalBluetoothAdapter mLocalAdapter;
    private int mRequest;
    private int mTimeout = -1;

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setResult(0);
        if (!parseIntent()) {
            finish();
            return;
        }
        if (getResources().getBoolean(R.bool.auto_confirm_bluetooth_activation_dialog)) {
            onClick(null, -1);
            dismiss();
        }
        createDialog();
    }

    void createDialog() {
        String string;
        String string2;
        String string3;
        String string4;
        AlertController.AlertParams alertParams = this.mAlertParams;
        int i = this.mRequest;
        if (i != 1) {
            if (i == 3) {
                if (this.mAppLabel != null) {
                    string4 = getString(R.string.bluetooth_ask_disablement, new Object[]{this.mAppLabel});
                } else {
                    string4 = getString(R.string.bluetooth_ask_disablement_no_name);
                }
                alertParams.mMessage = string4;
            }
        } else if (this.mTimeout < 0) {
            if (this.mAppLabel != null) {
                string3 = getString(R.string.bluetooth_ask_enablement, new Object[]{this.mAppLabel});
            } else {
                string3 = getString(R.string.bluetooth_ask_enablement_no_name);
            }
            alertParams.mMessage = string3;
        } else if (this.mTimeout == 0) {
            if (this.mAppLabel != null) {
                string2 = getString(R.string.bluetooth_ask_enablement_and_lasting_discovery, new Object[]{this.mAppLabel});
            } else {
                string2 = getString(R.string.bluetooth_ask_enablement_and_lasting_discovery_no_name);
            }
            alertParams.mMessage = string2;
        } else {
            if (this.mAppLabel != null) {
                string = getString(R.string.bluetooth_ask_enablement_and_discovery, new Object[]{this.mAppLabel, Integer.valueOf(this.mTimeout)});
            } else {
                string = getString(R.string.bluetooth_ask_enablement_and_discovery_no_name, new Object[]{Integer.valueOf(this.mTimeout)});
            }
            alertParams.mMessage = string;
        }
        alertParams.mPositiveButtonText = getString(R.string.allow);
        alertParams.mPositiveButtonListener = this;
        alertParams.mNegativeButtonText = getString(R.string.deny);
        setupAlert();
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (this.mRequest) {
            case 1:
            case 2:
                if (((UserManager) getSystemService(UserManager.class)).hasUserRestriction("no_bluetooth")) {
                    Intent intentCreateAdminSupportIntent = ((DevicePolicyManager) getSystemService(DevicePolicyManager.class)).createAdminSupportIntent("no_bluetooth");
                    if (intentCreateAdminSupportIntent != null) {
                        startActivity(intentCreateAdminSupportIntent);
                        break;
                    }
                } else {
                    this.mLocalAdapter.enable();
                    setResult(-1);
                    break;
                }
                break;
            case 3:
                this.mLocalAdapter.disable();
                setResult(-1);
                break;
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r5v0, resolved type: com.android.settings.bluetooth.RequestPermissionHelperActivity */
    /* JADX WARN: Multi-variable type inference failed */
    private boolean parseIntent() {
        Intent intent = getIntent();
        if (intent == null) {
            return false;
        }
        String action = intent.getAction();
        if ("com.android.settings.bluetooth.ACTION_INTERNAL_REQUEST_BT_ON".equals(action)) {
            this.mRequest = 1;
            if (intent.hasExtra("android.bluetooth.adapter.extra.DISCOVERABLE_DURATION")) {
                this.mTimeout = intent.getIntExtra("android.bluetooth.adapter.extra.DISCOVERABLE_DURATION", 120);
            }
        } else {
            if (!"com.android.settings.bluetooth.ACTION_INTERNAL_REQUEST_BT_OFF".equals(action)) {
                return false;
            }
            this.mRequest = 3;
        }
        LocalBluetoothManager localBtManager = Utils.getLocalBtManager(this);
        if (localBtManager == null) {
            Log.e("RequestPermissionHelperActivity", "Error: there's a problem starting Bluetooth");
            return false;
        }
        this.mAppLabel = getIntent().getCharSequenceExtra("com.android.settings.bluetooth.extra.APP_LABEL");
        this.mLocalAdapter = localBtManager.getBluetoothAdapter();
        return true;
    }
}
