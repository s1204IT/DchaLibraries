package android.os;

import android.accounts.AccountManager;
import android.annotation.SystemApi;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcelable;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import com.android.internal.R;
import com.android.internal.os.RoSystemProperties;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UserManager {
    private static final String ACTION_CREATE_USER = "android.os.action.CREATE_USER";

    @SystemApi
    public static final String ACTION_USER_RESTRICTIONS_CHANGED = "android.os.action.USER_RESTRICTIONS_CHANGED";
    public static final String ALLOW_PARENT_PROFILE_APP_LINKING = "allow_parent_profile_app_linking";
    public static final String DISALLOW_ADD_MANAGED_PROFILE = "no_add_managed_profile";
    public static final String DISALLOW_ADD_USER = "no_add_user";
    public static final String DISALLOW_ADJUST_VOLUME = "no_adjust_volume";
    public static final String DISALLOW_AIRPLANE_MODE = "no_airplane_mode";
    public static final String DISALLOW_AMBIENT_DISPLAY = "no_ambient_display";
    public static final String DISALLOW_APPS_CONTROL = "no_control_apps";
    public static final String DISALLOW_AUTOFILL = "no_autofill";
    public static final String DISALLOW_BLUETOOTH = "no_bluetooth";
    public static final String DISALLOW_BLUETOOTH_SHARING = "no_bluetooth_sharing";
    public static final String DISALLOW_CAMERA = "no_camera";
    public static final String DISALLOW_CONFIG_BLUETOOTH = "no_config_bluetooth";
    public static final String DISALLOW_CONFIG_BRIGHTNESS = "no_config_brightness";
    public static final String DISALLOW_CONFIG_CELL_BROADCASTS = "no_config_cell_broadcasts";
    public static final String DISALLOW_CONFIG_CREDENTIALS = "no_config_credentials";
    public static final String DISALLOW_CONFIG_DATE_TIME = "no_config_date_time";
    public static final String DISALLOW_CONFIG_LOCALE = "no_config_locale";
    public static final String DISALLOW_CONFIG_LOCATION = "no_config_location";
    public static final String DISALLOW_CONFIG_MOBILE_NETWORKS = "no_config_mobile_networks";
    public static final String DISALLOW_CONFIG_SCREEN_TIMEOUT = "no_config_screen_timeout";
    public static final String DISALLOW_CONFIG_TETHERING = "no_config_tethering";
    public static final String DISALLOW_CONFIG_VPN = "no_config_vpn";
    public static final String DISALLOW_CONFIG_WIFI = "no_config_wifi";
    public static final String DISALLOW_CREATE_WINDOWS = "no_create_windows";
    public static final String DISALLOW_CROSS_PROFILE_COPY_PASTE = "no_cross_profile_copy_paste";
    public static final String DISALLOW_DATA_ROAMING = "no_data_roaming";
    public static final String DISALLOW_DEBUGGING_FEATURES = "no_debugging_features";
    public static final String DISALLOW_FACTORY_RESET = "no_factory_reset";
    public static final String DISALLOW_FUN = "no_fun";
    public static final String DISALLOW_INSTALL_APPS = "no_install_apps";
    public static final String DISALLOW_INSTALL_UNKNOWN_SOURCES = "no_install_unknown_sources";
    public static final String DISALLOW_MODIFY_ACCOUNTS = "no_modify_accounts";
    public static final String DISALLOW_MOUNT_PHYSICAL_MEDIA = "no_physical_media";
    public static final String DISALLOW_NETWORK_RESET = "no_network_reset";

    @SystemApi
    @Deprecated
    public static final String DISALLOW_OEM_UNLOCK = "no_oem_unlock";
    public static final String DISALLOW_OUTGOING_BEAM = "no_outgoing_beam";
    public static final String DISALLOW_OUTGOING_CALLS = "no_outgoing_calls";
    public static final String DISALLOW_PRINTING = "no_printing";
    public static final String DISALLOW_RECORD_AUDIO = "no_record_audio";
    public static final String DISALLOW_REMOVE_MANAGED_PROFILE = "no_remove_managed_profile";
    public static final String DISALLOW_REMOVE_USER = "no_remove_user";

    @SystemApi
    public static final String DISALLOW_RUN_IN_BACKGROUND = "no_run_in_background";
    public static final String DISALLOW_SAFE_BOOT = "no_safe_boot";
    public static final String DISALLOW_SET_USER_ICON = "no_set_user_icon";
    public static final String DISALLOW_SET_WALLPAPER = "no_set_wallpaper";
    public static final String DISALLOW_SHARE_INTO_MANAGED_PROFILE = "no_sharing_into_profile";
    public static final String DISALLOW_SHARE_LOCATION = "no_share_location";
    public static final String DISALLOW_SMS = "no_sms";
    public static final String DISALLOW_SYSTEM_ERROR_DIALOGS = "no_system_error_dialogs";
    public static final String DISALLOW_UNIFIED_PASSWORD = "no_unified_password";
    public static final String DISALLOW_UNINSTALL_APPS = "no_uninstall_apps";
    public static final String DISALLOW_UNMUTE_DEVICE = "disallow_unmute_device";
    public static final String DISALLOW_UNMUTE_MICROPHONE = "no_unmute_microphone";
    public static final String DISALLOW_USB_FILE_TRANSFER = "no_usb_file_transfer";
    public static final String DISALLOW_USER_SWITCH = "no_user_switch";
    public static final String DISALLOW_WALLPAPER = "no_wallpaper";
    public static final String ENSURE_VERIFY_APPS = "ensure_verify_apps";
    public static final String EXTRA_USER_ACCOUNT_NAME = "android.os.extra.USER_ACCOUNT_NAME";
    public static final String EXTRA_USER_ACCOUNT_OPTIONS = "android.os.extra.USER_ACCOUNT_OPTIONS";
    public static final String EXTRA_USER_ACCOUNT_TYPE = "android.os.extra.USER_ACCOUNT_TYPE";
    public static final String EXTRA_USER_NAME = "android.os.extra.USER_NAME";
    public static final String KEY_RESTRICTIONS_PENDING = "restrictions_pending";
    public static final int PIN_VERIFICATION_FAILED_INCORRECT = -3;
    public static final int PIN_VERIFICATION_FAILED_NOT_SET = -2;
    public static final int PIN_VERIFICATION_SUCCESS = -1;

    @SystemApi
    public static final int RESTRICTION_NOT_SET = 0;

    @SystemApi
    public static final int RESTRICTION_SOURCE_DEVICE_OWNER = 2;

    @SystemApi
    public static final int RESTRICTION_SOURCE_PROFILE_OWNER = 4;

    @SystemApi
    public static final int RESTRICTION_SOURCE_SYSTEM = 1;
    private static final String TAG = "UserManager";
    public static final int USER_CREATION_FAILED_NOT_PERMITTED = 1;
    public static final int USER_CREATION_FAILED_NO_MORE_USERS = 2;
    public static final int USER_OPERATION_ERROR_CURRENT_USER = 4;
    public static final int USER_OPERATION_ERROR_LOW_STORAGE = 5;
    public static final int USER_OPERATION_ERROR_MANAGED_PROFILE = 2;
    public static final int USER_OPERATION_ERROR_MAX_RUNNING_USERS = 3;
    public static final int USER_OPERATION_ERROR_MAX_USERS = 6;
    public static final int USER_OPERATION_ERROR_UNKNOWN = 1;
    public static final int USER_OPERATION_SUCCESS = 0;
    private final Context mContext;
    private Boolean mIsManagedProfileCached;
    private final IUserManager mService;

    @Retention(RetentionPolicy.SOURCE)
    public @interface UserOperationResult {
    }

    @SystemApi
    @Retention(RetentionPolicy.SOURCE)
    public @interface UserRestrictionSource {
    }

    public static class UserOperationException extends RuntimeException {
        private final int mUserOperationResult;

        public UserOperationException(String str, int i) {
            super(str);
            this.mUserOperationResult = i;
        }

        public int getUserOperationResult() {
            return this.mUserOperationResult;
        }
    }

    public static UserManager get(Context context) {
        return (UserManager) context.getSystemService("user");
    }

    public UserManager(Context context, IUserManager iUserManager) {
        this.mService = iUserManager;
        this.mContext = context.getApplicationContext();
    }

    public static boolean supportsMultipleUsers() {
        return getMaxSupportedUsers() > 1 && SystemProperties.getBoolean("fw.show_multiuserui", Resources.getSystem().getBoolean(R.bool.config_enableMultiUserUI));
    }

    public static boolean isSplitSystemUser() {
        return RoSystemProperties.FW_SYSTEM_USER_SPLIT;
    }

    public static boolean isGuestUserEphemeral() {
        return Resources.getSystem().getBoolean(R.bool.config_guestUserEphemeral);
    }

    public boolean canSwitchUsers() {
        return ((!(Settings.Global.getInt(this.mContext.getContentResolver(), Settings.Global.ALLOW_USER_SWITCHING_WHEN_SYSTEM_USER_LOCKED, 0) != 0) && !isUserUnlocked(UserHandle.SYSTEM)) || (TelephonyManager.getDefault().getCallState() != 0) || hasUserRestriction(DISALLOW_USER_SWITCH)) ? false : true;
    }

    public int getUserHandle() {
        return UserHandle.myUserId();
    }

    public String getUserName() {
        UserInfo userInfo = getUserInfo(getUserHandle());
        return userInfo == null ? "" : userInfo.name;
    }

    public boolean isUserNameSet() {
        try {
            return this.mService.isUserNameSet(getUserHandle());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isUserAGoat() {
        return this.mContext.getPackageManager().isPackageAvailable("com.coffeestainstudios.goatsimulator");
    }

    public boolean isPrimaryUser() {
        UserInfo userInfo = getUserInfo(UserHandle.myUserId());
        return userInfo != null && userInfo.isPrimary();
    }

    public boolean isSystemUser() {
        return UserHandle.myUserId() == 0;
    }

    public boolean isAdminUser() {
        return isUserAdmin(UserHandle.myUserId());
    }

    public boolean isUserAdmin(int i) {
        UserInfo userInfo = getUserInfo(i);
        return userInfo != null && userInfo.isAdmin();
    }

    @Deprecated
    public boolean isLinkedUser() {
        return isRestrictedProfile();
    }

    @SystemApi
    public boolean isRestrictedProfile() {
        try {
            return this.mService.isRestricted();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean canHaveRestrictedProfile(int i) {
        try {
            return this.mService.canHaveRestrictedProfile(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public boolean hasRestrictedProfiles() {
        try {
            return this.mService.hasRestrictedProfiles();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isGuestUser(int i) {
        UserInfo userInfo = getUserInfo(i);
        return userInfo != null && userInfo.isGuest();
    }

    public boolean isGuestUser() {
        UserInfo userInfo = getUserInfo(UserHandle.myUserId());
        return userInfo != null && userInfo.isGuest();
    }

    public boolean isDemoUser() {
        try {
            return this.mService.isDemoUser(UserHandle.myUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public boolean isManagedProfile() {
        if (this.mIsManagedProfileCached != null) {
            return this.mIsManagedProfileCached.booleanValue();
        }
        try {
            this.mIsManagedProfileCached = Boolean.valueOf(this.mService.isManagedProfile(UserHandle.myUserId()));
            return this.mIsManagedProfileCached.booleanValue();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public boolean isManagedProfile(int i) {
        if (i == UserHandle.myUserId()) {
            return isManagedProfile();
        }
        try {
            return this.mService.isManagedProfile(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getManagedProfileBadge(int i) {
        try {
            return this.mService.getManagedProfileBadge(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isEphemeralUser() {
        return isUserEphemeral(UserHandle.myUserId());
    }

    public boolean isUserEphemeral(int i) {
        UserInfo userInfo = getUserInfo(i);
        return userInfo != null && userInfo.isEphemeral();
    }

    public boolean isUserRunning(UserHandle userHandle) {
        return isUserRunning(userHandle.getIdentifier());
    }

    public boolean isUserRunning(int i) {
        try {
            return this.mService.isUserRunning(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isUserRunningOrStopping(UserHandle userHandle) {
        try {
            return ActivityManager.getService().isUserRunning(userHandle.getIdentifier(), 1);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isUserUnlocked() {
        return isUserUnlocked(Process.myUserHandle());
    }

    public boolean isUserUnlocked(UserHandle userHandle) {
        return isUserUnlocked(userHandle.getIdentifier());
    }

    public boolean isUserUnlocked(int i) {
        try {
            return this.mService.isUserUnlocked(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isUserUnlockingOrUnlocked(UserHandle userHandle) {
        return isUserUnlockingOrUnlocked(userHandle.getIdentifier());
    }

    public boolean isUserUnlockingOrUnlocked(int i) {
        try {
            return this.mService.isUserUnlockingOrUnlocked(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public long getUserStartRealtime() {
        try {
            return this.mService.getUserStartRealtime();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public long getUserUnlockRealtime() {
        try {
            return this.mService.getUserUnlockRealtime();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public UserInfo getUserInfo(int i) {
        try {
            return this.mService.getUserInfo(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    @Deprecated
    public int getUserRestrictionSource(String str, UserHandle userHandle) {
        try {
            return this.mService.getUserRestrictionSource(str, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public List<EnforcingUser> getUserRestrictionSources(String str, UserHandle userHandle) {
        try {
            return this.mService.getUserRestrictionSources(str, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Bundle getUserRestrictions() {
        return getUserRestrictions(Process.myUserHandle());
    }

    public Bundle getUserRestrictions(UserHandle userHandle) {
        try {
            return this.mService.getUserRestrictions(userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean hasBaseUserRestriction(String str, UserHandle userHandle) {
        try {
            return this.mService.hasBaseUserRestriction(str, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void setUserRestrictions(Bundle bundle) {
        throw new UnsupportedOperationException("This method is no longer supported");
    }

    @Deprecated
    public void setUserRestrictions(Bundle bundle, UserHandle userHandle) {
        throw new UnsupportedOperationException("This method is no longer supported");
    }

    @Deprecated
    public void setUserRestriction(String str, boolean z) {
        setUserRestriction(str, z, Process.myUserHandle());
    }

    @Deprecated
    public void setUserRestriction(String str, boolean z, UserHandle userHandle) {
        try {
            this.mService.setUserRestriction(str, z, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean hasUserRestriction(String str) {
        return hasUserRestriction(str, Process.myUserHandle());
    }

    public boolean hasUserRestriction(String str, UserHandle userHandle) {
        try {
            return this.mService.hasUserRestriction(str, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean hasUserRestrictionOnAnyUser(String str) {
        try {
            return this.mService.hasUserRestrictionOnAnyUser(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public long getSerialNumberForUser(UserHandle userHandle) {
        return getUserSerialNumber(userHandle.getIdentifier());
    }

    public UserHandle getUserForSerialNumber(long j) {
        int userHandle = getUserHandle((int) j);
        if (userHandle >= 0) {
            return new UserHandle(userHandle);
        }
        return null;
    }

    public UserInfo createUser(String str, int i) {
        try {
            UserInfo userInfoCreateUser = this.mService.createUser(str, i);
            if (userInfoCreateUser != null && !userInfoCreateUser.isAdmin() && !userInfoCreateUser.isDemo()) {
                this.mService.setUserRestriction(DISALLOW_SMS, true, userInfoCreateUser.id);
                this.mService.setUserRestriction(DISALLOW_OUTGOING_CALLS, true, userInfoCreateUser.id);
            }
            return userInfoCreateUser;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public UserInfo createGuest(Context context, String str) {
        try {
            UserInfo userInfoCreateUser = this.mService.createUser(str, 4);
            if (userInfoCreateUser != null) {
                Settings.Secure.putStringForUser(context.getContentResolver(), Settings.Secure.SKIP_FIRST_USE_HINTS, WifiEnterpriseConfig.ENGINE_ENABLE, userInfoCreateUser.id);
            }
            return userInfoCreateUser;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public UserInfo createProfileForUser(String str, int i, int i2) {
        return createProfileForUser(str, i, i2, null);
    }

    public UserInfo createProfileForUser(String str, int i, int i2, String[] strArr) {
        try {
            return this.mService.createProfileForUser(str, i, i2, strArr);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public UserInfo createProfileForUserEvenWhenDisallowed(String str, int i, int i2, String[] strArr) {
        try {
            return this.mService.createProfileForUserEvenWhenDisallowed(str, i, i2, strArr);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public UserInfo createRestrictedProfile(String str) {
        try {
            UserHandle userHandleMyUserHandle = Process.myUserHandle();
            UserInfo userInfoCreateRestrictedProfile = this.mService.createRestrictedProfile(str, userHandleMyUserHandle.getIdentifier());
            if (userInfoCreateRestrictedProfile != null) {
                AccountManager.get(this.mContext).addSharedAccountsFromParentUser(userHandleMyUserHandle, UserHandle.of(userInfoCreateRestrictedProfile.id));
            }
            return userInfoCreateRestrictedProfile;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static Intent createUserCreationIntent(String str, String str2, String str3, PersistableBundle persistableBundle) {
        Intent intent = new Intent(ACTION_CREATE_USER);
        if (str != null) {
            intent.putExtra(EXTRA_USER_NAME, str);
        }
        if (str2 != null && str3 == null) {
            throw new IllegalArgumentException("accountType must be specified if accountName is specified");
        }
        if (str2 != null) {
            intent.putExtra(EXTRA_USER_ACCOUNT_NAME, str2);
        }
        if (str3 != null) {
            intent.putExtra(EXTRA_USER_ACCOUNT_TYPE, str3);
        }
        if (persistableBundle != null) {
            intent.putExtra(EXTRA_USER_ACCOUNT_OPTIONS, persistableBundle);
        }
        return intent;
    }

    @SystemApi
    public String getSeedAccountName() {
        try {
            return this.mService.getSeedAccountName();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public String getSeedAccountType() {
        try {
            return this.mService.getSeedAccountType();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public PersistableBundle getSeedAccountOptions() {
        try {
            return this.mService.getSeedAccountOptions();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setSeedAccountData(int i, String str, String str2, PersistableBundle persistableBundle) {
        try {
            this.mService.setSeedAccountData(i, str, str2, persistableBundle, true);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void clearSeedAccountData() {
        try {
            this.mService.clearSeedAccountData();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean markGuestForDeletion(int i) {
        try {
            return this.mService.markGuestForDeletion(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setUserEnabled(int i) {
        try {
            this.mService.setUserEnabled(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setUserAdmin(int i) {
        try {
            this.mService.setUserAdmin(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void evictCredentialEncryptionKey(int i) {
        try {
            this.mService.evictCredentialEncryptionKey(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getUserCount() {
        List<UserInfo> users = getUsers();
        if (users != null) {
            return users.size();
        }
        return 1;
    }

    public List<UserInfo> getUsers() {
        try {
            return this.mService.getUsers(false);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public long[] getSerialNumbersOfUsers(boolean z) {
        try {
            long[] jArr = new long[this.mService.getUsers(z).size()];
            for (int i = 0; i < jArr.length; i++) {
                jArr[i] = r5.get(i).serialNumber;
            }
            return jArr;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getUserAccount(int i) {
        try {
            return this.mService.getUserAccount(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setUserAccount(int i, String str) {
        try {
            this.mService.setUserAccount(i, str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public UserInfo getPrimaryUser() {
        try {
            return this.mService.getPrimaryUser();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean canAddMoreUsers() {
        List<UserInfo> users = getUsers(true);
        int size = users.size();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            if (!users.get(i2).isGuest()) {
                i++;
            }
        }
        if (i < getMaxSupportedUsers()) {
            return true;
        }
        return false;
    }

    public boolean canAddMoreManagedProfiles(int i, boolean z) {
        try {
            return this.mService.canAddMoreManagedProfiles(i, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<UserInfo> getProfiles(int i) {
        try {
            return this.mService.getProfiles(i, false);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isSameProfileGroup(int i, int i2) {
        try {
            return this.mService.isSameProfileGroup(i, i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<UserInfo> getEnabledProfiles(int i) {
        try {
            return this.mService.getProfiles(i, true);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<UserHandle> getUserProfiles() {
        int[] profileIds = getProfileIds(UserHandle.myUserId(), true);
        ArrayList arrayList = new ArrayList(profileIds.length);
        for (int i : profileIds) {
            arrayList.add(UserHandle.of(i));
        }
        return arrayList;
    }

    public int[] getProfileIds(int i, boolean z) {
        try {
            return this.mService.getProfileIds(i, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int[] getProfileIdsWithDisabled(int i) {
        return getProfileIds(i, false);
    }

    public int[] getEnabledProfileIds(int i) {
        return getProfileIds(i, true);
    }

    public int getCredentialOwnerProfile(int i) {
        try {
            return this.mService.getCredentialOwnerProfile(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public UserInfo getProfileParent(int i) {
        try {
            return this.mService.getProfileParent(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean requestQuietModeEnabled(boolean z, UserHandle userHandle) {
        return requestQuietModeEnabled(z, userHandle, null);
    }

    public boolean requestQuietModeEnabled(boolean z, UserHandle userHandle, IntentSender intentSender) {
        try {
            return this.mService.requestQuietModeEnabled(this.mContext.getPackageName(), z, userHandle.getIdentifier(), intentSender);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isQuietModeEnabled(UserHandle userHandle) {
        try {
            return this.mService.isQuietModeEnabled(userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Drawable getBadgedIconForUser(Drawable drawable, UserHandle userHandle) {
        return this.mContext.getPackageManager().getUserBadgedIcon(drawable, userHandle);
    }

    public Drawable getBadgedDrawableForUser(Drawable drawable, UserHandle userHandle, Rect rect, int i) {
        return this.mContext.getPackageManager().getUserBadgedDrawableForDensity(drawable, userHandle, rect, i);
    }

    public CharSequence getBadgedLabelForUser(CharSequence charSequence, UserHandle userHandle) {
        return this.mContext.getPackageManager().getUserBadgedLabel(charSequence, userHandle);
    }

    public List<UserInfo> getUsers(boolean z) {
        try {
            return this.mService.getUsers(z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean removeUser(int i) {
        try {
            return this.mService.removeUser(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean removeUserEvenWhenDisallowed(int i) {
        try {
            return this.mService.removeUserEvenWhenDisallowed(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setUserName(int i, String str) {
        try {
            this.mService.setUserName(i, str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setUserIcon(int i, Bitmap bitmap) {
        try {
            this.mService.setUserIcon(i, bitmap);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Bitmap getUserIcon(int i) {
        try {
            ParcelFileDescriptor userIcon = this.mService.getUserIcon(i);
            if (userIcon != null) {
                try {
                    return BitmapFactory.decodeFileDescriptor(userIcon.getFileDescriptor());
                } finally {
                    try {
                        userIcon.close();
                    } catch (IOException e) {
                    }
                }
            }
            return null;
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    public static int getMaxSupportedUsers() {
        if (Build.ID.startsWith("JVP")) {
            return 1;
        }
        if (!ActivityManager.isLowRamDeviceStatic() || (Resources.getSystem().getConfiguration().uiMode & 15) == 4) {
            return SystemProperties.getInt("fw.max_users", Resources.getSystem().getInteger(R.integer.config_multiuserMaximumUsers));
        }
        return 1;
    }

    public boolean isUserSwitcherEnabled() {
        List<UserInfo> users;
        if (!supportsMultipleUsers() || hasUserRestriction(DISALLOW_USER_SWITCH) || isDeviceInDemoMode(this.mContext) || (users = getUsers(true)) == null) {
            return false;
        }
        Iterator<UserInfo> it = users.iterator();
        int i = 0;
        while (it.hasNext()) {
            if (it.next().supportsSwitchToByUser()) {
                i++;
            }
        }
        return i > 1 || (((DevicePolicyManager) this.mContext.getSystemService(DevicePolicyManager.class)).getGuestUserDisabled(null) ^ true);
    }

    public static boolean isDeviceInDemoMode(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.DEVICE_DEMO_MODE, 0) > 0;
    }

    public int getUserSerialNumber(int i) {
        try {
            return this.mService.getUserSerialNumber(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getUserHandle(int i) {
        try {
            return this.mService.getUserHandle(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Bundle getApplicationRestrictions(String str) {
        try {
            return this.mService.getApplicationRestrictions(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Bundle getApplicationRestrictions(String str, UserHandle userHandle) {
        try {
            return this.mService.getApplicationRestrictionsForUser(str, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setApplicationRestrictions(String str, Bundle bundle, UserHandle userHandle) {
        try {
            this.mService.setApplicationRestrictions(str, bundle, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public boolean setRestrictionsChallenge(String str) {
        return false;
    }

    public void setDefaultGuestRestrictions(Bundle bundle) {
        try {
            this.mService.setDefaultGuestRestrictions(bundle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Bundle getDefaultGuestRestrictions() {
        try {
            return this.mService.getDefaultGuestRestrictions();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public long getUserCreationTime(UserHandle userHandle) {
        try {
            return this.mService.getUserCreationTime(userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean someUserHasSeedAccount(String str, String str2) {
        try {
            return this.mService.someUserHasSeedAccount(str, str2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public static final class EnforcingUser implements Parcelable {
        public static final Parcelable.Creator<EnforcingUser> CREATOR = new Parcelable.Creator<EnforcingUser>() {
            @Override
            public EnforcingUser createFromParcel(Parcel parcel) {
                return new EnforcingUser(parcel);
            }

            @Override
            public EnforcingUser[] newArray(int i) {
                return new EnforcingUser[i];
            }
        };
        private final int userId;
        private final int userRestrictionSource;

        public EnforcingUser(int i, int i2) {
            this.userId = i;
            this.userRestrictionSource = i2;
        }

        private EnforcingUser(Parcel parcel) {
            this.userId = parcel.readInt();
            this.userRestrictionSource = parcel.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.userId);
            parcel.writeInt(this.userRestrictionSource);
        }

        public UserHandle getUserHandle() {
            return UserHandle.of(this.userId);
        }

        public int getUserRestrictionSource() {
            return this.userRestrictionSource;
        }
    }
}
