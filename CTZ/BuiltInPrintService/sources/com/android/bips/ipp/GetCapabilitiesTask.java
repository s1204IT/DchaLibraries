package com.android.bips.ipp;

import android.net.Uri;
import android.os.AsyncTask;
import com.android.bips.jni.LocalPrinterCapabilities;
import com.android.bips.util.PriorityLock;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class GetCapabilitiesTask extends AsyncTask<Void, Void, LocalPrinterCapabilities> {
    private static final String TAG = GetCapabilitiesTask.class.getSimpleName();
    private static final PriorityLock sLock = new PriorityLock();
    private final Backend mBackend;
    private final boolean mPriority;
    private volatile Socket mSocket;
    private final long mTimeout;
    private final Uri mUri;

    GetCapabilitiesTask(Backend backend, Uri uri, long j, boolean z) {
        this.mUri = uri;
        this.mBackend = backend;
        this.mTimeout = j;
        this.mPriority = z;
    }

    private boolean isDeviceOnline(Uri uri) {
        Throwable th;
        Throwable th2;
        try {
            Socket socket = new Socket();
            try {
                this.mSocket = socket;
                socket.connect(new InetSocketAddress(uri.getHost(), uri.getPort()), (int) this.mTimeout);
                socket.close();
                return true;
            } catch (Throwable th3) {
                try {
                    throw th3;
                } catch (Throwable th4) {
                    th = th3;
                    th2 = th4;
                    if (th != null) {
                        socket.close();
                        throw th2;
                    }
                    try {
                        socket.close();
                        throw th2;
                    } catch (Throwable th5) {
                        th.addSuppressed(th5);
                        throw th2;
                    }
                }
            }
        } catch (IOException e) {
            return false;
        } finally {
            this.mSocket = null;
        }
    }

    public void forceCancel() {
        cancel(true);
        Socket socket = this.mSocket;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    @Override
    protected LocalPrinterCapabilities doInBackground(Void... voidArr) {
        System.currentTimeMillis();
        LocalPrinterCapabilities localPrinterCapabilities = new LocalPrinterCapabilities();
        try {
            localPrinterCapabilities.inetAddress = InetAddress.getByName(this.mUri.getHost());
            if (!isDeviceOnline(this.mUri) || isCancelled()) {
                return null;
            }
            try {
                sLock.lock(this.mPriority ? 1 : 0);
                System.currentTimeMillis();
                try {
                    if (isCancelled()) {
                        return null;
                    }
                    if (this.mBackend.nativeGetCapabilities(Backend.getIp(this.mUri.getHost()), this.mUri.getPort(), this.mUri.getPath(), this.mUri.getScheme(), this.mTimeout, localPrinterCapabilities) == 0) {
                        return localPrinterCapabilities;
                    }
                    return null;
                } finally {
                    sLock.unlock();
                }
            } catch (InterruptedException e) {
                return null;
            }
        } catch (UnknownHostException e2) {
            return null;
        }
    }
}
