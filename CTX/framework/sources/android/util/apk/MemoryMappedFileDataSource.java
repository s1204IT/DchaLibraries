package android.util.apk;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.DirectByteBuffer;

class MemoryMappedFileDataSource implements DataSource {
    private static final long MEMORY_PAGE_SIZE_BYTES = Os.sysconf(OsConstants._SC_PAGESIZE);
    private final FileDescriptor mFd;
    private final long mFilePosition;
    private final long mSize;

    MemoryMappedFileDataSource(FileDescriptor fileDescriptor, long j, long j2) {
        this.mFd = fileDescriptor;
        this.mFilePosition = j;
        this.mSize = j2;
    }

    @Override
    public long size() {
        return this.mSize;
    }

    @Override
    public void feedIntoDataDigester(DataDigester dataDigester, long j, int i) throws Throwable {
        long j2;
        Throwable th;
        long jMmap;
        long j3;
        long j4 = this.mFilePosition + j;
        long j5 = (j4 / MEMORY_PAGE_SIZE_BYTES) * MEMORY_PAGE_SIZE_BYTES;
        int i2 = (int) (j4 - j5);
        long j6 = i + i2;
        try {
            try {
                jMmap = Os.mmap(0L, j6, OsConstants.PROT_READ, OsConstants.MAP_SHARED | OsConstants.MAP_POPULATE, this.mFd, j5);
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (ErrnoException e) {
            e = e;
            j3 = j6;
        } catch (Throwable th3) {
            j2 = j6;
            th = th3;
            jMmap = 0;
            if (jMmap != 0) {
                throw th;
            }
            try {
                Os.munmap(jMmap, j2);
                throw th;
            } catch (ErrnoException e2) {
                throw th;
            }
        }
        try {
            j3 = j6;
            try {
                dataDigester.consume(new DirectByteBuffer(i, jMmap + ((long) i2), this.mFd, (Runnable) null, true));
                if (jMmap != 0) {
                    try {
                        Os.munmap(jMmap, j3);
                    } catch (ErrnoException e3) {
                    }
                }
            } catch (ErrnoException e4) {
                e = e4;
                throw new IOException("Failed to mmap " + j3 + " bytes", e);
            }
        } catch (ErrnoException e5) {
            e = e5;
            j3 = j6;
        } catch (Throwable th4) {
            th = th4;
            j2 = j6;
            th = th;
            if (jMmap != 0) {
            }
        }
    }
}
