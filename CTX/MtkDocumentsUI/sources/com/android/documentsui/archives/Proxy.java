package com.android.documentsui.archives;

import android.os.ProxyFileDescriptorCallback;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.jar.StrictJarFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import libcore.io.IoUtils;

public class Proxy extends ProxyFileDescriptorCallback {
    private final ZipEntry mEntry;
    private final StrictJarFile mFile;
    private InputStream mInputStream = null;
    private long mOffset = 0;

    Proxy(StrictJarFile strictJarFile, ZipEntry zipEntry) throws IOException {
        this.mFile = strictJarFile;
        this.mEntry = zipEntry;
        recreateInputStream();
    }

    @Override
    public long onGetSize() throws ErrnoException {
        return this.mEntry.getSize();
    }

    @Override
    public int onRead(long j, int i, byte[] bArr) throws ErrnoException {
        if (j < this.mOffset) {
            try {
                recreateInputStream();
            } catch (IOException e) {
                throw new ErrnoException("onRead", OsConstants.EIO);
            }
        }
        while (this.mOffset < j) {
            try {
                this.mOffset += this.mInputStream.skip(j - this.mOffset);
            } catch (IOException e2) {
                throw new ErrnoException("onRead", OsConstants.EIO);
            }
        }
        int i2 = i;
        while (i2 > 0) {
            try {
                int i3 = i - i2;
                int i4 = this.mInputStream.read(bArr, i3, i2);
                if (i4 <= 0) {
                    return i3;
                }
                i2 -= i4;
                this.mOffset += (long) i4;
            } catch (IOException e3) {
                throw new ErrnoException("onRead", OsConstants.EIO);
            }
        }
        return i - i2;
    }

    @Override
    public void onRelease() {
        IoUtils.closeQuietly(this.mInputStream);
    }

    private void recreateInputStream() throws IOException {
        IoUtils.closeQuietly(this.mInputStream);
        this.mInputStream = this.mFile.getInputStream(this.mEntry);
        this.mOffset = 0L;
    }
}
