package android.system;

import libcore.util.Objects;

public final class StructTimespec implements Comparable<StructTimespec> {
    public final long tv_nsec;
    public final long tv_sec;

    public StructTimespec(long j, long j2) {
        this.tv_sec = j;
        this.tv_nsec = j2;
        if (j2 < 0 || j2 > 999999999) {
            throw new IllegalArgumentException("tv_nsec value " + j2 + " is not in [0, 999999999]");
        }
    }

    @Override
    public int compareTo(StructTimespec structTimespec) {
        if (this.tv_sec > structTimespec.tv_sec) {
            return 1;
        }
        if (this.tv_sec < structTimespec.tv_sec) {
            return -1;
        }
        if (this.tv_nsec > structTimespec.tv_nsec) {
            return 1;
        }
        return this.tv_nsec < structTimespec.tv_nsec ? -1 : 0;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        StructTimespec structTimespec = (StructTimespec) obj;
        if (this.tv_sec == structTimespec.tv_sec && this.tv_nsec == structTimespec.tv_nsec) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * ((int) (this.tv_sec ^ (this.tv_sec >>> 32)))) + ((int) (this.tv_nsec ^ (this.tv_nsec >>> 32)));
    }

    public String toString() {
        return Objects.toString(this);
    }
}
