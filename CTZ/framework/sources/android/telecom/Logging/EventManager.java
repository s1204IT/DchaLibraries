package android.telecom.Logging;

import android.provider.SettingsStringUtil;
import android.telecom.Log;
import android.telecom.Logging.EventManager;
import android.telecom.Logging.SessionManager;
import android.text.TextUtils;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.ToLongFunction;

public class EventManager {

    @VisibleForTesting
    public static final int DEFAULT_EVENTS_TO_CACHE = 10;
    public static final String TAG = "Logging.Events";
    private SessionManager.ISessionIdQueryHandler mSessionIdHandler;
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final Object mSync = new Object();
    private final Map<Loggable, EventRecord> mCallEventRecordMap = new HashMap();
    private LinkedBlockingQueue<EventRecord> mEventRecords = new LinkedBlockingQueue<>(10);
    private List<EventListener> mEventListeners = new ArrayList();
    private final Map<String, List<TimedEventPair>> requestResponsePairs = new HashMap();

    public interface EventListener {
        void eventRecordAdded(EventRecord eventRecord);
    }

    public interface Loggable {
        String getDescription();

        String getId();
    }

    public static class TimedEventPair {
        private static final long DEFAULT_TIMEOUT = 3000;
        String mName;
        String mRequest;
        String mResponse;
        long mTimeoutMillis;

        public TimedEventPair(String str, String str2, String str3) {
            this.mTimeoutMillis = DEFAULT_TIMEOUT;
            this.mRequest = str;
            this.mResponse = str2;
            this.mName = str3;
        }

        public TimedEventPair(String str, String str2, String str3, long j) {
            this.mTimeoutMillis = DEFAULT_TIMEOUT;
            this.mRequest = str;
            this.mResponse = str2;
            this.mName = str3;
            this.mTimeoutMillis = j;
        }
    }

    public void addRequestResponsePair(TimedEventPair timedEventPair) {
        if (this.requestResponsePairs.containsKey(timedEventPair.mRequest)) {
            this.requestResponsePairs.get(timedEventPair.mRequest).add(timedEventPair);
            return;
        }
        ArrayList arrayList = new ArrayList();
        arrayList.add(timedEventPair);
        this.requestResponsePairs.put(timedEventPair.mRequest, arrayList);
    }

    public static class Event {
        public Object data;
        public String eventId;
        public String sessionId;
        public long time;
        public final String timestampString;

        public Event(String str, String str2, long j, Object obj) {
            this.eventId = str;
            this.sessionId = str2;
            this.time = j;
            this.timestampString = ZonedDateTime.ofInstant(Instant.ofEpochMilli(j), ZoneId.systemDefault()).format(EventManager.DATE_TIME_FORMATTER);
            this.data = obj;
        }
    }

    public class EventRecord {
        private final List<Event> mEvents = new LinkedList();
        private final Loggable mRecordEntry;

        public class EventTiming extends TimedEvent<String> {
            public String name;
            public long time;

            public EventTiming(String str, long j) {
                this.name = str;
                this.time = j;
            }

            @Override
            public String getKey() {
                return this.name;
            }

            @Override
            public long getTime() {
                return this.time;
            }
        }

        private class PendingResponse {
            String name;
            String requestEventId;
            long requestEventTimeMillis;
            long timeoutMillis;

            public PendingResponse(String str, long j, long j2, String str2) {
                this.requestEventId = str;
                this.requestEventTimeMillis = j;
                this.timeoutMillis = j2;
                this.name = str2;
            }
        }

        public EventRecord(Loggable loggable) {
            this.mRecordEntry = loggable;
        }

        public Loggable getRecordEntry() {
            return this.mRecordEntry;
        }

        public void addEvent(String str, String str2, Object obj) {
            this.mEvents.add(new Event(str, str2, System.currentTimeMillis(), obj));
            Log.i("Event", "RecordEntry %s: %s, %s", this.mRecordEntry.getId(), str, obj);
        }

        public List<Event> getEvents() {
            return this.mEvents;
        }

        public List<EventTiming> extractEventTimings() {
            if (this.mEvents == null) {
                return Collections.emptyList();
            }
            LinkedList linkedList = new LinkedList();
            HashMap map = new HashMap();
            for (Event event : this.mEvents) {
                if (EventManager.this.requestResponsePairs.containsKey(event.eventId)) {
                    for (TimedEventPair timedEventPair : (List) EventManager.this.requestResponsePairs.get(event.eventId)) {
                        map.put(timedEventPair.mResponse, new PendingResponse(event.eventId, event.time, timedEventPair.mTimeoutMillis, timedEventPair.mName));
                    }
                }
                PendingResponse pendingResponse = (PendingResponse) map.remove(event.eventId);
                if (pendingResponse != null) {
                    long j = event.time - pendingResponse.requestEventTimeMillis;
                    if (j < pendingResponse.timeoutMillis) {
                        linkedList.add(new EventTiming(pendingResponse.name, j));
                    }
                }
            }
            return linkedList;
        }

