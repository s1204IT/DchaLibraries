package com.android.calendar.alerts;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.provider.CalendarContract;
import android.util.Log;

public class InitAlarmsService extends JobService {
    Context mContext;
    JobParameters mParams;
    private static final Uri SCHEDULE_ALARM_REMOVE_URI = Uri.withAppendedPath(CalendarContract.CONTENT_URI, "schedule_alarms_remove");
    private static final String[] CALENDAR_PERMISSION = {"android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR"};

    private boolean hasRequiredPermission(String[] strArr) {
        for (String str : strArr) {
            if (checkSelfPermission(str) != 0) {
                return false;
            }
        }
        return true;
    }

    private boolean checkPermissions() {
        if (!hasRequiredPermission(CALENDAR_PERMISSION)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d("InitAlarmsService", "Clearing and rescheduling alarms.params: " + jobParameters);
        this.mParams = jobParameters;
        this.mContext = this;
        new InitAlarmsServiceTask().execute(new Void[0]);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }

    private class InitAlarmsServiceTask extends AsyncTask<Void, Void, Boolean> {
        private InitAlarmsServiceTask() {
        }

        @Override
        protected Boolean doInBackground(Void... voidArr) {
            Log.d("InitAlarmsService", "doInBackground Job sleep starts");
            SystemClock.sleep(5000L);
            try {
                Log.d("InitAlarmsService", "doInBackground Job sleep ends");
                if (InitAlarmsService.this.checkPermissions()) {
                    AlertService.updateAlertNotification(InitAlarmsService.this.mContext);
                    InitAlarmsService.this.getContentResolver().update(InitAlarmsService.SCHEDULE_ALARM_REMOVE_URI, new ContentValues(), null, null);
                }
            } catch (IllegalArgumentException e) {
                Log.e("InitAlarmsService", "update failed: " + e.toString());
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            Log.d("InitAlarmsService", "Job finished : " + bool);
            if (InitAlarmsService.this.mParams != null) {
                InitAlarmsService.this.jobFinished(InitAlarmsService.this.mParams, !bool.booleanValue());
            }
        }
    }
}
