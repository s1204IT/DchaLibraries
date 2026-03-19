package com.android.server.pm;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.IStopUserCallback;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.IUserManager;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.ShellCommand;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.security.GateKeeper;
import android.service.gatekeeper.IGateKeeperService;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.AtomicFile;
import android.util.IntArray;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IAppOpsService;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.pm.PackageManagerService;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import libcore.io.IoUtils;
import libcore.util.Objects;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class UserManagerService extends IUserManager.Stub {
    private static final int ALLOWED_FLAGS_FOR_CREATE_USERS_PERMISSION = 300;
    private static final String ATTR_CREATION_TIME = "created";
    private static final String ATTR_FLAGS = "flags";
    private static final String ATTR_GUEST_TO_REMOVE = "guestToRemove";
    private static final String ATTR_ICON_PATH = "icon";
    private static final String ATTR_ID = "id";
    private static final String ATTR_KEY = "key";
    private static final String ATTR_LAST_LOGGED_IN_FINGERPRINT = "lastLoggedInFingerprint";
    private static final String ATTR_LAST_LOGGED_IN_TIME = "lastLoggedIn";
    private static final String ATTR_MULTIPLE = "m";
    private static final String ATTR_NEXT_SERIAL_NO = "nextSerialNumber";
    private static final String ATTR_PARTIAL = "partial";
    private static final String ATTR_PROFILE_GROUP_ID = "profileGroupId";
    private static final String ATTR_RESTRICTED_PROFILE_PARENT_ID = "restrictedProfileParentId";
    private static final String ATTR_SEED_ACCOUNT_NAME = "seedAccountName";
    private static final String ATTR_SEED_ACCOUNT_TYPE = "seedAccountType";
    private static final String ATTR_SERIAL_NO = "serialNumber";
    private static final String ATTR_TYPE_BOOLEAN = "b";
    private static final String ATTR_TYPE_BUNDLE = "B";
    private static final String ATTR_TYPE_BUNDLE_ARRAY = "BA";
    private static final String ATTR_TYPE_INTEGER = "i";
    private static final String ATTR_TYPE_STRING = "s";
    private static final String ATTR_TYPE_STRING_ARRAY = "sa";
    private static final String ATTR_USER_VERSION = "version";
    private static final String ATTR_VALUE_TYPE = "type";
    static final boolean DBG = false;
    private static final boolean DBG_WITH_STACKTRACE = false;
    private static final long EPOCH_PLUS_30_YEARS = 946080000000L;
    private static final String LOG_TAG = "UserManagerService";
    private static final int MAX_MANAGED_PROFILES = 1;
    private static final int MAX_USER_ID = 21474;
    private static final int MIN_USER_ID = 10;
    private static final String RESTRICTIONS_FILE_PREFIX = "res_";
    private static final String TAG_ACCOUNT = "account";
    private static final String TAG_DEVICE_POLICY_RESTRICTIONS = "device_policy_restrictions";
    private static final String TAG_ENTRY = "entry";
    private static final String TAG_GLOBAL_RESTRICTION_OWNER_ID = "globalRestrictionOwnerUserId";
    private static final String TAG_GUEST_RESTRICTIONS = "guestRestrictions";
    private static final String TAG_NAME = "name";
    private static final String TAG_RESTRICTIONS = "restrictions";
    private static final String TAG_SEED_ACCOUNT_OPTIONS = "seedAccountOptions";
    private static final String TAG_USER = "user";
    private static final String TAG_USERS = "users";
    private static final String TAG_VALUE = "value";
    private static final String TRON_GUEST_CREATED = "users_guest_created";
    private static final String TRON_USER_CREATED = "users_user_created";
    private static final String USER_LIST_FILENAME = "userlist.xml";
    private static final String USER_PHOTO_FILENAME = "photo.png";
    private static final String USER_PHOTO_FILENAME_TMP = "photo.png.tmp";
    private static final int USER_VERSION = 6;
    static final int WRITE_USER_DELAY = 2000;
    static final int WRITE_USER_MSG = 1;
    private static final String XATTR_SERIAL = "user.serial";
    private static final String XML_SUFFIX = ".xml";
    private static UserManagerService sInstance;
    private final String ACTION_DISABLE_QUIET_MODE_AFTER_UNLOCK;
    private IAppOpsService mAppOpsService;

    @GuardedBy("mRestrictionsLock")
    private final SparseArray<Bundle> mAppliedUserRestrictions;

    @GuardedBy("mRestrictionsLock")
    private final SparseArray<Bundle> mBaseUserRestrictions;

    @GuardedBy("mRestrictionsLock")
    private final SparseArray<Bundle> mCachedEffectiveUserRestrictions;
    private final Context mContext;

    @GuardedBy("mRestrictionsLock")
    private Bundle mDevicePolicyGlobalUserRestrictions;

    @GuardedBy("mRestrictionsLock")
    private final SparseArray<Bundle> mDevicePolicyLocalUserRestrictions;
    private final BroadcastReceiver mDisableQuietModeCallback;

    @GuardedBy("mUsersLock")
    private boolean mForceEphemeralUsers;

    @GuardedBy("mRestrictionsLock")
    private int mGlobalRestrictionOwnerUserId;

    @GuardedBy("mGuestRestrictions")
    private final Bundle mGuestRestrictions;
    private final Handler mHandler;

    @GuardedBy("mUsersLock")
    private boolean mIsDeviceManaged;

    @GuardedBy("mUsersLock")
    private final SparseBooleanArray mIsUserManaged;
    private final LocalService mLocalService;
    private final LockPatternUtils mLockPatternUtils;

    @GuardedBy("mPackagesLock")
    private int mNextSerialNumber;
    private final Object mPackagesLock;
    private final PackageManagerService mPm;

    @GuardedBy("mUsersLock")
    private final SparseBooleanArray mRemovingUserIds;
    private final Object mRestrictionsLock;
    private int mSwitchedUserId;

    @GuardedBy("mUsersLock")
    private int[] mUserIds;
    private final File mUserListFile;

    @GuardedBy("mUserRestrictionsListeners")
    private final ArrayList<UserManagerInternal.UserRestrictionsListener> mUserRestrictionsListeners;

    @GuardedBy("mUserStates")
    private final SparseIntArray mUserStates;
    private int mUserVersion;

    @GuardedBy("mUsersLock")
    private final SparseArray<UserData> mUsers;
    private final File mUsersDir;
    private final Object mUsersLock;
    private static final String USER_INFO_DIR = "system" + File.separator + "users";
    private static final IBinder mUserRestriconToken = new Binder();

    private static class UserData {
        String account;
        UserInfo info;
        boolean persistSeedData;
        String seedAccountName;
        PersistableBundle seedAccountOptions;
        String seedAccountType;

        UserData(UserData userData) {
            this();
        }

        private UserData() {
        }

        void clearSeedAccountData() {
            this.seedAccountName = null;
            this.seedAccountType = null;
            this.seedAccountOptions = null;
            this.persistSeedData = false;
        }
    }

    public static UserManagerService getInstance() {
        UserManagerService userManagerService;
        synchronized (UserManagerService.class) {
            userManagerService = sInstance;
        }
        return userManagerService;
    }

    public static class LifeCycle extends SystemService {
        private UserManagerService mUms;

        public LifeCycle(Context context) {
            super(context);
        }

        @Override
        public void onStart() {
            this.mUms = UserManagerService.getInstance();
            publishBinderService(UserManagerService.TAG_USER, this.mUms);
        }

        @Override
        public void onBootPhase(int phase) {
            if (phase != 550) {
                return;
            }
            this.mUms.cleanupPartialUsers();
        }
    }

    UserManagerService(File dataDir) {
        this(null, null, new Object(), dataDir);
    }

    UserManagerService(Context context, PackageManagerService pm, Object packagesLock) {
        this(context, pm, packagesLock, Environment.getDataDirectory());
    }

    private UserManagerService(Context context, PackageManagerService pm, Object packagesLock, File dataDir) {
        this.mUsersLock = new Object();
        this.mRestrictionsLock = new Object();
        this.mUsers = new SparseArray<>();
        this.mBaseUserRestrictions = new SparseArray<>();
        this.mCachedEffectiveUserRestrictions = new SparseArray<>();
        this.mAppliedUserRestrictions = new SparseArray<>();
        this.mGlobalRestrictionOwnerUserId = -10000;
        this.mDevicePolicyLocalUserRestrictions = new SparseArray<>();
        this.mGuestRestrictions = new Bundle();
        this.mRemovingUserIds = new SparseBooleanArray();
        this.mUserVersion = 0;
        this.mSwitchedUserId = 0;
        this.mIsUserManaged = new SparseBooleanArray();
        this.mUserRestrictionsListeners = new ArrayList<>();
        this.ACTION_DISABLE_QUIET_MODE_AFTER_UNLOCK = "com.android.server.pm.DISABLE_QUIET_MODE_AFTER_UNLOCK";
        this.mDisableQuietModeCallback = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (!"com.android.server.pm.DISABLE_QUIET_MODE_AFTER_UNLOCK".equals(intent.getAction())) {
                    return;
                }
                IntentSender target = (IntentSender) intent.getParcelableExtra("android.intent.extra.INTENT");
                int userHandle = intent.getIntExtra("android.intent.extra.USER_ID", 0);
                UserManagerService.this.setQuietModeEnabled(userHandle, false);
                if (target == null) {
                    return;
                }
                try {
                    UserManagerService.this.mContext.startIntentSender(target, null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                }
            }
        };
        this.mUserStates = new SparseIntArray();
        this.mContext = context;
        this.mPm = pm;
        this.mPackagesLock = packagesLock;
        this.mHandler = new MainHandler();
        synchronized (this.mPackagesLock) {
            this.mUsersDir = new File(dataDir, USER_INFO_DIR);
            this.mUsersDir.mkdirs();
            File userZeroDir = new File(this.mUsersDir, String.valueOf(0));
            userZeroDir.mkdirs();
            FileUtils.setPermissions(this.mUsersDir.toString(), 509, -1, -1);
            this.mUserListFile = new File(this.mUsersDir, USER_LIST_FILENAME);
            initDefaultGuestRestrictions();
            readUserListLP();
            sInstance = this;
        }
        this.mLocalService = new LocalService(this, null);
        LocalServices.addService(UserManagerInternal.class, this.mLocalService);
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mUserStates.put(0, 0);
    }

    void systemReady() {
        this.mAppOpsService = IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
        synchronized (this.mRestrictionsLock) {
            applyUserRestrictionsLR(0);
        }
        UserInfo currentGuestUser = findCurrentGuestUser();
        if (currentGuestUser != null && !hasUserRestriction("no_config_wifi", currentGuestUser.id)) {
            setUserRestriction("no_config_wifi", true, currentGuestUser.id);
        }
        this.mContext.registerReceiver(this.mDisableQuietModeCallback, new IntentFilter("com.android.server.pm.DISABLE_QUIET_MODE_AFTER_UNLOCK"), null, this.mHandler);
    }

    void cleanupPartialUsers() {
        ArrayList<UserInfo> partials = new ArrayList<>();
        synchronized (this.mUsersLock) {
            int userSize = this.mUsers.size();
            for (int i = 0; i < userSize; i++) {
                UserInfo ui = this.mUsers.valueAt(i).info;
                if ((ui.partial || ui.guestToRemove || ui.isEphemeral()) && i != 0) {
                    partials.add(ui);
                }
            }
        }
        int partialsSize = partials.size();
        for (int i2 = 0; i2 < partialsSize; i2++) {
            UserInfo ui2 = partials.get(i2);
            Slog.w(LOG_TAG, "Removing partially created user " + ui2.id + " (name=" + ui2.name + ")");
            removeUserState(ui2.id);
        }
    }

    public String getUserAccount(int userId) {
        String str;
        checkManageUserAndAcrossUsersFullPermission("get user account");
        synchronized (this.mUsersLock) {
            str = this.mUsers.get(userId).account;
        }
        return str;
    }

    public void setUserAccount(int userId, String accountName) {
        checkManageUserAndAcrossUsersFullPermission("set user account");
        UserData userToUpdate = null;
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                UserData userData = this.mUsers.get(userId);
                if (userData == null) {
                    Slog.e(LOG_TAG, "User not found for setting user account: u" + userId);
                    return;
                }
                String currentAccount = userData.account;
                if (!Objects.equal(currentAccount, accountName)) {
                    userData.account = accountName;
                    userToUpdate = userData;
                }
                if (userToUpdate != null) {
                    writeUserLP(userToUpdate);
                }
            }
        }
    }

    public UserInfo getPrimaryUser() {
        checkManageUsersPermission("query users");
        synchronized (this.mUsersLock) {
            int userSize = this.mUsers.size();
            for (int i = 0; i < userSize; i++) {
                UserInfo ui = this.mUsers.valueAt(i).info;
                if (ui.isPrimary() && !this.mRemovingUserIds.get(ui.id)) {
                    return ui;
                }
            }
            return null;
        }
    }

    public List<UserInfo> getUsers(boolean excludeDying) {
        ArrayList<UserInfo> users;
        checkManageOrCreateUsersPermission("query users");
        synchronized (this.mUsersLock) {
            users = new ArrayList<>(this.mUsers.size());
            int userSize = this.mUsers.size();
            for (int i = 0; i < userSize; i++) {
                UserInfo ui = this.mUsers.valueAt(i).info;
                if (!ui.partial && (!excludeDying || !this.mRemovingUserIds.get(ui.id))) {
                    users.add(userWithName(ui));
                }
            }
        }
        return users;
    }

    public List<UserInfo> getProfiles(int userId, boolean enabledOnly) {
        List<UserInfo> profilesLU;
        boolean returnFullInfo = true;
        if (userId != UserHandle.getCallingUserId()) {
            checkManageOrCreateUsersPermission("getting profiles related to user " + userId);
        } else {
            returnFullInfo = hasManageUsersPermission();
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mUsersLock) {
                profilesLU = getProfilesLU(userId, enabledOnly, returnFullInfo);
            }
            return profilesLU;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int[] getProfileIds(int userId, boolean enabledOnly) {
        int[] array;
        if (userId != UserHandle.getCallingUserId()) {
            checkManageOrCreateUsersPermission("getting profiles related to user " + userId);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mUsersLock) {
                array = getProfileIdsLU(userId, enabledOnly).toArray();
            }
            return array;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private List<UserInfo> getProfilesLU(int userId, boolean enabledOnly, boolean fullInfo) {
        UserInfo userInfo;
        IntArray profileIds = getProfileIdsLU(userId, enabledOnly);
        ArrayList<UserInfo> users = new ArrayList<>(profileIds.size());
        for (int i = 0; i < profileIds.size(); i++) {
            int profileId = profileIds.get(i);
            UserInfo userInfo2 = this.mUsers.get(profileId).info;
            if (!fullInfo) {
                UserInfo userInfo3 = new UserInfo(userInfo2);
                userInfo3.name = null;
                userInfo3.iconPath = null;
                userInfo = userInfo3;
            } else {
                userInfo = userWithName(userInfo2);
            }
            users.add(userInfo);
        }
        return users;
    }

    private IntArray getProfileIdsLU(int userId, boolean enabledOnly) {
        UserInfo user = getUserInfoLU(userId);
        IntArray result = new IntArray(this.mUsers.size());
        if (user == null) {
            return result;
        }
        int userSize = this.mUsers.size();
        for (int i = 0; i < userSize; i++) {
            UserInfo profile = this.mUsers.valueAt(i).info;
            if (isProfileOf(user, profile) && ((!enabledOnly || profile.isEnabled()) && !this.mRemovingUserIds.get(profile.id) && !profile.partial)) {
                result.add(profile.id);
            }
        }
        return result;
    }

    public int getCredentialOwnerProfile(int userHandle) {
        checkManageUsersPermission("get the credential owner");
        if (!this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userHandle)) {
            synchronized (this.mUsersLock) {
                UserInfo profileParent = getProfileParentLU(userHandle);
                if (profileParent != null) {
                    return profileParent.id;
                }
            }
        }
        return userHandle;
    }

    public boolean isSameProfileGroup(int userId, int otherUserId) {
        boolean zIsSameProfileGroupLP;
        if (userId == otherUserId) {
            return true;
        }
        checkManageUsersPermission("check if in the same profile group");
        synchronized (this.mPackagesLock) {
            zIsSameProfileGroupLP = isSameProfileGroupLP(userId, otherUserId);
        }
        return zIsSameProfileGroupLP;
    }

    private boolean isSameProfileGroupLP(int userId, int otherUserId) {
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userId);
            if (userInfo == null || userInfo.profileGroupId == -10000) {
                return false;
            }
            UserInfo otherUserInfo = getUserInfoLU(otherUserId);
            if (otherUserInfo == null || otherUserInfo.profileGroupId == -10000) {
                return false;
            }
            return userInfo.profileGroupId == otherUserInfo.profileGroupId;
        }
    }

    public UserInfo getProfileParent(int userHandle) {
        UserInfo profileParentLU;
        checkManageUsersPermission("get the profile parent");
        synchronized (this.mUsersLock) {
            profileParentLU = getProfileParentLU(userHandle);
        }
        return profileParentLU;
    }

    private UserInfo getProfileParentLU(int userHandle) {
        int parentUserId;
        UserInfo profile = getUserInfoLU(userHandle);
        if (profile == null || (parentUserId = profile.profileGroupId) == -10000) {
            return null;
        }
        return getUserInfoLU(parentUserId);
    }

    private static boolean isProfileOf(UserInfo user, UserInfo profile) {
        if (user.id != profile.id) {
            return user.profileGroupId != -10000 && user.profileGroupId == profile.profileGroupId;
        }
        return true;
    }

    private void broadcastProfileAvailabilityChanges(UserHandle profileHandle, UserHandle parentHandle, boolean inQuietMode) {
        Intent intent = new Intent();
        if (inQuietMode) {
            intent.setAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
        } else {
            intent.setAction("android.intent.action.MANAGED_PROFILE_AVAILABLE");
        }
        intent.putExtra("android.intent.extra.QUIET_MODE", inQuietMode);
        intent.putExtra("android.intent.extra.USER", profileHandle);
        intent.putExtra("android.intent.extra.user_handle", profileHandle.getIdentifier());
        intent.addFlags(1073741824);
        this.mContext.sendBroadcastAsUser(intent, parentHandle);
    }

    public void setQuietModeEnabled(int userHandle, boolean enableQuietMode) {
        UserInfo profile;
        UserInfo parent;
        checkManageUsersPermission("silence profile");
        boolean changed = false;
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                profile = getUserInfoLU(userHandle);
                parent = getProfileParentLU(userHandle);
            }
            if (profile == null || !profile.isManagedProfile()) {
                throw new IllegalArgumentException("User " + userHandle + " is not a profile");
            }
            if (profile.isQuietModeEnabled() != enableQuietMode) {
                profile.flags ^= 128;
                writeUserLP(getUserDataLU(profile.id));
                changed = true;
            }
        }
        if (!changed) {
            return;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            if (enableQuietMode) {
                ActivityManagerNative.getDefault().stopUser(userHandle, true, (IStopUserCallback) null);
                ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).killForegroundAppsForUser(userHandle);
            } else {
                ActivityManagerNative.getDefault().startUserInBackground(userHandle);
            }
        } catch (RemoteException e) {
            Slog.e(LOG_TAG, "fail to start/stop user for quiet mode", e);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
        broadcastProfileAvailabilityChanges(profile.getUserHandle(), parent.getUserHandle(), enableQuietMode);
    }

    public boolean isQuietModeEnabled(int userHandle) {
        UserInfo info;
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                info = getUserInfoLU(userHandle);
            }
            if (info == null || !info.isManagedProfile()) {
                return false;
            }
            return info.isQuietModeEnabled();
        }
    }

    public boolean trySetQuietModeDisabled(int userHandle, IntentSender target) {
        checkManageUsersPermission("silence profile");
        if (StorageManager.isUserKeyUnlocked(userHandle) || !this.mLockPatternUtils.isSecure(userHandle)) {
            setQuietModeEnabled(userHandle, false);
            return true;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            KeyguardManager km = (KeyguardManager) this.mContext.getSystemService("keyguard");
            Intent unlockIntent = km.createConfirmDeviceCredentialIntent(null, null, userHandle);
            if (unlockIntent == null) {
                return false;
            }
            Intent callBackIntent = new Intent("com.android.server.pm.DISABLE_QUIET_MODE_AFTER_UNLOCK");
            if (target != null) {
                callBackIntent.putExtra("android.intent.extra.INTENT", target);
            }
            callBackIntent.putExtra("android.intent.extra.USER_ID", userHandle);
            callBackIntent.setPackage(this.mContext.getPackageName());
            callBackIntent.addFlags(268435456);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this.mContext, 0, callBackIntent, 1409286144);
            unlockIntent.putExtra("android.intent.extra.INTENT", pendingIntent.getIntentSender());
            unlockIntent.setFlags(276824064);
            this.mContext.startActivity(unlockIntent);
            return false;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void setUserEnabled(int userId) {
        UserInfo info;
        checkManageUsersPermission("enable user");
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                info = getUserInfoLU(userId);
            }
            if (info != null && !info.isEnabled()) {
                info.flags ^= 64;
                writeUserLP(getUserDataLU(info.id));
            }
        }
    }

    public UserInfo getUserInfo(int userId) {
        UserInfo userInfoUserWithName;
        checkManageOrCreateUsersPermission("query user");
        synchronized (this.mUsersLock) {
            userInfoUserWithName = userWithName(getUserInfoLU(userId));
        }
        return userInfoUserWithName;
    }

    private UserInfo userWithName(UserInfo orig) {
        if (orig != null && orig.name == null && orig.id == 0) {
            UserInfo withName = new UserInfo(orig);
            withName.name = getOwnerName();
            return withName;
        }
        return orig;
    }

    public boolean isManagedProfile(int userId) {
        boolean zIsManagedProfile;
        int callingUserId = UserHandle.getCallingUserId();
        if (callingUserId != userId && !hasManageUsersPermission()) {
            synchronized (this.mPackagesLock) {
                if (!isSameProfileGroupLP(callingUserId, userId)) {
                    throw new SecurityException("You need MANAGE_USERS permission to: check if specified user a managed profile outside your profile group");
                }
            }
        }
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userId);
            zIsManagedProfile = userInfo != null ? userInfo.isManagedProfile() : false;
        }
        return zIsManagedProfile;
    }

    public boolean isRestricted() {
        boolean zIsRestricted;
        synchronized (this.mUsersLock) {
            zIsRestricted = getUserInfoLU(UserHandle.getCallingUserId()).isRestricted();
        }
        return zIsRestricted;
    }

    public boolean canHaveRestrictedProfile(int userId) {
        boolean z = false;
        checkManageUsersPermission("canHaveRestrictedProfile");
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userId);
            if (userInfo == null || !userInfo.canHaveProfile()) {
                return false;
            }
            if (!userInfo.isAdmin()) {
                return false;
            }
            if (!this.mIsDeviceManaged) {
                if (!this.mIsUserManaged.get(userId)) {
                    z = true;
                }
            }
            return z;
        }
    }

    private UserInfo getUserInfoLU(int userId) {
        UserData userData = this.mUsers.get(userId);
        if (userData != null && userData.info.partial && !this.mRemovingUserIds.get(userId)) {
            Slog.w(LOG_TAG, "getUserInfo: unknown user #" + userId);
            return null;
        }
        if (userData != null) {
            return userData.info;
        }
        return null;
    }

    private UserData getUserDataLU(int userId) {
        UserData userData = this.mUsers.get(userId);
        if (userData == null || !userData.info.partial || this.mRemovingUserIds.get(userId)) {
            return userData;
        }
        return null;
    }

    private UserInfo getUserInfoNoChecks(int userId) {
        UserInfo userInfo;
        synchronized (this.mUsersLock) {
            UserData userData = this.mUsers.get(userId);
            userInfo = userData != null ? userData.info : null;
        }
        return userInfo;
    }

    private UserData getUserDataNoChecks(int userId) {
        UserData userData;
        synchronized (this.mUsersLock) {
            userData = this.mUsers.get(userId);
        }
        return userData;
    }

    public boolean exists(int userId) {
        return getUserInfoNoChecks(userId) != null;
    }

    public void setUserName(int userId, String name) {
        checkManageUsersPermission("rename users");
        boolean changed = false;
        synchronized (this.mPackagesLock) {
            UserData userData = getUserDataNoChecks(userId);
            if (userData == null || userData.info.partial) {
                Slog.w(LOG_TAG, "setUserName: unknown user #" + userId);
                return;
            }
            if (name != null && !name.equals(userData.info.name)) {
                userData.info.name = name;
                writeUserLP(userData);
                changed = true;
            }
            if (!changed) {
                return;
            }
            sendUserInfoChangedBroadcast(userId);
        }
    }

    public void setUserIcon(int userId, Bitmap bitmap) {
        checkManageUsersPermission("update users");
        if (hasUserRestriction("no_set_user_icon", userId)) {
            Log.w(LOG_TAG, "Cannot set user icon. DISALLOW_SET_USER_ICON is enabled.");
        } else {
            this.mLocalService.setUserIcon(userId, bitmap);
        }
    }

    private void sendUserInfoChangedBroadcast(int userId) {
        Intent changedIntent = new Intent("android.intent.action.USER_INFO_CHANGED");
        changedIntent.putExtra("android.intent.extra.user_handle", userId);
        changedIntent.addFlags(1073741824);
        this.mContext.sendBroadcastAsUser(changedIntent, UserHandle.ALL);
    }

    public ParcelFileDescriptor getUserIcon(int targetUserId) {
        synchronized (this.mPackagesLock) {
            UserInfo targetUserInfo = getUserInfoNoChecks(targetUserId);
            if (targetUserInfo == null || targetUserInfo.partial) {
                Slog.w(LOG_TAG, "getUserIcon: unknown user #" + targetUserId);
                return null;
            }
            int callingUserId = UserHandle.getCallingUserId();
            int callingGroupId = getUserInfoNoChecks(callingUserId).profileGroupId;
            int targetGroupId = targetUserInfo.profileGroupId;
            boolean sameGroup = callingGroupId != -10000 && callingGroupId == targetGroupId;
            if (callingUserId != targetUserId && !sameGroup) {
                checkManageUsersPermission("get the icon of a user who is not related");
            }
            if (targetUserInfo.iconPath == null) {
                return null;
            }
            String iconPath = targetUserInfo.iconPath;
            try {
                return ParcelFileDescriptor.open(new File(iconPath), 268435456);
            } catch (FileNotFoundException e) {
                Log.e(LOG_TAG, "Couldn't find icon file", e);
                return null;
            }
        }
    }

    public void makeInitialized(int userId) {
        checkManageUsersPermission("makeInitialized");
        boolean scheduleWriteUser = false;
        synchronized (this.mUsersLock) {
            UserData userData = this.mUsers.get(userId);
            if (userData == null || userData.info.partial) {
                Slog.w(LOG_TAG, "makeInitialized: unknown user #" + userId);
                return;
            }
            if ((userData.info.flags & 16) == 0) {
                userData.info.flags |= 16;
                scheduleWriteUser = true;
            }
            if (!scheduleWriteUser) {
                return;
            }
            scheduleWriteUser(userData);
        }
    }

    private void initDefaultGuestRestrictions() {
        synchronized (this.mGuestRestrictions) {
            if (this.mGuestRestrictions.isEmpty()) {
                this.mGuestRestrictions.putBoolean("no_config_wifi", true);
                this.mGuestRestrictions.putBoolean("no_install_unknown_sources", true);
                this.mGuestRestrictions.putBoolean("no_outgoing_calls", true);
                this.mGuestRestrictions.putBoolean("no_sms", true);
            }
        }
    }

    public Bundle getDefaultGuestRestrictions() {
        Bundle bundle;
        checkManageUsersPermission("getDefaultGuestRestrictions");
        synchronized (this.mGuestRestrictions) {
            bundle = new Bundle(this.mGuestRestrictions);
        }
        return bundle;
    }

    public void setDefaultGuestRestrictions(Bundle restrictions) {
        checkManageUsersPermission("setDefaultGuestRestrictions");
        synchronized (this.mGuestRestrictions) {
            this.mGuestRestrictions.clear();
            this.mGuestRestrictions.putAll(restrictions);
        }
        synchronized (this.mPackagesLock) {
            writeUserListLP();
        }
    }

    void setDevicePolicyUserRestrictionsInner(int userId, Bundle local, Bundle global) {
        boolean localChanged;
        Preconditions.checkNotNull(local);
        boolean globalChanged = false;
        synchronized (this.mRestrictionsLock) {
            if (global != null) {
                globalChanged = !UserRestrictionsUtils.areEqual(this.mDevicePolicyGlobalUserRestrictions, global);
                if (globalChanged) {
                    this.mDevicePolicyGlobalUserRestrictions = global;
                }
                this.mGlobalRestrictionOwnerUserId = userId;
            } else if (this.mGlobalRestrictionOwnerUserId == userId) {
                this.mGlobalRestrictionOwnerUserId = -10000;
            }
            Bundle prev = this.mDevicePolicyLocalUserRestrictions.get(userId);
            localChanged = !UserRestrictionsUtils.areEqual(prev, local);
            if (localChanged) {
                this.mDevicePolicyLocalUserRestrictions.put(userId, local);
            }
        }
        synchronized (this.mPackagesLock) {
            if (localChanged) {
                writeUserLP(getUserDataNoChecks(userId));
                if (globalChanged) {
                    writeUserListLP();
                }
            } else if (globalChanged) {
            }
        }
        synchronized (this.mRestrictionsLock) {
            if (globalChanged) {
                applyUserRestrictionsForAllUsersLR();
            } else if (localChanged) {
                applyUserRestrictionsLR(userId);
            }
        }
    }

    @GuardedBy("mRestrictionsLock")
    private Bundle computeEffectiveUserRestrictionsLR(int userId) {
        Bundle baseRestrictions = UserRestrictionsUtils.nonNull(this.mBaseUserRestrictions.get(userId));
        Bundle global = this.mDevicePolicyGlobalUserRestrictions;
        Bundle local = this.mDevicePolicyLocalUserRestrictions.get(userId);
        if (UserRestrictionsUtils.isEmpty(global) && UserRestrictionsUtils.isEmpty(local)) {
            return baseRestrictions;
        }
        Bundle effective = UserRestrictionsUtils.clone(baseRestrictions);
        UserRestrictionsUtils.merge(effective, global);
        UserRestrictionsUtils.merge(effective, local);
        return effective;
    }

    @GuardedBy("mRestrictionsLock")
    private void invalidateEffectiveUserRestrictionsLR(int userId) {
        this.mCachedEffectiveUserRestrictions.remove(userId);
    }

    private Bundle getEffectiveUserRestrictions(int userId) {
        Bundle restrictions;
        synchronized (this.mRestrictionsLock) {
            restrictions = this.mCachedEffectiveUserRestrictions.get(userId);
            if (restrictions == null) {
                restrictions = computeEffectiveUserRestrictionsLR(userId);
                this.mCachedEffectiveUserRestrictions.put(userId, restrictions);
            }
        }
        return restrictions;
    }

    public boolean hasUserRestriction(String restrictionKey, int userId) {
        Bundle restrictions;
        if (UserRestrictionsUtils.isValidRestriction(restrictionKey) && (restrictions = getEffectiveUserRestrictions(userId)) != null) {
            return restrictions.getBoolean(restrictionKey);
        }
        return false;
    }

    public int getUserRestrictionSource(String restrictionKey, int userId) {
        checkManageUsersPermission("getUserRestrictionSource");
        int result = 0;
        if (!hasUserRestriction(restrictionKey, userId)) {
            return 0;
        }
        if (hasBaseUserRestriction(restrictionKey, userId)) {
            result = 1;
        }
        synchronized (this.mRestrictionsLock) {
            Bundle localRestrictions = this.mDevicePolicyLocalUserRestrictions.get(userId);
            if (!UserRestrictionsUtils.isEmpty(localRestrictions) && localRestrictions.getBoolean(restrictionKey)) {
                if (this.mGlobalRestrictionOwnerUserId == userId) {
                    result |= 2;
                } else {
                    result |= 4;
                }
            }
            if (!UserRestrictionsUtils.isEmpty(this.mDevicePolicyGlobalUserRestrictions)) {
                if (this.mDevicePolicyGlobalUserRestrictions.getBoolean(restrictionKey)) {
                    result |= 2;
                }
            }
        }
        return result;
    }

    public Bundle getUserRestrictions(int userId) {
        return UserRestrictionsUtils.clone(getEffectiveUserRestrictions(userId));
    }

    public boolean hasBaseUserRestriction(String restrictionKey, int userId) {
        boolean z;
        checkManageUsersPermission("hasBaseUserRestriction");
        if (!UserRestrictionsUtils.isValidRestriction(restrictionKey)) {
            return false;
        }
        synchronized (this.mRestrictionsLock) {
            Bundle bundle = this.mBaseUserRestrictions.get(userId);
            z = bundle != null ? bundle.getBoolean(restrictionKey, false) : false;
        }
        return z;
    }

    public void setUserRestriction(String key, boolean value, int userId) {
        checkManageUsersPermission("setUserRestriction");
        if (!UserRestrictionsUtils.isValidRestriction(key)) {
            return;
        }
        synchronized (this.mRestrictionsLock) {
            Bundle newRestrictions = UserRestrictionsUtils.clone(this.mBaseUserRestrictions.get(userId));
            newRestrictions.putBoolean(key, value);
            updateUserRestrictionsInternalLR(newRestrictions, userId);
        }
    }

    @GuardedBy("mRestrictionsLock")
    private void updateUserRestrictionsInternalLR(Bundle newRestrictions, final int userId) {
        Bundle prevAppliedRestrictions = UserRestrictionsUtils.nonNull(this.mAppliedUserRestrictions.get(userId));
        if (newRestrictions != null) {
            Bundle prevBaseRestrictions = this.mBaseUserRestrictions.get(userId);
            Preconditions.checkState(prevBaseRestrictions != newRestrictions);
            Preconditions.checkState(this.mCachedEffectiveUserRestrictions.get(userId) != newRestrictions);
            if (!UserRestrictionsUtils.areEqual(prevBaseRestrictions, newRestrictions)) {
                this.mBaseUserRestrictions.put(userId, newRestrictions);
                scheduleWriteUser(getUserDataNoChecks(userId));
            }
        }
        final Bundle effective = computeEffectiveUserRestrictionsLR(userId);
        this.mCachedEffectiveUserRestrictions.put(userId, effective);
        if (this.mAppOpsService != null) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        UserManagerService.this.mAppOpsService.setUserRestrictions(effective, UserManagerService.mUserRestriconToken, userId);
                    } catch (RemoteException e) {
                        Log.w(UserManagerService.LOG_TAG, "Unable to notify AppOpsService of UserRestrictions");
                    }
                }
            });
        }
        propagateUserRestrictionsLR(userId, effective, prevAppliedRestrictions);
        this.mAppliedUserRestrictions.put(userId, new Bundle(effective));
    }

    private void propagateUserRestrictionsLR(final int userId, Bundle newRestrictions, Bundle prevRestrictions) {
        if (UserRestrictionsUtils.areEqual(newRestrictions, prevRestrictions)) {
            return;
        }
        final Bundle newRestrictionsFinal = new Bundle(newRestrictions);
        final Bundle prevRestrictionsFinal = new Bundle(prevRestrictions);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                UserManagerInternal.UserRestrictionsListener[] listeners;
                UserRestrictionsUtils.applyUserRestrictions(UserManagerService.this.mContext, userId, newRestrictionsFinal, prevRestrictionsFinal);
                synchronized (UserManagerService.this.mUserRestrictionsListeners) {
                    listeners = new UserManagerInternal.UserRestrictionsListener[UserManagerService.this.mUserRestrictionsListeners.size()];
                    UserManagerService.this.mUserRestrictionsListeners.toArray(listeners);
                }
                for (UserManagerInternal.UserRestrictionsListener userRestrictionsListener : listeners) {
                    userRestrictionsListener.onUserRestrictionsChanged(userId, newRestrictionsFinal, prevRestrictionsFinal);
                }
            }
        });
    }

    void applyUserRestrictionsLR(int userId) {
        updateUserRestrictionsInternalLR(null, userId);
    }

    @GuardedBy("mRestrictionsLock")
    void applyUserRestrictionsForAllUsersLR() {
        this.mCachedEffectiveUserRestrictions.clear();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    int[] runningUsers = ActivityManagerNative.getDefault().getRunningUserIds();
                    synchronized (UserManagerService.this.mRestrictionsLock) {
                        for (int i : runningUsers) {
                            UserManagerService.this.applyUserRestrictionsLR(i);
                        }
                    }
                } catch (RemoteException e) {
                    Log.w(UserManagerService.LOG_TAG, "Unable to access ActivityManagerNative");
                }
            }
        };
        this.mHandler.post(r);
    }

    private boolean isUserLimitReached() {
        int count;
        synchronized (this.mUsersLock) {
            count = getAliveUsersExcludingGuestsCountLU();
        }
        return count >= UserManager.getMaxSupportedUsers();
    }

    public boolean canAddMoreManagedProfiles(int userId, boolean allowedToRemoveOne) {
        boolean z = true;
        checkManageUsersPermission("check if more managed profiles can be added.");
        if (ActivityManager.isLowRamDeviceStatic() || !this.mContext.getPackageManager().hasSystemFeature("android.software.managed_users")) {
            return false;
        }
        int managedProfilesCount = getProfiles(userId, true).size() - 1;
        int profilesRemovedCount = (managedProfilesCount <= 0 || !allowedToRemoveOne) ? 0 : 1;
        if (managedProfilesCount - profilesRemovedCount >= 1) {
            return false;
        }
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userId);
            if (!userInfo.canHaveProfile()) {
                return false;
            }
            int usersCountAfterRemoving = getAliveUsersExcludingGuestsCountLU() - profilesRemovedCount;
            if (usersCountAfterRemoving != 1) {
                if (usersCountAfterRemoving >= UserManager.getMaxSupportedUsers()) {
                    z = false;
                }
            }
            return z;
        }
    }

    private int getAliveUsersExcludingGuestsCountLU() {
        int aliveUserCount = 0;
        int totalUserCount = this.mUsers.size();
        for (int i = 0; i < totalUserCount; i++) {
            UserInfo user = this.mUsers.valueAt(i).info;
            if (!this.mRemovingUserIds.get(user.id) && !user.isGuest() && !user.partial) {
                aliveUserCount++;
            }
        }
        return aliveUserCount;
    }

    private static final void checkManageUserAndAcrossUsersFullPermission(String message) {
        int uid = Binder.getCallingUid();
        if (uid == 1000 || uid == 0 || ActivityManager.checkComponentPermission("android.permission.MANAGE_USERS", uid, -1, true) == 0 || ActivityManager.checkComponentPermission("android.permission.INTERACT_ACROSS_USERS_FULL", uid, -1, true) == 0) {
        } else {
            throw new SecurityException("You need MANAGE_USERS and INTERACT_ACROSS_USERS_FULL permission to: " + message);
        }
    }

    private static final void checkManageUsersPermission(String message) {
        if (hasManageUsersPermission()) {
        } else {
            throw new SecurityException("You need MANAGE_USERS permission to: " + message);
        }
    }

    private static final void checkManageOrCreateUsersPermission(String message) {
        if (hasManageOrCreateUsersPermission()) {
        } else {
            throw new SecurityException("You either need MANAGE_USERS or CREATE_USERS permission to: " + message);
        }
    }

    private static final void checkManageOrCreateUsersPermission(int creationFlags) {
        if ((creationFlags & (-301)) == 0) {
            if (hasManageOrCreateUsersPermission()) {
            } else {
                throw new SecurityException("You either need MANAGE_USERS or CREATE_USERS permission to create an user with flags: " + creationFlags);
            }
        } else if (hasManageUsersPermission()) {
        } else {
            throw new SecurityException("You need MANAGE_USERS permission to create an user  with flags: " + creationFlags);
        }
    }

    private static final boolean hasManageUsersPermission() {
        int callingUid = Binder.getCallingUid();
        return UserHandle.isSameApp(callingUid, 1000) || callingUid == 0 || ActivityManager.checkComponentPermission("android.permission.MANAGE_USERS", callingUid, -1, true) == 0;
    }

    private static final boolean hasManageOrCreateUsersPermission() {
        int callingUid = Binder.getCallingUid();
        return UserHandle.isSameApp(callingUid, 1000) || callingUid == 0 || ActivityManager.checkComponentPermission("android.permission.MANAGE_USERS", callingUid, -1, true) == 0 || ActivityManager.checkComponentPermission("android.permission.CREATE_USERS", callingUid, -1, true) == 0;
    }

    private static void checkSystemOrRoot(String message) {
        int uid = Binder.getCallingUid();
        if (UserHandle.isSameApp(uid, 1000) || uid == 0) {
        } else {
            throw new SecurityException("Only system may: " + message);
        }
    }

    private void writeBitmapLP(UserInfo info, Bitmap bitmap) {
        if (bitmap == null) {
            info.iconPath = null;
            return;
        }
        try {
            File dir = new File(this.mUsersDir, Integer.toString(info.id));
            File file = new File(dir, USER_PHOTO_FILENAME);
            File tmp = new File(dir, USER_PHOTO_FILENAME_TMP);
            if (!dir.exists()) {
                dir.mkdir();
                FileUtils.setPermissions(dir.getPath(), 505, -1, -1);
            }
            Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.PNG;
            FileOutputStream os = new FileOutputStream(tmp);
            if (bitmap.compress(compressFormat, 100, os) && tmp.renameTo(file) && SELinux.restorecon(file)) {
                info.iconPath = file.getAbsolutePath();
            }
            try {
                os.close();
            } catch (IOException e) {
            }
            tmp.delete();
        } catch (FileNotFoundException e2) {
            Slog.w(LOG_TAG, "Error setting photo for user ", e2);
        }
    }

    public int[] getUserIds() {
        int[] iArr;
        synchronized (this.mUsersLock) {
            iArr = this.mUserIds;
        }
        return iArr;
    }

    private void readUserListLP() {
        int type;
        String ownerUserId;
        if (!this.mUserListFile.exists()) {
            fallbackToSingleUserLP();
            return;
        }
        AtomicFile userListFile = new AtomicFile(this.mUserListFile);
        try {
            try {
                FileInputStream fis = userListFile.openRead();
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(fis, StandardCharsets.UTF_8.name());
                do {
                    type = parser.next();
                    if (type == 2) {
                        break;
                    }
                } while (type != 1);
                if (type != 2) {
                    Slog.e(LOG_TAG, "Unable to read user list");
                    fallbackToSingleUserLP();
                    IoUtils.closeQuietly(fis);
                    return;
                }
                this.mNextSerialNumber = -1;
                if (parser.getName().equals("users")) {
                    String lastSerialNumber = parser.getAttributeValue(null, ATTR_NEXT_SERIAL_NO);
                    if (lastSerialNumber != null) {
                        this.mNextSerialNumber = Integer.parseInt(lastSerialNumber);
                    }
                    String versionNumber = parser.getAttributeValue(null, ATTR_USER_VERSION);
                    if (versionNumber != null) {
                        this.mUserVersion = Integer.parseInt(versionNumber);
                    }
                }
                Bundle newDevicePolicyGlobalUserRestrictions = new Bundle();
                while (true) {
                    int type2 = parser.next();
                    if (type2 == 1) {
                        synchronized (this.mRestrictionsLock) {
                            this.mDevicePolicyGlobalUserRestrictions = newDevicePolicyGlobalUserRestrictions;
                        }
                        updateUserIds();
                        upgradeIfNecessaryLP();
                        IoUtils.closeQuietly(fis);
                        return;
                    }
                    if (type2 == 2) {
                        String name = parser.getName();
                        if (name.equals(TAG_USER)) {
                            String id = parser.getAttributeValue(null, ATTR_ID);
                            UserData userData = readUserLP(Integer.parseInt(id));
                            if (userData != null) {
                                synchronized (this.mUsersLock) {
                                    this.mUsers.put(userData.info.id, userData);
                                    if (this.mNextSerialNumber < 0 || this.mNextSerialNumber <= userData.info.id) {
                                        this.mNextSerialNumber = userData.info.id + 1;
                                    }
                                }
                            } else {
                                continue;
                            }
                        } else if (name.equals(TAG_GUEST_RESTRICTIONS)) {
                            while (true) {
                                int type3 = parser.next();
                                if (type3 == 1 || type3 == 3) {
                                    break;
                                } else if (type3 == 2) {
                                    break;
                                }
                            }
                        } else if (name.equals(TAG_GLOBAL_RESTRICTION_OWNER_ID) && (ownerUserId = parser.getAttributeValue(null, ATTR_ID)) != null) {
                            this.mGlobalRestrictionOwnerUserId = Integer.parseInt(ownerUserId);
                        }
                    }
                }
            } catch (IOException | XmlPullParserException e) {
                fallbackToSingleUserLP();
                IoUtils.closeQuietly((AutoCloseable) null);
            }
        } catch (Throwable th) {
            IoUtils.closeQuietly((AutoCloseable) null);
            throw th;
        }
    }

    private void upgradeIfNecessaryLP() {
        int originalVersion = this.mUserVersion;
        int userVersion = this.mUserVersion;
        if (userVersion < 1) {
            UserData userData = getUserDataNoChecks(0);
            if ("Primary".equals(userData.info.name)) {
                userData.info.name = this.mContext.getResources().getString(R.string.js_dialog_before_unload);
                scheduleWriteUser(userData);
            }
            userVersion = 1;
        }
        if (userVersion < 2) {
            UserData userData2 = getUserDataNoChecks(0);
            if ((userData2.info.flags & 16) == 0) {
                userData2.info.flags |= 16;
                scheduleWriteUser(userData2);
            }
            userVersion = 2;
        }
        if (userVersion < 4) {
            userVersion = 4;
        }
        if (userVersion < 5) {
            initDefaultGuestRestrictions();
            userVersion = 5;
        }
        if (userVersion < 6) {
            boolean splitSystemUser = UserManager.isSplitSystemUser();
            synchronized (this.mUsersLock) {
                for (int i = 0; i < this.mUsers.size(); i++) {
                    UserData userData3 = this.mUsers.valueAt(i);
                    if (!splitSystemUser && userData3.info.isRestricted() && userData3.info.restrictedProfileParentId == -10000) {
                        userData3.info.restrictedProfileParentId = 0;
                        scheduleWriteUser(userData3);
                    }
                }
            }
            userVersion = 6;
        }
        if (userVersion < 6) {
            Slog.w(LOG_TAG, "User version " + this.mUserVersion + " didn't upgrade as expected to 6");
            return;
        }
        this.mUserVersion = userVersion;
        if (originalVersion >= this.mUserVersion) {
            return;
        }
        writeUserListLP();
    }

    private void fallbackToSingleUserLP() {
        UserData userData = null;
        int flags = 16;
        if (!UserManager.isSplitSystemUser()) {
            flags = 19;
        }
        UserInfo system = new UserInfo(0, (String) null, (String) null, flags);
        UserData userData2 = new UserData(userData);
        userData2.info = system;
        synchronized (this.mUsersLock) {
            this.mUsers.put(system.id, userData2);
        }
        this.mNextSerialNumber = 10;
        this.mUserVersion = 6;
        Bundle restrictions = new Bundle();
        synchronized (this.mRestrictionsLock) {
            this.mBaseUserRestrictions.append(0, restrictions);
        }
        updateUserIds();
        initDefaultGuestRestrictions();
        writeUserLP(userData2);
        writeUserListLP();
    }

    private String getOwnerName() {
        return this.mContext.getResources().getString(R.string.js_dialog_before_unload);
    }

    private void scheduleWriteUser(UserData UserData2) {
        if (this.mHandler.hasMessages(1, UserData2)) {
            return;
        }
        Message msg = this.mHandler.obtainMessage(1, UserData2);
        this.mHandler.sendMessageDelayed(msg, 2000L);
    }

    private void writeUserLP(UserData userData) {
        FileOutputStream fos = null;
        AtomicFile userFile = new AtomicFile(new File(this.mUsersDir, userData.info.id + XML_SUFFIX));
        try {
            fos = userFile.startWrite();
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            XmlSerializer serializer = new FastXmlSerializer();
            serializer.setOutput(bos, StandardCharsets.UTF_8.name());
            serializer.startDocument(null, true);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            UserInfo userInfo = userData.info;
            serializer.startTag(null, TAG_USER);
            serializer.attribute(null, ATTR_ID, Integer.toString(userInfo.id));
            serializer.attribute(null, ATTR_SERIAL_NO, Integer.toString(userInfo.serialNumber));
            serializer.attribute(null, ATTR_FLAGS, Integer.toString(userInfo.flags));
            serializer.attribute(null, ATTR_CREATION_TIME, Long.toString(userInfo.creationTime));
            serializer.attribute(null, ATTR_LAST_LOGGED_IN_TIME, Long.toString(userInfo.lastLoggedInTime));
            if (userInfo.lastLoggedInFingerprint != null) {
                serializer.attribute(null, ATTR_LAST_LOGGED_IN_FINGERPRINT, userInfo.lastLoggedInFingerprint);
            }
            if (userInfo.iconPath != null) {
                serializer.attribute(null, ATTR_ICON_PATH, userInfo.iconPath);
            }
            if (userInfo.partial) {
                serializer.attribute(null, ATTR_PARTIAL, "true");
            }
            if (userInfo.guestToRemove) {
                serializer.attribute(null, ATTR_GUEST_TO_REMOVE, "true");
            }
            if (userInfo.profileGroupId != -10000) {
                serializer.attribute(null, ATTR_PROFILE_GROUP_ID, Integer.toString(userInfo.profileGroupId));
            }
            if (userInfo.restrictedProfileParentId != -10000) {
                serializer.attribute(null, ATTR_RESTRICTED_PROFILE_PARENT_ID, Integer.toString(userInfo.restrictedProfileParentId));
            }
            if (userData.persistSeedData) {
                if (userData.seedAccountName != null) {
                    serializer.attribute(null, ATTR_SEED_ACCOUNT_NAME, userData.seedAccountName);
                }
                if (userData.seedAccountType != null) {
                    serializer.attribute(null, ATTR_SEED_ACCOUNT_TYPE, userData.seedAccountType);
                }
            }
            if (userInfo.name != null) {
                serializer.startTag(null, TAG_NAME);
                serializer.text(userInfo.name);
                serializer.endTag(null, TAG_NAME);
            }
            synchronized (this.mRestrictionsLock) {
                UserRestrictionsUtils.writeRestrictions(serializer, this.mBaseUserRestrictions.get(userInfo.id), TAG_RESTRICTIONS);
                UserRestrictionsUtils.writeRestrictions(serializer, this.mDevicePolicyLocalUserRestrictions.get(userInfo.id), TAG_DEVICE_POLICY_RESTRICTIONS);
            }
            if (userData.account != null) {
                serializer.startTag(null, TAG_ACCOUNT);
                serializer.text(userData.account);
                serializer.endTag(null, TAG_ACCOUNT);
            }
            if (userData.persistSeedData && userData.seedAccountOptions != null) {
                serializer.startTag(null, TAG_SEED_ACCOUNT_OPTIONS);
                userData.seedAccountOptions.saveToXml(serializer);
                serializer.endTag(null, TAG_SEED_ACCOUNT_OPTIONS);
            }
            serializer.endTag(null, TAG_USER);
            serializer.endDocument();
            userFile.finishWrite(fos);
        } catch (Exception ioe) {
            Slog.e(LOG_TAG, "Error writing user info " + userData.info.id, ioe);
            userFile.failWrite(fos);
        }
    }

    private void writeUserListLP() {
        int[] userIdsToWrite;
        FileOutputStream fos = null;
        AtomicFile userListFile = new AtomicFile(this.mUserListFile);
        try {
            fos = userListFile.startWrite();
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(bos, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag(null, "users");
            fastXmlSerializer.attribute(null, ATTR_NEXT_SERIAL_NO, Integer.toString(this.mNextSerialNumber));
            fastXmlSerializer.attribute(null, ATTR_USER_VERSION, Integer.toString(this.mUserVersion));
            fastXmlSerializer.startTag(null, TAG_GUEST_RESTRICTIONS);
            synchronized (this.mGuestRestrictions) {
                UserRestrictionsUtils.writeRestrictions(fastXmlSerializer, this.mGuestRestrictions, TAG_RESTRICTIONS);
            }
            fastXmlSerializer.endTag(null, TAG_GUEST_RESTRICTIONS);
            synchronized (this.mRestrictionsLock) {
                UserRestrictionsUtils.writeRestrictions(fastXmlSerializer, this.mDevicePolicyGlobalUserRestrictions, TAG_DEVICE_POLICY_RESTRICTIONS);
            }
            fastXmlSerializer.startTag(null, TAG_GLOBAL_RESTRICTION_OWNER_ID);
            fastXmlSerializer.attribute(null, ATTR_ID, Integer.toString(this.mGlobalRestrictionOwnerUserId));
            fastXmlSerializer.endTag(null, TAG_GLOBAL_RESTRICTION_OWNER_ID);
            synchronized (this.mUsersLock) {
                userIdsToWrite = new int[this.mUsers.size()];
                for (int i = 0; i < userIdsToWrite.length; i++) {
                    UserInfo user = this.mUsers.valueAt(i).info;
                    userIdsToWrite[i] = user.id;
                }
            }
            for (int id : userIdsToWrite) {
                fastXmlSerializer.startTag(null, TAG_USER);
                fastXmlSerializer.attribute(null, ATTR_ID, Integer.toString(id));
                fastXmlSerializer.endTag(null, TAG_USER);
            }
            fastXmlSerializer.endTag(null, "users");
            fastXmlSerializer.endDocument();
            userListFile.finishWrite(fos);
        } catch (Exception e) {
            userListFile.failWrite(fos);
            Slog.e(LOG_TAG, "Error writing user list");
        }
    }

    private UserData readUserLP(int id) {
        int type;
        int flags = 0;
        int serialNumber = id;
        String name = null;
        String account = null;
        String iconPath = null;
        long creationTime = 0;
        long lastLoggedInTime = 0;
        String lastLoggedInFingerprint = null;
        int profileGroupId = -10000;
        int restrictedProfileParentId = -10000;
        String seedAccountName = null;
        String seedAccountType = null;
        PersistableBundle seedAccountOptions = null;
        Bundle baseRestrictions = new Bundle();
        Bundle localRestrictions = new Bundle();
        FileInputStream fileInputStream = null;
        try {
            AtomicFile userFile = new AtomicFile(new File(this.mUsersDir, Integer.toString(id) + XML_SUFFIX));
            FileInputStream fis = userFile.openRead();
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis, StandardCharsets.UTF_8.name());
            do {
                type = parser.next();
                if (type == 2) {
                    break;
                }
            } while (type != 1);
            if (type != 2) {
                Slog.e(LOG_TAG, "Unable to read user " + id);
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                    }
                }
                return null;
            }
            if (type == 2 && parser.getName().equals(TAG_USER)) {
                int storedId = readIntAttribute(parser, ATTR_ID, -1);
                if (storedId != id) {
                    Slog.e(LOG_TAG, "User id does not match the file name");
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e2) {
                        }
                    }
                    return null;
                }
                serialNumber = readIntAttribute(parser, ATTR_SERIAL_NO, id);
                flags = readIntAttribute(parser, ATTR_FLAGS, 0);
                iconPath = parser.getAttributeValue(null, ATTR_ICON_PATH);
                creationTime = readLongAttribute(parser, ATTR_CREATION_TIME, 0L);
                lastLoggedInTime = readLongAttribute(parser, ATTR_LAST_LOGGED_IN_TIME, 0L);
                lastLoggedInFingerprint = parser.getAttributeValue(null, ATTR_LAST_LOGGED_IN_FINGERPRINT);
                profileGroupId = readIntAttribute(parser, ATTR_PROFILE_GROUP_ID, -10000);
                restrictedProfileParentId = readIntAttribute(parser, ATTR_RESTRICTED_PROFILE_PARENT_ID, -10000);
                String valueString = parser.getAttributeValue(null, ATTR_PARTIAL);
                partial = "true".equals(valueString);
                String valueString2 = parser.getAttributeValue(null, ATTR_GUEST_TO_REMOVE);
                guestToRemove = "true".equals(valueString2);
                seedAccountName = parser.getAttributeValue(null, ATTR_SEED_ACCOUNT_NAME);
                seedAccountType = parser.getAttributeValue(null, ATTR_SEED_ACCOUNT_TYPE);
                persistSeedData = (seedAccountName == null && seedAccountType == null) ? false : true;
                int outerDepth = parser.getDepth();
                while (true) {
                    int type2 = parser.next();
                    if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                        break;
                    }
                    if (type2 != 3 && type2 != 4) {
                        String tag = parser.getName();
                        if (TAG_NAME.equals(tag)) {
                            if (parser.next() == 4) {
                                name = parser.getText();
                            }
                        } else if (TAG_RESTRICTIONS.equals(tag)) {
                            UserRestrictionsUtils.readRestrictions(parser, baseRestrictions);
                        } else if (TAG_DEVICE_POLICY_RESTRICTIONS.equals(tag)) {
                            UserRestrictionsUtils.readRestrictions(parser, localRestrictions);
                        } else if (TAG_ACCOUNT.equals(tag)) {
                            if (parser.next() == 4) {
                                account = parser.getText();
                            }
                        } else if (TAG_SEED_ACCOUNT_OPTIONS.equals(tag)) {
                            seedAccountOptions = PersistableBundle.restoreFromXml(parser);
                            persistSeedData = true;
                        }
                    }
                }
            }
            UserInfo userInfo = new UserInfo(id, name, iconPath, flags);
            userInfo.serialNumber = serialNumber;
            userInfo.creationTime = creationTime;
            userInfo.lastLoggedInTime = lastLoggedInTime;
            userInfo.lastLoggedInFingerprint = lastLoggedInFingerprint;
            userInfo.partial = partial;
            userInfo.guestToRemove = guestToRemove;
            userInfo.profileGroupId = profileGroupId;
            userInfo.restrictedProfileParentId = restrictedProfileParentId;
            UserData userData = new UserData(null);
            userData.info = userInfo;
            userData.account = account;
            userData.seedAccountName = seedAccountName;
            userData.seedAccountType = seedAccountType;
            userData.persistSeedData = persistSeedData;
            userData.seedAccountOptions = seedAccountOptions;
            synchronized (this.mRestrictionsLock) {
                this.mBaseUserRestrictions.put(id, baseRestrictions);
                this.mDevicePolicyLocalUserRestrictions.put(id, localRestrictions);
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e3) {
                }
            }
            return userData;
        } catch (IOException e4) {
            if (0 == 0) {
                return null;
            }
            try {
                fileInputStream.close();
                return null;
            } catch (IOException e5) {
                return null;
            }
        } catch (XmlPullParserException e6) {
            if (0 == 0) {
                return null;
            }
            try {
                fileInputStream.close();
                return null;
            } catch (IOException e7) {
                return null;
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    fileInputStream.close();
                } catch (IOException e8) {
                }
            }
            throw th;
        }
    }

    private int readIntAttribute(XmlPullParser parser, String attr, int defaultValue) {
        String valueString = parser.getAttributeValue(null, attr);
        if (valueString == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(valueString);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private long readLongAttribute(XmlPullParser parser, String attr, long defaultValue) {
        String valueString = parser.getAttributeValue(null, attr);
        if (valueString == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(valueString);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void cleanAppRestrictionsForPackage(String pkg, int userId) {
        synchronized (this.mPackagesLock) {
            File dir = Environment.getUserSystemDirectory(userId);
            File resFile = new File(dir, packageToRestrictionsFileName(pkg));
            if (resFile.exists()) {
                resFile.delete();
            }
        }
    }

    public UserInfo createProfileForUser(String name, int flags, int userId) {
        checkManageOrCreateUsersPermission(flags);
        return createUserInternal(name, flags, userId);
    }

    public UserInfo createUser(String name, int flags) {
        checkManageOrCreateUsersPermission(flags);
        return createUserInternal(name, flags, -10000);
    }

    private UserInfo createUserInternal(String name, int flags, int parentId) {
        if (hasUserRestriction("no_add_user", UserHandle.getCallingUserId())) {
            Log.w(LOG_TAG, "Cannot add user. DISALLOW_ADD_USER is enabled.");
            return null;
        }
        return createUserInternalUnchecked(name, flags, parentId);
    }

    private UserInfo createUserInternalUnchecked(String name, int flags, int parentId) {
        UserInfo userInfo;
        long now;
        UserData userData;
        if (ActivityManager.isLowRamDeviceStatic()) {
            return null;
        }
        boolean isGuest = (flags & 4) != 0;
        boolean isManagedProfile = (flags & 32) != 0;
        boolean isRestricted = (flags & 8) != 0;
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPackagesLock) {
                UserData parent = null;
                if (parentId != -10000) {
                    synchronized (this.mUsersLock) {
                        parent = getUserDataLU(parentId);
                    }
                    if (parent == null) {
                        return null;
                    }
                    if (!isManagedProfile && !canAddMoreManagedProfiles(parentId, false)) {
                        Log.e(LOG_TAG, "Cannot add more managed profiles for user " + parentId);
                        return null;
                    }
                    if (isGuest && !isManagedProfile && isUserLimitReached()) {
                        return null;
                    }
                    if (!isGuest && findCurrentGuestUser() != null) {
                        return null;
                    }
                    if (!isRestricted && !UserManager.isSplitSystemUser() && parentId != 0) {
                        Log.w(LOG_TAG, "Cannot add restricted profile - parent user must be owner");
                        return null;
                    }
                    if (isRestricted && UserManager.isSplitSystemUser()) {
                        if (parent != null) {
                            Log.w(LOG_TAG, "Cannot add restricted profile - parent user must be specified");
                            return null;
                        }
                        if (!parent.info.canHaveProfile()) {
                            Log.w(LOG_TAG, "Cannot add restricted profile - profiles cannot be created for the specified parent user id " + parentId);
                            return null;
                        }
                    }
                    if (UserManager.isSplitSystemUser() && (flags & 256) != 0) {
                        Log.e(LOG_TAG, "Ephemeral users are supported on split-system-user systems only.");
                        return null;
                    }
                    if (UserManager.isSplitSystemUser() || isGuest || isManagedProfile || getPrimaryUser() != null) {
                        int userId = getNextAvailableId();
                        Environment.getUserSystemDirectory(userId).mkdirs();
                        boolean ephemeralGuests = Resources.getSystem().getBoolean(R.^attr-private.panelMenuIsCompact);
                        synchronized (this.mUsersLock) {
                            if (!isGuest || !ephemeralGuests) {
                                if (this.mForceEphemeralUsers || (parent != null && parent.info.isEphemeral())) {
                                }
                                userInfo = new UserInfo(userId, name, (String) null, flags);
                                int i = this.mNextSerialNumber;
                                this.mNextSerialNumber = i + 1;
                                userInfo.serialNumber = i;
                                now = System.currentTimeMillis();
                                if (now <= EPOCH_PLUS_30_YEARS) {
                                    now = 0;
                                }
                                userInfo.creationTime = now;
                                userInfo.partial = true;
                                userInfo.lastLoggedInFingerprint = Build.FINGERPRINT;
                                userData = new UserData(null);
                                userData.info = userInfo;
                                this.mUsers.put(userId, userData);
                            }
                            flags |= 256;
                            userInfo = new UserInfo(userId, name, (String) null, flags);
                            int i2 = this.mNextSerialNumber;
                            this.mNextSerialNumber = i2 + 1;
                            userInfo.serialNumber = i2;
                            now = System.currentTimeMillis();
                            if (now <= EPOCH_PLUS_30_YEARS) {
                            }
                            userInfo.creationTime = now;
                            userInfo.partial = true;
                            userInfo.lastLoggedInFingerprint = Build.FINGERPRINT;
                            userData = new UserData(null);
                            userData.info = userInfo;
                            this.mUsers.put(userId, userData);
                        }
                        writeUserLP(userData);
                        writeUserListLP();
                        if (parent != null) {
                            if (isManagedProfile) {
                                if (parent.info.profileGroupId == -10000) {
                                    parent.info.profileGroupId = parent.info.id;
                                    writeUserLP(parent);
                                }
                                userInfo.profileGroupId = parent.info.profileGroupId;
                            } else if (isRestricted) {
                                if (parent.info.restrictedProfileParentId == -10000) {
                                    parent.info.restrictedProfileParentId = parent.info.id;
                                    writeUserLP(parent);
                                }
                                userInfo.restrictedProfileParentId = parent.info.restrictedProfileParentId;
                            }
                        }
                        StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
                        storage.createUserKey(userId, userInfo.serialNumber, userInfo.isEphemeral());
                        this.mPm.prepareUserData(userId, userInfo.serialNumber, 3);
                        this.mPm.createNewUser(userId);
                        userInfo.partial = false;
                        synchronized (this.mPackagesLock) {
                            writeUserLP(userData);
                        }
                        updateUserIds();
                        Bundle restrictions = new Bundle();
                        if (isGuest) {
                            synchronized (this.mGuestRestrictions) {
                                restrictions.putAll(this.mGuestRestrictions);
                            }
                        }
                        synchronized (this.mRestrictionsLock) {
                            this.mBaseUserRestrictions.append(userId, restrictions);
                        }
                        Intent addedIntent = new Intent("android.intent.action.USER_ADDED");
                        addedIntent.putExtra("android.intent.extra.user_handle", userId);
                        this.mContext.sendBroadcastAsUser(addedIntent, UserHandle.ALL, "android.permission.MANAGE_USERS");
                        MetricsLogger.count(this.mContext, isGuest ? TRON_GUEST_CREATED : TRON_USER_CREATED, 1);
                        return userInfo;
                    }
                    flags |= 1;
                    synchronized (this.mUsersLock) {
                        if (!this.mIsDeviceManaged) {
                            flags |= 2;
                        }
                    }
                    int userId2 = getNextAvailableId();
                    Environment.getUserSystemDirectory(userId2).mkdirs();
                    boolean ephemeralGuests2 = Resources.getSystem().getBoolean(R.^attr-private.panelMenuIsCompact);
                    synchronized (this.mUsersLock) {
                    }
                } else {
                    if (!isManagedProfile) {
                    }
                    if (isGuest) {
                    }
                    if (!isGuest) {
                    }
                    if (!isRestricted) {
                    }
                    if (isRestricted) {
                        if (parent != null) {
                        }
                    }
                    if (UserManager.isSplitSystemUser()) {
                    }
                    if (UserManager.isSplitSystemUser()) {
                        int userId22 = getNextAvailableId();
                        Environment.getUserSystemDirectory(userId22).mkdirs();
                        boolean ephemeralGuests22 = Resources.getSystem().getBoolean(R.^attr-private.panelMenuIsCompact);
                        synchronized (this.mUsersLock) {
                        }
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public UserInfo createRestrictedProfile(String name, int parentUserId) {
        checkManageOrCreateUsersPermission("setupRestrictedProfile");
        UserInfo user = createProfileForUser(name, 8, parentUserId);
        if (user == null) {
            return null;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            setUserRestriction("no_modify_accounts", true, user.id);
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "location_mode", 0, user.id);
            setUserRestriction("no_share_location", true, user.id);
            return user;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private UserInfo findCurrentGuestUser() {
        synchronized (this.mUsersLock) {
            int size = this.mUsers.size();
            for (int i = 0; i < size; i++) {
                UserInfo user = this.mUsers.valueAt(i).info;
                if (user.isGuest() && !user.guestToRemove && !this.mRemovingUserIds.get(user.id)) {
                    return user;
                }
            }
            return null;
        }
    }

    public boolean markGuestForDeletion(int userHandle) {
        checkManageUsersPermission("Only the system can remove users");
        if (getUserRestrictions(UserHandle.getCallingUserId()).getBoolean("no_remove_user", false)) {
            Log.w(LOG_TAG, "Cannot remove user. DISALLOW_REMOVE_USER is enabled.");
            return false;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPackagesLock) {
                synchronized (this.mUsersLock) {
                    UserData userData = this.mUsers.get(userHandle);
                    if (userHandle != 0 && userData != null) {
                        if (!this.mRemovingUserIds.get(userHandle)) {
                            if (!userData.info.isGuest()) {
                                return false;
                            }
                            userData.info.guestToRemove = true;
                            userData.info.flags |= 64;
                            writeUserLP(userData);
                            return true;
                        }
                    }
                    return false;
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean removeUser(int userHandle) {
        checkManageOrCreateUsersPermission("Only the system can remove users");
        if (getUserRestrictions(UserHandle.getCallingUserId()).getBoolean("no_remove_user", false)) {
            Log.w(LOG_TAG, "Cannot remove user. DISALLOW_REMOVE_USER is enabled.");
            return false;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            int currentUser = ActivityManager.getCurrentUser();
            if (currentUser == userHandle) {
                Log.w(LOG_TAG, "Current user cannot be removed");
                return false;
            }
            synchronized (this.mPackagesLock) {
                synchronized (this.mUsersLock) {
                    UserData userData = this.mUsers.get(userHandle);
                    if (userHandle == 0 || userData == null || this.mRemovingUserIds.get(userHandle)) {
                        return false;
                    }
                    this.mRemovingUserIds.put(userHandle, true);
                    try {
                        this.mAppOpsService.removeUser(userHandle);
                    } catch (RemoteException e) {
                        Log.w(LOG_TAG, "Unable to notify AppOpsService of removing user", e);
                    }
                    userData.info.partial = true;
                    userData.info.flags |= 64;
                    writeUserLP(userData);
                    if (userData.info.profileGroupId != -10000 && userData.info.isManagedProfile()) {
                        sendProfileRemovedBroadcast(userData.info.profileGroupId, userData.info.id);
                    }
                    try {
                        int res = ActivityManagerNative.getDefault().stopUser(userHandle, true, new IStopUserCallback.Stub() {
                            public void userStopped(int userId) {
                                UserManagerService.this.finishRemoveUser(userId);
                            }

                            public void userStopAborted(int userId) {
                            }
                        });
                        return res == 0;
                    } catch (RemoteException e2) {
                        return false;
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    void finishRemoveUser(final int userHandle) {
        long ident = Binder.clearCallingIdentity();
        try {
            Intent addedIntent = new Intent("android.intent.action.USER_REMOVED");
            addedIntent.putExtra("android.intent.extra.user_handle", userHandle);
            this.mContext.sendOrderedBroadcastAsUser(addedIntent, UserHandle.ALL, "android.permission.MANAGE_USERS", new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    final int i = userHandle;
                    new Thread() {
                        @Override
                        public void run() {
                            ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).onUserRemoved(i);
                            UserManagerService.this.removeUserState(i);
                        }
                    }.start();
                }
            }, null, -1, null, null);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void removeUserState(int userHandle) {
        try {
            ((StorageManager) this.mContext.getSystemService(StorageManager.class)).destroyUserKey(userHandle);
        } catch (IllegalStateException e) {
            Slog.i(LOG_TAG, "Destroying key for user " + userHandle + " failed, continuing anyway", e);
        }
        try {
            IGateKeeperService gk = GateKeeper.getService();
            if (gk != null) {
                gk.clearSecureUserId(userHandle);
            }
        } catch (Exception e2) {
            Slog.w(LOG_TAG, "unable to clear GK secure user id");
        }
        this.mPm.cleanUpUser(this, userHandle);
        this.mPm.destroyUserData(userHandle, 3);
        synchronized (this.mUsersLock) {
            this.mUsers.remove(userHandle);
            this.mIsUserManaged.delete(userHandle);
        }
        synchronized (this.mUserStates) {
            this.mUserStates.delete(userHandle);
        }
        synchronized (this.mRestrictionsLock) {
            this.mBaseUserRestrictions.remove(userHandle);
            this.mAppliedUserRestrictions.remove(userHandle);
            this.mCachedEffectiveUserRestrictions.remove(userHandle);
            this.mDevicePolicyLocalUserRestrictions.remove(userHandle);
        }
        synchronized (this.mPackagesLock) {
            writeUserListLP();
        }
        AtomicFile userFile = new AtomicFile(new File(this.mUsersDir, userHandle + XML_SUFFIX));
        userFile.delete();
        updateUserIds();
    }

    private void sendProfileRemovedBroadcast(int parentUserId, int removedUserId) {
        Intent managedProfileIntent = new Intent("android.intent.action.MANAGED_PROFILE_REMOVED");
        managedProfileIntent.addFlags(1342177280);
        managedProfileIntent.putExtra("android.intent.extra.USER", new UserHandle(removedUserId));
        managedProfileIntent.putExtra("android.intent.extra.user_handle", removedUserId);
        this.mContext.sendBroadcastAsUser(managedProfileIntent, new UserHandle(parentUserId), null);
    }

    public Bundle getApplicationRestrictions(String packageName) {
        return getApplicationRestrictionsForUser(packageName, UserHandle.getCallingUserId());
    }

    public Bundle getApplicationRestrictionsForUser(String packageName, int userId) {
        Bundle applicationRestrictionsLP;
        if (UserHandle.getCallingUserId() != userId || !UserHandle.isSameApp(Binder.getCallingUid(), getUidForPackage(packageName))) {
            checkSystemOrRoot("get application restrictions for other users/apps");
        }
        synchronized (this.mPackagesLock) {
            applicationRestrictionsLP = readApplicationRestrictionsLP(packageName, userId);
        }
        return applicationRestrictionsLP;
    }

    public void setApplicationRestrictions(String packageName, Bundle restrictions, int userId) {
        checkSystemOrRoot("set application restrictions");
        if (restrictions != null) {
            restrictions.setDefusable(true);
        }
        synchronized (this.mPackagesLock) {
            if (restrictions != null) {
                if (restrictions.isEmpty()) {
                    cleanAppRestrictionsForPackage(packageName, userId);
                } else {
                    writeApplicationRestrictionsLP(packageName, restrictions, userId);
                }
            }
        }
        Intent changeIntent = new Intent("android.intent.action.APPLICATION_RESTRICTIONS_CHANGED");
        changeIntent.setPackage(packageName);
        changeIntent.addFlags(1073741824);
        this.mContext.sendBroadcastAsUser(changeIntent, UserHandle.of(userId));
    }

    private int getUidForPackage(String packageName) {
        long ident = Binder.clearCallingIdentity();
        try {
            return this.mContext.getPackageManager().getApplicationInfo(packageName, PackageManagerService.DumpState.DUMP_PREFERRED_XML).uid;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private Bundle readApplicationRestrictionsLP(String packageName, int userId) {
        AtomicFile restrictionsFile = new AtomicFile(new File(Environment.getUserSystemDirectory(userId), packageToRestrictionsFileName(packageName)));
        return readApplicationRestrictionsLP(restrictionsFile);
    }

    static Bundle readApplicationRestrictionsLP(AtomicFile restrictionsFile) {
        FileInputStream fis;
        XmlPullParser parser;
        Bundle restrictions = new Bundle();
        ArrayList<String> values = new ArrayList<>();
        if (!restrictionsFile.getBaseFile().exists()) {
            return restrictions;
        }
        try {
            try {
                fis = restrictionsFile.openRead();
                parser = Xml.newPullParser();
                parser.setInput(fis, StandardCharsets.UTF_8.name());
                XmlUtils.nextElement(parser);
            } catch (IOException | XmlPullParserException e) {
                Log.w(LOG_TAG, "Error parsing " + restrictionsFile.getBaseFile(), e);
                IoUtils.closeQuietly((AutoCloseable) null);
            }
            if (parser.getEventType() != 2) {
                Slog.e(LOG_TAG, "Unable to read restrictions file " + restrictionsFile.getBaseFile());
                IoUtils.closeQuietly(fis);
                return restrictions;
            }
            while (parser.next() != 1) {
                readEntry(restrictions, values, parser);
            }
            IoUtils.closeQuietly(fis);
            return restrictions;
        } catch (Throwable th) {
            IoUtils.closeQuietly((AutoCloseable) null);
            throw th;
        }
    }

    private static void readEntry(Bundle restrictions, ArrayList<String> values, XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != 2 || !parser.getName().equals(TAG_ENTRY)) {
            return;
        }
        String key = parser.getAttributeValue(null, ATTR_KEY);
        String valType = parser.getAttributeValue(null, "type");
        String multiple = parser.getAttributeValue(null, ATTR_MULTIPLE);
        if (multiple != null) {
            values.clear();
            int count = Integer.parseInt(multiple);
            while (count > 0) {
                int type = parser.next();
                if (type == 1) {
                    break;
                }
                if (type == 2 && parser.getName().equals(TAG_VALUE)) {
                    values.add(parser.nextText().trim());
                    count--;
                }
            }
            String[] valueStrings = new String[values.size()];
            values.toArray(valueStrings);
            restrictions.putStringArray(key, valueStrings);
            return;
        }
        if (ATTR_TYPE_BUNDLE.equals(valType)) {
            restrictions.putBundle(key, readBundleEntry(parser, values));
            return;
        }
        if (ATTR_TYPE_BUNDLE_ARRAY.equals(valType)) {
            int outerDepth = parser.getDepth();
            ArrayList<Bundle> bundleList = new ArrayList<>();
            while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                Bundle childBundle = readBundleEntry(parser, values);
                bundleList.add(childBundle);
            }
            restrictions.putParcelableArray(key, (Parcelable[]) bundleList.toArray(new Bundle[bundleList.size()]));
            return;
        }
        String value = parser.nextText().trim();
        if (ATTR_TYPE_BOOLEAN.equals(valType)) {
            restrictions.putBoolean(key, Boolean.parseBoolean(value));
        } else if (ATTR_TYPE_INTEGER.equals(valType)) {
            restrictions.putInt(key, Integer.parseInt(value));
        } else {
            restrictions.putString(key, value);
        }
    }

    private static Bundle readBundleEntry(XmlPullParser parser, ArrayList<String> values) throws XmlPullParserException, IOException {
        Bundle childBundle = new Bundle();
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            readEntry(childBundle, values, parser);
        }
        return childBundle;
    }

    private void writeApplicationRestrictionsLP(String packageName, Bundle restrictions, int userId) {
        AtomicFile restrictionsFile = new AtomicFile(new File(Environment.getUserSystemDirectory(userId), packageToRestrictionsFileName(packageName)));
        writeApplicationRestrictionsLP(restrictions, restrictionsFile);
    }

    static void writeApplicationRestrictionsLP(Bundle restrictions, AtomicFile restrictionsFile) {
        FileOutputStream fos = null;
        try {
            fos = restrictionsFile.startWrite();
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(bos, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag(null, TAG_RESTRICTIONS);
            writeBundle(restrictions, fastXmlSerializer);
            fastXmlSerializer.endTag(null, TAG_RESTRICTIONS);
            fastXmlSerializer.endDocument();
            restrictionsFile.finishWrite(fos);
        } catch (Exception e) {
            restrictionsFile.failWrite(fos);
            Slog.e(LOG_TAG, "Error writing application restrictions list", e);
        }
    }

    private static void writeBundle(Bundle restrictions, XmlSerializer serializer) throws IOException {
        for (String key : restrictions.keySet()) {
            Object value = restrictions.get(key);
            serializer.startTag(null, TAG_ENTRY);
            serializer.attribute(null, ATTR_KEY, key);
            if (value instanceof Boolean) {
                serializer.attribute(null, "type", ATTR_TYPE_BOOLEAN);
                serializer.text(value.toString());
            } else if (value instanceof Integer) {
                serializer.attribute(null, "type", ATTR_TYPE_INTEGER);
                serializer.text(value.toString());
            } else if (value == null || (value instanceof String)) {
                serializer.attribute(null, "type", ATTR_TYPE_STRING);
                serializer.text(value != null ? (String) value : "");
            } else if (value instanceof Bundle) {
                serializer.attribute(null, "type", ATTR_TYPE_BUNDLE);
                writeBundle((Bundle) value, serializer);
            } else if (value instanceof Parcelable[]) {
                serializer.attribute(null, "type", ATTR_TYPE_BUNDLE_ARRAY);
                Parcelable[] array = (Parcelable[]) value;
                for (Parcelable parcelable : array) {
                    if (!(parcelable instanceof Bundle)) {
                        throw new IllegalArgumentException("bundle-array can only hold Bundles");
                    }
                    serializer.startTag(null, TAG_ENTRY);
                    serializer.attribute(null, "type", ATTR_TYPE_BUNDLE);
                    writeBundle((Bundle) parcelable, serializer);
                    serializer.endTag(null, TAG_ENTRY);
                }
            } else {
                serializer.attribute(null, "type", ATTR_TYPE_STRING_ARRAY);
                String[] values = (String[]) value;
                serializer.attribute(null, ATTR_MULTIPLE, Integer.toString(values.length));
                for (String choice : values) {
                    serializer.startTag(null, TAG_VALUE);
                    if (choice == null) {
                        choice = "";
                    }
                    serializer.text(choice);
                    serializer.endTag(null, TAG_VALUE);
                }
            }
            serializer.endTag(null, TAG_ENTRY);
        }
    }

    public int getUserSerialNumber(int userHandle) {
        synchronized (this.mUsersLock) {
            if (!exists(userHandle)) {
                return -1;
            }
            return getUserInfoLU(userHandle).serialNumber;
        }
    }

    public int getUserHandle(int userSerialNumber) {
        synchronized (this.mUsersLock) {
            for (int userId : this.mUserIds) {
                UserInfo info = getUserInfoLU(userId);
                if (info != null && info.serialNumber == userSerialNumber) {
                    return userId;
                }
            }
            return -1;
        }
    }

    public long getUserCreationTime(int userHandle) {
        int callingUserId = UserHandle.getCallingUserId();
        UserInfo userInfo = null;
        synchronized (this.mUsersLock) {
            if (callingUserId == userHandle) {
                userInfo = getUserInfoLU(userHandle);
            } else {
                UserInfo parent = getProfileParentLU(userHandle);
                if (parent != null && parent.id == callingUserId) {
                    userInfo = getUserInfoLU(userHandle);
                }
            }
        }
        if (userInfo == null) {
            throw new SecurityException("userHandle can only be the calling user or a managed profile associated with this user");
        }
        return userInfo.creationTime;
    }

    private void updateUserIds() {
        int n;
        int num = 0;
        synchronized (this.mUsersLock) {
            int userSize = this.mUsers.size();
            for (int i = 0; i < userSize; i++) {
                if (!this.mUsers.valueAt(i).info.partial) {
                    num++;
                }
            }
            int[] newUsers = new int[num];
            int i2 = 0;
            int n2 = 0;
            while (i2 < userSize) {
                if (this.mUsers.valueAt(i2).info.partial) {
                    n = n2;
                } else {
                    n = n2 + 1;
                    newUsers[n2] = this.mUsers.keyAt(i2);
                }
                i2++;
                n2 = n;
            }
            this.mUserIds = newUsers;
        }
    }

    public void onBeforeStartUser(int userId) {
        int userSerial = getUserSerialNumber(userId);
        this.mPm.prepareUserData(userId, userSerial, 1);
        this.mPm.reconcileAppsData(userId, 1);
        if (userId == 0) {
            return;
        }
        synchronized (this.mRestrictionsLock) {
            applyUserRestrictionsLR(userId);
        }
        UserInfo userInfo = getUserInfoNoChecks(userId);
        if (userInfo == null || userInfo.isInitialized()) {
            return;
        }
        this.mPm.onBeforeUserStartUninitialized(userId);
    }

    public void onBeforeUnlockUser(int userId) {
        int userSerial = getUserSerialNumber(userId);
        this.mPm.prepareUserData(userId, userSerial, 2);
        this.mPm.reconcileAppsData(userId, 2);
    }

    public void onUserLoggedIn(int userId) {
        UserData userData = getUserDataNoChecks(userId);
        if (userData == null || userData.info.partial) {
            Slog.w(LOG_TAG, "userForeground: unknown user #" + userId);
            return;
        }
        Slog.d(LOG_TAG, "LoggedIn User Id from: " + this.mSwitchedUserId + " to : " + userId);
        this.mSwitchedUserId = userId;
        long now = System.currentTimeMillis();
        if (now > EPOCH_PLUS_30_YEARS) {
            userData.info.lastLoggedInTime = now;
        }
        userData.info.lastLoggedInFingerprint = Build.FINGERPRINT;
        scheduleWriteUser(userData);
    }

    private int getNextAvailableId() {
        synchronized (this.mUsersLock) {
            for (int i = 10; i < MAX_USER_ID; i++) {
                if (this.mUsers.indexOfKey(i) < 0 && !this.mRemovingUserIds.get(i)) {
                    return i;
                }
            }
            throw new IllegalStateException("No user id available!");
        }
    }

    private String packageToRestrictionsFileName(String packageName) {
        return RESTRICTIONS_FILE_PREFIX + packageName + XML_SUFFIX;
    }

    public static void enforceSerialNumber(File file, int serialNumber) throws IOException {
        if (StorageManager.isFileEncryptedEmulatedOnly()) {
            Slog.w(LOG_TAG, "Device is emulating FBE; assuming current serial number is valid");
            return;
        }
        int foundSerial = getSerialNumber(file);
        Slog.v(LOG_TAG, "Found " + file + " with serial number " + foundSerial);
        if (foundSerial == -1) {
            Slog.d(LOG_TAG, "Serial number missing on " + file + "; assuming current is valid");
            try {
                setSerialNumber(file, serialNumber);
                return;
            } catch (IOException e) {
                Slog.w(LOG_TAG, "Failed to set serial number on " + file, e);
                return;
            }
        }
        if (foundSerial == serialNumber) {
        } else {
            throw new IOException("Found serial number " + foundSerial + " doesn't match expected " + serialNumber);
        }
    }

    private static void setSerialNumber(File file, int serialNumber) throws IOException {
        try {
            byte[] buf = Integer.toString(serialNumber).getBytes(StandardCharsets.UTF_8);
            Os.setxattr(file.getAbsolutePath(), XATTR_SERIAL, buf, OsConstants.XATTR_CREATE);
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    private static int getSerialNumber(File file) throws IOException {
        try {
            byte[] buf = new byte[256];
            int len = Os.getxattr(file.getAbsolutePath(), XATTR_SERIAL, buf);
            String serial = new String(buf, 0, len);
            try {
                return Integer.parseInt(serial);
            } catch (NumberFormatException e) {
                throw new IOException("Bad serial number: " + serial);
            }
        } catch (ErrnoException e2) {
            if (e2.errno == OsConstants.ENODATA) {
                return -1;
            }
            throw e2.rethrowAsIOException();
        }
    }

    public void setSeedAccountData(int userId, String accountName, String accountType, PersistableBundle accountOptions, boolean persist) {
        checkManageUsersPermission("Require MANAGE_USERS permission to set user seed data");
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                UserData userData = getUserDataLU(userId);
                if (userData == null) {
                    Slog.e(LOG_TAG, "No such user for settings seed data u=" + userId);
                    return;
                }
                userData.seedAccountName = accountName;
                userData.seedAccountType = accountType;
                userData.seedAccountOptions = accountOptions;
                userData.persistSeedData = persist;
                if (persist) {
                    writeUserLP(userData);
                }
            }
        }
    }

    public String getSeedAccountName() throws RemoteException {
        String str;
        checkManageUsersPermission("Cannot get seed account information");
        synchronized (this.mUsersLock) {
            UserData userData = getUserDataLU(UserHandle.getCallingUserId());
            str = userData.seedAccountName;
        }
        return str;
    }

    public String getSeedAccountType() throws RemoteException {
        String str;
        checkManageUsersPermission("Cannot get seed account information");
        synchronized (this.mUsersLock) {
            UserData userData = getUserDataLU(UserHandle.getCallingUserId());
            str = userData.seedAccountType;
        }
        return str;
    }

    public PersistableBundle getSeedAccountOptions() throws RemoteException {
        PersistableBundle persistableBundle;
        checkManageUsersPermission("Cannot get seed account information");
        synchronized (this.mUsersLock) {
            UserData userData = getUserDataLU(UserHandle.getCallingUserId());
            persistableBundle = userData.seedAccountOptions;
        }
        return persistableBundle;
    }

    public void clearSeedAccountData() throws RemoteException {
        checkManageUsersPermission("Cannot clear seed account information");
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                UserData userData = getUserDataLU(UserHandle.getCallingUserId());
                if (userData == null) {
                    return;
                }
                userData.clearSeedAccountData();
                writeUserLP(userData);
            }
        }
    }

    public boolean someUserHasSeedAccount(String accountName, String accountType) throws RemoteException {
        checkManageUsersPermission("Cannot check seed account information");
        synchronized (this.mUsersLock) {
            int userSize = this.mUsers.size();
            for (int i = 0; i < userSize; i++) {
                UserData data = this.mUsers.valueAt(i);
                if (!data.info.isInitialized() && data.seedAccountName != null && data.seedAccountName.equals(accountName) && data.seedAccountType != null && data.seedAccountType.equals(accountType)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ResultReceiver resultReceiver) {
        new Shell(this, null).exec(this, in, out, err, args, resultReceiver);
    }

    int onShellCommand(Shell shell, String cmd) {
        if (cmd == null) {
            return shell.handleDefaultCommands(cmd);
        }
        PrintWriter pw = shell.getOutPrintWriter();
        try {
            if (cmd.equals("list")) {
                return runList(pw);
            }
            return -1;
        } catch (RemoteException e) {
            pw.println("Remote exception: " + e);
            return -1;
        }
    }

    private int runList(PrintWriter pw) throws RemoteException {
        IActivityManager am = ActivityManagerNative.getDefault();
        List<UserInfo> users = getUsers(false);
        if (users == null) {
            pw.println("Error: couldn't get users");
            return 1;
        }
        pw.println("Users:");
        for (int i = 0; i < users.size(); i++) {
            String running = am.isUserRunning(users.get(i).id, 0) ? " running" : "";
            pw.println("\t" + users.get(i).toString() + running);
        }
        return 0;
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump UserManager from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " without permission android.permission.DUMP");
            return;
        }
        long now = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                pw.println("Users:");
                for (int i = 0; i < this.mUsers.size(); i++) {
                    UserData userData = this.mUsers.valueAt(i);
                    if (userData != null) {
                        UserInfo userInfo = userData.info;
                        int userId = userInfo.id;
                        pw.print("  ");
                        pw.print(userInfo);
                        pw.print(" serialNo=");
                        pw.print(userInfo.serialNumber);
                        if (this.mRemovingUserIds.get(userId)) {
                            pw.print(" <removing> ");
                        }
                        if (userInfo.partial) {
                            pw.print(" <partial>");
                        }
                        pw.println();
                        pw.print("    Created: ");
                        if (userInfo.creationTime == 0) {
                            pw.println("<unknown>");
                        } else {
                            sb.setLength(0);
                            TimeUtils.formatDuration(now - userInfo.creationTime, sb);
                            sb.append(" ago");
                            pw.println(sb);
                        }
                        pw.print("    Last logged in: ");
                        if (userInfo.lastLoggedInTime == 0) {
                            pw.println("<unknown>");
                        } else {
                            sb.setLength(0);
                            TimeUtils.formatDuration(now - userInfo.lastLoggedInTime, sb);
                            sb.append(" ago");
                            pw.println(sb);
                        }
                        pw.print("    Last logged in fingerprint: ");
                        pw.println(userInfo.lastLoggedInFingerprint);
                        pw.print("    Has profile owner: ");
                        pw.println(this.mIsUserManaged.get(userId));
                        pw.println("    Restrictions:");
                        synchronized (this.mRestrictionsLock) {
                            UserRestrictionsUtils.dumpRestrictions(pw, "      ", this.mBaseUserRestrictions.get(userInfo.id));
                            pw.println("    Device policy local restrictions:");
                            UserRestrictionsUtils.dumpRestrictions(pw, "      ", this.mDevicePolicyLocalUserRestrictions.get(userInfo.id));
                            pw.println("    Effective restrictions:");
                            UserRestrictionsUtils.dumpRestrictions(pw, "      ", this.mCachedEffectiveUserRestrictions.get(userInfo.id));
                        }
                        if (userData.account != null) {
                            pw.print("    Account name: " + userData.account);
                            pw.println();
                        }
                        if (userData.seedAccountName != null) {
                            pw.print("    Seed account name: " + userData.seedAccountName);
                            pw.println();
                            if (userData.seedAccountType != null) {
                                pw.print("         account type: " + userData.seedAccountType);
                                pw.println();
                            }
                            if (userData.seedAccountOptions != null) {
                                pw.print("         account options exist");
                                pw.println();
                            }
                        }
                    }
                }
            }
            pw.println();
            pw.println("  Device policy global restrictions:");
            synchronized (this.mRestrictionsLock) {
                UserRestrictionsUtils.dumpRestrictions(pw, "    ", this.mDevicePolicyGlobalUserRestrictions);
            }
            pw.println();
            pw.println("  Global restrictions owner id:" + this.mGlobalRestrictionOwnerUserId);
            pw.println();
            pw.println("  Guest restrictions:");
            synchronized (this.mGuestRestrictions) {
                UserRestrictionsUtils.dumpRestrictions(pw, "    ", this.mGuestRestrictions);
            }
            synchronized (this.mUsersLock) {
                pw.println();
                pw.println("  Device managed: " + this.mIsDeviceManaged);
            }
            synchronized (this.mUserStates) {
                pw.println("  Started users state: " + this.mUserStates);
            }
            pw.println();
            pw.println("  Max users: " + UserManager.getMaxSupportedUsers());
            pw.println("  Supports switchable users: " + UserManager.supportsMultipleUsers());
            pw.println("  All guests ephemeral: " + Resources.getSystem().getBoolean(R.^attr-private.panelMenuIsCompact));
        }
    }

    final class MainHandler extends Handler {
        MainHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    removeMessages(1, msg.obj);
                    synchronized (UserManagerService.this.mPackagesLock) {
                        int userId = ((UserData) msg.obj).info.id;
                        UserData userData = UserManagerService.this.getUserDataNoChecks(userId);
                        if (userData != null) {
                            UserManagerService.this.writeUserLP(userData);
                        }
                        break;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    boolean isInitialized(int userId) {
        return (getUserInfo(userId).flags & 16) != 0;
    }

    private class LocalService extends UserManagerInternal {
        LocalService(UserManagerService this$0, LocalService localService) {
            this();
        }

        private LocalService() {
        }

        public void setDevicePolicyUserRestrictions(int userId, Bundle localRestrictions, Bundle globalRestrictions) {
            UserManagerService.this.setDevicePolicyUserRestrictionsInner(userId, localRestrictions, globalRestrictions);
        }

        public Bundle getBaseUserRestrictions(int userId) {
            Bundle bundle;
            synchronized (UserManagerService.this.mRestrictionsLock) {
                bundle = (Bundle) UserManagerService.this.mBaseUserRestrictions.get(userId);
            }
            return bundle;
        }

        public void setBaseUserRestrictionsByDpmsForMigration(int userId, Bundle baseRestrictions) {
            synchronized (UserManagerService.this.mRestrictionsLock) {
                UserManagerService.this.mBaseUserRestrictions.put(userId, new Bundle(baseRestrictions));
                UserManagerService.this.invalidateEffectiveUserRestrictionsLR(userId);
            }
            UserData userData = UserManagerService.this.getUserDataNoChecks(userId);
            synchronized (UserManagerService.this.mPackagesLock) {
                if (userData != null) {
                    UserManagerService.this.writeUserLP(userData);
                } else {
                    Slog.w(UserManagerService.LOG_TAG, "UserInfo not found for " + userId);
                }
            }
        }

        public boolean getUserRestriction(int userId, String key) {
            return UserManagerService.this.getUserRestrictions(userId).getBoolean(key);
        }

        public void addUserRestrictionsListener(UserManagerInternal.UserRestrictionsListener listener) {
            synchronized (UserManagerService.this.mUserRestrictionsListeners) {
                UserManagerService.this.mUserRestrictionsListeners.add(listener);
            }
        }

        public void removeUserRestrictionsListener(UserManagerInternal.UserRestrictionsListener listener) {
            synchronized (UserManagerService.this.mUserRestrictionsListeners) {
                UserManagerService.this.mUserRestrictionsListeners.remove(listener);
            }
        }

        public void setDeviceManaged(boolean isManaged) {
            synchronized (UserManagerService.this.mUsersLock) {
                UserManagerService.this.mIsDeviceManaged = isManaged;
            }
        }

        public void setUserManaged(int userId, boolean isManaged) {
            synchronized (UserManagerService.this.mUsersLock) {
                UserManagerService.this.mIsUserManaged.put(userId, isManaged);
            }
        }

        public void setUserIcon(int userId, Bitmap bitmap) {
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (UserManagerService.this.mPackagesLock) {
                    UserData userData = UserManagerService.this.getUserDataNoChecks(userId);
                    if (userData == null || userData.info.partial) {
                        Slog.w(UserManagerService.LOG_TAG, "setUserIcon: unknown user #" + userId);
                        return;
                    }
                    UserManagerService.this.writeBitmapLP(userData.info, bitmap);
                    UserManagerService.this.writeUserLP(userData);
                    UserManagerService.this.sendUserInfoChangedBroadcast(userId);
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void setForceEphemeralUsers(boolean forceEphemeralUsers) {
            synchronized (UserManagerService.this.mUsersLock) {
                UserManagerService.this.mForceEphemeralUsers = forceEphemeralUsers;
            }
        }

        public void removeAllUsers() {
            if (ActivityManager.getCurrentUser() == 0) {
                UserManagerService.this.removeNonSystemUsers();
                return;
            }
            BroadcastReceiver userSwitchedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                    if (userId != 0) {
                        return;
                    }
                    UserManagerService.this.mContext.unregisterReceiver(this);
                    UserManagerService.this.removeNonSystemUsers();
                }
            };
            IntentFilter userSwitchedFilter = new IntentFilter();
            userSwitchedFilter.addAction("android.intent.action.USER_SWITCHED");
            UserManagerService.this.mContext.registerReceiver(userSwitchedReceiver, userSwitchedFilter, null, UserManagerService.this.mHandler);
            ActivityManager am = (ActivityManager) UserManagerService.this.mContext.getSystemService("activity");
            am.switchUser(0);
        }

        public void onEphemeralUserStop(int userId) {
            synchronized (UserManagerService.this.mUsersLock) {
                UserInfo userInfo = UserManagerService.this.getUserInfoLU(userId);
                if (userInfo != null && userInfo.isEphemeral()) {
                    userInfo.flags |= 64;
                    if (userInfo.isGuest()) {
                        userInfo.guestToRemove = true;
                    }
                }
            }
        }

        public UserInfo createUserEvenWhenDisallowed(String name, int flags) {
            UserInfo user = UserManagerService.this.createUserInternalUnchecked(name, flags, -10000);
            if (user != null && !user.isAdmin()) {
                UserManagerService.this.setUserRestriction("no_sms", true, user.id);
                UserManagerService.this.setUserRestriction("no_outgoing_calls", true, user.id);
            }
            return user;
        }

        public boolean isUserRunning(int userId) {
            boolean z;
            synchronized (UserManagerService.this.mUserStates) {
                z = UserManagerService.this.mUserStates.get(userId, -1) >= 0;
            }
            return z;
        }

        public void setUserState(int userId, int userState) {
            synchronized (UserManagerService.this.mUserStates) {
                UserManagerService.this.mUserStates.put(userId, userState);
            }
        }

        public void removeUserState(int userId) {
            synchronized (UserManagerService.this.mUserStates) {
                UserManagerService.this.mUserStates.delete(userId);
            }
        }

        public boolean isUserUnlockingOrUnlocked(int userId) {
            boolean z = true;
            synchronized (UserManagerService.this.mUserStates) {
                int state = UserManagerService.this.mUserStates.get(userId, -1);
                if (state != 2 && state != 3) {
                    z = false;
                }
            }
            return z;
        }
    }

    private void removeNonSystemUsers() {
        ArrayList<UserInfo> usersToRemove = new ArrayList<>();
        synchronized (this.mUsersLock) {
            int userSize = this.mUsers.size();
            for (int i = 0; i < userSize; i++) {
                UserInfo ui = this.mUsers.valueAt(i).info;
                if (ui.id != 0) {
                    usersToRemove.add(ui);
                }
            }
        }
        Iterator ui$iterator = usersToRemove.iterator();
        while (ui$iterator.hasNext()) {
            removeUser(((UserInfo) ui$iterator.next()).id);
        }
    }

    private class Shell extends ShellCommand {
        Shell(UserManagerService this$0, Shell shell) {
            this();
        }

        private Shell() {
        }

        public int onCommand(String cmd) {
            return UserManagerService.this.onShellCommand(this, cmd);
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("User manager (user) commands:");
            pw.println("  help");
            pw.println("    Print this help text.");
            pw.println("");
            pw.println("  list");
            pw.println("    Prints all users on the system.");
        }
    }

    private static void debug(String message) {
        Log.d(LOG_TAG, message + "");
    }
}
