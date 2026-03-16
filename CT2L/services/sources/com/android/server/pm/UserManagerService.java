package com.android.server.pm;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IStopUserCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IUserManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.TimeUtils;
import android.util.Xml;
import com.android.internal.app.IAppOpsService;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.am.ProcessList;
import com.android.server.pm.PackageManagerService;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class UserManagerService extends IUserManager.Stub {
    private static final int ALLOWED_FLAGS_FOR_CREATE_USERS_PERMISSION = 44;
    private static final String ATTR_CREATION_TIME = "created";
    private static final String ATTR_FAILED_ATTEMPTS = "failedAttempts";
    private static final String ATTR_FLAGS = "flags";
    private static final String ATTR_GUEST_TO_REMOVE = "guestToRemove";
    private static final String ATTR_ICON_PATH = "icon";
    private static final String ATTR_ID = "id";
    private static final String ATTR_KEY = "key";
    private static final String ATTR_LAST_LOGGED_IN_TIME = "lastLoggedIn";
    private static final String ATTR_LAST_RETRY_MS = "lastAttemptMs";
    private static final String ATTR_MULTIPLE = "m";
    private static final String ATTR_NEXT_SERIAL_NO = "nextSerialNumber";
    private static final String ATTR_PARTIAL = "partial";
    private static final String ATTR_PIN_HASH = "pinHash";
    private static final String ATTR_PROFILE_GROUP_ID = "profileGroupId";
    private static final String ATTR_SALT = "salt";
    private static final String ATTR_SERIAL_NO = "serialNumber";
    private static final String ATTR_TYPE_BOOLEAN = "b";
    private static final String ATTR_TYPE_INTEGER = "i";
    private static final String ATTR_TYPE_STRING = "s";
    private static final String ATTR_TYPE_STRING_ARRAY = "sa";
    private static final String ATTR_USER_VERSION = "version";
    private static final String ATTR_VALUE_TYPE = "type";
    private static final int BACKOFF_INC_INTERVAL = 5;
    private static final boolean DBG = false;
    private static final long EPOCH_PLUS_30_YEARS = 946080000000L;
    private static final String LOG_TAG = "UserManagerService";
    private static final int MAX_MANAGED_PROFILES = 1;
    private static final int MIN_USER_ID = 10;
    private static final String RESTRICTIONS_FILE_PREFIX = "res_";
    private static final String TAG_ENTRY = "entry";
    private static final String TAG_GUEST_RESTRICTIONS = "guestRestrictions";
    private static final String TAG_NAME = "name";
    private static final String TAG_RESTRICTIONS = "restrictions";
    private static final String TAG_USER = "user";
    private static final String TAG_USERS = "users";
    private static final String TAG_VALUE = "value";
    private static final String USER_LIST_FILENAME = "userlist.xml";
    private static final String USER_PHOTO_FILENAME = "photo.png";
    private static final int USER_VERSION = 5;
    private static final String XML_SUFFIX = ".xml";
    private static UserManagerService sInstance;
    private IAppOpsService mAppOpsService;
    private final File mBaseUserPath;
    private final Context mContext;
    private final Bundle mGuestRestrictions;
    private final Handler mHandler;
    private final Object mInstallLock;
    private int mNextSerialNumber;
    private final Object mPackagesLock;
    private final PackageManagerService mPm;
    private final SparseBooleanArray mRemovingUserIds;
    private final SparseArray<RestrictionsPinState> mRestrictionsPinStates;
    private int[] mUserIds;
    private final File mUserListFile;
    private final SparseArray<Bundle> mUserRestrictions;
    private int mUserVersion;
    private final SparseArray<UserInfo> mUsers;
    private final File mUsersDir;
    private static final String USER_INFO_DIR = "system" + File.separator + "users";
    private static final int[] BACKOFF_TIMES = {0, 30000, 60000, 300000, ProcessList.PSS_MAX_INTERVAL};

    class RestrictionsPinState {
        int failedAttempts;
        long lastAttemptTime;
        String pinHash;
        long salt;

        RestrictionsPinState() {
        }
    }

    public static UserManagerService getInstance() {
        UserManagerService userManagerService;
        synchronized (UserManagerService.class) {
            userManagerService = sInstance;
        }
        return userManagerService;
    }

    UserManagerService(File dataDir, File baseUserPath) {
        this(null, null, new Object(), new Object(), dataDir, baseUserPath);
    }

    UserManagerService(Context context, PackageManagerService pm, Object installLock, Object packagesLock) {
        this(context, pm, installLock, packagesLock, Environment.getDataDirectory(), new File(Environment.getDataDirectory(), TAG_USER));
    }

    private UserManagerService(Context context, PackageManagerService pm, Object installLock, Object packagesLock, File dataDir, File baseUserPath) {
        this.mUsers = new SparseArray<>();
        this.mUserRestrictions = new SparseArray<>();
        this.mGuestRestrictions = new Bundle();
        this.mRestrictionsPinStates = new SparseArray<>();
        this.mRemovingUserIds = new SparseBooleanArray();
        this.mUserVersion = 0;
        this.mContext = context;
        this.mPm = pm;
        this.mInstallLock = installLock;
        this.mPackagesLock = packagesLock;
        this.mHandler = new Handler();
        synchronized (this.mInstallLock) {
            synchronized (this.mPackagesLock) {
                this.mUsersDir = new File(dataDir, USER_INFO_DIR);
                this.mUsersDir.mkdirs();
                File userZeroDir = new File(this.mUsersDir, "0");
                userZeroDir.mkdirs();
                this.mBaseUserPath = baseUserPath;
                FileUtils.setPermissions(this.mUsersDir.toString(), 509, -1, -1);
                this.mUserListFile = new File(this.mUsersDir, USER_LIST_FILENAME);
                initDefaultGuestRestrictions();
                readUserListLocked();
                ArrayList<UserInfo> partials = new ArrayList<>();
                for (int i = 0; i < this.mUsers.size(); i++) {
                    UserInfo ui = this.mUsers.valueAt(i);
                    if ((ui.partial || ui.guestToRemove) && i != 0) {
                        partials.add(ui);
                    }
                }
                for (int i2 = 0; i2 < partials.size(); i2++) {
                    UserInfo ui2 = partials.get(i2);
                    Slog.w(LOG_TAG, "Removing partially created user #" + i2 + " (name=" + ui2.name + ")");
                    removeUserStateLocked(ui2.id);
                }
                sInstance = this;
            }
        }
    }

    void systemReady() {
        UserInfo currentGuestUser;
        userForeground(0);
        this.mAppOpsService = IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
        for (int i = 0; i < this.mUserIds.length; i++) {
            try {
                this.mAppOpsService.setUserRestrictions(this.mUserRestrictions.get(this.mUserIds[i]), this.mUserIds[i]);
            } catch (RemoteException e) {
                Log.w(LOG_TAG, "Unable to notify AppOpsService of UserRestrictions");
            }
        }
        synchronized (this.mPackagesLock) {
            currentGuestUser = findCurrentGuestUserLocked();
        }
        if (currentGuestUser != null && !hasUserRestriction("no_config_wifi", currentGuestUser.id)) {
            Bundle userRestrictionsToSet = new Bundle();
            userRestrictionsToSet.putBoolean("no_config_wifi", true);
            setUserRestrictions(userRestrictionsToSet, currentGuestUser.id);
        }
    }

    public List<UserInfo> getUsers(boolean excludeDying) {
        ArrayList<UserInfo> users;
        checkManageOrCreateUsersPermission("query users");
        synchronized (this.mPackagesLock) {
            users = new ArrayList<>(this.mUsers.size());
            for (int i = 0; i < this.mUsers.size(); i++) {
                UserInfo ui = this.mUsers.valueAt(i);
                if (!ui.partial && (!excludeDying || !this.mRemovingUserIds.get(ui.id))) {
                    users.add(ui);
                }
            }
        }
        return users;
    }

    public List<UserInfo> getProfiles(int userId, boolean enabledOnly) {
        List<UserInfo> profilesLocked;
        if (userId != UserHandle.getCallingUserId()) {
            checkManageOrCreateUsersPermission("getting profiles related to user " + userId);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPackagesLock) {
                profilesLocked = getProfilesLocked(userId, enabledOnly);
            }
            return profilesLocked;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private List<UserInfo> getProfilesLocked(int userId, boolean enabledOnly) {
        UserInfo user = getUserInfoLocked(userId);
        ArrayList<UserInfo> users = new ArrayList<>(this.mUsers.size());
        if (user != null) {
            for (int i = 0; i < this.mUsers.size(); i++) {
                UserInfo profile = this.mUsers.valueAt(i);
                if (isProfileOf(user, profile) && ((!enabledOnly || profile.isEnabled()) && !this.mRemovingUserIds.get(profile.id))) {
                    users.add(profile);
                }
            }
        }
        return users;
    }

    public UserInfo getProfileParent(int userHandle) {
        UserInfo userInfoLocked = null;
        checkManageUsersPermission("get the profile parent");
        synchronized (this.mPackagesLock) {
            UserInfo profile = getUserInfoLocked(userHandle);
            if (profile != null) {
                int parentUserId = profile.profileGroupId;
                if (parentUserId != -1) {
                    userInfoLocked = getUserInfoLocked(parentUserId);
                }
            }
        }
        return userInfoLocked;
    }

    private boolean isProfileOf(UserInfo user, UserInfo profile) {
        if (user.id == profile.id || (user.profileGroupId != -1 && user.profileGroupId == profile.profileGroupId)) {
            return true;
        }
        return DBG;
    }

    public void setUserEnabled(int userId) {
        checkManageUsersPermission("enable user");
        synchronized (this.mPackagesLock) {
            UserInfo info = getUserInfoLocked(userId);
            if (info != null && !info.isEnabled()) {
                info.flags ^= 64;
                writeUserLocked(info);
            }
        }
    }

    public UserInfo getUserInfo(int userId) {
        UserInfo userInfoLocked;
        checkManageOrCreateUsersPermission("query user");
        synchronized (this.mPackagesLock) {
            userInfoLocked = getUserInfoLocked(userId);
        }
        return userInfoLocked;
    }

    public boolean isRestricted() {
        boolean zIsRestricted;
        synchronized (this.mPackagesLock) {
            zIsRestricted = getUserInfoLocked(UserHandle.getCallingUserId()).isRestricted();
        }
        return zIsRestricted;
    }

    private UserInfo getUserInfoLocked(int userId) {
        UserInfo ui = this.mUsers.get(userId);
        if (ui != null && ui.partial && !this.mRemovingUserIds.get(userId)) {
            Slog.w(LOG_TAG, "getUserInfo: unknown user #" + userId);
            return null;
        }
        return ui;
    }

    public boolean exists(int userId) {
        boolean zContains;
        synchronized (this.mPackagesLock) {
            zContains = ArrayUtils.contains(this.mUserIds, userId);
        }
        return zContains;
    }

    public void setUserName(int userId, String name) {
        checkManageUsersPermission("rename users");
        boolean changed = DBG;
        synchronized (this.mPackagesLock) {
            UserInfo info = this.mUsers.get(userId);
            if (info == null || info.partial) {
                Slog.w(LOG_TAG, "setUserName: unknown user #" + userId);
                return;
            }
            if (name != null && !name.equals(info.name)) {
                info.name = name;
                writeUserLocked(info);
                changed = true;
            }
            if (changed) {
                sendUserInfoChangedBroadcast(userId);
            }
        }
    }

    public void setUserIcon(int userId, Bitmap bitmap) {
        checkManageUsersPermission("update users");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPackagesLock) {
                UserInfo info = this.mUsers.get(userId);
                if (info == null || info.partial) {
                    Slog.w(LOG_TAG, "setUserIcon: unknown user #" + userId);
                    return;
                }
                writeBitmapLocked(info, bitmap);
                writeUserLocked(info);
                sendUserInfoChangedBroadcast(userId);
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void sendUserInfoChangedBroadcast(int userId) {
        Intent changedIntent = new Intent("android.intent.action.USER_INFO_CHANGED");
        changedIntent.putExtra("android.intent.extra.user_handle", userId);
        changedIntent.addFlags(1073741824);
        this.mContext.sendBroadcastAsUser(changedIntent, UserHandle.ALL);
    }

    public Bitmap getUserIcon(int userId) {
        synchronized (this.mPackagesLock) {
            UserInfo info = this.mUsers.get(userId);
            if (info == null || info.partial) {
                Slog.w(LOG_TAG, "getUserIcon: unknown user #" + userId);
                return null;
            }
            int callingGroupId = this.mUsers.get(UserHandle.getCallingUserId()).profileGroupId;
            if (callingGroupId == -1 || callingGroupId != info.profileGroupId) {
                checkManageUsersPermission("get the icon of a user who is not related");
            }
            if (info.iconPath == null) {
                return null;
            }
            return BitmapFactory.decodeFile(info.iconPath);
        }
    }

    public void makeInitialized(int userId) {
        checkManageUsersPermission("makeInitialized");
        synchronized (this.mPackagesLock) {
            UserInfo info = this.mUsers.get(userId);
            if (info == null || info.partial) {
                Slog.w(LOG_TAG, "makeInitialized: unknown user #" + userId);
            }
            if ((info.flags & 16) == 0) {
                info.flags |= 16;
                writeUserLocked(info);
            }
        }
    }

    private void initDefaultGuestRestrictions() {
        if (this.mGuestRestrictions.isEmpty()) {
            this.mGuestRestrictions.putBoolean("no_config_wifi", true);
            this.mGuestRestrictions.putBoolean("no_outgoing_calls", true);
            this.mGuestRestrictions.putBoolean("no_sms", true);
        }
    }

    public Bundle getDefaultGuestRestrictions() {
        Bundle bundle;
        checkManageUsersPermission("getDefaultGuestRestrictions");
        synchronized (this.mPackagesLock) {
            bundle = new Bundle(this.mGuestRestrictions);
        }
        return bundle;
    }

    public void setDefaultGuestRestrictions(Bundle restrictions) {
        checkManageUsersPermission("setDefaultGuestRestrictions");
        synchronized (this.mPackagesLock) {
            this.mGuestRestrictions.clear();
            this.mGuestRestrictions.putAll(restrictions);
            writeUserListLocked();
        }
    }

    public boolean hasUserRestriction(String restrictionKey, int userId) {
        boolean z;
        synchronized (this.mPackagesLock) {
            Bundle restrictions = this.mUserRestrictions.get(userId);
            z = restrictions != null ? restrictions.getBoolean(restrictionKey) : DBG;
        }
        return z;
    }

    public Bundle getUserRestrictions(int userId) {
        Bundle bundle;
        synchronized (this.mPackagesLock) {
            Bundle restrictions = this.mUserRestrictions.get(userId);
            bundle = restrictions != null ? new Bundle(restrictions) : new Bundle();
        }
        return bundle;
    }

    public void setUserRestrictions(Bundle restrictions, int userId) {
        checkManageUsersPermission("setUserRestrictions");
        if (restrictions != null) {
            synchronized (this.mPackagesLock) {
                this.mUserRestrictions.get(userId).clear();
                this.mUserRestrictions.get(userId).putAll(restrictions);
                long token = Binder.clearCallingIdentity();
                try {
                    try {
                        this.mAppOpsService.setUserRestrictions(this.mUserRestrictions.get(userId), userId);
                    } catch (RemoteException e) {
                        Log.w(LOG_TAG, "Unable to notify AppOpsService of UserRestrictions");
                        Binder.restoreCallingIdentity(token);
                    }
                    writeUserLocked(this.mUsers.get(userId));
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }
    }

    private boolean isUserLimitReachedLocked() {
        int aliveUserCount = 0;
        int totalUserCount = this.mUsers.size();
        for (int i = 0; i < totalUserCount; i++) {
            UserInfo user = this.mUsers.valueAt(i);
            if (!this.mRemovingUserIds.get(user.id) && !user.isGuest() && !user.partial) {
                aliveUserCount++;
            }
        }
        if (aliveUserCount >= UserManager.getMaxSupportedUsers()) {
            return true;
        }
        return DBG;
    }

    private static final void checkManageUsersPermission(String message) {
        int uid = Binder.getCallingUid();
        if (uid != 1000 && uid != 0 && ActivityManager.checkComponentPermission("android.permission.MANAGE_USERS", uid, -1, true) != 0) {
            throw new SecurityException("You need MANAGE_USERS permission to: " + message);
        }
    }

    private static final void checkManageOrCreateUsersPermission(String message) {
        if (!hasManageOrCreateUsersPermission()) {
            throw new SecurityException("You either need MANAGE_USERS or CREATE_USERS permission to: " + message);
        }
    }

    private static final void checkManageOrCreateUsersPermission(int creationFlags) {
        if ((creationFlags & (-45)) == 0) {
            if (!hasManageOrCreateUsersPermission()) {
                throw new SecurityException("You either need MANAGE_USERS or CREATE_USERS permission to create an user with flags: " + creationFlags);
            }
        } else if (!hasManageUsersPermission()) {
            throw new SecurityException("You need MANAGE_USERS permission to create an user  with flags: " + creationFlags);
        }
    }

    private static final boolean hasManageUsersPermission() {
        int callingUid = Binder.getCallingUid();
        if (UserHandle.isSameApp(callingUid, 1000) || callingUid == 0 || ActivityManager.checkComponentPermission("android.permission.MANAGE_USERS", callingUid, -1, true) == 0) {
            return true;
        }
        return DBG;
    }

    private static final boolean hasManageOrCreateUsersPermission() {
        int callingUid = Binder.getCallingUid();
        if (UserHandle.isSameApp(callingUid, 1000) || callingUid == 0 || ActivityManager.checkComponentPermission("android.permission.MANAGE_USERS", callingUid, -1, true) == 0 || ActivityManager.checkComponentPermission("android.permission.CREATE_USERS", callingUid, -1, true) == 0) {
            return true;
        }
        return DBG;
    }

    private void writeBitmapLocked(UserInfo info, Bitmap bitmap) {
        try {
            File dir = new File(this.mUsersDir, Integer.toString(info.id));
            File file = new File(dir, USER_PHOTO_FILENAME);
            if (!dir.exists()) {
                dir.mkdir();
                FileUtils.setPermissions(dir.getPath(), 505, -1, -1);
            }
            Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.PNG;
            FileOutputStream os = new FileOutputStream(file);
            if (bitmap.compress(compressFormat, 100, os)) {
                info.iconPath = file.getAbsolutePath();
            }
            try {
                os.close();
            } catch (IOException e) {
            }
        } catch (FileNotFoundException e2) {
            Slog.w(LOG_TAG, "Error setting photo for user ", e2);
        }
    }

    public int[] getUserIds() {
        int[] iArr;
        synchronized (this.mPackagesLock) {
            iArr = this.mUserIds;
        }
        return iArr;
    }

    int[] getUserIdsLPr() {
        return this.mUserIds;
    }

    private void readUserListLocked() {
        int type;
        if (!this.mUserListFile.exists()) {
            fallbackToSingleUserLocked();
            return;
        }
        FileInputStream fis = null;
        AtomicFile userListFile = new AtomicFile(this.mUserListFile);
        try {
            try {
                FileInputStream fis2 = userListFile.openRead();
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(fis2, null);
                do {
                    type = parser.next();
                    if (type == 2) {
                        break;
                    }
                } while (type != 1);
                if (type != 2) {
                    Slog.e(LOG_TAG, "Unable to read user list");
                    fallbackToSingleUserLocked();
                    if (fis2 != null) {
                        try {
                            fis2.close();
                            return;
                        } catch (IOException e) {
                            return;
                        }
                    }
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
                while (true) {
                    int type2 = parser.next();
                    if (type2 == 1) {
                        break;
                    }
                    if (type2 == 2) {
                        String name = parser.getName();
                        if (name.equals(TAG_USER)) {
                            String id = parser.getAttributeValue(null, ATTR_ID);
                            UserInfo user = readUserLocked(Integer.parseInt(id));
                            if (user != null) {
                                this.mUsers.put(user.id, user);
                                if (this.mNextSerialNumber < 0 || this.mNextSerialNumber <= user.id) {
                                    this.mNextSerialNumber = user.id + 1;
                                }
                            }
                        } else if (name.equals(TAG_GUEST_RESTRICTIONS)) {
                            while (true) {
                                int type3 = parser.next();
                                if (type3 == 1 || type3 == 3) {
                                    break;
                                } else if (type3 == 2) {
                                    if (parser.getName().equals(TAG_RESTRICTIONS)) {
                                        readRestrictionsLocked(parser, this.mGuestRestrictions);
                                    }
                                }
                            }
                        }
                    }
                }
                updateUserIdsLocked();
                upgradeIfNecessaryLocked();
                if (fis2 != null) {
                    try {
                        fis2.close();
                    } catch (IOException e2) {
                    }
                }
            } catch (Throwable th) {
                if (0 != 0) {
                    try {
                        fis.close();
                    } catch (IOException e3) {
                    }
                }
                throw th;
            }
        } catch (IOException e4) {
            fallbackToSingleUserLocked();
            if (0 != 0) {
                try {
                    fis.close();
                } catch (IOException e5) {
                }
            }
        } catch (XmlPullParserException e6) {
            fallbackToSingleUserLocked();
            if (0 != 0) {
                try {
                    fis.close();
                } catch (IOException e7) {
                }
            }
        }
    }

    private void upgradeIfNecessaryLocked() {
        int userVersion = this.mUserVersion;
        if (userVersion < 1) {
            UserInfo user = this.mUsers.get(0);
            if ("Primary".equals(user.name)) {
                user.name = this.mContext.getResources().getString(R.string.mediasize_iso_c7);
                writeUserLocked(user);
            }
            userVersion = 1;
        }
        if (userVersion < 2) {
            UserInfo user2 = this.mUsers.get(0);
            if ((user2.flags & 16) == 0) {
                user2.flags |= 16;
                writeUserLocked(user2);
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
        if (userVersion < 5) {
            Slog.w(LOG_TAG, "User version " + this.mUserVersion + " didn't upgrade as expected to 5");
        } else {
            this.mUserVersion = userVersion;
            writeUserListLocked();
        }
    }

    private void fallbackToSingleUserLocked() {
        UserInfo primary = new UserInfo(0, this.mContext.getResources().getString(R.string.mediasize_iso_c7), (String) null, 19);
        this.mUsers.put(0, primary);
        this.mNextSerialNumber = 10;
        this.mUserVersion = 5;
        Bundle restrictions = new Bundle();
        this.mUserRestrictions.append(0, restrictions);
        updateUserIdsLocked();
        initDefaultGuestRestrictions();
        writeUserListLocked();
        writeUserLocked(primary);
    }

    private void writeUserLocked(UserInfo userInfo) {
        FileOutputStream fos = null;
        AtomicFile userFile = new AtomicFile(new File(this.mUsersDir, userInfo.id + XML_SUFFIX));
        try {
            fos = userFile.startWrite();
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(bos, "utf-8");
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag(null, TAG_USER);
            fastXmlSerializer.attribute(null, ATTR_ID, Integer.toString(userInfo.id));
            fastXmlSerializer.attribute(null, ATTR_SERIAL_NO, Integer.toString(userInfo.serialNumber));
            fastXmlSerializer.attribute(null, ATTR_FLAGS, Integer.toString(userInfo.flags));
            fastXmlSerializer.attribute(null, ATTR_CREATION_TIME, Long.toString(userInfo.creationTime));
            fastXmlSerializer.attribute(null, ATTR_LAST_LOGGED_IN_TIME, Long.toString(userInfo.lastLoggedInTime));
            RestrictionsPinState pinState = this.mRestrictionsPinStates.get(userInfo.id);
            if (pinState != null) {
                if (pinState.salt != 0) {
                    fastXmlSerializer.attribute(null, ATTR_SALT, Long.toString(pinState.salt));
                }
                if (pinState.pinHash != null) {
                    fastXmlSerializer.attribute(null, ATTR_PIN_HASH, pinState.pinHash);
                }
                if (pinState.failedAttempts != 0) {
                    fastXmlSerializer.attribute(null, ATTR_FAILED_ATTEMPTS, Integer.toString(pinState.failedAttempts));
                    fastXmlSerializer.attribute(null, ATTR_LAST_RETRY_MS, Long.toString(pinState.lastAttemptTime));
                }
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
            if (userInfo.profileGroupId != -1) {
                fastXmlSerializer.attribute(null, ATTR_PROFILE_GROUP_ID, Integer.toString(userInfo.profileGroupId));
            }
            fastXmlSerializer.startTag(null, TAG_NAME);
            fastXmlSerializer.text(userInfo.name);
            fastXmlSerializer.endTag(null, TAG_NAME);
            Bundle restrictions = this.mUserRestrictions.get(userInfo.id);
            if (restrictions != null) {
                writeRestrictionsLocked(fastXmlSerializer, restrictions);
            }
            fastXmlSerializer.endTag(null, TAG_USER);
            fastXmlSerializer.endDocument();
            userFile.finishWrite(fos);
        } catch (Exception ioe) {
            Slog.e(LOG_TAG, "Error writing user info " + userInfo.id + "\n" + ioe);
            userFile.failWrite(fos);
        }
    }

    private void writeUserListLocked() {
        FileOutputStream fos = null;
        AtomicFile userListFile = new AtomicFile(this.mUserListFile);
        try {
            fos = userListFile.startWrite();
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(bos, "utf-8");
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag(null, "users");
            fastXmlSerializer.attribute(null, ATTR_NEXT_SERIAL_NO, Integer.toString(this.mNextSerialNumber));
            fastXmlSerializer.attribute(null, ATTR_USER_VERSION, Integer.toString(this.mUserVersion));
            fastXmlSerializer.startTag(null, TAG_GUEST_RESTRICTIONS);
            writeRestrictionsLocked(fastXmlSerializer, this.mGuestRestrictions);
            fastXmlSerializer.endTag(null, TAG_GUEST_RESTRICTIONS);
            for (int i = 0; i < this.mUsers.size(); i++) {
                UserInfo user = this.mUsers.valueAt(i);
                fastXmlSerializer.startTag(null, TAG_USER);
                fastXmlSerializer.attribute(null, ATTR_ID, Integer.toString(user.id));
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

    private void writeRestrictionsLocked(XmlSerializer serializer, Bundle restrictions) throws IOException {
        serializer.startTag(null, TAG_RESTRICTIONS);
        writeBoolean(serializer, restrictions, "no_config_wifi");
        writeBoolean(serializer, restrictions, "no_modify_accounts");
        writeBoolean(serializer, restrictions, "no_install_apps");
        writeBoolean(serializer, restrictions, "no_uninstall_apps");
        writeBoolean(serializer, restrictions, "no_share_location");
        writeBoolean(serializer, restrictions, "no_install_unknown_sources");
        writeBoolean(serializer, restrictions, "no_config_bluetooth");
        writeBoolean(serializer, restrictions, "no_usb_file_transfer");
        writeBoolean(serializer, restrictions, "no_config_credentials");
        writeBoolean(serializer, restrictions, "no_remove_user");
        writeBoolean(serializer, restrictions, "no_debugging_features");
        writeBoolean(serializer, restrictions, "no_config_vpn");
        writeBoolean(serializer, restrictions, "no_config_tethering");
        writeBoolean(serializer, restrictions, "no_factory_reset");
        writeBoolean(serializer, restrictions, "no_add_user");
        writeBoolean(serializer, restrictions, "ensure_verify_apps");
        writeBoolean(serializer, restrictions, "no_config_cell_broadcasts");
        writeBoolean(serializer, restrictions, "no_config_mobile_networks");
        writeBoolean(serializer, restrictions, "no_control_apps");
        writeBoolean(serializer, restrictions, "no_physical_media");
        writeBoolean(serializer, restrictions, "no_unmute_microphone");
        writeBoolean(serializer, restrictions, "no_adjust_volume");
        writeBoolean(serializer, restrictions, "no_outgoing_calls");
        writeBoolean(serializer, restrictions, "no_sms");
        writeBoolean(serializer, restrictions, "no_create_windows");
        writeBoolean(serializer, restrictions, "no_cross_profile_copy_paste");
        writeBoolean(serializer, restrictions, "no_outgoing_beam");
        serializer.endTag(null, TAG_RESTRICTIONS);
    }

    private UserInfo readUserLocked(int id) {
        int type;
        int flags = 0;
        int serialNumber = id;
        String name = null;
        String iconPath = null;
        long creationTime = 0;
        long lastLoggedInTime = 0;
        long salt = 0;
        String pinHash = null;
        int failedAttempts = 0;
        int profileGroupId = -1;
        long lastAttemptTime = 0;
        boolean partial = DBG;
        boolean guestToRemove = DBG;
        Bundle restrictions = new Bundle();
        FileInputStream fis = null;
        try {
            AtomicFile userFile = new AtomicFile(new File(this.mUsersDir, Integer.toString(id) + XML_SUFFIX));
            FileInputStream fis2 = userFile.openRead();
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis2, null);
            do {
                type = parser.next();
                if (type == 2) {
                    break;
                }
            } while (type != 1);
            if (type != 2) {
                Slog.e(LOG_TAG, "Unable to read user " + id);
                if (fis2 == null) {
                    return null;
                }
                try {
                    fis2.close();
                    return null;
                } catch (IOException e) {
                    return null;
                }
            }
            if (type == 2 && parser.getName().equals(TAG_USER)) {
                int storedId = readIntAttribute(parser, ATTR_ID, -1);
                if (storedId != id) {
                    Slog.e(LOG_TAG, "User id does not match the file name");
                    if (fis2 == null) {
                        return null;
                    }
                    try {
                        fis2.close();
                        return null;
                    } catch (IOException e2) {
                        return null;
                    }
                }
                serialNumber = readIntAttribute(parser, ATTR_SERIAL_NO, id);
                flags = readIntAttribute(parser, ATTR_FLAGS, 0);
                iconPath = parser.getAttributeValue(null, ATTR_ICON_PATH);
                creationTime = readLongAttribute(parser, ATTR_CREATION_TIME, 0L);
                lastLoggedInTime = readLongAttribute(parser, ATTR_LAST_LOGGED_IN_TIME, 0L);
                salt = readLongAttribute(parser, ATTR_SALT, 0L);
                pinHash = parser.getAttributeValue(null, ATTR_PIN_HASH);
                failedAttempts = readIntAttribute(parser, ATTR_FAILED_ATTEMPTS, 0);
                lastAttemptTime = readLongAttribute(parser, ATTR_LAST_RETRY_MS, 0L);
                profileGroupId = readIntAttribute(parser, ATTR_PROFILE_GROUP_ID, -1);
                if (profileGroupId == -1) {
                    profileGroupId = readIntAttribute(parser, "relatedGroupId", -1);
                }
                String valueString = parser.getAttributeValue(null, ATTR_PARTIAL);
                if ("true".equals(valueString)) {
                    partial = true;
                }
                String valueString2 = parser.getAttributeValue(null, ATTR_GUEST_TO_REMOVE);
                if ("true".equals(valueString2)) {
                    guestToRemove = true;
                }
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
                            readRestrictionsLocked(parser, restrictions);
                        }
                    }
                }
            }
            UserInfo userInfo = new UserInfo(id, name, iconPath, flags);
            userInfo.serialNumber = serialNumber;
            userInfo.creationTime = creationTime;
            userInfo.lastLoggedInTime = lastLoggedInTime;
            userInfo.partial = partial;
            userInfo.guestToRemove = guestToRemove;
            userInfo.profileGroupId = profileGroupId;
            this.mUserRestrictions.append(id, restrictions);
            if (salt != 0) {
                RestrictionsPinState pinState = this.mRestrictionsPinStates.get(id);
                if (pinState == null) {
                    pinState = new RestrictionsPinState();
                    this.mRestrictionsPinStates.put(id, pinState);
                }
                pinState.salt = salt;
                pinState.pinHash = pinHash;
                pinState.failedAttempts = failedAttempts;
                pinState.lastAttemptTime = lastAttemptTime;
            }
            if (fis2 == null) {
                return userInfo;
            }
            try {
                fis2.close();
                return userInfo;
            } catch (IOException e3) {
                return userInfo;
            }
        } catch (IOException e4) {
            if (0 != 0) {
                try {
                    fis.close();
                } catch (IOException e5) {
                }
            }
            return null;
        } catch (XmlPullParserException e6) {
            if (0 != 0) {
                try {
                    fis.close();
                } catch (IOException e7) {
                }
            }
            return null;
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    fis.close();
                } catch (IOException e8) {
                }
            }
            throw th;
        }
    }

    private void readRestrictionsLocked(XmlPullParser parser, Bundle restrictions) throws IOException {
        readBoolean(parser, restrictions, "no_config_wifi");
        readBoolean(parser, restrictions, "no_modify_accounts");
        readBoolean(parser, restrictions, "no_install_apps");
        readBoolean(parser, restrictions, "no_uninstall_apps");
        readBoolean(parser, restrictions, "no_share_location");
        readBoolean(parser, restrictions, "no_install_unknown_sources");
        readBoolean(parser, restrictions, "no_config_bluetooth");
        readBoolean(parser, restrictions, "no_usb_file_transfer");
        readBoolean(parser, restrictions, "no_config_credentials");
        readBoolean(parser, restrictions, "no_remove_user");
        readBoolean(parser, restrictions, "no_debugging_features");
        readBoolean(parser, restrictions, "no_config_vpn");
        readBoolean(parser, restrictions, "no_config_tethering");
        readBoolean(parser, restrictions, "no_factory_reset");
        readBoolean(parser, restrictions, "no_add_user");
        readBoolean(parser, restrictions, "ensure_verify_apps");
        readBoolean(parser, restrictions, "no_config_cell_broadcasts");
        readBoolean(parser, restrictions, "no_config_mobile_networks");
        readBoolean(parser, restrictions, "no_control_apps");
        readBoolean(parser, restrictions, "no_physical_media");
        readBoolean(parser, restrictions, "no_unmute_microphone");
        readBoolean(parser, restrictions, "no_adjust_volume");
        readBoolean(parser, restrictions, "no_outgoing_calls");
        readBoolean(parser, restrictions, "no_sms");
        readBoolean(parser, restrictions, "no_create_windows");
        readBoolean(parser, restrictions, "no_cross_profile_copy_paste");
        readBoolean(parser, restrictions, "no_outgoing_beam");
    }

    private void readBoolean(XmlPullParser parser, Bundle restrictions, String restrictionKey) {
        String value = parser.getAttributeValue(null, restrictionKey);
        if (value != null) {
            restrictions.putBoolean(restrictionKey, Boolean.parseBoolean(value));
        }
    }

    private void writeBoolean(XmlSerializer xml, Bundle restrictions, String restrictionKey) throws IOException {
        if (restrictions.containsKey(restrictionKey)) {
            xml.attribute(null, restrictionKey, Boolean.toString(restrictions.getBoolean(restrictionKey)));
        }
    }

    private int readIntAttribute(XmlPullParser parser, String attr, int defaultValue) {
        String valueString = parser.getAttributeValue(null, attr);
        if (valueString != null) {
            try {
                return Integer.parseInt(valueString);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private long readLongAttribute(XmlPullParser parser, String attr, long defaultValue) {
        String valueString = parser.getAttributeValue(null, attr);
        if (valueString != null) {
            try {
                return Long.parseLong(valueString);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private boolean isPackageInstalled(String pkg, int userId) {
        ApplicationInfo info = this.mPm.getApplicationInfo(pkg, PackageManagerService.DumpState.DUMP_INSTALLS, userId);
        if (info == null || (info.flags & 8388608) == 0) {
            return DBG;
        }
        return true;
    }

    private void cleanAppRestrictions(int userId) {
        synchronized (this.mPackagesLock) {
            File dir = Environment.getUserSystemDirectory(userId);
            String[] files = dir.list();
            if (files != null) {
                for (String fileName : files) {
                    if (fileName.startsWith(RESTRICTIONS_FILE_PREFIX)) {
                        File resFile = new File(dir, fileName);
                        if (resFile.exists()) {
                            resFile.delete();
                        }
                    }
                }
            }
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
        if (userId == 0) {
            return createUserInternal(name, flags, userId);
        }
        Slog.w(LOG_TAG, "Only user owner can have profiles");
        return null;
    }

    public UserInfo createUser(String name, int flags) {
        checkManageOrCreateUsersPermission(flags);
        return createUserInternal(name, flags, -10000);
    }

    private UserInfo createUserInternal(String name, int flags, int parentId) throws Throwable {
        if (getUserRestrictions(UserHandle.getCallingUserId()).getBoolean("no_add_user", DBG)) {
            Log.w(LOG_TAG, "Cannot add user. DISALLOW_ADD_USER is enabled.");
            return null;
        }
        boolean isGuest = (flags & 4) != 0 ? true : DBG;
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mInstallLock) {
                try {
                    synchronized (this.mPackagesLock) {
                        UserInfo parent = null;
                        try {
                            if (parentId != -10000 && (parent = getUserInfoLocked(parentId)) == null) {
                                Binder.restoreCallingIdentity(ident);
                                return null;
                            }
                            if (!isGuest && isUserLimitReachedLocked()) {
                                Binder.restoreCallingIdentity(ident);
                                return null;
                            }
                            if (isGuest && findCurrentGuestUserLocked() != null) {
                                Binder.restoreCallingIdentity(ident);
                                return null;
                            }
                            if ((flags & 32) != 0 && numberOfUsersOfTypeLocked(32, true) >= 1) {
                                Binder.restoreCallingIdentity(ident);
                                return null;
                            }
                            int userId = getNextAvailableIdLocked();
                            UserInfo userInfo = new UserInfo(userId, name, (String) null, flags);
                            try {
                                File userPath = new File(this.mBaseUserPath, Integer.toString(userId));
                                int i = this.mNextSerialNumber;
                                this.mNextSerialNumber = i + 1;
                                userInfo.serialNumber = i;
                                long now = System.currentTimeMillis();
                                if (now <= EPOCH_PLUS_30_YEARS) {
                                    now = 0;
                                }
                                userInfo.creationTime = now;
                                userInfo.partial = true;
                                Environment.getUserSystemDirectory(userInfo.id).mkdirs();
                                this.mUsers.put(userId, userInfo);
                                writeUserListLocked();
                                if (parent != null) {
                                    if (parent.profileGroupId == -1) {
                                        parent.profileGroupId = parent.id;
                                        writeUserLocked(parent);
                                    }
                                    userInfo.profileGroupId = parent.profileGroupId;
                                }
                                writeUserLocked(userInfo);
                                this.mPm.createNewUserLILPw(userId, userPath);
                                userInfo.partial = DBG;
                                writeUserLocked(userInfo);
                                updateUserIdsLocked();
                                Bundle restrictions = new Bundle();
                                this.mUserRestrictions.append(userId, restrictions);
                                try {
                                } catch (Throwable th) {
                                    th = th;
                                    throw th;
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    }
                } catch (Throwable th4) {
                    th = th4;
                }
            }
            throw th;
        } catch (Throwable th5) {
            th = th5;
        }
    }

    private int numberOfUsersOfTypeLocked(int flags, boolean excludeDying) {
        int count = 0;
        for (int i = this.mUsers.size() - 1; i >= 0; i--) {
            UserInfo user = this.mUsers.valueAt(i);
            if ((!excludeDying || !this.mRemovingUserIds.get(user.id)) && (user.flags & flags) != 0) {
                count++;
            }
        }
        return count;
    }

    private UserInfo findCurrentGuestUserLocked() {
        int size = this.mUsers.size();
        for (int i = 0; i < size; i++) {
            UserInfo user = this.mUsers.valueAt(i);
            if (user.isGuest() && !user.guestToRemove && !this.mRemovingUserIds.get(user.id)) {
                return user;
            }
        }
        return null;
    }

    public boolean markGuestForDeletion(int userHandle) {
        boolean z = DBG;
        checkManageUsersPermission("Only the system can remove users");
        if (getUserRestrictions(UserHandle.getCallingUserId()).getBoolean("no_remove_user", DBG)) {
            Log.w(LOG_TAG, "Cannot remove user. DISALLOW_REMOVE_USER is enabled.");
        } else {
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mPackagesLock) {
                    UserInfo user = this.mUsers.get(userHandle);
                    if (userHandle != 0 && user != null && !this.mRemovingUserIds.get(userHandle)) {
                        if (user.isGuest()) {
                            user.guestToRemove = true;
                            user.flags |= 64;
                            writeUserLocked(user);
                            Binder.restoreCallingIdentity(ident);
                            z = true;
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return z;
    }

    public boolean removeUser(int userHandle) {
        checkManageOrCreateUsersPermission("Only the system can remove users");
        if (getUserRestrictions(UserHandle.getCallingUserId()).getBoolean("no_remove_user", DBG)) {
            Log.w(LOG_TAG, "Cannot remove user. DISALLOW_REMOVE_USER is enabled.");
            return DBG;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPackagesLock) {
                UserInfo user = this.mUsers.get(userHandle);
                if (userHandle == 0 || user == null || this.mRemovingUserIds.get(userHandle)) {
                    return DBG;
                }
                this.mRemovingUserIds.put(userHandle, true);
                try {
                    this.mAppOpsService.removeUser(userHandle);
                } catch (RemoteException e) {
                    Log.w(LOG_TAG, "Unable to notify AppOpsService of removing user", e);
                }
                user.partial = true;
                user.flags |= 64;
                writeUserLocked(user);
                if (user.profileGroupId != -1 && user.isManagedProfile()) {
                    sendProfileRemovedBroadcast(user.profileGroupId, user.id);
                }
                try {
                    int res = ActivityManagerNative.getDefault().stopUser(userHandle, new IStopUserCallback.Stub() {
                        public void userStopped(int userId) {
                            UserManagerService.this.finishRemoveUser(userId);
                        }

                        public void userStopAborted(int userId) {
                        }
                    });
                    boolean z = res == 0;
                    Binder.restoreCallingIdentity(ident);
                    return z;
                } catch (RemoteException e2) {
                    return DBG;
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
                    new Thread() {
                        @Override
                        public void run() {
                            synchronized (UserManagerService.this.mInstallLock) {
                                synchronized (UserManagerService.this.mPackagesLock) {
                                    UserManagerService.this.removeUserStateLocked(userHandle);
                                }
                            }
                        }
                    }.start();
                }
            }, null, -1, null, null);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void removeUserStateLocked(int userHandle) {
        this.mPm.cleanUpUserLILPw(this, userHandle);
        this.mUsers.remove(userHandle);
        this.mRestrictionsPinStates.remove(userHandle);
        AtomicFile userFile = new AtomicFile(new File(this.mUsersDir, userHandle + XML_SUFFIX));
        userFile.delete();
        writeUserListLocked();
        updateUserIdsLocked();
        removeDirectoryRecursive(Environment.getUserSystemDirectory(userHandle));
    }

    private void removeDirectoryRecursive(File parent) {
        if (parent.isDirectory()) {
            String[] files = parent.list();
            for (String filename : files) {
                File child = new File(parent, filename);
                removeDirectoryRecursive(child);
            }
        }
        parent.delete();
    }

    private void sendProfileRemovedBroadcast(int parentUserId, int removedUserId) {
        Intent managedProfileIntent = new Intent("android.intent.action.MANAGED_PROFILE_REMOVED");
        managedProfileIntent.addFlags(1342177280);
        managedProfileIntent.putExtra("android.intent.extra.USER", new UserHandle(removedUserId));
        this.mContext.sendBroadcastAsUser(managedProfileIntent, new UserHandle(parentUserId), null);
    }

    public Bundle getApplicationRestrictions(String packageName) {
        return getApplicationRestrictionsForUser(packageName, UserHandle.getCallingUserId());
    }

    public Bundle getApplicationRestrictionsForUser(String packageName, int userId) {
        Bundle applicationRestrictionsLocked;
        if (UserHandle.getCallingUserId() != userId || !UserHandle.isSameApp(Binder.getCallingUid(), getUidForPackage(packageName))) {
            checkManageUsersPermission("Only system can get restrictions for other users/apps");
        }
        synchronized (this.mPackagesLock) {
            applicationRestrictionsLocked = readApplicationRestrictionsLocked(packageName, userId);
        }
        return applicationRestrictionsLocked;
    }

    public void setApplicationRestrictions(String packageName, Bundle restrictions, int userId) {
        if (UserHandle.getCallingUserId() != userId || !UserHandle.isSameApp(Binder.getCallingUid(), getUidForPackage(packageName))) {
            checkManageUsersPermission("Only system can set restrictions for other users/apps");
        }
        synchronized (this.mPackagesLock) {
            if (restrictions != null) {
                if (restrictions.isEmpty()) {
                    cleanAppRestrictionsForPackage(packageName, userId);
                } else {
                    writeApplicationRestrictionsLocked(packageName, restrictions, userId);
                }
            }
        }
        if (isPackageInstalled(packageName, userId)) {
            Intent changeIntent = new Intent("android.intent.action.APPLICATION_RESTRICTIONS_CHANGED");
            changeIntent.setPackage(packageName);
            changeIntent.addFlags(1073741824);
            this.mContext.sendBroadcastAsUser(changeIntent, new UserHandle(userId));
        }
    }

    public boolean setRestrictionsChallenge(String newPin) {
        checkManageUsersPermission("Only system can modify the restrictions pin");
        int userId = UserHandle.getCallingUserId();
        synchronized (this.mPackagesLock) {
            RestrictionsPinState pinState = this.mRestrictionsPinStates.get(userId);
            if (pinState == null) {
                pinState = new RestrictionsPinState();
            }
            if (newPin == null) {
                pinState.salt = 0L;
                pinState.pinHash = null;
            } else {
                try {
                    pinState.salt = SecureRandom.getInstance("SHA1PRNG").nextLong();
                } catch (NoSuchAlgorithmException e) {
                    pinState.salt = (long) (Math.random() * 9.223372036854776E18d);
                }
                pinState.pinHash = passwordToHash(newPin, pinState.salt);
                pinState.failedAttempts = 0;
            }
            this.mRestrictionsPinStates.put(userId, pinState);
            writeUserLocked(this.mUsers.get(userId));
        }
        return true;
    }

    public int checkRestrictionsChallenge(String pin) {
        int waitTime;
        checkManageUsersPermission("Only system can verify the restrictions pin");
        int userId = UserHandle.getCallingUserId();
        synchronized (this.mPackagesLock) {
            RestrictionsPinState pinState = this.mRestrictionsPinStates.get(userId);
            if (pinState == null || pinState.salt == 0 || pinState.pinHash == null) {
                waitTime = -2;
            } else if (pin == null) {
                waitTime = getRemainingTimeForPinAttempt(pinState);
                Slog.d(LOG_TAG, "Remaining waittime peek=" + waitTime);
            } else {
                waitTime = getRemainingTimeForPinAttempt(pinState);
                Slog.d(LOG_TAG, "Remaining waittime=" + waitTime);
                if (waitTime <= 0) {
                    if (passwordToHash(pin, pinState.salt).equals(pinState.pinHash)) {
                        pinState.failedAttempts = 0;
                        writeUserLocked(this.mUsers.get(userId));
                        waitTime = -1;
                    } else {
                        pinState.failedAttempts++;
                        pinState.lastAttemptTime = System.currentTimeMillis();
                        writeUserLocked(this.mUsers.get(userId));
                    }
                }
            }
        }
        return waitTime;
    }

    private int getRemainingTimeForPinAttempt(RestrictionsPinState pinState) {
        int backoffIndex = Math.min(pinState.failedAttempts / 5, BACKOFF_TIMES.length - 1);
        int backoffTime = pinState.failedAttempts % 5 == 0 ? BACKOFF_TIMES[backoffIndex] : 0;
        return (int) Math.max((((long) backoffTime) + pinState.lastAttemptTime) - System.currentTimeMillis(), 0L);
    }

    public boolean hasRestrictionsChallenge() {
        boolean zHasRestrictionsPinLocked;
        int userId = UserHandle.getCallingUserId();
        synchronized (this.mPackagesLock) {
            zHasRestrictionsPinLocked = hasRestrictionsPinLocked(userId);
        }
        return zHasRestrictionsPinLocked;
    }

    private boolean hasRestrictionsPinLocked(int userId) {
        RestrictionsPinState pinState = this.mRestrictionsPinStates.get(userId);
        if (pinState == null || pinState.salt == 0 || pinState.pinHash == null) {
            return DBG;
        }
        return true;
    }

    public void removeRestrictions() {
        checkManageUsersPermission("Only system can remove restrictions");
        int userHandle = UserHandle.getCallingUserId();
        removeRestrictionsForUser(userHandle, true);
    }

    private void removeRestrictionsForUser(int userHandle, boolean unhideApps) {
        synchronized (this.mPackagesLock) {
            setUserRestrictions(new Bundle(), userHandle);
            setRestrictionsChallenge(null);
            cleanAppRestrictions(userHandle);
        }
        if (unhideApps) {
            unhideAllInstalledAppsForUser(userHandle);
        }
    }

    private void unhideAllInstalledAppsForUser(final int userHandle) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                List<ApplicationInfo> apps = UserManagerService.this.mPm.getInstalledApplications(PackageManagerService.DumpState.DUMP_INSTALLS, userHandle).getList();
                long ident = Binder.clearCallingIdentity();
                try {
                    for (ApplicationInfo appInfo : apps) {
                        if ((appInfo.flags & 8388608) != 0 && (appInfo.flags & 134217728) != 0) {
                            UserManagerService.this.mPm.setApplicationHiddenSettingAsUser(appInfo.packageName, UserManagerService.DBG, userHandle);
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        });
    }

    private String passwordToHash(String password, long salt) {
        if (password == null) {
            return null;
        }
        String algo = null;
        String str = salt + password;
        try {
            byte[] saltedPassword = (password + salt).getBytes();
            byte[] sha1 = MessageDigest.getInstance("SHA-1").digest(saltedPassword);
            algo = "MD5";
            byte[] md5 = MessageDigest.getInstance("MD5").digest(saltedPassword);
            String hashed = toHex(sha1) + toHex(md5);
            return hashed;
        } catch (NoSuchAlgorithmException e) {
            Log.w(LOG_TAG, "Failed to encode string because of missing algorithm: " + algo);
            return str;
        }
    }

    private static String toHex(byte[] ary) {
        String ret = "";
        for (int i = 0; i < ary.length; i++) {
            ret = (ret + "0123456789ABCDEF".charAt((ary[i] >> 4) & 15)) + "0123456789ABCDEF".charAt(ary[i] & 15);
        }
        return ret;
    }

    private int getUidForPackage(String packageName) {
        long ident = Binder.clearCallingIdentity();
        try {
            return this.mContext.getPackageManager().getApplicationInfo(packageName, PackageManagerService.DumpState.DUMP_INSTALLS).uid;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private Bundle readApplicationRestrictionsLocked(String packageName, int userId) {
        int type;
        Bundle restrictions = new Bundle();
        ArrayList<String> values = new ArrayList<>();
        FileInputStream fis = null;
        try {
            AtomicFile restrictionsFile = new AtomicFile(new File(Environment.getUserSystemDirectory(userId), packageToRestrictionsFileName(packageName)));
            FileInputStream fis2 = restrictionsFile.openRead();
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis2, null);
            do {
                type = parser.next();
                if (type == 2) {
                    break;
                }
            } while (type != 1);
            if (type != 2) {
                Slog.e(LOG_TAG, "Unable to read restrictions file " + restrictionsFile.getBaseFile());
                if (fis2 != null) {
                    try {
                        fis2.close();
                    } catch (IOException e) {
                    }
                }
            } else {
                while (true) {
                    int type2 = parser.next();
                    if (type2 == 1) {
                        break;
                    }
                    if (type2 == 2 && parser.getName().equals(TAG_ENTRY)) {
                        String key = parser.getAttributeValue(null, ATTR_KEY);
                        String valType = parser.getAttributeValue(null, "type");
                        String multiple = parser.getAttributeValue(null, ATTR_MULTIPLE);
                        if (multiple != null) {
                            values.clear();
                            int count = Integer.parseInt(multiple);
                            while (count > 0) {
                                int type3 = parser.next();
                                if (type3 == 1) {
                                    break;
                                }
                                if (type3 == 2 && parser.getName().equals(TAG_VALUE)) {
                                    values.add(parser.nextText().trim());
                                    count--;
                                }
                            }
                            String[] valueStrings = new String[values.size()];
                            values.toArray(valueStrings);
                            restrictions.putStringArray(key, valueStrings);
                        } else {
                            String value = parser.nextText().trim();
                            if (ATTR_TYPE_BOOLEAN.equals(valType)) {
                                restrictions.putBoolean(key, Boolean.parseBoolean(value));
                            } else if (ATTR_TYPE_INTEGER.equals(valType)) {
                                restrictions.putInt(key, Integer.parseInt(value));
                            } else {
                                restrictions.putString(key, value);
                            }
                        }
                    }
                }
                if (fis2 != null) {
                    try {
                        fis2.close();
                    } catch (IOException e2) {
                    }
                }
            }
        } catch (IOException e3) {
            if (0 != 0) {
                try {
                    fis.close();
                } catch (IOException e4) {
                }
            }
        } catch (XmlPullParserException e5) {
            if (0 != 0) {
                try {
                    fis.close();
                } catch (IOException e6) {
                }
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    fis.close();
                } catch (IOException e7) {
                }
            }
            throw th;
        }
        return restrictions;
    }

    private void writeApplicationRestrictionsLocked(String packageName, Bundle restrictions, int userId) {
        FileOutputStream fos = null;
        AtomicFile restrictionsFile = new AtomicFile(new File(Environment.getUserSystemDirectory(userId), packageToRestrictionsFileName(packageName)));
        try {
            fos = restrictionsFile.startWrite();
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(bos, "utf-8");
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag(null, TAG_RESTRICTIONS);
            for (String key : restrictions.keySet()) {
                Object value = restrictions.get(key);
                fastXmlSerializer.startTag(null, TAG_ENTRY);
                fastXmlSerializer.attribute(null, ATTR_KEY, key);
                if (value instanceof Boolean) {
                    fastXmlSerializer.attribute(null, "type", ATTR_TYPE_BOOLEAN);
                    fastXmlSerializer.text(value.toString());
                } else if (value instanceof Integer) {
                    fastXmlSerializer.attribute(null, "type", ATTR_TYPE_INTEGER);
                    fastXmlSerializer.text(value.toString());
                } else if (value == null || (value instanceof String)) {
                    fastXmlSerializer.attribute(null, "type", ATTR_TYPE_STRING);
                    fastXmlSerializer.text(value != null ? (String) value : "");
                } else {
                    fastXmlSerializer.attribute(null, "type", ATTR_TYPE_STRING_ARRAY);
                    String[] values = (String[]) value;
                    fastXmlSerializer.attribute(null, ATTR_MULTIPLE, Integer.toString(values.length));
                    for (String choice : values) {
                        fastXmlSerializer.startTag(null, TAG_VALUE);
                        if (choice == null) {
                            choice = "";
                        }
                        fastXmlSerializer.text(choice);
                        fastXmlSerializer.endTag(null, TAG_VALUE);
                    }
                }
                fastXmlSerializer.endTag(null, TAG_ENTRY);
            }
            fastXmlSerializer.endTag(null, TAG_RESTRICTIONS);
            fastXmlSerializer.endDocument();
            restrictionsFile.finishWrite(fos);
        } catch (Exception e) {
            restrictionsFile.failWrite(fos);
            Slog.e(LOG_TAG, "Error writing application restrictions list");
        }
    }

    public int getUserSerialNumber(int userHandle) {
        int i;
        synchronized (this.mPackagesLock) {
            i = !exists(userHandle) ? -1 : getUserInfoLocked(userHandle).serialNumber;
        }
        return i;
    }

    public int getUserHandle(int userSerialNumber) {
        int userId;
        synchronized (this.mPackagesLock) {
            int[] arr$ = this.mUserIds;
            int len$ = arr$.length;
            int i$ = 0;
            while (true) {
                if (i$ < len$) {
                    userId = arr$[i$];
                    if (getUserInfoLocked(userId).serialNumber == userSerialNumber) {
                        break;
                    }
                    i$++;
                } else {
                    userId = -1;
                    break;
                }
            }
        }
        return userId;
    }

    private void updateUserIdsLocked() {
        int num = 0;
        for (int i = 0; i < this.mUsers.size(); i++) {
            if (!this.mUsers.valueAt(i).partial) {
                num++;
            }
        }
        int[] newUsers = new int[num];
        int n = 0;
        for (int i2 = 0; i2 < this.mUsers.size(); i2++) {
            if (!this.mUsers.valueAt(i2).partial) {
                newUsers[n] = this.mUsers.keyAt(i2);
                n++;
            }
        }
        this.mUserIds = newUsers;
    }

    public void userForeground(int userId) {
        synchronized (this.mPackagesLock) {
            UserInfo user = this.mUsers.get(userId);
            long now = System.currentTimeMillis();
            if (user == null || user.partial) {
                Slog.w(LOG_TAG, "userForeground: unknown user #" + userId);
                return;
            }
            if (now > EPOCH_PLUS_30_YEARS) {
                user.lastLoggedInTime = now;
                writeUserLocked(user);
            }
        }
    }

    private int getNextAvailableIdLocked() {
        int i;
        synchronized (this.mPackagesLock) {
            i = 10;
            while (i < Integer.MAX_VALUE) {
                if (this.mUsers.indexOfKey(i) < 0 && !this.mRemovingUserIds.get(i)) {
                    break;
                }
                i++;
            }
        }
        return i;
    }

    private String packageToRestrictionsFileName(String packageName) {
        return RESTRICTIONS_FILE_PREFIX + packageName + XML_SUFFIX;
    }

    private String restrictionsFileNameToPackage(String fileName) {
        return fileName.substring(RESTRICTIONS_FILE_PREFIX.length(), fileName.length() - XML_SUFFIX.length());
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump UserManager from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " without permission android.permission.DUMP");
            return;
        }
        long now = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        synchronized (this.mPackagesLock) {
            pw.println("Users:");
            for (int i = 0; i < this.mUsers.size(); i++) {
                UserInfo user = this.mUsers.valueAt(i);
                if (user != null) {
                    pw.print("  ");
                    pw.print(user);
                    pw.print(" serialNo=");
                    pw.print(user.serialNumber);
                    if (this.mRemovingUserIds.get(this.mUsers.keyAt(i))) {
                        pw.print(" <removing> ");
                    }
                    if (user.partial) {
                        pw.print(" <partial>");
                    }
                    pw.println();
                    pw.print("    Created: ");
                    if (user.creationTime == 0) {
                        pw.println("<unknown>");
                    } else {
                        sb.setLength(0);
                        TimeUtils.formatDuration(now - user.creationTime, sb);
                        sb.append(" ago");
                        pw.println(sb);
                    }
                    pw.print("    Last logged in: ");
                    if (user.lastLoggedInTime == 0) {
                        pw.println("<unknown>");
                    } else {
                        sb.setLength(0);
                        TimeUtils.formatDuration(now - user.lastLoggedInTime, sb);
                        sb.append(" ago");
                        pw.println(sb);
                    }
                }
            }
        }
    }
}
