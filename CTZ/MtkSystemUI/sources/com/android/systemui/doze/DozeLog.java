package com.android.systemui.doze;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.util.TimeUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.doze.DozeMachine;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DozeLog {
    private static final boolean DEBUG = Log.isLoggable("DozeLog", 3);
    static final SimpleDateFormat FORMAT;
    private static final int SIZE;
    private static int sCount;
    private static SummaryStats sEmergencyCallStats;
    private static final KeyguardUpdateMonitorCallback sKeyguardCallback;
    private static String[] sMessages;
    private static SummaryStats sNotificationPulseStats;
    private static SummaryStats sPickupPulseNearVibrationStats;
    private static SummaryStats sPickupPulseNotNearVibrationStats;
    private static int sPosition;
    private static SummaryStats[][] sProxStats;
    private static boolean sPulsing;
    private static boolean sRegisterKeyguardCallback;
    private static SummaryStats sScreenOnNotPulsingStats;
    private static SummaryStats sScreenOnPulsingStats;
    private static long sSince;
    private static long[] sTimes;

    static {
        SIZE = Build.IS_DEBUGGABLE ? 400 : 50;
        FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
        sRegisterKeyguardCallback = true;
        sKeyguardCallback = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onEmergencyCallAction() {
                DozeLog.traceEmergencyCall();
            }

            @Override
            public void onKeyguardBouncerChanged(boolean z) {
                DozeLog.traceKeyguardBouncerChanged(z);
            }

            @Override
            public void onStartedWakingUp() {
                DozeLog.traceScreenOn();
            }

            @Override
            public void onFinishedGoingToSleep(int i) {
                DozeLog.traceScreenOff(i);
            }

            @Override
            public void onKeyguardVisibilityChanged(boolean z) {
                DozeLog.traceKeyguard(z);
            }
        };
    }

    public static void tracePickupPulse(Context context, boolean z) {
        init(context);
        log("pickupPulse withinVibrationThreshold=" + z);
        (z ? sPickupPulseNearVibrationStats : sPickupPulseNotNearVibrationStats).append();
    }

    public static void tracePulseStart(int i) {
        sPulsing = true;
        log("pulseStart reason=" + pulseReasonToString(i));
    }

    public static void tracePulseFinish() {
        sPulsing = false;
        log("pulseFinish");
    }

    public static void traceNotificationPulse(Context context) {
        init(context);
        log("notificationPulse");
        sNotificationPulseStats.append();
    }

    private static void init(Context context) {
        synchronized (DozeLog.class) {
            if (sMessages == null) {
                sTimes = new long[SIZE];
                sMessages = new String[SIZE];
                sSince = System.currentTimeMillis();
                sPickupPulseNearVibrationStats = new SummaryStats();
                sPickupPulseNotNearVibrationStats = new SummaryStats();
                sNotificationPulseStats = new SummaryStats();
                sScreenOnPulsingStats = new SummaryStats();
                sScreenOnNotPulsingStats = new SummaryStats();
                sEmergencyCallStats = new SummaryStats();
                sProxStats = (SummaryStats[][]) Array.newInstance((Class<?>) SummaryStats.class, 6, 2);
                for (int i = 0; i < 6; i++) {
                    sProxStats[i][0] = new SummaryStats();
                    sProxStats[i][1] = new SummaryStats();
                }
                log("init");
                if (sRegisterKeyguardCallback) {
                    KeyguardUpdateMonitor.getInstance(context).registerCallback(sKeyguardCallback);
                }
            }
        }
    }

    public static void traceDozing(Context context, boolean z) {
        sPulsing = false;
        init(context);
        log("dozing " + z);
    }

    public static void traceFling(boolean z, boolean z2, boolean z3, boolean z4) {
        log("fling expand=" + z + " aboveThreshold=" + z2 + " thresholdNeeded=" + z3 + " screenOnFromTouch=" + z4);
    }

    public static void traceEmergencyCall() {
        log("emergencyCall");
        sEmergencyCallStats.append();
    }

    public static void traceKeyguardBouncerChanged(boolean z) {
        log("bouncer " + z);
    }

    public static void traceScreenOn() {
        log("screenOn pulsing=" + sPulsing);
        (sPulsing ? sScreenOnPulsingStats : sScreenOnNotPulsingStats).append();
        sPulsing = false;
    }

    public static void traceScreenOff(int i) {
        log("screenOff why=" + i);
    }

    public static void traceMissedTick(String str) {
        log("missedTick by=" + str);
    }

    public static void traceKeyguard(boolean z) {
        log("keyguard " + z);
        if (!z) {
            sPulsing = false;
        }
    }

    public static void traceState(DozeMachine.State state) {
        log("state " + state);
    }

    public static void traceProximityResult(Context context, boolean z, long j, int i) {
        init(context);
        log("proximityResult reason=" + pulseReasonToString(i) + " near=" + z + " millis=" + j);
        sProxStats[i][!z ? 1 : 0].append();
    }

    public static String pulseReasonToString(int i) {
        switch (i) {
            case 0:
                return "intent";
            case 1:
                return "notification";
            case 2:
                return "sigmotion";
            case 3:
                return "pickup";
            case 4:
                return "doubletap";
            case 5:
                return "longpress";
            default:
                throw new IllegalArgumentException("bad reason: " + i);
        }
    }

    public static void dump(PrintWriter printWriter) {
        synchronized (DozeLog.class) {
            if (sMessages == null) {
                return;
            }
            printWriter.println("  Doze log:");
            int i = ((sPosition - sCount) + SIZE) % SIZE;
            for (int i2 = 0; i2 < sCount; i2++) {
                int i3 = (i + i2) % SIZE;
                printWriter.print("    ");
                printWriter.print(FORMAT.format(new Date(sTimes[i3])));
                printWriter.print(' ');
                printWriter.println(sMessages[i3]);
            }
            printWriter.print("  Doze summary stats (for ");
            TimeUtils.formatDuration(System.currentTimeMillis() - sSince, printWriter);
            printWriter.println("):");
            sPickupPulseNearVibrationStats.dump(printWriter, "Pickup pulse (near vibration)");
            sPickupPulseNotNearVibrationStats.dump(printWriter, "Pickup pulse (not near vibration)");
            sNotificationPulseStats.dump(printWriter, "Notification pulse");
            sScreenOnPulsingStats.dump(printWriter, "Screen on (pulsing)");
            sScreenOnNotPulsingStats.dump(printWriter, "Screen on (not pulsing)");
            sEmergencyCallStats.dump(printWriter, "Emergency call");
            for (int i4 = 0; i4 < 6; i4++) {
                String strPulseReasonToString = pulseReasonToString(i4);
                sProxStats[i4][0].dump(printWriter, "Proximity near (" + strPulseReasonToString + ")");
                sProxStats[i4][1].dump(printWriter, "Proximity far (" + strPulseReasonToString + ")");
            }
        }
    }

    private static void log(String str) {
        synchronized (DozeLog.class) {
            if (sMessages == null) {
                return;
            }
            sTimes[sPosition] = System.currentTimeMillis();
            sMessages[sPosition] = str;
            sPosition = (sPosition + 1) % SIZE;
            sCount = Math.min(sCount + 1, SIZE);
            if (DEBUG) {
                Log.d("DozeLog", str);
            }
        }
    }

    public static void tracePulseDropped(Context context, boolean z, DozeMachine.State state, boolean z2) {
        init(context);
        log("pulseDropped pulsePending=" + z + " state=" + state + " blocked=" + z2);
    }

    public static void tracePulseTouchDisabledByProx(Context context, boolean z) {
        init(context);
        log("pulseTouchDisabledByProx " + z);
    }

    public static void traceSensor(Context context, int i) {
        init(context);
        log("sensor type=" + pulseReasonToString(i));
    }

    private static class SummaryStats {
        private int mCount;

        private SummaryStats() {
        }

        public void append() {
            this.mCount++;
        }

        public void dump(PrintWriter printWriter, String str) {
            if (this.mCount == 0) {
                return;
            }
            printWriter.print("    ");
            printWriter.print(str);
            printWriter.print(": n=");
            printWriter.print(this.mCount);
            printWriter.print(" (");
            printWriter.print((((double) this.mCount) / (System.currentTimeMillis() - DozeLog.sSince)) * 1000.0d * 60.0d * 60.0d);
            printWriter.print("/hr)");
            printWriter.println();
        }
    }
}
