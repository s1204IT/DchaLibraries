package androidx.slice;

import androidx.versionedparcelable.VersionedParcelable;

public final class SliceSpec implements VersionedParcelable {
    int mRevision;
    String mType;

    public SliceSpec() {
    }

    public SliceSpec(String type, int revision) {
        this.mType = type;
        this.mRevision = revision;
    }

    public String getType() {
        return this.mType;
    }

    public int getRevision() {
        return this.mRevision;
    }

    public boolean canRender(SliceSpec candidate) {
        return this.mType.equals(candidate.mType) && this.mRevision >= candidate.mRevision;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof SliceSpec)) {
            return false;
        }
        SliceSpec other = (SliceSpec) obj;
        return this.mType.equals(other.mType) && this.mRevision == other.mRevision;
    }

    public int hashCode() {
        return this.mType.hashCode() + this.mRevision;
    }

    public String toString() {
        return String.format("SliceSpec{%s,%d}", this.mType, Integer.valueOf(this.mRevision));
    }
}
