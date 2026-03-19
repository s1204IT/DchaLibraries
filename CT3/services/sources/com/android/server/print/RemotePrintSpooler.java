package com.android.server.print;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Icon;
import android.os.Binder;
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
import com.mediatek.datashaping.DataShapingUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.TimeoutException;
import libcore.io.IoUtils;

final class RemotePrintSpooler {
    private static final long BIND_SPOOLER_SERVICE_TIMEOUT = 10000;
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "RemotePrintSpooler";
    private final PrintSpoolerCallbacks mCallbacks;
    private boolean mCanUnbind;
    private final Context mContext;
    private boolean mDestroyed;
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
    private final ServiceConnection mServiceConnection = new MyServiceConnection(this, null);
    private final PrintSpoolerClient mClient = new PrintSpoolerClient(this);
    private final Intent mIntent = new Intent();

    public interface PrintSpoolerCallbacks {
        void onAllPrintJobsForServiceHandled(ComponentName componentName);

        void onPrintJobQueued(PrintJobInfo printJobInfo);

        void onPrintJobStateChanged(PrintJobInfo printJobInfo);
    }

    public RemotePrintSpooler(Context context, int userId, boolean lowPriority, PrintSpoolerCallbacks callbacks) {
        this.mContext = context;
        this.mUserHandle = new UserHandle(userId);
        this.mCallbacks = callbacks;
        this.mIsLowPriority = lowPriority;
        this.mIntent.setComponent(new ComponentName("com.android.printspooler", "com.android.printspooler.model.PrintSpoolerService"));
    }

