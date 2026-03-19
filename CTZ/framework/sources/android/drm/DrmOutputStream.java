package android.drm;

import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownServiceException;
import java.util.Arrays;
import libcore.io.IoBridge;
import libcore.io.Streams;

public class DrmOutputStream extends OutputStream {
    private static final String TAG = "DrmOutputStream";
    private final DrmManagerClient mClient;
    private final FileDescriptor mFd;
    private final ParcelFileDescriptor mPfd;
    private int mSessionId;

    public DrmOutputStream(DrmManagerClient drmManagerClient, ParcelFileDescriptor parcelFileDescriptor, String str) throws IOException {
        this.mSessionId = -1;
        this.mClient = drmManagerClient;
        this.mPfd = parcelFileDescriptor;
        this.mFd = parcelFileDescriptor.getFileDescriptor();
        this.mSessionId = this.mClient.openConvertSession(str);
        if (this.mSessionId == -1) {
            throw new UnknownServiceException("Failed to open DRM session for " + str);
        }
    }

    public void finish() throws IOException {
        DrmConvertedStatus drmConvertedStatusCloseConvertSession = this.mClient.closeConvertSession(this.mSessionId);
        if (drmConvertedStatusCloseConvertSession.statusCode == 1) {
            try {
                Os.lseek(this.mFd, drmConvertedStatusCloseConvertSession.offset, OsConstants.SEEK_SET);
            } catch (ErrnoException e) {
                e.rethrowAsIOException();
            }
            IoBridge.write(this.mFd, drmConvertedStatusCloseConvertSession.convertedData, 0, drmConvertedStatusCloseConvertSession.convertedData.length);
            this.mSessionId = -1;
            return;
        }
        throw new IOException("Unexpected DRM status: " + drmConvertedStatusCloseConvertSession.statusCode);
    }

    @Override
    public void close() throws IOException {
        if (this.mSessionId == -1) {
            Log.w(TAG, "Closing stream without finishing");
        }
        this.mPfd.close();
    }

    @Override
    public void write(byte[] bArr, int i, int i2) throws IOException {
        Arrays.checkOffsetAndCount(bArr.length, i, i2);
        if (i2 != bArr.length) {
            byte[] bArr2 = new byte[i2];
            System.arraycopy(bArr, i, bArr2, 0, i2);
            bArr = bArr2;
        }
        DrmConvertedStatus drmConvertedStatusConvertData = this.mClient.convertData(this.mSessionId, bArr);
        if (drmConvertedStatusConvertData.statusCode == 1) {
            IoBridge.write(this.mFd, drmConvertedStatusConvertData.convertedData, 0, drmConvertedStatusConvertData.convertedData.length);
            return;
        }
        throw new IOException("Unexpected DRM status: " + drmConvertedStatusConvertData.statusCode);
    }

    @Override
    public void write(int i) throws IOException {
        Streams.writeSingleByte(this, i);
    }
}
