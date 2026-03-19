package com.android.deskclock.data;

import android.content.SharedPreferences;
import com.android.deskclock.data.Timer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

final class TimerDAO {
    private static final String DELETE_AFTER_USE = "delete_after_use_";
    private static final String LABEL = "timer_label_";
    private static final String LAST_START_TIME = "timer_start_time_";
    private static final String LAST_WALL_CLOCK_TIME = "timer_wall_clock_time_";
    private static final String LENGTH = "timer_setup_timet_";
    private static final String NEXT_TIMER_ID = "next_timer_id";
    private static final String REMAINING_TIME = "timer_time_left_";
    private static final String STATE = "timer_state_";
    private static final String TIMER_IDS = "timers_list";
    private static final String TOTAL_LENGTH = "timer_original_timet_";

    private TimerDAO() {
    }

    static List<Timer> getTimers(SharedPreferences sharedPreferences) {
        SharedPreferences sharedPreferences2 = sharedPreferences;
        Set<String> stringSet = sharedPreferences2.getStringSet(TIMER_IDS, Collections.emptySet());
        ArrayList arrayList = new ArrayList(stringSet.size());
        Iterator<String> it = stringSet.iterator();
        while (it.hasNext()) {
            int i = Integer.parseInt(it.next());
            Timer.State stateFromValue = Timer.State.fromValue(sharedPreferences2.getInt(STATE + i, Timer.State.RESET.getValue()));
            if (stateFromValue != null) {
                long j = sharedPreferences2.getLong(LENGTH + i, Long.MIN_VALUE);
                long j2 = sharedPreferences2.getLong(TOTAL_LENGTH + i, Long.MIN_VALUE);
                arrayList.add(new Timer(i, stateFromValue, j, j2, sharedPreferences2.getLong(LAST_START_TIME + i, Long.MIN_VALUE), sharedPreferences2.getLong(LAST_WALL_CLOCK_TIME + i, Long.MIN_VALUE), sharedPreferences2.getLong(REMAINING_TIME + i, j2), sharedPreferences2.getString(LABEL + i, null), sharedPreferences2.getBoolean(DELETE_AFTER_USE + i, false)));
            }
            sharedPreferences2 = sharedPreferences;
        }
        return arrayList;
    }

    static Timer addTimer(SharedPreferences sharedPreferences, Timer timer) {
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        int i = sharedPreferences.getInt(NEXT_TIMER_ID, 0);
        editorEdit.putInt(NEXT_TIMER_ID, i + 1);
        HashSet hashSet = new HashSet(getTimerIds(sharedPreferences));
        hashSet.add(String.valueOf(i));
        editorEdit.putStringSet(TIMER_IDS, hashSet);
        editorEdit.putInt(STATE + i, timer.getState().getValue());
        editorEdit.putLong(LENGTH + i, timer.getLength());
        editorEdit.putLong(TOTAL_LENGTH + i, timer.getTotalLength());
        editorEdit.putLong(LAST_START_TIME + i, timer.getLastStartTime());
        editorEdit.putLong(LAST_WALL_CLOCK_TIME + i, timer.getLastWallClockTime());
        editorEdit.putLong(REMAINING_TIME + i, timer.getRemainingTime());
        editorEdit.putString(LABEL + i, timer.getLabel());
        editorEdit.putBoolean(DELETE_AFTER_USE + i, timer.getDeleteAfterUse());
        editorEdit.apply();
        return new Timer(i, timer.getState(), timer.getLength(), timer.getTotalLength(), timer.getLastStartTime(), timer.getLastWallClockTime(), timer.getRemainingTime(), timer.getLabel(), timer.getDeleteAfterUse());
    }

    static void updateTimer(SharedPreferences sharedPreferences, Timer timer) {
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        int id = timer.getId();
        editorEdit.putInt(STATE + id, timer.getState().getValue());
        editorEdit.putLong(LENGTH + id, timer.getLength());
        editorEdit.putLong(TOTAL_LENGTH + id, timer.getTotalLength());
        editorEdit.putLong(LAST_START_TIME + id, timer.getLastStartTime());
        editorEdit.putLong(LAST_WALL_CLOCK_TIME + id, timer.getLastWallClockTime());
        editorEdit.putLong(REMAINING_TIME + id, timer.getRemainingTime());
        editorEdit.putString(LABEL + id, timer.getLabel());
        editorEdit.putBoolean(DELETE_AFTER_USE + id, timer.getDeleteAfterUse());
        editorEdit.apply();
    }

    static void removeTimer(SharedPreferences sharedPreferences, Timer timer) {
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        int id = timer.getId();
        HashSet hashSet = new HashSet(getTimerIds(sharedPreferences));
        hashSet.remove(String.valueOf(id));
        if (hashSet.isEmpty()) {
            editorEdit.remove(TIMER_IDS);
            editorEdit.remove(NEXT_TIMER_ID);
        } else {
            editorEdit.putStringSet(TIMER_IDS, hashSet);
        }
        editorEdit.remove(STATE + id);
        editorEdit.remove(LENGTH + id);
        editorEdit.remove(TOTAL_LENGTH + id);
        editorEdit.remove(LAST_START_TIME + id);
        editorEdit.remove(LAST_WALL_CLOCK_TIME + id);
        editorEdit.remove(REMAINING_TIME + id);
        editorEdit.remove(LABEL + id);
        editorEdit.remove(DELETE_AFTER_USE + id);
        editorEdit.apply();
    }

    private static Set<String> getTimerIds(SharedPreferences sharedPreferences) {
        return sharedPreferences.getStringSet(TIMER_IDS, Collections.emptySet());
    }
}
