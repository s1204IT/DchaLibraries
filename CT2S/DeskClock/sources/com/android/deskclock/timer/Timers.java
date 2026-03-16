package com.android.deskclock.timer;

import java.util.ArrayList;
import java.util.Iterator;

public class Timers {
    public static TimerObj findTimer(ArrayList<TimerObj> timers, int timerId) {
        for (TimerObj t : timers) {
            if (t.mTimerId == timerId) {
                return t;
            }
        }
        return null;
    }

    public static TimerObj findExpiredTimer(ArrayList<TimerObj> timers) {
        for (TimerObj t : timers) {
            if (t.mState == 3) {
                return t;
            }
        }
        return null;
    }

    public static ArrayList<TimerObj> timersInUse(ArrayList<TimerObj> timers) {
        ArrayList<TimerObj> result = (ArrayList) timers.clone();
        Iterator<TimerObj> it = result.iterator();
        while (it.hasNext()) {
            TimerObj timer = it.next();
            if (!timer.isInUse()) {
                it.remove();
            }
        }
        return result;
    }

    public static ArrayList<TimerObj> timersInTimesUp(ArrayList<TimerObj> timers) {
        ArrayList<TimerObj> result = (ArrayList) timers.clone();
        Iterator<TimerObj> it = result.iterator();
        while (it.hasNext()) {
            TimerObj timer = it.next();
            if (timer.mState != 3) {
                it.remove();
            }
        }
        return result;
    }
}
