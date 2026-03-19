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
        FilterRedEyeRepresentation filterRedEyeRepresentation = new FilterRedEyeRepresentation();
        copyAllParameters(filterRedEyeRepresentation);
        return filterRedEyeRepresentation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation filterRepresentation) {
        super.copyAllParameters(filterRepresentation);
        filterRepresentation.useParametersFrom(this);
    }

    public void addRect(RectF rectF, RectF rectF2) {
        Vector vector = new Vector();
        for (int i = 0; i < getCandidates().size(); i++) {
            RedEyeCandidate redEyeCandidate = (RedEyeCandidate) getCandidate(i);
            if (redEyeCandidate.intersect(rectF)) {
                vector.add(redEyeCandidate);
            }
        }
        for (int i2 = 0; i2 < vector.size(); i2++) {
            RedEyeCandidate redEyeCandidate2 = (RedEyeCandidate) vector.elementAt(i2);
            rectF.union(redEyeCandidate2.mRect);
            rectF2.union(redEyeCandidate2.mBounds);
            removeCandidate(redEyeCandidate2);
        }
        addCandidate(new RedEyeCandidate(rectF, rectF2));
    }
}
