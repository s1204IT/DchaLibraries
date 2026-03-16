package com.android.server.trust;

import android.content.ComponentName;
import android.os.SystemClock;
import android.util.TimeUtils;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Iterator;

public class TrustArchive {
    private static final int HISTORY_LIMIT = 200;
    private static final int TYPE_AGENT_CONNECTED = 4;
    private static final int TYPE_AGENT_DIED = 3;
    private static final int TYPE_AGENT_STOPPED = 5;
    private static final int TYPE_GRANT_TRUST = 0;
    private static final int TYPE_MANAGING_TRUST = 6;
    private static final int TYPE_REVOKE_TRUST = 1;
    private static final int TYPE_TRUST_TIMEOUT = 2;
    ArrayDeque<Event> mEvents = new ArrayDeque<>();

    private static class Event {
        final ComponentName agent;
        final long duration;
        final long elapsedTimestamp;
        final boolean managingTrust;
        final String message;
        final int type;
        final int userId;
        final boolean userInitiated;

        private Event(int type, int userId, ComponentName agent, String message, long duration, boolean userInitiated, boolean managingTrust) {
            this.type = type;
            this.userId = userId;
            this.agent = agent;
            this.elapsedTimestamp = SystemClock.elapsedRealtime();
            this.message = message;
            this.duration = duration;
            this.userInitiated = userInitiated;
            this.managingTrust = managingTrust;
        }
    }

    public void logGrantTrust(int userId, ComponentName agent, String message, long duration, boolean userInitiated) {
        addEvent(new Event(0, userId, agent, message, duration, userInitiated, false));
    }

    public void logRevokeTrust(int i, ComponentName componentName) {
        boolean z = false;
        addEvent(new Event(1, i, componentName, null, 0L, z, z));
    }

    public void logTrustTimeout(int i, ComponentName componentName) {
        boolean z = false;
        addEvent(new Event(2, i, componentName, null, 0L, z, z));
    }

    public void logAgentDied(int i, ComponentName componentName) {
        boolean z = false;
        addEvent(new Event(3, i, componentName, null, 0L, z, z));
    }

    public void logAgentConnected(int i, ComponentName componentName) {
        boolean z = false;
        addEvent(new Event(4, i, componentName, null, 0L, z, z));
    }

    public void logAgentStopped(int i, ComponentName componentName) {
        boolean z = false;
        addEvent(new Event(5, i, componentName, null, 0L, z, z));
    }

    public void logManagingTrust(int i, ComponentName componentName, boolean z) {
        addEvent(new Event(6, i, componentName, null, 0L, false, z));
    }

    private void addEvent(Event e) {
        if (this.mEvents.size() >= HISTORY_LIMIT) {
            this.mEvents.removeFirst();
        }
        this.mEvents.addLast(e);
    }

    public void dump(PrintWriter writer, int limit, int userId, String linePrefix, boolean duplicateSimpleNames) {
        int count = 0;
        Iterator<Event> iter = this.mEvents.descendingIterator();
        while (iter.hasNext() && count < limit) {
            Event ev = iter.next();
            if (userId == -1 || userId == ev.userId) {
                writer.print(linePrefix);
                writer.printf("#%-2d %s %s: ", Integer.valueOf(count), formatElapsed(ev.elapsedTimestamp), dumpType(ev.type));
                if (userId == -1) {
                    writer.print("user=");
                    writer.print(ev.userId);
                    writer.print(", ");
                }
                writer.print("agent=");
                if (duplicateSimpleNames) {
                    writer.print(ev.agent.flattenToShortString());
                } else {
                    writer.print(getSimpleName(ev.agent));
                }
                switch (ev.type) {
                    case 0:
                        Object[] objArr = new Object[3];
                        objArr[0] = ev.message;
                        objArr[1] = formatDuration(ev.duration);
                        objArr[2] = Integer.valueOf(ev.userInitiated ? 1 : 0);
                        writer.printf(", message=\"%s\", duration=%s, initiatedByUser=%d", objArr);
                        break;
                    case 6:
                        writer.printf(", managingTrust=" + ev.managingTrust, new Object[0]);
                        break;
                }
                writer.println();
                count++;
            }
        }
    }

    public static String formatDuration(long duration) {
        StringBuilder sb = new StringBuilder();
        TimeUtils.formatDuration(duration, sb);
        return sb.toString();
    }

    private static String formatElapsed(long elapsed) {
        long delta = elapsed - SystemClock.elapsedRealtime();
        long wallTime = delta + System.currentTimeMillis();
        return TimeUtils.logTimeOfDay(wallTime);
    }

    static String getSimpleName(ComponentName cn) {
        String name = cn.getClassName();
        int idx = name.lastIndexOf(46);
        if (idx < name.length() && idx >= 0) {
            return name.substring(idx + 1);
        }
        return name;
    }

    private String dumpType(int type) {
        switch (type) {
            case 0:
                return "GrantTrust";
            case 1:
                return "RevokeTrust";
            case 2:
                return "TrustTimeout";
            case 3:
                return "AgentDied";
            case 4:
                return "AgentConnected";
            case 5:
                return "AgentStopped";
            case 6:
                return "ManagingTrust";
            default:
                return "Unknown(" + type + ")";
        }
    }
}
