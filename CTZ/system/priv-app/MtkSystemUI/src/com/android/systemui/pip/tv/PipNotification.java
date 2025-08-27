package com.android.systemui.pip.tv;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ParceledListSlice;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.text.TextUtils;
import android.util.Log;
import com.android.systemui.R;
import com.android.systemui.pip.tv.PipManager;
import com.android.systemui.util.NotificationChannels;

/* loaded from: classes.dex */
public class PipNotification {
    private Bitmap mArt;
    private int mDefaultIconResId;
    private String mDefaultTitle;
    private MediaController mMediaController;
    private final Notification.Builder mNotificationBuilder;
    private final NotificationManager mNotificationManager;
    private boolean mNotified;
    private String mTitle;
    private static final String NOTIFICATION_TAG = PipNotification.class.getName();
    private static final boolean DEBUG = PipManager.DEBUG;
    private final PipManager mPipManager = PipManager.getInstance();
    private PipManager.Listener mPipListener = new PipManager.Listener() { // from class: com.android.systemui.pip.tv.PipNotification.1
        AnonymousClass1() {
        }

        @Override // com.android.systemui.pip.tv.PipManager.Listener
        public void onPipEntered() {
            PipNotification.this.updateMediaControllerMetadata();
            PipNotification.this.notifyPipNotification();
        }

        @Override // com.android.systemui.pip.tv.PipManager.Listener
        public void onPipActivityClosed() {
            PipNotification.this.dismissPipNotification();
        }

        @Override // com.android.systemui.pip.tv.PipManager.Listener
        public void onShowPipMenu() {
        }

        @Override // com.android.systemui.pip.tv.PipManager.Listener
        public void onPipMenuActionsChanged(ParceledListSlice parceledListSlice) {
        }

        @Override // com.android.systemui.pip.tv.PipManager.Listener
        public void onMoveToFullscreen() {
            PipNotification.this.dismissPipNotification();
        }

        @Override // com.android.systemui.pip.tv.PipManager.Listener
        public void onPipResizeAboutToStart() {
        }
    };
    private MediaController.Callback mMediaControllerCallback = new MediaController.Callback() { // from class: com.android.systemui.pip.tv.PipNotification.2
        AnonymousClass2() {
        }

        @Override // android.media.session.MediaController.Callback
        public void onPlaybackStateChanged(PlaybackState playbackState) {
            if (PipNotification.this.updateMediaControllerMetadata() && PipNotification.this.mNotified) {
                PipNotification.this.notifyPipNotification();
            }
        }
    };
    private final PipManager.MediaListener mPipMediaListener = new PipManager.MediaListener() { // from class: com.android.systemui.pip.tv.PipNotification.3
        AnonymousClass3() {
        }

        @Override // com.android.systemui.pip.tv.PipManager.MediaListener
        public void onMediaControllerChanged() {
            MediaController mediaController = PipNotification.this.mPipManager.getMediaController();
            if (PipNotification.this.mMediaController != mediaController) {
                if (PipNotification.this.mMediaController != null) {
                    PipNotification.this.mMediaController.unregisterCallback(PipNotification.this.mMediaControllerCallback);
                }
                PipNotification.this.mMediaController = mediaController;
                if (PipNotification.this.mMediaController != null) {
                    PipNotification.this.mMediaController.registerCallback(PipNotification.this.mMediaControllerCallback);
                }
                if (PipNotification.this.updateMediaControllerMetadata() && PipNotification.this.mNotified) {
                    PipNotification.this.notifyPipNotification();
                }
            }
        }
    };
    private final BroadcastReceiver mEventReceiver = new BroadcastReceiver() { // from class: com.android.systemui.pip.tv.PipNotification.4
        AnonymousClass4() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (PipNotification.DEBUG) {
                Log.d("PipNotification", "Received " + intent.getAction() + " from the notification UI");
            }
            String action = intent.getAction();
            char c = 65535;
            int iHashCode = action.hashCode();
            if (iHashCode != -1402086132) {
                if (iHashCode == 1201988555 && action.equals("PipNotification.menu")) {
                    c = 0;
                }
            } else if (action.equals("PipNotification.close")) {
                c = 1;
            }
            switch (c) {
                case 0:
                    PipNotification.this.mPipManager.showPictureInPictureMenu();
                    break;
                case 1:
                    PipNotification.this.mPipManager.closePip();
                    break;
            }
        }
    };

    /* renamed from: com.android.systemui.pip.tv.PipNotification$1 */
    class AnonymousClass1 implements PipManager.Listener {
        AnonymousClass1() {
        }

        @Override // com.android.systemui.pip.tv.PipManager.Listener
        public void onPipEntered() {
            PipNotification.this.updateMediaControllerMetadata();
            PipNotification.this.notifyPipNotification();
        }

        @Override // com.android.systemui.pip.tv.PipManager.Listener
        public void onPipActivityClosed() {
            PipNotification.this.dismissPipNotification();
        }

        @Override // com.android.systemui.pip.tv.PipManager.Listener
        public void onShowPipMenu() {
        }

        @Override // com.android.systemui.pip.tv.PipManager.Listener
        public void onPipMenuActionsChanged(ParceledListSlice parceledListSlice) {
        }

        @Override // com.android.systemui.pip.tv.PipManager.Listener
        public void onMoveToFullscreen() {
            PipNotification.this.dismissPipNotification();
        }

        @Override // com.android.systemui.pip.tv.PipManager.Listener
        public void onPipResizeAboutToStart() {
        }
    }

    /* renamed from: com.android.systemui.pip.tv.PipNotification$2 */
    class AnonymousClass2 extends MediaController.Callback {
        AnonymousClass2() {
        }

        @Override // android.media.session.MediaController.Callback
        public void onPlaybackStateChanged(PlaybackState playbackState) {
            if (PipNotification.this.updateMediaControllerMetadata() && PipNotification.this.mNotified) {
                PipNotification.this.notifyPipNotification();
            }
        }
    }

    /* renamed from: com.android.systemui.pip.tv.PipNotification$3 */
    class AnonymousClass3 implements PipManager.MediaListener {
        AnonymousClass3() {
        }

        @Override // com.android.systemui.pip.tv.PipManager.MediaListener
        public void onMediaControllerChanged() {
            MediaController mediaController = PipNotification.this.mPipManager.getMediaController();
            if (PipNotification.this.mMediaController != mediaController) {
                if (PipNotification.this.mMediaController != null) {
                    PipNotification.this.mMediaController.unregisterCallback(PipNotification.this.mMediaControllerCallback);
                }
                PipNotification.this.mMediaController = mediaController;
                if (PipNotification.this.mMediaController != null) {
                    PipNotification.this.mMediaController.registerCallback(PipNotification.this.mMediaControllerCallback);
                }
                if (PipNotification.this.updateMediaControllerMetadata() && PipNotification.this.mNotified) {
                    PipNotification.this.notifyPipNotification();
                }
            }
        }
    }

    /* renamed from: com.android.systemui.pip.tv.PipNotification$4 */
    class AnonymousClass4 extends BroadcastReceiver {
        AnonymousClass4() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (PipNotification.DEBUG) {
                Log.d("PipNotification", "Received " + intent.getAction() + " from the notification UI");
            }
            String action = intent.getAction();
            char c = 65535;
            int iHashCode = action.hashCode();
            if (iHashCode != -1402086132) {
                if (iHashCode == 1201988555 && action.equals("PipNotification.menu")) {
                    c = 0;
                }
            } else if (action.equals("PipNotification.close")) {
                c = 1;
            }
            switch (c) {
                case 0:
                    PipNotification.this.mPipManager.showPictureInPictureMenu();
                    break;
                case 1:
                    PipNotification.this.mPipManager.closePip();
                    break;
            }
        }
    }

    public PipNotification(Context context) {
        this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
        this.mNotificationBuilder = new Notification.Builder(context, NotificationChannels.TVPIP).setLocalOnly(true).setOngoing(false).setCategory("sys").extend(new Notification.TvExtender().setContentIntent(createPendingIntent(context, "PipNotification.menu")).setDeleteIntent(createPendingIntent(context, "PipNotification.close")));
        this.mPipManager.addListener(this.mPipListener);
        this.mPipManager.addMediaListener(this.mPipMediaListener);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("PipNotification.menu");
        intentFilter.addAction("PipNotification.close");
        context.registerReceiver(this.mEventReceiver, intentFilter);
        onConfigurationChanged(context);
    }

    void onConfigurationChanged(Context context) {
        this.mDefaultTitle = context.getResources().getString(R.string.pip_notification_unknown_title);
        this.mDefaultIconResId = R.drawable.pip_icon;
        if (this.mNotified) {
            notifyPipNotification();
        }
    }

    private void notifyPipNotification() {
        this.mNotified = true;
        this.mNotificationBuilder.setShowWhen(true).setWhen(System.currentTimeMillis()).setSmallIcon(this.mDefaultIconResId).setContentTitle(!TextUtils.isEmpty(this.mTitle) ? this.mTitle : this.mDefaultTitle);
        if (this.mArt != null) {
            this.mNotificationBuilder.setStyle(new Notification.BigPictureStyle().bigPicture(this.mArt));
        } else {
            this.mNotificationBuilder.setStyle(null);
        }
        this.mNotificationManager.notify(NOTIFICATION_TAG, 1100, this.mNotificationBuilder.build());
    }

    private void dismissPipNotification() {
        this.mNotified = false;
        this.mNotificationManager.cancel(NOTIFICATION_TAG, 1100);
    }

    private boolean updateMediaControllerMetadata() {
        Bitmap bitmap;
        MediaMetadata metadata;
        String string = null;
        if (this.mPipManager.getMediaController() == null || (metadata = this.mPipManager.getMediaController().getMetadata()) == null) {
            bitmap = null;
        } else {
            string = metadata.getString("android.media.metadata.DISPLAY_TITLE");
            if (TextUtils.isEmpty(string)) {
                string = metadata.getString("android.media.metadata.TITLE");
            }
            Bitmap bitmap2 = metadata.getBitmap("android.media.metadata.ALBUM_ART");
            if (bitmap2 == null) {
                bitmap = metadata.getBitmap("android.media.metadata.ART");
            } else {
                bitmap = bitmap2;
            }
        }
        if (!TextUtils.equals(string, this.mTitle) || bitmap != this.mArt) {
            this.mTitle = string;
            this.mArt = bitmap;
            return true;
        }
        return false;
    }

    private static PendingIntent createPendingIntent(Context context, String str) {
        return PendingIntent.getBroadcast(context, 0, new Intent(str), 268435456);
    }
}
