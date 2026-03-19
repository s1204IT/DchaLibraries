package android.content.pm;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.hardware.Camera;
import android.media.TtmlUtils;
import android.net.ProxyInfo;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.PatternMatcher;
import android.os.Trace;
import android.os.UserHandle;
import android.security.keystore.KeyProperties;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.TypedValue;
import android.util.apk.ApkSignatureSchemeV2Verifier;
import android.util.jar.StrictJarFile;
import com.android.internal.R;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import dalvik.system.BlockGuard;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class PackageParser {
    private static final String ANDROID_MANIFEST_FILENAME = "AndroidManifest.xml";
    private static final String ANDROID_RESOURCES = "http://schemas.android.com/apk/res/android";
    public static final int APK_SIGNING_UNKNOWN = 0;
    public static final int APK_SIGNING_V1 = 1;
    public static final int APK_SIGNING_V2 = 2;
    private static final Set<String> CHILD_PACKAGE_TAGS = new ArraySet();
    private static final boolean DEBUG_BACKUP = false;
    private static final boolean DEBUG_JAR = false;
    private static final boolean DEBUG_PARSER = false;
    private static final boolean IsForceHardwareAccelerated = false;
    private static final int MAX_PACKAGES_PER_APK = 5;
    private static final String MNT_EXPAND = "/mnt/expand/";
    private static final boolean MULTI_PACKAGE_APK_ENABLED = false;
    public static final NewPermissionInfo[] NEW_PERMISSIONS;
    public static final int PARSE_CHATTY = 2;
    public static final int PARSE_COLLECT_CERTIFICATES = 256;
    private static final int PARSE_DEFAULT_INSTALL_LOCATION = -1;
    public static final int PARSE_ENFORCE_CODE = 1024;
    public static final int PARSE_EXTERNAL_STORAGE = 32;
    public static final int PARSE_FORCE_SDK = 4096;
    public static final int PARSE_FORWARD_LOCK = 16;
    public static final int PARSE_IGNORE_PROCESSES = 8;
    public static final int PARSE_IS_EPHEMERAL = 2048;
    public static final int PARSE_IS_OPERATOR = 8192;
    public static final int PARSE_IS_PRIVILEGED = 128;
    public static final int PARSE_IS_SYSTEM = 1;
    public static final int PARSE_IS_SYSTEM_DIR = 64;
    public static final int PARSE_MUST_BE_APK = 4;
    public static final int PARSE_TRUSTED_OVERLAY = 512;
    private static final boolean RIGID_PARSER = false;
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
    private static AtomicReference<byte[]> sBuffer;
    private static boolean sCompatibilityModeEnabled;
    private static final Comparator<String> sSplitNameComparator;

    @Deprecated
    private String mArchiveSourcePath;
    private boolean mOnlyCoreApps;
    private ParseComponentArgs mParseActivityAliasArgs;
    private ParseComponentArgs mParseActivityArgs;
    private ParsePackageItemArgs mParseInstrumentationArgs;
    private ParseComponentArgs mParseProviderArgs;
    private ParseComponentArgs mParseServiceArgs;
    private String[] mSeparateProcesses;
    private int mParseError = 1;
    private DisplayMetrics mMetrics = new DisplayMetrics();

    public static class IntentInfo extends IntentFilter {
        public int banner;
        public boolean hasDefault;
        public int icon;
        public int labelRes;
        public int logo;
        public CharSequence nonLocalizedLabel;
        public int preferred;
    }

    static {
        CHILD_PACKAGE_TAGS.add("application");
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
        NEW_PERMISSIONS = new NewPermissionInfo[]{new NewPermissionInfo(Manifest.permission.WRITE_EXTERNAL_STORAGE, 4, 0), new NewPermissionInfo(Manifest.permission.READ_PHONE_STATE, 4, 0)};
        SPLIT_PERMISSIONS = new SplitPermissionInfo[]{new SplitPermissionInfo(Manifest.permission.WRITE_EXTERNAL_STORAGE, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 10001), new SplitPermissionInfo(Manifest.permission.READ_CONTACTS, new String[]{Manifest.permission.READ_CALL_LOG}, 16), new SplitPermissionInfo(Manifest.permission.WRITE_CONTACTS, new String[]{Manifest.permission.WRITE_CALL_LOG}, 16)};
        SDK_VERSION = Build.VERSION.SDK_INT;
        SDK_CODENAMES = Build.VERSION.ACTIVE_CODENAMES;
        sCompatibilityModeEnabled = true;
        sSplitNameComparator = new SplitNameComparator(null);
        sBuffer = new AtomicReference<>();
    }

    public static class NewPermissionInfo {
        public final int fileVersion;
        public final String name;
        public final int sdkVersion;

        public NewPermissionInfo(String name, int sdkVersion, int fileVersion) {
            this.name = name;
            this.sdkVersion = sdkVersion;
            this.fileVersion = fileVersion;
        }
    }

    public static class SplitPermissionInfo {
        public final String[] newPerms;
        public final String rootPerm;
        public final int targetSdk;

        public SplitPermissionInfo(String rootPerm, String[] newPerms, int targetSdk) {
            this.rootPerm = rootPerm;
            this.newPerms = newPerms;
            this.targetSdk = targetSdk;
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
        TypedArray sa;
        String tag;

        ParsePackageItemArgs(Package _owner, String[] _outError, int _nameRes, int _labelRes, int _iconRes, int _logoRes, int _bannerRes) {
            this.owner = _owner;
            this.outError = _outError;
            this.nameRes = _nameRes;
            this.labelRes = _labelRes;
            this.iconRes = _iconRes;
            this.logoRes = _logoRes;
            this.bannerRes = _bannerRes;
        }
    }

    static class ParseComponentArgs extends ParsePackageItemArgs {
        final int descriptionRes;
        final int enabledRes;
        int flags;
        final int processRes;
        final String[] sepProcesses;

        ParseComponentArgs(Package _owner, String[] _outError, int _nameRes, int _labelRes, int _iconRes, int _logoRes, int _bannerRes, String[] _sepProcesses, int _processRes, int _descriptionRes, int _enabledRes) {
            super(_owner, _outError, _nameRes, _labelRes, _iconRes, _logoRes, _bannerRes);
            this.sepProcesses = _sepProcesses;
            this.processRes = _processRes;
            this.descriptionRes = _descriptionRes;
            this.enabledRes = _enabledRes;
        }
    }

    public static class PackageLite {
        public final String baseCodePath;
        public final int baseRevisionCode;
        public final String codePath;
        public final boolean coreApp;
        public final boolean extractNativeLibs;
        public final int installLocation;
        public final boolean multiArch;
        public final String packageName;
        public final String[] splitCodePaths;
        public final String[] splitNames;
        public final int[] splitRevisionCodes;
        public final boolean use32bitAbi;
        public final VerifierInfo[] verifiers;
        public final int versionCode;

        public PackageLite(String codePath, ApkLite baseApk, String[] splitNames, String[] splitCodePaths, int[] splitRevisionCodes) {
            this.packageName = baseApk.packageName;
            this.versionCode = baseApk.versionCode;
            this.installLocation = baseApk.installLocation;
            this.verifiers = baseApk.verifiers;
            this.splitNames = splitNames;
            this.codePath = codePath;
            this.baseCodePath = baseApk.codePath;
            this.splitCodePaths = splitCodePaths;
            this.baseRevisionCode = baseApk.revisionCode;
            this.splitRevisionCodes = splitRevisionCodes;
            this.coreApp = baseApk.coreApp;
            this.multiArch = baseApk.multiArch;
            this.use32bitAbi = baseApk.use32bitAbi;
            this.extractNativeLibs = baseApk.extractNativeLibs;
        }

        public List<String> getAllCodePaths() {
            ArrayList<String> paths = new ArrayList<>();
            paths.add(this.baseCodePath);
            if (!ArrayUtils.isEmpty(this.splitCodePaths)) {
                Collections.addAll(paths, this.splitCodePaths);
            }
            return paths;
        }
    }

    public static class ApkLite {
        public final Certificate[][] certificates;
        public final String codePath;
        public final boolean coreApp;
        public final boolean extractNativeLibs;
        public final int installLocation;
        public final boolean multiArch;
        public final String packageName;
        public final int revisionCode;
        public final Signature[] signatures;
        public final String splitName;
        public final boolean use32bitAbi;
        public final VerifierInfo[] verifiers;
        public final int versionCode;

        public ApkLite(String codePath, String packageName, String splitName, int versionCode, int revisionCode, int installLocation, List<VerifierInfo> verifiers, Signature[] signatures, Certificate[][] certificates, boolean coreApp, boolean multiArch, boolean use32bitAbi, boolean extractNativeLibs) {
            this.codePath = codePath;
            this.packageName = packageName;
            this.splitName = splitName;
            this.versionCode = versionCode;
            this.revisionCode = revisionCode;
            this.installLocation = installLocation;
            this.verifiers = (VerifierInfo[]) verifiers.toArray(new VerifierInfo[verifiers.size()]);
            this.signatures = signatures;
            this.certificates = certificates;
            this.coreApp = coreApp;
            this.multiArch = multiArch;
            this.use32bitAbi = use32bitAbi;
            this.extractNativeLibs = extractNativeLibs;
        }
    }

    public PackageParser() {
        this.mMetrics.setToDefaults();
    }

    public void setSeparateProcesses(String[] procs) {
        this.mSeparateProcesses = procs;
    }

    public void setOnlyCoreApps(boolean onlyCoreApps) {
        this.mOnlyCoreApps = onlyCoreApps;
    }

    public void setDisplayMetrics(DisplayMetrics metrics) {
        this.mMetrics = metrics;
    }

    public static final boolean isApkFile(File file) {
        return isApkPath(file.getName());
    }

    private static boolean isApkPath(String path) {
        return path.endsWith(".apk");
    }

    public static PackageInfo generatePackageInfo(Package p, int[] gids, int flags, long firstInstallTime, long lastUpdateTime, Set<String> grantedPermissions, PackageUserState state) {
        return generatePackageInfo(p, gids, flags, firstInstallTime, lastUpdateTime, grantedPermissions, state, UserHandle.getCallingUserId());
    }

    private static boolean checkUseInstalledOrHidden(int flags, PackageUserState state) {
        return (state.installed && !state.hidden) || (flags & 8192) != 0;
    }

    public static boolean isAvailable(PackageUserState state) {
        return checkUseInstalledOrHidden(0, state);
    }

    public static PackageInfo generatePackageInfo(Package p, int[] gids, int flags, long firstInstallTime, long lastUpdateTime, Set<String> grantedPermissions, PackageUserState state, int userId) {
        int N;
        int N2;
        int num;
        int N3;
        int num2;
        int N4;
        int num3;
        int N5;
        int num4;
        if (!checkUseInstalledOrHidden(flags, state) || !p.isMatch(flags)) {
            return null;
        }
        PackageInfo pi = new PackageInfo();
        pi.packageName = p.packageName;
        pi.splitNames = p.splitNames;
        pi.versionCode = p.mVersionCode;
        pi.baseRevisionCode = p.baseRevisionCode;
        pi.splitRevisionCodes = p.splitRevisionCodes;
        pi.versionName = p.mVersionName;
        pi.sharedUserId = p.mSharedUserId;
        pi.sharedUserLabel = p.mSharedUserLabel;
        pi.applicationInfo = generateApplicationInfo(p, flags, state, userId);
        pi.installLocation = p.installLocation;
        pi.coreApp = p.coreApp;
        if ((pi.applicationInfo.flags & 1) != 0 || (pi.applicationInfo.flags & 128) != 0) {
            pi.requiredForAllUsers = p.mRequiredForAllUsers;
        }
        pi.restrictedAccountType = p.mRestrictedAccountType;
        pi.requiredAccountType = p.mRequiredAccountType;
        pi.overlayTarget = p.mOverlayTarget;
        pi.firstInstallTime = firstInstallTime;
        pi.lastUpdateTime = lastUpdateTime;
        if ((flags & 256) != 0) {
            pi.gids = gids;
        }
        if ((flags & 16384) != 0) {
            int N6 = p.configPreferences != null ? p.configPreferences.size() : 0;
            if (N6 > 0) {
                pi.configPreferences = new ConfigurationInfo[N6];
                p.configPreferences.toArray(pi.configPreferences);
            }
            int N7 = p.reqFeatures != null ? p.reqFeatures.size() : 0;
            if (N7 > 0) {
                pi.reqFeatures = new FeatureInfo[N7];
                p.reqFeatures.toArray(pi.reqFeatures);
            }
            int N8 = p.featureGroups != null ? p.featureGroups.size() : 0;
            if (N8 > 0) {
                pi.featureGroups = new FeatureGroupInfo[N8];
                p.featureGroups.toArray(pi.featureGroups);
            }
        }
        if ((flags & 1) != 0 && (N5 = p.activities.size()) > 0) {
            ActivityInfo[] res = new ActivityInfo[N5];
            int i = 0;
            int num5 = 0;
            while (i < N5) {
                Activity a = p.activities.get(i);
                if (state.isMatch(a.info, flags)) {
                    num4 = num5 + 1;
                    res[num5] = generateActivityInfo(a, flags, state, userId);
                } else {
                    num4 = num5;
                }
                i++;
                num5 = num4;
            }
            pi.activities = (ActivityInfo[]) ArrayUtils.trimToSize(res, num5);
        }
        if ((flags & 2) != 0 && (N4 = p.receivers.size()) > 0) {
            ActivityInfo[] res2 = new ActivityInfo[N4];
            int i2 = 0;
            int num6 = 0;
            while (i2 < N4) {
                Activity a2 = p.receivers.get(i2);
                if (state.isMatch(a2.info, flags)) {
                    num3 = num6 + 1;
                    res2[num6] = generateActivityInfo(a2, flags, state, userId);
                } else {
                    num3 = num6;
                }
                i2++;
                num6 = num3;
            }
            pi.receivers = (ActivityInfo[]) ArrayUtils.trimToSize(res2, num6);
        }
        if ((flags & 4) != 0 && (N3 = p.services.size()) > 0) {
            ServiceInfo[] res3 = new ServiceInfo[N3];
            int i3 = 0;
            int num7 = 0;
            while (i3 < N3) {
                Service s = p.services.get(i3);
                if (state.isMatch(s.info, flags)) {
                    num2 = num7 + 1;
                    res3[num7] = generateServiceInfo(s, flags, state, userId);
                } else {
                    num2 = num7;
                }
                i3++;
                num7 = num2;
            }
            pi.services = (ServiceInfo[]) ArrayUtils.trimToSize(res3, num7);
        }
        if ((flags & 8) != 0 && (N2 = p.providers.size()) > 0) {
            ProviderInfo[] res4 = new ProviderInfo[N2];
            int i4 = 0;
            int num8 = 0;
            while (i4 < N2) {
                Provider pr = p.providers.get(i4);
                if (state.isMatch(pr.info, flags)) {
                    num = num8 + 1;
                    res4[num8] = generateProviderInfo(pr, flags, state, userId);
                } else {
                    num = num8;
                }
                i4++;
                num8 = num;
            }
            pi.providers = (ProviderInfo[]) ArrayUtils.trimToSize(res4, num8);
        }
        if ((flags & 16) != 0 && (N = p.instrumentation.size()) > 0) {
            pi.instrumentation = new InstrumentationInfo[N];
            for (int i5 = 0; i5 < N; i5++) {
                pi.instrumentation[i5] = generateInstrumentationInfo(p.instrumentation.get(i5), flags);
            }
        }
        if ((flags & 4096) != 0) {
            int N9 = p.permissions.size();
            if (N9 > 0) {
                pi.permissions = new PermissionInfo[N9];
                for (int i6 = 0; i6 < N9; i6++) {
                    pi.permissions[i6] = generatePermissionInfo(p.permissions.get(i6), flags);
                }
            }
            int N10 = p.requestedPermissions.size();
            if (N10 > 0) {
                pi.requestedPermissions = new String[N10];
                pi.requestedPermissionsFlags = new int[N10];
                for (int i7 = 0; i7 < N10; i7++) {
                    String perm = p.requestedPermissions.get(i7);
                    pi.requestedPermissions[i7] = perm;
                    int[] iArr = pi.requestedPermissionsFlags;
                    iArr[i7] = iArr[i7] | 1;
                    if (grantedPermissions != null && grantedPermissions.contains(perm)) {
                        int[] iArr2 = pi.requestedPermissionsFlags;
                        iArr2[i7] = iArr2[i7] | 2;
                    }
                }
            }
        }
        if ((flags & 64) != 0) {
            int N11 = p.mSignatures != null ? p.mSignatures.length : 0;
            if (N11 > 0) {
                pi.signatures = new Signature[N11];
                System.arraycopy(p.mSignatures, 0, pi.signatures, 0, N11);
            }
        }
        return pi;
    }

    private static Certificate[][] loadCertificates(StrictJarFile jarFile, ZipEntry entry) throws PackageParserException {
        InputStream is = null;
        try {
            try {
                is = jarFile.getInputStream(entry);
                readFullyIgnoringContents(is);
                return jarFile.getCertificateChains(entry);
            } catch (IOException | RuntimeException e) {
                throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed reading " + entry.getName() + " in " + jarFile, e);
            }
        } finally {
            IoUtils.closeQuietly(is);
        }
    }

    private static class SplitNameComparator implements Comparator<String> {
        SplitNameComparator(SplitNameComparator splitNameComparator) {
            this();
        }

        private SplitNameComparator() {
        }

        @Override
        public int compare(String lhs, String rhs) {
            if (lhs == null) {
                return -1;
            }
            if (rhs == null) {
                return 1;
            }
            return lhs.compareTo(rhs);
        }
    }

    public static PackageLite parsePackageLite(File packageFile, int flags) throws PackageParserException {
        if (packageFile.isDirectory()) {
            return parseClusterPackageLite(packageFile, flags);
        }
        return parseMonolithicPackageLite(packageFile, flags);
    }

    private static PackageLite parseMonolithicPackageLite(File packageFile, int flags) throws Throwable {
        ApkLite baseApk = parseApkLite(packageFile, flags);
        String packagePath = packageFile.getAbsolutePath();
        return new PackageLite(packagePath, baseApk, null, null, null);
    }

    private static PackageLite parseClusterPackageLite(File packageDir, int flags) throws Throwable {
        File[] files = packageDir.listFiles();
        if (ArrayUtils.isEmpty(files)) {
            throw new PackageParserException(-100, "No packages found in split");
        }
        String packageName = null;
        int versionCode = 0;
        ArrayMap<String, ApkLite> apks = new ArrayMap<>();
        for (File file : files) {
            if (isApkFile(file)) {
                ApkLite lite = parseApkLite(file, flags);
                if (packageName == null) {
                    packageName = lite.packageName;
                    versionCode = lite.versionCode;
                } else {
                    if (!packageName.equals(lite.packageName)) {
                        throw new PackageParserException(-101, "Inconsistent package " + lite.packageName + " in " + file + "; expected " + packageName);
                    }
                    if (versionCode != lite.versionCode) {
                        throw new PackageParserException(-101, "Inconsistent version " + lite.versionCode + " in " + file + "; expected " + versionCode);
                    }
                }
                if (apks.put(lite.splitName, lite) != null) {
                    throw new PackageParserException(-101, "Split name " + lite.splitName + " defined more than once; most recent was " + file);
                }
            }
        }
        ApkLite baseApk = apks.remove(null);
        if (baseApk == null) {
            throw new PackageParserException(-101, "Missing base APK in " + packageDir);
        }
        int size = apks.size();
        String[] splitNames = null;
        String[] splitCodePaths = null;
        int[] splitRevisionCodes = null;
        if (size > 0) {
            String[] splitNames2 = new String[size];
            splitCodePaths = new String[size];
            splitRevisionCodes = new int[size];
            splitNames = (String[]) apks.keySet().toArray(splitNames2);
            Arrays.sort(splitNames, sSplitNameComparator);
            for (int i = 0; i < size; i++) {
                splitCodePaths[i] = apks.get(splitNames[i]).codePath;
                splitRevisionCodes[i] = apks.get(splitNames[i]).revisionCode;
            }
        }
        String codePath = packageDir.getAbsolutePath();
        return new PackageLite(codePath, baseApk, splitNames, splitCodePaths, splitRevisionCodes);
    }

    public Package parsePackage(File packageFile, int flags) throws PackageParserException {
        if (packageFile.isDirectory()) {
            return parseClusterPackage(packageFile, flags);
        }
        return parseMonolithicPackage(packageFile, flags);
    }

    private Package parseClusterPackage(File packageDir, int flags) throws Throwable {
        PackageLite lite = parseClusterPackageLite(packageDir, 0);
        if (this.mOnlyCoreApps && !lite.coreApp) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "Not a coreApp: " + packageDir);
        }
        AssetManager assets = new AssetManager();
        try {
            loadApkIntoAssetManager(assets, lite.baseCodePath, flags);
            if (!ArrayUtils.isEmpty(lite.splitCodePaths)) {
                for (String path : lite.splitCodePaths) {
                    loadApkIntoAssetManager(assets, path, flags);
                }
            }
            File baseApk = new File(lite.baseCodePath);
            Package pkg = parseBaseApk(baseApk, assets, flags);
            if (pkg == null) {
                throw new PackageParserException(-100, "Failed to parse base APK: " + baseApk);
            }
            if (!ArrayUtils.isEmpty(lite.splitNames)) {
                int num = lite.splitNames.length;
                pkg.splitNames = lite.splitNames;
                pkg.splitCodePaths = lite.splitCodePaths;
                pkg.splitRevisionCodes = lite.splitRevisionCodes;
                pkg.splitFlags = new int[num];
                pkg.splitPrivateFlags = new int[num];
                for (int i = 0; i < num; i++) {
                    parseSplitApk(pkg, i, assets, flags);
                }
            }
            pkg.setCodePath(packageDir.getAbsolutePath());
            pkg.setUse32bitAbi(lite.use32bitAbi);
            return pkg;
        } finally {
            IoUtils.closeQuietly(assets);
        }
    }

    @Deprecated
    public Package parseMonolithicPackage(File apkFile, int flags) throws PackageParserException {
        PackageLite lite = parseMonolithicPackageLite(apkFile, flags);
        if (this.mOnlyCoreApps && !lite.coreApp) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "Not a coreApp: " + apkFile);
        }
        AssetManager assets = new AssetManager();
        try {
            Package pkg = parseBaseApk(apkFile, assets, flags);
            pkg.setCodePath(apkFile.getAbsolutePath());
            pkg.setUse32bitAbi(lite.use32bitAbi);
            return pkg;
        } finally {
            IoUtils.closeQuietly(assets);
        }
    }

    private static int loadApkIntoAssetManager(AssetManager assets, String apkPath, int flags) throws PackageParserException {
        if ((flags & 4) != 0 && !isApkPath(apkPath)) {
            throw new PackageParserException(-100, "Invalid package file: " + apkPath);
        }
        int cookie = assets.addAssetPath(apkPath);
        if (cookie == 0) {
            throw new PackageParserException(-101, "Failed adding asset path: " + apkPath);
        }
        return cookie;
    }

    private Package parseBaseApk(File apkFile, AssetManager assets, int flags) throws Throwable {
        Resources res;
        String apkPath = apkFile.getAbsolutePath();
        String volumeUuid = null;
        if (apkPath.startsWith(MNT_EXPAND)) {
            int end = apkPath.indexOf(47, MNT_EXPAND.length());
            volumeUuid = apkPath.substring(MNT_EXPAND.length(), end);
        }
        this.mParseError = 1;
        this.mArchiveSourcePath = apkFile.getAbsolutePath();
        int cookie = loadApkIntoAssetManager(assets, apkPath, flags);
        try {
            try {
                res = new Resources(assets, this.mMetrics, null);
            } catch (Throwable th) {
                th = th;
            }
        } catch (PackageParserException e) {
            throw e;
        } catch (Exception e2) {
            e = e2;
        }
        try {
            assets.setConfiguration(0, 0, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Build.VERSION.RESOURCES_SDK_INT);
            XmlResourceParser parser = assets.openXmlResourceParser(cookie, ANDROID_MANIFEST_FILENAME);
            String[] outError = new String[1];
            Package pkg = parseBaseApk(res, parser, flags, outError);
            if (pkg == null) {
                throw new PackageParserException(this.mParseError, apkPath + " (at " + parser.getPositionDescription() + "): " + outError[0]);
            }
            pkg.setVolumeUuid(volumeUuid);
            pkg.setApplicationVolumeUuid(volumeUuid);
            pkg.setBaseCodePath(apkPath);
            pkg.setSignatures(null);
            IoUtils.closeQuietly(parser);
            return pkg;
        } catch (PackageParserException e3) {
            throw e3;
        } catch (Exception e4) {
            e = e4;
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed to read manifest from " + apkPath, e);
        } catch (Throwable th2) {
            th = th2;
            IoUtils.closeQuietly((AutoCloseable) null);
            throw th;
        }
    }

    private void parseSplitApk(Package pkg, int splitIndex, AssetManager assets, int flags) throws Throwable {
        XmlResourceParser parser;
        Resources res;
        String apkPath = pkg.splitCodePaths[splitIndex];
        this.mParseError = 1;
        this.mArchiveSourcePath = apkPath;
        int cookie = loadApkIntoAssetManager(assets, apkPath, flags);
        try {
            try {
                res = new Resources(assets, this.mMetrics, null);
                try {
                    assets.setConfiguration(0, 0, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Build.VERSION.RESOURCES_SDK_INT);
                    parser = assets.openXmlResourceParser(cookie, ANDROID_MANIFEST_FILENAME);
                } catch (PackageParserException e) {
                    e = e;
                } catch (Exception e2) {
                    e = e2;
                } catch (Throwable th) {
                    th = th;
                    parser = null;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (PackageParserException e3) {
            e = e3;
        } catch (Exception e4) {
            e = e4;
        } catch (Throwable th3) {
            th = th3;
            parser = null;
        }
        try {
            String[] outError = new String[1];
            if (parseSplitApk(pkg, res, parser, flags, splitIndex, outError) == null) {
                throw new PackageParserException(this.mParseError, apkPath + " (at " + parser.getPositionDescription() + "): " + outError[0]);
            }
            IoUtils.closeQuietly(parser);
        } catch (PackageParserException e5) {
            e = e5;
            throw e;
        } catch (Exception e6) {
            e = e6;
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed to read manifest from " + apkPath, e);
        } catch (Throwable th4) {
            th = th4;
            IoUtils.closeQuietly(parser);
            throw th;
        }
    }

    private Package parseSplitApk(Package pkg, Resources res, XmlResourceParser parser, int flags, int splitIndex, String[] outError) throws XmlPullParserException, PackageParserException, IOException {
        parsePackageSplitNames(parser, parser);
        this.mParseInstrumentationArgs = null;
        this.mParseActivityArgs = null;
        this.mParseServiceArgs = null;
        this.mParseProviderArgs = null;
        boolean foundApp = false;
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if (!tagName.equals("application")) {
                    Slog.w(TAG, "Unknown element under <manifest>: " + parser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                } else if (foundApp) {
                    Slog.w(TAG, "<manifest> has more than one <application>");
                    XmlUtils.skipCurrentTag(parser);
                } else {
                    foundApp = true;
                    if (!parseSplitApplication(pkg, res, parser, flags, splitIndex, outError)) {
                        return null;
                    }
                }
            }
        }
    }

    public static int getApkSigningVersion(Package pkg) {
        try {
            if (ApkSignatureSchemeV2Verifier.hasSignature(pkg.baseCodePath)) {
                return 2;
            }
            return 1;
        } catch (IOException e) {
            return 0;
        }
    }

    public static void populateCertificates(Package pkg, Certificate[][] certificates) throws PackageParserException {
        pkg.mCertificates = null;
        pkg.mSignatures = null;
        pkg.mSigningKeys = null;
        pkg.mCertificates = certificates;
        try {
            pkg.mSignatures = convertToSignatures(certificates);
            pkg.mSigningKeys = new ArraySet<>(certificates.length);
            for (Certificate[] signerCerts : certificates) {
                Certificate signerCert = signerCerts[0];
                pkg.mSigningKeys.add(signerCert.getPublicKey());
            }
            int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
            for (int i = 0; i < childCount; i++) {
                Package childPkg = pkg.childPackages.get(i);
                childPkg.mCertificates = pkg.mCertificates;
                childPkg.mSignatures = pkg.mSignatures;
                childPkg.mSigningKeys = pkg.mSigningKeys;
            }
        } catch (CertificateEncodingException e) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "Failed to collect certificates from " + pkg.baseCodePath, e);
        }
    }

    public static void collectCertificates(Package pkg, int parseFlags) throws PackageParserException {
        collectCertificatesInternal(pkg, parseFlags);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            Package childPkg = pkg.childPackages.get(i);
            childPkg.mCertificates = pkg.mCertificates;
            childPkg.mSignatures = pkg.mSignatures;
            childPkg.mSigningKeys = pkg.mSigningKeys;
        }
    }

    private static void collectCertificatesInternal(Package pkg, int parseFlags) throws PackageParserException {
        pkg.mCertificates = null;
        pkg.mSignatures = null;
        pkg.mSigningKeys = null;
        Trace.traceBegin(1048576L, "collectCertificates");
        try {
            collectCertificates(pkg, new File(pkg.baseCodePath), parseFlags);
            if (!ArrayUtils.isEmpty(pkg.splitCodePaths)) {
                for (int i = 0; i < pkg.splitCodePaths.length; i++) {
                    collectCertificates(pkg, new File(pkg.splitCodePaths[i]), parseFlags);
                }
            }
        } finally {
            Trace.traceEnd(1048576L);
        }
    }

    private static void collectCertificates(Package pkg, File apkFile, int parseFlags) throws Throwable {
        StrictJarFile jarFile;
        String apkPath = apkFile.getAbsolutePath();
        boolean verified = false;
        Certificate[][] allSignersCerts = null;
        Signature[] signatures = null;
        try {
            try {
                Trace.traceBegin(1048576L, "verifyV2");
                allSignersCerts = ApkSignatureSchemeV2Verifier.verify(apkPath);
                signatures = convertToSignatures(allSignersCerts);
                verified = true;
                Trace.traceEnd(1048576L);
            } catch (Throwable th) {
                Trace.traceEnd(1048576L);
                throw th;
            }
        } catch (Exception e) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "Failed to collect certificates from " + apkPath + " using APK Signature Scheme v2", e);
        } catch (ApkSignatureSchemeV2Verifier.SignatureNotFoundException e2) {
            Trace.traceEnd(1048576L);
        }
        if (verified) {
            if (pkg.mCertificates == null) {
                pkg.mCertificates = allSignersCerts;
                pkg.mSignatures = signatures;
                pkg.mSigningKeys = new ArraySet<>(allSignersCerts.length);
                for (Certificate[] signerCerts : allSignersCerts) {
                    Certificate signerCert = signerCerts[0];
                    pkg.mSigningKeys.add(signerCert.getPublicKey());
                }
            } else if (!Signature.areExactMatch(pkg.mSignatures, signatures)) {
                throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES, apkPath + " has mismatched certificates");
            }
        }
        StrictJarFile strictJarFile = null;
        try {
            try {
                Trace.traceBegin(1048576L, "strictJarFileCtor");
                boolean signatureSchemeRollbackProtectionsEnforced = (parseFlags & 64) == 0;
                jarFile = new StrictJarFile(apkPath, !verified, signatureSchemeRollbackProtectionsEnforced);
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (IOException | RuntimeException e3) {
            e = e3;
        } catch (GeneralSecurityException e4) {
            e = e4;
        }
        try {
            try {
                Trace.traceEnd(1048576L);
                ZipEntry manifestEntry = jarFile.findEntry(ANDROID_MANIFEST_FILENAME);
                if (manifestEntry == null) {
                    throw new PackageParserException(-101, "Package " + apkPath + " has no manifest");
                }
                if (!verified) {
                    Trace.traceBegin(1048576L, "verifyV1");
                    List<ZipEntry> toVerify = new ArrayList<>();
                    toVerify.add(manifestEntry);
                    if ((parseFlags & 64) == 0) {
                        for (ZipEntry entry : jarFile) {
                            if (!entry.isDirectory()) {
                                String entryName = entry.getName();
                                if (!entryName.startsWith("META-INF/") && !entryName.equals(ANDROID_MANIFEST_FILENAME)) {
                                    toVerify.add(entry);
                                }
                            }
                        }
                    }
                    for (ZipEntry entry2 : toVerify) {
                        Certificate[][] entryCerts = loadCertificates(jarFile, entry2);
                        if (ArrayUtils.isEmpty(entryCerts)) {
                            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "Package " + apkPath + " has no certificates at entry " + entry2.getName());
                        }
                        Signature[] entrySignatures = convertToSignatures(entryCerts);
                        if (pkg.mCertificates == null) {
                            pkg.mCertificates = entryCerts;
                            pkg.mSignatures = entrySignatures;
                            pkg.mSigningKeys = new ArraySet<>();
                            for (Certificate[] certificateArr : entryCerts) {
                                pkg.mSigningKeys.add(certificateArr[0].getPublicKey());
                            }
                        } else if (!Signature.areExactMatch(pkg.mSignatures, entrySignatures)) {
                            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES, "Package " + apkPath + " has mismatched certificates at entry " + entry2.getName());
                        }
                    }
                    Trace.traceEnd(1048576L);
                    closeQuietly(jarFile);
                    return;
                }
                closeQuietly(jarFile);
            } catch (IOException | RuntimeException e5) {
                e = e5;
                throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "Failed to collect certificates from " + apkPath, e);
            }
        } catch (GeneralSecurityException e6) {
            e = e6;
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING, "Failed to collect certificates from " + apkPath, e);
        } catch (Throwable th3) {
            th = th3;
            strictJarFile = jarFile;
            closeQuietly(strictJarFile);
            throw th;
        }
    }

    private static Signature[] convertToSignatures(Certificate[][] certs) throws CertificateEncodingException {
        Signature[] res = new Signature[certs.length];
        for (int i = 0; i < certs.length; i++) {
            res[i] = new Signature(certs[i]);
        }
        return res;
    }

    public static ApkLite parseApkLite(File apkFile, int flags) throws Throwable {
        XmlResourceParser parser;
        AssetManager assets;
        Signature[] signatures;
        Certificate[][] certificateArr;
        String apkPath = apkFile.getAbsolutePath();
        try {
            try {
                assets = new AssetManager();
                try {
                    try {
                        assets.setConfiguration(0, 0, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Build.VERSION.RESOURCES_SDK_INT);
                        int cookie = assets.addAssetPath(apkPath);
                        if (cookie == 0) {
                            throw new PackageParserException(-100, "Failed to parse " + apkPath);
                        }
                        DisplayMetrics metrics = new DisplayMetrics();
                        metrics.setToDefaults();
                        Resources res = new Resources(assets, metrics, null);
                        XmlResourceParser parser2 = assets.openXmlResourceParser(cookie, ANDROID_MANIFEST_FILENAME);
                        if ((flags & 256) != 0) {
                            try {
                                Package tempPkg = new Package(null);
                                Trace.traceBegin(1048576L, "collectCertificates");
                                try {
                                    collectCertificates(tempPkg, apkFile, 0);
                                    Trace.traceEnd(1048576L);
                                    signatures = tempPkg.mSignatures;
                                    certificateArr = tempPkg.mCertificates;
                                } catch (Throwable th) {
                                    Trace.traceEnd(1048576L);
                                    throw th;
                                }
                            } catch (IOException | RuntimeException | XmlPullParserException e) {
                                e = e;
                                throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed to parse " + apkPath, e);
                            }
                        } else {
                            signatures = null;
                            certificateArr = null;
                        }
                        ApkLite apkLite = parseApkLite(apkPath, res, parser2, parser2, flags, signatures, certificateArr);
                        IoUtils.closeQuietly(parser2);
                        IoUtils.closeQuietly(assets);
                        return apkLite;
                    } catch (Throwable th2) {
                        th = th2;
                        parser = null;
                        IoUtils.closeQuietly(parser);
                        IoUtils.closeQuietly(assets);
                        throw th;
                    }
                } catch (IOException | RuntimeException | XmlPullParserException e2) {
                    e = e2;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        } catch (IOException | RuntimeException | XmlPullParserException e3) {
            e = e3;
        } catch (Throwable th4) {
            th = th4;
            parser = null;
            assets = null;
        }
    }

    private static String validateName(String name, boolean requireSeparator, boolean requireFilename) {
        int N = name.length();
        boolean hasSep = false;
        boolean front = true;
        for (int i = 0; i < N; i++) {
            char c = name.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                front = false;
            } else if (front || ((c < '0' || c > '9') && c != '_')) {
                if (c == '.') {
                    hasSep = true;
                    front = true;
                } else {
                    return "bad character '" + c + "'";
                }
            }
        }
        if (requireFilename && !FileUtils.isValidExtFilename(name)) {
            return "Invalid filename";
        }
        if (hasSep || !requireSeparator) {
            return null;
        }
        return "must have at least one '.' separator";
    }

    private static Pair<String, String> parsePackageSplitNames(XmlPullParser parser, AttributeSet attrs) throws XmlPullParserException, PackageParserException, IOException {
        int type;
        String error;
        do {
            type = parser.next();
            if (type == 2) {
                break;
            }
        } while (type != 1);
        if (type != 2) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "No start tag found");
        }
        if (!parser.getName().equals(TAG_MANIFEST)) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "No <manifest> tag");
        }
        String packageName = attrs.getAttributeValue(null, TAG_PACKAGE);
        if (!ZenModeConfig.SYSTEM_AUTHORITY.equals(packageName) && (error = validateName(packageName, true, true)) != null) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME, "Invalid manifest package: " + error);
        }
        String splitName = attrs.getAttributeValue(null, "split");
        if (splitName != null) {
            if (splitName.length() == 0) {
                splitName = null;
            } else {
                String error2 = validateName(splitName, false, false);
                if (error2 != null) {
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME, "Invalid manifest split: " + error2);
                }
            }
        }
        String strIntern = packageName.intern();
        if (splitName != null) {
            splitName = splitName.intern();
        }
        return Pair.create(strIntern, splitName);
    }

    private static ApkLite parseApkLite(String codePath, Resources res, XmlPullParser parser, AttributeSet attrs, int flags, Signature[] signatures, Certificate[][] certificates) throws XmlPullParserException, PackageParserException, IOException {
        VerifierInfo verifier;
        Pair<String, String> packageSplit = parsePackageSplitNames(parser, attrs);
        int installLocation = -1;
        int versionCode = 0;
        int revisionCode = 0;
        boolean coreApp = false;
        boolean multiArch = false;
        boolean use32bitAbi = false;
        boolean extractNativeLibs = true;
        for (int i = 0; i < attrs.getAttributeCount(); i++) {
            String attr = attrs.getAttributeName(i);
            if (attr.equals("installLocation")) {
                installLocation = attrs.getAttributeIntValue(i, -1);
            } else if (attr.equals("versionCode")) {
                versionCode = attrs.getAttributeIntValue(i, 0);
            } else if (attr.equals("revisionCode")) {
                revisionCode = attrs.getAttributeIntValue(i, 0);
            } else if (attr.equals("coreApp")) {
                coreApp = attrs.getAttributeBooleanValue(i, false);
            }
        }
        int searchDepth = parser.getDepth() + 1;
        List<VerifierInfo> verifiers = new ArrayList<>();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() < searchDepth)) {
                break;
            }
            if (type != 3 && type != 4) {
                if (parser.getDepth() == searchDepth && "package-verifier".equals(parser.getName()) && (verifier = parseVerifier(res, parser, attrs, flags)) != null) {
                    verifiers.add(verifier);
                }
                if (parser.getDepth() == searchDepth && "application".equals(parser.getName())) {
                    for (int i2 = 0; i2 < attrs.getAttributeCount(); i2++) {
                        String attr2 = attrs.getAttributeName(i2);
                        if ("multiArch".equals(attr2)) {
                            multiArch = attrs.getAttributeBooleanValue(i2, false);
                        }
                        if ("use32bitAbi".equals(attr2)) {
                            use32bitAbi = attrs.getAttributeBooleanValue(i2, false);
                        }
                        if ("extractNativeLibs".equals(attr2)) {
                            extractNativeLibs = attrs.getAttributeBooleanValue(i2, true);
                        }
                    }
                }
            }
        }
        return new ApkLite(codePath, (String) packageSplit.first, (String) packageSplit.second, versionCode, revisionCode, installLocation, verifiers, signatures, certificates, coreApp, multiArch, use32bitAbi, extractNativeLibs);
    }

    public static Signature stringToSignature(String str) {
        int N = str.length();
        byte[] sig = new byte[N];
        for (int i = 0; i < N; i++) {
            sig[i] = (byte) str.charAt(i);
        }
        return new Signature(sig);
    }

    private boolean parseBaseApkChild(Package parentPkg, Resources res, XmlResourceParser parser, int flags, String[] outError) throws XmlPullParserException, IOException {
        if (parentPkg.childPackages != null && parentPkg.childPackages.size() + 2 > 5) {
            outError[0] = "Maximum number of packages per APK is: 5";
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        String childPackageName = parser.getAttributeValue(null, TAG_PACKAGE);
        if (validateName(childPackageName, true, false) != null) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME;
            return false;
        }
        if (childPackageName.equals(parentPkg.packageName)) {
            String message = "Child package name cannot be equal to parent package name: " + parentPkg.packageName;
            Slog.w(TAG, message);
            outError[0] = message;
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        if (parentPkg.hasChildPackage(childPackageName)) {
            String message2 = "Duplicate child package:" + childPackageName;
            Slog.w(TAG, message2);
            outError[0] = message2;
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        Package childPkg = new Package(childPackageName);
        childPkg.mVersionCode = parentPkg.mVersionCode;
        childPkg.baseRevisionCode = parentPkg.baseRevisionCode;
        childPkg.mVersionName = parentPkg.mVersionName;
        childPkg.applicationInfo.targetSdkVersion = parentPkg.applicationInfo.targetSdkVersion;
        childPkg.applicationInfo.minSdkVersion = parentPkg.applicationInfo.minSdkVersion;
        Package childPkg2 = parseBaseApkCommon(childPkg, CHILD_PACKAGE_TAGS, res, parser, flags, outError);
        if (childPkg2 == null) {
            return false;
        }
        if (parentPkg.childPackages == null) {
            parentPkg.childPackages = new ArrayList<>();
        }
        parentPkg.childPackages.add(childPkg2);
        childPkg2.parentPackage = parentPkg;
        return true;
    }

    private Package parseBaseApk(Resources res, XmlResourceParser parser, int flags, String[] outError) throws XmlPullParserException, IOException {
        try {
            Pair<String, String> packageSplit = parsePackageSplitNames(parser, parser);
            String pkgName = (String) packageSplit.first;
            String splitName = (String) packageSplit.second;
            if (!TextUtils.isEmpty(splitName)) {
                outError[0] = "Expected base APK, but found split " + splitName;
                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME;
                return null;
            }
            Package pkg = new Package(pkgName);
            TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifest);
            int integer = sa.getInteger(1, 0);
            pkg.applicationInfo.versionCode = integer;
            pkg.mVersionCode = integer;
            pkg.baseRevisionCode = sa.getInteger(5, 0);
            pkg.mVersionName = sa.getNonConfigurationString(2, 0);
            if (pkg.mVersionName != null) {
                pkg.mVersionName = pkg.mVersionName.intern();
            }
            pkg.coreApp = parser.getAttributeBooleanValue(null, "coreApp", false);
            sa.recycle();
            return parseBaseApkCommon(pkg, null, res, parser, flags, outError);
        } catch (PackageParserException e) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME;
            return null;
        }
    }

    private Package parseBaseApkCommon(Package pkg, Set<String> acceptedTags, Resources res, XmlResourceParser parser, int flags, String[] outError) throws XmlPullParserException, IOException {
        this.mParseInstrumentationArgs = null;
        this.mParseActivityArgs = null;
        this.mParseServiceArgs = null;
        this.mParseProviderArgs = null;
        boolean foundApp = false;
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifest);
        String str = sa.getNonConfigurationString(0, 0);
        if (str != null && str.length() > 0) {
            String nameError = validateName(str, true, false);
            if (nameError != null && !ZenModeConfig.SYSTEM_AUTHORITY.equals(pkg.packageName)) {
                outError[0] = "<manifest> specifies bad sharedUserId name \"" + str + "\": " + nameError;
                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID;
                return null;
            }
            pkg.mSharedUserId = str.intern();
            pkg.mSharedUserLabel = sa.getResourceId(3, 0);
        }
        pkg.installLocation = sa.getInteger(4, -1);
        pkg.applicationInfo.installLocation = pkg.installLocation;
        if ((flags & 16) != 0) {
            pkg.applicationInfo.privateFlags |= 4;
        }
        if ((flags & 32) != 0) {
            pkg.applicationInfo.flags |= 262144;
        }
        if ((flags & 2048) != 0) {
            pkg.applicationInfo.privateFlags |= 512;
        }
        int supportsSmallScreens = 1;
        int supportsNormalScreens = 1;
        int supportsLargeScreens = 1;
        int supportsXLargeScreens = 1;
        int resizeable = 1;
        int anyDensity = 1;
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if (acceptedTags != null && !acceptedTags.contains(tagName)) {
                    Slog.w(TAG, "Skipping unsupported element under <manifest>: " + tagName + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals("application")) {
                    if (foundApp) {
                        Slog.w(TAG, "<manifest> has more than one <application>");
                        XmlUtils.skipCurrentTag(parser);
                    } else {
                        foundApp = true;
                        if (!parseBaseApplication(pkg, res, parser, flags, outError)) {
                            return null;
                        }
                    }
                } else if (tagName.equals(TAG_OVERLAY)) {
                    TypedArray sa2 = res.obtainAttributes(parser, R.styleable.AndroidManifestResourceOverlay);
                    pkg.mOverlayTarget = sa2.getString(1);
                    pkg.mOverlayPriority = sa2.getInt(0, -1);
                    sa2.recycle();
                    if (pkg.mOverlayTarget == null) {
                        outError[0] = "<overlay> does not specify a target package";
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return null;
                    }
                    if (pkg.mOverlayPriority < 0 || pkg.mOverlayPriority > 9999) {
                        break;
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals(TAG_KEY_SETS)) {
                    if (!parseKeySets(pkg, res, parser, outError)) {
                        return null;
                    }
                } else if (tagName.equals(TAG_PERMISSION_GROUP)) {
                    if (parsePermissionGroup(pkg, flags, res, parser, outError) == null) {
                        return null;
                    }
                } else if (tagName.equals("permission")) {
                    if (parsePermission(pkg, res, parser, outError) == null) {
                        return null;
                    }
                } else if (tagName.equals(TAG_PERMISSION_TREE)) {
                    if (parsePermissionTree(pkg, res, parser, outError) == null) {
                        return null;
                    }
                } else if (tagName.equals(TAG_USES_PERMISSION)) {
                    if (!parseUsesPermission(pkg, res, parser)) {
                        return null;
                    }
                } else if (tagName.equals(TAG_USES_PERMISSION_SDK_M) || tagName.equals(TAG_USES_PERMISSION_SDK_23)) {
                    if (!parseUsesPermission(pkg, res, parser)) {
                        return null;
                    }
                } else if (tagName.equals(TAG_USES_CONFIGURATION)) {
                    ConfigurationInfo cPref = new ConfigurationInfo();
                    TypedArray sa3 = res.obtainAttributes(parser, R.styleable.AndroidManifestUsesConfiguration);
                    cPref.reqTouchScreen = sa3.getInt(0, 0);
                    cPref.reqKeyboardType = sa3.getInt(1, 0);
                    if (sa3.getBoolean(2, false)) {
                        cPref.reqInputFeatures |= 1;
                    }
                    cPref.reqNavigation = sa3.getInt(3, 0);
                    if (sa3.getBoolean(4, false)) {
                        cPref.reqInputFeatures |= 2;
                    }
                    sa3.recycle();
                    pkg.configPreferences = ArrayUtils.add(pkg.configPreferences, cPref);
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals(TAG_USES_FEATURE)) {
                    FeatureInfo fi = parseUsesFeature(res, parser);
                    pkg.reqFeatures = ArrayUtils.add(pkg.reqFeatures, fi);
                    if (fi.name == null) {
                        ConfigurationInfo cPref2 = new ConfigurationInfo();
                        cPref2.reqGlEsVersion = fi.reqGlEsVersion;
                        pkg.configPreferences = ArrayUtils.add(pkg.configPreferences, cPref2);
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals(TAG_FEATURE_GROUP)) {
                    FeatureGroupInfo group = new FeatureGroupInfo();
                    ArrayList<FeatureInfo> features = null;
                    int innerDepth = parser.getDepth();
                    while (true) {
                        int type2 = parser.next();
                        if (type2 == 1 || (type2 == 3 && parser.getDepth() <= innerDepth)) {
                            break;
                        }
                        if (type2 != 3 && type2 != 4) {
                            String innerTagName = parser.getName();
                            if (innerTagName.equals(TAG_USES_FEATURE)) {
                                FeatureInfo featureInfo = parseUsesFeature(res, parser);
                                featureInfo.flags |= 1;
                                features = ArrayUtils.add(features, featureInfo);
                            } else {
                                Slog.w(TAG, "Unknown element under <feature-group>: " + innerTagName + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                            }
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                    if (features != null) {
                        group.features = new FeatureInfo[features.size()];
                        group.features = (FeatureInfo[]) features.toArray(group.features);
                    }
                    pkg.featureGroups = ArrayUtils.add(pkg.featureGroups, group);
                } else if (tagName.equals(TAG_USES_SDK)) {
                    if (SDK_VERSION > 0) {
                        TypedArray sa4 = res.obtainAttributes(parser, R.styleable.AndroidManifestUsesSdk);
                        int minVers = 1;
                        String minCode = null;
                        int targetVers = 0;
                        String targetCode = null;
                        TypedValue val = sa4.peekValue(0);
                        if (val != null) {
                            if (val.type != 3 || val.string == null) {
                                minVers = val.data;
                                targetVers = minVers;
                            } else {
                                minCode = val.string.toString();
                                targetCode = minCode;
                            }
                        }
                        TypedValue val2 = sa4.peekValue(1);
                        if (val2 != null) {
                            if (val2.type != 3 || val2.string == null) {
                                targetVers = val2.data;
                            } else {
                                targetCode = val2.string.toString();
                                if (minCode == null) {
                                    minCode = targetCode;
                                }
                            }
                        }
                        sa4.recycle();
                        if (minCode != null) {
                            boolean allowedCodename = false;
                            String[] strArr = SDK_CODENAMES;
                            int i = 0;
                            int length = strArr.length;
                            while (true) {
                                if (i >= length) {
                                    break;
                                }
                                String codename = strArr[i];
                                if (minCode.equals(codename)) {
                                    allowedCodename = true;
                                    break;
                                }
                                i++;
                            }
                            if (!allowedCodename) {
                                if (SDK_CODENAMES.length > 0) {
                                    outError[0] = "Requires development platform " + minCode + " (current platform is any of " + Arrays.toString(SDK_CODENAMES) + ")";
                                } else {
                                    outError[0] = "Requires development platform " + minCode + " but this is a release platform.";
                                }
                                this.mParseError = -12;
                                return null;
                            }
                            pkg.applicationInfo.minSdkVersion = 10000;
                        } else {
                            if (minVers > SDK_VERSION) {
                                outError[0] = "Requires newer sdk version #" + minVers + " (current version is #" + SDK_VERSION + ")";
                                this.mParseError = -12;
                                return null;
                            }
                            pkg.applicationInfo.minSdkVersion = minVers;
                        }
                        if (targetCode != null) {
                            boolean allowedCodename2 = false;
                            String[] strArr2 = SDK_CODENAMES;
                            int i2 = 0;
                            int length2 = strArr2.length;
                            while (true) {
                                if (i2 >= length2) {
                                    break;
                                }
                                String codename2 = strArr2[i2];
                                if (targetCode.equals(codename2)) {
                                    allowedCodename2 = true;
                                    break;
                                }
                                i2++;
                            }
                            if (!allowedCodename2) {
                                if (SDK_CODENAMES.length > 0) {
                                    outError[0] = "Requires development platform " + targetCode + " (current platform is any of " + Arrays.toString(SDK_CODENAMES) + ")";
                                } else {
                                    outError[0] = "Requires development platform " + targetCode + " but this is a release platform.";
                                }
                                this.mParseError = -12;
                                return null;
                            }
                            pkg.applicationInfo.targetSdkVersion = 10000;
                        } else {
                            pkg.applicationInfo.targetSdkVersion = targetVers;
                        }
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals(TAG_SUPPORT_SCREENS)) {
                    TypedArray sa5 = res.obtainAttributes(parser, R.styleable.AndroidManifestSupportsScreens);
                    pkg.applicationInfo.requiresSmallestWidthDp = sa5.getInteger(6, 0);
                    pkg.applicationInfo.compatibleWidthLimitDp = sa5.getInteger(7, 0);
                    pkg.applicationInfo.largestWidthLimitDp = sa5.getInteger(8, 0);
                    supportsSmallScreens = sa5.getInteger(1, supportsSmallScreens);
                    supportsNormalScreens = sa5.getInteger(2, supportsNormalScreens);
                    supportsLargeScreens = sa5.getInteger(3, supportsLargeScreens);
                    supportsXLargeScreens = sa5.getInteger(5, supportsXLargeScreens);
                    resizeable = sa5.getInteger(4, resizeable);
                    anyDensity = sa5.getInteger(0, anyDensity);
                    sa5.recycle();
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals(TAG_PROTECTED_BROADCAST)) {
                    TypedArray sa6 = res.obtainAttributes(parser, R.styleable.AndroidManifestProtectedBroadcast);
                    String name = sa6.getNonResourceString(0);
                    sa6.recycle();
                    if (name != null && (flags & 1) != 0) {
                        if (pkg.protectedBroadcasts == null) {
                            pkg.protectedBroadcasts = new ArrayList<>();
                        }
                        if (!pkg.protectedBroadcasts.contains(name)) {
                            pkg.protectedBroadcasts.add(name.intern());
                        }
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals(TAG_INSTRUMENTATION)) {
                    if (parseInstrumentation(pkg, res, parser, outError) == null) {
                        return null;
                    }
                } else if (tagName.equals(TAG_ORIGINAL_PACKAGE)) {
                    TypedArray sa7 = res.obtainAttributes(parser, R.styleable.AndroidManifestOriginalPackage);
                    String orig = sa7.getNonConfigurationString(0, 0);
                    if (!pkg.packageName.equals(orig)) {
                        if (pkg.mOriginalPackages == null) {
                            pkg.mOriginalPackages = new ArrayList<>();
                            pkg.mRealPackage = pkg.packageName;
                        }
                        pkg.mOriginalPackages.add(orig);
                    }
                    sa7.recycle();
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals(TAG_ADOPT_PERMISSIONS)) {
                    TypedArray sa8 = res.obtainAttributes(parser, R.styleable.AndroidManifestOriginalPackage);
                    String name2 = sa8.getNonConfigurationString(0, 0);
                    sa8.recycle();
                    if (name2 != null) {
                        if (pkg.mAdoptPermissions == null) {
                            pkg.mAdoptPermissions = new ArrayList<>();
                        }
                        pkg.mAdoptPermissions.add(name2);
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals(TAG_USES_GL_TEXTURE)) {
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals(TAG_COMPATIBLE_SCREENS)) {
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals(TAG_SUPPORTS_INPUT)) {
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals(TAG_EAT_COMMENT)) {
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals(TAG_PACKAGE)) {
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals(TAG_RESTRICT_UPDATE)) {
                    if ((flags & 64) != 0) {
                        TypedArray sa9 = res.obtainAttributes(parser, R.styleable.AndroidManifestRestrictUpdate);
                        String hash = sa9.getNonConfigurationString(0, 0);
                        sa9.recycle();
                        pkg.restrictUpdateHash = null;
                        if (hash != null) {
                            int hashLength = hash.length();
                            byte[] hashBytes = new byte[hashLength / 2];
                            for (int i3 = 0; i3 < hashLength; i3 += 2) {
                                hashBytes[i3 / 2] = (byte) ((Character.digit(hash.charAt(i3), 16) << 4) + Character.digit(hash.charAt(i3 + 1), 16));
                            }
                            pkg.restrictUpdateHash = hashBytes;
                        }
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else {
                    Slog.w(TAG, "Unknown element under <manifest>: " + parser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    private FeatureInfo parseUsesFeature(Resources res, AttributeSet attrs) {
        FeatureInfo fi = new FeatureInfo();
        TypedArray sa = res.obtainAttributes(attrs, R.styleable.AndroidManifestUsesFeature);
        fi.name = sa.getNonResourceString(0);
        fi.version = sa.getInt(3, 0);
        if (fi.name == null) {
            fi.reqGlEsVersion = sa.getInt(1, 0);
        }
        if (sa.getBoolean(2, true)) {
            fi.flags |= 1;
        }
        sa.recycle();
        return fi;
    }

    private boolean parseUsesPermission(Package pkg, Resources res, XmlResourceParser parser) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestUsesPermission);
        String name = sa.getNonResourceString(0);
        int maxSdkVersion = 0;
        TypedValue val = sa.peekValue(1);
        if (val != null && val.type >= 16 && val.type <= 31) {
            maxSdkVersion = val.data;
        }
        sa.recycle();
        if ((maxSdkVersion == 0 || maxSdkVersion >= Build.VERSION.RESOURCES_SDK_INT) && name != null) {
            int index = pkg.requestedPermissions.indexOf(name);
            if (index == -1) {
                pkg.requestedPermissions.add(name.intern());
            } else {
                Slog.w(TAG, "Ignoring duplicate uses-permissions/uses-permissions-sdk-m: " + name + " in package: " + pkg.packageName + " at: " + parser.getPositionDescription());
            }
        }
        XmlUtils.skipCurrentTag(parser);
        return true;
    }

    private static String buildClassName(String pkg, CharSequence clsSeq, String[] outError) {
        if (clsSeq == null || clsSeq.length() <= 0) {
            outError[0] = "Empty class name in package " + pkg;
            return null;
        }
        String cls = clsSeq.toString();
        char c = cls.charAt(0);
        if (c == '.') {
            return (pkg + cls).intern();
        }
        if (cls.indexOf(46) < 0) {
            return (pkg + '.' + cls).intern();
        }
        if (c >= 'a' && c <= 'z') {
            return cls.intern();
        }
        outError[0] = "Bad class name " + cls + " in package " + pkg;
        return null;
    }

    private static String buildCompoundName(String pkg, CharSequence procSeq, String type, String[] outError) {
        String proc = procSeq.toString();
        char c = proc.charAt(0);
        if (pkg != null && c == ':') {
            if (proc.length() < 2) {
                outError[0] = "Bad " + type + " name " + proc + " in package " + pkg + ": must be at least two characters";
                return null;
            }
            String subName = proc.substring(1);
            String nameError = validateName(subName, false, false);
            if (nameError != null) {
                outError[0] = "Invalid " + type + " name " + proc + " in package " + pkg + ": " + nameError;
                return null;
            }
            return (pkg + proc).intern();
        }
        String nameError2 = validateName(proc, true, false);
        if (nameError2 != null && !"system".equals(proc)) {
            outError[0] = "Invalid " + type + " name " + proc + " in package " + pkg + ": " + nameError2;
            return null;
        }
        return proc.intern();
    }

    private static String buildProcessName(String pkg, String defProc, CharSequence procSeq, int flags, String[] separateProcesses, String[] outError) {
        if ((flags & 8) != 0 && !"system".equals(procSeq)) {
            return defProc != null ? defProc : pkg;
        }
        if (separateProcesses != null) {
            for (int i = separateProcesses.length - 1; i >= 0; i--) {
                String sp = separateProcesses[i];
                if (sp.equals(pkg) || sp.equals(defProc) || sp.equals(procSeq)) {
                    return pkg;
                }
            }
        }
        if (procSeq == null || procSeq.length() <= 0) {
            return defProc;
        }
        return buildCompoundName(pkg, procSeq, "process", outError);
    }

    private static String buildTaskAffinityName(String pkg, String defProc, CharSequence procSeq, String[] outError) {
        if (procSeq == null) {
            return defProc;
        }
        if (procSeq.length() <= 0) {
            return null;
        }
        return buildCompoundName(pkg, procSeq, "taskAffinity", outError);
    }

    private boolean parseKeySets(Package owner, Resources res, XmlResourceParser parser, String[] outError) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        int currentKeySetDepth = -1;
        String currentKeySet = null;
        ArrayMap<String, PublicKey> publicKeys = new ArrayMap<>();
        ArraySet<String> upgradeKeySets = new ArraySet<>();
        ArrayMap<String, ArraySet<String>> definedKeySets = new ArrayMap<>();
        ArraySet<String> improperKeySets = new ArraySet<>();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            if (type != 3) {
                String tagName = parser.getName();
                if (tagName.equals("key-set")) {
                    if (currentKeySet != null) {
                        outError[0] = "Improperly nested 'key-set' tag at " + parser.getPositionDescription();
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestKeySet);
                    String keysetName = sa.getNonResourceString(0);
                    definedKeySets.put(keysetName, new ArraySet<>());
                    currentKeySet = keysetName;
                    currentKeySetDepth = parser.getDepth();
                    sa.recycle();
                } else if (tagName.equals("public-key")) {
                    if (currentKeySet == null) {
                        outError[0] = "Improperly nested 'key-set' tag at " + parser.getPositionDescription();
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    TypedArray sa2 = res.obtainAttributes(parser, R.styleable.AndroidManifestPublicKey);
                    String publicKeyName = sa2.getNonResourceString(0);
                    String encodedKey = sa2.getNonResourceString(1);
                    if (encodedKey == null && publicKeys.get(publicKeyName) == null) {
                        outError[0] = "'public-key' " + publicKeyName + " must define a public-key value on first use at " + parser.getPositionDescription();
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        sa2.recycle();
                        return false;
                    }
                    if (encodedKey != null) {
                        PublicKey currentKey = parsePublicKey(encodedKey);
                        if (currentKey == null) {
                            Slog.w(TAG, "No recognized valid key in 'public-key' tag at " + parser.getPositionDescription() + " key-set " + currentKeySet + " will not be added to the package's defined key-sets.");
                            sa2.recycle();
                            improperKeySets.add(currentKeySet);
                            XmlUtils.skipCurrentTag(parser);
                        } else {
                            if (publicKeys.get(publicKeyName) != null && !publicKeys.get(publicKeyName).equals(currentKey)) {
                                outError[0] = "Value of 'public-key' " + publicKeyName + " conflicts with previously defined value at " + parser.getPositionDescription();
                                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                sa2.recycle();
                                return false;
                            }
                            publicKeys.put(publicKeyName, currentKey);
                        }
                    }
                    definedKeySets.get(currentKeySet).add(publicKeyName);
                    sa2.recycle();
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals("upgrade-key-set")) {
                    TypedArray sa3 = res.obtainAttributes(parser, R.styleable.AndroidManifestUpgradeKeySet);
                    String name = sa3.getNonResourceString(0);
                    upgradeKeySets.add(name);
                    sa3.recycle();
                    XmlUtils.skipCurrentTag(parser);
                } else {
                    Slog.w(TAG, "Unknown element under <key-sets>: " + parser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                }
            } else if (parser.getDepth() == currentKeySetDepth) {
                currentKeySet = null;
                currentKeySetDepth = -1;
            }
        }
    }

    private PermissionGroup parsePermissionGroup(Package owner, int flags, Resources res, XmlResourceParser parser, String[] outError) throws XmlPullParserException, IOException {
        PermissionGroup perm = new PermissionGroup(owner);
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestPermissionGroup);
        if (!parsePackageItemInfo(owner, perm.info, outError, "<permission-group>", sa, 2, 0, 1, 5, 7)) {
            sa.recycle();
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        perm.info.descriptionRes = sa.getResourceId(4, 0);
        perm.info.flags = sa.getInt(6, 0);
        perm.info.priority = sa.getInt(3, 0);
        if (perm.info.priority > 0 && (flags & 1) == 0) {
            perm.info.priority = 0;
        }
        sa.recycle();
        if (!parseAllMetaData(res, parser, "<permission-group>", perm, outError)) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        owner.permissionGroups.add(perm);
        return perm;
    }

    private Permission parsePermission(Package owner, Resources res, XmlResourceParser parser, String[] outError) throws XmlPullParserException, IOException {
        Permission perm = new Permission(owner);
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestPermission);
        if (!parsePackageItemInfo(owner, perm.info, outError, "<permission>", sa, 2, 0, 1, 6, 8)) {
            sa.recycle();
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        perm.info.group = sa.getNonResourceString(4);
        if (perm.info.group != null) {
            perm.info.group = perm.info.group.intern();
        }
        perm.info.descriptionRes = sa.getResourceId(5, 0);
        perm.info.protectionLevel = sa.getInt(3, 0);
        perm.info.flags = sa.getInt(7, 0);
        sa.recycle();
        if (perm.info.protectionLevel == -1) {
            outError[0] = "<permission> does not specify protectionLevel";
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        perm.info.protectionLevel = PermissionInfo.fixProtectionLevel(perm.info.protectionLevel);
        if ((perm.info.protectionLevel & PermissionInfo.PROTECTION_MASK_FLAGS) != 0 && (perm.info.protectionLevel & 15) != 2) {
            outError[0] = "<permission>  protectionLevel specifies a flag but is not based on signature type";
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        if (!parseAllMetaData(res, parser, "<permission>", perm, outError)) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        owner.permissions.add(perm);
        return perm;
    }

    private Permission parsePermissionTree(Package owner, Resources res, XmlResourceParser parser, String[] outError) throws XmlPullParserException, IOException {
        Permission perm = new Permission(owner);
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestPermissionTree);
        if (!parsePackageItemInfo(owner, perm.info, outError, "<permission-tree>", sa, 2, 0, 1, 3, 4)) {
            sa.recycle();
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        sa.recycle();
        int index = perm.info.name.indexOf(46);
        if (index > 0) {
            index = perm.info.name.indexOf(46, index + 1);
        }
        if (index < 0) {
            outError[0] = "<permission-tree> name has less than three segments: " + perm.info.name;
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        perm.info.descriptionRes = 0;
        perm.info.protectionLevel = 0;
        perm.tree = true;
        if (!parseAllMetaData(res, parser, "<permission-tree>", perm, outError)) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        owner.permissions.add(perm);
        return perm;
    }

    private Instrumentation parseInstrumentation(Package owner, Resources res, XmlResourceParser parser, String[] outError) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestInstrumentation);
        if (this.mParseInstrumentationArgs == null) {
            this.mParseInstrumentationArgs = new ParsePackageItemArgs(owner, outError, 2, 0, 1, 6, 7);
            this.mParseInstrumentationArgs.tag = "<instrumentation>";
        }
        this.mParseInstrumentationArgs.sa = sa;
        Instrumentation a = new Instrumentation(this.mParseInstrumentationArgs, new InstrumentationInfo());
        if (outError[0] != null) {
            sa.recycle();
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        String str = sa.getNonResourceString(3);
        a.info.targetPackage = str != null ? str.intern() : null;
        a.info.handleProfiling = sa.getBoolean(4, false);
        a.info.functionalTest = sa.getBoolean(5, false);
        sa.recycle();
        if (a.info.targetPackage == null) {
            outError[0] = "<instrumentation> does not specify targetPackage";
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        if (!parseAllMetaData(res, parser, "<instrumentation>", a, outError)) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        owner.instrumentation.add(a);
        return a;
    }

    private boolean parseBaseApplication(Package owner, Resources res, XmlResourceParser parser, int flags, String[] outError) throws XmlPullParserException, BlockGuard.BlockGuardPolicyException, IOException {
        ApplicationInfo ai = owner.applicationInfo;
        String pkgName = owner.applicationInfo.packageName;
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestApplication);
        String name = sa.getNonConfigurationString(3, 0);
        if (name != null) {
            ai.className = buildClassName(pkgName, name, outError);
            if (ai.className == null) {
                sa.recycle();
                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                return false;
            }
        }
        String manageSpaceActivity = sa.getNonConfigurationString(4, 1024);
        if (manageSpaceActivity != null) {
            ai.manageSpaceActivityName = buildClassName(pkgName, manageSpaceActivity, outError);
        }
        boolean allowBackup = sa.getBoolean(17, true);
        if (allowBackup) {
            ai.flags |= 32768;
            String backupAgent = sa.getNonConfigurationString(16, 1024);
            if (backupAgent != null) {
                ai.backupAgentName = buildClassName(pkgName, backupAgent, outError);
                if (sa.getBoolean(18, true)) {
                    ai.flags |= 65536;
                }
                if (sa.getBoolean(21, false)) {
                    ai.flags |= 131072;
                }
                if (sa.getBoolean(32, false)) {
                    ai.flags |= 67108864;
                }
                if (sa.getBoolean(40, false)) {
                    ai.privateFlags |= 4096;
                }
            }
            TypedValue v = sa.peekValue(35);
            if (v != null) {
                int i = v.resourceId;
                ai.fullBackupContent = i;
                if (i == 0) {
                    ai.fullBackupContent = v.data == 0 ? -1 : 0;
                }
            }
        }
        TypedValue v2 = sa.peekValue(1);
        if (v2 != null) {
            int i2 = v2.resourceId;
            ai.labelRes = i2;
            if (i2 == 0) {
                ai.nonLocalizedLabel = v2.coerceToString();
            }
        }
        ai.icon = sa.getResourceId(2, 0);
        ai.logo = sa.getResourceId(22, 0);
        ai.banner = sa.getResourceId(30, 0);
        ai.theme = sa.getResourceId(0, 0);
        ai.descriptionRes = sa.getResourceId(13, 0);
        if ((flags & 1) != 0 && sa.getBoolean(8, false)) {
            ai.flags |= 8;
        }
        if (sa.getBoolean(27, false)) {
            owner.mRequiredForAllUsers = true;
        }
        String restrictedAccountType = sa.getString(28);
        if (restrictedAccountType != null && restrictedAccountType.length() > 0) {
            owner.mRestrictedAccountType = restrictedAccountType;
        }
        String requiredAccountType = sa.getString(29);
        if (requiredAccountType != null && requiredAccountType.length() > 0) {
            owner.mRequiredAccountType = requiredAccountType;
        }
        if (sa.getBoolean(10, false)) {
            ai.flags |= 2;
        }
        if (sa.getBoolean(20, false)) {
            ai.flags |= 16384;
        }
        owner.baseHardwareAccelerated = sa.getBoolean(23, owner.applicationInfo.targetSdkVersion >= 14);
        if (owner.baseHardwareAccelerated) {
            ai.flags |= 536870912;
        }
        if (sa.getBoolean(7, true)) {
            ai.flags |= 4;
        }
        if (sa.getBoolean(14, false)) {
            ai.flags |= 32;
        }
        if (sa.getBoolean(5, true)) {
            ai.flags |= 64;
        }
        if (owner.parentPackage == null && sa.getBoolean(15, false)) {
            ai.flags |= 256;
        }
        if (sa.getBoolean(24, false)) {
            ai.flags |= 1048576;
        }
        if (sa.getBoolean(36, true)) {
            ai.flags |= 134217728;
        }
        if (sa.getBoolean(26, false)) {
            ai.flags |= 4194304;
        }
        if (sa.getBoolean(33, false)) {
            ai.flags |= Integer.MIN_VALUE;
        }
        if (sa.getBoolean(34, true)) {
            ai.flags |= 268435456;
        }
        if (sa.getBoolean(38, false)) {
            ai.privateFlags |= 32;
        }
        if (sa.getBoolean(39, false)) {
            ai.privateFlags |= 64;
        }
        if (sa.getBoolean(37, owner.applicationInfo.targetSdkVersion >= 24)) {
            ai.privateFlags |= 2048;
        }
        ai.networkSecurityConfigRes = sa.getResourceId(41, 0);
        String str = sa.getNonConfigurationString(6, 0);
        ai.permission = (str == null || str.length() <= 0) ? null : str.intern();
        ai.taskAffinity = buildTaskAffinityName(ai.packageName, ai.packageName, owner.applicationInfo.targetSdkVersion >= 8 ? sa.getNonConfigurationString(12, 1024) : sa.getNonResourceString(12), outError);
        if (outError[0] == null) {
            CharSequence pname = owner.applicationInfo.targetSdkVersion >= 8 ? sa.getNonConfigurationString(11, 1024) : sa.getNonResourceString(11);
            ai.processName = buildProcessName(ai.packageName, null, pname, flags, this.mSeparateProcesses, outError);
            ai.enabled = sa.getBoolean(9, true);
            if (sa.getBoolean(31, false)) {
                ai.flags |= 33554432;
            }
        }
        ai.uiOptions = sa.getInt(25, 0);
        sa.recycle();
        if (outError[0] != null) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        int innerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= innerDepth)) {
                break;
            }
            if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if (tagName.equals(Context.ACTIVITY_SERVICE)) {
                    Activity a = parseActivity(owner, res, parser, flags, outError, false, owner.baseHardwareAccelerated);
                    if (a == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    owner.activities.add(a);
                } else if (tagName.equals("receiver")) {
                    Activity a2 = parseActivity(owner, res, parser, flags, outError, true, false);
                    if (a2 == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    owner.receivers.add(a2);
                } else if (tagName.equals("service")) {
                    Service s = parseService(owner, res, parser, flags, outError);
                    if (s == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    owner.services.add(s);
                } else if (tagName.equals("provider")) {
                    Provider p = parseProvider(owner, res, parser, flags, outError);
                    if (p == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    owner.providers.add(p);
                } else if (tagName.equals("activity-alias")) {
                    Activity a3 = parseActivityAlias(owner, res, parser, flags, outError);
                    if (a3 == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    owner.activities.add(a3);
                } else if (parser.getName().equals("meta-data")) {
                    Bundle metaData = parseMetaData(res, parser, owner.mAppMetaData, outError);
                    owner.mAppMetaData = metaData;
                    if (metaData == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                } else if (tagName.equals("library")) {
                    TypedArray sa2 = res.obtainAttributes(parser, R.styleable.AndroidManifestLibrary);
                    String lname = sa2.getNonResourceString(0);
                    sa2.recycle();
                    if (lname != null) {
                        String lname2 = lname.intern();
                        if (!ArrayUtils.contains(owner.libraryNames, lname2)) {
                            owner.libraryNames = ArrayUtils.add(owner.libraryNames, lname2);
                        }
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals("uses-library")) {
                    TypedArray sa3 = res.obtainAttributes(parser, R.styleable.AndroidManifestUsesLibrary);
                    String lname3 = sa3.getNonResourceString(0);
                    boolean req = sa3.getBoolean(1, true);
                    sa3.recycle();
                    if (lname3 != null) {
                        String lname4 = lname3.intern();
                        if (req) {
                            owner.usesLibraries = ArrayUtils.add(owner.usesLibraries, lname4);
                        } else {
                            owner.usesOptionalLibraries = ArrayUtils.add(owner.usesOptionalLibraries, lname4);
                        }
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals("uses-package")) {
                    XmlUtils.skipCurrentTag(parser);
                } else {
                    Slog.w(TAG, "Unknown element under <application>: " + tagName + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    private static void modifySharedLibrariesForBackwardCompatibility(Package owner) {
        owner.usesLibraries = ArrayUtils.remove(owner.usesLibraries, "org.apache.http.legacy");
        owner.usesOptionalLibraries = ArrayUtils.remove(owner.usesOptionalLibraries, "org.apache.http.legacy");
    }

    private static boolean hasDomainURLs(Package pkg) {
        if (pkg == null || pkg.activities == null) {
            return false;
        }
        ArrayList<Activity> activities = pkg.activities;
        int countActivities = activities.size();
        for (int n = 0; n < countActivities; n++) {
            Activity activity = activities.get(n);
            ArrayList<II> arrayList = activity.intents;
            if (arrayList != 0) {
                int countFilters = arrayList.size();
                for (int m = 0; m < countFilters; m++) {
                    ActivityIntentInfo aii = (ActivityIntentInfo) arrayList.get(m);
                    if (aii.hasAction("android.intent.action.VIEW") && aii.hasAction("android.intent.action.VIEW") && (aii.hasDataScheme(IntentFilter.SCHEME_HTTP) || aii.hasDataScheme(IntentFilter.SCHEME_HTTPS))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean parseSplitApplication(Package owner, Resources res, XmlResourceParser parser, int flags, int splitIndex, String[] outError) throws XmlPullParserException, BlockGuard.BlockGuardPolicyException, IOException {
        if (res.obtainAttributes(parser, R.styleable.AndroidManifestApplication).getBoolean(7, true)) {
            int[] iArr = owner.splitFlags;
            iArr[splitIndex] = iArr[splitIndex] | 4;
        }
        int innerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return true;
            }
            if (type == 3 && parser.getDepth() <= innerDepth) {
                return true;
            }
            if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if (tagName.equals(Context.ACTIVITY_SERVICE)) {
                    Activity a = parseActivity(owner, res, parser, flags, outError, false, owner.baseHardwareAccelerated);
                    if (a == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    owner.activities.add(a);
                } else if (tagName.equals("receiver")) {
                    Activity a2 = parseActivity(owner, res, parser, flags, outError, true, false);
                    if (a2 == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    owner.receivers.add(a2);
                } else if (tagName.equals("service")) {
                    Service s = parseService(owner, res, parser, flags, outError);
                    if (s == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    owner.services.add(s);
                } else if (tagName.equals("provider")) {
                    Provider p = parseProvider(owner, res, parser, flags, outError);
                    if (p == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    owner.providers.add(p);
                } else if (tagName.equals("activity-alias")) {
                    Activity a3 = parseActivityAlias(owner, res, parser, flags, outError);
                    if (a3 == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    owner.activities.add(a3);
                } else if (parser.getName().equals("meta-data")) {
                    Bundle metaData = parseMetaData(res, parser, owner.mAppMetaData, outError);
                    owner.mAppMetaData = metaData;
                    if (metaData == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                } else if (tagName.equals("uses-library")) {
                    TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestUsesLibrary);
                    String lname = sa.getNonResourceString(0);
                    boolean req = sa.getBoolean(1, true);
                    sa.recycle();
                    if (lname != null) {
                        String lname2 = lname.intern();
                        if (req) {
                            owner.usesLibraries = ArrayUtils.add(owner.usesLibraries, lname2);
                            owner.usesOptionalLibraries = ArrayUtils.remove(owner.usesOptionalLibraries, lname2);
                        } else if (!ArrayUtils.contains(owner.usesLibraries, lname2)) {
                            owner.usesOptionalLibraries = ArrayUtils.add(owner.usesOptionalLibraries, lname2);
                        }
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals("uses-package")) {
                    XmlUtils.skipCurrentTag(parser);
                } else {
                    Slog.w(TAG, "Unknown element under <application>: " + tagName + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    private boolean parsePackageItemInfo(Package owner, PackageItemInfo outInfo, String[] outError, String tag, TypedArray sa, int nameRes, int labelRes, int iconRes, int logoRes, int bannerRes) {
        String name = sa.getNonConfigurationString(nameRes, 0);
        if (name == null) {
            outError[0] = tag + " does not specify android:name";
            return false;
        }
        outInfo.name = buildClassName(owner.applicationInfo.packageName, name, outError);
        if (outInfo.name == null) {
            return false;
        }
        int iconVal = sa.getResourceId(iconRes, 0);
        if (iconVal != 0) {
            outInfo.icon = iconVal;
            outInfo.nonLocalizedLabel = null;
        }
        int logoVal = sa.getResourceId(logoRes, 0);
        if (logoVal != 0) {
            outInfo.logo = logoVal;
        }
        int bannerVal = sa.getResourceId(bannerRes, 0);
        if (bannerVal != 0) {
            outInfo.banner = bannerVal;
        }
        TypedValue v = sa.peekValue(labelRes);
        if (v != null) {
            int i = v.resourceId;
            outInfo.labelRes = i;
            if (i == 0) {
                outInfo.nonLocalizedLabel = v.coerceToString();
            }
        }
        outInfo.packageName = owner.packageName;
        return true;
    }

    private Activity parseActivity(Package r28, Resources resources, XmlResourceParser xmlResourceParser, int i, String[] strArr, boolean z, boolean z2) throws XmlPullParserException, BlockGuard.BlockGuardPolicyException, IOException {
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestActivity);
        if (this.mParseActivityArgs == null) {
            this.mParseActivityArgs = new ParseComponentArgs(r28, strArr, 3, 1, 2, 23, 30, this.mSeparateProcesses, 7, 17, 5);
        }
        this.mParseActivityArgs.tag = z ? "<receiver>" : "<activity>";
        this.mParseActivityArgs.sa = typedArrayObtainAttributes;
        this.mParseActivityArgs.flags = i;
        Activity activity = new Activity(this.mParseActivityArgs, new ActivityInfo());
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
        String nonConfigurationString2 = typedArrayObtainAttributes.getNonConfigurationString(4, 0);
        if (nonConfigurationString2 == null) {
            activity.info.permission = r28.applicationInfo.permission;
        } else {
            activity.info.permission = nonConfigurationString2.length() > 0 ? nonConfigurationString2.toString().intern() : null;
        }
        activity.info.taskAffinity = buildTaskAffinityName(r28.applicationInfo.packageName, r28.applicationInfo.taskAffinity, typedArrayObtainAttributes.getNonConfigurationString(8, 1024), strArr);
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
            activity.info.flags |= 8;
        }
        if (typedArrayObtainAttributes.getBoolean(12, false)) {
            activity.info.flags |= 16;
        }
        if (typedArrayObtainAttributes.getBoolean(13, false)) {
            activity.info.flags |= 32;
        }
        if (typedArrayObtainAttributes.getBoolean(19, (r28.applicationInfo.flags & 32) != 0)) {
            activity.info.flags |= 64;
        }
        if (typedArrayObtainAttributes.getBoolean(22, false)) {
            activity.info.flags |= 256;
        }
        if (typedArrayObtainAttributes.getBoolean(29, false) || typedArrayObtainAttributes.getBoolean(39, false)) {
            activity.info.flags |= 1024;
        }
        if (typedArrayObtainAttributes.getBoolean(24, false)) {
            activity.info.flags |= 2048;
        }
        if (typedArrayObtainAttributes.getBoolean(44, false)) {
            activity.info.flags |= 536870912;
        }
        if (z) {
            activity.info.launchMode = 0;
            activity.info.configChanges = 0;
            if (typedArrayObtainAttributes.getBoolean(28, false)) {
                activity.info.flags |= 1073741824;
                if (activity.info.exported && (i & 128) == 0) {
                    Slog.w(TAG, "Activity exported request ignored due to singleUser: " + activity.className + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                    activity.info.exported = false;
                    zHasValue = true;
                }
            }
            ActivityInfo activityInfo = activity.info;
            boolean z3 = typedArrayObtainAttributes.getBoolean(42, false);
            activity.info.directBootAware = z3;
            activityInfo.encryptionAware = z3;
        } else {
            if (typedArrayObtainAttributes.getBoolean(25, z2)) {
                activity.info.flags |= 512;
            }
            activity.info.launchMode = typedArrayObtainAttributes.getInt(14, 0);
            activity.info.documentLaunchMode = typedArrayObtainAttributes.getInt(33, 0);
            activity.info.maxRecents = typedArrayObtainAttributes.getInt(34, ActivityManager.getDefaultAppRecentsLimitStatic());
            activity.info.configChanges = typedArrayObtainAttributes.getInt(16, 0);
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
            activity.info.resizeMode = 0;
            boolean z4 = (r28.applicationInfo.privateFlags & 2048) != 0;
            boolean zHasValue2 = typedArrayObtainAttributes.hasValue(40);
            if (typedArrayObtainAttributes.getBoolean(40, z4)) {
                if (typedArrayObtainAttributes.getBoolean(41, false)) {
                    activity.info.resizeMode = 3;
                } else {
                    activity.info.resizeMode = 2;
                }
            } else if (r28.applicationInfo.targetSdkVersion >= 24 || zHasValue2) {
                activity.info.resizeMode = 0;
            } else if (!activity.info.isFixedOrientation() && (activity.info.flags & 2048) == 0) {
                activity.info.resizeMode = 4;
            }
            if (typedArrayObtainAttributes.getBoolean(45, false)) {
                activity.info.flags |= 262144;
            }
            activity.info.lockTaskLaunchMode = typedArrayObtainAttributes.getInt(38, 0);
            ActivityInfo activityInfo2 = activity.info;
            boolean z5 = typedArrayObtainAttributes.getBoolean(42, false);
            activity.info.directBootAware = z5;
            activityInfo2.encryptionAware = z5;
            activity.info.requestedVrComponent = typedArrayObtainAttributes.getString(43);
        }
        if (activity.info.directBootAware) {
            r28.applicationInfo.privateFlags |= 256;
        }
        typedArrayObtainAttributes.recycle();
        if (z && (r28.applicationInfo.privateFlags & 2) != 0 && activity.info.processName == r28.packageName) {
            strArr[0] = "Heavy-weight applications can not have receivers in main process";
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
            if (next != 3 && next != 4) {
                if (xmlResourceParser.getName().equals("intent-filter")) {
                    ActivityIntentInfo activityIntentInfo = new ActivityIntentInfo(activity);
                    if (!parseIntent(resources, xmlResourceParser, true, true, activityIntentInfo, strArr)) {
                        return null;
                    }
                    if (activityIntentInfo.countActions() == 0) {
                        Slog.w(TAG, "No actions in intent filter at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                    } else {
                        activity.intents.add(activityIntentInfo);
                    }
                } else if (!z && xmlResourceParser.getName().equals("preferred")) {
                    ActivityIntentInfo activityIntentInfo2 = new ActivityIntentInfo(activity);
                    if (!parseIntent(resources, xmlResourceParser, false, false, activityIntentInfo2, strArr)) {
                        return null;
                    }
                    if (activityIntentInfo2.countActions() == 0) {
                        Slog.w(TAG, "No actions in preferred at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                    } else {
                        if (r28.preferredActivityFilters == null) {
                            r28.preferredActivityFilters = new ArrayList<>();
                        }
                        r28.preferredActivityFilters.add(activityIntentInfo2);
                    }
                } else if (xmlResourceParser.getName().equals("meta-data")) {
                    Bundle metaData = parseMetaData(resources, xmlResourceParser, activity.metaData, strArr);
                    activity.metaData = metaData;
                    if (metaData == null) {
                        return null;
                    }
                } else if (z || !xmlResourceParser.getName().equals(TtmlUtils.TAG_LAYOUT)) {
                    Slog.w(TAG, "Problem in package " + this.mArchiveSourcePath + ":");
                    if (z) {
                        Slog.w(TAG, "Unknown element under <receiver>: " + xmlResourceParser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                    } else {
                        Slog.w(TAG, "Unknown element under <activity>: " + xmlResourceParser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                    }
                    XmlUtils.skipCurrentTag(xmlResourceParser);
                } else {
                    parseLayout(resources, xmlResourceParser, activity);
                }
            }
        }
    }

    private void parseLayout(Resources res, AttributeSet attrs, Activity a) throws BlockGuard.BlockGuardPolicyException {
        TypedArray sw = res.obtainAttributes(attrs, R.styleable.AndroidManifestLayout);
        int width = -1;
        float widthFraction = -1.0f;
        int height = -1;
        float heightFraction = -1.0f;
        int widthType = sw.getType(3);
        if (widthType == 6) {
            widthFraction = sw.getFraction(3, 1, 1, -1.0f);
        } else if (widthType == 5) {
            width = sw.getDimensionPixelSize(3, -1);
        }
        int heightType = sw.getType(4);
        if (heightType == 6) {
            heightFraction = sw.getFraction(4, 1, 1, -1.0f);
        } else if (heightType == 5) {
            height = sw.getDimensionPixelSize(4, -1);
        }
        int gravity = sw.getInt(0, 17);
        int minWidth = sw.getDimensionPixelSize(1, -1);
        int minHeight = sw.getDimensionPixelSize(2, -1);
        sw.recycle();
        a.info.windowLayout = new ActivityInfo.WindowLayout(width, widthFraction, height, heightFraction, gravity, minWidth, minHeight);
    }

    private Activity parseActivityAlias(Package r31, Resources resources, XmlResourceParser xmlResourceParser, int i, String[] strArr) throws XmlPullParserException, IOException {
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestActivityAlias);
        String nonConfigurationString = typedArrayObtainAttributes.getNonConfigurationString(7, 1024);
        if (nonConfigurationString == null) {
            strArr[0] = "<activity-alias> does not specify android:targetActivity";
            typedArrayObtainAttributes.recycle();
            return null;
        }
        String strBuildClassName = buildClassName(r31.applicationInfo.packageName, nonConfigurationString, strArr);
        if (strBuildClassName == null) {
            typedArrayObtainAttributes.recycle();
            return null;
        }
        if (this.mParseActivityAliasArgs == null) {
            this.mParseActivityAliasArgs = new ParseComponentArgs(r31, strArr, 2, 0, 1, 8, 10, this.mSeparateProcesses, 0, 6, 4);
            this.mParseActivityAliasArgs.tag = "<activity-alias>";
        }
        this.mParseActivityAliasArgs.sa = typedArrayObtainAttributes;
        this.mParseActivityAliasArgs.flags = i;
        Activity activity = null;
        int size = r31.activities.size();
        int i2 = 0;
        while (true) {
            if (i2 >= size) {
                break;
            }
            Activity activity2 = r31.activities.get(i2);
            if (strBuildClassName.equals(activity2.info.name)) {
                activity = activity2;
                break;
            }
            i2++;
        }
        if (activity == null) {
            strArr[0] = "<activity-alias> target activity " + strBuildClassName + " not found in manifest";
            typedArrayObtainAttributes.recycle();
            return null;
        }
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.targetActivity = strBuildClassName;
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
        boolean z = activity.info.directBootAware;
        activityInfo.directBootAware = z;
        activityInfo.encryptionAware = z;
        Activity activity3 = new Activity(this.mParseActivityAliasArgs, activityInfo);
        if (strArr[0] != null) {
            typedArrayObtainAttributes.recycle();
            return null;
        }
        boolean zHasValue = typedArrayObtainAttributes.hasValue(5);
        if (zHasValue) {
            activity3.info.exported = typedArrayObtainAttributes.getBoolean(5, false);
        }
        String nonConfigurationString2 = typedArrayObtainAttributes.getNonConfigurationString(3, 0);
        if (nonConfigurationString2 != null) {
            activity3.info.permission = nonConfigurationString2.length() > 0 ? nonConfigurationString2.toString().intern() : null;
        }
        String nonConfigurationString3 = typedArrayObtainAttributes.getNonConfigurationString(9, 1024);
        if (nonConfigurationString3 != null) {
            String strBuildClassName2 = buildClassName(activity3.info.packageName, nonConfigurationString3, strArr);
            if (strArr[0] == null) {
                activity3.info.parentActivityName = strBuildClassName2;
            } else {
                Log.e(TAG, "Activity alias " + activity3.info.name + " specified invalid parentActivityName " + nonConfigurationString3);
                strArr[0] = null;
            }
        }
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
                    ActivityIntentInfo activityIntentInfo = new ActivityIntentInfo(activity3);
                    if (!parseIntent(resources, xmlResourceParser, true, true, activityIntentInfo, strArr)) {
                        return null;
                    }
                    if (activityIntentInfo.countActions() == 0) {
                        Slog.w(TAG, "No actions in intent filter at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                    } else {
                        activity3.intents.add(activityIntentInfo);
                    }
                } else if (xmlResourceParser.getName().equals("meta-data")) {
                    Bundle metaData = parseMetaData(resources, xmlResourceParser, activity3.metaData, strArr);
                    activity3.metaData = metaData;
                    if (metaData == null) {
                        return null;
                    }
                } else {
                    Slog.w(TAG, "Unknown element under <activity-alias>: " + xmlResourceParser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                    XmlUtils.skipCurrentTag(xmlResourceParser);
                }
            }
        }
        if (!zHasValue) {
            activity3.info.exported = activity3.intents.size() > 0;
        }
        return activity3;
    }

    private Provider parseProvider(Package owner, Resources res, XmlResourceParser parser, int flags, String[] outError) throws XmlPullParserException, BlockGuard.BlockGuardPolicyException, IOException {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestProvider);
        if (this.mParseProviderArgs == null) {
            this.mParseProviderArgs = new ParseComponentArgs(owner, outError, 2, 0, 1, 15, 17, this.mSeparateProcesses, 8, 14, 6);
            this.mParseProviderArgs.tag = "<provider>";
        }
        this.mParseProviderArgs.sa = sa;
        this.mParseProviderArgs.flags = flags;
        Provider p = new Provider(this.mParseProviderArgs, new ProviderInfo());
        if (outError[0] != null) {
            sa.recycle();
            return null;
        }
        boolean providerExportedDefault = owner.applicationInfo.targetSdkVersion < 17;
        p.info.exported = sa.getBoolean(7, providerExportedDefault);
        String cpname = sa.getNonConfigurationString(10, 0);
        p.info.isSyncable = sa.getBoolean(11, false);
        String permission = sa.getNonConfigurationString(3, 0);
        String str = sa.getNonConfigurationString(4, 0);
        if (str == null) {
            str = permission;
        }
        if (str == null) {
            p.info.readPermission = owner.applicationInfo.permission;
        } else {
            p.info.readPermission = str.length() > 0 ? str.toString().intern() : null;
        }
        String str2 = sa.getNonConfigurationString(5, 0);
        if (str2 == null) {
            str2 = permission;
        }
        if (str2 == null) {
            p.info.writePermission = owner.applicationInfo.permission;
        } else {
            p.info.writePermission = str2.length() > 0 ? str2.toString().intern() : null;
        }
        p.info.grantUriPermissions = sa.getBoolean(13, false);
        p.info.multiprocess = sa.getBoolean(9, false);
        p.info.initOrder = sa.getInt(12, 0);
        p.info.flags = 0;
        if (sa.getBoolean(16, false)) {
            p.info.flags |= 1073741824;
            if (p.info.exported && (flags & 128) == 0) {
                Slog.w(TAG, "Provider exported request ignored due to singleUser: " + p.className + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                p.info.exported = false;
            }
        }
        ProviderInfo providerInfo = p.info;
        boolean z = sa.getBoolean(18, false);
        p.info.directBootAware = z;
        providerInfo.encryptionAware = z;
        if (p.info.directBootAware) {
            owner.applicationInfo.privateFlags |= 256;
        }
        sa.recycle();
        if ((owner.applicationInfo.privateFlags & 2) != 0 && p.info.processName == owner.packageName) {
            outError[0] = "Heavy-weight applications can not have providers in main process";
            return null;
        }
        if (cpname == null) {
            outError[0] = "<provider> does not include authorities attribute";
            return null;
        }
        if (cpname.length() <= 0) {
            outError[0] = "<provider> has empty authorities attribute";
            return null;
        }
        p.info.authority = cpname.intern();
        if (parseProviderTags(res, parser, p, outError)) {
            return p;
        }
        return null;
    }

    private boolean parseProviderTags(Resources resources, XmlResourceParser xmlResourceParser, Provider provider, String[] strArr) throws XmlPullParserException, IOException {
        int depth = xmlResourceParser.getDepth();
        while (true) {
            int next = xmlResourceParser.next();
            if (next == 1) {
                return true;
            }
            if (next == 3 && xmlResourceParser.getDepth() <= depth) {
                return true;
            }
            if (next != 3 && next != 4) {
                if (xmlResourceParser.getName().equals("intent-filter")) {
                    ProviderIntentInfo providerIntentInfo = new ProviderIntentInfo(provider);
                    if (!parseIntent(resources, xmlResourceParser, true, false, providerIntentInfo, strArr)) {
                        return false;
                    }
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
                    PatternMatcher patternMatcher = nonConfigurationString != null ? new PatternMatcher(nonConfigurationString, 0) : null;
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
                    if (nonConfigurationString6 == null) {
                        nonConfigurationString6 = nonConfigurationString4;
                    }
                    boolean z = false;
                    if (nonConfigurationString5 != null) {
                        nonConfigurationString5 = nonConfigurationString5.intern();
                        z = true;
                    }
                    if (nonConfigurationString6 != null) {
                        nonConfigurationString6 = nonConfigurationString6.intern();
                        z = true;
                    }
                    if (z) {
                        String nonConfigurationString7 = typedArrayObtainAttributes2.getNonConfigurationString(3, 0);
                        PathPermission pathPermission = nonConfigurationString7 != null ? new PathPermission(nonConfigurationString7, 0, nonConfigurationString5, nonConfigurationString6) : null;
                        String nonConfigurationString8 = typedArrayObtainAttributes2.getNonConfigurationString(4, 0);
                        if (nonConfigurationString8 != null) {
                            pathPermission = new PathPermission(nonConfigurationString8, 1, nonConfigurationString5, nonConfigurationString6);
                        }
                        String nonConfigurationString9 = typedArrayObtainAttributes2.getNonConfigurationString(5, 0);
                        if (nonConfigurationString9 != null) {
                            pathPermission = new PathPermission(nonConfigurationString9, 2, nonConfigurationString5, nonConfigurationString6);
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
                    } else {
                        Slog.w(TAG, "No readPermission or writePermssion for <path-permission>: " + xmlResourceParser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                        XmlUtils.skipCurrentTag(xmlResourceParser);
                    }
                } else {
                    Slog.w(TAG, "Unknown element under <provider>: " + xmlResourceParser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                    XmlUtils.skipCurrentTag(xmlResourceParser);
                }
            }
        }
    }

    private Service parseService(Package r23, Resources resources, XmlResourceParser xmlResourceParser, int i, String[] strArr) throws XmlPullParserException, BlockGuard.BlockGuardPolicyException, IOException {
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestService);
        if (this.mParseServiceArgs == null) {
            this.mParseServiceArgs = new ParseComponentArgs(r23, strArr, 2, 0, 1, 8, 12, this.mSeparateProcesses, 6, 7, 4);
            this.mParseServiceArgs.tag = "<service>";
        }
        this.mParseServiceArgs.sa = typedArrayObtainAttributes;
        this.mParseServiceArgs.flags = i;
        Service service = new Service(this.mParseServiceArgs, new ServiceInfo());
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
            service.info.permission = r23.applicationInfo.permission;
        } else {
            service.info.permission = nonConfigurationString.length() > 0 ? nonConfigurationString.toString().intern() : null;
        }
        service.info.flags = 0;
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
            if (service.info.exported && (i & 128) == 0) {
                Slog.w(TAG, "Service exported request ignored due to singleUser: " + service.className + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                service.info.exported = false;
                zHasValue = true;
            }
        }
        ServiceInfo serviceInfo = service.info;
        boolean z = typedArrayObtainAttributes.getBoolean(13, false);
        service.info.directBootAware = z;
        serviceInfo.encryptionAware = z;
        if (service.info.directBootAware) {
            r23.applicationInfo.privateFlags |= 256;
        }
        typedArrayObtainAttributes.recycle();
        if ((r23.applicationInfo.privateFlags & 2) != 0 && service.info.processName == r23.packageName) {
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
                    ServiceIntentInfo serviceIntentInfo = new ServiceIntentInfo(service);
                    if (!parseIntent(resources, xmlResourceParser, true, false, serviceIntentInfo, strArr)) {
                        return null;
                    }
                    service.intents.add(serviceIntentInfo);
                } else if (xmlResourceParser.getName().equals("meta-data")) {
                    Bundle metaData = parseMetaData(resources, xmlResourceParser, service.metaData, strArr);
                    service.metaData = metaData;
                    if (metaData == null) {
                        return null;
                    }
                } else {
                    Slog.w(TAG, "Unknown element under <service>: " + xmlResourceParser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + xmlResourceParser.getPositionDescription());
                    XmlUtils.skipCurrentTag(xmlResourceParser);
                }
            }
        }
    }

    private boolean parseAllMetaData(Resources res, XmlResourceParser parser, String tag, Component<?> outInfo, String[] outError) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            if (type != 3 && type != 4) {
                if (parser.getName().equals("meta-data")) {
                    Bundle metaData = parseMetaData(res, parser, outInfo.metaData, outError);
                    outInfo.metaData = metaData;
                    if (metaData == null) {
                        return false;
                    }
                } else {
                    Slog.w(TAG, "Unknown element under " + tag + ": " + parser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    private Bundle parseMetaData(Resources res, XmlResourceParser parser, Bundle data, String[] outError) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestMetaData);
        if (data == null) {
            data = new Bundle();
        }
        String name = sa.getNonConfigurationString(0, 0);
        if (name == null) {
            outError[0] = "<meta-data> requires an android:name attribute";
            sa.recycle();
            return null;
        }
        String name2 = name.intern();
        TypedValue v = sa.peekValue(2);
        if (v == null || v.resourceId == 0) {
            TypedValue v2 = sa.peekValue(1);
            if (v2 == null) {
                outError[0] = "<meta-data> requires an android:value or android:resource attribute";
                data = null;
            } else if (v2.type == 3) {
                CharSequence cs = v2.coerceToString();
                data.putString(name2, cs != null ? cs.toString().intern() : null);
            } else if (v2.type == 18) {
                data.putBoolean(name2, v2.data != 0);
            } else if (v2.type >= 16 && v2.type <= 31) {
                data.putInt(name2, v2.data);
            } else if (v2.type == 4) {
                data.putFloat(name2, v2.getFloat());
            } else {
                Slog.w(TAG, "<meta-data> only supports string, integer, float, color, boolean, and resource reference types: " + parser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
            }
        } else {
            data.putInt(name2, v.resourceId);
        }
        sa.recycle();
        XmlUtils.skipCurrentTag(parser);
        return data;
    }

    private static VerifierInfo parseVerifier(Resources res, XmlPullParser parser, AttributeSet attrs, int flags) {
        TypedArray sa = res.obtainAttributes(attrs, R.styleable.AndroidManifestPackageVerifier);
        String packageName = sa.getNonResourceString(0);
        String encodedPublicKey = sa.getNonResourceString(1);
        sa.recycle();
        if (packageName == null || packageName.length() == 0) {
            Slog.i(TAG, "verifier package name was null; skipping");
            return null;
        }
        PublicKey publicKey = parsePublicKey(encodedPublicKey);
        if (publicKey == null) {
            Slog.i(TAG, "Unable to parse verifier public key for " + packageName);
            return null;
        }
        return new VerifierInfo(packageName, publicKey);
    }

    public static final PublicKey parsePublicKey(String encodedPublicKey) {
        if (encodedPublicKey == null) {
            Slog.w(TAG, "Could not parse null public key");
            return null;
        }
        try {
            byte[] encoded = Base64.decode(encodedPublicKey, 0);
            EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            try {
                KeyFactory keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_RSA);
                return keyFactory.generatePublic(keySpec);
            } catch (NoSuchAlgorithmException e) {
                Slog.wtf(TAG, "Could not parse public key: RSA KeyFactory not included in build");
                try {
                    KeyFactory keyFactory2 = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC);
                    return keyFactory2.generatePublic(keySpec);
                } catch (NoSuchAlgorithmException e2) {
                    Slog.wtf(TAG, "Could not parse public key: EC KeyFactory not included in build");
                    try {
                        KeyFactory keyFactory3 = KeyFactory.getInstance("DSA");
                        return keyFactory3.generatePublic(keySpec);
                    } catch (NoSuchAlgorithmException e3) {
                        Slog.wtf(TAG, "Could not parse public key: DSA KeyFactory not included in build");
                        return null;
                    } catch (InvalidKeySpecException e4) {
                        return null;
                    }
                } catch (InvalidKeySpecException e5) {
                    KeyFactory keyFactory32 = KeyFactory.getInstance("DSA");
                    return keyFactory32.generatePublic(keySpec);
                }
            } catch (InvalidKeySpecException e6) {
                KeyFactory keyFactory22 = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC);
                return keyFactory22.generatePublic(keySpec);
            }
        } catch (IllegalArgumentException e7) {
            Slog.w(TAG, "Could not parse verifier public key; invalid Base64");
            return null;
        }
    }

    private boolean parseIntent(Resources res, XmlResourceParser parser, boolean allowGlobs, boolean allowAutoVerify, IntentInfo outInfo, String[] outError) throws XmlPullParserException, BlockGuard.BlockGuardPolicyException, IOException {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestIntentFilter);
        int priority = sa.getInt(2, 0);
        outInfo.setPriority(priority);
        TypedValue v = sa.peekValue(0);
        if (v != null) {
            int i = v.resourceId;
            outInfo.labelRes = i;
            if (i == 0) {
                outInfo.nonLocalizedLabel = v.coerceToString();
            }
        }
        outInfo.icon = sa.getResourceId(1, 0);
        outInfo.logo = sa.getResourceId(3, 0);
        outInfo.banner = sa.getResourceId(4, 0);
        if (allowAutoVerify) {
            outInfo.setAutoVerify(sa.getBoolean(5, false));
        }
        sa.recycle();
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            if (type != 3 && type != 4) {
                String nodeName = parser.getName();
                if (nodeName.equals(Camera.Parameters.SCENE_MODE_ACTION)) {
                    String value = parser.getAttributeValue(ANDROID_RESOURCES, "name");
                    if (value == null || value == ProxyInfo.LOCAL_EXCL_LIST) {
                        break;
                    }
                    XmlUtils.skipCurrentTag(parser);
                    outInfo.addAction(value);
                } else if (nodeName.equals("category")) {
                    String value2 = parser.getAttributeValue(ANDROID_RESOURCES, "name");
                    if (value2 == null || value2 == ProxyInfo.LOCAL_EXCL_LIST) {
                        break;
                    }
                    XmlUtils.skipCurrentTag(parser);
                    outInfo.addCategory(value2);
                } else if (nodeName.equals("data")) {
                    TypedArray sa2 = res.obtainAttributes(parser, R.styleable.AndroidManifestData);
                    String str = sa2.getNonConfigurationString(0, 0);
                    if (str != null) {
                        try {
                            outInfo.addDataType(str);
                        } catch (IntentFilter.MalformedMimeTypeException e) {
                            outError[0] = e.toString();
                            sa2.recycle();
                            return false;
                        }
                    }
                    String str2 = sa2.getNonConfigurationString(1, 0);
                    if (str2 != null) {
                        outInfo.addDataScheme(str2);
                    }
                    String str3 = sa2.getNonConfigurationString(7, 0);
                    if (str3 != null) {
                        outInfo.addDataSchemeSpecificPart(str3, 0);
                    }
                    String str4 = sa2.getNonConfigurationString(8, 0);
                    if (str4 != null) {
                        outInfo.addDataSchemeSpecificPart(str4, 1);
                    }
                    String str5 = sa2.getNonConfigurationString(9, 0);
                    if (str5 != null) {
                        if (!allowGlobs) {
                            outError[0] = "sspPattern not allowed here; ssp must be literal";
                            return false;
                        }
                        outInfo.addDataSchemeSpecificPart(str5, 2);
                    }
                    String host = sa2.getNonConfigurationString(2, 0);
                    String port = sa2.getNonConfigurationString(3, 0);
                    if (host != null) {
                        outInfo.addDataAuthority(host, port);
                    }
                    String str6 = sa2.getNonConfigurationString(4, 0);
                    if (str6 != null) {
                        outInfo.addDataPath(str6, 0);
                    }
                    String str7 = sa2.getNonConfigurationString(5, 0);
                    if (str7 != null) {
                        outInfo.addDataPath(str7, 1);
                    }
                    String str8 = sa2.getNonConfigurationString(6, 0);
                    if (str8 != null) {
                        if (!allowGlobs) {
                            outError[0] = "pathPattern not allowed here; path must be literal";
                            return false;
                        }
                        outInfo.addDataPath(str8, 2);
                    }
                    sa2.recycle();
                    XmlUtils.skipCurrentTag(parser);
                } else {
                    Slog.w(TAG, "Unknown element under <intent-filter>: " + parser.getName() + " at " + this.mArchiveSourcePath + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    public static final class Package {
        public String baseCodePath;
        public boolean baseHardwareAccelerated;
        public int baseRevisionCode;
        public ArrayList<Package> childPackages;
        public String codePath;
        public boolean coreApp;
        public String cpuAbiOverride;
        public int installLocation;
        public Certificate[][] mCertificates;
        public Object mExtras;
        public ArrayMap<String, ArraySet<PublicKey>> mKeySetMapping;
        public int mOverlayPriority;
        public String mOverlayTarget;
        public String mRequiredAccountType;
        public boolean mRequiredForAllUsers;
        public String mRestrictedAccountType;
        public String mSharedUserId;
        public int mSharedUserLabel;
        public Signature[] mSignatures;
        public ArraySet<PublicKey> mSigningKeys;
        public boolean mTrustedOverlay;
        public ArraySet<String> mUpgradeKeySets;
        public int mVersionCode;
        public String mVersionName;
        public String packageName;
        public Package parentPackage;
        public ArrayList<String> protectedBroadcasts;
        public byte[] restrictUpdateHash;
        public String[] splitCodePaths;
        public int[] splitFlags;
        public String[] splitNames;
        public int[] splitPrivateFlags;
        public int[] splitRevisionCodes;
        public boolean use32bitAbi;
        public String volumeUuid;
        public final ApplicationInfo applicationInfo = new ApplicationInfo();
        public final ArrayList<Permission> permissions = new ArrayList<>(0);
        public final ArrayList<PermissionGroup> permissionGroups = new ArrayList<>(0);
        public final ArrayList<Activity> activities = new ArrayList<>(0);
        public final ArrayList<Activity> receivers = new ArrayList<>(0);
        public final ArrayList<Provider> providers = new ArrayList<>(0);
        public final ArrayList<Service> services = new ArrayList<>(0);
        public final ArrayList<Instrumentation> instrumentation = new ArrayList<>(0);
        public final ArrayList<String> requestedPermissions = new ArrayList<>();
        public ArrayList<String> libraryNames = null;
        public ArrayList<String> usesLibraries = null;
        public ArrayList<String> usesOptionalLibraries = null;
        public String[] usesLibraryFiles = null;
        public ArrayList<ActivityIntentInfo> preferredActivityFilters = null;
        public ArrayList<String> mOriginalPackages = null;
        public String mRealPackage = null;
        public ArrayList<String> mAdoptPermissions = null;
        public Bundle mAppMetaData = null;
        public int mPreferredOrder = 0;
        public long[] mLastPackageUsageTimeInMills = new long[8];
        public ArrayList<ConfigurationInfo> configPreferences = null;
        public ArrayList<FeatureInfo> reqFeatures = null;
        public ArrayList<FeatureGroupInfo> featureGroups = null;

        public Package(String packageName) {
            this.packageName = packageName;
            this.applicationInfo.packageName = packageName;
            this.applicationInfo.uid = -1;
        }

        public void setApplicationVolumeUuid(String volumeUuid) {
            this.applicationInfo.volumeUuid = volumeUuid;
            if (this.childPackages == null) {
                return;
            }
            int packageCount = this.childPackages.size();
            for (int i = 0; i < packageCount; i++) {
                this.childPackages.get(i).applicationInfo.volumeUuid = volumeUuid;
            }
        }

        public void setApplicationInfoCodePath(String codePath) {
            this.applicationInfo.setCodePath(codePath);
            if (this.childPackages == null) {
                return;
            }
            int packageCount = this.childPackages.size();
            for (int i = 0; i < packageCount; i++) {
                this.childPackages.get(i).applicationInfo.setCodePath(codePath);
            }
        }

        public void setApplicationInfoResourcePath(String resourcePath) {
            this.applicationInfo.setResourcePath(resourcePath);
            if (this.childPackages == null) {
                return;
            }
            int packageCount = this.childPackages.size();
            for (int i = 0; i < packageCount; i++) {
                this.childPackages.get(i).applicationInfo.setResourcePath(resourcePath);
            }
        }

        public void setApplicationInfoBaseResourcePath(String resourcePath) {
            this.applicationInfo.setBaseResourcePath(resourcePath);
            if (this.childPackages == null) {
                return;
            }
            int packageCount = this.childPackages.size();
            for (int i = 0; i < packageCount; i++) {
                this.childPackages.get(i).applicationInfo.setBaseResourcePath(resourcePath);
            }
        }

        public void setApplicationInfoBaseCodePath(String baseCodePath) {
            this.applicationInfo.setBaseCodePath(baseCodePath);
            if (this.childPackages == null) {
                return;
            }
            int packageCount = this.childPackages.size();
            for (int i = 0; i < packageCount; i++) {
                this.childPackages.get(i).applicationInfo.setBaseCodePath(baseCodePath);
            }
        }

        public boolean hasChildPackage(String packageName) {
            int childCount = this.childPackages != null ? this.childPackages.size() : 0;
            for (int i = 0; i < childCount; i++) {
                if (this.childPackages.get(i).packageName.equals(packageName)) {
                    return true;
                }
            }
            return false;
        }

        public void setApplicationInfoSplitCodePaths(String[] splitCodePaths) {
            this.applicationInfo.setSplitCodePaths(splitCodePaths);
        }

        public void setApplicationInfoSplitResourcePaths(String[] resroucePaths) {
            this.applicationInfo.setSplitResourcePaths(resroucePaths);
        }

        public void setSplitCodePaths(String[] codePaths) {
            this.splitCodePaths = codePaths;
        }

        public void setCodePath(String codePath) {
            this.codePath = codePath;
            if (this.childPackages == null) {
                return;
            }
            int packageCount = this.childPackages.size();
            for (int i = 0; i < packageCount; i++) {
                this.childPackages.get(i).codePath = codePath;
            }
        }

        public void setBaseCodePath(String baseCodePath) {
            this.baseCodePath = baseCodePath;
            if (this.childPackages == null) {
                return;
            }
            int packageCount = this.childPackages.size();
            for (int i = 0; i < packageCount; i++) {
                this.childPackages.get(i).baseCodePath = baseCodePath;
            }
        }

        public void setSignatures(Signature[] signatures) {
            this.mSignatures = signatures;
            if (this.childPackages == null) {
                return;
            }
            int packageCount = this.childPackages.size();
            for (int i = 0; i < packageCount; i++) {
                this.childPackages.get(i).mSignatures = signatures;
            }
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
            if (this.childPackages == null) {
                return;
            }
            int packageCount = this.childPackages.size();
            for (int i = 0; i < packageCount; i++) {
                this.childPackages.get(i).volumeUuid = volumeUuid;
            }
        }

        public void setApplicationInfoFlags(int mask, int flags) {
            this.applicationInfo.flags = (this.applicationInfo.flags & (~mask)) | (mask & flags);
            if (this.childPackages == null) {
                return;
            }
            int packageCount = this.childPackages.size();
            for (int i = 0; i < packageCount; i++) {
                this.childPackages.get(i).applicationInfo.flags = (this.applicationInfo.flags & (~mask)) | (mask & flags);
            }
        }

        public void setUse32bitAbi(boolean use32bitAbi) {
            this.use32bitAbi = use32bitAbi;
            if (this.childPackages == null) {
                return;
            }
            int packageCount = this.childPackages.size();
            for (int i = 0; i < packageCount; i++) {
                this.childPackages.get(i).use32bitAbi = use32bitAbi;
            }
        }

        public List<String> getAllCodePaths() {
            ArrayList<String> paths = new ArrayList<>();
            paths.add(this.baseCodePath);
            if (!ArrayUtils.isEmpty(this.splitCodePaths)) {
                Collections.addAll(paths, this.splitCodePaths);
            }
            return paths;
        }

        public List<String> getAllCodePathsExcludingResourceOnly() {
            ArrayList<String> paths = new ArrayList<>();
            if ((this.applicationInfo.flags & 4) != 0) {
                paths.add(this.baseCodePath);
            }
            if (!ArrayUtils.isEmpty(this.splitCodePaths)) {
                for (int i = 0; i < this.splitCodePaths.length; i++) {
                    if ((this.splitFlags[i] & 4) != 0) {
                        paths.add(this.splitCodePaths[i]);
                    }
                }
            }
            return paths;
        }

        public void setPackageName(String newName) {
            this.packageName = newName;
            this.applicationInfo.packageName = newName;
            for (int i = this.permissions.size() - 1; i >= 0; i--) {
                this.permissions.get(i).setPackageName(newName);
            }
            for (int i2 = this.permissionGroups.size() - 1; i2 >= 0; i2--) {
                this.permissionGroups.get(i2).setPackageName(newName);
            }
            for (int i3 = this.activities.size() - 1; i3 >= 0; i3--) {
                this.activities.get(i3).setPackageName(newName);
            }
            for (int i4 = this.receivers.size() - 1; i4 >= 0; i4--) {
                this.receivers.get(i4).setPackageName(newName);
            }
            for (int i5 = this.providers.size() - 1; i5 >= 0; i5--) {
                this.providers.get(i5).setPackageName(newName);
            }
            for (int i6 = this.services.size() - 1; i6 >= 0; i6--) {
                this.services.get(i6).setPackageName(newName);
            }
            for (int i7 = this.instrumentation.size() - 1; i7 >= 0; i7--) {
                this.instrumentation.get(i7).setPackageName(newName);
            }
        }

        public boolean hasComponentClassName(String name) {
            for (int i = this.activities.size() - 1; i >= 0; i--) {
                if (name.equals(this.activities.get(i).className)) {
                    return true;
                }
            }
            for (int i2 = this.receivers.size() - 1; i2 >= 0; i2--) {
                if (name.equals(this.receivers.get(i2).className)) {
                    return true;
                }
            }
            for (int i3 = this.providers.size() - 1; i3 >= 0; i3--) {
                if (name.equals(this.providers.get(i3).className)) {
                    return true;
                }
            }
            for (int i4 = this.services.size() - 1; i4 >= 0; i4--) {
                if (name.equals(this.services.get(i4).className)) {
                    return true;
                }
            }
            for (int i5 = this.instrumentation.size() - 1; i5 >= 0; i5--) {
                if (name.equals(this.instrumentation.get(i5).className)) {
                    return true;
                }
            }
            return false;
        }

        public boolean isForwardLocked() {
            return this.applicationInfo.isForwardLocked();
        }

        public boolean isSystemApp() {
            return this.applicationInfo.isSystemApp();
        }

        public boolean isPrivilegedApp() {
            return this.applicationInfo.isPrivilegedApp();
        }

        public boolean isUpdatedSystemApp() {
            return this.applicationInfo.isUpdatedSystemApp();
        }

        public boolean canHaveOatDir() {
            boolean isVendorApps = this.applicationInfo.isVendorApp();
            if (isSystemApp()) {
                isVendorApps = true;
            }
            return ((isVendorApps && !isUpdatedSystemApp()) || isForwardLocked() || this.applicationInfo.isExternalAsec()) ? false : true;
        }

        public boolean isMatch(int flags) {
            if ((1048576 & flags) != 0) {
                return isSystemApp();
            }
            return true;
        }

        public long getLatestPackageUseTimeInMills() {
            long latestUse = 0;
            for (long use : this.mLastPackageUsageTimeInMills) {
                latestUse = Math.max(latestUse, use);
            }
            return latestUse;
        }

        public long getLatestForegroundPackageUseTimeInMills() {
            int[] foregroundReasons = {0, 2};
            long latestUse = 0;
            for (int reason : foregroundReasons) {
                latestUse = Math.max(latestUse, this.mLastPackageUsageTimeInMills[reason]);
            }
            return latestUse;
        }

        public String toString() {
            return "Package{" + Integer.toHexString(System.identityHashCode(this)) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.packageName + "}";
        }
    }

    public static class Component<II extends IntentInfo> {
        public final String className;
        ComponentName componentName;
        String componentShortName;
        public final ArrayList<II> intents;
        public Bundle metaData;
        public final Package owner;

        public Component(Package _owner) {
            this.owner = _owner;
            this.intents = null;
            this.className = null;
        }

        public Component(ParsePackageItemArgs args, PackageItemInfo outInfo) {
            this.owner = args.owner;
            this.intents = new ArrayList<>(0);
            String name = args.sa.getNonConfigurationString(args.nameRes, 0);
            if (name == null) {
                this.className = null;
                args.outError[0] = args.tag + " does not specify android:name";
                return;
            }
            outInfo.name = PackageParser.buildClassName(this.owner.applicationInfo.packageName, name, args.outError);
            if (outInfo.name == null) {
                this.className = null;
                args.outError[0] = args.tag + " does not have valid android:name";
                return;
            }
            this.className = outInfo.name;
            int iconVal = args.sa.getResourceId(args.iconRes, 0);
            if (iconVal != 0) {
                outInfo.icon = iconVal;
                outInfo.nonLocalizedLabel = null;
            }
            int logoVal = args.sa.getResourceId(args.logoRes, 0);
            if (logoVal != 0) {
                outInfo.logo = logoVal;
            }
            int bannerVal = args.sa.getResourceId(args.bannerRes, 0);
            if (bannerVal != 0) {
                outInfo.banner = bannerVal;
            }
            TypedValue v = args.sa.peekValue(args.labelRes);
            if (v != null) {
                int i = v.resourceId;
                outInfo.labelRes = i;
                if (i == 0) {
                    outInfo.nonLocalizedLabel = v.coerceToString();
                }
            }
            outInfo.packageName = this.owner.packageName;
        }

        public Component(ParseComponentArgs args, ComponentInfo outInfo) {
            CharSequence pname;
            this((ParsePackageItemArgs) args, (PackageItemInfo) outInfo);
            if (args.outError[0] != null) {
                return;
            }
            if (args.processRes != 0) {
                if (this.owner.applicationInfo.targetSdkVersion >= 8) {
                    pname = args.sa.getNonConfigurationString(args.processRes, 1024);
                } else {
                    pname = args.sa.getNonResourceString(args.processRes);
                }
                outInfo.processName = PackageParser.buildProcessName(this.owner.applicationInfo.packageName, this.owner.applicationInfo.processName, pname, args.flags, args.sepProcesses, args.outError);
            }
            if (args.descriptionRes != 0) {
                outInfo.descriptionRes = args.sa.getResourceId(args.descriptionRes, 0);
            }
            outInfo.enabled = args.sa.getBoolean(args.enabledRes, true);
        }

        public Component(Component<II> clone) {
            this.owner = clone.owner;
            this.intents = clone.intents;
            this.className = clone.className;
            this.componentName = clone.componentName;
            this.componentShortName = clone.componentShortName;
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

        public void appendComponentShortName(StringBuilder sb) {
            ComponentName.appendShortString(sb, this.owner.applicationInfo.packageName, this.className);
        }

        public void printComponentShortName(PrintWriter pw) {
            ComponentName.printShortString(pw, this.owner.applicationInfo.packageName, this.className);
        }

        public void setPackageName(String packageName) {
            this.componentName = null;
            this.componentShortName = null;
        }
    }

    public static final class Permission extends Component<IntentInfo> {
        public PermissionGroup group;
        public final PermissionInfo info;
        public boolean tree;

        public Permission(Package _owner) {
            super(_owner);
            this.info = new PermissionInfo();
        }

        public Permission(Package _owner, PermissionInfo _info) {
            super(_owner);
            this.info = _info;
        }

        @Override
        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
        }

        public String toString() {
            return "Permission{" + Integer.toHexString(System.identityHashCode(this)) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.info.name + "}";
        }
    }

    public static final class PermissionGroup extends Component<IntentInfo> {
        public final PermissionGroupInfo info;

        public PermissionGroup(Package _owner) {
            super(_owner);
            this.info = new PermissionGroupInfo();
        }

        public PermissionGroup(Package _owner, PermissionGroupInfo _info) {
            super(_owner);
            this.info = _info;
        }

        @Override
        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
        }

        public String toString() {
            return "PermissionGroup{" + Integer.toHexString(System.identityHashCode(this)) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.info.name + "}";
        }
    }

    private static boolean copyNeeded(int flags, Package p, PackageUserState state, Bundle metaData, int userId) {
        if (userId != 0) {
            return true;
        }
        if (state.enabled != 0) {
            boolean enabled = state.enabled == 1;
            if (p.applicationInfo.enabled != enabled) {
                return true;
            }
        }
        boolean suspended = (p.applicationInfo.flags & 1073741824) != 0;
        if (state.suspended != suspended || !state.installed || state.hidden || state.stopped) {
            return true;
        }
        if ((flags & 128) == 0 || (metaData == null && p.mAppMetaData == null)) {
            return ((flags & 1024) == 0 || p.usesLibraryFiles == null) ? false : true;
        }
        return true;
    }

    public static ApplicationInfo generateApplicationInfo(Package p, int flags, PackageUserState state) {
        return generateApplicationInfo(p, flags, state, UserHandle.getCallingUserId());
    }

    private static void updateApplicationInfo(ApplicationInfo ai, int flags, PackageUserState state) {
        if (!sCompatibilityModeEnabled) {
            ai.disableCompatibilityMode();
        }
        if (state.installed) {
            ai.flags |= 8388608;
        } else {
            ai.flags &= -8388609;
        }
        if (state.suspended) {
            ai.flags |= 1073741824;
        } else {
            ai.flags &= -1073741825;
        }
        if (state.hidden) {
            ai.privateFlags |= 1;
        } else {
            ai.privateFlags &= -2;
        }
        if (state.enabled == 1) {
            ai.enabled = true;
        } else if (state.enabled == 4) {
            ai.enabled = (32768 & flags) != 0;
        } else if (state.enabled == 2 || state.enabled == 3) {
            ai.enabled = false;
        }
        ai.enabledSetting = state.enabled;
    }

    public static ApplicationInfo generateApplicationInfo(Package p, int flags, PackageUserState state, int userId) {
        if (p == null || !checkUseInstalledOrHidden(flags, state) || !p.isMatch(flags)) {
            return null;
        }
        if (!copyNeeded(flags, p, state, null, userId) && ((32768 & flags) == 0 || state.enabled != 4)) {
            updateApplicationInfo(p.applicationInfo, flags, state);
            return p.applicationInfo;
        }
        ApplicationInfo ai = new ApplicationInfo(p.applicationInfo);
        ai.initForUser(userId);
        if ((flags & 128) != 0) {
            ai.metaData = p.mAppMetaData;
        }
        if ((flags & 1024) != 0) {
            ai.sharedLibraryFiles = p.usesLibraryFiles;
        }
        if (state.stopped) {
            ai.flags |= 2097152;
        } else {
            ai.flags &= -2097153;
        }
        updateApplicationInfo(ai, flags, state);
        return ai;
    }

    public static ApplicationInfo generateApplicationInfo(ApplicationInfo ai, int flags, PackageUserState state, int userId) {
        if (ai == null || !checkUseInstalledOrHidden(flags, state)) {
            return null;
        }
        ApplicationInfo ai2 = new ApplicationInfo(ai);
        ai2.initForUser(userId);
        if (state.stopped) {
            ai2.flags |= 2097152;
        } else {
            ai2.flags &= -2097153;
        }
        updateApplicationInfo(ai2, flags, state);
        return ai2;
    }

    public static final PermissionInfo generatePermissionInfo(Permission p, int flags) {
        if (p == null) {
            return null;
        }
        if ((flags & 128) == 0) {
            return p.info;
        }
        PermissionInfo pi = new PermissionInfo(p.info);
        pi.metaData = p.metaData;
        return pi;
    }

    public static final PermissionGroupInfo generatePermissionGroupInfo(PermissionGroup pg, int flags) {
        if (pg == null) {
            return null;
        }
        if ((flags & 128) == 0) {
            return pg.info;
        }
        PermissionGroupInfo pgi = new PermissionGroupInfo(pg.info);
        pgi.metaData = pg.metaData;
        return pgi;
    }

    public static final class Activity extends Component<ActivityIntentInfo> {
        public final ActivityInfo info;

        public Activity(ParseComponentArgs args, ActivityInfo _info) {
            super(args, (ComponentInfo) _info);
            this.info = _info;
            this.info.applicationInfo = args.owner.applicationInfo;
        }

        @Override
        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
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
    }

    public static final ActivityInfo generateActivityInfo(Activity a, int flags, PackageUserState state, int userId) {
        if (a == null || !checkUseInstalledOrHidden(flags, state)) {
            return null;
        }
        if (!copyNeeded(flags, a.owner, state, a.metaData, userId)) {
            return a.info;
        }
        ActivityInfo ai = new ActivityInfo(a.info);
        ai.metaData = a.metaData;
        ai.applicationInfo = generateApplicationInfo(a.owner, flags, state, userId);
        return ai;
    }

    public static final ActivityInfo generateActivityInfo(ActivityInfo ai, int flags, PackageUserState state, int userId) {
        if (ai == null || !checkUseInstalledOrHidden(flags, state)) {
            return null;
        }
        ActivityInfo ai2 = new ActivityInfo(ai);
        ai2.applicationInfo = generateApplicationInfo(ai2.applicationInfo, flags, state, userId);
        return ai2;
    }

    public static final class Service extends Component<ServiceIntentInfo> {
        public final ServiceInfo info;

        public Service(ParseComponentArgs args, ServiceInfo _info) {
            super(args, (ComponentInfo) _info);
            this.info = _info;
            this.info.applicationInfo = args.owner.applicationInfo;
        }

        @Override
        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
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
    }

    public static final ServiceInfo generateServiceInfo(Service s, int flags, PackageUserState state, int userId) {
        if (s == null || !checkUseInstalledOrHidden(flags, state)) {
            return null;
        }
        if (!copyNeeded(flags, s.owner, state, s.metaData, userId)) {
            return s.info;
        }
        ServiceInfo si = new ServiceInfo(s.info);
        si.metaData = s.metaData;
        si.applicationInfo = generateApplicationInfo(s.owner, flags, state, userId);
        return si;
    }

    public static final class Provider extends Component<ProviderIntentInfo> {
        public final ProviderInfo info;
        public boolean syncable;

        public Provider(ParseComponentArgs args, ProviderInfo _info) {
            super(args, (ComponentInfo) _info);
            this.info = _info;
            this.info.applicationInfo = args.owner.applicationInfo;
            this.syncable = false;
        }

        public Provider(Provider existingProvider) {
            super(existingProvider);
            this.info = existingProvider.info;
            this.syncable = existingProvider.syncable;
        }

        @Override
        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
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
    }

    public static final ProviderInfo generateProviderInfo(Provider p, int flags, PackageUserState state, int userId) {
        if (p == null || !checkUseInstalledOrHidden(flags, state)) {
            return null;
        }
        if (!copyNeeded(flags, p.owner, state, p.metaData, userId) && ((flags & 2048) != 0 || p.info.uriPermissionPatterns == null)) {
            return p.info;
        }
        ProviderInfo pi = new ProviderInfo(p.info);
        pi.metaData = p.metaData;
        if ((flags & 2048) == 0) {
            pi.uriPermissionPatterns = null;
        }
        pi.applicationInfo = generateApplicationInfo(p.owner, flags, state, userId);
        return pi;
    }

    public static final class Instrumentation extends Component<IntentInfo> {
        public final InstrumentationInfo info;

        public Instrumentation(ParsePackageItemArgs args, InstrumentationInfo _info) {
            super(args, _info);
            this.info = _info;
        }

        @Override
        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
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
    }

    public static final InstrumentationInfo generateInstrumentationInfo(Instrumentation i, int flags) {
        if (i == null) {
            return null;
        }
        if ((flags & 128) == 0) {
            return i.info;
        }
        InstrumentationInfo ii = new InstrumentationInfo(i.info);
        ii.metaData = i.metaData;
        return ii;
    }

    public static final class ActivityIntentInfo extends IntentInfo {
        public final Activity activity;

        public ActivityIntentInfo(Activity _activity) {
            this.activity = _activity;
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
    }

    public static final class ServiceIntentInfo extends IntentInfo {
        public final Service service;

        public ServiceIntentInfo(Service _service) {
            this.service = _service;
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
    }

    public static final class ProviderIntentInfo extends IntentInfo {
        public final Provider provider;

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
    }

    public static void setCompatibilityModeEnabled(boolean compatibilityModeEnabled) {
        sCompatibilityModeEnabled = compatibilityModeEnabled;
    }

    public static long readFullyIgnoringContents(InputStream in) throws IOException {
        byte[] buffer = sBuffer.getAndSet(null);
        if (buffer == null) {
            buffer = new byte[4096];
        }
        int count = 0;
        while (true) {
            int n = in.read(buffer, 0, buffer.length);
            if (n != -1) {
                count += n;
            } else {
                sBuffer.set(buffer);
                return count;
            }
        }
    }

    public static void closeQuietly(StrictJarFile jarFile) {
        if (jarFile == null) {
            return;
        }
        try {
            jarFile.close();
        } catch (Exception e) {
        }
    }

    public static class PackageParserException extends Exception {
        public final int error;

        public PackageParserException(int error, String detailMessage) {
            super(detailMessage);
            this.error = error;
        }

        public PackageParserException(int error, String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
            this.error = error;
        }
    }
}
