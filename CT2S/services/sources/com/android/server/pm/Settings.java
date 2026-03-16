package com.android.server.pm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageCleanItem;
import android.content.pm.PackageParser;
import android.content.pm.PackageUserState;
import android.content.pm.PermissionInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.pm.UserInfo;
import android.content.pm.VerifierDeviceIdentity;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.FileUtils;
import android.os.PatternMatcher;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.JournaledFile;
import com.android.internal.util.XmlUtils;
import com.android.server.am.ProcessList;
import com.android.server.pm.PackageManagerService;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

final class Settings {
    private static final String ATTR_BLOCKED = "blocked";
    private static final String ATTR_BLOCK_UNINSTALL = "blockUninstall";
    private static final String ATTR_CODE = "code";
    private static final String ATTR_ENABLED = "enabled";
    private static final String ATTR_ENABLED_CALLER = "enabledCaller";
    private static final String ATTR_ENFORCEMENT = "enforcement";
    private static final String ATTR_HIDDEN = "hidden";
    private static final String ATTR_INSTALLED = "inst";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_NOT_LAUNCHED = "nl";
    private static final String ATTR_STOPPED = "stopped";
    private static final String ATTR_USER = "user";
    private static final int CURRENT_DATABASE_VERSION = 3;
    private static final boolean DEBUG_MU = false;
    private static final boolean DEBUG_STOPPED = false;
    private static final String TAG = "PackageSettings";
    static final String TAG_CROSS_PROFILE_INTENT_FILTERS = "crossProfile-intent-filters";
    private static final String TAG_DISABLED_COMPONENTS = "disabled-components";
    private static final String TAG_ENABLED_COMPONENTS = "enabled-components";
    private static final String TAG_ITEM = "item";
    private static final String TAG_PACKAGE = "pkg";
    private static final String TAG_PACKAGE_RESTRICTIONS = "package-restrictions";
    private static final String TAG_PERSISTENT_PREFERRED_ACTIVITIES = "persistent-preferred-activities";
    private static final String TAG_READ_EXTERNAL_STORAGE = "read-external-storage";
    private final File mBackupSettingsFilename;
    private final File mBackupStoppedPackagesFilename;
    final SparseArray<CrossProfileIntentResolver> mCrossProfileIntentResolvers;
    private final ArrayMap<String, PackageSetting> mDisabledSysPackages;
    int mExternalDatabaseVersion;
    int mExternalSdkPlatform;
    String mFingerprint;
    int mInternalDatabaseVersion;
    int mInternalSdkPlatform;
    public final KeySetManagerService mKeySetManagerService;
    private final SparseArray<Object> mOtherUserIds;
    private final File mPackageListFilename;
    final ArrayMap<String, PackageSetting> mPackages;
    final ArrayList<PackageCleanItem> mPackagesToBeCleaned;
    private final ArrayList<Signature> mPastSignatures;
    private final ArrayList<PendingPackage> mPendingPackages;
    final ArrayMap<String, BasePermission> mPermissionTrees;
    final ArrayMap<String, BasePermission> mPermissions;
    final SparseArray<PersistentPreferredIntentResolver> mPersistentPreferredActivities;
    final SparseArray<PreferredIntentResolver> mPreferredActivities;
    Boolean mReadExternalStorageEnforced;
    final StringBuilder mReadMessages;
    final ArrayMap<String, String> mRenamedPackages;
    private final File mSettingsFilename;
    final ArrayMap<String, SharedUserSetting> mSharedUsers;
    private final File mStoppedPackagesFilename;
    private final File mSystemDir;
    private final ArrayList<Object> mUserIds;
    private VerifierDeviceIdentity mVerifierDeviceIdentity;
    private static int mFirstAvailableUid = 0;
    static final Object[] FLAG_DUMP_SPEC = {1, "SYSTEM", 2, "DEBUGGABLE", 4, "HAS_CODE", 8, "PERSISTENT", 16, "FACTORY_TEST", 32, "ALLOW_TASK_REPARENTING", 64, "ALLOW_CLEAR_USER_DATA", 128, "UPDATED_SYSTEM_APP", Integer.valueOf(PackageManagerService.DumpState.DUMP_VERIFIERS), "TEST_ONLY", 16384, "VM_SAFE_MODE", 32768, "ALLOW_BACKUP", 65536, "KILL_AFTER_RESTORE", 131072, "RESTORE_ANY_VERSION", 262144, "EXTERNAL_STORAGE", 1048576, "LARGE_HEAP", 1073741824, "PRIVILEGED", 536870912, "FORWARD_LOCK", 268435456, "CANT_SAVE_STATE"};

    public static class DatabaseVersion {
        public static final int FIRST_VERSION = 1;
        public static final int SIGNATURE_END_ENTITY = 2;
        public static final int SIGNATURE_MALFORMED_RECOVER = 3;
    }

    Settings(Context context) {
        this(context, Environment.getDataDirectory());
    }

    Settings(Context context, File dataDir) {
        this.mPackages = new ArrayMap<>();
        this.mDisabledSysPackages = new ArrayMap<>();
        this.mPreferredActivities = new SparseArray<>();
        this.mPersistentPreferredActivities = new SparseArray<>();
        this.mCrossProfileIntentResolvers = new SparseArray<>();
        this.mSharedUsers = new ArrayMap<>();
        this.mUserIds = new ArrayList<>();
        this.mOtherUserIds = new SparseArray<>();
        this.mPastSignatures = new ArrayList<>();
        this.mPermissions = new ArrayMap<>();
        this.mPermissionTrees = new ArrayMap<>();
        this.mPackagesToBeCleaned = new ArrayList<>();
        this.mRenamedPackages = new ArrayMap<>();
        this.mReadMessages = new StringBuilder();
        this.mPendingPackages = new ArrayList<>();
        this.mKeySetManagerService = new KeySetManagerService(this.mPackages);
        this.mSystemDir = new File(dataDir, "system");
        this.mSystemDir.mkdirs();
        FileUtils.setPermissions(this.mSystemDir.toString(), 509, -1, -1);
        this.mSettingsFilename = new File(this.mSystemDir, "packages.xml");
        this.mBackupSettingsFilename = new File(this.mSystemDir, "packages-backup.xml");
        this.mPackageListFilename = new File(this.mSystemDir, "packages.list");
        FileUtils.setPermissions(this.mPackageListFilename, 416, 1000, 1032);
        this.mStoppedPackagesFilename = new File(this.mSystemDir, "packages-stopped.xml");
        this.mBackupStoppedPackagesFilename = new File(this.mSystemDir, "packages-stopped-backup.xml");
    }

    PackageSetting getPackageLPw(PackageParser.Package pkg, PackageSetting origPackage, String realName, SharedUserSetting sharedUser, File codePath, File resourcePath, String legacyNativeLibraryPathString, String primaryCpuAbi, String secondaryCpuAbi, int pkgFlags, UserHandle user, boolean add) {
        String name = pkg.packageName;
        PackageSetting p = getPackageLPw(name, origPackage, realName, sharedUser, codePath, resourcePath, legacyNativeLibraryPathString, primaryCpuAbi, secondaryCpuAbi, pkg.mVersionCode, pkgFlags, user, add, true);
        return p;
    }

    PackageSetting peekPackageLPr(String name) {
        return this.mPackages.get(name);
    }

    void setInstallStatus(String pkgName, int status) {
        PackageSetting p = this.mPackages.get(pkgName);
        if (p != null && p.getInstallStatus() != status) {
            p.setInstallStatus(status);
        }
    }

    void setInstallerPackageName(String pkgName, String installerPkgName) {
        PackageSetting p = this.mPackages.get(pkgName);
        if (p != null) {
            p.setInstallerPackageName(installerPkgName);
        }
    }

    SharedUserSetting getSharedUserLPw(String name, int pkgFlags, boolean create) {
        SharedUserSetting s = this.mSharedUsers.get(name);
        if (s == null) {
            if (!create) {
                return null;
            }
            s = new SharedUserSetting(name, pkgFlags);
            s.userId = newUserIdLPw(s);
            Log.i("PackageManager", "New shared user " + name + ": id=" + s.userId);
            if (s.userId >= 0) {
                this.mSharedUsers.put(name, s);
            }
        }
        return s;
    }

    Collection<SharedUserSetting> getAllSharedUsersLPw() {
        return this.mSharedUsers.values();
    }

    boolean disableSystemPackageLPw(String name) {
        PackageSetting p = this.mPackages.get(name);
        if (p == null) {
            Log.w("PackageManager", "Package:" + name + " is not an installed package");
            return false;
        }
        PackageSetting dp = this.mDisabledSysPackages.get(name);
        if (dp != null) {
            return false;
        }
        if (p.pkg != null && p.pkg.applicationInfo != null) {
            p.pkg.applicationInfo.flags |= 128;
        }
        this.mDisabledSysPackages.put(name, p);
        PackageSetting newp = new PackageSetting(p);
        replacePackageLPw(name, newp);
        return true;
    }

    PackageSetting enableSystemPackageLPw(String name) {
        PackageSetting p = this.mDisabledSysPackages.get(name);
        if (p == null) {
            Log.w("PackageManager", "Package:" + name + " is not disabled");
            return null;
        }
        if (p.pkg != null && p.pkg.applicationInfo != null) {
            p.pkg.applicationInfo.flags &= -129;
        }
        PackageSetting packageSettingAddPackageLPw = addPackageLPw(name, p.realName, p.codePath, p.resourcePath, p.legacyNativeLibraryPathString, p.primaryCpuAbiString, p.secondaryCpuAbiString, p.secondaryCpuAbiString, p.appId, p.versionCode, p.pkgFlags);
        this.mDisabledSysPackages.remove(name);
        return packageSettingAddPackageLPw;
    }

    boolean isDisabledSystemPackageLPr(String name) {
        return this.mDisabledSysPackages.containsKey(name);
    }

    void removeDisabledSystemPackageLPw(String name) {
        this.mDisabledSysPackages.remove(name);
    }

    PackageSetting addPackageLPw(String name, String realName, File codePath, File resourcePath, String legacyNativeLibraryPathString, String primaryCpuAbiString, String secondaryCpuAbiString, String cpuAbiOverrideString, int uid, int vc, int pkgFlags) {
        PackageSetting p = this.mPackages.get(name);
        if (p != null) {
            if (p.appId == uid) {
                return p;
            }
            PackageManagerService.reportSettingsProblem(6, "Adding duplicate package, keeping first: " + name);
            return null;
        }
        PackageSetting p2 = new PackageSetting(name, realName, codePath, resourcePath, legacyNativeLibraryPathString, primaryCpuAbiString, secondaryCpuAbiString, cpuAbiOverrideString, vc, pkgFlags);
        p2.appId = uid;
        if (addUserIdLPw(uid, p2, name)) {
            this.mPackages.put(name, p2);
            return p2;
        }
        return null;
    }

    SharedUserSetting addSharedUserLPw(String name, int uid, int pkgFlags) {
        SharedUserSetting s = this.mSharedUsers.get(name);
        if (s != null) {
            if (s.userId == uid) {
                return s;
            }
            PackageManagerService.reportSettingsProblem(6, "Adding duplicate shared user, keeping first: " + name);
            return null;
        }
        SharedUserSetting s2 = new SharedUserSetting(name, pkgFlags);
        s2.userId = uid;
        if (!addUserIdLPw(uid, s2, name)) {
            return null;
        }
        this.mSharedUsers.put(name, s2);
        return s2;
    }

    void pruneSharedUsersLPw() {
        ArrayList<String> removeStage = new ArrayList<>();
        for (Map.Entry<String, SharedUserSetting> entry : this.mSharedUsers.entrySet()) {
            SharedUserSetting sus = entry.getValue();
            if (sus == null || sus.packages.size() == 0) {
                removeStage.add(entry.getKey());
            }
        }
        for (int i = 0; i < removeStage.size(); i++) {
            this.mSharedUsers.remove(removeStage.get(i));
        }
    }

    void transferPermissionsLPw(String origPkg, String newPkg) {
        int i = 0;
        while (i < 2) {
            ArrayMap<String, BasePermission> permissions = i == 0 ? this.mPermissionTrees : this.mPermissions;
            for (BasePermission bp : permissions.values()) {
                if (origPkg.equals(bp.sourcePackage)) {
                    bp.sourcePackage = newPkg;
                    bp.packageSetting = null;
                    bp.perm = null;
                    if (bp.pendingInfo != null) {
                        bp.pendingInfo.packageName = newPkg;
                    }
                    bp.uid = 0;
                    bp.gids = null;
                }
            }
            i++;
        }
    }

    private PackageSetting getPackageLPw(String name, PackageSetting origPackage, String realName, SharedUserSetting sharedUser, File codePath, File resourcePath, String legacyNativeLibraryPathString, String primaryCpuAbiString, String secondaryCpuAbiString, int vc, int pkgFlags, UserHandle installUser, boolean add, boolean allowInstall) {
        List<UserInfo> users;
        PackageSetting p = this.mPackages.get(name);
        UserManagerService userManager = UserManagerService.getInstance();
        if (p != null) {
            p.primaryCpuAbiString = primaryCpuAbiString;
            p.secondaryCpuAbiString = secondaryCpuAbiString;
            if (!p.codePath.equals(codePath)) {
                if ((p.pkgFlags & 1) != 0) {
                    Slog.w("PackageManager", "Trying to update system app code path from " + p.codePathString + " to " + codePath.toString());
                } else {
                    Slog.i("PackageManager", "Package " + name + " codePath changed from " + p.codePath + " to " + codePath + "; Retaining data and using new");
                    p.legacyNativeLibraryPathString = legacyNativeLibraryPathString;
                }
            }
            if (p.sharedUser != sharedUser) {
                PackageManagerService.reportSettingsProblem(5, "Package " + name + " shared user changed from " + (p.sharedUser != null ? p.sharedUser.name : "<nothing>") + " to " + (sharedUser != null ? sharedUser.name : "<nothing>") + "; replacing with new");
                p = null;
            } else {
                int sysPrivFlags = pkgFlags & 1073741825;
                p.pkgFlags |= sysPrivFlags;
            }
        }
        if (p == null) {
            if (origPackage != null) {
                p = new PackageSetting(origPackage.name, name, codePath, resourcePath, legacyNativeLibraryPathString, primaryCpuAbiString, secondaryCpuAbiString, null, vc, pkgFlags);
                PackageSignatures s = p.signatures;
                p.copyFrom(origPackage);
                p.signatures = s;
                p.sharedUser = origPackage.sharedUser;
                p.appId = origPackage.appId;
                p.origPackage = origPackage;
                this.mRenamedPackages.put(name, origPackage.name);
                name = origPackage.name;
                p.setTimeStamp(codePath.lastModified());
            } else {
                p = new PackageSetting(name, realName, codePath, resourcePath, legacyNativeLibraryPathString, primaryCpuAbiString, secondaryCpuAbiString, null, vc, pkgFlags);
                p.setTimeStamp(codePath.lastModified());
                p.sharedUser = sharedUser;
                if ((pkgFlags & 1) == 0) {
                    List<UserInfo> users2 = getAllUsers();
                    int installUserId = installUser != null ? installUser.getIdentifier() : 0;
                    if (users2 != null && allowInstall) {
                        for (UserInfo user : users2) {
                            boolean installed = installUser == null || (installUserId == -1 && !isAdbInstallDisallowed(userManager, user.id)) || installUserId == user.id;
                            p.setUserState(user.id, 0, installed, true, true, false, null, null, null, false);
                            writePackageRestrictionsLPr(user.id);
                        }
                    }
                }
                if (sharedUser != null) {
                    p.appId = sharedUser.userId;
                } else {
                    PackageSetting dis = this.mDisabledSysPackages.get(name);
                    if (dis != null) {
                        if (dis.signatures.mSignatures != null) {
                            p.signatures.mSignatures = (Signature[]) dis.signatures.mSignatures.clone();
                        }
                        p.appId = dis.appId;
                        p.grantedPermissions = new ArraySet<>((ArraySet) dis.grantedPermissions);
                        List<UserInfo> users3 = getAllUsers();
                        if (users3 != null) {
                            Iterator<UserInfo> it = users3.iterator();
                            while (it.hasNext()) {
                                int userId = it.next().id;
                                p.setDisabledComponentsCopy(dis.getDisabledComponents(userId), userId);
                                p.setEnabledComponentsCopy(dis.getEnabledComponents(userId), userId);
                            }
                        }
                        addUserIdLPw(p.appId, p, name);
                    } else {
                        p.appId = newUserIdLPw(p);
                    }
                }
            }
            if (p.appId < 0) {
                PackageManagerService.reportSettingsProblem(5, "Package " + name + " could not be assigned a valid uid");
                return null;
            }
            if (add) {
                addPackageSettingLPw(p, name, sharedUser);
            }
        } else if (installUser != null && allowInstall && (users = getAllUsers()) != null) {
            for (UserInfo user2 : users) {
                if ((installUser.getIdentifier() == -1 && !isAdbInstallDisallowed(userManager, user2.id)) || installUser.getIdentifier() == user2.id) {
                    boolean installed2 = p.getInstalled(user2.id);
                    if (!installed2) {
                        p.setInstalled(true, user2.id);
                        writePackageRestrictionsLPr(user2.id);
                    }
                }
            }
        }
        return p;
    }

