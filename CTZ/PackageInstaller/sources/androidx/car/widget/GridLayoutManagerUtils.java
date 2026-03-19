package androidx.car.widget;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

class GridLayoutManagerUtils {
    public static int getFirstRowItemCount(RecyclerView parent) {
        GridLayoutManager manager = (GridLayoutManager) parent.getLayoutManager();
        int itemCount = parent.getAdapter().getItemCount();
        int spanCount = manager.getSpanCount();
        int spanSum = 0;
        int pos = 0;
        while (pos < itemCount && spanSum < spanCount) {
            spanSum += manager.getSpanSizeLookup().getSpanSize(pos);
            pos++;
        }
        return pos;
    }

    public static int getSpanIndex(View item) {
        GridLayoutManager.LayoutParams layoutParams = (GridLayoutManager.LayoutParams) item.getLayoutParams();
        return layoutParams.getSpanIndex();
    }

    public static int getSpanSize(View item) {
        GridLayoutManager.LayoutParams layoutParams = (GridLayoutManager.LayoutParams) item.getLayoutParams();
        return layoutParams.getSpanSize();
    }

    public static int getLastIndexOnSameRow(int index, RecyclerView parent) {
        int spanCount = ((GridLayoutManager) parent.getLayoutManager()).getSpanCount();
        int spanSum = getSpanIndex(parent.getChildAt(index));
        int spanSum2 = spanSum;
        for (int spanSum3 = index; spanSum3 < parent.getChildCount(); spanSum3++) {
            spanSum2 += getSpanSize(parent.getChildAt(spanSum3));
            if (spanSum2 > spanCount) {
                return spanSum3 - 1;
            }
        }
        int i = parent.getChildCount();
        return i - 1;
    }
}
