package com.android.launcher3;

import android.animation.AnimatorSet;
import android.animation.FloatArrayEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;
import com.android.launcher3.DropTarget;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.launcher3.util.Themes;

/* loaded from: classes.dex */
public abstract class ButtonDropTarget extends TextView implements DropTarget, DragController.DragListener, View.OnClickListener {
    private static final int DRAG_VIEW_DROP_DURATION = 285;
    public static final int TOOLTIP_DEFAULT = 0;
    public static final int TOOLTIP_LEFT = 1;
    public static final int TOOLTIP_RIGHT = 2;
    private static final int[] sTempCords = new int[2];
    private boolean mAccessibleDrag;
    protected boolean mActive;
    private int mBottomDragPadding;
    private AnimatorSet mCurrentColorAnim;
    ColorMatrix mCurrentFilter;
    private final int mDragDistanceThreshold;
    protected Drawable mDrawable;
    protected DropTargetBar mDropTargetBar;
    ColorMatrix mDstFilter;
    protected int mHoverColor;
    protected final Launcher mLauncher;
    protected ColorStateList mOriginalTextColor;
    ColorMatrix mSrcFilter;
    protected CharSequence mText;
    private boolean mTextVisible;
    private PopupWindow mToolTip;
    private int mToolTipLocation;

    public abstract void completeDrop(DropTarget.DragObject dragObject);

    public abstract int getAccessibilityAction();

    public abstract LauncherLogProto.Target getDropTargetForLogging();

    public abstract void onAccessibilityDrop(View view, ItemInfo itemInfo);

    public abstract boolean supportsAccessibilityDrop(ItemInfo itemInfo, View view);

    protected abstract boolean supportsDrop(ItemInfo itemInfo);

    public ButtonDropTarget(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ButtonDropTarget(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mHoverColor = 0;
        this.mTextVisible = true;
        this.mLauncher = Launcher.getLauncher(context);
        Resources resources = getResources();
        this.mBottomDragPadding = resources.getDimensionPixelSize(R.dimen.drop_target_drag_padding);
        this.mDragDistanceThreshold = resources.getDimensionPixelSize(R.dimen.drag_distanceThreshold);
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mText = getText();
        this.mOriginalTextColor = getTextColors();
        setContentDescription(this.mText);
    }

    protected void updateText(int i) {
        setText(i);
        this.mText = getText();
        setContentDescription(this.mText);
    }

    protected void setDrawable(int i) {
        if (this.mTextVisible) {
            setCompoundDrawablesRelativeWithIntrinsicBounds(i, 0, 0, 0);
            this.mDrawable = getCompoundDrawablesRelative()[0];
        } else {
            setCompoundDrawablesRelativeWithIntrinsicBounds(0, i, 0, 0);
            this.mDrawable = getCompoundDrawablesRelative()[1];
        }
    }

    public void setDropTargetBar(DropTargetBar dropTargetBar) {
        this.mDropTargetBar = dropTargetBar;
    }

    private void hideTooltip() {
        if (this.mToolTip != null) {
            this.mToolTip.dismiss();
            this.mToolTip = null;
        }
    }

    @Override // com.android.launcher3.DropTarget
    public final void onDragEnter(DropTarget.DragObject dragObject) {
        int i;
        if (!dragObject.accessibleDrag && !this.mTextVisible) {
            hideTooltip();
            TextView textView = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.drop_target_tool_tip, (ViewGroup) null);
            textView.setText(this.mText);
            this.mToolTip = new PopupWindow(textView, -2, -2);
            int measuredWidth = 0;
            if (this.mToolTipLocation != 0) {
                i = -getMeasuredHeight();
                textView.measure(0, 0);
                if (this.mToolTipLocation == 1) {
                    measuredWidth = (-getMeasuredWidth()) - (textView.getMeasuredWidth() / 2);
                } else {
                    measuredWidth = (getMeasuredWidth() / 2) + (textView.getMeasuredWidth() / 2);
                }
            } else {
                i = 0;
            }
            this.mToolTip.showAsDropDown(this, measuredWidth, i);
        }
        dragObject.dragView.setColor(this.mHoverColor);
        animateTextColor(this.mHoverColor);
        if (dragObject.stateAnnouncer != null) {
            dragObject.stateAnnouncer.cancel();
        }
        sendAccessibilityEvent(4);
    }

