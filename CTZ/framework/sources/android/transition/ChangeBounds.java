package android.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.RectEvaluator;
import android.animation.TypeConverter;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.BrowserContract;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.R;
import java.util.Map;

public class ChangeBounds extends Transition {
    private static final String LOG_TAG = "ChangeBounds";
    boolean mReparent;
    boolean mResizeClip;
    int[] tempLocation;
    private static final String PROPNAME_BOUNDS = "android:changeBounds:bounds";
    private static final String PROPNAME_CLIP = "android:changeBounds:clip";
    private static final String PROPNAME_PARENT = "android:changeBounds:parent";
    private static final String PROPNAME_WINDOW_X = "android:changeBounds:windowX";
    private static final String PROPNAME_WINDOW_Y = "android:changeBounds:windowY";
    private static final String[] sTransitionProperties = {PROPNAME_BOUNDS, PROPNAME_CLIP, PROPNAME_PARENT, PROPNAME_WINDOW_X, PROPNAME_WINDOW_Y};
    private static final Property<Drawable, PointF> DRAWABLE_ORIGIN_PROPERTY = new Property<Drawable, PointF>(PointF.class, "boundsOrigin") {
        private Rect mBounds = new Rect();

        @Override
        public void set(Drawable drawable, PointF pointF) {
            drawable.copyBounds(this.mBounds);
            this.mBounds.offsetTo(Math.round(pointF.x), Math.round(pointF.y));
            drawable.setBounds(this.mBounds);
        }

        @Override
        public PointF get(Drawable drawable) {
            drawable.copyBounds(this.mBounds);
            return new PointF(this.mBounds.left, this.mBounds.top);
        }
    };
    private static final Property<ViewBounds, PointF> TOP_LEFT_PROPERTY = new Property<ViewBounds, PointF>(PointF.class, "topLeft") {
        @Override
        public void set(ViewBounds viewBounds, PointF pointF) {
            viewBounds.setTopLeft(pointF);
        }

        @Override
        public PointF get(ViewBounds viewBounds) {
            return null;
        }
    };
    private static final Property<ViewBounds, PointF> BOTTOM_RIGHT_PROPERTY = new Property<ViewBounds, PointF>(PointF.class, "bottomRight") {
        @Override
        public void set(ViewBounds viewBounds, PointF pointF) {
            viewBounds.setBottomRight(pointF);
        }

        @Override
        public PointF get(ViewBounds viewBounds) {
            return null;
        }
    };
    private static final Property<View, PointF> BOTTOM_RIGHT_ONLY_PROPERTY = new Property<View, PointF>(PointF.class, "bottomRight") {
        @Override
        public void set(View view, PointF pointF) {
            view.setLeftTopRightBottom(view.getLeft(), view.getTop(), Math.round(pointF.x), Math.round(pointF.y));
        }

        @Override
        public PointF get(View view) {
            return null;
        }
    };
    private static final Property<View, PointF> TOP_LEFT_ONLY_PROPERTY = new Property<View, PointF>(PointF.class, "topLeft") {
        @Override
        public void set(View view, PointF pointF) {
            view.setLeftTopRightBottom(Math.round(pointF.x), Math.round(pointF.y), view.getRight(), view.getBottom());
        }

        @Override
        public PointF get(View view) {
            return null;
        }
    };
    private static final Property<View, PointF> POSITION_PROPERTY = new Property<View, PointF>(PointF.class, BrowserContract.Bookmarks.POSITION) {
        @Override
        public void set(View view, PointF pointF) {
            int iRound = Math.round(pointF.x);
            int iRound2 = Math.round(pointF.y);
            view.setLeftTopRightBottom(iRound, iRound2, view.getWidth() + iRound, view.getHeight() + iRound2);
        }

        @Override
        public PointF get(View view) {
            return null;
        }
    };
    private static RectEvaluator sRectEvaluator = new RectEvaluator();

    public ChangeBounds() {
        this.tempLocation = new int[2];
        this.mResizeClip = false;
        this.mReparent = false;
    }

