package com.android.deskclock.data;

public interface StopwatchListener {
    void lapAdded(Lap lap);

    void stopwatchUpdated(Stopwatch stopwatch, Stopwatch stopwatch2);
}
