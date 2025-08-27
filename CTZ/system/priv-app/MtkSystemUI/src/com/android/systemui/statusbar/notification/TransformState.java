package com.android.systemui.statusbar.notification;

import android.util.Pools;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.internal.widget.MessagingImageMessage;
import com.android.internal.widget.MessagingPropertyAnimator;
import com.android.internal.widget.ViewClippingUtil;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.TransformableView;
import com.android.systemui.statusbar.ViewTransformationHelper;

/* loaded from: classes.dex */
public class TransformState {
    private boolean mSameAsAny;
    protected TransformInfo mTransformInfo;
    protected View mTransformedView;
    private static Pools.SimplePool<TransformState> sInstancePool = new Pools.SimplePool<>(40);
    private static ViewClippingUtil.ClippingParameters CLIPPING_PARAMETERS = new ViewClippingUtil.ClippingParameters() { // from class: com.android.systemui.statusbar.notification.TransformState.1
        AnonymousClass1() {
        }

        public boolean shouldFinish(View view) {
            if (view instanceof ExpandableNotificationRow) {
                return !((ExpandableNotificationRow) view).isChildInGroup();
            }
            return false;
        }

        public void onClippingStateChanged(View view, boolean z) {
            if (view instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
                if (z) {
                    expandableNotificationRow.setClipToActualHeight(true);
                } else if (expandableNotificationRow.isChildInGroup()) {
                    expandableNotificationRow.setClipToActualHeight(false);
                }
            }
        }
    };
    private int[] mOwnPosition = new int[2];
    private float mTransformationEndY = -1.0f;
    private float mTransformationEndX = -1.0f;
    protected Interpolator mDefaultInterpolator = Interpolators.FAST_OUT_SLOW_IN;

    public interface TransformInfo {
        boolean isAnimating();
    }

    /* renamed from: com.android.systemui.statusbar.notification.TransformState$1 */
    class AnonymousClass1 implements ViewClippingUtil.ClippingParameters {
        AnonymousClass1() {
        }

        public boolean shouldFinish(View view) {
            if (view instanceof ExpandableNotificationRow) {
                return !((ExpandableNotificationRow) view).isChildInGroup();
            }
            return false;
        }

        public void onClippingStateChanged(View view, boolean z) {
            if (view instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
                if (z) {
                    expandableNotificationRow.setClipToActualHeight(true);
                } else if (expandableNotificationRow.isChildInGroup()) {
                    expandableNotificationRow.setClipToActualHeight(false);
                }
            }
        }
    }

    public void initFrom(View view, TransformInfo transformInfo) {
        this.mTransformedView = view;
        this.mTransformInfo = transformInfo;
    }

    public void transformViewFrom(TransformState transformState, float f) {
        this.mTransformedView.animate().cancel();
        if (sameAs(transformState)) {
            ensureVisible();
        } else {
            CrossFadeHelper.fadeIn(this.mTransformedView, f);
        }
        transformViewFullyFrom(transformState, f);
    }

    protected void ensureVisible() {
        if (this.mTransformedView.getVisibility() == 4 || this.mTransformedView.getAlpha() != 1.0f) {
            this.mTransformedView.setAlpha(1.0f);
            this.mTransformedView.setVisibility(0);
        }
    }

    public void transformViewFullyFrom(TransformState transformState, float f) {
        transformViewFrom(transformState, 17, null, f);
    }

    public void transformViewFullyFrom(TransformState transformState, ViewTransformationHelper.CustomTransformation customTransformation, float f) {
        transformViewFrom(transformState, 17, customTransformation, f);
    }

    public void transformViewVerticalFrom(TransformState transformState, ViewTransformationHelper.CustomTransformation customTransformation, float f) {
        transformViewFrom(transformState, 16, customTransformation, f);
    }

    public void transformViewVerticalFrom(TransformState transformState, float f) {
        transformViewFrom(transformState, 16, null, f);
    }

