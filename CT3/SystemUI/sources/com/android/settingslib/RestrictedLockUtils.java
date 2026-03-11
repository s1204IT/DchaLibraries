package com.android.settingslib;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.BenesseExtension;
import android.os.UserHandle;
import android.os.UserManager;

public class RestrictedLockUtils {
    public static EnforcedAdmin checkIfRestrictionEnforced(Context context, String userRestriction, int userId) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
        if (dpm == null) {
            return null;
        }
        UserManager um = UserManager.get(context);
        int restrictionSource = um.getUserRestrictionSource(userRestriction, UserHandle.of(userId));
        if (restrictionSource == 0 || restrictionSource == 1) {
            return null;
        }
        boolean enforcedByProfileOwner = (restrictionSource & 4) != 0;
        boolean enforcedByDeviceOwner = (restrictionSource & 2) != 0;
        if (enforcedByProfileOwner) {
            return getProfileOwner(context, userId);
        }
        if (!enforcedByDeviceOwner) {
            return null;
        }
        EnforcedAdmin deviceOwner = getDeviceOwner(context);
        return deviceOwner.userId == userId ? deviceOwner : EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
    }

    public static boolean hasBaseUserRestriction(Context context, String userRestriction, int userId) {
        UserManager um = (UserManager) context.getSystemService("user");
        return um.hasBaseUserRestriction(userRestriction, UserHandle.of(userId));
    }

    public static EnforcedAdmin getDeviceOwner(Context context) {
        ComponentName adminComponent;
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
        if (dpm == null || (adminComponent = dpm.getDeviceOwnerComponentOnAnyUser()) == null) {
            return null;
        }
        return new EnforcedAdmin(adminComponent, dpm.getDeviceOwnerUserId());
    }

    private static EnforcedAdmin getProfileOwner(Context context, int userId) {
        DevicePolicyManager dpm;
        ComponentName adminComponent;
        if (userId == -10000 || (dpm = (DevicePolicyManager) context.getSystemService("device_policy")) == null || (adminComponent = dpm.getProfileOwnerAsUser(userId)) == null) {
            return null;
        }
        return new EnforcedAdmin(adminComponent, userId);
    }

    public static void sendShowAdminSupportDetailsIntent(Context context, EnforcedAdmin admin) {
        Intent intent = getShowAdminSupportDetailsIntent(context, admin);
        if (intent == null) {
            return;
        }
        int targetUserId = UserHandle.myUserId();
        if (admin != null && admin.userId != -10000 && isCurrentUserOrProfile(context, admin.userId)) {
            targetUserId = admin.userId;
        }
        context.startActivityAsUser(intent, new UserHandle(targetUserId));
    }

    public static Intent getShowAdminSupportDetailsIntent(Context context, EnforcedAdmin admin) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent("android.settings.SHOW_ADMIN_SUPPORT_DETAILS");
        if (admin != null) {
            if (admin.component != null) {
                intent.putExtra("android.app.extra.DEVICE_ADMIN", admin.component);
            }
            int adminUserId = UserHandle.myUserId();
            if (admin.userId != -10000) {
                adminUserId = admin.userId;
            }
            intent.putExtra("android.intent.extra.USER_ID", adminUserId);
        }
        return intent;
    }

    public static boolean isCurrentUserOrProfile(Context context, int userId) {
        UserManager um = UserManager.get(context);
        for (UserInfo userInfo : um.getProfiles(UserHandle.myUserId())) {
            if (userInfo.id == userId) {
                return true;
            }
        }
        return false;
    }

    public static class EnforcedAdmin {
        public static final EnforcedAdmin MULTIPLE_ENFORCED_ADMIN = new EnforcedAdmin();
        public ComponentName component;
        public int userId;

        public EnforcedAdmin(ComponentName component, int userId) {
            this.component = null;
            this.userId = -10000;
            this.component = component;
            this.userId = userId;
        }

        public EnforcedAdmin(EnforcedAdmin other) {
            this.component = null;
            this.userId = -10000;
            if (other == null) {
                throw new IllegalArgumentException();
            }
            this.component = other.component;
            this.userId = other.userId;
        }

        public EnforcedAdmin() {
            this.component = null;
            this.userId = -10000;
        }

        public boolean equals(Object object) {
            if (object == this) {
                return true;
            }
            if (!(object instanceof EnforcedAdmin)) {
                return false;
            }
            EnforcedAdmin other = (EnforcedAdmin) object;
            if (this.userId != other.userId) {
                return false;
            }
            return (this.component == null && other.component == null) || (this.component != null && this.component.equals(other.component));
        }

        public String toString() {
            return "EnforcedAdmin{component=" + this.component + ",userId=" + this.userId + "}";
        }

        public void copyTo(EnforcedAdmin other) {
            if (other == null) {
                throw new IllegalArgumentException();
            }
            other.component = this.component;
            other.userId = this.userId;
        }
    }
}
