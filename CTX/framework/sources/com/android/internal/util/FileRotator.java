package com.android.internal.util;

import android.os.FileUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import libcore.io.IoUtils;

public class FileRotator {
    private static final boolean LOGD = false;
    private static final String SUFFIX_BACKUP = ".backup";
    private static final String SUFFIX_NO_BACKUP = ".no_backup";
    private static final String TAG = "FileRotator";
    private final File mBasePath;
    private final long mDeleteAgeMillis;
    private final String mPrefix;
    private final long mRotateAgeMillis;

    public interface Reader {
        void read(InputStream inputStream) throws IOException;
    }

    public interface Rewriter extends Reader, Writer {
        void reset();

        boolean shouldWrite();
    }

    public interface Writer {
        void write(OutputStream outputStream) throws IOException;
    }

    public FileRotator(File file, String str, long j, long j2) {
        this.mBasePath = (File) Preconditions.checkNotNull(file);
        this.mPrefix = (String) Preconditions.checkNotNull(str);
        this.mRotateAgeMillis = j;
        this.mDeleteAgeMillis = j2;
        this.mBasePath.mkdirs();
        for (String str2 : this.mBasePath.list()) {
            if (str2.startsWith(this.mPrefix)) {
                if (str2.endsWith(SUFFIX_BACKUP)) {
                    new File(this.mBasePath, str2).renameTo(new File(this.mBasePath, str2.substring(0, str2.length() - SUFFIX_BACKUP.length())));
                } else if (str2.endsWith(SUFFIX_NO_BACKUP)) {
                    File file2 = new File(this.mBasePath, str2);
                    File file3 = new File(this.mBasePath, str2.substring(0, str2.length() - SUFFIX_NO_BACKUP.length()));
                    file2.delete();
                    file3.delete();
                }
            }
        }
    }

    public void deleteAll() {
        FileInfo fileInfo = new FileInfo(this.mPrefix);
        for (String str : this.mBasePath.list()) {
            if (fileInfo.parse(str)) {
                new File(this.mBasePath, str).delete();
            }
        }
    }

    public void dumpAll(OutputStream outputStream) throws IOException {
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
        try {
            FileInfo fileInfo = new FileInfo(this.mPrefix);
            for (String str : this.mBasePath.list()) {
                if (fileInfo.parse(str)) {
                    zipOutputStream.putNextEntry(new ZipEntry(str));
                    FileInputStream fileInputStream = new FileInputStream(new File(this.mBasePath, str));
                    try {
                        FileUtils.copy(fileInputStream, zipOutputStream);
                        IoUtils.closeQuietly(fileInputStream);
                        zipOutputStream.closeEntry();
                    } finally {
                    }
                }
            }
        } finally {
            IoUtils.closeQuietly(zipOutputStream);
        }
    }

    public void rewriteActive(Rewriter rewriter, long j) throws IOException {
        rewriteSingle(rewriter, getActiveName(j));
    }

    @Deprecated
    public void combineActive(final Reader reader, final Writer writer, long j) throws IOException {
        rewriteActive(new Rewriter() {
            @Override
            public void reset() {
            }

            @Override
            public void read(InputStream inputStream) throws IOException {
                reader.read(inputStream);
            }

            @Override
            public boolean shouldWrite() {
                return true;
            }

            @Override
            public void write(OutputStream outputStream) throws IOException {
                writer.write(outputStream);
            }
        }, j);
    }

    public void rewriteAll(Rewriter rewriter) throws IOException {
        FileInfo fileInfo = new FileInfo(this.mPrefix);
        for (String str : this.mBasePath.list()) {
            if (fileInfo.parse(str)) {
                rewriteSingle(rewriter, str);
            }
        }
    }

    private void rewriteSingle(Rewriter rewriter, String str) throws IOException {
        File file = new File(this.mBasePath, str);
        rewriter.reset();
        if (file.exists()) {
            readFile(file, rewriter);
            if (rewriter.shouldWrite()) {
                File file2 = new File(this.mBasePath, str + SUFFIX_BACKUP);
                file.renameTo(file2);
                try {
                    writeFile(file, rewriter);
                    file2.delete();
                    return;
                } catch (Throwable th) {
                    file.delete();
                    file2.renameTo(file);
                    throw rethrowAsIoException(th);
                }
            }
            return;
        }
        File file3 = new File(this.mBasePath, str + SUFFIX_NO_BACKUP);
        file3.createNewFile();
        try {
            writeFile(file, rewriter);
            file3.delete();
        } catch (Throwable th2) {
            file.delete();
            file3.delete();
            throw rethrowAsIoException(th2);
        }
    }

