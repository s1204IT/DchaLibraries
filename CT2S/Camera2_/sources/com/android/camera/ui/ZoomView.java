package com.android.camera.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;
import com.android.camera.data.LocalDataUtil;
import com.android.camera.debug.Log;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ZoomView extends ImageView {
    private static final Log.Tag TAG = new Log.Tag("ZoomView");
    private int mOrientation;
    private DecodePartialBitmap mPartialDecodingTask;
    private BitmapRegionDecoder mRegionDecoder;
    private Uri mUri;
    private int mViewportHeight;
    private int mViewportWidth;

    private class DecodePartialBitmap extends AsyncTask<RectF, Void, Bitmap> {
        BitmapRegionDecoder mDecoder;

        private DecodePartialBitmap() {
        }

        @Override
        protected void onPreExecute() {
            this.mDecoder = ZoomView.this.mRegionDecoder;
        }

        @Override
        protected Bitmap doInBackground(RectF... params) {
            RectF endRect = params[0];
            InputStream isForDimensions = ZoomView.this.getInputStream();
            if (isForDimensions == null) {
                return null;
            }
            Point imageSize = LocalDataUtil.decodeBitmapDimension(isForDimensions);
            try {
                isForDimensions.close();
            } catch (IOException e) {
                Log.e(ZoomView.TAG, "exception closing dimensions inputstream", e);
            }
            if (imageSize == null) {
                return null;
            }
            RectF fullResRect = new RectF(0.0f, 0.0f, imageSize.x - 1, imageSize.y - 1);
            Matrix rotationMatrix = new Matrix();
            rotationMatrix.setRotate(ZoomView.this.mOrientation, 0.0f, 0.0f);
            rotationMatrix.mapRect(fullResRect);
            rotationMatrix.postTranslate(-fullResRect.left, -fullResRect.top);
            rotationMatrix.mapRect(fullResRect, new RectF(0.0f, 0.0f, imageSize.x - 1, imageSize.y - 1));
            RectF visibleRect = new RectF(endRect);
            visibleRect.intersect(0.0f, 0.0f, ZoomView.this.mViewportWidth - 1, ZoomView.this.mViewportHeight - 1);
            Matrix mapping = new Matrix();
            mapping.setRectToRect(endRect, fullResRect, Matrix.ScaleToFit.CENTER);
            RectF visibleAfterRotation = new RectF();
            mapping.mapRect(visibleAfterRotation, visibleRect);
            RectF visibleInImage = new RectF();
            Matrix invertRotation = new Matrix();
            rotationMatrix.invert(invertRotation);
            invertRotation.mapRect(visibleInImage, visibleAfterRotation);
            Rect region = new Rect();
            visibleInImage.round(region);
            region.intersect(0, 0, imageSize.x - 1, imageSize.y - 1);
            if (region.width() == 0 || region.height() == 0) {
                Log.e(ZoomView.TAG, "Invalid size for partial region. Region: " + region.toString());
                return null;
            }
            if (isCancelled()) {
                return null;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            if ((ZoomView.this.mOrientation + 360) % 180 == 0) {
                options.inSampleSize = ZoomView.this.getSampleFactor(region.width(), region.height());
            } else {
                options.inSampleSize = ZoomView.this.getSampleFactor(region.height(), region.width());
            }
            if (this.mDecoder == null) {
                InputStream is = ZoomView.this.getInputStream();
                if (is == null) {
                    return null;
                }
                try {
                    this.mDecoder = BitmapRegionDecoder.newInstance(is, false);
                    is.close();
                } catch (IOException e2) {
                    Log.e(ZoomView.TAG, "Failed to instantiate region decoder");
                }
            }
            if (this.mDecoder == null) {
                return null;
            }
            Bitmap b = this.mDecoder.decodeRegion(region, options);
            if (isCancelled()) {
                return null;
            }
            Matrix rotation = new Matrix();
            rotation.setRotate(ZoomView.this.mOrientation);
            return Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), rotation, false);
        }

        @Override
        protected void onPostExecute(Bitmap b) {
            ZoomView.this.mPartialDecodingTask = null;
            if (this.mDecoder != ZoomView.this.mRegionDecoder) {
                this.mDecoder.recycle();
            }
            if (b != null) {
                ZoomView.this.setImageBitmap(b);
                ZoomView.this.showPartiallyDecodedImage(true);
            }
        }
    }

    public ZoomView(Context context) {
        super(context);
        this.mViewportWidth = 0;
        this.mViewportHeight = 0;
        setScaleType(ImageView.ScaleType.FIT_CENTER);
        addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int w = right - left;
                int h = bottom - top;
                if (ZoomView.this.mViewportHeight != h || ZoomView.this.mViewportWidth != w) {
                    ZoomView.this.mViewportWidth = w;
                    ZoomView.this.mViewportHeight = h;
                }
            }
        });
    }

    public void resetDecoder() {
        if (this.mRegionDecoder != null) {
            cancelPartialDecodingTask();
            if (this.mPartialDecodingTask == null) {
                this.mRegionDecoder.recycle();
            }
            this.mRegionDecoder = null;
        }
    }

    public void loadBitmap(Uri uri, int orientation, RectF imageRect) {
        if (!uri.equals(this.mUri)) {
            resetDecoder();
            this.mUri = uri;
            this.mOrientation = orientation;
        }
        startPartialDecodingTask(imageRect);
    }

    private void showPartiallyDecodedImage(boolean show) {
        if (show) {
            setVisibility(0);
        } else {
            setVisibility(8);
        }
    }

    public void cancelPartialDecodingTask() {
        if (this.mPartialDecodingTask != null && !this.mPartialDecodingTask.isCancelled()) {
            this.mPartialDecodingTask.cancel(true);
            setVisibility(8);
        }
    }

    public static RectF adjustToFitInBounds(RectF rect, int viewportWidth, int viewportHeight) {
        float dx = 0.0f;
        float dy = 0.0f;
        RectF newRect = new RectF(rect);
        if (newRect.width() < viewportWidth) {
            dx = (viewportWidth / 2) - ((newRect.left + newRect.right) / 2.0f);
        } else if (newRect.left > 0.0f) {
            dx = -newRect.left;
        } else if (newRect.right < viewportWidth) {
            dx = viewportWidth - newRect.right;
        }
        if (newRect.height() < viewportHeight) {
            dy = (viewportHeight / 2) - ((newRect.top + newRect.bottom) / 2.0f);
        } else if (newRect.top > 0.0f) {
            dy = -newRect.top;
        } else if (newRect.bottom < viewportHeight) {
            dy = viewportHeight - newRect.bottom;
        }
        if (dx != 0.0f || dy != 0.0f) {
            newRect.offset(dx, dy);
        }
        return newRect;
    }

    private void startPartialDecodingTask(RectF endRect) {
        cancelPartialDecodingTask();
        this.mPartialDecodingTask = new DecodePartialBitmap();
        this.mPartialDecodingTask.execute(endRect);
    }

    private InputStream getInputStream() {
        try {
            InputStream is = getContext().getContentResolver().openInputStream(this.mUri);
            return is;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found at: " + this.mUri);
            return null;
        }
    }

    private int getSampleFactor(int width, int height) {
        float fitWidthScale = this.mViewportWidth / width;
        float fitHeightScale = this.mViewportHeight / height;
        float scale = Math.min(fitHeightScale, fitWidthScale);
        int sampleFactor = (int) (1.0f / scale);
        if (sampleFactor <= 1) {
            return 1;
        }
        int i = 0;
        while (true) {
            if (i >= 32) {
                break;
            }
            if ((1 << (i + 1)) <= sampleFactor) {
                i++;
            } else {
                sampleFactor = 1 << i;
                break;
            }
        }
        return sampleFactor;
    }
}
