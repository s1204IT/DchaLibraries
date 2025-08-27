package com.android.settings.deviceinfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.provider.DocumentsContract;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.applications.manageapplications.ManageApplications;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.deviceinfo.StorageSettings;
import com.android.settingslib.deviceinfo.StorageMeasurement;
import com.google.android.collect.Lists;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/* loaded from: classes.dex */
public class PrivateVolumeSettings extends SettingsPreferenceFragment {
    private static final int[] ITEMS_NO_SHOW_SHARED = {R.string.storage_detail_apps, R.string.storage_detail_system};
    private static final int[] ITEMS_SHOW_SHARED = {R.string.storage_detail_apps, R.string.storage_detail_images, R.string.storage_detail_videos, R.string.storage_detail_audio, R.string.storage_detail_system, R.string.storage_detail_other};
    private UserInfo mCurrentUser;
    private Preference mExplore;
    private int mHeaderPoolIndex;
    private int mItemPoolIndex;
    private StorageMeasurement mMeasure;
    private boolean mNeedsUpdate;
    private VolumeInfo mSharedVolume;
    private StorageManager mStorageManager;
    private StorageSummaryPreference mSummary;
    private long mSystemSize;
    private long mTotalSize;
    private UserManager mUserManager;
    private VolumeInfo mVolume;
    private String mVolumeId;
    private List<StorageItemPreference> mItemPreferencePool = Lists.newArrayList();
    private List<PreferenceCategory> mHeaderPreferencePool = Lists.newArrayList();
    private final StorageMeasurement.MeasurementReceiver mReceiver = new StorageMeasurement.MeasurementReceiver() { // from class: com.android.settings.deviceinfo.PrivateVolumeSettings.1
        @Override // com.android.settingslib.deviceinfo.StorageMeasurement.MeasurementReceiver
        public void onDetailsChanged(StorageMeasurement.MeasurementDetails measurementDetails) throws NumberFormatException {
            PrivateVolumeSettings.this.updateDetails(measurementDetails);
        }
    };
    private final StorageEventListener mStorageListener = new StorageEventListener() { // from class: com.android.settings.deviceinfo.PrivateVolumeSettings.2
        public void onVolumeStateChanged(VolumeInfo volumeInfo, int i, int i2) {
            if (Objects.equals(PrivateVolumeSettings.this.mVolume.getId(), volumeInfo.getId())) {
                PrivateVolumeSettings.this.mVolume = volumeInfo;
                PrivateVolumeSettings.this.update();
            }
        }

        public void onVolumeRecordChanged(VolumeRecord volumeRecord) {
            if (Objects.equals(PrivateVolumeSettings.this.mVolume.getFsUuid(), volumeRecord.getFsUuid())) {
                PrivateVolumeSettings.this.mVolume = PrivateVolumeSettings.this.mStorageManager.findVolumeById(PrivateVolumeSettings.this.mVolumeId);
                PrivateVolumeSettings.this.update();
            }
        }
    };

    private boolean isVolumeValid() {
        return this.mVolume != null && this.mVolume.getType() == 1 && this.mVolume.isMountedReadable();
    }

    public PrivateVolumeSettings() {
        setRetainInstance(true);
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 42;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Activity activity = getActivity();
        this.mUserManager = (UserManager) activity.getSystemService(UserManager.class);
        this.mStorageManager = (StorageManager) activity.getSystemService(StorageManager.class);
        this.mVolumeId = getArguments().getString("android.os.storage.extra.VOLUME_ID");
        this.mVolume = this.mStorageManager.findVolumeById(this.mVolumeId);
        long totalSpace = this.mVolume.getPath().getTotalSpace();
        this.mTotalSize = getArguments().getLong("volume_size", 0L);
        this.mSystemSize = this.mTotalSize - totalSpace;
        if (this.mTotalSize <= 0) {
            this.mTotalSize = totalSpace;
            this.mSystemSize = 0L;
        }
        this.mSharedVolume = this.mStorageManager.findEmulatedForPrivate(this.mVolume);
        this.mMeasure = new StorageMeasurement(activity, this.mVolume, this.mSharedVolume);
        this.mMeasure.setReceiver(this.mReceiver);
        if (!isVolumeValid()) {
            getActivity().finish();
            return;
        }
        addPreferencesFromResource(R.xml.device_info_storage_volume);
        getPreferenceScreen().setOrderingAsAdded(true);
        this.mSummary = new StorageSummaryPreference(getPrefContext());
        this.mCurrentUser = this.mUserManager.getUserInfo(UserHandle.myUserId());
        this.mExplore = buildAction(R.string.storage_menu_explore);
        this.mNeedsUpdate = true;
        setHasOptionsMenu(true);
    }

