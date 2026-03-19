package android.support.design.transformation;

import android.content.Context;
import android.support.design.animation.MotionSpec;
import android.support.design.animation.Positioning;
import android.support.design.transformation.FabTransformationBehavior;
import android.util.AttributeSet;
import com.android.contacts.ContactPhotoManager;

public class FabTransformationSheetBehavior extends FabTransformationBehavior {
    public FabTransformationSheetBehavior() {
    }

    public FabTransformationSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected FabTransformationBehavior.FabTransformationSpec onCreateMotionSpec(Context context, boolean expanded) {
        int specRes;
        if (expanded) {
            specRes = R.animator.mtrl_fab_transformation_sheet_expand_spec;
        } else {
            specRes = R.animator.mtrl_fab_transformation_sheet_collapse_spec;
        }
        FabTransformationBehavior.FabTransformationSpec spec = new FabTransformationBehavior.FabTransformationSpec();
        spec.timings = MotionSpec.createFromResource(context, specRes);
        spec.positioning = new Positioning(17, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT);
        return spec;
    }
}
