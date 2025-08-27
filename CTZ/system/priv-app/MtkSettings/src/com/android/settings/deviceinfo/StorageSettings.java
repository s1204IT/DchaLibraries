package com.android.settings.deviceinfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* loaded from: classes.dex */
public class StorageSettings extends SettingsPreferenceFragment implements Indexable {
    private static long sTotalInternalStorage;
    private PreferenceCategory mExternalCategory;
    private PreferenceCategory mInternalCategory;
    private StorageSummaryPreference mInternalSummary;
    private StorageManager mStorageManager;
    static final int COLOR_PUBLIC = Color.parseColor("#ff9e9e9e");
    static final int[] COLOR_PRIVATE = {Color.parseColor("#ff26a69a"), Color.parseColor("#ffab47bc"), Color.parseColor("#fff2a600"), Color.parseColor("#ffec407a"), Color.parseColor("#ffc0ca33")};
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() { // from class: com.android.settings.deviceinfo.-$$Lambda$StorageSettings$pjFUgWj2HWW95DLVydfI8EgfTdg
        @Override // com.android.settings.dashboard.SummaryLoader.SummaryProviderFactory
        public final SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return StorageSettings.lambda$static$0(activity, summaryLoader);
        }
    };
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.deviceinfo.StorageSettings.2
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableRaw searchIndexableRaw = new SearchIndexableRaw(context);
            searchIndexableRaw.title = context.getString(R.string.storage_settings);
            searchIndexableRaw.key = "storage_settings";
            searchIndexableRaw.screenTitle = context.getString(R.string.storage_settings);
            searchIndexableRaw.keywords = context.getString(R.string.keywords_storage_settings);
            arrayList.add(searchIndexableRaw);
            SearchIndexableRaw searchIndexableRaw2 = new SearchIndexableRaw(context);
            searchIndexableRaw2.title = context.getString(R.string.internal_storage);
            searchIndexableRaw2.key = "storage_settings_internal_storage";
            searchIndexableRaw2.screenTitle = context.getString(R.string.storage_settings);
            arrayList.add(searchIndexableRaw2);
            SearchIndexableRaw searchIndexableRaw3 = new SearchIndexableRaw(context);
            StorageManager storageManager = (StorageManager) context.getSystemService(StorageManager.class);
            for (VolumeInfo volumeInfo : storageManager.getVolumes()) {
                if (StorageSettings.isInteresting(volumeInfo)) {
                    searchIndexableRaw3.title = storageManager.getBestVolumeDescription(volumeInfo);
                    searchIndexableRaw3.key = "storage_settings_volume_" + volumeInfo.id;
                    searchIndexableRaw3.screenTitle = context.getString(R.string.storage_settings);
                    arrayList.add(searchIndexableRaw3);
                }
            }
            SearchIndexableRaw searchIndexableRaw4 = new SearchIndexableRaw(context);
            searchIndexableRaw4.title = context.getString(R.string.memory_size);
            searchIndexableRaw4.key = "storage_settings_memory_size";
            searchIndexableRaw4.screenTitle = context.getString(R.string.storage_settings);
            arrayList.add(searchIndexableRaw4);
            SearchIndexableRaw searchIndexableRaw5 = new SearchIndexableRaw(context);
            searchIndexableRaw5.title = context.getString(R.string.memory_available);
            searchIndexableRaw5.key = "storage_settings_memory_available";
            searchIndexableRaw5.screenTitle = context.getString(R.string.storage_settings);
            arrayList.add(searchIndexableRaw5);
            SearchIndexableRaw searchIndexableRaw6 = new SearchIndexableRaw(context);
            searchIndexableRaw6.title = context.getString(R.string.memory_apps_usage);
            searchIndexableRaw6.key = "storage_settings_apps_space";
            searchIndexableRaw6.screenTitle = context.getString(R.string.storage_settings);
            arrayList.add(searchIndexableRaw6);
            SearchIndexableRaw searchIndexableRaw7 = new SearchIndexableRaw(context);
            searchIndexableRaw7.title = context.getString(R.string.memory_dcim_usage);
            searchIndexableRaw7.key = "storage_settings_dcim_space";
            searchIndexableRaw7.screenTitle = context.getString(R.string.storage_settings);
            arrayList.add(searchIndexableRaw7);
            SearchIndexableRaw searchIndexableRaw8 = new SearchIndexableRaw(context);
            searchIndexableRaw8.title = context.getString(R.string.memory_music_usage);
            searchIndexableRaw8.key = "storage_settings_music_space";
            searchIndexableRaw8.screenTitle = context.getString(R.string.storage_settings);
            arrayList.add(searchIndexableRaw8);
            SearchIndexableRaw searchIndexableRaw9 = new SearchIndexableRaw(context);
            searchIndexableRaw9.title = context.getString(R.string.memory_media_misc_usage);
            searchIndexableRaw9.key = "storage_settings_misc_space";
            searchIndexableRaw9.screenTitle = context.getString(R.string.storage_settings);
            arrayList.add(searchIndexableRaw9);
            SearchIndexableRaw searchIndexableRaw10 = new SearchIndexableRaw(context);
            searchIndexableRaw10.title = context.getString(R.string.storage_menu_free);
            searchIndexableRaw10.key = "storage_settings_free_space";
            searchIndexableRaw10.screenTitle = context.getString(R.string.storage_menu_free);
            searchIndexableRaw10.intentAction = "android.os.storage.action.MANAGE_STORAGE";
            searchIndexableRaw10.intentTargetPackage = context.getString(R.string.config_deletion_helper_package);
            searchIndexableRaw10.intentTargetClass = context.getString(R.string.config_deletion_helper_class);
            arrayList.add(searchIndexableRaw10);
            return arrayList;
        }
    };
    private boolean mHasLaunchedPrivateVolumeSettings = false;
    private final StorageEventListener mStorageListener = new StorageEventListener() { // from class: com.android.settings.deviceinfo.StorageSettings.1
        public void onVolumeStateChanged(VolumeInfo volumeInfo, int i, int i2) {
            if (StorageSettings.isInteresting(volumeInfo)) {
                StorageSettings.this.refresh();
            }
        }

        public void onDiskDestroyed(DiskInfo diskInfo) {
            StorageSettings.this.refresh();
        }
    };

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 42;
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_uri_storage;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mStorageManager = (StorageManager) getActivity().getSystemService(StorageManager.class);
        if (sTotalInternalStorage <= 0) {
            sTotalInternalStorage = this.mStorageManager.getPrimaryStorageSize();
        }
        addPreferencesFromResource(R.xml.device_info_storage);
        this.mInternalCategory = (PreferenceCategory) findPreference("storage_internal");
        this.mExternalCategory = (PreferenceCategory) findPreference("storage_external");
        this.mInternalSummary = new StorageSummaryPreference(getPrefContext());
        setHasOptionsMenu(true);
    }

    private static boolean isInteresting(VolumeInfo volumeInfo) {
        switch (volumeInfo.getType()) {
            case 0:
            case 1:
                return true;
            default:
                return false;
        }
    }

    private synchronized void refresh() {
        Context prefContext = getPrefContext();
        getPreferenceScreen().removeAll();
        this.mInternalCategory.removeAll();
        this.mExternalCategory.removeAll();
        this.mInternalCategory.addPreference(this.mInternalSummary);
        PrivateStorageInfo privateStorageInfo = PrivateStorageInfo.getPrivateStorageInfo(new StorageManagerVolumeProvider(this.mStorageManager));
        long j = privateStorageInfo.totalBytes;
        long j2 = privateStorageInfo.totalBytes - privateStorageInfo.freeBytes;
        List<VolumeInfo> volumes = this.mStorageManager.getVolumes();
        Collections.sort(volumes, VolumeInfo.getDescriptionComparator());
        int i = 0;
        for (VolumeInfo volumeInfo : volumes) {
            if (volumeInfo.getType() == 1) {
                this.mInternalCategory.addPreference(new StorageVolumePreference(prefContext, volumeInfo, COLOR_PRIVATE[i % COLOR_PRIVATE.length], PrivateStorageInfo.getTotalSize(volumeInfo, sTotalInternalStorage)));
                i++;
            } else if (volumeInfo.getType() == 0) {
                this.mExternalCategory.addPreference(new StorageVolumePreference(prefContext, volumeInfo, COLOR_PUBLIC, 0L));
            }
        }
        for (VolumeRecord volumeRecord : this.mStorageManager.getVolumeRecords()) {
            if (volumeRecord.getType() == 1 && this.mStorageManager.findVolumeByUuid(volumeRecord.getFsUuid()) == null) {
                Drawable drawable = prefContext.getDrawable(R.drawable.ic_sim_sd);
                drawable.mutate();
                drawable.setTint(COLOR_PUBLIC);
                Preference preference = new Preference(prefContext);
                preference.setKey(volumeRecord.getFsUuid());
                preference.setTitle(volumeRecord.getNickname());
                preference.setSummary(android.R.string.config_bodyFontFamilyMedium);
                preference.setIcon(drawable);
                this.mInternalCategory.addPreference(preference);
            }
        }
        for (DiskInfo diskInfo : this.mStorageManager.getDisks()) {
            if (diskInfo.volumeCount == 0 && diskInfo.size > 0) {
                Preference preference2 = new Preference(prefContext);
                preference2.setKey(diskInfo.getId());
                preference2.setTitle(diskInfo.getDescription());
                preference2.setSummary(android.R.string.config_chooseTypeAndAccountActivity);
                preference2.setIcon(R.drawable.ic_sim_sd);
                this.mExternalCategory.addPreference(preference2);
            }
        }
        Formatter.BytesResult bytes = Formatter.formatBytes(getResources(), j2, 0);
        this.mInternalSummary.setTitle(TextUtils.expandTemplate(getText(R.string.storage_size_large), bytes.value, bytes.units));
        this.mInternalSummary.setSummary(getString(R.string.storage_volume_used_total, new Object[]{Formatter.formatFileSize(prefContext, j)}));
        if (this.mInternalCategory.getPreferenceCount() > 0) {
            getPreferenceScreen().addPreference(this.mInternalCategory);
        }
        if (this.mExternalCategory.getPreferenceCount() > 0) {
            getPreferenceScreen().addPreference(this.mExternalCategory);
        }
        if (this.mInternalCategory.getPreferenceCount() == 2 && this.mExternalCategory.getPreferenceCount() == 0 && !this.mHasLaunchedPrivateVolumeSettings) {
            this.mHasLaunchedPrivateVolumeSettings = true;
            Bundle bundle = new Bundle();
            bundle.putString("android.os.storage.extra.VOLUME_ID", "private");
            if (Utils.isMonkeyRunning()) {
                Log.e("StorageSettings", "Monkey test running so finishing storage manager dashboard settings");
            } else {
                new SubSettingLauncher(getActivity()).setDestination(StorageDashboardFragment.class.getName()).setArguments(bundle).setTitle(R.string.storage_settings).setSourceMetricsCategory(getMetricsCategory()).launch();
            }
            finish();
        }
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        this.mStorageManager.registerListener(this.mStorageListener);
        refresh();
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        super.onPause();
        this.mStorageManager.unregisterListener(this.mStorageListener);
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.support.v7.preference.PreferenceManager.OnPreferenceTreeClickListener
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();
        if (preference instanceof StorageVolumePreference) {
            VolumeInfo volumeInfoFindVolumeById = this.mStorageManager.findVolumeById(key);
            if (volumeInfoFindVolumeById == null) {
                return false;
            }
            if (volumeInfoFindVolumeById.getState() == 0) {
                VolumeUnmountedFragment.show(this, volumeInfoFindVolumeById.getId());
                return true;
            }
            if (volumeInfoFindVolumeById.getState() == 6) {
                DiskInitFragment.show(this, R.string.storage_dialog_unmountable, volumeInfoFindVolumeById.getDiskId());
                return true;
            }
            if (volumeInfoFindVolumeById.getType() == 1) {
                Bundle bundle = new Bundle();
                bundle.putString("android.os.storage.extra.VOLUME_ID", volumeInfoFindVolumeById.getId());
                if ("private".equals(volumeInfoFindVolumeById.getId())) {
                    if (Utils.isMonkeyRunning()) {
                        Log.e("StorageSettings", "Monkey test running so finishing storage manager dashboard settings");
                    } else {
                        new SubSettingLauncher(getContext()).setDestination(StorageDashboardFragment.class.getCanonicalName()).setTitle(R.string.storage_settings).setSourceMetricsCategory(getMetricsCategory()).setArguments(bundle).launch();
                    }
                } else {
                    PrivateVolumeSettings.setVolumeSize(bundle, PrivateStorageInfo.getTotalSize(volumeInfoFindVolumeById, sTotalInternalStorage));
                    new SubSettingLauncher(getContext()).setDestination(PrivateVolumeSettings.class.getCanonicalName()).setTitle(-1).setSourceMetricsCategory(getMetricsCategory()).setArguments(bundle).launch();
                }
                return true;
            }
            if (volumeInfoFindVolumeById.getType() != 0) {
                return false;
            }
            return handlePublicVolumeClick(getContext(), volumeInfoFindVolumeById);
        }
        if (key.startsWith("disk:")) {
            DiskInitFragment.show(this, R.string.storage_dialog_unsupported, key);
            return true;
        }
        Bundle bundle2 = new Bundle();
        bundle2.putString("android.os.storage.extra.FS_UUID", key);
        new SubSettingLauncher(getContext()).setDestination(PrivateVolumeForget.class.getCanonicalName()).setTitle(R.string.storage_menu_forget).setSourceMetricsCategory(getMetricsCategory()).setArguments(bundle2).launch();
        return true;
    }

    static boolean handlePublicVolumeClick(Context context, VolumeInfo volumeInfo) {
        Intent intentBuildBrowseIntent = volumeInfo.buildBrowseIntent();
        if (volumeInfo.isMountedReadable() && intentBuildBrowseIntent != null) {
            context.startActivity(intentBuildBrowseIntent);
            return true;
        }
        Bundle bundle = new Bundle();
        bundle.putString("android.os.storage.extra.VOLUME_ID", volumeInfo.getId());
        new SubSettingLauncher(context).setDestination(PublicVolumeSettings.class.getCanonicalName()).setTitle(-1).setSourceMetricsCategory(42).setArguments(bundle).launch();
        return true;
    }

    public static class MountTask extends AsyncTask<Void, Void, Exception> {
        private final Context mContext;
        private final String mDescription;
        private final StorageManager mStorageManager;
        private final String mVolumeId;

        public MountTask(Context context, VolumeInfo volumeInfo) {
            this.mContext = context.getApplicationContext();
            this.mStorageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
            this.mVolumeId = volumeInfo.getId();
            this.mDescription = this.mStorageManager.getBestVolumeDescription(volumeInfo);
        }

        /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.os.AsyncTask
        protected Exception doInBackground(Void... voidArr) {
            try {
                this.mStorageManager.mount(this.mVolumeId);
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        /* JADX DEBUG: Method merged with bridge method: onPostExecute(Ljava/lang/Object;)V */
        @Override // android.os.AsyncTask
        protected void onPostExecute(Exception exc) {
            if (exc == null) {
                Toast.makeText(this.mContext, this.mContext.getString(R.string.storage_mount_success, this.mDescription), 0).show();
                return;
            }
            Log.e("StorageSettings", "Failed to mount " + this.mVolumeId, exc);
            Toast.makeText(this.mContext, this.mContext.getString(R.string.storage_mount_failure, this.mDescription), 0).show();
        }
    }

    public static class UnmountTask extends AsyncTask<Void, Void, Exception> {
        private final Context mContext;
        private final String mDescription;
        private final StorageManager mStorageManager;
        private final String mVolumeId;

        public UnmountTask(Context context, VolumeInfo volumeInfo) {
            this.mContext = context.getApplicationContext();
            this.mStorageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
            this.mVolumeId = volumeInfo.getId();
            this.mDescription = this.mStorageManager.getBestVolumeDescription(volumeInfo);
        }

        /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.os.AsyncTask
        protected Exception doInBackground(Void... voidArr) {
            try {
                this.mStorageManager.unmount(this.mVolumeId);
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        /* JADX DEBUG: Method merged with bridge method: onPostExecute(Ljava/lang/Object;)V */
        @Override // android.os.AsyncTask
        protected void onPostExecute(Exception exc) {
            if (exc == null) {
                Toast.makeText(this.mContext, this.mContext.getString(R.string.storage_unmount_success, this.mDescription), 0).show();
                return;
            }
            Log.e("StorageSettings", "Failed to unmount " + this.mVolumeId, exc);
            Toast.makeText(this.mContext, this.mContext.getString(R.string.storage_unmount_failure, this.mDescription), 0).show();
        }
    }

    public static class VolumeUnmountedFragment extends InstrumentedDialogFragment {
        public static void show(Fragment fragment, String str) {
            Bundle bundle = new Bundle();
            bundle.putString("android.os.storage.extra.VOLUME_ID", str);
            VolumeUnmountedFragment volumeUnmountedFragment = new VolumeUnmountedFragment();
            volumeUnmountedFragment.setArguments(bundle);
            volumeUnmountedFragment.setTargetFragment(fragment, 0);
            volumeUnmountedFragment.show(fragment.getFragmentManager(), "volume_unmounted");
        }

        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 562;
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            final Activity activity = getActivity();
            final VolumeInfo volumeInfoFindVolumeById = ((StorageManager) activity.getSystemService(StorageManager.class)).findVolumeById(getArguments().getString("android.os.storage.extra.VOLUME_ID"));
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(TextUtils.expandTemplate(getText(R.string.storage_dialog_unmounted), volumeInfoFindVolumeById.getDisk().getDescription()));
            builder.setPositiveButton(R.string.storage_menu_mount, new DialogInterface.OnClickListener() { // from class: com.android.settings.deviceinfo.StorageSettings.VolumeUnmountedFragment.1
                private boolean wasAdminSupportIntentShown(String str) {
                    RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfRestrictionEnforced = RestrictedLockUtils.checkIfRestrictionEnforced(VolumeUnmountedFragment.this.getActivity(), str, UserHandle.myUserId());
                    boolean zHasBaseUserRestriction = RestrictedLockUtils.hasBaseUserRestriction(VolumeUnmountedFragment.this.getActivity(), str, UserHandle.myUserId());
                    if (enforcedAdminCheckIfRestrictionEnforced != null && !zHasBaseUserRestriction) {
                        RestrictedLockUtils.sendShowAdminSupportDetailsIntent(VolumeUnmountedFragment.this.getActivity(), enforcedAdminCheckIfRestrictionEnforced);
                        return true;
                    }
                    return false;
                }

                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (wasAdminSupportIntentShown("no_physical_media")) {
                        return;
                    }
                    if (volumeInfoFindVolumeById.disk != null && volumeInfoFindVolumeById.disk.isUsb() && wasAdminSupportIntentShown("no_usb_file_transfer")) {
                        return;
                    }
                    new MountTask(activity, volumeInfoFindVolumeById).execute(new Void[0]);
                }
            });
            builder.setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null);
            return builder.create();
        }
    }

    public static class DiskInitFragment extends InstrumentedDialogFragment {
        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 561;
        }

        public static void show(Fragment fragment, int i, String str) {
            Bundle bundle = new Bundle();
            bundle.putInt("android.intent.extra.TEXT", i);
            bundle.putString("android.os.storage.extra.DISK_ID", str);
            DiskInitFragment diskInitFragment = new DiskInitFragment();
            diskInitFragment.setArguments(bundle);
            diskInitFragment.setTargetFragment(fragment, 0);
            diskInitFragment.show(fragment.getFragmentManager(), "disk_init");
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            final Activity activity = getActivity();
            StorageManager storageManager = (StorageManager) activity.getSystemService(StorageManager.class);
            int i = getArguments().getInt("android.intent.extra.TEXT");
            final String string = getArguments().getString("android.os.storage.extra.DISK_ID");
            DiskInfo diskInfoFindDiskById = storageManager.findDiskById(string);
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(TextUtils.expandTemplate(getText(i), diskInfoFindDiskById.getDescription()));
            builder.setPositiveButton(R.string.storage_menu_set_up, new DialogInterface.OnClickListener() { // from class: com.android.settings.deviceinfo.StorageSettings.DiskInitFragment.1
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i2) {
                    Intent intent = new Intent(activity, (Class<?>) StorageWizardInit.class);
                    intent.putExtra("android.os.storage.extra.DISK_ID", string);
                    DiskInitFragment.this.startActivity(intent);
                }
            });
            builder.setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null);
            return builder.create();
        }
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mLoader;
        private final StorageManagerVolumeProvider mStorageManagerVolumeProvider;

        private SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mLoader = summaryLoader;
            this.mStorageManagerVolumeProvider = new StorageManagerVolumeProvider((StorageManager) this.mContext.getSystemService(StorageManager.class));
        }

        @Override // com.android.settings.dashboard.SummaryLoader.SummaryProvider
        public void setListening(boolean z) {
            if (z) {
                updateSummary();
            }
        }

        private void updateSummary() {
            this.mLoader.setSummary(this, this.mContext.getString(R.string.storage_summary, NumberFormat.getPercentInstance().format((r1.totalBytes - r1.freeBytes) / r1.totalBytes), Formatter.formatFileSize(this.mContext, PrivateStorageInfo.getPrivateStorageInfo(this.mStorageManagerVolumeProvider).freeBytes)));
        }
    }

    /* JADX DEBUG: Can't inline method, not implemented redirect type for insn: 0x0003: CONSTRUCTOR 
  (r2v0 android.app.Activity)
  (r3v0 com.android.settings.dashboard.SummaryLoader)
  (null com.android.settings.deviceinfo.StorageSettings$1)
 A[MD:(android.content.Context, com.android.settings.dashboard.SummaryLoader, com.android.settings.deviceinfo.StorageSettings$1):void (m)] (LINE:583) call: com.android.settings.deviceinfo.StorageSettings.SummaryProvider.<init>(android.content.Context, com.android.settings.dashboard.SummaryLoader, com.android.settings.deviceinfo.StorageSettings$1):void type: CONSTRUCTOR */
    static /* synthetic */ SummaryLoader.SummaryProvider lambda$static$0(Activity activity, SummaryLoader summaryLoader) {
        return new SummaryProvider(activity, summaryLoader);
    }
}
