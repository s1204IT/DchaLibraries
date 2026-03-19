package com.android.server.print;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.print.IPrintSpooler;
import android.print.IPrintSpoolerCallbacks;
import android.print.IPrintSpoolerClient;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrinterId;
import android.util.Slog;
import android.util.TimedRemoteCaller;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.TransferPipe;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.job.controllers.JobStatus;
import com.android.server.utils.PriorityDump;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.TimeoutException;
import libcore.io.IoUtils;

final class RemotePrintSpooler {
    private static final long BIND_SPOOLER_SERVICE_TIMEOUT;
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "RemotePrintSpooler";
    private final PrintSpoolerCallbacks mCallbacks;
    private boolean mCanUnbind;
    private final Context mContext;
    private boolean mDestroyed;

    @GuardedBy("mLock")
    private boolean mIsBinding;
    private boolean mIsLowPriority;
    private IPrintSpooler mRemoteInstance;
    private final UserHandle mUserHandle;
    private final Object mLock = new Object();
    private final GetPrintJobInfosCaller mGetPrintJobInfosCaller = new GetPrintJobInfosCaller();
    private final GetPrintJobInfoCaller mGetPrintJobInfoCaller = new GetPrintJobInfoCaller();
    private final SetPrintJobStateCaller mSetPrintJobStatusCaller = new SetPrintJobStateCaller();
    private final SetPrintJobTagCaller mSetPrintJobTagCaller = new SetPrintJobTagCaller();
    private final OnCustomPrinterIconLoadedCaller mCustomPrinterIconLoadedCaller = new OnCustomPrinterIconLoadedCaller();
    private final ClearCustomPrinterIconCacheCaller mClearCustomPrinterIconCache = new ClearCustomPrinterIconCacheCaller();
    private final GetCustomPrinterIconCaller mGetCustomPrinterIconCaller = new GetCustomPrinterIconCaller();
    private final ServiceConnection mServiceConnection = new MyServiceConnection();
    private final PrintSpoolerClient mClient = new PrintSpoolerClient(this);
    private final Intent mIntent = new Intent();

    public interface PrintSpoolerCallbacks {
        void onAllPrintJobsForServiceHandled(ComponentName componentName);

        void onPrintJobQueued(PrintJobInfo printJobInfo);

        void onPrintJobStateChanged(PrintJobInfo printJobInfo);
    }

    static {
        BIND_SPOOLER_SERVICE_TIMEOUT = Build.IS_ENG ? JobStatus.DEFAULT_TRIGGER_MAX_DELAY : JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
    }

    public RemotePrintSpooler(Context context, int i, boolean z, PrintSpoolerCallbacks printSpoolerCallbacks) {
        this.mContext = context;
        this.mUserHandle = new UserHandle(i);
        this.mCallbacks = printSpoolerCallbacks;
        this.mIsLowPriority = z;
        this.mIntent.setComponent(new ComponentName("com.android.printspooler", "com.android.printspooler.model.PrintSpoolerService"));
    }

    public void increasePriority() {
        if (this.mIsLowPriority) {
            this.mIsLowPriority = false;
            synchronized (this.mLock) {
                throwIfDestroyedLocked();
                while (!this.mCanUnbind) {
                    try {
                        this.mLock.wait();
                    } catch (InterruptedException e) {
                        Slog.e(LOG_TAG, "Interrupted while waiting for operation to complete");
                    }
                }
                unbindLocked();
            }
        }
    }

