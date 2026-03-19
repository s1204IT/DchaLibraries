package com.android.server.connectivity;

import android.app.PendingIntent;
import android.content.Context;
import android.net.ConnectivityMetricsEvent;
import android.net.IConnectivityMetricsLogger;
import android.os.Binder;
import android.os.Parcel;
import android.text.format.DateUtils;
import android.util.Log;
import com.android.server.SystemService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;

public class MetricsLoggerService extends SystemService {
    private static final boolean DBG = true;
    private static String TAG = "ConnectivityMetricsLoggerService";
    private static final boolean VDBG = false;
    private final int EVENTS_NOTIFICATION_THRESHOLD;
    private final int MAX_NUMBER_OF_EVENTS;
    private final int THROTTLING_MAX_NUMBER_OF_MESSAGES_PER_COMPONENT;
    private final int THROTTLING_TIME_INTERVAL_MILLIS;
    private final IConnectivityMetricsLogger.Stub mBinder;
    private DnsEventListenerService mDnsListener;
    private int mEventCounter;
    private final ArrayDeque<ConnectivityMetricsEvent> mEvents;
    private long mLastEventReference;
    private final int[] mThrottlingCounters;
    private long mThrottlingIntervalBoundaryMillis;

    public MetricsLoggerService(Context context) {
        super(context);
        this.MAX_NUMBER_OF_EVENTS = 1000;
        this.EVENTS_NOTIFICATION_THRESHOLD = 300;
        this.THROTTLING_TIME_INTERVAL_MILLIS = 3600000;
        this.THROTTLING_MAX_NUMBER_OF_MESSAGES_PER_COMPONENT = 1000;
        this.mEventCounter = 0;
        this.mLastEventReference = 0L;
        this.mThrottlingCounters = new int[5];
        this.mEvents = new ArrayDeque<>();
        this.mBinder = new IConnectivityMetricsLogger.Stub() {
            private final ArrayList<PendingIntent> mPendingIntents = new ArrayList<>();

            protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
                if (MetricsLoggerService.this.getContext().checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                    pw.println("Permission Denial: can't dump ConnectivityMetricsLoggerService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
                    return;
                }
                boolean dumpSerializedSize = false;
                boolean dumpEvents = false;
                boolean dumpDebugInfo = false;
                for (String arg : args) {
                    if (arg.equals("--debug")) {
                        dumpDebugInfo = true;
                    } else if (arg.equals("--events")) {
                        dumpEvents = true;
                    } else if (arg.equals("--size")) {
                        dumpSerializedSize = true;
                    } else if (arg.equals("--all")) {
                        dumpDebugInfo = true;
                        dumpEvents = true;
                        dumpSerializedSize = true;
                    }
                }
                synchronized (MetricsLoggerService.this.mEvents) {
                    pw.println("Number of events: " + MetricsLoggerService.this.mEvents.size());
                    pw.println("Counter: " + MetricsLoggerService.this.mEventCounter);
                    if (MetricsLoggerService.this.mEvents.size() > 0) {
                        pw.println("Time span: " + DateUtils.formatElapsedTime((System.currentTimeMillis() - ((ConnectivityMetricsEvent) MetricsLoggerService.this.mEvents.peekFirst()).timestamp) / 1000));
                    }
                    if (dumpSerializedSize) {
                        Parcel p = Parcel.obtain();
                        for (ConnectivityMetricsEvent e : MetricsLoggerService.this.mEvents) {
                            p.writeParcelable(e, 0);
                        }
                        pw.println("Serialized data size: " + p.dataSize());
                        p.recycle();
                    }
                    if (dumpEvents) {
                        pw.println();
                        pw.println("Events:");
                        for (ConnectivityMetricsEvent e2 : MetricsLoggerService.this.mEvents) {
                            pw.println(e2.toString());
                        }
                    }
                }
                if (dumpDebugInfo) {
                    synchronized (MetricsLoggerService.this.mThrottlingCounters) {
                        pw.println();
                        for (int i = 0; i < 5; i++) {
                            if (MetricsLoggerService.this.mThrottlingCounters[i] > 0) {
                                pw.println("Throttling Counter #" + i + ": " + MetricsLoggerService.this.mThrottlingCounters[i]);
                            }
                        }
                        pw.println("Throttling Time Remaining: " + DateUtils.formatElapsedTime((MetricsLoggerService.this.mThrottlingIntervalBoundaryMillis - System.currentTimeMillis()) / 1000));
                    }
                }
                synchronized (this.mPendingIntents) {
                    if (!this.mPendingIntents.isEmpty()) {
                        pw.println();
                        pw.println("Pending intents:");
                        for (PendingIntent pi : this.mPendingIntents) {
                            pw.println(pi.toString());
                        }
                    }
                }
                pw.println();
                MetricsLoggerService.this.mDnsListener.dump(pw);
            }

            public long logEvent(ConnectivityMetricsEvent event) {
                ConnectivityMetricsEvent[] events = {event};
                return logEvents(events);
            }

            public long logEvents(ConnectivityMetricsEvent[] events) {
                MetricsLoggerService.this.enforceConnectivityInternalPermission();
                if (events == null || events.length == 0) {
                    Log.wtf(MetricsLoggerService.TAG, "No events passed to logEvents()");
                    return -1L;
                }
                int componentTag = events[0].componentTag;
                if (componentTag < 0 || componentTag >= 5) {
                    Log.wtf(MetricsLoggerService.TAG, "Unexpected tag: " + componentTag);
                    return -1L;
                }
                synchronized (MetricsLoggerService.this.mThrottlingCounters) {
                    long currentTimeMillis = System.currentTimeMillis();
                    if (currentTimeMillis > MetricsLoggerService.this.mThrottlingIntervalBoundaryMillis) {
                        MetricsLoggerService.this.resetThrottlingCounters(currentTimeMillis);
                    }
                    int[] iArr = MetricsLoggerService.this.mThrottlingCounters;
                    iArr[componentTag] = iArr[componentTag] + events.length;
                    if (MetricsLoggerService.this.mThrottlingCounters[componentTag] > 1000) {
                        Log.w(MetricsLoggerService.TAG, "Too many events from #" + componentTag + ". Block until " + MetricsLoggerService.this.mThrottlingIntervalBoundaryMillis);
                        return MetricsLoggerService.this.mThrottlingIntervalBoundaryMillis;
                    }
                    boolean sendPendingIntents = false;
                    synchronized (MetricsLoggerService.this.mEvents) {
                        for (ConnectivityMetricsEvent e : events) {
                            if (e.componentTag != componentTag) {
                                Log.wtf(MetricsLoggerService.TAG, "Unexpected tag: " + e.componentTag);
                                return -1L;
                            }
                            MetricsLoggerService.this.addEvent(e);
                        }
                        MetricsLoggerService.this.mLastEventReference += (long) events.length;
                        MetricsLoggerService.this.mEventCounter += events.length;
                        if (MetricsLoggerService.this.mEventCounter >= 300) {
                            MetricsLoggerService.this.mEventCounter = 0;
                            sendPendingIntents = true;
                        }
                        if (sendPendingIntents) {
                            synchronized (this.mPendingIntents) {
                                for (PendingIntent pi : this.mPendingIntents) {
                                    try {
                                        pi.send(MetricsLoggerService.this.getContext(), 0, null, null, null);
                                    } catch (PendingIntent.CanceledException e2) {
                                        Log.e(MetricsLoggerService.TAG, "Pending intent canceled: " + pi);
                                        this.mPendingIntents.remove(pi);
                                    }
                                }
                            }
                            return 0L;
                        }
                        return 0L;
                    }
                }
            }

            public ConnectivityMetricsEvent[] getEvents(ConnectivityMetricsEvent.Reference reference) {
                int i;
                MetricsLoggerService.this.enforceDumpPermission();
                long ref = reference.getValue();
                synchronized (MetricsLoggerService.this.mEvents) {
                    if (ref > MetricsLoggerService.this.mLastEventReference) {
                        Log.e(MetricsLoggerService.TAG, "Invalid reference");
                        reference.setValue(MetricsLoggerService.this.mLastEventReference);
                        return null;
                    }
                    if (ref < MetricsLoggerService.this.mLastEventReference - ((long) MetricsLoggerService.this.mEvents.size())) {
                        ref = MetricsLoggerService.this.mLastEventReference - ((long) MetricsLoggerService.this.mEvents.size());
                    }
                    int numEventsToSkip = MetricsLoggerService.this.mEvents.size() - ((int) (MetricsLoggerService.this.mLastEventReference - ref));
                    ConnectivityMetricsEvent[] result = new ConnectivityMetricsEvent[MetricsLoggerService.this.mEvents.size() - numEventsToSkip];
                    int i2 = 0;
                    for (ConnectivityMetricsEvent e : MetricsLoggerService.this.mEvents) {
                        if (numEventsToSkip > 0) {
                            numEventsToSkip--;
                            i = i2;
                        } else {
                            i = i2 + 1;
                            result[i2] = e;
                        }
                        i2 = i;
                    }
                    reference.setValue(MetricsLoggerService.this.mLastEventReference);
                    return result;
                }
            }

            public boolean register(PendingIntent newEventsIntent) {
                MetricsLoggerService.this.enforceDumpPermission();
                synchronized (this.mPendingIntents) {
                    if (this.mPendingIntents.remove(newEventsIntent)) {
                        Log.w(MetricsLoggerService.TAG, "Replacing registered pending intent");
                    }
                    this.mPendingIntents.add(newEventsIntent);
                }
                return true;
            }

            public void unregister(PendingIntent newEventsIntent) {
                MetricsLoggerService.this.enforceDumpPermission();
                synchronized (this.mPendingIntents) {
                    if (!this.mPendingIntents.remove(newEventsIntent)) {
                        Log.e(MetricsLoggerService.TAG, "Pending intent is not registered");
                    }
                }
            }
        };
    }