        public void dump(IndentingPrintWriter indentingPrintWriter) {
            EventRecord eventRecord;
            indentingPrintWriter.print(this.mRecordEntry.getDescription());
            indentingPrintWriter.increaseIndent();
            for (Event event : this.mEvents) {
                indentingPrintWriter.print(event.timestampString);
                indentingPrintWriter.print(" - ");
                indentingPrintWriter.print(event.eventId);
                if (event.data != null) {
                    indentingPrintWriter.print(" (");
                    Object obj = event.data;
                    if ((obj instanceof Loggable) && (eventRecord = (EventRecord) EventManager.this.mCallEventRecordMap.get(obj)) != null) {
                        obj = "RecordEntry " + eventRecord.mRecordEntry.getId();
                    }
                    indentingPrintWriter.print(obj);
                    indentingPrintWriter.print(")");
                }
                if (!TextUtils.isEmpty(event.sessionId)) {
                    indentingPrintWriter.print(SettingsStringUtil.DELIMITER);
                    indentingPrintWriter.print(event.sessionId);
                }
                indentingPrintWriter.println();
            }
            indentingPrintWriter.println("Timings (average for this call, milliseconds):");
            indentingPrintWriter.increaseIndent();
            Map mapAverageTimings = EventTiming.averageTimings(extractEventTimings());
            ArrayList<String> arrayList = new ArrayList(mapAverageTimings.keySet());
            Collections.sort(arrayList);
            for (String str : arrayList) {
                indentingPrintWriter.printf("%s: %.2f\n", str, mapAverageTimings.get(str));
            }
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.decreaseIndent();
        }
    }

    public EventManager(SessionManager.ISessionIdQueryHandler iSessionIdQueryHandler) {
        this.mSessionIdHandler = iSessionIdQueryHandler;
    }

    public void event(Loggable loggable, String str, Object obj) {
        String sessionId = this.mSessionIdHandler.getSessionId();
        if (loggable == null) {
            Log.i(TAG, "Non-call EVENT: %s, %s", str, obj);
            return;
        }
        synchronized (this.mEventRecords) {
            if (!this.mCallEventRecordMap.containsKey(loggable)) {
                addEventRecord(new EventRecord(loggable));
            }
            this.mCallEventRecordMap.get(loggable).addEvent(str, sessionId, obj);
        }
    }

    public void event(Loggable loggable, String str, String str2, Object... objArr) {
        if (objArr != null) {
            try {
                if (objArr.length != 0) {
                    str2 = String.format(Locale.US, str2, objArr);
                }
            } catch (IllegalFormatException e) {
                Log.e(this, e, "IllegalFormatException: formatString='%s' numArgs=%d", str2, Integer.valueOf(objArr.length));
                str2 = str2 + " (An error occurred while formatting the message.)";
            }
        }
        event(loggable, str, str2);
    }

    public void dumpEvents(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println("Historical Events:");
        indentingPrintWriter.increaseIndent();
        Iterator<EventRecord> it = this.mEventRecords.iterator();
        while (it.hasNext()) {
            it.next().dump(indentingPrintWriter);
        }
        indentingPrintWriter.decreaseIndent();
    }

    public void dumpEventsTimeline(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println("Historical Events (sorted by time):");
        ArrayList<Pair> arrayList = new ArrayList();
        for (EventRecord eventRecord : this.mEventRecords) {
            Iterator<Event> it = eventRecord.getEvents().iterator();
            while (it.hasNext()) {
                arrayList.add(new Pair(eventRecord.getRecordEntry(), it.next()));
            }
        }
        arrayList.sort(Comparator.comparingLong(new ToLongFunction() {
            @Override
            public final long applyAsLong(Object obj) {
                return ((EventManager.Event) ((Pair) obj).second).time;
            }
        }));
        indentingPrintWriter.increaseIndent();
        for (Pair pair : arrayList) {
            indentingPrintWriter.print(((Event) pair.second).timestampString);
            indentingPrintWriter.print(",");
            indentingPrintWriter.print(((Loggable) pair.first).getId());
            indentingPrintWriter.print(",");
            indentingPrintWriter.print(((Event) pair.second).eventId);
            indentingPrintWriter.print(",");
            indentingPrintWriter.println(((Event) pair.second).data);
        }
        indentingPrintWriter.decreaseIndent();
    }

    public void changeEventCacheSize(int i) {
        LinkedBlockingQueue<EventRecord> linkedBlockingQueue = this.mEventRecords;
        this.mEventRecords = new LinkedBlockingQueue<>(i);
        this.mCallEventRecordMap.clear();
        linkedBlockingQueue.forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                EventManager.lambda$changeEventCacheSize$1(this.f$0, (EventManager.EventRecord) obj);
            }
        });
    }

    public static void lambda$changeEventCacheSize$1(EventManager eventManager, EventRecord eventRecord) {
        EventRecord eventRecordPoll;
        Loggable recordEntry = eventRecord.getRecordEntry();
        if (eventManager.mEventRecords.remainingCapacity() == 0 && (eventRecordPoll = eventManager.mEventRecords.poll()) != null) {
            eventManager.mCallEventRecordMap.remove(eventRecordPoll.getRecordEntry());
        }
        eventManager.mEventRecords.add(eventRecord);
        eventManager.mCallEventRecordMap.put(recordEntry, eventRecord);
    }

    public void registerEventListener(EventListener eventListener) {
        if (eventListener != null) {
            synchronized (mSync) {
                this.mEventListeners.add(eventListener);
            }
        }
    }

    @VisibleForTesting
    public LinkedBlockingQueue<EventRecord> getEventRecords() {
        return this.mEventRecords;
    }

    @VisibleForTesting
    public Map<Loggable, EventRecord> getCallEventRecordMap() {
        return this.mCallEventRecordMap;
    }

    private void addEventRecord(EventRecord eventRecord) {
        EventRecord eventRecordPoll;
        Loggable recordEntry = eventRecord.getRecordEntry();
        if (this.mEventRecords.remainingCapacity() == 0 && (eventRecordPoll = this.mEventRecords.poll()) != null) {
            this.mCallEventRecordMap.remove(eventRecordPoll.getRecordEntry());
        }
        this.mEventRecords.add(eventRecord);
        this.mCallEventRecordMap.put(recordEntry, eventRecord);
        synchronized (mSync) {
            Iterator<EventListener> it = this.mEventListeners.iterator();
            while (it.hasNext()) {
                it.next().eventRecordAdded(eventRecord);
            }
        }
    }
}
