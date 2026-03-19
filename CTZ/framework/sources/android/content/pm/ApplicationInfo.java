package android.content.pm;

import android.annotation.SystemApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.util.Printer;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.R;
import com.android.internal.util.ArrayUtils;
import com.android.server.SystemConfig;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

public class ApplicationInfo extends PackageItemInfo implements Parcelable {
    public static final int CATEGORY_AUDIO = 1;
    public static final int CATEGORY_GAME = 0;
    public static final int CATEGORY_IMAGE = 3;
    public static final int CATEGORY_MAPS = 6;
    public static final int CATEGORY_NEWS = 5;
    public static final int CATEGORY_PRODUCTIVITY = 7;
    public static final int CATEGORY_SOCIAL = 4;
    public static final int CATEGORY_UNDEFINED = -1;
    public static final int CATEGORY_VIDEO = 2;
    public static final Parcelable.Creator<ApplicationInfo> CREATOR = new Parcelable.Creator<ApplicationInfo>() {
        @Override
        public ApplicationInfo createFromParcel(Parcel parcel) {
            return new ApplicationInfo(parcel);
        }

        @Override
        public ApplicationInfo[] newArray(int i) {
            return new ApplicationInfo[i];
        }
    };
    public static final int FLAG_ALLOW_BACKUP = 32768;
    public static final int FLAG_ALLOW_CLEAR_USER_DATA = 64;
    public static final int FLAG_ALLOW_TASK_REPARENTING = 32;
    public static final int FLAG_DEBUGGABLE = 2;
    public static final int FLAG_EXTERNAL_STORAGE = 262144;
    public static final int FLAG_EXTRACT_NATIVE_LIBS = 268435456;
    public static final int FLAG_FACTORY_TEST = 16;
    public static final int FLAG_FULL_BACKUP_ONLY = 67108864;
    public static final int FLAG_HARDWARE_ACCELERATED = 536870912;
    public static final int FLAG_HAS_CODE = 4;
    public static final int FLAG_INSTALLED = 8388608;
    public static final int FLAG_IS_DATA_ONLY = 16777216;

    @Deprecated
    public static final int FLAG_IS_GAME = 33554432;
    public static final int FLAG_KILL_AFTER_RESTORE = 65536;
    public static final int FLAG_LARGE_HEAP = 1048576;
    public static final int FLAG_MULTIARCH = Integer.MIN_VALUE;
    public static final int FLAG_PERSISTENT = 8;
    public static final int FLAG_RESIZEABLE_FOR_SCREENS = 4096;
    public static final int FLAG_RESTORE_ANY_VERSION = 131072;
    public static final int FLAG_STOPPED = 2097152;
    public static final int FLAG_SUPPORTS_LARGE_SCREENS = 2048;
    public static final int FLAG_SUPPORTS_NORMAL_SCREENS = 1024;
    public static final int FLAG_SUPPORTS_RTL = 4194304;
    public static final int FLAG_SUPPORTS_SCREEN_DENSITIES = 8192;
    public static final int FLAG_SUPPORTS_SMALL_SCREENS = 512;
    public static final int FLAG_SUPPORTS_XLARGE_SCREENS = 524288;
    public static final int FLAG_SUSPENDED = 1073741824;
    public static final int FLAG_SYSTEM = 1;
    public static final int FLAG_TEST_ONLY = 256;
    public static final int FLAG_UPDATED_SYSTEM_APP = 128;
    public static final int FLAG_USES_CLEARTEXT_TRAFFIC = 134217728;
    public static final int FLAG_VM_SAFE_MODE = 16384;
    public static final int HIDDEN_API_ENFORCEMENT_BLACK = 3;
    public static final int HIDDEN_API_ENFORCEMENT_DARK_GREY_AND_BLACK = 2;
    public static final int HIDDEN_API_ENFORCEMENT_DEFAULT = -1;
    public static final int HIDDEN_API_ENFORCEMENT_JUST_WARN = 1;
    private static final int HIDDEN_API_ENFORCEMENT_MAX = 3;
    public static final int HIDDEN_API_ENFORCEMENT_NONE = 0;
    public static final String METADATA_PRELOADED_FONTS = "preloaded_fonts";
    public static final int PRIVATE_FLAG_ACTIVITIES_RESIZE_MODE_RESIZEABLE = 1024;
    public static final int PRIVATE_FLAG_ACTIVITIES_RESIZE_MODE_RESIZEABLE_VIA_SDK_VERSION = 4096;
    public static final int PRIVATE_FLAG_ACTIVITIES_RESIZE_MODE_UNRESIZEABLE = 2048;
    public static final int PRIVATE_FLAG_BACKUP_IN_FOREGROUND = 8192;
    public static final int PRIVATE_FLAG_CANT_SAVE_STATE = 2;
    public static final int PRIVATE_FLAG_DEFAULT_TO_DEVICE_PROTECTED_STORAGE = 32;
    public static final int PRIVATE_FLAG_DIRECT_BOOT_AWARE = 64;
    public static final int PRIVATE_FLAG_FORWARD_LOCK = 4;
    public static final int PRIVATE_FLAG_HAS_DOMAIN_URLS = 16;
    public static final int PRIVATE_FLAG_HIDDEN = 1;
    public static final int PRIVATE_FLAG_INSTANT = 128;
    public static final int PRIVATE_FLAG_ISOLATED_SPLIT_LOADING = 32768;
    public static final int PRIVATE_FLAG_OEM = 131072;
    public static final int PRIVATE_FLAG_PARTIALLY_DIRECT_BOOT_AWARE = 256;
    public static final int PRIVATE_FLAG_PRIVILEGED = 8;
    public static final int PRIVATE_FLAG_PRODUCT = 524288;
    public static final int PRIVATE_FLAG_REQUIRED_FOR_SYSTEM_USER = 512;
    public static final int PRIVATE_FLAG_SIGNED_WITH_PLATFORM_KEY = 1048576;
    public static final int PRIVATE_FLAG_STATIC_SHARED_LIBRARY = 16384;
    public static final int PRIVATE_FLAG_VENDOR = 262144;
    public static final int PRIVATE_FLAG_VIRTUAL_PRELOAD = 65536;
    public String appComponentFactory;
    public String backupAgentName;
    public int category;
    public String classLoaderName;
    public String className;
    public int compatibleWidthLimitDp;
    public int compileSdkVersion;
    public String compileSdkVersionCodename;

