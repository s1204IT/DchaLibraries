package com.android.server.telecom;

import android.telecom.DisconnectCause;
import android.telecom.Log;
import android.telecom.Logging.EventManager;
import android.telecom.ParcelableCallAnalytics;
import android.telecom.TelecomAnalytics;
import android.util.Base64;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.telecom.Analytics;
import com.android.server.telecom.nano.TelecomLogClass;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import mediatek.telecom.MtkConnection;

public class Analytics {
    private static final LinkedList<String> sActiveCallIds;
    private static final Map<String, CallInfoImpl> sCallIdToInfo;
    private static final Object sLock;
    private static final List<TelecomAnalytics.SessionTiming> sSessionTimings;
    public static final Map<String, Integer> sLogEventToAnalyticsEvent = new HashMap<String, Integer>() {
        {
            put("SET_SELECT_PHONE_ACCOUNT", 0);
            put("REQUEST_HOLD", 400);
            put("REQUEST_UNHOLD", 401);
            put("SWAP", 405);
            put("SKIP_RINGING", 200);
            put("CONF_WITH", 300);
            put("CONF_SPLIT", 301);
            put("SET_PARENT", 302);
            put("MUTE", 202);
            put("UNMUTE", 203);
            put("AUDIO_ROUTE_BT", 204);
            put("AUDIO_ROUTE_EARPIECE", 205);
            put("AUDIO_ROUTE_HEADSET", 206);
            put("AUDIO_ROUTE_SPEAKER", 207);
            put("SILENCE", 201);
            put("SCREENING_COMPLETED", 101);
            put("BLOCK_CHECK_FINISHED", 105);
            put("DIRECT_TO_VM_FINISHED", 103);
            put("REMOTELY_HELD", 402);
            put("REMOTELY_UNHELD", 403);
            put("PULL", 500);
            put("REQUEST_ACCEPT", 7);
            put("REQUEST_REJECT", 8);
            put("SET_ACTIVE", 1);
            put("SET_DISCONNECTED", 2);
            put("SET_HOLD", 404);
            put("SET_DIALING", 4);
            put("START_CONNECTION", 3);
            put("BIND_CS", 5);
            put("CS_BOUND", 6);
            put("SCREENING_SENT", 100);
            put("DIRECT_TO_VM_INITIATED", 102);
            put("BLOCK_CHECK_INITIATED", 104);
            put("FILTERING_INITIATED", 106);
            put("FILTERING_COMPLETED", 107);
            put("FILTERING_TIMED_OUT", 108);
        }
    };
    public static final Map<String, Integer> sLogSessionToSessionId = new HashMap<String, Integer>() {
        {
            put("ICA.aC", 1);
            put("ICA.rC", 2);
            put("ICA.dC", 3);
            put("ICA.hC", 4);
            put("ICA.uC", 5);
            put("ICA.m", 6);
            put("ICA.sAR", 7);
            put("ICA.c", 8);
            put("CSW.hCCC", 100);
            put("CSW.sA", 101);
            put("CSW.sR", 102);
            put("CSW.sD", 103);
            put("CSW.sDc", 104);
            put("CSW.sOH", 105);
            put("CSW.rC", 106);
            put("CSW.sIC", 107);
            put("CSW.aCC", 108);
        }
    };
    public static final Map<String, Integer> sLogEventTimingToAnalyticsEventTiming = new HashMap<String, Integer>() {
        {
            put("accept", 0);
            put("reject", 1);
            put("disconnect", 2);
            put("hold", 3);
            put("unhold", 4);
            put("outgoing_time_to_dialing", 5);
            put("bind_cs", 6);
            put("screening_completed", 7);
            put("direct_to_vm_finished", 8);
            put("block_check_finished", 9);
            put("filtering_completed", 10);
            put("filtering_timed_out", 11);
        }
    };
    public static final Map<Integer, String> sSessionIdToLogSession = new HashMap();

    static {
        for (Map.Entry<String, Integer> entry : sLogSessionToSessionId.entrySet()) {
            sSessionIdToLogSession.put(entry.getValue(), entry.getKey());
        }
        sLock = new Object();
        sCallIdToInfo = new HashMap();
        sActiveCallIds = new LinkedList<>();
        sSessionTimings = new LinkedList();
    }

