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
import android.content.pm.ShortcutServiceInternal;
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
import android.os.IProgressListener;
import android.os.IUserManager;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.security.GateKeeper;
import android.service.gatekeeper.IGateKeeperService;
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
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IAppOpsService;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.LocalServices;
import com.android.server.LockGuard;
import com.android.server.SystemService;
import com.android.server.am.UserState;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.storage.DeviceStorageMonitorInternal;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class UserManagerService extends IUserManager.Stub {
    private static final int ALLOWED_FLAGS_FOR_CREATE_USERS_PERMISSION = 812;
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
    private static final String ATTR_PROFILE_BADGE = "profileBadge";
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

    @VisibleForTesting
    static final int MAX_MANAGED_PROFILES = 1;

    @VisibleForTesting
    static final int MAX_RECENTLY_REMOVED_IDS_SIZE = 100;

    @VisibleForTesting
    static final int MAX_USER_ID = 21474;

    @VisibleForTesting
    static final int MIN_USER_ID = 10;
    private static final boolean RELEASE_DELETED_USER_ID = false;
    private static final String RESTRICTIONS_FILE_PREFIX = "res_";
    private static final String TAG_ACCOUNT = "account";
    private static final String TAG_DEVICE_OWNER_USER_ID = "deviceOwnerUserId";
    private static final String TAG_DEVICE_POLICY_GLOBAL_RESTRICTIONS = "device_policy_global_restrictions";
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
    private static final String TRON_DEMO_CREATED = "users_demo_created";
    private static final String TRON_GUEST_CREATED = "users_guest_created";
    private static final String TRON_USER_CREATED = "users_user_created";
    private static final String USER_LIST_FILENAME = "userlist.xml";
    private static final String USER_PHOTO_FILENAME = "photo.png";
    private static final String USER_PHOTO_FILENAME_TMP = "photo.png.tmp";
    private static final int USER_VERSION = 7;
    static final int WRITE_USER_DELAY = 2000;
    static final int WRITE_USER_MSG = 1;
    private static final String XML_SUFFIX = ".xml";
    private static UserManagerService sInstance;
    private final String ACTION_DISABLE_QUIET_MODE_AFTER_UNLOCK;
    private IAppOpsService mAppOpsService;
    private final Object mAppRestrictionsLock;

    @GuardedBy("mRestrictionsLock")
    private final SparseArray<Bundle> mAppliedUserRestrictions;

    @GuardedBy("mRestrictionsLock")
    private final SparseArray<Bundle> mBaseUserRestrictions;

    @GuardedBy("mRestrictionsLock")
    private final SparseArray<Bundle> mCachedEffectiveUserRestrictions;
    private final Context mContext;

    @GuardedBy("mRestrictionsLock")
    private int mDeviceOwnerUserId;

    @GuardedBy("mRestrictionsLock")
    private final SparseArray<Bundle> mDevicePolicyGlobalUserRestrictions;

    @GuardedBy("mRestrictionsLock")
    private final SparseArray<Bundle> mDevicePolicyLocalUserRestrictions;
    private final BroadcastReceiver mDisableQuietModeCallback;

    @GuardedBy("mUsersLock")
    private boolean mForceEphemeralUsers;

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
    private final LinkedList<Integer> mRecentlyRemovedIds;

    @GuardedBy("mUsersLock")
    private final SparseBooleanArray mRemovingUserIds;
    private final Object mRestrictionsLock;
    private final UserDataPreparer mUserDataPreparer;

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

    @VisibleForTesting
    static class UserData {
        String account;
        UserInfo info;
        boolean persistSeedData;
        String seedAccountName;
        PersistableBundle seedAccountOptions;
        String seedAccountType;
        long startRealtime;
        long unlockRealtime;

        UserData() {
        }

        void clearSeedAccountData() {
            this.seedAccountName = null;
            this.seedAccountType = null;
            this.seedAccountOptions = null;
            this.persistSeedData = false;
        }
    }

    class AnonymousClass1 extends BroadcastReceiver {
        AnonymousClass1() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!"com.android.server.pm.DISABLE_QUIET_MODE_AFTER_UNLOCK".equals(intent.getAction())) {
                return;
            }
            final IntentSender intentSender = (IntentSender) intent.getParcelableExtra("android.intent.extra.INTENT");
            final int intExtra = intent.getIntExtra("android.intent.extra.USER_ID", -10000);
            BackgroundThread.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    UserManagerService.this.setQuietModeEnabled(intExtra, false, intentSender);
                }
            });
        }
    }

    private class DisableQuietModeUserUnlockedCallback extends IProgressListener.Stub {
        private final IntentSender mTarget;

        public DisableQuietModeUserUnlockedCallback(IntentSender intentSender) {
            Preconditions.checkNotNull(intentSender);
            this.mTarget = intentSender;
        }

        public void onStarted(int i, Bundle bundle) {
        }

        public void onProgress(int i, int i2, Bundle bundle) {
        }

        public void onFinished(int i, Bundle bundle) {
            try {
                UserManagerService.this.mContext.startIntentSender(this.mTarget, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                Slog.e(UserManagerService.LOG_TAG, "Failed to start the target in the callback", e);
            }
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
        public void onBootPhase(int i) {
            if (i == 550) {
                this.mUms.cleanupPartialUsers();
            }
        }

        @Override
        public void onStartUser(int i) {
            synchronized (this.mUms.mUsersLock) {
                UserData userDataLU = this.mUms.getUserDataLU(i);
                if (userDataLU != null) {
                    userDataLU.startRealtime = SystemClock.elapsedRealtime();
                }
            }
        }

        @Override
        public void onUnlockUser(int i) {
            synchronized (this.mUms.mUsersLock) {
                UserData userDataLU = this.mUms.getUserDataLU(i);
                if (userDataLU != null) {
                    userDataLU.unlockRealtime = SystemClock.elapsedRealtime();
                }
            }
        }

        @Override
        public void onStopUser(int i) {
            synchronized (this.mUms.mUsersLock) {
                UserData userDataLU = this.mUms.getUserDataLU(i);
                if (userDataLU != null) {
                    userDataLU.startRealtime = 0L;
                    userDataLU.unlockRealtime = 0L;
                }
            }
        }
    }

    @VisibleForTesting
    UserManagerService(Context context) {
        this(context, null, null, new Object(), context.getCacheDir());
    }

    UserManagerService(Context context, PackageManagerService packageManagerService, UserDataPreparer userDataPreparer, Object obj) {
        this(context, packageManagerService, userDataPreparer, obj, Environment.getDataDirectory());
    }

    private UserManagerService(Context context, PackageManagerService packageManagerService, UserDataPreparer userDataPreparer, Object obj, File file) {
        this.mUsersLock = LockGuard.installNewLock(2);
        this.mRestrictionsLock = new Object();
        this.mAppRestrictionsLock = new Object();
        this.mUsers = new SparseArray<>();
        this.mBaseUserRestrictions = new SparseArray<>();
        this.mCachedEffectiveUserRestrictions = new SparseArray<>();
        this.mAppliedUserRestrictions = new SparseArray<>();
        this.mDevicePolicyGlobalUserRestrictions = new SparseArray<>();
        this.mDeviceOwnerUserId = -10000;
        this.mDevicePolicyLocalUserRestrictions = new SparseArray<>();
        this.mGuestRestrictions = new Bundle();
        this.mRemovingUserIds = new SparseBooleanArray();
        this.mRecentlyRemovedIds = new LinkedList<>();
        this.mUserVersion = 0;
        this.mIsUserManaged = new SparseBooleanArray();
        this.mUserRestrictionsListeners = new ArrayList<>();
        this.ACTION_DISABLE_QUIET_MODE_AFTER_UNLOCK = "com.android.server.pm.DISABLE_QUIET_MODE_AFTER_UNLOCK";
        this.mDisableQuietModeCallback = new AnonymousClass1();
        this.mUserStates = new SparseIntArray();
        this.mContext = context;
        this.mPm = packageManagerService;
        this.mPackagesLock = obj;
        this.mHandler = new MainHandler();
        this.mUserDataPreparer = userDataPreparer;
        synchronized (this.mPackagesLock) {
            this.mUsersDir = new File(file, USER_INFO_DIR);
            this.mUsersDir.mkdirs();
            new File(this.mUsersDir, String.valueOf(0)).mkdirs();
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
        UserInfo userInfoFindCurrentGuestUser = findCurrentGuestUser();
        if (userInfoFindCurrentGuestUser != null && !hasUserRestriction("no_config_wifi", userInfoFindCurrentGuestUser.id)) {
            setUserRestriction("no_config_wifi", true, userInfoFindCurrentGuestUser.id);
        }
        this.mContext.registerReceiver(this.mDisableQuietModeCallback, new IntentFilter("com.android.server.pm.DISABLE_QUIET_MODE_AFTER_UNLOCK"), null, this.mHandler);
    }

    void cleanupPartialUsers() {
        int i;
        ArrayList arrayList = new ArrayList();
        synchronized (this.mUsersLock) {
            int size = this.mUsers.size();
            for (int i2 = 0; i2 < size; i2++) {
                UserInfo userInfo = this.mUsers.valueAt(i2).info;
                if ((userInfo.partial || userInfo.guestToRemove || userInfo.isEphemeral()) && i2 != 0) {
                    arrayList.add(userInfo);
                    addRemovingUserIdLocked(userInfo.id);
                    userInfo.partial = true;
                }
            }
        }
        int size2 = arrayList.size();
        for (i = 0; i < size2; i++) {
            UserInfo userInfo2 = (UserInfo) arrayList.get(i);
            Slog.w(LOG_TAG, "Removing partially created user " + userInfo2.id + " (name=" + userInfo2.name + ")");
            removeUserState(userInfo2.id);
        }
    }

    public String getUserAccount(int i) {
        String str;
        checkManageUserAndAcrossUsersFullPermission("get user account");
        synchronized (this.mUsersLock) {
            str = this.mUsers.get(i).account;
        }
        return str;
    }

    public void setUserAccount(int i, String str) {
        checkManageUserAndAcrossUsersFullPermission("set user account");
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                UserData userData = this.mUsers.get(i);
                if (userData == null) {
                    Slog.e(LOG_TAG, "User not found for setting user account: u" + i);
                    return;
                }
                if (!Objects.equals(userData.account, str)) {
                    userData.account = str;
                } else {
                    userData = null;
                }
                if (userData != null) {
                    writeUserLP(userData);
                }
            }
        }
    }

    public UserInfo getPrimaryUser() {
        checkManageUsersPermission("query users");
        synchronized (this.mUsersLock) {
            int size = this.mUsers.size();
            for (int i = 0; i < size; i++) {
                UserInfo userInfo = this.mUsers.valueAt(i).info;
                if (userInfo.isPrimary() && !this.mRemovingUserIds.get(userInfo.id)) {
                    return userInfo;
                }
            }
            return null;
        }
    }

    public List<UserInfo> getUsers(boolean z) {
        ArrayList arrayList;
        checkManageOrCreateUsersPermission("query users");
        synchronized (this.mUsersLock) {
            arrayList = new ArrayList(this.mUsers.size());
            int size = this.mUsers.size();
            for (int i = 0; i < size; i++) {
                UserInfo userInfo = this.mUsers.valueAt(i).info;
                if (!userInfo.partial && (!z || !this.mRemovingUserIds.get(userInfo.id))) {
                    arrayList.add(userWithName(userInfo));
                }
            }
        }
        return arrayList;
    }

    public List<UserInfo> getProfiles(int i, boolean z) {
        boolean zHasManageUsersPermission;
        List<UserInfo> profilesLU;
        if (i != UserHandle.getCallingUserId()) {
            checkManageOrCreateUsersPermission("getting profiles related to user " + i);
            zHasManageUsersPermission = true;
        } else {
            zHasManageUsersPermission = hasManageUsersPermission();
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mUsersLock) {
                profilesLU = getProfilesLU(i, z, zHasManageUsersPermission);
            }
            return profilesLU;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public int[] getProfileIds(int i, boolean z) {
        int[] array;
        if (i != UserHandle.getCallingUserId()) {
            checkManageOrCreateUsersPermission("getting profiles related to user " + i);
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mUsersLock) {
                array = getProfileIdsLU(i, z).toArray();
            }
            return array;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private List<UserInfo> getProfilesLU(int i, boolean z, boolean z2) {
        UserInfo userInfoUserWithName;
        IntArray profileIdsLU = getProfileIdsLU(i, z);
        ArrayList arrayList = new ArrayList(profileIdsLU.size());
        for (int i2 = 0; i2 < profileIdsLU.size(); i2++) {
            UserInfo userInfo = this.mUsers.get(profileIdsLU.get(i2)).info;
            if (!z2) {
                userInfoUserWithName = new UserInfo(userInfo);
                userInfoUserWithName.name = null;
                userInfoUserWithName.iconPath = null;
            } else {
                userInfoUserWithName = userWithName(userInfo);
            }
            arrayList.add(userInfoUserWithName);
        }
        return arrayList;
    }

    private IntArray getProfileIdsLU(int i, boolean z) {
        UserInfo userInfoLU = getUserInfoLU(i);
        IntArray intArray = new IntArray(this.mUsers.size());
        if (userInfoLU == null) {
            return intArray;
        }
        int size = this.mUsers.size();
        for (int i2 = 0; i2 < size; i2++) {
            UserInfo userInfo = this.mUsers.valueAt(i2).info;
            if (isProfileOf(userInfoLU, userInfo) && ((!z || userInfo.isEnabled()) && !this.mRemovingUserIds.get(userInfo.id) && !userInfo.partial)) {
                intArray.add(userInfo.id);
            }
        }
        return intArray;
    }

    public int getCredentialOwnerProfile(int i) {
        checkManageUsersPermission("get the credential owner");
        if (!this.mLockPatternUtils.isSeparateProfileChallengeEnabled(i)) {
            synchronized (this.mUsersLock) {
                UserInfo profileParentLU = getProfileParentLU(i);
                if (profileParentLU != null) {
                    return profileParentLU.id;
                }
            }
        }
        return i;
    }

    public boolean isSameProfileGroup(int i, int i2) {
        if (i == i2) {
            return true;
        }
        checkManageUsersPermission("check if in the same profile group");
        return isSameProfileGroupNoChecks(i, i2);
    }

    private boolean isSameProfileGroupNoChecks(int i, int i2) {
        synchronized (this.mUsersLock) {
            UserInfo userInfoLU = getUserInfoLU(i);
            if (userInfoLU != null && userInfoLU.profileGroupId != -10000) {
                UserInfo userInfoLU2 = getUserInfoLU(i2);
                if (userInfoLU2 != null && userInfoLU2.profileGroupId != -10000) {
                    return userInfoLU.profileGroupId == userInfoLU2.profileGroupId;
                }
                return false;
            }
            return false;
        }
    }

    public UserInfo getProfileParent(int i) {
        UserInfo profileParentLU;
        checkManageUsersPermission("get the profile parent");
        synchronized (this.mUsersLock) {
            profileParentLU = getProfileParentLU(i);
        }
        return profileParentLU;
    }

    public int getProfileParentId(int i) {
        checkManageUsersPermission("get the profile parent");
        return this.mLocalService.getProfileParentId(i);
    }

    private UserInfo getProfileParentLU(int i) {
        int i2;
        UserInfo userInfoLU = getUserInfoLU(i);
        if (userInfoLU == null || (i2 = userInfoLU.profileGroupId) == i || i2 == -10000) {
            return null;
        }
        return getUserInfoLU(i2);
    }

    private static boolean isProfileOf(UserInfo userInfo, UserInfo userInfo2) {
        return userInfo.id == userInfo2.id || (userInfo.profileGroupId != -10000 && userInfo.profileGroupId == userInfo2.profileGroupId);
    }

    private void broadcastProfileAvailabilityChanges(UserHandle userHandle, UserHandle userHandle2, boolean z) {
        Intent intent = new Intent();
        if (z) {
            intent.setAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
        } else {
            intent.setAction("android.intent.action.MANAGED_PROFILE_AVAILABLE");
        }
        intent.putExtra("android.intent.extra.QUIET_MODE", z);
        intent.putExtra("android.intent.extra.USER", userHandle);
        intent.putExtra("android.intent.extra.user_handle", userHandle.getIdentifier());
        intent.addFlags(1073741824);
        this.mContext.sendBroadcastAsUser(intent, userHandle2);
    }

    public boolean requestQuietModeEnabled(String str, boolean z, int i, IntentSender intentSender) {
        Preconditions.checkNotNull(str);
        if (z && intentSender != null) {
            throw new IllegalArgumentException("target should only be specified when we are disabling quiet mode.");
        }
        ensureCanModifyQuietMode(str, Binder.getCallingUid(), intentSender != null);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (z) {
                setQuietModeEnabled(i, true, intentSender);
                return true;
            }
            if (this.mLockPatternUtils.isSecure(i) && !StorageManager.isUserKeyUnlocked(i)) {
                showConfirmCredentialToDisableQuietMode(i, intentSender);
                return false;
            }
            setQuietModeEnabled(i, false, intentSender);
            return true;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void ensureCanModifyQuietMode(String str, int i, boolean z) {
        if (hasManageUsersPermission()) {
            return;
        }
        if (z) {
            throw new SecurityException("MANAGE_USERS permission is required to start intent after disabling quiet mode.");
        }
        if (hasPermissionGranted("android.permission.MODIFY_QUIET_MODE", i)) {
            return;
        }
        verifyCallingPackage(str, i);
        ShortcutServiceInternal shortcutServiceInternal = (ShortcutServiceInternal) LocalServices.getService(ShortcutServiceInternal.class);
        if (shortcutServiceInternal != null && shortcutServiceInternal.isForegroundDefaultLauncher(str, i)) {
        } else {
            throw new SecurityException("Can't modify quiet mode, caller is neither foreground default launcher nor has MANAGE_USERS/MODIFY_QUIET_MODE permission");
        }
    }

    private void setQuietModeEnabled(int i, boolean z, IntentSender intentSender) {
        synchronized (this.mUsersLock) {
            UserInfo userInfoLU = getUserInfoLU(i);
            UserInfo profileParentLU = getProfileParentLU(i);
            if (userInfoLU == null || !userInfoLU.isManagedProfile()) {
                throw new IllegalArgumentException("User " + i + " is not a profile");
            }
            if (userInfoLU.isQuietModeEnabled() == z) {
                Slog.i(LOG_TAG, "Quiet mode is already " + z);
                return;
            }
            userInfoLU.flags ^= 128;
            UserData userDataLU = getUserDataLU(userInfoLU.id);
            synchronized (this.mPackagesLock) {
                writeUserLP(userDataLU);
            }
            DisableQuietModeUserUnlockedCallback disableQuietModeUserUnlockedCallback = null;
            try {
                if (z) {
                    ActivityManager.getService().stopUser(i, true, (IStopUserCallback) null);
                    ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).killForegroundAppsForUser(i);
                } else {
                    if (intentSender != null) {
                        disableQuietModeUserUnlockedCallback = new DisableQuietModeUserUnlockedCallback(intentSender);
                    }
                    ActivityManager.getService().startUserInBackgroundWithListener(i, disableQuietModeUserUnlockedCallback);
                }
            } catch (RemoteException e) {
                e.rethrowAsRuntimeException();
            }
            broadcastProfileAvailabilityChanges(userInfoLU.getUserHandle(), profileParentLU.getUserHandle(), z);
        }
    }

    public boolean isQuietModeEnabled(int i) {
        UserInfo userInfoLU;
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                userInfoLU = getUserInfoLU(i);
            }
            if (userInfoLU != null && userInfoLU.isManagedProfile()) {
                return userInfoLU.isQuietModeEnabled();
            }
            return false;
        }
    }

    private void showConfirmCredentialToDisableQuietMode(int i, IntentSender intentSender) {
        Intent intentCreateConfirmDeviceCredentialIntent = ((KeyguardManager) this.mContext.getSystemService("keyguard")).createConfirmDeviceCredentialIntent(null, null, i);
        if (intentCreateConfirmDeviceCredentialIntent == null) {
            return;
        }
        Intent intent = new Intent("com.android.server.pm.DISABLE_QUIET_MODE_AFTER_UNLOCK");
        if (intentSender != null) {
            intent.putExtra("android.intent.extra.INTENT", intentSender);
        }
        intent.putExtra("android.intent.extra.USER_ID", i);
        intent.setPackage(this.mContext.getPackageName());
        intent.addFlags(268435456);
        intentCreateConfirmDeviceCredentialIntent.putExtra("android.intent.extra.INTENT", PendingIntent.getBroadcast(this.mContext, 0, intent, 1409286144).getIntentSender());
        intentCreateConfirmDeviceCredentialIntent.setFlags(276824064);
        this.mContext.startActivity(intentCreateConfirmDeviceCredentialIntent);
    }

    public void setUserEnabled(int i) {
        UserInfo userInfoLU;
        checkManageUsersPermission("enable user");
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                userInfoLU = getUserInfoLU(i);
            }
            if (userInfoLU != null && !userInfoLU.isEnabled()) {
                userInfoLU.flags ^= 64;
                writeUserLP(getUserDataLU(userInfoLU.id));
            }
        }
    }

    public void setUserAdmin(int i) {
        UserInfo userInfoLU;
        checkManageUserAndAcrossUsersFullPermission("set user admin");
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                userInfoLU = getUserInfoLU(i);
            }
            if (userInfoLU != null && !userInfoLU.isAdmin()) {
                userInfoLU.flags ^= 2;
                writeUserLP(getUserDataLU(userInfoLU.id));
                setUserRestriction("no_sms", false, i);
                setUserRestriction("no_outgoing_calls", false, i);
            }
        }
    }

    public void evictCredentialEncryptionKey(int i) {
        checkManageUsersPermission("evict CE key");
        IActivityManager iActivityManager = ActivityManagerNative.getDefault();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                iActivityManager.restartUserInBackground(i);
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public UserInfo getUserInfo(int i) {
        UserInfo userInfoUserWithName;
        checkManageOrCreateUsersPermission("query user");
        synchronized (this.mUsersLock) {
            userInfoUserWithName = userWithName(getUserInfoLU(i));
        }
        return userInfoUserWithName;
    }

    private UserInfo userWithName(UserInfo userInfo) {
        if (userInfo != null && userInfo.name == null && userInfo.id == 0) {
            UserInfo userInfo2 = new UserInfo(userInfo);
            userInfo2.name = getOwnerName();
            return userInfo2;
        }
        return userInfo;
    }

    public int getManagedProfileBadge(int i) {
        int i2;
        int callingUserId = UserHandle.getCallingUserId();
        if (callingUserId != i && !hasManageUsersPermission() && !isSameProfileGroupNoChecks(callingUserId, i)) {
            throw new SecurityException("You need MANAGE_USERS permission to: check if specified user a managed profile outside your profile group");
        }
        synchronized (this.mUsersLock) {
            UserInfo userInfoLU = getUserInfoLU(i);
            i2 = userInfoLU != null ? userInfoLU.profileBadge : 0;
        }
        return i2;
    }

    public boolean isManagedProfile(int i) {
        boolean z;
        int callingUserId = UserHandle.getCallingUserId();
        if (callingUserId != i && !hasManageUsersPermission() && !isSameProfileGroupNoChecks(callingUserId, i)) {
            throw new SecurityException("You need MANAGE_USERS permission to: check if specified user a managed profile outside your profile group");
        }
        synchronized (this.mUsersLock) {
            UserInfo userInfoLU = getUserInfoLU(i);
            z = userInfoLU != null && userInfoLU.isManagedProfile();
        }
        return z;
    }

    public boolean isUserUnlockingOrUnlocked(int i) {
        checkManageOrInteractPermIfCallerInOtherProfileGroup(i, "isUserUnlockingOrUnlocked");
        return this.mLocalService.isUserUnlockingOrUnlocked(i);
    }

    public boolean isUserUnlocked(int i) {
        checkManageOrInteractPermIfCallerInOtherProfileGroup(i, "isUserUnlocked");
        return this.mLocalService.isUserUnlocked(i);
    }

    public boolean isUserRunning(int i) {
        checkManageOrInteractPermIfCallerInOtherProfileGroup(i, "isUserRunning");
        return this.mLocalService.isUserRunning(i);
    }

    public long getUserStartRealtime() {
        int userId = UserHandle.getUserId(Binder.getCallingUid());
        synchronized (this.mUsersLock) {
            UserData userDataLU = getUserDataLU(userId);
            if (userDataLU != null) {
                return userDataLU.startRealtime;
            }
            return 0L;
        }
    }

    public long getUserUnlockRealtime() {
        synchronized (this.mUsersLock) {
            UserData userDataLU = getUserDataLU(UserHandle.getUserId(Binder.getCallingUid()));
            if (userDataLU != null) {
                return userDataLU.unlockRealtime;
            }
            return 0L;
        }
    }

    private void checkManageOrInteractPermIfCallerInOtherProfileGroup(int i, String str) {
        int callingUserId = UserHandle.getCallingUserId();
        if (callingUserId != i && !isSameProfileGroupNoChecks(callingUserId, i) && !hasManageUsersPermission() && !hasPermissionGranted("android.permission.INTERACT_ACROSS_USERS", Binder.getCallingUid())) {
            throw new SecurityException("You need INTERACT_ACROSS_USERS or MANAGE_USERS permission to: check " + str);
        }
    }

    public boolean isDemoUser(int i) {
        boolean z;
        if (UserHandle.getCallingUserId() != i && !hasManageUsersPermission()) {
            throw new SecurityException("You need MANAGE_USERS permission to query if u=" + i + " is a demo user");
        }
        synchronized (this.mUsersLock) {
            UserInfo userInfoLU = getUserInfoLU(i);
            z = userInfoLU != null && userInfoLU.isDemo();
        }
        return z;
    }

    public boolean isRestricted() {
        boolean zIsRestricted;
        synchronized (this.mUsersLock) {
            zIsRestricted = getUserInfoLU(UserHandle.getCallingUserId()).isRestricted();
        }
        return zIsRestricted;
    }

    public boolean canHaveRestrictedProfile(int i) {
        checkManageUsersPermission("canHaveRestrictedProfile");
        synchronized (this.mUsersLock) {
            UserInfo userInfoLU = getUserInfoLU(i);
            boolean z = false;
            if (userInfoLU != null && userInfoLU.canHaveProfile()) {
                if (!userInfoLU.isAdmin()) {
                    return false;
                }
                if (!this.mIsDeviceManaged && !this.mIsUserManaged.get(i)) {
                    z = true;
                }
                return z;
            }
            return false;
        }
    }

    public boolean hasRestrictedProfiles() {
        checkManageUsersPermission("hasRestrictedProfiles");
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this.mUsersLock) {
            int size = this.mUsers.size();
            for (int i = 0; i < size; i++) {
                UserInfo userInfo = this.mUsers.valueAt(i).info;
                if (callingUserId != userInfo.id && userInfo.restrictedProfileParentId == callingUserId) {
                    return true;
                }
            }
            return false;
        }
    }

    private UserInfo getUserInfoLU(int i) {
        UserData userData = this.mUsers.get(i);
        if (userData != null && userData.info.partial && !this.mRemovingUserIds.get(i)) {
            Slog.w(LOG_TAG, "getUserInfo: unknown user #" + i);
            return null;
        }
        if (userData != null) {
            return userData.info;
        }
        return null;
    }

    private UserData getUserDataLU(int i) {
        UserData userData = this.mUsers.get(i);
        if (userData != null && userData.info.partial && !this.mRemovingUserIds.get(i)) {
            return null;
        }
        return userData;
    }

    private UserInfo getUserInfoNoChecks(int i) {
        UserInfo userInfo;
        synchronized (this.mUsersLock) {
            UserData userData = this.mUsers.get(i);
            userInfo = userData != null ? userData.info : null;
        }
        return userInfo;
    }

    private UserData getUserDataNoChecks(int i) {
        UserData userData;
        synchronized (this.mUsersLock) {
            userData = this.mUsers.get(i);
        }
        return userData;
    }

    public boolean exists(int i) {
        return this.mLocalService.exists(i);
    }

    public void setUserName(int i, String str) {
        boolean z;
        checkManageUsersPermission("rename users");
        synchronized (this.mPackagesLock) {
            UserData userDataNoChecks = getUserDataNoChecks(i);
            if (userDataNoChecks != null && !userDataNoChecks.info.partial) {
                if (str != null && !str.equals(userDataNoChecks.info.name)) {
                    userDataNoChecks.info.name = str;
                    writeUserLP(userDataNoChecks);
                    z = true;
                } else {
                    z = false;
                }
                if (z) {
                    sendUserInfoChangedBroadcast(i);
                    return;
                }
                return;
            }
            Slog.w(LOG_TAG, "setUserName: unknown user #" + i);
        }
    }

    public void setUserIcon(int i, Bitmap bitmap) {
        checkManageUsersPermission("update users");
        if (hasUserRestriction("no_set_user_icon", i)) {
            Log.w(LOG_TAG, "Cannot set user icon. DISALLOW_SET_USER_ICON is enabled.");
        } else {
            this.mLocalService.setUserIcon(i, bitmap);
        }
    }

    private void sendUserInfoChangedBroadcast(int i) {
        Intent intent = new Intent("android.intent.action.USER_INFO_CHANGED");
        intent.putExtra("android.intent.extra.user_handle", i);
        intent.addFlags(1073741824);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public ParcelFileDescriptor getUserIcon(int i) {
        synchronized (this.mPackagesLock) {
            UserInfo userInfoNoChecks = getUserInfoNoChecks(i);
            if (userInfoNoChecks != null && !userInfoNoChecks.partial) {
                int callingUserId = UserHandle.getCallingUserId();
                int i2 = getUserInfoNoChecks(callingUserId).profileGroupId;
                boolean z = i2 != -10000 && i2 == userInfoNoChecks.profileGroupId;
                if (callingUserId != i && !z) {
                    checkManageUsersPermission("get the icon of a user who is not related");
                }
                if (userInfoNoChecks.iconPath == null) {
                    return null;
                }
                String str = userInfoNoChecks.iconPath;
                try {
                    return ParcelFileDescriptor.open(new File(str), 268435456);
                } catch (FileNotFoundException e) {
                    Log.e(LOG_TAG, "Couldn't find icon file", e);
                    return null;
                }
            }
            Slog.w(LOG_TAG, "getUserIcon: unknown user #" + i);
            return null;
        }
    }

    public void makeInitialized(int i) {
        boolean z;
        checkManageUsersPermission("makeInitialized");
        synchronized (this.mUsersLock) {
            UserData userData = this.mUsers.get(i);
            if (userData != null && !userData.info.partial) {
                if ((userData.info.flags & 16) == 0) {
                    userData.info.flags |= 16;
                    z = true;
                } else {
                    z = false;
                }
                if (z) {
                    scheduleWriteUser(userData);
                    return;
                }
                return;
            }
            Slog.w(LOG_TAG, "makeInitialized: unknown user #" + i);
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

    public void setDefaultGuestRestrictions(Bundle bundle) {
        checkManageUsersPermission("setDefaultGuestRestrictions");
        synchronized (this.mGuestRestrictions) {
            this.mGuestRestrictions.clear();
            this.mGuestRestrictions.putAll(bundle);
        }
        synchronized (this.mPackagesLock) {
            writeUserListLP();
        }
    }

    private void setDevicePolicyUserRestrictionsInner(int i, Bundle bundle, boolean z, int i2) {
        boolean zUpdateRestrictionsIfNeededLR;
        boolean zUpdateRestrictionsIfNeededLR2;
        Bundle bundle2 = new Bundle();
        Bundle bundle3 = new Bundle();
        UserRestrictionsUtils.sortToGlobalAndLocal(bundle, z, i2, bundle2, bundle3);
        synchronized (this.mRestrictionsLock) {
            zUpdateRestrictionsIfNeededLR = updateRestrictionsIfNeededLR(i, bundle2, this.mDevicePolicyGlobalUserRestrictions);
            zUpdateRestrictionsIfNeededLR2 = updateRestrictionsIfNeededLR(i, bundle3, this.mDevicePolicyLocalUserRestrictions);
            if (z) {
                this.mDeviceOwnerUserId = i;
            } else if (this.mDeviceOwnerUserId == i) {
                this.mDeviceOwnerUserId = -10000;
            }
        }
        synchronized (this.mPackagesLock) {
            if (zUpdateRestrictionsIfNeededLR2 || zUpdateRestrictionsIfNeededLR) {
                writeUserLP(getUserDataNoChecks(i));
            }
        }
        synchronized (this.mRestrictionsLock) {
            try {
                if (zUpdateRestrictionsIfNeededLR) {
                    applyUserRestrictionsForAllUsersLR();
                } else if (zUpdateRestrictionsIfNeededLR2) {
                    applyUserRestrictionsLR(i);
                }
            } finally {
            }
        }
    }

    private boolean updateRestrictionsIfNeededLR(int i, Bundle bundle, SparseArray<Bundle> sparseArray) {
        boolean z = !UserRestrictionsUtils.areEqual(sparseArray.get(i), bundle);
        if (z) {
            if (!UserRestrictionsUtils.isEmpty(bundle)) {
                sparseArray.put(i, bundle);
            } else {
                sparseArray.delete(i);
            }
        }
        return z;
    }

    @GuardedBy("mRestrictionsLock")
    private Bundle computeEffectiveUserRestrictionsLR(int i) {
        Bundle bundleNonNull = UserRestrictionsUtils.nonNull(this.mBaseUserRestrictions.get(i));
        Bundle bundleMergeAll = UserRestrictionsUtils.mergeAll(this.mDevicePolicyGlobalUserRestrictions);
        Bundle bundle = this.mDevicePolicyLocalUserRestrictions.get(i);
        if (UserRestrictionsUtils.isEmpty(bundleMergeAll) && UserRestrictionsUtils.isEmpty(bundle)) {
            return bundleNonNull;
        }
        Bundle bundleClone = UserRestrictionsUtils.clone(bundleNonNull);
        UserRestrictionsUtils.merge(bundleClone, bundleMergeAll);
        UserRestrictionsUtils.merge(bundleClone, bundle);
        return bundleClone;
    }

    @GuardedBy("mRestrictionsLock")
    private void invalidateEffectiveUserRestrictionsLR(int i) {
        this.mCachedEffectiveUserRestrictions.remove(i);
    }

    private Bundle getEffectiveUserRestrictions(int i) {
        Bundle bundleComputeEffectiveUserRestrictionsLR;
        synchronized (this.mRestrictionsLock) {
            bundleComputeEffectiveUserRestrictionsLR = this.mCachedEffectiveUserRestrictions.get(i);
            if (bundleComputeEffectiveUserRestrictionsLR == null) {
                bundleComputeEffectiveUserRestrictionsLR = computeEffectiveUserRestrictionsLR(i);
                this.mCachedEffectiveUserRestrictions.put(i, bundleComputeEffectiveUserRestrictionsLR);
            }
        }
        return bundleComputeEffectiveUserRestrictionsLR;
    }

    public boolean hasUserRestriction(String str, int i) {
        Bundle effectiveUserRestrictions;
        return UserRestrictionsUtils.isValidRestriction(str) && (effectiveUserRestrictions = getEffectiveUserRestrictions(i)) != null && effectiveUserRestrictions.getBoolean(str);
    }

    public boolean hasUserRestrictionOnAnyUser(String str) {
        if (!UserRestrictionsUtils.isValidRestriction(str)) {
            return false;
        }
        List<UserInfo> users = getUsers(true);
        for (int i = 0; i < users.size(); i++) {
            Bundle effectiveUserRestrictions = getEffectiveUserRestrictions(users.get(i).id);
            if (effectiveUserRestrictions != null && effectiveUserRestrictions.getBoolean(str)) {
                return true;
            }
        }
        return false;
    }

    public int getUserRestrictionSource(String str, int i) {
        List<UserManager.EnforcingUser> userRestrictionSources = getUserRestrictionSources(str, i);
        int userRestrictionSource = 0;
        for (int size = userRestrictionSources.size() - 1; size >= 0; size--) {
            userRestrictionSource |= userRestrictionSources.get(size).getUserRestrictionSource();
        }
        return userRestrictionSource;
    }

    public List<UserManager.EnforcingUser> getUserRestrictionSources(String str, int i) {
        checkManageUsersPermission("getUserRestrictionSource");
        if (!hasUserRestriction(str, i)) {
            return Collections.emptyList();
        }
        ArrayList arrayList = new ArrayList();
        if (hasBaseUserRestriction(str, i)) {
            arrayList.add(new UserManager.EnforcingUser(-10000, 1));
        }
        synchronized (this.mRestrictionsLock) {
            if (UserRestrictionsUtils.contains(this.mDevicePolicyLocalUserRestrictions.get(i), str)) {
                arrayList.add(getEnforcingUserLocked(i));
            }
            for (int size = this.mDevicePolicyGlobalUserRestrictions.size() - 1; size >= 0; size--) {
                Bundle bundleValueAt = this.mDevicePolicyGlobalUserRestrictions.valueAt(size);
                int iKeyAt = this.mDevicePolicyGlobalUserRestrictions.keyAt(size);
                if (UserRestrictionsUtils.contains(bundleValueAt, str)) {
                    arrayList.add(getEnforcingUserLocked(iKeyAt));
                }
            }
        }
        return arrayList;
    }

    @GuardedBy("mRestrictionsLock")
    private UserManager.EnforcingUser getEnforcingUserLocked(int i) {
        return new UserManager.EnforcingUser(i, this.mDeviceOwnerUserId == i ? 2 : 4);
    }

    public Bundle getUserRestrictions(int i) {
        return UserRestrictionsUtils.clone(getEffectiveUserRestrictions(i));
    }

    public boolean hasBaseUserRestriction(String str, int i) {
        checkManageUsersPermission("hasBaseUserRestriction");
        boolean z = false;
        if (!UserRestrictionsUtils.isValidRestriction(str)) {
            return false;
        }
        synchronized (this.mRestrictionsLock) {
            Bundle bundle = this.mBaseUserRestrictions.get(i);
            if (bundle != null && bundle.getBoolean(str, false)) {
                z = true;
            }
        }
        return z;
    }

    public void setUserRestriction(String str, boolean z, int i) {
        checkManageUsersPermission("setUserRestriction");
        if (!UserRestrictionsUtils.isValidRestriction(str)) {
            return;
        }
        synchronized (this.mRestrictionsLock) {
            Bundle bundleClone = UserRestrictionsUtils.clone(this.mBaseUserRestrictions.get(i));
            bundleClone.putBoolean(str, z);
            updateUserRestrictionsInternalLR(bundleClone, i);
        }
    }

    @GuardedBy("mRestrictionsLock")
    private void updateUserRestrictionsInternalLR(Bundle bundle, final int i) {
        Bundle bundleNonNull = UserRestrictionsUtils.nonNull(this.mAppliedUserRestrictions.get(i));
        if (bundle != null) {
            Preconditions.checkState(this.mBaseUserRestrictions.get(i) != bundle);
            Preconditions.checkState(this.mCachedEffectiveUserRestrictions.get(i) != bundle);
            if (updateRestrictionsIfNeededLR(i, bundle, this.mBaseUserRestrictions)) {
                scheduleWriteUser(getUserDataNoChecks(i));
            }
        }
        final Bundle bundleComputeEffectiveUserRestrictionsLR = computeEffectiveUserRestrictionsLR(i);
        this.mCachedEffectiveUserRestrictions.put(i, bundleComputeEffectiveUserRestrictionsLR);
        if (this.mAppOpsService != null) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        UserManagerService.this.mAppOpsService.setUserRestrictions(bundleComputeEffectiveUserRestrictionsLR, UserManagerService.mUserRestriconToken, i);
                    } catch (RemoteException e) {
                        Log.w(UserManagerService.LOG_TAG, "Unable to notify AppOpsService of UserRestrictions");
                    }
                }
            });
        }
        propagateUserRestrictionsLR(i, bundleComputeEffectiveUserRestrictionsLR, bundleNonNull);
        this.mAppliedUserRestrictions.put(i, new Bundle(bundleComputeEffectiveUserRestrictionsLR));
    }

    private void propagateUserRestrictionsLR(final int i, Bundle bundle, Bundle bundle2) {
        if (UserRestrictionsUtils.areEqual(bundle, bundle2)) {
            return;
        }
        final Bundle bundle3 = new Bundle(bundle);
        final Bundle bundle4 = new Bundle(bundle2);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                UserManagerInternal.UserRestrictionsListener[] userRestrictionsListenerArr;
                UserRestrictionsUtils.applyUserRestrictions(UserManagerService.this.mContext, i, bundle3, bundle4);
                synchronized (UserManagerService.this.mUserRestrictionsListeners) {
                    userRestrictionsListenerArr = new UserManagerInternal.UserRestrictionsListener[UserManagerService.this.mUserRestrictionsListeners.size()];
                    UserManagerService.this.mUserRestrictionsListeners.toArray(userRestrictionsListenerArr);
                }
                for (UserManagerInternal.UserRestrictionsListener userRestrictionsListener : userRestrictionsListenerArr) {
                    userRestrictionsListener.onUserRestrictionsChanged(i, bundle3, bundle4);
                }
                UserManagerService.this.mContext.sendBroadcastAsUser(new Intent("android.os.action.USER_RESTRICTIONS_CHANGED").setFlags(1073741824), UserHandle.of(i));
            }
        });
    }

    void applyUserRestrictionsLR(int i) {
        updateUserRestrictionsInternalLR(null, i);
    }

    @GuardedBy("mRestrictionsLock")
    void applyUserRestrictionsForAllUsersLR() {
        this.mCachedEffectiveUserRestrictions.clear();
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    int[] runningUserIds = ActivityManager.getService().getRunningUserIds();
                    synchronized (UserManagerService.this.mRestrictionsLock) {
                        for (int i : runningUserIds) {
                            UserManagerService.this.applyUserRestrictionsLR(i);
                        }
                    }
                } catch (RemoteException e) {
                    Log.w(UserManagerService.LOG_TAG, "Unable to access ActivityManagerService");
                }
            }
        });
    }

    private boolean isUserLimitReached() {
        int aliveUsersExcludingGuestsCountLU;
        synchronized (this.mUsersLock) {
            aliveUsersExcludingGuestsCountLU = getAliveUsersExcludingGuestsCountLU();
        }
        return aliveUsersExcludingGuestsCountLU >= UserManager.getMaxSupportedUsers();
    }

    public boolean canAddMoreManagedProfiles(int i, boolean z) {
        checkManageUsersPermission("check if more managed profiles can be added.");
        boolean z2 = false;
        if (ActivityManager.isLowRamDeviceStatic() || !this.mContext.getPackageManager().hasSystemFeature("android.software.managed_users")) {
            return false;
        }
        int size = getProfiles(i, false).size() - 1;
        int i2 = (size <= 0 || !z) ? 0 : 1;
        if (size - i2 >= getMaxManagedProfiles()) {
            return false;
        }
        synchronized (this.mUsersLock) {
            UserInfo userInfoLU = getUserInfoLU(i);
            if (userInfoLU != null && userInfoLU.canHaveProfile()) {
                int aliveUsersExcludingGuestsCountLU = getAliveUsersExcludingGuestsCountLU() - i2;
                if (aliveUsersExcludingGuestsCountLU == 1 || aliveUsersExcludingGuestsCountLU < UserManager.getMaxSupportedUsers()) {
                    z2 = true;
                }
                return z2;
            }
            return false;
        }
    }

    private int getAliveUsersExcludingGuestsCountLU() {
        int size = this.mUsers.size();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            UserInfo userInfo = this.mUsers.valueAt(i2).info;
            if (!this.mRemovingUserIds.get(userInfo.id) && !userInfo.isGuest()) {
                i++;
            }
        }
        return i;
    }

    private static final void checkManageUserAndAcrossUsersFullPermission(String str) {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 1000 || callingUid == 0) {
            return;
        }
        if (hasPermissionGranted("android.permission.MANAGE_USERS", callingUid) && hasPermissionGranted("android.permission.INTERACT_ACROSS_USERS_FULL", callingUid)) {
            return;
        }
        throw new SecurityException("You need MANAGE_USERS and INTERACT_ACROSS_USERS_FULL permission to: " + str);
    }

    private static boolean hasPermissionGranted(String str, int i) {
        return ActivityManager.checkComponentPermission(str, i, -1, true) == 0;
    }

    private static final void checkManageUsersPermission(String str) {
        if (!hasManageUsersPermission()) {
            throw new SecurityException("You need MANAGE_USERS permission to: " + str);
        }
    }

    private static final void checkManageOrCreateUsersPermission(String str) {
        if (!hasManageOrCreateUsersPermission()) {
            throw new SecurityException("You either need MANAGE_USERS or CREATE_USERS permission to: " + str);
        }
    }

    private static final void checkManageOrCreateUsersPermission(int i) {
        if ((i & (-813)) == 0) {
            if (!hasManageOrCreateUsersPermission()) {
                throw new SecurityException("You either need MANAGE_USERS or CREATE_USERS permission to create an user with flags: " + i);
            }
            return;
        }
        if (!hasManageUsersPermission()) {
            throw new SecurityException("You need MANAGE_USERS permission to create an user  with flags: " + i);
        }
    }

    private static final boolean hasManageUsersPermission() {
        int callingUid = Binder.getCallingUid();
        return UserHandle.isSameApp(callingUid, 1000) || callingUid == 0 || hasPermissionGranted("android.permission.MANAGE_USERS", callingUid);
    }

    private static final boolean hasManageOrCreateUsersPermission() {
        int callingUid = Binder.getCallingUid();
        return UserHandle.isSameApp(callingUid, 1000) || callingUid == 0 || hasPermissionGranted("android.permission.MANAGE_USERS", callingUid) || hasPermissionGranted("android.permission.CREATE_USERS", callingUid);
    }

    private static void checkSystemOrRoot(String str) {
        int callingUid = Binder.getCallingUid();
        if (!UserHandle.isSameApp(callingUid, 1000) && callingUid != 0) {
            throw new SecurityException("Only system may: " + str);
        }
    }

    private void writeBitmapLP(UserInfo userInfo, Bitmap bitmap) {
        try {
            File file = new File(this.mUsersDir, Integer.toString(userInfo.id));
            File file2 = new File(file, USER_PHOTO_FILENAME);
            File file3 = new File(file, USER_PHOTO_FILENAME_TMP);
            if (!file.exists()) {
                file.mkdir();
                FileUtils.setPermissions(file.getPath(), 505, -1, -1);
            }
            Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.PNG;
            FileOutputStream fileOutputStream = new FileOutputStream(file3);
            if (bitmap.compress(compressFormat, 100, fileOutputStream) && file3.renameTo(file2) && SELinux.restorecon(file2)) {
                userInfo.iconPath = file2.getAbsolutePath();
            }
            try {
                fileOutputStream.close();
            } catch (IOException e) {
            }
            file3.delete();
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

    private void readUserListLP() throws Throwable {
        Throwable th;
        FileInputStream fileInputStreamOpenRead;
        int next;
        if (!this.mUserListFile.exists()) {
            fallbackToSingleUserLP();
            return;
        }
        FileInputStream fileInputStream = null;
        try {
            try {
                fileInputStreamOpenRead = new AtomicFile(this.mUserListFile).openRead();
                try {
                    try {
                        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                        xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                        do {
                            next = xmlPullParserNewPullParser.next();
                            if (next == 2) {
                                break;
                            }
                        } while (next != 1);
                        if (next != 2) {
                            Slog.e(LOG_TAG, "Unable to read user list");
                            fallbackToSingleUserLP();
                            IoUtils.closeQuietly(fileInputStreamOpenRead);
                            return;
                        }
                        this.mNextSerialNumber = -1;
                        if (xmlPullParserNewPullParser.getName().equals("users")) {
                            String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_NEXT_SERIAL_NO);
                            if (attributeValue != null) {
                                this.mNextSerialNumber = Integer.parseInt(attributeValue);
                            }
                            String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_USER_VERSION);
                            if (attributeValue2 != null) {
                                this.mUserVersion = Integer.parseInt(attributeValue2);
                            }
                        }
                        Bundle restrictions = null;
                        while (true) {
                            int next2 = xmlPullParserNewPullParser.next();
                            if (next2 == 1) {
                                updateUserIds();
                                upgradeIfNecessaryLP(restrictions);
                                IoUtils.closeQuietly(fileInputStreamOpenRead);
                                return;
                            }
                            if (next2 == 2) {
                                String name = xmlPullParserNewPullParser.getName();
                                if (name.equals(TAG_USER)) {
                                    UserData userLP = readUserLP(Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_ID)));
                                    if (userLP != null) {
                                        synchronized (this.mUsersLock) {
                                            this.mUsers.put(userLP.info.id, userLP);
                                            if (this.mNextSerialNumber < 0 || this.mNextSerialNumber <= userLP.info.id) {
                                                this.mNextSerialNumber = userLP.info.id + 1;
                                            }
                                        }
                                    }
                                } else if (name.equals(TAG_GUEST_RESTRICTIONS)) {
                                    while (true) {
                                        int next3 = xmlPullParserNewPullParser.next();
                                        if (next3 == 1 || next3 == 3) {
                                            break;
                                        } else if (next3 == 2) {
                                            break;
                                        }
                                    }
                                } else if (name.equals(TAG_DEVICE_OWNER_USER_ID) || name.equals(TAG_GLOBAL_RESTRICTION_OWNER_ID)) {
                                    String attributeValue3 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_ID);
                                    if (attributeValue3 != null) {
                                        this.mDeviceOwnerUserId = Integer.parseInt(attributeValue3);
                                    }
                                } else if (name.equals(TAG_DEVICE_POLICY_RESTRICTIONS)) {
                                    restrictions = UserRestrictionsUtils.readRestrictions(xmlPullParserNewPullParser);
                                }
                            }
                        }
                    } catch (IOException | XmlPullParserException e) {
                        fileInputStream = fileInputStreamOpenRead;
                        fallbackToSingleUserLP();
                        IoUtils.closeQuietly(fileInputStream);
                    }
                } catch (Throwable th2) {
                    th = th2;
                    IoUtils.closeQuietly(fileInputStreamOpenRead);
                    throw th;
                }
            } catch (Throwable th3) {
                FileInputStream fileInputStream2 = fileInputStream;
                th = th3;
                fileInputStreamOpenRead = fileInputStream2;
            }
        } catch (IOException | XmlPullParserException e2) {
        }
    }

    private void upgradeIfNecessaryLP(Bundle bundle) {
        int i = this.mUserVersion;
        int i2 = this.mUserVersion;
        if (i2 < 1) {
            UserData userDataNoChecks = getUserDataNoChecks(0);
            if ("Primary".equals(userDataNoChecks.info.name)) {
                userDataNoChecks.info.name = this.mContext.getResources().getString(R.string.factory_reset_message);
                scheduleWriteUser(userDataNoChecks);
            }
            i2 = 1;
        }
        if (i2 < 2) {
            UserData userDataNoChecks2 = getUserDataNoChecks(0);
            if ((userDataNoChecks2.info.flags & 16) == 0) {
                userDataNoChecks2.info.flags |= 16;
                scheduleWriteUser(userDataNoChecks2);
            }
            i2 = 2;
        }
        if (i2 < 4) {
            i2 = 4;
        }
        if (i2 < 5) {
            initDefaultGuestRestrictions();
            i2 = 5;
        }
        if (i2 < 6) {
            boolean zIsSplitSystemUser = UserManager.isSplitSystemUser();
            synchronized (this.mUsersLock) {
                for (int i3 = 0; i3 < this.mUsers.size(); i3++) {
                    UserData userDataValueAt = this.mUsers.valueAt(i3);
                    if (!zIsSplitSystemUser && userDataValueAt.info.isRestricted() && userDataValueAt.info.restrictedProfileParentId == -10000) {
                        userDataValueAt.info.restrictedProfileParentId = 0;
                        scheduleWriteUser(userDataValueAt);
                    }
                }
            }
            i2 = 6;
        }
        if (i2 < 7) {
            synchronized (this.mRestrictionsLock) {
                if (!UserRestrictionsUtils.isEmpty(bundle) && this.mDeviceOwnerUserId != -10000) {
                    this.mDevicePolicyGlobalUserRestrictions.put(this.mDeviceOwnerUserId, bundle);
                }
                UserRestrictionsUtils.moveRestriction("ensure_verify_apps", this.mDevicePolicyLocalUserRestrictions, this.mDevicePolicyGlobalUserRestrictions);
            }
            i2 = 7;
        }
        if (i2 < 7) {
            Slog.w(LOG_TAG, "User version " + this.mUserVersion + " didn't upgrade as expected to 7");
            return;
        }
        this.mUserVersion = i2;
        if (i < this.mUserVersion) {
            writeUserListLP();
        }
    }

    private void fallbackToSingleUserLP() {
        int i;
        if (!UserManager.isSplitSystemUser()) {
            i = 19;
        } else {
            i = 16;
        }
        UserData userDataPutUserInfo = putUserInfo(new UserInfo(0, (String) null, (String) null, i));
        this.mNextSerialNumber = 10;
        this.mUserVersion = 7;
        Bundle bundle = new Bundle();
        try {
            for (String str : this.mContext.getResources().getStringArray(R.array.config_availableColorModes)) {
                if (UserRestrictionsUtils.isValidRestriction(str)) {
                    bundle.putBoolean(str, true);
                }
            }
        } catch (Resources.NotFoundException e) {
            Log.e(LOG_TAG, "Couldn't find resource: config_defaultFirstUserRestrictions", e);
        }
        if (!bundle.isEmpty()) {
            synchronized (this.mRestrictionsLock) {
                this.mBaseUserRestrictions.append(0, bundle);
            }
        }
        updateUserIds();
        initDefaultGuestRestrictions();
        writeUserLP(userDataPutUserInfo);
        writeUserListLP();
    }

    private String getOwnerName() {
        return this.mContext.getResources().getString(R.string.factory_reset_message);
    }

    private void scheduleWriteUser(UserData userData) {
        if (!this.mHandler.hasMessages(1, userData)) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1, userData), 2000L);
        }
    }

    private void writeUserLP(UserData userData) {
        FileOutputStream fileOutputStreamStartWrite;
        AtomicFile atomicFile = new AtomicFile(new File(this.mUsersDir, userData.info.id + XML_SUFFIX));
        try {
            fileOutputStreamStartWrite = atomicFile.startWrite();
        } catch (Exception e) {
            e = e;
            fileOutputStreamStartWrite = null;
        }
        try {
            writeUserLP(userData, new BufferedOutputStream(fileOutputStreamStartWrite));
            atomicFile.finishWrite(fileOutputStreamStartWrite);
        } catch (Exception e2) {
            e = e2;
            Slog.e(LOG_TAG, "Error writing user info " + userData.info.id, e);
            atomicFile.failWrite(fileOutputStreamStartWrite);
        }
    }

    @VisibleForTesting
    void writeUserLP(UserData userData, OutputStream outputStream) throws XmlPullParserException, IOException {
        XmlSerializer fastXmlSerializer = new FastXmlSerializer();
        fastXmlSerializer.setOutput(outputStream, StandardCharsets.UTF_8.name());
        fastXmlSerializer.startDocument(null, true);
        fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        UserInfo userInfo = userData.info;
        fastXmlSerializer.startTag(null, TAG_USER);
        fastXmlSerializer.attribute(null, ATTR_ID, Integer.toString(userInfo.id));
        fastXmlSerializer.attribute(null, ATTR_SERIAL_NO, Integer.toString(userInfo.serialNumber));
        fastXmlSerializer.attribute(null, ATTR_FLAGS, Integer.toString(userInfo.flags));
        fastXmlSerializer.attribute(null, ATTR_CREATION_TIME, Long.toString(userInfo.creationTime));
        fastXmlSerializer.attribute(null, ATTR_LAST_LOGGED_IN_TIME, Long.toString(userInfo.lastLoggedInTime));
        if (userInfo.lastLoggedInFingerprint != null) {
            fastXmlSerializer.attribute(null, ATTR_LAST_LOGGED_IN_FINGERPRINT, userInfo.lastLoggedInFingerprint);
        }
        if (userInfo.iconPath != null) {
            fastXmlSerializer.attribute(null, ATTR_ICON_PATH, userInfo.iconPath);
        }
        if (userInfo.partial) {
            fastXmlSerializer.attribute(null, ATTR_PARTIAL, "true");
        }
        if (userInfo.guestToRemove) {
            fastXmlSerializer.attribute(null, ATTR_GUEST_TO_REMOVE, "true");
        }
        if (userInfo.profileGroupId != -10000) {
            fastXmlSerializer.attribute(null, ATTR_PROFILE_GROUP_ID, Integer.toString(userInfo.profileGroupId));
        }
        fastXmlSerializer.attribute(null, ATTR_PROFILE_BADGE, Integer.toString(userInfo.profileBadge));
        if (userInfo.restrictedProfileParentId != -10000) {
            fastXmlSerializer.attribute(null, ATTR_RESTRICTED_PROFILE_PARENT_ID, Integer.toString(userInfo.restrictedProfileParentId));
        }
        if (userData.persistSeedData) {
            if (userData.seedAccountName != null) {
                fastXmlSerializer.attribute(null, ATTR_SEED_ACCOUNT_NAME, userData.seedAccountName);
            }
            if (userData.seedAccountType != null) {
                fastXmlSerializer.attribute(null, ATTR_SEED_ACCOUNT_TYPE, userData.seedAccountType);
            }
        }
        if (userInfo.name != null) {
            fastXmlSerializer.startTag(null, "name");
            fastXmlSerializer.text(userInfo.name);
            fastXmlSerializer.endTag(null, "name");
        }
        synchronized (this.mRestrictionsLock) {
            UserRestrictionsUtils.writeRestrictions(fastXmlSerializer, this.mBaseUserRestrictions.get(userInfo.id), TAG_RESTRICTIONS);
            UserRestrictionsUtils.writeRestrictions(fastXmlSerializer, this.mDevicePolicyLocalUserRestrictions.get(userInfo.id), TAG_DEVICE_POLICY_RESTRICTIONS);
            UserRestrictionsUtils.writeRestrictions(fastXmlSerializer, this.mDevicePolicyGlobalUserRestrictions.get(userInfo.id), TAG_DEVICE_POLICY_GLOBAL_RESTRICTIONS);
        }
        if (userData.account != null) {
            fastXmlSerializer.startTag(null, TAG_ACCOUNT);
            fastXmlSerializer.text(userData.account);
            fastXmlSerializer.endTag(null, TAG_ACCOUNT);
        }
        if (userData.persistSeedData && userData.seedAccountOptions != null) {
            fastXmlSerializer.startTag(null, TAG_SEED_ACCOUNT_OPTIONS);
            userData.seedAccountOptions.saveToXml(fastXmlSerializer);
            fastXmlSerializer.endTag(null, TAG_SEED_ACCOUNT_OPTIONS);
        }
        fastXmlSerializer.endTag(null, TAG_USER);
        fastXmlSerializer.endDocument();
    }

    private void writeUserListLP() {
        FileOutputStream fileOutputStreamStartWrite;
        int[] iArr;
        int i;
        AtomicFile atomicFile = new AtomicFile(this.mUserListFile);
        try {
            fileOutputStreamStartWrite = atomicFile.startWrite();
        } catch (Exception e) {
            fileOutputStreamStartWrite = null;
        }
        try {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStreamStartWrite);
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(bufferedOutputStream, StandardCharsets.UTF_8.name());
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
            fastXmlSerializer.startTag(null, TAG_DEVICE_OWNER_USER_ID);
            fastXmlSerializer.attribute(null, ATTR_ID, Integer.toString(this.mDeviceOwnerUserId));
            fastXmlSerializer.endTag(null, TAG_DEVICE_OWNER_USER_ID);
            synchronized (this.mUsersLock) {
                iArr = new int[this.mUsers.size()];
                for (int i2 = 0; i2 < iArr.length; i2++) {
                    iArr[i2] = this.mUsers.valueAt(i2).info.id;
                }
            }
            for (int i3 : iArr) {
                fastXmlSerializer.startTag(null, TAG_USER);
                fastXmlSerializer.attribute(null, ATTR_ID, Integer.toString(i3));
                fastXmlSerializer.endTag(null, TAG_USER);
            }
            fastXmlSerializer.endTag(null, "users");
            fastXmlSerializer.endDocument();
            atomicFile.finishWrite(fileOutputStreamStartWrite);
        } catch (Exception e2) {
            atomicFile.failWrite(fileOutputStreamStartWrite);
            Slog.e(LOG_TAG, "Error writing user list");
        }
    }

    private UserData readUserLP(int i) throws Throwable {
        FileInputStream fileInputStreamOpenRead;
        AutoCloseable autoCloseable;
        AutoCloseable autoCloseable2 = null;
        try {
            try {
                fileInputStreamOpenRead = new AtomicFile(new File(this.mUsersDir, Integer.toString(i) + XML_SUFFIX)).openRead();
                try {
                    UserData userLP = readUserLP(i, fileInputStreamOpenRead);
                    IoUtils.closeQuietly(fileInputStreamOpenRead);
                    return userLP;
                } catch (IOException e) {
                    Slog.e(LOG_TAG, "Error reading user list");
                    IoUtils.closeQuietly(fileInputStreamOpenRead);
                    return null;
                } catch (XmlPullParserException e2) {
                    Slog.e(LOG_TAG, "Error reading user list");
                    IoUtils.closeQuietly(fileInputStreamOpenRead);
                    return null;
                }
            } catch (Throwable th) {
                th = th;
                autoCloseable2 = autoCloseable;
                IoUtils.closeQuietly(autoCloseable2);
                throw th;
            }
        } catch (IOException e3) {
            fileInputStreamOpenRead = null;
        } catch (XmlPullParserException e4) {
            fileInputStreamOpenRead = null;
        } catch (Throwable th2) {
            th = th2;
            IoUtils.closeQuietly(autoCloseable2);
            throw th;
        }
    }

    @VisibleForTesting
    UserData readUserLP(int i, InputStream inputStream) throws XmlPullParserException, IOException {
        int next;
        int i2;
        int intAttribute;
        long longAttribute;
        String str;
        String str2;
        int i3;
        boolean z;
        int i4;
        boolean z2;
        String attributeValue;
        String attributeValue2;
        String str3;
        String str4;
        boolean z3;
        PersistableBundle persistableBundle;
        Bundle bundle;
        Bundle bundle2;
        Bundle bundle3;
        String str5;
        int i5;
        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
        xmlPullParserNewPullParser.setInput(inputStream, StandardCharsets.UTF_8.name());
        do {
            next = xmlPullParserNewPullParser.next();
            if (next == 2) {
                break;
            }
        } while (next != 1);
        if (next != 2) {
            Slog.e(LOG_TAG, "Unable to read user " + i);
            return null;
        }
        int intAttribute2 = -10000;
        long longAttribute2 = 0;
        if (next != 2 || !xmlPullParserNewPullParser.getName().equals(TAG_USER)) {
            i2 = i;
            intAttribute = -10000;
            longAttribute = 0;
            str = null;
            str2 = null;
            i3 = 0;
            z = false;
            i4 = 0;
            z2 = false;
            attributeValue = null;
            attributeValue2 = null;
            str3 = null;
            str4 = null;
            z3 = false;
            persistableBundle = null;
            bundle = null;
            bundle2 = null;
            bundle3 = null;
        } else {
            if (readIntAttribute(xmlPullParserNewPullParser, ATTR_ID, -1) != i) {
                Slog.e(LOG_TAG, "User id does not match the file name");
                return null;
            }
            int intAttribute3 = readIntAttribute(xmlPullParserNewPullParser, ATTR_SERIAL_NO, i);
            int intAttribute4 = readIntAttribute(xmlPullParserNewPullParser, ATTR_FLAGS, 0);
            attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_ICON_PATH);
            longAttribute = readLongAttribute(xmlPullParserNewPullParser, ATTR_CREATION_TIME, 0L);
            longAttribute2 = readLongAttribute(xmlPullParserNewPullParser, ATTR_LAST_LOGGED_IN_TIME, 0L);
            attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_LAST_LOGGED_IN_FINGERPRINT);
            intAttribute = readIntAttribute(xmlPullParserNewPullParser, ATTR_PROFILE_GROUP_ID, -10000);
            int intAttribute5 = readIntAttribute(xmlPullParserNewPullParser, ATTR_PROFILE_BADGE, 0);
            intAttribute2 = readIntAttribute(xmlPullParserNewPullParser, ATTR_RESTRICTED_PROFILE_PARENT_ID, -10000);
            z2 = "true".equals(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_PARTIAL));
            boolean z4 = "true".equals(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_GUEST_TO_REMOVE));
            String attributeValue3 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_SEED_ACCOUNT_NAME);
            String attributeValue4 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_SEED_ACCOUNT_TYPE);
            boolean z5 = (attributeValue3 == null && attributeValue4 == null) ? false : true;
            int depth = xmlPullParserNewPullParser.getDepth();
            boolean z6 = z5;
            String text = null;
            PersistableBundle persistableBundleRestoreFromXml = null;
            String text2 = null;
            Bundle restrictions = null;
            Bundle restrictions2 = null;
            Bundle restrictions3 = null;
            while (true) {
                str5 = attributeValue4;
                int next2 = xmlPullParserNewPullParser.next();
                i5 = intAttribute5;
                if (next2 == 1 || (next2 == 3 && xmlPullParserNewPullParser.getDepth() <= depth)) {
                    break;
                }
                if (next2 != 3 && next2 != 4) {
                    String name = xmlPullParserNewPullParser.getName();
                    if ("name".equals(name)) {
                        if (xmlPullParserNewPullParser.next() == 4) {
                            text2 = xmlPullParserNewPullParser.getText();
                        }
                    } else if (TAG_RESTRICTIONS.equals(name)) {
                        restrictions = UserRestrictionsUtils.readRestrictions(xmlPullParserNewPullParser);
                    } else if (TAG_DEVICE_POLICY_RESTRICTIONS.equals(name)) {
                        restrictions2 = UserRestrictionsUtils.readRestrictions(xmlPullParserNewPullParser);
                    } else if (TAG_DEVICE_POLICY_GLOBAL_RESTRICTIONS.equals(name)) {
                        restrictions3 = UserRestrictionsUtils.readRestrictions(xmlPullParserNewPullParser);
                    } else if (TAG_ACCOUNT.equals(name)) {
                        if (xmlPullParserNewPullParser.next() == 4) {
                            text = xmlPullParserNewPullParser.getText();
                        }
                    } else if (TAG_SEED_ACCOUNT_OPTIONS.equals(name)) {
                        persistableBundleRestoreFromXml = PersistableBundle.restoreFromXml(xmlPullParserNewPullParser);
                        z6 = true;
                    }
                }
                attributeValue4 = str5;
                intAttribute5 = i5;
            }
            str3 = attributeValue3;
            str = text;
            z3 = z6;
            i2 = intAttribute3;
            persistableBundle = persistableBundleRestoreFromXml;
            z = z4;
            i3 = intAttribute4;
            str2 = text2;
            bundle = restrictions;
            bundle2 = restrictions2;
            bundle3 = restrictions3;
            str4 = str5;
            i4 = i5;
        }
        String str6 = str;
        UserInfo userInfo = new UserInfo(i, str2, attributeValue, i3);
        userInfo.serialNumber = i2;
        userInfo.creationTime = longAttribute;
        userInfo.lastLoggedInTime = longAttribute2;
        userInfo.lastLoggedInFingerprint = attributeValue2;
        userInfo.partial = z2;
        userInfo.guestToRemove = z;
        userInfo.profileGroupId = intAttribute;
        userInfo.profileBadge = i4;
        userInfo.restrictedProfileParentId = intAttribute2;
        UserData userData = new UserData();
        userData.info = userInfo;
        userData.account = str6;
        userData.seedAccountName = str3;
        userData.seedAccountType = str4;
        userData.persistSeedData = z3;
        userData.seedAccountOptions = persistableBundle;
        synchronized (this.mRestrictionsLock) {
            Bundle bundle4 = bundle;
            if (bundle4 != null) {
                try {
                    this.mBaseUserRestrictions.put(i, bundle4);
                } catch (Throwable th) {
                    throw th;
                }
            }
            Bundle bundle5 = bundle2;
            if (bundle5 != null) {
                this.mDevicePolicyLocalUserRestrictions.put(i, bundle5);
            }
            Bundle bundle6 = bundle3;
            if (bundle6 != null) {
                this.mDevicePolicyGlobalUserRestrictions.put(i, bundle6);
            }
        }
        return userData;
    }

    private int readIntAttribute(XmlPullParser xmlPullParser, String str, int i) {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        if (attributeValue == null) {
            return i;
        }
        try {
            return Integer.parseInt(attributeValue);
        } catch (NumberFormatException e) {
            return i;
        }
    }

    private long readLongAttribute(XmlPullParser xmlPullParser, String str, long j) {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        if (attributeValue == null) {
            return j;
        }
        try {
            return Long.parseLong(attributeValue);
        } catch (NumberFormatException e) {
            return j;
        }
    }

    private static void cleanAppRestrictionsForPackageLAr(String str, int i) {
        File file = new File(Environment.getUserSystemDirectory(i), packageToRestrictionsFileName(str));
        if (file.exists()) {
            file.delete();
        }
    }

    public UserInfo createProfileForUser(String str, int i, int i2, String[] strArr) {
        checkManageOrCreateUsersPermission(i);
        return createUserInternal(str, i, i2, strArr);
    }

    public UserInfo createProfileForUserEvenWhenDisallowed(String str, int i, int i2, String[] strArr) {
        checkManageOrCreateUsersPermission(i);
        return createUserInternalUnchecked(str, i, i2, strArr);
    }

    public boolean removeUserEvenWhenDisallowed(int i) {
        checkManageOrCreateUsersPermission("Only the system can remove users");
        return removeUserUnchecked(i);
    }

    public UserInfo createUser(String str, int i) {
        checkManageOrCreateUsersPermission(i);
        return createUserInternal(str, i, -10000);
    }

    private UserInfo createUserInternal(String str, int i, int i2) {
        return createUserInternal(str, i, i2, null);
    }

    private UserInfo createUserInternal(String str, int i, int i2, String[] strArr) {
        String str2;
        if ((i & 32) != 0) {
            str2 = "no_add_managed_profile";
        } else {
            str2 = "no_add_user";
        }
        if (hasUserRestriction(str2, UserHandle.getCallingUserId())) {
            Log.w(LOG_TAG, "Cannot add user. " + str2 + " is enabled.");
            return null;
        }
        return createUserInternalUnchecked(str, i, i2, strArr);
    }

    private UserInfo createUserInternalUnchecked(String str, int i, int i2, String[] strArr) throws Throwable {
        UserData userDataLU;
        UserInfo userInfo;
        long jCurrentTimeMillis;
        int i3 = i;
        if (((DeviceStorageMonitorInternal) LocalServices.getService(DeviceStorageMonitorInternal.class)).isMemoryLow()) {
            Log.w(LOG_TAG, "Cannot add user. Not enough space on disk.");
            return null;
        }
        ?? r3 = (i3 & 4) != 0 ? 1 : 0;
        boolean z = (i3 & 32) != 0;
        boolean z2 = (i3 & 8) != 0;
        boolean z3 = (i3 & 512) != 0;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPackagesLock) {
                try {
                    try {
                        if (i2 != -10000) {
                            try {
                                synchronized (this.mUsersLock) {
                                    userDataLU = getUserDataLU(i2);
                                }
                                if (userDataLU == null) {
                                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                                    return null;
                                }
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        } else {
                            userDataLU = null;
                        }
                        if (z && !canAddMoreManagedProfiles(i2, false)) {
                            Log.e(LOG_TAG, "Cannot add more managed profiles for user " + i2);
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                            return null;
                        }
                        if (r3 == 0 && !z && !z3 && isUserLimitReached()) {
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                            return null;
                        }
                        if (r3 != 0 && findCurrentGuestUser() != null) {
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                            return null;
                        }
                        if (z2 && !UserManager.isSplitSystemUser() && i2 != 0) {
                            Log.w(LOG_TAG, "Cannot add restricted profile - parent user must be owner");
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                            return null;
                        }
                        if (z2 && UserManager.isSplitSystemUser()) {
                            if (userDataLU == null) {
                                Log.w(LOG_TAG, "Cannot add restricted profile - parent user must be specified");
                                Binder.restoreCallingIdentity(jClearCallingIdentity);
                                return null;
                            }
                            if (!userDataLU.info.canHaveProfile()) {
                                Log.w(LOG_TAG, "Cannot add restricted profile - profiles cannot be created for the specified parent user id " + i2);
                                Binder.restoreCallingIdentity(jClearCallingIdentity);
                                return null;
                            }
                        }
                        if (UserManager.isSplitSystemUser() && r3 == 0 && !z && getPrimaryUser() == null) {
                            i3 |= 1;
                            synchronized (this.mUsersLock) {
                                if (!this.mIsDeviceManaged) {
                                    i3 |= 2;
                                }
                            }
                        }
                        int nextAvailableId = getNextAvailableId();
                        Environment.getUserSystemDirectory(nextAvailableId).mkdirs();
                        boolean z4 = Resources.getSystem().getBoolean(R.^attr-private.interpolatorY);
                        synchronized (this.mUsersLock) {
                            try {
                                try {
                                    try {
                                        if (r3 == 0 || !z4) {
                                            try {
                                                if (this.mForceEphemeralUsers || (userDataLU != null && userDataLU.info.isEphemeral())) {
                                                }
                                                userInfo = new UserInfo(nextAvailableId, str, (String) null, i3);
                                                int i4 = this.mNextSerialNumber;
                                                this.mNextSerialNumber = i4 + 1;
                                                userInfo.serialNumber = i4;
                                                jCurrentTimeMillis = System.currentTimeMillis();
                                                if (jCurrentTimeMillis <= EPOCH_PLUS_30_YEARS) {
                                                    jCurrentTimeMillis = 0;
                                                }
                                                userInfo.creationTime = jCurrentTimeMillis;
                                                userInfo.partial = true;
                                                userInfo.lastLoggedInFingerprint = Build.FINGERPRINT;
                                                if (z && i2 != -10000) {
                                                    userInfo.profileBadge = getFreeProfileBadgeLU(i2);
                                                }
                                                UserData userData = new UserData();
                                                userData.info = userInfo;
                                                this.mUsers.put(nextAvailableId, userData);
                                                writeUserLP(userData);
                                                writeUserListLP();
                                                if (userDataLU != null) {
                                                    if (z) {
                                                        if (userDataLU.info.profileGroupId == -10000) {
                                                            userDataLU.info.profileGroupId = userDataLU.info.id;
                                                            writeUserLP(userDataLU);
                                                        }
                                                        userInfo.profileGroupId = userDataLU.info.profileGroupId;
                                                    } else if (z2) {
                                                        if (userDataLU.info.restrictedProfileParentId == -10000) {
                                                            userDataLU.info.restrictedProfileParentId = userDataLU.info.id;
                                                            writeUserLP(userDataLU);
                                                        }
                                                        userInfo.restrictedProfileParentId = userDataLU.info.restrictedProfileParentId;
                                                    }
                                                }
                                                ((StorageManager) this.mContext.getSystemService(StorageManager.class)).createUserKey(nextAvailableId, userInfo.serialNumber, userInfo.isEphemeral());
                                                this.mUserDataPreparer.prepareUserData(nextAvailableId, userInfo.serialNumber, 3);
                                                this.mPm.createNewUser(nextAvailableId, strArr);
                                                userInfo.partial = false;
                                                synchronized (this.mPackagesLock) {
                                                    try {
                                                        writeUserLP(userData);
                                                    } catch (Throwable th2) {
                                                        th = th2;
                                                        while (true) {
                                                            try {
                                                                throw th;
                                                            } catch (Throwable th3) {
                                                                th = th3;
                                                            }
                                                        }
                                                    }
                                                }
                                                updateUserIds();
                                                Bundle bundle = new Bundle();
                                                if (r3 != 0) {
                                                    synchronized (this.mGuestRestrictions) {
                                                        bundle.putAll(this.mGuestRestrictions);
                                                    }
                                                }
                                                synchronized (this.mRestrictionsLock) {
                                                    try {
                                                        this.mBaseUserRestrictions.append(nextAvailableId, bundle);
                                                    } catch (Throwable th4) {
                                                        th = th4;
                                                        while (true) {
                                                            try {
                                                                throw th;
                                                            } catch (Throwable th5) {
                                                                th = th5;
                                                            }
                                                        }
                                                    }
                                                }
                                                this.mPm.onNewUserCreated(nextAvailableId);
                                                Intent intent = new Intent("android.intent.action.USER_ADDED");
                                                intent.putExtra("android.intent.extra.user_handle", nextAvailableId);
                                                this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.MANAGE_USERS");
                                                MetricsLogger.count(this.mContext, r3 != 0 ? TRON_GUEST_CREATED : z3 ? TRON_DEMO_CREATED : TRON_USER_CREATED, 1);
                                                Binder.restoreCallingIdentity(jClearCallingIdentity);
                                                return userInfo;
                                            } catch (Throwable th6) {
                                                th = th6;
                                                while (true) {
                                                    try {
                                                        throw th;
                                                    } catch (Throwable th7) {
                                                        th = th7;
                                                    }
                                                }
                                            }
                                        }
                                        ((StorageManager) this.mContext.getSystemService(StorageManager.class)).createUserKey(nextAvailableId, userInfo.serialNumber, userInfo.isEphemeral());
                                        this.mUserDataPreparer.prepareUserData(nextAvailableId, userInfo.serialNumber, 3);
                                        this.mPm.createNewUser(nextAvailableId, strArr);
                                        userInfo.partial = false;
                                        synchronized (this.mPackagesLock) {
                                        }
                                    } catch (Throwable th8) {
                                        th = th8;
                                        r3 = jClearCallingIdentity;
                                        Binder.restoreCallingIdentity(r3);
                                        throw th;
                                    }
                                    writeUserLP(userData);
                                    writeUserListLP();
                                    if (userDataLU != null) {
                                    }
                                } catch (Throwable th9) {
                                    th = th9;
                                    throw th;
                                }
                                userInfo.creationTime = jCurrentTimeMillis;
                                userInfo.partial = true;
                                userInfo.lastLoggedInFingerprint = Build.FINGERPRINT;
                                if (z) {
                                    userInfo.profileBadge = getFreeProfileBadgeLU(i2);
                                }
                                UserData userData2 = new UserData();
                                userData2.info = userInfo;
                                this.mUsers.put(nextAvailableId, userData2);
                            } catch (Throwable th10) {
                                th = th10;
                                while (true) {
                                    throw th;
                                }
                            }
                            i3 |= 256;
                            userInfo = new UserInfo(nextAvailableId, str, (String) null, i3);
                            int i42 = this.mNextSerialNumber;
                            this.mNextSerialNumber = i42 + 1;
                            userInfo.serialNumber = i42;
                            jCurrentTimeMillis = System.currentTimeMillis();
                            if (jCurrentTimeMillis <= EPOCH_PLUS_30_YEARS) {
                            }
                        }
                    } catch (Throwable th11) {
                        th = th11;
                        Binder.restoreCallingIdentity(r3);
                        throw th;
                    }
                } catch (Throwable th12) {
                    th = th12;
                }
            }
        } catch (Throwable th13) {
            th = th13;
            r3 = jClearCallingIdentity;
        }
    }

    @VisibleForTesting
    UserData putUserInfo(UserInfo userInfo) {
        UserData userData = new UserData();
        userData.info = userInfo;
        synchronized (this.mUsers) {
            this.mUsers.put(userInfo.id, userData);
        }
        return userData;
    }

    @VisibleForTesting
    void removeUserInfo(int i) {
        synchronized (this.mUsers) {
            this.mUsers.remove(i);
        }
    }

    public UserInfo createRestrictedProfile(String str, int i) {
        checkManageOrCreateUsersPermission("setupRestrictedProfile");
        UserInfo userInfoCreateProfileForUser = createProfileForUser(str, 8, i, null);
        if (userInfoCreateProfileForUser == null) {
            return null;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            setUserRestriction("no_modify_accounts", true, userInfoCreateProfileForUser.id);
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "location_mode", 0, userInfoCreateProfileForUser.id);
            setUserRestriction("no_share_location", true, userInfoCreateProfileForUser.id);
            return userInfoCreateProfileForUser;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private UserInfo findCurrentGuestUser() {
        synchronized (this.mUsersLock) {
            int size = this.mUsers.size();
            for (int i = 0; i < size; i++) {
                UserInfo userInfo = this.mUsers.valueAt(i).info;
                if (userInfo.isGuest() && !userInfo.guestToRemove && !this.mRemovingUserIds.get(userInfo.id)) {
                    return userInfo;
                }
            }
            return null;
        }
    }

    public boolean markGuestForDeletion(int i) {
        checkManageUsersPermission("Only the system can remove users");
        if (getUserRestrictions(UserHandle.getCallingUserId()).getBoolean("no_remove_user", false)) {
            Log.w(LOG_TAG, "Cannot remove user. DISALLOW_REMOVE_USER is enabled.");
            return false;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPackagesLock) {
                synchronized (this.mUsersLock) {
                    UserData userData = this.mUsers.get(i);
                    if (i != 0 && userData != null && !this.mRemovingUserIds.get(i)) {
                        if (!userData.info.isGuest()) {
                            return false;
                        }
                        userData.info.guestToRemove = true;
                        userData.info.flags |= 64;
                        writeUserLP(userData);
                        return true;
                    }
                    return false;
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public boolean removeUser(int i) {
        boolean z;
        Slog.i(LOG_TAG, "removeUser u" + i);
        checkManageOrCreateUsersPermission("Only the system can remove users");
        synchronized (this.mUsersLock) {
            UserInfo userInfoLU = getUserInfoLU(i);
            z = userInfoLU != null && userInfoLU.isManagedProfile();
        }
        String str = z ? "no_remove_managed_profile" : "no_remove_user";
        if (getUserRestrictions(UserHandle.getCallingUserId()).getBoolean(str, false)) {
            Log.w(LOG_TAG, "Cannot remove user. " + str + " is enabled.");
            return false;
        }
        return removeUserUnchecked(i);
    }

    private boolean removeUserUnchecked(int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (ActivityManager.getCurrentUser() == i) {
                Log.w(LOG_TAG, "Current user cannot be removed");
                return false;
            }
            synchronized (this.mPackagesLock) {
                synchronized (this.mUsersLock) {
                    UserData userData = this.mUsers.get(i);
                    if (i != 0 && userData != null && !this.mRemovingUserIds.get(i)) {
                        addRemovingUserIdLocked(i);
                        userData.info.partial = true;
                        userData.info.flags |= 64;
                        writeUserLP(userData);
                        try {
                            this.mAppOpsService.removeUser(i);
                        } catch (RemoteException e) {
                            Log.w(LOG_TAG, "Unable to notify AppOpsService of removing user", e);
                        }
                        if (userData.info.profileGroupId != -10000 && userData.info.isManagedProfile()) {
                            sendProfileRemovedBroadcast(userData.info.profileGroupId, userData.info.id);
                        }
                        try {
                            return ActivityManager.getService().stopUser(i, true, new IStopUserCallback.Stub() {
                                public void userStopped(int i2) {
                                    UserManagerService.this.finishRemoveUser(i2);
                                }

                                public void userStopAborted(int i2) {
                                }
                            }) == 0;
                        } catch (RemoteException e2) {
                            return false;
                        }
                    }
                    return false;
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @GuardedBy("mUsersLock")
    @VisibleForTesting
    void addRemovingUserIdLocked(int i) {
        this.mRemovingUserIds.put(i, true);
        this.mRecentlyRemovedIds.add(Integer.valueOf(i));
        if (this.mRecentlyRemovedIds.size() > 100) {
            this.mRecentlyRemovedIds.removeFirst();
        }
    }

    void finishRemoveUser(final int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Intent intent = new Intent("android.intent.action.USER_REMOVED");
            intent.putExtra("android.intent.extra.user_handle", i);
            this.mContext.sendOrderedBroadcastAsUser(intent, UserHandle.ALL, "android.permission.MANAGE_USERS", new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent2) {
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
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void removeUserState(int i) {
        try {
            ((StorageManager) this.mContext.getSystemService(StorageManager.class)).destroyUserKey(i);
        } catch (IllegalStateException e) {
            Slog.i(LOG_TAG, "Destroying key for user " + i + " failed, continuing anyway", e);
        }
        try {
            IGateKeeperService service = GateKeeper.getService();
            if (service != null) {
                service.clearSecureUserId(i);
            }
        } catch (Exception e2) {
            Slog.w(LOG_TAG, "unable to clear GK secure user id");
        }
        this.mPm.cleanUpUser(this, i);
        this.mUserDataPreparer.destroyUserData(i, 3);
        synchronized (this.mUsersLock) {
            this.mUsers.remove(i);
            this.mIsUserManaged.delete(i);
        }
        synchronized (this.mUserStates) {
            this.mUserStates.delete(i);
        }
        synchronized (this.mRestrictionsLock) {
            this.mBaseUserRestrictions.remove(i);
            this.mAppliedUserRestrictions.remove(i);
            this.mCachedEffectiveUserRestrictions.remove(i);
            this.mDevicePolicyLocalUserRestrictions.remove(i);
            if (this.mDevicePolicyGlobalUserRestrictions.get(i) != null) {
                this.mDevicePolicyGlobalUserRestrictions.remove(i);
                applyUserRestrictionsForAllUsersLR();
            }
        }
        synchronized (this.mPackagesLock) {
            writeUserListLP();
        }
        new AtomicFile(new File(this.mUsersDir, i + XML_SUFFIX)).delete();
        updateUserIds();
    }

    private void sendProfileRemovedBroadcast(int i, int i2) {
        Intent intent = new Intent("android.intent.action.MANAGED_PROFILE_REMOVED");
        intent.addFlags(1342177280);
        intent.putExtra("android.intent.extra.USER", new UserHandle(i2));
        intent.putExtra("android.intent.extra.user_handle", i2);
        this.mContext.sendBroadcastAsUser(intent, new UserHandle(i), null);
    }

    public Bundle getApplicationRestrictions(String str) {
        return getApplicationRestrictionsForUser(str, UserHandle.getCallingUserId());
    }

    public Bundle getApplicationRestrictionsForUser(String str, int i) {
        Bundle applicationRestrictionsLAr;
        if (UserHandle.getCallingUserId() != i || !UserHandle.isSameApp(Binder.getCallingUid(), getUidForPackage(str))) {
            checkSystemOrRoot("get application restrictions for other user/app " + str);
        }
        synchronized (this.mAppRestrictionsLock) {
            applicationRestrictionsLAr = readApplicationRestrictionsLAr(str, i);
        }
        return applicationRestrictionsLAr;
    }

    public void setApplicationRestrictions(String str, Bundle bundle, int i) {
        checkSystemOrRoot("set application restrictions");
        if (bundle != null) {
            bundle.setDefusable(true);
        }
        synchronized (this.mAppRestrictionsLock) {
            if (bundle == null) {
                cleanAppRestrictionsForPackageLAr(str, i);
            } else {
                try {
                    if (bundle.isEmpty()) {
                        cleanAppRestrictionsForPackageLAr(str, i);
                    } else {
                        writeApplicationRestrictionsLAr(str, bundle, i);
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
        Intent intent = new Intent("android.intent.action.APPLICATION_RESTRICTIONS_CHANGED");
        intent.setPackage(str);
        intent.addFlags(1073741824);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.of(i));
    }

    private int getUidForPackage(String str) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return this.mContext.getPackageManager().getApplicationInfo(str, DumpState.DUMP_CHANGES).uid;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @GuardedBy("mAppRestrictionsLock")
    private static Bundle readApplicationRestrictionsLAr(String str, int i) {
        return readApplicationRestrictionsLAr(new AtomicFile(new File(Environment.getUserSystemDirectory(i), packageToRestrictionsFileName(str))));
    }

    @GuardedBy("mAppRestrictionsLock")
    @VisibleForTesting
    static Bundle readApplicationRestrictionsLAr(AtomicFile atomicFile) throws Throwable {
        ?? OpenRead;
        ?? NewPullParser;
        Bundle bundle = new Bundle();
        ArrayList arrayList = new ArrayList();
        if (!atomicFile.getBaseFile().exists()) {
            return bundle;
        }
        ?? r2 = 0;
        ?? r22 = 0;
        try {
            try {
                OpenRead = atomicFile.openRead();
            } catch (Throwable th) {
                th = th;
                OpenRead = r2;
            }
        } catch (IOException | XmlPullParserException e) {
            e = e;
        }
        try {
            try {
                NewPullParser = Xml.newPullParser();
                NewPullParser.setInput(OpenRead, StandardCharsets.UTF_8.name());
                XmlUtils.nextElement((XmlPullParser) NewPullParser);
            } catch (IOException | XmlPullParserException e2) {
                e = e2;
                r22 = OpenRead;
                Log.w(LOG_TAG, "Error parsing " + atomicFile.getBaseFile(), e);
                IoUtils.closeQuietly((AutoCloseable) r22);
                r2 = r22;
            }
            if (NewPullParser.getEventType() == 2) {
                while (NewPullParser.next() != 1) {
                    readEntry(bundle, arrayList, NewPullParser);
                }
                IoUtils.closeQuietly((AutoCloseable) OpenRead);
                r2 = NewPullParser;
                return bundle;
            }
            Slog.e(LOG_TAG, "Unable to read restrictions file " + atomicFile.getBaseFile());
            IoUtils.closeQuietly((AutoCloseable) OpenRead);
            return bundle;
        } catch (Throwable th2) {
            th = th2;
            IoUtils.closeQuietly((AutoCloseable) OpenRead);
            throw th;
        }
    }

    private static void readEntry(Bundle bundle, ArrayList<String> arrayList, XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        if (xmlPullParser.getEventType() == 2 && xmlPullParser.getName().equals(TAG_ENTRY)) {
            String attributeValue = xmlPullParser.getAttributeValue(null, ATTR_KEY);
            String attributeValue2 = xmlPullParser.getAttributeValue(null, "type");
            String attributeValue3 = xmlPullParser.getAttributeValue(null, ATTR_MULTIPLE);
            if (attributeValue3 != null) {
                arrayList.clear();
                int i = Integer.parseInt(attributeValue3);
                while (i > 0) {
                    int next = xmlPullParser.next();
                    if (next == 1) {
                        break;
                    }
                    if (next == 2 && xmlPullParser.getName().equals(TAG_VALUE)) {
                        arrayList.add(xmlPullParser.nextText().trim());
                        i--;
                    }
                }
                String[] strArr = new String[arrayList.size()];
                arrayList.toArray(strArr);
                bundle.putStringArray(attributeValue, strArr);
                return;
            }
            if (ATTR_TYPE_BUNDLE.equals(attributeValue2)) {
                bundle.putBundle(attributeValue, readBundleEntry(xmlPullParser, arrayList));
                return;
            }
            if (ATTR_TYPE_BUNDLE_ARRAY.equals(attributeValue2)) {
                int depth = xmlPullParser.getDepth();
                ArrayList arrayList2 = new ArrayList();
                while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
                    arrayList2.add(readBundleEntry(xmlPullParser, arrayList));
                }
                bundle.putParcelableArray(attributeValue, (Parcelable[]) arrayList2.toArray(new Bundle[arrayList2.size()]));
                return;
            }
            String strTrim = xmlPullParser.nextText().trim();
            if (ATTR_TYPE_BOOLEAN.equals(attributeValue2)) {
                bundle.putBoolean(attributeValue, Boolean.parseBoolean(strTrim));
            } else if (ATTR_TYPE_INTEGER.equals(attributeValue2)) {
                bundle.putInt(attributeValue, Integer.parseInt(strTrim));
            } else {
                bundle.putString(attributeValue, strTrim);
            }
        }
    }

    private static Bundle readBundleEntry(XmlPullParser xmlPullParser, ArrayList<String> arrayList) throws XmlPullParserException, IOException {
        Bundle bundle = new Bundle();
        int depth = xmlPullParser.getDepth();
        while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
            readEntry(bundle, arrayList, xmlPullParser);
        }
        return bundle;
    }

    @GuardedBy("mAppRestrictionsLock")
    private static void writeApplicationRestrictionsLAr(String str, Bundle bundle, int i) {
        writeApplicationRestrictionsLAr(bundle, new AtomicFile(new File(Environment.getUserSystemDirectory(i), packageToRestrictionsFileName(str))));
    }

    @GuardedBy("mAppRestrictionsLock")
    @VisibleForTesting
    static void writeApplicationRestrictionsLAr(Bundle bundle, AtomicFile atomicFile) {
        FileOutputStream fileOutputStreamStartWrite;
        try {
            fileOutputStreamStartWrite = atomicFile.startWrite();
        } catch (Exception e) {
            e = e;
            fileOutputStreamStartWrite = null;
        }
        try {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStreamStartWrite);
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(bufferedOutputStream, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag(null, TAG_RESTRICTIONS);
            writeBundle(bundle, fastXmlSerializer);
            fastXmlSerializer.endTag(null, TAG_RESTRICTIONS);
            fastXmlSerializer.endDocument();
            atomicFile.finishWrite(fileOutputStreamStartWrite);
        } catch (Exception e2) {
            e = e2;
            atomicFile.failWrite(fileOutputStreamStartWrite);
            Slog.e(LOG_TAG, "Error writing application restrictions list", e);
        }
    }

    private static void writeBundle(Bundle bundle, XmlSerializer xmlSerializer) throws IOException {
        for (String str : bundle.keySet()) {
            Object obj = bundle.get(str);
            xmlSerializer.startTag(null, TAG_ENTRY);
            xmlSerializer.attribute(null, ATTR_KEY, str);
            if (obj instanceof Boolean) {
                xmlSerializer.attribute(null, "type", ATTR_TYPE_BOOLEAN);
                xmlSerializer.text(obj.toString());
            } else if (obj instanceof Integer) {
                xmlSerializer.attribute(null, "type", ATTR_TYPE_INTEGER);
                xmlSerializer.text(obj.toString());
            } else if (obj == null || (obj instanceof String)) {
                xmlSerializer.attribute(null, "type", ATTR_TYPE_STRING);
                xmlSerializer.text(obj != null ? (String) obj : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            } else if (obj instanceof Bundle) {
                xmlSerializer.attribute(null, "type", ATTR_TYPE_BUNDLE);
                writeBundle((Bundle) obj, xmlSerializer);
            } else {
                int i = 0;
                if (obj instanceof Parcelable[]) {
                    xmlSerializer.attribute(null, "type", ATTR_TYPE_BUNDLE_ARRAY);
                    Parcelable[] parcelableArr = (Parcelable[]) obj;
                    int length = parcelableArr.length;
                    while (i < length) {
                        Parcelable parcelable = parcelableArr[i];
                        if (!(parcelable instanceof Bundle)) {
                            throw new IllegalArgumentException("bundle-array can only hold Bundles");
                        }
                        xmlSerializer.startTag(null, TAG_ENTRY);
                        xmlSerializer.attribute(null, "type", ATTR_TYPE_BUNDLE);
                        writeBundle((Bundle) parcelable, xmlSerializer);
                        xmlSerializer.endTag(null, TAG_ENTRY);
                        i++;
                    }
                } else {
                    xmlSerializer.attribute(null, "type", ATTR_TYPE_STRING_ARRAY);
                    String[] strArr = (String[]) obj;
                    xmlSerializer.attribute(null, ATTR_MULTIPLE, Integer.toString(strArr.length));
                    int length2 = strArr.length;
                    while (i < length2) {
                        String str2 = strArr[i];
                        xmlSerializer.startTag(null, TAG_VALUE);
                        if (str2 == null) {
                            str2 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                        }
                        xmlSerializer.text(str2);
                        xmlSerializer.endTag(null, TAG_VALUE);
                        i++;
                    }
                }
            }
            xmlSerializer.endTag(null, TAG_ENTRY);
        }
    }

    public int getUserSerialNumber(int i) {
        synchronized (this.mUsersLock) {
            if (!exists(i)) {
                return -1;
            }
            return getUserInfoLU(i).serialNumber;
        }
    }

    public boolean isUserNameSet(int i) {
        boolean z;
        synchronized (this.mUsersLock) {
            UserInfo userInfoLU = getUserInfoLU(i);
            z = (userInfoLU == null || userInfoLU.name == null) ? false : true;
        }
        return z;
    }

    public int getUserHandle(int i) {
        synchronized (this.mUsersLock) {
            for (int i2 : this.mUserIds) {
                UserInfo userInfoLU = getUserInfoLU(i2);
                if (userInfoLU != null && userInfoLU.serialNumber == i) {
                    return i2;
                }
            }
            return -1;
        }
    }

    public long getUserCreationTime(int i) {
        UserInfo userInfoLU;
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this.mUsersLock) {
            try {
                if (callingUserId == i) {
                    userInfoLU = getUserInfoLU(i);
                } else {
                    UserInfo profileParentLU = getProfileParentLU(i);
                    if (profileParentLU != null && profileParentLU.id == callingUserId) {
                        userInfoLU = getUserInfoLU(i);
                    } else {
                        userInfoLU = null;
                    }
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        if (userInfoLU == null) {
            throw new SecurityException("userHandle can only be the calling user or a managed profile associated with this user");
        }
        return userInfoLU.creationTime;
    }

    private void updateUserIds() {
        synchronized (this.mUsersLock) {
            int size = this.mUsers.size();
            int i = 0;
            for (int i2 = 0; i2 < size; i2++) {
                if (!this.mUsers.valueAt(i2).info.partial) {
                    i++;
                }
            }
            int[] iArr = new int[i];
            int i3 = 0;
            for (int i4 = 0; i4 < size; i4++) {
                if (!this.mUsers.valueAt(i4).info.partial) {
                    iArr[i3] = this.mUsers.keyAt(i4);
                    i3++;
                }
            }
            this.mUserIds = iArr;
        }
    }

    public void onBeforeStartUser(int i) {
        UserInfo userInfo = getUserInfo(i);
        if (userInfo == null) {
            return;
        }
        int i2 = userInfo.serialNumber;
        boolean z = !Build.FINGERPRINT.equals(userInfo.lastLoggedInFingerprint);
        this.mUserDataPreparer.prepareUserData(i, i2, 1);
        this.mPm.reconcileAppsData(i, 1, z);
        if (i != 0) {
            synchronized (this.mRestrictionsLock) {
                applyUserRestrictionsLR(i);
            }
        }
    }

    public void onBeforeUnlockUser(int i) {
        UserInfo userInfo = getUserInfo(i);
        if (userInfo == null) {
            return;
        }
        int i2 = userInfo.serialNumber;
        boolean z = !Build.FINGERPRINT.equals(userInfo.lastLoggedInFingerprint);
        this.mUserDataPreparer.prepareUserData(i, i2, 2);
        this.mPm.reconcileAppsData(i, 2, z);
    }

    void reconcileUsers(String str) {
        this.mUserDataPreparer.reconcileUsers(str, getUsers(true));
    }

    public void onUserLoggedIn(int i) {
        UserData userDataNoChecks = getUserDataNoChecks(i);
        if (userDataNoChecks == null || userDataNoChecks.info.partial) {
            Slog.w(LOG_TAG, "userForeground: unknown user #" + i);
            return;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (jCurrentTimeMillis > EPOCH_PLUS_30_YEARS) {
            userDataNoChecks.info.lastLoggedInTime = jCurrentTimeMillis;
        }
        userDataNoChecks.info.lastLoggedInFingerprint = Build.FINGERPRINT;
        scheduleWriteUser(userDataNoChecks);
    }

    @VisibleForTesting
    int getNextAvailableId() {
        synchronized (this.mUsersLock) {
            int iScanNextAvailableIdLocked = scanNextAvailableIdLocked();
            if (iScanNextAvailableIdLocked >= 0) {
                return iScanNextAvailableIdLocked;
            }
            if (this.mRemovingUserIds.size() > 0) {
                Slog.i(LOG_TAG, "All available IDs are used. Recycling LRU ids.");
                this.mRemovingUserIds.clear();
                Iterator<Integer> it = this.mRecentlyRemovedIds.iterator();
                while (it.hasNext()) {
                    this.mRemovingUserIds.put(it.next().intValue(), true);
                }
                iScanNextAvailableIdLocked = scanNextAvailableIdLocked();
            }
            if (iScanNextAvailableIdLocked < 0) {
                throw new IllegalStateException("No user id available!");
            }
            return iScanNextAvailableIdLocked;
        }
    }

    @GuardedBy("mUsersLock")
    private int scanNextAvailableIdLocked() {
        for (int i = 10; i < MAX_USER_ID; i++) {
            if (this.mUsers.indexOfKey(i) < 0 && !this.mRemovingUserIds.get(i)) {
                return i;
            }
        }
        return -1;
    }

    private static String packageToRestrictionsFileName(String str) {
        return RESTRICTIONS_FILE_PREFIX + str + XML_SUFFIX;
    }

    public void setSeedAccountData(int i, String str, String str2, PersistableBundle persistableBundle, boolean z) {
        checkManageUsersPermission("Require MANAGE_USERS permission to set user seed data");
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                UserData userDataLU = getUserDataLU(i);
                if (userDataLU == null) {
                    Slog.e(LOG_TAG, "No such user for settings seed data u=" + i);
                    return;
                }
                userDataLU.seedAccountName = str;
                userDataLU.seedAccountType = str2;
                userDataLU.seedAccountOptions = persistableBundle;
                userDataLU.persistSeedData = z;
                if (z) {
                    writeUserLP(userDataLU);
                }
            }
        }
    }

    public String getSeedAccountName() throws RemoteException {
        String str;
        checkManageUsersPermission("Cannot get seed account information");
        synchronized (this.mUsersLock) {
            str = getUserDataLU(UserHandle.getCallingUserId()).seedAccountName;
        }
        return str;
    }

    public String getSeedAccountType() throws RemoteException {
        String str;
        checkManageUsersPermission("Cannot get seed account information");
        synchronized (this.mUsersLock) {
            str = getUserDataLU(UserHandle.getCallingUserId()).seedAccountType;
        }
        return str;
    }

    public PersistableBundle getSeedAccountOptions() throws RemoteException {
        PersistableBundle persistableBundle;
        checkManageUsersPermission("Cannot get seed account information");
        synchronized (this.mUsersLock) {
            persistableBundle = getUserDataLU(UserHandle.getCallingUserId()).seedAccountOptions;
        }
        return persistableBundle;
    }

    public void clearSeedAccountData() throws RemoteException {
        checkManageUsersPermission("Cannot clear seed account information");
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                UserData userDataLU = getUserDataLU(UserHandle.getCallingUserId());
                if (userDataLU == null) {
                    return;
                }
                userDataLU.clearSeedAccountData();
                writeUserLP(userDataLU);
            }
        }
    }

    public boolean someUserHasSeedAccount(String str, String str2) throws RemoteException {
        checkManageUsersPermission("Cannot check seed account information");
        synchronized (this.mUsersLock) {
            int size = this.mUsers.size();
            for (int i = 0; i < size; i++) {
                UserData userDataValueAt = this.mUsers.valueAt(i);
                if (!userDataValueAt.info.isInitialized() && userDataValueAt.seedAccountName != null && userDataValueAt.seedAccountName.equals(str) && userDataValueAt.seedAccountType != null && userDataValueAt.seedAccountType.equals(str2)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
        new Shell(this, null).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
    }

    int onShellCommand(Shell shell, String str) {
        if (str == null) {
            return shell.handleDefaultCommands(str);
        }
        PrintWriter outPrintWriter = shell.getOutPrintWriter();
        try {
            if (((str.hashCode() == 3322014 && str.equals("list")) ? (byte) 0 : (byte) -1) == 0) {
                return runList(outPrintWriter);
            }
        } catch (RemoteException e) {
            outPrintWriter.println("Remote exception: " + e);
        }
        return -1;
    }

    private int runList(PrintWriter printWriter) throws RemoteException {
        IActivityManager service = ActivityManager.getService();
        List<UserInfo> users = getUsers(false);
        if (users == null) {
            printWriter.println("Error: couldn't get users");
            return 1;
        }
        printWriter.println("Users:");
        for (int i = 0; i < users.size(); i++) {
            printWriter.println("\t" + users.get(i).toString() + (service.isUserRunning(users.get(i).id, 0) ? " running" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS));
        }
        return 0;
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        int i;
        int i2;
        UserManagerService userManagerService = this;
        if (DumpUtils.checkDumpPermission(userManagerService.mContext, LOG_TAG, printWriter)) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            StringBuilder sb = new StringBuilder();
            synchronized (userManagerService.mPackagesLock) {
                synchronized (userManagerService.mUsersLock) {
                    printWriter.println("Users:");
                    int i3 = 0;
                    while (i3 < userManagerService.mUsers.size()) {
                        UserData userDataValueAt = userManagerService.mUsers.valueAt(i3);
                        if (userDataValueAt == null) {
                            i2 = i3;
                        } else {
                            UserInfo userInfo = userDataValueAt.info;
                            int i4 = userInfo.id;
                            printWriter.print("  ");
                            printWriter.print(userInfo);
                            printWriter.print(" serialNo=");
                            printWriter.print(userInfo.serialNumber);
                            if (userManagerService.mRemovingUserIds.get(i4)) {
                                printWriter.print(" <removing> ");
                            }
                            if (userInfo.partial) {
                                printWriter.print(" <partial>");
                            }
                            printWriter.println();
                            printWriter.print("    State: ");
                            synchronized (userManagerService.mUserStates) {
                                i = userManagerService.mUserStates.get(i4, -1);
                            }
                            printWriter.println(UserState.stateToString(i));
                            printWriter.print("    Created: ");
                            i2 = i3;
                            dumpTimeAgo(printWriter, sb, jCurrentTimeMillis, userInfo.creationTime);
                            printWriter.print("    Last logged in: ");
                            dumpTimeAgo(printWriter, sb, jCurrentTimeMillis, userInfo.lastLoggedInTime);
                            printWriter.print("    Last logged in fingerprint: ");
                            printWriter.println(userInfo.lastLoggedInFingerprint);
                            printWriter.print("    Start time: ");
                            dumpTimeAgo(printWriter, sb, jElapsedRealtime, userDataValueAt.startRealtime);
                            printWriter.print("    Unlock time: ");
                            dumpTimeAgo(printWriter, sb, jElapsedRealtime, userDataValueAt.unlockRealtime);
                            printWriter.print("    Has profile owner: ");
                            userManagerService = this;
                            printWriter.println(userManagerService.mIsUserManaged.get(i4));
                            printWriter.println("    Restrictions:");
                            synchronized (userManagerService.mRestrictionsLock) {
                                UserRestrictionsUtils.dumpRestrictions(printWriter, "      ", userManagerService.mBaseUserRestrictions.get(userInfo.id));
                                printWriter.println("    Device policy global restrictions:");
                                UserRestrictionsUtils.dumpRestrictions(printWriter, "      ", userManagerService.mDevicePolicyGlobalUserRestrictions.get(userInfo.id));
                                printWriter.println("    Device policy local restrictions:");
                                UserRestrictionsUtils.dumpRestrictions(printWriter, "      ", userManagerService.mDevicePolicyLocalUserRestrictions.get(userInfo.id));
                                printWriter.println("    Effective restrictions:");
                                UserRestrictionsUtils.dumpRestrictions(printWriter, "      ", userManagerService.mCachedEffectiveUserRestrictions.get(userInfo.id));
                            }
                            if (userDataValueAt.account != null) {
                                printWriter.print("    Account name: " + userDataValueAt.account);
                                printWriter.println();
                            }
                            if (userDataValueAt.seedAccountName != null) {
                                printWriter.print("    Seed account name: " + userDataValueAt.seedAccountName);
                                printWriter.println();
                                if (userDataValueAt.seedAccountType != null) {
                                    printWriter.print("         account type: " + userDataValueAt.seedAccountType);
                                    printWriter.println();
                                }
                                if (userDataValueAt.seedAccountOptions != null) {
                                    printWriter.print("         account options exist");
                                    printWriter.println();
                                }
                            }
                        }
                        i3 = i2 + 1;
                    }
                }
                printWriter.println();
                printWriter.println("  Device owner id:" + userManagerService.mDeviceOwnerUserId);
                printWriter.println();
                printWriter.println("  Guest restrictions:");
                synchronized (userManagerService.mGuestRestrictions) {
                    UserRestrictionsUtils.dumpRestrictions(printWriter, "    ", userManagerService.mGuestRestrictions);
                }
                synchronized (userManagerService.mUsersLock) {
                    printWriter.println();
                    printWriter.println("  Device managed: " + userManagerService.mIsDeviceManaged);
                    if (userManagerService.mRemovingUserIds.size() > 0) {
                        printWriter.println();
                        printWriter.println("  Recently removed userIds: " + userManagerService.mRecentlyRemovedIds);
                    }
                }
                synchronized (userManagerService.mUserStates) {
                    printWriter.println("  Started users state: " + userManagerService.mUserStates);
                }
                printWriter.println();
                printWriter.println("  Max users: " + UserManager.getMaxSupportedUsers());
                printWriter.println("  Supports switchable users: " + UserManager.supportsMultipleUsers());
                printWriter.println("  All guests ephemeral: " + Resources.getSystem().getBoolean(R.^attr-private.interpolatorY));
            }
        }
    }

    private static void dumpTimeAgo(PrintWriter printWriter, StringBuilder sb, long j, long j2) {
        if (j2 == 0) {
            printWriter.println("<unknown>");
            return;
        }
        sb.setLength(0);
        TimeUtils.formatDuration(j - j2, sb);
        sb.append(" ago");
        printWriter.println(sb);
    }

    final class MainHandler extends Handler {
        MainHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                removeMessages(1, message.obj);
                synchronized (UserManagerService.this.mPackagesLock) {
                    UserData userDataNoChecks = UserManagerService.this.getUserDataNoChecks(((UserData) message.obj).info.id);
                    if (userDataNoChecks != null) {
                        UserManagerService.this.writeUserLP(userDataNoChecks);
                    }
                }
            }
        }
    }

    boolean isUserInitialized(int i) {
        return this.mLocalService.isUserInitialized(i);
    }

    private class LocalService extends UserManagerInternal {
        private LocalService() {
        }

        LocalService(UserManagerService userManagerService, AnonymousClass1 anonymousClass1) {
            this();
        }

        public void setDevicePolicyUserRestrictions(int i, Bundle bundle, boolean z, int i2) {
            UserManagerService.this.setDevicePolicyUserRestrictionsInner(i, bundle, z, i2);
        }

        public Bundle getBaseUserRestrictions(int i) {
            Bundle bundle;
            synchronized (UserManagerService.this.mRestrictionsLock) {
                bundle = (Bundle) UserManagerService.this.mBaseUserRestrictions.get(i);
            }
            return bundle;
        }

        public void setBaseUserRestrictionsByDpmsForMigration(int i, Bundle bundle) {
            synchronized (UserManagerService.this.mRestrictionsLock) {
                if (UserManagerService.this.updateRestrictionsIfNeededLR(i, new Bundle(bundle), UserManagerService.this.mBaseUserRestrictions)) {
                    UserManagerService.this.invalidateEffectiveUserRestrictionsLR(i);
                }
            }
            UserData userDataNoChecks = UserManagerService.this.getUserDataNoChecks(i);
            synchronized (UserManagerService.this.mPackagesLock) {
                try {
                    if (userDataNoChecks != null) {
                        UserManagerService.this.writeUserLP(userDataNoChecks);
                    } else {
                        Slog.w(UserManagerService.LOG_TAG, "UserInfo not found for " + i);
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }

        public boolean getUserRestriction(int i, String str) {
            return UserManagerService.this.getUserRestrictions(i).getBoolean(str);
        }

        public void addUserRestrictionsListener(UserManagerInternal.UserRestrictionsListener userRestrictionsListener) {
            synchronized (UserManagerService.this.mUserRestrictionsListeners) {
                UserManagerService.this.mUserRestrictionsListeners.add(userRestrictionsListener);
            }
        }

        public void removeUserRestrictionsListener(UserManagerInternal.UserRestrictionsListener userRestrictionsListener) {
            synchronized (UserManagerService.this.mUserRestrictionsListeners) {
                UserManagerService.this.mUserRestrictionsListeners.remove(userRestrictionsListener);
            }
        }

        public void setDeviceManaged(boolean z) {
            synchronized (UserManagerService.this.mUsersLock) {
                UserManagerService.this.mIsDeviceManaged = z;
            }
        }

        public void setUserManaged(int i, boolean z) {
            synchronized (UserManagerService.this.mUsersLock) {
                UserManagerService.this.mIsUserManaged.put(i, z);
            }
        }

        public void setUserIcon(int i, Bitmap bitmap) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (UserManagerService.this.mPackagesLock) {
                    UserData userDataNoChecks = UserManagerService.this.getUserDataNoChecks(i);
                    if (userDataNoChecks != null && !userDataNoChecks.info.partial) {
                        UserManagerService.this.writeBitmapLP(userDataNoChecks.info, bitmap);
                        UserManagerService.this.writeUserLP(userDataNoChecks);
                        UserManagerService.this.sendUserInfoChangedBroadcast(i);
                        return;
                    }
                    Slog.w(UserManagerService.LOG_TAG, "setUserIcon: unknown user #" + i);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setForceEphemeralUsers(boolean z) {
            synchronized (UserManagerService.this.mUsersLock) {
                UserManagerService.this.mForceEphemeralUsers = z;
            }
        }

        public void removeAllUsers() {
            if (ActivityManager.getCurrentUser() == 0) {
                UserManagerService.this.removeNonSystemUsers();
                return;
            }
            BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getIntExtra("android.intent.extra.user_handle", -10000) == 0) {
                        UserManagerService.this.mContext.unregisterReceiver(this);
                        UserManagerService.this.removeNonSystemUsers();
                    }
                }
            };
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.USER_SWITCHED");
            UserManagerService.this.mContext.registerReceiver(broadcastReceiver, intentFilter, null, UserManagerService.this.mHandler);
            ((ActivityManager) UserManagerService.this.mContext.getSystemService("activity")).switchUser(0);
        }

        public void onEphemeralUserStop(int i) {
            synchronized (UserManagerService.this.mUsersLock) {
                UserInfo userInfoLU = UserManagerService.this.getUserInfoLU(i);
                if (userInfoLU != null && userInfoLU.isEphemeral()) {
                    userInfoLU.flags |= 64;
                    if (userInfoLU.isGuest()) {
                        userInfoLU.guestToRemove = true;
                    }
                }
            }
        }

        public UserInfo createUserEvenWhenDisallowed(String str, int i, String[] strArr) throws Throwable {
            UserInfo userInfoCreateUserInternalUnchecked = UserManagerService.this.createUserInternalUnchecked(str, i, -10000, strArr);
            if (userInfoCreateUserInternalUnchecked != null && !userInfoCreateUserInternalUnchecked.isAdmin() && !userInfoCreateUserInternalUnchecked.isDemo()) {
                UserManagerService.this.setUserRestriction("no_sms", true, userInfoCreateUserInternalUnchecked.id);
                UserManagerService.this.setUserRestriction("no_outgoing_calls", true, userInfoCreateUserInternalUnchecked.id);
            }
            return userInfoCreateUserInternalUnchecked;
        }

        public boolean removeUserEvenWhenDisallowed(int i) {
            return UserManagerService.this.removeUserUnchecked(i);
        }

        public boolean isUserRunning(int i) {
            boolean z;
            synchronized (UserManagerService.this.mUserStates) {
                z = UserManagerService.this.mUserStates.get(i, -1) >= 0;
            }
            return z;
        }

        public void setUserState(int i, int i2) {
            synchronized (UserManagerService.this.mUserStates) {
                UserManagerService.this.mUserStates.put(i, i2);
            }
        }

        public void removeUserState(int i) {
            synchronized (UserManagerService.this.mUserStates) {
                UserManagerService.this.mUserStates.delete(i);
            }
        }

        public int[] getUserIds() {
            return UserManagerService.this.getUserIds();
        }

        public boolean isUserUnlockingOrUnlocked(int i) {
            int i2;
            synchronized (UserManagerService.this.mUserStates) {
                i2 = UserManagerService.this.mUserStates.get(i, -1);
            }
            if (i2 == 4 || i2 == 5) {
                return StorageManager.isUserKeyUnlocked(i);
            }
            return i2 == 2 || i2 == 3;
        }

        public boolean isUserUnlocked(int i) {
            int i2;
            synchronized (UserManagerService.this.mUserStates) {
                i2 = UserManagerService.this.mUserStates.get(i, -1);
            }
            if (i2 == 4 || i2 == 5) {
                return StorageManager.isUserKeyUnlocked(i);
            }
            return i2 == 3;
        }

        public boolean isUserInitialized(int i) {
            return (UserManagerService.this.getUserInfo(i).flags & 16) != 0;
        }

        public boolean exists(int i) {
            return UserManagerService.this.getUserInfoNoChecks(i) != null;
        }

        public boolean isProfileAccessible(int i, int i2, String str, boolean z) {
            if (i2 != i) {
                synchronized (UserManagerService.this.mUsersLock) {
                    UserInfo userInfoLU = UserManagerService.this.getUserInfoLU(i);
                    if ((userInfoLU != null && !userInfoLU.isManagedProfile()) || !z) {
                        UserInfo userInfoLU2 = UserManagerService.this.getUserInfoLU(i2);
                        if (userInfoLU2 != null && userInfoLU2.isEnabled()) {
                            if (userInfoLU2.profileGroupId != -10000 && userInfoLU2.profileGroupId == userInfoLU.profileGroupId) {
                                return true;
                            }
                            if (!z) {
                                return false;
                            }
                            throw new SecurityException(str + " for unrelated profile " + i2);
                        }
                        if (z) {
                            Slog.w(UserManagerService.LOG_TAG, str + " for disabled profile " + i2 + " from " + i);
                        }
                        return false;
                    }
                    throw new SecurityException(str + " for another profile " + i2 + " from " + i);
                }
            }
            return true;
        }

        public int getProfileParentId(int i) {
            synchronized (UserManagerService.this.mUsersLock) {
                UserInfo profileParentLU = UserManagerService.this.getProfileParentLU(i);
                if (profileParentLU == null) {
                    return i;
                }
                return profileParentLU.id;
            }
        }

        public boolean isSettingRestrictedForUser(String str, int i, String str2, int i2) {
            return UserRestrictionsUtils.isSettingRestrictedForUser(UserManagerService.this.mContext, str, i, str2, i2);
        }
    }

    private void removeNonSystemUsers() {
        ArrayList arrayList = new ArrayList();
        synchronized (this.mUsersLock) {
            int size = this.mUsers.size();
            for (int i = 0; i < size; i++) {
                UserInfo userInfo = this.mUsers.valueAt(i).info;
                if (userInfo.id != 0) {
                    arrayList.add(userInfo);
                }
            }
        }
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            removeUser(((UserInfo) it.next()).id);
        }
    }

    private class Shell extends ShellCommand {
        private Shell() {
        }

        Shell(UserManagerService userManagerService, AnonymousClass1 anonymousClass1) {
            this();
        }

        public int onCommand(String str) {
            return UserManagerService.this.onShellCommand(this, str);
        }

        public void onHelp() {
            PrintWriter outPrintWriter = getOutPrintWriter();
            outPrintWriter.println("User manager (user) commands:");
            outPrintWriter.println("  help");
            outPrintWriter.println("    Print this help text.");
            outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            outPrintWriter.println("  list");
            outPrintWriter.println("    Prints all users on the system.");
        }
    }

    private static void debug(String str) {
        Log.d(LOG_TAG, str + BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
    }

    @VisibleForTesting
    static int getMaxManagedProfiles() {
        if (Build.IS_DEBUGGABLE) {
            return SystemProperties.getInt("persist.sys.max_profiles", 1);
        }
        return 1;
    }

    @VisibleForTesting
    int getFreeProfileBadgeLU(int i) {
        int maxManagedProfiles = getMaxManagedProfiles();
        boolean[] zArr = new boolean[maxManagedProfiles];
        int size = this.mUsers.size();
        for (int i2 = 0; i2 < size; i2++) {
            UserInfo userInfo = this.mUsers.valueAt(i2).info;
            if (userInfo.isManagedProfile() && userInfo.profileGroupId == i && !this.mRemovingUserIds.get(userInfo.id) && userInfo.profileBadge < maxManagedProfiles) {
                zArr[userInfo.profileBadge] = true;
            }
        }
        for (int i3 = 0; i3 < maxManagedProfiles; i3++) {
            if (!zArr[i3]) {
                return i3;
            }
        }
        return 0;
    }

    boolean hasManagedProfile(int i) {
        synchronized (this.mUsersLock) {
            UserInfo userInfoLU = getUserInfoLU(i);
            int size = this.mUsers.size();
            for (int i2 = 0; i2 < size; i2++) {
                UserInfo userInfo = this.mUsers.valueAt(i2).info;
                if (i != userInfo.id && isProfileOf(userInfoLU, userInfo)) {
                    return true;
                }
            }
            return false;
        }
    }

    private void verifyCallingPackage(String str, int i) {
        if (this.mPm.getPackageUid(str, 0, UserHandle.getUserId(i)) != i) {
            throw new SecurityException("Specified package " + str + " does not match the calling uid " + i);
        }
    }
}
