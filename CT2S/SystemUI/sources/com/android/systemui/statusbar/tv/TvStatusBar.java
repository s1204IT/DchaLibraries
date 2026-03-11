package com.android.systemui.statusbar.tv;

import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.statusbar.ActivatableNotificationView;
import com.android.systemui.statusbar.BaseStatusBar;

public class TvStatusBar extends BaseStatusBar {
    @Override
    public void addIcon(String slot, int index, int viewIndex, StatusBarIcon icon) {
    }

    @Override
    public void updateIcon(String slot, int index, int viewIndex, StatusBarIcon old, StatusBarIcon icon) {
    }

    @Override
    public void removeIcon(String slot, int index, int viewIndex) {
    }

    @Override
    public void addNotification(StatusBarNotification notification, NotificationListenerService.RankingMap ranking) {
    }

    @Override
    protected void updateNotificationRanking(NotificationListenerService.RankingMap ranking) {
    }

    @Override
    public void removeNotification(String key, NotificationListenerService.RankingMap ranking) {
    }

    @Override
    public void disable(int state, boolean animate) {
    }

    @Override
    public void animateExpandNotificationsPanel() {
    }

    @Override
    public void animateCollapsePanels(int flags) {
    }

    @Override
    public void setSystemUiVisibility(int vis, int mask) {
    }

    @Override
    public void topAppWindowChanged(boolean visible) {
    }

    @Override
    public void setImeWindowStatus(IBinder token, int vis, int backDisposition, boolean showImeSwitcher) {
    }

    @Override
    public void toggleRecentApps() {
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
    protected WindowManager.LayoutParams getSearchLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return null;
    }

    @Override
    protected void haltTicker() {
    }

    @Override
    protected void setAreThereNotifications() {
    }

    @Override
    protected void updateNotifications() {
    }

    @Override
    protected void tick(StatusBarNotification n, boolean firstTime) {
    }

    @Override
    protected void updateExpandedViewPos(int expandedPosition) {
    }

    @Override
    protected boolean shouldDisableNavbarGestures() {
        return true;
    }

    @Override
    public View getStatusBarView() {
        return null;
    }

    @Override
    public void resetHeadsUpDecayTimer() {
    }

    @Override
    public void scheduleHeadsUpEscalation() {
    }

    @Override
    protected int getMaxKeyguardNotifications() {
        return 0;
    }

    @Override
    public void animateExpandSettingsPanel() {
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
    public void showScreenPinningRequest() {
    }
}
