package android.os;

import android.os.IBinder;
import android.util.Log;
import com.android.internal.util.FastPrintWriter;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import libcore.io.IoUtils;

public class Binder implements IBinder {
    private static final boolean CHECK_PARCEL_SIZE = false;
    private static final boolean FIND_POTENTIAL_LEAKS = false;
    static final String TAG = "Binder";
    private String mDescriptor;
    private long mObject;
    private IInterface mOwner;
    public static boolean LOG_RUNTIME_EXCEPTION = false;
    private static String sDumpDisabled = null;
    private static TransactionTracker sTransactionTracker = null;
    private static boolean sTracingEnabled = false;

    public static final native void blockUntilThreadAvailable();

    public static final native long clearCallingIdentity();

    private final native void destroy();

    public static final native void flushPendingCommands();

    public static final native int getCallingPid();

    public static final native int getCallingUid();

    public static final native int getThreadStrictModePolicy();

    private final native void init();

    public static final native void joinThreadPool();

    public static final native void restoreCallingIdentity(long j);

    public static final native void setThreadStrictModePolicy(int i);

    public static void enableTracing() {
        sTracingEnabled = true;
    }

    public static void disableTracing() {
        sTracingEnabled = false;
    }

    public static boolean isTracingEnabled() {
        return sTracingEnabled;
    }

    public static synchronized TransactionTracker getTransactionTracker() {
        if (sTransactionTracker == null) {
            sTransactionTracker = new TransactionTracker();
        }
        return sTransactionTracker;
    }

    public static final UserHandle getCallingUserHandle() {
        return UserHandle.of(UserHandle.getUserId(getCallingUid()));
    }

    public static final boolean isProxy(IInterface iface) {
        return iface.asBinder() != iface;
    }

    public Binder() {
        init();
    }

    public void attachInterface(IInterface owner, String descriptor) {
        this.mOwner = owner;
        this.mDescriptor = descriptor;
    }

    @Override
    public String getInterfaceDescriptor() {
        return this.mDescriptor;
    }

    @Override
    public boolean pingBinder() {
        return true;
    }

    @Override
    public boolean isBinderAlive() {
        return true;
    }

    @Override
    public IInterface queryLocalInterface(String descriptor) {
        if (this.mDescriptor.equals(descriptor)) {
            return this.mOwner;
        }
        return null;
    }

