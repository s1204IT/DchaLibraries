package com.android.server.print;

import android.R;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.print.IPrintDocumentAdapter;
import android.print.IPrintJobStateChangeListener;
import android.print.IPrintServicesChangeListener;
import android.print.IPrinterDiscoveryObserver;
import android.print.PrintAttributes;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.PrintServiceInfo;
import android.printservice.recommendation.IRecommendationsChangeListener;
import android.printservice.recommendation.RecommendationInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.internal.util.dump.DumpUtils;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.internal.util.function.pooled.PooledSupplier;
import com.android.server.print.RemotePrintService;
import com.android.server.print.RemotePrintServiceRecommendationService;
import com.android.server.print.RemotePrintSpooler;
import com.android.server.print.UserState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

final class UserState implements RemotePrintSpooler.PrintSpoolerCallbacks, RemotePrintService.PrintServiceCallbacks, RemotePrintServiceRecommendationService.RemotePrintServiceRecommendationServiceCallbacks {
    private static final char COMPONENT_NAME_SEPARATOR = ':';
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "UserState";
    private static final int SERVICE_RESTART_DELAY_MILLIS = 500;
    private final Context mContext;
    private boolean mDestroyed;
    private boolean mIsInstantServiceAllowed;
    private final Object mLock;
    private List<PrintJobStateChangeListenerRecord> mPrintJobStateChangeListenerRecords;
    private List<RecommendationInfo> mPrintServiceRecommendations;
    private List<ListenerRecord<IRecommendationsChangeListener>> mPrintServiceRecommendationsChangeListenerRecords;
    private RemotePrintServiceRecommendationService mPrintServiceRecommendationsService;
    private List<ListenerRecord<IPrintServicesChangeListener>> mPrintServicesChangeListenerRecords;
    private PrinterDiscoverySessionMediator mPrinterDiscoverySession;
    private final RemotePrintSpooler mSpooler;
    private final int mUserId;
    private final TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(COMPONENT_NAME_SEPARATOR);
    private final Intent mQueryIntent = new Intent("android.printservice.PrintService");
    private final ArrayMap<ComponentName, RemotePrintService> mActiveServices = new ArrayMap<>();
    private final List<PrintServiceInfo> mInstalledServices = new ArrayList();
    private final Set<ComponentName> mDisabledServices = new ArraySet();
    private final PrintJobForAppCache mPrintJobForAppCache = new PrintJobForAppCache();

    public UserState(Context context, int i, Object obj, boolean z) {
        this.mContext = context;
        this.mUserId = i;
        this.mLock = obj;
        this.mSpooler = new RemotePrintSpooler(context, i, z, this);
        synchronized (this.mLock) {
            readInstalledPrintServicesLocked();
            upgradePersistentStateIfNeeded();
            readDisabledPrintServicesLocked();
        }
        prunePrintServices();
        onConfigurationChanged();
    }

    public void increasePriority() {
        this.mSpooler.increasePriority();
    }

