package com.android.common;

import android.util.EventLog;

public class GoogleLogTags {
    public static final int C2DM = 204005;
    public static final int EXP_DET_SNET = 206003;
    public static final int GLS_ACCOUNT_SAVED = 205009;
    public static final int GLS_ACCOUNT_TRIED = 205008;
    public static final int GLS_AUTHENTICATE = 205010;
    public static final int GOOGLE_HTTP_REQUEST = 203002;
    public static final int GOOGLE_MAIL_SWITCH = 205011;
    public static final int GTALKSERVICE = 204001;
    public static final int GTALK_CONNECTION = 204002;
    public static final int GTALK_CONN_CLOSE = 204003;
    public static final int GTALK_HEARTBEAT_RESET = 204004;
    public static final int METRICS_HEARTBEAT = 208000;
    public static final int SETUP_COMPLETED = 205007;
    public static final int SETUP_IO_ERROR = 205003;
    public static final int SETUP_NO_DATA_NETWORK = 205006;
    public static final int SETUP_REQUIRED_CAPTCHA = 205002;
    public static final int SETUP_RETRIES_EXHAUSTED = 205005;
    public static final int SETUP_SERVER_ERROR = 205004;
    public static final int SETUP_SERVER_TIMEOUT = 205001;
    public static final int SNET = 206001;
    public static final int SYNC_DETAILS = 203001;
    public static final int SYSTEM_UPDATE = 201001;
    public static final int SYSTEM_UPDATE_USER = 201002;
    public static final int TRANSACTION_EVENT = 202901;
    public static final int VENDING_RECONSTRUCT = 202001;

    private GoogleLogTags() {
    }

    public static void writeSystemUpdate(int i, int i2, long j, String str) {
        EventLog.writeEvent(SYSTEM_UPDATE, Integer.valueOf(i), Integer.valueOf(i2), Long.valueOf(j), str);
    }

    public static void writeSystemUpdateUser(String str) {
        EventLog.writeEvent(SYSTEM_UPDATE_USER, str);
    }

    public static void writeVendingReconstruct(int i) {
        EventLog.writeEvent(VENDING_RECONSTRUCT, i);
    }

    public static void writeTransactionEvent(String str) {
        EventLog.writeEvent(TRANSACTION_EVENT, str);
    }

    public static void writeSyncDetails(String str, int i, int i2, String str2) {
        EventLog.writeEvent(SYNC_DETAILS, str, Integer.valueOf(i), Integer.valueOf(i2), str2);
    }

    public static void writeGoogleHttpRequest(long j, int i, String str, int i2) {
        EventLog.writeEvent(GOOGLE_HTTP_REQUEST, Long.valueOf(j), Integer.valueOf(i), str, Integer.valueOf(i2));
    }

    public static void writeGtalkservice(int i) {
        EventLog.writeEvent(GTALKSERVICE, i);
    }

    public static void writeGtalkConnection(int i) {
        EventLog.writeEvent(GTALK_CONNECTION, i);
    }

    public static void writeGtalkConnClose(int i, int i2) {
        EventLog.writeEvent(GTALK_CONN_CLOSE, Integer.valueOf(i), Integer.valueOf(i2));
    }

    public static void writeGtalkHeartbeatReset(int i, String str) {
        EventLog.writeEvent(GTALK_HEARTBEAT_RESET, Integer.valueOf(i), str);
    }

    public static void writeC2Dm(int i, String str, int i2, int i3) {
        EventLog.writeEvent(C2DM, Integer.valueOf(i), str, Integer.valueOf(i2), Integer.valueOf(i3));
    }

    public static void writeSetupServerTimeout() {
        EventLog.writeEvent(SETUP_SERVER_TIMEOUT, new Object[0]);
    }

    public static void writeSetupRequiredCaptcha(String str) {
        EventLog.writeEvent(SETUP_REQUIRED_CAPTCHA, str);
    }

    public static void writeSetupIoError(String str) {
        EventLog.writeEvent(SETUP_IO_ERROR, str);
    }

    public static void writeSetupServerError() {
        EventLog.writeEvent(SETUP_SERVER_ERROR, new Object[0]);
    }

    public static void writeSetupRetriesExhausted() {
        EventLog.writeEvent(SETUP_RETRIES_EXHAUSTED, new Object[0]);
    }

    public static void writeSetupNoDataNetwork() {
        EventLog.writeEvent(SETUP_NO_DATA_NETWORK, new Object[0]);
    }

    public static void writeSetupCompleted() {
        EventLog.writeEvent(SETUP_COMPLETED, new Object[0]);
    }

    public static void writeGlsAccountTried(int i) {
        EventLog.writeEvent(GLS_ACCOUNT_TRIED, i);
    }

    public static void writeGlsAccountSaved(int i) {
        EventLog.writeEvent(GLS_ACCOUNT_SAVED, i);
    }

    public static void writeGlsAuthenticate(int i, String str) {
        EventLog.writeEvent(GLS_AUTHENTICATE, Integer.valueOf(i), str);
    }

    public static void writeGoogleMailSwitch(int i) {
        EventLog.writeEvent(GOOGLE_MAIL_SWITCH, i);
    }

    public static void writeSnet(String str) {
        EventLog.writeEvent(SNET, str);
    }

    public static void writeExpDetSnet(String str) {
        EventLog.writeEvent(EXP_DET_SNET, str);
    }

    public static void writeMetricsHeartbeat() {
        EventLog.writeEvent(METRICS_HEARTBEAT, new Object[0]);
    }
}
