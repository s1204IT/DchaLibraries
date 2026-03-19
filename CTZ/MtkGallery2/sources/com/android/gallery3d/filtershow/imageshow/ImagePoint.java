package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import com.android.gallery3d.filtershow.editors.EditorRedEye;
import com.android.gallery3d.filtershow.filters.FilterPoint;
import com.android.gallery3d.filtershow.filters.FilterRedEyeRepresentation;
import com.android.gallery3d.filtershow.filters.ImageFilterRedEye;
import java.util.Iterator;

public abstract class ImagePoint extends ImageShow {
    protected static float mTouchPadding = 80.0f;
    protected EditorRedEye mEditorRedEye;
    protected FilterRedEyeRepresentation mRedEyeRep;

    protected abstract void drawPoint(FilterPoint filterPoint, Canvas canvas, Matrix matrix, Matrix matrix2, Paint paint);

    public ImagePoint(Context context) {
        super(context);
    }

    @Override
    public void resetParameter() {
        ImageFilterRedEye imageFilterRedEye = (ImageFilterRedEye) getCurrentFilter();
        if (imageFilterRedEye != null) {
            imageFilterRedEye.clear();
        }
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(-65536);
        paint.setStrokeWidth(2.0f);
        Matrix imageToScreenMatrix = getImageToScreenMatrix(false);
        Matrix imageToScreenMatrix2 = getImageToScreenMatrix(true);
        if (this.mRedEyeRep != null) {
            Iterator<FilterPoint> it = this.mRedEyeRep.getCandidates().iterator();
            while (it.hasNext()) {
                drawPoint(it.next(), canvas, imageToScreenMatrix, imageToScreenMatrix2, paint);
            }
        }
    }

    public void setEditor(EditorRedEye editorRedEye) {
        this.mEditorRedEye = editorRedEye;
    }

    public void setRepresentation(FilterRedEyeRepresentation filterRedEyeRepresentation) {
        this.mRedEyeRep = filterRedEyeRepresentation;
    }
}
