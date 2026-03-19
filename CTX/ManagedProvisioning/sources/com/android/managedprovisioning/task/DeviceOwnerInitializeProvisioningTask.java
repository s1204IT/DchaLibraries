package com.android.managedprovisioning.task;

import android.app.AlarmManager;
import android.content.Context;
import com.android.internal.app.LocalePicker;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.task.AbstractProvisioningTask;
import java.util.Locale;

public class DeviceOwnerInitializeProvisioningTask extends AbstractProvisioningTask {
    public DeviceOwnerInitializeProvisioningTask(Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback) {
        super(context, provisioningParams, callback);
    }

    @Override
    public int getStatusMsgId() {
        return R.string.progress_initialize;
    }

    @Override
    public void run(int i) {
        setTimeAndTimezone(this.mProvisioningParams.timeZone, this.mProvisioningParams.localTime);
        setLocale(this.mProvisioningParams.locale);
        success();
    }

    private void setTimeAndTimezone(String str, long j) {
        try {
            AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService(AlarmManager.class);
            if (str != null) {
                alarmManager.setTimeZone(str);
            }
            if (j > 0) {
                alarmManager.setTime(j);
            }
        } catch (Exception e) {
            ProvisionLogger.loge("Alarm manager failed to set the system time/timezone.", e);
        }
    }

    private void setLocale(Locale locale) {
        if (locale == null || locale.equals(Locale.getDefault())) {
            return;
        }
        try {
            LocalePicker.updateLocale(locale);
        } catch (Exception e) {
            ProvisionLogger.loge("Failed to set the system locale.", e);
        }
    }
}
