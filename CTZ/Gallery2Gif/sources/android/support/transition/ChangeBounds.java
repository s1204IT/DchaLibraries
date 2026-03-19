package android.support.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.transition.Transition;
import android.support.v4.view.ViewCompat;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import java.util.Map;

public class ChangeBounds extends Transition {
    private static final String[] sTransitionProperties = {"android:changeBounds:bounds", "android:changeBounds:clip", "android:changeBounds:parent", "android:changeBounds:windowX", "android:changeBounds:windowY"};
    private static final Property<Drawable, PointF> DRAWABLE_ORIGIN_PROPERTY = new Property<Drawable, PointF>(PointF.class, "boundsOrigin") {
        private Rect mBounds = new Rect();

        @Override
        public void set(Drawable object, PointF value) {
            object.copyBounds(this.mBounds);
            this.mBounds.offsetTo(Math.round(value.x), Math.round(value.y));
            object.setBounds(this.mBounds);
        }

        @Override
        public PointF get(Drawable object) {
            object.copyBounds(this.mBounds);
            return new PointF(this.mBounds.left, this.mBounds.top);
        }
    };
    private static final Property<ViewBounds, PointF> TOP_LEFT_PROPERTY = new Property<ViewBounds, PointF>(PointF.class, "topLeft") {
        @Override
        public void set(ViewBounds viewBounds, PointF topLeft) {
            viewBounds.setTopLeft(topLeft);
        }

        @Override
        public PointF get(ViewBounds viewBounds) {
            return null;
        }
    };
    private static final Property<ViewBounds, PointF> BOTTOM_RIGHT_PROPERTY = new Property<ViewBounds, PointF>(PointF.class, "bottomRight") {
        @Override
        public void set(ViewBounds viewBounds, PointF bottomRight) {
            viewBounds.setBottomRight(bottomRight);
        }

        @Override
        public PointF get(ViewBounds viewBounds) {
            return null;
        }
    };
    private static final Property<View, PointF> BOTTOM_RIGHT_ONLY_PROPERTY = new Property<View, PointF>(PointF.class, "bottomRight") {
        @Override
        public void set(View view, PointF bottomRight) {
            int left = view.getLeft();
            int top = view.getTop();
            int right = Math.round(bottomRight.x);
            int bottom = Math.round(bottomRight.y);
            ViewUtils.setLeftTopRightBottom(view, left, top, right, bottom);
        }

        @Override
        public PointF get(View view) {
            return null;
        }
    };
    private static final Property<View, PointF> TOP_LEFT_ONLY_PROPERTY = new Property<View, PointF>(PointF.class, "topLeft") {
        @Override
        public void set(View view, PointF topLeft) {
            int left = Math.round(topLeft.x);
            int top = Math.round(topLeft.y);
            int right = view.getRight();
            int bottom = view.getBottom();
            ViewUtils.setLeftTopRightBottom(view, left, top, right, bottom);
        }

        @Override
        public PointF get(View view) {
            return null;
        }
    };
    private static final Property<View, PointF> POSITION_PROPERTY = new Property<View, PointF>(PointF.class, "position") {
        @Override
        public void set(View view, PointF topLeft) {
            int left = Math.round(topLeft.x);
            int top = Math.round(topLeft.y);
            int right = view.getWidth() + left;
            int bottom = view.getHeight() + top;
            ViewUtils.setLeftTopRightBottom(view, left, top, right, bottom);
        }

        @Override
        public PointF get(View view) {
            return null;
        }
    };
    private static RectEvaluator sRectEvaluator = new RectEvaluator();
    private int[] mTempLocation = new int[2];
    private boolean mResizeClip = false;
    private boolean mReparent = false;

    @Override
    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    private void captureValues(TransitionValues values) {
        View view = values.view;
        if (ViewCompat.isLaidOut(view) || view.getWidth() != 0 || view.getHeight() != 0) {
            values.values.put("android:changeBounds:bounds", new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom()));
            values.values.put("android:changeBounds:parent", values.view.getParent());
            if (this.mReparent) {
                values.view.getLocationInWindow(this.mTempLocation);
                values.values.put("android:changeBounds:windowX", Integer.valueOf(this.mTempLocation[0]));
                values.values.put("android:changeBounds:windowY", Integer.valueOf(this.mTempLocation[1]));
            }
            if (this.mResizeClip) {
                values.values.put("android:changeBounds:clip", ViewCompat.getClipBounds(view));
            }
        }
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    private boolean parentMatches(View startParent, View endParent) {
        if (!this.mReparent) {
            return true;
        }
        boolean z = true;
        TransitionValues endValues = getMatchedTransitionValues(startParent, true);
        if (endValues == null) {
            if (startParent != endParent) {
                z = false;
            }
            boolean parentMatches = z;
            return parentMatches;
        }
        if (endParent != endValues.view) {
            z = false;
        }
        boolean parentMatches2 = z;
        return parentMatches2;
    }

