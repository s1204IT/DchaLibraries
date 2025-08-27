package com.android.systemui.qs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Animatable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Dependency;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.statusbar.CommandQueue;
import java.util.Objects;

/* loaded from: classes.dex */
public class QSDetail extends LinearLayout {
    private boolean mAnimatingOpen;
    private QSDetailClipper mClipper;
    private boolean mClosingDetail;
    private DetailAdapter mDetailAdapter;
    private ViewGroup mDetailContent;
    protected TextView mDetailDoneButton;
    protected TextView mDetailSettingsButton;
    private final SparseArray<View> mDetailViews;
    private View mFooter;
    private boolean mFullyExpanded;
    private QuickStatusBarHeader mHeader;
    private final AnimatorListenerAdapter mHideGridContentWhenDone;
    protected QSTileHost mHost;
    private int mOpenX;
    private int mOpenY;
    protected View mQsDetailHeader;
    protected ImageView mQsDetailHeaderProgress;
    protected Switch mQsDetailHeaderSwitch;
    protected TextView mQsDetailHeaderTitle;
    private QSPanel mQsPanel;
    protected Callback mQsPanelCallback;
    private boolean mScanState;
    private boolean mSwitchState;
    private final AnimatorListenerAdapter mTeardownDetailWhenDone;
    private boolean mTriggeredExpand;

    public interface Callback {
        void onScanStateChanged(boolean z);

        void onShowingDetail(DetailAdapter detailAdapter, int i, int i2);

        void onToggleStateChanged(boolean z);
    }

    public QSDetail(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mDetailViews = new SparseArray<>();
        this.mQsPanelCallback = new Callback() { // from class: com.android.systemui.qs.QSDetail.3
            AnonymousClass3() {
            }

            /* renamed from: com.android.systemui.qs.QSDetail$3$1 */
            class AnonymousClass1 implements Runnable {
                final /* synthetic */ boolean val$state;

                AnonymousClass1(boolean z) {
                    z = z;
                }

                @Override // java.lang.Runnable
                public void run() {
                    QSDetail.this.handleToggleStateChanged(z, QSDetail.this.mDetailAdapter != null && QSDetail.this.mDetailAdapter.getToggleEnabled());
                }
            }

            @Override // com.android.systemui.qs.QSDetail.Callback
            public void onToggleStateChanged(boolean z) {
                QSDetail.this.post(new Runnable() { // from class: com.android.systemui.qs.QSDetail.3.1
                    final /* synthetic */ boolean val$state;

                    AnonymousClass1(boolean z2) {
                        z = z2;
                    }

                    @Override // java.lang.Runnable
                    public void run() {
                        QSDetail.this.handleToggleStateChanged(z, QSDetail.this.mDetailAdapter != null && QSDetail.this.mDetailAdapter.getToggleEnabled());
                    }
                });
            }

            /* renamed from: com.android.systemui.qs.QSDetail$3$2 */
            class AnonymousClass2 implements Runnable {
                final /* synthetic */ DetailAdapter val$detail;
                final /* synthetic */ int val$x;
                final /* synthetic */ int val$y;

                AnonymousClass2(DetailAdapter detailAdapter, int i, int i2) {
                    detailAdapter = detailAdapter;
                    i = i;
                    i = i2;
                }

                @Override // java.lang.Runnable
                public void run() {
                    QSDetail.this.handleShowingDetail(detailAdapter, i, i, false);
                }
            }

            @Override // com.android.systemui.qs.QSDetail.Callback
            public void onShowingDetail(DetailAdapter detailAdapter, int i, int i2) {
                QSDetail.this.post(new Runnable() { // from class: com.android.systemui.qs.QSDetail.3.2
                    final /* synthetic */ DetailAdapter val$detail;
                    final /* synthetic */ int val$x;
                    final /* synthetic */ int val$y;

                    AnonymousClass2(DetailAdapter detailAdapter2, int i3, int i22) {
                        detailAdapter = detailAdapter2;
                        i = i3;
                        i = i22;
                    }

                    @Override // java.lang.Runnable
                    public void run() {
                        QSDetail.this.handleShowingDetail(detailAdapter, i, i, false);
                    }
                });
            }

            /* renamed from: com.android.systemui.qs.QSDetail$3$3 */
            class RunnableC00033 implements Runnable {
                final /* synthetic */ boolean val$state;

                RunnableC00033(boolean z) {
                    z = z;
                }

                @Override // java.lang.Runnable
                public void run() {
                    QSDetail.this.handleScanStateChanged(z);
                }
            }

            @Override // com.android.systemui.qs.QSDetail.Callback
            public void onScanStateChanged(boolean z) {
                QSDetail.this.post(new Runnable() { // from class: com.android.systemui.qs.QSDetail.3.3
                    final /* synthetic */ boolean val$state;

                    RunnableC00033(boolean z2) {
                        z = z2;
                    }

                    @Override // java.lang.Runnable
                    public void run() {
                        QSDetail.this.handleScanStateChanged(z);
                    }
                });
            }
        };
        this.mHideGridContentWhenDone = new AnimatorListenerAdapter() { // from class: com.android.systemui.qs.QSDetail.4
            AnonymousClass4() {
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
                animator.removeListener(this);
                QSDetail.this.mAnimatingOpen = false;
                QSDetail.this.checkPendingAnimations();
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                if (QSDetail.this.mDetailAdapter != null) {
                    QSDetail.this.mQsPanel.setGridContentVisibility(false);
                    QSDetail.this.mHeader.setVisibility(4);
                    QSDetail.this.mFooter.setVisibility(4);
                }
                QSDetail.this.mAnimatingOpen = false;
                QSDetail.this.checkPendingAnimations();
            }
        };
        this.mTeardownDetailWhenDone = new AnimatorListenerAdapter() { // from class: com.android.systemui.qs.QSDetail.5
            AnonymousClass5() {
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                QSDetail.this.mDetailContent.removeAllViews();
                QSDetail.this.setVisibility(4);
                QSDetail.this.mClosingDetail = false;
            }
        };
    }

