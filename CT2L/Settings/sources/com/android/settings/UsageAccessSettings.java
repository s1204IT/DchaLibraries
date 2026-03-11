package com.android.settings;

import android.app.ActivityThread;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.content.PackageMonitor;
import java.util.List;

public class UsageAccessSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
    AppOpsManager mAppOpsManager;
    private AppsRequestingAccessFetcher mLastFetcherTask;
    ArrayMap<String, PackageEntry> mPackageEntryMap = new ArrayMap<>();
    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        public void onPackageAdded(String packageName, int uid) {
            UsageAccessSettings.this.updateInterestedApps();
        }

        public void onPackageRemoved(String packageName, int uid) {
            UsageAccessSettings.this.updateInterestedApps();
        }
    };
    PreferenceScreen mPreferenceScreen;
    private static final String[] PM_USAGE_STATS_PERMISSION = {"android.permission.PACKAGE_USAGE_STATS"};
    private static final int[] APP_OPS_OP_CODES = {43};

    private static class PackageEntry {
        int appOpMode = 3;
        PackageInfo packageInfo;
        final String packageName;
        boolean permissionGranted;
        SwitchPreference preference;

        public PackageEntry(String packageName) {
            this.packageName = packageName;
        }
    }

    private class AppsRequestingAccessFetcher extends AsyncTask<Void, Void, ArrayMap<String, PackageEntry>> {
        private final Context mContext;
        private final IPackageManager mIPackageManager = ActivityThread.getPackageManager();
        private final PackageManager mPackageManager;

        public AppsRequestingAccessFetcher(Context context) {
            this.mContext = context;
            this.mPackageManager = context.getPackageManager();
        }

        @Override
        public ArrayMap<String, PackageEntry> doInBackground(Void... params) {
            try {
                String[] packages = this.mIPackageManager.getAppOpPermissionPackages("android.permission.PACKAGE_USAGE_STATS");
                if (packages == null) {
                    return null;
                }
                ArrayMap<String, PackageEntry> entries = new ArrayMap<>();
                for (String packageName : packages) {
                    if (!UsageAccessSettings.shouldIgnorePackage(packageName)) {
                        entries.put(packageName, new PackageEntry(packageName));
                    }
                }
                List<PackageInfo> packageInfos = this.mPackageManager.getPackagesHoldingPermissions(UsageAccessSettings.PM_USAGE_STATS_PERMISSION, 0);
                int packageInfoCount = packageInfos != null ? packageInfos.size() : 0;
                for (int i = 0; i < packageInfoCount; i++) {
                    PackageInfo packageInfo = packageInfos.get(i);
                    PackageEntry pe = entries.get(packageInfo.packageName);
                    if (pe != null) {
                        pe.packageInfo = packageInfo;
                        pe.permissionGranted = true;
                    }
                }
                int packageCount = entries.size();
                int i2 = 0;
                while (i2 < packageCount) {
                    PackageEntry pe2 = entries.valueAt(i2);
                    if (pe2.packageInfo == null) {
                        try {
                            pe2.packageInfo = this.mPackageManager.getPackageInfo(pe2.packageName, 0);
                        } catch (PackageManager.NameNotFoundException e) {
                            entries.removeAt(i2);
                            i2--;
                            packageCount--;
                        }
                    }
                    i2++;
                }
                List<AppOpsManager.PackageOps> packageOps = UsageAccessSettings.this.mAppOpsManager.getPackagesForOps(UsageAccessSettings.APP_OPS_OP_CODES);
                int packageOpsCount = packageOps != null ? packageOps.size() : 0;
                for (int i3 = 0; i3 < packageOpsCount; i3++) {
                    AppOpsManager.PackageOps packageOp = packageOps.get(i3);
                    PackageEntry pe3 = entries.get(packageOp.getPackageName());
                    if (pe3 == null) {
                        Log.w("UsageAccessSettings", "AppOp permission exists for package " + packageOp.getPackageName() + " but package doesn't exist or did not request UsageStats access");
                    } else if (packageOp.getUid() == pe3.packageInfo.applicationInfo.uid) {
                        if (packageOp.getOps().size() < 1) {
                            Log.w("UsageAccessSettings", "No AppOps permission exists for package " + packageOp.getPackageName());
                        } else {
                            pe3.appOpMode = ((AppOpsManager.OpEntry) packageOp.getOps().get(0)).getMode();
                        }
                    }
                }
                return entries;
            } catch (RemoteException e2) {
                Log.w("UsageAccessSettings", "PackageManager is dead. Can't get list of packages requesting android.permission.PACKAGE_USAGE_STATS");
                return null;
            }
        }

        @Override
        public void onPostExecute(ArrayMap<String, PackageEntry> newEntries) {
            UsageAccessSettings.this.mLastFetcherTask = null;
            if (UsageAccessSettings.this.getActivity() != null) {
                if (newEntries == null) {
                    UsageAccessSettings.this.mPackageEntryMap.clear();
                    UsageAccessSettings.this.mPreferenceScreen.removeAll();
                    return;
                }
                int oldPackageCount = UsageAccessSettings.this.mPackageEntryMap.size();
                for (int i = 0; i < oldPackageCount; i++) {
                    PackageEntry oldPackageEntry = UsageAccessSettings.this.mPackageEntryMap.valueAt(i);
                    PackageEntry newPackageEntry = newEntries.get(oldPackageEntry.packageName);
                    if (newPackageEntry == null) {
                        UsageAccessSettings.this.mPreferenceScreen.removePreference(oldPackageEntry.preference);
                    } else {
                        newPackageEntry.preference = oldPackageEntry.preference;
                    }
                }
                int packageCount = newEntries.size();
                for (int i2 = 0; i2 < packageCount; i2++) {
                    PackageEntry packageEntry = newEntries.valueAt(i2);
                    if (packageEntry.preference == null) {
                        packageEntry.preference = new SwitchPreference(this.mContext);
                        packageEntry.preference.setPersistent(false);
                        packageEntry.preference.setOnPreferenceChangeListener(UsageAccessSettings.this);
                        UsageAccessSettings.this.mPreferenceScreen.addPreference(packageEntry.preference);
                    }
                    updatePreference(packageEntry);
                }
                UsageAccessSettings.this.mPackageEntryMap.clear();
                UsageAccessSettings.this.mPackageEntryMap = newEntries;
            }
        }

        private void updatePreference(PackageEntry pe) {
            pe.preference.setIcon(pe.packageInfo.applicationInfo.loadIcon(this.mPackageManager));
            pe.preference.setTitle(pe.packageInfo.applicationInfo.loadLabel(this.mPackageManager));
            pe.preference.setKey(pe.packageName);
            boolean check = false;
            if (pe.appOpMode == 0) {
                check = true;
            } else if (pe.appOpMode == 3) {
                check = pe.permissionGranted;
            }
            if (check != pe.preference.isChecked()) {
                pe.preference.setChecked(check);
            }
        }
    }

    static boolean shouldIgnorePackage(String packageName) {
        return packageName.equals("android") || packageName.equals("com.android.settings");
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.usage_access_settings);
        this.mPreferenceScreen = getPreferenceScreen();
        this.mPreferenceScreen.setOrderingAsAdded(false);
        this.mAppOpsManager = (AppOpsManager) getSystemService("appops");
    }

    @Override
    public void onResume() {
        super.onResume();
        updateInterestedApps();
        this.mPackageMonitor.register(getActivity(), Looper.getMainLooper(), false);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mPackageMonitor.unregister();
        if (this.mLastFetcherTask != null) {
            this.mLastFetcherTask.cancel(true);
            this.mLastFetcherTask = null;
        }
    }

    public void updateInterestedApps() {
        if (this.mLastFetcherTask != null) {
            this.mLastFetcherTask.cancel(true);
        }
        this.mLastFetcherTask = new AppsRequestingAccessFetcher(getActivity());
        this.mLastFetcherTask.execute(new Void[0]);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String packageName = preference.getKey();
        PackageEntry pe = this.mPackageEntryMap.get(packageName);
        if (pe == null) {
            Log.w("UsageAccessSettings", "Preference change event for package " + packageName + " but that package is no longer valid.");
            return false;
        }
        if (!(newValue instanceof Boolean)) {
            Log.w("UsageAccessSettings", "Preference change event for package " + packageName + " had non boolean value of type " + newValue.getClass().getName());
            return false;
        }
        int newMode = ((Boolean) newValue).booleanValue() ? 0 : 1;
        if (pe.appOpMode == newMode) {
            return true;
        }
        if (newMode != 0) {
            setNewMode(pe, newMode);
            return true;
        }
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        Fragment prev = getChildFragmentManager().findFragmentByTag("warning");
        if (prev != null) {
            ft.remove(prev);
        }
        WarningDialogFragment.newInstance(pe.packageName).show(ft, "warning");
        return false;
    }

    void setNewMode(PackageEntry pe, int newMode) {
        this.mAppOpsManager.setMode(43, pe.packageInfo.applicationInfo.uid, pe.packageName, newMode);
        pe.appOpMode = newMode;
    }

    void allowAccess(String packageName) {
        PackageEntry entry = this.mPackageEntryMap.get(packageName);
        if (entry == null) {
            Log.w("UsageAccessSettings", "Unable to give access to package " + packageName + ": it does not exist.");
        } else {
            setNewMode(entry, 0);
            entry.preference.setChecked(true);
        }
    }

    public static class WarningDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
        public static WarningDialogFragment newInstance(String packageName) {
            WarningDialogFragment dialog = new WarningDialogFragment();
            Bundle args = new Bundle();
            args.putString("package", packageName);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity()).setTitle(R.string.allow_usage_access_title).setMessage(R.string.allow_usage_access_message).setIconAttribute(android.R.attr.alertDialogIcon).setNegativeButton(R.string.cancel, this).setPositiveButton(android.R.string.ok, this).create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == -1) {
                ((UsageAccessSettings) getParentFragment()).allowAccess(getArguments().getString("package"));
            } else {
                dialog.cancel();
            }
        }
    }
}
