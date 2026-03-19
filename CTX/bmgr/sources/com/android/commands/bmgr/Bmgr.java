package com.android.commands.bmgr;

import android.app.backup.BackupProgress;
import android.app.backup.IBackupManager;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import android.app.backup.IRestoreObserver;
import android.app.backup.IRestoreSession;
import android.app.backup.ISelectBackupTransportCallback;
import android.app.backup.RestoreSet;
import android.content.ComponentName;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.ArraySet;
import com.android.internal.annotations.GuardedBy;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.function.IntFunction;

public final class Bmgr {
    static final String BMGR_NOT_RUNNING_ERR = "Error: Could not access the Backup Manager.  Is the system running?";
    static final String PM_NOT_RUNNING_ERR = "Error: Could not access the Package Manager.  Is the system running?";
    static final String TRANSPORT_NOT_RUNNING_ERR = "Error: Could not access the backup transport.  Is the system running?";
    private String[] mArgs;
    IBackupManager mBmgr;
    private int mNextArg;
    IRestoreSession mRestore;

    public static void main(String[] strArr) {
        try {
            new Bmgr().run(strArr);
        } catch (Exception e) {
            System.err.println("Exception caught:");
            e.printStackTrace();
        }
    }

    public void run(String[] strArr) {
        if (strArr.length < 1) {
            showUsage();
            return;
        }
        this.mBmgr = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
        if (this.mBmgr == null) {
            System.err.println(BMGR_NOT_RUNNING_ERR);
            return;
        }
        this.mArgs = strArr;
        String str = strArr[0];
        this.mNextArg = 1;
        if ("enabled".equals(str)) {
            doEnabled();
            return;
        }
        if ("enable".equals(str)) {
            doEnable();
            return;
        }
        if ("run".equals(str)) {
            doRun();
            return;
        }
        if ("backup".equals(str)) {
            doBackup();
            return;
        }
        if ("init".equals(str)) {
            doInit();
            return;
        }
        if ("list".equals(str)) {
            doList();
            return;
        }
        if ("restore".equals(str)) {
            doRestore();
            return;
        }
        if ("transport".equals(str)) {
            doTransport();
            return;
        }
        if ("wipe".equals(str)) {
            doWipe();
            return;
        }
        if ("fullbackup".equals(str)) {
            doFullTransportBackup();
            return;
        }
        if ("backupnow".equals(str)) {
            doBackupNow();
            return;
        }
        if ("cancel".equals(str)) {
            doCancel();
        } else if ("whitelist".equals(str)) {
            doPrintWhitelist();
        } else {
            System.err.println("Unknown command");
            showUsage();
        }
    }

    private String enableToString(boolean z) {
        return z ? "enabled" : "disabled";
    }

    private void doEnabled() {
        try {
            boolean zIsBackupEnabled = this.mBmgr.isBackupEnabled();
            System.out.println("Backup Manager currently " + enableToString(zIsBackupEnabled));
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(BMGR_NOT_RUNNING_ERR);
        }
    }

    private void doEnable() {
        String strNextArg = nextArg();
        if (strNextArg == null) {
            showUsage();
            return;
        }
        try {
            boolean z = Boolean.parseBoolean(strNextArg);
            this.mBmgr.setBackupEnabled(z);
            System.out.println("Backup Manager now " + enableToString(z));
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(BMGR_NOT_RUNNING_ERR);
        } catch (NumberFormatException e2) {
            showUsage();
        }
    }

    private void doRun() {
        try {
            this.mBmgr.backupNow();
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(BMGR_NOT_RUNNING_ERR);
        }
    }

    private void doBackup() {
        String strNextArg = nextArg();
        if (strNextArg == null) {
            showUsage();
            return;
        }
        try {
            this.mBmgr.dataChanged(strNextArg);
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(BMGR_NOT_RUNNING_ERR);
        }
    }

    private void doFullTransportBackup() {
        System.out.println("Performing full transport backup");
        ArraySet arraySet = new ArraySet();
        while (true) {
            String strNextArg = nextArg();
            if (strNextArg == null) {
                break;
            } else {
                arraySet.add(strNextArg);
            }
        }
        if (arraySet.size() > 0) {
            try {
                this.mBmgr.fullTransportBackup((String[]) arraySet.toArray(new String[arraySet.size()]));
            } catch (RemoteException e) {
                System.err.println(e.toString());
                System.err.println(BMGR_NOT_RUNNING_ERR);
            }
        }
    }

