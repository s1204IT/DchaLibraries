package com.android.server.connectivity.tethering;

import android.hardware.tetheroffload.control.V1_0.IOffloadControl;
import android.hardware.tetheroffload.control.V1_0.ITetheringOffloadCallback;
import android.hardware.tetheroffload.control.V1_0.NatTimeoutUpdate;
import android.net.util.SharedLog;
import android.os.Handler;
import android.os.RemoteException;
import android.system.OsConstants;
import com.android.internal.util.BitUtils;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.connectivity.tethering.OffloadHardwareInterface;
import java.util.ArrayList;

public class OffloadHardwareInterface {
    private static final int DEFAULT_TETHER_OFFLOAD_DISABLED = 0;
    private static final String NO_INTERFACE_NAME = "";
    private static final String NO_IPV4_ADDRESS = "";
    private static final String NO_IPV4_GATEWAY = "";
    private static final String TAG = OffloadHardwareInterface.class.getSimpleName();
    private static final String YIELDS = " -> ";
    private ControlCallback mControlCallback;
    private final Handler mHandler;
    private final SharedLog mLog;
    private IOffloadControl mOffloadControl;
    private TetheringOffloadCallback mTetheringOffloadCallback;

    private static native boolean configOffload();

    public static class ControlCallback {
        public void onStarted() {
        }

        public void onStoppedError() {
        }

        public void onStoppedUnsupported() {
        }

        public void onSupportAvailable() {
        }

        public void onStoppedLimitReached() {
        }

        public void onNatTimeoutUpdate(int i, String str, int i2, String str2, int i3) {
        }
    }

    public static class ForwardedStats {
        public long rxBytes = 0;
        public long txBytes = 0;

        public void add(ForwardedStats forwardedStats) {
            this.rxBytes += forwardedStats.rxBytes;
            this.txBytes += forwardedStats.txBytes;
        }

        public String toString() {
            return String.format("rx:%s tx:%s", Long.valueOf(this.rxBytes), Long.valueOf(this.txBytes));
        }
    }

    public OffloadHardwareInterface(Handler handler, SharedLog sharedLog) {
        this.mHandler = handler;
        this.mLog = sharedLog.forSubComponent(TAG);
    }

    public int getDefaultTetherOffloadDisabled() {
        return 0;
    }

    public boolean initOffloadConfig() {
        return configOffload();
    }

    public boolean initOffloadControl(ControlCallback controlCallback) {
        String str;
        this.mControlCallback = controlCallback;
        if (this.mOffloadControl == null) {
            try {
                this.mOffloadControl = IOffloadControl.getService();
                if (this.mOffloadControl == null) {
                    this.mLog.e("tethering IOffloadControl.getService() returned null");
                    return false;
                }
            } catch (RemoteException e) {
                this.mLog.e("tethering offload control not supported: " + e);
                return false;
            }
        }
        Object[] objArr = new Object[1];
        if (controlCallback == null) {
            str = "null";
        } else {
            str = "0x" + Integer.toHexString(System.identityHashCode(controlCallback));
        }
        objArr[0] = str;
        String str2 = String.format("initOffloadControl(%s)", objArr);
        this.mTetheringOffloadCallback = new TetheringOffloadCallback(this.mHandler, this.mControlCallback, this.mLog);
        final CbResults cbResults = new CbResults();
        try {
            this.mOffloadControl.initOffload(this.mTetheringOffloadCallback, new IOffloadControl.initOffloadCallback() {
                @Override
                public final void onValues(boolean z, String str3) {
                    OffloadHardwareInterface.lambda$initOffloadControl$0(cbResults, z, str3);
                }
            });
            record(str2, cbResults);
            return cbResults.success;
        } catch (RemoteException e2) {
            record(str2, e2);
            return false;
        }
    }

    static void lambda$initOffloadControl$0(CbResults cbResults, boolean z, String str) {
        cbResults.success = z;
        cbResults.errMsg = str;
    }

    public void stopOffloadControl() {
        if (this.mOffloadControl != null) {
            try {
                this.mOffloadControl.stopOffload(new IOffloadControl.stopOffloadCallback() {
                    @Override
                    public final void onValues(boolean z, String str) {
                        OffloadHardwareInterface.lambda$stopOffloadControl$1(this.f$0, z, str);
                    }
                });
            } catch (RemoteException e) {
                this.mLog.e("failed to stopOffload: " + e);
            }
        }
        this.mOffloadControl = null;
        this.mTetheringOffloadCallback = null;
        this.mControlCallback = null;
        this.mLog.log("stopOffloadControl()");
    }

