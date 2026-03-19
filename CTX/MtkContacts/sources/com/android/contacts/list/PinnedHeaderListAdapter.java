package com.android.contacts.list;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import com.android.common.widget.CompositeCursorAdapter;
import com.android.contacts.list.PinnedHeaderListView;

public abstract class PinnedHeaderListAdapter extends CompositeCursorAdapter implements PinnedHeaderListView.PinnedHeaderAdapter {
    private boolean[] mHeaderVisibility;
    private boolean mPinnedPartitionHeadersEnabled;

    public PinnedHeaderListAdapter(Context context) {
        super(context);
    }

    public boolean getPinnedPartitionHeadersEnabled() {
        return this.mPinnedPartitionHeadersEnabled;
    }

    public void setPinnedPartitionHeadersEnabled(boolean z) {
        this.mPinnedPartitionHeadersEnabled = z;
    }

    public int getPinnedHeaderCount() {
        if (this.mPinnedPartitionHeadersEnabled) {
            return getPartitionCount();
        }
        return 0;
    }

    protected boolean isPinnedPartitionHeaderVisible(int i) {
        return getPinnedPartitionHeadersEnabled() && hasHeader(i) && !isPartitionEmpty(i);
    }

    public View getPinnedHeaderView(int i, View view, ViewGroup viewGroup) {
        Integer num;
        if (!hasHeader(i)) {
            return null;
        }
        if (view == null || (num = (Integer) view.getTag()) == null || num.intValue() != 0) {
            view = null;
        }
        if (view == null) {
            view = newHeaderView(getContext(), i, null, viewGroup);
            view.setTag(0);
            view.setFocusable(false);
            view.setEnabled(false);
        }
        bindHeaderView(view, i, getCursor(i));
        view.setLayoutDirection(viewGroup.getLayoutDirection());
        return view;
    }

    public void configurePinnedHeaders(PinnedHeaderListView pinnedHeaderListView) {
        int partitionForPosition;
        if (!getPinnedPartitionHeadersEnabled()) {
            return;
        }
        int partitionCount = getPartitionCount();
        if (this.mHeaderVisibility == null || this.mHeaderVisibility.length != partitionCount) {
            this.mHeaderVisibility = new boolean[partitionCount];
        }
        for (int i = 0; i < partitionCount; i++) {
            boolean zIsPinnedPartitionHeaderVisible = isPinnedPartitionHeaderVisible(i);
            this.mHeaderVisibility[i] = zIsPinnedPartitionHeaderVisible;
            if (!zIsPinnedPartitionHeaderVisible) {
                pinnedHeaderListView.setHeaderInvisible(i, true);
            }
        }
        int headerViewsCount = pinnedHeaderListView.getHeaderViewsCount();
        int pinnedHeaderHeight = 0;
        int i2 = -1;
        for (int i3 = 0; i3 < partitionCount; i3++) {
            if (this.mHeaderVisibility[i3]) {
                if (i3 > getPartitionForPosition(pinnedHeaderListView.getPositionAt(pinnedHeaderHeight) - headerViewsCount)) {
                    break;
                }
                pinnedHeaderListView.setHeaderPinnedAtTop(i3, pinnedHeaderHeight, false);
                pinnedHeaderHeight += pinnedHeaderListView.getPinnedHeaderHeight(i3);
                i2 = i3;
            }
        }
        int height = pinnedHeaderListView.getHeight();
        int i4 = partitionCount;
        int pinnedHeaderHeight2 = 0;
        while (true) {
            partitionCount--;
            if (partitionCount <= i2) {
                break;
            }
            if (this.mHeaderVisibility[partitionCount]) {
                int positionAt = pinnedHeaderListView.getPositionAt(height - pinnedHeaderHeight2) - headerViewsCount;
                if (positionAt < 0 || (partitionForPosition = getPartitionForPosition(positionAt - 1)) == -1 || partitionCount <= partitionForPosition) {
                    break;
                }
                pinnedHeaderHeight2 += pinnedHeaderListView.getPinnedHeaderHeight(partitionCount);
                pinnedHeaderListView.setHeaderPinnedAtBottom(partitionCount, height - pinnedHeaderHeight2, false);
                i4 = partitionCount;
            }
        }
        for (int i5 = i2 + 1; i5 < i4; i5++) {
            if (this.mHeaderVisibility[i5]) {
                pinnedHeaderListView.setHeaderInvisible(i5, isPartitionEmpty(i5));
            }
        }
    }

    @Override
    public int getScrollPositionForHeader(int i) {
        return getPositionForPartition(i);
    }
}
