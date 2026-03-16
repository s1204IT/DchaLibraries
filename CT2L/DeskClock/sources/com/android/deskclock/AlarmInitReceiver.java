package com.android.deskclock;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import com.android.deskclock.alarms.AlarmActivity;
import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.timer.TimerObj;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

public class AlarmInitReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        final String action = intent.getAction();
        LogUtils.v("AlarmInitReceiver " + action, new Object[0]);
        final BroadcastReceiver.PendingResult result = goAsync();
        final PowerManager.WakeLock wl = AlarmAlertWakeLock.createPartialWakeLock(context);
        wl.acquire();
        AlarmStateManager.updateGlobalIntentId(context);
        AsyncHandler.post(new Runnable() {
            @Override
            public void run() throws Throwable {
                AlarmInstance instance;
                if (action.equals("android.intent.action.BOOT_COMPLETED")) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    LogUtils.v("AlarmInitReceiver - Reset timers and clear stopwatch data", new Object[0]);
                    TimerObj.resetTimersInSharedPrefs(prefs);
                    Utils.clearSwSharedPref(prefs);
                    if (!prefs.getBoolean("vol_def_done", false)) {
                        LogUtils.v("AlarmInitReceiver - resetting volume button default", new Object[0]);
                        AlarmInitReceiver.this.switchVolumeButtonDefault(prefs);
                    }
                    long powerOffAlarmTimeSec = AlarmInitReceiver.this.readPowerOffAlarm();
                    if (powerOffAlarmTimeSec > 0 && (instance = AlarmInitReceiver.this.getAlarmByTime(context, powerOffAlarmTimeSec)) != null) {
                        AlarmActivity.sPowerOnAlarmId = instance.mId;
                        AlarmStateManager.setFiredState(context, instance);
                    }
                }
                AlarmStateManager.fixAlarmInstances(context);
                result.finish();
                LogUtils.v("AlarmInitReceiver finished", new Object[0]);
                wl.release();
            }
        });
    }

    private long readPowerOffAlarm() throws Throwable {
        BufferedReader is;
        File powerOffAlarmDeviceFile = new File("/dev/alarm-poweroff");
        BufferedReader is2 = null;
        try {
            try {
                is = new BufferedReader(new FileReader(powerOffAlarmDeviceFile));
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e) {
            e = e;
        } catch (NumberFormatException e2) {
            e = e2;
        } catch (Exception e3) {
            e = e3;
        }
        try {
            String alarmTimeString = is.readLine();
            alarmTime = alarmTimeString != null ? Long.parseLong(alarmTimeString) : 0L;
            try {
                is.close();
                is2 = is;
            } catch (Exception e4) {
                Log.e("Fail to close file reader" + e4);
                is2 = is;
            }
        } catch (IOException e5) {
            e = e5;
            is2 = is;
            Log.e("Fail to read from alarm-poweroff :" + e);
            try {
                is2.close();
            } catch (Exception e6) {
                Log.e("Fail to close file reader" + e6);
            }
        } catch (NumberFormatException e7) {
            e = e7;
            is2 = is;
            Log.e("Fail to convert alarmTime :" + e);
            try {
                is2.close();
            } catch (Exception e8) {
                Log.e("Fail to close file reader" + e8);
            }
        } catch (Exception e9) {
            e = e9;
            is2 = is;
            Log.e("Fail to get power off alarm time :" + e);
            try {
                is2.close();
            } catch (Exception e10) {
                Log.e("Fail to close file reader" + e10);
            }
        } catch (Throwable th2) {
            th = th2;
            is2 = is;
            try {
                is2.close();
            } catch (Exception e11) {
                Log.e("Fail to close file reader" + e11);
            }
            throw th;
        }
        return 1000 * alarmTime;
    }

    private AlarmInstance getAlarmByTime(Context context, long whenMillis) {
        ContentResolver contentResolver = context.getContentResolver();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(whenMillis);
        String year = Integer.toString(c.getTime().getYear());
        String month = Integer.toString(c.getTime().getMonth());
        String day = Integer.toString(c.getTime().getDay());
        String hour = Integer.toString(c.getTime().getHours());
        String minutes = Integer.toString(c.getTime().getMinutes());
        Log.i("whenMillis=" + whenMillis + ", year=" + year + ", month=" + month + ", day=" + day + ", hour=" + hour + ", minutes=" + minutes);
        List<AlarmInstance> instances = AlarmInstance.getInstances(contentResolver, "hour=" + hour + " AND minutes=" + minutes, new String[0]);
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        return instances.get(0);
    }

    private void switchVolumeButtonDefault(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("volume_button_setting", "0");
        editor.putBoolean("vol_def_done", true);
        editor.apply();
    }
}
