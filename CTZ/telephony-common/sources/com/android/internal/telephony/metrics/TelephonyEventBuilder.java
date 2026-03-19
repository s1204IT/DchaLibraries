package com.android.internal.telephony.metrics;

import android.os.SystemClock;
import com.android.internal.telephony.nano.TelephonyProto;

public class TelephonyEventBuilder {
    private final TelephonyProto.TelephonyEvent mEvent;

    public TelephonyProto.TelephonyEvent build() {
        return this.mEvent;
    }

    public TelephonyEventBuilder(int i) {
        this(SystemClock.elapsedRealtime(), i);
    }

    public TelephonyEventBuilder(long j, int i) {
        this.mEvent = new TelephonyProto.TelephonyEvent();
        this.mEvent.timestampMillis = j;
        this.mEvent.phoneId = i;
    }

    public TelephonyEventBuilder setSettings(TelephonyProto.TelephonySettings telephonySettings) {
        this.mEvent.type = 1;
        this.mEvent.settings = telephonySettings;
        return this;
    }

    public TelephonyEventBuilder setServiceState(TelephonyProto.TelephonyServiceState telephonyServiceState) {
        this.mEvent.type = 2;
        this.mEvent.serviceState = telephonyServiceState;
        return this;
    }

    public TelephonyEventBuilder setImsConnectionState(TelephonyProto.ImsConnectionState imsConnectionState) {
        this.mEvent.type = 3;
        this.mEvent.imsConnectionState = imsConnectionState;
        return this;
    }

    public TelephonyEventBuilder setImsCapabilities(TelephonyProto.ImsCapabilities imsCapabilities) {
        this.mEvent.type = 4;
        this.mEvent.imsCapabilities = imsCapabilities;
        return this;
    }

    public TelephonyEventBuilder setDataStallRecoveryAction(int i) {
        this.mEvent.type = 10;
        this.mEvent.dataStallAction = i;
        return this;
    }

    public TelephonyEventBuilder setSetupDataCall(TelephonyProto.TelephonyEvent.RilSetupDataCall rilSetupDataCall) {
        this.mEvent.type = 5;
        this.mEvent.setupDataCall = rilSetupDataCall;
        return this;
    }

    public TelephonyEventBuilder setSetupDataCallResponse(TelephonyProto.TelephonyEvent.RilSetupDataCallResponse rilSetupDataCallResponse) {
        this.mEvent.type = 6;
        this.mEvent.setupDataCallResponse = rilSetupDataCallResponse;
        return this;
    }

    public TelephonyEventBuilder setDeactivateDataCall(TelephonyProto.TelephonyEvent.RilDeactivateDataCall rilDeactivateDataCall) {
        this.mEvent.type = 8;
        this.mEvent.deactivateDataCall = rilDeactivateDataCall;
        return this;
    }

    public TelephonyEventBuilder setDeactivateDataCallResponse(int i) {
        this.mEvent.type = 9;
        this.mEvent.error = i;
        return this;
    }

    public TelephonyEventBuilder setDataCalls(TelephonyProto.RilDataCall[] rilDataCallArr) {
        this.mEvent.type = 7;
        this.mEvent.dataCalls = rilDataCallArr;
        return this;
    }

    public TelephonyEventBuilder setNITZ(long j) {
        this.mEvent.type = 12;
        this.mEvent.nitzTimestampMillis = j;
        return this;
    }

    public TelephonyEventBuilder setModemRestart(TelephonyProto.TelephonyEvent.ModemRestart modemRestart) {
        this.mEvent.type = 11;
        this.mEvent.modemRestart = modemRestart;
        return this;
    }

    public TelephonyEventBuilder setCarrierIdMatching(TelephonyProto.TelephonyEvent.CarrierIdMatching carrierIdMatching) {
        this.mEvent.type = 13;
        this.mEvent.carrierIdMatching = carrierIdMatching;
        return this;
    }

    public TelephonyEventBuilder setCarrierKeyChange(TelephonyProto.TelephonyEvent.CarrierKeyChange carrierKeyChange) {
        this.mEvent.type = 14;
        this.mEvent.carrierKeyChange = carrierKeyChange;
        return this;
    }
}
