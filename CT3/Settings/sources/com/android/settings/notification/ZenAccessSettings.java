package com.android.settings.notification;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.ArraySet;
import android.view.View;
import com.android.settings.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ZenAccessSettings extends EmptyTextSettings {
    private Context mContext;
    private NotificationManager mNoMan;
    private final SettingObserver mObserver = new SettingObserver();
    private PackageManager mPkgMan;

    @Override
    protected int getMetricsCategory() {
        return 180;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mContext = getActivity();
        this.mPkgMan = this.mContext.getPackageManager();
        this.mNoMan = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(this.mContext));
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setEmptyText(R.string.zen_access_empty_text);
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadList();
        getContentResolver().registerContentObserver(Settings.Secure.getUriFor("enabled_notification_policy_access_packages"), false, this.mObserver);
        getContentResolver().registerContentObserver(Settings.Secure.getUriFor("enabled_notification_listeners"), false, this.mObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(this.mObserver);
    }

    public void reloadList() {
        List<ApplicationInfo> installed;
        PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();
        ArrayList<ApplicationInfo> apps = new ArrayList<>();
        ArraySet<String> requesting = this.mNoMan.getPackagesRequestingNotificationPolicyAccess();
        if (!requesting.isEmpty() && (installed = this.mPkgMan.getInstalledApplications(0)) != null) {
            for (ApplicationInfo app : installed) {
                if (requesting.contains(app.packageName)) {
                    apps.add(app);
                }
            }
        }
        ArraySet<String> autoApproved = getEnabledNotificationListeners();
        requesting.addAll((ArraySet<? extends String>) autoApproved);
        Collections.sort(apps, new PackageItemInfo.DisplayNameComparator(this.mPkgMan));
        for (ApplicationInfo app2 : apps) {
            final String pkg = app2.packageName;
            final CharSequence label = app2.loadLabel(this.mPkgMan);
            SwitchPreference pref = new SwitchPreference(getPrefContext());
            pref.setPersistent(false);
            pref.setIcon(app2.loadIcon(this.mPkgMan));
            pref.setTitle(label);
            pref.setChecked(hasAccess(pkg));
            if (autoApproved.contains(pkg)) {
                pref.setEnabled(false);
                pref.setSummary(getString(R.string.zen_access_disabled_package_warning));
            }
            pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean access = ((Boolean) newValue).booleanValue();
                    if (access) {
                        new ScaryWarningDialogFragment().setPkgInfo(pkg, label).show(ZenAccessSettings.this.getFragmentManager(), "dialog");
                        return false;
                    }
                    new FriendlyWarningDialogFragment().setPkgInfo(pkg, label).show(ZenAccessSettings.this.getFragmentManager(), "dialog");
                    return false;
                }
            });
            screen.addPreference(pref);
        }
    }

    private ArraySet<String> getEnabledNotificationListeners() {
        ArraySet<String> packages = new ArraySet<>();
        String settingValue = Settings.Secure.getString(getContext().getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(settingValue)) {
            String[] restored = settingValue.split(":");
            for (String str : restored) {
                ComponentName value = ComponentName.unflattenFromString(str);
                if (value != null) {
                    packages.add(value.getPackageName());
                }
            }
        }
        return packages;
    }

    private boolean hasAccess(String pkg) {
        return this.mNoMan.isNotificationPolicyAccessGrantedForPackage(pkg);
    }

    public static void setAccess(final Context context, final String pkg, final boolean access) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                NotificationManager mgr = (NotificationManager) context.getSystemService(NotificationManager.class);
                mgr.setNotificationPolicyAccessGranted(pkg, access);
            }
        });
    }

    public static void deleteRules(final Context context, final String pkg) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                NotificationManager mgr = (NotificationManager) context.getSystemService(NotificationManager.class);
                mgr.removeAutomaticZenRules(pkg);
            }
        });
    }

    private final class SettingObserver extends ContentObserver {
        public SettingObserver() {
            super(new Handler(Looper.getMainLooper()));
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            ZenAccessSettings.this.reloadList();
        }
    }

    public static class ScaryWarningDialogFragment extends DialogFragment {
        public ScaryWarningDialogFragment setPkgInfo(String pkg, CharSequence label) {
            Bundle args = new Bundle();
            args.putString("p", pkg);
            if (!TextUtils.isEmpty(label)) {
                pkg = label.toString();
            }
            args.putString("l", pkg);
            setArguments(args);
            return this;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle args = getArguments();
            final String pkg = args.getString("p");
            String label = args.getString("l");
            String title = getResources().getString(R.string.zen_access_warning_dialog_title, label);
            String summary = getResources().getString(R.string.zen_access_warning_dialog_summary);
            return new AlertDialog.Builder(getContext()).setMessage(summary).setTitle(title).setCancelable(true).setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    ZenAccessSettings.setAccess(ScaryWarningDialogFragment.this.getContext(), pkg, true);
                }
            }).setNegativeButton(R.string.deny, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                }
            }).create();
        }
    }

    public static class FriendlyWarningDialogFragment extends DialogFragment {
        public FriendlyWarningDialogFragment setPkgInfo(String pkg, CharSequence label) {
            Bundle args = new Bundle();
            args.putString("p", pkg);
            if (!TextUtils.isEmpty(label)) {
                pkg = label.toString();
            }
            args.putString("l", pkg);
            setArguments(args);
            return this;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle args = getArguments();
            final String pkg = args.getString("p");
            String label = args.getString("l");
            String title = getResources().getString(R.string.zen_access_revoke_warning_dialog_title, label);
            String summary = getResources().getString(R.string.zen_access_revoke_warning_dialog_summary);
            return new AlertDialog.Builder(getContext()).setMessage(summary).setTitle(title).setCancelable(true).setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    ZenAccessSettings.deleteRules(FriendlyWarningDialogFragment.this.getContext(), pkg);
                    ZenAccessSettings.setAccess(FriendlyWarningDialogFragment.this.getContext(), pkg, false);
                }
            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                }
            }).create();
        }
    }
}
