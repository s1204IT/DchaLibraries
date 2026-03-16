package com.android.deskclock;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.provider.DaysOfWeek;
import com.android.deskclock.timer.TimerObj;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

public class HandleApiCalls extends Activity {
    @Override
    protected void onCreate(Bundle icicle) {
        try {
            super.onCreate(icicle);
            Intent intent = getIntent();
            if (intent != null) {
                if ("android.intent.action.SET_ALARM".equals(intent.getAction())) {
                    handleSetAlarm(intent);
                } else if ("android.intent.action.SHOW_ALARMS".equals(intent.getAction())) {
                    handleShowAlarms();
                } else if ("android.intent.action.SET_TIMER".equals(intent.getAction())) {
                    handleSetTimer(intent);
                }
            }
        } finally {
            finish();
        }
    }

    private void handleSetAlarm(Intent intent) {
        int minutes;
        int hour = intent.getIntExtra("android.intent.extra.alarm.HOUR", -1);
        if (intent.hasExtra("android.intent.extra.alarm.MINUTES")) {
            minutes = intent.getIntExtra("android.intent.extra.alarm.MINUTES", -1);
        } else {
            minutes = 0;
        }
        if (hour < 0 || hour > 23 || minutes < 0 || minutes > 59) {
            Intent createAlarm = Alarm.createIntent(this, DeskClock.class, -1L);
            createAlarm.addFlags(268435456);
            createAlarm.putExtra("deskclock.create.new", true);
            createAlarm.putExtra("deskclock.select.tab", 0);
            startActivity(createAlarm);
            finish();
            LogUtils.i("HandleApiCalls no/invalid time; opening UI", new Object[0]);
            return;
        }
        boolean skipUi = intent.getBooleanExtra("android.intent.extra.alarm.SKIP_UI", false);
        StringBuilder selection = new StringBuilder();
        List<String> args = new ArrayList<>();
        setSelectionFromIntent(intent, hour, minutes, selection, args);
        ContentResolver cr = getContentResolver();
        List<Alarm> alarms = Alarm.getAlarms(cr, selection.toString(), (String[]) args.toArray(new String[args.size()]));
        if (!alarms.isEmpty()) {
            Alarm alarm = alarms.get(0);
            alarm.enabled = true;
            Alarm.updateAlarm(cr, alarm);
            AlarmStateManager.deleteAllInstances(this, alarm.id);
            setupInstance(alarm.createInstanceAfter(Calendar.getInstance()), skipUi);
            LogUtils.i("HandleApiCalls deleted old, created new alarm: %s", alarm);
            finish();
            return;
        }
        String message = getMessageFromIntent(intent);
        DaysOfWeek daysOfWeek = getDaysFromIntent(intent);
        boolean vibrate = intent.getBooleanExtra("android.intent.extra.alarm.VIBRATE", false);
        String alert = intent.getStringExtra("android.intent.extra.alarm.RINGTONE");
        Alarm alarm2 = new Alarm(hour, minutes);
        alarm2.enabled = true;
        alarm2.label = message;
        alarm2.daysOfWeek = daysOfWeek;
        alarm2.vibrate = vibrate;
        if (alert == null) {
            alarm2.alert = RingtoneManager.getDefaultUri(4);
        } else if ("silent".equals(alert) || alert.isEmpty()) {
            alarm2.alert = Alarm.NO_RINGTONE_URI;
        } else {
            alarm2.alert = Uri.parse(alert);
        }
        alarm2.deleteAfterUse = !daysOfWeek.isRepeating() && skipUi;
        Alarm alarm3 = Alarm.addAlarm(cr, alarm2);
        setupInstance(alarm3.createInstanceAfter(Calendar.getInstance()), skipUi);
        LogUtils.i("HandleApiCalls set up alarm: %s", alarm3);
        finish();
    }

    private void handleShowAlarms() {
        startActivity(new Intent(this, (Class<?>) DeskClock.class).putExtra("deskclock.select.tab", 0));
        LogUtils.i("HandleApiCalls show alarms", new Object[0]);
    }

