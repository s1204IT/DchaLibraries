package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.widget.FrameLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.filters.FilterRedEyeRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageRedEye;

public class EditorRedEye extends Editor {
    public static int ID = R.id.editorRedEye;
    private final String LOGTAG;
    ImageRedEye mImageRedEyes;

    public EditorRedEye() {
        super(ID);
        this.LOGTAG = "EditorRedEye";
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        super.createEditor(context, frameLayout);
        ImageRedEye imageRedEye = new ImageRedEye(context);
        this.mImageRedEyes = imageRedEye;
        this.mImageShow = imageRedEye;
        this.mView = imageRedEye;
        this.mImageRedEyes.setEditor(this);
    }

    @Override
    public void reflectCurrentFilter() {
        super.reflectCurrentFilter();
        FilterRepresentation localRepresentation = getLocalRepresentation();
        if (localRepresentation != null && (getLocalRepresentation() instanceof FilterRedEyeRepresentation)) {
            this.mImageRedEyes.setRepresentation((FilterRedEyeRepresentation) localRepresentation);
        }
    }

    @Override
    public boolean showsSeekBar() {
        return false;
    }
}
