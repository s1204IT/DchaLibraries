package com.android.settings.deviceinfo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
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
import com.android.settings.applications.ManageApplications;
import com.android.settings.deviceinfo.StorageSettings;
import com.android.settingslib.deviceinfo.StorageMeasurement;
import com.google.android.collect.Lists;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import com.mediatek.settings.ext.IStorageSettingsExt;
import com.mediatek.storage.StorageManagerEx;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class PrivateVolumeSettings extends SettingsPreferenceFragment {
    private static final int[] ITEMS_NO_SHOW_SHARED = {R.string.storage_detail_apps};
    private static final int[] ITEMS_SHOW_SHARED = {R.string.storage_detail_apps, R.string.storage_detail_images, R.string.storage_detail_videos, R.string.storage_detail_audio, R.string.storage_detail_other};
    private UserInfo mCurrentUser;
    private Preference mExplore;
    private IStorageSettingsExt mExt;
    private int mHeaderPoolIndex;
    private int mItemPoolIndex;
    private StorageMeasurement mMeasure;
    private boolean mNeedsUpdate;
    private VolumeInfo mSharedVolume;
    private StorageManager mStorageManager;
    private StorageSummaryPreference mSummary;
    private UserManager mUserManager;
    private VolumeInfo mVolume;
    private String mVolumeId;
    private List<StorageItemPreference> mItemPreferencePool = Lists.newArrayList();
    private List<PreferenceCategory> mHeaderPreferencePool = Lists.newArrayList();
    private final StorageMeasurement.MeasurementReceiver mReceiver = new StorageMeasurement.MeasurementReceiver() {
        @Override
        public void onDetailsChanged(StorageMeasurement.MeasurementDetails details) {
            PrivateVolumeSettings.this.updateDetails(details);
            PrivateVolumeSettings.this.mExt.updateCustomizedPrefDetails(PrivateVolumeSettings.this.mVolume);
        }
    };
    private final StorageEventListener mStorageListener = new StorageEventListener() {
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            if (!Objects.equals(PrivateVolumeSettings.this.mVolume.getId(), vol.getId())) {
                return;
            }
            PrivateVolumeSettings.this.mVolume = vol;
            PrivateVolumeSettings.this.update();
        }

        public void onVolumeRecordChanged(VolumeRecord rec) {
            if (!Objects.equals(PrivateVolumeSettings.this.mVolume.getFsUuid(), rec.getFsUuid())) {
                return;
            }
            PrivateVolumeSettings.this.mVolume = PrivateVolumeSettings.this.mStorageManager.findVolumeById(PrivateVolumeSettings.this.mVolumeId);
            PrivateVolumeSettings.this.update();
        }
    };

    private boolean isVolumeValid() {
        if (this.mVolume == null || this.mVolume.getType() != 1) {
            return false;
        }
        return this.mVolume.isMountedReadable();
    }

    public PrivateVolumeSettings() {
        setRetainInstance(true);
    }

    @Override
    protected int getMetricsCategory() {
        return 42;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Context context = getActivity();
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
        this.mStorageManager = (StorageManager) context.getSystemService(StorageManager.class);
        this.mVolumeId = getArguments().getString("android.os.storage.extra.VOLUME_ID");
        this.mVolume = this.mStorageManager.findVolumeById(this.mVolumeId);
        this.mSharedVolume = this.mStorageManager.findEmulatedForPrivate(this.mVolume);
        this.mMeasure = new StorageMeasurement(context, this.mVolume, this.mSharedVolume);
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
        this.mExt = UtilsExt.getStorageSettingsPlugin(context);
        this.mExt.initCustomizationStoragePlugin(context);
    }

    private void setTitle() {
        getActivity().setTitle(this.mStorageManager.getBestVolumeDescription(this.mVolume));
    }

    public void update() {
        if (!isVolumeValid()) {
            getActivity().finish();
            return;
        }
        setTitle();
        getFragmentManager().invalidateOptionsMenu();
        Context context = getActivity();
        PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();
        addPreference(screen, this.mSummary);
        List<UserInfo> allUsers = this.mUserManager.getUsers();
        int userCount = allUsers.size();
        boolean showHeaders = userCount > 1;
        boolean zIsMountedReadable = this.mSharedVolume != null ? this.mSharedVolume.isMountedReadable() : false;
        this.mItemPoolIndex = 0;
        this.mHeaderPoolIndex = 0;
        int addedUserCount = 0;
        for (int userIndex = 0; userIndex < userCount; userIndex++) {
            UserInfo userInfo = allUsers.get(userIndex);
            if (isProfileOf(this.mCurrentUser, userInfo)) {
                PreferenceGroup details = showHeaders ? addCategory(screen, userInfo.name) : screen;
                addDetailItems(details, zIsMountedReadable, userInfo.id);
                addedUserCount++;
            }
        }
        if (userCount - addedUserCount > 0) {
            PreferenceGroup otherUsers = addCategory(screen, getText(R.string.storage_other_users));
            for (int userIndex2 = 0; userIndex2 < userCount; userIndex2++) {
                UserInfo userInfo2 = allUsers.get(userIndex2);
                if (!isProfileOf(this.mCurrentUser, userInfo2)) {
                    addItem(otherUsers, 0, userInfo2.name, userInfo2.id);
                }
            }
        }
        addItem(screen, R.string.storage_detail_cached, null, -10000);
        if (zIsMountedReadable) {
            addPreference(screen, this.mExplore);
        }
        File file = this.mVolume.getPath();
        long totalBytes = file.getTotalSpace();
        long freeBytes = file.getFreeSpace();
        long usedBytes = totalBytes - freeBytes;
        Formatter.BytesResult result = Formatter.formatBytes(getResources(), usedBytes, 0);
        this.mSummary.setTitle(TextUtils.expandTemplate(getText(R.string.storage_size_large), result.value, result.units));
        this.mSummary.setSummary(getString(R.string.storage_volume_used, new Object[]{Formatter.formatFileSize(context, totalBytes)}));
        this.mSummary.setPercent((int) ((100 * usedBytes) / totalBytes));
        this.mExt.updateCustomizedPrivateSettingsPlugin(screen, this.mVolume);
        this.mMeasure.forceMeasure();
        this.mNeedsUpdate = false;
    }

    private void addPreference(PreferenceGroup group, Preference pref) {
        pref.setOrder(Integer.MAX_VALUE);
        group.addPreference(pref);
    }

    private PreferenceCategory addCategory(PreferenceGroup group, CharSequence title) {
        PreferenceCategory category;
        if (this.mHeaderPoolIndex < this.mHeaderPreferencePool.size()) {
            category = this.mHeaderPreferencePool.get(this.mHeaderPoolIndex);
        } else {
            category = new PreferenceCategory(getPrefContext(), null, android.R.attr.preferenceCategoryStyle);
            this.mHeaderPreferencePool.add(category);
        }
        category.setTitle(title);
        category.removeAll();
        addPreference(group, category);
        this.mHeaderPoolIndex++;
        return category;
    }

    private void addDetailItems(PreferenceGroup category, boolean showShared, int userId) {
        int[] itemsToAdd = showShared ? ITEMS_SHOW_SHARED : ITEMS_NO_SHOW_SHARED;
        for (int i : itemsToAdd) {
            addItem(category, i, null, userId);
        }
    }

    private void addItem(PreferenceGroup group, int titleRes, CharSequence title, int userId) {
        StorageItemPreference item;
        if (this.mItemPoolIndex < this.mItemPreferencePool.size()) {
            item = this.mItemPreferencePool.get(this.mItemPoolIndex);
        } else {
            item = buildItem();
            this.mItemPreferencePool.add(item);
        }
        if (title != null) {
            item.setTitle(title);
            item.setKey(title.toString());
        } else {
            item.setTitle(titleRes);
            item.setKey(Integer.toString(titleRes));
        }
        item.setSummary(R.string.memory_calculating_size);
        item.userHandle = userId;
        addPreference(group, item);
        this.mItemPoolIndex++;
    }

    private StorageItemPreference buildItem() {
        StorageItemPreference item = new StorageItemPreference(getPrefContext());
        return item;
    }

    private Preference buildAction(int titleRes) {
        Preference pref = new Preference(getPrefContext());
        pref.setTitle(titleRes);
        pref.setKey(Integer.toString(titleRes));
        return pref;
    }

    @Override
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

    @Override
    public void onPause() {
        super.onPause();
        this.mStorageManager.unregisterListener(this.mStorageListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mMeasure == null) {
            return;
        }
        this.mMeasure.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.storage_volume, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        boolean zIsSetPrimaryStorageUuidFinished = false;
        if (isVolumeValid()) {
            MenuItem rename = menu.findItem(R.id.storage_rename);
            MenuItem mount = menu.findItem(R.id.storage_mount);
            MenuItem unmount = menu.findItem(R.id.storage_unmount);
            MenuItem format = menu.findItem(R.id.storage_format);
            MenuItem migrate = menu.findItem(R.id.storage_migrate);
            if ("private".equals(this.mVolume.getId())) {
                rename.setVisible(false);
                mount.setVisible(false);
                unmount.setVisible(false);
                format.setVisible(false);
            } else {
                rename.setVisible(this.mVolume.getType() == 1);
                mount.setVisible(this.mVolume.getState() == 0);
                unmount.setVisible(this.mVolume.isMountedReadable());
                format.setVisible(true);
            }
            format.setTitle(R.string.storage_menu_format_public);
            VolumeInfo privateVol = getActivity().getPackageManager().getPrimaryStorageCurrentVolume();
            if (privateVol != null && privateVol.getType() == 1 && !Objects.equals(this.mVolume, privateVol)) {
                zIsSetPrimaryStorageUuidFinished = StorageManagerEx.isSetPrimaryStorageUuidFinished();
            }
            migrate.setVisible(zIsSetPrimaryStorageUuidFinished);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Context context = getActivity();
        Bundle args = new Bundle();
        switch (item.getItemId()) {
            case R.id.storage_rename:
                RenameFragment.show(this, this.mVolume);
                return true;
            case R.id.storage_mount:
                new StorageSettings.MountTask(context, this.mVolume).execute(new Void[0]);
                return true;
            case R.id.storage_unmount:
                args.putString("android.os.storage.extra.VOLUME_ID", this.mVolume.getId());
                startFragment(this, PrivateVolumeUnmount.class.getCanonicalName(), R.string.storage_menu_unmount, 0, args);
                return true;
            case R.id.storage_format:
                args.putString("android.os.storage.extra.VOLUME_ID", this.mVolume.getId());
                startFragment(this, PrivateVolumeFormat.class.getCanonicalName(), R.string.storage_menu_format, 0, args);
                return true;
            case R.id.storage_migrate:
                Intent intent = new Intent(context, (Class<?>) StorageWizardMigrateConfirm.class);
                intent.putExtra("android.os.storage.extra.VOLUME_ID", this.mVolume.getId());
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference pref) {
        int itemTitleId;
        int userId = pref instanceof StorageItemPreference ? ((StorageItemPreference) pref).userHandle : -1;
        try {
            itemTitleId = Integer.parseInt(pref.getKey());
        } catch (NumberFormatException e) {
            itemTitleId = 0;
        }
        Intent intent = null;
        switch (itemTitleId) {
            case DefaultWfcSettingsExt.RESUME:
                UserInfoFragment.show(this, pref.getTitle(), pref.getSummary());
                return true;
            case R.string.storage_menu_explore:
                intent = this.mSharedVolume.buildBrowseIntent();
                break;
            case R.string.storage_detail_apps:
                Bundle args = new Bundle();
                args.putString("classname", Settings.StorageUseActivity.class.getName());
                args.putString("volumeUuid", this.mVolume.getFsUuid());
                args.putString("volumeName", this.mVolume.getDescription());
                intent = Utils.onBuildStartFragmentIntent(getActivity(), ManageApplications.class.getName(), args, null, R.string.apps_storage, null, false);
                break;
            case R.string.storage_detail_images:
                intent = new Intent("android.provider.action.BROWSE");
                intent.setData(DocumentsContract.buildRootUri("com.android.providers.media.documents", "images_root"));
                intent.addCategory("android.intent.category.DEFAULT");
                break;
            case R.string.storage_detail_videos:
                intent = new Intent("android.provider.action.BROWSE");
                intent.setData(DocumentsContract.buildRootUri("com.android.providers.media.documents", "videos_root"));
                intent.addCategory("android.intent.category.DEFAULT");
                break;
            case R.string.storage_detail_audio:
                intent = new Intent("android.provider.action.BROWSE");
                intent.setData(DocumentsContract.buildRootUri("com.android.providers.media.documents", "audio_root"));
                intent.addCategory("android.intent.category.DEFAULT");
                break;
            case R.string.storage_detail_cached:
                ConfirmClearCacheFragment.show(this);
                return true;
            case R.string.storage_detail_other:
                OtherInfoFragment.show(this, this.mStorageManager.getBestVolumeDescription(this.mVolume), this.mSharedVolume);
                return true;
        }
        if (intent != null) {
            try {
                if (userId == -1) {
                    startActivity(intent);
                } else {
                    getActivity().startActivityAsUser(intent, new UserHandle(userId));
                }
            } catch (ActivityNotFoundException e2) {
                Log.w("StorageSettings", "No activity found for " + intent);
            }
            return true;
        }
        return super.onPreferenceTreeClick(pref);
    }

    public void updateDetails(StorageMeasurement.MeasurementDetails details) {
        int itemTitleId;
        for (int i = 0; i < this.mItemPoolIndex; i++) {
            StorageItemPreference item = this.mItemPreferencePool.get(i);
            int userId = item.userHandle;
            try {
                itemTitleId = Integer.parseInt(item.getKey());
            } catch (NumberFormatException e) {
                itemTitleId = 0;
            }
            switch (itemTitleId) {
                case DefaultWfcSettingsExt.RESUME:
                    long userSize = details.usersSize.get(userId);
                    updatePreference(item, userSize);
                    break;
                case R.string.storage_detail_apps:
                    updatePreference(item, details.appsSize.get(userId));
                    break;
                case R.string.storage_detail_images:
                    long imagesSize = totalValues(details, userId, Environment.DIRECTORY_DCIM, Environment.DIRECTORY_MOVIES, Environment.DIRECTORY_PICTURES);
                    updatePreference(item, imagesSize);
                    break;
                case R.string.storage_detail_videos:
                    long videosSize = totalValues(details, userId, Environment.DIRECTORY_MOVIES);
                    updatePreference(item, videosSize);
                    break;
                case R.string.storage_detail_audio:
                    long audioSize = totalValues(details, userId, Environment.DIRECTORY_MUSIC, Environment.DIRECTORY_ALARMS, Environment.DIRECTORY_NOTIFICATIONS, Environment.DIRECTORY_RINGTONES, Environment.DIRECTORY_PODCASTS);
                    updatePreference(item, audioSize);
                    break;
                case R.string.storage_detail_cached:
                    updatePreference(item, details.cacheSize);
                    break;
                case R.string.storage_detail_other:
                    updatePreference(item, details.miscSize.get(userId));
                    break;
            }
        }
    }

    private void updatePreference(StorageItemPreference pref, long size) {
        pref.setStorageSize(size, this.mVolume.getPath().getTotalSpace());
    }

    private boolean isProfileOf(UserInfo user, UserInfo profile) {
        if (user.id != profile.id) {
            return user.profileGroupId != -10000 && user.profileGroupId == profile.profileGroupId;
        }
        return true;
    }

    private static long totalValues(StorageMeasurement.MeasurementDetails details, int userId, String... keys) {
        long total = 0;
        HashMap<String, Long> map = details.mediaSize.get(userId);
        if (map != null) {
            for (String key : keys) {
                if (map.containsKey(key)) {
                    total += map.get(key).longValue();
                }
            }
        } else {
            Log.w("StorageSettings", "MeasurementDetails mediaSize array does not have key for user " + userId);
        }
        return total;
    }

    public static class RenameFragment extends DialogFragment {
        public static void show(PrivateVolumeSettings parent, VolumeInfo vol) {
            if (parent.isAdded()) {
                RenameFragment dialog = new RenameFragment();
                dialog.setTargetFragment(parent, 0);
                Bundle args = new Bundle();
                args.putString("android.os.storage.extra.FS_UUID", vol.getFsUuid());
                dialog.setArguments(args);
                dialog.show(parent.getFragmentManager(), "rename");
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            final StorageManager storageManager = (StorageManager) context.getSystemService(StorageManager.class);
            final String fsUuid = getArguments().getString("android.os.storage.extra.FS_UUID");
            storageManager.findVolumeByUuid(fsUuid);
            VolumeRecord rec = storageManager.findRecordByUuid(fsUuid);
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());
            View view = dialogInflater.inflate(R.layout.dialog_edittext, (ViewGroup) null, false);
            final EditText nickname = (EditText) view.findViewById(R.id.edittext);
            nickname.setText(rec.getNickname());
            builder.setTitle(R.string.storage_rename_title);
            builder.setView(view);
            builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    storageManager.setVolumeNickname(fsUuid, nickname.getText().toString());
                }
            });
            builder.setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null);
            return builder.create();
        }
    }

    public static class OtherInfoFragment extends DialogFragment {
        public static void show(Fragment parent, String title, VolumeInfo sharedVol) {
            if (parent.isAdded()) {
                OtherInfoFragment dialog = new OtherInfoFragment();
                dialog.setTargetFragment(parent, 0);
                Bundle args = new Bundle();
                args.putString("android.intent.extra.TITLE", title);
                args.putParcelable("android.intent.extra.INTENT", sharedVol.buildBrowseIntent());
                dialog.setArguments(args);
                dialog.show(parent.getFragmentManager(), "otherInfo");
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            String title = getArguments().getString("android.intent.extra.TITLE");
            final Intent intent = (Intent) getArguments().getParcelable("android.intent.extra.INTENT");
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(TextUtils.expandTemplate(getText(R.string.storage_detail_dialog_other), title));
            builder.setPositiveButton(R.string.storage_menu_explore, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    OtherInfoFragment.this.startActivity(intent);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
            return builder.create();
        }
    }

    public static class UserInfoFragment extends DialogFragment {
        public static void show(Fragment parent, CharSequence userLabel, CharSequence userSize) {
            if (parent.isAdded()) {
                UserInfoFragment dialog = new UserInfoFragment();
                dialog.setTargetFragment(parent, 0);
                Bundle args = new Bundle();
                args.putCharSequence("android.intent.extra.TITLE", userLabel);
                args.putCharSequence("android.intent.extra.SUBJECT", userSize);
                dialog.setArguments(args);
                dialog.show(parent.getFragmentManager(), "userInfo");
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            CharSequence userLabel = getArguments().getCharSequence("android.intent.extra.TITLE");
            CharSequence userSize = getArguments().getCharSequence("android.intent.extra.SUBJECT");
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(TextUtils.expandTemplate(getText(R.string.storage_detail_dialog_user), userLabel, userSize));
            builder.setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null);
            return builder.create();
        }
    }

    public static class ConfirmClearCacheFragment extends DialogFragment {
        public static void show(Fragment parent) {
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
                    PrivateVolumeSettings target = (PrivateVolumeSettings) ConfirmClearCacheFragment.this.getTargetFragment();
                    PackageManager pm = context.getPackageManager();
                    UserManager um = (UserManager) context.getSystemService(UserManager.class);
                    for (int userId : um.getProfileIdsWithDisabled(context.getUserId())) {
                        List<PackageInfo> infos = pm.getInstalledPackagesAsUser(0, userId);
                        ClearCacheObserver observer = new ClearCacheObserver(target, infos.size());
                        for (PackageInfo info : infos) {
                            pm.deleteApplicationCacheFilesAsUser(info.packageName, userId, observer);
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

        public ClearCacheObserver(PrivateVolumeSettings target, int remaining) {
            this.mTarget = target;
            this.mRemaining = remaining;
        }

        public void onRemoveCompleted(String packageName, boolean succeeded) {
            synchronized (this) {
                int i = this.mRemaining - 1;
                this.mRemaining = i;
                if (i == 0) {
                    this.mTarget.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ClearCacheObserver.this.mTarget.update();
                        }
                    });
                }
            }
        }
    }
}
