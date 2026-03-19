package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.MotionEvent;
import com.android.gallery3d.filtershow.editors.EditorRotate;
import com.android.gallery3d.filtershow.filters.FilterRotateRepresentation;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;

public class ImageRotate extends ImageShow {
    private static final String TAG = ImageRotate.class.getSimpleName();
    private GeometryMathUtils.GeometryHolder mDrawHolder;
    private EditorRotate mEditorRotate;
    private FilterRotateRepresentation mLocalRep;

    public ImageRotate(Context context) {
        super(context);
        this.mLocalRep = new FilterRotateRepresentation();
        this.mDrawHolder = new GeometryMathUtils.GeometryHolder();
    }

    public void setFilterRotateRepresentation(FilterRotateRepresentation filterRotateRepresentation) {
        if (filterRotateRepresentation == null) {
            filterRotateRepresentation = new FilterRotateRepresentation();
        }
        this.mLocalRep = filterRotateRepresentation;
    }

    public void rotate() {
        this.mLocalRep.rotateCW();
        invalidate();
    }

    public FilterRotateRepresentation getFinalRepresentation() {
        return this.mLocalRep;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return true;
    }

    public int getLocalValue() {
        return this.mLocalRep.getRotation().value();
    }

    @Override
    public void onDraw(Canvas canvas) {
        Bitmap filtersOnlyImage = MasterImage.getImage().getFiltersOnlyImage();
        if (filtersOnlyImage == null) {
            return;
        }
        GeometryMathUtils.initializeHolder(this.mDrawHolder, this.mLocalRep);
        GeometryMathUtils.drawTransformedCropped(this.mDrawHolder, canvas, filtersOnlyImage, canvas.getWidth(), canvas.getHeight());
    }

    public void setEditor(EditorRotate editorRotate) {
        this.mEditorRotate = editorRotate;
    }
}
