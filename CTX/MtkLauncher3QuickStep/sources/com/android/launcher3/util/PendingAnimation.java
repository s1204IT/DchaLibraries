package com.android.launcher3.util;

import android.animation.AnimatorSet;
import android.annotation.TargetApi;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

@TargetApi(26)
public class PendingAnimation {
    public final AnimatorSet anim;
    private final ArrayList<Consumer<OnEndListener>> mEndListeners = new ArrayList<>();

    public PendingAnimation(AnimatorSet animatorSet) {
        this.anim = animatorSet;
    }

    public void finish(boolean z, int i) {
        Iterator<Consumer<OnEndListener>> it = this.mEndListeners.iterator();
        while (it.hasNext()) {
            it.next().accept(new OnEndListener(z, i));
        }
        this.mEndListeners.clear();
    }

    public void addEndListener(Consumer<OnEndListener> consumer) {
        this.mEndListeners.add(consumer);
    }

    public static class OnEndListener {
        public boolean isSuccess;
        public int logAction;

        public OnEndListener(boolean z, int i) {
            this.isSuccess = z;
            this.logAction = i;
        }
    }
}
