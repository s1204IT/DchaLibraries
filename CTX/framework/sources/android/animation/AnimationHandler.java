package android.animation;

import android.os.SystemClock;
import android.util.ArrayMap;
import android.view.Choreographer;
import java.util.ArrayList;

public class AnimationHandler {
    public static final ThreadLocal<AnimationHandler> sAnimatorHandler = new ThreadLocal<>();
    private AnimationFrameCallbackProvider mProvider;
    private final ArrayMap<AnimationFrameCallback, Long> mDelayedCallbackStartTime = new ArrayMap<>();
    private final ArrayList<AnimationFrameCallback> mAnimationCallbacks = new ArrayList<>();
    private final ArrayList<AnimationFrameCallback> mCommitCallbacks = new ArrayList<>();
    private final Choreographer.FrameCallback mFrameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long j) {
            AnimationHandler.this.doAnimationFrame(AnimationHandler.this.getProvider().getFrameTime());
            if (AnimationHandler.this.mAnimationCallbacks.size() > 0) {
                AnimationHandler.this.getProvider().postFrameCallback(this);
            }
        }
    };
    private boolean mListDirty = false;

    interface AnimationFrameCallback {
        void commitAnimationFrame(long j);

        boolean doAnimationFrame(long j);
    }

    public interface AnimationFrameCallbackProvider {
        long getFrameDelay();

        long getFrameTime();

        void postCommitCallback(Runnable runnable);

        void postFrameCallback(Choreographer.FrameCallback frameCallback);

        void setFrameDelay(long j);
    }

    public static AnimationHandler getInstance() {
        if (sAnimatorHandler.get() == null) {
            sAnimatorHandler.set(new AnimationHandler());
        }
        return sAnimatorHandler.get();
    }

    public void setProvider(AnimationFrameCallbackProvider animationFrameCallbackProvider) {
        if (animationFrameCallbackProvider == null) {
            this.mProvider = new MyFrameCallbackProvider();
        } else {
            this.mProvider = animationFrameCallbackProvider;
        }
    }

    private AnimationFrameCallbackProvider getProvider() {
        if (this.mProvider == null) {
            this.mProvider = new MyFrameCallbackProvider();
        }
        return this.mProvider;
    }

    public void addAnimationFrameCallback(AnimationFrameCallback animationFrameCallback, long j) {
        if (this.mAnimationCallbacks.size() == 0) {
            getProvider().postFrameCallback(this.mFrameCallback);
        }
        if (!this.mAnimationCallbacks.contains(animationFrameCallback)) {
            this.mAnimationCallbacks.add(animationFrameCallback);
        }
        if (j > 0) {
            this.mDelayedCallbackStartTime.put(animationFrameCallback, Long.valueOf(SystemClock.uptimeMillis() + j));
        }
    }

    public void addOneShotCommitCallback(AnimationFrameCallback animationFrameCallback) {
        if (!this.mCommitCallbacks.contains(animationFrameCallback)) {
            this.mCommitCallbacks.add(animationFrameCallback);
        }
    }

    public void removeCallback(AnimationFrameCallback animationFrameCallback) {
        this.mCommitCallbacks.remove(animationFrameCallback);
        this.mDelayedCallbackStartTime.remove(animationFrameCallback);
        int iIndexOf = this.mAnimationCallbacks.indexOf(animationFrameCallback);
        if (iIndexOf >= 0) {
            this.mAnimationCallbacks.set(iIndexOf, null);
            this.mListDirty = true;
        }
    }

    private void doAnimationFrame(long j) {
        long jUptimeMillis = SystemClock.uptimeMillis();
        int size = this.mAnimationCallbacks.size();
        for (int i = 0; i < size; i++) {
            final AnimationFrameCallback animationFrameCallback = this.mAnimationCallbacks.get(i);
            if (animationFrameCallback != null && isCallbackDue(animationFrameCallback, jUptimeMillis)) {
                animationFrameCallback.doAnimationFrame(j);
                if (this.mCommitCallbacks.contains(animationFrameCallback)) {
                    getProvider().postCommitCallback(new Runnable() {
                        @Override
                        public void run() {
                            AnimationHandler.this.commitAnimationFrame(animationFrameCallback, AnimationHandler.this.getProvider().getFrameTime());
                        }
                    });
                }
            }
        }
        cleanUpList();
    }

    private void commitAnimationFrame(AnimationFrameCallback animationFrameCallback, long j) {
        if (!this.mDelayedCallbackStartTime.containsKey(animationFrameCallback) && this.mCommitCallbacks.contains(animationFrameCallback)) {
            animationFrameCallback.commitAnimationFrame(j);
            this.mCommitCallbacks.remove(animationFrameCallback);
        }
    }

    private boolean isCallbackDue(AnimationFrameCallback animationFrameCallback, long j) {
        Long l = this.mDelayedCallbackStartTime.get(animationFrameCallback);
        if (l == null) {
            return true;
        }
        if (l.longValue() < j) {
            this.mDelayedCallbackStartTime.remove(animationFrameCallback);
            return true;
        }
        return false;
    }

    public static int getAnimationCount() {
        AnimationHandler animationHandler = sAnimatorHandler.get();
        if (animationHandler == null) {
            return 0;
        }
        return animationHandler.getCallbackSize();
    }

    public static void setFrameDelay(long j) {
        getInstance().getProvider().setFrameDelay(j);
    }

    public static long getFrameDelay() {
        return getInstance().getProvider().getFrameDelay();
    }

    void autoCancelBasedOn(ObjectAnimator objectAnimator) {
        for (int size = this.mAnimationCallbacks.size() - 1; size >= 0; size--) {
            AnimationFrameCallback animationFrameCallback = this.mAnimationCallbacks.get(size);
            if (animationFrameCallback != null && objectAnimator.shouldAutoCancel(animationFrameCallback)) {
                ((Animator) this.mAnimationCallbacks.get(size)).cancel();
            }
        }
    }

    private void cleanUpList() {
        if (this.mListDirty) {
            for (int size = this.mAnimationCallbacks.size() - 1; size >= 0; size--) {
                if (this.mAnimationCallbacks.get(size) == null) {
                    this.mAnimationCallbacks.remove(size);
                }
            }
            this.mListDirty = false;
        }
    }

    private int getCallbackSize() {
        int i = 0;
        for (int size = this.mAnimationCallbacks.size() - 1; size >= 0; size--) {
            if (this.mAnimationCallbacks.get(size) != null) {
                i++;
            }
        }
        return i;
    }

    private class MyFrameCallbackProvider implements AnimationFrameCallbackProvider {
        final Choreographer mChoreographer;

        private MyFrameCallbackProvider() {
            this.mChoreographer = Choreographer.getInstance();
        }

        @Override
        public void postFrameCallback(Choreographer.FrameCallback frameCallback) {
            this.mChoreographer.postFrameCallback(frameCallback);
        }

        @Override
        public void postCommitCallback(Runnable runnable) {
            this.mChoreographer.postCallback(3, runnable, null);
        }

        @Override
        public long getFrameTime() {
            return this.mChoreographer.getFrameTime();
        }

        @Override
        public long getFrameDelay() {
            return Choreographer.getFrameDelay();
        }

        @Override
        public void setFrameDelay(long j) {
            Choreographer.setFrameDelay(j);
        }
    }
}
