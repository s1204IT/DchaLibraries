package com.android.timezone.distro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class TimeZoneDistro {
    private static final int BUFFER_SIZE = 8192;
    public static final String DISTRO_VERSION_FILE_NAME = "distro_version";
    public static final String FILE_NAME = "distro.zip";
    public static final String ICU_DATA_FILE_NAME = "icu/icu_tzdata.dat";
    private static final long MAX_GET_ENTRY_CONTENTS_SIZE = 131072;
    public static final String TZDATA_FILE_NAME = "tzdata";
    public static final String TZLOOKUP_FILE_NAME = "tzlookup.xml";
    private final InputStream inputStream;

    public TimeZoneDistro(byte[] bArr) {
        this(new ByteArrayInputStream(bArr));
    }

    public TimeZoneDistro(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public DistroVersion getDistroVersion() throws Exception {
        byte[] entryContents = getEntryContents(this.inputStream, DISTRO_VERSION_FILE_NAME);
        if (entryContents == null) {
            throw new DistroException("Distro version file entry not found");
        }
        return DistroVersion.fromBytes(entryContents);
    }

    private static byte[] getEntryContents(InputStream inputStream, String str) throws Exception {
        ZipEntry nextEntry;
        Throwable th;
        Throwable th2;
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        do {
            try {
                nextEntry = zipInputStream.getNextEntry();
                if (nextEntry == null) {
                    return null;
                }
            } finally {
                $closeResource(null, zipInputStream);
            }
        } while (!str.equals(nextEntry.getName()));
        if (nextEntry.getSize() > MAX_GET_ENTRY_CONTENTS_SIZE) {
            throw new IOException("Entry " + str + " too large: " + nextEntry.getSize());
        }
        byte[] bArr = new byte[8192];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        while (true) {
            try {
                int i = zipInputStream.read(bArr);
                if (i == -1) {
                    byte[] byteArray = byteArrayOutputStream.toByteArray();
                    $closeResource(null, byteArrayOutputStream);
                    return byteArray;
                }
                byteArrayOutputStream.write(bArr, 0, i);
            } catch (Throwable th3) {
                try {
                    throw th3;
                } catch (Throwable th4) {
                    th = th3;
                    th2 = th4;
                    $closeResource(th, byteArrayOutputStream);
                    throw th2;
                }
            }
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

    public void extractTo(File file) throws Exception {
        extractZipSafely(this.inputStream, file, true);
    }

    static void extractZipSafely(InputStream inputStream, File file, boolean z) throws Exception {
        Throwable th;
        FileUtils.ensureDirectoriesExist(file, z);
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        try {
            byte[] bArr = new byte[8192];
            while (true) {
                ZipEntry nextEntry = zipInputStream.getNextEntry();
                if (nextEntry != null) {
                    File fileCreateSubFile = FileUtils.createSubFile(file, nextEntry.getName());
                    if (nextEntry.isDirectory()) {
                        FileUtils.ensureDirectoriesExist(fileCreateSubFile, z);
                    } else {
                        if (!fileCreateSubFile.getParentFile().exists()) {
                            FileUtils.ensureDirectoriesExist(fileCreateSubFile.getParentFile(), z);
                        }
                        FileOutputStream fileOutputStream = new FileOutputStream(fileCreateSubFile);
                        while (true) {
                            try {
                                int i = zipInputStream.read(bArr);
                                if (i == -1) {
                                    break;
                                } else {
                                    fileOutputStream.write(bArr, 0, i);
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                th = null;
                                $closeResource(th, fileOutputStream);
                                throw th;
                            }
                        }
                        fileOutputStream.getFD().sync();
                        $closeResource(null, fileOutputStream);
                        if (z) {
                            FileUtils.makeWorldReadable(fileCreateSubFile);
                        }
                    }
                } else {
                    return;
                }
            }
        } finally {
            $closeResource(null, zipInputStream);
        }
    }
}
