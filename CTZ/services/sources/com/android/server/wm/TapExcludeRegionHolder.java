package com.android.server.wm;

import android.graphics.Rect;
import android.graphics.Region;
import android.util.SparseArray;

class TapExcludeRegionHolder {
    private SparseArray<Rect> mTapExcludeRects = new SparseArray<>();

    TapExcludeRegionHolder() {
    }

    void updateRegion(int i, int i2, int i3, int i4, int i5) {
        if (i4 <= 0 || i5 <= 0) {
            this.mTapExcludeRects.remove(i);
            return;
        }
        Rect rect = this.mTapExcludeRects.get(i);
        if (rect == null) {
            rect = new Rect();
        }
        rect.set(i2, i3, i4 + i2, i5 + i3);
        this.mTapExcludeRects.put(i, rect);
    }

    void amendRegion(Region region, Rect rect) {
        for (int size = this.mTapExcludeRects.size() - 1; size >= 0; size--) {
            Rect rectValueAt = this.mTapExcludeRects.valueAt(size);
            rectValueAt.intersect(rect);
            region.union(rectValueAt);
        }
    }
}
