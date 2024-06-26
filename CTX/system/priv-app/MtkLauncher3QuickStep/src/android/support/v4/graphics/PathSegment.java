package android.support.v4.graphics;

import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.v4.util.Preconditions;
/* loaded from: classes.dex */
public final class PathSegment {
    private final PointF mEnd;
    private final float mEndFraction;
    private final PointF mStart;
    private final float mStartFraction;

    public PathSegment(@NonNull PointF start, float startFraction, @NonNull PointF end, float endFraction) {
        this.mStart = (PointF) Preconditions.checkNotNull(start, "start == null");
        this.mStartFraction = startFraction;
        this.mEnd = (PointF) Preconditions.checkNotNull(end, "end == null");
        this.mEndFraction = endFraction;
    }

    @NonNull
    public PointF getStart() {
        return this.mStart;
    }

    public float getStartFraction() {
        return this.mStartFraction;
    }

    @NonNull
    public PointF getEnd() {
        return this.mEnd;
    }

    public float getEndFraction() {
        return this.mEndFraction;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof PathSegment) {
            PathSegment that = (PathSegment) o;
            return Float.compare(this.mStartFraction, that.mStartFraction) == 0 && Float.compare(this.mEndFraction, that.mEndFraction) == 0 && this.mStart.equals(that.mStart) && this.mEnd.equals(that.mEnd);
        }
        return false;
    }

    public int hashCode() {
        int result = this.mStart.hashCode();
        return (31 * ((31 * ((31 * result) + (this.mStartFraction != 0.0f ? Float.floatToIntBits(this.mStartFraction) : 0))) + this.mEnd.hashCode())) + (this.mEndFraction != 0.0f ? Float.floatToIntBits(this.mEndFraction) : 0);
    }

    public String toString() {
        return "PathSegment{start=" + this.mStart + ", startFraction=" + this.mStartFraction + ", end=" + this.mEnd + ", endFraction=" + this.mEndFraction + '}';
    }
}