    @Override
    public void onStart() {
        resetThrottlingCounters(System.currentTimeMillis());
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase != 500) {
            return;
        }
        Log.d(TAG, "onBootPhase: PHASE_SYSTEM_SERVICES_READY");
        publishBinderService("connectivity_metrics_logger", this.mBinder);
        this.mDnsListener = new DnsEventListenerService(getContext());
        publishBinderService(DnsEventListenerService.SERVICE_NAME, this.mDnsListener);
    }

    private void enforceConnectivityInternalPermission() {
        getContext().enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", "MetricsLoggerService");
    }

    private void enforceDumpPermission() {
        getContext().enforceCallingOrSelfPermission("android.permission.DUMP", "MetricsLoggerService");
    }

    private void resetThrottlingCounters(long currentTimeMillis) {
        synchronized (this.mThrottlingCounters) {
            for (int i = 0; i < this.mThrottlingCounters.length; i++) {
                this.mThrottlingCounters[i] = 0;
            }
            this.mThrottlingIntervalBoundaryMillis = 3600000 + currentTimeMillis;
        }
    }

    private void addEvent(ConnectivityMetricsEvent e) {
        while (this.mEvents.size() >= 1000) {
            this.mEvents.removeFirst();
        }
        this.mEvents.addLast(e);
    }
}