    @SystemApi
    public String credentialProtectedDataDir;
    public String dataDir;
    public int descriptionRes;
    public String deviceProtectedDataDir;
    public boolean enabled;
    public int enabledSetting;
    public int flags;
    public int fullBackupContent;
    public int installLocation;
    public int largestWidthLimitDp;
    public long longVersionCode;
    private int mHiddenApiPolicy;
    public String manageSpaceActivityName;
    public float maxAspectRatio;
    public int minSdkVersion;
    public String nativeLibraryDir;
    public String nativeLibraryRootDir;
    public boolean nativeLibraryRootRequiresIsa;
    public int networkSecurityConfigRes;
    public String permission;
    public String primaryCpuAbi;
    public int privateFlags;
    public String processName;
    public String publicSourceDir;
    public int requiresSmallestWidthDp;
    public String[] resourceDirs;
    public String scanPublicSourceDir;
    public String scanSourceDir;
    public String seInfo;
    public String seInfoUser;
    public String secondaryCpuAbi;
    public String secondaryNativeLibraryDir;
    public String[] sharedLibraryFiles;
    public String sourceDir;
    public String[] splitClassLoaderNames;
    public SparseArray<int[]> splitDependencies;
    public String[] splitNames;
    public String[] splitPublicSourceDirs;
    public String[] splitSourceDirs;
    public UUID storageUuid;

    @SystemApi
    public int targetSandboxVersion;
    public int targetSdkVersion;
    public String taskAffinity;
    public int theme;
    public int uiOptions;
    public int uid;

    @Deprecated
    public int versionCode;

    @Deprecated
    public String volumeUuid;

    @Retention(RetentionPolicy.SOURCE)
    public @interface ApplicationInfoPrivateFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Category {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface HiddenApiEnforcementPolicy {
    }

    public static CharSequence getCategoryTitle(Context context, int i) {
        switch (i) {
            case 0:
                return context.getText(R.string.app_category_game);
            case 1:
                return context.getText(R.string.app_category_audio);
            case 2:
                return context.getText(R.string.app_category_video);
            case 3:
                return context.getText(R.string.app_category_image);
            case 4:
                return context.getText(R.string.app_category_social);
            case 5:
                return context.getText(R.string.app_category_news);
            case 6:
                return context.getText(R.string.app_category_maps);
            case 7:
                return context.getText(R.string.app_category_productivity);
            default:
                return null;
        }
    }

    public static boolean isValidHiddenApiEnforcementPolicy(int i) {
        return i >= -1 && i <= 3;
    }

    public void dump(Printer printer, String str) {
        dump(printer, str, 3);
    }

