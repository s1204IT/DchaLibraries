package android.system;

import libcore.util.Objects;

public final class StructStat {
    public final StructTimespec st_atim;
    public final long st_atime;
    public final long st_blksize;
    public final long st_blocks;
    public final StructTimespec st_ctim;
    public final long st_ctime;
    public final long st_dev;
    public final int st_gid;
    public final long st_ino;
    public final int st_mode;
    public final StructTimespec st_mtim;
    public final long st_mtime;
    public final long st_nlink;
    public final long st_rdev;
    public final long st_size;
    public final int st_uid;

    public StructStat(long j, long j2, int i, long j3, int i2, int i3, long j4, long j5, long j6, long j7, long j8, long j9, long j10) {
        this(j, j2, i, j3, i2, i3, j4, j5, new StructTimespec(j6, 0L), new StructTimespec(j7, 0L), new StructTimespec(j8, 0L), j9, j10);
    }

    public StructStat(long j, long j2, int i, long j3, int i2, int i3, long j4, long j5, StructTimespec structTimespec, StructTimespec structTimespec2, StructTimespec structTimespec3, long j6, long j7) {
        this.st_dev = j;
        this.st_ino = j2;
        this.st_mode = i;
        this.st_nlink = j3;
        this.st_uid = i2;
        this.st_gid = i3;
        this.st_rdev = j4;
        this.st_size = j5;
        this.st_atime = structTimespec.tv_sec;
        this.st_mtime = structTimespec2.tv_sec;
        this.st_ctime = structTimespec3.tv_sec;
        this.st_atim = structTimespec;
        this.st_mtim = structTimespec2;
        this.st_ctim = structTimespec3;
        this.st_blksize = j6;
        this.st_blocks = j7;
    }

    public String toString() {
        return Objects.toString(this);
    }
}
