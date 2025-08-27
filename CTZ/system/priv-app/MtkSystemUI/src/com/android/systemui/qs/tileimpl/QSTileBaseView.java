package com.android.systemui.qs.tileimpl;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import com.android.settingslib.Utils;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSIconView;
import com.android.systemui.plugins.qs.QSTile;

/* loaded from: classes.dex */
public class QSTileBaseView extends com.android.systemui.plugins.qs.QSTileView {
    private String mAccessibilityClass;
    private final ImageView mBg;
    private int mCircleColor;
    private boolean mClicked;
    private boolean mCollapsedView;
    private final int mColorActive;
    private final int mColorDisabled;
    private final int mColorInactive;
    private final H mHandler;
    protected QSIconView mIcon;
    private final FrameLayout mIconFrame;
    protected RippleDrawable mRipple;
    private Drawable mTileBackground;
    private boolean mTileState;

    public QSTileBaseView(Context context, QSIconView qSIconView, boolean z) throws Resources.NotFoundException {
        super(context);
        this.mHandler = new H();
        int dimensionPixelSize = context.getResources().getDimensionPixelSize(R.dimen.qs_quick_tile_padding);
        this.mIconFrame = new FrameLayout(context);
        this.mIconFrame.setForegroundGravity(17);
        int dimensionPixelSize2 = context.getResources().getDimensionPixelSize(R.dimen.qs_quick_tile_size);
        addView(this.mIconFrame, new LinearLayout.LayoutParams(dimensionPixelSize2, dimensionPixelSize2));
        this.mBg = new ImageView(getContext());
        this.mBg.setScaleType(ImageView.ScaleType.FIT_CENTER);
        this.mBg.setImageResource(R.drawable.ic_qs_circle);
        this.mIconFrame.addView(this.mBg);
        this.mIcon = qSIconView;
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2);
        layoutParams.setMargins(0, dimensionPixelSize, 0, dimensionPixelSize);
        this.mIconFrame.addView(this.mIcon, layoutParams);
        this.mIconFrame.setClipChildren(false);
        this.mIconFrame.setClipToPadding(false);
        this.mTileBackground = newTileBackground();
        if (this.mTileBackground instanceof RippleDrawable) {
            setRipple((RippleDrawable) this.mTileBackground);
        }
        setImportantForAccessibility(1);
        setBackground(this.mTileBackground);
        this.mColorActive = Utils.getColorAttr(context, android.R.attr.colorAccent);
        this.mColorDisabled = Utils.getDisabled(context, Utils.getColorAttr(context, android.R.attr.textColorTertiary));
        this.mColorInactive = Utils.getColorAttr(context, android.R.attr.textColorSecondary);
        setPadding(0, 0, 0, 0);
        setClipChildren(false);
        setClipToPadding(false);
        this.mCollapsedView = z;
        setFocusable(true);
    }

    protected Drawable newTileBackground() {
        TypedArray typedArrayObtainStyledAttributes = getContext().obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackgroundBorderless});
        Drawable drawable = typedArrayObtainStyledAttributes.getDrawable(0);
        typedArrayObtainStyledAttributes.recycle();
        return drawable;
    }

    private void setRipple(RippleDrawable rippleDrawable) {
        this.mRipple = rippleDrawable;
        if (getWidth() != 0) {
            updateRippleSize();
        }
    }

    private void updateRippleSize() {
        int measuredWidth = (this.mIconFrame.getMeasuredWidth() / 2) + this.mIconFrame.getLeft();
        int measuredHeight = (this.mIconFrame.getMeasuredHeight() / 2) + this.mIconFrame.getTop();
        int height = (int) (this.mIcon.getHeight() * 0.85f);
        this.mRipple.setHotspotBounds(measuredWidth - height, measuredHeight - height, measuredWidth + height, measuredHeight + height);
    }

    @Override // com.android.systemui.plugins.qs.QSTileView
    public void init(final QSTile qSTile) {
        init(new View.OnClickListener() { // from class: com.android.systemui.qs.tileimpl.-$$Lambda$QSTileBaseView$aVxKNvlJE7IFS8nVmOyLdAcByFA
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                qSTile.click();
            }
        }, new View.OnClickListener() { // from class: com.android.systemui.qs.tileimpl.-$$Lambda$QSTileBaseView$W9w1scJAVZm5V6Q1VB4ZO5o3C8A
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                qSTile.secondaryClick();
            }
        }, new View.OnLongClickListener() { // from class: com.android.systemui.qs.tileimpl.-$$Lambda$QSTileBaseView$STEfvGmwtIL_pMrVYwBQuK3x1jo
            @Override // android.view.View.OnLongClickListener
            public final boolean onLongClick(View view) {
                return QSTileBaseView.lambda$init$2(qSTile, view);
            }
        });
    }

    static /* synthetic */ boolean lambda$init$2(QSTile qSTile, View view) {
        qSTile.longClick();
        return true;
    }

    public void init(View.OnClickListener onClickListener, View.OnClickListener onClickListener2, View.OnLongClickListener onLongClickListener) {
        setOnClickListener(onClickListener);
        setOnLongClickListener(onLongClickListener);
    }

    @Override // android.widget.LinearLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (this.mRipple != null) {
            updateRippleSize();
        }
    }

    @Override // android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override // com.android.systemui.plugins.qs.QSTileView
    public View updateAccessibilityOrder(View view) {
        setAccessibilityTraversalAfter(view.getId());
        return this;
    }

    @Override // com.android.systemui.plugins.qs.QSTileView
    public void onStateChanged(QSTile.State state) {
        this.mHandler.obtainMessage(1, state).sendToTarget();
    }

    protected void handleStateChanged(QSTile.State state) {
        boolean z;
        int circleColor = getCircleColor(state.state);
        if (circleColor != this.mCircleColor) {
            if (this.mBg.isShown() && animationsEnabled()) {
                ValueAnimator duration = ValueAnimator.ofArgb(this.mCircleColor, circleColor).setDuration(350L);
                duration.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.systemui.qs.tileimpl.-$$Lambda$QSTileBaseView$R4RxHhlQ5aUQCBgq0kdDEHJXn14
                    @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                        this.f$0.mBg.setImageTintList(ColorStateList.valueOf(((Integer) valueAnimator.getAnimatedValue()).intValue()));
                    }
                });
                duration.start();
            } else {
                QSIconViewImpl.setTint(this.mBg, circleColor);
            }
            this.mCircleColor = circleColor;
        }
        setClickable(state.state != 0);
        this.mIcon.setIcon(state);
        setContentDescription(state.contentDescription);
        this.mAccessibilityClass = state.expandedAccessibilityClassName;
        if ((state instanceof QSTile.BooleanState) && this.mTileState != (z = ((QSTile.BooleanState) state).value)) {
            this.mClicked = false;
            this.mTileState = z;
        }
    }

    protected boolean animationsEnabled() {
        return true;
    }

    private int getCircleColor(int i) {
        switch (i) {
            case 0:
            case 1:
                return this.mColorDisabled;
            case 2:
                return this.mColorActive;
            default:
                Log.e("QSTileBaseView", "Invalid state " + i);
                return 0;
        }
    }

    @Override // android.view.View
    public void setClickable(boolean z) {
        super.setClickable(z);
        setBackground(z ? this.mRipple : null);
    }

    @Override // com.android.systemui.plugins.qs.QSTileView
    public int getDetailY() {
        return getTop() + (getHeight() / 2);
    }

    @Override // com.android.systemui.plugins.qs.QSTileView
    public QSIconView getIcon() {
        return this.mIcon;
    }

    @Override // com.android.systemui.plugins.qs.QSTileView
    public View getIconWithBackground() {
        return this.mIconFrame;
    }

    @Override // android.view.View
    public boolean performClick() {
        this.mClicked = true;
        return super.performClick();
    }

    @Override // android.view.View
    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        if (!TextUtils.isEmpty(this.mAccessibilityClass)) {
            accessibilityEvent.setClassName(this.mAccessibilityClass);
            if (Switch.class.getName().equals(this.mAccessibilityClass)) {
                boolean z = this.mClicked ? !this.mTileState : this.mTileState;
                accessibilityEvent.setContentDescription(getResources().getString(z ? R.string.switch_bar_on : R.string.switch_bar_off));
                accessibilityEvent.setChecked(z);
            }
        }
    }

    @Override // android.view.View
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        boolean z;
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        if (!TextUtils.isEmpty(this.mAccessibilityClass)) {
            accessibilityNodeInfo.setClassName(this.mAccessibilityClass);
            if (Switch.class.getName().equals(this.mAccessibilityClass)) {
                if (!this.mClicked) {
                    z = this.mTileState;
                } else if (this.mTileState) {
                    z = false;
                } else {
                    z = true;
                }
                accessibilityNodeInfo.setText(getResources().getString(z ? R.string.switch_bar_on : R.string.switch_bar_off));
                accessibilityNodeInfo.setChecked(z);
                accessibilityNodeInfo.setCheckable(true);
                accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK.getId(), getResources().getString(R.string.accessibility_long_click_tile)));
            }
        }
    }

    private class H extends Handler {
        public H() {
            super(Looper.getMainLooper());
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            if (message.what == 1) {
                QSTileBaseView.this.handleStateChanged((QSTile.State) message.obj);
            }
        }
    }
}
