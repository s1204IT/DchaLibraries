package com.mediatek.galleryraw;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.mediatek.gallerybasic.base.Layer;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.base.Player;
import com.mediatek.gallerybasic.gl.MGLView;
import com.mediatek.gallerybasic.util.Log;

public class RawLayer extends Layer {
    private static final String TAG = "MtkGallery2/RawLayer";
    private boolean mIsFilmMode;
    private ImageView mRawImageView;
    private ViewGroup mRawViewGroup;
    private Resources mResources;

    public RawLayer(Resources resources) {
        this.mResources = resources;
    }

    @Override
    public void onCreate(Activity activity, ViewGroup viewGroup) {
        this.mRawViewGroup = (ViewGroup) LayoutInflater.from(activity).inflate(this.mResources.getLayout(R.layout.raw), (ViewGroup) null);
        this.mRawViewGroup.setVisibility(4);
        this.mRawImageView = (ImageView) this.mRawViewGroup.findViewById(R.id.raw_indicator);
        this.mRawImageView.setContentDescription(this.mResources.getString(R.string.indicator_description));
        if (Build.VERSION.SDK_INT >= 21) {
            this.mRawImageView.setImageDrawable(this.mResources.getDrawable(R.drawable.raw_indicator, null));
        } else {
            this.mRawImageView.setImageDrawable(this.mResources.getDrawable(R.drawable.raw_indicator));
        }
    }

    @Override
    public void onResume(boolean z) {
        Log.d(TAG, "<onResume>");
        this.mIsFilmMode = z;
        updateIndicatorVisibility();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "<onPause>");
        this.mRawViewGroup.setVisibility(4);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "<onDestroy>");
    }

    @Override
    public void setData(MediaData mediaData) {
    }

    @Override
    public void setPlayer(Player player) {
    }

    @Override
    public View getView() {
        return this.mRawViewGroup;
    }

    @Override
    public MGLView getMGLView() {
        return null;
    }

    @Override
    public void onChange(Player player, int i, int i2, Object obj) {
    }

    @Override
    public void onFilmModeChange(boolean z) {
        this.mIsFilmMode = z;
        updateIndicatorVisibility();
    }

    private void updateIndicatorVisibility() {
        if (this.mIsFilmMode) {
            this.mRawViewGroup.setVisibility(4);
            Log.d(TAG, "<updateIndicatorVisibility> INVISIBLE");
        } else {
            this.mRawViewGroup.setVisibility(0);
            Log.d(TAG, "<updateIndicatorVisibility> VISIBLE");
        }
    }
}
