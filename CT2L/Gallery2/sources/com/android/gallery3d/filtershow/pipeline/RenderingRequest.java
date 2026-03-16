package com.android.gallery3d.filtershow.pipeline;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import com.android.gallery3d.app.Log;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.imageshow.MasterImage;

public class RenderingRequest {
    private static final Bitmap.Config mConfig = Bitmap.Config.ARGB_8888;
    private boolean mIsDirect = false;
    private Bitmap mBitmap = null;
    private ImagePreset mImagePreset = null;
    private ImagePreset mOriginalImagePreset = null;
    private RenderingRequestCaller mCaller = null;
    private float mScaleFactor = 1.0f;
    private Rect mBounds = null;
    private Rect mDestination = null;
    private Rect mIconBounds = null;
    private int mType = 0;

    public static void post(Context context, Bitmap source, ImagePreset preset, int type, RenderingRequestCaller caller) {
        post(context, source, preset, type, caller, null, null);
    }

    public static void post(Context context, Bitmap source, ImagePreset preset, int type, RenderingRequestCaller caller, Rect bounds, Rect destination) {
        if ((type != 4 && type != 5 && type != 2 && type != 1 && source == null) || preset == null || caller == null) {
            Log.v("RenderingRequest", "something null: source: " + source + " or preset: " + preset + " or caller: " + caller);
            return;
        }
        RenderingRequest request = new RenderingRequest();
        Bitmap bitmap = null;
        if (type == 0 || type == 3 || type == 6) {
            CachingPipeline pipeline = new CachingPipeline(FiltersManager.getManager(), "Icon");
            bitmap = pipeline.renderGeometryIcon(source, preset);
        } else if (type != 4 && type != 5 && type != 2 && type != 1) {
            bitmap = MasterImage.getImage().getBitmapCache().getBitmap(source.getWidth(), source.getHeight(), 8);
        }
        request.setBitmap(bitmap);
        ImagePreset passedPreset = new ImagePreset(preset);
        request.setOriginalImagePreset(preset);
        request.setScaleFactor(MasterImage.getImage().getScaleFactor());
        if (type == 4) {
            request.setBounds(bounds);
            request.setDestination(destination);
            passedPreset.setPartialRendering(true, bounds);
        }
        request.setImagePreset(passedPreset);
        request.setType(type);
        request.setCaller(caller);
        request.post(context);
    }

    public static void postIconRequest(Context context, int w, int h, ImagePreset preset, RenderingRequestCaller caller) {
        if (preset == null || caller == null) {
            Log.v("RenderingRequest", "something null, preset: " + preset + " or caller: " + caller);
            return;
        }
        RenderingRequest request = new RenderingRequest();
        ImagePreset passedPreset = new ImagePreset(preset);
        request.setOriginalImagePreset(preset);
        request.setScaleFactor(MasterImage.getImage().getScaleFactor());
        request.setImagePreset(passedPreset);
        request.setType(3);
        request.setCaller(caller);
        request.setIconBounds(new Rect(0, 0, w, h));
        request.post(context);
    }

    public void post(Context context) {
        if (context instanceof FilterShowActivity) {
            FilterShowActivity activity = (FilterShowActivity) context;
            ProcessingService service = activity.getProcessingService();
            service.postRenderingRequest(this);
        }
    }

    public void markAvailable() {
        if (this.mBitmap != null && this.mImagePreset != null && this.mCaller != null) {
            this.mCaller.available(this);
        }
    }

    public Bitmap getBitmap() {
        return this.mBitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
    }

    public ImagePreset getImagePreset() {
        return this.mImagePreset;
    }

    public void setImagePreset(ImagePreset imagePreset) {
        this.mImagePreset = imagePreset;
    }

    public int getType() {
        return this.mType;
    }

    public void setType(int type) {
        this.mType = type;
    }

    public void setCaller(RenderingRequestCaller caller) {
        this.mCaller = caller;
    }

    public Rect getBounds() {
        return this.mBounds;
    }

    public void setBounds(Rect bounds) {
        this.mBounds = bounds;
    }

    public void setScaleFactor(float scaleFactor) {
        this.mScaleFactor = scaleFactor;
    }

    public float getScaleFactor() {
        return this.mScaleFactor;
    }

    public Rect getDestination() {
        return this.mDestination;
    }

    public void setDestination(Rect destination) {
        this.mDestination = destination;
    }

    public void setIconBounds(Rect bounds) {
        this.mIconBounds = bounds;
    }

    public Rect getIconBounds() {
        return this.mIconBounds;
    }

    public void setOriginalImagePreset(ImagePreset originalImagePreset) {
        this.mOriginalImagePreset = originalImagePreset;
    }
}
