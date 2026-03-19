package com.android.commands.bu;

import android.app.backup.IBackupManager;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.system.OsConstants;
import android.util.Log;
import java.io.IOException;
import java.util.ArrayList;

public final class Backup {
    static final String TAG = "bu";
    static String[] mArgs;
    IBackupManager mBackupManager;
    int mNextArg;

    public static void main(String[] strArr) throws Throwable {
        Log.d(TAG, "Beginning: " + strArr[0]);
        mArgs = strArr;
        try {
            new Backup().run();
        } catch (Exception e) {
            Log.e(TAG, "Error running backup/restore", e);
        }
        Log.d(TAG, "Finished.");
    }

    public void run() throws Throwable {
        this.mBackupManager = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
        if (this.mBackupManager == null) {
            Log.e(TAG, "Can't obtain Backup Manager binder");
            return;
        }
        String strNextArg = nextArg();
        if (strNextArg.equals("backup")) {
            doBackup(OsConstants.STDOUT_FILENO);
        } else if (strNextArg.equals("restore")) {
            doRestore(OsConstants.STDIN_FILENO);
        } else {
            showUsage();
        }
    }

    private void doBackup(int i) throws Throwable {
        ParcelFileDescriptor parcelFileDescriptorAdoptFd;
        String str;
        StringBuilder sb;
        ArrayList arrayList = new ArrayList();
        boolean z = true;
        boolean z2 = true;
        boolean z3 = false;
        boolean z4 = false;
        boolean z5 = false;
        boolean z6 = false;
        boolean z7 = false;
        boolean z8 = false;
        while (true) {
            String strNextArg = nextArg();
            if (strNextArg == null) {
                break;
            }
            if (!strNextArg.startsWith("-")) {
                arrayList.add(strNextArg);
            } else if ("-apk".equals(strNextArg)) {
                z3 = true;
            } else if ("-noapk".equals(strNextArg)) {
                z3 = false;
            } else if ("-obb".equals(strNextArg)) {
                z4 = true;
            } else if ("-noobb".equals(strNextArg)) {
                z4 = false;
            } else if ("-shared".equals(strNextArg)) {
                z5 = true;
            } else if ("-noshared".equals(strNextArg)) {
                z5 = false;
            } else if ("-system".equals(strNextArg)) {
                z = true;
            } else if ("-nosystem".equals(strNextArg)) {
                z = false;
            } else if ("-widgets".equals(strNextArg)) {
                z6 = true;
            } else if ("-nowidgets".equals(strNextArg)) {
                z6 = false;
            } else if ("-all".equals(strNextArg)) {
                z7 = true;
            } else if ("-compress".equals(strNextArg)) {
                z2 = true;
            } else if ("-nocompress".equals(strNextArg)) {
                z2 = false;
            } else if ("-keyvalue".equals(strNextArg)) {
                z8 = true;
            } else if ("-nokeyvalue".equals(strNextArg)) {
                z8 = false;
            } else {
                Log.w(TAG, "Unknown backup flag " + strNextArg);
            }
        }
        if (z7 && arrayList.size() > 0) {
            Log.w(TAG, "-all passed for backup along with specific package names");
        }
        if (!z7 && !z5 && arrayList.size() == 0) {
            Log.e(TAG, "no backup packages supplied and neither -shared nor -all given");
            return;
        }
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            try {
                parcelFileDescriptorAdoptFd = ParcelFileDescriptor.adoptFd(i);
            } catch (RemoteException e) {
            }
        } catch (Throwable th) {
            th = th;
            parcelFileDescriptorAdoptFd = parcelFileDescriptor;
        }
        try {
            this.mBackupManager.adbBackup(parcelFileDescriptorAdoptFd, z3, z4, z5, z6, z7, z, z2, z8, (String[]) arrayList.toArray(new String[arrayList.size()]));
            if (parcelFileDescriptorAdoptFd != null) {
                try {
                    parcelFileDescriptorAdoptFd.close();
                } catch (IOException e2) {
                    e = e2;
                    str = TAG;
                    sb = new StringBuilder();
                    sb.append("IO error closing output for backup: ");
                    sb.append(e.getMessage());
                    Log.e(str, sb.toString());
                }
            }
        } catch (RemoteException e3) {
            parcelFileDescriptor = parcelFileDescriptorAdoptFd;
            Log.e(TAG, "Unable to invoke backup manager for backup");
            if (parcelFileDescriptor != null) {
                try {
                    parcelFileDescriptor.close();
                } catch (IOException e4) {
                    e = e4;
                    str = TAG;
                    sb = new StringBuilder();
                    sb.append("IO error closing output for backup: ");
                    sb.append(e.getMessage());
                    Log.e(str, sb.toString());
                }
            }
        } catch (Throwable th2) {
            th = th2;
            Throwable th3 = th;
            if (parcelFileDescriptorAdoptFd == null) {
                throw th3;
            }
            try {
                parcelFileDescriptorAdoptFd.close();
                throw th3;
            } catch (IOException e5) {
                Log.e(TAG, "IO error closing output for backup: " + e5.getMessage());
                throw th3;
            }
        }
    }

    private void doRestore(int i) throws Throwable {
        ParcelFileDescriptor parcelFileDescriptorAdoptFd;
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            try {
                try {
                    parcelFileDescriptorAdoptFd = ParcelFileDescriptor.adoptFd(i);
                } catch (RemoteException e) {
                }
            } catch (Throwable th) {
                th = th;
            }
            try {
                this.mBackupManager.adbRestore(parcelFileDescriptorAdoptFd);
            } catch (RemoteException e2) {
                parcelFileDescriptor = parcelFileDescriptorAdoptFd;
                Log.e(TAG, "Unable to invoke backup manager for restore");
                if (parcelFileDescriptor == null) {
                    return;
                } else {
                    parcelFileDescriptor.close();
                }
            } catch (Throwable th2) {
                parcelFileDescriptor = parcelFileDescriptorAdoptFd;
                th = th2;
                if (parcelFileDescriptor != null) {
                    try {
                        parcelFileDescriptor.close();
                    } catch (IOException e3) {
                    }
                }
                throw th;
            }
            if (parcelFileDescriptorAdoptFd != null) {
                parcelFileDescriptorAdoptFd.close();
            }
        } catch (IOException e4) {
        }
    }

    private static void showUsage() {
        System.err.println(" backup [-f FILE] [-apk|-noapk] [-obb|-noobb] [-shared|-noshared] [-all]");
        System.err.println("        [-system|-nosystem] [-keyvalue|-nokeyvalue] [PACKAGE...]");
        System.err.println("     write an archive of the device's data to FILE [default=backup.adb]");
        System.err.println("     package list optional if -all/-shared are supplied");
        System.err.println("     -apk/-noapk: do/don't back up .apk files (default -noapk)");
        System.err.println("     -obb/-noobb: do/don't back up .obb files (default -noobb)");
        System.err.println("     -shared|-noshared: do/don't back up shared storage (default -noshared)");
        System.err.println("     -all: back up all installed applications");
        System.err.println("     -system|-nosystem: include system apps in -all (default -system)");
        System.err.println("     -keyvalue|-nokeyvalue: include apps that perform key/value backups.");
        System.err.println("         (default -nokeyvalue)");
        System.err.println(" restore FILE             restore device contents from FILE");
    }

    private String nextArg() {
        if (this.mNextArg >= mArgs.length) {
            return null;
        }
        String str = mArgs[this.mNextArg];
        this.mNextArg++;
        return str;
    }
}
