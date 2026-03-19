package com.android.server.print;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.print.IPrintDocumentAdapter;
import android.print.IPrintJobStateChangeListener;
import android.print.IPrintManager;
import android.print.IPrintServicesChangeListener;
import android.print.IPrinterDiscoveryObserver;
import android.print.PrintAttributes;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrinterId;
import android.printservice.PrintServiceInfo;
import android.printservice.recommendation.IRecommendationsChangeListener;
import android.printservice.recommendation.RecommendationInfo;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.Preconditions;
import com.android.server.SystemService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

public final class PrintManagerService extends SystemService {
    private static final String LOG_TAG = "PrintManagerService";
    private final PrintManagerImpl mPrintManagerImpl;

    public PrintManagerService(Context context) {
        super(context);
        this.mPrintManagerImpl = new PrintManagerImpl(context);
    }

    @Override
    public void onStart() {
        publishBinderService("print", this.mPrintManagerImpl);
    }

    @Override
    public void onUnlockUser(int userHandle) {
        this.mPrintManagerImpl.handleUserUnlocked(userHandle);
    }

    @Override
    public void onStopUser(int userHandle) {
        this.mPrintManagerImpl.handleUserStopped(userHandle);
    }

    class PrintManagerImpl extends IPrintManager.Stub {
        private static final int BACKGROUND_USER_ID = -10;
        private final Context mContext;
        private final UserManager mUserManager;
        private final Object mLock = new Object();
        private final SparseArray<UserState> mUserStates = new SparseArray<>();

        PrintManagerImpl(Context context) {
            this.mContext = context;
            this.mUserManager = (UserManager) context.getSystemService("user");
            registerContentObservers();
            registerBroadcastReceivers();
        }

