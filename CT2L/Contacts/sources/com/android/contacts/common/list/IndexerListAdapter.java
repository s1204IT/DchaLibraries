package com.android.contacts.common.list;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SectionIndexer;

public abstract class IndexerListAdapter extends PinnedHeaderListAdapter implements SectionIndexer {
    protected Context mContext;
    private View mHeader;
    private int mIndexedPartition;
    private SectionIndexer mIndexer;
    private Placement mPlacementCache;
    private boolean mSectionHeaderDisplayEnabled;

    protected abstract View createPinnedSectionHeaderView(Context context, ViewGroup viewGroup);

    protected abstract void setPinnedSectionTitle(View view, String str);

    public static final class Placement {
        public boolean firstInSection;
        public boolean lastInSection;
        private int position = -1;
        public String sectionHeader;

        public void invalidate() {
            this.position = -1;
        }
    }

    public IndexerListAdapter(Context context) {
        super(context);
        this.mIndexedPartition = 0;
        this.mPlacementCache = new Placement();
        this.mContext = context;
    }

    public boolean isSectionHeaderDisplayEnabled() {
        return this.mSectionHeaderDisplayEnabled;
    }

    public void setSectionHeaderDisplayEnabled(boolean flag) {
        this.mSectionHeaderDisplayEnabled = flag;
    }

    public int getIndexedPartition() {
        return this.mIndexedPartition;
    }

    public void setIndexedPartition(int partition) {
        this.mIndexedPartition = partition;
    }

    public SectionIndexer getIndexer() {
        return this.mIndexer;
    }

    public void setIndexer(SectionIndexer indexer) {
        this.mIndexer = indexer;
        this.mPlacementCache.invalidate();
    }

    @Override
    public Object[] getSections() {
        return this.mIndexer == null ? new String[]{" "} : this.mIndexer.getSections();
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        if (this.mIndexer == null) {
            return -1;
        }
        return this.mIndexer.getPositionForSection(sectionIndex);
    }

    @Override
    public int getSectionForPosition(int position) {
        if (this.mIndexer == null) {
            return -1;
        }
        return this.mIndexer.getSectionForPosition(position);
    }

    @Override
    public int getPinnedHeaderCount() {
        return isSectionHeaderDisplayEnabled() ? super.getPinnedHeaderCount() + 1 : super.getPinnedHeaderCount();
    }

    @Override
    public View getPinnedHeaderView(int viewIndex, View convertView, ViewGroup parent) {
        if (!isSectionHeaderDisplayEnabled() || viewIndex != getPinnedHeaderCount() - 1) {
            return super.getPinnedHeaderView(viewIndex, convertView, parent);
        }
        if (this.mHeader == null) {
            this.mHeader = createPinnedSectionHeaderView(this.mContext, parent);
        }
        return this.mHeader;
    }

    @Override
    public void configurePinnedHeaders(PinnedHeaderListView listView) {
        int offset;
        super.configurePinnedHeaders(listView);
        if (isSectionHeaderDisplayEnabled()) {
            int index = getPinnedHeaderCount() - 1;
            if (this.mIndexer == null || getCount() == 0) {
                listView.setHeaderInvisible(index, false);
                return;
            }
            int listPosition = listView.getPositionAt(listView.getTotalTopPinnedHeaderHeight());
            int position = listPosition - listView.getHeaderViewsCount();
            int section = -1;
            int partition = getPartitionForPosition(position);
            if (partition == this.mIndexedPartition && (offset = getOffsetInPartition(position)) != -1) {
                section = getSectionForPosition(offset);
            }
            if (section == -1) {
                listView.setHeaderInvisible(index, false);
                return;
            }
            View topChild = listView.getChildAt(listPosition);
            if (topChild != null) {
                this.mHeader.setMinimumHeight(topChild.getMeasuredHeight());
            }
            setPinnedSectionTitle(this.mHeader, (String) this.mIndexer.getSections()[section]);
            int partitionStart = getPositionForPartition(this.mIndexedPartition);
            if (hasHeader(this.mIndexedPartition)) {
                partitionStart++;
            }
            int nextSectionPosition = partitionStart + getPositionForSection(section + 1);
            boolean isLastInSection = position == nextSectionPosition + (-1);
            listView.setFadingHeader(index, listPosition, isLastInSection);
        }
    }

    public Placement getItemPlacementInSection(int position) {
        if (this.mPlacementCache.position != position) {
            this.mPlacementCache.position = position;
            if (isSectionHeaderDisplayEnabled()) {
                int section = getSectionForPosition(position);
                if (section != -1 && getPositionForSection(section) == position) {
                    this.mPlacementCache.firstInSection = true;
                    this.mPlacementCache.sectionHeader = (String) getSections()[section];
                } else {
                    this.mPlacementCache.firstInSection = false;
                    this.mPlacementCache.sectionHeader = null;
                }
                this.mPlacementCache.lastInSection = getPositionForSection(section + 1) + (-1) == position;
            } else {
                this.mPlacementCache.firstInSection = false;
                this.mPlacementCache.lastInSection = false;
                this.mPlacementCache.sectionHeader = null;
            }
            return this.mPlacementCache;
        }
        return this.mPlacementCache;
    }
}
