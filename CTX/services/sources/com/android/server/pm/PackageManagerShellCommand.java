package com.android.server.pm;

import android.accounts.IAccountManager;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.content.ComponentName;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageInstaller;
import android.content.pm.IPackageManager;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.ParceledListSlice;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.pm.VersionedPackage;
import android.content.pm.dex.DexMetadataHelper;
import android.content.pm.dex.ISnapshotRuntimeProfileCallback;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;
import android.net.util.NetworkConstants;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IUserManager;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.PrintWriterPrinter;
import com.android.internal.content.PackageHelper;
import com.android.internal.util.ArrayUtils;
import com.android.server.BatteryService;
import com.android.server.LocalServices;
import com.android.server.SystemConfig;
import com.android.server.UiModeManagerService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.backup.internal.BackupHandler;
import com.android.server.display.DisplayTransformManager;
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.server.net.NetworkPolicyManagerService;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.usb.descriptors.UsbACInterface;
import com.android.server.usb.descriptors.UsbDescriptor;
import com.android.server.wm.WindowManagerService;
import dalvik.system.DexFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import libcore.io.IoUtils;
import libcore.io.Streams;

class PackageManagerShellCommand extends ShellCommand {
    private static final String ART_PROFILE_SNAPSHOT_DEBUG_LOCATION = "/data/misc/profman/";
    private static final String STDIN_PATH = "-";
    boolean mBrief;
    boolean mComponents;
    final IPackageManager mInterface;
    private final WeakHashMap<String, Resources> mResourceCache = new WeakHashMap<>();
    int mTargetUser;

    PackageManagerShellCommand(PackageManagerService packageManagerService) {
        this.mInterface = packageManagerService;
    }

    public int onCommand(String str) {
        byte b;
        if (str == null) {
            return handleDefaultCommands(str);
        }
        PrintWriter outPrintWriter = getOutPrintWriter();
        try {
            switch (str.hashCode()) {
                case -2102802879:
                    b = !str.equals("set-harmful-app-warning") ? (byte) -1 : (byte) 55;
                    break;
                case -1967190973:
                    if (str.equals("install-abandon")) {
                        b = 8;
                        break;
                    }
                    break;
                case -1937348290:
                    if (str.equals("get-install-location")) {
                        b = UsbDescriptor.DESCRIPTORTYPE_CAPABILITY;
                        break;
                    }
                    break;
                case -1852006340:
                    if (str.equals("suspend")) {
                        b = UsbDescriptor.DESCRIPTORTYPE_REPORT;
                        break;
                    }
                    break;
                case -1846646502:
                    if (str.equals("get-max-running-users")) {
                        b = 50;
                        break;
                    }
                    break;
                case -1741208611:
                    if (str.equals("set-installer")) {
                        b = 52;
                        break;
                    }
                    break;
                case -1347307837:
                    if (str.equals("has-feature")) {
                        b = 54;
                        break;
                    }
                    break;
                case -1298848381:
                    if (str.equals("enable")) {
                        b = 27;
                        break;
                    }
                    break;
                case -1267782244:
                    if (str.equals("get-instantapp-resolver")) {
                        b = 53;
                        break;
                    }
                    break;
                case -1231004208:
                    if (str.equals("resolve-activity")) {
                        b = 3;
                        break;
                    }
                    break;
                case -1102348235:
                    if (str.equals("get-privapp-deny-permissions")) {
                        b = UsbDescriptor.DESCRIPTORTYPE_HUB;
                        break;
                    }
                    break;
                case -1091400553:
                    if (str.equals("get-oem-permissions")) {
                        b = UsbDescriptor.DESCRIPTORTYPE_SUPERSPEED_HUB;
                        break;
                    }
                    break;
                case -1070704814:
                    if (str.equals("get-privapp-permissions")) {
                        b = 40;
                        break;
                    }
                    break;
                case -1032029296:
                    if (str.equals("disable-user")) {
                        b = 29;
                        break;
                    }
                    break;
                case -934343034:
                    if (str.equals("revoke")) {
                        b = UsbDescriptor.DESCRIPTORTYPE_AUDIO_ENDPOINT;
                        break;
                    }
                    break;
                case -919935069:
                    if (str.equals("dump-profiles")) {
                        b = 23;
                        break;
                    }
                    break;
                case -840566949:
                    if (str.equals("unhide")) {
                        b = UsbDescriptor.DESCRIPTORTYPE_HID;
                        break;
                    }
                    break;
                case -625596190:
                    if (str.equals("uninstall")) {
                        b = 25;
                        break;
                    }
                    break;
                case -623224643:
                    if (str.equals("get-app-link")) {
                        b = 44;
                        break;
                    }
                    break;
                case -539710980:
                    if (str.equals("create-user")) {
                        b = 46;
                        break;
                    }
                    break;
                case -458695741:
                    if (str.equals("query-services")) {
                        b = 5;
                        break;
                    }
                    break;
                case -444750796:
                    if (str.equals("bg-dexopt-job")) {
                        b = 22;
                        break;
                    }
                    break;
                case -440994401:
                    if (str.equals("query-receivers")) {
                        b = 6;
                        break;
                    }
                    break;
                case -339687564:
                    if (str.equals("remove-user")) {
                        b = 47;
                        break;
                    }
                    break;
                case -220055275:
                    if (str.equals("set-permission-enforced")) {
                        b = 39;
                        break;
                    }
                    break;
                case -140205181:
                    if (str.equals("unsuspend")) {
                        b = UsbDescriptor.DESCRIPTORTYPE_PHYSICAL;
                        break;
                    }
                    break;
                case -132384343:
                    if (str.equals("install-commit")) {
                        b = 10;
                        break;
                    }
                    break;
                case -129863314:
                    if (str.equals("install-create")) {
                        b = 11;
                        break;
                    }
                    break;
                case -115000827:
                    if (str.equals("default-state")) {
                        b = 31;
                        break;
                    }
                    break;
                case -87258188:
                    if (str.equals("move-primary-storage")) {
                        b = 18;
                        break;
                    }
                    break;
                case 3095028:
                    if (str.equals("dump")) {
                        b = 1;
                        break;
                    }
                    break;
                case 3202370:
                    if (str.equals("hide")) {
                        b = 32;
                        break;
                    }
                    break;
                case 3322014:
                    if (str.equals("list")) {
                        b = 2;
                        break;
                    }
                    break;
                case 3433509:
                    if (str.equals("path")) {
                        b = 0;
                        break;
                    }
                    break;
                case 18936394:
                    if (str.equals("move-package")) {
                        b = 17;
                        break;
                    }
                    break;
                case 86600360:
                    if (str.equals("get-max-users")) {
                        b = 49;
                        break;
                    }
                    break;
                case 94746189:
                    if (str.equals("clear")) {
                        b = 26;
                        break;
                    }
                    break;
                case 98615580:
                    if (str.equals("grant")) {
                        b = UsbDescriptor.DESCRIPTORTYPE_AUDIO_INTERFACE;
                        break;
                    }
                    break;
                case 107262333:
                    if (str.equals("install-existing")) {
                        b = 14;
                        break;
                    }
                    break;
                case 139892533:
                    if (str.equals("get-harmful-app-warning")) {
                        b = 56;
                        break;
                    }
                    break;
                case 287820022:
                    if (str.equals("install-remove")) {
                        b = 12;
                        break;
                    }
                    break;
                case 359572742:
                    if (str.equals("reset-permissions")) {
                        b = 38;
                        break;
                    }
                    break;
                case 467549856:
                    if (str.equals("snapshot-profile")) {
                        b = 24;
                        break;
                    }
                    break;
                case 798023112:
                    if (str.equals("install-destroy")) {
                        b = 9;
                        break;
                    }
                    break;
                case 826473335:
                    if (str.equals("uninstall-system-updates")) {
                        b = 57;
                        break;
                    }
                    break;
                case 925176533:
                    if (str.equals("set-user-restriction")) {
                        b = UsbDescriptor.DESCRIPTORTYPE_ENDPOINT_COMPANION;
                        break;
                    }
                    break;
                case 925767985:
                    if (str.equals("set-app-link")) {
                        b = 43;
                        break;
                    }
                    break;
                case 950491699:
                    if (str.equals("compile")) {
                        b = 19;
                        break;
                    }
                    break;
                case 1053409810:
                    if (str.equals("query-activities")) {
                        b = 4;
                        break;
                    }
                    break;
                case 1124603675:
                    if (str.equals("force-dex-opt")) {
                        b = 21;
                        break;
                    }
                    break;
                case 1177857340:
                    if (str.equals("trim-caches")) {
                        b = 45;
                        break;
                    }
                    break;
                case 1429366290:
                    if (str.equals("set-home-activity")) {
                        b = 51;
                        break;
                    }
                    break;
                case 1538306349:
                    if (str.equals("install-write")) {
                        b = UsbACInterface.ACI_SAMPLE_RATE_CONVERTER;
                        break;
                    }
                    break;
                case 1671308008:
                    if (str.equals("disable")) {
                        b = 28;
                        break;
                    }
                    break;
                case 1697997009:
                    if (str.equals("disable-until-used")) {
                        b = 30;
                        break;
                    }
                    break;
                case 1746695602:
                    if (str.equals("set-install-location")) {
                        b = UsbDescriptor.DESCRIPTORTYPE_BOS;
                        break;
                    }
                    break;
                case 1783979817:
                    if (str.equals("reconcile-secondary-dex-files")) {
                        b = 20;
                        break;
                    }
                    break;
                case 1957569947:
                    if (str.equals("install")) {
                        b = 7;
                        break;
                    }
                    break;
                default:
                    break;
            }
            switch (b) {
                case 0:
                    return runPath();
                case 1:
                    return runDump();
                case 2:
                    return runList();
                case 3:
                    return runResolveActivity();
                case 4:
                    return runQueryIntentActivities();
                case 5:
                    return runQueryIntentServices();
                case 6:
                    return runQueryIntentReceivers();
                case 7:
                    return runInstall();
                case 8:
                case 9:
                    return runInstallAbandon();
                case 10:
                    return runInstallCommit();
                case 11:
                    return runInstallCreate();
                case 12:
                    return runInstallRemove();
                case 13:
                    return runInstallWrite();
                case 14:
                    return runInstallExisting();
                case 15:
                    return runSetInstallLocation();
                case 16:
                    return runGetInstallLocation();
                case 17:
                    return runMovePackage();
                case 18:
                    return runMovePrimaryStorage();
                case WindowManagerService.H.REPORT_WINDOWS_CHANGE:
                    return runCompile();
                case 20:
                    return runreconcileSecondaryDexFiles();
                case BackupHandler.MSG_OP_COMPLETE:
                    return runForceDexOpt();
                case WindowManagerService.H.REPORT_HARD_KEYBOARD_STATUS_CHANGE:
                    return runDexoptJob();
                case WindowManagerService.H.BOOT_TIMEOUT:
                    return runDumpProfiles();
                case 24:
                    return runSnapshotProfile();
                case WindowManagerService.H.SHOW_STRICT_MODE_VIOLATION:
                    return runUninstall();
                case WindowManagerService.H.DO_ANIMATION_CALLBACK:
                    return runClear();
                case 27:
                    return runSetEnabledSetting(1);
                case NetworkConstants.ARP_PAYLOAD_LEN:
                    return runSetEnabledSetting(2);
                case HdmiCecKeycode.CEC_KEYCODE_NUMBER_ENTRY_MODE:
                    return runSetEnabledSetting(3);
                case 30:
                    return runSetEnabledSetting(4);
                case HdmiCecKeycode.CEC_KEYCODE_NUMBER_12:
                    return runSetEnabledSetting(0);
                case 32:
                    return runSetHiddenSetting(true);
                case 33:
                    return runSetHiddenSetting(false);
                case 34:
                    return runSuspend(true);
                case 35:
                    return runSuspend(false);
                case 36:
                    return runGrantRevokePermission(true);
                case 37:
                    return runGrantRevokePermission(false);
                case 38:
                    return runResetPermissions();
                case 39:
                    return runSetPermissionEnforced();
                case 40:
                    return runGetPrivappPermissions();
                case 41:
                    return runGetPrivappDenyPermissions();
                case HdmiCecKeycode.CEC_KEYCODE_DOT:
                    return runGetOemPermissions();
                case HdmiCecKeycode.CEC_KEYCODE_ENTER:
                    return runSetAppLink();
                case HdmiCecKeycode.CEC_KEYCODE_CLEAR:
                    return runGetAppLink();
                case NetworkPolicyManagerService.TYPE_RAPID:
                    return runTrimCaches();
                case WindowManagerService.H.WINDOW_REPLACEMENT_TIMEOUT:
                    return runCreateUser();
                case 47:
                    return runRemoveUser();
                case 48:
                    return runSetUserRestriction();
                case 49:
                    return runGetMaxUsers();
                case HdmiCecKeycode.CEC_KEYCODE_PREVIOUS_CHANNEL:
                    return runGetMaxRunningUsers();
                case 51:
                    return runSetHomeActivity();
                case 52:
                    return runSetInstaller();
                case 53:
                    return runGetInstantAppResolver();
                case 54:
                    return runHasFeature();
                case 55:
                    return runSetHarmfulAppWarning();
                case 56:
                    return runGetHarmfulAppWarning();
                case WindowManagerService.H.NOTIFY_KEYGUARD_TRUSTED_CHANGED:
                    return uninstallSystemUpdates();
                default:
                    String nextArg = getNextArg();
                    if (nextArg == null) {
                        if (str.equalsIgnoreCase("-l")) {
                            return runListPackages(false);
                        }
                        if (str.equalsIgnoreCase("-lf")) {
                            return runListPackages(true);
                        }
                    } else if (getNextArg() == null && str.equalsIgnoreCase("-p")) {
                        return displayPackageFilePath(nextArg, 0);
                    }
                    return handleDefaultCommands(str);
            }
        } catch (RemoteException e) {
            outPrintWriter.println("Remote exception: " + e);
            return -1;
        }
    }