    public static class CallInfo {
        public void setCallStartTime(long j) {
        }

        public void setCallEndTime(long j) {
        }

        public void setCallIsAdditional(boolean z) {
        }

        public void setCallIsInterrupted(boolean z) {
        }

        public void setCallDisconnectCause(DisconnectCause disconnectCause) {
        }

        public void addCallTechnology(int i) {
        }

        public void setCreatedFromExistingConnection(boolean z) {
        }

        public void setCallConnectionService(String str) {
        }

        public void setCallEvents(EventManager.EventRecord eventRecord) {
        }

        public void setCallIsVideo(boolean z) {
        }

        public void addVideoEvent(int i, int i2) {
        }

        public void addInCallService(String str, int i) {
        }

        public void addCallProperties(int i) {
        }
    }

    @VisibleForTesting
    public static class CallInfoImpl extends CallInfo {
        public int callDirection;
        public EventManager.EventRecord callEvents;
        public String callId;
        public int callProperties;
        public int callTechnologies;
        public DisconnectCause callTerminationReason;
        public String connectionService;
        public boolean createdFromExistingConnection;
        public long endTime;
        public List<TelecomLogClass.InCallServiceInfo> inCallServiceInfos;
        public boolean isAdditionalCall;
        public boolean isEmergency;
        public boolean isInterrupted;
        public boolean isVideo;
        private long mTimeOfLastVideoEvent;
        public long startTime;
        public List<TelecomLogClass.VideoEvent> videoEvents;

        CallInfoImpl(String str, int i) {
            this.isAdditionalCall = false;
            this.isInterrupted = false;
            this.createdFromExistingConnection = false;
            this.isEmergency = false;
            this.isVideo = false;
            this.callProperties = 0;
            this.mTimeOfLastVideoEvent = -1L;
            this.callId = str;
            this.startTime = 0L;
            this.endTime = 0L;
            this.callDirection = i;
            this.callTechnologies = 0;
            this.connectionService = "";
            this.videoEvents = new LinkedList();
            this.inCallServiceInfos = new LinkedList();
        }

        CallInfoImpl(CallInfoImpl callInfoImpl) {
            this.isAdditionalCall = false;
            this.isInterrupted = false;
            this.createdFromExistingConnection = false;
            this.isEmergency = false;
            this.isVideo = false;
            this.callProperties = 0;
            this.mTimeOfLastVideoEvent = -1L;
            this.callId = callInfoImpl.callId;
            this.startTime = callInfoImpl.startTime;
            this.endTime = callInfoImpl.endTime;
            this.callDirection = callInfoImpl.callDirection;
            this.isAdditionalCall = callInfoImpl.isAdditionalCall;
            this.isInterrupted = callInfoImpl.isInterrupted;
            this.callTechnologies = callInfoImpl.callTechnologies;
            this.createdFromExistingConnection = callInfoImpl.createdFromExistingConnection;
            this.connectionService = callInfoImpl.connectionService;
            this.isEmergency = callInfoImpl.isEmergency;
            this.callEvents = callInfoImpl.callEvents;
            this.isVideo = callInfoImpl.isVideo;
            this.videoEvents = callInfoImpl.videoEvents;
            this.callProperties = callInfoImpl.callProperties;
            if (callInfoImpl.callTerminationReason != null) {
                this.callTerminationReason = new DisconnectCause(callInfoImpl.callTerminationReason.getCode(), callInfoImpl.callTerminationReason.getLabel(), callInfoImpl.callTerminationReason.getDescription(), callInfoImpl.callTerminationReason.getReason(), callInfoImpl.callTerminationReason.getTone());
            } else {
                this.callTerminationReason = null;
            }
        }

        @Override
        public void setCallStartTime(long j) {
            Log.d("TelecomAnalytics", "setting startTime for call " + this.callId + " to " + j, new Object[0]);
            this.startTime = j;
        }

        @Override
        public void setCallEndTime(long j) {
            Log.d("TelecomAnalytics", "setting endTime for call " + this.callId + " to " + j, new Object[0]);
            this.endTime = j;
        }

        @Override
        public void setCallIsAdditional(boolean z) {
            Log.d("TelecomAnalytics", "setting isAdditional for call " + this.callId + " to " + z, new Object[0]);
            this.isAdditionalCall = z;
        }

