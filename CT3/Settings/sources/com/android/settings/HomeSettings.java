package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import java.util.ArrayList;
import java.util.List;

public class HomeSettings extends SettingsPreferenceFragment implements Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList<>();
            PackageManager pm = context.getPackageManager();
            ArrayList<ResolveInfo> homeActivities = new ArrayList<>();
            pm.getHomeActivities(homeActivities);
            SharedPreferences sp = context.getSharedPreferences("home_prefs", 0);
            boolean doShowHome = sp.getBoolean("do_show", false);
            if (homeActivities.size() > 1 || doShowHome) {
                Resources res = context.getResources();
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.home_settings);
                data.screenTitle = res.getString(R.string.home_settings);
                data.keywords = res.getString(R.string.keywords_home);
                result.add(data);
                for (int i = 0; i < homeActivities.size(); i++) {
                    ResolveInfo resolveInfo = homeActivities.get(i);
                    ActivityInfo activityInfo = resolveInfo.activityInfo;
                    try {
                        CharSequence name = activityInfo.loadLabel(pm);
                        if (!TextUtils.isEmpty(name)) {
                            SearchIndexableRaw data2 = new SearchIndexableRaw(context);
                            data2.title = name.toString();
                            data2.screenTitle = res.getString(R.string.home_settings);
                            result.add(data2);
                        }
                    } catch (Exception e) {
                        Log.v("HomeSettings", "Problem dealing with Home " + activityInfo.name, e);
                    }
                }
            }
            return result;
        }
    };
    private ComponentName[] mHomeComponentSet;
    private PackageManager mPm;
    private PreferenceGroup mPrefGroup;
    private ArrayList<HomeAppPreference> mPrefs;
    private boolean mShowNotice;
    private HomeAppPreference mCurrentHome = null;
    private HomePackageReceiver mHomePackageReceiver = new HomePackageReceiver(this, null);
    View.OnClickListener mHomeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int index = ((Integer) v.getTag()).intValue();
            HomeAppPreference pref = (HomeAppPreference) HomeSettings.this.mPrefs.get(index);
            if (pref.isChecked) {
                return;
            }
            HomeSettings.this.makeCurrentHome(pref);
        }
    };
    View.OnClickListener mDeleteClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int index = ((Integer) v.getTag()).intValue();
            HomeSettings.this.uninstallApp((HomeAppPreference) HomeSettings.this.mPrefs.get(index));
        }
    };
    private final IntentFilter mHomeFilter = new IntentFilter("android.intent.action.MAIN");

    private class HomePackageReceiver extends BroadcastReceiver {
        HomePackageReceiver(HomeSettings this$0, HomePackageReceiver homePackageReceiver) {
            this();
        }

        private HomePackageReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            HomeSettings.this.buildHomeActivitiesList();
            Index.getInstance(context).updateFromClassNameResource(HomeSettings.class.getName(), true, true);
        }
    }

    public HomeSettings() {
        this.mHomeFilter.addCategory("android.intent.category.HOME");
        this.mHomeFilter.addCategory("android.intent.category.DEFAULT");
    }

    void makeCurrentHome(HomeAppPreference newHome) {
        if (this.mCurrentHome != null) {
            this.mCurrentHome.setChecked(false);
        }
        newHome.setChecked(true);
        this.mCurrentHome = newHome;
        this.mPm.replacePreferredActivity(this.mHomeFilter, 1048576, this.mHomeComponentSet, newHome.activityName);
        getActivity().setResult(-1);
    }

    void uninstallApp(HomeAppPreference pref) {
        Uri packageURI = Uri.parse("package:" + pref.uninstallTarget);
        Intent uninstallIntent = new Intent("android.intent.action.UNINSTALL_PACKAGE", packageURI);
        uninstallIntent.putExtra("android.intent.extra.UNINSTALL_ALL_USERS", false);
        int requestCode = (pref.isChecked ? 1 : 0) + 10;
        startActivityForResult(uninstallIntent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        buildHomeActivitiesList();
        if (requestCode <= 10 || this.mCurrentHome != null) {
            return;
        }
        for (int i = 0; i < this.mPrefs.size(); i++) {
            HomeAppPreference pref = this.mPrefs.get(i);
            if (pref.isSystem) {
                makeCurrentHome(pref);
                return;
            }
        }
    }

    public void buildHomeActivitiesList() {
        boolean z;
        HomeAppPreference pref;
        ArrayList<ResolveInfo> homeActivities = new ArrayList<>();
        ComponentName currentDefaultHome = this.mPm.getHomeActivities(homeActivities);
        Context context = getPrefContext();
        this.mCurrentHome = null;
        this.mPrefGroup.removeAll();
        this.mPrefs = new ArrayList<>();
        this.mHomeComponentSet = new ComponentName[homeActivities.size()];
        int prefIndex = 0;
        boolean supportManagedProfilesExtra = getActivity().getIntent().getBooleanExtra("support_managed_profiles", false);
        if (hasManagedProfile()) {
            z = true;
        } else {
            z = supportManagedProfilesExtra;
        }
        for (int i = 0; i < homeActivities.size(); i++) {
            ResolveInfo candidate = homeActivities.get(i);
            ActivityInfo info = candidate.activityInfo;
            ComponentName activityName = new ComponentName(info.packageName, info.name);
            this.mHomeComponentSet[i] = activityName;
            try {
                Drawable icon = info.loadIcon(this.mPm);
                CharSequence name = info.loadLabel(this.mPm);
                if (z && !launcherHasManagedProfilesFeature(candidate)) {
                    pref = new HomeAppPreference(context, activityName, prefIndex, icon, name, this, info, false, getResources().getString(R.string.home_work_profile_not_supported));
                } else {
                    pref = new HomeAppPreference(context, activityName, prefIndex, icon, name, this, info, true, null);
                }
                this.mPrefs.add(pref);
                this.mPrefGroup.addPreference(pref);
                if (activityName.equals(currentDefaultHome)) {
                    this.mCurrentHome = pref;
                }
                prefIndex++;
            } catch (Exception e) {
                Log.v("HomeSettings", "Problem dealing with activity " + activityName, e);
            }
        }
        if (this.mCurrentHome == null) {
            return;
        }
        if (this.mCurrentHome.isEnabled()) {
            getActivity().setResult(-1);
        }
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                HomeSettings.this.mCurrentHome.setChecked(true);
            }
        });
    }

    private boolean hasManagedProfile() {
        Context context = getActivity();
        UserManager userManager = (UserManager) getSystemService("user");
        List<UserInfo> profiles = userManager.getProfiles(context.getUserId());
        for (UserInfo userInfo : profiles) {
            if (userInfo.isManagedProfile()) {
                return true;
            }
        }
        return false;
    }

    private boolean launcherHasManagedProfilesFeature(ResolveInfo resolveInfo) {
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(resolveInfo.activityInfo.packageName, 0);
            return versionNumberAtLeastL(appInfo.targetSdkVersion);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean versionNumberAtLeastL(int versionNumber) {
        return versionNumber >= 21;
    }

    @Override
    protected int getMetricsCategory() {
        return 55;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.home_selection);
        this.mPm = getPackageManager();
        this.mPrefGroup = (PreferenceGroup) findPreference("home");
        Bundle args = getArguments();
        this.mShowNotice = args != null ? args.getBoolean("show", false) : false;
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addDataScheme("package");
        getActivity().registerReceiver(this.mHomePackageReceiver, filter);
        buildHomeActivitiesList();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(this.mHomePackageReceiver);
    }

    private class HomeAppPreference extends Preference {
        ComponentName activityName;
        HomeSettings fragment;
        final ColorFilter grayscaleFilter;
        int index;
        boolean isChecked;
        boolean isSystem;
        String uninstallTarget;

        public HomeAppPreference(Context context, ComponentName activity, int i, Drawable icon, CharSequence title, HomeSettings parent, ActivityInfo info, boolean enabled, CharSequence summary) {
            super(context);
            setLayoutResource(R.layout.preference_home_app);
            setIcon(icon);
            setTitle(title);
            setEnabled(enabled);
            setSummary(summary);
            this.activityName = activity;
            this.fragment = parent;
            this.index = i;
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0.0f);
            float[] matrix = colorMatrix.getArray();
            matrix[18] = 0.5f;
            this.grayscaleFilter = new ColorMatrixColorFilter(colorMatrix);
            determineTargets(info);
        }

        private void determineTargets(ActivityInfo info) {
            String altHomePackage;
            Bundle meta = info.metaData;
            if (meta != null && (altHomePackage = meta.getString("android.app.home.alternate")) != null) {
                try {
                    int match = HomeSettings.this.mPm.checkSignatures(info.packageName, altHomePackage);
                    if (match >= 0) {
                        PackageInfo altInfo = HomeSettings.this.mPm.getPackageInfo(altHomePackage, 0);
                        int altFlags = altInfo.applicationInfo.flags;
                        this.isSystem = (altFlags & 1) != 0;
                        this.uninstallTarget = altInfo.packageName;
                        return;
                    }
                } catch (Exception e) {
                    Log.w("HomeSettings", "Unable to compare/resolve alternate", e);
                }
            }
            this.isSystem = (info.applicationInfo.flags & 1) != 0;
            this.uninstallTarget = info.packageName;
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder view) {
            super.onBindViewHolder(view);
            RadioButton radio = (RadioButton) view.findViewById(R.id.home_radio);
            radio.setChecked(this.isChecked);
            Integer indexObj = new Integer(this.index);
            ImageView icon = (ImageView) view.findViewById(R.id.home_app_uninstall);
            if (this.isSystem) {
                icon.setEnabled(false);
                icon.setColorFilter(this.grayscaleFilter);
            } else {
                icon.setEnabled(true);
                icon.setOnClickListener(HomeSettings.this.mDeleteClickListener);
                icon.setTag(indexObj);
            }
            View v = view.findViewById(R.id.home_app_pref);
            v.setTag(indexObj);
            v.setOnClickListener(HomeSettings.this.mHomeClickListener);
        }

        void setChecked(boolean state) {
            if (state == this.isChecked) {
                return;
            }
            this.isChecked = state;
            notifyChanged();
        }
    }
}
