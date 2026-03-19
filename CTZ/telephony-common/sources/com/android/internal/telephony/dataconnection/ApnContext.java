package com.android.internal.telephony.dataconnection;

import android.R;
import android.app.PendingIntent;
import android.net.NetworkCapabilities;
import android.net.NetworkConfig;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.telephony.Rlog;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.SparseIntArray;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.RetryManager;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ApnContext {
    protected static final boolean DBG = false;
    private static final String SLOG_TAG = "ApnContext";
    private static Method sMethodApnIdForApnNameEx;
    private static Method sMethodApnIdForNetworkRequestEx;
    private static Method sMethodApnIdForTypeEx;
    public final String LOG_TAG;
    private ApnSetting mApnSetting;
    protected final String mApnType;
    private boolean mConcurrentVoiceAndDataAllowed;
    AtomicBoolean mDataEnabled;
    DcAsyncChannel mDcAc;
    protected final DcTracker mDcTracker;
    AtomicBoolean mDependencyMet;
    private final Phone mPhone;
    String mReason;
    PendingIntent mReconnectAlarmIntent;
    protected final RetryManager mRetryManager;
    public final int priority;
    private final Object mRefCountLock = new Object();
    private int mRefCount = 0;
    private final AtomicInteger mConnectionGeneration = new AtomicInteger(0);
    private final ArrayList<LocalLog> mLocalLogs = new ArrayList<>();
    private final ArrayList<NetworkRequest> mNetworkRequests = new ArrayList<>();
    private final LocalLog mStateLocalLog = new LocalLog(50);
    private final SparseIntArray mRetriesLeftPerErrorCode = new SparseIntArray();
    private DctConstants.State mState = DctConstants.State.IDLE;

    static {
        Class<?> cls;
        try {
            cls = Class.forName("com.mediatek.internal.telephony.dataconnection.MtkApnContext");
        } catch (Exception e) {
            Rlog.d(SLOG_TAG, e.toString());
            cls = null;
        }
        if (cls != null) {
            try {
                sMethodApnIdForTypeEx = cls.getDeclaredMethod("apnIdForTypeEx", Integer.TYPE);
                sMethodApnIdForTypeEx.setAccessible(true);
            } catch (Exception e2) {
                Rlog.d(SLOG_TAG, e2.toString());
            }
            try {
                sMethodApnIdForNetworkRequestEx = cls.getDeclaredMethod("apnIdForNetworkRequestEx", NetworkCapabilities.class, Integer.TYPE, Boolean.TYPE);
                sMethodApnIdForNetworkRequestEx.setAccessible(true);
            } catch (Exception e3) {
                Rlog.d(SLOG_TAG, e3.toString());
            }
            try {
                sMethodApnIdForApnNameEx = cls.getDeclaredMethod("apnIdForApnNameEx", String.class);
                sMethodApnIdForApnNameEx.setAccessible(true);
            } catch (Exception e4) {
                Rlog.d(SLOG_TAG, e4.toString());
            }
        }
    }

    public ApnContext(Phone phone, String str, String str2, NetworkConfig networkConfig, DcTracker dcTracker) {
        this.mPhone = phone;
        this.mApnType = str;
        setReason(PhoneInternalInterface.REASON_DATA_ENABLED);
        this.mDataEnabled = new AtomicBoolean(false);
        this.mDependencyMet = new AtomicBoolean(networkConfig.dependencyMet);
        this.priority = networkConfig.priority;
        this.LOG_TAG = str2;
        this.mDcTracker = dcTracker;
        this.mRetryManager = TelephonyComponentFactory.getInstance().makeRetryManager(phone, str);
    }

    public String getApnType() {
        return this.mApnType;
    }

    public synchronized DcAsyncChannel getDcAc() {
        return this.mDcAc;
    }

    public synchronized void setDataConnectionAc(DcAsyncChannel dcAsyncChannel) {
        this.mDcAc = dcAsyncChannel;
    }

    public synchronized void releaseDataConnection(String str) {
        if (this.mDcAc != null) {
            this.mDcAc.tearDown(this, str, null);
            this.mDcAc = null;
        }
        setState(DctConstants.State.IDLE);
    }

    public synchronized PendingIntent getReconnectIntent() {
        return this.mReconnectAlarmIntent;
    }

    public synchronized void setReconnectIntent(PendingIntent pendingIntent) {
        this.mReconnectAlarmIntent = pendingIntent;
    }

    public synchronized ApnSetting getApnSetting() {
        return this.mApnSetting;
    }

    public synchronized void setApnSetting(ApnSetting apnSetting) {
        this.mApnSetting = apnSetting;
    }

    public synchronized void setWaitingApns(ArrayList<ApnSetting> arrayList) {
        this.mRetryManager.setWaitingApns(arrayList);
    }

    public ApnSetting getNextApnSetting() {
        return this.mRetryManager.getNextApnSetting();
    }

    public void setModemSuggestedDelay(long j) {
        this.mRetryManager.setModemSuggestedDelay(j);
    }

    public long getDelayForNextApn(boolean z) {
        return this.mRetryManager.getDelayForNextApn(z || isFastRetryReason());
    }

    public void markApnPermanentFailed(ApnSetting apnSetting) {
        this.mRetryManager.markApnPermanentFailed(apnSetting);
    }

    public ArrayList<ApnSetting> getWaitingApns() {
        return this.mRetryManager.getWaitingApns();
    }

    public synchronized void setConcurrentVoiceAndDataAllowed(boolean z) {
        this.mConcurrentVoiceAndDataAllowed = z;
    }

    public synchronized boolean isConcurrentVoiceAndDataAllowed() {
        return this.mConcurrentVoiceAndDataAllowed;
    }

    public synchronized void setState(DctConstants.State state) {
        if (this.mState != state) {
            this.mStateLocalLog.log("State changed from " + this.mState + " to " + state);
            this.mState = state;
        }
        if (this.mState == DctConstants.State.FAILED && this.mRetryManager.getWaitingApns() != null) {
            this.mRetryManager.getWaitingApns().clear();
        }
    }

    public synchronized DctConstants.State getState() {
        return this.mState;
    }

    public boolean isDisconnected() {
        DctConstants.State state = getState();
        return state == DctConstants.State.IDLE || state == DctConstants.State.FAILED;
    }

    public synchronized void setReason(String str) {
        this.mReason = str;
    }

    public synchronized String getReason() {
        return this.mReason;
    }

    public boolean isReady() {
        return this.mDataEnabled.get() && this.mDependencyMet.get();
    }

    public boolean isConnectable() {
        return isReady() && (this.mState == DctConstants.State.IDLE || this.mState == DctConstants.State.SCANNING || this.mState == DctConstants.State.RETRYING || this.mState == DctConstants.State.FAILED);
    }

    private boolean isFastRetryReason() {
        return PhoneInternalInterface.REASON_NW_TYPE_CHANGED.equals(this.mReason) || PhoneInternalInterface.REASON_APN_CHANGED.equals(this.mReason);
    }

    public boolean isConnectedOrConnecting() {
        return isReady() && (this.mState == DctConstants.State.CONNECTED || this.mState == DctConstants.State.CONNECTING || this.mState == DctConstants.State.SCANNING || this.mState == DctConstants.State.RETRYING);
    }

    public void setEnabled(boolean z) {
        this.mDataEnabled.set(z);
    }

    public boolean isEnabled() {
        return this.mDataEnabled.get();
    }

    public void setDependencyMet(boolean z) {
        this.mDependencyMet.set(z);
    }

    public boolean getDependencyMet() {
        return this.mDependencyMet.get();
    }

    public boolean isProvisioningApn() {
        String string = this.mPhone.getContext().getResources().getString(R.string.ext_media_move_failure_title);
        if (!TextUtils.isEmpty(string) && this.mApnSetting != null && this.mApnSetting.apn != null) {
            return this.mApnSetting.apn.equals(string);
        }
        return false;
    }

    public void requestLog(String str) {
        synchronized (this.mRefCountLock) {
            Iterator<LocalLog> it = this.mLocalLogs.iterator();
            while (it.hasNext()) {
                it.next().log(str);
            }
        }
    }

    public void requestNetwork(NetworkRequest networkRequest, LocalLog localLog) {
        synchronized (this.mRefCountLock) {
            if (this.mLocalLogs.contains(localLog) || this.mNetworkRequests.contains(networkRequest)) {
                localLog.log("ApnContext.requestNetwork has duplicate add - " + this.mNetworkRequests.size());
            } else {
                this.mLocalLogs.add(localLog);
                this.mNetworkRequests.add(networkRequest);
                this.mDcTracker.setEnabled(apnIdForApnName(this.mApnType), true);
            }
        }
    }

    public void releaseNetwork(NetworkRequest networkRequest, LocalLog localLog) {
        synchronized (this.mRefCountLock) {
            if (!this.mLocalLogs.contains(localLog)) {
                localLog.log("ApnContext.releaseNetwork can't find this log");
            } else {
                this.mLocalLogs.remove(localLog);
            }
            if (!this.mNetworkRequests.contains(networkRequest)) {
                localLog.log("ApnContext.releaseNetwork can't find this request (" + networkRequest + ")");
            } else {
                this.mNetworkRequests.remove(networkRequest);
                localLog.log("ApnContext.releaseNetwork left with " + this.mNetworkRequests.size() + " requests.");
                if (this.mNetworkRequests.size() == 0) {
                    this.mDcTracker.setEnabled(apnIdForApnName(this.mApnType), false);
                }
            }
        }
    }

    public List<NetworkRequest> getNetworkRequests() {
        ArrayList arrayList;
        synchronized (this.mRefCountLock) {
            arrayList = new ArrayList(this.mNetworkRequests);
        }
        return arrayList;
    }

    public boolean hasNoRestrictedRequests(boolean z) {
        synchronized (this.mRefCountLock) {
            for (NetworkRequest networkRequest : this.mNetworkRequests) {
                if (!z || !networkRequest.networkCapabilities.hasCapability(2)) {
                    if (!networkRequest.networkCapabilities.hasCapability(13)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    public void resetErrorCodeRetries() {
        requestLog("ApnContext.resetErrorCodeRetries");
        String[] stringArray = this.mPhone.getContext().getResources().getStringArray(R.array.config_autoKeyboardBacklightIncreaseLuxThreshold);
        synchronized (this.mRetriesLeftPerErrorCode) {
            this.mRetriesLeftPerErrorCode.clear();
            for (String str : stringArray) {
                String[] strArrSplit = str.split(",");
                if (strArrSplit != null && strArrSplit.length == 2) {
                    try {
                        int i = Integer.parseInt(strArrSplit[0]);
                        int i2 = Integer.parseInt(strArrSplit[1]);
                        if (i2 > 0 && i > 0) {
                            this.mRetriesLeftPerErrorCode.put(i, i2);
                        }
                    } catch (NumberFormatException e) {
                        log("Exception parsing config_retries_per_error_code: " + e);
                    }
                } else {
                    log("Exception parsing config_retries_per_error_code: " + str);
                }
            }
        }
    }

    public boolean restartOnError(int i) {
        int i2;
        boolean z;
        synchronized (this.mRetriesLeftPerErrorCode) {
            i2 = this.mRetriesLeftPerErrorCode.get(i);
            z = false;
            switch (i2) {
                case 0:
                    break;
                case 1:
                    resetErrorCodeRetries();
                    z = true;
                    break;
                default:
                    this.mRetriesLeftPerErrorCode.put(i, i2 - 1);
                    break;
            }
        }
        requestLog("ApnContext.restartOnError(" + i + ") found " + i2 + " and returned " + z);
        return z;
    }

    public int incAndGetConnectionGeneration() {
        return this.mConnectionGeneration.incrementAndGet();
    }

    public int getConnectionGeneration() {
        return this.mConnectionGeneration.get();
    }

    long getRetryAfterDisconnectDelay() {
        return this.mRetryManager.getRetryAfterDisconnectDelay();
    }

    public static int apnIdForType(int i) {
        switch (i) {
            case 0:
                return 0;
            case 1:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 13:
            default:
                try {
                    if (sMethodApnIdForTypeEx != null) {
                        return ((Integer) sMethodApnIdForTypeEx.invoke(null, Integer.valueOf(i))).intValue();
                    }
                    return -1;
                } catch (Exception e) {
                    Rlog.d(SLOG_TAG, e.toString());
                    return -1;
                }
            case 2:
                return 1;
            case 3:
                return 2;
            case 4:
                return 3;
            case 10:
                return 6;
            case 11:
                return 5;
            case 12:
                return 7;
            case 14:
                return 8;
            case 15:
                return 9;
        }
    }

    public static int apnIdForNetworkRequest(NetworkRequest networkRequest) {
        boolean z;
        NetworkCapabilities networkCapabilities = networkRequest.networkCapabilities;
        if (networkCapabilities.getTransportTypes().length > 0 && !networkCapabilities.hasTransport(0)) {
            return -1;
        }
        int i = networkCapabilities.hasCapability(12) ? 0 : -1;
        if (networkCapabilities.hasCapability(0)) {
            z = i != -1;
            i = 1;
        } else {
            z = false;
        }
        if (networkCapabilities.hasCapability(1)) {
            if (i != -1) {
                z = true;
            }
            i = 2;
        }
        if (networkCapabilities.hasCapability(2)) {
            if (i != -1) {
                z = true;
            }
            i = 3;
        }
        if (networkCapabilities.hasCapability(3)) {
            if (i != -1) {
                z = true;
            }
            i = 6;
        }
        if (networkCapabilities.hasCapability(4)) {
            if (i != -1) {
                z = true;
            }
            i = 5;
        }
        if (networkCapabilities.hasCapability(5)) {
            if (i != -1) {
                z = true;
            }
            i = 7;
        }
        if (networkCapabilities.hasCapability(7)) {
            if (i != -1) {
                z = true;
            }
            i = 8;
        }
        if (networkCapabilities.hasCapability(8)) {
            if (i != -1) {
                z = true;
            }
            Rlog.d(SLOG_TAG, "RCS APN type not yet supported");
        }
        if (networkCapabilities.hasCapability(9)) {
            if (i != -1) {
                z = true;
            }
            Rlog.d(SLOG_TAG, "XCAP APN type not yet supported");
        }
        if (networkCapabilities.hasCapability(10)) {
            if (i != -1) {
                z = true;
            }
            i = 9;
        }
        try {
            if (sMethodApnIdForNetworkRequestEx != null) {
                Bundle bundle = (Bundle) sMethodApnIdForNetworkRequestEx.invoke(null, networkCapabilities, Integer.valueOf(i), Boolean.valueOf(z));
                int i2 = bundle.getInt("apnId");
                try {
                    z = bundle.getBoolean("error");
                    i = i2;
                } catch (Exception e) {
                    e = e;
                    i = i2;
                    Rlog.d(SLOG_TAG, e.toString());
                }
            }
        } catch (Exception e2) {
            e = e2;
        }
        if (z) {
            Rlog.d(SLOG_TAG, "Multiple apn types specified in request - result is unspecified!");
        }
        if (i == -1) {
            Rlog.d(SLOG_TAG, "Unsupported NetworkRequest in Telephony: nr=" + networkRequest);
        }
        return i;
    }

    public static int apnIdForApnName(String str) {
        switch (str) {
            case "default":
                return 0;
            case "mms":
                return 1;
            case "supl":
                return 2;
            case "dun":
                return 3;
            case "hipri":
                return 4;
            case "ims":
                return 5;
            case "fota":
                return 6;
            case "cbs":
                return 7;
            case "ia":
                return 8;
            case "emergency":
                return 9;
            default:
                try {
                    if (sMethodApnIdForApnNameEx != null) {
                        return ((Integer) sMethodApnIdForApnNameEx.invoke(null, str)).intValue();
                    }
                } catch (Exception e) {
                    Rlog.d(SLOG_TAG, e.toString());
                }
                return -1;
        }
    }

    private static String apnNameForApnId(int i) {
        switch (i) {
            case 0:
                return "default";
            case 1:
                return "mms";
            case 2:
                return "supl";
            case 3:
                return "dun";
            case 4:
                return "hipri";
            case 5:
                return "ims";
            case 6:
                return "fota";
            case 7:
                return "cbs";
            case 8:
                return "ia";
            case 9:
                return "emergency";
            default:
                Rlog.d(SLOG_TAG, "Unknown id (" + i + ") in apnIdToType");
                return "default";
        }
    }

    public synchronized String toString() {
        return "{mApnType=" + this.mApnType + " mState=" + getState() + " mWaitingApns={" + this.mRetryManager.getWaitingApns() + "} mApnSetting={" + this.mApnSetting + "} mReason=" + this.mReason + " mDataEnabled=" + this.mDataEnabled + " mDependencyMet=" + this.mDependencyMet + "}";
    }

    protected void log(String str) {
        Rlog.d(this.LOG_TAG, "[ApnContext:" + this.mApnType + "] " + str);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        synchronized (this.mRefCountLock) {
            indentingPrintWriter.println(toString());
            if (this.mNetworkRequests.size() > 0) {
                indentingPrintWriter.println("NetworkRequests:");
                indentingPrintWriter.increaseIndent();
                Iterator<NetworkRequest> it = this.mNetworkRequests.iterator();
                while (it.hasNext()) {
                    indentingPrintWriter.println(it.next());
                }
                indentingPrintWriter.decreaseIndent();
            }
            indentingPrintWriter.increaseIndent();
            Iterator<LocalLog> it2 = this.mLocalLogs.iterator();
            while (it2.hasNext()) {
                it2.next().dump(fileDescriptor, indentingPrintWriter, strArr);
                indentingPrintWriter.println("-----");
            }
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println("Historical APN state:");
            indentingPrintWriter.increaseIndent();
            this.mStateLocalLog.dump(fileDescriptor, indentingPrintWriter, strArr);
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println(this.mRetryManager);
            indentingPrintWriter.println("--------------------------");
        }
    }
}