    public void dump(Printer printer, String str, int i) {
        super.dumpFront(printer, str);
        int i2 = i & 1;
        if (i2 != 0 && this.className != null) {
            printer.println(str + "className=" + this.className);
        }
        if (this.permission != null) {
            printer.println(str + "permission=" + this.permission);
        }
        printer.println(str + "processName=" + this.processName);
        if (i2 != 0) {
            printer.println(str + "taskAffinity=" + this.taskAffinity);
        }
        printer.println(str + "uid=" + this.uid + " flags=0x" + Integer.toHexString(this.flags) + " privateFlags=0x" + Integer.toHexString(this.privateFlags) + " theme=0x" + Integer.toHexString(this.theme));
        if (i2 != 0) {
            printer.println(str + "requiresSmallestWidthDp=" + this.requiresSmallestWidthDp + " compatibleWidthLimitDp=" + this.compatibleWidthLimitDp + " largestWidthLimitDp=" + this.largestWidthLimitDp);
        }
        printer.println(str + "sourceDir=" + this.sourceDir);
        if (!Objects.equals(this.sourceDir, this.publicSourceDir)) {
            printer.println(str + "publicSourceDir=" + this.publicSourceDir);
        }
        if (!ArrayUtils.isEmpty(this.splitSourceDirs)) {
            printer.println(str + "splitSourceDirs=" + Arrays.toString(this.splitSourceDirs));
        }
        if (!ArrayUtils.isEmpty(this.splitPublicSourceDirs) && !Arrays.equals(this.splitSourceDirs, this.splitPublicSourceDirs)) {
            printer.println(str + "splitPublicSourceDirs=" + Arrays.toString(this.splitPublicSourceDirs));
        }
        if (this.resourceDirs != null) {
            printer.println(str + "resourceDirs=" + Arrays.toString(this.resourceDirs));
        }
        if (i2 != 0 && this.seInfo != null) {
            printer.println(str + "seinfo=" + this.seInfo);
            printer.println(str + "seinfoUser=" + this.seInfoUser);
        }
        printer.println(str + "dataDir=" + this.dataDir);
        if (i2 != 0) {
            printer.println(str + "deviceProtectedDataDir=" + this.deviceProtectedDataDir);
            printer.println(str + "credentialProtectedDataDir=" + this.credentialProtectedDataDir);
            if (this.sharedLibraryFiles != null) {
                printer.println(str + "sharedLibraryFiles=" + Arrays.toString(this.sharedLibraryFiles));
            }
        }
        if (this.classLoaderName != null) {
            printer.println(str + "classLoaderName=" + this.classLoaderName);
        }
        if (!ArrayUtils.isEmpty(this.splitClassLoaderNames)) {
            printer.println(str + "splitClassLoaderNames=" + Arrays.toString(this.splitClassLoaderNames));
        }
        printer.println(str + "enabled=" + this.enabled + " minSdkVersion=" + this.minSdkVersion + " targetSdkVersion=" + this.targetSdkVersion + " versionCode=" + this.longVersionCode + " targetSandboxVersion=" + this.targetSandboxVersion);
        if (i2 != 0) {
            if (this.manageSpaceActivityName != null) {
                printer.println(str + "manageSpaceActivityName=" + this.manageSpaceActivityName);
            }
            if (this.descriptionRes != 0) {
                printer.println(str + "description=0x" + Integer.toHexString(this.descriptionRes));
            }
            if (this.uiOptions != 0) {
                printer.println(str + "uiOptions=0x" + Integer.toHexString(this.uiOptions));
            }
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append("supportsRtl=");
            sb.append(hasRtlSupport() ? "true" : "false");
            printer.println(sb.toString());
            if (this.fullBackupContent > 0) {
                printer.println(str + "fullBackupContent=@xml/" + this.fullBackupContent);
            } else {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(str);
                sb2.append("fullBackupContent=");
                sb2.append(this.fullBackupContent < 0 ? "false" : "true");
                printer.println(sb2.toString());
            }
            if (this.networkSecurityConfigRes != 0) {
                printer.println(str + "networkSecurityConfigRes=0x" + Integer.toHexString(this.networkSecurityConfigRes));
            }
            if (this.category != -1) {
                printer.println(str + "category=" + this.category);
            }
            printer.println(str + "HiddenApiEnforcementPolicy=" + getHiddenApiEnforcementPolicy());
        }
        super.dumpBack(printer, str);
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j, int i) {
        long jStart = protoOutputStream.start(j);
        super.writeToProto(protoOutputStream, 1146756268033L);
        protoOutputStream.write(1138166333442L, this.permission);
        protoOutputStream.write(1138166333443L, this.processName);
        protoOutputStream.write(1120986464260L, this.uid);
        protoOutputStream.write(1120986464261L, this.flags);
        protoOutputStream.write(1120986464262L, this.privateFlags);
        protoOutputStream.write(1120986464263L, this.theme);
        protoOutputStream.write(1138166333448L, this.sourceDir);
        if (!Objects.equals(this.sourceDir, this.publicSourceDir)) {
            protoOutputStream.write(1138166333449L, this.publicSourceDir);
        }
        if (!ArrayUtils.isEmpty(this.splitSourceDirs)) {
            for (String str : this.splitSourceDirs) {
                protoOutputStream.write(2237677961226L, str);
            }
        }
        if (!ArrayUtils.isEmpty(this.splitPublicSourceDirs) && !Arrays.equals(this.splitSourceDirs, this.splitPublicSourceDirs)) {
            for (String str2 : this.splitPublicSourceDirs) {
                protoOutputStream.write(2237677961227L, str2);
            }
        }
        if (this.resourceDirs != null) {
            for (String str3 : this.resourceDirs) {
                protoOutputStream.write(2237677961228L, str3);
            }
        }
        protoOutputStream.write(1138166333453L, this.dataDir);
        protoOutputStream.write(1138166333454L, this.classLoaderName);
        if (!ArrayUtils.isEmpty(this.splitClassLoaderNames)) {
            for (String str4 : this.splitClassLoaderNames) {
                protoOutputStream.write(ApplicationInfoProto.SPLIT_CLASS_LOADER_NAMES, str4);
            }
        }
        long jStart2 = protoOutputStream.start(1146756268048L);
        protoOutputStream.write(1133871366145L, this.enabled);
        protoOutputStream.write(1120986464258L, this.minSdkVersion);
        protoOutputStream.write(1120986464259L, this.targetSdkVersion);
        protoOutputStream.write(1120986464260L, this.longVersionCode);
        protoOutputStream.write(1120986464261L, this.targetSandboxVersion);
        protoOutputStream.end(jStart2);
        if ((i & 1) != 0) {
            long jStart3 = protoOutputStream.start(1146756268049L);
            if (this.className != null) {
                protoOutputStream.write(1138166333441L, this.className);
            }
            protoOutputStream.write(1138166333442L, this.taskAffinity);
            protoOutputStream.write(1120986464259L, this.requiresSmallestWidthDp);
            protoOutputStream.write(1120986464260L, this.compatibleWidthLimitDp);
            protoOutputStream.write(1120986464261L, this.largestWidthLimitDp);
            if (this.seInfo != null) {
                protoOutputStream.write(1138166333446L, this.seInfo);
                protoOutputStream.write(1138166333447L, this.seInfoUser);
            }
            protoOutputStream.write(1138166333448L, this.deviceProtectedDataDir);
            protoOutputStream.write(1138166333449L, this.credentialProtectedDataDir);
            if (this.sharedLibraryFiles != null) {
                for (String str5 : this.sharedLibraryFiles) {
                    protoOutputStream.write(2237677961226L, str5);
                }
            }
            if (this.manageSpaceActivityName != null) {
                protoOutputStream.write(1138166333451L, this.manageSpaceActivityName);
            }
            if (this.descriptionRes != 0) {
                protoOutputStream.write(1120986464268L, this.descriptionRes);
            }
            if (this.uiOptions != 0) {
                protoOutputStream.write(1120986464269L, this.uiOptions);
            }
            protoOutputStream.write(1133871366158L, hasRtlSupport());
            if (this.fullBackupContent > 0) {
                protoOutputStream.write(1138166333455L, "@xml/" + this.fullBackupContent);
            } else {
                protoOutputStream.write(1133871366160L, this.fullBackupContent == 0);
            }
            if (this.networkSecurityConfigRes != 0) {
                protoOutputStream.write(1120986464273L, this.networkSecurityConfigRes);
            }
            if (this.category != -1) {
                protoOutputStream.write(1120986464274L, this.category);
            }
            protoOutputStream.end(jStart3);
        }
        protoOutputStream.end(jStart);
    }

