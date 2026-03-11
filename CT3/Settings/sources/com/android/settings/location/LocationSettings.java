package com.android.settings.location;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Switch;
import com.android.settings.DimmableIconPreference;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.InstalledAppDetails;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.location.RecentLocationApps;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LocationSettings extends LocationSettingsBase implements SwitchBar.OnSwitchChangeListener {
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
    private SettingsInjector injector;
    private PreferenceCategory mCategoryRecentLocationRequests;
    private ISettingsMiscExt mExt;
    private Preference mLocationMode;
    private UserHandle mManagedProfile;
    private RestrictedSwitchPreference mManagedProfileSwitch;
    private BroadcastReceiver mReceiver;
    private Switch mSwitch;
    private SwitchBar mSwitchBar;
    private UserManager mUm;
    private boolean mValidListener = false;
    private Preference.OnPreferenceClickListener mManagedProfileSwitchClickListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            boolean switchState = LocationSettings.this.mManagedProfileSwitch.isChecked();
            LocationSettings.this.mUm.setUserRestriction("no_share_location", !switchState, LocationSettings.this.mManagedProfile);
            LocationSettings.this.mManagedProfileSwitch.setSummary(switchState ? R.string.switch_on_text : R.string.switch_off_text);
            return true;
        }
    };

    @Override
    protected int getMetricsCategory() {
        return 63;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SettingsActivity activity = (SettingsActivity) getActivity();
        this.mUm = (UserManager) activity.getSystemService("user");
        setHasOptionsMenu(true);
        this.mSwitchBar = activity.getSwitchBar();
        this.mSwitch = this.mSwitchBar.getSwitch();
        this.mSwitchBar.show();
        this.mExt = UtilsExt.getMiscPlugin(activity);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.mSwitchBar.hide();
    }

    @Override
    public void onResume() {
        super.onResume();
        createPreferenceHierarchy();
        if (this.mValidListener) {
            return;
        }
        this.mSwitchBar.addOnSwitchChangeListener(this);
        this.mValidListener = true;
    }

    @Override
    public void onPause() {
        try {
            getActivity().unregisterReceiver(this.mReceiver);
        } catch (RuntimeException e) {
            if (Log.isLoggable("LocationSettings", 2)) {
                Log.v("LocationSettings", "Swallowing " + e);
            }
        }
        if (this.mValidListener) {
            this.mSwitchBar.removeOnSwitchChangeListener(this);
            this.mValidListener = false;
        }
        super.onPause();
    }

    private void addPreferencesSorted(List<Preference> prefs, PreferenceGroup container) {
        Collections.sort(prefs, new Comparator<Preference>() {
            @Override
            public int compare(Preference lhs, Preference rhs) {
                return lhs.getTitle().toString().compareTo(rhs.getTitle().toString());
            }
        });
        for (Preference entry : prefs) {
            container.addPreference(entry);
        }
    }

    private PreferenceScreen createPreferenceHierarchy() {
        final SettingsActivity activity = (SettingsActivity) getActivity();
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.location_settings);
        PreferenceScreen root2 = getPreferenceScreen();
        setupManagedProfileCategory(root2);
        this.mLocationMode = root2.findPreference("location_mode");
        this.mLocationMode.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                activity.startPreferencePanel(LocationMode.class.getName(), null, R.string.location_mode_screen_title, null, LocationSettings.this, 0);
                return true;
            }
        });
        this.mExt.initCustomizedLocationSettings(root2, this.mLocationMode.getOrder() + 1);
        this.mCategoryRecentLocationRequests = (PreferenceCategory) root2.findPreference("recent_location_requests");
        RecentLocationApps recentApps = new RecentLocationApps(activity);
        List<RecentLocationApps.Request> recentLocationRequests = recentApps.getAppList();
        List<Preference> recentLocationPrefs = new ArrayList<>(recentLocationRequests.size());
        for (RecentLocationApps.Request request : recentLocationRequests) {
            Preference pref = new DimmableIconPreference(getPrefContext(), request.contentDescription);
            pref.setIcon(request.icon);
            pref.setTitle(request.label);
            if (request.isHighBattery) {
                pref.setSummary(R.string.location_high_battery_use);
            } else {
                pref.setSummary(R.string.location_low_battery_use);
            }
            pref.setOnPreferenceClickListener(new PackageEntryClickedListener(request.packageName, request.userHandle));
            recentLocationPrefs.add(pref);
        }
        if (recentLocationRequests.size() > 0) {
            addPreferencesSorted(recentLocationPrefs, this.mCategoryRecentLocationRequests);
        } else {
            Preference banner = new Preference(getPrefContext());
            banner.setLayoutResource(R.layout.location_list_no_item);
            banner.setTitle(R.string.location_no_recent_apps);
            banner.setSelectable(false);
            this.mCategoryRecentLocationRequests.addPreference(banner);
        }
        boolean lockdownOnLocationAccess = false;
        if (this.mManagedProfile != null && this.mUm.hasUserRestriction("no_share_location", this.mManagedProfile)) {
            lockdownOnLocationAccess = true;
        }
        addLocationServices(activity, root2, lockdownOnLocationAccess);
        refreshLocationMode();
        return root2;
    }

    private void setupManagedProfileCategory(PreferenceScreen root) {
        this.mManagedProfile = Utils.getManagedProfile(this.mUm);
        if (this.mManagedProfile == null) {
            root.removePreference(root.findPreference("managed_profile_location_switch"));
            this.mManagedProfileSwitch = null;
        } else {
            this.mManagedProfileSwitch = (RestrictedSwitchPreference) root.findPreference("managed_profile_location_switch");
            this.mManagedProfileSwitch.setOnPreferenceClickListener(null);
        }
    }

    private void changeManagedProfileLocationAccessStatus(boolean mainSwitchOn) {
        if (this.mManagedProfileSwitch == null) {
            return;
        }
        this.mManagedProfileSwitch.setOnPreferenceClickListener(null);
        RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(getActivity(), "no_share_location", this.mManagedProfile.getIdentifier());
        boolean isRestrictedByBase = isManagedProfileRestrictedByBase();
        if (!isRestrictedByBase && admin != null) {
            this.mManagedProfileSwitch.setDisabledByAdmin(admin);
            this.mManagedProfileSwitch.setChecked(false);
            return;
        }
        this.mManagedProfileSwitch.setEnabled(mainSwitchOn);
        int summaryResId = R.string.switch_off_text;
        if (!mainSwitchOn) {
            this.mManagedProfileSwitch.setChecked(false);
        } else {
            this.mManagedProfileSwitch.setChecked(isRestrictedByBase ? false : true);
            summaryResId = isRestrictedByBase ? R.string.switch_off_text : R.string.switch_on_text;
            this.mManagedProfileSwitch.setOnPreferenceClickListener(this.mManagedProfileSwitchClickListener);
        }
        this.mManagedProfileSwitch.setSummary(summaryResId);
    }

    private void addLocationServices(Context context, PreferenceScreen root, boolean lockdownOnLocationAccess) {
        PreferenceCategory categoryLocationServices = (PreferenceCategory) root.findPreference("location_services");
        this.injector = new SettingsInjector(context);
        List<Preference> locationServices = this.injector.getInjectedSettings(lockdownOnLocationAccess ? UserHandle.myUserId() : -2);
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (Log.isLoggable("LocationSettings", 3)) {
                    Log.d("LocationSettings", "Received settings change intent: " + intent);
                }
                LocationSettings.this.injector.reloadStatusMessages();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.location.InjectedSettingChanged");
        context.registerReceiver(this.mReceiver, filter);
        if (locationServices.size() > 0) {
            addPreferencesSorted(locationServices, categoryLocationServices);
        } else {
            root.removePreference(categoryLocationServices);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, 1, 0, R.string.location_menu_scanning);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SettingsActivity activity = (SettingsActivity) getActivity();
        switch (item.getItemId()) {
            case DefaultWfcSettingsExt.PAUSE:
                activity.startPreferencePanel(ScanningSettings.class.getName(), null, R.string.location_scanning_screen_title, null, this, 0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_location_access;
    }

    public static int getLocationString(int mode) {
        switch (mode) {
            case DefaultWfcSettingsExt.RESUME:
                return R.string.location_mode_location_off_title;
            case DefaultWfcSettingsExt.PAUSE:
                return R.string.location_mode_sensors_only_title;
            case DefaultWfcSettingsExt.CREATE:
                return R.string.location_mode_battery_saving_title;
            case DefaultWfcSettingsExt.DESTROY:
                return R.string.location_mode_high_accuracy_title;
            default:
                return 0;
        }
    }

    @Override
    public void onModeChanged(int mode, boolean restricted) {
        boolean z = false;
        int modeDescription = getLocationString(mode);
        if (modeDescription != 0) {
            this.mLocationMode.setSummary(modeDescription);
        }
        boolean enabled = mode != 0;
        RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(getActivity(), "no_share_location", UserHandle.myUserId());
        boolean hasBaseUserRestriction = RestrictedLockUtils.hasBaseUserRestriction(getActivity(), "no_share_location", UserHandle.myUserId());
        if (!hasBaseUserRestriction && admin != null) {
            this.mSwitchBar.setDisabledByAdmin(admin);
        } else {
            this.mSwitchBar.setEnabled(!restricted);
        }
        Preference preference = this.mLocationMode;
        if (enabled && !restricted) {
            z = true;
        }
        preference.setEnabled(z);
        this.mExt.updateCustomizedLocationSettings();
        this.mCategoryRecentLocationRequests.setEnabled(enabled);
        if (enabled != this.mSwitch.isChecked()) {
            if (this.mValidListener) {
                this.mSwitchBar.removeOnSwitchChangeListener(this);
            }
            this.mSwitch.setChecked(enabled);
            if (this.mValidListener) {
                this.mSwitchBar.addOnSwitchChangeListener(this);
            }
        }
        changeManagedProfileLocationAccessStatus(enabled);
        this.injector.reloadStatusMessages();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (isChecked) {
            setLocationMode(-1);
        } else {
            setLocationMode(0);
        }
    }

    private boolean isManagedProfileRestrictedByBase() {
        if (this.mManagedProfile == null) {
            return false;
        }
        return this.mUm.hasBaseUserRestriction("no_share_location", this.mManagedProfile);
    }

    private class PackageEntryClickedListener implements Preference.OnPreferenceClickListener {
        private String mPackage;
        private UserHandle mUserHandle;

        public PackageEntryClickedListener(String packageName, UserHandle userHandle) {
            this.mPackage = packageName;
            this.mUserHandle = userHandle;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            Bundle args = new Bundle();
            args.putString("package", this.mPackage);
            ((SettingsActivity) LocationSettings.this.getActivity()).startPreferencePanelAsUser(InstalledAppDetails.class.getName(), args, R.string.application_info_label, null, this.mUserHandle);
            return true;
        }
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean listening) {
            if (!listening) {
                return;
            }
            int mode = Settings.Secure.getInt(this.mContext.getContentResolver(), "location_mode", 0);
            if (mode != 0) {
                this.mSummaryLoader.setSummary(this, this.mContext.getString(R.string.location_on_summary, this.mContext.getString(LocationSettings.getLocationString(mode))));
            } else {
                this.mSummaryLoader.setSummary(this, this.mContext.getString(R.string.location_off_summary));
            }
        }
    }
}
