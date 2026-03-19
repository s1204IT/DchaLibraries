package com.android.server.backup.utils;

import android.content.Context;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.FileMetadata;
import com.android.server.backup.restore.RestoreDeleteObserver;
import com.android.server.backup.restore.RestorePolicy;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public class RestoreUtils {
    public static boolean installApk(InputStream inputStream, Context context, RestoreDeleteObserver restoreDeleteObserver, HashMap<String, Signature[]> map, HashMap<String, RestorePolicy> map2, FileMetadata fileMetadata, String str, BytesReadListener bytesReadListener) throws Exception {
        Throwable th;
        Slog.d(BackupManagerService.TAG, "Installing from backup: " + fileMetadata.packageName);
        try {
            Throwable th2 = null;
            new LocalIntentReceiver();
            PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(1);
            sessionParams.setInstallerPackageName(str);
            int iCreateSession = packageInstaller.createSession(sessionParams);
            try {
                PackageInstaller.Session sessionOpenSession = packageInstaller.openSession(iCreateSession);
                try {
                    OutputStream outputStreamOpenWrite = sessionOpenSession.openWrite(fileMetadata.packageName, 0L, fileMetadata.size);
                    try {
                        byte[] bArr = new byte[32768];
                        long j = fileMetadata.size;
                        while (j > 0) {
                            int i = inputStream.read(bArr, 0, (int) (((long) bArr.length) < j ? bArr.length : j));
                            if (i >= 0) {
                                bytesReadListener.onBytesRead(i);
                            }
                            outputStreamOpenWrite.write(bArr, 0, i);
                            j -= (long) i;
                        }
                        if (outputStreamOpenWrite != null) {
                            $closeResource(null, outputStreamOpenWrite);
                        }
                        sessionOpenSession.abandon();
                        return map2.get(fileMetadata.packageName) == RestorePolicy.ACCEPT;
                    } catch (Throwable th3) {
                        th = th3;
                        th = null;
                        if (outputStreamOpenWrite != null) {
                        }
                        throw th;
                    }
                } finally {
                    if (sessionOpenSession != null) {
                        $closeResource(null, sessionOpenSession);
                    }
                }
            } catch (Exception e) {
                packageInstaller.abandonSession(iCreateSession);
                throw e;
            }
        } catch (IOException e2) {
            Slog.e(BackupManagerService.TAG, "Unable to transcribe restored apk for install");
            return false;
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

    private static class LocalIntentReceiver {
        private IIntentSender.Stub mLocalSender;
        private final Object mLock;

        @GuardedBy("mLock")
        private Intent mResult;

        private LocalIntentReceiver() {
            this.mLock = new Object();
            this.mResult = null;
            this.mLocalSender = new IIntentSender.Stub() {
                public void send(int i, Intent intent, String str, IBinder iBinder, IIntentReceiver iIntentReceiver, String str2, Bundle bundle) {
                    synchronized (LocalIntentReceiver.this.mLock) {
                        LocalIntentReceiver.this.mResult = intent;
                        LocalIntentReceiver.this.mLock.notifyAll();
                    }
                }
            };
        }

        public IntentSender getIntentSender() {
            return new IntentSender(this.mLocalSender);
        }

        public Intent getResult() {
            Intent intent;
            synchronized (this.mLock) {
                while (this.mResult == null) {
                    try {
                        this.mLock.wait();
                    } catch (InterruptedException e) {
                    }
                }
                intent = this.mResult;
            }
            return intent;
        }
    }
}
