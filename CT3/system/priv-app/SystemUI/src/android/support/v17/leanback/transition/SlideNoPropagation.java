package android.support.v17.leanback.transition;

import android.content.Context;
import android.transition.Slide;
import android.util.AttributeSet;
/* loaded from: a.zip:android/support/v17/leanback/transition/SlideNoPropagation.class */
public class SlideNoPropagation extends Slide {
    public SlideNoPropagation() {
    }

    public SlideNoPropagation(int i) {
        super(i);
    }

    public SlideNoPropagation(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override // android.transition.Slide
    public void setSlideEdge(int i) {
        super.setSlideEdge(i);
        setPropagation(null);
    }
}
