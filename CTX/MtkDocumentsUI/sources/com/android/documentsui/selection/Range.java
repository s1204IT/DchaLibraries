package com.android.documentsui.selection;

import android.support.v4.util.Preconditions;

final class Range {
    private final int mBegin;
    private final Callbacks mCallbacks;
    private int mEnd = -1;

    public Range(Callbacks callbacks, int i) {
        this.mCallbacks = callbacks;
        this.mBegin = i;
    }

    void extendSelection(int i, int i2) {
        Preconditions.checkArgument(i != -1, "Position cannot be NO_POSITION.");
        if (this.mEnd == -1 || this.mEnd == this.mBegin) {
            this.mEnd = -1;
            establishRange(i, i2);
        } else {
            reviseRange(i, i2);
        }
    }

    private void establishRange(int i, int i2) {
        Preconditions.checkArgument(this.mEnd == -1, "End has already been set.");
        if (i == this.mBegin) {
            this.mEnd = i;
        }
        if (i > this.mBegin) {
            updateRange(this.mBegin + 1, i, true, i2);
        } else if (i < this.mBegin) {
            updateRange(i, this.mBegin - 1, true, i2);
        }
        this.mEnd = i;
    }

    private void reviseRange(int i, int i2) {
        Preconditions.checkArgument(this.mEnd != -1, "End must already be set.");
        Preconditions.checkArgument(this.mBegin != this.mEnd, "Beging and end point to same position.");
        int i3 = this.mEnd;
        if (this.mEnd > this.mBegin) {
            reviseAscendingRange(i, i2);
        } else if (this.mEnd < this.mBegin) {
            reviseDescendingRange(i, i2);
        }
        this.mEnd = i;
    }

    private void reviseAscendingRange(int i, int i2) {
        if (i < this.mEnd) {
            if (i < this.mBegin) {
                updateRange(this.mBegin + 1, this.mEnd, false, i2);
                updateRange(i, this.mBegin - 1, true, i2);
                return;
            } else {
                updateRange(i + 1, this.mEnd, false, i2);
                return;
            }
        }
        if (i > this.mEnd) {
            updateRange(this.mEnd + 1, i, true, i2);
        }
    }

    private void reviseDescendingRange(int i, int i2) {
        if (i > this.mEnd) {
            if (i > this.mBegin) {
                updateRange(this.mEnd, this.mBegin - 1, false, i2);
                updateRange(this.mBegin + 1, i, true, i2);
                return;
            } else {
                updateRange(this.mEnd, i - 1, false, i2);
                return;
            }
        }
        if (i < this.mEnd) {
            updateRange(i, this.mEnd - 1, true, i2);
        }
    }

    private void updateRange(int i, int i2, boolean z, int i3) {
        this.mCallbacks.updateForRange(i, i2, z, i3);
    }

    public String toString() {
        return "Range{begin=" + this.mBegin + ", end=" + this.mEnd + "}";
    }

    static abstract class Callbacks {
        abstract void updateForRange(int i, int i2, boolean z, int i3);

        Callbacks() {
        }
    }
}
