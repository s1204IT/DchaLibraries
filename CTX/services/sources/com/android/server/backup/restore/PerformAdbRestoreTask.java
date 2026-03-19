package com.android.server.backup.restore;

import android.app.IBackupAgent;
import android.app.backup.IFullBackupRestoreObserver;
import android.content.pm.ApplicationInfo;
import android.content.pm.Signature;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupPasswordManager;
import com.android.server.backup.PackageManagerBackupAgent;
import com.android.server.backup.fullbackup.FullBackupObbConnection;
import com.android.server.backup.utils.FullBackupRestoreObserverUtils;
import com.android.server.backup.utils.PasswordUtils;
import com.android.server.pm.PackageManagerService;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.InflaterInputStream;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class PerformAdbRestoreTask implements Runnable {
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    private long mAppVersion;
    private final BackupManagerService mBackupManagerService;
    private long mBytes;
    private final String mCurrentPassword;
    private final String mDecryptPassword;
    private final ParcelFileDescriptor mInputFile;
    private final AtomicBoolean mLatchObject;
    private FullBackupObbConnection mObbConnection;
    private IFullBackupRestoreObserver mObserver;
    private final PackageManagerBackupAgent mPackageManagerBackupAgent;
    private final RestoreDeleteObserver mDeleteObserver = new RestoreDeleteObserver();
    private ParcelFileDescriptor[] mPipes = null;
    private byte[] mWidgetData = null;
    private final HashMap<String, RestorePolicy> mPackagePolicies = new HashMap<>();
    private final HashMap<String, String> mPackageInstallers = new HashMap<>();
    private final HashMap<String, Signature[]> mManifestSignatures = new HashMap<>();
    private final HashSet<String> mClearedPackages = new HashSet<>();
    private IBackupAgent mAgent = null;
    private String mAgentPackage = null;
    private ApplicationInfo mTargetApp = null;

    static long access$014(PerformAdbRestoreTask performAdbRestoreTask, long j) {
        long j2 = performAdbRestoreTask.mBytes + j;
        performAdbRestoreTask.mBytes = j2;
        return j2;
    }

    private static class RestoreFinishedRunnable implements Runnable {
        private final IBackupAgent mAgent;
        private final BackupManagerService mBackupManagerService;
        private final int mToken;

        RestoreFinishedRunnable(IBackupAgent iBackupAgent, int i, BackupManagerService backupManagerService) {
            this.mAgent = iBackupAgent;
            this.mToken = i;
            this.mBackupManagerService = backupManagerService;
        }

        @Override
        public void run() {
            try {
                this.mAgent.doRestoreFinished(this.mToken, this.mBackupManagerService.getBackupManagerBinder());
            } catch (RemoteException e) {
            }
        }
    }

    public PerformAdbRestoreTask(BackupManagerService backupManagerService, ParcelFileDescriptor parcelFileDescriptor, String str, String str2, IFullBackupRestoreObserver iFullBackupRestoreObserver, AtomicBoolean atomicBoolean) {
        this.mObbConnection = null;
        this.mBackupManagerService = backupManagerService;
        this.mInputFile = parcelFileDescriptor;
        this.mCurrentPassword = str;
        this.mDecryptPassword = str2;
        this.mObserver = iFullBackupRestoreObserver;
        this.mLatchObject = atomicBoolean;
        this.mPackageManagerBackupAgent = backupManagerService.makeMetadataAgent();
        this.mObbConnection = new FullBackupObbConnection(backupManagerService);
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Preconditions.checkNotNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
        this.mClearedPackages.add(PackageManagerService.PLATFORM_PACKAGE_NAME);
        this.mClearedPackages.add(BackupManagerService.SETTINGS_PACKAGE);
    }

    @Override
    public void run() throws Throwable {
        Slog.i(BackupManagerService.TAG, "--- Performing full-dataset restore ---");
        this.mObbConnection.establish();
        this.mObserver = FullBackupRestoreObserverUtils.sendStartRestore(this.mObserver);
        if (Environment.getExternalStorageState().equals("mounted")) {
            this.mPackagePolicies.put(BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE, RestorePolicy.ACCEPT);
        }
        FileInputStream fileInputStream = null;
        try {
            if (!this.mBackupManagerService.backupPasswordMatches(this.mCurrentPassword)) {
                Slog.w(BackupManagerService.TAG, "Backup password mismatch; aborting");
                tearDownPipes();
                tearDownAgent(this.mTargetApp, true);
                try {
                    this.mInputFile.close();
                } catch (IOException e) {
                    Slog.w(BackupManagerService.TAG, "Close of restore data pipe threw", e);
                }
                synchronized (this.mLatchObject) {
                    this.mLatchObject.set(true);
                    this.mLatchObject.notifyAll();
                }
                this.mObbConnection.tearDown();
                this.mObserver = FullBackupRestoreObserverUtils.sendEndRestore(this.mObserver);
                Slog.d(BackupManagerService.TAG, "Full restore pass complete.");
                this.mBackupManagerService.getWakelock().release();
                return;
            }
            this.mBytes = 0L;
            FileInputStream fileInputStream2 = new FileInputStream(this.mInputFile.getFileDescriptor());
            try {
                InputStream backupFileHeaderAndReturnTarStream = parseBackupFileHeaderAndReturnTarStream(fileInputStream2, this.mDecryptPassword);
                if (backupFileHeaderAndReturnTarStream == null) {
                    tearDownPipes();
                    tearDownAgent(this.mTargetApp, true);
                    try {
                        fileInputStream2.close();
                        this.mInputFile.close();
                    } catch (IOException e2) {
                        Slog.w(BackupManagerService.TAG, "Close of restore data pipe threw", e2);
                    }
                    synchronized (this.mLatchObject) {
                        this.mLatchObject.set(true);
                        this.mLatchObject.notifyAll();
                    }
                    this.mObbConnection.tearDown();
                    this.mObserver = FullBackupRestoreObserverUtils.sendEndRestore(this.mObserver);
                    Slog.d(BackupManagerService.TAG, "Full restore pass complete.");
                    this.mBackupManagerService.getWakelock().release();
                    return;
                }
                do {
                } while (restoreOneFile(backupFileHeaderAndReturnTarStream, false, new byte[32768], null, true, this.mBackupManagerService.generateRandomIntegerToken(), null));
                tearDownPipes();
                tearDownAgent(this.mTargetApp, true);
                try {
                    fileInputStream2.close();
                    this.mInputFile.close();
                } catch (IOException e3) {
                    Slog.w(BackupManagerService.TAG, "Close of restore data pipe threw", e3);
                }
                synchronized (this.mLatchObject) {
                    this.mLatchObject.set(true);
                    this.mLatchObject.notifyAll();
                }
            } catch (IOException e4) {
                fileInputStream = fileInputStream2;
                Slog.e(BackupManagerService.TAG, "Unable to read restore input");
                tearDownPipes();
                tearDownAgent(this.mTargetApp, true);
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e5) {
                        Slog.w(BackupManagerService.TAG, "Close of restore data pipe threw", e5);
                        synchronized (this.mLatchObject) {
                            this.mLatchObject.set(true);
                            this.mLatchObject.notifyAll();
                        }
                        this.mObbConnection.tearDown();
                        this.mObserver = FullBackupRestoreObserverUtils.sendEndRestore(this.mObserver);
                        Slog.d(BackupManagerService.TAG, "Full restore pass complete.");
                        this.mBackupManagerService.getWakelock().release();
                    }
                }
                this.mInputFile.close();
                synchronized (this.mLatchObject) {
                }
            } catch (Throwable th) {
                th = th;
                fileInputStream = fileInputStream2;
                tearDownPipes();
                tearDownAgent(this.mTargetApp, true);
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e6) {
                        Slog.w(BackupManagerService.TAG, "Close of restore data pipe threw", e6);
                        synchronized (this.mLatchObject) {
                            this.mLatchObject.set(true);
                            this.mLatchObject.notifyAll();
                        }
                        this.mObbConnection.tearDown();
                        this.mObserver = FullBackupRestoreObserverUtils.sendEndRestore(this.mObserver);
                        Slog.d(BackupManagerService.TAG, "Full restore pass complete.");
                        this.mBackupManagerService.getWakelock().release();
                        throw th;
                    }
                }
                this.mInputFile.close();
                synchronized (this.mLatchObject) {
                }
            }
            this.mObbConnection.tearDown();
            this.mObserver = FullBackupRestoreObserverUtils.sendEndRestore(this.mObserver);
            Slog.d(BackupManagerService.TAG, "Full restore pass complete.");
            this.mBackupManagerService.getWakelock().release();
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private static void readFullyOrThrow(InputStream inputStream, byte[] bArr) throws IOException {
        int i = 0;
        while (i < bArr.length) {
            int i2 = inputStream.read(bArr, i, bArr.length - i);
            if (i2 <= 0) {
                throw new IOException("Couldn't fully read data");
            }
            i += i2;
        }
    }

    @VisibleForTesting
    public static InputStream parseBackupFileHeaderAndReturnTarStream(InputStream inputStream, String str) throws IOException {
        boolean z;
        byte[] bArr = new byte[BackupManagerService.BACKUP_FILE_HEADER_MAGIC.length()];
        readFullyOrThrow(inputStream, bArr);
        boolean z2 = false;
        if (Arrays.equals(BackupManagerService.BACKUP_FILE_HEADER_MAGIC.getBytes("UTF-8"), bArr)) {
            String headerLine = readHeaderLine(inputStream);
            int i = Integer.parseInt(headerLine);
            if (i <= 5) {
                boolean z3 = true;
                boolean z4 = i == 1;
                z = Integer.parseInt(readHeaderLine(inputStream)) != 0;
                String headerLine2 = readHeaderLine(inputStream);
                if (!headerLine2.equals("none")) {
                    if (str != null && str.length() > 0) {
                        inputStream = decodeAesHeaderAndInitialize(str, headerLine2, z4, inputStream);
                        if (inputStream == null) {
                        }
                    } else {
                        Slog.w(BackupManagerService.TAG, "Archive is encrypted but no password given");
                    }
                    z3 = false;
                }
                z2 = z3;
            } else {
                Slog.w(BackupManagerService.TAG, "Wrong header version: " + headerLine);
                z = false;
            }
        } else {
            Slog.w(BackupManagerService.TAG, "Didn't read the right header magic");
            z = false;
        }
        if (z2) {
            return z ? new InflaterInputStream(inputStream) : inputStream;
        }
        Slog.w(BackupManagerService.TAG, "Invalid restore data; aborting.");
        return null;
    }

    private static String readHeaderLine(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder(80);
        while (true) {
            int i = inputStream.read();
            if (i < 0 || i == 10) {
                break;
            }
            sb.append((char) i);
        }
        return sb.toString();
    }

    private static InputStream attemptMasterKeyDecryption(String str, String str2, byte[] bArr, byte[] bArr2, int i, String str3, String str4, InputStream inputStream, boolean z) {
        CipherInputStream cipherInputStream = null;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKey secretKeyBuildPasswordKey = PasswordUtils.buildPasswordKey(str2, str, bArr, i);
            cipher.init(2, new SecretKeySpec(secretKeyBuildPasswordKey.getEncoded(), "AES"), new IvParameterSpec(PasswordUtils.hexToByteArray(str3)));
            byte[] bArrDoFinal = cipher.doFinal(PasswordUtils.hexToByteArray(str4));
            int i2 = bArrDoFinal[0] + 1;
            byte[] bArrCopyOfRange = Arrays.copyOfRange(bArrDoFinal, 1, i2);
            int i3 = i2 + 1;
            int i4 = bArrDoFinal[i2] + i3;
            byte[] bArrCopyOfRange2 = Arrays.copyOfRange(bArrDoFinal, i3, i4);
            int i5 = i4 + 1;
            if (Arrays.equals(PasswordUtils.makeKeyChecksum(str2, bArrCopyOfRange2, bArr2, i), Arrays.copyOfRange(bArrDoFinal, i5, bArrDoFinal[i4] + i5))) {
                cipher.init(2, new SecretKeySpec(bArrCopyOfRange2, "AES"), new IvParameterSpec(bArrCopyOfRange));
                cipherInputStream = new CipherInputStream(inputStream, cipher);
            } else if (z) {
                Slog.w(BackupManagerService.TAG, "Incorrect password");
            }
        } catch (InvalidAlgorithmParameterException e) {
            if (z) {
                Slog.e(BackupManagerService.TAG, "Needed parameter spec unavailable!", e);
            }
        } catch (InvalidKeyException e2) {
            if (z) {
                Slog.w(BackupManagerService.TAG, "Illegal password; aborting");
            }
        } catch (NoSuchAlgorithmException e3) {
            if (z) {
                Slog.e(BackupManagerService.TAG, "Needed decryption algorithm unavailable!");
            }
        } catch (BadPaddingException e4) {
            if (z) {
                Slog.w(BackupManagerService.TAG, "Incorrect password");
            }
        } catch (IllegalBlockSizeException e5) {
            if (z) {
                Slog.w(BackupManagerService.TAG, "Invalid block size in master key");
            }
        } catch (NoSuchPaddingException e6) {
            if (z) {
                Slog.e(BackupManagerService.TAG, "Needed padding mechanism unavailable!");
            }
        }
        return cipherInputStream;
    }

    private static InputStream decodeAesHeaderAndInitialize(String str, String str2, boolean z, InputStream inputStream) {
        InputStream inputStreamAttemptMasterKeyDecryption = null;
        try {
            if (str2.equals(PasswordUtils.ENCRYPTION_ALGORITHM_NAME)) {
                byte[] bArrHexToByteArray = PasswordUtils.hexToByteArray(readHeaderLine(inputStream));
                byte[] bArrHexToByteArray2 = PasswordUtils.hexToByteArray(readHeaderLine(inputStream));
                int i = Integer.parseInt(readHeaderLine(inputStream));
                String headerLine = readHeaderLine(inputStream);
                String headerLine2 = readHeaderLine(inputStream);
                InputStream inputStreamAttemptMasterKeyDecryption2 = attemptMasterKeyDecryption(str, BackupPasswordManager.PBKDF_CURRENT, bArrHexToByteArray, bArrHexToByteArray2, i, headerLine, headerLine2, inputStream, false);
                if (inputStreamAttemptMasterKeyDecryption2 == null && z) {
                    try {
                        inputStreamAttemptMasterKeyDecryption = attemptMasterKeyDecryption(str, BackupPasswordManager.PBKDF_FALLBACK, bArrHexToByteArray, bArrHexToByteArray2, i, headerLine, headerLine2, inputStream, true);
                    } catch (IOException e) {
                        inputStreamAttemptMasterKeyDecryption = inputStreamAttemptMasterKeyDecryption2;
                        Slog.w(BackupManagerService.TAG, "Can't read input header");
                    } catch (NumberFormatException e2) {
                        inputStreamAttemptMasterKeyDecryption = inputStreamAttemptMasterKeyDecryption2;
                        Slog.w(BackupManagerService.TAG, "Can't parse restore data header");
                    }
                } else {
                    inputStreamAttemptMasterKeyDecryption = inputStreamAttemptMasterKeyDecryption2;
                }
            } else {
                Slog.w(BackupManagerService.TAG, "Unsupported encryption method: " + str2);
            }
        } catch (IOException e3) {
        } catch (NumberFormatException e4) {
        }
        return inputStreamAttemptMasterKeyDecryption;
    }

    boolean restoreOneFile(

    static class AnonymousClass2 {
        static final int[] $SwitchMap$com$android$server$backup$restore$RestorePolicy = new int[RestorePolicy.values().length];

        static {
            try {
                $SwitchMap$com$android$server$backup$restore$RestorePolicy[RestorePolicy.IGNORE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$backup$restore$RestorePolicy[RestorePolicy.ACCEPT_IF_APK.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$backup$restore$RestorePolicy[RestorePolicy.ACCEPT.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    private static boolean isCanonicalFilePath(String str) {
        if (str.contains("..") || str.contains("//")) {
            return false;
        }
        return true;
    }

    private void setUpPipes() throws IOException {
        this.mPipes = ParcelFileDescriptor.createPipe();
    }

    private void tearDownPipes() {
        if (this.mPipes != null) {
            try {
                this.mPipes[0].close();
                this.mPipes[0] = null;
                this.mPipes[1].close();
                this.mPipes[1] = null;
            } catch (IOException e) {
                Slog.w(BackupManagerService.TAG, "Couldn't close agent pipes", e);
            }
            this.mPipes = null;
        }
    }

    private void tearDownAgent(ApplicationInfo applicationInfo, boolean z) {
        if (this.mAgent != null) {
            if (z) {
                try {
                    int iGenerateRandomIntegerToken = this.mBackupManagerService.generateRandomIntegerToken();
                    long fullBackupAgentTimeoutMillis = this.mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis();
                    AdbRestoreFinishedLatch adbRestoreFinishedLatch = new AdbRestoreFinishedLatch(this.mBackupManagerService, iGenerateRandomIntegerToken);
                    this.mBackupManagerService.prepareOperationTimeout(iGenerateRandomIntegerToken, fullBackupAgentTimeoutMillis, adbRestoreFinishedLatch, 1);
                    if (this.mTargetApp.processName.equals("system")) {
                        new Thread(new RestoreFinishedRunnable(this.mAgent, iGenerateRandomIntegerToken, this.mBackupManagerService), "restore-sys-finished-runner").start();
                    } else {
                        this.mAgent.doRestoreFinished(iGenerateRandomIntegerToken, this.mBackupManagerService.getBackupManagerBinder());
                    }
                    adbRestoreFinishedLatch.await();
                } catch (RemoteException e) {
                    Slog.d(BackupManagerService.TAG, "Lost app trying to shut down");
                }
            }
            this.mBackupManagerService.tearDownAgentAndKill(applicationInfo);
            this.mAgent = null;
        }
    }
}
