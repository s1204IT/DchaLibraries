package com.android.gallery3d.ingest.adapter;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.gallery3d.R;
import com.android.gallery3d.ingest.data.IngestObjectInfo;
import com.android.gallery3d.ingest.data.MtpDeviceIndex;
import com.android.gallery3d.ingest.ui.MtpFullscreenView;

@TargetApi(12)
public class MtpPagerAdapter extends PagerAdapter {
    private CheckBroker mBroker;
    private LayoutInflater mInflater;
    private MtpDeviceIndex mModel;
    private int mGeneration = 0;
    private MtpDeviceIndex.SortOrder mSortOrder = MtpDeviceIndex.SortOrder.DESCENDING;
    private MtpFullscreenView mReusableView = null;

    public MtpPagerAdapter(Context context, CheckBroker broker) {
        this.mInflater = LayoutInflater.from(context);
        this.mBroker = broker;
    }

    public void setMtpDeviceIndex(MtpDeviceIndex index) {
        this.mModel = index;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if (this.mModel != null) {
            return this.mModel.sizeWithoutLabels();
        }
        return 0;
    }

    @Override
    public void notifyDataSetChanged() {
        this.mGeneration++;
        super.notifyDataSetChanged();
    }

    public int translatePositionWithLabels(int position) {
        if (this.mModel == null) {
            return -1;
        }
        return this.mModel.getPositionWithoutLabelsFromPosition(position, this.mSortOrder);
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        this.mReusableView = null;
        super.finishUpdate(container);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        MtpFullscreenView v = (MtpFullscreenView) object;
        container.removeView(v);
        this.mBroker.unregisterOnCheckedChangeListener(v);
        this.mReusableView = v;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        MtpFullscreenView v;
        if (this.mReusableView != null) {
            v = this.mReusableView;
            this.mReusableView = null;
        } else {
            v = (MtpFullscreenView) this.mInflater.inflate(R.layout.ingest_fullsize, container, false);
        }
        IngestObjectInfo i = this.mModel.getWithoutLabels(position, this.mSortOrder);
        v.getImageView().setMtpDeviceAndObjectInfo(this.mModel.getDevice(), i, this.mGeneration);
        v.setPositionAndBroker(position, this.mBroker);
        container.addView(v);
        return v;
    }
}
