package com.android.server.backup;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class DataChangedJournal {
    private static final int BUFFER_SIZE_BYTES = 8192;
    private static final String FILE_NAME_PREFIX = "journal";
    private final File mFile;

    @FunctionalInterface
    public interface Consumer {
        void accept(String str);
    }

    DataChangedJournal(File file) {
        this.mFile = file;
    }

    public void addPackage(String str) throws Exception {
        RandomAccessFile randomAccessFile = new RandomAccessFile(this.mFile, "rws");
        Throwable th = null;
        try {
            try {
                randomAccessFile.seek(randomAccessFile.length());
                randomAccessFile.writeUTF(str);
            } finally {
            }
        } finally {
            $closeResource(th, randomAccessFile);
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

    public void forEach(Consumer consumer) throws Exception {
        Throwable th;
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(this.mFile), 8192);
        try {
            DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);
            while (dataInputStream.available() > 0) {
                try {
                    consumer.accept(dataInputStream.readUTF());
                } catch (Throwable th2) {
                    th = th2;
                    th = null;
                    $closeResource(th, dataInputStream);
                    throw th;
                }
            }
            $closeResource(null, dataInputStream);
        } finally {
            $closeResource(null, bufferedInputStream);
        }
    }

    public boolean delete() {
        return this.mFile.delete();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof DataChangedJournal)) {
            return false;
        }
        try {
            return this.mFile.getCanonicalPath().equals(((DataChangedJournal) obj).mFile.getCanonicalPath());
        } catch (IOException e) {
            return false;
        }
    }

    public String toString() {
        return this.mFile.toString();
    }

    static DataChangedJournal newJournal(File file) throws IOException {
        return new DataChangedJournal(File.createTempFile(FILE_NAME_PREFIX, null, file));
    }

    static ArrayList<DataChangedJournal> listJournals(File file) {
        ArrayList<DataChangedJournal> arrayList = new ArrayList<>();
        for (File file2 : file.listFiles()) {
            arrayList.add(new DataChangedJournal(file2));
        }
        return arrayList;
    }
}
