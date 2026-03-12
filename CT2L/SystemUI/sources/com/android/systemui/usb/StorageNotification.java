package com.android.systemui.usb;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.UserHandle;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.app.ExternalMediaFormatActivity;
import com.android.systemui.SystemUI;

public class StorageNotification extends SystemUI {
    private Handler mAsyncEventHandler;
    private Notification mMediaStorageNotification;
    private StorageManager mStorageManager;
    private boolean mUmsAvailable;
    private Notification mUsbStorageNotification;

    private class StorageNotificationEventListener extends StorageEventListener {
        private StorageNotificationEventListener() {
        }

        public void onUsbMassStorageConnectionChanged(final boolean connected) {
            StorageNotification.this.mAsyncEventHandler.post(new Runnable() {
                @Override
                public void run() {
                    StorageNotification.this.onUsbMassStorageConnectionChangedAsync(connected);
                }
            });
        }

        public void onStorageStateChanged(final String path, final String oldState, final String newState) {
            StorageNotification.this.mAsyncEventHandler.post(new Runnable() {
                @Override
                public void run() {
                    StorageNotification.this.onStorageStateChangedAsync(path, oldState, newState);
                }
            });
        }
    }

    @Override
    public void start() {
        this.mStorageManager = (StorageManager) this.mContext.getSystemService("storage");
        boolean connected = this.mStorageManager.isUsbMassStorageConnected();
        HandlerThread thr = new HandlerThread("SystemUI StorageNotification");
        thr.start();
        this.mAsyncEventHandler = new Handler(thr.getLooper());
        StorageNotificationEventListener listener = new StorageNotificationEventListener();
        listener.onUsbMassStorageConnectionChanged(connected);
        this.mStorageManager.registerListener(listener);
    }

    public void onUsbMassStorageConnectionChangedAsync(boolean connected) {
        this.mUmsAvailable = connected;
        String st = Environment.getExternalStorageState();
        if (connected && (st.equals("removed") || st.equals("checking"))) {
            connected = false;
        }
        updateUsbMassStorageNotification(connected);
    }

    public void onStorageStateChangedAsync(String path, String oldState, String newState) {
        if (newState.equals("shared")) {
            Intent intent = new Intent();
            intent.setClass(this.mContext, UsbStorageActivity.class);
            PendingIntent pi = PendingIntent.getActivity(this.mContext, 0, intent, 0);
            setUsbStorageNotification(R.string.indeterminate_progress_30, R.string.indeterminate_progress_31, R.drawable.stat_sys_warning, false, true, pi);
            return;
        }
        if (newState.equals("checking")) {
            setMediaStorageNotification(R.string.input_method_ime_switch_button_desc, R.string.input_method_ime_switch_long_click_action_desc, R.drawable.stat_notify_sdcard_prepare, true, false, null);
            updateUsbMassStorageNotification(false);
            return;
        }
        if (newState.equals("mounted")) {
            setMediaStorageNotification(0, 0, 0, false, false, null);
            updateUsbMassStorageNotification(this.mUmsAvailable);
            return;
        }
        if (newState.equals("unmounted")) {
            if (!this.mStorageManager.isUsbMassStorageEnabled()) {
                if (oldState.equals("shared")) {
                    setMediaStorageNotification(0, 0, 0, false, false, null);
                    updateUsbMassStorageNotification(this.mUmsAvailable);
                    return;
                } else {
                    if (Environment.isExternalStorageRemovable()) {
                        setMediaStorageNotification(R.string.install_carrier_app_notification_title, R.string.invalidPin, R.drawable.stat_notify_sdcard, true, true, null);
                    } else {
                        setMediaStorageNotification(0, 0, 0, false, false, null);
                    }
                    updateUsbMassStorageNotification(this.mUmsAvailable);
                    return;
                }
            }
            setMediaStorageNotification(0, 0, 0, false, false, null);
            updateUsbMassStorageNotification(false);
            return;
        }
        if (newState.equals("nofs")) {
            Intent intent2 = new Intent();
            intent2.setClass(this.mContext, ExternalMediaFormatActivity.class);
            StorageVolume[] vl = this.mStorageManager.getVolumeList();
            for (StorageVolume sv : vl) {
                if (sv.getPath().equals(path)) {
                    intent2.putExtra("storage_volume", sv);
                }
            }
            PendingIntent pi2 = PendingIntent.getActivity(this.mContext, 0, intent2, 0);
            setMediaStorageNotification(R.string.input_method_language_settings, R.string.input_method_nav_back_button_desc, R.drawable.stat_notify_sdcard_usb, true, false, pi2);
            updateUsbMassStorageNotification(this.mUmsAvailable);
            return;
        }
        if (newState.equals("unmountable")) {
            Intent intent3 = new Intent();
            intent3.setClass(this.mContext, ExternalMediaFormatActivity.class);
            StorageVolume[] vl2 = this.mStorageManager.getVolumeList();
            for (StorageVolume sv2 : vl2) {
                if (sv2.getPath().equals(path)) {
                    intent3.putExtra("storage_volume", sv2);
                }
            }
            PendingIntent pi3 = PendingIntent.getActivity(this.mContext, 0, intent3, 0);
            setMediaStorageNotification(R.string.input_method_switcher_settings_button, R.string.install_carrier_app_notification_button, R.drawable.stat_notify_sdcard_usb, true, false, pi3);
            updateUsbMassStorageNotification(this.mUmsAvailable);
            return;
        }
        if (newState.equals("removed")) {
            setMediaStorageNotification(R.string.invalidPuk, R.string.issued_by, R.drawable.stat_notify_sdcard_usb, true, true, null);
            updateUsbMassStorageNotification(false);
        } else if (newState.equals("bad_removal")) {
            setMediaStorageNotification(R.string.install_carrier_app_notification_text, R.string.install_carrier_app_notification_text_app_name, R.drawable.stat_sys_warning, true, true, null);
            updateUsbMassStorageNotification(false);
        } else {
            Log.w("StorageNotification", String.format("Ignoring unknown state {%s}", newState));
        }
    }