    private int uninstallSystemUpdates() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        LinkedList linkedList = new LinkedList();
        try {
            ParceledListSlice installedApplications = this.mInterface.getInstalledApplications(DumpState.DUMP_DEXOPT, 0);
            IPackageInstaller packageInstaller = this.mInterface.getPackageInstaller();
            for (ApplicationInfo applicationInfo : installedApplications.getList()) {
                if (applicationInfo.isUpdatedSystemApp()) {
                    outPrintWriter.println("Uninstalling updates to " + applicationInfo.packageName + "...");
                    LocalIntentReceiver localIntentReceiver = new LocalIntentReceiver();
                    packageInstaller.uninstall(new VersionedPackage(applicationInfo.packageName, applicationInfo.versionCode), (String) null, 0, localIntentReceiver.getIntentSender(), 0);
                    if (localIntentReceiver.getResult().getIntExtra("android.content.pm.extra.STATUS", 1) != 0) {
                        linkedList.add(applicationInfo.packageName);
                    }
                }
            }
            if (!linkedList.isEmpty()) {
                outPrintWriter.println("Failure [Couldn't uninstall packages: " + TextUtils.join(", ", linkedList) + "]");
                return 0;
            }
            outPrintWriter.println("Success");
            return 1;
        } catch (RemoteException e) {
            outPrintWriter.println("Failure [" + e.getClass().getName() + " - " + e.getMessage() + "]");
            return 0;
        }
    }

    private void setParamsSize(InstallParams installParams, String str) {
        if (installParams.sessionParams.sizeBytes == -1 && !STDIN_PATH.equals(str)) {
            ParcelFileDescriptor parcelFileDescriptorOpenFileForSystem = openFileForSystem(str, "r");
            if (parcelFileDescriptorOpenFileForSystem == null) {
                getErrPrintWriter().println("Error: Can't open file: " + str);
                throw new IllegalArgumentException("Error: Can't open file: " + str);
            }
            try {
                try {
                    installParams.sessionParams.setSize(PackageHelper.calculateInstalledSize(new PackageParser.PackageLite((String) null, PackageParser.parseApkLite(parcelFileDescriptorOpenFileForSystem.getFileDescriptor(), str, 0), (String[]) null, (boolean[]) null, (String[]) null, (String[]) null, (String[]) null, (int[]) null), installParams.sessionParams.abiOverride, parcelFileDescriptorOpenFileForSystem.getFileDescriptor()));
                    try {
                        parcelFileDescriptorOpenFileForSystem.close();
                    } catch (IOException e) {
                    }
                } catch (PackageParser.PackageParserException | IOException e2) {
                    getErrPrintWriter().println("Error: Failed to parse APK file: " + str);
                    throw new IllegalArgumentException("Error: Failed to parse APK file: " + str, e2);
                }
            } catch (Throwable th) {
                try {
                    parcelFileDescriptorOpenFileForSystem.close();
                } catch (IOException e3) {
                }
                throw th;
            }
        }
    }

    private int displayPackageFilePath(String str, int i) throws RemoteException {
        PackageInfo packageInfo = this.mInterface.getPackageInfo(str, 0, i);
        if (packageInfo != null && packageInfo.applicationInfo != null) {
            PrintWriter outPrintWriter = getOutPrintWriter();
            outPrintWriter.print("package:");
            outPrintWriter.println(packageInfo.applicationInfo.sourceDir);
            if (!ArrayUtils.isEmpty(packageInfo.applicationInfo.splitSourceDirs)) {
                for (String str2 : packageInfo.applicationInfo.splitSourceDirs) {
                    outPrintWriter.print("package:");
                    outPrintWriter.println(str2);
                }
            }
            return 0;
        }
        return 1;
    }

    private int runPath() throws RemoteException {
        int userArg;
        String nextOption = getNextOption();
        if (nextOption != null && nextOption.equals("--user")) {
            userArg = UserHandle.parseUserArg(getNextArgRequired());
        } else {
            userArg = 0;
        }
        String nextArgRequired = getNextArgRequired();
        if (nextArgRequired == null) {
            getErrPrintWriter().println("Error: no package specified");
            return 1;
        }
        return displayPackageFilePath(nextArgRequired, userArg);
    }

    private int runList() throws RemoteException {
        PrintWriter outPrintWriter = getOutPrintWriter();
        String nextArg = getNextArg();
        if (nextArg != null) {
            switch (nextArg) {
                case "features":
                    break;
                case "instrumentation":
                    break;
                case "libraries":
                    break;
                case "package":
                case "packages":
                    break;
                case "permission-groups":
                    break;
                case "permissions":
                    break;
                case "users":
                    ServiceManager.getService("user").shellCommand(getInFileDescriptor(), getOutFileDescriptor(), getErrFileDescriptor(), new String[]{"list"}, getShellCallback(), adoptResultReceiver());
                    break;
                default:
                    outPrintWriter.println("Error: unknown list type '" + nextArg + "'");
                    break;
            }
            return -1;
        }
        outPrintWriter.println("Error: didn't specify type of data to list");
        return -1;
    }

    private int runListFeatures() throws RemoteException {
        PrintWriter outPrintWriter = getOutPrintWriter();
        List list = this.mInterface.getSystemAvailableFeatures().getList();
        Collections.sort(list, new Comparator<FeatureInfo>() {
            @Override
            public int compare(FeatureInfo featureInfo, FeatureInfo featureInfo2) {
                if (featureInfo.name == featureInfo2.name) {
                    return 0;
                }
                if (featureInfo.name == null) {
                    return -1;
                }
                if (featureInfo2.name == null) {
                    return 1;
                }
                return featureInfo.name.compareTo(featureInfo2.name);
            }
        });
        int size = list != null ? list.size() : 0;
        for (int i = 0; i < size; i++) {
            FeatureInfo featureInfo = (FeatureInfo) list.get(i);
            outPrintWriter.print("feature:");
            if (featureInfo.name != null) {
                outPrintWriter.print(featureInfo.name);
                if (featureInfo.version > 0) {
                    outPrintWriter.print("=");
                    outPrintWriter.print(featureInfo.version);
                }
                outPrintWriter.println();
            } else {
                outPrintWriter.println("reqGlEsVersion=0x" + Integer.toHexString(featureInfo.reqGlEsVersion));
            }
        }
        return 0;
    }

    private int runListInstrumentation() throws RemoteException {
        PrintWriter outPrintWriter = getOutPrintWriter();
        String str = null;
        boolean z = false;
        while (true) {
            try {
                String nextArg = getNextArg();
                if (nextArg != null) {
                    if (((nextArg.hashCode() == 1497 && nextArg.equals("-f")) ? (byte) 0 : (byte) -1) != 0) {
                        if (nextArg.charAt(0) == '-') {
                            outPrintWriter.println("Error: Unknown option: " + nextArg);
                            return -1;
                        }
                        str = nextArg;
                    } else {
                        z = true;
                    }
                } else {
                    List list = this.mInterface.queryInstrumentation(str, 0).getList();
                    Collections.sort(list, new Comparator<InstrumentationInfo>() {
                        @Override
                        public int compare(InstrumentationInfo instrumentationInfo, InstrumentationInfo instrumentationInfo2) {
                            return instrumentationInfo.targetPackage.compareTo(instrumentationInfo2.targetPackage);
                        }
                    });
                    int size = list != null ? list.size() : 0;
                    for (int i = 0; i < size; i++) {
                        InstrumentationInfo instrumentationInfo = (InstrumentationInfo) list.get(i);
                        outPrintWriter.print("instrumentation:");
                        if (z) {
                            outPrintWriter.print(instrumentationInfo.sourceDir);
                            outPrintWriter.print("=");
                        }
                        outPrintWriter.print(new ComponentName(instrumentationInfo.packageName, instrumentationInfo.name).flattenToShortString());
                        outPrintWriter.print(" (target=");
                        outPrintWriter.print(instrumentationInfo.targetPackage);
                        outPrintWriter.println(")");
                    }
                    return 0;
                }
            } catch (RuntimeException e) {
                outPrintWriter.println("Error: " + e.toString());
                return -1;
            }
        }
    }

    private int runListLibraries() throws RemoteException {
        PrintWriter outPrintWriter = getOutPrintWriter();
        ArrayList arrayList = new ArrayList();
        for (String str : this.mInterface.getSystemSharedLibraryNames()) {
            arrayList.add(str);
        }
        Collections.sort(arrayList, new Comparator<String>() {
            @Override
            public int compare(String str2, String str3) {
                if (str2 == str3) {
                    return 0;
                }
                if (str2 == null) {
                    return -1;
                }
                if (str3 == null) {
                    return 1;
                }
                return str2.compareTo(str3);
            }
        });
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            String str2 = (String) arrayList.get(i);
            outPrintWriter.print("library:");
            outPrintWriter.println(str2);
        }
        return 0;
    }

    private int runListPackages(boolean z) throws RemoteException {
        String nextOption;
        List list;
        String str;
        byte b;
        PrintWriter outPrintWriter = getOutPrintWriter();
        boolean z2 = z;
        int i = 0;
        int userArg = 0;
        int i2 = -1;
        boolean z3 = false;
        boolean z4 = false;
        boolean z5 = false;
        boolean z6 = false;
        boolean z7 = false;
        boolean z8 = false;
        boolean z9 = false;
        while (true) {
            try {
                nextOption = getNextOption();
            } catch (RuntimeException e) {
                outPrintWriter.println("Error: " + e.toString());
                return -1;
            }
            if (nextOption == null) {
                String nextArg = getNextArg();
                List list2 = this.mInterface.getInstalledPackages(i, userArg).getList();
                int size = list2.size();
                int i3 = 0;
                while (i3 < size) {
                    PackageInfo packageInfo = (PackageInfo) list2.get(i3);
                    if (nextArg != null) {
                        list = list2;
                        if (!packageInfo.packageName.contains(nextArg)) {
                            str = nextArg;
                        }
                        i3++;
                        list2 = list;
                        nextArg = str;
                    } else {
                        list = list2;
                    }
                    if (i2 == -1 || packageInfo.applicationInfo.uid == i2) {
                        boolean z10 = (packageInfo.applicationInfo.flags & 1) != 0;
                        if (z3) {
                            str = nextArg;
                            if (!packageInfo.applicationInfo.enabled) {
                            }
                        } else {
                            str = nextArg;
                        }
                        if ((!z4 || packageInfo.applicationInfo.enabled) && ((!z5 || z10) && (!z6 || !z10))) {
                            outPrintWriter.print("package:");
                            if (z2) {
                                outPrintWriter.print(packageInfo.applicationInfo.sourceDir);
                                outPrintWriter.print("=");
                            }
                            outPrintWriter.print(packageInfo.packageName);
                            if (z7) {
                                outPrintWriter.print(" versionCode:");
                                outPrintWriter.print(packageInfo.applicationInfo.versionCode);
                            }
                            if (z8) {
                                outPrintWriter.print("  installer=");
                                outPrintWriter.print(this.mInterface.getInstallerPackageName(packageInfo.packageName));
                            }
                            if (z9) {
                                outPrintWriter.print(" uid:");
                                outPrintWriter.print(packageInfo.applicationInfo.uid);
                            }
                            outPrintWriter.println();
                        }
                    }
                    i3++;
                    list2 = list;
                    nextArg = str;
                }
                return 0;
            }
            int iHashCode = nextOption.hashCode();
            if (iHashCode != -493830763) {
                if (iHashCode != 1446) {
                    if (iHashCode != 1480) {
                        if (iHashCode != 1500) {
                            if (iHashCode != 1503) {
                                if (iHashCode != 1510) {
                                    if (iHashCode != 1512) {
                                        if (iHashCode != 43014832) {
                                            if (iHashCode != 1333469547) {
                                                switch (iHashCode) {
                                                    case 1495:
                                                        b = !nextOption.equals("-d") ? (byte) -1 : (byte) 0;
                                                        break;
                                                    case 1496:
                                                        if (nextOption.equals("-e")) {
                                                            b = 1;
                                                            break;
                                                        }
                                                        break;
                                                    case 1497:
                                                        if (nextOption.equals("-f")) {
                                                            b = 2;
                                                            break;
                                                        }
                                                        break;
                                                    default:
                                                        break;
                                                }
                                            } else if (nextOption.equals("--user")) {
                                                b = 10;
                                            }
                                        } else if (nextOption.equals("--uid")) {
                                            b = 11;
                                        }
                                    } else if (nextOption.equals("-u")) {
                                        b = 7;
                                    }
                                } else if (nextOption.equals("-s")) {
                                    b = 5;
                                }
                            } else if (nextOption.equals("-l")) {
                                b = 4;
                            }
                        } else if (nextOption.equals("-i")) {
                            b = 3;
                        }
                    } else if (nextOption.equals("-U")) {
                        b = 6;
                    }
                } else if (nextOption.equals("-3")) {
                    b = 8;
                }
            } else if (nextOption.equals("--show-versioncode")) {
                b = 9;
            }
            switch (b) {
                case 0:
                    z3 = true;
                    break;
                case 1:
                    z4 = true;
                    break;
                case 2:
                    z2 = true;
                    break;
                case 3:
                    z8 = true;
                    break;
                case 4:
                    break;
                case 5:
                    z5 = true;
                    break;
                case 6:
                    z9 = true;
                    break;
                case 7:
                    i |= 8192;
                    break;
                case 8:
                    z6 = true;
                    break;
                case 9:
                    z7 = true;
                    break;
                case 10:
                    userArg = UserHandle.parseUserArg(getNextArgRequired());
                    break;
                case 11:
                    i2 = Integer.parseInt(getNextArgRequired());
                    z9 = true;
                    break;
                default:
                    outPrintWriter.println("Error: Unknown option: " + nextOption);
                    break;
            }
            return -1;
        }
    }

    private int runListPermissionGroups() throws RemoteException {
        PrintWriter outPrintWriter = getOutPrintWriter();
        List list = this.mInterface.getAllPermissionGroups(0).getList();
        int size = list.size();
        for (int i = 0; i < size; i++) {
            PermissionGroupInfo permissionGroupInfo = (PermissionGroupInfo) list.get(i);
            outPrintWriter.print("permission group:");
            outPrintWriter.println(permissionGroupInfo.name);
        }
        return 0;
    }

    private int runListPermissions() throws RemoteException {
        PrintWriter outPrintWriter = getOutPrintWriter();
        boolean z = false;
        boolean z2 = false;
        boolean z3 = false;
        boolean z4 = false;
        boolean z5 = false;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption == null) {
                ArrayList<String> arrayList = new ArrayList<>();
                if (z2) {
                    List list = this.mInterface.getAllPermissionGroups(0).getList();
                    int size = list.size();
                    for (int i = 0; i < size; i++) {
                        arrayList.add(((PermissionGroupInfo) list.get(i)).name);
                    }
                    arrayList.add(null);
                } else {
                    arrayList.add(getNextArg());
                }
                if (z) {
                    outPrintWriter.println("Dangerous Permissions:");
                    outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    doListPermissions(arrayList, z2, z3, z4, 1, 1);
                    if (z5) {
                        outPrintWriter.println("Normal Permissions:");
                        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                        doListPermissions(arrayList, z2, z3, z4, 0, 0);
                    }
                } else if (z5) {
                    outPrintWriter.println("Dangerous and Normal Permissions:");
                    outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    doListPermissions(arrayList, z2, z3, z4, 0, 1);
                } else {
                    outPrintWriter.println("All Permissions:");
                    outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    doListPermissions(arrayList, z2, z3, z4, -10000, 10000);
                }
                return 0;
            }
            byte b = -1;
            int iHashCode = nextOption.hashCode();
            if (iHashCode != 1495) {
                if (iHashCode != 1510) {
                    if (iHashCode != 1512) {
                        switch (iHashCode) {
                            case 1497:
                                if (nextOption.equals("-f")) {
                                    b = 1;
                                }
                                break;
                            case 1498:
                                if (nextOption.equals("-g")) {
                                    b = 2;
                                }
                                break;
                        }
                    } else if (nextOption.equals("-u")) {
                        b = 4;
                    }
                } else if (nextOption.equals("-s")) {
                    b = 3;
                }
            } else if (nextOption.equals("-d")) {
                b = 0;
            }
            switch (b) {
                case 0:
                    z = true;
                    break;
                case 1:
                    z3 = true;
                    break;
                case 2:
                    z2 = true;
                    break;
                case 3:
                    z2 = true;
                    z3 = true;
                    z4 = true;
                    break;
                case 4:
                    z5 = true;
                    break;
                default:
                    outPrintWriter.println("Error: Unknown option: " + nextOption);
                    return 1;
            }
        }
    }

    private Intent parseIntentAndUser() throws URISyntaxException {
        this.mTargetUser = -2;
        this.mBrief = false;
        this.mComponents = false;
        Intent commandArgs = Intent.parseCommandArgs(this, new Intent.CommandOptionHandler() {
            public boolean handleOption(String str, ShellCommand shellCommand) {
                if ("--user".equals(str)) {
                    PackageManagerShellCommand.this.mTargetUser = UserHandle.parseUserArg(shellCommand.getNextArgRequired());
                    return true;
                }
                if ("--brief".equals(str)) {
                    PackageManagerShellCommand.this.mBrief = true;
                    return true;
                }
                if ("--components".equals(str)) {
                    PackageManagerShellCommand.this.mComponents = true;
                    return true;
                }
                return false;
            }
        });
        this.mTargetUser = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), this.mTargetUser, false, false, null, null);
        return commandArgs;
    }

    private void printResolveInfo(PrintWriterPrinter printWriterPrinter, String str, ResolveInfo resolveInfo, boolean z, boolean z2) {
        ComponentName componentName;
        if (z || z2) {
            if (resolveInfo.activityInfo != null) {
                componentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
            } else if (resolveInfo.serviceInfo != null) {
                componentName = new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
            } else if (resolveInfo.providerInfo != null) {
                componentName = new ComponentName(resolveInfo.providerInfo.packageName, resolveInfo.providerInfo.name);
            } else {
                componentName = null;
            }
            if (componentName != null) {
                if (!z2) {
                    printWriterPrinter.println(str + "priority=" + resolveInfo.priority + " preferredOrder=" + resolveInfo.preferredOrder + " match=0x" + Integer.toHexString(resolveInfo.match) + " specificIndex=" + resolveInfo.specificIndex + " isDefault=" + resolveInfo.isDefault);
                }
                printWriterPrinter.println(str + componentName.flattenToShortString());
                return;
            }
        }
        resolveInfo.dump(printWriterPrinter, str);
    }

    private int runResolveActivity() {
        try {
            Intent intentAndUser = parseIntentAndUser();
            try {
                ResolveInfo resolveInfoResolveIntent = this.mInterface.resolveIntent(intentAndUser, intentAndUser.getType(), 0, this.mTargetUser);
                PrintWriter outPrintWriter = getOutPrintWriter();
                if (resolveInfoResolveIntent == null) {
                    outPrintWriter.println("No activity found");
                } else {
                    printResolveInfo(new PrintWriterPrinter(outPrintWriter), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, resolveInfoResolveIntent, this.mBrief, this.mComponents);
                }
                return 0;
            } catch (RemoteException e) {
                throw new RuntimeException("Failed calling service", e);
            }
        } catch (URISyntaxException e2) {
            throw new RuntimeException(e2.getMessage(), e2);
        }
    }

    private int runQueryIntentActivities() {
        try {
            Intent intentAndUser = parseIntentAndUser();
            try {
                List list = this.mInterface.queryIntentActivities(intentAndUser, intentAndUser.getType(), 0, this.mTargetUser).getList();
                PrintWriter outPrintWriter = getOutPrintWriter();
                if (list == null || list.size() <= 0) {
                    outPrintWriter.println("No activities found");
                } else if (!this.mComponents) {
                    outPrintWriter.print(list.size());
                    outPrintWriter.println(" activities found:");
                    PrintWriterPrinter printWriterPrinter = new PrintWriterPrinter(outPrintWriter);
                    for (int i = 0; i < list.size(); i++) {
                        outPrintWriter.print("  Activity #");
                        outPrintWriter.print(i);
                        outPrintWriter.println(":");
                        printResolveInfo(printWriterPrinter, "    ", (ResolveInfo) list.get(i), this.mBrief, this.mComponents);
                    }
                } else {
                    PrintWriterPrinter printWriterPrinter2 = new PrintWriterPrinter(outPrintWriter);
                    for (int i2 = 0; i2 < list.size(); i2++) {
                        printResolveInfo(printWriterPrinter2, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, (ResolveInfo) list.get(i2), this.mBrief, this.mComponents);
                    }
                }
                return 0;
            } catch (RemoteException e) {
                throw new RuntimeException("Failed calling service", e);
            }
        } catch (URISyntaxException e2) {
            throw new RuntimeException(e2.getMessage(), e2);
        }
    }

    private int runQueryIntentServices() {
        try {
            Intent intentAndUser = parseIntentAndUser();
            try {
                List list = this.mInterface.queryIntentServices(intentAndUser, intentAndUser.getType(), 0, this.mTargetUser).getList();
                PrintWriter outPrintWriter = getOutPrintWriter();
                if (list == null || list.size() <= 0) {
                    outPrintWriter.println("No services found");
                } else if (!this.mComponents) {
                    outPrintWriter.print(list.size());
                    outPrintWriter.println(" services found:");
                    PrintWriterPrinter printWriterPrinter = new PrintWriterPrinter(outPrintWriter);
                    for (int i = 0; i < list.size(); i++) {
                        outPrintWriter.print("  Service #");
                        outPrintWriter.print(i);
                        outPrintWriter.println(":");
                        printResolveInfo(printWriterPrinter, "    ", (ResolveInfo) list.get(i), this.mBrief, this.mComponents);
                    }
                } else {
                    PrintWriterPrinter printWriterPrinter2 = new PrintWriterPrinter(outPrintWriter);
                    for (int i2 = 0; i2 < list.size(); i2++) {
                        printResolveInfo(printWriterPrinter2, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, (ResolveInfo) list.get(i2), this.mBrief, this.mComponents);
                    }
                }
                return 0;
            } catch (RemoteException e) {
                throw new RuntimeException("Failed calling service", e);
            }
        } catch (URISyntaxException e2) {
            throw new RuntimeException(e2.getMessage(), e2);
        }
    }

    private int runQueryIntentReceivers() {
        try {
            Intent intentAndUser = parseIntentAndUser();
            try {
                List list = this.mInterface.queryIntentReceivers(intentAndUser, intentAndUser.getType(), 0, this.mTargetUser).getList();
                PrintWriter outPrintWriter = getOutPrintWriter();
                if (list == null || list.size() <= 0) {
                    outPrintWriter.println("No receivers found");
                } else if (!this.mComponents) {
                    outPrintWriter.print(list.size());
                    outPrintWriter.println(" receivers found:");
                    PrintWriterPrinter printWriterPrinter = new PrintWriterPrinter(outPrintWriter);
                    for (int i = 0; i < list.size(); i++) {
                        outPrintWriter.print("  Receiver #");
                        outPrintWriter.print(i);
                        outPrintWriter.println(":");
                        printResolveInfo(printWriterPrinter, "    ", (ResolveInfo) list.get(i), this.mBrief, this.mComponents);
                    }
                } else {
                    PrintWriterPrinter printWriterPrinter2 = new PrintWriterPrinter(outPrintWriter);
                    for (int i2 = 0; i2 < list.size(); i2++) {
                        printResolveInfo(printWriterPrinter2, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, (ResolveInfo) list.get(i2), this.mBrief, this.mComponents);
                    }
                }
                return 0;
            } catch (RemoteException e) {
                throw new RuntimeException("Failed calling service", e);
            }
        } catch (URISyntaxException e2) {
            throw new RuntimeException(e2.getMessage(), e2);
        }
    }

    private int runInstall() throws Throwable {
        PrintWriter outPrintWriter = getOutPrintWriter();
        InstallParams installParamsMakeInstallParams = makeInstallParams();
        String nextArg = getNextArg();
        setParamsSize(installParamsMakeInstallParams, nextArg);
        int iDoCreateSession = doCreateSession(installParamsMakeInstallParams.sessionParams, installParamsMakeInstallParams.installerPackageName, installParamsMakeInstallParams.userId);
        boolean z = true;
        if (nextArg == null) {
            try {
                if (installParamsMakeInstallParams.sessionParams.sizeBytes == -1) {
                    outPrintWriter.println("Error: must either specify a package size or an APK file");
                    try {
                        doAbandonSession(iDoCreateSession, false);
                    } catch (Exception e) {
                    }
                    return 1;
                }
                if (doWriteSplit(iDoCreateSession, nextArg, installParamsMakeInstallParams.sessionParams.sizeBytes, "base.apk", false) == 0) {
                    try {
                        doAbandonSession(iDoCreateSession, false);
                    } catch (Exception e2) {
                    }
                    return 1;
                }
                if (doCommitSession(iDoCreateSession, false) != 0) {
                    try {
                        doAbandonSession(iDoCreateSession, false);
                    } catch (Exception e3) {
                    }
                    return 1;
                }
                try {
                    outPrintWriter.println("Success");
                    return 0;
                } catch (Throwable th) {
                    th = th;
                    z = false;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } else if (doWriteSplit(iDoCreateSession, nextArg, installParamsMakeInstallParams.sessionParams.sizeBytes, "base.apk", false) == 0) {
        }
        if (z) {
            try {
                doAbandonSession(iDoCreateSession, false);
            } catch (Exception e4) {
            }
        }
        throw th;
    }

    private int runInstallAbandon() throws RemoteException {
        return doAbandonSession(Integer.parseInt(getNextArg()), true);
    }

    private int runInstallCommit() throws RemoteException {
        return doCommitSession(Integer.parseInt(getNextArg()), true);
    }

    private int runInstallCreate() throws RemoteException {
        PrintWriter outPrintWriter = getOutPrintWriter();
        InstallParams installParamsMakeInstallParams = makeInstallParams();
        outPrintWriter.println("Success: created install session [" + doCreateSession(installParamsMakeInstallParams.sessionParams, installParamsMakeInstallParams.installerPackageName, installParamsMakeInstallParams.userId) + "]");
        return 0;
    }

    private int runInstallWrite() throws RemoteException {
        long j = -1;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (nextOption.equals("-S")) {
                    j = Long.parseLong(getNextArg());
                } else {
                    throw new IllegalArgumentException("Unknown option: " + nextOption);
                }
            } else {
                return doWriteSplit(Integer.parseInt(getNextArg()), getNextArg(), j, getNextArg(), true);
            }
        }
    }

    private int runInstallRemove() throws RemoteException {
        PrintWriter outPrintWriter = getOutPrintWriter();
        int i = Integer.parseInt(getNextArg());
        String nextArg = getNextArg();
        if (nextArg == null) {
            outPrintWriter.println("Error: split name not specified");
            return 1;
        }
        return doRemoveSplit(i, nextArg, true);
    }

    private int runInstallExisting() throws RemoteException {
        PrintWriter outPrintWriter = getOutPrintWriter();
        int i = 0;
        int userArg = 0;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                byte b = -1;
                int iHashCode = nextOption.hashCode();
                if (iHashCode != -951415743) {
                    if (iHashCode != 1051781117) {
                        if (iHashCode != 1333024815) {
                            if (iHashCode == 1333469547 && nextOption.equals("--user")) {
                                b = 0;
                            }
                        } else if (nextOption.equals("--full")) {
                            b = 3;
                        }
                    } else if (nextOption.equals("--ephemeral")) {
                        b = 1;
                    }
                } else if (nextOption.equals("--instant")) {
                    b = 2;
                }
                switch (b) {
                    case 0:
                        userArg = UserHandle.parseUserArg(getNextArgRequired());
                        break;
                    case 1:
                    case 2:
                        i = (i | 2048) & (-16385);
                        break;
                    case 3:
                        i = (i & (-2049)) | 16384;
                        break;
                    default:
                        outPrintWriter.println("Error: Unknown option: " + nextOption);
                        return 1;
                }
            } else {
                String nextArg = getNextArg();
                if (nextArg == null) {
                    outPrintWriter.println("Error: package name not specified");
                    return 1;
                }
                try {
                    if (this.mInterface.installExistingPackageAsUser(nextArg, userArg, i, 0) == -3) {
                        throw new PackageManager.NameNotFoundException("Package " + nextArg + " doesn't exist");
                    }
                    outPrintWriter.println("Package " + nextArg + " installed for user: " + userArg);
                    return 0;
                } catch (PackageManager.NameNotFoundException | RemoteException e) {
                    outPrintWriter.println(e.toString());
                    return 1;
                }
            }
        }
    }

    private int runSetInstallLocation() throws RemoteException {
        String nextArg = getNextArg();
        if (nextArg == null) {
            getErrPrintWriter().println("Error: no install location specified.");
            return 1;
        }
        try {
            if (!this.mInterface.setInstallLocation(Integer.parseInt(nextArg))) {
                getErrPrintWriter().println("Error: install location has to be a number.");
                return 1;
            }
            return 0;
        } catch (NumberFormatException e) {
            getErrPrintWriter().println("Error: install location has to be a number.");
            return 1;
        }
    }

    private int runGetInstallLocation() throws RemoteException {
        int installLocation = this.mInterface.getInstallLocation();
        String str = "invalid";
        if (installLocation == 0) {
            str = UiModeManagerService.Shell.NIGHT_MODE_STR_AUTO;
        } else if (installLocation == 1) {
            str = "internal";
        } else if (installLocation == 2) {
            str = "external";
        }
        getOutPrintWriter().println(installLocation + "[" + str + "]");
        return 0;
    }

    public int runMovePackage() throws RemoteException {
        String nextArg = getNextArg();
        if (nextArg == null) {
            getErrPrintWriter().println("Error: package name not specified");
            return 1;
        }
        String nextArg2 = getNextArg();
        if ("internal".equals(nextArg2)) {
            nextArg2 = null;
        }
        int iMovePackage = this.mInterface.movePackage(nextArg, nextArg2);
        int moveStatus = this.mInterface.getMoveStatus(iMovePackage);
        while (!PackageManager.isMoveStatusFinished(moveStatus)) {
            SystemClock.sleep(1000L);
            moveStatus = this.mInterface.getMoveStatus(iMovePackage);
        }
        if (moveStatus == -100) {
            getOutPrintWriter().println("Success");
            return 0;
        }
        getErrPrintWriter().println("Failure [" + moveStatus + "]");
        return 1;
    }

    public int runMovePrimaryStorage() throws RemoteException {
        String nextArg = getNextArg();
        if ("internal".equals(nextArg)) {
            nextArg = null;
        }
        int iMovePrimaryStorage = this.mInterface.movePrimaryStorage(nextArg);
        int moveStatus = this.mInterface.getMoveStatus(iMovePrimaryStorage);
        while (!PackageManager.isMoveStatusFinished(moveStatus)) {
            SystemClock.sleep(1000L);
            moveStatus = this.mInterface.getMoveStatus(iMovePrimaryStorage);
        }
        if (moveStatus == -100) {
            getOutPrintWriter().println("Success");
            return 0;
        }
        getErrPrintWriter().println("Failure [" + moveStatus + "]");
        return 1;
    }

    private int runCompile() throws RemoteException {
        List listSingletonList;
        String str;
        Iterator it;
        int i;
        boolean z;
        boolean zPerformDexOptMode;
        PackageManagerShellCommand packageManagerShellCommand = this;
        PrintWriter outPrintWriter = getOutPrintWriter();
        boolean z2 = SystemProperties.getBoolean("dalvik.vm.usejitprofiles", false);
        String nextArgRequired = null;
        boolean z3 = false;
        boolean z4 = false;
        boolean z5 = false;
        boolean z6 = false;
        String compilerFilterForReason = null;
        String nextArgRequired2 = null;
        String nextArgRequired3 = null;
        while (true) {
            String nextOption = getNextOption();
            byte b = -1;
            int i2 = 1;
            if (nextOption == null) {
                if (nextArgRequired != null) {
                    if ("true".equals(nextArgRequired)) {
                        z2 = true;
                    } else {
                        if (!"false".equals(nextArgRequired)) {
                            outPrintWriter.println("Invalid value for \"--check-prof\". Expected \"true\" or \"false\".");
                            return 1;
                        }
                        z2 = false;
                    }
                }
                if (compilerFilterForReason != null && nextArgRequired2 != null) {
                    outPrintWriter.println("Cannot use compilation filter (\"-m\") and compilation reason (\"-r\") at the same time");
                    return 1;
                }
                if (compilerFilterForReason == null && nextArgRequired2 == null) {
                    outPrintWriter.println("Cannot run without any of compilation filter (\"-m\") and compilation reason (\"-r\") at the same time");
                    return 1;
                }
                if (z3 && nextArgRequired3 != null) {
                    outPrintWriter.println("-a cannot be specified together with --split");
                    return 1;
                }
                if (z5 && nextArgRequired3 != null) {
                    outPrintWriter.println("--secondary-dex cannot be specified together with --split");
                    return 1;
                }
                if (compilerFilterForReason == null) {
                    int i3 = 0;
                    while (true) {
                        if (i3 >= PackageManagerServiceCompilerMapping.REASON_STRINGS.length) {
                            i3 = -1;
                        } else if (!PackageManagerServiceCompilerMapping.REASON_STRINGS[i3].equals(nextArgRequired2)) {
                            i3++;
                        }
                    }
                    if (i3 == -1) {
                        outPrintWriter.println("Error: Unknown compilation reason: " + nextArgRequired2);
                        return 1;
                    }
                    compilerFilterForReason = PackageManagerServiceCompilerMapping.getCompilerFilterForReason(i3);
                } else if (!DexFile.isValidCompilerFilter(compilerFilterForReason)) {
                    outPrintWriter.println("Error: \"" + compilerFilterForReason + "\" is not a valid compilation filter.");
                    return 1;
                }
                if (z3) {
                    listSingletonList = packageManagerShellCommand.mInterface.getAllPackages();
                } else {
                    String nextArg = getNextArg();
                    if (nextArg == null) {
                        outPrintWriter.println("Error: package name not specified");
                        return 1;
                    }
                    listSingletonList = Collections.singletonList(nextArg);
                }
                ArrayList<String> arrayList = new ArrayList();
                Iterator it2 = listSingletonList.iterator();
                int i4 = 0;
                while (it2.hasNext()) {
                    String str2 = (String) it2.next();
                    if (z6) {
                        packageManagerShellCommand.mInterface.clearApplicationProfileData(str2);
                    }
                    if (z3) {
                        StringBuilder sb = new StringBuilder();
                        i4++;
                        sb.append(i4);
                        sb.append(SliceClientPermissions.SliceAuthority.DELIMITER);
                        sb.append(listSingletonList.size());
                        sb.append(": ");
                        sb.append(str2);
                        outPrintWriter.println(sb.toString());
                        outPrintWriter.flush();
                    }
                    int i5 = i4;
                    if (z5) {
                        zPerformDexOptMode = packageManagerShellCommand.mInterface.performDexOptSecondary(str2, compilerFilterForReason, z4);
                        str = str2;
                        it = it2;
                        i = i2;
                        z = z4;
                    } else {
                        str = str2;
                        it = it2;
                        i = i2;
                        z = z4;
                        zPerformDexOptMode = packageManagerShellCommand.mInterface.performDexOptMode(str2, z2, compilerFilterForReason, z4, true, nextArgRequired3);
                    }
                    if (!zPerformDexOptMode) {
                        arrayList.add(str);
                    }
                    i2 = i;
                    i4 = i5;
                    z4 = z;
                    it2 = it;
                    packageManagerShellCommand = this;
                }
                int i6 = i2;
                if (arrayList.isEmpty()) {
                    outPrintWriter.println("Success");
                    return 0;
                }
                if (arrayList.size() == i6) {
                    outPrintWriter.println("Failure: package " + ((String) arrayList.get(0)) + " could not be compiled");
                    return i6;
                }
                outPrintWriter.print("Failure: the following packages could not be compiled: ");
                int i7 = i6;
                for (String str3 : arrayList) {
                    if (i7 != 0) {
                        i7 = 0;
                    } else {
                        outPrintWriter.print(", ");
                    }
                    outPrintWriter.print(str3);
                }
                outPrintWriter.println();
                return i6;
            }
            int iHashCode = nextOption.hashCode();
            if (iHashCode != -1615291473) {
                if (iHashCode != -1614046854) {
                    if (iHashCode != 1492) {
                        if (iHashCode != 1494) {
                            if (iHashCode != 1497) {
                                if (iHashCode != 1504) {
                                    if (iHashCode != 1509) {
                                        if (iHashCode != 1269477022) {
                                            if (iHashCode == 1690714782 && nextOption.equals("--check-prof")) {
                                                b = 5;
                                            }
                                        } else if (nextOption.equals("--secondary-dex")) {
                                            b = 7;
                                        }
                                    } else if (nextOption.equals("-r")) {
                                        b = 4;
                                    }
                                } else if (nextOption.equals("-m")) {
                                    b = 3;
                                }
                            } else if (nextOption.equals("-f")) {
                                b = 2;
                            }
                        } else if (nextOption.equals("-c")) {
                            b = 1;
                        }
                    } else if (nextOption.equals("-a")) {
                        b = 0;
                    }
                } else if (nextOption.equals("--split")) {
                    b = 8;
                }
            } else if (nextOption.equals("--reset")) {
                b = 6;
            }
            switch (b) {
                case 0:
                    z3 = true;
                    break;
                case 1:
                    z6 = true;
                    break;
                case 2:
                    z4 = true;
                    break;
                case 3:
                    compilerFilterForReason = getNextArgRequired();
                    break;
                case 4:
                    nextArgRequired2 = getNextArgRequired();
                    break;
                case 5:
                    nextArgRequired = getNextArgRequired();
                    break;
                case 6:
                    nextArgRequired2 = "install";
                    z4 = true;
                    z6 = true;
                    break;
                case 7:
                    z5 = true;
                    break;
                case 8:
                    nextArgRequired3 = getNextArgRequired();
                    break;
                default:
                    outPrintWriter.println("Error: Unknown option: " + nextOption);
                    return 1;
            }
        }
    }

    private int runreconcileSecondaryDexFiles() throws RemoteException {
        this.mInterface.reconcileSecondaryDexFiles(getNextArg());
        return 0;
    }

    public int runForceDexOpt() throws RemoteException {
        this.mInterface.forceDexOpt(getNextArgRequired());
        return 0;
    }

    private int runDexoptJob() throws RemoteException {
        ArrayList arrayList = new ArrayList();
        while (true) {
            String nextArg = getNextArg();
            if (nextArg == null) {
                break;
            }
            arrayList.add(nextArg);
        }
        IPackageManager iPackageManager = this.mInterface;
        if (arrayList.isEmpty()) {
            arrayList = null;
        }
        return iPackageManager.runBackgroundDexoptJob(arrayList) ? 0 : -1;
    }

    private int runDumpProfiles() throws RemoteException {
        this.mInterface.dumpProfiles(getNextArg());
        return 0;
    }

    private int runSnapshotProfile() throws RemoteException {
        String str;
        String str2;
        String str3;
        Throwable th;
        PrintWriter outPrintWriter = getOutPrintWriter();
        String nextArg = getNextArg();
        boolean zEquals = PackageManagerService.PLATFORM_PACKAGE_NAME.equals(nextArg);
        String nextArg2 = null;
        while (true) {
            String nextArg3 = getNextArg();
            if (nextArg3 != null) {
                if (((nextArg3.hashCode() == -684928411 && nextArg3.equals("--code-path")) ? (byte) 0 : (byte) -1) == 0) {
                    if (zEquals) {
                        outPrintWriter.write("--code-path cannot be used for the boot image.");
                        return -1;
                    }
                    nextArg2 = getNextArg();
                } else {
                    outPrintWriter.write("Unknown arg: " + nextArg3);
                    return -1;
                }
            } else {
                if (zEquals) {
                    str = nextArg2;
                    str2 = null;
                } else {
                    PackageInfo packageInfo = this.mInterface.getPackageInfo(nextArg, 0, 0);
                    if (packageInfo == null) {
                        outPrintWriter.write("Package not found " + nextArg);
                        return -1;
                    }
                    String baseCodePath = packageInfo.applicationInfo.getBaseCodePath();
                    if (nextArg2 == null) {
                        str = baseCodePath;
                        str2 = str;
                    } else {
                        str = nextArg2;
                        str2 = baseCodePath;
                    }
                }
                SnapshotRuntimeProfileCallback snapshotRuntimeProfileCallback = new SnapshotRuntimeProfileCallback();
                String str4 = Binder.getCallingUid() == 0 ? "root" : "com.android.shell";
                int i = zEquals ? 1 : 0;
                if (!this.mInterface.getArtManager().isRuntimeProfilingEnabled(i, str4)) {
                    outPrintWriter.println("Error: Runtime profiling is not enabled");
                    return -1;
                }
                this.mInterface.getArtManager().snapshotRuntimeProfile(i, nextArg, str, snapshotRuntimeProfileCallback, str4);
                if (!snapshotRuntimeProfileCallback.waitTillDone()) {
                    outPrintWriter.println("Error: callback not called");
                    return snapshotRuntimeProfileCallback.mErrCode;
                }
                try {
                    ParcelFileDescriptor.AutoCloseInputStream autoCloseInputStream = new ParcelFileDescriptor.AutoCloseInputStream(snapshotRuntimeProfileCallback.mProfileReadFd);
                    try {
                        if (!zEquals) {
                            try {
                                if (Objects.equals(str2, str)) {
                                    str3 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                                } else {
                                    str3 = STDIN_PATH + new File(str).getName();
                                }
                            } finally {
                            }
                        } else {
                            str3 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                        }
                        String str5 = ART_PROFILE_SNAPSHOT_DEBUG_LOCATION + nextArg + str3 + ".prof";
                        FileOutputStream fileOutputStream = new FileOutputStream(str5);
                        try {
                            Streams.copy(autoCloseInputStream, fileOutputStream);
                            $closeResource(null, fileOutputStream);
                            Os.chmod(str5, 420);
                            $closeResource(null, autoCloseInputStream);
                            return 0;
                        } catch (Throwable th2) {
                            th = th2;
                            th = null;
                            $closeResource(th, fileOutputStream);
                            throw th;
                        }
                    } catch (Throwable th3) {
                        $closeResource(null, autoCloseInputStream);
                        throw th3;
                    }
                } catch (ErrnoException | IOException e) {
                    outPrintWriter.println("Error when reading the profile fd: " + e.getMessage());
                    e.printStackTrace(outPrintWriter);
                    return -1;
                }
            }
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    private static class SnapshotRuntimeProfileCallback extends ISnapshotRuntimeProfileCallback.Stub {
        private CountDownLatch mDoneSignal;
        private int mErrCode;
        private ParcelFileDescriptor mProfileReadFd;
        private boolean mSuccess;

        private SnapshotRuntimeProfileCallback() {
            this.mSuccess = false;
            this.mErrCode = -1;
            this.mProfileReadFd = null;
            this.mDoneSignal = new CountDownLatch(1);
        }

        public void onSuccess(ParcelFileDescriptor parcelFileDescriptor) {
            this.mSuccess = true;
            try {
                this.mProfileReadFd = parcelFileDescriptor.dup();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.mDoneSignal.countDown();
        }

        public void onError(int i) {
            this.mSuccess = false;
            this.mErrCode = i;
            this.mDoneSignal.countDown();
        }

        boolean waitTillDone() {
            boolean zAwait;
            try {
                zAwait = this.mDoneSignal.await(10000000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                zAwait = false;
            }
            return zAwait && this.mSuccess;
        }
    }

    private int runUninstall() throws RemoteException {
        int i;
        int i2;
        byte b;
        PrintWriter outPrintWriter = getOutPrintWriter();
        long j = -1;
        int userArg = -1;
        int i3 = 0;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                int iHashCode = nextOption.hashCode();
                if (iHashCode != 1502) {
                    if (iHashCode != 1333469547) {
                        b = (iHashCode == 1884113221 && nextOption.equals("--versionCode")) ? (byte) 2 : (byte) -1;
                    } else if (nextOption.equals("--user")) {
                        b = 1;
                    }
                } else if (nextOption.equals("-k")) {
                    b = 0;
                }
                switch (b) {
                    case 0:
                        i3 |= 1;
                        break;
                    case 1:
                        userArg = UserHandle.parseUserArg(getNextArgRequired());
                        break;
                    case 2:
                        j = Long.parseLong(getNextArgRequired());
                        break;
                    default:
                        outPrintWriter.println("Error: Unknown option: " + nextOption);
                        return 1;
                }
            } else {
                String nextArg = getNextArg();
                if (nextArg == null) {
                    outPrintWriter.println("Error: package name not specified");
                    return 1;
                }
                String nextArg2 = getNextArg();
                if (nextArg2 != null) {
                    return runRemoveSplit(nextArg, nextArg2);
                }
                int iTranslateUserId = translateUserId(userArg, true, "runUninstall");
                if (iTranslateUserId != -1) {
                    PackageInfo packageInfo = this.mInterface.getPackageInfo(nextArg, 67108864, iTranslateUserId);
                    if (packageInfo == null) {
                        outPrintWriter.println("Failure [not installed for " + iTranslateUserId + "]");
                        return 1;
                    }
                    if ((packageInfo.applicationInfo.flags & 1) != 0) {
                        i3 |= 4;
                    }
                    i = i3;
                    i2 = iTranslateUserId;
                } else {
                    i2 = 0;
                    i = i3 | 2;
                }
                LocalIntentReceiver localIntentReceiver = new LocalIntentReceiver();
                this.mInterface.getPackageInstaller().uninstall(new VersionedPackage(nextArg, j), (String) null, i, localIntentReceiver.getIntentSender(), i2);
                Intent result = localIntentReceiver.getResult();
                if (result.getIntExtra("android.content.pm.extra.STATUS", 1) == 0) {
                    outPrintWriter.println("Success");
                    return 0;
                }
                outPrintWriter.println("Failure [" + result.getStringExtra("android.content.pm.extra.STATUS_MESSAGE") + "]");
                return 1;
            }
        }
    }

    private int runRemoveSplit(String str, String str2) throws Throwable {
        PrintWriter outPrintWriter = getOutPrintWriter();
        PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(2);
        sessionParams.installFlags = 2 | sessionParams.installFlags;
        sessionParams.appPackageName = str;
        int iDoCreateSession = doCreateSession(sessionParams, null, -1);
        boolean z = true;
        try {
            if (doRemoveSplit(iDoCreateSession, str2, false) != 0) {
                try {
                    doAbandonSession(iDoCreateSession, false);
                } catch (Exception e) {
                }
                return 1;
            }
            if (doCommitSession(iDoCreateSession, false) != 0) {
                try {
                    doAbandonSession(iDoCreateSession, false);
                } catch (Exception e2) {
                }
                return 1;
            }
            try {
                outPrintWriter.println("Success");
                return 0;
            } catch (Throwable th) {
                th = th;
                z = false;
            }
        } catch (Throwable th2) {
            th = th2;
        }
        if (z) {
            try {
                doAbandonSession(iDoCreateSession, false);
            } catch (Exception e3) {
            }
        }
        throw th;
    }

    static class ClearDataObserver extends IPackageDataObserver.Stub {
        boolean finished;
        boolean result;

        ClearDataObserver() {
        }

        public void onRemoveCompleted(String str, boolean z) throws RemoteException {
            synchronized (this) {
                this.finished = true;
                this.result = z;
                notifyAll();
            }
        }
    }

    private int runClear() throws RemoteException {
        int userArg;
        String nextOption = getNextOption();
        if (nextOption != null && nextOption.equals("--user")) {
            userArg = UserHandle.parseUserArg(getNextArgRequired());
        } else {
            userArg = 0;
        }
        String nextArg = getNextArg();
        if (nextArg == null) {
            getErrPrintWriter().println("Error: no package specified");
            return 1;
        }
        ClearDataObserver clearDataObserver = new ClearDataObserver();
        ActivityManager.getService().clearApplicationUserData(nextArg, false, clearDataObserver, userArg);
        synchronized (clearDataObserver) {
            while (!clearDataObserver.finished) {
                try {
                    clearDataObserver.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        if (clearDataObserver.result) {
            getOutPrintWriter().println("Success");
            return 0;
        }
        getErrPrintWriter().println("Failed");
        return 1;
    }

    private static String enabledSettingToString(int i) {
        switch (i) {
            case 0:
                return BatteryService.HealthServiceWrapper.INSTANCE_VENDOR;
            case 1:
                return "enabled";
            case 2:
                return "disabled";
            case 3:
                return "disabled-user";
            case 4:
                return "disabled-until-used";
            default:
                return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
        }
    }

    private int runSetEnabledSetting(int i) throws RemoteException {
        int userArg;
        String nextOption = getNextOption();
        if (nextOption != null && nextOption.equals("--user")) {
            userArg = UserHandle.parseUserArg(getNextArgRequired());
        } else {
            userArg = 0;
        }
        String nextArg = getNextArg();
        if (nextArg == null) {
            getErrPrintWriter().println("Error: no package or component specified");
            return 1;
        }
        ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(nextArg);
        if (componentNameUnflattenFromString != null) {
            this.mInterface.setComponentEnabledSetting(componentNameUnflattenFromString, i, 0, userArg);
            getOutPrintWriter().println("Component " + componentNameUnflattenFromString.toShortString() + " new state: " + enabledSettingToString(this.mInterface.getComponentEnabledSetting(componentNameUnflattenFromString, userArg)));
            return 0;
        }
        this.mInterface.setApplicationEnabledSetting(nextArg, i, 0, userArg, "shell:" + Process.myUid());
        getOutPrintWriter().println("Package " + nextArg + " new state: " + enabledSettingToString(this.mInterface.getApplicationEnabledSetting(nextArg, userArg)));
        return 0;
    }

    private int runSetHiddenSetting(boolean z) throws RemoteException {
        int userArg;
        String nextOption = getNextOption();
        if (nextOption != null && nextOption.equals("--user")) {
            userArg = UserHandle.parseUserArg(getNextArgRequired());
        } else {
            userArg = 0;
        }
        String nextArg = getNextArg();
        if (nextArg == null) {
            getErrPrintWriter().println("Error: no package or component specified");
            return 1;
        }
        this.mInterface.setApplicationHiddenSettingAsUser(nextArg, z, userArg);
        getOutPrintWriter().println("Package " + nextArg + " new hidden state: " + this.mInterface.getApplicationHiddenSettingAsUser(nextArg, userArg));
        return 0;
    }

    private int runSuspend(boolean z) {
        PrintWriter outPrintWriter = getOutPrintWriter();
        PersistableBundle persistableBundle = new PersistableBundle();
        PersistableBundle persistableBundle2 = new PersistableBundle();
        String nextArgRequired = null;
        int userArg = 0;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                switch (nextOption) {
                    case "--user":
                        userArg = UserHandle.parseUserArg(getNextArgRequired());
                        break;
                    case "--dialogMessage":
                        nextArgRequired = getNextArgRequired();
                        break;
                    case "--ael":
                    case "--aes":
                    case "--aed":
                    case "--lel":
                    case "--les":
                    case "--led":
                        String nextArgRequired2 = getNextArgRequired();
                        String nextArgRequired3 = getNextArgRequired();
                        if (z) {
                            PersistableBundle persistableBundle3 = nextOption.startsWith("--a") ? persistableBundle : persistableBundle2;
                            char cCharAt = nextOption.charAt(4);
                            if (cCharAt == 'd') {
                                persistableBundle3.putDouble(nextArgRequired2, Double.valueOf(nextArgRequired3).doubleValue());
                            } else if (cCharAt == 'l') {
                                persistableBundle3.putLong(nextArgRequired2, Long.valueOf(nextArgRequired3).longValue());
                            } else if (cCharAt == 's') {
                                persistableBundle3.putString(nextArgRequired2, nextArgRequired3);
                            }
                            break;
                        } else {
                            break;
                        }
                        break;
                    default:
                        outPrintWriter.println("Error: Unknown option: " + nextOption);
                        return 1;
                }
            } else {
                String nextArg = getNextArg();
                if (nextArg == null) {
                    outPrintWriter.println("Error: package name not specified");
                    return 1;
                }
                try {
                    this.mInterface.setPackagesSuspendedAsUser(new String[]{nextArg}, z, persistableBundle, persistableBundle2, nextArgRequired, Binder.getCallingUid() == 0 ? "root" : "com.android.shell", userArg);
                    outPrintWriter.println("Package " + nextArg + " new suspended state: " + this.mInterface.isPackageSuspendedForUser(nextArg, userArg));
                    return 0;
                } catch (RemoteException | IllegalArgumentException e) {
                    outPrintWriter.println(e.toString());
                    return 1;
                }
            }
        }
    }

    private int runGrantRevokePermission(boolean z) throws RemoteException {
        int userArg = 0;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption == null) {
                break;
            }
            if (nextOption.equals("--user")) {
                userArg = UserHandle.parseUserArg(getNextArgRequired());
            }
        }
        String nextArg = getNextArg();
        if (nextArg == null) {
            getErrPrintWriter().println("Error: no package specified");
            return 1;
        }
        String nextArg2 = getNextArg();
        if (nextArg2 == null) {
            getErrPrintWriter().println("Error: no permission specified");
            return 1;
        }
        if (z) {
            this.mInterface.grantRuntimePermission(nextArg, nextArg2, userArg);
        } else {
            this.mInterface.revokeRuntimePermission(nextArg, nextArg2, userArg);
        }
        return 0;
    }

    private int runResetPermissions() throws RemoteException {
        this.mInterface.resetRuntimePermissions();
        return 0;
    }

    private int runSetPermissionEnforced() throws RemoteException {
        String nextArg = getNextArg();
        if (nextArg == null) {
            getErrPrintWriter().println("Error: no permission specified");
            return 1;
        }
        String nextArg2 = getNextArg();
        if (nextArg2 == null) {
            getErrPrintWriter().println("Error: no enforcement specified");
            return 1;
        }
        this.mInterface.setPermissionEnforced(nextArg, Boolean.parseBoolean(nextArg2));
        return 0;
    }

    private boolean isVendorApp(String str) {
        try {
            PackageInfo packageInfo = this.mInterface.getPackageInfo(str, 0, 0);
            if (packageInfo != null) {
                return packageInfo.applicationInfo.isVendor();
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    private boolean isProductApp(String str) {
        try {
            PackageInfo packageInfo = this.mInterface.getPackageInfo(str, 0, 0);
            if (packageInfo != null) {
                return packageInfo.applicationInfo.isProduct();
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    private int runGetPrivappPermissions() {
        ArraySet privAppPermissions;
        String nextArg = getNextArg();
        if (nextArg == null) {
            getErrPrintWriter().println("Error: no package specified.");
            return 1;
        }
        if (isVendorApp(nextArg)) {
            privAppPermissions = SystemConfig.getInstance().getVendorPrivAppPermissions(nextArg);
        } else if (isProductApp(nextArg)) {
            privAppPermissions = SystemConfig.getInstance().getProductPrivAppPermissions(nextArg);
        } else {
            privAppPermissions = SystemConfig.getInstance().getPrivAppPermissions(nextArg);
        }
        getOutPrintWriter().println(privAppPermissions == null ? "{}" : privAppPermissions.toString());
        return 0;
    }

    private int runGetPrivappDenyPermissions() {
        ArraySet privAppDenyPermissions;
        String nextArg = getNextArg();
        if (nextArg == null) {
            getErrPrintWriter().println("Error: no package specified.");
            return 1;
        }
        if (isVendorApp(nextArg)) {
            privAppDenyPermissions = SystemConfig.getInstance().getVendorPrivAppDenyPermissions(nextArg);
        } else if (isProductApp(nextArg)) {
            privAppDenyPermissions = SystemConfig.getInstance().getProductPrivAppDenyPermissions(nextArg);
        } else {
            privAppDenyPermissions = SystemConfig.getInstance().getPrivAppDenyPermissions(nextArg);
        }
        getOutPrintWriter().println(privAppDenyPermissions == null ? "{}" : privAppDenyPermissions.toString());
        return 0;
    }

    private int runGetOemPermissions() {
        String nextArg = getNextArg();
        if (nextArg == null) {
            getErrPrintWriter().println("Error: no package specified.");
            return 1;
        }
        Map oemPermissions = SystemConfig.getInstance().getOemPermissions(nextArg);
        if (oemPermissions == null || oemPermissions.isEmpty()) {
            getOutPrintWriter().println("{}");
            return 0;
        }
        oemPermissions.forEach(new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                this.f$0.getOutPrintWriter().println(((String) obj) + " granted:" + ((Boolean) obj2));
            }
        });
        return 0;
    }

    private String linkStateToString(int i) {
        switch (i) {
            case 0:
                return "undefined";
            case 1:
                return "ask";
            case 2:
                return "always";
            case 3:
                return "never";
            case 4:
                return "always ask";
            default:
                return "Unknown link state: " + i;
        }
    }

    private int runSetAppLink() throws RemoteException {
        int i;
        int userArg = 0;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption == null) {
                String nextArg = getNextArg();
                if (nextArg == null) {
                    getErrPrintWriter().println("Error: no package specified.");
                    return 1;
                }
                String nextArg2 = getNextArg();
                if (nextArg2 == null) {
                    getErrPrintWriter().println("Error: no app link state specified.");
                    return 1;
                }
                i = 2;
                switch (nextArg2.toLowerCase()) {
                    case "undefined":
                        i = 0;
                        break;
                    case "always":
                        break;
                    case "ask":
                        i = 1;
                        break;
                    case "always-ask":
                        i = 4;
                        break;
                    case "never":
                        i = 3;
                        break;
                    default:
                        getErrPrintWriter().println("Error: unknown app link state '" + nextArg2 + "'");
                        return 1;
                }
                PackageInfo packageInfo = this.mInterface.getPackageInfo(nextArg, 0, userArg);
                if (packageInfo == null) {
                    getErrPrintWriter().println("Error: package " + nextArg + " not found.");
                    return 1;
                }
                if ((packageInfo.applicationInfo.privateFlags & 16) == 0) {
                    getErrPrintWriter().println("Error: package " + nextArg + " does not handle web links.");
                    return 1;
                }
                if (this.mInterface.updateIntentVerificationStatus(nextArg, i, userArg)) {
                    return 0;
                }
                getErrPrintWriter().println("Error: unable to update app link status for " + nextArg);
                return 1;
            }
            if (!nextOption.equals("--user")) {
                getErrPrintWriter().println("Error: unknown option: " + nextOption);
                return 1;
            }
            userArg = UserHandle.parseUserArg(getNextArgRequired());
        }
    }

    private int runGetAppLink() throws RemoteException {
        int userArg = 0;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (nextOption.equals("--user")) {
                    userArg = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    getErrPrintWriter().println("Error: unknown option: " + nextOption);
                    return 1;
                }
            } else {
                String nextArg = getNextArg();
                if (nextArg != null) {
                    PackageInfo packageInfo = this.mInterface.getPackageInfo(nextArg, 0, userArg);
                    if (packageInfo == null) {
                        getErrPrintWriter().println("Error: package " + nextArg + " not found.");
                        return 1;
                    }
                    if ((packageInfo.applicationInfo.privateFlags & 16) == 0) {
                        getErrPrintWriter().println("Error: package " + nextArg + " does not handle web links.");
                        return 1;
                    }
                    getOutPrintWriter().println(linkStateToString(this.mInterface.getIntentVerificationStatus(nextArg, userArg)));
                    return 0;
                }
                getErrPrintWriter().println("Error: no package specified.");
                return 1;
            }
        }
    }

    private int runTrimCaches() throws RemoteException {
        String nextArg = getNextArg();
        if (nextArg == null) {
            getErrPrintWriter().println("Error: no size specified");
            return 1;
        }
        long j = 1;
        int length = nextArg.length() - 1;
        char cCharAt = nextArg.charAt(length);
        if (cCharAt < '0' || cCharAt > '9') {
            if (cCharAt == 'K' || cCharAt == 'k') {
                j = 1024;
            } else if (cCharAt == 'M' || cCharAt == 'm') {
                j = 1048576;
            } else if (cCharAt == 'G' || cCharAt == 'g') {
                j = 1073741824;
            } else {
                getErrPrintWriter().println("Invalid suffix: " + cCharAt);
                return 1;
            }
            nextArg = nextArg.substring(0, length);
        }
        try {
            long j2 = Long.parseLong(nextArg) * j;
            String nextArg2 = getNextArg();
            if ("internal".equals(nextArg2)) {
                nextArg2 = null;
            }
            ClearDataObserver clearDataObserver = new ClearDataObserver();
            this.mInterface.freeStorageAndNotify(nextArg2, j2, 2, clearDataObserver);
            synchronized (clearDataObserver) {
                while (!clearDataObserver.finished) {
                    try {
                        clearDataObserver.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            return 0;
        } catch (NumberFormatException e2) {
            getErrPrintWriter().println("Error: expected number at: " + nextArg);
            return 1;
        }
    }

    private static boolean isNumber(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public int runCreateUser() throws RemoteException {
        UserInfo userInfoCreateProfileForUser;
        int userArg = -1;
        int i = 0;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if ("--profileOf".equals(nextOption)) {
                    userArg = UserHandle.parseUserArg(getNextArgRequired());
                } else if ("--managed".equals(nextOption)) {
                    i |= 32;
                } else if ("--restricted".equals(nextOption)) {
                    i |= 8;
                } else if ("--ephemeral".equals(nextOption)) {
                    i |= 256;
                } else if ("--guest".equals(nextOption)) {
                    i |= 4;
                } else if ("--demo".equals(nextOption)) {
                    i |= 512;
                } else {
                    getErrPrintWriter().println("Error: unknown option " + nextOption);
                    return 1;
                }
            } else {
                String nextArg = getNextArg();
                if (nextArg == null) {
                    getErrPrintWriter().println("Error: no user name specified.");
                    return 1;
                }
                IUserManager iUserManagerAsInterface = IUserManager.Stub.asInterface(ServiceManager.getService("user"));
                IAccountManager iAccountManagerAsInterface = IAccountManager.Stub.asInterface(ServiceManager.getService("account"));
                if ((i & 8) != 0) {
                    int i2 = userArg >= 0 ? userArg : 0;
                    userInfoCreateProfileForUser = iUserManagerAsInterface.createRestrictedProfile(nextArg, i2);
                    iAccountManagerAsInterface.addSharedAccountsFromParentUser(i2, userArg, Process.myUid() == 0 ? "root" : "com.android.shell");
                } else if (userArg < 0) {
                    userInfoCreateProfileForUser = iUserManagerAsInterface.createUser(nextArg, i);
                } else {
                    userInfoCreateProfileForUser = iUserManagerAsInterface.createProfileForUser(nextArg, i, userArg, (String[]) null);
                }
                if (userInfoCreateProfileForUser != null) {
                    getOutPrintWriter().println("Success: created user id " + userInfoCreateProfileForUser.id);
                    return 0;
                }
                getErrPrintWriter().println("Error: couldn't create User.");
                return 1;
            }
        }
    }

    public int runRemoveUser() throws RemoteException {
        String nextArg = getNextArg();
        if (nextArg == null) {
            getErrPrintWriter().println("Error: no user id specified.");
            return 1;
        }
        int userArg = UserHandle.parseUserArg(nextArg);
        if (IUserManager.Stub.asInterface(ServiceManager.getService("user")).removeUser(userArg)) {
            getOutPrintWriter().println("Success: removed user");
            return 0;
        }
        getErrPrintWriter().println("Error: couldn't remove user id " + userArg);
        return 1;
    }

    public int runSetUserRestriction() throws RemoteException {
        int userArg;
        String nextOption = getNextOption();
        if (nextOption != null && "--user".equals(nextOption)) {
            userArg = UserHandle.parseUserArg(getNextArgRequired());
        } else {
            userArg = 0;
        }
        String nextArg = getNextArg();
        String nextArg2 = getNextArg();
        boolean z = true;
        if (!"1".equals(nextArg2)) {
            if (!"0".equals(nextArg2)) {
                getErrPrintWriter().println("Error: valid value not specified");
                return 1;
            }
            z = false;
        }
        IUserManager.Stub.asInterface(ServiceManager.getService("user")).setUserRestriction(nextArg, z, userArg);
        return 0;
    }

    public int runGetMaxUsers() {
        getOutPrintWriter().println("Maximum supported users: " + UserManager.getMaxSupportedUsers());
        return 0;
    }

    public int runGetMaxRunningUsers() {
        ActivityManagerInternal activityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        getOutPrintWriter().println("Maximum supported running users: " + activityManagerInternal.getMaxRunningUsers());
        return 0;
    }

    private static class InstallParams {
        String installerPackageName;
        PackageInstaller.SessionParams sessionParams;
        int userId;

        private InstallParams() {
            this.userId = -1;
        }
    }

    private InstallParams makeInstallParams() {
        PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(1);
        InstallParams installParams = new InstallParams();
        installParams.sessionParams = sessionParams;
        boolean z = true;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                byte b = -1;
                switch (nextOption.hashCode()) {
                    case -1950997763:
                        if (nextOption.equals("--force-uuid")) {
                            b = 23;
                        }
                        break;
                    case -1777984902:
                        if (nextOption.equals("--dont-kill")) {
                            b = 9;
                        }
                        break;
                    case -1313152697:
                        if (nextOption.equals("--install-location")) {
                            b = 22;
                        }
                        break;
                    case -1137116608:
                        if (nextOption.equals("--instantapp")) {
                            b = 18;
                        }
                        break;
                    case -951415743:
                        if (nextOption.equals("--instant")) {
                            b = 17;
                        }
                        break;
                    case -706813505:
                        if (nextOption.equals("--referrer")) {
                            b = 11;
                        }
                        break;
                    case 1477:
                        if (nextOption.equals("-R")) {
                            b = 2;
                        }
                        break;
                    case 1478:
                        if (nextOption.equals("-S")) {
                            b = 14;
                        }
                        break;
                    case 1495:
                        if (nextOption.equals("-d")) {
                            b = 7;
                        }
                        break;
                    case 1497:
                        if (nextOption.equals("-f")) {
                            b = 6;
                        }
                        break;
                    case 1498:
                        if (nextOption.equals("-g")) {
                            b = 8;
                        }
                        break;
                    case NetworkConstants.ETHER_MTU:
                        if (nextOption.equals("-i")) {
                            b = 3;
                        }
                        break;
                    case 1503:
                        if (nextOption.equals("-l")) {
                            b = 0;
                        }
                        break;
                    case 1507:
                        if (nextOption.equals("-p")) {
                            b = 12;
                        }
                        break;
                    case 1509:
                        if (nextOption.equals("-r")) {
                            b = 1;
                        }
                        break;
                    case 1510:
                        if (nextOption.equals("-s")) {
                            b = 5;
                        }
                        break;
                    case 1511:
                        if (nextOption.equals("-t")) {
                            b = 4;
                        }
                        break;
                    case 42995400:
                        if (nextOption.equals("--abi")) {
                            b = UsbDescriptor.DESCRIPTORTYPE_BOS;
                        }
                        break;
                    case 43010092:
                        if (nextOption.equals("--pkg")) {
                            b = UsbACInterface.ACI_SAMPLE_RATE_CONVERTER;
                        }
                        break;
                    case 148207464:
                        if (nextOption.equals("--originating-uri")) {
                            b = 10;
                        }
                        break;
                    case 1051781117:
                        if (nextOption.equals("--ephemeral")) {
                            b = 16;
                        }
                        break;
                    case 1067504745:
                        if (nextOption.equals("--preload")) {
                            b = 20;
                        }
                        break;
                    case 1333024815:
                        if (nextOption.equals("--full")) {
                            b = 19;
                        }
                        break;
                    case 1333469547:
                        if (nextOption.equals("--user")) {
                            b = 21;
                        }
                        break;
                    case 2015272120:
                        if (nextOption.equals("--force-sdk")) {
                            b = 24;
                        }
                        break;
                }
                switch (b) {
                    case 0:
                        sessionParams.installFlags |= 1;
                        break;
                    case 1:
                        break;
                    case 2:
                        z = false;
                        break;
                    case 3:
                        installParams.installerPackageName = getNextArg();
                        if (installParams.installerPackageName == null) {
                            throw new IllegalArgumentException("Missing installer package");
                        }
                        break;
                    case 4:
                        sessionParams.installFlags |= 4;
                        break;
                    case 5:
                        sessionParams.installFlags |= 8;
                        break;
                    case 6:
                        sessionParams.installFlags |= 16;
                        break;
                    case 7:
                        sessionParams.installFlags |= 128;
                        break;
                    case 8:
                        sessionParams.installFlags |= 256;
                        break;
                    case 9:
                        sessionParams.installFlags |= 4096;
                        break;
                    case 10:
                        sessionParams.originatingUri = Uri.parse(getNextArg());
                        break;
                    case 11:
                        sessionParams.referrerUri = Uri.parse(getNextArg());
                        break;
                    case 12:
                        sessionParams.mode = 2;
                        sessionParams.appPackageName = getNextArg();
                        if (sessionParams.appPackageName == null) {
                            throw new IllegalArgumentException("Missing inherit package name");
                        }
                        break;
                    case 13:
                        sessionParams.appPackageName = getNextArg();
                        if (sessionParams.appPackageName == null) {
                            throw new IllegalArgumentException("Missing package name");
                        }
                        break;
                    case 14:
                        long j = Long.parseLong(getNextArg());
                        if (j <= 0) {
                            throw new IllegalArgumentException("Size must be positive");
                        }
                        sessionParams.setSize(j);
                        break;
                        break;
                    case 15:
                        sessionParams.abiOverride = checkAbiArgument(getNextArg());
                        break;
                    case 16:
                    case 17:
                    case 18:
                        sessionParams.setInstallAsInstantApp(true);
                        break;
                    case WindowManagerService.H.REPORT_WINDOWS_CHANGE:
                        sessionParams.setInstallAsInstantApp(false);
                        break;
                    case 20:
                        sessionParams.setInstallAsVirtualPreload();
                        break;
                    case BackupHandler.MSG_OP_COMPLETE:
                        installParams.userId = UserHandle.parseUserArg(getNextArgRequired());
                        break;
                    case WindowManagerService.H.REPORT_HARD_KEYBOARD_STATUS_CHANGE:
                        sessionParams.installLocation = Integer.parseInt(getNextArg());
                        break;
                    case WindowManagerService.H.BOOT_TIMEOUT:
                        sessionParams.installFlags |= 512;
                        sessionParams.volumeUuid = getNextArg();
                        if ("internal".equals(sessionParams.volumeUuid)) {
                            sessionParams.volumeUuid = null;
                        }
                        break;
                    case 24:
                        sessionParams.installFlags |= 8192;
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown option " + nextOption);
                }
                if (z) {
                    sessionParams.installFlags |= 2;
                }
            } else {
                return installParams;
            }
        }
    }

    private int runSetHomeActivity() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        int userArg = 0;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                byte b = -1;
                if (nextOption.hashCode() == 1333469547 && nextOption.equals("--user")) {
                    b = 0;
                }
                if (b == 0) {
                    userArg = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    outPrintWriter.println("Error: Unknown option: " + nextOption);
                    return 1;
                }
            } else {
                String nextArg = getNextArg();
                ComponentName componentNameUnflattenFromString = nextArg != null ? ComponentName.unflattenFromString(nextArg) : null;
                if (componentNameUnflattenFromString == null) {
                    outPrintWriter.println("Error: component name not specified or invalid");
                    return 1;
                }
                try {
                    this.mInterface.setHomeActivity(componentNameUnflattenFromString, userArg);
                    outPrintWriter.println("Success");
                    return 0;
                } catch (Exception e) {
                    outPrintWriter.println(e.toString());
                    return 1;
                }
            }
        }
    }

    private int runSetInstaller() throws RemoteException {
        String nextArg = getNextArg();
        String nextArg2 = getNextArg();
        if (nextArg == null || nextArg2 == null) {
            getErrPrintWriter().println("Must provide both target and installer package names");
            return 1;
        }
        this.mInterface.setInstallerPackageName(nextArg, nextArg2);
        getOutPrintWriter().println("Success");
        return 0;
    }

    private int runGetInstantAppResolver() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        try {
            ComponentName instantAppResolverComponent = this.mInterface.getInstantAppResolverComponent();
            if (instantAppResolverComponent == null) {
                return 1;
            }
            outPrintWriter.println(instantAppResolverComponent.flattenToString());
            return 0;
        } catch (Exception e) {
            outPrintWriter.println(e.toString());
            return 1;
        }
    }

    private int runHasFeature() {
        int i;
        PrintWriter errPrintWriter = getErrPrintWriter();
        String nextArg = getNextArg();
        if (nextArg == null) {
            errPrintWriter.println("Error: expected FEATURE name");
            return 1;
        }
        String nextArg2 = getNextArg();
        if (nextArg2 == null) {
            i = 0;
        } else {
            try {
                i = Integer.parseInt(nextArg2);
            } catch (RemoteException e) {
                errPrintWriter.println(e.toString());
                return 1;
            } catch (NumberFormatException e2) {
                errPrintWriter.println("Error: illegal version number " + nextArg2);
                return 1;
            }
        }
        boolean zHasSystemFeature = this.mInterface.hasSystemFeature(nextArg, i);
        getOutPrintWriter().println(zHasSystemFeature);
        return !zHasSystemFeature ? 1 : 0;
    }

    private int runDump() {
        String nextArg = getNextArg();
        if (nextArg == null) {
            getErrPrintWriter().println("Error: no package specified");
            return 1;
        }
        ActivityManager.dumpPackageStateStatic(getOutFileDescriptor(), nextArg);
        return 0;
    }

    private int runSetHarmfulAppWarning() throws RemoteException {
        int userArg = -2;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (nextOption.equals("--user")) {
                    userArg = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    getErrPrintWriter().println("Error: Unknown option: " + nextOption);
                    return -1;
                }
            } else {
                this.mInterface.setHarmfulAppWarning(getNextArgRequired(), getNextArg(), translateUserId(userArg, false, "runSetHarmfulAppWarning"));
                return 0;
            }
        }
    }

    private int runGetHarmfulAppWarning() throws RemoteException {
        int userArg = -2;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (nextOption.equals("--user")) {
                    userArg = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    getErrPrintWriter().println("Error: Unknown option: " + nextOption);
                    return -1;
                }
            } else {
                CharSequence harmfulAppWarning = this.mInterface.getHarmfulAppWarning(getNextArgRequired(), translateUserId(userArg, false, "runGetHarmfulAppWarning"));
                if (!TextUtils.isEmpty(harmfulAppWarning)) {
                    getOutPrintWriter().println(harmfulAppWarning);
                    return 0;
                }
                return 1;
            }
        }
    }

    private static String checkAbiArgument(String str) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("Missing ABI argument");
        }
        if (STDIN_PATH.equals(str)) {
            return str;
        }
        for (String str2 : Build.SUPPORTED_ABIS) {
            if (str2.equals(str)) {
                return str;
            }
        }
        throw new IllegalArgumentException("ABI " + str + " not supported on this device");
    }

    private int translateUserId(int i, boolean z, String str) {
        return ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i, z, true, str, "pm command");
    }

    private int doCreateSession(PackageInstaller.SessionParams sessionParams, String str, int i) throws RemoteException {
        int iTranslateUserId = translateUserId(i, true, "runInstallCreate");
        if (iTranslateUserId == -1) {
            iTranslateUserId = 0;
            sessionParams.installFlags |= 64;
        }
        return this.mInterface.getPackageInstaller().createSession(sessionParams, str, iTranslateUserId);
    }

    private int doWriteSplit(int i, String str, long j, String str2, boolean z) throws Throwable {
        ParcelFileDescriptor parcelFileDescriptor;
        long j2;
        PackageInstaller.Session session;
        PrintWriter outPrintWriter = getOutPrintWriter();
        if (!STDIN_PATH.equals(str) && str != null) {
            ParcelFileDescriptor parcelFileDescriptorOpenFileForSystem = openFileForSystem(str, "r");
            if (parcelFileDescriptorOpenFileForSystem == null) {
                return -1;
            }
            long statSize = parcelFileDescriptorOpenFileForSystem.getStatSize();
            if (statSize < 0) {
                getErrPrintWriter().println("Unable to get size of: " + str);
                return -1;
            }
            parcelFileDescriptor = parcelFileDescriptorOpenFileForSystem;
            j2 = statSize;
            if (j2 > 0) {
                getErrPrintWriter().println("Error: must specify a APK size");
                return 1;
            }
            PackageInstaller.Session session2 = null;
            try {
                try {
                    session = new PackageInstaller.Session(this.mInterface.getPackageInstaller().openSession(i));
                } catch (Throwable th) {
                    th = th;
                }
            } catch (IOException e) {
                e = e;
            }
            try {
                session.write(str2, 0L, j2, parcelFileDescriptor);
                if (z) {
                    outPrintWriter.println("Success: streamed " + j2 + " bytes");
                }
                IoUtils.closeQuietly(session);
                return 0;
            } catch (IOException e2) {
                e = e2;
                session2 = session;
                getErrPrintWriter().println("Error: failed to write; " + e.getMessage());
                IoUtils.closeQuietly(session2);
                return 1;
            } catch (Throwable th2) {
                th = th2;
                session2 = session;
                IoUtils.closeQuietly(session2);
                throw th;
            }
        }
        ParcelFileDescriptor parcelFileDescriptor2 = new ParcelFileDescriptor(getInFileDescriptor());
        j2 = j;
        parcelFileDescriptor = parcelFileDescriptor2;
        if (j2 > 0) {
        }
    }

    private int doRemoveSplit(int i, String str, boolean z) throws Throwable {
        PackageInstaller.Session session;
        PrintWriter outPrintWriter = getOutPrintWriter();
        PackageInstaller.Session session2 = null;
        try {
            try {
                session = new PackageInstaller.Session(this.mInterface.getPackageInstaller().openSession(i));
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e) {
            e = e;
        }
        try {
            session.removeSplit(str);
            if (z) {
                outPrintWriter.println("Success");
            }
            IoUtils.closeQuietly(session);
            return 0;
        } catch (IOException e2) {
            e = e2;
            session2 = session;
            outPrintWriter.println("Error: failed to remove split; " + e.getMessage());
            IoUtils.closeQuietly(session2);
            return 1;
        } catch (Throwable th2) {
            th = th2;
            session2 = session;
            IoUtils.closeQuietly(session2);
            throw th;
        }
    }

    private int doCommitSession(int i, boolean z) throws Throwable {
        PackageInstaller.Session session;
        PrintWriter outPrintWriter = getOutPrintWriter();
        try {
            session = new PackageInstaller.Session(this.mInterface.getPackageInstaller().openSession(i));
        } catch (Throwable th) {
            th = th;
            session = null;
        }
        try {
            try {
                DexMetadataHelper.validateDexPaths(session.getNames());
            } catch (IOException | IllegalStateException e) {
                outPrintWriter.println("Warning [Could not validate the dex paths: " + e.getMessage() + "]");
            }
            LocalIntentReceiver localIntentReceiver = new LocalIntentReceiver();
            session.commit(localIntentReceiver.getIntentSender());
            Intent result = localIntentReceiver.getResult(DisplayTransformManager.LEVEL_COLOR_MATRIX_INVERT_COLOR);
            if (result == null) {
                outPrintWriter.println("Failure [install timeout]");
                IoUtils.closeQuietly(session);
                return 1;
            }
            int intExtra = result.getIntExtra("android.content.pm.extra.STATUS", 1);
            if (intExtra != 0) {
                outPrintWriter.println("Failure [" + result.getStringExtra("android.content.pm.extra.STATUS_MESSAGE") + "]");
            } else if (z) {
                outPrintWriter.println("Success");
            }
            IoUtils.closeQuietly(session);
            return intExtra;
        } catch (Throwable th2) {
            th = th2;
            IoUtils.closeQuietly(session);
            throw th;
        }
    }

    private int doAbandonSession(int i, boolean z) throws Throwable {
        PrintWriter outPrintWriter = getOutPrintWriter();
        PackageInstaller.Session session = null;
        try {
            PackageInstaller.Session session2 = new PackageInstaller.Session(this.mInterface.getPackageInstaller().openSession(i));
            try {
                session2.abandon();
                if (z) {
                    outPrintWriter.println("Success");
                }
                IoUtils.closeQuietly(session2);
                return 0;
            } catch (Throwable th) {
                th = th;
                session = session2;
                IoUtils.closeQuietly(session);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private void doListPermissions(ArrayList<String> arrayList, boolean z, boolean z2, boolean z3, int i, int i2) throws RemoteException {
        int i3;
        ArrayList<String> arrayList2 = arrayList;
        PrintWriter outPrintWriter = getOutPrintWriter();
        int size = arrayList.size();
        int i4 = 0;
        int i5 = 0;
        while (i5 < size) {
            String str = arrayList2.get(i5);
            String str2 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            if (z) {
                if (i5 > 0) {
                    outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                }
                if (str != null) {
                    PermissionGroupInfo permissionGroupInfo = this.mInterface.getPermissionGroupInfo(str, i4);
                    if (z3) {
                        if (getResources(permissionGroupInfo) != null) {
                            outPrintWriter.print(loadText(permissionGroupInfo, permissionGroupInfo.labelRes, permissionGroupInfo.nonLocalizedLabel) + ": ");
                        } else {
                            outPrintWriter.print(permissionGroupInfo.name + ": ");
                        }
                    } else {
                        StringBuilder sb = new StringBuilder();
                        sb.append(z2 ? "+ " : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                        sb.append("group:");
                        sb.append(permissionGroupInfo.name);
                        outPrintWriter.println(sb.toString());
                        if (z2) {
                            outPrintWriter.println("  package:" + permissionGroupInfo.packageName);
                            if (getResources(permissionGroupInfo) != null) {
                                outPrintWriter.println("  label:" + loadText(permissionGroupInfo, permissionGroupInfo.labelRes, permissionGroupInfo.nonLocalizedLabel));
                                outPrintWriter.println("  description:" + loadText(permissionGroupInfo, permissionGroupInfo.descriptionRes, permissionGroupInfo.nonLocalizedDescription));
                            }
                        }
                    }
                } else {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append((!z2 || z3) ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : "+ ");
                    sb2.append("ungrouped:");
                    outPrintWriter.println(sb2.toString());
                }
                str2 = "  ";
            }
            List list = this.mInterface.queryPermissionsByGroup(arrayList2.get(i5), i4).getList();
            int size2 = list.size();
            boolean z4 = true;
            for (int i6 = i4; i6 < size2; i6++) {
                PermissionInfo permissionInfo = (PermissionInfo) list.get(i6);
                if ((!z || str != null || permissionInfo.group == null) && (i3 = permissionInfo.protectionLevel & 15) >= i && i3 <= i2) {
                    if (z3) {
                        if (!z4) {
                            outPrintWriter.print(", ");
                        } else {
                            z4 = false;
                        }
                        if (getResources(permissionInfo) != null) {
                            outPrintWriter.print(loadText(permissionInfo, permissionInfo.labelRes, permissionInfo.nonLocalizedLabel));
                        } else {
                            outPrintWriter.print(permissionInfo.name);
                        }
                    } else {
                        StringBuilder sb3 = new StringBuilder();
                        sb3.append(str2);
                        sb3.append(z2 ? "+ " : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                        sb3.append("permission:");
                        sb3.append(permissionInfo.name);
                        outPrintWriter.println(sb3.toString());
                        if (z2) {
                            outPrintWriter.println(str2 + "  package:" + permissionInfo.packageName);
                            if (getResources(permissionInfo) != null) {
                                outPrintWriter.println(str2 + "  label:" + loadText(permissionInfo, permissionInfo.labelRes, permissionInfo.nonLocalizedLabel));
                                outPrintWriter.println(str2 + "  description:" + loadText(permissionInfo, permissionInfo.descriptionRes, permissionInfo.nonLocalizedDescription));
                            }
                            outPrintWriter.println(str2 + "  protectionLevel:" + PermissionInfo.protectionToString(permissionInfo.protectionLevel));
                        }
                    }
                }
            }
            if (z3) {
                outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            }
            i5++;
            arrayList2 = arrayList;
            i4 = 0;
        }
    }

    private String loadText(PackageItemInfo packageItemInfo, int i, CharSequence charSequence) throws RemoteException {
        Resources resources;
        if (charSequence != null) {
            return charSequence.toString();
        }
        if (i != 0 && (resources = getResources(packageItemInfo)) != null) {
            try {
                return resources.getString(i);
            } catch (Resources.NotFoundException e) {
                return null;
            }
        }
        return null;
    }

    private Resources getResources(PackageItemInfo packageItemInfo) throws RemoteException {
        Resources resources = this.mResourceCache.get(packageItemInfo.packageName);
        if (resources != null) {
            return resources;
        }
        ApplicationInfo applicationInfo = this.mInterface.getApplicationInfo(packageItemInfo.packageName, 0, 0);
        AssetManager assetManager = new AssetManager();
        assetManager.addAssetPath(applicationInfo.publicSourceDir);
        Resources resources2 = new Resources(assetManager, null, null);
        this.mResourceCache.put(packageItemInfo.packageName, resources2);
        return resources2;
    }

    public void onHelp() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        outPrintWriter.println("Package manager (package) commands:");
        outPrintWriter.println("  help");
        outPrintWriter.println("    Print this help text.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  path [--user USER_ID] PACKAGE");
        outPrintWriter.println("    Print the path to the .apk of the given PACKAGE.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  dump PACKAGE");
        outPrintWriter.println("    Print various system state associated with the given PACKAGE.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  list features");
        outPrintWriter.println("    Prints all features of the system.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  has-feature FEATURE_NAME [version]");
        outPrintWriter.println("    Prints true and returns exit status 0 when system has a FEATURE_NAME,");
        outPrintWriter.println("    otherwise prints false and returns exit status 1");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  list instrumentation [-f] [TARGET-PACKAGE]");
        outPrintWriter.println("    Prints all test packages; optionally only those targeting TARGET-PACKAGE");
        outPrintWriter.println("    Options:");
        outPrintWriter.println("      -f: dump the name of the .apk file containing the test package");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  list libraries");
        outPrintWriter.println("    Prints all system libraries.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  list packages [-f] [-d] [-e] [-s] [-3] [-i] [-l] [-u] [-U] ");
        outPrintWriter.println("      [--uid UID] [--user USER_ID] [FILTER]");
        outPrintWriter.println("    Prints all packages; optionally only those whose name contains");
        outPrintWriter.println("    the text in FILTER.  Options are:");
        outPrintWriter.println("      -f: see their associated file");
        outPrintWriter.println("      -d: filter to only show disabled packages");
        outPrintWriter.println("      -e: filter to only show enabled packages");
        outPrintWriter.println("      -s: filter to only show system packages");
        outPrintWriter.println("      -3: filter to only show third party packages");
        outPrintWriter.println("      -i: see the installer for the packages");
        outPrintWriter.println("      -l: ignored (used for compatibility with older releases)");
        outPrintWriter.println("      -U: also show the package UID");
        outPrintWriter.println("      -u: also include uninstalled packages");
        outPrintWriter.println("      --uid UID: filter to only show packages with the given UID");
        outPrintWriter.println("      --user USER_ID: only list packages belonging to the given user");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  list permission-groups");
        outPrintWriter.println("    Prints all known permission groups.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  list permissions [-g] [-f] [-d] [-u] [GROUP]");
        outPrintWriter.println("    Prints all known permissions; optionally only those in GROUP.  Options are:");
        outPrintWriter.println("      -g: organize by group");
        outPrintWriter.println("      -f: print all information");
        outPrintWriter.println("      -s: short summary");
        outPrintWriter.println("      -d: only list dangerous permissions");
        outPrintWriter.println("      -u: list only the permissions users will see");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  resolve-activity [--brief] [--components] [--user USER_ID] INTENT");
        outPrintWriter.println("    Prints the activity that resolves to the given INTENT.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  query-activities [--brief] [--components] [--user USER_ID] INTENT");
        outPrintWriter.println("    Prints all activities that can handle the given INTENT.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  query-services [--brief] [--components] [--user USER_ID] INTENT");
        outPrintWriter.println("    Prints all services that can handle the given INTENT.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  query-receivers [--brief] [--components] [--user USER_ID] INTENT");
        outPrintWriter.println("    Prints all broadcast receivers that can handle the given INTENT.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  install [-lrtsfdg] [-i PACKAGE] [--user USER_ID|all|current]");
        outPrintWriter.println("       [-p INHERIT_PACKAGE] [--install-location 0/1/2]");
        outPrintWriter.println("       [--originating-uri URI] [---referrer URI]");
        outPrintWriter.println("       [--abi ABI_NAME] [--force-sdk]");
        outPrintWriter.println("       [--preload] [--instantapp] [--full] [--dont-kill]");
        outPrintWriter.println("       [--force-uuid internal|UUID] [--pkg PACKAGE] [-S BYTES] [PATH|-]");
        outPrintWriter.println("    Install an application.  Must provide the apk data to install, either as a");
        outPrintWriter.println("    file path or '-' to read from stdin.  Options are:");
        outPrintWriter.println("      -l: forward lock application");
        outPrintWriter.println("      -R: disallow replacement of existing application");
        outPrintWriter.println("      -t: allow test packages");
        outPrintWriter.println("      -i: specify package name of installer owning the app");
        outPrintWriter.println("      -s: install application on sdcard");
        outPrintWriter.println("      -f: install application on internal flash");
        outPrintWriter.println("      -d: allow version code downgrade (debuggable packages only)");
        outPrintWriter.println("      -p: partial application install (new split on top of existing pkg)");
        outPrintWriter.println("      -g: grant all runtime permissions");
        outPrintWriter.println("      -S: size in bytes of package, required for stdin");
        outPrintWriter.println("      --user: install under the given user.");
        outPrintWriter.println("      --dont-kill: installing a new feature split, don't kill running app");
        outPrintWriter.println("      --originating-uri: set URI where app was downloaded from");
        outPrintWriter.println("      --referrer: set URI that instigated the install of the app");
        outPrintWriter.println("      --pkg: specify expected package name of app being installed");
        outPrintWriter.println("      --abi: override the default ABI of the platform");
        outPrintWriter.println("      --instantapp: cause the app to be installed as an ephemeral install app");
        outPrintWriter.println("      --full: cause the app to be installed as a non-ephemeral full app");
        outPrintWriter.println("      --install-location: force the install location:");
        outPrintWriter.println("          0=auto, 1=internal only, 2=prefer external");
        outPrintWriter.println("      --force-uuid: force install on to disk volume with given UUID");
        outPrintWriter.println("      --force-sdk: allow install even when existing app targets platform");
        outPrintWriter.println("          codename but new one targets a final API level");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  install-create [-lrtsfdg] [-i PACKAGE] [--user USER_ID|all|current]");
        outPrintWriter.println("       [-p INHERIT_PACKAGE] [--install-location 0/1/2]");
        outPrintWriter.println("       [--originating-uri URI] [---referrer URI]");
        outPrintWriter.println("       [--abi ABI_NAME] [--force-sdk]");
        outPrintWriter.println("       [--preload] [--instantapp] [--full] [--dont-kill]");
        outPrintWriter.println("       [--force-uuid internal|UUID] [--pkg PACKAGE] [-S BYTES]");
        outPrintWriter.println("    Like \"install\", but starts an install session.  Use \"install-write\"");
        outPrintWriter.println("    to push data into the session, and \"install-commit\" to finish.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  install-write [-S BYTES] SESSION_ID SPLIT_NAME [PATH|-]");
        outPrintWriter.println("    Write an apk into the given install session.  If the path is '-', data");
        outPrintWriter.println("    will be read from stdin.  Options are:");
        outPrintWriter.println("      -S: size in bytes of package, required for stdin");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  install-commit SESSION_ID");
        outPrintWriter.println("    Commit the given active install session, installing the app.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  install-abandon SESSION_ID");
        outPrintWriter.println("    Delete the given active install session.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  set-install-location LOCATION");
        outPrintWriter.println("    Changes the default install location.  NOTE this is only intended for debugging;");
        outPrintWriter.println("    using this can cause applications to break and other undersireable behavior.");
        outPrintWriter.println("    LOCATION is one of:");
        outPrintWriter.println("    0 [auto]: Let system decide the best location");
        outPrintWriter.println("    1 [internal]: Install on internal device storage");
        outPrintWriter.println("    2 [external]: Install on external media");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  get-install-location");
        outPrintWriter.println("    Returns the current install location: 0, 1 or 2 as per set-install-location.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  move-package PACKAGE [internal|UUID]");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  move-primary-storage [internal|UUID]");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  pm uninstall [-k] [--user USER_ID] [--versionCode VERSION_CODE] PACKAGE [SPLIT]");
        outPrintWriter.println("    Remove the given package name from the system.  May remove an entire app");
        outPrintWriter.println("    if no SPLIT name is specified, otherwise will remove only the split of the");
        outPrintWriter.println("    given app.  Options are:");
        outPrintWriter.println("      -k: keep the data and cache directories around after package removal.");
        outPrintWriter.println("      --user: remove the app from the given user.");
        outPrintWriter.println("      --versionCode: only uninstall if the app has the given version code.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  clear [--user USER_ID] PACKAGE");
        outPrintWriter.println("    Deletes all data associated with a package.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  enable [--user USER_ID] PACKAGE_OR_COMPONENT");
        outPrintWriter.println("  disable [--user USER_ID] PACKAGE_OR_COMPONENT");
        outPrintWriter.println("  disable-user [--user USER_ID] PACKAGE_OR_COMPONENT");
        outPrintWriter.println("  disable-until-used [--user USER_ID] PACKAGE_OR_COMPONENT");
        outPrintWriter.println("  default-state [--user USER_ID] PACKAGE_OR_COMPONENT");
        outPrintWriter.println("    These commands change the enabled state of a given package or");
        outPrintWriter.println("    component (written as \"package/class\").");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  hide [--user USER_ID] PACKAGE_OR_COMPONENT");
        outPrintWriter.println("  unhide [--user USER_ID] PACKAGE_OR_COMPONENT");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  suspend [--user USER_ID] TARGET-PACKAGE");
        outPrintWriter.println("    Suspends the specified package (as user).");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  unsuspend [--user USER_ID] TARGET-PACKAGE");
        outPrintWriter.println("    Unsuspends the specified package (as user).");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  grant [--user USER_ID] PACKAGE PERMISSION");
        outPrintWriter.println("  revoke [--user USER_ID] PACKAGE PERMISSION");
        outPrintWriter.println("    These commands either grant or revoke permissions to apps.  The permissions");
        outPrintWriter.println("    must be declared as used in the app's manifest, be runtime permissions");
        outPrintWriter.println("    (protection level dangerous), and the app targeting SDK greater than Lollipop MR1.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  reset-permissions");
        outPrintWriter.println("    Revert all runtime permissions to their default state.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  set-permission-enforced PERMISSION [true|false]");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  get-privapp-permissions TARGET-PACKAGE");
        outPrintWriter.println("    Prints all privileged permissions for a package.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  get-privapp-deny-permissions TARGET-PACKAGE");
        outPrintWriter.println("    Prints all privileged permissions that are denied for a package.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  get-oem-permissions TARGET-PACKAGE");
        outPrintWriter.println("    Prints all OEM permissions for a package.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  set-app-link [--user USER_ID] PACKAGE {always|ask|never|undefined}");
        outPrintWriter.println("  get-app-link [--user USER_ID] PACKAGE");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  trim-caches DESIRED_FREE_SPACE [internal|UUID]");
        outPrintWriter.println("    Trim cache files to reach the given free space.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  create-user [--profileOf USER_ID] [--managed] [--restricted] [--ephemeral]");
        outPrintWriter.println("      [--guest] USER_NAME");
        outPrintWriter.println("    Create a new user with the given USER_NAME, printing the new user identifier");
        outPrintWriter.println("    of the user.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  remove-user USER_ID");
        outPrintWriter.println("    Remove the user with the given USER_IDENTIFIER, deleting all data");
        outPrintWriter.println("    associated with that user");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  set-user-restriction [--user USER_ID] RESTRICTION VALUE");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  get-max-users");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  get-max-running-users");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  compile [-m MODE | -r REASON] [-f] [-c] [--split SPLIT_NAME]");
        outPrintWriter.println("          [--reset] [--check-prof (true | false)] (-a | TARGET-PACKAGE)");
        outPrintWriter.println("    Trigger compilation of TARGET-PACKAGE or all packages if \"-a\".  Options are:");
        outPrintWriter.println("      -a: compile all packages");
        outPrintWriter.println("      -c: clear profile data before compiling");
        outPrintWriter.println("      -f: force compilation even if not needed");
        outPrintWriter.println("      -m: select compilation mode");
        outPrintWriter.println("          MODE is one of the dex2oat compiler filters:");
        outPrintWriter.println("            assume-verified");
        outPrintWriter.println("            extract");
        outPrintWriter.println("            verify");
        outPrintWriter.println("            quicken");
        outPrintWriter.println("            space-profile");
        outPrintWriter.println("            space");
        outPrintWriter.println("            speed-profile");
        outPrintWriter.println("            speed");
        outPrintWriter.println("            everything");
        outPrintWriter.println("      -r: select compilation reason");
        outPrintWriter.println("          REASON is one of:");
        for (int i = 0; i < PackageManagerServiceCompilerMapping.REASON_STRINGS.length; i++) {
            outPrintWriter.println("            " + PackageManagerServiceCompilerMapping.REASON_STRINGS[i]);
        }
        outPrintWriter.println("      --reset: restore package to its post-install state");
        outPrintWriter.println("      --check-prof (true | false): look at profiles when doing dexopt?");
        outPrintWriter.println("      --secondary-dex: compile app secondary dex files");
        outPrintWriter.println("      --split SPLIT: compile only the given split name");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  force-dex-opt PACKAGE");
        outPrintWriter.println("    Force immediate execution of dex opt for the given PACKAGE.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  bg-dexopt-job");
        outPrintWriter.println("    Execute the background optimizations immediately.");
        outPrintWriter.println("    Note that the command only runs the background optimizer logic. It may");
        outPrintWriter.println("    overlap with the actual job but the job scheduler will not be able to");
        outPrintWriter.println("    cancel it. It will also run even if the device is not in the idle");
        outPrintWriter.println("    maintenance mode.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  reconcile-secondary-dex-files TARGET-PACKAGE");
        outPrintWriter.println("    Reconciles the package secondary dex files with the generated oat files.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  dump-profiles TARGET-PACKAGE");
        outPrintWriter.println("    Dumps method/class profile files to");
        outPrintWriter.println("    /data/misc/profman/TARGET-PACKAGE.txt");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  snapshot-profile TARGET-PACKAGE [--code-path path]");
        outPrintWriter.println("    Take a snapshot of the package profiles to");
        outPrintWriter.println("    /data/misc/profman/TARGET-PACKAGE[-code-path].prof");
        outPrintWriter.println("    If TARGET-PACKAGE=android it will take a snapshot of the boot image");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  set-home-activity [--user USER_ID] TARGET-COMPONENT");
        outPrintWriter.println("    Set the default home activity (aka launcher).");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  set-installer PACKAGE INSTALLER");
        outPrintWriter.println("    Set installer package name");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  get-instantapp-resolver");
        outPrintWriter.println("    Return the name of the component that is the current instant app installer.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  set-harmful-app-warning [--user <USER_ID>] <PACKAGE> [<WARNING>]");
        outPrintWriter.println("    Mark the app as harmful with the given warning message.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  get-harmful-app-warning [--user <USER_ID>] <PACKAGE>");
        outPrintWriter.println("    Return the harmful app warning message for the given app, if present");
        outPrintWriter.println();
        outPrintWriter.println("  uninstall-system-updates");
        outPrintWriter.println("    Remove updates to all system applications and fall back to their /system version.");
        outPrintWriter.println();
        Intent.printIntentArgsHelp(outPrintWriter, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
    }

    private static class LocalIntentReceiver {
        private IIntentSender.Stub mLocalSender;
        private final SynchronousQueue<Intent> mResult;

        private LocalIntentReceiver() {
            this.mResult = new SynchronousQueue<>();
            this.mLocalSender = new IIntentSender.Stub() {
                public void send(int i, Intent intent, String str, IBinder iBinder, IIntentReceiver iIntentReceiver, String str2, Bundle bundle) {
                    try {
                        LocalIntentReceiver.this.mResult.offer(intent, 5L, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }

        public IntentSender getIntentSender() {
            return new IntentSender(this.mLocalSender);
        }

        public Intent getResult() {
            try {
                return this.mResult.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public Intent getResult(int i) {
            try {
                return this.mResult.poll(i, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
