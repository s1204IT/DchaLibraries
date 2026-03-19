package com.android.server.pm;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.IPackageManager;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageItemInfo;
import android.content.pm.ParceledListSlice;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.PrintWriterPrinter;
import com.android.internal.util.SizedInputStream;
import com.android.server.pm.PackageManagerService;
import dalvik.system.DexFile;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;

class PackageManagerShellCommand extends ShellCommand {
    boolean mBrief;
    boolean mComponents;
    final IPackageManager mInterface;
    private final WeakHashMap<String, Resources> mResourceCache = new WeakHashMap<>();
    int mTargetUser;

    PackageManagerShellCommand(PackageManagerService service) {
        this.mInterface = service;
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        try {
            return cmd.equals("install") ? runInstall() : (cmd.equals("install-abandon") || cmd.equals("install-destroy")) ? runInstallAbandon() : cmd.equals("install-commit") ? runInstallCommit() : cmd.equals("install-create") ? runInstallCreate() : cmd.equals("install-remove") ? runInstallRemove() : cmd.equals("install-write") ? runInstallWrite() : cmd.equals("compile") ? runCompile() : cmd.equals("dump-profiles") ? runDumpProfiles() : cmd.equals("list") ? runList() : cmd.equals("uninstall") ? runUninstall() : cmd.equals("resolve-activity") ? runResolveActivity() : cmd.equals("query-activities") ? runQueryIntentActivities() : cmd.equals("query-services") ? runQueryIntentServices() : cmd.equals("query-receivers") ? runQueryIntentReceivers() : cmd.equals("suspend") ? runSuspend(true) : cmd.equals("unsuspend") ? runSuspend(false) : cmd.equals("set-home-activity") ? runSetHomeActivity() : handleDefaultCommands(cmd);
        } catch (RemoteException e) {
            pw.println("Remote exception: " + e);
            return -1;
        }
    }

    private int runInstall() throws Throwable {
        PrintWriter pw = getOutPrintWriter();
        InstallParams params = makeInstallParams();
        int sessionId = doCreateSession(params.sessionParams, params.installerPackageName, params.userId);
        try {
            String inPath = getNextArg();
            if (inPath == null && params.sessionParams.sizeBytes == 0) {
                pw.println("Error: must either specify a package size or an APK file");
                if (1 != 0) {
                    try {
                        doAbandonSession(sessionId, false);
                    } catch (Exception e) {
                    }
                }
                return 1;
            }
            if (doWriteSplit(sessionId, inPath, params.sessionParams.sizeBytes, "base.apk", false) != 0) {
                if (1 != 0) {
                    try {
                        doAbandonSession(sessionId, false);
                    } catch (Exception e2) {
                    }
                }
                return 1;
            }
            if (doCommitSession(sessionId, false) != 0) {
                if (1 != 0) {
                    try {
                        doAbandonSession(sessionId, false);
                    } catch (Exception e3) {
                    }
                }
                return 1;
            }
            pw.println("Success");
            if (0 != 0) {
                try {
                    doAbandonSession(sessionId, false);
                } catch (Exception e4) {
                }
            }
            return 0;
        } catch (Throwable th) {
            if (1 != 0) {
                try {
                    doAbandonSession(sessionId, false);
                } catch (Exception e5) {
                }
            }
            throw th;
        }
    }

    private int runSuspend(boolean suspendedState) {
        PrintWriter pw = getOutPrintWriter();
        int userId = 0;
        while (true) {
            String opt = getNextOption();
            if (opt != null) {
                if (opt.equals("--user")) {
                    userId = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    pw.println("Error: Unknown option: " + opt);
                    return 1;
                }
            } else {
                String packageName = getNextArg();
                if (packageName == null) {
                    pw.println("Error: package name not specified");
                    return 1;
                }
                try {
                    this.mInterface.setPackagesSuspendedAsUser(new String[]{packageName}, suspendedState, userId);
                    pw.println("Package " + packageName + " new suspended state: " + this.mInterface.isPackageSuspendedForUser(packageName, userId));
                    return 0;
                } catch (RemoteException | IllegalArgumentException e) {
                    pw.println(e.toString());
                    return 1;
                }
            }
        }
    }

    private int runInstallAbandon() throws RemoteException {
        int sessionId = Integer.parseInt(getNextArg());
        return doAbandonSession(sessionId, true);
    }

    private int runInstallCommit() throws RemoteException {
        int sessionId = Integer.parseInt(getNextArg());
        return doCommitSession(sessionId, true);
    }

