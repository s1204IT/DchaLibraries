package com.android.packageinstaller.wear;

import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import com.android.packageinstaller.wear.PackageInstallerImpl;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

public class InstallTask {
    private PackageInstallerImpl.InstallListener mCallback;
    private IntentSender mCommitCallback;
    private final Context mContext;
    private String mPackageName;
    private ParcelFileDescriptor mParcelFileDescriptor;
    private PackageInstaller.Session mSession;
    private Exception mException = null;
    private int mErrorCode = 0;
    private String mErrorDesc = null;

    public InstallTask(Context context, String str, ParcelFileDescriptor parcelFileDescriptor, PackageInstallerImpl.InstallListener installListener, PackageInstaller.Session session, IntentSender intentSender) {
        this.mContext = context;
        this.mPackageName = str;
        this.mParcelFileDescriptor = parcelFileDescriptor;
        this.mCallback = installListener;
        this.mSession = session;
        this.mCommitCallback = intentSender;
    }

    public boolean isError() {
        return (this.mErrorCode == 0 && TextUtils.isEmpty(this.mErrorDesc)) ? false : true;
    }

    public void execute() throws Throwable {
        OutputStream outputStreamOpenWrite;
        Throwable th;
        Exception e;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("This method cannot be called from the UI thread.");
        }
        try {
            outputStreamOpenWrite = this.mSession.openWrite(this.mPackageName, 0L, -1L);
            try {
                try {
                    writeToOutputStreamFromAsset(outputStreamOpenWrite);
                    this.mSession.fsync(outputStreamOpenWrite);
                    if (outputStreamOpenWrite != null) {
                        try {
                            outputStreamOpenWrite.close();
                        } catch (Exception e2) {
                            e = e2;
                            if (this.mException == null) {
                                this.mException = e;
                                this.mErrorCode = -621;
                                this.mErrorDesc = "Could not close session stream";
                            }
                        }
                    }
                } catch (Exception e3) {
                    e = e3;
                    this.mException = e;
                    this.mErrorCode = -620;
                    this.mErrorDesc = "Could not write to stream";
                    if (outputStreamOpenWrite != null) {
                        try {
                            outputStreamOpenWrite.close();
                        } catch (Exception e4) {
                            e = e4;
                            if (this.mException == null) {
                            }
                        }
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                if (outputStreamOpenWrite != null) {
                    try {
                        outputStreamOpenWrite.close();
                    } catch (Exception e5) {
                        if (this.mException == null) {
                            this.mException = e5;
                            this.mErrorCode = -621;
                            this.mErrorDesc = "Could not close session stream";
                        }
                    }
                }
                throw th;
            }
        } catch (Exception e6) {
            outputStreamOpenWrite = null;
            e = e6;
        } catch (Throwable th3) {
            outputStreamOpenWrite = null;
            th = th3;
            if (outputStreamOpenWrite != null) {
            }
            throw th;
        }
        if (this.mErrorCode == 0) {
            this.mCallback.installBeginning();
            this.mSession.commit(this.mCommitCallback);
            this.mSession.close();
            return;
        }
        Log.e("InstallTask", "Exception while installing " + this.mPackageName + ": " + this.mErrorCode + ", " + this.mErrorDesc + ", " + this.mException);
        this.mSession.close();
        PackageInstallerImpl.InstallListener installListener = this.mCallback;
        int i = this.mErrorCode;
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(this.mPackageName);
        sb.append("]");
        sb.append(this.mErrorDesc);
        installListener.installFailed(i, sb.toString());
    }

    private boolean writeToOutputStreamFromAsset(OutputStream outputStream) throws Throwable {
        if (outputStream == null) {
            this.mErrorCode = -615;
            this.mErrorDesc = "Got a null OutputStream.";
            return false;
        }
        if (this.mParcelFileDescriptor == null || this.mParcelFileDescriptor.getFileDescriptor() == null) {
            this.mErrorCode = -603;
            this.mErrorDesc = "Could not get FD";
            return false;
        }
        ParcelFileDescriptor.AutoCloseInputStream autoCloseInputStream = null;
        try {
            try {
                byte[] bArr = new byte[8192];
                ParcelFileDescriptor.AutoCloseInputStream autoCloseInputStream2 = new ParcelFileDescriptor.AutoCloseInputStream(this.mParcelFileDescriptor);
                while (true) {
                    try {
                        int i = autoCloseInputStream2.read(bArr);
                        if (i <= -1) {
                            outputStream.flush();
                            safeClose(autoCloseInputStream2);
                            return true;
                        }
                        if (i > 0) {
                            outputStream.write(bArr, 0, i);
                        }
                    } catch (IOException e) {
                        e = e;
                        autoCloseInputStream = autoCloseInputStream2;
                        this.mErrorCode = -619;
                        this.mErrorDesc = "Reading from Asset FD or writing to temp file failed: " + e;
                        safeClose(autoCloseInputStream);
                        return false;
                    } catch (Throwable th) {
                        th = th;
                        autoCloseInputStream = autoCloseInputStream2;
                        safeClose(autoCloseInputStream);
                        throw th;
                    }
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (IOException e2) {
            e = e2;
        }
    }

    public static void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }
}
