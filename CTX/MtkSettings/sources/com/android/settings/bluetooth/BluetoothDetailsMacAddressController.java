package com.android.settings.bluetooth;

import android.content.Context;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.FooterPreferenceMixin;

public class BluetoothDetailsMacAddressController extends BluetoothDetailsController {
    FooterPreference mFooterPreference;
    FooterPreferenceMixin mFooterPreferenceMixin;

    public BluetoothDetailsMacAddressController(Context context, PreferenceFragment preferenceFragment, CachedBluetoothDevice cachedBluetoothDevice, Lifecycle lifecycle) {
        super(context, preferenceFragment, cachedBluetoothDevice, lifecycle);
        this.mFooterPreferenceMixin = new FooterPreferenceMixin(preferenceFragment, lifecycle);
    }

    @Override
    protected void init(PreferenceScreen preferenceScreen) {
        this.mFooterPreference = this.mFooterPreferenceMixin.createFooterPreference();
        this.mFooterPreference.setTitle(this.mContext.getString(R.string.bluetooth_device_mac_address, this.mCachedDevice.getAddress()));
    }

    @Override
    protected void refresh() {
    }

    @Override
    public String getPreferenceKey() {
        if (this.mFooterPreference == null) {
            return null;
        }
        return this.mFooterPreference.getKey();
    }
}
