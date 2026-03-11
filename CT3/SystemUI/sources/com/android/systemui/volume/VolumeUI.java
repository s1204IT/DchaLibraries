package com.android.systemui.volume;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.session.MediaSessionManager;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.qs.tiles.DndTile;
import com.android.systemui.statusbar.ServiceMonitor;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.statusbar.policy.ZenModeControllerImpl;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class VolumeUI extends SystemUI {
    private static boolean LOGD = Log.isLoggable("VolumeUI", 3);
    private AudioManager mAudioManager;
    private boolean mEnabled;
    private MediaSessionManager mMediaSessionManager;
    private NotificationManager mNotificationManager;
    private VolumeDialogComponent mVolumeComponent;
    private ServiceMonitor mVolumeControllerService;
    private final Handler mHandler = new Handler();
    private final Receiver mReceiver = new Receiver(this, null);
    private final RestorationNotification mRestorationNotification = new RestorationNotification(this, 0 == true ? 1 : 0);

    @Override
    public void start() {
        ServiceMonitorCallbacks serviceMonitorCallbacks = null;
        this.mEnabled = this.mContext.getResources().getBoolean(R.bool.enable_volume_ui);
        if (this.mEnabled) {
            this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
            this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
            this.mMediaSessionManager = (MediaSessionManager) this.mContext.getSystemService("media_session");
            ZenModeController zenController = new ZenModeControllerImpl(this.mContext, this.mHandler);
            this.mVolumeComponent = new VolumeDialogComponent(this, this.mContext, null, zenController);
            putComponent(VolumeComponent.class, getVolumeComponent());
            this.mReceiver.start();
            this.mVolumeControllerService = new ServiceMonitor("VolumeUI", LOGD, this.mContext, "volume_controller_service_component", new ServiceMonitorCallbacks(this, serviceMonitorCallbacks));
            this.mVolumeControllerService.start();
        }
    }

    public VolumeComponent getVolumeComponent() {
        return this.mVolumeComponent;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.mEnabled) {
            getVolumeComponent().onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.print("mEnabled=");
        pw.println(this.mEnabled);
        if (this.mEnabled) {
            pw.print("mVolumeControllerService=");
            pw.println(this.mVolumeControllerService.getComponent());
            getVolumeComponent().dump(fd, pw, args);
        }
    }

    public void setDefaultVolumeController(boolean register) {
        if (register) {
            DndTile.setVisible(this.mContext, true);
            if (LOGD) {
                Log.d("VolumeUI", "Registering default volume controller");
            }
            getVolumeComponent().register();
            return;
        }
        if (LOGD) {
            Log.d("VolumeUI", "Unregistering default volume controller");
        }
        this.mAudioManager.setVolumeController(null);
        this.mMediaSessionManager.setRemoteVolumeController(null);
    }

    public String getAppLabel(ComponentName component) {
        String rt;
        String pkg = component.getPackageName();
        try {
            ApplicationInfo ai = this.mContext.getPackageManager().getApplicationInfo(pkg, 0);
            rt = this.mContext.getPackageManager().getApplicationLabel(ai).toString();
        } catch (Exception e) {
            Log.w("VolumeUI", "Error loading app label", e);
        }
        if (!TextUtils.isEmpty(rt)) {
            return rt;
        }
        return pkg;
    }

    public void showServiceActivationDialog(final ComponentName component) {
        SystemUIDialog d = new SystemUIDialog(this.mContext);
        d.setMessage(this.mContext.getString(R.string.volumeui_prompt_message, getAppLabel(component)));
        d.setPositiveButton(R.string.volumeui_prompt_allow, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                VolumeUI.this.mVolumeControllerService.setComponent(component);
            }
        });
        d.setNegativeButton(R.string.volumeui_prompt_deny, null);
        d.show();
    }

    private final class ServiceMonitorCallbacks implements ServiceMonitor.Callbacks {
        ServiceMonitorCallbacks(VolumeUI this$0, ServiceMonitorCallbacks serviceMonitorCallbacks) {
            this();
        }

        private ServiceMonitorCallbacks() {
        }

        @Override
        public void onNoService() {
            if (VolumeUI.LOGD) {
                Log.d("VolumeUI", "onNoService");
            }
            VolumeUI.this.setDefaultVolumeController(true);
            VolumeUI.this.mRestorationNotification.hide();
            if (VolumeUI.this.mVolumeControllerService.isPackageAvailable()) {
                return;
            }
            VolumeUI.this.mVolumeControllerService.setComponent(null);
        }

        @Override
        public long onServiceStartAttempt() {
            if (VolumeUI.LOGD) {
                Log.d("VolumeUI", "onServiceStartAttempt");
            }
            VolumeUI.this.mVolumeControllerService.setComponent(VolumeUI.this.mVolumeControllerService.getComponent());
            VolumeUI.this.setDefaultVolumeController(false);
            VolumeUI.this.getVolumeComponent().dismissNow();
            VolumeUI.this.mRestorationNotification.show();
            return 0L;
        }
    }

    private final class Receiver extends BroadcastReceiver {
        Receiver(VolumeUI this$0, Receiver receiver) {
            this();
        }

        private Receiver() {
        }

        public void start() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.android.systemui.vui.ENABLE");
            filter.addAction("com.android.systemui.vui.DISABLE");
            filter.addAction("com.android.systemui.PREF");
            VolumeUI.this.mContext.registerReceiver(this, filter, null, VolumeUI.this.mHandler);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean zEquals;
            String action = intent.getAction();
            if ("com.android.systemui.PREF".equals(action)) {
                String key = intent.getStringExtra("key");
                if (key != null && intent.getExtras() != null) {
                    Object value = intent.getExtras().get("value");
                    if (value == null) {
                        Prefs.remove(VolumeUI.this.mContext, key);
                        return;
                    }
                    if (value instanceof Boolean) {
                        Prefs.putBoolean(VolumeUI.this.mContext, key, ((Boolean) value).booleanValue());
                        return;
                    } else if (value instanceof Integer) {
                        Prefs.putInt(VolumeUI.this.mContext, key, ((Integer) value).intValue());
                        return;
                    } else {
                        if (value instanceof Long) {
                            Prefs.putLong(VolumeUI.this.mContext, key, ((Long) value).longValue());
                            return;
                        }
                        return;
                    }
                }
                return;
            }
            ComponentName component = (ComponentName) intent.getParcelableExtra("component");
            if (component == null) {
                zEquals = false;
            } else {
                zEquals = component.equals(VolumeUI.this.mVolumeControllerService.getComponent());
            }
            if ("com.android.systemui.vui.ENABLE".equals(action) && component != null && !zEquals) {
                VolumeUI.this.showServiceActivationDialog(component);
            }
            if (!"com.android.systemui.vui.DISABLE".equals(action) || component == null || !zEquals) {
                return;
            }
            VolumeUI.this.mVolumeControllerService.setComponent(null);
        }
    }

    private final class RestorationNotification {
        RestorationNotification(VolumeUI this$0, RestorationNotification restorationNotification) {
            this();
        }

        private RestorationNotification() {
        }

        public void hide() {
            VolumeUI.this.mNotificationManager.cancel(R.id.notification_volumeui);
        }

        public void show() {
            ComponentName component = VolumeUI.this.mVolumeControllerService.getComponent();
            if (component == null) {
                Log.w("VolumeUI", "Not showing restoration notification, component not active");
                return;
            }
            Intent intent = new Intent("com.android.systemui.vui.DISABLE").putExtra("component", component);
            Notification.Builder builder = new Notification.Builder(VolumeUI.this.mContext).setSmallIcon(R.drawable.ic_volume_media).setWhen(0L).setShowWhen(false).setOngoing(true).setContentTitle(VolumeUI.this.mContext.getString(R.string.volumeui_notification_title, VolumeUI.this.getAppLabel(component))).setContentText(VolumeUI.this.mContext.getString(R.string.volumeui_notification_text)).setContentIntent(PendingIntent.getBroadcast(VolumeUI.this.mContext, 0, intent, 134217728)).setPriority(-2).setVisibility(1).setColor(VolumeUI.this.mContext.getColor(android.R.color.system_accent3_600));
            VolumeUI.overrideNotificationAppName(VolumeUI.this.mContext, builder);
            VolumeUI.this.mNotificationManager.notify(R.id.notification_volumeui, builder.build());
        }
    }
}
