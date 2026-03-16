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

    public MtpAdapter(Activity context) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
    }

    public void setMtpDeviceIndex(MtpDeviceIndex index) {
        this.mModel = index;
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
    public Object getItem(int position) {
        return this.mModel.get(position, this.mSortOrder);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return position == getPositionForSection(getSectionForPosition(position)) ? 1 : 0;
    }

    public boolean itemAtPositionIsBucket(int position) {
        return getItemViewType(position) == 1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DateTileView dateTile;
        MtpThumbnailTileView imageView;
        int type = getItemViewType(position);
        if (type == 0) {
            if (convertView == null) {
                imageView = (MtpThumbnailTileView) this.mInflater.inflate(R.layout.ingest_thumbnail, parent, false);
            } else {
                imageView = (MtpThumbnailTileView) convertView;
            }
            imageView.setMtpDeviceAndObjectInfo(this.mModel.getDevice(), (IngestObjectInfo) getItem(position), this.mGeneration);
            return imageView;
        }
        if (convertView == null) {
            dateTile = (DateTileView) this.mInflater.inflate(R.layout.ingest_date_tile, parent, false);
        } else {
            dateTile = (DateTileView) convertView;
        }
        dateTile.setDate((SimpleDate) getItem(position));
        return dateTile;
    }

    @Override
    public int getPositionForSection(int section) {
        if (getCount() == 0) {
            return 0;
        }
        int numSections = getSections().length;
        if (section >= numSections) {
            section = numSections - 1;
        }
        return this.mModel.getFirstPositionForBucketNumber(section, this.mSortOrder);
    }

    @Override
    public int getSectionForPosition(int position) {
        int count = getCount();
        if (count == 0) {
            return 0;
        }
        if (position >= count) {
            position = count - 1;
        }
        return this.mModel.getBucketNumberForPosition(position, this.mSortOrder);
    }

    @Override
    public Object[] getSections() {
        if (getCount() > 0) {
            return this.mModel.getBuckets(this.mSortOrder);
        }
        return null;
    }

    public int translatePositionWithoutLabels(int position) {
        if (this.mModel == null) {
            return -1;
        }
        return this.mModel.getPositionFromPositionWithoutLabels(position, this.mSortOrder);
    }
}
