package com.android.launcher3.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import com.android.launcher3.DropTarget;
import com.android.launcher3.Insettable;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherAppWidgetHost;
import com.android.launcher3.R;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.launcher3.views.RecyclerViewFastScroller;
import com.android.launcher3.views.TopRoundedCornerView;

/* loaded from: classes.dex */
public class WidgetsFullSheet extends BaseWidgetSheet implements Insettable, LauncherAppWidgetHost.ProviderChangedListener {
    private static final long DEFAULT_OPEN_DURATION = 267;
    private static final long FADE_IN_DURATION = 150;
    private static final float VERTICAL_START_POSITION = 0.3f;
    private final WidgetsListAdapter mAdapter;
    private final Rect mInsets;
    private WidgetsRecyclerView mRecyclerView;

    @Override // com.android.launcher3.widget.BaseWidgetSheet, com.android.launcher3.logging.UserEventDispatcher.LogContainerProvider
    public /* bridge */ /* synthetic */ void fillInLogContainerData(View view, ItemInfo itemInfo, LauncherLogProto.Target target, LauncherLogProto.Target target2) {
        super.fillInLogContainerData(view, itemInfo, target, target2);
    }

    @Override // com.android.launcher3.widget.BaseWidgetSheet, com.android.launcher3.DragSource
    public /* bridge */ /* synthetic */ void onDropCompleted(View view, DropTarget.DragObject dragObject, boolean z) {
        super.onDropCompleted(view, dragObject, z);
    }

    public WidgetsFullSheet(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mInsets = new Rect();
        LauncherAppState launcherAppState = LauncherAppState.getInstance(context);
        this.mAdapter = new WidgetsListAdapter(context, LayoutInflater.from(context), launcherAppState.getWidgetCache(), launcherAppState.getIconCache(), this, this);
    }

    public WidgetsFullSheet(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mContent = findViewById(R.id.container);
        this.mRecyclerView = (WidgetsRecyclerView) findViewById(R.id.widgets_list_view);
        this.mRecyclerView.setAdapter(this.mAdapter);
        this.mAdapter.setApplyBitmapDeferred(true, this.mRecyclerView);
        TopRoundedCornerView topRoundedCornerView = (TopRoundedCornerView) this.mContent;
        topRoundedCornerView.addSpringView(R.id.widgets_list_view);
        this.mRecyclerView.setEdgeEffectFactory(topRoundedCornerView.createEdgeEffectFactory());
        onWidgetsBound();
    }

    @Override // com.android.launcher3.AbstractFloatingView
    protected Pair<View, String> getAccessibilityTarget() {
        return Pair.create(this.mRecyclerView, getContext().getString(this.mIsOpen ? R.string.widgets_list : R.string.widgets_list_closed));
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mLauncher.getAppWidgetHost().addProviderChangeListener(this);
        notifyWidgetProvidersChanged();
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mLauncher.getAppWidgetHost().removeProviderChangeListener(this);
    }

    @Override // com.android.launcher3.Insettable
    public void setInsets(Rect rect) {
        this.mInsets.set(rect);
        this.mRecyclerView.setPadding(this.mRecyclerView.getPaddingLeft(), this.mRecyclerView.getPaddingTop(), this.mRecyclerView.getPaddingRight(), rect.bottom);
        if (rect.bottom > 0) {
            setupNavBarColor();
        } else {
            clearNavBarColor();
        }
        ((TopRoundedCornerView) this.mContent).setNavBarScrimHeight(this.mInsets.bottom);
        requestLayout();
    }

