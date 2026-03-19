package com.android.internal.telephony.metrics;

import android.os.SystemClock;
import com.android.internal.telephony.nano.TelephonyProto;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class InProgressCallSession {
    private static final int MAX_EVENTS = 300;
    private int mLastKnownPhoneState;
    public final int phoneId;
    private boolean mEventsDropped = false;
    public final Deque<TelephonyProto.TelephonyCallSession.Event> events = new ArrayDeque();
    public final int startSystemTimeMin = TelephonyMetrics.roundSessionStart(System.currentTimeMillis());
    public final long startElapsedTimeMs = SystemClock.elapsedRealtime();
    private long mLastElapsedTimeMs = this.startElapsedTimeMs;

    public boolean isEventsDropped() {
        return this.mEventsDropped;
    }

    public InProgressCallSession(int i) {
        this.phoneId = i;
    }

    public void addEvent(CallSessionEventBuilder callSessionEventBuilder) {
        addEvent(SystemClock.elapsedRealtime(), callSessionEventBuilder);
    }

    public synchronized void addEvent(long j, CallSessionEventBuilder callSessionEventBuilder) {
        if (this.events.size() >= 300) {
            this.events.removeFirst();
            this.mEventsDropped = true;
        }
        callSessionEventBuilder.setDelay(TelephonyMetrics.toPrivacyFuzzedTimeInterval(this.mLastElapsedTimeMs, j));
        this.events.add(callSessionEventBuilder.build());
        this.mLastElapsedTimeMs = j;
    }

    public boolean containsCsCalls() {
        Iterator<TelephonyProto.TelephonyCallSession.Event> it = this.events.iterator();
        while (it.hasNext()) {
            if (it.next().type == 10) {
                return true;
            }
        }
        return false;
    }

    public void setLastKnownPhoneState(int i) {
        this.mLastKnownPhoneState = i;
    }

    public boolean isPhoneIdle() {
        return this.mLastKnownPhoneState == 1;
    }
}
