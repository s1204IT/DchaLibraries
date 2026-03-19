package com.android.server.wifi;

import android.util.Base64;
import android.util.Log;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.util.ByteArrayRingBuffer;
import com.android.server.wifi.util.InformationElementUtil;
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
import java.util.Map;
import java.util.zip.Deflater;

class WifiLogger extends BaseWifiLogger {
    private static final boolean DBG = false;
    public static final String DRIVER_DUMP_SECTION_HEADER = "Driver state dump";
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
    public static final int RING_BUFFER_BYTE_LIMIT_LARGE = 1048576;
    public static final int RING_BUFFER_BYTE_LIMIT_SMALL = 32768;
    public static final int RING_BUFFER_FLAG_HAS_ASCII_ENTRIES = 2;
    public static final int RING_BUFFER_FLAG_HAS_BINARY_ENTRIES = 1;
    public static final int RING_BUFFER_FLAG_HAS_PER_PACKET_ENTRIES = 4;
    private static final String TAG = "WifiLogger";
    public static final int VERBOSE_DETAILED_LOG_WITH_WAKEUP = 3;
    public static final int VERBOSE_LOG_WITH_WAKEUP = 2;
    public static final int VERBOSE_NORMAL_LOG = 1;
    public static final int VERBOSE_NO_LOG = 0;
    private final BuildProperties mBuildProperties;
    private ArrayList<WifiNative.FateReport> mPacketFatesForLastFailure;
    private WifiNative.RingBufferStatus mPerPacketRingBuffer;
    private WifiNative.RingBufferStatus[] mRingBuffers;
    private final WifiNative mWifiNative;
    private WifiStateMachine mWifiStateMachine;
    private static final int[] MinWakeupIntervals = {0, 3600, 60, 10};
    private static final int[] MinBufferSizes = {0, 16384, 16384, 65536};
    private int mLogLevel = 0;
    private int mMaxRingBufferSizeBytes = RING_BUFFER_BYTE_LIMIT_SMALL;
    private final LimitedCircularArray<BugReport> mLastAlerts = new LimitedCircularArray<>(1);
    private final LimitedCircularArray<BugReport> mLastBugReports = new LimitedCircularArray<>(4);
    private final HashMap<String, ByteArrayRingBuffer> mRingBufferData = new HashMap<>();
    private final WifiNative.WifiLoggerEventHandler mHandler = new WifiNative.WifiLoggerEventHandler() {
        @Override
        public void onRingBufferData(WifiNative.RingBufferStatus status, byte[] buffer) {
            WifiLogger.this.onRingBufferData(status, buffer);
        }

        @Override
        public void onWifiAlert(int errorCode, byte[] buffer) {
            WifiLogger.this.onWifiAlert(errorCode, buffer);
        }
    };
    private boolean mIsLoggingEventHandlerRegistered = false;

    public WifiLogger(WifiStateMachine wifiStateMachine, WifiNative wifiNative, BuildProperties buildProperties) {
        this.mWifiStateMachine = wifiStateMachine;
        this.mWifiNative = wifiNative;
        this.mBuildProperties = buildProperties;
    }

    @Override
    public synchronized void startLogging(boolean verboseEnabled) {
        int i = RING_BUFFER_BYTE_LIMIT_LARGE;
        synchronized (this) {
            this.mFirmwareVersion = this.mWifiNative.getFirmwareVersion();
            this.mDriverVersion = this.mWifiNative.getDriverVersion();
            this.mSupportedFeatureSet = this.mWifiNative.getSupportedLoggerFeatureSet();
            if (!this.mIsLoggingEventHandlerRegistered) {
                this.mIsLoggingEventHandlerRegistered = this.mWifiNative.setLoggingEventHandler(this.mHandler);
            }
            if (verboseEnabled) {
                this.mLogLevel = 2;
                this.mMaxRingBufferSizeBytes = RING_BUFFER_BYTE_LIMIT_LARGE;
            } else {
                this.mLogLevel = 1;
                if (!enableVerboseLoggingForDogfood()) {
                    i = RING_BUFFER_BYTE_LIMIT_SMALL;
                }
                this.mMaxRingBufferSizeBytes = i;
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
            if (!this.mWifiNative.startPktFateMonitoring()) {
                Log.e(TAG, "Failed to start packet fate monitoring");
            }
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
                Log.e(TAG, "Fail to reset log handler");
            }
            this.mIsLoggingEventHandlerRegistered = false;
        }
        if (this.mLogLevel != 0) {
            stopLoggingAllBuffers();
            this.mRingBuffers = null;
            this.mLogLevel = 0;
        }
    }

