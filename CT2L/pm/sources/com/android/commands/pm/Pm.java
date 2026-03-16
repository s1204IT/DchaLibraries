package com.android.commands.pm;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.PackageInstallObserver;
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
import android.content.pm.ManifestDigest;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.UserInfo;
import android.content.pm.VerificationParams;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IUserManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.SizedInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;

public final class Pm {
    private static final String PM_NOT_RUNNING_ERR = "Error: Could not access the Package Manager.  Is the system running?";
    private static final String TAG = "Pm";
    private String[] mArgs;
    private String mCurArgData;
    IPackageInstaller mInstaller;
    private int mNextArg;
    IPackageManager mPm;
    private WeakHashMap<String, Resources> mResourceCache = new WeakHashMap<>();
    IUserManager mUm;

    public static void main(String[] args) {
        int exitCode = 1;
        try {
            exitCode = new Pm().run(args);
        } catch (Exception e) {
            Log.e(TAG, "Error", e);
            System.err.println("Error: " + e);
            if (e instanceof RemoteException) {
                System.err.println(PM_NOT_RUNNING_ERR);
            }
        }
        System.exit(exitCode);
    }

    public int run(String[] args) throws IOException, RemoteException {
        int iDisplayPackageFilePath = 1;
        boolean validCommand = false;
        if (args.length < 1) {
            return showUsage();
        }
        this.mUm = IUserManager.Stub.asInterface(ServiceManager.getService("user"));
        this.mPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        if (this.mPm == null) {
            System.err.println(PM_NOT_RUNNING_ERR);
            return 1;
        }
        this.mInstaller = this.mPm.getPackageInstaller();
        this.mArgs = args;
        String op = args[0];
        this.mNextArg = 1;
        if ("list".equals(op)) {
            return runList();
        }
        if ("path".equals(op)) {
            return runPath();
        }
        if ("dump".equals(op)) {
            return runDump();
        }
        if ("install".equals(op)) {
            return runInstall();
        }
        if ("install-create".equals(op)) {
            return runInstallCreate();
        }
        if ("install-write".equals(op)) {
            return runInstallWrite();
        }
        if ("install-commit".equals(op)) {
            return runInstallCommit();
        }
        if ("install-abandon".equals(op) || "install-destroy".equals(op)) {
            return runInstallAbandon();
        }
        if ("set-installer".equals(op)) {
            return runSetInstaller();
        }
        if ("uninstall".equals(op)) {
            return runUninstall();
        }
        if ("clear".equals(op)) {
            return runClear();
        }
        if ("enable".equals(op)) {
            return runSetEnabledSetting(1);
        }
        if ("disable".equals(op)) {
            return runSetEnabledSetting(2);
        }
        if ("disable-user".equals(op)) {
            return runSetEnabledSetting(3);
        }
        if ("disable-until-used".equals(op)) {
            return runSetEnabledSetting(4);
        }
        if ("hide".equals(op)) {
            return runSetHiddenSetting(true);
        }
        if ("unhide".equals(op)) {
            return runSetHiddenSetting(false);
        }
        if ("grant".equals(op)) {
            return runGrantRevokePermission(true);
        }
        if ("revoke".equals(op)) {
            return runGrantRevokePermission(false);
        }
        if ("set-permission-enforced".equals(op)) {
            return runSetPermissionEnforced();
        }
        if ("set-install-location".equals(op)) {
            return runSetInstallLocation();
        }
        if ("get-install-location".equals(op)) {
            return runGetInstallLocation();
        }
        if ("trim-caches".equals(op)) {
            return runTrimCaches();
        }
        if ("create-user".equals(op)) {
            return runCreateUser();
        }
        if ("remove-user".equals(op)) {
            return runRemoveUser();
        }
        if ("get-max-users".equals(op)) {
            return runGetMaxUsers();
        }
        if ("force-dex-opt".equals(op)) {
            return runForceDexOpt();
        }
        if ("set-user-restriction".equals(op)) {
            return runSetUserRestriction();
        }
        try {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("-l")) {
                    validCommand = true;
                    iDisplayPackageFilePath = runListPackages(false);
                } else if (args[0].equalsIgnoreCase("-lf")) {
                    validCommand = true;
                    iDisplayPackageFilePath = runListPackages(true);
                    if (1 == 0) {
                        if (op != null) {
                            System.err.println("Error: unknown command '" + op + "'");
                        }
                        showUsage();
                    }
                } else if (0 == 0) {
                    if (op != null) {
                        System.err.println("Error: unknown command '" + op + "'");
                    }
                    showUsage();
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("-p")) {
                validCommand = true;
                iDisplayPackageFilePath = displayPackageFilePath(args[1]);
                if (1 == 0) {
                    if (op != null) {
                        System.err.println("Error: unknown command '" + op + "'");
                    }
                    showUsage();
                }
            }
            return iDisplayPackageFilePath;
        } finally {
            if (!validCommand) {
                if (op != null) {
                    System.err.println("Error: unknown command '" + op + "'");
                }
                showUsage();
            }
        }
    }

    private int runList() {
        String type = nextArg();
        if (type == null) {
            System.err.println("Error: didn't specify type of data to list");
            return 1;
        }
        if ("package".equals(type) || "packages".equals(type)) {
            return runListPackages(false);
        }
        if ("permission-groups".equals(type)) {
            return runListPermissionGroups();
        }
        if ("permissions".equals(type)) {
            return runListPermissions();
        }
        if ("features".equals(type)) {
            return runListFeatures();
        }
        if ("libraries".equals(type)) {
            return runListLibraries();
        }
        if ("instrumentation".equals(type)) {
            return runListInstrumentation();
        }
        if ("users".equals(type)) {
            return runListUsers();
        }
        System.err.println("Error: unknown list type '" + type + "'");
        return 1;
    }

    private int runListPackages(boolean showApplicationPackage) {
        int getFlags = 0;
        boolean listDisabled = false;
        boolean listEnabled = false;
        boolean listSystem = false;
        boolean listThirdParty = false;
        boolean listInstaller = false;
        int userId = 0;
        while (true) {
            try {
                String opt = nextOption();
                if (opt != null) {
                    if (!opt.equals("-l")) {
                        if (opt.equals("-lf")) {
                            showApplicationPackage = true;
                        } else if (opt.equals("-f")) {
                            showApplicationPackage = true;
                        } else if (opt.equals("-d")) {
                            listDisabled = true;
                        } else if (opt.equals("-e")) {
                            listEnabled = true;
                        } else if (opt.equals("-s")) {
                            listSystem = true;
                        } else if (opt.equals("-3")) {
                            listThirdParty = true;
                        } else if (opt.equals("-i")) {
                            listInstaller = true;
                        } else if (opt.equals("--user")) {
                            userId = Integer.parseInt(nextArg());
                        } else if (opt.equals("-u")) {
                            getFlags |= 8192;
                        } else {
                            System.err.println("Error: Unknown option: " + opt);
                            return 1;
                        }
                    }
                } else {
                    String filter = nextArg();
                    try {
                        List<PackageInfo> packages = getInstalledPackages(this.mPm, getFlags, userId);
                        int count = packages.size();
                        for (int p = 0; p < count; p++) {
                            PackageInfo info = packages.get(p);
                            if (filter == null || info.packageName.contains(filter)) {
                                boolean isSystem = (info.applicationInfo.flags & 1) != 0;
                                if ((!listDisabled || !info.applicationInfo.enabled) && ((!listEnabled || info.applicationInfo.enabled) && ((!listSystem || isSystem) && (!listThirdParty || !isSystem)))) {
                                    System.out.print("package:");
                                    if (showApplicationPackage) {
                                        System.out.print(info.applicationInfo.sourceDir);
                                        System.out.print("=");
                                    }
                                    System.out.print(info.packageName);
                                    if (listInstaller) {
                                        System.out.print("  installer=");
                                        System.out.print(this.mPm.getInstallerPackageName(info.packageName));
                                    }
                                    System.out.println();
                                }
                            }
                        }
                        return 0;
                    } catch (RemoteException e) {
                        System.err.println(e.toString());
                        System.err.println(PM_NOT_RUNNING_ERR);
                        return 1;
                    }
                }
            } catch (RuntimeException ex) {
                System.err.println("Error: " + ex.toString());
                return 1;
            }
        }
    }

    private List<PackageInfo> getInstalledPackages(IPackageManager pm, int flags, int userId) throws RemoteException {
        ParceledListSlice<PackageInfo> slice = pm.getInstalledPackages(flags, userId);
        return slice.getList();
    }

    private int runListFeatures() {
        try {
            List<FeatureInfo> list = new ArrayList<>();
            FeatureInfo[] rawList = this.mPm.getSystemAvailableFeatures();
            for (FeatureInfo featureInfo : rawList) {
                list.add(featureInfo);
            }
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
                System.out.print("feature:");
                if (fi.name != null) {
                    System.out.println(fi.name);
                } else {
                    System.out.println("reqGlEsVersion=0x" + Integer.toHexString(fi.reqGlEsVersion));
                }
            }
            return 0;
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(PM_NOT_RUNNING_ERR);
            return 1;
        }
    }

    private int runListLibraries() {
        try {
            List<String> list = new ArrayList<>();
            String[] rawList = this.mPm.getSystemSharedLibraryNames();
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
                System.out.print("library:");
                System.out.println(lib);
            }
            return 0;
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(PM_NOT_RUNNING_ERR);
            return 1;
        }
    }

    private int runListInstrumentation() {
        boolean showPackage = false;
        String targetPackage = null;
        while (true) {
            try {
                String opt = nextArg();
                if (opt == null) {
                    try {
                        List<InstrumentationInfo> list = this.mPm.queryInstrumentation(targetPackage, 0);
                        Collections.sort(list, new Comparator<InstrumentationInfo>() {
                            @Override
                            public int compare(InstrumentationInfo o1, InstrumentationInfo o2) {
                                return o1.targetPackage.compareTo(o2.targetPackage);
                            }
                        });
                        int count = list != null ? list.size() : 0;
                        for (int p = 0; p < count; p++) {
                            InstrumentationInfo ii = list.get(p);
                            System.out.print("instrumentation:");
                            if (showPackage) {
                                System.out.print(ii.sourceDir);
                                System.out.print("=");
                            }
                            ComponentName cn = new ComponentName(ii.packageName, ii.name);
                            System.out.print(cn.flattenToShortString());
                            System.out.print(" (target=");
                            System.out.print(ii.targetPackage);
                            System.out.println(")");
                        }
                        return 0;
                    } catch (RemoteException e) {
                        System.err.println(e.toString());
                        System.err.println(PM_NOT_RUNNING_ERR);
                        return 1;
                    }
                }
                if (opt.equals("-f")) {
                    showPackage = true;
                } else if (opt.charAt(0) != '-') {
                    targetPackage = opt;
                } else {
                    System.err.println("Error: Unknown option: " + opt);
                    return 1;
                }
            } catch (RuntimeException ex) {
                System.err.println("Error: " + ex.toString());
                return 1;
            }
        }
    }

    private int runListPermissionGroups() {
        try {
            List<PermissionGroupInfo> pgs = this.mPm.getAllPermissionGroups(0);
            int count = pgs.size();
            for (int p = 0; p < count; p++) {
                PermissionGroupInfo pgi = pgs.get(p);
                System.out.print("permission group:");
                System.out.println(pgi.name);
            }
            return 0;
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(PM_NOT_RUNNING_ERR);
            return 1;
        }
    }

    private String loadText(PackageItemInfo pii, int res, CharSequence nonLocalized) {
        Resources r;
        if (nonLocalized != null) {
            return nonLocalized.toString();
        }
        if (res != 0 && (r = getResources(pii)) != null) {
            return r.getString(res);
        }
        return null;
    }

    private int runListPermissions() {
        boolean labels = false;
        boolean groups = false;
        boolean userOnly = false;
        boolean summary = false;
        boolean dangerousOnly = false;
        while (true) {
            try {
                String opt = nextOption();
                if (opt != null) {
                    if (opt.equals("-f")) {
                        labels = true;
                    } else if (opt.equals("-g")) {
                        groups = true;
                    } else if (opt.equals("-s")) {
                        groups = true;
                        labels = true;
                        summary = true;
                    } else if (opt.equals("-u")) {
                        userOnly = true;
                    } else if (opt.equals("-d")) {
                        dangerousOnly = true;
                    } else {
                        System.err.println("Error: Unknown option: " + opt);
                        return 1;
                    }
                } else {
                    String grp = nextOption();
                    ArrayList<String> groupList = new ArrayList<>();
                    if (groups) {
                        List<PermissionGroupInfo> infos = this.mPm.getAllPermissionGroups(0);
                        for (int i = 0; i < infos.size(); i++) {
                            groupList.add(infos.get(i).name);
                        }
                        groupList.add(null);
                    } else {
                        groupList.add(grp);
                    }
                    if (dangerousOnly) {
                        System.out.println("Dangerous Permissions:");
                        System.out.println("");
                        doListPermissions(groupList, groups, labels, summary, 1, 1);
                        if (userOnly) {
                            System.out.println("Normal Permissions:");
                            System.out.println("");
                            doListPermissions(groupList, groups, labels, summary, 0, 0);
                        }
                    } else if (userOnly) {
                        System.out.println("Dangerous and Normal Permissions:");
                        System.out.println("");
                        doListPermissions(groupList, groups, labels, summary, 0, 1);
                    } else {
                        System.out.println("All Permissions:");
                        System.out.println("");
                        doListPermissions(groupList, groups, labels, summary, -10000, 10000);
                    }
                    return 0;
                }
            } catch (RemoteException e) {
                System.err.println(e.toString());
                System.err.println(PM_NOT_RUNNING_ERR);
                return 1;
            }
        }
    }

    private void doListPermissions(ArrayList<String> groupList, boolean groups, boolean labels, boolean summary, int startProtectionLevel, int endProtectionLevel) throws RemoteException {
        int base;
        for (int i = 0; i < groupList.size(); i++) {
            String groupName = groupList.get(i);
            String prefix = "";
            if (groups) {
                if (i > 0) {
                    System.out.println("");
                }
                if (groupName != null) {
                    PermissionGroupInfo pgi = this.mPm.getPermissionGroupInfo(groupName, 0);
                    if (summary) {
                        Resources res = getResources(pgi);
                        if (res != null) {
                            System.out.print(loadText(pgi, pgi.labelRes, pgi.nonLocalizedLabel) + ": ");
                        } else {
                            System.out.print(pgi.name + ": ");
                        }
                    } else {
                        System.out.println((labels ? "+ " : "") + "group:" + pgi.name);
                        if (labels) {
                            System.out.println("  package:" + pgi.packageName);
                            Resources res2 = getResources(pgi);
                            if (res2 != null) {
                                System.out.println("  label:" + loadText(pgi, pgi.labelRes, pgi.nonLocalizedLabel));
                                System.out.println("  description:" + loadText(pgi, pgi.descriptionRes, pgi.nonLocalizedDescription));
                            }
                        }
                    }
                } else {
                    System.out.println(((!labels || summary) ? "" : "+ ") + "ungrouped:");
                }
                prefix = "  ";
            }
            List<PermissionInfo> ps = this.mPm.queryPermissionsByGroup(groupList.get(i), 0);
            int count = ps.size();
            boolean first = true;
            for (int p = 0; p < count; p++) {
                PermissionInfo pi = ps.get(p);
                if ((!groups || groupName != null || pi.group == null) && (base = pi.protectionLevel & 15) >= startProtectionLevel && base <= endProtectionLevel) {
                    if (summary) {
                        if (first) {
                            first = false;
                        } else {
                            System.out.print(", ");
                        }
                        Resources res3 = getResources(pi);
                        if (res3 != null) {
                            System.out.print(loadText(pi, pi.labelRes, pi.nonLocalizedLabel));
                        } else {
                            System.out.print(pi.name);
                        }
                    } else {
                        System.out.println(prefix + (labels ? "+ " : "") + "permission:" + pi.name);
                        if (labels) {
                            System.out.println(prefix + "  package:" + pi.packageName);
                            Resources res4 = getResources(pi);
                            if (res4 != null) {
                                System.out.println(prefix + "  label:" + loadText(pi, pi.labelRes, pi.nonLocalizedLabel));
                                System.out.println(prefix + "  description:" + loadText(pi, pi.descriptionRes, pi.nonLocalizedDescription));
                            }
                            System.out.println(prefix + "  protectionLevel:" + PermissionInfo.protectionToString(pi.protectionLevel));
                        }
                    }
                }
            }
            if (summary) {
                System.out.println("");
            }
        }
    }

    private int runPath() {
        String pkg = nextArg();
        if (pkg != null) {
            return displayPackageFilePath(pkg);
        }
        System.err.println("Error: no package specified");
        return 1;
    }

    private int runDump() {
        String pkg = nextArg();
        if (pkg == null) {
            System.err.println("Error: no package specified");
            return 1;
        }
        ActivityManager.dumpPackageStateStatic(FileDescriptor.out, pkg);
        return 0;
    }

    class LocalPackageInstallObserver extends PackageInstallObserver {
        String extraPackage;
        String extraPermission;
        boolean finished;
        int result;

        LocalPackageInstallObserver() {
        }

        public void onPackageInstalled(String name, int status, String msg, Bundle extras) {
            synchronized (this) {
                this.finished = true;
                this.result = status;
                if (status == -112) {
                    this.extraPermission = extras.getString("android.content.pm.extra.FAILURE_EXISTING_PERMISSION");
                    this.extraPackage = extras.getString("android.content.pm.extra.FAILURE_EXISTING_PACKAGE");
                }
                notifyAll();
            }
        }
    }

    private String installFailureToString(LocalPackageInstallObserver obs) {
        int result = obs.result;
        Field[] fields = PackageManager.class.getFields();
        for (Field f : fields) {
            if (f.getType() == Integer.TYPE) {
                int modifiers = f.getModifiers();
                if ((modifiers & 16) != 0 && (modifiers & 1) != 0 && (modifiers & 8) != 0) {
                    String fieldName = f.getName();
                    if (fieldName.startsWith("INSTALL_FAILED_") || fieldName.startsWith("INSTALL_PARSE_FAILED_")) {
                        try {
                            if (result == f.getInt(null)) {
                                StringBuilder sb = new StringBuilder(64);
                                sb.append(fieldName);
                                if (obs.extraPermission != null) {
                                    sb.append(" perm=");
                                    sb.append(obs.extraPermission);
                                }
                                if (obs.extraPackage != null) {
                                    sb.append(" pkg=" + obs.extraPackage);
                                }
                                return sb.toString();
                            }
                            continue;
                        } catch (IllegalAccessException e) {
                        }
                    }
                }
            }
        }
        return Integer.toString(result);
    }

    private int runSetInstallLocation() {
        String arg = nextArg();
        if (arg == null) {
            System.err.println("Error: no install location specified.");
            return 1;
        }
        try {
            int loc = Integer.parseInt(arg);
            try {
                if (!this.mPm.setInstallLocation(loc)) {
                    System.err.println("Error: install location has to be a number.");
                    return 1;
                }
                return 0;
            } catch (RemoteException e) {
                System.err.println(e.toString());
                System.err.println(PM_NOT_RUNNING_ERR);
                return 1;
            }
        } catch (NumberFormatException e2) {
            System.err.println("Error: install location has to be a number.");
            return 1;
        }
    }

    private int runGetInstallLocation() {
        int i = 1;
        try {
            int loc = this.mPm.getInstallLocation();
            String locStr = "invalid";
            if (loc == 0) {
                locStr = "auto";
            } else if (loc == 1) {
                locStr = "internal";
            } else if (loc == 2) {
                locStr = "external";
            }
            System.out.println(loc + "[" + locStr + "]");
            i = 0;
            return 0;
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(PM_NOT_RUNNING_ERR);
            return i;
        }
    }

    private int runInstall() {
        Uri originatingURI;
        Uri referrerURI;
        Uri verificationURI;
        int installFlags = 0;
        int userId = -1;
        String installerPackageName = null;
        String originatingUriString = null;
        String referrer = null;
        String abi = null;
        while (true) {
            String opt = nextOption();
            if (opt != null) {
                if (opt.equals("-l")) {
                    installFlags |= 1;
                } else if (opt.equals("-r")) {
                    installFlags |= 2;
                } else if (opt.equals("-i")) {
                    installerPackageName = nextOptionData();
                    if (installerPackageName == null) {
                        System.err.println("Error: no value specified for -i");
                        return 1;
                    }
                } else if (opt.equals("-t")) {
                    installFlags |= 4;
                } else if (opt.equals("-s")) {
                    installFlags |= 8;
                } else if (opt.equals("-f")) {
                    installFlags |= 16;
                } else if (opt.equals("-d")) {
                    installFlags |= 128;
                } else if (opt.equals("--originating-uri")) {
                    originatingUriString = nextOptionData();
                    if (originatingUriString == null) {
                        System.err.println("Error: must supply argument for --originating-uri");
                        return 1;
                    }
                } else if (opt.equals("--referrer")) {
                    referrer = nextOptionData();
                    if (referrer == null) {
                        System.err.println("Error: must supply argument for --referrer");
                        return 1;
                    }
                } else if (opt.equals("--abi")) {
                    abi = checkAbiArgument(nextOptionData());
                } else if (opt.equals("--user")) {
                    userId = Integer.parseInt(nextOptionData());
                } else {
                    System.err.println("Error: Unknown option: " + opt);
                    return 1;
                }
            } else {
                if (userId == -1) {
                    userId = 0;
                    installFlags |= 64;
                }
                if (originatingUriString != null) {
                    originatingURI = Uri.parse(originatingUriString);
                } else {
                    originatingURI = null;
                }
                if (referrer != null) {
                    referrerURI = Uri.parse(referrer);
                } else {
                    referrerURI = null;
                }
                String apkFilePath = nextArg();
                System.err.println("\tpkg: " + apkFilePath);
                if (apkFilePath == null) {
                    System.err.println("Error: no package specified");
                    return 1;
                }
                String verificationFilePath = nextArg();
                if (verificationFilePath != null) {
                    System.err.println("\tver: " + verificationFilePath);
                    verificationURI = Uri.fromFile(new File(verificationFilePath));
                } else {
                    verificationURI = null;
                }
                LocalPackageInstallObserver obs = new LocalPackageInstallObserver();
                try {
                    VerificationParams verificationParams = new VerificationParams(verificationURI, originatingURI, referrerURI, -1, (ManifestDigest) null);
                    this.mPm.installPackageAsUser(apkFilePath, obs.getBinder(), installFlags, installerPackageName, verificationParams, abi, userId);
                    synchronized (obs) {
                        while (!obs.finished) {
                            try {
                                obs.wait();
                            } catch (InterruptedException e) {
                            }
                        }
                        if (obs.result == 1) {
                            System.out.println("Success");
                            return 0;
                        }
                        System.err.println("Failure [" + installFailureToString(obs) + "]");
                        return 1;
                    }
                } catch (RemoteException e2) {
                    System.err.println(e2.toString());
                    System.err.println(PM_NOT_RUNNING_ERR);
                    return 1;
                }
            }
        }
    }

    private int runInstallCreate() throws RemoteException {
        int userId = -1;
        String installerPackageName = null;
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(1);
        while (true) {
            String opt = nextOption();
            if (opt != null) {
                if (opt.equals("-l")) {
                    params.installFlags |= 1;
                } else if (opt.equals("-r")) {
                    params.installFlags |= 2;
                } else if (opt.equals("-i")) {
                    installerPackageName = nextArg();
                    if (installerPackageName == null) {
                        throw new IllegalArgumentException("Missing installer package");
                    }
                } else if (opt.equals("-t")) {
                    params.installFlags |= 4;
                } else if (opt.equals("-s")) {
                    params.installFlags |= 8;
                } else if (opt.equals("-f")) {
                    params.installFlags |= 16;
                } else if (opt.equals("-d")) {
                    params.installFlags |= 128;
                } else if (opt.equals("--originating-uri")) {
                    params.originatingUri = Uri.parse(nextOptionData());
                } else if (opt.equals("--referrer")) {
                    params.referrerUri = Uri.parse(nextOptionData());
                } else if (opt.equals("-p")) {
                    params.mode = 2;
                    params.appPackageName = nextOptionData();
                    if (params.appPackageName == null) {
                        throw new IllegalArgumentException("Missing inherit package name");
                    }
                } else if (opt.equals("-S")) {
                    params.setSize(Long.parseLong(nextOptionData()));
                } else if (opt.equals("--abi")) {
                    params.abiOverride = checkAbiArgument(nextOptionData());
                } else if (opt.equals("--user")) {
                    userId = Integer.parseInt(nextOptionData());
                } else {
                    throw new IllegalArgumentException("Unknown option " + opt);
                }
            } else {
                if (userId == -1) {
                    userId = 0;
                    params.installFlags |= 64;
                }
                int sessionId = this.mInstaller.createSession(params, installerPackageName, userId);
                System.out.println("Success: created install session [" + sessionId + "]");
                return 0;
            }
        }
    }

    private int runInstallWrite() throws Throwable {
        PackageInstaller.Session session;
        long sizeBytes = -1;
        while (true) {
            String opt = nextOption();
            if (opt != null) {
                if (opt.equals("-S")) {
                    sizeBytes = Long.parseLong(nextOptionData());
                } else {
                    throw new IllegalArgumentException("Unknown option: " + opt);
                }
            } else {
                int sessionId = Integer.parseInt(nextArg());
                String splitName = nextArg();
                String path = nextArg();
                if ("-".equals(path)) {
                    path = null;
                } else if (path != null) {
                    File file = new File(path);
                    if (file.isFile()) {
                        sizeBytes = file.length();
                    }
                }
                PackageInstaller.SessionInfo info = this.mInstaller.getSessionInfo(sessionId);
                InputStream in = null;
                OutputStream out = null;
                try {
                    session = new PackageInstaller.Session(this.mInstaller.openSession(sessionId));
                } catch (Throwable th) {
                    th = th;
                    session = null;
                }
                try {
                    if (path != null) {
                        in = new FileInputStream(path);
                    } else {
                        in = new SizedInputStream(System.in, sizeBytes);
                    }
                    out = session.openWrite(splitName, 0L, sizeBytes);
                    int total = 0;
                    byte[] buffer = new byte[65536];
                    while (true) {
                        int c = in.read(buffer);
                        if (c != -1) {
                            total += c;
                            out.write(buffer, 0, c);
                            if (info.sizeBytes > 0) {
                                float fraction = c / info.sizeBytes;
                                session.addProgress(fraction);
                            }
                        } else {
                            session.fsync(out);
                            System.out.println("Success: streamed " + total + " bytes");
                            IoUtils.closeQuietly(out);
                            IoUtils.closeQuietly(in);
                            IoUtils.closeQuietly(session);
                            return 0;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    IoUtils.closeQuietly(out);
                    IoUtils.closeQuietly(in);
                    IoUtils.closeQuietly(session);
                    throw th;
                }
            }
        }
    }

    private int runInstallCommit() throws Throwable {
        int sessionId = Integer.parseInt(nextArg());
        PackageInstaller.Session session = null;
        try {
            PackageInstaller.Session session2 = new PackageInstaller.Session(this.mInstaller.openSession(sessionId));
            try {
                LocalIntentReceiver receiver = new LocalIntentReceiver();
                session2.commit(receiver.getIntentSender());
                Intent result = receiver.getResult();
                int status = result.getIntExtra("android.content.pm.extra.STATUS", 1);
                if (status == 0) {
                    System.out.println("Success");
                    IoUtils.closeQuietly(session2);
                    return 0;
                }
                Log.e(TAG, "Failure details: " + result.getExtras());
                System.err.println("Failure [" + result.getStringExtra("android.content.pm.extra.STATUS_MESSAGE") + "]");
                IoUtils.closeQuietly(session2);
                return 1;
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

    private int runInstallAbandon() throws Throwable {
        PackageInstaller.Session session;
        int sessionId = Integer.parseInt(nextArg());
        PackageInstaller.Session session2 = null;
        try {
            session = new PackageInstaller.Session(this.mInstaller.openSession(sessionId));
        } catch (Throwable th) {
            th = th;
        }
        try {
            session.abandon();
            System.out.println("Success");
            IoUtils.closeQuietly(session);
            return 0;
        } catch (Throwable th2) {
            th = th2;
            session2 = session;
            IoUtils.closeQuietly(session2);
            throw th;
        }
    }

    private int runSetInstaller() throws RemoteException {
        String targetPackage = nextArg();
        String installerPackageName = nextArg();
        if (targetPackage == null || installerPackageName == null) {
            throw new IllegalArgumentException("must provide both target and installer package names");
        }
        this.mPm.setInstallerPackageName(targetPackage, installerPackageName);
        System.out.println("Success");
        return 0;
    }

    public int runCreateUser() {
        int userId = -1;
        int flags = 0;
        while (true) {
            String opt = nextOption();
            if (opt != null) {
                if (!"--profileOf".equals(opt)) {
                    if ("--managed".equals(opt)) {
                        flags |= 32;
                    } else {
                        System.err.println("Error: unknown option " + opt);
                        showUsage();
                        break;
                    }
                } else {
                    String optionData = nextOptionData();
                    if (optionData == null || !isNumber(optionData)) {
                        break;
                    }
                    userId = Integer.parseInt(optionData);
                }
            } else {
                String arg = nextArg();
                if (arg == null) {
                    System.err.println("Error: no user name specified.");
                } else {
                    try {
                        UserInfo info = userId < 0 ? this.mUm.createUser(arg, flags) : this.mUm.createProfileForUser(arg, flags, userId);
                        if (info != null) {
                            System.out.println("Success: created user id " + info.id);
                        } else {
                            System.err.println("Error: couldn't create User.");
                        }
                    } catch (RemoteException e) {
                        System.err.println(e.toString());
                        System.err.println(PM_NOT_RUNNING_ERR);
                    }
                }
            }
        }
        return 1;
    }

    public int runRemoveUser() {
        int i = 1;
        String arg = nextArg();
        if (arg == null) {
            System.err.println("Error: no user id specified.");
        } else {
            try {
                int userId = Integer.parseInt(arg);
                try {
                    if (this.mUm.removeUser(userId)) {
                        System.out.println("Success: removed user");
                        i = 0;
                    } else {
                        System.err.println("Error: couldn't remove user id " + userId);
                    }
                } catch (RemoteException e) {
                    System.err.println(e.toString());
                    System.err.println(PM_NOT_RUNNING_ERR);
                }
            } catch (NumberFormatException e2) {
                System.err.println("Error: user id '" + arg + "' is not a number.");
            }
        }
        return i;
    }

    public int runListUsers() {
        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            List<UserInfo> users = this.mUm.getUsers(false);
            if (users == null) {
                System.err.println("Error: couldn't get users");
                return 1;
            }
            System.out.println("Users:");
            for (int i = 0; i < users.size(); i++) {
                String running = am.isUserRunning(users.get(i).id, false) ? " running" : "";
                System.out.println("\t" + users.get(i).toString() + running);
            }
            return 0;
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(PM_NOT_RUNNING_ERR);
            return 1;
        }
    }

    public int runGetMaxUsers() {
        System.out.println("Maximum supported users: " + UserManager.getMaxSupportedUsers());
        return 0;
    }

    public int runForceDexOpt() {
        String packageName = nextArg();
        try {
            this.mPm.forceDexOpt(packageName);
            return 0;
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    public int runSetUserRestriction() {
        boolean value;
        int userId = 0;
        String opt = nextOption();
        if (opt != null && "--user".equals(opt)) {
            String arg = nextArg();
            if (arg == null || !isNumber(arg)) {
                System.err.println("Error: valid userId not specified");
                return 1;
            }
            userId = Integer.parseInt(arg);
        }
        String restriction = nextArg();
        String arg2 = nextArg();
        if ("1".equals(arg2)) {
            value = true;
        } else if ("0".equals(arg2)) {
            value = false;
        } else {
            System.err.println("Error: valid value not specified");
            return 1;
        }
        try {
            Bundle restrictions = new Bundle();
            restrictions.putBoolean(restriction, value);
            this.mUm.setUserRestrictions(restrictions, userId);
            return 0;
        } catch (RemoteException e) {
            System.err.println(e.toString());
            return 1;
        }
    }

    private int runUninstall() throws RemoteException {
        int flags = 0;
        int userId = -1;
        while (true) {
            String opt = nextOption();
            if (opt != null) {
                if (opt.equals("-k")) {
                    flags |= 1;
                } else if (opt.equals("--user")) {
                    String param = nextArg();
                    if (isNumber(param)) {
                        userId = Integer.parseInt(param);
                    } else {
                        showUsage();
                        System.err.println("Error: Invalid user: " + param);
                        return 1;
                    }
                } else {
                    System.err.println("Error: Unknown option: " + opt);
                    return 1;
                }
            } else {
                String pkg = nextArg();
                if (pkg == null) {
                    System.err.println("Error: no package specified");
                    showUsage();
                    return 1;
                }
                if (userId == -1) {
                    userId = 0;
                    flags |= 2;
                } else {
                    try {
                        PackageInfo info = this.mPm.getPackageInfo(pkg, 0, userId);
                        if (info == null) {
                            System.err.println("Failure - not installed for " + userId);
                            return 1;
                        }
                        boolean isSystem = (info.applicationInfo.flags & 1) != 0;
                        if (isSystem) {
                            flags |= 4;
                        }
                    } catch (RemoteException e) {
                        System.err.println(e.toString());
                        System.err.println(PM_NOT_RUNNING_ERR);
                        return 1;
                    }
                }
                LocalIntentReceiver receiver = new LocalIntentReceiver();
                this.mInstaller.uninstall(pkg, flags, receiver.getIntentSender(), userId);
                Intent result = receiver.getResult();
                int status = result.getIntExtra("android.content.pm.extra.STATUS", 1);
                if (status == 0) {
                    System.out.println("Success");
                    return 0;
                }
                Log.e(TAG, "Failure details: " + result.getExtras());
                System.err.println("Failure [" + result.getStringExtra("android.content.pm.extra.STATUS_MESSAGE") + "]");
                return 1;
            }
        }
    }

    static class ClearDataObserver extends IPackageDataObserver.Stub {
        boolean finished;
        boolean result;

        ClearDataObserver() {
        }

        public void onRemoveCompleted(String packageName, boolean succeeded) throws RemoteException {
            synchronized (this) {
                this.finished = true;
                this.result = succeeded;
                notifyAll();
            }
        }
    }

    private int runClear() {
        int userId = 0;
        String option = nextOption();
        if (option != null && option.equals("--user")) {
            String optionData = nextOptionData();
            if (optionData == null || !isNumber(optionData)) {
                System.err.println("Error: no USER_ID specified");
                showUsage();
                return 1;
            }
            userId = Integer.parseInt(optionData);
        }
        String pkg = nextArg();
        if (pkg == null) {
            System.err.println("Error: no package specified");
            showUsage();
            return 1;
        }
        ClearDataObserver obs = new ClearDataObserver();
        try {
            ActivityManagerNative.getDefault().clearApplicationUserData(pkg, obs, userId);
            synchronized (obs) {
                while (!obs.finished) {
                    try {
                        obs.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            if (obs.result) {
                System.out.println("Success");
                return 0;
            }
            System.err.println("Failed");
            return 1;
        } catch (RemoteException e2) {
            System.err.println(e2.toString());
            System.err.println(PM_NOT_RUNNING_ERR);
            return 1;
        }
    }

    private static String enabledSettingToString(int state) {
        switch (state) {
            case 0:
                return "default";
            case 1:
                return "enabled";
            case 2:
                return "disabled";
            case 3:
                return "disabled-user";
            case 4:
                return "disabled-until-used";
            default:
                return "unknown";
        }
    }

    private static boolean isNumber(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private int runSetEnabledSetting(int state) {
        int userId = 0;
        String option = nextOption();
        if (option != null && option.equals("--user")) {
            String optionData = nextOptionData();
            if (optionData == null || !isNumber(optionData)) {
                System.err.println("Error: no USER_ID specified");
                showUsage();
                return 1;
            }
            userId = Integer.parseInt(optionData);
        }
        String pkg = nextArg();
        if (pkg == null) {
            System.err.println("Error: no package or component specified");
            showUsage();
            return 1;
        }
        ComponentName cn = ComponentName.unflattenFromString(pkg);
        if (cn == null) {
            try {
                this.mPm.setApplicationEnabledSetting(pkg, state, 0, userId, "shell:" + Process.myUid());
                System.out.println("Package " + pkg + " new state: " + enabledSettingToString(this.mPm.getApplicationEnabledSetting(pkg, userId)));
                return 0;
            } catch (RemoteException e) {
                System.err.println(e.toString());
                System.err.println(PM_NOT_RUNNING_ERR);
                return 1;
            }
        }
        try {
            this.mPm.setComponentEnabledSetting(cn, state, 0, userId);
            System.out.println("Component " + cn.toShortString() + " new state: " + enabledSettingToString(this.mPm.getComponentEnabledSetting(cn, userId)));
            return 0;
        } catch (RemoteException e2) {
            System.err.println(e2.toString());
            System.err.println(PM_NOT_RUNNING_ERR);
            return 1;
        }
    }

    private int runSetHiddenSetting(boolean state) {
        int userId = 0;
        String option = nextOption();
        if (option != null && option.equals("--user")) {
            String optionData = nextOptionData();
            if (optionData == null || !isNumber(optionData)) {
                System.err.println("Error: no USER_ID specified");
                showUsage();
                return 1;
            }
            userId = Integer.parseInt(optionData);
        }
        String pkg = nextArg();
        if (pkg == null) {
            System.err.println("Error: no package or component specified");
            showUsage();
            return 1;
        }
        try {
            this.mPm.setApplicationHiddenSettingAsUser(pkg, state, userId);
            System.out.println("Package " + pkg + " new hidden state: " + this.mPm.getApplicationHiddenSettingAsUser(pkg, userId));
            return 0;
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(PM_NOT_RUNNING_ERR);
            return 1;
        }
    }

    private int runGrantRevokePermission(boolean grant) {
        int i = 1;
        String pkg = nextArg();
        if (pkg == null) {
            System.err.println("Error: no package specified");
            showUsage();
            return 1;
        }
        String perm = nextArg();
        if (perm == null) {
            System.err.println("Error: no permission specified");
            showUsage();
            return 1;
        }
        try {
            if (grant) {
                this.mPm.grantPermission(pkg, perm);
            } else {
                this.mPm.revokePermission(pkg, perm);
            }
            i = 0;
            return 0;
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(PM_NOT_RUNNING_ERR);
            return i;
        } catch (IllegalArgumentException e2) {
            System.err.println("Bad argument: " + e2.toString());
            showUsage();
            return i;
        } catch (SecurityException e3) {
            System.err.println("Operation not allowed: " + e3.toString());
            return i;
        }
    }

    private int runSetPermissionEnforced() {
        String permission = nextArg();
        if (permission == null) {
            System.err.println("Error: no permission specified");
            showUsage();
            return 1;
        }
        String enforcedRaw = nextArg();
        if (enforcedRaw == null) {
            System.err.println("Error: no enforcement specified");
            showUsage();
            return 1;
        }
        boolean enforced = Boolean.parseBoolean(enforcedRaw);
        try {
            this.mPm.setPermissionEnforced(permission, enforced);
            return 0;
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(PM_NOT_RUNNING_ERR);
            return 1;
        } catch (IllegalArgumentException e2) {
            System.err.println("Bad argument: " + e2.toString());
            showUsage();
            return 1;
        } catch (SecurityException e3) {
            System.err.println("Operation not allowed: " + e3.toString());
            return 1;
        }
    }

    static class ClearCacheObserver extends IPackageDataObserver.Stub {
        boolean finished;
        boolean result;

        ClearCacheObserver() {
        }

        public void onRemoveCompleted(String packageName, boolean succeeded) throws RemoteException {
            synchronized (this) {
                this.finished = true;
                this.result = succeeded;
                notifyAll();
            }
        }
    }

    private int runTrimCaches() {
        String size = nextArg();
        if (size == null) {
            System.err.println("Error: no size specified");
            showUsage();
            return 1;
        }
        int len = size.length();
        long multiplier = 1;
        if (len > 1) {
            char c = size.charAt(len - 1);
            if (c == 'K' || c == 'k') {
                multiplier = 1024;
            } else if (c == 'M' || c == 'm') {
                multiplier = 1048576;
            } else if (c == 'G' || c == 'g') {
                multiplier = 1073741824;
            } else {
                System.err.println("Invalid suffix: " + c);
                showUsage();
                return 1;
            }
            size = size.substring(0, len - 1);
        }
        try {
            long sizeVal = Long.parseLong(size) * multiplier;
            ClearDataObserver obs = new ClearDataObserver();
            try {
                this.mPm.freeStorageAndNotify(sizeVal, obs);
                synchronized (obs) {
                    while (!obs.finished) {
                        try {
                            obs.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
                return 0;
            } catch (RemoteException e2) {
                System.err.println(e2.toString());
                System.err.println(PM_NOT_RUNNING_ERR);
                return 1;
            } catch (IllegalArgumentException e3) {
                System.err.println("Bad argument: " + e3.toString());
                showUsage();
                return 1;
            } catch (SecurityException e4) {
                System.err.println("Operation not allowed: " + e4.toString());
                return 1;
            }
        } catch (NumberFormatException e5) {
            System.err.println("Error: expected number at: " + size);
            showUsage();
            return 1;
        }
    }

    private int displayPackageFilePath(String pckg) {
        try {
            PackageInfo info = this.mPm.getPackageInfo(pckg, 0, 0);
            if (info != null && info.applicationInfo != null) {
                System.out.print("package:");
                System.out.println(info.applicationInfo.sourceDir);
                if (ArrayUtils.isEmpty(info.applicationInfo.splitSourceDirs)) {
                    return 0;
                }
                String[] arr$ = info.applicationInfo.splitSourceDirs;
                for (String splitSourceDir : arr$) {
                    System.out.print("package:");
                    System.out.println(splitSourceDir);
                }
                return 0;
            }
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(PM_NOT_RUNNING_ERR);
        }
        return 1;
    }

    private Resources getResources(PackageItemInfo pii) {
        Resources res;
        Resources res2 = this.mResourceCache.get(pii.packageName);
        if (res2 != null) {
            return res2;
        }
        try {
            ApplicationInfo ai = this.mPm.getApplicationInfo(pii.packageName, 0, 0);
            AssetManager am = new AssetManager();
            am.addAssetPath(ai.publicSourceDir);
            res = new Resources(am, null, null);
        } catch (RemoteException e) {
            e = e;
        }
        try {
            this.mResourceCache.put(pii.packageName, res);
            return res;
        } catch (RemoteException e2) {
            e = e2;
            System.err.println(e.toString());
            System.err.println(PM_NOT_RUNNING_ERR);
            return null;
        }
    }

    private static String checkAbiArgument(String abi) {
        if (TextUtils.isEmpty(abi)) {
            throw new IllegalArgumentException("Missing ABI argument");
        }
        if (!"-".equals(abi)) {
            String[] supportedAbis = Build.SUPPORTED_ABIS;
            for (String supportedAbi : supportedAbis) {
                if (!supportedAbi.equals(abi)) {
                }
            }
            throw new IllegalArgumentException("ABI " + abi + " not supported on this device");
        }
        return abi;
    }

    private static class LocalIntentReceiver {
        private IIntentSender.Stub mLocalSender;
        private final SynchronousQueue<Intent> mResult;

        private LocalIntentReceiver() {
            this.mResult = new SynchronousQueue<>();
            this.mLocalSender = new IIntentSender.Stub() {
                public int send(int code, Intent intent, String resolvedType, IIntentReceiver finishedReceiver, String requiredPermission) {
                    try {
                        LocalIntentReceiver.this.mResult.offer(intent, 5L, TimeUnit.SECONDS);
                        return 0;
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

    private String nextOption() {
        if (this.mNextArg >= this.mArgs.length) {
            return null;
        }
        String arg = this.mArgs[this.mNextArg];
        if (!arg.startsWith("-")) {
            return null;
        }
        this.mNextArg++;
        if (arg.equals("--")) {
            return null;
        }
        if (arg.length() > 1 && arg.charAt(1) != '-') {
            if (arg.length() > 2) {
                this.mCurArgData = arg.substring(2);
                return arg.substring(0, 2);
            }
            this.mCurArgData = null;
            return arg;
        }
        this.mCurArgData = null;
        return arg;
    }

    private String nextOptionData() {
        if (this.mCurArgData != null) {
            return this.mCurArgData;
        }
        if (this.mNextArg >= this.mArgs.length) {
            return null;
        }
        String str = this.mArgs[this.mNextArg];
        this.mNextArg++;
        return str;
    }

    private String nextArg() {
        if (this.mNextArg >= this.mArgs.length) {
            return null;
        }
        String str = this.mArgs[this.mNextArg];
        this.mNextArg++;
        return str;
    }

    private static int showUsage() {
        System.err.println("usage: pm list packages [-f] [-d] [-e] [-s] [-3] [-i] [-u] [--user USER_ID] [FILTER]");
        System.err.println("       pm list permission-groups");
        System.err.println("       pm list permissions [-g] [-f] [-d] [-u] [GROUP]");
        System.err.println("       pm list instrumentation [-f] [TARGET-PACKAGE]");
        System.err.println("       pm list features");
        System.err.println("       pm list libraries");
        System.err.println("       pm list users");
        System.err.println("       pm path PACKAGE");
        System.err.println("       pm dump PACKAGE");
        System.err.println("       pm install [-lrtsfd] [-i PACKAGE] [PATH]");
        System.err.println("       pm install-create [-lrtsfdp] [-i PACKAGE] [-S BYTES]");
        System.err.println("       pm install-write [-S BYTES] SESSION_ID SPLIT_NAME [PATH]");
        System.err.println("       pm install-commit SESSION_ID");
        System.err.println("       pm install-abandon SESSION_ID");
        System.err.println("       pm uninstall [-k] [--user USER_ID] PACKAGE");
        System.err.println("       pm set-installer PACKAGE INSTALLER");
        System.err.println("       pm clear [--user USER_ID] PACKAGE");
        System.err.println("       pm enable [--user USER_ID] PACKAGE_OR_COMPONENT");
        System.err.println("       pm disable [--user USER_ID] PACKAGE_OR_COMPONENT");
        System.err.println("       pm disable-user [--user USER_ID] PACKAGE_OR_COMPONENT");
        System.err.println("       pm disable-until-used [--user USER_ID] PACKAGE_OR_COMPONENT");
        System.err.println("       pm hide [--user USER_ID] PACKAGE_OR_COMPONENT");
        System.err.println("       pm unhide [--user USER_ID] PACKAGE_OR_COMPONENT");
        System.err.println("       pm grant PACKAGE PERMISSION");
        System.err.println("       pm revoke PACKAGE PERMISSION");
        System.err.println("       pm set-install-location [0/auto] [1/internal] [2/external]");
        System.err.println("       pm get-install-location");
        System.err.println("       pm set-permission-enforced PERMISSION [true|false]");
        System.err.println("       pm trim-caches DESIRED_FREE_SPACE");
        System.err.println("       pm create-user [--profileOf USER_ID] [--managed] USER_NAME");
        System.err.println("       pm remove-user USER_ID");
        System.err.println("       pm get-max-users");
        System.err.println("");
        System.err.println("pm list packages: prints all packages, optionally only");
        System.err.println("  those whose package name contains the text in FILTER.  Options:");
        System.err.println("    -f: see their associated file.");
        System.err.println("    -d: filter to only show disbled packages.");
        System.err.println("    -e: filter to only show enabled packages.");
        System.err.println("    -s: filter to only show system packages.");
        System.err.println("    -3: filter to only show third party packages.");
        System.err.println("    -i: see the installer for the packages.");
        System.err.println("    -u: also include uninstalled packages.");
        System.err.println("");
        System.err.println("pm list permission-groups: prints all known permission groups.");
        System.err.println("");
        System.err.println("pm list permissions: prints all known permissions, optionally only");
        System.err.println("  those in GROUP.  Options:");
        System.err.println("    -g: organize by group.");
        System.err.println("    -f: print all information.");
        System.err.println("    -s: short summary.");
        System.err.println("    -d: only list dangerous permissions.");
        System.err.println("    -u: list only the permissions users will see.");
        System.err.println("");
        System.err.println("pm list instrumentation: use to list all test packages; optionally");
        System.err.println("  supply <TARGET-PACKAGE> to list the test packages for a particular");
        System.err.println("  application.  Options:");
        System.err.println("    -f: list the .apk file for the test package.");
        System.err.println("");
        System.err.println("pm list features: prints all features of the system.");
        System.err.println("");
        System.err.println("pm list users: prints all users on the system.");
        System.err.println("");
        System.err.println("pm path: print the path to the .apk of the given PACKAGE.");
        System.err.println("");
        System.err.println("pm dump: print system state associated with the given PACKAGE.");
        System.err.println("");
        System.err.println("pm install: install a single legacy package");
        System.err.println("pm install-create: create an install session");
        System.err.println("    -l: forward lock application");
        System.err.println("    -r: replace existing application");
        System.err.println("    -t: allow test packages");
        System.err.println("    -i: specify the installer package name");
        System.err.println("    -s: install application on sdcard");
        System.err.println("    -f: install application on internal flash");
        System.err.println("    -d: allow version code downgrade");
        System.err.println("    -p: partial application install");
        System.err.println("    -S: size in bytes of entire session");
        System.err.println("");
        System.err.println("pm install-write: write a package into existing session; path may");
        System.err.println("  be '-' to read from stdin");
        System.err.println("    -S: size in bytes of package, required for stdin");
        System.err.println("");
        System.err.println("pm install-commit: perform install of fully staged session");
        System.err.println("pm install-abandon: abandon session");
        System.err.println("");
        System.err.println("pm set-installer: set installer package name");
        System.err.println("");
        System.err.println("pm uninstall: removes a package from the system. Options:");
        System.err.println("    -k: keep the data and cache directories around after package removal.");
        System.err.println("");
        System.err.println("pm clear: deletes all data associated with a package.");
        System.err.println("");
        System.err.println("pm enable, disable, disable-user, disable-until-used: these commands");
        System.err.println("  change the enabled state of a given package or component (written");
        System.err.println("  as \"package/class\").");
        System.err.println("");
        System.err.println("pm grant, revoke: these commands either grant or revoke permissions");
        System.err.println("  to applications.  Only optional permissions the application has");
        System.err.println("  declared can be granted or revoked.");
        System.err.println("");
        System.err.println("pm get-install-location: returns the current install location.");
        System.err.println("    0 [auto]: Let system decide the best location");
        System.err.println("    1 [internal]: Install on internal device storage");
        System.err.println("    2 [external]: Install on external media");
        System.err.println("");
        System.err.println("pm set-install-location: changes the default install location.");
        System.err.println("  NOTE: this is only intended for debugging; using this can cause");
        System.err.println("  applications to break and other undersireable behavior.");
        System.err.println("    0 [auto]: Let system decide the best location");
        System.err.println("    1 [internal]: Install on internal device storage");
        System.err.println("    2 [external]: Install on external media");
        System.err.println("");
        System.err.println("pm trim-caches: trim cache files to reach the given free space.");
        System.err.println("");
        System.err.println("pm create-user: create a new user with the given USER_NAME,");
        System.err.println("  printing the new user identifier of the user.");
        System.err.println("");
        System.err.println("pm remove-user: remove the user with the given USER_IDENTIFIER,");
        System.err.println("  deleting all data associated with that user");
        System.err.println("");
        return 1;
    }
}
