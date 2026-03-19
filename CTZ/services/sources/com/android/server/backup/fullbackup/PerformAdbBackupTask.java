package com.android.server.backup.fullbackup;

import android.app.backup.IFullBackupRestoreObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.AppWidgetBackupBridge;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupPasswordManager;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.KeyValueAdbBackupEngine;
import com.android.server.backup.utils.AppBackupUtils;
import com.android.server.backup.utils.PasswordUtils;
import com.android.server.job.controllers.JobStatus;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class PerformAdbBackupTask extends FullBackupTask implements BackupRestoreTask {
    private BackupManagerService backupManagerService;
    boolean mAllApps;
    FullBackupEngine mBackupEngine;
    boolean mCompress;
    private final int mCurrentOpToken;
    String mCurrentPassword;
    PackageInfo mCurrentTarget;
    DeflaterOutputStream mDeflater;
    boolean mDoWidgets;
    String mEncryptPassword;
    boolean mIncludeApks;
    boolean mIncludeObbs;
    boolean mIncludeShared;
    boolean mIncludeSystem;
    boolean mKeyValue;
    final AtomicBoolean mLatch;
    ParcelFileDescriptor mOutputFile;
    ArrayList<String> mPackages;

    public PerformAdbBackupTask(BackupManagerService backupManagerService, ParcelFileDescriptor parcelFileDescriptor, IFullBackupRestoreObserver iFullBackupRestoreObserver, boolean z, boolean z2, boolean z3, boolean z4, String str, String str2, boolean z5, boolean z6, boolean z7, boolean z8, String[] strArr, AtomicBoolean atomicBoolean) {
        ArrayList<String> arrayList;
        super(iFullBackupRestoreObserver);
        this.backupManagerService = backupManagerService;
        this.mCurrentOpToken = backupManagerService.generateRandomIntegerToken();
        this.mLatch = atomicBoolean;
        this.mOutputFile = parcelFileDescriptor;
        this.mIncludeApks = z;
        this.mIncludeObbs = z2;
        this.mIncludeShared = z3;
        this.mDoWidgets = z4;
        this.mAllApps = z5;
        this.mIncludeSystem = z6;
        if (strArr == null) {
            arrayList = new ArrayList<>();
        } else {
            arrayList = new ArrayList<>(Arrays.asList(strArr));
        }
        this.mPackages = arrayList;
        this.mCurrentPassword = str;
        if (str2 == null || BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(str2)) {
            this.mEncryptPassword = str;
        } else {
            this.mEncryptPassword = str2;
        }
        this.mCompress = z7;
        this.mKeyValue = z8;
    }

    void addPackagesToSet(TreeMap<String, PackageInfo> treeMap, List<String> list) {
        for (String str : list) {
            if (!treeMap.containsKey(str)) {
                try {
                    treeMap.put(str, this.backupManagerService.getPackageManager().getPackageInfo(str, 134217728));
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.w(BackupManagerService.TAG, "Unknown package " + str + ", skipping");
                }
            }
        }
    }

    private OutputStream emitAesBackupHeader(StringBuilder sb, OutputStream outputStream) throws Exception {
        byte[] bArrRandomBytes = this.backupManagerService.randomBytes(512);
        SecretKey secretKeyBuildPasswordKey = PasswordUtils.buildPasswordKey(BackupPasswordManager.PBKDF_CURRENT, this.mEncryptPassword, bArrRandomBytes, 10000);
        byte[] bArr = new byte[32];
        this.backupManagerService.getRng().nextBytes(bArr);
        byte[] bArrRandomBytes2 = this.backupManagerService.randomBytes(512);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(bArr, "AES");
        cipher.init(1, secretKeySpec);
        CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);
        sb.append(PasswordUtils.ENCRYPTION_ALGORITHM_NAME);
        sb.append('\n');
        sb.append(PasswordUtils.byteArrayToHex(bArrRandomBytes));
        sb.append('\n');
        sb.append(PasswordUtils.byteArrayToHex(bArrRandomBytes2));
        sb.append('\n');
        sb.append(10000);
        sb.append('\n');
        Cipher cipher2 = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher2.init(1, secretKeyBuildPasswordKey);
        sb.append(PasswordUtils.byteArrayToHex(cipher2.getIV()));
        sb.append('\n');
        byte[] iv = cipher.getIV();
        byte[] encoded = secretKeySpec.getEncoded();
        byte[] bArrMakeKeyChecksum = PasswordUtils.makeKeyChecksum(BackupPasswordManager.PBKDF_CURRENT, secretKeySpec.getEncoded(), bArrRandomBytes2, 10000);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(iv.length + encoded.length + bArrMakeKeyChecksum.length + 3);
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        dataOutputStream.writeByte(iv.length);
        dataOutputStream.write(iv);
        dataOutputStream.writeByte(encoded.length);
        dataOutputStream.write(encoded);
        dataOutputStream.writeByte(bArrMakeKeyChecksum.length);
        dataOutputStream.write(bArrMakeKeyChecksum);
        dataOutputStream.flush();
        sb.append(PasswordUtils.byteArrayToHex(cipher2.doFinal(byteArrayOutputStream.toByteArray())));
        sb.append('\n');
        return cipherOutputStream;
    }

    private void finalizeBackup(OutputStream outputStream) {
        try {
            outputStream.write(new byte[1024]);
        } catch (IOException e) {
            Slog.w(BackupManagerService.TAG, "Error attempting to finalize backup stream");
        }
    }

    @Override
    public void run() throws Throwable {
        OutputStream outputStream;
        Throwable th;
        boolean z;
        OutputStream outputStreamEmitAesBackupHeader;
        PackageInfo packageInfo;
        String str;
        List<String> widgetParticipants;
        Slog.i(BackupManagerService.TAG, "--- Performing adb backup" + (this.mKeyValue ? ", including key-value backups" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS) + " ---");
        TreeMap<String, PackageInfo> treeMap = new TreeMap<>();
        FullBackupObbConnection fullBackupObbConnection = new FullBackupObbConnection(this.backupManagerService);
        fullBackupObbConnection.establish();
        sendStartBackup();
        PackageManager packageManager = this.backupManagerService.getPackageManager();
        if (this.mAllApps) {
            List<PackageInfo> installedPackages = packageManager.getInstalledPackages(134217728);
            for (int i = 0; i < installedPackages.size(); i++) {
                PackageInfo packageInfo2 = installedPackages.get(i);
                if (this.mIncludeSystem || (packageInfo2.applicationInfo.flags & 1) == 0) {
                    treeMap.put(packageInfo2.packageName, packageInfo2);
                }
            }
        }
        if (this.mDoWidgets && (widgetParticipants = AppWidgetBackupBridge.getWidgetParticipants(0)) != null) {
            addPackagesToSet(treeMap, widgetParticipants);
        }
        if (this.mPackages != null) {
            addPackagesToSet(treeMap, this.mPackages);
        }
        ArrayList<PackageInfo> arrayList = new ArrayList();
        Iterator<Map.Entry<String, PackageInfo>> it = treeMap.entrySet().iterator();
        while (it.hasNext()) {
            PackageInfo value = it.next().getValue();
            if (!AppBackupUtils.appIsEligibleForBackup(value.applicationInfo, packageManager) || AppBackupUtils.appIsStopped(value.applicationInfo)) {
                it.remove();
                Slog.i(BackupManagerService.TAG, "Package " + value.packageName + " is not eligible for backup, removing.");
            } else if (AppBackupUtils.appIsKeyValueOnly(value)) {
                it.remove();
                Slog.i(BackupManagerService.TAG, "Package " + value.packageName + " is key-value.");
                arrayList.add(value);
            }
        }
        ArrayList arrayList2 = new ArrayList(treeMap.values());
        FileOutputStream fileOutputStream = new FileOutputStream(this.mOutputFile.getFileDescriptor());
        OutputStream outputStream2 = null;
        try {
            try {
                try {
                    z = this.mEncryptPassword != null && this.mEncryptPassword.length() > 0;
                } catch (RemoteException e) {
                }
            } catch (Exception e2) {
                e = e2;
            }
            if (this.backupManagerService.deviceIsEncrypted() && !z) {
                Slog.e(BackupManagerService.TAG, "Unencrypted backup of encrypted device; aborting");
                try {
                    this.mOutputFile.close();
                } catch (IOException e3) {
                    Slog.e(BackupManagerService.TAG, "IO error closing adb backup file: " + e3.getMessage());
                }
                synchronized (this.mLatch) {
                    this.mLatch.set(true);
                    this.mLatch.notifyAll();
                }
                sendEndBackup();
                fullBackupObbConnection.tearDown();
                Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                this.backupManagerService.getWakelock().release();
                return;
            }
            if (!this.backupManagerService.backupPasswordMatches(this.mCurrentPassword)) {
                Slog.w(BackupManagerService.TAG, "Backup password mismatch; aborting");
                try {
                    this.mOutputFile.close();
                } catch (IOException e4) {
                    Slog.e(BackupManagerService.TAG, "IO error closing adb backup file: " + e4.getMessage());
                }
                synchronized (this.mLatch) {
                    this.mLatch.set(true);
                    this.mLatch.notifyAll();
                }
                sendEndBackup();
                fullBackupObbConnection.tearDown();
                Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                this.backupManagerService.getWakelock().release();
                return;
            }
            StringBuilder sb = new StringBuilder(1024);
            sb.append(BackupManagerService.BACKUP_FILE_HEADER_MAGIC);
            sb.append(5);
            sb.append(this.mCompress ? "\n1\n" : "\n0\n");
            try {
                if (z) {
                    outputStreamEmitAesBackupHeader = emitAesBackupHeader(sb, fileOutputStream);
                } else {
                    sb.append("none\n");
                    outputStreamEmitAesBackupHeader = fileOutputStream;
                }
                fileOutputStream.write(sb.toString().getBytes("UTF-8"));
                OutputStream deflaterOutputStream = this.mCompress ? new DeflaterOutputStream(outputStreamEmitAesBackupHeader, new Deflater(9), true) : outputStreamEmitAesBackupHeader;
                try {
                    if (this.mIncludeShared) {
                        try {
                            try {
                                arrayList2.add(this.backupManagerService.getPackageManager().getPackageInfo(BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE, 0));
                            } catch (PackageManager.NameNotFoundException e5) {
                                Slog.e(BackupManagerService.TAG, "Unable to find shared-storage backup handler");
                            }
                        } catch (RemoteException e6) {
                            outputStream2 = deflaterOutputStream;
                            Slog.e(BackupManagerService.TAG, "App died during full backup");
                            if (outputStream2 != null) {
                            }
                            this.mOutputFile.close();
                            synchronized (this.mLatch) {
                            }
                        } catch (Exception e7) {
                            e = e7;
                            outputStream2 = deflaterOutputStream;
                            Slog.e(BackupManagerService.TAG, "Internal exception during full backup", e);
                            if (outputStream2 != null) {
                            }
                            this.mOutputFile.close();
                            synchronized (this.mLatch) {
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            outputStream = deflaterOutputStream;
                            if (outputStream != null) {
                            }
                            this.mOutputFile.close();
                            synchronized (this.mLatch) {
                            }
                        }
                    }
                    int size = arrayList2.size();
                    int i2 = 0;
                    while (i2 < size) {
                        PackageInfo packageInfo3 = (PackageInfo) arrayList2.get(i2);
                        Slog.i(BackupManagerService.TAG, "--- Performing full backup for package " + packageInfo3.packageName + " ---");
                        boolean zEquals = packageInfo3.packageName.equals(BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE);
                        int i3 = i2;
                        OutputStream outputStream3 = deflaterOutputStream;
                        ArrayList arrayList3 = arrayList2;
                        try {
                            this.mBackupEngine = new FullBackupEngine(this.backupManagerService, deflaterOutputStream, null, packageInfo3, this.mIncludeApks, this, JobStatus.NO_LATEST_RUNTIME, this.mCurrentOpToken, 0);
                            if (zEquals) {
                                str = "Shared storage";
                                packageInfo = packageInfo3;
                            } else {
                                packageInfo = packageInfo3;
                                str = packageInfo.packageName;
                            }
                            sendOnBackupPackage(str);
                            this.mCurrentTarget = packageInfo;
                            this.mBackupEngine.backupOnePackage();
                            if (!this.mIncludeObbs || zEquals) {
                                outputStream = outputStream3;
                            } else {
                                outputStream = outputStream3;
                                try {
                                    if (!fullBackupObbConnection.backupObbs(packageInfo, outputStream)) {
                                        throw new RuntimeException("Failure writing OBB stack for " + packageInfo);
                                    }
                                } catch (RemoteException e8) {
                                    outputStream2 = outputStream;
                                    Slog.e(BackupManagerService.TAG, "App died during full backup");
                                    if (outputStream2 != null) {
                                        try {
                                            outputStream2.flush();
                                            outputStream2.close();
                                        } catch (IOException e9) {
                                            Slog.e(BackupManagerService.TAG, "IO error closing adb backup file: " + e9.getMessage());
                                            synchronized (this.mLatch) {
                                                this.mLatch.set(true);
                                                this.mLatch.notifyAll();
                                            }
                                            sendEndBackup();
                                            fullBackupObbConnection.tearDown();
                                            Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                            this.backupManagerService.getWakelock().release();
                                        }
                                    }
                                    this.mOutputFile.close();
                                    synchronized (this.mLatch) {
                                    }
                                } catch (Exception e10) {
                                    e = e10;
                                    outputStream2 = outputStream;
                                    Slog.e(BackupManagerService.TAG, "Internal exception during full backup", e);
                                    if (outputStream2 != null) {
                                        try {
                                            outputStream2.flush();
                                            outputStream2.close();
                                        } catch (IOException e11) {
                                            Slog.e(BackupManagerService.TAG, "IO error closing adb backup file: " + e11.getMessage());
                                            synchronized (this.mLatch) {
                                                this.mLatch.set(true);
                                                this.mLatch.notifyAll();
                                            }
                                            sendEndBackup();
                                            fullBackupObbConnection.tearDown();
                                            Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                            this.backupManagerService.getWakelock().release();
                                        }
                                    }
                                    this.mOutputFile.close();
                                    synchronized (this.mLatch) {
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    th = th;
                                    if (outputStream != null) {
                                        try {
                                            outputStream.flush();
                                            outputStream.close();
                                        } catch (IOException e12) {
                                            Slog.e(BackupManagerService.TAG, "IO error closing adb backup file: " + e12.getMessage());
                                            synchronized (this.mLatch) {
                                                this.mLatch.set(true);
                                                this.mLatch.notifyAll();
                                            }
                                            sendEndBackup();
                                            fullBackupObbConnection.tearDown();
                                            Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                            this.backupManagerService.getWakelock().release();
                                            throw th;
                                        }
                                    }
                                    this.mOutputFile.close();
                                    synchronized (this.mLatch) {
                                    }
                                }
                            }
                            i2 = i3 + 1;
                            deflaterOutputStream = outputStream;
                            arrayList2 = arrayList3;
                        } catch (RemoteException e13) {
                            outputStream = outputStream3;
                        } catch (Exception e14) {
                            e = e14;
                            outputStream = outputStream3;
                        } catch (Throwable th4) {
                            th = th4;
                            outputStream = outputStream3;
                        }
                    }
                    OutputStream outputStream4 = deflaterOutputStream;
                    if (this.mKeyValue) {
                        for (PackageInfo packageInfo4 : arrayList) {
                            Slog.i(BackupManagerService.TAG, "--- Performing key-value backup for package " + packageInfo4.packageName + " ---");
                            KeyValueAdbBackupEngine keyValueAdbBackupEngine = new KeyValueAdbBackupEngine(outputStream4, packageInfo4, this.backupManagerService, this.backupManagerService.getPackageManager(), this.backupManagerService.getBaseStateDir(), this.backupManagerService.getDataDir());
                            sendOnBackupPackage(packageInfo4.packageName);
                            keyValueAdbBackupEngine.backupOnePackage();
                        }
                    }
                    finalizeBackup(outputStream4);
                    if (outputStream4 != null) {
                        try {
                            outputStream4.flush();
                            outputStream4.close();
                        } catch (IOException e15) {
                            Slog.e(BackupManagerService.TAG, "IO error closing adb backup file: " + e15.getMessage());
                        }
                    }
                    this.mOutputFile.close();
                    synchronized (this.mLatch) {
                        this.mLatch.set(true);
                        this.mLatch.notifyAll();
                    }
                } catch (RemoteException e16) {
                    outputStream = deflaterOutputStream;
                } catch (Exception e17) {
                    e = e17;
                    outputStream = deflaterOutputStream;
                } catch (Throwable th5) {
                    th = th5;
                    outputStream = deflaterOutputStream;
                }
                sendEndBackup();
                fullBackupObbConnection.tearDown();
                Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                this.backupManagerService.getWakelock().release();
            } catch (Exception e18) {
                Slog.e(BackupManagerService.TAG, "Unable to emit archive header", e18);
                try {
                    this.mOutputFile.close();
                } catch (IOException e19) {
                    Slog.e(BackupManagerService.TAG, "IO error closing adb backup file: " + e19.getMessage());
                }
                synchronized (this.mLatch) {
                    this.mLatch.set(true);
                    this.mLatch.notifyAll();
                    sendEndBackup();
                    fullBackupObbConnection.tearDown();
                    Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                    this.backupManagerService.getWakelock().release();
                }
            }
        } catch (Throwable th6) {
            th = th6;
            outputStream = null;
        }
    }

    @Override
    public void execute() {
    }

    @Override
    public void operationComplete(long j) {
    }

    @Override
    public void handleCancel(boolean z) {
        PackageInfo packageInfo = this.mCurrentTarget;
        Slog.w(BackupManagerService.TAG, "adb backup cancel of " + packageInfo);
        if (packageInfo != null) {
            this.backupManagerService.tearDownAgentAndKill(this.mCurrentTarget.applicationInfo);
        }
        this.backupManagerService.removeOperation(this.mCurrentOpToken);
    }
}
