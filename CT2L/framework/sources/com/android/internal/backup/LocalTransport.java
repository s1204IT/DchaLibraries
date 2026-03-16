package com.android.internal.backup;

import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupTransport;
import android.app.backup.RestoreDescription;
import android.app.backup.RestoreSet;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.SELinux;
import android.util.Log;
import com.android.org.bouncycastle.util.encoders.Base64;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import libcore.io.IoUtils;

public class LocalTransport extends BackupTransport {
    private static final long CURRENT_SET_TOKEN = 1;
    private static final boolean DEBUG = false;
    private static final String FULL_DATA_DIR = "_full";
    private static final String INCREMENTAL_DIR = "_delta";
    static final long[] POSSIBLE_SETS = {2, 3, 4, 5, 6, 7, 8, 9};
    private static final String TAG = "LocalTransport";
    private static final String TRANSPORT_DATA_MANAGEMENT_LABEL = "";
    private static final String TRANSPORT_DESTINATION_STRING = "Backing up to debug-only private cache";
    private static final String TRANSPORT_DIR_NAME = "com.android.internal.backup.LocalTransport";
    private Context mContext;
    private FileInputStream mCurFullRestoreStream;
    private byte[] mFullBackupBuffer;
    private BufferedOutputStream mFullBackupOutputStream;
    private byte[] mFullRestoreBuffer;
    private HashSet<String> mFullRestorePackages;
    private File mFullRestoreSetDir;
    private FileOutputStream mFullRestoreSocketStream;
    private String mFullTargetPackage;
    private File mRestoreSetDir;
    private File mRestoreSetFullDir;
    private File mRestoreSetIncrementalDir;
    private long mRestoreToken;
    private int mRestoreType;
    private ParcelFileDescriptor mSocket;
    private FileInputStream mSocketInputStream;
    private File mDataDir = new File(Environment.getDownloadCacheDirectory(), Context.BACKUP_SERVICE);
    private File mCurrentSetDir = new File(this.mDataDir, Long.toString(1));
    private File mCurrentSetIncrementalDir = new File(this.mCurrentSetDir, INCREMENTAL_DIR);
    private File mCurrentSetFullDir = new File(this.mCurrentSetDir, FULL_DATA_DIR);
    private PackageInfo[] mRestorePackages = null;
    private int mRestorePackage = -1;

    public LocalTransport(Context context) {
        this.mContext = context;
        this.mCurrentSetDir.mkdirs();
        this.mCurrentSetFullDir.mkdir();
        this.mCurrentSetIncrementalDir.mkdir();
        if (!SELinux.restorecon(this.mCurrentSetDir)) {
            Log.e(TAG, "SELinux restorecon failed for " + this.mCurrentSetDir);
        }
    }

    @Override
    public String name() {
        return new ComponentName(this.mContext, getClass()).flattenToShortString();
    }

    @Override
    public Intent configurationIntent() {
        return null;
    }

    @Override
    public String currentDestinationString() {
        return TRANSPORT_DESTINATION_STRING;
    }

    @Override
    public Intent dataManagementIntent() {
        return null;
    }

    @Override
    public String dataManagementLabel() {
        return "";
    }

    @Override
    public String transportDirName() {
        return TRANSPORT_DIR_NAME;
    }

    @Override
    public long requestBackupTime() {
        return 0L;
    }

    @Override
    public int initializeDevice() {
        deleteContents(this.mCurrentSetDir);
        return 0;
    }

    @Override
    public int performBackup(PackageInfo packageInfo, ParcelFileDescriptor data) {
        File packageDir = new File(this.mCurrentSetIncrementalDir, packageInfo.packageName);
        packageDir.mkdirs();
        BackupDataInput changeSet = new BackupDataInput(data.getFileDescriptor());
        int bufSize = 512;
        try {
            byte[] buf = new byte[512];
            while (changeSet.readNextHeader()) {
                String key = changeSet.getKey();
                String base64Key = new String(Base64.encode(key.getBytes()));
                File entityFile = new File(packageDir, base64Key);
                int dataSize = changeSet.getDataSize();
                if (dataSize >= 0) {
                    if (entityFile.exists()) {
                        entityFile.delete();
                    }
                    FileOutputStream entity = new FileOutputStream(entityFile);
                    if (dataSize > bufSize) {
                        bufSize = dataSize;
                        buf = new byte[bufSize];
                    }
                    changeSet.readEntityData(buf, 0, dataSize);
                    try {
                        try {
                            entity.write(buf, 0, dataSize);
                        } catch (IOException e) {
                            Log.e(TAG, "Unable to update key file " + entityFile.getAbsolutePath());
                            entity.close();
                            return -1000;
                        }
                    } finally {
                        entity.close();
                    }
                } else {
                    entityFile.delete();
                }
            }
            return 0;
        } catch (IOException e2) {
            Log.v(TAG, "Exception reading backup input:", e2);
            return -1000;
        }
    }

