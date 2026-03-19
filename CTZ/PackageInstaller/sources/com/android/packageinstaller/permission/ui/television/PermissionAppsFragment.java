package com.android.packageinstaller.permission.ui.television;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.DialogFragment;
import android.support.v4.util.ArrayMap;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.ArraySet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.android.packageinstaller.DeviceUtils;
import com.android.packageinstaller.R;
import com.android.packageinstaller.permission.model.AppPermissionGroup;
import com.android.packageinstaller.permission.model.PermissionApps;
import com.android.packageinstaller.permission.ui.ReviewPermissionsActivity;
import com.android.packageinstaller.permission.utils.LocationUtils;
import com.android.packageinstaller.permission.utils.SafetyNetLogger;
import com.android.packageinstaller.permission.utils.Utils;
import java.util.ArrayList;

public final class PermissionAppsFragment extends SettingsWithHeader implements Preference.OnPreferenceChangeListener, PermissionApps.Callback {
    private PreferenceScreen mExtraScreen;
    private boolean mHasConfirmedRevoke;
    private boolean mHasSystemApps;
    private MenuItem mHideSystemMenu;
    private ArraySet<String> mLauncherPkgs;
    private PermissionApps.Callback mOnPermissionsLoadedListener;
    private PermissionApps mPermissionApps;
    private boolean mShowSystem;
    private MenuItem mShowSystemMenu;
    private ArrayMap<String, AppPermissionGroup> mToggledGroups;

    public static PermissionAppsFragment newInstance(String str) {
        return (PermissionAppsFragment) setPermissionName(new PermissionAppsFragment(), str);
    }