    public boolean hasRtlSupport() {
        return (this.flags & 4194304) == 4194304;
    }

    public boolean hasCode() {
        return (this.flags & 4) != 0;
    }

    public static class DisplayNameComparator implements Comparator<ApplicationInfo> {
        private PackageManager mPM;
        private final Collator sCollator = Collator.getInstance();

        public DisplayNameComparator(PackageManager packageManager) {
            this.mPM = packageManager;
        }

        @Override
        public final int compare(ApplicationInfo applicationInfo, ApplicationInfo applicationInfo2) {
            CharSequence applicationLabel = this.mPM.getApplicationLabel(applicationInfo);
            if (applicationLabel == null) {
                applicationLabel = applicationInfo.packageName;
            }
            CharSequence applicationLabel2 = this.mPM.getApplicationLabel(applicationInfo2);
            if (applicationLabel2 == null) {
                applicationLabel2 = applicationInfo2.packageName;
            }
            return this.sCollator.compare(applicationLabel.toString(), applicationLabel2.toString());
        }
    }

    public ApplicationInfo() {
        this.fullBackupContent = 0;
        this.uiOptions = 0;
        this.flags = 0;
        this.requiresSmallestWidthDp = 0;
        this.compatibleWidthLimitDp = 0;
        this.largestWidthLimitDp = 0;
        this.enabled = true;
        this.enabledSetting = 0;
        this.installLocation = -1;
        this.category = -1;
        this.mHiddenApiPolicy = -1;
    }

