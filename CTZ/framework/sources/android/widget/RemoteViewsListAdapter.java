package android.widget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;

public class RemoteViewsListAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<RemoteViews> mRemoteViewsList;
    private int mViewTypeCount;
    private ArrayList<Integer> mViewTypes = new ArrayList<>();

    public RemoteViewsListAdapter(Context context, ArrayList<RemoteViews> arrayList, int i) {
        this.mContext = context;
        this.mRemoteViewsList = arrayList;
        this.mViewTypeCount = i;
        init();
    }

    public void setViewsList(ArrayList<RemoteViews> arrayList) {
        this.mRemoteViewsList = arrayList;
        init();
        notifyDataSetChanged();
    }

    private void init() {
        if (this.mRemoteViewsList == null) {
            return;
        }
        this.mViewTypes.clear();
        for (RemoteViews remoteViews : this.mRemoteViewsList) {
            if (!this.mViewTypes.contains(Integer.valueOf(remoteViews.getLayoutId()))) {
                this.mViewTypes.add(Integer.valueOf(remoteViews.getLayoutId()));
            }
        }
        if (this.mViewTypes.size() > this.mViewTypeCount || this.mViewTypeCount < 1) {
            throw new RuntimeException("Invalid view type count -- view type count must be >= 1and must be as large as the total number of distinct view types");
        }
    }

    @Override
    public int getCount() {
        if (this.mRemoteViewsList != null) {
            return this.mRemoteViewsList.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (i < getCount()) {
            RemoteViews remoteViews = this.mRemoteViewsList.get(i);
            remoteViews.setIsWidgetCollectionChild(true);
            if (view != null && remoteViews != null && view.getId() == remoteViews.getLayoutId()) {
                remoteViews.reapply(this.mContext, view);
                return view;
            }
            return remoteViews.apply(this.mContext, viewGroup);
        }
        return null;
    }

    @Override
    public int getItemViewType(int i) {
        if (i < getCount()) {
            return this.mViewTypes.indexOf(Integer.valueOf(this.mRemoteViewsList.get(i).getLayoutId()));
        }
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return this.mViewTypeCount;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }
}
