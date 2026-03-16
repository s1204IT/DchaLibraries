package com.android.server.print;

import android.R;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.print.IPrintDocumentAdapter;
import android.print.IPrintJobStateChangeListener;
import android.print.IPrinterDiscoveryObserver;
import android.print.PrintAttributes;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.PrintServiceInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.SomeArgs;
import com.android.server.print.RemotePrintService;
import com.android.server.print.RemotePrintSpooler;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class UserState implements RemotePrintSpooler.PrintSpoolerCallbacks, RemotePrintService.PrintServiceCallbacks {
    private static final char COMPONENT_NAME_SEPARATOR = ':';
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "UserState";
    private final Context mContext;
    private boolean mDestroyed;
    private final Handler mHandler;
    private final Object mLock;
    private List<PrintJobStateChangeListenerRecord> mPrintJobStateChangeListenerRecords;
    private PrinterDiscoverySessionMediator mPrinterDiscoverySession;
    private final RemotePrintSpooler mSpooler;
    private final int mUserId;
    private final TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(COMPONENT_NAME_SEPARATOR);
    private final Intent mQueryIntent = new Intent("android.printservice.PrintService");
    private final ArrayMap<ComponentName, RemotePrintService> mActiveServices = new ArrayMap<>();
    private final List<PrintServiceInfo> mInstalledServices = new ArrayList();
    private final Set<ComponentName> mEnabledServices = new ArraySet();
    private final PrintJobForAppCache mPrintJobForAppCache = new PrintJobForAppCache();

    public UserState(Context context, int userId, Object lock) {
        this.mContext = context;
        this.mUserId = userId;
        this.mLock = lock;
        this.mSpooler = new RemotePrintSpooler(context, userId, this);
        this.mHandler = new UserStateHandler(context.getMainLooper());
        synchronized (this.mLock) {
            enableSystemPrintServicesLocked();
            onConfigurationChangedLocked();
        }
    }

    @Override
    public void onPrintJobQueued(PrintJobInfo printJob) {
        RemotePrintService service;
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            ComponentName printServiceName = printJob.getPrinterId().getServiceName();
            service = this.mActiveServices.get(printServiceName);
        }
        if (service != null) {
            service.onPrintJobQueued(printJob);
        } else {
            this.mSpooler.setPrintJobState(printJob.getId(), 6, this.mContext.getString(R.string.miniresolver_use_personal_browser));
        }
    }

    @Override
    public void onAllPrintJobsForServiceHandled(ComponentName printService) {
        RemotePrintService service;
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            service = this.mActiveServices.get(printService);
        }
        if (service != null) {
            service.onAllPrintJobsHandled();
        }
    }

    public void removeObsoletePrintJobs() {
        this.mSpooler.removeObsoletePrintJobs();
    }

    public Bundle print(String printJobName, IPrintDocumentAdapter adapter, PrintAttributes attributes, String packageName, int appId) {
        final PrintJobInfo printJob = new PrintJobInfo();
        printJob.setId(new PrintJobId());
        printJob.setAppId(appId);
        printJob.setLabel(printJobName);
        printJob.setAttributes(attributes);
        printJob.setState(1);
        printJob.setCopies(1);
        printJob.setCreationTime(System.currentTimeMillis());
        if (!this.mPrintJobForAppCache.onPrintJobCreated(adapter.asBinder(), appId, printJob)) {
            return null;
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                UserState.this.mSpooler.createPrintJob(printJob);
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
        long identity = Binder.clearCallingIdentity();
        try {
            Intent intent = new Intent("android.print.PRINT_DIALOG");
            intent.setData(Uri.fromParts("printjob", printJob.getId().flattenToString(), null));
            intent.putExtra("android.print.intent.extra.EXTRA_PRINT_DOCUMENT_ADAPTER", adapter.asBinder());
            intent.putExtra("android.print.intent.extra.EXTRA_PRINT_JOB", printJob);
            intent.putExtra("android.content.extra.PACKAGE_NAME", packageName);
            IntentSender intentSender = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 1342177280, null, new UserHandle(this.mUserId)).getIntentSender();
            Bundle result = new Bundle();
            result.putParcelable("android.print.intent.extra.EXTRA_PRINT_JOB", printJob);
            result.putParcelable("android.print.intent.extra.EXTRA_PRINT_DIALOG_INTENT", intentSender);
            return result;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public List<PrintJobInfo> getPrintJobInfos(int appId) throws Throwable {
        List<PrintJobInfo> cachedPrintJobs = this.mPrintJobForAppCache.getPrintJobs(appId);
        ArrayMap<PrintJobId, PrintJobInfo> result = new ArrayMap<>();
        int cachedPrintJobCount = cachedPrintJobs.size();
        for (int i = 0; i < cachedPrintJobCount; i++) {
            PrintJobInfo cachedPrintJob = cachedPrintJobs.get(i);
            result.put(cachedPrintJob.getId(), cachedPrintJob);
            cachedPrintJob.setTag(null);
            cachedPrintJob.setAdvancedOptions(null);
        }
        List<PrintJobInfo> printJobs = this.mSpooler.getPrintJobInfos(null, -1, appId);
        if (printJobs != null) {
            int printJobCount = printJobs.size();
            for (int i2 = 0; i2 < printJobCount; i2++) {
                PrintJobInfo printJob = printJobs.get(i2);
                result.put(printJob.getId(), printJob);
                printJob.setTag(null);
                printJob.setAdvancedOptions(null);
            }
        }
        return new ArrayList(result.values());
    }

    public PrintJobInfo getPrintJobInfo(PrintJobId printJobId, int appId) {
        PrintJobInfo printJob = this.mPrintJobForAppCache.getPrintJob(printJobId, appId);
        if (printJob == null) {
            printJob = this.mSpooler.getPrintJobInfo(printJobId, appId);
        }
        if (printJob != null) {
            printJob.setTag(null);
            printJob.setAdvancedOptions(null);
        }
        return printJob;
    }

    public void cancelPrintJob(PrintJobId printJobId, int appId) {
        RemotePrintService printService;
        PrintJobInfo printJobInfo = this.mSpooler.getPrintJobInfo(printJobId, appId);
        if (printJobInfo != null) {
            this.mSpooler.setPrintJobCancelling(printJobId, true);
            if (printJobInfo.getState() != 6) {
                ComponentName printServiceName = printJobInfo.getPrinterId().getServiceName();
                synchronized (this.mLock) {
                    printService = this.mActiveServices.get(printServiceName);
                }
                if (printService != null) {
                    printService.onRequestCancelPrintJob(printJobInfo);
                    return;
                }
                return;
            }
            this.mSpooler.setPrintJobState(printJobId, 7, null);
        }
    }

    public void restartPrintJob(PrintJobId printJobId, int appId) {
        PrintJobInfo printJobInfo = getPrintJobInfo(printJobId, appId);
        if (printJobInfo != null && printJobInfo.getState() == 6) {
            this.mSpooler.setPrintJobState(printJobId, 2, null);
        }
    }

    public List<PrintServiceInfo> getEnabledPrintServices() throws Throwable {
        List<PrintServiceInfo> enabledServices;
        synchronized (this.mLock) {
            try {
                int installedServiceCount = this.mInstalledServices.size();
                int i = 0;
                List<PrintServiceInfo> enabledServices2 = null;
                while (i < installedServiceCount) {
                    try {
                        PrintServiceInfo installedService = this.mInstalledServices.get(i);
                        ComponentName componentName = new ComponentName(installedService.getResolveInfo().serviceInfo.packageName, installedService.getResolveInfo().serviceInfo.name);
                        if (this.mActiveServices.containsKey(componentName)) {
                            enabledServices = enabledServices2 == null ? new ArrayList<>() : enabledServices2;
                            enabledServices.add(installedService);
                        } else {
                            enabledServices = enabledServices2;
                        }
                        i++;
                        enabledServices2 = enabledServices;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                return enabledServices2;
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public List<PrintServiceInfo> getInstalledPrintServices() {
        List<PrintServiceInfo> list;
        synchronized (this.mLock) {
            list = this.mInstalledServices;
        }
        return list;
    }

    public void createPrinterDiscoverySession(IPrinterDiscoveryObserver observer) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (!this.mActiveServices.isEmpty()) {
                if (this.mPrinterDiscoverySession == null) {
                    this.mPrinterDiscoverySession = new PrinterDiscoverySessionMediator(this.mContext) {
                        @Override
                        public void onDestroyed() {
                            UserState.this.mPrinterDiscoverySession = null;
                        }
                    };
                    this.mPrinterDiscoverySession.addObserverLocked(observer);
                } else {
                    this.mPrinterDiscoverySession.addObserverLocked(observer);
                }
            }
        }
    }

    public void destroyPrinterDiscoverySession(IPrinterDiscoveryObserver observer) {
        synchronized (this.mLock) {
            if (this.mPrinterDiscoverySession != null) {
                this.mPrinterDiscoverySession.removeObserverLocked(observer);
            }
        }
    }

    public void startPrinterDiscovery(IPrinterDiscoveryObserver observer, List<PrinterId> printerIds) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (!this.mActiveServices.isEmpty()) {
                if (this.mPrinterDiscoverySession != null) {
                    this.mPrinterDiscoverySession.startPrinterDiscoveryLocked(observer, printerIds);
                }
            }
        }
    }

    public void stopPrinterDiscovery(IPrinterDiscoveryObserver observer) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (!this.mActiveServices.isEmpty()) {
                if (this.mPrinterDiscoverySession != null) {
                    this.mPrinterDiscoverySession.stopPrinterDiscoveryLocked(observer);
                }
            }
        }
    }

    public void validatePrinters(List<PrinterId> printerIds) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (!this.mActiveServices.isEmpty()) {
                if (this.mPrinterDiscoverySession != null) {
                    this.mPrinterDiscoverySession.validatePrintersLocked(printerIds);
                }
            }
        }
    }

    public void startPrinterStateTracking(PrinterId printerId) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (!this.mActiveServices.isEmpty()) {
                if (this.mPrinterDiscoverySession != null) {
                    this.mPrinterDiscoverySession.startPrinterStateTrackingLocked(printerId);
                }
            }
        }
    }

    public void stopPrinterStateTracking(PrinterId printerId) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (!this.mActiveServices.isEmpty()) {
                if (this.mPrinterDiscoverySession != null) {
                    this.mPrinterDiscoverySession.stopPrinterStateTrackingLocked(printerId);
                }
            }
        }
    }

    public void addPrintJobStateChangeListener(IPrintJobStateChangeListener listener, int appId) throws RemoteException {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrintJobStateChangeListenerRecords == null) {
                this.mPrintJobStateChangeListenerRecords = new ArrayList();
            }
            this.mPrintJobStateChangeListenerRecords.add(new PrintJobStateChangeListenerRecord(listener, appId) {
                @Override
                public void onBinderDied() {
                    UserState.this.mPrintJobStateChangeListenerRecords.remove(this);
                }
            });
        }
    }

    public void removePrintJobStateChangeListener(IPrintJobStateChangeListener listener) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrintJobStateChangeListenerRecords != null) {
                int recordCount = this.mPrintJobStateChangeListenerRecords.size();
                int i = 0;
                while (true) {
                    if (i >= recordCount) {
                        break;
                    }
                    PrintJobStateChangeListenerRecord record = this.mPrintJobStateChangeListenerRecords.get(i);
                    if (!record.listener.asBinder().equals(listener.asBinder())) {
                        i++;
                    } else {
                        this.mPrintJobStateChangeListenerRecords.remove(i);
                        break;
                    }
                }
                if (this.mPrintJobStateChangeListenerRecords.isEmpty()) {
                    this.mPrintJobStateChangeListenerRecords = null;
                }
            }
        }
    }

    @Override
    public void onPrintJobStateChanged(PrintJobInfo printJob) {
        this.mPrintJobForAppCache.onPrintJobStateChanged(printJob);
        this.mHandler.obtainMessage(1, printJob.getAppId(), 0, printJob.getId()).sendToTarget();
    }

    @Override
    public void onPrintersAdded(List<PrinterInfo> printers) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (!this.mActiveServices.isEmpty()) {
                if (this.mPrinterDiscoverySession != null) {
                    this.mPrinterDiscoverySession.onPrintersAddedLocked(printers);
                }
            }
        }
    }

    @Override
    public void onPrintersRemoved(List<PrinterId> printerIds) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (!this.mActiveServices.isEmpty()) {
                if (this.mPrinterDiscoverySession != null) {
                    this.mPrinterDiscoverySession.onPrintersRemovedLocked(printerIds);
                }
            }
        }
    }

    @Override
    public void onServiceDied(RemotePrintService service) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (!this.mActiveServices.isEmpty()) {
                failActivePrintJobsForService(service.getComponentName());
                service.onAllPrintJobsHandled();
                if (this.mPrinterDiscoverySession != null) {
                    this.mPrinterDiscoverySession.onServiceDiedLocked(service);
                }
            }
        }
    }

    public void updateIfNeededLocked() {
        throwIfDestroyedLocked();
        if (readConfigurationLocked()) {
            onConfigurationChangedLocked();
        }
    }

    public Set<ComponentName> getEnabledServices() {
        Set<ComponentName> set;
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            set = this.mEnabledServices;
        }
        return set;
    }

    public void destroyLocked() {
        throwIfDestroyedLocked();
        this.mSpooler.destroy();
        for (RemotePrintService service : this.mActiveServices.values()) {
            service.destroy();
        }
        this.mActiveServices.clear();
        this.mInstalledServices.clear();
        this.mEnabledServices.clear();
        if (this.mPrinterDiscoverySession != null) {
            this.mPrinterDiscoverySession.destroyLocked();
            this.mPrinterDiscoverySession = null;
        }
        this.mDestroyed = true;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String prefix) {
        pw.append((CharSequence) prefix).append("user state ").append((CharSequence) String.valueOf(this.mUserId)).append(":");
        pw.println();
        pw.append((CharSequence) prefix).append("  ").append("installed services:").println();
        int installedServiceCount = this.mInstalledServices.size();
        for (int i = 0; i < installedServiceCount; i++) {
            PrintServiceInfo installedService = this.mInstalledServices.get(i);
            String installedServicePrefix = prefix + "    ";
            pw.append((CharSequence) installedServicePrefix).append("service:").println();
            ResolveInfo resolveInfo = installedService.getResolveInfo();
            ComponentName componentName = new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
            pw.append((CharSequence) installedServicePrefix).append("  ").append("componentName=").append((CharSequence) componentName.flattenToString()).println();
            pw.append((CharSequence) installedServicePrefix).append("  ").append("settingsActivity=").append((CharSequence) installedService.getSettingsActivityName()).println();
            pw.append((CharSequence) installedServicePrefix).append("  ").append("addPrintersActivity=").append((CharSequence) installedService.getAddPrintersActivityName()).println();
            pw.append((CharSequence) installedServicePrefix).append("  ").append("avancedOptionsActivity=").append((CharSequence) installedService.getAdvancedOptionsActivityName()).println();
        }
        pw.append((CharSequence) prefix).append("  ").append("enabled services:").println();
        for (ComponentName enabledService : this.mEnabledServices) {
            String enabledServicePrefix = prefix + "    ";
            pw.append((CharSequence) enabledServicePrefix).append("service:").println();
            pw.append((CharSequence) enabledServicePrefix).append("  ").append("componentName=").append((CharSequence) enabledService.flattenToString());
            pw.println();
        }
        pw.append((CharSequence) prefix).append("  ").append("active services:").println();
        int activeServiceCount = this.mActiveServices.size();
        for (int i2 = 0; i2 < activeServiceCount; i2++) {
            RemotePrintService activeService = this.mActiveServices.valueAt(i2);
            activeService.dump(pw, prefix + "    ");
            pw.println();
        }
        pw.append((CharSequence) prefix).append("  ").append("cached print jobs:").println();
        this.mPrintJobForAppCache.dump(pw, prefix + "    ");
        pw.append((CharSequence) prefix).append("  ").append("discovery mediator:").println();
        if (this.mPrinterDiscoverySession != null) {
            this.mPrinterDiscoverySession.dump(pw, prefix + "    ");
        }
        pw.append((CharSequence) prefix).append("  ").append("print spooler:").println();
        this.mSpooler.dump(fd, pw, prefix + "    ");
        pw.println();
    }

    private boolean readConfigurationLocked() {
        boolean somethingChanged = false | readInstalledPrintServicesLocked();
        return somethingChanged | readEnabledPrintServicesLocked();
    }

    private boolean readInstalledPrintServicesLocked() {
        PrintServiceInfo newService;
        PrintServiceInfo oldService;
        Set<PrintServiceInfo> tempPrintServices = new HashSet<>();
        List<ResolveInfo> installedServices = this.mContext.getPackageManager().queryIntentServicesAsUser(this.mQueryIntent, 132, this.mUserId);
        int installedCount = installedServices.size();
        for (int i = 0; i < installedCount; i++) {
            ResolveInfo installedService = installedServices.get(i);
            if (!"android.permission.BIND_PRINT_SERVICE".equals(installedService.serviceInfo.permission)) {
                ComponentName serviceName = new ComponentName(installedService.serviceInfo.packageName, installedService.serviceInfo.name);
                Slog.w(LOG_TAG, "Skipping print service " + serviceName.flattenToShortString() + " since it does not require permission android.permission.BIND_PRINT_SERVICE");
            } else {
                tempPrintServices.add(PrintServiceInfo.create(installedService, this.mContext));
            }
        }
        boolean someServiceChanged = DEBUG;
        if (tempPrintServices.size() != this.mInstalledServices.size()) {
            someServiceChanged = true;
        } else {
            Iterator<PrintServiceInfo> it = tempPrintServices.iterator();
            do {
                if (!it.hasNext()) {
                    break;
                }
                newService = it.next();
                int oldServiceIndex = this.mInstalledServices.indexOf(newService);
                if (oldServiceIndex < 0) {
                    someServiceChanged = true;
                    break;
                }
                oldService = this.mInstalledServices.get(oldServiceIndex);
                if (!TextUtils.equals(oldService.getAddPrintersActivityName(), newService.getAddPrintersActivityName()) || !TextUtils.equals(oldService.getAdvancedOptionsActivityName(), newService.getAdvancedOptionsActivityName())) {
                    break;
                }
            } while (TextUtils.equals(oldService.getSettingsActivityName(), newService.getSettingsActivityName()));
            someServiceChanged = true;
        }
        if (someServiceChanged) {
            this.mInstalledServices.clear();
            this.mInstalledServices.addAll(tempPrintServices);
            return true;
        }
        return DEBUG;
    }

    private boolean readEnabledPrintServicesLocked() {
        Set<ComponentName> tempEnabledServiceNameSet = new HashSet<>();
        readPrintServicesFromSettingLocked("enabled_print_services", tempEnabledServiceNameSet);
        if (tempEnabledServiceNameSet.equals(this.mEnabledServices)) {
            return DEBUG;
        }
        this.mEnabledServices.clear();
        this.mEnabledServices.addAll(tempEnabledServiceNameSet);
        return true;
    }

    private void readPrintServicesFromSettingLocked(String setting, Set<ComponentName> outServiceNames) {
        ComponentName componentName;
        String settingValue = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), setting, this.mUserId);
        if (!TextUtils.isEmpty(settingValue)) {
            TextUtils.SimpleStringSplitter splitter = this.mStringColonSplitter;
            splitter.setString(settingValue);
            while (splitter.hasNext()) {
                String string = splitter.next();
                if (!TextUtils.isEmpty(string) && (componentName = ComponentName.unflattenFromString(string)) != null) {
                    outServiceNames.add(componentName);
                }
            }
        }
    }

    private void enableSystemPrintServicesLocked() {
        String enabledServices;
        String enabledOnFirstBootServices;
        readEnabledPrintServicesLocked();
        readInstalledPrintServicesLocked();
        Set<ComponentName> enabledOnFirstBoot = new HashSet<>();
        readPrintServicesFromSettingLocked("enabled_on_first_boot_system_print_services", enabledOnFirstBoot);
        StringBuilder builder = new StringBuilder();
        int serviceCount = this.mInstalledServices.size();
        for (int i = 0; i < serviceCount; i++) {
            ServiceInfo serviceInfo = this.mInstalledServices.get(i).getResolveInfo().serviceInfo;
            if ((serviceInfo.applicationInfo.flags & 1) != 0) {
                ComponentName serviceName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
                if (!this.mEnabledServices.contains(serviceName) && !enabledOnFirstBoot.contains(serviceName)) {
                    if (builder.length() > 0) {
                        builder.append(":");
                    }
                    builder.append(serviceName.flattenToString());
                }
            }
        }
        if (builder.length() > 0) {
            String servicesToEnable = builder.toString();
            String enabledServices2 = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "enabled_print_services", this.mUserId);
            if (TextUtils.isEmpty(enabledServices2)) {
                enabledServices = servicesToEnable;
            } else {
                enabledServices = enabledServices2 + ":" + servicesToEnable;
            }
            Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "enabled_print_services", enabledServices, this.mUserId);
            String enabledOnFirstBootServices2 = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "enabled_on_first_boot_system_print_services", this.mUserId);
            if (TextUtils.isEmpty(enabledOnFirstBootServices2)) {
                enabledOnFirstBootServices = servicesToEnable;
            } else {
                enabledOnFirstBootServices = enabledOnFirstBootServices2 + ":" + enabledServices;
            }
            Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "enabled_on_first_boot_system_print_services", enabledOnFirstBootServices, this.mUserId);
        }
    }

    private void onConfigurationChangedLocked() {
        Set<ComponentName> installedComponents = new ArraySet<>();
        int installedCount = this.mInstalledServices.size();
        for (int i = 0; i < installedCount; i++) {
            ResolveInfo resolveInfo = this.mInstalledServices.get(i).getResolveInfo();
            ComponentName serviceName = new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
            installedComponents.add(serviceName);
            if (this.mEnabledServices.contains(serviceName)) {
                if (!this.mActiveServices.containsKey(serviceName)) {
                    RemotePrintService service = new RemotePrintService(this.mContext, serviceName, this.mUserId, this.mSpooler, this);
                    addServiceLocked(service);
                }
            } else {
                RemotePrintService service2 = this.mActiveServices.remove(serviceName);
                if (service2 != null) {
                    removeServiceLocked(service2);
                }
            }
        }
        Iterator<Map.Entry<ComponentName, RemotePrintService>> iterator = this.mActiveServices.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ComponentName, RemotePrintService> entry = iterator.next();
            ComponentName serviceName2 = entry.getKey();
            RemotePrintService service3 = entry.getValue();
            if (!installedComponents.contains(serviceName2)) {
                removeServiceLocked(service3);
                iterator.remove();
            }
        }
    }

    private void addServiceLocked(RemotePrintService service) {
        this.mActiveServices.put(service.getComponentName(), service);
        if (this.mPrinterDiscoverySession != null) {
            this.mPrinterDiscoverySession.onServiceAddedLocked(service);
        }
    }

    private void removeServiceLocked(RemotePrintService service) {
        failActivePrintJobsForService(service.getComponentName());
        if (this.mPrinterDiscoverySession != null) {
            this.mPrinterDiscoverySession.onServiceRemovedLocked(service);
        } else {
            service.destroy();
        }
    }

    private void failActivePrintJobsForService(final ComponentName serviceName) {
        if (Looper.getMainLooper().isCurrentThread()) {
            BackgroundThread.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    UserState.this.failScheduledPrintJobsForServiceInternal(serviceName);
                }
            });
        } else {
            failScheduledPrintJobsForServiceInternal(serviceName);
        }
    }

    private void failScheduledPrintJobsForServiceInternal(ComponentName serviceName) {
        List<PrintJobInfo> printJobs = this.mSpooler.getPrintJobInfos(serviceName, -4, -2);
        if (printJobs != null) {
            long identity = Binder.clearCallingIdentity();
            try {
                int printJobCount = printJobs.size();
                for (int i = 0; i < printJobCount; i++) {
                    PrintJobInfo printJob = printJobs.get(i);
                    this.mSpooler.setPrintJobState(printJob.getId(), 6, this.mContext.getString(R.string.miniresolver_use_personal_browser));
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    private void throwIfDestroyedLocked() {
        if (this.mDestroyed) {
            throw new IllegalStateException("Cannot interact with a destroyed instance.");
        }
    }

    private void handleDispatchPrintJobStateChanged(PrintJobId printJobId, int appId) {
        synchronized (this.mLock) {
            if (this.mPrintJobStateChangeListenerRecords != null) {
                List<PrintJobStateChangeListenerRecord> records = new ArrayList<>(this.mPrintJobStateChangeListenerRecords);
                int recordCount = records.size();
                for (int i = 0; i < recordCount; i++) {
                    PrintJobStateChangeListenerRecord record = records.get(i);
                    if (record.appId == -2 || record.appId == appId) {
                        try {
                            record.listener.onPrintJobStateChanged(printJobId);
                        } catch (RemoteException re) {
                            Log.e(LOG_TAG, "Error notifying for print job state change", re);
                        }
                    }
                }
            }
        }
    }

    private final class UserStateHandler extends Handler {
        public static final int MSG_DISPATCH_PRINT_JOB_STATE_CHANGED = 1;

        public UserStateHandler(Looper looper) {
            super(looper, null, UserState.DEBUG);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                PrintJobId printJobId = (PrintJobId) message.obj;
                int appId = message.arg1;
                UserState.this.handleDispatchPrintJobStateChanged(printJobId, appId);
            }
        }
    }

    private abstract class PrintJobStateChangeListenerRecord implements IBinder.DeathRecipient {
        final int appId;
        final IPrintJobStateChangeListener listener;

        public abstract void onBinderDied();

        public PrintJobStateChangeListenerRecord(IPrintJobStateChangeListener listener, int appId) throws RemoteException {
            this.listener = listener;
            this.appId = appId;
            listener.asBinder().linkToDeath(this, 0);
        }

        @Override
        public void binderDied() {
            this.listener.asBinder().unlinkToDeath(this, 0);
            onBinderDied();
        }
    }

    private class PrinterDiscoverySessionMediator {
        private final Handler mHandler;
        private boolean mIsDestroyed;
        private final ArrayMap<PrinterId, PrinterInfo> mPrinters = new ArrayMap<>();
        private final RemoteCallbackList<IPrinterDiscoveryObserver> mDiscoveryObservers = new RemoteCallbackList<IPrinterDiscoveryObserver>() {
            @Override
            public void onCallbackDied(IPrinterDiscoveryObserver observer) {
                synchronized (UserState.this.mLock) {
                    PrinterDiscoverySessionMediator.this.stopPrinterDiscoveryLocked(observer);
                    PrinterDiscoverySessionMediator.this.removeObserverLocked(observer);
                }
            }
        };
        private final List<IBinder> mStartedPrinterDiscoveryTokens = new ArrayList();
        private final List<PrinterId> mStateTrackedPrinters = new ArrayList();

        public PrinterDiscoverySessionMediator(Context context) {
            this.mHandler = new SessionHandler(context.getMainLooper());
            List<RemotePrintService> services = new ArrayList<>((Collection<? extends RemotePrintService>) UserState.this.mActiveServices.values());
            this.mHandler.obtainMessage(9, services).sendToTarget();
        }

        public void addObserverLocked(IPrinterDiscoveryObserver observer) {
            this.mDiscoveryObservers.register(observer);
            if (!this.mPrinters.isEmpty()) {
                List<PrinterInfo> printers = new ArrayList<>(this.mPrinters.values());
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = observer;
                args.arg2 = printers;
                this.mHandler.obtainMessage(1, args).sendToTarget();
            }
        }

        public void removeObserverLocked(IPrinterDiscoveryObserver observer) {
            this.mDiscoveryObservers.unregister(observer);
            if (this.mDiscoveryObservers.getRegisteredCallbackCount() == 0) {
                destroyLocked();
            }
        }

        public final void startPrinterDiscoveryLocked(IPrinterDiscoveryObserver observer, List<PrinterId> priorityList) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not starting dicovery - session destroyed");
                return;
            }
            boolean discoveryStarted = !this.mStartedPrinterDiscoveryTokens.isEmpty() ? true : UserState.DEBUG;
            this.mStartedPrinterDiscoveryTokens.add(observer.asBinder());
            if (discoveryStarted && priorityList != null && !priorityList.isEmpty()) {
                UserState.this.validatePrinters(priorityList);
                return;
            }
            if (this.mStartedPrinterDiscoveryTokens.size() <= 1) {
                List<RemotePrintService> services = new ArrayList<>((Collection<? extends RemotePrintService>) UserState.this.mActiveServices.values());
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = services;
                args.arg2 = priorityList;
                this.mHandler.obtainMessage(11, args).sendToTarget();
            }
        }

        public final void stopPrinterDiscoveryLocked(IPrinterDiscoveryObserver observer) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not stopping dicovery - session destroyed");
            } else if (this.mStartedPrinterDiscoveryTokens.remove(observer.asBinder()) && this.mStartedPrinterDiscoveryTokens.isEmpty()) {
                List<RemotePrintService> services = new ArrayList<>((Collection<? extends RemotePrintService>) UserState.this.mActiveServices.values());
                this.mHandler.obtainMessage(12, services).sendToTarget();
            }
        }

        public void validatePrintersLocked(List<PrinterId> printerIds) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not validating pritners - session destroyed");
                return;
            }
            List<PrinterId> remainingList = new ArrayList<>(printerIds);
            while (!remainingList.isEmpty()) {
                Iterator<PrinterId> iterator = remainingList.iterator();
                List<PrinterId> updateList = new ArrayList<>();
                ComponentName serviceName = null;
                while (iterator.hasNext()) {
                    PrinterId printerId = iterator.next();
                    if (updateList.isEmpty()) {
                        updateList.add(printerId);
                        serviceName = printerId.getServiceName();
                        iterator.remove();
                    } else if (printerId.getServiceName().equals(serviceName)) {
                        updateList.add(printerId);
                        iterator.remove();
                    }
                }
                RemotePrintService service = (RemotePrintService) UserState.this.mActiveServices.get(serviceName);
                if (service != null) {
                    SomeArgs args = SomeArgs.obtain();
                    args.arg1 = service;
                    args.arg2 = updateList;
                    this.mHandler.obtainMessage(13, args).sendToTarget();
                }
            }
        }

        public final void startPrinterStateTrackingLocked(PrinterId printerId) {
            RemotePrintService service;
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not starting printer state tracking - session destroyed");
                return;
            }
            if (!this.mStartedPrinterDiscoveryTokens.isEmpty()) {
                boolean containedPrinterId = this.mStateTrackedPrinters.contains(printerId);
                this.mStateTrackedPrinters.add(printerId);
                if (!containedPrinterId && (service = (RemotePrintService) UserState.this.mActiveServices.get(printerId.getServiceName())) != null) {
                    SomeArgs args = SomeArgs.obtain();
                    args.arg1 = service;
                    args.arg2 = printerId;
                    this.mHandler.obtainMessage(14, args).sendToTarget();
                }
            }
        }

        public final void stopPrinterStateTrackingLocked(PrinterId printerId) {
            RemotePrintService service;
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not stopping printer state tracking - session destroyed");
                return;
            }
            if (!this.mStartedPrinterDiscoveryTokens.isEmpty() && this.mStateTrackedPrinters.remove(printerId) && (service = (RemotePrintService) UserState.this.mActiveServices.get(printerId.getServiceName())) != null) {
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = service;
                args.arg2 = printerId;
                this.mHandler.obtainMessage(15, args).sendToTarget();
            }
        }

        public void onDestroyed() {
        }

        public void destroyLocked() {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not destroying - session destroyed");
                return;
            }
            this.mIsDestroyed = true;
            int printerCount = this.mStateTrackedPrinters.size();
            for (int i = 0; i < printerCount; i++) {
                PrinterId printerId = this.mStateTrackedPrinters.get(i);
                UserState.this.stopPrinterStateTracking(printerId);
            }
            int observerCount = this.mStartedPrinterDiscoveryTokens.size();
            for (int i2 = 0; i2 < observerCount; i2++) {
                IBinder token = this.mStartedPrinterDiscoveryTokens.get(i2);
                stopPrinterDiscoveryLocked(IPrinterDiscoveryObserver.Stub.asInterface(token));
            }
            List<RemotePrintService> services = new ArrayList<>((Collection<? extends RemotePrintService>) UserState.this.mActiveServices.values());
            this.mHandler.obtainMessage(10, services).sendToTarget();
        }

        public void onPrintersAddedLocked(List<PrinterInfo> printers) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not adding printers - session destroyed");
                return;
            }
            List<PrinterInfo> addedPrinters = null;
            int addedPrinterCount = printers.size();
            for (int i = 0; i < addedPrinterCount; i++) {
                PrinterInfo printer = printers.get(i);
                PrinterInfo oldPrinter = this.mPrinters.put(printer.getId(), printer);
                if (oldPrinter == null || !oldPrinter.equals(printer)) {
                    if (addedPrinters == null) {
                        addedPrinters = new ArrayList<>();
                    }
                    addedPrinters.add(printer);
                }
            }
            if (addedPrinters != null) {
                this.mHandler.obtainMessage(3, addedPrinters).sendToTarget();
            }
        }

        public void onPrintersRemovedLocked(List<PrinterId> printerIds) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not removing printers - session destroyed");
                return;
            }
            List<PrinterId> removedPrinterIds = null;
            int removedPrinterCount = printerIds.size();
            for (int i = 0; i < removedPrinterCount; i++) {
                PrinterId removedPrinterId = printerIds.get(i);
                if (this.mPrinters.remove(removedPrinterId) != null) {
                    if (removedPrinterIds == null) {
                        removedPrinterIds = new ArrayList<>();
                    }
                    removedPrinterIds.add(removedPrinterId);
                }
            }
            if (removedPrinterIds != null) {
                this.mHandler.obtainMessage(4, removedPrinterIds).sendToTarget();
            }
        }

        public void onServiceRemovedLocked(RemotePrintService service) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not updating removed service - session destroyed");
                return;
            }
            ComponentName serviceName = service.getComponentName();
            removePrintersForServiceLocked(serviceName);
            service.destroy();
        }

        public void onServiceDiedLocked(RemotePrintService service) {
            removePrintersForServiceLocked(service.getComponentName());
        }

        public void onServiceAddedLocked(RemotePrintService service) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not updating added service - session destroyed");
                return;
            }
            this.mHandler.obtainMessage(5, service).sendToTarget();
            if (!this.mStartedPrinterDiscoveryTokens.isEmpty()) {
                this.mHandler.obtainMessage(7, service).sendToTarget();
            }
            int trackedPrinterCount = this.mStateTrackedPrinters.size();
            for (int i = 0; i < trackedPrinterCount; i++) {
                PrinterId printerId = this.mStateTrackedPrinters.get(i);
                if (printerId.getServiceName().equals(service.getComponentName())) {
                    SomeArgs args = SomeArgs.obtain();
                    args.arg1 = service;
                    args.arg2 = printerId;
                    this.mHandler.obtainMessage(14, args).sendToTarget();
                }
            }
        }

        public void dump(PrintWriter pw, String prefix) {
            pw.append((CharSequence) prefix).append("destroyed=").append((CharSequence) String.valueOf(UserState.this.mDestroyed)).println();
            pw.append((CharSequence) prefix).append("printDiscoveryInProgress=").append((CharSequence) String.valueOf(!this.mStartedPrinterDiscoveryTokens.isEmpty() ? true : UserState.DEBUG)).println();
            pw.append((CharSequence) prefix).append("  ").append("printer discovery observers:").println();
            int observerCount = this.mDiscoveryObservers.beginBroadcast();
            for (int i = 0; i < observerCount; i++) {
                IPrinterDiscoveryObserver observer = this.mDiscoveryObservers.getBroadcastItem(i);
                pw.append((CharSequence) prefix).append((CharSequence) prefix).append((CharSequence) observer.toString());
                pw.println();
            }
            this.mDiscoveryObservers.finishBroadcast();
            pw.append((CharSequence) prefix).append("  ").append("start discovery requests:").println();
            int tokenCount = this.mStartedPrinterDiscoveryTokens.size();
            for (int i2 = 0; i2 < tokenCount; i2++) {
                IBinder token = this.mStartedPrinterDiscoveryTokens.get(i2);
                pw.append((CharSequence) prefix).append("  ").append("  ").append((CharSequence) token.toString()).println();
            }
            pw.append((CharSequence) prefix).append("  ").append("tracked printer requests:").println();
            int trackedPrinters = this.mStateTrackedPrinters.size();
            for (int i3 = 0; i3 < trackedPrinters; i3++) {
                PrinterId printer = this.mStateTrackedPrinters.get(i3);
                pw.append((CharSequence) prefix).append("  ").append("  ").append((CharSequence) printer.toString()).println();
            }
            pw.append((CharSequence) prefix).append("  ").append("printers:").println();
            int pritnerCount = this.mPrinters.size();
            for (int i4 = 0; i4 < pritnerCount; i4++) {
                PrinterInfo printer2 = this.mPrinters.valueAt(i4);
                pw.append((CharSequence) prefix).append("  ").append("  ").append((CharSequence) printer2.toString()).println();
            }
        }

        private void removePrintersForServiceLocked(ComponentName serviceName) {
            if (!this.mPrinters.isEmpty()) {
                List<PrinterId> removedPrinterIds = null;
                int printerCount = this.mPrinters.size();
                for (int i = 0; i < printerCount; i++) {
                    PrinterId printerId = this.mPrinters.keyAt(i);
                    if (printerId.getServiceName().equals(serviceName)) {
                        if (removedPrinterIds == null) {
                            removedPrinterIds = new ArrayList<>();
                        }
                        removedPrinterIds.add(printerId);
                    }
                }
                if (removedPrinterIds != null) {
                    int removedPrinterCount = removedPrinterIds.size();
                    for (int i2 = 0; i2 < removedPrinterCount; i2++) {
                        this.mPrinters.remove(removedPrinterIds.get(i2));
                    }
                    this.mHandler.obtainMessage(4, removedPrinterIds).sendToTarget();
                }
            }
        }

        private void handleDispatchPrintersAdded(List<PrinterInfo> addedPrinters) {
            int observerCount = this.mDiscoveryObservers.beginBroadcast();
            for (int i = 0; i < observerCount; i++) {
                IPrinterDiscoveryObserver observer = (IPrinterDiscoveryObserver) this.mDiscoveryObservers.getBroadcastItem(i);
                handlePrintersAdded(observer, addedPrinters);
            }
            this.mDiscoveryObservers.finishBroadcast();
        }

        private void handleDispatchPrintersRemoved(List<PrinterId> removedPrinterIds) {
            int observerCount = this.mDiscoveryObservers.beginBroadcast();
            for (int i = 0; i < observerCount; i++) {
                IPrinterDiscoveryObserver observer = (IPrinterDiscoveryObserver) this.mDiscoveryObservers.getBroadcastItem(i);
                handlePrintersRemoved(observer, removedPrinterIds);
            }
            this.mDiscoveryObservers.finishBroadcast();
        }

        private void handleDispatchCreatePrinterDiscoverySession(List<RemotePrintService> services) {
            int serviceCount = services.size();
            for (int i = 0; i < serviceCount; i++) {
                RemotePrintService service = services.get(i);
                service.createPrinterDiscoverySession();
            }
        }

        private void handleDispatchDestroyPrinterDiscoverySession(List<RemotePrintService> services) {
            int serviceCount = services.size();
            for (int i = 0; i < serviceCount; i++) {
                RemotePrintService service = services.get(i);
                service.destroyPrinterDiscoverySession();
            }
            onDestroyed();
        }

        private void handleDispatchStartPrinterDiscovery(List<RemotePrintService> services, List<PrinterId> printerIds) {
            int serviceCount = services.size();
            for (int i = 0; i < serviceCount; i++) {
                RemotePrintService service = services.get(i);
                service.startPrinterDiscovery(printerIds);
            }
        }

        private void handleDispatchStopPrinterDiscovery(List<RemotePrintService> services) {
            int serviceCount = services.size();
            for (int i = 0; i < serviceCount; i++) {
                RemotePrintService service = services.get(i);
                service.stopPrinterDiscovery();
            }
        }

        private void handleValidatePrinters(RemotePrintService service, List<PrinterId> printerIds) {
            service.validatePrinters(printerIds);
        }

        private void handleStartPrinterStateTracking(RemotePrintService service, PrinterId printerId) {
            service.startPrinterStateTracking(printerId);
        }

        private void handleStopPrinterStateTracking(RemotePrintService service, PrinterId printerId) {
            service.stopPrinterStateTracking(printerId);
        }

        private void handlePrintersAdded(IPrinterDiscoveryObserver observer, List<PrinterInfo> printers) {
            try {
                observer.onPrintersAdded(new ParceledListSlice(printers));
            } catch (RemoteException re) {
                Log.e(UserState.LOG_TAG, "Error sending added printers", re);
            }
        }

        private void handlePrintersRemoved(IPrinterDiscoveryObserver observer, List<PrinterId> printerIds) {
            try {
                observer.onPrintersRemoved(new ParceledListSlice(printerIds));
            } catch (RemoteException re) {
                Log.e(UserState.LOG_TAG, "Error sending removed printers", re);
            }
        }

        private final class SessionHandler extends Handler {
            public static final int MSG_CREATE_PRINTER_DISCOVERY_SESSION = 5;
            public static final int MSG_DESTROY_PRINTER_DISCOVERY_SESSION = 6;
            public static final int MSG_DESTROY_SERVICE = 16;
            public static final int MSG_DISPATCH_CREATE_PRINTER_DISCOVERY_SESSION = 9;
            public static final int MSG_DISPATCH_DESTROY_PRINTER_DISCOVERY_SESSION = 10;
            public static final int MSG_DISPATCH_PRINTERS_ADDED = 3;
            public static final int MSG_DISPATCH_PRINTERS_REMOVED = 4;
            public static final int MSG_DISPATCH_START_PRINTER_DISCOVERY = 11;
            public static final int MSG_DISPATCH_STOP_PRINTER_DISCOVERY = 12;
            public static final int MSG_PRINTERS_ADDED = 1;
            public static final int MSG_PRINTERS_REMOVED = 2;
            public static final int MSG_START_PRINTER_DISCOVERY = 7;
            public static final int MSG_START_PRINTER_STATE_TRACKING = 14;
            public static final int MSG_STOP_PRINTER_DISCOVERY = 8;
            public static final int MSG_STOP_PRINTER_STATE_TRACKING = 15;
            public static final int MSG_VALIDATE_PRINTERS = 13;

            SessionHandler(Looper looper) {
                super(looper, null, UserState.DEBUG);
            }

            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        SomeArgs args = (SomeArgs) message.obj;
                        IPrinterDiscoveryObserver observer = (IPrinterDiscoveryObserver) args.arg1;
                        List<PrinterInfo> addedPrinters = (List) args.arg2;
                        args.recycle();
                        PrinterDiscoverySessionMediator.this.handlePrintersAdded(observer, addedPrinters);
                        return;
                    case 2:
                        SomeArgs args2 = (SomeArgs) message.obj;
                        IPrinterDiscoveryObserver observer2 = (IPrinterDiscoveryObserver) args2.arg1;
                        List<PrinterId> removedPrinterIds = (List) args2.arg2;
                        args2.recycle();
                        PrinterDiscoverySessionMediator.this.handlePrintersRemoved(observer2, removedPrinterIds);
                        break;
                    case 3:
                        break;
                    case 4:
                        List<PrinterId> removedPrinterIds2 = (List) message.obj;
                        PrinterDiscoverySessionMediator.this.handleDispatchPrintersRemoved(removedPrinterIds2);
                        return;
                    case 5:
                        RemotePrintService service = (RemotePrintService) message.obj;
                        service.createPrinterDiscoverySession();
                        return;
                    case 6:
                        RemotePrintService service2 = (RemotePrintService) message.obj;
                        service2.destroyPrinterDiscoverySession();
                        return;
                    case 7:
                        RemotePrintService service3 = (RemotePrintService) message.obj;
                        service3.startPrinterDiscovery(null);
                        return;
                    case 8:
                        RemotePrintService service4 = (RemotePrintService) message.obj;
                        service4.stopPrinterDiscovery();
                        return;
                    case 9:
                        List<RemotePrintService> services = (List) message.obj;
                        PrinterDiscoverySessionMediator.this.handleDispatchCreatePrinterDiscoverySession(services);
                        return;
                    case 10:
                        List<RemotePrintService> services2 = (List) message.obj;
                        PrinterDiscoverySessionMediator.this.handleDispatchDestroyPrinterDiscoverySession(services2);
                        return;
                    case 11:
                        SomeArgs args3 = (SomeArgs) message.obj;
                        List<RemotePrintService> services3 = (List) args3.arg1;
                        List<PrinterId> printerIds = (List) args3.arg2;
                        args3.recycle();
                        PrinterDiscoverySessionMediator.this.handleDispatchStartPrinterDiscovery(services3, printerIds);
                        return;
                    case 12:
                        List<RemotePrintService> services4 = (List) message.obj;
                        PrinterDiscoverySessionMediator.this.handleDispatchStopPrinterDiscovery(services4);
                        return;
                    case 13:
                        SomeArgs args4 = (SomeArgs) message.obj;
                        RemotePrintService service5 = (RemotePrintService) args4.arg1;
                        List<PrinterId> printerIds2 = (List) args4.arg2;
                        args4.recycle();
                        PrinterDiscoverySessionMediator.this.handleValidatePrinters(service5, printerIds2);
                        return;
                    case 14:
                        SomeArgs args5 = (SomeArgs) message.obj;
                        RemotePrintService service6 = (RemotePrintService) args5.arg1;
                        PrinterId printerId = (PrinterId) args5.arg2;
                        args5.recycle();
                        PrinterDiscoverySessionMediator.this.handleStartPrinterStateTracking(service6, printerId);
                        return;
                    case 15:
                        SomeArgs args6 = (SomeArgs) message.obj;
                        RemotePrintService service7 = (RemotePrintService) args6.arg1;
                        PrinterId printerId2 = (PrinterId) args6.arg2;
                        args6.recycle();
                        PrinterDiscoverySessionMediator.this.handleStopPrinterStateTracking(service7, printerId2);
                        return;
                    case 16:
                        RemotePrintService service8 = (RemotePrintService) message.obj;
                        service8.destroy();
                        return;
                    default:
                        return;
                }
                List<PrinterInfo> addedPrinters2 = (List) message.obj;
                PrinterDiscoverySessionMediator.this.handleDispatchPrintersAdded(addedPrinters2);
            }
        }
    }

    private final class PrintJobForAppCache {
        private final SparseArray<List<PrintJobInfo>> mPrintJobsForRunningApp;

        private PrintJobForAppCache() {
            this.mPrintJobsForRunningApp = new SparseArray<>();
        }

        public boolean onPrintJobCreated(final IBinder creator, final int appId, PrintJobInfo printJob) {
            try {
                creator.linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        creator.unlinkToDeath(this, 0);
                        synchronized (UserState.this.mLock) {
                            PrintJobForAppCache.this.mPrintJobsForRunningApp.remove(appId);
                        }
                    }
                }, 0);
                synchronized (UserState.this.mLock) {
                    List<PrintJobInfo> printJobsForApp = this.mPrintJobsForRunningApp.get(appId);
                    if (printJobsForApp == null) {
                        printJobsForApp = new ArrayList<>();
                        this.mPrintJobsForRunningApp.put(appId, printJobsForApp);
                    }
                    printJobsForApp.add(printJob);
                }
                return true;
            } catch (RemoteException e) {
                return UserState.DEBUG;
            }
        }

        public void onPrintJobStateChanged(PrintJobInfo printJob) {
            synchronized (UserState.this.mLock) {
                List<PrintJobInfo> printJobsForApp = this.mPrintJobsForRunningApp.get(printJob.getAppId());
                if (printJobsForApp != null) {
                    int printJobCount = printJobsForApp.size();
                    for (int i = 0; i < printJobCount; i++) {
                        PrintJobInfo oldPrintJob = printJobsForApp.get(i);
                        if (oldPrintJob.getId().equals(printJob.getId())) {
                            printJobsForApp.set(i, printJob);
                        }
                    }
                }
            }
        }

        public PrintJobInfo getPrintJob(PrintJobId printJobId, int appId) {
            synchronized (UserState.this.mLock) {
                List<PrintJobInfo> printJobsForApp = this.mPrintJobsForRunningApp.get(appId);
                if (printJobsForApp == null) {
                    return null;
                }
                int printJobCount = printJobsForApp.size();
                for (int i = 0; i < printJobCount; i++) {
                    PrintJobInfo printJob = printJobsForApp.get(i);
                    if (printJob.getId().equals(printJobId)) {
                        return printJob;
                    }
                }
                return null;
            }
        }

        public List<PrintJobInfo> getPrintJobs(int appId) throws Throwable {
            synchronized (UserState.this.mLock) {
                List<PrintJobInfo> printJobs = null;
                try {
                    if (appId == -2) {
                        int bucketCount = this.mPrintJobsForRunningApp.size();
                        int i = 0;
                        List<PrintJobInfo> printJobs2 = null;
                        while (i < bucketCount) {
                            try {
                                List<PrintJobInfo> bucket = this.mPrintJobsForRunningApp.valueAt(i);
                                List<PrintJobInfo> printJobs3 = printJobs2 == null ? new ArrayList<>() : printJobs2;
                                printJobs3.addAll(bucket);
                                i++;
                                printJobs2 = printJobs3;
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        }
                        printJobs = printJobs2;
                    } else {
                        List<PrintJobInfo> bucket2 = this.mPrintJobsForRunningApp.get(appId);
                        if (bucket2 != null) {
                            if (0 == 0) {
                                printJobs = new ArrayList<>();
                            }
                            printJobs.addAll(bucket2);
                        }
                    }
                    if (printJobs != null) {
                        return printJobs;
                    }
                    return Collections.emptyList();
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }

        public void dump(PrintWriter pw, String prefix) {
            synchronized (UserState.this.mLock) {
                int bucketCount = this.mPrintJobsForRunningApp.size();
                for (int i = 0; i < bucketCount; i++) {
                    int appId = this.mPrintJobsForRunningApp.keyAt(i);
                    pw.append((CharSequence) prefix).append((CharSequence) ("appId=" + appId)).append(UserState.COMPONENT_NAME_SEPARATOR).println();
                    List<PrintJobInfo> bucket = this.mPrintJobsForRunningApp.valueAt(i);
                    int printJobCount = bucket.size();
                    for (int j = 0; j < printJobCount; j++) {
                        PrintJobInfo printJob = bucket.get(j);
                        pw.append((CharSequence) prefix).append("  ").append((CharSequence) printJob.toString()).println();
                    }
                }
            }
        }
    }
}
