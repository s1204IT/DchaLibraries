package android.app.backup;

import java.io.IOException;
import java.io.InputStream;

public class BackupDataInputStream extends InputStream {
    int dataSize;
    String key;
    BackupDataInput mData;
    byte[] mOneByte;

    BackupDataInputStream(BackupDataInput backupDataInput) {
        this.mData = backupDataInput;
    }

    @Override
    public int read() throws IOException {
        byte[] bArr = this.mOneByte;
        if (this.mOneByte == null) {
            bArr = new byte[1];
            this.mOneByte = bArr;
        }
        this.mData.readEntityData(bArr, 0, 1);
        return bArr[0];
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        return this.mData.readEntityData(bArr, i, i2);
    }

    @Override
    public int read(byte[] bArr) throws IOException {
        return this.mData.readEntityData(bArr, 0, bArr.length);
    }

    public String getKey() {
        return this.key;
    }

    public int size() {
        return this.dataSize;
    }
}