    private static <T extends Fragment> T setPermissionName(T t, String str) {
        Bundle bundle = new Bundle();
        bundle.putString("android.intent.extra.PERMISSION_NAME", str);
        t.setArguments(bundle);
        return t;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setLoading(true, false);
        setHasOptionsMenu(true);
        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        this.mLauncherPkgs = Utils.getLauncherPackages(getContext());
        this.mPermissionApps = new PermissionApps(getActivity(), getArguments().getString("android.intent.extra.PERMISSION_NAME"), this);
        this.mPermissionApps.refresh(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mPermissionApps.refresh(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        if (this.mHasSystemApps) {
            this.mShowSystemMenu = menu.add(0, 1, 0, R.string.menu_show_system);
            this.mHideSystemMenu = menu.add(0, 2, 0, R.string.menu_hide_system);
            updateMenu();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == 16908332) {
            getActivity().finish();
            return true;
        }
        switch (itemId) {
            case DialogFragment.STYLE_NO_TITLE:
            case DialogFragment.STYLE_NO_FRAME:
                this.mShowSystem = menuItem.getItemId() == 1;
                if (this.mPermissionApps.getApps() != null) {
                    onPermissionsLoaded(this.mPermissionApps);
                }
                updateMenu();
                break;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void updateMenu() {
        this.mShowSystemMenu.setVisible(!this.mShowSystem);
        this.mHideSystemMenu.setVisible(this.mShowSystem);
    }

    @Override
    protected void onSetEmptyText(TextView textView) {
        textView.setText(R.string.no_apps);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        bindUi(this, this.mPermissionApps);
    }

    private static void bindUi(SettingsWithHeader settingsWithHeader, PermissionApps permissionApps) {
        permissionApps.getIcon();
        settingsWithHeader.setHeader(null, null, null, settingsWithHeader.getString(R.string.permission_apps_decor_title, new Object[]{permissionApps.getLabel()}));
    }

    private void setOnPermissionsLoadedListener(PermissionApps.Callback callback) {
        this.mOnPermissionsLoadedListener = callback;
    }

    @Override
    public void onPermissionsLoaded(PermissionApps permissionApps) {
        Preference preferenceFindPreference;
        Context context = getPreferenceManager().getContext();
        if (context == null) {
            return;
        }
        boolean zIsTelevision = DeviceUtils.isTelevision(context);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        ArraySet<String> arraySet = new ArraySet();
        int preferenceCount = preferenceScreen.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            arraySet.add(preferenceScreen.getPreference(i).getKey());
        }
        if (this.mExtraScreen != null) {
            int preferenceCount2 = this.mExtraScreen.getPreferenceCount();
            for (int i2 = 0; i2 < preferenceCount2; i2++) {
                arraySet.add(this.mExtraScreen.getPreference(i2).getKey());
            }
        }
        this.mHasSystemApps = false;
        boolean z = false;
        for (PermissionApps.PermissionApp permissionApp : permissionApps.getApps()) {
            if (Utils.shouldShowPermission(permissionApp)) {
                String key = permissionApp.getKey();
                arraySet.remove(key);
                Preference preferenceFindPreference2 = preferenceScreen.findPreference(key);
                if (preferenceFindPreference2 == null && this.mExtraScreen != null) {
                    preferenceFindPreference2 = this.mExtraScreen.findPreference(key);
                }
                boolean zIsSystem = Utils.isSystem(permissionApp, this.mLauncherPkgs);
                if (zIsSystem && !z) {
                    this.mHasSystemApps = true;
                    getActivity().invalidateOptionsMenu();
                    z = true;
                }
                if (!zIsSystem || zIsTelevision || this.mShowSystem) {
                    if (preferenceFindPreference2 != null) {
                        if (permissionApp.isPolicyFixed()) {
                            preferenceFindPreference2.setSummary(getString(R.string.permission_summary_enforced_by_policy));
                        }
                        preferenceFindPreference2.setPersistent(false);
                        preferenceFindPreference2.setEnabled(true ^ permissionApp.isPolicyFixed());
                        if (preferenceFindPreference2 instanceof SwitchPreference) {
                            ((SwitchPreference) preferenceFindPreference2).setChecked(permissionApp.areRuntimePermissionsGranted());
                        }
                    } else {
                        SwitchPreference switchPreference = new SwitchPreference(context);
                        switchPreference.setOnPreferenceChangeListener(this);
                        switchPreference.setKey(permissionApp.getKey());
                        switchPreference.setIcon(permissionApp.getIcon());
                        switchPreference.setTitle(permissionApp.getLabel());
                        if (permissionApp.isPolicyFixed()) {
                            switchPreference.setSummary(getString(R.string.permission_summary_enforced_by_policy));
                        }
                        switchPreference.setPersistent(false);
                        switchPreference.setEnabled(true ^ permissionApp.isPolicyFixed());
                        switchPreference.setChecked(permissionApp.areRuntimePermissionsGranted());
                        if (zIsSystem && zIsTelevision) {
                            if (this.mExtraScreen == null) {
                                this.mExtraScreen = getPreferenceManager().createPreferenceScreen(context);
                            }
                            this.mExtraScreen.addPreference(switchPreference);
                        } else {
                            preferenceScreen.addPreference(switchPreference);
                        }
                    }
                } else if (preferenceFindPreference2 != null) {
                    preferenceScreen.removePreference(preferenceFindPreference2);
                }
            }
        }
        if (this.mExtraScreen != null) {
            arraySet.remove("_showSystem");
            Preference preferenceFindPreference3 = preferenceScreen.findPreference("_showSystem");
            if (preferenceFindPreference3 == null) {
                preferenceFindPreference3 = new Preference(context);
                preferenceFindPreference3.setKey("_showSystem");
                preferenceFindPreference3.setIcon(Utils.applyTint(context, R.drawable.ic_toc, android.R.attr.colorControlNormal));
                preferenceFindPreference3.setTitle(R.string.preference_show_system_apps);
                preferenceFindPreference3.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        SystemAppsFragment systemAppsFragment = new SystemAppsFragment();
                        PermissionAppsFragment.setPermissionName(systemAppsFragment, PermissionAppsFragment.this.getArguments().getString("android.intent.extra.PERMISSION_NAME"));
                        systemAppsFragment.setTargetFragment(PermissionAppsFragment.this, 0);
                        PermissionAppsFragment.this.getFragmentManager().beginTransaction().replace(android.R.id.content, systemAppsFragment).addToBackStack("SystemApps").commit();
                        return true;
                    }
                });
                preferenceScreen.addPreference(preferenceFindPreference3);
            }
            int preferenceCount3 = this.mExtraScreen.getPreferenceCount();
            int i3 = 0;
            for (int i4 = 0; i4 < preferenceCount3; i4++) {
                if (((SwitchPreference) this.mExtraScreen.getPreference(i4)).isChecked()) {
                    i3++;
                }
            }
            preferenceFindPreference3.setSummary(getString(R.string.app_permissions_group_summary, new Object[]{Integer.valueOf(i3), Integer.valueOf(this.mExtraScreen.getPreferenceCount())}));
        }
        for (String str : arraySet) {
            Preference preferenceFindPreference4 = preferenceScreen.findPreference(str);
            if (preferenceFindPreference4 != null) {
                preferenceScreen.removePreference(preferenceFindPreference4);
            } else if (this.mExtraScreen != null && (preferenceFindPreference = this.mExtraScreen.findPreference(str)) != null) {
                this.mExtraScreen.removePreference(preferenceFindPreference);
            }
        }
        setLoading(false, true);
        if (this.mOnPermissionsLoadedListener != null) {
            this.mOnPermissionsLoadedListener.onPermissionsLoaded(permissionApps);
        }
    }

