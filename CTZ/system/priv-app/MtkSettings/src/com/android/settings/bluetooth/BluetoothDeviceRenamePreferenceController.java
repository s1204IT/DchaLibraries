package com.android.settings.bluetooth;

import android.app.Fragment;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Pair;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/* loaded from: classes.dex */
public class BluetoothDeviceRenamePreferenceController extends BluetoothDeviceNamePreferenceController {
    private Fragment mFragment;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    public BluetoothDeviceRenamePreferenceController(Context context, String str) {
        super(context, str);
        this.mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    BluetoothDeviceRenamePreferenceController(Context context, LocalBluetoothAdapter localBluetoothAdapter, String str) {
        super(context, localBluetoothAdapter, str);
        this.mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    public void setFragment(Fragment fragment) {
        this.mFragment = fragment;
    }

    @Override // com.android.settings.bluetooth.BluetoothDeviceNamePreferenceController
    protected void updatePreferenceState(Preference preference) {
        preference.setSummary(getSummary());
        preference.setVisible(this.mLocalAdapter != null && this.mLocalAdapter.isEnabled());
    }

    @Override // com.android.settings.bluetooth.BluetoothDeviceNamePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public CharSequence getSummary() {
        return getDeviceName();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(getPreferenceKey(), preference.getKey()) || this.mFragment == null) {
            return false;
        }
        this.mMetricsFeatureProvider.action(this.mContext, 161, new Pair[0]);
        LocalDeviceNameDialogFragment.newInstance().show(this.mFragment.getFragmentManager(), "LocalAdapterName");
        return true;
    }
}
