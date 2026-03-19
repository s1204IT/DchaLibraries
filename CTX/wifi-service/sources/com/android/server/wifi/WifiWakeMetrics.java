package com.android.server.wifi;

import android.os.SystemClock;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.nano.WifiMetricsProto;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WifiWakeMetrics {

    @VisibleForTesting
    static final int MAX_RECORDED_SESSIONS = 10;

    @GuardedBy("mLock")
    private Session mCurrentSession;

    @GuardedBy("mLock")
    private final List<Session> mSessions = new ArrayList();
    private boolean mIsInSession = false;
    private int mTotalSessions = 0;
    private int mTotalWakeups = 0;
    private int mIgnoredStarts = 0;
    private final Object mLock = new Object();

    public void recordStartEvent(int i) {
        synchronized (this.mLock) {
            this.mCurrentSession = new Session(i, SystemClock.elapsedRealtime());
            this.mIsInSession = true;
        }
    }

    public void recordInitializeEvent(int i, int i2) {
        synchronized (this.mLock) {
            if (this.mIsInSession) {
                this.mCurrentSession.recordInitializeEvent(i, i2, SystemClock.elapsedRealtime());
            }
        }
    }

    public void recordUnlockEvent(int i) {
        synchronized (this.mLock) {
            if (this.mIsInSession) {
                this.mCurrentSession.recordUnlockEvent(i, SystemClock.elapsedRealtime());
            }
        }
    }

    public void recordWakeupEvent(int i) {
        synchronized (this.mLock) {
            if (this.mIsInSession) {
                this.mCurrentSession.recordWakeupEvent(i, SystemClock.elapsedRealtime());
            }
        }
    }

    public void recordResetEvent(int i) {
        synchronized (this.mLock) {
            if (this.mIsInSession) {
                this.mCurrentSession.recordResetEvent(i, SystemClock.elapsedRealtime());
                if (this.mCurrentSession.hasWakeupTriggered()) {
                    this.mTotalWakeups++;
                }
                this.mTotalSessions++;
                if (this.mSessions.size() < 10) {
                    this.mSessions.add(this.mCurrentSession);
                }
                this.mIsInSession = false;
            }
        }
    }

    public void recordIgnoredStart() {
        this.mIgnoredStarts++;
    }

    public WifiMetricsProto.WifiWakeStats buildProto() {
        WifiMetricsProto.WifiWakeStats wifiWakeStats = new WifiMetricsProto.WifiWakeStats();
        wifiWakeStats.numSessions = this.mTotalSessions;
        wifiWakeStats.numWakeups = this.mTotalWakeups;
        wifiWakeStats.numIgnoredStarts = this.mIgnoredStarts;
        wifiWakeStats.sessions = new WifiMetricsProto.WifiWakeStats.Session[this.mSessions.size()];
        for (int i = 0; i < this.mSessions.size(); i++) {
            wifiWakeStats.sessions[i] = this.mSessions.get(i).buildProto();
        }
        return wifiWakeStats;
    }

    public void dump(PrintWriter printWriter) {
        synchronized (this.mLock) {
            printWriter.println("-------WifiWake metrics-------");
            printWriter.println("mTotalSessions: " + this.mTotalSessions);
            printWriter.println("mTotalWakeups: " + this.mTotalWakeups);
            printWriter.println("mIgnoredStarts: " + this.mIgnoredStarts);
            printWriter.println("mIsInSession: " + this.mIsInSession);
            printWriter.println("Stored Sessions: " + this.mSessions.size());
            Iterator<Session> it = this.mSessions.iterator();
            while (it.hasNext()) {
                it.next().dump(printWriter);
            }
            if (this.mCurrentSession != null) {
                printWriter.println("Current Session: ");
                this.mCurrentSession.dump(printWriter);
            }
            printWriter.println("----end of WifiWake metrics----");
        }
    }

    public void clear() {
        synchronized (this.mLock) {
            this.mSessions.clear();
            this.mTotalSessions = 0;
            this.mTotalWakeups = 0;
            this.mIgnoredStarts = 0;
        }
    }

    public static class Session {

        @VisibleForTesting
        Event mInitEvent;
        private int mInitializeNetworks = 0;

        @VisibleForTesting
        Event mResetEvent;
        private final int mStartNetworks;
        private final long mStartTimestamp;

        @VisibleForTesting
        Event mUnlockEvent;

        @VisibleForTesting
        Event mWakeupEvent;

        public Session(int i, long j) {
            this.mStartNetworks = i;
            this.mStartTimestamp = j;
        }

        public void recordInitializeEvent(int i, int i2, long j) {
            if (this.mInitEvent == null) {
                this.mInitializeNetworks = i2;
                this.mInitEvent = new Event(i, j - this.mStartTimestamp);
            }
        }

        public void recordUnlockEvent(int i, long j) {
            if (this.mUnlockEvent == null) {
                this.mUnlockEvent = new Event(i, j - this.mStartTimestamp);
            }
        }

        public void recordWakeupEvent(int i, long j) {
            if (this.mWakeupEvent == null) {
                this.mWakeupEvent = new Event(i, j - this.mStartTimestamp);
            }
        }

        public boolean hasWakeupTriggered() {
            return this.mWakeupEvent != null;
        }

        public void recordResetEvent(int i, long j) {
            if (this.mResetEvent == null) {
                this.mResetEvent = new Event(i, j - this.mStartTimestamp);
            }
        }

        public WifiMetricsProto.WifiWakeStats.Session buildProto() {
            WifiMetricsProto.WifiWakeStats.Session session = new WifiMetricsProto.WifiWakeStats.Session();
            session.startTimeMillis = this.mStartTimestamp;
            session.lockedNetworksAtStart = this.mStartNetworks;
            if (this.mInitEvent != null) {
                session.lockedNetworksAtInitialize = this.mInitializeNetworks;
                session.initializeEvent = this.mInitEvent.buildProto();
            }
            if (this.mUnlockEvent != null) {
                session.unlockEvent = this.mUnlockEvent.buildProto();
            }
            if (this.mWakeupEvent != null) {
                session.wakeupEvent = this.mWakeupEvent.buildProto();
            }
            if (this.mResetEvent != null) {
                session.resetEvent = this.mResetEvent.buildProto();
            }
            return session;
        }

        public void dump(PrintWriter printWriter) {
            printWriter.println("WifiWakeMetrics.Session:");
            printWriter.println("mStartTimestamp: " + this.mStartTimestamp);
            printWriter.println("mStartNetworks: " + this.mStartNetworks);
            printWriter.println("mInitializeNetworks: " + this.mInitializeNetworks);
            StringBuilder sb = new StringBuilder();
            sb.append("mInitEvent: ");
            sb.append(this.mInitEvent == null ? "{}" : this.mInitEvent.toString());
            printWriter.println(sb.toString());
            StringBuilder sb2 = new StringBuilder();
            sb2.append("mUnlockEvent: ");
            sb2.append(this.mUnlockEvent == null ? "{}" : this.mUnlockEvent.toString());
            printWriter.println(sb2.toString());
            StringBuilder sb3 = new StringBuilder();
            sb3.append("mWakeupEvent: ");
            sb3.append(this.mWakeupEvent == null ? "{}" : this.mWakeupEvent.toString());
            printWriter.println(sb3.toString());
            StringBuilder sb4 = new StringBuilder();
            sb4.append("mResetEvent: ");
            sb4.append(this.mResetEvent == null ? "{}" : this.mResetEvent.toString());
            printWriter.println(sb4.toString());
        }
    }

    public static class Event {
        public final long mElapsedTime;
        public final int mNumScans;

        public Event(int i, long j) {
            this.mNumScans = i;
            this.mElapsedTime = j;
        }

        public WifiMetricsProto.WifiWakeStats.Session.Event buildProto() {
            WifiMetricsProto.WifiWakeStats.Session.Event event = new WifiMetricsProto.WifiWakeStats.Session.Event();
            event.elapsedScans = this.mNumScans;
            event.elapsedTimeMillis = this.mElapsedTime;
            return event;
        }

        public String toString() {
            return "{ mNumScans: " + this.mNumScans + ", elapsedTime: " + this.mElapsedTime + " }";
        }
    }
}
