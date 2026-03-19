package android.app.backup;

import android.os.ParcelFileDescriptor;
import android.util.ArrayMap;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public abstract class BlobBackupHelper implements BackupHelper {
    private static final boolean DEBUG = false;
    private static final String TAG = "BlobBackupHelper";
    private final int mCurrentBlobVersion;
    private final String[] mKeys;

    protected abstract void applyRestoredPayload(String str, byte[] bArr);

    protected abstract byte[] getBackupPayload(String str);

    public BlobBackupHelper(int i, String... strArr) {
        this.mCurrentBlobVersion = i;
        this.mKeys = strArr;
    }

    private ArrayMap<String, Long> readOldState(ParcelFileDescriptor parcelFileDescriptor) {
        ArrayMap<String, Long> arrayMap = new ArrayMap<>();
        DataInputStream dataInputStream = new DataInputStream(new FileInputStream(parcelFileDescriptor.getFileDescriptor()));
        try {
            int i = dataInputStream.readInt();
            if (i <= this.mCurrentBlobVersion) {
                int i2 = dataInputStream.readInt();
                for (int i3 = 0; i3 < i2; i3++) {
                    arrayMap.put(dataInputStream.readUTF(), Long.valueOf(dataInputStream.readLong()));
                }
            } else {
                Log.w(TAG, "Prior state from unrecognized version " + i);
            }
        } catch (EOFException e) {
            arrayMap.clear();
        } catch (Exception e2) {
            Log.e(TAG, "Error examining prior backup state " + e2.getMessage());
            arrayMap.clear();
        }
        return arrayMap;
    }

    private void writeBackupState(ArrayMap<String, Long> arrayMap, ParcelFileDescriptor parcelFileDescriptor) {
        try {
            DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(parcelFileDescriptor.getFileDescriptor()));
            dataOutputStream.writeInt(this.mCurrentBlobVersion);
            int size = arrayMap != null ? arrayMap.size() : 0;
            dataOutputStream.writeInt(size);
            for (int i = 0; i < size; i++) {
                String strKeyAt = arrayMap.keyAt(i);
                long jLongValue = arrayMap.valueAt(i).longValue();
                dataOutputStream.writeUTF(strKeyAt);
                dataOutputStream.writeLong(jLongValue);
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to write updated state", e);
        }
    }

    private byte[] deflate(byte[] bArr) {
        if (bArr != null) {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                new DataOutputStream(byteArrayOutputStream).writeInt(this.mCurrentBlobVersion);
                DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream);
                deflaterOutputStream.write(bArr);
                deflaterOutputStream.close();
                return byteArrayOutputStream.toByteArray();
            } catch (IOException e) {
                Log.w(TAG, "Unable to process payload: " + e.getMessage());
            }
        }
        return null;
    }

    private byte[] inflate(byte[] bArr) {
        if (bArr != null) {
            try {
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
                int i = new DataInputStream(byteArrayInputStream).readInt();
                if (i > this.mCurrentBlobVersion) {
                    Log.w(TAG, "Saved payload from unrecognized version " + i);
                    return null;
                }
                InflaterInputStream inflaterInputStream = new InflaterInputStream(byteArrayInputStream);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] bArr2 = new byte[4096];
                while (true) {
                    int i2 = inflaterInputStream.read(bArr2);
                    if (i2 > 0) {
                        byteArrayOutputStream.write(bArr2, 0, i2);
                    } else {
                        inflaterInputStream.close();
                        byteArrayOutputStream.flush();
                        return byteArrayOutputStream.toByteArray();
                    }
                }
            } catch (IOException e) {
                Log.w(TAG, "Unable to process restored payload: " + e.getMessage());
            }
        }
        return null;
    }

    private long checksum(byte[] bArr) {
        if (bArr != null) {
            try {
                CRC32 crc32 = new CRC32();
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
                byte[] bArr2 = new byte[4096];
                while (true) {
                    int i = byteArrayInputStream.read(bArr2);
                    if (i >= 0) {
                        crc32.update(bArr2, 0, i);
                    } else {
                        return crc32.getValue();
                    }
                }
            } catch (Exception e) {
                return -1L;
            }
        } else {
            return -1L;
        }
    }

    @Override
    public void performBackup(ParcelFileDescriptor parcelFileDescriptor, BackupDataOutput backupDataOutput, ParcelFileDescriptor parcelFileDescriptor2) {
        ArrayMap<String, Long> oldState = readOldState(parcelFileDescriptor);
        ArrayMap<String, Long> arrayMap = new ArrayMap<>();
        try {
            try {
                for (String str : this.mKeys) {
                    byte[] bArrDeflate = deflate(getBackupPayload(str));
                    long jChecksum = checksum(bArrDeflate);
                    arrayMap.put(str, Long.valueOf(jChecksum));
                    Long l = oldState.get(str);
                    if (l == null || jChecksum != l.longValue()) {
                        if (bArrDeflate != null) {
                            backupDataOutput.writeEntityHeader(str, bArrDeflate.length);
                            backupDataOutput.writeEntityData(bArrDeflate, bArrDeflate.length);
                        } else {
                            backupDataOutput.writeEntityHeader(str, -1);
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Unable to record notification state: " + e.getMessage());
                arrayMap.clear();
            }
        } finally {
            writeBackupState(arrayMap, parcelFileDescriptor2);
        }
    }

    @Override
    public void restoreEntity(BackupDataInputStream backupDataInputStream) {
        String key = backupDataInputStream.getKey();
        int i = 0;
        while (i < this.mKeys.length && !key.equals(this.mKeys[i])) {
            try {
                i++;
            } catch (Exception e) {
                Log.e(TAG, "Exception restoring entity " + key + " : " + e.getMessage());
                return;
            }
        }
        if (i >= this.mKeys.length) {
            Log.e(TAG, "Unrecognized key " + key + ", ignoring");
            return;
        }
        byte[] bArr = new byte[backupDataInputStream.size()];
        backupDataInputStream.read(bArr);
        applyRestoredPayload(key, inflate(bArr));
    }

    @Override
    public void writeNewStateDescription(ParcelFileDescriptor parcelFileDescriptor) {
        writeBackupState(null, parcelFileDescriptor);
    }
}
