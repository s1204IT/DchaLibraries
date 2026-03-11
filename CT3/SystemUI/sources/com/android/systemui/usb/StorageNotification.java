package com.android.systemui.usb;

import android.R;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.systemui.SystemUI;
import java.util.List;

public class StorageNotification extends SystemUI {
    private NotificationManager mNotificationManager;
    private StorageManager mStorageManager;
    private Notification mUsbStorageNotification;
    private boolean mIsUmsConnect = false;
    private int mNotifcationState = 0;
    private boolean mIsLastVisible = false;
    private final SparseArray<MoveInfo> mMoves = new SparseArray<>();
    private final StorageEventListener mListener = new StorageEventListener() {
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            StorageNotification.this.onVolumeStateChangedInternal(vol);
        }

        public void onVolumeRecordChanged(VolumeRecord rec) {
            VolumeInfo vol = StorageNotification.this.mStorageManager.findVolumeByUuid(rec.getFsUuid());
            if (vol == null || !vol.isMountedReadable()) {
                return;
            }
            StorageNotification.this.onVolumeStateChangedInternal(vol);
        }

        public void onVolumeForgotten(String fsUuid) {
            StorageNotification.this.mNotificationManager.cancelAsUser(fsUuid, 1397772886, UserHandle.ALL);
        }

        public void onDiskScanned(DiskInfo disk, int volumeCount) {
            StorageNotification.this.onDiskScannedInternal(disk, volumeCount);
        }