    @Override
    synchronized void reportConnectionFailure() {
        this.mPacketFatesForLastFailure = fetchPacketFates();
    }

    @Override
    public synchronized void captureBugReportData(int reason) {
        BugReport report = captureBugreport(reason, isVerboseLoggingEnabled());
        this.mLastBugReports.addLast(report);
    }

    @Override
    public synchronized void captureAlertData(int errorCode, byte[] alertData) {
        BugReport report = captureBugreport(errorCode, isVerboseLoggingEnabled());
        report.alertData = alertData;
        this.mLastAlerts.addLast(report);
    }

    @Override
    public synchronized void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(pw);
        for (int i = 0; i < this.mLastAlerts.size(); i++) {
            pw.println("--------------------------------------------------------------------");
            pw.println("Alert dump " + i);
            pw.print(this.mLastAlerts.get(i));
            pw.println("--------------------------------------------------------------------");
        }
        for (int i2 = 0; i2 < this.mLastBugReports.size(); i2++) {
            pw.println("--------------------------------------------------------------------");
            pw.println("Bug dump " + i2);
            pw.print(this.mLastBugReports.get(i2));
            pw.println("--------------------------------------------------------------------");
        }
        dumpPacketFates(pw);
        pw.println("--------------------------------------------------------------------");
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
            StringBuilder builder = new StringBuilder();
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(this.systemTimeMs);
            builder.append("system time = ").append(String.format("%tm-%td %tH:%tM:%tS.%tL", c, c, c, c, c, c)).append("\n");
            long kernelTimeMs = this.kernelTimeNanos / 1000000;
            builder.append("kernel time = ").append(kernelTimeMs / 1000).append(".").append(kernelTimeMs % 1000).append("\n");
            if (this.alertData == null) {
                builder.append("reason = ").append(this.errorCode).append("\n");
            } else {
                builder.append("errorCode = ").append(this.errorCode);
                builder.append("data \n");
                builder.append(WifiLogger.compressToBase64(this.alertData)).append("\n");
            }
            if (this.kernelLogLines != null) {
                builder.append("kernel log: \n");
                for (int i = 0; i < this.kernelLogLines.size(); i++) {
                    builder.append(this.kernelLogLines.get(i)).append("\n");
                }
                builder.append("\n");
            }
            if (this.logcatLines != null) {
                builder.append("system log: \n");
                for (int i2 = 0; i2 < this.logcatLines.size(); i2++) {
                    builder.append(this.logcatLines.get(i2)).append("\n");
                }
                builder.append("\n");
            }
            for (Map.Entry<String, byte[][]> e : this.ringBuffers.entrySet()) {
                String ringName = e.getKey();
                byte[][] buffers = e.getValue();
                builder.append("ring-buffer = ").append(ringName).append("\n");
                int size = 0;
                for (byte[] bArr : buffers) {
                    size += bArr.length;
                }
                byte[] buffer = new byte[size];
                int index = 0;
                for (int i3 = 0; i3 < buffers.length; i3++) {
                    System.arraycopy(buffers[i3], 0, buffer, index, buffers[i3].length);
                    index += buffers[i3].length;
                }
                builder.append(WifiLogger.compressToBase64(buffer));
                builder.append("\n");
            }
            if (this.fwMemoryDump != null) {
                builder.append(WifiLogger.FIRMWARE_DUMP_SECTION_HEADER);
                builder.append("\n");
                builder.append(WifiLogger.compressToBase64(this.fwMemoryDump));
                builder.append("\n");
            }
            if (this.mDriverStateDump != null) {
                builder.append(WifiLogger.DRIVER_DUMP_SECTION_HEADER);
                if (StringUtil.isAsciiPrintable(this.mDriverStateDump)) {
                    builder.append(" (ascii)\n");
                    builder.append(new String(this.mDriverStateDump, Charset.forName("US-ASCII")));
                    builder.append("\n");
                } else {
                    builder.append(" (base64)\n");
                    builder.append(WifiLogger.compressToBase64(this.mDriverStateDump));
                }
            }
            return builder.toString();
        }
    }

    class LimitedCircularArray<E> {
        private ArrayList<E> mArrayList;
        private int mMax;

        LimitedCircularArray(int max) {
            this.mArrayList = new ArrayList<>(max);
            this.mMax = max;
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

    synchronized void onRingBufferData(WifiNative.RingBufferStatus status, byte[] buffer) {
        ByteArrayRingBuffer ring = this.mRingBufferData.get(status.name);
        if (ring != null) {
            ring.appendBuffer(buffer);
        }
    }

    synchronized void onWifiAlert(int errorCode, byte[] buffer) {
        if (this.mWifiStateMachine != null) {
            this.mWifiStateMachine.sendMessage(131172, errorCode, 0, buffer);
        }
    }

    private boolean isVerboseLoggingEnabled() {
        return this.mLogLevel > 1;
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
            for (WifiNative.RingBufferStatus buffer : this.mRingBuffers) {
                if (!this.mRingBufferData.containsKey(buffer.name)) {
                    this.mRingBufferData.put(buffer.name, new ByteArrayRingBuffer(this.mMaxRingBufferSizeBytes));
                }
                if ((buffer.flag & 4) != 0) {
                    this.mPerPacketRingBuffer = buffer;
                }
            }
        } else {
            Log.e(TAG, "no ring buffers found");
        }
        return this.mRingBuffers != null;
    }

    private void resizeRingBuffers() {
        for (ByteArrayRingBuffer byteArrayRingBuffer : this.mRingBufferData.values()) {
            byteArrayRingBuffer.resize(this.mMaxRingBufferSizeBytes);
        }
    }

    private boolean startLoggingAllExceptPerPacketBuffers() {
        if (this.mRingBuffers == null) {
            return false;
        }
        for (WifiNative.RingBufferStatus buffer : this.mRingBuffers) {
            if ((buffer.flag & 4) == 0) {
                startLoggingRingBuffer(buffer);
            }
        }
        return true;
    }

    private boolean startLoggingRingBuffer(WifiNative.RingBufferStatus buffer) {
        int minInterval = MinWakeupIntervals[this.mLogLevel];
        int minDataSize = MinBufferSizes[this.mLogLevel];
        return this.mWifiNative.startLoggingRingBuffer(this.mLogLevel, 0, minInterval, minDataSize, buffer.name);
    }

    private boolean stopLoggingRingBuffer(WifiNative.RingBufferStatus buffer) {
        if (!this.mWifiNative.startLoggingRingBuffer(0, 0, 0, 0, buffer.name)) {
        }
        return true;
    }

    private boolean stopLoggingAllBuffers() {
        if (this.mRingBuffers != null) {
            for (WifiNative.RingBufferStatus buffer : this.mRingBuffers) {
                stopLoggingRingBuffer(buffer);
            }
            return true;
        }
        return true;
    }

    private boolean getAllRingBufferData() {
        if (this.mRingBuffers == null) {
            Log.e(TAG, "Not ring buffers available to collect data!");
            return false;
        }
        for (WifiNative.RingBufferStatus element : this.mRingBuffers) {
            boolean result = this.mWifiNative.getRingBufferData(element.name);
            if (!result) {
                Log.e(TAG, "Fail to get ring buffer data of: " + element.name);
                return false;
            }
        }
        Log.d(TAG, "getAllRingBufferData Successfully!");
        return true;
    }

    private boolean enableVerboseLoggingForDogfood() {
        return false;
    }

    private BugReport captureBugreport(int errorCode, boolean captureFWDump) {
        BugReport report = new BugReport();
        report.errorCode = errorCode;
        report.systemTimeMs = System.currentTimeMillis();
        report.kernelTimeNanos = System.nanoTime();
        if (this.mRingBuffers != null) {
            for (WifiNative.RingBufferStatus buffer : this.mRingBuffers) {
                this.mWifiNative.getRingBufferData(buffer.name);
                ByteArrayRingBuffer data = this.mRingBufferData.get(buffer.name);
                byte[][] buffers = new byte[data.getNumBuffers()][];
                for (int i = 0; i < data.getNumBuffers(); i++) {
                    buffers[i] = (byte[]) data.getBuffer(i).clone();
                }
                report.ringBuffers.put(buffer.name, buffers);
            }
        }
        report.logcatLines = getLogcat(InformationElementUtil.SupportedRates.MASK);
        report.kernelLogLines = getKernelLog(InformationElementUtil.SupportedRates.MASK);
        if (captureFWDump) {
            report.fwMemoryDump = this.mWifiNative.getFwMemoryDump();
            report.mDriverStateDump = this.mWifiNative.getDriverStateDump();
        }
        return report;
    }

    LimitedCircularArray<BugReport> getBugReports() {
        return this.mLastBugReports;
    }

    private static String compressToBase64(byte[] input) {
        Deflater compressor = new Deflater();
        compressor.setLevel(9);
        compressor.setInput(input);
        compressor.finish();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
        byte[] buf = new byte[1024];
        while (!compressor.finished()) {
            int count = compressor.deflate(buf);
            bos.write(buf, 0, count);
        }
        try {
            compressor.end();
            bos.close();
            byte[] compressed = bos.toByteArray();
            if (compressed.length >= input.length) {
                compressed = input;
            }
            String result = Base64.encodeToString(compressed, 0);
            return result;
        } catch (IOException e) {
            Log.e(TAG, "ByteArrayOutputStream close error");
            String result2 = Base64.encodeToString(input, 0);
            return result2;
        }
    }

    private ArrayList<String> getLogcat(int maxLines) {
        Process process;
        BufferedReader reader;
        ArrayList<String> lines = new ArrayList<>(maxLines);
        try {
            process = Runtime.getRuntime().exec(String.format("logcat -t %d", Integer.valueOf(maxLines)));
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Exception while capturing logcat" + e);
        }
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            lines.add(line);
            return lines;
        }
        BufferedReader reader2 = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        while (true) {
            String line2 = reader2.readLine();
            if (line2 == null) {
                break;
            }
            lines.add(line2);
            return lines;
        }
        process.waitFor();
        return lines;
    }

    private LimitedCircularArray<String> getKernelLog(int maxLines) {
        LimitedCircularArray<String> lines = new LimitedCircularArray<>(maxLines);
        String log = this.mWifiNative.readKernelLog();
        String[] logLines = log.split("\n");
        for (String str : logLines) {
            lines.addLast(str);
        }
        return lines;
    }

    private ArrayList<WifiNative.FateReport> fetchPacketFates() {
        ArrayList<WifiNative.FateReport> mergedFates = new ArrayList<>();
        WifiNative.TxFateReport[] txFates = new WifiNative.TxFateReport[32];
        if (this.mWifiNative.getTxPktFates(txFates)) {
            for (int i = 0; i < txFates.length && txFates[i] != null; i++) {
                mergedFates.add(txFates[i]);
            }
        }
        WifiNative.RxFateReport[] rxFates = new WifiNative.RxFateReport[32];
        if (this.mWifiNative.getRxPktFates(rxFates)) {
            for (int i2 = 0; i2 < rxFates.length && rxFates[i2] != null; i2++) {
                mergedFates.add(rxFates[i2]);
            }
        }
        Collections.sort(mergedFates, new Comparator<WifiNative.FateReport>() {
            @Override
            public int compare(WifiNative.FateReport lhs, WifiNative.FateReport rhs) {
                return Long.compare(lhs.mDriverTimestampUSec, rhs.mDriverTimestampUSec);
            }
        });
        return mergedFates;
    }

    private void dumpPacketFates(PrintWriter pw) {
        dumpPacketFatesInternal(pw, "Last failed connection fates", this.mPacketFatesForLastFailure, isVerboseLoggingEnabled());
        dumpPacketFatesInternal(pw, "Latest fates", fetchPacketFates(), isVerboseLoggingEnabled());
    }

    private static void dumpPacketFatesInternal(PrintWriter pw, String description, ArrayList<WifiNative.FateReport> fates, boolean verbose) {
        if (fates == null) {
            pw.format("No fates fetched for \"%s\"\n", description);
            return;
        }
        if (fates.size() == 0) {
            pw.format("HAL provided zero fates for \"%s\"\n", description);
            return;
        }
        pw.format("--------------------- %s ----------------------\n", description);
        StringBuilder verboseOutput = new StringBuilder();
        pw.print(WifiNative.FateReport.getTableHeader());
        for (WifiNative.FateReport fate : fates) {
            pw.print(fate.toTableRowString());
            if (verbose) {
                verboseOutput.append(fate.toVerboseStringWithPiiAllowed());
                verboseOutput.append("\n");
            }
        }
        if (verbose) {
            pw.format("\n>>> VERBOSE PACKET FATE DUMP <<<\n\n", new Object[0]);
            pw.print(verboseOutput.toString());
        }
        pw.println("--------------------------------------------------------------------");
    }
}
