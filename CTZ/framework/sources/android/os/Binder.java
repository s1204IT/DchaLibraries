package android.os;

import android.os.IBinder;
import android.provider.SettingsStringUtil;
import android.util.Log;
import com.android.internal.os.BinderCallsStats;
import com.android.internal.os.BinderInternal;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.FunctionalUtils;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import libcore.io.IoUtils;
import libcore.util.NativeAllocationRegistry;

public class Binder implements IBinder {
    public static final boolean CHECK_PARCEL_SIZE = false;
    private static final boolean FIND_POTENTIAL_LEAKS = false;
    private static final int NATIVE_ALLOCATION_SIZE = 500;
    static final String TAG = "Binder";
    private String mDescriptor;
    private final long mObject = getNativeBBinderHolder();
    private IInterface mOwner;
    public static boolean LOG_RUNTIME_EXCEPTION = false;
    private static volatile String sDumpDisabled = null;
    private static volatile TransactionTracker sTransactionTracker = null;
    private static volatile boolean sTracingEnabled = false;
    static volatile boolean sWarnOnBlocking = false;

    public static final native void blockUntilThreadAvailable();

    public static final native long clearCallingIdentity();

    public static final native void flushPendingCommands();

    public static final native int getCallingPid();

    public static final native int getCallingUid();

    private static native long getFinalizer();

    private static native long getNativeBBinderHolder();

    private static native long getNativeFinalizer();

    public static final native int getThreadStrictModePolicy();

    public static final native void restoreCallingIdentity(long j);

    public static final native void setThreadStrictModePolicy(int i);

    private static class NoImagePreloadHolder {
        public static final NativeAllocationRegistry sRegistry = new NativeAllocationRegistry(Binder.class.getClassLoader(), Binder.getNativeFinalizer(), 500);

        private NoImagePreloadHolder() {
        }
    }

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

    public static void setWarnOnBlocking(boolean z) {
        sWarnOnBlocking = z;
    }

    public static IBinder allowBlocking(IBinder iBinder) {
        try {
            if (iBinder instanceof BinderProxy) {
                ((BinderProxy) iBinder).mWarnOnBlocking = false;
            } else if (iBinder != null && iBinder.getInterfaceDescriptor() != null && iBinder.queryLocalInterface(iBinder.getInterfaceDescriptor()) == null) {
                Log.w(TAG, "Unable to allow blocking on interface " + iBinder);
            }
        } catch (RemoteException e) {
        }
        return iBinder;
    }

    public static IBinder defaultBlocking(IBinder iBinder) {
        if (iBinder instanceof BinderProxy) {
            ((BinderProxy) iBinder).mWarnOnBlocking = sWarnOnBlocking;
        }
        return iBinder;
    }

    public static void copyAllowBlocking(IBinder iBinder, IBinder iBinder2) {
        if ((iBinder instanceof BinderProxy) && (iBinder2 instanceof BinderProxy)) {
            ((BinderProxy) iBinder2).mWarnOnBlocking = ((BinderProxy) iBinder).mWarnOnBlocking;
        }
    }

    public static final UserHandle getCallingUserHandle() {
        return UserHandle.of(UserHandle.getUserId(getCallingUid()));
    }

