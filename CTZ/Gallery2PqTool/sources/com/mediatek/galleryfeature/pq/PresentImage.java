package com.mediatek.galleryfeature.pq;

import android.content.Context;
import android.graphics.Bitmap;

public class PresentImage {
    private static PresentImage sPresentImage;
    private RenderingRequestListener mListener;
    private LoadBitmapTask mLoadBitmapTask;

    public interface RenderingRequestListener {
        boolean available(Bitmap bitmap, String str);
    }

    public static PresentImage getPresentImage() {
        if (sPresentImage == null) {
            sPresentImage = new PresentImage();
        }
        return sPresentImage;
    }

    public void setListener(Context context, RenderingRequestListener renderingRequestListener) {
        LoadBitmapTask.init(context);
        this.mListener = renderingRequestListener;
    }

    public void setBitmap(Bitmap bitmap, String str) {
        boolean zAvailable = this.mListener.available(bitmap, str);
        if (LoadBitmapTask.needRegionDecode() && zAvailable) {
            loadBitmap(str);
        }
    }

    public void loadBitmap(String str) {
        if (str == null) {
            return;
        }
        stopLoadBitmap();
        this.mLoadBitmapTask = new LoadBitmapTask(str);
        this.mLoadBitmapTask.execute(new Void[0]);
    }

    public void free() {
        if (this.mLoadBitmapTask != null) {
            this.mLoadBitmapTask.free();
        }
    }

    public void stopLoadBitmap() {
        if (this.mLoadBitmapTask == null) {
            return;
        }
        this.mLoadBitmapTask.cancel(true);
    }
}
