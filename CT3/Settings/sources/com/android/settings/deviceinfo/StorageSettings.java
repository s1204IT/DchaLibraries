package com.android.settings.deviceinfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
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
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.RestrictedLockUtils;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import com.mediatek.settings.ext.IStorageSettingsExt;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StorageSettings extends SettingsPreferenceFragment implements Indexable {
    private static IStorageSettingsExt mExt;
    private static boolean sHasOpened;
    private PreferenceCategory mExternalCategory;
    private PreferenceCategory mInternalCategory;
    private StorageSummaryPreference mInternalSummary;
    private final StorageEventListener mStorageListener = new StorageEventListener() {
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            if (!StorageSettings.isInteresting(vol)) {
                return;
            }
            StorageSettings.this.refresh();
        }

        public void onDiskDestroyed(DiskInfo disk) {
            StorageSettings.this.refresh();
        }

        public void onDiskScanned(DiskInfo disk, int volumeCount) {
            StorageSettings.this.refresh();
        }
    };
    private StorageManager mStorageManager;
    static final int COLOR_PUBLIC = Color.parseColor("#ff9e9e9e");
    static final int COLOR_WARNING = Color.parseColor("#fff4511e");
    static final int[] COLOR_PRIVATE = {Color.parseColor("#ff26a69a"), Color.parseColor("#ffab47bc"), Color.parseColor("#fff2a600"), Color.parseColor("#ffec407a"), Color.parseColor("#ffc0ca33")};
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader, null);
        }
    };
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList<>();
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = context.getString(R.string.storage_settings);
            data.screenTitle = context.getString(R.string.storage_settings);
            result.add(data);
            SearchIndexableRaw data2 = new SearchIndexableRaw(context);
            data2.title = context.getString(R.string.internal_storage);
            data2.screenTitle = context.getString(R.string.storage_settings);
            result.add(data2);
            SearchIndexableRaw data3 = new SearchIndexableRaw(context);
            StorageManager storage = (StorageManager) context.getSystemService(StorageManager.class);
            List<VolumeInfo> vols = storage.getVolumes();
            for (VolumeInfo vol : vols) {
                if (StorageSettings.isInteresting(vol)) {
                    data3.title = storage.getBestVolumeDescription(vol);
                    data3.screenTitle = context.getString(R.string.storage_settings);
                    result.add(data3);
                }
            }
            SearchIndexableRaw data4 = new SearchIndexableRaw(context);
            data4.title = context.getString(R.string.memory_size);
            data4.screenTitle = context.getString(R.string.storage_settings);
            result.add(data4);
            SearchIndexableRaw data5 = new SearchIndexableRaw(context);
            data5.title = context.getString(R.string.memory_available);
            data5.screenTitle = context.getString(R.string.storage_settings);
            result.add(data5);
            SearchIndexableRaw data6 = new SearchIndexableRaw(context);
            data6.title = context.getString(R.string.memory_apps_usage);
            data6.screenTitle = context.getString(R.string.storage_settings);
            result.add(data6);
            SearchIndexableRaw data7 = new SearchIndexableRaw(context);
            data7.title = context.getString(R.string.memory_dcim_usage);
            data7.screenTitle = context.getString(R.string.storage_settings);
            result.add(data7);
            SearchIndexableRaw data8 = new SearchIndexableRaw(context);
            data8.title = context.getString(R.string.memory_music_usage);
            data8.screenTitle = context.getString(R.string.storage_settings);
            result.add(data8);
            SearchIndexableRaw data9 = new SearchIndexableRaw(context);
            data9.title = context.getString(R.string.memory_downloads_usage);
            data9.screenTitle = context.getString(R.string.storage_settings);
            result.add(data9);
            SearchIndexableRaw data10 = new SearchIndexableRaw(context);
            data10.title = context.getString(R.string.memory_media_cache_usage);
            data10.screenTitle = context.getString(R.string.storage_settings);
            result.add(data10);
            SearchIndexableRaw data11 = new SearchIndexableRaw(context);
            data11.title = context.getString(R.string.memory_media_misc_usage);
            data11.screenTitle = context.getString(R.string.storage_settings);
            result.add(data11);
            return result;
        }
    };

    @Override
    protected int getMetricsCategory() {
        return 42;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_storage;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Context context = getActivity();
        this.mStorageManager = (StorageManager) context.getSystemService(StorageManager.class);
        addPreferencesFromResource(R.xml.device_info_storage);
        this.mInternalCategory = (PreferenceCategory) findPreference("storage_internal");
        this.mExternalCategory = (PreferenceCategory) findPreference("storage_external");
        this.mInternalSummary = new StorageSummaryPreference(getPrefContext());
        setHasOptionsMenu(true);
    }

    public static boolean isInteresting(VolumeInfo vol) {
        switch (vol.getType()) {
            case DefaultWfcSettingsExt.RESUME:
            case DefaultWfcSettingsExt.PAUSE:
                return true;
            default:
                return false;
        }
    }

    public void refresh() {
        Context context = getPrefContext();
        getPreferenceScreen().removeAll();
        this.mInternalCategory.removeAll();
        this.mExternalCategory.removeAll();
        this.mInternalCategory.addPreference(this.mInternalSummary);
        int privateCount = 0;
        long privateUsedBytes = 0;
        long privateTotalBytes = 0;
        List<VolumeInfo> volumes = this.mStorageManager.getVolumes();
        Collections.sort(volumes, VolumeInfo.getDescriptionComparator());
        for (VolumeInfo vol : volumes) {
            if (vol.getType() == 1) {
                int privateCount2 = privateCount + 1;
                int color = COLOR_PRIVATE[privateCount % COLOR_PRIVATE.length];
                this.mInternalCategory.addPreference(new StorageVolumePreference(context, vol, color));
                if (vol.isMountedReadable()) {
                    File path = vol.getPath();
                    privateUsedBytes += path.getTotalSpace() - path.getFreeSpace();
                    privateTotalBytes += path.getTotalSpace();
                    privateCount = privateCount2;
                } else {
                    privateCount = privateCount2;
                }
            } else if (vol.getType() == 0) {
                this.mExternalCategory.addPreference(new StorageVolumePreference(context, vol, COLOR_PUBLIC));
            }
        }
        List<VolumeRecord> recs = this.mStorageManager.getVolumeRecords();
        for (VolumeRecord rec : recs) {
            if (rec.getType() == 1 && this.mStorageManager.findVolumeByUuid(rec.getFsUuid()) == null) {
                Drawable icon = context.getDrawable(R.drawable.ic_sim_sd);
                icon.mutate();
                icon.setTint(COLOR_PUBLIC);
                Preference pref = new Preference(context);
                pref.setKey(rec.getFsUuid());
                pref.setTitle(rec.getNickname());
                pref.setSummary(android.R.string.fingerprint_error_unable_to_process);
                pref.setIcon(icon);
                this.mInternalCategory.addPreference(pref);
            }
        }
        List<DiskInfo> disks = this.mStorageManager.getDisks();
        for (DiskInfo disk : disks) {
            if (disk.volumeCount == 0 && disk.size > 0) {
                Preference pref2 = new Preference(context);
                pref2.setKey(disk.getId());
                pref2.setTitle(disk.getDescription());
                pref2.setSummary(android.R.string.fingerprint_error_power_pressed);
                pref2.setIcon(R.drawable.ic_sim_sd);
                this.mExternalCategory.addPreference(pref2);
            }
        }
        Formatter.BytesResult result = Formatter.formatBytes(getResources(), privateUsedBytes, 0);
        this.mInternalSummary.setTitle(TextUtils.expandTemplate(getText(R.string.storage_size_large), result.value, result.units));
        this.mInternalSummary.setSummary(getString(R.string.storage_volume_used_total, new Object[]{Formatter.formatFileSize(context, privateTotalBytes)}));
        mExt.updateCustomizedStorageSettingsPlugin(this.mInternalCategory);
        if (this.mInternalCategory.getPreferenceCount() > 0) {
            getPreferenceScreen().addPreference(this.mInternalCategory);
        }
        if (this.mExternalCategory.getPreferenceCount() > 0) {
            getPreferenceScreen().addPreference(this.mExternalCategory);
        }
        if (this.mInternalCategory.getPreferenceCount() != 2 || this.mExternalCategory.getPreferenceCount() != 0 || sHasOpened) {
            return;
        }
        Bundle args = new Bundle();
        args.putString("android.os.storage.extra.VOLUME_ID", "private");
        Intent intent = Utils.onBuildStartFragmentIntent(getActivity(), PrivateVolumeSettings.class.getName(), args, null, R.string.apps_storage, null, false);
        intent.putExtra("show_drawer_menu", true);
        getActivity().startActivity(intent);
        sHasOpened = true;
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mStorageManager.registerListener(this.mStorageListener);
        sHasOpened = false;
        refresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mStorageManager.unregisterListener(this.mStorageListener);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference pref) {
        String key = pref.getKey();
        if (pref instanceof StorageVolumePreference) {
            VolumeInfo vol = this.mStorageManager.findVolumeById(key);
            if (vol == null) {
                return false;
            }
            if (vol.getState() == 0) {
                VolumeUnmountedFragment.show(this, vol.getId());
                return true;
            }
            if (vol.getState() == 6) {
                DiskInitFragment.show(this, R.string.storage_dialog_unmountable, vol.getDiskId());
                return true;
            }
            if (vol.getType() == 1) {
                Bundle args = new Bundle();
                args.putString("android.os.storage.extra.VOLUME_ID", vol.getId());
                startFragment(this, PrivateVolumeSettings.class.getCanonicalName(), -1, 0, args);
                return true;
            }
            if (vol.getType() != 0) {
                return false;
            }
            if (vol.isMountedReadable()) {
                startActivity(vol.buildBrowseIntent());
                return true;
            }
            Bundle args2 = new Bundle();
            args2.putString("android.os.storage.extra.VOLUME_ID", vol.getId());
            startFragment(this, PublicVolumeSettings.class.getCanonicalName(), -1, 0, args2);
            return true;
        }
        if (key.startsWith("disk:")) {
            DiskInitFragment.show(this, R.string.storage_dialog_unsupported, key);
            return true;
        }
        if (key.startsWith("/storage/")) {
            return false;
        }
        Bundle args3 = new Bundle();
        args3.putString("android.os.storage.extra.FS_UUID", key);
        startFragment(this, PrivateVolumeForget.class.getCanonicalName(), R.string.storage_menu_forget, 0, args3);
        return true;
    }

    public static class MountTask extends AsyncTask<Void, Void, Exception> {
        private final Context mContext;
        private final String mDescription;
        private final StorageManager mStorageManager;
        private final String mVolumeId;

        public MountTask(Context context, VolumeInfo volume) {
            this.mContext = context.getApplicationContext();
            this.mStorageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
            this.mVolumeId = volume.getId();
            this.mDescription = this.mStorageManager.getBestVolumeDescription(volume);
        }

        @Override
        public Exception doInBackground(Void... params) {
            try {
                this.mStorageManager.mount(this.mVolumeId);
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        public void onPostExecute(Exception e) {
            if (e == null) {
                Toast.makeText(this.mContext, this.mContext.getString(R.string.storage_mount_success, this.mDescription), 0).show();
            } else {
                Log.e("StorageSettings", "Failed to mount " + this.mVolumeId, e);
                Toast.makeText(this.mContext, this.mContext.getString(R.string.storage_mount_failure, this.mDescription), 0).show();
            }
        }
    }

    public static class UnmountTask extends AsyncTask<Void, Void, Exception> {
        private final Context mContext;
        private final String mDescription;
        private final StorageManager mStorageManager;
        private final String mVolumeId;

        public UnmountTask(Context context, VolumeInfo volume) {
            this.mContext = context.getApplicationContext();
            this.mStorageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
            this.mVolumeId = volume.getId();
            this.mDescription = this.mStorageManager.getBestVolumeDescription(volume);
        }

        @Override
        public Exception doInBackground(Void... params) {
            try {
                this.mStorageManager.unmount(this.mVolumeId);
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        public void onPostExecute(Exception e) {
            if (e == null) {
                Toast.makeText(this.mContext, this.mContext.getString(R.string.storage_unmount_success, this.mDescription), 0).show();
            } else {
                Log.e("StorageSettings", "Failed to unmount " + this.mVolumeId, e);
                Toast.makeText(this.mContext, this.mContext.getString(R.string.storage_unmount_failure, this.mDescription), 0).show();
            }
        }
    }

    public static class VolumeUnmountedFragment extends DialogFragment {
        public static void show(Fragment parent, String volumeId) {
            Bundle args = new Bundle();
            args.putString("android.os.storage.extra.VOLUME_ID", volumeId);
            VolumeUnmountedFragment dialog = new VolumeUnmountedFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), "volume_unmounted");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            StorageManager sm = (StorageManager) context.getSystemService(StorageManager.class);
            String volumeId = getArguments().getString("android.os.storage.extra.VOLUME_ID");
            final VolumeInfo vol = sm.findVolumeById(volumeId);
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(TextUtils.expandTemplate(getText(R.string.storage_dialog_unmounted), vol.getDisk().getDescription()));
            builder.setPositiveButton(R.string.storage_menu_mount, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(VolumeUnmountedFragment.this.getActivity(), "no_physical_media", UserHandle.myUserId());
                    boolean hasBaseUserRestriction = RestrictedLockUtils.hasBaseUserRestriction(VolumeUnmountedFragment.this.getActivity(), "no_physical_media", UserHandle.myUserId());
                    if (admin != null && !hasBaseUserRestriction) {
                        RestrictedLockUtils.sendShowAdminSupportDetailsIntent(VolumeUnmountedFragment.this.getActivity(), admin);
                    } else {
                        new MountTask(context, vol).execute(new Void[0]);
                    }
                }
            });
            builder.setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null);
            return builder.create();
        }
    }

    public static class DiskInitFragment extends DialogFragment {
        public static void show(Fragment parent, int resId, String diskId) {
            Bundle args = new Bundle();
            args.putInt("android.intent.extra.TEXT", resId);
            args.putString("android.os.storage.extra.DISK_ID", diskId);
            DiskInitFragment dialog = new DiskInitFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), "disk_init");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            StorageManager sm = (StorageManager) context.getSystemService(StorageManager.class);
            int resId = getArguments().getInt("android.intent.extra.TEXT");
            final String diskId = getArguments().getString("android.os.storage.extra.DISK_ID");
            DiskInfo disk = sm.findDiskById(diskId);
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(TextUtils.expandTemplate(getText(resId), disk.getDescription()));
            builder.setPositiveButton(R.string.storage_menu_set_up, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(context, (Class<?>) StorageWizardInit.class);
                    intent.putExtra("android.os.storage.extra.DISK_ID", diskId);
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

        SummaryProvider(Context context, SummaryLoader loader, SummaryProvider summaryProvider) {
            this(context, loader);
        }

        private SummaryProvider(Context context, SummaryLoader loader) {
            this.mContext = context;
            this.mLoader = loader;
            IStorageSettingsExt unused = StorageSettings.mExt = UtilsExt.getStorageSettingsPlugin(context);
        }

        @Override
        public void setListening(boolean listening) {
            if (!listening) {
                return;
            }
            updateSummary();
            StorageSettings.mExt.updateCustomizedStorageSummary(this, this.mLoader);
        }

        private void updateSummary() {
            StorageManager storageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
            List<VolumeInfo> volumes = storageManager.getVolumes();
            long privateUsedBytes = 0;
            long privateTotalBytes = 0;
            for (VolumeInfo info : volumes) {
                if (info.getType() == 0 || info.getType() == 1) {
                    File path = info.getPath();
                    if (path != null) {
                        privateUsedBytes += path.getTotalSpace() - path.getFreeSpace();
                        privateTotalBytes += path.getTotalSpace();
                    }
                }
            }
            this.mLoader.setSummary(this, this.mContext.getString(R.string.storage_summary, Formatter.formatFileSize(this.mContext, privateUsedBytes), Formatter.formatFileSize(this.mContext, privateTotalBytes)));
        }
    }
}