    @Override
    public boolean onPreferenceChange(final Preference preference, Object obj) {
        final PermissionApps.PermissionApp app = this.mPermissionApps.getApp(preference.getKey());
        if (app == null) {
            return false;
        }
        if (LocationUtils.isLocationGroupAndProvider(this.mPermissionApps.getGroupName(), app.getPackageName())) {
            LocationUtils.showLocationDialog(getContext(), app.getLabel());
            return false;
        }
        addToggledGroup(app.getPackageName(), app.getPermissionGroup());
        if (app.isReviewRequired()) {
            Intent intent = new Intent(getActivity(), (Class<?>) ReviewPermissionsActivity.class);
            intent.putExtra("android.intent.extra.PACKAGE_NAME", app.getPackageName());
            startActivity(intent);
            return false;
        }
        if (obj == Boolean.TRUE) {
            app.grantRuntimePermissions();
            return true;
        }
        final boolean zHasGrantedByDefaultPermissions = app.hasGrantedByDefaultPermissions();
        if (zHasGrantedByDefaultPermissions || (!app.doesSupportRuntimePermissions() && !this.mHasConfirmedRevoke)) {
            new AlertDialog.Builder(getContext()).setMessage(zHasGrantedByDefaultPermissions ? R.string.system_warning : R.string.old_sdk_deny_warning).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.grant_dialog_button_deny_anyway, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ((SwitchPreference) preference).setChecked(false);
                    app.revokeRuntimePermissions();
                    if (!zHasGrantedByDefaultPermissions) {
                        PermissionAppsFragment.this.mHasConfirmedRevoke = true;
                    }
                }
            }).show();
            return false;
        }
        app.revokeRuntimePermissions();
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        logToggledGroups();
    }

    private void addToggledGroup(String str, AppPermissionGroup appPermissionGroup) {
        if (this.mToggledGroups == null) {
            this.mToggledGroups = new ArrayMap<>();
        }
        if (this.mToggledGroups.containsKey(str)) {
            this.mToggledGroups.remove(str);
        } else {
            this.mToggledGroups.put(str, appPermissionGroup);
        }
    }

    private void logToggledGroups() {
        if (this.mToggledGroups != null) {
            int size = this.mToggledGroups.size();
            for (int i = 0; i < size; i++) {
                String strKeyAt = this.mToggledGroups.keyAt(i);
                ArrayList arrayList = new ArrayList();
                arrayList.add(this.mToggledGroups.valueAt(i));
                SafetyNetLogger.logPermissionsToggled(strKeyAt, arrayList);
            }
            this.mToggledGroups = null;
        }
    }

    public static class SystemAppsFragment extends SettingsWithHeader implements PermissionApps.Callback {
        PermissionAppsFragment mOuterFragment;

        @Override
        public void onCreate(Bundle bundle) {
            this.mOuterFragment = (PermissionAppsFragment) getTargetFragment();
            setLoading(true, false);
            super.onCreate(bundle);
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String str) {
            if (this.mOuterFragment.mExtraScreen == null) {
                this.mOuterFragment.setOnPermissionsLoadedListener(this);
            } else {
                setPreferenceScreen();
            }
        }

        @Override
        public void onViewCreated(View view, Bundle bundle) {
            super.onViewCreated(view, bundle);
            bindUi(this, new PermissionApps(getActivity(), getArguments().getString("android.intent.extra.PERMISSION_NAME"), null));
        }

        @Override
        public void onResume() {
            super.onResume();
            this.mOuterFragment.mPermissionApps.refresh(true);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            this.mOuterFragment.setOnPermissionsLoadedListener(null);
        }

        private static void bindUi(SettingsWithHeader settingsWithHeader, PermissionApps permissionApps) {
            settingsWithHeader.setHeader(null, null, null, settingsWithHeader.getString(R.string.system_apps_decor_title, new Object[]{permissionApps.getLabel()}));
        }

        @Override
        public void onPermissionsLoaded(PermissionApps permissionApps) {
            setPreferenceScreen();
        }

        private void setPreferenceScreen() {
            setPreferenceScreen(this.mOuterFragment.mExtraScreen);
            setLoading(false, true);
        }
    }
}
