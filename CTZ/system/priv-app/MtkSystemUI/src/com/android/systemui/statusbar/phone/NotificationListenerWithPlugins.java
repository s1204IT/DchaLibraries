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

/* loaded from: classes.dex */
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

    @Override // android.service.notification.NotificationListenerService
    public StatusBarNotification[] getActiveNotifications() {
        StatusBarNotification[] activeNotifications = super.getActiveNotifications();
        Iterator<NotificationListenerController> it = this.mPlugins.iterator();
        while (it.hasNext()) {
            activeNotifications = it.next().getActiveNotifications(activeNotifications);
        }
        return activeNotifications;
    }

    @Override // android.service.notification.NotificationListenerService
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
        this.mPlugins.forEach(new Consumer() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$NotificationListenerWithPlugins$AOWJwBGrUF4vFOVx-Lxlu4eVQD0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((NotificationListenerController) obj).onListenerConnected(this.f$0.getProvider());
            }
        });
    }

    public boolean onPluginNotificationPosted(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap) {
        Iterator<NotificationListenerController> it = this.mPlugins.iterator();
        while (it.hasNext()) {
            NotificationListenerController next = it.next();
            if (next.onNotificationPosted(statusBarNotification, rankingMap)) {
                if (StatusBar.DEBUG) {
                    Log.d("NotificationListenerWithPlugins", "onPluginNotificationPosted, plugin: " + next + ", sbn: " + statusBarNotification);
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

    /* JADX DEBUG: Method merged with bridge method: onPluginConnected(Lcom/android/systemui/plugins/Plugin;Landroid/content/Context;)V */
    @Override // com.android.systemui.plugins.PluginListener
    public void onPluginConnected(NotificationListenerController notificationListenerController, Context context) {
        this.mPlugins.add(notificationListenerController);
        if (this.mConnected) {
            notificationListenerController.onListenerConnected(getProvider());
        }
    }

    /* JADX DEBUG: Method merged with bridge method: onPluginDisconnected(Lcom/android/systemui/plugins/Plugin;)V */
    @Override // com.android.systemui.plugins.PluginListener
    public void onPluginDisconnected(NotificationListenerController notificationListenerController) {
        this.mPlugins.remove(notificationListenerController);
    }

    private NotificationListenerController.NotificationProvider getProvider() {
        return new NotificationListenerController.NotificationProvider() { // from class: com.android.systemui.statusbar.phone.NotificationListenerWithPlugins.1
            @Override // com.android.systemui.plugins.NotificationListenerController.NotificationProvider
            public StatusBarNotification[] getActiveNotifications() {
                return NotificationListenerWithPlugins.super.getActiveNotifications();
            }

            @Override // com.android.systemui.plugins.NotificationListenerController.NotificationProvider
            public NotificationListenerService.RankingMap getRankingMap() {
                return NotificationListenerWithPlugins.super.getCurrentRanking();
            }

            @Override // com.android.systemui.plugins.NotificationListenerController.NotificationProvider
            public void addNotification(StatusBarNotification statusBarNotification) {
                NotificationListenerWithPlugins.this.onNotificationPosted(statusBarNotification, getRankingMap());
            }

            @Override // com.android.systemui.plugins.NotificationListenerController.NotificationProvider
            public void removeNotification(StatusBarNotification statusBarNotification) {
                NotificationListenerWithPlugins.this.onNotificationRemoved(statusBarNotification, getRankingMap());
            }

            @Override // com.android.systemui.plugins.NotificationListenerController.NotificationProvider
            public void updateRanking() {
                NotificationListenerWithPlugins.this.onNotificationRankingUpdate(getRankingMap());
            }
        };
    }
}