        public void onDiskDestroyed(DiskInfo disk) {
            StorageNotification.this.onDiskDestroyedInternal(disk);
        }
    };
    private final BroadcastReceiver mSnoozeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String fsUuid = intent.getStringExtra("android.os.storage.extra.FS_UUID");
            StorageNotification.this.mStorageManager.setVolumeSnoozed(fsUuid, true);
        }
    };
    private final BroadcastReceiver mFinishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            StorageNotification.this.mNotificationManager.cancelAsUser(null, 1397575510, UserHandle.ALL);
        }
    };
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean booleanExtra;
            if (!intent.getBooleanExtra("configured", false)) {
                booleanExtra = false;
            } else {
                booleanExtra = intent.getBooleanExtra("mass_storage", false);
            }
            Log.i("StorageNotification", "onReceive=" + intent.getAction() + ",available=" + booleanExtra);
            StorageNotification.this.onUsbMassStorageConnectionChangedAsync(booleanExtra);
        }
    };
    private final BroadcastReceiver mUserReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!"android.intent.action.USER_SWITCHED".equals(action)) {
                return;
            }
            StorageNotification.this.updateUsbMassStorageNotification();
        }
    };
    private final PackageManager.MoveCallback mMoveCallback = new PackageManager.MoveCallback() {
        public void onCreated(int moveId, Bundle extras) {
            MoveInfo move = new MoveInfo(null);
            move.moveId = moveId;
            move.extras = extras;
            if (extras != null) {
                move.packageName = extras.getString("android.intent.extra.PACKAGE_NAME");
                move.label = extras.getString("android.intent.extra.TITLE");
                move.volumeUuid = extras.getString("android.os.storage.extra.FS_UUID");
            }
            StorageNotification.this.mMoves.put(moveId, move);
        }

        public void onStatusChanged(int moveId, int status, long estMillis) {
            MoveInfo move = (MoveInfo) StorageNotification.this.mMoves.get(moveId);
            if (move == null) {
                Log.w("StorageNotification", "Ignoring unknown move " + moveId);
            } else if (PackageManager.isMoveStatusFinished(status)) {
                StorageNotification.this.onMoveFinished(move, status);
            } else {
                StorageNotification.this.onMoveProgress(move, status, estMillis);
            }
        }
    };

    private static class MoveInfo {
        public Bundle extras;
        public String label;
        public int moveId;
        public String packageName;
        public String volumeUuid;

        MoveInfo(MoveInfo moveInfo) {
            this();
        }

        private MoveInfo() {
        }
    }

    @Override
    public void start() {
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        this.mStorageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        this.mStorageManager.registerListener(this.mListener);
        this.mContext.registerReceiver(this.mSnoozeReceiver, new IntentFilter("com.android.systemui.action.SNOOZE_VOLUME"), "android.permission.MOUNT_UNMOUNT_FILESYSTEMS", null);
        this.mContext.registerReceiver(this.mFinishReceiver, new IntentFilter("com.android.systemui.action.FINISH_WIZARD"), "android.permission.MOUNT_UNMOUNT_FILESYSTEMS", null);
        this.mContext.registerReceiver(this.mUsbReceiver, new IntentFilter("android.hardware.usb.action.USB_STATE"));
        this.mContext.registerReceiver(this.mUserReceiver, new IntentFilter("android.intent.action.USER_SWITCHED"));
        List<DiskInfo> disks = this.mStorageManager.getDisks();
        for (DiskInfo disk : disks) {
            onDiskScannedInternal(disk, disk.volumeCount);
        }
        List<VolumeInfo> vols = this.mStorageManager.getVolumes();
        for (VolumeInfo vol : vols) {
            onVolumeStateChangedInternal(vol);
        }
        this.mContext.getPackageManager().registerMoveCallback(this.mMoveCallback, new Handler());
        updateMissingPrivateVolumes();
    }

    private int sharableStorageNum() {
        int num = 0;
        List<VolumeInfo> infos = this.mStorageManager.getVolumes();
        for (VolumeInfo info : infos) {
            if (info != null && info.isAllowUsbMassStorage(ActivityManager.getCurrentUser()) && info.getType() == 0 && (info.getState() != 6 || info.getState() != 7 || info.getState() != 8 || info.getState() != 4)) {
                num++;
            }
        }
        return num;
    }

    private int sharedStorageNum() {
        int num = 0;
        List<VolumeInfo> infos = this.mStorageManager.getVolumes();
        for (VolumeInfo info : infos) {
            if (info != null && info.getState() == 9 && info.getType() == 0) {
                num++;
            }
        }
        return num;
    }

    public void onUsbMassStorageConnectionChangedAsync(boolean connected) {
        this.mIsUmsConnect = connected;
        updateUsbMassStorageNotification();
    }

    void updateUsbMassStorageNotification() {
        int canSharedNum = sharableStorageNum();
        int sharedNum = sharedStorageNum();
        Log.d("StorageNotification", "updateUsbMassStorageNotification - canSharedNum=" + canSharedNum + ",sharedNum=" + sharedNum + ",mIsUmsConnect=" + this.mIsUmsConnect + ",mNotifcationState=" + this.mNotifcationState);
        if (this.mIsUmsConnect && canSharedNum > 0 && this.mNotifcationState != 1) {
            Log.d("StorageNotification", "updateUsbMassStorageNotification - Turn on noti.");
            this.mNotifcationState = 1;
            return;
        }
        if (this.mIsUmsConnect && sharedNum > 0 && this.mNotifcationState != 2) {
            Log.d("StorageNotification", "updateUsbMassStorageNotification - Turn off noti.");
            this.mNotifcationState = 2;
        } else {
            if ((this.mIsUmsConnect && canSharedNum != 0) || this.mNotifcationState == 0) {
                Log.d("StorageNotification", "updateUsbMassStorageNotification - What?");
                return;
            }
            Log.d("StorageNotification", "updateUsbMassStorageNotification - Cancel noti.");
            setUsbStorageNotification(0, 0, 0, false, false, null);
            this.mNotifcationState = 0;
        }
    }

    private synchronized void setUsbStorageNotification(int titleId, int messageId, int icon, boolean sound, boolean visible, PendingIntent pi) {
        Log.d("StorageNotification", "setUsbStorageNotification visible=" + visible + ",mIsLastVisible=" + this.mIsLastVisible);
        if (!visible && this.mUsbStorageNotification == null) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        if (notificationManager == null) {
            return;
        }
        if (visible) {
            Resources r = Resources.getSystem();
            CharSequence title = r.getText(titleId);
            CharSequence message = r.getText(messageId);
            if (this.mUsbStorageNotification == null) {
                this.mUsbStorageNotification = new Notification();
                this.mUsbStorageNotification.icon = icon;
                this.mUsbStorageNotification.when = 0L;
                this.mUsbStorageNotification.priority = -2;
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
        }
        int notificationId = this.mUsbStorageNotification.icon;
        if (visible) {
            notificationManager.notifyAsUser(null, notificationId, this.mUsbStorageNotification, UserHandle.ALL);
            this.mIsLastVisible = true;
        } else {
            notificationManager.cancelAsUser(null, notificationId, UserHandle.ALL);
            this.mIsLastVisible = false;
        }
    }

    private void updateMissingPrivateVolumes() {
        List<VolumeRecord> recs = this.mStorageManager.getVolumeRecords();
        for (VolumeRecord rec : recs) {
            if (rec.getType() == 1) {
                String fsUuid = rec.getFsUuid();
                VolumeInfo info = this.mStorageManager.findVolumeByUuid(fsUuid);
                if ((info == null || !info.isMountedWritable()) && !rec.isSnoozed()) {
                    CharSequence title = this.mContext.getString(R.string.fingerprint_dangling_notification_msg_2, rec.getNickname());
                    CharSequence text = this.mContext.getString(R.string.fingerprint_dangling_notification_msg_all_deleted_1);
                    Notification.Builder builder = new Notification.Builder(this.mContext).setSmallIcon(R.drawable.ic_doc_codes).setColor(this.mContext.getColor(R.color.system_accent3_600)).setContentTitle(title).setContentText(text).setContentIntent(buildForgetPendingIntent(rec)).setStyle(new Notification.BigTextStyle().bigText(text)).setVisibility(1).setLocalOnly(true).setCategory("sys").setDeleteIntent(buildSnoozeIntent(fsUuid));
                    SystemUI.overrideNotificationAppName(this.mContext, builder);
                    this.mNotificationManager.notifyAsUser(fsUuid, 1397772886, builder.build(), UserHandle.ALL);
                } else {
                    this.mNotificationManager.cancelAsUser(fsUuid, 1397772886, UserHandle.ALL);
                }
            }
        }
    }

    public void onDiskScannedInternal(DiskInfo disk, int volumeCount) {
        if (volumeCount != 0 || disk.size <= 0) {
            this.mNotificationManager.cancelAsUser(disk.getId(), 1396986699, UserHandle.ALL);
            return;
        }
        CharSequence title = this.mContext.getString(R.string.fingerprint_acquired_immobile, disk.getDescription());
        CharSequence text = this.mContext.getString(R.string.fingerprint_acquired_insufficient, disk.getDescription());
        Notification.Builder builder = new Notification.Builder(this.mContext).setSmallIcon(getSmallIcon(disk, 6)).setColor(this.mContext.getColor(R.color.system_accent3_600)).setContentTitle(title).setContentText(text).setContentIntent(buildInitPendingIntent(disk)).setStyle(new Notification.BigTextStyle().bigText(text)).setVisibility(1).setLocalOnly(true).setCategory("err");
        SystemUI.overrideNotificationAppName(this.mContext, builder);
        this.mNotificationManager.notifyAsUser(disk.getId(), 1396986699, builder.build(), UserHandle.ALL);
    }

    public void onDiskDestroyedInternal(DiskInfo disk) {
        this.mNotificationManager.cancelAsUser(disk.getId(), 1396986699, UserHandle.ALL);
    }

    public void onVolumeStateChangedInternal(VolumeInfo vol) {
        switch (vol.getType()) {
            case 0:
                onPublicVolumeStateChangedInternal(vol);
                break;
            case 1:
                onPrivateVolumeStateChangedInternal(vol);
                break;
        }
    }

    private void onPrivateVolumeStateChangedInternal(VolumeInfo vol) {
        Log.d("StorageNotification", "Notifying about private volume: " + vol.toString());
        updateMissingPrivateVolumes();
    }

    private void onPublicVolumeStateChangedInternal(VolumeInfo vol) {
        Notification notif;
        Log.d("StorageNotification", "Notifying about public volume: " + vol.toString());
        switch (vol.getState()) {
            case 0:
                notif = onVolumeUnmounted(vol);
                break;
            case 1:
                notif = onVolumeChecking(vol);
                break;
            case 2:
            case 3:
                notif = onVolumeMounted(vol);
                break;
            case 4:
                notif = onVolumeFormatting(vol);
                break;
            case 5:
                notif = onVolumeEjecting(vol);
                break;
            case 6:
                notif = onVolumeUnmountable(vol);
                break;
            case 7:
                notif = onVolumeRemoved(vol);
                break;
            case 8:
                notif = onVolumeBadRemoval(vol);
                break;
            case 9:
                notif = null;
                break;
            default:
                notif = null;
                break;
        }
        updateUsbMassStorageNotification();
        if (notif != null) {
            this.mNotificationManager.notifyAsUser(vol.getId(), 1397773634, notif, UserHandle.ALL);
        } else {
            this.mNotificationManager.cancelAsUser(vol.getId(), 1397773634, UserHandle.ALL);
        }
    }

    private Notification onVolumeUnmounted(VolumeInfo vol) {
        return null;
    }

    private Notification onVolumeChecking(VolumeInfo vol) {
        DiskInfo disk = vol.getDisk();
        CharSequence title = this.mContext.getString(R.string.find_next, disk.getDescription());
        CharSequence text = this.mContext.getString(R.string.find_on_page, disk.getDescription());
        return buildNotificationBuilder(vol, title, text).setCategory("progress").setPriority(-1).setOngoing(true).build();
    }

    private Notification onVolumeMounted(VolumeInfo vol) {
        VolumeRecord rec = this.mStorageManager.findRecordByUuid(vol.getFsUuid());
        DiskInfo disk = vol.getDisk();
        if (rec == null) {
            return null;
        }
        if (rec.isSnoozed() && disk.isAdoptable()) {
            return null;
        }
        if (disk.isAdoptable() && !rec.isInited()) {
            CharSequence title = disk.getDescription();
            CharSequence text = this.mContext.getString(R.string.find_previous, disk.getDescription());
            buildInitPendingIntent(vol);
            return buildNotificationBuilder(vol, title, text).addAction(new Notification.Action(R.drawable.edit_query_background_normal, this.mContext.getString(R.string.fingerprint_authenticated), buildUnmountPendingIntent(vol))).setDeleteIntent(buildSnoozeIntent(vol.getFsUuid())).setCategory("sys").build();
        }
        CharSequence title2 = disk.getDescription();
        CharSequence text2 = this.mContext.getString(R.string.fingerprint_acquired_already_enrolled, disk.getDescription());
        buildBrowsePendingIntent(vol);
        Notification.Builder builder = buildNotificationBuilder(vol, title2, text2).addAction(new Notification.Action(R.drawable.edit_query_background_normal, this.mContext.getString(R.string.fingerprint_authenticated), buildUnmountPendingIntent(vol))).setCategory("sys").setPriority(-1);
        if (disk.isAdoptable()) {
            builder.setDeleteIntent(buildSnoozeIntent(vol.getFsUuid()));
        }
        return builder.build();
    }

    private Notification onVolumeFormatting(VolumeInfo vol) {
        return null;
    }

    private Notification onVolumeEjecting(VolumeInfo vol) {
        DiskInfo disk = vol.getDisk();
        CharSequence title = this.mContext.getString(R.string.fingerprint_acquired_too_slow, disk.getDescription());
        CharSequence text = this.mContext.getString(R.string.fingerprint_acquired_try_adjusting, disk.getDescription());
        return buildNotificationBuilder(vol, title, text).setCategory("progress").setPriority(-1).setOngoing(true).build();
    }

    private Notification onVolumeUnmountable(VolumeInfo vol) {
        DiskInfo disk = vol.getDisk();
        CharSequence title = this.mContext.getString(R.string.fingerprint_acquired_imager_dirty, disk.getDescription());
        CharSequence text = this.mContext.getString(R.string.fingerprint_acquired_imager_dirty_alt, disk.getDescription());
        return buildNotificationBuilder(vol, title, text).setContentIntent(buildInitPendingIntent(vol)).setCategory("err").build();
    }

    private Notification onVolumeRemoved(VolumeInfo vol) {
        if (!vol.isPrimary()) {
            return null;
        }
        DiskInfo disk = vol.getDisk();
        CharSequence title = this.mContext.getString(R.string.fingerprint_acquired_too_bright, disk.getDescription());
        CharSequence text = this.mContext.getString(R.string.fingerprint_acquired_too_fast, disk.getDescription());
        return buildNotificationBuilder(vol, title, text).setCategory("err").build();
    }

    private Notification onVolumeBadRemoval(VolumeInfo vol) {
        if (!vol.isPrimary()) {
            return null;
        }
        DiskInfo disk = vol.getDisk();
        CharSequence title = this.mContext.getString(R.string.fingerprint_acquired_partial, disk.getDescription());
        CharSequence text = this.mContext.getString(R.string.fingerprint_acquired_power_press, disk.getDescription());
        return buildNotificationBuilder(vol, title, text).setCategory("err").build();
    }

    public void onMoveProgress(MoveInfo move, int status, long estMillis) {
        CharSequence title = !TextUtils.isEmpty(move.label) ? this.mContext.getString(R.string.fingerprint_dangling_notification_msg_all_deleted_2, move.label) : this.mContext.getString(R.string.fingerprint_dangling_notification_title);
        CharSequence duration = estMillis < 0 ? null : DateUtils.formatDuration(estMillis);
        PendingIntent intent = move.packageName != null ? buildWizardMovePendingIntent(move) : buildWizardMigratePendingIntent(move);
        if (intent == null) {
            return;
        }
        Notification.Builder builder = new Notification.Builder(this.mContext).setSmallIcon(R.drawable.ic_doc_codes).setColor(this.mContext.getColor(R.color.system_accent3_600)).setContentTitle(title).setContentText(duration).setContentIntent(intent).setStyle(new Notification.BigTextStyle().bigText(duration)).setVisibility(1).setLocalOnly(true).setCategory("progress").setPriority(-1).setProgress(100, status, false).setOngoing(true);
        SystemUI.overrideNotificationAppName(this.mContext, builder);
        this.mNotificationManager.notifyAsUser(move.packageName, 1397575510, builder.build(), UserHandle.ALL);
    }

    public void onMoveFinished(MoveInfo move, int status) {
        CharSequence title;
        CharSequence text;
        if (move.packageName != null) {
            this.mNotificationManager.cancelAsUser(move.packageName, 1397575510, UserHandle.ALL);
            return;
        }
        VolumeInfo privateVol = this.mContext.getPackageManager().getPrimaryStorageCurrentVolume();
        String descrip = this.mStorageManager.getBestVolumeDescription(privateVol);
        if (status == -100) {
            title = this.mContext.getString(R.string.fingerprint_dialog_default_subtitle);
            text = this.mContext.getString(R.string.fingerprint_dialog_use_fingerprint_instead, descrip);
        } else {
            title = this.mContext.getString(R.string.fingerprint_error_bad_calibration);
            text = this.mContext.getString(R.string.fingerprint_error_canceled);
        }
        PendingIntent intent = (privateVol == null || privateVol.getDisk() == null) ? privateVol != null ? buildVolumeSettingsPendingIntent(privateVol) : null : buildWizardReadyPendingIntent(privateVol.getDisk());
        Notification.Builder builder = new Notification.Builder(this.mContext).setSmallIcon(R.drawable.ic_doc_codes).setColor(this.mContext.getColor(R.color.system_accent3_600)).setContentTitle(title).setContentText(text).setContentIntent(intent).setStyle(new Notification.BigTextStyle().bigText(text)).setVisibility(1).setLocalOnly(true).setCategory("sys").setPriority(-1).setAutoCancel(true);
        SystemUI.overrideNotificationAppName(this.mContext, builder);
        this.mNotificationManager.notifyAsUser(move.packageName, 1397575510, builder.build(), UserHandle.ALL);
    }

    private int getSmallIcon(DiskInfo disk, int state) {
        if (!disk.isSd()) {
            return disk.isUsb() ? R.drawable.ic_expand_more : R.drawable.ic_doc_codes;
        }
        switch (state) {
            case 1:
            case 5:
                break;
        }
        return R.drawable.ic_doc_codes;
    }

    private Notification.Builder buildNotificationBuilder(VolumeInfo vol, CharSequence title, CharSequence text) {
        Notification.Builder builder = new Notification.Builder(this.mContext).setSmallIcon(getSmallIcon(vol.getDisk(), vol.getState())).setColor(this.mContext.getColor(R.color.system_accent3_600)).setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setVisibility(1).setLocalOnly(true);
        overrideNotificationAppName(this.mContext, builder);
        return builder;
    }

    private PendingIntent buildInitPendingIntent(DiskInfo disk) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardInit");
        intent.putExtra("android.os.storage.extra.DISK_ID", disk.getId());
        int requestKey = disk.getId().hashCode();
        return PendingIntent.getActivityAsUser(this.mContext, requestKey, intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildInitPendingIntent(VolumeInfo vol) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardInit");
        intent.putExtra("android.os.storage.extra.VOLUME_ID", vol.getId());
        int requestKey = vol.getId().hashCode();
        return PendingIntent.getActivityAsUser(this.mContext, requestKey, intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildUnmountPendingIntent(VolumeInfo vol) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageUnmountReceiver");
        intent.putExtra("android.os.storage.extra.VOLUME_ID", vol.getId());
        int requestKey = vol.getId().hashCode();
        return PendingIntent.getBroadcastAsUser(this.mContext, requestKey, intent, 268435456, UserHandle.CURRENT);
    }

    private PendingIntent buildBrowsePendingIntent(VolumeInfo vol) {
        Intent intent = vol.buildBrowseIntent();
        int requestKey = vol.getId().hashCode();
        return PendingIntent.getActivityAsUser(this.mContext, requestKey, intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildVolumeSettingsPendingIntent(VolumeInfo vol) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent();
        switch (vol.getType()) {
            case 0:
                intent.setClassName("com.android.settings", "com.android.settings.Settings$PublicVolumeSettingsActivity");
                break;
            case 1:
                intent.setClassName("com.android.settings", "com.android.settings.Settings$PrivateVolumeSettingsActivity");
                break;
            default:
                return null;
        }
        intent.putExtra("android.os.storage.extra.VOLUME_ID", vol.getId());
        int requestKey = vol.getId().hashCode();
        return PendingIntent.getActivityAsUser(this.mContext, requestKey, intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildSnoozeIntent(String fsUuid) {
        Intent intent = new Intent("com.android.systemui.action.SNOOZE_VOLUME");
        intent.putExtra("android.os.storage.extra.FS_UUID", fsUuid);
        int requestKey = fsUuid.hashCode();
        return PendingIntent.getBroadcastAsUser(this.mContext, requestKey, intent, 268435456, UserHandle.CURRENT);
    }

    private PendingIntent buildForgetPendingIntent(VolumeRecord rec) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.Settings$PrivateVolumeForgetActivity");
        intent.putExtra("android.os.storage.extra.FS_UUID", rec.getFsUuid());
        int requestKey = rec.getFsUuid().hashCode();
        return PendingIntent.getActivityAsUser(this.mContext, requestKey, intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildWizardMigratePendingIntent(MoveInfo move) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardMigrateProgress");
        intent.putExtra("android.content.pm.extra.MOVE_ID", move.moveId);
        VolumeInfo vol = this.mStorageManager.findVolumeByQualifiedUuid(move.volumeUuid);
        if (vol != null) {
            intent.putExtra("android.os.storage.extra.VOLUME_ID", vol.getId());
        }
        return PendingIntent.getActivityAsUser(this.mContext, move.moveId, intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildWizardMovePendingIntent(MoveInfo move) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardMoveProgress");
        intent.putExtra("android.content.pm.extra.MOVE_ID", move.moveId);
        return PendingIntent.getActivityAsUser(this.mContext, move.moveId, intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildWizardReadyPendingIntent(DiskInfo disk) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardReady");
        intent.putExtra("android.os.storage.extra.DISK_ID", disk.getId());
        int requestKey = disk.getId().hashCode();
        return PendingIntent.getActivityAsUser(this.mContext, requestKey, intent, 268435456, null, UserHandle.CURRENT);
    }
}