    public static void setDumpDisabled(String msg) {
        synchronized (Binder.class) {
            sDumpDisabled = msg;
        }
    }

    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        ParcelFileDescriptor fd;
        FileDescriptor fileDescriptor = null;
        if (code == 1598968902) {
            reply.writeString(getInterfaceDescriptor());
            return true;
        }
        if (code == 1598311760) {
            fd = data.readFileDescriptor();
            String[] args = data.readStringArray();
            if (fd != null) {
                try {
                    dump(fd.getFileDescriptor(), args);
                } finally {
                    IoUtils.closeQuietly(fd);
                }
            }
            if (reply != null) {
                reply.writeNoException();
            } else {
                StrictMode.clearGatheredViolations();
            }
            return true;
        }
        if (code == 1598246212) {
            ParcelFileDescriptor in = data.readFileDescriptor();
            ParcelFileDescriptor out = data.readFileDescriptor();
            fd = data.readFileDescriptor();
            String[] args2 = data.readStringArray();
            ResultReceiver resultReceiver = ResultReceiver.CREATOR.createFromParcel(data);
            if (out != null) {
                if (in != null) {
                    try {
                        fileDescriptor = in.getFileDescriptor();
                    } finally {
                        IoUtils.closeQuietly(in);
                        IoUtils.closeQuietly(out);
                        if (reply != null) {
                            reply.writeNoException();
                        } else {
                            StrictMode.clearGatheredViolations();
                        }
                    }
                }
                shellCommand(fileDescriptor, out.getFileDescriptor(), fd != null ? fd.getFileDescriptor() : out.getFileDescriptor(), args2, resultReceiver);
            }
            return true;
        }
        return false;
    }

    @Override
    public void dump(FileDescriptor fd, String[] args) {
        FileOutputStream fout = new FileOutputStream(fd);
        FastPrintWriter fastPrintWriter = new FastPrintWriter(fout);
        try {
            doDump(fd, fastPrintWriter, args);
        } finally {
            fastPrintWriter.flush();
        }
    }

    void doDump(FileDescriptor fd, PrintWriter pw, String[] args) {
        String disabled;
        synchronized (Binder.class) {
            disabled = sDumpDisabled;
        }
        if (disabled == null) {
            try {
                dump(fd, pw, args);
                return;
            } catch (SecurityException e) {
                pw.println("Security exception: " + e.getMessage());
                throw e;
            } catch (Throwable e2) {
                pw.println();
                pw.println("Exception occurred while dumping:");
                e2.printStackTrace(pw);
                return;
            }
        }
        pw.println(sDumpDisabled);
    }

    @Override
    public void dumpAsync(final FileDescriptor fd, final String[] args) {
        FileOutputStream fout = new FileOutputStream(fd);
        final FastPrintWriter fastPrintWriter = new FastPrintWriter(fout);
        Thread thr = new Thread("Binder.dumpAsync") {
            @Override
            public void run() {
                try {
                    Binder.this.dump(fd, fastPrintWriter, args);
                } finally {
                    fastPrintWriter.flush();
                }
            }
        };
        thr.start();
    }

    protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
    }

    @Override
    public void shellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ResultReceiver resultReceiver) throws RemoteException {
        onShellCommand(in, out, err, args, resultReceiver);
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ResultReceiver resultReceiver) throws RemoteException {
        if (err == null) {
            err = out;
        }
        FileOutputStream fout = new FileOutputStream(err);
        FastPrintWriter fastPrintWriter = new FastPrintWriter(fout);
        fastPrintWriter.println("No shell command implementation.");
        fastPrintWriter.flush();
        resultReceiver.send(0, null);
    }

    @Override
    public final boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (data != null) {
            data.setDataPosition(0);
        }
        boolean r = onTransact(code, data, reply, flags);
        if (reply != null) {
            reply.setDataPosition(0);
        }
        return r;
    }

    @Override
    public void linkToDeath(IBinder.DeathRecipient recipient, int flags) {
    }

    @Override
    public boolean unlinkToDeath(IBinder.DeathRecipient recipient, int flags) {
        return true;
    }

    protected void finalize() throws Throwable {
        try {
            destroy();
        } finally {
            super.finalize();
        }
    }

    static void checkParcel(IBinder obj, int code, Parcel parcel, String msg) {
    }

    private boolean execTransact(int code, long dataObj, long replyObj, int flags) throws Exception {
        boolean zOnTransact;
        Parcel data = Parcel.obtain(dataObj);
        Parcel reply = Parcel.obtain(replyObj);
        try {
            zOnTransact = onTransact(code, data, reply, flags);
        } catch (RemoteException | RuntimeException e) {
            if (LOG_RUNTIME_EXCEPTION) {
                Log.w(TAG, "Caught a RuntimeException from the binder stub implementation.", e);
            }
            if ((flags & 1) != 0) {
                if (e instanceof RemoteException) {
                    Log.w(TAG, "Binder call failed.", e);
                } else {
                    Log.w(TAG, "Caught a RuntimeException from the binder stub implementation.", e);
                }
            } else {
                reply.setDataPosition(0);
                reply.writeException(e);
            }
            e.printStackTrace();
            zOnTransact = true;
        } catch (OutOfMemoryError e2) {
            Log.e(TAG, "Caught an OutOfMemoryError from the binder stub implementation.", e2);
            RuntimeException re = new RuntimeException("Out of memory", e2);
            re.printStackTrace();
            reply.setDataPosition(0);
            reply.writeException(re);
            zOnTransact = true;
        }
        checkParcel(this, code, reply, "Unreasonably large binder reply buffer");
        reply.recycle();
        data.recycle();
        StrictMode.clearGatheredViolations();
        return zOnTransact;
    }
}
