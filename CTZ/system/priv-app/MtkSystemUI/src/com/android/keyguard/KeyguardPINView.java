package com.android.keyguard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;

/* loaded from: classes.dex */
public class KeyguardPINView extends KeyguardPinBasedInputView {
    private final AppearAnimationUtils mAppearAnimationUtils;
    private ViewGroup mContainer;
    private final DisappearAnimationUtils mDisappearAnimationUtils;
    private final DisappearAnimationUtils mDisappearAnimationUtilsLocked;
    private int mDisappearYTranslation;
    private View mDivider;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private ViewGroup mRow0;
    private ViewGroup mRow1;
    private ViewGroup mRow2;
    private ViewGroup mRow3;
    private View[][] mViews;

    public KeyguardPINView(Context context) {
        this(context, null);
    }

    public KeyguardPINView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mAppearAnimationUtils = new AppearAnimationUtils(context);
        this.mDisappearAnimationUtils = new DisappearAnimationUtils(context, 125L, 0.6f, 0.45f, AnimationUtils.loadInterpolator(this.mContext, android.R.interpolator.fast_out_linear_in));
        this.mDisappearAnimationUtilsLocked = new DisappearAnimationUtils(context, 187L, 0.6f, 0.45f, AnimationUtils.loadInterpolator(this.mContext, android.R.interpolator.fast_out_linear_in));
        this.mDisappearYTranslation = getResources().getDimensionPixelSize(com.android.systemui.R.dimen.disappear_y_translation);
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
    }

    @Override // com.android.keyguard.KeyguardPinBasedInputView, com.android.keyguard.KeyguardAbsKeyInputView
    protected void resetState() {
        super.resetState();
        this.mSecurityMessageDisplay.setMessage("");
    }

    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    protected int getPasswordTextViewId() {
        return com.android.systemui.R.id.pinEntry;
    }

    @Override // com.android.keyguard.KeyguardPinBasedInputView, com.android.keyguard.KeyguardAbsKeyInputView, android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mContainer = (ViewGroup) findViewById(com.android.systemui.R.id.container);
        this.mRow0 = (ViewGroup) findViewById(com.android.systemui.R.id.row0);
        this.mRow1 = (ViewGroup) findViewById(com.android.systemui.R.id.row1);
        this.mRow2 = (ViewGroup) findViewById(com.android.systemui.R.id.row2);
        this.mRow3 = (ViewGroup) findViewById(com.android.systemui.R.id.row3);
        this.mDivider = findViewById(com.android.systemui.R.id.divider);
        this.mViews = new View[][]{new View[]{this.mRow0, null, null}, new View[]{findViewById(com.android.systemui.R.id.key1), findViewById(com.android.systemui.R.id.key2), findViewById(com.android.systemui.R.id.key3)}, new View[]{findViewById(com.android.systemui.R.id.key4), findViewById(com.android.systemui.R.id.key5), findViewById(com.android.systemui.R.id.key6)}, new View[]{findViewById(com.android.systemui.R.id.key7), findViewById(com.android.systemui.R.id.key8), findViewById(com.android.systemui.R.id.key9)}, new View[]{null, findViewById(com.android.systemui.R.id.key0), findViewById(com.android.systemui.R.id.key_enter)}, new View[]{null, this.mEcaView, null}};
        View viewFindViewById = findViewById(com.android.systemui.R.id.cancel_button);
        if (viewFindViewById != null) {
            viewFindViewById.setOnClickListener(new View.OnClickListener() { // from class: com.android.keyguard.-$$Lambda$KeyguardPINView$32q9EwjCzWlJ6lNiw9pw0PSsPxs
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.mCallback.reset();
                }
            });
        }
    }

    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public int getWrongPasswordStringId() {
        return com.android.systemui.R.string.kg_wrong_pin;
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void startAppearAnimation() {
        enableClipping(false);
        setAlpha(1.0f);
        setTranslationY(this.mAppearAnimationUtils.getStartTranslation());
        AppearAnimationUtils.startTranslationYAnimation(this, 0L, 500L, 0.0f, this.mAppearAnimationUtils.getInterpolator());
        this.mAppearAnimationUtils.startAnimation2d(this.mViews, new Runnable() { // from class: com.android.keyguard.KeyguardPINView.1
            @Override // java.lang.Runnable
            public void run() {
                KeyguardPINView.this.enableClipping(true);
            }
        });
    }

    @Override // com.android.keyguard.KeyguardAbsKeyInputView, com.android.keyguard.KeyguardSecurityView
    public boolean startDisappearAnimation(final Runnable runnable) {
        DisappearAnimationUtils disappearAnimationUtils;
        enableClipping(false);
        setTranslationY(0.0f);
        AppearAnimationUtils.startTranslationYAnimation(this, 0L, 280L, this.mDisappearYTranslation, this.mDisappearAnimationUtils.getInterpolator());
        if (this.mKeyguardUpdateMonitor.needsSlowUnlockTransition()) {
            disappearAnimationUtils = this.mDisappearAnimationUtilsLocked;
        } else {
            disappearAnimationUtils = this.mDisappearAnimationUtils;
        }
        disappearAnimationUtils.startAnimation2d(this.mViews, new Runnable() { // from class: com.android.keyguard.KeyguardPINView.2
            @Override // java.lang.Runnable
            public void run() {
                KeyguardPINView.this.enableClipping(true);
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
        return true;
    }

    private void enableClipping(boolean z) {
        this.mContainer.setClipToPadding(z);
        this.mContainer.setClipChildren(z);
        this.mRow1.setClipToPadding(z);
        this.mRow2.setClipToPadding(z);
        this.mRow3.setClipToPadding(z);
        setClipChildren(z);
    }

    @Override // android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }
}
