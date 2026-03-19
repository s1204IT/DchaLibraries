package android.animation;

import android.content.res.ConstantState;
import java.util.ArrayList;

public abstract class Animator implements Cloneable {
    public static final long DURATION_INFINITE = -1;
    private AnimatorConstantState mConstantState;
    ArrayList<AnimatorListener> mListeners = null;
    ArrayList<AnimatorPauseListener> mPauseListeners = null;
    boolean mPaused = false;
    int mChangingConfigurations = 0;

    public interface AnimatorListener {
        void onAnimationCancel(Animator animator);

        void onAnimationEnd(Animator animator);

        void onAnimationRepeat(Animator animator);

        void onAnimationStart(Animator animator);
    }

    public interface AnimatorPauseListener {
        void onAnimationPause(Animator animator);

        void onAnimationResume(Animator animator);
    }

    public abstract long getDuration();

    public abstract long getStartDelay();

    public abstract boolean isRunning();

    public abstract Animator setDuration(long j);

    public abstract void setInterpolator(TimeInterpolator timeInterpolator);

    public abstract void setStartDelay(long j);

    public void start() {
    }

    public void cancel() {
    }

    public void end() {
    }

    public void pause() {
        if (!isStarted() || this.mPaused) {
            return;
        }
        this.mPaused = true;
        if (this.mPauseListeners == null) {
            return;
        }
        ArrayList<AnimatorPauseListener> tmpListeners = (ArrayList) this.mPauseListeners.clone();
        int numListeners = tmpListeners.size();
        for (int i = 0; i < numListeners; i++) {
            tmpListeners.get(i).onAnimationPause(this);
        }
    }

    public void resume() {
        if (!this.mPaused) {
            return;
        }
        this.mPaused = false;
        if (this.mPauseListeners == null) {
            return;
        }
        ArrayList<AnimatorPauseListener> tmpListeners = (ArrayList) this.mPauseListeners.clone();
        int numListeners = tmpListeners.size();
        for (int i = 0; i < numListeners; i++) {
            tmpListeners.get(i).onAnimationResume(this);
        }
    }

    public boolean isPaused() {
        return this.mPaused;
    }

    public long getTotalDuration() {
        long duration = getDuration();
        if (duration == -1) {
            return -1L;
        }
        return getStartDelay() + duration;
    }

    public TimeInterpolator getInterpolator() {
        return null;
    }

    public boolean isStarted() {
        return isRunning();
    }

    public void addListener(AnimatorListener listener) {
        if (this.mListeners == null) {
            this.mListeners = new ArrayList<>();
        }
        this.mListeners.add(listener);
    }

    public void removeListener(AnimatorListener listener) {
        if (this.mListeners == null) {
            return;
        }
        this.mListeners.remove(listener);
        if (this.mListeners.size() != 0) {
            return;
        }
        this.mListeners = null;
    }

    public ArrayList<AnimatorListener> getListeners() {
        return this.mListeners;
    }

    public void addPauseListener(AnimatorPauseListener listener) {
        if (this.mPauseListeners == null) {
            this.mPauseListeners = new ArrayList<>();
        }
        this.mPauseListeners.add(listener);
    }

    public void removePauseListener(AnimatorPauseListener listener) {
        if (this.mPauseListeners == null) {
            return;
        }
        this.mPauseListeners.remove(listener);
        if (this.mPauseListeners.size() != 0) {
            return;
        }
        this.mPauseListeners = null;
    }

    public void removeAllListeners() {
        if (this.mListeners != null) {
            this.mListeners.clear();
            this.mListeners = null;
        }
        if (this.mPauseListeners == null) {
            return;
        }
        this.mPauseListeners.clear();
        this.mPauseListeners = null;
    }

    public int getChangingConfigurations() {
        return this.mChangingConfigurations;
    }

    public void setChangingConfigurations(int configs) {
        this.mChangingConfigurations = configs;
    }

    public void appendChangingConfigurations(int configs) {
        this.mChangingConfigurations |= configs;
    }

    public ConstantState<Animator> createConstantState() {
        return new AnimatorConstantState(this);
    }

    public Animator m35clone() {
        try {
            Animator anim = (Animator) super.clone();
            if (this.mListeners != null) {
                anim.mListeners = new ArrayList<>(this.mListeners);
            }
            if (this.mPauseListeners != null) {
                anim.mPauseListeners = new ArrayList<>(this.mPauseListeners);
            }
            return anim;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public void setupStartValues() {
    }

    public void setupEndValues() {
    }

    public void setTarget(Object target) {
    }

    public boolean canReverse() {
        return false;
    }

    public void reverse() {
        throw new IllegalStateException("Reverse is not supported");
    }

    public void setAllowRunningAsynchronously(boolean mayRunAsync) {
    }

    private static class AnimatorConstantState extends ConstantState<Animator> {
        final Animator mAnimator;
        int mChangingConf;

        public AnimatorConstantState(Animator animator) {
            this.mAnimator = animator;
            this.mAnimator.mConstantState = this;
            this.mChangingConf = this.mAnimator.getChangingConfigurations();
        }

        @Override
        public int getChangingConfigurations() {
            return this.mChangingConf;
        }

        @Override
        public Animator newInstance() {
            Animator clone = this.mAnimator.m35clone();
            clone.mConstantState = this;
            return clone;
        }
    }
}
