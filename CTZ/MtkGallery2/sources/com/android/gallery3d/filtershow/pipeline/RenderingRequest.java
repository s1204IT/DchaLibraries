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

    public static void post(Context context, Bitmap bitmap, ImagePreset imagePreset, int i, RenderingRequestCaller renderingRequestCaller) {
        post(context, bitmap, imagePreset, i, renderingRequestCaller, null, null);
    }

    public static void post(Context context, Bitmap bitmap, ImagePreset imagePreset, int i, RenderingRequestCaller renderingRequestCaller, Rect rect, Rect rect2) {
        if ((i != 4 && i != 5 && i != 2 && i != 1 && bitmap == null) || imagePreset == null || renderingRequestCaller == null) {
            Log.v("RenderingRequest", "something null: source: " + bitmap + " or preset: " + imagePreset + " or caller: " + renderingRequestCaller);
            return;
        }
        RenderingRequest renderingRequest = new RenderingRequest();
        Bitmap bitmapRenderGeometryIcon = null;
        if (i == 0 || i == 3 || i == 6) {
            bitmapRenderGeometryIcon = new CachingPipeline(FiltersManager.getManager(), "Icon").renderGeometryIcon(bitmap, imagePreset);
        } else if (i != 4 && i != 5 && i != 2 && i != 1) {
            bitmapRenderGeometryIcon = MasterImage.getImage().getBitmapCache().getBitmap(bitmap.getWidth(), bitmap.getHeight(), 8);
        }
        renderingRequest.setBitmap(bitmapRenderGeometryIcon);
        ImagePreset imagePreset2 = new ImagePreset(imagePreset);
        renderingRequest.setOriginalImagePreset(imagePreset);
        renderingRequest.setScaleFactor(MasterImage.getImage().getScaleFactor());
        if (i == 4) {
            renderingRequest.setBounds(rect);
            renderingRequest.setDestination(rect2);
            imagePreset2.setPartialRendering(true, rect);
        }
        renderingRequest.setImagePreset(imagePreset2);
        renderingRequest.setType(i);
        renderingRequest.setCaller(renderingRequestCaller);
        renderingRequest.post(context);
    }

    public static void postIconRequest(Context context, int i, int i2, ImagePreset imagePreset, RenderingRequestCaller renderingRequestCaller) {
        if (imagePreset == null || renderingRequestCaller == null) {
            Log.v("RenderingRequest", "something null, preset: " + imagePreset + " or caller: " + renderingRequestCaller);
            return;
        }
        RenderingRequest renderingRequest = new RenderingRequest();
        ImagePreset imagePreset2 = new ImagePreset(imagePreset);
        renderingRequest.setOriginalImagePreset(imagePreset);
        renderingRequest.setScaleFactor(MasterImage.getImage().getScaleFactor());
        renderingRequest.setImagePreset(imagePreset2);
        renderingRequest.setType(3);
        renderingRequest.setCaller(renderingRequestCaller);
        renderingRequest.setIconBounds(new Rect(0, 0, i, i2));
        renderingRequest.post(context);
    }

    public void post(Context context) {
        if (context instanceof FilterShowActivity) {
            context.getProcessingService().postRenderingRequest(this);
        }
    }

    public void markAvailable() {
        if (this.mBitmap == null || this.mImagePreset == null || this.mCaller == null) {
            return;
        }
        this.mCaller.available(this);
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

    public void setType(int i) {
        this.mType = i;
    }

    public void setCaller(RenderingRequestCaller renderingRequestCaller) {
        this.mCaller = renderingRequestCaller;
    }

    public Rect getBounds() {
        return this.mBounds;
    }

    public void setBounds(Rect rect) {
        this.mBounds = rect;
    }

    public void setScaleFactor(float f) {
        this.mScaleFactor = f;
    }

    public float getScaleFactor() {
        return this.mScaleFactor;
    }

    public Rect getDestination() {
        return this.mDestination;
    }

    public void setDestination(Rect rect) {
        this.mDestination = rect;
    }

    public void setIconBounds(Rect rect) {
        this.mIconBounds = rect;
    }

    public Rect getIconBounds() {
        return this.mIconBounds;
    }

    public void setOriginalImagePreset(ImagePreset imagePreset) {
        this.mOriginalImagePreset = imagePreset;
    }
}
