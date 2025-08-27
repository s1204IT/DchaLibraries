package com.android.settings.bluetooth;

import android.content.Context;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.settings.R;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class BluetoothDeviceDetailsFragment extends RestrictedDashboardFragment {
    static int EDIT_DEVICE_NAME_ITEM_ID = 1;
    static TestDataFactory sTestDataFactory;
    private CachedBluetoothDevice mCachedDevice;
    private String mDeviceAddress;
    private LocalBluetoothManager mManager;

    interface TestDataFactory {
        CachedBluetoothDevice getDevice(String str);

        LocalBluetoothManager getManager(Context context);
    }

    public BluetoothDeviceDetailsFragment() {
        super("no_config_bluetooth");
    }

    LocalBluetoothManager getLocalBluetoothManager(Context context) {
        if (sTestDataFactory != null) {
            return sTestDataFactory.getManager(context);
        }
        return Utils.getLocalBtManager(context);
    }

    CachedBluetoothDevice getCachedDevice(String str) {
        if (sTestDataFactory != null) {
            return sTestDataFactory.getDevice(str);
        }
        return this.mManager.getCachedDeviceManager().findDevice(this.mManager.getBluetoothAdapter().getRemoteDevice(str));
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onAttach(Context context) {
        this.mDeviceAddress = getArguments().getString("device_address");
        this.mManager = getLocalBluetoothManager(context);
        this.mCachedDevice = getCachedDevice(this.mDeviceAddress);
        super.onAttach(context);
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 1009;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "BTDeviceDetailsFrg";
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.bluetooth_device_details_fragment;
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        MenuItem menuItemAdd = menu.add(0, EDIT_DEVICE_NAME_ITEM_ID, 0, R.string.bluetooth_rename_button);
        menuItemAdd.setIcon(R.drawable.ic_mode_edit);
        menuItemAdd.setShowAsAction(2);
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == EDIT_DEVICE_NAME_ITEM_ID) {
            RemoteDeviceNameDialogFragment.newInstance(this.mCachedDevice).show(getFragmentManager(), "RemoteDeviceName");
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        if (this.mCachedDevice != null) {
            Lifecycle lifecycle = getLifecycle();
            arrayList.add(new BluetoothDetailsHeaderController(context, this, this.mCachedDevice, lifecycle, this.mManager));
            arrayList.add(new BluetoothDetailsButtonsController(context, this, this.mCachedDevice, lifecycle));
            arrayList.add(new BluetoothDetailsProfilesController(context, this, this.mManager, this.mCachedDevice, lifecycle));
            arrayList.add(new BluetoothDetailsMacAddressController(context, this, this.mCachedDevice, lifecycle));
        }
        return arrayList;
    }
}
