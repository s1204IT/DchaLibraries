package android.support.v4.media.session;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;
import android.view.KeyEvent;
import java.util.List;

public class MediaButtonReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !"android.intent.action.MEDIA_BUTTON".equals(intent.getAction()) || !intent.hasExtra("android.intent.extra.KEY_EVENT")) {
            Log.d("MediaButtonReceiver", "Ignore unsupported intent: " + intent);
            return;
        }
        ComponentName mediaButtonServiceComponentName = getServiceComponentByAction(context, "android.intent.action.MEDIA_BUTTON");
        if (mediaButtonServiceComponentName != null) {
            intent.setComponent(mediaButtonServiceComponentName);
            startForegroundService(context, intent);
            return;
        }
        ComponentName mediaBrowserServiceComponentName = getServiceComponentByAction(context, "android.media.browse.MediaBrowserService");
        if (mediaBrowserServiceComponentName != null) {
            BroadcastReceiver.PendingResult pendingResult = goAsync();
            Context applicationContext = context.getApplicationContext();
            MediaButtonConnectionCallback connectionCallback = new MediaButtonConnectionCallback(applicationContext, intent, pendingResult);
            MediaBrowserCompat mediaBrowser = new MediaBrowserCompat(applicationContext, mediaBrowserServiceComponentName, connectionCallback, null);
            connectionCallback.setMediaBrowser(mediaBrowser);
            mediaBrowser.connect();
            return;
        }
        throw new IllegalStateException("Could not find any Service that handles android.intent.action.MEDIA_BUTTON or implements a media browser service.");
    }

    private static class MediaButtonConnectionCallback extends MediaBrowserCompat.ConnectionCallback {
        private final Context mContext;
        private final Intent mIntent;
        private MediaBrowserCompat mMediaBrowser;
        private final BroadcastReceiver.PendingResult mPendingResult;

        MediaButtonConnectionCallback(Context context, Intent intent, BroadcastReceiver.PendingResult pendingResult) {
            this.mContext = context;
            this.mIntent = intent;
            this.mPendingResult = pendingResult;
        }

        void setMediaBrowser(MediaBrowserCompat mediaBrowser) {
            this.mMediaBrowser = mediaBrowser;
        }

        @Override
        public void onConnected() {
            try {
                MediaControllerCompat mediaController = new MediaControllerCompat(this.mContext, this.mMediaBrowser.getSessionToken());
                KeyEvent ke = (KeyEvent) this.mIntent.getParcelableExtra("android.intent.extra.KEY_EVENT");
                mediaController.dispatchMediaButtonEvent(ke);
            } catch (RemoteException e) {
                Log.e("MediaButtonReceiver", "Failed to create a media controller", e);
            }
            finish();
        }

        @Override
        public void onConnectionSuspended() {
            finish();
        }

        @Override
        public void onConnectionFailed() {
            finish();
        }

        private void finish() {
            this.mMediaBrowser.disconnect();
            this.mPendingResult.finish();
        }
    }

    private static void startForegroundService(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private static ComponentName getServiceComponentByAction(Context context, String action) {
        PackageManager pm = context.getPackageManager();
        Intent queryIntent = new Intent(action);
        queryIntent.setPackage(context.getPackageName());
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(queryIntent, 0);
        if (resolveInfos.size() == 1) {
            ResolveInfo resolveInfo = resolveInfos.get(0);
            return new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
        }
        if (resolveInfos.isEmpty()) {
            return null;
        }
        throw new IllegalStateException("Expected 1 service that handles " + action + ", found " + resolveInfos.size());
    }
}
