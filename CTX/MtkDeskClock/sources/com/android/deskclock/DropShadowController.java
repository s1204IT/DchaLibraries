package com.android.deskclock;

import android.animation.ValueAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.uidata.TabScrollListener;
import com.android.deskclock.uidata.UiDataModel;

public final class DropShadowController {
    private final ValueAnimator mDropShadowAnimator;
    private final View mDropShadowView;
    private View mHairlineView;
    private ListView mListView;
    private RecyclerView mRecyclerView;
    private final ScrollChangeWatcher mScrollChangeWatcher;
    private UiDataModel mUiDataModel;

    public DropShadowController(View view, UiDataModel uiDataModel, View view2) {
        this(view);
        this.mUiDataModel = uiDataModel;
        this.mUiDataModel.addTabScrollListener(this.mScrollChangeWatcher);
        this.mHairlineView = view2;
        updateDropShadow(!uiDataModel.isSelectedTabScrolledToTop());
    }

    public DropShadowController(View view, ListView listView) {
        this(view);
        this.mListView = listView;
        this.mListView.setOnScrollListener(this.mScrollChangeWatcher);
        updateDropShadow(!Utils.isScrolledToTop(listView));
    }

    public DropShadowController(View view, RecyclerView recyclerView) {
        this(view);
        this.mRecyclerView = recyclerView;
        this.mRecyclerView.addOnScrollListener(this.mScrollChangeWatcher);
        updateDropShadow(!Utils.isScrolledToTop(recyclerView));
    }

    private DropShadowController(View view) {
        this.mScrollChangeWatcher = new ScrollChangeWatcher();
        this.mDropShadowView = view;
        this.mDropShadowAnimator = AnimatorUtils.getAlphaAnimator(this.mDropShadowView, 0.0f, 1.0f).setDuration(UiDataModel.getUiDataModel().getShortAnimationDuration());
    }

    public void stop() {
        if (this.mRecyclerView != null) {
            this.mRecyclerView.removeOnScrollListener(this.mScrollChangeWatcher);
        } else if (this.mListView != null) {
            this.mListView.setOnScrollListener(null);
        } else if (this.mUiDataModel != null) {
            this.mUiDataModel.removeTabScrollListener(this.mScrollChangeWatcher);
        }
    }

    private void updateDropShadow(boolean z) {
        if (!z && this.mDropShadowView.getAlpha() != 0.0f) {
            if (DataModel.getDataModel().isApplicationInForeground()) {
                this.mDropShadowAnimator.reverse();
            } else {
                this.mDropShadowView.setAlpha(0.0f);
            }
            if (this.mHairlineView != null) {
                this.mHairlineView.setVisibility(0);
            }
        }
        if (z && this.mDropShadowView.getAlpha() != 1.0f) {
            if (DataModel.getDataModel().isApplicationInForeground()) {
                this.mDropShadowAnimator.start();
            } else {
                this.mDropShadowView.setAlpha(1.0f);
            }
            if (this.mHairlineView != null) {
                this.mHairlineView.setVisibility(4);
            }
        }
    }

    private final class ScrollChangeWatcher extends RecyclerView.OnScrollListener implements TabScrollListener, AbsListView.OnScrollListener {
        private ScrollChangeWatcher() {
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int i, int i2) {
            DropShadowController.this.updateDropShadow(!Utils.isScrolledToTop(recyclerView));
        }

        @Override
        public void onScrollStateChanged(AbsListView absListView, int i) {
        }

        @Override
        public void onScroll(AbsListView absListView, int i, int i2, int i3) {
            DropShadowController.this.updateDropShadow(!Utils.isScrolledToTop(absListView));
        }

        @Override
        public void selectedTabScrollToTopChanged(UiDataModel.Tab tab, boolean z) {
            DropShadowController.this.updateDropShadow(!z);
        }
    }
}
