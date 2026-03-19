package com.android.internal.telephony.metrics;

import android.hardware.radio.V1_0.SetupDataCallResult;
import android.os.Build;
import android.os.SystemClock;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyHistogram;
import android.telephony.data.DataCallResponse;
import android.telephony.ims.ImsCallSession;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.feature.MmTelFeature;
import android.text.TextUtils;
import android.util.Base64;
import android.util.SparseArray;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.GsmCdmaConnection;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.SmsResponse;
import com.android.internal.telephony.UUSInfo;
import com.android.internal.telephony.nano.TelephonyProto;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public class TelephonyMetrics {
    private static final boolean DBG = true;
    private static final int MAX_COMPLETED_CALL_SESSIONS = 50;
    private static final int MAX_COMPLETED_SMS_SESSIONS = 500;
    private static final int MAX_TELEPHONY_EVENTS = 1000;
    private static final int SESSION_START_PRECISION_MINUTES = 5;
    private static final String TAG = TelephonyMetrics.class.getSimpleName();
    private static final boolean VDBG = false;
    private static TelephonyMetrics sInstance;
    private long mStartElapsedTimeMs;
    private long mStartSystemTimeMs;
    private final Deque<TelephonyProto.TelephonyEvent> mTelephonyEvents = new ArrayDeque();
    private final SparseArray<InProgressCallSession> mInProgressCallSessions = new SparseArray<>();
    private final Deque<TelephonyProto.TelephonyCallSession> mCompletedCallSessions = new ArrayDeque();
    private final SparseArray<InProgressSmsSession> mInProgressSmsSessions = new SparseArray<>();
    private final Deque<TelephonyProto.SmsSession> mCompletedSmsSessions = new ArrayDeque();
    private final SparseArray<TelephonyProto.TelephonyServiceState> mLastServiceState = new SparseArray<>();
    private final SparseArray<TelephonyProto.ImsCapabilities> mLastImsCapabilities = new SparseArray<>();
    private final SparseArray<TelephonyProto.ImsConnectionState> mLastImsConnectionState = new SparseArray<>();
    private final SparseArray<TelephonyProto.TelephonySettings> mLastSettings = new SparseArray<>();
    private boolean mTelephonyEventsDropped = false;

    public TelephonyMetrics() {
        reset();
    }

    public static synchronized TelephonyMetrics getInstance() {
        if (sInstance == null) {
            sInstance = new TelephonyMetrics();
        }
        return sInstance;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (strArr != null && strArr.length > 0) {
            byte b = 0;
            String str = strArr[0];
            int iHashCode = str.hashCode();
            if (iHashCode != -1953159389) {
                b = (iHashCode == 950313125 && str.equals("--metricsproto")) ? (byte) 1 : (byte) -1;
            } else if (!str.equals("--metrics")) {
            }
            switch (b) {
                case 0:
                    printAllMetrics(printWriter);
                    break;
                case 1:
                    printWriter.println(convertProtoToBase64String(buildProto()));
                    reset();
                    break;
            }
        }
    }

    private static String telephonyEventToString(int i) {
        switch (i) {
            case 0:
                return "UNKNOWN";
            case 1:
                return "SETTINGS_CHANGED";
            case 2:
                return "RIL_SERVICE_STATE_CHANGED";
            case 3:
                return "IMS_CONNECTION_STATE_CHANGED";
            case 4:
                return "IMS_CAPABILITIES_CHANGED";
            case 5:
                return "DATA_CALL_SETUP";
            case 6:
                return "DATA_CALL_SETUP_RESPONSE";
            case 7:
                return "DATA_CALL_LIST_CHANGED";
            case 8:
                return "DATA_CALL_DEACTIVATE";
            case 9:
                return "DATA_CALL_DEACTIVATE_RESPONSE";
            case 10:
                return "DATA_STALL_ACTION";
            case 11:
                return "MODEM_RESTART";
            case 12:
            default:
                return Integer.toString(i);
            case 13:
                return "CARRIER_ID_MATCHING";
        }
    }

    private static String callSessionEventToString(int i) {
        switch (i) {
            case 0:
                return "EVENT_UNKNOWN";
            case 1:
                return "SETTINGS_CHANGED";
            case 2:
                return "RIL_SERVICE_STATE_CHANGED";
            case 3:
                return "IMS_CONNECTION_STATE_CHANGED";
            case 4:
                return "IMS_CAPABILITIES_CHANGED";
            case 5:
                return "DATA_CALL_LIST_CHANGED";
            case 6:
                return "RIL_REQUEST";
            case 7:
                return "RIL_RESPONSE";
            case 8:
                return "RIL_CALL_RING";
            case 9:
                return "RIL_CALL_SRVCC";
            case 10:
                return "RIL_CALL_LIST_CHANGED";
            case 11:
                return "IMS_COMMAND";
            case 12:
                return "IMS_COMMAND_RECEIVED";
            case 13:
                return "IMS_COMMAND_FAILED";
            case 14:
                return "IMS_COMMAND_COMPLETE";
            case 15:
                return "IMS_CALL_RECEIVE";
            case 16:
                return "IMS_CALL_STATE_CHANGED";
            case 17:
                return "IMS_CALL_TERMINATED";
            case 18:
                return "IMS_CALL_HANDOVER";
            case 19:
                return "IMS_CALL_HANDOVER_FAILED";
            case 20:
                return "PHONE_STATE_CHANGED";
            case 21:
                return "NITZ_TIME";
            default:
                return Integer.toString(i);
        }
    }

    private static String smsSessionEventToString(int i) {
        switch (i) {
            case 0:
                return "EVENT_UNKNOWN";
            case 1:
                return "SETTINGS_CHANGED";
            case 2:
                return "RIL_SERVICE_STATE_CHANGED";
            case 3:
                return "IMS_CONNECTION_STATE_CHANGED";
            case 4:
                return "IMS_CAPABILITIES_CHANGED";
            case 5:
                return "DATA_CALL_LIST_CHANGED";
            case 6:
                return "SMS_SEND";
            case 7:
                return "SMS_SEND_RESULT";
            case 8:
                return "SMS_RECEIVED";
            default:
                return Integer.toString(i);
        }
    }

    private synchronized void printAllMetrics(PrintWriter printWriter) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        indentingPrintWriter.println("Telephony metrics proto:");
        indentingPrintWriter.println("------------------------------------------");
        indentingPrintWriter.println("Telephony events:");
        indentingPrintWriter.increaseIndent();
        for (TelephonyProto.TelephonyEvent telephonyEvent : this.mTelephonyEvents) {
            indentingPrintWriter.print(telephonyEvent.timestampMillis);
            indentingPrintWriter.print(" [");
            indentingPrintWriter.print(telephonyEvent.phoneId);
            indentingPrintWriter.print("] ");
            indentingPrintWriter.print("T=");
            if (telephonyEvent.type == 2) {
                indentingPrintWriter.print(telephonyEventToString(telephonyEvent.type) + "(" + telephonyEvent.serviceState.dataRat + ")");
            } else {
                indentingPrintWriter.print(telephonyEventToString(telephonyEvent.type));
            }
            indentingPrintWriter.println("");
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("Call sessions:");
        indentingPrintWriter.increaseIndent();
        for (TelephonyProto.TelephonyCallSession telephonyCallSession : this.mCompletedCallSessions) {
            indentingPrintWriter.println("Start time in minutes: " + telephonyCallSession.startTimeMinutes);
            indentingPrintWriter.println("Events dropped: " + telephonyCallSession.eventsDropped);
            indentingPrintWriter.println("Events: ");
            indentingPrintWriter.increaseIndent();
            for (TelephonyProto.TelephonyCallSession.Event event : telephonyCallSession.events) {
                indentingPrintWriter.print(event.delay);
                indentingPrintWriter.print(" T=");
                if (event.type == 2) {
                    indentingPrintWriter.println(callSessionEventToString(event.type) + "(" + event.serviceState.dataRat + ")");
                } else if (event.type == 10) {
                    indentingPrintWriter.println(callSessionEventToString(event.type));
                    indentingPrintWriter.increaseIndent();
                    for (TelephonyProto.TelephonyCallSession.Event.RilCall rilCall : event.calls) {
                        indentingPrintWriter.println(rilCall.index + ". Type = " + rilCall.type + " State = " + rilCall.state + " End Reason " + rilCall.callEndReason + " isMultiparty = " + rilCall.isMultiparty);
                    }
                    indentingPrintWriter.decreaseIndent();
                } else {
                    indentingPrintWriter.println(callSessionEventToString(event.type));
                }
            }
            indentingPrintWriter.decreaseIndent();
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("Sms sessions:");
        indentingPrintWriter.increaseIndent();
        int i = 0;
        for (TelephonyProto.SmsSession smsSession : this.mCompletedSmsSessions) {
            i++;
            indentingPrintWriter.print("[" + i + "] Start time in minutes: " + smsSession.startTimeMinutes);
            if (smsSession.eventsDropped) {
                indentingPrintWriter.println(", events dropped: " + smsSession.eventsDropped);
            }
            indentingPrintWriter.println("Events: ");
            indentingPrintWriter.increaseIndent();
            for (TelephonyProto.SmsSession.Event event2 : smsSession.events) {
                indentingPrintWriter.print(event2.delay);
                indentingPrintWriter.print(" T=");
                indentingPrintWriter.println(smsSessionEventToString(event2.type));
            }
            indentingPrintWriter.decreaseIndent();
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("Modem power stats:");
        indentingPrintWriter.increaseIndent();
        TelephonyProto.ModemPowerStats modemPowerStatsBuildProto = new ModemPowerMetrics().buildProto();
        indentingPrintWriter.println("Power log duration (battery time) (ms): " + modemPowerStatsBuildProto.loggingDurationMs);
        indentingPrintWriter.println("Energy consumed by modem (mAh): " + modemPowerStatsBuildProto.energyConsumedMah);
        indentingPrintWriter.println("Number of packets sent (tx): " + modemPowerStatsBuildProto.numPacketsTx);
        indentingPrintWriter.println("Amount of time kernel is active because of cellular data (ms): " + modemPowerStatsBuildProto.cellularKernelActiveTimeMs);
        indentingPrintWriter.println("Amount of time spent in very poor rx signal level (ms): " + modemPowerStatsBuildProto.timeInVeryPoorRxSignalLevelMs);
        indentingPrintWriter.println("Amount of time modem is in sleep (ms): " + modemPowerStatsBuildProto.sleepTimeMs);
        indentingPrintWriter.println("Amount of time modem is in idle (ms): " + modemPowerStatsBuildProto.idleTimeMs);
        indentingPrintWriter.println("Amount of time modem is in rx (ms): " + modemPowerStatsBuildProto.rxTimeMs);
        indentingPrintWriter.println("Amount of time modem is in tx (ms): " + Arrays.toString(modemPowerStatsBuildProto.txTimeMs));
        indentingPrintWriter.decreaseIndent();
    }

    private static String convertProtoToBase64String(TelephonyProto.TelephonyLog telephonyLog) {
        return Base64.encodeToString(TelephonyProto.TelephonyLog.toByteArray(telephonyLog), 0);
    }

    private synchronized void reset() {
        this.mTelephonyEvents.clear();
        this.mCompletedCallSessions.clear();
        this.mCompletedSmsSessions.clear();
        this.mTelephonyEventsDropped = false;
        this.mStartSystemTimeMs = System.currentTimeMillis();
        this.mStartElapsedTimeMs = SystemClock.elapsedRealtime();
        for (int i = 0; i < this.mLastServiceState.size(); i++) {
            int iKeyAt = this.mLastServiceState.keyAt(i);
            addTelephonyEvent(new TelephonyEventBuilder(this.mStartElapsedTimeMs, iKeyAt).setServiceState(this.mLastServiceState.get(iKeyAt)).build());
        }
        for (int i2 = 0; i2 < this.mLastImsCapabilities.size(); i2++) {
            int iKeyAt2 = this.mLastImsCapabilities.keyAt(i2);
            addTelephonyEvent(new TelephonyEventBuilder(this.mStartElapsedTimeMs, iKeyAt2).setImsCapabilities(this.mLastImsCapabilities.get(iKeyAt2)).build());
        }
        for (int i3 = 0; i3 < this.mLastImsConnectionState.size(); i3++) {
            int iKeyAt3 = this.mLastImsConnectionState.keyAt(i3);
            addTelephonyEvent(new TelephonyEventBuilder(this.mStartElapsedTimeMs, iKeyAt3).setImsConnectionState(this.mLastImsConnectionState.get(iKeyAt3)).build());
        }
    }

    private synchronized TelephonyProto.TelephonyLog buildProto() {
        TelephonyProto.TelephonyLog telephonyLog;
        telephonyLog = new TelephonyProto.TelephonyLog();
        telephonyLog.events = new TelephonyProto.TelephonyEvent[this.mTelephonyEvents.size()];
        this.mTelephonyEvents.toArray(telephonyLog.events);
        telephonyLog.eventsDropped = this.mTelephonyEventsDropped;
        telephonyLog.callSessions = new TelephonyProto.TelephonyCallSession[this.mCompletedCallSessions.size()];
        this.mCompletedCallSessions.toArray(telephonyLog.callSessions);
        telephonyLog.smsSessions = new TelephonyProto.SmsSession[this.mCompletedSmsSessions.size()];
        this.mCompletedSmsSessions.toArray(telephonyLog.smsSessions);
        List<TelephonyHistogram> telephonyRILTimingHistograms = RIL.getTelephonyRILTimingHistograms();
        telephonyLog.histograms = new TelephonyProto.TelephonyHistogram[telephonyRILTimingHistograms.size()];
        for (int i = 0; i < telephonyRILTimingHistograms.size(); i++) {
            telephonyLog.histograms[i] = new TelephonyProto.TelephonyHistogram();
            TelephonyHistogram telephonyHistogram = telephonyRILTimingHistograms.get(i);
            TelephonyProto.TelephonyHistogram telephonyHistogram2 = telephonyLog.histograms[i];
            telephonyHistogram2.category = telephonyHistogram.getCategory();
            telephonyHistogram2.id = telephonyHistogram.getId();
            telephonyHistogram2.minTimeMillis = telephonyHistogram.getMinTime();
            telephonyHistogram2.maxTimeMillis = telephonyHistogram.getMaxTime();
            telephonyHistogram2.avgTimeMillis = telephonyHistogram.getAverageTime();
            telephonyHistogram2.count = telephonyHistogram.getSampleCount();
            telephonyHistogram2.bucketCount = telephonyHistogram.getBucketCount();
            telephonyHistogram2.bucketEndPoints = telephonyHistogram.getBucketEndPoints();
            telephonyHistogram2.bucketCounters = telephonyHistogram.getBucketCounters();
        }
        telephonyLog.modemPowerStats = new ModemPowerMetrics().buildProto();
        telephonyLog.startTime = new TelephonyProto.Time();
        telephonyLog.startTime.systemTimestampMillis = this.mStartSystemTimeMs;
        telephonyLog.startTime.elapsedTimestampMillis = this.mStartElapsedTimeMs;
        telephonyLog.endTime = new TelephonyProto.Time();
        telephonyLog.endTime.systemTimestampMillis = System.currentTimeMillis();
        telephonyLog.endTime.elapsedTimestampMillis = SystemClock.elapsedRealtime();
        return telephonyLog;
    }

    static int roundSessionStart(long j) {
        return (int) ((j / 300000) * 5);
    }

    public void writeCarrierKeyEvent(int i, int i2, boolean z) {
        TelephonyProto.TelephonyEvent.CarrierKeyChange carrierKeyChange = new TelephonyProto.TelephonyEvent.CarrierKeyChange();
        carrierKeyChange.keyType = i2;
        carrierKeyChange.isDownloadSuccessful = z;
        addTelephonyEvent(new TelephonyEventBuilder(i).setCarrierKeyChange(carrierKeyChange).build());
    }

    static int toPrivacyFuzzedTimeInterval(long j, long j2) {
        long j3 = j2 - j;
        if (j3 < 0) {
            return 0;
        }
        if (j3 <= 10) {
            return 1;
        }
        if (j3 <= 20) {
            return 2;
        }
        if (j3 <= 50) {
            return 3;
        }
        if (j3 <= 100) {
            return 4;
        }
        if (j3 <= 200) {
            return 5;
        }
        if (j3 <= 500) {
            return 6;
        }
        if (j3 <= 1000) {
            return 7;
        }
        if (j3 <= 2000) {
            return 8;
        }
        if (j3 <= 5000) {
            return 9;
        }
        if (j3 <= 10000) {
            return 10;
        }
        if (j3 <= 30000) {
            return 11;
        }
        if (j3 <= 60000) {
            return 12;
        }
        if (j3 <= 180000) {
            return 13;
        }
        if (j3 <= 600000) {
            return 14;
        }
        if (j3 <= 1800000) {
            return 15;
        }
        if (j3 <= 3600000) {
            return 16;
        }
        if (j3 <= 7200000) {
            return 17;
        }
        if (j3 <= 14400000) {
            return 18;
        }
        return 19;
    }

    private TelephonyProto.TelephonyServiceState toServiceStateProto(ServiceState serviceState) {
        TelephonyProto.TelephonyServiceState telephonyServiceState = new TelephonyProto.TelephonyServiceState();
        telephonyServiceState.voiceRoamingType = serviceState.getVoiceRoamingType();
        telephonyServiceState.dataRoamingType = serviceState.getDataRoamingType();
        telephonyServiceState.voiceOperator = new TelephonyProto.TelephonyServiceState.TelephonyOperator();
        if (serviceState.getVoiceOperatorAlphaLong() != null) {
            telephonyServiceState.voiceOperator.alphaLong = serviceState.getVoiceOperatorAlphaLong();
        }
        if (serviceState.getVoiceOperatorAlphaShort() != null) {
            telephonyServiceState.voiceOperator.alphaShort = serviceState.getVoiceOperatorAlphaShort();
        }
        if (serviceState.getVoiceOperatorNumeric() != null) {
            telephonyServiceState.voiceOperator.numeric = serviceState.getVoiceOperatorNumeric();
        }
        telephonyServiceState.dataOperator = new TelephonyProto.TelephonyServiceState.TelephonyOperator();
        if (serviceState.getDataOperatorAlphaLong() != null) {
            telephonyServiceState.dataOperator.alphaLong = serviceState.getDataOperatorAlphaLong();
        }
        if (serviceState.getDataOperatorAlphaShort() != null) {
            telephonyServiceState.dataOperator.alphaShort = serviceState.getDataOperatorAlphaShort();
        }
        if (serviceState.getDataOperatorNumeric() != null) {
            telephonyServiceState.dataOperator.numeric = serviceState.getDataOperatorNumeric();
        }
        telephonyServiceState.voiceRat = serviceState.getRilVoiceRadioTechnology();
        telephonyServiceState.dataRat = serviceState.getRilDataRadioTechnology();
        return telephonyServiceState;
    }

    private synchronized void annotateInProgressCallSession(long j, int i, CallSessionEventBuilder callSessionEventBuilder) {
        InProgressCallSession inProgressCallSession = this.mInProgressCallSessions.get(i);
        if (inProgressCallSession != null) {
            inProgressCallSession.addEvent(j, callSessionEventBuilder);
        }
    }

    private synchronized void annotateInProgressSmsSession(long j, int i, SmsSessionEventBuilder smsSessionEventBuilder) {
        InProgressSmsSession inProgressSmsSession = this.mInProgressSmsSessions.get(i);
        if (inProgressSmsSession != null) {
            inProgressSmsSession.addEvent(j, smsSessionEventBuilder);
        }
    }

    private synchronized InProgressCallSession startNewCallSessionIfNeeded(int i) {
        InProgressCallSession inProgressCallSession;
        inProgressCallSession = this.mInProgressCallSessions.get(i);
        if (inProgressCallSession == null) {
            inProgressCallSession = new InProgressCallSession(i);
            this.mInProgressCallSessions.append(i, inProgressCallSession);
            TelephonyProto.TelephonyServiceState telephonyServiceState = this.mLastServiceState.get(i);
            if (telephonyServiceState != null) {
                inProgressCallSession.addEvent(inProgressCallSession.startElapsedTimeMs, new CallSessionEventBuilder(2).setServiceState(telephonyServiceState));
            }
            TelephonyProto.ImsCapabilities imsCapabilities = this.mLastImsCapabilities.get(i);
            if (imsCapabilities != null) {
                inProgressCallSession.addEvent(inProgressCallSession.startElapsedTimeMs, new CallSessionEventBuilder(4).setImsCapabilities(imsCapabilities));
            }
            TelephonyProto.ImsConnectionState imsConnectionState = this.mLastImsConnectionState.get(i);
            if (imsConnectionState != null) {
                inProgressCallSession.addEvent(inProgressCallSession.startElapsedTimeMs, new CallSessionEventBuilder(3).setImsConnectionState(imsConnectionState));
            }
        }
        return inProgressCallSession;
    }

    private synchronized InProgressSmsSession startNewSmsSessionIfNeeded(int i) {
        InProgressSmsSession inProgressSmsSession;
        inProgressSmsSession = this.mInProgressSmsSessions.get(i);
        if (inProgressSmsSession == null) {
            inProgressSmsSession = new InProgressSmsSession(i);
            this.mInProgressSmsSessions.append(i, inProgressSmsSession);
            TelephonyProto.TelephonyServiceState telephonyServiceState = this.mLastServiceState.get(i);
            if (telephonyServiceState != null) {
                inProgressSmsSession.addEvent(inProgressSmsSession.startElapsedTimeMs, new SmsSessionEventBuilder(2).setServiceState(telephonyServiceState));
            }
            TelephonyProto.ImsCapabilities imsCapabilities = this.mLastImsCapabilities.get(i);
            if (imsCapabilities != null) {
                inProgressSmsSession.addEvent(inProgressSmsSession.startElapsedTimeMs, new SmsSessionEventBuilder(4).setImsCapabilities(imsCapabilities));
            }
            TelephonyProto.ImsConnectionState imsConnectionState = this.mLastImsConnectionState.get(i);
            if (imsConnectionState != null) {
                inProgressSmsSession.addEvent(inProgressSmsSession.startElapsedTimeMs, new SmsSessionEventBuilder(3).setImsConnectionState(imsConnectionState));
            }
        }
        return inProgressSmsSession;
    }

    private synchronized void finishCallSession(InProgressCallSession inProgressCallSession) {
        TelephonyProto.TelephonyCallSession telephonyCallSession = new TelephonyProto.TelephonyCallSession();
        telephonyCallSession.events = new TelephonyProto.TelephonyCallSession.Event[inProgressCallSession.events.size()];
        inProgressCallSession.events.toArray(telephonyCallSession.events);
        telephonyCallSession.startTimeMinutes = inProgressCallSession.startSystemTimeMin;
        telephonyCallSession.phoneId = inProgressCallSession.phoneId;
        telephonyCallSession.eventsDropped = inProgressCallSession.isEventsDropped();
        if (this.mCompletedCallSessions.size() >= 50) {
            this.mCompletedCallSessions.removeFirst();
        }
        this.mCompletedCallSessions.add(telephonyCallSession);
        this.mInProgressCallSessions.remove(inProgressCallSession.phoneId);
    }

    private synchronized void finishSmsSessionIfNeeded(InProgressSmsSession inProgressSmsSession) {
        if (inProgressSmsSession.getNumExpectedResponses() == 0) {
            TelephonyProto.SmsSession smsSession = new TelephonyProto.SmsSession();
            smsSession.events = new TelephonyProto.SmsSession.Event[inProgressSmsSession.events.size()];
            inProgressSmsSession.events.toArray(smsSession.events);
            smsSession.startTimeMinutes = inProgressSmsSession.startSystemTimeMin;
            smsSession.phoneId = inProgressSmsSession.phoneId;
            smsSession.eventsDropped = inProgressSmsSession.isEventsDropped();
            if (this.mCompletedSmsSessions.size() >= MAX_COMPLETED_SMS_SESSIONS) {
                this.mCompletedSmsSessions.removeFirst();
            }
            this.mCompletedSmsSessions.add(smsSession);
            this.mInProgressSmsSessions.remove(inProgressSmsSession.phoneId);
        }
    }

    private synchronized void addTelephonyEvent(TelephonyProto.TelephonyEvent telephonyEvent) {
        if (this.mTelephonyEvents.size() >= 1000) {
            this.mTelephonyEvents.removeFirst();
            this.mTelephonyEventsDropped = true;
        }
        this.mTelephonyEvents.add(telephonyEvent);
    }

    public synchronized void writeServiceStateChanged(int i, ServiceState serviceState) {
        TelephonyProto.TelephonyEvent telephonyEventBuild = new TelephonyEventBuilder(i).setServiceState(toServiceStateProto(serviceState)).build();
        if (this.mLastServiceState.get(i) == null || !Arrays.equals(TelephonyProto.TelephonyServiceState.toByteArray(this.mLastServiceState.get(i)), TelephonyProto.TelephonyServiceState.toByteArray(telephonyEventBuild.serviceState))) {
            this.mLastServiceState.put(i, telephonyEventBuild.serviceState);
            addTelephonyEvent(telephonyEventBuild);
            annotateInProgressCallSession(telephonyEventBuild.timestampMillis, i, new CallSessionEventBuilder(2).setServiceState(telephonyEventBuild.serviceState));
            annotateInProgressSmsSession(telephonyEventBuild.timestampMillis, i, new SmsSessionEventBuilder(2).setServiceState(telephonyEventBuild.serviceState));
        }
    }

    public void writeDataStallEvent(int i, int i2) {
        addTelephonyEvent(new TelephonyEventBuilder(i).setDataStallRecoveryAction(i2).build());
    }

    public void writeImsSetFeatureValue(int i, int i2, int i3, int i4) {
        TelephonyProto.TelephonySettings telephonySettings = new TelephonyProto.TelephonySettings();
        if (i3 == 0) {
            switch (i2) {
                case 1:
                    telephonySettings.isEnhanced4GLteModeEnabled = i4 != 0;
                    break;
                case 2:
                    telephonySettings.isVtOverLteEnabled = i4 != 0;
                    break;
            }
        } else if (i3 == 1) {
            switch (i2) {
                case 1:
                    telephonySettings.isWifiCallingEnabled = i4 != 0;
                    break;
                case 2:
                    telephonySettings.isVtOverWifiEnabled = i4 != 0;
                    break;
            }
        }
        if (this.mLastSettings.get(i) != null && Arrays.equals(TelephonyProto.TelephonySettings.toByteArray(this.mLastSettings.get(i)), TelephonyProto.TelephonySettings.toByteArray(telephonySettings))) {
            return;
        }
        this.mLastSettings.put(i, telephonySettings);
        TelephonyProto.TelephonyEvent telephonyEventBuild = new TelephonyEventBuilder(i).setSettings(telephonySettings).build();
        addTelephonyEvent(telephonyEventBuild);
        annotateInProgressCallSession(telephonyEventBuild.timestampMillis, i, new CallSessionEventBuilder(1).setSettings(telephonySettings));
        annotateInProgressSmsSession(telephonyEventBuild.timestampMillis, i, new SmsSessionEventBuilder(1).setSettings(telephonySettings));
    }

    public void writeSetPreferredNetworkType(int i, int i2) {
        TelephonyProto.TelephonySettings telephonySettings = new TelephonyProto.TelephonySettings();
        telephonySettings.preferredNetworkMode = i2 + 1;
        if (this.mLastSettings.get(i) != null && Arrays.equals(TelephonyProto.TelephonySettings.toByteArray(this.mLastSettings.get(i)), TelephonyProto.TelephonySettings.toByteArray(telephonySettings))) {
            return;
        }
        this.mLastSettings.put(i, telephonySettings);
        addTelephonyEvent(new TelephonyEventBuilder(i).setSettings(telephonySettings).build());
    }

    public synchronized void writeOnImsConnectionState(int i, int i2, ImsReasonInfo imsReasonInfo) {
        TelephonyProto.ImsConnectionState imsConnectionState = new TelephonyProto.ImsConnectionState();
        imsConnectionState.state = i2;
        if (imsReasonInfo != null) {
            TelephonyProto.ImsReasonInfo imsReasonInfo2 = new TelephonyProto.ImsReasonInfo();
            imsReasonInfo2.reasonCode = imsReasonInfo.getCode();
            imsReasonInfo2.extraCode = imsReasonInfo.getExtraCode();
            String extraMessage = imsReasonInfo.getExtraMessage();
            if (extraMessage != null) {
                imsReasonInfo2.extraMessage = extraMessage;
            }
            imsConnectionState.reasonInfo = imsReasonInfo2;
        }
        if (this.mLastImsConnectionState.get(i) == null || !Arrays.equals(TelephonyProto.ImsConnectionState.toByteArray(this.mLastImsConnectionState.get(i)), TelephonyProto.ImsConnectionState.toByteArray(imsConnectionState))) {
            this.mLastImsConnectionState.put(i, imsConnectionState);
            TelephonyProto.TelephonyEvent telephonyEventBuild = new TelephonyEventBuilder(i).setImsConnectionState(imsConnectionState).build();
            addTelephonyEvent(telephonyEventBuild);
            annotateInProgressCallSession(telephonyEventBuild.timestampMillis, i, new CallSessionEventBuilder(3).setImsConnectionState(telephonyEventBuild.imsConnectionState));
            annotateInProgressSmsSession(telephonyEventBuild.timestampMillis, i, new SmsSessionEventBuilder(3).setImsConnectionState(telephonyEventBuild.imsConnectionState));
        }
    }

    public synchronized void writeOnImsCapabilities(int i, int i2, MmTelFeature.MmTelCapabilities mmTelCapabilities) {
        TelephonyProto.ImsCapabilities imsCapabilities = new TelephonyProto.ImsCapabilities();
        if (i2 == 0) {
            imsCapabilities.voiceOverLte = mmTelCapabilities.isCapable(1);
            imsCapabilities.videoOverLte = mmTelCapabilities.isCapable(2);
            imsCapabilities.utOverLte = mmTelCapabilities.isCapable(4);
        } else if (i2 == 1) {
            imsCapabilities.voiceOverWifi = mmTelCapabilities.isCapable(1);
            imsCapabilities.videoOverWifi = mmTelCapabilities.isCapable(2);
            imsCapabilities.utOverWifi = mmTelCapabilities.isCapable(4);
        }
        TelephonyProto.TelephonyEvent telephonyEventBuild = new TelephonyEventBuilder(i).setImsCapabilities(imsCapabilities).build();
        if (this.mLastImsCapabilities.get(i) == null || !Arrays.equals(TelephonyProto.ImsCapabilities.toByteArray(this.mLastImsCapabilities.get(i)), TelephonyProto.ImsCapabilities.toByteArray(imsCapabilities))) {
            this.mLastImsCapabilities.put(i, imsCapabilities);
            addTelephonyEvent(telephonyEventBuild);
            annotateInProgressCallSession(telephonyEventBuild.timestampMillis, i, new CallSessionEventBuilder(4).setImsCapabilities(telephonyEventBuild.imsCapabilities));
            annotateInProgressSmsSession(telephonyEventBuild.timestampMillis, i, new SmsSessionEventBuilder(4).setImsCapabilities(telephonyEventBuild.imsCapabilities));
        }
    }

    private int toPdpType(String str) {
        byte b;
        int iHashCode = str.hashCode();
        if (iHashCode != -2128542875) {
            if (iHashCode != 2343) {
                if (iHashCode != 79440) {
                    b = (iHashCode == 2254343 && str.equals("IPV6")) ? (byte) 1 : (byte) -1;
                } else if (str.equals("PPP")) {
                    b = 3;
                }
            } else if (str.equals("IP")) {
                b = 0;
            }
        } else if (str.equals("IPV4V6")) {
            b = 2;
        }
        switch (b) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            default:
                Rlog.e(TAG, "Unknown type: " + str);
                return 0;
        }
    }

    public void writeSetupDataCall(int i, int i2, int i3, String str, String str2) {
        TelephonyProto.TelephonyEvent.RilSetupDataCall rilSetupDataCall = new TelephonyProto.TelephonyEvent.RilSetupDataCall();
        rilSetupDataCall.rat = i2;
        rilSetupDataCall.dataProfile = i3 + 1;
        if (str != null) {
            rilSetupDataCall.apn = str;
        }
        if (str2 != null) {
            rilSetupDataCall.type = toPdpType(str2);
        }
        addTelephonyEvent(new TelephonyEventBuilder(i).setSetupDataCall(rilSetupDataCall).build());
    }

    public void writeRilDeactivateDataCall(int i, int i2, int i3, int i4) {
        TelephonyProto.TelephonyEvent.RilDeactivateDataCall rilDeactivateDataCall = new TelephonyProto.TelephonyEvent.RilDeactivateDataCall();
        rilDeactivateDataCall.cid = i3;
        switch (i4) {
            case 1:
                rilDeactivateDataCall.reason = 1;
                break;
            case 2:
                rilDeactivateDataCall.reason = 2;
                break;
            case 3:
                rilDeactivateDataCall.reason = 4;
                break;
            default:
                rilDeactivateDataCall.reason = 0;
                break;
        }
        addTelephonyEvent(new TelephonyEventBuilder(i).setDeactivateDataCall(rilDeactivateDataCall).build());
    }

    public void writeRilDataCallList(int i, ArrayList<DataCallResponse> arrayList) {
        TelephonyProto.RilDataCall[] rilDataCallArr = new TelephonyProto.RilDataCall[arrayList.size()];
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            rilDataCallArr[i2] = new TelephonyProto.RilDataCall();
            rilDataCallArr[i2].cid = arrayList.get(i2).getCallId();
            if (!TextUtils.isEmpty(arrayList.get(i2).getIfname())) {
                rilDataCallArr[i2].iframe = arrayList.get(i2).getIfname();
            }
            if (!TextUtils.isEmpty(arrayList.get(i2).getType())) {
                rilDataCallArr[i2].type = toPdpType(arrayList.get(i2).getType());
            }
        }
        addTelephonyEvent(new TelephonyEventBuilder(i).setDataCalls(rilDataCallArr).build());
    }

    public void writeRilCallList(int i, ArrayList<GsmCdmaConnection> arrayList) {
        InProgressCallSession inProgressCallSessionStartNewCallSessionIfNeeded = startNewCallSessionIfNeeded(i);
        if (inProgressCallSessionStartNewCallSessionIfNeeded == null) {
            Rlog.e(TAG, "writeRilCallList: Call session is missing");
            return;
        }
        TelephonyProto.TelephonyCallSession.Event.RilCall[] rilCallArrConvertConnectionsToRilCalls = convertConnectionsToRilCalls(arrayList);
        inProgressCallSessionStartNewCallSessionIfNeeded.addEvent(new CallSessionEventBuilder(10).setRilCalls(rilCallArrConvertConnectionsToRilCalls));
        if (inProgressCallSessionStartNewCallSessionIfNeeded.isPhoneIdle() && disconnectReasonsKnown(rilCallArrConvertConnectionsToRilCalls)) {
            finishCallSession(inProgressCallSessionStartNewCallSessionIfNeeded);
        }
    }

    private boolean disconnectReasonsKnown(TelephonyProto.TelephonyCallSession.Event.RilCall[] rilCallArr) {
        for (TelephonyProto.TelephonyCallSession.Event.RilCall rilCall : rilCallArr) {
            if (rilCall.callEndReason == 0) {
                return false;
            }
        }
        return true;
    }

    private TelephonyProto.TelephonyCallSession.Event.RilCall[] convertConnectionsToRilCalls(ArrayList<GsmCdmaConnection> arrayList) {
        TelephonyProto.TelephonyCallSession.Event.RilCall[] rilCallArr = new TelephonyProto.TelephonyCallSession.Event.RilCall[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            rilCallArr[i] = new TelephonyProto.TelephonyCallSession.Event.RilCall();
            rilCallArr[i].index = i;
            convertConnectionToRilCall(arrayList.get(i), rilCallArr[i]);
        }
        return rilCallArr;
    }

    private void convertConnectionToRilCall(GsmCdmaConnection gsmCdmaConnection, TelephonyProto.TelephonyCallSession.Event.RilCall rilCall) {
        if (gsmCdmaConnection.isIncoming()) {
            rilCall.type = 2;
        } else {
            rilCall.type = 1;
        }
        switch (gsmCdmaConnection.getState()) {
            case IDLE:
                rilCall.state = 1;
                break;
            case ACTIVE:
                rilCall.state = 2;
                break;
            case HOLDING:
                rilCall.state = 3;
                break;
            case DIALING:
                rilCall.state = 4;
                break;
            case ALERTING:
                rilCall.state = 5;
                break;
            case INCOMING:
                rilCall.state = 6;
                break;
            case WAITING:
                rilCall.state = 7;
                break;
            case DISCONNECTED:
                rilCall.state = 8;
                break;
            case DISCONNECTING:
                rilCall.state = 9;
                break;
            default:
                rilCall.state = 0;
                break;
        }
        rilCall.callEndReason = gsmCdmaConnection.getDisconnectCause();
        rilCall.isMultiparty = gsmCdmaConnection.isMultiparty();
    }

    public void writeRilDial(int i, GsmCdmaConnection gsmCdmaConnection, int i2, UUSInfo uUSInfo) {
        InProgressCallSession inProgressCallSessionStartNewCallSessionIfNeeded = startNewCallSessionIfNeeded(i);
        if (inProgressCallSessionStartNewCallSessionIfNeeded == null) {
            Rlog.e(TAG, "writeRilDial: Call session is missing");
            return;
        }
        TelephonyProto.TelephonyCallSession.Event.RilCall[] rilCallArr = {new TelephonyProto.TelephonyCallSession.Event.RilCall()};
        rilCallArr[0].index = -1;
        convertConnectionToRilCall(gsmCdmaConnection, rilCallArr[0]);
        inProgressCallSessionStartNewCallSessionIfNeeded.addEvent(inProgressCallSessionStartNewCallSessionIfNeeded.startElapsedTimeMs, new CallSessionEventBuilder(6).setRilRequest(1).setRilCalls(rilCallArr));
    }

    public void writeRilCallRing(int i, char[] cArr) {
        InProgressCallSession inProgressCallSessionStartNewCallSessionIfNeeded = startNewCallSessionIfNeeded(i);
        inProgressCallSessionStartNewCallSessionIfNeeded.addEvent(inProgressCallSessionStartNewCallSessionIfNeeded.startElapsedTimeMs, new CallSessionEventBuilder(8));
    }

    public void writeRilHangup(int i, GsmCdmaConnection gsmCdmaConnection, int i2) {
        InProgressCallSession inProgressCallSession = this.mInProgressCallSessions.get(i);
        if (inProgressCallSession == null) {
            Rlog.e(TAG, "writeRilHangup: Call session is missing");
            return;
        }
        TelephonyProto.TelephonyCallSession.Event.RilCall[] rilCallArr = {new TelephonyProto.TelephonyCallSession.Event.RilCall()};
        rilCallArr[0].index = i2;
        convertConnectionToRilCall(gsmCdmaConnection, rilCallArr[0]);
        inProgressCallSession.addEvent(new CallSessionEventBuilder(6).setRilRequest(3).setRilCalls(rilCallArr));
    }

    public void writeRilAnswer(int i, int i2) {
        InProgressCallSession inProgressCallSession = this.mInProgressCallSessions.get(i);
        if (inProgressCallSession == null) {
            Rlog.e(TAG, "writeRilAnswer: Call session is missing");
        } else {
            inProgressCallSession.addEvent(new CallSessionEventBuilder(6).setRilRequest(2).setRilRequestId(i2));
        }
    }

    public void writeRilSrvcc(int i, int i2) {
        InProgressCallSession inProgressCallSession = this.mInProgressCallSessions.get(i);
        if (inProgressCallSession == null) {
            Rlog.e(TAG, "writeRilSrvcc: Call session is missing");
        } else {
            inProgressCallSession.addEvent(new CallSessionEventBuilder(9).setSrvccState(i2 + 1));
        }
    }

    private int toCallSessionRilRequest(int i) {
        if (i == 10) {
            return 1;
        }
        if (i == 36) {
            return 4;
        }
        if (i == 40) {
            return 2;
        }
        if (i != 84) {
            switch (i) {
                case 12:
                case 13:
                case 14:
                    return 3;
                case 15:
                    return 5;
                case 16:
                    return 7;
                default:
                    Rlog.e(TAG, "Unknown RIL request: " + i);
                    return 0;
            }
        }
        return 6;
    }

    private void writeOnSetupDataCallResponse(int i, int i2, int i3, int i4, SetupDataCallResult setupDataCallResult) {
        TelephonyProto.TelephonyEvent.RilSetupDataCallResponse rilSetupDataCallResponse = new TelephonyProto.TelephonyEvent.RilSetupDataCallResponse();
        TelephonyProto.RilDataCall rilDataCall = new TelephonyProto.RilDataCall();
        if (setupDataCallResult != null) {
            rilSetupDataCallResponse.status = setupDataCallResult.status == 0 ? 1 : setupDataCallResult.status;
            rilSetupDataCallResponse.suggestedRetryTimeMillis = setupDataCallResult.suggestedRetryTime;
            rilDataCall.cid = setupDataCallResult.cid;
            if (!TextUtils.isEmpty(setupDataCallResult.type)) {
                rilDataCall.type = toPdpType(setupDataCallResult.type);
            }
            if (!TextUtils.isEmpty(setupDataCallResult.ifname)) {
                rilDataCall.iframe = setupDataCallResult.ifname;
            }
        }
        rilSetupDataCallResponse.call = rilDataCall;
        addTelephonyEvent(new TelephonyEventBuilder(i).setSetupDataCallResponse(rilSetupDataCallResponse).build());
    }

    private void writeOnCallSolicitedResponse(int i, int i2, int i3, int i4) {
        InProgressCallSession inProgressCallSession = this.mInProgressCallSessions.get(i);
        if (inProgressCallSession == null) {
            Rlog.e(TAG, "writeOnCallSolicitedResponse: Call session is missing");
        } else {
            inProgressCallSession.addEvent(new CallSessionEventBuilder(7).setRilRequest(toCallSessionRilRequest(i4)).setRilRequestId(i2).setRilError(i3 + 1));
        }
    }

    private synchronized void writeOnSmsSolicitedResponse(int i, int i2, int i3, SmsResponse smsResponse) {
        InProgressSmsSession inProgressSmsSession = this.mInProgressSmsSessions.get(i);
        if (inProgressSmsSession == null) {
            Rlog.e(TAG, "SMS session is missing");
        } else {
            int i4 = 0;
            if (smsResponse != null) {
                i4 = smsResponse.mErrorCode;
            }
            inProgressSmsSession.addEvent(new SmsSessionEventBuilder(7).setErrorCode(i4).setRilErrno(i3 + 1).setRilRequestId(i2));
            inProgressSmsSession.decreaseExpectedResponse();
            finishSmsSessionIfNeeded(inProgressSmsSession);
        }
    }

    private void writeOnDeactivateDataCallResponse(int i, int i2) {
        addTelephonyEvent(new TelephonyEventBuilder(i).setDeactivateDataCallResponse(i2 + 1).build());
    }

    public void writeOnRilSolicitedResponse(int i, int i2, int i3, int i4, Object obj) {
        switch (i4) {
            case 10:
            case 12:
            case 13:
            case 14:
            case 40:
                writeOnCallSolicitedResponse(i, i2, i3, i4);
                break;
            case 25:
            case 26:
            case 87:
            case 113:
                writeOnSmsSolicitedResponse(i, i2, i3, (SmsResponse) obj);
                break;
            case 27:
                writeOnSetupDataCallResponse(i, i2, i3, i4, (SetupDataCallResult) obj);
                break;
            case 41:
                writeOnDeactivateDataCallResponse(i, i3);
                break;
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$internal$telephony$PhoneConstants$State = new int[PhoneConstants.State.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$PhoneConstants$State[PhoneConstants.State.IDLE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$PhoneConstants$State[PhoneConstants.State.RINGING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$PhoneConstants$State[PhoneConstants.State.OFFHOOK.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            $SwitchMap$com$android$internal$telephony$Call$State = new int[Call.State.values().length];
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.IDLE.ordinal()] = 1;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.ACTIVE.ordinal()] = 2;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.HOLDING.ordinal()] = 3;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.DIALING.ordinal()] = 4;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.ALERTING.ordinal()] = 5;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.INCOMING.ordinal()] = 6;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.WAITING.ordinal()] = 7;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.DISCONNECTED.ordinal()] = 8;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.DISCONNECTING.ordinal()] = 9;
            } catch (NoSuchFieldError e12) {
            }
        }
    }

    public void writePhoneState(int i, PhoneConstants.State state) {
        int i2;
        switch (AnonymousClass1.$SwitchMap$com$android$internal$telephony$PhoneConstants$State[state.ordinal()]) {
            case 1:
                i2 = 1;
                break;
            case 2:
                i2 = 2;
                break;
            case 3:
                i2 = 3;
                break;
            default:
                i2 = 0;
                break;
        }
        InProgressCallSession inProgressCallSession = this.mInProgressCallSessions.get(i);
        if (inProgressCallSession == null) {
            Rlog.e(TAG, "writePhoneState: Call session is missing");
            return;
        }
        inProgressCallSession.setLastKnownPhoneState(i2);
        if (i2 == 1 && !inProgressCallSession.containsCsCalls()) {
            finishCallSession(inProgressCallSession);
        }
        inProgressCallSession.addEvent(new CallSessionEventBuilder(20).setPhoneState(i2));
    }

    private int getCallId(ImsCallSession imsCallSession) {
        if (imsCallSession == null) {
            return -1;
        }
        try {
            return Integer.parseInt(imsCallSession.getCallId());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public void writeImsCallState(int i, ImsCallSession imsCallSession, Call.State state) {
        int i2;
        switch (state) {
            case IDLE:
                i2 = 1;
                break;
            case ACTIVE:
                i2 = 2;
                break;
            case HOLDING:
                i2 = 3;
                break;
            case DIALING:
                i2 = 4;
                break;
            case ALERTING:
                i2 = 5;
                break;
            case INCOMING:
                i2 = 6;
                break;
            case WAITING:
                i2 = 7;
                break;
            case DISCONNECTED:
                i2 = 8;
                break;
            case DISCONNECTING:
                i2 = 9;
                break;
            default:
                i2 = 0;
                break;
        }
        InProgressCallSession inProgressCallSession = this.mInProgressCallSessions.get(i);
        if (inProgressCallSession == null) {
            Rlog.e(TAG, "Call session is missing");
        } else {
            inProgressCallSession.addEvent(new CallSessionEventBuilder(16).setCallIndex(getCallId(imsCallSession)).setCallState(i2));
        }
    }

    public void writeOnImsCallStart(int i, ImsCallSession imsCallSession) {
        startNewCallSessionIfNeeded(i).addEvent(new CallSessionEventBuilder(11).setCallIndex(getCallId(imsCallSession)).setImsCommand(1));
    }

    public void writeOnImsCallReceive(int i, ImsCallSession imsCallSession) {
        startNewCallSessionIfNeeded(i).addEvent(new CallSessionEventBuilder(15).setCallIndex(getCallId(imsCallSession)));
    }

    public void writeOnImsCommand(int i, ImsCallSession imsCallSession, int i2) {
        InProgressCallSession inProgressCallSession = this.mInProgressCallSessions.get(i);
        if (inProgressCallSession == null) {
            Rlog.e(TAG, "Call session is missing");
        } else {
            inProgressCallSession.addEvent(new CallSessionEventBuilder(11).setCallIndex(getCallId(imsCallSession)).setImsCommand(i2));
        }
    }

    private TelephonyProto.ImsReasonInfo toImsReasonInfoProto(ImsReasonInfo imsReasonInfo) {
        TelephonyProto.ImsReasonInfo imsReasonInfo2 = new TelephonyProto.ImsReasonInfo();
        if (imsReasonInfo != null) {
            imsReasonInfo2.reasonCode = imsReasonInfo.getCode();
            imsReasonInfo2.extraCode = imsReasonInfo.getExtraCode();
            String extraMessage = imsReasonInfo.getExtraMessage();
            if (extraMessage != null) {
                imsReasonInfo2.extraMessage = extraMessage;
            }
        }
        return imsReasonInfo2;
    }

    public void writeOnImsCallTerminated(int i, ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
        InProgressCallSession inProgressCallSession = this.mInProgressCallSessions.get(i);
        if (inProgressCallSession == null) {
            Rlog.e(TAG, "Call session is missing");
        } else {
            inProgressCallSession.addEvent(new CallSessionEventBuilder(17).setCallIndex(getCallId(imsCallSession)).setImsReasonInfo(toImsReasonInfoProto(imsReasonInfo)));
        }
    }

    public void writeOnImsCallHandoverEvent(int i, int i2, ImsCallSession imsCallSession, int i3, int i4, ImsReasonInfo imsReasonInfo) {
        InProgressCallSession inProgressCallSession = this.mInProgressCallSessions.get(i);
        if (inProgressCallSession == null) {
            Rlog.e(TAG, "Call session is missing");
        } else {
            inProgressCallSession.addEvent(new CallSessionEventBuilder(i2).setCallIndex(getCallId(imsCallSession)).setSrcAccessTech(i3).setTargetAccessTech(i4).setImsReasonInfo(toImsReasonInfoProto(imsReasonInfo)));
        }
    }

    public synchronized void writeRilSendSms(int i, int i2, int i3, int i4) {
        InProgressSmsSession inProgressSmsSessionStartNewSmsSessionIfNeeded = startNewSmsSessionIfNeeded(i);
        inProgressSmsSessionStartNewSmsSessionIfNeeded.addEvent(new SmsSessionEventBuilder(6).setTech(i3).setRilRequestId(i2).setFormat(i4));
        inProgressSmsSessionStartNewSmsSessionIfNeeded.increaseExpectedResponse();
    }

    public synchronized void writeRilNewSms(int i, int i2, int i3) {
        InProgressSmsSession inProgressSmsSessionStartNewSmsSessionIfNeeded = startNewSmsSessionIfNeeded(i);
        inProgressSmsSessionStartNewSmsSessionIfNeeded.addEvent(new SmsSessionEventBuilder(8).setTech(i2).setFormat(i3));
        finishSmsSessionIfNeeded(inProgressSmsSessionStartNewSmsSessionIfNeeded);
    }

    public synchronized void writeNewCBSms(int i, int i2, int i3, boolean z, boolean z2, int i4) {
        int i5;
        InProgressSmsSession inProgressSmsSessionStartNewSmsSessionIfNeeded = startNewSmsSessionIfNeeded(i);
        if (z) {
            i5 = 2;
        } else if (!z2) {
            i5 = 3;
        } else {
            i5 = 1;
        }
        TelephonyProto.SmsSession.Event.CBMessage cBMessage = new TelephonyProto.SmsSession.Event.CBMessage();
        cBMessage.msgFormat = i2;
        cBMessage.msgPriority = i3 + 1;
        cBMessage.msgType = i5;
        cBMessage.serviceCategory = i4;
        inProgressSmsSessionStartNewSmsSessionIfNeeded.addEvent(new SmsSessionEventBuilder(9).setCellBroadcastMessage(cBMessage));
        finishSmsSessionIfNeeded(inProgressSmsSessionStartNewSmsSessionIfNeeded);
    }

    public void writeNITZEvent(int i, long j) {
        TelephonyProto.TelephonyEvent telephonyEventBuild = new TelephonyEventBuilder(i).setNITZ(j).build();
        addTelephonyEvent(telephonyEventBuild);
        annotateInProgressCallSession(telephonyEventBuild.timestampMillis, i, new CallSessionEventBuilder(21).setNITZ(j));
    }

    public void writeModemRestartEvent(int i, String str) {
        TelephonyProto.TelephonyEvent.ModemRestart modemRestart = new TelephonyProto.TelephonyEvent.ModemRestart();
        String radioVersion = Build.getRadioVersion();
        if (radioVersion != null) {
            modemRestart.basebandVersion = radioVersion;
        }
        if (str != null) {
            modemRestart.reason = str;
        }
        addTelephonyEvent(new TelephonyEventBuilder(i).setModemRestart(modemRestart).build());
    }

    public void writeCarrierIdMatchingEvent(int i, int i2, int i3, String str, String str2) {
        TelephonyProto.TelephonyEvent.CarrierIdMatching carrierIdMatching = new TelephonyProto.TelephonyEvent.CarrierIdMatching();
        TelephonyProto.TelephonyEvent.CarrierIdMatchingResult carrierIdMatchingResult = new TelephonyProto.TelephonyEvent.CarrierIdMatchingResult();
        if (i3 != -1) {
            carrierIdMatchingResult.carrierId = i3;
            if (str2 != null) {
                carrierIdMatchingResult.mccmnc = str;
                carrierIdMatchingResult.gid1 = str2;
            }
        } else if (str != null) {
            carrierIdMatchingResult.mccmnc = str;
        }
        carrierIdMatching.cidTableVersion = i2;
        carrierIdMatching.result = carrierIdMatchingResult;
        addTelephonyEvent(new TelephonyEventBuilder(i).setCarrierIdMatching(carrierIdMatching).build());
    }

    public void writeOnImsCallProgressing(int i, ImsCallSession imsCallSession) {
    }

    public void writeOnImsCallStarted(int i, ImsCallSession imsCallSession) {
    }

    public void writeOnImsCallStartFailed(int i, ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
    }

    public void writeOnImsCallHeld(int i, ImsCallSession imsCallSession) {
    }

    public void writeOnImsCallHoldReceived(int i, ImsCallSession imsCallSession) {
    }

    public void writeOnImsCallHoldFailed(int i, ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
    }

    public void writeOnImsCallResumed(int i, ImsCallSession imsCallSession) {
    }

    public void writeOnImsCallResumeReceived(int i, ImsCallSession imsCallSession) {
    }

    public void writeOnImsCallResumeFailed(int i, ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
    }

    public void writeOnRilTimeoutResponse(int i, int i2, int i3) {
    }
}
