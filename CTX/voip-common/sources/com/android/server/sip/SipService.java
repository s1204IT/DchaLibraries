package com.android.server.sip;

import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.sip.ISipService;
import android.net.sip.ISipSession;
import android.net.sip.ISipSessionListener;
import android.net.sip.SipErrorCode;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipSessionAdapter;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.telephony.Rlog;
import com.android.server.sip.SipSessionGroup;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.sip.SipException;

public final class SipService extends ISipService.Stub {
    static final boolean DBG = true;
    private static final int DEFAULT_KEEPALIVE_INTERVAL = 10;
    private static final int DEFAULT_MAX_KEEPALIVE_INTERVAL = 120;
    private static final int EXPIRY_TIME = 3600;
    private static final int MIN_EXPIRY_TIME = 60;
    private static final int SHORT_EXPIRY_TIME = 10;
    static final String TAG = "SipService";
    private final AppOpsManager mAppOps;
    private ConnectivityReceiver mConnectivityReceiver;
    private Context mContext;
    private int mKeepAliveInterval;
    private String mLocalIp;
    private SipWakeLock mMyWakeLock;
    private SipKeepAliveProcessCallback mSipKeepAliveProcessCallback;
    private boolean mSipOnWifiOnly;
    private SipWakeupTimer mTimer;
    private WifiManager.WifiLock mWifiLock;
    private int mNetworkType = -1;
    private MyExecutor mExecutor = new MyExecutor();
    private Map<String, SipSessionGroupExt> mSipGroups = new HashMap();
    private Map<String, ISipSession> mPendingSessions = new HashMap();
    private int mLastGoodKeepAliveInterval = 10;

    public static void start(Context context) {
        if (SipManager.isApiSupported(context) && ServiceManager.getService("sip") == null) {
            ServiceManager.addService("sip", new SipService(context));
            context.sendBroadcast(new Intent(SipManager.ACTION_SIP_SERVICE_UP));
            slog("start:");
        }
    }

    private SipService(Context context) {
        log("SipService: started!");
        this.mContext = context;
        this.mConnectivityReceiver = new ConnectivityReceiver();
        this.mWifiLock = ((WifiManager) context.getSystemService("wifi")).createWifiLock(1, TAG);
        this.mWifiLock.setReferenceCounted(false);
        this.mSipOnWifiOnly = SipManager.isSipWifiOnly(context);
        this.mMyWakeLock = new SipWakeLock((PowerManager) context.getSystemService("power"));
        this.mTimer = new SipWakeupTimer(context, this.mExecutor);
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
    }

    @Override
    public synchronized SipProfile[] getListOfProfiles(String str) {
        if (!canUseSip(str, "getListOfProfiles")) {
            return new SipProfile[0];
        }
        boolean zIsCallerRadio = isCallerRadio();
        ArrayList arrayList = new ArrayList();
        for (SipSessionGroupExt sipSessionGroupExt : this.mSipGroups.values()) {
            if (zIsCallerRadio || isCallerCreator(sipSessionGroupExt)) {
                arrayList.add(sipSessionGroupExt.getLocalProfile());
            }
        }
        return (SipProfile[]) arrayList.toArray(new SipProfile[arrayList.size()]);
    }

    @Override
    public synchronized void open(SipProfile sipProfile, String str) {
        if (canUseSip(str, "open")) {
            sipProfile.setCallingUid(Binder.getCallingUid());
            try {
                createGroup(sipProfile);
            } catch (SipException e) {
                loge("openToMakeCalls()", e);
            }
        }
    }

    @Override
    public synchronized void open3(SipProfile sipProfile, PendingIntent pendingIntent, ISipSessionListener iSipSessionListener, String str) {
        if (canUseSip(str, "open3")) {
            sipProfile.setCallingUid(Binder.getCallingUid());
            if (pendingIntent == null) {
                log("open3: incomingCallPendingIntent cannot be null; the profile is not opened");
                return;
            }
            log("open3: " + obfuscateSipUri(sipProfile.getUriString()) + ": " + pendingIntent + ": " + iSipSessionListener);
            try {
                SipSessionGroupExt sipSessionGroupExtCreateGroup = createGroup(sipProfile, pendingIntent, iSipSessionListener);
                if (sipProfile.getAutoRegistration()) {
                    sipSessionGroupExtCreateGroup.openToReceiveCalls();
                    updateWakeLocks();
                }
            } catch (SipException e) {
                loge("open3:", e);
            }
        }
    }

    private boolean isCallerCreator(SipSessionGroupExt sipSessionGroupExt) {
        if (sipSessionGroupExt.getLocalProfile().getCallingUid() == Binder.getCallingUid()) {
            return DBG;
        }
        return false;
    }

    private boolean isCallerCreatorOrRadio(SipSessionGroupExt sipSessionGroupExt) {
        if (isCallerRadio() || isCallerCreator(sipSessionGroupExt)) {
            return DBG;
        }
        return false;
    }

    private boolean isCallerRadio() {
        if (Binder.getCallingUid() == 1001) {
            return DBG;
        }
        return false;
    }