    private void handleSetTimer(Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!intent.hasExtra("android.intent.extra.alarm.LENGTH")) {
            startActivity(new Intent(this, (Class<?>) DeskClock.class).putExtra("deskclock.select.tab", 2).putExtra("deskclock.timers.gotosetup", true));
            LogUtils.i("HandleApiCalls showing timer setup", new Object[0]);
            return;
        }
        long length = 1000 * ((long) intent.getIntExtra("android.intent.extra.alarm.LENGTH", 0));
        if (length < 1000 || length > 86400000) {
            LogUtils.i("Invalid timer length requested: " + length, new Object[0]);
            return;
        }
        String label = getMessageFromIntent(intent);
        TimerObj timer = null;
        ArrayList<TimerObj> timers = new ArrayList<>();
        TimerObj.getTimersFromSharedPrefs(prefs, timers);
        Iterator<TimerObj> it = timers.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            TimerObj t = it.next();
            if (t.mSetupLength == length && TextUtils.equals(label, t.mLabel) && t.mState == 5) {
                timer = t;
                break;
            }
        }
        boolean skipUi = intent.getBooleanExtra("android.intent.extra.alarm.SKIP_UI", false);
        if (timer == null) {
            timer = new TimerObj(length, label, this);
            timer.mDeleteAfterUse = skipUi;
        }
        timer.mState = 1;
        timer.mStartTime = Utils.getTimeNow();
        timer.writeToSharedPref(prefs);
        sendBroadcast(new Intent().setAction("start_timer").putExtra("timer.intent.extra", timer.mTimerId));
        if (skipUi) {
            Utils.showInUseNotifications(this);
        } else {
            startActivity(new Intent(this, (Class<?>) DeskClock.class).putExtra("deskclock.select.tab", 2).putExtra("first_launch_from_api_call", true));
        }
        LogUtils.i("HandleApiCalls timer created: %s", timer);
    }

    private void setupInstance(AlarmInstance instance, boolean skipUi) {
        AlarmInstance instance2 = AlarmInstance.addInstance(getContentResolver(), instance);
        AlarmStateManager.registerInstance(this, instance2, true);
        AlarmUtils.popAlarmSetToast(this, instance2.getAlarmTime().getTimeInMillis());
        if (!skipUi) {
            Intent showAlarm = Alarm.createIntent(this, DeskClock.class, instance2.mAlarmId.longValue());
            showAlarm.putExtra("deskclock.select.tab", 0);
            showAlarm.putExtra("deskclock.scroll.to.alarm", instance2.mAlarmId);
            showAlarm.addFlags(268435456);
            startActivity(showAlarm);
        }
    }

    private String getMessageFromIntent(Intent intent) {
        String message = intent.getStringExtra("android.intent.extra.alarm.MESSAGE");
        return message == null ? "" : message;
    }

    private DaysOfWeek getDaysFromIntent(Intent intent) {
        DaysOfWeek daysOfWeek = new DaysOfWeek(0);
        ArrayList<Integer> days = intent.getIntegerArrayListExtra("android.intent.extra.alarm.DAYS");
        if (days != null) {
            int[] daysArray = new int[days.size()];
            for (int i = 0; i < days.size(); i++) {
                daysArray[i] = days.get(i).intValue();
            }
            daysOfWeek.setDaysOfWeek(true, daysArray);
        } else {
            int[] daysArray2 = intent.getIntArrayExtra("android.intent.extra.alarm.DAYS");
            if (daysArray2 != null) {
                daysOfWeek.setDaysOfWeek(true, daysArray2);
            }
        }
        return daysOfWeek;
    }

    private void setSelectionFromIntent(Intent intent, int hour, int minutes, StringBuilder selection, List<String> args) {
        selection.append("hour").append("=?");
        args.add(String.valueOf(hour));
        selection.append(" AND ").append("minutes").append("=?");
        args.add(String.valueOf(minutes));
        if (intent.hasExtra("android.intent.extra.alarm.MESSAGE")) {
            selection.append(" AND ").append("label").append("=?");
            args.add(getMessageFromIntent(intent));
        }
        selection.append(" AND ").append("daysofweek").append("=?");
        args.add(String.valueOf(intent.hasExtra("android.intent.extra.alarm.DAYS") ? getDaysFromIntent(intent).getBitSet() : 0));
        if (intent.hasExtra("android.intent.extra.alarm.VIBRATE")) {
            selection.append(" AND ").append("vibrate").append("=?");
            args.add(intent.getBooleanExtra("android.intent.extra.alarm.VIBRATE", false) ? "1" : "0");
        }
        if (intent.hasExtra("android.intent.extra.alarm.RINGTONE")) {
            selection.append(" AND ").append("ringtone").append("=?");
            String ringTone = intent.getStringExtra("android.intent.extra.alarm.RINGTONE");
            if (ringTone == null) {
                ringTone = RingtoneManager.getDefaultUri(4).toString();
            } else if ("silent".equals(ringTone) || ringTone.isEmpty()) {
                ringTone = Alarm.NO_RINGTONE;
            }
            args.add(ringTone);
        }
    }
}
