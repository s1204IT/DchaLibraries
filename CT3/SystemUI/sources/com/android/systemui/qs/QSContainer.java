package com.android.systemui.qs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.qs.customize.QSCustomizer;
import com.android.systemui.statusbar.phone.BaseStatusBarHeader;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.phone.QSTileHost;

public class QSContainer extends FrameLayout {
    private final Animator.AnimatorListener mAnimateHeaderSlidingInListener;
    private long mDelay;
    protected BaseStatusBarHeader mHeader;
    private boolean mHeaderAnimating;
    private int mHeightOverride;
    private boolean mKeyguardShowing;
    private boolean mListening;
    private NotificationPanelView mPanelView;
    private QSAnimator mQSAnimator;
    private QSCustomizer mQSCustomizer;
    private QSDetail mQSDetail;
    protected QSPanel mQSPanel;
    private final Rect mQsBounds;
    private boolean mQsExpanded;
    protected float mQsExpansion;
    private final Point mSizePoint;
    private boolean mStackScrollerOverscrolling;
    private final ViewTreeObserver.OnPreDrawListener mStartHeaderSlidingIn;

    public QSContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mSizePoint = new Point();
        this.mQsBounds = new Rect();
        this.mHeightOverride = -1;
        this.mStartHeaderSlidingIn = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                QSContainer.this.getViewTreeObserver().removeOnPreDrawListener(this);
                QSContainer.this.animate().translationY(0.0f).setStartDelay(QSContainer.this.mDelay).setDuration(448L).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).setListener(QSContainer.this.mAnimateHeaderSlidingInListener).start();
                QSContainer.this.setY(-QSContainer.this.mHeader.getHeight());
                return true;
            }
        };
        this.mAnimateHeaderSlidingInListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                QSContainer.this.mHeaderAnimating = false;
                QSContainer.this.updateQsState();
            }
        };
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mQSPanel = (QSPanel) findViewById(R.id.quick_settings_panel);
        this.mQSDetail = (QSDetail) findViewById(R.id.qs_detail);
        this.mHeader = (BaseStatusBarHeader) findViewById(R.id.header);
        this.mQSDetail.setQsPanel(this.mQSPanel, this.mHeader);
        this.mQSAnimator = new QSAnimator(this, (QuickQSPanel) this.mHeader.findViewById(R.id.quick_qs_panel), this.mQSPanel);
        this.mQSCustomizer = (QSCustomizer) findViewById(R.id.qs_customize);
        this.mQSCustomizer.setQsContainer(this);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        this.mQSAnimator.onRtlChanged();
    }

    public void setHost(QSTileHost qsh) {
        this.mQSPanel.setHost(qsh, this.mQSCustomizer);
        this.mHeader.setQSPanel(this.mQSPanel);
        this.mQSDetail.setHost(qsh);
        this.mQSAnimator.setHost(qsh);
    }

    public void setPanelView(NotificationPanelView panelView) {
        this.mPanelView = panelView;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.mQSPanel.measure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(heightMeasureSpec), 0));
        int width = this.mQSPanel.getMeasuredWidth();
        int height = ((FrameLayout.LayoutParams) this.mQSPanel.getLayoutParams()).topMargin + this.mQSPanel.getMeasuredHeight();
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(width, 1073741824), View.MeasureSpec.makeMeasureSpec(height, 1073741824));
        getDisplay().getRealSize(this.mSizePoint);
        this.mQSCustomizer.measure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(this.mSizePoint.y, 1073741824));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateBottom();
    }

    public boolean isCustomizing() {
        return this.mQSCustomizer.isCustomizing();
    }

    public void setHeightOverride(int heightOverride) {
        this.mHeightOverride = heightOverride;
        updateBottom();
    }

    public int getDesiredHeight() {
        if (isCustomizing()) {
            return getHeight();
        }
        if (this.mQSDetail.isClosingDetail()) {
            return this.mQSPanel.getGridHeight() + this.mHeader.getCollapsedHeight() + getPaddingBottom();
        }
        return getMeasuredHeight();
    }

    public void notifyCustomizeChanged() {
        updateBottom();
        this.mQSPanel.setVisibility(!this.mQSCustomizer.isCustomizing() ? 0 : 4);
        this.mHeader.setVisibility(this.mQSCustomizer.isCustomizing() ? 4 : 0);
        this.mPanelView.onQsHeightChanged();
    }

    private void updateBottom() {
        int height = calculateContainerHeight();
        setBottom(getTop() + height);
        this.mQSDetail.setBottom(getTop() + height);
    }

    protected int calculateContainerHeight() {
        int heightOverride = this.mHeightOverride != -1 ? this.mHeightOverride : getMeasuredHeight();
        return this.mQSCustomizer.isCustomizing() ? this.mQSCustomizer.getHeight() : ((int) (this.mQsExpansion * (heightOverride - this.mHeader.getCollapsedHeight()))) + this.mHeader.getCollapsedHeight();
    }

    public void updateQsState() {
        boolean z;
        boolean z2 = (this.mQsExpanded || this.mStackScrollerOverscrolling) ? true : this.mHeaderAnimating;
        this.mQSPanel.setExpanded(this.mQsExpanded);
        this.mQSDetail.setExpanded(this.mQsExpanded);
        this.mHeader.setVisibility((this.mQsExpanded || !this.mKeyguardShowing || this.mHeaderAnimating) ? 0 : 4);
        BaseStatusBarHeader baseStatusBarHeader = this.mHeader;
        if (!this.mKeyguardShowing || this.mHeaderAnimating) {
            z = this.mQsExpanded && !this.mStackScrollerOverscrolling;
        } else {
            z = true;
        }
        baseStatusBarHeader.setExpanded(z);
        this.mQSPanel.setVisibility(z2 ? 0 : 4);
    }

    public BaseStatusBarHeader getHeader() {
        return this.mHeader;
    }

    public QSPanel getQsPanel() {
        return this.mQSPanel;
    }

    public QSCustomizer getCustomizer() {
        return this.mQSCustomizer;
    }

    public boolean isShowingDetail() {
        if (this.mQSPanel.isShowingCustomize()) {
            return true;
        }
        return this.mQSDetail.isShowingDetail();
    }

    public void setHeaderClickable(boolean clickable) {
        this.mHeader.setClickable(clickable);
    }

    public void setExpanded(boolean expanded) {
        this.mQsExpanded = expanded;
        this.mQSPanel.setListening(this.mListening ? this.mQsExpanded : false);
        updateQsState();
    }

    public void setKeyguardShowing(boolean keyguardShowing) {
        this.mKeyguardShowing = keyguardShowing;
        this.mQSAnimator.setOnKeyguard(keyguardShowing);
        updateQsState();
    }

    public void setOverscrolling(boolean stackScrollerOverscrolling) {
        this.mStackScrollerOverscrolling = stackScrollerOverscrolling;
        updateQsState();
    }

    public void setListening(boolean listening) {
        this.mListening = listening;
        this.mHeader.setListening(listening);
        this.mQSPanel.setListening(this.mListening ? this.mQsExpanded : false);
    }

    public void setQsExpansion(float expansion, float headerTranslation) {
        this.mQsExpansion = expansion;
        float translationScaleY = expansion - 1.0f;
        if (!this.mHeaderAnimating) {
            if (this.mKeyguardShowing) {
                headerTranslation = translationScaleY * this.mHeader.getHeight();
            }
            setTranslationY(headerTranslation);
        }
        this.mHeader.setExpansion(this.mKeyguardShowing ? 1.0f : expansion);
        this.mQSPanel.setTranslationY(this.mQSPanel.getHeight() * translationScaleY);
        this.mQSDetail.setFullyExpanded(expansion == 1.0f);
        this.mQSAnimator.setPosition(expansion);
        updateBottom();
        this.mQsBounds.top = (int) ((1.0f - expansion) * this.mQSPanel.getHeight());
        this.mQsBounds.right = this.mQSPanel.getWidth();
        this.mQsBounds.bottom = this.mQSPanel.getHeight();
        this.mQSPanel.setClipBounds(this.mQsBounds);
    }

    public void animateHeaderSlidingIn(long delay) {
        if (this.mQsExpanded) {
            return;
        }
        this.mHeaderAnimating = true;
        this.mDelay = delay;
        getViewTreeObserver().addOnPreDrawListener(this.mStartHeaderSlidingIn);
    }

    public void animateHeaderSlidingOut() {
        this.mHeaderAnimating = true;
        animate().y(-this.mHeader.getHeight()).setStartDelay(0L).setDuration(360L).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                QSContainer.this.animate().setListener(null);
                QSContainer.this.mHeaderAnimating = false;
                QSContainer.this.updateQsState();
            }
        }).start();
    }

    public int getQsMinExpansionHeight() {
        return this.mHeader.getHeight();
    }
}