    public static final void withCleanCallingIdentity(FunctionalUtils.ThrowingRunnable throwingRunnable) {
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            throwingRunnable.runOrThrow();
            restoreCallingIdentity(jClearCallingIdentity);
        } catch (Throwable th) {
            restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    public static final <T> T withCleanCallingIdentity(FunctionalUtils.ThrowingSupplier<T> throwingSupplier) {
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            T orThrow = throwingSupplier.getOrThrow();
            restoreCallingIdentity(jClearCallingIdentity);
            return orThrow;
        } catch (Throwable th) {
            restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    public static final void joinThreadPool() {
        BinderInternal.joinThreadPool();
    }

    public static final boolean isProxy(IInterface iInterface) {
        return iInterface.asBinder() != iInterface;
    }

    public Binder() {
        NoImagePreloadHolder.sRegistry.registerNativeAllocation(this, this.mObject);
    }

    public void attachInterface(IInterface iInterface, String str) {
        this.mOwner = iInterface;
        this.mDescriptor = str;
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
    public IInterface queryLocalInterface(String str) {
        if (this.mDescriptor != null && this.mDescriptor.equals(str)) {
            return this.mOwner;
        }
        return null;
    }

    public static void setDumpDisabled(String str) {
        sDumpDisabled = str;
    }

    protected boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
        ParcelFileDescriptor fileDescriptor;
        FileDescriptor fileDescriptor2;
        if (i == 1598968902) {
            parcel2.writeString(getInterfaceDescriptor());
            return true;
        }
        if (i == 1598311760) {
            fileDescriptor = parcel.readFileDescriptor();
            String[] stringArray = parcel.readStringArray();
            if (fileDescriptor != null) {
                try {
                    dump(fileDescriptor.getFileDescriptor(), stringArray);
                } finally {
                    IoUtils.closeQuietly(fileDescriptor);
                }
            }
            if (parcel2 != null) {
                parcel2.writeNoException();
            } else {
                StrictMode.clearGatheredViolations();
            }
            return true;
        }
        if (i == 1598246212) {
            ParcelFileDescriptor fileDescriptor3 = parcel.readFileDescriptor();
            ParcelFileDescriptor fileDescriptor4 = parcel.readFileDescriptor();
            fileDescriptor = parcel.readFileDescriptor();
            String[] stringArray2 = parcel.readStringArray();
            ShellCallback shellCallbackCreateFromParcel = ShellCallback.CREATOR.createFromParcel(parcel);
            ResultReceiver resultReceiverCreateFromParcel = ResultReceiver.CREATOR.createFromParcel(parcel);
            if (fileDescriptor4 != null) {
                if (fileDescriptor3 == null) {
                    fileDescriptor2 = null;
                } else {
                    try {
                        fileDescriptor2 = fileDescriptor3.getFileDescriptor();
                    } catch (Throwable th) {
                        IoUtils.closeQuietly(fileDescriptor3);
                        IoUtils.closeQuietly(fileDescriptor4);
                        if (parcel2 != null) {
                            parcel2.writeNoException();
                        } else {
                            StrictMode.clearGatheredViolations();
                        }
                        throw th;
                    }
                }
                shellCommand(fileDescriptor2, fileDescriptor4.getFileDescriptor(), fileDescriptor != null ? fileDescriptor.getFileDescriptor() : fileDescriptor4.getFileDescriptor(), stringArray2, shellCallbackCreateFromParcel, resultReceiverCreateFromParcel);
            }
            IoUtils.closeQuietly(fileDescriptor3);
            IoUtils.closeQuietly(fileDescriptor4);
            if (parcel2 != null) {
                parcel2.writeNoException();
            } else {
                StrictMode.clearGatheredViolations();
            }
            return true;
        }
        return false;
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, String[] strArr) {
        FastPrintWriter fastPrintWriter = new FastPrintWriter(new FileOutputStream(fileDescriptor));
        try {
            doDump(fileDescriptor, fastPrintWriter, strArr);
        } finally {
            fastPrintWriter.flush();
        }
    }

    void doDump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (sDumpDisabled == null) {
            try {
                dump(fileDescriptor, printWriter, strArr);
                return;
            } catch (SecurityException e) {
                printWriter.println("Security exception: " + e.getMessage());
                throw e;
            } catch (Throwable th) {
                printWriter.println();
                printWriter.println("Exception occurred while dumping:");
                th.printStackTrace(printWriter);
                return;
            }
        }
        printWriter.println(sDumpDisabled);
    }

    @Override
    public void dumpAsync(final FileDescriptor fileDescriptor, final String[] strArr) {
        final FastPrintWriter fastPrintWriter = new FastPrintWriter(new FileOutputStream(fileDescriptor));
        new Thread("Binder.dumpAsync") {
            @Override
            public void run() {
                try {
                    Binder.this.dump(fileDescriptor, fastPrintWriter, strArr);
                } finally {
                    fastPrintWriter.flush();
                }
            }
        }.start();
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
    }

    @Override
    public void shellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) throws RemoteException {
        onShellCommand(fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) throws RemoteException {
        if (fileDescriptor3 != null) {
            fileDescriptor2 = fileDescriptor3;
        }
        FastPrintWriter fastPrintWriter = new FastPrintWriter(new FileOutputStream(fileDescriptor2));
        fastPrintWriter.println("No shell command implementation.");
        fastPrintWriter.flush();
        resultReceiver.send(0, null);
    }

    @Override
    public final boolean transact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
        if (parcel != null) {
            parcel.setDataPosition(0);
        }
        boolean zOnTransact = onTransact(i, parcel, parcel2, i2);
        if (parcel2 != null) {
            parcel2.setDataPosition(0);
        }
        return zOnTransact;
    }

    @Override
    public void linkToDeath(IBinder.DeathRecipient deathRecipient, int i) {
    }

    @Override
    public boolean unlinkToDeath(IBinder.DeathRecipient deathRecipient, int i) {
        return true;
    }

    static void checkParcel(IBinder iBinder, int i, Parcel parcel, String str) {
    }

    private boolean execTransact(int i, long j, long j2, int i2) {
        BinderCallsStats binderCallsStats = BinderCallsStats.getInstance();
        BinderCallsStats.CallSession callSessionCallStarted = binderCallsStats.callStarted(this, i);
        Parcel parcelObtain = Parcel.obtain(j);
        Parcel parcelObtain2 = Parcel.obtain(j2);
        boolean zIsTracingEnabled = isTracingEnabled();
        boolean z = true;
        try {
            if (zIsTracingEnabled) {
                try {
                    Trace.traceBegin(1L, getClass().getName() + SettingsStringUtil.DELIMITER + i);
                } catch (RemoteException | RuntimeException e) {
                    if (LOG_RUNTIME_EXCEPTION) {
                        Log.w(TAG, "Caught a RuntimeException from the binder stub implementation.", e);
                    }
                    if ((i2 & 1) == 0) {
                        parcelObtain2.setDataSize(0);
                        parcelObtain2.setDataPosition(0);
                        parcelObtain2.writeException(e);
                    } else if (e instanceof RemoteException) {
                        Log.w(TAG, "Binder call failed.", e);
                    } else {
                        Log.w(TAG, "Caught a RuntimeException from the binder stub implementation.", e);
                    }
                    if (zIsTracingEnabled) {
                        Trace.traceEnd(1L);
                    }
                }
            }
            boolean zOnTransact = onTransact(i, parcelObtain, parcelObtain2, i2);
            if (zIsTracingEnabled) {
                Trace.traceEnd(1L);
            }
            z = zOnTransact;
            checkParcel(this, i, parcelObtain2, "Unreasonably large binder reply buffer");
            parcelObtain2.recycle();
            parcelObtain.recycle();
            StrictMode.clearGatheredViolations();
            binderCallsStats.callEnded(callSessionCallStarted);
            return z;
        } catch (Throwable th) {
            if (zIsTracingEnabled) {
                Trace.traceEnd(1L);
            }
            throw th;
        }
    }
}