    public ApplicationInfo(ApplicationInfo applicationInfo) {
        super(applicationInfo);
        this.fullBackupContent = 0;
        this.uiOptions = 0;
        this.flags = 0;
        this.requiresSmallestWidthDp = 0;
        this.compatibleWidthLimitDp = 0;
        this.largestWidthLimitDp = 0;
        this.enabled = true;
        this.enabledSetting = 0;
        this.installLocation = -1;
        this.category = -1;
        this.mHiddenApiPolicy = -1;
        this.taskAffinity = applicationInfo.taskAffinity;
        this.permission = applicationInfo.permission;
        this.processName = applicationInfo.processName;
        this.className = applicationInfo.className;
        this.theme = applicationInfo.theme;
        this.flags = applicationInfo.flags;
        this.privateFlags = applicationInfo.privateFlags;
        this.requiresSmallestWidthDp = applicationInfo.requiresSmallestWidthDp;
        this.compatibleWidthLimitDp = applicationInfo.compatibleWidthLimitDp;
        this.largestWidthLimitDp = applicationInfo.largestWidthLimitDp;
        this.volumeUuid = applicationInfo.volumeUuid;
        this.storageUuid = applicationInfo.storageUuid;
        this.scanSourceDir = applicationInfo.scanSourceDir;
        this.scanPublicSourceDir = applicationInfo.scanPublicSourceDir;
        this.sourceDir = applicationInfo.sourceDir;
        this.publicSourceDir = applicationInfo.publicSourceDir;
        this.splitNames = applicationInfo.splitNames;
        this.splitSourceDirs = applicationInfo.splitSourceDirs;
        this.splitPublicSourceDirs = applicationInfo.splitPublicSourceDirs;
        this.splitDependencies = applicationInfo.splitDependencies;
        this.nativeLibraryDir = applicationInfo.nativeLibraryDir;
        this.secondaryNativeLibraryDir = applicationInfo.secondaryNativeLibraryDir;
        this.nativeLibraryRootDir = applicationInfo.nativeLibraryRootDir;
        this.nativeLibraryRootRequiresIsa = applicationInfo.nativeLibraryRootRequiresIsa;
        this.primaryCpuAbi = applicationInfo.primaryCpuAbi;
        this.secondaryCpuAbi = applicationInfo.secondaryCpuAbi;
        this.resourceDirs = applicationInfo.resourceDirs;
        this.seInfo = applicationInfo.seInfo;
        this.seInfoUser = applicationInfo.seInfoUser;
        this.sharedLibraryFiles = applicationInfo.sharedLibraryFiles;
        this.dataDir = applicationInfo.dataDir;
        this.deviceProtectedDataDir = applicationInfo.deviceProtectedDataDir;
        this.credentialProtectedDataDir = applicationInfo.credentialProtectedDataDir;
        this.uid = applicationInfo.uid;
        this.minSdkVersion = applicationInfo.minSdkVersion;
        this.targetSdkVersion = applicationInfo.targetSdkVersion;
        setVersionCode(applicationInfo.longVersionCode);
        this.enabled = applicationInfo.enabled;
        this.enabledSetting = applicationInfo.enabledSetting;
        this.installLocation = applicationInfo.installLocation;
        this.manageSpaceActivityName = applicationInfo.manageSpaceActivityName;
        this.descriptionRes = applicationInfo.descriptionRes;
        this.uiOptions = applicationInfo.uiOptions;
        this.backupAgentName = applicationInfo.backupAgentName;
        this.fullBackupContent = applicationInfo.fullBackupContent;
        this.networkSecurityConfigRes = applicationInfo.networkSecurityConfigRes;
        this.category = applicationInfo.category;
        this.targetSandboxVersion = applicationInfo.targetSandboxVersion;
        this.classLoaderName = applicationInfo.classLoaderName;
        this.splitClassLoaderNames = applicationInfo.splitClassLoaderNames;
        this.appComponentFactory = applicationInfo.appComponentFactory;
        this.compileSdkVersion = applicationInfo.compileSdkVersion;
        this.compileSdkVersionCodename = applicationInfo.compileSdkVersionCodename;
        this.mHiddenApiPolicy = applicationInfo.mHiddenApiPolicy;
    }