    @Override
    public synchronized void close(String str, String str2) {
        if (canUseSip(str2, "close")) {
            SipSessionGroupExt sipSessionGroupExt = this.mSipGroups.get(str);
            if (sipSessionGroupExt == null) {
                return;
            }
            if (!isCallerCreatorOrRadio(sipSessionGroupExt)) {
                log("only creator or radio can close this profile");
                return;
            }
            SipSessionGroupExt sipSessionGroupExtRemove = this.mSipGroups.remove(str);
            notifyProfileRemoved(sipSessionGroupExtRemove.getLocalProfile());
            sipSessionGroupExtRemove.close();
            updateWakeLocks();
        }
    }

    @Override
    public synchronized boolean isOpened(String str, String str2) {
        if (!canUseSip(str2, "isOpened")) {
            return false;
        }
        SipSessionGroupExt sipSessionGroupExt = this.mSipGroups.get(str);
        if (sipSessionGroupExt == null) {
            return false;
        }
        if (isCallerCreatorOrRadio(sipSessionGroupExt)) {
            return DBG;
        }
        log("only creator or radio can query on the profile");
        return false;
    }

    @Override
    public synchronized boolean isRegistered(String str, String str2) {
        if (!canUseSip(str2, "isRegistered")) {
            return false;
        }
        SipSessionGroupExt sipSessionGroupExt = this.mSipGroups.get(str);
        if (sipSessionGroupExt == null) {
            return false;
        }
        if (isCallerCreatorOrRadio(sipSessionGroupExt)) {
            return sipSessionGroupExt.isRegistered();
        }
        log("only creator or radio can query on the profile");
        return false;
    }

    @Override
    public synchronized void setRegistrationListener(String str, ISipSessionListener iSipSessionListener, String str2) {
        if (canUseSip(str2, "setRegistrationListener")) {
            SipSessionGroupExt sipSessionGroupExt = this.mSipGroups.get(str);
            if (sipSessionGroupExt == null) {
                return;
            }
            if (isCallerCreator(sipSessionGroupExt)) {
                sipSessionGroupExt.setListener(iSipSessionListener);
            } else {
                log("only creator can set listener on the profile");
            }
        }
    }

    @Override
    public synchronized ISipSession createSession(SipProfile sipProfile, ISipSessionListener iSipSessionListener, String str) {
        log("createSession: profile" + sipProfile);
        if (!canUseSip(str, "createSession")) {
            return null;
        }
        sipProfile.setCallingUid(Binder.getCallingUid());
        if (this.mNetworkType == -1) {
            log("createSession: mNetworkType==-1 ret=null");
            return null;
        }
        try {
            return createGroup(sipProfile).createSession(iSipSessionListener);
        } catch (SipException e) {
            loge("createSession;", e);
            return null;
        }
    }

    @Override
    public synchronized ISipSession getPendingSession(String str, String str2) {
        if (!canUseSip(str2, "getPendingSession")) {
            return null;
        }
        if (str == null) {
            return null;
        }
        return this.mPendingSessions.get(str);
    }

    private String determineLocalIp() {
        try {
            DatagramSocket datagramSocket = new DatagramSocket();
            datagramSocket.connect(InetAddress.getByName("192.168.1.1"), 80);
            return datagramSocket.getLocalAddress().getHostAddress();
        } catch (IOException e) {
            loge("determineLocalIp()", e);
            return null;
        }
    }

    private SipSessionGroupExt createGroup(SipProfile sipProfile) throws SipException {
        String uriString = sipProfile.getUriString();
        SipSessionGroupExt sipSessionGroupExt = this.mSipGroups.get(uriString);
        if (sipSessionGroupExt == null) {
            SipSessionGroupExt sipSessionGroupExt2 = new SipSessionGroupExt(sipProfile, null, null);
            this.mSipGroups.put(uriString, sipSessionGroupExt2);
            notifyProfileAdded(sipProfile);
            return sipSessionGroupExt2;
        }
        if (!isCallerCreator(sipSessionGroupExt)) {
            throw new SipException("only creator can access the profile");
        }
        return sipSessionGroupExt;
    }

    private SipSessionGroupExt createGroup(SipProfile sipProfile, PendingIntent pendingIntent, ISipSessionListener iSipSessionListener) throws SipException {
        String uriString = sipProfile.getUriString();
        SipSessionGroupExt sipSessionGroupExt = this.mSipGroups.get(uriString);
        if (sipSessionGroupExt != null) {
            if (!isCallerCreator(sipSessionGroupExt)) {
                throw new SipException("only creator can access the profile");
            }
            sipSessionGroupExt.setIncomingCallPendingIntent(pendingIntent);
            sipSessionGroupExt.setListener(iSipSessionListener);
            return sipSessionGroupExt;
        }
        SipSessionGroupExt sipSessionGroupExt2 = new SipSessionGroupExt(sipProfile, pendingIntent, iSipSessionListener);
        this.mSipGroups.put(uriString, sipSessionGroupExt2);
        notifyProfileAdded(sipProfile);
        return sipSessionGroupExt2;
    }

