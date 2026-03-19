package android.animation;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LayoutTransition {
    public static final int APPEARING = 2;
    public static final int CHANGE_APPEARING = 0;
    public static final int CHANGE_DISAPPEARING = 1;
    public static final int CHANGING = 4;
    public static final int DISAPPEARING = 3;
    private static final int FLAG_APPEARING = 1;
    private static final int FLAG_CHANGE_APPEARING = 4;
    private static final int FLAG_CHANGE_DISAPPEARING = 8;
    private static final int FLAG_CHANGING = 16;
    private static final int FLAG_DISAPPEARING = 2;
    private static ObjectAnimator defaultChange;
    private static ObjectAnimator defaultChangeIn;
    private static ObjectAnimator defaultChangeOut;
    private static ObjectAnimator defaultFadeIn;
    private static ObjectAnimator defaultFadeOut;
    private Animator mAppearingAnim;
    private Animator mChangingAnim;
    private Animator mChangingAppearingAnim;
    private Animator mChangingDisappearingAnim;
    private Animator mDisappearingAnim;
    private ArrayList<TransitionListener> mListeners;
    private long staggerDelay;
    private static long DEFAULT_DURATION = 300;
    private static TimeInterpolator ACCEL_DECEL_INTERPOLATOR = new AccelerateDecelerateInterpolator();
    private static TimeInterpolator DECEL_INTERPOLATOR = new DecelerateInterpolator();
    private static TimeInterpolator sAppearingInterpolator = ACCEL_DECEL_INTERPOLATOR;
    private static TimeInterpolator sDisappearingInterpolator = ACCEL_DECEL_INTERPOLATOR;
    private static TimeInterpolator sChangingAppearingInterpolator = DECEL_INTERPOLATOR;
    private static TimeInterpolator sChangingDisappearingInterpolator = DECEL_INTERPOLATOR;
    private static TimeInterpolator sChangingInterpolator = DECEL_INTERPOLATOR;
    private long mChangingAppearingDuration = DEFAULT_DURATION;
    private long mChangingDisappearingDuration = DEFAULT_DURATION;
    private long mChangingDuration = DEFAULT_DURATION;
    private long mAppearingDuration = DEFAULT_DURATION;
    private long mDisappearingDuration = DEFAULT_DURATION;
    private long mAppearingDelay = DEFAULT_DURATION;
    private long mDisappearingDelay = 0;
    private long mChangingAppearingDelay = 0;
    private long mChangingDisappearingDelay = DEFAULT_DURATION;
    private long mChangingDelay = 0;
    private long mChangingAppearingStagger = 0;
    private long mChangingDisappearingStagger = 0;
    private long mChangingStagger = 0;
    private TimeInterpolator mAppearingInterpolator = sAppearingInterpolator;
    private TimeInterpolator mDisappearingInterpolator = sDisappearingInterpolator;
    private TimeInterpolator mChangingAppearingInterpolator = sChangingAppearingInterpolator;
    private TimeInterpolator mChangingDisappearingInterpolator = sChangingDisappearingInterpolator;
    private TimeInterpolator mChangingInterpolator = sChangingInterpolator;
    private final HashMap<View, Animator> pendingAnimations = new HashMap<>();
    private final LinkedHashMap<View, Animator> currentChangingAnimations = new LinkedHashMap<>();
    private final LinkedHashMap<View, Animator> currentAppearingAnimations = new LinkedHashMap<>();
    private final LinkedHashMap<View, Animator> currentDisappearingAnimations = new LinkedHashMap<>();
    private final HashMap<View, View.OnLayoutChangeListener> layoutChangeListenerMap = new HashMap<>();
    private int mTransitionTypes = 15;
    private boolean mAnimateParentHierarchy = true;

    public interface TransitionListener {
        void endTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int i);

        void startTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int i);
    }

    static long access$214(LayoutTransition layoutTransition, long j) {
        long j2 = layoutTransition.staggerDelay + j;
        layoutTransition.staggerDelay = j2;
        return j2;
    }

    public LayoutTransition() {
        this.mDisappearingAnim = null;
        this.mAppearingAnim = null;
        this.mChangingAppearingAnim = null;
        this.mChangingDisappearingAnim = null;
        this.mChangingAnim = null;
        if (defaultChangeIn == null) {
            defaultChangeIn = ObjectAnimator.ofPropertyValuesHolder(null, PropertyValuesHolder.ofInt("left", 0, 1), PropertyValuesHolder.ofInt("top", 0, 1), PropertyValuesHolder.ofInt("right", 0, 1), PropertyValuesHolder.ofInt("bottom", 0, 1), PropertyValuesHolder.ofInt("scrollX", 0, 1), PropertyValuesHolder.ofInt("scrollY", 0, 1));
            defaultChangeIn.setDuration(DEFAULT_DURATION);
            defaultChangeIn.setStartDelay(this.mChangingAppearingDelay);
            defaultChangeIn.setInterpolator(this.mChangingAppearingInterpolator);
            defaultChangeOut = defaultChangeIn.mo0clone();
            defaultChangeOut.setStartDelay(this.mChangingDisappearingDelay);
            defaultChangeOut.setInterpolator(this.mChangingDisappearingInterpolator);
            defaultChange = defaultChangeIn.mo0clone();
            defaultChange.setStartDelay(this.mChangingDelay);
            defaultChange.setInterpolator(this.mChangingInterpolator);
            defaultFadeIn = ObjectAnimator.ofFloat((Object) null, "alpha", 0.0f, 1.0f);
            defaultFadeIn.setDuration(DEFAULT_DURATION);
            defaultFadeIn.setStartDelay(this.mAppearingDelay);
            defaultFadeIn.setInterpolator(this.mAppearingInterpolator);
            defaultFadeOut = ObjectAnimator.ofFloat((Object) null, "alpha", 1.0f, 0.0f);
            defaultFadeOut.setDuration(DEFAULT_DURATION);
            defaultFadeOut.setStartDelay(this.mDisappearingDelay);
            defaultFadeOut.setInterpolator(this.mDisappearingInterpolator);
        }
        this.mChangingAppearingAnim = defaultChangeIn;
        this.mChangingDisappearingAnim = defaultChangeOut;
        this.mChangingAnim = defaultChange;
        this.mAppearingAnim = defaultFadeIn;
        this.mDisappearingAnim = defaultFadeOut;
    }

    public void setDuration(long j) {
        this.mChangingAppearingDuration = j;
        this.mChangingDisappearingDuration = j;
        this.mChangingDuration = j;
        this.mAppearingDuration = j;
        this.mDisappearingDuration = j;
    }

    public void enableTransitionType(int i) {
        switch (i) {
            case 0:
                this.mTransitionTypes |= 4;
                break;
            case 1:
                this.mTransitionTypes |= 8;
                break;
            case 2:
                this.mTransitionTypes |= 1;
                break;
            case 3:
                this.mTransitionTypes |= 2;
                break;
            case 4:
                this.mTransitionTypes |= 16;
                break;
        }
    }

    public void disableTransitionType(int i) {
        switch (i) {
            case 0:
                this.mTransitionTypes &= -5;
                break;
            case 1:
                this.mTransitionTypes &= -9;
                break;
            case 2:
                this.mTransitionTypes &= -2;
                break;
            case 3:
                this.mTransitionTypes &= -3;
                break;
            case 4:
                this.mTransitionTypes &= -17;
                break;
        }
    }

    public boolean isTransitionTypeEnabled(int i) {
        switch (i) {
            case 0:
                return (this.mTransitionTypes & 4) == 4;
            case 1:
                return (this.mTransitionTypes & 8) == 8;
            case 2:
                return (this.mTransitionTypes & 1) == 1;
            case 3:
                return (this.mTransitionTypes & 2) == 2;
            case 4:
                return (this.mTransitionTypes & 16) == 16;
            default:
                return false;
        }
    }

    public void setStartDelay(int i, long j) {
        switch (i) {
            case 0:
                this.mChangingAppearingDelay = j;
                break;
            case 1:
                this.mChangingDisappearingDelay = j;
                break;
            case 2:
                this.mAppearingDelay = j;
                break;
            case 3:
                this.mDisappearingDelay = j;
                break;
            case 4:
                this.mChangingDelay = j;
                break;
        }
    }

    public long getStartDelay(int i) {
        switch (i) {
            case 0:
                return this.mChangingAppearingDelay;
            case 1:
                return this.mChangingDisappearingDelay;
            case 2:
                return this.mAppearingDelay;
            case 3:
                return this.mDisappearingDelay;
            case 4:
                return this.mChangingDelay;
            default:
                return 0L;
        }
    }

    public void setDuration(int i, long j) {
        switch (i) {
            case 0:
                this.mChangingAppearingDuration = j;
                break;
            case 1:
                this.mChangingDisappearingDuration = j;
                break;
            case 2:
                this.mAppearingDuration = j;
                break;
            case 3:
                this.mDisappearingDuration = j;
                break;
            case 4:
                this.mChangingDuration = j;
                break;
        }
    }

    public long getDuration(int i) {
        switch (i) {
            case 0:
                return this.mChangingAppearingDuration;
            case 1:
                return this.mChangingDisappearingDuration;
            case 2:
                return this.mAppearingDuration;
            case 3:
                return this.mDisappearingDuration;
            case 4:
                return this.mChangingDuration;
            default:
                return 0L;
        }
    }

    public void setStagger(int i, long j) {
        if (i != 4) {
            switch (i) {
                case 0:
                    this.mChangingAppearingStagger = j;
                    break;
                case 1:
                    this.mChangingDisappearingStagger = j;
                    break;
            }
        }
        this.mChangingStagger = j;
    }

    public long getStagger(int i) {
        if (i != 4) {
            switch (i) {
                case 0:
                    return this.mChangingAppearingStagger;
                case 1:
                    return this.mChangingDisappearingStagger;
                default:
                    return 0L;
            }
        }
        return this.mChangingStagger;
    }

    public void setInterpolator(int i, TimeInterpolator timeInterpolator) {
        switch (i) {
            case 0:
                this.mChangingAppearingInterpolator = timeInterpolator;
                break;
            case 1:
                this.mChangingDisappearingInterpolator = timeInterpolator;
                break;
            case 2:
                this.mAppearingInterpolator = timeInterpolator;
                break;
            case 3:
                this.mDisappearingInterpolator = timeInterpolator;
                break;
            case 4:
                this.mChangingInterpolator = timeInterpolator;
                break;
        }
    }

    public TimeInterpolator getInterpolator(int i) {
        switch (i) {
            case 0:
                return this.mChangingAppearingInterpolator;
            case 1:
                return this.mChangingDisappearingInterpolator;
            case 2:
                return this.mAppearingInterpolator;
            case 3:
                return this.mDisappearingInterpolator;
            case 4:
                return this.mChangingInterpolator;
            default:
                return null;
        }
    }

    public void setAnimator(int i, Animator animator) {
        switch (i) {
            case 0:
                this.mChangingAppearingAnim = animator;
                break;
            case 1:
                this.mChangingDisappearingAnim = animator;
                break;
            case 2:
                this.mAppearingAnim = animator;
                break;
            case 3:
                this.mDisappearingAnim = animator;
                break;
            case 4:
                this.mChangingAnim = animator;
                break;
        }
    }

    public Animator getAnimator(int i) {
        switch (i) {
            case 0:
                return this.mChangingAppearingAnim;
            case 1:
                return this.mChangingDisappearingAnim;
            case 2:
                return this.mAppearingAnim;
            case 3:
                return this.mDisappearingAnim;
            case 4:
                return this.mChangingAnim;
            default:
                return null;
        }
    }

    private void runChangeTransition(ViewGroup viewGroup, View view, int i) {
        Animator animator;
        long j;
        Animator animator2;
        Animator animator3;
        long j2;
        Animator animator4;
        int i2;
        int i3;
        switch (i) {
            case 2:
                animator = this.mChangingAppearingAnim;
                j = this.mChangingAppearingDuration;
                animator2 = defaultChangeIn;
                animator3 = animator;
                j2 = j;
                animator4 = animator2;
                break;
            case 3:
                animator = this.mChangingDisappearingAnim;
                j = this.mChangingDisappearingDuration;
                animator2 = defaultChangeOut;
                animator3 = animator;
                j2 = j;
                animator4 = animator2;
                break;
            case 4:
                animator = this.mChangingAnim;
                j = this.mChangingDuration;
                animator2 = defaultChange;
                animator3 = animator;
                j2 = j;
                animator4 = animator2;
                break;
            default:
                j2 = 0;
                animator3 = null;
                animator4 = null;
                break;
        }
        if (animator3 == null) {
            return;
        }
        this.staggerDelay = 0L;
        ViewTreeObserver viewTreeObserver = viewGroup.getViewTreeObserver();
        if (!viewTreeObserver.isAlive()) {
            return;
        }
        int childCount = viewGroup.getChildCount();
        int i4 = 0;
        while (i4 < childCount) {
            View childAt = viewGroup.getChildAt(i4);
            if (childAt == view) {
                i2 = i4;
                i3 = childCount;
            } else {
                i2 = i4;
                i3 = childCount;
                setupChangeAnimation(viewGroup, i, animator3, j2, childAt);
            }
            i4 = i2 + 1;
            childCount = i3;
        }
        if (this.mAnimateParentHierarchy) {
            ViewGroup viewGroup2 = viewGroup;
            while (viewGroup2 != null) {
                ViewParent parent = viewGroup2.getParent();
                if (parent instanceof ViewGroup) {
                    ViewGroup viewGroup3 = (ViewGroup) parent;
                    setupChangeAnimation(viewGroup3, i, animator4, j2, viewGroup2);
                    viewGroup2 = viewGroup3;
                } else {
                    viewGroup2 = null;
                }
            }
        }
        CleanupCallback cleanupCallback = new CleanupCallback(this.layoutChangeListenerMap, viewGroup);
        viewTreeObserver.addOnPreDrawListener(cleanupCallback);
        viewGroup.addOnAttachStateChangeListener(cleanupCallback);
    }

    public void setAnimateParentHierarchy(boolean z) {
        this.mAnimateParentHierarchy = z;
    }

    private void setupChangeAnimation(final ViewGroup viewGroup, final int i, Animator animator, final long j, final View view) {
        if (this.layoutChangeListenerMap.get(view) != null) {
            return;
        }
        if (view.getWidth() == 0 && view.getHeight() == 0) {
            return;
        }
        final Animator animatorMo0clone = animator.mo0clone();
        animatorMo0clone.setTarget(view);
        animatorMo0clone.setupStartValues();
        Animator animator2 = this.pendingAnimations.get(view);
        if (animator2 != null) {
            animator2.cancel();
            this.pendingAnimations.remove(view);
        }
        this.pendingAnimations.put(view, animatorMo0clone);
        ValueAnimator duration = ValueAnimator.ofFloat(0.0f, 1.0f).setDuration(100 + j);
        duration.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator3) {
                LayoutTransition.this.pendingAnimations.remove(view);
            }
        });
        duration.start();
        final View.OnLayoutChangeListener onLayoutChangeListener = new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view2, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9) {
                animatorMo0clone.setupEndValues();
                if (animatorMo0clone instanceof ValueAnimator) {
                    boolean z = false;
                    for (PropertyValuesHolder propertyValuesHolder : ((ValueAnimator) animatorMo0clone).getValues()) {
                        if (propertyValuesHolder.mKeyframes instanceof KeyframeSet) {
                            KeyframeSet keyframeSet = (KeyframeSet) propertyValuesHolder.mKeyframes;
                            if (keyframeSet.mFirstKeyframe == null || keyframeSet.mLastKeyframe == null || !keyframeSet.mFirstKeyframe.getValue().equals(keyframeSet.mLastKeyframe.getValue())) {
                                z = true;
                            }
                        } else if (!propertyValuesHolder.mKeyframes.getValue(0.0f).equals(propertyValuesHolder.mKeyframes.getValue(1.0f))) {
                            z = true;
                        }
                    }
                    if (!z) {
                        return;
                    }
                }
                long j2 = 0;
                switch (i) {
                    case 2:
                        j2 = LayoutTransition.this.mChangingAppearingDelay + LayoutTransition.this.staggerDelay;
                        LayoutTransition.access$214(LayoutTransition.this, LayoutTransition.this.mChangingAppearingStagger);
                        if (LayoutTransition.this.mChangingAppearingInterpolator != LayoutTransition.sChangingAppearingInterpolator) {
                            animatorMo0clone.setInterpolator(LayoutTransition.this.mChangingAppearingInterpolator);
                        }
                        break;
                    case 3:
                        j2 = LayoutTransition.this.mChangingDisappearingDelay + LayoutTransition.this.staggerDelay;
                        LayoutTransition.access$214(LayoutTransition.this, LayoutTransition.this.mChangingDisappearingStagger);
                        if (LayoutTransition.this.mChangingDisappearingInterpolator != LayoutTransition.sChangingDisappearingInterpolator) {
                            animatorMo0clone.setInterpolator(LayoutTransition.this.mChangingDisappearingInterpolator);
                        }
                        break;
                    case 4:
                        j2 = LayoutTransition.this.mChangingDelay + LayoutTransition.this.staggerDelay;
                        LayoutTransition.access$214(LayoutTransition.this, LayoutTransition.this.mChangingStagger);
                        if (LayoutTransition.this.mChangingInterpolator != LayoutTransition.sChangingInterpolator) {
                            animatorMo0clone.setInterpolator(LayoutTransition.this.mChangingInterpolator);
                        }
                        break;
                }
                animatorMo0clone.setStartDelay(j2);
                animatorMo0clone.setDuration(j);
                Animator animator3 = (Animator) LayoutTransition.this.currentChangingAnimations.get(view);
                if (animator3 != null) {
                    animator3.cancel();
                }
                if (((Animator) LayoutTransition.this.pendingAnimations.get(view)) != null) {
                    LayoutTransition.this.pendingAnimations.remove(view);
                }
                LayoutTransition.this.currentChangingAnimations.put(view, animatorMo0clone);
                viewGroup.requestTransitionStart(LayoutTransition.this);
                view.removeOnLayoutChangeListener(this);
                LayoutTransition.this.layoutChangeListenerMap.remove(view);
            }
        };
        animatorMo0clone.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator3) {
                int i2;
                if (LayoutTransition.this.hasListeners()) {
                    for (TransitionListener transitionListener : (ArrayList) LayoutTransition.this.mListeners.clone()) {
                        LayoutTransition layoutTransition = LayoutTransition.this;
                        ViewGroup viewGroup2 = viewGroup;
                        View view2 = view;
                        if (i == 2) {
                            i2 = 0;
                        } else {
                            i2 = i == 3 ? 1 : 4;
                        }
                        transitionListener.startTransition(layoutTransition, viewGroup2, view2, i2);
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animator3) {
                view.removeOnLayoutChangeListener(onLayoutChangeListener);
                LayoutTransition.this.layoutChangeListenerMap.remove(view);
            }

            @Override
            public void onAnimationEnd(Animator animator3) {
                int i2;
                LayoutTransition.this.currentChangingAnimations.remove(view);
                if (LayoutTransition.this.hasListeners()) {
                    for (TransitionListener transitionListener : (ArrayList) LayoutTransition.this.mListeners.clone()) {
                        LayoutTransition layoutTransition = LayoutTransition.this;
                        ViewGroup viewGroup2 = viewGroup;
                        View view2 = view;
                        if (i == 2) {
                            i2 = 0;
                        } else {
                            i2 = i == 3 ? 1 : 4;
                        }
                        transitionListener.endTransition(layoutTransition, viewGroup2, view2, i2);
                    }
                }
            }
        });
        view.addOnLayoutChangeListener(onLayoutChangeListener);
        this.layoutChangeListenerMap.put(view, onLayoutChangeListener);
    }

    public void startChangingAnimations() {
        for (Animator animator : ((LinkedHashMap) this.currentChangingAnimations.clone()).values()) {
            if (animator instanceof ObjectAnimator) {
                ((ObjectAnimator) animator).setCurrentPlayTime(0L);
            }
            animator.start();
        }
    }

    public void endChangingAnimations() {
        for (Animator animator : ((LinkedHashMap) this.currentChangingAnimations.clone()).values()) {
            animator.start();
            animator.end();
        }
        this.currentChangingAnimations.clear();
    }

    public boolean isChangingLayout() {
        return this.currentChangingAnimations.size() > 0;
    }

    public boolean isRunning() {
        return this.currentChangingAnimations.size() > 0 || this.currentAppearingAnimations.size() > 0 || this.currentDisappearingAnimations.size() > 0;
    }

    public void cancel() {
        if (this.currentChangingAnimations.size() > 0) {
            Iterator it = ((LinkedHashMap) this.currentChangingAnimations.clone()).values().iterator();
            while (it.hasNext()) {
                ((Animator) it.next()).cancel();
            }
            this.currentChangingAnimations.clear();
        }
        if (this.currentAppearingAnimations.size() > 0) {
            Iterator it2 = ((LinkedHashMap) this.currentAppearingAnimations.clone()).values().iterator();
            while (it2.hasNext()) {
                ((Animator) it2.next()).end();
            }
            this.currentAppearingAnimations.clear();
        }
        if (this.currentDisappearingAnimations.size() > 0) {
            Iterator it3 = ((LinkedHashMap) this.currentDisappearingAnimations.clone()).values().iterator();
            while (it3.hasNext()) {
                ((Animator) it3.next()).end();
            }
            this.currentDisappearingAnimations.clear();
        }
    }

    public void cancel(int i) {
        switch (i) {
            case 0:
            case 1:
            case 4:
                if (this.currentChangingAnimations.size() > 0) {
                    Iterator it = ((LinkedHashMap) this.currentChangingAnimations.clone()).values().iterator();
                    while (it.hasNext()) {
                        ((Animator) it.next()).cancel();
                    }
                    this.currentChangingAnimations.clear();
                }
                break;
            case 2:
                if (this.currentAppearingAnimations.size() > 0) {
                    Iterator it2 = ((LinkedHashMap) this.currentAppearingAnimations.clone()).values().iterator();
                    while (it2.hasNext()) {
                        ((Animator) it2.next()).end();
                    }
                    this.currentAppearingAnimations.clear();
                }
                break;
            case 3:
                if (this.currentDisappearingAnimations.size() > 0) {
                    Iterator it3 = ((LinkedHashMap) this.currentDisappearingAnimations.clone()).values().iterator();
                    while (it3.hasNext()) {
                        ((Animator) it3.next()).end();
                    }
                    this.currentDisappearingAnimations.clear();
                }
                break;
        }
    }

    private void runAppearingTransition(final ViewGroup viewGroup, final View view) {
        Animator animator = this.currentDisappearingAnimations.get(view);
        if (animator != null) {
            animator.cancel();
        }
        if (this.mAppearingAnim == null) {
            if (hasListeners()) {
                Iterator it = ((ArrayList) this.mListeners.clone()).iterator();
                while (it.hasNext()) {
                    ((TransitionListener) it.next()).endTransition(this, viewGroup, view, 2);
                }
                return;
            }
            return;
        }
        Animator animatorMo0clone = this.mAppearingAnim.mo0clone();
        animatorMo0clone.setTarget(view);
        animatorMo0clone.setStartDelay(this.mAppearingDelay);
        animatorMo0clone.setDuration(this.mAppearingDuration);
        if (this.mAppearingInterpolator != sAppearingInterpolator) {
            animatorMo0clone.setInterpolator(this.mAppearingInterpolator);
        }
        if (animatorMo0clone instanceof ObjectAnimator) {
            ((ObjectAnimator) animatorMo0clone).setCurrentPlayTime(0L);
        }
        animatorMo0clone.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator2) {
                LayoutTransition.this.currentAppearingAnimations.remove(view);
                if (LayoutTransition.this.hasListeners()) {
                    Iterator it2 = ((ArrayList) LayoutTransition.this.mListeners.clone()).iterator();
                    while (it2.hasNext()) {
                        ((TransitionListener) it2.next()).endTransition(LayoutTransition.this, viewGroup, view, 2);
                    }
                }
            }
        });
        this.currentAppearingAnimations.put(view, animatorMo0clone);
        animatorMo0clone.start();
    }

    private void runDisappearingTransition(final ViewGroup viewGroup, final View view) {
        Animator animator = this.currentAppearingAnimations.get(view);
        if (animator != null) {
            animator.cancel();
        }
        if (this.mDisappearingAnim == null) {
            if (hasListeners()) {
                Iterator it = ((ArrayList) this.mListeners.clone()).iterator();
                while (it.hasNext()) {
                    ((TransitionListener) it.next()).endTransition(this, viewGroup, view, 3);
                }
                return;
            }
            return;
        }
        Animator animatorMo0clone = this.mDisappearingAnim.mo0clone();
        animatorMo0clone.setStartDelay(this.mDisappearingDelay);
        animatorMo0clone.setDuration(this.mDisappearingDuration);
        if (this.mDisappearingInterpolator != sDisappearingInterpolator) {
            animatorMo0clone.setInterpolator(this.mDisappearingInterpolator);
        }
        animatorMo0clone.setTarget(view);
        final float alpha = view.getAlpha();
        animatorMo0clone.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator2) {
                LayoutTransition.this.currentDisappearingAnimations.remove(view);
                view.setAlpha(alpha);
                if (LayoutTransition.this.hasListeners()) {
                    Iterator it2 = ((ArrayList) LayoutTransition.this.mListeners.clone()).iterator();
                    while (it2.hasNext()) {
                        ((TransitionListener) it2.next()).endTransition(LayoutTransition.this, viewGroup, view, 3);
                    }
                }
            }
        });
        if (animatorMo0clone instanceof ObjectAnimator) {
            ((ObjectAnimator) animatorMo0clone).setCurrentPlayTime(0L);
        }
        this.currentDisappearingAnimations.put(view, animatorMo0clone);
        animatorMo0clone.start();
    }

    private void addChild(ViewGroup viewGroup, View view, boolean z) {
        if (viewGroup.getWindowVisibility() != 0) {
            return;
        }
        if ((this.mTransitionTypes & 1) == 1) {
            cancel(3);
        }
        if (z && (this.mTransitionTypes & 4) == 4) {
            cancel(0);
            cancel(4);
        }
        if (hasListeners() && (this.mTransitionTypes & 1) == 1) {
            Iterator it = ((ArrayList) this.mListeners.clone()).iterator();
            while (it.hasNext()) {
                ((TransitionListener) it.next()).startTransition(this, viewGroup, view, 2);
            }
        }
        if (z && (this.mTransitionTypes & 4) == 4) {
            runChangeTransition(viewGroup, view, 2);
        }
        if ((this.mTransitionTypes & 1) == 1) {
            runAppearingTransition(viewGroup, view);
        }
    }

    private boolean hasListeners() {
        return this.mListeners != null && this.mListeners.size() > 0;
    }

    public void layoutChange(ViewGroup viewGroup) {
        if (viewGroup.getWindowVisibility() == 0 && (this.mTransitionTypes & 16) == 16 && !isRunning()) {
            runChangeTransition(viewGroup, null, 4);
        }
    }

    public void addChild(ViewGroup viewGroup, View view) {
        addChild(viewGroup, view, true);
    }

    @Deprecated
    public void showChild(ViewGroup viewGroup, View view) {
        addChild(viewGroup, view, true);
    }

    public void showChild(ViewGroup viewGroup, View view, int i) {
        addChild(viewGroup, view, i == 8);
    }

    private void removeChild(ViewGroup viewGroup, View view, boolean z) {
        if (viewGroup.getWindowVisibility() != 0) {
            return;
        }
        if ((this.mTransitionTypes & 2) == 2) {
            cancel(2);
        }
        if (z && (this.mTransitionTypes & 8) == 8) {
            cancel(1);
            cancel(4);
        }
        if (hasListeners() && (this.mTransitionTypes & 2) == 2) {
            Iterator it = ((ArrayList) this.mListeners.clone()).iterator();
            while (it.hasNext()) {
                ((TransitionListener) it.next()).startTransition(this, viewGroup, view, 3);
            }
        }
        if (z && (this.mTransitionTypes & 8) == 8) {
            runChangeTransition(viewGroup, view, 3);
        }
        if ((this.mTransitionTypes & 2) == 2) {
            runDisappearingTransition(viewGroup, view);
        }
    }

    public void removeChild(ViewGroup viewGroup, View view) {
        removeChild(viewGroup, view, true);
    }

    @Deprecated
    public void hideChild(ViewGroup viewGroup, View view) {
        removeChild(viewGroup, view, true);
    }

    public void hideChild(ViewGroup viewGroup, View view, int i) {
        removeChild(viewGroup, view, i == 8);
    }

    public void addTransitionListener(TransitionListener transitionListener) {
        if (this.mListeners == null) {
            this.mListeners = new ArrayList<>();
        }
        this.mListeners.add(transitionListener);
    }

    public void removeTransitionListener(TransitionListener transitionListener) {
        if (this.mListeners == null) {
            return;
        }
        this.mListeners.remove(transitionListener);
    }

    public List<TransitionListener> getTransitionListeners() {
        return this.mListeners;
    }

    private static final class CleanupCallback implements ViewTreeObserver.OnPreDrawListener, View.OnAttachStateChangeListener {
        final Map<View, View.OnLayoutChangeListener> layoutChangeListenerMap;
        final ViewGroup parent;

        CleanupCallback(Map<View, View.OnLayoutChangeListener> map, ViewGroup viewGroup) {
            this.layoutChangeListenerMap = map;
            this.parent = viewGroup;
        }

        private void cleanup() {
            this.parent.getViewTreeObserver().removeOnPreDrawListener(this);
            this.parent.removeOnAttachStateChangeListener(this);
            if (this.layoutChangeListenerMap.size() > 0) {
                for (View view : this.layoutChangeListenerMap.keySet()) {
                    view.removeOnLayoutChangeListener(this.layoutChangeListenerMap.get(view));
                }
                this.layoutChangeListenerMap.clear();
            }
        }

        @Override
        public void onViewAttachedToWindow(View view) {
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
            cleanup();
        }

        @Override
        public boolean onPreDraw() {
            cleanup();
            return true;
        }
    }
}
