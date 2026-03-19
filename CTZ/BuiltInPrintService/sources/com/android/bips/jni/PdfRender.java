package com.android.bips.jni;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import com.android.bips.render.IPdfRender;
import com.android.bips.render.PdfRenderService;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class PdfRender {
    private static final boolean DEBUG = false;
    private static PdfRender sInstance;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            PdfRender.this.mService = IPdfRender.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.w(PdfRender.TAG, "PdfRender service unexpectedly disconnected, reconnecting");
            PdfRender.this.mService = null;
            PdfRender.this.mContext.bindService(PdfRender.this.mIntent, this, 1);
        }
    };
    private final Context mContext;
    private String mCurrentFile;
    private final Intent mIntent;
    private IPdfRender mService;
    private static final String TAG = PdfRender.class.getSimpleName();
    private static final Object sLock = new Object();

    public static PdfRender getInstance(Context context) {
        PdfRender pdfRender;
        synchronized (sLock) {
            if (sInstance == null && context != null) {
                sInstance = new PdfRender(context.getApplicationContext());
            }
            pdfRender = sInstance;
        }
        return pdfRender;
    }

    private PdfRender(Context context) {
        this.mContext = context;
        this.mIntent = new Intent(context, (Class<?>) PdfRenderService.class);
        context.bindService(this.mIntent, this.mConnection, 1);
    }

    public void close() {
        this.mContext.unbindService(this.mConnection);
        this.mService = null;
        synchronized (sLock) {
            sInstance = null;
        }
    }

    private int openDocument(String str) {
        if (this.mService == null) {
            return 0;
        }
        if (this.mCurrentFile != null && !this.mCurrentFile.equals(str)) {
            closeDocument();
        }
        try {
            return this.mService.openDocument(ParcelFileDescriptor.open(new File(str), 268435456));
        } catch (RemoteException | FileNotFoundException e) {
            Log.w(TAG, "Failed to open " + str, e);
            return 0;
        }
    }

    public SizeD getPageSize(int i) {
        if (this.mService == null) {
            return null;
        }
        try {
            return this.mService.getPageSize(i - 1);
        } catch (RemoteException | IllegalArgumentException e) {
            Log.w(TAG, "getPageWidth failed", e);
            return null;
        }
    }

    public boolean renderPageStripe(int i, int i2, int i3, int i4, double d, ByteBuffer byteBuffer) {
        if (this.mService == null) {
            return false;
        }
        try {
            System.currentTimeMillis();
            int i5 = i3 * i4 * 3;
            byte[] bArr = new byte[131072];
            ParcelFileDescriptor.AutoCloseInputStream autoCloseInputStream = new ParcelFileDescriptor.AutoCloseInputStream(this.mService.renderPageStripe(i - 1, i2, i3, i4, d));
            Throwable th = null;
            while (true) {
                try {
                    try {
                        int i6 = autoCloseInputStream.read(bArr, 0, bArr.length);
                        if (i6 <= 0) {
                            break;
                        }
                        byteBuffer.put(bArr, 0, i6);
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                } catch (Throwable th3) {
                    Throwable th4 = th;
                    if (th4 == null) {
                        autoCloseInputStream.close();
                        throw th3;
                    }
                    try {
                        autoCloseInputStream.close();
                        throw th3;
                    } catch (Throwable th5) {
                        th4.addSuppressed(th5);
                        throw th3;
                    }
                }
            }
            autoCloseInputStream.close();
            if (byteBuffer.position() == i5) {
                return true;
            }
            Log.w(TAG, "Render failed: expected " + byteBuffer.position() + ", got " + i5 + " bytes");
            return false;
        } catch (RemoteException | IOException | IllegalArgumentException | OutOfMemoryError e) {
            Log.w(TAG, "Render failed", e);
            return false;
        }
    }

    public void closeDocument() {
        if (this.mService == null) {
            return;
        }
        try {
            this.mService.closeDocument();
        } catch (RemoteException e) {
        }
    }
}
