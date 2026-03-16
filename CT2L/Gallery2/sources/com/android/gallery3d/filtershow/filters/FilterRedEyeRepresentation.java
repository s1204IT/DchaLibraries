package com.android.gallery3d.filtershow.filters;

import android.graphics.RectF;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.editors.EditorRedEye;
import java.util.Vector;

public class FilterRedEyeRepresentation extends FilterPointRepresentation {
    public FilterRedEyeRepresentation() {
        super("RedEye", R.string.redeye, EditorRedEye.ID);
        setSerializationName("REDEYE");
        setFilterClass(ImageFilterRedEye.class);
        setOverlayId(R.drawable.photoeditor_effect_redeye);
        setOverlayOnly(true);
    }

    @Override
    public FilterRepresentation copy() {
        FilterRedEyeRepresentation representation = new FilterRedEyeRepresentation();
        copyAllParameters(representation);
        return representation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    public void addRect(RectF rect, RectF bounds) {
        Vector<RedEyeCandidate> intersects = new Vector<>();
        for (int i = 0; i < getCandidates().size(); i++) {
            RedEyeCandidate r = (RedEyeCandidate) getCandidate(i);
            if (r.intersect(rect)) {
                intersects.add(r);
            }
        }
        for (int i2 = 0; i2 < intersects.size(); i2++) {
            RedEyeCandidate r2 = intersects.elementAt(i2);
            rect.union(r2.mRect);
            bounds.union(r2.mBounds);
            removeCandidate(r2);
        }
        addCandidate(new RedEyeCandidate(rect, bounds));
    }
}
