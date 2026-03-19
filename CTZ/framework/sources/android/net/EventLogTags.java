package android.net;

import android.util.EventLog;

public class EventLogTags {
    public static final int NTP_FAILURE = 50081;
    public static final int NTP_SUCCESS = 50080;

    private EventLogTags() {
    }

    public static void writeNtpSuccess(String str, long j, long j2) {
        EventLog.writeEvent(NTP_SUCCESS, str, Long.valueOf(j), Long.valueOf(j2));
    }

    public static void writeNtpFailure(String str, String str2) {
        EventLog.writeEvent(NTP_FAILURE, str, str2);
    }
}
