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

    static RandomBlock fromFile(String filename) throws Throwable {
        InputStream stream = null;
        try {
            InputStream stream2 = new FileInputStream(filename);
            try {
                RandomBlock randomBlockFromStream = fromStream(stream2);
                close(stream2);
                return randomBlockFromStream;
            } catch (Throwable th) {
                th = th;
                stream = stream2;
                close(stream);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private static RandomBlock fromStream(InputStream in) throws IOException {
        RandomBlock retval = new RandomBlock();
        int total = 0;
        while (total < 512) {
            int result = in.read(retval.block, total, 512 - total);
            if (result == -1) {
                throw new EOFException();
            }
            total += result;
        }
        return retval;
    }

    void toFile(String filename, boolean sync) throws Throwable {
        RandomAccessFile out;
        RandomAccessFile out2 = null;
        try {
            out = new RandomAccessFile(filename, sync ? "rws" : "rw");
        } catch (Throwable th) {
            th = th;
        }
        try {
            toDataOut(out);
            truncateIfPossible(out);
            close(out);
        } catch (Throwable th2) {
            th = th2;
            out2 = out;
            close(out2);
            throw th;
        }
    }

    private static void truncateIfPossible(RandomAccessFile f) {
        try {
            f.setLength(512L);
        } catch (IOException e) {
        }
    }

    private void toDataOut(DataOutput out) throws IOException {
        out.write(this.block);
    }

    private static void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                Slog.w(TAG, "IOException thrown while closing Closeable", e);
            }
        }
    }
}
