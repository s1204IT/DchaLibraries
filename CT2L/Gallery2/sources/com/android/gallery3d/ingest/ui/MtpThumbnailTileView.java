package com.android.gallery3d.ingest.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.mtp.MtpDevice;
import android.util.AttributeSet;
import android.widget.Checkable;
import com.android.gallery3d.R;
import com.android.gallery3d.ingest.data.IngestObjectInfo;
import com.android.gallery3d.ingest.data.MtpBitmapFetch;

public class MtpThumbnailTileView extends MtpImageView implements Checkable {
    private Bitmap mBitmap;
    private Paint mForegroundPaint;
    private boolean mIsChecked;

    private void init() {
        this.mForegroundPaint = new Paint();
        this.mForegroundPaint.setColor(getResources().getColor(R.color.ingest_highlight_semitransparent));
    }

    public MtpThumbnailTileView(Context context) {
        super(context);
        init();
    }

    public MtpThumbnailTileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MtpThumbnailTileView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    @Override
    protected Object fetchMtpImageDataFromDevice(MtpDevice device, IngestObjectInfo info) {
        return MtpBitmapFetch.getThumbnail(device, info);
    }

    @Override
    protected void onMtpImageDataFetchedFromDevice(Object result) {
        this.mBitmap = (Bitmap) result;
        setImageBitmap(this.mBitmap);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (isChecked()) {
            canvas.drawRect(canvas.getClipBounds(), this.mForegroundPaint);
        }
    }

    @Override
    public boolean isChecked() {
        return this.mIsChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        if (this.mIsChecked != checked) {
            this.mIsChecked = checked;
            invalidate();
        }
    }

    @Override
    public void toggle() {
        setChecked(!this.mIsChecked);
    }

    @Override
    protected void cancelLoadingAndClear() {
        super.cancelLoadingAndClear();
        if (this.mBitmap != null) {
            MtpBitmapFetch.recycleThumbnail(this.mBitmap);
            this.mBitmap = null;
        }
    }
}
