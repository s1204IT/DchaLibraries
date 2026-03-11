package com.android.systemui.statusbar;

import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.phone.NotificationListenerWithPlugins;
import com.android.systemui.statusbar.phone.StatusBar;

public class NotificationListener extends NotificationListenerWithPlugins {
    private final Context mContext;
    protected NotificationEntryManager mEntryManager;
    protected NotificationPresenter mPresenter;
    private final NotificationRemoteInputManager mRemoteInputManager = (NotificationRemoteInputManager) Dependency.get(NotificationRemoteInputManager.class);

    public NotificationListener(Context context) {
        this.mContext = context;
    }

    @Override
    public void onListenerConnected() {
        if (StatusBar.DEBUG) {
            Log.d("NotificationListener", "onListenerConnected");
        }
        onPluginConnected();
        final StatusBarNotification[] activeNotifications = getActiveNotifications();
        if (activeNotifications == null) {
            Log.w("NotificationListener", "onListenerConnected unable to get active notifications.");
        } else {
            final NotificationListenerService.RankingMap currentRanking = getCurrentRanking();
            this.mPresenter.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    NotificationListener.lambda$onListenerConnected$0(this.f$0, activeNotifications, currentRanking);
                }
            });
        }
    }

    public static void lambda$onListenerConnected$0(NotificationListener notificationListener, StatusBarNotification[] statusBarNotificationArr, NotificationListenerService.RankingMap rankingMap) {
        for (StatusBarNotification statusBarNotification : statusBarNotificationArr) {
            notificationListener.mEntryManager.addNotification(statusBarNotification, rankingMap);
        }
    }

    @Override
    public void onNotificationPosted(final StatusBarNotification statusBarNotification, final NotificationListenerService.RankingMap rankingMap) {
        if (StatusBar.DEBUG) {
            Log.d("NotificationListener", "onNotificationPosted: " + statusBarNotification);
        }
        if (statusBarNotification != null && !onPluginNotificationPosted(statusBarNotification, rankingMap)) {
            this.mPresenter.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    NotificationListener.lambda$onNotificationPosted$1(this.f$0, statusBarNotification, rankingMap);
                }
            });
        }
    }

    public static void lambda$onNotificationPosted$1(NotificationListener notificationListener, StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap) {
        RemoteInputController.processForRemoteInput(statusBarNotification.getNotification(), notificationListener.mContext);
        String key = statusBarNotification.getKey();
        notificationListener.mEntryManager.removeKeyKeptForRemoteInput(key);
        boolean z = notificationListener.mEntryManager.getNotificationData().get(key) != null;
        if (!StatusBar.ENABLE_CHILD_NOTIFICATIONS && notificationListener.mPresenter.getGroupManager().isChildInGroupWithSummary(statusBarNotification)) {
            if (StatusBar.DEBUG) {
                Log.d("NotificationListener", "Ignoring group child due to existing summary: " + statusBarNotification);
            }
            if (z) {
                if (StatusBar.DEBUG) {
                    Log.d("NotificationListener", "onNotificationPosted, removeNotification: " + statusBarNotification);
                }
                notificationListener.mEntryManager.removeNotification(key, rankingMap);
                return;
            }
            if (StatusBar.DEBUG) {
                Log.d("NotificationListener", "onNotificationPosted, updateRanking: " + statusBarNotification);
            }
            notificationListener.mEntryManager.getNotificationData().updateRanking(rankingMap);
            return;
        }
        if (z) {
            notificationListener.mEntryManager.updateNotification(statusBarNotification, rankingMap);
        } else {
            notificationListener.mEntryManager.addNotification(statusBarNotification, rankingMap);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification statusBarNotification, final NotificationListenerService.RankingMap rankingMap) {
        if (StatusBar.DEBUG) {
            Log.d("NotificationListener", "onNotificationRemoved: " + statusBarNotification);
        }
        if (statusBarNotification != null && !onPluginNotificationRemoved(statusBarNotification, rankingMap)) {
            final String key = statusBarNotification.getKey();
            this.mPresenter.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.mEntryManager.removeNotification(key, rankingMap);
                }
            });
        }
    }

    @Override
    public void onNotificationRankingUpdate(NotificationListenerService.RankingMap rankingMap) {
        if (StatusBar.DEBUG) {
            Log.d("NotificationListener", "onRankingUpdate");
        }
        if (rankingMap != null) {
            final NotificationListenerService.RankingMap rankingMapOnPluginRankingUpdate = onPluginRankingUpdate(rankingMap);
            this.mPresenter.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.mEntryManager.updateNotificationRanking(rankingMapOnPluginRankingUpdate);
                }
            });
        }
    }

    public void setUpWithPresenter(NotificationPresenter notificationPresenter, NotificationEntryManager notificationEntryManager) {
        this.mPresenter = notificationPresenter;
        this.mEntryManager = notificationEntryManager;
        try {
            registerAsSystemService(this.mContext, new ComponentName(this.mContext.getPackageName(), getClass().getCanonicalName()), -1);
        } catch (RemoteException e) {
            Log.e("NotificationListener", "Unable to register notification listener", e);
        }
    }
}
