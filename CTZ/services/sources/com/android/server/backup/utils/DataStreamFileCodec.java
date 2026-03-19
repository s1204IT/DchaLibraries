package com.android.server.backup.utils;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public final class DataStreamFileCodec<T> {
    private final DataStreamCodec<T> mCodec;
    private final File mFile;

    public DataStreamFileCodec(File file, DataStreamCodec<T> dataStreamCodec) {
        this.mFile = file;
        this.mCodec = dataStreamCodec;
    }

    public T deserialize() throws Exception {
        Throwable th;
        FileInputStream fileInputStream = new FileInputStream(this.mFile);
        try {
            DataInputStream dataInputStream = new DataInputStream(fileInputStream);
            try {
                T tDeserialize = this.mCodec.deserialize(dataInputStream);
                $closeResource(null, dataInputStream);
                return tDeserialize;
            } catch (Throwable th2) {
                th = th2;
                th = null;
                $closeResource(th, dataInputStream);
                throw th;
            }
        } finally {
            $closeResource(null, fileInputStream);
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    public void serialize(T t) throws Exception {
        Throwable th;
        Throwable th2;
        Throwable th3;
        FileOutputStream fileOutputStream = new FileOutputStream(this.mFile);
        try {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            try {
                DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);
                try {
                    this.mCodec.serialize(t, dataOutputStream);
                    dataOutputStream.flush();
                    $closeResource(null, dataOutputStream);
                    $closeResource(null, bufferedOutputStream);
                } catch (Throwable th4) {
                    th = th4;
                    th3 = null;
                    $closeResource(th3, dataOutputStream);
                    throw th;
                }
            } catch (Throwable th5) {
                try {
                    throw th5;
                } catch (Throwable th6) {
                    th = th5;
                    th2 = th6;
                    $closeResource(th, bufferedOutputStream);
                    throw th2;
                }
            }
        } finally {
            $closeResource(null, fileOutputStream);
        }
    }
}
