package libcore.tzdata.update;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ConfigBundle {
    private static final int BUFFER_SIZE = 8192;
    public static final String CHECKSUMS_FILE_NAME = "checksums";
    public static final String ICU_DATA_FILE_NAME = "icu/icu_tzdata.dat";
    public static final String TZ_DATA_VERSION_FILE_NAME = "tzdata_version";
    public static final String ZONEINFO_FILE_NAME = "tzdata";
    private final byte[] bytes;

    public ConfigBundle(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBundleBytes() {
        return this.bytes;
    }

    public void extractTo(File targetDir) throws Throwable {
        extractZipSafely(new ByteArrayInputStream(this.bytes), targetDir, true);
    }

    static void extractZipSafely(InputStream is, File targetDir, boolean makeWorldReadable) throws Throwable {
        Throwable th;
        FileUtils.ensureDirectoriesExist(targetDir, makeWorldReadable);
        Throwable th2 = null;
        ZipInputStream zipInputStream = null;
        try {
            ZipInputStream zipInputStream2 = new ZipInputStream(is);
            try {
                try {
                    byte[] buffer = new byte[8192];
                    while (true) {
                        ZipEntry entry = zipInputStream2.getNextEntry();
                        if (entry == null) {
                            if (zipInputStream2 != null) {
                                try {
                                    zipInputStream2.close();
                                } catch (Throwable th3) {
                                    th2 = th3;
                                }
                            }
                            if (th2 != null) {
                                throw th2;
                            }
                            return;
                        }
                        String name = entry.getName();
                        File entryFile = FileUtils.createSubFile(targetDir, name);
                        if (entry.isDirectory()) {
                            FileUtils.ensureDirectoriesExist(entryFile, makeWorldReadable);
                        } else {
                            if (!entryFile.getParentFile().exists()) {
                                FileUtils.ensureDirectoriesExist(entryFile.getParentFile(), makeWorldReadable);
                            }
                            Throwable th4 = null;
                            FileOutputStream fileOutputStream = null;
                            try {
                                FileOutputStream fos = new FileOutputStream(entryFile);
                                while (true) {
                                    try {
                                        int count = zipInputStream2.read(buffer);
                                        if (count == -1) {
                                            break;
                                        } else {
                                            fos.write(buffer, 0, count);
                                        }
                                    } catch (Throwable th5) {
                                        th = th5;
                                        fileOutputStream = fos;
                                        if (fileOutputStream != null) {
                                        }
                                        if (th4 != null) {
                                        }
                                    }
                                }
                                fos.getFD().sync();
                                if (fos != null) {
                                    try {
                                        fos.close();
                                    } catch (Throwable th6) {
                                        th4 = th6;
                                    }
                                }
                                if (th4 != null) {
                                    throw th4;
                                }
                                if (makeWorldReadable) {
                                    FileUtils.makeWorldReadable(entryFile);
                                }
                            } catch (Throwable th7) {
                                th = th7;
                            }
                        }
                    }
                } catch (Throwable th8) {
                    th = th8;
                    th = null;
                    zipInputStream = zipInputStream2;
                    if (zipInputStream != null) {
                    }
                    if (th != null) {
                    }
                }
            } catch (Throwable th9) {
                th = th9;
                zipInputStream = zipInputStream2;
                try {
                    throw th;
                } catch (Throwable th10) {
                    th = th;
                    th = th10;
                    if (zipInputStream != null) {
                        try {
                            zipInputStream.close();
                        } catch (Throwable th11) {
                            if (th == null) {
                                th = th11;
                            } else if (th != th11) {
                                th.addSuppressed(th11);
                            }
                        }
                    }
                    if (th != null) {
                        throw th;
                    }
                    throw th;
                }
            }
        } catch (Throwable th12) {
            th = th12;
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConfigBundle that = (ConfigBundle) o;
        return Arrays.equals(this.bytes, that.bytes);
    }
}
