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
    private static final int TYPE_POLICY_CHANGED = 7;
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

        private Event(int i, int i2, ComponentName componentName, String str, long j, int i3, boolean z) {
            this.type = i;
            this.userId = i2;
            this.agent = componentName;
            this.elapsedTimestamp = SystemClock.elapsedRealtime();
            this.message = str;
            this.duration = j;
            this.flags = i3;
            this.managingTrust = z;
        }
    }

    public void logGrantTrust(int i, ComponentName componentName, String str, long j, int i2) {
        addEvent(new Event(0, i, componentName, str, j, i2, false));
    }

    public void logRevokeTrust(int i, ComponentName componentName) {
        addEvent(new Event(1, i, componentName, null, 0L, 0, false));
    }

    public void logTrustTimeout(int i, ComponentName componentName) {
        addEvent(new Event(2, i, componentName, null, 0L, 0, false));
    }

    public void logAgentDied(int i, ComponentName componentName) {
        addEvent(new Event(3, i, componentName, null, 0L, 0, false));
    }

    public void logAgentConnected(int i, ComponentName componentName) {
        addEvent(new Event(4, i, componentName, null, 0L, 0, false));
    }

    public void logAgentStopped(int i, ComponentName componentName) {
        addEvent(new Event(5, i, componentName, null, 0L, 0, false));
    }

    public void logManagingTrust(int i, ComponentName componentName, boolean z) {
        addEvent(new Event(6, i, componentName, null, 0L, 0, z));
    }

    public void logDevicePolicyChanged() {
        addEvent(new Event(7, -1, null, null, 0L, 0, false));
    }

    private void addEvent(Event event) {
        if (this.mEvents.size() >= 200) {
            this.mEvents.removeFirst();
        }
        this.mEvents.addLast(event);
    }

    public void dump(PrintWriter printWriter, int i, int i2, String str, boolean z) {
        Iterator<Event> itDescendingIterator = this.mEvents.descendingIterator();
        int i3 = 0;
        while (itDescendingIterator.hasNext() && i3 < i) {
            Event next = itDescendingIterator.next();
            if (i2 == -1 || i2 == next.userId || next.userId == -1) {
                printWriter.print(str);
                printWriter.printf("#%-2d %s %s: ", Integer.valueOf(i3), formatElapsed(next.elapsedTimestamp), dumpType(next.type));
                if (i2 == -1) {
                    printWriter.print("user=");
                    printWriter.print(next.userId);
                    printWriter.print(", ");
                }
                if (next.agent != null) {
                    printWriter.print("agent=");
                    if (z) {
                        printWriter.print(next.agent.flattenToShortString());
                    } else {
                        printWriter.print(getSimpleName(next.agent));
                    }
                }
                int i4 = next.type;
                if (i4 == 0) {
                    printWriter.printf(", message=\"%s\", duration=%s, flags=%s", next.message, formatDuration(next.duration), dumpGrantFlags(next.flags));
                } else if (i4 == 6) {
                    printWriter.printf(", managingTrust=" + next.managingTrust, new Object[0]);
                }
                printWriter.println();
                i3++;
            }
        }
    }

    public static String formatDuration(long j) {
        StringBuilder sb = new StringBuilder();
        TimeUtils.formatDuration(j, sb);
        return sb.toString();
    }

    private static String formatElapsed(long j) {
        return TimeUtils.logTimeOfDay((j - SystemClock.elapsedRealtime()) + System.currentTimeMillis());
    }

    static String getSimpleName(ComponentName componentName) {
        String className = componentName.getClassName();
        int iLastIndexOf = className.lastIndexOf(46);
        if (iLastIndexOf < className.length() && iLastIndexOf >= 0) {
            return className.substring(iLastIndexOf + 1);
        }
        return className;
    }

    private String dumpType(int i) {
        switch (i) {
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
            case 7:
                return "DevicePolicyChanged";
            default:
                return "Unknown(" + i + ")";
        }
    }

    private String dumpGrantFlags(int i) {
        StringBuilder sb = new StringBuilder();
        if ((i & 1) != 0) {
            if (sb.length() != 0) {
                sb.append('|');
            }
            sb.append("INITIATED_BY_USER");
        }
        if ((i & 2) != 0) {
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