    private void setTitle() {
        getActivity().setTitle(this.mStorageManager.getBestVolumeDescription(this.mVolume));
    }

    private void update() {
        PreferenceGroup preferenceGroupAddCategory;
        if (!isVolumeValid()) {
            getActivity().finish();
            return;
        }
        setTitle();
        getFragmentManager().invalidateOptionsMenu();
        Activity activity = getActivity();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();
        addPreference(preferenceScreen, this.mSummary);
        List users = this.mUserManager.getUsers();
        int size = users.size();
        boolean z = size > 1;
        boolean z2 = this.mSharedVolume != null && this.mSharedVolume.isMountedReadable();
        this.mItemPoolIndex = 0;
        this.mHeaderPoolIndex = 0;
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            UserInfo userInfo = (UserInfo) users.get(i2);
            if (Utils.isProfileOf(this.mCurrentUser, userInfo)) {
                if (z) {
                    preferenceGroupAddCategory = addCategory(preferenceScreen, userInfo.name);
                } else {
                    preferenceGroupAddCategory = preferenceScreen;
                }
                addDetailItems(preferenceGroupAddCategory, z2, userInfo.id);
                i++;
            }
        }
        if (size - i > 0) {
            PreferenceCategory preferenceCategoryAddCategory = addCategory(preferenceScreen, getText(R.string.storage_other_users));
            for (int i3 = 0; i3 < size; i3++) {
                UserInfo userInfo2 = (UserInfo) users.get(i3);
                if (!Utils.isProfileOf(this.mCurrentUser, userInfo2)) {
                    addItem(preferenceCategoryAddCategory, 0, userInfo2.name, userInfo2.id);
                }
            }
        }
        addItem(preferenceScreen, R.string.storage_detail_cached, null, -10000);
        if (z2) {
            addPreference(preferenceScreen, this.mExplore);
        }
        long freeSpace = this.mTotalSize - this.mVolume.getPath().getFreeSpace();
        Formatter.BytesResult bytes = Formatter.formatBytes(getResources(), freeSpace, 0);
        this.mSummary.setTitle(TextUtils.expandTemplate(getText(R.string.storage_size_large), bytes.value, bytes.units));
        this.mSummary.setSummary(getString(R.string.storage_volume_used, new Object[]{Formatter.formatFileSize(activity, this.mTotalSize)}));
        this.mSummary.setPercent(freeSpace, this.mTotalSize);
        this.mMeasure.forceMeasure();
        this.mNeedsUpdate = false;
    }

    private void addPreference(PreferenceGroup preferenceGroup, Preference preference) {
        preference.setOrder(Preference.DEFAULT_ORDER);
        preferenceGroup.addPreference(preference);
    }

    private PreferenceCategory addCategory(PreferenceGroup preferenceGroup, CharSequence charSequence) {
        PreferenceCategory preferenceCategory;
        if (this.mHeaderPoolIndex < this.mHeaderPreferencePool.size()) {
            preferenceCategory = this.mHeaderPreferencePool.get(this.mHeaderPoolIndex);
        } else {
            preferenceCategory = new PreferenceCategory(getPrefContext());
            this.mHeaderPreferencePool.add(preferenceCategory);
        }
        preferenceCategory.setTitle(charSequence);
        preferenceCategory.removeAll();
        addPreference(preferenceGroup, preferenceCategory);
        this.mHeaderPoolIndex++;
        return preferenceCategory;
    }

    private void addDetailItems(PreferenceGroup preferenceGroup, boolean z, int i) {
        for (int i2 : z ? ITEMS_SHOW_SHARED : ITEMS_NO_SHOW_SHARED) {
            addItem(preferenceGroup, i2, null, i);
        }
    }

    private void addItem(PreferenceGroup preferenceGroup, int i, CharSequence charSequence, int i2) {
        StorageItemPreference storageItemPreferenceBuildItem;
        if (i == R.string.storage_detail_system) {
            if (this.mSystemSize <= 0) {
                Log.w("PrivateVolumeSettings", "Skipping System storage because its size is " + this.mSystemSize);
                return;
            }
            if (i2 != UserHandle.myUserId()) {
                return;
            }
        }
        if (this.mItemPoolIndex < this.mItemPreferencePool.size()) {
            storageItemPreferenceBuildItem = this.mItemPreferencePool.get(this.mItemPoolIndex);
        } else {
            storageItemPreferenceBuildItem = buildItem();
            this.mItemPreferencePool.add(storageItemPreferenceBuildItem);
        }
        if (charSequence != null) {
            storageItemPreferenceBuildItem.setTitle(charSequence);
            storageItemPreferenceBuildItem.setKey(charSequence.toString());
        } else {
            storageItemPreferenceBuildItem.setTitle(i);
            storageItemPreferenceBuildItem.setKey(Integer.toString(i));
        }
        storageItemPreferenceBuildItem.setSummary(R.string.memory_calculating_size);
        storageItemPreferenceBuildItem.userHandle = i2;
        addPreference(preferenceGroup, storageItemPreferenceBuildItem);
        this.mItemPoolIndex++;
    }

    private StorageItemPreference buildItem() {
        StorageItemPreference storageItemPreference = new StorageItemPreference(getPrefContext());
        storageItemPreference.setIcon(R.drawable.empty_icon);
        return storageItemPreference;
    }

    private Preference buildAction(int i) {
        Preference preference = new Preference(getPrefContext());
        preference.setTitle(i);
        preference.setKey(Integer.toString(i));
        return preference;
    }

    static void setVolumeSize(Bundle bundle, long j) {
        bundle.putLong("volume_size", j);
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        this.mVolume = this.mStorageManager.findVolumeById(this.mVolumeId);
        if (!isVolumeValid()) {
            getActivity().finish();
            return;
        }
        this.mStorageManager.registerListener(this.mStorageListener);
        if (this.mNeedsUpdate) {
            update();
        } else {
            setTitle();
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        super.onPause();
        this.mStorageManager.unregisterListener(this.mStorageListener);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onDestroy() {
        super.onDestroy();
        if (this.mMeasure != null) {
            this.mMeasure.onDestroy();
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.storage_volume, menu);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPrepareOptionsMenu(Menu menu) {
        if (isVolumeValid()) {
            MenuItem menuItemFindItem = menu.findItem(R.id.storage_rename);
            MenuItem menuItemFindItem2 = menu.findItem(R.id.storage_mount);
            MenuItem menuItemFindItem3 = menu.findItem(R.id.storage_unmount);
            MenuItem menuItemFindItem4 = menu.findItem(R.id.storage_format);
            MenuItem menuItemFindItem5 = menu.findItem(R.id.storage_migrate);
            MenuItem menuItemFindItem6 = menu.findItem(R.id.storage_free);
            if ("private".equals(this.mVolume.getId())) {
                menuItemFindItem.setVisible(false);
                menuItemFindItem2.setVisible(false);
                menuItemFindItem3.setVisible(false);
                menuItemFindItem4.setVisible(false);
                menuItemFindItem6.setVisible(getResources().getBoolean(R.bool.config_storage_manager_settings_enabled));
            } else {
                menuItemFindItem.setVisible(this.mVolume.getType() == 1);
                menuItemFindItem2.setVisible(this.mVolume.getState() == 0);
                menuItemFindItem3.setVisible(this.mVolume.isMountedReadable());
                menuItemFindItem4.setVisible(true);
                menuItemFindItem6.setVisible(false);
            }
            menuItemFindItem4.setTitle(R.string.storage_menu_format_public);
            VolumeInfo primaryStorageCurrentVolume = getActivity().getPackageManager().getPrimaryStorageCurrentVolume();
            menuItemFindItem5.setVisible((primaryStorageCurrentVolume == null || primaryStorageCurrentVolume.getType() != 1 || Objects.equals(this.mVolume, primaryStorageCurrentVolume)) ? false : true);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        Activity activity = getActivity();
        Bundle bundle = new Bundle();
        switch (menuItem.getItemId()) {
            case R.id.storage_format /* 2131362600 */:
                bundle.putString("android.os.storage.extra.VOLUME_ID", this.mVolume.getId());
                new SubSettingLauncher(activity).setDestination(PrivateVolumeFormat.class.getCanonicalName()).setTitle(R.string.storage_menu_format).setSourceMetricsCategory(getMetricsCategory()).setArguments(bundle).launch();
                return true;
            case R.id.storage_free /* 2131362601 */:
                startActivity(new Intent("android.os.storage.action.MANAGE_STORAGE"));
                return true;
            case R.id.storage_migrate /* 2131362602 */:
                Intent intent = new Intent(activity, (Class<?>) StorageWizardMigrateConfirm.class);
                intent.putExtra("android.os.storage.extra.VOLUME_ID", this.mVolume.getId());
                startActivity(intent);
                return true;
            case R.id.storage_mount /* 2131362603 */:
                new StorageSettings.MountTask(activity, this.mVolume).execute(new Void[0]);
                return true;
            case R.id.storage_next_button /* 2131362604 */:
            default:
                return super.onOptionsItemSelected(menuItem);
            case R.id.storage_rename /* 2131362605 */:
                RenameFragment.show(this, this.mVolume);
                return true;
            case R.id.storage_unmount /* 2131362606 */:
                bundle.putString("android.os.storage.extra.VOLUME_ID", this.mVolume.getId());
                new SubSettingLauncher(activity).setDestination(PrivateVolumeUnmount.class.getCanonicalName()).setTitle(R.string.storage_menu_unmount).setSourceMetricsCategory(getMetricsCategory()).setArguments(bundle).launch();
                return true;
        }
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.support.v7.preference.PreferenceManager.OnPreferenceTreeClickListener
    public boolean onPreferenceTreeClick(Preference preference) throws NumberFormatException {
        int i;
        int i2 = preference instanceof StorageItemPreference ? ((StorageItemPreference) preference).userHandle : -1;
        try {
            i = Integer.parseInt(preference.getKey());
        } catch (NumberFormatException e) {
            i = 0;
        }
        Intent intentBuildBrowseIntent = null;
        if (i == 0) {
            UserInfoFragment.show(this, preference.getTitle(), preference.getSummary());
            return true;
        }
        if (i != R.string.storage_menu_explore) {
            switch (i) {
                case R.string.storage_detail_apps /* 2131889340 */:
                    Bundle bundle = new Bundle();
                    bundle.putString("classname", Settings.StorageUseActivity.class.getName());
                    bundle.putString("volumeUuid", this.mVolume.getFsUuid());
                    bundle.putString("volumeName", this.mVolume.getDescription());
                    bundle.putInt("storageType", 2);
                    intentBuildBrowseIntent = new SubSettingLauncher(getActivity()).setDestination(ManageApplications.class.getName()).setArguments(bundle).setTitle(R.string.apps_storage).setSourceMetricsCategory(getMetricsCategory()).toIntent();
                    break;
                case R.string.storage_detail_audio /* 2131889341 */:
                    intentBuildBrowseIntent = getIntentForStorage("com.android.providers.media.documents", "audio_root");
                    break;
                case R.string.storage_detail_cached /* 2131889342 */:
                    ConfirmClearCacheFragment.show(this);
                    return true;
                default:
                    switch (i) {
                        case R.string.storage_detail_images /* 2131889347 */:
                            intentBuildBrowseIntent = getIntentForStorage("com.android.providers.media.documents", "images_root");
                            break;
                        case R.string.storage_detail_other /* 2131889348 */:
                            OtherInfoFragment.show(this, this.mStorageManager.getBestVolumeDescription(this.mVolume), this.mSharedVolume, i2);
                            return true;
                        case R.string.storage_detail_system /* 2131889349 */:
                            SystemInfoFragment.show(this);
                            return true;
                        case R.string.storage_detail_videos /* 2131889350 */:
                            intentBuildBrowseIntent = getIntentForStorage("com.android.providers.media.documents", "videos_root");
                            break;
                    }
            }
        } else {
            intentBuildBrowseIntent = this.mSharedVolume.buildBrowseIntent();
        }
        if (intentBuildBrowseIntent != null) {
            intentBuildBrowseIntent.putExtra("android.intent.extra.USER_ID", i2);
            Utils.launchIntent(this, intentBuildBrowseIntent);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    private Intent getIntentForStorage(String str, String str2) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndType(DocumentsContract.buildRootUri(str, str2), "vnd.android.document/root");
        intent.addCategory("android.intent.category.DEFAULT");
        return intent;
    }

    private void updateDetails(StorageMeasurement.MeasurementDetails measurementDetails) throws NumberFormatException {
        int i;
        long j = 0;
        long j2 = 0;
        long j3 = 0;
        StorageItemPreference storageItemPreference = null;
        for (int i2 = 0; i2 < this.mItemPoolIndex; i2++) {
            StorageItemPreference storageItemPreference2 = this.mItemPreferencePool.get(i2);
            int i3 = storageItemPreference2.userHandle;
            try {
                i = Integer.parseInt(storageItemPreference2.getKey());
            } catch (NumberFormatException e) {
                i = 0;
            }
            if (i != 0) {
                switch (i) {
                    case R.string.storage_detail_apps /* 2131889340 */:
                        updatePreference(storageItemPreference2, measurementDetails.appsSize.get(i3));
                        j += measurementDetails.appsSize.get(i3);
                        break;
                    case R.string.storage_detail_audio /* 2131889341 */:
                        long j4 = totalValues(measurementDetails, i3, Environment.DIRECTORY_MUSIC, Environment.DIRECTORY_ALARMS, Environment.DIRECTORY_NOTIFICATIONS, Environment.DIRECTORY_RINGTONES, Environment.DIRECTORY_PODCASTS);
                        updatePreference(storageItemPreference2, j4);
                        j += j4;
                        break;
                    case R.string.storage_detail_cached /* 2131889342 */:
                        updatePreference(storageItemPreference2, measurementDetails.cacheSize);
                        j += measurementDetails.cacheSize;
                        break;
                    default:
                        switch (i) {
                            case R.string.storage_detail_images /* 2131889347 */:
                                long j5 = totalValues(measurementDetails, i3, Environment.DIRECTORY_DCIM, Environment.DIRECTORY_PICTURES);
                                updatePreference(storageItemPreference2, j5);
                                j += j5;
                                break;
                            case R.string.storage_detail_other /* 2131889348 */:
                                long j6 = totalValues(measurementDetails, i3, Environment.DIRECTORY_DOWNLOADS);
                                long j7 = measurementDetails.miscSize.get(i3);
                                j3 += j6;
                                j2 += j7;
                                j += j7 + j6;
                                storageItemPreference = storageItemPreference2;
                                break;
                            case R.string.storage_detail_system /* 2131889349 */:
                                updatePreference(storageItemPreference2, this.mSystemSize);
                                j += this.mSystemSize;
                                break;
                            case R.string.storage_detail_videos /* 2131889350 */:
                                long j8 = totalValues(measurementDetails, i3, Environment.DIRECTORY_MOVIES);
                                updatePreference(storageItemPreference2, j8);
                                j += j8;
                                break;
                        }
                }
            } else {
                long j9 = measurementDetails.usersSize.get(i3);
                updatePreference(storageItemPreference2, j9);
                j += j9;
            }
        }
        if (storageItemPreference != null) {
            long j10 = this.mTotalSize - measurementDetails.availSize;
            long j11 = j10 - j;
            Log.v("PrivateVolumeSettings", "Other items: \n\tmTotalSize: " + this.mTotalSize + " availSize: " + measurementDetails.availSize + " usedSize: " + j10 + "\n\taccountedSize: " + j + " unaccountedSize size: " + j11 + "\n\ttotalMiscSize: " + j2 + " totalDownloadsSize: " + j3 + "\n\tdetails: " + measurementDetails);
            updatePreference(storageItemPreference, j2 + j3 + j11);
        }
    }

    private void updatePreference(StorageItemPreference storageItemPreference, long j) {
        storageItemPreference.setStorageSize(j, this.mTotalSize);
    }

    private static long totalValues(StorageMeasurement.MeasurementDetails measurementDetails, int i, String... strArr) {
        HashMap<String, Long> map = measurementDetails.mediaSize.get(i);
        long jLongValue = 0;
        if (map != null) {
            for (String str : strArr) {
                if (map.containsKey(str)) {
                    jLongValue += map.get(str).longValue();
                }
            }
        } else {
            Log.w("PrivateVolumeSettings", "MeasurementDetails mediaSize array does not have key for user " + i);
        }
        return jLongValue;
    }

    public static class RenameFragment extends InstrumentedDialogFragment {
        public static void show(PrivateVolumeSettings privateVolumeSettings, VolumeInfo volumeInfo) {
            if (privateVolumeSettings.isAdded()) {
                RenameFragment renameFragment = new RenameFragment();
                renameFragment.setTargetFragment(privateVolumeSettings, 0);
                Bundle bundle = new Bundle();
                bundle.putString("android.os.storage.extra.FS_UUID", volumeInfo.getFsUuid());
                renameFragment.setArguments(bundle);
                renameFragment.show(privateVolumeSettings.getFragmentManager(), "rename");
            }
        }

        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 563;
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            Activity activity = getActivity();
            final StorageManager storageManager = (StorageManager) activity.getSystemService(StorageManager.class);
            final String string = getArguments().getString("android.os.storage.extra.FS_UUID");
            storageManager.findVolumeByUuid(string);
            VolumeRecord volumeRecordFindRecordByUuid = storageManager.findRecordByUuid(string);
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            View viewInflate = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_edittext, (ViewGroup) null, false);
            final EditText editText = (EditText) viewInflate.findViewById(R.id.edittext);
            editText.setText(volumeRecordFindRecordByUuid.getNickname());
            builder.setTitle(R.string.storage_rename_title);
            builder.setView(viewInflate);
            builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() { // from class: com.android.settings.deviceinfo.PrivateVolumeSettings.RenameFragment.1
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i) {
                    storageManager.setVolumeNickname(string, editText.getText().toString());
                }
            });
            builder.setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null);
            return builder.create();
        }
    }

    public static class SystemInfoFragment extends InstrumentedDialogFragment {
        public static void show(Fragment fragment) {
            if (fragment.isAdded()) {
                SystemInfoFragment systemInfoFragment = new SystemInfoFragment();
                systemInfoFragment.setTargetFragment(fragment, 0);
                systemInfoFragment.show(fragment.getFragmentManager(), "systemInfo");
            }
        }

        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 565;
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            return new AlertDialog.Builder(getActivity()).setMessage(getContext().getString(R.string.storage_detail_dialog_system, Build.VERSION.RELEASE)).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).create();
        }
    }

    public static class OtherInfoFragment extends InstrumentedDialogFragment {
        public static void show(Fragment fragment, String str, VolumeInfo volumeInfo, int i) {
            if (fragment.isAdded()) {
                OtherInfoFragment otherInfoFragment = new OtherInfoFragment();
                otherInfoFragment.setTargetFragment(fragment, 0);
                Bundle bundle = new Bundle();
                bundle.putString("android.intent.extra.TITLE", str);
                Intent intentBuildBrowseIntent = volumeInfo.buildBrowseIntent();
                intentBuildBrowseIntent.putExtra("android.intent.extra.USER_ID", i);
                bundle.putParcelable("android.intent.extra.INTENT", intentBuildBrowseIntent);
                otherInfoFragment.setArguments(bundle);
                otherInfoFragment.show(fragment.getFragmentManager(), "otherInfo");
            }
        }

        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 566;
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            Activity activity = getActivity();
            String string = getArguments().getString("android.intent.extra.TITLE");
            final Intent intent = (Intent) getArguments().getParcelable("android.intent.extra.INTENT");
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(TextUtils.expandTemplate(getText(R.string.storage_detail_dialog_other), string));
            builder.setPositiveButton(R.string.storage_menu_explore, new DialogInterface.OnClickListener() { // from class: com.android.settings.deviceinfo.PrivateVolumeSettings.OtherInfoFragment.1
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i) {
                    Utils.launchIntent(OtherInfoFragment.this, intent);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
            return builder.create();
        }
    }

    public static class UserInfoFragment extends InstrumentedDialogFragment {
        public static void show(Fragment fragment, CharSequence charSequence, CharSequence charSequence2) {
            if (fragment.isAdded()) {
                UserInfoFragment userInfoFragment = new UserInfoFragment();
                userInfoFragment.setTargetFragment(fragment, 0);
                Bundle bundle = new Bundle();
                bundle.putCharSequence("android.intent.extra.TITLE", charSequence);
                bundle.putCharSequence("android.intent.extra.SUBJECT", charSequence2);
                userInfoFragment.setArguments(bundle);
                userInfoFragment.show(fragment.getFragmentManager(), "userInfo");
            }
        }

        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 567;
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            Activity activity = getActivity();
            CharSequence charSequence = getArguments().getCharSequence("android.intent.extra.TITLE");
            CharSequence charSequence2 = getArguments().getCharSequence("android.intent.extra.SUBJECT");
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(TextUtils.expandTemplate(getText(R.string.storage_detail_dialog_user), charSequence, charSequence2));
            builder.setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null);
            return builder.create();
        }
    }

    public static class ConfirmClearCacheFragment extends InstrumentedDialogFragment {
        public static void show(Fragment fragment) {
            if (fragment.isAdded()) {
                ConfirmClearCacheFragment confirmClearCacheFragment = new ConfirmClearCacheFragment();
                confirmClearCacheFragment.setTargetFragment(fragment, 0);
                confirmClearCacheFragment.show(fragment.getFragmentManager(), "confirmClearCache");
            }
        }

        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 564;
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            final Activity activity = getActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(R.string.memory_clear_cache_title);
            builder.setMessage(getString(R.string.memory_clear_cache_message));
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() { // from class: com.android.settings.deviceinfo.PrivateVolumeSettings.ConfirmClearCacheFragment.1
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i) {
                    PrivateVolumeSettings privateVolumeSettings = (PrivateVolumeSettings) ConfirmClearCacheFragment.this.getTargetFragment();
                    PackageManager packageManager = activity.getPackageManager();
                    for (int i2 : ((UserManager) activity.getSystemService(UserManager.class)).getProfileIdsWithDisabled(activity.getUserId())) {
                        List installedPackagesAsUser = packageManager.getInstalledPackagesAsUser(0, i2);
                        ClearCacheObserver clearCacheObserver = new ClearCacheObserver(privateVolumeSettings, installedPackagesAsUser.size());
                        Iterator it = installedPackagesAsUser.iterator();
                        while (it.hasNext()) {
                            packageManager.deleteApplicationCacheFilesAsUser(((PackageInfo) it.next()).packageName, i2, clearCacheObserver);
                        }
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
            return builder.create();
        }
    }

    private static class ClearCacheObserver extends IPackageDataObserver.Stub {
        private int mRemaining;
        private final PrivateVolumeSettings mTarget;

        public ClearCacheObserver(PrivateVolumeSettings privateVolumeSettings, int i) {
            this.mTarget = privateVolumeSettings;
            this.mRemaining = i;
        }

        public void onRemoveCompleted(String str, boolean z) {
            synchronized (this) {
                int i = this.mRemaining - 1;
                this.mRemaining = i;
                if (i == 0) {
                    this.mTarget.getActivity().runOnUiThread(new Runnable() { // from class: com.android.settings.deviceinfo.PrivateVolumeSettings.ClearCacheObserver.1
                        @Override // java.lang.Runnable
                        public void run() {
                            ClearCacheObserver.this.mTarget.update();
                        }
                    });
                }
            }
        }
    }
}