    /* JADX WARN: Removed duplicated region for block: B:167:0x00f0  */
    /* JADX WARN: Removed duplicated region for block: B:169:0x00f5  */
    /* JADX WARN: Removed duplicated region for block: B:171:0x00fa  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    protected void transformViewFrom(TransformState transformState, int i, ViewTransformationHelper.CustomTransformation customTransformation, float f) {
        int[] locationOnScreen;
        float f2;
        float interpolation;
        Interpolator customInterpolator;
        float interpolation2;
        Interpolator customInterpolator2;
        View view = this.mTransformedView;
        boolean z = (i & 1) != 0;
        boolean z2 = (i & 16) != 0;
        int viewHeight = getViewHeight();
        int viewHeight2 = transformState.getViewHeight();
        boolean z3 = (viewHeight2 == viewHeight || viewHeight2 == 0 || viewHeight == 0) ? false : true;
        int viewWidth = getViewWidth();
        int viewWidth2 = transformState.getViewWidth();
        boolean z4 = (viewWidth2 == viewWidth || viewWidth2 == 0 || viewWidth == 0) ? false : true;
        boolean z5 = transformScale(transformState) && (z3 || z4);
        if (f == 0.0f || ((z && getTransformationStartX() == -1.0f) || ((z2 && getTransformationStartY() == -1.0f) || ((z5 && getTransformationStartScaleX() == -1.0f && z4) || (z5 && getTransformationStartScaleY() == -1.0f && z3))))) {
            if (f != 0.0f) {
                locationOnScreen = transformState.getLaidOutLocationOnScreen();
            } else {
                locationOnScreen = transformState.getLocationOnScreen();
            }
            int[] laidOutLocationOnScreen = getLaidOutLocationOnScreen();
            if (customTransformation == null || !customTransformation.initTransformation(this, transformState)) {
                if (z) {
                    setTransformationStartX(locationOnScreen[0] - laidOutLocationOnScreen[0]);
                }
                if (z2) {
                    setTransformationStartY(locationOnScreen[1] - laidOutLocationOnScreen[1]);
                }
                View transformedView = transformState.getTransformedView();
                if (z5 && z4) {
                    setTransformationStartScaleX((viewWidth2 * transformedView.getScaleX()) / viewWidth);
                    view.setPivotX(0.0f);
                } else {
                    setTransformationStartScaleX(-1.0f);
                }
                if (z5 && z3) {
                    setTransformationStartScaleY((viewHeight2 * transformedView.getScaleY()) / viewHeight);
                    view.setPivotY(0.0f);
                    f2 = -1.0f;
                    if (!z) {
                    }
                    if (!z2) {
                    }
                    if (!z5) {
                    }
                    setClippingDeactivated(view, true);
                } else {
                    f2 = -1.0f;
                    setTransformationStartScaleY(-1.0f);
                    if (!z) {
                        setTransformationStartX(f2);
                    }
                    if (!z2) {
                        setTransformationStartY(f2);
                    }
                    if (!z5) {
                        setTransformationStartScaleX(f2);
                        setTransformationStartScaleY(f2);
                    }
                    setClippingDeactivated(view, true);
                }
            } else {
                f2 = -1.0f;
                if (!z) {
                }
                if (!z2) {
                }
                if (!z5) {
                }
                setClippingDeactivated(view, true);
            }
        }
        float interpolation3 = this.mDefaultInterpolator.getInterpolation(f);
        if (z) {
            if (customTransformation != null && (customInterpolator2 = customTransformation.getCustomInterpolator(1, true)) != null) {
                interpolation2 = customInterpolator2.getInterpolation(f);
            } else {
                interpolation2 = interpolation3;
            }
            view.setTranslationX(NotificationUtils.interpolate(getTransformationStartX(), 0.0f, interpolation2));
        }
        if (z2) {
            if (customTransformation != null && (customInterpolator = customTransformation.getCustomInterpolator(16, true)) != null) {
                interpolation = customInterpolator.getInterpolation(f);
            } else {
                interpolation = interpolation3;
            }
            view.setTranslationY(NotificationUtils.interpolate(getTransformationStartY(), 0.0f, interpolation));
        }
        if (z5) {
            float transformationStartScaleX = getTransformationStartScaleX();
            if (transformationStartScaleX != -1.0f) {
                view.setScaleX(NotificationUtils.interpolate(transformationStartScaleX, 1.0f, interpolation3));
            }
            float transformationStartScaleY = getTransformationStartScaleY();
            if (transformationStartScaleY != -1.0f) {
                view.setScaleY(NotificationUtils.interpolate(transformationStartScaleY, 1.0f, interpolation3));
            }
        }
    }

    protected int getViewWidth() {
        return this.mTransformedView.getWidth();
    }

    protected int getViewHeight() {
        return this.mTransformedView.getHeight();
    }

    protected boolean transformScale(TransformState transformState) {
        return false;
    }

    public boolean transformViewTo(TransformState transformState, float f) {
        this.mTransformedView.animate().cancel();
        if (sameAs(transformState)) {
            if (this.mTransformedView.getVisibility() == 0) {
                this.mTransformedView.setAlpha(0.0f);
                this.mTransformedView.setVisibility(4);
                return false;
            }
            return false;
        }
        CrossFadeHelper.fadeOut(this.mTransformedView, f);
        transformViewFullyTo(transformState, f);
        return true;
    }

    public void transformViewFullyTo(TransformState transformState, float f) {
        transformViewTo(transformState, 17, null, f);
    }

    public void transformViewFullyTo(TransformState transformState, ViewTransformationHelper.CustomTransformation customTransformation, float f) {
        transformViewTo(transformState, 17, customTransformation, f);
    }

    public void transformViewVerticalTo(TransformState transformState, ViewTransformationHelper.CustomTransformation customTransformation, float f) {
        transformViewTo(transformState, 16, customTransformation, f);
    }

    public void transformViewVerticalTo(TransformState transformState, float f) {
        transformViewTo(transformState, 16, null, f);
    }

    /* JADX WARN: Removed duplicated region for block: B:115:0x00b4 A[PHI: r6
  0x00b4: PHI (r6v8 float) = (r6v7 float), (r6v11 float) binds: [B:108:0x009e, B:113:0x00ad] A[DONT_GENERATE, DONT_INLINE]] */
    /* JADX WARN: Removed duplicated region for block: B:126:0x00df A[PHI: r2
  0x00df: PHI (r2v9 float) = (r2v8 float), (r2v12 float) binds: [B:119:0x00c9, B:124:0x00d8] A[DONT_GENERATE, DONT_INLINE]] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void transformViewTo(TransformState transformState, int i, ViewTransformationHelper.CustomTransformation customTransformation, float f) {
        float interpolation;
        float interpolation2;
        View view = this.mTransformedView;
        boolean z = (i & 1) != 0;
        boolean z2 = (i & 16) != 0;
        boolean zTransformScale = transformScale(transformState);
        if (f == 0.0f) {
            if (z) {
                float transformationStartX = getTransformationStartX();
                if (transformationStartX == -1.0f) {
                    transformationStartX = view.getTranslationX();
                }
                setTransformationStartX(transformationStartX);
            }
            if (z2) {
                float transformationStartY = getTransformationStartY();
                if (transformationStartY == -1.0f) {
                    transformationStartY = view.getTranslationY();
                }
                setTransformationStartY(transformationStartY);
            }
            transformState.getTransformedView();
            if (zTransformScale && transformState.getViewWidth() != getViewWidth()) {
                setTransformationStartScaleX(view.getScaleX());
                view.setPivotX(0.0f);
            } else {
                setTransformationStartScaleX(-1.0f);
            }
            if (zTransformScale && transformState.getViewHeight() != getViewHeight()) {
                setTransformationStartScaleY(view.getScaleY());
                view.setPivotY(0.0f);
            } else {
                setTransformationStartScaleY(-1.0f);
            }
            setClippingDeactivated(view, true);
        }
        float interpolation3 = this.mDefaultInterpolator.getInterpolation(f);
        int[] laidOutLocationOnScreen = transformState.getLaidOutLocationOnScreen();
        int[] laidOutLocationOnScreen2 = getLaidOutLocationOnScreen();
        if (z) {
            float f2 = laidOutLocationOnScreen[0] - laidOutLocationOnScreen2[0];
            if (customTransformation != null) {
                if (customTransformation.customTransformTarget(this, transformState)) {
                    f2 = this.mTransformationEndX;
                }
                Interpolator customInterpolator = customTransformation.getCustomInterpolator(1, false);
                if (customInterpolator != null) {
                    interpolation2 = customInterpolator.getInterpolation(f);
                }
                view.setTranslationX(NotificationUtils.interpolate(getTransformationStartX(), f2, interpolation2));
            } else {
                interpolation2 = interpolation3;
                view.setTranslationX(NotificationUtils.interpolate(getTransformationStartX(), f2, interpolation2));
            }
        }
        if (z2) {
            float f3 = laidOutLocationOnScreen[1] - laidOutLocationOnScreen2[1];
            if (customTransformation != null) {
                if (customTransformation.customTransformTarget(this, transformState)) {
                    f3 = this.mTransformationEndY;
                }
                Interpolator customInterpolator2 = customTransformation.getCustomInterpolator(16, false);
                if (customInterpolator2 != null) {
                    interpolation = customInterpolator2.getInterpolation(f);
                }
                view.setTranslationY(NotificationUtils.interpolate(getTransformationStartY(), f3, interpolation));
            } else {
                interpolation = interpolation3;
                view.setTranslationY(NotificationUtils.interpolate(getTransformationStartY(), f3, interpolation));
            }
        }
        if (zTransformScale) {
            transformState.getTransformedView();
            float transformationStartScaleX = getTransformationStartScaleX();
            if (transformationStartScaleX != -1.0f) {
                view.setScaleX(NotificationUtils.interpolate(transformationStartScaleX, transformState.getViewWidth() / getViewWidth(), interpolation3));
            }
            float transformationStartScaleY = getTransformationStartScaleY();
            if (transformationStartScaleY != -1.0f) {
                view.setScaleY(NotificationUtils.interpolate(transformationStartScaleY, transformState.getViewHeight() / getViewHeight(), interpolation3));
            }
        }
    }

    protected void setClippingDeactivated(View view, boolean z) {
        ViewClippingUtil.setClippingDeactivated(view, z, CLIPPING_PARAMETERS);
    }

    public int[] getLaidOutLocationOnScreen() {
        int[] locationOnScreen = getLocationOnScreen();
        locationOnScreen[0] = (int) (locationOnScreen[0] - this.mTransformedView.getTranslationX());
        locationOnScreen[1] = (int) (locationOnScreen[1] - this.mTransformedView.getTranslationY());
        return locationOnScreen;
    }

    public int[] getLocationOnScreen() {
        this.mTransformedView.getLocationOnScreen(this.mOwnPosition);
        this.mOwnPosition[0] = (int) (r0[0] - ((1.0f - this.mTransformedView.getScaleX()) * this.mTransformedView.getPivotX()));
        this.mOwnPosition[1] = (int) (r0[1] - ((1.0f - this.mTransformedView.getScaleY()) * this.mTransformedView.getPivotY()));
        int[] iArr = this.mOwnPosition;
        iArr[1] = iArr[1] - (MessagingPropertyAnimator.getTop(this.mTransformedView) - MessagingPropertyAnimator.getLayoutTop(this.mTransformedView));
        return this.mOwnPosition;
    }

    protected boolean sameAs(TransformState transformState) {
        return this.mSameAsAny;
    }

    public void appear(float f, TransformableView transformableView) {
        if (f == 0.0f) {
            prepareFadeIn();
        }
        CrossFadeHelper.fadeIn(this.mTransformedView, f);
    }

    public void disappear(float f, TransformableView transformableView) {
        CrossFadeHelper.fadeOut(this.mTransformedView, f);
    }

    public static TransformState createFrom(View view, TransformInfo transformInfo) {
        if (view instanceof TextView) {
            TextViewTransformState textViewTransformStateObtain = TextViewTransformState.obtain();
            textViewTransformStateObtain.initFrom(view, transformInfo);
            return textViewTransformStateObtain;
        }
        if (view.getId() == 16908685) {
            ActionListTransformState actionListTransformStateObtain = ActionListTransformState.obtain();
            actionListTransformStateObtain.initFrom(view, transformInfo);
            return actionListTransformStateObtain;
        }
        if (view.getId() == 16909125) {
            MessagingLayoutTransformState messagingLayoutTransformStateObtain = MessagingLayoutTransformState.obtain();
            messagingLayoutTransformStateObtain.initFrom(view, transformInfo);
            return messagingLayoutTransformStateObtain;
        }
        if (view instanceof MessagingImageMessage) {
            MessagingImageTransformState messagingImageTransformStateObtain = MessagingImageTransformState.obtain();
            messagingImageTransformStateObtain.initFrom(view, transformInfo);
            return messagingImageTransformStateObtain;
        }
        if (view instanceof ImageView) {
            ImageTransformState imageTransformStateObtain = ImageTransformState.obtain();
            imageTransformStateObtain.initFrom(view, transformInfo);
            if (view.getId() == 16909229) {
                imageTransformStateObtain.setIsSameAsAnyView(true);
            }
            return imageTransformStateObtain;
        }
        if (view instanceof ProgressBar) {
            ProgressTransformState progressTransformStateObtain = ProgressTransformState.obtain();
            progressTransformStateObtain.initFrom(view, transformInfo);
            return progressTransformStateObtain;
        }
        TransformState transformStateObtain = obtain();
        transformStateObtain.initFrom(view, transformInfo);
        return transformStateObtain;
    }

    public void setIsSameAsAnyView(boolean z) {
        this.mSameAsAny = z;
    }

    public void recycle() {
        reset();
        if (getClass() == TransformState.class) {
            sInstancePool.release(this);
        }
    }

    public void setTransformationEndY(float f) {
        this.mTransformationEndY = f;
    }

    public float getTransformationStartX() {
        Object tag = this.mTransformedView.getTag(R.id.transformation_start_x_tag);
        if (tag == null) {
            return -1.0f;
        }
        return ((Float) tag).floatValue();
    }

    public float getTransformationStartY() {
        Object tag = this.mTransformedView.getTag(R.id.transformation_start_y_tag);
        if (tag == null) {
            return -1.0f;
        }
        return ((Float) tag).floatValue();
    }

    public float getTransformationStartScaleX() {
        Object tag = this.mTransformedView.getTag(R.id.transformation_start_scale_x_tag);
        if (tag == null) {
            return -1.0f;
        }
        return ((Float) tag).floatValue();
    }

    public float getTransformationStartScaleY() {
        Object tag = this.mTransformedView.getTag(R.id.transformation_start_scale_y_tag);
        if (tag == null) {
            return -1.0f;
        }
        return ((Float) tag).floatValue();
    }

    public void setTransformationStartX(float f) {
        this.mTransformedView.setTag(R.id.transformation_start_x_tag, Float.valueOf(f));
    }

    public void setTransformationStartY(float f) {
        this.mTransformedView.setTag(R.id.transformation_start_y_tag, Float.valueOf(f));
    }

    private void setTransformationStartScaleX(float f) {
        this.mTransformedView.setTag(R.id.transformation_start_scale_x_tag, Float.valueOf(f));
    }

    private void setTransformationStartScaleY(float f) {
        this.mTransformedView.setTag(R.id.transformation_start_scale_y_tag, Float.valueOf(f));
    }

    protected void reset() {
        this.mTransformedView = null;
        this.mTransformInfo = null;
        this.mSameAsAny = false;
        this.mTransformationEndX = -1.0f;
        this.mTransformationEndY = -1.0f;
        this.mDefaultInterpolator = Interpolators.FAST_OUT_SLOW_IN;
    }

    public void setVisible(boolean z, boolean z2) {
        if (z2 || this.mTransformedView.getVisibility() != 8) {
            if (this.mTransformedView.getVisibility() != 8) {
                this.mTransformedView.setVisibility(z ? 0 : 4);
            }
            this.mTransformedView.animate().cancel();
            this.mTransformedView.setAlpha(z ? 1.0f : 0.0f);
            resetTransformedView();
        }
    }

    public void prepareFadeIn() {
        resetTransformedView();
    }

    protected void resetTransformedView() {
        this.mTransformedView.setTranslationX(0.0f);
        this.mTransformedView.setTranslationY(0.0f);
        this.mTransformedView.setScaleX(1.0f);
        this.mTransformedView.setScaleY(1.0f);
        setClippingDeactivated(this.mTransformedView, false);
        abortTransformation();
    }

    public void abortTransformation() {
        this.mTransformedView.setTag(R.id.transformation_start_x_tag, Float.valueOf(-1.0f));
        this.mTransformedView.setTag(R.id.transformation_start_y_tag, Float.valueOf(-1.0f));
        this.mTransformedView.setTag(R.id.transformation_start_scale_x_tag, Float.valueOf(-1.0f));
        this.mTransformedView.setTag(R.id.transformation_start_scale_y_tag, Float.valueOf(-1.0f));
    }

    public static TransformState obtain() {
        TransformState transformState = (TransformState) sInstancePool.acquire();
        if (transformState != null) {
            return transformState;
        }
        return new TransformState();
    }

    public View getTransformedView() {
        return this.mTransformedView;
    }

    public void setDefaultInterpolator(Interpolator interpolator) {
        this.mDefaultInterpolator = interpolator;
    }
}
