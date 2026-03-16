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
    private Bitmap mImage;
    private Rect mImageFrame;
    private boolean mIsDoubleAction;
    private String mName;
    private Bitmap mOverlayBitmap;
    private FilterRepresentation mRepresentation;
    private int mTextSize;
    private int mType;

    public Action(FilterShowActivity context, FilterRepresentation representation, int type, boolean canBeRemoved) {
        this(context, representation, type);
        this.mCanBeRemoved = canBeRemoved;
        this.mTextSize = context.getResources().getDimensionPixelSize(R.dimen.category_panel_text_size);
    }

    public Action(FilterShowActivity context, FilterRepresentation representation, int type) {
        this(context, type);
        setRepresentation(representation);
    }

    public Action(FilterShowActivity context, int type) {
        this.mType = 1;
        this.mCanBeRemoved = false;
        this.mTextSize = 32;
        this.mIsDoubleAction = false;
        this.mContext = context;
        setType(type);
        this.mContext.registerAction(this);
    }

    public Action(FilterShowActivity context, FilterRepresentation representation) {
        this(context, representation, 1);
    }

    public boolean isDoubleAction() {
        return this.mIsDoubleAction;
    }

    public void setIsDoubleAction(boolean value) {
        this.mIsDoubleAction = value;
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

    public void setRepresentation(FilterRepresentation representation) {
        this.mRepresentation = representation;
        this.mName = representation.getName();
    }

    public String getName() {
        return this.mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public void setImageFrame(Rect imageFrame, int orientation) {
        if ((this.mImageFrame == null || !this.mImageFrame.equals(imageFrame)) && getType() != 2) {
            Bitmap temp = MasterImage.getImage().getTemporaryThumbnailBitmap();
            if (temp != null) {
                this.mImage = temp;
            }
            Bitmap bitmap = MasterImage.getImage().getThumbnailBitmap();
            if (bitmap != null) {
                this.mImageFrame = imageFrame;
                int w = this.mImageFrame.width();
                int h = this.mImageFrame.height();
                postNewIconRenderRequest(w, h);
            }
        }
    }

    public Bitmap getImage() {
        return this.mImage;
    }

    public void setAdapter(ArrayAdapter adapter) {
        this.mAdapter = adapter;
    }

    public void setType(int type) {
        this.mType = type;
    }

    private void postNewIconRenderRequest(int w, int h) {
        if (this.mRepresentation != null) {
            ImagePreset preset = new ImagePreset();
            preset.addFilter(this.mRepresentation);
            RenderingRequest.postIconRequest(this.mContext, w, h, preset, this);
        }
    }

    private void drawCenteredImage(Bitmap source, Bitmap destination, boolean scale) {
        int minSide = Math.min(destination.getWidth(), destination.getHeight());
        Matrix m = new Matrix();
        float scaleFactor = minSide / Math.min(source.getWidth(), source.getHeight());
        float dx = (destination.getWidth() - (source.getWidth() * scaleFactor)) / 2.0f;
        float dy = (destination.getHeight() - (source.getHeight() * scaleFactor)) / 2.0f;
        if (this.mImageFrame.height() > this.mImageFrame.width()) {
            dy -= this.mTextSize;
        }
        m.setScale(scaleFactor, scaleFactor);
        m.postTranslate(dx, dy);
        Canvas canvas = new Canvas(destination);
        canvas.drawBitmap(source, m, new Paint(2));
    }

    @Override
    public void available(RenderingRequest request) {
        clearBitmap();
        this.mImage = request.getBitmap();
        if (this.mImage == null) {
            this.mImageFrame = null;
            return;
        }
        if (this.mRepresentation.getOverlayId() != 0 && this.mOverlayBitmap == null) {
            this.mOverlayBitmap = BitmapFactory.decodeResource(this.mContext.getResources(), this.mRepresentation.getOverlayId());
        }
        if (this.mOverlayBitmap != null) {
            if (getRepresentation().getFilterType() == 1) {
                Canvas canvas = new Canvas(this.mImage);
                canvas.drawBitmap(this.mOverlayBitmap, new Rect(0, 0, this.mOverlayBitmap.getWidth(), this.mOverlayBitmap.getHeight()), new Rect(0, 0, this.mImage.getWidth(), this.mImage.getHeight()), new Paint());
            } else {
                Canvas canvas2 = new Canvas(this.mImage);
                canvas2.drawARGB(128, 0, 0, 0);
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