        @Override
        public void setCallIsInterrupted(boolean z) {
            Log.d("TelecomAnalytics", "setting isInterrupted for call " + this.callId + " to " + z, new Object[0]);
            this.isInterrupted = z;
        }

        @Override
        public void addCallTechnology(int i) {
            Log.d("TelecomAnalytics", "adding callTechnology for call " + this.callId + ": " + i, new Object[0]);
            this.callTechnologies = i | this.callTechnologies;
        }

        @Override
        public void setCallDisconnectCause(DisconnectCause disconnectCause) {
            Log.d("TelecomAnalytics", "setting disconnectCause for call " + this.callId + " to " + disconnectCause, new Object[0]);
            this.callTerminationReason = disconnectCause;
        }

        @Override
        public void setCreatedFromExistingConnection(boolean z) {
            Log.d("TelecomAnalytics", "setting createdFromExistingConnection for call " + this.callId + " to " + z, new Object[0]);
            this.createdFromExistingConnection = z;
        }

        @Override
        public void setCallConnectionService(String str) {
            Log.d("TelecomAnalytics", "setting connection service for call " + this.callId + ": " + str, new Object[0]);
            this.connectionService = str;
        }

        @Override
        public void setCallEvents(EventManager.EventRecord eventRecord) {
            this.callEvents = eventRecord;
        }

        @Override
        public void setCallIsVideo(boolean z) {
            this.isVideo = z;
        }

        @Override
        public void addVideoEvent(int i, int i2) {
            long jRoundToOneSigFig;
            long jCurrentTimeMillis = System.currentTimeMillis();
            if (this.mTimeOfLastVideoEvent < 0) {
                jRoundToOneSigFig = -1;
            } else {
                jRoundToOneSigFig = Analytics.roundToOneSigFig(jCurrentTimeMillis - this.mTimeOfLastVideoEvent);
            }
            this.mTimeOfLastVideoEvent = jCurrentTimeMillis;
            this.videoEvents.add(new TelecomLogClass.VideoEvent().setEventName(i).setTimeSinceLastEventMillis(jRoundToOneSigFig).setVideoState(i2));
        }

        @Override
        public void addInCallService(String str, int i) {
            this.inCallServiceInfos.add(new TelecomLogClass.InCallServiceInfo().setInCallServiceName(str).setInCallServiceType(i));
        }

        @Override
        public void addCallProperties(int i) {
            this.callProperties = i | this.callProperties;
        }

        public String toString() {
            return "{\n    startTime: " + this.startTime + "\n    endTime: " + this.endTime + "\n    direction: " + getCallDirectionString() + "\n    isAdditionalCall: " + this.isAdditionalCall + "\n    isInterrupted: " + this.isInterrupted + "\n    callTechnologies: " + getCallTechnologiesAsString() + "\n    callTerminationReason: " + getCallDisconnectReasonString() + "\n    connectionService: " + this.connectionService + "\n    isVideoCall: " + this.isVideo + "\n    inCallServices: " + getInCallServicesString() + "\n    callProperties: " + MtkConnection.propertiesToStringShort(this.callProperties) + "\n}\n";
        }

        public ParcelableCallAnalytics toParcelableAnalytics() {
            TelecomLogClass.CallLog proto = toProto();
            ParcelableCallAnalytics parcelableCallAnalytics = new ParcelableCallAnalytics(proto.getStartTime5Min(), proto.getCallDurationMillis(), proto.getType(), proto.getIsAdditionalCall(), proto.getIsInterrupted(), proto.getCallTechnologies(), proto.getCallTerminationCode(), proto.getIsEmergencyCall(), proto.connectionService[0], proto.getIsCreatedFromExistingConnection(), (List) Arrays.stream(proto.callEvents).map(new Function() {
                @Override
                public final Object apply(Object obj) {
                    return Analytics.CallInfoImpl.lambda$toParcelableAnalytics$0((TelecomLogClass.Event) obj);
                }
            }).collect(Collectors.toList()), (List) Arrays.stream(proto.callTimings).map(new Function() {
                @Override
                public final Object apply(Object obj) {
                    return Analytics.CallInfoImpl.lambda$toParcelableAnalytics$1((TelecomLogClass.EventTimingEntry) obj);
                }
            }).collect(Collectors.toList()));
            parcelableCallAnalytics.setIsVideoCall(proto.getIsVideoCall());
            parcelableCallAnalytics.setVideoEvents((List) Arrays.stream(proto.videoEvents).map(new Function() {
                @Override
                public final Object apply(Object obj) {
                    return Analytics.CallInfoImpl.lambda$toParcelableAnalytics$2((TelecomLogClass.VideoEvent) obj);
                }
            }).collect(Collectors.toList()));
            return parcelableCallAnalytics;
        }

