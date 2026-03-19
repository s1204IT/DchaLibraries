package com.android.gallery3d.ingest.adapter;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;
import com.android.gallery3d.R;
import com.android.gallery3d.ingest.data.IngestObjectInfo;
import com.android.gallery3d.ingest.data.MtpDeviceIndex;
import com.android.gallery3d.ingest.data.SimpleDate;
import com.android.gallery3d.ingest.ui.DateTileView;
import com.android.gallery3d.ingest.ui.MtpThumbnailTileView;

@TargetApi(12)
public class MtpAdapter extends BaseAdapter implements SectionIndexer {
    private Context mContext;
    private LayoutInflater mInflater;
    private MtpDeviceIndex mModel;
    private MtpDeviceIndex.SortOrder mSortOrder = MtpDeviceIndex.SortOrder.DESCENDING;
    private int mGeneration = 0;

    public MtpAdapter(Activity activity) {
        this.mContext = activity;
        this.mInflater = LayoutInflater.from(activity);
    }

    public void setMtpDeviceIndex(MtpDeviceIndex mtpDeviceIndex) {
        this.mModel = mtpDeviceIndex;
        notifyDataSetChanged();
    }

    public MtpDeviceIndex getMtpDeviceIndex() {
        return this.mModel;
    }

    @Override
    public void notifyDataSetChanged() {
        this.mGeneration++;
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        this.mGeneration++;
        super.notifyDataSetInvalidated();
    }

    public boolean deviceConnected() {
        return this.mModel != null && this.mModel.isDeviceConnected();
    }

    public boolean indexReady() {
        return this.mModel != null && this.mModel.isIndexReady();
    }

    @Override
    public int getCount() {
        if (this.mModel != null) {
            return this.mModel.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int i) {
        return this.mModel.get(i, this.mSortOrder);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int i) {
        return true;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int i) {
        if (i == getPositionForSection(getSectionForPosition(i))) {
            return 1;
        }
        return 0;
    }

    public boolean itemAtPositionIsBucket(int i) {
        return getItemViewType(i) == 1;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        DateTileView dateTileView;
        MtpThumbnailTileView mtpThumbnailTileView;
        if (getItemViewType(i) == 0) {
            if (view == null) {
                mtpThumbnailTileView = (MtpThumbnailTileView) this.mInflater.inflate(R.layout.ingest_thumbnail, viewGroup, false);
            } else {
                mtpThumbnailTileView = (MtpThumbnailTileView) view;
            }
            mtpThumbnailTileView.setMtpDeviceAndObjectInfo(this.mModel.getDevice(), (IngestObjectInfo) getItem(i), this.mGeneration);
            return mtpThumbnailTileView;
        }
        if (view == null) {
            dateTileView = (DateTileView) this.mInflater.inflate(R.layout.ingest_date_tile, viewGroup, false);
        } else {
            dateTileView = (DateTileView) view;
        }
        dateTileView.setDate((SimpleDate) getItem(i));
        return dateTileView;
    }

    @Override
    public int getPositionForSection(int i) {
        if (getCount() == 0) {
            return 0;
        }
        int length = getSections().length;
        if (i >= length) {
            i = length - 1;
        }
        return this.mModel.getFirstPositionForBucketNumber(i, this.mSortOrder);
    }

    @Override
    public int getSectionForPosition(int i) {
        int count = getCount();
        if (count == 0) {
            return 0;
        }
        if (i >= count) {
            i = count - 1;
        }
        return this.mModel.getBucketNumberForPosition(i, this.mSortOrder);
    }

    @Override
    public Object[] getSections() {
        if (getCount() > 0) {
            return this.mModel.getBuckets(this.mSortOrder);
        }
        return null;
    }

    public int translatePositionWithoutLabels(int i) {
        if (this.mModel == null) {
            return -1;
        }
        return this.mModel.getPositionFromPositionWithoutLabels(i, this.mSortOrder);
    }
}
