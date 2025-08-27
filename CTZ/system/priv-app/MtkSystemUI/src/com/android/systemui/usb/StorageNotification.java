package com.android.systemui.usb;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import com.android.systemui.util.NotificationChannels;
import java.util.Iterator;

/* loaded from: classes.dex */
public class StorageNotification extends SystemUI {
    private NotificationManager mNotificationManager;
    private StorageManager mStorageManager;
    private final SparseArray<MoveInfo> mMoves = new SparseArray<>();
    private final StorageEventListener mListener = new StorageEventListener() { // from class: com.android.systemui.usb.StorageNotification.1
        public void onVolumeStateChanged(VolumeInfo volumeInfo, int i, int i2) {
            StorageNotification.this.onVolumeStateChangedInternal(volumeInfo);
        }

        public void onVolumeRecordChanged(VolumeRecord volumeRecord) {
            VolumeInfo volumeInfoFindVolumeByUuid = StorageNotification.this.mStorageManager.findVolumeByUuid(volumeRecord.getFsUuid());
            if (volumeInfoFindVolumeByUuid != null && volumeInfoFindVolumeByUuid.isMountedReadable()) {
                StorageNotification.this.onVolumeStateChangedInternal(volumeInfoFindVolumeByUuid);
            }
        }

        public void onVolumeForgotten(String str) {
            StorageNotification.this.mNotificationManager.cancelAsUser(str, 1397772886, UserHandle.ALL);
        }

        public void onDiskScanned(DiskInfo diskInfo, int i) {
            StorageNotification.this.onDiskScannedInternal(diskInfo, i);
        }

        public void onDiskDestroyed(DiskInfo diskInfo) {
            StorageNotification.this.onDiskDestroyedInternal(diskInfo);
        }
    };
    private final BroadcastReceiver mSnoozeReceiver = new BroadcastReceiver() { // from class: com.android.systemui.usb.StorageNotification.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            StorageNotification.this.mStorageManager.setVolumeSnoozed(intent.getStringExtra("android.os.storage.extra.FS_UUID"), true);
        }
    };
    private final BroadcastReceiver mFinishReceiver = new BroadcastReceiver() { // from class: com.android.systemui.usb.StorageNotification.3
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            StorageNotification.this.mNotificationManager.cancelAsUser(null, 1397575510, UserHandle.ALL);
        }
    };
    private final PackageManager.MoveCallback mMoveCallback = new PackageManager.MoveCallback() { // from class: com.android.systemui.usb.StorageNotification.4
        public void onCreated(int i, Bundle bundle) {
            MoveInfo moveInfo = new MoveInfo();
            moveInfo.moveId = i;
            moveInfo.extras = bundle;
            if (bundle != null) {
                moveInfo.packageName = bundle.getString("android.intent.extra.PACKAGE_NAME");
                moveInfo.label = bundle.getString("android.intent.extra.TITLE");
                moveInfo.volumeUuid = bundle.getString("android.os.storage.extra.FS_UUID");
            }
            StorageNotification.this.mMoves.put(i, moveInfo);
        }

        public void onStatusChanged(int i, int i2, long j) {
            MoveInfo moveInfo = (MoveInfo) StorageNotification.this.mMoves.get(i);
            if (moveInfo == null) {
                Log.w("StorageNotification", "Ignoring unknown move " + i);
                return;
            }
            if (PackageManager.isMoveStatusFinished(i2)) {
                StorageNotification.this.onMoveFinished(moveInfo, i2);
            } else {
                StorageNotification.this.onMoveProgress(moveInfo, i2, j);
            }
        }
    };

    private static class MoveInfo {
        public Bundle extras;
        public String label;
        public int moveId;
        public String packageName;
        public String volumeUuid;

        private MoveInfo() {
        }
    }

    @Override // com.android.systemui.SystemUI
    public void start() {
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        this.mStorageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        this.mStorageManager.registerListener(this.mListener);
        this.mContext.registerReceiver(this.mSnoozeReceiver, new IntentFilter("com.android.systemui.action.SNOOZE_VOLUME"), "android.permission.MOUNT_UNMOUNT_FILESYSTEMS", null);
        this.mContext.registerReceiver(this.mFinishReceiver, new IntentFilter("com.android.systemui.action.FINISH_WIZARD"), "android.permission.MOUNT_UNMOUNT_FILESYSTEMS", null);
        for (DiskInfo diskInfo : this.mStorageManager.getDisks()) {
            onDiskScannedInternal(diskInfo, diskInfo.volumeCount);
        }
        Iterator it = this.mStorageManager.getVolumes().iterator();
        while (it.hasNext()) {
            onVolumeStateChangedInternal((VolumeInfo) it.next());
        }
        this.mContext.getPackageManager().registerMoveCallback(this.mMoveCallback, new Handler());
        updateMissingPrivateVolumes();
    }

    private void updateMissingPrivateVolumes() {
        if (isTv()) {
            return;
        }
        for (VolumeRecord volumeRecord : this.mStorageManager.getVolumeRecords()) {
            if (volumeRecord.getType() == 1) {
                String fsUuid = volumeRecord.getFsUuid();
                VolumeInfo volumeInfoFindVolumeByUuid = this.mStorageManager.findVolumeByUuid(fsUuid);
                if ((volumeInfoFindVolumeByUuid != null && volumeInfoFindVolumeByUuid.isMountedWritable()) || volumeRecord.isSnoozed()) {
                    this.mNotificationManager.cancelAsUser(fsUuid, 1397772886, UserHandle.ALL);
                } else {
                    String string = this.mContext.getString(R.string.config_accountTypeToKeepFirstAccount, volumeRecord.getNickname());
                    String string2 = this.mContext.getString(R.string.config_UsbDeviceConnectionHandling_component);
                    Notification.Builder builderExtend = new Notification.Builder(this.mContext, NotificationChannels.STORAGE).setSmallIcon(R.drawable.ic_media_route_connecting_light_00_mtrl).setColor(this.mContext.getColor(R.color.car_cyan_400)).setContentTitle(string).setContentText(string2).setContentIntent(buildForgetPendingIntent(volumeRecord)).setStyle(new Notification.BigTextStyle().bigText(string2)).setVisibility(1).setLocalOnly(true).setCategory("sys").setDeleteIntent(buildSnoozeIntent(fsUuid)).extend(new Notification.TvExtender());
                    SystemUI.overrideNotificationAppName(this.mContext, builderExtend, false);
                    this.mNotificationManager.notifyAsUser(fsUuid, 1397772886, builderExtend.build(), UserHandle.ALL);
                }
            }
        }
    }

    private void onDiskScannedInternal(DiskInfo diskInfo, int i) {
        if (i != 0 || diskInfo.size <= 0) {
            this.mNotificationManager.cancelAsUser(diskInfo.getId(), 1396986699, UserHandle.ALL);
            return;
        }
        String string = this.mContext.getString(R.string.config_customAdbPublicKeyConfirmationSecondaryUserComponent, diskInfo.getDescription());
        String string2 = this.mContext.getString(R.string.config_customAdbPublicKeyConfirmationComponent, diskInfo.getDescription());
        Notification.Builder builderExtend = new Notification.Builder(this.mContext, NotificationChannels.STORAGE).setSmallIcon(getSmallIcon(diskInfo, 6)).setColor(this.mContext.getColor(R.color.car_cyan_400)).setContentTitle(string).setContentText(string2).setContentIntent(buildInitPendingIntent(diskInfo)).setStyle(new Notification.BigTextStyle().bigText(string2)).setVisibility(1).setLocalOnly(true).setCategory("err").extend(new Notification.TvExtender());
        SystemUI.overrideNotificationAppName(this.mContext, builderExtend, false);
        this.mNotificationManager.notifyAsUser(diskInfo.getId(), 1396986699, builderExtend.build(), UserHandle.ALL);
    }

    private void onDiskDestroyedInternal(DiskInfo diskInfo) {
        this.mNotificationManager.cancelAsUser(diskInfo.getId(), 1396986699, UserHandle.ALL);
    }

    private void onVolumeStateChangedInternal(VolumeInfo volumeInfo) {
        switch (volumeInfo.getType()) {
            case 0:
                onPublicVolumeStateChangedInternal(volumeInfo);
                break;
            case 1:
                onPrivateVolumeStateChangedInternal(volumeInfo);
                break;
        }
    }

    private void onPrivateVolumeStateChangedInternal(VolumeInfo volumeInfo) {
        Log.d("StorageNotification", "Notifying about private volume: " + volumeInfo.toString());
        updateMissingPrivateVolumes();
    }

    private void onPublicVolumeStateChangedInternal(VolumeInfo volumeInfo) {
        Notification notificationOnVolumeUnmounted;
        Log.d("StorageNotification", "Notifying about public volume: " + volumeInfo.toString());
        switch (volumeInfo.getState()) {
            case 0:
                notificationOnVolumeUnmounted = onVolumeUnmounted(volumeInfo);
                break;
            case 1:
                notificationOnVolumeUnmounted = onVolumeChecking(volumeInfo);
                break;
            case 2:
            case 3:
                notificationOnVolumeUnmounted = onVolumeMounted(volumeInfo);
                break;
            case 4:
                notificationOnVolumeUnmounted = onVolumeFormatting(volumeInfo);
                break;
            case 5:
                notificationOnVolumeUnmounted = onVolumeEjecting(volumeInfo);
                break;
            case 6:
                notificationOnVolumeUnmounted = onVolumeUnmountable(volumeInfo);
                break;
            case 7:
                notificationOnVolumeUnmounted = onVolumeRemoved(volumeInfo);
                break;
            case 8:
                notificationOnVolumeUnmounted = onVolumeBadRemoval(volumeInfo);
                break;
            default:
                notificationOnVolumeUnmounted = null;
                break;
        }
        if (notificationOnVolumeUnmounted != null) {
            this.mNotificationManager.notifyAsUser(volumeInfo.getId(), 1397773634, notificationOnVolumeUnmounted, UserHandle.of(volumeInfo.getMountUserId()));
        } else {
            this.mNotificationManager.cancelAsUser(volumeInfo.getId(), 1397773634, UserHandle.of(volumeInfo.getMountUserId()));
        }
    }

    private Notification onVolumeUnmounted(VolumeInfo volumeInfo) {
        return null;
    }

    private Notification onVolumeChecking(VolumeInfo volumeInfo) {
        DiskInfo disk = volumeInfo.getDisk();
        return buildNotificationBuilder(volumeInfo, this.mContext.getString(R.string.condition_provider_service_binding_label, disk.getDescription()), this.mContext.getString(R.string.concurrent_display_notification_thermal_title, disk.getDescription())).setCategory("progress").setOngoing(true).build();
    }

    private Notification onVolumeMounted(VolumeInfo volumeInfo) {
        VolumeRecord volumeRecordFindRecordByUuid = this.mStorageManager.findRecordByUuid(volumeInfo.getFsUuid());
        DiskInfo disk = volumeInfo.getDisk();
        if (volumeRecordFindRecordByUuid.isSnoozed() && disk.isAdoptable()) {
            return null;
        }
        if (disk.isAdoptable() && !volumeRecordFindRecordByUuid.isInited()) {
            String description = disk.getDescription();
            String string = this.mContext.getString(R.string.config_batterySaverDeviceSpecificConfig, disk.getDescription());
            buildInitPendingIntent(volumeInfo);
            return buildNotificationBuilder(volumeInfo, description, string).addAction(new Notification.Action(R.drawable.ic_ab_back_holo_dark_am, this.mContext.getString(R.string.config_chooserActivity), buildUnmountPendingIntent(volumeInfo))).setDeleteIntent(buildSnoozeIntent(volumeInfo.getFsUuid())).build();
        }
        String description2 = disk.getDescription();
        String string2 = this.mContext.getString(R.string.config_batterymeterFillMask, disk.getDescription());
        buildBrowsePendingIntent(volumeInfo);
        Notification.Builder category = buildNotificationBuilder(volumeInfo, description2, string2).addAction(new Notification.Action(R.drawable.ic_ab_back_holo_dark_am, this.mContext.getString(R.string.config_chooserActivity), buildUnmountPendingIntent(volumeInfo))).setCategory("sys");
        if (disk.isAdoptable()) {
            category.setDeleteIntent(buildSnoozeIntent(volumeInfo.getFsUuid()));
        }
        return category.build();
    }

    private Notification onVolumeFormatting(VolumeInfo volumeInfo) {
        return null;
    }

    private Notification onVolumeEjecting(VolumeInfo volumeInfo) {
        DiskInfo disk = volumeInfo.getDisk();
        return buildNotificationBuilder(volumeInfo, this.mContext.getString(R.string.config_credentialManagerReceiverComponent, disk.getDescription()), this.mContext.getString(R.string.config_controlsPackage, disk.getDescription())).setCategory("progress").setOngoing(true).build();
    }

    private Notification onVolumeUnmountable(VolumeInfo volumeInfo) {
        DiskInfo disk = volumeInfo.getDisk();
        return buildNotificationBuilder(volumeInfo, this.mContext.getString(R.string.config_companionDeviceManagerPackage, disk.getDescription()), this.mContext.getString(R.string.config_clockFontFamily, disk.getDescription())).setContentIntent(buildInitPendingIntent(volumeInfo)).setCategory("err").build();
    }

    private Notification onVolumeRemoved(VolumeInfo volumeInfo) {
        if (!volumeInfo.isPrimary()) {
            return null;
        }
        DiskInfo disk = volumeInfo.getDisk();
        return buildNotificationBuilder(volumeInfo, this.mContext.getString(R.string.config_batterymeterErrorPerimeterPath, disk.getDescription()), this.mContext.getString(R.string.config_batterymeterBoltPath, disk.getDescription())).setCategory("err").build();
    }

    private Notification onVolumeBadRemoval(VolumeInfo volumeInfo) {
        if (!volumeInfo.isPrimary()) {
            return null;
        }
        DiskInfo disk = volumeInfo.getDisk();
        return buildNotificationBuilder(volumeInfo, this.mContext.getString(R.string.concurrent_display_notification_power_save_title, disk.getDescription()), this.mContext.getString(R.string.concurrent_display_notification_power_save_content, disk.getDescription())).setCategory("err").build();
    }

    private void onMoveProgress(MoveInfo moveInfo, int i, long j) {
        String string;
        CharSequence duration;
        PendingIntent pendingIntentBuildWizardMigratePendingIntent;
        if (!TextUtils.isEmpty(moveInfo.label)) {
            string = this.mContext.getString(R.string.config_ambientContextPackageNameExtraKey, moveInfo.label);
        } else {
            string = this.mContext.getString(R.string.config_bandwidthEstimateSource);
        }
        if (j < 0) {
            duration = null;
        } else {
            duration = DateUtils.formatDuration(j);
        }
        if (moveInfo.packageName != null) {
            pendingIntentBuildWizardMigratePendingIntent = buildWizardMovePendingIntent(moveInfo);
        } else {
            pendingIntentBuildWizardMigratePendingIntent = buildWizardMigratePendingIntent(moveInfo);
        }
        Notification.Builder ongoing = new Notification.Builder(this.mContext, NotificationChannels.STORAGE).setSmallIcon(R.drawable.ic_media_route_connecting_light_00_mtrl).setColor(this.mContext.getColor(R.color.car_cyan_400)).setContentTitle(string).setContentText(duration).setContentIntent(pendingIntentBuildWizardMigratePendingIntent).setStyle(new Notification.BigTextStyle().bigText(duration)).setVisibility(1).setLocalOnly(true).setCategory("progress").setProgress(100, i, false).setOngoing(true);
        SystemUI.overrideNotificationAppName(this.mContext, ongoing, false);
        this.mNotificationManager.notifyAsUser(moveInfo.packageName, 1397575510, ongoing.build(), UserHandle.ALL);
    }

    private void onMoveFinished(MoveInfo moveInfo, int i) {
        String string;
        String string2;
        PendingIntent pendingIntentBuildVolumeSettingsPendingIntent;
        if (moveInfo.packageName != null) {
            this.mNotificationManager.cancelAsUser(moveInfo.packageName, 1397575510, UserHandle.ALL);
            return;
        }
        VolumeInfo primaryStorageCurrentVolume = this.mContext.getPackageManager().getPrimaryStorageCurrentVolume();
        String bestVolumeDescription = this.mStorageManager.getBestVolumeDescription(primaryStorageCurrentVolume);
        if (i == -100) {
            string = this.mContext.getString(R.string.config_appsNotReportingCrashes);
            string2 = this.mContext.getString(R.string.config_appsAuthorizedForSharedAccounts, bestVolumeDescription);
        } else {
            string = this.mContext.getString(R.string.config_ambientContextEventArrayExtraKey);
            string2 = this.mContext.getString(R.string.config_activityRecognitionHardwarePackageName);
        }
        if (primaryStorageCurrentVolume != null && primaryStorageCurrentVolume.getDisk() != null) {
            pendingIntentBuildVolumeSettingsPendingIntent = buildWizardReadyPendingIntent(primaryStorageCurrentVolume.getDisk());
        } else if (primaryStorageCurrentVolume != null) {
            pendingIntentBuildVolumeSettingsPendingIntent = buildVolumeSettingsPendingIntent(primaryStorageCurrentVolume);
        } else {
            pendingIntentBuildVolumeSettingsPendingIntent = null;
        }
        Notification.Builder autoCancel = new Notification.Builder(this.mContext, NotificationChannels.STORAGE).setSmallIcon(R.drawable.ic_media_route_connecting_light_00_mtrl).setColor(this.mContext.getColor(R.color.car_cyan_400)).setContentTitle(string).setContentText(string2).setContentIntent(pendingIntentBuildVolumeSettingsPendingIntent).setStyle(new Notification.BigTextStyle().bigText(string2)).setVisibility(1).setLocalOnly(true).setCategory("sys").setAutoCancel(true);
        SystemUI.overrideNotificationAppName(this.mContext, autoCancel, false);
        this.mNotificationManager.notifyAsUser(moveInfo.packageName, 1397575510, autoCancel.build(), UserHandle.ALL);
    }

    private int getSmallIcon(DiskInfo diskInfo, int i) {
        return diskInfo.isSd() ? (i == 1 || i == 5) ? R.drawable.ic_media_route_connecting_light_00_mtrl : R.drawable.ic_media_route_connecting_light_00_mtrl : diskInfo.isUsb() ? R.drawable.ic_media_route_disabled_holo_dark : R.drawable.ic_media_route_connecting_light_00_mtrl;
    }

    private Notification.Builder buildNotificationBuilder(VolumeInfo volumeInfo, CharSequence charSequence, CharSequence charSequence2) {
        Notification.Builder builderExtend = new Notification.Builder(this.mContext, NotificationChannels.STORAGE).setSmallIcon(getSmallIcon(volumeInfo.getDisk(), volumeInfo.getState())).setColor(this.mContext.getColor(R.color.car_cyan_400)).setContentTitle(charSequence).setContentText(charSequence2).setStyle(new Notification.BigTextStyle().bigText(charSequence2)).setVisibility(1).setLocalOnly(true).extend(new Notification.TvExtender());
        overrideNotificationAppName(this.mContext, builderExtend, false);
        return builderExtend;
    }

    private PendingIntent buildInitPendingIntent(DiskInfo diskInfo) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent();
        if (isTv()) {
            intent.setPackage("com.android.tv.settings");
            intent.setAction("com.android.tv.settings.action.NEW_STORAGE");
        } else {
            intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardInit");
        }
        intent.putExtra("android.os.storage.extra.DISK_ID", diskInfo.getId());
        return PendingIntent.getActivityAsUser(this.mContext, diskInfo.getId().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildInitPendingIntent(VolumeInfo volumeInfo) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent();
        if (isTv()) {
            intent.setPackage("com.android.tv.settings");
            intent.setAction("com.android.tv.settings.action.NEW_STORAGE");
        } else {
            intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardInit");
        }
        intent.putExtra("android.os.storage.extra.VOLUME_ID", volumeInfo.getId());
        return PendingIntent.getActivityAsUser(this.mContext, volumeInfo.getId().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildUnmountPendingIntent(VolumeInfo volumeInfo) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent();
        if (isTv()) {
            intent.setPackage("com.android.tv.settings");
            intent.setAction("com.android.tv.settings.action.UNMOUNT_STORAGE");
            intent.putExtra("android.os.storage.extra.VOLUME_ID", volumeInfo.getId());
            return PendingIntent.getActivityAsUser(this.mContext, volumeInfo.getId().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
        }
        intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageUnmountReceiver");
        intent.putExtra("android.os.storage.extra.VOLUME_ID", volumeInfo.getId());
        return PendingIntent.getBroadcastAsUser(this.mContext, volumeInfo.getId().hashCode(), intent, 268435456, UserHandle.CURRENT);
    }

    private PendingIntent buildBrowsePendingIntent(VolumeInfo volumeInfo) {
        Intent intentBuildBrowseIntentForUser = volumeInfo.buildBrowseIntentForUser(volumeInfo.getMountUserId());
        return PendingIntent.getActivityAsUser(this.mContext, volumeInfo.getId().hashCode(), intentBuildBrowseIntentForUser, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildVolumeSettingsPendingIntent(VolumeInfo volumeInfo) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent();
        if (isTv()) {
            intent.setPackage("com.android.tv.settings");
            intent.setAction("android.settings.INTERNAL_STORAGE_SETTINGS");
        } else {
            switch (volumeInfo.getType()) {
                case 0:
                    intent.setClassName("com.android.settings", "com.android.settings.Settings$PublicVolumeSettingsActivity");
                    break;
                case 1:
                    intent.setClassName("com.android.settings", "com.android.settings.Settings$PrivateVolumeSettingsActivity");
                    break;
                default:
                    return null;
            }
        }
        intent.putExtra("android.os.storage.extra.VOLUME_ID", volumeInfo.getId());
        return PendingIntent.getActivityAsUser(this.mContext, volumeInfo.getId().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildSnoozeIntent(String str) {
        Intent intent = new Intent("com.android.systemui.action.SNOOZE_VOLUME");
        intent.putExtra("android.os.storage.extra.FS_UUID", str);
        return PendingIntent.getBroadcastAsUser(this.mContext, str.hashCode(), intent, 268435456, UserHandle.CURRENT);
    }

    private PendingIntent buildForgetPendingIntent(VolumeRecord volumeRecord) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.Settings$PrivateVolumeForgetActivity");
        intent.putExtra("android.os.storage.extra.FS_UUID", volumeRecord.getFsUuid());
        return PendingIntent.getActivityAsUser(this.mContext, volumeRecord.getFsUuid().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildWizardMigratePendingIntent(MoveInfo moveInfo) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent();
        if (isTv()) {
            intent.setPackage("com.android.tv.settings");
            intent.setAction("com.android.tv.settings.action.MIGRATE_STORAGE");
        } else {
            intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardMigrateProgress");
        }
        intent.putExtra("android.content.pm.extra.MOVE_ID", moveInfo.moveId);
        VolumeInfo volumeInfoFindVolumeByQualifiedUuid = this.mStorageManager.findVolumeByQualifiedUuid(moveInfo.volumeUuid);
        if (volumeInfoFindVolumeByQualifiedUuid != null) {
            intent.putExtra("android.os.storage.extra.VOLUME_ID", volumeInfoFindVolumeByQualifiedUuid.getId());
        }
        return PendingIntent.getActivityAsUser(this.mContext, moveInfo.moveId, intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildWizardMovePendingIntent(MoveInfo moveInfo) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent();
        if (isTv()) {
            intent.setPackage("com.android.tv.settings");
            intent.setAction("com.android.tv.settings.action.MOVE_APP");
        } else {
            intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardMoveProgress");
        }
        intent.putExtra("android.content.pm.extra.MOVE_ID", moveInfo.moveId);
        return PendingIntent.getActivityAsUser(this.mContext, moveInfo.moveId, intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildWizardReadyPendingIntent(DiskInfo diskInfo) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent();
        if (isTv()) {
            intent.setPackage("com.android.tv.settings");
            intent.setAction("android.settings.INTERNAL_STORAGE_SETTINGS");
        } else {
            intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardReady");
        }
        intent.putExtra("android.os.storage.extra.DISK_ID", diskInfo.getId());
        return PendingIntent.getActivityAsUser(this.mContext, diskInfo.getId().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }

    private boolean isTv() {
        return this.mContext.getPackageManager().hasSystemFeature("android.software.leanback");
    }
}
