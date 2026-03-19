package com.android.gallery3d.ingest.adapter;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.gallery3d.R;
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

    public MtpPagerAdapter(Context context, CheckBroker checkBroker) {
        this.mInflater = LayoutInflater.from(context);
        this.mBroker = checkBroker;
    }

    public void setMtpDeviceIndex(MtpDeviceIndex mtpDeviceIndex) {
        this.mModel = mtpDeviceIndex;
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

    public int translatePositionWithLabels(int i) {
        if (this.mModel == null) {
            return -1;
        }
        return this.mModel.getPositionWithoutLabelsFromPosition(i, this.mSortOrder);
    }

    @Override
    public void finishUpdate(ViewGroup viewGroup) {
        this.mReusableView = null;
        super.finishUpdate(viewGroup);
    }

    @Override
    public boolean isViewFromObject(View view, Object obj) {
        return view == obj;
    }

    @Override
    public void destroyItem(ViewGroup viewGroup, int i, Object obj) {
        MtpFullscreenView mtpFullscreenView = (MtpFullscreenView) obj;
        viewGroup.removeView(mtpFullscreenView);
        this.mBroker.unregisterOnCheckedChangeListener(mtpFullscreenView);
        this.mReusableView = mtpFullscreenView;
    }

    @Override
    public Object instantiateItem(ViewGroup viewGroup, int i) {
        MtpFullscreenView mtpFullscreenView;
        if (this.mReusableView != null) {
            mtpFullscreenView = this.mReusableView;
            this.mReusableView = null;
        } else {
            mtpFullscreenView = (MtpFullscreenView) this.mInflater.inflate(R.layout.ingest_fullsize, viewGroup, false);
        }
        mtpFullscreenView.getImageView().setMtpDeviceAndObjectInfo(this.mModel.getDevice(), this.mModel.getWithoutLabels(i, this.mSortOrder), this.mGeneration);
        mtpFullscreenView.setPositionAndBroker(i, this.mBroker);
        viewGroup.addView(mtpFullscreenView);
        return mtpFullscreenView;
    }
}
