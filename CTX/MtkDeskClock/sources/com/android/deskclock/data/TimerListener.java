package com.android.deskclock.data;

public interface TimerListener {
    void timerAdded(Timer timer);

    void timerRemoved(Timer timer);

    void timerUpdated(Timer timer, Timer timer2);
}
