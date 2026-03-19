package com.android.server.wifi;

import android.R;
import android.content.Context;
import android.util.Base64;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.util.ByteArrayRingBuffer;
import com.android.server.wifi.util.StringUtil;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.Deflater;

class WifiDiagnostics extends BaseWifiDiagnostics {
    private static final boolean DBG = false;

    @VisibleForTesting
    public static final String DRIVER_DUMP_SECTION_HEADER = "Driver state dump";

    @VisibleForTesting
    public static final String FIRMWARE_DUMP_SECTION_HEADER = "FW Memory dump";
    public static final int MAX_ALERT_REPORTS = 1;
    public static final int MAX_BUG_REPORTS = 4;
    public static final int REPORT_REASON_ASSOC_FAILURE = 1;
    public static final int REPORT_REASON_AUTH_FAILURE = 2;
    public static final int REPORT_REASON_AUTOROAM_FAILURE = 3;
    public static final int REPORT_REASON_DHCP_FAILURE = 4;
    public static final int REPORT_REASON_NONE = 0;
    public static final int REPORT_REASON_SCAN_FAILURE = 6;
    public static final int REPORT_REASON_UNEXPECTED_DISCONNECT = 5;
    public static final int REPORT_REASON_USER_ACTION = 7;
    public static final int REPORT_REASON_WIFINATIVE_FAILURE = 8;
    public static final int RING_BUFFER_FLAG_HAS_ASCII_ENTRIES = 2;
    public static final int RING_BUFFER_FLAG_HAS_BINARY_ENTRIES = 1;
    public static final int RING_BUFFER_FLAG_HAS_PER_PACKET_ENTRIES = 4;
    private static final String TAG = "WifiDiags";
    public static final int VERBOSE_DETAILED_LOG_WITH_WAKEUP = 3;
    public static final int VERBOSE_LOG_WITH_WAKEUP = 2;
    public static final int VERBOSE_NORMAL_LOG = 1;
    public static final int VERBOSE_NO_LOG = 0;
    private final int RING_BUFFER_BYTE_LIMIT_LARGE;
    private final int RING_BUFFER_BYTE_LIMIT_SMALL;
    private final BuildProperties mBuildProperties;
    private final WifiNative.WifiLoggerEventHandler mHandler;
    private boolean mIsLoggingEventHandlerRegistered;
    private final Runtime mJavaRuntime;
    private final LimitedCircularArray<BugReport> mLastAlerts;
    private final LimitedCircularArray<BugReport> mLastBugReports;
    private final LastMileLogger mLastMileLogger;
    private final WifiLog mLog;
    private int mLogLevel;
    private int mMaxRingBufferSizeBytes;
    private ArrayList<WifiNative.FateReport> mPacketFatesForLastFailure;
    private WifiNative.RingBufferStatus mPerPacketRingBuffer;
    private final HashMap<String, ByteArrayRingBuffer> mRingBufferData;
    private WifiNative.RingBufferStatus[] mRingBuffers;
    private WifiInjector mWifiInjector;
    private final WifiMetrics mWifiMetrics;
    private static final int[] MinWakeupIntervals = {0, 3600, 60, 10};
    private static final int[] MinBufferSizes = {0, 16384, 16384, 65536};

