package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.widget.FrameLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.filters.FilterTinyPlanetRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageTinyPlanet;

public class EditorTinyPlanet extends BasicEditor {
    ImageTinyPlanet mImageTinyPlanet;

    public EditorTinyPlanet() {
        super(R.id.tinyPlanetEditor, R.layout.filtershow_tiny_planet_editor, R.id.imageTinyPlanet);
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        super.createEditor(context, frameLayout);
        this.mImageTinyPlanet = (ImageTinyPlanet) this.mImageShow;
        this.mImageTinyPlanet.setEditor(this);
    }

    @Override
    public void reflectCurrentFilter() {
        super.reflectCurrentFilter();
        ?? localRepresentation = getLocalRepresentation();
        if (localRepresentation != 0 && (localRepresentation instanceof FilterTinyPlanetRepresentation)) {
            this.mImageTinyPlanet.setRepresentation(localRepresentation);
        }
    }

    public void updateUI() {
        if (this.mControl != null) {
            this.mControl.updateUI();
        }
    }
}
