package sun.nio.fs;

class UnixFileKey {
    private final long st_dev;
    private final long st_ino;

    UnixFileKey(long j, long j2) {
        this.st_dev = j;
        this.st_ino = j2;
    }

    public int hashCode() {
        return ((int) (this.st_dev ^ (this.st_dev >>> 32))) + ((int) (this.st_ino ^ (this.st_ino >>> 32)));
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof UnixFileKey)) {
            return false;
        }
        UnixFileKey unixFileKey = (UnixFileKey) obj;
        return this.st_dev == unixFileKey.st_dev && this.st_ino == unixFileKey.st_ino;
    }

    public String toString() {
        return "(dev=" + Long.toHexString(this.st_dev) + ",ino=" + this.st_ino + ')';
    }
}
