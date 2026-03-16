package com.android.server;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.DataConnectionRealTimeInfo;
import android.telephony.PreciseCallState;
import android.telephony.PreciseDataConnectionState;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.VoLteServiceState;
import android.text.TextUtils;
import android.text.format.Time;
import com.android.internal.app.IBatteryStats;
import com.android.internal.telephony.DefaultPhoneNotifier;
import com.android.internal.telephony.IOnSubscriptionsChangedListener;
import com.android.internal.telephony.IPhoneStateListener;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.server.am.BatteryStatsService;
import com.android.server.pm.PackageManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TelephonyRegistry extends ITelephonyRegistry.Stub {
    private static final boolean DBG = false;
    private static final boolean DBG_LOC = false;
    private static final int MSG_UPDATE_DEFAULT_SUB = 2;
    private static final int MSG_USER_SWITCHED = 1;
    static final int PHONE_STATE_PERMISSION_MASK = 16620;
    static final int PRECISE_PHONE_STATE_PERMISSION_MASK = 6144;
    private static final String TAG = "TelephonyRegistry";
    private static final boolean VDBG = false;
    private ArrayList<HashMap<String, Integer>> mApnStateMap;
    private final IBatteryStats mBatteryStats;
    private boolean[] mCallForwarding;
    private String[] mCallIncomingNumber;
    private int[] mCallState;
    private ArrayList<List<CellInfo>> mCellInfo;
    private Bundle[] mCellLocation;
    private final Context mContext;
    private int[] mDataActivity;
    private String[] mDataConnectionApn;
    private LinkProperties[] mDataConnectionLinkProperties;
    private NetworkCapabilities[] mDataConnectionNetworkCapabilities;
    private int[] mDataConnectionNetworkType;
    private boolean[] mDataConnectionPossible;
    private String[] mDataConnectionReason;
    private int[] mDataConnectionState;
    private boolean[] mMessageWaiting;
    private int mNumPhones;
    private ServiceState[] mServiceState;
    private SignalStrength[] mSignalStrength;
    private final ArrayList<IBinder> mRemoveList = new ArrayList<>();
    private final ArrayList<Record> mRecords = new ArrayList<>();
    private boolean hasNotifySubscriptionInfoChangedOccurred = false;
    private int mOtaspMode = 1;
    private VoLteServiceState mVoLteServiceState = new VoLteServiceState();
    private int mDefaultSubId = -1;
    private int mDefaultPhoneId = -1;
    private DataConnectionRealTimeInfo mDcRtInfo = new DataConnectionRealTimeInfo();
    private int mRingingCallState = 0;
    private int mForegroundCallState = 0;
    private int mBackgroundCallState = 0;
    private PreciseCallState mPreciseCallState = new PreciseCallState();
    private PreciseDataConnectionState mPreciseDataConnectionState = new PreciseDataConnectionState();
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    int numPhones = TelephonyManager.getDefault().getPhoneCount();
                    for (int sub = 0; sub < numPhones; sub++) {
                        TelephonyRegistry.this.notifyCellLocationForSubscriber(sub, TelephonyRegistry.this.mCellLocation[sub]);
                    }
                    return;
                case 2:
                    int newDefaultPhoneId = msg.arg1;
                    int newDefaultSubId = ((Integer) msg.obj).intValue();
                    synchronized (TelephonyRegistry.this.mRecords) {
                        for (Record r : TelephonyRegistry.this.mRecords) {
                            if (r.subId == Integer.MAX_VALUE) {
                                TelephonyRegistry.this.checkPossibleMissNotify(r, newDefaultPhoneId);
                            }
                        }
                        TelephonyRegistry.this.handleRemoveListLocked();
                        break;
                    }
                    TelephonyRegistry.this.mDefaultSubId = newDefaultSubId;
                    TelephonyRegistry.this.mDefaultPhoneId = newDefaultPhoneId;
                    return;
                default:
                    return;
            }
        }
    };
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.USER_SWITCHED".equals(action)) {
                int userHandle = intent.getIntExtra("android.intent.extra.user_handle", 0);
                TelephonyRegistry.this.mHandler.sendMessage(TelephonyRegistry.this.mHandler.obtainMessage(1, userHandle, 0));
            } else if (action.equals("android.intent.action.ACTION_DEFAULT_SUBSCRIPTION_CHANGED")) {
                Integer newDefaultSubIdObj = new Integer(intent.getIntExtra("subscription", SubscriptionManager.getDefaultSubId()));
                int newDefaultPhoneId = intent.getIntExtra("slot", SubscriptionManager.getPhoneId(TelephonyRegistry.this.mDefaultSubId));
                if (TelephonyRegistry.this.validatePhoneId(newDefaultPhoneId)) {
                    if (newDefaultSubIdObj.equals(Integer.valueOf(TelephonyRegistry.this.mDefaultSubId)) || newDefaultPhoneId != TelephonyRegistry.this.mDefaultPhoneId) {
                        TelephonyRegistry.this.mHandler.sendMessage(TelephonyRegistry.this.mHandler.obtainMessage(2, newDefaultPhoneId, 0, newDefaultSubIdObj));
                    }
                }
            }
        }
    };
    private LogSSC[] logSSC = new LogSSC[10];
    private int next = 0;

    private static class Record {
        IBinder binder;
        IPhoneStateListener callback;
        int callerUid;
        int events;
        IOnSubscriptionsChangedListener onSubscriptionsChangedListenerCallback;
        int phoneId;
        String pkgForDebug;
        int subId;

        private Record() {
            this.subId = -1;
            this.phoneId = -1;
        }

        boolean matchPhoneStateListenerEvent(int events) {
            return (this.callback == null || (this.events & events) == 0) ? false : true;
        }

        boolean matchOnSubscriptionsChangedListener() {
            return this.onSubscriptionsChangedListenerCallback != null;
        }

        public String toString() {
            return "{pkgForDebug=" + this.pkgForDebug + " binder=" + this.binder + " callback=" + this.callback + " onSubscriptionsChangedListenererCallback=" + this.onSubscriptionsChangedListenerCallback + " callerUid=" + this.callerUid + " subId=" + this.subId + " phoneId=" + this.phoneId + " events=" + Integer.toHexString(this.events) + "}";
        }
    }

    TelephonyRegistry(Context context) {
        this.mCellInfo = null;
        CellLocation location = CellLocation.getEmpty();
        this.mContext = context;
        this.mBatteryStats = BatteryStatsService.getService();
        int numPhones = TelephonyManager.getDefault().getPhoneCount();
        this.mNumPhones = numPhones;
        this.mCallState = new int[numPhones];
        this.mDataActivity = new int[numPhones];
        this.mDataConnectionState = new int[numPhones];
        this.mDataConnectionNetworkType = new int[numPhones];
        this.mCallIncomingNumber = new String[numPhones];
        this.mServiceState = new ServiceState[numPhones];
        this.mSignalStrength = new SignalStrength[numPhones];
        this.mMessageWaiting = new boolean[numPhones];
        this.mDataConnectionPossible = new boolean[numPhones];
        this.mDataConnectionReason = new String[numPhones];
        this.mDataConnectionApn = new String[numPhones];
        this.mCallForwarding = new boolean[numPhones];
        this.mCellLocation = new Bundle[numPhones];
        this.mDataConnectionLinkProperties = new LinkProperties[numPhones];
        this.mDataConnectionNetworkCapabilities = new NetworkCapabilities[numPhones];
        this.mCellInfo = new ArrayList<>();
        this.mApnStateMap = new ArrayList<>();
        for (int i = 0; i < numPhones; i++) {
            this.mCallState[i] = 0;
            this.mDataActivity[i] = 0;
            this.mDataConnectionState[i] = -1;
            this.mCallIncomingNumber[i] = "";
            this.mServiceState[i] = new ServiceState();
            this.mSignalStrength[i] = new SignalStrength();
            this.mMessageWaiting[i] = false;
            this.mCallForwarding[i] = false;
            this.mDataConnectionPossible[i] = false;
            this.mDataConnectionReason[i] = "";
            this.mDataConnectionApn[i] = "";
            this.mCellLocation[i] = new Bundle();
            this.mCellInfo.add(i, null);
            this.mApnStateMap.add(i, new HashMap<>());
        }
        if (location != null) {
            for (int i2 = 0; i2 < numPhones; i2++) {
                location.fillInNotifierBundle(this.mCellLocation[i2]);
            }
        }
    }

    public void systemRunning() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_SWITCHED");
        filter.addAction("android.intent.action.USER_REMOVED");
        filter.addAction("android.intent.action.ACTION_DEFAULT_SUBSCRIPTION_CHANGED");
        log("systemRunning register for intents");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
    }

    public void addOnSubscriptionsChangedListener(String pkgForDebug, IOnSubscriptionsChangedListener callback) throws Throwable {
        Record r;
        int callerUid = UserHandle.getCallingUserId();
        UserHandle.myUserId();
        checkOnSubscriptionsChangedListenerPermission();
        synchronized (this.mRecords) {
            try {
                IBinder b = callback.asBinder();
                int N = this.mRecords.size();
                int i = 0;
                Record r2 = null;
                while (true) {
                    if (i >= N) {
                        break;
                    }
                    try {
                        r = this.mRecords.get(i);
                        if (b == r.binder) {
                            break;
                        }
                        i++;
                        r2 = r;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                r.onSubscriptionsChangedListenerCallback = callback;
                r.pkgForDebug = pkgForDebug;
                r.callerUid = callerUid;
                r.events = 0;
                if (this.hasNotifySubscriptionInfoChangedOccurred) {
                    try {
                        r.onSubscriptionsChangedListenerCallback.onSubscriptionsChanged();
                    } catch (RemoteException e) {
                        remove(r.binder);
                    }
                } else {
                    log("listen oscl: hasNotifySubscriptionInfoChangedOccurred==false no callback");
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    public void removeOnSubscriptionsChangedListener(String pkgForDebug, IOnSubscriptionsChangedListener callback) {
        remove(callback.asBinder());
    }

    private void checkOnSubscriptionsChangedListenerPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", null);
    }

    public void notifySubscriptionInfoChanged() {
        synchronized (this.mRecords) {
            if (!this.hasNotifySubscriptionInfoChangedOccurred) {
                log("notifySubscriptionInfoChanged: first invocation mRecords.size=" + this.mRecords.size());
            }
            this.hasNotifySubscriptionInfoChangedOccurred = true;
            this.mRemoveList.clear();
            for (Record r : this.mRecords) {
                if (r.matchOnSubscriptionsChangedListener()) {
                    try {
                        r.onSubscriptionsChangedListenerCallback.onSubscriptionsChanged();
                    } catch (RemoteException e) {
                        this.mRemoveList.add(r.binder);
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void listen(String pkgForDebug, IPhoneStateListener callback, int events, boolean notifyNow) throws Throwable {
        listenForSubscriber(Integer.MAX_VALUE, pkgForDebug, callback, events, notifyNow);
    }

    public void listenForSubscriber(int subId, String pkgForDebug, IPhoneStateListener callback, int events, boolean notifyNow) throws Throwable {
        listen(pkgForDebug, callback, events, notifyNow, subId);
    }

    private void listen(String pkgForDebug, IPhoneStateListener callback, int events, boolean notifyNow, int subId) throws Throwable {
        Record r;
        int callerUid = UserHandle.getCallingUserId();
        UserHandle.myUserId();
        if (events != 0) {
            checkListenerPermission(events);
            synchronized (this.mRecords) {
                try {
                    IBinder b = callback.asBinder();
                    int N = this.mRecords.size();
                    int i = 0;
                    Record r2 = null;
                    while (true) {
                        if (i >= N) {
                            break;
                        }
                        try {
                            r = this.mRecords.get(i);
                            if (b == r.binder) {
                                break;
                            }
                            i++;
                            r2 = r;
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                    r.callback = callback;
                    r.pkgForDebug = pkgForDebug;
                    r.callerUid = callerUid;
                    if (!SubscriptionManager.isValidSubscriptionId(subId)) {
                        r.subId = Integer.MAX_VALUE;
                    } else {
                        r.subId = subId;
                    }
                    r.phoneId = SubscriptionManager.getPhoneId(r.subId);
                    int phoneId = r.phoneId;
                    r.events = events;
                    if (notifyNow && validatePhoneId(phoneId)) {
                        if ((events & 1) != 0) {
                            try {
                                r.callback.onServiceStateChanged(new ServiceState(this.mServiceState[phoneId]));
                            } catch (RemoteException e) {
                                remove(r.binder);
                            }
                        }
                        if ((events & 2) != 0) {
                            try {
                                int gsmSignalStrength = this.mSignalStrength[phoneId].getGsmSignalStrength();
                                IPhoneStateListener iPhoneStateListener = r.callback;
                                if (gsmSignalStrength == 99) {
                                    gsmSignalStrength = -1;
                                }
                                iPhoneStateListener.onSignalStrengthChanged(gsmSignalStrength);
                            } catch (RemoteException e2) {
                                remove(r.binder);
                            }
                        }
                        if ((events & 4) != 0) {
                            try {
                                r.callback.onMessageWaitingIndicatorChanged(this.mMessageWaiting[phoneId]);
                            } catch (RemoteException e3) {
                                remove(r.binder);
                            }
                        }
                        if ((events & 8) != 0) {
                            try {
                                r.callback.onCallForwardingIndicatorChanged(this.mCallForwarding[phoneId]);
                            } catch (RemoteException e4) {
                                remove(r.binder);
                            }
                        }
                        if (validateEventsAndUserLocked(r, 16)) {
                            try {
                                r.callback.onCellLocationChanged(new Bundle(this.mCellLocation[phoneId]));
                            } catch (RemoteException e5) {
                                remove(r.binder);
                            }
                        }
                        if ((events & 32) != 0) {
                            try {
                                r.callback.onCallStateChanged(this.mCallState[phoneId], this.mCallIncomingNumber[phoneId]);
                            } catch (RemoteException e6) {
                                remove(r.binder);
                            }
                        }
                        if ((events & 64) != 0) {
                            try {
                                r.callback.onDataConnectionStateChanged(this.mDataConnectionState[phoneId], this.mDataConnectionNetworkType[phoneId]);
                            } catch (RemoteException e7) {
                                remove(r.binder);
                            }
                        }
                        if ((events & 128) != 0) {
                            try {
                                r.callback.onDataActivity(this.mDataActivity[phoneId]);
                            } catch (RemoteException e8) {
                                remove(r.binder);
                            }
                        }
                        if ((events & PackageManagerService.DumpState.DUMP_VERIFIERS) != 0) {
                            try {
                                r.callback.onSignalStrengthsChanged(this.mSignalStrength[phoneId]);
                            } catch (RemoteException e9) {
                                remove(r.binder);
                            }
                        }
                        if ((events & 512) != 0) {
                            try {
                                r.callback.onOtaspChanged(this.mOtaspMode);
                            } catch (RemoteException e10) {
                                remove(r.binder);
                            }
                        }
                        if (validateEventsAndUserLocked(r, 1024)) {
                            try {
                                r.callback.onCellInfoChanged(this.mCellInfo.get(phoneId));
                            } catch (RemoteException e11) {
                                remove(r.binder);
                            }
                        }
                        if ((events & PackageManagerService.DumpState.DUMP_INSTALLS) != 0) {
                            try {
                                r.callback.onDataConnectionRealTimeInfoChanged(this.mDcRtInfo);
                            } catch (RemoteException e12) {
                                remove(r.binder);
                            }
                        }
                        if ((events & PackageManagerService.DumpState.DUMP_KEYSETS) != 0) {
                            try {
                                r.callback.onPreciseCallStateChanged(this.mPreciseCallState);
                            } catch (RemoteException e13) {
                                remove(r.binder);
                            }
                        }
                        if ((events & PackageManagerService.DumpState.DUMP_VERSION) != 0) {
                            try {
                                r.callback.onPreciseDataConnectionStateChanged(this.mPreciseDataConnectionState);
                            } catch (RemoteException e14) {
                                remove(r.binder);
                            }
                        }
                    }
                    return;
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }
        remove(callback.asBinder());
    }

    private void remove(IBinder binder) {
        synchronized (this.mRecords) {
            int recordCount = this.mRecords.size();
            for (int i = 0; i < recordCount; i++) {
                if (this.mRecords.get(i).binder == binder) {
                    this.mRecords.remove(i);
                    return;
                }
            }
        }
    }

    public void notifyCallState(int state, String incomingNumber) {
        if (checkNotifyPermission("notifyCallState()")) {
            synchronized (this.mRecords) {
                for (Record r : this.mRecords) {
                    if (r.matchPhoneStateListenerEvent(32) && r.subId == Integer.MAX_VALUE) {
                        try {
                            r.callback.onCallStateChanged(state, incomingNumber);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
                handleRemoveListLocked();
            }
            broadcastCallStateChanged(state, incomingNumber, Integer.MAX_VALUE);
        }
    }

    public void notifyCallStateForSubscriber(int subId, int state, String incomingNumber) {
        if (checkNotifyPermission("notifyCallState()")) {
            synchronized (this.mRecords) {
                int phoneId = SubscriptionManager.getPhoneId(subId);
                if (validatePhoneId(phoneId)) {
                    this.mCallState[phoneId] = state;
                    this.mCallIncomingNumber[phoneId] = incomingNumber;
                    for (Record r : this.mRecords) {
                        if (r.matchPhoneStateListenerEvent(32) && r.subId == subId && r.subId != Integer.MAX_VALUE) {
                            try {
                                r.callback.onCallStateChanged(state, incomingNumber);
                            } catch (RemoteException e) {
                                this.mRemoveList.add(r.binder);
                            }
                        }
                    }
                    handleRemoveListLocked();
                } else {
                    handleRemoveListLocked();
                }
            }
            broadcastCallStateChanged(state, incomingNumber, subId);
        }
    }

    public void notifyServiceStateForPhoneId(int phoneId, int subId, ServiceState state) {
        if (checkNotifyPermission("notifyServiceState()")) {
            synchronized (this.mRecords) {
                if (validatePhoneId(phoneId)) {
                    this.mServiceState[phoneId] = state;
                    logServiceStateChanged("notifyServiceStateForSubscriber", subId, phoneId, state);
                    for (Record r : this.mRecords) {
                        if (r.matchPhoneStateListenerEvent(1) && idMatch(r.subId, subId, phoneId)) {
                            try {
                                r.callback.onServiceStateChanged(new ServiceState(state));
                            } catch (RemoteException e) {
                                this.mRemoveList.add(r.binder);
                            }
                        }
                    }
                } else {
                    log("notifyServiceStateForSubscriber: INVALID phoneId=" + phoneId);
                }
                handleRemoveListLocked();
            }
            broadcastServiceStateChanged(state, subId);
        }
    }

    public void notifySignalStrength(SignalStrength signalStrength) {
        notifySignalStrengthForSubscriber(Integer.MAX_VALUE, signalStrength);
    }

    private int getPhoneIdForDummySubId(int subId) {
        if (subId > -2 || subId <= (-2) - TelephonyManager.from(this.mContext).getSimCount()) {
            return -1;
        }
        int phoneId = (-2) - subId;
        return phoneId;
    }

    public void notifySignalStrengthForSubscriber(int subId, SignalStrength signalStrength) {
        if (checkNotifyPermission("notifySignalStrength()")) {
            synchronized (this.mRecords) {
                int phoneId = getPhoneIdForDummySubId(subId);
                if (!validatePhoneId(phoneId)) {
                    phoneId = SubscriptionManager.getPhoneId(subId);
                }
                if (validatePhoneId(phoneId)) {
                    this.mSignalStrength[phoneId] = signalStrength;
                    for (Record r : this.mRecords) {
                        if (r.matchPhoneStateListenerEvent(PackageManagerService.DumpState.DUMP_VERIFIERS) && idMatch(r.subId, subId, phoneId)) {
                            try {
                                r.callback.onSignalStrengthsChanged(new SignalStrength(signalStrength));
                            } catch (RemoteException e) {
                                this.mRemoveList.add(r.binder);
                            }
                            if (!r.matchPhoneStateListenerEvent(2)) {
                            }
                        } else if (!r.matchPhoneStateListenerEvent(2) && idMatch(r.subId, subId, phoneId)) {
                            try {
                                int gsmSignalStrength = signalStrength.getGsmSignalStrength();
                                int ss = gsmSignalStrength == 99 ? -1 : gsmSignalStrength;
                                r.callback.onSignalStrengthChanged(ss);
                            } catch (RemoteException e2) {
                                this.mRemoveList.add(r.binder);
                            }
                        }
                    }
                }
                log("notifySignalStrengthForSubscriber: invalid phoneId=" + phoneId);
                handleRemoveListLocked();
            }
            broadcastSignalStrengthChanged(signalStrength, subId);
        }
    }

    public void notifyCellInfo(List<CellInfo> cellInfo) {
        notifyCellInfoForSubscriber(Integer.MAX_VALUE, cellInfo);
    }

    public void notifyCellInfoForSubscriber(int subId, List<CellInfo> cellInfo) {
        if (checkNotifyPermission("notifyCellInfo()")) {
            synchronized (this.mRecords) {
                int phoneId = SubscriptionManager.getPhoneId(subId);
                if (validatePhoneId(phoneId)) {
                    this.mCellInfo.set(phoneId, cellInfo);
                    for (Record r : this.mRecords) {
                        if (validateEventsAndUserLocked(r, 1024) && idMatch(r.subId, subId, phoneId)) {
                            try {
                                r.callback.onCellInfoChanged(cellInfo);
                            } catch (RemoteException e) {
                                this.mRemoveList.add(r.binder);
                            }
                        }
                    }
                    handleRemoveListLocked();
                } else {
                    handleRemoveListLocked();
                }
            }
        }
    }

    public void notifyDataConnectionRealTimeInfo(DataConnectionRealTimeInfo dcRtInfo) {
        if (checkNotifyPermission("notifyDataConnectionRealTimeInfo()")) {
            synchronized (this.mRecords) {
                this.mDcRtInfo = dcRtInfo;
                for (Record r : this.mRecords) {
                    if (validateEventsAndUserLocked(r, PackageManagerService.DumpState.DUMP_INSTALLS)) {
                        try {
                            r.callback.onDataConnectionRealTimeInfoChanged(this.mDcRtInfo);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
                handleRemoveListLocked();
            }
        }
    }

    public void notifyMessageWaitingChangedForPhoneId(int phoneId, int subId, boolean mwi) {
        if (checkNotifyPermission("notifyMessageWaitingChanged()")) {
            synchronized (this.mRecords) {
                if (validatePhoneId(phoneId)) {
                    this.mMessageWaiting[phoneId] = mwi;
                    for (Record r : this.mRecords) {
                        if (r.matchPhoneStateListenerEvent(4) && idMatch(r.subId, subId, phoneId)) {
                            try {
                                r.callback.onMessageWaitingIndicatorChanged(mwi);
                            } catch (RemoteException e) {
                                this.mRemoveList.add(r.binder);
                            }
                        }
                    }
                    handleRemoveListLocked();
                } else {
                    handleRemoveListLocked();
                }
            }
        }
    }

    public void notifyCallForwardingChanged(boolean cfi) {
        notifyCallForwardingChangedForSubscriber(Integer.MAX_VALUE, cfi);
    }

    public void notifyCallForwardingChangedForSubscriber(int subId, boolean cfi) {
        if (checkNotifyPermission("notifyCallForwardingChanged()")) {
            synchronized (this.mRecords) {
                int phoneId = SubscriptionManager.getPhoneId(subId);
                if (validatePhoneId(phoneId)) {
                    this.mCallForwarding[phoneId] = cfi;
                    for (Record r : this.mRecords) {
                        if (r.matchPhoneStateListenerEvent(8) && idMatch(r.subId, subId, phoneId)) {
                            try {
                                r.callback.onCallForwardingIndicatorChanged(cfi);
                            } catch (RemoteException e) {
                                this.mRemoveList.add(r.binder);
                            }
                        }
                    }
                    handleRemoveListLocked();
                } else {
                    handleRemoveListLocked();
                }
            }
        }
    }

    public void notifyDataActivity(int state) {
        notifyDataActivityForSubscriber(Integer.MAX_VALUE, state);
    }

    public void notifyDataActivityForSubscriber(int subId, int state) {
        if (checkNotifyPermission("notifyDataActivity()")) {
            synchronized (this.mRecords) {
                int phoneId = SubscriptionManager.getPhoneId(subId);
                if (validatePhoneId(phoneId)) {
                    this.mDataActivity[phoneId] = state;
                    for (Record r : this.mRecords) {
                        if (r.matchPhoneStateListenerEvent(128)) {
                            try {
                                r.callback.onDataActivity(state);
                            } catch (RemoteException e) {
                                this.mRemoveList.add(r.binder);
                            }
                        }
                    }
                    handleRemoveListLocked();
                } else {
                    handleRemoveListLocked();
                }
            }
        }
    }

    private int getDataConnectionState(int phoneId, String newApnType, int newState) {
        int retState = 0;
        this.mApnStateMap.get(phoneId).put(newApnType, Integer.valueOf(newState));
        for (Map.Entry<String, Integer> entry : this.mApnStateMap.get(phoneId).entrySet()) {
            int state = entry.getValue().intValue();
            if (2 == state) {
                return 2;
            }
            if (retState < state) {
                retState = state;
            }
        }
        return retState;
    }

    public void notifyDataConnection(int state, boolean isDataConnectivityPossible, String reason, String apn, String apnType, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, int networkType, boolean roaming) {
        notifyDataConnectionForSubscriber(Integer.MAX_VALUE, state, isDataConnectivityPossible, reason, apn, apnType, linkProperties, networkCapabilities, networkType, roaming);
    }

    public void notifyDataConnectionForSubscriber(int subId, int state, boolean isDataConnectivityPossible, String reason, String apn, String apnType, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, int networkType, boolean roaming) {
        if (checkNotifyPermission("notifyDataConnection()")) {
            synchronized (this.mRecords) {
                int phoneId = SubscriptionManager.getPhoneId(subId);
                if (validatePhoneId(phoneId)) {
                    boolean modified = false;
                    int newState = getDataConnectionState(phoneId, apnType, state);
                    if (this.mDataConnectionState[phoneId] != newState) {
                        this.mDataConnectionState[phoneId] = newState;
                        modified = true;
                    }
                    this.mDataConnectionPossible[phoneId] = isDataConnectivityPossible;
                    this.mDataConnectionReason[phoneId] = reason;
                    this.mDataConnectionLinkProperties[phoneId] = linkProperties;
                    this.mDataConnectionNetworkCapabilities[phoneId] = networkCapabilities;
                    if (this.mDataConnectionNetworkType[phoneId] != networkType) {
                        this.mDataConnectionNetworkType[phoneId] = networkType;
                        modified = true;
                    }
                    if (modified) {
                        for (Record r : this.mRecords) {
                            if (r.matchPhoneStateListenerEvent(64) && idMatch(r.subId, subId, phoneId)) {
                                try {
                                    log("Notify data connection state changed on sub: " + subId);
                                    r.callback.onDataConnectionStateChanged(this.mDataConnectionState[phoneId], this.mDataConnectionNetworkType[phoneId]);
                                } catch (RemoteException e) {
                                    this.mRemoveList.add(r.binder);
                                }
                            }
                        }
                        handleRemoveListLocked();
                        this.mPreciseDataConnectionState = new PreciseDataConnectionState(state, networkType, apnType, apn, reason, linkProperties, "");
                        for (Record r2 : this.mRecords) {
                            if (r2.matchPhoneStateListenerEvent(PackageManagerService.DumpState.DUMP_VERSION)) {
                                try {
                                    r2.callback.onPreciseDataConnectionStateChanged(this.mPreciseDataConnectionState);
                                } catch (RemoteException e2) {
                                    this.mRemoveList.add(r2.binder);
                                }
                            }
                        }
                        handleRemoveListLocked();
                    } else {
                        this.mPreciseDataConnectionState = new PreciseDataConnectionState(state, networkType, apnType, apn, reason, linkProperties, "");
                        while (r15.hasNext()) {
                        }
                        handleRemoveListLocked();
                    }
                } else {
                    handleRemoveListLocked();
                }
            }
            broadcastDataConnectionStateChanged(state, isDataConnectivityPossible, reason, apn, apnType, linkProperties, networkCapabilities, roaming, subId);
            broadcastPreciseDataConnectionStateChanged(state, networkType, apnType, apn, reason, linkProperties, "");
        }
    }

    public void notifyDataConnectionFailed(String reason, String apnType) {
        notifyDataConnectionFailedForSubscriber(Integer.MAX_VALUE, reason, apnType);
    }

    public void notifyDataConnectionFailedForSubscriber(int subId, String reason, String apnType) {
        if (checkNotifyPermission("notifyDataConnectionFailed()")) {
            synchronized (this.mRecords) {
                this.mPreciseDataConnectionState = new PreciseDataConnectionState(-1, 0, apnType, "", reason, null, "");
                for (Record r : this.mRecords) {
                    if (r.matchPhoneStateListenerEvent(PackageManagerService.DumpState.DUMP_VERSION)) {
                        try {
                            r.callback.onPreciseDataConnectionStateChanged(this.mPreciseDataConnectionState);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
                handleRemoveListLocked();
            }
            broadcastDataConnectionFailed(reason, apnType, subId);
            broadcastPreciseDataConnectionStateChanged(-1, 0, apnType, "", reason, null, "");
        }
    }

    public void notifyCellLocation(Bundle cellLocation) {
        notifyCellLocationForSubscriber(Integer.MAX_VALUE, cellLocation);
    }

    public void notifyCellLocationForSubscriber(int subId, Bundle cellLocation) {
        log("notifyCellLocationForSubscriber: subId=" + subId + " cellLocation=" + cellLocation);
        if (checkNotifyPermission("notifyCellLocation()")) {
            synchronized (this.mRecords) {
                int phoneId = SubscriptionManager.getPhoneId(subId);
                if (validatePhoneId(phoneId)) {
                    this.mCellLocation[phoneId] = cellLocation;
                    for (Record r : this.mRecords) {
                        if (validateEventsAndUserLocked(r, 16) && idMatch(r.subId, subId, phoneId)) {
                            try {
                                r.callback.onCellLocationChanged(new Bundle(cellLocation));
                            } catch (RemoteException e) {
                                this.mRemoveList.add(r.binder);
                            }
                        }
                    }
                    handleRemoveListLocked();
                } else {
                    handleRemoveListLocked();
                }
            }
        }
    }

    public void notifyOtaspChanged(int otaspMode) {
        if (checkNotifyPermission("notifyOtaspChanged()")) {
            synchronized (this.mRecords) {
                this.mOtaspMode = otaspMode;
                for (Record r : this.mRecords) {
                    if (r.matchPhoneStateListenerEvent(512)) {
                        try {
                            r.callback.onOtaspChanged(otaspMode);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
                handleRemoveListLocked();
            }
        }
    }

    public void notifyPreciseCallState(int ringingCallState, int foregroundCallState, int backgroundCallState) {
        if (checkNotifyPermission("notifyPreciseCallState()")) {
            synchronized (this.mRecords) {
                this.mRingingCallState = ringingCallState;
                this.mForegroundCallState = foregroundCallState;
                this.mBackgroundCallState = backgroundCallState;
                this.mPreciseCallState = new PreciseCallState(ringingCallState, foregroundCallState, backgroundCallState, -1, -1);
                for (Record r : this.mRecords) {
                    if (r.matchPhoneStateListenerEvent(PackageManagerService.DumpState.DUMP_KEYSETS)) {
                        try {
                            r.callback.onPreciseCallStateChanged(this.mPreciseCallState);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
                handleRemoveListLocked();
            }
            broadcastPreciseCallStateChanged(ringingCallState, foregroundCallState, backgroundCallState, -1, -1);
        }
    }

    public void notifyDisconnectCause(int disconnectCause, int preciseDisconnectCause) {
        if (checkNotifyPermission("notifyDisconnectCause()")) {
            synchronized (this.mRecords) {
                this.mPreciseCallState = new PreciseCallState(this.mRingingCallState, this.mForegroundCallState, this.mBackgroundCallState, disconnectCause, preciseDisconnectCause);
                for (Record r : this.mRecords) {
                    if (r.matchPhoneStateListenerEvent(PackageManagerService.DumpState.DUMP_KEYSETS)) {
                        try {
                            r.callback.onPreciseCallStateChanged(this.mPreciseCallState);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
                handleRemoveListLocked();
            }
            broadcastPreciseCallStateChanged(this.mRingingCallState, this.mForegroundCallState, this.mBackgroundCallState, disconnectCause, preciseDisconnectCause);
        }
    }

    public void notifyPreciseDataConnectionFailed(String reason, String apnType, String apn, String failCause) {
        if (checkNotifyPermission("notifyPreciseDataConnectionFailed()")) {
            synchronized (this.mRecords) {
                this.mPreciseDataConnectionState = new PreciseDataConnectionState(-1, 0, apnType, apn, reason, null, failCause);
                for (Record r : this.mRecords) {
                    if (r.matchPhoneStateListenerEvent(PackageManagerService.DumpState.DUMP_VERSION)) {
                        try {
                            r.callback.onPreciseDataConnectionStateChanged(this.mPreciseDataConnectionState);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
                handleRemoveListLocked();
            }
            broadcastPreciseDataConnectionStateChanged(-1, 0, apnType, apn, reason, null, failCause);
        }
    }

    public void notifyVoLteServiceStateChanged(VoLteServiceState lteState) {
        if (checkNotifyPermission("notifyVoLteServiceStateChanged()")) {
            synchronized (this.mRecords) {
                this.mVoLteServiceState = lteState;
                for (Record r : this.mRecords) {
                    if (r.matchPhoneStateListenerEvent(16384)) {
                        try {
                            r.callback.onVoLteServiceStateChanged(new VoLteServiceState(this.mVoLteServiceState));
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
                handleRemoveListLocked();
            }
        }
    }

    public void notifyOemHookRawEventForSubscriber(int subId, byte[] rawData) {
        if (checkNotifyPermission("notifyOemHookRawEventForSubscriber")) {
            synchronized (this.mRecords) {
                for (Record r : this.mRecords) {
                    if (r.matchPhoneStateListenerEvent(32768) && (r.subId == subId || r.subId == Integer.MAX_VALUE)) {
                        try {
                            r.callback.onOemHookRawEvent(rawData);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
                handleRemoveListLocked();
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump telephony.registry from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        synchronized (this.mRecords) {
            int recordCount = this.mRecords.size();
            pw.println("last known state:");
            for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
                pw.println("  Phone Id=" + i);
                pw.println("  mCallState=" + this.mCallState[i]);
                pw.println("  mCallIncomingNumber=" + this.mCallIncomingNumber[i]);
                pw.println("  mServiceState=" + this.mServiceState[i]);
                pw.println("  mSignalStrength=" + this.mSignalStrength[i]);
                pw.println("  mMessageWaiting=" + this.mMessageWaiting[i]);
                pw.println("  mCallForwarding=" + this.mCallForwarding[i]);
                pw.println("  mDataActivity=" + this.mDataActivity[i]);
                pw.println("  mDataConnectionState=" + this.mDataConnectionState[i]);
                pw.println("  mDataConnectionPossible=" + this.mDataConnectionPossible[i]);
                pw.println("  mDataConnectionReason=" + this.mDataConnectionReason[i]);
                pw.println("  mDataConnectionApn=" + this.mDataConnectionApn[i]);
                pw.println("  mDataConnectionLinkProperties=" + this.mDataConnectionLinkProperties[i]);
                pw.println("  mDataConnectionNetworkCapabilities=" + this.mDataConnectionNetworkCapabilities[i]);
                pw.println("  mCellLocation=" + this.mCellLocation[i]);
                pw.println("  mCellInfo=" + this.mCellInfo.get(i));
            }
            pw.println("  mDcRtInfo=" + this.mDcRtInfo);
            pw.println("registrations: count=" + recordCount);
            for (Record r : this.mRecords) {
                pw.println("  " + r);
            }
        }
    }

    private void broadcastServiceStateChanged(ServiceState state, int subId) {
        long ident = Binder.clearCallingIdentity();
        try {
            this.mBatteryStats.notePhoneState(state.getState());
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
        Intent intent = new Intent("android.intent.action.SERVICE_STATE");
        Bundle data = new Bundle();
        state.fillInNotifierBundle(data);
        intent.putExtras(data);
        intent.putExtra("subscription", subId);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void broadcastSignalStrengthChanged(SignalStrength signalStrength, int subId) {
        long ident = Binder.clearCallingIdentity();
        try {
            this.mBatteryStats.notePhoneSignalStrength(signalStrength);
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
        Intent intent = new Intent("android.intent.action.SIG_STR");
        intent.addFlags(536870912);
        Bundle data = new Bundle();
        signalStrength.fillInNotifierBundle(data);
        intent.putExtras(data);
        intent.putExtra("subscription", subId);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void broadcastCallStateChanged(int state, String incomingNumber, int subId) {
        long ident = Binder.clearCallingIdentity();
        try {
            if (state == 0) {
                this.mBatteryStats.notePhoneOff();
            } else {
                this.mBatteryStats.notePhoneOn();
            }
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
        Intent intent = new Intent("android.intent.action.PHONE_STATE");
        intent.putExtra("state", DefaultPhoneNotifier.convertCallState(state).toString());
        if (!TextUtils.isEmpty(incomingNumber)) {
            intent.putExtra("incoming_number", incomingNumber);
        }
        intent.putExtra("subscription", subId);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.READ_PHONE_STATE");
    }

    private void broadcastDataConnectionStateChanged(int state, boolean isDataConnectivityPossible, String reason, String apn, String apnType, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, boolean roaming, int subId) {
        Intent intent = new Intent("android.intent.action.ANY_DATA_STATE");
        intent.putExtra("state", DefaultPhoneNotifier.convertDataState(state).toString());
        if (!isDataConnectivityPossible) {
            intent.putExtra("networkUnvailable", true);
        }
        if (reason != null) {
            intent.putExtra("reason", reason);
        }
        if (linkProperties != null) {
            intent.putExtra("linkProperties", linkProperties);
            String iface = linkProperties.getInterfaceName();
            if (iface != null) {
                intent.putExtra("iface", iface);
            }
        }
        if (networkCapabilities != null) {
            intent.putExtra("networkCapabilities", networkCapabilities);
        }
        if (roaming) {
            intent.putExtra("networkRoaming", true);
        }
        intent.putExtra("apn", apn);
        intent.putExtra("apnType", apnType);
        intent.putExtra("subscription", subId);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void broadcastDataConnectionFailed(String reason, String apnType, int subId) {
        Intent intent = new Intent("android.intent.action.DATA_CONNECTION_FAILED");
        intent.putExtra("reason", reason);
        intent.putExtra("apnType", apnType);
        intent.putExtra("subscription", subId);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void broadcastPreciseCallStateChanged(int ringingCallState, int foregroundCallState, int backgroundCallState, int disconnectCause, int preciseDisconnectCause) {
        Intent intent = new Intent("android.intent.action.PRECISE_CALL_STATE");
        intent.putExtra("ringing_state", ringingCallState);
        intent.putExtra("foreground_state", foregroundCallState);
        intent.putExtra("background_state", backgroundCallState);
        intent.putExtra("disconnect_cause", disconnectCause);
        intent.putExtra("precise_disconnect_cause", preciseDisconnectCause);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.READ_PRECISE_PHONE_STATE");
    }

    private void broadcastPreciseDataConnectionStateChanged(int state, int networkType, String apnType, String apn, String reason, LinkProperties linkProperties, String failCause) {
        Intent intent = new Intent("android.intent.action.PRECISE_DATA_CONNECTION_STATE_CHANGED");
        intent.putExtra("state", state);
        intent.putExtra("networkType", networkType);
        if (reason != null) {
            intent.putExtra("reason", reason);
        }
        if (apnType != null) {
            intent.putExtra("apnType", apnType);
        }
        if (apn != null) {
            intent.putExtra("apn", apn);
        }
        if (linkProperties != null) {
            intent.putExtra("linkProperties", linkProperties);
        }
        if (failCause != null) {
            intent.putExtra("failCause", failCause);
        }
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.READ_PRECISE_PHONE_STATE");
    }

    private boolean checkNotifyPermission(String method) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") == 0) {
            return true;
        }
        String str = "Modify Phone State Permission Denial: " + method + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
        return false;
    }

    private void checkListenerPermission(int events) {
        if ((events & 16) != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_COARSE_LOCATION", null);
        }
        if ((events & 1024) != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_COARSE_LOCATION", null);
        }
        if ((events & PHONE_STATE_PERMISSION_MASK) != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", null);
        }
        if ((events & PRECISE_PHONE_STATE_PERMISSION_MASK) != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRECISE_PHONE_STATE", null);
        }
        if ((32768 & events) != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", null);
        }
    }

    private void handleRemoveListLocked() {
        int size = this.mRemoveList.size();
        if (size > 0) {
            for (IBinder b : this.mRemoveList) {
                remove(b);
            }
            this.mRemoveList.clear();
        }
    }

    private boolean validateEventsAndUserLocked(Record r, int events) {
        boolean valid;
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            int foregroundUser = ActivityManager.getCurrentUser();
            if (r.callerUid == foregroundUser) {
                valid = r.matchPhoneStateListenerEvent(events);
            }
            return valid;
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    private boolean validatePhoneId(int phoneId) {
        return phoneId >= 0 && phoneId < this.mNumPhones;
    }

    private static void log(String s) {
        Rlog.d(TAG, s);
    }

    private static class LogSSC {
        private int mPhoneId;
        private String mS;
        private ServiceState mState;
        private int mSubId;
        private Time mTime;

        private LogSSC() {
        }

        public void set(Time t, String s, int subId, int phoneId, ServiceState state) {
            this.mTime = t;
            this.mS = s;
            this.mSubId = subId;
            this.mPhoneId = phoneId;
            this.mState = state;
        }

        public String toString() {
            return this.mS + " Time " + this.mTime.toString() + " mSubId " + this.mSubId + " mPhoneId " + this.mPhoneId + "  mState " + this.mState;
        }
    }

    private void logServiceStateChanged(String s, int subId, int phoneId, ServiceState state) {
        if (this.logSSC != null && this.logSSC.length != 0) {
            if (this.logSSC[this.next] == null) {
                this.logSSC[this.next] = new LogSSC();
            }
            Time t = new Time();
            t.setToNow();
            this.logSSC[this.next].set(t, s, subId, phoneId, state);
            int i = this.next + 1;
            this.next = i;
            if (i >= this.logSSC.length) {
                this.next = 0;
            }
        }
    }

    private void toStringLogSSC(String prompt) {
        if (this.logSSC == null || this.logSSC.length == 0 || (this.next == 0 && this.logSSC[this.next] == null)) {
            log(prompt + ": logSSC is empty");
            return;
        }
        log(prompt + ": logSSC.length=" + this.logSSC.length + " next=" + this.next);
        int i = this.next;
        if (this.logSSC[i] == null) {
            i = 0;
        }
        do {
            log(this.logSSC[i].toString());
            i++;
            if (i >= this.logSSC.length) {
                i = 0;
            }
        } while (i != this.next);
        log(prompt + ": ----------------");
    }

    boolean idMatch(int rSubId, int subId, int phoneId) {
        return subId < 0 ? this.mDefaultPhoneId == phoneId : rSubId == Integer.MAX_VALUE ? subId == this.mDefaultSubId : rSubId == subId;
    }

    private void checkPossibleMissNotify(Record r, int phoneId) {
        int events = r.events;
        if ((events & 1) != 0) {
            try {
                r.callback.onServiceStateChanged(new ServiceState(this.mServiceState[phoneId]));
            } catch (RemoteException e) {
                this.mRemoveList.add(r.binder);
            }
        }
        if ((events & PackageManagerService.DumpState.DUMP_VERIFIERS) != 0) {
            try {
                SignalStrength signalStrength = this.mSignalStrength[phoneId];
                r.callback.onSignalStrengthsChanged(new SignalStrength(signalStrength));
            } catch (RemoteException e2) {
                this.mRemoveList.add(r.binder);
            }
        }
        if ((events & 2) != 0) {
            try {
                int gsmSignalStrength = this.mSignalStrength[phoneId].getGsmSignalStrength();
                IPhoneStateListener iPhoneStateListener = r.callback;
                if (gsmSignalStrength == 99) {
                    gsmSignalStrength = -1;
                }
                iPhoneStateListener.onSignalStrengthChanged(gsmSignalStrength);
            } catch (RemoteException e3) {
                this.mRemoveList.add(r.binder);
            }
        }
        if (validateEventsAndUserLocked(r, 1024)) {
            try {
                r.callback.onCellInfoChanged(this.mCellInfo.get(phoneId));
            } catch (RemoteException e4) {
                this.mRemoveList.add(r.binder);
            }
        }
        if ((events & 4) != 0) {
            try {
                r.callback.onMessageWaitingIndicatorChanged(this.mMessageWaiting[phoneId]);
            } catch (RemoteException e5) {
                this.mRemoveList.add(r.binder);
            }
        }
        if ((events & 8) != 0) {
            try {
                r.callback.onCallForwardingIndicatorChanged(this.mCallForwarding[phoneId]);
            } catch (RemoteException e6) {
                this.mRemoveList.add(r.binder);
            }
        }
        if (validateEventsAndUserLocked(r, 16)) {
            try {
                r.callback.onCellLocationChanged(new Bundle(this.mCellLocation[phoneId]));
            } catch (RemoteException e7) {
                this.mRemoveList.add(r.binder);
            }
        }
        if ((events & 64) != 0) {
            try {
                r.callback.onDataConnectionStateChanged(this.mDataConnectionState[phoneId], this.mDataConnectionNetworkType[phoneId]);
            } catch (RemoteException e8) {
                this.mRemoveList.add(r.binder);
            }
        }
    }
}
