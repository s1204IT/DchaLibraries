package com.android.deskclock.alarms;

import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import com.android.deskclock.AlarmUtils;
import com.android.deskclock.R;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.widget.toast.SnackbarManager;
import java.util.Calendar;
import java.util.Iterator;

public final class AlarmUpdateHandler {
    private final Context mAppContext;
    private Alarm mDeletedAlarm;
    private final ScrollHandler mScrollHandler;
    private final View mSnackbarAnchor;

    public AlarmUpdateHandler(Context context, ScrollHandler scrollHandler, ViewGroup viewGroup) {
        this.mAppContext = context.getApplicationContext();
        this.mScrollHandler = scrollHandler;
        this.mSnackbarAnchor = viewGroup;
    }

    public void asyncAddAlarm(final Alarm alarm) {
        new AsyncTask<Void, Void, AlarmInstance>() {
            @Override
            protected AlarmInstance doInBackground(Void... voidArr) {
                if (alarm != null) {
                    Events.sendAlarmEvent(R.string.action_create, R.string.label_deskclock);
                    Alarm alarmAddAlarm = Alarm.addAlarm(AlarmUpdateHandler.this.mAppContext.getContentResolver(), alarm);
                    AlarmUpdateHandler.this.mScrollHandler.setSmoothScrollStableId(alarmAddAlarm.id);
                    if (alarmAddAlarm.enabled) {
                        return AlarmUpdateHandler.this.setupAlarmInstance(alarmAddAlarm);
                    }
                    return null;
                }
                return null;
            }

            @Override
            protected void onPostExecute(AlarmInstance alarmInstance) {
                if (alarmInstance != null) {
                    AlarmUtils.popAlarmSetSnackbar(AlarmUpdateHandler.this.mSnackbarAnchor, alarmInstance.getAlarmTime().getTimeInMillis());
                }
            }
        }.execute(new Void[0]);
    }

    public void asyncUpdateAlarm(final Alarm alarm, final boolean z, final boolean z2) {
        new AsyncTask<Void, Void, AlarmInstance>() {
            @Override
            protected AlarmInstance doInBackground(Void... voidArr) {
                ContentResolver contentResolver = AlarmUpdateHandler.this.mAppContext.getContentResolver();
                Alarm.updateAlarm(contentResolver, alarm);
                if (!z2) {
                    AlarmStateManager.deleteAllInstances(AlarmUpdateHandler.this.mAppContext, alarm.id);
                    if (alarm.enabled) {
                        return AlarmUpdateHandler.this.setupAlarmInstance(alarm);
                    }
                    return null;
                }
                Iterator<AlarmInstance> it = AlarmInstance.getInstancesByAlarmId(contentResolver, alarm.id).iterator();
                while (it.hasNext()) {
                    AlarmInstance alarmInstance = new AlarmInstance(it.next());
                    alarmInstance.mVibrate = alarm.vibrate;
                    alarmInstance.mRingtone = alarm.alert;
                    alarmInstance.mLabel = alarm.label;
                    AlarmInstance.updateInstance(contentResolver, alarmInstance);
                    AlarmNotifications.updateNotification(AlarmUpdateHandler.this.mAppContext, alarmInstance);
                }
                return null;
            }

            @Override
            protected void onPostExecute(AlarmInstance alarmInstance) {
                if (z && alarmInstance != null) {
                    AlarmUtils.popAlarmSetSnackbar(AlarmUpdateHandler.this.mSnackbarAnchor, alarmInstance.getAlarmTime().getTimeInMillis());
                }
            }
        }.execute(new Void[0]);
    }

    public void asyncDeleteAlarm(final Alarm alarm) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voidArr) {
                if (alarm != null) {
                    AlarmStateManager.deleteAllInstances(AlarmUpdateHandler.this.mAppContext, alarm.id);
                    return Boolean.valueOf(Alarm.deleteAlarm(AlarmUpdateHandler.this.mAppContext.getContentResolver(), alarm.id));
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean bool) {
                if (bool.booleanValue()) {
                    AlarmUpdateHandler.this.mDeletedAlarm = alarm;
                    AlarmUpdateHandler.this.showUndoBar();
                }
            }
        }.execute(new Void[0]);
    }

    public void showPredismissToast(AlarmInstance alarmInstance) {
        SnackbarManager.show(Snackbar.make(this.mSnackbarAnchor, this.mAppContext.getString(R.string.alarm_is_dismissed, DateFormat.getTimeFormat(this.mAppContext).format(alarmInstance.getAlarmTime().getTime())), -1));
    }

    public void hideUndoBar() {
        this.mDeletedAlarm = null;
        SnackbarManager.dismiss();
    }

    private void showUndoBar() {
        final Alarm alarm = this.mDeletedAlarm;
        SnackbarManager.show(Snackbar.make(this.mSnackbarAnchor, this.mAppContext.getString(R.string.alarm_deleted), 0).setAction(R.string.alarm_undo, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlarmUpdateHandler.this.mDeletedAlarm = null;
                AlarmUpdateHandler.this.asyncAddAlarm(alarm);
            }
        }));
    }

    private AlarmInstance setupAlarmInstance(Alarm alarm) throws Exception {
        AlarmInstance alarmInstanceAddInstance = AlarmInstance.addInstance(this.mAppContext.getContentResolver(), alarm.createInstanceAfter(Calendar.getInstance()));
        AlarmStateManager.registerInstance(this.mAppContext, alarmInstanceAddInstance, true);
        return alarmInstanceAddInstance;
    }
}
