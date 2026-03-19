package com.android.server;

import android.util.Slog;
import java.io.Closeable;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

class RandomBlock {
    private static final int BLOCK_SIZE = 512;
    private static final boolean DEBUG = false;
    private static final String TAG = "RandomBlock";
    private byte[] block = new byte[512];

    private RandomBlock() {
    }

    static RandomBlock fromFile(String str) throws Throwable {
        FileInputStream fileInputStream;
        FileInputStream fileInputStream2 = null;
        try {
            fileInputStream = new FileInputStream(str);
        } catch (Throwable th) {
            th = th;
        }
        try {
            RandomBlock randomBlockFromStream = fromStream(fileInputStream);
            close(fileInputStream);
            return randomBlockFromStream;
        } catch (Throwable th2) {
            th = th2;
            fileInputStream2 = fileInputStream;
            close(fileInputStream2);
            throw th;
        }
    }

    private static RandomBlock fromStream(InputStream inputStream) throws IOException {
        RandomBlock randomBlock = new RandomBlock();
        int i = 0;
        while (i < 512) {
            int i2 = inputStream.read(randomBlock.block, i, 512 - i);
            if (i2 == -1) {
                throw new EOFException();
            }
            i += i2;
        }
        return randomBlock;
    }

    void toFile(String str, boolean z) throws Throwable {
        RandomAccessFile randomAccessFile = null;
        try {
            RandomAccessFile randomAccessFile2 = new RandomAccessFile(str, z ? "rws" : "rw");
            try {
                toDataOut(randomAccessFile2);
                truncateIfPossible(randomAccessFile2);
                close(randomAccessFile2);
            } catch (Throwable th) {
                th = th;
                randomAccessFile = randomAccessFile2;
                close(randomAccessFile);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private static void truncateIfPossible(RandomAccessFile randomAccessFile) {
        try {
            randomAccessFile.setLength(512L);
        } catch (IOException e) {
        }
    }

    private void toDataOut(DataOutput dataOutput) throws IOException {
        dataOutput.write(this.block);
    }

    private static void close(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            Slog.w(TAG, "IOException thrown while closing Closeable", e);
        }
    }
}
