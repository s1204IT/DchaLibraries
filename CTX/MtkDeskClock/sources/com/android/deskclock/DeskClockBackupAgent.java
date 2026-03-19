package com.android.deskclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import com.android.deskclock.LogUtils;
import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

public class DeskClockBackupAgent extends BackupAgent {
    public static final String ACTION_COMPLETE_RESTORE = "com.android.deskclock.action.COMPLETE_RESTORE";
    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("DeskClockBackupAgent");

    @Override
    public void onBackup(ParcelFileDescriptor parcelFileDescriptor, BackupDataOutput backupDataOutput, ParcelFileDescriptor parcelFileDescriptor2) throws IOException {
    }

    @Override
    public void onRestore(BackupDataInput backupDataInput, int i, ParcelFileDescriptor parcelFileDescriptor) throws IOException {
    }

    @Override
    public void onRestoreFile(@NonNull ParcelFileDescriptor parcelFileDescriptor, long j, File file, int i, long j2, long j3) throws IOException {
        File file2;
        if (file.getName().endsWith("_preferences.xml")) {
            file2 = new File(file.getParentFile(), getPackageName() + "_preferences.xml");
        } else {
            file2 = file;
        }
        super.onRestoreFile(parcelFileDescriptor, j, file2, i, j2, j3);
    }

    @Override
    public void onRestoreFinished() {
        Utils.isNOrLater();
        DataModel.getDataModel().setRestoreBackupFinished(true);
        ((AlarmManager) getSystemService(NotificationCompat.CATEGORY_ALARM)).setExact(2, SystemClock.elapsedRealtime() + 10000, PendingIntent.getBroadcast(this, 0, new Intent(ACTION_COMPLETE_RESTORE).setClass(this, AlarmInitReceiver.class), 1342177280));
        LOGGER.i("Waiting for %s to complete the data restore", ACTION_COMPLETE_RESTORE);
    }

    public static boolean processRestoredData(Context context) throws Exception {
        if (!DataModel.getDataModel().isRestoreBackupFinished()) {
            return false;
        }
        LOGGER.i("processRestoredData() started", new Object[0]);
        ContentResolver contentResolver = context.getContentResolver();
        List<Alarm> alarms = Alarm.getAlarms(contentResolver, null, new String[0]);
        Calendar calendar = Calendar.getInstance();
        for (Alarm alarm : alarms) {
            AlarmStateManager.deleteAllInstances(context, alarm.id);
            if (alarm.enabled) {
                AlarmInstance alarmInstanceAddInstance = AlarmInstance.addInstance(contentResolver, alarm.createInstanceAfter(calendar));
                AlarmStateManager.registerInstance(context, alarmInstanceAddInstance, true);
                LOGGER.i("DeskClockBackupAgent scheduled alarm instance: %s", alarmInstanceAddInstance);
            }
        }
        DataModel.getDataModel().setRestoreBackupFinished(false);
        LOGGER.i("processRestoredData() completed", new Object[0]);
        return true;
    }
}