    abstract class Observer extends IBackupObserver.Stub {
        private final Object trigger = new Object();

        @GuardedBy("trigger")
        private volatile boolean done = false;

        Observer() {
        }

        public void onUpdate(String str, BackupProgress backupProgress) {
        }

        public void onResult(String str, int i) {
        }

        public void backupFinished(int i) {
            synchronized (this.trigger) {
                this.done = true;
                this.trigger.notify();
            }
        }

        public boolean done() {
            return this.done;
        }

        public void waitForCompletion() {
            waitForCompletion(0L);
        }

        public void waitForCompletion(long j) {
            long jElapsedRealtime = SystemClock.elapsedRealtime() + j;
            synchronized (this.trigger) {
                while (!this.done && (j <= 0 || SystemClock.elapsedRealtime() < jElapsedRealtime)) {
                    try {
                        this.trigger.wait(1000L);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    class BackupObserver extends Observer {
        BackupObserver() {
            super();
        }

        @Override
        public void onUpdate(String str, BackupProgress backupProgress) {
            super.onUpdate(str, backupProgress);
            System.out.println("Package " + str + " with progress: " + backupProgress.bytesTransferred + "/" + backupProgress.bytesExpected);
        }

        @Override
        public void onResult(String str, int i) {
            super.onResult(str, i);
            System.out.println("Package " + str + " with result: " + Bmgr.convertBackupStatusToString(i));
        }

        @Override
        public void backupFinished(int i) {
            super.backupFinished(i);
            System.out.println("Backup finished with result: " + Bmgr.convertBackupStatusToString(i));
            if (i == -2003) {
                System.out.println("Backups can be cancelled if a backup is already running, check backup dumpsys");
            }
        }
    }

    private static String convertBackupStatusToString(int i) {
        if (i == -1005) {
            return "Size quota exceeded";
        }
        if (i == -1000) {
            return "Transport error";
        }
        if (i == 0) {
            return "Success";
        }
        switch (i) {
            case -2003:
                return "Backup cancelled";
            case -2002:
                return "Package not found";
            case -2001:
                return "Backup is not allowed";
            default:
                switch (i) {
                    case -1003:
                        return "Agent error";
                    case -1002:
                        return "Transport rejected package because it wasn't able to process it at the time";
                    default:
                        return "Unknown error";
                }
        }
    }

    private void backupNowAllPackages(boolean z) {
        List list;
        String[] strArrFilterAppsEligibleForBackup;
        IPackageManager iPackageManagerAsInterface = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        if (iPackageManagerAsInterface == null) {
            System.err.println(PM_NOT_RUNNING_ERR);
            return;
        }
        try {
            list = iPackageManagerAsInterface.getInstalledPackages(0, 0).getList();
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(PM_NOT_RUNNING_ERR);
            list = null;
        }
        if (list != null) {
            String[] strArr = new String[0];
            try {
                strArrFilterAppsEligibleForBackup = this.mBmgr.filterAppsEligibleForBackup((String[]) list.stream().map(new Function() {
                    @Override
                    public final Object apply(Object obj) {
                        return ((PackageInfo) obj).packageName;
                    }
                }).toArray(new IntFunction() {
                    @Override
                    public final Object apply(int i) {
                        return Bmgr.lambda$backupNowAllPackages$1(i);
                    }
                }));
            } catch (RemoteException e2) {
                System.err.println(e2.toString());
                System.err.println(BMGR_NOT_RUNNING_ERR);
                strArrFilterAppsEligibleForBackup = strArr;
            }
            backupNowPackages(Arrays.asList(strArrFilterAppsEligibleForBackup), z);
        }
    }

    static String[] lambda$backupNowAllPackages$1(int i) {
        return new String[i];
    }

    private void backupNowPackages(List<String> list, boolean z) {
        int i;
        if (z) {
            i = 1;
        } else {
            i = 0;
        }
        try {
            BackupObserver backupObserver = new BackupObserver();
            if (this.mBmgr.requestBackup((String[]) list.toArray(new String[list.size()]), backupObserver, (IBackupManagerMonitor) null, i) == 0) {
                backupObserver.waitForCompletion();
            } else {
                System.err.println("Unable to run backup");
            }
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(BMGR_NOT_RUNNING_ERR);
        }
    }

    private void doBackupNow() {
        ArrayList arrayList = new ArrayList();
        boolean z = false;
        boolean z2 = false;
        while (true) {
            String strNextArg = nextArg();
            if (strNextArg == null) {
                break;
            }
            if (strNextArg.equals("--all")) {
                z = true;
            } else if (strNextArg.equals("--non-incremental")) {
                z2 = true;
            } else if (strNextArg.equals("--incremental")) {
                z2 = false;
            } else if (!arrayList.contains(strNextArg)) {
                arrayList.add(strNextArg);
            }
        }
        if (z) {
            if (arrayList.size() != 0) {
                System.err.println("Provide only '--all' flag or list of packages.");
                return;
            }
            PrintStream printStream = System.out;
            StringBuilder sb = new StringBuilder();
            sb.append("Running ");
            sb.append(z2 ? "non-" : "");
            sb.append("incremental backup for all packages.");
            printStream.println(sb.toString());
            backupNowAllPackages(z2);
            return;
        }
        if (arrayList.size() <= 0) {
            System.err.println("Provide '--all' flag or list of packages.");
            return;
        }
        PrintStream printStream2 = System.out;
        StringBuilder sb2 = new StringBuilder();
        sb2.append("Running ");
        sb2.append(z2 ? "non-" : "");
        sb2.append("incremental backup for ");
        sb2.append(arrayList.size());
        sb2.append(" requested packages.");
        printStream2.println(sb2.toString());
        backupNowPackages(arrayList, z2);
    }

    private void doCancel() {
        if ("backups".equals(nextArg())) {
            try {
                this.mBmgr.cancelBackups();
                return;
            } catch (RemoteException e) {
                System.err.println(e.toString());
                System.err.println(BMGR_NOT_RUNNING_ERR);
                return;
            }
        }
        System.err.println("Unknown command.");
    }

    private void doTransport() {
        try {
            String strNextArg = nextArg();
            if (strNextArg == null) {
                showUsage();
                return;
            }
            if ("-c".equals(strNextArg)) {
                doTransportByComponent();
                return;
            }
            String strSelectBackupTransport = this.mBmgr.selectBackupTransport(strNextArg);
            if (strSelectBackupTransport == null) {
                System.out.println("Unknown transport '" + strNextArg + "' specified; no changes made.");
                return;
            }
            System.out.println("Selected transport " + strNextArg + " (formerly " + strSelectBackupTransport + ")");
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(BMGR_NOT_RUNNING_ERR);
        }
    }

    private void doTransportByComponent() {
        String strNextArg = nextArg();
        if (strNextArg == null) {
            showUsage();
            return;
        }
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            this.mBmgr.selectBackupTransportAsync(ComponentName.unflattenFromString(strNextArg), new ISelectBackupTransportCallback.Stub() {
                public void onSuccess(String str) {
                    System.out.println("Success. Selected transport: " + str);
                    countDownLatch.countDown();
                }

                public void onFailure(int i) {
                    System.err.println("Failure. error=" + i);
                    countDownLatch.countDown();
                }
            });
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                System.err.println("Operation interrupted.");
            }
        } catch (RemoteException e2) {
            System.err.println(e2.toString());
            System.err.println(BMGR_NOT_RUNNING_ERR);
        }
    }

    private void doWipe() {
        String strNextArg = nextArg();
        if (strNextArg == null) {
            showUsage();
            return;
        }
        String strNextArg2 = nextArg();
        if (strNextArg2 == null) {
            showUsage();
            return;
        }
        try {
            this.mBmgr.clearBackupData(strNextArg, strNextArg2);
            System.out.println("Wiped backup data for " + strNextArg2 + " on " + strNextArg);
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(BMGR_NOT_RUNNING_ERR);
        }
    }

    class InitObserver extends Observer {
        public int result;

        InitObserver() {
            super();
            this.result = -1000;
        }

        @Override
        public void backupFinished(int i) {
            super.backupFinished(i);
            this.result = i;
        }
    }

    private void doInit() {
        ArraySet arraySet = new ArraySet();
        while (true) {
            String strNextArg = nextArg();
            if (strNextArg == null) {
                break;
            } else {
                arraySet.add(strNextArg);
            }
        }
        if (arraySet.size() == 0) {
            showUsage();
            return;
        }
        InitObserver initObserver = new InitObserver();
        try {
            System.out.println("Initializing transports: " + arraySet);
            this.mBmgr.initializeTransports((String[]) arraySet.toArray(new String[arraySet.size()]), initObserver);
            initObserver.waitForCompletion(30000L);
            System.out.println("Initialization result: " + initObserver.result);
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(BMGR_NOT_RUNNING_ERR);
        }
    }

    private void doList() {
        String strNextArg = nextArg();
        if ("transports".equals(strNextArg)) {
            doListTransports();
            return;
        }
        try {
            this.mRestore = this.mBmgr.beginRestoreSession((String) null, (String) null);
            if (this.mRestore == null) {
                System.err.println(BMGR_NOT_RUNNING_ERR);
                return;
            }
            if ("sets".equals(strNextArg)) {
                doListRestoreSets();
            } else if ("transports".equals(strNextArg)) {
                doListTransports();
            }
            this.mRestore.endRestoreSession();
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(BMGR_NOT_RUNNING_ERR);
        }
    }

    private void doListTransports() {
        try {
            int i = 0;
            if ("-c".equals(nextArg())) {
                ComponentName[] componentNameArrListAllTransportComponents = this.mBmgr.listAllTransportComponents();
                int length = componentNameArrListAllTransportComponents.length;
                while (i < length) {
                    System.out.println(componentNameArrListAllTransportComponents[i].flattenToShortString());
                    i++;
                }
                return;
            }
            String currentTransport = this.mBmgr.getCurrentTransport();
            String[] strArrListAllTransports = this.mBmgr.listAllTransports();
            if (strArrListAllTransports != null && strArrListAllTransports.length != 0) {
                int length2 = strArrListAllTransports.length;
                while (i < length2) {
                    String str = strArrListAllTransports[i];
                    String str2 = str.equals(currentTransport) ? "  * " : "    ";
                    System.out.println(str2 + str);
                    i++;
                }
                return;
            }
            System.out.println("No transports available.");
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(BMGR_NOT_RUNNING_ERR);
        }
    }

    private void doListRestoreSets() {
        try {
            RestoreObserver restoreObserver = new RestoreObserver();
            if (this.mRestore.getAvailableRestoreSets(restoreObserver, (IBackupManagerMonitor) null) != 0) {
                System.out.println("Unable to request restore sets");
            } else {
                restoreObserver.waitForCompletion();
                printRestoreSets(restoreObserver.sets);
            }
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(TRANSPORT_NOT_RUNNING_ERR);
        }
    }

    private void printRestoreSets(RestoreSet[] restoreSetArr) {
        if (restoreSetArr == null || restoreSetArr.length == 0) {
            System.out.println("No restore sets");
            return;
        }
        for (RestoreSet restoreSet : restoreSetArr) {
            System.out.println("  " + Long.toHexString(restoreSet.token) + " : " + restoreSet.name);
        }
    }

    class RestoreObserver extends IRestoreObserver.Stub {
        boolean done;
        RestoreSet[] sets = null;

        RestoreObserver() {
        }

        public void restoreSetsAvailable(RestoreSet[] restoreSetArr) {
            synchronized (this) {
                this.sets = restoreSetArr;
                this.done = true;
                notify();
            }
        }

        public void restoreStarting(int i) {
            System.out.println("restoreStarting: " + i + " packages");
        }

        public void onUpdate(int i, String str) {
            System.out.println("onUpdate: " + i + " = " + str);
        }

        public void restoreFinished(int i) {
            System.out.println("restoreFinished: " + i);
            synchronized (this) {
                this.done = true;
                notify();
            }
        }

        public void waitForCompletion() {
            synchronized (this) {
                while (!this.done) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
                this.done = false;
            }
        }
    }

    private void doRestore() {
        String strNextArg = nextArg();
        if (strNextArg == null) {
            showUsage();
            return;
        }
        if (strNextArg.indexOf(46) >= 0 || strNextArg.equals("android")) {
            doRestorePackage(strNextArg);
        } else {
            try {
                long j = Long.parseLong(strNextArg, 16);
                HashSet<String> hashSet = null;
                while (true) {
                    String strNextArg2 = nextArg();
                    if (strNextArg2 == null) {
                        break;
                    }
                    if (hashSet == null) {
                        hashSet = new HashSet<>();
                    }
                    hashSet.add(strNextArg2);
                }
                doRestoreAll(j, hashSet);
            } catch (NumberFormatException e) {
                showUsage();
                return;
            }
        }
        System.out.println("done");
    }

    private void doRestorePackage(String str) {
        try {
            this.mRestore = this.mBmgr.beginRestoreSession(str, (String) null);
            if (this.mRestore == null) {
                System.err.println(BMGR_NOT_RUNNING_ERR);
                return;
            }
            RestoreObserver restoreObserver = new RestoreObserver();
            if (this.mRestore.restorePackage(str, restoreObserver, (IBackupManagerMonitor) null) == 0) {
                restoreObserver.waitForCompletion();
            } else {
                System.err.println("Unable to restore package " + str);
            }
            this.mRestore.endRestoreSession();
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(BMGR_NOT_RUNNING_ERR);
        }
    }

    private void doRestoreAll(long j, HashSet<String> hashSet) {
        RestoreSet[] restoreSetArr;
        RestoreObserver restoreObserver = new RestoreObserver();
        try {
            this.mRestore = this.mBmgr.beginRestoreSession((String) null, (String) null);
            if (this.mRestore == null) {
                System.err.println(BMGR_NOT_RUNNING_ERR);
                return;
            }
            boolean z = false;
            if (this.mRestore.getAvailableRestoreSets(restoreObserver, (IBackupManagerMonitor) null) == 0) {
                restoreObserver.waitForCompletion();
                restoreSetArr = restoreObserver.sets;
                if (restoreSetArr != null) {
                    int length = restoreSetArr.length;
                    int i = 0;
                    while (true) {
                        if (i >= length) {
                            break;
                        }
                        RestoreSet restoreSet = restoreSetArr[i];
                        if (restoreSet.token != j) {
                            i++;
                        } else {
                            System.out.println("Scheduling restore: " + restoreSet.name);
                            if (hashSet == null) {
                                if (this.mRestore.restoreAll(j, restoreObserver, (IBackupManagerMonitor) null) == 0) {
                                    z = true;
                                }
                            } else {
                                String[] strArr = new String[hashSet.size()];
                                hashSet.toArray(strArr);
                                if (this.mRestore.restoreSome(j, restoreObserver, (IBackupManagerMonitor) null, strArr) == 0) {
                                    z = true;
                                }
                            }
                        }
                    }
                }
            } else {
                restoreSetArr = null;
            }
            if (!z) {
                if (restoreSetArr == null || restoreSetArr.length == 0) {
                    System.out.println("No available restore sets; no restore performed");
                } else {
                    System.out.println("No matching restore set token.  Available sets:");
                    printRestoreSets(restoreSetArr);
                }
            }
            if (z) {
                restoreObserver.waitForCompletion();
            }
            this.mRestore.endRestoreSession();
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(BMGR_NOT_RUNNING_ERR);
        }
    }

    private void doPrintWhitelist() {
        try {
            String[] transportWhitelist = this.mBmgr.getTransportWhitelist();
            if (transportWhitelist != null) {
                for (String str : transportWhitelist) {
                    System.out.println(str);
                }
            }
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(BMGR_NOT_RUNNING_ERR);
        }
    }

    private String nextArg() {
        if (this.mNextArg >= this.mArgs.length) {
            return null;
        }
        String str = this.mArgs[this.mNextArg];
        this.mNextArg++;
        return str;
    }

    private static void showUsage() {
        System.err.println("usage: bmgr [backup|restore|list|transport|run]");
        System.err.println("       bmgr backup PACKAGE");
        System.err.println("       bmgr enable BOOL");
        System.err.println("       bmgr enabled");
        System.err.println("       bmgr list transports [-c]");
        System.err.println("       bmgr list sets");
        System.err.println("       bmgr transport WHICH|-c WHICH_COMPONENT");
        System.err.println("       bmgr restore TOKEN");
        System.err.println("       bmgr restore TOKEN PACKAGE...");
        System.err.println("       bmgr restore PACKAGE");
        System.err.println("       bmgr run");
        System.err.println("       bmgr wipe TRANSPORT PACKAGE");
        System.err.println("       bmgr fullbackup PACKAGE...");
        System.err.println("       bmgr backupnow --all|PACKAGE...");
        System.err.println("       bmgr cancel backups");
        System.err.println("");
        System.err.println("The 'backup' command schedules a backup pass for the named package.");
        System.err.println("Note that the backup pass will effectively be a no-op if the package");
        System.err.println("does not actually have changed data to store.");
        System.err.println("");
        System.err.println("The 'enable' command enables or disables the entire backup mechanism.");
        System.err.println("If the argument is 'true' it will be enabled, otherwise it will be");
        System.err.println("disabled.  When disabled, neither backup or restore operations will");
        System.err.println("be performed.");
        System.err.println("");
        System.err.println("The 'enabled' command reports the current enabled/disabled state of");
        System.err.println("the backup mechanism.");
        System.err.println("");
        System.err.println("The 'list transports' command reports the names of the backup transports");
        System.err.println("BackupManager is currently bound to. These names can be passed as arguments");
        System.err.println("to the 'transport' and 'wipe' commands.  The currently active transport");
        System.err.println("is indicated with a '*' character. If -c flag is used, all available");
        System.err.println("transport components on the device are listed. These can be used with");
        System.err.println("the component variant of 'transport' command.");
        System.err.println("");
        System.err.println("The 'list sets' command reports the token and name of each restore set");
        System.err.println("available to the device via the currently active transport.");
        System.err.println("");
        System.err.println("The 'transport' command designates the named transport as the currently");
        System.err.println("active one.  This setting is persistent across reboots. If -c flag is");
        System.err.println("specified, the following string is treated as a component name.");
        System.err.println("");
        System.err.println("The 'restore' command when given just a restore token initiates a full-system");
        System.err.println("restore operation from the currently active transport.  It will deliver");
        System.err.println("the restore set designated by the TOKEN argument to each application");
        System.err.println("that had contributed data to that restore set.");
        System.err.println("");
        System.err.println("The 'restore' command when given a token and one or more package names");
        System.err.println("initiates a restore operation of just those given packages from the restore");
        System.err.println("set designated by the TOKEN argument.  It is effectively the same as the");
        System.err.println("'restore' operation supplying only a token, but applies a filter to the");
        System.err.println("set of applications to be restored.");
        System.err.println("");
        System.err.println("The 'restore' command when given just a package name intiates a restore of");
        System.err.println("just that one package according to the restore set selection algorithm");
        System.err.println("used by the RestoreSession.restorePackage() method.");
        System.err.println("");
        System.err.println("The 'run' command causes any scheduled backup operation to be initiated");
        System.err.println("immediately, without the usual waiting period for batching together");
        System.err.println("data changes.");
        System.err.println("");
        System.err.println("The 'wipe' command causes all backed-up data for the given package to be");
        System.err.println("erased from the given transport's storage.  The next backup operation");
        System.err.println("that the given application performs will rewrite its entire data set.");
        System.err.println("Transport names to use here are those reported by 'list transports'.");
        System.err.println("");
        System.err.println("The 'fullbackup' command induces a full-data stream backup for one or more");
        System.err.println("packages.  The data is sent via the currently active transport.");
        System.err.println("");
        System.err.println("The 'backupnow' command runs an immediate backup for one or more packages.");
        System.err.println("    --all flag runs backup for all eligible packages.");
        System.err.println("For each package it will run key/value or full data backup ");
        System.err.println("depending on the package's manifest declarations.");
        System.err.println("The data is sent via the currently active transport.");
        System.err.println("The 'cancel backups' command cancels all running backups.");
    }
}
