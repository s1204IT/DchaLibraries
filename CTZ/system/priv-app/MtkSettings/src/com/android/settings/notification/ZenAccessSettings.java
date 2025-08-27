package com.android.settings.notification;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.AppSwitchPreference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class ZenAccessSettings extends EmptyTextSettings {
    private Context mContext;
    private NotificationManager mNoMan;
    private PackageManager mPkgMan;
    private final String TAG = "ZenAccessSettings";
    private final SettingObserver mObserver = new SettingObserver();

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 180;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mContext = getActivity();
        this.mPkgMan = this.mContext.getPackageManager();
        this.mNoMan = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
    }

    @Override // com.android.settings.notification.EmptyTextSettings, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        setEmptyText(R.string.zen_access_empty_text);
    }

    @Override // com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.zen_access_settings;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        if (!ActivityManager.isLowRamDeviceStatic()) {
            reloadList();
            getContentResolver().registerContentObserver(Settings.Secure.getUriFor("enabled_notification_policy_access_packages"), false, this.mObserver);
            getContentResolver().registerContentObserver(Settings.Secure.getUriFor("enabled_notification_listeners"), false, this.mObserver);
            return;
        }
        setEmptyText(R.string.disabled_low_ram_device);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        super.onPause();
        if (!ActivityManager.isLowRamDeviceStatic()) {
            getContentResolver().unregisterContentObserver(this.mObserver);
        }
    }

    private void reloadList() {
        List<ApplicationInfo> installedApplications;
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();
        ArrayList arrayList = new ArrayList();
        ArraySet<String> packagesRequestingNotificationPolicyAccess = getPackagesRequestingNotificationPolicyAccess();
        if (!packagesRequestingNotificationPolicyAccess.isEmpty() && (installedApplications = this.mPkgMan.getInstalledApplications(0)) != null) {
            for (ApplicationInfo applicationInfo : installedApplications) {
                if (packagesRequestingNotificationPolicyAccess.contains(applicationInfo.packageName)) {
                    arrayList.add(applicationInfo);
                }
            }
        }
        ArraySet<? extends String> arraySet = new ArraySet<>();
        arraySet.addAll(this.mNoMan.getEnabledNotificationListenerPackages());
        packagesRequestingNotificationPolicyAccess.addAll(arraySet);
        Collections.sort(arrayList, new PackageItemInfo.DisplayNameComparator(this.mPkgMan));
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            ApplicationInfo applicationInfo2 = (ApplicationInfo) it.next();
            final String str = applicationInfo2.packageName;
            final CharSequence charSequenceLoadLabel = applicationInfo2.loadLabel(this.mPkgMan);
            AppSwitchPreference appSwitchPreference = new AppSwitchPreference(getPrefContext());
            appSwitchPreference.setKey(str);
            appSwitchPreference.setPersistent(false);
            appSwitchPreference.setIcon(applicationInfo2.loadIcon(this.mPkgMan));
            appSwitchPreference.setTitle(charSequenceLoadLabel);
            appSwitchPreference.setChecked(hasAccess(str));
            if (arraySet.contains(str)) {
                appSwitchPreference.setEnabled(false);
                appSwitchPreference.setSummary(getString(R.string.zen_access_disabled_package_warning));
            }
            appSwitchPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.android.settings.notification.ZenAccessSettings.1
                @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
                public boolean onPreferenceChange(Preference preference, Object obj) {
                    if (((Boolean) obj).booleanValue()) {
                        new ScaryWarningDialogFragment().setPkgInfo(str, charSequenceLoadLabel).show(ZenAccessSettings.this.getFragmentManager(), "dialog");
                        return false;
                    }
                    new FriendlyWarningDialogFragment().setPkgInfo(str, charSequenceLoadLabel).show(ZenAccessSettings.this.getFragmentManager(), "dialog");
                    return false;
                }
            });
            preferenceScreen.addPreference(appSwitchPreference);
        }
    }

    private ArraySet<String> getPackagesRequestingNotificationPolicyAccess() {
        ArraySet<String> arraySet = new ArraySet<>();
        try {
            List list = AppGlobals.getPackageManager().getPackagesHoldingPermissions(new String[]{"android.permission.ACCESS_NOTIFICATION_POLICY"}, 0, ActivityManager.getCurrentUser()).getList();
            if (list != null) {
                Iterator it = list.iterator();
                while (it.hasNext()) {
                    arraySet.add(((PackageInfo) it.next()).packageName);
                }
            }
        } catch (RemoteException e) {
            Log.e("ZenAccessSettings", "Cannot reach packagemanager", e);
        }
        return arraySet;
    }

    private boolean hasAccess(String str) {
        return this.mNoMan.isNotificationPolicyAccessGrantedForPackage(str);
    }

    private static void setAccess(final Context context, final String str, final boolean z) {
        logSpecialPermissionChange(z, str, context);
        AsyncTask.execute(new Runnable() { // from class: com.android.settings.notification.ZenAccessSettings.2
            @Override // java.lang.Runnable
            public void run() {
                ((NotificationManager) context.getSystemService(NotificationManager.class)).setNotificationPolicyAccessGranted(str, z);
            }
        });
    }

    @VisibleForTesting
    static void logSpecialPermissionChange(boolean z, String str, Context context) {
        FeatureFactory.getFactory(context).getMetricsFeatureProvider().action(context, z ? 768 : 769, str, new Pair[0]);
    }

    private static void deleteRules(final Context context, final String str) {
        AsyncTask.execute(new Runnable() { // from class: com.android.settings.notification.ZenAccessSettings.3
            @Override // java.lang.Runnable
            public void run() {
                ((NotificationManager) context.getSystemService(NotificationManager.class)).removeAutomaticZenRules(str);
            }
        });
    }

    private final class SettingObserver extends ContentObserver {
        public SettingObserver() {
            super(new Handler(Looper.getMainLooper()));
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean z, Uri uri) {
            ZenAccessSettings.this.reloadList();
        }
    }

    public static class ScaryWarningDialogFragment extends InstrumentedDialogFragment {
        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 554;
        }

        public ScaryWarningDialogFragment setPkgInfo(String str, CharSequence charSequence) {
            Bundle bundle = new Bundle();
            bundle.putString("p", str);
            if (!TextUtils.isEmpty(charSequence)) {
                str = charSequence.toString();
            }
            bundle.putString("l", str);
            setArguments(bundle);
            return this;
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            super.onCreate(bundle);
            Bundle arguments = getArguments();
            final String string = arguments.getString("p");
            return new AlertDialog.Builder(getContext()).setMessage(getResources().getString(R.string.zen_access_warning_dialog_summary)).setTitle(getResources().getString(R.string.zen_access_warning_dialog_title, arguments.getString("l"))).setCancelable(true).setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() { // from class: com.android.settings.notification.ZenAccessSettings.ScaryWarningDialogFragment.2
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i) {
                    ZenAccessSettings.setAccess(ScaryWarningDialogFragment.this.getContext(), string, true);
                }
            }).setNegativeButton(R.string.deny, new DialogInterface.OnClickListener() { // from class: com.android.settings.notification.ZenAccessSettings.ScaryWarningDialogFragment.1
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            }).create();
        }
    }

    public static class FriendlyWarningDialogFragment extends InstrumentedDialogFragment {
        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 555;
        }

        public FriendlyWarningDialogFragment setPkgInfo(String str, CharSequence charSequence) {
            Bundle bundle = new Bundle();
            bundle.putString("p", str);
            if (!TextUtils.isEmpty(charSequence)) {
                str = charSequence.toString();
            }
            bundle.putString("l", str);
            setArguments(bundle);
            return this;
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            super.onCreate(bundle);
            Bundle arguments = getArguments();
            final String string = arguments.getString("p");
            return new AlertDialog.Builder(getContext()).setMessage(getResources().getString(R.string.zen_access_revoke_warning_dialog_summary)).setTitle(getResources().getString(R.string.zen_access_revoke_warning_dialog_title, arguments.getString("l"))).setCancelable(true).setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() { // from class: com.android.settings.notification.ZenAccessSettings.FriendlyWarningDialogFragment.2
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i) {
                    ZenAccessSettings.deleteRules(FriendlyWarningDialogFragment.this.getContext(), string);
                    ZenAccessSettings.setAccess(FriendlyWarningDialogFragment.this.getContext(), string, false);
                }
            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() { // from class: com.android.settings.notification.ZenAccessSettings.FriendlyWarningDialogFragment.1
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            }).create();
        }
    }
}
