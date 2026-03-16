package com.android.server.print;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.util.Slog;
import android.util.TimedRemoteCaller;
import java.io.FileDescriptor;
import java.io.PrintWriter;
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
    private IPrintSpooler mRemoteInstance;
    private final UserHandle mUserHandle;
    private final Object mLock = new Object();
    private final GetPrintJobInfosCaller mGetPrintJobInfosCaller = new GetPrintJobInfosCaller();
    private final GetPrintJobInfoCaller mGetPrintJobInfoCaller = new GetPrintJobInfoCaller();
    private final SetPrintJobStateCaller mSetPrintJobStatusCaller = new SetPrintJobStateCaller();
    private final SetPrintJobTagCaller mSetPrintJobTagCaller = new SetPrintJobTagCaller();
    private final ServiceConnection mServiceConnection = new MyServiceConnection();
    private final PrintSpoolerClient mClient = new PrintSpoolerClient(this);
    private final Intent mIntent = new Intent();

    public interface PrintSpoolerCallbacks {
        void onAllPrintJobsForServiceHandled(ComponentName componentName);

        void onPrintJobQueued(PrintJobInfo printJobInfo);

        void onPrintJobStateChanged(PrintJobInfo printJobInfo);
    }

    static {
        BIND_SPOOLER_SERVICE_TIMEOUT = "eng".equals(Build.TYPE) ? 120000L : 10000L;
    }

    public RemotePrintSpooler(Context context, int userId, PrintSpoolerCallbacks callbacks) {
        this.mContext = context;
        this.mUserHandle = new UserHandle(userId);
        this.mCallbacks = callbacks;
        this.mIntent.setComponent(new ComponentName("com.android.printspooler", "com.android.printspooler.model.PrintSpoolerService"));
    }

    public final List<PrintJobInfo> getPrintJobInfos(ComponentName componentName, int state, int appId) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = DEBUG;
            try {
            } catch (Throwable th) {
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    throw th;
                }
            }
        }
        try {
            List<PrintJobInfo> printJobInfos = this.mGetPrintJobInfosCaller.getPrintJobInfos(getRemoteInstanceLazy(), componentName, state, appId);
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
            return printJobInfos;
        } catch (RemoteException re) {
            Slog.e(LOG_TAG, "Error getting print jobs.", re);
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                return null;
            }
        } catch (TimeoutException te) {
            Slog.e(LOG_TAG, "Error getting print jobs.", te);
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                return null;
            }
        }
    }

    public final void createPrintJob(PrintJobInfo printJob) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = DEBUG;
            try {
            } catch (Throwable th) {
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    throw th;
                }
            }
        }
        try {
            try {
                getRemoteInstanceLazy().createPrintJob(printJob);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (TimeoutException te) {
                Slog.e(LOG_TAG, "Error creating print job.", te);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        } catch (RemoteException re) {
            Slog.e(LOG_TAG, "Error creating print job.", re);
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        }
    }

    public final void writePrintJobData(ParcelFileDescriptor fd, PrintJobId printJobId) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = DEBUG;
        }
        try {
            try {
                try {
                    getRemoteInstanceLazy().writePrintJobData(fd, printJobId);
                    IoUtils.closeQuietly(fd);
                    synchronized (this.mLock) {
                        this.mCanUnbind = true;
                        this.mLock.notifyAll();
                    }
                } catch (TimeoutException te) {
                    Slog.e(LOG_TAG, "Error writing print job data.", te);
                    IoUtils.closeQuietly(fd);
                    synchronized (this.mLock) {
                        this.mCanUnbind = true;
                        this.mLock.notifyAll();
                    }
                }
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error writing print job data.", re);
                IoUtils.closeQuietly(fd);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
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
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = DEBUG;
            try {
            } catch (Throwable th) {
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    throw th;
                }
            }
        }
        try {
            PrintJobInfo printJobInfo = this.mGetPrintJobInfoCaller.getPrintJobInfo(getRemoteInstanceLazy(), printJobId, appId);
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
            return printJobInfo;
        } catch (RemoteException re) {
            Slog.e(LOG_TAG, "Error getting print job info.", re);
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                return null;
            }
        } catch (TimeoutException te) {
            Slog.e(LOG_TAG, "Error getting print job info.", te);
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                return null;
            }
        }
    }

    public final boolean setPrintJobState(PrintJobId printJobId, int state, String error) {
        boolean printJobState = DEBUG;
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = DEBUG;
            try {
            } catch (Throwable th) {
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    throw th;
                }
            }
        }
        try {
            printJobState = this.mSetPrintJobStatusCaller.setPrintJobState(getRemoteInstanceLazy(), printJobId, state, error);
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (RemoteException re) {
            Slog.e(LOG_TAG, "Error setting print job state.", re);
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (TimeoutException te) {
            Slog.e(LOG_TAG, "Error setting print job state.", te);
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        }
        return printJobState;
    }

    public final boolean setPrintJobTag(PrintJobId printJobId, String tag) {
        boolean printJobTag = DEBUG;
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = DEBUG;
            try {
            } catch (Throwable th) {
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    throw th;
                }
            }
        }
        try {
            printJobTag = this.mSetPrintJobTagCaller.setPrintJobTag(getRemoteInstanceLazy(), printJobId, tag);
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (RemoteException re) {
            Slog.e(LOG_TAG, "Error setting print job tag.", re);
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (TimeoutException te) {
            Slog.e(LOG_TAG, "Error setting print job tag.", te);
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        }
        return printJobTag;
    }

    public final void setPrintJobCancelling(PrintJobId printJobId, boolean cancelling) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = DEBUG;
            try {
            } catch (Throwable th) {
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    throw th;
                }
            }
        }
        try {
            try {
                getRemoteInstanceLazy().setPrintJobCancelling(printJobId, cancelling);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (TimeoutException te) {
                Slog.e(LOG_TAG, "Error setting print job cancelling.", te);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        } catch (RemoteException re) {
            Slog.e(LOG_TAG, "Error setting print job cancelling.", re);
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        }
    }

    public final void removeObsoletePrintJobs() {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = DEBUG;
            try {
            } catch (Throwable th) {
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    throw th;
                }
            }
        }
        try {
            try {
                getRemoteInstanceLazy().removeObsoletePrintJobs();
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (TimeoutException te) {
                Slog.e(LOG_TAG, "Error removing obsolete print jobs .", te);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        } catch (RemoteException re) {
            Slog.e(LOG_TAG, "Error removing obsolete print jobs .", re);
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        }
    }

    public final void destroy() {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            unbindLocked();
            this.mDestroyed = true;
            this.mCanUnbind = DEBUG;
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
        IPrintSpooler iPrintSpooler;
        synchronized (this.mLock) {
            if (this.mRemoteInstance != null) {
                iPrintSpooler = this.mRemoteInstance;
            } else {
                bindLocked();
                iPrintSpooler = this.mRemoteInstance;
            }
        }
        return iPrintSpooler;
    }

    private void bindLocked() throws TimeoutException {
        if (this.mRemoteInstance == null) {
            this.mContext.bindServiceAsUser(this.mIntent, this.mServiceConnection, 1, this.mUserHandle);
            long startMillis = SystemClock.uptimeMillis();
            while (this.mRemoteInstance == null) {
                long elapsedMillis = SystemClock.uptimeMillis() - startMillis;
                long remainingMillis = BIND_SPOOLER_SERVICE_TIMEOUT - elapsedMillis;
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
    }

    private void unbindLocked() {
        if (this.mRemoteInstance != null) {
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
            this.mRemoteInstance.setClient((IPrintSpoolerClient) null);
        } catch (RemoteException re) {
            Slog.d(LOG_TAG, "Error clearing print spooler client", re);
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
            super(5000L);
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
            super(5000L);
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
            super(5000L);
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
            super(5000L);
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

    private static abstract class BasePrintSpoolerServiceCallbacks extends IPrintSpoolerCallbacks.Stub {
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
    }

    private static final class PrintSpoolerClient extends IPrintSpoolerClient.Stub {
        private final WeakReference<RemotePrintSpooler> mWeakSpooler;

        public PrintSpoolerClient(RemotePrintSpooler spooler) {
            this.mWeakSpooler = new WeakReference<>(spooler);
        }

        public void onPrintJobQueued(PrintJobInfo printJob) {
            RemotePrintSpooler spooler = this.mWeakSpooler.get();
            if (spooler != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    spooler.mCallbacks.onPrintJobQueued(printJob);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void onAllPrintJobsForServiceHandled(ComponentName printService) {
            RemotePrintSpooler spooler = this.mWeakSpooler.get();
            if (spooler != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    spooler.mCallbacks.onAllPrintJobsForServiceHandled(printService);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void onAllPrintJobsHandled() {
            RemotePrintSpooler spooler = this.mWeakSpooler.get();
            if (spooler != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    spooler.onAllPrintJobsHandled();
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void onPrintJobStateChanged(PrintJobInfo printJob) {
            RemotePrintSpooler spooler = this.mWeakSpooler.get();
            if (spooler != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    spooler.onPrintJobStateChanged(printJob);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }
}