    boolean isAdbInstallDisallowed(UserManagerService userManager, int userId) {
        return userManager.hasUserRestriction("no_debugging_features", userId);
    }

    void insertPackageSettingLPw(PackageSetting p, PackageParser.Package pkg) {
        p.pkg = pkg;
        String codePath = pkg.applicationInfo.getCodePath();
        String resourcePath = pkg.applicationInfo.getResourcePath();
        String legacyNativeLibraryPath = pkg.applicationInfo.nativeLibraryRootDir;
        if (!Objects.equals(codePath, p.codePathString)) {
            Slog.w("PackageManager", "Code path for pkg : " + p.pkg.packageName + " changing from " + p.codePathString + " to " + codePath);
            p.codePath = new File(codePath);
            p.codePathString = codePath;
        }
        if (!Objects.equals(resourcePath, p.resourcePathString)) {
            Slog.w("PackageManager", "Resource path for pkg : " + p.pkg.packageName + " changing from " + p.resourcePathString + " to " + resourcePath);
            p.resourcePath = new File(resourcePath);
            p.resourcePathString = resourcePath;
        }
        if (!Objects.equals(legacyNativeLibraryPath, p.legacyNativeLibraryPathString)) {
            p.legacyNativeLibraryPathString = legacyNativeLibraryPath;
        }
        p.primaryCpuAbiString = pkg.applicationInfo.primaryCpuAbi;
        p.secondaryCpuAbiString = pkg.applicationInfo.secondaryCpuAbi;
        p.cpuAbiOverrideString = pkg.cpuAbiOverride;
        if (pkg.mVersionCode != p.versionCode) {
            p.versionCode = pkg.mVersionCode;
        }
        if (p.signatures.mSignatures == null) {
            p.signatures.assignSignatures(pkg.mSignatures);
        }
        if (pkg.applicationInfo.flags != p.pkgFlags) {
            p.pkgFlags = pkg.applicationInfo.flags;
        }
        if (p.sharedUser != null && p.sharedUser.signatures.mSignatures == null) {
            p.sharedUser.signatures.assignSignatures(pkg.mSignatures);
        }
        addPackageSettingLPw(p, pkg.packageName, p.sharedUser);
    }

    private void addPackageSettingLPw(PackageSetting p, String name, SharedUserSetting sharedUser) {
        this.mPackages.put(name, p);
        if (sharedUser != null) {
            if (p.sharedUser != null && p.sharedUser != sharedUser) {
                PackageManagerService.reportSettingsProblem(6, "Package " + p.name + " was user " + p.sharedUser + " but is now " + sharedUser + "; I am not changing its files so it will probably fail!");
                p.sharedUser.removePackage(p);
            } else if (p.appId != sharedUser.userId) {
                PackageManagerService.reportSettingsProblem(6, "Package " + p.name + " was user id " + p.appId + " but is now user " + sharedUser + " with id " + sharedUser.userId + "; I am not changing its files so it will probably fail!");
            }
            sharedUser.addPackage(p);
            p.sharedUser = sharedUser;
            p.appId = sharedUser.userId;
        }
    }

