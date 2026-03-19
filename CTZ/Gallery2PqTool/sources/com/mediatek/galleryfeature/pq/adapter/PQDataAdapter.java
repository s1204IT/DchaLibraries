package com.mediatek.galleryfeature.pq.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import com.mediatek.gallerybasic.util.Log;
import com.mediatek.galleryfeature.pq.PictureQualityActivity;
import com.mediatek.galleryfeature.pq.R;
import com.mediatek.galleryfeature.pq.Representation;
import com.mediatek.galleryfeature.pq.dcfilter.DCFilter;
import com.mediatek.galleryfeature.pq.filter.Filter;
import com.mediatek.galleryfeature.pq.filter.FilterInterface;
import java.util.ArrayList;
import java.util.HashMap;

public class PQDataAdapter extends BaseAdapter {
    private static final String TAG = "MtkGallery2/PQDataAdapter";
    private PictureQualityActivity mActivity;
    private HashMap<ViewHolder, Representation> mAllPresentation = new HashMap<>();
    private Context mContext;
    private ArrayList<FilterInterface> mData;
    private FilterInterface mFilter;
    private LayoutInflater mInflater;
    private ListView mListView;
    private String mUri;

    public final class ViewHolder {
        public TextView blow;
        public RelativeLayout layout;
        public TextView left;
        public TextView right;
        public SeekBar seekbar;

        public ViewHolder() {
        }
    }

    public PQDataAdapter(PictureQualityActivity pictureQualityActivity, Context context, String str) {
        this.mActivity = pictureQualityActivity;
        this.mUri = str;
        this.mInflater = LayoutInflater.from(context);
        if (str != null) {
            this.mFilter = new Filter();
        } else {
            this.mFilter = new DCFilter();
        }
        this.mData = this.mFilter.getFilterList();
        this.mContext = context;
    }

    @Override
    public int getCount() {
        return this.mData.size();
    }

    public void setListView(ListView listView) {
        this.mListView = listView;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0L;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View viewInflate;
        ViewHolder viewHolder;
        if (view == null) {
            viewHolder = new ViewHolder();
            viewInflate = this.mInflater.inflate(R.layout.m_pq_seekbar, (ViewGroup) null);
            viewHolder.left = (TextView) viewInflate.findViewById(R.id.m_textViewMinValue);
            viewHolder.seekbar = (SeekBar) viewInflate.findViewById(R.id.m_seekbar);
            viewHolder.blow = (TextView) viewInflate.findViewById(R.id.m_textViewCurrentIndex);
            viewHolder.right = (TextView) viewInflate.findViewById(R.id.m_textViewMaxValue);
            viewHolder.layout = (RelativeLayout) viewInflate.findViewById(R.id.m_listitem);
            viewInflate.setTag(viewHolder);
        } else {
            viewInflate = view;
            viewHolder = (ViewHolder) view.getTag();
        }
        Representation representation = this.mAllPresentation.get(viewHolder);
        if (representation == null) {
            representation = new Representation(this.mUri);
            this.mAllPresentation.put(viewHolder, representation);
        }
        representation.init(viewHolder, this.mData.get(i));
        setItemHeight(viewHolder, this.mData.size());
        return viewInflate;
    }

    private void setItemHeight(ViewHolder viewHolder, int i) {
        int actionBarHeight;
        Log.d(TAG, "<setItemHeight> setItemHeight");
        int height = this.mListView.getHeight();
        if (height == 0) {
            height = ((Activity) this.mContext).getWindowManager().getDefaultDisplay().getHeight() - this.mActivity.getActionBarHeight();
        }
        int defaultItemHeight = this.mActivity.getDefaultItemHeight();
        if (i * defaultItemHeight < height) {
            actionBarHeight = (height - this.mActivity.getActionBarHeight()) / i;
        } else {
            actionBarHeight = defaultItemHeight - (this.mActivity.getActionBarHeight() / i);
        }
        viewHolder.layout.setMinimumHeight(actionBarHeight);
    }

    public void restoreIndex() {
        int size = this.mData.size();
        for (int i = 0; i < size; i++) {
            FilterInterface filterInterface = this.mData.get(i);
            if (filterInterface != null) {
                filterInterface.setIndex(filterInterface.getDefaultIndex());
            }
        }
    }

    public void onResume() {
        this.mFilter.onResume();
    }

    public void onDestroy() {
        this.mFilter.onDestroy();
    }
}