    private void deleteContents(File dirname) {
        File[] contents = dirname.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (f.isDirectory()) {
                    deleteContents(f);
                }
                f.delete();
            }
        }
    }

    @Override
    public int clearBackupData(PackageInfo packageInfo) {
        File packageDir = new File(this.mCurrentSetIncrementalDir, packageInfo.packageName);
        File[] fileset = packageDir.listFiles();
        if (fileset != null) {
            for (File f : fileset) {
                f.delete();
            }
            packageDir.delete();
        }
        File packageDir2 = new File(this.mCurrentSetFullDir, packageInfo.packageName);
        File[] tarballs = packageDir2.listFiles();
        if (tarballs != null) {
            for (File f2 : tarballs) {
                f2.delete();
            }
            packageDir2.delete();
            return 0;
        }
        return 0;
    }

    @Override
    public int finishBackup() {
        return tearDownFullBackup();
    }

    private int tearDownFullBackup() {
        if (this.mSocket != null) {
            try {
                this.mFullBackupOutputStream.flush();
                this.mFullBackupOutputStream.close();
                this.mSocketInputStream = null;
                this.mFullTargetPackage = null;
                this.mSocket.close();
            } catch (IOException e) {
                return -1000;
            } finally {
                this.mSocket = null;
            }
        }
        return 0;
    }

    private File tarballFile(String pkgName) {
        return new File(this.mCurrentSetFullDir, pkgName);
    }

    @Override
    public long requestFullBackupTime() {
        return 0L;
    }

    @Override
    public int performFullBackup(PackageInfo targetPackage, ParcelFileDescriptor socket) {
        if (this.mSocket != null) {
            Log.e(TAG, "Attempt to initiate full backup while one is in progress");
            return -1000;
        }
        try {
            this.mSocket = ParcelFileDescriptor.dup(socket.getFileDescriptor());
            this.mSocketInputStream = new FileInputStream(this.mSocket.getFileDescriptor());
            this.mFullTargetPackage = targetPackage.packageName;
            try {
                File tarball = tarballFile(this.mFullTargetPackage);
                FileOutputStream tarstream = new FileOutputStream(tarball);
                this.mFullBackupOutputStream = new BufferedOutputStream(tarstream);
                this.mFullBackupBuffer = new byte[4096];
                return 0;
            } catch (FileNotFoundException e) {
                return -1000;
            }
        } catch (IOException e2) {
            Log.e(TAG, "Unable to process socket for full backup");
            return -1000;
        }
    }

    @Override
    public int sendBackupData(int numBytes) {
        if (this.mFullBackupBuffer == null) {
            Log.w(TAG, "Attempted sendBackupData before performFullBackup");
            return -1000;
        }
        if (numBytes > this.mFullBackupBuffer.length) {
            this.mFullBackupBuffer = new byte[numBytes];
        }
        while (numBytes > 0) {
            try {
                int nRead = this.mSocketInputStream.read(this.mFullBackupBuffer, 0, numBytes);
                if (nRead < 0) {
                    Log.w(TAG, "Unexpected EOD; failing backup");
                    return -1000;
                }
                this.mFullBackupOutputStream.write(this.mFullBackupBuffer, 0, nRead);
                numBytes -= nRead;
            } catch (IOException e) {
                Log.e(TAG, "Error handling backup data for " + this.mFullTargetPackage);
                return -1000;
            }
        }
        return 0;
    }

    @Override
    public void cancelFullBackup() {
        File archive = tarballFile(this.mFullTargetPackage);
        tearDownFullBackup();
        if (archive.exists()) {
            archive.delete();
        }
    }

    @Override
    public RestoreSet[] getAvailableRestoreSets() {
        int num;
        long[] existing = new long[POSSIBLE_SETS.length + 1];
        long[] arr$ = POSSIBLE_SETS;
        int len$ = arr$.length;
        int i$ = 0;
        int num2 = 0;
        while (i$ < len$) {
            long token = arr$[i$];
            if (new File(this.mDataDir, Long.toString(token)).exists()) {
                num = num2 + 1;
                existing[num2] = token;
            } else {
                num = num2;
            }
            i$++;
            num2 = num;
        }
        existing[num2] = 1;
        RestoreSet[] available = new RestoreSet[num2 + 1];
        for (int i = 0; i < available.length; i++) {
            available[i] = new RestoreSet("Local disk image", "flash", existing[i]);
        }
        return available;
    }

    @Override
    public long getCurrentRestoreSet() {
        return 1L;
    }

    @Override
    public int startRestore(long token, PackageInfo[] packages) {
        this.mRestorePackages = packages;
        this.mRestorePackage = -1;
        this.mRestoreToken = token;
        this.mRestoreSetDir = new File(this.mDataDir, Long.toString(token));
        this.mRestoreSetIncrementalDir = new File(this.mRestoreSetDir, INCREMENTAL_DIR);
        this.mRestoreSetFullDir = new File(this.mRestoreSetDir, FULL_DATA_DIR);
        return 0;
    }

    @Override
    public RestoreDescription nextRestorePackage() {
        String name;
        if (this.mRestorePackages == null) {
            throw new IllegalStateException("startRestore not called");
        }
        boolean found = false;
        do {
            int i = this.mRestorePackage + 1;
            this.mRestorePackage = i;
            if (i >= this.mRestorePackages.length) {
                return RestoreDescription.NO_MORE_PACKAGES;
            }
            name = this.mRestorePackages[this.mRestorePackage].packageName;
            String[] contents = new File(this.mRestoreSetIncrementalDir, name).list();
            if (contents != null && contents.length > 0) {
                this.mRestoreType = 1;
                found = true;
            }
            if (!found) {
                File maybeFullData = new File(this.mRestoreSetFullDir, name);
                if (maybeFullData.length() > 0) {
                    this.mRestoreType = 2;
                    this.mCurFullRestoreStream = null;
                    found = true;
                }
            }
        } while (!found);
        return new RestoreDescription(name, this.mRestoreType);
    }

    @Override
    public int getRestoreData(ParcelFileDescriptor outFd) {
        if (this.mRestorePackages == null) {
            throw new IllegalStateException("startRestore not called");
        }
        if (this.mRestorePackage < 0) {
            throw new IllegalStateException("nextRestorePackage not called");
        }
        if (this.mRestoreType != 1) {
            throw new IllegalStateException("getRestoreData(fd) for non-key/value dataset");
        }
        File packageDir = new File(this.mRestoreSetIncrementalDir, this.mRestorePackages[this.mRestorePackage].packageName);
        ArrayList<DecodedFilename> blobs = contentsByKey(packageDir);
        if (blobs == null) {
            Log.e(TAG, "No keys for package: " + packageDir);
            return -1000;
        }
        BackupDataOutput out = new BackupDataOutput(outFd.getFileDescriptor());
        try {
            for (DecodedFilename keyEntry : blobs) {
                File f = keyEntry.file;
                FileInputStream in = new FileInputStream(f);
                try {
                    int size = (int) f.length();
                    byte[] buf = new byte[size];
                    in.read(buf);
                    out.writeEntityHeader(keyEntry.key, size);
                    out.writeEntityData(buf, size);
                    in.close();
                } catch (Throwable th) {
                    in.close();
                    throw th;
                }
            }
            return 0;
        } catch (IOException e) {
            Log.e(TAG, "Unable to read backup records", e);
            return -1000;
        }
    }

    static class DecodedFilename implements Comparable<DecodedFilename> {
        public File file;
        public String key;

        public DecodedFilename(File f) {
            this.file = f;
            this.key = new String(Base64.decode(f.getName()));
        }

        @Override
        public int compareTo(DecodedFilename other) {
            return this.key.compareTo(other.key);
        }
    }

    private ArrayList<DecodedFilename> contentsByKey(File dir) {
        File[] allFiles = dir.listFiles();
        if (allFiles == null || allFiles.length == 0) {
            return null;
        }
        ArrayList<DecodedFilename> contents = new ArrayList<>();
        for (File f : allFiles) {
            contents.add(new DecodedFilename(f));
        }
        Collections.sort(contents);
        return contents;
    }

    @Override
    public void finishRestore() {
        if (this.mRestoreType == 2) {
            resetFullRestoreState();
        }
        this.mRestoreType = 0;
    }

    private void resetFullRestoreState() {
        IoUtils.closeQuietly(this.mCurFullRestoreStream);
        this.mCurFullRestoreStream = null;
        this.mFullRestoreSocketStream = null;
        this.mFullRestoreBuffer = null;
    }

    @Override
    public int getNextFullRestoreDataChunk(ParcelFileDescriptor socket) {
        int nRead;
        if (this.mRestoreType != 2) {
            throw new IllegalStateException("Asked for full restore data for non-stream package");
        }
        if (this.mCurFullRestoreStream == null) {
            String name = this.mRestorePackages[this.mRestorePackage].packageName;
            File dataset = new File(this.mRestoreSetFullDir, name);
            try {
                this.mCurFullRestoreStream = new FileInputStream(dataset);
                this.mFullRestoreSocketStream = new FileOutputStream(socket.getFileDescriptor());
                this.mFullRestoreBuffer = new byte[2048];
            } catch (IOException e) {
                Log.e(TAG, "Unable to read archive for " + name);
                return BackupTransport.TRANSPORT_PACKAGE_REJECTED;
            }
        }
        try {
            nRead = this.mCurFullRestoreStream.read(this.mFullRestoreBuffer);
            if (nRead < 0) {
                nRead = -1;
            } else if (nRead == 0) {
                Log.w(TAG, "read() of archive file returned 0; treating as EOF");
                nRead = -1;
            } else {
                this.mFullRestoreSocketStream.write(this.mFullRestoreBuffer, 0, nRead);
            }
        } catch (IOException e2) {
            nRead = -1000;
        }
        return nRead;
    }

    @Override
    public int abortFullRestore() {
        if (this.mRestoreType != 2) {
            throw new IllegalStateException("abortFullRestore() but not currently restoring");
        }
        resetFullRestoreState();
        this.mRestoreType = 0;
        return 0;
    }
}