    public ChangeBounds(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.tempLocation = new int[2];
        this.mResizeClip = false;
        this.mReparent = false;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ChangeBounds);
        boolean z = typedArrayObtainStyledAttributes.getBoolean(0, false);
        typedArrayObtainStyledAttributes.recycle();
        setResizeClip(z);
    }

    @Override
    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    public void setResizeClip(boolean z) {
        this.mResizeClip = z;
    }

    public boolean getResizeClip() {
        return this.mResizeClip;
    }

    @Deprecated
    public void setReparent(boolean z) {
        this.mReparent = z;
    }

    private void captureValues(TransitionValues transitionValues) {
        View view = transitionValues.view;
        if (view.isLaidOut() || view.getWidth() != 0 || view.getHeight() != 0) {
            transitionValues.values.put(PROPNAME_BOUNDS, new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom()));
            transitionValues.values.put(PROPNAME_PARENT, transitionValues.view.getParent());
            if (this.mReparent) {
                transitionValues.view.getLocationInWindow(this.tempLocation);
                transitionValues.values.put(PROPNAME_WINDOW_X, Integer.valueOf(this.tempLocation[0]));
                transitionValues.values.put(PROPNAME_WINDOW_Y, Integer.valueOf(this.tempLocation[1]));
            }
            if (this.mResizeClip) {
                transitionValues.values.put(PROPNAME_CLIP, view.getClipBounds());
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

    private boolean parentMatches(View view, View view2) {
        if (!this.mReparent) {
            return true;
        }
        TransitionValues matchedTransitionValues = getMatchedTransitionValues(view, true);
        if (matchedTransitionValues == null) {
            if (view == view2) {
                return true;
            }
        } else if (view2 == matchedTransitionValues.view) {
            return true;
        }
        return false;
    }

    @Override
    public Animator createAnimator(final ViewGroup viewGroup, TransitionValues transitionValues, TransitionValues transitionValues2) {
        int i;
        Rect rect;
        int i2;
        ChangeBounds changeBounds;
        ObjectAnimator objectAnimatorOfObject;
        int i3;
        Rect rect2;
        Rect rect3;
        ObjectAnimator objectAnimatorOfObject2;
        if (transitionValues == null || transitionValues2 == null) {
            return null;
        }
        Map<String, Object> map = transitionValues.values;
        Map<String, Object> map2 = transitionValues2.values;
        ViewGroup viewGroup2 = (ViewGroup) map.get(PROPNAME_PARENT);
        ViewGroup viewGroup3 = (ViewGroup) map2.get(PROPNAME_PARENT);
        if (viewGroup2 == null || viewGroup3 == null) {
            return null;
        }
        final View view = transitionValues2.view;
        if (parentMatches(viewGroup2, viewGroup3)) {
            Rect rect4 = (Rect) transitionValues.values.get(PROPNAME_BOUNDS);
            Rect rect5 = (Rect) transitionValues2.values.get(PROPNAME_BOUNDS);
            int i4 = rect4.left;
            final int i5 = rect5.left;
            int i6 = rect4.top;
            final int i7 = rect5.top;
            int i8 = rect4.right;
            final int i9 = rect5.right;
            int i10 = rect4.bottom;
            int i11 = rect5.bottom;
            int i12 = i8 - i4;
            int i13 = i10 - i6;
            int i14 = i9 - i5;
            int i15 = i11 - i7;
            Rect rect6 = (Rect) transitionValues.values.get(PROPNAME_CLIP);
            final Rect rect7 = (Rect) transitionValues2.values.get(PROPNAME_CLIP);
            if ((i12 != 0 && i13 != 0) || (i14 != 0 && i15 != 0)) {
                i = (i4 == i5 && i6 == i7) ? 0 : 1;
                if (i8 != i9 || i10 != i11) {
                    i++;
                }
            } else {
                i = 0;
            }
            if ((rect6 != null && !rect6.equals(rect7)) || (rect6 == null && rect7 != null)) {
                i++;
            }
            if (i <= 0) {
                return null;
            }
            if (view.getParent() instanceof ViewGroup) {
                final ViewGroup viewGroup4 = (ViewGroup) view.getParent();
                rect = rect6;
                viewGroup4.suppressLayout(true);
                i2 = i11;
                changeBounds = this;
                changeBounds.addListener(new TransitionListenerAdapter() {
                    boolean mCanceled = false;

                    @Override
                    public void onTransitionCancel(Transition transition) {
                        viewGroup4.suppressLayout(false);
                        this.mCanceled = true;
                    }

                    @Override
                    public void onTransitionEnd(Transition transition) {
                        if (!this.mCanceled) {
                            viewGroup4.suppressLayout(false);
                        }
                        transition.removeListener(this);
                    }

                    @Override
                    public void onTransitionPause(Transition transition) {
                        viewGroup4.suppressLayout(false);
                    }

                    @Override
                    public void onTransitionResume(Transition transition) {
                        viewGroup4.suppressLayout(true);
                    }
                });
            } else {
                rect = rect6;
                i2 = i11;
                changeBounds = this;
            }
            if (!changeBounds.mResizeClip) {
                view.setLeftTopRightBottom(i4, i6, i8, i10);
                if (i == 2) {
                    if (i12 == i14 && i13 == i15) {
                        return ObjectAnimator.ofObject(view, (Property<View, V>) POSITION_PROPERTY, (TypeConverter) null, getPathMotion().getPath(i4, i6, i5, i7));
                    }
                    final ViewBounds viewBounds = new ViewBounds(view);
                    ObjectAnimator objectAnimatorOfObject3 = ObjectAnimator.ofObject(viewBounds, (Property<ViewBounds, V>) TOP_LEFT_PROPERTY, (TypeConverter) null, getPathMotion().getPath(i4, i6, i5, i7));
                    ObjectAnimator objectAnimatorOfObject4 = ObjectAnimator.ofObject(viewBounds, (Property<ViewBounds, V>) BOTTOM_RIGHT_PROPERTY, (TypeConverter) null, getPathMotion().getPath(i8, i10, i9, i2));
                    AnimatorSet animatorSet = new AnimatorSet();
                    animatorSet.playTogether(objectAnimatorOfObject3, objectAnimatorOfObject4);
                    animatorSet.addListener(new AnimatorListenerAdapter() {
                        private ViewBounds mViewBounds;

                        {
                            this.mViewBounds = viewBounds;
                        }
                    });
                    return animatorSet;
                }
                int i16 = i2;
                if (i4 != i5 || i6 != i7) {
                    return ObjectAnimator.ofObject(view, (Property<View, V>) TOP_LEFT_ONLY_PROPERTY, (TypeConverter) null, getPathMotion().getPath(i4, i6, i5, i7));
                }
                return ObjectAnimator.ofObject(view, (Property<View, V>) BOTTOM_RIGHT_ONLY_PROPERTY, (TypeConverter) null, getPathMotion().getPath(i8, i10, i9, i16));
            }
            final int i17 = i2;
            view.setLeftTopRightBottom(i4, i6, Math.max(i12, i14) + i4, Math.max(i13, i15) + i6);
            if (i4 != i5 || i6 != i7) {
                objectAnimatorOfObject = ObjectAnimator.ofObject(view, (Property<View, V>) POSITION_PROPERTY, (TypeConverter) null, getPathMotion().getPath(i4, i6, i5, i7));
            } else {
                objectAnimatorOfObject = null;
            }
            if (rect == null) {
                i3 = 0;
                rect2 = new Rect(0, 0, i12, i13);
            } else {
                i3 = 0;
                rect2 = rect;
            }
            if (rect7 == null) {
                rect3 = new Rect(i3, i3, i14, i15);
            } else {
                rect3 = rect7;
            }
            if (!rect2.equals(rect3)) {
                view.setClipBounds(rect2);
                objectAnimatorOfObject2 = ObjectAnimator.ofObject(view, "clipBounds", sRectEvaluator, rect2, rect3);
                objectAnimatorOfObject2.addListener(new AnimatorListenerAdapter() {
                    private boolean mIsCanceled;

                    @Override
                    public void onAnimationCancel(Animator animator) {
                        this.mIsCanceled = true;
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        if (!this.mIsCanceled) {
                            view.setClipBounds(rect7);
                            view.setLeftTopRightBottom(i5, i7, i9, i17);
                        }
                    }
                });
            } else {
                objectAnimatorOfObject2 = null;
            }
            return TransitionUtils.mergeAnimators(objectAnimatorOfObject, objectAnimatorOfObject2);
        }
        viewGroup.getLocationInWindow(this.tempLocation);
        int iIntValue = ((Integer) transitionValues.values.get(PROPNAME_WINDOW_X)).intValue() - this.tempLocation[0];
        int iIntValue2 = ((Integer) transitionValues.values.get(PROPNAME_WINDOW_Y)).intValue() - this.tempLocation[1];
        int iIntValue3 = ((Integer) transitionValues2.values.get(PROPNAME_WINDOW_X)).intValue() - this.tempLocation[0];
        int iIntValue4 = ((Integer) transitionValues2.values.get(PROPNAME_WINDOW_Y)).intValue() - this.tempLocation[1];
        if (iIntValue != iIntValue3 || iIntValue2 != iIntValue4) {
            int width = view.getWidth();
            int height = view.getHeight();
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            view.draw(new Canvas(bitmapCreateBitmap));
            final BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmapCreateBitmap);
            bitmapDrawable.setBounds(iIntValue, iIntValue2, width + iIntValue, height + iIntValue2);
            final float transitionAlpha = view.getTransitionAlpha();
            view.setTransitionAlpha(0.0f);
            viewGroup.getOverlay().add(bitmapDrawable);
            ObjectAnimator objectAnimatorOfPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(bitmapDrawable, PropertyValuesHolder.ofObject(DRAWABLE_ORIGIN_PROPERTY, (TypeConverter) null, getPathMotion().getPath(iIntValue, iIntValue2, iIntValue3, iIntValue4)));
            objectAnimatorOfPropertyValuesHolder.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    viewGroup.getOverlay().remove(bitmapDrawable);
                    view.setTransitionAlpha(transitionAlpha);
                }
            });
            return objectAnimatorOfPropertyValuesHolder;
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

        public ViewBounds(View view) {
            this.mView = view;
        }

        public void setTopLeft(PointF pointF) {
            this.mLeft = Math.round(pointF.x);
            this.mTop = Math.round(pointF.y);
            this.mTopLeftCalls++;
            if (this.mTopLeftCalls == this.mBottomRightCalls) {
                setLeftTopRightBottom();
            }
        }

        public void setBottomRight(PointF pointF) {
            this.mRight = Math.round(pointF.x);
            this.mBottom = Math.round(pointF.y);
            this.mBottomRightCalls++;
            if (this.mTopLeftCalls == this.mBottomRightCalls) {
                setLeftTopRightBottom();
            }
        }

        private void setLeftTopRightBottom() {
            this.mView.setLeftTopRightBottom(this.mLeft, this.mTop, this.mRight, this.mBottom);
            this.mTopLeftCalls = 0;
            this.mBottomRightCalls = 0;
        }
    }
}
