package com.android.server.wifi;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public class BaseWifiDiagnostics {
    public static final byte CONNECTION_EVENT_FAILED = 2;
    public static final byte CONNECTION_EVENT_STARTED = 0;
    public static final byte CONNECTION_EVENT_SUCCEEDED = 1;
    protected String mDriverVersion;
    protected String mFirmwareVersion;
    protected int mSupportedFeatureSet;
    protected final WifiNative mWifiNative;

    public BaseWifiDiagnostics(WifiNative wifiNative) {
        this.mWifiNative = wifiNative;
    }

    public synchronized void startLogging(boolean z) {
        this.mFirmwareVersion = this.mWifiNative.getFirmwareVersion();
        this.mDriverVersion = this.mWifiNative.getDriverVersion();
        this.mSupportedFeatureSet = this.mWifiNative.getSupportedLoggerFeatureSet();
    }

    public synchronized void startPacketLog() {
    }

    public synchronized void stopPacketLog() {
    }

    public synchronized void stopLogging() {
    }

    synchronized void reportConnectionEvent(long j, byte b) {
    }

    public synchronized void captureBugReportData(int i) {
    }

    public synchronized void captureAlertData(int i, byte[] bArr) {
    }

    public synchronized void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        dump(printWriter);
        printWriter.println("*** firmware logging disabled, no debug data ****");
        printWriter.println("set config_wifi_enable_wifi_firmware_debugging to enable");
    }

    public void takeBugReport(String str, String str2) {
    }

    protected synchronized void dump(PrintWriter printWriter) {
        printWriter.println("Chipset information :-----------------------------------------------");
        printWriter.println("FW Version is: " + this.mFirmwareVersion);
        printWriter.println("Driver Version is: " + this.mDriverVersion);
        printWriter.println("Supported Feature set: " + this.mSupportedFeatureSet);
    }
}
