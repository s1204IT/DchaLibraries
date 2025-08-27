package com.android.systemui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.util.leak.RotationUtils;

/* loaded from: classes.dex */
public class HardwareUiLayout extends FrameLayout implements TunerService.Tunable {
    private Animator mAnimator;
    private HardwareBgDrawable mBackground;
    private View mChild;
    private boolean mCollapse;
    private View mDivision;
    private boolean mEdgeBleed;
    private int mEndPoint;
    private boolean mHasOutsideTouch;
    private final ViewTreeObserver.OnComputeInternalInsetsListener mInsetsListener;
    private int mOldHeight;
    private boolean mRotatedBackground;
    private int mRotation;
    private boolean mRoundedDivider;
    private boolean mSwapOrientation;
    private final int[] mTmp2;

    public HardwareUiLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mTmp2 = new int[2];
        this.mRotation = 0;
        this.mSwapOrientation = true;
        this.mInsetsListener = new ViewTreeObserver.OnComputeInternalInsetsListener() { // from class: com.android.systemui.-$$Lambda$HardwareUiLayout$Wopid983i_OFN_0DzaqL8EnwZHc
            public final void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo internalInsetsInfo) {
                HardwareUiLayout.lambda$new$5(this.f$0, internalInsetsInfo);
            }
        };
        updateSettings();
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateSettings();
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "sysui_hwui_edge_bleed", "sysui_hwui_rounded_divider");
        getViewTreeObserver().addOnComputeInternalInsetsListener(this.mInsetsListener);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnComputeInternalInsetsListener(this.mInsetsListener);
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
    }

    @Override // com.android.systemui.tuner.TunerService.Tunable
    public void onTuningChanged(String str, String str2) {
        updateSettings();
    }

    private void updateSettings() {
        this.mEdgeBleed = Settings.Secure.getInt(getContext().getContentResolver(), "sysui_hwui_edge_bleed", 0) != 0;
        this.mRoundedDivider = Settings.Secure.getInt(getContext().getContentResolver(), "sysui_hwui_rounded_divider", 0) != 0;
        updateEdgeMargin(this.mEdgeBleed ? 0 : getEdgePadding());
        this.mBackground = new HardwareBgDrawable(this.mRoundedDivider, true ^ this.mEdgeBleed, getContext());
        if (this.mChild != null) {
            this.mChild.setBackground(this.mBackground);
            requestLayout();
        }
    }

    private void updateEdgeMargin(int i) {
        if (this.mChild != null) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.mChild.getLayoutParams();
            if (this.mRotation == 1) {
                marginLayoutParams.topMargin = i;
            } else if (this.mRotation == 2) {
                marginLayoutParams.bottomMargin = i;
            } else {
                marginLayoutParams.rightMargin = i;
            }
            this.mChild.setLayoutParams(marginLayoutParams);
        }
    }

    private int getEdgePadding() {
        return getContext().getResources().getDimensionPixelSize(R.dimen.edge_margin);
    }

    @Override // android.widget.FrameLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        if (this.mChild == null) {
            if (getChildCount() != 0) {
                this.mChild = getChildAt(0);
                this.mChild.setBackground(this.mBackground);
                updateEdgeMargin(this.mEdgeBleed ? 0 : getEdgePadding());
                this.mOldHeight = this.mChild.getMeasuredHeight();
                this.mChild.addOnLayoutChangeListener(new View.OnLayoutChangeListener() { // from class: com.android.systemui.-$$Lambda$HardwareUiLayout$iXisPfYv_BRDXTMSPDe1D5AA0Wo
                    @Override // android.view.View.OnLayoutChangeListener
                    public final void onLayoutChange(View view, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10) {
                        this.f$0.updatePosition();
                    }
                });
                updateRotation();
            } else {
                return;
            }
        }
        int measuredHeight = this.mChild.getMeasuredHeight();
        if (measuredHeight != this.mOldHeight) {
            animateChild(this.mOldHeight, measuredHeight);
        }
        post(new Runnable() { // from class: com.android.systemui.-$$Lambda$HardwareUiLayout$zQ_qVVlFo_33izIMpEk2X8p8Su8
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.updatePosition();
            }
        });
    }

    @Override // android.view.View
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateRotation();
    }

    private void updateRotation() {
        int rotation = RotationUtils.getRotation(getContext());
        if (rotation != this.mRotation) {
            rotate(this.mRotation, rotation);
            this.mRotation = rotation;
        }
    }

    private void rotate(int i, int i2) {
        if (i != 0 && i2 != 0) {
            rotate(i, 0);
            rotate(0, i2);
            return;
        }
        if (i == 1 || i2 == 2) {
            rotateRight();
        } else {
            rotateLeft();
        }
        if (i2 != 0) {
            if (this.mChild instanceof LinearLayout) {
                this.mRotatedBackground = true;
                this.mBackground.setRotatedBackground(true);
                LinearLayout linearLayout = (LinearLayout) this.mChild;
                if (this.mSwapOrientation) {
                    linearLayout.setOrientation(0);
                }
                swapDimens(this.mChild);
                return;
            }
            return;
        }
        if (this.mChild instanceof LinearLayout) {
            this.mRotatedBackground = false;
            this.mBackground.setRotatedBackground(false);
            LinearLayout linearLayout2 = (LinearLayout) this.mChild;
            if (this.mSwapOrientation) {
                linearLayout2.setOrientation(1);
            }
            swapDimens(this.mChild);
        }
    }

    private void rotateRight() {
        rotateRight(this);
        rotateRight(this.mChild);
        swapDimens(this);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.mChild.getLayoutParams();
        layoutParams.gravity = rotateGravityRight(layoutParams.gravity);
        this.mChild.setLayoutParams(layoutParams);
    }

    private void swapDimens(View view) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        int i = layoutParams.width;
        layoutParams.width = layoutParams.height;
        layoutParams.height = i;
        view.setLayoutParams(layoutParams);
    }

    private int rotateGravityRight(int i) {
        int absoluteGravity = Gravity.getAbsoluteGravity(i, getLayoutDirection());
        int i2 = i & com.android.systemui.plugins.R.styleable.AppCompatTheme_windowActionBarOverlay;
        int i3 = absoluteGravity & 7;
        int i4 = i3 != 1 ? i3 != 5 ? 48 : 80 : 16;
        if (i2 == 16) {
            return i4 | 1;
        }
        if (i2 == 80) {
            return i4 | 3;
        }
        return i4 | 5;
    }

    private void rotateLeft() {
        rotateLeft(this);
        rotateLeft(this.mChild);
        swapDimens(this);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.mChild.getLayoutParams();
        layoutParams.gravity = rotateGravityLeft(layoutParams.gravity);
        this.mChild.setLayoutParams(layoutParams);
    }

    private int rotateGravityLeft(int i) {
        if (i == -1) {
            i = 8388659;
        }
        int absoluteGravity = Gravity.getAbsoluteGravity(i, getLayoutDirection());
        int i2 = i & com.android.systemui.plugins.R.styleable.AppCompatTheme_windowActionBarOverlay;
        int i3 = absoluteGravity & 7;
        int i4 = i3 != 1 ? i3 != 5 ? 80 : 48 : 16;
        if (i2 == 16) {
            return i4 | 1;
        }
        if (i2 == 80) {
            return i4 | 5;
        }
        return i4 | 3;
    }

    private void rotateLeft(View view) {
        view.setPadding(view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom(), view.getPaddingLeft());
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        marginLayoutParams.setMargins(marginLayoutParams.topMargin, marginLayoutParams.rightMargin, marginLayoutParams.bottomMargin, marginLayoutParams.leftMargin);
        view.setLayoutParams(marginLayoutParams);
    }

    private void rotateRight(View view) {
        view.setPadding(view.getPaddingBottom(), view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight());
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        marginLayoutParams.setMargins(marginLayoutParams.bottomMargin, marginLayoutParams.leftMargin, marginLayoutParams.topMargin, marginLayoutParams.rightMargin);
        view.setLayoutParams(marginLayoutParams);
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        post(new Runnable() { // from class: com.android.systemui.-$$Lambda$HardwareUiLayout$cC2d-RGmOoAkRlNqsTu1n43qy3A
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.updatePosition();
            }
        });
    }

    private void animateChild(int i, int i2) {
    }

    public void setDivisionView(View view) {
        this.mDivision = view;
        if (this.mDivision != null) {
            this.mDivision.addOnLayoutChangeListener(new View.OnLayoutChangeListener() { // from class: com.android.systemui.-$$Lambda$HardwareUiLayout$2j9eMBfPQJX3xgwLvM_hUGNd8jc
                @Override // android.view.View.OnLayoutChangeListener
                public final void onLayoutChange(View view2, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                    this.f$0.updatePosition();
                }
            });
        }
        updatePosition();
    }

    private void updatePosition() {
        if (this.mChild == null) {
            return;
        }
        if (this.mDivision != null && this.mDivision.getVisibility() == 0) {
            int i = !this.mRotatedBackground ? 1 : 0;
            this.mDivision.getLocationOnScreen(this.mTmp2);
            int translationX = (int) (this.mTmp2[i] + (this.mRotatedBackground ? this.mDivision.getTranslationX() : this.mDivision.getTranslationY()));
            this.mChild.getLocationOnScreen(this.mTmp2);
            setCutPoint(translationX - this.mTmp2[i]);
            return;
        }
        setCutPoint(this.mChild.getMeasuredHeight());
    }

    private void setCutPoint(int i) {
        int cutPoint = this.mBackground.getCutPoint();
        if (cutPoint == i) {
            return;
        }
        if (getAlpha() == 0.0f || cutPoint == 0) {
            this.mBackground.setCutPoint(i);
            return;
        }
        if (this.mAnimator != null) {
            if (this.mEndPoint == i) {
                return;
            } else {
                this.mAnimator.cancel();
            }
        }
        this.mEndPoint = i;
        this.mAnimator = ObjectAnimator.ofInt(this.mBackground, "cutPoint", cutPoint, i);
        if (this.mCollapse) {
            this.mAnimator.setStartDelay(300L);
            this.mCollapse = false;
        }
        this.mAnimator.start();
    }

    @Override // android.view.View
    public ViewOutlineProvider getOutlineProvider() {
        return super.getOutlineProvider();
    }

    public void setOutsideTouchListener(View.OnClickListener onClickListener) {
        this.mHasOutsideTouch = true;
        requestLayout();
        setOnClickListener(onClickListener);
        setClickable(true);
        setFocusable(true);
    }

    public static HardwareUiLayout get(View view) {
        if (view instanceof HardwareUiLayout) {
            return (HardwareUiLayout) view;
        }
        if (view.getParent() instanceof View) {
            return get((View) view.getParent());
        }
        return null;
    }

    public static /* synthetic */ void lambda$new$5(HardwareUiLayout hardwareUiLayout, ViewTreeObserver.InternalInsetsInfo internalInsetsInfo) {
        if (hardwareUiLayout.mHasOutsideTouch || hardwareUiLayout.mChild == null) {
            internalInsetsInfo.setTouchableInsets(0);
        } else {
            internalInsetsInfo.setTouchableInsets(1);
            internalInsetsInfo.contentInsets.set(hardwareUiLayout.mChild.getLeft(), hardwareUiLayout.mChild.getTop(), 0, hardwareUiLayout.getBottom() - hardwareUiLayout.mChild.getBottom());
        }
    }
}
