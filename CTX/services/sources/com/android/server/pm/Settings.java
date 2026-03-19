package com.android.server.pm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageCleanItem;
import android.content.pm.PackageManager;
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
import android.os.Handler;
import android.os.Message;
import android.os.PatternMatcher;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Log;
import android.util.LogPrinter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.util.Xml;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.logging.EventLogTags;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.JournaledFile;
import com.android.internal.util.XmlUtils;
import com.android.server.BatteryService;
import com.android.server.UiModeManagerService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.pm.Installer;
import com.android.server.pm.permission.BasePermission;
import com.android.server.pm.permission.PermissionSettings;
import com.android.server.pm.permission.PermissionsState;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class Settings {
    private static final String ATTR_APP_LINK_GENERATION = "app-link-generation";
    private static final String ATTR_BLOCKED = "blocked";

    @Deprecated
    private static final String ATTR_BLOCK_UNINSTALL = "blockUninstall";
    private static final String ATTR_CE_DATA_INODE = "ceDataInode";
    private static final String ATTR_CODE = "code";
    private static final String ATTR_DATABASE_VERSION = "databaseVersion";
    private static final String ATTR_DOMAIN_VERIFICATON_STATE = "domainVerificationStatus";
    private static final String ATTR_ENABLED = "enabled";
    private static final String ATTR_ENABLED_CALLER = "enabledCaller";
    private static final String ATTR_ENFORCEMENT = "enforcement";
    private static final String ATTR_FINGERPRINT = "fingerprint";
    private static final String ATTR_FLAGS = "flags";
    private static final String ATTR_GRANTED = "granted";
    private static final String ATTR_HARMFUL_APP_WARNING = "harmful-app-warning";
    private static final String ATTR_HIDDEN = "hidden";
    private static final String ATTR_INSTALLED = "inst";
    private static final String ATTR_INSTALL_REASON = "install-reason";
    private static final String ATTR_INSTANT_APP = "instant-app";
    public static final String ATTR_NAME = "name";
    private static final String ATTR_NOT_LAUNCHED = "nl";
    public static final String ATTR_PACKAGE = "package";
    private static final String ATTR_PACKAGE_NAME = "packageName";
    private static final String ATTR_REVOKE_ON_UPGRADE = "rou";
    private static final String ATTR_SDK_VERSION = "sdkVersion";
    private static final String ATTR_STOPPED = "stopped";
    private static final String ATTR_SUSPENDED = "suspended";
    private static final String ATTR_SUSPENDING_PACKAGE = "suspending-package";
    private static final String ATTR_SUSPEND_DIALOG_MESSAGE = "suspend_dialog_message";
    private static final String ATTR_USER = "user";
    private static final String ATTR_USER_FIXED = "fixed";
    private static final String ATTR_USER_SET = "set";
    private static final String ATTR_VERSION = "version";
    private static final String ATTR_VIRTUAL_PRELOAD = "virtual-preload";
    private static final String ATTR_VOLUME_UUID = "volumeUuid";
    public static final int CURRENT_DATABASE_VERSION = 3;
    private static final boolean DEBUG_KERNEL = false;
    private static final boolean DEBUG_MU = false;
    private static final boolean DEBUG_PARSER = false;
    private static final boolean DEBUG_STOPPED = false;
    private static final String RUNTIME_PERMISSIONS_FILE_NAME = "runtime-permissions.xml";
    private static final String TAG = "PackageSettings";
    private static final String TAG_ALL_INTENT_FILTER_VERIFICATION = "all-intent-filter-verifications";
    private static final String TAG_BLOCK_UNINSTALL = "block-uninstall";
    private static final String TAG_BLOCK_UNINSTALL_PACKAGES = "block-uninstall-packages";
    private static final String TAG_CHILD_PACKAGE = "child-package";
    static final String TAG_CROSS_PROFILE_INTENT_FILTERS = "crossProfile-intent-filters";
    private static final String TAG_DEFAULT_APPS = "default-apps";
    private static final String TAG_DEFAULT_BROWSER = "default-browser";
    private static final String TAG_DEFAULT_DIALER = "default-dialer";
    private static final String TAG_DISABLED_COMPONENTS = "disabled-components";
    private static final String TAG_DOMAIN_VERIFICATION = "domain-verification";
    private static final String TAG_ENABLED_COMPONENTS = "enabled-components";
    public static final String TAG_ITEM = "item";
    private static final String TAG_PACKAGE = "pkg";
    private static final String TAG_PACKAGE_RESTRICTIONS = "package-restrictions";
    private static final String TAG_PERMISSIONS = "perms";
    private static final String TAG_PERMISSION_ENTRY = "perm";
    private static final String TAG_PERSISTENT_PREFERRED_ACTIVITIES = "persistent-preferred-activities";
    private static final String TAG_READ_EXTERNAL_STORAGE = "read-external-storage";
    private static final String TAG_RESTORED_RUNTIME_PERMISSIONS = "restored-perms";
    private static final String TAG_RUNTIME_PERMISSIONS = "runtime-permissions";
    private static final String TAG_SHARED_USER = "shared-user";
    private static final String TAG_SUSPENDED_APP_EXTRAS = "suspended-app-extras";
    private static final String TAG_SUSPENDED_LAUNCHER_EXTRAS = "suspended-launcher-extras";
    private static final String TAG_USES_STATIC_LIB = "uses-static-lib";
    private static final String TAG_VERSION = "version";
    private static final int USER_RUNTIME_GRANT_MASK = 11;
    private final File mBackupSettingsFilename;
    private final File mBackupStoppedPackagesFilename;
    private final SparseArray<ArraySet<String>> mBlockUninstallPackages;
    final SparseArray<CrossProfileIntentResolver> mCrossProfileIntentResolvers;
    final SparseArray<String> mDefaultBrowserApp;
    final SparseArray<String> mDefaultDialerApp;
    private final ArrayMap<String, PackageSetting> mDisabledSysPackages;
    final ArraySet<String> mInstallerPackages;
    private final ArrayMap<String, KernelPackageState> mKernelMapping;
    private final File mKernelMappingFilename;
    public final KeySetManagerService mKeySetManagerService;
    private final ArrayMap<Long, Integer> mKeySetRefs;
    private final Object mLock;
    final SparseIntArray mNextAppLinkGeneration;
    private final SparseArray<Object> mOtherUserIds;
    private final File mPackageListFilename;
    final ArrayMap<String, PackageSetting> mPackages;
    final ArrayList<PackageCleanItem> mPackagesToBeCleaned;
    private final ArrayList<Signature> mPastSignatures;
    private final ArrayList<PackageSetting> mPendingPackages;
    final PermissionSettings mPermissions;
    final SparseArray<PersistentPreferredIntentResolver> mPersistentPreferredActivities;
    final SparseArray<PreferredIntentResolver> mPreferredActivities;
    Boolean mReadExternalStorageEnforced;
    final StringBuilder mReadMessages;
    private final ArrayMap<String, String> mRenamedPackages;
    private final ArrayMap<String, IntentFilterVerificationInfo> mRestoredIntentFilterVerifications;
    private final SparseArray<ArrayMap<String, ArraySet<RestoredPermissionGrant>>> mRestoredUserGrants;
    private final RuntimePermissionPersistence mRuntimePermissionsPersistence;
    private final File mSettingsFilename;
    final ArrayMap<String, SharedUserSetting> mSharedUsers;
    private final File mStoppedPackagesFilename;
    private final File mSystemDir;
    private final ArrayList<Object> mUserIds;
    private VerifierDeviceIdentity mVerifierDeviceIdentity;
    private ArrayMap<String, VersionInfo> mVersion;
    private static int mFirstAvailableUid = 0;
    private static int PRE_M_APP_INFO_FLAG_HIDDEN = 134217728;
    private static int PRE_M_APP_INFO_FLAG_CANT_SAVE_STATE = 268435456;
    private static int PRE_M_APP_INFO_FLAG_FORWARD_LOCK = 536870912;
    private static int PRE_M_APP_INFO_FLAG_PRIVILEGED = 1073741824;
    static final Object[] FLAG_DUMP_SPEC = {1, "SYSTEM", 2, "DEBUGGABLE", 4, "HAS_CODE", 8, "PERSISTENT", 16, "FACTORY_TEST", 32, "ALLOW_TASK_REPARENTING", 64, "ALLOW_CLEAR_USER_DATA", 128, "UPDATED_SYSTEM_APP", 256, "TEST_ONLY", 16384, "VM_SAFE_MODE", 32768, "ALLOW_BACKUP", 65536, "KILL_AFTER_RESTORE", Integer.valueOf(DumpState.DUMP_INTENT_FILTER_VERIFIERS), "RESTORE_ANY_VERSION", Integer.valueOf(DumpState.DUMP_DOMAIN_PREFERRED), "EXTERNAL_STORAGE", Integer.valueOf(DumpState.DUMP_DEXOPT), "LARGE_HEAP"};
    private static final Object[] PRIVATE_FLAG_DUMP_SPEC = {1024, "PRIVATE_FLAG_ACTIVITIES_RESIZE_MODE_RESIZEABLE", 4096, "PRIVATE_FLAG_ACTIVITIES_RESIZE_MODE_RESIZEABLE_VIA_SDK_VERSION", 2048, "PRIVATE_FLAG_ACTIVITIES_RESIZE_MODE_UNRESIZEABLE", 8192, "BACKUP_IN_FOREGROUND", 2, "CANT_SAVE_STATE", 32, "DEFAULT_TO_DEVICE_PROTECTED_STORAGE", 64, "DIRECT_BOOT_AWARE", 4, "FORWARD_LOCK", 16, "HAS_DOMAIN_URLS", 1, "HIDDEN", 128, "EPHEMERAL", 32768, "ISOLATED_SPLIT_LOADING", Integer.valueOf(DumpState.DUMP_INTENT_FILTER_VERIFIERS), "OEM", 256, "PARTIALLY_DIRECT_BOOT_AWARE", 8, "PRIVILEGED", 512, "REQUIRED_FOR_SYSTEM_USER", 16384, "STATIC_SHARED_LIBRARY", Integer.valueOf(DumpState.DUMP_DOMAIN_PREFERRED), "VENDOR", Integer.valueOf(DumpState.DUMP_FROZEN), "PRODUCT", 65536, "VIRTUAL_PRELOAD"};

    public static class DatabaseVersion {
        public static final int FIRST_VERSION = 1;
        public static final int SIGNATURE_END_ENTITY = 2;
        public static final int SIGNATURE_MALFORMED_RECOVER = 3;
    }

    private static final class KernelPackageState {
        int appId;
        int[] excludedUserIds;

        private KernelPackageState() {
        }
    }

    final class RestoredPermissionGrant {
        int grantBits;
        boolean granted;
        String permissionName;

        RestoredPermissionGrant(String str, boolean z, int i) {
            this.permissionName = str;
            this.granted = z;
            this.grantBits = i;
        }
    }

    public static class VersionInfo {
        int databaseVersion;
        String fingerprint;
        int sdkVersion;

        public void forceCurrent() {
            this.sdkVersion = Build.VERSION.SDK_INT;
            this.databaseVersion = 3;
            this.fingerprint = Build.FINGERPRINT;
        }
    }

    Settings(PermissionSettings permissionSettings, Object obj) {
        this(Environment.getDataDirectory(), permissionSettings, obj);
    }

    Settings(File file, PermissionSettings permissionSettings, Object obj) {
        this.mPackages = new ArrayMap<>();
        this.mInstallerPackages = new ArraySet<>();
        this.mKernelMapping = new ArrayMap<>();
        this.mDisabledSysPackages = new ArrayMap<>();
        this.mBlockUninstallPackages = new SparseArray<>();
        this.mRestoredIntentFilterVerifications = new ArrayMap<>();
        this.mRestoredUserGrants = new SparseArray<>();
        this.mVersion = new ArrayMap<>();
        this.mPreferredActivities = new SparseArray<>();
        this.mPersistentPreferredActivities = new SparseArray<>();
        this.mCrossProfileIntentResolvers = new SparseArray<>();
        this.mSharedUsers = new ArrayMap<>();
        this.mUserIds = new ArrayList<>();
        this.mOtherUserIds = new SparseArray<>();
        this.mPastSignatures = new ArrayList<>();
        this.mKeySetRefs = new ArrayMap<>();
        this.mPackagesToBeCleaned = new ArrayList<>();
        this.mRenamedPackages = new ArrayMap<>();
        this.mDefaultBrowserApp = new SparseArray<>();
        this.mDefaultDialerApp = new SparseArray<>();
        this.mNextAppLinkGeneration = new SparseIntArray();
        this.mReadMessages = new StringBuilder();
        this.mPendingPackages = new ArrayList<>();
        this.mKeySetManagerService = new KeySetManagerService(this.mPackages);
        this.mLock = obj;
        this.mPermissions = permissionSettings;
        this.mRuntimePermissionsPersistence = new RuntimePermissionPersistence(this.mLock);
        this.mSystemDir = new File(file, "system");
        this.mSystemDir.mkdirs();
        FileUtils.setPermissions(this.mSystemDir.toString(), 509, -1, -1);
        this.mSettingsFilename = new File(this.mSystemDir, "packages.xml");
        this.mBackupSettingsFilename = new File(this.mSystemDir, "packages-backup.xml");
        this.mPackageListFilename = new File(this.mSystemDir, "packages.list");
        FileUtils.setPermissions(this.mPackageListFilename, 416, 1000, 1032);
        File file2 = new File("/config/sdcardfs");
        this.mKernelMappingFilename = file2.exists() ? file2 : null;
        this.mStoppedPackagesFilename = new File(this.mSystemDir, "packages-stopped.xml");
        this.mBackupStoppedPackagesFilename = new File(this.mSystemDir, "packages-stopped-backup.xml");
    }

    PackageSetting getPackageLPr(String str) {
        return this.mPackages.get(str);
    }

    String getRenamedPackageLPr(String str) {
        return this.mRenamedPackages.get(str);
    }

    String addRenamedPackageLPw(String str, String str2) {
        return this.mRenamedPackages.put(str, str2);
    }

    void applyPendingPermissionGrantsLPw(String str, int i) {
        ArraySet<RestoredPermissionGrant> arraySet;
        ArrayMap<String, ArraySet<RestoredPermissionGrant>> arrayMap = this.mRestoredUserGrants.get(i);
        if (arrayMap == null || arrayMap.size() == 0 || (arraySet = arrayMap.get(str)) == null || arraySet.size() == 0) {
            return;
        }
        PackageSetting packageSetting = this.mPackages.get(str);
        if (packageSetting == null) {
            Slog.e(TAG, "Can't find supposedly installed package " + str);
            return;
        }
        PermissionsState permissionsState = packageSetting.getPermissionsState();
        for (RestoredPermissionGrant restoredPermissionGrant : arraySet) {
            BasePermission permission = this.mPermissions.getPermission(restoredPermissionGrant.permissionName);
            if (permission != null) {
                if (restoredPermissionGrant.granted) {
                    permissionsState.grantRuntimePermission(permission, i);
                }
                permissionsState.updatePermissionFlags(permission, i, 11, restoredPermissionGrant.grantBits);
            }
        }
        arrayMap.remove(str);
        if (arrayMap.size() < 1) {
            this.mRestoredUserGrants.remove(i);
        }
        writeRuntimePermissionsForUserLPr(i, false);
    }

    public boolean canPropagatePermissionToInstantApp(String str) {
        return this.mPermissions.canPropagatePermissionToInstantApp(str);
    }

    void setInstallerPackageName(String str, String str2) {
        PackageSetting packageSetting = this.mPackages.get(str);
        if (packageSetting != null) {
            packageSetting.setInstallerPackageName(str2);
            if (str2 != null) {
                this.mInstallerPackages.add(str2);
            }
        }
    }

    SharedUserSetting getSharedUserLPw(String str, int i, int i2, boolean z) throws PackageManagerException {
        SharedUserSetting sharedUserSetting = this.mSharedUsers.get(str);
        if (sharedUserSetting == null && z) {
            sharedUserSetting = new SharedUserSetting(str, i, i2);
            sharedUserSetting.userId = newUserIdLPw(sharedUserSetting);
            if (sharedUserSetting.userId < 0) {
                throw new PackageManagerException(-4, "Creating shared user " + str + " failed");
            }
            Log.i("PackageManager", "New shared user " + str + ": id=" + sharedUserSetting.userId);
            this.mSharedUsers.put(str, sharedUserSetting);
        }
        return sharedUserSetting;
    }

    Collection<SharedUserSetting> getAllSharedUsersLPw() {
        return this.mSharedUsers.values();
    }

    boolean disableSystemPackageLPw(String str, boolean z) {
        PackageSetting packageSetting = this.mPackages.get(str);
        if (packageSetting == null) {
            Log.w("PackageManager", "Package " + str + " is not an installed package");
            return false;
        }
        if (this.mDisabledSysPackages.get(str) != null || packageSetting.pkg == null || !packageSetting.pkg.isSystem() || packageSetting.pkg.isUpdatedSystemApp()) {
            return false;
        }
        if (packageSetting.pkg != null && packageSetting.pkg.applicationInfo != null) {
            packageSetting.pkg.applicationInfo.flags |= 128;
        }
        this.mDisabledSysPackages.put(str, packageSetting);
        if (z) {
            replacePackageLPw(str, new PackageSetting(packageSetting));
            return true;
        }
        return true;
    }

    PackageSetting enableSystemPackageLPw(String str) {
        PackageSetting packageSetting = this.mDisabledSysPackages.get(str);
        if (packageSetting == null) {
            Log.w("PackageManager", "Package " + str + " is not disabled");
            return null;
        }
        if (packageSetting.pkg != null && packageSetting.pkg.applicationInfo != null) {
            packageSetting.pkg.applicationInfo.flags &= -129;
        }
        PackageSetting packageSettingAddPackageLPw = addPackageLPw(str, packageSetting.realName, packageSetting.codePath, packageSetting.resourcePath, packageSetting.legacyNativeLibraryPathString, packageSetting.primaryCpuAbiString, packageSetting.secondaryCpuAbiString, packageSetting.cpuAbiOverrideString, packageSetting.appId, packageSetting.versionCode, packageSetting.pkgFlags, packageSetting.pkgPrivateFlags, packageSetting.parentPackageName, packageSetting.childPackageNames, packageSetting.usesStaticLibraries, packageSetting.usesStaticLibrariesVersions);
        this.mDisabledSysPackages.remove(str);
        return packageSettingAddPackageLPw;
    }

    boolean isDisabledSystemPackageLPr(String str) {
        return this.mDisabledSysPackages.containsKey(str);
    }

    void removeDisabledSystemPackageLPw(String str) {
        this.mDisabledSysPackages.remove(str);
    }

    PackageSetting addPackageLPw(String str, String str2, File file, File file2, String str3, String str4, String str5, String str6, int i, long j, int i2, int i3, String str7, List<String> list, String[] strArr, long[] jArr) {
        PackageSetting packageSetting = this.mPackages.get(str);
        if (packageSetting != null) {
            if (packageSetting.appId == i) {
                return packageSetting;
            }
            PackageManagerService.reportSettingsProblem(6, "Adding duplicate package, keeping first: " + str);
            return null;
        }
        PackageSetting packageSetting2 = new PackageSetting(str, str2, file, file2, str3, str4, str5, str6, j, i2, i3, str7, list, 0, strArr, jArr);
        packageSetting2.appId = i;
        if (!addUserIdLPw(i, packageSetting2, str)) {
            return null;
        }
        this.mPackages.put(str, packageSetting2);
        return packageSetting2;
    }

    void addAppOpPackage(String str, String str2) {
        this.mPermissions.addAppOpPackage(str, str2);
    }

    SharedUserSetting addSharedUserLPw(String str, int i, int i2, int i3) {
        SharedUserSetting sharedUserSetting = this.mSharedUsers.get(str);
        if (sharedUserSetting != null) {
            if (sharedUserSetting.userId == i) {
                return sharedUserSetting;
            }
            PackageManagerService.reportSettingsProblem(6, "Adding duplicate shared user, keeping first: " + str);
            return null;
        }
        SharedUserSetting sharedUserSetting2 = new SharedUserSetting(str, i2, i3);
        sharedUserSetting2.userId = i;
        if (!addUserIdLPw(i, sharedUserSetting2, str)) {
            return null;
        }
        this.mSharedUsers.put(str, sharedUserSetting2);
        return sharedUserSetting2;
    }

    void pruneSharedUsersLPw() {
        ArrayList arrayList = new ArrayList();
        for (Map.Entry<String, SharedUserSetting> entry : this.mSharedUsers.entrySet()) {
            SharedUserSetting value = entry.getValue();
            if (value == null) {
                arrayList.add(entry.getKey());
            } else {
                Iterator<PackageSetting> it = value.packages.iterator();
                while (it.hasNext()) {
                    if (this.mPackages.get(it.next().name) == null) {
                        it.remove();
                    }
                }
                if (value.packages.size() == 0) {
                    arrayList.add(entry.getKey());
                }
            }
        }
        for (int i = 0; i < arrayList.size(); i++) {
            this.mSharedUsers.remove(arrayList.get(i));
        }
    }

    static PackageSetting createNewSetting(String str, PackageSetting packageSetting, PackageSetting packageSetting2, String str2, SharedUserSetting sharedUserSetting, File file, File file2, String str3, String str4, String str5, long j, int i, int i2, UserHandle userHandle, boolean z, boolean z2, boolean z3, String str6, List<String> list, UserManagerService userManagerService, String[] strArr, long[] jArr) {
        PackageSetting packageSetting3;
        boolean z4;
        if (packageSetting != null) {
            if (PackageManagerService.DEBUG_UPGRADE) {
                Log.v("PackageManager", "Package " + str + " is adopting original package " + packageSetting.name);
            }
            packageSetting3 = new PackageSetting(packageSetting, str);
            packageSetting3.childPackageNames = list != null ? new ArrayList(list) : null;
            packageSetting3.codePath = file;
            packageSetting3.legacyNativeLibraryPathString = str3;
            packageSetting3.parentPackageName = str6;
            packageSetting3.pkgFlags = i;
            packageSetting3.pkgPrivateFlags = i2;
            packageSetting3.primaryCpuAbiString = str4;
            packageSetting3.resourcePath = file2;
            packageSetting3.secondaryCpuAbiString = str5;
            packageSetting3.signatures = new PackageSignatures();
            packageSetting3.versionCode = j;
            packageSetting3.usesStaticLibraries = strArr;
            packageSetting3.usesStaticLibrariesVersions = jArr;
            packageSetting3.setTimeStamp(file.lastModified());
        } else {
            packageSetting3 = new PackageSetting(str, str2, file, file2, str3, str4, str5, null, j, i, i2, str6, list, 0, strArr, jArr);
            packageSetting3.setTimeStamp(file.lastModified());
            packageSetting3.sharedUser = sharedUserSetting;
            if ((i & 1) == 0) {
                List<UserInfo> allUsers = getAllUsers(userManagerService);
                int identifier = userHandle != null ? userHandle.getIdentifier() : 0;
                if (allUsers != null && z) {
                    for (UserInfo userInfo : allUsers) {
                        if (userHandle == null || (identifier == -1 && !isAdbInstallDisallowed(userManagerService, userInfo.id))) {
                            z4 = true;
                        } else if (identifier != userInfo.id) {
                            z4 = false;
                        }
                        packageSetting3.setUserState(userInfo.id, 0L, 0, z4, true, true, false, false, null, null, null, null, z2, z3, null, null, null, 0, 0, 0, null);
                    }
                }
            }
            if (sharedUserSetting != null) {
                packageSetting3.appId = sharedUserSetting.userId;
            } else if (packageSetting2 != null) {
                packageSetting3.signatures = new PackageSignatures(packageSetting2.signatures);
                packageSetting3.appId = packageSetting2.appId;
                packageSetting3.getPermissionsState().copyFrom(packageSetting2.getPermissionsState());
                List<UserInfo> allUsers2 = getAllUsers(userManagerService);
                if (allUsers2 != null) {
                    Iterator<UserInfo> it = allUsers2.iterator();
                    while (it.hasNext()) {
                        int i3 = it.next().id;
                        packageSetting3.setDisabledComponentsCopy(packageSetting2.getDisabledComponents(i3), i3);
                        packageSetting3.setEnabledComponentsCopy(packageSetting2.getEnabledComponents(i3), i3);
                    }
                }
            }
        }
        return packageSetting3;
    }

    static void updatePackageSetting(PackageSetting packageSetting, PackageSetting packageSetting2, SharedUserSetting sharedUserSetting, File file, File file2, String str, String str2, String str3, int i, int i2, List<String> list, UserManagerService userManagerService, String[] strArr, long[] jArr) throws PackageManagerException {
        List<UserInfo> allUsers;
        String str4 = packageSetting.name;
        if (packageSetting.sharedUser != sharedUserSetting) {
            StringBuilder sb = new StringBuilder();
            sb.append("Package ");
            sb.append(str4);
            sb.append(" shared user changed from ");
            sb.append(packageSetting.sharedUser != null ? packageSetting.sharedUser.name : "<nothing>");
            sb.append(" to ");
            sb.append(sharedUserSetting != null ? sharedUserSetting.name : "<nothing>");
            PackageManagerService.reportSettingsProblem(5, sb.toString());
            throw new PackageManagerException(-8, "Updating application package " + str4 + " failed");
        }
        if (!packageSetting.codePath.equals(file)) {
            boolean zIsSystem = packageSetting.isSystem();
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Update");
            sb2.append(zIsSystem ? " system" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            sb2.append(" package ");
            sb2.append(str4);
            sb2.append(" code path from ");
            sb2.append(packageSetting.codePathString);
            sb2.append(" to ");
            sb2.append(file.toString());
            sb2.append("; Retain data and using new");
            Slog.i("PackageManager", sb2.toString());
            if (!zIsSystem) {
                if ((i & 1) != 0 && packageSetting2 == null && (allUsers = getAllUsers(userManagerService)) != null) {
                    Iterator<UserInfo> it = allUsers.iterator();
                    while (it.hasNext()) {
                        packageSetting.setInstalled(true, it.next().id);
                    }
                }
                packageSetting.legacyNativeLibraryPathString = str;
            }
            packageSetting.codePath = file;
            packageSetting.codePathString = file.toString();
        }
        if (!packageSetting.resourcePath.equals(file2)) {
            boolean zIsSystem2 = packageSetting.isSystem();
            StringBuilder sb3 = new StringBuilder();
            sb3.append("Update");
            sb3.append(zIsSystem2 ? " system" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            sb3.append(" package ");
            sb3.append(str4);
            sb3.append(" resource path from ");
            sb3.append(packageSetting.resourcePathString);
            sb3.append(" to ");
            sb3.append(file2.toString());
            sb3.append("; Retain data and using new");
            Slog.i("PackageManager", sb3.toString());
            packageSetting.resourcePath = file2;
            packageSetting.resourcePathString = file2.toString();
        }
        packageSetting.pkgFlags &= -2;
        packageSetting.pkgPrivateFlags &= -917513;
        packageSetting.pkgFlags |= i & 1;
        packageSetting.pkgPrivateFlags |= i2 & 8;
        packageSetting.pkgPrivateFlags |= i2 & DumpState.DUMP_INTENT_FILTER_VERIFIERS;
        packageSetting.pkgPrivateFlags |= i2 & DumpState.DUMP_DOMAIN_PREFERRED;
        packageSetting.pkgPrivateFlags |= i2 & DumpState.DUMP_FROZEN;
        packageSetting.primaryCpuAbiString = str2;
        packageSetting.secondaryCpuAbiString = str3;
        if (list != null) {
            packageSetting.childPackageNames = new ArrayList(list);
        }
        if (strArr != null && jArr != null && strArr.length == jArr.length) {
            packageSetting.usesStaticLibraries = strArr;
            packageSetting.usesStaticLibrariesVersions = jArr;
        } else {
            packageSetting.usesStaticLibraries = null;
            packageSetting.usesStaticLibrariesVersions = null;
        }
    }

    void addUserToSettingLPw(PackageSetting packageSetting) throws PackageManagerException {
        if (packageSetting.appId == 0) {
            packageSetting.appId = newUserIdLPw(packageSetting);
        } else {
            addUserIdLPw(packageSetting.appId, packageSetting, packageSetting.name);
        }
        if (packageSetting.appId < 0) {
            PackageManagerService.reportSettingsProblem(5, "Package " + packageSetting.name + " could not be assigned a valid UID");
            throw new PackageManagerException(-4, "Package " + packageSetting.name + " could not be assigned a valid UID");
        }
    }

    void writeUserRestrictionsLPw(PackageSetting packageSetting, PackageSetting packageSetting2) {
        List<UserInfo> allUsers;
        PackageUserState userState;
        if (getPackageLPr(packageSetting.name) == null || (allUsers = getAllUsers(UserManagerService.getInstance())) == null) {
            return;
        }
        for (UserInfo userInfo : allUsers) {
            if (packageSetting2 == null) {
                userState = PackageSettingBase.DEFAULT_USER_STATE;
            } else {
                userState = packageSetting2.readUserState(userInfo.id);
            }
            if (!userState.equals(packageSetting.readUserState(userInfo.id))) {
                writePackageRestrictionsLPr(userInfo.id);
            }
        }
    }

    static boolean isAdbInstallDisallowed(UserManagerService userManagerService, int i) {
        return userManagerService.hasUserRestriction("no_debugging_features", i);
    }

    void insertPackageSettingLPw(PackageSetting packageSetting, PackageParser.Package r4) {
        if (packageSetting.signatures.mSigningDetails.signatures == null) {
            packageSetting.signatures.mSigningDetails = r4.mSigningDetails;
        }
        if (packageSetting.sharedUser != null && packageSetting.sharedUser.signatures.mSigningDetails.signatures == null) {
            packageSetting.sharedUser.signatures.mSigningDetails = r4.mSigningDetails;
        }
        addPackageSettingLPw(packageSetting, packageSetting.sharedUser);
    }

    private void addPackageSettingLPw(PackageSetting packageSetting, SharedUserSetting sharedUserSetting) {
        this.mPackages.put(packageSetting.name, packageSetting);
        if (sharedUserSetting != null) {
            if (packageSetting.sharedUser != null && packageSetting.sharedUser != sharedUserSetting) {
                PackageManagerService.reportSettingsProblem(6, "Package " + packageSetting.name + " was user " + packageSetting.sharedUser + " but is now " + sharedUserSetting + "; I am not changing its files so it will probably fail!");
                packageSetting.sharedUser.removePackage(packageSetting);
            } else if (packageSetting.appId != sharedUserSetting.userId) {
                PackageManagerService.reportSettingsProblem(6, "Package " + packageSetting.name + " was user id " + packageSetting.appId + " but is now user " + sharedUserSetting + " with id " + sharedUserSetting.userId + "; I am not changing its files so it will probably fail!");
            }
            sharedUserSetting.addPackage(packageSetting);
            packageSetting.sharedUser = sharedUserSetting;
            packageSetting.appId = sharedUserSetting.userId;
        }
        Object userIdLPr = getUserIdLPr(packageSetting.appId);
        if (sharedUserSetting == null) {
            if (userIdLPr != null && userIdLPr != packageSetting) {
                replaceUserIdLPw(packageSetting.appId, packageSetting);
            }
        } else if (userIdLPr != null && userIdLPr != sharedUserSetting) {
            replaceUserIdLPw(packageSetting.appId, sharedUserSetting);
        }
        IntentFilterVerificationInfo intentFilterVerificationInfo = this.mRestoredIntentFilterVerifications.get(packageSetting.name);
        if (intentFilterVerificationInfo != null) {
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                Slog.i(TAG, "Applying restored IVI for " + packageSetting.name + " : " + intentFilterVerificationInfo.getStatusString());
            }
            this.mRestoredIntentFilterVerifications.remove(packageSetting.name);
            packageSetting.setIntentFilterVerificationInfo(intentFilterVerificationInfo);
        }
    }

    int updateSharedUserPermsLPw(PackageSetting packageSetting, int i) {
        boolean z;
        boolean z2;
        if (packageSetting == null || packageSetting.pkg == null) {
            Slog.i("PackageManager", "Trying to update info for null package. Just ignoring");
            return -10000;
        }
        if (packageSetting.sharedUser == null) {
            return -10000;
        }
        SharedUserSetting sharedUserSetting = packageSetting.sharedUser;
        for (String str : packageSetting.pkg.requestedPermissions) {
            BasePermission permission = this.mPermissions.getPermission(str);
            if (permission != null) {
                Iterator<PackageSetting> it = sharedUserSetting.packages.iterator();
                while (true) {
                    if (it.hasNext()) {
                        PackageSetting next = it.next();
                        if (next.pkg != null && !next.pkg.packageName.equals(packageSetting.pkg.packageName) && next.pkg.requestedPermissions.contains(str)) {
                            z = true;
                            break;
                        }
                    } else {
                        z = false;
                        break;
                    }
                }
                if (z) {
                    continue;
                } else {
                    PermissionsState permissionsState = sharedUserSetting.getPermissionsState();
                    PackageSetting disabledSystemPkgLPr = getDisabledSystemPkgLPr(packageSetting.pkg.packageName);
                    if (disabledSystemPkgLPr != null) {
                        Iterator it2 = disabledSystemPkgLPr.pkg.requestedPermissions.iterator();
                        while (true) {
                            if (it2.hasNext()) {
                                if (((String) it2.next()).equals(str)) {
                                    z2 = true;
                                    break;
                                }
                            } else {
                                z2 = false;
                                break;
                            }
                        }
                        if (z2) {
                            continue;
                        }
                    }
                    permissionsState.updatePermissionFlags(permission, i, 255, 0);
                    if (permissionsState.revokeInstallPermission(permission) == 1) {
                        return -1;
                    }
                    if (permissionsState.revokeRuntimePermission(permission, i) == 1) {
                        return i;
                    }
                }
            }
        }
        return -10000;
    }

    int removePackageLPw(String str) {
        PackageSetting packageSetting = this.mPackages.get(str);
        if (packageSetting != null) {
            this.mPackages.remove(str);
            removeInstallerPackageStatus(str);
            if (packageSetting.sharedUser != null) {
                packageSetting.sharedUser.removePackage(packageSetting);
                if (packageSetting.sharedUser.packages.size() == 0) {
                    this.mSharedUsers.remove(packageSetting.sharedUser.name);
                    removeUserIdLPw(packageSetting.sharedUser.userId);
                    return packageSetting.sharedUser.userId;
                }
                return -1;
            }
            removeUserIdLPw(packageSetting.appId);
            return packageSetting.appId;
        }
        return -1;
    }

    private void removeInstallerPackageStatus(String str) {
        if (!this.mInstallerPackages.contains(str)) {
            return;
        }
        for (int i = 0; i < this.mPackages.size(); i++) {
            PackageSetting packageSettingValueAt = this.mPackages.valueAt(i);
            String installerPackageName = packageSettingValueAt.getInstallerPackageName();
            if (installerPackageName != null && installerPackageName.equals(str)) {
                packageSettingValueAt.setInstallerPackageName(null);
                packageSettingValueAt.isOrphaned = true;
            }
        }
        this.mInstallerPackages.remove(str);
    }

    private void replacePackageLPw(String str, PackageSetting packageSetting) {
        PackageSetting packageSetting2 = this.mPackages.get(str);
        if (packageSetting2 != null) {
            if (packageSetting2.sharedUser != null) {
                packageSetting2.sharedUser.removePackage(packageSetting2);
                packageSetting2.sharedUser.addPackage(packageSetting);
            } else {
                replaceUserIdLPw(packageSetting2.appId, packageSetting);
            }
        }
        this.mPackages.put(str, packageSetting);
    }

    private boolean addUserIdLPw(int i, Object obj, Object obj2) {
        if (i > 19999) {
            return false;
        }
        if (i >= 10000) {
            int i2 = i - 10000;
            for (int size = this.mUserIds.size(); i2 >= size; size++) {
                this.mUserIds.add(null);
            }
            if (this.mUserIds.get(i2) != null) {
                PackageManagerService.reportSettingsProblem(6, "Adding duplicate user id: " + i + " name=" + obj2);
                return false;
            }
            this.mUserIds.set(i2, obj);
            return true;
        }
        if (this.mOtherUserIds.get(i) != null) {
            PackageManagerService.reportSettingsProblem(6, "Adding duplicate shared id: " + i + " name=" + obj2);
            return false;
        }
        this.mOtherUserIds.put(i, obj);
        return true;
    }

    public Object getUserIdLPr(int i) {
        if (i >= 10000) {
            int i2 = i - 10000;
            if (i2 < this.mUserIds.size()) {
                return this.mUserIds.get(i2);
            }
            return null;
        }
        return this.mOtherUserIds.get(i);
    }

    private void removeUserIdLPw(int i) {
        if (i >= 10000) {
            int i2 = i - 10000;
            if (i2 < this.mUserIds.size()) {
                this.mUserIds.set(i2, null);
            }
        } else {
            this.mOtherUserIds.remove(i);
        }
        setFirstAvailableUid(i + 1);
    }

    private void replaceUserIdLPw(int i, Object obj) {
        if (i >= 10000) {
            int i2 = i - 10000;
            if (i2 < this.mUserIds.size()) {
                this.mUserIds.set(i2, obj);
                return;
            }
            return;
        }
        this.mOtherUserIds.put(i, obj);
    }

    PreferredIntentResolver editPreferredActivitiesLPw(int i) {
        PreferredIntentResolver preferredIntentResolver = this.mPreferredActivities.get(i);
        if (preferredIntentResolver == null) {
            PreferredIntentResolver preferredIntentResolver2 = new PreferredIntentResolver();
            this.mPreferredActivities.put(i, preferredIntentResolver2);
            return preferredIntentResolver2;
        }
        return preferredIntentResolver;
    }

    PersistentPreferredIntentResolver editPersistentPreferredActivitiesLPw(int i) {
        PersistentPreferredIntentResolver persistentPreferredIntentResolver = this.mPersistentPreferredActivities.get(i);
        if (persistentPreferredIntentResolver == null) {
            PersistentPreferredIntentResolver persistentPreferredIntentResolver2 = new PersistentPreferredIntentResolver();
            this.mPersistentPreferredActivities.put(i, persistentPreferredIntentResolver2);
            return persistentPreferredIntentResolver2;
        }
        return persistentPreferredIntentResolver;
    }

    CrossProfileIntentResolver editCrossProfileIntentResolverLPw(int i) {
        CrossProfileIntentResolver crossProfileIntentResolver = this.mCrossProfileIntentResolvers.get(i);
        if (crossProfileIntentResolver == null) {
            CrossProfileIntentResolver crossProfileIntentResolver2 = new CrossProfileIntentResolver();
            this.mCrossProfileIntentResolvers.put(i, crossProfileIntentResolver2);
            return crossProfileIntentResolver2;
        }
        return crossProfileIntentResolver;
    }

    IntentFilterVerificationInfo getIntentFilterVerificationLPr(String str) {
        PackageSetting packageSetting = this.mPackages.get(str);
        if (packageSetting == null) {
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                Slog.w("PackageManager", "No package known: " + str);
                return null;
            }
            return null;
        }
        return packageSetting.getIntentFilterVerificationInfo();
    }

    IntentFilterVerificationInfo createIntentFilterVerificationIfNeededLPw(String str, ArraySet<String> arraySet) {
        PackageSetting packageSetting = this.mPackages.get(str);
        if (packageSetting == null) {
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                Slog.w("PackageManager", "No package known: " + str);
                return null;
            }
            return null;
        }
        IntentFilterVerificationInfo intentFilterVerificationInfo = packageSetting.getIntentFilterVerificationInfo();
        if (intentFilterVerificationInfo == null) {
            intentFilterVerificationInfo = new IntentFilterVerificationInfo(str, arraySet);
            packageSetting.setIntentFilterVerificationInfo(intentFilterVerificationInfo);
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                Slog.d("PackageManager", "Creating new IntentFilterVerificationInfo for pkg: " + str);
            }
        } else {
            intentFilterVerificationInfo.setDomains(arraySet);
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                Slog.d("PackageManager", "Setting domains to existing IntentFilterVerificationInfo for pkg: " + str + " and with domains: " + intentFilterVerificationInfo.getDomainsString());
            }
        }
        return intentFilterVerificationInfo;
    }

    int getIntentFilterVerificationStatusLPr(String str, int i) {
        PackageSetting packageSetting = this.mPackages.get(str);
        if (packageSetting == null) {
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                Slog.w("PackageManager", "No package known: " + str);
                return 0;
            }
            return 0;
        }
        return (int) (packageSetting.getDomainVerificationStatusForUser(i) >> 32);
    }

    boolean updateIntentFilterVerificationStatusLPw(String str, int i, int i2) {
        PackageSetting packageSetting = this.mPackages.get(str);
        int i3 = 0;
        if (packageSetting == null) {
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                Slog.w("PackageManager", "No package known: " + str);
            }
            return false;
        }
        if (i == 2) {
            i3 = this.mNextAppLinkGeneration.get(i2) + 1;
            this.mNextAppLinkGeneration.put(i2, i3);
        }
        packageSetting.setDomainVerificationStatusForUser(i, i3, i2);
        return true;
    }

    List<IntentFilterVerificationInfo> getIntentFilterVerificationsLPr(String str) {
        if (str == null) {
            return Collections.emptyList();
        }
        ArrayList arrayList = new ArrayList();
        Iterator<PackageSetting> it = this.mPackages.values().iterator();
        while (it.hasNext()) {
            IntentFilterVerificationInfo intentFilterVerificationInfo = it.next().getIntentFilterVerificationInfo();
            if (intentFilterVerificationInfo != null && !TextUtils.isEmpty(intentFilterVerificationInfo.getPackageName()) && intentFilterVerificationInfo.getPackageName().equalsIgnoreCase(str)) {
                arrayList.add(intentFilterVerificationInfo);
            }
        }
        return arrayList;
    }

    boolean removeIntentFilterVerificationLPw(String str, int i, boolean z) {
        PackageSetting packageSetting = this.mPackages.get(str);
        if (packageSetting == null) {
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                Slog.w("PackageManager", "No package known: " + str);
                return false;
            }
            return false;
        }
        if (z) {
            packageSetting.clearDomainVerificationStatusForUser(i);
        }
        packageSetting.setIntentFilterVerificationInfo(null);
        return true;
    }

    boolean removeIntentFilterVerificationLPw(String str, int[] iArr) {
        boolean zRemoveIntentFilterVerificationLPw = false;
        for (int i : iArr) {
            zRemoveIntentFilterVerificationLPw |= removeIntentFilterVerificationLPw(str, i, true);
        }
        return zRemoveIntentFilterVerificationLPw;
    }

    boolean setDefaultBrowserPackageNameLPw(String str, int i) {
        if (i == -1) {
            return false;
        }
        if (str != null) {
            this.mDefaultBrowserApp.put(i, str);
        } else {
            this.mDefaultBrowserApp.remove(i);
        }
        writePackageRestrictionsLPr(i);
        return true;
    }

    String getDefaultBrowserPackageNameLPw(int i) {
        if (i == -1) {
            return null;
        }
        return this.mDefaultBrowserApp.get(i);
    }

    boolean setDefaultDialerPackageNameLPw(String str, int i) {
        if (i == -1) {
            return false;
        }
        this.mDefaultDialerApp.put(i, str);
        writePackageRestrictionsLPr(i);
        return true;
    }

    String getDefaultDialerPackageNameLPw(int i) {
        if (i == -1) {
            return null;
        }
        return this.mDefaultDialerApp.get(i);
    }

    private File getUserPackagesStateFile(int i) {
        return new File(new File(new File(this.mSystemDir, DatabaseHelper.SoundModelContract.KEY_USERS), Integer.toString(i)), "package-restrictions.xml");
    }

    private File getUserRuntimePermissionsFile(int i) {
        return new File(new File(new File(this.mSystemDir, DatabaseHelper.SoundModelContract.KEY_USERS), Integer.toString(i)), RUNTIME_PERMISSIONS_FILE_NAME);
    }

    private File getUserPackagesStateBackupFile(int i) {
        return new File(Environment.getUserSystemDirectory(i), "package-restrictions-backup.xml");
    }

    void writeAllUsersPackageRestrictionsLPr() {
        List<UserInfo> allUsers = getAllUsers(UserManagerService.getInstance());
        if (allUsers == null) {
            return;
        }
        Iterator<UserInfo> it = allUsers.iterator();
        while (it.hasNext()) {
            writePackageRestrictionsLPr(it.next().id);
        }
    }

    void writeAllRuntimePermissionsLPr() {
        for (int i : UserManagerService.getInstance().getUserIds()) {
            this.mRuntimePermissionsPersistence.writePermissionsForUserAsyncLPr(i);
        }
    }

    boolean areDefaultRuntimePermissionsGrantedLPr(int i) {
        return this.mRuntimePermissionsPersistence.areDefaultRuntimPermissionsGrantedLPr(i);
    }

    void onDefaultRuntimePermissionsGrantedLPr(int i) {
        this.mRuntimePermissionsPersistence.onDefaultRuntimePermissionsGrantedLPr(i);
    }

    public VersionInfo findOrCreateVersion(String str) {
        VersionInfo versionInfo = this.mVersion.get(str);
        if (versionInfo == null) {
            VersionInfo versionInfo2 = new VersionInfo();
            this.mVersion.put(str, versionInfo2);
            return versionInfo2;
        }
        return versionInfo;
    }

    public VersionInfo getInternalVersion() {
        return this.mVersion.get(StorageManager.UUID_PRIVATE_INTERNAL);
    }

    public VersionInfo getExternalVersion() {
        return this.mVersion.get("primary_physical");
    }

    public void onVolumeForgotten(String str) {
        this.mVersion.remove(str);
    }

    void readPreferredActivitiesLPw(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4) {
                        if (xmlPullParser.getName().equals(TAG_ITEM)) {
                            PreferredActivity preferredActivity = new PreferredActivity(xmlPullParser);
                            if (preferredActivity.mPref.getParseError() == null) {
                                editPreferredActivitiesLPw(i).addFilter(preferredActivity);
                            } else {
                                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <preferred-activity> " + preferredActivity.mPref.getParseError() + " at " + xmlPullParser.getPositionDescription());
                            }
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Unknown element under <preferred-activities>: " + xmlPullParser.getName());
                            XmlUtils.skipCurrentTag(xmlPullParser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void readPersistentPreferredActivitiesLPw(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4) {
                        if (xmlPullParser.getName().equals(TAG_ITEM)) {
                            editPersistentPreferredActivitiesLPw(i).addFilter(new PersistentPreferredActivity(xmlPullParser));
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Unknown element under <persistent-preferred-activities>: " + xmlPullParser.getName());
                            XmlUtils.skipCurrentTag(xmlPullParser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void readCrossProfileIntentFiltersLPw(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4) {
                        String name = xmlPullParser.getName();
                        if (name.equals(TAG_ITEM)) {
                            editCrossProfileIntentResolverLPw(i).addFilter(new CrossProfileIntentFilter(xmlPullParser));
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Unknown element under crossProfile-intent-filters: " + name);
                            XmlUtils.skipCurrentTag(xmlPullParser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void readDomainVerificationLPw(XmlPullParser xmlPullParser, PackageSettingBase packageSettingBase) throws XmlPullParserException, IOException {
        packageSettingBase.setIntentFilterVerificationInfo(new IntentFilterVerificationInfo(xmlPullParser));
    }

    private void readRestoredIntentFilterVerifications(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4) {
                        String name = xmlPullParser.getName();
                        if (name.equals(TAG_DOMAIN_VERIFICATION)) {
                            IntentFilterVerificationInfo intentFilterVerificationInfo = new IntentFilterVerificationInfo(xmlPullParser);
                            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                                Slog.i(TAG, "Restored IVI for " + intentFilterVerificationInfo.getPackageName() + " status=" + intentFilterVerificationInfo.getStatusString());
                            }
                            this.mRestoredIntentFilterVerifications.put(intentFilterVerificationInfo.getPackageName(), intentFilterVerificationInfo);
                        } else {
                            Slog.w(TAG, "Unknown element: " + name);
                            XmlUtils.skipCurrentTag(xmlPullParser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    void readDefaultAppsLPw(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4) {
                        String name = xmlPullParser.getName();
                        if (name.equals(TAG_DEFAULT_BROWSER)) {
                            this.mDefaultBrowserApp.put(i, xmlPullParser.getAttributeValue(null, "packageName"));
                        } else if (name.equals(TAG_DEFAULT_DIALER)) {
                            this.mDefaultDialerApp.put(i, xmlPullParser.getAttributeValue(null, "packageName"));
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Unknown element under default-apps: " + xmlPullParser.getName());
                            XmlUtils.skipCurrentTag(xmlPullParser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    void readBlockUninstallPackagesLPw(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        ArraySet<String> arraySet = new ArraySet<>();
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != 4) {
                if (xmlPullParser.getName().equals(TAG_BLOCK_UNINSTALL)) {
                    arraySet.add(xmlPullParser.getAttributeValue(null, "packageName"));
                } else {
                    PackageManagerService.reportSettingsProblem(5, "Unknown element under block-uninstall-packages: " + xmlPullParser.getName());
                    XmlUtils.skipCurrentTag(xmlPullParser);
                }
            }
        }
        if (arraySet.isEmpty()) {
            this.mBlockUninstallPackages.remove(i);
        } else {
            this.mBlockUninstallPackages.put(i, arraySet);
        }
    }

    void readPackageRestrictionsLPr(int i) {
        FileInputStream fileInputStream;
        Settings settings;
        int i2;
        XmlPullParser xmlPullParserNewPullParser;
        int next;
        char c;
        boolean z;
        boolean z2;
        int i3;
        boolean z3;
        char c2;
        String str;
        int i4;
        XmlPullParser xmlPullParser;
        int i5;
        boolean z4;
        int i6;
        boolean z5;
        Settings settings2 = this;
        int i7 = i;
        File userPackagesStateFile = getUserPackagesStateFile(i);
        File userPackagesStateBackupFile = getUserPackagesStateBackupFile(i);
        int i8 = 4;
        String str2 = null;
        if (userPackagesStateBackupFile.exists()) {
            try {
                fileInputStream = new FileInputStream(userPackagesStateBackupFile);
                try {
                    settings2.mReadMessages.append("Reading from backup stopped packages file\n");
                    PackageManagerService.reportSettingsProblem(4, "Need to read from backup stopped packages file");
                    if (userPackagesStateFile.exists()) {
                        Slog.w("PackageManager", "Cleaning up stopped packages file " + userPackagesStateFile);
                        userPackagesStateFile.delete();
                    }
                } catch (IOException e) {
                }
            } catch (IOException e2) {
                fileInputStream = null;
            }
        } else {
            fileInputStream = null;
        }
        int i9 = 6;
        try {
            if (fileInputStream == null) {
                try {
                    try {
                        if (!userPackagesStateFile.exists()) {
                            settings2.mReadMessages.append("No stopped packages file found\n");
                            PackageManagerService.reportSettingsProblem(4, "No stopped packages file; assuming all started");
                            Iterator<PackageSetting> it = settings2.mPackages.values().iterator();
                            while (it.hasNext()) {
                                it.next().setUserState(i7, 0L, 0, true, false, false, false, false, null, null, null, null, false, false, null, null, null, 0, 0, 0, null);
                                i9 = 6;
                                i7 = i;
                            }
                            return;
                        }
                        fileInputStream = new FileInputStream(userPackagesStateFile);
                        FileInputStream fileInputStream2 = fileInputStream;
                        xmlPullParserNewPullParser = Xml.newPullParser();
                        xmlPullParserNewPullParser.setInput(fileInputStream2, StandardCharsets.UTF_8.name());
                        do {
                            next = xmlPullParserNewPullParser.next();
                            c = 2;
                            z = true;
                            if (next != 2) {
                                break;
                            }
                        } while (next != 1);
                        if (next == 2) {
                            settings2.mReadMessages.append("No start tag found in package restrictions file\n");
                            PackageManagerService.reportSettingsProblem(5, "No start tag found in package manager stopped packages");
                            return;
                        }
                        int depth = xmlPullParserNewPullParser.getDepth();
                        boolean z6 = false;
                        int i10 = 0;
                        while (true) {
                            int next2 = xmlPullParserNewPullParser.next();
                            if (next2 != z && (next2 != 3 || xmlPullParserNewPullParser.getDepth() > depth)) {
                                if (next2 == 3 || next2 == i8) {
                                    settings = settings2;
                                    z2 = z6;
                                    i3 = depth;
                                    z3 = z;
                                    c2 = c;
                                    str = str2;
                                    i4 = i8;
                                    xmlPullParser = xmlPullParserNewPullParser;
                                } else {
                                    String name = xmlPullParserNewPullParser.getName();
                                    if (name.equals(TAG_PACKAGE)) {
                                        String attributeValue = xmlPullParserNewPullParser.getAttributeValue(str2, ATTR_NAME);
                                        PackageSetting packageSetting = settings2.mPackages.get(attributeValue);
                                        if (packageSetting == null) {
                                            Slog.w("PackageManager", "No package known for stopped package " + attributeValue);
                                            XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                            settings = settings2;
                                            z2 = z6;
                                            i3 = depth;
                                            z3 = z;
                                            c2 = c;
                                            str = str2;
                                            i4 = i8;
                                            xmlPullParser = xmlPullParserNewPullParser;
                                        } else {
                                            long longAttribute = XmlUtils.readLongAttribute(xmlPullParserNewPullParser, ATTR_CE_DATA_INODE, 0L);
                                            boolean booleanAttribute = XmlUtils.readBooleanAttribute(xmlPullParserNewPullParser, ATTR_INSTALLED, z);
                                            boolean booleanAttribute2 = XmlUtils.readBooleanAttribute(xmlPullParserNewPullParser, ATTR_STOPPED, z6);
                                            boolean booleanAttribute3 = XmlUtils.readBooleanAttribute(xmlPullParserNewPullParser, ATTR_NOT_LAUNCHED, z6);
                                            String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(str2, ATTR_BLOCKED);
                                            boolean z7 = attributeValue2 == null ? z6 : Boolean.parseBoolean(attributeValue2);
                                            String attributeValue3 = xmlPullParserNewPullParser.getAttributeValue(str2, ATTR_HIDDEN);
                                            if (attributeValue3 != null) {
                                                z7 = Boolean.parseBoolean(attributeValue3);
                                            }
                                            boolean z8 = z7;
                                            boolean booleanAttribute4 = XmlUtils.readBooleanAttribute(xmlPullParserNewPullParser, ATTR_SUSPENDED, z6);
                                            String attributeValue4 = xmlPullParserNewPullParser.getAttributeValue(str2, ATTR_SUSPENDING_PACKAGE);
                                            String attributeValue5 = xmlPullParserNewPullParser.getAttributeValue(str2, ATTR_SUSPEND_DIALOG_MESSAGE);
                                            if (booleanAttribute4 && attributeValue4 == null) {
                                                attributeValue4 = PackageManagerService.PLATFORM_PACKAGE_NAME;
                                            }
                                            String str3 = attributeValue4;
                                            boolean booleanAttribute5 = XmlUtils.readBooleanAttribute(xmlPullParserNewPullParser, ATTR_BLOCK_UNINSTALL, false);
                                            boolean booleanAttribute6 = XmlUtils.readBooleanAttribute(xmlPullParserNewPullParser, ATTR_INSTANT_APP, false);
                                            boolean booleanAttribute7 = XmlUtils.readBooleanAttribute(xmlPullParserNewPullParser, ATTR_VIRTUAL_PRELOAD, false);
                                            int intAttribute = XmlUtils.readIntAttribute(xmlPullParserNewPullParser, ATTR_ENABLED, 0);
                                            int i11 = depth;
                                            String attributeValue6 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_ENABLED_CALLER);
                                            String attributeValue7 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_HARMFUL_APP_WARNING);
                                            int intAttribute2 = XmlUtils.readIntAttribute(xmlPullParserNewPullParser, ATTR_DOMAIN_VERIFICATON_STATE, 0);
                                            int intAttribute3 = XmlUtils.readIntAttribute(xmlPullParserNewPullParser, ATTR_APP_LINK_GENERATION, 0);
                                            int i12 = intAttribute3 > i10 ? intAttribute3 : i10;
                                            int intAttribute4 = XmlUtils.readIntAttribute(xmlPullParserNewPullParser, ATTR_INSTALL_REASON, 0);
                                            int depth2 = xmlPullParserNewPullParser.getDepth();
                                            ArraySet<String> componentsLPr = null;
                                            ArraySet<String> componentsLPr2 = null;
                                            PersistableBundle persistableBundleRestoreFromXml = null;
                                            PersistableBundle persistableBundleRestoreFromXml2 = null;
                                            while (true) {
                                                int next3 = xmlPullParserNewPullParser.next();
                                                i5 = intAttribute3;
                                                if (next3 != 1 && (next3 != 3 || xmlPullParserNewPullParser.getDepth() > depth2)) {
                                                    if (next3 != 3) {
                                                        z5 = z8;
                                                        if (next3 != 4) {
                                                            String name2 = xmlPullParserNewPullParser.getName();
                                                            byte b = -1;
                                                            int iHashCode = name2.hashCode();
                                                            if (iHashCode != -2027581689) {
                                                                if (iHashCode != -1963032286) {
                                                                    if (iHashCode != -1592287551) {
                                                                        if (iHashCode == -1422791362 && name2.equals(TAG_SUSPENDED_LAUNCHER_EXTRAS)) {
                                                                            b = 3;
                                                                        }
                                                                    } else if (name2.equals(TAG_SUSPENDED_APP_EXTRAS)) {
                                                                        b = 2;
                                                                    }
                                                                } else if (name2.equals(TAG_ENABLED_COMPONENTS)) {
                                                                    b = 0;
                                                                }
                                                            } else if (name2.equals(TAG_DISABLED_COMPONENTS)) {
                                                                b = 1;
                                                            }
                                                            switch (b) {
                                                                case 0:
                                                                    componentsLPr = settings2.readComponentsLPr(xmlPullParserNewPullParser);
                                                                    break;
                                                                case 1:
                                                                    componentsLPr2 = settings2.readComponentsLPr(xmlPullParserNewPullParser);
                                                                    break;
                                                                case 2:
                                                                    persistableBundleRestoreFromXml = PersistableBundle.restoreFromXml(xmlPullParserNewPullParser);
                                                                    break;
                                                                case 3:
                                                                    persistableBundleRestoreFromXml2 = PersistableBundle.restoreFromXml(xmlPullParserNewPullParser);
                                                                    break;
                                                                default:
                                                                    Slog.wtf(TAG, "Unknown tag " + xmlPullParserNewPullParser.getName() + " under tag " + TAG_PACKAGE);
                                                                    break;
                                                            }
                                                        }
                                                    } else {
                                                        z5 = z8;
                                                    }
                                                    intAttribute3 = i5;
                                                    z8 = z5;
                                                }
                                            }
                                            boolean z9 = z8;
                                            if (booleanAttribute5) {
                                                z4 = true;
                                                i6 = i;
                                                settings2.setBlockUninstallLPw(i6, attributeValue, true);
                                            } else {
                                                z4 = true;
                                                i6 = i;
                                            }
                                            z2 = false;
                                            i3 = i11;
                                            z3 = z4;
                                            i4 = 4;
                                            c2 = 2;
                                            str = null;
                                            xmlPullParser = xmlPullParserNewPullParser;
                                            try {
                                                try {
                                                    packageSetting.setUserState(i6, longAttribute, intAttribute, booleanAttribute, booleanAttribute2, booleanAttribute3, z9, booleanAttribute4, str3, attributeValue5, persistableBundleRestoreFromXml, persistableBundleRestoreFromXml2, booleanAttribute6, booleanAttribute7, attributeValue6, componentsLPr, componentsLPr2, intAttribute2, i5, intAttribute4, attributeValue7);
                                                    i10 = i12;
                                                    settings = this;
                                                } catch (XmlPullParserException e3) {
                                                    e = e3;
                                                    i2 = 6;
                                                    settings = this;
                                                }
                                            } catch (IOException e4) {
                                                e = e4;
                                                settings = this;
                                                settings.mReadMessages.append("Error reading: " + e.toString());
                                                PackageManagerService.reportSettingsProblem(6, "Error reading settings: " + e);
                                                Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
                                                return;
                                            }
                                        }
                                    } else {
                                        z2 = z6;
                                        i3 = depth;
                                        z3 = z;
                                        c2 = c;
                                        str = str2;
                                        i4 = i8;
                                        xmlPullParser = xmlPullParserNewPullParser;
                                        try {
                                            if (name.equals("preferred-activities")) {
                                                settings = this;
                                                try {
                                                    settings.readPreferredActivitiesLPw(xmlPullParser, i);
                                                } catch (IOException e5) {
                                                    e = e5;
                                                    settings.mReadMessages.append("Error reading: " + e.toString());
                                                    PackageManagerService.reportSettingsProblem(6, "Error reading settings: " + e);
                                                    Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
                                                    return;
                                                } catch (XmlPullParserException e6) {
                                                    e = e6;
                                                    i2 = 6;
                                                    settings.mReadMessages.append("Error reading: " + e.toString());
                                                    PackageManagerService.reportSettingsProblem(i2, "Error reading stopped packages: " + e);
                                                    Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
                                                }
                                            } else {
                                                settings = this;
                                                if (name.equals(TAG_PERSISTENT_PREFERRED_ACTIVITIES)) {
                                                    settings.readPersistentPreferredActivitiesLPw(xmlPullParser, i);
                                                } else if (name.equals(TAG_CROSS_PROFILE_INTENT_FILTERS)) {
                                                    settings.readCrossProfileIntentFiltersLPw(xmlPullParser, i);
                                                } else if (name.equals(TAG_DEFAULT_APPS)) {
                                                    settings.readDefaultAppsLPw(xmlPullParser, i);
                                                } else if (name.equals(TAG_BLOCK_UNINSTALL_PACKAGES)) {
                                                    settings.readBlockUninstallPackagesLPw(xmlPullParser, i);
                                                } else {
                                                    Slog.w("PackageManager", "Unknown element under <stopped-packages>: " + xmlPullParser.getName());
                                                    XmlUtils.skipCurrentTag(xmlPullParser);
                                                }
                                            }
                                        } catch (XmlPullParserException e7) {
                                            e = e7;
                                            settings = this;
                                            i2 = 6;
                                            settings.mReadMessages.append("Error reading: " + e.toString());
                                            PackageManagerService.reportSettingsProblem(i2, "Error reading stopped packages: " + e);
                                            Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
                                        }
                                    }
                                }
                                xmlPullParserNewPullParser = xmlPullParser;
                                settings2 = settings;
                                c = c2;
                                z6 = z2;
                                depth = i3;
                                z = z3;
                                i8 = i4;
                                str2 = str;
                            }
                        }
                        settings = settings2;
                        fileInputStream2.close();
                        settings.mNextAppLinkGeneration.put(i, i10 + 1);
                        return;
                    } catch (XmlPullParserException e8) {
                        e = e8;
                        settings = settings2;
                        i2 = i9;
                    }
                } catch (IOException e9) {
                    e = e9;
                    settings = settings2;
                    settings.mReadMessages.append("Error reading: " + e.toString());
                    PackageManagerService.reportSettingsProblem(6, "Error reading settings: " + e);
                    Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
                    return;
                }
            } else {
                FileInputStream fileInputStream22 = fileInputStream;
                xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(fileInputStream22, StandardCharsets.UTF_8.name());
                do {
                    next = xmlPullParserNewPullParser.next();
                    c = 2;
                    z = true;
                    if (next != 2) {
                    }
                } while (next != 1);
                if (next == 2) {
                }
            }
        } catch (XmlPullParserException e10) {
            e = e10;
            settings = settings2;
        }
        settings.mReadMessages.append("Error reading: " + e.toString());
        PackageManagerService.reportSettingsProblem(i2, "Error reading stopped packages: " + e);
        Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
    }

    void setBlockUninstallLPw(int i, String str, boolean z) {
        ArraySet<String> arraySet = this.mBlockUninstallPackages.get(i);
        if (z) {
            if (arraySet == null) {
                arraySet = new ArraySet<>();
                this.mBlockUninstallPackages.put(i, arraySet);
            }
            arraySet.add(str);
            return;
        }
        if (arraySet != null) {
            arraySet.remove(str);
            if (arraySet.isEmpty()) {
                this.mBlockUninstallPackages.remove(i);
            }
        }
    }

    boolean getBlockUninstallLPr(int i, String str) {
        ArraySet<String> arraySet = this.mBlockUninstallPackages.get(i);
        if (arraySet == null) {
            return false;
        }
        return arraySet.contains(str);
    }

    private ArraySet<String> readComponentsLPr(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        String attributeValue;
        int depth = xmlPullParser.getDepth();
        ArraySet<String> arraySet = null;
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != 4 && xmlPullParser.getName().equals(TAG_ITEM) && (attributeValue = xmlPullParser.getAttributeValue(null, ATTR_NAME)) != null) {
                if (arraySet == null) {
                    arraySet = new ArraySet<>();
                }
                arraySet.add(attributeValue);
            }
        }
        return arraySet;
    }

    void writePreferredActivitiesLPr(XmlSerializer xmlSerializer, int i, boolean z) throws IllegalStateException, IOException, IllegalArgumentException {
        xmlSerializer.startTag(null, "preferred-activities");
        PreferredIntentResolver preferredIntentResolver = this.mPreferredActivities.get(i);
        if (preferredIntentResolver != null) {
            for (PreferredActivity preferredActivity : preferredIntentResolver.filterSet()) {
                xmlSerializer.startTag(null, TAG_ITEM);
                preferredActivity.writeToXml(xmlSerializer, z);
                xmlSerializer.endTag(null, TAG_ITEM);
            }
        }
        xmlSerializer.endTag(null, "preferred-activities");
    }

    void writePersistentPreferredActivitiesLPr(XmlSerializer xmlSerializer, int i) throws IllegalStateException, IOException, IllegalArgumentException {
        xmlSerializer.startTag(null, TAG_PERSISTENT_PREFERRED_ACTIVITIES);
        PersistentPreferredIntentResolver persistentPreferredIntentResolver = this.mPersistentPreferredActivities.get(i);
        if (persistentPreferredIntentResolver != null) {
            for (PersistentPreferredActivity persistentPreferredActivity : persistentPreferredIntentResolver.filterSet()) {
                xmlSerializer.startTag(null, TAG_ITEM);
                persistentPreferredActivity.writeToXml(xmlSerializer);
                xmlSerializer.endTag(null, TAG_ITEM);
            }
        }
        xmlSerializer.endTag(null, TAG_PERSISTENT_PREFERRED_ACTIVITIES);
    }

    void writeCrossProfileIntentFiltersLPr(XmlSerializer xmlSerializer, int i) throws IllegalStateException, IOException, IllegalArgumentException {
        xmlSerializer.startTag(null, TAG_CROSS_PROFILE_INTENT_FILTERS);
        CrossProfileIntentResolver crossProfileIntentResolver = this.mCrossProfileIntentResolvers.get(i);
        if (crossProfileIntentResolver != null) {
            for (CrossProfileIntentFilter crossProfileIntentFilter : crossProfileIntentResolver.filterSet()) {
                xmlSerializer.startTag(null, TAG_ITEM);
                crossProfileIntentFilter.writeToXml(xmlSerializer);
                xmlSerializer.endTag(null, TAG_ITEM);
            }
        }
        xmlSerializer.endTag(null, TAG_CROSS_PROFILE_INTENT_FILTERS);
    }

    void writeDomainVerificationsLPr(XmlSerializer xmlSerializer, IntentFilterVerificationInfo intentFilterVerificationInfo) throws IllegalStateException, IOException, IllegalArgumentException {
        if (intentFilterVerificationInfo != null && intentFilterVerificationInfo.getPackageName() != null) {
            xmlSerializer.startTag(null, TAG_DOMAIN_VERIFICATION);
            intentFilterVerificationInfo.writeToXml(xmlSerializer);
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                Slog.d(TAG, "Wrote domain verification for package: " + intentFilterVerificationInfo.getPackageName());
            }
            xmlSerializer.endTag(null, TAG_DOMAIN_VERIFICATION);
        }
    }

    void writeAllDomainVerificationsLPr(XmlSerializer xmlSerializer, int i) throws IllegalStateException, IOException, IllegalArgumentException {
        xmlSerializer.startTag(null, TAG_ALL_INTENT_FILTER_VERIFICATION);
        int size = this.mPackages.size();
        for (int i2 = 0; i2 < size; i2++) {
            IntentFilterVerificationInfo intentFilterVerificationInfo = this.mPackages.valueAt(i2).getIntentFilterVerificationInfo();
            if (intentFilterVerificationInfo != null) {
                writeDomainVerificationsLPr(xmlSerializer, intentFilterVerificationInfo);
            }
        }
        xmlSerializer.endTag(null, TAG_ALL_INTENT_FILTER_VERIFICATION);
    }

    void readAllDomainVerificationsLPr(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
        this.mRestoredIntentFilterVerifications.clear();
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4) {
                        if (xmlPullParser.getName().equals(TAG_DOMAIN_VERIFICATION)) {
                            IntentFilterVerificationInfo intentFilterVerificationInfo = new IntentFilterVerificationInfo(xmlPullParser);
                            String packageName = intentFilterVerificationInfo.getPackageName();
                            PackageSetting packageSetting = this.mPackages.get(packageName);
                            if (packageSetting != null) {
                                packageSetting.setIntentFilterVerificationInfo(intentFilterVerificationInfo);
                                if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                                    Slog.d(TAG, "Restored IVI for existing app " + packageName + " status=" + intentFilterVerificationInfo.getStatusString());
                                }
                            } else {
                                this.mRestoredIntentFilterVerifications.put(packageName, intentFilterVerificationInfo);
                                if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                                    Slog.d(TAG, "Restored IVI for pending app " + packageName + " status=" + intentFilterVerificationInfo.getStatusString());
                                }
                            }
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Unknown element under <all-intent-filter-verification>: " + xmlPullParser.getName());
                            XmlUtils.skipCurrentTag(xmlPullParser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    public void processRestoredPermissionGrantLPr(String str, String str2, boolean z, int i, int i2) {
        this.mRuntimePermissionsPersistence.rememberRestoredUserGrantLPr(str, str2, z, i, i2);
    }

    void writeDefaultAppsLPr(XmlSerializer xmlSerializer, int i) throws IllegalStateException, IOException, IllegalArgumentException {
        xmlSerializer.startTag(null, TAG_DEFAULT_APPS);
        String str = this.mDefaultBrowserApp.get(i);
        if (!TextUtils.isEmpty(str)) {
            xmlSerializer.startTag(null, TAG_DEFAULT_BROWSER);
            xmlSerializer.attribute(null, "packageName", str);
            xmlSerializer.endTag(null, TAG_DEFAULT_BROWSER);
        }
        String str2 = this.mDefaultDialerApp.get(i);
        if (!TextUtils.isEmpty(str2)) {
            xmlSerializer.startTag(null, TAG_DEFAULT_DIALER);
            xmlSerializer.attribute(null, "packageName", str2);
            xmlSerializer.endTag(null, TAG_DEFAULT_DIALER);
        }
        xmlSerializer.endTag(null, TAG_DEFAULT_APPS);
    }

    void writeBlockUninstallPackagesLPr(XmlSerializer xmlSerializer, int i) throws IOException {
        ArraySet<String> arraySet = this.mBlockUninstallPackages.get(i);
        if (arraySet != null) {
            xmlSerializer.startTag(null, TAG_BLOCK_UNINSTALL_PACKAGES);
            for (int i2 = 0; i2 < arraySet.size(); i2++) {
                xmlSerializer.startTag(null, TAG_BLOCK_UNINSTALL);
                xmlSerializer.attribute(null, "packageName", arraySet.valueAt(i2));
                xmlSerializer.endTag(null, TAG_BLOCK_UNINSTALL);
            }
            xmlSerializer.endTag(null, TAG_BLOCK_UNINSTALL_PACKAGES);
        }
    }

    void writePackageRestrictionsLPr(int i) {
        long jUptimeMillis = SystemClock.uptimeMillis();
        File userPackagesStateFile = getUserPackagesStateFile(i);
        File userPackagesStateBackupFile = getUserPackagesStateBackupFile(i);
        new File(userPackagesStateFile.getParent()).mkdirs();
        if (userPackagesStateFile.exists()) {
            if (!userPackagesStateBackupFile.exists()) {
                if (!userPackagesStateFile.renameTo(userPackagesStateBackupFile)) {
                    Slog.wtf("PackageManager", "Unable to backup user packages state file, current changes will be lost at reboot");
                    return;
                }
            } else {
                userPackagesStateFile.delete();
                Slog.w("PackageManager", "Preserving older stopped packages backup");
            }
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(userPackagesStateFile);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            XmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(bufferedOutputStream, StandardCharsets.UTF_8.name());
            String str = null;
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag(null, TAG_PACKAGE_RESTRICTIONS);
            for (PackageSetting packageSetting : this.mPackages.values()) {
                PackageUserState userState = packageSetting.readUserState(i);
                fastXmlSerializer.startTag(str, TAG_PACKAGE);
                fastXmlSerializer.attribute(str, ATTR_NAME, packageSetting.name);
                if (userState.ceDataInode != 0) {
                    XmlUtils.writeLongAttribute(fastXmlSerializer, ATTR_CE_DATA_INODE, userState.ceDataInode);
                }
                if (!userState.installed) {
                    fastXmlSerializer.attribute(null, ATTR_INSTALLED, "false");
                }
                if (userState.stopped) {
                    fastXmlSerializer.attribute(null, ATTR_STOPPED, "true");
                }
                if (userState.notLaunched) {
                    fastXmlSerializer.attribute(null, ATTR_NOT_LAUNCHED, "true");
                }
                if (userState.hidden) {
                    fastXmlSerializer.attribute(null, ATTR_HIDDEN, "true");
                }
                if (userState.suspended) {
                    fastXmlSerializer.attribute(null, ATTR_SUSPENDED, "true");
                    if (userState.suspendingPackage != null) {
                        fastXmlSerializer.attribute(null, ATTR_SUSPENDING_PACKAGE, userState.suspendingPackage);
                    }
                    if (userState.dialogMessage != null) {
                        fastXmlSerializer.attribute(null, ATTR_SUSPEND_DIALOG_MESSAGE, userState.dialogMessage);
                    }
                    if (userState.suspendedAppExtras != null) {
                        fastXmlSerializer.startTag(null, TAG_SUSPENDED_APP_EXTRAS);
                        try {
                            userState.suspendedAppExtras.saveToXml(fastXmlSerializer);
                        } catch (XmlPullParserException e) {
                            Slog.wtf(TAG, "Exception while trying to write suspendedAppExtras for " + packageSetting + ". Will be lost on reboot", e);
                        }
                        fastXmlSerializer.endTag(null, TAG_SUSPENDED_APP_EXTRAS);
                    }
                    if (userState.suspendedLauncherExtras != null) {
                        fastXmlSerializer.startTag(null, TAG_SUSPENDED_LAUNCHER_EXTRAS);
                        try {
                            userState.suspendedLauncherExtras.saveToXml(fastXmlSerializer);
                        } catch (XmlPullParserException e2) {
                            Slog.wtf(TAG, "Exception while trying to write suspendedLauncherExtras for " + packageSetting + ". Will be lost on reboot", e2);
                        }
                        fastXmlSerializer.endTag(null, TAG_SUSPENDED_LAUNCHER_EXTRAS);
                    }
                }
                if (userState.instantApp) {
                    fastXmlSerializer.attribute(null, ATTR_INSTANT_APP, "true");
                }
                if (userState.virtualPreload) {
                    fastXmlSerializer.attribute(null, ATTR_VIRTUAL_PRELOAD, "true");
                }
                if (userState.enabled != 0) {
                    fastXmlSerializer.attribute(null, ATTR_ENABLED, Integer.toString(userState.enabled));
                    if (userState.lastDisableAppCaller != null) {
                        fastXmlSerializer.attribute(null, ATTR_ENABLED_CALLER, userState.lastDisableAppCaller);
                    }
                }
                if (userState.domainVerificationStatus != 0) {
                    XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_DOMAIN_VERIFICATON_STATE, userState.domainVerificationStatus);
                }
                if (userState.appLinkGeneration != 0) {
                    XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_APP_LINK_GENERATION, userState.appLinkGeneration);
                }
                if (userState.installReason != 0) {
                    fastXmlSerializer.attribute(null, ATTR_INSTALL_REASON, Integer.toString(userState.installReason));
                }
                if (userState.harmfulAppWarning != null) {
                    fastXmlSerializer.attribute(null, ATTR_HARMFUL_APP_WARNING, userState.harmfulAppWarning);
                }
                if (!ArrayUtils.isEmpty(userState.enabledComponents)) {
                    fastXmlSerializer.startTag(null, TAG_ENABLED_COMPONENTS);
                    for (String str2 : userState.enabledComponents) {
                        fastXmlSerializer.startTag(null, TAG_ITEM);
                        fastXmlSerializer.attribute(null, ATTR_NAME, str2);
                        fastXmlSerializer.endTag(null, TAG_ITEM);
                    }
                    fastXmlSerializer.endTag(null, TAG_ENABLED_COMPONENTS);
                }
                if (!ArrayUtils.isEmpty(userState.disabledComponents)) {
                    fastXmlSerializer.startTag(null, TAG_DISABLED_COMPONENTS);
                    for (String str3 : userState.disabledComponents) {
                        fastXmlSerializer.startTag(null, TAG_ITEM);
                        fastXmlSerializer.attribute(null, ATTR_NAME, str3);
                        fastXmlSerializer.endTag(null, TAG_ITEM);
                    }
                    fastXmlSerializer.endTag(null, TAG_DISABLED_COMPONENTS);
                }
                fastXmlSerializer.endTag(null, TAG_PACKAGE);
                str = null;
            }
            writePreferredActivitiesLPr(fastXmlSerializer, i, true);
            writePersistentPreferredActivitiesLPr(fastXmlSerializer, i);
            writeCrossProfileIntentFiltersLPr(fastXmlSerializer, i);
            writeDefaultAppsLPr(fastXmlSerializer, i);
            writeBlockUninstallPackagesLPr(fastXmlSerializer, i);
            fastXmlSerializer.endTag(null, TAG_PACKAGE_RESTRICTIONS);
            fastXmlSerializer.endDocument();
            bufferedOutputStream.flush();
            FileUtils.sync(fileOutputStream);
            bufferedOutputStream.close();
            userPackagesStateBackupFile.delete();
            FileUtils.setPermissions(userPackagesStateFile.toString(), 432, -1, -1);
            EventLogTags.writeCommitSysConfigFile("package-user-" + i, SystemClock.uptimeMillis() - jUptimeMillis);
        } catch (IOException e3) {
            Slog.wtf("PackageManager", "Unable to write package manager user packages state,  current changes will be lost at reboot", e3);
            if (userPackagesStateFile.exists() && !userPackagesStateFile.delete()) {
                Log.i("PackageManager", "Failed to clean up mangled file: " + this.mStoppedPackagesFilename);
            }
        }
    }

    void readInstallPermissionsLPr(XmlPullParser xmlPullParser, PermissionsState permissionsState) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            boolean z = true;
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4) {
                        if (xmlPullParser.getName().equals(TAG_ITEM)) {
                            String attributeValue = xmlPullParser.getAttributeValue(null, ATTR_NAME);
                            BasePermission permission = this.mPermissions.getPermission(attributeValue);
                            if (permission == null) {
                                Slog.w("PackageManager", "Unknown permission: " + attributeValue);
                                XmlUtils.skipCurrentTag(xmlPullParser);
                            } else {
                                String attributeValue2 = xmlPullParser.getAttributeValue(null, ATTR_GRANTED);
                                int i = 0;
                                if (attributeValue2 != null && !Boolean.parseBoolean(attributeValue2)) {
                                    z = false;
                                }
                                String attributeValue3 = xmlPullParser.getAttributeValue(null, ATTR_FLAGS);
                                if (attributeValue3 != null) {
                                    i = Integer.parseInt(attributeValue3, 16);
                                }
                                if (z) {
                                    if (permissionsState.grantInstallPermission(permission) == -1) {
                                        Slog.w("PackageManager", "Permission already added: " + attributeValue);
                                        XmlUtils.skipCurrentTag(xmlPullParser);
                                    } else {
                                        permissionsState.updatePermissionFlags(permission, -1, 255, i);
                                    }
                                } else if (permissionsState.revokeInstallPermission(permission) == -1) {
                                    Slog.w("PackageManager", "Permission already added: " + attributeValue);
                                    XmlUtils.skipCurrentTag(xmlPullParser);
                                } else {
                                    permissionsState.updatePermissionFlags(permission, -1, 255, i);
                                }
                            }
                        } else {
                            Slog.w("PackageManager", "Unknown element under <permissions>: " + xmlPullParser.getName());
                            XmlUtils.skipCurrentTag(xmlPullParser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    void writePermissionsLPr(XmlSerializer xmlSerializer, List<PermissionsState.PermissionState> list) throws IOException {
        if (list.isEmpty()) {
            return;
        }
        xmlSerializer.startTag(null, TAG_PERMISSIONS);
        for (PermissionsState.PermissionState permissionState : list) {
            xmlSerializer.startTag(null, TAG_ITEM);
            xmlSerializer.attribute(null, ATTR_NAME, permissionState.getName());
            xmlSerializer.attribute(null, ATTR_GRANTED, String.valueOf(permissionState.isGranted()));
            xmlSerializer.attribute(null, ATTR_FLAGS, Integer.toHexString(permissionState.getFlags()));
            xmlSerializer.endTag(null, TAG_ITEM);
        }
        xmlSerializer.endTag(null, TAG_PERMISSIONS);
    }

    void writeChildPackagesLPw(XmlSerializer xmlSerializer, List<String> list) throws IOException {
        if (list == null) {
            return;
        }
        int size = list.size();
        for (int i = 0; i < size; i++) {
            String str = list.get(i);
            xmlSerializer.startTag(null, TAG_CHILD_PACKAGE);
            xmlSerializer.attribute(null, ATTR_NAME, str);
            xmlSerializer.endTag(null, TAG_CHILD_PACKAGE);
        }
    }

    void readUsesStaticLibLPw(XmlPullParser xmlPullParser, PackageSetting packageSetting) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4) {
                        String attributeValue = xmlPullParser.getAttributeValue(null, ATTR_NAME);
                        long j = -1;
                        try {
                            j = Long.parseLong(xmlPullParser.getAttributeValue(null, "version"));
                        } catch (NumberFormatException e) {
                        }
                        if (attributeValue != null && j >= 0) {
                            packageSetting.usesStaticLibraries = (String[]) ArrayUtils.appendElement(String.class, packageSetting.usesStaticLibraries, attributeValue);
                            packageSetting.usesStaticLibrariesVersions = ArrayUtils.appendLong(packageSetting.usesStaticLibrariesVersions, j);
                        }
                        XmlUtils.skipCurrentTag(xmlPullParser);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    void writeUsesStaticLibLPw(XmlSerializer xmlSerializer, String[] strArr, long[] jArr) throws IOException {
        if (ArrayUtils.isEmpty(strArr) || ArrayUtils.isEmpty(jArr) || strArr.length != jArr.length) {
            return;
        }
        int length = strArr.length;
        for (int i = 0; i < length; i++) {
            String str = strArr[i];
            long j = jArr[i];
            xmlSerializer.startTag(null, TAG_USES_STATIC_LIB);
            xmlSerializer.attribute(null, ATTR_NAME, str);
            xmlSerializer.attribute(null, "version", Long.toString(j));
            xmlSerializer.endTag(null, TAG_USES_STATIC_LIB);
        }
    }

    void readStoppedLPw() {
        FileInputStream fileInputStream;
        int next;
        if (this.mBackupStoppedPackagesFilename.exists()) {
            try {
                fileInputStream = new FileInputStream(this.mBackupStoppedPackagesFilename);
                try {
                    this.mReadMessages.append("Reading from backup stopped packages file\n");
                    PackageManagerService.reportSettingsProblem(4, "Need to read from backup stopped packages file");
                    if (this.mSettingsFilename.exists()) {
                        Slog.w("PackageManager", "Cleaning up stopped packages file " + this.mStoppedPackagesFilename);
                        this.mStoppedPackagesFilename.delete();
                    }
                } catch (IOException e) {
                }
            } catch (IOException e2) {
                fileInputStream = null;
            }
        } else {
            fileInputStream = null;
        }
        if (fileInputStream == null) {
            try {
                if (!this.mStoppedPackagesFilename.exists()) {
                    this.mReadMessages.append("No stopped packages file found\n");
                    PackageManagerService.reportSettingsProblem(4, "No stopped packages file file; assuming all started");
                    for (PackageSetting packageSetting : this.mPackages.values()) {
                        packageSetting.setStopped(false, 0);
                        packageSetting.setNotLaunched(false, 0);
                    }
                    return;
                }
                fileInputStream = new FileInputStream(this.mStoppedPackagesFilename);
            } catch (IOException e3) {
                this.mReadMessages.append("Error reading: " + e3.toString());
                PackageManagerService.reportSettingsProblem(6, "Error reading settings: " + e3);
                Slog.wtf("PackageManager", "Error reading package manager stopped packages", e3);
                return;
            } catch (XmlPullParserException e4) {
                this.mReadMessages.append("Error reading: " + e4.toString());
                PackageManagerService.reportSettingsProblem(6, "Error reading stopped packages: " + e4);
                Slog.wtf("PackageManager", "Error reading package manager stopped packages", e4);
                return;
            }
        }
        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
        xmlPullParserNewPullParser.setInput(fileInputStream, null);
        do {
            next = xmlPullParserNewPullParser.next();
            if (next == 2) {
                break;
            }
        } while (next != 1);
        if (next != 2) {
            this.mReadMessages.append("No start tag found in stopped packages file\n");
            PackageManagerService.reportSettingsProblem(5, "No start tag found in package manager stopped packages");
            return;
        }
        int depth = xmlPullParserNewPullParser.getDepth();
        while (true) {
            int next2 = xmlPullParserNewPullParser.next();
            if (next2 == 1 || (next2 == 3 && xmlPullParserNewPullParser.getDepth() <= depth)) {
                break;
            }
            if (next2 != 3 && next2 != 4) {
                if (xmlPullParserNewPullParser.getName().equals(TAG_PACKAGE)) {
                    String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_NAME);
                    PackageSetting packageSetting2 = this.mPackages.get(attributeValue);
                    if (packageSetting2 != null) {
                        packageSetting2.setStopped(true, 0);
                        if ("1".equals(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_NOT_LAUNCHED))) {
                            packageSetting2.setNotLaunched(true, 0);
                        }
                    } else {
                        Slog.w("PackageManager", "No package known for stopped package " + attributeValue);
                    }
                    XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                } else {
                    Slog.w("PackageManager", "Unknown element under <stopped-packages>: " + xmlPullParserNewPullParser.getName());
                    XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                }
            }
        }
        fileInputStream.close();
    }

    void writeLPr() {
        long jUptimeMillis = SystemClock.uptimeMillis();
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
            FileOutputStream fileOutputStream = new FileOutputStream(this.mSettingsFilename);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            XmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(bufferedOutputStream, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag(null, "packages");
            for (int i = 0; i < this.mVersion.size(); i++) {
                String strKeyAt = this.mVersion.keyAt(i);
                VersionInfo versionInfoValueAt = this.mVersion.valueAt(i);
                fastXmlSerializer.startTag(null, "version");
                XmlUtils.writeStringAttribute(fastXmlSerializer, ATTR_VOLUME_UUID, strKeyAt);
                XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_SDK_VERSION, versionInfoValueAt.sdkVersion);
                XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_DATABASE_VERSION, versionInfoValueAt.databaseVersion);
                XmlUtils.writeStringAttribute(fastXmlSerializer, ATTR_FINGERPRINT, versionInfoValueAt.fingerprint);
                fastXmlSerializer.endTag(null, "version");
            }
            if (this.mVerifierDeviceIdentity != null) {
                fastXmlSerializer.startTag(null, "verifier");
                fastXmlSerializer.attribute(null, "device", this.mVerifierDeviceIdentity.toString());
                fastXmlSerializer.endTag(null, "verifier");
            }
            if (this.mReadExternalStorageEnforced != null) {
                fastXmlSerializer.startTag(null, TAG_READ_EXTERNAL_STORAGE);
                fastXmlSerializer.attribute(null, ATTR_ENFORCEMENT, this.mReadExternalStorageEnforced.booleanValue() ? "1" : "0");
                fastXmlSerializer.endTag(null, TAG_READ_EXTERNAL_STORAGE);
            }
            fastXmlSerializer.startTag(null, "permission-trees");
            this.mPermissions.writePermissionTrees(fastXmlSerializer);
            fastXmlSerializer.endTag(null, "permission-trees");
            fastXmlSerializer.startTag(null, "permissions");
            this.mPermissions.writePermissions(fastXmlSerializer);
            fastXmlSerializer.endTag(null, "permissions");
            Iterator<PackageSetting> it = this.mPackages.values().iterator();
            while (it.hasNext()) {
                writePackageLPr(fastXmlSerializer, it.next());
            }
            Iterator<PackageSetting> it2 = this.mDisabledSysPackages.values().iterator();
            while (it2.hasNext()) {
                writeDisabledSysPackageLPr(fastXmlSerializer, it2.next());
            }
            for (SharedUserSetting sharedUserSetting : this.mSharedUsers.values()) {
                fastXmlSerializer.startTag(null, TAG_SHARED_USER);
                fastXmlSerializer.attribute(null, ATTR_NAME, sharedUserSetting.name);
                fastXmlSerializer.attribute(null, "userId", Integer.toString(sharedUserSetting.userId));
                sharedUserSetting.signatures.writeXml(fastXmlSerializer, "sigs", this.mPastSignatures);
                writePermissionsLPr(fastXmlSerializer, sharedUserSetting.getPermissionsState().getInstallPermissionStates());
                fastXmlSerializer.endTag(null, TAG_SHARED_USER);
            }
            if (this.mPackagesToBeCleaned.size() > 0) {
                for (PackageCleanItem packageCleanItem : this.mPackagesToBeCleaned) {
                    String string = Integer.toString(packageCleanItem.userId);
                    fastXmlSerializer.startTag(null, "cleaning-package");
                    fastXmlSerializer.attribute(null, ATTR_NAME, packageCleanItem.packageName);
                    fastXmlSerializer.attribute(null, ATTR_CODE, packageCleanItem.andCode ? "true" : "false");
                    fastXmlSerializer.attribute(null, ATTR_USER, string);
                    fastXmlSerializer.endTag(null, "cleaning-package");
                }
            }
            if (this.mRenamedPackages.size() > 0) {
                for (Map.Entry<String, String> entry : this.mRenamedPackages.entrySet()) {
                    fastXmlSerializer.startTag(null, "renamed-package");
                    fastXmlSerializer.attribute(null, "new", entry.getKey());
                    fastXmlSerializer.attribute(null, "old", entry.getValue());
                    fastXmlSerializer.endTag(null, "renamed-package");
                }
            }
            int size = this.mRestoredIntentFilterVerifications.size();
            if (size > 0) {
                if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                    Slog.i(TAG, "Writing restored-ivi entries to packages.xml");
                }
                fastXmlSerializer.startTag(null, "restored-ivi");
                for (int i2 = 0; i2 < size; i2++) {
                    writeDomainVerificationsLPr(fastXmlSerializer, this.mRestoredIntentFilterVerifications.valueAt(i2));
                }
                fastXmlSerializer.endTag(null, "restored-ivi");
            } else if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                Slog.i(TAG, "  no restored IVI entries to write");
            }
            this.mKeySetManagerService.writeKeySetManagerServiceLPr(fastXmlSerializer);
            fastXmlSerializer.endTag(null, "packages");
            fastXmlSerializer.endDocument();
            bufferedOutputStream.flush();
            FileUtils.sync(fileOutputStream);
            bufferedOutputStream.close();
            this.mBackupSettingsFilename.delete();
            FileUtils.setPermissions(this.mSettingsFilename.toString(), 432, -1, -1);
            writeKernelMappingLPr();
            writePackageListLPr();
            writeAllUsersPackageRestrictionsLPr();
            writeAllRuntimePermissionsLPr();
            EventLogTags.writeCommitSysConfigFile(ATTR_PACKAGE, SystemClock.uptimeMillis() - jUptimeMillis);
        } catch (IOException e) {
            Slog.wtf("PackageManager", "Unable to write package manager settings, current changes will be lost at reboot", e);
            if (this.mSettingsFilename.exists() && !this.mSettingsFilename.delete()) {
                Slog.wtf("PackageManager", "Failed to clean up mangled file: " + this.mSettingsFilename);
            }
        }
    }

    private void writeKernelRemoveUserLPr(int i) {
        if (this.mKernelMappingFilename == null) {
            return;
        }
        writeIntToFile(new File(this.mKernelMappingFilename, "remove_userid"), i);
    }

    void writeKernelMappingLPr() {
        if (this.mKernelMappingFilename == null) {
            return;
        }
        String[] list = this.mKernelMappingFilename.list();
        ArraySet arraySet = new ArraySet(list.length);
        for (String str : list) {
            arraySet.add(str);
        }
        for (PackageSetting packageSetting : this.mPackages.values()) {
            arraySet.remove(packageSetting.name);
            writeKernelMappingLPr(packageSetting);
        }
        for (int i = 0; i < arraySet.size(); i++) {
            String str2 = (String) arraySet.valueAt(i);
            this.mKernelMapping.remove(str2);
            new File(this.mKernelMappingFilename, str2).delete();
        }
    }

    void writeKernelMappingLPr(PackageSetting packageSetting) {
        if (this.mKernelMappingFilename == null || packageSetting == null || packageSetting.name == null) {
            return;
        }
        KernelPackageState kernelPackageState = this.mKernelMapping.get(packageSetting.name);
        boolean z = true;
        boolean z2 = kernelPackageState == null;
        int[] notInstalledUserIds = packageSetting.getNotInstalledUserIds();
        if (!z2 && Arrays.equals(notInstalledUserIds, kernelPackageState.excludedUserIds)) {
            z = false;
        }
        File file = new File(this.mKernelMappingFilename, packageSetting.name);
        if (z2) {
            file.mkdir();
            kernelPackageState = new KernelPackageState();
            this.mKernelMapping.put(packageSetting.name, kernelPackageState);
        }
        if (kernelPackageState.appId != packageSetting.appId) {
            writeIntToFile(new File(file, "appid"), packageSetting.appId);
        }
        if (z) {
            for (int i = 0; i < notInstalledUserIds.length; i++) {
                if (kernelPackageState.excludedUserIds == null || !ArrayUtils.contains(kernelPackageState.excludedUserIds, notInstalledUserIds[i])) {
                    writeIntToFile(new File(file, "excluded_userids"), notInstalledUserIds[i]);
                }
            }
            if (kernelPackageState.excludedUserIds != null) {
                for (int i2 = 0; i2 < kernelPackageState.excludedUserIds.length; i2++) {
                    if (!ArrayUtils.contains(notInstalledUserIds, kernelPackageState.excludedUserIds[i2])) {
                        writeIntToFile(new File(file, "clear_userid"), kernelPackageState.excludedUserIds[i2]);
                    }
                }
            }
            kernelPackageState.excludedUserIds = notInstalledUserIds;
        }
    }

    private void writeIntToFile(File file, int i) {
        try {
            FileUtils.bytesToFile(file.getAbsolutePath(), Integer.toString(i).getBytes(StandardCharsets.US_ASCII));
        } catch (IOException e) {
            Slog.w(TAG, "Couldn't write " + i + " to " + file.getAbsolutePath());
        }
    }

    void writePackageListLPr() {
        writePackageListLPr(-1);
    }

    void writePackageListLPr(int i) {
        BufferedWriter bufferedWriter;
        FileOutputStream fileOutputStream;
        List<UserInfo> users = UserManagerService.getInstance().getUsers(true);
        int[] iArrAppendInt = new int[users.size()];
        for (int i2 = 0; i2 < iArrAppendInt.length; i2++) {
            iArrAppendInt[i2] = users.get(i2).id;
        }
        if (i != -1) {
            iArrAppendInt = ArrayUtils.appendInt(iArrAppendInt, i);
        }
        JournaledFile journaledFile = new JournaledFile(this.mPackageListFilename, new File(this.mPackageListFilename.getAbsolutePath() + ".tmp"));
        try {
            fileOutputStream = new FileOutputStream(journaledFile.chooseForWrite());
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream, Charset.defaultCharset()));
        } catch (Exception e) {
            e = e;
            bufferedWriter = null;
        }
        try {
            FileUtils.setPermissions(fileOutputStream.getFD(), 416, 1000, 1032);
            StringBuilder sb = new StringBuilder();
            for (PackageSetting packageSetting : this.mPackages.values()) {
                if (packageSetting.pkg == null || packageSetting.pkg.applicationInfo == null || packageSetting.pkg.applicationInfo.dataDir == null) {
                    if (!PackageManagerService.PLATFORM_PACKAGE_NAME.equals(packageSetting.name)) {
                        Slog.w(TAG, "Skipping " + packageSetting + " due to missing metadata");
                    }
                } else {
                    ApplicationInfo applicationInfo = packageSetting.pkg.applicationInfo;
                    String str = applicationInfo.dataDir;
                    boolean z = (applicationInfo.flags & 2) != 0;
                    int[] iArrComputeGids = packageSetting.getPermissionsState().computeGids(iArrAppendInt);
                    if (str.indexOf(32) < 0) {
                        sb.setLength(0);
                        sb.append(applicationInfo.packageName);
                        sb.append(" ");
                        sb.append(applicationInfo.uid);
                        sb.append(z ? " 1 " : " 0 ");
                        sb.append(str);
                        sb.append(" ");
                        sb.append(applicationInfo.seInfo);
                        sb.append(" ");
                        if (iArrComputeGids != null && iArrComputeGids.length > 0) {
                            sb.append(iArrComputeGids[0]);
                            for (int i3 = 1; i3 < iArrComputeGids.length; i3++) {
                                sb.append(",");
                                sb.append(iArrComputeGids[i3]);
                            }
                        } else {
                            sb.append("none");
                        }
                        sb.append("\n");
                        bufferedWriter.append((CharSequence) sb);
                    }
                }
            }
            bufferedWriter.flush();
            FileUtils.sync(fileOutputStream);
            bufferedWriter.close();
            journaledFile.commit();
        } catch (Exception e2) {
            e = e2;
            Slog.wtf(TAG, "Failed to write packages.list", e);
            IoUtils.closeQuietly(bufferedWriter);
            journaledFile.rollback();
        }
    }

    void writeDisabledSysPackageLPr(XmlSerializer xmlSerializer, PackageSetting packageSetting) throws IOException {
        xmlSerializer.startTag(null, "updated-package");
        xmlSerializer.attribute(null, ATTR_NAME, packageSetting.name);
        if (packageSetting.realName != null) {
            xmlSerializer.attribute(null, "realName", packageSetting.realName);
        }
        xmlSerializer.attribute(null, "codePath", packageSetting.codePathString);
        xmlSerializer.attribute(null, "ft", Long.toHexString(packageSetting.timeStamp));
        xmlSerializer.attribute(null, "it", Long.toHexString(packageSetting.firstInstallTime));
        xmlSerializer.attribute(null, "ut", Long.toHexString(packageSetting.lastUpdateTime));
        xmlSerializer.attribute(null, "version", String.valueOf(packageSetting.versionCode));
        if (!packageSetting.resourcePathString.equals(packageSetting.codePathString)) {
            xmlSerializer.attribute(null, "resourcePath", packageSetting.resourcePathString);
        }
        if (packageSetting.legacyNativeLibraryPathString != null) {
            xmlSerializer.attribute(null, "nativeLibraryPath", packageSetting.legacyNativeLibraryPathString);
        }
        if (packageSetting.primaryCpuAbiString != null) {
            xmlSerializer.attribute(null, "primaryCpuAbi", packageSetting.primaryCpuAbiString);
        }
        if (packageSetting.secondaryCpuAbiString != null) {
            xmlSerializer.attribute(null, "secondaryCpuAbi", packageSetting.secondaryCpuAbiString);
        }
        if (packageSetting.cpuAbiOverrideString != null) {
            xmlSerializer.attribute(null, "cpuAbiOverride", packageSetting.cpuAbiOverrideString);
        }
        if (packageSetting.sharedUser == null) {
            xmlSerializer.attribute(null, "userId", Integer.toString(packageSetting.appId));
        } else {
            xmlSerializer.attribute(null, "sharedUserId", Integer.toString(packageSetting.appId));
        }
        if (packageSetting.parentPackageName != null) {
            xmlSerializer.attribute(null, "parentPackageName", packageSetting.parentPackageName);
        }
        writeChildPackagesLPw(xmlSerializer, packageSetting.childPackageNames);
        writeUsesStaticLibLPw(xmlSerializer, packageSetting.usesStaticLibraries, packageSetting.usesStaticLibrariesVersions);
        if (packageSetting.sharedUser == null) {
            writePermissionsLPr(xmlSerializer, packageSetting.getPermissionsState().getInstallPermissionStates());
        }
        xmlSerializer.endTag(null, "updated-package");
    }

    void writePackageLPr(XmlSerializer xmlSerializer, PackageSetting packageSetting) throws IOException {
        xmlSerializer.startTag(null, ATTR_PACKAGE);
        xmlSerializer.attribute(null, ATTR_NAME, packageSetting.name);
        if (packageSetting.realName != null) {
            xmlSerializer.attribute(null, "realName", packageSetting.realName);
        }
        xmlSerializer.attribute(null, "codePath", packageSetting.codePathString);
        if (!packageSetting.resourcePathString.equals(packageSetting.codePathString)) {
            xmlSerializer.attribute(null, "resourcePath", packageSetting.resourcePathString);
        }
        if (packageSetting.legacyNativeLibraryPathString != null) {
            xmlSerializer.attribute(null, "nativeLibraryPath", packageSetting.legacyNativeLibraryPathString);
        }
        if (packageSetting.primaryCpuAbiString != null) {
            xmlSerializer.attribute(null, "primaryCpuAbi", packageSetting.primaryCpuAbiString);
        }
        if (packageSetting.secondaryCpuAbiString != null) {
            xmlSerializer.attribute(null, "secondaryCpuAbi", packageSetting.secondaryCpuAbiString);
        }
        if (packageSetting.cpuAbiOverrideString != null) {
            xmlSerializer.attribute(null, "cpuAbiOverride", packageSetting.cpuAbiOverrideString);
        }
        xmlSerializer.attribute(null, "publicFlags", Integer.toString(packageSetting.pkgFlags));
        xmlSerializer.attribute(null, "privateFlags", Integer.toString(packageSetting.pkgPrivateFlags));
        xmlSerializer.attribute(null, "ft", Long.toHexString(packageSetting.timeStamp));
        xmlSerializer.attribute(null, "it", Long.toHexString(packageSetting.firstInstallTime));
        xmlSerializer.attribute(null, "ut", Long.toHexString(packageSetting.lastUpdateTime));
        xmlSerializer.attribute(null, "version", String.valueOf(packageSetting.versionCode));
        if (packageSetting.sharedUser == null) {
            xmlSerializer.attribute(null, "userId", Integer.toString(packageSetting.appId));
        } else {
            xmlSerializer.attribute(null, "sharedUserId", Integer.toString(packageSetting.appId));
        }
        if (packageSetting.uidError) {
            xmlSerializer.attribute(null, "uidError", "true");
        }
        if (packageSetting.installerPackageName != null) {
            xmlSerializer.attribute(null, "installer", packageSetting.installerPackageName);
        }
        if (packageSetting.isOrphaned) {
            xmlSerializer.attribute(null, "isOrphaned", "true");
        }
        if (packageSetting.volumeUuid != null) {
            xmlSerializer.attribute(null, ATTR_VOLUME_UUID, packageSetting.volumeUuid);
        }
        if (packageSetting.categoryHint != -1) {
            xmlSerializer.attribute(null, "categoryHint", Integer.toString(packageSetting.categoryHint));
        }
        if (packageSetting.parentPackageName != null) {
            xmlSerializer.attribute(null, "parentPackageName", packageSetting.parentPackageName);
        }
        if (packageSetting.updateAvailable) {
            xmlSerializer.attribute(null, "updateAvailable", "true");
        }
        writeChildPackagesLPw(xmlSerializer, packageSetting.childPackageNames);
        writeUsesStaticLibLPw(xmlSerializer, packageSetting.usesStaticLibraries, packageSetting.usesStaticLibrariesVersions);
        packageSetting.signatures.writeXml(xmlSerializer, "sigs", this.mPastSignatures);
        writePermissionsLPr(xmlSerializer, packageSetting.getPermissionsState().getInstallPermissionStates());
        writeSigningKeySetLPr(xmlSerializer, packageSetting.keySetData);
        writeUpgradeKeySetsLPr(xmlSerializer, packageSetting.keySetData);
        writeKeySetAliasesLPr(xmlSerializer, packageSetting.keySetData);
        writeDomainVerificationsLPr(xmlSerializer, packageSetting.verificationInfo);
        xmlSerializer.endTag(null, ATTR_PACKAGE);
    }

    void writeSigningKeySetLPr(XmlSerializer xmlSerializer, PackageKeySetData packageKeySetData) throws IOException {
        xmlSerializer.startTag(null, "proper-signing-keyset");
        xmlSerializer.attribute(null, "identifier", Long.toString(packageKeySetData.getProperSigningKeySet()));
        xmlSerializer.endTag(null, "proper-signing-keyset");
    }

    void writeUpgradeKeySetsLPr(XmlSerializer xmlSerializer, PackageKeySetData packageKeySetData) throws IOException {
        if (packageKeySetData.isUsingUpgradeKeySets()) {
            for (long j : packageKeySetData.getUpgradeKeySets()) {
                xmlSerializer.startTag(null, "upgrade-keyset");
                xmlSerializer.attribute(null, "identifier", Long.toString(j));
                xmlSerializer.endTag(null, "upgrade-keyset");
            }
        }
    }

    void writeKeySetAliasesLPr(XmlSerializer xmlSerializer, PackageKeySetData packageKeySetData) throws IOException {
        for (Map.Entry<String, Long> entry : packageKeySetData.getAliases().entrySet()) {
            xmlSerializer.startTag(null, "defined-keyset");
            xmlSerializer.attribute(null, "alias", entry.getKey());
            xmlSerializer.attribute(null, "identifier", Long.toString(entry.getValue().longValue()));
            xmlSerializer.endTag(null, "defined-keyset");
        }
    }

    void writePermissionLPr(XmlSerializer xmlSerializer, BasePermission basePermission) throws IOException {
        basePermission.writeLPr(xmlSerializer);
    }

    void addPackageToCleanLPw(PackageCleanItem packageCleanItem) {
        if (!this.mPackagesToBeCleaned.contains(packageCleanItem)) {
            this.mPackagesToBeCleaned.add(packageCleanItem);
        }
    }

    boolean readLPw(List<UserInfo> list) {
        FileInputStream fileInputStream;
        int next;
        int i;
        boolean z;
        if (this.mBackupSettingsFilename.exists()) {
            try {
                fileInputStream = new FileInputStream(this.mBackupSettingsFilename);
                try {
                    this.mReadMessages.append("Reading from backup settings file\n");
                    PackageManagerService.reportSettingsProblem(4, "Need to read from backup settings file");
                    if (this.mSettingsFilename.exists()) {
                        Slog.w("PackageManager", "Cleaning up settings file " + this.mSettingsFilename);
                        this.mSettingsFilename.delete();
                    }
                } catch (IOException e) {
                }
            } catch (IOException e2) {
                fileInputStream = null;
            }
        } else {
            fileInputStream = null;
        }
        this.mPendingPackages.clear();
        this.mPastSignatures.clear();
        this.mKeySetRefs.clear();
        this.mInstallerPackages.clear();
        if (fileInputStream == null) {
            try {
                if (!this.mSettingsFilename.exists()) {
                    this.mReadMessages.append("No settings file found\n");
                    PackageManagerService.reportSettingsProblem(4, "No settings file; creating initial state");
                    findOrCreateVersion(StorageManager.UUID_PRIVATE_INTERNAL).forceCurrent();
                    findOrCreateVersion("primary_physical").forceCurrent();
                    return false;
                }
                fileInputStream = new FileInputStream(this.mSettingsFilename);
            } catch (IOException e3) {
                this.mReadMessages.append("Error reading: " + e3.toString());
                PackageManagerService.reportSettingsProblem(6, "Error reading settings: " + e3);
                Slog.wtf("PackageManager", "Error reading package manager settings", e3);
            } catch (XmlPullParserException e4) {
                this.mReadMessages.append("Error reading: " + e4.toString());
                PackageManagerService.reportSettingsProblem(6, "Error reading settings: " + e4);
                Slog.wtf("PackageManager", "Error reading package manager settings", e4);
            }
        }
        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
        xmlPullParserNewPullParser.setInput(fileInputStream, StandardCharsets.UTF_8.name());
        do {
            next = xmlPullParserNewPullParser.next();
            if (next == 2) {
                break;
            }
        } while (next != 1);
        if (next != 2) {
            this.mReadMessages.append("No start tag found in settings file\n");
            PackageManagerService.reportSettingsProblem(5, "No start tag found in package manager settings");
            Slog.wtf("PackageManager", "No start tag found in package manager settings");
            return false;
        }
        int depth = xmlPullParserNewPullParser.getDepth();
        while (true) {
            int next2 = xmlPullParserNewPullParser.next();
            if (next2 == 1 || (next2 == 3 && xmlPullParserNewPullParser.getDepth() <= depth)) {
                break;
            }
            if (next2 != 3 && next2 != 4) {
                String name = xmlPullParserNewPullParser.getName();
                if (name.equals(ATTR_PACKAGE)) {
                    readPackageLPw(xmlPullParserNewPullParser);
                } else if (name.equals("permissions")) {
                    this.mPermissions.readPermissions(xmlPullParserNewPullParser);
                } else if (name.equals("permission-trees")) {
                    this.mPermissions.readPermissionTrees(xmlPullParserNewPullParser);
                } else if (name.equals(TAG_SHARED_USER)) {
                    readSharedUserLPw(xmlPullParserNewPullParser);
                } else if (!name.equals("preferred-packages")) {
                    if (name.equals("preferred-activities")) {
                        readPreferredActivitiesLPw(xmlPullParserNewPullParser, 0);
                    } else if (name.equals(TAG_PERSISTENT_PREFERRED_ACTIVITIES)) {
                        readPersistentPreferredActivitiesLPw(xmlPullParserNewPullParser, 0);
                    } else if (name.equals(TAG_CROSS_PROFILE_INTENT_FILTERS)) {
                        readCrossProfileIntentFiltersLPw(xmlPullParserNewPullParser, 0);
                    } else if (name.equals(TAG_DEFAULT_BROWSER)) {
                        readDefaultAppsLPw(xmlPullParserNewPullParser, 0);
                    } else if (name.equals("updated-package")) {
                        readDisabledSysPackageLPw(xmlPullParserNewPullParser);
                    } else if (name.equals("cleaning-package")) {
                        String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_NAME);
                        String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_USER);
                        String attributeValue3 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_CODE);
                        if (attributeValue != null) {
                            if (attributeValue2 != null) {
                                try {
                                    i = Integer.parseInt(attributeValue2);
                                } catch (NumberFormatException e5) {
                                    i = 0;
                                }
                            } else {
                                i = 0;
                            }
                            if (attributeValue3 != null) {
                                z = Boolean.parseBoolean(attributeValue3);
                            } else {
                                z = true;
                            }
                            addPackageToCleanLPw(new PackageCleanItem(i, attributeValue, z));
                        }
                    } else if (name.equals("renamed-package")) {
                        String attributeValue4 = xmlPullParserNewPullParser.getAttributeValue(null, "new");
                        String attributeValue5 = xmlPullParserNewPullParser.getAttributeValue(null, "old");
                        if (attributeValue4 != null && attributeValue5 != null) {
                            this.mRenamedPackages.put(attributeValue4, attributeValue5);
                        }
                    } else if (name.equals("restored-ivi")) {
                        readRestoredIntentFilterVerifications(xmlPullParserNewPullParser);
                    } else if (name.equals("last-platform-version")) {
                        VersionInfo versionInfoFindOrCreateVersion = findOrCreateVersion(StorageManager.UUID_PRIVATE_INTERNAL);
                        VersionInfo versionInfoFindOrCreateVersion2 = findOrCreateVersion("primary_physical");
                        versionInfoFindOrCreateVersion.sdkVersion = XmlUtils.readIntAttribute(xmlPullParserNewPullParser, "internal", 0);
                        versionInfoFindOrCreateVersion2.sdkVersion = XmlUtils.readIntAttribute(xmlPullParserNewPullParser, "external", 0);
                        String stringAttribute = XmlUtils.readStringAttribute(xmlPullParserNewPullParser, ATTR_FINGERPRINT);
                        versionInfoFindOrCreateVersion2.fingerprint = stringAttribute;
                        versionInfoFindOrCreateVersion.fingerprint = stringAttribute;
                    } else if (name.equals("database-version")) {
                        VersionInfo versionInfoFindOrCreateVersion3 = findOrCreateVersion(StorageManager.UUID_PRIVATE_INTERNAL);
                        VersionInfo versionInfoFindOrCreateVersion4 = findOrCreateVersion("primary_physical");
                        versionInfoFindOrCreateVersion3.databaseVersion = XmlUtils.readIntAttribute(xmlPullParserNewPullParser, "internal", 0);
                        versionInfoFindOrCreateVersion4.databaseVersion = XmlUtils.readIntAttribute(xmlPullParserNewPullParser, "external", 0);
                    } else if (name.equals("verifier")) {
                        try {
                            this.mVerifierDeviceIdentity = VerifierDeviceIdentity.parse(xmlPullParserNewPullParser.getAttributeValue(null, "device"));
                        } catch (IllegalArgumentException e6) {
                            Slog.w("PackageManager", "Discard invalid verifier device id: " + e6.getMessage());
                        }
                    } else if (TAG_READ_EXTERNAL_STORAGE.equals(name)) {
                        this.mReadExternalStorageEnforced = "1".equals(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_ENFORCEMENT)) ? Boolean.TRUE : Boolean.FALSE;
                    } else if (name.equals("keyset-settings")) {
                        this.mKeySetManagerService.readKeySetsLPw(xmlPullParserNewPullParser, this.mKeySetRefs);
                    } else if ("version".equals(name)) {
                        VersionInfo versionInfoFindOrCreateVersion5 = findOrCreateVersion(XmlUtils.readStringAttribute(xmlPullParserNewPullParser, ATTR_VOLUME_UUID));
                        versionInfoFindOrCreateVersion5.sdkVersion = XmlUtils.readIntAttribute(xmlPullParserNewPullParser, ATTR_SDK_VERSION);
                        versionInfoFindOrCreateVersion5.databaseVersion = XmlUtils.readIntAttribute(xmlPullParserNewPullParser, ATTR_DATABASE_VERSION);
                        versionInfoFindOrCreateVersion5.fingerprint = XmlUtils.readStringAttribute(xmlPullParserNewPullParser, ATTR_FINGERPRINT);
                    } else {
                        Slog.w("PackageManager", "Unknown element under <packages>: " + xmlPullParserNewPullParser.getName());
                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                    }
                }
            }
        }
        fileInputStream.close();
        int size = this.mPendingPackages.size();
        for (int i2 = 0; i2 < size; i2++) {
            PackageSetting packageSetting = this.mPendingPackages.get(i2);
            int sharedUserId = packageSetting.getSharedUserId();
            Object userIdLPr = getUserIdLPr(sharedUserId);
            if (userIdLPr instanceof SharedUserSetting) {
                SharedUserSetting sharedUserSetting = (SharedUserSetting) userIdLPr;
                packageSetting.sharedUser = sharedUserSetting;
                packageSetting.appId = sharedUserSetting.userId;
                addPackageSettingLPw(packageSetting, sharedUserSetting);
            } else if (userIdLPr != null) {
                String str = "Bad package setting: package " + packageSetting.name + " has shared uid " + sharedUserId + " that is not a shared uid\n";
                this.mReadMessages.append(str);
                PackageManagerService.reportSettingsProblem(6, str);
            } else {
                String str2 = "Bad package setting: package " + packageSetting.name + " has shared uid " + sharedUserId + " that is not defined\n";
                this.mReadMessages.append(str2);
                PackageManagerService.reportSettingsProblem(6, str2);
            }
        }
        this.mPendingPackages.clear();
        if (this.mBackupStoppedPackagesFilename.exists() || this.mStoppedPackagesFilename.exists()) {
            readStoppedLPw();
            this.mBackupStoppedPackagesFilename.delete();
            this.mStoppedPackagesFilename.delete();
            writePackageRestrictionsLPr(0);
        } else {
            Iterator<UserInfo> it = list.iterator();
            while (it.hasNext()) {
                readPackageRestrictionsLPr(it.next().id);
            }
        }
        Iterator<UserInfo> it2 = list.iterator();
        while (it2.hasNext()) {
            this.mRuntimePermissionsPersistence.readStateForUserSyncLPr(it2.next().id);
        }
        for (PackageSetting packageSetting2 : this.mDisabledSysPackages.values()) {
            Object userIdLPr2 = getUserIdLPr(packageSetting2.appId);
            if (userIdLPr2 != null && (userIdLPr2 instanceof SharedUserSetting)) {
                packageSetting2.sharedUser = (SharedUserSetting) userIdLPr2;
            }
        }
        this.mReadMessages.append("Read completed successfully: " + this.mPackages.size() + " packages, " + this.mSharedUsers.size() + " shared uids\n");
        writeKernelMappingLPr();
        return true;
    }

    void applyDefaultPreferredAppsLPw(PackageManagerService packageManagerService, int i) throws Throwable {
        int i2;
        BufferedInputStream bufferedInputStream;
        XmlPullParserException e;
        IOException e2;
        int next;
        Iterator<PackageSetting> it = this.mPackages.values().iterator();
        while (true) {
            i2 = 0;
            if (!it.hasNext()) {
                break;
            }
            PackageSetting next2 = it.next();
            if ((1 & next2.pkgFlags) != 0 && next2.pkg != null && next2.pkg.preferredActivityFilters != null) {
                ArrayList arrayList = next2.pkg.preferredActivityFilters;
                while (i2 < arrayList.size()) {
                    PackageParser.ActivityIntentInfo activityIntentInfo = (PackageParser.ActivityIntentInfo) arrayList.get(i2);
                    applyDefaultPreferredActivityLPw(packageManagerService, activityIntentInfo, new ComponentName(next2.name, activityIntentInfo.activity.className), i);
                    i2++;
                }
            }
        }
        File file = new File(Environment.getRootDirectory(), "etc/preferred-apps");
        if (file.exists() && file.isDirectory()) {
            if (!file.canRead()) {
                Slog.w(TAG, "Directory " + file + " cannot be read");
                return;
            }
            File[] fileArrListFiles = file.listFiles();
            int length = fileArrListFiles.length;
            while (i2 < length) {
                File file2 = fileArrListFiles[i2];
                if (!file2.getPath().endsWith(".xml")) {
                    Slog.i(TAG, "Non-xml file " + file2 + " in " + file + " directory, ignoring");
                } else if (file2.canRead()) {
                    if (PackageManagerService.DEBUG_PREFERRED) {
                        Log.d(TAG, "Reading default preferred " + file2);
                    }
                    try {
                        bufferedInputStream = new BufferedInputStream(new FileInputStream(file2));
                        try {
                            try {
                                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                                xmlPullParserNewPullParser.setInput(bufferedInputStream, null);
                                do {
                                    next = xmlPullParserNewPullParser.next();
                                    if (next == 2) {
                                        break;
                                    }
                                } while (next != 1);
                                if (next != 2) {
                                    Slog.w(TAG, "Preferred apps file " + file2 + " does not have start tag");
                                    try {
                                        bufferedInputStream.close();
                                    } catch (IOException e3) {
                                    }
                                } else if ("preferred-activities".equals(xmlPullParserNewPullParser.getName())) {
                                    readDefaultPreferredActivitiesLPw(packageManagerService, xmlPullParserNewPullParser, i);
                                    bufferedInputStream.close();
                                } else {
                                    Slog.w(TAG, "Preferred apps file " + file2 + " does not start with 'preferred-activities'");
                                    bufferedInputStream.close();
                                }
                            } catch (Throwable th) {
                                th = th;
                                if (bufferedInputStream != null) {
                                    try {
                                        bufferedInputStream.close();
                                    } catch (IOException e4) {
                                    }
                                }
                                throw th;
                            }
                        } catch (IOException e5) {
                            e2 = e5;
                            Slog.w(TAG, "Error reading apps file " + file2, e2);
                            if (bufferedInputStream != null) {
                                bufferedInputStream.close();
                            }
                        } catch (XmlPullParserException e6) {
                            e = e6;
                            Slog.w(TAG, "Error reading apps file " + file2, e);
                            if (bufferedInputStream != null) {
                                bufferedInputStream.close();
                            }
                        }
                    } catch (IOException e7) {
                        bufferedInputStream = null;
                        e2 = e7;
                    } catch (XmlPullParserException e8) {
                        bufferedInputStream = null;
                        e = e8;
                    } catch (Throwable th2) {
                        th = th2;
                        bufferedInputStream = null;
                    }
                } else {
                    Slog.w(TAG, "Preferred apps file " + file2 + " cannot be read");
                }
                i2++;
            }
        }
    }

    private void applyDefaultPreferredActivityLPw(PackageManagerService packageManagerService, IntentFilter intentFilter, ComponentName componentName, int i) {
        int i2;
        if (PackageManagerService.DEBUG_PREFERRED) {
            Log.d(TAG, "Processing preferred:");
            intentFilter.dump(new LogPrinter(3, TAG), "  ");
        }
        Intent intent = new Intent();
        intent.setAction(intentFilter.getAction(0));
        int i3 = 786432;
        for (int i4 = 0; i4 < intentFilter.countCategories(); i4++) {
            String category = intentFilter.getCategory(i4);
            if (category.equals("android.intent.category.DEFAULT")) {
                i3 |= 65536;
            } else {
                intent.addCategory(category);
            }
        }
        int i5 = 0;
        boolean z = false;
        boolean z2 = true;
        while (i5 < intentFilter.countDataSchemes()) {
            String dataScheme = intentFilter.getDataScheme(i5);
            if (dataScheme != null && !dataScheme.isEmpty()) {
                z = true;
            }
            int i6 = 0;
            boolean z3 = true;
            while (i6 < intentFilter.countDataSchemeSpecificParts()) {
                Uri.Builder builder = new Uri.Builder();
                builder.scheme(dataScheme);
                PatternMatcher dataSchemeSpecificPart = intentFilter.getDataSchemeSpecificPart(i6);
                builder.opaquePart(dataSchemeSpecificPart.getPath());
                Intent intent2 = new Intent(intent);
                intent2.setData(builder.build());
                applyDefaultPreferredActivityLPw(packageManagerService, intent2, i3, componentName, dataScheme, dataSchemeSpecificPart, null, null, i);
                i6++;
                z3 = false;
            }
            int i7 = 0;
            while (i7 < intentFilter.countDataAuthorities()) {
                IntentFilter.AuthorityEntry dataAuthority = intentFilter.getDataAuthority(i7);
                int i8 = 0;
                boolean z4 = z3;
                boolean z5 = true;
                while (i8 < intentFilter.countDataPaths()) {
                    Uri.Builder builder2 = new Uri.Builder();
                    builder2.scheme(dataScheme);
                    if (dataAuthority.getHost() != null) {
                        builder2.authority(dataAuthority.getHost());
                    }
                    PatternMatcher dataPath = intentFilter.getDataPath(i8);
                    builder2.path(dataPath.getPath());
                    Intent intent3 = new Intent(intent);
                    intent3.setData(builder2.build());
                    applyDefaultPreferredActivityLPw(packageManagerService, intent3, i3, componentName, dataScheme, null, dataAuthority, dataPath, i);
                    i8++;
                    dataAuthority = dataAuthority;
                    z5 = false;
                    z4 = false;
                    i7 = i7;
                }
                IntentFilter.AuthorityEntry authorityEntry = dataAuthority;
                int i9 = i7;
                if (z5) {
                    Uri.Builder builder3 = new Uri.Builder();
                    builder3.scheme(dataScheme);
                    if (authorityEntry.getHost() != null) {
                        builder3.authority(authorityEntry.getHost());
                    }
                    Intent intent4 = new Intent(intent);
                    intent4.setData(builder3.build());
                    applyDefaultPreferredActivityLPw(packageManagerService, intent4, i3, componentName, dataScheme, null, authorityEntry, null, i);
                    z3 = false;
                } else {
                    z3 = z4;
                }
                i7 = i9 + 1;
            }
            if (z3) {
                Uri.Builder builder4 = new Uri.Builder();
                builder4.scheme(dataScheme);
                Intent intent5 = new Intent(intent);
                intent5.setData(builder4.build());
                applyDefaultPreferredActivityLPw(packageManagerService, intent5, i3, componentName, dataScheme, null, null, null, i);
            }
            i5++;
            z2 = false;
        }
        int i10 = 0;
        while (i10 < intentFilter.countDataTypes()) {
            String dataType = intentFilter.getDataType(i10);
            if (z) {
                Uri.Builder builder5 = new Uri.Builder();
                int i11 = 0;
                while (i11 < intentFilter.countDataSchemes()) {
                    String dataScheme2 = intentFilter.getDataScheme(i11);
                    if (dataScheme2 == null || dataScheme2.isEmpty()) {
                        i2 = i11;
                    } else {
                        Intent intent6 = new Intent(intent);
                        builder5.scheme(dataScheme2);
                        intent6.setDataAndType(builder5.build(), dataType);
                        i2 = i11;
                        applyDefaultPreferredActivityLPw(packageManagerService, intent6, i3, componentName, dataScheme2, null, null, null, i);
                    }
                    i11 = i2 + 1;
                }
            } else {
                Intent intent7 = new Intent(intent);
                intent7.setType(dataType);
                applyDefaultPreferredActivityLPw(packageManagerService, intent7, i3, componentName, null, null, null, null, i);
            }
            i10++;
            z2 = false;
        }
        if (z2) {
            applyDefaultPreferredActivityLPw(packageManagerService, intent, i3, componentName, null, null, null, null, i);
        }
    }

    private void applyDefaultPreferredActivityLPw(PackageManagerService packageManagerService, Intent intent, int i, ComponentName componentName, String str, PatternMatcher patternMatcher, IntentFilter.AuthorityEntry authorityEntry, PatternMatcher patternMatcher2, int i2) {
        ComponentName componentName2;
        ComponentName componentName3;
        int iUpdateFlagsForResolve = packageManagerService.updateFlagsForResolve(i, i2, intent, Binder.getCallingUid(), false);
        List<ResolveInfo> listQueryIntent = packageManagerService.mActivities.queryIntent(intent, intent.getType(), iUpdateFlagsForResolve, 0);
        if (PackageManagerService.DEBUG_PREFERRED) {
            Log.d(TAG, "Queried " + intent + " results: " + listQueryIntent);
        }
        if (listQueryIntent != null && listQueryIntent.size() > 1) {
            ComponentName[] componentNameArr = new ComponentName[listQueryIntent.size()];
            int i3 = 0;
            boolean z = false;
            int i4 = 0;
            while (true) {
                if (i3 < listQueryIntent.size()) {
                    ActivityInfo activityInfo = listQueryIntent.get(i3).activityInfo;
                    componentNameArr[i3] = new ComponentName(activityInfo.packageName, activityInfo.name);
                    if ((activityInfo.applicationInfo.flags & 1) == 0) {
                        if (listQueryIntent.get(i3).match >= 0) {
                            if (PackageManagerService.DEBUG_PREFERRED) {
                                Log.d(TAG, "Result " + activityInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + activityInfo.name + ": non-system!");
                            }
                            componentName2 = componentNameArr[i3];
                        }
                    } else if (componentName.getPackageName().equals(activityInfo.packageName) && componentName.getClassName().equals(activityInfo.name)) {
                        if (PackageManagerService.DEBUG_PREFERRED) {
                            Log.d(TAG, "Result " + activityInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + activityInfo.name + ": default!");
                        }
                        i4 = listQueryIntent.get(i3).match;
                        z = true;
                    } else if (PackageManagerService.DEBUG_PREFERRED) {
                        Log.d(TAG, "Result " + activityInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + activityInfo.name + ": skipped");
                    }
                    i3++;
                } else {
                    componentName2 = null;
                    break;
                }
            }
            ComponentName componentName4 = (componentName2 == null || i4 <= 0) ? componentName2 : null;
            if (!z || componentName4 != null) {
                if (componentName4 == null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("No component ");
                    sb.append(componentName.flattenToShortString());
                    sb.append(" found setting preferred ");
                    sb.append(intent);
                    sb.append("; possible matches are ");
                    for (int i5 = 0; i5 < componentNameArr.length; i5++) {
                        if (i5 > 0) {
                            sb.append(", ");
                        }
                        sb.append(componentNameArr[i5].flattenToShortString());
                    }
                    Slog.w(TAG, sb.toString());
                    return;
                }
                Slog.i(TAG, "Not setting preferred " + intent + "; found third party match " + componentName4.flattenToShortString());
                return;
            }
            IntentFilter intentFilter = new IntentFilter();
            if (intent.getAction() != null) {
                intentFilter.addAction(intent.getAction());
            }
            if (intent.getCategories() != null) {
                Iterator<String> it = intent.getCategories().iterator();
                while (it.hasNext()) {
                    intentFilter.addCategory(it.next());
                }
            }
            if ((iUpdateFlagsForResolve & 65536) != 0) {
                intentFilter.addCategory("android.intent.category.DEFAULT");
            }
            if (str != null) {
                intentFilter.addDataScheme(str);
            }
            if (patternMatcher != null) {
                intentFilter.addDataSchemeSpecificPart(patternMatcher.getPath(), patternMatcher.getType());
            }
            if (authorityEntry != null) {
                intentFilter.addDataAuthority(authorityEntry);
            }
            if (patternMatcher2 != null) {
                intentFilter.addDataPath(patternMatcher2);
            }
            if (intent.getType() != null) {
                try {
                    intentFilter.addDataType(intent.getType());
                    componentName3 = componentName;
                } catch (IntentFilter.MalformedMimeTypeException e) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("Malformed mimetype ");
                    sb2.append(intent.getType());
                    sb2.append(" for ");
                    componentName3 = componentName;
                    sb2.append(componentName3);
                    Slog.w(TAG, sb2.toString());
                }
            } else {
                componentName3 = componentName;
            }
            editPreferredActivitiesLPw(i2).addFilter(new PreferredActivity(intentFilter, i4, componentNameArr, componentName3, true));
            return;
        }
        Slog.w(TAG, "No potential matches found for " + intent + " while setting preferred " + componentName.flattenToShortString());
    }

    private void readDefaultPreferredActivitiesLPw(PackageManagerService packageManagerService, XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4) {
                        if (xmlPullParser.getName().equals(TAG_ITEM)) {
                            PreferredActivity preferredActivity = new PreferredActivity(xmlPullParser);
                            if (preferredActivity.mPref.getParseError() == null) {
                                applyDefaultPreferredActivityLPw(packageManagerService, preferredActivity, preferredActivity.mPref.mComponent, i);
                            } else {
                                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <preferred-activity> " + preferredActivity.mPref.getParseError() + " at " + xmlPullParser.getPositionDescription());
                            }
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Unknown element under <preferred-activities>: " + xmlPullParser.getName());
                            XmlUtils.skipCurrentTag(xmlPullParser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void readDisabledSysPackageLPw(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        long j;
        PackageSetting packageSetting;
        String attributeValue = xmlPullParser.getAttributeValue(null, ATTR_NAME);
        String attributeValue2 = xmlPullParser.getAttributeValue(null, "realName");
        String attributeValue3 = xmlPullParser.getAttributeValue(null, "codePath");
        String attributeValue4 = xmlPullParser.getAttributeValue(null, "resourcePath");
        String attributeValue5 = xmlPullParser.getAttributeValue(null, "requiredCpuAbi");
        String attributeValue6 = xmlPullParser.getAttributeValue(null, "nativeLibraryPath");
        String attributeValue7 = xmlPullParser.getAttributeValue(null, "parentPackageName");
        String attributeValue8 = xmlPullParser.getAttributeValue(null, "primaryCpuAbi");
        String attributeValue9 = xmlPullParser.getAttributeValue(null, "secondaryCpuAbi");
        String attributeValue10 = xmlPullParser.getAttributeValue(null, "cpuAbiOverride");
        String str = (attributeValue8 != null || attributeValue5 == null) ? attributeValue8 : attributeValue5;
        String str2 = attributeValue4 == null ? attributeValue3 : attributeValue4;
        String attributeValue11 = xmlPullParser.getAttributeValue(null, "version");
        if (attributeValue11 != null) {
            try {
                j = Long.parseLong(attributeValue11);
            } catch (NumberFormatException e) {
                j = 0;
            }
        } else {
            j = 0;
        }
        PackageSetting packageSetting2 = new PackageSetting(attributeValue, attributeValue2, new File(attributeValue3), new File(str2), attributeValue6, str, attributeValue9, attributeValue10, j, 1, PackageManagerService.locationIsPrivileged(attributeValue3) ? 8 : 0, attributeValue7, null, 0, null, null);
        String attributeValue12 = xmlPullParser.getAttributeValue(null, "ft");
        if (attributeValue12 != null) {
            try {
                packageSetting = packageSetting2;
                try {
                    packageSetting.setTimeStamp(Long.parseLong(attributeValue12, 16));
                } catch (NumberFormatException e2) {
                }
            } catch (NumberFormatException e3) {
                packageSetting = packageSetting2;
            }
        } else {
            packageSetting = packageSetting2;
            String attributeValue13 = xmlPullParser.getAttributeValue(null, "ts");
            if (attributeValue13 != null) {
                try {
                    packageSetting.setTimeStamp(Long.parseLong(attributeValue13));
                } catch (NumberFormatException e4) {
                }
            }
        }
        String attributeValue14 = xmlPullParser.getAttributeValue(null, "it");
        if (attributeValue14 != null) {
            try {
                packageSetting.firstInstallTime = Long.parseLong(attributeValue14, 16);
            } catch (NumberFormatException e5) {
            }
        }
        String attributeValue15 = xmlPullParser.getAttributeValue(null, "ut");
        if (attributeValue15 != null) {
            try {
                packageSetting.lastUpdateTime = Long.parseLong(attributeValue15, 16);
            } catch (NumberFormatException e6) {
            }
        }
        String attributeValue16 = xmlPullParser.getAttributeValue(null, "userId");
        packageSetting.appId = attributeValue16 != null ? Integer.parseInt(attributeValue16) : 0;
        if (packageSetting.appId <= 0) {
            String attributeValue17 = xmlPullParser.getAttributeValue(null, "sharedUserId");
            packageSetting.appId = attributeValue17 != null ? Integer.parseInt(attributeValue17) : 0;
        }
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != 4) {
                if (xmlPullParser.getName().equals(TAG_PERMISSIONS)) {
                    readInstallPermissionsLPr(xmlPullParser, packageSetting.getPermissionsState());
                } else if (xmlPullParser.getName().equals(TAG_CHILD_PACKAGE)) {
                    String attributeValue18 = xmlPullParser.getAttributeValue(null, ATTR_NAME);
                    if (packageSetting.childPackageNames == null) {
                        packageSetting.childPackageNames = new ArrayList();
                    }
                    packageSetting.childPackageNames.add(attributeValue18);
                } else if (xmlPullParser.getName().equals(TAG_USES_STATIC_LIB)) {
                    readUsesStaticLibLPw(xmlPullParser, packageSetting);
                } else {
                    PackageManagerService.reportSettingsProblem(5, "Unknown element under <updated-package>: " + xmlPullParser.getName());
                    XmlUtils.skipCurrentTag(xmlPullParser);
                }
            }
        }
        this.mDisabledSysPackages.put(attributeValue, packageSetting);
    }

    private void readPackageLPw(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int i;
        Settings settings;
        int i2;
        String attributeValue;
        String attributeValue2;
        String attributeValue3;
        String attributeValue4;
        String attributeValue5;
        String attributeValue6;
        String str;
        String attributeValue7;
        String attributeValue8;
        String attributeValue9;
        String str2;
        String str3;
        int i3;
        ?? r8;
        String attributeValue10;
        String attributeValue11;
        long j;
        String attributeValue12;
        String attributeValue13;
        int i4;
        int i5;
        int i6;
        String attributeValue14;
        long j2;
        String attributeValue15;
        long j3;
        String attributeValue16;
        long j4;
        String str4;
        String str5;
        int i7;
        PackageSetting packageSettingAddPackageLPw = null;
        try {
            attributeValue2 = xmlPullParser.getAttributeValue(null, ATTR_NAME);
            try {
                attributeValue10 = xmlPullParser.getAttributeValue(null, "realName");
                attributeValue = xmlPullParser.getAttributeValue(null, "userId");
            } catch (NumberFormatException e) {
                i = 5;
                settings = this;
                i2 = -1;
                attributeValue = null;
            }
        } catch (NumberFormatException e2) {
            i = 5;
            settings = this;
            i2 = -1;
            attributeValue = null;
            attributeValue2 = null;
        }
        try {
            attributeValue3 = xmlPullParser.getAttributeValue(null, "uidError");
        } catch (NumberFormatException e3) {
            i = 5;
            settings = this;
            i2 = -1;
            packageSettingAddPackageLPw = null;
            attributeValue3 = null;
            attributeValue4 = null;
            attributeValue5 = null;
            attributeValue6 = null;
            str = null;
            attributeValue7 = null;
            attributeValue8 = null;
            attributeValue9 = null;
            PackageManagerService.reportSettingsProblem(i, "Error in package manager settings: package " + attributeValue2 + " has bad userId " + attributeValue + " at " + xmlPullParser.getPositionDescription());
            str2 = attributeValue;
            str3 = attributeValue2;
            String str6 = attributeValue3;
            String str7 = attributeValue4;
            String str8 = attributeValue5;
            String str9 = attributeValue6;
            String str10 = str;
            String str11 = attributeValue7;
            String str12 = attributeValue8;
            String str13 = attributeValue9;
            int i8 = i2;
            if (packageSettingAddPackageLPw == null) {
            }
        }
        try {
            String attributeValue17 = xmlPullParser.getAttributeValue(null, "sharedUserId");
            String attributeValue18 = xmlPullParser.getAttributeValue(null, "codePath");
            String attributeValue19 = xmlPullParser.getAttributeValue(null, "resourcePath");
            String attributeValue20 = xmlPullParser.getAttributeValue(null, "requiredCpuAbi");
            String attributeValue21 = xmlPullParser.getAttributeValue(null, "parentPackageName");
            attributeValue4 = xmlPullParser.getAttributeValue(null, "nativeLibraryPath");
            try {
                String attributeValue22 = xmlPullParser.getAttributeValue(null, "primaryCpuAbi");
                try {
                    attributeValue5 = xmlPullParser.getAttributeValue(null, "secondaryCpuAbi");
                    try {
                        attributeValue11 = xmlPullParser.getAttributeValue(null, "cpuAbiOverride");
                        attributeValue6 = xmlPullParser.getAttributeValue(null, "updateAvailable");
                        str = (attributeValue22 != null || attributeValue20 == null) ? attributeValue22 : attributeValue20;
                    } catch (NumberFormatException e4) {
                        i = 5;
                        settings = this;
                        i2 = -1;
                        str = attributeValue22;
                        packageSettingAddPackageLPw = null;
                        attributeValue6 = null;
                        attributeValue7 = null;
                        attributeValue8 = null;
                        attributeValue9 = null;
                        PackageManagerService.reportSettingsProblem(i, "Error in package manager settings: package " + attributeValue2 + " has bad userId " + attributeValue + " at " + xmlPullParser.getPositionDescription());
                        str2 = attributeValue;
                        str3 = attributeValue2;
                        String str62 = attributeValue3;
                        String str72 = attributeValue4;
                        String str82 = attributeValue5;
                        String str92 = attributeValue6;
                        String str102 = str;
                        String str112 = attributeValue7;
                        String str122 = attributeValue8;
                        String str132 = attributeValue9;
                        int i82 = i2;
                        if (packageSettingAddPackageLPw == null) {
                        }
                    }
                    try {
                        String attributeValue23 = xmlPullParser.getAttributeValue(null, "version");
                        if (attributeValue23 != null) {
                            try {
                                j = Long.parseLong(attributeValue23);
                            } catch (NumberFormatException e5) {
                                j = 0;
                            }
                            attributeValue7 = xmlPullParser.getAttributeValue(null, "installer");
                            try {
                                attributeValue8 = xmlPullParser.getAttributeValue(null, "isOrphaned");
                            } catch (NumberFormatException e6) {
                                i = 5;
                                settings = this;
                                i2 = -1;
                                packageSettingAddPackageLPw = null;
                                attributeValue8 = null;
                                attributeValue9 = null;
                                PackageManagerService.reportSettingsProblem(i, "Error in package manager settings: package " + attributeValue2 + " has bad userId " + attributeValue + " at " + xmlPullParser.getPositionDescription());
                                str2 = attributeValue;
                                str3 = attributeValue2;
                                String str622 = attributeValue3;
                                String str722 = attributeValue4;
                                String str822 = attributeValue5;
                                String str922 = attributeValue6;
                                String str1022 = str;
                                String str1122 = attributeValue7;
                                String str1222 = attributeValue8;
                                String str1322 = attributeValue9;
                                int i822 = i2;
                                if (packageSettingAddPackageLPw == null) {
                                }
                            }
                            try {
                                attributeValue9 = xmlPullParser.getAttributeValue(null, ATTR_VOLUME_UUID);
                                try {
                                    attributeValue12 = xmlPullParser.getAttributeValue(null, "categoryHint");
                                    if (attributeValue12 == null) {
                                        try {
                                            i2 = Integer.parseInt(attributeValue12);
                                        } catch (NumberFormatException e7) {
                                            i2 = -1;
                                        }
                                        try {
                                            attributeValue13 = xmlPullParser.getAttributeValue(null, "publicFlags");
                                            try {
                                                if (attributeValue13 == null) {
                                                    try {
                                                        i4 = Integer.parseInt(attributeValue13);
                                                    } catch (NumberFormatException e8) {
                                                        i4 = 0;
                                                    }
                                                    String attributeValue24 = xmlPullParser.getAttributeValue(null, "privateFlags");
                                                    if (attributeValue24 != null) {
                                                        try {
                                                            i5 = Integer.parseInt(attributeValue24);
                                                            i6 = i4;
                                                        } catch (NumberFormatException e9) {
                                                            i6 = i4;
                                                            i5 = 0;
                                                        }
                                                        attributeValue14 = xmlPullParser.getAttributeValue(null, "ft");
                                                        if (attributeValue14 != null) {
                                                            try {
                                                                j2 = Long.parseLong(attributeValue14, 16);
                                                            } catch (NumberFormatException e10) {
                                                                j2 = 0;
                                                            }
                                                            attributeValue15 = xmlPullParser.getAttributeValue(null, "it");
                                                            if (attributeValue15 != null) {
                                                                try {
                                                                    j3 = Long.parseLong(attributeValue15, 16);
                                                                } catch (NumberFormatException e11) {
                                                                    j3 = 0;
                                                                }
                                                                attributeValue16 = xmlPullParser.getAttributeValue(null, "ut");
                                                                if (attributeValue16 == null) {
                                                                    try {
                                                                        j4 = Long.parseLong(attributeValue16, 16);
                                                                    } catch (NumberFormatException e12) {
                                                                        j4 = 0;
                                                                    }
                                                                    if (PackageManagerService.DEBUG_SETTINGS) {
                                                                        try {
                                                                            Log.v("PackageManager", "Reading package: " + attributeValue2 + " userId=" + attributeValue + " sharedUserId=" + attributeValue17);
                                                                        } catch (NumberFormatException e13) {
                                                                            i = 5;
                                                                            settings = this;
                                                                            packageSettingAddPackageLPw = null;
                                                                            PackageManagerService.reportSettingsProblem(i, "Error in package manager settings: package " + attributeValue2 + " has bad userId " + attributeValue + " at " + xmlPullParser.getPositionDescription());
                                                                            str2 = attributeValue;
                                                                            str3 = attributeValue2;
                                                                            String str6222 = attributeValue3;
                                                                            String str7222 = attributeValue4;
                                                                            String str8222 = attributeValue5;
                                                                            String str9222 = attributeValue6;
                                                                            String str10222 = str;
                                                                            String str11222 = attributeValue7;
                                                                            String str12222 = attributeValue8;
                                                                            String str13222 = attributeValue9;
                                                                            int i8222 = i2;
                                                                            if (packageSettingAddPackageLPw == null) {
                                                                            }
                                                                        }
                                                                    }
                                                                    int i9 = attributeValue == null ? Integer.parseInt(attributeValue) : 0;
                                                                    int i10 = attributeValue17 == null ? Integer.parseInt(attributeValue17) : 0;
                                                                    if (attributeValue19 == null) {
                                                                        attributeValue19 = attributeValue18;
                                                                    }
                                                                    if (attributeValue10 != null) {
                                                                        attributeValue10 = attributeValue10.intern();
                                                                    }
                                                                    String str14 = attributeValue10;
                                                                    if (attributeValue2 != null) {
                                                                        PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <package> has no name at " + xmlPullParser.getPositionDescription());
                                                                    } else if (attributeValue18 == null) {
                                                                        PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <package> has no codePath at " + xmlPullParser.getPositionDescription());
                                                                    } else if (i9 > 0) {
                                                                        try {
                                                                            String strIntern = attributeValue2.intern();
                                                                            File file = new File(attributeValue18);
                                                                            long j5 = j4;
                                                                            long j6 = j2;
                                                                            int i11 = i9;
                                                                            try {
                                                                                packageSettingAddPackageLPw = addPackageLPw(strIntern, str14, file, new File(attributeValue19), attributeValue4, str, attributeValue5, attributeValue11, i9, j, i6, i5, attributeValue21, null, null, null);
                                                                                try {
                                                                                    if (PackageManagerService.DEBUG_SETTINGS) {
                                                                                        StringBuilder sb = new StringBuilder();
                                                                                        sb.append("Reading package ");
                                                                                        str4 = attributeValue2;
                                                                                        try {
                                                                                            sb.append(str4);
                                                                                            sb.append(": userId=");
                                                                                            sb.append(i11);
                                                                                            sb.append(" pkg=");
                                                                                            sb.append(packageSettingAddPackageLPw);
                                                                                            Log.i("PackageManager", sb.toString());
                                                                                        } catch (NumberFormatException e14) {
                                                                                            attributeValue2 = str4;
                                                                                            attributeValue = attributeValue;
                                                                                            settings = this;
                                                                                            i = 5;
                                                                                            PackageManagerService.reportSettingsProblem(i, "Error in package manager settings: package " + attributeValue2 + " has bad userId " + attributeValue + " at " + xmlPullParser.getPositionDescription());
                                                                                            str2 = attributeValue;
                                                                                            str3 = attributeValue2;
                                                                                        }
                                                                                    } else {
                                                                                        str4 = attributeValue2;
                                                                                    }
                                                                                    if (packageSettingAddPackageLPw == null) {
                                                                                        PackageManagerService.reportSettingsProblem(6, "Failure adding uid " + i11 + " while parsing settings at " + xmlPullParser.getPositionDescription());
                                                                                    } else {
                                                                                        packageSettingAddPackageLPw.setTimeStamp(j6);
                                                                                        packageSettingAddPackageLPw.firstInstallTime = j3;
                                                                                        packageSettingAddPackageLPw.lastUpdateTime = j5;
                                                                                    }
                                                                                    str5 = attributeValue;
                                                                                    settings = this;
                                                                                    i = 5;
                                                                                    str2 = str5;
                                                                                    str3 = str4;
                                                                                } catch (NumberFormatException e15) {
                                                                                    str4 = attributeValue2;
                                                                                }
                                                                            } catch (NumberFormatException e16) {
                                                                                attributeValue2 = attributeValue2;
                                                                                attributeValue = attributeValue;
                                                                                settings = this;
                                                                                i = 5;
                                                                                packageSettingAddPackageLPw = null;
                                                                                PackageManagerService.reportSettingsProblem(i, "Error in package manager settings: package " + attributeValue2 + " has bad userId " + attributeValue + " at " + xmlPullParser.getPositionDescription());
                                                                                str2 = attributeValue;
                                                                                str3 = attributeValue2;
                                                                            }
                                                                        } catch (NumberFormatException e17) {
                                                                            i = 5;
                                                                            settings = this;
                                                                            packageSettingAddPackageLPw = null;
                                                                            PackageManagerService.reportSettingsProblem(i, "Error in package manager settings: package " + attributeValue2 + " has bad userId " + attributeValue + " at " + xmlPullParser.getPositionDescription());
                                                                            str2 = attributeValue;
                                                                            str3 = attributeValue2;
                                                                        }
                                                                    } else {
                                                                        long j7 = j4;
                                                                        long j8 = j2;
                                                                        str4 = attributeValue2;
                                                                        long j9 = j3;
                                                                        if (attributeValue17 == null) {
                                                                            settings = this;
                                                                            i = 5;
                                                                            try {
                                                                                StringBuilder sb2 = new StringBuilder();
                                                                                sb2.append("Error in package manager settings: package ");
                                                                                sb2.append(str4);
                                                                                sb2.append(" has bad userId ");
                                                                                str5 = attributeValue;
                                                                                try {
                                                                                    sb2.append(str5);
                                                                                    sb2.append(" at ");
                                                                                    sb2.append(xmlPullParser.getPositionDescription());
                                                                                    PackageManagerService.reportSettingsProblem(5, sb2.toString());
                                                                                    packageSettingAddPackageLPw = null;
                                                                                    str2 = str5;
                                                                                    str3 = str4;
                                                                                } catch (NumberFormatException e18) {
                                                                                    attributeValue = str5;
                                                                                    attributeValue2 = str4;
                                                                                    packageSettingAddPackageLPw = null;
                                                                                    PackageManagerService.reportSettingsProblem(i, "Error in package manager settings: package " + attributeValue2 + " has bad userId " + attributeValue + " at " + xmlPullParser.getPositionDescription());
                                                                                    str2 = attributeValue;
                                                                                    str3 = attributeValue2;
                                                                                }
                                                                            } catch (NumberFormatException e19) {
                                                                                str5 = attributeValue;
                                                                            }
                                                                        } else if (i10 > 0) {
                                                                            try {
                                                                                PackageSetting packageSetting = new PackageSetting(str4.intern(), str14, new File(attributeValue18), new File(attributeValue19), attributeValue4, str, attributeValue5, attributeValue11, j, i6, i5, attributeValue21, null, i10, null, null);
                                                                                try {
                                                                                    packageSetting.setTimeStamp(j8);
                                                                                    packageSetting.firstInstallTime = j9;
                                                                                    packageSetting.lastUpdateTime = j7;
                                                                                    settings = this;
                                                                                    try {
                                                                                        settings.mPendingPackages.add(packageSetting);
                                                                                        if (PackageManagerService.DEBUG_SETTINGS) {
                                                                                            Log.i("PackageManager", "Reading package " + str4 + ": sharedUserId=" + i10 + " pkg=" + packageSetting);
                                                                                        }
                                                                                        packageSettingAddPackageLPw = packageSetting;
                                                                                        str5 = attributeValue;
                                                                                        i = 5;
                                                                                        str2 = str5;
                                                                                        str3 = str4;
                                                                                    } catch (NumberFormatException e20) {
                                                                                        packageSettingAddPackageLPw = packageSetting;
                                                                                        attributeValue2 = str4;
                                                                                        attributeValue = attributeValue;
                                                                                        i = 5;
                                                                                        PackageManagerService.reportSettingsProblem(i, "Error in package manager settings: package " + attributeValue2 + " has bad userId " + attributeValue + " at " + xmlPullParser.getPositionDescription());
                                                                                        str2 = attributeValue;
                                                                                        str3 = attributeValue2;
                                                                                    }
                                                                                } catch (NumberFormatException e21) {
                                                                                    settings = this;
                                                                                }
                                                                            } catch (NumberFormatException e22) {
                                                                                settings = this;
                                                                                attributeValue2 = str4;
                                                                                attributeValue = attributeValue;
                                                                                i = 5;
                                                                                packageSettingAddPackageLPw = null;
                                                                                PackageManagerService.reportSettingsProblem(i, "Error in package manager settings: package " + attributeValue2 + " has bad userId " + attributeValue + " at " + xmlPullParser.getPositionDescription());
                                                                                str2 = attributeValue;
                                                                                str3 = attributeValue2;
                                                                            }
                                                                        } else {
                                                                            settings = this;
                                                                            try {
                                                                                i = 5;
                                                                                try {
                                                                                    PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: package " + str4 + " has bad sharedId " + attributeValue17 + " at " + xmlPullParser.getPositionDescription());
                                                                                    str5 = attributeValue;
                                                                                    packageSettingAddPackageLPw = null;
                                                                                    str2 = str5;
                                                                                    str3 = str4;
                                                                                } catch (NumberFormatException e23) {
                                                                                    attributeValue2 = str4;
                                                                                    attributeValue = attributeValue;
                                                                                    packageSettingAddPackageLPw = null;
                                                                                    PackageManagerService.reportSettingsProblem(i, "Error in package manager settings: package " + attributeValue2 + " has bad userId " + attributeValue + " at " + xmlPullParser.getPositionDescription());
                                                                                    str2 = attributeValue;
                                                                                    str3 = attributeValue2;
                                                                                }
                                                                            } catch (NumberFormatException e24) {
                                                                                i = 5;
                                                                            }
                                                                        }
                                                                    }
                                                                    str5 = attributeValue;
                                                                    str4 = attributeValue2;
                                                                    i = 5;
                                                                    settings = this;
                                                                    packageSettingAddPackageLPw = null;
                                                                    str2 = str5;
                                                                    str3 = str4;
                                                                } else {
                                                                    j4 = 0;
                                                                    if (PackageManagerService.DEBUG_SETTINGS) {
                                                                    }
                                                                    if (attributeValue == null) {
                                                                    }
                                                                    if (attributeValue17 == null) {
                                                                    }
                                                                    if (attributeValue19 == null) {
                                                                    }
                                                                    if (attributeValue10 != null) {
                                                                    }
                                                                    String str142 = attributeValue10;
                                                                    if (attributeValue2 != null) {
                                                                    }
                                                                    str5 = attributeValue;
                                                                    str4 = attributeValue2;
                                                                    i = 5;
                                                                    settings = this;
                                                                    packageSettingAddPackageLPw = null;
                                                                    str2 = str5;
                                                                    str3 = str4;
                                                                }
                                                            } else {
                                                                j3 = 0;
                                                                attributeValue16 = xmlPullParser.getAttributeValue(null, "ut");
                                                                if (attributeValue16 == null) {
                                                                }
                                                            }
                                                        } else {
                                                            String attributeValue25 = xmlPullParser.getAttributeValue(null, "ts");
                                                            if (attributeValue25 != null) {
                                                                try {
                                                                    j2 = Long.parseLong(attributeValue25);
                                                                } catch (NumberFormatException e25) {
                                                                    j2 = 0;
                                                                }
                                                                attributeValue15 = xmlPullParser.getAttributeValue(null, "it");
                                                                if (attributeValue15 != null) {
                                                                }
                                                            } else {
                                                                j2 = 0;
                                                                attributeValue15 = xmlPullParser.getAttributeValue(null, "it");
                                                                if (attributeValue15 != null) {
                                                                }
                                                            }
                                                        }
                                                    }
                                                    i6 = i4;
                                                    i5 = 0;
                                                    attributeValue14 = xmlPullParser.getAttributeValue(null, "ft");
                                                    if (attributeValue14 != null) {
                                                    }
                                                } else {
                                                    String attributeValue26 = xmlPullParser.getAttributeValue(null, ATTR_FLAGS);
                                                    if (attributeValue26 != null) {
                                                        try {
                                                            i7 = Integer.parseInt(attributeValue26);
                                                        } catch (NumberFormatException e26) {
                                                            i7 = 0;
                                                        }
                                                        int i12 = (PRE_M_APP_INFO_FLAG_HIDDEN & i7) != 0 ? 1 : 0;
                                                        if ((i7 & PRE_M_APP_INFO_FLAG_CANT_SAVE_STATE) != 0) {
                                                            i12 |= 2;
                                                        }
                                                        if ((i7 & PRE_M_APP_INFO_FLAG_FORWARD_LOCK) != 0) {
                                                            i12 |= 4;
                                                        }
                                                        if ((i7 & PRE_M_APP_INFO_FLAG_PRIVILEGED) != 0) {
                                                            i12 |= 8;
                                                        }
                                                        i6 = i7 & (~(PRE_M_APP_INFO_FLAG_HIDDEN | PRE_M_APP_INFO_FLAG_CANT_SAVE_STATE | PRE_M_APP_INFO_FLAG_FORWARD_LOCK | PRE_M_APP_INFO_FLAG_PRIVILEGED));
                                                        i5 = i12;
                                                    } else {
                                                        String attributeValue27 = xmlPullParser.getAttributeValue(null, "system");
                                                        if (attributeValue27 != null) {
                                                            i6 = ("true".equalsIgnoreCase(attributeValue27) ? 1 : 0) | 0;
                                                            i5 = 0;
                                                        } else {
                                                            i5 = 0;
                                                            i6 = 1;
                                                        }
                                                    }
                                                    attributeValue14 = xmlPullParser.getAttributeValue(null, "ft");
                                                    if (attributeValue14 != null) {
                                                    }
                                                }
                                            } catch (NumberFormatException e27) {
                                                i = 5;
                                                settings = this;
                                            }
                                        } catch (NumberFormatException e28) {
                                        }
                                    } else {
                                        i2 = -1;
                                        attributeValue13 = xmlPullParser.getAttributeValue(null, "publicFlags");
                                        if (attributeValue13 == null) {
                                        }
                                    }
                                } catch (NumberFormatException e29) {
                                    i = 5;
                                    settings = this;
                                    i2 = -1;
                                }
                            } catch (NumberFormatException e30) {
                                i = 5;
                                settings = this;
                                i2 = -1;
                                packageSettingAddPackageLPw = null;
                                attributeValue9 = null;
                                PackageManagerService.reportSettingsProblem(i, "Error in package manager settings: package " + attributeValue2 + " has bad userId " + attributeValue + " at " + xmlPullParser.getPositionDescription());
                                str2 = attributeValue;
                                str3 = attributeValue2;
                                String str62222 = attributeValue3;
                                String str72222 = attributeValue4;
                                String str82222 = attributeValue5;
                                String str92222 = attributeValue6;
                                String str102222 = str;
                                String str112222 = attributeValue7;
                                String str122222 = attributeValue8;
                                String str132222 = attributeValue9;
                                int i82222 = i2;
                                if (packageSettingAddPackageLPw == null) {
                                }
                            }
                        } else {
                            j = 0;
                            attributeValue7 = xmlPullParser.getAttributeValue(null, "installer");
                            attributeValue8 = xmlPullParser.getAttributeValue(null, "isOrphaned");
                            attributeValue9 = xmlPullParser.getAttributeValue(null, ATTR_VOLUME_UUID);
                            attributeValue12 = xmlPullParser.getAttributeValue(null, "categoryHint");
                            if (attributeValue12 == null) {
                            }
                        }
                    } catch (NumberFormatException e31) {
                        i = 5;
                        settings = this;
                        i2 = -1;
                        packageSettingAddPackageLPw = null;
                        attributeValue7 = null;
                        attributeValue8 = null;
                        attributeValue9 = null;
                        PackageManagerService.reportSettingsProblem(i, "Error in package manager settings: package " + attributeValue2 + " has bad userId " + attributeValue + " at " + xmlPullParser.getPositionDescription());
                        str2 = attributeValue;
                        str3 = attributeValue2;
                        String str622222 = attributeValue3;
                        String str722222 = attributeValue4;
                        String str822222 = attributeValue5;
                        String str922222 = attributeValue6;
                        String str1022222 = str;
                        String str1122222 = attributeValue7;
                        String str1222222 = attributeValue8;
                        String str1322222 = attributeValue9;
                        int i822222 = i2;
                        if (packageSettingAddPackageLPw == null) {
                        }
                    }
                } catch (NumberFormatException e32) {
                    i = 5;
                    settings = this;
                    i2 = -1;
                    str = attributeValue22;
                    packageSettingAddPackageLPw = null;
                    attributeValue5 = null;
                }
            } catch (NumberFormatException e33) {
                i = 5;
                settings = this;
                i2 = -1;
                packageSettingAddPackageLPw = null;
                attributeValue5 = null;
                attributeValue6 = null;
                str = null;
                attributeValue7 = null;
                attributeValue8 = null;
                attributeValue9 = null;
                PackageManagerService.reportSettingsProblem(i, "Error in package manager settings: package " + attributeValue2 + " has bad userId " + attributeValue + " at " + xmlPullParser.getPositionDescription());
                str2 = attributeValue;
                str3 = attributeValue2;
                String str6222222 = attributeValue3;
                String str7222222 = attributeValue4;
                String str8222222 = attributeValue5;
                String str9222222 = attributeValue6;
                String str10222222 = str;
                String str11222222 = attributeValue7;
                String str12222222 = attributeValue8;
                String str13222222 = attributeValue9;
                int i8222222 = i2;
                if (packageSettingAddPackageLPw == null) {
                }
            }
        } catch (NumberFormatException e34) {
            i = 5;
            settings = this;
            i2 = -1;
            packageSettingAddPackageLPw = null;
            attributeValue4 = null;
            attributeValue5 = null;
            attributeValue6 = null;
            str = null;
            attributeValue7 = null;
            attributeValue8 = null;
            attributeValue9 = null;
            PackageManagerService.reportSettingsProblem(i, "Error in package manager settings: package " + attributeValue2 + " has bad userId " + attributeValue + " at " + xmlPullParser.getPositionDescription());
            str2 = attributeValue;
            str3 = attributeValue2;
            String str62222222 = attributeValue3;
            String str72222222 = attributeValue4;
            String str82222222 = attributeValue5;
            String str92222222 = attributeValue6;
            String str102222222 = str;
            String str112222222 = attributeValue7;
            String str122222222 = attributeValue8;
            String str132222222 = attributeValue9;
            int i82222222 = i2;
            if (packageSettingAddPackageLPw == null) {
            }
        }
        String str622222222 = attributeValue3;
        String str722222222 = attributeValue4;
        String str822222222 = attributeValue5;
        String str922222222 = attributeValue6;
        String str1022222222 = str;
        String str1122222222 = attributeValue7;
        String str1222222222 = attributeValue8;
        String str1322222222 = attributeValue9;
        int i822222222 = i2;
        if (packageSettingAddPackageLPw == null) {
            XmlUtils.skipCurrentTag(xmlPullParser);
            return;
        }
        packageSettingAddPackageLPw.uidError = "true".equals(str622222222);
        packageSettingAddPackageLPw.installerPackageName = str1122222222;
        packageSettingAddPackageLPw.isOrphaned = "true".equals(str1222222222);
        packageSettingAddPackageLPw.volumeUuid = str1322222222;
        packageSettingAddPackageLPw.categoryHint = i822222222;
        packageSettingAddPackageLPw.legacyNativeLibraryPathString = str722222222;
        packageSettingAddPackageLPw.primaryCpuAbiString = str1022222222;
        packageSettingAddPackageLPw.secondaryCpuAbiString = str822222222;
        packageSettingAddPackageLPw.updateAvailable = "true".equals(str922222222);
        String attributeValue28 = xmlPullParser.getAttributeValue(null, ATTR_ENABLED);
        if (attributeValue28 != null) {
            try {
                i3 = 0;
                try {
                    packageSettingAddPackageLPw.setEnabled(Integer.parseInt(attributeValue28), 0, null);
                    r8 = 1;
                } catch (NumberFormatException e35) {
                    if (attributeValue28.equalsIgnoreCase("true")) {
                        r8 = 1;
                        packageSettingAddPackageLPw.setEnabled(1, i3, null);
                    } else {
                        r8 = 1;
                        r8 = 1;
                        r8 = 1;
                        if (attributeValue28.equalsIgnoreCase("false")) {
                            packageSettingAddPackageLPw.setEnabled(2, i3, null);
                        } else if (attributeValue28.equalsIgnoreCase(BatteryService.HealthServiceWrapper.INSTANCE_VENDOR)) {
                            packageSettingAddPackageLPw.setEnabled(i3, i3, null);
                        } else {
                            PackageManagerService.reportSettingsProblem(i, "Error in package manager settings: package " + str3 + " has bad enabled value: " + str2 + " at " + xmlPullParser.getPositionDescription());
                        }
                    }
                }
            } catch (NumberFormatException e36) {
                i3 = 0;
            }
        } else {
            i3 = 0;
            r8 = 1;
            packageSettingAddPackageLPw.setEnabled(0, 0, null);
        }
        if (str1122222222 != null) {
            settings.mInstallerPackages.add(str1122222222);
        }
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next == r8) {
                return;
            }
            if (next == 3 && xmlPullParser.getDepth() <= depth) {
                return;
            }
            if (next != 3 && next != 4) {
                String name = xmlPullParser.getName();
                if (name.equals(TAG_DISABLED_COMPONENTS)) {
                    settings.readDisabledComponentsLPw(packageSettingAddPackageLPw, xmlPullParser, i3);
                } else if (name.equals(TAG_ENABLED_COMPONENTS)) {
                    settings.readEnabledComponentsLPw(packageSettingAddPackageLPw, xmlPullParser, i3);
                } else if (name.equals("sigs")) {
                    packageSettingAddPackageLPw.signatures.readXml(xmlPullParser, settings.mPastSignatures);
                } else if (name.equals(TAG_PERMISSIONS)) {
                    settings.readInstallPermissionsLPr(xmlPullParser, packageSettingAddPackageLPw.getPermissionsState());
                    packageSettingAddPackageLPw.installPermissionsFixed = r8;
                } else if (name.equals("proper-signing-keyset")) {
                    long j10 = Long.parseLong(xmlPullParser.getAttributeValue(null, "identifier"));
                    Integer num = settings.mKeySetRefs.get(Long.valueOf(j10));
                    if (num != null) {
                        settings.mKeySetRefs.put(Long.valueOf(j10), Integer.valueOf(num.intValue() + r8));
                    } else {
                        settings.mKeySetRefs.put(Long.valueOf(j10), Integer.valueOf((int) r8));
                    }
                    packageSettingAddPackageLPw.keySetData.setProperSigningKeySet(j10);
                } else if (!name.equals("signing-keyset")) {
                    if (name.equals("upgrade-keyset")) {
                        packageSettingAddPackageLPw.keySetData.addUpgradeKeySetById(Long.parseLong(xmlPullParser.getAttributeValue(null, "identifier")));
                    } else if (name.equals("defined-keyset")) {
                        long j11 = Long.parseLong(xmlPullParser.getAttributeValue(null, "identifier"));
                        String attributeValue29 = xmlPullParser.getAttributeValue(null, "alias");
                        Integer num2 = settings.mKeySetRefs.get(Long.valueOf(j11));
                        if (num2 != null) {
                            settings.mKeySetRefs.put(Long.valueOf(j11), Integer.valueOf(num2.intValue() + r8));
                        } else {
                            settings.mKeySetRefs.put(Long.valueOf(j11), Integer.valueOf((int) r8));
                        }
                        packageSettingAddPackageLPw.keySetData.addDefinedKeySet(j11, attributeValue29);
                    } else if (name.equals(TAG_DOMAIN_VERIFICATION)) {
                        settings.readDomainVerificationLPw(xmlPullParser, packageSettingAddPackageLPw);
                    } else if (name.equals(TAG_CHILD_PACKAGE)) {
                        String attributeValue30 = xmlPullParser.getAttributeValue(null, ATTR_NAME);
                        if (packageSettingAddPackageLPw.childPackageNames == null) {
                            packageSettingAddPackageLPw.childPackageNames = new ArrayList();
                        }
                        packageSettingAddPackageLPw.childPackageNames.add(attributeValue30);
                    } else {
                        PackageManagerService.reportSettingsProblem(i, "Unknown element under <package>: " + xmlPullParser.getName());
                        XmlUtils.skipCurrentTag(xmlPullParser);
                    }
                }
            }
        }
    }

    private void readDisabledComponentsLPw(PackageSettingBase packageSettingBase, XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4) {
                        if (xmlPullParser.getName().equals(TAG_ITEM)) {
                            String attributeValue = xmlPullParser.getAttributeValue(null, ATTR_NAME);
                            if (attributeValue != null) {
                                packageSettingBase.addDisabledComponent(attributeValue.intern(), i);
                            } else {
                                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <disabled-components> has no name at " + xmlPullParser.getPositionDescription());
                            }
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Unknown element under <disabled-components>: " + xmlPullParser.getName());
                        }
                        XmlUtils.skipCurrentTag(xmlPullParser);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void readEnabledComponentsLPw(PackageSettingBase packageSettingBase, XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4) {
                        if (xmlPullParser.getName().equals(TAG_ITEM)) {
                            String attributeValue = xmlPullParser.getAttributeValue(null, ATTR_NAME);
                            if (attributeValue != null) {
                                packageSettingBase.addEnabledComponent(attributeValue.intern(), i);
                            } else {
                                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <enabled-components> has no name at " + xmlPullParser.getPositionDescription());
                            }
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Unknown element under <enabled-components>: " + xmlPullParser.getName());
                        }
                        XmlUtils.skipCurrentTag(xmlPullParser);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void readSharedUserLPw(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        String attributeValue;
        String attributeValue2;
        int i;
        int i2;
        SharedUserSetting sharedUserSetting = null;
        try {
            attributeValue = xmlPullParser.getAttributeValue(null, ATTR_NAME);
            try {
                attributeValue2 = xmlPullParser.getAttributeValue(null, "userId");
                if (attributeValue2 == null) {
                    i = 0;
                } else {
                    try {
                        i = Integer.parseInt(attributeValue2);
                    } catch (NumberFormatException e) {
                        PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: package " + attributeValue + " has bad userId " + attributeValue2 + " at " + xmlPullParser.getPositionDescription());
                        if (sharedUserSetting == null) {
                        }
                    }
                }
                if (!"true".equals(xmlPullParser.getAttributeValue(null, "system"))) {
                    i2 = 0;
                } else {
                    i2 = 1;
                }
                if (attributeValue == null) {
                    PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <shared-user> has no name at " + xmlPullParser.getPositionDescription());
                } else if (i != 0) {
                    SharedUserSetting sharedUserSettingAddSharedUserLPw = addSharedUserLPw(attributeValue.intern(), i, i2, 0);
                    if (sharedUserSettingAddSharedUserLPw == null) {
                        try {
                            PackageManagerService.reportSettingsProblem(6, "Occurred while parsing settings at " + xmlPullParser.getPositionDescription());
                        } catch (NumberFormatException e2) {
                            sharedUserSetting = sharedUserSettingAddSharedUserLPw;
                            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: package " + attributeValue + " has bad userId " + attributeValue2 + " at " + xmlPullParser.getPositionDescription());
                        }
                    }
                    sharedUserSetting = sharedUserSettingAddSharedUserLPw;
                } else {
                    PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: shared-user " + attributeValue + " has bad userId " + attributeValue2 + " at " + xmlPullParser.getPositionDescription());
                }
            } catch (NumberFormatException e3) {
                attributeValue2 = null;
            }
        } catch (NumberFormatException e4) {
            attributeValue = null;
            attributeValue2 = null;
        }
        if (sharedUserSetting == null) {
            int depth = xmlPullParser.getDepth();
            while (true) {
                int next = xmlPullParser.next();
                if (next != 1) {
                    if (next != 3 || xmlPullParser.getDepth() > depth) {
                        if (next != 3 && next != 4) {
                            String name = xmlPullParser.getName();
                            if (name.equals("sigs")) {
                                sharedUserSetting.signatures.readXml(xmlPullParser, this.mPastSignatures);
                            } else if (name.equals(TAG_PERMISSIONS)) {
                                readInstallPermissionsLPr(xmlPullParser, sharedUserSetting.getPermissionsState());
                            } else {
                                PackageManagerService.reportSettingsProblem(5, "Unknown element under <shared-user>: " + xmlPullParser.getName());
                                XmlUtils.skipCurrentTag(xmlPullParser);
                            }
                        }
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }
        } else {
            XmlUtils.skipCurrentTag(xmlPullParser);
        }
    }

    void createNewUserLI(PackageManagerService packageManagerService, Installer installer, int i, String[] strArr) {
        int size;
        String[] strArr2;
        String[] strArr3;
        int[] iArr;
        String[] strArr4;
        int[] iArr2;
        int i2;
        int[] iArr3;
        synchronized (this.mPackages) {
            Collection<PackageSetting> collectionValues = this.mPackages.values();
            size = collectionValues.size();
            strArr2 = new String[size];
            strArr3 = new String[size];
            iArr = new int[size];
            strArr4 = new String[size];
            iArr2 = new int[size];
            Iterator<PackageSetting> it = collectionValues.iterator();
            for (int i3 = 0; i3 < size; i3++) {
                PackageSetting next = it.next();
                if (next.pkg != null && next.pkg.applicationInfo != null) {
                    boolean z = next.isSystem() && !ArrayUtils.contains(strArr, next.name);
                    next.setInstalled(z, i);
                    if (!z) {
                        writeKernelMappingLPr(next);
                    }
                    strArr2[i3] = next.volumeUuid;
                    strArr3[i3] = next.name;
                    iArr[i3] = next.appId;
                    strArr4[i3] = next.pkg.applicationInfo.seInfo;
                    iArr2[i3] = next.pkg.applicationInfo.targetSdkVersion;
                }
            }
        }
        int i4 = 0;
        while (i4 < size) {
            if (strArr3[i4] == null) {
                i2 = i4;
                iArr3 = iArr2;
            } else {
                try {
                    i2 = i4;
                    iArr3 = iArr2;
                    try {
                        installer.createAppData(strArr2[i4], strArr3[i4], i, 3, iArr[i4], strArr4[i4], iArr2[i4]);
                    } catch (Installer.InstallerException e) {
                        e = e;
                        Slog.w(TAG, "Failed to prepare app data", e);
                    }
                } catch (Installer.InstallerException e2) {
                    e = e2;
                    i2 = i4;
                    iArr3 = iArr2;
                }
            }
            i4 = i2 + 1;
            iArr2 = iArr3;
        }
        synchronized (this.mPackages) {
            applyDefaultPreferredAppsLPw(packageManagerService, i);
        }
    }

    void removeUserLPw(int i) {
        Iterator<Map.Entry<String, PackageSetting>> it = this.mPackages.entrySet().iterator();
        while (it.hasNext()) {
            it.next().getValue().removeUser(i);
        }
        this.mPreferredActivities.remove(i);
        getUserPackagesStateFile(i).delete();
        getUserPackagesStateBackupFile(i).delete();
        removeCrossProfileIntentFiltersLPw(i);
        this.mRuntimePermissionsPersistence.onUserRemovedLPw(i);
        writePackageListLPr();
        writeKernelRemoveUserLPr(i);
    }

    void removeCrossProfileIntentFiltersLPw(int i) {
        synchronized (this.mCrossProfileIntentResolvers) {
            if (this.mCrossProfileIntentResolvers.get(i) != null) {
                this.mCrossProfileIntentResolvers.remove(i);
                writePackageRestrictionsLPr(i);
            }
            int size = this.mCrossProfileIntentResolvers.size();
            for (int i2 = 0; i2 < size; i2++) {
                int iKeyAt = this.mCrossProfileIntentResolvers.keyAt(i2);
                CrossProfileIntentResolver crossProfileIntentResolver = this.mCrossProfileIntentResolvers.get(iKeyAt);
                boolean z = false;
                for (CrossProfileIntentFilter crossProfileIntentFilter : new ArraySet(crossProfileIntentResolver.filterSet())) {
                    if (crossProfileIntentFilter.getTargetUserId() == i) {
                        crossProfileIntentResolver.removeFilter(crossProfileIntentFilter);
                        z = true;
                    }
                }
                if (z) {
                    writePackageRestrictionsLPr(iKeyAt);
                }
            }
        }
    }

    private void setFirstAvailableUid(int i) {
        if (i > mFirstAvailableUid) {
            mFirstAvailableUid = i;
        }
    }

    private int newUserIdLPw(Object obj) {
        int size = this.mUserIds.size();
        for (int i = mFirstAvailableUid; i < size; i++) {
            if (this.mUserIds.get(i) == null) {
                this.mUserIds.set(i, obj);
                return 10000 + i;
            }
        }
        if (size > 9999) {
            return -1;
        }
        this.mUserIds.add(obj);
        return 10000 + size;
    }

    public VerifierDeviceIdentity getVerifierDeviceIdentityLPw() {
        if (this.mVerifierDeviceIdentity == null) {
            this.mVerifierDeviceIdentity = VerifierDeviceIdentity.generate();
            writeLPr();
        }
        return this.mVerifierDeviceIdentity;
    }

    boolean hasOtherDisabledSystemPkgWithChildLPr(String str, String str2) {
        int size = this.mDisabledSysPackages.size();
        for (int i = 0; i < size; i++) {
            PackageSetting packageSettingValueAt = this.mDisabledSysPackages.valueAt(i);
            if (packageSettingValueAt.childPackageNames != null && !packageSettingValueAt.childPackageNames.isEmpty() && !packageSettingValueAt.name.equals(str)) {
                int size2 = packageSettingValueAt.childPackageNames.size();
                for (int i2 = 0; i2 < size2; i2++) {
                    if (packageSettingValueAt.childPackageNames.get(i2).equals(str2)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public PackageSetting getDisabledSystemPkgLPr(String str) {
        return this.mDisabledSysPackages.get(str);
    }

    boolean isEnabledAndMatchLPr(ComponentInfo componentInfo, int i, int i2) {
        PackageSetting packageSetting = this.mPackages.get(componentInfo.packageName);
        if (packageSetting == null) {
            return false;
        }
        return packageSetting.readUserState(i2).isMatch(componentInfo, i);
    }

    String getInstallerPackageNameLPr(String str) {
        PackageSetting packageSetting = this.mPackages.get(str);
        if (packageSetting == null) {
            throw new IllegalArgumentException("Unknown package: " + str);
        }
        return packageSetting.installerPackageName;
    }

    boolean isOrphaned(String str) {
        PackageSetting packageSetting = this.mPackages.get(str);
        if (packageSetting == null) {
            throw new IllegalArgumentException("Unknown package: " + str);
        }
        return packageSetting.isOrphaned;
    }

    int getApplicationEnabledSettingLPr(String str, int i) {
        PackageSetting packageSetting = this.mPackages.get(str);
        if (packageSetting == null) {
            throw new IllegalArgumentException("Unknown package: " + str);
        }
        return packageSetting.getEnabled(i);
    }

    int getComponentEnabledSettingLPr(ComponentName componentName, int i) {
        PackageSetting packageSetting = this.mPackages.get(componentName.getPackageName());
        if (packageSetting == null) {
            throw new IllegalArgumentException("Unknown component: " + componentName);
        }
        return packageSetting.getCurrentEnabledStateLPr(componentName.getClassName(), i);
    }

    boolean wasPackageEverLaunchedLPr(String str, int i) {
        if (this.mPackages.get(str) == null) {
            throw new IllegalArgumentException("Unknown package: " + str);
        }
        return !r0.getNotLaunched(i);
    }

    boolean setPackageStoppedStateLPw(PackageManagerService packageManagerService, String str, boolean z, boolean z2, int i, int i2) {
        int appId = UserHandle.getAppId(i);
        PackageSetting packageSetting = this.mPackages.get(str);
        if (packageSetting == null) {
            throw new IllegalArgumentException("Unknown package: " + str);
        }
        if (!z2 && appId != packageSetting.appId) {
            throw new SecurityException("Permission Denial: attempt to change stopped state from pid=" + Binder.getCallingPid() + ", uid=" + i + ", package uid=" + packageSetting.appId);
        }
        if (packageSetting.getStopped(i2) == z) {
            return false;
        }
        packageSetting.setStopped(z, i2);
        if (packageSetting.getNotLaunched(i2)) {
            if (packageSetting.installerPackageName != null) {
                packageManagerService.notifyFirstLaunch(packageSetting.name, packageSetting.installerPackageName, i2);
            }
            packageSetting.setNotLaunched(false, i2);
            return true;
        }
        return true;
    }

    void setHarmfulAppWarningLPw(String str, CharSequence charSequence, int i) {
        PackageSetting packageSetting = this.mPackages.get(str);
        if (packageSetting == null) {
            throw new IllegalArgumentException("Unknown package: " + str);
        }
        packageSetting.setHarmfulAppWarning(i, charSequence == null ? null : charSequence.toString());
    }

    String getHarmfulAppWarningLPr(String str, int i) {
        PackageSetting packageSetting = this.mPackages.get(str);
        if (packageSetting == null) {
            throw new IllegalArgumentException("Unknown package: " + str);
        }
        return packageSetting.getHarmfulAppWarning(i);
    }

    private static List<UserInfo> getAllUsers(UserManagerService userManagerService) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            List<UserInfo> users = userManagerService.getUsers(false);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return users;
        } catch (NullPointerException e) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return null;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    List<PackageSetting> getVolumePackagesLPr(String str) {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < this.mPackages.size(); i++) {
            PackageSetting packageSettingValueAt = this.mPackages.valueAt(i);
            if (Objects.equals(str, packageSettingValueAt.volumeUuid)) {
                arrayList.add(packageSettingValueAt);
            }
        }
        return arrayList;
    }

    static void printFlags(PrintWriter printWriter, int i, Object[] objArr) {
        printWriter.print("[ ");
        for (int i2 = 0; i2 < objArr.length; i2 += 2) {
            if ((((Integer) objArr[i2]).intValue() & i) != 0) {
                printWriter.print(objArr[i2 + 1]);
                printWriter.print(" ");
            }
        }
        printWriter.print("]");
    }

    void dumpVersionLPr(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.increaseIndent();
        for (int i = 0; i < this.mVersion.size(); i++) {
            String strKeyAt = this.mVersion.keyAt(i);
            VersionInfo versionInfoValueAt = this.mVersion.valueAt(i);
            if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, strKeyAt)) {
                indentingPrintWriter.println("Internal:");
            } else if (Objects.equals("primary_physical", strKeyAt)) {
                indentingPrintWriter.println("External:");
            } else {
                indentingPrintWriter.println("UUID " + strKeyAt + ":");
            }
            indentingPrintWriter.increaseIndent();
            indentingPrintWriter.printPair(ATTR_SDK_VERSION, Integer.valueOf(versionInfoValueAt.sdkVersion));
            indentingPrintWriter.printPair(ATTR_DATABASE_VERSION, Integer.valueOf(versionInfoValueAt.databaseVersion));
            indentingPrintWriter.println();
            indentingPrintWriter.printPair(ATTR_FINGERPRINT, versionInfoValueAt.fingerprint);
            indentingPrintWriter.println();
            indentingPrintWriter.decreaseIndent();
        }
        indentingPrintWriter.decreaseIndent();
    }

    void dumpPackageLPr(PrintWriter printWriter, String str, String str2, ArraySet<String> arraySet, PackageSetting packageSetting, SimpleDateFormat simpleDateFormat, Date date, List<UserInfo> list, boolean z) {
        boolean z2;
        if (str2 != null) {
            printWriter.print(str2);
            printWriter.print(",");
            printWriter.print(packageSetting.realName != null ? packageSetting.realName : packageSetting.name);
            printWriter.print(",");
            printWriter.print(packageSetting.appId);
            printWriter.print(",");
            printWriter.print(packageSetting.versionCode);
            printWriter.print(",");
            printWriter.print(packageSetting.firstInstallTime);
            printWriter.print(",");
            printWriter.print(packageSetting.lastUpdateTime);
            printWriter.print(",");
            printWriter.print(packageSetting.installerPackageName != null ? packageSetting.installerPackageName : "?");
            printWriter.println();
            if (packageSetting.pkg != null) {
                printWriter.print(str2);
                printWriter.print("-");
                printWriter.print("splt,");
                printWriter.print("base,");
                printWriter.println(packageSetting.pkg.baseRevisionCode);
                if (packageSetting.pkg.splitNames != null) {
                    for (int i = 0; i < packageSetting.pkg.splitNames.length; i++) {
                        printWriter.print(str2);
                        printWriter.print("-");
                        printWriter.print("splt,");
                        printWriter.print(packageSetting.pkg.splitNames[i]);
                        printWriter.print(",");
                        printWriter.println(packageSetting.pkg.splitRevisionCodes[i]);
                    }
                }
            }
            for (UserInfo userInfo : list) {
                printWriter.print(str2);
                printWriter.print("-");
                printWriter.print("usr");
                printWriter.print(",");
                printWriter.print(userInfo.id);
                printWriter.print(",");
                printWriter.print(packageSetting.getInstalled(userInfo.id) ? "I" : "i");
                printWriter.print(packageSetting.getHidden(userInfo.id) ? "B" : "b");
                printWriter.print(packageSetting.getSuspended(userInfo.id) ? "SU" : "su");
                printWriter.print(packageSetting.getStopped(userInfo.id) ? "S" : "s");
                printWriter.print(packageSetting.getNotLaunched(userInfo.id) ? "l" : "L");
                printWriter.print(packageSetting.getInstantApp(userInfo.id) ? "IA" : "ia");
                printWriter.print(packageSetting.getVirtulalPreload(userInfo.id) ? "VPI" : "vpi");
                printWriter.print(packageSetting.getHarmfulAppWarning(userInfo.id) != null ? "HA" : "ha");
                printWriter.print(",");
                printWriter.print(packageSetting.getEnabled(userInfo.id));
                String lastDisabledAppCaller = packageSetting.getLastDisabledAppCaller(userInfo.id);
                printWriter.print(",");
                if (lastDisabledAppCaller == null) {
                    lastDisabledAppCaller = "?";
                }
                printWriter.print(lastDisabledAppCaller);
                printWriter.print(",");
                printWriter.println();
            }
            return;
        }
        printWriter.print(str);
        printWriter.print("Package [");
        printWriter.print(packageSetting.realName != null ? packageSetting.realName : packageSetting.name);
        printWriter.print("] (");
        printWriter.print(Integer.toHexString(System.identityHashCode(packageSetting)));
        printWriter.println("):");
        if (packageSetting.realName != null) {
            printWriter.print(str);
            printWriter.print("  compat name=");
            printWriter.println(packageSetting.name);
        }
        printWriter.print(str);
        printWriter.print("  userId=");
        printWriter.println(packageSetting.appId);
        if (packageSetting.sharedUser != null) {
            printWriter.print(str);
            printWriter.print("  sharedUser=");
            printWriter.println(packageSetting.sharedUser);
        }
        printWriter.print(str);
        printWriter.print("  pkg=");
        printWriter.println(packageSetting.pkg);
        printWriter.print(str);
        printWriter.print("  codePath=");
        printWriter.println(packageSetting.codePathString);
        if (arraySet == null) {
            printWriter.print(str);
            printWriter.print("  resourcePath=");
            printWriter.println(packageSetting.resourcePathString);
            printWriter.print(str);
            printWriter.print("  legacyNativeLibraryDir=");
            printWriter.println(packageSetting.legacyNativeLibraryPathString);
            printWriter.print(str);
            printWriter.print("  primaryCpuAbi=");
            printWriter.println(packageSetting.primaryCpuAbiString);
            printWriter.print(str);
            printWriter.print("  secondaryCpuAbi=");
            printWriter.println(packageSetting.secondaryCpuAbiString);
        }
        printWriter.print(str);
        printWriter.print("  versionCode=");
        printWriter.print(packageSetting.versionCode);
        if (packageSetting.pkg != null) {
            printWriter.print(" minSdk=");
            printWriter.print(packageSetting.pkg.applicationInfo.minSdkVersion);
            printWriter.print(" targetSdk=");
            printWriter.print(packageSetting.pkg.applicationInfo.targetSdkVersion);
        }
        printWriter.println();
        if (packageSetting.pkg != null) {
            if (packageSetting.pkg.parentPackage != null) {
                PackageParser.Package r0 = packageSetting.pkg.parentPackage;
                PackageSetting packageSetting2 = this.mPackages.get(r0.packageName);
                if (packageSetting2 == null || !packageSetting2.codePathString.equals(r0.codePath)) {
                    packageSetting2 = this.mDisabledSysPackages.get(r0.packageName);
                }
                if (packageSetting2 != null) {
                    printWriter.print(str);
                    printWriter.print("  parentPackage=");
                    printWriter.println(packageSetting2.realName != null ? packageSetting2.realName : packageSetting2.name);
                }
            } else if (packageSetting.pkg.childPackages != null) {
                printWriter.print(str);
                printWriter.print("  childPackages=[");
                int size = packageSetting.pkg.childPackages.size();
                for (int i2 = 0; i2 < size; i2++) {
                    PackageParser.Package r4 = (PackageParser.Package) packageSetting.pkg.childPackages.get(i2);
                    PackageSetting packageSetting3 = this.mPackages.get(r4.packageName);
                    if (packageSetting3 == null || !packageSetting3.codePathString.equals(r4.codePath)) {
                        packageSetting3 = this.mDisabledSysPackages.get(r4.packageName);
                    }
                    if (packageSetting3 != null) {
                        if (i2 > 0) {
                            printWriter.print(", ");
                        }
                        printWriter.print(packageSetting3.realName != null ? packageSetting3.realName : packageSetting3.name);
                    }
                }
                printWriter.println("]");
            }
            printWriter.print(str);
            printWriter.print("  versionName=");
            printWriter.println(packageSetting.pkg.mVersionName);
            printWriter.print(str);
            printWriter.print("  splits=");
            dumpSplitNames(printWriter, packageSetting.pkg);
            printWriter.println();
            int i3 = packageSetting.pkg.mSigningDetails.signatureSchemeVersion;
            printWriter.print(str);
            printWriter.print("  apkSigningVersion=");
            printWriter.println(i3);
            printWriter.print(str);
            printWriter.print("  applicationInfo=");
            printWriter.println(packageSetting.pkg.applicationInfo.toString());
            printWriter.print(str);
            printWriter.print("  flags=");
            printFlags(printWriter, packageSetting.pkg.applicationInfo.flags, FLAG_DUMP_SPEC);
            printWriter.println();
            if (packageSetting.pkg.applicationInfo.privateFlags != 0) {
                printWriter.print(str);
                printWriter.print("  privateFlags=");
                printFlags(printWriter, packageSetting.pkg.applicationInfo.privateFlags, PRIVATE_FLAG_DUMP_SPEC);
                printWriter.println();
            }
            printWriter.print(str);
            printWriter.print("  dataDir=");
            printWriter.println(packageSetting.pkg.applicationInfo.dataDir);
            printWriter.print(str);
            printWriter.print("  supportsScreens=[");
            if ((packageSetting.pkg.applicationInfo.flags & 512) != 0) {
                printWriter.print("small");
                z2 = false;
            } else {
                z2 = true;
            }
            if ((packageSetting.pkg.applicationInfo.flags & 1024) != 0) {
                if (!z2) {
                    printWriter.print(", ");
                }
                printWriter.print("medium");
                z2 = false;
            }
            if ((packageSetting.pkg.applicationInfo.flags & 2048) != 0) {
                if (!z2) {
                    printWriter.print(", ");
                }
                printWriter.print("large");
                z2 = false;
            }
            if ((packageSetting.pkg.applicationInfo.flags & DumpState.DUMP_FROZEN) != 0) {
                if (!z2) {
                    printWriter.print(", ");
                }
                printWriter.print("xlarge");
                z2 = false;
            }
            if ((packageSetting.pkg.applicationInfo.flags & 4096) != 0) {
                if (!z2) {
                    printWriter.print(", ");
                }
                printWriter.print("resizeable");
                z2 = false;
            }
            if ((packageSetting.pkg.applicationInfo.flags & 8192) != 0) {
                if (!z2) {
                    printWriter.print(", ");
                }
                printWriter.print("anyDensity");
            }
            printWriter.println("]");
            if (packageSetting.pkg.libraryNames != null && packageSetting.pkg.libraryNames.size() > 0) {
                printWriter.print(str);
                printWriter.println("  dynamic libraries:");
                for (int i4 = 0; i4 < packageSetting.pkg.libraryNames.size(); i4++) {
                    printWriter.print(str);
                    printWriter.print("    ");
                    printWriter.println((String) packageSetting.pkg.libraryNames.get(i4));
                }
            }
            if (packageSetting.pkg.staticSharedLibName != null) {
                printWriter.print(str);
                printWriter.println("  static library:");
                printWriter.print(str);
                printWriter.print("    ");
                printWriter.print("name:");
                printWriter.print(packageSetting.pkg.staticSharedLibName);
                printWriter.print(" version:");
                printWriter.println(packageSetting.pkg.staticSharedLibVersion);
            }
            if (packageSetting.pkg.usesLibraries != null && packageSetting.pkg.usesLibraries.size() > 0) {
                printWriter.print(str);
                printWriter.println("  usesLibraries:");
                for (int i5 = 0; i5 < packageSetting.pkg.usesLibraries.size(); i5++) {
                    printWriter.print(str);
                    printWriter.print("    ");
                    printWriter.println((String) packageSetting.pkg.usesLibraries.get(i5));
                }
            }
            if (packageSetting.pkg.usesStaticLibraries != null && packageSetting.pkg.usesStaticLibraries.size() > 0) {
                printWriter.print(str);
                printWriter.println("  usesStaticLibraries:");
                for (int i6 = 0; i6 < packageSetting.pkg.usesStaticLibraries.size(); i6++) {
                    printWriter.print(str);
                    printWriter.print("    ");
                    printWriter.print((String) packageSetting.pkg.usesStaticLibraries.get(i6));
                    printWriter.print(" version:");
                    printWriter.println(packageSetting.pkg.usesStaticLibrariesVersions[i6]);
                }
            }
            if (packageSetting.pkg.usesOptionalLibraries != null && packageSetting.pkg.usesOptionalLibraries.size() > 0) {
                printWriter.print(str);
                printWriter.println("  usesOptionalLibraries:");
                for (int i7 = 0; i7 < packageSetting.pkg.usesOptionalLibraries.size(); i7++) {
                    printWriter.print(str);
                    printWriter.print("    ");
                    printWriter.println((String) packageSetting.pkg.usesOptionalLibraries.get(i7));
                }
            }
            if (packageSetting.pkg.usesLibraryFiles != null && packageSetting.pkg.usesLibraryFiles.length > 0) {
                printWriter.print(str);
                printWriter.println("  usesLibraryFiles:");
                for (int i8 = 0; i8 < packageSetting.pkg.usesLibraryFiles.length; i8++) {
                    printWriter.print(str);
                    printWriter.print("    ");
                    printWriter.println(packageSetting.pkg.usesLibraryFiles[i8]);
                }
            }
        }
        printWriter.print(str);
        printWriter.print("  timeStamp=");
        date.setTime(packageSetting.timeStamp);
        printWriter.println(simpleDateFormat.format(date));
        printWriter.print(str);
        printWriter.print("  firstInstallTime=");
        date.setTime(packageSetting.firstInstallTime);
        printWriter.println(simpleDateFormat.format(date));
        printWriter.print(str);
        printWriter.print("  lastUpdateTime=");
        date.setTime(packageSetting.lastUpdateTime);
        printWriter.println(simpleDateFormat.format(date));
        if (packageSetting.installerPackageName != null) {
            printWriter.print(str);
            printWriter.print("  installerPackageName=");
            printWriter.println(packageSetting.installerPackageName);
        }
        if (packageSetting.volumeUuid != null) {
            printWriter.print(str);
            printWriter.print("  volumeUuid=");
            printWriter.println(packageSetting.volumeUuid);
        }
        printWriter.print(str);
        printWriter.print("  signatures=");
        printWriter.println(packageSetting.signatures);
        printWriter.print(str);
        printWriter.print("  installPermissionsFixed=");
        printWriter.print(packageSetting.installPermissionsFixed);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("  pkgFlags=");
        printFlags(printWriter, packageSetting.pkgFlags, FLAG_DUMP_SPEC);
        printWriter.println();
        if (packageSetting.pkg != null && packageSetting.pkg.mOverlayTarget != null) {
            printWriter.print(str);
            printWriter.print("  overlayTarget=");
            printWriter.println(packageSetting.pkg.mOverlayTarget);
            printWriter.print(str);
            printWriter.print("  overlayCategory=");
            printWriter.println(packageSetting.pkg.mOverlayCategory);
        }
        if (packageSetting.pkg != null && packageSetting.pkg.permissions != null && packageSetting.pkg.permissions.size() > 0) {
            ArrayList arrayList = packageSetting.pkg.permissions;
            printWriter.print(str);
            printWriter.println("  declared permissions:");
            for (int i9 = 0; i9 < arrayList.size(); i9++) {
                PackageParser.Permission permission = (PackageParser.Permission) arrayList.get(i9);
                if (arraySet == null || arraySet.contains(permission.info.name)) {
                    printWriter.print(str);
                    printWriter.print("    ");
                    printWriter.print(permission.info.name);
                    printWriter.print(": prot=");
                    printWriter.print(PermissionInfo.protectionToString(permission.info.protectionLevel));
                    if ((permission.info.flags & 1) != 0) {
                        printWriter.print(", COSTS_MONEY");
                    }
                    if ((permission.info.flags & 2) != 0) {
                        printWriter.print(", HIDDEN");
                    }
                    if ((permission.info.flags & 1073741824) != 0) {
                        printWriter.print(", INSTALLED");
                    }
                    printWriter.println();
                }
            }
        }
        if ((arraySet != null || z) && packageSetting.pkg != null && packageSetting.pkg.requestedPermissions != null && packageSetting.pkg.requestedPermissions.size() > 0) {
            ArrayList arrayList2 = packageSetting.pkg.requestedPermissions;
            printWriter.print(str);
            printWriter.println("  requested permissions:");
            for (int i10 = 0; i10 < arrayList2.size(); i10++) {
                String str3 = (String) arrayList2.get(i10);
                if (arraySet == null || arraySet.contains(str3)) {
                    printWriter.print(str);
                    printWriter.print("    ");
                    printWriter.println(str3);
                }
            }
        }
        if (packageSetting.sharedUser == null || arraySet != null || z) {
            dumpInstallPermissionsLPr(printWriter, str + "  ", arraySet, packageSetting.getPermissionsState());
        }
        for (UserInfo userInfo2 : list) {
            printWriter.print(str);
            printWriter.print("  User ");
            printWriter.print(userInfo2.id);
            printWriter.print(": ");
            printWriter.print("ceDataInode=");
            printWriter.print(packageSetting.getCeDataInode(userInfo2.id));
            printWriter.print(" installed=");
            printWriter.print(packageSetting.getInstalled(userInfo2.id));
            printWriter.print(" hidden=");
            printWriter.print(packageSetting.getHidden(userInfo2.id));
            printWriter.print(" suspended=");
            printWriter.print(packageSetting.getSuspended(userInfo2.id));
            if (packageSetting.getSuspended(userInfo2.id)) {
                PackageUserState userState = packageSetting.readUserState(userInfo2.id);
                printWriter.print(" suspendingPackage=");
                printWriter.print(userState.suspendingPackage);
                printWriter.print(" dialogMessage=");
                printWriter.print(userState.dialogMessage);
            }
            printWriter.print(" stopped=");
            printWriter.print(packageSetting.getStopped(userInfo2.id));
            printWriter.print(" notLaunched=");
            printWriter.print(packageSetting.getNotLaunched(userInfo2.id));
            printWriter.print(" enabled=");
            printWriter.print(packageSetting.getEnabled(userInfo2.id));
            printWriter.print(" instant=");
            printWriter.print(packageSetting.getInstantApp(userInfo2.id));
            printWriter.print(" virtual=");
            printWriter.println(packageSetting.getVirtulalPreload(userInfo2.id));
            String[] overlayPaths = packageSetting.getOverlayPaths(userInfo2.id);
            if (overlayPaths != null && overlayPaths.length > 0) {
                printWriter.print(str);
                printWriter.println("  overlay paths:");
                for (String str4 : overlayPaths) {
                    printWriter.print(str);
                    printWriter.print("    ");
                    printWriter.println(str4);
                }
            }
            String lastDisabledAppCaller2 = packageSetting.getLastDisabledAppCaller(userInfo2.id);
            if (lastDisabledAppCaller2 != null) {
                printWriter.print(str);
                printWriter.print("    lastDisabledCaller: ");
                printWriter.println(lastDisabledAppCaller2);
            }
            if (packageSetting.sharedUser == null) {
                PermissionsState permissionsState = packageSetting.getPermissionsState();
                dumpGidsLPr(printWriter, str + "    ", permissionsState.computeGids(userInfo2.id));
                dumpRuntimePermissionsLPr(printWriter, str + "    ", arraySet, permissionsState.getRuntimePermissionStates(userInfo2.id), z);
            }
            String harmfulAppWarning = packageSetting.getHarmfulAppWarning(userInfo2.id);
            if (harmfulAppWarning != null) {
                printWriter.print(str);
                printWriter.print("      harmfulAppWarning: ");
                printWriter.println(harmfulAppWarning);
            }
            if (arraySet == null) {
                ArraySet<String> disabledComponents = packageSetting.getDisabledComponents(userInfo2.id);
                if (disabledComponents != null && disabledComponents.size() > 0) {
                    printWriter.print(str);
                    printWriter.println("    disabledComponents:");
                    for (String str5 : disabledComponents) {
                        printWriter.print(str);
                        printWriter.print("      ");
                        printWriter.println(str5);
                    }
                }
                ArraySet<String> enabledComponents = packageSetting.getEnabledComponents(userInfo2.id);
                if (enabledComponents != null && enabledComponents.size() > 0) {
                    printWriter.print(str);
                    printWriter.println("    enabledComponents:");
                    for (String str6 : enabledComponents) {
                        printWriter.print(str);
                        printWriter.print("      ");
                        printWriter.println(str6);
                    }
                }
            }
        }
    }

    void dumpPackagesLPr(PrintWriter printWriter, String str, ArraySet<String> arraySet, DumpState dumpState, boolean z) {
        boolean z2;
        boolean z3;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        List<UserInfo> allUsers = getAllUsers(UserManagerService.getInstance());
        Iterator<PackageSetting> it = this.mPackages.values().iterator();
        boolean z4 = false;
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            PackageSetting next = it.next();
            if (str == null || str.equals(next.realName) || str.equals(next.name)) {
                if (arraySet == null || next.getPermissionsState().hasRequestedPermission(arraySet)) {
                    if (!z && str != null) {
                        dumpState.setSharedUser(next.sharedUser);
                    }
                    if (z || z4) {
                        z3 = z4;
                    } else {
                        if (dumpState.onTitlePrinted()) {
                            printWriter.println();
                        }
                        printWriter.println("Packages:");
                        z3 = true;
                    }
                    dumpPackageLPr(printWriter, "  ", z ? TAG_PACKAGE : null, arraySet, next, simpleDateFormat, date, allUsers, str != null);
                    z4 = z3;
                    it = it;
                    date = date;
                }
            }
        }
        Date date2 = date;
        if (this.mRenamedPackages.size() > 0 && arraySet == null) {
            boolean z5 = false;
            for (Map.Entry<String, String> entry : this.mRenamedPackages.entrySet()) {
                if (str == null || str.equals(entry.getKey()) || str.equals(entry.getValue())) {
                    if (!z) {
                        if (!z5) {
                            if (dumpState.onTitlePrinted()) {
                                printWriter.println();
                            }
                            printWriter.println("Renamed packages:");
                            z5 = true;
                        }
                        printWriter.print("  ");
                    } else {
                        printWriter.print("ren,");
                    }
                    printWriter.print(entry.getKey());
                    printWriter.print(z ? " -> " : ",");
                    printWriter.println(entry.getValue());
                }
            }
        }
        if (this.mDisabledSysPackages.size() > 0 && arraySet == null) {
            Iterator<PackageSetting> it2 = this.mDisabledSysPackages.values().iterator();
            boolean z6 = false;
            while (it2.hasNext()) {
                PackageSetting next2 = it2.next();
                if (str == null || str.equals(next2.realName) || str.equals(next2.name)) {
                    if (z || z6) {
                        z2 = z6;
                    } else {
                        if (dumpState.onTitlePrinted()) {
                            printWriter.println();
                        }
                        printWriter.println("Hidden system packages:");
                        z2 = true;
                    }
                    dumpPackageLPr(printWriter, "  ", z ? "dis" : null, arraySet, next2, simpleDateFormat, date2, allUsers, str != null);
                    z6 = z2;
                    it2 = it2;
                }
            }
        }
    }

    void dumpPackagesProto(ProtoOutputStream protoOutputStream) {
        List<UserInfo> allUsers = getAllUsers(UserManagerService.getInstance());
        int size = this.mPackages.size();
        for (int i = 0; i < size; i++) {
            this.mPackages.valueAt(i).writeToProto(protoOutputStream, 2246267895813L, allUsers);
        }
    }

    void dumpPermissionsLPr(PrintWriter printWriter, String str, ArraySet<String> arraySet, DumpState dumpState) {
        this.mPermissions.dumpPermissions(printWriter, str, arraySet, this.mReadExternalStorageEnforced == Boolean.TRUE, dumpState);
    }

    void dumpSharedUsersLPr(PrintWriter printWriter, String str, ArraySet<String> arraySet, DumpState dumpState, boolean z) {
        boolean z2;
        int i;
        int i2;
        int[] iArr;
        PermissionsState permissionsState;
        boolean z3 = false;
        for (SharedUserSetting sharedUserSetting : this.mSharedUsers.values()) {
            if (str == null || sharedUserSetting == dumpState.getSharedUser()) {
                if (arraySet == null || sharedUserSetting.getPermissionsState().hasRequestedPermission(arraySet)) {
                    if (!z) {
                        if (z3) {
                            z2 = z3;
                        } else {
                            if (dumpState.onTitlePrinted()) {
                                printWriter.println();
                            }
                            printWriter.println("Shared users:");
                            z2 = true;
                        }
                        printWriter.print("  SharedUser [");
                        printWriter.print(sharedUserSetting.name);
                        printWriter.print("] (");
                        printWriter.print(Integer.toHexString(System.identityHashCode(sharedUserSetting)));
                        printWriter.println("):");
                        printWriter.print("    ");
                        printWriter.print("userId=");
                        printWriter.println(sharedUserSetting.userId);
                        PermissionsState permissionsState2 = sharedUserSetting.getPermissionsState();
                        dumpInstallPermissionsLPr(printWriter, "    ", arraySet, permissionsState2);
                        int[] userIds = UserManagerService.getInstance().getUserIds();
                        int length = userIds.length;
                        int i3 = 0;
                        while (i3 < length) {
                            int i4 = userIds[i3];
                            int[] iArrComputeGids = permissionsState2.computeGids(i4);
                            List<PermissionsState.PermissionState> runtimePermissionStates = permissionsState2.getRuntimePermissionStates(i4);
                            if (ArrayUtils.isEmpty(iArrComputeGids) && runtimePermissionStates.isEmpty()) {
                                i = i3;
                                i2 = length;
                                iArr = userIds;
                                permissionsState = permissionsState2;
                            } else {
                                printWriter.print("    ");
                                printWriter.print("User ");
                                printWriter.print(i4);
                                printWriter.println(": ");
                                dumpGidsLPr(printWriter, "      ", iArrComputeGids);
                                i = i3;
                                i2 = length;
                                iArr = userIds;
                                permissionsState = permissionsState2;
                                dumpRuntimePermissionsLPr(printWriter, "      ", arraySet, runtimePermissionStates, str != null);
                            }
                            i3 = i + 1;
                            permissionsState2 = permissionsState;
                            length = i2;
                            userIds = iArr;
                        }
                        z3 = z2;
                    } else {
                        printWriter.print("suid,");
                        printWriter.print(sharedUserSetting.userId);
                        printWriter.print(",");
                        printWriter.println(sharedUserSetting.name);
                    }
                }
            }
        }
    }

    void dumpSharedUsersProto(ProtoOutputStream protoOutputStream) {
        int size = this.mSharedUsers.size();
        for (int i = 0; i < size; i++) {
            this.mSharedUsers.valueAt(i).writeToProto(protoOutputStream, 2246267895814L);
        }
    }

    void dumpReadMessagesLPr(PrintWriter printWriter, DumpState dumpState) {
        printWriter.println("Settings parse messages:");
        printWriter.print(this.mReadMessages.toString());
    }

    void dumpRestoredPermissionGrantsLPr(PrintWriter printWriter, DumpState dumpState) {
        if (this.mRestoredUserGrants.size() > 0) {
            printWriter.println();
            printWriter.println("Restored (pending) permission grants:");
            for (int i = 0; i < this.mRestoredUserGrants.size(); i++) {
                ArrayMap<String, ArraySet<RestoredPermissionGrant>> arrayMapValueAt = this.mRestoredUserGrants.valueAt(i);
                if (arrayMapValueAt != null && arrayMapValueAt.size() > 0) {
                    int iKeyAt = this.mRestoredUserGrants.keyAt(i);
                    printWriter.print("  User ");
                    printWriter.println(iKeyAt);
                    for (int i2 = 0; i2 < arrayMapValueAt.size(); i2++) {
                        ArraySet<RestoredPermissionGrant> arraySetValueAt = arrayMapValueAt.valueAt(i2);
                        if (arraySetValueAt != null && arraySetValueAt.size() > 0) {
                            String strKeyAt = arrayMapValueAt.keyAt(i2);
                            printWriter.print("    ");
                            printWriter.print(strKeyAt);
                            printWriter.println(" :");
                            for (RestoredPermissionGrant restoredPermissionGrant : arraySetValueAt) {
                                printWriter.print("      ");
                                printWriter.print(restoredPermissionGrant.permissionName);
                                if (restoredPermissionGrant.granted) {
                                    printWriter.print(" GRANTED");
                                }
                                if ((restoredPermissionGrant.grantBits & 1) != 0) {
                                    printWriter.print(" user_set");
                                }
                                if ((restoredPermissionGrant.grantBits & 2) != 0) {
                                    printWriter.print(" user_fixed");
                                }
                                if ((restoredPermissionGrant.grantBits & 8) != 0) {
                                    printWriter.print(" revoke_on_upgrade");
                                }
                                printWriter.println();
                            }
                        }
                    }
                }
            }
            printWriter.println();
        }
    }

    private static void dumpSplitNames(PrintWriter printWriter, PackageParser.Package r3) {
        if (r3 == null) {
            printWriter.print(UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN);
            return;
        }
        printWriter.print("[");
        printWriter.print("base");
        if (r3.baseRevisionCode != 0) {
            printWriter.print(":");
            printWriter.print(r3.baseRevisionCode);
        }
        if (r3.splitNames != null) {
            for (int i = 0; i < r3.splitNames.length; i++) {
                printWriter.print(", ");
                printWriter.print(r3.splitNames[i]);
                if (r3.splitRevisionCodes[i] != 0) {
                    printWriter.print(":");
                    printWriter.print(r3.splitRevisionCodes[i]);
                }
            }
        }
        printWriter.print("]");
    }

    void dumpGidsLPr(PrintWriter printWriter, String str, int[] iArr) {
        if (!ArrayUtils.isEmpty(iArr)) {
            printWriter.print(str);
            printWriter.print("gids=");
            printWriter.println(PackageManagerService.arrayToString(iArr));
        }
    }

    void dumpRuntimePermissionsLPr(PrintWriter printWriter, String str, ArraySet<String> arraySet, List<PermissionsState.PermissionState> list, boolean z) {
        if (!list.isEmpty() || z) {
            printWriter.print(str);
            printWriter.println("runtime permissions:");
            for (PermissionsState.PermissionState permissionState : list) {
                if (arraySet == null || arraySet.contains(permissionState.getName())) {
                    printWriter.print(str);
                    printWriter.print("  ");
                    printWriter.print(permissionState.getName());
                    printWriter.print(": granted=");
                    printWriter.print(permissionState.isGranted());
                    printWriter.println(permissionFlagsToString(", flags=", permissionState.getFlags()));
                }
            }
        }
    }

    private static String permissionFlagsToString(String str, int i) {
        StringBuilder sb = null;
        while (i != 0) {
            if (sb == null) {
                sb = new StringBuilder();
                sb.append(str);
                sb.append("[ ");
            }
            int iNumberOfTrailingZeros = 1 << Integer.numberOfTrailingZeros(i);
            i &= ~iNumberOfTrailingZeros;
            sb.append(PackageManager.permissionFlagToString(iNumberOfTrailingZeros));
            sb.append(' ');
        }
        if (sb != null) {
            sb.append(']');
            return sb.toString();
        }
        return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    }

    void dumpInstallPermissionsLPr(PrintWriter printWriter, String str, ArraySet<String> arraySet, PermissionsState permissionsState) {
        List<PermissionsState.PermissionState> installPermissionStates = permissionsState.getInstallPermissionStates();
        if (!installPermissionStates.isEmpty()) {
            printWriter.print(str);
            printWriter.println("install permissions:");
            for (PermissionsState.PermissionState permissionState : installPermissionStates) {
                if (arraySet == null || arraySet.contains(permissionState.getName())) {
                    printWriter.print(str);
                    printWriter.print("  ");
                    printWriter.print(permissionState.getName());
                    printWriter.print(": granted=");
                    printWriter.print(permissionState.isGranted());
                    printWriter.println(permissionFlagsToString(", flags=", permissionState.getFlags()));
                }
            }
        }
    }

    public void writeRuntimePermissionsForUserLPr(int i, boolean z) {
        if (z) {
            this.mRuntimePermissionsPersistence.writePermissionsForUserSyncLPr(i);
        } else {
            this.mRuntimePermissionsPersistence.writePermissionsForUserAsyncLPr(i);
        }
    }

    private final class RuntimePermissionPersistence {
        private static final long MAX_WRITE_PERMISSIONS_DELAY_MILLIS = 2000;
        private static final long WRITE_PERMISSIONS_DELAY_MILLIS = 200;
        private final Object mPersistenceLock;
        private final Handler mHandler = new MyHandler();

        @GuardedBy("mLock")
        private final SparseBooleanArray mWriteScheduled = new SparseBooleanArray();

        @GuardedBy("mLock")
        private final SparseLongArray mLastNotWrittenMutationTimesMillis = new SparseLongArray();

        @GuardedBy("mLock")
        private final SparseArray<String> mFingerprints = new SparseArray<>();

        @GuardedBy("mLock")
        private final SparseBooleanArray mDefaultPermissionsGranted = new SparseBooleanArray();

        public RuntimePermissionPersistence(Object obj) {
            this.mPersistenceLock = obj;
        }

        public boolean areDefaultRuntimPermissionsGrantedLPr(int i) {
            return this.mDefaultPermissionsGranted.get(i);
        }

        public void onDefaultRuntimePermissionsGrantedLPr(int i) {
            this.mFingerprints.put(i, Build.FINGERPRINT);
            writePermissionsForUserAsyncLPr(i);
        }

        public void writePermissionsForUserSyncLPr(int i) throws Throwable {
            this.mHandler.removeMessages(i);
            writePermissionsSync(i);
        }

        public void writePermissionsForUserAsyncLPr(int i) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            if (this.mWriteScheduled.get(i)) {
                this.mHandler.removeMessages(i);
                long j = this.mLastNotWrittenMutationTimesMillis.get(i);
                if (jUptimeMillis - j >= MAX_WRITE_PERMISSIONS_DELAY_MILLIS) {
                    this.mHandler.obtainMessage(i).sendToTarget();
                    return;
                }
                long jMin = Math.min(WRITE_PERMISSIONS_DELAY_MILLIS, Math.max((j + MAX_WRITE_PERMISSIONS_DELAY_MILLIS) - jUptimeMillis, 0L));
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(i), jMin);
                return;
            }
            this.mLastNotWrittenMutationTimesMillis.put(i, jUptimeMillis);
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(i), WRITE_PERMISSIONS_DELAY_MILLIS);
            this.mWriteScheduled.put(i, true);
        }

        private void writePermissionsSync(int i) throws Throwable {
            FileOutputStream fileOutputStreamStartWrite;
            ArrayMap arrayMap;
            AtomicFile atomicFile = new AtomicFile(Settings.this.getUserRuntimePermissionsFile(i), "package-perms-" + i);
            ArrayMap arrayMap2 = new ArrayMap();
            ArrayMap arrayMap3 = new ArrayMap();
            synchronized (this.mPersistenceLock) {
                this.mWriteScheduled.delete(i);
                int size = Settings.this.mPackages.size();
                for (int i2 = 0; i2 < size; i2++) {
                    String strKeyAt = Settings.this.mPackages.keyAt(i2);
                    PackageSetting packageSettingValueAt = Settings.this.mPackages.valueAt(i2);
                    if (packageSettingValueAt.sharedUser == null) {
                        List<PermissionsState.PermissionState> runtimePermissionStates = packageSettingValueAt.getPermissionsState().getRuntimePermissionStates(i);
                        if (!runtimePermissionStates.isEmpty()) {
                            arrayMap2.put(strKeyAt, runtimePermissionStates);
                        }
                    }
                }
                int size2 = Settings.this.mSharedUsers.size();
                for (int i3 = 0; i3 < size2; i3++) {
                    String strKeyAt2 = Settings.this.mSharedUsers.keyAt(i3);
                    List<PermissionsState.PermissionState> runtimePermissionStates2 = Settings.this.mSharedUsers.valueAt(i3).getPermissionsState().getRuntimePermissionStates(i);
                    if (!runtimePermissionStates2.isEmpty()) {
                        arrayMap3.put(strKeyAt2, runtimePermissionStates2);
                    }
                }
            }
            FileOutputStream fileOutputStream = null;
            try {
                try {
                    fileOutputStreamStartWrite = atomicFile.startWrite();
                } catch (Throwable th) {
                    th = th;
                    fileOutputStreamStartWrite = fileOutputStream;
                }
            } catch (Throwable th2) {
                th = th2;
            }
            try {
                XmlSerializer xmlSerializerNewSerializer = Xml.newSerializer();
                xmlSerializerNewSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
                xmlSerializerNewSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                xmlSerializerNewSerializer.startDocument(null, true);
                xmlSerializerNewSerializer.startTag(null, Settings.TAG_RUNTIME_PERMISSIONS);
                String str = this.mFingerprints.get(i);
                if (str != null) {
                    xmlSerializerNewSerializer.attribute(null, Settings.ATTR_FINGERPRINT, str);
                }
                int size3 = arrayMap2.size();
                for (int i4 = 0; i4 < size3; i4++) {
                    String str2 = (String) arrayMap2.keyAt(i4);
                    List<PermissionsState.PermissionState> list = (List) arrayMap2.valueAt(i4);
                    xmlSerializerNewSerializer.startTag(null, Settings.TAG_PACKAGE);
                    xmlSerializerNewSerializer.attribute(null, Settings.ATTR_NAME, str2);
                    writePermissions(xmlSerializerNewSerializer, list);
                    xmlSerializerNewSerializer.endTag(null, Settings.TAG_PACKAGE);
                }
                int size4 = arrayMap3.size();
                for (int i5 = 0; i5 < size4; i5++) {
                    String str3 = (String) arrayMap3.keyAt(i5);
                    List<PermissionsState.PermissionState> list2 = (List) arrayMap3.valueAt(i5);
                    xmlSerializerNewSerializer.startTag(null, Settings.TAG_SHARED_USER);
                    xmlSerializerNewSerializer.attribute(null, Settings.ATTR_NAME, str3);
                    writePermissions(xmlSerializerNewSerializer, list2);
                    xmlSerializerNewSerializer.endTag(null, Settings.TAG_SHARED_USER);
                }
                xmlSerializerNewSerializer.endTag(null, Settings.TAG_RUNTIME_PERMISSIONS);
                if (Settings.this.mRestoredUserGrants.get(i) != null && (arrayMap = (ArrayMap) Settings.this.mRestoredUserGrants.get(i)) != null) {
                    int size5 = arrayMap.size();
                    for (int i6 = 0; i6 < size5; i6++) {
                        ArraySet arraySet = (ArraySet) arrayMap.valueAt(i6);
                        if (arraySet != null && arraySet.size() > 0) {
                            String str4 = (String) arrayMap.keyAt(i6);
                            xmlSerializerNewSerializer.startTag(null, Settings.TAG_RESTORED_RUNTIME_PERMISSIONS);
                            xmlSerializerNewSerializer.attribute(null, "packageName", str4);
                            int size6 = arraySet.size();
                            for (int i7 = 0; i7 < size6; i7++) {
                                RestoredPermissionGrant restoredPermissionGrant = (RestoredPermissionGrant) arraySet.valueAt(i7);
                                xmlSerializerNewSerializer.startTag(null, Settings.TAG_PERMISSION_ENTRY);
                                xmlSerializerNewSerializer.attribute(null, Settings.ATTR_NAME, restoredPermissionGrant.permissionName);
                                if (restoredPermissionGrant.granted) {
                                    xmlSerializerNewSerializer.attribute(null, Settings.ATTR_GRANTED, "true");
                                }
                                if ((restoredPermissionGrant.grantBits & 1) != 0) {
                                    xmlSerializerNewSerializer.attribute(null, Settings.ATTR_USER_SET, "true");
                                }
                                if ((restoredPermissionGrant.grantBits & 2) != 0) {
                                    xmlSerializerNewSerializer.attribute(null, Settings.ATTR_USER_FIXED, "true");
                                }
                                if ((restoredPermissionGrant.grantBits & 8) != 0) {
                                    xmlSerializerNewSerializer.attribute(null, Settings.ATTR_REVOKE_ON_UPGRADE, "true");
                                }
                                xmlSerializerNewSerializer.endTag(null, Settings.TAG_PERMISSION_ENTRY);
                            }
                            xmlSerializerNewSerializer.endTag(null, Settings.TAG_RESTORED_RUNTIME_PERMISSIONS);
                        }
                    }
                }
                xmlSerializerNewSerializer.endDocument();
                atomicFile.finishWrite(fileOutputStreamStartWrite);
                if (Build.FINGERPRINT.equals(str)) {
                    this.mDefaultPermissionsGranted.put(i, true);
                }
                IoUtils.closeQuietly(fileOutputStreamStartWrite);
            } catch (Throwable th3) {
                th = th3;
                fileOutputStream = fileOutputStreamStartWrite;
                Slog.wtf("PackageManager", "Failed to write settings, restoring backup", th);
                atomicFile.failWrite(fileOutputStream);
                IoUtils.closeQuietly(fileOutputStream);
            }
        }

        private void onUserRemovedLPw(int i) {
            this.mHandler.removeMessages(i);
            Iterator<PackageSetting> it = Settings.this.mPackages.values().iterator();
            while (it.hasNext()) {
                revokeRuntimePermissionsAndClearFlags(it.next(), i);
            }
            Iterator<SharedUserSetting> it2 = Settings.this.mSharedUsers.values().iterator();
            while (it2.hasNext()) {
                revokeRuntimePermissionsAndClearFlags(it2.next(), i);
            }
            this.mDefaultPermissionsGranted.delete(i);
            this.mFingerprints.remove(i);
        }

        private void revokeRuntimePermissionsAndClearFlags(SettingBase settingBase, int i) {
            PermissionsState permissionsState = settingBase.getPermissionsState();
            Iterator<PermissionsState.PermissionState> it = permissionsState.getRuntimePermissionStates(i).iterator();
            while (it.hasNext()) {
                BasePermission permission = Settings.this.mPermissions.getPermission(it.next().getName());
                if (permission != null) {
                    permissionsState.revokeRuntimePermission(permission, i);
                    permissionsState.updatePermissionFlags(permission, i, 255, 0);
                }
            }
        }

        public void deleteUserRuntimePermissionsFile(int i) {
            Settings.this.getUserRuntimePermissionsFile(i).delete();
        }

        public void readStateForUserSyncLPr(int i) {
            File userRuntimePermissionsFile = Settings.this.getUserRuntimePermissionsFile(i);
            if (!userRuntimePermissionsFile.exists()) {
                return;
            }
            try {
                FileInputStream fileInputStreamOpenRead = new AtomicFile(userRuntimePermissionsFile).openRead();
                try {
                    try {
                        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                        xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, null);
                        parseRuntimePermissionsLPr(xmlPullParserNewPullParser, i);
                    } catch (IOException | XmlPullParserException e) {
                        throw new IllegalStateException("Failed parsing permissions file: " + userRuntimePermissionsFile, e);
                    }
                } finally {
                    IoUtils.closeQuietly(fileInputStreamOpenRead);
                }
            } catch (FileNotFoundException e2) {
                Slog.i("PackageManager", "No permissions state");
            }
        }

        public void rememberRestoredUserGrantLPr(String str, String str2, boolean z, int i, int i2) {
            ArrayMap arrayMap = (ArrayMap) Settings.this.mRestoredUserGrants.get(i2);
            if (arrayMap == null) {
                arrayMap = new ArrayMap();
                Settings.this.mRestoredUserGrants.put(i2, arrayMap);
            }
            ArraySet arraySet = (ArraySet) arrayMap.get(str);
            if (arraySet == null) {
                arraySet = new ArraySet();
                arrayMap.put(str, arraySet);
            }
            arraySet.add(Settings.this.new RestoredPermissionGrant(str2, z, i));
        }

        private void parseRuntimePermissionsLPr(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
            int depth = xmlPullParser.getDepth();
            while (true) {
                int next = xmlPullParser.next();
                byte b = 1;
                if (next != 1) {
                    if (next != 3 || xmlPullParser.getDepth() > depth) {
                        if (next != 3 && next != 4) {
                            String name = xmlPullParser.getName();
                            int iHashCode = name.hashCode();
                            if (iHashCode == -2044791156) {
                                if (name.equals(Settings.TAG_RESTORED_RUNTIME_PERMISSIONS)) {
                                    b = 3;
                                }
                                switch (b) {
                                }
                            } else if (iHashCode == 111052) {
                                if (!name.equals(Settings.TAG_PACKAGE)) {
                                }
                                switch (b) {
                                }
                            } else if (iHashCode != 160289295) {
                                b = (iHashCode == 485578803 && name.equals(Settings.TAG_SHARED_USER)) ? (byte) 2 : (byte) -1;
                                switch (b) {
                                    case 0:
                                        String attributeValue = xmlPullParser.getAttributeValue(null, Settings.ATTR_FINGERPRINT);
                                        this.mFingerprints.put(i, attributeValue);
                                        this.mDefaultPermissionsGranted.put(i, Build.FINGERPRINT.equals(attributeValue));
                                        break;
                                    case 1:
                                        String attributeValue2 = xmlPullParser.getAttributeValue(null, Settings.ATTR_NAME);
                                        PackageSetting packageSetting = Settings.this.mPackages.get(attributeValue2);
                                        if (packageSetting == null) {
                                            Slog.w("PackageManager", "Unknown package:" + attributeValue2);
                                            XmlUtils.skipCurrentTag(xmlPullParser);
                                        } else {
                                            parsePermissionsLPr(xmlPullParser, packageSetting.getPermissionsState(), i);
                                        }
                                        break;
                                    case 2:
                                        String attributeValue3 = xmlPullParser.getAttributeValue(null, Settings.ATTR_NAME);
                                        SharedUserSetting sharedUserSetting = Settings.this.mSharedUsers.get(attributeValue3);
                                        if (sharedUserSetting == null) {
                                            Slog.w("PackageManager", "Unknown shared user:" + attributeValue3);
                                            XmlUtils.skipCurrentTag(xmlPullParser);
                                        } else {
                                            parsePermissionsLPr(xmlPullParser, sharedUserSetting.getPermissionsState(), i);
                                        }
                                        break;
                                    case 3:
                                        parseRestoredRuntimePermissionsLPr(xmlPullParser, xmlPullParser.getAttributeValue(null, "packageName"), i);
                                        break;
                                }
                            } else {
                                if (name.equals(Settings.TAG_RUNTIME_PERMISSIONS)) {
                                    b = 0;
                                }
                                switch (b) {
                                }
                            }
                        }
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }
        }

        private void parseRestoredRuntimePermissionsLPr(XmlPullParser xmlPullParser, String str, int i) throws XmlPullParserException, IOException {
            int depth = xmlPullParser.getDepth();
            while (true) {
                int next = xmlPullParser.next();
                if (next != 1) {
                    if (next != 3 || xmlPullParser.getDepth() > depth) {
                        if (next != 3 && next != 4) {
                            String name = xmlPullParser.getName();
                            byte b = -1;
                            if (name.hashCode() == 3437296 && name.equals(Settings.TAG_PERMISSION_ENTRY)) {
                                b = 0;
                            }
                            if (b == 0) {
                                String attributeValue = xmlPullParser.getAttributeValue(null, Settings.ATTR_NAME);
                                boolean zEquals = "true".equals(xmlPullParser.getAttributeValue(null, Settings.ATTR_GRANTED));
                                int i2 = "true".equals(xmlPullParser.getAttributeValue(null, Settings.ATTR_USER_SET)) ? 1 : 0;
                                if ("true".equals(xmlPullParser.getAttributeValue(null, Settings.ATTR_USER_FIXED))) {
                                    i2 |= 2;
                                }
                                int i3 = "true".equals(xmlPullParser.getAttributeValue(null, Settings.ATTR_REVOKE_ON_UPGRADE)) ? i2 | 8 : i2;
                                if (zEquals || i3 != 0) {
                                    rememberRestoredUserGrantLPr(str, attributeValue, zEquals, i3, i);
                                }
                            }
                        }
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }
        }

        private void parsePermissionsLPr(XmlPullParser xmlPullParser, PermissionsState permissionsState, int i) throws XmlPullParserException, IOException {
            int depth = xmlPullParser.getDepth();
            while (true) {
                int next = xmlPullParser.next();
                boolean z = true;
                if (next != 1) {
                    if (next != 3 || xmlPullParser.getDepth() > depth) {
                        if (next != 3 && next != 4) {
                            String name = xmlPullParser.getName();
                            byte b = -1;
                            if (name.hashCode() == 3242771 && name.equals(Settings.TAG_ITEM)) {
                                b = 0;
                            }
                            if (b == 0) {
                                String attributeValue = xmlPullParser.getAttributeValue(null, Settings.ATTR_NAME);
                                BasePermission permission = Settings.this.mPermissions.getPermission(attributeValue);
                                if (permission == null) {
                                    Slog.w("PackageManager", "Unknown permission:" + attributeValue);
                                    XmlUtils.skipCurrentTag(xmlPullParser);
                                } else {
                                    String attributeValue2 = xmlPullParser.getAttributeValue(null, Settings.ATTR_GRANTED);
                                    if (attributeValue2 != null && !Boolean.parseBoolean(attributeValue2)) {
                                        z = false;
                                    }
                                    String attributeValue3 = xmlPullParser.getAttributeValue(null, Settings.ATTR_FLAGS);
                                    int i2 = attributeValue3 != null ? Integer.parseInt(attributeValue3, 16) : 0;
                                    if (z) {
                                        permissionsState.grantRuntimePermission(permission, i);
                                        permissionsState.updatePermissionFlags(permission, i, 255, i2);
                                    } else {
                                        permissionsState.updatePermissionFlags(permission, i, 255, i2);
                                    }
                                }
                            }
                        }
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }
        }

        private void writePermissions(XmlSerializer xmlSerializer, List<PermissionsState.PermissionState> list) throws IOException {
            for (PermissionsState.PermissionState permissionState : list) {
                xmlSerializer.startTag(null, Settings.TAG_ITEM);
                xmlSerializer.attribute(null, Settings.ATTR_NAME, permissionState.getName());
                xmlSerializer.attribute(null, Settings.ATTR_GRANTED, String.valueOf(permissionState.isGranted()));
                xmlSerializer.attribute(null, Settings.ATTR_FLAGS, Integer.toHexString(permissionState.getFlags()));
                xmlSerializer.endTag(null, Settings.TAG_ITEM);
            }
        }

        private final class MyHandler extends Handler {
            public MyHandler() {
                super(BackgroundThread.getHandler().getLooper());
            }

            @Override
            public void handleMessage(Message message) throws Throwable {
                int i = message.what;
                Runnable runnable = (Runnable) message.obj;
                RuntimePermissionPersistence.this.writePermissionsSync(i);
                if (runnable != null) {
                    runnable.run();
                }
            }
        }
    }
}
