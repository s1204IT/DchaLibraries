package com.android.settings.location;

import android.content.Intent;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.Immutable;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
@Immutable
/* loaded from: classes.dex */
public class InjectedSetting {
    public final String className;
    public final int iconId;
    public final UserHandle mUserHandle;
    public final String packageName;
    public final String settingsActivity;
    public final String title;
    public final String userRestriction;

    private InjectedSetting(Builder builder) {
        this.packageName = builder.mPackageName;
        this.className = builder.mClassName;
        this.title = builder.mTitle;
        this.iconId = builder.mIconId;
        this.mUserHandle = builder.mUserHandle;
        this.settingsActivity = builder.mSettingsActivity;
        this.userRestriction = builder.mUserRestriction;
    }

    public String toString() {
        return "InjectedSetting{mPackageName='" + this.packageName + "', mClassName='" + this.className + "', label=" + this.title + ", iconId=" + this.iconId + ", userId=" + this.mUserHandle.getIdentifier() + ", settingsActivity='" + this.settingsActivity + "', userRestriction='" + this.userRestriction + '}';
    }

    public Intent getServiceIntent() {
        Intent intent = new Intent();
        intent.setClassName(this.packageName, this.className);
        return intent;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof InjectedSetting) {
            InjectedSetting injectedSetting = (InjectedSetting) obj;
            return Objects.equals(this.packageName, injectedSetting.packageName) && Objects.equals(this.className, injectedSetting.className) && Objects.equals(this.title, injectedSetting.title) && Objects.equals(Integer.valueOf(this.iconId), Integer.valueOf(injectedSetting.iconId)) && Objects.equals(this.mUserHandle, injectedSetting.mUserHandle) && Objects.equals(this.settingsActivity, injectedSetting.settingsActivity) && Objects.equals(this.userRestriction, injectedSetting.userRestriction);
        }
        return false;
    }

    public int hashCode() {
        return (31 * ((((((((((this.packageName.hashCode() * 31) + this.className.hashCode()) * 31) + this.title.hashCode()) * 31) + this.iconId) * 31) + (this.mUserHandle == null ? 0 : this.mUserHandle.hashCode())) * 31) + this.settingsActivity.hashCode())) + (this.userRestriction != null ? this.userRestriction.hashCode() : 0);
    }

    /* loaded from: classes.dex */
    public static class Builder {
        private String mClassName;
        private int mIconId;
        private String mPackageName;
        private String mSettingsActivity;
        private String mTitle;
        private UserHandle mUserHandle;
        private String mUserRestriction;

        public Builder setPackageName(String str) {
            this.mPackageName = str;
            return this;
        }

        public Builder setClassName(String str) {
            this.mClassName = str;
            return this;
        }

        public Builder setTitle(String str) {
            this.mTitle = str;
            return this;
        }

        public Builder setIconId(int i) {
            this.mIconId = i;
            return this;
        }

        public Builder setUserHandle(UserHandle userHandle) {
            this.mUserHandle = userHandle;
            return this;
        }

        public Builder setSettingsActivity(String str) {
            this.mSettingsActivity = str;
            return this;
        }

        public Builder setUserRestriction(String str) {
            this.mUserRestriction = str;
            return this;
        }

        public InjectedSetting build() {
            if (this.mPackageName == null || this.mClassName == null || TextUtils.isEmpty(this.mTitle) || TextUtils.isEmpty(this.mSettingsActivity)) {
                if (Log.isLoggable("SettingsInjector", 5)) {
                    Log.w("SettingsInjector", "Illegal setting specification: package=" + this.mPackageName + ", class=" + this.mClassName + ", title=" + this.mTitle + ", settingsActivity=" + this.mSettingsActivity);
                }
                return null;
            }
            return new InjectedSetting(this);
        }
    }
}
