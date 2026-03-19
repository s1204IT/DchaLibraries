package android.transition;

import android.animation.Animator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.R;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class Visibility extends Transition {
    public static final int MODE_IN = 1;
    public static final int MODE_OUT = 2;
    private static final String PROPNAME_SCREEN_LOCATION = "android:visibility:screenLocation";
    private int mMode;
    private boolean mSuppressLayout;
    static final String PROPNAME_VISIBILITY = "android:visibility:visibility";
    private static final String PROPNAME_PARENT = "android:visibility:parent";
    private static final String[] sTransitionProperties = {PROPNAME_VISIBILITY, PROPNAME_PARENT};

    @Retention(RetentionPolicy.SOURCE)
    @interface VisibilityMode {
    }

    private static class VisibilityInfo {
        ViewGroup endParent;
        int endVisibility;
        boolean fadeIn;
        ViewGroup startParent;
        int startVisibility;
        boolean visibilityChange;

        private VisibilityInfo() {
        }
    }

    public Visibility() {
        this.mMode = 3;
        this.mSuppressLayout = true;
    }

    public Visibility(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mMode = 3;
        this.mSuppressLayout = true;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.VisibilityTransition);
        int i = typedArrayObtainStyledAttributes.getInt(0, 0);
        typedArrayObtainStyledAttributes.recycle();
        if (i != 0) {
            setMode(i);
        }
    }

    public void setSuppressLayout(boolean z) {
        this.mSuppressLayout = z;
    }

    public void setMode(int i) {
        if ((i & (-4)) != 0) {
            throw new IllegalArgumentException("Only MODE_IN and MODE_OUT flags are allowed");
        }
        this.mMode = i;
    }

    public int getMode() {
        return this.mMode;
    }

    @Override
    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    private void captureValues(TransitionValues transitionValues) {
        transitionValues.values.put(PROPNAME_VISIBILITY, Integer.valueOf(transitionValues.view.getVisibility()));
        transitionValues.values.put(PROPNAME_PARENT, transitionValues.view.getParent());
        int[] iArr = new int[2];
        transitionValues.view.getLocationOnScreen(iArr);
        transitionValues.values.put(PROPNAME_SCREEN_LOCATION, iArr);
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    public boolean isVisible(TransitionValues transitionValues) {
        if (transitionValues == null) {
            return false;
        }
        return ((Integer) transitionValues.values.get(PROPNAME_VISIBILITY)).intValue() == 0 && ((View) transitionValues.values.get(PROPNAME_PARENT)) != null;
    }

    private static VisibilityInfo getVisibilityChangeInfo(TransitionValues transitionValues, TransitionValues transitionValues2) {
        VisibilityInfo visibilityInfo = new VisibilityInfo();
        visibilityInfo.visibilityChange = false;
        visibilityInfo.fadeIn = false;
        if (transitionValues != null && transitionValues.values.containsKey(PROPNAME_VISIBILITY)) {
            visibilityInfo.startVisibility = ((Integer) transitionValues.values.get(PROPNAME_VISIBILITY)).intValue();
            visibilityInfo.startParent = (ViewGroup) transitionValues.values.get(PROPNAME_PARENT);
        } else {
            visibilityInfo.startVisibility = -1;
            visibilityInfo.startParent = null;
        }
        if (transitionValues2 != null && transitionValues2.values.containsKey(PROPNAME_VISIBILITY)) {
            visibilityInfo.endVisibility = ((Integer) transitionValues2.values.get(PROPNAME_VISIBILITY)).intValue();
            visibilityInfo.endParent = (ViewGroup) transitionValues2.values.get(PROPNAME_PARENT);
        } else {
            visibilityInfo.endVisibility = -1;
            visibilityInfo.endParent = null;
        }
        if (transitionValues != null && transitionValues2 != null) {
            if (visibilityInfo.startVisibility == visibilityInfo.endVisibility && visibilityInfo.startParent == visibilityInfo.endParent) {
                return visibilityInfo;
            }
            if (visibilityInfo.startVisibility != visibilityInfo.endVisibility) {
                if (visibilityInfo.startVisibility == 0) {
                    visibilityInfo.fadeIn = false;
                    visibilityInfo.visibilityChange = true;
                } else if (visibilityInfo.endVisibility == 0) {
                    visibilityInfo.fadeIn = true;
                    visibilityInfo.visibilityChange = true;
                }
            } else if (visibilityInfo.startParent != visibilityInfo.endParent) {
                if (visibilityInfo.endParent == null) {
                    visibilityInfo.fadeIn = false;
                    visibilityInfo.visibilityChange = true;
                } else if (visibilityInfo.startParent == null) {
                    visibilityInfo.fadeIn = true;
                    visibilityInfo.visibilityChange = true;
                }
            }
        } else if (transitionValues == null && visibilityInfo.endVisibility == 0) {
            visibilityInfo.fadeIn = true;
            visibilityInfo.visibilityChange = true;
        } else if (transitionValues2 == null && visibilityInfo.startVisibility == 0) {
            visibilityInfo.fadeIn = false;
            visibilityInfo.visibilityChange = true;
        }
        return visibilityInfo;
    }

    @Override
    public Animator createAnimator(ViewGroup viewGroup, TransitionValues transitionValues, TransitionValues transitionValues2) {
        VisibilityInfo visibilityChangeInfo = getVisibilityChangeInfo(transitionValues, transitionValues2);
        if (!visibilityChangeInfo.visibilityChange) {
            return null;
        }
        if (visibilityChangeInfo.startParent != null || visibilityChangeInfo.endParent != null) {
            if (visibilityChangeInfo.fadeIn) {
                return onAppear(viewGroup, transitionValues, visibilityChangeInfo.startVisibility, transitionValues2, visibilityChangeInfo.endVisibility);
            }
            return onDisappear(viewGroup, transitionValues, visibilityChangeInfo.startVisibility, transitionValues2, visibilityChangeInfo.endVisibility);
        }
        return null;
    }

    public Animator onAppear(ViewGroup viewGroup, TransitionValues transitionValues, int i, TransitionValues transitionValues2, int i2) {
        if ((this.mMode & 1) != 1 || transitionValues2 == null) {
            return null;
        }
        if (transitionValues == null) {
            View view = (View) transitionValues2.view.getParent();
            if (getVisibilityChangeInfo(getMatchedTransitionValues(view, false), getTransitionValues(view, false)).visibilityChange) {
                return null;
            }
        }
        return onAppear(viewGroup, transitionValues2.view, transitionValues, transitionValues2);
    }

    public Animator onAppear(ViewGroup viewGroup, View view, TransitionValues transitionValues, TransitionValues transitionValues2) {
        return null;
    }

    public Animator onDisappear(final ViewGroup viewGroup, TransitionValues transitionValues, int i, TransitionValues transitionValues2, int i2) {
        int id;
        if ((this.mMode & 2) != 2) {
            return null;
        }
        final View viewCopyViewImage = transitionValues != null ? transitionValues.view : null;
        View view = transitionValues2 != null ? transitionValues2.view : null;
        if (view == null || view.getParent() == null) {
            if (view == null) {
                if (viewCopyViewImage != null) {
                    if (viewCopyViewImage.getParent() != null) {
                        if (viewCopyViewImage.getParent() instanceof View) {
                            View view2 = (View) viewCopyViewImage.getParent();
                            if (!getVisibilityChangeInfo(getTransitionValues(view2, true), getMatchedTransitionValues(view2, true)).visibilityChange) {
                                viewCopyViewImage = TransitionUtils.copyViewImage(viewGroup, viewCopyViewImage, view2);
                            } else if (view2.getParent() != null || (id = view2.getId()) == -1 || viewGroup.findViewById(id) == null || !this.mCanRemoveViews) {
                                viewCopyViewImage = null;
                            }
                        }
                    }
                }
                viewCopyViewImage = null;
                view = null;
            } else {
                viewCopyViewImage = view;
            }
            view = null;
        } else if (i2 != 4 && viewCopyViewImage != view) {
            if (!this.mCanRemoveViews) {
                viewCopyViewImage = TransitionUtils.copyViewImage(viewGroup, viewCopyViewImage, (View) viewCopyViewImage.getParent());
            }
            view = null;
        } else {
            viewCopyViewImage = null;
        }
        if (viewCopyViewImage != null) {
            int[] iArr = (int[]) transitionValues.values.get(PROPNAME_SCREEN_LOCATION);
            int i3 = iArr[0];
            int i4 = iArr[1];
            int[] iArr2 = new int[2];
            viewGroup.getLocationOnScreen(iArr2);
            viewCopyViewImage.offsetLeftAndRight((i3 - iArr2[0]) - viewCopyViewImage.getLeft());
            viewCopyViewImage.offsetTopAndBottom((i4 - iArr2[1]) - viewCopyViewImage.getTop());
            viewGroup.getOverlay().add(viewCopyViewImage);
            Animator animatorOnDisappear = onDisappear(viewGroup, viewCopyViewImage, transitionValues, transitionValues2);
            if (animatorOnDisappear == null) {
                viewGroup.getOverlay().remove(viewCopyViewImage);
            } else {
                addListener(new TransitionListenerAdapter() {
                    @Override
                    public void onTransitionEnd(Transition transition) {
                        viewGroup.getOverlay().remove(viewCopyViewImage);
                        transition.removeListener(this);
                    }
                });
            }
            return animatorOnDisappear;
        }
        if (view == null) {
            return null;
        }
        int visibility = view.getVisibility();
        view.setTransitionVisibility(0);
        Animator animatorOnDisappear2 = onDisappear(viewGroup, view, transitionValues, transitionValues2);
        if (animatorOnDisappear2 != null) {
            DisappearListener disappearListener = new DisappearListener(view, i2, this.mSuppressLayout);
            animatorOnDisappear2.addListener(disappearListener);
            animatorOnDisappear2.addPauseListener(disappearListener);
            addListener(disappearListener);
        } else {
            view.setTransitionVisibility(visibility);
        }
        return animatorOnDisappear2;
    }

    @Override
    public boolean isTransitionRequired(TransitionValues transitionValues, TransitionValues transitionValues2) {
        if (transitionValues == null && transitionValues2 == null) {
            return false;
        }
        if (transitionValues != null && transitionValues2 != null && transitionValues2.values.containsKey(PROPNAME_VISIBILITY) != transitionValues.values.containsKey(PROPNAME_VISIBILITY)) {
            return false;
        }
        VisibilityInfo visibilityChangeInfo = getVisibilityChangeInfo(transitionValues, transitionValues2);
        if (visibilityChangeInfo.visibilityChange) {
            return visibilityChangeInfo.startVisibility == 0 || visibilityChangeInfo.endVisibility == 0;
        }
        return false;
    }

    public Animator onDisappear(ViewGroup viewGroup, View view, TransitionValues transitionValues, TransitionValues transitionValues2) {
        return null;
    }

    private static class DisappearListener extends TransitionListenerAdapter implements Animator.AnimatorListener, Animator.AnimatorPauseListener {
        boolean mCanceled = false;
        private final int mFinalVisibility;
        private boolean mLayoutSuppressed;
        private final ViewGroup mParent;
        private final boolean mSuppressLayout;
        private final View mView;

        public DisappearListener(View view, int i, boolean z) {
            this.mView = view;
            this.mFinalVisibility = i;
            this.mParent = (ViewGroup) view.getParent();
            this.mSuppressLayout = z;
            suppressLayout(true);
        }

        @Override
        public void onAnimationPause(Animator animator) {
            if (!this.mCanceled) {
                this.mView.setTransitionVisibility(this.mFinalVisibility);
            }
        }

        @Override
        public void onAnimationResume(Animator animator) {
            if (!this.mCanceled) {
                this.mView.setTransitionVisibility(0);
            }
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            this.mCanceled = true;
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
        }

        @Override
        public void onAnimationStart(Animator animator) {
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            hideViewWhenNotCanceled();
        }

        @Override
        public void onTransitionEnd(Transition transition) {
            hideViewWhenNotCanceled();
            transition.removeListener(this);
        }

        @Override
        public void onTransitionPause(Transition transition) {
            suppressLayout(false);
        }

        @Override
        public void onTransitionResume(Transition transition) {
            suppressLayout(true);
        }

        private void hideViewWhenNotCanceled() {
            if (!this.mCanceled) {
                this.mView.setTransitionVisibility(this.mFinalVisibility);
                if (this.mParent != null) {
                    this.mParent.invalidate();
                }
            }
            suppressLayout(false);
        }

        private void suppressLayout(boolean z) {
            if (this.mSuppressLayout && this.mLayoutSuppressed != z && this.mParent != null) {
                this.mLayoutSuppressed = z;
                this.mParent.suppressLayout(z);
            }
        }
    }
}