    @Override // android.widget.LinearLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        int iMax;
        if (this.mInsets.bottom > 0) {
            iMax = 0;
        } else {
            Rect rect = this.mLauncher.getDeviceProfile().workspacePadding;
            iMax = Math.max(rect.left + rect.right, 2 * (this.mInsets.left + this.mInsets.right));
        }
        int i3 = iMax;
        measureChildWithMargins(this.mContent, i, i3, i2, this.mInsets.top + this.mLauncher.getDeviceProfile().edgeMarginPx);
        setMeasuredDimension(View.MeasureSpec.getSize(i), View.MeasureSpec.getSize(i2));
    }

    @Override // android.widget.LinearLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int i5 = i4 - i2;
        int measuredWidth = this.mContent.getMeasuredWidth();
        int i6 = ((i3 - i) - measuredWidth) / 2;
        this.mContent.layout(i6, i5 - this.mContent.getMeasuredHeight(), measuredWidth + i6, i5);
        setTranslationShift(this.mTranslationShift);
    }

    @Override // com.android.launcher3.LauncherAppWidgetHost.ProviderChangedListener
    public void notifyWidgetProvidersChanged() {
        this.mLauncher.refreshAndBindWidgetsForPackageUser(null);
    }

    @Override // com.android.launcher3.AbstractFloatingView
    protected void onWidgetsBound() {
        this.mAdapter.setWidgets(this.mLauncher.getPopupDataProvider().getAllWidgets());
    }

    private void open(boolean z) {
        if (z) {
            if (this.mLauncher.getDragLayer().getInsets().bottom > 0) {
                this.mContent.setAlpha(0.0f);
                setTranslationShift(VERTICAL_START_POSITION);
            }
            this.mOpenCloseAnimator.setValues(PropertyValuesHolder.ofFloat(TRANSLATION_SHIFT, 0.0f));
            this.mOpenCloseAnimator.setDuration(DEFAULT_OPEN_DURATION).setInterpolator(AnimationUtils.loadInterpolator(getContext(), android.R.interpolator.linear_out_slow_in));
            this.mOpenCloseAnimator.addListener(new AnimatorListenerAdapter() { // from class: com.android.launcher3.widget.WidgetsFullSheet.1
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    WidgetsFullSheet.this.mRecyclerView.setLayoutFrozen(false);
                    WidgetsFullSheet.this.mAdapter.setApplyBitmapDeferred(false, WidgetsFullSheet.this.mRecyclerView);
                    WidgetsFullSheet.this.mOpenCloseAnimator.removeListener(this);
                }
            });
            post(new Runnable() { // from class: com.android.launcher3.widget.-$$Lambda$WidgetsFullSheet$DCKrq8gm3s0feqrjqMAAoPXsKvk
                @Override // java.lang.Runnable
                public final void run() {
                    WidgetsFullSheet.lambda$open$0(this.f$0);
                }
            });
            return;
        }
        setTranslationShift(0.0f);
        this.mAdapter.setApplyBitmapDeferred(false, this.mRecyclerView);
        post(new Runnable() { // from class: com.android.launcher3.widget.-$$Lambda$WidgetsFullSheet$Z-8XzIssRwaXuYbnBlJkmydDq-8
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.announceAccessibilityChanges();
            }
        });
    }

    public static /* synthetic */ void lambda$open$0(WidgetsFullSheet widgetsFullSheet) {
        widgetsFullSheet.mRecyclerView.setLayoutFrozen(true);
        widgetsFullSheet.mOpenCloseAnimator.start();
        widgetsFullSheet.mContent.animate().alpha(1.0f).setDuration(FADE_IN_DURATION);
    }

    @Override // com.android.launcher3.AbstractFloatingView
    protected void handleClose(boolean z) {
        handleClose(z, DEFAULT_OPEN_DURATION);
    }

    @Override // com.android.launcher3.AbstractFloatingView
    protected boolean isOfType(int i) {
        return (i & 16) != 0;
    }

    @Override // com.android.launcher3.views.AbstractSlideInView, com.android.launcher3.util.TouchController
    public boolean onControllerInterceptTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            this.mNoIntercept = false;
            RecyclerViewFastScroller scrollbar = this.mRecyclerView.getScrollbar();
            if (scrollbar.getThumbOffsetY() >= 0 && this.mLauncher.getDragLayer().isEventOverView(scrollbar, motionEvent)) {
                this.mNoIntercept = true;
            } else if (this.mLauncher.getDragLayer().isEventOverView(this.mContent, motionEvent)) {
                this.mNoIntercept = !this.mRecyclerView.shouldContainerScroll(motionEvent, this.mLauncher.getDragLayer());
            }
        }
        return super.onControllerInterceptTouchEvent(motionEvent);
    }

    public static WidgetsFullSheet show(Launcher launcher, boolean z) {
        WidgetsFullSheet widgetsFullSheet = (WidgetsFullSheet) launcher.getLayoutInflater().inflate(R.layout.widgets_full_sheet, (ViewGroup) launcher.getDragLayer(), false);
        widgetsFullSheet.mIsOpen = true;
        launcher.getDragLayer().addView(widgetsFullSheet);
        widgetsFullSheet.open(z);
        return widgetsFullSheet;
    }

    @Override // com.android.launcher3.widget.BaseWidgetSheet
    protected int getElementsRowCount() {
        return this.mAdapter.getItemCount();
    }
}
