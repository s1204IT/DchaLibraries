package android.support.v4.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompatBase;
import android.widget.RemoteViews;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
class NotificationCompatApi24 {
    NotificationCompatApi24() {
    }

    /* loaded from: classes.dex */
    public static class Builder implements NotificationBuilderWithBuilderAccessor, NotificationBuilderWithActions {
        private Notification.Builder b;

        public Builder(Context context, Notification n, CharSequence contentTitle, CharSequence contentText, CharSequence contentInfo, RemoteViews tickerView, int number, PendingIntent contentIntent, PendingIntent fullScreenIntent, Bitmap largeIcon, int progressMax, int progress, boolean progressIndeterminate, boolean showWhen, boolean useChronometer, int priority, CharSequence subText, boolean localOnly, String category, ArrayList<String> people, Bundle extras, int color, int visibility, Notification publicVersion, String groupKey, boolean groupSummary, String sortKey, CharSequence[] remoteInputHistory, RemoteViews contentView, RemoteViews bigContentView, RemoteViews headsUpContentView) {
            this.b = new Notification.Builder(context).setWhen(n.when).setShowWhen(showWhen).setSmallIcon(n.icon, n.iconLevel).setContent(n.contentView).setTicker(n.tickerText, tickerView).setSound(n.sound, n.audioStreamType).setVibrate(n.vibrate).setLights(n.ledARGB, n.ledOnMS, n.ledOffMS).setOngoing((n.flags & 2) != 0).setOnlyAlertOnce((n.flags & 8) != 0).setAutoCancel((n.flags & 16) != 0).setDefaults(n.defaults).setContentTitle(contentTitle).setContentText(contentText).setSubText(subText).setContentInfo(contentInfo).setContentIntent(contentIntent).setDeleteIntent(n.deleteIntent).setFullScreenIntent(fullScreenIntent, (n.flags & 128) != 0).setLargeIcon(largeIcon).setNumber(number).setUsesChronometer(useChronometer).setPriority(priority).setProgress(progressMax, progress, progressIndeterminate).setLocalOnly(localOnly).setExtras(extras).setGroup(groupKey).setGroupSummary(groupSummary).setSortKey(sortKey).setCategory(category).setColor(color).setVisibility(visibility).setPublicVersion(publicVersion).setRemoteInputHistory(remoteInputHistory);
            if (contentView != null) {
                this.b.setCustomContentView(contentView);
            }
            if (bigContentView != null) {
                this.b.setCustomBigContentView(bigContentView);
            }
            if (headsUpContentView != null) {
                this.b.setCustomHeadsUpContentView(headsUpContentView);
            }
            for (String person : people) {
                this.b.addPerson(person);
            }
        }

        @Override // android.support.v4.app.NotificationBuilderWithActions
        public void addAction(NotificationCompatBase.Action action) {
            Bundle actionExtras;
            android.app.RemoteInput[] fromCompat;
            Notification.Action.Builder actionBuilder = new Notification.Action.Builder(action.getIcon(), action.getTitle(), action.getActionIntent());
            if (action.getRemoteInputs() != null) {
                for (android.app.RemoteInput remoteInput : RemoteInputCompatApi20.fromCompat(action.getRemoteInputs())) {
                    actionBuilder.addRemoteInput(remoteInput);
                }
            }
            if (action.getExtras() != null) {
                actionExtras = new Bundle(action.getExtras());
            } else {
                actionExtras = new Bundle();
            }
            actionExtras.putBoolean("android.support.allowGeneratedReplies", action.getAllowGeneratedReplies());
            actionBuilder.addExtras(actionExtras);
            actionBuilder.setAllowGeneratedReplies(action.getAllowGeneratedReplies());
            this.b.addAction(actionBuilder.build());
        }

        @Override // android.support.v4.app.NotificationBuilderWithBuilderAccessor
        public Notification.Builder getBuilder() {
            return this.b;
        }

        @Override // android.support.v4.app.NotificationBuilderWithBuilderAccessor
        public Notification build() {
            return this.b.build();
        }
    }

    public static void addMessagingStyle(NotificationBuilderWithBuilderAccessor b, CharSequence userDisplayName, CharSequence conversationTitle, List<CharSequence> texts, List<Long> timestamps, List<CharSequence> senders, List<String> dataMimeTypes, List<Uri> dataUris) {
        Notification.MessagingStyle style = new Notification.MessagingStyle(userDisplayName).setConversationTitle(conversationTitle);
        for (int i = 0; i < texts.size(); i++) {
            Notification.MessagingStyle.Message message = new Notification.MessagingStyle.Message(texts.get(i), timestamps.get(i).longValue(), senders.get(i));
            if (dataMimeTypes.get(i) != null) {
                message.setData(dataMimeTypes.get(i), dataUris.get(i));
            }
            style.addMessage(message);
        }
        style.setBuilder(b.getBuilder());
    }
}