    private void notifyProfileAdded(SipProfile sipProfile) {
        log("notify: profile added: " + sipProfile);
        Intent intent = new Intent(SipManager.ACTION_SIP_ADD_PHONE);
        intent.putExtra(SipManager.EXTRA_LOCAL_URI, sipProfile.getUriString());
        this.mContext.sendBroadcast(intent, "android.permission.USE_SIP");
        if (this.mSipGroups.size() == 1) {
            registerReceivers();
        }
    }

    private void notifyProfileRemoved(SipProfile sipProfile) {
        log("notify: profile removed: " + sipProfile);
        Intent intent = new Intent(SipManager.ACTION_SIP_REMOVE_PHONE);
        intent.putExtra(SipManager.EXTRA_LOCAL_URI, sipProfile.getUriString());
        this.mContext.sendBroadcast(intent, "android.permission.USE_SIP");
        if (this.mSipGroups.size() == 0) {
            unregisterReceivers();
        }
    }

    private void stopPortMappingMeasurement() {
        if (this.mSipKeepAliveProcessCallback != null) {
            this.mSipKeepAliveProcessCallback.stop();
            this.mSipKeepAliveProcessCallback = null;
        }
    }

    private void startPortMappingLifetimeMeasurement(SipProfile sipProfile) {
        startPortMappingLifetimeMeasurement(sipProfile, DEFAULT_MAX_KEEPALIVE_INTERVAL);
    }

    private void startPortMappingLifetimeMeasurement(SipProfile sipProfile, int i) {
        if (this.mSipKeepAliveProcessCallback == null && this.mKeepAliveInterval == -1 && isBehindNAT(this.mLocalIp)) {
            log("startPortMappingLifetimeMeasurement: profile=" + sipProfile.getUriString());
            int i2 = this.mLastGoodKeepAliveInterval;
            if (i2 >= i) {
                i2 = 10;
                this.mLastGoodKeepAliveInterval = 10;
                log("  reset min interval to 10");
            }
            this.mSipKeepAliveProcessCallback = new SipKeepAliveProcessCallback(sipProfile, i2, i);
            this.mSipKeepAliveProcessCallback.start();
        }
    }

    private void restartPortMappingLifetimeMeasurement(SipProfile sipProfile, int i) {
        stopPortMappingMeasurement();
        this.mKeepAliveInterval = -1;
        startPortMappingLifetimeMeasurement(sipProfile, i);
    }

    private synchronized void addPendingSession(ISipSession iSipSession) {
        try {
            cleanUpPendingSessions();
            this.mPendingSessions.put(iSipSession.getCallId(), iSipSession);
            log("#pending sess=" + this.mPendingSessions.size());
        } catch (RemoteException e) {
            loge("addPendingSession()", e);
        }
    }

    private void cleanUpPendingSessions() throws RemoteException {
        for (Map.Entry entry : (Map.Entry[]) this.mPendingSessions.entrySet().toArray(new Map.Entry[this.mPendingSessions.size()])) {
            if (((ISipSession) entry.getValue()).getState() != 3) {
                this.mPendingSessions.remove(entry.getKey());
            }
        }
    }

    private synchronized boolean callingSelf(SipSessionGroupExt sipSessionGroupExt, SipSessionGroup.SipSessionImpl sipSessionImpl) {
        String callId = sipSessionImpl.getCallId();
        for (SipSessionGroupExt sipSessionGroupExt2 : this.mSipGroups.values()) {
            if (sipSessionGroupExt2 != sipSessionGroupExt && sipSessionGroupExt2.containsSession(callId)) {
                log("call self: " + sipSessionImpl.getLocalProfile().getUriString() + " -> " + sipSessionGroupExt2.getLocalProfile().getUriString());
                return DBG;
            }
        }
        return false;
    }

    private synchronized void onKeepAliveIntervalChanged() {
        Iterator<SipSessionGroupExt> it = this.mSipGroups.values().iterator();
        while (it.hasNext()) {
            it.next().onKeepAliveIntervalChanged();
        }
    }

    private int getKeepAliveInterval() {
        if (this.mKeepAliveInterval < 0) {
            return this.mLastGoodKeepAliveInterval;
        }
        return this.mKeepAliveInterval;
    }

    private boolean isBehindNAT(String str) {
        byte[] address;
        try {
            address = InetAddress.getByName(str).getAddress();
        } catch (UnknownHostException e) {
            loge("isBehindAT()" + str, e);
        }
        if (address[0] != 10 && ((address[0] & 255) != 172 || (240 & address[1]) != 16)) {
            if ((address[0] & 255) == 192) {
                if ((255 & address[1]) == 168) {
                }
            }
            return false;
        }
        return DBG;
    }

