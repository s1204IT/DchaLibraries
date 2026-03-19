package com.android.settingslib;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import java.util.Iterator;
import java.util.Objects;

public class RestrictedLockUtils {
    static Proxy sProxy = new Proxy();

    public static boolean hasBaseUserRestriction(Context context, String str, int i) {
        return ((UserManager) context.getSystemService("user")).hasBaseUserRestriction(str, UserHandle.of(i));
    }

    public static EnforcedAdmin getDeviceOwner(Context context) {
        return getDeviceOwner(context, null);
    }

    private static EnforcedAdmin getDeviceOwner(Context context, String str) {
        ComponentName deviceOwnerComponentOnAnyUser;
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        if (devicePolicyManager == null || (deviceOwnerComponentOnAnyUser = devicePolicyManager.getDeviceOwnerComponentOnAnyUser()) == null) {
            return null;
        }
        return new EnforcedAdmin(deviceOwnerComponentOnAnyUser, str, devicePolicyManager.getDeviceOwnerUserId());
    }

    public static void sendShowAdminSupportDetailsIntent(Context context, EnforcedAdmin enforcedAdmin) {
        Intent showAdminSupportDetailsIntent = getShowAdminSupportDetailsIntent(context, enforcedAdmin);
        int iMyUserId = UserHandle.myUserId();
        if (enforcedAdmin != null && enforcedAdmin.userId != -10000 && isCurrentUserOrProfile(context, enforcedAdmin.userId)) {
            iMyUserId = enforcedAdmin.userId;
        }
        showAdminSupportDetailsIntent.putExtra("android.app.extra.RESTRICTION", enforcedAdmin.enforcedRestriction);
        context.startActivityAsUser(showAdminSupportDetailsIntent, new UserHandle(iMyUserId));
    }

    public static Intent getShowAdminSupportDetailsIntent(Context context, EnforcedAdmin enforcedAdmin) {
        Intent intent = new Intent("android.settings.SHOW_ADMIN_SUPPORT_DETAILS");
        if (enforcedAdmin != null) {
            if (enforcedAdmin.component != null) {
                intent.putExtra("android.app.extra.DEVICE_ADMIN", enforcedAdmin.component);
            }
            int iMyUserId = UserHandle.myUserId();
            if (enforcedAdmin.userId != -10000) {
                iMyUserId = enforcedAdmin.userId;
            }
            intent.putExtra("android.intent.extra.USER_ID", iMyUserId);
        }
        return intent;
    }

    public static boolean isCurrentUserOrProfile(Context context, int i) {
        Iterator it = UserManager.get(context).getProfiles(UserHandle.myUserId()).iterator();
        while (it.hasNext()) {
            if (((UserInfo) it.next()).id == i) {
                return true;
            }
        }
        return false;
    }

    public static class EnforcedAdmin {
        public static final EnforcedAdmin MULTIPLE_ENFORCED_ADMIN = new EnforcedAdmin();
        public ComponentName component;
        public String enforcedRestriction;
        public int userId;

        public EnforcedAdmin(ComponentName componentName, String str, int i) {
            this.component = null;
            this.enforcedRestriction = null;
            this.userId = -10000;
            this.component = componentName;
            this.enforcedRestriction = str;
            this.userId = i;
        }

        public EnforcedAdmin() {
            this.component = null;
            this.enforcedRestriction = null;
            this.userId = -10000;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            EnforcedAdmin enforcedAdmin = (EnforcedAdmin) obj;
            if (this.userId == enforcedAdmin.userId && Objects.equals(this.component, enforcedAdmin.component) && Objects.equals(this.enforcedRestriction, enforcedAdmin.enforcedRestriction)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(this.component, this.enforcedRestriction, Integer.valueOf(this.userId));
        }

        public String toString() {
            return "EnforcedAdmin{component=" + this.component + ", enforcedRestriction='" + this.enforcedRestriction + ", userId=" + this.userId + '}';
        }
    }

    static class Proxy {
        Proxy() {
        }
    }
}
