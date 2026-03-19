package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.widget.FrameLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.imageshow.ImageShow;

public class ImageOnlyEditor extends Editor {
    private final String LOGTAG;

    public ImageOnlyEditor() {
        super(R.id.imageOnlyEditor);
        this.LOGTAG = "ImageOnlyEditor";
    }

    @Override
    public boolean useUtilityPanel() {
        return false;
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        super.createEditor(context, frameLayout);
        ImageShow imageShow = new ImageShow(context);
        this.mImageShow = imageShow;
        this.mView = imageShow;
    }
}
