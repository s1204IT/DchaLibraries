package com.android.server.print;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
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
import android.util.proto.ProtoOutputStream;
import android.widget.Toast;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.utils.PriorityDump;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
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
    public void onUnlockUser(int i) {
        this.mPrintManagerImpl.handleUserUnlocked(i);
    }

    @Override
    public void onStopUser(int i) {
        this.mPrintManagerImpl.handleUserStopped(i);
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

        public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
            new PrintShellCommand(this).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
        }

        public Bundle print(String str, IPrintDocumentAdapter iPrintDocumentAdapter, PrintAttributes printAttributes, String str2, int i, int i2) {
            long jClearCallingIdentity;
            IPrintDocumentAdapter iPrintDocumentAdapter2 = (IPrintDocumentAdapter) Preconditions.checkNotNull(iPrintDocumentAdapter);
            if (!isPrintingEnabled()) {
                DevicePolicyManagerInternal devicePolicyManagerInternal = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
                int callingUserId = UserHandle.getCallingUserId();
                jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    CharSequence printingDisabledReasonForUser = devicePolicyManagerInternal.getPrintingDisabledReasonForUser(callingUserId);
                    if (printingDisabledReasonForUser != null) {
                        Toast.makeText(this.mContext, Looper.getMainLooper(), printingDisabledReasonForUser, 1).show();
                    }
                    try {
                        iPrintDocumentAdapter2.start();
                    } catch (RemoteException e) {
                        Log.e(PrintManagerService.LOG_TAG, "Error calling IPrintDocumentAdapter.start()");
                    }
                    try {
                        iPrintDocumentAdapter2.finish();
                    } catch (RemoteException e2) {
                        Log.e(PrintManagerService.LOG_TAG, "Error calling IPrintDocumentAdapter.finish()");
                    }
                    return null;
                } finally {
                }
            }
            String str3 = (String) Preconditions.checkStringNotEmpty(str);
            String str4 = (String) Preconditions.checkStringNotEmpty(str2);
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i2);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return null;
                }
                int iResolveCallingAppEnforcingPermissions = resolveCallingAppEnforcingPermissions(i);
                String strResolveCallingPackageNameEnforcingSecurity = resolveCallingPackageNameEnforcingSecurity(str4);
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return orCreateUserStateLocked.print(str3, iPrintDocumentAdapter2, printAttributes, strResolveCallingPackageNameEnforcingSecurity, iResolveCallingAppEnforcingPermissions);
                } finally {
                }
            }
        }

        public List<PrintJobInfo> getPrintJobInfos(int i, int i2) {
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i2);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return null;
                }
                int iResolveCallingAppEnforcingPermissions = resolveCallingAppEnforcingPermissions(i);
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return orCreateUserStateLocked.getPrintJobInfos(iResolveCallingAppEnforcingPermissions);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public PrintJobInfo getPrintJobInfo(PrintJobId printJobId, int i, int i2) {
            if (printJobId == null) {
                return null;
            }
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i2);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return null;
                }
                int iResolveCallingAppEnforcingPermissions = resolveCallingAppEnforcingPermissions(i);
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return orCreateUserStateLocked.getPrintJobInfo(printJobId, iResolveCallingAppEnforcingPermissions);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public Icon getCustomPrinterIcon(PrinterId printerId, int i) {
            PrinterId printerId2 = (PrinterId) Preconditions.checkNotNull(printerId);
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return null;
                }
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return orCreateUserStateLocked.getCustomPrinterIcon(printerId2);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void cancelPrintJob(PrintJobId printJobId, int i, int i2) {
            if (printJobId == null) {
                return;
            }
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i2);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return;
                }
                int iResolveCallingAppEnforcingPermissions = resolveCallingAppEnforcingPermissions(i);
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    orCreateUserStateLocked.cancelPrintJob(printJobId, iResolveCallingAppEnforcingPermissions);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void restartPrintJob(PrintJobId printJobId, int i, int i2) {
            if (printJobId == null || !isPrintingEnabled()) {
                return;
            }
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i2);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return;
                }
                int iResolveCallingAppEnforcingPermissions = resolveCallingAppEnforcingPermissions(i);
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    orCreateUserStateLocked.restartPrintJob(printJobId, iResolveCallingAppEnforcingPermissions);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public List<PrintServiceInfo> getPrintServices(int i, int i2) {
            Preconditions.checkFlagsArgument(i, 3);
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRINT_SERVICES", null);
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i2);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return null;
                }
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return orCreateUserStateLocked.getPrintServices(i);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void setPrintServiceEnabled(ComponentName componentName, boolean z, int i) {
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i);
            int appId = UserHandle.getAppId(Binder.getCallingUid());
            if (appId != 1000) {
                try {
                    if (appId != UserHandle.getAppId(this.mContext.getPackageManager().getPackageUidAsUser("com.android.printspooler", iResolveCallingUserEnforcingPermissions))) {
                        throw new SecurityException("Only system and print spooler can call this");
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(PrintManagerService.LOG_TAG, "Could not verify caller", e);
                    return;
                }
            }
            ComponentName componentName2 = (ComponentName) Preconditions.checkNotNull(componentName);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return;
                }
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    orCreateUserStateLocked.setPrintServiceEnabled(componentName2, z);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public List<RecommendationInfo> getPrintServiceRecommendations(int i) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRINT_SERVICE_RECOMMENDATIONS", null);
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return null;
                }
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return orCreateUserStateLocked.getPrintServiceRecommendations();
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void createPrinterDiscoverySession(IPrinterDiscoveryObserver iPrinterDiscoveryObserver, int i) {
            IPrinterDiscoveryObserver iPrinterDiscoveryObserver2 = (IPrinterDiscoveryObserver) Preconditions.checkNotNull(iPrinterDiscoveryObserver);
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return;
                }
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    orCreateUserStateLocked.createPrinterDiscoverySession(iPrinterDiscoveryObserver2);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void destroyPrinterDiscoverySession(IPrinterDiscoveryObserver iPrinterDiscoveryObserver, int i) {
            IPrinterDiscoveryObserver iPrinterDiscoveryObserver2 = (IPrinterDiscoveryObserver) Preconditions.checkNotNull(iPrinterDiscoveryObserver);
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return;
                }
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    orCreateUserStateLocked.destroyPrinterDiscoverySession(iPrinterDiscoveryObserver2);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void startPrinterDiscovery(IPrinterDiscoveryObserver iPrinterDiscoveryObserver, List<PrinterId> list, int i) {
            IPrinterDiscoveryObserver iPrinterDiscoveryObserver2 = (IPrinterDiscoveryObserver) Preconditions.checkNotNull(iPrinterDiscoveryObserver);
            if (list != null) {
                list = (List) Preconditions.checkCollectionElementsNotNull(list, "PrinterId");
            }
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return;
                }
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    orCreateUserStateLocked.startPrinterDiscovery(iPrinterDiscoveryObserver2, list);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void stopPrinterDiscovery(IPrinterDiscoveryObserver iPrinterDiscoveryObserver, int i) {
            IPrinterDiscoveryObserver iPrinterDiscoveryObserver2 = (IPrinterDiscoveryObserver) Preconditions.checkNotNull(iPrinterDiscoveryObserver);
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return;
                }
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    orCreateUserStateLocked.stopPrinterDiscovery(iPrinterDiscoveryObserver2);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void validatePrinters(List<PrinterId> list, int i) {
            List<PrinterId> list2 = (List) Preconditions.checkCollectionElementsNotNull(list, "PrinterId");
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return;
                }
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    orCreateUserStateLocked.validatePrinters(list2);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void startPrinterStateTracking(PrinterId printerId, int i) {
            PrinterId printerId2 = (PrinterId) Preconditions.checkNotNull(printerId);
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return;
                }
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    orCreateUserStateLocked.startPrinterStateTracking(printerId2);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void stopPrinterStateTracking(PrinterId printerId, int i) {
            PrinterId printerId2 = (PrinterId) Preconditions.checkNotNull(printerId);
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return;
                }
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    orCreateUserStateLocked.stopPrinterStateTracking(printerId2);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void addPrintJobStateChangeListener(IPrintJobStateChangeListener iPrintJobStateChangeListener, int i, int i2) throws RemoteException {
            IPrintJobStateChangeListener iPrintJobStateChangeListener2 = (IPrintJobStateChangeListener) Preconditions.checkNotNull(iPrintJobStateChangeListener);
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i2);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return;
                }
                int iResolveCallingAppEnforcingPermissions = resolveCallingAppEnforcingPermissions(i);
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    orCreateUserStateLocked.addPrintJobStateChangeListener(iPrintJobStateChangeListener2, iResolveCallingAppEnforcingPermissions);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void removePrintJobStateChangeListener(IPrintJobStateChangeListener iPrintJobStateChangeListener, int i) {
            IPrintJobStateChangeListener iPrintJobStateChangeListener2 = (IPrintJobStateChangeListener) Preconditions.checkNotNull(iPrintJobStateChangeListener);
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return;
                }
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    orCreateUserStateLocked.removePrintJobStateChangeListener(iPrintJobStateChangeListener2);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void addPrintServicesChangeListener(IPrintServicesChangeListener iPrintServicesChangeListener, int i) throws RemoteException {
            IPrintServicesChangeListener iPrintServicesChangeListener2 = (IPrintServicesChangeListener) Preconditions.checkNotNull(iPrintServicesChangeListener);
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRINT_SERVICES", null);
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return;
                }
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    orCreateUserStateLocked.addPrintServicesChangeListener(iPrintServicesChangeListener2);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void removePrintServicesChangeListener(IPrintServicesChangeListener iPrintServicesChangeListener, int i) {
            IPrintServicesChangeListener iPrintServicesChangeListener2 = (IPrintServicesChangeListener) Preconditions.checkNotNull(iPrintServicesChangeListener);
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRINT_SERVICES", null);
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return;
                }
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    orCreateUserStateLocked.removePrintServicesChangeListener(iPrintServicesChangeListener2);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void addPrintServiceRecommendationsChangeListener(IRecommendationsChangeListener iRecommendationsChangeListener, int i) throws RemoteException {
            IRecommendationsChangeListener iRecommendationsChangeListener2 = (IRecommendationsChangeListener) Preconditions.checkNotNull(iRecommendationsChangeListener);
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRINT_SERVICE_RECOMMENDATIONS", null);
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return;
                }
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    orCreateUserStateLocked.addPrintServiceRecommendationsChangeListener(iRecommendationsChangeListener2);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void removePrintServiceRecommendationsChangeListener(IRecommendationsChangeListener iRecommendationsChangeListener, int i) {
            IRecommendationsChangeListener iRecommendationsChangeListener2 = (IRecommendationsChangeListener) Preconditions.checkNotNull(iRecommendationsChangeListener);
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRINT_SERVICE_RECOMMENDATIONS", null);
            int iResolveCallingUserEnforcingPermissions = resolveCallingUserEnforcingPermissions(i);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(iResolveCallingUserEnforcingPermissions) != getCurrentUserId()) {
                    return;
                }
                UserState orCreateUserStateLocked = getOrCreateUserStateLocked(iResolveCallingUserEnforcingPermissions, false);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    orCreateUserStateLocked.removePrintServiceRecommendationsChangeListener(iRecommendationsChangeListener2);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            String str;
            FileDescriptor fileDescriptor2 = (FileDescriptor) Preconditions.checkNotNull(fileDescriptor);
            if (DumpUtils.checkDumpPermission(this.mContext, PrintManagerService.LOG_TAG, printWriter)) {
                int i = 0;
                boolean z = false;
                while (i < strArr.length && (str = strArr[i]) != null && str.length() > 0 && str.charAt(0) == '-') {
                    i++;
                    if (PriorityDump.PROTO_ARG.equals(str)) {
                        z = true;
                    } else {
                        printWriter.println("Unknown argument: " + str + "; use -h for help");
                    }
                }
                ArrayList<UserState> arrayList = new ArrayList<>();
                synchronized (this.mLock) {
                    int size = this.mUserStates.size();
                    for (int i2 = 0; i2 < size; i2++) {
                        arrayList.add(this.mUserStates.valueAt(i2));
                    }
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    if (z) {
                        dump(new DualDumpOutputStream(new ProtoOutputStream(fileDescriptor2)), arrayList);
                    } else {
                        printWriter.println("PRINT MANAGER STATE (dumpsys print)");
                        dump(new DualDumpOutputStream(new IndentingPrintWriter(printWriter, "  ")), arrayList);
                    }
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public boolean getBindInstantServiceAllowed(int i) {
            UserState orCreateUserStateLocked;
            int callingUid = Binder.getCallingUid();
            if (callingUid != 2000 && callingUid != 0) {
                throw new SecurityException("Can only be called by uid 2000 or 0");
            }
            synchronized (this.mLock) {
                orCreateUserStateLocked = getOrCreateUserStateLocked(i, false);
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return orCreateUserStateLocked.getBindInstantServiceAllowed();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setBindInstantServiceAllowed(int i, boolean z) {
            UserState orCreateUserStateLocked;
            int callingUid = Binder.getCallingUid();
            if (callingUid != 2000 && callingUid != 0) {
                throw new SecurityException("Can only be called by uid 2000 or 0");
            }
            synchronized (this.mLock) {
                orCreateUserStateLocked = getOrCreateUserStateLocked(i, false);
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                orCreateUserStateLocked.setBindInstantServiceAllowed(z);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        private boolean isPrintingEnabled() {
            return !this.mUserManager.hasUserRestriction("no_printing", Binder.getCallingUserHandle());
        }

        private void dump(DualDumpOutputStream dualDumpOutputStream, ArrayList<UserState> arrayList) {
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                long jStart = dualDumpOutputStream.start("user_states", 2246267895809L);
                arrayList.get(i).dump(dualDumpOutputStream);
                dualDumpOutputStream.end(jStart);
            }
            dualDumpOutputStream.flush();
        }

        private void registerContentObservers() {
            final Uri uriFor = Settings.Secure.getUriFor("disabled_print_services");
            this.mContext.getContentResolver().registerContentObserver(uriFor, false, new ContentObserver(BackgroundThread.getHandler()) {
                @Override
                public void onChange(boolean z, Uri uri, int i) {
                    if (uriFor.equals(uri)) {
                        synchronized (PrintManagerImpl.this.mLock) {
                            int size = PrintManagerImpl.this.mUserStates.size();
                            for (int i2 = 0; i2 < size; i2++) {
                                if (i == -1 || i == PrintManagerImpl.this.mUserStates.keyAt(i2)) {
                                    ((UserState) PrintManagerImpl.this.mUserStates.valueAt(i2)).updateIfNeededLocked();
                                }
                            }
                        }
                    }
                }
            }, -1);
        }

        private void registerBroadcastReceivers() {
            new PackageMonitor() {
                private boolean hasPrintService(String str) {
                    Intent intent = new Intent("android.printservice.PrintService");
                    intent.setPackage(str);
                    List listQueryIntentServicesAsUser = PrintManagerImpl.this.mContext.getPackageManager().queryIntentServicesAsUser(intent, 276824068, getChangingUserId());
                    return (listQueryIntentServicesAsUser == null || listQueryIntentServicesAsUser.isEmpty()) ? false : true;
                }

                private boolean hadPrintService(UserState userState, String str) {
                    List<PrintServiceInfo> printServices = userState.getPrintServices(3);
                    if (printServices == null) {
                        return false;
                    }
                    int size = printServices.size();
                    for (int i = 0; i < size; i++) {
                        if (printServices.get(i).getResolveInfo().serviceInfo.packageName.equals(str)) {
                            return true;
                        }
                    }
                    return false;
                }

                public void onPackageModified(String str) {
                    if (PrintManagerImpl.this.mUserManager.isUserUnlockingOrUnlocked(getChangingUserId())) {
                        boolean z = false;
                        UserState orCreateUserStateLocked = PrintManagerImpl.this.getOrCreateUserStateLocked(getChangingUserId(), false, false);
                        synchronized (PrintManagerImpl.this.mLock) {
                            if (hadPrintService(orCreateUserStateLocked, str) || hasPrintService(str)) {
                                orCreateUserStateLocked.updateIfNeededLocked();
                                z = true;
                            }
                        }
                        if (z) {
                            orCreateUserStateLocked.prunePrintServices();
                        }
                    }
                }

                public void onPackageRemoved(String str, int i) {
                    if (PrintManagerImpl.this.mUserManager.isUserUnlockingOrUnlocked(getChangingUserId())) {
                        boolean z = false;
                        UserState orCreateUserStateLocked = PrintManagerImpl.this.getOrCreateUserStateLocked(getChangingUserId(), false, false);
                        synchronized (PrintManagerImpl.this.mLock) {
                            if (hadPrintService(orCreateUserStateLocked, str)) {
                                orCreateUserStateLocked.updateIfNeededLocked();
                                z = true;
                            }
                        }
                        if (z) {
                            orCreateUserStateLocked.prunePrintServices();
                        }
                    }
                }

                public boolean onHandleForceStop(Intent intent, String[] strArr, int i, boolean z) {
                    if (!PrintManagerImpl.this.mUserManager.isUserUnlockingOrUnlocked(getChangingUserId())) {
                        return false;
                    }
                    synchronized (PrintManagerImpl.this.mLock) {
                        UserState orCreateUserStateLocked = PrintManagerImpl.this.getOrCreateUserStateLocked(getChangingUserId(), false, false);
                        List<PrintServiceInfo> printServices = orCreateUserStateLocked.getPrintServices(1);
                        if (printServices == null) {
                            return false;
                        }
                        Iterator<PrintServiceInfo> it = printServices.iterator();
                        boolean z2 = false;
                        while (it.hasNext()) {
                            String packageName = it.next().getComponentName().getPackageName();
                            int length = strArr.length;
                            int i2 = 0;
                            while (true) {
                                if (i2 >= length) {
                                    break;
                                }
                                if (!packageName.equals(strArr[i2])) {
                                    i2++;
                                } else {
                                    if (!z) {
                                        return true;
                                    }
                                    z2 = true;
                                }
                            }
                        }
                        if (z2) {
                            orCreateUserStateLocked.updateIfNeededLocked();
                        }
                        return false;
                    }
                }

                public void onPackageAdded(String str, int i) {
                    if (PrintManagerImpl.this.mUserManager.isUserUnlockingOrUnlocked(getChangingUserId())) {
                        synchronized (PrintManagerImpl.this.mLock) {
                            if (hasPrintService(str)) {
                                PrintManagerImpl.this.getOrCreateUserStateLocked(getChangingUserId(), false, false).updateIfNeededLocked();
                            }
                        }
                    }
                }
            }.register(this.mContext, BackgroundThread.getHandler().getLooper(), UserHandle.ALL, true);
        }

        private UserState getOrCreateUserStateLocked(int i, boolean z) {
            return getOrCreateUserStateLocked(i, z, true);
        }

        private UserState getOrCreateUserStateLocked(int i, boolean z, boolean z2) {
            if (z2 && !this.mUserManager.isUserUnlockingOrUnlocked(i)) {
                throw new IllegalStateException("User " + i + " must be unlocked for printing to be available");
            }
            UserState userState = this.mUserStates.get(i);
            if (userState == null) {
                userState = new UserState(this.mContext, i, this.mLock, z);
                this.mUserStates.put(i, userState);
            }
            if (!z) {
                userState.increasePriority();
            }
            return userState;
        }

        private void handleUserUnlocked(final int i) {
            BackgroundThread.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    UserState orCreateUserStateLocked;
                    if (PrintManagerImpl.this.mUserManager.isUserUnlockingOrUnlocked(i)) {
                        synchronized (PrintManagerImpl.this.mLock) {
                            orCreateUserStateLocked = PrintManagerImpl.this.getOrCreateUserStateLocked(i, true, false);
                            orCreateUserStateLocked.updateIfNeededLocked();
                        }
                        orCreateUserStateLocked.removeObsoletePrintJobs();
                    }
                }
            });
        }

        private void handleUserStopped(final int i) {
            BackgroundThread.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    synchronized (PrintManagerImpl.this.mLock) {
                        UserState userState = (UserState) PrintManagerImpl.this.mUserStates.get(i);
                        if (userState != null) {
                            userState.destroyLocked();
                            PrintManagerImpl.this.mUserStates.remove(i);
                        }
                    }
                }
            });
        }

        private int resolveCallingProfileParentLocked(int i) {
            if (i != getCurrentUserId()) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    UserInfo profileParent = this.mUserManager.getProfileParent(i);
                    if (profileParent != null) {
                        return profileParent.getUserHandle().getIdentifier();
                    }
                    return -10;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            return i;
        }

        private int resolveCallingAppEnforcingPermissions(int i) {
            int appId;
            int callingUid = Binder.getCallingUid();
            if (callingUid != 0 && i != (appId = UserHandle.getAppId(callingUid)) && appId != 2000 && appId != 1000 && this.mContext.checkCallingPermission("com.android.printspooler.permission.ACCESS_ALL_PRINT_JOBS") != 0) {
                throw new SecurityException("Call from app " + appId + " as app " + i + " without com.android.printspooler.permission.ACCESS_ALL_PRINT_JOBS");
            }
            return i;
        }

        private int resolveCallingUserEnforcingPermissions(int i) {
            try {
                return ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i, true, true, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, (String) null);
            } catch (RemoteException e) {
                return i;
            }
        }

        private String resolveCallingPackageNameEnforcingSecurity(String str) {
            for (String str2 : this.mContext.getPackageManager().getPackagesForUid(Binder.getCallingUid())) {
                if (str.equals(str2)) {
                    return str;
                }
            }
            throw new IllegalArgumentException("packageName has to belong to the caller");
        }

        private int getCurrentUserId() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return ActivityManager.getCurrentUser();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }
}
