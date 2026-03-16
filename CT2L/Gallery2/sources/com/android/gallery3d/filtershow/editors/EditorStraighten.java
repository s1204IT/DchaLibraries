package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.util.Log;
import android.widget.FrameLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterStraightenRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageStraighten;
import com.android.gallery3d.filtershow.imageshow.MasterImage;

public class EditorStraighten extends Editor {
    public static final String TAG = EditorStraighten.class.getSimpleName();
    ImageStraighten mImageStraighten;

    public EditorStraighten() {
        super(R.id.editorStraighten);
        this.mShowParameter = SHOW_VALUE_INT;
        this.mChangesGeometry = true;
    }

    @Override
    public String calculateUserMessage(Context context, String effectName, Object parameterValue) {
        String apply = context.getString(R.string.apply_effect);
        return (apply + " " + effectName).toUpperCase();
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        super.createEditor(context, frameLayout);
        if (this.mImageStraighten == null) {
            this.mImageStraighten = new ImageStraighten(context);
        }
        ImageStraighten imageStraighten = this.mImageStraighten;
        this.mImageShow = imageStraighten;
        this.mView = imageStraighten;
        this.mImageStraighten.setEditor(this);
    }

    @Override
    public void reflectCurrentFilter() {
        MasterImage master = MasterImage.getImage();
        master.setCurrentFilterRepresentation(master.getPreset().getFilterWithSerializationName("STRAIGHTEN"));
        super.reflectCurrentFilter();
        FilterRepresentation rep = getLocalRepresentation();
        if (rep == null || (rep instanceof FilterStraightenRepresentation)) {
            this.mImageStraighten.setFilterStraightenRepresentation((FilterStraightenRepresentation) rep);
        } else {
            Log.w(TAG, "Could not reflect current filter, not of type: " + FilterStraightenRepresentation.class.getSimpleName());
        }
        this.mImageStraighten.invalidate();
    }

    @Override
    public void finalApplyCalled() {
        commitLocalRepresentation(this.mImageStraighten.getFinalRepresentation());
    }

    @Override
    public boolean showsSeekBar() {
        return false;
    }

    @Override
    public boolean showsPopupIndicator() {
        return false;
    }
}
