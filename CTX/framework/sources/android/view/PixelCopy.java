package android.view;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class PixelCopy {
    public static final int ERROR_DESTINATION_INVALID = 5;
    public static final int ERROR_SOURCE_INVALID = 4;
    public static final int ERROR_SOURCE_NO_DATA = 3;
    public static final int ERROR_TIMEOUT = 2;
    public static final int ERROR_UNKNOWN = 1;
    public static final int SUCCESS = 0;

    @Retention(RetentionPolicy.SOURCE)
    public @interface CopyResultStatus {
    }

    public interface OnPixelCopyFinishedListener {
        void onPixelCopyFinished(int i);
    }

    public static void request(SurfaceView surfaceView, Bitmap bitmap, OnPixelCopyFinishedListener onPixelCopyFinishedListener, Handler handler) {
        request(surfaceView.getHolder().getSurface(), bitmap, onPixelCopyFinishedListener, handler);
    }

    public static void request(SurfaceView surfaceView, Rect rect, Bitmap bitmap, OnPixelCopyFinishedListener onPixelCopyFinishedListener, Handler handler) {
        request(surfaceView.getHolder().getSurface(), rect, bitmap, onPixelCopyFinishedListener, handler);
    }

    public static void request(Surface surface, Bitmap bitmap, OnPixelCopyFinishedListener onPixelCopyFinishedListener, Handler handler) {
        request(surface, (Rect) null, bitmap, onPixelCopyFinishedListener, handler);
    }

    public static void request(Surface surface, Rect rect, Bitmap bitmap, final OnPixelCopyFinishedListener onPixelCopyFinishedListener, Handler handler) {
        validateBitmapDest(bitmap);
        if (!surface.isValid()) {
            throw new IllegalArgumentException("Surface isn't valid, source.isValid() == false");
        }
        if (rect != null && rect.isEmpty()) {
            throw new IllegalArgumentException("sourceRect is empty");
        }
        final int iCopySurfaceInto = ThreadedRenderer.copySurfaceInto(surface, rect, bitmap);
        handler.post(new Runnable() {
            @Override
            public void run() {
                onPixelCopyFinishedListener.onPixelCopyFinished(iCopySurfaceInto);
            }
        });
    }

    public static void request(Window window, Bitmap bitmap, OnPixelCopyFinishedListener onPixelCopyFinishedListener, Handler handler) {
        request(window, (Rect) null, bitmap, onPixelCopyFinishedListener, handler);
    }

    public static void request(Window window, Rect rect, Bitmap bitmap, OnPixelCopyFinishedListener onPixelCopyFinishedListener, Handler handler) {
        validateBitmapDest(bitmap);
        if (window == null) {
            throw new IllegalArgumentException("source is null");
        }
        if (window.peekDecorView() == null) {
            throw new IllegalArgumentException("Only able to copy windows with decor views");
        }
        Surface surface = null;
        ViewRootImpl viewRootImpl = window.peekDecorView().getViewRootImpl();
        if (viewRootImpl != null) {
            surface = viewRootImpl.mSurface;
            Rect rect2 = viewRootImpl.mWindowAttributes.surfaceInsets;
            if (rect == null) {
                rect = new Rect(rect2.left, rect2.top, viewRootImpl.mWidth + rect2.left, viewRootImpl.mHeight + rect2.top);
            } else {
                rect.offset(rect2.left, rect2.top);
            }
        }
        if (surface == null || !surface.isValid()) {
            throw new IllegalArgumentException("Window doesn't have a backing surface!");
        }
        request(surface, rect, bitmap, onPixelCopyFinishedListener, handler);
    }

    private static void validateBitmapDest(Bitmap bitmap) {
        if (bitmap == null) {
            throw new IllegalArgumentException("Bitmap cannot be null");
        }
        if (bitmap.isRecycled()) {
            throw new IllegalArgumentException("Bitmap is recycled");
        }
        if (!bitmap.isMutable()) {
            throw new IllegalArgumentException("Bitmap is immutable");
        }
    }

    private PixelCopy() {
    }
}
