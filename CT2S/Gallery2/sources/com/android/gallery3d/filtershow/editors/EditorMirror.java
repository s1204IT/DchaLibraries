package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.filters.FilterMirrorRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageMirror;
import com.android.gallery3d.filtershow.imageshow.MasterImage;

public class EditorMirror extends Editor {
    public static final String TAG = EditorMirror.class.getSimpleName();
    ImageMirror mImageMirror;

    public EditorMirror() {
        super(R.id.editorFlip);
        this.mChangesGeometry = true;
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        super.createEditor(context, frameLayout);
        if (this.mImageMirror == null) {
            this.mImageMirror = new ImageMirror(context);
        }
        ImageMirror imageMirror = this.mImageMirror;
        this.mImageShow = imageMirror;
        this.mView = imageMirror;
        this.mImageMirror.setEditor(this);
    }

    @Override
    public void reflectCurrentFilter() {
        MasterImage master = MasterImage.getImage();
        master.setCurrentFilterRepresentation(master.getPreset().getFilterWithSerializationName("MIRROR"));
        super.reflectCurrentFilter();
        FilterRepresentation rep = getLocalRepresentation();
        if (rep == null || (rep instanceof FilterMirrorRepresentation)) {
            this.mImageMirror.setFilterMirrorRepresentation((FilterMirrorRepresentation) rep);
        } else {
            Log.w(TAG, "Could not reflect current filter, not of type: " + FilterMirrorRepresentation.class.getSimpleName());
        }
        this.mImageMirror.invalidate();
    }

    @Override
    public void openUtilityPanel(LinearLayout accessoryViewList) {
        Button button = (Button) accessoryViewList.findViewById(R.id.applyEffect);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                EditorMirror.this.mImageMirror.flip();
            }
        });
    }

    @Override
    public void finalApplyCalled() {
        commitLocalRepresentation(this.mImageMirror.getFinalRepresentation());
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
