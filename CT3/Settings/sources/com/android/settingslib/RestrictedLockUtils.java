package com.android.settingslib;

import android.app.AppGlobals;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.BenesseExtension;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.view.MenuItem;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import java.util.List;

public class RestrictedLockUtils {
    public static Drawable getRestrictedPadlock(Context context) {
        Drawable restrictedPadlock = context.getDrawable(R$drawable.ic_info);
        int iconSize = context.getResources().getDimensionPixelSize(R$dimen.restricted_icon_size);
        restrictedPadlock.setBounds(0, 0, iconSize, iconSize);
        return restrictedPadlock;
    }

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

    public static EnforcedAdmin checkIfKeyguardFeaturesDisabled(Context context, int keyguardFeatures, int userId) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
        if (dpm == null) {
            return null;
        }
        UserManager um = (UserManager) context.getSystemService("user");
        LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
        EnforcedAdmin enforcedAdmin = null;
        if (um.getUserInfo(userId).isManagedProfile()) {
            List<ComponentName> admins = dpm.getActiveAdminsAsUser(userId);
            if (admins == null) {
                return null;
            }
            for (ComponentName admin : admins) {
                if ((dpm.getKeyguardDisabledFeatures(admin, userId) & keyguardFeatures) != 0) {
                    if (enforcedAdmin == null) {
                        enforcedAdmin = new EnforcedAdmin(admin, userId);
                    } else {
                        return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
                    }
                }
            }
        } else {
            for (UserInfo userInfo : um.getProfiles(userId)) {
                List<ComponentName> admins2 = dpm.getActiveAdminsAsUser(userInfo.id);
                if (admins2 != null) {
                    boolean isSeparateProfileChallengeEnabled = lockPatternUtils.isSeparateProfileChallengeEnabled(userInfo.id);
                    for (ComponentName admin2 : admins2) {
                        if (!isSeparateProfileChallengeEnabled && (dpm.getKeyguardDisabledFeatures(admin2, userInfo.id) & keyguardFeatures) != 0) {
                            if (enforcedAdmin == null) {
                                enforcedAdmin = new EnforcedAdmin(admin2, userInfo.id);
                            } else {
                                return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
                            }
                        } else if (userInfo.isManagedProfile()) {
                            DevicePolicyManager parentDpm = dpm.getParentProfileInstance(userInfo);
                            if ((parentDpm.getKeyguardDisabledFeatures(admin2, userInfo.id) & keyguardFeatures) == 0) {
                                continue;
                            } else if (enforcedAdmin == null) {
                                enforcedAdmin = new EnforcedAdmin(admin2, userInfo.id);
                            } else {
                                return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
                            }
                        } else {
                            continue;
                        }
                    }
                }
            }
        }
        return enforcedAdmin;
    }

    public static EnforcedAdmin checkIfUninstallBlocked(Context context, String packageName, int userId) {
        EnforcedAdmin allAppsControlDisallowedAdmin = checkIfRestrictionEnforced(context, "no_control_apps", userId);
        if (allAppsControlDisallowedAdmin != null) {
            return allAppsControlDisallowedAdmin;
        }
        EnforcedAdmin allAppsUninstallDisallowedAdmin = checkIfRestrictionEnforced(context, "no_uninstall_apps", userId);
        if (allAppsUninstallDisallowedAdmin != null) {
            return allAppsUninstallDisallowedAdmin;
        }
        IPackageManager ipm = AppGlobals.getPackageManager();
        try {
            if (ipm.getBlockUninstallForUser(packageName, userId)) {
                return getProfileOrDeviceOwner(context, userId);
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    public static EnforcedAdmin checkIfApplicationIsSuspended(Context context, String packageName, int userId) {
        IPackageManager ipm = AppGlobals.getPackageManager();
        try {
            if (ipm.isPackageSuspendedForUser(packageName, userId)) {
                return getProfileOrDeviceOwner(context, userId);
            }
            return null;
        } catch (RemoteException | IllegalArgumentException e) {
            return null;
        }
    }

    public static EnforcedAdmin checkIfInputMethodDisallowed(Context context, String packageName, int userId) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
        if (dpm == null) {
            return null;
        }
        EnforcedAdmin admin = getProfileOrDeviceOwner(context, userId);
        boolean permitted = true;
        if (admin != null) {
            permitted = dpm.isInputMethodPermittedByAdmin(admin.component, packageName, userId);
        }
        int managedProfileId = getManagedProfileId(context, userId);
        EnforcedAdmin profileAdmin = getProfileOrDeviceOwner(context, managedProfileId);
        boolean permittedByProfileAdmin = true;
        if (profileAdmin != null) {
            permittedByProfileAdmin = dpm.isInputMethodPermittedByAdmin(profileAdmin.component, packageName, managedProfileId);
        }
        if (!permitted && !permittedByProfileAdmin) {
            return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
        }
        if (!permitted) {
            return admin;
        }
        if (permittedByProfileAdmin) {
            return null;
        }
        return profileAdmin;
    }

    public static EnforcedAdmin checkIfRemoteContactSearchDisallowed(Context context, int userId) {
        EnforcedAdmin admin;
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
        if (dpm == null || (admin = getProfileOwner(context, userId)) == null) {
            return null;
        }
        UserHandle userHandle = UserHandle.of(userId);
        if (dpm.getCrossProfileContactsSearchDisabled(userHandle) && dpm.getCrossProfileCallerIdDisabled(userHandle)) {
            return admin;
        }
        return null;
    }

    public static EnforcedAdmin checkIfAccessibilityServiceDisallowed(Context context, String packageName, int userId) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
        if (dpm == null) {
            return null;
        }
        EnforcedAdmin admin = getProfileOrDeviceOwner(context, userId);
        boolean permitted = true;
        if (admin != null) {
            permitted = dpm.isAccessibilityServicePermittedByAdmin(admin.component, packageName, userId);
        }
        int managedProfileId = getManagedProfileId(context, userId);
        EnforcedAdmin profileAdmin = getProfileOrDeviceOwner(context, managedProfileId);
        boolean permittedByProfileAdmin = true;
        if (profileAdmin != null) {
            permittedByProfileAdmin = dpm.isAccessibilityServicePermittedByAdmin(profileAdmin.component, packageName, managedProfileId);
        }
        if (!permitted && !permittedByProfileAdmin) {
            return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
        }
        if (!permitted) {
            return admin;
        }
        if (permittedByProfileAdmin) {
            return null;
        }
        return profileAdmin;
    }

    private static int getManagedProfileId(Context context, int userId) {
        UserManager um = (UserManager) context.getSystemService("user");
        List<UserInfo> userProfiles = um.getProfiles(userId);
        for (UserInfo uInfo : userProfiles) {
            if (uInfo.id != userId && uInfo.isManagedProfile()) {
                return uInfo.id;
            }
        }
        return -10000;
    }

    public static EnforcedAdmin checkIfAccountManagementDisabled(Context context, String accountType, int userId) {
        DevicePolicyManager dpm;
        if (accountType == null || (dpm = (DevicePolicyManager) context.getSystemService("device_policy")) == null) {
            return null;
        }
        boolean isAccountTypeDisabled = false;
        String[] disabledTypes = dpm.getAccountTypesWithManagementDisabledAsUser(userId);
        int i = 0;
        int length = disabledTypes.length;
        while (true) {
            if (i >= length) {
                break;
            }
            String type = disabledTypes[i];
            if (!accountType.equals(type)) {
                i++;
            } else {
                isAccountTypeDisabled = true;
                break;
            }
        }
        if (isAccountTypeDisabled) {
            return getProfileOrDeviceOwner(context, userId);
        }
        return null;
    }

    public static EnforcedAdmin checkIfAutoTimeRequired(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
        if (dpm == null || !dpm.getAutoTimeRequired()) {
            return null;
        }
        ComponentName adminComponent = dpm.getDeviceOwnerComponentOnCallingUser();
        return new EnforcedAdmin(adminComponent, UserHandle.myUserId());
    }

    public static EnforcedAdmin checkIfPasswordQualityIsSet(Context context, int userId) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
        if (dpm == null) {
            return null;
        }
        LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
        EnforcedAdmin enforcedAdmin = null;
        if (lockPatternUtils.isSeparateProfileChallengeEnabled(userId)) {
            List<ComponentName> admins = dpm.getActiveAdminsAsUser(userId);
            if (admins == null) {
                return null;
            }
            for (ComponentName admin : admins) {
                if (dpm.getPasswordQuality(admin, userId) > 0) {
                    if (enforcedAdmin == null) {
                        enforcedAdmin = new EnforcedAdmin(admin, userId);
                    } else {
                        return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
                    }
                }
            }
        } else {
            UserManager um = (UserManager) context.getSystemService("user");
            for (UserInfo userInfo : um.getProfiles(userId)) {
                List<ComponentName> admins2 = dpm.getActiveAdminsAsUser(userInfo.id);
                if (admins2 != null) {
                    boolean isSeparateProfileChallengeEnabled = lockPatternUtils.isSeparateProfileChallengeEnabled(userInfo.id);
                    for (ComponentName admin2 : admins2) {
                        if (!isSeparateProfileChallengeEnabled && dpm.getPasswordQuality(admin2, userInfo.id) > 0) {
                            if (enforcedAdmin == null) {
                                enforcedAdmin = new EnforcedAdmin(admin2, userInfo.id);
                            } else {
                                return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
                            }
                        } else if (userInfo.isManagedProfile()) {
                            DevicePolicyManager parentDpm = dpm.getParentProfileInstance(userInfo);
                            if (parentDpm.getPasswordQuality(admin2, userInfo.id) <= 0) {
                                continue;
                            } else if (enforcedAdmin == null) {
                                enforcedAdmin = new EnforcedAdmin(admin2, userInfo.id);
                            } else {
                                return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
                            }
                        } else {
                            continue;
                        }
                    }
                }
            }
        }
        return enforcedAdmin;
    }

    public static EnforcedAdmin checkIfMaximumTimeToLockIsSet(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
        if (dpm == null) {
            return null;
        }
        new LockPatternUtils(context);
        EnforcedAdmin enforcedAdmin = null;
        int userId = UserHandle.myUserId();
        UserManager um = UserManager.get(context);
        List<UserInfo> profiles = um.getProfiles(userId);
        int profilesSize = profiles.size();
        for (int i = 0; i < profilesSize; i++) {
            UserInfo userInfo = profiles.get(i);
            List<ComponentName> admins = dpm.getActiveAdminsAsUser(userInfo.id);
            if (admins != null) {
                for (ComponentName admin : admins) {
                    if (dpm.getMaximumTimeToLock(admin, userInfo.id) > 0) {
                        if (enforcedAdmin == null) {
                            enforcedAdmin = new EnforcedAdmin(admin, userInfo.id);
                        } else {
                            return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
                        }
                    } else if (userInfo.isManagedProfile()) {
                        DevicePolicyManager parentDpm = dpm.getParentProfileInstance(userInfo);
                        if (parentDpm.getMaximumTimeToLock(admin, userInfo.id) <= 0) {
                            continue;
                        } else if (enforcedAdmin == null) {
                            enforcedAdmin = new EnforcedAdmin(admin, userInfo.id);
                        } else {
                            return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
                        }
                    } else {
                        continue;
                    }
                }
            }
        }
        return enforcedAdmin;
    }

    public static EnforcedAdmin getProfileOrDeviceOwner(Context context, int userId) {
        DevicePolicyManager dpm;
        ComponentName adminComponent;
        if (userId == -10000 || (dpm = (DevicePolicyManager) context.getSystemService("device_policy")) == null) {
            return null;
        }
        ComponentName adminComponent2 = dpm.getProfileOwnerAsUser(userId);
        if (adminComponent2 != null) {
            return new EnforcedAdmin(adminComponent2, userId);
        }
        if (dpm.getDeviceOwnerUserId() != userId || (adminComponent = dpm.getDeviceOwnerComponentOnAnyUser()) == null) {
            return null;
        }
        return new EnforcedAdmin(adminComponent, userId);
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

    public static void setMenuItemAsDisabledByAdmin(final Context context, MenuItem item, final EnforcedAdmin admin) {
        SpannableStringBuilder sb = new SpannableStringBuilder(item.getTitle());
        removeExistingRestrictedSpans(sb);
        if (admin != null) {
            int disabledColor = context.getColor(R$color.disabled_text_color);
            sb.setSpan(new ForegroundColorSpan(disabledColor), 0, sb.length(), 33);
            ImageSpan image = new RestrictedLockImageSpan(context);
            sb.append(" ", image, 33);
            item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item2) {
                    RestrictedLockUtils.sendShowAdminSupportDetailsIntent(context, admin);
                    return true;
                }
            });
        } else {
            item.setOnMenuItemClickListener(null);
        }
        item.setTitle(sb);
    }

    private static void removeExistingRestrictedSpans(SpannableStringBuilder sb) {
        int length = sb.length();
        ImageSpan[] imageSpans = (RestrictedLockImageSpan[]) sb.getSpans(length - 1, length, RestrictedLockImageSpan.class);
        for (ImageSpan span : imageSpans) {
            int start = sb.getSpanStart(span);
            int end = sb.getSpanEnd(span);
            sb.removeSpan(span);
            sb.delete(start, end);
        }
        ForegroundColorSpan[] colorSpans = (ForegroundColorSpan[]) sb.getSpans(0, length, ForegroundColorSpan.class);
        for (ForegroundColorSpan foregroundColorSpan : colorSpans) {
            sb.removeSpan(foregroundColorSpan);
        }
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

    public static boolean isAdminInCurrentUserOrProfile(Context context, ComponentName admin) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
        UserManager um = UserManager.get(context);
        for (UserInfo userInfo : um.getProfiles(UserHandle.myUserId())) {
            if (dpm.isAdminActiveAsUser(admin, userInfo.id)) {
                return true;
            }
        }
        return false;
    }

    public static void setTextViewAsDisabledByAdmin(Context context, TextView textView, boolean disabled) {
        SpannableStringBuilder sb = new SpannableStringBuilder(textView.getText());
        removeExistingRestrictedSpans(sb);
        if (disabled) {
            int disabledColor = context.getColor(R$color.disabled_text_color);
            sb.setSpan(new ForegroundColorSpan(disabledColor), 0, sb.length(), 33);
            textView.setCompoundDrawables(null, null, getRestrictedPadlock(context), null);
            textView.setCompoundDrawablePadding(context.getResources().getDimensionPixelSize(R$dimen.restricted_icon_padding));
        } else {
            textView.setCompoundDrawables(null, null, null, null);
        }
        textView.setText(sb);
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
    }
}