    @Override
    public void onPrintJobQueued(PrintJobInfo printJobInfo) {
        RemotePrintService remotePrintService;
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            remotePrintService = this.mActiveServices.get(printJobInfo.getPrinterId().getServiceName());
        }
        if (remotePrintService != null) {
            remotePrintService.onPrintJobQueued(printJobInfo);
        } else {
            this.mSpooler.setPrintJobState(printJobInfo.getId(), 6, this.mContext.getString(R.string.keyguard_password_enter_pin_password_code));
        }
    }

    @Override
    public void onAllPrintJobsForServiceHandled(ComponentName componentName) {
        RemotePrintService remotePrintService;
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            remotePrintService = this.mActiveServices.get(componentName);
        }
        if (remotePrintService != null) {
            remotePrintService.onAllPrintJobsHandled();
        }
    }

    public void removeObsoletePrintJobs() {
        this.mSpooler.removeObsoletePrintJobs();
    }

    public Bundle print(String str, IPrintDocumentAdapter iPrintDocumentAdapter, PrintAttributes printAttributes, String str2, int i) {
        PrintJobInfo printJobInfo = new PrintJobInfo();
        printJobInfo.setId(new PrintJobId());
        printJobInfo.setAppId(i);
        printJobInfo.setLabel(str);
        printJobInfo.setAttributes(printAttributes);
        printJobInfo.setState(1);
        printJobInfo.setCopies(1);
        printJobInfo.setCreationTime(System.currentTimeMillis());
        if (!this.mPrintJobForAppCache.onPrintJobCreated(iPrintDocumentAdapter.asBinder(), i, printJobInfo)) {
            return null;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Intent intent = new Intent("android.print.PRINT_DIALOG");
            intent.setData(Uri.fromParts("printjob", printJobInfo.getId().flattenToString(), null));
            intent.putExtra("android.print.intent.extra.EXTRA_PRINT_DOCUMENT_ADAPTER", iPrintDocumentAdapter.asBinder());
            intent.putExtra("android.print.intent.extra.EXTRA_PRINT_JOB", printJobInfo);
            intent.putExtra("android.content.extra.PACKAGE_NAME", str2);
            IntentSender intentSender = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 1409286144, null, new UserHandle(this.mUserId)).getIntentSender();
            Bundle bundle = new Bundle();
            bundle.putParcelable("android.print.intent.extra.EXTRA_PRINT_JOB", printJobInfo);
            bundle.putParcelable("android.print.intent.extra.EXTRA_PRINT_DIALOG_INTENT", intentSender);
            return bundle;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public List<PrintJobInfo> getPrintJobInfos(int i) {
        List<PrintJobInfo> printJobs = this.mPrintJobForAppCache.getPrintJobs(i);
        ArrayMap arrayMap = new ArrayMap();
        int size = printJobs.size();
        for (int i2 = 0; i2 < size; i2++) {
            PrintJobInfo printJobInfo = printJobs.get(i2);
            arrayMap.put(printJobInfo.getId(), printJobInfo);
            printJobInfo.setTag(null);
            printJobInfo.setAdvancedOptions(null);
        }
        List<PrintJobInfo> printJobInfos = this.mSpooler.getPrintJobInfos(null, -1, i);
        if (printJobInfos != null) {
            int size2 = printJobInfos.size();
            for (int i3 = 0; i3 < size2; i3++) {
                PrintJobInfo printJobInfo2 = printJobInfos.get(i3);
                arrayMap.put(printJobInfo2.getId(), printJobInfo2);
                printJobInfo2.setTag(null);
                printJobInfo2.setAdvancedOptions(null);
            }
        }
        return new ArrayList(arrayMap.values());
    }

    public PrintJobInfo getPrintJobInfo(PrintJobId printJobId, int i) {
        PrintJobInfo printJob = this.mPrintJobForAppCache.getPrintJob(printJobId, i);
        if (printJob == null) {
            printJob = this.mSpooler.getPrintJobInfo(printJobId, i);
        }
        if (printJob != null) {
            printJob.setTag(null);
            printJob.setAdvancedOptions(null);
        }
        return printJob;
    }

    public Icon getCustomPrinterIcon(PrinterId printerId) {
        RemotePrintService remotePrintService;
        Icon customPrinterIcon = this.mSpooler.getCustomPrinterIcon(printerId);
        if (customPrinterIcon == null && (remotePrintService = this.mActiveServices.get(printerId.getServiceName())) != null) {
            remotePrintService.requestCustomPrinterIcon(printerId);
        }
        return customPrinterIcon;
    }

    public void cancelPrintJob(PrintJobId printJobId, int i) {
        RemotePrintService remotePrintService;
        PrintJobInfo printJobInfo = this.mSpooler.getPrintJobInfo(printJobId, i);
        if (printJobInfo == null) {
            return;
        }
        this.mSpooler.setPrintJobCancelling(printJobId, true);
        if (printJobInfo.getState() != 6) {
            PrinterId printerId = printJobInfo.getPrinterId();
            if (printerId != null) {
                ComponentName serviceName = printerId.getServiceName();
                synchronized (this.mLock) {
                    remotePrintService = this.mActiveServices.get(serviceName);
                }
                if (remotePrintService == null) {
                    return;
                }
                remotePrintService.onRequestCancelPrintJob(printJobInfo);
                return;
            }
            return;
        }
        this.mSpooler.setPrintJobState(printJobId, 7, null);
    }

    public void restartPrintJob(PrintJobId printJobId, int i) {
        PrintJobInfo printJobInfo = getPrintJobInfo(printJobId, i);
        if (printJobInfo == null || printJobInfo.getState() != 6) {
            return;
        }
        this.mSpooler.setPrintJobState(printJobId, 2, null);
    }

    public List<PrintServiceInfo> getPrintServices(int i) {
        ArrayList arrayList;
        synchronized (this.mLock) {
            arrayList = null;
            int size = this.mInstalledServices.size();
            for (int i2 = 0; i2 < size; i2++) {
                PrintServiceInfo printServiceInfo = this.mInstalledServices.get(i2);
                printServiceInfo.setIsEnabled(this.mActiveServices.containsKey(new ComponentName(printServiceInfo.getResolveInfo().serviceInfo.packageName, printServiceInfo.getResolveInfo().serviceInfo.name)));
                if (printServiceInfo.isEnabled()) {
                    if ((i & 1) != 0) {
                        if (arrayList == null) {
                            arrayList = new ArrayList();
                        }
                        arrayList.add(printServiceInfo);
                    }
                } else if ((i & 2) == 0) {
                }
            }
        }
        return arrayList;
    }

    public void setPrintServiceEnabled(ComponentName componentName, boolean z) {
        synchronized (this.mLock) {
            boolean zRemove = false;
            try {
                if (z) {
                    zRemove = this.mDisabledServices.remove(componentName);
                } else {
                    int size = this.mInstalledServices.size();
                    int i = 0;
                    while (true) {
                        if (i >= size) {
                            break;
                        }
                        if (!this.mInstalledServices.get(i).getComponentName().equals(componentName)) {
                            i++;
                        } else {
                            this.mDisabledServices.add(componentName);
                            zRemove = true;
                            break;
                        }
                    }
                }
                if (zRemove) {
                    writeDisabledPrintServicesLocked(this.mDisabledServices);
                    MetricsLogger.action(this.mContext, 511, !z ? 1 : 0);
                    onConfigurationChangedLocked();
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public List<RecommendationInfo> getPrintServiceRecommendations() {
        return this.mPrintServiceRecommendations;
    }

    public void createPrinterDiscoverySession(IPrinterDiscoveryObserver iPrinterDiscoveryObserver) {
        this.mSpooler.clearCustomPrinterIconCache();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrinterDiscoverySession == null) {
                this.mPrinterDiscoverySession = new PrinterDiscoverySessionMediator() {
                    @Override
                    public void onDestroyed() {
                        UserState.this.mPrinterDiscoverySession = null;
                    }
                };
                this.mPrinterDiscoverySession.addObserverLocked(iPrinterDiscoveryObserver);
            } else {
                this.mPrinterDiscoverySession.addObserverLocked(iPrinterDiscoveryObserver);
            }
        }
    }

    public void destroyPrinterDiscoverySession(IPrinterDiscoveryObserver iPrinterDiscoveryObserver) {
        synchronized (this.mLock) {
            if (this.mPrinterDiscoverySession == null) {
                return;
            }
            this.mPrinterDiscoverySession.removeObserverLocked(iPrinterDiscoveryObserver);
        }
    }

    public void startPrinterDiscovery(IPrinterDiscoveryObserver iPrinterDiscoveryObserver, List<PrinterId> list) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrinterDiscoverySession == null) {
                return;
            }
            this.mPrinterDiscoverySession.startPrinterDiscoveryLocked(iPrinterDiscoveryObserver, list);
        }
    }

    public void stopPrinterDiscovery(IPrinterDiscoveryObserver iPrinterDiscoveryObserver) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrinterDiscoverySession == null) {
                return;
            }
            this.mPrinterDiscoverySession.stopPrinterDiscoveryLocked(iPrinterDiscoveryObserver);
        }
    }

    public void validatePrinters(List<PrinterId> list) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mActiveServices.isEmpty()) {
                return;
            }
            if (this.mPrinterDiscoverySession == null) {
                return;
            }
            this.mPrinterDiscoverySession.validatePrintersLocked(list);
        }
    }

    public void startPrinterStateTracking(PrinterId printerId) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mActiveServices.isEmpty()) {
                return;
            }
            if (this.mPrinterDiscoverySession == null) {
                return;
            }
            this.mPrinterDiscoverySession.startPrinterStateTrackingLocked(printerId);
        }
    }

    public void stopPrinterStateTracking(PrinterId printerId) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mActiveServices.isEmpty()) {
                return;
            }
            if (this.mPrinterDiscoverySession == null) {
                return;
            }
            this.mPrinterDiscoverySession.stopPrinterStateTrackingLocked(printerId);
        }
    }

    public void addPrintJobStateChangeListener(IPrintJobStateChangeListener iPrintJobStateChangeListener, int i) throws RemoteException {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrintJobStateChangeListenerRecords == null) {
                this.mPrintJobStateChangeListenerRecords = new ArrayList();
            }
            this.mPrintJobStateChangeListenerRecords.add(new PrintJobStateChangeListenerRecord(iPrintJobStateChangeListener, i) {
                @Override
                public void onBinderDied() {
                    synchronized (UserState.this.mLock) {
                        if (UserState.this.mPrintJobStateChangeListenerRecords != null) {
                            UserState.this.mPrintJobStateChangeListenerRecords.remove(this);
                        }
                    }
                }
            });
        }
    }

    public void removePrintJobStateChangeListener(IPrintJobStateChangeListener iPrintJobStateChangeListener) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrintJobStateChangeListenerRecords == null) {
                return;
            }
            int size = this.mPrintJobStateChangeListenerRecords.size();
            int i = 0;
            while (true) {
                if (i >= size) {
                    break;
                }
                PrintJobStateChangeListenerRecord printJobStateChangeListenerRecord = this.mPrintJobStateChangeListenerRecords.get(i);
                if (!printJobStateChangeListenerRecord.listener.asBinder().equals(iPrintJobStateChangeListener.asBinder())) {
                    i++;
                } else {
                    printJobStateChangeListenerRecord.destroy();
                    this.mPrintJobStateChangeListenerRecords.remove(i);
                    break;
                }
            }
            if (this.mPrintJobStateChangeListenerRecords.isEmpty()) {
                this.mPrintJobStateChangeListenerRecords = null;
            }
        }
    }

    public void addPrintServicesChangeListener(IPrintServicesChangeListener iPrintServicesChangeListener) throws RemoteException {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrintServicesChangeListenerRecords == null) {
                this.mPrintServicesChangeListenerRecords = new ArrayList();
            }
            this.mPrintServicesChangeListenerRecords.add(new ListenerRecord<IPrintServicesChangeListener>(iPrintServicesChangeListener) {
                @Override
                public void onBinderDied() {
                    synchronized (UserState.this.mLock) {
                        if (UserState.this.mPrintServicesChangeListenerRecords != null) {
                            UserState.this.mPrintServicesChangeListenerRecords.remove(this);
                        }
                    }
                }
            });
        }
    }

    public void removePrintServicesChangeListener(IPrintServicesChangeListener iPrintServicesChangeListener) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrintServicesChangeListenerRecords == null) {
                return;
            }
            int size = this.mPrintServicesChangeListenerRecords.size();
            int i = 0;
            while (true) {
                if (i >= size) {
                    break;
                }
                ListenerRecord<IPrintServicesChangeListener> listenerRecord = this.mPrintServicesChangeListenerRecords.get(i);
                if (!listenerRecord.listener.asBinder().equals(iPrintServicesChangeListener.asBinder())) {
                    i++;
                } else {
                    listenerRecord.destroy();
                    this.mPrintServicesChangeListenerRecords.remove(i);
                    break;
                }
            }
            if (this.mPrintServicesChangeListenerRecords.isEmpty()) {
                this.mPrintServicesChangeListenerRecords = null;
            }
        }
    }

    public void addPrintServiceRecommendationsChangeListener(IRecommendationsChangeListener iRecommendationsChangeListener) throws RemoteException {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrintServiceRecommendationsChangeListenerRecords == null) {
                this.mPrintServiceRecommendationsChangeListenerRecords = new ArrayList();
                this.mPrintServiceRecommendationsService = new RemotePrintServiceRecommendationService(this.mContext, UserHandle.getUserHandleForUid(this.mUserId), this);
            }
            this.mPrintServiceRecommendationsChangeListenerRecords.add(new ListenerRecord<IRecommendationsChangeListener>(iRecommendationsChangeListener) {
                @Override
                public void onBinderDied() {
                    synchronized (UserState.this.mLock) {
                        if (UserState.this.mPrintServiceRecommendationsChangeListenerRecords != null) {
                            UserState.this.mPrintServiceRecommendationsChangeListenerRecords.remove(this);
                        }
                    }
                }
            });
        }
    }

    public void removePrintServiceRecommendationsChangeListener(IRecommendationsChangeListener iRecommendationsChangeListener) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrintServiceRecommendationsChangeListenerRecords == null) {
                return;
            }
            int size = this.mPrintServiceRecommendationsChangeListenerRecords.size();
            int i = 0;
            while (true) {
                if (i >= size) {
                    break;
                }
                ListenerRecord<IRecommendationsChangeListener> listenerRecord = this.mPrintServiceRecommendationsChangeListenerRecords.get(i);
                if (!listenerRecord.listener.asBinder().equals(iRecommendationsChangeListener.asBinder())) {
                    i++;
                } else {
                    listenerRecord.destroy();
                    this.mPrintServiceRecommendationsChangeListenerRecords.remove(i);
                    break;
                }
            }
            if (this.mPrintServiceRecommendationsChangeListenerRecords.isEmpty()) {
                this.mPrintServiceRecommendationsChangeListenerRecords = null;
                this.mPrintServiceRecommendations = null;
                this.mPrintServiceRecommendationsService.close();
                this.mPrintServiceRecommendationsService = null;
            }
        }
    }

    @Override
    public void onPrintJobStateChanged(PrintJobInfo printJobInfo) {
        this.mPrintJobForAppCache.onPrintJobStateChanged(printJobInfo);
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new TriConsumer() {
            public final void accept(Object obj, Object obj2, Object obj3) {
                ((UserState) obj).handleDispatchPrintJobStateChanged((PrintJobId) obj2, (PooledSupplier.OfInt) obj3);
            }
        }, this, printJobInfo.getId(), PooledLambda.obtainSupplier(printJobInfo.getAppId()).recycleOnUse()));
    }

    public void onPrintServicesChanged() {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((UserState) obj).handleDispatchPrintServicesChanged();
            }
        }, this));
    }

    @Override
    public void onPrintServiceRecommendationsUpdated(List<RecommendationInfo> list) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((UserState) obj).handleDispatchPrintServiceRecommendationsUpdated((List) obj2);
            }
        }, this, list));
    }

    @Override
    public void onPrintersAdded(List<PrinterInfo> list) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mActiveServices.isEmpty()) {
                return;
            }
            if (this.mPrinterDiscoverySession == null) {
                return;
            }
            this.mPrinterDiscoverySession.onPrintersAddedLocked(list);
        }
    }

    @Override
    public void onPrintersRemoved(List<PrinterId> list) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mActiveServices.isEmpty()) {
                return;
            }
            if (this.mPrinterDiscoverySession == null) {
                return;
            }
            this.mPrinterDiscoverySession.onPrintersRemovedLocked(list);
        }
    }

    @Override
    public void onCustomPrinterIconLoaded(PrinterId printerId, Icon icon) {
        this.mSpooler.onCustomPrinterIconLoaded(printerId, icon);
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrinterDiscoverySession == null) {
                return;
            }
            this.mPrinterDiscoverySession.onCustomPrinterIconLoadedLocked(printerId);
        }
    }

    @Override
    public void onServiceDied(RemotePrintService remotePrintService) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mActiveServices.isEmpty()) {
                return;
            }
            failActivePrintJobsForService(remotePrintService.getComponentName());
            remotePrintService.onAllPrintJobsHandled();
            this.mActiveServices.remove(remotePrintService.getComponentName());
            Handler.getMain().sendMessageDelayed(PooledLambda.obtainMessage(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    ((UserState) obj).onConfigurationChanged();
                }
            }, this), 500L);
            if (this.mPrinterDiscoverySession == null) {
                return;
            }
            this.mPrinterDiscoverySession.onServiceDiedLocked(remotePrintService);
        }
    }

    public void updateIfNeededLocked() {
        throwIfDestroyedLocked();
        readConfigurationLocked();
        onConfigurationChangedLocked();
    }

    public void destroyLocked() {
        throwIfDestroyedLocked();
        this.mSpooler.destroy();
        Iterator<RemotePrintService> it = this.mActiveServices.values().iterator();
        while (it.hasNext()) {
            it.next().destroy();
        }
        this.mActiveServices.clear();
        this.mInstalledServices.clear();
        this.mDisabledServices.clear();
        if (this.mPrinterDiscoverySession != null) {
            this.mPrinterDiscoverySession.destroyLocked();
            this.mPrinterDiscoverySession = null;
        }
        this.mDestroyed = true;
    }

    public void dump(DualDumpOutputStream dualDumpOutputStream) {
        synchronized (this.mLock) {
            dualDumpOutputStream.write("user_id", 1120986464257L, this.mUserId);
            int size = this.mInstalledServices.size();
            for (int i = 0; i < size; i++) {
                long jStart = dualDumpOutputStream.start("installed_services", 2246267895810L);
                PrintServiceInfo printServiceInfo = this.mInstalledServices.get(i);
                ResolveInfo resolveInfo = printServiceInfo.getResolveInfo();
                DumpUtils.writeComponentName(dualDumpOutputStream, "component_name", 1146756268033L, new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name));
                DumpUtils.writeStringIfNotNull(dualDumpOutputStream, "settings_activity", 1138166333442L, printServiceInfo.getSettingsActivityName());
                DumpUtils.writeStringIfNotNull(dualDumpOutputStream, "add_printers_activity", 1138166333443L, printServiceInfo.getAddPrintersActivityName());
                DumpUtils.writeStringIfNotNull(dualDumpOutputStream, "advanced_options_activity", 1138166333444L, printServiceInfo.getAdvancedOptionsActivityName());
                dualDumpOutputStream.end(jStart);
            }
            Iterator<ComponentName> it = this.mDisabledServices.iterator();
            while (it.hasNext()) {
                DumpUtils.writeComponentName(dualDumpOutputStream, "disabled_services", 2246267895811L, it.next());
            }
            int size2 = this.mActiveServices.size();
            for (int i2 = 0; i2 < size2; i2++) {
                long jStart2 = dualDumpOutputStream.start("actives_services", 2246267895812L);
                this.mActiveServices.valueAt(i2).dump(dualDumpOutputStream);
                dualDumpOutputStream.end(jStart2);
            }
            this.mPrintJobForAppCache.dumpLocked(dualDumpOutputStream);
            if (this.mPrinterDiscoverySession != null) {
                long jStart3 = dualDumpOutputStream.start("discovery_service", 2246267895814L);
                this.mPrinterDiscoverySession.dumpLocked(dualDumpOutputStream);
                dualDumpOutputStream.end(jStart3);
            }
        }
        long jStart4 = dualDumpOutputStream.start("print_spooler_state", 1146756268039L);
        this.mSpooler.dump(dualDumpOutputStream);
        dualDumpOutputStream.end(jStart4);
    }

    private void readConfigurationLocked() {
        readInstalledPrintServicesLocked();
        readDisabledPrintServicesLocked();
    }

    private void readInstalledPrintServicesLocked() {
        int i;
        HashSet hashSet = new HashSet();
        if (this.mIsInstantServiceAllowed) {
            i = 276824196;
        } else {
            i = 268435588;
        }
        List listQueryIntentServicesAsUser = this.mContext.getPackageManager().queryIntentServicesAsUser(this.mQueryIntent, i, this.mUserId);
        int size = listQueryIntentServicesAsUser.size();
        for (int i2 = 0; i2 < size; i2++) {
            ResolveInfo resolveInfo = (ResolveInfo) listQueryIntentServicesAsUser.get(i2);
            if (!"android.permission.BIND_PRINT_SERVICE".equals(resolveInfo.serviceInfo.permission)) {
                Slog.w(LOG_TAG, "Skipping print service " + new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name).flattenToShortString() + " since it does not require permission android.permission.BIND_PRINT_SERVICE");
            } else {
                hashSet.add(PrintServiceInfo.create(this.mContext, resolveInfo));
            }
        }
        this.mInstalledServices.clear();
        this.mInstalledServices.addAll(hashSet);
    }

    private void upgradePersistentStateIfNeeded() {
        if (Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "enabled_print_services", this.mUserId) != null) {
            Set<ComponentName> hashSet = new HashSet<>();
            readPrintServicesFromSettingLocked("enabled_print_services", hashSet);
            ArraySet arraySet = new ArraySet();
            int size = this.mInstalledServices.size();
            for (int i = 0; i < size; i++) {
                ComponentName componentName = this.mInstalledServices.get(i).getComponentName();
                if (!hashSet.contains(componentName)) {
                    arraySet.add(componentName);
                }
            }
            writeDisabledPrintServicesLocked(arraySet);
            Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "enabled_print_services", null, this.mUserId);
        }
    }

    private void readDisabledPrintServicesLocked() {
        HashSet hashSet = new HashSet();
        readPrintServicesFromSettingLocked("disabled_print_services", hashSet);
        if (!hashSet.equals(this.mDisabledServices)) {
            this.mDisabledServices.clear();
            this.mDisabledServices.addAll(hashSet);
        }
    }

    private void readPrintServicesFromSettingLocked(String str, Set<ComponentName> set) {
        ComponentName componentNameUnflattenFromString;
        String stringForUser = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), str, this.mUserId);
        if (!TextUtils.isEmpty(stringForUser)) {
            TextUtils.SimpleStringSplitter simpleStringSplitter = this.mStringColonSplitter;
            simpleStringSplitter.setString(stringForUser);
            while (simpleStringSplitter.hasNext()) {
                String next = simpleStringSplitter.next();
                if (!TextUtils.isEmpty(next) && (componentNameUnflattenFromString = ComponentName.unflattenFromString(next)) != null) {
                    set.add(componentNameUnflattenFromString);
                }
            }
        }
    }

    private void writeDisabledPrintServicesLocked(Set<ComponentName> set) {
        StringBuilder sb = new StringBuilder();
        for (ComponentName componentName : set) {
            if (sb.length() > 0) {
                sb.append(COMPONENT_NAME_SEPARATOR);
            }
            sb.append(componentName.flattenToShortString());
        }
        Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "disabled_print_services", sb.toString(), this.mUserId);
    }

    private ArrayList<ComponentName> getInstalledComponents() {
        ArrayList<ComponentName> arrayList = new ArrayList<>();
        int size = this.mInstalledServices.size();
        for (int i = 0; i < size; i++) {
            ResolveInfo resolveInfo = this.mInstalledServices.get(i).getResolveInfo();
            arrayList.add(new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name));
        }
        return arrayList;
    }

    public void prunePrintServices() {
        ArrayList<ComponentName> installedComponents;
        synchronized (this.mLock) {
            installedComponents = getInstalledComponents();
            if (this.mDisabledServices.retainAll(installedComponents)) {
                writeDisabledPrintServicesLocked(this.mDisabledServices);
            }
        }
        this.mSpooler.pruneApprovedPrintServices(installedComponents);
    }

    private void onConfigurationChangedLocked() {
        ArrayList<ComponentName> installedComponents = getInstalledComponents();
        int size = installedComponents.size();
        for (int i = 0; i < size; i++) {
            ComponentName componentName = installedComponents.get(i);
            if (!this.mDisabledServices.contains(componentName)) {
                if (!this.mActiveServices.containsKey(componentName)) {
                    addServiceLocked(new RemotePrintService(this.mContext, componentName, this.mUserId, this.mSpooler, this));
                }
            } else {
                RemotePrintService remotePrintServiceRemove = this.mActiveServices.remove(componentName);
                if (remotePrintServiceRemove != null) {
                    removeServiceLocked(remotePrintServiceRemove);
                }
            }
        }
        Iterator<Map.Entry<ComponentName, RemotePrintService>> it = this.mActiveServices.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ComponentName, RemotePrintService> next = it.next();
            ComponentName key = next.getKey();
            RemotePrintService value = next.getValue();
            if (!installedComponents.contains(key)) {
                removeServiceLocked(value);
                it.remove();
            }
        }
        onPrintServicesChanged();
    }

    private void addServiceLocked(RemotePrintService remotePrintService) {
        this.mActiveServices.put(remotePrintService.getComponentName(), remotePrintService);
        if (this.mPrinterDiscoverySession != null) {
            this.mPrinterDiscoverySession.onServiceAddedLocked(remotePrintService);
        }
    }

    private void removeServiceLocked(RemotePrintService remotePrintService) {
        failActivePrintJobsForService(remotePrintService.getComponentName());
        if (this.mPrinterDiscoverySession != null) {
            this.mPrinterDiscoverySession.onServiceRemovedLocked(remotePrintService);
        } else {
            remotePrintService.destroy();
        }
    }

    private void failActivePrintJobsForService(ComponentName componentName) {
        if (Looper.getMainLooper().isCurrentThread()) {
            BackgroundThread.getHandler().sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
                @Override
                public final void accept(Object obj, Object obj2) {
                    ((UserState) obj).failScheduledPrintJobsForServiceInternal((ComponentName) obj2);
                }
            }, this, componentName));
        } else {
            failScheduledPrintJobsForServiceInternal(componentName);
        }
    }

    private void failScheduledPrintJobsForServiceInternal(ComponentName componentName) {
        List<PrintJobInfo> printJobInfos = this.mSpooler.getPrintJobInfos(componentName, -4, -2);
        if (printJobInfos == null) {
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            int size = printJobInfos.size();
            for (int i = 0; i < size; i++) {
                this.mSpooler.setPrintJobState(printJobInfos.get(i).getId(), 6, this.mContext.getString(R.string.keyguard_password_enter_pin_password_code));
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void throwIfDestroyedLocked() {
        if (this.mDestroyed) {
            throw new IllegalStateException("Cannot interact with a destroyed instance.");
        }
    }

    private void handleDispatchPrintJobStateChanged(PrintJobId printJobId, IntSupplier intSupplier) {
        int asInt = intSupplier.getAsInt();
        synchronized (this.mLock) {
            if (this.mPrintJobStateChangeListenerRecords == null) {
                return;
            }
            ArrayList arrayList = new ArrayList(this.mPrintJobStateChangeListenerRecords);
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                PrintJobStateChangeListenerRecord printJobStateChangeListenerRecord = (PrintJobStateChangeListenerRecord) arrayList.get(i);
                if (printJobStateChangeListenerRecord.appId == -2 || printJobStateChangeListenerRecord.appId == asInt) {
                    try {
                        printJobStateChangeListenerRecord.listener.onPrintJobStateChanged(printJobId);
                    } catch (RemoteException e) {
                        Log.e(LOG_TAG, "Error notifying for print job state change", e);
                    }
                }
            }
        }
    }

    private void handleDispatchPrintServicesChanged() {
        synchronized (this.mLock) {
            if (this.mPrintServicesChangeListenerRecords == null) {
                return;
            }
            ArrayList arrayList = new ArrayList(this.mPrintServicesChangeListenerRecords);
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                try {
                    ((ListenerRecord) arrayList.get(i)).listener.onPrintServicesChanged();
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "Error notifying for print services change", e);
                }
            }
        }
    }

    private void handleDispatchPrintServiceRecommendationsUpdated(List<RecommendationInfo> list) {
        synchronized (this.mLock) {
            if (this.mPrintServiceRecommendationsChangeListenerRecords == null) {
                return;
            }
            ArrayList arrayList = new ArrayList(this.mPrintServiceRecommendationsChangeListenerRecords);
            this.mPrintServiceRecommendations = list;
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                try {
                    ((ListenerRecord) arrayList.get(i)).listener.onRecommendationsChanged();
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "Error notifying for print service recommendations change", e);
                }
            }
        }
    }

    private void onConfigurationChanged() {
        synchronized (this.mLock) {
            onConfigurationChangedLocked();
        }
    }

    public boolean getBindInstantServiceAllowed() {
        return this.mIsInstantServiceAllowed;
    }

    public void setBindInstantServiceAllowed(boolean z) {
        synchronized (this.mLock) {
            this.mIsInstantServiceAllowed = z;
            updateIfNeededLocked();
        }
    }

    private abstract class PrintJobStateChangeListenerRecord implements IBinder.DeathRecipient {
        final int appId;
        final IPrintJobStateChangeListener listener;

        public abstract void onBinderDied();

        public PrintJobStateChangeListenerRecord(IPrintJobStateChangeListener iPrintJobStateChangeListener, int i) throws RemoteException {
            this.listener = iPrintJobStateChangeListener;
            this.appId = i;
            iPrintJobStateChangeListener.asBinder().linkToDeath(this, 0);
        }

        public void destroy() {
            this.listener.asBinder().unlinkToDeath(this, 0);
        }

        @Override
        public void binderDied() {
            this.listener.asBinder().unlinkToDeath(this, 0);
            onBinderDied();
        }
    }

    private abstract class ListenerRecord<T extends IInterface> implements IBinder.DeathRecipient {
        final T listener;

        public abstract void onBinderDied();

        public ListenerRecord(T t) throws RemoteException {
            this.listener = t;
            t.asBinder().linkToDeath(this, 0);
        }

        public void destroy() {
            this.listener.asBinder().unlinkToDeath(this, 0);
        }

        @Override
        public void binderDied() {
            this.listener.asBinder().unlinkToDeath(this, 0);
            onBinderDied();
        }
    }

    private class PrinterDiscoverySessionMediator {
        private boolean mIsDestroyed;
        private final ArrayMap<PrinterId, PrinterInfo> mPrinters = new ArrayMap<>();
        private final RemoteCallbackList<IPrinterDiscoveryObserver> mDiscoveryObservers = new RemoteCallbackList<IPrinterDiscoveryObserver>() {
            @Override
            public void onCallbackDied(IPrinterDiscoveryObserver iPrinterDiscoveryObserver) {
                synchronized (UserState.this.mLock) {
                    PrinterDiscoverySessionMediator.this.stopPrinterDiscoveryLocked(iPrinterDiscoveryObserver);
                    PrinterDiscoverySessionMediator.this.removeObserverLocked(iPrinterDiscoveryObserver);
                }
            }
        };
        private final List<IBinder> mStartedPrinterDiscoveryTokens = new ArrayList();
        private final List<PrinterId> mStateTrackedPrinters = new ArrayList();

        PrinterDiscoverySessionMediator() {
            Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
                @Override
                public final void accept(Object obj, Object obj2) {
                    ((UserState.PrinterDiscoverySessionMediator) obj).handleDispatchCreatePrinterDiscoverySession((ArrayList) obj2);
                }
            }, this, new ArrayList(UserState.this.mActiveServices.values())));
        }

        public void addObserverLocked(IPrinterDiscoveryObserver iPrinterDiscoveryObserver) {
            this.mDiscoveryObservers.register(iPrinterDiscoveryObserver);
            if (!this.mPrinters.isEmpty()) {
                Handler.getMain().sendMessage(PooledLambda.obtainMessage(new TriConsumer() {
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        ((UserState.PrinterDiscoverySessionMediator) obj).handlePrintersAdded((IPrinterDiscoveryObserver) obj2, (ArrayList) obj3);
                    }
                }, this, iPrinterDiscoveryObserver, new ArrayList(this.mPrinters.values())));
            }
        }

        public void removeObserverLocked(IPrinterDiscoveryObserver iPrinterDiscoveryObserver) {
            this.mDiscoveryObservers.unregister(iPrinterDiscoveryObserver);
            if (this.mDiscoveryObservers.getRegisteredCallbackCount() == 0) {
                destroyLocked();
            }
        }

        public final void startPrinterDiscoveryLocked(IPrinterDiscoveryObserver iPrinterDiscoveryObserver, List<PrinterId> list) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not starting dicovery - session destroyed");
                return;
            }
            boolean z = !this.mStartedPrinterDiscoveryTokens.isEmpty();
            this.mStartedPrinterDiscoveryTokens.add(iPrinterDiscoveryObserver.asBinder());
            if (!z || list == null || list.isEmpty()) {
                if (this.mStartedPrinterDiscoveryTokens.size() > 1) {
                    return;
                }
                Handler.getMain().sendMessage(PooledLambda.obtainMessage(new TriConsumer() {
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        ((UserState.PrinterDiscoverySessionMediator) obj).handleDispatchStartPrinterDiscovery((ArrayList) obj2, (List) obj3);
                    }
                }, this, new ArrayList(UserState.this.mActiveServices.values()), list));
                return;
            }
            UserState.this.validatePrinters(list);
        }

        public final void stopPrinterDiscoveryLocked(IPrinterDiscoveryObserver iPrinterDiscoveryObserver) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not stopping dicovery - session destroyed");
            } else {
                if (!this.mStartedPrinterDiscoveryTokens.remove(iPrinterDiscoveryObserver.asBinder()) || !this.mStartedPrinterDiscoveryTokens.isEmpty()) {
                    return;
                }
                Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
                    @Override
                    public final void accept(Object obj, Object obj2) {
                        ((UserState.PrinterDiscoverySessionMediator) obj).handleDispatchStopPrinterDiscovery((ArrayList) obj2);
                    }
                }, this, new ArrayList(UserState.this.mActiveServices.values())));
            }
        }

        public void validatePrintersLocked(List<PrinterId> list) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not validating pritners - session destroyed");
                return;
            }
            ArrayList arrayList = new ArrayList(list);
            while (!arrayList.isEmpty()) {
                Iterator it = arrayList.iterator();
                ArrayList arrayList2 = new ArrayList();
                ComponentName serviceName = null;
                while (it.hasNext()) {
                    PrinterId printerId = (PrinterId) it.next();
                    if (printerId != null) {
                        if (arrayList2.isEmpty()) {
                            arrayList2.add(printerId);
                            serviceName = printerId.getServiceName();
                            it.remove();
                        } else if (printerId.getServiceName().equals(serviceName)) {
                            arrayList2.add(printerId);
                            it.remove();
                        }
                    }
                }
                RemotePrintService remotePrintService = (RemotePrintService) UserState.this.mActiveServices.get(serviceName);
                if (remotePrintService != null) {
                    Handler.getMain().sendMessage(PooledLambda.obtainMessage(new TriConsumer() {
                        public final void accept(Object obj, Object obj2, Object obj3) {
                            ((UserState.PrinterDiscoverySessionMediator) obj).handleValidatePrinters((RemotePrintService) obj2, (List) obj3);
                        }
                    }, this, remotePrintService, arrayList2));
                }
            }
        }

        public final void startPrinterStateTrackingLocked(PrinterId printerId) {
            RemotePrintService remotePrintService;
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not starting printer state tracking - session destroyed");
                return;
            }
            if (this.mStartedPrinterDiscoveryTokens.isEmpty()) {
                return;
            }
            boolean zContains = this.mStateTrackedPrinters.contains(printerId);
            this.mStateTrackedPrinters.add(printerId);
            if (zContains || (remotePrintService = (RemotePrintService) UserState.this.mActiveServices.get(printerId.getServiceName())) == null) {
                return;
            }
            Handler.getMain().sendMessage(PooledLambda.obtainMessage(new TriConsumer() {
                public final void accept(Object obj, Object obj2, Object obj3) {
                    ((UserState.PrinterDiscoverySessionMediator) obj).handleStartPrinterStateTracking((RemotePrintService) obj2, (PrinterId) obj3);
                }
            }, this, remotePrintService, printerId));
        }

        public final void stopPrinterStateTrackingLocked(PrinterId printerId) {
            RemotePrintService remotePrintService;
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not stopping printer state tracking - session destroyed");
            } else {
                if (this.mStartedPrinterDiscoveryTokens.isEmpty() || !this.mStateTrackedPrinters.remove(printerId) || (remotePrintService = (RemotePrintService) UserState.this.mActiveServices.get(printerId.getServiceName())) == null) {
                    return;
                }
                Handler.getMain().sendMessage(PooledLambda.obtainMessage(new TriConsumer() {
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        ((UserState.PrinterDiscoverySessionMediator) obj).handleStopPrinterStateTracking((RemotePrintService) obj2, (PrinterId) obj3);
                    }
                }, this, remotePrintService, printerId));
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
            int size = this.mStateTrackedPrinters.size();
            for (int i = 0; i < size; i++) {
                UserState.this.stopPrinterStateTracking(this.mStateTrackedPrinters.get(i));
            }
            int size2 = this.mStartedPrinterDiscoveryTokens.size();
            for (int i2 = 0; i2 < size2; i2++) {
                stopPrinterDiscoveryLocked(IPrinterDiscoveryObserver.Stub.asInterface(this.mStartedPrinterDiscoveryTokens.get(i2)));
            }
            Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
                @Override
                public final void accept(Object obj, Object obj2) {
                    ((UserState.PrinterDiscoverySessionMediator) obj).handleDispatchDestroyPrinterDiscoverySession((ArrayList) obj2);
                }
            }, this, new ArrayList(UserState.this.mActiveServices.values())));
        }

        public void onPrintersAddedLocked(List<PrinterInfo> list) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not adding printers - session destroyed");
                return;
            }
            ArrayList arrayList = null;
            int size = list.size();
            for (int i = 0; i < size; i++) {
                PrinterInfo printerInfo = list.get(i);
                PrinterInfo printerInfoPut = this.mPrinters.put(printerInfo.getId(), printerInfo);
                if (printerInfoPut == null || !printerInfoPut.equals(printerInfo)) {
                    if (arrayList == null) {
                        arrayList = new ArrayList();
                    }
                    arrayList.add(printerInfo);
                }
            }
            if (arrayList != null) {
                Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
                    @Override
                    public final void accept(Object obj, Object obj2) {
                        ((UserState.PrinterDiscoverySessionMediator) obj).handleDispatchPrintersAdded((List) obj2);
                    }
                }, this, arrayList));
            }
        }

        public void onPrintersRemovedLocked(List<PrinterId> list) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not removing printers - session destroyed");
                return;
            }
            ArrayList arrayList = null;
            int size = list.size();
            for (int i = 0; i < size; i++) {
                PrinterId printerId = list.get(i);
                if (this.mPrinters.remove(printerId) != null) {
                    if (arrayList == null) {
                        arrayList = new ArrayList();
                    }
                    arrayList.add(printerId);
                }
            }
            if (arrayList != null) {
                Handler.getMain().sendMessage(PooledLambda.obtainMessage($$Lambda$UserState$PrinterDiscoverySessionMediator$CjemUQP8s7wGdqpIggj9Oze6I.INSTANCE, this, arrayList));
            }
        }

        public void onServiceRemovedLocked(RemotePrintService remotePrintService) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not updating removed service - session destroyed");
            } else {
                removePrintersForServiceLocked(remotePrintService.getComponentName());
                remotePrintService.destroy();
            }
        }

        public void onCustomPrinterIconLoadedLocked(PrinterId printerId) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not updating printer - session destroyed");
                return;
            }
            PrinterInfo printerInfo = this.mPrinters.get(printerId);
            if (printerInfo != null) {
                PrinterInfo printerInfoBuild = new PrinterInfo.Builder(printerInfo).incCustomPrinterIconGen().build();
                this.mPrinters.put(printerId, printerInfoBuild);
                ArrayList arrayList = new ArrayList(1);
                arrayList.add(printerInfoBuild);
                Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
                    @Override
                    public final void accept(Object obj, Object obj2) {
                        ((UserState.PrinterDiscoverySessionMediator) obj).handleDispatchPrintersAdded((ArrayList) obj2);
                    }
                }, this, arrayList));
            }
        }

        public void onServiceDiedLocked(RemotePrintService remotePrintService) {
            UserState.this.removeServiceLocked(remotePrintService);
        }

        public void onServiceAddedLocked(RemotePrintService remotePrintService) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not updating added service - session destroyed");
                return;
            }
            Handler.getMain().sendMessage(PooledLambda.obtainMessage(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    ((RemotePrintService) obj).createPrinterDiscoverySession();
                }
            }, remotePrintService));
            if (!this.mStartedPrinterDiscoveryTokens.isEmpty()) {
                Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
                    @Override
                    public final void accept(Object obj, Object obj2) {
                        ((RemotePrintService) obj).startPrinterDiscovery((List) obj2);
                    }
                }, remotePrintService, (Object) null));
            }
            int size = this.mStateTrackedPrinters.size();
            for (int i = 0; i < size; i++) {
                PrinterId printerId = this.mStateTrackedPrinters.get(i);
                if (printerId.getServiceName().equals(remotePrintService.getComponentName())) {
                    Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
                        @Override
                        public final void accept(Object obj, Object obj2) {
                            ((RemotePrintService) obj).startPrinterStateTracking((PrinterId) obj2);
                        }
                    }, remotePrintService, printerId));
                }
            }
        }

        public void dumpLocked(DualDumpOutputStream dualDumpOutputStream) {
            dualDumpOutputStream.write("is_destroyed", 1133871366145L, UserState.this.mDestroyed);
            dualDumpOutputStream.write("is_printer_discovery_in_progress", 1133871366146L, !this.mStartedPrinterDiscoveryTokens.isEmpty());
            int iBeginBroadcast = this.mDiscoveryObservers.beginBroadcast();
            for (int i = 0; i < iBeginBroadcast; i++) {
                dualDumpOutputStream.write("printer_discovery_observers", 2237677961219L, this.mDiscoveryObservers.getBroadcastItem(i).toString());
            }
            this.mDiscoveryObservers.finishBroadcast();
            int size = this.mStartedPrinterDiscoveryTokens.size();
            for (int i2 = 0; i2 < size; i2++) {
                dualDumpOutputStream.write("discovery_requests", 2237677961220L, this.mStartedPrinterDiscoveryTokens.get(i2).toString());
            }
            int size2 = this.mStateTrackedPrinters.size();
            for (int i3 = 0; i3 < size2; i3++) {
                com.android.internal.print.DumpUtils.writePrinterId(dualDumpOutputStream, "tracked_printer_requests", 2246267895813L, this.mStateTrackedPrinters.get(i3));
            }
            int size3 = this.mPrinters.size();
            for (int i4 = 0; i4 < size3; i4++) {
                com.android.internal.print.DumpUtils.writePrinterInfo(UserState.this.mContext, dualDumpOutputStream, "printer", 2246267895814L, this.mPrinters.valueAt(i4));
            }
        }

        private void removePrintersForServiceLocked(ComponentName componentName) {
            if (this.mPrinters.isEmpty()) {
                return;
            }
            int size = this.mPrinters.size();
            ArrayList arrayList = null;
            for (int i = 0; i < size; i++) {
                PrinterId printerIdKeyAt = this.mPrinters.keyAt(i);
                if (printerIdKeyAt.getServiceName().equals(componentName)) {
                    if (arrayList == null) {
                        arrayList = new ArrayList();
                    }
                    arrayList.add(printerIdKeyAt);
                }
            }
            if (arrayList != null) {
                int size2 = arrayList.size();
                for (int i2 = 0; i2 < size2; i2++) {
                    this.mPrinters.remove(arrayList.get(i2));
                }
                Handler.getMain().sendMessage(PooledLambda.obtainMessage($$Lambda$UserState$PrinterDiscoverySessionMediator$CjemUQP8s7wGdqpIggj9Oze6I.INSTANCE, this, arrayList));
            }
        }

        private void handleDispatchPrintersAdded(List<PrinterInfo> list) {
            int iBeginBroadcast = this.mDiscoveryObservers.beginBroadcast();
            for (int i = 0; i < iBeginBroadcast; i++) {
                handlePrintersAdded((IPrinterDiscoveryObserver) this.mDiscoveryObservers.getBroadcastItem(i), list);
            }
            this.mDiscoveryObservers.finishBroadcast();
        }

        private void handleDispatchPrintersRemoved(List<PrinterId> list) {
            int iBeginBroadcast = this.mDiscoveryObservers.beginBroadcast();
            for (int i = 0; i < iBeginBroadcast; i++) {
                handlePrintersRemoved((IPrinterDiscoveryObserver) this.mDiscoveryObservers.getBroadcastItem(i), list);
            }
            this.mDiscoveryObservers.finishBroadcast();
        }

        private void handleDispatchCreatePrinterDiscoverySession(List<RemotePrintService> list) {
            int size = list.size();
            for (int i = 0; i < size; i++) {
                list.get(i).createPrinterDiscoverySession();
            }
        }

        private void handleDispatchDestroyPrinterDiscoverySession(List<RemotePrintService> list) {
            int size = list.size();
            for (int i = 0; i < size; i++) {
                list.get(i).destroyPrinterDiscoverySession();
            }
            onDestroyed();
        }

        private void handleDispatchStartPrinterDiscovery(List<RemotePrintService> list, List<PrinterId> list2) {
            int size = list.size();
            for (int i = 0; i < size; i++) {
                list.get(i).startPrinterDiscovery(list2);
            }
        }

        private void handleDispatchStopPrinterDiscovery(List<RemotePrintService> list) {
            int size = list.size();
            for (int i = 0; i < size; i++) {
                list.get(i).stopPrinterDiscovery();
            }
        }

        private void handleValidatePrinters(RemotePrintService remotePrintService, List<PrinterId> list) {
            remotePrintService.validatePrinters(list);
        }

        private void handleStartPrinterStateTracking(RemotePrintService remotePrintService, PrinterId printerId) {
            remotePrintService.startPrinterStateTracking(printerId);
        }

        private void handleStopPrinterStateTracking(RemotePrintService remotePrintService, PrinterId printerId) {
            remotePrintService.stopPrinterStateTracking(printerId);
        }

        private void handlePrintersAdded(IPrinterDiscoveryObserver iPrinterDiscoveryObserver, List<PrinterInfo> list) {
            try {
                iPrinterDiscoveryObserver.onPrintersAdded(new ParceledListSlice(list));
            } catch (RemoteException e) {
                Log.e(UserState.LOG_TAG, "Error sending added printers", e);
            }
        }

        private void handlePrintersRemoved(IPrinterDiscoveryObserver iPrinterDiscoveryObserver, List<PrinterId> list) {
            try {
                iPrinterDiscoveryObserver.onPrintersRemoved(new ParceledListSlice(list));
            } catch (RemoteException e) {
                Log.e(UserState.LOG_TAG, "Error sending removed printers", e);
            }
        }
    }

    private final class PrintJobForAppCache {
        private final SparseArray<List<PrintJobInfo>> mPrintJobsForRunningApp;

        private PrintJobForAppCache() {
            this.mPrintJobsForRunningApp = new SparseArray<>();
        }

        public boolean onPrintJobCreated(final IBinder iBinder, final int i, PrintJobInfo printJobInfo) {
            try {
                iBinder.linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        iBinder.unlinkToDeath(this, 0);
                        synchronized (UserState.this.mLock) {
                            PrintJobForAppCache.this.mPrintJobsForRunningApp.remove(i);
                        }
                    }
                }, 0);
                synchronized (UserState.this.mLock) {
                    List<PrintJobInfo> arrayList = this.mPrintJobsForRunningApp.get(i);
                    if (arrayList == null) {
                        arrayList = new ArrayList<>();
                        this.mPrintJobsForRunningApp.put(i, arrayList);
                    }
                    arrayList.add(printJobInfo);
                }
                return true;
            } catch (RemoteException e) {
                return false;
            }
        }

        public void onPrintJobStateChanged(PrintJobInfo printJobInfo) {
            synchronized (UserState.this.mLock) {
                List<PrintJobInfo> list = this.mPrintJobsForRunningApp.get(printJobInfo.getAppId());
                if (list == null) {
                    return;
                }
                int size = list.size();
                for (int i = 0; i < size; i++) {
                    if (list.get(i).getId().equals(printJobInfo.getId())) {
                        list.set(i, printJobInfo);
                    }
                }
            }
        }

        public PrintJobInfo getPrintJob(PrintJobId printJobId, int i) {
            synchronized (UserState.this.mLock) {
                List<PrintJobInfo> list = this.mPrintJobsForRunningApp.get(i);
                if (list == null) {
                    return null;
                }
                int size = list.size();
                for (int i2 = 0; i2 < size; i2++) {
                    PrintJobInfo printJobInfo = list.get(i2);
                    if (printJobInfo.getId().equals(printJobId)) {
                        return printJobInfo;
                    }
                }
                return null;
            }
        }

        public List<PrintJobInfo> getPrintJobs(int i) {
            synchronized (UserState.this.mLock) {
                ArrayList arrayList = null;
                try {
                    if (i == -2) {
                        int size = this.mPrintJobsForRunningApp.size();
                        for (int i2 = 0; i2 < size; i2++) {
                            List<PrintJobInfo> listValueAt = this.mPrintJobsForRunningApp.valueAt(i2);
                            if (arrayList == null) {
                                arrayList = new ArrayList();
                            }
                            arrayList.addAll(listValueAt);
                        }
                    } else {
                        List<PrintJobInfo> list = this.mPrintJobsForRunningApp.get(i);
                        if (list != null) {
                            arrayList = new ArrayList();
                            arrayList.addAll(list);
                        }
                    }
                    if (arrayList != null) {
                        return arrayList;
                    }
                    return Collections.emptyList();
                } finally {
                }
            }
        }

        public void dumpLocked(DualDumpOutputStream dualDumpOutputStream) {
            int size = this.mPrintJobsForRunningApp.size();
            int i = 0;
            while (i < size) {
                int iKeyAt = this.mPrintJobsForRunningApp.keyAt(i);
                List<PrintJobInfo> listValueAt = this.mPrintJobsForRunningApp.valueAt(i);
                int size2 = listValueAt.size();
                int i2 = 0;
                while (i2 < size2) {
                    long jStart = dualDumpOutputStream.start("cached_print_jobs", 2246267895813L);
                    dualDumpOutputStream.write("app_id", 1120986464257L, iKeyAt);
                    com.android.internal.print.DumpUtils.writePrintJobInfo(UserState.this.mContext, dualDumpOutputStream, "print_job", 1146756268034L, listValueAt.get(i2));
                    dualDumpOutputStream.end(jStart);
                    i2++;
                    i = i;
                }
                i++;
            }
        }
    }
}