        public Bundle print(String printJobName, IPrintDocumentAdapter adapter, PrintAttributes attributes, String packageName, int appId, int userId) {
            String printJobName2 = (String) Preconditions.checkStringNotEmpty(printJobName);
            IPrintDocumentAdapter adapter2 = (IPrintDocumentAdapter) Preconditions.checkNotNull(adapter);
            String packageName2 = (String) Preconditions.checkStringNotEmpty(packageName);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return null;
                }
                int resolvedAppId = resolveCallingAppEnforcingPermissions(appId);
                String resolvedPackageName = resolveCallingPackageNameEnforcingSecurity(packageName2);
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    return userState.print(printJobName2, adapter2, attributes, resolvedPackageName, resolvedAppId);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public List<PrintJobInfo> getPrintJobInfos(int appId, int userId) {
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return null;
                }
                int resolvedAppId = resolveCallingAppEnforcingPermissions(appId);
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    return userState.getPrintJobInfos(resolvedAppId);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public PrintJobInfo getPrintJobInfo(PrintJobId printJobId, int appId, int userId) {
            if (printJobId == null) {
                return null;
            }
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return null;
                }
                int resolvedAppId = resolveCallingAppEnforcingPermissions(appId);
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    return userState.getPrintJobInfo(printJobId, resolvedAppId);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public Icon getCustomPrinterIcon(PrinterId printerId, int userId) {
            PrinterId printerId2 = (PrinterId) Preconditions.checkNotNull(printerId);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return null;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    return userState.getCustomPrinterIcon(printerId2);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void cancelPrintJob(PrintJobId printJobId, int appId, int userId) {
            if (printJobId == null) {
                return;
            }
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                int resolvedAppId = resolveCallingAppEnforcingPermissions(appId);
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    userState.cancelPrintJob(printJobId, resolvedAppId);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void restartPrintJob(PrintJobId printJobId, int appId, int userId) {
            if (printJobId == null) {
                return;
            }
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                int resolvedAppId = resolveCallingAppEnforcingPermissions(appId);
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    userState.restartPrintJob(printJobId, resolvedAppId);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public List<PrintServiceInfo> getPrintServices(int selectionFlags, int userId) {
            Preconditions.checkFlagsArgument(selectionFlags, 3);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return null;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    return userState.getPrintServices(selectionFlags);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void setPrintServiceEnabled(ComponentName service, boolean isEnabled, int userId) {
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            int appId = UserHandle.getAppId(Binder.getCallingUid());
            if (appId != 1000) {
                try {
                    if (appId != UserHandle.getAppId(this.mContext.getPackageManager().getPackageUidAsUser("com.android.printspooler", resolvedUserId))) {
                        throw new SecurityException("Only system and print spooler can call this");
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(PrintManagerService.LOG_TAG, "Could not verify caller", e);
                    return;
                }
            }
            ComponentName service2 = (ComponentName) Preconditions.checkNotNull(service);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    userState.setPrintServiceEnabled(service2, isEnabled);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public List<RecommendationInfo> getPrintServiceRecommendations(int userId) {
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return null;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    return userState.getPrintServiceRecommendations();
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void createPrinterDiscoverySession(IPrinterDiscoveryObserver observer, int userId) {
            IPrinterDiscoveryObserver observer2 = (IPrinterDiscoveryObserver) Preconditions.checkNotNull(observer);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    userState.createPrinterDiscoverySession(observer2);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void destroyPrinterDiscoverySession(IPrinterDiscoveryObserver observer, int userId) {
            IPrinterDiscoveryObserver observer2 = (IPrinterDiscoveryObserver) Preconditions.checkNotNull(observer);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    userState.destroyPrinterDiscoverySession(observer2);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void startPrinterDiscovery(IPrinterDiscoveryObserver observer, List<PrinterId> priorityList, int userId) {
            IPrinterDiscoveryObserver observer2 = (IPrinterDiscoveryObserver) Preconditions.checkNotNull(observer);
            if (priorityList != null) {
                priorityList = (List) Preconditions.checkCollectionElementsNotNull(priorityList, "PrinterId");
            }
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    userState.startPrinterDiscovery(observer2, priorityList);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void stopPrinterDiscovery(IPrinterDiscoveryObserver observer, int userId) {
            IPrinterDiscoveryObserver observer2 = (IPrinterDiscoveryObserver) Preconditions.checkNotNull(observer);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    userState.stopPrinterDiscovery(observer2);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void validatePrinters(List<PrinterId> printerIds, int userId) {
            List<PrinterId> printerIds2 = (List) Preconditions.checkCollectionElementsNotNull(printerIds, "PrinterId");
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    userState.validatePrinters(printerIds2);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void startPrinterStateTracking(PrinterId printerId, int userId) {
            PrinterId printerId2 = (PrinterId) Preconditions.checkNotNull(printerId);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    userState.startPrinterStateTracking(printerId2);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void stopPrinterStateTracking(PrinterId printerId, int userId) {
            PrinterId printerId2 = (PrinterId) Preconditions.checkNotNull(printerId);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    userState.stopPrinterStateTracking(printerId2);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void addPrintJobStateChangeListener(IPrintJobStateChangeListener listener, int appId, int userId) throws RemoteException {
            IPrintJobStateChangeListener listener2 = (IPrintJobStateChangeListener) Preconditions.checkNotNull(listener);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                int resolvedAppId = resolveCallingAppEnforcingPermissions(appId);
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    userState.addPrintJobStateChangeListener(listener2, resolvedAppId);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void removePrintJobStateChangeListener(IPrintJobStateChangeListener listener, int userId) {
            IPrintJobStateChangeListener listener2 = (IPrintJobStateChangeListener) Preconditions.checkNotNull(listener);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    userState.removePrintJobStateChangeListener(listener2);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void addPrintServicesChangeListener(IPrintServicesChangeListener listener, int userId) throws RemoteException {
            IPrintServicesChangeListener listener2 = (IPrintServicesChangeListener) Preconditions.checkNotNull(listener);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    userState.addPrintServicesChangeListener(listener2);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void removePrintServicesChangeListener(IPrintServicesChangeListener listener, int userId) {
            IPrintServicesChangeListener listener2 = (IPrintServicesChangeListener) Preconditions.checkNotNull(listener);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    userState.removePrintServicesChangeListener(listener2);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void addPrintServiceRecommendationsChangeListener(IRecommendationsChangeListener listener, int userId) throws RemoteException {
            IRecommendationsChangeListener listener2 = (IRecommendationsChangeListener) Preconditions.checkNotNull(listener);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    userState.addPrintServiceRecommendationsChangeListener(listener2);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void removePrintServiceRecommendationsChangeListener(IRecommendationsChangeListener listener, int userId) {
            IRecommendationsChangeListener listener2 = (IRecommendationsChangeListener) Preconditions.checkNotNull(listener);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, false);
                long identity = Binder.clearCallingIdentity();
                try {
                    userState.removePrintServiceRecommendationsChangeListener(listener2);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            FileDescriptor fd2 = (FileDescriptor) Preconditions.checkNotNull(fd);
            PrintWriter pw2 = (PrintWriter) Preconditions.checkNotNull(pw);
            if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                pw2.println("Permission Denial: can't dump PrintManager from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
                return;
            }
            synchronized (this.mLock) {
                long identity = Binder.clearCallingIdentity();
                try {
                    pw2.println("PRINT MANAGER STATE (dumpsys print)");
                    int userStateCount = this.mUserStates.size();
                    for (int i = 0; i < userStateCount; i++) {
                        UserState userState = this.mUserStates.valueAt(i);
                        userState.dump(fd2, pw2, "");
                        pw2.println();
                    }
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        private void registerContentObservers() {
            final Uri enabledPrintServicesUri = Settings.Secure.getUriFor("disabled_print_services");
            ContentObserver observer = new ContentObserver(BackgroundThread.getHandler()) {
                @Override
                public void onChange(boolean selfChange, Uri uri, int userId) {
                    if (!enabledPrintServicesUri.equals(uri)) {
                        return;
                    }
                    synchronized (PrintManagerImpl.this.mLock) {
                        int userCount = PrintManagerImpl.this.mUserStates.size();
                        for (int i = 0; i < userCount; i++) {
                            if (userId != -1) {
                                if (userId == PrintManagerImpl.this.mUserStates.keyAt(i)) {
                                    ((UserState) PrintManagerImpl.this.mUserStates.valueAt(i)).updateIfNeededLocked();
                                }
                            }
                        }
                    }
                }
            };
            this.mContext.getContentResolver().registerContentObserver(enabledPrintServicesUri, false, observer, -1);
        }

        private void registerBroadcastReceivers() {
            PackageMonitor monitor = new PackageMonitor() {
                private boolean hasPrintService(String packageName) {
                    Intent intent = new Intent("android.printservice.PrintService");
                    intent.setPackage(packageName);
                    List<ResolveInfo> installedServices = PrintManagerImpl.this.mContext.getPackageManager().queryIntentServicesAsUser(intent, 268435460, getChangingUserId());
                    return (installedServices == null || installedServices.isEmpty()) ? false : true;
                }

                private boolean hadPrintService(UserState userState, String packageName) throws Throwable {
                    List<PrintServiceInfo> installedServices = userState.getPrintServices(3);
                    if (installedServices == null) {
                        return false;
                    }
                    int numInstalledServices = installedServices.size();
                    for (int i = 0; i < numInstalledServices; i++) {
                        if (installedServices.get(i).getResolveInfo().serviceInfo.packageName.equals(packageName)) {
                            return true;
                        }
                    }
                    return false;
                }

                public void onPackageModified(String packageName) {
                    if (PrintManagerImpl.this.mUserManager.isUserUnlockingOrUnlocked(getChangingUserId())) {
                        UserState userState = PrintManagerImpl.this.getOrCreateUserStateLocked(getChangingUserId(), false);
                        synchronized (PrintManagerImpl.this.mLock) {
                            if (hadPrintService(userState, packageName) || hasPrintService(packageName)) {
                                userState.updateIfNeededLocked();
                            }
                        }
                        userState.prunePrintServices();
                    }
                }

                public void onPackageRemoved(String packageName, int uid) {
                    if (PrintManagerImpl.this.mUserManager.isUserUnlockingOrUnlocked(getChangingUserId())) {
                        UserState userState = PrintManagerImpl.this.getOrCreateUserStateLocked(getChangingUserId(), false);
                        synchronized (PrintManagerImpl.this.mLock) {
                            if (hadPrintService(userState, packageName)) {
                                userState.updateIfNeededLocked();
                            }
                        }
                        userState.prunePrintServices();
                    }
                }

                public boolean onHandleForceStop(Intent intent, String[] stoppedPackages, int uid, boolean doit) {
                    if (!PrintManagerImpl.this.mUserManager.isUserUnlockingOrUnlocked(getChangingUserId())) {
                        return false;
                    }
                    synchronized (PrintManagerImpl.this.mLock) {
                        UserState userState = PrintManagerImpl.this.getOrCreateUserStateLocked(getChangingUserId(), false);
                        boolean stoppedSomePackages = false;
                        List<PrintServiceInfo> enabledServices = userState.getPrintServices(1);
                        if (enabledServices == null) {
                            return false;
                        }
                        Iterator<PrintServiceInfo> iterator = enabledServices.iterator();
                        while (iterator.hasNext()) {
                            ComponentName componentName = iterator.next().getComponentName();
                            String componentPackage = componentName.getPackageName();
                            int i = 0;
                            int length = stoppedPackages.length;
                            while (true) {
                                if (i < length) {
                                    String stoppedPackage = stoppedPackages[i];
                                    if (!componentPackage.equals(stoppedPackage)) {
                                        i++;
                                    } else {
                                        if (!doit) {
                                            return true;
                                        }
                                        stoppedSomePackages = true;
                                    }
                                }
                            }
                        }
                        if (stoppedSomePackages) {
                            userState.updateIfNeededLocked();
                        }
                        return false;
                    }
                }

                public void onPackageAdded(String packageName, int uid) {
                    if (PrintManagerImpl.this.mUserManager.isUserUnlockingOrUnlocked(getChangingUserId())) {
                        synchronized (PrintManagerImpl.this.mLock) {
                            if (hasPrintService(packageName)) {
                                UserState userState = PrintManagerImpl.this.getOrCreateUserStateLocked(getChangingUserId(), false);
                                userState.updateIfNeededLocked();
                            }
                        }
                    }
                }
            };
            monitor.register(this.mContext, BackgroundThread.getHandler().getLooper(), UserHandle.ALL, true);
        }

        private UserState getOrCreateUserStateLocked(int userId, boolean lowPriority) {
            if (!this.mUserManager.isUserUnlockingOrUnlocked(userId)) {
                throw new IllegalStateException("User " + userId + " must be unlocked for printing to be available");
            }
            UserState userState = this.mUserStates.get(userId);
            if (userState == null) {
                userState = new UserState(this.mContext, userId, this.mLock, lowPriority);
                this.mUserStates.put(userId, userState);
            }
            if (!lowPriority) {
                userState.increasePriority();
            }
            return userState;
        }

        private void handleUserUnlocked(final int userId) {
            BackgroundThread.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    UserState userState;
                    if (PrintManagerImpl.this.mUserManager.isUserUnlockingOrUnlocked(userId)) {
                        synchronized (PrintManagerImpl.this.mLock) {
                            userState = PrintManagerImpl.this.getOrCreateUserStateLocked(userId, true);
                            userState.updateIfNeededLocked();
                        }
                        userState.removeObsoletePrintJobs();
                    }
                }
            });
        }

        private void handleUserStopped(final int userId) {
            BackgroundThread.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    synchronized (PrintManagerImpl.this.mLock) {
                        UserState userState = (UserState) PrintManagerImpl.this.mUserStates.get(userId);
                        if (userState != null) {
                            userState.destroyLocked();
                            PrintManagerImpl.this.mUserStates.remove(userId);
                        }
                    }
                }
            });
        }

        private int resolveCallingProfileParentLocked(int userId) {
            if (userId != getCurrentUserId()) {
                long identity = Binder.clearCallingIdentity();
                try {
                    UserInfo parent = this.mUserManager.getProfileParent(userId);
                    if (parent != null) {
                        return parent.getUserHandle().getIdentifier();
                    }
                    return BACKGROUND_USER_ID;
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
            return userId;
        }

        private int resolveCallingAppEnforcingPermissions(int appId) {
            int callingAppId;
            int callingUid = Binder.getCallingUid();
            if (callingUid != 0 && callingUid != 1000 && callingUid != 2000 && appId != (callingAppId = UserHandle.getAppId(callingUid)) && this.mContext.checkCallingPermission("com.android.printspooler.permission.ACCESS_ALL_PRINT_JOBS") != 0) {
                throw new SecurityException("Call from app " + callingAppId + " as app " + appId + " without com.android.printspooler.permission.ACCESS_ALL_PRINT_JOBS");
            }
            return appId;
        }

        private int resolveCallingUserEnforcingPermissions(int userId) {
            try {
                return ActivityManagerNative.getDefault().handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, true, "", (String) null);
            } catch (RemoteException e) {
                return userId;
            }
        }

        private String resolveCallingPackageNameEnforcingSecurity(String packageName) {
            String[] packages = this.mContext.getPackageManager().getPackagesForUid(Binder.getCallingUid());
            for (String str : packages) {
                if (packageName.equals(str)) {
                    return packageName;
                }
            }
            throw new IllegalArgumentException("packageName has to belong to the caller");
        }

        private int getCurrentUserId() {
            long identity = Binder.clearCallingIdentity();
            try {
                return ActivityManager.getCurrentUser();
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }
}
