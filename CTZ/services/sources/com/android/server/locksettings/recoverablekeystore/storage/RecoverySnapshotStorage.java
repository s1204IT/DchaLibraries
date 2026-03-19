package com.android.server.locksettings.recoverablekeystore.storage;

import android.os.Environment;
import android.security.keystore.recovery.KeyChainSnapshot;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.locksettings.recoverablekeystore.serialization.KeyChainSnapshotDeserializer;
import com.android.server.locksettings.recoverablekeystore.serialization.KeyChainSnapshotParserException;
import com.android.server.locksettings.recoverablekeystore.serialization.KeyChainSnapshotSerializer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.util.Locale;

public class RecoverySnapshotStorage {
    private static final String ROOT_PATH = "system";
    private static final String STORAGE_PATH = "recoverablekeystore/snapshots/";
    private static final String TAG = "RecoverySnapshotStorage";

    @GuardedBy("this")
    private final SparseArray<KeyChainSnapshot> mSnapshotByUid = new SparseArray<>();
    private final File rootDirectory;

    public static RecoverySnapshotStorage newInstance() {
        return new RecoverySnapshotStorage(new File(Environment.getDataDirectory(), ROOT_PATH));
    }

    @VisibleForTesting
    public RecoverySnapshotStorage(File file) {
        this.rootDirectory = file;
    }

    public synchronized void put(int i, KeyChainSnapshot keyChainSnapshot) {
        this.mSnapshotByUid.put(i, keyChainSnapshot);
        try {
            writeToDisk(i, keyChainSnapshot);
        } catch (IOException | CertificateEncodingException e) {
            Log.e(TAG, String.format(Locale.US, "Error persisting snapshot for %d to disk", Integer.valueOf(i)), e);
        }
    }

    public synchronized KeyChainSnapshot get(int i) {
        KeyChainSnapshot keyChainSnapshot = this.mSnapshotByUid.get(i);
        if (keyChainSnapshot != null) {
            return keyChainSnapshot;
        }
        try {
            return readFromDisk(i);
        } catch (KeyChainSnapshotParserException | IOException e) {
            Log.e(TAG, String.format(Locale.US, "Error reading snapshot for %d from disk", Integer.valueOf(i)), e);
            return null;
        }
    }

    public synchronized void remove(int i) {
        this.mSnapshotByUid.remove(i);
        getSnapshotFile(i).delete();
    }

    private void writeToDisk(int i, KeyChainSnapshot keyChainSnapshot) throws IOException, CertificateEncodingException {
        File snapshotFile = getSnapshotFile(i);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(snapshotFile);
            try {
                KeyChainSnapshotSerializer.serialize(keyChainSnapshot, fileOutputStream);
            } finally {
                $closeResource(null, fileOutputStream);
            }
        } catch (IOException | CertificateEncodingException e) {
            snapshotFile.delete();
            throw e;
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

    private KeyChainSnapshot readFromDisk(int i) throws KeyChainSnapshotParserException, IOException {
        File snapshotFile = getSnapshotFile(i);
        try {
            FileInputStream fileInputStream = new FileInputStream(snapshotFile);
            try {
                return KeyChainSnapshotDeserializer.deserialize(fileInputStream);
            } finally {
                $closeResource(null, fileInputStream);
            }
        } catch (KeyChainSnapshotParserException | IOException e) {
            snapshotFile.delete();
            throw e;
        }
    }

    private File getSnapshotFile(int i) {
        return new File(getStorageFolder(), getSnapshotFileName(i));
    }

    private String getSnapshotFileName(int i) {
        return String.format(Locale.US, "%d.xml", Integer.valueOf(i));
    }

    private File getStorageFolder() {
        File file = new File(this.rootDirectory, STORAGE_PATH);
        file.mkdirs();
        return file;
    }
}
