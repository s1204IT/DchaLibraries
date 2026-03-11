package android.support.v17.leanback.widget;

import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v17.leanback.widget.GridLayoutManager;
import android.support.v17.leanback.widget.ItemAlignmentFacet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

class ItemAlignmentFacetHelper {
    private static Rect sRect = new Rect();

    ItemAlignmentFacetHelper() {
    }

    static int getAlignmentPosition(View itemView, ItemAlignmentFacet.ItemAlignmentDef facet, int orientation) {
        GridLayoutManager.LayoutParams p = (GridLayoutManager.LayoutParams) itemView.getLayoutParams();
        View view = itemView;
        if (facet.mViewId != 0 && (view = itemView.findViewById(facet.mViewId)) == null) {
            view = itemView;
        }
        int alignPos = facet.mOffset;
        if (orientation == 0) {
            if (facet.mOffset >= 0) {
                if (facet.mOffsetWithPadding) {
                    alignPos += view.getPaddingLeft();
                }
            } else if (facet.mOffsetWithPadding) {
                alignPos -= view.getPaddingRight();
            }
            if (facet.mOffsetPercent != -1.0f) {
                alignPos = (int) ((((view == itemView ? p.getOpticalWidth(view) : view.getWidth()) * facet.mOffsetPercent) / 100.0f) + alignPos);
            }
            if (itemView != view) {
                sRect.left = alignPos;
                ((ViewGroup) itemView).offsetDescendantRectToMyCoords(view, sRect);
                return sRect.left - p.getOpticalLeftInset();
            }
            return alignPos;
        }
        if (facet.mOffset >= 0) {
            if (facet.mOffsetWithPadding) {
                alignPos += view.getPaddingTop();
            }
        } else if (facet.mOffsetWithPadding) {
            alignPos -= view.getPaddingBottom();
        }
        if (facet.mOffsetPercent != -1.0f) {
            alignPos = (int) ((((view == itemView ? p.getOpticalHeight(view) : view.getHeight()) * facet.mOffsetPercent) / 100.0f) + alignPos);
        }
        if (itemView != view) {
            sRect.top = alignPos;
            ((ViewGroup) itemView).offsetDescendantRectToMyCoords(view, sRect);
            alignPos = sRect.top - p.getOpticalTopInset();
        }
        if ((view instanceof TextView) && facet.isAlignedToTextViewBaseLine()) {
            Paint textPaint = ((TextView) view).getPaint();
            int titleViewTextHeight = -textPaint.getFontMetricsInt().top;
            return alignPos + titleViewTextHeight;
        }
        return alignPos;
    }
}