    public String toString() {
        return "ApplicationInfo{" + Integer.toHexString(System.identityHashCode(this)) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.packageName + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeString(this.taskAffinity);
        parcel.writeString(this.permission);
        parcel.writeString(this.processName);
        parcel.writeString(this.className);
        parcel.writeInt(this.theme);
        parcel.writeInt(this.flags);
        parcel.writeInt(this.privateFlags);
        parcel.writeInt(this.requiresSmallestWidthDp);
        parcel.writeInt(this.compatibleWidthLimitDp);
        parcel.writeInt(this.largestWidthLimitDp);
        if (this.storageUuid != null) {
            parcel.writeInt(1);
            parcel.writeLong(this.storageUuid.getMostSignificantBits());
            parcel.writeLong(this.storageUuid.getLeastSignificantBits());
        } else {
            parcel.writeInt(0);
        }
        parcel.writeString(this.scanSourceDir);
        parcel.writeString(this.scanPublicSourceDir);
        parcel.writeString(this.sourceDir);
        parcel.writeString(this.publicSourceDir);
        parcel.writeStringArray(this.splitNames);
        parcel.writeStringArray(this.splitSourceDirs);
        parcel.writeStringArray(this.splitPublicSourceDirs);
        parcel.writeSparseArray(this.splitDependencies);
        parcel.writeString(this.nativeLibraryDir);
        parcel.writeString(this.secondaryNativeLibraryDir);
        parcel.writeString(this.nativeLibraryRootDir);
        parcel.writeInt(this.nativeLibraryRootRequiresIsa ? 1 : 0);
        parcel.writeString(this.primaryCpuAbi);
        parcel.writeString(this.secondaryCpuAbi);
        parcel.writeStringArray(this.resourceDirs);
        parcel.writeString(this.seInfo);
        parcel.writeString(this.seInfoUser);
        parcel.writeStringArray(this.sharedLibraryFiles);
        parcel.writeString(this.dataDir);
        parcel.writeString(this.deviceProtectedDataDir);
        parcel.writeString(this.credentialProtectedDataDir);
        parcel.writeInt(this.uid);
        parcel.writeInt(this.minSdkVersion);
        parcel.writeInt(this.targetSdkVersion);
        parcel.writeLong(this.longVersionCode);
        parcel.writeInt(this.enabled ? 1 : 0);
        parcel.writeInt(this.enabledSetting);
        parcel.writeInt(this.installLocation);
        parcel.writeString(this.manageSpaceActivityName);
        parcel.writeString(this.backupAgentName);
        parcel.writeInt(this.descriptionRes);
        parcel.writeInt(this.uiOptions);
        parcel.writeInt(this.fullBackupContent);
        parcel.writeInt(this.networkSecurityConfigRes);
        parcel.writeInt(this.category);
        parcel.writeInt(this.targetSandboxVersion);
        parcel.writeString(this.classLoaderName);
        parcel.writeStringArray(this.splitClassLoaderNames);
        parcel.writeInt(this.compileSdkVersion);
        parcel.writeString(this.compileSdkVersionCodename);
        parcel.writeString(this.appComponentFactory);
        parcel.writeInt(this.mHiddenApiPolicy);
    }

