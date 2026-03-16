package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRotateRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageRotate;
import com.android.gallery3d.filtershow.imageshow.MasterImage;

public class EditorRotate extends Editor {
    public static final String TAG = EditorRotate.class.getSimpleName();
    ImageRotate mImageRotate;

    public EditorRotate() {
        super(R.id.editorRotate);
        this.mChangesGeometry = true;
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        super.createEditor(context, frameLayout);
        if (this.mImageRotate == null) {
            this.mImageRotate = new ImageRotate(context);
        }
        ImageRotate imageRotate = this.mImageRotate;
        this.mImageShow = imageRotate;
        this.mView = imageRotate;
        this.mImageRotate.setEditor(this);
    }

    @Override
    public void reflectCurrentFilter() {
        MasterImage master = MasterImage.getImage();
        master.setCurrentFilterRepresentation(master.getPreset().getFilterWithSerializationName("ROTATION"));
        super.reflectCurrentFilter();
        FilterRepresentation rep = getLocalRepresentation();
        if (rep == null || (rep instanceof FilterRotateRepresentation)) {
            this.mImageRotate.setFilterRotateRepresentation((FilterRotateRepresentation) rep);
        } else {
            Log.w(TAG, "Could not reflect current filter, not of type: " + FilterRotateRepresentation.class.getSimpleName());
        }
        this.mImageRotate.invalidate();
    }

    @Override
    public void openUtilityPanel(LinearLayout accessoryViewList) {
        final Button button = (Button) accessoryViewList.findViewById(R.id.applyEffect);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                EditorRotate.this.mImageRotate.rotate();
                String displayVal = EditorRotate.this.mContext.getString(EditorRotate.this.getTextId()) + " " + EditorRotate.this.mImageRotate.getLocalValue();
                button.setText(displayVal);
            }
        });
    }

    @Override
    public void finalApplyCalled() {
        commitLocalRepresentation(this.mImageRotate.getFinalRepresentation());
    }

    public int getTextId() {
        return R.string.rotate;
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
