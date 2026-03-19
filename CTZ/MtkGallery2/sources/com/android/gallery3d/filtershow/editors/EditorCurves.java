package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.filters.FilterCurvesRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageCurves;

public class EditorCurves extends Editor {
    ImageCurves mImageCurves;

    public EditorCurves() {
        super(R.id.imageCurves);
    }

    @Override
    protected void updateText() {
    }

    @Override
    public boolean showsPopupIndicator() {
        return true;
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        super.createEditor(context, frameLayout);
        ImageCurves imageCurves = new ImageCurves(context);
        this.mImageCurves = imageCurves;
        this.mImageShow = imageCurves;
        this.mView = imageCurves;
        this.mImageCurves.setEditor(this);
    }

    @Override
    public void reflectCurrentFilter() {
        super.reflectCurrentFilter();
        FilterRepresentation localRepresentation = getLocalRepresentation();
        if (localRepresentation != null && (getLocalRepresentation() instanceof FilterCurvesRepresentation)) {
            this.mImageCurves.setFilterDrawRepresentation((FilterCurvesRepresentation) localRepresentation);
        }
    }

    @Override
    public void setUtilityPanelUI(View view, View view2) {
        super.setUtilityPanelUI(view, view2);
        setMenuIcon(true);
    }

    @Override
    public boolean showsSeekBar() {
        return false;
    }
}
