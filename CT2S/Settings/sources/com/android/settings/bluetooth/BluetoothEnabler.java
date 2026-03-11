package com.android.settings.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.widget.Switch;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.WirelessSettings;
import com.android.settings.search.Index;
import com.android.settings.widget.SwitchBar;

public final class BluetoothEnabler implements SwitchBar.OnSwitchChangeListener {
    private Context mContext;
    private final IntentFilter mIntentFilter;
    private final LocalBluetoothAdapter mLocalAdapter;
    private Switch mSwitch;
    private SwitchBar mSwitchBar;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    boolean isBluetoothOn = msg.getData().getBoolean("is_bluetooth_on");
                    Index.getInstance(BluetoothEnabler.this.mContext).updateFromClassNameResource(BluetoothSettings.class.getName(), true, isBluetoothOn);
                    break;
            }
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE);
            BluetoothEnabler.this.handleStateChanged(state);
        }
    };
    private boolean mValidListener = false;

    public BluetoothEnabler(Context context, SwitchBar switchBar) {
        this.mContext = context;
        this.mSwitchBar = switchBar;
        this.mSwitch = switchBar.getSwitch();
        LocalBluetoothManager manager = LocalBluetoothManager.getInstance(context);
        if (manager == null) {
            this.mLocalAdapter = null;
            this.mSwitch.setEnabled(false);
        } else {
            this.mLocalAdapter = manager.getBluetoothAdapter();
        }
        this.mIntentFilter = new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED");
    }

    public void setupSwitchBar() {
        this.mSwitchBar.show();
    }

    public void teardownSwitchBar() {
        this.mSwitchBar.hide();
    }

    public void resume(Context context) {
        if (this.mLocalAdapter == null) {
            this.mSwitch.setEnabled(false);
            return;
        }
        if (this.mContext != context) {
            this.mContext = context;
        }
        handleStateChanged(this.mLocalAdapter.getBluetoothState());
        this.mSwitchBar.addOnSwitchChangeListener(this);
        this.mContext.registerReceiver(this.mReceiver, this.mIntentFilter);
        this.mValidListener = true;
    }

    public void pause() {
        if (this.mLocalAdapter != null) {
            this.mSwitchBar.removeOnSwitchChangeListener(this);
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mValidListener = false;
        }
    }

    void handleStateChanged(int state) {
        switch (state) {
            case 10:
                setChecked(false);
                this.mSwitch.setEnabled(true);
                updateSearchIndex(false);
                break;
            case 11:
                this.mSwitch.setEnabled(false);
                break;
            case 12:
                setChecked(true);
                this.mSwitch.setEnabled(true);
                updateSearchIndex(true);
                break;
            case 13:
                this.mSwitch.setEnabled(false);
                break;
            default:
                setChecked(false);
                this.mSwitch.setEnabled(true);
                updateSearchIndex(false);
                break;
        }
    }

    private void setChecked(boolean isChecked) {
        if (isChecked != this.mSwitch.isChecked()) {
            if (this.mValidListener) {
                this.mSwitchBar.removeOnSwitchChangeListener(this);
            }
            this.mSwitch.setChecked(isChecked);
            if (this.mValidListener) {
                this.mSwitchBar.addOnSwitchChangeListener(this);
            }
        }
    }

    private void updateSearchIndex(boolean isBluetoothOn) {
        this.mHandler.removeMessages(0);
        Message msg = new Message();
        msg.what = 0;
        msg.getData().putBoolean("is_bluetooth_on", isBluetoothOn);
        this.mHandler.sendMessage(msg);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (isChecked && !WirelessSettings.isRadioAllowed(this.mContext, "bluetooth")) {
            Toast.makeText(this.mContext, R.string.wifi_in_airplane_mode, 0).show();
            switchView.setChecked(false);
        }
        if (this.mLocalAdapter != null) {
            this.mLocalAdapter.setBluetoothEnabled(isChecked);
        }
        this.mSwitch.setEnabled(false);
    }
}
