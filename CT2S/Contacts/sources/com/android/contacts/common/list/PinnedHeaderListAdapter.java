package com.android.contacts.common.list;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import com.android.common.widget.CompositeCursorAdapter;
import com.android.contacts.common.list.PinnedHeaderListView;

public abstract class PinnedHeaderListAdapter extends CompositeCursorAdapter implements PinnedHeaderListView.PinnedHeaderAdapter {
    private boolean[] mHeaderVisibility;
    private boolean mPinnedPartitionHeadersEnabled;

    public PinnedHeaderListAdapter(Context context) {
        super(context);
    }

    public boolean getPinnedPartitionHeadersEnabled() {
        return this.mPinnedPartitionHeadersEnabled;
    }

    public void setPinnedPartitionHeadersEnabled(boolean flag) {
        this.mPinnedPartitionHeadersEnabled = flag;
    }

    public int getPinnedHeaderCount() {
        if (this.mPinnedPartitionHeadersEnabled) {
            return getPartitionCount();
        }
        return 0;
    }

    protected boolean isPinnedPartitionHeaderVisible(int partition) {
        return getPinnedPartitionHeadersEnabled() && hasHeader(partition) && !isPartitionEmpty(partition);
    }

    public View getPinnedHeaderView(int partition, View convertView, ViewGroup parent) {
        Integer headerType;
        if (!hasHeader(partition)) {
            return null;
        }
        View view = null;
        if (convertView != null && (headerType = (Integer) convertView.getTag()) != null && headerType.intValue() == 0) {
            view = convertView;
        }
        if (view == null) {
            view = newHeaderView(getContext(), partition, null, parent);
            view.setTag(0);
            view.setFocusable(false);
            view.setEnabled(false);
        }
        bindHeaderView(view, partition, getCursor(partition));
        view.setLayoutDirection(parent.getLayoutDirection());
        return view;
    }

    public void configurePinnedHeaders(PinnedHeaderListView listView) {
        int partition;
        if (getPinnedPartitionHeadersEnabled()) {
            int size = getPartitionCount();
            if (this.mHeaderVisibility == null || this.mHeaderVisibility.length != size) {
                this.mHeaderVisibility = new boolean[size];
            }
            for (int i = 0; i < size; i++) {
                boolean visible = isPinnedPartitionHeaderVisible(i);
                this.mHeaderVisibility[i] = visible;
                if (!visible) {
                    listView.setHeaderInvisible(i, true);
                }
            }
            int headerViewsCount = listView.getHeaderViewsCount();
            int maxTopHeader = -1;
            int topHeaderHeight = 0;
            for (int i2 = 0; i2 < size; i2++) {
                if (this.mHeaderVisibility[i2]) {
                    if (i2 > getPartitionForPosition(listView.getPositionAt(topHeaderHeight) - headerViewsCount)) {
                        break;
                    }
                    listView.setHeaderPinnedAtTop(i2, topHeaderHeight, false);
                    topHeaderHeight += listView.getPinnedHeaderHeight(i2);
                    maxTopHeader = i2;
                }
            }
            int maxBottomHeader = size;
            int bottomHeaderHeight = 0;
            int listHeight = listView.getHeight();
            int i3 = size;
            while (true) {
                i3--;
                if (i3 <= maxTopHeader) {
                    break;
                }
                if (this.mHeaderVisibility[i3]) {
                    int position = listView.getPositionAt(listHeight - bottomHeaderHeight) - headerViewsCount;
                    if (position < 0 || (partition = getPartitionForPosition(position - 1)) == -1 || i3 <= partition) {
                        break;
                    }
                    int height = listView.getPinnedHeaderHeight(i3);
                    bottomHeaderHeight += height;
                    listView.setHeaderPinnedAtBottom(i3, listHeight - bottomHeaderHeight, false);
                    maxBottomHeader = i3;
                }
            }
            for (int i4 = maxTopHeader + 1; i4 < maxBottomHeader; i4++) {
                if (this.mHeaderVisibility[i4]) {
                    listView.setHeaderInvisible(i4, isPartitionEmpty(i4));
                }
            }
        }
    }

    @Override
    public int getScrollPositionForHeader(int viewIndex) {
        return getPositionForPartition(viewIndex);
    }
}
