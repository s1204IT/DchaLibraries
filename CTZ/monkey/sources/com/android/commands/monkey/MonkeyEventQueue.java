package com.android.commands.monkey;

import java.util.LinkedList;
import java.util.Random;

public class MonkeyEventQueue extends LinkedList<MonkeyEvent> {
    private Random mRandom;
    private boolean mRandomizeThrottle;
    private long mThrottle;

    public MonkeyEventQueue(Random random, long j, boolean z) {
        this.mRandom = random;
        this.mThrottle = j;
        this.mRandomizeThrottle = z;
    }

    @Override
    public void addLast(MonkeyEvent monkeyEvent) {
        super.add(monkeyEvent);
        if (monkeyEvent.isThrottlable()) {
            long j = this.mThrottle;
            if (this.mRandomizeThrottle && this.mThrottle > 0) {
                long jNextLong = this.mRandom.nextLong();
                if (jNextLong < 0) {
                    jNextLong = -jNextLong;
                }
                j = (jNextLong % this.mThrottle) + 1;
            }
            super.add(new MonkeyThrottleEvent(j));
        }
    }
}
