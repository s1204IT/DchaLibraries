package android.content.pm;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageParserCacheHelper;
import android.content.pm.split.DefaultSplitAssetLoader;
import android.content.pm.split.SplitAssetDependencyLoader;
import android.content.pm.split.SplitAssetLoader;
import android.content.pm.split.SplitDependencyLoader;
import android.content.res.ApkAssets;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.media.TtmlUtils;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PatternMatcher;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.provider.SettingsStringUtil;
import android.security.keystore.KeyProperties;
import android.service.notification.ZenModeConfig;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.ByteStringUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.PackageUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TypedValue;
import android.util.apk.ApkSignatureVerifier;
import android.widget.GridLayout;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.DumpHeapActivity;
import com.android.internal.os.ClassLoaderFactory;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import libcore.io.IoUtils;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class PackageParser {
    public static final String ANDROID_MANIFEST_FILENAME = "AndroidManifest.xml";
    private static final String ANDROID_RESOURCES = "http://schemas.android.com/apk/res/android";
    public static final String APK_FILE_EXTENSION = ".apk";
    private static final Set<String> CHILD_PACKAGE_TAGS;
    private static final boolean DEBUG_BACKUP = false;
    private static final boolean DEBUG_JAR = false;
    private static final boolean DEBUG_PARSER = false;
    private static final float DEFAULT_PRE_O_MAX_ASPECT_RATIO = 1.86f;
    private static final boolean LOG_PARSE_TIMINGS = Build.IS_DEBUGGABLE;
    private static final int LOG_PARSE_TIMINGS_THRESHOLD_MS = 100;
    private static final boolean LOG_UNSAFE_BROADCASTS = false;
    private static final String METADATA_MAX_ASPECT_RATIO = "android.max_aspect";
    private static final String MNT_EXPAND = "/mnt/expand/";
    private static final boolean MULTI_PACKAGE_APK_ENABLED;
    public static final NewPermissionInfo[] NEW_PERMISSIONS;
    public static final int PARSE_CHATTY = Integer.MIN_VALUE;
    public static final int PARSE_COLLECT_CERTIFICATES = 32;
    private static final int PARSE_DEFAULT_INSTALL_LOCATION = -1;
    private static final int PARSE_DEFAULT_TARGET_SANDBOX = 1;
    public static final int PARSE_ENFORCE_CODE = 64;
    public static final int PARSE_EXTERNAL_STORAGE = 8;
    public static final int PARSE_FORCE_SDK = 128;

    @Deprecated
    public static final int PARSE_FORWARD_LOCK = 4;
    public static final int PARSE_IGNORE_PROCESSES = 2;
    public static final int PARSE_IS_SYSTEM_DIR = 16;
    public static final int PARSE_MUST_BE_APK = 1;
    private static final String PROPERTY_CHILD_PACKAGES_ENABLED = "persist.sys.child_packages_enabled";
    private static final int RECREATE_ON_CONFIG_CHANGES_MASK = 3;
    private static final boolean RIGID_PARSER = false;
    private static final Set<String> SAFE_BROADCASTS;
    private static final String[] SDK_CODENAMES;
    private static final int SDK_VERSION;
    public static final SplitPermissionInfo[] SPLIT_PERMISSIONS;
    private static final String TAG = "PackageParser";
    private static final String TAG_ADOPT_PERMISSIONS = "adopt-permissions";
    private static final String TAG_APPLICATION = "application";
    private static final String TAG_COMPATIBLE_SCREENS = "compatible-screens";
    private static final String TAG_EAT_COMMENT = "eat-comment";
    private static final String TAG_FEATURE_GROUP = "feature-group";
    private static final String TAG_INSTRUMENTATION = "instrumentation";
    private static final String TAG_KEY_SETS = "key-sets";
    private static final String TAG_MANIFEST = "manifest";
    private static final String TAG_ORIGINAL_PACKAGE = "original-package";
    private static final String TAG_OVERLAY = "overlay";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_PACKAGE_VERIFIER = "package-verifier";
    private static final String TAG_PERMISSION = "permission";
    private static final String TAG_PERMISSION_GROUP = "permission-group";
    private static final String TAG_PERMISSION_TREE = "permission-tree";
    private static final String TAG_PROTECTED_BROADCAST = "protected-broadcast";
    private static final String TAG_RESTRICT_UPDATE = "restrict-update";
    private static final String TAG_SUPPORTS_INPUT = "supports-input";
    private static final String TAG_SUPPORT_SCREENS = "supports-screens";
    private static final String TAG_USES_CONFIGURATION = "uses-configuration";
    private static final String TAG_USES_FEATURE = "uses-feature";
    private static final String TAG_USES_GL_TEXTURE = "uses-gl-texture";
    private static final String TAG_USES_PERMISSION = "uses-permission";
    private static final String TAG_USES_PERMISSION_SDK_23 = "uses-permission-sdk-23";
    private static final String TAG_USES_PERMISSION_SDK_M = "uses-permission-sdk-m";
    private static final String TAG_USES_SDK = "uses-sdk";
    private static final String TAG_USES_SPLIT = "uses-split";
    public static final AtomicInteger sCachedPackageReadCount;
    private static boolean sCompatibilityModeEnabled;
    private static final Comparator<String> sSplitNameComparator;

    @Deprecated
    private String mArchiveSourcePath;
    private File mCacheDir;
    private Callback mCallback;
    private boolean mOnlyCoreApps;
    private ParsePackageItemArgs mParseInstrumentationArgs;
    private String[] mSeparateProcesses;
    private int mParseError = 1;
    private DisplayMetrics mMetrics = new DisplayMetrics();

    public interface Callback {
        String[] getOverlayApks(String str);

        String[] getOverlayPaths(String str, String str2);

        boolean hasFeature(String str);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ParseFlags {
    }

    static {
        MULTI_PACKAGE_APK_ENABLED = Build.IS_DEBUGGABLE && SystemProperties.getBoolean(PROPERTY_CHILD_PACKAGES_ENABLED, false);
        CHILD_PACKAGE_TAGS = new ArraySet();
        CHILD_PACKAGE_TAGS.add(TAG_APPLICATION);
        CHILD_PACKAGE_TAGS.add(TAG_USES_PERMISSION);
        CHILD_PACKAGE_TAGS.add(TAG_USES_PERMISSION_SDK_M);
        CHILD_PACKAGE_TAGS.add(TAG_USES_PERMISSION_SDK_23);
        CHILD_PACKAGE_TAGS.add(TAG_USES_CONFIGURATION);
        CHILD_PACKAGE_TAGS.add(TAG_USES_FEATURE);
        CHILD_PACKAGE_TAGS.add(TAG_FEATURE_GROUP);
        CHILD_PACKAGE_TAGS.add(TAG_USES_SDK);
        CHILD_PACKAGE_TAGS.add(TAG_SUPPORT_SCREENS);
        CHILD_PACKAGE_TAGS.add(TAG_INSTRUMENTATION);
        CHILD_PACKAGE_TAGS.add(TAG_USES_GL_TEXTURE);
        CHILD_PACKAGE_TAGS.add(TAG_COMPATIBLE_SCREENS);
        CHILD_PACKAGE_TAGS.add(TAG_SUPPORTS_INPUT);
        CHILD_PACKAGE_TAGS.add(TAG_EAT_COMMENT);
        sCachedPackageReadCount = new AtomicInteger();
        SAFE_BROADCASTS = new ArraySet();
        SAFE_BROADCASTS.add(Intent.ACTION_BOOT_COMPLETED);
        NEW_PERMISSIONS = new NewPermissionInfo[]{new NewPermissionInfo(Manifest.permission.WRITE_EXTERNAL_STORAGE, 4, 0), new NewPermissionInfo(Manifest.permission.READ_PHONE_STATE, 4, 0)};
        SPLIT_PERMISSIONS = new SplitPermissionInfo[]{new SplitPermissionInfo(Manifest.permission.WRITE_EXTERNAL_STORAGE, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 10001), new SplitPermissionInfo(Manifest.permission.READ_CONTACTS, new String[]{Manifest.permission.READ_CALL_LOG}, 16), new SplitPermissionInfo(Manifest.permission.WRITE_CONTACTS, new String[]{Manifest.permission.WRITE_CALL_LOG}, 16)};
        SDK_VERSION = Build.VERSION.SDK_INT;
        SDK_CODENAMES = Build.VERSION.ACTIVE_CODENAMES;
        sCompatibilityModeEnabled = true;
        sSplitNameComparator = new SplitNameComparator();
    }

    public static class NewPermissionInfo {
        public final int fileVersion;
        public final String name;
        public final int sdkVersion;

        public NewPermissionInfo(String str, int i, int i2) {
            this.name = str;
            this.sdkVersion = i;
            this.fileVersion = i2;
        }
    }

    public static class SplitPermissionInfo {
        public final String[] newPerms;
        public final String rootPerm;
        public final int targetSdk;

        public SplitPermissionInfo(String str, String[] strArr, int i) {
            this.rootPerm = str;
            this.newPerms = strArr;
            this.targetSdk = i;
        }
    }

    static class ParsePackageItemArgs {
        final int bannerRes;
        final int iconRes;
        final int labelRes;
        final int logoRes;
        final int nameRes;
        final String[] outError;
        final Package owner;
        final int roundIconRes;
        TypedArray sa;
        String tag;

        ParsePackageItemArgs(Package r1, String[] strArr, int i, int i2, int i3, int i4, int i5, int i6) {
            this.owner = r1;
            this.outError = strArr;
            this.nameRes = i;
            this.labelRes = i2;
            this.iconRes = i3;
            this.logoRes = i5;
            this.bannerRes = i6;
            this.roundIconRes = i4;
        }
    }

    @VisibleForTesting
    public static class ParseComponentArgs extends ParsePackageItemArgs {
        final int descriptionRes;
        final int enabledRes;
        int flags;
        final int processRes;
        final String[] sepProcesses;

        public ParseComponentArgs(Package r1, String[] strArr, int i, int i2, int i3, int i4, int i5, int i6, String[] strArr2, int i7, int i8, int i9) {
            super(r1, strArr, i, i2, i3, i4, i5, i6);
            this.sepProcesses = strArr2;
            this.processRes = i7;
            this.descriptionRes = i8;
            this.enabledRes = i9;
        }
    }

    public static class PackageLite {
        public final String baseCodePath;
        public final int baseRevisionCode;
        public final String codePath;
        public final String[] configForSplit;
        public final boolean coreApp;
        public final boolean debuggable;
        public final boolean extractNativeLibs;
        public final int installLocation;
        public final boolean[] isFeatureSplits;
        public final boolean isolatedSplits;
        public final boolean multiArch;
        public final String packageName;
        public final String[] splitCodePaths;
        public final String[] splitNames;
        public final int[] splitRevisionCodes;
        public final boolean use32bitAbi;
        public final String[] usesSplitNames;
        public final VerifierInfo[] verifiers;
        public final int versionCode;
        public final int versionCodeMajor;

        public PackageLite(String str, ApkLite apkLite, String[] strArr, boolean[] zArr, String[] strArr2, String[] strArr3, String[] strArr4, int[] iArr) {
            this.packageName = apkLite.packageName;
            this.versionCode = apkLite.versionCode;
            this.versionCodeMajor = apkLite.versionCodeMajor;
            this.installLocation = apkLite.installLocation;
            this.verifiers = apkLite.verifiers;
            this.splitNames = strArr;
            this.isFeatureSplits = zArr;
            this.usesSplitNames = strArr2;
            this.configForSplit = strArr3;
            this.codePath = str;
            this.baseCodePath = apkLite.codePath;
            this.splitCodePaths = strArr4;
            this.baseRevisionCode = apkLite.revisionCode;
            this.splitRevisionCodes = iArr;
            this.coreApp = apkLite.coreApp;
            this.debuggable = apkLite.debuggable;
            this.multiArch = apkLite.multiArch;
            this.use32bitAbi = apkLite.use32bitAbi;
            this.extractNativeLibs = apkLite.extractNativeLibs;
            this.isolatedSplits = apkLite.isolatedSplits;
        }

        public List<String> getAllCodePaths() {
            ArrayList arrayList = new ArrayList();
            arrayList.add(this.baseCodePath);
            if (!ArrayUtils.isEmpty(this.splitCodePaths)) {
                Collections.addAll(arrayList, this.splitCodePaths);
            }
            return arrayList;
        }
    }

    public static class ApkLite {
        public final String codePath;
        public final String configForSplit;
        public final boolean coreApp;
        public final boolean debuggable;
        public final boolean extractNativeLibs;
        public final int installLocation;
        public boolean isFeatureSplit;
        public final boolean isolatedSplits;
        public final boolean multiArch;
        public final String packageName;
        public final int revisionCode;
        public final SigningDetails signingDetails;
        public final String splitName;
        public final boolean use32bitAbi;
        public final String usesSplitName;
        public final VerifierInfo[] verifiers;
        public final int versionCode;
        public final int versionCodeMajor;

        public ApkLite(String str, String str2, String str3, boolean z, String str4, String str5, int i, int i2, int i3, int i4, List<VerifierInfo> list, SigningDetails signingDetails, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, boolean z7) {
            this.codePath = str;
            this.packageName = str2;
            this.splitName = str3;
            this.isFeatureSplit = z;
            this.configForSplit = str4;
            this.usesSplitName = str5;
            this.versionCode = i;
            this.versionCodeMajor = i2;
            this.revisionCode = i3;
            this.installLocation = i4;
            this.signingDetails = signingDetails;
            this.verifiers = (VerifierInfo[]) list.toArray(new VerifierInfo[list.size()]);
            this.coreApp = z2;
            this.debuggable = z3;
            this.multiArch = z4;
            this.use32bitAbi = z5;
            this.extractNativeLibs = z6;
            this.isolatedSplits = z7;
        }

        public long getLongVersionCode() {
            return PackageInfo.composeLongVersionCode(this.versionCodeMajor, this.versionCode);
        }
    }

    private static class CachedComponentArgs {
        ParseComponentArgs mActivityAliasArgs;
        ParseComponentArgs mActivityArgs;
        ParseComponentArgs mProviderArgs;
        ParseComponentArgs mServiceArgs;

        private CachedComponentArgs() {
        }
    }

    public PackageParser() {
        this.mMetrics.setToDefaults();
    }

    public void setSeparateProcesses(String[] strArr) {
        this.mSeparateProcesses = strArr;
    }

    public void setOnlyCoreApps(boolean z) {
        this.mOnlyCoreApps = z;
    }

    public void setDisplayMetrics(DisplayMetrics displayMetrics) {
        this.mMetrics = displayMetrics;
    }

    public void setCacheDir(File file) {
        this.mCacheDir = file;
    }

    public static final class CallbackImpl implements Callback {
        private final PackageManager mPm;

        public CallbackImpl(PackageManager packageManager) {
            this.mPm = packageManager;
        }

        @Override
        public boolean hasFeature(String str) {
            return this.mPm.hasSystemFeature(str);
        }

        @Override
        public String[] getOverlayPaths(String str, String str2) {
            return null;
        }

        @Override
        public String[] getOverlayApks(String str) {
            return null;
        }
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public static final boolean isApkFile(File file) {
        return isApkPath(file.getName());
    }

    public static boolean isApkPath(String str) {
        return str.endsWith(APK_FILE_EXTENSION);
    }

    public static PackageInfo generatePackageInfo(Package r10, int[] iArr, int i, long j, long j2, Set<String> set, PackageUserState packageUserState) {
        return generatePackageInfo(r10, iArr, i, j, j2, set, packageUserState, UserHandle.getCallingUserId());
    }

    private static boolean checkUseInstalledOrHidden(int i, PackageUserState packageUserState, ApplicationInfo applicationInfo) {
        return packageUserState.isAvailable(i) || !(applicationInfo == null || !applicationInfo.isSystemApp() || (i & PackageManager.MATCH_KNOWN_PACKAGES) == 0);
    }

    public static boolean isAvailable(PackageUserState packageUserState) {
        return checkUseInstalledOrHidden(0, packageUserState, null);
    }

    public static PackageInfo generatePackageInfo(Package r5, int[] iArr, int i, long j, long j2, Set<String> set, PackageUserState packageUserState, int i2) {
        int size;
        int size2;
        int size3;
        int size4;
        int size5;
        if (!checkUseInstalledOrHidden(i, packageUserState, r5.applicationInfo) || !r5.isMatch(i)) {
            return null;
        }
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = r5.packageName;
        packageInfo.splitNames = r5.splitNames;
        packageInfo.versionCode = r5.mVersionCode;
        packageInfo.versionCodeMajor = r5.mVersionCodeMajor;
        packageInfo.baseRevisionCode = r5.baseRevisionCode;
        packageInfo.splitRevisionCodes = r5.splitRevisionCodes;
        packageInfo.versionName = r5.mVersionName;
        packageInfo.sharedUserId = r5.mSharedUserId;
        packageInfo.sharedUserLabel = r5.mSharedUserLabel;
        packageInfo.applicationInfo = generateApplicationInfo(r5, i, packageUserState, i2);
        packageInfo.installLocation = r5.installLocation;
        packageInfo.isStub = r5.isStub;
        packageInfo.coreApp = r5.coreApp;
        if ((packageInfo.applicationInfo.flags & 1) != 0 || (packageInfo.applicationInfo.flags & 128) != 0) {
            packageInfo.requiredForAllUsers = r5.mRequiredForAllUsers;
        }
        packageInfo.restrictedAccountType = r5.mRestrictedAccountType;
        packageInfo.requiredAccountType = r5.mRequiredAccountType;
        packageInfo.overlayTarget = r5.mOverlayTarget;
        packageInfo.overlayCategory = r5.mOverlayCategory;
        packageInfo.overlayPriority = r5.mOverlayPriority;
        packageInfo.mOverlayIsStatic = r5.mOverlayIsStatic;
        packageInfo.compileSdkVersion = r5.mCompileSdkVersion;
        packageInfo.compileSdkVersionCodename = r5.mCompileSdkVersionCodename;
        packageInfo.firstInstallTime = j;
        packageInfo.lastUpdateTime = j2;
        if ((i & 256) != 0) {
            packageInfo.gids = iArr;
        }
        if ((i & 16384) != 0) {
            int size6 = r5.configPreferences != null ? r5.configPreferences.size() : 0;
            if (size6 > 0) {
                packageInfo.configPreferences = new ConfigurationInfo[size6];
                r5.configPreferences.toArray(packageInfo.configPreferences);
            }
            int size7 = r5.reqFeatures != null ? r5.reqFeatures.size() : 0;
            if (size7 > 0) {
                packageInfo.reqFeatures = new FeatureInfo[size7];
                r5.reqFeatures.toArray(packageInfo.reqFeatures);
            }
            int size8 = r5.featureGroups != null ? r5.featureGroups.size() : 0;
            if (size8 > 0) {
                packageInfo.featureGroups = new FeatureGroupInfo[size8];
                r5.featureGroups.toArray(packageInfo.featureGroups);
            }
        }
        if ((i & 1) != 0 && (size5 = r5.activities.size()) > 0) {
            ActivityInfo[] activityInfoArr = new ActivityInfo[size5];
            int i3 = 0;
            for (int i4 = 0; i4 < size5; i4++) {
                Activity activity = r5.activities.get(i4);
                if (packageUserState.isMatch(activity.info, i)) {
                    activityInfoArr[i3] = generateActivityInfo(activity, i, packageUserState, i2);
                    i3++;
                }
            }
            packageInfo.activities = (ActivityInfo[]) ArrayUtils.trimToSize(activityInfoArr, i3);
        }
        if ((i & 2) != 0 && (size4 = r5.receivers.size()) > 0) {
            ActivityInfo[] activityInfoArr2 = new ActivityInfo[size4];
            int i5 = 0;
            for (int i6 = 0; i6 < size4; i6++) {
                Activity activity2 = r5.receivers.get(i6);
                if (packageUserState.isMatch(activity2.info, i)) {
                    activityInfoArr2[i5] = generateActivityInfo(activity2, i, packageUserState, i2);
                    i5++;
                }
            }
            packageInfo.receivers = (ActivityInfo[]) ArrayUtils.trimToSize(activityInfoArr2, i5);
        }
        if ((i & 4) != 0 && (size3 = r5.services.size()) > 0) {
            ServiceInfo[] serviceInfoArr = new ServiceInfo[size3];
            int i7 = 0;
            for (int i8 = 0; i8 < size3; i8++) {
                Service service = r5.services.get(i8);
                if (packageUserState.isMatch(service.info, i)) {
                    serviceInfoArr[i7] = generateServiceInfo(service, i, packageUserState, i2);
                    i7++;
                }
            }
            packageInfo.services = (ServiceInfo[]) ArrayUtils.trimToSize(serviceInfoArr, i7);
        }
        if ((i & 8) != 0 && (size2 = r5.providers.size()) > 0) {
            ProviderInfo[] providerInfoArr = new ProviderInfo[size2];
            int i9 = 0;
            for (int i10 = 0; i10 < size2; i10++) {
                Provider provider = r5.providers.get(i10);
                if (packageUserState.isMatch(provider.info, i)) {
                    providerInfoArr[i9] = generateProviderInfo(provider, i, packageUserState, i2);
                    i9++;
                }
            }
            packageInfo.providers = (ProviderInfo[]) ArrayUtils.trimToSize(providerInfoArr, i9);
        }
        if ((i & 16) != 0 && (size = r5.instrumentation.size()) > 0) {
            packageInfo.instrumentation = new InstrumentationInfo[size];
            for (int i11 = 0; i11 < size; i11++) {
                packageInfo.instrumentation[i11] = generateInstrumentationInfo(r5.instrumentation.get(i11), i);
            }
        }
        if ((i & 4096) != 0) {
            int size9 = r5.permissions.size();
            if (size9 > 0) {
                packageInfo.permissions = new PermissionInfo[size9];
                for (int i12 = 0; i12 < size9; i12++) {
                    packageInfo.permissions[i12] = generatePermissionInfo(r5.permissions.get(i12), i);
                }
            }
            int size10 = r5.requestedPermissions.size();
            if (size10 > 0) {
                packageInfo.requestedPermissions = new String[size10];
                packageInfo.requestedPermissionsFlags = new int[size10];
                for (int i13 = 0; i13 < size10; i13++) {
                    String str = r5.requestedPermissions.get(i13);
                    packageInfo.requestedPermissions[i13] = str;
                    int[] iArr2 = packageInfo.requestedPermissionsFlags;
                    iArr2[i13] = iArr2[i13] | 1;
                    if (set != null && set.contains(str)) {
                        int[] iArr3 = packageInfo.requestedPermissionsFlags;
                        iArr3[i13] = iArr3[i13] | 2;
                    }
                }
            }
        }
        if ((i & 64) != 0) {
            if (r5.mSigningDetails.hasPastSigningCertificates()) {
                packageInfo.signatures = new Signature[1];
                packageInfo.signatures[0] = r5.mSigningDetails.pastSigningCertificates[0];
            } else if (r5.mSigningDetails.hasSignatures()) {
                int length = r5.mSigningDetails.signatures.length;
                packageInfo.signatures = new Signature[length];
                System.arraycopy(r5.mSigningDetails.signatures, 0, packageInfo.signatures, 0, length);
            }
        }
        if ((134217728 & i) != 0) {
            if (r5.mSigningDetails != SigningDetails.UNKNOWN) {
                packageInfo.signingInfo = new SigningInfo(r5.mSigningDetails);
            } else {
                packageInfo.signingInfo = null;
            }
        }
        return packageInfo;
    }

    private static class SplitNameComparator implements Comparator<String> {
        private SplitNameComparator() {
        }

        @Override
        public int compare(String str, String str2) {
            if (str == null) {
                return -1;
            }
            if (str2 == null) {
                return 1;
            }
            return str.compareTo(str2);
        }
    }

    public static PackageLite parsePackageLite(File file, int i) throws PackageParserException {
        if (file.isDirectory()) {
            return parseClusterPackageLite(file, i);
        }
        return parseMonolithicPackageLite(file, i);
    }

    private static PackageLite parseMonolithicPackageLite(File file, int i) throws PackageParserException {
        Trace.traceBegin(Trace.TRACE_TAG_PACKAGE_MANAGER, "parseApkLite");
        ApkLite apkLite = parseApkLite(file, i);
        String absolutePath = file.getAbsolutePath();
        Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
        return new PackageLite(absolutePath, apkLite, null, null, null, null, null, null);
    }

    static PackageLite parseClusterPackageLite(File file, int i) throws PackageParserException {
        String[] strArr;
        boolean[] zArr;
        String[] strArr2;
        String[] strArr3;
        String[] strArr4;
        int[] iArr;
        File[] fileArrListFiles = file.listFiles();
        if (ArrayUtils.isEmpty(fileArrListFiles)) {
            throw new PackageParserException(-100, "No packages found in split");
        }
        Trace.traceBegin(Trace.TRACE_TAG_PACKAGE_MANAGER, "parseApkLite");
        ArrayMap arrayMap = new ArrayMap();
        int i2 = 0;
        String str = null;
        for (File file2 : fileArrListFiles) {
            if (isApkFile(file2)) {
                ApkLite apkLite = parseApkLite(file2, i);
                if (str == null) {
                    str = apkLite.packageName;
                    i2 = apkLite.versionCode;
                } else {
                    if (!str.equals(apkLite.packageName)) {
                        throw new PackageParserException(-101, "Inconsistent package " + apkLite.packageName + " in " + file2 + "; expected " + str);
                    }
                    if (i2 != apkLite.versionCode) {
                        throw new PackageParserException(-101, "Inconsistent version " + apkLite.versionCode + " in " + file2 + "; expected " + i2);
                    }
                }
                if (arrayMap.put(apkLite.splitName, apkLite) != null) {
                    throw new PackageParserException(-101, "Split name " + apkLite.splitName + " defined more than once; most recent was " + file2);
                }
            }
        }
        Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
        ApkLite apkLite2 = (ApkLite) arrayMap.remove(null);
        if (apkLite2 == null) {
            throw new PackageParserException(-101, "Missing base APK in " + file);
        }
        int size = arrayMap.size();
        if (size > 0) {
            boolean[] zArr2 = new boolean[size];
            String[] strArr5 = new String[size];
            String[] strArr6 = new String[size];
            String[] strArr7 = new String[size];
            int[] iArr2 = new int[size];
            String[] strArr8 = (String[]) arrayMap.keySet().toArray(new String[size]);
            Arrays.sort(strArr8, sSplitNameComparator);
            for (int i3 = 0; i3 < size; i3++) {
                ApkLite apkLite3 = (ApkLite) arrayMap.get(strArr8[i3]);
                strArr5[i3] = apkLite3.usesSplitName;
                zArr2[i3] = apkLite3.isFeatureSplit;
                strArr6[i3] = apkLite3.configForSplit;
                strArr7[i3] = apkLite3.codePath;
                iArr2[i3] = apkLite3.revisionCode;
            }
            strArr = strArr8;
            strArr2 = strArr5;
            strArr3 = strArr6;
            zArr = zArr2;
            strArr4 = strArr7;
            iArr = iArr2;
        } else {
            strArr = null;
            zArr = null;
            strArr2 = null;
            strArr3 = null;
            strArr4 = null;
            iArr = null;
        }
        return new PackageLite(file.getAbsolutePath(), apkLite2, strArr, zArr, strArr2, strArr3, strArr4, iArr);
    }

    public Package parsePackage(File file, int i, boolean z) throws PackageParserException {
        Package monolithicPackage;
        Package cachedResult = z ? getCachedResult(file, i) : null;
        if (cachedResult != null) {
            return cachedResult;
        }
        long jUptimeMillis = LOG_PARSE_TIMINGS ? SystemClock.uptimeMillis() : 0L;
        if (file.isDirectory()) {
            monolithicPackage = parseClusterPackage(file, i);
        } else {
            monolithicPackage = parseMonolithicPackage(file, i);
        }
        long jUptimeMillis2 = LOG_PARSE_TIMINGS ? SystemClock.uptimeMillis() : 0L;
        cacheResult(file, i, monolithicPackage);
        if (LOG_PARSE_TIMINGS) {
            long j = jUptimeMillis2 - jUptimeMillis;
            long jUptimeMillis3 = SystemClock.uptimeMillis() - jUptimeMillis2;
            if (j + jUptimeMillis3 > 100) {
                Slog.i(TAG, "Parse times for '" + file + "': parse=" + j + "ms, update_cache=" + jUptimeMillis3 + " ms");
            }
        }
        return monolithicPackage;
    }

    public Package parsePackage(File file, int i) throws PackageParserException {
        return parsePackage(file, i, false);
    }

    private String getCacheKey(File file, int i) {
        return file.getName() + '-' + i;
    }

    @VisibleForTesting
    protected Package fromCacheEntry(byte[] bArr) {
        return fromCacheEntryStatic(bArr);
    }

    @VisibleForTesting
    public static Package fromCacheEntryStatic(byte[] bArr) {
        Parcel parcelObtain = Parcel.obtain();
        parcelObtain.unmarshall(bArr, 0, bArr.length);
        parcelObtain.setDataPosition(0);
        new PackageParserCacheHelper.ReadHelper(parcelObtain).startAndInstall();
        Package r3 = new Package(parcelObtain);
        parcelObtain.recycle();
        sCachedPackageReadCount.incrementAndGet();
        return r3;
    }

    @VisibleForTesting
    protected byte[] toCacheEntry(Package r1) {
        return toCacheEntryStatic(r1);
    }

    @VisibleForTesting
    public static byte[] toCacheEntryStatic(Package r3) {
        Parcel parcelObtain = Parcel.obtain();
        PackageParserCacheHelper.WriteHelper writeHelper = new PackageParserCacheHelper.WriteHelper(parcelObtain);
        r3.writeToParcel(parcelObtain, 0);
        writeHelper.finishAndUninstall();
        byte[] bArrMarshall = parcelObtain.marshall();
        parcelObtain.recycle();
        return bArrMarshall;
    }

    private static boolean isCacheUpToDate(File file, File file2) {
        try {
            return Os.stat(file.getAbsolutePath()).st_mtime < Os.stat(file2.getAbsolutePath()).st_mtime;
        } catch (ErrnoException e) {
            if (e.errno != OsConstants.ENOENT) {
                Slog.w("Error while stating package cache : ", e);
            }
            return false;
        }
    }

    private Package getCachedResult(File file, int i) {
        String[] overlayApks;
        if (this.mCacheDir == null) {
            return null;
        }
        File file2 = new File(this.mCacheDir, getCacheKey(file, i));
        try {
            if (!isCacheUpToDate(file, file2)) {
                return null;
            }
            Package packageFromCacheEntry = fromCacheEntry(IoUtils.readFileAsByteArray(file2.getAbsolutePath()));
            if (this.mCallback != null && (overlayApks = this.mCallback.getOverlayApks(packageFromCacheEntry.packageName)) != null && overlayApks.length > 0) {
                for (String str : overlayApks) {
                    if (!isCacheUpToDate(new File(str), file2)) {
                        return null;
                    }
                }
            }
            return packageFromCacheEntry;
        } catch (Throwable th) {
            Slog.w(TAG, "Error reading package cache: ", th);
            file2.delete();
            return null;
        }
    }

    private void cacheResult(File file, int i, Package r5) {
        if (this.mCacheDir == null) {
            return;
        }
        try {
            File file2 = new File(this.mCacheDir, getCacheKey(file, i));
            if (file2.exists() && !file2.delete()) {
                Slog.e(TAG, "Unable to delete cache file: " + file2);
            }
            byte[] cacheEntry = toCacheEntry(r5);
            if (cacheEntry == null) {
                return;
            }
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file2);
                Throwable th = null;
                try {
                    fileOutputStream.write(cacheEntry);
                    fileOutputStream.close();
                } catch (Throwable th2) {
                    if (0 != 0) {
                        try {
                            fileOutputStream.close();
                        } catch (Throwable th3) {
                            th.addSuppressed(th3);
                        }
                    } else {
                        fileOutputStream.close();
                    }
                    throw th2;
                }
            } catch (IOException e) {
                Slog.w(TAG, "Error writing cache entry.", e);
                file2.delete();
            }
        } catch (Throwable th4) {
            Slog.w(TAG, "Error saving package cache.", th4);
        }
    }

    private Package parseClusterPackage(File file, int i) throws PackageParserException {
        SplitAssetLoader defaultSplitAssetLoader;
        PackageLite clusterPackageLite = parseClusterPackageLite(file, 0);
        if (this.mOnlyCoreApps && !clusterPackageLite.coreApp) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "Not a coreApp: " + file);
        }
        SparseArray<int[]> sparseArrayCreateDependenciesFromPackage = null;
        if (clusterPackageLite.isolatedSplits && !ArrayUtils.isEmpty(clusterPackageLite.splitNames)) {
            try {
                sparseArrayCreateDependenciesFromPackage = SplitAssetDependencyLoader.createDependenciesFromPackage(clusterPackageLite);
                defaultSplitAssetLoader = new SplitAssetDependencyLoader(clusterPackageLite, sparseArrayCreateDependenciesFromPackage, i);
            } catch (SplitDependencyLoader.IllegalDependencyException e) {
                throw new PackageParserException(-101, e.getMessage());
            }
        } else {
            defaultSplitAssetLoader = new DefaultSplitAssetLoader(clusterPackageLite, i);
        }
        try {
            try {
                AssetManager baseAssetManager = defaultSplitAssetLoader.getBaseAssetManager();
                File file2 = new File(clusterPackageLite.baseCodePath);
                Package baseApk = parseBaseApk(file2, baseAssetManager, i);
                if (baseApk == null) {
                    throw new PackageParserException(-100, "Failed to parse base APK: " + file2);
                }
                if (!ArrayUtils.isEmpty(clusterPackageLite.splitNames)) {
                    int length = clusterPackageLite.splitNames.length;
                    baseApk.splitNames = clusterPackageLite.splitNames;
                    baseApk.splitCodePaths = clusterPackageLite.splitCodePaths;
                    baseApk.splitRevisionCodes = clusterPackageLite.splitRevisionCodes;
                    baseApk.splitFlags = new int[length];
                    baseApk.splitPrivateFlags = new int[length];
                    baseApk.applicationInfo.splitNames = baseApk.splitNames;
                    baseApk.applicationInfo.splitDependencies = sparseArrayCreateDependenciesFromPackage;
                    baseApk.applicationInfo.splitClassLoaderNames = new String[length];
                    for (int i2 = 0; i2 < length; i2++) {
                        parseSplitApk(baseApk, i2, defaultSplitAssetLoader.getSplitAssetManager(i2), i);
                    }
                }
                baseApk.setCodePath(file.getCanonicalPath());
                baseApk.setUse32bitAbi(clusterPackageLite.use32bitAbi);
                return baseApk;
            } catch (IOException e2) {
                throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed to get path: " + clusterPackageLite.baseCodePath, e2);
            }
        } finally {
            IoUtils.closeQuietly(defaultSplitAssetLoader);
        }
    }

    @Deprecated
    public Package parseMonolithicPackage(File file, int i) throws PackageParserException {
        PackageLite monolithicPackageLite = parseMonolithicPackageLite(file, i);
        if (this.mOnlyCoreApps && !monolithicPackageLite.coreApp) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "Not a coreApp: " + file);
        }
        DefaultSplitAssetLoader defaultSplitAssetLoader = new DefaultSplitAssetLoader(monolithicPackageLite, i);
        try {
            try {
                Package baseApk = parseBaseApk(file, defaultSplitAssetLoader.getBaseAssetManager(), i);
                baseApk.setCodePath(file.getCanonicalPath());
                baseApk.setUse32bitAbi(monolithicPackageLite.use32bitAbi);
                return baseApk;
            } catch (IOException e) {
                throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed to get path: " + file, e);
            }
        } finally {
            IoUtils.closeQuietly(defaultSplitAssetLoader);
        }
    }

    private Package parseBaseApk(File file, AssetManager assetManager, int i) throws Throwable {
        String strSubstring;
        XmlResourceParser xmlResourceParserOpenXmlResourceParser;
        String absolutePath = file.getAbsolutePath();
        if (absolutePath.startsWith(MNT_EXPAND)) {
            strSubstring = absolutePath.substring(MNT_EXPAND.length(), absolutePath.indexOf(47, MNT_EXPAND.length()));
        } else {
            strSubstring = null;
        }
        this.mParseError = 1;
        this.mArchiveSourcePath = file.getAbsolutePath();
        try {
            try {
                int iFindCookieForPath = assetManager.findCookieForPath(absolutePath);
                if (iFindCookieForPath == 0) {
                    throw new PackageParserException(-101, "Failed adding asset path: " + absolutePath);
                }
                xmlResourceParserOpenXmlResourceParser = assetManager.openXmlResourceParser(iFindCookieForPath, ANDROID_MANIFEST_FILENAME);
                try {
                    Resources resources = new Resources(assetManager, this.mMetrics, null);
                    String[] strArr = new String[1];
                    Package baseApk = parseBaseApk(absolutePath, resources, xmlResourceParserOpenXmlResourceParser, i, strArr);
                    if (baseApk != null) {
                        baseApk.setVolumeUuid(strSubstring);
                        baseApk.setApplicationVolumeUuid(strSubstring);
                        baseApk.setBaseCodePath(absolutePath);
                        baseApk.setSigningDetails(SigningDetails.UNKNOWN);
                        IoUtils.closeQuietly(xmlResourceParserOpenXmlResourceParser);
                        return baseApk;
                    }
                    throw new PackageParserException(this.mParseError, absolutePath + " (at " + xmlResourceParserOpenXmlResourceParser.getPositionDescription() + "): " + strArr[0]);
                } catch (PackageParserException e) {
                    throw e;
                } catch (Exception e2) {
                    e = e2;
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed to read manifest from " + absolutePath, e);
                } catch (Throwable th) {
                    th = th;
                    IoUtils.closeQuietly(xmlResourceParserOpenXmlResourceParser);
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                xmlResourceParserOpenXmlResourceParser = null;
            }
        } catch (PackageParserException e3) {
            throw e3;
        } catch (Exception e4) {
            e = e4;
        }
    }

    private void parseSplitApk(Package r12, int i, AssetManager assetManager, int i2) throws Throwable {
        String str = r12.splitCodePaths[i];
        this.mParseError = 1;
        this.mArchiveSourcePath = str;
        XmlResourceParser xmlResourceParser = null;
        try {
            try {
                int iFindCookieForPath = assetManager.findCookieForPath(str);
                if (iFindCookieForPath == 0) {
                    throw new PackageParserException(-101, "Failed adding asset path: " + str);
                }
                XmlResourceParser xmlResourceParserOpenXmlResourceParser = assetManager.openXmlResourceParser(iFindCookieForPath, ANDROID_MANIFEST_FILENAME);
                try {
                    String[] strArr = new String[1];
                    if (parseSplitApk(r12, new Resources(assetManager, this.mMetrics, null), xmlResourceParserOpenXmlResourceParser, i2, i, strArr) != null) {
                        IoUtils.closeQuietly(xmlResourceParserOpenXmlResourceParser);
                        return;
                    }
                    throw new PackageParserException(this.mParseError, str + " (at " + xmlResourceParserOpenXmlResourceParser.getPositionDescription() + "): " + strArr[0]);
                } catch (PackageParserException e) {
                } catch (Exception e2) {
                    e = e2;
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed to read manifest from " + str, e);
                } catch (Throwable th) {
                    th = th;
                    xmlResourceParser = xmlResourceParserOpenXmlResourceParser;
                    IoUtils.closeQuietly(xmlResourceParser);
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (PackageParserException e3) {
            throw e3;
        } catch (Exception e4) {
            e = e4;
        }
    }

    private Package parseSplitApk(Package r9, Resources resources, XmlResourceParser xmlResourceParser, int i, int i2, String[] strArr) throws XmlPullParserException, PackageParserException, IOException {
        parsePackageSplitNames(xmlResourceParser, xmlResourceParser);
        this.mParseInstrumentationArgs = null;
        int depth = xmlResourceParser.getDepth();
        boolean z = false;
        while (true) {
            int next = xmlResourceParser.next();
            if (next == 1 || (next == 3 && xmlResourceParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != 4) {
                if (xmlResourceParser.getName().equals(TAG_APPLICATION)) {
                    if (z) {
                        Slog.w(TAG, "<manifest> has more than one <application>");
                        XmlUtils.skipCurrentTag(xmlResourceParser);
                    } else {
                        if (!parseSplitApplication(r9, resources, xmlResourceParser, i, i2, strArr)) {
                            return null;
                        }
                        z = true;
                    }
                } else {
                    Slog.w(TAG, "Unknown element under <manifest>: " + xmlResourceParser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                    XmlUtils.skipCurrentTag(xmlResourceParser);
                }
            }
        }
    }

    public static ArraySet<PublicKey> toSigningKeys(Signature[] signatureArr) throws CertificateException {
        ArraySet<PublicKey> arraySet = new ArraySet<>(signatureArr.length);
        for (Signature signature : signatureArr) {
            arraySet.add(signature.getPublicKey());
        }
        return arraySet;
    }

    public static void collectCertificates(Package r3, boolean z) throws PackageParserException {
        collectCertificatesInternal(r3, z);
        int size = r3.childPackages != null ? r3.childPackages.size() : 0;
        for (int i = 0; i < size; i++) {
            r3.childPackages.get(i).mSigningDetails = r3.mSigningDetails;
        }
    }

    private static void collectCertificatesInternal(Package r5, boolean z) throws PackageParserException {
        r5.mSigningDetails = SigningDetails.UNKNOWN;
        Trace.traceBegin(Trace.TRACE_TAG_PACKAGE_MANAGER, "collectCertificates");
        try {
            collectCertificates(r5, new File(r5.baseCodePath), z);
            if (!ArrayUtils.isEmpty(r5.splitCodePaths)) {
                for (int i = 0; i < r5.splitCodePaths.length; i++) {
                    collectCertificates(r5, new File(r5.splitCodePaths[i]), z);
                }
            }
        } finally {
            Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
        }
    }

    private static void collectCertificates(Package r2, File file, boolean z) throws PackageParserException {
        int i;
        SigningDetails signingDetailsVerify;
        String absolutePath = file.getAbsolutePath();
        if (r2.applicationInfo.isStaticSharedLibrary()) {
            i = 2;
        } else {
            i = 1;
        }
        if (z) {
            signingDetailsVerify = ApkSignatureVerifier.plsCertsNoVerifyOnlyCerts(absolutePath, i);
        } else {
            signingDetailsVerify = ApkSignatureVerifier.verify(absolutePath, i);
        }
        if (r2.mSigningDetails == SigningDetails.UNKNOWN) {
            r2.mSigningDetails = signingDetailsVerify;
        } else if (!Signature.areExactMatch(r2.mSigningDetails.signatures, signingDetailsVerify.signatures)) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES, absolutePath + " has mismatched certificates");
        }
    }

    private static AssetManager newConfiguredAssetManager() {
        AssetManager assetManager = new AssetManager();
        assetManager.setConfiguration(0, 0, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Build.VERSION.RESOURCES_SDK_INT);
        return assetManager;
    }

    public static ApkLite parseApkLite(File file, int i) throws PackageParserException {
        return parseApkLiteInner(file, null, null, i);
    }

    public static ApkLite parseApkLite(FileDescriptor fileDescriptor, String str, int i) throws PackageParserException {
        return parseApkLiteInner(null, fileDescriptor, str, i);
    }

    private static ApkLite parseApkLiteInner(File file, FileDescriptor fileDescriptor, String str, int i) throws Throwable {
        XmlResourceParser xmlResourceParserOpenXml;
        SigningDetails signingDetails;
        String absolutePath = fileDescriptor != null ? str : file.getAbsolutePath();
        XmlResourceParser xmlResourceParser = null;
        try {
            try {
                try {
                    xmlResourceParserOpenXml = (fileDescriptor != null ? ApkAssets.loadFromFd(fileDescriptor, str, false, false) : ApkAssets.loadFromPath(absolutePath)).openXml(ANDROID_MANIFEST_FILENAME);
                } catch (IOException e) {
                    throw new PackageParserException(-100, "Failed to parse " + absolutePath);
                }
            } catch (Throwable th) {
                th = th;
            }
            try {
                try {
                    if ((i & 32) != 0) {
                        Package r7 = new Package((String) null);
                        boolean z = (i & 16) != 0;
                        Trace.traceBegin(Trace.TRACE_TAG_PACKAGE_MANAGER, "collectCertificates");
                        try {
                            collectCertificates(r7, file, z);
                            Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                            signingDetails = r7.mSigningDetails;
                        } catch (Throwable th2) {
                            Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                            throw th2;
                        }
                    } else {
                        signingDetails = SigningDetails.UNKNOWN;
                    }
                    ApkLite apkLite = parseApkLite(absolutePath, xmlResourceParserOpenXml, xmlResourceParserOpenXml, signingDetails);
                    IoUtils.closeQuietly(xmlResourceParserOpenXml);
                    return apkLite;
                } catch (IOException | RuntimeException | XmlPullParserException e2) {
                    e = e2;
                    Slog.w(TAG, "Failed to parse " + absolutePath, e);
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed to parse " + absolutePath, e);
                }
            } catch (Throwable th3) {
                th = th3;
                xmlResourceParser = xmlResourceParserOpenXml;
                IoUtils.closeQuietly(xmlResourceParser);
                throw th;
            }
        } catch (IOException | RuntimeException | XmlPullParserException e3) {
            e = e3;
        }
    }

    private static String validateName(String str, boolean z, boolean z2) {
        int length = str.length();
        boolean z3 = false;
        boolean z4 = true;
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if ((cCharAt >= 'a' && cCharAt <= 'z') || (cCharAt >= 'A' && cCharAt <= 'Z')) {
                z4 = false;
            } else if (z4 || ((cCharAt < '0' || cCharAt > '9') && cCharAt != '_')) {
                if (cCharAt != '.') {
                    return "bad character '" + cCharAt + "'";
                }
                z3 = true;
                z4 = true;
            }
        }
        if (z2 && !FileUtils.isValidExtFilename(str)) {
            return "Invalid filename";
        }
        if (z3 || !z) {
            return null;
        }
        return "must have at least one '.' separator";
    }

    private static Pair<String, String> parsePackageSplitNames(XmlPullParser xmlPullParser, AttributeSet attributeSet) throws XmlPullParserException, PackageParserException, IOException {
        int next;
        String strValidateName;
        do {
            next = xmlPullParser.next();
            if (next == 2) {
                break;
            }
        } while (next != 1);
        if (next != 2) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "No start tag found");
        }
        if (!xmlPullParser.getName().equals(TAG_MANIFEST)) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "No <manifest> tag");
        }
        String attributeValue = attributeSet.getAttributeValue(null, "package");
        if (!ZenModeConfig.SYSTEM_AUTHORITY.equals(attributeValue) && (strValidateName = validateName(attributeValue, true, true)) != null) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME, "Invalid manifest package: " + strValidateName);
        }
        String attributeValue2 = attributeSet.getAttributeValue(null, "split");
        if (attributeValue2 != null) {
            if (attributeValue2.length() != 0) {
                String strValidateName2 = validateName(attributeValue2, false, false);
                if (strValidateName2 != null) {
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME, "Invalid manifest split: " + strValidateName2);
                }
            } else {
                attributeValue2 = null;
            }
        }
        String strIntern = attributeValue.intern();
        if (attributeValue2 != null) {
            attributeValue2 = attributeValue2.intern();
        }
        return Pair.create(strIntern, attributeValue2);
    }

    private static ApkLite parseApkLite(String str, XmlPullParser xmlPullParser, AttributeSet attributeSet, SigningDetails signingDetails) throws XmlPullParserException, PackageParserException, IOException {
        Pair<String, String> packageSplitNames = parsePackageSplitNames(xmlPullParser, attributeSet);
        String attributeValue = null;
        String attributeValue2 = null;
        int attributeIntValue = -1;
        boolean attributeBooleanValue = false;
        int attributeIntValue2 = 0;
        int attributeIntValue3 = 0;
        int attributeIntValue4 = 0;
        boolean attributeBooleanValue2 = false;
        boolean attributeBooleanValue3 = false;
        for (int i = 0; i < attributeSet.getAttributeCount(); i++) {
            String attributeName = attributeSet.getAttributeName(i);
            if (attributeName.equals("installLocation")) {
                attributeIntValue = attributeSet.getAttributeIntValue(i, -1);
            } else if (attributeName.equals("versionCode")) {
                attributeIntValue2 = attributeSet.getAttributeIntValue(i, 0);
            } else if (attributeName.equals("versionCodeMajor")) {
                attributeIntValue3 = attributeSet.getAttributeIntValue(i, 0);
            } else if (attributeName.equals("revisionCode")) {
                attributeIntValue4 = attributeSet.getAttributeIntValue(i, 0);
            } else if (attributeName.equals("coreApp")) {
                attributeBooleanValue2 = attributeSet.getAttributeBooleanValue(i, false);
            } else if (attributeName.equals("isolatedSplits")) {
                attributeBooleanValue3 = attributeSet.getAttributeBooleanValue(i, false);
            } else if (attributeName.equals("configForSplit")) {
                attributeValue2 = attributeSet.getAttributeValue(i);
            } else if (attributeName.equals("isFeatureSplit")) {
                attributeBooleanValue = attributeSet.getAttributeBooleanValue(i, false);
            }
        }
        int depth = xmlPullParser.getDepth() + 1;
        ArrayList arrayList = new ArrayList();
        boolean attributeBooleanValue4 = false;
        boolean attributeBooleanValue5 = false;
        boolean attributeBooleanValue6 = false;
        boolean attributeBooleanValue7 = true;
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() < depth)) {
                break;
            }
            if (next != 3 && next != 4 && xmlPullParser.getDepth() == depth) {
                if (TAG_PACKAGE_VERIFIER.equals(xmlPullParser.getName())) {
                    VerifierInfo verifier = parseVerifier(attributeSet);
                    if (verifier != null) {
                        arrayList.add(verifier);
                    }
                } else if (TAG_APPLICATION.equals(xmlPullParser.getName())) {
                    for (int i2 = 0; i2 < attributeSet.getAttributeCount(); i2++) {
                        String attributeName2 = attributeSet.getAttributeName(i2);
                        if ("debuggable".equals(attributeName2)) {
                            attributeBooleanValue4 = attributeSet.getAttributeBooleanValue(i2, false);
                        }
                        if ("multiArch".equals(attributeName2)) {
                            attributeBooleanValue5 = attributeSet.getAttributeBooleanValue(i2, false);
                        }
                        if ("use32bitAbi".equals(attributeName2)) {
                            attributeBooleanValue6 = attributeSet.getAttributeBooleanValue(i2, false);
                        }
                        if ("extractNativeLibs".equals(attributeName2)) {
                            attributeBooleanValue7 = attributeSet.getAttributeBooleanValue(i2, true);
                        }
                    }
                } else if (!TAG_USES_SPLIT.equals(xmlPullParser.getName())) {
                    continue;
                } else if (attributeValue != null) {
                    Slog.w(TAG, "Only one <uses-split> permitted. Ignoring others.");
                } else {
                    attributeValue = attributeSet.getAttributeValue(ANDROID_RESOURCES, "name");
                    if (attributeValue == null) {
                        throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "<uses-split> tag requires 'android:name' attribute");
                    }
                }
            }
        }
    }

    private boolean parseBaseApkChild(Package r10, Resources resources, XmlResourceParser xmlResourceParser, int i, String[] strArr) throws XmlPullParserException, IOException {
        String attributeValue = xmlResourceParser.getAttributeValue(null, "package");
        if (validateName(attributeValue, true, false) != null) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME;
            return false;
        }
        if (attributeValue.equals(r10.packageName)) {
            String str = "Child package name cannot be equal to parent package name: " + r10.packageName;
            Slog.w(TAG, str);
            strArr[0] = str;
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        if (r10.hasChildPackage(attributeValue)) {
            String str2 = "Duplicate child package:" + attributeValue;
            Slog.w(TAG, str2);
            strArr[0] = str2;
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        Package r2 = new Package(attributeValue);
        r2.mVersionCode = r10.mVersionCode;
        r2.baseRevisionCode = r10.baseRevisionCode;
        r2.mVersionName = r10.mVersionName;
        r2.applicationInfo.targetSdkVersion = r10.applicationInfo.targetSdkVersion;
        r2.applicationInfo.minSdkVersion = r10.applicationInfo.minSdkVersion;
        Package baseApkCommon = parseBaseApkCommon(r2, CHILD_PACKAGE_TAGS, resources, xmlResourceParser, i, strArr);
        if (baseApkCommon == null) {
            return false;
        }
        if (r10.childPackages == null) {
            r10.childPackages = new ArrayList<>();
        }
        r10.childPackages.add(baseApkCommon);
        baseApkCommon.parentPackage = r10;
        return true;
    }

    private Package parseBaseApk(String str, Resources resources, XmlResourceParser xmlResourceParser, int i, String[] strArr) throws XmlPullParserException, IOException {
        String[] overlayPaths;
        try {
            Pair<String, String> packageSplitNames = parsePackageSplitNames(xmlResourceParser, xmlResourceParser);
            String str2 = packageSplitNames.first;
            String str3 = packageSplitNames.second;
            if (!TextUtils.isEmpty(str3)) {
                strArr[0] = "Expected base APK, but found split " + str3;
                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME;
                return null;
            }
            if (this.mCallback != null && (overlayPaths = this.mCallback.getOverlayPaths(str2, str)) != null && overlayPaths.length > 0) {
                for (String str4 : overlayPaths) {
                    resources.getAssets().addOverlayPath(str4);
                }
            }
            Package r2 = new Package(str2);
            TypedArray typedArrayObtainAttributes = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifest);
            r2.mVersionCode = typedArrayObtainAttributes.getInteger(1, 0);
            r2.mVersionCodeMajor = typedArrayObtainAttributes.getInteger(11, 0);
            r2.applicationInfo.setVersionCode(r2.getLongVersionCode());
            r2.baseRevisionCode = typedArrayObtainAttributes.getInteger(5, 0);
            r2.mVersionName = typedArrayObtainAttributes.getNonConfigurationString(2, 0);
            if (r2.mVersionName != null) {
                r2.mVersionName = r2.mVersionName.intern();
            }
            r2.coreApp = xmlResourceParser.getAttributeBooleanValue(null, "coreApp", false);
            r2.mCompileSdkVersion = typedArrayObtainAttributes.getInteger(9, 0);
            r2.applicationInfo.compileSdkVersion = r2.mCompileSdkVersion;
            r2.mCompileSdkVersionCodename = typedArrayObtainAttributes.getNonConfigurationString(10, 0);
            if (r2.mCompileSdkVersionCodename != null) {
                r2.mCompileSdkVersionCodename = r2.mCompileSdkVersionCodename.intern();
            }
            r2.applicationInfo.compileSdkVersionCodename = r2.mCompileSdkVersionCodename;
            typedArrayObtainAttributes.recycle();
            return parseBaseApkCommon(r2, null, resources, xmlResourceParser, i, strArr);
        } catch (PackageParserException e) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME;
            return null;
        }
    }

    private Package parseBaseApkCommon(Package r27, Set<String> set, Resources resources, XmlResourceParser xmlResourceParser, int i, String[] strArr) throws XmlPullParserException, IOException {
        int i2;
        int i3;
        int i4;
        Package r0;
        int i5;
        int i6;
        int i7;
        Package r02;
        int i8;
        int integer;
        ?? r2;
        int i9;
        String string;
        String string2;
        ?? r22;
        TypedValue typedValuePeekValue;
        int iComputeMinSdkVersion;
        Package r13 = null;
        this.mParseInstrumentationArgs = null;
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifest);
        ?? r14 = 0;
        String nonConfigurationString = typedArrayObtainAttributes.getNonConfigurationString(0, 0);
        int i10 = 3;
        int i11 = 1;
        if (nonConfigurationString != null && nonConfigurationString.length() > 0) {
            String strValidateName = validateName(nonConfigurationString, true, false);
            if (strValidateName != null && !ZenModeConfig.SYSTEM_AUTHORITY.equals(r27.packageName)) {
                strArr[0] = "<manifest> specifies bad sharedUserId name \"" + nonConfigurationString + "\": " + strValidateName;
                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID;
                return null;
            }
            r27.mSharedUserId = nonConfigurationString.intern();
            r27.mSharedUserLabel = typedArrayObtainAttributes.getResourceId(3, 0);
        }
        int i12 = 4;
        r27.installLocation = typedArrayObtainAttributes.getInteger(4, -1);
        r27.applicationInfo.installLocation = r27.installLocation;
        r27.applicationInfo.targetSandboxVersion = typedArrayObtainAttributes.getInteger(7, 1);
        if ((i & 4) != 0) {
            r27.applicationInfo.privateFlags |= 4;
        }
        if ((i & 8) != 0) {
            r27.applicationInfo.flags |= 262144;
        }
        if (typedArrayObtainAttributes.getBoolean(6, false)) {
            r27.applicationInfo.privateFlags |= 32768;
        }
        int depth = xmlResourceParser.getDepth();
        int i13 = 1;
        int i14 = 1;
        int i15 = 1;
        int i16 = 1;
        int i17 = 1;
        int i18 = 1;
        boolean z = false;
        while (true) {
            int next = xmlResourceParser.next();
            if (next == i11 || (next == i10 && xmlResourceParser.getDepth() <= depth)) {
                break;
            }
            if (next == i10 || next == i12) {
                i2 = depth;
                i3 = i18;
                i4 = i13;
                r0 = r13;
                i5 = i16;
                i6 = i17;
                i7 = i11;
                i11 = i7;
                depth = i2;
                i12 = 4;
                r14 = 0;
                i17 = i6;
                i16 = i5;
                r13 = r0;
                i13 = i4;
                i18 = i3;
                i10 = 3;
            } else {
                String name = xmlResourceParser.getName();
                if (set == null || set.contains(name)) {
                    if (!name.equals(TAG_APPLICATION)) {
                        int i19 = i13;
                        i2 = depth;
                        if (name.equals("overlay")) {
                            ?? ObtainAttributes = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestResourceOverlay);
                            r27.mOverlayTarget = ObtainAttributes.getString(1);
                            r27.mOverlayCategory = ObtainAttributes.getString(2);
                            r27.mOverlayPriority = ObtainAttributes.getInt(r14, r14);
                            r27.mOverlayIsStatic = ObtainAttributes.getBoolean(3, r14);
                            String string3 = ObtainAttributes.getString(4);
                            String string4 = ObtainAttributes.getString(5);
                            ObtainAttributes.recycle();
                            if (r27.mOverlayTarget == null) {
                                strArr[r14] = "<overlay> does not specify a target package";
                                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                return r13;
                            }
                            if (r27.mOverlayPriority < 0 || r27.mOverlayPriority > 9999) {
                                break;
                            }
                            if (!checkOverlayRequiredSystemProperty(string3, string4)) {
                                Slog.i(TAG, "Skipping target and overlay pair " + r27.mOverlayTarget + " and " + r27.baseCodePath + ": overlay ignored due to required system property: " + string3 + " with value: " + string4);
                                return r13;
                            }
                            XmlUtils.skipCurrentTag(xmlResourceParser);
                        } else if (name.equals(TAG_KEY_SETS)) {
                            if (!parseKeySets(r27, resources, xmlResourceParser, strArr)) {
                                return r13;
                            }
                        } else if (name.equals(TAG_PERMISSION_GROUP)) {
                            if (!parsePermissionGroup(r27, i, resources, xmlResourceParser, strArr)) {
                                return r13;
                            }
                        } else if (name.equals("permission")) {
                            if (!parsePermission(r27, resources, xmlResourceParser, strArr)) {
                                return r13;
                            }
                        } else if (name.equals(TAG_PERMISSION_TREE)) {
                            if (!parsePermissionTree(r27, resources, xmlResourceParser, strArr)) {
                                return r13;
                            }
                        } else if (name.equals(TAG_USES_PERMISSION)) {
                            if (!parseUsesPermission(r27, resources, xmlResourceParser)) {
                                return r13;
                            }
                        } else if (name.equals(TAG_USES_PERMISSION_SDK_M) || name.equals(TAG_USES_PERMISSION_SDK_23)) {
                            i5 = i16;
                            i6 = i17;
                            i7 = 1;
                            i3 = i18;
                            i4 = i19;
                            if (!parseUsesPermission(r27, resources, xmlResourceParser)) {
                                return null;
                            }
                            r02 = null;
                            i8 = i4;
                            i18 = i3;
                            int i20 = i6;
                            i16 = i5;
                            integer = i20;
                        } else if (name.equals(TAG_USES_CONFIGURATION)) {
                            ConfigurationInfo configurationInfo = new ConfigurationInfo();
                            ?? ObtainAttributes2 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestUsesConfiguration);
                            configurationInfo.reqTouchScreen = ObtainAttributes2.getInt(r14, r14);
                            configurationInfo.reqKeyboardType = ObtainAttributes2.getInt(1, r14);
                            if (ObtainAttributes2.getBoolean(2, r14)) {
                                configurationInfo.reqInputFeatures |= 1;
                            }
                            configurationInfo.reqNavigation = ObtainAttributes2.getInt(3, r14);
                            if (ObtainAttributes2.getBoolean(4, r14)) {
                                configurationInfo.reqInputFeatures |= 2;
                            }
                            ObtainAttributes2.recycle();
                            r27.configPreferences = ArrayUtils.add(r27.configPreferences, configurationInfo);
                            XmlUtils.skipCurrentTag(xmlResourceParser);
                        } else {
                            int i21 = 4;
                            if (name.equals(TAG_USES_FEATURE)) {
                                FeatureInfo usesFeature = parseUsesFeature(resources, xmlResourceParser);
                                r27.reqFeatures = ArrayUtils.add(r27.reqFeatures, usesFeature);
                                if (usesFeature.name == null) {
                                    ConfigurationInfo configurationInfo2 = new ConfigurationInfo();
                                    configurationInfo2.reqGlEsVersion = usesFeature.reqGlEsVersion;
                                    r27.configPreferences = ArrayUtils.add(r27.configPreferences, configurationInfo2);
                                }
                                XmlUtils.skipCurrentTag(xmlResourceParser);
                            } else if (name.equals(TAG_FEATURE_GROUP)) {
                                FeatureGroupInfo featureGroupInfo = new FeatureGroupInfo();
                                int depth2 = xmlResourceParser.getDepth();
                                ?? Add = r13;
                                while (true) {
                                    int next2 = xmlResourceParser.next();
                                    if (next2 == 1 || (next2 == 3 && xmlResourceParser.getDepth() <= depth2)) {
                                        break;
                                    }
                                    if (next2 != 3 && next2 != i21) {
                                        String name2 = xmlResourceParser.getName();
                                        if (name2.equals(TAG_USES_FEATURE)) {
                                            FeatureInfo usesFeature2 = parseUsesFeature(resources, xmlResourceParser);
                                            usesFeature2.flags |= 1;
                                            Add = ArrayUtils.add((ArrayList<FeatureInfo>) Add, usesFeature2);
                                        } else {
                                            Slog.w(TAG, "Unknown element under <feature-group>: " + name2 + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                                            Add = Add;
                                        }
                                        XmlUtils.skipCurrentTag(xmlResourceParser);
                                    }
                                    i21 = 4;
                                    Add = Add;
                                }
                                if (Add != 0) {
                                    featureGroupInfo.features = new FeatureInfo[Add.size()];
                                    featureGroupInfo.features = (FeatureInfo[]) Add.toArray(featureGroupInfo.features);
                                }
                                r27.featureGroups = ArrayUtils.add(r27.featureGroups, featureGroupInfo);
                                i5 = i16;
                                i6 = i17;
                                r02 = null;
                                i7 = 1;
                                i3 = i18;
                                i4 = i19;
                                i8 = i4;
                                i18 = i3;
                                int i202 = i6;
                                i16 = i5;
                                integer = i202;
                            } else if (name.equals(TAG_USES_SDK)) {
                                if (SDK_VERSION > 0) {
                                    ?? ObtainAttributes3 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestUsesSdk);
                                    TypedValue typedValuePeekValue2 = ObtainAttributes3.peekValue(r14);
                                    if (typedValuePeekValue2 == null) {
                                        r2 = r14;
                                        i9 = 1;
                                    } else if (typedValuePeekValue2.type != 3 || typedValuePeekValue2.string == null) {
                                        i9 = typedValuePeekValue2.data;
                                        r2 = i9;
                                    } else {
                                        string2 = typedValuePeekValue2.string.toString();
                                        string = string2;
                                        r22 = r14;
                                        i9 = 1;
                                        typedValuePeekValue = ObtainAttributes3.peekValue(1);
                                        ?? r23 = r22;
                                        if (typedValuePeekValue != null) {
                                            if (typedValuePeekValue.type != 3 || typedValuePeekValue.string == null) {
                                                r23 = typedValuePeekValue.data;
                                            } else {
                                                string = typedValuePeekValue.string.toString();
                                                r23 = r22;
                                                if (string2 == null) {
                                                    string2 = string;
                                                    r23 = r22;
                                                }
                                            }
                                        }
                                        ObtainAttributes3.recycle();
                                        iComputeMinSdkVersion = computeMinSdkVersion(i9, string2, SDK_VERSION, SDK_CODENAMES, strArr);
                                        if (iComputeMinSdkVersion >= 0) {
                                            this.mParseError = -12;
                                            return null;
                                        }
                                        int iComputeTargetSdkVersion = computeTargetSdkVersion(r23, string, SDK_CODENAMES, strArr, (i & 128) != 0 ? 1 : r14);
                                        if (iComputeTargetSdkVersion < 0) {
                                            this.mParseError = -12;
                                            return null;
                                        }
                                        r27.applicationInfo.minSdkVersion = iComputeMinSdkVersion;
                                        r27.applicationInfo.targetSdkVersion = iComputeTargetSdkVersion;
                                    }
                                    string = null;
                                    string2 = null;
                                    r22 = r2;
                                    typedValuePeekValue = ObtainAttributes3.peekValue(1);
                                    ?? r232 = r22;
                                    if (typedValuePeekValue != null) {
                                    }
                                    ObtainAttributes3.recycle();
                                    iComputeMinSdkVersion = computeMinSdkVersion(i9, string2, SDK_VERSION, SDK_CODENAMES, strArr);
                                    if (iComputeMinSdkVersion >= 0) {
                                    }
                                }
                                XmlUtils.skipCurrentTag(xmlResourceParser);
                                i5 = i16;
                                i6 = i17;
                                i3 = i18;
                                i4 = i19;
                                r02 = null;
                                i7 = 1;
                                i8 = i4;
                                i18 = i3;
                                int i2022 = i6;
                                i16 = i5;
                                integer = i2022;
                            } else if (name.equals(TAG_SUPPORT_SCREENS)) {
                                ?? ObtainAttributes4 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestSupportsScreens);
                                r27.applicationInfo.requiresSmallestWidthDp = ObtainAttributes4.getInteger(6, r14);
                                r27.applicationInfo.compatibleWidthLimitDp = ObtainAttributes4.getInteger(7, r14);
                                r27.applicationInfo.largestWidthLimitDp = ObtainAttributes4.getInteger(8, r14);
                                int integer2 = ObtainAttributes4.getInteger(1, i19);
                                int integer3 = ObtainAttributes4.getInteger(2, i14);
                                int integer4 = ObtainAttributes4.getInteger(3, i15);
                                int integer5 = ObtainAttributes4.getInteger(5, i16);
                                integer = ObtainAttributes4.getInteger(4, i17);
                                int integer6 = ObtainAttributes4.getInteger(r14, i18);
                                ObtainAttributes4.recycle();
                                XmlUtils.skipCurrentTag(xmlResourceParser);
                                i16 = integer5;
                                i8 = integer2;
                                i14 = integer3;
                                i7 = 1;
                                i15 = integer4;
                                i18 = integer6;
                                r02 = null;
                            } else {
                                i5 = i16;
                                i6 = i17;
                                i3 = i18;
                                if (name.equals(TAG_PROTECTED_BROADCAST)) {
                                    ?? ObtainAttributes5 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestProtectedBroadcast);
                                    String nonResourceString = ObtainAttributes5.getNonResourceString(r14);
                                    ObtainAttributes5.recycle();
                                    if (nonResourceString != null) {
                                        if (r27.protectedBroadcasts == null) {
                                            r27.protectedBroadcasts = new ArrayList<>();
                                        }
                                        if (!r27.protectedBroadcasts.contains(nonResourceString)) {
                                            r27.protectedBroadcasts.add(nonResourceString.intern());
                                        }
                                    }
                                    XmlUtils.skipCurrentTag(xmlResourceParser);
                                } else if (name.equals(TAG_INSTRUMENTATION)) {
                                    if (parseInstrumentation(r27, resources, xmlResourceParser, strArr) == null) {
                                        return null;
                                    }
                                } else if (name.equals(TAG_ORIGINAL_PACKAGE)) {
                                    ?? ObtainAttributes6 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestOriginalPackage);
                                    String nonConfigurationString2 = ObtainAttributes6.getNonConfigurationString(r14, r14);
                                    if (!r27.packageName.equals(nonConfigurationString2)) {
                                        if (r27.mOriginalPackages == null) {
                                            r27.mOriginalPackages = new ArrayList<>();
                                            r27.mRealPackage = r27.packageName;
                                        }
                                        r27.mOriginalPackages.add(nonConfigurationString2);
                                    }
                                    ObtainAttributes6.recycle();
                                    XmlUtils.skipCurrentTag(xmlResourceParser);
                                } else if (name.equals(TAG_ADOPT_PERMISSIONS)) {
                                    ?? ObtainAttributes7 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestOriginalPackage);
                                    String nonConfigurationString3 = ObtainAttributes7.getNonConfigurationString(r14, r14);
                                    ObtainAttributes7.recycle();
                                    if (nonConfigurationString3 != null) {
                                        if (r27.mAdoptPermissions == null) {
                                            r27.mAdoptPermissions = new ArrayList<>();
                                        }
                                        r27.mAdoptPermissions.add(nonConfigurationString3);
                                    }
                                    XmlUtils.skipCurrentTag(xmlResourceParser);
                                } else {
                                    if (name.equals(TAG_USES_GL_TEXTURE)) {
                                        XmlUtils.skipCurrentTag(xmlResourceParser);
                                    } else if (name.equals(TAG_COMPATIBLE_SCREENS)) {
                                        XmlUtils.skipCurrentTag(xmlResourceParser);
                                    } else if (name.equals(TAG_SUPPORTS_INPUT)) {
                                        XmlUtils.skipCurrentTag(xmlResourceParser);
                                    } else if (name.equals(TAG_EAT_COMMENT)) {
                                        XmlUtils.skipCurrentTag(xmlResourceParser);
                                    } else {
                                        if (!name.equals("package")) {
                                            i7 = 1;
                                            i4 = i19;
                                            if (name.equals(TAG_RESTRICT_UPDATE)) {
                                                if ((i & 16) != 0) {
                                                    ?? ObtainAttributes8 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestRestrictUpdate);
                                                    ?? nonConfigurationString4 = ObtainAttributes8.getNonConfigurationString(r14, r14);
                                                    ObtainAttributes8.recycle();
                                                    r27.restrictUpdateHash = null;
                                                    if (nonConfigurationString4 != 0) {
                                                        int length = nonConfigurationString4.length();
                                                        byte[] bArr = new byte[length / 2];
                                                        for (?? r3 = r14; r3 < length; r3 += 2) {
                                                            bArr[r3 / 2] = (byte) ((Character.digit(nonConfigurationString4.charAt(r3), 16) << 4) + Character.digit(nonConfigurationString4.charAt(r3 + 1), 16));
                                                            length = length;
                                                        }
                                                        r27.restrictUpdateHash = bArr;
                                                    }
                                                }
                                                XmlUtils.skipCurrentTag(xmlResourceParser);
                                            } else {
                                                Slog.w(TAG, "Unknown element under <manifest>: " + xmlResourceParser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                                                XmlUtils.skipCurrentTag(xmlResourceParser);
                                                r0 = null;
                                                i11 = i7;
                                                depth = i2;
                                                i12 = 4;
                                                r14 = 0;
                                                i17 = i6;
                                                i16 = i5;
                                                r13 = r0;
                                                i13 = i4;
                                                i18 = i3;
                                                i10 = 3;
                                            }
                                        } else if (MULTI_PACKAGE_APK_ENABLED) {
                                            i7 = 1;
                                            i4 = i19;
                                            if (!parseBaseApkChild(r27, resources, xmlResourceParser, i, strArr)) {
                                                return null;
                                            }
                                        } else {
                                            XmlUtils.skipCurrentTag(xmlResourceParser);
                                        }
                                        r02 = null;
                                        i8 = i4;
                                        i18 = i3;
                                        int i20222 = i6;
                                        i16 = i5;
                                        integer = i20222;
                                    }
                                    i7 = 1;
                                    i4 = i19;
                                    r0 = null;
                                    i11 = i7;
                                    depth = i2;
                                    i12 = 4;
                                    r14 = 0;
                                    i17 = i6;
                                    i16 = i5;
                                    r13 = r0;
                                    i13 = i4;
                                    i18 = i3;
                                    i10 = 3;
                                }
                                i7 = 1;
                                i4 = i19;
                                r02 = null;
                                i8 = i4;
                                i18 = i3;
                                int i202222 = i6;
                                i16 = i5;
                                integer = i202222;
                            }
                        }
                        r02 = r13;
                        i5 = i16;
                        i6 = i17;
                        i7 = 1;
                        i3 = i18;
                        i4 = i19;
                        i8 = i4;
                        i18 = i3;
                        int i2022222 = i6;
                        i16 = i5;
                        integer = i2022222;
                    } else if (z) {
                        Slog.w(TAG, "<manifest> has more than one <application>");
                        XmlUtils.skipCurrentTag(xmlResourceParser);
                    } else {
                        i2 = depth;
                        i8 = i13;
                        if (!parseBaseApplication(r27, resources, xmlResourceParser, i, strArr)) {
                            return r13;
                        }
                        r02 = r13;
                        z = true;
                        integer = i17;
                        i7 = 1;
                    }
                    i11 = i7;
                    depth = i2;
                    i12 = 4;
                    r14 = 0;
                    i10 = 3;
                    i17 = integer;
                    r13 = r02;
                    i13 = i8;
                } else {
                    Slog.w(TAG, "Skipping unsupported element under <manifest>: " + name + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                    XmlUtils.skipCurrentTag(xmlResourceParser);
                }
                i2 = depth;
                i3 = i18;
                i4 = i13;
                r0 = r13;
                i5 = i16;
                i6 = i17;
                i7 = 1;
                i11 = i7;
                depth = i2;
                i12 = 4;
                r14 = 0;
                i17 = i6;
                i16 = i5;
                r13 = r0;
                i13 = i4;
                i18 = i3;
                i10 = 3;
            }
        }
    }

    private boolean checkOverlayRequiredSystemProperty(String str, String str2) {
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
            if (TextUtils.isEmpty(str) && TextUtils.isEmpty(str2)) {
                return true;
            }
            Slog.w(TAG, "Disabling overlay - incomplete property :'" + str + "=" + str2 + "' - require both requiredSystemPropertyName AND requiredSystemPropertyValue to be specified.");
            return false;
        }
        String str3 = SystemProperties.get(str);
        return str3 != null && str3.equals(str2);
    }

    private void adjustPackageToBeUnresizeableAndUnpipable(Package r4) {
        for (Activity activity : r4.activities) {
            activity.info.resizeMode = 0;
            activity.info.flags &= -4194305;
        }
    }

    public static int computeTargetSdkVersion(int i, String str, String[] strArr, String[] strArr2, boolean z) {
        if (str == null) {
            return i;
        }
        if (ArrayUtils.contains(strArr, str) || z) {
            return 10000;
        }
        if (strArr.length > 0) {
            strArr2[0] = "Requires development platform " + str + " (current platform is any of " + Arrays.toString(strArr) + ")";
            return -1;
        }
        strArr2[0] = "Requires development platform " + str + " but this is a release platform.";
        return -1;
    }

    public static int computeMinSdkVersion(int i, String str, int i2, String[] strArr, String[] strArr2) {
        if (str == null) {
            if (i <= i2) {
                return i;
            }
            strArr2[0] = "Requires newer sdk version #" + i + " (current version is #" + i2 + ")";
            return -1;
        }
        if (ArrayUtils.contains(strArr, str)) {
            return 10000;
        }
        if (strArr.length > 0) {
            strArr2[0] = "Requires development platform " + str + " (current platform is any of " + Arrays.toString(strArr) + ")";
        } else {
            strArr2[0] = "Requires development platform " + str + " but this is a release platform.";
        }
        return -1;
    }

    private FeatureInfo parseUsesFeature(Resources resources, AttributeSet attributeSet) {
        FeatureInfo featureInfo = new FeatureInfo();
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(attributeSet, R.styleable.AndroidManifestUsesFeature);
        featureInfo.name = typedArrayObtainAttributes.getNonResourceString(0);
        featureInfo.version = typedArrayObtainAttributes.getInt(3, 0);
        if (featureInfo.name == null) {
            featureInfo.reqGlEsVersion = typedArrayObtainAttributes.getInt(1, 0);
        }
        if (typedArrayObtainAttributes.getBoolean(2, true)) {
            featureInfo.flags |= 1;
        }
        typedArrayObtainAttributes.recycle();
        return featureInfo;
    }

    private boolean parseUsesStaticLibrary(Package r9, Resources resources, XmlResourceParser xmlResourceParser, String[] strArr) throws XmlPullParserException, IOException {
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestUsesStaticLibrary);
        String nonResourceString = typedArrayObtainAttributes.getNonResourceString(0);
        int i = typedArrayObtainAttributes.getInt(1, -1);
        String nonResourceString2 = typedArrayObtainAttributes.getNonResourceString(2);
        typedArrayObtainAttributes.recycle();
        if (nonResourceString == null || i < 0 || nonResourceString2 == null) {
            strArr[0] = "Bad uses-static-library declaration name: " + nonResourceString + " version: " + i + " certDigest" + nonResourceString2;
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            XmlUtils.skipCurrentTag(xmlResourceParser);
            return false;
        }
        if (r9.usesStaticLibraries != null && r9.usesStaticLibraries.contains(nonResourceString)) {
            strArr[0] = "Depending on multiple versions of static library " + nonResourceString;
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            XmlUtils.skipCurrentTag(xmlResourceParser);
            return false;
        }
        String strIntern = nonResourceString.intern();
        String lowerCase = nonResourceString2.replace(SettingsStringUtil.DELIMITER, "").toLowerCase();
        String[] additionalCertificates = EmptyArray.STRING;
        if (r9.applicationInfo.targetSdkVersion >= 27) {
            additionalCertificates = parseAdditionalCertificates(resources, xmlResourceParser, strArr);
            if (additionalCertificates == null) {
                return false;
            }
        } else {
            XmlUtils.skipCurrentTag(xmlResourceParser);
        }
        String[] strArr2 = new String[additionalCertificates.length + 1];
        strArr2[0] = lowerCase;
        System.arraycopy(additionalCertificates, 0, strArr2, 1, additionalCertificates.length);
        r9.usesStaticLibraries = ArrayUtils.add(r9.usesStaticLibraries, strIntern);
        r9.usesStaticLibrariesVersions = ArrayUtils.appendLong(r9.usesStaticLibrariesVersions, i, true);
        r9.usesStaticLibrariesCertDigests = (String[][]) ArrayUtils.appendElement(String[].class, r9.usesStaticLibrariesCertDigests, strArr2, true);
        return true;
    }

    private String[] parseAdditionalCertificates(Resources resources, XmlResourceParser xmlResourceParser, String[] strArr) throws XmlPullParserException, IOException {
        String[] strArr2 = EmptyArray.STRING;
        int depth = xmlResourceParser.getDepth();
        while (true) {
            int next = xmlResourceParser.next();
            if (next == 1 || (next == 3 && xmlResourceParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != 4) {
                if (xmlResourceParser.getName().equals("additional-certificate")) {
                    TypedArray typedArrayObtainAttributes = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestAdditionalCertificate);
                    String nonResourceString = typedArrayObtainAttributes.getNonResourceString(0);
                    typedArrayObtainAttributes.recycle();
                    if (TextUtils.isEmpty(nonResourceString)) {
                        strArr[0] = "Bad additional-certificate declaration with empty certDigest:" + nonResourceString;
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        XmlUtils.skipCurrentTag(xmlResourceParser);
                        typedArrayObtainAttributes.recycle();
                        return null;
                    }
                    strArr2 = (String[]) ArrayUtils.appendElement(String.class, strArr2, nonResourceString.replace(SettingsStringUtil.DELIMITER, "").toLowerCase());
                } else {
                    XmlUtils.skipCurrentTag(xmlResourceParser);
                }
            }
        }
    }

    private boolean parseUsesPermission(Package r7, Resources resources, XmlResourceParser xmlResourceParser) throws XmlPullParserException, IOException {
        int i;
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestUsesPermission);
        String nonResourceString = typedArrayObtainAttributes.getNonResourceString(0);
        TypedValue typedValuePeekValue = typedArrayObtainAttributes.peekValue(1);
        if (typedValuePeekValue != null && typedValuePeekValue.type >= 16 && typedValuePeekValue.type <= 31) {
            i = typedValuePeekValue.data;
        } else {
            i = 0;
        }
        String nonConfigurationString = typedArrayObtainAttributes.getNonConfigurationString(2, 0);
        String nonConfigurationString2 = typedArrayObtainAttributes.getNonConfigurationString(3, 0);
        typedArrayObtainAttributes.recycle();
        XmlUtils.skipCurrentTag(xmlResourceParser);
        if (nonResourceString == null) {
            return true;
        }
        if (i != 0 && i < Build.VERSION.RESOURCES_SDK_INT) {
            return true;
        }
        if (nonConfigurationString != null && this.mCallback != null && !this.mCallback.hasFeature(nonConfigurationString)) {
            return true;
        }
        if (nonConfigurationString2 != null && this.mCallback != null && this.mCallback.hasFeature(nonConfigurationString2)) {
            return true;
        }
        if (r7.requestedPermissions.indexOf(nonResourceString) == -1) {
            r7.requestedPermissions.add(nonResourceString.intern());
        } else {
            Slog.w(TAG, "Ignoring duplicate uses-permissions/uses-permissions-sdk-m: " + nonResourceString + " in package: " + r7.packageName + " at: " + xmlResourceParser.getPositionDescription());
        }
        return true;
    }

    private static String buildClassName(String str, CharSequence charSequence, String[] strArr) {
        if (charSequence == null || charSequence.length() <= 0) {
            strArr[0] = "Empty class name in package " + str;
            return null;
        }
        String string = charSequence.toString();
        if (string.charAt(0) == '.') {
            return str + string;
        }
        if (string.indexOf(46) < 0) {
            return str + '.' + string;
        }
        return string;
    }

    private static String buildCompoundName(String str, CharSequence charSequence, String str2, String[] strArr) {
        String string = charSequence.toString();
        char cCharAt = string.charAt(0);
        if (str != null && cCharAt == ':') {
            if (string.length() < 2) {
                strArr[0] = "Bad " + str2 + " name " + string + " in package " + str + ": must be at least two characters";
                return null;
            }
            String strValidateName = validateName(string.substring(1), false, false);
            if (strValidateName != null) {
                strArr[0] = "Invalid " + str2 + " name " + string + " in package " + str + ": " + strValidateName;
                return null;
            }
            return str + string;
        }
        String strValidateName2 = validateName(string, true, false);
        if (strValidateName2 != null && !StorageManager.UUID_SYSTEM.equals(string)) {
            strArr[0] = "Invalid " + str2 + " name " + string + " in package " + str + ": " + strValidateName2;
            return null;
        }
        return string;
    }

    private static String buildProcessName(String str, String str2, CharSequence charSequence, int i, String[] strArr, String[] strArr2) {
        if ((i & 2) != 0 && !StorageManager.UUID_SYSTEM.equals(charSequence)) {
            return str2 != null ? str2 : str;
        }
        if (strArr != null) {
            for (int length = strArr.length - 1; length >= 0; length--) {
                String str3 = strArr[length];
                if (str3.equals(str) || str3.equals(str2) || str3.equals(charSequence)) {
                    return str;
                }
            }
        }
        if (charSequence == null || charSequence.length() <= 0) {
            return str2;
        }
        return TextUtils.safeIntern(buildCompoundName(str, charSequence, DumpHeapActivity.KEY_PROCESS, strArr2));
    }

    private static String buildTaskAffinityName(String str, String str2, CharSequence charSequence, String[] strArr) {
        if (charSequence == null) {
            return str2;
        }
        if (charSequence.length() <= 0) {
            return null;
        }
        return buildCompoundName(str, charSequence, "taskAffinity", strArr);
    }

    private boolean parseKeySets(Package r18, Resources resources, XmlResourceParser xmlResourceParser, String[] strArr) throws XmlPullParserException, IOException {
        int depth = xmlResourceParser.getDepth();
        ArrayMap arrayMap = new ArrayMap();
        ArraySet<String> arraySet = new ArraySet<>();
        ArrayMap arrayMap2 = new ArrayMap();
        ArraySet arraySet2 = new ArraySet();
        loop0: while (true) {
            int depth2 = -1;
            String str = null;
            while (true) {
                int next = xmlResourceParser.next();
                if (next == 1 || (next == 3 && xmlResourceParser.getDepth() <= depth)) {
                    break loop0;
                }
                if (next == 3) {
                    if (xmlResourceParser.getDepth() == depth2) {
                        break;
                    }
                } else {
                    String name = xmlResourceParser.getName();
                    if (name.equals("key-set")) {
                        if (str != null) {
                            strArr[0] = "Improperly nested 'key-set' tag at " + xmlResourceParser.getPositionDescription();
                            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                            return false;
                        }
                        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestKeySet);
                        String nonResourceString = typedArrayObtainAttributes.getNonResourceString(0);
                        arrayMap2.put(nonResourceString, new ArraySet());
                        depth2 = xmlResourceParser.getDepth();
                        typedArrayObtainAttributes.recycle();
                        str = nonResourceString;
                    } else if (name.equals("public-key")) {
                        if (str == null) {
                            strArr[0] = "Improperly nested 'key-set' tag at " + xmlResourceParser.getPositionDescription();
                            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                            return false;
                        }
                        TypedArray typedArrayObtainAttributes2 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestPublicKey);
                        String nonResourceString2 = typedArrayObtainAttributes2.getNonResourceString(0);
                        String nonResourceString3 = typedArrayObtainAttributes2.getNonResourceString(1);
                        if (nonResourceString3 == null && arrayMap.get(nonResourceString2) == null) {
                            strArr[0] = "'public-key' " + nonResourceString2 + " must define a public-key value on first use at " + xmlResourceParser.getPositionDescription();
                            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                            typedArrayObtainAttributes2.recycle();
                            return false;
                        }
                        if (nonResourceString3 != null) {
                            PublicKey publicKey = parsePublicKey(nonResourceString3);
                            if (publicKey == null) {
                                Slog.w(TAG, "No recognized valid key in 'public-key' tag at " + xmlResourceParser.getPositionDescription() + " key-set " + str + " will not be added to the package's defined key-sets.");
                                typedArrayObtainAttributes2.recycle();
                                arraySet2.add(str);
                                XmlUtils.skipCurrentTag(xmlResourceParser);
                            } else if (arrayMap.get(nonResourceString2) == null || ((PublicKey) arrayMap.get(nonResourceString2)).equals(publicKey)) {
                                arrayMap.put(nonResourceString2, publicKey);
                            } else {
                                strArr[0] = "Value of 'public-key' " + nonResourceString2 + " conflicts with previously defined value at " + xmlResourceParser.getPositionDescription();
                                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                typedArrayObtainAttributes2.recycle();
                                return false;
                            }
                        }
                        ((ArraySet) arrayMap2.get(str)).add(nonResourceString2);
                        typedArrayObtainAttributes2.recycle();
                        XmlUtils.skipCurrentTag(xmlResourceParser);
                    } else if (name.equals("upgrade-key-set")) {
                        TypedArray typedArrayObtainAttributes3 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestUpgradeKeySet);
                        arraySet.add(typedArrayObtainAttributes3.getNonResourceString(0));
                        typedArrayObtainAttributes3.recycle();
                        XmlUtils.skipCurrentTag(xmlResourceParser);
                    } else {
                        Slog.w(TAG, "Unknown element under <key-sets>: " + xmlResourceParser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                        XmlUtils.skipCurrentTag(xmlResourceParser);
                    }
                }
            }
        }
    }

    private boolean parsePermissionGroup(Package r20, int i, Resources resources, XmlResourceParser xmlResourceParser, String[] strArr) throws XmlPullParserException, IOException {
        PermissionGroup permissionGroup = new PermissionGroup(r20);
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestPermissionGroup);
        if (!parsePackageItemInfo(r20, permissionGroup.info, strArr, "<permission-group>", typedArrayObtainAttributes, true, 2, 0, 1, 8, 5, 7)) {
            typedArrayObtainAttributes.recycle();
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        permissionGroup.info.descriptionRes = typedArrayObtainAttributes.getResourceId(4, 0);
        permissionGroup.info.requestRes = typedArrayObtainAttributes.getResourceId(9, 0);
        permissionGroup.info.flags = typedArrayObtainAttributes.getInt(6, 0);
        permissionGroup.info.priority = typedArrayObtainAttributes.getInt(3, 0);
        typedArrayObtainAttributes.recycle();
        if (!parseAllMetaData(resources, xmlResourceParser, "<permission-group>", permissionGroup, strArr)) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        r20.permissionGroups.add(permissionGroup);
        return true;
    }

    private boolean parsePermission(Package r21, Resources resources, XmlResourceParser xmlResourceParser, String[] strArr) throws XmlPullParserException, IOException {
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestPermission);
        Permission permission = new Permission(r21);
        if (!parsePackageItemInfo(r21, permission.info, strArr, "<permission>", typedArrayObtainAttributes, true, 2, 0, 1, 9, 6, 8)) {
            typedArrayObtainAttributes.recycle();
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        permission.info.group = typedArrayObtainAttributes.getNonResourceString(4);
        if (permission.info.group != null) {
            permission.info.group = permission.info.group.intern();
        }
        permission.info.descriptionRes = typedArrayObtainAttributes.getResourceId(5, 0);
        permission.info.requestRes = typedArrayObtainAttributes.getResourceId(10, 0);
        permission.info.protectionLevel = typedArrayObtainAttributes.getInt(3, 0);
        permission.info.flags = typedArrayObtainAttributes.getInt(7, 0);
        typedArrayObtainAttributes.recycle();
        if (permission.info.protectionLevel == -1) {
            strArr[0] = "<permission> does not specify protectionLevel";
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        permission.info.protectionLevel = PermissionInfo.fixProtectionLevel(permission.info.protectionLevel);
        if (permission.info.getProtectionFlags() != 0 && (permission.info.protectionLevel & 4096) == 0 && (permission.info.protectionLevel & 8192) == 0 && (permission.info.protectionLevel & 15) != 2) {
            strArr[0] = "<permission>  protectionLevel specifies a non-instant flag but is not based on signature type";
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        if (!parseAllMetaData(resources, xmlResourceParser, "<permission>", permission, strArr)) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        r21.permissions.add(permission);
        return true;
    }

    private boolean parsePermissionTree(Package r20, Resources resources, XmlResourceParser xmlResourceParser, String[] strArr) throws XmlPullParserException, IOException {
        Permission permission = new Permission(r20);
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestPermissionTree);
        if (!parsePackageItemInfo(r20, permission.info, strArr, "<permission-tree>", typedArrayObtainAttributes, true, 2, 0, 1, 5, 3, 4)) {
            typedArrayObtainAttributes.recycle();
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        typedArrayObtainAttributes.recycle();
        int iIndexOf = permission.info.name.indexOf(46);
        if (iIndexOf > 0) {
            iIndexOf = permission.info.name.indexOf(46, iIndexOf + 1);
        }
        if (iIndexOf < 0) {
            strArr[0] = "<permission-tree> name has less than three segments: " + permission.info.name;
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        permission.info.descriptionRes = 0;
        permission.info.requestRes = 0;
        permission.info.protectionLevel = 0;
        permission.tree = true;
        if (!parseAllMetaData(resources, xmlResourceParser, "<permission-tree>", permission, strArr)) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        r20.permissions.add(permission);
        return true;
    }

    private Instrumentation parseInstrumentation(Package r12, Resources resources, XmlResourceParser xmlResourceParser, String[] strArr) throws XmlPullParserException, IOException {
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestInstrumentation);
        if (this.mParseInstrumentationArgs == null) {
            this.mParseInstrumentationArgs = new ParsePackageItemArgs(r12, strArr, 2, 0, 1, 8, 6, 7);
            this.mParseInstrumentationArgs.tag = "<instrumentation>";
        }
        this.mParseInstrumentationArgs.sa = typedArrayObtainAttributes;
        Instrumentation instrumentation = new Instrumentation(this.mParseInstrumentationArgs, new InstrumentationInfo());
        if (strArr[0] != null) {
            typedArrayObtainAttributes.recycle();
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        String nonResourceString = typedArrayObtainAttributes.getNonResourceString(3);
        instrumentation.info.targetPackage = nonResourceString != null ? nonResourceString.intern() : null;
        String nonResourceString2 = typedArrayObtainAttributes.getNonResourceString(9);
        instrumentation.info.targetProcesses = nonResourceString2 != null ? nonResourceString2.intern() : null;
        instrumentation.info.handleProfiling = typedArrayObtainAttributes.getBoolean(4, false);
        instrumentation.info.functionalTest = typedArrayObtainAttributes.getBoolean(5, false);
        typedArrayObtainAttributes.recycle();
        if (instrumentation.info.targetPackage == null) {
            strArr[0] = "<instrumentation> does not specify targetPackage";
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        if (!parseAllMetaData(resources, xmlResourceParser, "<instrumentation>", instrumentation, strArr)) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        r12.instrumentation.add(instrumentation);
        return instrumentation;
    }

    private boolean parseBaseApplication(Package r25, Resources resources, XmlResourceParser xmlResourceParser, int i, String[] strArr) throws XmlPullParserException, IOException {
        TypedArray typedArray;
        String str;
        ApplicationInfo applicationInfo;
        String[] strArr2;
        CachedComponentArgs cachedComponentArgs;
        XmlResourceParser xmlResourceParser2;
        Resources resources2;
        Package r13;
        boolean z;
        String nonResourceString;
        Package r8 = r25;
        Resources resources3 = resources;
        XmlResourceParser xmlResourceParser3 = xmlResourceParser;
        ApplicationInfo applicationInfo2 = r8.applicationInfo;
        String str2 = r8.applicationInfo.packageName;
        TypedArray typedArrayObtainAttributes = resources3.obtainAttributes(xmlResourceParser3, R.styleable.AndroidManifestApplication);
        if (!parsePackageItemInfo(r8, applicationInfo2, strArr, "<application>", typedArrayObtainAttributes, false, 3, 1, 2, 42, 22, 30)) {
            typedArrayObtainAttributes.recycle();
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        if (applicationInfo2.name != null) {
            applicationInfo2.className = applicationInfo2.name;
        }
        String nonConfigurationString = typedArrayObtainAttributes.getNonConfigurationString(4, 1024);
        if (nonConfigurationString != null) {
            applicationInfo2.manageSpaceActivityName = buildClassName(str2, nonConfigurationString, strArr);
        }
        int i2 = 1;
        if (typedArrayObtainAttributes.getBoolean(17, true)) {
            applicationInfo2.flags |= 32768;
            String nonConfigurationString2 = typedArrayObtainAttributes.getNonConfigurationString(16, 1024);
            if (nonConfigurationString2 != null) {
                applicationInfo2.backupAgentName = buildClassName(str2, nonConfigurationString2, strArr);
                if (typedArrayObtainAttributes.getBoolean(18, true)) {
                    applicationInfo2.flags |= 65536;
                }
                if (typedArrayObtainAttributes.getBoolean(21, false)) {
                    applicationInfo2.flags |= 131072;
                }
                if (typedArrayObtainAttributes.getBoolean(32, false)) {
                    applicationInfo2.flags |= 67108864;
                }
                if (typedArrayObtainAttributes.getBoolean(40, false)) {
                    applicationInfo2.privateFlags |= 8192;
                }
            }
            TypedValue typedValuePeekValue = typedArrayObtainAttributes.peekValue(35);
            if (typedValuePeekValue != null) {
                int i3 = typedValuePeekValue.resourceId;
                applicationInfo2.fullBackupContent = i3;
                if (i3 == 0) {
                    applicationInfo2.fullBackupContent = typedValuePeekValue.data == 0 ? -1 : 0;
                }
            }
        }
        applicationInfo2.theme = typedArrayObtainAttributes.getResourceId(0, 0);
        applicationInfo2.descriptionRes = typedArrayObtainAttributes.getResourceId(13, 0);
        if (typedArrayObtainAttributes.getBoolean(8, false) && ((nonResourceString = typedArrayObtainAttributes.getNonResourceString(45)) == null || this.mCallback.hasFeature(nonResourceString))) {
            applicationInfo2.flags |= 8;
        }
        if (typedArrayObtainAttributes.getBoolean(27, false)) {
            r8.mRequiredForAllUsers = true;
        }
        String string = typedArrayObtainAttributes.getString(28);
        if (string != null && string.length() > 0) {
            r8.mRestrictedAccountType = string;
        }
        String string2 = typedArrayObtainAttributes.getString(29);
        if (string2 != null && string2.length() > 0) {
            r8.mRequiredAccountType = string2;
        }
        if (typedArrayObtainAttributes.getBoolean(10, false)) {
            applicationInfo2.flags |= 2;
        }
        if (typedArrayObtainAttributes.getBoolean(20, false)) {
            applicationInfo2.flags |= 16384;
        }
        r8.baseHardwareAccelerated = typedArrayObtainAttributes.getBoolean(23, r8.applicationInfo.targetSdkVersion >= 14);
        if (r8.baseHardwareAccelerated) {
            applicationInfo2.flags |= 536870912;
        }
        if (typedArrayObtainAttributes.getBoolean(7, true)) {
            applicationInfo2.flags |= 4;
        }
        if (typedArrayObtainAttributes.getBoolean(14, false)) {
            applicationInfo2.flags |= 32;
        }
        if (typedArrayObtainAttributes.getBoolean(5, true)) {
            applicationInfo2.flags |= 64;
        }
        if (r8.parentPackage == null && typedArrayObtainAttributes.getBoolean(15, false)) {
            applicationInfo2.flags |= 256;
        }
        if (typedArrayObtainAttributes.getBoolean(24, false)) {
            applicationInfo2.flags |= 1048576;
        }
        if (typedArrayObtainAttributes.getBoolean(36, r8.applicationInfo.targetSdkVersion < 28)) {
            applicationInfo2.flags |= 134217728;
        }
        if (typedArrayObtainAttributes.getBoolean(26, false)) {
            applicationInfo2.flags |= 4194304;
        }
        if (typedArrayObtainAttributes.getBoolean(33, false)) {
            applicationInfo2.flags |= Integer.MIN_VALUE;
        }
        if (typedArrayObtainAttributes.getBoolean(34, true)) {
            applicationInfo2.flags |= 268435456;
        }
        if (typedArrayObtainAttributes.getBoolean(38, false)) {
            applicationInfo2.privateFlags |= 32;
        }
        if (typedArrayObtainAttributes.getBoolean(39, false)) {
            applicationInfo2.privateFlags |= 64;
        }
        if (typedArrayObtainAttributes.hasValueOrEmpty(37)) {
            if (typedArrayObtainAttributes.getBoolean(37, true)) {
                applicationInfo2.privateFlags |= 1024;
            } else {
                applicationInfo2.privateFlags |= 2048;
            }
        } else if (r8.applicationInfo.targetSdkVersion >= 24) {
            applicationInfo2.privateFlags |= 4096;
        }
        applicationInfo2.maxAspectRatio = typedArrayObtainAttributes.getFloat(44, 0.0f);
        applicationInfo2.networkSecurityConfigRes = typedArrayObtainAttributes.getResourceId(41, 0);
        applicationInfo2.category = typedArrayObtainAttributes.getInt(43, -1);
        String nonConfigurationString3 = typedArrayObtainAttributes.getNonConfigurationString(6, 0);
        applicationInfo2.permission = (nonConfigurationString3 == null || nonConfigurationString3.length() <= 0) ? null : nonConfigurationString3.intern();
        applicationInfo2.taskAffinity = buildTaskAffinityName(applicationInfo2.packageName, applicationInfo2.packageName, r8.applicationInfo.targetSdkVersion >= 8 ? typedArrayObtainAttributes.getNonConfigurationString(12, 1024) : typedArrayObtainAttributes.getNonResourceString(12), strArr);
        String nonResourceString2 = typedArrayObtainAttributes.getNonResourceString(48);
        if (nonResourceString2 != null) {
            applicationInfo2.appComponentFactory = buildClassName(applicationInfo2.packageName, nonResourceString2, strArr);
        }
        if (strArr[0] == null) {
            typedArray = typedArrayObtainAttributes;
            str = str2;
            applicationInfo = applicationInfo2;
            strArr2 = strArr;
            applicationInfo.processName = buildProcessName(applicationInfo2.packageName, null, r8.applicationInfo.targetSdkVersion >= 8 ? typedArrayObtainAttributes.getNonConfigurationString(11, 1024) : typedArrayObtainAttributes.getNonResourceString(11), i, this.mSeparateProcesses, strArr);
            applicationInfo.enabled = typedArray.getBoolean(9, true);
            if (typedArray.getBoolean(31, false)) {
                applicationInfo.flags |= 33554432;
            }
            if (typedArray.getBoolean(47, false)) {
                applicationInfo.privateFlags |= 2;
                if (applicationInfo.processName != null && !applicationInfo.processName.equals(applicationInfo.packageName)) {
                    strArr2[0] = "cantSaveState applications can not use custom processes";
                }
            }
        } else {
            typedArray = typedArrayObtainAttributes;
            str = str2;
            applicationInfo = applicationInfo2;
            strArr2 = strArr;
        }
        applicationInfo.uiOptions = typedArray.getInt(25, 0);
        applicationInfo.classLoaderName = typedArray.getString(46);
        if (applicationInfo.classLoaderName != null && !ClassLoaderFactory.isValidClassLoaderName(applicationInfo.classLoaderName)) {
            strArr2[0] = "Invalid class loader name: " + applicationInfo.classLoaderName;
        }
        typedArray.recycle();
        if (strArr2[0] != null) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        int depth = xmlResourceParser.getDepth();
        CachedComponentArgs cachedComponentArgs2 = new CachedComponentArgs();
        boolean z2 = false;
        boolean z3 = false;
        boolean z4 = false;
        while (true) {
            int next = xmlResourceParser.next();
            if (next == i2 || (next == 3 && xmlResourceParser.getDepth() <= depth)) {
                break;
            }
            if (next == 3) {
                cachedComponentArgs = cachedComponentArgs2;
                xmlResourceParser2 = xmlResourceParser3;
                resources2 = resources3;
                r13 = r8;
            } else if (next == 4) {
                cachedComponentArgs = cachedComponentArgs2;
                xmlResourceParser2 = xmlResourceParser3;
                resources2 = resources3;
                r13 = r8;
            } else {
                String name = xmlResourceParser.getName();
                if (name.equals(Context.ACTIVITY_SERVICE)) {
                    cachedComponentArgs = cachedComponentArgs2;
                    r13 = r8;
                    Activity activity = parseActivity(r8, resources3, xmlResourceParser3, i, strArr2, cachedComponentArgs, false, r8.baseHardwareAccelerated);
                    if (activity == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    z = z2 | (activity.order != 0);
                    r13.activities.add(activity);
                } else {
                    cachedComponentArgs = cachedComponentArgs2;
                    r13 = r8;
                    if (name.equals("receiver")) {
                        Activity activity2 = parseActivity(r13, resources, xmlResourceParser, i, strArr2, cachedComponentArgs, true, false);
                        if (activity2 == null) {
                            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                            return false;
                        }
                        boolean z5 = activity2.order != 0;
                        r13.receivers.add(activity2);
                        xmlResourceParser2 = xmlResourceParser;
                        z3 |= z5;
                    } else if (name.equals("service")) {
                        Service service = parseService(r13, resources, xmlResourceParser, i, strArr2, cachedComponentArgs);
                        if (service == null) {
                            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                            return false;
                        }
                        boolean z6 = service.order != 0;
                        r13.services.add(service);
                        xmlResourceParser2 = xmlResourceParser;
                        z4 |= z6;
                    } else if (name.equals("provider")) {
                        Provider provider = parseProvider(r13, resources, xmlResourceParser, i, strArr2, cachedComponentArgs);
                        if (provider == null) {
                            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                            return false;
                        }
                        r13.providers.add(provider);
                        resources2 = resources;
                        xmlResourceParser2 = xmlResourceParser;
                    } else if (name.equals("activity-alias")) {
                        Activity activityAlias = parseActivityAlias(r13, resources, xmlResourceParser, i, strArr2, cachedComponentArgs);
                        if (activityAlias == null) {
                            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                            return false;
                        }
                        z = z2 | (activityAlias.order != 0);
                        r13.activities.add(activityAlias);
                    } else if (xmlResourceParser.getName().equals("meta-data")) {
                        resources2 = resources;
                        xmlResourceParser2 = xmlResourceParser;
                        Bundle metaData = parseMetaData(resources2, xmlResourceParser2, r13.mAppMetaData, strArr2);
                        r13.mAppMetaData = metaData;
                        if (metaData == null) {
                            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                            return false;
                        }
                    } else {
                        resources2 = resources;
                        xmlResourceParser2 = xmlResourceParser;
                        if (name.equals("static-library")) {
                            TypedArray typedArrayObtainAttributes2 = resources2.obtainAttributes(xmlResourceParser2, R.styleable.AndroidManifestStaticLibrary);
                            String nonResourceString3 = typedArrayObtainAttributes2.getNonResourceString(0);
                            int i4 = typedArrayObtainAttributes2.getInt(1, -1);
                            int i5 = typedArrayObtainAttributes2.getInt(2, 0);
                            typedArrayObtainAttributes2.recycle();
                            if (nonResourceString3 == null || i4 < 0) {
                                break;
                            }
                            if (r13.mSharedUserId != null) {
                                strArr2[0] = "sharedUserId not allowed in static shared library";
                                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID;
                                XmlUtils.skipCurrentTag(xmlResourceParser);
                                return false;
                            }
                            if (r13.staticSharedLibName != null) {
                                strArr2[0] = "Multiple static-shared libs for package " + str;
                                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                XmlUtils.skipCurrentTag(xmlResourceParser);
                                return false;
                            }
                            r13.staticSharedLibName = nonResourceString3.intern();
                            if (i4 >= 0) {
                                r13.staticSharedLibVersion = PackageInfo.composeLongVersionCode(i5, i4);
                            } else {
                                r13.staticSharedLibVersion = i4;
                            }
                            applicationInfo.privateFlags |= 16384;
                            XmlUtils.skipCurrentTag(xmlResourceParser);
                        } else if (name.equals("library")) {
                            TypedArray typedArrayObtainAttributes3 = resources2.obtainAttributes(xmlResourceParser2, R.styleable.AndroidManifestLibrary);
                            String nonResourceString4 = typedArrayObtainAttributes3.getNonResourceString(0);
                            typedArrayObtainAttributes3.recycle();
                            if (nonResourceString4 != null) {
                                String strIntern = nonResourceString4.intern();
                                if (!ArrayUtils.contains(r13.libraryNames, strIntern)) {
                                    r13.libraryNames = ArrayUtils.add(r13.libraryNames, strIntern);
                                }
                            }
                            XmlUtils.skipCurrentTag(xmlResourceParser);
                        } else if (name.equals("uses-static-library")) {
                            if (!parseUsesStaticLibrary(r13, resources2, xmlResourceParser2, strArr2)) {
                                return false;
                            }
                        } else if (name.equals("uses-library")) {
                            TypedArray typedArrayObtainAttributes4 = resources2.obtainAttributes(xmlResourceParser2, R.styleable.AndroidManifestUsesLibrary);
                            String nonResourceString5 = typedArrayObtainAttributes4.getNonResourceString(0);
                            boolean z7 = typedArrayObtainAttributes4.getBoolean(1, true);
                            typedArrayObtainAttributes4.recycle();
                            if (nonResourceString5 != null) {
                                String strIntern2 = nonResourceString5.intern();
                                if (z7) {
                                    r13.usesLibraries = ArrayUtils.add(r13.usesLibraries, strIntern2);
                                } else {
                                    r13.usesOptionalLibraries = ArrayUtils.add(r13.usesOptionalLibraries, strIntern2);
                                }
                            }
                            XmlUtils.skipCurrentTag(xmlResourceParser);
                        } else if (name.equals("uses-package")) {
                            XmlUtils.skipCurrentTag(xmlResourceParser);
                        } else {
                            Slog.w(TAG, "Unknown element under <application>: " + name + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                            XmlUtils.skipCurrentTag(xmlResourceParser);
                        }
                    }
                    resources2 = resources;
                }
                xmlResourceParser2 = xmlResourceParser;
                z2 = z;
                resources2 = resources;
            }
            resources3 = resources2;
            xmlResourceParser3 = xmlResourceParser2;
            r8 = r13;
            cachedComponentArgs2 = cachedComponentArgs;
            i2 = 1;
        }
    }

    private static boolean hasDomainURLs(Package r8) {
        if (r8 == null || r8.activities == null) {
            return false;
        }
        ArrayList<Activity> arrayList = r8.activities;
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            ArrayList<II> arrayList2 = arrayList.get(i).intents;
            if (arrayList2 != 0) {
                int size2 = arrayList2.size();
                for (int i2 = 0; i2 < size2; i2++) {
                    ActivityIntentInfo activityIntentInfo = (ActivityIntentInfo) arrayList2.get(i2);
                    if (activityIntentInfo.hasAction("android.intent.action.VIEW") && activityIntentInfo.hasAction("android.intent.action.VIEW") && (activityIntentInfo.hasDataScheme(IntentFilter.SCHEME_HTTP) || activityIntentInfo.hasDataScheme(IntentFilter.SCHEME_HTTPS))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean parseSplitApplication(Package r21, Resources resources, XmlResourceParser xmlResourceParser, int i, int i2, String[] strArr) throws XmlPullParserException, IOException {
        int i3;
        int i4;
        int i5;
        boolean z;
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestApplication);
        int i6 = 1;
        int i7 = 4;
        if (typedArrayObtainAttributes.getBoolean(7, true)) {
            int[] iArr = r21.splitFlags;
            iArr[i2] = iArr[i2] | 4;
        }
        String string = typedArrayObtainAttributes.getString(46);
        int i8 = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
        boolean z2 = false;
        if (string == null || ClassLoaderFactory.isValidClassLoaderName(string)) {
            r21.applicationInfo.splitClassLoaderNames[i2] = string;
            int depth = xmlResourceParser.getDepth();
            while (true) {
                int next = xmlResourceParser.next();
                if (next != i6) {
                    if (next != 3 || xmlResourceParser.getDepth() > depth) {
                        if (next != 3 && next != i7) {
                            ?? r0 = 0;
                            Object obj = null;
                            r0 = 0;
                            r0 = 0;
                            CachedComponentArgs cachedComponentArgs = new CachedComponentArgs();
                            String name = xmlResourceParser.getName();
                            if (name.equals(Context.ACTIVITY_SERVICE)) {
                                i3 = depth;
                                i4 = i8;
                                i5 = i7;
                                Activity activity = parseActivity(r21, resources, xmlResourceParser, i, strArr, cachedComponentArgs, false, r21.baseHardwareAccelerated);
                                if (activity == null) {
                                    this.mParseError = i4;
                                    return false;
                                }
                                r21.activities.add(activity);
                                obj = activity.info;
                            } else {
                                i3 = depth;
                                i4 = i8;
                                i5 = i7;
                                if (name.equals("receiver")) {
                                    Activity activity2 = parseActivity(r21, resources, xmlResourceParser, i, strArr, cachedComponentArgs, true, false);
                                    if (activity2 == null) {
                                        this.mParseError = i4;
                                        return false;
                                    }
                                    r21.receivers.add(activity2);
                                    obj = activity2.info;
                                } else if (name.equals("service")) {
                                    Service service = parseService(r21, resources, xmlResourceParser, i, strArr, cachedComponentArgs);
                                    if (service == null) {
                                        this.mParseError = i4;
                                        return false;
                                    }
                                    r21.services.add(service);
                                    obj = service.info;
                                } else if (name.equals("provider")) {
                                    Provider provider = parseProvider(r21, resources, xmlResourceParser, i, strArr, cachedComponentArgs);
                                    if (provider == null) {
                                        this.mParseError = i4;
                                        return false;
                                    }
                                    r21.providers.add(provider);
                                    obj = provider.info;
                                } else if (name.equals("activity-alias")) {
                                    Activity activityAlias = parseActivityAlias(r21, resources, xmlResourceParser, i, strArr, cachedComponentArgs);
                                    if (activityAlias == null) {
                                        this.mParseError = i4;
                                        return false;
                                    }
                                    r21.activities.add(activityAlias);
                                    obj = activityAlias.info;
                                } else if (xmlResourceParser.getName().equals("meta-data")) {
                                    Bundle metaData = parseMetaData(resources, xmlResourceParser, r21.mAppMetaData, strArr);
                                    r21.mAppMetaData = metaData;
                                    if (metaData == null) {
                                        this.mParseError = i4;
                                        return false;
                                    }
                                } else {
                                    z = false;
                                    if (name.equals("uses-static-library")) {
                                        if (!parseUsesStaticLibrary(r21, resources, xmlResourceParser, strArr)) {
                                            return false;
                                        }
                                    } else if (name.equals("uses-library")) {
                                        TypedArray typedArrayObtainAttributes2 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestUsesLibrary);
                                        String nonResourceString = typedArrayObtainAttributes2.getNonResourceString(0);
                                        boolean z3 = typedArrayObtainAttributes2.getBoolean(1, true);
                                        typedArrayObtainAttributes2.recycle();
                                        if (nonResourceString != null) {
                                            String strIntern = nonResourceString.intern();
                                            if (z3) {
                                                r21.usesLibraries = ArrayUtils.add(r21.usesLibraries, strIntern);
                                                r21.usesOptionalLibraries = ArrayUtils.remove(r21.usesOptionalLibraries, strIntern);
                                            } else if (!ArrayUtils.contains(r21.usesLibraries, strIntern)) {
                                                r21.usesOptionalLibraries = ArrayUtils.add(r21.usesOptionalLibraries, strIntern);
                                            }
                                        }
                                        XmlUtils.skipCurrentTag(xmlResourceParser);
                                    } else if (name.equals("uses-package")) {
                                        XmlUtils.skipCurrentTag(xmlResourceParser);
                                    } else {
                                        Slog.w(TAG, "Unknown element under <application>: " + name + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                                        XmlUtils.skipCurrentTag(xmlResourceParser);
                                        z2 = z;
                                        i8 = i4;
                                        i7 = i5;
                                        depth = i3;
                                        i6 = 1;
                                    }
                                    if (r0 != 0 && r0.splitName == null) {
                                        r0.splitName = r21.splitNames[i2];
                                    }
                                    z2 = z;
                                    i8 = i4;
                                    i7 = i5;
                                    depth = i3;
                                    i6 = 1;
                                }
                            }
                            z = false;
                            r0 = obj;
                            if (r0 != 0) {
                                r0.splitName = r21.splitNames[i2];
                            }
                            z2 = z;
                            i8 = i4;
                            i7 = i5;
                            depth = i3;
                            i6 = 1;
                        }
                    } else {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        } else {
            strArr[0] = "Invalid class loader name: " + string;
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
    }

    private static boolean parsePackageItemInfo(Package r1, PackageItemInfo packageItemInfo, String[] strArr, String str, TypedArray typedArray, boolean z, int i, int i2, int i3, int i4, int i5, int i6) {
        int resourceId;
        if (typedArray == null) {
            strArr[0] = str + " does not contain any attributes";
            return false;
        }
        String nonConfigurationString = typedArray.getNonConfigurationString(i, 0);
        if (nonConfigurationString == null) {
            if (z) {
                strArr[0] = str + " does not specify android:name";
                return false;
            }
        } else {
            packageItemInfo.name = buildClassName(r1.applicationInfo.packageName, nonConfigurationString, strArr);
            if (packageItemInfo.name == null) {
                return false;
            }
        }
        if (Resources.getSystem().getBoolean(R.bool.config_useRoundIcon)) {
            resourceId = typedArray.getResourceId(i4, 0);
        } else {
            resourceId = 0;
        }
        if (resourceId != 0) {
            packageItemInfo.icon = resourceId;
            packageItemInfo.nonLocalizedLabel = null;
        } else {
            int resourceId2 = typedArray.getResourceId(i3, 0);
            if (resourceId2 != 0) {
                packageItemInfo.icon = resourceId2;
                packageItemInfo.nonLocalizedLabel = null;
            }
        }
        int resourceId3 = typedArray.getResourceId(i5, 0);
        if (resourceId3 != 0) {
            packageItemInfo.logo = resourceId3;
        }
        int resourceId4 = typedArray.getResourceId(i6, 0);
        if (resourceId4 != 0) {
            packageItemInfo.banner = resourceId4;
        }
        TypedValue typedValuePeekValue = typedArray.peekValue(i2);
        if (typedValuePeekValue != null) {
            int i7 = typedValuePeekValue.resourceId;
            packageItemInfo.labelRes = i7;
            if (i7 == 0) {
                packageItemInfo.nonLocalizedLabel = typedValuePeekValue.coerceToString();
            }
        }
        packageItemInfo.packageName = r1.packageName;
        return true;
    }

    private Activity parseActivity(Package r24, Resources resources, XmlResourceParser xmlResourceParser, int i, String[] strArr, CachedComponentArgs cachedComponentArgs, boolean z, boolean z2) throws XmlPullParserException, IOException {
        int i2;
        Resources resources2;
        XmlResourceParser xmlResourceParser2;
        Package r6 = r24;
        Resources resources3 = resources;
        XmlResourceParser xmlResourceParser3 = xmlResourceParser;
        TypedArray typedArrayObtainAttributes = resources3.obtainAttributes(xmlResourceParser3, R.styleable.AndroidManifestActivity);
        if (cachedComponentArgs.mActivityArgs == null) {
            cachedComponentArgs.mActivityArgs = new ParseComponentArgs(r6, strArr, 3, 1, 2, 44, 23, 30, this.mSeparateProcesses, 7, 17, 5);
        }
        cachedComponentArgs.mActivityArgs.tag = z ? "<receiver>" : "<activity>";
        cachedComponentArgs.mActivityArgs.sa = typedArrayObtainAttributes;
        cachedComponentArgs.mActivityArgs.flags = i;
        Activity activity = new Activity(cachedComponentArgs.mActivityArgs, new ActivityInfo());
        if (strArr[0] != null) {
            typedArrayObtainAttributes.recycle();
            return null;
        }
        boolean zHasValue = typedArrayObtainAttributes.hasValue(6);
        if (zHasValue) {
            activity.info.exported = typedArrayObtainAttributes.getBoolean(6, false);
        }
        activity.info.theme = typedArrayObtainAttributes.getResourceId(0, 0);
        activity.info.uiOptions = typedArrayObtainAttributes.getInt(26, activity.info.applicationInfo.uiOptions);
        String nonConfigurationString = typedArrayObtainAttributes.getNonConfigurationString(27, 1024);
        if (nonConfigurationString != null) {
            String strBuildClassName = buildClassName(activity.info.packageName, nonConfigurationString, strArr);
            if (strArr[0] == null) {
                activity.info.parentActivityName = strBuildClassName;
            } else {
                Log.e(TAG, "Activity " + activity.info.name + " specified invalid parentActivityName " + nonConfigurationString);
                strArr[0] = null;
            }
        }
        int i3 = 4;
        String nonConfigurationString2 = typedArrayObtainAttributes.getNonConfigurationString(4, 0);
        if (nonConfigurationString2 == null) {
            activity.info.permission = r6.applicationInfo.permission;
        } else {
            activity.info.permission = nonConfigurationString2.length() > 0 ? nonConfigurationString2.toString().intern() : null;
        }
        activity.info.taskAffinity = buildTaskAffinityName(r6.applicationInfo.packageName, r6.applicationInfo.taskAffinity, typedArrayObtainAttributes.getNonConfigurationString(8, 1024), strArr);
        activity.info.splitName = typedArrayObtainAttributes.getNonConfigurationString(48, 0);
        activity.info.flags = 0;
        if (typedArrayObtainAttributes.getBoolean(9, false)) {
            activity.info.flags |= 1;
        }
        if (typedArrayObtainAttributes.getBoolean(10, false)) {
            activity.info.flags |= 2;
        }
        if (typedArrayObtainAttributes.getBoolean(11, false)) {
            activity.info.flags |= 4;
        }
        if (typedArrayObtainAttributes.getBoolean(21, false)) {
            activity.info.flags |= 128;
        }
        if (typedArrayObtainAttributes.getBoolean(18, false)) {
            ActivityInfo activityInfo = activity.info;
            activityInfo.flags = 8 | activityInfo.flags;
        }
        if (typedArrayObtainAttributes.getBoolean(12, false)) {
            activity.info.flags |= 16;
        }
        if (typedArrayObtainAttributes.getBoolean(13, false)) {
            activity.info.flags |= 32;
        }
        if (typedArrayObtainAttributes.getBoolean(19, (r6.applicationInfo.flags & 32) != 0)) {
            activity.info.flags |= 64;
        }
        if (typedArrayObtainAttributes.getBoolean(22, false)) {
            activity.info.flags |= 256;
        }
        if (typedArrayObtainAttributes.getBoolean(29, false) || typedArrayObtainAttributes.getBoolean(39, false)) {
            ActivityInfo activityInfo2 = activity.info;
            activityInfo2.flags = 1024 | activityInfo2.flags;
        }
        if (typedArrayObtainAttributes.getBoolean(24, false)) {
            activity.info.flags |= 2048;
        }
        if (typedArrayObtainAttributes.getBoolean(54, false)) {
            activity.info.flags |= 536870912;
        }
        if (z) {
            activity.info.launchMode = 0;
            activity.info.configChanges = 0;
            if (typedArrayObtainAttributes.getBoolean(28, false)) {
                activity.info.flags |= 1073741824;
            }
            ActivityInfo activityInfo3 = activity.info;
            ActivityInfo activityInfo4 = activity.info;
            boolean z3 = typedArrayObtainAttributes.getBoolean(42, false);
            activityInfo4.directBootAware = z3;
            activityInfo3.encryptionAware = z3;
        } else {
            if (typedArrayObtainAttributes.getBoolean(25, z2)) {
                activity.info.flags |= 512;
            }
            activity.info.launchMode = typedArrayObtainAttributes.getInt(14, 0);
            activity.info.documentLaunchMode = typedArrayObtainAttributes.getInt(33, 0);
            activity.info.maxRecents = typedArrayObtainAttributes.getInt(34, ActivityManager.getDefaultAppRecentsLimitStatic());
            activity.info.configChanges = getActivityConfigChanges(typedArrayObtainAttributes.getInt(16, 0), typedArrayObtainAttributes.getInt(47, 0));
            activity.info.softInputMode = typedArrayObtainAttributes.getInt(20, 0);
            activity.info.persistableMode = typedArrayObtainAttributes.getInteger(32, 0);
            if (typedArrayObtainAttributes.getBoolean(31, false)) {
                activity.info.flags |= Integer.MIN_VALUE;
            }
            if (typedArrayObtainAttributes.getBoolean(35, false)) {
                activity.info.flags |= 8192;
            }
            if (typedArrayObtainAttributes.getBoolean(36, false)) {
                activity.info.flags |= 4096;
            }
            if (typedArrayObtainAttributes.getBoolean(37, false)) {
                activity.info.flags |= 16384;
            }
            activity.info.screenOrientation = typedArrayObtainAttributes.getInt(15, -1);
            setActivityResizeMode(activity.info, typedArrayObtainAttributes, r6);
            if (typedArrayObtainAttributes.getBoolean(41, false)) {
                activity.info.flags |= 4194304;
            }
            if (typedArrayObtainAttributes.getBoolean(53, false)) {
                activity.info.flags |= 262144;
            }
            if (typedArrayObtainAttributes.hasValue(50) && typedArrayObtainAttributes.getType(50) == 4) {
                activity.setMaxAspectRatio(typedArrayObtainAttributes.getFloat(50, 0.0f));
            }
            activity.info.lockTaskLaunchMode = typedArrayObtainAttributes.getInt(38, 0);
            ActivityInfo activityInfo5 = activity.info;
            ActivityInfo activityInfo6 = activity.info;
            boolean z4 = typedArrayObtainAttributes.getBoolean(42, false);
            activityInfo6.directBootAware = z4;
            activityInfo5.encryptionAware = z4;
            activity.info.requestedVrComponent = typedArrayObtainAttributes.getString(43);
            activity.info.rotationAnimation = typedArrayObtainAttributes.getInt(46, -1);
            activity.info.colorMode = typedArrayObtainAttributes.getInt(49, 0);
            if (typedArrayObtainAttributes.getBoolean(51, false)) {
                activity.info.flags |= 8388608;
            }
            if (typedArrayObtainAttributes.getBoolean(52, false)) {
                activity.info.flags |= 16777216;
            }
        }
        if (activity.info.directBootAware) {
            r6.applicationInfo.privateFlags |= 256;
        }
        boolean z5 = typedArrayObtainAttributes.getBoolean(45, false);
        if (z5) {
            activity.info.flags |= 1048576;
            r6.visibleToInstantApps = true;
        }
        typedArrayObtainAttributes.recycle();
        if (z) {
            i2 = 2;
            if ((r6.applicationInfo.privateFlags & 2) != 0 && activity.info.processName == r6.packageName) {
                strArr[0] = "Heavy-weight applications can not have receivers in main process";
            }
        } else {
            i2 = 2;
        }
        if (strArr[0] != null) {
            return null;
        }
        int depth = xmlResourceParser.getDepth();
        while (true) {
            int next = xmlResourceParser.next();
            if (next == 1 || (next == 3 && xmlResourceParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != i3) {
                if (xmlResourceParser.getName().equals("intent-filter")) {
                    ActivityIntentInfo activityIntentInfo = new ActivityIntentInfo(activity);
                    Package r9 = r6;
                    if (!parseIntent(resources3, xmlResourceParser3, true, true, activityIntentInfo, strArr)) {
                        return null;
                    }
                    if (activityIntentInfo.countActions() == 0) {
                        Slog.w(TAG, "No actions in intent filter at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                    } else {
                        activity.order = Math.max(activityIntentInfo.getOrder(), activity.order);
                        activity.intents.add(activityIntentInfo);
                    }
                    activityIntentInfo.setVisibilityToInstantApp(z5 ? 1 : (z || !isImplicitlyExposedIntent(activityIntentInfo)) ? 0 : i2);
                    if (activityIntentInfo.isVisibleToInstantApp()) {
                        activity.info.flags |= 1048576;
                    }
                    if (activityIntentInfo.isImplicitlyVisibleToInstantApp()) {
                        activity.info.flags |= 2097152;
                    }
                    resources3 = resources;
                    xmlResourceParser3 = xmlResourceParser;
                    r6 = r9;
                } else {
                    Package r92 = r6;
                    if (z || !xmlResourceParser.getName().equals("preferred")) {
                        if (xmlResourceParser.getName().equals("meta-data")) {
                            resources2 = resources;
                            xmlResourceParser2 = xmlResourceParser;
                            Bundle metaData = parseMetaData(resources2, xmlResourceParser2, activity.metaData, strArr);
                            activity.metaData = metaData;
                            if (metaData == null) {
                                return null;
                            }
                        } else {
                            resources2 = resources;
                            xmlResourceParser2 = xmlResourceParser;
                            if (z || !xmlResourceParser.getName().equals(TtmlUtils.TAG_LAYOUT)) {
                                Slog.w(TAG, "Problem in package " + this.mArchiveSourcePath + SettingsStringUtil.DELIMITER);
                                if (z) {
                                    Slog.w(TAG, "Unknown element under <receiver>: " + xmlResourceParser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                                } else {
                                    Slog.w(TAG, "Unknown element under <activity>: " + xmlResourceParser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                                }
                                XmlUtils.skipCurrentTag(xmlResourceParser);
                            } else {
                                parseLayout(resources2, xmlResourceParser2, activity);
                            }
                        }
                        resources3 = resources2;
                        xmlResourceParser3 = xmlResourceParser2;
                    } else {
                        ActivityIntentInfo activityIntentInfo2 = new ActivityIntentInfo(activity);
                        if (!parseIntent(resources, xmlResourceParser, false, false, activityIntentInfo2, strArr)) {
                            return null;
                        }
                        if (activityIntentInfo2.countActions() == 0) {
                            Slog.w(TAG, "No actions in preferred at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                        } else {
                            if (r92.preferredActivityFilters == null) {
                                r92.preferredActivityFilters = new ArrayList<>();
                            }
                            r92.preferredActivityFilters.add(activityIntentInfo2);
                        }
                        activityIntentInfo2.setVisibilityToInstantApp(z5 ? 1 : (z || !isImplicitlyExposedIntent(activityIntentInfo2)) ? 0 : i2);
                        if (activityIntentInfo2.isVisibleToInstantApp()) {
                            activity.info.flags |= 1048576;
                        }
                        if (activityIntentInfo2.isImplicitlyVisibleToInstantApp()) {
                            activity.info.flags |= 2097152;
                        }
                        resources3 = resources;
                        xmlResourceParser3 = xmlResourceParser;
                    }
                    r6 = r92;
                    i3 = 4;
                }
            }
        }
    }

    private void setActivityResizeMode(ActivityInfo activityInfo, TypedArray typedArray, Package r8) {
        boolean z = (r8.applicationInfo.privateFlags & 3072) != 0;
        if (typedArray.hasValue(40) || z) {
            if (typedArray.getBoolean(40, (r8.applicationInfo.privateFlags & 1024) != 0)) {
                activityInfo.resizeMode = 2;
                return;
            } else {
                activityInfo.resizeMode = 0;
                return;
            }
        }
        if ((r8.applicationInfo.privateFlags & 4096) != 0) {
            activityInfo.resizeMode = 1;
            return;
        }
        if (activityInfo.isFixedOrientationPortrait()) {
            activityInfo.resizeMode = 6;
            return;
        }
        if (activityInfo.isFixedOrientationLandscape()) {
            activityInfo.resizeMode = 5;
        } else if (activityInfo.isFixedOrientation()) {
            activityInfo.resizeMode = 7;
        } else {
            activityInfo.resizeMode = 4;
        }
    }

    private void setMaxAspectRatio(Package r5) {
        float f;
        float f2;
        if (r5.applicationInfo.targetSdkVersion < 26) {
            f = DEFAULT_PRE_O_MAX_ASPECT_RATIO;
        } else {
            f = 0.0f;
        }
        if (r5.applicationInfo.maxAspectRatio != 0.0f) {
            f = r5.applicationInfo.maxAspectRatio;
        } else if (r5.mAppMetaData != null && r5.mAppMetaData.containsKey(METADATA_MAX_ASPECT_RATIO)) {
            f = r5.mAppMetaData.getFloat(METADATA_MAX_ASPECT_RATIO, f);
        }
        for (Activity activity : r5.activities) {
            if (!activity.hasMaxAspectRatio()) {
                if (activity.metaData != null) {
                    f2 = activity.metaData.getFloat(METADATA_MAX_ASPECT_RATIO, f);
                } else {
                    f2 = f;
                }
                activity.setMaxAspectRatio(f2);
            }
        }
    }

    public static int getActivityConfigChanges(int i, int i2) {
        return i | ((~i2) & 3);
    }

    private void parseLayout(Resources resources, AttributeSet attributeSet, Activity activity) {
        float fraction;
        int dimensionPixelSize;
        int type;
        int dimensionPixelSize2;
        float f;
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(attributeSet, R.styleable.AndroidManifestLayout);
        int type2 = typedArrayObtainAttributes.getType(3);
        float fraction2 = -1.0f;
        if (type2 == 6) {
            fraction = typedArrayObtainAttributes.getFraction(3, 1, 1, -1.0f);
        } else {
            if (type2 == 5) {
                dimensionPixelSize = typedArrayObtainAttributes.getDimensionPixelSize(3, -1);
                fraction = -1.0f;
                type = typedArrayObtainAttributes.getType(4);
                if (type != 6) {
                    fraction2 = typedArrayObtainAttributes.getFraction(4, 1, 1, -1.0f);
                } else {
                    if (type == 5) {
                        dimensionPixelSize2 = typedArrayObtainAttributes.getDimensionPixelSize(4, -1);
                        f = -1.0f;
                    }
                    int i = typedArrayObtainAttributes.getInt(0, 17);
                    int dimensionPixelSize3 = typedArrayObtainAttributes.getDimensionPixelSize(1, -1);
                    int dimensionPixelSize4 = typedArrayObtainAttributes.getDimensionPixelSize(2, -1);
                    typedArrayObtainAttributes.recycle();
                    activity.info.windowLayout = new ActivityInfo.WindowLayout(dimensionPixelSize, fraction, dimensionPixelSize2, f, i, dimensionPixelSize3, dimensionPixelSize4);
                }
                f = fraction2;
                dimensionPixelSize2 = -1;
                int i2 = typedArrayObtainAttributes.getInt(0, 17);
                int dimensionPixelSize32 = typedArrayObtainAttributes.getDimensionPixelSize(1, -1);
                int dimensionPixelSize42 = typedArrayObtainAttributes.getDimensionPixelSize(2, -1);
                typedArrayObtainAttributes.recycle();
                activity.info.windowLayout = new ActivityInfo.WindowLayout(dimensionPixelSize, fraction, dimensionPixelSize2, f, i2, dimensionPixelSize32, dimensionPixelSize42);
            }
            fraction = -1.0f;
        }
        dimensionPixelSize = -1;
        type = typedArrayObtainAttributes.getType(4);
        if (type != 6) {
        }
        f = fraction2;
        dimensionPixelSize2 = -1;
        int i22 = typedArrayObtainAttributes.getInt(0, 17);
        int dimensionPixelSize322 = typedArrayObtainAttributes.getDimensionPixelSize(1, -1);
        int dimensionPixelSize422 = typedArrayObtainAttributes.getDimensionPixelSize(2, -1);
        typedArrayObtainAttributes.recycle();
        activity.info.windowLayout = new ActivityInfo.WindowLayout(dimensionPixelSize, fraction, dimensionPixelSize2, f, i22, dimensionPixelSize322, dimensionPixelSize422);
    }

    private Activity parseActivityAlias(Package r29, Resources resources, XmlResourceParser xmlResourceParser, int i, String[] strArr, CachedComponentArgs cachedComponentArgs) throws XmlPullParserException, IOException {
        String str;
        String str2;
        Activity activity;
        Resources resources2;
        XmlResourceParser xmlResourceParser2;
        String[] strArr2;
        Resources resources3 = resources;
        XmlResourceParser xmlResourceParser3 = xmlResourceParser;
        TypedArray typedArrayObtainAttributes = resources3.obtainAttributes(xmlResourceParser3, R.styleable.AndroidManifestActivityAlias);
        String nonConfigurationString = typedArrayObtainAttributes.getNonConfigurationString(7, 1024);
        if (nonConfigurationString == null) {
            strArr[0] = "<activity-alias> does not specify android:targetActivity";
            typedArrayObtainAttributes.recycle();
            return null;
        }
        String strBuildClassName = buildClassName(r29.applicationInfo.packageName, nonConfigurationString, strArr);
        if (strBuildClassName == null) {
            typedArrayObtainAttributes.recycle();
            return null;
        }
        if (cachedComponentArgs.mActivityAliasArgs == null) {
            str = strBuildClassName;
            cachedComponentArgs.mActivityAliasArgs = new ParseComponentArgs(r29, strArr, 2, 0, 1, 11, 8, 10, this.mSeparateProcesses, 0, 6, 4);
            cachedComponentArgs.mActivityAliasArgs.tag = "<activity-alias>";
        } else {
            str = strBuildClassName;
        }
        cachedComponentArgs.mActivityAliasArgs.sa = typedArrayObtainAttributes;
        cachedComponentArgs.mActivityAliasArgs.flags = i;
        int size = r29.activities.size();
        int i2 = 0;
        while (true) {
            if (i2 < size) {
                activity = r29.activities.get(i2);
                str2 = str;
                if (str2.equals(activity.info.name)) {
                    break;
                }
                i2++;
                str = str2;
            } else {
                str2 = str;
                activity = null;
                break;
            }
        }
        if (activity == null) {
            strArr[0] = "<activity-alias> target activity " + str2 + " not found in manifest";
            typedArrayObtainAttributes.recycle();
            return null;
        }
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.targetActivity = str2;
        activityInfo.configChanges = activity.info.configChanges;
        activityInfo.flags = activity.info.flags;
        activityInfo.icon = activity.info.icon;
        activityInfo.logo = activity.info.logo;
        activityInfo.banner = activity.info.banner;
        activityInfo.labelRes = activity.info.labelRes;
        activityInfo.nonLocalizedLabel = activity.info.nonLocalizedLabel;
        activityInfo.launchMode = activity.info.launchMode;
        activityInfo.lockTaskLaunchMode = activity.info.lockTaskLaunchMode;
        activityInfo.processName = activity.info.processName;
        if (activityInfo.descriptionRes == 0) {
            activityInfo.descriptionRes = activity.info.descriptionRes;
        }
        activityInfo.screenOrientation = activity.info.screenOrientation;
        activityInfo.taskAffinity = activity.info.taskAffinity;
        activityInfo.theme = activity.info.theme;
        activityInfo.softInputMode = activity.info.softInputMode;
        activityInfo.uiOptions = activity.info.uiOptions;
        activityInfo.parentActivityName = activity.info.parentActivityName;
        activityInfo.maxRecents = activity.info.maxRecents;
        activityInfo.windowLayout = activity.info.windowLayout;
        activityInfo.resizeMode = activity.info.resizeMode;
        activityInfo.maxAspectRatio = activity.info.maxAspectRatio;
        activityInfo.requestedVrComponent = activity.info.requestedVrComponent;
        boolean z = activity.info.directBootAware;
        activityInfo.directBootAware = z;
        activityInfo.encryptionAware = z;
        Activity activity2 = new Activity(cachedComponentArgs.mActivityAliasArgs, activityInfo);
        if (strArr[0] != null) {
            typedArrayObtainAttributes.recycle();
            return null;
        }
        boolean zHasValue = typedArrayObtainAttributes.hasValue(5);
        if (zHasValue) {
            activity2.info.exported = typedArrayObtainAttributes.getBoolean(5, false);
        }
        String nonConfigurationString2 = typedArrayObtainAttributes.getNonConfigurationString(3, 0);
        if (nonConfigurationString2 != null) {
            activity2.info.permission = nonConfigurationString2.length() > 0 ? nonConfigurationString2.toString().intern() : null;
        }
        String nonConfigurationString3 = typedArrayObtainAttributes.getNonConfigurationString(9, 1024);
        if (nonConfigurationString3 != null) {
            String strBuildClassName2 = buildClassName(activity2.info.packageName, nonConfigurationString3, strArr);
            if (strArr[0] == null) {
                activity2.info.parentActivityName = strBuildClassName2;
            } else {
                Log.e(TAG, "Activity alias " + activity2.info.name + " specified invalid parentActivityName " + nonConfigurationString3);
                strArr[0] = null;
            }
        }
        boolean z2 = (activity2.info.flags & 1048576) != 0;
        typedArrayObtainAttributes.recycle();
        if (strArr[0] != null) {
            return null;
        }
        int depth = xmlResourceParser.getDepth();
        while (true) {
            int next = xmlResourceParser.next();
            if (next == 1 || (next == 3 && xmlResourceParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != 4) {
                if (xmlResourceParser.getName().equals("intent-filter")) {
                    ActivityIntentInfo activityIntentInfo = new ActivityIntentInfo(activity2);
                    resources2 = resources3;
                    if (!parseIntent(resources3, xmlResourceParser3, true, true, activityIntentInfo, strArr)) {
                        return null;
                    }
                    if (activityIntentInfo.countActions() == 0) {
                        Slog.w(TAG, "No actions in intent filter at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                    } else {
                        activity2.order = Math.max(activityIntentInfo.getOrder(), activity2.order);
                        activity2.intents.add(activityIntentInfo);
                    }
                    activityIntentInfo.setVisibilityToInstantApp(z2 ? 1 : isImplicitlyExposedIntent(activityIntentInfo) ? 2 : 0);
                    if (activityIntentInfo.isVisibleToInstantApp()) {
                        activity2.info.flags |= 1048576;
                    }
                    if (activityIntentInfo.isImplicitlyVisibleToInstantApp()) {
                        activity2.info.flags |= 2097152;
                    }
                    xmlResourceParser3 = xmlResourceParser;
                } else {
                    resources2 = resources3;
                    if (xmlResourceParser.getName().equals("meta-data")) {
                        xmlResourceParser2 = xmlResourceParser;
                        strArr2 = strArr;
                        Bundle metaData = parseMetaData(resources2, xmlResourceParser2, activity2.metaData, strArr2);
                        activity2.metaData = metaData;
                        if (metaData == null) {
                            return null;
                        }
                    } else {
                        xmlResourceParser2 = xmlResourceParser;
                        strArr2 = strArr;
                        Slog.w(TAG, "Unknown element under <activity-alias>: " + xmlResourceParser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                        XmlUtils.skipCurrentTag(xmlResourceParser);
                    }
                    xmlResourceParser3 = xmlResourceParser2;
                }
                resources3 = resources2;
            }
        }
        if (!zHasValue) {
            activity2.info.exported = activity2.intents.size() > 0;
        }
        return activity2;
    }

    private Provider parseProvider(Package r22, Resources resources, XmlResourceParser xmlResourceParser, int i, String[] strArr, CachedComponentArgs cachedComponentArgs) throws XmlPullParserException, IOException {
        TypedArray typedArray;
        boolean z;
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestProvider);
        if (cachedComponentArgs.mProviderArgs == null) {
            typedArray = typedArrayObtainAttributes;
            cachedComponentArgs.mProviderArgs = new ParseComponentArgs(r22, strArr, 2, 0, 1, 19, 15, 17, this.mSeparateProcesses, 8, 14, 6);
            cachedComponentArgs.mProviderArgs.tag = "<provider>";
        } else {
            typedArray = typedArrayObtainAttributes;
        }
        TypedArray typedArray2 = typedArray;
        cachedComponentArgs.mProviderArgs.sa = typedArray2;
        cachedComponentArgs.mProviderArgs.flags = i;
        Provider provider = new Provider(cachedComponentArgs.mProviderArgs, new ProviderInfo());
        if (strArr[0] != null) {
            typedArray2.recycle();
            return null;
        }
        if (r22.applicationInfo.targetSdkVersion >= 17) {
            z = false;
        } else {
            z = true;
        }
        provider.info.exported = typedArray2.getBoolean(7, z);
        String nonConfigurationString = typedArray2.getNonConfigurationString(10, 0);
        provider.info.isSyncable = typedArray2.getBoolean(11, false);
        String nonConfigurationString2 = typedArray2.getNonConfigurationString(3, 0);
        String nonConfigurationString3 = typedArray2.getNonConfigurationString(4, 0);
        if (nonConfigurationString3 == null) {
            nonConfigurationString3 = nonConfigurationString2;
        }
        if (nonConfigurationString3 == null) {
            provider.info.readPermission = r22.applicationInfo.permission;
        } else {
            provider.info.readPermission = nonConfigurationString3.length() > 0 ? nonConfigurationString3.toString().intern() : null;
        }
        String nonConfigurationString4 = typedArray2.getNonConfigurationString(5, 0);
        if (nonConfigurationString4 != null) {
            nonConfigurationString2 = nonConfigurationString4;
        }
        if (nonConfigurationString2 == null) {
            provider.info.writePermission = r22.applicationInfo.permission;
        } else {
            provider.info.writePermission = nonConfigurationString2.length() > 0 ? nonConfigurationString2.toString().intern() : null;
        }
        provider.info.grantUriPermissions = typedArray2.getBoolean(13, false);
        provider.info.multiprocess = typedArray2.getBoolean(9, false);
        provider.info.initOrder = typedArray2.getInt(12, 0);
        provider.info.splitName = typedArray2.getNonConfigurationString(21, 0);
        provider.info.flags = 0;
        if (typedArray2.getBoolean(16, false)) {
            provider.info.flags |= 1073741824;
        }
        ProviderInfo providerInfo = provider.info;
        ProviderInfo providerInfo2 = provider.info;
        boolean z2 = typedArray2.getBoolean(18, false);
        providerInfo2.directBootAware = z2;
        providerInfo.encryptionAware = z2;
        if (provider.info.directBootAware) {
            r22.applicationInfo.privateFlags |= 256;
        }
        boolean z3 = typedArray2.getBoolean(20, false);
        if (z3) {
            provider.info.flags |= 1048576;
            r22.visibleToInstantApps = true;
        }
        typedArray2.recycle();
        if ((r22.applicationInfo.privateFlags & 2) != 0 && provider.info.processName == r22.packageName) {
            strArr[0] = "Heavy-weight applications can not have providers in main process";
            return null;
        }
        if (nonConfigurationString == null) {
            strArr[0] = "<provider> does not include authorities attribute";
            return null;
        }
        if (nonConfigurationString.length() <= 0) {
            strArr[0] = "<provider> has empty authorities attribute";
            return null;
        }
        provider.info.authority = nonConfigurationString.intern();
        if (parseProviderTags(resources, xmlResourceParser, z3, provider, strArr)) {
            return provider;
        }
        return null;
    }

    private boolean parseProviderTags(Resources resources, XmlResourceParser xmlResourceParser, boolean z, Provider provider, String[] strArr) throws XmlPullParserException, IOException {
        PatternMatcher patternMatcher;
        String strIntern;
        boolean z2;
        int depth = xmlResourceParser.getDepth();
        while (true) {
            int next = xmlResourceParser.next();
            if (next == 1 || (next == 3 && xmlResourceParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != 4) {
                if (xmlResourceParser.getName().equals("intent-filter")) {
                    ProviderIntentInfo providerIntentInfo = new ProviderIntentInfo(provider);
                    if (!parseIntent(resources, xmlResourceParser, true, false, providerIntentInfo, strArr)) {
                        return false;
                    }
                    if (z) {
                        providerIntentInfo.setVisibilityToInstantApp(1);
                        provider.info.flags |= 1048576;
                    }
                    provider.order = Math.max(providerIntentInfo.getOrder(), provider.order);
                    provider.intents.add(providerIntentInfo);
                } else if (xmlResourceParser.getName().equals("meta-data")) {
                    Bundle metaData = parseMetaData(resources, xmlResourceParser, provider.metaData, strArr);
                    provider.metaData = metaData;
                    if (metaData == null) {
                        return false;
                    }
                } else if (xmlResourceParser.getName().equals("grant-uri-permission")) {
                    TypedArray typedArrayObtainAttributes = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestGrantUriPermission);
                    String nonConfigurationString = typedArrayObtainAttributes.getNonConfigurationString(0, 0);
                    if (nonConfigurationString != null) {
                        patternMatcher = new PatternMatcher(nonConfigurationString, 0);
                    } else {
                        patternMatcher = null;
                    }
                    String nonConfigurationString2 = typedArrayObtainAttributes.getNonConfigurationString(1, 0);
                    if (nonConfigurationString2 != null) {
                        patternMatcher = new PatternMatcher(nonConfigurationString2, 1);
                    }
                    String nonConfigurationString3 = typedArrayObtainAttributes.getNonConfigurationString(2, 0);
                    if (nonConfigurationString3 != null) {
                        patternMatcher = new PatternMatcher(nonConfigurationString3, 2);
                    }
                    typedArrayObtainAttributes.recycle();
                    if (patternMatcher != null) {
                        if (provider.info.uriPermissionPatterns == null) {
                            provider.info.uriPermissionPatterns = new PatternMatcher[1];
                            provider.info.uriPermissionPatterns[0] = patternMatcher;
                        } else {
                            int length = provider.info.uriPermissionPatterns.length;
                            PatternMatcher[] patternMatcherArr = new PatternMatcher[length + 1];
                            System.arraycopy(provider.info.uriPermissionPatterns, 0, patternMatcherArr, 0, length);
                            patternMatcherArr[length] = patternMatcher;
                            provider.info.uriPermissionPatterns = patternMatcherArr;
                        }
                        provider.info.grantUriPermissions = true;
                        XmlUtils.skipCurrentTag(xmlResourceParser);
                    } else {
                        Slog.w(TAG, "Unknown element under <path-permission>: " + xmlResourceParser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                        XmlUtils.skipCurrentTag(xmlResourceParser);
                    }
                } else if (xmlResourceParser.getName().equals("path-permission")) {
                    TypedArray typedArrayObtainAttributes2 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestPathPermission);
                    String nonConfigurationString4 = typedArrayObtainAttributes2.getNonConfigurationString(0, 0);
                    String nonConfigurationString5 = typedArrayObtainAttributes2.getNonConfigurationString(1, 0);
                    if (nonConfigurationString5 == null) {
                        nonConfigurationString5 = nonConfigurationString4;
                    }
                    String nonConfigurationString6 = typedArrayObtainAttributes2.getNonConfigurationString(2, 0);
                    if (nonConfigurationString6 != null) {
                        nonConfigurationString4 = nonConfigurationString6;
                    }
                    if (nonConfigurationString5 != null) {
                        strIntern = nonConfigurationString5.intern();
                        z2 = true;
                    } else {
                        strIntern = nonConfigurationString5;
                        z2 = false;
                    }
                    if (nonConfigurationString4 != null) {
                        nonConfigurationString4 = nonConfigurationString4.intern();
                        z2 = true;
                    }
                    if (!z2) {
                        Slog.w(TAG, "No readPermission or writePermssion for <path-permission>: " + xmlResourceParser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                        XmlUtils.skipCurrentTag(xmlResourceParser);
                    } else {
                        String nonConfigurationString7 = typedArrayObtainAttributes2.getNonConfigurationString(3, 0);
                        PathPermission pathPermission = nonConfigurationString7 != null ? new PathPermission(nonConfigurationString7, 0, strIntern, nonConfigurationString4) : null;
                        String nonConfigurationString8 = typedArrayObtainAttributes2.getNonConfigurationString(4, 0);
                        if (nonConfigurationString8 != null) {
                            pathPermission = new PathPermission(nonConfigurationString8, 1, strIntern, nonConfigurationString4);
                        }
                        String nonConfigurationString9 = typedArrayObtainAttributes2.getNonConfigurationString(5, 0);
                        if (nonConfigurationString9 != null) {
                            pathPermission = new PathPermission(nonConfigurationString9, 2, strIntern, nonConfigurationString4);
                        }
                        String nonConfigurationString10 = typedArrayObtainAttributes2.getNonConfigurationString(6, 0);
                        if (nonConfigurationString10 != null) {
                            pathPermission = new PathPermission(nonConfigurationString10, 3, strIntern, nonConfigurationString4);
                        }
                        typedArrayObtainAttributes2.recycle();
                        if (pathPermission != null) {
                            if (provider.info.pathPermissions == null) {
                                provider.info.pathPermissions = new PathPermission[1];
                                provider.info.pathPermissions[0] = pathPermission;
                            } else {
                                int length2 = provider.info.pathPermissions.length;
                                PathPermission[] pathPermissionArr = new PathPermission[length2 + 1];
                                System.arraycopy(provider.info.pathPermissions, 0, pathPermissionArr, 0, length2);
                                pathPermissionArr[length2] = pathPermission;
                                provider.info.pathPermissions = pathPermissionArr;
                            }
                            XmlUtils.skipCurrentTag(xmlResourceParser);
                        } else {
                            Slog.w(TAG, "No path, pathPrefix, or pathPattern for <path-permission>: " + xmlResourceParser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                            XmlUtils.skipCurrentTag(xmlResourceParser);
                        }
                    }
                } else {
                    Slog.w(TAG, "Unknown element under <provider>: " + xmlResourceParser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                    XmlUtils.skipCurrentTag(xmlResourceParser);
                }
            }
        }
    }

    private Service parseService(Package r24, Resources resources, XmlResourceParser xmlResourceParser, int i, String[] strArr, CachedComponentArgs cachedComponentArgs) throws XmlPullParserException, IOException {
        int i2;
        Resources resources2;
        XmlResourceParser xmlResourceParser2;
        String[] strArr2;
        ServiceIntentInfo serviceIntentInfo;
        Resources resources3 = resources;
        XmlResourceParser xmlResourceParser3 = xmlResourceParser;
        TypedArray typedArrayObtainAttributes = resources3.obtainAttributes(xmlResourceParser3, R.styleable.AndroidManifestService);
        if (cachedComponentArgs.mServiceArgs == null) {
            cachedComponentArgs.mServiceArgs = new ParseComponentArgs(r24, strArr, 2, 0, 1, 15, 8, 12, this.mSeparateProcesses, 6, 7, 4);
            cachedComponentArgs.mServiceArgs.tag = "<service>";
        }
        cachedComponentArgs.mServiceArgs.sa = typedArrayObtainAttributes;
        cachedComponentArgs.mServiceArgs.flags = i;
        Service service = new Service(cachedComponentArgs.mServiceArgs, new ServiceInfo());
        if (strArr[0] != null) {
            typedArrayObtainAttributes.recycle();
            return null;
        }
        boolean zHasValue = typedArrayObtainAttributes.hasValue(5);
        if (zHasValue) {
            service.info.exported = typedArrayObtainAttributes.getBoolean(5, false);
        }
        String nonConfigurationString = typedArrayObtainAttributes.getNonConfigurationString(3, 0);
        if (nonConfigurationString == null) {
            service.info.permission = r24.applicationInfo.permission;
        } else {
            service.info.permission = nonConfigurationString.length() > 0 ? nonConfigurationString.toString().intern() : null;
        }
        service.info.splitName = typedArrayObtainAttributes.getNonConfigurationString(17, 0);
        service.info.flags = 0;
        boolean z = true;
        if (typedArrayObtainAttributes.getBoolean(9, false)) {
            service.info.flags |= 1;
        }
        if (typedArrayObtainAttributes.getBoolean(10, false)) {
            service.info.flags |= 2;
        }
        if (typedArrayObtainAttributes.getBoolean(14, false)) {
            service.info.flags |= 4;
        }
        if (typedArrayObtainAttributes.getBoolean(11, false)) {
            service.info.flags |= 1073741824;
        }
        ServiceInfo serviceInfo = service.info;
        ServiceInfo serviceInfo2 = service.info;
        boolean z2 = typedArrayObtainAttributes.getBoolean(13, false);
        serviceInfo2.directBootAware = z2;
        serviceInfo.encryptionAware = z2;
        if (service.info.directBootAware) {
            r24.applicationInfo.privateFlags |= 256;
        }
        boolean z3 = typedArrayObtainAttributes.getBoolean(16, false);
        if (z3) {
            service.info.flags |= 1048576;
            r24.visibleToInstantApps = true;
        }
        typedArrayObtainAttributes.recycle();
        if ((r24.applicationInfo.privateFlags & 2) != 0 && service.info.processName == r24.packageName) {
            strArr[0] = "Heavy-weight applications can not have services in main process";
            return null;
        }
        int depth = xmlResourceParser.getDepth();
        while (true) {
            int next = xmlResourceParser.next();
            if (next == 1 || (next == 3 && xmlResourceParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != 4) {
                if (xmlResourceParser.getName().equals("intent-filter")) {
                    ServiceIntentInfo serviceIntentInfo2 = new ServiceIntentInfo(service);
                    i2 = depth;
                    resources2 = resources3;
                    if (!parseIntent(resources3, xmlResourceParser3, true, false, serviceIntentInfo2, strArr)) {
                        return null;
                    }
                    if (z3) {
                        serviceIntentInfo = serviceIntentInfo2;
                        serviceIntentInfo.setVisibilityToInstantApp(1);
                        service.info.flags |= 1048576;
                    } else {
                        serviceIntentInfo = serviceIntentInfo2;
                    }
                    service.order = Math.max(serviceIntentInfo.getOrder(), service.order);
                    service.intents.add(serviceIntentInfo);
                    xmlResourceParser3 = xmlResourceParser;
                } else {
                    i2 = depth;
                    resources2 = resources3;
                    if (xmlResourceParser.getName().equals("meta-data")) {
                        xmlResourceParser2 = xmlResourceParser;
                        strArr2 = strArr;
                        Bundle metaData = parseMetaData(resources2, xmlResourceParser2, service.metaData, strArr2);
                        service.metaData = metaData;
                        if (metaData == null) {
                            return null;
                        }
                    } else {
                        xmlResourceParser2 = xmlResourceParser;
                        strArr2 = strArr;
                        Slog.w(TAG, "Unknown element under <service>: " + xmlResourceParser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                        XmlUtils.skipCurrentTag(xmlResourceParser);
                    }
                    xmlResourceParser3 = xmlResourceParser2;
                }
                resources3 = resources2;
                depth = i2;
            }
        }
    }

    private boolean isImplicitlyExposedIntent(IntentInfo intentInfo) {
        return intentInfo.hasCategory(Intent.CATEGORY_BROWSABLE) || intentInfo.hasAction(Intent.ACTION_SEND) || intentInfo.hasAction(Intent.ACTION_SENDTO) || intentInfo.hasAction(Intent.ACTION_SEND_MULTIPLE);
    }

    private boolean parseAllMetaData(Resources resources, XmlResourceParser xmlResourceParser, String str, Component<?> component, String[] strArr) throws XmlPullParserException, IOException {
        int depth = xmlResourceParser.getDepth();
        while (true) {
            int next = xmlResourceParser.next();
            if (next == 1 || (next == 3 && xmlResourceParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != 4) {
                if (xmlResourceParser.getName().equals("meta-data")) {
                    Bundle metaData = parseMetaData(resources, xmlResourceParser, component.metaData, strArr);
                    component.metaData = metaData;
                    if (metaData == null) {
                        return false;
                    }
                } else {
                    Slog.w(TAG, "Unknown element under " + str + ": " + xmlResourceParser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                    XmlUtils.skipCurrentTag(xmlResourceParser);
                }
            }
        }
    }

    private Bundle parseMetaData(Resources resources, XmlResourceParser xmlResourceParser, Bundle bundle, String[] strArr) throws XmlPullParserException, IOException {
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestMetaData);
        if (bundle == null) {
            bundle = new Bundle();
        }
        boolean z = false;
        String nonConfigurationString = typedArrayObtainAttributes.getNonConfigurationString(0, 0);
        if (nonConfigurationString == null) {
            strArr[0] = "<meta-data> requires an android:name attribute";
            typedArrayObtainAttributes.recycle();
            return null;
        }
        String strIntern = nonConfigurationString.intern();
        TypedValue typedValuePeekValue = typedArrayObtainAttributes.peekValue(2);
        if (typedValuePeekValue != null && typedValuePeekValue.resourceId != 0) {
            bundle.putInt(strIntern, typedValuePeekValue.resourceId);
        } else {
            TypedValue typedValuePeekValue2 = typedArrayObtainAttributes.peekValue(1);
            if (typedValuePeekValue2 == null) {
                strArr[0] = "<meta-data> requires an android:value or android:resource attribute";
                bundle = null;
            } else if (typedValuePeekValue2.type == 3) {
                CharSequence charSequenceCoerceToString = typedValuePeekValue2.coerceToString();
                bundle.putString(strIntern, charSequenceCoerceToString != null ? charSequenceCoerceToString.toString() : null);
            } else if (typedValuePeekValue2.type == 18) {
                if (typedValuePeekValue2.data != 0) {
                    z = true;
                }
                bundle.putBoolean(strIntern, z);
            } else if (typedValuePeekValue2.type >= 16 && typedValuePeekValue2.type <= 31) {
                bundle.putInt(strIntern, typedValuePeekValue2.data);
            } else if (typedValuePeekValue2.type == 4) {
                bundle.putFloat(strIntern, typedValuePeekValue2.getFloat());
            } else {
                Slog.w(TAG, "<meta-data> only supports string, integer, float, color, boolean, and resource reference types: " + xmlResourceParser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
            }
        }
        typedArrayObtainAttributes.recycle();
        XmlUtils.skipCurrentTag(xmlResourceParser);
        return bundle;
    }

    private static VerifierInfo parseVerifier(AttributeSet attributeSet) {
        int attributeCount = attributeSet.getAttributeCount();
        String attributeValue = null;
        String attributeValue2 = null;
        for (int i = 0; i < attributeCount; i++) {
            int attributeNameResource = attributeSet.getAttributeNameResource(i);
            if (attributeNameResource == 16842755) {
                attributeValue = attributeSet.getAttributeValue(i);
            } else if (attributeNameResource == 16843686) {
                attributeValue2 = attributeSet.getAttributeValue(i);
            }
        }
        if (attributeValue == null || attributeValue.length() == 0) {
            Slog.i(TAG, "verifier package name was null; skipping");
            return null;
        }
        PublicKey publicKey = parsePublicKey(attributeValue2);
        if (publicKey == null) {
            Slog.i(TAG, "Unable to parse verifier public key for " + attributeValue);
            return null;
        }
        return new VerifierInfo(attributeValue, publicKey);
    }

    public static final PublicKey parsePublicKey(String str) {
        if (str == null) {
            Slog.w(TAG, "Could not parse null public key");
            return null;
        }
        try {
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(Base64.decode(str, 0));
            try {
                return KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_RSA).generatePublic(x509EncodedKeySpec);
            } catch (NoSuchAlgorithmException e) {
                Slog.wtf(TAG, "Could not parse public key: RSA KeyFactory not included in build");
                try {
                    return KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC).generatePublic(x509EncodedKeySpec);
                } catch (NoSuchAlgorithmException e2) {
                    Slog.wtf(TAG, "Could not parse public key: EC KeyFactory not included in build");
                    try {
                        return KeyFactory.getInstance("DSA").generatePublic(x509EncodedKeySpec);
                    } catch (NoSuchAlgorithmException e3) {
                        Slog.wtf(TAG, "Could not parse public key: DSA KeyFactory not included in build");
                        return null;
                    } catch (InvalidKeySpecException e4) {
                        return null;
                    }
                } catch (InvalidKeySpecException e5) {
                    return KeyFactory.getInstance("DSA").generatePublic(x509EncodedKeySpec);
                }
            } catch (InvalidKeySpecException e6) {
                return KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC).generatePublic(x509EncodedKeySpec);
            }
        } catch (IllegalArgumentException e7) {
            Slog.w(TAG, "Could not parse verifier public key; invalid Base64");
            return null;
        }
    }

    private boolean parseIntent(Resources resources, XmlResourceParser xmlResourceParser, boolean z, boolean z2, IntentInfo intentInfo, String[] strArr) throws XmlPullParserException, IOException {
        int resourceId;
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestIntentFilter);
        int i = 2;
        intentInfo.setPriority(typedArrayObtainAttributes.getInt(2, 0));
        intentInfo.setOrder(typedArrayObtainAttributes.getInt(3, 0));
        TypedValue typedValuePeekValue = typedArrayObtainAttributes.peekValue(0);
        if (typedValuePeekValue != null) {
            int i2 = typedValuePeekValue.resourceId;
            intentInfo.labelRes = i2;
            if (i2 == 0) {
                intentInfo.nonLocalizedLabel = typedValuePeekValue.coerceToString();
            }
        }
        int i3 = 7;
        if (Resources.getSystem().getBoolean(R.bool.config_useRoundIcon)) {
            resourceId = typedArrayObtainAttributes.getResourceId(7, 0);
        } else {
            resourceId = 0;
        }
        if (resourceId != 0) {
            intentInfo.icon = resourceId;
        } else {
            intentInfo.icon = typedArrayObtainAttributes.getResourceId(1, 0);
        }
        intentInfo.logo = typedArrayObtainAttributes.getResourceId(4, 0);
        intentInfo.banner = typedArrayObtainAttributes.getResourceId(5, 0);
        if (z2) {
            intentInfo.setAutoVerify(typedArrayObtainAttributes.getBoolean(6, false));
        }
        typedArrayObtainAttributes.recycle();
        int depth = xmlResourceParser.getDepth();
        while (true) {
            int next = xmlResourceParser.next();
            if (next == 1 || (next == 3 && xmlResourceParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != 4) {
                String name = xmlResourceParser.getName();
                if (name.equals("action")) {
                    String attributeValue = xmlResourceParser.getAttributeValue(ANDROID_RESOURCES, "name");
                    if (attributeValue == null || attributeValue == "") {
                        break;
                    }
                    XmlUtils.skipCurrentTag(xmlResourceParser);
                    intentInfo.addAction(attributeValue);
                } else if (name.equals("category")) {
                    String attributeValue2 = xmlResourceParser.getAttributeValue(ANDROID_RESOURCES, "name");
                    if (attributeValue2 == null || attributeValue2 == "") {
                        break;
                    }
                    XmlUtils.skipCurrentTag(xmlResourceParser);
                    intentInfo.addCategory(attributeValue2);
                } else if (name.equals("data")) {
                    TypedArray typedArrayObtainAttributes2 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestData);
                    String nonConfigurationString = typedArrayObtainAttributes2.getNonConfigurationString(0, 0);
                    if (nonConfigurationString != null) {
                        try {
                            intentInfo.addDataType(nonConfigurationString);
                        } catch (IntentFilter.MalformedMimeTypeException e) {
                            strArr[0] = e.toString();
                            typedArrayObtainAttributes2.recycle();
                            return false;
                        }
                    }
                    String nonConfigurationString2 = typedArrayObtainAttributes2.getNonConfigurationString(1, 0);
                    if (nonConfigurationString2 != null) {
                        intentInfo.addDataScheme(nonConfigurationString2);
                    }
                    String nonConfigurationString3 = typedArrayObtainAttributes2.getNonConfigurationString(i3, 0);
                    if (nonConfigurationString3 != null) {
                        intentInfo.addDataSchemeSpecificPart(nonConfigurationString3, 0);
                    }
                    String nonConfigurationString4 = typedArrayObtainAttributes2.getNonConfigurationString(8, 0);
                    if (nonConfigurationString4 != null) {
                        intentInfo.addDataSchemeSpecificPart(nonConfigurationString4, 1);
                    }
                    String nonConfigurationString5 = typedArrayObtainAttributes2.getNonConfigurationString(9, 0);
                    if (nonConfigurationString5 != null) {
                        if (!z) {
                            strArr[0] = "sspPattern not allowed here; ssp must be literal";
                            return false;
                        }
                        intentInfo.addDataSchemeSpecificPart(nonConfigurationString5, i);
                    }
                    String nonConfigurationString6 = typedArrayObtainAttributes2.getNonConfigurationString(i, 0);
                    String nonConfigurationString7 = typedArrayObtainAttributes2.getNonConfigurationString(3, 0);
                    if (nonConfigurationString6 != null) {
                        intentInfo.addDataAuthority(nonConfigurationString6, nonConfigurationString7);
                    }
                    String nonConfigurationString8 = typedArrayObtainAttributes2.getNonConfigurationString(4, 0);
                    if (nonConfigurationString8 != null) {
                        intentInfo.addDataPath(nonConfigurationString8, 0);
                    }
                    String nonConfigurationString9 = typedArrayObtainAttributes2.getNonConfigurationString(5, 0);
                    if (nonConfigurationString9 != null) {
                        intentInfo.addDataPath(nonConfigurationString9, 1);
                    }
                    String nonConfigurationString10 = typedArrayObtainAttributes2.getNonConfigurationString(6, 0);
                    if (nonConfigurationString10 != null) {
                        if (!z) {
                            strArr[0] = "pathPattern not allowed here; path must be literal";
                            return false;
                        }
                        intentInfo.addDataPath(nonConfigurationString10, i);
                    }
                    String nonConfigurationString11 = typedArrayObtainAttributes2.getNonConfigurationString(10, 0);
                    if (nonConfigurationString11 != null) {
                        if (!z) {
                            strArr[0] = "pathAdvancedPattern not allowed here; path must be literal";
                            return false;
                        }
                        intentInfo.addDataPath(nonConfigurationString11, 3);
                    }
                    typedArrayObtainAttributes2.recycle();
                    XmlUtils.skipCurrentTag(xmlResourceParser);
                } else {
                    Slog.w(TAG, "Unknown element under <intent-filter>: " + xmlResourceParser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                    XmlUtils.skipCurrentTag(xmlResourceParser);
                    i = 2;
                    i3 = 7;
                }
                i = 2;
                i3 = 7;
            }
        }
    }

    public static final class SigningDetails implements Parcelable {
        private static final int PAST_CERT_EXISTS = 0;
        public final Signature[] pastSigningCertificates;
        public final int[] pastSigningCertificatesFlags;
        public final ArraySet<PublicKey> publicKeys;

        @SignatureSchemeVersion
        public final int signatureSchemeVersion;
        public final Signature[] signatures;
        public static final SigningDetails UNKNOWN = new SigningDetails(null, 0, null, null, null);
        public static final Parcelable.Creator<SigningDetails> CREATOR = new Parcelable.Creator<SigningDetails>() {
            @Override
            public SigningDetails createFromParcel(Parcel parcel) {
                if (parcel.readBoolean()) {
                    return SigningDetails.UNKNOWN;
                }
                return new SigningDetails(parcel);
            }

            @Override
            public SigningDetails[] newArray(int i) {
                return new SigningDetails[i];
            }
        };

        public @interface CertCapabilities {
            public static final int AUTH = 16;
            public static final int INSTALLED_DATA = 1;
            public static final int PERMISSION = 4;
            public static final int ROLLBACK = 8;
            public static final int SHARED_USER_ID = 2;
        }

        public @interface SignatureSchemeVersion {
            public static final int JAR = 1;
            public static final int SIGNING_BLOCK_V2 = 2;
            public static final int SIGNING_BLOCK_V3 = 3;
            public static final int UNKNOWN = 0;
        }

        @VisibleForTesting
        public SigningDetails(Signature[] signatureArr, @SignatureSchemeVersion int i, ArraySet<PublicKey> arraySet, Signature[] signatureArr2, int[] iArr) {
            this.signatures = signatureArr;
            this.signatureSchemeVersion = i;
            this.publicKeys = arraySet;
            this.pastSigningCertificates = signatureArr2;
            this.pastSigningCertificatesFlags = iArr;
        }

        public SigningDetails(Signature[] signatureArr, @SignatureSchemeVersion int i, Signature[] signatureArr2, int[] iArr) throws CertificateException {
            this(signatureArr, i, PackageParser.toSigningKeys(signatureArr), signatureArr2, iArr);
        }

        public SigningDetails(Signature[] signatureArr, @SignatureSchemeVersion int i) throws CertificateException {
            this(signatureArr, i, null, null);
        }

        public SigningDetails(SigningDetails signingDetails) {
            if (signingDetails != null) {
                if (signingDetails.signatures != null) {
                    this.signatures = (Signature[]) signingDetails.signatures.clone();
                } else {
                    this.signatures = null;
                }
                this.signatureSchemeVersion = signingDetails.signatureSchemeVersion;
                this.publicKeys = new ArraySet<>((ArraySet) signingDetails.publicKeys);
                if (signingDetails.pastSigningCertificates != null) {
                    this.pastSigningCertificates = (Signature[]) signingDetails.pastSigningCertificates.clone();
                    this.pastSigningCertificatesFlags = (int[]) signingDetails.pastSigningCertificatesFlags.clone();
                    return;
                } else {
                    this.pastSigningCertificates = null;
                    this.pastSigningCertificatesFlags = null;
                    return;
                }
            }
            this.signatures = null;
            this.signatureSchemeVersion = 0;
            this.publicKeys = null;
            this.pastSigningCertificates = null;
            this.pastSigningCertificatesFlags = null;
        }

        public boolean hasSignatures() {
            return this.signatures != null && this.signatures.length > 0;
        }

        public boolean hasPastSigningCertificates() {
            return this.pastSigningCertificates != null && this.pastSigningCertificates.length > 0;
        }

        public boolean hasAncestorOrSelf(SigningDetails signingDetails) {
            if (this == UNKNOWN || signingDetails == UNKNOWN) {
                return false;
            }
            if (signingDetails.signatures.length > 1) {
                return signaturesMatchExactly(signingDetails);
            }
            return hasCertificate(signingDetails.signatures[0]);
        }

        public boolean hasAncestor(SigningDetails signingDetails) {
            if (this != UNKNOWN && signingDetails != UNKNOWN && hasPastSigningCertificates() && signingDetails.signatures.length == 1) {
                for (int i = 0; i < this.pastSigningCertificates.length - 1; i++) {
                    if (this.pastSigningCertificates[i].equals(signingDetails.signatures[i])) {
                        return true;
                    }
                }
            }
            return false;
        }

        public boolean checkCapability(SigningDetails signingDetails, @CertCapabilities int i) {
            if (this == UNKNOWN || signingDetails == UNKNOWN) {
                return false;
            }
            if (signingDetails.signatures.length > 1) {
                return signaturesMatchExactly(signingDetails);
            }
            return hasCertificate(signingDetails.signatures[0], i);
        }

        public boolean checkCapabilityRecover(SigningDetails signingDetails, @CertCapabilities int i) throws CertificateException {
            if (signingDetails == UNKNOWN || this == UNKNOWN) {
                return false;
            }
            if (hasPastSigningCertificates() && signingDetails.signatures.length == 1) {
                for (int i2 = 0; i2 < this.pastSigningCertificates.length; i2++) {
                    if (Signature.areEffectiveMatch(signingDetails.signatures[0], this.pastSigningCertificates[i2]) && this.pastSigningCertificatesFlags[i2] == i) {
                        return true;
                    }
                }
                return false;
            }
            return Signature.areEffectiveMatch(signingDetails.signatures, this.signatures);
        }

        public boolean hasCertificate(Signature signature) {
            return hasCertificateInternal(signature, 0);
        }

        public boolean hasCertificate(Signature signature, @CertCapabilities int i) {
            return hasCertificateInternal(signature, i);
        }

        public boolean hasCertificate(byte[] bArr) {
            return hasCertificate(new Signature(bArr));
        }

        private boolean hasCertificateInternal(Signature signature, int i) {
            if (this == UNKNOWN) {
                return false;
            }
            if (hasPastSigningCertificates()) {
                for (int i2 = 0; i2 < this.pastSigningCertificates.length - 1; i2++) {
                    if (this.pastSigningCertificates[i2].equals(signature) && (i == 0 || (this.pastSigningCertificatesFlags[i2] & i) == i)) {
                        return true;
                    }
                }
            }
            return this.signatures.length == 1 && this.signatures[0].equals(signature);
        }

        public boolean checkCapability(String str, @CertCapabilities int i) {
            if (this == UNKNOWN) {
                return false;
            }
            if (hasSha256Certificate(ByteStringUtils.fromHexToByteArray(str), i)) {
                return true;
            }
            return PackageUtils.computeSignaturesSha256Digest(PackageUtils.computeSignaturesSha256Digests(this.signatures)).equals(str);
        }

        public boolean hasSha256Certificate(byte[] bArr) {
            return hasSha256CertificateInternal(bArr, 0);
        }

        public boolean hasSha256Certificate(byte[] bArr, @CertCapabilities int i) {
            return hasSha256CertificateInternal(bArr, i);
        }

        private boolean hasSha256CertificateInternal(byte[] bArr, int i) {
            if (this == UNKNOWN) {
                return false;
            }
            if (hasPastSigningCertificates()) {
                for (int i2 = 0; i2 < this.pastSigningCertificates.length - 1; i2++) {
                    if (Arrays.equals(bArr, PackageUtils.computeSha256DigestBytes(this.pastSigningCertificates[i2].toByteArray())) && (i == 0 || (this.pastSigningCertificatesFlags[i2] & i) == i)) {
                        return true;
                    }
                }
            }
            if (this.signatures.length == 1) {
                return Arrays.equals(bArr, PackageUtils.computeSha256DigestBytes(this.signatures[0].toByteArray()));
            }
            return false;
        }

        public boolean signaturesMatchExactly(SigningDetails signingDetails) {
            return Signature.areExactMatch(this.signatures, signingDetails.signatures);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            boolean z = UNKNOWN == this;
            parcel.writeBoolean(z);
            if (z) {
                return;
            }
            parcel.writeTypedArray(this.signatures, i);
            parcel.writeInt(this.signatureSchemeVersion);
            parcel.writeArraySet(this.publicKeys);
            parcel.writeTypedArray(this.pastSigningCertificates, i);
            parcel.writeIntArray(this.pastSigningCertificatesFlags);
        }

        protected SigningDetails(Parcel parcel) {
            ClassLoader classLoader = Object.class.getClassLoader();
            this.signatures = (Signature[]) parcel.createTypedArray(Signature.CREATOR);
            this.signatureSchemeVersion = parcel.readInt();
            this.publicKeys = parcel.readArraySet(classLoader);
            this.pastSigningCertificates = (Signature[]) parcel.createTypedArray(Signature.CREATOR);
            this.pastSigningCertificatesFlags = parcel.createIntArray();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SigningDetails)) {
                return false;
            }
            SigningDetails signingDetails = (SigningDetails) obj;
            if (this.signatureSchemeVersion != signingDetails.signatureSchemeVersion || !Signature.areExactMatch(this.signatures, signingDetails.signatures)) {
                return false;
            }
            if (this.publicKeys != null) {
                if (!this.publicKeys.equals(signingDetails.publicKeys)) {
                    return false;
                }
            } else if (signingDetails.publicKeys != null) {
                return false;
            }
            return Arrays.equals(this.pastSigningCertificates, signingDetails.pastSigningCertificates) && Arrays.equals(this.pastSigningCertificatesFlags, signingDetails.pastSigningCertificatesFlags);
        }

        public int hashCode() {
            return (31 * ((((((Arrays.hashCode(this.signatures) * 31) + this.signatureSchemeVersion) * 31) + (this.publicKeys != null ? this.publicKeys.hashCode() : 0)) * 31) + Arrays.hashCode(this.pastSigningCertificates))) + Arrays.hashCode(this.pastSigningCertificatesFlags);
        }

        public static class Builder {
            private Signature[] mPastSigningCertificates;
            private int[] mPastSigningCertificatesFlags;
            private int mSignatureSchemeVersion = 0;
            private Signature[] mSignatures;

            public Builder setSignatures(Signature[] signatureArr) {
                this.mSignatures = signatureArr;
                return this;
            }

            public Builder setSignatureSchemeVersion(int i) {
                this.mSignatureSchemeVersion = i;
                return this;
            }

            public Builder setPastSigningCertificates(Signature[] signatureArr) {
                this.mPastSigningCertificates = signatureArr;
                return this;
            }

            public Builder setPastSigningCertificatesFlags(int[] iArr) {
                this.mPastSigningCertificatesFlags = iArr;
                return this;
            }

            private void checkInvariants() {
                if (this.mSignatures == null) {
                    throw new IllegalStateException("SigningDetails requires the current signing certificates.");
                }
                boolean z = true;
                if (this.mPastSigningCertificates == null || this.mPastSigningCertificatesFlags == null ? !(this.mPastSigningCertificates != null || this.mPastSigningCertificatesFlags != null) : this.mPastSigningCertificates.length == this.mPastSigningCertificatesFlags.length) {
                    z = false;
                }
                if (z) {
                    throw new IllegalStateException("SigningDetails must have a one to one mapping between pastSigningCertificates and pastSigningCertificatesFlags");
                }
            }

            public SigningDetails build() throws CertificateException {
                checkInvariants();
                return new SigningDetails(this.mSignatures, this.mSignatureSchemeVersion, this.mPastSigningCertificates, this.mPastSigningCertificatesFlags);
            }
        }
    }

    public static final class Package implements Parcelable {
        public static final Parcelable.Creator CREATOR = new Parcelable.Creator<Package>() {
            @Override
            public Package createFromParcel(Parcel parcel) {
                return new Package(parcel);
            }

            @Override
            public Package[] newArray(int i) {
                return new Package[i];
            }
        };
        public final ArrayList<Activity> activities;
        public ApplicationInfo applicationInfo;
        public String baseCodePath;
        public boolean baseHardwareAccelerated;
        public int baseRevisionCode;
        public ArrayList<Package> childPackages;
        public String codePath;
        public ArrayList<ConfigurationInfo> configPreferences;
        public boolean coreApp;
        public String cpuAbiOverride;
        public ArrayList<FeatureGroupInfo> featureGroups;
        public int installLocation;
        public final ArrayList<Instrumentation> instrumentation;
        public boolean isStub;
        public ArrayList<String> libraryNames;
        public ArrayList<String> mAdoptPermissions;
        public Bundle mAppMetaData;
        public int mCompileSdkVersion;
        public String mCompileSdkVersionCodename;
        public Object mExtras;
        public ArrayMap<String, ArraySet<PublicKey>> mKeySetMapping;
        public long[] mLastPackageUsageTimeInMills;
        public ArrayList<String> mOriginalPackages;
        public String mOverlayCategory;
        public boolean mOverlayIsStatic;
        public int mOverlayPriority;
        public String mOverlayTarget;
        public int mPreferredOrder;
        public String mRealPackage;
        public String mRequiredAccountType;
        public boolean mRequiredForAllUsers;
        public String mRestrictedAccountType;
        public String mSharedUserId;
        public int mSharedUserLabel;
        public SigningDetails mSigningDetails;
        public ArraySet<String> mUpgradeKeySets;
        public int mVersionCode;
        public int mVersionCodeMajor;
        public String mVersionName;
        public String manifestPackageName;
        public String packageName;
        public Package parentPackage;
        public final ArrayList<PermissionGroup> permissionGroups;
        public final ArrayList<Permission> permissions;
        public ArrayList<ActivityIntentInfo> preferredActivityFilters;
        public ArrayList<String> protectedBroadcasts;
        public final ArrayList<Provider> providers;
        public final ArrayList<Activity> receivers;
        public ArrayList<FeatureInfo> reqFeatures;
        public final ArrayList<String> requestedPermissions;
        public byte[] restrictUpdateHash;
        public final ArrayList<Service> services;
        public String[] splitCodePaths;
        public int[] splitFlags;
        public String[] splitNames;
        public int[] splitPrivateFlags;
        public int[] splitRevisionCodes;
        public String staticSharedLibName;
        public long staticSharedLibVersion;
        public boolean use32bitAbi;
        public ArrayList<String> usesLibraries;
        public String[] usesLibraryFiles;
        public ArrayList<String> usesOptionalLibraries;
        public ArrayList<String> usesStaticLibraries;
        public String[][] usesStaticLibrariesCertDigests;
        public long[] usesStaticLibrariesVersions;
        public boolean visibleToInstantApps;
        public String volumeUuid;

        public long getLongVersionCode() {
            return PackageInfo.composeLongVersionCode(this.mVersionCodeMajor, this.mVersionCode);
        }

        public Package(String str) {
            this.applicationInfo = new ApplicationInfo();
            this.permissions = new ArrayList<>(0);
            this.permissionGroups = new ArrayList<>(0);
            this.activities = new ArrayList<>(0);
            this.receivers = new ArrayList<>(0);
            this.providers = new ArrayList<>(0);
            this.services = new ArrayList<>(0);
            this.instrumentation = new ArrayList<>(0);
            this.requestedPermissions = new ArrayList<>();
            this.staticSharedLibName = null;
            this.staticSharedLibVersion = 0L;
            this.libraryNames = null;
            this.usesLibraries = null;
            this.usesStaticLibraries = null;
            this.usesStaticLibrariesVersions = null;
            this.usesStaticLibrariesCertDigests = null;
            this.usesOptionalLibraries = null;
            this.usesLibraryFiles = null;
            this.preferredActivityFilters = null;
            this.mOriginalPackages = null;
            this.mRealPackage = null;
            this.mAdoptPermissions = null;
            this.mAppMetaData = null;
            this.mSigningDetails = SigningDetails.UNKNOWN;
            this.mPreferredOrder = 0;
            this.mLastPackageUsageTimeInMills = new long[8];
            this.configPreferences = null;
            this.reqFeatures = null;
            this.featureGroups = null;
            this.packageName = str;
            this.manifestPackageName = str;
            this.applicationInfo.packageName = str;
            this.applicationInfo.uid = -1;
        }

        public void setApplicationVolumeUuid(String str) {
            UUID uuidConvert = StorageManager.convert(str);
            this.applicationInfo.volumeUuid = str;
            this.applicationInfo.storageUuid = uuidConvert;
            if (this.childPackages != null) {
                int size = this.childPackages.size();
                for (int i = 0; i < size; i++) {
                    this.childPackages.get(i).applicationInfo.volumeUuid = str;
                    this.childPackages.get(i).applicationInfo.storageUuid = uuidConvert;
                }
            }
        }

        public void setApplicationInfoCodePath(String str) {
            this.applicationInfo.setCodePath(str);
            if (this.childPackages != null) {
                int size = this.childPackages.size();
                for (int i = 0; i < size; i++) {
                    this.childPackages.get(i).applicationInfo.setCodePath(str);
                }
            }
        }

        @Deprecated
        public void setApplicationInfoResourcePath(String str) {
            this.applicationInfo.setResourcePath(str);
            if (this.childPackages != null) {
                int size = this.childPackages.size();
                for (int i = 0; i < size; i++) {
                    this.childPackages.get(i).applicationInfo.setResourcePath(str);
                }
            }
        }

        @Deprecated
        public void setApplicationInfoBaseResourcePath(String str) {
            this.applicationInfo.setBaseResourcePath(str);
            if (this.childPackages != null) {
                int size = this.childPackages.size();
                for (int i = 0; i < size; i++) {
                    this.childPackages.get(i).applicationInfo.setBaseResourcePath(str);
                }
            }
        }

        public void setApplicationInfoBaseCodePath(String str) {
            this.applicationInfo.setBaseCodePath(str);
            if (this.childPackages != null) {
                int size = this.childPackages.size();
                for (int i = 0; i < size; i++) {
                    this.childPackages.get(i).applicationInfo.setBaseCodePath(str);
                }
            }
        }

        public List<String> getChildPackageNames() {
            if (this.childPackages == null) {
                return null;
            }
            int size = this.childPackages.size();
            ArrayList arrayList = new ArrayList(size);
            for (int i = 0; i < size; i++) {
                arrayList.add(this.childPackages.get(i).packageName);
            }
            return arrayList;
        }

        public boolean hasChildPackage(String str) {
            int size = this.childPackages != null ? this.childPackages.size() : 0;
            for (int i = 0; i < size; i++) {
                if (this.childPackages.get(i).packageName.equals(str)) {
                    return true;
                }
            }
            return false;
        }

        public void setApplicationInfoSplitCodePaths(String[] strArr) {
            this.applicationInfo.setSplitCodePaths(strArr);
        }

        @Deprecated
        public void setApplicationInfoSplitResourcePaths(String[] strArr) {
            this.applicationInfo.setSplitResourcePaths(strArr);
        }

        public void setSplitCodePaths(String[] strArr) {
            this.splitCodePaths = strArr;
        }

        public void setCodePath(String str) {
            this.codePath = str;
            if (this.childPackages != null) {
                int size = this.childPackages.size();
                for (int i = 0; i < size; i++) {
                    this.childPackages.get(i).codePath = str;
                }
            }
        }

        public void setBaseCodePath(String str) {
            this.baseCodePath = str;
            if (this.childPackages != null) {
                int size = this.childPackages.size();
                for (int i = 0; i < size; i++) {
                    this.childPackages.get(i).baseCodePath = str;
                }
            }
        }

        public void setSigningDetails(SigningDetails signingDetails) {
            this.mSigningDetails = signingDetails;
            if (this.childPackages != null) {
                int size = this.childPackages.size();
                for (int i = 0; i < size; i++) {
                    this.childPackages.get(i).mSigningDetails = signingDetails;
                }
            }
        }

        public void setVolumeUuid(String str) {
            this.volumeUuid = str;
            if (this.childPackages != null) {
                int size = this.childPackages.size();
                for (int i = 0; i < size; i++) {
                    this.childPackages.get(i).volumeUuid = str;
                }
            }
        }

        public void setApplicationInfoFlags(int i, int i2) {
            ApplicationInfo applicationInfo = this.applicationInfo;
            int i3 = this.applicationInfo.flags;
            int i4 = ~i;
            int i5 = i & i2;
            applicationInfo.flags = (i3 & i4) | i5;
            if (this.childPackages != null) {
                int size = this.childPackages.size();
                for (int i6 = 0; i6 < size; i6++) {
                    this.childPackages.get(i6).applicationInfo.flags = (this.applicationInfo.flags & i4) | i5;
                }
            }
        }

        public void setUse32bitAbi(boolean z) {
            this.use32bitAbi = z;
            if (this.childPackages != null) {
                int size = this.childPackages.size();
                for (int i = 0; i < size; i++) {
                    this.childPackages.get(i).use32bitAbi = z;
                }
            }
        }

        public boolean isLibrary() {
            return (this.staticSharedLibName == null && ArrayUtils.isEmpty(this.libraryNames)) ? false : true;
        }

        public List<String> getAllCodePaths() {
            ArrayList arrayList = new ArrayList();
            arrayList.add(this.baseCodePath);
            if (!ArrayUtils.isEmpty(this.splitCodePaths)) {
                Collections.addAll(arrayList, this.splitCodePaths);
            }
            return arrayList;
        }

        public List<String> getAllCodePathsExcludingResourceOnly() {
            ArrayList arrayList = new ArrayList();
            if ((this.applicationInfo.flags & 4) != 0) {
                arrayList.add(this.baseCodePath);
            }
            if (!ArrayUtils.isEmpty(this.splitCodePaths)) {
                for (int i = 0; i < this.splitCodePaths.length; i++) {
                    if ((this.splitFlags[i] & 4) != 0) {
                        arrayList.add(this.splitCodePaths[i]);
                    }
                }
            }
            return arrayList;
        }

        public void setPackageName(String str) {
            this.packageName = str;
            this.applicationInfo.packageName = str;
            for (int size = this.permissions.size() - 1; size >= 0; size--) {
                this.permissions.get(size).setPackageName(str);
            }
            for (int size2 = this.permissionGroups.size() - 1; size2 >= 0; size2--) {
                this.permissionGroups.get(size2).setPackageName(str);
            }
            for (int size3 = this.activities.size() - 1; size3 >= 0; size3--) {
                this.activities.get(size3).setPackageName(str);
            }
            for (int size4 = this.receivers.size() - 1; size4 >= 0; size4--) {
                this.receivers.get(size4).setPackageName(str);
            }
            for (int size5 = this.providers.size() - 1; size5 >= 0; size5--) {
                this.providers.get(size5).setPackageName(str);
            }
            for (int size6 = this.services.size() - 1; size6 >= 0; size6--) {
                this.services.get(size6).setPackageName(str);
            }
            for (int size7 = this.instrumentation.size() - 1; size7 >= 0; size7--) {
                this.instrumentation.get(size7).setPackageName(str);
            }
        }

        public boolean hasComponentClassName(String str) {
            for (int size = this.activities.size() - 1; size >= 0; size--) {
                if (str.equals(this.activities.get(size).className)) {
                    return true;
                }
            }
            for (int size2 = this.receivers.size() - 1; size2 >= 0; size2--) {
                if (str.equals(this.receivers.get(size2).className)) {
                    return true;
                }
            }
            for (int size3 = this.providers.size() - 1; size3 >= 0; size3--) {
                if (str.equals(this.providers.get(size3).className)) {
                    return true;
                }
            }
            for (int size4 = this.services.size() - 1; size4 >= 0; size4--) {
                if (str.equals(this.services.get(size4).className)) {
                    return true;
                }
            }
            for (int size5 = this.instrumentation.size() - 1; size5 >= 0; size5--) {
                if (str.equals(this.instrumentation.get(size5).className)) {
                    return true;
                }
            }
            return false;
        }

        public boolean isExternal() {
            return this.applicationInfo.isExternal();
        }

        public boolean isForwardLocked() {
            return this.applicationInfo.isForwardLocked();
        }

        public boolean isOem() {
            return this.applicationInfo.isOem();
        }

        public boolean isVendor() {
            return this.applicationInfo.isVendor();
        }

        public boolean isProduct() {
            return this.applicationInfo.isProduct();
        }

        public boolean isPrivileged() {
            return this.applicationInfo.isPrivilegedApp();
        }

        public boolean isSystem() {
            return this.applicationInfo.isSystemApp();
        }

        public boolean isUpdatedSystemApp() {
            return this.applicationInfo.isUpdatedSystemApp();
        }

        public boolean canHaveOatDir() {
            return ((isSystem() && !isUpdatedSystemApp()) || isForwardLocked() || this.applicationInfo.isExternalAsec()) ? false : true;
        }

        public boolean isMatch(int i) {
            if ((i & 1048576) != 0) {
                return isSystem();
            }
            return true;
        }

        public long getLatestPackageUseTimeInMills() {
            long jMax = 0;
            for (long j : this.mLastPackageUsageTimeInMills) {
                jMax = Math.max(jMax, j);
            }
            return jMax;
        }

        public long getLatestForegroundPackageUseTimeInMills() {
            long jMax = 0;
            for (int i : new int[]{0, 2}) {
                jMax = Math.max(jMax, this.mLastPackageUsageTimeInMills[i]);
            }
            return jMax;
        }

        public String toString() {
            return "Package{" + Integer.toHexString(System.identityHashCode(this)) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.packageName + "}";
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public Package(Parcel parcel) {
            this.applicationInfo = new ApplicationInfo();
            this.permissions = new ArrayList<>(0);
            this.permissionGroups = new ArrayList<>(0);
            this.activities = new ArrayList<>(0);
            this.receivers = new ArrayList<>(0);
            this.providers = new ArrayList<>(0);
            this.services = new ArrayList<>(0);
            this.instrumentation = new ArrayList<>(0);
            this.requestedPermissions = new ArrayList<>();
            this.staticSharedLibName = null;
            this.staticSharedLibVersion = 0L;
            this.libraryNames = null;
            this.usesLibraries = null;
            this.usesStaticLibraries = null;
            this.usesStaticLibrariesVersions = null;
            this.usesStaticLibrariesCertDigests = null;
            this.usesOptionalLibraries = null;
            this.usesLibraryFiles = null;
            this.preferredActivityFilters = null;
            this.mOriginalPackages = null;
            this.mRealPackage = null;
            this.mAdoptPermissions = null;
            this.mAppMetaData = null;
            this.mSigningDetails = SigningDetails.UNKNOWN;
            this.mPreferredOrder = 0;
            this.mLastPackageUsageTimeInMills = new long[8];
            this.configPreferences = null;
            this.reqFeatures = null;
            this.featureGroups = null;
            ClassLoader classLoader = Object.class.getClassLoader();
            this.packageName = parcel.readString().intern();
            this.manifestPackageName = parcel.readString();
            this.splitNames = parcel.readStringArray();
            this.volumeUuid = parcel.readString();
            this.codePath = parcel.readString();
            this.baseCodePath = parcel.readString();
            this.splitCodePaths = parcel.readStringArray();
            this.baseRevisionCode = parcel.readInt();
            this.splitRevisionCodes = parcel.createIntArray();
            this.splitFlags = parcel.createIntArray();
            this.splitPrivateFlags = parcel.createIntArray();
            this.baseHardwareAccelerated = parcel.readInt() == 1;
            this.applicationInfo = (ApplicationInfo) parcel.readParcelable(classLoader);
            if (this.applicationInfo.permission != null) {
                this.applicationInfo.permission = this.applicationInfo.permission.intern();
            }
            parcel.readParcelableList(this.permissions, classLoader);
            fixupOwner(this.permissions);
            parcel.readParcelableList(this.permissionGroups, classLoader);
            fixupOwner(this.permissionGroups);
            parcel.readParcelableList(this.activities, classLoader);
            fixupOwner(this.activities);
            parcel.readParcelableList(this.receivers, classLoader);
            fixupOwner(this.receivers);
            parcel.readParcelableList(this.providers, classLoader);
            fixupOwner(this.providers);
            parcel.readParcelableList(this.services, classLoader);
            fixupOwner(this.services);
            parcel.readParcelableList(this.instrumentation, classLoader);
            fixupOwner(this.instrumentation);
            parcel.readStringList(this.requestedPermissions);
            internStringArrayList(this.requestedPermissions);
            this.protectedBroadcasts = parcel.createStringArrayList();
            internStringArrayList(this.protectedBroadcasts);
            this.parentPackage = (Package) parcel.readParcelable(classLoader);
            this.childPackages = new ArrayList<>();
            parcel.readParcelableList(this.childPackages, classLoader);
            if (this.childPackages.size() == 0) {
                this.childPackages = null;
            }
            this.staticSharedLibName = parcel.readString();
            if (this.staticSharedLibName != null) {
                this.staticSharedLibName = this.staticSharedLibName.intern();
            }
            this.staticSharedLibVersion = parcel.readLong();
            this.libraryNames = parcel.createStringArrayList();
            internStringArrayList(this.libraryNames);
            this.usesLibraries = parcel.createStringArrayList();
            internStringArrayList(this.usesLibraries);
            this.usesOptionalLibraries = parcel.createStringArrayList();
            internStringArrayList(this.usesOptionalLibraries);
            this.usesLibraryFiles = parcel.readStringArray();
            int i = parcel.readInt();
            if (i > 0) {
                this.usesStaticLibraries = new ArrayList<>(i);
                parcel.readStringList(this.usesStaticLibraries);
                internStringArrayList(this.usesStaticLibraries);
                this.usesStaticLibrariesVersions = new long[i];
                parcel.readLongArray(this.usesStaticLibrariesVersions);
                this.usesStaticLibrariesCertDigests = new String[i][];
                for (int i2 = 0; i2 < i; i2++) {
                    this.usesStaticLibrariesCertDigests[i2] = parcel.createStringArray();
                }
            }
            this.preferredActivityFilters = new ArrayList<>();
            parcel.readParcelableList(this.preferredActivityFilters, classLoader);
            if (this.preferredActivityFilters.size() == 0) {
                this.preferredActivityFilters = null;
            }
            this.mOriginalPackages = parcel.createStringArrayList();
            this.mRealPackage = parcel.readString();
            this.mAdoptPermissions = parcel.createStringArrayList();
            this.mAppMetaData = parcel.readBundle();
            this.mVersionCode = parcel.readInt();
            this.mVersionCodeMajor = parcel.readInt();
            this.mVersionName = parcel.readString();
            if (this.mVersionName != null) {
                this.mVersionName = this.mVersionName.intern();
            }
            this.mSharedUserId = parcel.readString();
            if (this.mSharedUserId != null) {
                this.mSharedUserId = this.mSharedUserId.intern();
            }
            this.mSharedUserLabel = parcel.readInt();
            this.mSigningDetails = (SigningDetails) parcel.readParcelable(classLoader);
            this.mPreferredOrder = parcel.readInt();
            this.configPreferences = new ArrayList<>();
            parcel.readParcelableList(this.configPreferences, classLoader);
            if (this.configPreferences.size() == 0) {
                this.configPreferences = null;
            }
            this.reqFeatures = new ArrayList<>();
            parcel.readParcelableList(this.reqFeatures, classLoader);
            if (this.reqFeatures.size() == 0) {
                this.reqFeatures = null;
            }
            this.featureGroups = new ArrayList<>();
            parcel.readParcelableList(this.featureGroups, classLoader);
            if (this.featureGroups.size() == 0) {
                this.featureGroups = null;
            }
            this.installLocation = parcel.readInt();
            this.coreApp = parcel.readInt() == 1;
            this.mRequiredForAllUsers = parcel.readInt() == 1;
            this.mRestrictedAccountType = parcel.readString();
            this.mRequiredAccountType = parcel.readString();
            this.mOverlayTarget = parcel.readString();
            this.mOverlayCategory = parcel.readString();
            this.mOverlayPriority = parcel.readInt();
            this.mOverlayIsStatic = parcel.readInt() == 1;
            this.mCompileSdkVersion = parcel.readInt();
            this.mCompileSdkVersionCodename = parcel.readString();
            this.mUpgradeKeySets = parcel.readArraySet(classLoader);
            this.mKeySetMapping = readKeySetMapping(parcel);
            this.cpuAbiOverride = parcel.readString();
            this.use32bitAbi = parcel.readInt() == 1;
            this.restrictUpdateHash = parcel.createByteArray();
            this.visibleToInstantApps = parcel.readInt() == 1;
        }

        private static void internStringArrayList(List<String> list) {
            if (list != null) {
                int size = list.size();
                for (int i = 0; i < size; i++) {
                    list.set(i, list.get(i).intern());
                }
            }
        }

        private void fixupOwner(List<? extends Component<?>> list) {
            if (list != null) {
                for (Component<?> component : list) {
                    component.owner = this;
                    if (component instanceof Activity) {
                        ((Activity) component).info.applicationInfo = this.applicationInfo;
                    } else if (component instanceof Service) {
                        ((Service) component).info.applicationInfo = this.applicationInfo;
                    } else if (component instanceof Provider) {
                        ((Provider) component).info.applicationInfo = this.applicationInfo;
                    }
                }
            }
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(this.packageName);
            parcel.writeString(this.manifestPackageName);
            parcel.writeStringArray(this.splitNames);
            parcel.writeString(this.volumeUuid);
            parcel.writeString(this.codePath);
            parcel.writeString(this.baseCodePath);
            parcel.writeStringArray(this.splitCodePaths);
            parcel.writeInt(this.baseRevisionCode);
            parcel.writeIntArray(this.splitRevisionCodes);
            parcel.writeIntArray(this.splitFlags);
            parcel.writeIntArray(this.splitPrivateFlags);
            parcel.writeInt(this.baseHardwareAccelerated ? 1 : 0);
            parcel.writeParcelable(this.applicationInfo, i);
            parcel.writeParcelableList(this.permissions, i);
            parcel.writeParcelableList(this.permissionGroups, i);
            parcel.writeParcelableList(this.activities, i);
            parcel.writeParcelableList(this.receivers, i);
            parcel.writeParcelableList(this.providers, i);
            parcel.writeParcelableList(this.services, i);
            parcel.writeParcelableList(this.instrumentation, i);
            parcel.writeStringList(this.requestedPermissions);
            parcel.writeStringList(this.protectedBroadcasts);
            parcel.writeParcelable(this.parentPackage, i);
            parcel.writeParcelableList(this.childPackages, i);
            parcel.writeString(this.staticSharedLibName);
            parcel.writeLong(this.staticSharedLibVersion);
            parcel.writeStringList(this.libraryNames);
            parcel.writeStringList(this.usesLibraries);
            parcel.writeStringList(this.usesOptionalLibraries);
            parcel.writeStringArray(this.usesLibraryFiles);
            if (ArrayUtils.isEmpty(this.usesStaticLibraries)) {
                parcel.writeInt(-1);
            } else {
                parcel.writeInt(this.usesStaticLibraries.size());
                parcel.writeStringList(this.usesStaticLibraries);
                parcel.writeLongArray(this.usesStaticLibrariesVersions);
                for (String[] strArr : this.usesStaticLibrariesCertDigests) {
                    parcel.writeStringArray(strArr);
                }
            }
            parcel.writeParcelableList(this.preferredActivityFilters, i);
            parcel.writeStringList(this.mOriginalPackages);
            parcel.writeString(this.mRealPackage);
            parcel.writeStringList(this.mAdoptPermissions);
            parcel.writeBundle(this.mAppMetaData);
            parcel.writeInt(this.mVersionCode);
            parcel.writeInt(this.mVersionCodeMajor);
            parcel.writeString(this.mVersionName);
            parcel.writeString(this.mSharedUserId);
            parcel.writeInt(this.mSharedUserLabel);
            parcel.writeParcelable(this.mSigningDetails, i);
            parcel.writeInt(this.mPreferredOrder);
            parcel.writeParcelableList(this.configPreferences, i);
            parcel.writeParcelableList(this.reqFeatures, i);
            parcel.writeParcelableList(this.featureGroups, i);
            parcel.writeInt(this.installLocation);
            parcel.writeInt(this.coreApp ? 1 : 0);
            parcel.writeInt(this.mRequiredForAllUsers ? 1 : 0);
            parcel.writeString(this.mRestrictedAccountType);
            parcel.writeString(this.mRequiredAccountType);
            parcel.writeString(this.mOverlayTarget);
            parcel.writeString(this.mOverlayCategory);
            parcel.writeInt(this.mOverlayPriority);
            parcel.writeInt(this.mOverlayIsStatic ? 1 : 0);
            parcel.writeInt(this.mCompileSdkVersion);
            parcel.writeString(this.mCompileSdkVersionCodename);
            parcel.writeArraySet(this.mUpgradeKeySets);
            writeKeySetMapping(parcel, this.mKeySetMapping);
            parcel.writeString(this.cpuAbiOverride);
            parcel.writeInt(this.use32bitAbi ? 1 : 0);
            parcel.writeByteArray(this.restrictUpdateHash);
            parcel.writeInt(this.visibleToInstantApps ? 1 : 0);
        }

        private static void writeKeySetMapping(Parcel parcel, ArrayMap<String, ArraySet<PublicKey>> arrayMap) {
            if (arrayMap == null) {
                parcel.writeInt(-1);
                return;
            }
            int size = arrayMap.size();
            parcel.writeInt(size);
            for (int i = 0; i < size; i++) {
                parcel.writeString(arrayMap.keyAt(i));
                ArraySet<PublicKey> arraySetValueAt = arrayMap.valueAt(i);
                if (arraySetValueAt == null) {
                    parcel.writeInt(-1);
                } else {
                    int size2 = arraySetValueAt.size();
                    parcel.writeInt(size2);
                    for (int i2 = 0; i2 < size2; i2++) {
                        parcel.writeSerializable(arraySetValueAt.valueAt(i2));
                    }
                }
            }
        }

        private static ArrayMap<String, ArraySet<PublicKey>> readKeySetMapping(Parcel parcel) {
            int i = parcel.readInt();
            if (i == -1) {
                return null;
            }
            ArrayMap<String, ArraySet<PublicKey>> arrayMap = new ArrayMap<>();
            for (int i2 = 0; i2 < i; i2++) {
                String string = parcel.readString();
                int i3 = parcel.readInt();
                if (i3 == -1) {
                    arrayMap.put(string, null);
                } else {
                    ArraySet<PublicKey> arraySet = new ArraySet<>(i3);
                    for (int i4 = 0; i4 < i3; i4++) {
                        arraySet.add((PublicKey) parcel.readSerializable());
                    }
                    arrayMap.put(string, arraySet);
                }
            }
            return arrayMap;
        }
    }

    public static abstract class Component<II extends IntentInfo> {
        public final String className;
        ComponentName componentName;
        String componentShortName;
        public final ArrayList<II> intents;
        public Bundle metaData;
        public int order;
        public Package owner;

        public Component(Package r1) {
            this.owner = r1;
            this.intents = null;
            this.className = null;
        }

        public Component(ParsePackageItemArgs parsePackageItemArgs, PackageItemInfo packageItemInfo) {
            this.owner = parsePackageItemArgs.owner;
            this.intents = new ArrayList<>(0);
            if (PackageParser.parsePackageItemInfo(parsePackageItemArgs.owner, packageItemInfo, parsePackageItemArgs.outError, parsePackageItemArgs.tag, parsePackageItemArgs.sa, true, parsePackageItemArgs.nameRes, parsePackageItemArgs.labelRes, parsePackageItemArgs.iconRes, parsePackageItemArgs.roundIconRes, parsePackageItemArgs.logoRes, parsePackageItemArgs.bannerRes)) {
                this.className = packageItemInfo.name;
            } else {
                this.className = null;
            }
        }

        public Component(ParseComponentArgs parseComponentArgs, ComponentInfo componentInfo) {
            String nonResourceString;
            this((ParsePackageItemArgs) parseComponentArgs, (PackageItemInfo) componentInfo);
            if (parseComponentArgs.outError[0] != null) {
                return;
            }
            if (parseComponentArgs.processRes != 0) {
                if (this.owner.applicationInfo.targetSdkVersion >= 8) {
                    nonResourceString = parseComponentArgs.sa.getNonConfigurationString(parseComponentArgs.processRes, 1024);
                } else {
                    nonResourceString = parseComponentArgs.sa.getNonResourceString(parseComponentArgs.processRes);
                }
                componentInfo.processName = PackageParser.buildProcessName(this.owner.applicationInfo.packageName, this.owner.applicationInfo.processName, nonResourceString, parseComponentArgs.flags, parseComponentArgs.sepProcesses, parseComponentArgs.outError);
            }
            if (parseComponentArgs.descriptionRes != 0) {
                componentInfo.descriptionRes = parseComponentArgs.sa.getResourceId(parseComponentArgs.descriptionRes, 0);
            }
            componentInfo.enabled = parseComponentArgs.sa.getBoolean(parseComponentArgs.enabledRes, true);
        }

        public Component(Component<II> component) {
            this.owner = component.owner;
            this.intents = component.intents;
            this.className = component.className;
            this.componentName = component.componentName;
            this.componentShortName = component.componentShortName;
        }

        public ComponentName getComponentName() {
            if (this.componentName != null) {
                return this.componentName;
            }
            if (this.className != null) {
                this.componentName = new ComponentName(this.owner.applicationInfo.packageName, this.className);
            }
            return this.componentName;
        }

        protected Component(Parcel parcel) {
            this.className = parcel.readString();
            this.metaData = parcel.readBundle();
            this.intents = createIntentsList(parcel);
            this.owner = null;
        }

        protected void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(this.className);
            parcel.writeBundle(this.metaData);
            writeIntentsList(this.intents, parcel, i);
        }

        private static void writeIntentsList(ArrayList<? extends IntentInfo> arrayList, Parcel parcel, int i) {
            if (arrayList == null) {
                parcel.writeInt(-1);
                return;
            }
            int size = arrayList.size();
            parcel.writeInt(size);
            if (size > 0) {
                parcel.writeString(arrayList.get(0).getClass().getName());
                for (int i2 = 0; i2 < size; i2++) {
                    arrayList.get(i2).writeIntentInfoToParcel(parcel, i);
                }
            }
        }

        private static <T extends IntentInfo> ArrayList<T> createIntentsList(Parcel parcel) {
            int i = parcel.readInt();
            if (i == -1) {
                return null;
            }
            if (i == 0) {
                return new ArrayList<>(0);
            }
            String string = parcel.readString();
            try {
                Constructor<?> constructor = Class.forName(string).getConstructor(Parcel.class);
                GridLayout.Assoc assoc = (ArrayList<T>) new ArrayList(i);
                for (int i2 = 0; i2 < i; i2++) {
                    assoc.add((IntentInfo) constructor.newInstance(parcel));
                }
                return assoc;
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Unable to construct intent list for: " + string);
            }
        }

        public void appendComponentShortName(StringBuilder sb) {
            ComponentName.appendShortString(sb, this.owner.applicationInfo.packageName, this.className);
        }

        public void printComponentShortName(PrintWriter printWriter) {
            ComponentName.printShortString(printWriter, this.owner.applicationInfo.packageName, this.className);
        }

        public void setPackageName(String str) {
            this.componentName = null;
            this.componentShortName = null;
        }
    }

    public static final class Permission extends Component<IntentInfo> implements Parcelable {
        public static final Parcelable.Creator CREATOR = new Parcelable.Creator<Permission>() {
            @Override
            public Permission createFromParcel(Parcel parcel) {
                return new Permission(parcel);
            }

            @Override
            public Permission[] newArray(int i) {
                return new Permission[i];
            }
        };
        public PermissionGroup group;
        public final PermissionInfo info;
        public boolean tree;

        public Permission(Package r1) {
            super(r1);
            this.info = new PermissionInfo();
        }

        public Permission(Package r1, PermissionInfo permissionInfo) {
            super(r1);
            this.info = permissionInfo;
        }

        @Override
        public void setPackageName(String str) {
            super.setPackageName(str);
            this.info.packageName = str;
        }

        public String toString() {
            return "Permission{" + Integer.toHexString(System.identityHashCode(this)) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.info.name + "}";
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeParcelable(this.info, i);
            parcel.writeInt(this.tree ? 1 : 0);
            parcel.writeParcelable(this.group, i);
        }

        public boolean isAppOp() {
            return this.info.isAppOp();
        }

        private Permission(Parcel parcel) {
            super(parcel);
            ClassLoader classLoader = Object.class.getClassLoader();
            this.info = (PermissionInfo) parcel.readParcelable(classLoader);
            if (this.info.group != null) {
                this.info.group = this.info.group.intern();
            }
            this.tree = parcel.readInt() == 1;
            this.group = (PermissionGroup) parcel.readParcelable(classLoader);
        }
    }

    public static final class PermissionGroup extends Component<IntentInfo> implements Parcelable {
        public static final Parcelable.Creator CREATOR = new Parcelable.Creator<PermissionGroup>() {
            @Override
            public PermissionGroup createFromParcel(Parcel parcel) {
                return new PermissionGroup(parcel);
            }

            @Override
            public PermissionGroup[] newArray(int i) {
                return new PermissionGroup[i];
            }
        };
        public final PermissionGroupInfo info;

        public PermissionGroup(Package r1) {
            super(r1);
            this.info = new PermissionGroupInfo();
        }

        public PermissionGroup(Package r1, PermissionGroupInfo permissionGroupInfo) {
            super(r1);
            this.info = permissionGroupInfo;
        }

        @Override
        public void setPackageName(String str) {
            super.setPackageName(str);
            this.info.packageName = str;
        }

        public String toString() {
            return "PermissionGroup{" + Integer.toHexString(System.identityHashCode(this)) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.info.name + "}";
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeParcelable(this.info, i);
        }

        private PermissionGroup(Parcel parcel) {
            super(parcel);
            this.info = (PermissionGroupInfo) parcel.readParcelable(Object.class.getClassLoader());
        }
    }

    private static boolean copyNeeded(int i, Package r4, PackageUserState packageUserState, Bundle bundle, int i2) {
        if (i2 != 0) {
            return true;
        }
        if (packageUserState.enabled != 0) {
            if (r4.applicationInfo.enabled != (packageUserState.enabled == 1)) {
                return true;
            }
        }
        if (packageUserState.suspended != ((r4.applicationInfo.flags & 1073741824) != 0) || !packageUserState.installed || packageUserState.hidden || packageUserState.stopped || packageUserState.instantApp != r4.applicationInfo.isInstantApp()) {
            return true;
        }
        if ((i & 128) == 0 || (bundle == null && r4.mAppMetaData == null)) {
            return (((i & 1024) == 0 || r4.usesLibraryFiles == null) && r4.staticSharedLibName == null) ? false : true;
        }
        return true;
    }

    public static ApplicationInfo generateApplicationInfo(Package r1, int i, PackageUserState packageUserState) {
        return generateApplicationInfo(r1, i, packageUserState, UserHandle.getCallingUserId());
    }

    private static void updateApplicationInfo(ApplicationInfo applicationInfo, int i, PackageUserState packageUserState) {
        if (!sCompatibilityModeEnabled) {
            applicationInfo.disableCompatibilityMode();
        }
        if (packageUserState.installed) {
            applicationInfo.flags |= 8388608;
        } else {
            applicationInfo.flags &= -8388609;
        }
        if (packageUserState.suspended) {
            applicationInfo.flags |= 1073741824;
        } else {
            applicationInfo.flags &= -1073741825;
        }
        if (packageUserState.instantApp) {
            applicationInfo.privateFlags |= 128;
        } else {
            applicationInfo.privateFlags &= -129;
        }
        if (packageUserState.virtualPreload) {
            applicationInfo.privateFlags |= 65536;
        } else {
            applicationInfo.privateFlags &= -65537;
        }
        if (packageUserState.hidden) {
            applicationInfo.privateFlags |= 1;
        } else {
            applicationInfo.privateFlags &= -2;
        }
        if (packageUserState.enabled == 1) {
            applicationInfo.enabled = true;
        } else if (packageUserState.enabled == 4) {
            applicationInfo.enabled = (i & 32768) != 0;
        } else if (packageUserState.enabled == 2 || packageUserState.enabled == 3) {
            applicationInfo.enabled = false;
        }
        applicationInfo.enabledSetting = packageUserState.enabled;
        if (applicationInfo.category == -1) {
            applicationInfo.category = packageUserState.categoryHint;
        }
        if (applicationInfo.category == -1) {
            applicationInfo.category = FallbackCategoryProvider.getFallbackCategory(applicationInfo.packageName);
        }
        applicationInfo.seInfoUser = SELinuxUtil.assignSeinfoUser(packageUserState);
        applicationInfo.resourceDirs = packageUserState.overlayPaths;
    }

    public static ApplicationInfo generateApplicationInfo(Package r2, int i, PackageUserState packageUserState, int i2) {
        if (r2 == null || !checkUseInstalledOrHidden(i, packageUserState, r2.applicationInfo) || !r2.isMatch(i)) {
            return null;
        }
        if (!copyNeeded(i, r2, packageUserState, null, i2) && ((32768 & i) == 0 || packageUserState.enabled != 4)) {
            updateApplicationInfo(r2.applicationInfo, i, packageUserState);
            return r2.applicationInfo;
        }
        ApplicationInfo applicationInfo = new ApplicationInfo(r2.applicationInfo);
        applicationInfo.initForUser(i2);
        if ((i & 128) != 0) {
            applicationInfo.metaData = r2.mAppMetaData;
        }
        if ((i & 1024) != 0) {
            applicationInfo.sharedLibraryFiles = r2.usesLibraryFiles;
        }
        if (packageUserState.stopped) {
            applicationInfo.flags |= 2097152;
        } else {
            applicationInfo.flags &= -2097153;
        }
        updateApplicationInfo(applicationInfo, i, packageUserState);
        return applicationInfo;
    }

    public static ApplicationInfo generateApplicationInfo(ApplicationInfo applicationInfo, int i, PackageUserState packageUserState, int i2) {
        if (applicationInfo == null || !checkUseInstalledOrHidden(i, packageUserState, applicationInfo)) {
            return null;
        }
        ApplicationInfo applicationInfo2 = new ApplicationInfo(applicationInfo);
        applicationInfo2.initForUser(i2);
        if (packageUserState.stopped) {
            applicationInfo2.flags |= 2097152;
        } else {
            applicationInfo2.flags &= -2097153;
        }
        updateApplicationInfo(applicationInfo2, i, packageUserState);
        return applicationInfo2;
    }

    public static final PermissionInfo generatePermissionInfo(Permission permission, int i) {
        if (permission == null) {
            return null;
        }
        if ((i & 128) == 0) {
            return permission.info;
        }
        PermissionInfo permissionInfo = new PermissionInfo(permission.info);
        permissionInfo.metaData = permission.metaData;
        return permissionInfo;
    }

    public static final PermissionGroupInfo generatePermissionGroupInfo(PermissionGroup permissionGroup, int i) {
        if (permissionGroup == null) {
            return null;
        }
        if ((i & 128) == 0) {
            return permissionGroup.info;
        }
        PermissionGroupInfo permissionGroupInfo = new PermissionGroupInfo(permissionGroup.info);
        permissionGroupInfo.metaData = permissionGroup.metaData;
        return permissionGroupInfo;
    }

    public static final class Activity extends Component<ActivityIntentInfo> implements Parcelable {
        public static final Parcelable.Creator CREATOR = new Parcelable.Creator<Activity>() {
            @Override
            public Activity createFromParcel(Parcel parcel) {
                return new Activity(parcel);
            }

            @Override
            public Activity[] newArray(int i) {
                return new Activity[i];
            }
        };
        public final ActivityInfo info;
        private boolean mHasMaxAspectRatio;

        private boolean hasMaxAspectRatio() {
            return this.mHasMaxAspectRatio;
        }

        public Activity(ParseComponentArgs parseComponentArgs, ActivityInfo activityInfo) {
            super(parseComponentArgs, (ComponentInfo) activityInfo);
            this.info = activityInfo;
            this.info.applicationInfo = parseComponentArgs.owner.applicationInfo;
        }

        @Override
        public void setPackageName(String str) {
            super.setPackageName(str);
            this.info.packageName = str;
        }

        private void setMaxAspectRatio(float f) {
            if (this.info.resizeMode == 2 || this.info.resizeMode == 1) {
                return;
            }
            if (f < 1.0f && f != 0.0f) {
                return;
            }
            this.info.maxAspectRatio = f;
            this.mHasMaxAspectRatio = true;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Activity{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeParcelable(this.info, i | 2);
            parcel.writeBoolean(this.mHasMaxAspectRatio);
        }

        private Activity(Parcel parcel) {
            super(parcel);
            this.info = (ActivityInfo) parcel.readParcelable(Object.class.getClassLoader());
            this.mHasMaxAspectRatio = parcel.readBoolean();
            for (II ii : this.intents) {
                ii.activity = this;
                this.order = Math.max(ii.getOrder(), this.order);
            }
            if (this.info.permission != null) {
                this.info.permission = this.info.permission.intern();
            }
        }
    }

    public static final ActivityInfo generateActivityInfo(Activity activity, int i, PackageUserState packageUserState, int i2) {
        if (activity == null || !checkUseInstalledOrHidden(i, packageUserState, activity.owner.applicationInfo)) {
            return null;
        }
        if (!copyNeeded(i, activity.owner, packageUserState, activity.metaData, i2)) {
            updateApplicationInfo(activity.info.applicationInfo, i, packageUserState);
            return activity.info;
        }
        ActivityInfo activityInfo = new ActivityInfo(activity.info);
        activityInfo.metaData = activity.metaData;
        activityInfo.applicationInfo = generateApplicationInfo(activity.owner, i, packageUserState, i2);
        return activityInfo;
    }

    public static final ActivityInfo generateActivityInfo(ActivityInfo activityInfo, int i, PackageUserState packageUserState, int i2) {
        if (activityInfo == null || !checkUseInstalledOrHidden(i, packageUserState, activityInfo.applicationInfo)) {
            return null;
        }
        ActivityInfo activityInfo2 = new ActivityInfo(activityInfo);
        activityInfo2.applicationInfo = generateApplicationInfo(activityInfo2.applicationInfo, i, packageUserState, i2);
        return activityInfo2;
    }

    public static final class Service extends Component<ServiceIntentInfo> implements Parcelable {
        public static final Parcelable.Creator CREATOR = new Parcelable.Creator<Service>() {
            @Override
            public Service createFromParcel(Parcel parcel) {
                return new Service(parcel);
            }

            @Override
            public Service[] newArray(int i) {
                return new Service[i];
            }
        };
        public final ServiceInfo info;

        public Service(ParseComponentArgs parseComponentArgs, ServiceInfo serviceInfo) {
            super(parseComponentArgs, (ComponentInfo) serviceInfo);
            this.info = serviceInfo;
            this.info.applicationInfo = parseComponentArgs.owner.applicationInfo;
        }

        @Override
        public void setPackageName(String str) {
            super.setPackageName(str);
            this.info.packageName = str;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Service{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeParcelable(this.info, i | 2);
        }

        private Service(Parcel parcel) {
            super(parcel);
            this.info = (ServiceInfo) parcel.readParcelable(Object.class.getClassLoader());
            for (II ii : this.intents) {
                ii.service = this;
                this.order = Math.max(ii.getOrder(), this.order);
            }
            if (this.info.permission != null) {
                this.info.permission = this.info.permission.intern();
            }
        }
    }

    public static final ServiceInfo generateServiceInfo(Service service, int i, PackageUserState packageUserState, int i2) {
        if (service == null || !checkUseInstalledOrHidden(i, packageUserState, service.owner.applicationInfo)) {
            return null;
        }
        if (!copyNeeded(i, service.owner, packageUserState, service.metaData, i2)) {
            updateApplicationInfo(service.info.applicationInfo, i, packageUserState);
            return service.info;
        }
        ServiceInfo serviceInfo = new ServiceInfo(service.info);
        serviceInfo.metaData = service.metaData;
        serviceInfo.applicationInfo = generateApplicationInfo(service.owner, i, packageUserState, i2);
        return serviceInfo;
    }

    public static final class Provider extends Component<ProviderIntentInfo> implements Parcelable {
        public static final Parcelable.Creator CREATOR = new Parcelable.Creator<Provider>() {
            @Override
            public Provider createFromParcel(Parcel parcel) {
                return new Provider(parcel);
            }

            @Override
            public Provider[] newArray(int i) {
                return new Provider[i];
            }
        };
        public final ProviderInfo info;
        public boolean syncable;

        public Provider(ParseComponentArgs parseComponentArgs, ProviderInfo providerInfo) {
            super(parseComponentArgs, (ComponentInfo) providerInfo);
            this.info = providerInfo;
            this.info.applicationInfo = parseComponentArgs.owner.applicationInfo;
            this.syncable = false;
        }

        public Provider(Provider provider) {
            super(provider);
            this.info = provider.info;
            this.syncable = provider.syncable;
        }

        @Override
        public void setPackageName(String str) {
            super.setPackageName(str);
            this.info.packageName = str;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Provider{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeParcelable(this.info, i | 2);
            parcel.writeInt(this.syncable ? 1 : 0);
        }

        private Provider(Parcel parcel) {
            super(parcel);
            this.info = (ProviderInfo) parcel.readParcelable(Object.class.getClassLoader());
            this.syncable = parcel.readInt() == 1;
            Iterator it = this.intents.iterator();
            while (it.hasNext()) {
                ((ProviderIntentInfo) it.next()).provider = this;
            }
            if (this.info.readPermission != null) {
                this.info.readPermission = this.info.readPermission.intern();
            }
            if (this.info.writePermission != null) {
                this.info.writePermission = this.info.writePermission.intern();
            }
            if (this.info.authority != null) {
                this.info.authority = this.info.authority.intern();
            }
        }
    }

    public static final ProviderInfo generateProviderInfo(Provider provider, int i, PackageUserState packageUserState, int i2) {
        if (provider == null || !checkUseInstalledOrHidden(i, packageUserState, provider.owner.applicationInfo)) {
            return null;
        }
        if (!copyNeeded(i, provider.owner, packageUserState, provider.metaData, i2) && ((i & 2048) != 0 || provider.info.uriPermissionPatterns == null)) {
            updateApplicationInfo(provider.info.applicationInfo, i, packageUserState);
            return provider.info;
        }
        ProviderInfo providerInfo = new ProviderInfo(provider.info);
        providerInfo.metaData = provider.metaData;
        if ((i & 2048) == 0) {
            providerInfo.uriPermissionPatterns = null;
        }
        providerInfo.applicationInfo = generateApplicationInfo(provider.owner, i, packageUserState, i2);
        return providerInfo;
    }

    public static final class Instrumentation extends Component<IntentInfo> implements Parcelable {
        public static final Parcelable.Creator CREATOR = new Parcelable.Creator<Instrumentation>() {
            @Override
            public Instrumentation createFromParcel(Parcel parcel) {
                return new Instrumentation(parcel);
            }

            @Override
            public Instrumentation[] newArray(int i) {
                return new Instrumentation[i];
            }
        };
        public final InstrumentationInfo info;

        public Instrumentation(ParsePackageItemArgs parsePackageItemArgs, InstrumentationInfo instrumentationInfo) {
            super(parsePackageItemArgs, instrumentationInfo);
            this.info = instrumentationInfo;
        }

        @Override
        public void setPackageName(String str) {
            super.setPackageName(str);
            this.info.packageName = str;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Instrumentation{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeParcelable(this.info, i);
        }

        private Instrumentation(Parcel parcel) {
            super(parcel);
            this.info = (InstrumentationInfo) parcel.readParcelable(Object.class.getClassLoader());
            if (this.info.targetPackage != null) {
                this.info.targetPackage = this.info.targetPackage.intern();
            }
            if (this.info.targetProcesses != null) {
                this.info.targetProcesses = this.info.targetProcesses.intern();
            }
        }
    }

    public static final InstrumentationInfo generateInstrumentationInfo(Instrumentation instrumentation, int i) {
        if (instrumentation == null) {
            return null;
        }
        if ((i & 128) == 0) {
            return instrumentation.info;
        }
        InstrumentationInfo instrumentationInfo = new InstrumentationInfo(instrumentation.info);
        instrumentationInfo.metaData = instrumentation.metaData;
        return instrumentationInfo;
    }

    public static abstract class IntentInfo extends IntentFilter {
        public int banner;
        public boolean hasDefault;
        public int icon;
        public int labelRes;
        public int logo;
        public CharSequence nonLocalizedLabel;
        public int preferred;

        protected IntentInfo() {
        }

        protected IntentInfo(Parcel parcel) {
            super(parcel);
            this.hasDefault = parcel.readInt() == 1;
            this.labelRes = parcel.readInt();
            this.nonLocalizedLabel = parcel.readCharSequence();
            this.icon = parcel.readInt();
            this.logo = parcel.readInt();
            this.banner = parcel.readInt();
            this.preferred = parcel.readInt();
        }

        public void writeIntentInfoToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.hasDefault ? 1 : 0);
            parcel.writeInt(this.labelRes);
            parcel.writeCharSequence(this.nonLocalizedLabel);
            parcel.writeInt(this.icon);
            parcel.writeInt(this.logo);
            parcel.writeInt(this.banner);
            parcel.writeInt(this.preferred);
        }
    }

    public static final class ActivityIntentInfo extends IntentInfo {
        public Activity activity;

        public ActivityIntentInfo(Activity activity) {
            this.activity = activity;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ActivityIntentInfo{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            this.activity.appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }

        public ActivityIntentInfo(Parcel parcel) {
            super(parcel);
        }
    }

    public static final class ServiceIntentInfo extends IntentInfo {
        public Service service;

        public ServiceIntentInfo(Service service) {
            this.service = service;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ServiceIntentInfo{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            this.service.appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }

        public ServiceIntentInfo(Parcel parcel) {
            super(parcel);
        }
    }

    public static final class ProviderIntentInfo extends IntentInfo {
        public Provider provider;

        public ProviderIntentInfo(Provider provider) {
            this.provider = provider;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ProviderIntentInfo{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            this.provider.appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }

        public ProviderIntentInfo(Parcel parcel) {
            super(parcel);
        }
    }

    public static void setCompatibilityModeEnabled(boolean z) {
        sCompatibilityModeEnabled = z;
    }

    public static class PackageParserException extends Exception {
        public final int error;

        public PackageParserException(int i, String str) {
            super(str);
            this.error = i;
        }

        public PackageParserException(int i, String str, Throwable th) {
            super(str, th);
            this.error = i;
        }
    }
}