    private ApplicationInfo(Parcel parcel) {
        boolean z;
        super(parcel);
        this.fullBackupContent = 0;
        this.uiOptions = 0;
        this.flags = 0;
        this.requiresSmallestWidthDp = 0;
        this.compatibleWidthLimitDp = 0;
        this.largestWidthLimitDp = 0;
        this.enabled = true;
        this.enabledSetting = 0;
        this.installLocation = -1;
        this.category = -1;
        this.mHiddenApiPolicy = -1;
        this.taskAffinity = parcel.readString();
        this.permission = parcel.readString();
        this.processName = parcel.readString();
        this.className = parcel.readString();
        this.theme = parcel.readInt();
        this.flags = parcel.readInt();
        this.privateFlags = parcel.readInt();
        this.requiresSmallestWidthDp = parcel.readInt();
        this.compatibleWidthLimitDp = parcel.readInt();
        this.largestWidthLimitDp = parcel.readInt();
        if (parcel.readInt() != 0) {
            this.storageUuid = new UUID(parcel.readLong(), parcel.readLong());
            this.volumeUuid = StorageManager.convert(this.storageUuid);
        }
        this.scanSourceDir = parcel.readString();
        this.scanPublicSourceDir = parcel.readString();
        this.sourceDir = parcel.readString();
        this.publicSourceDir = parcel.readString();
        this.splitNames = parcel.readStringArray();
        this.splitSourceDirs = parcel.readStringArray();
        this.splitPublicSourceDirs = parcel.readStringArray();
        this.splitDependencies = parcel.readSparseArray(null);
        this.nativeLibraryDir = parcel.readString();
        this.secondaryNativeLibraryDir = parcel.readString();
        this.nativeLibraryRootDir = parcel.readString();
        if (parcel.readInt() == 0) {
            z = false;
        } else {
            z = true;
        }
        this.nativeLibraryRootRequiresIsa = z;
        this.primaryCpuAbi = parcel.readString();
        this.secondaryCpuAbi = parcel.readString();
        this.resourceDirs = parcel.readStringArray();
        this.seInfo = parcel.readString();
        this.seInfoUser = parcel.readString();
        this.sharedLibraryFiles = parcel.readStringArray();
        this.dataDir = parcel.readString();
        this.deviceProtectedDataDir = parcel.readString();
        this.credentialProtectedDataDir = parcel.readString();
        this.uid = parcel.readInt();
        this.minSdkVersion = parcel.readInt();
        this.targetSdkVersion = parcel.readInt();
        setVersionCode(parcel.readLong());
        this.enabled = parcel.readInt() != 0;
        this.enabledSetting = parcel.readInt();
        this.installLocation = parcel.readInt();
        this.manageSpaceActivityName = parcel.readString();
        this.backupAgentName = parcel.readString();
        this.descriptionRes = parcel.readInt();
        this.uiOptions = parcel.readInt();
        this.fullBackupContent = parcel.readInt();
        this.networkSecurityConfigRes = parcel.readInt();
        this.category = parcel.readInt();
        this.targetSandboxVersion = parcel.readInt();
        this.classLoaderName = parcel.readString();
        this.splitClassLoaderNames = parcel.readStringArray();
        this.compileSdkVersion = parcel.readInt();
        this.compileSdkVersionCodename = parcel.readString();
        this.appComponentFactory = parcel.readString();
        this.mHiddenApiPolicy = parcel.readInt();
    }

    public CharSequence loadDescription(PackageManager packageManager) {
        CharSequence text;
        if (this.descriptionRes != 0 && (text = packageManager.getText(this.packageName, this.descriptionRes, this)) != null) {
            return text;
        }
        return null;
    }

    public void disableCompatibilityMode() {
        this.flags |= 540160;
    }

    public boolean usesCompatibilityMode() {
        return this.targetSdkVersion < 4 || (this.flags & 540160) == 0;
    }

    public void initForUser(int i) {
        this.uid = UserHandle.getUid(i, UserHandle.getAppId(this.uid));
        if (ZenModeConfig.SYSTEM_AUTHORITY.equals(this.packageName)) {
            this.dataDir = Environment.getDataSystemDirectory().getAbsolutePath();
            return;
        }
        this.deviceProtectedDataDir = Environment.getDataUserDePackageDirectory(this.volumeUuid, i, this.packageName).getAbsolutePath();
        this.credentialProtectedDataDir = Environment.getDataUserCePackageDirectory(this.volumeUuid, i, this.packageName).getAbsolutePath();
        if ((this.privateFlags & 32) != 0) {
            this.dataDir = this.deviceProtectedDataDir;
        } else {
            this.dataDir = this.credentialProtectedDataDir;
        }
    }

    private boolean isPackageWhitelistedForHiddenApis() {
        return SystemConfig.getInstance().getHiddenApiWhitelistedApps().contains(this.packageName);
    }

    private boolean isAllowedToUseHiddenApis() {
        return isSignedWithPlatformKey() || (isPackageWhitelistedForHiddenApis() && (isSystemApp() || isUpdatedSystemApp()));
    }