    public void readMatching(Reader reader, long j, long j2) throws IOException {
        FileInfo fileInfo = new FileInfo(this.mPrefix);
        for (String str : this.mBasePath.list()) {
            if (fileInfo.parse(str) && fileInfo.startMillis <= j2 && j <= fileInfo.endMillis) {
                readFile(new File(this.mBasePath, str), reader);
            }
        }
    }

    private String getActiveName(long j) {
        FileInfo fileInfo = new FileInfo(this.mPrefix);
        String str = null;
        long j2 = Long.MAX_VALUE;
        for (String str2 : this.mBasePath.list()) {
            if (fileInfo.parse(str2) && fileInfo.isActive() && fileInfo.startMillis < j && fileInfo.startMillis < j2) {
                j2 = fileInfo.startMillis;
                str = str2;
            }
        }
        if (str != null) {
            return str;
        }
        fileInfo.startMillis = j;
        fileInfo.endMillis = Long.MAX_VALUE;
        return fileInfo.build();
    }

    public void maybeRotate(long j) {
        long j2 = j - this.mRotateAgeMillis;
        long j3 = j - this.mDeleteAgeMillis;
        FileInfo fileInfo = new FileInfo(this.mPrefix);
        String[] list = this.mBasePath.list();
        if (list == null) {
            return;
        }
        for (String str : list) {
            if (fileInfo.parse(str)) {
                if (fileInfo.isActive()) {
                    if (fileInfo.startMillis <= j2) {
                        fileInfo.endMillis = j;
                        new File(this.mBasePath, str).renameTo(new File(this.mBasePath, fileInfo.build()));
                    }
                } else if (fileInfo.endMillis <= j3) {
                    new File(this.mBasePath, str).delete();
                }
            }
        }
    }

    private static void readFile(File file, Reader reader) throws IOException {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
        try {
            reader.read(bufferedInputStream);
        } finally {
            IoUtils.closeQuietly(bufferedInputStream);
        }
    }

    private static void writeFile(File file, Writer writer) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        try {
            writer.write(bufferedOutputStream);
            bufferedOutputStream.flush();
        } finally {
            FileUtils.sync(fileOutputStream);
            IoUtils.closeQuietly(bufferedOutputStream);
        }
    }

    private static IOException rethrowAsIoException(Throwable th) throws IOException {
        if (th instanceof IOException) {
            throw ((IOException) th);
        }
        throw new IOException(th.getMessage(), th);
    }

    private static class FileInfo {
        public long endMillis;
        public final String prefix;
        public long startMillis;

        public FileInfo(String str) {
            this.prefix = (String) Preconditions.checkNotNull(str);
        }

        public boolean parse(String str) {
            this.endMillis = -1L;
            this.startMillis = -1L;
            int iLastIndexOf = str.lastIndexOf(46);
            int iLastIndexOf2 = str.lastIndexOf(45);
            if (iLastIndexOf == -1 || iLastIndexOf2 == -1 || !this.prefix.equals(str.substring(0, iLastIndexOf))) {
                return false;
            }
            try {
                this.startMillis = Long.parseLong(str.substring(iLastIndexOf + 1, iLastIndexOf2));
                if (str.length() - iLastIndexOf2 == 1) {
                    this.endMillis = Long.MAX_VALUE;
                } else {
                    this.endMillis = Long.parseLong(str.substring(iLastIndexOf2 + 1));
                }
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        public String build() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.prefix);
            sb.append('.');
            sb.append(this.startMillis);
            sb.append('-');
            if (this.endMillis != Long.MAX_VALUE) {
                sb.append(this.endMillis);
            }
            return sb.toString();
        }

        public boolean isActive() {
            return this.endMillis == Long.MAX_VALUE;
        }
    }
}
