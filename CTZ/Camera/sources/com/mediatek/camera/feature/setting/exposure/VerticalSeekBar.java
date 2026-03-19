package com.mediatek.camera.feature.setting.exposure;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;

public class VerticalSeekBar extends AppCompatSeekBar {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(VerticalSeekBar.class.getSimpleName());
    private Context mContext;
    private SeekBar.OnSeekBarChangeListener mListener;

    public VerticalSeekBar(Context context) {
        super(context);
        this.mContext = context;
    }

    public VerticalSeekBar(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mContext = context;
    }

    public VerticalSeekBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mContext = context;
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i2, i, i4, i3);
    }

    @Override
    public synchronized void setProgress(int i) {
        super.setProgress(i);
        onSizeChanged(getWidth(), getHeight(), 0, 0);
    }

    @Override
    protected synchronized void onMeasure(int i, int i2) {
        super.onMeasure(i2, i);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    @Override
    public void setThumb(Drawable drawable) {
        if (drawable == 0 || this.mContext == null || !(drawable instanceof VectorDrawable)) {
            return;
        }
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        int i = (int) ((4.25d * ((double) this.mContext.getResources().getDisplayMetrics().density)) + 0.5d);
        drawable.setBounds(i, i, canvas.getWidth() - i, canvas.getHeight() - i);
        drawable.draw(canvas);
        super.setThumb(new BitmapDrawable(this.mContext.getResources(), bitmapCreateBitmap));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.rotate(-90.0f);
        canvas.translate(-getHeight(), 0.0f);
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!isEnabled()) {
            return false;
        }
        getProgressDrawable().setAlpha(255);
        setProgressDrawable(new ColorDrawable(-1));
        int max = getMax() - ((int) ((getMax() * motionEvent.getY()) / getHeight()));
        switch (motionEvent.getAction()) {
            case 0:
                if (this.mListener != null) {
                    this.mListener.onStartTrackingTouch(this);
                }
                return true;
            case Camera2Proxy.TEMPLATE_PREVIEW:
                setProgress(max);
                if (this.mListener != null) {
                    this.mListener.onStopTrackingTouch(this);
                }
                return true;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                setProgress(max);
                if (this.mListener != null) {
                    this.mListener.onProgressChanged(this, max, true);
                }
                return true;
            case Camera2Proxy.TEMPLATE_RECORD:
            default:
                return true;
        }
    }

    @Override
    public void setOnSeekBarChangeListener(SeekBar.OnSeekBarChangeListener onSeekBarChangeListener) {
        super.setOnSeekBarChangeListener(onSeekBarChangeListener);
        this.mListener = onSeekBarChangeListener;
    }
}
