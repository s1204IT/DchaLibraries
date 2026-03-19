package android.os;

import android.content.Context;
import android.os.storage.StorageManager;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Slog;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InterruptedIOException;
import libcore.io.IoUtils;

public class RevocableFileDescriptor {
    private static final boolean DEBUG = true;
    private static final String TAG = "RevocableFileDescriptor";
    private final ProxyFileDescriptorCallback mCallback = new ProxyFileDescriptorCallback() {
        private void checkRevoked() throws ErrnoException {
            if (RevocableFileDescriptor.this.mRevoked) {
                throw new ErrnoException(RevocableFileDescriptor.TAG, OsConstants.EPERM);
            }
        }

        @Override
        public long onGetSize() throws ErrnoException {
            checkRevoked();
            return Os.fstat(RevocableFileDescriptor.this.mInner).st_size;
        }

        @Override
        public int onRead(long j, int i, byte[] bArr) throws ErrnoException {
            checkRevoked();
            int i2 = 0;
            while (i2 < i) {
                try {
                    return i2 + Os.pread(RevocableFileDescriptor.this.mInner, bArr, i2, i - i2, j + ((long) i2));
                } catch (InterruptedIOException e) {
                    i2 += e.bytesTransferred;
                }
            }
            return i2;
        }

        @Override
        public int onWrite(long j, int i, byte[] bArr) throws ErrnoException {
            checkRevoked();
            int i2 = 0;
            while (i2 < i) {
                try {
                    return i2 + Os.pwrite(RevocableFileDescriptor.this.mInner, bArr, i2, i - i2, j + ((long) i2));
                } catch (InterruptedIOException e) {
                    i2 += e.bytesTransferred;
                }
            }
            return i2;
        }

        @Override
        public void onFsync() throws ErrnoException {
            Slog.v(RevocableFileDescriptor.TAG, "onFsync()");
            checkRevoked();
            Os.fsync(RevocableFileDescriptor.this.mInner);
        }

        @Override
        public void onRelease() {
            Slog.v(RevocableFileDescriptor.TAG, "onRelease()");
            RevocableFileDescriptor.this.mRevoked = true;
            IoUtils.closeQuietly(RevocableFileDescriptor.this.mInner);
        }
    };
    private FileDescriptor mInner;
    private ParcelFileDescriptor mOuter;
    private volatile boolean mRevoked;

    public RevocableFileDescriptor() {
    }

    public RevocableFileDescriptor(Context context, File file) throws IOException {
        try {
            init(context, Os.open(file.getAbsolutePath(), OsConstants.O_CREAT | OsConstants.O_RDWR, 448));
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    public RevocableFileDescriptor(Context context, FileDescriptor fileDescriptor) throws IOException {
        init(context, fileDescriptor);
    }

    public void init(Context context, FileDescriptor fileDescriptor) throws IOException {
        this.mInner = fileDescriptor;
        this.mOuter = ((StorageManager) context.getSystemService(StorageManager.class)).openProxyFileDescriptor(805306368, this.mCallback);
    }

    public ParcelFileDescriptor getRevocableFileDescriptor() {
        return this.mOuter;
    }

    public void revoke() {
        this.mRevoked = true;
        IoUtils.closeQuietly(this.mInner);
    }

    public boolean isRevoked() {
        return this.mRevoked;
    }
}
