package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import com.android.gallery3d.filtershow.editors.EditorRedEye;
import com.android.gallery3d.filtershow.filters.FilterPoint;
import com.android.gallery3d.filtershow.filters.FilterRedEyeRepresentation;
import com.android.gallery3d.filtershow.filters.ImageFilterRedEye;

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
        ImageFilterRedEye filter = (ImageFilterRedEye) getCurrentFilter();
        if (filter != null) {
            filter.clear();
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
        Matrix originalToScreen = getImageToScreenMatrix(false);
        Matrix originalRotateToScreen = getImageToScreenMatrix(true);
        if (this.mRedEyeRep != null) {
            for (FilterPoint candidate : this.mRedEyeRep.getCandidates()) {
                drawPoint(candidate, canvas, originalToScreen, originalRotateToScreen, paint);
            }
        }
    }

    public void setEditor(EditorRedEye editorRedEye) {
        this.mEditorRedEye = editorRedEye;
    }

    public void setRepresentation(FilterRedEyeRepresentation redEyeRep) {
        this.mRedEyeRep = redEyeRep;
    }
}
