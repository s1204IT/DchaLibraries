package com.android.settings.deviceinfo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.IMountService;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.google.android.collect.Lists;
import java.util.ArrayList;
import java.util.List;

public class Memory extends SettingsPreferenceFragment implements Indexable {
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
            StorageVolume[] storageVolumes = StorageManager.from(context).getVolumeList();
            for (StorageVolume volume : storageVolumes) {
                if (!volume.isEmulated()) {
                    data3.title = volume.getDescription(context);
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
    private static String sClickedMountPoint;
    private static Preference sLastClickedMountToggle;
    private IMountService mMountService;
    private StorageManager mStorageManager;
    private UsbManager mUsbManager;
    private ArrayList<StorageVolumePreferenceCategory> mCategories = Lists.newArrayList();
    StorageEventListener mStorageListener = new StorageEventListener() {
        public void onStorageStateChanged(String path, String oldState, String newState) {
            Log.i("MemorySettings", "Received storage state changed notification that " + path + " changed state from " + oldState + " to " + newState);
            for (StorageVolumePreferenceCategory category : Memory.this.mCategories) {
                StorageVolume volume = category.getStorageVolume();
                if (volume != null && path.equals(volume.getPath())) {
                    category.onStorageStateChanged();
                    return;
                }
            }
        }
    };
    private final BroadcastReceiver mMediaScannerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.hardware.usb.action.USB_STATE")) {
                boolean isUsbConnected = intent.getBooleanExtra("connected", false);
                String usbFunction = Memory.this.mUsbManager.getDefaultFunction();
                for (StorageVolumePreferenceCategory category : Memory.this.mCategories) {
                    category.onUsbStateChanged(isUsbConnected, usbFunction);
                }
                return;
            }
            if (action.equals("android.intent.action.MEDIA_SCANNER_FINISHED")) {
                for (StorageVolumePreferenceCategory category2 : Memory.this.mCategories) {
                    category2.onMediaScannerFinished();
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Context context = getActivity();
        this.mUsbManager = (UsbManager) getSystemService("usb");
        this.mStorageManager = StorageManager.from(context);
        this.mStorageManager.registerListener(this.mStorageListener);
        addPreferencesFromResource(R.xml.device_info_memory);
        addCategory(StorageVolumePreferenceCategory.buildForInternal(context));
        if (UserHandle.myUserId() == 0) {
            StorageVolume[] storageVolumes = this.mStorageManager.getVolumeList();
            for (StorageVolume volume : storageVolumes) {
                if (!volume.isEmulated()) {
                    addCategory(StorageVolumePreferenceCategory.buildForPhysical(context, volume));
                }
            }
        }
        setHasOptionsMenu(true);
    }

    private void addCategory(StorageVolumePreferenceCategory category) {
        this.mCategories.add(category);
        getPreferenceScreen().addPreference(category);
        category.init();
    }

    private boolean isMassStorageEnabled() {
        StorageVolume[] volumes = this.mStorageManager.getVolumeList();
        StorageVolume primary = StorageManager.getPrimaryVolume(volumes);
        return primary != null && primary.allowMassStorage();
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter("android.intent.action.MEDIA_SCANNER_STARTED");
        intentFilter.addAction("android.intent.action.MEDIA_SCANNER_FINISHED");
        intentFilter.addDataScheme("file");
        getActivity().registerReceiver(this.mMediaScannerReceiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.hardware.usb.action.USB_STATE");
        getActivity().registerReceiver(this.mMediaScannerReceiver, intentFilter2);
        for (StorageVolumePreferenceCategory category : this.mCategories) {
            category.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(this.mMediaScannerReceiver);
        for (StorageVolumePreferenceCategory category : this.mCategories) {
            category.onPause();
        }
    }

    @Override
    public void onDestroy() {
        if (this.mStorageManager != null && this.mStorageListener != null) {
            this.mStorageManager.unregisterListener(this.mStorageListener);
        }
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.storage, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem usb = menu.findItem(R.id.storage_usb);
        UserManager um = (UserManager) getActivity().getSystemService("user");
        boolean usbItemVisible = (isMassStorageEnabled() || um.hasUserRestriction("no_usb_file_transfer")) ? false : true;
        usb.setVisible(usbItemVisible);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.storage_usb:
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).startPreferencePanel(UsbSettings.class.getCanonicalName(), null, R.string.storage_title_usb, null, this, 0);
                } else {
                    startFragment(this, UsbSettings.class.getCanonicalName(), R.string.storage_title_usb, -1, null);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private synchronized IMountService getMountService() {
        if (this.mMountService == null) {
            IBinder service = ServiceManager.getService("mount");
            if (service != null) {
                this.mMountService = IMountService.Stub.asInterface(service);
            } else {
                Log.e("MemorySettings", "Can't get mount service");
            }
        }
        return this.mMountService;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if ("cache".equals(preference.getKey())) {
            ConfirmClearCacheFragment.show(this);
            return true;
        }
        for (StorageVolumePreferenceCategory category : this.mCategories) {
            Intent intent = category.intentForClick(preference);
            if (intent != null) {
                if (Utils.isMonkeyRunning()) {
                    return true;
                }
                try {
                    startActivity(intent);
                    return true;
                } catch (ActivityNotFoundException e) {
                    Log.w("MemorySettings", "No activity found for intent " + intent);
                    return true;
                }
            }
            StorageVolume volume = category.getStorageVolume();
            if (volume != null && category.mountToggleClicked(preference)) {
                sLastClickedMountToggle = preference;
                sClickedMountPoint = volume.getPath();
                String state = this.mStorageManager.getVolumeState(volume.getPath());
                if ("mounted".equals(state) || "mounted_ro".equals(state)) {
                    unmount();
                    return true;
                }
                mount();
                return true;
            }
        }
        return false;
    }

    @Override
    public Dialog onCreateDialog(int id) {
        switch (id) {
            case 1:
                return new AlertDialog.Builder(getActivity()).setTitle(R.string.dlg_confirm_unmount_title).setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Memory.this.doUnmount();
                    }
                }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).setMessage(R.string.dlg_confirm_unmount_text).create();
            case 2:
                return new AlertDialog.Builder(getActivity()).setTitle(R.string.dlg_error_unmount_title).setNeutralButton(R.string.dlg_ok, (DialogInterface.OnClickListener) null).setMessage(R.string.dlg_error_unmount_text).create();
            default:
                return null;
        }
    }

    public void doUnmount() {
        Toast.makeText(getActivity(), R.string.unmount_inform_text, 0).show();
        IMountService mountService = getMountService();
        try {
            sLastClickedMountToggle.setEnabled(false);
            sLastClickedMountToggle.setTitle(getString(R.string.sd_ejecting_title));
            sLastClickedMountToggle.setSummary(getString(R.string.sd_ejecting_summary));
            mountService.unmountVolume(sClickedMountPoint, true, false);
        } catch (RemoteException e) {
            showDialogInner(2);
        }
    }

    private void showDialogInner(int id) {
        removeDialog(id);
        showDialog(id);
    }

    private boolean hasAppsAccessingStorage() throws RemoteException {
        IMountService mountService = getMountService();
        int[] stUsers = mountService.getStorageUsers(sClickedMountPoint);
        if (stUsers == null || stUsers.length > 0) {
        }
        return true;
    }

    private void unmount() {
        try {
            if (hasAppsAccessingStorage()) {
                showDialogInner(1);
            } else {
                doUnmount();
            }
        } catch (RemoteException e) {
            Log.e("MemorySettings", "Is MountService running?");
            showDialogInner(2);
        }
    }

    private void mount() {
        IMountService mountService = getMountService();
        try {
            if (mountService != null) {
                mountService.mountVolume(sClickedMountPoint);
            } else {
                Log.e("MemorySettings", "Mount service is null, can't mount");
            }
        } catch (RemoteException e) {
        }
    }

    public void onCacheCleared() {
        for (StorageVolumePreferenceCategory category : this.mCategories) {
            category.onCacheCleared();
        }
    }

    private static class ClearCacheObserver extends IPackageDataObserver.Stub {
        private int mRemaining;
        private final Memory mTarget;

        public ClearCacheObserver(Memory target, int remaining) {
            this.mTarget = target;
            this.mRemaining = remaining;
        }

        public void onRemoveCompleted(String packageName, boolean succeeded) {
            synchronized (this) {
                int i = this.mRemaining - 1;
                this.mRemaining = i;
                if (i == 0) {
                    this.mTarget.onCacheCleared();
                }
            }
        }
    }

    public static class ConfirmClearCacheFragment extends DialogFragment {
        public static void show(Memory parent) {
            if (parent.isAdded()) {
                ConfirmClearCacheFragment dialog = new ConfirmClearCacheFragment();
                dialog.setTargetFragment(parent, 0);
                dialog.show(parent.getFragmentManager(), "confirmClearCache");
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.memory_clear_cache_title);
            builder.setMessage(getString(R.string.memory_clear_cache_message));
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Memory target = (Memory) ConfirmClearCacheFragment.this.getTargetFragment();
                    PackageManager pm = context.getPackageManager();
                    List<PackageInfo> infos = pm.getInstalledPackages(0);
                    ClearCacheObserver observer = new ClearCacheObserver(target, infos.size());
                    for (PackageInfo info : infos) {
                        pm.deleteApplicationCacheFiles(info.packageName, observer);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
            return builder.create();
        }
    }
}
