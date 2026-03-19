package android.support.v17.leanback.transition;

import android.animation.Animator;
import android.transition.ChangeBounds;
import android.transition.TransitionValues;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import java.util.HashMap;

class CustomChangeBounds extends ChangeBounds {
    int mDefaultStartDelay;
    final HashMap<View, Integer> mViewStartDelays = new HashMap<>();
    final SparseIntArray mIdStartDelays = new SparseIntArray();
    final HashMap<String, Integer> mClassStartDelays = new HashMap<>();

    CustomChangeBounds() {
    }

    private int getDelay(View view) {
        Integer delay = this.mViewStartDelays.get(view);
        if (delay != null) {
            return delay.intValue();
        }
        int idStartDelay = this.mIdStartDelays.get(view.getId(), -1);
        if (idStartDelay != -1) {
            return idStartDelay;
        }
        Integer delay2 = this.mClassStartDelays.get(view.getClass().getName());
        if (delay2 != null) {
            return delay2.intValue();
        }
        return this.mDefaultStartDelay;
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
        Animator animator = super.createAnimator(sceneRoot, startValues, endValues);
        if (animator != null && endValues != null && endValues.view != null) {
            animator.setStartDelay(getDelay(endValues.view));
        }
        return animator;
    }
}
