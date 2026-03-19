package com.android.settings.bluetooth;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import com.android.settings.R;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.wifi.AccessPoint;

public class LocalDeviceNameDialogFragment extends BluetoothNameDialogFragment {
    private LocalBluetoothAdapter mLocalAdapter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED".equals(action) || ("android.bluetooth.adapter.action.STATE_CHANGED".equals(action) && intent.getIntExtra("android.bluetooth.adapter.extra.STATE", AccessPoint.UNREACHABLE_RSSI) == 12)) {
                LocalDeviceNameDialogFragment.this.updateDeviceName();
            }
        }
    };

    @Override
    public void afterTextChanged(Editable editable) {
        super.afterTextChanged(editable);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        super.beforeTextChanged(charSequence, i, i2, i3);
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        return super.onCreateDialog(bundle);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        super.onTextChanged(charSequence, i, i2, i3);
    }

    public static LocalDeviceNameDialogFragment newInstance() {
        return new LocalDeviceNameDialogFragment();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mLocalAdapter = Utils.getLocalBtManager(getActivity()).getBluetoothAdapter();
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED");
        getActivity().registerReceiver(this.mReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(this.mReceiver);
    }

    @Override
    public int getMetricsCategory() {
        return 538;
    }

    @Override
    protected int getDialogTitle() {
        return R.string.bluetooth_rename_device;
    }

    @Override
    protected String getDeviceName() {
        if (this.mLocalAdapter != null && this.mLocalAdapter.isEnabled()) {
            return this.mLocalAdapter.getName();
        }
        return null;
    }

    @Override
    protected void setDeviceName(String str) {
        this.mLocalAdapter.setName(str);
    }
}
