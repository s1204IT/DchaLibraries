package com.android.settings.connecteddevice;

import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.bluetooth.BluetoothDeviceRenamePreferenceController;
import com.android.settings.bluetooth.BluetoothSwitchPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBarController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.FooterPreference;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class BluetoothDashboardFragment extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.connecteddevice.BluetoothDashboardFragment.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableRaw searchIndexableRaw = new SearchIndexableRaw(context);
            searchIndexableRaw.title = context.getString(R.string.bluetooth_settings_title);
            searchIndexableRaw.screenTitle = context.getString(R.string.bluetooth_settings_title);
            searchIndexableRaw.keywords = context.getString(R.string.keywords_bluetooth_settings);
            searchIndexableRaw.key = "bluetooth_switchbar_screen";
            arrayList.add(searchIndexableRaw);
            return arrayList;
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<String> getNonIndexableKeys(Context context) {
            char c;
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService("bluetooth");
            if (bluetoothManager != null) {
                if (bluetoothManager.getAdapter() != null) {
                    c = 0;
                } else {
                    c = 2;
                }
                if (c != 0) {
                    nonIndexableKeys.add("bluetooth_switchbar_screen");
                }
            }
            return nonIndexableKeys;
        }
    };
    private BluetoothSwitchPreferenceController mController;
    private FooterPreference mFooterPreference;
    private SwitchBar mSwitchBar;

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 1390;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "BluetoothDashboardFrag";
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_uri_bluetooth_screen;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.bluetooth_screen;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mFooterPreference = this.mFooterPreferenceMixin.createFooterPreference();
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        ((BluetoothDeviceRenamePreferenceController) use(BluetoothDeviceRenamePreferenceController.class)).setFragment(this);
    }

    @Override // com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        SettingsActivity settingsActivity = (SettingsActivity) getActivity();
        this.mSwitchBar = settingsActivity.getSwitchBar();
        this.mController = new BluetoothSwitchPreferenceController(settingsActivity, new SwitchBarController(this.mSwitchBar), this.mFooterPreference);
        Lifecycle lifecycle = getLifecycle();
        if (lifecycle != null) {
            lifecycle.addObserver(this.mController);
        }
    }
}
