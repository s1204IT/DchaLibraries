package com.android.settings.widget;

import android.app.ActionBar;
import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/* loaded from: classes.dex */
public class ActionBarShadowController implements LifecycleObserver, OnStart, OnStop {
    static final float ELEVATION_HIGH = 8.0f;
    static final float ELEVATION_LOW = 0.0f;
    private boolean isScrollWatcherAttached;
    private RecyclerView mRecyclerView;
    ScrollChangeWatcher mScrollChangeWatcher;

    public static ActionBarShadowController attachToRecyclerView(Activity activity, Lifecycle lifecycle, RecyclerView recyclerView) {
        return new ActionBarShadowController(activity, lifecycle, recyclerView);
    }

    public static ActionBarShadowController attachToRecyclerView(View view, Lifecycle lifecycle, RecyclerView recyclerView) {
        return new ActionBarShadowController(view, lifecycle, recyclerView);
    }

    private ActionBarShadowController(Activity activity, Lifecycle lifecycle, RecyclerView recyclerView) {
        this.mScrollChangeWatcher = new ScrollChangeWatcher(activity);
        this.mRecyclerView = recyclerView;
        attachScrollWatcher();
        lifecycle.addObserver(this);
    }

    private ActionBarShadowController(View view, Lifecycle lifecycle, RecyclerView recyclerView) {
        this.mScrollChangeWatcher = new ScrollChangeWatcher(view);
        this.mRecyclerView = recyclerView;
        attachScrollWatcher();
        lifecycle.addObserver(this);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStop
    public void onStop() {
        detachScrollWatcher();
    }

    private void detachScrollWatcher() {
        this.mRecyclerView.removeOnScrollListener(this.mScrollChangeWatcher);
        this.isScrollWatcherAttached = false;
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStart
    public void onStart() {
        attachScrollWatcher();
    }

    private void attachScrollWatcher() {
        if (!this.isScrollWatcherAttached) {
            this.isScrollWatcherAttached = true;
            this.mRecyclerView.addOnScrollListener(this.mScrollChangeWatcher);
            this.mScrollChangeWatcher.updateDropShadow(this.mRecyclerView);
        }
    }

    final class ScrollChangeWatcher extends RecyclerView.OnScrollListener {
        private final Activity mActivity;
        private final View mAnchorView;

        public ScrollChangeWatcher(Activity activity) {
            this.mActivity = activity;
            this.mAnchorView = null;
        }

        public ScrollChangeWatcher(View view) {
            this.mAnchorView = view;
            this.mActivity = null;
        }

        @Override // android.support.v7.widget.RecyclerView.OnScrollListener
        public void onScrolled(RecyclerView recyclerView, int i, int i2) {
            updateDropShadow(recyclerView);
        }

        public void updateDropShadow(View view) {
            ActionBar actionBar;
            boolean zCanScrollVertically = view.canScrollVertically(-1);
            if (this.mAnchorView != null) {
                this.mAnchorView.setElevation(zCanScrollVertically ? 8.0f : 0.0f);
            } else if (this.mActivity != null && (actionBar = this.mActivity.getActionBar()) != null) {
                actionBar.setElevation(zCanScrollVertically ? 8.0f : 0.0f);
            }
        }
    }
}