    public WifiDiagnostics(Context context, WifiInjector wifiInjector, WifiNative wifiNative, BuildProperties buildProperties, LastMileLogger lastMileLogger) {
        super(wifiNative);
        this.mLogLevel = 0;
        this.mLastAlerts = new LimitedCircularArray<>(1);
        this.mLastBugReports = new LimitedCircularArray<>(4);
        this.mRingBufferData = new HashMap<>();
        this.mHandler = new WifiNative.WifiLoggerEventHandler() {
            @Override
            public void onRingBufferData(WifiNative.RingBufferStatus ringBufferStatus, byte[] bArr) {
                WifiDiagnostics.this.onRingBufferData(ringBufferStatus, bArr);
            }

            @Override
            public void onWifiAlert(int i, byte[] bArr) {
                WifiDiagnostics.this.onWifiAlert(i, bArr);
            }
        };
        this.RING_BUFFER_BYTE_LIMIT_SMALL = context.getResources().getInteger(R.integer.config_maxShortcutTargetsPerApp) * 1024;
        this.RING_BUFFER_BYTE_LIMIT_LARGE = context.getResources().getInteger(R.integer.config_maxUiWidth) * 1024;
        this.mBuildProperties = buildProperties;
        this.mIsLoggingEventHandlerRegistered = DBG;
        this.mMaxRingBufferSizeBytes = this.RING_BUFFER_BYTE_LIMIT_SMALL;
        this.mLog = wifiInjector.makeLog(TAG);
        this.mLastMileLogger = lastMileLogger;
        this.mJavaRuntime = wifiInjector.getJavaRuntime();
        this.mWifiMetrics = wifiInjector.getWifiMetrics();
        this.mWifiInjector = wifiInjector;
    }

    @Override
    public synchronized void startLogging(boolean z) {
        this.mFirmwareVersion = this.mWifiNative.getFirmwareVersion();
        this.mDriverVersion = this.mWifiNative.getDriverVersion();
        this.mSupportedFeatureSet = this.mWifiNative.getSupportedLoggerFeatureSet();
        if (!this.mIsLoggingEventHandlerRegistered) {
            this.mIsLoggingEventHandlerRegistered = this.mWifiNative.setLoggingEventHandler(this.mHandler);
        }
        if (z) {
            this.mLogLevel = 2;
            this.mMaxRingBufferSizeBytes = this.RING_BUFFER_BYTE_LIMIT_LARGE;
        } else {
            this.mLogLevel = 1;
            this.mMaxRingBufferSizeBytes = enableVerboseLoggingForDogfood() ? this.RING_BUFFER_BYTE_LIMIT_LARGE : this.RING_BUFFER_BYTE_LIMIT_SMALL;
            clearVerboseLogs();
        }
        if (this.mRingBuffers == null) {
            fetchRingBuffers();
        }
        if (this.mRingBuffers != null) {
            stopLoggingAllBuffers();
            resizeRingBuffers();
            startLoggingAllExceptPerPacketBuffers();
        }
        if (!this.mWifiNative.startPktFateMonitoring(this.mWifiNative.getClientInterfaceName())) {
            this.mLog.wC("Failed to start packet fate monitoring");
        }
    }

    @Override
    public synchronized void startPacketLog() {
        if (this.mPerPacketRingBuffer != null) {
            startLoggingRingBuffer(this.mPerPacketRingBuffer);
        }
    }

    @Override
    public synchronized void stopPacketLog() {
        if (this.mPerPacketRingBuffer != null) {
            stopLoggingRingBuffer(this.mPerPacketRingBuffer);
        }
    }

    @Override
    public synchronized void stopLogging() {
        if (this.mIsLoggingEventHandlerRegistered) {
            if (!this.mWifiNative.resetLogHandler()) {
                this.mLog.wC("Fail to reset log handler");
            }
            this.mIsLoggingEventHandlerRegistered = DBG;
        }
        if (this.mLogLevel != 0) {
            stopLoggingAllBuffers();
            this.mRingBuffers = null;
            this.mLogLevel = 0;
        }
    }

    @Override
    synchronized void reportConnectionEvent(long j, byte b) {
        this.mLastMileLogger.reportConnectionEvent(j, b);
        if (b == 2) {
            this.mPacketFatesForLastFailure = fetchPacketFates();
        }
    }

    @Override
    public synchronized void captureBugReportData(int i) {
        this.mLastBugReports.addLast(captureBugreport(i, isVerboseLoggingEnabled()));
    }

