package java.util.zip;

import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ZipEntry implements ZipConstants, Cloneable {
    public static final int DEFLATED = 8;
    static final long DOSTIME_BEFORE_1980 = 2162688;
    public static final int STORED = 0;
    public static final long UPPER_DOSTIME_BOUND = 4036608000000L;
    FileTime atime;
    String comment;
    long crc;
    long csize;
    FileTime ctime;
    long dataOffset;
    byte[] extra;
    int flag;
    int method;
    FileTime mtime;
    String name;
    long size;
    long xdostime;

    public ZipEntry(String str, String str2, long j, long j2, long j3, int i, int i2, byte[] bArr, long j4) {
        this.xdostime = -1L;
        this.crc = -1L;
        this.size = -1L;
        this.csize = -1L;
        this.method = -1;
        this.flag = 0;
        this.name = str;
        this.comment = str2;
        this.crc = j;
        this.csize = j2;
        this.size = j3;
        this.method = i;
        this.xdostime = i2;
        this.dataOffset = j4;
        setExtra0(bArr, false);
    }

    public ZipEntry(String str) {
        this.xdostime = -1L;
        this.crc = -1L;
        this.size = -1L;
        this.csize = -1L;
        this.method = -1;
        this.flag = 0;
        Objects.requireNonNull(str, "name");
        if (str.getBytes(StandardCharsets.UTF_8).length > 65535) {
            throw new IllegalArgumentException(str + " too long: " + str.getBytes(StandardCharsets.UTF_8).length);
        }
        this.name = str;
    }

    public ZipEntry(ZipEntry zipEntry) {
        this.xdostime = -1L;
        this.crc = -1L;
        this.size = -1L;
        this.csize = -1L;
        this.method = -1;
        this.flag = 0;
        Objects.requireNonNull(zipEntry, "entry");
        this.name = zipEntry.name;
        this.xdostime = zipEntry.xdostime;
        this.mtime = zipEntry.mtime;
        this.atime = zipEntry.atime;
        this.ctime = zipEntry.ctime;
        this.crc = zipEntry.crc;
        this.size = zipEntry.size;
        this.csize = zipEntry.csize;
        this.method = zipEntry.method;
        this.flag = zipEntry.flag;
        this.extra = zipEntry.extra;
        this.comment = zipEntry.comment;
        this.dataOffset = zipEntry.dataOffset;
    }

    ZipEntry() {
        this.xdostime = -1L;
        this.crc = -1L;
        this.size = -1L;
        this.csize = -1L;
        this.method = -1;
        this.flag = 0;
    }

    public long getDataOffset() {
        return this.dataOffset;
    }

    public String getName() {
        return this.name;
    }

    public void setTime(long j) {
        this.xdostime = ZipUtils.javaToExtendedDosTime(j);
        if (this.xdostime != DOSTIME_BEFORE_1980 && j <= UPPER_DOSTIME_BOUND) {
            this.mtime = null;
        } else {
            this.mtime = FileTime.from(j, TimeUnit.MILLISECONDS);
        }
    }

    public long getTime() {
        if (this.mtime != null) {
            return this.mtime.toMillis();
        }
        if (this.xdostime != -1) {
            return ZipUtils.extendedDosToJavaTime(this.xdostime);
        }
        return -1L;
    }

    public ZipEntry setLastModifiedTime(FileTime fileTime) {
        this.mtime = (FileTime) Objects.requireNonNull(fileTime, "lastModifiedTime");
        this.xdostime = ZipUtils.javaToExtendedDosTime(fileTime.to(TimeUnit.MILLISECONDS));
        return this;
    }

    public FileTime getLastModifiedTime() {
        if (this.mtime != null) {
            return this.mtime;
        }
        if (this.xdostime == -1) {
            return null;
        }
        return FileTime.from(getTime(), TimeUnit.MILLISECONDS);
    }

    public ZipEntry setLastAccessTime(FileTime fileTime) {
        this.atime = (FileTime) Objects.requireNonNull(fileTime, "lastAccessTime");
        return this;
    }

    public FileTime getLastAccessTime() {
        return this.atime;
    }

    public ZipEntry setCreationTime(FileTime fileTime) {
        this.ctime = (FileTime) Objects.requireNonNull(fileTime, "creationTime");
        return this;
    }

    public FileTime getCreationTime() {
        return this.ctime;
    }

    public void setSize(long j) {
        if (j < 0) {
            throw new IllegalArgumentException("invalid entry size");
        }
        this.size = j;
    }

    public long getSize() {
        return this.size;
    }

    public long getCompressedSize() {
        return this.csize;
    }

    public void setCompressedSize(long j) {
        this.csize = j;
    }

    public void setCrc(long j) {
        if (j < 0 || j > 4294967295L) {
            throw new IllegalArgumentException("invalid entry crc-32");
        }
        this.crc = j;
    }

    public long getCrc() {
        return this.crc;
    }

    public void setMethod(int i) {
        if (i != 0 && i != 8) {
            throw new IllegalArgumentException("invalid compression method");
        }
        this.method = i;
    }

    public int getMethod() {
        return this.method;
    }

    public void setExtra(byte[] bArr) {
        setExtra0(bArr, false);
    }

    void setExtra0(byte[] bArr, boolean z) {
        int i;
        if (bArr != null) {
            if (bArr.length > 65535) {
                throw new IllegalArgumentException("invalid extra field length");
            }
            int i2 = 0;
            int length = bArr.length;
            while (true) {
                int i3 = i2 + 4;
                if (i3 >= length) {
                    break;
                }
                int i4 = ZipUtils.get16(bArr, i2);
                int i5 = ZipUtils.get16(bArr, i2 + 2);
                int i6 = i3 + i5;
                if (i6 > length) {
                    break;
                }
                int i7 = 1;
                if (i4 != 1) {
                    if (i4 != 10) {
                        if (i4 == 21589) {
                            int unsignedInt = Byte.toUnsignedInt(bArr[i3]);
                            if ((unsignedInt & 1) != 0 && 5 <= i5) {
                                this.mtime = ZipUtils.unixTimeToFileTime(ZipUtils.get32(bArr, i3 + 1));
                                i7 = 5;
                            }
                            if ((unsignedInt & 2) != 0 && (i = i7 + 4) <= i5) {
                                this.atime = ZipUtils.unixTimeToFileTime(ZipUtils.get32(bArr, i7 + i3));
                                i7 = i;
                            }
                            if ((unsignedInt & 4) != 0 && i7 + 4 <= i5) {
                                this.ctime = ZipUtils.unixTimeToFileTime(ZipUtils.get32(bArr, i3 + i7));
                            }
                        }
                    } else if (i5 >= 32) {
                        int i8 = i3 + 4;
                        if (ZipUtils.get16(bArr, i8) == 1 && ZipUtils.get16(bArr, i8 + 2) == 24) {
                            this.mtime = ZipUtils.winTimeToFileTime(ZipUtils.get64(bArr, i8 + 4));
                            this.atime = ZipUtils.winTimeToFileTime(ZipUtils.get64(bArr, i8 + 12));
                            this.ctime = ZipUtils.winTimeToFileTime(ZipUtils.get64(bArr, i8 + 20));
                        }
                    }
                } else if (z && i5 >= 16) {
                    this.size = ZipUtils.get64(bArr, i3);
                    this.csize = ZipUtils.get64(bArr, i3 + 8);
                }
                i2 = i6;
            }
        }
        this.extra = bArr;
    }

    public byte[] getExtra() {
        return this.extra;
    }

    public void setComment(String str) {
        if (str == null) {
            this.comment = null;
            return;
        }
        if (str.getBytes(StandardCharsets.UTF_8).length > 65535) {
            throw new IllegalArgumentException(str + " too long: " + str.getBytes(StandardCharsets.UTF_8).length);
        }
        this.comment = str;
    }

    public String getComment() {
        return this.comment;
    }

    public boolean isDirectory() {
        return this.name.endsWith("/");
    }

    public String toString() {
        return getName();
    }

    public int hashCode() {
        return this.name.hashCode();
    }

    public Object clone() {
        try {
            ZipEntry zipEntry = (ZipEntry) super.clone();
            zipEntry.extra = this.extra == null ? null : (byte[]) this.extra.clone();
            return zipEntry;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
