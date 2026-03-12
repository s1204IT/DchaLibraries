package com.android.systemui.recent;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.MotionEvent;
import android.view.View;
import com.android.systemui.R;
import com.android.systemui.statusbar.StatusBarPanel;
import java.util.List;

public class RecentsActivity extends Activity {
    private boolean mForeground;
    private IntentFilter mIntentFilter;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.android.systemui.recent.action.CLOSE".equals(intent.getAction())) {
                if (RecentsActivity.this.mRecentsPanel != null && RecentsActivity.this.mRecentsPanel.isShowing() && RecentsActivity.this.mShowing && !RecentsActivity.this.mForeground) {
                    RecentsActivity.this.mRecentsPanel.show(false);
                    return;
                }
                return;
            }
            if ("com.android.systemui.recent.action.WINDOW_ANIMATION_START".equals(intent.getAction()) && RecentsActivity.this.mRecentsPanel != null) {
                RecentsActivity.this.mRecentsPanel.onWindowAnimationStart();
            }
        }
    };
    private RecentsPanelView mRecentsPanel;
    private boolean mShowing;

    public class TouchOutsideListener implements View.OnTouchListener {
        private StatusBarPanel mPanel;

        public TouchOutsideListener(StatusBarPanel panel) {
            this.mPanel = panel;
        }

        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            int action = ev.getAction();
            if (action != 4 && (action != 0 || this.mPanel.isInContentArea((int) ev.getX(), (int) ev.getY()))) {
                return false;
            }
            RecentsActivity.this.dismissAndGoHome();
            return true;
        }
    }

    @Override
    public void onPause() {
        overridePendingTransition(R.anim.recents_return_to_launcher_enter, R.anim.recents_return_to_launcher_exit);
        this.mForeground = false;
        super.onPause();
    }

    @Override
    public void onStop() {
        this.mShowing = false;
        if (this.mRecentsPanel != null) {
            this.mRecentsPanel.onUiHidden();
        }
        super.onStop();
    }

    private void updateWallpaperVisibility(boolean visible) {
        int wpflags = visible ? 1048576 : 0;
        int curflags = getWindow().getAttributes().flags & 1048576;
        if (wpflags != curflags) {
            getWindow().setFlags(wpflags, 1048576);
        }
    }

    public static boolean forceOpaqueBackground(Context context) {
        return WallpaperManager.getInstance(context).getWallpaperInfo() != null;
    }

    @Override
    public void onStart() {
        if (forceOpaqueBackground(this)) {
            updateWallpaperVisibility(false);
        } else {
            updateWallpaperVisibility(true);
        }
        this.mShowing = true;
        if (this.mRecentsPanel != null) {
            this.mRecentsPanel.refreshRecentTasksList();
            this.mRecentsPanel.refreshViews();
        }
        super.onStart();
    }

    @Override
    public void onResume() {
        this.mForeground = true;
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        dismissAndGoBack();
    }

    public void dismissAndGoHome() {
        if (this.mRecentsPanel != null) {
            Intent homeIntent = new Intent("android.intent.action.MAIN", (Uri) null);
            homeIntent.addCategory("android.intent.category.HOME");
            homeIntent.addFlags(270532608);
            startActivityAsUser(homeIntent, new UserHandle(-2));
            this.mRecentsPanel.show(false);
        }
    }

    public void dismissAndGoBack() {
        if (this.mRecentsPanel != null) {
            ActivityManager am = (ActivityManager) getSystemService("activity");
            List<ActivityManager.RecentTaskInfo> recentTasks = am.getRecentTasks(2, 7);
            if (recentTasks.size() <= 1 || !this.mRecentsPanel.simulateClick(recentTasks.get(1).persistentId)) {
                this.mRecentsPanel.show(false);
            } else {
                return;
            }
        }
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addPrivateFlags(512);
        setContentView(R.layout.status_bar_recent_panel);
        this.mRecentsPanel = (RecentsPanelView) findViewById(R.id.recents_root);
        this.mRecentsPanel.setOnTouchListener(new TouchOutsideListener(this.mRecentsPanel));
        this.mRecentsPanel.setSystemUiVisibility(1792);
        RecentTasksLoader recentTasksLoader = RecentTasksLoader.getInstance(this);
        recentTasksLoader.setRecentsPanel(this.mRecentsPanel, this.mRecentsPanel);
        this.mRecentsPanel.setMinSwipeAlpha(getResources().getInteger(R.integer.config_recent_item_min_alpha) / 100.0f);
        if (savedInstanceState == null || savedInstanceState.getBoolean("was_showing")) {
            handleIntent(getIntent(), savedInstanceState == null);
        }
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("com.android.systemui.recent.action.CLOSE");
        this.mIntentFilter.addAction("com.android.systemui.recent.action.WINDOW_ANIMATION_START");
        registerReceiver(this.mIntentReceiver, this.mIntentFilter);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("was_showing", this.mRecentsPanel.isShowing());
    }

    @Override
    protected void onDestroy() {
        RecentTasksLoader.getInstance(this).setRecentsPanel(null, this.mRecentsPanel);
        unregisterReceiver(this.mIntentReceiver);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent, true);
    }

    private void handleIntent(Intent intent, boolean checkWaitingForAnimationParam) {
        boolean waitingForWindowAnimation = false;
        super.onNewIntent(intent);
        if ("com.android.systemui.recent.action.TOGGLE_RECENTS".equals(intent.getAction()) && this.mRecentsPanel != null) {
            if (this.mRecentsPanel.isShowing()) {
                dismissAndGoBack();
                return;
            }
            RecentTasksLoader recentTasksLoader = RecentTasksLoader.getInstance(this);
            if (checkWaitingForAnimationParam && intent.getBooleanExtra("com.android.systemui.recent.WAITING_FOR_WINDOW_ANIMATION", false)) {
                waitingForWindowAnimation = true;
            }
            this.mRecentsPanel.show(true, recentTasksLoader.getLoadedTasks(), recentTasksLoader.isFirstScreenful(), waitingForWindowAnimation);
        }
    }

    boolean isActivityShowing() {
        return this.mShowing;
    }
}
