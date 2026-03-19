package com.android.mtp;

import android.content.Context;
import android.mtp.MtpObjectInfo;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import com.android.internal.util.Preconditions;
import java.io.File;
import java.io.IOException;

class MtpFileWriter implements AutoCloseable {
    final ParcelFileDescriptor mCacheFd;
    boolean mDirty = false;
    final String mDocumentId;

    MtpFileWriter(Context context, String str) throws IOException {
        this.mDocumentId = str;
        File fileCreateTempFile = File.createTempFile("mtp", "tmp", context.getCacheDir());
        this.mCacheFd = ParcelFileDescriptor.open(fileCreateTempFile, 1006632960);
        fileCreateTempFile.delete();
    }

    String getDocumentId() {
        return this.mDocumentId;
    }

    int write(long j, int i, byte[] bArr) throws IOException, ErrnoException {
        Preconditions.checkArgumentNonnegative(j, "offset");
        Preconditions.checkArgumentNonnegative(i, "size");
        Preconditions.checkArgument(i <= bArr.length);
        if (i == 0) {
            return 0;
        }
        this.mDirty = true;
        Os.lseek(this.mCacheFd.getFileDescriptor(), j, OsConstants.SEEK_SET);
        return Os.write(this.mCacheFd.getFileDescriptor(), bArr, 0, i);
    }

    void flush(MtpManager mtpManager, MtpDatabase mtpDatabase, int[] iArr) throws IOException, ErrnoException {
        if (!this.mDirty) {
            return;
        }
        Identifier identifierCreateIdentifier = mtpDatabase.createIdentifier(this.mDocumentId);
        MtpObjectInfo objectInfo = mtpManager.getObjectInfo(identifierCreateIdentifier.mDeviceId, identifierCreateIdentifier.mObjectHandle);
        mtpManager.deleteDocument(identifierCreateIdentifier.mDeviceId, identifierCreateIdentifier.mObjectHandle);
        long jLseek = Os.lseek(this.mCacheFd.getFileDescriptor(), 0L, OsConstants.SEEK_END);
        MtpObjectInfo mtpObjectInfoBuild = new MtpObjectInfo.Builder(objectInfo).setCompressedSize(jLseek).build();
        Os.lseek(this.mCacheFd.getFileDescriptor(), 0L, OsConstants.SEEK_SET);
        mtpDatabase.updateObject(identifierCreateIdentifier.mDocumentId, identifierCreateIdentifier.mDeviceId, mtpDatabase.getParentIdentifier(identifierCreateIdentifier.mDocumentId).mDocumentId, iArr, mtpManager.getObjectInfo(identifierCreateIdentifier.mDeviceId, mtpManager.createDocument(identifierCreateIdentifier.mDeviceId, mtpObjectInfoBuild, this.mCacheFd)), Long.valueOf(jLseek));
        this.mDirty = false;
    }

    @Override
    public void close() throws IOException {
        this.mCacheFd.close();
    }
}
