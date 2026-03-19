package com.android.systemui.qs;

import android.util.Log;
import android.view.View;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.qs.QS;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.qs.QSTileView;
import com.android.systemui.qs.PagedTileLayout;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.TouchAnimator;
import com.android.systemui.tuner.TunerService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class QSAnimator implements View.OnAttachStateChangeListener, View.OnLayoutChangeListener, PagedTileLayout.PageListener, QSHost.Callback, TouchAnimator.Listener, TunerService.Tunable {
    private boolean mAllowFancy;
    private TouchAnimator mBrightnessAnimator;
    private TouchAnimator mFirstPageAnimator;
    private TouchAnimator mFirstPageDelayedAnimator;
    private boolean mFullRows;
    private QSTileHost mHost;
    private float mLastPosition;
    private TouchAnimator mNonfirstPageAnimator;
    private TouchAnimator mNonfirstPageDelayedAnimator;
    private int mNumQuickTiles;
    private boolean mOnKeyguard;
    private PagedTileLayout mPagedLayout;
    private final QS mQs;
    private final QSPanel mQsPanel;
    private final QuickQSPanel mQuickQsPanel;
    private TouchAnimator mTranslationXAnimator;
    private TouchAnimator mTranslationYAnimator;
    private final ArrayList<View> mAllViews = new ArrayList<>();
    private final ArrayList<View> mQuickQsViews = new ArrayList<>();
    private boolean mOnFirstPage = true;
    private final TouchAnimator.Listener mNonFirstPageListener = new TouchAnimator.ListenerAdapter() {
        @Override
        public void onAnimationAtEnd() {
            QSAnimator.this.mQuickQsPanel.setVisibility(4);
        }

        @Override
        public void onAnimationStarted() {
            QSAnimator.this.mQuickQsPanel.setVisibility(0);
        }
    };
    private Runnable mUpdateAnimators = new Runnable() {
        @Override
        public void run() {
            QSAnimator.this.updateAnimators();
            QSAnimator.this.setPosition(QSAnimator.this.mLastPosition);
        }
    };

    public QSAnimator(QS qs, QuickQSPanel quickQSPanel, QSPanel qSPanel) {
        this.mQs = qs;
        this.mQuickQsPanel = quickQSPanel;
        this.mQsPanel = qSPanel;
        this.mQsPanel.addOnAttachStateChangeListener(this);
        qs.getView().addOnLayoutChangeListener(this);
        if (this.mQsPanel.isAttachedToWindow()) {
            onViewAttachedToWindow(null);
        }
        QSPanel.QSTileLayout tileLayout = this.mQsPanel.getTileLayout();
        if (tileLayout instanceof PagedTileLayout) {
            this.mPagedLayout = (PagedTileLayout) tileLayout;
        } else {
            Log.w("QSAnimator", "QS Not using page layout");
        }
        qSPanel.setPageListener(this);
    }

    public void onRtlChanged() {
        updateAnimators();
    }

    public void setOnKeyguard(boolean z) {
        this.mOnKeyguard = z;
        this.mQuickQsPanel.setVisibility(this.mOnKeyguard ? 4 : 0);
        if (this.mOnKeyguard) {
            clearAnimationState();
        }
    }

    public void setHost(QSTileHost qSTileHost) {
        this.mHost = qSTileHost;
        qSTileHost.addCallback(this);
        updateAnimators();
    }

    @Override
    public void onViewAttachedToWindow(View view) {
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "sysui_qs_fancy_anim", "sysui_qs_move_whole_rows", "sysui_qqs_count");
    }

    @Override
    public void onViewDetachedFromWindow(View view) {
        if (this.mHost != null) {
            this.mHost.removeCallback(this);
        }
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
    }

    @Override
    public void onTuningChanged(String str, String str2) {
        boolean z = true;
        if ("sysui_qs_fancy_anim".equals(str)) {
            if (str2 != null && Integer.parseInt(str2) == 0) {
                z = false;
            }
            this.mAllowFancy = z;
            if (!this.mAllowFancy) {
                clearAnimationState();
            }
        } else if ("sysui_qs_move_whole_rows".equals(str)) {
            if (str2 != null && Integer.parseInt(str2) == 0) {
                z = false;
            }
            this.mFullRows = z;
        } else if ("sysui_qqs_count".equals(str)) {
            QuickQSPanel quickQSPanel = this.mQuickQsPanel;
            this.mNumQuickTiles = QuickQSPanel.getNumQuickTiles(this.mQs.getContext());
            clearAnimationState();
        }
        updateAnimators();
    }

    @Override
    public void onPageChanged(boolean z) {
        if (this.mOnFirstPage == z) {
            return;
        }
        if (!z) {
            clearAnimationState();
        }
        this.mOnFirstPage = z;
    }

    private void updateAnimators() {
        Object obj;
        Iterator<QSTile> it;
        Collection<QSTile> collection;
        Object obj2;
        int i;
        int[] iArr;
        TouchAnimator.Builder builder = new TouchAnimator.Builder();
        TouchAnimator.Builder builder2 = new TouchAnimator.Builder();
        TouchAnimator.Builder builder3 = new TouchAnimator.Builder();
        if (this.mQsPanel.getHost() == null) {
            return;
        }
        Collection<QSTile> tiles = this.mQsPanel.getHost().getTiles();
        int[] iArr2 = new int[2];
        int[] iArr3 = new int[2];
        clearAnimationState();
        this.mAllViews.clear();
        this.mQuickQsViews.clear();
        Object tileLayout = this.mQsPanel.getTileLayout();
        this.mAllViews.add((View) tileLayout);
        int measuredHeight = ((this.mQs.getView() != null ? this.mQs.getView().getMeasuredHeight() : 0) - this.mQs.getHeader().getBottom()) + this.mQs.getHeader().getPaddingBottom();
        float f = measuredHeight;
        builder.addFloat(tileLayout, "translationY", f, 0.0f);
        Iterator<QSTile> it2 = tiles.iterator();
        int i2 = 0;
        int i3 = 0;
        while (it2.hasNext()) {
            QSTile next = it2.next();
            QSTileView tileView = this.mQsPanel.getTileView(next);
            if (tileView == null) {
                Log.e("QSAnimator", "tileView is null " + next.getTileSpec());
                it = it2;
            } else {
                View iconView = tileView.getIcon().getIconView();
                View view = this.mQs.getView();
                it = it2;
                if (i2 >= this.mNumQuickTiles || !this.mAllowFancy) {
                    collection = tiles;
                    obj2 = tileLayout;
                    i = measuredHeight;
                    if (this.mFullRows && isIconInAnimatedRow(i2)) {
                        iArr2[0] = iArr2[0] + i3;
                        getRelativePosition(iArr3, iconView, view);
                        int i4 = iArr3[0] - iArr2[0];
                        int i5 = iArr3[1] - iArr2[1];
                        iArr = iArr2;
                        builder.addFloat(tileView, "translationY", f, 0.0f);
                        builder2.addFloat(tileView, "translationX", -i4, 0.0f);
                        float f2 = -i5;
                        builder3.addFloat(tileView, "translationY", f2, 0.0f);
                        builder3.addFloat(iconView, "translationY", f2, 0.0f);
                        this.mAllViews.add(iconView);
                    } else {
                        iArr = iArr2;
                        builder.addFloat(tileView, "alpha", 0.0f, 1.0f);
                        measuredHeight = i;
                        builder.addFloat(tileView, "translationY", -measuredHeight, 0.0f);
                        this.mAllViews.add(tileView);
                        i2++;
                        it2 = it;
                        tiles = collection;
                        tileLayout = obj2;
                        iArr2 = iArr;
                    }
                } else {
                    QSTileView tileView2 = this.mQuickQsPanel.getTileView(next);
                    if (tileView2 != null) {
                        int i6 = iArr2[0];
                        getRelativePosition(iArr2, tileView2.getIcon().getIconView(), view);
                        getRelativePosition(iArr3, iconView, view);
                        int i7 = iArr3[0] - iArr2[0];
                        int i8 = iArr3[1] - iArr2[1];
                        int i9 = iArr2[0] - i6;
                        collection = tiles;
                        obj2 = tileLayout;
                        i = measuredHeight;
                        builder2.addFloat(tileView2, "translationX", 0.0f, i7);
                        builder3.addFloat(tileView2, "translationY", 0.0f, i8);
                        builder2.addFloat(tileView, "translationX", -i7, 0.0f);
                        builder3.addFloat(tileView, "translationY", -i8, 0.0f);
                        this.mQuickQsViews.add(tileView.getIconWithBackground());
                        this.mAllViews.add(tileView.getIcon());
                        this.mAllViews.add(tileView2);
                        iArr = iArr2;
                        i3 = i9;
                    }
                }
                measuredHeight = i;
                this.mAllViews.add(tileView);
                i2++;
                it2 = it;
                tiles = collection;
                tileLayout = obj2;
                iArr2 = iArr;
            }
            it2 = it;
        }
        Collection<QSTile> collection2 = tiles;
        Object obj3 = tileLayout;
        if (this.mAllowFancy) {
            View brightnessView = this.mQsPanel.getBrightnessView();
            if (brightnessView != null) {
                builder.addFloat(brightnessView, "translationY", f, 0.0f);
                this.mBrightnessAnimator = new TouchAnimator.Builder().addFloat(brightnessView, "alpha", 0.0f, 1.0f).setStartDelay(0.5f).build();
                this.mAllViews.add(brightnessView);
            } else {
                this.mBrightnessAnimator = null;
            }
            this.mFirstPageAnimator = builder.setListener(this).build();
            obj = obj3;
            this.mFirstPageDelayedAnimator = new TouchAnimator.Builder().setStartDelay(0.86f).addFloat(this.mQsPanel.getPageIndicator(), "alpha", 0.0f, 1.0f).addFloat(obj, "alpha", 0.0f, 1.0f).addFloat(this.mQsPanel.getDivider(), "alpha", 0.0f, 1.0f).addFloat(this.mQsPanel.getFooter().getView(), "alpha", 0.0f, 1.0f).build();
            this.mAllViews.add(this.mQsPanel.getPageIndicator());
            this.mAllViews.add(this.mQsPanel.getDivider());
            this.mAllViews.add(this.mQsPanel.getFooter().getView());
            PathInterpolatorBuilder pathInterpolatorBuilder = new PathInterpolatorBuilder(0.0f, 0.0f, collection2.size() <= 3 ? 1.0f : collection2.size() <= 6 ? 0.4f : 0.0f, 1.0f);
            builder2.setInterpolator(pathInterpolatorBuilder.getXInterpolator());
            builder3.setInterpolator(pathInterpolatorBuilder.getYInterpolator());
            this.mTranslationXAnimator = builder2.build();
            this.mTranslationYAnimator = builder3.build();
        } else {
            obj = obj3;
        }
        this.mNonfirstPageAnimator = new TouchAnimator.Builder().addFloat(this.mQuickQsPanel, "alpha", 1.0f, 0.0f).addFloat(this.mQsPanel.getPageIndicator(), "alpha", 0.0f, 1.0f).addFloat(this.mQsPanel.getDivider(), "alpha", 0.0f, 1.0f).setListener(this.mNonFirstPageListener).setEndDelay(0.5f).build();
        this.mNonfirstPageDelayedAnimator = new TouchAnimator.Builder().setStartDelay(0.14f).addFloat(obj, "alpha", 0.0f, 1.0f).build();
    }

    private boolean isIconInAnimatedRow(int i) {
        if (this.mPagedLayout == null) {
            return false;
        }
        int columnCount = this.mPagedLayout.getColumnCount();
        return i < (((this.mNumQuickTiles + columnCount) - 1) / columnCount) * columnCount;
    }

    private void getRelativePosition(int[] iArr, View view, View view2) {
        iArr[0] = (view.getWidth() / 2) + 0;
        iArr[1] = 0;
        getRelativePositionInt(iArr, view, view2);
    }

    private void getRelativePositionInt(int[] iArr, View view, View view2) {
        if (view == view2 || view == null) {
            return;
        }
        if (!(view instanceof PagedTileLayout.TilePage)) {
            iArr[0] = iArr[0] + view.getLeft();
            iArr[1] = iArr[1] + view.getTop();
        }
        getRelativePositionInt(iArr, (View) view.getParent(), view2);
    }

    public void setPosition(float f) {
        if (this.mFirstPageAnimator == null || this.mOnKeyguard) {
            return;
        }
        this.mLastPosition = f;
        if (this.mOnFirstPage && this.mAllowFancy) {
            this.mQuickQsPanel.setAlpha(1.0f);
            this.mFirstPageAnimator.setPosition(f);
            this.mFirstPageDelayedAnimator.setPosition(f);
            this.mTranslationXAnimator.setPosition(f);
            this.mTranslationYAnimator.setPosition(f);
            if (this.mBrightnessAnimator != null) {
                this.mBrightnessAnimator.setPosition(f);
                return;
            }
            return;
        }
        this.mNonfirstPageAnimator.setPosition(f);
        this.mNonfirstPageDelayedAnimator.setPosition(f);
    }

    @Override
    public void onAnimationAtStart() {
        this.mQuickQsPanel.setVisibility(0);
    }

    @Override
    public void onAnimationAtEnd() {
        this.mQuickQsPanel.setVisibility(4);
        int size = this.mQuickQsViews.size();
        for (int i = 0; i < size; i++) {
            this.mQuickQsViews.get(i).setVisibility(0);
        }
    }

    @Override
    public void onAnimationStarted() {
        this.mQuickQsPanel.setVisibility(this.mOnKeyguard ? 4 : 0);
        if (this.mOnFirstPage) {
            int size = this.mQuickQsViews.size();
            for (int i = 0; i < size; i++) {
                this.mQuickQsViews.get(i).setVisibility(4);
            }
        }
    }

    private void clearAnimationState() {
        int size = this.mAllViews.size();
        this.mQuickQsPanel.setAlpha(0.0f);
        for (int i = 0; i < size; i++) {
            View view = this.mAllViews.get(i);
            view.setAlpha(1.0f);
            view.setTranslationX(0.0f);
            view.setTranslationY(0.0f);
        }
        int size2 = this.mQuickQsViews.size();
        for (int i2 = 0; i2 < size2; i2++) {
            this.mQuickQsViews.get(i2).setVisibility(0);
        }
    }

    @Override
    public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        this.mQsPanel.post(this.mUpdateAnimators);
    }

    @Override
    public void onTilesChanged() {
        this.mQsPanel.post(this.mUpdateAnimators);
    }
}