    @Override // com.android.launcher3.DropTarget
    public void onDragOver(DropTarget.DragObject dragObject) {
    }

    protected void resetHoverColor() {
        animateTextColor(this.mOriginalTextColor.getDefaultColor());
    }

    private void animateTextColor(int i) {
        if (this.mCurrentColorAnim != null) {
            this.mCurrentColorAnim.cancel();
        }
        this.mCurrentColorAnim = new AnimatorSet();
        this.mCurrentColorAnim.setDuration(120L);
        if (this.mSrcFilter == null) {
            this.mSrcFilter = new ColorMatrix();
            this.mDstFilter = new ColorMatrix();
            this.mCurrentFilter = new ColorMatrix();
        }
        int defaultColor = this.mOriginalTextColor.getDefaultColor();
        Themes.setColorChangeOnMatrix(defaultColor, getTextColor(), this.mSrcFilter);
        Themes.setColorChangeOnMatrix(defaultColor, i, this.mDstFilter);
        ValueAnimator valueAnimatorOfObject = ValueAnimator.ofObject(new FloatArrayEvaluator(this.mCurrentFilter.getArray()), this.mSrcFilter.getArray(), this.mDstFilter.getArray());
        valueAnimatorOfObject.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.launcher3.-$$Lambda$ButtonDropTarget$N4YlzUmBPkqf317Di_jCKDNcDyE
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                ButtonDropTarget.lambda$animateTextColor$0(this.f$0, valueAnimator);
            }
        });
        this.mCurrentColorAnim.play(valueAnimatorOfObject);
        this.mCurrentColorAnim.play(ObjectAnimator.ofArgb(this, "textColor", i));
        this.mCurrentColorAnim.start();
    }

    public static /* synthetic */ void lambda$animateTextColor$0(ButtonDropTarget buttonDropTarget, ValueAnimator valueAnimator) {
        buttonDropTarget.mDrawable.setColorFilter(new ColorMatrixColorFilter(buttonDropTarget.mCurrentFilter));
        buttonDropTarget.invalidate();
    }

    @Override // com.android.launcher3.DropTarget
    public final void onDragExit(DropTarget.DragObject dragObject) {
        hideTooltip();
        if (!dragObject.dragComplete) {
            dragObject.dragView.setColor(0);
            resetHoverColor();
        } else {
            dragObject.dragView.setColor(this.mHoverColor);
        }
    }

    @Override // com.android.launcher3.dragndrop.DragController.DragListener
    public void onDragStart(DropTarget.DragObject dragObject, DragOptions dragOptions) {
        this.mActive = supportsDrop(dragObject.dragInfo);
        this.mDrawable.setColorFilter(null);
        if (this.mCurrentColorAnim != null) {
            this.mCurrentColorAnim.cancel();
            this.mCurrentColorAnim = null;
        }
        setTextColor(this.mOriginalTextColor);
        setVisibility(this.mActive ? 0 : 8);
        this.mAccessibleDrag = dragOptions.isAccessibleDrag;
        setOnClickListener(this.mAccessibleDrag ? this : null);
    }

    @Override // com.android.launcher3.DropTarget
    public final boolean acceptDrop(DropTarget.DragObject dragObject) {
        return supportsDrop(dragObject.dragInfo);
    }

    @Override // com.android.launcher3.DropTarget
    public boolean isDropEnabled() {
        return this.mActive && (this.mAccessibleDrag || this.mLauncher.getDragController().getDistanceDragged() >= ((float) this.mDragDistanceThreshold));
    }

    @Override // com.android.launcher3.dragndrop.DragController.DragListener
    public void onDragEnd() {
        this.mActive = false;
        setOnClickListener(null);
    }

    @Override // com.android.launcher3.DropTarget
    public void onDrop(final DropTarget.DragObject dragObject, DragOptions dragOptions) {
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        Rect rect = new Rect();
        dragLayer.getViewRectRelativeToSelf(dragObject.dragView, rect);
        this.mDropTargetBar.deferOnDragEnd();
        dragLayer.animateView(dragObject.dragView, rect, getIconRect(dragObject), r6.width() / rect.width(), 1.0f, 1.0f, 0.1f, 0.1f, DRAG_VIEW_DROP_DURATION, Interpolators.DEACCEL_2, Interpolators.LINEAR, new Runnable() { // from class: com.android.launcher3.-$$Lambda$ButtonDropTarget$qr2_DaqtDn0T6cPZLzHlj54aOQg
            @Override // java.lang.Runnable
            public final void run() {
                ButtonDropTarget.lambda$onDrop$1(this.f$0, dragObject);
            }
        }, 0, null);
    }

    public static /* synthetic */ void lambda$onDrop$1(ButtonDropTarget buttonDropTarget, DropTarget.DragObject dragObject) {
        buttonDropTarget.completeDrop(dragObject);
        buttonDropTarget.mDropTargetBar.onDragEnd();
        buttonDropTarget.mLauncher.getStateManager().goToState(LauncherState.NORMAL);
    }

    @Override // com.android.launcher3.DropTarget
    public void prepareAccessibilityDrop() {
    }

    @Override // com.android.launcher3.DropTarget
    public void getHitRectRelativeToDragLayer(Rect rect) {
        super.getHitRect(rect);
        rect.bottom += this.mBottomDragPadding;
        int[] iArr = sTempCords;
        sTempCords[1] = 0;
        iArr[0] = 0;
        this.mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(this, sTempCords);
        rect.offsetTo(sTempCords[0], sTempCords[1]);
    }

    public Rect getIconRect(DropTarget.DragObject dragObject) {
        int paddingLeft;
        int paddingRight;
        int measuredWidth = dragObject.dragView.getMeasuredWidth();
        int measuredHeight = dragObject.dragView.getMeasuredHeight();
        int intrinsicWidth = this.mDrawable.getIntrinsicWidth();
        int intrinsicHeight = this.mDrawable.getIntrinsicHeight();
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        Rect rect = new Rect();
        dragLayer.getViewRectRelativeToSelf(this, rect);
        if (Utilities.isRtl(getResources())) {
            paddingRight = rect.right - getPaddingRight();
            paddingLeft = paddingRight - intrinsicWidth;
        } else {
            paddingLeft = getPaddingLeft() + rect.left;
            paddingRight = paddingLeft + intrinsicWidth;
        }
        int measuredHeight2 = rect.top + ((getMeasuredHeight() - intrinsicHeight) / 2);
        rect.set(paddingLeft, measuredHeight2, paddingRight, measuredHeight2 + intrinsicHeight);
        rect.offset((-(measuredWidth - intrinsicWidth)) / 2, (-(measuredHeight - intrinsicHeight)) / 2);
        return rect;
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        this.mLauncher.getAccessibilityDelegate().handleAccessibleDrop(this, null, null);
    }

    public int getTextColor() {
        return getTextColors().getDefaultColor();
    }

    public void setTextVisible(boolean z) {
        String str = z ? this.mText : "";
        if (this.mTextVisible != z || !TextUtils.equals(str, getText())) {
            this.mTextVisible = z;
            setText(str);
            if (this.mTextVisible) {
                setCompoundDrawablesRelativeWithIntrinsicBounds(this.mDrawable, (Drawable) null, (Drawable) null, (Drawable) null);
            } else {
                setCompoundDrawablesRelativeWithIntrinsicBounds((Drawable) null, this.mDrawable, (Drawable) null, (Drawable) null);
            }
        }
    }

    public void setToolTipLocation(int i) {
        this.mToolTipLocation = i;
        hideTooltip();
    }

    public boolean isTextTruncated(int i) {
        return !this.mText.equals(TextUtils.ellipsize(this.mText, getPaint(), i - (((getPaddingLeft() + getPaddingRight()) + this.mDrawable.getIntrinsicWidth()) + getCompoundDrawablePadding()), TextUtils.TruncateAt.END));
    }
}
