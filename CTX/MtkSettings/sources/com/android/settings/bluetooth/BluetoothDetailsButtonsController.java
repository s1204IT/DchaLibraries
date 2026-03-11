package com.android.settings.bluetooth;

import android.content.Context;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.PreferenceScreen;
import android.view.View;
import com.android.settings.R;
import com.android.settings.widget.ActionButtonPreference;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class BluetoothDetailsButtonsController extends BluetoothDetailsController {
    private ActionButtonPreference mActionButtons;
    private boolean mConnectButtonInitialized;
    private boolean mIsConnected;

    public BluetoothDetailsButtonsController(Context context, PreferenceFragment preferenceFragment, CachedBluetoothDevice cachedBluetoothDevice, Lifecycle lifecycle) {
        super(context, preferenceFragment, cachedBluetoothDevice, lifecycle);
        this.mIsConnected = cachedBluetoothDevice.isConnected();
    }

    public void onForgetButtonPressed() {
        ForgetDeviceDialogFragment.newInstance(this.mCachedDevice.getAddress()).show(this.mFragment.getFragmentManager(), "ForgetBluetoothDevice");
    }

    @Override
    protected void init(PreferenceScreen preferenceScreen) {
        this.mActionButtons = ((ActionButtonPreference) preferenceScreen.findPreference(getPreferenceKey())).setButton1Text(R.string.forget).setButton1OnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.onForgetButtonPressed();
            }
        }).setButton1Positive(false).setButton1Enabled(true);
    }

    @Override
    protected void refresh() {
        this.mActionButtons.setButton2Enabled(!this.mCachedDevice.isBusy());
        boolean z = this.mIsConnected;
        this.mIsConnected = this.mCachedDevice.isConnected();
        if (this.mIsConnected) {
            if (!this.mConnectButtonInitialized || !z) {
                this.mActionButtons.setButton2Text(R.string.bluetooth_device_context_disconnect).setButton2OnClickListener(new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        this.f$0.mCachedDevice.disconnect();
                    }
                }).setButton2Positive(false);
                this.mConnectButtonInitialized = true;
                return;
            }
            return;
        }
        if (!this.mConnectButtonInitialized || z) {
            this.mActionButtons.setButton2Text(R.string.bluetooth_device_context_connect).setButton2OnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    this.f$0.mCachedDevice.connect(true);
                }
            }).setButton2Positive(true);
            this.mConnectButtonInitialized = true;
        }
    }

    @Override
    public String getPreferenceKey() {
        return "action_buttons";
    }
}
