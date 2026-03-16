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

    public void setFilterRotateRepresentation(FilterRotateRepresentation rep) {
        if (rep == null) {
            rep = new FilterRotateRepresentation();
        }
        this.mLocalRep = rep;
    }

    public void rotate() {
        this.mLocalRep.rotateCW();
        invalidate();
    }

    public FilterRotateRepresentation getFinalRepresentation() {
        return this.mLocalRep;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    public int getLocalValue() {
        return this.mLocalRep.getRotation().value();
    }

    @Override
    public void onDraw(Canvas canvas) {
        MasterImage master = MasterImage.getImage();
        Bitmap image = master.getFiltersOnlyImage();
        if (image != null) {
            GeometryMathUtils.initializeHolder(this.mDrawHolder, this.mLocalRep);
            GeometryMathUtils.drawTransformedCropped(this.mDrawHolder, canvas, image, canvas.getWidth(), canvas.getHeight());
        }
    }

    public void setEditor(EditorRotate editorRotate) {
        this.mEditorRotate = editorRotate;
    }
}