    @Override
    public Animator createAnimator(final ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
        final View view;
        int startLeft;
        int startTop;
        int endLeft;
        ObjectAnimator positionAnimator;
        int i;
        Rect startClip;
        boolean z;
        ObjectAnimator clipAnimator;
        Animator anim;
        int endWidth;
        int startHeight;
        if (startValues == null || endValues == null) {
            return null;
        }
        Map<String, Object> startParentVals = startValues.values;
        Map<String, Object> endParentVals = endValues.values;
        ViewGroup startParent = (ViewGroup) startParentVals.get("android:changeBounds:parent");
        ViewGroup endParent = (ViewGroup) endParentVals.get("android:changeBounds:parent");
        if (startParent != null && endParent != null) {
            final View view2 = endValues.view;
            if (!parentMatches(startParent, endParent)) {
                int startX = ((Integer) startValues.values.get("android:changeBounds:windowX")).intValue();
                int startY = ((Integer) startValues.values.get("android:changeBounds:windowY")).intValue();
                int endX = ((Integer) endValues.values.get("android:changeBounds:windowX")).intValue();
                int endY = ((Integer) endValues.values.get("android:changeBounds:windowY")).intValue();
                if (startX == endX && startY == endY) {
                    return null;
                }
                sceneRoot.getLocationInWindow(this.mTempLocation);
                Bitmap bitmap = Bitmap.createBitmap(view2.getWidth(), view2.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                view2.draw(canvas);
                final BitmapDrawable drawable = new BitmapDrawable(bitmap);
                final float transitionAlpha = ViewUtils.getTransitionAlpha(view2);
                ViewUtils.setTransitionAlpha(view2, 0.0f);
                ViewUtils.getOverlay(sceneRoot).add(drawable);
                Path topLeftPath = getPathMotion().getPath(startX - this.mTempLocation[0], startY - this.mTempLocation[1], endX - this.mTempLocation[0], endY - this.mTempLocation[1]);
                PropertyValuesHolder origin = PropertyValuesHolderUtils.ofPointF(DRAWABLE_ORIGIN_PROPERTY, topLeftPath);
                ObjectAnimator anim2 = ObjectAnimator.ofPropertyValuesHolder(drawable, origin);
                anim2.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ViewUtils.getOverlay(sceneRoot).remove(drawable);
                        ViewUtils.setTransitionAlpha(view2, transitionAlpha);
                    }
                });
                return anim2;
            }
            Rect startBounds = (Rect) startValues.values.get("android:changeBounds:bounds");
            Rect endBounds = (Rect) endValues.values.get("android:changeBounds:bounds");
            int startLeft2 = startBounds.left;
            int endLeft2 = endBounds.left;
            int startTop2 = startBounds.top;
            final int endTop = endBounds.top;
            int startRight = startBounds.right;
            final int endRight = endBounds.right;
            int startBottom = startBounds.bottom;
            final int endBottom = endBounds.bottom;
            int startWidth = startRight - startLeft2;
            int startHeight2 = startBottom - startTop2;
            int endWidth2 = endRight - endLeft2;
            int endHeight = endBottom - endTop;
            Rect startClip2 = (Rect) startValues.values.get("android:changeBounds:clip");
            final Rect endClip = (Rect) endValues.values.get("android:changeBounds:clip");
            if ((startWidth != 0 && startHeight2 != 0) || (endWidth2 != 0 && endHeight != 0)) {
                numChanges = (startLeft2 == endLeft2 && startTop2 == endTop) ? 0 : 0 + 1;
                if (startRight != endRight || startBottom != endBottom) {
                    numChanges++;
                }
            }
            if ((startClip2 != null && !startClip2.equals(endClip)) || (startClip2 == null && endClip != null)) {
                numChanges++;
            }
            if (numChanges <= 0) {
                return null;
            }
            if (this.mResizeClip) {
                view = view2;
                int maxWidth = Math.max(startWidth, endWidth2);
                int maxHeight = Math.max(startHeight2, endHeight);
                ViewUtils.setLeftTopRightBottom(view, startLeft2, startTop2, startLeft2 + maxWidth, startTop2 + maxHeight);
                if (startLeft2 == endLeft2 && startTop2 == endTop) {
                    endLeft = endLeft2;
                    positionAnimator = null;
                    startTop = startTop2;
                    startLeft = startLeft2;
                } else {
                    startLeft = startLeft2;
                    startTop = startTop2;
                    endLeft = endLeft2;
                    Path topLeftPath2 = getPathMotion().getPath(startLeft2, startTop2, endLeft2, endTop);
                    ObjectAnimator positionAnimator2 = ObjectAnimatorUtils.ofPointF(view, POSITION_PROPERTY, topLeftPath2);
                    positionAnimator = positionAnimator2;
                }
                if (startClip2 == null) {
                    i = 0;
                    startClip = new Rect(0, 0, startWidth, startHeight2);
                } else {
                    i = 0;
                    startClip = startClip2;
                }
                Rect endClip2 = endClip == null ? new Rect(i, i, endWidth2, endHeight) : endClip;
                if (startClip.equals(endClip2)) {
                    z = true;
                    clipAnimator = null;
                } else {
                    ViewCompat.setClipBounds(view, startClip);
                    ObjectAnimator clipAnimator2 = ObjectAnimator.ofObject(view, "clipBounds", sRectEvaluator, startClip, endClip2);
                    final int endLeft3 = endLeft;
                    clipAnimator = clipAnimator2;
                    z = true;
                    clipAnimator.addListener(new AnimatorListenerAdapter() {
                        private boolean mIsCanceled;

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            this.mIsCanceled = true;
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (!this.mIsCanceled) {
                                ViewCompat.setClipBounds(view, endClip);
                                ViewUtils.setLeftTopRightBottom(view, endLeft3, endTop, endRight, endBottom);
                            }
                        }
                    });
                }
                anim = TransitionUtils.mergeAnimators(positionAnimator, clipAnimator);
            } else {
                view = view2;
                ViewUtils.setLeftTopRightBottom(view, startLeft2, startTop2, startRight, startBottom);
                if (numChanges != 2) {
                    endWidth = endWidth2;
                    startHeight = startHeight2;
                    if (startLeft2 == endLeft2 && startTop2 == endTop) {
                        Path bottomRight = getPathMotion().getPath(startRight, startBottom, endRight, endBottom);
                        view = view;
                        anim = ObjectAnimatorUtils.ofPointF(view, BOTTOM_RIGHT_ONLY_PROPERTY, bottomRight);
                    } else {
                        view = view;
                        Path topLeftPath3 = getPathMotion().getPath(startLeft2, startTop2, endLeft2, endTop);
                        anim = ObjectAnimatorUtils.ofPointF(view, TOP_LEFT_ONLY_PROPERTY, topLeftPath3);
                    }
                } else if (startWidth == endWidth2 && startHeight2 == endHeight) {
                    startHeight = startHeight2;
                    endWidth = endWidth2;
                    Path topLeftPath4 = getPathMotion().getPath(startLeft2, startTop2, endLeft2, endTop);
                    anim = ObjectAnimatorUtils.ofPointF(view, POSITION_PROPERTY, topLeftPath4);
                } else {
                    endWidth = endWidth2;
                    startHeight = startHeight2;
                    final ViewBounds viewBounds = new ViewBounds(view);
                    Path topLeftPath5 = getPathMotion().getPath(startLeft2, startTop2, endLeft2, endTop);
                    ObjectAnimator topLeftAnimator = ObjectAnimatorUtils.ofPointF(viewBounds, TOP_LEFT_PROPERTY, topLeftPath5);
                    Path bottomRightPath = getPathMotion().getPath(startRight, startBottom, endRight, endBottom);
                    ObjectAnimator bottomRightAnimator = ObjectAnimatorUtils.ofPointF(viewBounds, BOTTOM_RIGHT_PROPERTY, bottomRightPath);
                    AnimatorSet set = new AnimatorSet();
                    set.playTogether(topLeftAnimator, bottomRightAnimator);
                    set.addListener(new AnimatorListenerAdapter() {
                        private ViewBounds mViewBounds;

                        {
                            this.mViewBounds = viewBounds;
                        }
                    });
                    anim = set;
                    view = view;
                }
                z = true;
            }
            if (view.getParent() instanceof ViewGroup) {
                final ViewGroup parent = (ViewGroup) view.getParent();
                ViewGroupUtils.suppressLayout(parent, z);
                Transition.TransitionListener transitionListener = new TransitionListenerAdapter() {
                    boolean mCanceled = false;

                    @Override
                    public void onTransitionEnd(Transition transition) {
                        if (!this.mCanceled) {
                            ViewGroupUtils.suppressLayout(parent, false);
                        }
                        transition.removeListener(this);
                    }

                    @Override
                    public void onTransitionPause(Transition transition) {
                        ViewGroupUtils.suppressLayout(parent, false);
                    }

                    @Override
                    public void onTransitionResume(Transition transition) {
                        ViewGroupUtils.suppressLayout(parent, true);
                    }
                };
                addListener(transitionListener);
            }
            return anim;
        }
        return null;
    }

    private static class ViewBounds {
        private int mBottom;
        private int mBottomRightCalls;
        private int mLeft;
        private int mRight;
        private int mTop;
        private int mTopLeftCalls;
        private View mView;

        ViewBounds(View view) {
            this.mView = view;
        }

        void setTopLeft(PointF topLeft) {
            this.mLeft = Math.round(topLeft.x);
            this.mTop = Math.round(topLeft.y);
            this.mTopLeftCalls++;
            if (this.mTopLeftCalls == this.mBottomRightCalls) {
                setLeftTopRightBottom();
            }
        }

        void setBottomRight(PointF bottomRight) {
            this.mRight = Math.round(bottomRight.x);
            this.mBottom = Math.round(bottomRight.y);
            this.mBottomRightCalls++;
            if (this.mTopLeftCalls == this.mBottomRightCalls) {
                setLeftTopRightBottom();
            }
        }

        private void setLeftTopRightBottom() {
            ViewUtils.setLeftTopRightBottom(this.mView, this.mLeft, this.mTop, this.mRight, this.mBottom);
            this.mTopLeftCalls = 0;
            this.mBottomRightCalls = 0;
        }
    }
}
