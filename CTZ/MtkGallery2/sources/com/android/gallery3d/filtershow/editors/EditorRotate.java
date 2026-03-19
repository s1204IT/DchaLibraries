package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRotateRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageRotate;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.mediatek.gallery3d.util.Log;

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
        MasterImage image = MasterImage.getImage();
        image.setCurrentFilterRepresentation(image.getPreset().getFilterWithSerializationName("ROTATION"));
        super.reflectCurrentFilter();
        FilterRepresentation localRepresentation = getLocalRepresentation();
        if (localRepresentation == null || (localRepresentation instanceof FilterRotateRepresentation)) {
            this.mImageRotate.setFilterRotateRepresentation((FilterRotateRepresentation) localRepresentation);
        } else {
            Log.w(TAG, "Could not reflect current filter, not of type: " + FilterRotateRepresentation.class.getSimpleName());
        }
        this.mImageRotate.invalidate();
    }

    @Override
    public void openUtilityPanel(LinearLayout linearLayout) {
        final Button button = (Button) linearLayout.findViewById(R.id.applyEffect);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditorRotate.this.mImageRotate.rotate();
                button.setText(EditorRotate.this.mContext.getString(EditorRotate.this.getTextId()) + " " + EditorRotate.this.mImageRotate.getLocalValue());
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
