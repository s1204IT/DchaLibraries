package com.android.settings.deletionhelper;

import android.content.Context;
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

public class AutomaticStorageManagerSettings extends DashboardFragment implements Preference.OnPreferenceChangeListener {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        protected boolean isPageSearchEnabled(Context context) {
            return false;
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return AutomaticStorageManagerSettings.buildPreferenceControllers(context);
        }
    };
    private DropDownPreference mDaysToRetain;
    private SwitchBar mSwitchBar;
    private AutomaticStorageManagerSwitchBarController mSwitchController;

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewOnCreateView = super.onCreateView(layoutInflater, viewGroup, bundle);
        initializeDaysToRetainPreference();
        initializeSwitchBar();
        return viewOnCreateView;
    }

    private void initializeDaysToRetainPreference() {
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

    @Override
    public void onResume() {
        super.onResume();
        this.mDaysToRetain.setEnabled(Utils.isStorageManagerEnabled(getContext()));
    }

    @Override
    protected String getLogTag() {
        return null;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.automatic_storage_management_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.mSwitchBar.hide();
        this.mSwitchController.tearDown();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if ("days".equals(preference.getKey())) {
            Settings.Secure.putInt(getContentResolver(), "automatic_storage_manager_days_to_retain", Integer.parseInt((String) obj));
            return true;
        }
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return 458;
    }

    @Override
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

    public static List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new AutomaticStorageManagerDescriptionPreferenceController(context));
        return arrayList;
    }
}