    @Override // android.view.View
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        FontSizeUtils.updateFontSize(this.mDetailDoneButton, R.dimen.qs_detail_button_text_size);
        FontSizeUtils.updateFontSize(this.mDetailSettingsButton, R.dimen.qs_detail_button_text_size);
        for (int i = 0; i < this.mDetailViews.size(); i++) {
            this.mDetailViews.valueAt(i).dispatchConfigurationChanged(configuration);
        }
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mDetailContent = (ViewGroup) findViewById(android.R.id.content);
        this.mDetailSettingsButton = (TextView) findViewById(android.R.id.button2);
        this.mDetailDoneButton = (TextView) findViewById(android.R.id.button1);
        this.mQsDetailHeader = findViewById(R.id.qs_detail_header);
        this.mQsDetailHeaderTitle = (TextView) this.mQsDetailHeader.findViewById(android.R.id.title);
        this.mQsDetailHeaderSwitch = (Switch) this.mQsDetailHeader.findViewById(android.R.id.toggle);
        this.mQsDetailHeaderProgress = (ImageView) findViewById(R.id.qs_detail_header_progress);
        updateDetailText();
        this.mClipper = new QSDetailClipper(this);
        this.mDetailDoneButton.setOnClickListener(new View.OnClickListener() { // from class: com.android.systemui.qs.QSDetail.1
            AnonymousClass1() {
            }

            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                QSDetail.this.announceForAccessibility(QSDetail.this.mContext.getString(R.string.accessibility_desc_quick_settings));
                QSDetail.this.mQsPanel.closeDetail();
            }
        });
    }

    /* renamed from: com.android.systemui.qs.QSDetail$1 */
    class AnonymousClass1 implements View.OnClickListener {
        AnonymousClass1() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            QSDetail.this.announceForAccessibility(QSDetail.this.mContext.getString(R.string.accessibility_desc_quick_settings));
            QSDetail.this.mQsPanel.closeDetail();
        }
    }

    public void setQsPanel(QSPanel qSPanel, QuickStatusBarHeader quickStatusBarHeader, View view) {
        this.mQsPanel = qSPanel;
        this.mHeader = quickStatusBarHeader;
        this.mFooter = view;
        this.mHeader.setCallback(this.mQsPanelCallback);
        this.mQsPanel.setCallback(this.mQsPanelCallback);
    }

    public void setHost(QSTileHost qSTileHost) {
        this.mHost = qSTileHost;
    }

    public boolean isShowingDetail() {
        return this.mDetailAdapter != null;
    }

    public void setFullyExpanded(boolean z) {
        this.mFullyExpanded = z;
    }

    public void setExpanded(boolean z) {
        if (!z) {
            this.mTriggeredExpand = false;
        }
    }

    private void updateDetailText() {
        this.mDetailDoneButton.setText(R.string.quick_settings_done);
        this.mDetailSettingsButton.setText(R.string.quick_settings_more_settings);
    }

    public boolean isClosingDetail() {
        return this.mClosingDetail;
    }

    public void handleShowingDetail(DetailAdapter detailAdapter, int i, int i2, boolean z) {
        AnimatorListenerAdapter animatorListenerAdapter;
        boolean z2 = detailAdapter != null;
        setClickable(z2);
        if (z2) {
            setupDetailHeader(detailAdapter);
            if (z && !this.mFullyExpanded) {
                this.mTriggeredExpand = true;
                ((CommandQueue) SysUiServiceProvider.getComponent(this.mContext, CommandQueue.class)).animateExpandSettingsPanel(null);
            } else {
                this.mTriggeredExpand = false;
            }
            this.mOpenX = i;
            this.mOpenY = i2;
        } else {
            i = this.mOpenX;
            i2 = this.mOpenY;
            if (z && this.mTriggeredExpand) {
                ((CommandQueue) SysUiServiceProvider.getComponent(this.mContext, CommandQueue.class)).animateCollapsePanels();
                this.mTriggeredExpand = false;
            }
        }
        boolean z3 = (this.mDetailAdapter != null) != (detailAdapter != null);
        if (z3 || this.mDetailAdapter != detailAdapter) {
            if (detailAdapter != null) {
                int metricsCategory = detailAdapter.getMetricsCategory();
                View viewCreateDetailView = detailAdapter.createDetailView(this.mContext, this.mDetailViews.get(metricsCategory), this.mDetailContent);
                if (viewCreateDetailView == null) {
                    throw new IllegalStateException("Must return detail view");
                }
                setupDetailFooter(detailAdapter);
                this.mDetailContent.removeAllViews();
                this.mDetailContent.addView(viewCreateDetailView);
                this.mDetailViews.put(metricsCategory, viewCreateDetailView);
                ((MetricsLogger) Dependency.get(MetricsLogger.class)).visible(detailAdapter.getMetricsCategory());
                announceForAccessibility(this.mContext.getString(R.string.accessibility_quick_settings_detail, detailAdapter.getTitle()));
                this.mDetailAdapter = detailAdapter;
                animatorListenerAdapter = this.mHideGridContentWhenDone;
                setVisibility(0);
            } else {
                if (this.mDetailAdapter != null) {
                    ((MetricsLogger) Dependency.get(MetricsLogger.class)).hidden(this.mDetailAdapter.getMetricsCategory());
                }
                this.mClosingDetail = true;
                this.mDetailAdapter = null;
                animatorListenerAdapter = this.mTeardownDetailWhenDone;
                this.mHeader.setVisibility(0);
                this.mFooter.setVisibility(0);
                this.mQsPanel.setGridContentVisibility(true);
                this.mQsPanelCallback.onScanStateChanged(false);
            }
            sendAccessibilityEvent(32);
            animateDetailVisibleDiff(i, i2, z3, animatorListenerAdapter);
        }
    }

    protected void animateDetailVisibleDiff(int i, int i2, boolean z, Animator.AnimatorListener animatorListener) {
        if (z) {
            this.mAnimatingOpen = this.mDetailAdapter != null;
            if (this.mFullyExpanded || this.mDetailAdapter != null) {
                setAlpha(1.0f);
                this.mClipper.animateCircularClip(i, i2, this.mDetailAdapter != null, animatorListener);
            } else {
                animate().alpha(0.0f).setDuration(300L).setListener(animatorListener).start();
            }
        }
    }

    protected void setupDetailFooter(final DetailAdapter detailAdapter) {
        final Intent settingsIntent = detailAdapter.getSettingsIntent();
        this.mDetailSettingsButton.setVisibility(settingsIntent != null ? 0 : 8);
        this.mDetailSettingsButton.setOnClickListener(new View.OnClickListener() { // from class: com.android.systemui.qs.-$$Lambda$QSDetail$NHQwfesA2Z6J0e0FBlLg3IIEATQ
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                QSDetail.lambda$setupDetailFooter$0(detailAdapter, settingsIntent, view);
            }
        });
    }

    static /* synthetic */ void lambda$setupDetailFooter$0(DetailAdapter detailAdapter, Intent intent, View view) {
        ((MetricsLogger) Dependency.get(MetricsLogger.class)).action(929, detailAdapter.getMetricsCategory());
        ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(intent, 0);
    }

    protected void setupDetailHeader(DetailAdapter detailAdapter) {
        this.mQsDetailHeaderTitle.setText(detailAdapter.getTitle());
        Boolean toggleState = detailAdapter.getToggleState();
        if (toggleState != null) {
            this.mQsDetailHeaderSwitch.setVisibility(0);
            handleToggleStateChanged(toggleState.booleanValue(), detailAdapter.getToggleEnabled());
            this.mQsDetailHeader.setClickable(true);
            this.mQsDetailHeader.setOnClickListener(new View.OnClickListener() { // from class: com.android.systemui.qs.QSDetail.2
                final /* synthetic */ DetailAdapter val$adapter;

                AnonymousClass2(DetailAdapter detailAdapter2) {
                    detailAdapter = detailAdapter2;
                }

                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    boolean z = !QSDetail.this.mQsDetailHeaderSwitch.isChecked();
                    QSDetail.this.mQsDetailHeaderSwitch.setChecked(z);
                    detailAdapter.setToggleState(z);
                }
            });
            return;
        }
        this.mQsDetailHeaderSwitch.setVisibility(4);
        this.mQsDetailHeader.setClickable(false);
    }

    /* renamed from: com.android.systemui.qs.QSDetail$2 */
    class AnonymousClass2 implements View.OnClickListener {
        final /* synthetic */ DetailAdapter val$adapter;

        AnonymousClass2(DetailAdapter detailAdapter2) {
            detailAdapter = detailAdapter2;
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            boolean z = !QSDetail.this.mQsDetailHeaderSwitch.isChecked();
            QSDetail.this.mQsDetailHeaderSwitch.setChecked(z);
            detailAdapter.setToggleState(z);
        }
    }

    private void handleToggleStateChanged(boolean z, boolean z2) {
        this.mSwitchState = z;
        if (this.mAnimatingOpen) {
            return;
        }
        this.mQsDetailHeaderSwitch.setChecked(z);
        this.mQsDetailHeader.setEnabled(z2);
        this.mQsDetailHeaderSwitch.setEnabled(z2);
    }

    private void handleScanStateChanged(boolean z) {
        if (this.mScanState == z) {
            return;
        }
        this.mScanState = z;
        final Animatable animatable = (Animatable) this.mQsDetailHeaderProgress.getDrawable();
        if (z) {
            this.mQsDetailHeaderProgress.animate().cancel();
            ViewPropertyAnimator viewPropertyAnimatorAlpha = this.mQsDetailHeaderProgress.animate().alpha(1.0f);
            Objects.requireNonNull(animatable);
            viewPropertyAnimatorAlpha.withEndAction(new Runnable() { // from class: com.android.systemui.qs.-$$Lambda$dWuG3P2xqsast1TFpf_9V5OJbdM
                @Override // java.lang.Runnable
                public final void run() {
                    animatable.start();
                }
            }).start();
            return;
        }
        this.mQsDetailHeaderProgress.animate().cancel();
        ViewPropertyAnimator viewPropertyAnimatorAlpha2 = this.mQsDetailHeaderProgress.animate().alpha(0.0f);
        Objects.requireNonNull(animatable);
        viewPropertyAnimatorAlpha2.withEndAction(new Runnable() { // from class: com.android.systemui.qs.-$$Lambda$uWzoJtW0gRQtylxIzOBLYDei0eA
            @Override // java.lang.Runnable
            public final void run() {
                animatable.stop();
            }
        }).start();
    }

    private void checkPendingAnimations() {
        handleToggleStateChanged(this.mSwitchState, this.mDetailAdapter != null && this.mDetailAdapter.getToggleEnabled());
    }

    /* renamed from: com.android.systemui.qs.QSDetail$3 */
    class AnonymousClass3 implements Callback {
        AnonymousClass3() {
        }

        /* renamed from: com.android.systemui.qs.QSDetail$3$1 */
        class AnonymousClass1 implements Runnable {
            final /* synthetic */ boolean val$state;

            AnonymousClass1(boolean z2) {
                z = z2;
            }

            @Override // java.lang.Runnable
            public void run() {
                QSDetail.this.handleToggleStateChanged(z, QSDetail.this.mDetailAdapter != null && QSDetail.this.mDetailAdapter.getToggleEnabled());
            }
        }

        @Override // com.android.systemui.qs.QSDetail.Callback
        public void onToggleStateChanged(boolean z2) {
            QSDetail.this.post(new Runnable() { // from class: com.android.systemui.qs.QSDetail.3.1
                final /* synthetic */ boolean val$state;

                AnonymousClass1(boolean z22) {
                    z = z22;
                }

                @Override // java.lang.Runnable
                public void run() {
                    QSDetail.this.handleToggleStateChanged(z, QSDetail.this.mDetailAdapter != null && QSDetail.this.mDetailAdapter.getToggleEnabled());
                }
            });
        }

        /* renamed from: com.android.systemui.qs.QSDetail$3$2 */
        class AnonymousClass2 implements Runnable {
            final /* synthetic */ DetailAdapter val$detail;
            final /* synthetic */ int val$x;
            final /* synthetic */ int val$y;

            AnonymousClass2(DetailAdapter detailAdapter2, int i3, int i22) {
                detailAdapter = detailAdapter2;
                i = i3;
                i = i22;
            }

            @Override // java.lang.Runnable
            public void run() {
                QSDetail.this.handleShowingDetail(detailAdapter, i, i, false);
            }
        }

        @Override // com.android.systemui.qs.QSDetail.Callback
        public void onShowingDetail(DetailAdapter detailAdapter2, int i3, int i22) {
            QSDetail.this.post(new Runnable() { // from class: com.android.systemui.qs.QSDetail.3.2
                final /* synthetic */ DetailAdapter val$detail;
                final /* synthetic */ int val$x;
                final /* synthetic */ int val$y;

                AnonymousClass2(DetailAdapter detailAdapter22, int i32, int i222) {
                    detailAdapter = detailAdapter22;
                    i = i32;
                    i = i222;
                }

                @Override // java.lang.Runnable
                public void run() {
                    QSDetail.this.handleShowingDetail(detailAdapter, i, i, false);
                }
            });
        }

        /* renamed from: com.android.systemui.qs.QSDetail$3$3 */
        class RunnableC00033 implements Runnable {
            final /* synthetic */ boolean val$state;

            RunnableC00033(boolean z2) {
                z = z2;
            }

            @Override // java.lang.Runnable
            public void run() {
                QSDetail.this.handleScanStateChanged(z);
            }
        }

        @Override // com.android.systemui.qs.QSDetail.Callback
        public void onScanStateChanged(boolean z2) {
            QSDetail.this.post(new Runnable() { // from class: com.android.systemui.qs.QSDetail.3.3
                final /* synthetic */ boolean val$state;

                RunnableC00033(boolean z22) {
                    z = z22;
                }

                @Override // java.lang.Runnable
                public void run() {
                    QSDetail.this.handleScanStateChanged(z);
                }
            });
        }
    }

    /* renamed from: com.android.systemui.qs.QSDetail$4 */
    class AnonymousClass4 extends AnimatorListenerAdapter {
        AnonymousClass4() {
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animator) {
            animator.removeListener(this);
            QSDetail.this.mAnimatingOpen = false;
            QSDetail.this.checkPendingAnimations();
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            if (QSDetail.this.mDetailAdapter != null) {
                QSDetail.this.mQsPanel.setGridContentVisibility(false);
                QSDetail.this.mHeader.setVisibility(4);
                QSDetail.this.mFooter.setVisibility(4);
            }
            QSDetail.this.mAnimatingOpen = false;
            QSDetail.this.checkPendingAnimations();
        }
    }

    /* renamed from: com.android.systemui.qs.QSDetail$5 */
    class AnonymousClass5 extends AnimatorListenerAdapter {
        AnonymousClass5() {
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            QSDetail.this.mDetailContent.removeAllViews();
            QSDetail.this.setVisibility(4);
            QSDetail.this.mClosingDetail = false;
        }
    }
}
