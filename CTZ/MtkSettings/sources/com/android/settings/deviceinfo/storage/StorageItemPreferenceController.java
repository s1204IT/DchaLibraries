package com.android.settings.deviceinfo.storage;

import android.R;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.storage.VolumeInfo;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import com.android.settings.Settings;
import com.android.settings.applications.manageapplications.ManageApplications;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.deviceinfo.PrivateVolumeSettings;
import com.android.settings.deviceinfo.StorageItemPreference;
import com.android.settings.deviceinfo.storage.StorageAsyncLoader;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.deviceinfo.StorageVolumeProvider;

public class StorageItemPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    static final String AUDIO_KEY = "pref_music_audio";
    static final String FILES_KEY = "pref_files";
    static final String GAME_KEY = "pref_games";
    static final String MOVIES_KEY = "pref_movies";
    static final String OTHER_APPS_KEY = "pref_other_apps";
    static final String PHOTO_KEY = "pref_photos_videos";
    static final String SYSTEM_KEY = "pref_system";
    private StorageItemPreference mAppPreference;
    private StorageItemPreference mAudioPreference;
    private StorageItemPreference mFilePreference;
    private final Fragment mFragment;
    private StorageItemPreference mGamePreference;
    private boolean mIsWorkProfile;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private StorageItemPreference mMoviesPreference;
    private StorageItemPreference mPhotoPreference;
    private PreferenceScreen mScreen;
    private final StorageVolumeProvider mSvp;
    private StorageItemPreference mSystemPreference;
    private long mTotalSize;
    private long mUsedBytes;
    private int mUserId;
    private VolumeInfo mVolume;

    public StorageItemPreferenceController(Context context, Fragment fragment, VolumeInfo volumeInfo, StorageVolumeProvider storageVolumeProvider) {
        super(context);
        this.mFragment = fragment;
        this.mVolume = volumeInfo;
        this.mSvp = storageVolumeProvider;
        this.mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        this.mUserId = UserHandle.myUserId();
    }

    public StorageItemPreferenceController(Context context, Fragment fragment, VolumeInfo volumeInfo, StorageVolumeProvider storageVolumeProvider, boolean z) {
        this(context, fragment, volumeInfo, storageVolumeProvider);
        this.mIsWorkProfile = z;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (preference == null) {
            return false;
        }
        Intent photosIntent = null;
        if (preference.getKey() == null) {
            return false;
        }
        switch (preference.getKey()) {
            case "pref_photos_videos":
                photosIntent = getPhotosIntent();
                break;
            case "pref_music_audio":
                photosIntent = getAudioIntent();
                break;
            case "pref_games":
                photosIntent = getGamesIntent();
                break;
            case "pref_movies":
                photosIntent = getMoviesIntent();
                break;
            case "pref_other_apps":
                if (this.mVolume != null) {
                    photosIntent = getAppsIntent();
                    break;
                }
                break;
            case "pref_files":
                photosIntent = getFilesIntent();
                FeatureFactory.getFactory(this.mContext).getMetricsFeatureProvider().action(this.mContext, 841, new Pair[0]);
                break;
            case "pref_system":
                PrivateVolumeSettings.SystemInfoFragment systemInfoFragment = new PrivateVolumeSettings.SystemInfoFragment();
                systemInfoFragment.setTargetFragment(this.mFragment, 0);
                systemInfoFragment.show(this.mFragment.getFragmentManager(), "SystemInfo");
                return true;
        }
        if (photosIntent != null) {
            photosIntent.putExtra("android.intent.extra.USER_ID", this.mUserId);
            launchIntent(photosIntent);
            return true;
        }
        return super.handlePreferenceTreeClick(preference);
    }

    @Override
    public String getPreferenceKey() {
        return null;
    }

    public void setVolume(VolumeInfo volumeInfo) {
        this.mVolume = volumeInfo;
        setFilesPreferenceVisibility();
    }

    private void setFilesPreferenceVisibility() {
        if (this.mScreen != null) {
            VolumeInfo volumeInfoFindEmulatedForPrivate = this.mSvp.findEmulatedForPrivate(this.mVolume);
            if (volumeInfoFindEmulatedForPrivate == null || !volumeInfoFindEmulatedForPrivate.isMountedReadable()) {
                this.mScreen.removePreference(this.mFilePreference);
            } else {
                this.mScreen.addPreference(this.mFilePreference);
            }
        }
    }

    public void setUserId(UserHandle userHandle) {
        this.mUserId = userHandle.getIdentifier();
        tintPreference(this.mPhotoPreference);
        tintPreference(this.mMoviesPreference);
        tintPreference(this.mAudioPreference);
        tintPreference(this.mGamePreference);
        tintPreference(this.mAppPreference);
        tintPreference(this.mSystemPreference);
        tintPreference(this.mFilePreference);
    }

    private void tintPreference(Preference preference) {
        if (preference != null) {
            preference.setIcon(applyTint(this.mContext, preference.getIcon()));
        }
    }

    private static Drawable applyTint(Context context, Drawable drawable) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(new int[]{R.attr.colorControlNormal});
        Drawable drawableMutate = drawable.mutate();
        drawableMutate.setTint(typedArrayObtainStyledAttributes.getColor(0, 0));
        typedArrayObtainStyledAttributes.recycle();
        return drawableMutate;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        this.mScreen = preferenceScreen;
        this.mPhotoPreference = (StorageItemPreference) preferenceScreen.findPreference(PHOTO_KEY);
        this.mAudioPreference = (StorageItemPreference) preferenceScreen.findPreference(AUDIO_KEY);
        this.mGamePreference = (StorageItemPreference) preferenceScreen.findPreference(GAME_KEY);
        this.mMoviesPreference = (StorageItemPreference) preferenceScreen.findPreference(MOVIES_KEY);
        this.mAppPreference = (StorageItemPreference) preferenceScreen.findPreference(OTHER_APPS_KEY);
        this.mSystemPreference = (StorageItemPreference) preferenceScreen.findPreference(SYSTEM_KEY);
        this.mFilePreference = (StorageItemPreference) preferenceScreen.findPreference(FILES_KEY);
        setFilesPreferenceVisibility();
    }

    public void onLoadFinished(SparseArray<StorageAsyncLoader.AppsStorageResult> sparseArray, int i) {
        StorageAsyncLoader.AppsStorageResult appsStorageResult = sparseArray.get(i);
        this.mPhotoPreference.setStorageSize(appsStorageResult.photosAppsSize + appsStorageResult.externalStats.imageBytes + appsStorageResult.externalStats.videoBytes, this.mTotalSize);
        this.mAudioPreference.setStorageSize(appsStorageResult.musicAppsSize + appsStorageResult.externalStats.audioBytes, this.mTotalSize);
        this.mGamePreference.setStorageSize(appsStorageResult.gamesSize, this.mTotalSize);
        this.mMoviesPreference.setStorageSize(appsStorageResult.videoAppsSize, this.mTotalSize);
        this.mAppPreference.setStorageSize(appsStorageResult.otherAppsSize, this.mTotalSize);
        this.mFilePreference.setStorageSize((((appsStorageResult.externalStats.totalBytes - appsStorageResult.externalStats.audioBytes) - appsStorageResult.externalStats.videoBytes) - appsStorageResult.externalStats.imageBytes) - appsStorageResult.externalStats.appBytes, this.mTotalSize);
        if (this.mSystemPreference != null) {
            long j = 0;
            for (int i2 = 0; i2 < sparseArray.size(); i2++) {
                StorageAsyncLoader.AppsStorageResult appsStorageResultValueAt = sparseArray.valueAt(i2);
                j = j + appsStorageResultValueAt.gamesSize + appsStorageResultValueAt.musicAppsSize + appsStorageResultValueAt.videoAppsSize + appsStorageResultValueAt.photosAppsSize + appsStorageResultValueAt.otherAppsSize + (appsStorageResultValueAt.externalStats.totalBytes - appsStorageResultValueAt.externalStats.appBytes);
            }
            this.mSystemPreference.setStorageSize(Math.max(1073741824L, this.mUsedBytes - j), this.mTotalSize);
        }
    }

    public void setUsedSize(long j) {
        this.mUsedBytes = j;
    }

    public void setTotalSize(long j) {
        this.mTotalSize = j;
    }

    private Intent getPhotosIntent() {
        Bundle workAnnotatedBundle = getWorkAnnotatedBundle(2);
        workAnnotatedBundle.putString("classname", Settings.PhotosStorageActivity.class.getName());
        workAnnotatedBundle.putInt("storageType", 3);
        return new SubSettingLauncher(this.mContext).setDestination(ManageApplications.class.getName()).setTitle(com.android.settings.R.string.storage_photos_videos).setArguments(workAnnotatedBundle).setSourceMetricsCategory(this.mMetricsFeatureProvider.getMetricsCategory(this.mFragment)).toIntent();
    }

    private Intent getAudioIntent() {
        if (this.mVolume == null) {
            return null;
        }
        Bundle workAnnotatedBundle = getWorkAnnotatedBundle(4);
        workAnnotatedBundle.putString("classname", Settings.StorageUseActivity.class.getName());
        workAnnotatedBundle.putString("volumeUuid", this.mVolume.getFsUuid());
        workAnnotatedBundle.putString("volumeName", this.mVolume.getDescription());
        workAnnotatedBundle.putInt("storageType", 1);
        return new SubSettingLauncher(this.mContext).setDestination(ManageApplications.class.getName()).setTitle(com.android.settings.R.string.storage_music_audio).setArguments(workAnnotatedBundle).setSourceMetricsCategory(this.mMetricsFeatureProvider.getMetricsCategory(this.mFragment)).toIntent();
    }

    private Intent getAppsIntent() {
        if (this.mVolume == null) {
            return null;
        }
        Bundle workAnnotatedBundle = getWorkAnnotatedBundle(3);
        workAnnotatedBundle.putString("classname", Settings.StorageUseActivity.class.getName());
        workAnnotatedBundle.putString("volumeUuid", this.mVolume.getFsUuid());
        workAnnotatedBundle.putString("volumeName", this.mVolume.getDescription());
        return new SubSettingLauncher(this.mContext).setDestination(ManageApplications.class.getName()).setTitle(com.android.settings.R.string.apps_storage).setArguments(workAnnotatedBundle).setSourceMetricsCategory(this.mMetricsFeatureProvider.getMetricsCategory(this.mFragment)).toIntent();
    }

    private Intent getGamesIntent() {
        Bundle workAnnotatedBundle = getWorkAnnotatedBundle(1);
        workAnnotatedBundle.putString("classname", Settings.GamesStorageActivity.class.getName());
        return new SubSettingLauncher(this.mContext).setDestination(ManageApplications.class.getName()).setTitle(com.android.settings.R.string.game_storage_settings).setArguments(workAnnotatedBundle).setSourceMetricsCategory(this.mMetricsFeatureProvider.getMetricsCategory(this.mFragment)).toIntent();
    }

    private Intent getMoviesIntent() {
        Bundle workAnnotatedBundle = getWorkAnnotatedBundle(1);
        workAnnotatedBundle.putString("classname", Settings.MoviesStorageActivity.class.getName());
        return new SubSettingLauncher(this.mContext).setDestination(ManageApplications.class.getName()).setTitle(com.android.settings.R.string.storage_movies_tv).setArguments(workAnnotatedBundle).setSourceMetricsCategory(this.mMetricsFeatureProvider.getMetricsCategory(this.mFragment)).toIntent();
    }

    private Bundle getWorkAnnotatedBundle(int i) {
        Bundle bundle = new Bundle(2 + i);
        bundle.putBoolean("workProfileOnly", this.mIsWorkProfile);
        bundle.putInt("workId", this.mUserId);
        return bundle;
    }

    private Intent getFilesIntent() {
        return this.mSvp.findEmulatedForPrivate(this.mVolume).buildBrowseIntent();
    }

    private void launchIntent(Intent intent) {
        try {
            int intExtra = intent.getIntExtra("android.intent.extra.USER_ID", -1);
            if (intExtra == -1) {
                this.mFragment.startActivity(intent);
            } else {
                this.mFragment.getActivity().startActivityAsUser(intent, new UserHandle(intExtra));
            }
        } catch (ActivityNotFoundException e) {
            Log.w("StorageItemPreference", "No activity found for " + intent);
        }
    }
}
