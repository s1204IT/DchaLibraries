package com.android.server.telecom;

import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.telecom.Log;
import android.telecom.Logging.EventManager;
import android.telecom.Logging.SessionManager;
import com.android.server.telecom.Analytics;

public class LogUtils {

    public static final class Events {

        public static class Timings {
            private static final EventManager.TimedEventPair[] sTimedEvents = {new EventManager.TimedEventPair("REQUEST_ACCEPT", "SET_ACTIVE", "accept"), new EventManager.TimedEventPair("REQUEST_REJECT", "SET_DISCONNECTED", "reject"), new EventManager.TimedEventPair("REQUEST_DISCONNECT", "SET_DISCONNECTED", "disconnect"), new EventManager.TimedEventPair("REQUEST_HOLD", "SET_HOLD", "hold"), new EventManager.TimedEventPair("REQUEST_UNHOLD", "SET_ACTIVE", "unhold"), new EventManager.TimedEventPair("START_CONNECTION", "SET_DIALING", "outgoing_time_to_dialing"), new EventManager.TimedEventPair("BIND_CS", "CS_BOUND", "bind_cs"), new EventManager.TimedEventPair("SCREENING_SENT", "SCREENING_COMPLETED", "screening_completed"), new EventManager.TimedEventPair("DIRECT_TO_VM_INITIATED", "DIRECT_TO_VM_FINISHED", "direct_to_vm_finished"), new EventManager.TimedEventPair("BLOCK_CHECK_INITIATED", "BLOCK_CHECK_FINISHED", "block_check_finished"), new EventManager.TimedEventPair("FILTERING_INITIATED", "FILTERING_COMPLETED", "filtering_completed"), new EventManager.TimedEventPair("FILTERING_INITIATED", "FILTERING_TIMED_OUT", "filtering_timed_out", 6000)};
        }
    }

    private static void eventRecordAdded(EventManager.EventRecord eventRecord) {
        EventManager.Loggable recordEntry = eventRecord.getRecordEntry();
        if (recordEntry instanceof Call) {
            Call call = (Call) recordEntry;
            Log.i("LogUtils", "EventRecord added as Call: " + call, new Object[0]);
            Analytics.CallInfo analytics = call.getAnalytics();
            if (analytics != null) {
                analytics.setCallEvents(eventRecord);
                return;
            } else {
                Log.w("LogUtils", "Could not get Analytics CallInfo.", new Object[0]);
                return;
            }
        }
        Log.w("LogUtils", "Non-Call EventRecord Added.", new Object[0]);
    }

    public static void initLogging(Context context) {
        Log.setTag("Telecom");
        setupLoggableFlags();
        Log.setSessionContext(context);
        for (EventManager.TimedEventPair timedEventPair : Events.Timings.sTimedEvents) {
            Log.addRequestResponsePair(timedEventPair);
        }
        Log.registerEventListener(new EventManager.EventListener() {
            public final void eventRecordAdded(EventManager.EventRecord eventRecord) {
                LogUtils.eventRecordAdded(eventRecord);
            }
        });
        Log.registerSessionListener(new SessionManager.ISessionListener() {
            public final void sessionComplete(String str, long j) {
                Analytics.addSessionTiming(str, j);
            }
        });
    }

    private static void setupLoggableFlags() {
        Log.ERROR = true;
        Log.WARN = true;
        Log.INFO = true;
        if (Build.IS_ENG || SystemProperties.getInt("persist.vendor.log.tel_dbg", 0) > 0) {
            Log.DEBUG = true;
            Log.VERBOSE = true;
        }
    }
}
