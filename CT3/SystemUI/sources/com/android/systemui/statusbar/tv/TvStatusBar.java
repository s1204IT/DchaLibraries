package com.android.systemui.statusbar.tv;

import android.content.ComponentName;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.statusbar.ActivatableNotificationView;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.tv.pip.PipManager;

public class TvStatusBar extends BaseStatusBar {
    int mSystemUiVisibility = 0;
    private int mLastDispatchedSystemUiVisibility = -1;

    @Override
    public void setIcon(String slot, StatusBarIcon icon) {
    }

    @Override
    public void removeIcon(String slot) {
    }

    @Override
    public void addNotification(StatusBarNotification notification, NotificationListenerService.RankingMap ranking, NotificationData.Entry entry) {
    }

    @Override
    protected void updateNotificationRanking(NotificationListenerService.RankingMap ranking) {
    }

    @Override
    public void removeNotification(String key, NotificationListenerService.RankingMap ranking) {
    }

    @Override
    public void disable(int state1, int state2, boolean animate) {
    }

    @Override
    public void animateExpandNotificationsPanel() {
    }

    @Override
    public void animateCollapsePanels(int flags) {
    }

    @Override
    public void setSystemUiVisibility(int vis, int fullscreenStackVis, int dockedStackVis, int mask, Rect fullscreenStackBounds, Rect dockedStackBounds) {
    }

    @Override
    public void topAppWindowChanged(boolean visible) {
    }

    @Override
    public void setImeWindowStatus(IBinder token, int vis, int backDisposition, boolean showImeSwitcher) {
    }

    @Override
    public void setWindowState(int window, int state) {
    }

    @Override
    public void buzzBeepBlinked() {
    }

    @Override
    public void notificationLightOff() {
    }

    @Override
    public void notificationLightPulse(int argb, int onMillis, int offMillis) {
    }

    @Override
    protected void setAreThereNotifications() {
    }

    @Override
    protected void updateNotifications() {
    }

    @Override
    protected void toggleSplitScreenMode(int metricsDockAction, int metricsUndockAction) {
    }

    @Override
    public void maybeEscalateHeadsUp() {
    }

    @Override
    public boolean isPanelFullyCollapsed() {
        return false;
    }

    @Override
    protected int getMaxKeyguardNotifications(boolean recompute) {
        return 0;
    }

    @Override
    public void animateExpandSettingsPanel(String subPanel) {
    }

    @Override
    protected void createAndAddWindows() {
    }

    @Override
    protected void refreshLayout(int layoutDirection) {
    }

    @Override
    public void onActivated(ActivatableNotificationView view) {
    }

    @Override
    public void onActivationReset(ActivatableNotificationView view) {
    }

    @Override
    public void showScreenPinningRequest(int taskId) {
    }

    @Override
    public void appTransitionPending() {
    }

    @Override
    public void appTransitionCancelled() {
    }

    @Override
    public void appTransitionStarting(long startTime, long duration) {
    }

    @Override
    public void appTransitionFinished() {
    }

    @Override
    public void onCameraLaunchGestureDetected(int source) {
    }

    @Override
    public void showTvPictureInPictureMenu() {
        PipManager.getInstance().showTvPictureInPictureMenu();
    }

    @Override
    protected void updateHeadsUp(String key, NotificationData.Entry entry, boolean shouldPeek, boolean alertAgain) {
    }

    @Override
    protected void setHeadsUpUser(int newUserId) {
    }

    @Override
    protected boolean isSnoozedPackage(StatusBarNotification sbn) {
        return false;
    }

    @Override
    public void addQsTile(ComponentName tile) {
    }

    @Override
    public void remQsTile(ComponentName tile) {
    }

    @Override
    public void clickTile(ComponentName tile) {
    }

    @Override
    public void start() {
        super.start();
        putComponent(TvStatusBar.class, this);
    }

    public void updatePipVisibility(boolean visible) {
        if (visible) {
            this.mSystemUiVisibility |= 65536;
        } else {
            this.mSystemUiVisibility &= -65537;
        }
        notifyUiVisibilityChanged(this.mSystemUiVisibility);
    }

    public void updateRecentsVisibility(boolean visible) {
        if (visible) {
            this.mSystemUiVisibility |= 16384;
        } else {
            this.mSystemUiVisibility &= -16385;
        }
        notifyUiVisibilityChanged(this.mSystemUiVisibility);
    }

    private void notifyUiVisibilityChanged(int vis) {
        try {
            if (this.mLastDispatchedSystemUiVisibility == vis) {
                return;
            }
            this.mWindowManagerService.statusBarVisibilityChanged(vis);
            this.mLastDispatchedSystemUiVisibility = vis;
        } catch (RemoteException e) {
        }
    }
}
