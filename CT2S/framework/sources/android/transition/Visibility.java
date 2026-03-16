package android.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.R;

public abstract class Visibility extends Transition {
    public static final int MODE_IN = 1;
    public static final int MODE_OUT = 2;
    private static final String PROPNAME_SCREEN_LOCATION = "android:visibility:screenLocation";
    private int mForcedEndVisibility;
    private int mForcedStartVisibility;
    private int mMode;
    static final String PROPNAME_VISIBILITY = "android:visibility:visibility";
    private static final String PROPNAME_PARENT = "android:visibility:parent";
    private static final String[] sTransitionProperties = {PROPNAME_VISIBILITY, PROPNAME_PARENT};

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
        this.mForcedStartVisibility = -1;
        this.mForcedEndVisibility = -1;
    }

    public Visibility(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mMode = 3;
        this.mForcedStartVisibility = -1;
        this.mForcedEndVisibility = -1;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.VisibilityTransition);
        int mode = a.getInt(0, 0);
        a.recycle();
        if (mode != 0) {
            setMode(mode);
        }
    }

    public void setMode(int mode) {
        if ((mode & (-4)) != 0) {
            throw new IllegalArgumentException("Only MODE_IN and MODE_OUT flags are allowed");
        }
        this.mMode = mode;
    }

    public int getMode() {
        return this.mMode;
    }

    @Override
    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    private void captureValues(TransitionValues transitionValues, int forcedVisibility) {
        int visibility;
        if (forcedVisibility != -1) {
            visibility = forcedVisibility;
        } else {
            visibility = transitionValues.view.getVisibility();
        }
        transitionValues.values.put(PROPNAME_VISIBILITY, Integer.valueOf(visibility));
        transitionValues.values.put(PROPNAME_PARENT, transitionValues.view.getParent());
        int[] loc = new int[2];
        transitionValues.view.getLocationOnScreen(loc);
        transitionValues.values.put(PROPNAME_SCREEN_LOCATION, loc);
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues, this.mForcedStartVisibility);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues, this.mForcedEndVisibility);
    }

    @Override
    public void forceVisibility(int visibility, boolean isStartValue) {
        if (isStartValue) {
            this.mForcedStartVisibility = visibility;
        } else {
            this.mForcedEndVisibility = visibility;
        }
    }

    public boolean isVisible(TransitionValues values) {
        if (values == null) {
            return false;
        }
        int visibility = ((Integer) values.values.get(PROPNAME_VISIBILITY)).intValue();
        View parent = (View) values.values.get(PROPNAME_PARENT);
        return visibility == 0 && parent != null;
    }

    private VisibilityInfo getVisibilityChangeInfo(TransitionValues startValues, TransitionValues endValues) {
        VisibilityInfo visInfo = new VisibilityInfo();
        visInfo.visibilityChange = false;
        visInfo.fadeIn = false;
        if (startValues != null && startValues.values.containsKey(PROPNAME_VISIBILITY)) {
            visInfo.startVisibility = ((Integer) startValues.values.get(PROPNAME_VISIBILITY)).intValue();
            visInfo.startParent = (ViewGroup) startValues.values.get(PROPNAME_PARENT);
        } else {
            visInfo.startVisibility = -1;
            visInfo.startParent = null;
        }
        if (endValues != null && endValues.values.containsKey(PROPNAME_VISIBILITY)) {
            visInfo.endVisibility = ((Integer) endValues.values.get(PROPNAME_VISIBILITY)).intValue();
            visInfo.endParent = (ViewGroup) endValues.values.get(PROPNAME_PARENT);
        } else {
            visInfo.endVisibility = -1;
            visInfo.endParent = null;
        }
        if (startValues != null && endValues != null) {
            if (visInfo.startVisibility != visInfo.endVisibility || visInfo.startParent != visInfo.endParent) {
                if (visInfo.startVisibility != visInfo.endVisibility) {
                    if (visInfo.startVisibility == 0) {
                        visInfo.fadeIn = false;
                        visInfo.visibilityChange = true;
                    } else if (visInfo.endVisibility == 0) {
                        visInfo.fadeIn = true;
                        visInfo.visibilityChange = true;
                    }
                } else if (visInfo.startParent != visInfo.endParent) {
                    if (visInfo.endParent == null) {
                        visInfo.fadeIn = false;
                        visInfo.visibilityChange = true;
                    } else if (visInfo.startParent == null) {
                        visInfo.fadeIn = true;
                        visInfo.visibilityChange = true;
                    }
                }
            }
        } else if (startValues == null && visInfo.endVisibility == 0) {
            visInfo.fadeIn = true;
            visInfo.visibilityChange = true;
        } else if (endValues == null && visInfo.startVisibility == 0) {
            visInfo.fadeIn = false;
            visInfo.visibilityChange = true;
        }
        return visInfo;
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
        VisibilityInfo visInfo = getVisibilityChangeInfo(startValues, endValues);
        if (visInfo.visibilityChange && (visInfo.startParent != null || visInfo.endParent != null)) {
            if (visInfo.fadeIn) {
                return onAppear(sceneRoot, startValues, visInfo.startVisibility, endValues, visInfo.endVisibility);
            }
            return onDisappear(sceneRoot, startValues, visInfo.startVisibility, endValues, visInfo.endVisibility);
        }
        return null;
    }

    public Animator onAppear(ViewGroup sceneRoot, TransitionValues startValues, int startVisibility, TransitionValues endValues, int endVisibility) {
        if ((this.mMode & 1) != 1 || endValues == null) {
            return null;
        }
        if (startValues == null) {
            View endParent = (View) endValues.view.getParent();
            TransitionValues startParentValues = getMatchedTransitionValues(endParent, false);
            TransitionValues endParentValues = getTransitionValues(endParent, false);
            VisibilityInfo parentVisibilityInfo = getVisibilityChangeInfo(startParentValues, endParentValues);
            if (parentVisibilityInfo.visibilityChange) {
                return null;
            }
        }
        return onAppear(sceneRoot, endValues.view, startValues, endValues);
    }

    public Animator onAppear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
        return null;
    }

    public Animator onDisappear(final ViewGroup sceneRoot, TransitionValues startValues, int startVisibility, TransitionValues endValues, final int endVisibility) {
        int id;
        if ((this.mMode & 2) != 2) {
            return null;
        }
        View startView = startValues != null ? startValues.view : null;
        View endView = endValues != null ? endValues.view : null;
        View overlayView = null;
        View viewToKeep = null;
        if (endView == null || endView.getParent() == null) {
            if (endView != null) {
                overlayView = endView;
            } else if (startView != null) {
                if (startView.getParent() == null) {
                    overlayView = startView;
                } else if (startView.getParent() instanceof View) {
                    View startParent = (View) startView.getParent();
                    TransitionValues startParentValues = getTransitionValues(startParent, true);
                    TransitionValues endParentValues = getMatchedTransitionValues(startParent, true);
                    VisibilityInfo parentVisibilityInfo = getVisibilityChangeInfo(startParentValues, endParentValues);
                    if (!parentVisibilityInfo.visibilityChange) {
                        overlayView = TransitionUtils.copyViewImage(sceneRoot, startView, startParent);
                    } else if (startParent.getParent() == null && (id = startParent.getId()) != -1 && sceneRoot.findViewById(id) != null && this.mCanRemoveViews) {
                        overlayView = startView;
                    }
                }
            }
        } else if (endVisibility == 4 || startView == endView) {
            viewToKeep = endView;
        } else {
            overlayView = startView;
        }
        if (overlayView != null) {
            int[] screenLoc = (int[]) startValues.values.get(PROPNAME_SCREEN_LOCATION);
            int screenX = screenLoc[0];
            int screenY = screenLoc[1];
            int[] loc = new int[2];
            sceneRoot.getLocationOnScreen(loc);
            overlayView.offsetLeftAndRight((screenX - loc[0]) - overlayView.getLeft());
            overlayView.offsetTopAndBottom((screenY - loc[1]) - overlayView.getTop());
            sceneRoot.getOverlay().add(overlayView);
            Animator animator = onDisappear(sceneRoot, overlayView, startValues, endValues);
            if (animator == null) {
                sceneRoot.getOverlay().remove(overlayView);
                return animator;
            }
            final View finalOverlayView = overlayView;
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    sceneRoot.getOverlay().remove(finalOverlayView);
                }
            });
            return animator;
        }
        if (viewToKeep != null) {
            int originalVisibility = -1;
            final boolean isForcedVisibility = (this.mForcedStartVisibility == -1 && this.mForcedEndVisibility == -1) ? false : true;
            if (!isForcedVisibility) {
                originalVisibility = viewToKeep.getVisibility();
                viewToKeep.setVisibility(0);
            }
            Animator animator2 = onDisappear(sceneRoot, viewToKeep, startValues, endValues);
            if (animator2 != null) {
                final View finalViewToKeep = viewToKeep;
                animator2.addListener(new AnimatorListenerAdapter() {
                    boolean mCanceled = false;

                    @Override
                    public void onAnimationPause(Animator animation) {
                        if (!this.mCanceled && !isForcedVisibility) {
                            finalViewToKeep.setVisibility(endVisibility);
                        }
                    }

                    @Override
                    public void onAnimationResume(Animator animation) {
                        if (!this.mCanceled && !isForcedVisibility) {
                            finalViewToKeep.setVisibility(0);
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        this.mCanceled = true;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!this.mCanceled) {
                            if (isForcedVisibility) {
                                finalViewToKeep.setTransitionAlpha(0.0f);
                            } else {
                                finalViewToKeep.setVisibility(endVisibility);
                            }
                        }
                    }
                });
                return animator2;
            }
            if (!isForcedVisibility) {
                viewToKeep.setVisibility(originalVisibility);
                return animator2;
            }
            return animator2;
        }
        return null;
    }

    @Override
    boolean areValuesChanged(TransitionValues oldValues, TransitionValues newValues) {
        if (oldValues == null && newValues == null) {
            return false;
        }
        if (oldValues != null && newValues != null && newValues.values.containsKey(PROPNAME_VISIBILITY) != oldValues.values.containsKey(PROPNAME_VISIBILITY)) {
            return false;
        }
        VisibilityInfo changeInfo = getVisibilityChangeInfo(oldValues, newValues);
        if (changeInfo.visibilityChange) {
            return changeInfo.startVisibility == 0 || changeInfo.endVisibility == 0;
        }
        return false;
    }

    public Animator onDisappear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
        return null;
    }
}