    void updateSharedUserPermsLPw(PackageSetting deletedPs, int[] globalGids) {
        if (deletedPs == null || deletedPs.pkg == null) {
            Slog.i("PackageManager", "Trying to update info for null package. Just ignoring");
            return;
        }
        if (deletedPs.sharedUser != null) {
            SharedUserSetting sus = deletedPs.sharedUser;
            for (String eachPerm : deletedPs.pkg.requestedPermissions) {
                boolean used = false;
                if (sus.grantedPermissions.contains(eachPerm)) {
                    Iterator<PackageSetting> it = sus.packages.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        PackageSetting pkg = it.next();
                        if (pkg.pkg != null && !pkg.pkg.packageName.equals(deletedPs.pkg.packageName) && pkg.pkg.requestedPermissions.contains(eachPerm)) {
                            used = true;
                            break;
                        }
                    }
                    if (!used) {
                        sus.grantedPermissions.remove(eachPerm);
                    }
                }
            }
            int[] newGids = globalGids;
            Iterator<String> it2 = sus.grantedPermissions.iterator();
            while (it2.hasNext()) {
                BasePermission bp = this.mPermissions.get(it2.next());
                if (bp != null) {
                    newGids = PackageManagerService.appendInts(newGids, bp.gids);
                }
            }
            sus.gids = newGids;
        }
    }

    int removePackageLPw(String name) {
        PackageSetting p = this.mPackages.get(name);
        if (p != null) {
            this.mPackages.remove(name);
            if (p.sharedUser != null) {
                p.sharedUser.removePackage(p);
                if (p.sharedUser.packages.size() == 0) {
                    this.mSharedUsers.remove(p.sharedUser.name);
                    removeUserIdLPw(p.sharedUser.userId);
                    return p.sharedUser.userId;
                }
            } else {
                removeUserIdLPw(p.appId);
                return p.appId;
            }
        }
        return -1;
    }

    private void replacePackageLPw(String name, PackageSetting newp) {
        PackageSetting p = this.mPackages.get(name);
        if (p != null) {
            if (p.sharedUser != null) {
                p.sharedUser.removePackage(p);
                p.sharedUser.addPackage(newp);
            } else {
                replaceUserIdLPw(p.appId, newp);
            }
        }
        this.mPackages.put(name, newp);
    }

    private boolean addUserIdLPw(int uid, Object obj, Object name) {
        if (uid > 19999) {
            return false;
        }
        if (uid >= 10000) {
            int index = uid - 10000;
            for (int N = this.mUserIds.size(); index >= N; N++) {
                this.mUserIds.add(null);
            }
            if (this.mUserIds.get(index) != null) {
                PackageManagerService.reportSettingsProblem(6, "Adding duplicate user id: " + uid + " name=" + name);
                return false;
            }
            this.mUserIds.set(index, obj);
        } else {
            if (this.mOtherUserIds.get(uid) != null) {
                PackageManagerService.reportSettingsProblem(6, "Adding duplicate shared id: " + uid + " name=" + name);
                return false;
            }
            this.mOtherUserIds.put(uid, obj);
        }
        return true;
    }

    public Object getUserIdLPr(int uid) {
        if (uid >= 10000) {
            int N = this.mUserIds.size();
            int index = uid - 10000;
            if (index < N) {
                return this.mUserIds.get(index);
            }
            return null;
        }
        return this.mOtherUserIds.get(uid);
    }

    private void removeUserIdLPw(int uid) {
        if (uid >= 10000) {
            int N = this.mUserIds.size();
            int index = uid - 10000;
            if (index < N) {
                this.mUserIds.set(index, null);
            }
        } else {
            this.mOtherUserIds.remove(uid);
        }
        setFirstAvailableUid(uid + 1);
    }

    private void replaceUserIdLPw(int uid, Object obj) {
        if (uid >= 10000) {
            int N = this.mUserIds.size();
            int index = uid - 10000;
            if (index < N) {
                this.mUserIds.set(index, obj);
                return;
            }
            return;
        }
        this.mOtherUserIds.put(uid, obj);
    }

    PreferredIntentResolver editPreferredActivitiesLPw(int userId) {
        PreferredIntentResolver pir = this.mPreferredActivities.get(userId);
        if (pir == null) {
            PreferredIntentResolver pir2 = new PreferredIntentResolver();
            this.mPreferredActivities.put(userId, pir2);
            return pir2;
        }
        return pir;
    }

    PersistentPreferredIntentResolver editPersistentPreferredActivitiesLPw(int userId) {
        PersistentPreferredIntentResolver ppir = this.mPersistentPreferredActivities.get(userId);
        if (ppir == null) {
            PersistentPreferredIntentResolver ppir2 = new PersistentPreferredIntentResolver();
            this.mPersistentPreferredActivities.put(userId, ppir2);
            return ppir2;
        }
        return ppir;
    }

    CrossProfileIntentResolver editCrossProfileIntentResolverLPw(int userId) {
        CrossProfileIntentResolver cpir = this.mCrossProfileIntentResolvers.get(userId);
        if (cpir == null) {
            CrossProfileIntentResolver cpir2 = new CrossProfileIntentResolver();
            this.mCrossProfileIntentResolvers.put(userId, cpir2);
            return cpir2;
        }
        return cpir;
    }

    private File getUserPackagesStateFile(int userId) {
        return new File(Environment.getUserSystemDirectory(userId), "package-restrictions.xml");
    }

    private File getUserPackagesStateBackupFile(int userId) {
        return new File(Environment.getUserSystemDirectory(userId), "package-restrictions-backup.xml");
    }

    void writeAllUsersPackageRestrictionsLPr() {
        List<UserInfo> users = getAllUsers();
        if (users != null) {
            for (UserInfo user : users) {
                writePackageRestrictionsLPr(user.id);
            }
        }
    }

    void readAllUsersPackageRestrictionsLPr() {
        List<UserInfo> users = getAllUsers();
        if (users == null) {
            readPackageRestrictionsLPr(0);
            return;
        }
        for (UserInfo user : users) {
            readPackageRestrictionsLPr(user.id);
        }
    }

    public boolean isInternalDatabaseVersionOlderThan(int version) {
        return this.mInternalDatabaseVersion < version;
    }

    public boolean isExternalDatabaseVersionOlderThan(int version) {
        return this.mExternalDatabaseVersion < version;
    }

    public void updateInternalDatabaseVersion() {
        this.mInternalDatabaseVersion = 3;
    }

    public void updateExternalDatabaseVersion() {
        this.mExternalDatabaseVersion = 3;
    }

    private void readPreferredActivitiesLPw(XmlPullParser parser, int userId) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type != 3 || parser.getDepth() > outerDepth) {
                if (type != 3 && type != 4) {
                    String tagName = parser.getName();
                    if (tagName.equals(TAG_ITEM)) {
                        PreferredActivity pa = new PreferredActivity(parser);
                        if (pa.mPref.getParseError() == null) {
                            editPreferredActivitiesLPw(userId).addFilter(pa);
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <preferred-activity> " + pa.mPref.getParseError() + " at " + parser.getPositionDescription());
                        }
                    } else {
                        PackageManagerService.reportSettingsProblem(5, "Unknown element under <preferred-activities>: " + parser.getName());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            } else {
                return;
            }
        }
    }

    private void readPersistentPreferredActivitiesLPw(XmlPullParser parser, int userId) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type != 3 || parser.getDepth() > outerDepth) {
                if (type != 3 && type != 4) {
                    String tagName = parser.getName();
                    if (tagName.equals(TAG_ITEM)) {
                        PersistentPreferredActivity ppa = new PersistentPreferredActivity(parser);
                        editPersistentPreferredActivitiesLPw(userId).addFilter(ppa);
                    } else {
                        PackageManagerService.reportSettingsProblem(5, "Unknown element under <persistent-preferred-activities>: " + parser.getName());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            } else {
                return;
            }
        }
    }

    private void readCrossProfileIntentFiltersLPw(XmlPullParser parser, int userId) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type != 3 || parser.getDepth() > outerDepth) {
                if (type != 3 && type != 4) {
                    String tagName = parser.getName();
                    if (tagName.equals(TAG_ITEM)) {
                        CrossProfileIntentFilter cpif = new CrossProfileIntentFilter(parser);
                        editCrossProfileIntentResolverLPw(userId).addFilter(cpif);
                    } else {
                        String msg = "Unknown element under crossProfile-intent-filters: " + parser.getName();
                        PackageManagerService.reportSettingsProblem(5, msg);
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            } else {
                return;
            }
        }
    }

    void readPackageRestrictionsLPr(int userId) {
        FileInputStream str;
        FileInputStream str2;
        int type;
        FileInputStream str3 = null;
        File userPackagesStateFile = getUserPackagesStateFile(userId);
        File backupFile = getUserPackagesStateBackupFile(userId);
        if (!backupFile.exists()) {
            str = null;
        } else {
            try {
                str = new FileInputStream(backupFile);
                try {
                    this.mReadMessages.append("Reading from backup stopped packages file\n");
                    PackageManagerService.reportSettingsProblem(4, "Need to read from backup stopped packages file");
                    if (userPackagesStateFile.exists()) {
                        Slog.w("PackageManager", "Cleaning up stopped packages file " + userPackagesStateFile);
                        userPackagesStateFile.delete();
                    }
                } catch (IOException e) {
                    str3 = str;
                    str = str3;
                }
            } catch (IOException e2) {
            }
        }
        if (str == null) {
            try {
                if (!userPackagesStateFile.exists()) {
                    this.mReadMessages.append("No stopped packages file found\n");
                    PackageManagerService.reportSettingsProblem(4, "No stopped packages file; assuming all started");
                    for (PackageSetting pkg : this.mPackages.values()) {
                        pkg.setUserState(userId, 0, true, false, false, false, null, null, null, false);
                    }
                    return;
                }
                str2 = new FileInputStream(userPackagesStateFile);
            } catch (IOException e3) {
                e = e3;
                this.mReadMessages.append("Error reading: " + e.toString());
                PackageManagerService.reportSettingsProblem(6, "Error reading settings: " + e);
                Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
            } catch (XmlPullParserException e4) {
                e = e4;
                this.mReadMessages.append("Error reading: " + e.toString());
                PackageManagerService.reportSettingsProblem(6, "Error reading stopped packages: " + e);
                Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
            }
        } else {
            str2 = str;
        }
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(str2, null);
            do {
                type = parser.next();
                if (type == 2) {
                    break;
                }
            } while (type != 1);
            if (type != 2) {
                this.mReadMessages.append("No start tag found in package restrictions file\n");
                PackageManagerService.reportSettingsProblem(5, "No start tag found in package manager stopped packages");
                return;
            }
            int outerDepth = parser.getDepth();
            while (true) {
                int type2 = parser.next();
                if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                    break;
                }
                if (type2 != 3 && type2 != 4) {
                    String tagName = parser.getName();
                    if (tagName.equals(TAG_PACKAGE)) {
                        String name = parser.getAttributeValue(null, ATTR_NAME);
                        PackageSetting ps = this.mPackages.get(name);
                        if (ps == null) {
                            Slog.w("PackageManager", "No package known for stopped package: " + name);
                            XmlUtils.skipCurrentTag(parser);
                        } else {
                            String enabledStr = parser.getAttributeValue(null, ATTR_ENABLED);
                            int enabled = enabledStr == null ? 0 : Integer.parseInt(enabledStr);
                            String enabledCaller = parser.getAttributeValue(null, ATTR_ENABLED_CALLER);
                            String installedStr = parser.getAttributeValue(null, ATTR_INSTALLED);
                            boolean installed = installedStr == null ? true : Boolean.parseBoolean(installedStr);
                            String stoppedStr = parser.getAttributeValue(null, ATTR_STOPPED);
                            boolean stopped = stoppedStr == null ? false : Boolean.parseBoolean(stoppedStr);
                            String blockedStr = parser.getAttributeValue(null, ATTR_BLOCKED);
                            boolean hidden = blockedStr == null ? false : Boolean.parseBoolean(blockedStr);
                            String hiddenStr = parser.getAttributeValue(null, ATTR_HIDDEN);
                            if (hiddenStr != null) {
                                hidden = Boolean.parseBoolean(hiddenStr);
                            }
                            String notLaunchedStr = parser.getAttributeValue(null, ATTR_NOT_LAUNCHED);
                            boolean notLaunched = stoppedStr == null ? false : Boolean.parseBoolean(notLaunchedStr);
                            String blockUninstallStr = parser.getAttributeValue(null, ATTR_BLOCK_UNINSTALL);
                            boolean blockUninstall = blockUninstallStr == null ? false : Boolean.parseBoolean(blockUninstallStr);
                            ArraySet<String> enabledComponents = null;
                            ArraySet<String> disabledComponents = null;
                            int packageDepth = parser.getDepth();
                            while (true) {
                                int type3 = parser.next();
                                if (type3 == 1 || (type3 == 3 && parser.getDepth() <= packageDepth)) {
                                    break;
                                }
                                if (type3 != 3 && type3 != 4) {
                                    String tagName2 = parser.getName();
                                    if (tagName2.equals(TAG_ENABLED_COMPONENTS)) {
                                        enabledComponents = readComponentsLPr(parser);
                                    } else if (tagName2.equals(TAG_DISABLED_COMPONENTS)) {
                                        disabledComponents = readComponentsLPr(parser);
                                    }
                                }
                            }
                            ps.setUserState(userId, enabled, installed, stopped, notLaunched, hidden, enabledCaller, enabledComponents, disabledComponents, blockUninstall);
                        }
                    } else if (tagName.equals("preferred-activities")) {
                        readPreferredActivitiesLPw(parser, userId);
                    } else if (tagName.equals(TAG_PERSISTENT_PREFERRED_ACTIVITIES)) {
                        readPersistentPreferredActivitiesLPw(parser, userId);
                    } else if (tagName.equals(TAG_CROSS_PROFILE_INTENT_FILTERS)) {
                        readCrossProfileIntentFiltersLPw(parser, userId);
                    } else {
                        Slog.w("PackageManager", "Unknown element under <stopped-packages>: " + parser.getName());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
            str2.close();
        } catch (IOException e5) {
            e = e5;
            this.mReadMessages.append("Error reading: " + e.toString());
            PackageManagerService.reportSettingsProblem(6, "Error reading settings: " + e);
            Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
        } catch (XmlPullParserException e6) {
            e = e6;
            this.mReadMessages.append("Error reading: " + e.toString());
            PackageManagerService.reportSettingsProblem(6, "Error reading stopped packages: " + e);
            Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
        }
    }

    private ArraySet<String> readComponentsLPr(XmlPullParser parser) throws XmlPullParserException, IOException {
        String componentName;
        ArraySet<String> components = null;
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if (tagName.equals(TAG_ITEM) && (componentName = parser.getAttributeValue(null, ATTR_NAME)) != null) {
                    if (components == null) {
                        components = new ArraySet<>();
                    }
                    components.add(componentName);
                }
            }
        }
        return components;
    }

    void writePreferredActivitiesLPr(XmlSerializer serializer, int userId, boolean full) throws IllegalStateException, IOException, IllegalArgumentException {
        serializer.startTag(null, "preferred-activities");
        PreferredIntentResolver pir = this.mPreferredActivities.get(userId);
        if (pir != null) {
            for (PreferredActivity pa : pir.filterSet()) {
                serializer.startTag(null, TAG_ITEM);
                pa.writeToXml(serializer, full);
                serializer.endTag(null, TAG_ITEM);
            }
        }
        serializer.endTag(null, "preferred-activities");
    }

    void writePersistentPreferredActivitiesLPr(XmlSerializer serializer, int userId) throws IllegalStateException, IOException, IllegalArgumentException {
        serializer.startTag(null, TAG_PERSISTENT_PREFERRED_ACTIVITIES);
        PersistentPreferredIntentResolver ppir = this.mPersistentPreferredActivities.get(userId);
        if (ppir != null) {
            for (PersistentPreferredActivity ppa : ppir.filterSet()) {
                serializer.startTag(null, TAG_ITEM);
                ppa.writeToXml(serializer);
                serializer.endTag(null, TAG_ITEM);
            }
        }
        serializer.endTag(null, TAG_PERSISTENT_PREFERRED_ACTIVITIES);
    }

    void writeCrossProfileIntentFiltersLPr(XmlSerializer serializer, int userId) throws IllegalStateException, IOException, IllegalArgumentException {
        serializer.startTag(null, TAG_CROSS_PROFILE_INTENT_FILTERS);
        CrossProfileIntentResolver cpir = this.mCrossProfileIntentResolvers.get(userId);
        if (cpir != null) {
            for (CrossProfileIntentFilter cpif : cpir.filterSet()) {
                serializer.startTag(null, TAG_ITEM);
                cpif.writeToXml(serializer);
                serializer.endTag(null, TAG_ITEM);
            }
        }
        serializer.endTag(null, TAG_CROSS_PROFILE_INTENT_FILTERS);
    }

    void writePackageRestrictionsLPr(int userId) {
        File userPackagesStateFile = getUserPackagesStateFile(userId);
        File backupFile = getUserPackagesStateBackupFile(userId);
        new File(userPackagesStateFile.getParent()).mkdirs();
        if (userPackagesStateFile.exists()) {
            if (!backupFile.exists()) {
                if (!userPackagesStateFile.renameTo(backupFile)) {
                    Slog.wtf("PackageManager", "Unable to backup user packages state file, current changes will be lost at reboot");
                    return;
                }
            } else {
                userPackagesStateFile.delete();
                Slog.w("PackageManager", "Preserving older stopped packages backup");
            }
        }
        try {
            FileOutputStream fstr = new FileOutputStream(userPackagesStateFile);
            BufferedOutputStream str = new BufferedOutputStream(fstr);
            XmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(str, "utf-8");
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag(null, TAG_PACKAGE_RESTRICTIONS);
            for (PackageSetting pkg : this.mPackages.values()) {
                PackageUserState ustate = pkg.readUserState(userId);
                if (ustate.stopped || ustate.notLaunched || !ustate.installed || ustate.enabled != 0 || ustate.hidden || ((ustate.enabledComponents != null && ustate.enabledComponents.size() > 0) || ((ustate.disabledComponents != null && ustate.disabledComponents.size() > 0) || ustate.blockUninstall))) {
                    fastXmlSerializer.startTag(null, TAG_PACKAGE);
                    fastXmlSerializer.attribute(null, ATTR_NAME, pkg.name);
                    if (!ustate.installed) {
                        fastXmlSerializer.attribute(null, ATTR_INSTALLED, "false");
                    }
                    if (ustate.stopped) {
                        fastXmlSerializer.attribute(null, ATTR_STOPPED, "true");
                    }
                    if (ustate.notLaunched) {
                        fastXmlSerializer.attribute(null, ATTR_NOT_LAUNCHED, "true");
                    }
                    if (ustate.hidden) {
                        fastXmlSerializer.attribute(null, ATTR_HIDDEN, "true");
                    }
                    if (ustate.blockUninstall) {
                        fastXmlSerializer.attribute(null, ATTR_BLOCK_UNINSTALL, "true");
                    }
                    if (ustate.enabled != 0) {
                        fastXmlSerializer.attribute(null, ATTR_ENABLED, Integer.toString(ustate.enabled));
                        if (ustate.lastDisableAppCaller != null) {
                            fastXmlSerializer.attribute(null, ATTR_ENABLED_CALLER, ustate.lastDisableAppCaller);
                        }
                    }
                    if (ustate.enabledComponents != null && ustate.enabledComponents.size() > 0) {
                        fastXmlSerializer.startTag(null, TAG_ENABLED_COMPONENTS);
                        for (String name : ustate.enabledComponents) {
                            fastXmlSerializer.startTag(null, TAG_ITEM);
                            fastXmlSerializer.attribute(null, ATTR_NAME, name);
                            fastXmlSerializer.endTag(null, TAG_ITEM);
                        }
                        fastXmlSerializer.endTag(null, TAG_ENABLED_COMPONENTS);
                    }
                    if (ustate.disabledComponents != null && ustate.disabledComponents.size() > 0) {
                        fastXmlSerializer.startTag(null, TAG_DISABLED_COMPONENTS);
                        for (String name2 : ustate.disabledComponents) {
                            fastXmlSerializer.startTag(null, TAG_ITEM);
                            fastXmlSerializer.attribute(null, ATTR_NAME, name2);
                            fastXmlSerializer.endTag(null, TAG_ITEM);
                        }
                        fastXmlSerializer.endTag(null, TAG_DISABLED_COMPONENTS);
                    }
                    fastXmlSerializer.endTag(null, TAG_PACKAGE);
                }
            }
            writePreferredActivitiesLPr(fastXmlSerializer, userId, true);
            writePersistentPreferredActivitiesLPr(fastXmlSerializer, userId);
            writeCrossProfileIntentFiltersLPr(fastXmlSerializer, userId);
            fastXmlSerializer.endTag(null, TAG_PACKAGE_RESTRICTIONS);
            fastXmlSerializer.endDocument();
            str.flush();
            FileUtils.sync(fstr);
            str.close();
            backupFile.delete();
            FileUtils.setPermissions(userPackagesStateFile.toString(), 432, -1, -1);
        } catch (IOException e) {
            Slog.wtf("PackageManager", "Unable to write package manager user packages state,  current changes will be lost at reboot", e);
            if (userPackagesStateFile.exists() && !userPackagesStateFile.delete()) {
                Log.i("PackageManager", "Failed to clean up mangled file: " + this.mStoppedPackagesFilename);
            }
        }
    }

    void readStoppedLPw() {
        FileInputStream str;
        FileInputStream str2;
        int type;
        FileInputStream str3 = null;
        if (!this.mBackupStoppedPackagesFilename.exists()) {
            str = null;
        } else {
            try {
                str = new FileInputStream(this.mBackupStoppedPackagesFilename);
            } catch (IOException e) {
            }
            try {
                this.mReadMessages.append("Reading from backup stopped packages file\n");
                PackageManagerService.reportSettingsProblem(4, "Need to read from backup stopped packages file");
                if (this.mSettingsFilename.exists()) {
                    Slog.w("PackageManager", "Cleaning up stopped packages file " + this.mStoppedPackagesFilename);
                    this.mStoppedPackagesFilename.delete();
                }
            } catch (IOException e2) {
                str3 = str;
                str = str3;
            }
        }
        if (str == null) {
            try {
                if (!this.mStoppedPackagesFilename.exists()) {
                    this.mReadMessages.append("No stopped packages file found\n");
                    PackageManagerService.reportSettingsProblem(4, "No stopped packages file file; assuming all started");
                    for (PackageSetting pkg : this.mPackages.values()) {
                        pkg.setStopped(false, 0);
                        pkg.setNotLaunched(false, 0);
                    }
                    return;
                }
                str2 = new FileInputStream(this.mStoppedPackagesFilename);
            } catch (IOException e3) {
                e = e3;
                this.mReadMessages.append("Error reading: " + e.toString());
                PackageManagerService.reportSettingsProblem(6, "Error reading settings: " + e);
                Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
            } catch (XmlPullParserException e4) {
                e = e4;
                this.mReadMessages.append("Error reading: " + e.toString());
                PackageManagerService.reportSettingsProblem(6, "Error reading stopped packages: " + e);
                Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
            }
        } else {
            str2 = str;
        }
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(str2, null);
            do {
                type = parser.next();
                if (type == 2) {
                    break;
                }
            } while (type != 1);
            if (type != 2) {
                this.mReadMessages.append("No start tag found in stopped packages file\n");
                PackageManagerService.reportSettingsProblem(5, "No start tag found in package manager stopped packages");
                return;
            }
            int outerDepth = parser.getDepth();
            while (true) {
                int type2 = parser.next();
                if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                    break;
                }
                if (type2 != 3 && type2 != 4) {
                    String tagName = parser.getName();
                    if (tagName.equals(TAG_PACKAGE)) {
                        String name = parser.getAttributeValue(null, ATTR_NAME);
                        PackageSetting ps = this.mPackages.get(name);
                        if (ps != null) {
                            ps.setStopped(true, 0);
                            if ("1".equals(parser.getAttributeValue(null, ATTR_NOT_LAUNCHED))) {
                                ps.setNotLaunched(true, 0);
                            }
                        } else {
                            Slog.w("PackageManager", "No package known for stopped package: " + name);
                        }
                        XmlUtils.skipCurrentTag(parser);
                    } else {
                        Slog.w("PackageManager", "Unknown element under <stopped-packages>: " + parser.getName());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
            str2.close();
        } catch (IOException e5) {
            e = e5;
            this.mReadMessages.append("Error reading: " + e.toString());
            PackageManagerService.reportSettingsProblem(6, "Error reading settings: " + e);
            Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
        } catch (XmlPullParserException e6) {
            e = e6;
            this.mReadMessages.append("Error reading: " + e.toString());
            PackageManagerService.reportSettingsProblem(6, "Error reading stopped packages: " + e);
            Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
        }
    }

    void writeLPr() {
        if (this.mSettingsFilename.exists()) {
            if (!this.mBackupSettingsFilename.exists()) {
                if (!this.mSettingsFilename.renameTo(this.mBackupSettingsFilename)) {
                    Slog.wtf("PackageManager", "Unable to backup package manager settings,  current changes will be lost at reboot");
                    return;
                }
            } else {
                this.mSettingsFilename.delete();
                Slog.w("PackageManager", "Preserving older settings backup");
            }
        }
        this.mPastSignatures.clear();
        try {
            FileOutputStream fstr = new FileOutputStream(this.mSettingsFilename);
            BufferedOutputStream str = new BufferedOutputStream(fstr);
            XmlSerializer serializer = new FastXmlSerializer();
            serializer.setOutput(str, "utf-8");
            serializer.startDocument(null, true);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startTag(null, "packages");
            serializer.startTag(null, "last-platform-version");
            serializer.attribute(null, "internal", Integer.toString(this.mInternalSdkPlatform));
            serializer.attribute(null, "external", Integer.toString(this.mExternalSdkPlatform));
            serializer.attribute(null, "fingerprint", this.mFingerprint);
            serializer.endTag(null, "last-platform-version");
            serializer.startTag(null, "database-version");
            serializer.attribute(null, "internal", Integer.toString(this.mInternalDatabaseVersion));
            serializer.attribute(null, "external", Integer.toString(this.mExternalDatabaseVersion));
            serializer.endTag(null, "database-version");
            if (this.mVerifierDeviceIdentity != null) {
                serializer.startTag(null, "verifier");
                serializer.attribute(null, "device", this.mVerifierDeviceIdentity.toString());
                serializer.endTag(null, "verifier");
            }
            if (this.mReadExternalStorageEnforced != null) {
                serializer.startTag(null, TAG_READ_EXTERNAL_STORAGE);
                serializer.attribute(null, ATTR_ENFORCEMENT, this.mReadExternalStorageEnforced.booleanValue() ? "1" : "0");
                serializer.endTag(null, TAG_READ_EXTERNAL_STORAGE);
            }
            serializer.startTag(null, "permission-trees");
            for (BasePermission bp : this.mPermissionTrees.values()) {
                writePermissionLPr(serializer, bp);
            }
            serializer.endTag(null, "permission-trees");
            serializer.startTag(null, "permissions");
            for (BasePermission bp2 : this.mPermissions.values()) {
                writePermissionLPr(serializer, bp2);
            }
            serializer.endTag(null, "permissions");
            Iterator<PackageSetting> it = this.mPackages.values().iterator();
            while (it.hasNext()) {
                writePackageLPr(serializer, it.next());
            }
            Iterator<PackageSetting> it2 = this.mDisabledSysPackages.values().iterator();
            while (it2.hasNext()) {
                writeDisabledSysPackageLPr(serializer, it2.next());
            }
            for (SharedUserSetting usr : this.mSharedUsers.values()) {
                serializer.startTag(null, "shared-user");
                serializer.attribute(null, ATTR_NAME, usr.name);
                serializer.attribute(null, "userId", Integer.toString(usr.userId));
                usr.signatures.writeXml(serializer, "sigs", this.mPastSignatures);
                serializer.startTag(null, "perms");
                for (String name : usr.grantedPermissions) {
                    serializer.startTag(null, TAG_ITEM);
                    serializer.attribute(null, ATTR_NAME, name);
                    serializer.endTag(null, TAG_ITEM);
                }
                serializer.endTag(null, "perms");
                serializer.endTag(null, "shared-user");
            }
            if (this.mPackagesToBeCleaned.size() > 0) {
                for (PackageCleanItem item : this.mPackagesToBeCleaned) {
                    String userStr = Integer.toString(item.userId);
                    serializer.startTag(null, "cleaning-package");
                    serializer.attribute(null, ATTR_NAME, item.packageName);
                    serializer.attribute(null, ATTR_CODE, item.andCode ? "true" : "false");
                    serializer.attribute(null, ATTR_USER, userStr);
                    serializer.endTag(null, "cleaning-package");
                }
            }
            if (this.mRenamedPackages.size() > 0) {
                for (Map.Entry<String, String> e : this.mRenamedPackages.entrySet()) {
                    serializer.startTag(null, "renamed-package");
                    serializer.attribute(null, "new", e.getKey());
                    serializer.attribute(null, "old", e.getValue());
                    serializer.endTag(null, "renamed-package");
                }
            }
            this.mKeySetManagerService.writeKeySetManagerServiceLPr(serializer);
            serializer.endTag(null, "packages");
            serializer.endDocument();
            str.flush();
            FileUtils.sync(fstr);
            str.close();
            this.mBackupSettingsFilename.delete();
            FileUtils.setPermissions(this.mSettingsFilename.toString(), 432, -1, -1);
            File tempFile = new File(this.mPackageListFilename.getAbsolutePath() + ".tmp");
            JournaledFile journal = new JournaledFile(this.mPackageListFilename, tempFile);
            File writeTarget = journal.chooseForWrite();
            FileOutputStream fstr2 = new FileOutputStream(writeTarget);
            BufferedOutputStream str2 = new BufferedOutputStream(fstr2);
            try {
                FileUtils.setPermissions(fstr2.getFD(), 416, 1000, 1032);
                StringBuilder sb = new StringBuilder();
                for (PackageSetting pkg : this.mPackages.values()) {
                    if (pkg.pkg == null || pkg.pkg.applicationInfo == null) {
                        Slog.w(TAG, "Skipping " + pkg + " due to missing metadata");
                    } else {
                        ApplicationInfo ai = pkg.pkg.applicationInfo;
                        String dataPath = ai.dataDir;
                        boolean isDebug = (ai.flags & 2) != 0;
                        int[] gids = pkg.getGids();
                        if (dataPath.indexOf(" ") < 0) {
                            sb.setLength(0);
                            sb.append(ai.packageName);
                            sb.append(" ");
                            sb.append(ai.uid);
                            sb.append(isDebug ? " 1 " : " 0 ");
                            sb.append(dataPath);
                            sb.append(" ");
                            sb.append(ai.seinfo);
                            sb.append(" ");
                            if (gids != null && gids.length > 0) {
                                sb.append(gids[0]);
                                for (int i = 1; i < gids.length; i++) {
                                    sb.append(",");
                                    sb.append(gids[i]);
                                }
                            } else {
                                sb.append("none");
                            }
                            sb.append("\n");
                            str2.write(sb.toString().getBytes());
                        }
                    }
                }
                str2.flush();
                FileUtils.sync(fstr2);
                str2.close();
                journal.commit();
            } catch (Exception e2) {
                Slog.wtf(TAG, "Failed to write packages.list", e2);
                IoUtils.closeQuietly(str2);
                journal.rollback();
            }
            writeAllUsersPackageRestrictionsLPr();
        } catch (IOException e3) {
            Slog.wtf("PackageManager", "Unable to write package manager settings, current changes will be lost at reboot", e3);
            if (!this.mSettingsFilename.exists() && !this.mSettingsFilename.delete()) {
                Slog.wtf("PackageManager", "Failed to clean up mangled file: " + this.mSettingsFilename);
            }
        } catch (XmlPullParserException e4) {
            Slog.wtf("PackageManager", "Unable to write package manager settings, current changes will be lost at reboot", e4);
            if (!this.mSettingsFilename.exists()) {
            }
        }
    }

    void writeDisabledSysPackageLPr(XmlSerializer serializer, PackageSetting pkg) throws IOException {
        serializer.startTag(null, "updated-package");
        serializer.attribute(null, ATTR_NAME, pkg.name);
        if (pkg.realName != null) {
            serializer.attribute(null, "realName", pkg.realName);
        }
        serializer.attribute(null, "codePath", pkg.codePathString);
        serializer.attribute(null, "ft", Long.toHexString(pkg.timeStamp));
        serializer.attribute(null, "it", Long.toHexString(pkg.firstInstallTime));
        serializer.attribute(null, "ut", Long.toHexString(pkg.lastUpdateTime));
        serializer.attribute(null, "version", String.valueOf(pkg.versionCode));
        if (!pkg.resourcePathString.equals(pkg.codePathString)) {
            serializer.attribute(null, "resourcePath", pkg.resourcePathString);
        }
        if (pkg.legacyNativeLibraryPathString != null) {
            serializer.attribute(null, "nativeLibraryPath", pkg.legacyNativeLibraryPathString);
        }
        if (pkg.primaryCpuAbiString != null) {
            serializer.attribute(null, "primaryCpuAbi", pkg.primaryCpuAbiString);
        }
        if (pkg.secondaryCpuAbiString != null) {
            serializer.attribute(null, "secondaryCpuAbi", pkg.secondaryCpuAbiString);
        }
        if (pkg.cpuAbiOverrideString != null) {
            serializer.attribute(null, "cpuAbiOverride", pkg.cpuAbiOverrideString);
        }
        if (pkg.sharedUser == null) {
            serializer.attribute(null, "userId", Integer.toString(pkg.appId));
        } else {
            serializer.attribute(null, "sharedUserId", Integer.toString(pkg.appId));
        }
        serializer.startTag(null, "perms");
        if (pkg.sharedUser == null) {
            for (String name : pkg.grantedPermissions) {
                BasePermission bp = this.mPermissions.get(name);
                if (bp != null) {
                    serializer.startTag(null, TAG_ITEM);
                    serializer.attribute(null, ATTR_NAME, name);
                    serializer.endTag(null, TAG_ITEM);
                }
            }
        }
        serializer.endTag(null, "perms");
        serializer.endTag(null, "updated-package");
    }

    void writePackageLPr(XmlSerializer serializer, PackageSetting pkg) throws IOException {
        serializer.startTag(null, "package");
        serializer.attribute(null, ATTR_NAME, pkg.name);
        if (pkg.realName != null) {
            serializer.attribute(null, "realName", pkg.realName);
        }
        serializer.attribute(null, "codePath", pkg.codePathString);
        if (!pkg.resourcePathString.equals(pkg.codePathString)) {
            serializer.attribute(null, "resourcePath", pkg.resourcePathString);
        }
        if (pkg.legacyNativeLibraryPathString != null) {
            serializer.attribute(null, "nativeLibraryPath", pkg.legacyNativeLibraryPathString);
        }
        if (pkg.primaryCpuAbiString != null) {
            serializer.attribute(null, "primaryCpuAbi", pkg.primaryCpuAbiString);
        }
        if (pkg.secondaryCpuAbiString != null) {
            serializer.attribute(null, "secondaryCpuAbi", pkg.secondaryCpuAbiString);
        }
        if (pkg.cpuAbiOverrideString != null) {
            serializer.attribute(null, "cpuAbiOverride", pkg.cpuAbiOverrideString);
        }
        serializer.attribute(null, "flags", Integer.toString(pkg.pkgFlags));
        serializer.attribute(null, "ft", Long.toHexString(pkg.timeStamp));
        serializer.attribute(null, "it", Long.toHexString(pkg.firstInstallTime));
        serializer.attribute(null, "ut", Long.toHexString(pkg.lastUpdateTime));
        serializer.attribute(null, "version", String.valueOf(pkg.versionCode));
        if (pkg.sharedUser == null) {
            serializer.attribute(null, "userId", Integer.toString(pkg.appId));
        } else {
            serializer.attribute(null, "sharedUserId", Integer.toString(pkg.appId));
        }
        if (pkg.uidError) {
            serializer.attribute(null, "uidError", "true");
        }
        if (pkg.installStatus == 0) {
            serializer.attribute(null, "installStatus", "false");
        }
        if (pkg.installerPackageName != null) {
            serializer.attribute(null, "installer", pkg.installerPackageName);
        }
        pkg.signatures.writeXml(serializer, "sigs", this.mPastSignatures);
        if ((pkg.pkgFlags & 1) == 0) {
            serializer.startTag(null, "perms");
            if (pkg.sharedUser == null) {
                for (String name : pkg.grantedPermissions) {
                    serializer.startTag(null, TAG_ITEM);
                    serializer.attribute(null, ATTR_NAME, name);
                    serializer.endTag(null, TAG_ITEM);
                }
            }
            serializer.endTag(null, "perms");
        }
        writeSigningKeySetsLPr(serializer, pkg.keySetData);
        writeUpgradeKeySetsLPr(serializer, pkg.keySetData);
        writeKeySetAliasesLPr(serializer, pkg.keySetData);
        serializer.endTag(null, "package");
    }

    void writeSigningKeySetsLPr(XmlSerializer serializer, PackageKeySetData data) throws IOException {
        if (data.getSigningKeySets() != null) {
            long properSigningKeySet = data.getProperSigningKeySet();
            serializer.startTag(null, "proper-signing-keyset");
            serializer.attribute(null, "identifier", Long.toString(properSigningKeySet));
            serializer.endTag(null, "proper-signing-keyset");
            long[] arr$ = data.getSigningKeySets();
            for (long id : arr$) {
                serializer.startTag(null, "signing-keyset");
                serializer.attribute(null, "identifier", Long.toString(id));
                serializer.endTag(null, "signing-keyset");
            }
        }
    }

    void writeUpgradeKeySetsLPr(XmlSerializer serializer, PackageKeySetData data) throws IOException {
        if (data.isUsingUpgradeKeySets()) {
            long[] arr$ = data.getUpgradeKeySets();
            for (long id : arr$) {
                serializer.startTag(null, "upgrade-keyset");
                serializer.attribute(null, "identifier", Long.toString(id));
                serializer.endTag(null, "upgrade-keyset");
            }
        }
    }

    void writeKeySetAliasesLPr(XmlSerializer serializer, PackageKeySetData data) throws IOException {
        for (Map.Entry<String, Long> e : data.getAliases().entrySet()) {
            serializer.startTag(null, "defined-keyset");
            serializer.attribute(null, "alias", e.getKey());
            serializer.attribute(null, "identifier", Long.toString(e.getValue().longValue()));
            serializer.endTag(null, "defined-keyset");
        }
    }

    void writePermissionLPr(XmlSerializer serializer, BasePermission bp) throws XmlPullParserException, IOException {
        if (bp.type != 1 && bp.sourcePackage != null) {
            serializer.startTag(null, TAG_ITEM);
            serializer.attribute(null, ATTR_NAME, bp.name);
            serializer.attribute(null, "package", bp.sourcePackage);
            if (bp.protectionLevel != 0) {
                serializer.attribute(null, "protection", Integer.toString(bp.protectionLevel));
            }
            if (bp.type == 2) {
                PermissionInfo pi = bp.perm != null ? bp.perm.info : bp.pendingInfo;
                if (pi != null) {
                    serializer.attribute(null, DatabaseHelper.SoundModelContract.KEY_TYPE, "dynamic");
                    if (pi.icon != 0) {
                        serializer.attribute(null, "icon", Integer.toString(pi.icon));
                    }
                    if (pi.nonLocalizedLabel != null) {
                        serializer.attribute(null, "label", pi.nonLocalizedLabel.toString());
                    }
                }
            }
            serializer.endTag(null, TAG_ITEM);
        }
    }

    ArrayList<PackageSetting> getListOfIncompleteInstallPackagesLPr() {
        ArraySet<String> kList = new ArraySet<>(this.mPackages.keySet());
        ArrayList<PackageSetting> ret = new ArrayList<>();
        for (String key : kList) {
            PackageSetting ps = this.mPackages.get(key);
            if (ps.getInstallStatus() == 0) {
                ret.add(ps);
            }
        }
        return ret;
    }

    void addPackageToCleanLPw(PackageCleanItem pkg) {
        if (!this.mPackagesToBeCleaned.contains(pkg)) {
            this.mPackagesToBeCleaned.add(pkg);
        }
    }

    boolean readLPw(PackageManagerService service, List<UserInfo> users, int sdkVersion, boolean onlyCore) {
        XmlPullParser parser;
        int type;
        FileInputStream str = null;
        if (this.mBackupSettingsFilename.exists()) {
            try {
                FileInputStream str2 = new FileInputStream(this.mBackupSettingsFilename);
                try {
                    this.mReadMessages.append("Reading from backup settings file\n");
                    PackageManagerService.reportSettingsProblem(4, "Need to read from backup settings file");
                    if (this.mSettingsFilename.exists()) {
                        Slog.w("PackageManager", "Cleaning up settings file " + this.mSettingsFilename);
                        this.mSettingsFilename.delete();
                    }
                    str = str2;
                } catch (IOException e) {
                    str = str2;
                }
            } catch (IOException e2) {
            }
        }
        this.mPendingPackages.clear();
        this.mPastSignatures.clear();
        if (str == null) {
            try {
                if (!this.mSettingsFilename.exists()) {
                    this.mReadMessages.append("No settings file found\n");
                    PackageManagerService.reportSettingsProblem(4, "No settings file; creating initial state");
                    this.mExternalSdkPlatform = sdkVersion;
                    this.mInternalSdkPlatform = sdkVersion;
                    this.mFingerprint = Build.FINGERPRINT;
                    return false;
                }
                str = new FileInputStream(this.mSettingsFilename);
                parser = Xml.newPullParser();
                parser.setInput(str, null);
                do {
                    type = parser.next();
                    if (type != 2) {
                        break;
                    }
                } while (type != 1);
                if (type == 2) {
                    this.mReadMessages.append("No start tag found in settings file\n");
                    PackageManagerService.reportSettingsProblem(5, "No start tag found in package manager settings");
                    Slog.wtf("PackageManager", "No start tag found in package manager settings");
                    return false;
                }
                int outerDepth = parser.getDepth();
                while (true) {
                    int type2 = parser.next();
                    if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                        break;
                    }
                    if (type2 != 3 && type2 != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals("package")) {
                            readPackageLPw(parser);
                        } else if (tagName.equals("permissions")) {
                            readPermissionsLPw(this.mPermissions, parser);
                        } else if (tagName.equals("permission-trees")) {
                            readPermissionsLPw(this.mPermissionTrees, parser);
                        } else if (tagName.equals("shared-user")) {
                            readSharedUserLPw(parser);
                        } else if (!tagName.equals("preferred-packages")) {
                            if (tagName.equals("preferred-activities")) {
                                readPreferredActivitiesLPw(parser, 0);
                            } else if (tagName.equals(TAG_PERSISTENT_PREFERRED_ACTIVITIES)) {
                                readPersistentPreferredActivitiesLPw(parser, 0);
                            } else if (tagName.equals(TAG_CROSS_PROFILE_INTENT_FILTERS)) {
                                readCrossProfileIntentFiltersLPw(parser, 0);
                            } else if (tagName.equals("updated-package")) {
                                readDisabledSysPackageLPw(parser);
                            } else if (tagName.equals("cleaning-package")) {
                                String name = parser.getAttributeValue(null, ATTR_NAME);
                                String userStr = parser.getAttributeValue(null, ATTR_USER);
                                String codeStr = parser.getAttributeValue(null, ATTR_CODE);
                                if (name != null) {
                                    int userId = 0;
                                    boolean andCode = true;
                                    if (userStr != null) {
                                        try {
                                            userId = Integer.parseInt(userStr);
                                        } catch (NumberFormatException e3) {
                                        }
                                    }
                                    if (codeStr != null) {
                                        andCode = Boolean.parseBoolean(codeStr);
                                    }
                                    addPackageToCleanLPw(new PackageCleanItem(userId, name, andCode));
                                }
                            } else if (tagName.equals("renamed-package")) {
                                String nname = parser.getAttributeValue(null, "new");
                                String oname = parser.getAttributeValue(null, "old");
                                if (nname != null && oname != null) {
                                    this.mRenamedPackages.put(nname, oname);
                                }
                            } else if (tagName.equals("last-platform-version")) {
                                this.mExternalSdkPlatform = 0;
                                this.mInternalSdkPlatform = 0;
                                try {
                                    String internal = parser.getAttributeValue(null, "internal");
                                    if (internal != null) {
                                        this.mInternalSdkPlatform = Integer.parseInt(internal);
                                    }
                                    String external = parser.getAttributeValue(null, "external");
                                    if (external != null) {
                                        this.mExternalSdkPlatform = Integer.parseInt(external);
                                    }
                                } catch (NumberFormatException e4) {
                                }
                                this.mFingerprint = parser.getAttributeValue(null, "fingerprint");
                            } else if (tagName.equals("database-version")) {
                                this.mExternalDatabaseVersion = 0;
                                this.mInternalDatabaseVersion = 0;
                                try {
                                    String internalDbVersionString = parser.getAttributeValue(null, "internal");
                                    if (internalDbVersionString != null) {
                                        this.mInternalDatabaseVersion = Integer.parseInt(internalDbVersionString);
                                    }
                                    String externalDbVersionString = parser.getAttributeValue(null, "external");
                                    if (externalDbVersionString != null) {
                                        this.mExternalDatabaseVersion = Integer.parseInt(externalDbVersionString);
                                    }
                                } catch (NumberFormatException e5) {
                                }
                            } else if (tagName.equals("verifier")) {
                                String deviceIdentity = parser.getAttributeValue(null, "device");
                                try {
                                    this.mVerifierDeviceIdentity = VerifierDeviceIdentity.parse(deviceIdentity);
                                } catch (IllegalArgumentException e6) {
                                    Slog.w("PackageManager", "Discard invalid verifier device id: " + e6.getMessage());
                                }
                            } else if (TAG_READ_EXTERNAL_STORAGE.equals(tagName)) {
                                String enforcement = parser.getAttributeValue(null, ATTR_ENFORCEMENT);
                                this.mReadExternalStorageEnforced = Boolean.valueOf("1".equals(enforcement));
                            } else if (tagName.equals("keyset-settings")) {
                                this.mKeySetManagerService.readKeySetsLPw(parser);
                            } else {
                                Slog.w("PackageManager", "Unknown element under <packages>: " + parser.getName());
                                XmlUtils.skipCurrentTag(parser);
                            }
                        }
                    }
                }
                str.close();
            } catch (IOException e7) {
                this.mReadMessages.append("Error reading: " + e7.toString());
                PackageManagerService.reportSettingsProblem(6, "Error reading settings: " + e7);
                Slog.wtf("PackageManager", "Error reading package manager settings", e7);
            } catch (XmlPullParserException e8) {
                this.mReadMessages.append("Error reading: " + e8.toString());
                PackageManagerService.reportSettingsProblem(6, "Error reading settings: " + e8);
                Slog.wtf("PackageManager", "Error reading package manager settings", e8);
            }
        } else {
            parser = Xml.newPullParser();
            parser.setInput(str, null);
            do {
                type = parser.next();
                if (type != 2) {
                }
            } while (type != 1);
            if (type == 2) {
            }
        }
        int N = this.mPendingPackages.size();
        for (int i = 0; i < N; i++) {
            PendingPackage pp = this.mPendingPackages.get(i);
            Object idObj = getUserIdLPr(pp.sharedId);
            if (idObj != null && (idObj instanceof SharedUserSetting)) {
                PackageSetting p = getPackageLPw(pp.name, null, pp.realName, (SharedUserSetting) idObj, pp.codePath, pp.resourcePath, pp.legacyNativeLibraryPathString, pp.primaryCpuAbiString, pp.secondaryCpuAbiString, pp.versionCode, pp.pkgFlags, null, true, false);
                if (p == null) {
                    PackageManagerService.reportSettingsProblem(5, "Unable to create application package for " + pp.name);
                } else {
                    p.copyFrom(pp);
                }
            } else if (idObj != null) {
                String msg = "Bad package setting: package " + pp.name + " has shared uid " + pp.sharedId + " that is not a shared uid\n";
                this.mReadMessages.append(msg);
                PackageManagerService.reportSettingsProblem(6, msg);
            } else {
                String msg2 = "Bad package setting: package " + pp.name + " has shared uid " + pp.sharedId + " that is not defined\n";
                this.mReadMessages.append(msg2);
                PackageManagerService.reportSettingsProblem(6, msg2);
            }
        }
        this.mPendingPackages.clear();
        if (this.mBackupStoppedPackagesFilename.exists() || this.mStoppedPackagesFilename.exists()) {
            readStoppedLPw();
            this.mBackupStoppedPackagesFilename.delete();
            this.mStoppedPackagesFilename.delete();
            writePackageRestrictionsLPr(0);
        } else if (users == null) {
            readPackageRestrictionsLPr(0);
        } else {
            for (UserInfo user : users) {
                readPackageRestrictionsLPr(user.id);
            }
        }
        for (PackageSetting disabledPs : this.mDisabledSysPackages.values()) {
            Object id = getUserIdLPr(disabledPs.appId);
            if (id != null && (id instanceof SharedUserSetting)) {
                disabledPs.sharedUser = (SharedUserSetting) id;
            }
        }
        this.mReadMessages.append("Read completed successfully: " + this.mPackages.size() + " packages, " + this.mSharedUsers.size() + " shared uids\n");
        return true;
    }

    void readDefaultPreferredAppsLPw(PackageManagerService service, int userId) throws Throwable {
        FileInputStream str;
        int type;
        for (PackageSetting ps : this.mPackages.values()) {
            if ((ps.pkgFlags & 1) != 0 && ps.pkg != null && ps.pkg.preferredActivityFilters != null) {
                ArrayList<PackageParser.ActivityIntentInfo> intents = ps.pkg.preferredActivityFilters;
                for (int i = 0; i < intents.size(); i++) {
                    IntentFilter aii = (PackageParser.ActivityIntentInfo) intents.get(i);
                    applyDefaultPreferredActivityLPw(service, aii, new ComponentName(ps.name, ((PackageParser.ActivityIntentInfo) aii).activity.className), userId);
                }
            }
        }
        File preferredDir = new File(Environment.getRootDirectory(), "etc/preferred-apps");
        if (preferredDir.exists() && preferredDir.isDirectory()) {
            if (!preferredDir.canRead()) {
                Slog.w(TAG, "Directory " + preferredDir + " cannot be read");
                return;
            }
            File[] arr$ = preferredDir.listFiles();
            for (File f : arr$) {
                if (!f.getPath().endsWith(".xml")) {
                    Slog.i(TAG, "Non-xml file " + f + " in " + preferredDir + " directory, ignoring");
                } else if (f.canRead()) {
                    FileInputStream str2 = null;
                    try {
                        try {
                            str = new FileInputStream(f);
                        } catch (Throwable th) {
                            th = th;
                        }
                    } catch (IOException e) {
                        e = e;
                    } catch (XmlPullParserException e2) {
                        e = e2;
                    }
                    try {
                        XmlPullParser parser = Xml.newPullParser();
                        parser.setInput(str, null);
                        do {
                            type = parser.next();
                            if (type == 2) {
                                break;
                            }
                        } while (type != 1);
                        if (type != 2) {
                            Slog.w(TAG, "Preferred apps file " + f + " does not have start tag");
                            if (str != null) {
                                try {
                                    str.close();
                                } catch (IOException e3) {
                                }
                            }
                        } else if ("preferred-activities".equals(parser.getName())) {
                            readDefaultPreferredActivitiesLPw(service, parser, userId);
                            if (str != null) {
                                try {
                                    str.close();
                                } catch (IOException e4) {
                                }
                            }
                        } else {
                            Slog.w(TAG, "Preferred apps file " + f + " does not start with 'preferred-activities'");
                            if (str != null) {
                                try {
                                    str.close();
                                } catch (IOException e5) {
                                }
                            }
                        }
                    } catch (IOException e6) {
                        e = e6;
                        str2 = str;
                        Slog.w(TAG, "Error reading apps file " + f, e);
                        if (str2 != null) {
                            try {
                                str2.close();
                            } catch (IOException e7) {
                            }
                        }
                    } catch (XmlPullParserException e8) {
                        e = e8;
                        str2 = str;
                        Slog.w(TAG, "Error reading apps file " + f, e);
                        if (str2 != null) {
                            try {
                                str2.close();
                            } catch (IOException e9) {
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        str2 = str;
                        if (str2 != null) {
                            try {
                                str2.close();
                            } catch (IOException e10) {
                            }
                        }
                        throw th;
                    }
                } else {
                    Slog.w(TAG, "Preferred apps file " + f + " cannot be read");
                }
            }
        }
    }

    private void applyDefaultPreferredActivityLPw(PackageManagerService service, IntentFilter tmpPa, ComponentName cn, int userId) {
        Intent intent = new Intent();
        int flags = 0;
        intent.setAction(tmpPa.getAction(0));
        for (int i = 0; i < tmpPa.countCategories(); i++) {
            String cat = tmpPa.getCategory(i);
            if (cat.equals("android.intent.category.DEFAULT")) {
                flags |= 65536;
            } else {
                intent.addCategory(cat);
            }
        }
        boolean doNonData = true;
        boolean hasSchemes = false;
        for (int ischeme = 0; ischeme < tmpPa.countDataSchemes(); ischeme++) {
            boolean doScheme = true;
            String scheme = tmpPa.getDataScheme(ischeme);
            if (scheme != null && !scheme.isEmpty()) {
                hasSchemes = true;
            }
            for (int issp = 0; issp < tmpPa.countDataSchemeSpecificParts(); issp++) {
                Uri.Builder builder = new Uri.Builder();
                builder.scheme(scheme);
                PatternMatcher ssp = tmpPa.getDataSchemeSpecificPart(issp);
                builder.opaquePart(ssp.getPath());
                Intent finalIntent = new Intent(intent);
                finalIntent.setData(builder.build());
                applyDefaultPreferredActivityLPw(service, finalIntent, flags, cn, scheme, ssp, null, null, userId);
                doScheme = false;
            }
            for (int iauth = 0; iauth < tmpPa.countDataAuthorities(); iauth++) {
                boolean doAuth = true;
                IntentFilter.AuthorityEntry auth = tmpPa.getDataAuthority(iauth);
                for (int ipath = 0; ipath < tmpPa.countDataPaths(); ipath++) {
                    Uri.Builder builder2 = new Uri.Builder();
                    builder2.scheme(scheme);
                    if (auth.getHost() != null) {
                        builder2.authority(auth.getHost());
                    }
                    PatternMatcher path = tmpPa.getDataPath(ipath);
                    builder2.path(path.getPath());
                    Intent finalIntent2 = new Intent(intent);
                    finalIntent2.setData(builder2.build());
                    applyDefaultPreferredActivityLPw(service, finalIntent2, flags, cn, scheme, null, auth, path, userId);
                    doScheme = false;
                    doAuth = false;
                }
                if (doAuth) {
                    Uri.Builder builder3 = new Uri.Builder();
                    builder3.scheme(scheme);
                    if (auth.getHost() != null) {
                        builder3.authority(auth.getHost());
                    }
                    Intent finalIntent3 = new Intent(intent);
                    finalIntent3.setData(builder3.build());
                    applyDefaultPreferredActivityLPw(service, finalIntent3, flags, cn, scheme, null, auth, null, userId);
                    doScheme = false;
                }
            }
            if (doScheme) {
                Uri.Builder builder4 = new Uri.Builder();
                builder4.scheme(scheme);
                Intent finalIntent4 = new Intent(intent);
                finalIntent4.setData(builder4.build());
                applyDefaultPreferredActivityLPw(service, finalIntent4, flags, cn, scheme, null, null, null, userId);
            }
            doNonData = false;
        }
        for (int idata = 0; idata < tmpPa.countDataTypes(); idata++) {
            String mimeType = tmpPa.getDataType(idata);
            if (hasSchemes) {
                Uri.Builder builder5 = new Uri.Builder();
                for (int ischeme2 = 0; ischeme2 < tmpPa.countDataSchemes(); ischeme2++) {
                    String scheme2 = tmpPa.getDataScheme(ischeme2);
                    if (scheme2 != null && !scheme2.isEmpty()) {
                        Intent finalIntent5 = new Intent(intent);
                        builder5.scheme(scheme2);
                        finalIntent5.setDataAndType(builder5.build(), mimeType);
                        applyDefaultPreferredActivityLPw(service, finalIntent5, flags, cn, scheme2, null, null, null, userId);
                    }
                }
            } else {
                Intent finalIntent6 = new Intent(intent);
                finalIntent6.setType(mimeType);
                applyDefaultPreferredActivityLPw(service, finalIntent6, flags, cn, null, null, null, null, userId);
            }
            doNonData = false;
        }
        if (doNonData) {
            applyDefaultPreferredActivityLPw(service, intent, flags, cn, null, null, null, null, userId);
        }
    }

    private void applyDefaultPreferredActivityLPw(PackageManagerService service, Intent intent, int flags, ComponentName cn, String scheme, PatternMatcher ssp, IntentFilter.AuthorityEntry auth, PatternMatcher path, int userId) {
        List<ResolveInfo> ri = service.mActivities.queryIntent(intent, intent.getType(), flags, 0);
        int systemMatch = 0;
        if (ri != null && ri.size() > 1) {
            boolean haveAct = false;
            ComponentName haveNonSys = null;
            ComponentName[] set = new ComponentName[ri.size()];
            int i = 0;
            while (true) {
                if (i >= ri.size()) {
                    break;
                }
                ActivityInfo ai = ri.get(i).activityInfo;
                set[i] = new ComponentName(ai.packageName, ai.name);
                if ((ai.applicationInfo.flags & 1) == 0) {
                    if (ri.get(i).match >= 0) {
                        haveNonSys = set[i];
                        break;
                    }
                } else if (cn.getPackageName().equals(ai.packageName) && cn.getClassName().equals(ai.name)) {
                    haveAct = true;
                    systemMatch = ri.get(i).match;
                }
                i++;
            }
            if (haveNonSys != null && 0 < systemMatch) {
                haveNonSys = null;
            }
            if (haveAct && haveNonSys == null) {
                IntentFilter filter = new IntentFilter();
                if (intent.getAction() != null) {
                    filter.addAction(intent.getAction());
                }
                if (intent.getCategories() != null) {
                    for (String cat : intent.getCategories()) {
                        filter.addCategory(cat);
                    }
                }
                if ((65536 & flags) != 0) {
                    filter.addCategory("android.intent.category.DEFAULT");
                }
                if (scheme != null) {
                    filter.addDataScheme(scheme);
                }
                if (ssp != null) {
                    filter.addDataSchemeSpecificPart(ssp.getPath(), ssp.getType());
                }
                if (auth != null) {
                    filter.addDataAuthority(auth);
                }
                if (path != null) {
                    filter.addDataPath(path);
                }
                if (intent.getType() != null) {
                    try {
                        filter.addDataType(intent.getType());
                    } catch (IntentFilter.MalformedMimeTypeException e) {
                        Slog.w(TAG, "Malformed mimetype " + intent.getType() + " for " + cn);
                    }
                }
                PreferredActivity pa = new PreferredActivity(filter, systemMatch, set, cn, true);
                editPreferredActivitiesLPw(userId).addFilter(pa);
                return;
            }
            if (haveNonSys == null) {
                StringBuilder sb = new StringBuilder();
                sb.append("No component ");
                sb.append(cn.flattenToShortString());
                sb.append(" found setting preferred ");
                sb.append(intent);
                sb.append("; possible matches are ");
                for (int i2 = 0; i2 < set.length; i2++) {
                    if (i2 > 0) {
                        sb.append(", ");
                    }
                    sb.append(set[i2].flattenToShortString());
                }
                Slog.w(TAG, sb.toString());
                return;
            }
            Slog.i(TAG, "Not setting preferred " + intent + "; found third party match " + haveNonSys.flattenToShortString());
            return;
        }
        Slog.w(TAG, "No potential matches found for " + intent + " while setting preferred " + cn.flattenToShortString());
    }

    private void readDefaultPreferredActivitiesLPw(PackageManagerService service, XmlPullParser parser, int userId) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type != 3 || parser.getDepth() > outerDepth) {
                if (type != 3 && type != 4) {
                    String tagName = parser.getName();
                    if (tagName.equals(TAG_ITEM)) {
                        PreferredActivity tmpPa = new PreferredActivity(parser);
                        if (tmpPa.mPref.getParseError() == null) {
                            applyDefaultPreferredActivityLPw(service, tmpPa, tmpPa.mPref.mComponent, userId);
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <preferred-activity> " + tmpPa.mPref.getParseError() + " at " + parser.getPositionDescription());
                        }
                    } else {
                        PackageManagerService.reportSettingsProblem(5, "Unknown element under <preferred-activities>: " + parser.getName());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            } else {
                return;
            }
        }
    }

    private int readInt(XmlPullParser parser, String ns, String name, int defValue) {
        String v = parser.getAttributeValue(ns, name);
        if (v != null) {
            try {
                return Integer.parseInt(v);
            } catch (NumberFormatException e) {
                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: attribute " + name + " has bad integer value " + v + " at " + parser.getPositionDescription());
                return defValue;
            }
        }
        return defValue;
    }

    private void readPermissionsLPw(ArrayMap<String, BasePermission> out, XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type != 3 || parser.getDepth() > outerDepth) {
                if (type != 3 && type != 4) {
                    String tagName = parser.getName();
                    if (tagName.equals(TAG_ITEM)) {
                        String name = parser.getAttributeValue(null, ATTR_NAME);
                        String sourcePackage = parser.getAttributeValue(null, "package");
                        String ptype = parser.getAttributeValue(null, DatabaseHelper.SoundModelContract.KEY_TYPE);
                        if (name != null && sourcePackage != null) {
                            boolean dynamic = "dynamic".equals(ptype);
                            BasePermission bp = new BasePermission(name, sourcePackage, dynamic ? 2 : 0);
                            bp.protectionLevel = readInt(parser, null, "protection", 0);
                            bp.protectionLevel = PermissionInfo.fixProtectionLevel(bp.protectionLevel);
                            if (dynamic) {
                                PermissionInfo pi = new PermissionInfo();
                                pi.packageName = sourcePackage.intern();
                                pi.name = name.intern();
                                pi.icon = readInt(parser, null, "icon", 0);
                                pi.nonLocalizedLabel = parser.getAttributeValue(null, "label");
                                pi.protectionLevel = bp.protectionLevel;
                                bp.pendingInfo = pi;
                            }
                            out.put(bp.name, bp);
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: permissions has no name at " + parser.getPositionDescription());
                        }
                    } else {
                        PackageManagerService.reportSettingsProblem(5, "Unknown element reading permissions: " + parser.getName() + " at " + parser.getPositionDescription());
                    }
                    XmlUtils.skipCurrentTag(parser);
                }
            } else {
                return;
            }
        }
    }

    private void readDisabledSysPackageLPw(XmlPullParser parser) throws XmlPullParserException, IOException {
        String name = parser.getAttributeValue(null, ATTR_NAME);
        String realName = parser.getAttributeValue(null, "realName");
        String codePathStr = parser.getAttributeValue(null, "codePath");
        String resourcePathStr = parser.getAttributeValue(null, "resourcePath");
        String legacyCpuAbiStr = parser.getAttributeValue(null, "requiredCpuAbi");
        String legacyNativeLibraryPathStr = parser.getAttributeValue(null, "nativeLibraryPath");
        String primaryCpuAbiStr = parser.getAttributeValue(null, "primaryCpuAbi");
        String secondaryCpuAbiStr = parser.getAttributeValue(null, "secondaryCpuAbi");
        String cpuAbiOverrideStr = parser.getAttributeValue(null, "cpuAbiOverride");
        if (primaryCpuAbiStr == null && legacyCpuAbiStr != null) {
            primaryCpuAbiStr = legacyCpuAbiStr;
        }
        if (resourcePathStr == null) {
            resourcePathStr = codePathStr;
        }
        String version = parser.getAttributeValue(null, "version");
        int versionCode = 0;
        if (version != null) {
            try {
                versionCode = Integer.parseInt(version);
            } catch (NumberFormatException e) {
            }
        }
        int pkgFlags = 0 | 1;
        File codePathFile = new File(codePathStr);
        if (PackageManagerService.locationIsPrivileged(codePathFile)) {
            pkgFlags = 1073741824 | 1;
        }
        PackageSetting ps = new PackageSetting(name, realName, codePathFile, new File(resourcePathStr), legacyNativeLibraryPathStr, primaryCpuAbiStr, secondaryCpuAbiStr, cpuAbiOverrideStr, versionCode, pkgFlags);
        String timeStampStr = parser.getAttributeValue(null, "ft");
        if (timeStampStr != null) {
            try {
                long timeStamp = Long.parseLong(timeStampStr, 16);
                ps.setTimeStamp(timeStamp);
            } catch (NumberFormatException e2) {
            }
        } else {
            String timeStampStr2 = parser.getAttributeValue(null, "ts");
            if (timeStampStr2 != null) {
                try {
                    long timeStamp2 = Long.parseLong(timeStampStr2);
                    ps.setTimeStamp(timeStamp2);
                } catch (NumberFormatException e3) {
                }
            }
        }
        String timeStampStr3 = parser.getAttributeValue(null, "it");
        if (timeStampStr3 != null) {
            try {
                ps.firstInstallTime = Long.parseLong(timeStampStr3, 16);
            } catch (NumberFormatException e4) {
            }
        }
        String timeStampStr4 = parser.getAttributeValue(null, "ut");
        if (timeStampStr4 != null) {
            try {
                ps.lastUpdateTime = Long.parseLong(timeStampStr4, 16);
            } catch (NumberFormatException e5) {
            }
        }
        String idStr = parser.getAttributeValue(null, "userId");
        ps.appId = idStr != null ? Integer.parseInt(idStr) : 0;
        if (ps.appId <= 0) {
            String sharedIdStr = parser.getAttributeValue(null, "sharedUserId");
            ps.appId = sharedIdStr != null ? Integer.parseInt(sharedIdStr) : 0;
        }
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if (tagName.equals("perms")) {
                    readGrantedPermissionsLPw(parser, ps.grantedPermissions);
                } else {
                    PackageManagerService.reportSettingsProblem(5, "Unknown element under <updated-package>: " + parser.getName());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
        this.mDisabledSysPackages.put(name, ps);
    }

    private void readPackageLPw(XmlPullParser parser) throws XmlPullParserException, IOException {
        PackageSettingBase packageSetting;
        String name = null;
        String idStr = null;
        String legacyNativeLibraryPathStr = null;
        String primaryCpuAbiString = null;
        String secondaryCpuAbiString = null;
        String installerPackageName = null;
        String uidError = null;
        int pkgFlags = 0;
        long timeStamp = 0;
        long firstInstallTime = 0;
        long lastUpdateTime = 0;
        int versionCode = 0;
        try {
            name = parser.getAttributeValue(null, ATTR_NAME);
            String realName = parser.getAttributeValue(null, "realName");
            idStr = parser.getAttributeValue(null, "userId");
            uidError = parser.getAttributeValue(null, "uidError");
            String sharedIdStr = parser.getAttributeValue(null, "sharedUserId");
            String codePathStr = parser.getAttributeValue(null, "codePath");
            String resourcePathStr = parser.getAttributeValue(null, "resourcePath");
            String legacyCpuAbiString = parser.getAttributeValue(null, "requiredCpuAbi");
            legacyNativeLibraryPathStr = parser.getAttributeValue(null, "nativeLibraryPath");
            primaryCpuAbiString = parser.getAttributeValue(null, "primaryCpuAbi");
            secondaryCpuAbiString = parser.getAttributeValue(null, "secondaryCpuAbi");
            String cpuAbiOverrideString = parser.getAttributeValue(null, "cpuAbiOverride");
            if (primaryCpuAbiString == null && legacyCpuAbiString != null) {
                primaryCpuAbiString = legacyCpuAbiString;
            }
            String version = parser.getAttributeValue(null, "version");
            if (version != null) {
                try {
                    versionCode = Integer.parseInt(version);
                } catch (NumberFormatException e) {
                }
            }
            installerPackageName = parser.getAttributeValue(null, "installer");
            String systemStr = parser.getAttributeValue(null, "flags");
            if (systemStr != null) {
                try {
                    pkgFlags = Integer.parseInt(systemStr);
                } catch (NumberFormatException e2) {
                }
            } else {
                String systemStr2 = parser.getAttributeValue(null, "system");
                if (systemStr2 != null) {
                    pkgFlags = 0 | ("true".equalsIgnoreCase(systemStr2) ? 1 : 0);
                } else {
                    pkgFlags = 0 | 1;
                }
            }
            String timeStampStr = parser.getAttributeValue(null, "ft");
            if (timeStampStr != null) {
                try {
                    timeStamp = Long.parseLong(timeStampStr, 16);
                } catch (NumberFormatException e3) {
                }
            } else {
                String timeStampStr2 = parser.getAttributeValue(null, "ts");
                if (timeStampStr2 != null) {
                    try {
                        timeStamp = Long.parseLong(timeStampStr2);
                    } catch (NumberFormatException e4) {
                    }
                }
            }
            String timeStampStr3 = parser.getAttributeValue(null, "it");
            if (timeStampStr3 != null) {
                try {
                    firstInstallTime = Long.parseLong(timeStampStr3, 16);
                } catch (NumberFormatException e5) {
                }
            }
            String timeStampStr4 = parser.getAttributeValue(null, "ut");
            if (timeStampStr4 != null) {
                try {
                    lastUpdateTime = Long.parseLong(timeStampStr4, 16);
                } catch (NumberFormatException e6) {
                }
            }
            int userId = idStr != null ? Integer.parseInt(idStr) : 0;
            if (resourcePathStr == null) {
                resourcePathStr = codePathStr;
            }
            if (realName != null) {
                realName = realName.intern();
            }
            if (name == null) {
                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <package> has no name at " + parser.getPositionDescription());
                packageSetting = null;
            } else if (codePathStr == null) {
                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <package> has no codePath at " + parser.getPositionDescription());
                packageSetting = null;
            } else {
                try {
                    if (userId > 0) {
                        packageSetting = addPackageLPw(name.intern(), realName, new File(codePathStr), new File(resourcePathStr), legacyNativeLibraryPathStr, primaryCpuAbiString, secondaryCpuAbiString, cpuAbiOverrideString, userId, versionCode, pkgFlags);
                        if (packageSetting == null) {
                            PackageManagerService.reportSettingsProblem(6, "Failure adding uid " + userId + " while parsing settings at " + parser.getPositionDescription());
                        } else {
                            packageSetting.setTimeStamp(timeStamp);
                            packageSetting.firstInstallTime = firstInstallTime;
                            packageSetting.lastUpdateTime = lastUpdateTime;
                        }
                    } else if (sharedIdStr != null) {
                        int userId2 = sharedIdStr != null ? Integer.parseInt(sharedIdStr) : 0;
                        if (userId2 > 0) {
                            packageSetting = new PendingPackage(name.intern(), realName, new File(codePathStr), new File(resourcePathStr), legacyNativeLibraryPathStr, primaryCpuAbiString, secondaryCpuAbiString, cpuAbiOverrideString, userId2, versionCode, pkgFlags);
                            packageSetting.setTimeStamp(timeStamp);
                            packageSetting.firstInstallTime = firstInstallTime;
                            packageSetting.lastUpdateTime = lastUpdateTime;
                            this.mPendingPackages.add((PendingPackage) packageSetting);
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: package " + name + " has bad sharedId " + sharedIdStr + " at " + parser.getPositionDescription());
                            packageSetting = null;
                        }
                    } else {
                        PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: package " + name + " has bad userId " + idStr + " at " + parser.getPositionDescription());
                        packageSetting = null;
                    }
                } catch (NumberFormatException e7) {
                    PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: package " + name + " has bad userId " + idStr + " at " + parser.getPositionDescription());
                }
            }
        } catch (NumberFormatException e8) {
            packageSetting = null;
        }
        if (packageSetting != null) {
            packageSetting.uidError = "true".equals(uidError);
            packageSetting.installerPackageName = installerPackageName;
            packageSetting.legacyNativeLibraryPathString = legacyNativeLibraryPathStr;
            packageSetting.primaryCpuAbiString = primaryCpuAbiString;
            packageSetting.secondaryCpuAbiString = secondaryCpuAbiString;
            String enabledStr = parser.getAttributeValue(null, ATTR_ENABLED);
            if (enabledStr != null) {
                try {
                    packageSetting.setEnabled(Integer.parseInt(enabledStr), 0, null);
                } catch (NumberFormatException e9) {
                    if (enabledStr.equalsIgnoreCase("true")) {
                        packageSetting.setEnabled(1, 0, null);
                    } else if (enabledStr.equalsIgnoreCase("false")) {
                        packageSetting.setEnabled(2, 0, null);
                    } else if (enabledStr.equalsIgnoreCase("default")) {
                        packageSetting.setEnabled(0, 0, null);
                    } else {
                        PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: package " + name + " has bad enabled value: " + idStr + " at " + parser.getPositionDescription());
                    }
                }
            } else {
                packageSetting.setEnabled(0, 0, null);
            }
            String installStatusStr = parser.getAttributeValue(null, "installStatus");
            if (installStatusStr != null) {
                if (installStatusStr.equalsIgnoreCase("false")) {
                    packageSetting.installStatus = 0;
                } else {
                    packageSetting.installStatus = 1;
                }
            }
            int outerDepth = parser.getDepth();
            while (true) {
                int type = parser.next();
                if (type == 1) {
                    return;
                }
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals(TAG_DISABLED_COMPONENTS)) {
                            readDisabledComponentsLPw(packageSetting, parser, 0);
                        } else if (tagName.equals(TAG_ENABLED_COMPONENTS)) {
                            readEnabledComponentsLPw(packageSetting, parser, 0);
                        } else if (tagName.equals("sigs")) {
                            packageSetting.signatures.readXml(parser, this.mPastSignatures);
                        } else if (tagName.equals("perms")) {
                            readGrantedPermissionsLPw(parser, packageSetting.grantedPermissions);
                            packageSetting.permissionsFixed = true;
                        } else if (tagName.equals("proper-signing-keyset")) {
                            long id = Long.parseLong(parser.getAttributeValue(null, "identifier"));
                            packageSetting.keySetData.setProperSigningKeySet(id);
                        } else if (tagName.equals("signing-keyset")) {
                            long id2 = Long.parseLong(parser.getAttributeValue(null, "identifier"));
                            packageSetting.keySetData.addSigningKeySet(id2);
                        } else if (tagName.equals("upgrade-keyset")) {
                            long id3 = Long.parseLong(parser.getAttributeValue(null, "identifier"));
                            packageSetting.keySetData.addUpgradeKeySetById(id3);
                        } else if (tagName.equals("defined-keyset")) {
                            long id4 = Long.parseLong(parser.getAttributeValue(null, "identifier"));
                            String alias = parser.getAttributeValue(null, "alias");
                            packageSetting.keySetData.addDefinedKeySet(id4, alias);
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Unknown element under <package>: " + parser.getName());
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                } else {
                    return;
                }
            }
        } else {
            XmlUtils.skipCurrentTag(parser);
        }
    }

    private void readDisabledComponentsLPw(PackageSettingBase packageSetting, XmlPullParser parser, int userId) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type != 3 || parser.getDepth() > outerDepth) {
                if (type != 3 && type != 4) {
                    String tagName = parser.getName();
                    if (tagName.equals(TAG_ITEM)) {
                        String name = parser.getAttributeValue(null, ATTR_NAME);
                        if (name != null) {
                            packageSetting.addDisabledComponent(name.intern(), userId);
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <disabled-components> has no name at " + parser.getPositionDescription());
                        }
                    } else {
                        PackageManagerService.reportSettingsProblem(5, "Unknown element under <disabled-components>: " + parser.getName());
                    }
                    XmlUtils.skipCurrentTag(parser);
                }
            } else {
                return;
            }
        }
    }

    private void readEnabledComponentsLPw(PackageSettingBase packageSetting, XmlPullParser parser, int userId) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type != 3 || parser.getDepth() > outerDepth) {
                if (type != 3 && type != 4) {
                    String tagName = parser.getName();
                    if (tagName.equals(TAG_ITEM)) {
                        String name = parser.getAttributeValue(null, ATTR_NAME);
                        if (name != null) {
                            packageSetting.addEnabledComponent(name.intern(), userId);
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <enabled-components> has no name at " + parser.getPositionDescription());
                        }
                    } else {
                        PackageManagerService.reportSettingsProblem(5, "Unknown element under <enabled-components>: " + parser.getName());
                    }
                    XmlUtils.skipCurrentTag(parser);
                }
            } else {
                return;
            }
        }
    }

    private void readSharedUserLPw(XmlPullParser parser) throws XmlPullParserException, IOException {
        int pkgFlags = 0;
        SharedUserSetting su = null;
        try {
            String name = parser.getAttributeValue(null, ATTR_NAME);
            String idStr = parser.getAttributeValue(null, "userId");
            int userId = idStr != null ? Integer.parseInt(idStr) : 0;
            if ("true".equals(parser.getAttributeValue(null, "system"))) {
                pkgFlags = 0 | 1;
            }
            if (name == null) {
                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <shared-user> has no name at " + parser.getPositionDescription());
            } else if (userId == 0) {
                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: shared-user " + name + " has bad userId " + idStr + " at " + parser.getPositionDescription());
            } else {
                su = addSharedUserLPw(name.intern(), userId, pkgFlags);
                if (su == null) {
                    PackageManagerService.reportSettingsProblem(6, "Occurred while parsing settings at " + parser.getPositionDescription());
                }
            }
        } catch (NumberFormatException e) {
            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: package " + ((String) null) + " has bad userId " + ((String) null) + " at " + parser.getPositionDescription());
        }
        if (su != null) {
            int outerDepth = parser.getDepth();
            while (true) {
                int type = parser.next();
                if (type == 1) {
                    return;
                }
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals("sigs")) {
                            su.signatures.readXml(parser, this.mPastSignatures);
                        } else if (tagName.equals("perms")) {
                            readGrantedPermissionsLPw(parser, su.grantedPermissions);
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Unknown element under <shared-user>: " + parser.getName());
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                } else {
                    return;
                }
            }
        } else {
            XmlUtils.skipCurrentTag(parser);
        }
    }

    private void readGrantedPermissionsLPw(XmlPullParser parser, ArraySet<String> outPerms) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type != 3 || parser.getDepth() > outerDepth) {
                if (type != 3 && type != 4) {
                    String tagName = parser.getName();
                    if (tagName.equals(TAG_ITEM)) {
                        String name = parser.getAttributeValue(null, ATTR_NAME);
                        if (name != null) {
                            outPerms.add(name.intern());
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <perms> has no name at " + parser.getPositionDescription());
                        }
                    } else {
                        PackageManagerService.reportSettingsProblem(5, "Unknown element under <perms>: " + parser.getName());
                    }
                    XmlUtils.skipCurrentTag(parser);
                }
            } else {
                return;
            }
        }
    }

    void createNewUserLILPw(PackageManagerService service, Installer installer, int userHandle, File path) {
        path.mkdir();
        FileUtils.setPermissions(path.toString(), 505, -1, -1);
        for (PackageSetting ps : this.mPackages.values()) {
            if (ps.pkg != null && ps.pkg.applicationInfo != null) {
                ps.setInstalled((ps.pkgFlags & 1) != 0, userHandle);
                installer.createUserData(ps.name, UserHandle.getUid(userHandle, ps.appId), userHandle, ps.pkg.applicationInfo.seinfo);
            }
        }
        readDefaultPreferredAppsLPw(service, userHandle);
        writePackageRestrictionsLPr(userHandle);
    }

    void removeUserLPw(int userId) {
        Set<Map.Entry<String, PackageSetting>> entries = this.mPackages.entrySet();
        for (Map.Entry<String, PackageSetting> entry : entries) {
            entry.getValue().removeUser(userId);
        }
        this.mPreferredActivities.remove(userId);
        File file = getUserPackagesStateFile(userId);
        file.delete();
        File file2 = getUserPackagesStateBackupFile(userId);
        file2.delete();
        removeCrossProfileIntentFiltersLPw(userId);
    }

    void removeCrossProfileIntentFiltersLPw(int userId) {
        synchronized (this.mCrossProfileIntentResolvers) {
            if (this.mCrossProfileIntentResolvers.get(userId) != null) {
                this.mCrossProfileIntentResolvers.remove(userId);
                writePackageRestrictionsLPr(userId);
            }
            int count = this.mCrossProfileIntentResolvers.size();
            for (int i = 0; i < count; i++) {
                int sourceUserId = this.mCrossProfileIntentResolvers.keyAt(i);
                CrossProfileIntentResolver cpir = this.mCrossProfileIntentResolvers.get(sourceUserId);
                boolean needsWriting = false;
                ArraySet<CrossProfileIntentFilter> cpifs = new ArraySet<>(cpir.filterSet());
                for (CrossProfileIntentFilter cpif : cpifs) {
                    if (cpif.getTargetUserId() == userId) {
                        needsWriting = true;
                        cpir.removeFilter(cpif);
                    }
                }
                if (needsWriting) {
                    writePackageRestrictionsLPr(sourceUserId);
                }
            }
        }
    }

    private void setFirstAvailableUid(int uid) {
        if (uid > mFirstAvailableUid) {
            mFirstAvailableUid = uid;
        }
    }

    private int newUserIdLPw(Object obj) {
        int N = this.mUserIds.size();
        for (int i = mFirstAvailableUid; i < N; i++) {
            if (this.mUserIds.get(i) == null) {
                this.mUserIds.set(i, obj);
                return i + ProcessList.PSS_TEST_MIN_TIME_FROM_STATE_CHANGE;
            }
        }
        if (N > 9999) {
            return -1;
        }
        this.mUserIds.add(obj);
        return N + ProcessList.PSS_TEST_MIN_TIME_FROM_STATE_CHANGE;
    }

    public VerifierDeviceIdentity getVerifierDeviceIdentityLPw() {
        if (this.mVerifierDeviceIdentity == null) {
            this.mVerifierDeviceIdentity = VerifierDeviceIdentity.generate();
            writeLPr();
        }
        return this.mVerifierDeviceIdentity;
    }

    public PackageSetting getDisabledSystemPkgLPr(String name) {
        PackageSetting ps = this.mDisabledSysPackages.get(name);
        return ps;
    }

    private String compToString(ArraySet<String> cmp) {
        return cmp != null ? Arrays.toString(cmp.toArray()) : "[]";
    }

    boolean isEnabledLPr(ComponentInfo componentInfo, int flags, int userId) {
        if ((flags & 512) != 0) {
            return true;
        }
        String pkgName = componentInfo.packageName;
        PackageSetting packageSettings = this.mPackages.get(pkgName);
        if (packageSettings == null) {
            return false;
        }
        PackageUserState ustate = packageSettings.readUserState(userId);
        if ((32768 & flags) != 0 && ustate.enabled == 4) {
            return true;
        }
        if (ustate.enabled == 2 || ustate.enabled == 3 || ustate.enabled == 4 || !(packageSettings.pkg == null || packageSettings.pkg.applicationInfo.enabled || ustate.enabled != 0)) {
            return false;
        }
        if (ustate.enabledComponents != null && ustate.enabledComponents.contains(componentInfo.name)) {
            return true;
        }
        if (ustate.disabledComponents == null || !ustate.disabledComponents.contains(componentInfo.name)) {
            return componentInfo.enabled;
        }
        return false;
    }

    String getInstallerPackageNameLPr(String packageName) {
        PackageSetting pkg = this.mPackages.get(packageName);
        if (pkg == null) {
            throw new IllegalArgumentException("Unknown package: " + packageName);
        }
        return pkg.installerPackageName;
    }

    int getApplicationEnabledSettingLPr(String packageName, int userId) {
        PackageSetting pkg = this.mPackages.get(packageName);
        if (pkg == null) {
            throw new IllegalArgumentException("Unknown package: " + packageName);
        }
        return pkg.getEnabled(userId);
    }

    int getComponentEnabledSettingLPr(ComponentName componentName, int userId) {
        String packageName = componentName.getPackageName();
        PackageSetting pkg = this.mPackages.get(packageName);
        if (pkg == null) {
            throw new IllegalArgumentException("Unknown component: " + componentName);
        }
        String classNameStr = componentName.getClassName();
        return pkg.getCurrentEnabledStateLPr(classNameStr, userId);
    }

    boolean setPackageStoppedStateLPw(String packageName, boolean stopped, boolean allowedByPermission, int uid, int userId) {
        int appId = UserHandle.getAppId(uid);
        PackageSetting pkgSetting = this.mPackages.get(packageName);
        if (pkgSetting == null) {
            throw new IllegalArgumentException("Unknown package: " + packageName);
        }
        if (!allowedByPermission && appId != pkgSetting.appId) {
            throw new SecurityException("Permission Denial: attempt to change stopped state from pid=" + Binder.getCallingPid() + ", uid=" + uid + ", package uid=" + pkgSetting.appId);
        }
        if (pkgSetting.getStopped(userId) == stopped) {
            return false;
        }
        pkgSetting.setStopped(stopped, userId);
        if (pkgSetting.getNotLaunched(userId)) {
            if (pkgSetting.installerPackageName != null) {
                PackageManagerService.sendPackageBroadcast("android.intent.action.PACKAGE_FIRST_LAUNCH", pkgSetting.name, null, pkgSetting.installerPackageName, null, new int[]{userId});
            }
            pkgSetting.setNotLaunched(false, userId);
        }
        return true;
    }

    private List<UserInfo> getAllUsers() {
        long id = Binder.clearCallingIdentity();
        try {
            List<UserInfo> users = UserManagerService.getInstance().getUsers(false);
            Binder.restoreCallingIdentity(id);
            return users;
        } catch (NullPointerException e) {
            Binder.restoreCallingIdentity(id);
            return null;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(id);
            throw th;
        }
    }

    static final void printFlags(PrintWriter pw, int val, Object[] spec) {
        pw.print("[ ");
        for (int i = 0; i < spec.length; i += 2) {
            int mask = ((Integer) spec[i]).intValue();
            if ((val & mask) != 0) {
                pw.print(spec[i + 1]);
                pw.print(" ");
            }
        }
        pw.print("]");
    }

    void dumpPackageLPr(PrintWriter pw, String prefix, String checkinTag, PackageSetting ps, SimpleDateFormat sdf, Date date, List<UserInfo> users) {
        if (checkinTag != null) {
            pw.print(checkinTag);
            pw.print(",");
            pw.print(ps.realName != null ? ps.realName : ps.name);
            pw.print(",");
            pw.print(ps.appId);
            pw.print(",");
            pw.print(ps.versionCode);
            pw.print(",");
            pw.print(ps.firstInstallTime);
            pw.print(",");
            pw.print(ps.lastUpdateTime);
            pw.print(",");
            pw.print(ps.installerPackageName != null ? ps.installerPackageName : "?");
            pw.println();
            if (ps.pkg != null) {
                pw.print(checkinTag);
                pw.print("-");
                pw.print("splt,");
                pw.print("base,");
                pw.println(ps.pkg.baseRevisionCode);
                if (ps.pkg.splitNames != null) {
                    for (int i = 0; i < ps.pkg.splitNames.length; i++) {
                        pw.print(checkinTag);
                        pw.print("-");
                        pw.print("splt,");
                        pw.print(ps.pkg.splitNames[i]);
                        pw.print(",");
                        pw.println(ps.pkg.splitRevisionCodes[i]);
                    }
                }
            }
            for (UserInfo user : users) {
                pw.print(checkinTag);
                pw.print("-");
                pw.print("usr");
                pw.print(",");
                pw.print(user.id);
                pw.print(",");
                pw.print(ps.getInstalled(user.id) ? "I" : "i");
                pw.print(ps.getHidden(user.id) ? "B" : "b");
                pw.print(ps.getStopped(user.id) ? "S" : "s");
                pw.print(ps.getNotLaunched(user.id) ? "l" : "L");
                pw.print(",");
                pw.print(ps.getEnabled(user.id));
                String lastDisabledAppCaller = ps.getLastDisabledAppCaller(user.id);
                pw.print(",");
                if (lastDisabledAppCaller == null) {
                    lastDisabledAppCaller = "?";
                }
                pw.print(lastDisabledAppCaller);
                pw.println();
            }
            return;
        }
        pw.print(prefix);
        pw.print("Package [");
        pw.print(ps.realName != null ? ps.realName : ps.name);
        pw.print("] (");
        pw.print(Integer.toHexString(System.identityHashCode(ps)));
        pw.println("):");
        if (ps.realName != null) {
            pw.print(prefix);
            pw.print("  compat name=");
            pw.println(ps.name);
        }
        pw.print(prefix);
        pw.print("  userId=");
        pw.print(ps.appId);
        pw.print(" gids=");
        pw.println(PackageManagerService.arrayToString(ps.gids));
        if (ps.sharedUser != null) {
            pw.print(prefix);
            pw.print("  sharedUser=");
            pw.println(ps.sharedUser);
        }
        pw.print(prefix);
        pw.print("  pkg=");
        pw.println(ps.pkg);
        pw.print(prefix);
        pw.print("  codePath=");
        pw.println(ps.codePathString);
        pw.print(prefix);
        pw.print("  resourcePath=");
        pw.println(ps.resourcePathString);
        pw.print(prefix);
        pw.print("  legacyNativeLibraryDir=");
        pw.println(ps.legacyNativeLibraryPathString);
        pw.print(prefix);
        pw.print("  primaryCpuAbi=");
        pw.println(ps.primaryCpuAbiString);
        pw.print(prefix);
        pw.print("  secondaryCpuAbi=");
        pw.println(ps.secondaryCpuAbiString);
        pw.print(prefix);
        pw.print("  versionCode=");
        pw.print(ps.versionCode);
        if (ps.pkg != null) {
            pw.print(" targetSdk=");
            pw.print(ps.pkg.applicationInfo.targetSdkVersion);
        }
        pw.println();
        if (ps.pkg != null) {
            pw.print(prefix);
            pw.print("  versionName=");
            pw.println(ps.pkg.mVersionName);
            pw.print(prefix);
            pw.print("  splits=");
            dumpSplitNames(pw, ps.pkg);
            pw.println();
            pw.print(prefix);
            pw.print("  applicationInfo=");
            pw.println(ps.pkg.applicationInfo.toString());
            pw.print(prefix);
            pw.print("  flags=");
            printFlags(pw, ps.pkg.applicationInfo.flags, FLAG_DUMP_SPEC);
            pw.println();
            pw.print(prefix);
            pw.print("  dataDir=");
            pw.println(ps.pkg.applicationInfo.dataDir);
            if (ps.pkg.mOperationPending) {
                pw.print(prefix);
                pw.println("  mOperationPending=true");
            }
            pw.print(prefix);
            pw.print("  supportsScreens=[");
            boolean first = true;
            if ((ps.pkg.applicationInfo.flags & 512) != 0) {
                if (1 == 0) {
                    pw.print(", ");
                }
                first = false;
                pw.print("small");
            }
            if ((ps.pkg.applicationInfo.flags & 1024) != 0) {
                if (!first) {
                    pw.print(", ");
                }
                first = false;
                pw.print("medium");
            }
            if ((ps.pkg.applicationInfo.flags & PackageManagerService.DumpState.DUMP_KEYSETS) != 0) {
                if (!first) {
                    pw.print(", ");
                }
                first = false;
                pw.print("large");
            }
            if ((ps.pkg.applicationInfo.flags & 524288) != 0) {
                if (!first) {
                    pw.print(", ");
                }
                first = false;
                pw.print("xlarge");
            }
            if ((ps.pkg.applicationInfo.flags & PackageManagerService.DumpState.DUMP_VERSION) != 0) {
                if (!first) {
                    pw.print(", ");
                }
                first = false;
                pw.print("resizeable");
            }
            if ((ps.pkg.applicationInfo.flags & PackageManagerService.DumpState.DUMP_INSTALLS) != 0) {
                if (!first) {
                    pw.print(", ");
                }
                pw.print("anyDensity");
            }
            pw.println("]");
            if (ps.pkg.libraryNames != null && ps.pkg.libraryNames.size() > 0) {
                pw.print(prefix);
                pw.println("  libraries:");
                for (int i2 = 0; i2 < ps.pkg.libraryNames.size(); i2++) {
                    pw.print(prefix);
                    pw.print("    ");
                    pw.println((String) ps.pkg.libraryNames.get(i2));
                }
            }
            if (ps.pkg.usesLibraries != null && ps.pkg.usesLibraries.size() > 0) {
                pw.print(prefix);
                pw.println("  usesLibraries:");
                for (int i3 = 0; i3 < ps.pkg.usesLibraries.size(); i3++) {
                    pw.print(prefix);
                    pw.print("    ");
                    pw.println((String) ps.pkg.usesLibraries.get(i3));
                }
            }
            if (ps.pkg.usesOptionalLibraries != null && ps.pkg.usesOptionalLibraries.size() > 0) {
                pw.print(prefix);
                pw.println("  usesOptionalLibraries:");
                for (int i4 = 0; i4 < ps.pkg.usesOptionalLibraries.size(); i4++) {
                    pw.print(prefix);
                    pw.print("    ");
                    pw.println((String) ps.pkg.usesOptionalLibraries.get(i4));
                }
            }
            if (ps.pkg.usesLibraryFiles != null && ps.pkg.usesLibraryFiles.length > 0) {
                pw.print(prefix);
                pw.println("  usesLibraryFiles:");
                for (int i5 = 0; i5 < ps.pkg.usesLibraryFiles.length; i5++) {
                    pw.print(prefix);
                    pw.print("    ");
                    pw.println(ps.pkg.usesLibraryFiles[i5]);
                }
            }
        }
        pw.print(prefix);
        pw.print("  timeStamp=");
        date.setTime(ps.timeStamp);
        pw.println(sdf.format(date));
        pw.print(prefix);
        pw.print("  firstInstallTime=");
        date.setTime(ps.firstInstallTime);
        pw.println(sdf.format(date));
        pw.print(prefix);
        pw.print("  lastUpdateTime=");
        date.setTime(ps.lastUpdateTime);
        pw.println(sdf.format(date));
        if (ps.installerPackageName != null) {
            pw.print(prefix);
            pw.print("  installerPackageName=");
            pw.println(ps.installerPackageName);
        }
        pw.print(prefix);
        pw.print("  signatures=");
        pw.println(ps.signatures);
        pw.print(prefix);
        pw.print("  permissionsFixed=");
        pw.print(ps.permissionsFixed);
        pw.print(" haveGids=");
        pw.print(ps.haveGids);
        pw.print(" installStatus=");
        pw.println(ps.installStatus);
        pw.print(prefix);
        pw.print("  pkgFlags=");
        printFlags(pw, ps.pkgFlags, FLAG_DUMP_SPEC);
        pw.println();
        for (UserInfo user2 : users) {
            pw.print(prefix);
            pw.print("  User ");
            pw.print(user2.id);
            pw.print(": ");
            pw.print(" installed=");
            pw.print(ps.getInstalled(user2.id));
            pw.print(" hidden=");
            pw.print(ps.getHidden(user2.id));
            pw.print(" stopped=");
            pw.print(ps.getStopped(user2.id));
            pw.print(" notLaunched=");
            pw.print(ps.getNotLaunched(user2.id));
            pw.print(" enabled=");
            pw.println(ps.getEnabled(user2.id));
            String lastDisabledAppCaller2 = ps.getLastDisabledAppCaller(user2.id);
            if (lastDisabledAppCaller2 != null) {
                pw.print(prefix);
                pw.print("    lastDisabledCaller: ");
                pw.println(lastDisabledAppCaller2);
            }
            ArraySet<String> cmp = ps.getDisabledComponents(user2.id);
            if (cmp != null && cmp.size() > 0) {
                pw.print(prefix);
                pw.println("    disabledComponents:");
                for (String s : cmp) {
                    pw.print(prefix);
                    pw.print("    ");
                    pw.println(s);
                }
            }
            ArraySet<String> cmp2 = ps.getEnabledComponents(user2.id);
            if (cmp2 != null && cmp2.size() > 0) {
                pw.print(prefix);
                pw.println("    enabledComponents:");
                for (String s2 : cmp2) {
                    pw.print(prefix);
                    pw.print("    ");
                    pw.println(s2);
                }
            }
        }
        if (ps.grantedPermissions.size() > 0) {
            pw.print(prefix);
            pw.println("  grantedPermissions:");
            for (String s3 : ps.grantedPermissions) {
                pw.print(prefix);
                pw.print("    ");
                pw.println(s3);
            }
        }
    }

    void dumpPackagesLPr(PrintWriter pw, String packageName, PackageManagerService.DumpState dumpState, boolean checkin) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        boolean printedSomething = false;
        List<UserInfo> users = getAllUsers();
        for (PackageSetting ps : this.mPackages.values()) {
            if (packageName == null || packageName.equals(ps.realName) || packageName.equals(ps.name)) {
                if (!checkin && packageName != null) {
                    dumpState.setSharedUser(ps.sharedUser);
                }
                if (!checkin && !printedSomething) {
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    pw.println("Packages:");
                    printedSomething = true;
                }
                dumpPackageLPr(pw, "  ", checkin ? TAG_PACKAGE : null, ps, sdf, date, users);
            }
        }
        boolean printedSomething2 = false;
        if (!checkin && this.mRenamedPackages.size() > 0) {
            for (Map.Entry<String, String> e : this.mRenamedPackages.entrySet()) {
                if (packageName == null || packageName.equals(e.getKey()) || packageName.equals(e.getValue())) {
                    if (!checkin) {
                        if (!printedSomething2) {
                            if (dumpState.onTitlePrinted()) {
                                pw.println();
                            }
                            pw.println("Renamed packages:");
                            printedSomething2 = true;
                        }
                        pw.print("  ");
                    } else {
                        pw.print("ren,");
                    }
                    pw.print(e.getKey());
                    pw.print(checkin ? " -> " : ",");
                    pw.println(e.getValue());
                }
            }
        }
        boolean printedSomething3 = false;
        if (this.mDisabledSysPackages.size() > 0) {
            for (PackageSetting ps2 : this.mDisabledSysPackages.values()) {
                if (packageName == null || packageName.equals(ps2.realName) || packageName.equals(ps2.name)) {
                    if (!checkin && !printedSomething3) {
                        if (dumpState.onTitlePrinted()) {
                            pw.println();
                        }
                        pw.println("Hidden system packages:");
                        printedSomething3 = true;
                    }
                    dumpPackageLPr(pw, "  ", checkin ? "dis" : null, ps2, sdf, date, users);
                }
            }
        }
    }

    void dumpPermissionsLPr(PrintWriter pw, String packageName, PackageManagerService.DumpState dumpState) {
        boolean printedSomething = false;
        for (BasePermission p : this.mPermissions.values()) {
            if (packageName == null || packageName.equals(p.sourcePackage)) {
                if (!printedSomething) {
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    pw.println("Permissions:");
                    printedSomething = true;
                }
                pw.print("  Permission [");
                pw.print(p.name);
                pw.print("] (");
                pw.print(Integer.toHexString(System.identityHashCode(p)));
                pw.println("):");
                pw.print("    sourcePackage=");
                pw.println(p.sourcePackage);
                pw.print("    uid=");
                pw.print(p.uid);
                pw.print(" gids=");
                pw.print(PackageManagerService.arrayToString(p.gids));
                pw.print(" type=");
                pw.print(p.type);
                pw.print(" prot=");
                pw.println(PermissionInfo.protectionToString(p.protectionLevel));
                if (p.packageSetting != null) {
                    pw.print("    packageSetting=");
                    pw.println(p.packageSetting);
                }
                if (p.perm != null) {
                    pw.print("    perm=");
                    pw.println(p.perm);
                }
                if ("android.permission.READ_EXTERNAL_STORAGE".equals(p.name)) {
                    pw.print("    enforced=");
                    pw.println(this.mReadExternalStorageEnforced);
                }
            }
        }
    }

    void dumpSharedUsersLPr(PrintWriter pw, String packageName, PackageManagerService.DumpState dumpState, boolean checkin) {
        boolean printedSomething = false;
        for (SharedUserSetting su : this.mSharedUsers.values()) {
            if (packageName == null || su == dumpState.getSharedUser()) {
                if (!checkin) {
                    if (!printedSomething) {
                        if (dumpState.onTitlePrinted()) {
                            pw.println();
                        }
                        pw.println("Shared users:");
                        printedSomething = true;
                    }
                    pw.print("  SharedUser [");
                    pw.print(su.name);
                    pw.print("] (");
                    pw.print(Integer.toHexString(System.identityHashCode(su)));
                    pw.println("):");
                    pw.print("    userId=");
                    pw.print(su.userId);
                    pw.print(" gids=");
                    pw.println(PackageManagerService.arrayToString(su.gids));
                    pw.println("    grantedPermissions:");
                    for (String s : su.grantedPermissions) {
                        pw.print("      ");
                        pw.println(s);
                    }
                } else {
                    pw.print("suid,");
                    pw.print(su.userId);
                    pw.print(",");
                    pw.println(su.name);
                }
            }
        }
    }

    void dumpReadMessagesLPr(PrintWriter pw, PackageManagerService.DumpState dumpState) {
        pw.println("Settings parse messages:");
        pw.print(this.mReadMessages.toString());
    }

    private static void dumpSplitNames(PrintWriter pw, PackageParser.Package pkg) {
        if (pkg == null) {
            pw.print("unknown");
            return;
        }
        pw.print("[");
        pw.print("base");
        if (pkg.baseRevisionCode != 0) {
            pw.print(":");
            pw.print(pkg.baseRevisionCode);
        }
        if (pkg.splitNames != null) {
            for (int i = 0; i < pkg.splitNames.length; i++) {
                pw.print(", ");
                pw.print(pkg.splitNames[i]);
                if (pkg.splitRevisionCodes[i] != 0) {
                    pw.print(":");
                    pw.print(pkg.splitRevisionCodes[i]);
                }
            }
        }
        pw.print("]");
    }
}
