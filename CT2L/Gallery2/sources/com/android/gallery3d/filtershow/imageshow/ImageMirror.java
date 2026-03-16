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

    public void setFilterMirrorRepresentation(FilterMirrorRepresentation rep) {
        if (rep == null) {
            rep = new FilterMirrorRepresentation();
        }
        this.mLocalRep = rep;
    }

    public void flip() {
        this.mLocalRep.cycle();
        invalidate();
    }

    public FilterMirrorRepresentation getFinalRepresentation() {
        return this.mLocalRep;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        MasterImage master = MasterImage.getImage();
        Bitmap image = master.getFiltersOnlyImage();
        if (image != null) {
            GeometryMathUtils.initializeHolder(this.mDrawHolder, this.mLocalRep);
            GeometryMathUtils.drawTransformedCropped(this.mDrawHolder, canvas, image, getWidth(), getHeight());
        }
    }

    public void setEditor(EditorMirror editorFlip) {
        this.mEditorMirror = editorFlip;
    }
}
