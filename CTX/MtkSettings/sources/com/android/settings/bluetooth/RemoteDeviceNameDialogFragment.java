package com.android.settings.bluetooth;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

public class RemoteDeviceNameDialogFragment extends BluetoothNameDialogFragment {
    private CachedBluetoothDevice mDevice;

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
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        super.onTextChanged(charSequence, i, i2, i3);
    }

    public static RemoteDeviceNameDialogFragment newInstance(CachedBluetoothDevice cachedBluetoothDevice) {
        Bundle bundle = new Bundle(1);
        bundle.putString("cached_device", cachedBluetoothDevice.getDevice().getAddress());
        RemoteDeviceNameDialogFragment remoteDeviceNameDialogFragment = new RemoteDeviceNameDialogFragment();
        remoteDeviceNameDialogFragment.setArguments(bundle);
        return remoteDeviceNameDialogFragment;
    }

    @VisibleForTesting
    CachedBluetoothDevice getDevice(Context context) {
        String string = getArguments().getString("cached_device");
        LocalBluetoothManager localBtManager = Utils.getLocalBtManager(context);
        return localBtManager.getCachedDeviceManager().findDevice(localBtManager.getBluetoothAdapter().getRemoteDevice(string));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mDevice = getDevice(context);
    }

    @Override
    public int getMetricsCategory() {
        return 1015;
    }

    @Override
    protected int getDialogTitle() {
        return R.string.bluetooth_device_name;
    }

    @Override
    protected String getDeviceName() {
        if (this.mDevice != null) {
            return this.mDevice.getName();
        }
        return null;
    }

    @Override
    protected void setDeviceName(String str) {
        if (this.mDevice != null) {
            this.mDevice.setName(str);
        }
    }
}