    void updateUsbMassStorageNotification(boolean available) {
        if (available) {
            Intent intent = new Intent();
            intent.setClass(this.mContext, UsbStorageActivity.class);
            intent.setFlags(268435456);
            PendingIntent pi = PendingIntent.getActivity(this.mContext, 0, intent, 0);
            setUsbStorageNotification(R.string.indeterminate_progress_28, R.string.indeterminate_progress_29, R.drawable.jog_tab_bar_right_end_confirm_yellow, false, true, pi);
            return;
        }
        setUsbStorageNotification(0, 0, 0, false, false, null);
    }

    private synchronized void setUsbStorageNotification(int titleId, int messageId, int icon, boolean sound, boolean visible, PendingIntent pi) {
        if (!visible) {
            if (this.mUsbStorageNotification != null) {
                NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
                if (notificationManager != null) {
                    if (visible) {
                        Resources r = Resources.getSystem();
                        CharSequence title = r.getText(titleId);
                        CharSequence message = r.getText(messageId);
                        if (this.mUsbStorageNotification == null) {
                            this.mUsbStorageNotification = new Notification();
                            this.mUsbStorageNotification.icon = icon;
                            this.mUsbStorageNotification.when = 0L;
                        }
                        if (sound) {
                            this.mUsbStorageNotification.defaults |= 1;
                        } else {
                            this.mUsbStorageNotification.defaults &= -2;
                        }
                        this.mUsbStorageNotification.flags = 2;
                        this.mUsbStorageNotification.tickerText = title;
                        if (pi == null) {
                            Intent intent = new Intent();
                            pi = PendingIntent.getBroadcastAsUser(this.mContext, 0, intent, 0, UserHandle.CURRENT);
                        }
                        this.mUsbStorageNotification.color = this.mContext.getResources().getColor(R.color.system_accent3_600);
                        this.mUsbStorageNotification.setLatestEventInfo(this.mContext, title, message, pi);
                        this.mUsbStorageNotification.visibility = 1;
                        this.mUsbStorageNotification.category = "sys";
                        boolean adbOn = 1 == Settings.Global.getInt(this.mContext.getContentResolver(), "adb_enabled", 0);
                        if (!adbOn) {
                            this.mUsbStorageNotification.fullScreenIntent = pi;
                        }
                    }
                    int notificationId = this.mUsbStorageNotification.icon;
                    if (visible) {
                        notificationManager.notifyAsUser(null, notificationId, this.mUsbStorageNotification, UserHandle.ALL);
                    } else {
                        notificationManager.cancelAsUser(null, notificationId, UserHandle.ALL);
                    }
                }
            }
        }
    }

    private synchronized void setMediaStorageNotification(int titleId, int messageId, int icon, boolean visible, boolean dismissable, PendingIntent pi) {
        if (!visible) {
            if (this.mMediaStorageNotification != null) {
                NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
                if (notificationManager != null) {
                    if (this.mMediaStorageNotification != null && visible) {
                        notificationManager.cancel(this.mMediaStorageNotification.icon);
                    }
                    if (visible) {
                        Resources r = Resources.getSystem();
                        CharSequence title = r.getText(titleId);
                        CharSequence message = r.getText(messageId);
                        if (this.mMediaStorageNotification == null) {
                            this.mMediaStorageNotification = new Notification();
                            this.mMediaStorageNotification.when = 0L;
                        }
                        this.mMediaStorageNotification.defaults &= -2;
                        if (dismissable) {
                            this.mMediaStorageNotification.flags = 16;
                        } else {
                            this.mMediaStorageNotification.flags = 2;
                        }
                        this.mMediaStorageNotification.tickerText = title;
                        if (pi == null) {
                            Intent intent = new Intent();
                            pi = PendingIntent.getBroadcastAsUser(this.mContext, 0, intent, 0, UserHandle.CURRENT);
                        }
                        this.mMediaStorageNotification.icon = icon;
                        this.mMediaStorageNotification.color = this.mContext.getResources().getColor(R.color.system_accent3_600);
                        this.mMediaStorageNotification.setLatestEventInfo(this.mContext, title, message, pi);
                        this.mMediaStorageNotification.visibility = 1;
                        this.mMediaStorageNotification.category = "sys";
                    }
                    int notificationId = this.mMediaStorageNotification.icon;
                    if (visible) {
                        notificationManager.notifyAsUser(null, notificationId, this.mMediaStorageNotification, UserHandle.ALL);
                    } else {
                        notificationManager.cancelAsUser(null, notificationId, UserHandle.ALL);
                    }
                }
            }
        }
    }
}
