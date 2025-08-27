package com.android.settings.deviceinfo.storage;

import android.R;
import android.content.Context;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.SparseArray;
import com.android.internal.util.Preconditions;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.deviceinfo.StorageItemPreference;
import com.android.settings.deviceinfo.StorageProfileFragment;
import com.android.settings.deviceinfo.storage.StorageAsyncLoader;
import com.android.settings.deviceinfo.storage.UserIconLoader;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class UserProfileController extends AbstractPreferenceController implements PreferenceControllerMixin, StorageAsyncLoader.ResultHandler, UserIconLoader.UserIconHandler {
    private final int mPreferenceOrder;
    private StorageItemPreference mStoragePreference;
    private long mTotalSizeBytes;
    private UserInfo mUser;

    public UserProfileController(Context context, UserInfo userInfo, int i) {
        super(context);
        this.mUser = (UserInfo) Preconditions.checkNotNull(userInfo);
        this.mPreferenceOrder = i;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "pref_profile_" + this.mUser.id;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        this.mStoragePreference = new StorageItemPreference(preferenceScreen.getContext());
        this.mStoragePreference.setOrder(this.mPreferenceOrder);
        this.mStoragePreference.setKey("pref_profile_" + this.mUser.id);
        this.mStoragePreference.setTitle(this.mUser.name);
        preferenceScreen.addPreference(this.mStoragePreference);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (preference != null && this.mStoragePreference == preference) {
            Bundle bundle = new Bundle();
            bundle.putInt("userId", this.mUser.id);
            bundle.putString("android.os.storage.extra.VOLUME_ID", "private");
            new SubSettingLauncher(this.mContext).setDestination(StorageProfileFragment.class.getName()).setArguments(bundle).setTitle(this.mUser.name).setSourceMetricsCategory(42).launch();
            return true;
        }
        return false;
    }

    @Override // com.android.settings.deviceinfo.storage.StorageAsyncLoader.ResultHandler
    public void handleResult(SparseArray<StorageAsyncLoader.AppsStorageResult> sparseArray) {
        Preconditions.checkNotNull(sparseArray);
        StorageAsyncLoader.AppsStorageResult appsStorageResult = sparseArray.get(this.mUser.id);
        if (appsStorageResult != null) {
            setSize(appsStorageResult.externalStats.totalBytes + appsStorageResult.otherAppsSize + appsStorageResult.videoAppsSize + appsStorageResult.musicAppsSize + appsStorageResult.gamesSize, this.mTotalSizeBytes);
        }
    }

    public void setSize(long j, long j2) {
        if (this.mStoragePreference != null) {
            this.mStoragePreference.setStorageSize(j, j2);
        }
    }

    @Override // com.android.settings.deviceinfo.storage.UserIconLoader.UserIconHandler
    public void handleUserIcons(SparseArray<Drawable> sparseArray) {
        Drawable drawable = sparseArray.get(this.mUser.id);
        if (drawable != null) {
            this.mStoragePreference.setIcon(applyTint(this.mContext, drawable));
        }
    }

    private static Drawable applyTint(Context context, Drawable drawable) {
        Drawable drawableMutate = drawable.mutate();
        drawableMutate.setTint(Utils.getColorAttr(context, R.attr.colorControlNormal));
        return drawableMutate;
    }
}
