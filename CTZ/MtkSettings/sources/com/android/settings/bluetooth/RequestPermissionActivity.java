package com.android.settings.bluetooth;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.bluetooth.RequestPermissionActivity;
import com.android.settingslib.bluetooth.BluetoothDiscoverableTimeoutReceiver;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.wifi.AccessPoint;

public class RequestPermissionActivity extends Activity implements DialogInterface.OnClickListener {
    private static int mRequestCode = 1;
    private CharSequence mAppLabel;
    private AlertDialog mDialog;
    private LocalBluetoothAdapter mLocalAdapter;
    private BroadcastReceiver mReceiver;
    private int mRequest;
    private int mTimeout = 120;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().addPrivateFlags(524288);
        setResult(0);
        if (parseIntent()) {
            finish();
        }
        int state = this.mLocalAdapter.getState();
        if (this.mRequest == 3) {
            switch (state) {
                case AccessPoint.Speed.MODERATE:
                case 13:
                    proceedAndFinish();
                    break;
                case 11:
                case 12:
                    Intent intent = new Intent(this, (Class<?>) RequestPermissionHelperActivity.class);
                    intent.putExtra("com.android.settings.bluetooth.extra.APP_LABEL", this.mAppLabel);
                    intent.setAction("com.android.settings.bluetooth.ACTION_INTERNAL_REQUEST_BT_OFF");
                    startActivityForResult(intent, 0);
                    break;
                default:
                    Log.e("RequestPermissionActivity", "Unknown adapter state: " + state);
                    cancelAndFinish();
                    break;
            }
        }
        switch (state) {
            case AccessPoint.Speed.MODERATE:
            case 11:
            case 13:
                Intent intent2 = new Intent(this, (Class<?>) RequestPermissionHelperActivity.class);
                intent2.setAction("com.android.settings.bluetooth.ACTION_INTERNAL_REQUEST_BT_ON");
                intent2.setFlags(67108864);
                intent2.putExtra("com.android.settings.bluetooth.extra.APP_LABEL", this.mAppLabel);
                if (this.mRequest == 2) {
                    intent2.putExtra("android.bluetooth.adapter.extra.DISCOVERABLE_DURATION", this.mTimeout);
                }
                startActivityForResult(intent2, mRequestCode);
                mRequestCode++;
                break;
            case 12:
                if (this.mRequest == 1) {
                    proceedAndFinish();
                } else {
                    createDialog();
                }
                break;
            default:
                Log.e("RequestPermissionActivity", "Unknown adapter state: " + state);
                cancelAndFinish();
                break;
        }
    }

    private void createDialog() {
        String string;
        String string2;
        if (getResources().getBoolean(R.bool.auto_confirm_bluetooth_activation_dialog)) {
            onClick(null, -1);
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (this.mReceiver != null) {
            switch (this.mRequest) {
                case 1:
                case 2:
                    builder.setMessage(getString(R.string.bluetooth_turning_on));
                    break;
                default:
                    builder.setMessage(getString(R.string.bluetooth_turning_off));
                    break;
            }
            builder.setCancelable(false);
        } else {
            if (this.mTimeout == 0) {
                if (this.mAppLabel != null) {
                    string2 = getString(R.string.bluetooth_ask_lasting_discovery, new Object[]{this.mAppLabel});
                } else {
                    string2 = getString(R.string.bluetooth_ask_lasting_discovery_no_name);
                }
                builder.setMessage(string2);
            } else {
                if (this.mAppLabel != null) {
                    string = getString(R.string.bluetooth_ask_discovery, new Object[]{this.mAppLabel, Integer.valueOf(this.mTimeout)});
                } else {
                    string = getString(R.string.bluetooth_ask_discovery_no_name, new Object[]{Integer.valueOf(this.mTimeout)});
                }
                builder.setMessage(string);
            }
            builder.setPositiveButton(getString(R.string.allow), this);
            builder.setNegativeButton(getString(R.string.deny), this);
        }
        this.mDialog = builder.create();
        this.mDialog.show();
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        if (i != mRequestCode - 1) {
            Log.e("RequestPermissionActivity", "Unexpected onActivityResult " + i + ' ' + i2);
            setResult(0);
            finish();
        }
        if (i2 != -1) {
            cancelAndFinish();
            return;
        }
        switch (this.mRequest) {
            case 1:
            case 2:
                if (this.mLocalAdapter.getBluetoothState() == 12) {
                    proceedAndFinish();
                } else {
                    this.mReceiver = new StateChangeReceiver();
                    registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED"));
                    createDialog();
                }
                break;
            case 3:
                if (this.mLocalAdapter.getBluetoothState() == 10) {
                    proceedAndFinish();
                } else {
                    this.mReceiver = new StateChangeReceiver();
                    registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED"));
                    createDialog();
                }
                break;
            default:
                cancelAndFinish();
                break;
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {
            case -2:
                setResult(0);
                finish();
                break;
            case -1:
                proceedAndFinish();
                break;
        }
    }

    public void proceedAndFinish() {
        int i;
        if (this.mRequest == 1 || this.mRequest == 3) {
            i = -1;
        } else if (this.mLocalAdapter.setScanMode(23, this.mTimeout)) {
            long jCurrentTimeMillis = System.currentTimeMillis() + (((long) this.mTimeout) * 1000);
            LocalBluetoothPreferences.persistDiscoverableEndTimestamp(this, jCurrentTimeMillis);
            if (this.mTimeout > 0) {
                BluetoothDiscoverableTimeoutReceiver.setDiscoverableAlarm(this, jCurrentTimeMillis);
            }
            i = this.mTimeout;
            if (i < 1) {
                i = 1;
            }
        } else {
            i = 0;
        }
        if (this.mDialog != null) {
            this.mDialog.dismiss();
        }
        setResult(i);
        finish();
    }

    public void cancelAndFinish() {
        setResult(0);
        finish();
    }

    private boolean parseIntent() {
        Intent intent = getIntent();
        if (intent == null) {
            return true;
        }
        if (intent.getAction().equals("android.bluetooth.adapter.action.REQUEST_ENABLE")) {
            this.mRequest = 1;
        } else if (intent.getAction().equals("android.bluetooth.adapter.action.REQUEST_DISABLE")) {
            this.mRequest = 3;
        } else if (intent.getAction().equals("android.bluetooth.adapter.action.REQUEST_DISCOVERABLE")) {
            this.mRequest = 2;
            this.mTimeout = intent.getIntExtra("android.bluetooth.adapter.extra.DISCOVERABLE_DURATION", 120);
            Log.d("RequestPermissionActivity", "Setting Bluetooth Discoverable Timeout = " + this.mTimeout);
            if (this.mTimeout < 1 || this.mTimeout > 3600) {
                this.mTimeout = 120;
            }
        } else {
            Log.e("RequestPermissionActivity", "Error: this activity may be started only with intent android.bluetooth.adapter.action.REQUEST_ENABLE or android.bluetooth.adapter.action.REQUEST_DISCOVERABLE");
            setResult(0);
            return true;
        }
        LocalBluetoothManager localBtManager = Utils.getLocalBtManager(this);
        if (localBtManager == null) {
            Log.e("RequestPermissionActivity", "Error: there's a problem starting Bluetooth");
            setResult(0);
            return true;
        }
        String callingPackage = getCallingPackage();
        if (TextUtils.isEmpty(callingPackage)) {
            callingPackage = getIntent().getStringExtra("android.intent.extra.PACKAGE_NAME");
        }
        if (!TextUtils.isEmpty(callingPackage)) {
            try {
                this.mAppLabel = getPackageManager().getApplicationInfo(callingPackage, 0).loadSafeLabel(getPackageManager());
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("RequestPermissionActivity", "Couldn't find app with package name " + callingPackage);
                setResult(0);
                return true;
            }
        }
        this.mLocalAdapter = localBtManager.getBluetoothAdapter();
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.mReceiver != null) {
            unregisterReceiver(this.mReceiver);
            this.mReceiver = null;
        }
    }

    @Override
    public void onBackPressed() {
        setResult(0);
        super.onBackPressed();
    }

    final class StateChangeReceiver extends BroadcastReceiver {
        public StateChangeReceiver() {
            RequestPermissionActivity.this.getWindow().getDecorView().postDelayed(new Runnable() {
                @Override
                public final void run() {
                    RequestPermissionActivity.StateChangeReceiver.lambda$new$0(this.f$0);
                }
            }, 10000L);
        }

        public static void lambda$new$0(StateChangeReceiver stateChangeReceiver) {
            if (!RequestPermissionActivity.this.isFinishing() && !RequestPermissionActivity.this.isDestroyed()) {
                RequestPermissionActivity.this.cancelAndFinish();
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
            }
            int intExtra = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", AccessPoint.UNREACHABLE_RSSI);
            switch (RequestPermissionActivity.this.mRequest) {
                case 1:
                case 2:
                    if (intExtra == 12) {
                        RequestPermissionActivity.this.proceedAndFinish();
                    }
                    break;
                case 3:
                    if (intExtra == 10) {
                        RequestPermissionActivity.this.proceedAndFinish();
                    }
                    break;
            }
        }
    }
}