        static ParcelableCallAnalytics.AnalyticsEvent lambda$toParcelableAnalytics$0(TelecomLogClass.Event event) {
            return new ParcelableCallAnalytics.AnalyticsEvent(event.getEventName(), event.getTimeSinceLastEventMillis());
        }

        static ParcelableCallAnalytics.EventTiming lambda$toParcelableAnalytics$1(TelecomLogClass.EventTimingEntry eventTimingEntry) {
            return new ParcelableCallAnalytics.EventTiming(eventTimingEntry.getTimingName(), eventTimingEntry.getTimeMillis());
        }

        static ParcelableCallAnalytics.VideoEvent lambda$toParcelableAnalytics$2(TelecomLogClass.VideoEvent videoEvent) {
            return new ParcelableCallAnalytics.VideoEvent(videoEvent.getEventName(), videoEvent.getTimeSinceLastEventMillis(), videoEvent.getVideoState());
        }

        public TelecomLogClass.CallLog toProto() {
            int code;
            TelecomLogClass.CallLog callLog = new TelecomLogClass.CallLog();
            callLog.setStartTime5Min(this.startTime - (this.startTime % 300000));
            long j = (this.endTime == 0 || this.startTime == 0) ? 0L : this.endTime - this.startTime;
            long j2 = j % 1000;
            callLog.setCallDurationMillis(j + (j2 != 0 ? 1000 - j2 : 0L));
            TelecomLogClass.CallLog callTechnologies = callLog.setType(this.callDirection).setIsAdditionalCall(this.isAdditionalCall).setIsInterrupted(this.isInterrupted).setCallTechnologies(this.callTechnologies);
            if (this.callTerminationReason == null) {
                code = -1;
            } else {
                code = this.callTerminationReason.getCode();
            }
            callTechnologies.setCallTerminationCode(code).setIsEmergencyCall(this.isEmergency).setIsCreatedFromExistingConnection(this.createdFromExistingConnection).setIsEmergencyCall(this.isEmergency).setIsVideoCall(this.isVideo).setConnectionProperties(this.callProperties);
            callLog.connectionService = new String[]{this.connectionService};
            if (this.callEvents != null) {
                callLog.callEvents = Analytics.convertLogEventsToProtoEvents(this.callEvents.getEvents());
                callLog.callTimings = (TelecomLogClass.EventTimingEntry[]) this.callEvents.extractEventTimings().stream().map(new Function() {
                    @Override
                    public final Object apply(Object obj) {
                        return Analytics.logEventTimingToProtoEventTiming((EventManager.EventRecord.EventTiming) obj);
                    }
                }).toArray(new IntFunction() {
                    @Override
                    public final Object apply(int i) {
                        return Analytics.CallInfoImpl.lambda$toProto$4(i);
                    }
                });
            }
            callLog.videoEvents = (TelecomLogClass.VideoEvent[]) this.videoEvents.toArray(new TelecomLogClass.VideoEvent[this.videoEvents.size()]);
            callLog.inCallServices = (TelecomLogClass.InCallServiceInfo[]) this.inCallServiceInfos.toArray(new TelecomLogClass.InCallServiceInfo[this.inCallServiceInfos.size()]);
            return callLog;
        }

        static TelecomLogClass.EventTimingEntry[] lambda$toProto$4(int i) {
            return new TelecomLogClass.EventTimingEntry[i];
        }

        private String getCallDirectionString() {
            switch (this.callDirection) {
                case CallState.NEW:
                    return "UNKNOWN";
                case 1:
                    return "INCOMING";
                case CallState.SELECT_PHONE_ACCOUNT:
                    return "OUTGOING";
                default:
                    return "UNKNOWN";
            }
        }

