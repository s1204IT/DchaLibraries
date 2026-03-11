package com.android.systemui.statusbar.phone;

import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.NotificationListenerController;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.plugins.PluginManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

public class NotificationListenerWithPlugins extends NotificationListenerService implements PluginListener<NotificationListenerController> {
    private boolean mConnected;
    private ArrayList<NotificationListenerController> mPlugins = new ArrayList<>();

    public void registerAsSystemService(Context context, ComponentName componentName, int i) throws RemoteException {
        super.registerAsSystemService(context, componentName, i);
        ((PluginManager) Dependency.get(PluginManager.class)).addPluginListener(this, NotificationListenerController.class);
    }

    public void unregisterAsSystemService() throws RemoteException {
        super.unregisterAsSystemService();
        ((PluginManager) Dependency.get(PluginManager.class)).removePluginListener(this);
    }

    @Override
    public StatusBarNotification[] getActiveNotifications() {
        StatusBarNotification[] activeNotifications = super.getActiveNotifications();
        Iterator<NotificationListenerController> it = this.mPlugins.iterator();
        while (it.hasNext()) {
            activeNotifications = it.next().getActiveNotifications(activeNotifications);
        }
        return activeNotifications;
    }

    @Override
    public NotificationListenerService.RankingMap getCurrentRanking() {
        NotificationListenerService.RankingMap currentRanking = super.getCurrentRanking();
        Iterator<NotificationListenerController> it = this.mPlugins.iterator();
        while (it.hasNext()) {
            currentRanking = it.next().getCurrentRanking(currentRanking);
        }
        return currentRanking;
    }

    public void onPluginConnected() {
        this.mConnected = true;
        this.mPlugins.forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((NotificationListenerController) obj).onListenerConnected(this.f$0.getProvider());
            }
        });
    }

    public boolean onPluginNotificationPosted(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap) {
        for (NotificationListenerController notificationListenerController : this.mPlugins) {
            if (notificationListenerController.onNotificationPosted(statusBarNotification, rankingMap)) {
                if (StatusBar.DEBUG) {
                    Log.d("NotificationListenerWithPlugins", "onPluginNotificationPosted, plugin: " + notificationListenerController + ", sbn: " + statusBarNotification);
                    return true;
                }
                return true;
            }
        }
        return false;
    }

    public boolean onPluginNotificationRemoved(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap) {
        Iterator<NotificationListenerController> it = this.mPlugins.iterator();
        while (it.hasNext()) {
            if (it.next().onNotificationRemoved(statusBarNotification, rankingMap)) {
                return true;
            }
        }
        return false;
    }

    public NotificationListenerService.RankingMap onPluginRankingUpdate(NotificationListenerService.RankingMap rankingMap) {
        return getCurrentRanking();
    }

    @Override
    public void onPluginConnected(NotificationListenerController notificationListenerController, Context context) {
        this.mPlugins.add(notificationListenerController);
        if (this.mConnected) {
            notificationListenerController.onListenerConnected(getProvider());
        }
    }

    @Override
    public void onPluginDisconnected(NotificationListenerController notificationListenerController) {
        this.mPlugins.remove(notificationListenerController);
    }

    private NotificationListenerController.NotificationProvider getProvider() {
        return new NotificationListenerController.NotificationProvider() {
            @Override
            public StatusBarNotification[] getActiveNotifications() {
                return NotificationListenerWithPlugins.super.getActiveNotifications();
            }

            @Override
            public NotificationListenerService.RankingMap getRankingMap() {
                return NotificationListenerWithPlugins.super.getCurrentRanking();
            }

            @Override
            public void addNotification(StatusBarNotification statusBarNotification) {
                NotificationListenerWithPlugins.this.onNotificationPosted(statusBarNotification, getRankingMap());
            }

            @Override
            public void removeNotification(StatusBarNotification statusBarNotification) {
                NotificationListenerWithPlugins.this.onNotificationRemoved(statusBarNotification, getRankingMap());
            }

            @Override
            public void updateRanking() {
                NotificationListenerWithPlugins.this.onNotificationRankingUpdate(getRankingMap());
            }
        };
    }
}
