package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.MotionEvent;
import com.android.gallery3d.filtershow.editors.EditorMirror;
import com.android.gallery3d.filtershow.filters.FilterMirrorRepresentation;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;

public class ImageMirror extends ImageShow {
    private static final String TAG = ImageMirror.class.getSimpleName();
    private GeometryMathUtils.GeometryHolder mDrawHolder;
    private EditorMirror mEditorMirror;
    private FilterMirrorRepresentation mLocalRep;

    public ImageMirror(Context context) {
        super(context);
        this.mLocalRep = new FilterMirrorRepresentation();
        this.mDrawHolder = new GeometryMathUtils.GeometryHolder();
    }

    public void setFilterMirrorRepresentation(FilterMirrorRepresentation filterMirrorRepresentation) {
        if (filterMirrorRepresentation == null) {
            filterMirrorRepresentation = new FilterMirrorRepresentation();
        }
        this.mLocalRep = filterMirrorRepresentation;
    }

    public void flip() {
        this.mLocalRep.cycle();
        invalidate();
    }

    public FilterMirrorRepresentation getFinalRepresentation() {
        return this.mLocalRep;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        Bitmap filtersOnlyImage = MasterImage.getImage().getFiltersOnlyImage();
        if (filtersOnlyImage == null) {
            return;
        }
        GeometryMathUtils.initializeHolder(this.mDrawHolder, this.mLocalRep);
        GeometryMathUtils.drawTransformedCropped(this.mDrawHolder, canvas, filtersOnlyImage, getWidth(), getHeight());
    }

    public void setEditor(EditorMirror editorMirror) {
        this.mEditorMirror = editorMirror;
    }
}