    public static void lambda$stopOffloadControl$1(OffloadHardwareInterface offloadHardwareInterface, boolean z, String str) {
        if (!z) {
            offloadHardwareInterface.mLog.e("stopOffload failed: " + str);
        }
    }

    public ForwardedStats getForwardedStats(String str) {
        String str2 = String.format("getForwardedStats(%s)", str);
        final ForwardedStats forwardedStats = new ForwardedStats();
        try {
            this.mOffloadControl.getForwardedStats(str, new IOffloadControl.getForwardedStatsCallback() {
                @Override
                public final void onValues(long j, long j2) {
                    OffloadHardwareInterface.lambda$getForwardedStats$2(forwardedStats, j, j2);
                }
            });
            this.mLog.log(str2 + YIELDS + forwardedStats);
            return forwardedStats;
        } catch (RemoteException e) {
            record(str2, e);
            return forwardedStats;
        }
    }

    static void lambda$getForwardedStats$2(ForwardedStats forwardedStats, long j, long j2) {
        if (j <= 0) {
            j = 0;
        }
        forwardedStats.rxBytes = j;
        if (j2 <= 0) {
            j2 = 0;
        }
        forwardedStats.txBytes = j2;
    }

    public boolean setLocalPrefixes(ArrayList<String> arrayList) {
        String str = String.format("setLocalPrefixes([%s])", String.join(",", arrayList));
        final CbResults cbResults = new CbResults();
        try {
            this.mOffloadControl.setLocalPrefixes(arrayList, new IOffloadControl.setLocalPrefixesCallback() {
                @Override
                public final void onValues(boolean z, String str2) {
                    OffloadHardwareInterface.lambda$setLocalPrefixes$3(cbResults, z, str2);
                }
            });
            record(str, cbResults);
            return cbResults.success;
        } catch (RemoteException e) {
            record(str, e);
            return false;
        }
    }

    static void lambda$setLocalPrefixes$3(CbResults cbResults, boolean z, String str) {
        cbResults.success = z;
        cbResults.errMsg = str;
    }

    public boolean setDataLimit(String str, long j) {
        String str2 = String.format("setDataLimit(%s, %d)", str, Long.valueOf(j));
        final CbResults cbResults = new CbResults();
        try {
            this.mOffloadControl.setDataLimit(str, j, new IOffloadControl.setDataLimitCallback() {
                @Override
                public final void onValues(boolean z, String str3) {
                    OffloadHardwareInterface.lambda$setDataLimit$4(cbResults, z, str3);
                }
            });
            record(str2, cbResults);
            return cbResults.success;
        } catch (RemoteException e) {
            record(str2, e);
            return false;
        }
    }

    static void lambda$setDataLimit$4(CbResults cbResults, boolean z, String str) {
        cbResults.success = z;
        cbResults.errMsg = str;
    }

