package androidx.car.widget;

import android.content.Context;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class PagedSnapHelper extends LinearSnapHelper {
    private final Context mContext;
    private OrientationHelper mHorizontalHelper;
    private RecyclerView mRecyclerView;
    private OrientationHelper mVerticalHelper;

    public PagedSnapHelper(Context context) {
        this.mContext = context;
    }

    @Override
    public int[] calculateDistanceToFinalSnap(RecyclerView.LayoutManager layoutManager, View targetView) {
        int[] out = new int[2];
        out[0] = layoutManager.canScrollHorizontally() ? getHorizontalHelper(layoutManager).getDecoratedStart(targetView) : 0;
        out[1] = layoutManager.canScrollVertically() ? getVerticalHelper(layoutManager).getDecoratedStart(targetView) : 0;
        return out;
    }

    @Override
    public View findSnapView(RecyclerView.LayoutManager layoutManager) {
        int childCount = layoutManager.getChildCount();
        if (childCount == 0) {
            return null;
        }
        OrientationHelper orientationHelper = getOrientationHelper(layoutManager);
        if (childCount != 1) {
            View firstChild = this.mRecyclerView.getChildAt(0);
            if (firstChild.getHeight() <= this.mRecyclerView.getHeight() || orientationHelper.getDecoratedStart(firstChild) >= 0 || orientationHelper.getDecoratedEnd(firstChild) <= this.mRecyclerView.getHeight() * 0.3f) {
                View lastVisibleChild = layoutManager.getChildAt(childCount - 1);
                boolean lastItemVisible = layoutManager.getPosition(lastVisibleChild) == layoutManager.getItemCount() - 1;
                float lastItemPercentageVisible = lastItemVisible ? getPercentageVisible(lastVisibleChild, orientationHelper) : 0.0f;
                View closestChild = null;
                int closestDistanceToStart = Integer.MAX_VALUE;
                float closestPercentageVisible = 0.0f;
                for (int i = 0; i < childCount; i++) {
                    View child = layoutManager.getChildAt(i);
                    int startOffset = orientationHelper.getDecoratedStart(child);
                    if (Math.abs(startOffset) < closestDistanceToStart) {
                        float percentageVisible = getPercentageVisible(child, orientationHelper);
                        if (percentageVisible > 0.5f && percentageVisible > closestPercentageVisible) {
                            closestDistanceToStart = startOffset;
                            closestChild = child;
                            closestPercentageVisible = percentageVisible;
                        }
                    }
                }
                View childToReturn = closestChild;
                if (childToReturn == null || (lastItemVisible && lastItemPercentageVisible > closestPercentageVisible)) {
                    childToReturn = lastVisibleChild;
                }
                if (isValidSnapView(childToReturn, orientationHelper)) {
                    return childToReturn;
                }
                return null;
            }
            return null;
        }
        View firstChild2 = layoutManager.getChildAt(0);
        if (isValidSnapView(firstChild2, orientationHelper)) {
            return firstChild2;
        }
        return null;
    }

    private boolean isValidSnapView(View view, OrientationHelper helper) {
        return helper.getDecoratedMeasurement(view) <= helper.getLayoutManager().getHeight();
    }

    private float getPercentageVisible(View view, OrientationHelper helper) {
        int end = helper.getEnd();
        int viewStart = helper.getDecoratedStart(view);
        int viewEnd = helper.getDecoratedEnd(view);
        if (viewStart >= 0 && viewEnd <= end) {
            return 1.0f;
        }
        if (viewStart > 0 || viewEnd < end) {
            return viewStart < 0 ? 1.0f - (Math.abs(viewStart) / helper.getDecoratedMeasurement(view)) : 1.0f - (Math.abs(viewEnd) / helper.getDecoratedMeasurement(view));
        }
        int viewHeight = helper.getDecoratedMeasurement(view);
        return 1.0f - ((Math.abs(viewStart) + Math.abs(viewEnd)) / viewHeight);
    }

    @Override
    public void attachToRecyclerView(RecyclerView recyclerView) {
        super.attachToRecyclerView(recyclerView);
        this.mRecyclerView = recyclerView;
    }

    @Override
    protected RecyclerView.SmoothScroller createScroller(RecyclerView.LayoutManager layoutManager) {
        return new PagedSmoothScroller(this.mContext);
    }

    @Override
    public int[] calculateScrollDistance(int velocityX, int velocityY) {
        RecyclerView.LayoutManager layoutManager;
        int[] outDist = super.calculateScrollDistance(velocityX, velocityY);
        if (this.mRecyclerView == null || (layoutManager = this.mRecyclerView.getLayoutManager()) == null || layoutManager.getChildCount() == 0) {
            return outDist;
        }
        int lastChildPosition = isAtEnd(layoutManager) ? 0 : layoutManager.getChildCount() - 1;
        OrientationHelper orientationHelper = getOrientationHelper(layoutManager);
        View lastChild = layoutManager.getChildAt(lastChildPosition);
        float percentageVisible = getPercentageVisible(lastChild, orientationHelper);
        int maxDistance = layoutManager.getHeight();
        if (percentageVisible > 0.0f) {
            maxDistance -= layoutManager.getDecoratedMeasuredHeight(lastChild);
        }
        int minDistance = -maxDistance;
        outDist[0] = clamp(outDist[0], minDistance, maxDistance);
        outDist[1] = clamp(outDist[1], minDistance, maxDistance);
        return outDist;
    }

    public boolean isAtStart(RecyclerView.LayoutManager layoutManager) {
        OrientationHelper orientationHelper;
        if (layoutManager == null || layoutManager.getChildCount() == 0) {
            return true;
        }
        View firstChild = layoutManager.getChildAt(0);
        if (layoutManager.canScrollVertically()) {
            orientationHelper = getVerticalHelper(layoutManager);
        } else {
            orientationHelper = getHorizontalHelper(layoutManager);
        }
        if (orientationHelper.getDecoratedStart(firstChild) >= 0 && layoutManager.getPosition(firstChild) == 0) {
            return true;
        }
        return false;
    }

    public boolean isAtEnd(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager == null || layoutManager.getChildCount() == 0) {
            return true;
        }
        int childCount = layoutManager.getChildCount();
        View lastVisibleChild = layoutManager.getChildAt(childCount - 1);
        return layoutManager.getPosition(lastVisibleChild) == layoutManager.getItemCount() - 1 && layoutManager.getDecoratedBottom(lastVisibleChild) <= layoutManager.getHeight();
    }

    private OrientationHelper getOrientationHelper(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager.canScrollVertically()) {
            return getVerticalHelper(layoutManager);
        }
        return getHorizontalHelper(layoutManager);
    }

    private OrientationHelper getVerticalHelper(RecyclerView.LayoutManager layoutManager) {
        if (this.mVerticalHelper == null || this.mVerticalHelper.getLayoutManager() != layoutManager) {
            this.mVerticalHelper = OrientationHelper.createVerticalHelper(layoutManager);
        }
        return this.mVerticalHelper;
    }

    private OrientationHelper getHorizontalHelper(RecyclerView.LayoutManager layoutManager) {
        if (this.mHorizontalHelper == null || this.mHorizontalHelper.getLayoutManager() != layoutManager) {
            this.mHorizontalHelper = OrientationHelper.createHorizontalHelper(layoutManager);
        }
        return this.mHorizontalHelper;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
