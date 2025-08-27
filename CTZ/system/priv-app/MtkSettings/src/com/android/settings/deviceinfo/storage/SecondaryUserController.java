package com.android.settings.deviceinfo.storage;

import android.content.Context;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.UserManager;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.util.SparseArray;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.deviceinfo.StorageItemPreference;
import com.android.settings.deviceinfo.storage.StorageAsyncLoader;
import com.android.settings.deviceinfo.storage.UserIconLoader;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class SecondaryUserController extends AbstractPreferenceController implements PreferenceControllerMixin, StorageAsyncLoader.ResultHandler, UserIconLoader.UserIconHandler {
    private long mSize;
    private StorageItemPreference mStoragePreference;
    private long mTotalSizeBytes;
    private UserInfo mUser;
    private Drawable mUserIcon;

    public static List<AbstractPreferenceController> getSecondaryUserControllers(Context context, UserManager userManager) {
        ArrayList arrayList = new ArrayList();
        UserInfo primaryUser = userManager.getPrimaryUser();
        List users = userManager.getUsers();
        int size = users.size();
        boolean z = false;
        for (int i = 0; i < size; i++) {
            UserInfo userInfo = (UserInfo) users.get(i);
            if (!userInfo.isPrimary()) {
                if (userInfo == null || Utils.isProfileOf(primaryUser, userInfo)) {
                    arrayList.add(new UserProfileController(context, userInfo, 6));
                } else {
                    arrayList.add(new SecondaryUserController(context, userInfo));
                    z = true;
                }
            }
        }
        if (!z) {
            arrayList.add(new NoSecondaryUserController(context));
        }
        return arrayList;
    }

    SecondaryUserController(Context context, UserInfo userInfo) {
        super(context);
        this.mUser = userInfo;
        this.mSize = -1L;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        if (this.mStoragePreference == null) {
            this.mStoragePreference = new StorageItemPreference(preferenceScreen.getContext());
            PreferenceGroup preferenceGroup = (PreferenceGroup) preferenceScreen.findPreference("pref_secondary_users");
            this.mStoragePreference.setTitle(this.mUser.name);
            this.mStoragePreference.setKey("pref_user_" + this.mUser.id);
            if (this.mSize != -1) {
                this.mStoragePreference.setStorageSize(this.mSize, this.mTotalSizeBytes);
            }
            preferenceGroup.setVisible(true);
            preferenceGroup.addPreference(this.mStoragePreference);
            maybeSetIcon();
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        if (this.mStoragePreference != null) {
            return this.mStoragePreference.getKey();
        }
        return null;
    }

    public UserInfo getUser() {
        return this.mUser;
    }

    public void setSize(long j) {
        this.mSize = j;
        if (this.mStoragePreference != null) {
            this.mStoragePreference.setStorageSize(this.mSize, this.mTotalSizeBytes);
        }
    }

    public void setTotalSize(long j) {
        this.mTotalSizeBytes = j;
    }

    @Override // com.android.settings.deviceinfo.storage.StorageAsyncLoader.ResultHandler
    public void handleResult(SparseArray<StorageAsyncLoader.AppsStorageResult> sparseArray) {
        StorageAsyncLoader.AppsStorageResult appsStorageResult = sparseArray.get(getUser().id);
        if (appsStorageResult != null) {
            setSize(appsStorageResult.externalStats.totalBytes);
        }
    }

    @Override // com.android.settings.deviceinfo.storage.UserIconLoader.UserIconHandler
    public void handleUserIcons(SparseArray<Drawable> sparseArray) {
        this.mUserIcon = sparseArray.get(this.mUser.id);
        maybeSetIcon();
    }

    private void maybeSetIcon() {
        if (this.mUserIcon != null && this.mStoragePreference != null) {
            this.mStoragePreference.setIcon(this.mUserIcon);
        }
    }

    private static class NoSecondaryUserController extends AbstractPreferenceController implements PreferenceControllerMixin {
        public NoSecondaryUserController(Context context) {
            super(context);
        }

        @Override // com.android.settingslib.core.AbstractPreferenceController
        public void displayPreference(PreferenceScreen preferenceScreen) {
            PreferenceGroup preferenceGroup = (PreferenceGroup) preferenceScreen.findPreference("pref_secondary_users");
            if (preferenceGroup == null) {
                return;
            }
            preferenceScreen.removePreference(preferenceGroup);
        }

        @Override // com.android.settingslib.core.AbstractPreferenceController
        public boolean isAvailable() {
            return true;
        }

        @Override // com.android.settingslib.core.AbstractPreferenceController
        public String getPreferenceKey() {
            return null;
        }
    }
}
