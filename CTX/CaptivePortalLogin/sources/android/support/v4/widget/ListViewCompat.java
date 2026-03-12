package android.support.v4.widget;

import android.os.Build;
import android.widget.ListView;

public final class ListViewCompat {
    public static boolean canScrollList(ListView listView, int direction) {
        if (Build.VERSION.SDK_INT >= 19) {
            return listView.canScrollList(direction);
        }
        int childCount = listView.getChildCount();
        if (childCount == 0) {
            return false;
        }
        int firstPosition = listView.getFirstVisiblePosition();
        if (direction > 0) {
            int lastBottom = listView.getChildAt(childCount - 1).getBottom();
            int lastPosition = firstPosition + childCount;
            if (lastPosition >= listView.getCount() && lastBottom <= listView.getHeight() - listView.getListPaddingBottom()) {
                return false;
            }
            return true;
        }
        int firstTop = listView.getChildAt(0).getTop();
        if (firstPosition <= 0 && firstTop >= listView.getListPaddingTop()) {
            return false;
        }
        return true;
    }
}
