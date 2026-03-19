package com.android.deskclock.data;

import android.content.SharedPreferences;
import com.android.deskclock.data.Stopwatch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class StopwatchDAO {
    private static final String ACCUMULATED_TIME = "sw_accum_time";
    private static final String LAP_ACCUMULATED_TIME = "sw_lap_time_";
    private static final String LAP_COUNT = "sw_lap_num";
    private static final String LAST_START_TIME = "sw_start_time";
    private static final String LAST_WALL_CLOCK_TIME = "sw_wall_clock_time";
    private static final String STATE = "sw_state";

    private StopwatchDAO() {
    }

    static Stopwatch getStopwatch(SharedPreferences sharedPreferences) {
        Stopwatch stopwatch = new Stopwatch(Stopwatch.State.values()[sharedPreferences.getInt(STATE, Stopwatch.State.RESET.ordinal())], sharedPreferences.getLong(LAST_START_TIME, Long.MIN_VALUE), sharedPreferences.getLong(LAST_WALL_CLOCK_TIME, Long.MIN_VALUE), sharedPreferences.getLong(ACCUMULATED_TIME, 0L));
        if (stopwatch.getTotalTime() < 0) {
            Stopwatch stopwatchReset = stopwatch.reset();
            setStopwatch(sharedPreferences, stopwatchReset);
            return stopwatchReset;
        }
        return stopwatch;
    }

    static void setStopwatch(SharedPreferences sharedPreferences, Stopwatch stopwatch) {
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        if (stopwatch.isReset()) {
            editorEdit.remove(STATE).remove(LAST_START_TIME).remove(LAST_WALL_CLOCK_TIME).remove(ACCUMULATED_TIME);
        } else {
            editorEdit.putInt(STATE, stopwatch.getState().ordinal()).putLong(LAST_START_TIME, stopwatch.getLastStartTime()).putLong(LAST_WALL_CLOCK_TIME, stopwatch.getLastWallClockTime()).putLong(ACCUMULATED_TIME, stopwatch.getAccumulatedTime());
        }
        editorEdit.apply();
    }

    static List<Lap> getLaps(SharedPreferences sharedPreferences) {
        int i = sharedPreferences.getInt(LAP_COUNT, 0);
        ArrayList arrayList = new ArrayList(i);
        int i2 = 1;
        long j = 0;
        while (i2 <= i) {
            long j2 = sharedPreferences.getLong(LAP_ACCUMULATED_TIME + i2, 0L);
            arrayList.add(new Lap(i2, j2 - j, j2));
            i2++;
            j = j2;
        }
        Collections.reverse(arrayList);
        return arrayList;
    }

    static void addLap(SharedPreferences sharedPreferences, int i, long j) {
        sharedPreferences.edit().putInt(LAP_COUNT, i).putLong(LAP_ACCUMULATED_TIME + i, j).apply();
    }

    static void clearLaps(SharedPreferences sharedPreferences) {
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        int i = sharedPreferences.getInt(LAP_COUNT, 0);
        for (int i2 = 1; i2 <= i; i2++) {
            editorEdit.remove(LAP_ACCUMULATED_TIME + i2);
        }
        editorEdit.remove(LAP_COUNT);
        editorEdit.apply();
    }
}
