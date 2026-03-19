package com.android.server.print;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ParceledListSlice;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.UserHandle;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.IPrintService;
import android.printservice.IPrintServiceClient;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.internal.util.dump.DumpUtils;
import com.android.internal.util.function.pooled.PooledLambda;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class RemotePrintService implements IBinder.DeathRecipient {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "RemotePrintService";
    private boolean mBinding;
    private final PrintServiceCallbacks mCallbacks;
    private final ComponentName mComponentName;
    private final Context mContext;
    private boolean mDestroyed;
    private List<PrinterId> mDiscoveryPriorityList;
    private boolean mHasActivePrintJobs;
    private boolean mHasPrinterDiscoverySession;
    private final Intent mIntent;
    private IPrintService mPrintService;
    private boolean mServiceDied;
    private final RemotePrintSpooler mSpooler;

    @GuardedBy("mLock")
    private List<PrinterId> mTrackedPrinterList;
    private final int mUserId;
    private final Object mLock = new Object();
    private final List<Runnable> mPendingCommands = new ArrayList();
    private final ServiceConnection mServiceConnection = new RemoteServiceConneciton();
    private final RemotePrintServiceClient mPrintServiceClient = new RemotePrintServiceClient(this);

    public interface PrintServiceCallbacks {
        void onCustomPrinterIconLoaded(PrinterId printerId, Icon icon);

        void onPrintersAdded(List<PrinterInfo> list);

        void onPrintersRemoved(List<PrinterId> list);

        void onServiceDied(RemotePrintService remotePrintService);
    }

    public RemotePrintService(Context context, ComponentName componentName, int i, RemotePrintSpooler remotePrintSpooler, PrintServiceCallbacks printServiceCallbacks) {
        this.mContext = context;
        this.mCallbacks = printServiceCallbacks;
        this.mComponentName = componentName;
        this.mIntent = new Intent().setComponent(this.mComponentName);
        this.mUserId = i;
        this.mSpooler = remotePrintSpooler;
    }

    public ComponentName getComponentName() {
        return this.mComponentName;
    }

    public void destroy() {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((RemotePrintService) obj).handleDestroy();
            }
        }, this));
    }

    private void handleDestroy() {
        stopTrackingAllPrinters();
        if (this.mDiscoveryPriorityList != null) {
            handleStopPrinterDiscovery();
        }
        if (this.mHasPrinterDiscoverySession) {
            handleDestroyPrinterDiscoverySession();
        }
        ensureUnbound();
        this.mDestroyed = true;
    }

    @Override
    public void binderDied() {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((RemotePrintService) obj).handleBinderDied();
            }
        }, this));
    }

    private void handleBinderDied() {
        if (this.mPrintService != null) {
            this.mPrintService.asBinder().unlinkToDeath(this, 0);
        }
        this.mPrintService = null;
        this.mServiceDied = true;
        this.mCallbacks.onServiceDied(this);
    }

    public void onAllPrintJobsHandled() {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((RemotePrintService) obj).handleOnAllPrintJobsHandled();
            }
        }, this));
    }

    private void handleOnAllPrintJobsHandled() {
        this.mHasActivePrintJobs = false;
        if (!isBound()) {
            if (this.mServiceDied && !this.mHasPrinterDiscoverySession) {
                ensureUnbound();
                return;
            } else {
                ensureBound();
                this.mPendingCommands.add(new Runnable() {
                    @Override
                    public void run() {
                        RemotePrintService.this.handleOnAllPrintJobsHandled();
                    }
                });
                return;
            }
        }
        if (!this.mHasPrinterDiscoverySession) {
            ensureUnbound();
        }
    }

    public void onRequestCancelPrintJob(PrintJobInfo printJobInfo) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((RemotePrintService) obj).handleRequestCancelPrintJob((PrintJobInfo) obj2);
            }
        }, this, printJobInfo));
    }

    private void handleRequestCancelPrintJob(final PrintJobInfo printJobInfo) {
        if (!isBound()) {
            ensureBound();
            this.mPendingCommands.add(new Runnable() {
                @Override
                public void run() {
                    RemotePrintService.this.handleRequestCancelPrintJob(printJobInfo);
                }
            });
        } else {
            try {
                this.mPrintService.requestCancelPrintJob(printJobInfo);
            } catch (RemoteException e) {
                Slog.e(LOG_TAG, "Error canceling a pring job.", e);
            }
        }
    }

    public void onPrintJobQueued(PrintJobInfo printJobInfo) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((RemotePrintService) obj).handleOnPrintJobQueued((PrintJobInfo) obj2);
            }
        }, this, printJobInfo));
    }

    private void handleOnPrintJobQueued(final PrintJobInfo printJobInfo) {
        this.mHasActivePrintJobs = true;
        if (!isBound()) {
            ensureBound();
            this.mPendingCommands.add(new Runnable() {
                @Override
                public void run() {
                    RemotePrintService.this.handleOnPrintJobQueued(printJobInfo);
                }
            });
        } else {
            try {
                this.mPrintService.onPrintJobQueued(printJobInfo);
            } catch (RemoteException e) {
                Slog.e(LOG_TAG, "Error announcing queued pring job.", e);
            }
        }
    }

    public void createPrinterDiscoverySession() {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((RemotePrintService) obj).handleCreatePrinterDiscoverySession();
            }
        }, this));
    }

    private void handleCreatePrinterDiscoverySession() {
        this.mHasPrinterDiscoverySession = true;
        if (!isBound()) {
            ensureBound();
            this.mPendingCommands.add(new Runnable() {
                @Override
                public void run() {
                    RemotePrintService.this.handleCreatePrinterDiscoverySession();
                }
            });
        } else {
            try {
                this.mPrintService.createPrinterDiscoverySession();
            } catch (RemoteException e) {
                Slog.e(LOG_TAG, "Error creating printer discovery session.", e);
            }
        }
    }

    public void destroyPrinterDiscoverySession() {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((RemotePrintService) obj).handleDestroyPrinterDiscoverySession();
            }
        }, this));
    }

    private void handleDestroyPrinterDiscoverySession() {
        this.mHasPrinterDiscoverySession = false;
        if (!isBound()) {
            if (this.mServiceDied && !this.mHasActivePrintJobs) {
                ensureUnbound();
                return;
            } else {
                ensureBound();
                this.mPendingCommands.add(new Runnable() {
                    @Override
                    public void run() {
                        RemotePrintService.this.handleDestroyPrinterDiscoverySession();
                    }
                });
                return;
            }
        }
        try {
            this.mPrintService.destroyPrinterDiscoverySession();
        } catch (RemoteException e) {
            Slog.e(LOG_TAG, "Error destroying printer dicovery session.", e);
        }
        if (!this.mHasActivePrintJobs) {
            ensureUnbound();
        }
    }

    public void startPrinterDiscovery(List<PrinterId> list) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((RemotePrintService) obj).handleStartPrinterDiscovery((List) obj2);
            }
        }, this, list));
    }

    private void handleStartPrinterDiscovery(final List<PrinterId> list) {
        this.mDiscoveryPriorityList = new ArrayList();
        if (list != null) {
            this.mDiscoveryPriorityList.addAll(list);
        }
        if (!isBound()) {
            ensureBound();
            this.mPendingCommands.add(new Runnable() {
                @Override
                public void run() {
                    RemotePrintService.this.handleStartPrinterDiscovery(list);
                }
            });
        } else {
            try {
                this.mPrintService.startPrinterDiscovery(list);
            } catch (RemoteException e) {
                Slog.e(LOG_TAG, "Error starting printer dicovery.", e);
            }
        }
    }

    public void stopPrinterDiscovery() {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((RemotePrintService) obj).handleStopPrinterDiscovery();
            }
        }, this));
    }

    private void handleStopPrinterDiscovery() {
        this.mDiscoveryPriorityList = null;
        if (!isBound()) {
            ensureBound();
            this.mPendingCommands.add(new Runnable() {
                @Override
                public void run() {
                    RemotePrintService.this.handleStopPrinterDiscovery();
                }
            });
            return;
        }
        stopTrackingAllPrinters();
        try {
            this.mPrintService.stopPrinterDiscovery();
        } catch (RemoteException e) {
            Slog.e(LOG_TAG, "Error stopping printer discovery.", e);
        }
    }

    public void validatePrinters(List<PrinterId> list) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((RemotePrintService) obj).handleValidatePrinters((List) obj2);
            }
        }, this, list));
    }

    private void handleValidatePrinters(final List<PrinterId> list) {
        if (!isBound()) {
            ensureBound();
            this.mPendingCommands.add(new Runnable() {
                @Override
                public void run() {
                    RemotePrintService.this.handleValidatePrinters(list);
                }
            });
        } else {
            try {
                this.mPrintService.validatePrinters(list);
            } catch (RemoteException e) {
                Slog.e(LOG_TAG, "Error requesting printers validation.", e);
            }
        }
    }

    public void startPrinterStateTracking(PrinterId printerId) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((RemotePrintService) obj).handleStartPrinterStateTracking((PrinterId) obj2);
            }
        }, this, printerId));
    }

    public void requestCustomPrinterIcon(PrinterId printerId) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((RemotePrintService) obj).handleRequestCustomPrinterIcon((PrinterId) obj2);
            }
        }, this, printerId));
    }

    private void handleRequestCustomPrinterIcon(final PrinterId printerId) {
        if (!isBound()) {
            ensureBound();
            this.mPendingCommands.add(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.handleRequestCustomPrinterIcon(printerId);
                }
            });
            return;
        }
        try {
            this.mPrintService.requestCustomPrinterIcon(printerId);
        } catch (RemoteException e) {
            Slog.e(LOG_TAG, "Error requesting icon for " + printerId, e);
        }
    }

    private void handleStartPrinterStateTracking(final PrinterId printerId) {
        synchronized (this.mLock) {
            if (this.mTrackedPrinterList == null) {
                this.mTrackedPrinterList = new ArrayList();
            }
            this.mTrackedPrinterList.add(printerId);
        }
        if (!isBound()) {
            ensureBound();
            this.mPendingCommands.add(new Runnable() {
                @Override
                public void run() {
                    RemotePrintService.this.handleStartPrinterStateTracking(printerId);
                }
            });
        } else {
            try {
                this.mPrintService.startPrinterStateTracking(printerId);
            } catch (RemoteException e) {
                Slog.e(LOG_TAG, "Error requesting start printer tracking.", e);
            }
        }
    }

    public void stopPrinterStateTracking(PrinterId printerId) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((RemotePrintService) obj).handleStopPrinterStateTracking((PrinterId) obj2);
            }
        }, this, printerId));
    }

    private void handleStopPrinterStateTracking(final PrinterId printerId) {
        synchronized (this.mLock) {
            if (this.mTrackedPrinterList != null && this.mTrackedPrinterList.remove(printerId)) {
                if (this.mTrackedPrinterList.isEmpty()) {
                    this.mTrackedPrinterList = null;
                }
                if (!isBound()) {
                    ensureBound();
                    this.mPendingCommands.add(new Runnable() {
                        @Override
                        public void run() {
                            RemotePrintService.this.handleStopPrinterStateTracking(printerId);
                        }
                    });
                } else {
                    try {
                        this.mPrintService.stopPrinterStateTracking(printerId);
                    } catch (RemoteException e) {
                        Slog.e(LOG_TAG, "Error requesting stop printer tracking.", e);
                    }
                }
            }
        }
    }

    private void stopTrackingAllPrinters() {
        synchronized (this.mLock) {
            if (this.mTrackedPrinterList == null) {
                return;
            }
            for (int size = this.mTrackedPrinterList.size() - 1; size >= 0; size--) {
                PrinterId printerId = this.mTrackedPrinterList.get(size);
                if (printerId.getServiceName().equals(this.mComponentName)) {
                    handleStopPrinterStateTracking(printerId);
                }
            }
        }
    }

    public void dump(DualDumpOutputStream dualDumpOutputStream) {
        DumpUtils.writeComponentName(dualDumpOutputStream, "component_name", 1146756268033L, this.mComponentName);
        dualDumpOutputStream.write("is_destroyed", 1133871366146L, this.mDestroyed);
        dualDumpOutputStream.write("is_bound", 1133871366147L, isBound());
        dualDumpOutputStream.write("has_discovery_session", 1133871366148L, this.mHasPrinterDiscoverySession);
        dualDumpOutputStream.write("has_active_print_jobs", 1133871366149L, this.mHasActivePrintJobs);
        dualDumpOutputStream.write("is_discovering_printers", 1133871366150L, this.mDiscoveryPriorityList != null);
        synchronized (this.mLock) {
            if (this.mTrackedPrinterList != null) {
                int size = this.mTrackedPrinterList.size();
                for (int i = 0; i < size; i++) {
                    com.android.internal.print.DumpUtils.writePrinterId(dualDumpOutputStream, "tracked_printers", 2246267895815L, this.mTrackedPrinterList.get(i));
                }
            }
        }
    }

    private boolean isBound() {
        return this.mPrintService != null;
    }

    private void ensureBound() {
        if (isBound() || this.mBinding) {
            return;
        }
        this.mBinding = true;
        if (!this.mContext.bindServiceAsUser(this.mIntent, this.mServiceConnection, 71303169, new UserHandle(this.mUserId))) {
            this.mBinding = false;
            if (!this.mServiceDied) {
                handleBinderDied();
            }
        }
    }

    private void ensureUnbound() {
        if (!isBound() && !this.mBinding) {
            return;
        }
        this.mBinding = false;
        this.mPendingCommands.clear();
        this.mHasActivePrintJobs = false;
        this.mHasPrinterDiscoverySession = false;
        this.mDiscoveryPriorityList = null;
        synchronized (this.mLock) {
            this.mTrackedPrinterList = null;
        }
        if (isBound()) {
            try {
                this.mPrintService.setClient((IPrintServiceClient) null);
            } catch (RemoteException e) {
            }
            this.mPrintService.asBinder().unlinkToDeath(this, 0);
            this.mPrintService = null;
            this.mContext.unbindService(this.mServiceConnection);
        }
    }

    private class RemoteServiceConneciton implements ServiceConnection {
        private RemoteServiceConneciton() {
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (RemotePrintService.this.mDestroyed || !RemotePrintService.this.mBinding) {
                RemotePrintService.this.mContext.unbindService(RemotePrintService.this.mServiceConnection);
                return;
            }
            RemotePrintService.this.mBinding = false;
            RemotePrintService.this.mPrintService = IPrintService.Stub.asInterface(iBinder);
            try {
                iBinder.linkToDeath(RemotePrintService.this, 0);
                try {
                    RemotePrintService.this.mPrintService.setClient(RemotePrintService.this.mPrintServiceClient);
                    if (RemotePrintService.this.mServiceDied && RemotePrintService.this.mHasPrinterDiscoverySession) {
                        RemotePrintService.this.handleCreatePrinterDiscoverySession();
                    }
                    if (RemotePrintService.this.mServiceDied && RemotePrintService.this.mDiscoveryPriorityList != null) {
                        RemotePrintService.this.handleStartPrinterDiscovery(RemotePrintService.this.mDiscoveryPriorityList);
                    }
                    synchronized (RemotePrintService.this.mLock) {
                        if (RemotePrintService.this.mServiceDied && RemotePrintService.this.mTrackedPrinterList != null) {
                            int size = RemotePrintService.this.mTrackedPrinterList.size();
                            for (int i = 0; i < size; i++) {
                                RemotePrintService.this.handleStartPrinterStateTracking((PrinterId) RemotePrintService.this.mTrackedPrinterList.get(i));
                            }
                        }
                    }
                    while (!RemotePrintService.this.mPendingCommands.isEmpty()) {
                        ((Runnable) RemotePrintService.this.mPendingCommands.remove(0)).run();
                    }
                    if (!RemotePrintService.this.mHasPrinterDiscoverySession && !RemotePrintService.this.mHasActivePrintJobs) {
                        RemotePrintService.this.ensureUnbound();
                    }
                    RemotePrintService.this.mServiceDied = false;
                } catch (RemoteException e) {
                    Slog.e(RemotePrintService.LOG_TAG, "Error setting client for: " + iBinder, e);
                    RemotePrintService.this.handleBinderDied();
                }
            } catch (RemoteException e2) {
                RemotePrintService.this.handleBinderDied();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            RemotePrintService.this.mBinding = true;
        }
    }

    private static final class RemotePrintServiceClient extends IPrintServiceClient.Stub {
        private final WeakReference<RemotePrintService> mWeakService;

        public RemotePrintServiceClient(RemotePrintService remotePrintService) {
            this.mWeakService = new WeakReference<>(remotePrintService);
        }

        public List<PrintJobInfo> getPrintJobInfos() {
            RemotePrintService remotePrintService = this.mWeakService.get();
            if (remotePrintService != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return remotePrintService.mSpooler.getPrintJobInfos(remotePrintService.mComponentName, -4, -2);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            return null;
        }

        public PrintJobInfo getPrintJobInfo(PrintJobId printJobId) {
            RemotePrintService remotePrintService = this.mWeakService.get();
            if (remotePrintService != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return remotePrintService.mSpooler.getPrintJobInfo(printJobId, -2);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            return null;
        }

        public boolean setPrintJobState(PrintJobId printJobId, int i, String str) {
            RemotePrintService remotePrintService = this.mWeakService.get();
            if (remotePrintService != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return remotePrintService.mSpooler.setPrintJobState(printJobId, i, str);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            return false;
        }

        public boolean setPrintJobTag(PrintJobId printJobId, String str) {
            RemotePrintService remotePrintService = this.mWeakService.get();
            if (remotePrintService != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return remotePrintService.mSpooler.setPrintJobTag(printJobId, str);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            return false;
        }

        public void writePrintJobData(ParcelFileDescriptor parcelFileDescriptor, PrintJobId printJobId) {
            RemotePrintService remotePrintService = this.mWeakService.get();
            if (remotePrintService != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    remotePrintService.mSpooler.writePrintJobData(parcelFileDescriptor, printJobId);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void setProgress(PrintJobId printJobId, float f) {
            RemotePrintService remotePrintService = this.mWeakService.get();
            if (remotePrintService != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    remotePrintService.mSpooler.setProgress(printJobId, f);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void setStatus(PrintJobId printJobId, CharSequence charSequence) {
            RemotePrintService remotePrintService = this.mWeakService.get();
            if (remotePrintService != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    remotePrintService.mSpooler.setStatus(printJobId, charSequence);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void setStatusRes(PrintJobId printJobId, int i, CharSequence charSequence) {
            RemotePrintService remotePrintService = this.mWeakService.get();
            if (remotePrintService != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    remotePrintService.mSpooler.setStatus(printJobId, i, charSequence);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void onPrintersAdded(ParceledListSlice parceledListSlice) {
            RemotePrintService remotePrintService = this.mWeakService.get();
            if (remotePrintService != null) {
                List<PrinterInfo> list = parceledListSlice.getList();
                throwIfPrinterIdsForPrinterInfoTampered(remotePrintService.mComponentName, list);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    remotePrintService.mCallbacks.onPrintersAdded(list);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void onPrintersRemoved(ParceledListSlice parceledListSlice) {
            RemotePrintService remotePrintService = this.mWeakService.get();
            if (remotePrintService != null) {
                List<PrinterId> list = parceledListSlice.getList();
                throwIfPrinterIdsTampered(remotePrintService.mComponentName, list);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    remotePrintService.mCallbacks.onPrintersRemoved(list);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        private void throwIfPrinterIdsForPrinterInfoTampered(ComponentName componentName, List<PrinterInfo> list) {
            int size = list.size();
            for (int i = 0; i < size; i++) {
                throwIfPrinterIdTampered(componentName, list.get(i).getId());
            }
        }

        private void throwIfPrinterIdsTampered(ComponentName componentName, List<PrinterId> list) {
            int size = list.size();
            for (int i = 0; i < size; i++) {
                throwIfPrinterIdTampered(componentName, list.get(i));
            }
        }

        private void throwIfPrinterIdTampered(ComponentName componentName, PrinterId printerId) {
            if (printerId == null || !printerId.getServiceName().equals(componentName)) {
                throw new IllegalArgumentException("Invalid printer id: " + printerId);
            }
        }

        public void onCustomPrinterIconLoaded(PrinterId printerId, Icon icon) throws RemoteException {
            RemotePrintService remotePrintService = this.mWeakService.get();
            if (remotePrintService != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    remotePrintService.mCallbacks.onCustomPrinterIconLoaded(printerId, icon);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }
    }
}
