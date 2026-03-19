package android.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.GraphicBuffer;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.transition.TransitionUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.util.List;
import java.util.Map;

public abstract class SharedElementCallback {
    private static final String BUNDLE_SNAPSHOT_BITMAP = "sharedElement:snapshot:bitmap";
    private static final String BUNDLE_SNAPSHOT_GRAPHIC_BUFFER = "sharedElement:snapshot:graphicBuffer";
    private static final String BUNDLE_SNAPSHOT_IMAGE_MATRIX = "sharedElement:snapshot:imageMatrix";
    private static final String BUNDLE_SNAPSHOT_IMAGE_SCALETYPE = "sharedElement:snapshot:imageScaleType";
    static final SharedElementCallback NULL_CALLBACK = new SharedElementCallback() {
    };
    private Matrix mTempMatrix;

    public interface OnSharedElementsReadyListener {
        void onSharedElementsReady();
    }

    public void onSharedElementStart(List<String> list, List<View> list2, List<View> list3) {
    }

    public void onSharedElementEnd(List<String> list, List<View> list2, List<View> list3) {
    }

    public void onRejectSharedElements(List<View> list) {
    }

    public void onMapSharedElements(List<String> list, Map<String, View> map) {
    }

    public Parcelable onCaptureSharedElementSnapshot(View view, Matrix matrix, RectF rectF) {
        Bitmap bitmapCreateDrawableBitmap;
        if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            Drawable drawable = imageView.getDrawable();
            Drawable background = imageView.getBackground();
            if (drawable != null && ((background == null || background.getAlpha() == 0) && (bitmapCreateDrawableBitmap = TransitionUtils.createDrawableBitmap(drawable, imageView)) != null)) {
                Bundle bundle = new Bundle();
                if (bitmapCreateDrawableBitmap.getConfig() != Bitmap.Config.HARDWARE) {
                    bundle.putParcelable(BUNDLE_SNAPSHOT_BITMAP, bitmapCreateDrawableBitmap);
                } else {
                    bundle.putParcelable(BUNDLE_SNAPSHOT_GRAPHIC_BUFFER, bitmapCreateDrawableBitmap.createGraphicBufferHandle());
                }
                bundle.putString(BUNDLE_SNAPSHOT_IMAGE_SCALETYPE, imageView.getScaleType().toString());
                if (imageView.getScaleType() == ImageView.ScaleType.MATRIX) {
                    float[] fArr = new float[9];
                    imageView.getImageMatrix().getValues(fArr);
                    bundle.putFloatArray(BUNDLE_SNAPSHOT_IMAGE_MATRIX, fArr);
                }
                return bundle;
            }
        }
        if (this.mTempMatrix == null) {
            this.mTempMatrix = new Matrix(matrix);
        } else {
            this.mTempMatrix.set(matrix);
        }
        return TransitionUtils.createViewBitmap(view, this.mTempMatrix, rectF, (ViewGroup) view.getParent());
    }

    public View onCreateSnapshotView(Context context, Parcelable parcelable) {
        if (!(parcelable instanceof Bundle)) {
            if (!(parcelable instanceof Bitmap)) {
                return null;
            }
            View view = new View(context);
            view.setBackground(new BitmapDrawable(context.getResources(), (Bitmap) parcelable));
            return view;
        }
        Bundle bundle = (Bundle) parcelable;
        GraphicBuffer graphicBuffer = (GraphicBuffer) bundle.getParcelable(BUNDLE_SNAPSHOT_GRAPHIC_BUFFER);
        Bitmap bitmapCreateHardwareBitmap = (Bitmap) bundle.getParcelable(BUNDLE_SNAPSHOT_BITMAP);
        if (graphicBuffer == null && bitmapCreateHardwareBitmap == null) {
            return null;
        }
        if (bitmapCreateHardwareBitmap == null) {
            bitmapCreateHardwareBitmap = Bitmap.createHardwareBitmap(graphicBuffer);
        }
        ImageView imageView = new ImageView(context);
        imageView.setImageBitmap(bitmapCreateHardwareBitmap);
        imageView.setScaleType(ImageView.ScaleType.valueOf(bundle.getString(BUNDLE_SNAPSHOT_IMAGE_SCALETYPE)));
        if (imageView.getScaleType() == ImageView.ScaleType.MATRIX) {
            float[] floatArray = bundle.getFloatArray(BUNDLE_SNAPSHOT_IMAGE_MATRIX);
            Matrix matrix = new Matrix();
            matrix.setValues(floatArray);
            imageView.setImageMatrix(matrix);
            return imageView;
        }
        return imageView;
    }

    public void onSharedElementsArrived(List<String> list, List<View> list2, OnSharedElementsReadyListener onSharedElementsReadyListener) {
        onSharedElementsReadyListener.onSharedElementsReady();
    }
}
