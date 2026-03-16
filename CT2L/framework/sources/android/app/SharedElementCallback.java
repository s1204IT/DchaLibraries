package android.app;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.transition.TransitionUtils;
import android.view.View;
import android.widget.ImageView;
import java.util.List;
import java.util.Map;

public abstract class SharedElementCallback {
    private static final String BUNDLE_SNAPSHOT_BITMAP = "sharedElement:snapshot:bitmap";
    private static final String BUNDLE_SNAPSHOT_IMAGE_MATRIX = "sharedElement:snapshot:imageMatrix";
    private static final String BUNDLE_SNAPSHOT_IMAGE_SCALETYPE = "sharedElement:snapshot:imageScaleType";
    static final SharedElementCallback NULL_CALLBACK = new SharedElementCallback() {
    };
    private Matrix mTempMatrix;

    public void onSharedElementStart(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
    }

    public void onSharedElementEnd(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
    }

    public void onRejectSharedElements(List<View> rejectedSharedElements) {
    }

    public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
    }

    public Parcelable onCaptureSharedElementSnapshot(View sharedElement, Matrix viewToGlobalMatrix, RectF screenBounds) {
        Bitmap bitmap;
        if (sharedElement instanceof ImageView) {
            ImageView imageView = (ImageView) sharedElement;
            Drawable d = imageView.getDrawable();
            Drawable bg = imageView.getBackground();
            if (d != null && ((bg == null || bg.getAlpha() == 0) && (bitmap = TransitionUtils.createDrawableBitmap(d)) != null)) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(BUNDLE_SNAPSHOT_BITMAP, bitmap);
                bundle.putString(BUNDLE_SNAPSHOT_IMAGE_SCALETYPE, imageView.getScaleType().toString());
                if (imageView.getScaleType() == ImageView.ScaleType.MATRIX) {
                    Matrix matrix = imageView.getImageMatrix();
                    float[] values = new float[9];
                    matrix.getValues(values);
                    bundle.putFloatArray(BUNDLE_SNAPSHOT_IMAGE_MATRIX, values);
                    return bundle;
                }
                return bundle;
            }
        }
        if (this.mTempMatrix == null) {
            this.mTempMatrix = new Matrix(viewToGlobalMatrix);
        } else {
            this.mTempMatrix.set(viewToGlobalMatrix);
        }
        return TransitionUtils.createViewBitmap(sharedElement, this.mTempMatrix, screenBounds);
    }

    public View onCreateSnapshotView(Context context, Parcelable snapshot) {
        View view = null;
        if (snapshot instanceof Bundle) {
            Bundle bundle = (Bundle) snapshot;
            Bitmap bitmap = (Bitmap) bundle.getParcelable(BUNDLE_SNAPSHOT_BITMAP);
            if (bitmap == null) {
                return null;
            }
            ImageView imageView = new ImageView(context);
            view = imageView;
            imageView.setImageBitmap(bitmap);
            imageView.setScaleType(ImageView.ScaleType.valueOf(bundle.getString(BUNDLE_SNAPSHOT_IMAGE_SCALETYPE)));
            if (imageView.getScaleType() == ImageView.ScaleType.MATRIX) {
                float[] values = bundle.getFloatArray(BUNDLE_SNAPSHOT_IMAGE_MATRIX);
                Matrix matrix = new Matrix();
                matrix.setValues(values);
                imageView.setImageMatrix(matrix);
            }
        } else if (snapshot instanceof Bitmap) {
            view = new View(context);
            Resources resources = context.getResources();
            view.setBackground(new BitmapDrawable(resources, (Bitmap) snapshot));
        }
        return view;
    }
}
