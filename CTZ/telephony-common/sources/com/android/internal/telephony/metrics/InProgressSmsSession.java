package com.android.internal.telephony.metrics;

import android.os.SystemClock;
import com.android.internal.telephony.nano.TelephonyProto;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

public class InProgressSmsSession {
    private static final int MAX_EVENTS = 20;
    public final int phoneId;
    private boolean mEventsDropped = false;
    private AtomicInteger mNumExpectedResponses = new AtomicInteger(0);
    public final Deque<TelephonyProto.SmsSession.Event> events = new ArrayDeque();
    public final int startSystemTimeMin = TelephonyMetrics.roundSessionStart(System.currentTimeMillis());
    public final long startElapsedTimeMs = SystemClock.elapsedRealtime();
    private long mLastElapsedTimeMs = this.startElapsedTimeMs;

    public void increaseExpectedResponse() {
        this.mNumExpectedResponses.incrementAndGet();
    }

    public void decreaseExpectedResponse() {
        this.mNumExpectedResponses.decrementAndGet();
    }

    public int getNumExpectedResponses() {
        return this.mNumExpectedResponses.get();
    }

    public boolean isEventsDropped() {
        return this.mEventsDropped;
    }

    public InProgressSmsSession(int i) {
        this.phoneId = i;
    }

    public void addEvent(SmsSessionEventBuilder smsSessionEventBuilder) {
        addEvent(SystemClock.elapsedRealtime(), smsSessionEventBuilder);
    }

    public synchronized void addEvent(long j, SmsSessionEventBuilder smsSessionEventBuilder) {
        if (this.events.size() >= 20) {
            this.events.removeFirst();
            this.mEventsDropped = true;
        }
        smsSessionEventBuilder.setDelay(TelephonyMetrics.toPrivacyFuzzedTimeInterval(this.mLastElapsedTimeMs, j));
        this.events.add(smsSessionEventBuilder.build());
        this.mLastElapsedTimeMs = j;
    }
}