    private int runInstallCreate() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        InstallParams installParams = makeInstallParams();
        int sessionId = doCreateSession(installParams.sessionParams, installParams.installerPackageName, installParams.userId);
        pw.println("Success: created install session [" + sessionId + "]");
        return 0;
    }

    private int runInstallWrite() throws RemoteException {
        long sizeBytes = -1;
        while (true) {
            String opt = getNextOption();
            if (opt != null) {
                if (opt.equals("-S")) {
                    sizeBytes = Long.parseLong(getNextArg());
                } else {
                    throw new IllegalArgumentException("Unknown option: " + opt);
                }
            } else {
                int sessionId = Integer.parseInt(getNextArg());
                String splitName = getNextArg();
                String path = getNextArg();
                return doWriteSplit(sessionId, path, sizeBytes, splitName, true);
            }
        }
    }

    private int runInstallRemove() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        int sessionId = Integer.parseInt(getNextArg());
        String splitName = getNextArg();
        if (splitName == null) {
            pw.println("Error: split name not specified");
            return 1;
        }
        return doRemoveSplit(sessionId, splitName, true);
    }

    private int runCompile() throws RemoteException {
        String targetCompilerFilter;
        List<String> packageNames;
        PrintWriter pw = getOutPrintWriter();
        boolean checkProfiles = SystemProperties.getBoolean("dalvik.vm.usejitprofiles", false);
        boolean forceCompilation = false;
        boolean allPackages = false;
        boolean clearProfileData = false;
        String compilerFilter = null;
        String compilationReason = null;
        String nextArgRequired = null;
        while (true) {
            String opt = getNextOption();
            if (opt == null) {
                if (nextArgRequired != null) {
                    if ("true".equals(nextArgRequired)) {
                        checkProfiles = true;
                    } else {
                        if (!"false".equals(nextArgRequired)) {
                            pw.println("Invalid value for \"--check-prof\". Expected \"true\" or \"false\".");
                            return 1;
                        }
                        checkProfiles = false;
                    }
                }
                if (compilerFilter != null && compilationReason != null) {
                    pw.println("Cannot use compilation filter (\"-m\") and compilation reason (\"-r\") at the same time");
                    return 1;
                }
                if (compilerFilter == null && compilationReason == null) {
                    pw.println("Cannot run without any of compilation filter (\"-m\") and compilation reason (\"-r\") at the same time");
                    return 1;
                }
                if (compilerFilter == null) {
                    int reason = -1;
                    int i = 0;
                    while (true) {
                        if (i >= PackageManagerServiceCompilerMapping.REASON_STRINGS.length) {
                            break;
                        }
                        if (PackageManagerServiceCompilerMapping.REASON_STRINGS[i].equals(compilationReason)) {
                            reason = i;
                            break;
                        }
                        i++;
                    }
                    if (reason == -1) {
                        pw.println("Error: Unknown compilation reason: " + compilationReason);
                        return 1;
                    }
                    targetCompilerFilter = PackageManagerServiceCompilerMapping.getCompilerFilterForReason(reason);
                } else {
                    if (!DexFile.isValidCompilerFilter(compilerFilter)) {
                        pw.println("Error: \"" + compilerFilter + "\" is not a valid compilation filter.");
                        return 1;
                    }
                    targetCompilerFilter = compilerFilter;
                }
                if (allPackages) {
                    packageNames = this.mInterface.getAllPackages();
                } else {
                    String packageName = getNextArg();
                    if (packageName == null) {
                        pw.println("Error: package name not specified");
                        return 1;
                    }
                    packageNames = Collections.singletonList(packageName);
                }
                List<String> failedPackages = new ArrayList<>();
                for (String packageName2 : packageNames) {
                    if (clearProfileData) {
                        this.mInterface.clearApplicationProfileData(packageName2);
                    }
                    boolean result = this.mInterface.performDexOptMode(packageName2, checkProfiles, targetCompilerFilter, forceCompilation);
                    if (!result) {
                        failedPackages.add(packageName2);
                    }
                }
                if (failedPackages.isEmpty()) {
                    pw.println("Success");
                    return 0;
                }
                if (failedPackages.size() == 1) {
                    pw.println("Failure: package " + failedPackages.get(0) + " could not be compiled");
                    return 1;
                }
                pw.print("Failure: the following packages could not be compiled: ");
                boolean is_first = true;
                for (String packageName3 : failedPackages) {
                    if (is_first) {
                        is_first = false;
                    } else {
                        pw.print(", ");
                    }
                    pw.print(packageName3);
                }
                pw.println();
                return 1;
            }
            if (opt.equals("-a")) {
                allPackages = true;
            } else if (opt.equals("-c")) {
                clearProfileData = true;
            } else if (opt.equals("-f")) {
                forceCompilation = true;
            } else if (opt.equals("-m")) {
                compilerFilter = getNextArgRequired();
            } else if (opt.equals("-r")) {
                compilationReason = getNextArgRequired();
            } else if (opt.equals("--check-prof")) {
                nextArgRequired = getNextArgRequired();
            } else {
                if (!opt.equals("--reset")) {
                    pw.println("Error: Unknown option: " + opt);
                    return 1;
                }
                forceCompilation = true;
                clearProfileData = true;
                compilationReason = "install";
            }
        }
    }

    private int runDumpProfiles() throws RemoteException {
        String packageName = getNextArg();
        this.mInterface.dumpProfiles(packageName);
        return 0;
    }

    private int runList() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        String type = getNextArg();
        if (type == null) {
            pw.println("Error: didn't specify type of data to list");
            return -1;
        }
        if (type.equals("features")) {
            return runListFeatures();
        }
        if (type.equals("instrumentation")) {
            return runListInstrumentation();
        }
        if (type.equals("libraries")) {
            return runListLibraries();
        }
        if (type.equals("package") || type.equals("packages")) {
            return runListPackages(false);
        }
        if (type.equals("permission-groups")) {
            return runListPermissionGroups();
        }
        if (type.equals("permissions")) {
            return runListPermissions();
        }
        pw.println("Error: unknown list type '" + type + "'");
        return -1;
    }

    private int runListFeatures() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        List<FeatureInfo> list = this.mInterface.getSystemAvailableFeatures().getList();
        Collections.sort(list, new Comparator<FeatureInfo>() {
            @Override
            public int compare(FeatureInfo o1, FeatureInfo o2) {
                if (o1.name == o2.name) {
                    return 0;
                }
                if (o1.name == null) {
                    return -1;
                }
                if (o2.name == null) {
                    return 1;
                }
                return o1.name.compareTo(o2.name);
            }
        });
        int count = list != null ? list.size() : 0;
        for (int p = 0; p < count; p++) {
            FeatureInfo fi = list.get(p);
            pw.print("feature:");
            if (fi.name != null) {
                pw.print(fi.name);
                if (fi.version > 0) {
                    pw.print("=");
                    pw.print(fi.version);
                }
                pw.println();
            } else {
                pw.println("reqGlEsVersion=0x" + Integer.toHexString(fi.reqGlEsVersion));
            }
        }
        return 0;
    }

    private int runListInstrumentation() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        boolean showSourceDir = false;
        String str = null;
        while (true) {
            try {
                String opt = getNextArg();
                if (opt != null) {
                    if (opt.equals("-f")) {
                        showSourceDir = true;
                    } else if (opt.charAt(0) != '-') {
                        str = opt;
                    } else {
                        pw.println("Error: Unknown option: " + opt);
                        return -1;
                    }
                } else {
                    List<InstrumentationInfo> list = this.mInterface.queryInstrumentation(str, 0).getList();
                    Collections.sort(list, new Comparator<InstrumentationInfo>() {
                        @Override
                        public int compare(InstrumentationInfo o1, InstrumentationInfo o2) {
                            return o1.targetPackage.compareTo(o2.targetPackage);
                        }
                    });
                    int count = list != null ? list.size() : 0;
                    for (int p = 0; p < count; p++) {
                        InstrumentationInfo ii = list.get(p);
                        pw.print("instrumentation:");
                        if (showSourceDir) {
                            pw.print(ii.sourceDir);
                            pw.print("=");
                        }
                        ComponentName cn = new ComponentName(ii.packageName, ii.name);
                        pw.print(cn.flattenToShortString());
                        pw.print(" (target=");
                        pw.print(ii.targetPackage);
                        pw.println(")");
                    }
                    return 0;
                }
            } catch (RuntimeException ex) {
                pw.println("Error: " + ex.toString());
                return -1;
            }
        }
    }

    private int runListLibraries() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        List<String> list = new ArrayList<>();
        String[] rawList = this.mInterface.getSystemSharedLibraryNames();
        for (String str : rawList) {
            list.add(str);
        }
        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if (o1 == o2) {
                    return 0;
                }
                if (o1 == null) {
                    return -1;
                }
                if (o2 == null) {
                    return 1;
                }
                return o1.compareTo(o2);
            }
        });
        int count = list != null ? list.size() : 0;
        for (int p = 0; p < count; p++) {
            String lib = list.get(p);
            pw.print("library:");
            pw.println(lib);
        }
        return 0;
    }

    private int runListPackages(boolean showSourceDir) throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        int getFlags = 0;
        boolean listDisabled = false;
        boolean listEnabled = false;
        boolean listSystem = false;
        boolean listThirdParty = false;
        boolean listInstaller = false;
        int userId = 0;
        while (true) {
            try {
                String opt = getNextOption();
                if (opt == null) {
                    String filter = getNextArg();
                    ParceledListSlice<PackageInfo> slice = this.mInterface.getInstalledPackages(getFlags, userId);
                    List<PackageInfo> packages = slice.getList();
                    int count = packages.size();
                    for (int p = 0; p < count; p++) {
                        PackageInfo info = packages.get(p);
                        if (filter == null || info.packageName.contains(filter)) {
                            boolean isSystem = (info.applicationInfo.flags & 1) != 0;
                            if ((!listDisabled || !info.applicationInfo.enabled) && ((!listEnabled || info.applicationInfo.enabled) && ((!listSystem || isSystem) && (!listThirdParty || !isSystem)))) {
                                pw.print("package:");
                                if (showSourceDir) {
                                    pw.print(info.applicationInfo.sourceDir);
                                    pw.print("=");
                                }
                                pw.print(info.packageName);
                                if (listInstaller) {
                                    pw.print("  installer=");
                                    pw.print(this.mInterface.getInstallerPackageName(info.packageName));
                                }
                                pw.println();
                            }
                        }
                    }
                    return 0;
                }
                if (opt.equals("-d")) {
                    listDisabled = true;
                } else if (opt.equals("-e")) {
                    listEnabled = true;
                } else if (opt.equals("-f")) {
                    showSourceDir = true;
                } else if (opt.equals("-i")) {
                    listInstaller = true;
                } else if (opt.equals("-l")) {
                    continue;
                } else if (opt.equals("-lf")) {
                    showSourceDir = true;
                } else if (opt.equals("-s")) {
                    listSystem = true;
                } else if (opt.equals("-u")) {
                    getFlags |= PackageManagerService.DumpState.DUMP_PREFERRED_XML;
                } else if (opt.equals("-3")) {
                    listThirdParty = true;
                } else {
                    if (!opt.equals("--user")) {
                        pw.println("Error: Unknown option: " + opt);
                        return -1;
                    }
                    userId = UserHandle.parseUserArg(getNextArgRequired());
                }
            } catch (RuntimeException ex) {
                pw.println("Error: " + ex.toString());
                return -1;
            }
        }
    }

    private int runListPermissionGroups() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        List<PermissionGroupInfo> pgs = this.mInterface.getAllPermissionGroups(0).getList();
        int count = pgs.size();
        for (int p = 0; p < count; p++) {
            PermissionGroupInfo pgi = pgs.get(p);
            pw.print("permission group:");
            pw.println(pgi.name);
        }
        return 0;
    }

    private int runListPermissions() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        boolean labels = false;
        boolean groups = false;
        boolean userOnly = false;
        boolean summary = false;
        boolean dangerousOnly = false;
        while (true) {
            String opt = getNextOption();
            if (opt == null) {
                ArrayList<String> groupList = new ArrayList<>();
                if (groups) {
                    List<PermissionGroupInfo> infos = this.mInterface.getAllPermissionGroups(0).getList();
                    int count = infos.size();
                    for (int i = 0; i < count; i++) {
                        groupList.add(infos.get(i).name);
                    }
                    groupList.add(null);
                } else {
                    String grp = getNextArg();
                    groupList.add(grp);
                }
                if (dangerousOnly) {
                    pw.println("Dangerous Permissions:");
                    pw.println("");
                    doListPermissions(groupList, groups, labels, summary, 1, 1);
                    if (!userOnly) {
                        return 0;
                    }
                    pw.println("Normal Permissions:");
                    pw.println("");
                    doListPermissions(groupList, groups, labels, summary, 0, 0);
                    return 0;
                }
                if (userOnly) {
                    pw.println("Dangerous and Normal Permissions:");
                    pw.println("");
                    doListPermissions(groupList, groups, labels, summary, 0, 1);
                    return 0;
                }
                pw.println("All Permissions:");
                pw.println("");
                doListPermissions(groupList, groups, labels, summary, -10000, 10000);
                return 0;
            }
            if (opt.equals("-d")) {
                dangerousOnly = true;
            } else if (opt.equals("-f")) {
                labels = true;
            } else if (opt.equals("-g")) {
                groups = true;
            } else if (opt.equals("-s")) {
                groups = true;
                labels = true;
                summary = true;
            } else {
                if (!opt.equals("-u")) {
                    pw.println("Error: Unknown option: " + opt);
                    return 1;
                }
                userOnly = true;
            }
        }
    }

    private int runUninstall() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        int flags = 0;
        int userId = -1;
        while (true) {
            String opt = getNextOption();
            if (opt != null) {
                if (!opt.equals("-k")) {
                    if (opt.equals("--user")) {
                        userId = UserHandle.parseUserArg(getNextArgRequired());
                    } else {
                        pw.println("Error: Unknown option: " + opt);
                        return 1;
                    }
                } else {
                    flags |= 1;
                }
            } else {
                String packageName = getNextArg();
                if (packageName == null) {
                    pw.println("Error: package name not specified");
                    return 1;
                }
                String splitName = getNextArg();
                if (splitName != null) {
                    return runRemoveSplit(packageName, splitName);
                }
                boolean deleteAll = false;
                int userId2 = translateUserId(userId, "runUninstall");
                if (userId2 == -1) {
                    userId2 = 0;
                    flags |= 2;
                    deleteAll = true;
                } else {
                    PackageInfo info = this.mInterface.getPackageInfo(packageName, 0, userId2);
                    if (info == null) {
                        pw.println("Failure [not installed for " + userId2 + "]");
                        return 1;
                    }
                    boolean isSystem = (info.applicationInfo.flags & 1) != 0;
                    if (isSystem) {
                        flags |= 4;
                    }
                }
                PackageInfo info2 = this.mInterface.getPackageInfo(packageName, 0, userId2);
                if (info2 == null && !deleteAll) {
                    pw.println("Failure - not installed for " + userId2);
                    return 1;
                }
                if (info2 != null && (info2.applicationInfo.flagsEx & 1) != 0) {
                    flags &= -3;
                }
                LocalIntentReceiver receiver = new LocalIntentReceiver(null);
                this.mInterface.getPackageInstaller().uninstall(packageName, (String) null, flags, receiver.getIntentSender(), userId2);
                Intent result = receiver.getResult();
                int status = result.getIntExtra("android.content.pm.extra.STATUS", 1);
                if (status == 0) {
                    pw.println("Success");
                    return 0;
                }
                pw.println("Failure [" + result.getStringExtra("android.content.pm.extra.STATUS_MESSAGE") + "]");
                return 1;
            }
        }
    }

    private int runRemoveSplit(String packageName, String splitName) throws Throwable {
        PrintWriter pw = getOutPrintWriter();
        PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(2);
        sessionParams.installFlags |= 2;
        sessionParams.appPackageName = packageName;
        int sessionId = doCreateSession(sessionParams, null, -1);
        boolean abandonSession = true;
        try {
            if (doRemoveSplit(sessionId, splitName, false) != 0) {
                if (1 != 0) {
                    try {
                        doAbandonSession(sessionId, false);
                    } catch (Exception e) {
                    }
                }
                return 1;
            }
            if (doCommitSession(sessionId, false) != 0) {
                if (1 != 0) {
                    try {
                        doAbandonSession(sessionId, false);
                    } catch (Exception e2) {
                    }
                }
                return 1;
            }
            abandonSession = false;
            pw.println("Success");
            if (0 != 0) {
                try {
                    doAbandonSession(sessionId, false);
                } catch (Exception e3) {
                }
            }
            return 0;
        } catch (Throwable th) {
            if (abandonSession) {
                try {
                    doAbandonSession(sessionId, false);
                } catch (Exception e4) {
                }
            }
            throw th;
        }
    }

    private Intent parseIntentAndUser() throws URISyntaxException {
        this.mTargetUser = -2;
        this.mBrief = false;
        this.mComponents = false;
        Intent intent = Intent.parseCommandArgs(this, new Intent.CommandOptionHandler() {
            public boolean handleOption(String opt, ShellCommand cmd) {
                if ("--user".equals(opt)) {
                    PackageManagerShellCommand.this.mTargetUser = UserHandle.parseUserArg(cmd.getNextArgRequired());
                    return true;
                }
                if ("--brief".equals(opt)) {
                    PackageManagerShellCommand.this.mBrief = true;
                    return true;
                }
                if ("--components".equals(opt)) {
                    PackageManagerShellCommand.this.mComponents = true;
                    return true;
                }
                return false;
            }
        });
        this.mTargetUser = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), this.mTargetUser, false, false, null, null);
        return intent;
    }

    private void printResolveInfo(PrintWriterPrinter pr, String prefix, ResolveInfo ri, boolean brief, boolean components) {
        if (brief || components) {
            ComponentName comp = ri.activityInfo != null ? new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name) : ri.serviceInfo != null ? new ComponentName(ri.serviceInfo.packageName, ri.serviceInfo.name) : ri.providerInfo != null ? new ComponentName(ri.providerInfo.packageName, ri.providerInfo.name) : null;
            if (comp != null) {
                if (!components) {
                    pr.println(prefix + "priority=" + ri.priority + " preferredOrder=" + ri.preferredOrder + " match=0x" + Integer.toHexString(ri.match) + " specificIndex=" + ri.specificIndex + " isDefault=" + ri.isDefault);
                }
                pr.println(prefix + comp.flattenToShortString());
                return;
            }
        }
        ri.dump(pr, prefix);
    }

    private int runResolveActivity() {
        try {
            Intent intent = parseIntentAndUser();
            try {
                ResolveInfo ri = this.mInterface.resolveIntent(intent, (String) null, 0, this.mTargetUser);
                PrintWriter pw = getOutPrintWriter();
                if (ri == null) {
                    pw.println("No activity found");
                } else {
                    PrintWriterPrinter pr = new PrintWriterPrinter(pw);
                    printResolveInfo(pr, "", ri, this.mBrief, this.mComponents);
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
            Intent intent = parseIntentAndUser();
            try {
                List<ResolveInfo> result = this.mInterface.queryIntentActivities(intent, (String) null, 0, this.mTargetUser).getList();
                PrintWriter pw = getOutPrintWriter();
                if (result == null || result.size() <= 0) {
                    pw.println("No activities found");
                } else if (!this.mComponents) {
                    pw.print(result.size());
                    pw.println(" activities found:");
                    PrintWriterPrinter pr = new PrintWriterPrinter(pw);
                    for (int i = 0; i < result.size(); i++) {
                        pw.print("  Activity #");
                        pw.print(i);
                        pw.println(":");
                        printResolveInfo(pr, "    ", result.get(i), this.mBrief, this.mComponents);
                    }
                } else {
                    PrintWriterPrinter pr2 = new PrintWriterPrinter(pw);
                    for (int i2 = 0; i2 < result.size(); i2++) {
                        printResolveInfo(pr2, "", result.get(i2), this.mBrief, this.mComponents);
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
            Intent intent = parseIntentAndUser();
            try {
                List<ResolveInfo> result = this.mInterface.queryIntentServices(intent, (String) null, 0, this.mTargetUser).getList();
                PrintWriter pw = getOutPrintWriter();
                if (result == null || result.size() <= 0) {
                    pw.println("No services found");
                } else if (!this.mComponents) {
                    pw.print(result.size());
                    pw.println(" services found:");
                    PrintWriterPrinter pr = new PrintWriterPrinter(pw);
                    for (int i = 0; i < result.size(); i++) {
                        pw.print("  Service #");
                        pw.print(i);
                        pw.println(":");
                        printResolveInfo(pr, "    ", result.get(i), this.mBrief, this.mComponents);
                    }
                } else {
                    PrintWriterPrinter pr2 = new PrintWriterPrinter(pw);
                    for (int i2 = 0; i2 < result.size(); i2++) {
                        printResolveInfo(pr2, "", result.get(i2), this.mBrief, this.mComponents);
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
            Intent intent = parseIntentAndUser();
            try {
                List<ResolveInfo> result = this.mInterface.queryIntentReceivers(intent, (String) null, 0, this.mTargetUser).getList();
                PrintWriter pw = getOutPrintWriter();
                if (result == null || result.size() <= 0) {
                    pw.println("No receivers found");
                } else if (!this.mComponents) {
                    pw.print(result.size());
                    pw.println(" receivers found:");
                    PrintWriterPrinter pr = new PrintWriterPrinter(pw);
                    for (int i = 0; i < result.size(); i++) {
                        pw.print("  Receiver #");
                        pw.print(i);
                        pw.println(":");
                        printResolveInfo(pr, "    ", result.get(i), this.mBrief, this.mComponents);
                    }
                } else {
                    PrintWriterPrinter pr2 = new PrintWriterPrinter(pw);
                    for (int i2 = 0; i2 < result.size(); i2++) {
                        printResolveInfo(pr2, "", result.get(i2), this.mBrief, this.mComponents);
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

    private static class InstallParams {
        String installerPackageName;
        PackageInstaller.SessionParams sessionParams;
        int userId;

        InstallParams(InstallParams installParams) {
            this();
        }

        private InstallParams() {
            this.userId = -1;
        }
    }

    private InstallParams makeInstallParams() {
        PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(1);
        InstallParams params = new InstallParams(null);
        params.sessionParams = sessionParams;
        while (true) {
            String opt = getNextOption();
            if (opt == null) {
                return params;
            }
            if (opt.equals("-l")) {
                sessionParams.installFlags |= 1;
            } else if (opt.equals("-r")) {
                sessionParams.installFlags |= 2;
            } else if (opt.equals("-i")) {
                params.installerPackageName = getNextArg();
                if (params.installerPackageName == null) {
                    throw new IllegalArgumentException("Missing installer package");
                }
            } else if (opt.equals("-t")) {
                sessionParams.installFlags |= 4;
            } else if (opt.equals("-s")) {
                sessionParams.installFlags |= 8;
            } else if (opt.equals("-f")) {
                sessionParams.installFlags |= 16;
            } else if (opt.equals("-d")) {
                sessionParams.installFlags |= 128;
            } else if (opt.equals("-g")) {
                sessionParams.installFlags |= 256;
            } else if (opt.equals("--dont-kill")) {
                sessionParams.installFlags |= 4096;
            } else if (opt.equals("--originating-uri")) {
                sessionParams.originatingUri = Uri.parse(getNextArg());
            } else if (opt.equals("--referrer")) {
                sessionParams.referrerUri = Uri.parse(getNextArg());
            } else if (opt.equals("-p")) {
                sessionParams.mode = 2;
                sessionParams.appPackageName = getNextArg();
                if (sessionParams.appPackageName == null) {
                    throw new IllegalArgumentException("Missing inherit package name");
                }
            } else if (opt.equals("-S")) {
                sessionParams.setSize(Long.parseLong(getNextArg()));
            } else if (opt.equals("--abi")) {
                sessionParams.abiOverride = checkAbiArgument(getNextArg());
            } else if (opt.equals("--ephemeral")) {
                sessionParams.installFlags |= PackageManagerService.DumpState.DUMP_VERIFIERS;
            } else if (opt.equals("--user")) {
                params.userId = UserHandle.parseUserArg(getNextArgRequired());
            } else if (opt.equals("--install-location")) {
                sessionParams.installLocation = Integer.parseInt(getNextArg());
            } else if (opt.equals("--force-uuid")) {
                sessionParams.installFlags |= 512;
                sessionParams.volumeUuid = getNextArg();
                if ("internal".equals(sessionParams.volumeUuid)) {
                    sessionParams.volumeUuid = null;
                }
            } else {
                if (!opt.equals("--force-sdk")) {
                    throw new IllegalArgumentException("Unknown option " + opt);
                }
                sessionParams.installFlags |= PackageManagerService.DumpState.DUMP_PREFERRED_XML;
            }
        }
    }

    private int runSetHomeActivity() {
        PrintWriter pw = getOutPrintWriter();
        int userId = 0;
        while (true) {
            String opt = getNextOption();
            if (opt != null) {
                if (opt.equals("--user")) {
                    userId = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    pw.println("Error: Unknown option: " + opt);
                    return 1;
                }
            } else {
                String component = getNextArg();
                ComponentName componentName = component != null ? ComponentName.unflattenFromString(component) : null;
                if (componentName == null) {
                    pw.println("Error: component name not specified or invalid");
                    return 1;
                }
                try {
                    this.mInterface.setHomeActivity(componentName, userId);
                    return 0;
                } catch (RemoteException e) {
                    pw.println(e.toString());
                    return 1;
                }
            }
        }
    }

    private static String checkAbiArgument(String abi) {
        if (TextUtils.isEmpty(abi)) {
            throw new IllegalArgumentException("Missing ABI argument");
        }
        if ("-".equals(abi)) {
            return abi;
        }
        String[] supportedAbis = Build.SUPPORTED_ABIS;
        for (String supportedAbi : supportedAbis) {
            if (supportedAbi.equals(abi)) {
                return abi;
            }
        }
        throw new IllegalArgumentException("ABI " + abi + " not supported on this device");
    }

    private int translateUserId(int userId, String logContext) {
        return ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, true, logContext, "pm command");
    }

    private int doCreateSession(PackageInstaller.SessionParams params, String installerPackageName, int userId) throws RemoteException {
        int userId2 = translateUserId(userId, "runInstallCreate");
        if (userId2 == -1) {
            userId2 = 0;
            params.installFlags |= 64;
        }
        int sessionId = this.mInterface.getPackageInstaller().createSession(params, installerPackageName, userId2);
        return sessionId;
    }

    private int doWriteSplit(int sessionId, String inPath, long sizeBytes, String splitName, boolean logSuccess) throws Throwable {
        PackageInstaller.Session session;
        SizedInputStream sizedInputStream;
        PrintWriter pw = getOutPrintWriter();
        if (sizeBytes <= 0) {
            pw.println("Error: must specify a APK size");
            return 1;
        }
        if (inPath != null && !"-".equals(inPath)) {
            pw.println("Error: APK content must be streamed");
            return 1;
        }
        PackageInstaller.SessionInfo info = this.mInterface.getPackageInstaller().getSessionInfo(sessionId);
        SizedInputStream sizedInputStream2 = null;
        OutputStream out = null;
        try {
            try {
                session = new PackageInstaller.Session(this.mInterface.getPackageInstaller().openSession(sessionId));
                try {
                    sizedInputStream = new SizedInputStream(getRawInputStream(), sizeBytes);
                } catch (IOException e) {
                    e = e;
                }
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e2) {
            e = e2;
            session = null;
        } catch (Throwable th2) {
            th = th2;
            session = null;
        }
        try {
            out = session.openWrite(splitName, 0L, sizeBytes);
            int total = 0;
            byte[] buffer = new byte[PackageManagerService.DumpState.DUMP_INSTALLS];
            while (true) {
                int c = sizedInputStream.read(buffer);
                if (c == -1) {
                    break;
                }
                total += c;
                out.write(buffer, 0, c);
                if (info.sizeBytes > 0) {
                    float fraction = c / info.sizeBytes;
                    session.addProgress(fraction);
                }
            }
            session.fsync(out);
            if (logSuccess) {
                pw.println("Success: streamed " + total + " bytes");
            }
            IoUtils.closeQuietly(out);
            IoUtils.closeQuietly(sizedInputStream);
            IoUtils.closeQuietly(session);
            return 0;
        } catch (IOException e3) {
            e = e3;
            sizedInputStream2 = sizedInputStream;
            pw.println("Error: failed to write; " + e.getMessage());
            IoUtils.closeQuietly(out);
            IoUtils.closeQuietly(sizedInputStream2);
            IoUtils.closeQuietly(session);
            return 1;
        } catch (Throwable th3) {
            th = th3;
            sizedInputStream2 = sizedInputStream;
            IoUtils.closeQuietly(out);
            IoUtils.closeQuietly(sizedInputStream2);
            IoUtils.closeQuietly(session);
            throw th;
        }
    }

    private int doRemoveSplit(int sessionId, String splitName, boolean logSuccess) throws Throwable {
        PackageInstaller.Session session;
        PrintWriter pw = getOutPrintWriter();
        PackageInstaller.Session session2 = null;
        try {
            try {
                session = new PackageInstaller.Session(this.mInterface.getPackageInstaller().openSession(sessionId));
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e) {
            e = e;
        }
        try {
            session.removeSplit(splitName);
            if (logSuccess) {
                pw.println("Success");
            }
            IoUtils.closeQuietly(session);
            return 0;
        } catch (IOException e2) {
            e = e2;
            session2 = session;
            pw.println("Error: failed to remove split; " + e.getMessage());
            IoUtils.closeQuietly(session2);
            return 1;
        } catch (Throwable th2) {
            th = th2;
            session2 = session;
            IoUtils.closeQuietly(session2);
            throw th;
        }
    }

    private int doCommitSession(int sessionId, boolean logSuccess) throws Throwable {
        PrintWriter pw = getOutPrintWriter();
        PackageInstaller.Session session = null;
        try {
            PackageInstaller.Session session2 = new PackageInstaller.Session(this.mInterface.getPackageInstaller().openSession(sessionId));
            try {
                LocalIntentReceiver receiver = new LocalIntentReceiver(null);
                session2.commit(receiver.getIntentSender());
                Intent result = receiver.getResult();
                int status = result.getIntExtra("android.content.pm.extra.STATUS", 1);
                if (status == 0) {
                    if (logSuccess) {
                        System.out.println("Success");
                    }
                } else {
                    pw.println("Failure [" + result.getStringExtra("android.content.pm.extra.STATUS_MESSAGE") + "]");
                }
                IoUtils.closeQuietly(session2);
                return status;
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

    private int doAbandonSession(int sessionId, boolean logSuccess) throws Throwable {
        PackageInstaller.Session session;
        PrintWriter pw = getOutPrintWriter();
        PackageInstaller.Session session2 = null;
        try {
            session = new PackageInstaller.Session(this.mInterface.getPackageInstaller().openSession(sessionId));
        } catch (Throwable th) {
            th = th;
        }
        try {
            session.abandon();
            if (logSuccess) {
                pw.println("Success");
            }
            IoUtils.closeQuietly(session);
            return 0;
        } catch (Throwable th2) {
            th = th2;
            session2 = session;
            IoUtils.closeQuietly(session2);
            throw th;
        }
    }

    private void doListPermissions(ArrayList<String> groupList, boolean groups, boolean labels, boolean summary, int startProtectionLevel, int endProtectionLevel) throws RemoteException {
        int base;
        PrintWriter pw = getOutPrintWriter();
        int groupCount = groupList.size();
        for (int i = 0; i < groupCount; i++) {
            String groupName = groupList.get(i);
            String prefix = "";
            if (groups) {
                if (i > 0) {
                    pw.println("");
                }
                if (groupName != null) {
                    PermissionGroupInfo pgi = this.mInterface.getPermissionGroupInfo(groupName, 0);
                    if (summary) {
                        Resources res = getResources(pgi);
                        if (res != null) {
                            pw.print(loadText(pgi, pgi.labelRes, pgi.nonLocalizedLabel) + ": ");
                        } else {
                            pw.print(pgi.name + ": ");
                        }
                    } else {
                        pw.println((labels ? "+ " : "") + "group:" + pgi.name);
                        if (labels) {
                            pw.println("  package:" + pgi.packageName);
                            Resources res2 = getResources(pgi);
                            if (res2 != null) {
                                pw.println("  label:" + loadText(pgi, pgi.labelRes, pgi.nonLocalizedLabel));
                                pw.println("  description:" + loadText(pgi, pgi.descriptionRes, pgi.nonLocalizedDescription));
                            }
                        }
                    }
                } else {
                    pw.println(((!labels || summary) ? "" : "+ ") + "ungrouped:");
                }
                prefix = "  ";
            }
            List<PermissionInfo> ps = this.mInterface.queryPermissionsByGroup(groupList.get(i), 0).getList();
            int count = ps.size();
            boolean first = true;
            for (int p = 0; p < count; p++) {
                PermissionInfo pi = ps.get(p);
                if ((!groups || groupName != null || pi.group == null) && (base = pi.protectionLevel & 15) >= startProtectionLevel && base <= endProtectionLevel) {
                    if (summary) {
                        if (first) {
                            first = false;
                        } else {
                            pw.print(", ");
                        }
                        Resources res3 = getResources(pi);
                        if (res3 != null) {
                            pw.print(loadText(pi, pi.labelRes, pi.nonLocalizedLabel));
                        } else {
                            pw.print(pi.name);
                        }
                    } else {
                        pw.println(prefix + (labels ? "+ " : "") + "permission:" + pi.name);
                        if (labels) {
                            pw.println(prefix + "  package:" + pi.packageName);
                            Resources res4 = getResources(pi);
                            if (res4 != null) {
                                pw.println(prefix + "  label:" + loadText(pi, pi.labelRes, pi.nonLocalizedLabel));
                                pw.println(prefix + "  description:" + loadText(pi, pi.descriptionRes, pi.nonLocalizedDescription));
                            }
                            pw.println(prefix + "  protectionLevel:" + PermissionInfo.protectionToString(pi.protectionLevel));
                        }
                    }
                }
            }
            if (summary) {
                pw.println("");
            }
        }
    }

    private String loadText(PackageItemInfo pii, int res, CharSequence nonLocalized) throws RemoteException {
        Resources r;
        if (nonLocalized != null) {
            return nonLocalized.toString();
        }
        if (res != 0 && (r = getResources(pii)) != null) {
            try {
                return r.getString(res);
            } catch (Resources.NotFoundException e) {
            }
        }
        return null;
    }

    private Resources getResources(PackageItemInfo pii) throws RemoteException {
        Resources res = this.mResourceCache.get(pii.packageName);
        if (res != null) {
            return res;
        }
        ApplicationInfo ai = this.mInterface.getApplicationInfo(pii.packageName, 0, 0);
        AssetManager am = new AssetManager();
        am.addAssetPath(ai.publicSourceDir);
        Resources res2 = new Resources(am, null, null);
        this.mResourceCache.put(pii.packageName, res2);
        return res2;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Package manager (package) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("");
        pw.println("  compile [-m MODE | -r REASON] [-f] [-c]");
        pw.println("          [--reset] [--check-prof (true | false)] (-a | TARGET-PACKAGE)");
        pw.println("    Trigger compilation of TARGET-PACKAGE or all packages if \"-a\".");
        pw.println("    Options:");
        pw.println("      -a: compile all packages");
        pw.println("      -c: clear profile data before compiling");
        pw.println("      -f: force compilation even if not needed");
        pw.println("      -m: select compilation mode");
        pw.println("          MODE is one of the dex2oat compiler filters:");
        pw.println("            verify-none");
        pw.println("            verify-at-runtime");
        pw.println("            verify-profile");
        pw.println("            interpret-only");
        pw.println("            space-profile");
        pw.println("            space");
        pw.println("            speed-profile");
        pw.println("            speed");
        pw.println("            everything");
        pw.println("      -r: select compilation reason");
        pw.println("          REASON is one of:");
        for (int i = 0; i < PackageManagerServiceCompilerMapping.REASON_STRINGS.length; i++) {
            pw.println("            " + PackageManagerServiceCompilerMapping.REASON_STRINGS[i]);
        }
        pw.println("      --reset: restore package to its post-install state");
        pw.println("      --check-prof (true | false): look at profiles when doing dexopt?");
        pw.println("  list features");
        pw.println("    Prints all features of the system.");
        pw.println("  list instrumentation [-f] [TARGET-PACKAGE]");
        pw.println("    Prints all test packages; optionally only those targeting TARGET-PACKAGE");
        pw.println("    Options:");
        pw.println("      -f: dump the name of the .apk file containing the test package");
        pw.println("  list libraries");
        pw.println("    Prints all system libraries.");
        pw.println("  list packages [-f] [-d] [-e] [-s] [-3] [-i] [-u] [--user USER_ID] [FILTER]");
        pw.println("    Prints all packages; optionally only those whose name contains");
        pw.println("    the text in FILTER.");
        pw.println("    Options:");
        pw.println("      -f: see their associated file");
        pw.println("      -d: filter to only show disabled packages");
        pw.println("      -e: filter to only show enabled packages");
        pw.println("      -s: filter to only show system packages");
        pw.println("      -3: filter to only show third party packages");
        pw.println("      -i: see the installer for the packages");
        pw.println("      -u: also include uninstalled packages");
        pw.println("  list permission-groups");
        pw.println("    Prints all known permission groups.");
        pw.println("  list permissions [-g] [-f] [-d] [-u] [GROUP]");
        pw.println("    Prints all known permissions; optionally only those in GROUP.");
        pw.println("    Options:");
        pw.println("      -g: organize by group");
        pw.println("      -f: print all information");
        pw.println("      -s: short summary");
        pw.println("      -d: only list dangerous permissions");
        pw.println("      -u: list only the permissions users will see");
        pw.println("  dump-profiles TARGET-PACKAGE");
        pw.println("    Dumps method/class profile files to");
        pw.println("    /data/misc/profman/TARGET-PACKAGE.txt");
        pw.println("  resolve-activity [--brief] [--components] [--user USER_ID] INTENT");
        pw.println("    Prints the activity that resolves to the given Intent.");
        pw.println("  query-activities [--brief] [--components] [--user USER_ID] INTENT");
        pw.println("    Prints all activities that can handle the given Intent.");
        pw.println("  query-services [--brief] [--components] [--user USER_ID] INTENT");
        pw.println("    Prints all services that can handle the given Intent.");
        pw.println("  query-receivers [--brief] [--components] [--user USER_ID] INTENT");
        pw.println("    Prints all broadcast receivers that can handle the given Intent.");
        pw.println("  suspend [--user USER_ID] TARGET-PACKAGE");
        pw.println("    Suspends the specified package (as user).");
        pw.println("  unsuspend [--user USER_ID] TARGET-PACKAGE");
        pw.println("    Unsuspends the specified package (as user).");
        pw.println("  set-home-activity [--user USER_ID] TARGET-COMPONENT");
        pw.println("    set the default home activity (aka launcher).");
        pw.println();
        Intent.printIntentArgsHelp(pw, "");
    }

    private static class LocalIntentReceiver {
        private IIntentSender.Stub mLocalSender;
        private final SynchronousQueue<Intent> mResult;

        LocalIntentReceiver(LocalIntentReceiver localIntentReceiver) {
            this();
        }

        private LocalIntentReceiver() {
            this.mResult = new SynchronousQueue<>();
            this.mLocalSender = new IIntentSender.Stub() {
                public void send(int code, Intent intent, String resolvedType, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
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
    }
}
