package com.android.systemui.qs;

import android.graphics.Path;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.systemui.qs.PagedTileLayout;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.TouchAnimator;
import com.android.systemui.statusbar.phone.QSTileHost;
import com.android.systemui.tuner.TunerService;
import java.util.ArrayList;
import java.util.Collection;

public class QSAnimator implements QSTile.Host.Callback, PagedTileLayout.PageListener, TouchAnimator.Listener, View.OnLayoutChangeListener, View.OnAttachStateChangeListener, TunerService.Tunable {
    private boolean mAllowFancy;
    private TouchAnimator mFirstPageAnimator;
    private TouchAnimator mFirstPageDelayedAnimator;
    private boolean mFullRows;
    private QSTileHost mHost;
    private float mLastPosition;
    private TouchAnimator mLastRowAnimator;
    private TouchAnimator mNonfirstPageAnimator;
    private int mNumQuickTiles;
    private boolean mOnKeyguard;
    private PagedTileLayout mPagedLayout;
    private final QSContainer mQsContainer;
    private final QSPanel mQsPanel;
    private final QuickQSPanel mQuickQsPanel;
    private TouchAnimator mTranslationXAnimator;
    private TouchAnimator mTranslationYAnimator;
    private final ArrayList<View> mAllViews = new ArrayList<>();
    private final ArrayList<View> mTopFiveQs = new ArrayList<>();
    private boolean mOnFirstPage = true;
    private final TouchAnimator.Listener mNonFirstPageListener = new TouchAnimator.ListenerAdapter() {
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

    public QSAnimator(QSContainer container, QuickQSPanel quickPanel, QSPanel panel) {
        this.mQsContainer = container;
        this.mQuickQsPanel = quickPanel;
        this.mQsPanel = panel;
        this.mQsPanel.addOnAttachStateChangeListener(this);
        container.addOnLayoutChangeListener(this);
        QSPanel.QSTileLayout tileLayout = this.mQsPanel.getTileLayout();
        if (tileLayout instanceof PagedTileLayout) {
            this.mPagedLayout = (PagedTileLayout) tileLayout;
            this.mPagedLayout.setPageListener(this);
        } else {
            Log.w("QSAnimator", "QS Not using page layout");
        }
    }

    public void onRtlChanged() {
        updateAnimators();
    }

    public void setOnKeyguard(boolean onKeyguard) {
        this.mOnKeyguard = onKeyguard;
        this.mQuickQsPanel.setVisibility(this.mOnKeyguard ? 4 : 0);
        if (!this.mOnKeyguard) {
            return;
        }
        clearAnimationState();
    }

    public void setHost(QSTileHost qsh) {
        this.mHost = qsh;
        qsh.addCallback(this);
        updateAnimators();
    }

    @Override
    public void onViewAttachedToWindow(View v) {
        TunerService.get(this.mQsContainer.getContext()).addTunable(this, "sysui_qs_fancy_anim", "sysui_qs_move_whole_rows", "sysui_qqs_count");
    }

    @Override
    public void onViewDetachedFromWindow(View v) {
        if (this.mHost != null) {
            this.mHost.removeCallback(this);
        }
        TunerService.get(this.mQsContainer.getContext()).removeTunable(this);
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        boolean z = true;
        if ("sysui_qs_fancy_anim".equals(key)) {
            if (newValue != null && Integer.parseInt(newValue) == 0) {
                z = false;
            }
            this.mAllowFancy = z;
            if (!this.mAllowFancy) {
                clearAnimationState();
            }
        } else if ("sysui_qs_move_whole_rows".equals(key)) {
            if (newValue != null && Integer.parseInt(newValue) == 0) {
                z = false;
            }
            this.mFullRows = z;
        } else if ("sysui_qqs_count".equals(key)) {
            this.mNumQuickTiles = this.mQuickQsPanel.getNumQuickTiles(this.mQsContainer.getContext());
            clearAnimationState();
        }
        updateAnimators();
    }

    @Override
    public void onPageChanged(boolean isFirst) {
        if (this.mOnFirstPage == isFirst) {
            return;
        }
        if (!isFirst) {
            clearAnimationState();
        }
        this.mOnFirstPage = isFirst;
    }

    public void updateAnimators() {
        TouchAnimator.Builder firstPageBuilder = new TouchAnimator.Builder();
        TouchAnimator.Builder translationXBuilder = new TouchAnimator.Builder();
        TouchAnimator.Builder translationYBuilder = new TouchAnimator.Builder();
        TouchAnimator.Builder lastRowBuilder = new TouchAnimator.Builder();
        if (this.mQsPanel.getHost() == null) {
            return;
        }
        Collection<QSTile<?>> tiles = this.mQsPanel.getHost().getTiles();
        int count = 0;
        int[] loc1 = new int[2];
        int[] loc2 = new int[2];
        int lastXDiff = 0;
        clearAnimationState();
        this.mAllViews.clear();
        this.mTopFiveQs.clear();
        this.mAllViews.add((View) this.mQsPanel.getTileLayout());
        for (QSTile<?> tile : tiles) {
            QSTileBaseView tileView = this.mQsPanel.getTileView(tile);
            TextView label = ((QSTileView) tileView).getLabel();
            View tileIcon = tileView.getIcon().getIconView();
            if (count < this.mNumQuickTiles && this.mAllowFancy) {
                QSTileBaseView quickTileView = this.mQuickQsPanel.getTileView(tile);
                int lastX = loc1[0];
                getRelativePosition(loc1, quickTileView.getIcon(), this.mQsContainer);
                getRelativePosition(loc2, tileIcon, this.mQsContainer);
                int xDiff = loc2[0] - loc1[0];
                int yDiff = loc2[1] - loc1[1];
                lastXDiff = loc1[0] - lastX;
                translationXBuilder.addFloat(quickTileView, "translationX", 0.0f, xDiff);
                translationYBuilder.addFloat(quickTileView, "translationY", 0.0f, yDiff);
                firstPageBuilder.addFloat(tileView, "translationY", this.mQsPanel.getHeight(), 0.0f);
                translationXBuilder.addFloat(label, "translationX", -xDiff, 0.0f);
                translationYBuilder.addFloat(label, "translationY", -yDiff, 0.0f);
                this.mTopFiveQs.add(tileIcon);
                this.mAllViews.add(tileIcon);
                this.mAllViews.add(quickTileView);
            } else if (this.mFullRows && isIconInAnimatedRow(count)) {
                loc1[0] = loc1[0] + lastXDiff;
                getRelativePosition(loc2, tileIcon, this.mQsContainer);
                int xDiff2 = loc2[0] - loc1[0];
                int yDiff2 = loc2[1] - loc1[1];
                firstPageBuilder.addFloat(tileView, "translationY", this.mQsPanel.getHeight(), 0.0f);
                translationXBuilder.addFloat(tileView, "translationX", -xDiff2, 0.0f);
                translationYBuilder.addFloat(label, "translationY", -yDiff2, 0.0f);
                translationYBuilder.addFloat(tileIcon, "translationY", -yDiff2, 0.0f);
                this.mAllViews.add(tileIcon);
            } else {
                lastRowBuilder.addFloat(tileView, "alpha", 0.0f, 1.0f);
            }
            this.mAllViews.add(tileView);
            this.mAllViews.add(label);
            count++;
        }
        if (this.mAllowFancy) {
            this.mFirstPageAnimator = firstPageBuilder.setListener(this).build();
            this.mFirstPageDelayedAnimator = new TouchAnimator.Builder().setStartDelay(0.7f).addFloat(this.mQsPanel.getTileLayout(), "alpha", 0.0f, 1.0f).build();
            this.mLastRowAnimator = lastRowBuilder.setStartDelay(0.86f).build();
            Path path = new Path();
            path.moveTo(0.0f, 0.0f);
            path.cubicTo(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
            PathInterpolatorBuilder interpolatorBuilder = new PathInterpolatorBuilder(0.0f, 0.0f, 0.0f, 1.0f);
            translationXBuilder.setInterpolator(interpolatorBuilder.getXInterpolator());
            translationYBuilder.setInterpolator(interpolatorBuilder.getYInterpolator());
            this.mTranslationXAnimator = translationXBuilder.build();
            this.mTranslationYAnimator = translationYBuilder.build();
        }
        this.mNonfirstPageAnimator = new TouchAnimator.Builder().addFloat(this.mQuickQsPanel, "alpha", 1.0f, 0.0f).setListener(this.mNonFirstPageListener).setEndDelay(0.5f).build();
    }

    private boolean isIconInAnimatedRow(int count) {
        if (this.mPagedLayout == null) {
            return false;
        }
        int columnCount = this.mPagedLayout.getColumnCount();
        return count < (((this.mNumQuickTiles + columnCount) + (-1)) / columnCount) * columnCount;
    }

    private void getRelativePosition(int[] loc1, View view, View parent) {
        loc1[0] = (view.getWidth() / 2) + 0;
        loc1[1] = 0;
        getRelativePositionInt(loc1, view, parent);
    }

    private void getRelativePositionInt(int[] loc1, View view, View parent) {
        if (view == parent || view == null) {
            return;
        }
        if (!(view instanceof PagedTileLayout.TilePage)) {
            loc1[0] = loc1[0] + view.getLeft();
            loc1[1] = loc1[1] + view.getTop();
        }
        getRelativePositionInt(loc1, (View) view.getParent(), parent);
    }

    public void setPosition(float position) {
        if (this.mFirstPageAnimator == null || this.mOnKeyguard) {
            return;
        }
        this.mLastPosition = position;
        if (this.mOnFirstPage && this.mAllowFancy) {
            this.mQuickQsPanel.setAlpha(1.0f);
            this.mFirstPageAnimator.setPosition(position);
            this.mFirstPageDelayedAnimator.setPosition(position);
            this.mTranslationXAnimator.setPosition(position);
            this.mTranslationYAnimator.setPosition(position);
            this.mLastRowAnimator.setPosition(position);
            return;
        }
        this.mNonfirstPageAnimator.setPosition(position);
    }

    @Override
    public void onAnimationAtStart() {
        this.mQuickQsPanel.setVisibility(0);
    }

    @Override
    public void onAnimationAtEnd() {
        this.mQuickQsPanel.setVisibility(4);
        int N = this.mTopFiveQs.size();
        for (int i = 0; i < N; i++) {
            this.mTopFiveQs.get(i).setVisibility(0);
        }
    }

    @Override
    public void onAnimationStarted() {
        this.mQuickQsPanel.setVisibility(this.mOnKeyguard ? 4 : 0);
        if (!this.mOnFirstPage) {
            return;
        }
        int N = this.mTopFiveQs.size();
        for (int i = 0; i < N; i++) {
            this.mTopFiveQs.get(i).setVisibility(4);
        }
    }

    private void clearAnimationState() {
        int N = this.mAllViews.size();
        this.mQuickQsPanel.setAlpha(0.0f);
        for (int i = 0; i < N; i++) {
            View v = this.mAllViews.get(i);
            v.setAlpha(1.0f);
            v.setTranslationX(0.0f);
            v.setTranslationY(0.0f);
        }
        int N2 = this.mTopFiveQs.size();
        for (int i2 = 0; i2 < N2; i2++) {
            this.mTopFiveQs.get(i2).setVisibility(0);
        }
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        this.mQsPanel.post(this.mUpdateAnimators);
    }

    @Override
    public void onTilesChanged() {
        this.mQsPanel.post(this.mUpdateAnimators);
    }
}
