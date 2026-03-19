package com.android.server.am;

import android.app.ApplicationErrorReport;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructTimeval;
import android.system.UnixSocketAddress;
import android.util.Slog;
import com.android.server.UiModeManagerService;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.InterruptedIOException;

final class NativeCrashListener extends Thread {
    static final boolean DEBUG = false;
    static final String DEBUGGERD_SOCKET_PATH = "/data/system/ndebugsocket";
    static final boolean MORE_DEBUG = false;
    static final long SOCKET_TIMEOUT_MILLIS = 10000;
    static final String TAG = "NativeCrashListener";
    final ActivityManagerService mAm;

    class NativeCrashReporter extends Thread {
        ProcessRecord mApp;
        String mCrashReport;
        int mSignal;

        NativeCrashReporter(ProcessRecord processRecord, int i, String str) {
            super("NativeCrashReport");
            this.mApp = processRecord;
            this.mSignal = i;
            this.mCrashReport = str;
        }

        @Override
        public void run() {
            try {
                ApplicationErrorReport.CrashInfo crashInfo = new ApplicationErrorReport.CrashInfo();
                crashInfo.exceptionClassName = "Native crash";
                crashInfo.exceptionMessage = Os.strsignal(this.mSignal);
                crashInfo.throwFileName = UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
                crashInfo.throwClassName = UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
                crashInfo.throwMethodName = UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
                crashInfo.stackTrace = this.mCrashReport;
                NativeCrashListener.this.mAm.handleApplicationCrashInner("native_crash", this.mApp, this.mApp.processName, crashInfo);
            } catch (Exception e) {
                Slog.e(NativeCrashListener.TAG, "Unable to report native crash", e);
            }
        }
    }

    NativeCrashListener(ActivityManagerService activityManagerService) {
        this.mAm = activityManagerService;
    }

    @Override
    public void run() throws Throwable {
        FileDescriptor fileDescriptorAccept;
        Exception e;
        byte[] bArr = new byte[1];
        File file = new File(DEBUGGERD_SOCKET_PATH);
        if (file.exists()) {
            file.delete();
        }
        try {
            FileDescriptor fileDescriptorSocket = Os.socket(OsConstants.AF_UNIX, OsConstants.SOCK_STREAM, 0);
            Os.bind(fileDescriptorSocket, UnixSocketAddress.createFileSystem(DEBUGGERD_SOCKET_PATH));
            Os.listen(fileDescriptorSocket, 1);
            Os.chmod(DEBUGGERD_SOCKET_PATH, 511);
            while (true) {
                try {
                    fileDescriptorAccept = Os.accept(fileDescriptorSocket, null);
                    if (fileDescriptorAccept != null) {
                        try {
                            try {
                                consumeNativeCrashData(fileDescriptorAccept);
                            } catch (Throwable th) {
                                th = th;
                                if (fileDescriptorAccept != null) {
                                    try {
                                        Os.write(fileDescriptorAccept, bArr, 0, 1);
                                    } catch (Exception e2) {
                                    }
                                    try {
                                        Os.close(fileDescriptorAccept);
                                    } catch (ErrnoException e3) {
                                    }
                                }
                                throw th;
                            }
                        } catch (Exception e4) {
                            e = e4;
                            Slog.w(TAG, "Error handling connection", e);
                            if (fileDescriptorAccept != null) {
                                try {
                                    Os.write(fileDescriptorAccept, bArr, 0, 1);
                                } catch (Exception e5) {
                                }
                                Os.close(fileDescriptorAccept);
                            }
                        }
                    }
                    if (fileDescriptorAccept != null) {
                        try {
                            Os.write(fileDescriptorAccept, bArr, 0, 1);
                        } catch (Exception e6) {
                        }
                        try {
                            Os.close(fileDescriptorAccept);
                        } catch (ErrnoException e7) {
                        }
                    }
                } catch (Exception e8) {
                    fileDescriptorAccept = null;
                    e = e8;
                } catch (Throwable th2) {
                    th = th2;
                    fileDescriptorAccept = null;
                }
            }
        } catch (Exception e9) {
            Slog.e(TAG, "Unable to init native debug socket!", e9);
        }
    }

    static int unpackInt(byte[] bArr, int i) {
        return (bArr[i + 3] & 255) | ((bArr[i] & 255) << 24) | ((bArr[i + 1] & 255) << 16) | ((bArr[i + 2] & 255) << 8);
    }

    static int readExactly(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2) throws ErrnoException, InterruptedIOException {
        int i3 = 0;
        while (i2 > 0) {
            int i4 = Os.read(fileDescriptor, bArr, i + i3, i2);
            if (i4 <= 0) {
                return -1;
            }
            i2 -= i4;
            i3 += i4;
        }
        return i3;
    }

    void consumeNativeCrashData(FileDescriptor fileDescriptor) {
        ProcessRecord processRecord;
        byte[] bArr = new byte[4096];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(4096);
        try {
            StructTimeval structTimevalFromMillis = StructTimeval.fromMillis(10000L);
            Os.setsockoptTimeval(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_RCVTIMEO, structTimevalFromMillis);
            Os.setsockoptTimeval(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_SNDTIMEO, structTimevalFromMillis);
            if (readExactly(fileDescriptor, bArr, 0, 8) != 8) {
                Slog.e(TAG, "Unable to read from debuggerd");
                return;
            }
            int iUnpackInt = unpackInt(bArr, 0);
            int iUnpackInt2 = unpackInt(bArr, 4);
            if (iUnpackInt > 0) {
                synchronized (this.mAm.mPidsSelfLocked) {
                    processRecord = this.mAm.mPidsSelfLocked.get(iUnpackInt);
                }
                if (processRecord != null) {
                    if (processRecord.persistent) {
                        return;
                    }
                    while (true) {
                        int i = Os.read(fileDescriptor, bArr, 0, bArr.length);
                        if (i > 0) {
                            int i2 = i - 1;
                            if (bArr[i2] == 0) {
                                byteArrayOutputStream.write(bArr, 0, i2);
                                break;
                            }
                            byteArrayOutputStream.write(bArr, 0, i);
                        }
                        if (i <= 0) {
                            break;
                        }
                    }
                    synchronized (this.mAm) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            processRecord.crashing = true;
                            processRecord.forceCrashReport = true;
                        } catch (Throwable th) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    new NativeCrashReporter(processRecord, iUnpackInt2, new String(byteArrayOutputStream.toByteArray(), "UTF-8")).start();
                    return;
                }
                Slog.w(TAG, "Couldn't find ProcessRecord for pid " + iUnpackInt);
                return;
            }
            Slog.e(TAG, "Bogus pid!");
        } catch (Exception e) {
            Slog.e(TAG, "Exception dealing with report", e);
        }
    }
}
