package android.support.v17.leanback.widget;

import android.graphics.Rect;
import android.support.v17.leanback.widget.GridLayoutManager;
import android.support.v17.leanback.widget.ItemAlignmentFacet;
import android.view.View;
import android.view.ViewGroup;

class ItemAlignmentFacetHelper {
    private static Rect sRect = new Rect();

    static int getAlignmentPosition(View itemView, ItemAlignmentFacet.ItemAlignmentDef facet, int orientation) {
        int alignPos;
        GridLayoutManager.LayoutParams p = (GridLayoutManager.LayoutParams) itemView.getLayoutParams();
        View view = itemView;
        if (facet.mViewId != 0 && (view = itemView.findViewById(facet.mViewId)) == null) {
            view = itemView;
        }
        int alignPos2 = facet.mOffset;
        if (orientation == 0) {
            if (itemView.getLayoutDirection() == 1) {
                int alignPos3 = (view == itemView ? p.getOpticalWidth(view) : view.getWidth()) - alignPos2;
                if (facet.mOffsetWithPadding) {
                    if (facet.mOffsetPercent == 0.0f) {
                        alignPos3 -= view.getPaddingRight();
                    } else if (facet.mOffsetPercent == 100.0f) {
                        alignPos3 += view.getPaddingLeft();
                    }
                }
                if (facet.mOffsetPercent != -1.0f) {
                    alignPos3 -= (int) (((view == itemView ? p.getOpticalWidth(view) : view.getWidth()) * facet.mOffsetPercent) / 100.0f);
                }
                if (itemView != view) {
                    sRect.right = alignPos3;
                    ((ViewGroup) itemView).offsetDescendantRectToMyCoords(view, sRect);
                    return sRect.right + p.getOpticalRightInset();
                }
                return alignPos3;
            }
            if (facet.mOffsetWithPadding) {
                if (facet.mOffsetPercent == 0.0f) {
                    alignPos2 += view.getPaddingLeft();
                } else if (facet.mOffsetPercent == 100.0f) {
                    alignPos2 -= view.getPaddingRight();
                }
            }
            if (facet.mOffsetPercent != -1.0f) {
                alignPos2 += (int) (((view == itemView ? p.getOpticalWidth(view) : view.getWidth()) * facet.mOffsetPercent) / 100.0f);
            }
            int alignPos4 = alignPos2;
            if (itemView != view) {
                sRect.left = alignPos4;
                ((ViewGroup) itemView).offsetDescendantRectToMyCoords(view, sRect);
                return sRect.left - p.getOpticalLeftInset();
            }
            return alignPos4;
        }
        if (facet.mOffsetWithPadding) {
            if (facet.mOffsetPercent == 0.0f) {
                alignPos2 += view.getPaddingTop();
            } else if (facet.mOffsetPercent == 100.0f) {
                alignPos2 -= view.getPaddingBottom();
            }
        }
        if (facet.mOffsetPercent != -1.0f) {
            alignPos2 += (int) (((view == itemView ? p.getOpticalHeight(view) : view.getHeight()) * facet.mOffsetPercent) / 100.0f);
        }
        if (itemView != view) {
            sRect.top = alignPos2;
            ((ViewGroup) itemView).offsetDescendantRectToMyCoords(view, sRect);
            alignPos = sRect.top - p.getOpticalTopInset();
        } else {
            alignPos = alignPos2;
        }
        if (facet.isAlignedToTextViewBaseLine()) {
            return alignPos + view.getBaseline();
        }
        return alignPos;
    }
}
