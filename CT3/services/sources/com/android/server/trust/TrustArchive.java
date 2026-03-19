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
        final int flags;
        final boolean managingTrust;
        final String message;
        final int type;
        final int userId;

        Event(int type, int userId, ComponentName agent, String message, long duration, int flags, boolean managingTrust, Event event) {
            this(type, userId, agent, message, duration, flags, managingTrust);
        }

        private Event(int type, int userId, ComponentName agent, String message, long duration, int flags, boolean managingTrust) {
            this.type = type;
            this.userId = userId;
            this.agent = agent;
            this.elapsedTimestamp = SystemClock.elapsedRealtime();
            this.message = message;
            this.duration = duration;
            this.flags = flags;
            this.managingTrust = managingTrust;
        }
    }

    public void logGrantTrust(int userId, ComponentName agent, String message, long duration, int flags) {
        addEvent(new Event(0, userId, agent, message, duration, flags, false, null));
    }

    public void logRevokeTrust(int i, ComponentName componentName) {
        int i2 = 1;
        addEvent(new Event(i2, i, componentName, null, 0L, 0, 0 == true ? 1 : 0, 0 == true ? 1 : 0));
    }

    public void logTrustTimeout(int i, ComponentName componentName) {
        int i2 = 2;
        addEvent(new Event(i2, i, componentName, null, 0L, 0, 0 == true ? 1 : 0, 0 == true ? 1 : 0));
    }

    public void logAgentDied(int i, ComponentName componentName) {
        int i2 = 3;
        addEvent(new Event(i2, i, componentName, null, 0L, 0, 0 == true ? 1 : 0, 0 == true ? 1 : 0));
    }

    public void logAgentConnected(int i, ComponentName componentName) {
        int i2 = 4;
        addEvent(new Event(i2, i, componentName, null, 0L, 0, 0 == true ? 1 : 0, 0 == true ? 1 : 0));
    }

    public void logAgentStopped(int i, ComponentName componentName) {
        int i2 = 5;
        addEvent(new Event(i2, i, componentName, null, 0L, 0, 0 == true ? 1 : 0, 0 == true ? 1 : 0));
    }

    public void logManagingTrust(int i, ComponentName componentName, boolean z) {
        addEvent(new Event(6, i, componentName, null, 0L, 0, z, 0 == true ? 1 : 0));
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
                        writer.printf(", message=\"%s\", duration=%s, flags=%s", ev.message, formatDuration(ev.duration), dumpGrantFlags(ev.flags));
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

    private String dumpGrantFlags(int flags) {
        StringBuilder sb = new StringBuilder();
        if ((flags & 1) != 0) {
            if (sb.length() != 0) {
                sb.append('|');
            }
            sb.append("INITIATED_BY_USER");
        }
        if ((flags & 2) != 0) {
            if (sb.length() != 0) {
                sb.append('|');
            }
            sb.append("DISMISS_KEYGUARD");
        }
        if (sb.length() == 0) {
            sb.append('0');
        }
        return sb.toString();
    }
}
