package com.mediatek.galleryfeature.pq;

import android.widget.SeekBar;
import android.widget.TextView;
import com.mediatek.gallerybasic.util.Log;
import com.mediatek.galleryfeature.pq.adapter.PQDataAdapter;
import com.mediatek.galleryfeature.pq.filter.FilterInterface;

public class Representation implements SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "MtkGallery2/Representation";
    SeekBar mController;
    TextView mCurrentValue;
    private FilterInterface mFilter;
    private PQDataAdapter.ViewHolder mHolder;
    TextView mMaxValue;
    TextView mMinValue;
    private String mUri;

    public Representation(String str) {
        this.mUri = str;
    }

    public void init(PQDataAdapter.ViewHolder viewHolder, FilterInterface filterInterface) {
        this.mHolder = viewHolder;
        this.mFilter = filterInterface;
        viewHolder.left.setText(this.mFilter.getMinValue());
        viewHolder.right.setText(this.mFilter.getMaxValue());
        viewHolder.blow.setText(this.mFilter.getCurrentValue());
        viewHolder.seekbar.setMax(this.mFilter.getRange() - 1);
        viewHolder.seekbar.setProgress(Integer.parseInt(this.mFilter.getSeekbarProgressValue()));
        Log.d(TAG, "<init>: mFilter.getCurrentValue() = " + this.mFilter.getCurrentValue() + " mFilter.getSeekbarProgressValue() = " + Integer.parseInt(this.mFilter.getSeekbarProgressValue()) + "  holder.seekbar Max=" + viewHolder.seekbar.getMax());
        viewHolder.seekbar.setOnSeekBarChangeListener(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
        if (z) {
            this.mFilter.setCurrentIndex(i);
            this.mHolder.blow.setText(this.mFilter.getCurrentValue());
            if (this.mUri != null) {
                PresentImage.getPresentImage().loadBitmap(this.mUri);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        this.mFilter.setIndex(this.mFilter.getCurrentIndex());
        this.mHolder.blow.setText(this.mFilter.getCurrentValue());
    }
}
