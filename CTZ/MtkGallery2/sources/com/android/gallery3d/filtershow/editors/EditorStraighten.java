package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.widget.FrameLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterStraightenRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageStraighten;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.mediatek.gallery3d.util.Log;

public class EditorStraighten extends Editor {
    public static final String TAG = EditorStraighten.class.getSimpleName();
    ImageStraighten mImageStraighten;

    public EditorStraighten() {
        super(R.id.editorStraighten);
        this.mShowParameter = SHOW_VALUE_INT;
        this.mChangesGeometry = true;
    }

    @Override
    public String calculateUserMessage(Context context, String str, Object obj) {
        return (context.getString(R.string.apply_effect) + " " + str).toUpperCase();
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
        MasterImage image = MasterImage.getImage();
        image.setCurrentFilterRepresentation(image.getPreset().getFilterWithSerializationName("STRAIGHTEN"));
        super.reflectCurrentFilter();
        FilterRepresentation localRepresentation = getLocalRepresentation();
        if (localRepresentation == null || (localRepresentation instanceof FilterStraightenRepresentation)) {
            this.mImageStraighten.setFilterStraightenRepresentation((FilterStraightenRepresentation) localRepresentation);
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
