package com.android.settings.deletionhelper;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class AutomaticStorageManagerSettings extends DashboardFragment implements Preference.OnPreferenceChangeListener {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.deletionhelper.AutomaticStorageManagerSettings.1
        @Override // com.android.settings.search.BaseSearchIndexProvider
        protected boolean isPageSearchEnabled(Context context) {
            return false;
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return AutomaticStorageManagerSettings.buildPreferenceControllers(context);
        }
    };
    private DropDownPreference mDaysToRetain;
    private SwitchBar mSwitchBar;
    private AutomaticStorageManagerSwitchBarController mSwitchController;

    @Override // com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) throws Resources.NotFoundException {
        View viewOnCreateView = super.onCreateView(layoutInflater, viewGroup, bundle);
        initializeDaysToRetainPreference();
        initializeSwitchBar();
        return viewOnCreateView;
    }

    private void initializeDaysToRetainPreference() throws Resources.NotFoundException {
        this.mDaysToRetain = (DropDownPreference) findPreference("days");
        this.mDaysToRetain.setOnPreferenceChangeListener(this);
        int i = Settings.Secure.getInt(getContentResolver(), "automatic_storage_manager_days_to_retain", Utils.getDefaultStorageManagerDaysToRetain(getResources()));
        String[] stringArray = getResources().getStringArray(R.array.automatic_storage_management_days_values);
        this.mDaysToRetain.setValue(stringArray[daysValueToIndex(i, stringArray)]);
    }

    private void initializeSwitchBar() {
        this.mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        this.mSwitchBar.setSwitchBarText(R.string.automatic_storage_manager_master_switch_title, R.string.automatic_storage_manager_master_switch_title);
        this.mSwitchBar.show();
        this.mSwitchController = new AutomaticStorageManagerSwitchBarController(getContext(), this.mSwitchBar, this.mMetricsFeatureProvider, this.mDaysToRetain, getFragmentManager());
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        this.mDaysToRetain.setEnabled(Utils.isStorageManagerEnabled(getContext()));
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return null;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.automatic_storage_management_settings;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onDestroyView() {
        super.onDestroyView();
        this.mSwitchBar.hide();
        this.mSwitchController.tearDown();
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if ("days".equals(preference.getKey())) {
            Settings.Secure.putInt(getContentResolver(), "automatic_storage_manager_days_to_retain", Integer.parseInt((String) obj));
            return true;
        }
        return true;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 458;
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_uri_storage;
    }

    private static int daysValueToIndex(int i, String[] strArr) {
        for (int i2 = 0; i2 < strArr.length; i2++) {
            if (i == Integer.parseInt(strArr[i2])) {
                return i2;
            }
        }
        return strArr.length - 1;
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new AutomaticStorageManagerDescriptionPreferenceController(context));
        return arrayList;
    }
}
