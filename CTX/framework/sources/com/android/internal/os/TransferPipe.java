package com.android.internal.os;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Slog;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import libcore.io.IoUtils;

public class TransferPipe implements Runnable, Closeable {
    static final boolean DEBUG = false;
    static final long DEFAULT_TIMEOUT = 5000;
    static final String TAG = "TransferPipe";
    String mBufferPrefix;
    boolean mComplete;
    long mEndTime;
    String mFailure;
    final ParcelFileDescriptor[] mFds;
    FileDescriptor mOutFd;
    final Thread mThread;

    interface Caller {
        void go(IInterface iInterface, FileDescriptor fileDescriptor, String str, String[] strArr) throws RemoteException;
    }

    public TransferPipe() throws IOException {
        this(null);
    }

    public TransferPipe(String str) throws IOException {
        this(str, TAG);
    }

    protected TransferPipe(String str, String str2) throws IOException {
        this.mThread = new Thread(this, str2);
        this.mFds = ParcelFileDescriptor.createPipe();
        this.mBufferPrefix = str;
    }

    ParcelFileDescriptor getReadFd() {
        return this.mFds[0];
    }

    public ParcelFileDescriptor getWriteFd() {
        return this.mFds[1];
    }

    public void setBufferPrefix(String str) {
        this.mBufferPrefix = str;
    }

    public static void dumpAsync(IBinder iBinder, FileDescriptor fileDescriptor, String[] strArr) throws Exception {
        goDump(iBinder, fileDescriptor, strArr);
    }

