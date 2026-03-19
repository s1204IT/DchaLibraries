package android.widget;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.Iterator;

public class HeaderViewListAdapter implements WrapperListAdapter, Filterable {
    static final ArrayList<ListView.FixedViewInfo> EMPTY_INFO_LIST = new ArrayList<>();
    private final ListAdapter mAdapter;
    boolean mAreAllFixedViewsSelectable;
    ArrayList<ListView.FixedViewInfo> mFooterViewInfos;
    ArrayList<ListView.FixedViewInfo> mHeaderViewInfos;
    private final boolean mIsFilterable;

    public HeaderViewListAdapter(ArrayList<ListView.FixedViewInfo> arrayList, ArrayList<ListView.FixedViewInfo> arrayList2, ListAdapter listAdapter) {
        this.mAdapter = listAdapter;
        this.mIsFilterable = listAdapter instanceof Filterable;
        if (arrayList == null) {
            this.mHeaderViewInfos = EMPTY_INFO_LIST;
        } else {
            this.mHeaderViewInfos = arrayList;
        }
        if (arrayList2 == null) {
            this.mFooterViewInfos = EMPTY_INFO_LIST;
        } else {
            this.mFooterViewInfos = arrayList2;
        }
        this.mAreAllFixedViewsSelectable = areAllListInfosSelectable(this.mHeaderViewInfos) && areAllListInfosSelectable(this.mFooterViewInfos);
    }

    public int getHeadersCount() {
        return this.mHeaderViewInfos.size();
    }

    public int getFootersCount() {
        return this.mFooterViewInfos.size();
    }

    @Override
    public boolean isEmpty() {
        return this.mAdapter == null || this.mAdapter.isEmpty();
    }

    private boolean areAllListInfosSelectable(ArrayList<ListView.FixedViewInfo> arrayList) {
        if (arrayList != null) {
            Iterator<ListView.FixedViewInfo> it = arrayList.iterator();
            while (it.hasNext()) {
                if (!it.next().isSelectable) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    public boolean removeHeader(View view) {
        boolean z = false;
        for (int i = 0; i < this.mHeaderViewInfos.size(); i++) {
            if (this.mHeaderViewInfos.get(i).view == view) {
                this.mHeaderViewInfos.remove(i);
                if (areAllListInfosSelectable(this.mHeaderViewInfos) && areAllListInfosSelectable(this.mFooterViewInfos)) {
                    z = true;
                }
                this.mAreAllFixedViewsSelectable = z;
                return true;
            }
        }
        return false;
    }

    public boolean removeFooter(View view) {
        boolean z = false;
        for (int i = 0; i < this.mFooterViewInfos.size(); i++) {
            if (this.mFooterViewInfos.get(i).view == view) {
                this.mFooterViewInfos.remove(i);
                if (areAllListInfosSelectable(this.mHeaderViewInfos) && areAllListInfosSelectable(this.mFooterViewInfos)) {
                    z = true;
                }
                this.mAreAllFixedViewsSelectable = z;
                return true;
            }
        }
        return false;
    }

    @Override
    public int getCount() {
        if (this.mAdapter != null) {
            return getFootersCount() + getHeadersCount() + this.mAdapter.getCount();
        }
        return getFootersCount() + getHeadersCount();
    }

    @Override
    public boolean areAllItemsEnabled() {
        if (this.mAdapter != null) {
            return this.mAreAllFixedViewsSelectable && this.mAdapter.areAllItemsEnabled();
        }
        return true;
    }

    @Override
    public boolean isEnabled(int i) {
        int headersCount = getHeadersCount();
        if (i < headersCount) {
            return this.mHeaderViewInfos.get(i).isSelectable;
        }
        int i2 = i - headersCount;
        int count = 0;
        if (this.mAdapter != null && i2 < (count = this.mAdapter.getCount())) {
            return this.mAdapter.isEnabled(i2);
        }
        return this.mFooterViewInfos.get(i2 - count).isSelectable;
    }

    @Override
    public Object getItem(int i) {
        int headersCount = getHeadersCount();
        if (i < headersCount) {
            return this.mHeaderViewInfos.get(i).data;
        }
        int i2 = i - headersCount;
        int count = 0;
        if (this.mAdapter != null && i2 < (count = this.mAdapter.getCount())) {
            return this.mAdapter.getItem(i2);
        }
        return this.mFooterViewInfos.get(i2 - count).data;
    }

    @Override
    public long getItemId(int i) {
        int i2;
        int headersCount = getHeadersCount();
        if (this.mAdapter != null && i >= headersCount && (i2 = i - headersCount) < this.mAdapter.getCount()) {
            return this.mAdapter.getItemId(i2);
        }
        return -1L;
    }

    @Override
    public boolean hasStableIds() {
        if (this.mAdapter != null) {
            return this.mAdapter.hasStableIds();
        }
        return false;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        int headersCount = getHeadersCount();
        if (i < headersCount) {
            return this.mHeaderViewInfos.get(i).view;
        }
        int i2 = i - headersCount;
        int count = 0;
        if (this.mAdapter != null && i2 < (count = this.mAdapter.getCount())) {
            return this.mAdapter.getView(i2, view, viewGroup);
        }
        return this.mFooterViewInfos.get(i2 - count).view;
    }

    @Override
    public int getItemViewType(int i) {
        int i2;
        int headersCount = getHeadersCount();
        if (this.mAdapter != null && i >= headersCount && (i2 = i - headersCount) < this.mAdapter.getCount()) {
            return this.mAdapter.getItemViewType(i2);
        }
        return -2;
    }

    @Override
    public int getViewTypeCount() {
        if (this.mAdapter != null) {
            return this.mAdapter.getViewTypeCount();
        }
        return 1;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver dataSetObserver) {
        if (this.mAdapter != null) {
            this.mAdapter.registerDataSetObserver(dataSetObserver);
        }
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {
        if (this.mAdapter != null) {
            this.mAdapter.unregisterDataSetObserver(dataSetObserver);
        }
    }

    @Override
    public Filter getFilter() {
        if (this.mIsFilterable) {
            return ((Filterable) this.mAdapter).getFilter();
        }
        return null;
    }

    @Override
    public ListAdapter getWrappedAdapter() {
        return this.mAdapter;
    }
}