        private String getCallTechnologiesAsString() {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            if ((this.callTechnologies & 1) != 0) {
                sb.append("CDMA ");
            }
            if ((this.callTechnologies & 2) != 0) {
                sb.append("GSM ");
            }
            if ((this.callTechnologies & 8) != 0) {
                sb.append("SIP ");
            }
            if ((this.callTechnologies & 4) != 0) {
                sb.append("IMS ");
            }
            if ((this.callTechnologies & 16) != 0) {
                sb.append("THIRD_PARTY ");
            }
            sb.append(']');
            return sb.toString();
        }

        private String getCallDisconnectReasonString() {
            if (this.callTerminationReason != null) {
                return this.callTerminationReason.toString();
            }
            return "NOT SET";
        }

        private String getInCallServicesString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (TelecomLogClass.InCallServiceInfo inCallServiceInfo : this.inCallServiceInfos) {
                sb.append("    ");
                sb.append("name: ");
                sb.append(inCallServiceInfo.getInCallServiceName());
                sb.append(" type: ");
                sb.append(inCallServiceInfo.getInCallServiceType());
                sb.append("\n");
            }
            sb.append("]");
            return sb.toString();
        }
    }

    public static void addSessionTiming(String str, long j) {
        if (sLogSessionToSessionId.containsKey(str)) {
            synchronized (sLock) {
                sSessionTimings.add(new TelecomAnalytics.SessionTiming(sLogSessionToSessionId.get(str).intValue(), j));
            }
        }
    }

    public static CallInfo initiateCallAnalytics(String str, int i) {
        Log.d("TelecomAnalytics", "Starting analytics for call " + str, new Object[0]);
        CallInfoImpl callInfoImpl = new CallInfoImpl(str, i);
        synchronized (sLock) {
            while (sActiveCallIds.size() >= 100) {
                sCallIdToInfo.remove(sActiveCallIds.remove());
            }
            sCallIdToInfo.put(str, callInfoImpl);
            sActiveCallIds.add(str);
        }
        return callInfoImpl;
    }

    public static TelecomAnalytics dumpToParcelableAnalytics() {
        LinkedList linkedList = new LinkedList();
        LinkedList linkedList2 = new LinkedList();
        synchronized (sLock) {
            linkedList.addAll((Collection) sCallIdToInfo.values().stream().map(new Function() {
                @Override
                public final Object apply(Object obj) {
                    return ((Analytics.CallInfoImpl) obj).toParcelableAnalytics();
                }
            }).collect(Collectors.toList()));
            linkedList2.addAll(sSessionTimings);
            sCallIdToInfo.clear();
            sSessionTimings.clear();
        }
        return new TelecomAnalytics(linkedList2, linkedList);
    }

    public static void dumpToEncodedProto(PrintWriter printWriter, String[] strArr) {
        TelecomLogClass.TelecomLog telecomLog = new TelecomLogClass.TelecomLog();
        synchronized (sLock) {
            telecomLog.callLogs = (TelecomLogClass.CallLog[]) sCallIdToInfo.values().stream().map(new Function() {
                @Override
                public final Object apply(Object obj) {
                    return ((Analytics.CallInfoImpl) obj).toProto();
                }
            }).toArray(new IntFunction() {
                @Override
                public final Object apply(int i) {
                    return Analytics.lambda$dumpToEncodedProto$0(i);
                }
            });
            telecomLog.sessionTimings = (TelecomLogClass.LogSessionTiming[]) sSessionTimings.stream().map(new Function() {
                @Override
                public final Object apply(Object obj) {
                    TelecomAnalytics.SessionTiming sessionTiming = (TelecomAnalytics.SessionTiming) obj;
                    return new TelecomLogClass.LogSessionTiming().setSessionEntryPoint(sessionTiming.getKey().intValue()).setTimeMillis(sessionTiming.getTime());
                }
            }).toArray(new IntFunction() {
                @Override
                public final Object apply(int i) {
                    return Analytics.lambda$dumpToEncodedProto$2(i);
                }
            });
            if (strArr.length > 1 && "clear".equals(strArr[1])) {
                sCallIdToInfo.clear();
                sSessionTimings.clear();
            }
        }
        printWriter.write(Base64.encodeToString(TelecomLogClass.TelecomLog.toByteArray(telecomLog), 0));
    }

    static TelecomLogClass.CallLog[] lambda$dumpToEncodedProto$0(int i) {
        return new TelecomLogClass.CallLog[i];
    }

    static TelecomLogClass.LogSessionTiming[] lambda$dumpToEncodedProto$2(int i) {
        return new TelecomLogClass.LogSessionTiming[i];
    }

    public static void dump(final IndentingPrintWriter indentingPrintWriter) {
        synchronized (sLock) {
            final int length = "TC@".length();
            ArrayList<String> arrayList = new ArrayList(sCallIdToInfo.keySet());
            try {
                Collections.sort(arrayList, new Comparator() {
                    @Override
                    public final int compare(Object obj, Object obj2) {
                        return Analytics.lambda$dump$3(length, (String) obj, (String) obj2);
                    }
                });
            } catch (IllegalArgumentException e) {
            }
            for (String str : arrayList) {
                indentingPrintWriter.printf("Call %s: ", new Object[]{str});
                indentingPrintWriter.println(sCallIdToInfo.get(str).toString());
            }
            TelecomAnalytics.SessionTiming.averageTimings(sSessionTimings).entrySet().stream().filter(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return Analytics.sSessionIdToLogSession.containsKey(((Map.Entry) obj).getKey());
                }
            }).forEach(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    Map.Entry entry = (Map.Entry) obj;
                    indentingPrintWriter.printf("%s: %.2f\n", new Object[]{Analytics.sSessionIdToLogSession.get(entry.getKey()), entry.getValue()});
                }
            });
        }
    }

    static int lambda$dump$3(int i, String str, String str2) {
        int iIntValue;
        int iIntValue2;
        try {
            iIntValue = Integer.valueOf(str.substring(i)).intValue();
        } catch (NumberFormatException e) {
            iIntValue = Integer.MAX_VALUE;
        }
        try {
            iIntValue2 = Integer.valueOf(str2.substring(i)).intValue();
        } catch (NumberFormatException e2) {
            iIntValue2 = Integer.MAX_VALUE;
        }
        return iIntValue - iIntValue2;
    }

    @VisibleForTesting
    public static Map<String, CallInfoImpl> cloneData() {
        HashMap map;
        synchronized (sLock) {
            map = new HashMap(sCallIdToInfo.size());
            for (Map.Entry<String, CallInfoImpl> entry : sCallIdToInfo.entrySet()) {
                map.put(entry.getKey(), new CallInfoImpl(entry.getValue()));
            }
        }
        return map;
    }

    private static TelecomLogClass.Event[] convertLogEventsToProtoEvents(List<EventManager.Event> list) {
        long j;
        ArrayList arrayList = new ArrayList(list.size());
        long j2 = -1;
        for (EventManager.Event event : list) {
            if (sLogEventToAnalyticsEvent.containsKey(event.eventId)) {
                TelecomLogClass.Event event2 = new TelecomLogClass.Event();
                event2.setEventName(sLogEventToAnalyticsEvent.get(event.eventId).intValue());
                if (j2 < 0) {
                    j = -1;
                } else {
                    j = event.time - j2;
                }
                event2.setTimeSinceLastEventMillis(roundToOneSigFig(j));
                arrayList.add(event2);
                j2 = event.time;
            }
        }
        return (TelecomLogClass.Event[]) arrayList.toArray(new TelecomLogClass.Event[arrayList.size()]);
    }

    private static TelecomLogClass.EventTimingEntry logEventTimingToProtoEventTiming(EventManager.EventRecord.EventTiming eventTiming) {
        int iIntValue;
        if (sLogEventTimingToAnalyticsEventTiming.containsKey(eventTiming.name)) {
            iIntValue = sLogEventTimingToAnalyticsEventTiming.get(eventTiming.name).intValue();
        } else {
            iIntValue = 999999;
        }
        return new TelecomLogClass.EventTimingEntry().setTimingName(iIntValue).setTimeMillis(eventTiming.time);
    }

    @VisibleForTesting
    public static long roundToOneSigFig(long j) {
        if (j == 0) {
            return j;
        }
        return (long) (Math.round(j / r0) * Math.pow(10.0d, (int) Math.floor(Math.log10(j < 0 ? -j : j))));
    }
}