    public boolean setUpstreamParameters(String str, String str2, String str3, ArrayList<String> arrayList) {
        if (str == null) {
            str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        String str4 = str;
        if (str2 == null) {
            str2 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        String str5 = str2;
        if (str3 == null) {
            str3 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        String str6 = str3;
        if (arrayList == null) {
            arrayList = new ArrayList<>();
        }
        ArrayList<String> arrayList2 = arrayList;
        String str7 = String.format("setUpstreamParameters(%s, %s, %s, [%s])", str4, str5, str6, String.join(",", arrayList2));
        final CbResults cbResults = new CbResults();
        try {
            this.mOffloadControl.setUpstreamParameters(str4, str5, str6, arrayList2, new IOffloadControl.setUpstreamParametersCallback() {
                @Override
                public final void onValues(boolean z, String str8) {
                    OffloadHardwareInterface.lambda$setUpstreamParameters$5(cbResults, z, str8);
                }
            });
            record(str7, cbResults);
            return cbResults.success;
        } catch (RemoteException e) {
            record(str7, e);
            return false;
        }
    }

    static void lambda$setUpstreamParameters$5(CbResults cbResults, boolean z, String str) {
        cbResults.success = z;
        cbResults.errMsg = str;
    }

    public boolean addDownstreamPrefix(String str, String str2) {
        String str3 = String.format("addDownstreamPrefix(%s, %s)", str, str2);
        final CbResults cbResults = new CbResults();
        try {
            this.mOffloadControl.addDownstream(str, str2, new IOffloadControl.addDownstreamCallback() {
                @Override
                public final void onValues(boolean z, String str4) {
                    OffloadHardwareInterface.lambda$addDownstreamPrefix$6(cbResults, z, str4);
                }
            });
            record(str3, cbResults);
            return cbResults.success;
        } catch (RemoteException e) {
            record(str3, e);
            return false;
        }
    }

    static void lambda$addDownstreamPrefix$6(CbResults cbResults, boolean z, String str) {
        cbResults.success = z;
        cbResults.errMsg = str;
    }

    public boolean removeDownstreamPrefix(String str, String str2) {
        String str3 = String.format("removeDownstreamPrefix(%s, %s)", str, str2);
        final CbResults cbResults = new CbResults();
        try {
            this.mOffloadControl.removeDownstream(str, str2, new IOffloadControl.removeDownstreamCallback() {
                @Override
                public final void onValues(boolean z, String str4) {
                    OffloadHardwareInterface.lambda$removeDownstreamPrefix$7(cbResults, z, str4);
                }
            });
            record(str3, cbResults);
            return cbResults.success;
        } catch (RemoteException e) {
            record(str3, e);
            return false;
        }
    }

    static void lambda$removeDownstreamPrefix$7(CbResults cbResults, boolean z, String str) {
        cbResults.success = z;
        cbResults.errMsg = str;
    }

    private void record(String str, Throwable th) {
        this.mLog.e(str + YIELDS + "exception: " + th);
    }

    private void record(String str, CbResults cbResults) {
        String str2 = str + YIELDS + cbResults;
        if (!cbResults.success) {
            this.mLog.e(str2);
        } else {
            this.mLog.log(str2);
        }
    }

    private static class TetheringOffloadCallback extends ITetheringOffloadCallback.Stub {
        public final ControlCallback controlCb;
        public final Handler handler;
        public final SharedLog log;

        public TetheringOffloadCallback(Handler handler, ControlCallback controlCallback, SharedLog sharedLog) {
            this.handler = handler;
            this.controlCb = controlCallback;
            this.log = sharedLog;
        }

        @Override
        public void onEvent(final int i) {
            this.handler.post(new Runnable() {
                @Override
                public final void run() {
                    OffloadHardwareInterface.TetheringOffloadCallback.lambda$onEvent$0(this.f$0, i);
                }
            });
        }

        public static void lambda$onEvent$0(TetheringOffloadCallback tetheringOffloadCallback, int i) {
            switch (i) {
                case 1:
                    tetheringOffloadCallback.controlCb.onStarted();
                    break;
                case 2:
                    tetheringOffloadCallback.controlCb.onStoppedError();
                    break;
                case 3:
                    tetheringOffloadCallback.controlCb.onStoppedUnsupported();
                    break;
                case 4:
                    tetheringOffloadCallback.controlCb.onSupportAvailable();
                    break;
                case 5:
                    tetheringOffloadCallback.controlCb.onStoppedLimitReached();
                    break;
                default:
                    tetheringOffloadCallback.log.e("Unsupported OffloadCallbackEvent: " + i);
                    break;
            }
        }

        @Override
        public void updateTimeout(final NatTimeoutUpdate natTimeoutUpdate) {
            this.handler.post(new Runnable() {
                @Override
                public final void run() {
                    OffloadHardwareInterface.TetheringOffloadCallback tetheringOffloadCallback = this.f$0;
                    NatTimeoutUpdate natTimeoutUpdate2 = natTimeoutUpdate;
                    tetheringOffloadCallback.controlCb.onNatTimeoutUpdate(OffloadHardwareInterface.networkProtocolToOsConstant(natTimeoutUpdate2.proto), natTimeoutUpdate2.src.addr, BitUtils.uint16(natTimeoutUpdate2.src.port), natTimeoutUpdate2.dst.addr, BitUtils.uint16(natTimeoutUpdate2.dst.port));
                }
            });
        }
    }

    private static int networkProtocolToOsConstant(int i) {
        if (i == 6) {
            return OsConstants.IPPROTO_TCP;
        }
        if (i == 17) {
            return OsConstants.IPPROTO_UDP;
        }
        return -Math.abs(i);
    }

    private static class CbResults {
        String errMsg;
        boolean success;

        private CbResults() {
        }

        public String toString() {
            if (this.success) {
                return "ok";
            }
            return "fail: " + this.errMsg;
        }
    }
}