    @Override
    public synchronized void captureAlertData(int i, byte[] bArr) {
        BugReport bugReportCaptureBugreport = captureBugreport(i, isVerboseLoggingEnabled());
        bugReportCaptureBugreport.alertData = bArr;
        this.mLastAlerts.addLast(bugReportCaptureBugreport);
    }

    @Override
    public synchronized void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        super.dump(printWriter);
        for (int i = 0; i < this.mLastAlerts.size(); i++) {
            printWriter.println("--------------------------------------------------------------------");
            printWriter.println("Alert dump " + i);
            printWriter.print(this.mLastAlerts.get(i));
            printWriter.println("--------------------------------------------------------------------");
        }
        for (int i2 = 0; i2 < this.mLastBugReports.size(); i2++) {
            printWriter.println("--------------------------------------------------------------------");
            printWriter.println("Bug dump " + i2);
            printWriter.print(this.mLastBugReports.get(i2));
            printWriter.println("--------------------------------------------------------------------");
        }
        dumpPacketFates(printWriter);
        this.mLastMileLogger.dump(printWriter);
        printWriter.println("--------------------------------------------------------------------");
    }

    @Override
    public void takeBugReport(String str, String str2) {
        if (this.mBuildProperties.isUserBuild()) {
            return;
        }
        try {
            this.mWifiInjector.getActivityManagerService().requestWifiBugReport(str, str2);
        } catch (Exception e) {
            this.mLog.err("error taking bugreport: %").c(e.getClass().getName()).flush();
        }
    }

    class BugReport {
        byte[] alertData;
        int errorCode;
        byte[] fwMemoryDump;
        LimitedCircularArray<String> kernelLogLines;
        long kernelTimeNanos;
        ArrayList<String> logcatLines;
        byte[] mDriverStateDump;
        HashMap<String, byte[][]> ringBuffers = new HashMap<>();
        long systemTimeMs;

        BugReport() {
        }

        void clearVerboseLogs() {
            this.fwMemoryDump = null;
            this.mDriverStateDump = null;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(this.systemTimeMs);
            sb.append("system time = ");
            sb.append(String.format("%tm-%td %tH:%tM:%tS.%tL", calendar, calendar, calendar, calendar, calendar, calendar));
            sb.append("\n");
            long j = this.kernelTimeNanos / 1000000;
            sb.append("kernel time = ");
            sb.append(j / 1000);
            sb.append(".");
            sb.append(j % 1000);
            sb.append("\n");
            if (this.alertData == null) {
                sb.append("reason = ");
                sb.append(this.errorCode);
                sb.append("\n");
            } else {
                sb.append("errorCode = ");
                sb.append(this.errorCode);
                sb.append("data \n");
                sb.append(WifiDiagnostics.this.compressToBase64(this.alertData));
                sb.append("\n");
            }
            if (this.kernelLogLines != null) {
                sb.append("kernel log: \n");
                for (int i = 0; i < this.kernelLogLines.size(); i++) {
                    sb.append(this.kernelLogLines.get(i));
                    sb.append("\n");
                }
                sb.append("\n");
            }
            if (this.logcatLines != null) {
                sb.append("system log: \n");
                for (int i2 = 0; i2 < this.logcatLines.size(); i2++) {
                    sb.append(this.logcatLines.get(i2));
                    sb.append("\n");
                }
                sb.append("\n");
            }
            for (Map.Entry<String, byte[][]> entry : this.ringBuffers.entrySet()) {
                String key = entry.getKey();
                byte[][] value = entry.getValue();
                sb.append("ring-buffer = ");
                sb.append(key);
                sb.append("\n");
                int length = 0;
                for (byte[] bArr : value) {
                    length += bArr.length;
                }
                byte[] bArr2 = new byte[length];
                int length2 = 0;
                for (int i3 = 0; i3 < value.length; i3++) {
                    System.arraycopy(value[i3], 0, bArr2, length2, value[i3].length);
                    length2 += value[i3].length;
                }
                sb.append(WifiDiagnostics.this.compressToBase64(bArr2));
                sb.append("\n");
            }
            if (this.fwMemoryDump != null) {
                sb.append(WifiDiagnostics.FIRMWARE_DUMP_SECTION_HEADER);
                sb.append("\n");
                sb.append(WifiDiagnostics.this.compressToBase64(this.fwMemoryDump));
                sb.append("\n");
            }
            if (this.mDriverStateDump != null) {
                sb.append(WifiDiagnostics.DRIVER_DUMP_SECTION_HEADER);
                if (StringUtil.isAsciiPrintable(this.mDriverStateDump)) {
                    sb.append(" (ascii)\n");
                    sb.append(new String(this.mDriverStateDump, Charset.forName("US-ASCII")));
                    sb.append("\n");
                } else {
                    sb.append(" (base64)\n");
                    sb.append(WifiDiagnostics.this.compressToBase64(this.mDriverStateDump));
                }
            }
            return sb.toString();
        }
    }

    class LimitedCircularArray<E> {
        private ArrayList<E> mArrayList;
        private int mMax;

        LimitedCircularArray(int i) {
            this.mArrayList = new ArrayList<>(i);
            this.mMax = i;
        }

        public final void addLast(E e) {
            if (this.mArrayList.size() >= this.mMax) {
                this.mArrayList.remove(0);
            }
            this.mArrayList.add(e);
        }

        public final int size() {
            return this.mArrayList.size();
        }

        public final E get(int i) {
            return this.mArrayList.get(i);
        }
    }

    synchronized void onRingBufferData(WifiNative.RingBufferStatus ringBufferStatus, byte[] bArr) {
        ByteArrayRingBuffer byteArrayRingBuffer = this.mRingBufferData.get(ringBufferStatus.name);
        if (byteArrayRingBuffer != null) {
            byteArrayRingBuffer.appendBuffer(bArr);
        }
    }

    synchronized void onWifiAlert(int i, byte[] bArr) {
        captureAlertData(i, bArr);
        this.mWifiMetrics.incrementAlertReasonCount(i);
    }

    private boolean isVerboseLoggingEnabled() {
        if (this.mLogLevel > 1) {
            return true;
        }
        return DBG;
    }

    private void clearVerboseLogs() {
        this.mPacketFatesForLastFailure = null;
        for (int i = 0; i < this.mLastAlerts.size(); i++) {
            this.mLastAlerts.get(i).clearVerboseLogs();
        }
        for (int i2 = 0; i2 < this.mLastBugReports.size(); i2++) {
            this.mLastBugReports.get(i2).clearVerboseLogs();
        }
    }

    private boolean fetchRingBuffers() {
        if (this.mRingBuffers != null) {
            return true;
        }
        this.mRingBuffers = this.mWifiNative.getRingBufferStatus();
        if (this.mRingBuffers != null) {
            for (WifiNative.RingBufferStatus ringBufferStatus : this.mRingBuffers) {
                if (!this.mRingBufferData.containsKey(ringBufferStatus.name)) {
                    this.mRingBufferData.put(ringBufferStatus.name, new ByteArrayRingBuffer(this.mMaxRingBufferSizeBytes));
                }
                if ((ringBufferStatus.flag & 4) != 0) {
                    this.mPerPacketRingBuffer = ringBufferStatus;
                }
            }
        } else {
            this.mLog.wC("no ring buffers found");
        }
        if (this.mRingBuffers != null) {
            return true;
        }
        return DBG;
    }

    private void resizeRingBuffers() {
        Iterator<ByteArrayRingBuffer> it = this.mRingBufferData.values().iterator();
        while (it.hasNext()) {
            it.next().resize(this.mMaxRingBufferSizeBytes);
        }
    }

    private boolean startLoggingAllExceptPerPacketBuffers() {
        if (this.mRingBuffers == null) {
            return DBG;
        }
        for (WifiNative.RingBufferStatus ringBufferStatus : this.mRingBuffers) {
            if ((ringBufferStatus.flag & 4) == 0) {
                startLoggingRingBuffer(ringBufferStatus);
            }
        }
        return true;
    }

    private boolean startLoggingRingBuffer(WifiNative.RingBufferStatus ringBufferStatus) {
        if (!this.mWifiNative.startLoggingRingBuffer(this.mLogLevel, 0, MinWakeupIntervals[this.mLogLevel], MinBufferSizes[this.mLogLevel], ringBufferStatus.name)) {
            return DBG;
        }
        return true;
    }

    private boolean stopLoggingRingBuffer(WifiNative.RingBufferStatus ringBufferStatus) {
        this.mWifiNative.startLoggingRingBuffer(0, 0, 0, 0, ringBufferStatus.name);
        return true;
    }

    private boolean stopLoggingAllBuffers() {
        if (this.mRingBuffers != null) {
            for (WifiNative.RingBufferStatus ringBufferStatus : this.mRingBuffers) {
                stopLoggingRingBuffer(ringBufferStatus);
            }
            return true;
        }
        return true;
    }

    private boolean enableVerboseLoggingForDogfood() {
        return true;
    }

    private BugReport captureBugreport(int i, boolean z) {
        BugReport bugReport = new BugReport();
        bugReport.errorCode = i;
        bugReport.systemTimeMs = System.currentTimeMillis();
        bugReport.kernelTimeNanos = System.nanoTime();
        if (this.mRingBuffers != null) {
            for (WifiNative.RingBufferStatus ringBufferStatus : this.mRingBuffers) {
                this.mWifiNative.getRingBufferData(ringBufferStatus.name);
                ByteArrayRingBuffer byteArrayRingBuffer = this.mRingBufferData.get(ringBufferStatus.name);
                byte[][] bArr = new byte[byteArrayRingBuffer.getNumBuffers()][];
                for (int i2 = 0; i2 < byteArrayRingBuffer.getNumBuffers(); i2++) {
                    bArr[i2] = (byte[]) byteArrayRingBuffer.getBuffer(i2).clone();
                }
                bugReport.ringBuffers.put(ringBufferStatus.name, bArr);
            }
        }
        bugReport.logcatLines = getLogcat(127);
        bugReport.kernelLogLines = getKernelLog(127);
        if (z) {
            bugReport.fwMemoryDump = this.mWifiNative.getFwMemoryDump();
            bugReport.mDriverStateDump = this.mWifiNative.getDriverStateDump();
        }
        return bugReport;
    }

    @VisibleForTesting
    LimitedCircularArray<BugReport> getBugReports() {
        return this.mLastBugReports;
    }

    @VisibleForTesting
    LimitedCircularArray<BugReport> getAlertReports() {
        return this.mLastAlerts;
    }

    private String compressToBase64(byte[] bArr) {
        Deflater deflater = new Deflater();
        deflater.setLevel(1);
        deflater.setInput(bArr);
        deflater.finish();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(bArr.length);
        byte[] bArr2 = new byte[1024];
        while (!deflater.finished()) {
            byteArrayOutputStream.write(bArr2, 0, deflater.deflate(bArr2));
        }
        try {
            deflater.end();
            byteArrayOutputStream.close();
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            if (byteArray.length < bArr.length) {
                bArr = byteArray;
            }
            return Base64.encodeToString(bArr, 0);
        } catch (IOException e) {
            this.mLog.wC("ByteArrayOutputStream close error");
            return Base64.encodeToString(bArr, 0);
        }
    }

    private ArrayList<String> getLogcat(int i) {
        Process processExec;
        BufferedReader bufferedReader;
        ArrayList<String> arrayList = new ArrayList<>(i);
        try {
            processExec = this.mJavaRuntime.exec(String.format("logcat -t %d", Integer.valueOf(i)));
            bufferedReader = new BufferedReader(new InputStreamReader(processExec.getInputStream()));
        } catch (IOException | InterruptedException e) {
            this.mLog.dump("Exception while capturing logcat: %").c(e.toString()).flush();
        }
        while (true) {
            String line = bufferedReader.readLine();
            if (line == null) {
                break;
            }
            arrayList.add(line);
            return arrayList;
        }
        BufferedReader bufferedReader2 = new BufferedReader(new InputStreamReader(processExec.getErrorStream()));
        while (true) {
            String line2 = bufferedReader2.readLine();
            if (line2 == null) {
                break;
            }
            arrayList.add(line2);
            return arrayList;
        }
        processExec.waitFor();
        return arrayList;
    }

    private LimitedCircularArray<String> getKernelLog(int i) {
        LimitedCircularArray<String> limitedCircularArray = new LimitedCircularArray<>(i);
        for (String str : this.mWifiNative.readKernelLog().split("\n")) {
            limitedCircularArray.addLast(str);
        }
        return limitedCircularArray;
    }

    private ArrayList<WifiNative.FateReport> fetchPacketFates() {
        ArrayList<WifiNative.FateReport> arrayList = new ArrayList<>();
        WifiNative.TxFateReport[] txFateReportArr = new WifiNative.TxFateReport[32];
        if (this.mWifiNative.getTxPktFates(this.mWifiNative.getClientInterfaceName(), txFateReportArr)) {
            for (int i = 0; i < txFateReportArr.length && txFateReportArr[i] != null; i++) {
                arrayList.add(txFateReportArr[i]);
            }
        }
        WifiNative.RxFateReport[] rxFateReportArr = new WifiNative.RxFateReport[32];
        if (this.mWifiNative.getRxPktFates(this.mWifiNative.getClientInterfaceName(), rxFateReportArr)) {
            for (int i2 = 0; i2 < rxFateReportArr.length && rxFateReportArr[i2] != null; i2++) {
                arrayList.add(rxFateReportArr[i2]);
            }
        }
        Collections.sort(arrayList, new Comparator<WifiNative.FateReport>() {
            @Override
            public int compare(WifiNative.FateReport fateReport, WifiNative.FateReport fateReport2) {
                return Long.compare(fateReport.mDriverTimestampUSec, fateReport2.mDriverTimestampUSec);
            }
        });
        return arrayList;
    }

    private void dumpPacketFates(PrintWriter printWriter) {
        dumpPacketFatesInternal(printWriter, "Last failed connection fates", this.mPacketFatesForLastFailure, isVerboseLoggingEnabled());
        dumpPacketFatesInternal(printWriter, "Latest fates", fetchPacketFates(), isVerboseLoggingEnabled());
    }

    private static void dumpPacketFatesInternal(PrintWriter printWriter, String str, ArrayList<WifiNative.FateReport> arrayList, boolean z) {
        if (arrayList == null) {
            printWriter.format("No fates fetched for \"%s\"\n", str);
            return;
        }
        if (arrayList.size() == 0) {
            printWriter.format("HAL provided zero fates for \"%s\"\n", str);
            return;
        }
        printWriter.format("--------------------- %s ----------------------\n", str);
        StringBuilder sb = new StringBuilder();
        printWriter.print(WifiNative.FateReport.getTableHeader());
        for (WifiNative.FateReport fateReport : arrayList) {
            printWriter.print(fateReport.toTableRowString());
            if (z) {
                sb.append(fateReport.toVerboseStringWithPiiAllowed());
                sb.append("\n");
            }
        }
        if (z) {
            printWriter.format("\n>>> VERBOSE PACKET FATE DUMP <<<\n\n", new Object[0]);
            printWriter.print(sb.toString());
        }
        printWriter.println("--------------------------------------------------------------------");
    }
}