    public static byte[] dumpAsync(IBinder iBinder, String... strArr) throws IOException, RemoteException {
        ByteArrayOutputStream byteArrayOutputStream;
        Throwable th;
        ParcelFileDescriptor[] parcelFileDescriptorArrCreatePipe = ParcelFileDescriptor.createPipe();
        try {
            try {
                dumpAsync(iBinder, parcelFileDescriptorArrCreatePipe[1].getFileDescriptor(), strArr);
                parcelFileDescriptorArrCreatePipe[1].close();
                iBinder = 0;
                ?? r8 = 0;
                parcelFileDescriptorArrCreatePipe[1] = null;
                byte[] bArr = new byte[4096];
                byteArrayOutputStream = new ByteArrayOutputStream();
                FileInputStream fileInputStream = new FileInputStream(parcelFileDescriptorArrCreatePipe[0].getFileDescriptor());
                while (true) {
                    try {
                        int i = fileInputStream.read(bArr);
                        if (i != -1) {
                            byteArrayOutputStream.write(bArr, 0, i);
                        } else {
                            $closeResource(null, fileInputStream);
                            return byteArrayOutputStream.toByteArray();
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        th = null;
                        $closeResource(th, fileInputStream);
                        throw th;
                    }
                }
            } finally {
                $closeResource(iBinder, byteArrayOutputStream);
            }
        } finally {
            parcelFileDescriptorArrCreatePipe[0].close();
            IoUtils.closeQuietly(parcelFileDescriptorArrCreatePipe[1]);
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    static void go(Caller caller, IInterface iInterface, FileDescriptor fileDescriptor, String str, String[] strArr) throws Exception {
        go(caller, iInterface, fileDescriptor, str, strArr, 5000L);
    }

    static void go(Caller caller, IInterface iInterface, FileDescriptor fileDescriptor, String str, String[] strArr, long j) throws Exception {
        if (iInterface.asBinder() instanceof Binder) {
            try {
                caller.go(iInterface, fileDescriptor, str, strArr);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        TransferPipe transferPipe = new TransferPipe();
        try {
            caller.go(iInterface, transferPipe.getWriteFd().getFileDescriptor(), str, strArr);
            transferPipe.go(fileDescriptor, j);
        } finally {
            $closeResource(null, transferPipe);
        }
    }

    static void goDump(IBinder iBinder, FileDescriptor fileDescriptor, String[] strArr) throws Exception {
        goDump(iBinder, fileDescriptor, strArr, 5000L);
    }

    static void goDump(IBinder iBinder, FileDescriptor fileDescriptor, String[] strArr, long j) throws Exception {
        if (iBinder instanceof Binder) {
            try {
                iBinder.dump(fileDescriptor, strArr);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        TransferPipe transferPipe = new TransferPipe();
        try {
            iBinder.dumpAsync(transferPipe.getWriteFd().getFileDescriptor(), strArr);
            transferPipe.go(fileDescriptor, j);
        } finally {
            $closeResource(null, transferPipe);
        }
    }

    public void go(FileDescriptor fileDescriptor) throws IOException {
        go(fileDescriptor, 5000L);
    }

    public void go(FileDescriptor fileDescriptor, long j) throws IOException {
        try {
            synchronized (this) {
                this.mOutFd = fileDescriptor;
                this.mEndTime = SystemClock.uptimeMillis() + j;
                closeFd(1);
                this.mThread.start();
                while (this.mFailure == null && !this.mComplete) {
                    long jUptimeMillis = this.mEndTime - SystemClock.uptimeMillis();
                    if (jUptimeMillis <= 0) {
                        this.mThread.interrupt();
                        throw new IOException("Timeout");
                    }
                    try {
                        wait(jUptimeMillis);
                    } catch (InterruptedException e) {
                    }
                }
                if (this.mFailure != null) {
                    throw new IOException(this.mFailure);
                }
            }
        } finally {
            kill();
        }
    }

    void closeFd(int i) {
        if (this.mFds[i] != null) {
            try {
                this.mFds[i].close();
            } catch (IOException e) {
            }
            this.mFds[i] = null;
        }
    }

    @Override
    public void close() {
        kill();
    }

    public void kill() {
        synchronized (this) {
            closeFd(0);
            closeFd(1);
        }
    }

    protected OutputStream getNewOutputStream() {
        return new FileOutputStream(this.mOutFd);
    }

    @Override
    public void run() {
        int i;
        byte[] bArr = new byte[1024];
        synchronized (this) {
            ParcelFileDescriptor readFd = getReadFd();
            if (readFd == null) {
                Slog.w(TAG, "Pipe has been closed...");
                return;
            }
            FileInputStream fileInputStream = new FileInputStream(readFd.getFileDescriptor());
            OutputStream newOutputStream = getNewOutputStream();
            byte[] bytes = null;
            if (this.mBufferPrefix != null) {
                bytes = this.mBufferPrefix.getBytes();
            }
            boolean z = true;
            while (true) {
                try {
                    int i2 = fileInputStream.read(bArr);
                    if (i2 <= 0) {
                        this.mThread.isInterrupted();
                        synchronized (this) {
                            this.mComplete = true;
                            notifyAll();
                        }
                        return;
                    }
                    if (bytes == null) {
                        newOutputStream.write(bArr, 0, i2);
                    } else {
                        boolean z2 = z;
                        int i3 = 0;
                        int i4 = 0;
                        while (i3 < i2) {
                            if (bArr[i3] != 10) {
                                if (i3 > i4) {
                                    newOutputStream.write(bArr, i4, i3 - i4);
                                }
                                if (z2) {
                                    newOutputStream.write(bytes);
                                    i = i3;
                                    z2 = false;
                                } else {
                                    i = i3;
                                }
                                do {
                                    i++;
                                    if (i >= i2) {
                                        break;
                                    }
                                } while (bArr[i] != 10);
                                if (i < i2) {
                                    z2 = true;
                                }
                            } else {
                                int i5 = i4;
                                i = i3;
                                i3 = i5;
                            }
                            int i6 = i + 1;
                            i4 = i3;
                            i3 = i6;
                        }
                        if (i2 > i4) {
                            newOutputStream.write(bArr, i4, i2 - i4);
                        }
                        z = z2;
                    }
                } catch (IOException e) {
                    synchronized (this) {
                        this.mFailure = e.toString();
                        notifyAll();
                        return;
                    }
                }
            }
        }
    }
}