    private boolean canUseSip(String str, String str2) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.USE_SIP", str2);
        if (this.mAppOps.noteOp(53, Binder.getCallingUid(), str) == 0) {
            return DBG;
        }
        return false;
    }

    private class SipSessionGroupExt extends SipSessionAdapter {
        private static final boolean SSGE_DBG = true;
        private static final String SSGE_TAG = "SipSessionGroupExt";
        private SipAutoReg mAutoRegistration;
        private PendingIntent mIncomingCallPendingIntent;
        private boolean mOpenedToReceiveCalls;
        private SipSessionGroup mSipGroup;

        public SipSessionGroupExt(SipProfile sipProfile, PendingIntent pendingIntent, ISipSessionListener iSipSessionListener) throws SipException {
            this.mAutoRegistration = new SipAutoReg();
            log("SipSessionGroupExt: profile=" + sipProfile);
            this.mSipGroup = new SipSessionGroup(duplicate(sipProfile), sipProfile.getPassword(), SipService.this.mTimer, SipService.this.mMyWakeLock);
            this.mIncomingCallPendingIntent = pendingIntent;
            this.mAutoRegistration.setListener(iSipSessionListener);
        }

        public SipProfile getLocalProfile() {
            return this.mSipGroup.getLocalProfile();
        }

        public boolean containsSession(String str) {
            return this.mSipGroup.containsSession(str);
        }

        public void onKeepAliveIntervalChanged() {
            this.mAutoRegistration.onKeepAliveIntervalChanged();
        }

        void setWakeupTimer(SipWakeupTimer sipWakeupTimer) {
            this.mSipGroup.setWakeupTimer(sipWakeupTimer);
        }

        private SipProfile duplicate(SipProfile sipProfile) {
            try {
                return new SipProfile.Builder(sipProfile).setPassword("*").build();
            } catch (Exception e) {
                loge("duplicate()", e);
                throw new RuntimeException("duplicate profile", e);
            }
        }

        public void setListener(ISipSessionListener iSipSessionListener) {
            this.mAutoRegistration.setListener(iSipSessionListener);
        }

        public void setIncomingCallPendingIntent(PendingIntent pendingIntent) {
            this.mIncomingCallPendingIntent = pendingIntent;
        }

        public void openToReceiveCalls() {
            this.mOpenedToReceiveCalls = SSGE_DBG;
            if (SipService.this.mNetworkType != -1) {
                this.mSipGroup.openToReceiveCalls(this);
                this.mAutoRegistration.start(this.mSipGroup);
            }
            log("openToReceiveCalls: " + SipService.obfuscateSipUri(getUri()) + ": " + this.mIncomingCallPendingIntent);
        }

        public void onConnectivityChanged(boolean z) throws SipException {
            log("onConnectivityChanged: connected=" + z + " uri=" + SipService.obfuscateSipUri(getUri()) + ": " + this.mIncomingCallPendingIntent);
            this.mSipGroup.onConnectivityChanged();
            if (z) {
                this.mSipGroup.reset();
                if (this.mOpenedToReceiveCalls) {
                    openToReceiveCalls();
                    return;
                }
                return;
            }
            this.mSipGroup.close();
            this.mAutoRegistration.stop();
        }

        public void close() {
            this.mOpenedToReceiveCalls = false;
            this.mSipGroup.close();
            this.mAutoRegistration.stop();
            log("close: " + SipService.obfuscateSipUri(getUri()) + ": " + this.mIncomingCallPendingIntent);
        }

        public ISipSession createSession(ISipSessionListener iSipSessionListener) {
            log("createSession");
            return this.mSipGroup.createSession(iSipSessionListener);
        }

        @Override
        public void onRinging(ISipSession iSipSession, SipProfile sipProfile, String str) {
            SipSessionGroup.SipSessionImpl sipSessionImpl = (SipSessionGroup.SipSessionImpl) iSipSession;
            synchronized (SipService.this) {
                try {
                    try {
                    } catch (PendingIntent.CanceledException e) {
                        loge("onRinging: pendingIntent is canceled, drop incoming call", e);
                        sipSessionImpl.endCall();
                    }
                    if (isRegistered() && !SipService.this.callingSelf(this, sipSessionImpl)) {
                        SipService.this.addPendingSession(sipSessionImpl);
                        Intent intentCreateIncomingCallBroadcast = SipManager.createIncomingCallBroadcast(sipSessionImpl.getCallId(), str);
                        log("onRinging: uri=" + getUri() + ": " + sipProfile.getUri() + ": " + sipSessionImpl.getCallId() + " " + this.mIncomingCallPendingIntent);
                        this.mIncomingCallPendingIntent.send(SipService.this.mContext, 101, intentCreateIncomingCallBroadcast);
                        return;
                    }
                    log("onRinging: end notReg or self");
                    sipSessionImpl.endCall();
                } catch (Throwable th) {
                    throw th;
                }
            }
        }

        @Override
        public void onError(ISipSession iSipSession, int i, String str) {
            log("onError: errorCode=" + i + " desc=" + SipErrorCode.toString(i) + ": " + str);
        }

        public boolean isOpenedToReceiveCalls() {
            return this.mOpenedToReceiveCalls;
        }

        public boolean isRegistered() {
            return this.mAutoRegistration.isRegistered();
        }

        private String getUri() {
            return this.mSipGroup.getLocalProfileUri();
        }

        private void log(String str) {
            Rlog.d(SSGE_TAG, str);
        }

        private void loge(String str, Throwable th) {
            Rlog.e(SSGE_TAG, str, th);
        }
    }

    private class SipKeepAliveProcessCallback implements Runnable, SipSessionGroup.KeepAliveProcessCallback {
        private static final int MIN_INTERVAL = 5;
        private static final int NAT_MEASUREMENT_RETRY_INTERVAL = 120;
        private static final int PASS_THRESHOLD = 10;
        private static final boolean SKAI_DBG = true;
        private static final String SKAI_TAG = "SipKeepAliveProcessCallback";
        private SipSessionGroupExt mGroup;
        private int mInterval;
        private SipProfile mLocalProfile;
        private int mMaxInterval;
        private int mMinInterval;
        private int mPassCount;
        private SipSessionGroup.SipSessionImpl mSession;

        public SipKeepAliveProcessCallback(SipProfile sipProfile, int i, int i2) {
            this.mMaxInterval = i2;
            this.mMinInterval = i;
            this.mLocalProfile = sipProfile;
        }

        public void start() {
            synchronized (SipService.this) {
                if (this.mSession != null) {
                    return;
                }
                this.mInterval = (this.mMaxInterval + this.mMinInterval) / 2;
                this.mPassCount = 0;
                if (this.mInterval < 10 || checkTermination()) {
                    log("start: measurement aborted; interval=[" + this.mMinInterval + "," + this.mMaxInterval + "]");
                    return;
                }
                try {
                    log("start: interval=" + this.mInterval);
                    this.mGroup = SipService.this.new SipSessionGroupExt(this.mLocalProfile, null, null);
                    this.mGroup.setWakeupTimer(new SipWakeupTimer(SipService.this.mContext, SipService.this.mExecutor));
                    this.mSession = (SipSessionGroup.SipSessionImpl) this.mGroup.createSession(null);
                    this.mSession.startKeepAliveProcess(this.mInterval, this);
                } catch (Throwable th) {
                    onError(-4, th.toString());
                }
            }
        }

        public void stop() {
            synchronized (SipService.this) {
                if (this.mSession != null) {
                    this.mSession.stopKeepAliveProcess();
                    this.mSession = null;
                }
                if (this.mGroup != null) {
                    this.mGroup.close();
                    this.mGroup = null;
                }
                SipService.this.mTimer.cancel(this);
                log("stop");
            }
        }

        private void restart() {
            synchronized (SipService.this) {
                if (this.mSession == null) {
                    return;
                }
                log("restart: interval=" + this.mInterval);
                try {
                    this.mSession.stopKeepAliveProcess();
                    this.mPassCount = 0;
                    this.mSession.startKeepAliveProcess(this.mInterval, this);
                } catch (SipException e) {
                    loge("restart", e);
                }
            }
        }

        private boolean checkTermination() {
            if (this.mMaxInterval - this.mMinInterval < 5) {
                return SKAI_DBG;
            }
            return false;
        }

        @Override
        public void onResponse(boolean z) {
            synchronized (SipService.this) {
                try {
                    if (!z) {
                        int i = this.mPassCount + 1;
                        this.mPassCount = i;
                        if (i != 10) {
                            return;
                        }
                        if (SipService.this.mKeepAliveInterval > 0) {
                            SipService.this.mLastGoodKeepAliveInterval = SipService.this.mKeepAliveInterval;
                        }
                        SipService sipService = SipService.this;
                        int i2 = this.mInterval;
                        this.mMinInterval = i2;
                        sipService.mKeepAliveInterval = i2;
                        log("onResponse: portChanged=" + z + " mKeepAliveInterval=" + SipService.this.mKeepAliveInterval);
                        SipService.this.onKeepAliveIntervalChanged();
                    } else {
                        this.mMaxInterval = this.mInterval;
                    }
                    if (checkTermination()) {
                        stop();
                        SipService.this.mKeepAliveInterval = this.mMinInterval;
                        log("onResponse: checkTermination mKeepAliveInterval=" + SipService.this.mKeepAliveInterval);
                    } else {
                        this.mInterval = (this.mMaxInterval + this.mMinInterval) / 2;
                        log("onResponse: mKeepAliveInterval=" + SipService.this.mKeepAliveInterval + ", new mInterval=" + this.mInterval);
                        restart();
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }

        @Override
        public void onError(int i, String str) {
            loge("onError: errorCode=" + i + " desc=" + str);
            restartLater();
        }

        @Override
        public void run() {
            SipService.this.mTimer.cancel(this);
            restart();
        }

        private void restartLater() {
            synchronized (SipService.this) {
                SipService.this.mTimer.cancel(this);
                SipService.this.mTimer.set(120000, this);
            }
        }

        private void log(String str) {
            Rlog.d(SKAI_TAG, str);
        }

        private void loge(String str) {
            Rlog.d(SKAI_TAG, str);
        }

        private void loge(String str, Throwable th) {
            Rlog.d(SKAI_TAG, str, th);
        }
    }

    private class SipAutoReg extends SipSessionAdapter implements Runnable, SipSessionGroup.KeepAliveProcessCallback {
        private static final int MIN_KEEPALIVE_SUCCESS_COUNT = 10;
        private static final boolean SAR_DBG = true;
        private String SAR_TAG;
        private int mBackoff;
        private int mErrorCode;
        private String mErrorMessage;
        private long mExpiryTime;
        private SipSessionGroup.SipSessionImpl mKeepAliveSession;
        private int mKeepAliveSuccessCount;
        private SipSessionListenerProxy mProxy;
        private boolean mRegistered;
        private boolean mRunning;
        private SipSessionGroup.SipSessionImpl mSession;

        private SipAutoReg() {
            this.mProxy = new SipSessionListenerProxy();
            this.mBackoff = 1;
            this.mRunning = false;
            this.mKeepAliveSuccessCount = 0;
        }

        public void start(SipSessionGroup sipSessionGroup) {
            if (!this.mRunning) {
                this.mRunning = SAR_DBG;
                this.mBackoff = 1;
                this.mSession = (SipSessionGroup.SipSessionImpl) sipSessionGroup.createSession(this);
                if (this.mSession == null) {
                    return;
                }
                SipService.this.mMyWakeLock.acquire(this.mSession);
                this.mSession.unregister();
                this.SAR_TAG = "SipAutoReg:" + SipService.obfuscateSipUri(this.mSession.getLocalProfile().getUriString());
                log("start: group=" + sipSessionGroup);
            }
        }

        private void startKeepAliveProcess(int i) {
            log("startKeepAliveProcess: interval=" + i);
            if (this.mKeepAliveSession == null) {
                this.mKeepAliveSession = this.mSession.duplicate();
            } else {
                this.mKeepAliveSession.stopKeepAliveProcess();
            }
            try {
                this.mKeepAliveSession.startKeepAliveProcess(i, this);
            } catch (SipException e) {
                loge("startKeepAliveProcess: interval=" + i, e);
            }
        }

        private void stopKeepAliveProcess() {
            if (this.mKeepAliveSession != null) {
                this.mKeepAliveSession.stopKeepAliveProcess();
                this.mKeepAliveSession = null;
            }
            this.mKeepAliveSuccessCount = 0;
        }

        @Override
        public void onResponse(boolean z) {
            synchronized (SipService.this) {
                try {
                    if (z) {
                        int keepAliveInterval = SipService.this.getKeepAliveInterval();
                        if (this.mKeepAliveSuccessCount < 10) {
                            log("onResponse: keepalive doesn't work with interval " + keepAliveInterval + ", past success count=" + this.mKeepAliveSuccessCount);
                            if (keepAliveInterval > 10) {
                                SipService.this.restartPortMappingLifetimeMeasurement(this.mSession.getLocalProfile(), keepAliveInterval);
                                this.mKeepAliveSuccessCount = 0;
                            }
                        } else {
                            log("keep keepalive going with interval " + keepAliveInterval + ", past success count=" + this.mKeepAliveSuccessCount);
                            this.mKeepAliveSuccessCount = this.mKeepAliveSuccessCount / 2;
                        }
                    } else {
                        SipService.this.startPortMappingLifetimeMeasurement(this.mSession.getLocalProfile());
                        this.mKeepAliveSuccessCount++;
                    }
                    if (this.mRunning && z) {
                        this.mKeepAliveSession = null;
                        SipService.this.mMyWakeLock.acquire(this.mSession);
                        this.mSession.register(SipService.EXPIRY_TIME);
                    }
                } finally {
                }
            }
        }

        @Override
        public void onError(int i, String str) {
            loge("onError: errorCode=" + i + " desc=" + str);
            onResponse(SAR_DBG);
        }

        public void stop() {
            if (this.mRunning) {
                this.mRunning = false;
                SipService.this.mMyWakeLock.release(this.mSession);
                if (this.mSession != null) {
                    this.mSession.setListener(null);
                    if (SipService.this.mNetworkType != -1 && this.mRegistered) {
                        this.mSession.unregister();
                    }
                }
                SipService.this.mTimer.cancel(this);
                stopKeepAliveProcess();
                this.mRegistered = false;
                setListener(this.mProxy.getListener());
            }
        }

        public void onKeepAliveIntervalChanged() {
            if (this.mKeepAliveSession != null) {
                int keepAliveInterval = SipService.this.getKeepAliveInterval();
                log("onKeepAliveIntervalChanged: interval=" + keepAliveInterval);
                this.mKeepAliveSuccessCount = 0;
                startKeepAliveProcess(keepAliveInterval);
            }
        }

        public void setListener(ISipSessionListener iSipSessionListener) {
            int state;
            synchronized (SipService.this) {
                this.mProxy.setListener(iSipSessionListener);
                try {
                    if (this.mSession == null) {
                        state = 0;
                    } else {
                        state = this.mSession.getState();
                    }
                    if (state == 1 || state == 2) {
                        this.mProxy.onRegistering(this.mSession);
                    } else if (this.mRegistered) {
                        this.mProxy.onRegistrationDone(this.mSession, (int) (this.mExpiryTime - SystemClock.elapsedRealtime()));
                    } else if (this.mErrorCode == 0) {
                        if (SipService.this.mNetworkType == -1) {
                            this.mProxy.onRegistrationFailed(this.mSession, -10, "no data connection");
                        } else if (!this.mRunning) {
                            this.mProxy.onRegistrationFailed(this.mSession, -4, "registration not running");
                        } else {
                            this.mProxy.onRegistrationFailed(this.mSession, -9, String.valueOf(state));
                        }
                    } else if (this.mErrorCode == -5) {
                        this.mProxy.onRegistrationTimeout(this.mSession);
                    } else {
                        this.mProxy.onRegistrationFailed(this.mSession, this.mErrorCode, this.mErrorMessage);
                    }
                } catch (Throwable th) {
                    loge("setListener: ", th);
                }
            }
        }

        public boolean isRegistered() {
            return this.mRegistered;
        }

        @Override
        public void run() {
            synchronized (SipService.this) {
                if (this.mRunning) {
                    this.mErrorCode = 0;
                    this.mErrorMessage = null;
                    log("run: registering");
                    if (SipService.this.mNetworkType != -1) {
                        SipService.this.mMyWakeLock.acquire(this.mSession);
                        this.mSession.register(SipService.EXPIRY_TIME);
                    }
                }
            }
        }

        private void restart(int i) {
            log("restart: duration=" + i + "s later.");
            SipService.this.mTimer.cancel(this);
            SipService.this.mTimer.set(i * 1000, this);
        }

        private int backoffDuration() {
            int i = 10 * this.mBackoff;
            if (i > SipService.EXPIRY_TIME) {
                return SipService.EXPIRY_TIME;
            }
            this.mBackoff *= 2;
            return i;
        }

        @Override
        public void onRegistering(ISipSession iSipSession) {
            log("onRegistering: " + iSipSession);
            synchronized (SipService.this) {
                if (notCurrentSession(iSipSession)) {
                    return;
                }
                this.mRegistered = false;
                this.mProxy.onRegistering(iSipSession);
            }
        }

        private boolean notCurrentSession(ISipSession iSipSession) {
            if (iSipSession != this.mSession) {
                ((SipSessionGroup.SipSessionImpl) iSipSession).setListener(null);
                SipService.this.mMyWakeLock.release(iSipSession);
                return SAR_DBG;
            }
            return this.mRunning ^ SAR_DBG;
        }

        @Override
        public void onRegistrationDone(ISipSession iSipSession, int i) {
            log("onRegistrationDone: " + iSipSession);
            synchronized (SipService.this) {
                if (notCurrentSession(iSipSession)) {
                    return;
                }
                this.mProxy.onRegistrationDone(iSipSession, i);
                if (i > 0) {
                    this.mExpiryTime = SystemClock.elapsedRealtime() + ((long) (i * 1000));
                    if (!this.mRegistered) {
                        this.mRegistered = SAR_DBG;
                        int i2 = i - 60;
                        if (i2 < SipService.MIN_EXPIRY_TIME) {
                            i2 = SipService.MIN_EXPIRY_TIME;
                        }
                        restart(i2);
                        SipProfile localProfile = this.mSession.getLocalProfile();
                        if (this.mKeepAliveSession == null && (SipService.this.isBehindNAT(SipService.this.mLocalIp) || localProfile.getSendKeepAlive())) {
                            startKeepAliveProcess(SipService.this.getKeepAliveInterval());
                        }
                    }
                    SipService.this.mMyWakeLock.release(iSipSession);
                } else {
                    this.mRegistered = false;
                    this.mExpiryTime = -1L;
                    log("Refresh registration immediately");
                    run();
                }
            }
        }

        @Override
        public void onRegistrationFailed(ISipSession iSipSession, int i, String str) {
            log("onRegistrationFailed: " + iSipSession + ": " + SipErrorCode.toString(i) + ": " + str);
            synchronized (SipService.this) {
                if (notCurrentSession(iSipSession)) {
                    return;
                }
                if (i == -12 || i == -8) {
                    log("   pause auto-registration");
                    stop();
                } else {
                    restartLater();
                }
                this.mErrorCode = i;
                this.mErrorMessage = str;
                this.mProxy.onRegistrationFailed(iSipSession, i, str);
                SipService.this.mMyWakeLock.release(iSipSession);
            }
        }

        @Override
        public void onRegistrationTimeout(ISipSession iSipSession) {
            log("onRegistrationTimeout: " + iSipSession);
            synchronized (SipService.this) {
                if (notCurrentSession(iSipSession)) {
                    return;
                }
                this.mErrorCode = -5;
                this.mProxy.onRegistrationTimeout(iSipSession);
                restartLater();
                SipService.this.mMyWakeLock.release(iSipSession);
            }
        }

        private void restartLater() {
            loge("restartLater");
            this.mRegistered = false;
            restart(backoffDuration());
        }

        private void log(String str) {
            Rlog.d(this.SAR_TAG, str);
        }

        private void loge(String str) {
            Rlog.e(this.SAR_TAG, str);
        }

        private void loge(String str, Throwable th) {
            Rlog.e(this.SAR_TAG, str, th);
        }
    }

    private class ConnectivityReceiver extends BroadcastReceiver {
        private ConnectivityReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                final NetworkInfo networkInfo = (NetworkInfo) extras.get("networkInfo");
                SipService.this.mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        SipService.this.onConnectivityChanged(networkInfo);
                    }
                });
            }
        }
    }

    private void registerReceivers() {
        this.mContext.registerReceiver(this.mConnectivityReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        log("registerReceivers:");
    }

    private void unregisterReceivers() {
        this.mContext.unregisterReceiver(this.mConnectivityReceiver);
        log("unregisterReceivers:");
        this.mWifiLock.release();
        this.mNetworkType = -1;
    }

    private void updateWakeLocks() {
        Iterator<SipSessionGroupExt> it = this.mSipGroups.values().iterator();
        while (it.hasNext()) {
            if (it.next().isOpenedToReceiveCalls()) {
                if (this.mNetworkType == 1 || this.mNetworkType == -1) {
                    this.mWifiLock.acquire();
                    return;
                } else {
                    this.mWifiLock.release();
                    return;
                }
            }
        }
        this.mWifiLock.release();
        this.mMyWakeLock.reset();
    }

    private synchronized void onConnectivityChanged(NetworkInfo networkInfo) {
        if (networkInfo != null) {
            try {
                if (networkInfo.isConnected() || networkInfo.getType() != this.mNetworkType) {
                    networkInfo = ((ConnectivityManager) this.mContext.getSystemService("connectivity")).getActiveNetworkInfo();
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        int type = (networkInfo == null || !networkInfo.isConnected()) ? -1 : networkInfo.getType();
        if (this.mSipOnWifiOnly && type != 1) {
            type = -1;
        }
        if (this.mNetworkType == type) {
            return;
        }
        log("onConnectivityChanged: " + this.mNetworkType + " -> " + type);
        try {
            if (this.mNetworkType != -1) {
                this.mLocalIp = null;
                stopPortMappingMeasurement();
                Iterator<SipSessionGroupExt> it = this.mSipGroups.values().iterator();
                while (it.hasNext()) {
                    it.next().onConnectivityChanged(false);
                }
            }
            this.mNetworkType = type;
            if (this.mNetworkType != -1) {
                this.mLocalIp = determineLocalIp();
                this.mKeepAliveInterval = -1;
                this.mLastGoodKeepAliveInterval = 10;
                Iterator<SipSessionGroupExt> it2 = this.mSipGroups.values().iterator();
                while (it2.hasNext()) {
                    it2.next().onConnectivityChanged(DBG);
                }
            }
            updateWakeLocks();
        } catch (SipException e) {
            loge("onConnectivityChanged()", e);
        }
    }

    private static Looper createLooper() {
        HandlerThread handlerThread = new HandlerThread("SipService.Executor");
        handlerThread.start();
        return handlerThread.getLooper();
    }

    private class MyExecutor extends Handler implements Executor {
        MyExecutor() {
            super(SipService.createLooper());
        }

        @Override
        public void execute(Runnable runnable) {
            SipService.this.mMyWakeLock.acquire(runnable);
            Message.obtain(this, 0, runnable).sendToTarget();
        }

        @Override
        public void handleMessage(Message message) {
            if (message.obj instanceof Runnable) {
                executeInternal((Runnable) message.obj);
                return;
            }
            SipService.this.log("handleMessage: not Runnable ignore msg=" + message);
        }

        private void executeInternal(Runnable runnable) {
            try {
                try {
                    runnable.run();
                } catch (Throwable th) {
                    SipService.this.loge("run task: " + runnable, th);
                }
            } finally {
                SipService.this.mMyWakeLock.release(runnable);
            }
        }
    }

    private void log(String str) {
        Rlog.d(TAG, str);
    }

    private static void slog(String str) {
        Rlog.d(TAG, str);
    }

    private void loge(String str, Throwable th) {
        Rlog.e(TAG, str, th);
    }

    public static String obfuscateSipUri(String str) {
        int i;
        StringBuilder sb = new StringBuilder();
        String strTrim = str.trim();
        if (strTrim.startsWith("sip:")) {
            i = 4;
            sb.append("sip:");
        } else {
            i = 0;
        }
        int length = strTrim.length();
        int i2 = i;
        char c = 0;
        while (i2 < length) {
            char cCharAt = strTrim.charAt(i2);
            int i3 = i2 + 1;
            char cCharAt2 = i3 < length ? strTrim.charAt(i3) : (char) 0;
            char c2 = '*';
            if (i2 - i < 1 || i3 == length || isAllowedCharacter(cCharAt) || c == '@' || cCharAt2 == '@') {
                c2 = cCharAt;
            }
            sb.append(c2);
            c = cCharAt;
            i2 = i3;
        }
        return sb.toString();
    }

    private static boolean isAllowedCharacter(char c) {
        if (c == '@' || c == '.') {
            return DBG;
        }
        return false;
    }
}