    public void increasePriority() {
        if (!this.mIsLowPriority) {
            return;
        }
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

    public final List<PrintJobInfo> getPrintJobInfos(ComponentName componentName, int state, int appId) {
        Object obj;
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                List<PrintJobInfo> printJobInfos = this.mGetPrintJobInfosCaller.getPrintJobInfos(getRemoteInstanceLazy(), componentName, state, appId);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
                return printJobInfos;
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error getting print jobs.", re);
                obj = this.mLock;
                synchronized (obj) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    return null;
                }
            } catch (TimeoutException te) {
                Slog.e(LOG_TAG, "Error getting print jobs.", te);
                obj = this.mLock;
                synchronized (obj) {
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

    public final void createPrintJob(PrintJobInfo printJob) {
        Object obj;
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                getRemoteInstanceLazy().createPrintJob(printJob);
                obj = this.mLock;
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error creating print job.", re);
                obj = this.mLock;
                synchronized (obj) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (TimeoutException te) {
                Slog.e(LOG_TAG, "Error creating print job.", te);
                obj = this.mLock;
                synchronized (obj) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
            synchronized (obj) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final void writePrintJobData(ParcelFileDescriptor fd, PrintJobId printJobId) {
        Object obj;
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                getRemoteInstanceLazy().writePrintJobData(fd, printJobId);
                IoUtils.closeQuietly(fd);
                obj = this.mLock;
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error writing print job data.", re);
                IoUtils.closeQuietly(fd);
                obj = this.mLock;
                synchronized (obj) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (TimeoutException te) {
                Slog.e(LOG_TAG, "Error writing print job data.", te);
                IoUtils.closeQuietly(fd);
                obj = this.mLock;
                synchronized (obj) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
            synchronized (obj) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (Throwable th) {
            IoUtils.closeQuietly(fd);
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final PrintJobInfo getPrintJobInfo(PrintJobId printJobId, int appId) {
        Object obj;
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                PrintJobInfo printJobInfo = this.mGetPrintJobInfoCaller.getPrintJobInfo(getRemoteInstanceLazy(), printJobId, appId);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
                return printJobInfo;
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error getting print job info.", re);
                obj = this.mLock;
                synchronized (obj) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    return null;
                }
            } catch (TimeoutException te) {
                Slog.e(LOG_TAG, "Error getting print job info.", te);
                obj = this.mLock;
                synchronized (obj) {
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

    public final boolean setPrintJobState(PrintJobId printJobId, int state, String error) {
        Object obj;
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                boolean printJobState = this.mSetPrintJobStatusCaller.setPrintJobState(getRemoteInstanceLazy(), printJobId, state, error);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
                return printJobState;
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error setting print job state.", re);
                obj = this.mLock;
                synchronized (obj) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    return false;
                }
            } catch (TimeoutException te) {
                Slog.e(LOG_TAG, "Error setting print job state.", te);
                obj = this.mLock;
                synchronized (obj) {
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

    public final void setProgress(PrintJobId printJobId, float progress) {
        Object obj;
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                getRemoteInstanceLazy().setProgress(printJobId, progress);
                obj = this.mLock;
            } catch (RemoteException | TimeoutException re) {
                Slog.e(LOG_TAG, "Error setting progress.", re);
                obj = this.mLock;
                synchronized (obj) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
            synchronized (obj) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final void setStatus(PrintJobId printJobId, CharSequence status) {
        Object obj;
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                getRemoteInstanceLazy().setStatus(printJobId, status);
                obj = this.mLock;
            } catch (RemoteException | TimeoutException re) {
                Slog.e(LOG_TAG, "Error setting status.", re);
                obj = this.mLock;
                synchronized (obj) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
            synchronized (obj) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final void setStatus(PrintJobId printJobId, int status, CharSequence appPackageName) {
        Object obj;
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                getRemoteInstanceLazy().setStatusRes(printJobId, status, appPackageName);
                obj = this.mLock;
            } catch (RemoteException | TimeoutException re) {
                Slog.e(LOG_TAG, "Error setting status.", re);
                obj = this.mLock;
                synchronized (obj) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
            synchronized (obj) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
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
        Object obj;
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                this.mCustomPrinterIconLoadedCaller.onCustomPrinterIconLoaded(getRemoteInstanceLazy(), printerId, icon);
                obj = this.mLock;
            } catch (RemoteException | TimeoutException re) {
                Slog.e(LOG_TAG, "Error loading new custom printer icon.", re);
                obj = this.mLock;
                synchronized (obj) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
            synchronized (obj) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
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
            } catch (RemoteException | TimeoutException re) {
                Slog.e(LOG_TAG, "Error getting custom printer icon.", re);
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
        Object obj;
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                this.mClearCustomPrinterIconCache.clearCustomPrinterIconCache(getRemoteInstanceLazy());
                obj = this.mLock;
            } catch (RemoteException | TimeoutException re) {
                Slog.e(LOG_TAG, "Error clearing custom printer icon cache.", re);
                obj = this.mLock;
                synchronized (obj) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
            synchronized (obj) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final boolean setPrintJobTag(PrintJobId printJobId, String tag) {
        Object obj;
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                boolean printJobTag = this.mSetPrintJobTagCaller.setPrintJobTag(getRemoteInstanceLazy(), printJobId, tag);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
                return printJobTag;
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error setting print job tag.", re);
                obj = this.mLock;
                synchronized (obj) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    return false;
                }
            } catch (TimeoutException te) {
                Slog.e(LOG_TAG, "Error setting print job tag.", te);
                obj = this.mLock;
                synchronized (obj) {
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

    public final void setPrintJobCancelling(PrintJobId printJobId, boolean cancelling) {
        Object obj;
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                getRemoteInstanceLazy().setPrintJobCancelling(printJobId, cancelling);
                obj = this.mLock;
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error setting print job cancelling.", re);
                obj = this.mLock;
                synchronized (obj) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (TimeoutException te) {
                Slog.e(LOG_TAG, "Error setting print job cancelling.", te);
                obj = this.mLock;
                synchronized (obj) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
            synchronized (obj) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final void pruneApprovedPrintServices(List<ComponentName> servicesToKeep) {
        Object obj;
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                getRemoteInstanceLazy().pruneApprovedPrintServices(servicesToKeep);
                obj = this.mLock;
            } catch (RemoteException | TimeoutException re) {
                Slog.e(LOG_TAG, "Error pruning approved print services.", re);
                obj = this.mLock;
                synchronized (obj) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
            synchronized (obj) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final void removeObsoletePrintJobs() {
        Object obj;
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                getRemoteInstanceLazy().removeObsoletePrintJobs();
                obj = this.mLock;
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error removing obsolete print jobs .", re);
                obj = this.mLock;
                synchronized (obj) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (TimeoutException te) {
                Slog.e(LOG_TAG, "Error removing obsolete print jobs .", te);
                obj = this.mLock;
                synchronized (obj) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
            synchronized (obj) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
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

    public void dump(FileDescriptor fd, PrintWriter pw, String prefix) {
        synchronized (this.mLock) {
            pw.append((CharSequence) prefix).append("destroyed=").append((CharSequence) String.valueOf(this.mDestroyed)).println();
            pw.append((CharSequence) prefix).append("bound=").append((CharSequence) (this.mRemoteInstance != null ? "true" : "false")).println();
            pw.flush();
            try {
                getRemoteInstanceLazy().asBinder().dump(fd, new String[]{prefix});
            } catch (RemoteException e) {
            } catch (TimeoutException e2) {
            }
        }
    }

    private void onAllPrintJobsHandled() {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            unbindLocked();
        }
    }

    private void onPrintJobStateChanged(PrintJobInfo printJob) {
        this.mCallbacks.onPrintJobStateChanged(printJob);
    }

    private IPrintSpooler getRemoteInstanceLazy() throws TimeoutException {
        synchronized (this.mLock) {
            if (this.mRemoteInstance != null) {
                return this.mRemoteInstance;
            }
            bindLocked();
            return this.mRemoteInstance;
        }
    }

    private void bindLocked() throws TimeoutException {
        int flags;
        if (this.mRemoteInstance != null) {
            return;
        }
        if (this.mIsLowPriority) {
            flags = 1;
        } else {
            flags = 67108865;
        }
        this.mContext.bindServiceAsUser(this.mIntent, this.mServiceConnection, flags, this.mUserHandle);
        long startMillis = SystemClock.uptimeMillis();
        while (this.mRemoteInstance == null) {
            long elapsedMillis = SystemClock.uptimeMillis() - startMillis;
            long remainingMillis = 10000 - elapsedMillis;
            if (remainingMillis <= 0) {
                throw new TimeoutException("Cannot get spooler!");
            }
            try {
                this.mLock.wait(remainingMillis);
            } catch (InterruptedException e) {
            }
        }
        this.mCanUnbind = true;
        this.mLock.notifyAll();
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
        } catch (RemoteException re) {
            Slog.d(LOG_TAG, "Error setting print spooler client", re);
        }
    }

    private void clearClientLocked() {
        try {
            if (this.mRemoteInstance == null) {
                return;
            }
            this.mRemoteInstance.setClient((IPrintSpoolerClient) null);
        } catch (RemoteException re) {
            Slog.d(LOG_TAG, "Error clearing print spooler client", re);
        }
    }

    private void throwIfDestroyedLocked() {
        if (!this.mDestroyed) {
        } else {
            throw new IllegalStateException("Cannot interact with a destroyed instance.");
        }
    }

    private void throwIfCalledOnMainThread() {
        if (Thread.currentThread() != this.mContext.getMainLooper().getThread()) {
        } else {
            throw new RuntimeException("Cannot invoke on the main thread");
        }
    }

    private final class MyServiceConnection implements ServiceConnection {
        MyServiceConnection(RemotePrintSpooler this$0, MyServiceConnection myServiceConnection) {
            this();
        }

        private MyServiceConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (RemotePrintSpooler.this.mLock) {
                RemotePrintSpooler.this.mRemoteInstance = IPrintSpooler.Stub.asInterface(service);
                RemotePrintSpooler.this.setClientLocked();
                RemotePrintSpooler.this.mLock.notifyAll();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (RemotePrintSpooler.this.mLock) {
                RemotePrintSpooler.this.clearClientLocked();
                RemotePrintSpooler.this.mRemoteInstance = null;
            }
        }
    }

    private static final class GetPrintJobInfosCaller extends TimedRemoteCaller<List<PrintJobInfo>> {
        private final IPrintSpoolerCallbacks mCallback;

        public GetPrintJobInfosCaller() {
            super(DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
            this.mCallback = new BasePrintSpoolerServiceCallbacks() {
                @Override
                public void onGetPrintJobInfosResult(List<PrintJobInfo> printJobs, int sequence) {
                    GetPrintJobInfosCaller.this.onRemoteMethodResult(printJobs, sequence);
                }
            };
        }

        public List<PrintJobInfo> getPrintJobInfos(IPrintSpooler target, ComponentName componentName, int state, int appId) throws TimeoutException, RemoteException {
            int sequence = onBeforeRemoteCall();
            target.getPrintJobInfos(this.mCallback, componentName, state, appId, sequence);
            return (List) getResultTimed(sequence);
        }
    }

    private static final class GetPrintJobInfoCaller extends TimedRemoteCaller<PrintJobInfo> {
        private final IPrintSpoolerCallbacks mCallback;

        public GetPrintJobInfoCaller() {
            super(DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
            this.mCallback = new BasePrintSpoolerServiceCallbacks() {
                @Override
                public void onGetPrintJobInfoResult(PrintJobInfo printJob, int sequence) {
                    GetPrintJobInfoCaller.this.onRemoteMethodResult(printJob, sequence);
                }
            };
        }

        public PrintJobInfo getPrintJobInfo(IPrintSpooler target, PrintJobId printJobId, int appId) throws TimeoutException, RemoteException {
            int sequence = onBeforeRemoteCall();
            target.getPrintJobInfo(printJobId, this.mCallback, appId, sequence);
            return (PrintJobInfo) getResultTimed(sequence);
        }
    }

    private static final class SetPrintJobStateCaller extends TimedRemoteCaller<Boolean> {
        private final IPrintSpoolerCallbacks mCallback;

        public SetPrintJobStateCaller() {
            super(DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
            this.mCallback = new BasePrintSpoolerServiceCallbacks() {
                @Override
                public void onSetPrintJobStateResult(boolean success, int sequence) {
                    SetPrintJobStateCaller.this.onRemoteMethodResult(Boolean.valueOf(success), sequence);
                }
            };
        }

        public boolean setPrintJobState(IPrintSpooler target, PrintJobId printJobId, int status, String error) throws TimeoutException, RemoteException {
            int sequence = onBeforeRemoteCall();
            target.setPrintJobState(printJobId, status, error, this.mCallback, sequence);
            return ((Boolean) getResultTimed(sequence)).booleanValue();
        }
    }

    private static final class SetPrintJobTagCaller extends TimedRemoteCaller<Boolean> {
        private final IPrintSpoolerCallbacks mCallback;

        public SetPrintJobTagCaller() {
            super(DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
            this.mCallback = new BasePrintSpoolerServiceCallbacks() {
                @Override
                public void onSetPrintJobTagResult(boolean success, int sequence) {
                    SetPrintJobTagCaller.this.onRemoteMethodResult(Boolean.valueOf(success), sequence);
                }
            };
        }

        public boolean setPrintJobTag(IPrintSpooler target, PrintJobId printJobId, String tag) throws TimeoutException, RemoteException {
            int sequence = onBeforeRemoteCall();
            target.setPrintJobTag(printJobId, tag, this.mCallback, sequence);
            return ((Boolean) getResultTimed(sequence)).booleanValue();
        }
    }

    private static final class OnCustomPrinterIconLoadedCaller extends TimedRemoteCaller<Void> {
        private final IPrintSpoolerCallbacks mCallback;

        public OnCustomPrinterIconLoadedCaller() {
            super(DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
            this.mCallback = new BasePrintSpoolerServiceCallbacks() {
                @Override
                public void onCustomPrinterIconCached(int sequence) {
                    OnCustomPrinterIconLoadedCaller.this.onRemoteMethodResult(null, sequence);
                }
            };
        }

        public Void onCustomPrinterIconLoaded(IPrintSpooler target, PrinterId printerId, Icon icon) throws TimeoutException, RemoteException {
            int sequence = onBeforeRemoteCall();
            target.onCustomPrinterIconLoaded(printerId, icon, this.mCallback, sequence);
            return (Void) getResultTimed(sequence);
        }
    }

    private static final class ClearCustomPrinterIconCacheCaller extends TimedRemoteCaller<Void> {
        private final IPrintSpoolerCallbacks mCallback;

        public ClearCustomPrinterIconCacheCaller() {
            super(DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
            this.mCallback = new BasePrintSpoolerServiceCallbacks() {
                @Override
                public void customPrinterIconCacheCleared(int sequence) {
                    ClearCustomPrinterIconCacheCaller.this.onRemoteMethodResult(null, sequence);
                }
            };
        }

        public Void clearCustomPrinterIconCache(IPrintSpooler target) throws TimeoutException, RemoteException {
            int sequence = onBeforeRemoteCall();
            target.clearCustomPrinterIconCache(this.mCallback, sequence);
            return (Void) getResultTimed(sequence);
        }
    }

    private static final class GetCustomPrinterIconCaller extends TimedRemoteCaller<Icon> {
        private final IPrintSpoolerCallbacks mCallback;

        public GetCustomPrinterIconCaller() {
            super(DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
            this.mCallback = new BasePrintSpoolerServiceCallbacks() {
                @Override
                public void onGetCustomPrinterIconResult(Icon icon, int sequence) {
                    GetCustomPrinterIconCaller.this.onRemoteMethodResult(icon, sequence);
                }
            };
        }

        public Icon getCustomPrinterIcon(IPrintSpooler target, PrinterId printerId) throws TimeoutException, RemoteException {
            int sequence = onBeforeRemoteCall();
            target.getCustomPrinterIcon(printerId, this.mCallback, sequence);
            return (Icon) getResultTimed(sequence);
        }
    }

    private static abstract class BasePrintSpoolerServiceCallbacks extends IPrintSpoolerCallbacks.Stub {
        BasePrintSpoolerServiceCallbacks(BasePrintSpoolerServiceCallbacks basePrintSpoolerServiceCallbacks) {
            this();
        }

        private BasePrintSpoolerServiceCallbacks() {
        }

        public void onGetPrintJobInfosResult(List<PrintJobInfo> printJobIds, int sequence) {
        }

        public void onGetPrintJobInfoResult(PrintJobInfo printJob, int sequence) {
        }

        public void onCancelPrintJobResult(boolean canceled, int sequence) {
        }

        public void onSetPrintJobStateResult(boolean success, int sequece) {
        }

        public void onSetPrintJobTagResult(boolean success, int sequence) {
        }

        public void onCustomPrinterIconCached(int sequence) {
        }

        public void onGetCustomPrinterIconResult(Icon icon, int sequence) {
        }

        public void customPrinterIconCacheCleared(int sequence) {
        }
    }

    private static final class PrintSpoolerClient extends IPrintSpoolerClient.Stub {
        private final WeakReference<RemotePrintSpooler> mWeakSpooler;

        public PrintSpoolerClient(RemotePrintSpooler spooler) {
            this.mWeakSpooler = new WeakReference<>(spooler);
        }

        public void onPrintJobQueued(PrintJobInfo printJob) {
            RemotePrintSpooler spooler = this.mWeakSpooler.get();
            if (spooler == null) {
                return;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                spooler.mCallbacks.onPrintJobQueued(printJob);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void onAllPrintJobsForServiceHandled(ComponentName printService) {
            RemotePrintSpooler spooler = this.mWeakSpooler.get();
            if (spooler == null) {
                return;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                spooler.mCallbacks.onAllPrintJobsForServiceHandled(printService);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void onAllPrintJobsHandled() {
            RemotePrintSpooler spooler = this.mWeakSpooler.get();
            if (spooler == null) {
                return;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                spooler.onAllPrintJobsHandled();
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void onPrintJobStateChanged(PrintJobInfo printJob) {
            RemotePrintSpooler spooler = this.mWeakSpooler.get();
            if (spooler == null) {
                return;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                spooler.onPrintJobStateChanged(printJob);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }
}
