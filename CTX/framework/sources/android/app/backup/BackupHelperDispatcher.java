package android.app.backup;

import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class BackupHelperDispatcher {
    private static final String TAG = "BackupHelperDispatcher";
    TreeMap<String, BackupHelper> mHelpers = new TreeMap<>();

    private static native int allocateHeader_native(Header header, FileDescriptor fileDescriptor);

    private static native int readHeader_native(Header header, FileDescriptor fileDescriptor);

    private static native int skipChunk_native(FileDescriptor fileDescriptor, int i);

    private static native int writeHeader_native(Header header, FileDescriptor fileDescriptor, int i);

    private static class Header {
        int chunkSize;
        String keyPrefix;

        private Header() {
        }
    }

    public void addHelper(String str, BackupHelper backupHelper) {
        this.mHelpers.put(str, backupHelper);
    }

    public void performBackup(ParcelFileDescriptor parcelFileDescriptor, BackupDataOutput backupDataOutput, ParcelFileDescriptor parcelFileDescriptor2) throws IOException {
        Header header = new Header();
        TreeMap treeMap = (TreeMap) this.mHelpers.clone();
        if (parcelFileDescriptor != null) {
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            while (true) {
                int header_native = readHeader_native(header, fileDescriptor);
                if (header_native < 0) {
                    break;
                }
                if (header_native == 0) {
                    BackupHelper backupHelper = (BackupHelper) treeMap.get(header.keyPrefix);
                    Log.d(TAG, "handling existing helper '" + header.keyPrefix + "' " + backupHelper);
                    if (backupHelper != null) {
                        doOneBackup(parcelFileDescriptor, backupDataOutput, parcelFileDescriptor2, header, backupHelper);
                        treeMap.remove(header.keyPrefix);
                    } else {
                        skipChunk_native(fileDescriptor, header.chunkSize);
                    }
                }
            }
        }
        for (Map.Entry entry : treeMap.entrySet()) {
            header.keyPrefix = (String) entry.getKey();
            Log.d(TAG, "handling new helper '" + header.keyPrefix + "'");
            doOneBackup(parcelFileDescriptor, backupDataOutput, parcelFileDescriptor2, header, (BackupHelper) entry.getValue());
        }
    }

    private void doOneBackup(ParcelFileDescriptor parcelFileDescriptor, BackupDataOutput backupDataOutput, ParcelFileDescriptor parcelFileDescriptor2, Header header, BackupHelper backupHelper) throws IOException {
        FileDescriptor fileDescriptor = parcelFileDescriptor2.getFileDescriptor();
        int iAllocateHeader_native = allocateHeader_native(header, fileDescriptor);
        if (iAllocateHeader_native < 0) {
            throw new IOException("allocateHeader_native failed (error " + iAllocateHeader_native + ")");
        }
        backupDataOutput.setKeyPrefix(header.keyPrefix);
        backupHelper.performBackup(parcelFileDescriptor, backupDataOutput, parcelFileDescriptor2);
        int iWriteHeader_native = writeHeader_native(header, fileDescriptor, iAllocateHeader_native);
        if (iWriteHeader_native != 0) {
            throw new IOException("writeHeader_native failed (error " + iWriteHeader_native + ")");
        }
    }

    public void performRestore(BackupDataInput backupDataInput, int i, ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        BackupDataInputStream backupDataInputStream = new BackupDataInputStream(backupDataInput);
        boolean z = false;
        while (backupDataInput.readNextHeader()) {
            String key = backupDataInput.getKey();
            int iIndexOf = key.indexOf(58);
            if (iIndexOf > 0) {
                BackupHelper backupHelper = this.mHelpers.get(key.substring(0, iIndexOf));
                if (backupHelper != null) {
                    backupDataInputStream.dataSize = backupDataInput.getDataSize();
                    backupDataInputStream.key = key.substring(iIndexOf + 1);
                    backupHelper.restoreEntity(backupDataInputStream);
                } else if (!z) {
                    Log.w(TAG, "Couldn't find helper for: '" + key + "'");
                    z = true;
                }
            } else if (!z) {
                Log.w(TAG, "Entity with no prefix: '" + key + "'");
                z = true;
            }
            backupDataInput.skipEntityData();
        }
        Iterator<BackupHelper> it = this.mHelpers.values().iterator();
        while (it.hasNext()) {
            it.next().writeNewStateDescription(parcelFileDescriptor);
        }
    }
}