    public int getHiddenApiEnforcementPolicy() {
        if (isAllowedToUseHiddenApis()) {
            return 0;
        }
        if (this.mHiddenApiPolicy != -1) {
            return this.mHiddenApiPolicy;
        }
        if (this.targetSdkVersion < 28) {
            return 3;
        }
        return 2;
    }

    public void setHiddenApiEnforcementPolicy(int i) {
        if (!isValidHiddenApiEnforcementPolicy(i)) {
            throw new IllegalArgumentException("Invalid API enforcement policy: " + i);
        }
        this.mHiddenApiPolicy = i;
    }

    public void maybeUpdateHiddenApiEnforcementPolicy(int i, int i2) {
        if (isPackageWhitelistedForHiddenApis()) {
            return;
        }
        if (this.targetSdkVersion < 28) {
            setHiddenApiEnforcementPolicy(i);
        } else if (this.targetSdkVersion >= 28) {
            setHiddenApiEnforcementPolicy(i2);
        }
    }

    public void setVersionCode(long j) {
        this.longVersionCode = j;
        this.versionCode = (int) j;
    }

    @Override
    public Drawable loadDefaultIcon(PackageManager packageManager) {
        if ((this.flags & 262144) != 0 && isPackageUnavailable(packageManager)) {
            return Resources.getSystem().getDrawable(R.drawable.sym_app_on_sd_unavailable_icon);
        }
        return packageManager.getDefaultActivityIcon();
    }

    private boolean isPackageUnavailable(PackageManager packageManager) {
        try {
            return packageManager.getPackageInfo(this.packageName, 0) == null;
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }

    public boolean isDefaultToDeviceProtectedStorage() {
        return (this.privateFlags & 32) != 0;
    }

    public boolean isDirectBootAware() {
        return (this.privateFlags & 64) != 0;
    }

    public boolean isEncryptionAware() {
        return isDirectBootAware() || isPartiallyDirectBootAware();
    }

    public boolean isExternal() {
        return (this.flags & 262144) != 0;
    }

    public boolean isExternalAsec() {
        return TextUtils.isEmpty(this.volumeUuid) && isExternal();
    }

    public boolean isForwardLocked() {
        return (this.privateFlags & 4) != 0;
    }

    @SystemApi
    public boolean isInstantApp() {
        return (this.privateFlags & 128) != 0;
    }

    public boolean isInternal() {
        return (this.flags & 262144) == 0;
    }

    public boolean isOem() {
        return (this.privateFlags & 131072) != 0;
    }

    public boolean isPartiallyDirectBootAware() {
        return (this.privateFlags & 256) != 0;
    }

    public boolean isSignedWithPlatformKey() {
        return (this.privateFlags & 1048576) != 0;
    }

    public boolean isPrivilegedApp() {
        return (this.privateFlags & 8) != 0;
    }

    public boolean isRequiredForSystemUser() {
        return (this.privateFlags & 512) != 0;
    }

    public boolean isStaticSharedLibrary() {
        return (this.privateFlags & 16384) != 0;
    }

    public boolean isSystemApp() {
        return (this.flags & 1) != 0;
    }

    public boolean isUpdatedSystemApp() {
        return (this.flags & 128) != 0;
    }

    public boolean isVendor() {
        return (this.privateFlags & 262144) != 0;
    }

    public boolean isProduct() {
        return (this.privateFlags & 524288) != 0;
    }

    public boolean isVirtualPreload() {
        return (this.privateFlags & 65536) != 0;
    }

    public boolean requestsIsolatedSplitLoading() {
        return (this.privateFlags & 32768) != 0;
    }

    @Override
    protected ApplicationInfo getApplicationInfo() {
        return this;
    }

    public void setCodePath(String str) {
        this.scanSourceDir = str;
    }

    public void setBaseCodePath(String str) {
        this.sourceDir = str;
    }

    public void setSplitCodePaths(String[] strArr) {
        this.splitSourceDirs = strArr;
    }

    public void setResourcePath(String str) {
        this.scanPublicSourceDir = str;
    }

    public void setBaseResourcePath(String str) {
        this.publicSourceDir = str;
    }

    public void setSplitResourcePaths(String[] strArr) {
        this.splitPublicSourceDirs = strArr;
    }

    public String getCodePath() {
        return this.scanSourceDir;
    }

    public String getBaseCodePath() {
        return this.sourceDir;
    }

    public String[] getSplitCodePaths() {
        return this.splitSourceDirs;
    }

    public String getResourcePath() {
        return this.scanPublicSourceDir;
    }

    public String getBaseResourcePath() {
        return this.publicSourceDir;
    }

    public String[] getSplitResourcePaths() {
        return this.splitPublicSourceDirs;
    }
}
