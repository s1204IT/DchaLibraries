package com.android.server.print;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.print.IPrintDocumentAdapter;
import android.print.IPrintJobStateChangeListener;
import android.print.IPrintManager;
import android.print.IPrinterDiscoveryObserver;
import android.print.PrintAttributes;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrinterId;
import android.printservice.PrintServiceInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.SparseArray;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.server.SystemService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class PrintManagerService extends SystemService {
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
    public void onStartUser(int userHandle) {
        this.mPrintManagerImpl.handleUserStarted(userHandle);
    }

    @Override
    public void onStopUser(int userHandle) {
        this.mPrintManagerImpl.handleUserStopped(userHandle);
    }

    class PrintManagerImpl extends IPrintManager.Stub {
        private static final int BACKGROUND_USER_ID = -10;
        private static final char COMPONENT_NAME_SEPARATOR = ':';
        private static final String EXTRA_PRINT_SERVICE_COMPONENT_NAME = "EXTRA_PRINT_SERVICE_COMPONENT_NAME";
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
            Bundle bundlePrint;
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    bundlePrint = null;
                } else {
                    int resolvedAppId = resolveCallingAppEnforcingPermissions(appId);
                    String resolvedPackageName = resolveCallingPackageNameEnforcingSecurity(packageName);
                    UserState userState = getOrCreateUserStateLocked(resolvedUserId);
                    long identity = Binder.clearCallingIdentity();
                    try {
                        bundlePrint = userState.print(printJobName, adapter, attributes, resolvedPackageName, resolvedAppId);
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            }
            return bundlePrint;
        }

        public List<PrintJobInfo> getPrintJobInfos(int appId, int userId) {
            List<PrintJobInfo> printJobInfos;
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    printJobInfos = null;
                } else {
                    int resolvedAppId = resolveCallingAppEnforcingPermissions(appId);
                    UserState userState = getOrCreateUserStateLocked(resolvedUserId);
                    long identity = Binder.clearCallingIdentity();
                    try {
                        printJobInfos = userState.getPrintJobInfos(resolvedAppId);
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            }
            return printJobInfos;
        }

        public PrintJobInfo getPrintJobInfo(PrintJobId printJobId, int appId, int userId) {
            PrintJobInfo printJobInfo;
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    printJobInfo = null;
                } else {
                    int resolvedAppId = resolveCallingAppEnforcingPermissions(appId);
                    UserState userState = getOrCreateUserStateLocked(resolvedUserId);
                    long identity = Binder.clearCallingIdentity();
                    try {
                        printJobInfo = userState.getPrintJobInfo(printJobId, resolvedAppId);
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            }
            return printJobInfo;
        }

        public void cancelPrintJob(PrintJobId printJobId, int appId, int userId) {
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) == getCurrentUserId()) {
                    int resolvedAppId = resolveCallingAppEnforcingPermissions(appId);
                    UserState userState = getOrCreateUserStateLocked(resolvedUserId);
                    long identity = Binder.clearCallingIdentity();
                    try {
                        userState.cancelPrintJob(printJobId, resolvedAppId);
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            }
        }

        public void restartPrintJob(PrintJobId printJobId, int appId, int userId) {
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) == getCurrentUserId()) {
                    int resolvedAppId = resolveCallingAppEnforcingPermissions(appId);
                    UserState userState = getOrCreateUserStateLocked(resolvedUserId);
                    long identity = Binder.clearCallingIdentity();
                    try {
                        userState.restartPrintJob(printJobId, resolvedAppId);
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            }
        }

        public List<PrintServiceInfo> getEnabledPrintServices(int userId) {
            List<PrintServiceInfo> enabledPrintServices;
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    enabledPrintServices = null;
                } else {
                    UserState userState = getOrCreateUserStateLocked(resolvedUserId);
                    long identity = Binder.clearCallingIdentity();
                    try {
                        enabledPrintServices = userState.getEnabledPrintServices();
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            }
            return enabledPrintServices;
        }

        public List<PrintServiceInfo> getInstalledPrintServices(int userId) {
            List<PrintServiceInfo> installedPrintServices;
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    installedPrintServices = null;
                } else {
                    UserState userState = getOrCreateUserStateLocked(resolvedUserId);
                    long identity = Binder.clearCallingIdentity();
                    try {
                        installedPrintServices = userState.getInstalledPrintServices();
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            }
            return installedPrintServices;
        }

        public void createPrinterDiscoverySession(IPrinterDiscoveryObserver observer, int userId) {
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) == getCurrentUserId()) {
                    UserState userState = getOrCreateUserStateLocked(resolvedUserId);
                    long identity = Binder.clearCallingIdentity();
                    try {
                        userState.createPrinterDiscoverySession(observer);
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            }
        }

        public void destroyPrinterDiscoverySession(IPrinterDiscoveryObserver observer, int userId) {
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) == getCurrentUserId()) {
                    UserState userState = getOrCreateUserStateLocked(resolvedUserId);
                    long identity = Binder.clearCallingIdentity();
                    try {
                        userState.destroyPrinterDiscoverySession(observer);
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            }
        }

        public void startPrinterDiscovery(IPrinterDiscoveryObserver observer, List<PrinterId> priorityList, int userId) {
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) == getCurrentUserId()) {
                    UserState userState = getOrCreateUserStateLocked(resolvedUserId);
                    long identity = Binder.clearCallingIdentity();
                    try {
                        userState.startPrinterDiscovery(observer, priorityList);
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            }
        }

        public void stopPrinterDiscovery(IPrinterDiscoveryObserver observer, int userId) {
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) == getCurrentUserId()) {
                    UserState userState = getOrCreateUserStateLocked(resolvedUserId);
                    long identity = Binder.clearCallingIdentity();
                    try {
                        userState.stopPrinterDiscovery(observer);
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            }
        }

        public void validatePrinters(List<PrinterId> printerIds, int userId) {
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) == getCurrentUserId()) {
                    UserState userState = getOrCreateUserStateLocked(resolvedUserId);
                    long identity = Binder.clearCallingIdentity();
                    try {
                        userState.validatePrinters(printerIds);
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            }
        }

        public void startPrinterStateTracking(PrinterId printerId, int userId) {
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) == getCurrentUserId()) {
                    UserState userState = getOrCreateUserStateLocked(resolvedUserId);
                    long identity = Binder.clearCallingIdentity();
                    try {
                        userState.startPrinterStateTracking(printerId);
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            }
        }

        public void stopPrinterStateTracking(PrinterId printerId, int userId) {
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) == getCurrentUserId()) {
                    UserState userState = getOrCreateUserStateLocked(resolvedUserId);
                    long identity = Binder.clearCallingIdentity();
                    try {
                        userState.stopPrinterStateTracking(printerId);
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            }
        }

        public void addPrintJobStateChangeListener(IPrintJobStateChangeListener listener, int appId, int userId) throws RemoteException {
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) == getCurrentUserId()) {
                    int resolvedAppId = resolveCallingAppEnforcingPermissions(appId);
                    UserState userState = getOrCreateUserStateLocked(resolvedUserId);
                    long identity = Binder.clearCallingIdentity();
                    try {
                        userState.addPrintJobStateChangeListener(listener, resolvedAppId);
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            }
        }

        public void removePrintJobStateChangeListener(IPrintJobStateChangeListener listener, int userId) {
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) == getCurrentUserId()) {
                    UserState userState = getOrCreateUserStateLocked(resolvedUserId);
                    long identity = Binder.clearCallingIdentity();
                    try {
                        userState.removePrintJobStateChangeListener(listener);
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                pw.println("Permission Denial: can't dump PrintManager from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
                return;
            }
            synchronized (this.mLock) {
                long identity = Binder.clearCallingIdentity();
                try {
                    pw.println("PRINT MANAGER STATE (dumpsys print)");
                    int userStateCount = this.mUserStates.size();
                    for (int i = 0; i < userStateCount; i++) {
                        UserState userState = this.mUserStates.valueAt(i);
                        userState.dump(fd, pw, "");
                        pw.println();
                    }
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        private void registerContentObservers() {
            final Uri enabledPrintServicesUri = Settings.Secure.getUriFor("enabled_print_services");
            ContentObserver observer = new ContentObserver(BackgroundThread.getHandler()) {
                @Override
                public void onChange(boolean selfChange, Uri uri, int userId) {
                    if (enabledPrintServicesUri.equals(uri)) {
                        synchronized (PrintManagerImpl.this.mLock) {
                            if (userId != -1) {
                                UserState userState = PrintManagerImpl.this.getOrCreateUserStateLocked(userId);
                                userState.updateIfNeededLocked();
                            } else {
                                int userCount = PrintManagerImpl.this.mUserStates.size();
                                for (int i = 0; i < userCount; i++) {
                                    UserState userState2 = (UserState) PrintManagerImpl.this.mUserStates.valueAt(i);
                                    userState2.updateIfNeededLocked();
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
                public void onPackageModified(String packageName) {
                    synchronized (PrintManagerImpl.this.mLock) {
                        boolean servicesChanged = false;
                        UserState userState = PrintManagerImpl.this.getOrCreateUserStateLocked(getChangingUserId());
                        for (ComponentName componentName : userState.getEnabledServices()) {
                            if (packageName.equals(componentName.getPackageName())) {
                                servicesChanged = true;
                            }
                        }
                        if (servicesChanged) {
                            userState.updateIfNeededLocked();
                        }
                    }
                }

                public void onPackageRemoved(String packageName, int uid) {
                    synchronized (PrintManagerImpl.this.mLock) {
                        boolean servicesRemoved = false;
                        UserState userState = PrintManagerImpl.this.getOrCreateUserStateLocked(getChangingUserId());
                        Iterator<ComponentName> iterator = userState.getEnabledServices().iterator();
                        while (iterator.hasNext()) {
                            ComponentName componentName = iterator.next();
                            if (packageName.equals(componentName.getPackageName())) {
                                iterator.remove();
                                servicesRemoved = true;
                            }
                        }
                        if (servicesRemoved) {
                            persistComponentNamesToSettingLocked("enabled_print_services", userState.getEnabledServices(), getChangingUserId());
                            userState.updateIfNeededLocked();
                        }
                    }
                }

                public boolean onHandleForceStop(Intent intent, String[] stoppedPackages, int uid, boolean doit) {
                    boolean z;
                    synchronized (PrintManagerImpl.this.mLock) {
                        UserState userState = PrintManagerImpl.this.getOrCreateUserStateLocked(getChangingUserId());
                        boolean stoppedSomePackages = false;
                        Iterator<ComponentName> iterator = userState.getEnabledServices().iterator();
                        while (true) {
                            if (iterator.hasNext()) {
                                ComponentName componentName = iterator.next();
                                String componentPackage = componentName.getPackageName();
                                int len$ = stoppedPackages.length;
                                int i$ = 0;
                                while (true) {
                                    if (i$ < len$) {
                                        String stoppedPackage = stoppedPackages[i$];
                                        if (!componentPackage.equals(stoppedPackage)) {
                                            i$++;
                                        } else {
                                            if (!doit) {
                                                z = true;
                                                break;
                                            }
                                            stoppedSomePackages = true;
                                        }
                                    }
                                }
                            } else {
                                if (stoppedSomePackages) {
                                    userState.updateIfNeededLocked();
                                }
                                z = false;
                            }
                        }
                    }
                    return z;
                }

                public void onPackageAdded(String packageName, int uid) {
                    Intent intent = new Intent("android.printservice.PrintService");
                    intent.setPackage(packageName);
                    List<ResolveInfo> installedServices = PrintManagerImpl.this.mContext.getPackageManager().queryIntentServicesAsUser(intent, 4, getChangingUserId());
                    if (installedServices != null) {
                        int installedServiceCount = installedServices.size();
                        for (int i = 0; i < installedServiceCount; i++) {
                            ServiceInfo serviceInfo = installedServices.get(i).serviceInfo;
                            ComponentName component = new ComponentName(serviceInfo.packageName, serviceInfo.name);
                            String label = serviceInfo.loadLabel(PrintManagerImpl.this.mContext.getPackageManager()).toString();
                            PrintManagerImpl.this.showEnableInstalledPrintServiceNotification(component, label, getChangingUserId());
                        }
                    }
                }

                private void persistComponentNamesToSettingLocked(String settingName, Set<ComponentName> componentNames, int userId) {
                    StringBuilder builder = new StringBuilder();
                    for (ComponentName componentName : componentNames) {
                        if (builder.length() > 0) {
                            builder.append(PrintManagerImpl.COMPONENT_NAME_SEPARATOR);
                        }
                        builder.append(componentName.flattenToShortString());
                    }
                    Settings.Secure.putStringForUser(PrintManagerImpl.this.mContext.getContentResolver(), settingName, builder.toString(), userId);
                }
            };
            monitor.register(this.mContext, BackgroundThread.getHandler().getLooper(), UserHandle.ALL, true);
        }

        private UserState getOrCreateUserStateLocked(int userId) {
            UserState userState = this.mUserStates.get(userId);
            if (userState == null) {
                UserState userState2 = new UserState(this.mContext, userId, this.mLock);
                this.mUserStates.put(userId, userState2);
                return userState2;
            }
            return userState;
        }

        private void handleUserStarted(final int userId) {
            BackgroundThread.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    UserState userState;
                    synchronized (PrintManagerImpl.this.mLock) {
                        userState = PrintManagerImpl.this.getOrCreateUserStateLocked(userId);
                        userState.updateIfNeededLocked();
                    }
                    userState.removeObsoletePrintJobs();
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
            if (TextUtils.isEmpty(packageName)) {
                return null;
            }
            String[] packages = this.mContext.getPackageManager().getPackagesForUid(Binder.getCallingUid());
            for (String str : packages) {
                if (packageName.equals(str)) {
                    return packageName;
                }
            }
            return null;
        }

        private int getCurrentUserId() {
            long identity = Binder.clearCallingIdentity();
            try {
                return ActivityManager.getCurrentUser();
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        private void showEnableInstalledPrintServiceNotification(ComponentName component, String label, int userId) {
            UserHandle userHandle = new UserHandle(userId);
            Intent intent = new Intent("android.settings.ACTION_PRINT_SETTINGS");
            intent.putExtra(EXTRA_PRINT_SERVICE_COMPONENT_NAME, component.flattenToString());
            PendingIntent pendingIntent = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 1342177280, null, userHandle);
            Context builderContext = this.mContext;
            try {
                builderContext = this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, userHandle);
            } catch (PackageManager.NameNotFoundException e) {
            }
            Notification.Builder builder = new Notification.Builder(builderContext).setSmallIcon(R.drawable.ic_drag_handle).setContentTitle(this.mContext.getString(R.string.miniresolver_use_work_browser, label)).setContentText(this.mContext.getString(R.string.minute)).setContentIntent(pendingIntent).setWhen(System.currentTimeMillis()).setAutoCancel(true).setShowWhen(true).setColor(this.mContext.getResources().getColor(R.color.system_accent3_600));
            NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
            String notificationTag = getClass().getName() + ":" + component.flattenToString();
            notificationManager.notifyAsUser(notificationTag, 0, builder.build(), userHandle);
        }
    }
}
