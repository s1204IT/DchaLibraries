package com.android.okhttp.internal.io;

import com.android.okhttp.okio.Okio;
import com.android.okhttp.okio.Sink;
import com.android.okhttp.okio.Source;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public interface FileSystem {
    public static final FileSystem SYSTEM = new FileSystem() {
        @Override
        public Source source(File file) throws FileNotFoundException {
            return Okio.source(file);
        }

        @Override
        public Sink sink(File file) throws FileNotFoundException {
            try {
                return Okio.sink(file);
            } catch (FileNotFoundException e) {
                file.getParentFile().mkdirs();
                return Okio.sink(file);
            }
        }

        @Override
        public Sink appendingSink(File file) throws FileNotFoundException {
            try {
                return Okio.appendingSink(file);
            } catch (FileNotFoundException e) {
                file.getParentFile().mkdirs();
                return Okio.appendingSink(file);
            }
        }

        @Override
        public void delete(File file) throws IOException {
            if (file.delete() || !file.exists()) {
            } else {
                throw new IOException("failed to delete " + file);
            }
        }

        @Override
        public boolean exists(File file) throws IOException {
            return file.exists();
        }

        @Override
        public long size(File file) {
            return file.length();
        }

        @Override
        public void rename(File from, File to) throws IOException {
            delete(to);
            if (from.renameTo(to)) {
            } else {
                throw new IOException("failed to rename " + from + " to " + to);
            }
        }

        @Override
        public void deleteContents(File directory) throws IOException {
            File[] files = directory.listFiles();
            if (files == null) {
                throw new IOException("not a readable directory: " + directory);
            }
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteContents(file);
                }
                if (!file.delete()) {
                    throw new IOException("failed to delete " + file);
                }
            }
        }
    };

    Sink appendingSink(File file) throws FileNotFoundException;

    void delete(File file) throws IOException;

    void deleteContents(File file) throws IOException;

    boolean exists(File file) throws IOException;

    void rename(File file, File file2) throws IOException;

    Sink sink(File file) throws FileNotFoundException;

    long size(File file);

    Source source(File file) throws FileNotFoundException;
}
