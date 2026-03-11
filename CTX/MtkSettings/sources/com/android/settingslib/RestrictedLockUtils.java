package com.android.settingslib;

import android.app.AppGlobals;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class RestrictedLockUtils {
    static Proxy sProxy = new Proxy();

    interface LockSettingCheck {
        boolean isEnforcing(DevicePolicyManager devicePolicyManager, ComponentName componentName, int i);
    }

    public static Drawable getRestrictedPadlock(Context context) {
        Drawable drawable = context.getDrawable(R.drawable.ic_info);
        int dimensionPixelSize = context.getResources().getDimensionPixelSize(R.dimen.restricted_icon_size);
        drawable.setBounds(0, 0, dimensionPixelSize, dimensionPixelSize);
        return drawable;
    }

    public static EnforcedAdmin checkIfRestrictionEnforced(Context context, String str, int i) {
        if (((DevicePolicyManager) context.getSystemService("device_policy")) == null) {
            return null;
        }
        UserManager userManager = UserManager.get(context);
        List userRestrictionSources = userManager.getUserRestrictionSources(str, UserHandle.of(i));
        if (userRestrictionSources.isEmpty()) {
            return null;
        }
        if (userRestrictionSources.size() > 1) {
            return EnforcedAdmin.createDefaultEnforcedAdminWithRestriction(str);
        }
        int userRestrictionSource = ((UserManager.EnforcingUser) userRestrictionSources.get(0)).getUserRestrictionSource();
        int identifier = ((UserManager.EnforcingUser) userRestrictionSources.get(0)).getUserHandle().getIdentifier();
        if (userRestrictionSource == 4) {
            if (identifier == i) {
                return getProfileOwner(context, str, identifier);
            }
            UserInfo profileParent = userManager.getProfileParent(identifier);
            if (profileParent != null && profileParent.id == i) {
                return getProfileOwner(context, str, identifier);
            }
            return EnforcedAdmin.createDefaultEnforcedAdminWithRestriction(str);
        }
        if (userRestrictionSource != 2) {
            return null;
        }
        if (identifier == i) {
            return getDeviceOwner(context, str);
        }
        return EnforcedAdmin.createDefaultEnforcedAdminWithRestriction(str);
    }

    public static boolean hasBaseUserRestriction(Context context, String str, int i) {
        return ((UserManager) context.getSystemService("user")).hasBaseUserRestriction(str, UserHandle.of(i));
    }

    public static EnforcedAdmin checkIfKeyguardFeaturesDisabled(Context context, final int i, final int i2) {
        LockSettingCheck lockSettingCheck = new LockSettingCheck() {
            @Override
            public final boolean isEnforcing(DevicePolicyManager devicePolicyManager, ComponentName componentName, int i3) {
                return RestrictedLockUtils.lambda$checkIfKeyguardFeaturesDisabled$0(i2, i, devicePolicyManager, componentName, i3);
            }
        };
        if (UserManager.get(context).getUserInfo(i2).isManagedProfile()) {
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
            return findEnforcedAdmin(devicePolicyManager.getActiveAdminsAsUser(i2), devicePolicyManager, i2, lockSettingCheck);
        }
        return checkForLockSetting(context, i2, lockSettingCheck);
    }

    static boolean lambda$checkIfKeyguardFeaturesDisabled$0(int i, int i2, DevicePolicyManager devicePolicyManager, ComponentName componentName, int i3) {
        int keyguardDisabledFeatures = devicePolicyManager.getKeyguardDisabledFeatures(componentName, i3);
        if (i3 != i) {
            keyguardDisabledFeatures &= 432;
        }
        return (keyguardDisabledFeatures & i2) != 0;
    }

    private static EnforcedAdmin findEnforcedAdmin(List<ComponentName> list, DevicePolicyManager devicePolicyManager, int i, LockSettingCheck lockSettingCheck) {
        EnforcedAdmin enforcedAdmin = null;
        if (list == null) {
            return null;
        }
        for (ComponentName componentName : list) {
            if (lockSettingCheck.isEnforcing(devicePolicyManager, componentName, i)) {
                if (enforcedAdmin == null) {
                    enforcedAdmin = new EnforcedAdmin(componentName, i);
                } else {
                    return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
                }
            }
        }
        return enforcedAdmin;
    }

    public static EnforcedAdmin checkIfUninstallBlocked(Context context, String str, int i) {
        EnforcedAdmin enforcedAdminCheckIfRestrictionEnforced = checkIfRestrictionEnforced(context, "no_control_apps", i);
        if (enforcedAdminCheckIfRestrictionEnforced != null) {
            return enforcedAdminCheckIfRestrictionEnforced;
        }
        EnforcedAdmin enforcedAdminCheckIfRestrictionEnforced2 = checkIfRestrictionEnforced(context, "no_uninstall_apps", i);
        if (enforcedAdminCheckIfRestrictionEnforced2 != null) {
            return enforcedAdminCheckIfRestrictionEnforced2;
        }
        try {
            if (AppGlobals.getPackageManager().getBlockUninstallForUser(str, i)) {
                return getProfileOrDeviceOwner(context, i);
            }
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    public static EnforcedAdmin checkIfApplicationIsSuspended(Context context, String str, int i) {
        try {
            if (AppGlobals.getPackageManager().isPackageSuspendedForUser(str, i)) {
                return getProfileOrDeviceOwner(context, i);
            }
            return null;
        } catch (RemoteException | IllegalArgumentException e) {
            return null;
        }
    }

    public static EnforcedAdmin checkIfInputMethodDisallowed(Context context, String str, int i) {
        boolean zIsInputMethodPermittedByAdmin;
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        if (devicePolicyManager == null) {
            return null;
        }
        EnforcedAdmin profileOrDeviceOwner = getProfileOrDeviceOwner(context, i);
        boolean zIsInputMethodPermittedByAdmin2 = true;
        if (profileOrDeviceOwner != null) {
            zIsInputMethodPermittedByAdmin = devicePolicyManager.isInputMethodPermittedByAdmin(profileOrDeviceOwner.component, str, i);
        } else {
            zIsInputMethodPermittedByAdmin = true;
        }
        int managedProfileId = getManagedProfileId(context, i);
        EnforcedAdmin profileOrDeviceOwner2 = getProfileOrDeviceOwner(context, managedProfileId);
        if (profileOrDeviceOwner2 != null) {
            zIsInputMethodPermittedByAdmin2 = devicePolicyManager.isInputMethodPermittedByAdmin(profileOrDeviceOwner2.component, str, managedProfileId);
        }
        if (!zIsInputMethodPermittedByAdmin && !zIsInputMethodPermittedByAdmin2) {
            return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
        }
        if (!zIsInputMethodPermittedByAdmin) {
            return profileOrDeviceOwner;
        }
        if (zIsInputMethodPermittedByAdmin2) {
            return null;
        }
        return profileOrDeviceOwner2;
    }

    public static EnforcedAdmin checkIfRemoteContactSearchDisallowed(Context context, int i) {
        EnforcedAdmin profileOwner;
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        if (devicePolicyManager == null || (profileOwner = getProfileOwner(context, i)) == null) {
            return null;
        }
        UserHandle userHandleOf = UserHandle.of(i);
        if (!devicePolicyManager.getCrossProfileContactsSearchDisabled(userHandleOf) || !devicePolicyManager.getCrossProfileCallerIdDisabled(userHandleOf)) {
            return null;
        }
        return profileOwner;
    }

    public static EnforcedAdmin checkIfAccessibilityServiceDisallowed(Context context, String str, int i) {
        boolean zIsAccessibilityServicePermittedByAdmin;
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        if (devicePolicyManager == null) {
            return null;
        }
        EnforcedAdmin profileOrDeviceOwner = getProfileOrDeviceOwner(context, i);
        boolean zIsAccessibilityServicePermittedByAdmin2 = true;
        if (profileOrDeviceOwner != null) {
            zIsAccessibilityServicePermittedByAdmin = devicePolicyManager.isAccessibilityServicePermittedByAdmin(profileOrDeviceOwner.component, str, i);
        } else {
            zIsAccessibilityServicePermittedByAdmin = true;
        }
        int managedProfileId = getManagedProfileId(context, i);
        EnforcedAdmin profileOrDeviceOwner2 = getProfileOrDeviceOwner(context, managedProfileId);
        if (profileOrDeviceOwner2 != null) {
            zIsAccessibilityServicePermittedByAdmin2 = devicePolicyManager.isAccessibilityServicePermittedByAdmin(profileOrDeviceOwner2.component, str, managedProfileId);
        }
        if (!zIsAccessibilityServicePermittedByAdmin && !zIsAccessibilityServicePermittedByAdmin2) {
            return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
        }
        if (!zIsAccessibilityServicePermittedByAdmin) {
            return profileOrDeviceOwner;
        }
        if (zIsAccessibilityServicePermittedByAdmin2) {
            return null;
        }
        return profileOrDeviceOwner2;
    }

    private static int getManagedProfileId(Context context, int i) {
        for (UserInfo userInfo : ((UserManager) context.getSystemService("user")).getProfiles(i)) {
            if (userInfo.id != i && userInfo.isManagedProfile()) {
                return userInfo.id;
            }
        }
        return -10000;
    }

    public static EnforcedAdmin checkIfAccountManagementDisabled(Context context, String str, int i) {
        if (str == null) {
            return null;
        }
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        if (!context.getPackageManager().hasSystemFeature("android.software.device_admin") || devicePolicyManager == null) {
            return null;
        }
        String[] accountTypesWithManagementDisabledAsUser = devicePolicyManager.getAccountTypesWithManagementDisabledAsUser(i);
        int length = accountTypesWithManagementDisabledAsUser.length;
        boolean z = false;
        int i2 = 0;
        while (true) {
            if (i2 >= length) {
                break;
            }
            if (!str.equals(accountTypesWithManagementDisabledAsUser[i2])) {
                i2++;
            } else {
                z = true;
                break;
            }
        }
        if (!z) {
            return null;
        }
        return getProfileOrDeviceOwner(context, i);
    }

    public static EnforcedAdmin checkIfMeteredDataRestricted(Context context, String str, int i) {
        EnforcedAdmin profileOrDeviceOwner = getProfileOrDeviceOwner(context, i);
        if (profileOrDeviceOwner != null && ((DevicePolicyManager) context.getSystemService("device_policy")).isMeteredDataDisabledPackageForUser(profileOrDeviceOwner.component, str, i)) {
            return profileOrDeviceOwner;
        }
        return null;
    }

    public static EnforcedAdmin checkIfAutoTimeRequired(Context context) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        if (devicePolicyManager == null || !devicePolicyManager.getAutoTimeRequired()) {
            return null;
        }
        return new EnforcedAdmin(devicePolicyManager.getDeviceOwnerComponentOnCallingUser(), UserHandle.myUserId());
    }

    public static EnforcedAdmin checkIfPasswordQualityIsSet(Context context, int i) {
        $$Lambda$RestrictedLockUtils$ZGpdJGoya42TrXyPazgpDXw5os __lambda_restrictedlockutils_zgpdjgoya42trxypazgpdxw5os = new LockSettingCheck() {
            @Override
            public final boolean isEnforcing(DevicePolicyManager devicePolicyManager, ComponentName componentName, int i2) {
                return RestrictedLockUtils.lambda$checkIfPasswordQualityIsSet$1(devicePolicyManager, componentName, i2);
            }
        };
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        EnforcedAdmin enforcedAdmin = null;
        if (devicePolicyManager == null) {
            return null;
        }
        if (sProxy.isSeparateProfileChallengeEnabled(new LockPatternUtils(context), i)) {
            List<ComponentName> activeAdminsAsUser = devicePolicyManager.getActiveAdminsAsUser(i);
            if (activeAdminsAsUser == null) {
                return null;
            }
            for (ComponentName componentName : activeAdminsAsUser) {
                if (__lambda_restrictedlockutils_zgpdjgoya42trxypazgpdxw5os.isEnforcing(devicePolicyManager, componentName, i)) {
                    if (enforcedAdmin == null) {
                        enforcedAdmin = new EnforcedAdmin(componentName, i);
                    } else {
                        return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
                    }
                }
            }
            return enforcedAdmin;
        }
        return checkForLockSetting(context, i, __lambda_restrictedlockutils_zgpdjgoya42trxypazgpdxw5os);
    }

    static boolean lambda$checkIfPasswordQualityIsSet$1(DevicePolicyManager devicePolicyManager, ComponentName componentName, int i) {
        return devicePolicyManager.getPasswordQuality(componentName, i) > 0;
    }

    public static EnforcedAdmin checkIfMaximumTimeToLockIsSet(Context context) {
        return checkForLockSetting(context, UserHandle.myUserId(), new LockSettingCheck() {
            @Override
            public final boolean isEnforcing(DevicePolicyManager devicePolicyManager, ComponentName componentName, int i) {
                return RestrictedLockUtils.lambda$checkIfMaximumTimeToLockIsSet$2(devicePolicyManager, componentName, i);
            }
        });
    }

    static boolean lambda$checkIfMaximumTimeToLockIsSet$2(DevicePolicyManager devicePolicyManager, ComponentName componentName, int i) {
        return devicePolicyManager.getMaximumTimeToLock(componentName, i) > 0;
    }

    private static EnforcedAdmin checkForLockSetting(Context context, int i, LockSettingCheck lockSettingCheck) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        EnforcedAdmin enforcedAdmin = null;
        if (devicePolicyManager == null) {
            return null;
        }
        LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
        for (UserInfo userInfo : UserManager.get(context).getProfiles(i)) {
            List<ComponentName> activeAdminsAsUser = devicePolicyManager.getActiveAdminsAsUser(userInfo.id);
            if (activeAdminsAsUser != null) {
                boolean zIsSeparateProfileChallengeEnabled = sProxy.isSeparateProfileChallengeEnabled(lockPatternUtils, userInfo.id);
                for (ComponentName componentName : activeAdminsAsUser) {
                    if (!zIsSeparateProfileChallengeEnabled && lockSettingCheck.isEnforcing(devicePolicyManager, componentName, userInfo.id)) {
                        if (enforcedAdmin == null) {
                            enforcedAdmin = new EnforcedAdmin(componentName, userInfo.id);
                        } else {
                            return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
                        }
                    } else if (userInfo.isManagedProfile() && lockSettingCheck.isEnforcing(sProxy.getParentProfileInstance(devicePolicyManager, userInfo), componentName, userInfo.id)) {
                        if (enforcedAdmin == null) {
                            enforcedAdmin = new EnforcedAdmin(componentName, userInfo.id);
                        } else {
                            return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
                        }
                    }
                }
            }
        }
        return enforcedAdmin;
    }

    public static EnforcedAdmin getProfileOrDeviceOwner(Context context, int i) {
        return getProfileOrDeviceOwner(context, null, i);
    }

    public static EnforcedAdmin getProfileOrDeviceOwner(Context context, String str, int i) {
        DevicePolicyManager devicePolicyManager;
        ComponentName deviceOwnerComponentOnAnyUser;
        if (i == -10000 || (devicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy")) == null) {
            return null;
        }
        ComponentName profileOwnerAsUser = devicePolicyManager.getProfileOwnerAsUser(i);
        if (profileOwnerAsUser != null) {
            return new EnforcedAdmin(profileOwnerAsUser, str, i);
        }
        if (devicePolicyManager.getDeviceOwnerUserId() != i || (deviceOwnerComponentOnAnyUser = devicePolicyManager.getDeviceOwnerComponentOnAnyUser()) == null) {
            return null;
        }
        return new EnforcedAdmin(deviceOwnerComponentOnAnyUser, str, i);
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

    private static EnforcedAdmin getProfileOwner(Context context, int i) {
        return getProfileOwner(context, null, i);
    }

    private static EnforcedAdmin getProfileOwner(Context context, String str, int i) {
        DevicePolicyManager devicePolicyManager;
        ComponentName profileOwnerAsUser;
        if (i == -10000 || (devicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy")) == null || (profileOwnerAsUser = devicePolicyManager.getProfileOwnerAsUser(i)) == null) {
            return null;
        }
        return new EnforcedAdmin(profileOwnerAsUser, str, i);
    }

    public static void setMenuItemAsDisabledByAdmin(final Context context, MenuItem menuItem, final EnforcedAdmin enforcedAdmin) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(menuItem.getTitle());
        removeExistingRestrictedSpans(spannableStringBuilder);
        if (enforcedAdmin != null) {
            spannableStringBuilder.setSpan(new ForegroundColorSpan(context.getColor(R.color.disabled_text_color)), 0, spannableStringBuilder.length(), 33);
            spannableStringBuilder.append(" ", new RestrictedLockImageSpan(context), 33);
            menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem2) {
                    RestrictedLockUtils.sendShowAdminSupportDetailsIntent(context, enforcedAdmin);
                    return true;
                }
            });
        } else {
            menuItem.setOnMenuItemClickListener(null);
        }
        menuItem.setTitle(spannableStringBuilder);
    }

    private static void removeExistingRestrictedSpans(SpannableStringBuilder spannableStringBuilder) {
        int length = spannableStringBuilder.length();
        for (RestrictedLockImageSpan restrictedLockImageSpan : (RestrictedLockImageSpan[]) spannableStringBuilder.getSpans(length - 1, length, RestrictedLockImageSpan.class)) {
            int spanStart = spannableStringBuilder.getSpanStart(restrictedLockImageSpan);
            int spanEnd = spannableStringBuilder.getSpanEnd(restrictedLockImageSpan);
            spannableStringBuilder.removeSpan(restrictedLockImageSpan);
            spannableStringBuilder.delete(spanStart, spanEnd);
        }
        for (ForegroundColorSpan foregroundColorSpan : (ForegroundColorSpan[]) spannableStringBuilder.getSpans(0, length, ForegroundColorSpan.class)) {
            spannableStringBuilder.removeSpan(foregroundColorSpan);
        }
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

    public static boolean isAdminInCurrentUserOrProfile(Context context, ComponentName componentName) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        Iterator it = UserManager.get(context).getProfiles(UserHandle.myUserId()).iterator();
        while (it.hasNext()) {
            if (devicePolicyManager.isAdminActiveAsUser(componentName, ((UserInfo) it.next()).id)) {
                return true;
            }
        }
        return false;
    }

    public static void setTextViewAsDisabledByAdmin(Context context, TextView textView, boolean z) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(textView.getText());
        removeExistingRestrictedSpans(spannableStringBuilder);
        if (z) {
            spannableStringBuilder.setSpan(new ForegroundColorSpan(context.getColor(R.color.disabled_text_color)), 0, spannableStringBuilder.length(), 33);
            textView.setCompoundDrawables(null, null, getRestrictedPadlock(context), null);
            textView.setCompoundDrawablePadding(context.getResources().getDimensionPixelSize(R.dimen.restricted_icon_padding));
        } else {
            textView.setCompoundDrawables(null, null, null, null);
        }
        textView.setText(spannableStringBuilder);
    }

    public static class EnforcedAdmin {
        public static final EnforcedAdmin MULTIPLE_ENFORCED_ADMIN = new EnforcedAdmin();
        public ComponentName component;
        public String enforcedRestriction;
        public int userId;

        public static EnforcedAdmin createDefaultEnforcedAdminWithRestriction(String str) {
            EnforcedAdmin enforcedAdmin = new EnforcedAdmin();
            enforcedAdmin.enforcedRestriction = str;
            return enforcedAdmin;
        }

        public EnforcedAdmin(ComponentName componentName, int i) {
            this.component = null;
            this.enforcedRestriction = null;
            this.userId = -10000;
            this.component = componentName;
            this.userId = i;
        }

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

        public boolean isSeparateProfileChallengeEnabled(LockPatternUtils lockPatternUtils, int i) {
            return lockPatternUtils.isSeparateProfileChallengeEnabled(i);
        }

        public DevicePolicyManager getParentProfileInstance(DevicePolicyManager devicePolicyManager, UserInfo userInfo) {
            return devicePolicyManager.getParentProfileInstance(userInfo);
        }
    }
}
