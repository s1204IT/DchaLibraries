package com.android.settings.bluetooth;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.PreferenceScreen;
import android.util.Pair;
import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.Lifecycle;

/* loaded from: classes.dex */
public class BluetoothDetailsHeaderController extends BluetoothDetailsController {
    private CachedBluetoothDeviceManager mDeviceManager;
    private EntityHeaderController mHeaderController;
    private LocalBluetoothManager mLocalManager;

    public BluetoothDetailsHeaderController(Context context, PreferenceFragment preferenceFragment, CachedBluetoothDevice cachedBluetoothDevice, Lifecycle lifecycle, LocalBluetoothManager localBluetoothManager) {
        super(context, preferenceFragment, cachedBluetoothDevice, lifecycle);
        this.mLocalManager = localBluetoothManager;
        this.mDeviceManager = this.mLocalManager.getCachedDeviceManager();
    }

    @Override // com.android.settings.bluetooth.BluetoothDetailsController
    protected void init(PreferenceScreen preferenceScreen) {
        LayoutPreference layoutPreference = (LayoutPreference) preferenceScreen.findPreference("bluetooth_device_header");
        this.mHeaderController = EntityHeaderController.newInstance(this.mFragment.getActivity(), this.mFragment, layoutPreference.findViewById(R.id.entity_header));
        preferenceScreen.addPreference(layoutPreference);
    }

    protected void setHeaderProperties() {
        Pair<Drawable, String> btClassDrawableWithDescription = com.android.settingslib.bluetooth.Utils.getBtClassDrawableWithDescription(this.mContext, this.mCachedDevice, this.mContext.getResources().getFraction(R.fraction.bt_battery_scale_fraction, 1, 1));
        String connectionSummary = this.mCachedDevice.getConnectionSummary();
        String hearingAidPairDeviceSummary = this.mDeviceManager.getHearingAidPairDeviceSummary(this.mCachedDevice);
        if (hearingAidPairDeviceSummary != null) {
            this.mHeaderController.setSecondSummary(hearingAidPairDeviceSummary);
        }
        this.mHeaderController.setLabel(this.mCachedDevice.getName());
        this.mHeaderController.setIcon((Drawable) btClassDrawableWithDescription.first);
        this.mHeaderController.setIconContentDescription((String) btClassDrawableWithDescription.second);
        this.mHeaderController.setSummary(connectionSummary);
    }

    @Override // com.android.settings.bluetooth.BluetoothDetailsController
    protected void refresh() {
        setHeaderProperties();
        this.mHeaderController.done(this.mFragment.getActivity(), true);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "bluetooth_device_header";
    }
}
