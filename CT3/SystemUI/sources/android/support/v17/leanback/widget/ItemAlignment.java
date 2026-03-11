package android.support.v17.leanback.widget;

import android.support.v17.leanback.widget.ItemAlignmentFacet;
import android.view.View;

class ItemAlignment {
    private int mOrientation = 0;
    public final Axis vertical = new Axis(1);
    public final Axis horizontal = new Axis(0);
    private Axis mMainAxis = this.horizontal;
    private Axis mSecondAxis = this.vertical;

    ItemAlignment() {
    }

    static final class Axis extends ItemAlignmentFacet.ItemAlignmentDef {
        private int mOrientation;

        Axis(int orientation) {
            this.mOrientation = orientation;
        }

        public int getAlignmentPosition(View itemView) {
            return ItemAlignmentFacetHelper.getAlignmentPosition(itemView, this, this.mOrientation);
        }
    }

    public final void setOrientation(int orientation) {
        this.mOrientation = orientation;
        if (this.mOrientation == 0) {
            this.mMainAxis = this.horizontal;
            this.mSecondAxis = this.vertical;
        } else {
            this.mMainAxis = this.vertical;
            this.mSecondAxis = this.horizontal;
        }
    }
}
