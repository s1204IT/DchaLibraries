package com.android.launcher3;

import android.graphics.Rect;
import com.android.launcher3.accessibility.DragViewStateAnnouncer;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.dragndrop.DragView;

/* loaded from: classes.dex */
public interface DropTarget {
    boolean acceptDrop(DragObject dragObject);

    void getHitRectRelativeToDragLayer(Rect rect);

    boolean isDropEnabled();

    void onDragEnter(DragObject dragObject);

    void onDragExit(DragObject dragObject);

    void onDragOver(DragObject dragObject);

    void onDrop(DragObject dragObject, DragOptions dragOptions);

    void prepareAccessibilityDrop();

    public static class DragObject {
        public boolean accessibleDrag;
        public DragViewStateAnnouncer stateAnnouncer;
        public int x = -1;
        public int y = -1;
        public int xOffset = -1;
        public int yOffset = -1;
        public boolean dragComplete = false;
        public DragView dragView = null;
        public ItemInfo dragInfo = null;
        public ItemInfo originalDragInfo = null;
        public DragSource dragSource = null;
        public boolean cancelled = false;
        public boolean deferDragViewCleanupPostAnimation = true;

        public final float[] getVisualCenter(float[] fArr) {
            if (fArr == null) {
                fArr = new float[2];
            }
            int i = this.x - this.xOffset;
            int i2 = this.y - this.yOffset;
            fArr[0] = i + (this.dragView.getDragRegion().width() / 2);
            fArr[1] = i2 + (this.dragView.getDragRegion().height() / 2);
            return fArr;
        }
    }
}