    public final List<PrintJobInfo> getPrintJobInfos(ComponentName componentName, int i, int i2) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                List<PrintJobInfo> printJobInfos = this.mGetPrintJobInfosCaller.getPrintJobInfos(getRemoteInstanceLazy(), componentName, i, i2);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
                return printJobInfos;
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error getting print jobs.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    return null;
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final void createPrintJob(PrintJobInfo printJobInfo) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        boolean z = 1;
        z = 1;
        try {
            try {
                getRemoteInstanceLazy().createPrintJob(printJobInfo);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    Object obj = this.mLock;
                    obj.notifyAll();
                    z = obj;
                }
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error creating print job.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    Object obj2 = this.mLock;
                    obj2.notifyAll();
                    z = obj2;
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = z;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final void writePrintJobData(ParcelFileDescriptor parcelFileDescriptor, PrintJobId printJobId) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                getRemoteInstanceLazy().writePrintJobData(parcelFileDescriptor, printJobId);
                IoUtils.closeQuietly(parcelFileDescriptor);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error writing print job data.", e);
                IoUtils.closeQuietly(parcelFileDescriptor);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        } catch (Throwable th) {
            IoUtils.closeQuietly(parcelFileDescriptor);
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final PrintJobInfo getPrintJobInfo(PrintJobId printJobId, int i) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                PrintJobInfo printJobInfo = this.mGetPrintJobInfoCaller.getPrintJobInfo(getRemoteInstanceLazy(), printJobId, i);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
                return printJobInfo;
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error getting print job info.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    return null;
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final boolean setPrintJobState(PrintJobId printJobId, int i, String str) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                boolean printJobState = this.mSetPrintJobStatusCaller.setPrintJobState(getRemoteInstanceLazy(), printJobId, i, str);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
                return printJobState;
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error setting print job state.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    return false;
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final void setProgress(PrintJobId printJobId, float f) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                getRemoteInstanceLazy().setProgress(printJobId, f);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error setting progress.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final void setStatus(PrintJobId printJobId, CharSequence charSequence) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                getRemoteInstanceLazy().setStatus(printJobId, charSequence);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error setting status.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final void setStatus(PrintJobId printJobId, int i, CharSequence charSequence) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                getRemoteInstanceLazy().setStatusRes(printJobId, i, charSequence);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error setting status.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final void onCustomPrinterIconLoaded(PrinterId printerId, Icon icon) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                this.mCustomPrinterIconLoadedCaller.onCustomPrinterIconLoaded(getRemoteInstanceLazy(), printerId, icon);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error loading new custom printer icon.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final Icon getCustomPrinterIcon(PrinterId printerId) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                Icon customPrinterIcon = this.mGetCustomPrinterIconCaller.getCustomPrinterIcon(getRemoteInstanceLazy(), printerId);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
                return customPrinterIcon;
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error getting custom printer icon.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    return null;
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public void clearCustomPrinterIconCache() {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                this.mClearCustomPrinterIconCache.clearCustomPrinterIconCache(getRemoteInstanceLazy());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error clearing custom printer icon cache.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final boolean setPrintJobTag(PrintJobId printJobId, String str) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                boolean printJobTag = this.mSetPrintJobTagCaller.setPrintJobTag(getRemoteInstanceLazy(), printJobId, str);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
                return printJobTag;
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error setting print job tag.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    return false;
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final void setPrintJobCancelling(PrintJobId printJobId, boolean z) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                getRemoteInstanceLazy().setPrintJobCancelling(printJobId, z);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error setting print job cancelling.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final void pruneApprovedPrintServices(List<ComponentName> list) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        boolean z = 1;
        z = 1;
        try {
            try {
                getRemoteInstanceLazy().pruneApprovedPrintServices(list);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    Object obj = this.mLock;
                    obj.notifyAll();
                    z = obj;
                }
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error pruning approved print services.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    Object obj2 = this.mLock;
                    obj2.notifyAll();
                    z = obj2;
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = z;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final void removeObsoletePrintJobs() {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        boolean z = 1;
        z = 1;
        try {
            try {
                getRemoteInstanceLazy().removeObsoletePrintJobs();
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    Object obj = this.mLock;
                    obj.notifyAll();
                    z = obj;
                }
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error removing obsolete print jobs .", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    Object obj2 = this.mLock;
                    obj2.notifyAll();
                    z = obj2;
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = z;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final void destroy() {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            unbindLocked();
            this.mDestroyed = true;
            this.mCanUnbind = false;
        }
    }

    public void dump(DualDumpOutputStream dualDumpOutputStream) {
        synchronized (this.mLock) {
            dualDumpOutputStream.write("is_destroyed", 1133871366145L, this.mDestroyed);
            dualDumpOutputStream.write("is_bound", 1133871366146L, this.mRemoteInstance != null);
        }
        try {
            if (dualDumpOutputStream.isProto()) {
                dualDumpOutputStream.write((String) null, 1146756268035L, TransferPipe.dumpAsync(getRemoteInstanceLazy().asBinder(), new String[]{PriorityDump.PROTO_ARG}));
            } else {
                dualDumpOutputStream.writeNested("internal_state", TransferPipe.dumpAsync(getRemoteInstanceLazy().asBinder(), new String[0]));
            }
        } catch (RemoteException | IOException | InterruptedException | TimeoutException e) {
            Slog.e(LOG_TAG, "Failed to dump remote instance", e);
        }
    }

    private void onAllPrintJobsHandled() {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            unbindLocked();
        }
    }

    private void onPrintJobStateChanged(PrintJobInfo printJobInfo) {
        this.mCallbacks.onPrintJobStateChanged(printJobInfo);
    }

    private IPrintSpooler getRemoteInstanceLazy() throws InterruptedException, TimeoutException {
        synchronized (this.mLock) {
            if (this.mRemoteInstance != null) {
                return this.mRemoteInstance;
            }
            bindLocked();
            return this.mRemoteInstance;
        }
    }

    @GuardedBy("mLock")
    private void bindLocked() throws InterruptedException, TimeoutException {
        int i;
        while (this.mIsBinding) {
            this.mLock.wait();
        }
        if (this.mRemoteInstance != null) {
            return;
        }
        this.mIsBinding = true;
        try {
            if (!this.mIsLowPriority) {
                i = 67108865;
            } else {
                i = 1;
            }
            this.mContext.bindServiceAsUser(this.mIntent, this.mServiceConnection, i, this.mUserHandle);
            long jUptimeMillis = SystemClock.uptimeMillis();
            while (this.mRemoteInstance == null) {
                long jUptimeMillis2 = BIND_SPOOLER_SERVICE_TIMEOUT - (SystemClock.uptimeMillis() - jUptimeMillis);
                if (jUptimeMillis2 <= 0) {
                    throw new TimeoutException("Cannot get spooler!");
                }
                this.mLock.wait(jUptimeMillis2);
            }
            this.mCanUnbind = true;
        } finally {
            this.mIsBinding = false;
            this.mLock.notifyAll();
        }
    }

    private void unbindLocked() {
        if (this.mRemoteInstance == null) {
            return;
        }
        while (!this.mCanUnbind) {
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
            }
        }
        clearClientLocked();
        this.mRemoteInstance = null;
        this.mContext.unbindService(this.mServiceConnection);
    }

    private void setClientLocked() {
        try {
            this.mRemoteInstance.setClient(this.mClient);
        } catch (RemoteException e) {
            Slog.d(LOG_TAG, "Error setting print spooler client", e);
        }
    }

    private void clearClientLocked() {
        try {
            this.mRemoteInstance.setClient((IPrintSpoolerClient) null);
        } catch (RemoteException e) {
            Slog.d(LOG_TAG, "Error clearing print spooler client", e);
        }
    }

    private void throwIfDestroyedLocked() {
        if (this.mDestroyed) {
            throw new IllegalStateException("Cannot interact with a destroyed instance.");
        }
    }

    private void throwIfCalledOnMainThread() {
        if (Thread.currentThread() == this.mContext.getMainLooper().getThread()) {
            throw new RuntimeException("Cannot invoke on the main thread");
        }
    }

    private final class MyServiceConnection implements ServiceConnection {
        private MyServiceConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            synchronized (RemotePrintSpooler.this.mLock) {
                RemotePrintSpooler.this.mRemoteInstance = IPrintSpooler.Stub.asInterface(iBinder);
                RemotePrintSpooler.this.setClientLocked();
                RemotePrintSpooler.this.mLock.notifyAll();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (RemotePrintSpooler.this.mLock) {
                RemotePrintSpooler.this.clearClientLocked();
                RemotePrintSpooler.this.mRemoteInstance = null;
            }
        }
    }

    private static final class GetPrintJobInfosCaller extends TimedRemoteCaller<List<PrintJobInfo>> {
        private final IPrintSpoolerCallbacks mCallback;

        public GetPrintJobInfosCaller() {
            super(5000L);
            this.mCallback = new BasePrintSpoolerServiceCallbacks() {
                @Override
                public void onGetPrintJobInfosResult(List<PrintJobInfo> list, int i) {
                    GetPrintJobInfosCaller.this.onRemoteMethodResult(list, i);
                }
            };
        }

        public List<PrintJobInfo> getPrintJobInfos(IPrintSpooler iPrintSpooler, ComponentName componentName, int i, int i2) throws TimeoutException, RemoteException {
            int iOnBeforeRemoteCall = onBeforeRemoteCall();
            iPrintSpooler.getPrintJobInfos(this.mCallback, componentName, i, i2, iOnBeforeRemoteCall);
            return (List) getResultTimed(iOnBeforeRemoteCall);
        }
    }

    private static final class GetPrintJobInfoCaller extends TimedRemoteCaller<PrintJobInfo> {
        private final IPrintSpoolerCallbacks mCallback;

        public GetPrintJobInfoCaller() {
            super(5000L);
            this.mCallback = new BasePrintSpoolerServiceCallbacks() {
                @Override
                public void onGetPrintJobInfoResult(PrintJobInfo printJobInfo, int i) {
                    GetPrintJobInfoCaller.this.onRemoteMethodResult(printJobInfo, i);
                }
            };
        }

        public PrintJobInfo getPrintJobInfo(IPrintSpooler iPrintSpooler, PrintJobId printJobId, int i) throws TimeoutException, RemoteException {
            int iOnBeforeRemoteCall = onBeforeRemoteCall();
            iPrintSpooler.getPrintJobInfo(printJobId, this.mCallback, i, iOnBeforeRemoteCall);
            return (PrintJobInfo) getResultTimed(iOnBeforeRemoteCall);
        }
    }

    private static final class SetPrintJobStateCaller extends TimedRemoteCaller<Boolean> {
        private final IPrintSpoolerCallbacks mCallback;

        public SetPrintJobStateCaller() {
            super(5000L);
            this.mCallback = new BasePrintSpoolerServiceCallbacks() {
                @Override
                public void onSetPrintJobStateResult(boolean z, int i) {
                    SetPrintJobStateCaller.this.onRemoteMethodResult(Boolean.valueOf(z), i);
                }
            };
        }

        public boolean setPrintJobState(IPrintSpooler iPrintSpooler, PrintJobId printJobId, int i, String str) throws TimeoutException, RemoteException {
            int iOnBeforeRemoteCall = onBeforeRemoteCall();
            iPrintSpooler.setPrintJobState(printJobId, i, str, this.mCallback, iOnBeforeRemoteCall);
            return ((Boolean) getResultTimed(iOnBeforeRemoteCall)).booleanValue();
        }
    }

    private static final class SetPrintJobTagCaller extends TimedRemoteCaller<Boolean> {
        private final IPrintSpoolerCallbacks mCallback;

        public SetPrintJobTagCaller() {
            super(5000L);
            this.mCallback = new BasePrintSpoolerServiceCallbacks() {
                @Override
                public void onSetPrintJobTagResult(boolean z, int i) {
                    SetPrintJobTagCaller.this.onRemoteMethodResult(Boolean.valueOf(z), i);
                }
            };
        }

        public boolean setPrintJobTag(IPrintSpooler iPrintSpooler, PrintJobId printJobId, String str) throws TimeoutException, RemoteException {
            int iOnBeforeRemoteCall = onBeforeRemoteCall();
            iPrintSpooler.setPrintJobTag(printJobId, str, this.mCallback, iOnBeforeRemoteCall);
            return ((Boolean) getResultTimed(iOnBeforeRemoteCall)).booleanValue();
        }
    }

    private static final class OnCustomPrinterIconLoadedCaller extends TimedRemoteCaller<Void> {
        private final IPrintSpoolerCallbacks mCallback;

        public OnCustomPrinterIconLoadedCaller() {
            super(5000L);
            this.mCallback = new BasePrintSpoolerServiceCallbacks() {
                @Override
                public void onCustomPrinterIconCached(int i) {
                    OnCustomPrinterIconLoadedCaller.this.onRemoteMethodResult(null, i);
                }
            };
        }

        public Void onCustomPrinterIconLoaded(IPrintSpooler iPrintSpooler, PrinterId printerId, Icon icon) throws TimeoutException, RemoteException {
            int iOnBeforeRemoteCall = onBeforeRemoteCall();
            iPrintSpooler.onCustomPrinterIconLoaded(printerId, icon, this.mCallback, iOnBeforeRemoteCall);
            return (Void) getResultTimed(iOnBeforeRemoteCall);
        }
    }

    private static final class ClearCustomPrinterIconCacheCaller extends TimedRemoteCaller<Void> {
        private final IPrintSpoolerCallbacks mCallback;

        public ClearCustomPrinterIconCacheCaller() {
            super(5000L);
            this.mCallback = new BasePrintSpoolerServiceCallbacks() {
                @Override
                public void customPrinterIconCacheCleared(int i) {
                    ClearCustomPrinterIconCacheCaller.this.onRemoteMethodResult(null, i);
                }
            };
        }

        public Void clearCustomPrinterIconCache(IPrintSpooler iPrintSpooler) throws TimeoutException, RemoteException {
            int iOnBeforeRemoteCall = onBeforeRemoteCall();
            iPrintSpooler.clearCustomPrinterIconCache(this.mCallback, iOnBeforeRemoteCall);
            return (Void) getResultTimed(iOnBeforeRemoteCall);
        }
    }

    private static final class GetCustomPrinterIconCaller extends TimedRemoteCaller<Icon> {
        private final IPrintSpoolerCallbacks mCallback;

        public GetCustomPrinterIconCaller() {
            super(5000L);
            this.mCallback = new BasePrintSpoolerServiceCallbacks() {
                @Override
                public void onGetCustomPrinterIconResult(Icon icon, int i) {
                    GetCustomPrinterIconCaller.this.onRemoteMethodResult(icon, i);
                }
            };
        }

        public Icon getCustomPrinterIcon(IPrintSpooler iPrintSpooler, PrinterId printerId) throws TimeoutException, RemoteException {
            int iOnBeforeRemoteCall = onBeforeRemoteCall();
            iPrintSpooler.getCustomPrinterIcon(printerId, this.mCallback, iOnBeforeRemoteCall);
            return (Icon) getResultTimed(iOnBeforeRemoteCall);
        }
    }

    private static abstract class BasePrintSpoolerServiceCallbacks extends IPrintSpoolerCallbacks.Stub {
        private BasePrintSpoolerServiceCallbacks() {
        }

        public void onGetPrintJobInfosResult(List<PrintJobInfo> list, int i) {
        }

        public void onGetPrintJobInfoResult(PrintJobInfo printJobInfo, int i) {
        }

        public void onCancelPrintJobResult(boolean z, int i) {
        }

        public void onSetPrintJobStateResult(boolean z, int i) {
        }

        public void onSetPrintJobTagResult(boolean z, int i) {
        }

        public void onCustomPrinterIconCached(int i) {
        }

        public void onGetCustomPrinterIconResult(Icon icon, int i) {
        }

        public void customPrinterIconCacheCleared(int i) {
        }
    }

    private static final class PrintSpoolerClient extends IPrintSpoolerClient.Stub {
        private final WeakReference<RemotePrintSpooler> mWeakSpooler;

        public PrintSpoolerClient(RemotePrintSpooler remotePrintSpooler) {
            this.mWeakSpooler = new WeakReference<>(remotePrintSpooler);
        }

        public void onPrintJobQueued(PrintJobInfo printJobInfo) {
            RemotePrintSpooler remotePrintSpooler = this.mWeakSpooler.get();
            if (remotePrintSpooler != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    remotePrintSpooler.mCallbacks.onPrintJobQueued(printJobInfo);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void onAllPrintJobsForServiceHandled(ComponentName componentName) {
            RemotePrintSpooler remotePrintSpooler = this.mWeakSpooler.get();
            if (remotePrintSpooler != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    remotePrintSpooler.mCallbacks.onAllPrintJobsForServiceHandled(componentName);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void onAllPrintJobsHandled() {
            RemotePrintSpooler remotePrintSpooler = this.mWeakSpooler.get();
            if (remotePrintSpooler != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    remotePrintSpooler.onAllPrintJobsHandled();
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void onPrintJobStateChanged(PrintJobInfo printJobInfo) {
            RemotePrintSpooler remotePrintSpooler = this.mWeakSpooler.get();
            if (remotePrintSpooler != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    remotePrintSpooler.onPrintJobStateChanged(printJobInfo);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }
    }
}
