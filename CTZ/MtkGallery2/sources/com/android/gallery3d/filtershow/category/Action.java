package com.android.gallery3d.filtershow.category;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.widget.ArrayAdapter;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import com.android.gallery3d.filtershow.pipeline.RenderingRequest;
import com.android.gallery3d.filtershow.pipeline.RenderingRequestCaller;

public class Action implements RenderingRequestCaller {
    private ArrayAdapter mAdapter;
    private boolean mCanBeRemoved;
    private FilterShowActivity mContext;
    public boolean mHasFinishAppliedFilterOperation;
    private Bitmap mImage;
    private Rect mImageFrame;
    public boolean mIsAddVersionOperation;
    private boolean mIsDoubleAction;
    private String mName;
    private Bitmap mOverlayBitmap;
    private FilterRepresentation mRepresentation;
    private int mTextSize;
    private int mType;

    public Action(FilterShowActivity filterShowActivity, FilterRepresentation filterRepresentation, int i, boolean z) {
        this(filterShowActivity, filterRepresentation, i);
        this.mCanBeRemoved = z;
        this.mTextSize = filterShowActivity.getResources().getDimensionPixelSize(R.dimen.category_panel_text_size);
    }

    public Action(FilterShowActivity filterShowActivity, FilterRepresentation filterRepresentation, int i) {
        this(filterShowActivity, i);
        setRepresentation(filterRepresentation);
    }

    public Action(FilterShowActivity filterShowActivity, int i) {
        this.mType = 1;
        this.mCanBeRemoved = false;
        this.mTextSize = 32;
        this.mIsDoubleAction = false;
        this.mHasFinishAppliedFilterOperation = false;
        this.mIsAddVersionOperation = false;
        this.mContext = filterShowActivity;
        setType(i);
        this.mContext.registerAction(this);
    }

    public Action(FilterShowActivity filterShowActivity, FilterRepresentation filterRepresentation) {
        this(filterShowActivity, filterRepresentation, 1);
    }

    public boolean isDoubleAction() {
        return this.mIsDoubleAction;
    }

    public void setIsDoubleAction(boolean z) {
        this.mIsDoubleAction = z;
    }

    public boolean canBeRemoved() {
        return this.mCanBeRemoved;
    }

    public int getType() {
        return this.mType;
    }

    public FilterRepresentation getRepresentation() {
        return this.mRepresentation;
    }

    public void setRepresentation(FilterRepresentation filterRepresentation) {
        this.mRepresentation = filterRepresentation;
        this.mName = filterRepresentation.getName();
    }

    public String getName() {
        return this.mName;
    }

    public void setName(String str) {
        this.mName = str;
    }

    public void setImageFrame(Rect rect, int i) {
        if ((this.mImageFrame != null && this.mImageFrame.equals(rect)) || getType() == 2) {
            return;
        }
        Bitmap temporaryThumbnailBitmap = MasterImage.getImage().getTemporaryThumbnailBitmap();
        if (temporaryThumbnailBitmap != null) {
            this.mImage = temporaryThumbnailBitmap;
        }
        if (MasterImage.getImage().getThumbnailBitmap() != null) {
            this.mImageFrame = rect;
            postNewIconRenderRequest(this.mImageFrame.width(), this.mImageFrame.height());
        }
    }

    public Bitmap getImage() {
        return this.mImage;
    }

    public void setAdapter(ArrayAdapter arrayAdapter) {
        this.mAdapter = arrayAdapter;
    }

    public void setType(int i) {
        this.mType = i;
    }

    private void postNewIconRenderRequest(int i, int i2) {
        if (this.mRepresentation != null) {
            ImagePreset imagePreset = new ImagePreset();
            imagePreset.addFilter(this.mRepresentation);
            RenderingRequest.postIconRequest(this.mContext, i, i2, imagePreset, this);
        }
    }

    private void drawCenteredImage(Bitmap bitmap, Bitmap bitmap2, boolean z) {
        int iMin = Math.min(bitmap2.getWidth(), bitmap2.getHeight());
        Matrix matrix = new Matrix();
        float fMin = iMin / Math.min(bitmap.getWidth(), bitmap.getHeight());
        float width = (bitmap2.getWidth() - (bitmap.getWidth() * fMin)) / 2.0f;
        float height = (bitmap2.getHeight() - (bitmap.getHeight() * fMin)) / 2.0f;
        if (this.mImageFrame.height() > this.mImageFrame.width()) {
            height -= this.mTextSize;
            if (height < 0.0f) {
                height = 0.0f;
            }
        }
        matrix.setScale(fMin, fMin);
        matrix.postTranslate(width, height);
        new Canvas(bitmap2).drawBitmap(bitmap, matrix, new Paint(2));
    }

    @Override
    public void available(RenderingRequest renderingRequest) {
        clearBitmap();
        this.mImage = renderingRequest.getBitmap();
        if (this.mIsAddVersionOperation && this.mImage != null) {
            this.mHasFinishAppliedFilterOperation = true;
        }
        if (this.mImage == null) {
            this.mImageFrame = null;
            return;
        }
        if (this.mRepresentation.getOverlayId() != 0 && this.mOverlayBitmap == null) {
            this.mOverlayBitmap = BitmapFactory.decodeResource(this.mContext.getResources(), this.mRepresentation.getOverlayId());
        }
        if (this.mOverlayBitmap != null) {
            if (getRepresentation().getFilterType() == 1) {
                new Canvas(this.mImage).drawBitmap(this.mOverlayBitmap, new Rect(0, 0, this.mOverlayBitmap.getWidth(), this.mOverlayBitmap.getHeight()), new Rect(0, 0, this.mImage.getWidth(), this.mImage.getHeight()), new Paint());
            } else {
                new Canvas(this.mImage).drawARGB(128, 0, 0, 0);
                drawCenteredImage(this.mOverlayBitmap, this.mImage, false);
            }
        }
        if (this.mAdapter != null) {
            this.mAdapter.notifyDataSetChanged();
        }
    }

    public void clearBitmap() {
        if (this.mImage != null && this.mImage != MasterImage.getImage().getTemporaryThumbnailBitmap()) {
            MasterImage.getImage().getBitmapCache().cache(this.mImage);
        }
        this.mImage = null;
    }
}
