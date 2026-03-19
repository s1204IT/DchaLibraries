package com.android.server;

import android.app.ActivityManager;
import android.app.AppOpsManager;
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
import android.telephony.LocationAccessPolicy;
import android.telephony.PhysicalChannelConfig;
import android.telephony.PreciseCallState;
import android.telephony.PreciseDataConnectionState;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.VoLteServiceState;
import android.text.TextUtils;
import android.util.LocalLog;
import com.android.internal.app.IBatteryStats;
import com.android.internal.telephony.IOnSubscriptionsChangedListener;
import com.android.internal.telephony.IPhoneStateListener;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.PhoneConstantConversions;
import com.android.internal.telephony.TelephonyPermissions;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.BatteryService;
import com.android.server.am.BatteryStatsService;
import com.android.server.audio.AudioService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.pm.DumpState;
import com.android.server.policy.PhoneWindowManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

class TelephonyRegistry extends ITelephonyRegistry.Stub {
    private static final boolean DBG = false;
    private static final boolean DBG_LOC = false;
    static final int ENFORCE_COARSE_LOCATION_PERMISSION_MASK = 1040;
    static final int ENFORCE_PHONE_STATE_PERMISSION_MASK = 16396;
    private static final int MSG_UPDATE_DEFAULT_SUB = 2;
    private static final int MSG_USER_SWITCHED = 1;
    static final int PRECISE_PHONE_STATE_PERMISSION_MASK = 6144;
    private static final String TAG = "TelephonyRegistry";
    private static final boolean VDBG = false;
    private static Method sMethodIdMatchEx;
    private final AppOpsManager mAppOps;
    private final IBatteryStats mBatteryStats;
    private boolean[] mCallForwarding;
    private String[] mCallIncomingNumber;
    private int[] mCallState;
    private ArrayList<List<CellInfo>> mCellInfo;
    private Bundle[] mCellLocation;
    private final Context mContext;
    private int[] mDataActivationState;
    private int[] mDataActivity;
    private int[] mDataConnectionNetworkType;
    private int[] mDataConnectionState;
    private boolean[] mMessageWaiting;
    private int mNumPhones;
    private ArrayList<List<PhysicalChannelConfig>> mPhysicalChannelConfigs;
    private PreciseDataConnectionState[] mPreciseDataConnectionState;
    private ServiceState[] mServiceState;
    private SignalStrength[] mSignalStrength;
    private boolean[] mUserMobileDataState;
    private int[] mVoiceActivationState;
    private final ArrayList<IBinder> mRemoveList = new ArrayList<>();
    private final ArrayList<Record> mRecords = new ArrayList<>();
    private boolean hasNotifySubscriptionInfoChangedOccurred = false;
    private int mOtaspMode = 1;
    private VoLteServiceState mVoLteServiceState = new VoLteServiceState();
    private int mDefaultSubId = -1;
    private int mDefaultPhoneId = -1;
    private int mRingingCallState = 0;
    private int mForegroundCallState = 0;
    private int mBackgroundCallState = 0;
    private PreciseCallState mPreciseCallState = new PreciseCallState();
    private boolean mCarrierNetworkChangeState = false;
    private final LocalLog mLocalLog = new LocalLog(100);
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    int phoneCount = TelephonyManager.getDefault().getPhoneCount();
                    for (int i = 0; i < phoneCount; i++) {
                        TelephonyRegistry.this.notifyCellLocationForSubscriber(i, TelephonyRegistry.this.mCellLocation[i]);
                    }
                    return;
                case 2:
                    int i2 = message.arg1;
                    int iIntValue = ((Integer) message.obj).intValue();
                    synchronized (TelephonyRegistry.this.mRecords) {
                        for (Record record : TelephonyRegistry.this.mRecords) {
                            if (record.subId == Integer.MAX_VALUE) {
                                TelephonyRegistry.this.checkPossibleMissNotify(record, i2);
                            }
                        }
                        TelephonyRegistry.this.handleRemoveListLocked();
                        break;
                    }
                    TelephonyRegistry.this.mDefaultSubId = iIntValue;
                    TelephonyRegistry.this.mDefaultPhoneId = i2;
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
                TelephonyRegistry.this.mHandler.sendMessage(TelephonyRegistry.this.mHandler.obtainMessage(1, intent.getIntExtra("android.intent.extra.user_handle", 0), 0));
            } else if (action.equals("android.telephony.action.DEFAULT_SUBSCRIPTION_CHANGED")) {
                Integer num = new Integer(intent.getIntExtra("subscription", SubscriptionManager.getDefaultSubscriptionId()));
                int intExtra = intent.getIntExtra("slot", SubscriptionManager.getPhoneId(TelephonyRegistry.this.mDefaultSubId));
                if (TelephonyRegistry.this.validatePhoneId(intExtra)) {
                    if (!num.equals(Integer.valueOf(TelephonyRegistry.this.mDefaultSubId)) || intExtra != TelephonyRegistry.this.mDefaultPhoneId) {
                        TelephonyRegistry.this.mHandler.sendMessage(TelephonyRegistry.this.mHandler.obtainMessage(2, intExtra, 0, num));
                    }
                }
            }
        }
    };

    static {
        Class<?> cls;
        try {
            cls = Class.forName("com.mediatek.internal.telephony.MtkTelephonyRegistryEx");
        } catch (Exception e) {
            e.printStackTrace();
            log(e.toString());
            cls = null;
        }
        if (cls != null) {
            try {
                sMethodIdMatchEx = cls.getDeclaredMethod("idMatchEx", Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE);
                sMethodIdMatchEx.setAccessible(true);
            } catch (Exception e2) {
                e2.printStackTrace();
                log(e2.toString());
            }
        }
    }

    private static class Record {
        IBinder binder;
        IPhoneStateListener callback;
        int callerPid;
        int callerUid;
        String callingPackage;
        Context context;
        TelephonyRegistryDeathRecipient deathRecipient;
        int events;
        IOnSubscriptionsChangedListener onSubscriptionsChangedListenerCallback;
        int phoneId;
        int subId;

        private Record() {
            this.subId = -1;
            this.phoneId = -1;
        }

        boolean matchPhoneStateListenerEvent(int i) {
            return (this.callback == null || (i & this.events) == 0) ? false : true;
        }

        boolean matchOnSubscriptionsChangedListener() {
            return this.onSubscriptionsChangedListenerCallback != null;
        }

        boolean canReadCallLog() {
            try {
                return TelephonyPermissions.checkReadCallLog(this.context, this.subId, this.callerPid, this.callerUid, this.callingPackage);
            } catch (SecurityException e) {
                return false;
            }
        }

        public String toString() {
            return "{callingPackage=" + this.callingPackage + " binder=" + this.binder + " callback=" + this.callback + " onSubscriptionsChangedListenererCallback=" + this.onSubscriptionsChangedListenerCallback + " callerUid=" + this.callerUid + " subId=" + this.subId + " phoneId=" + this.phoneId + " events=" + Integer.toHexString(this.events) + "}";
        }
    }

    private class TelephonyRegistryDeathRecipient implements IBinder.DeathRecipient {
        private final IBinder binder;

        TelephonyRegistryDeathRecipient(IBinder iBinder) {
            this.binder = iBinder;
        }

        @Override
        public void binderDied() {
            TelephonyRegistry.this.remove(this.binder);
        }
    }

    TelephonyRegistry(Context context) {
        this.mCellInfo = null;
        CellLocation empty = CellLocation.getEmpty();
        this.mContext = context;
        this.mBatteryStats = BatteryStatsService.getService();
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
        this.mNumPhones = phoneCount;
        this.mCallState = new int[phoneCount];
        this.mDataActivity = new int[phoneCount];
        this.mDataConnectionState = new int[phoneCount];
        this.mDataConnectionNetworkType = new int[phoneCount];
        this.mCallIncomingNumber = new String[phoneCount];
        this.mServiceState = new ServiceState[phoneCount];
        this.mVoiceActivationState = new int[phoneCount];
        this.mDataActivationState = new int[phoneCount];
        this.mUserMobileDataState = new boolean[phoneCount];
        this.mSignalStrength = new SignalStrength[phoneCount];
        this.mMessageWaiting = new boolean[phoneCount];
        this.mCallForwarding = new boolean[phoneCount];
        this.mCellLocation = new Bundle[phoneCount];
        this.mCellInfo = new ArrayList<>();
        this.mPhysicalChannelConfigs = new ArrayList<>();
        this.mPreciseDataConnectionState = new PreciseDataConnectionState[phoneCount];
        for (int i = 0; i < phoneCount; i++) {
            this.mCallState[i] = 0;
            this.mDataActivity[i] = 0;
            this.mDataConnectionState[i] = -1;
            this.mVoiceActivationState[i] = 0;
            this.mDataActivationState[i] = 0;
            this.mCallIncomingNumber[i] = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            this.mServiceState[i] = makeServiceState();
            this.mSignalStrength[i] = makeSignalStrength(i);
            this.mUserMobileDataState[i] = false;
            this.mMessageWaiting[i] = false;
            this.mCallForwarding[i] = false;
            this.mCellLocation[i] = new Bundle();
            this.mCellInfo.add(i, null);
            this.mPhysicalChannelConfigs.add(i, new ArrayList());
            this.mPreciseDataConnectionState[i] = new PreciseDataConnectionState();
        }
        if (empty != null) {
            for (int i2 = 0; i2 < phoneCount; i2++) {
                empty.fillInNotifierBundle(this.mCellLocation[i2]);
            }
        }
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
    }

    public void systemRunning() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.telephony.action.DEFAULT_SUBSCRIPTION_CHANGED");
        log("systemRunning register for intents");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
    }

    public void addOnSubscriptionsChangedListener(String str, IOnSubscriptionsChangedListener iOnSubscriptionsChangedListener) {
        UserHandle.getCallingUserId();
        this.mAppOps.checkPackage(Binder.getCallingUid(), str);
        synchronized (this.mRecords) {
            Record recordAdd = add(iOnSubscriptionsChangedListener.asBinder());
            if (recordAdd == null) {
                return;
            }
            recordAdd.context = this.mContext;
            recordAdd.onSubscriptionsChangedListenerCallback = iOnSubscriptionsChangedListener;
            recordAdd.callingPackage = str;
            recordAdd.callerUid = Binder.getCallingUid();
            recordAdd.callerPid = Binder.getCallingPid();
            recordAdd.events = 0;
            if (this.hasNotifySubscriptionInfoChangedOccurred) {
                try {
                    recordAdd.onSubscriptionsChangedListenerCallback.onSubscriptionsChanged();
                } catch (RemoteException e) {
                    remove(recordAdd.binder);
                }
            } else {
                log("listen oscl: hasNotifySubscriptionInfoChangedOccurred==false no callback");
            }
        }
    }

    public void removeOnSubscriptionsChangedListener(String str, IOnSubscriptionsChangedListener iOnSubscriptionsChangedListener) {
        remove(iOnSubscriptionsChangedListener.asBinder());
    }

    public void notifySubscriptionInfoChanged() {
        synchronized (this.mRecords) {
            if (!this.hasNotifySubscriptionInfoChangedOccurred) {
                log("notifySubscriptionInfoChanged: first invocation mRecords.size=" + this.mRecords.size());
            }
            this.hasNotifySubscriptionInfoChangedOccurred = true;
            this.mRemoveList.clear();
            for (Record record : this.mRecords) {
                if (record.matchOnSubscriptionsChangedListener()) {
                    try {
                        record.onSubscriptionsChangedListenerCallback.onSubscriptionsChanged();
                    } catch (RemoteException e) {
                        this.mRemoveList.add(record.binder);
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void listen(String str, IPhoneStateListener iPhoneStateListener, int i, boolean z) {
        listenForSubscriber(Integer.MAX_VALUE, str, iPhoneStateListener, i, z);
    }

    public void listenForSubscriber(int i, String str, IPhoneStateListener iPhoneStateListener, int i2, boolean z) {
        listen(str, iPhoneStateListener, i2, z, i);
    }

    private void listen(String str, IPhoneStateListener iPhoneStateListener, int i, boolean z, int i2) {
        UserHandle.getCallingUserId();
        this.mAppOps.checkPackage(Binder.getCallingUid(), str);
        if (i != 0) {
            if (!checkListenerPermission(i, i2, str, "listen")) {
                return;
            }
            int phoneId = SubscriptionManager.getPhoneId(i2);
            synchronized (this.mRecords) {
                Record recordAdd = add(iPhoneStateListener.asBinder());
                if (recordAdd == null) {
                    return;
                }
                recordAdd.context = this.mContext;
                recordAdd.callback = iPhoneStateListener;
                recordAdd.callingPackage = str;
                recordAdd.callerUid = Binder.getCallingUid();
                recordAdd.callerPid = Binder.getCallingPid();
                if (!SubscriptionManager.isValidSubscriptionId(i2)) {
                    recordAdd.subId = Integer.MAX_VALUE;
                } else {
                    recordAdd.subId = i2;
                }
                recordAdd.phoneId = phoneId;
                recordAdd.events = i;
                if (z && validatePhoneId(phoneId)) {
                    if ((i & 1) != 0) {
                        try {
                            recordAdd.callback.onServiceStateChanged(makeServiceState(this.mServiceState[phoneId]));
                        } catch (RemoteException e) {
                            remove(recordAdd.binder);
                        }
                    }
                    if ((i & 2) != 0) {
                        try {
                            int gsmSignalStrength = this.mSignalStrength[phoneId].getGsmSignalStrength();
                            IPhoneStateListener iPhoneStateListener2 = recordAdd.callback;
                            if (gsmSignalStrength == 99) {
                                gsmSignalStrength = -1;
                            }
                            iPhoneStateListener2.onSignalStrengthChanged(gsmSignalStrength);
                        } catch (RemoteException e2) {
                            remove(recordAdd.binder);
                        }
                    }
                    if ((i & 4) != 0) {
                        try {
                            recordAdd.callback.onMessageWaitingIndicatorChanged(this.mMessageWaiting[phoneId]);
                        } catch (RemoteException e3) {
                            remove(recordAdd.binder);
                        }
                    }
                    if ((i & 8) != 0) {
                        try {
                            recordAdd.callback.onCallForwardingIndicatorChanged(this.mCallForwarding[phoneId]);
                        } catch (RemoteException e4) {
                            remove(recordAdd.binder);
                        }
                    }
                    if (validateEventsAndUserLocked(recordAdd, 16)) {
                        try {
                            if (checkLocationAccess(recordAdd)) {
                                recordAdd.callback.onCellLocationChanged(new Bundle(this.mCellLocation[phoneId]));
                            }
                        } catch (RemoteException e5) {
                            remove(recordAdd.binder);
                        }
                    }
                    if ((i & 32) != 0) {
                        try {
                            recordAdd.callback.onCallStateChanged(this.mCallState[phoneId], getCallIncomingNumber(recordAdd, phoneId));
                        } catch (RemoteException e6) {
                            remove(recordAdd.binder);
                        }
                    }
                    if ((i & 64) != 0) {
                        try {
                            recordAdd.callback.onDataConnectionStateChanged(this.mDataConnectionState[phoneId], this.mDataConnectionNetworkType[phoneId]);
                        } catch (RemoteException e7) {
                            remove(recordAdd.binder);
                        }
                    }
                    if ((i & 128) != 0) {
                        try {
                            recordAdd.callback.onDataActivity(this.mDataActivity[phoneId]);
                        } catch (RemoteException e8) {
                            remove(recordAdd.binder);
                        }
                    }
                    if ((i & 256) != 0) {
                        try {
                            recordAdd.callback.onSignalStrengthsChanged(this.mSignalStrength[phoneId]);
                        } catch (RemoteException e9) {
                            remove(recordAdd.binder);
                        }
                    }
                    if ((i & 512) != 0) {
                        try {
                            recordAdd.callback.onOtaspChanged(this.mOtaspMode);
                        } catch (RemoteException e10) {
                            remove(recordAdd.binder);
                        }
                    }
                    if (validateEventsAndUserLocked(recordAdd, 1024)) {
                        try {
                            if (checkLocationAccess(recordAdd)) {
                                recordAdd.callback.onCellInfoChanged(this.mCellInfo.get(phoneId));
                            }
                        } catch (RemoteException e11) {
                            remove(recordAdd.binder);
                        }
                    }
                    if ((i & 2048) != 0) {
                        try {
                            recordAdd.callback.onPreciseCallStateChanged(this.mPreciseCallState);
                        } catch (RemoteException e12) {
                            remove(recordAdd.binder);
                        }
                    }
                    if ((i & 4096) != 0) {
                        try {
                            PreciseDataConnectionState preciseDataConnectionState = this.mPreciseDataConnectionState[phoneId];
                            recordAdd.callback.onPreciseDataConnectionStateChanged(new PreciseDataConnectionState(preciseDataConnectionState.getDataConnectionState(), preciseDataConnectionState.getDataConnectionNetworkType(), preciseDataConnectionState.getDataConnectionAPNType(), preciseDataConnectionState.getDataConnectionAPN(), preciseDataConnectionState.getDataConnectionChangeReason(), preciseDataConnectionState.getDataConnectionLinkProperties(), preciseDataConnectionState.getDataConnectionFailCause()));
                        } catch (RemoteException e13) {
                            remove(recordAdd.binder);
                        }
                    }
                    if ((65536 & i) != 0) {
                        try {
                            recordAdd.callback.onCarrierNetworkChange(this.mCarrierNetworkChangeState);
                        } catch (RemoteException e14) {
                            remove(recordAdd.binder);
                        }
                    }
                    if ((131072 & i) != 0) {
                        try {
                            recordAdd.callback.onVoiceActivationStateChanged(this.mVoiceActivationState[phoneId]);
                        } catch (RemoteException e15) {
                            remove(recordAdd.binder);
                        }
                    }
                    if ((262144 & i) != 0) {
                        try {
                            recordAdd.callback.onDataActivationStateChanged(this.mDataActivationState[phoneId]);
                        } catch (RemoteException e16) {
                            remove(recordAdd.binder);
                        }
                    }
                    if ((524288 & i) != 0) {
                        try {
                            recordAdd.callback.onUserMobileDataStateChanged(this.mUserMobileDataState[phoneId]);
                        } catch (RemoteException e17) {
                            remove(recordAdd.binder);
                        }
                    }
                    if ((1048576 & i) != 0) {
                        try {
                            recordAdd.callback.onPhysicalChannelConfigurationChanged(this.mPhysicalChannelConfigs.get(phoneId));
                        } catch (RemoteException e18) {
                            remove(recordAdd.binder);
                        }
                    }
                }
                return;
            }
        }
        remove(iPhoneStateListener.asBinder());
    }

    private String getCallIncomingNumber(Record record, int i) {
        return record.canReadCallLog() ? this.mCallIncomingNumber[i] : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    }

    private Record add(IBinder iBinder) {
        synchronized (this.mRecords) {
            int size = this.mRecords.size();
            for (int i = 0; i < size; i++) {
                Record record = this.mRecords.get(i);
                if (iBinder == record.binder) {
                    return record;
                }
            }
            Record record2 = new Record();
            record2.binder = iBinder;
            record2.deathRecipient = new TelephonyRegistryDeathRecipient(iBinder);
            try {
                iBinder.linkToDeath(record2.deathRecipient, 0);
                this.mRecords.add(record2);
                return record2;
            } catch (RemoteException e) {
                return null;
            }
        }
    }

    private void remove(IBinder iBinder) {
        synchronized (this.mRecords) {
            int size = this.mRecords.size();
            for (int i = 0; i < size; i++) {
                Record record = this.mRecords.get(i);
                if (record.binder == iBinder) {
                    if (record.deathRecipient != null) {
                        try {
                            iBinder.unlinkToDeath(record.deathRecipient, 0);
                        } catch (NoSuchElementException e) {
                        }
                    }
                    this.mRecords.remove(i);
                    return;
                }
            }
        }
    }

    public void notifyCallState(int i, String str) {
        String str2;
        if (!checkNotifyPermission("notifyCallState()")) {
            return;
        }
        synchronized (this.mRecords) {
            for (Record record : this.mRecords) {
                if (record.matchPhoneStateListenerEvent(32) && record.subId == Integer.MAX_VALUE) {
                    try {
                        if (!record.canReadCallLog()) {
                            str2 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                        } else {
                            str2 = str;
                        }
                        record.callback.onCallStateChanged(i, str2);
                    } catch (RemoteException e) {
                        this.mRemoveList.add(record.binder);
                    }
                }
            }
            handleRemoveListLocked();
        }
        broadcastCallStateChanged(i, str, -1, -1);
    }

    public void notifyCallStateForPhoneId(int i, int i2, int i3, String str) {
        if (!checkNotifyPermission("notifyCallState()")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(i)) {
                this.mCallState[i] = i3;
                this.mCallIncomingNumber[i] = str;
                for (Record record : this.mRecords) {
                    if (record.matchPhoneStateListenerEvent(32) && record.subId == i2 && record.subId != Integer.MAX_VALUE) {
                        try {
                            record.callback.onCallStateChanged(i3, getCallIncomingNumber(record, i));
                        } catch (RemoteException e) {
                            this.mRemoveList.add(record.binder);
                        }
                    }
                }
                handleRemoveListLocked();
            } else {
                handleRemoveListLocked();
            }
        }
        broadcastCallStateChanged(i3, str, i, i2);
    }

    public void notifyServiceStateForPhoneId(int i, int i2, ServiceState serviceState) {
        if (!checkNotifyPermission("notifyServiceState()")) {
            return;
        }
        synchronized (this.mRecords) {
            this.mLocalLog.log("notifyServiceStateForSubscriber: subId=" + i2 + " phoneId=" + i + " state=" + serviceState);
            if (validatePhoneId(i)) {
                this.mServiceState[i] = serviceState;
                for (Record record : this.mRecords) {
                    if (record.matchPhoneStateListenerEvent(1) && idMatch(record.subId, i2, i)) {
                        try {
                            record.callback.onServiceStateChanged(makeServiceState(serviceState));
                        } catch (RemoteException e) {
                            this.mRemoveList.add(record.binder);
                        }
                    }
                }
            } else {
                log("notifyServiceStateForSubscriber: INVALID phoneId=" + i);
            }
            handleRemoveListLocked();
        }
        broadcastServiceStateChanged(serviceState, i, i2);
    }

    public void notifySimActivationStateChangedForPhoneId(int i, int i2, int i3, int i4) {
        if (!checkNotifyPermission("notifySimActivationState()")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(i)) {
                switch (i3) {
                    case 0:
                        this.mVoiceActivationState[i] = i4;
                        break;
                    case 1:
                        this.mDataActivationState[i] = i4;
                        break;
                    default:
                        return;
                }
                for (Record record : this.mRecords) {
                    if (i3 == 0) {
                        try {
                            if (record.matchPhoneStateListenerEvent(DumpState.DUMP_INTENT_FILTER_VERIFIERS) && idMatch(record.subId, i2, i)) {
                                record.callback.onVoiceActivationStateChanged(i4);
                            }
                            if (i3 != 1 && record.matchPhoneStateListenerEvent(DumpState.DUMP_DOMAIN_PREFERRED) && idMatch(record.subId, i2, i)) {
                                record.callback.onDataActivationStateChanged(i4);
                            }
                        } catch (RemoteException e) {
                            this.mRemoveList.add(record.binder);
                        }
                    } else if (i3 != 1) {
                    }
                }
            } else {
                log("notifySimActivationStateForPhoneId: INVALID phoneId=" + i);
            }
            handleRemoveListLocked();
        }
    }

    public void notifySignalStrengthForPhoneId(int i, int i2, SignalStrength signalStrength) {
        if (!checkNotifyPermission("notifySignalStrength()")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(i)) {
                this.mSignalStrength[i] = signalStrength;
                for (Record record : this.mRecords) {
                    if (record.matchPhoneStateListenerEvent(256) && idMatch(record.subId, i2, i)) {
                        try {
                            record.callback.onSignalStrengthsChanged(makeSignalStrength(i, signalStrength));
                        } catch (RemoteException e) {
                            this.mRemoveList.add(record.binder);
                        }
                        if (!record.matchPhoneStateListenerEvent(2)) {
                        }
                    } else if (!record.matchPhoneStateListenerEvent(2) && idMatch(record.subId, i2, i)) {
                        try {
                            int gsmSignalStrength = signalStrength.getGsmSignalStrength();
                            if (gsmSignalStrength == 99) {
                                gsmSignalStrength = -1;
                            }
                            record.callback.onSignalStrengthChanged(gsmSignalStrength);
                        } catch (RemoteException e2) {
                            this.mRemoveList.add(record.binder);
                        }
                    }
                }
            }
            log("notifySignalStrengthForPhoneId: invalid phoneId=" + i);
            handleRemoveListLocked();
        }
        broadcastSignalStrengthChanged(signalStrength, i, i2);
    }

    public void notifyCarrierNetworkChange(boolean z) {
        enforceNotifyPermissionOrCarrierPrivilege("notifyCarrierNetworkChange()");
        synchronized (this.mRecords) {
            this.mCarrierNetworkChangeState = z;
            for (Record record : this.mRecords) {
                if (record.matchPhoneStateListenerEvent(65536)) {
                    try {
                        record.callback.onCarrierNetworkChange(z);
                    } catch (RemoteException e) {
                        this.mRemoveList.add(record.binder);
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyCellInfo(List<CellInfo> list) {
        notifyCellInfoForSubscriber(Integer.MAX_VALUE, list);
    }

    public void notifyCellInfoForSubscriber(int i, List<CellInfo> list) {
        if (!checkNotifyPermission("notifyCellInfo()")) {
            return;
        }
        int phoneId = SubscriptionManager.getPhoneId(i);
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mCellInfo.set(phoneId, list);
                for (Record record : this.mRecords) {
                    if (validateEventsAndUserLocked(record, 1024) && idMatch(record.subId, i, phoneId) && checkLocationAccess(record)) {
                        try {
                            record.callback.onCellInfoChanged(list);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(record.binder);
                        }
                    }
                }
                handleRemoveListLocked();
            } else {
                handleRemoveListLocked();
            }
        }
    }

    public void notifyPhysicalChannelConfiguration(List<PhysicalChannelConfig> list) {
        notifyPhysicalChannelConfigurationForSubscriber(Integer.MAX_VALUE, list);
    }

    public void notifyPhysicalChannelConfigurationForSubscriber(int i, List<PhysicalChannelConfig> list) {
        if (!checkNotifyPermission("notifyPhysicalChannelConfiguration()")) {
            return;
        }
        synchronized (this.mRecords) {
            int phoneId = SubscriptionManager.getPhoneId(i);
            if (validatePhoneId(phoneId)) {
                this.mPhysicalChannelConfigs.set(phoneId, list);
                for (Record record : this.mRecords) {
                    if (record.matchPhoneStateListenerEvent(DumpState.DUMP_DEXOPT) && idMatch(record.subId, i, phoneId)) {
                        try {
                            record.callback.onPhysicalChannelConfigurationChanged(list);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(record.binder);
                        }
                    }
                }
                handleRemoveListLocked();
            } else {
                handleRemoveListLocked();
            }
        }
    }

    public void notifyMessageWaitingChangedForPhoneId(int i, int i2, boolean z) {
        if (!checkNotifyPermission("notifyMessageWaitingChanged()")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(i)) {
                this.mMessageWaiting[i] = z;
                for (Record record : this.mRecords) {
                    if (record.matchPhoneStateListenerEvent(4) && idMatch(record.subId, i2, i)) {
                        try {
                            record.callback.onMessageWaitingIndicatorChanged(z);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(record.binder);
                        }
                    }
                }
                handleRemoveListLocked();
            } else {
                handleRemoveListLocked();
            }
        }
    }

    public void notifyUserMobileDataStateChangedForPhoneId(int i, int i2, boolean z) {
        if (!checkNotifyPermission("notifyUserMobileDataStateChanged()")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(i)) {
                this.mMessageWaiting[i] = z;
                for (Record record : this.mRecords) {
                    if (record.matchPhoneStateListenerEvent(DumpState.DUMP_FROZEN) && idMatch(record.subId, i2, i)) {
                        try {
                            record.callback.onUserMobileDataStateChanged(z);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(record.binder);
                        }
                    }
                }
                handleRemoveListLocked();
            } else {
                handleRemoveListLocked();
            }
        }
    }

    public void notifyCallForwardingChanged(boolean z) {
        notifyCallForwardingChangedForSubscriber(Integer.MAX_VALUE, z);
    }

    public void notifyCallForwardingChangedForSubscriber(int i, boolean z) {
        if (!checkNotifyPermission("notifyCallForwardingChanged()")) {
            return;
        }
        int phoneId = SubscriptionManager.getPhoneId(i);
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mCallForwarding[phoneId] = z;
                for (Record record : this.mRecords) {
                    if (record.matchPhoneStateListenerEvent(8) && idMatch(record.subId, i, phoneId)) {
                        try {
                            record.callback.onCallForwardingIndicatorChanged(z);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(record.binder);
                        }
                    }
                }
                handleRemoveListLocked();
            } else {
                handleRemoveListLocked();
            }
        }
    }

    public void notifyDataActivity(int i) {
        notifyDataActivityForSubscriber(Integer.MAX_VALUE, i);
    }

    public void notifyDataActivityForSubscriber(int i, int i2) {
        if (!checkNotifyPermission("notifyDataActivity()")) {
            return;
        }
        int phoneId = SubscriptionManager.getPhoneId(i);
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mDataActivity[phoneId] = i2;
                for (Record record : this.mRecords) {
                    if (record.matchPhoneStateListenerEvent(128) && idMatchEx(record.subId, i, record.phoneId, phoneId)) {
                        try {
                            record.callback.onDataActivity(i2);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(record.binder);
                        }
                    }
                }
                handleRemoveListLocked();
            } else {
                handleRemoveListLocked();
            }
        }
    }

    public void notifyDataConnection(int i, boolean z, String str, String str2, String str3, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, int i2, boolean z2) {
        notifyDataConnectionForSubscriber(Integer.MAX_VALUE, i, z, str, str2, str3, linkProperties, networkCapabilities, i2, z2);
    }

    public void notifyDataConnectionForSubscriber(int i, int i2, boolean z, String str, String str2, String str3, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, int i3, boolean z2) {
        String str4;
        if (!checkNotifyPermission("notifyDataConnection()")) {
            return;
        }
        int phoneId = SubscriptionManager.getPhoneId(i);
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                str4 = str3;
                if (BatteryService.HealthServiceWrapper.INSTANCE_VENDOR.equals(str4) && (this.mDataConnectionState[phoneId] != i2 || this.mDataConnectionNetworkType[phoneId] != i3)) {
                    String str5 = "[" + phoneId + "]onDataConnectionStateChanged(" + i2 + ", " + i3 + ")";
                    log(str5);
                    this.mLocalLog.log(str5);
                    for (Record record : this.mRecords) {
                        if (record.matchPhoneStateListenerEvent(64) && idMatchEx(record.subId, i, record.phoneId, phoneId)) {
                            try {
                                record.callback.onDataConnectionStateChanged(i2, i3);
                            } catch (RemoteException e) {
                                this.mRemoveList.add(record.binder);
                            }
                        }
                    }
                    handleRemoveListLocked();
                    this.mDataConnectionState[phoneId] = i2;
                    this.mDataConnectionNetworkType[phoneId] = i3;
                    this.mPreciseDataConnectionState[phoneId] = new PreciseDataConnectionState(i2, i3, str4, str2, str, linkProperties, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    while (r1.hasNext()) {
                    }
                } else {
                    this.mPreciseDataConnectionState[phoneId] = new PreciseDataConnectionState(i2, i3, str4, str2, str, linkProperties, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    for (Record record2 : this.mRecords) {
                        if (record2.matchPhoneStateListenerEvent(4096) && idMatchEx(record2.subId, i, record2.phoneId, phoneId)) {
                            try {
                                record2.callback.onPreciseDataConnectionStateChanged(this.mPreciseDataConnectionState[phoneId]);
                            } catch (RemoteException e2) {
                                this.mRemoveList.add(record2.binder);
                            }
                        }
                    }
                }
            }
            str4 = str3;
            handleRemoveListLocked();
        }
        broadcastDataConnectionStateChanged(i2, z, str, str2, str4, linkProperties, networkCapabilities, z2, i);
        broadcastPreciseDataConnectionStateChanged(i2, i3, str4, str2, str, linkProperties, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, i);
    }

    public void notifyDataConnectionFailed(String str, String str2) {
        notifyDataConnectionFailedForSubscriber(Integer.MAX_VALUE, str, str2);
    }

    public void notifyDataConnectionFailedForSubscriber(int i, String str, String str2) {
        if (!checkNotifyPermission("notifyDataConnectionFailed()")) {
            return;
        }
        synchronized (this.mRecords) {
            int phoneId = SubscriptionManager.getPhoneId(i);
            this.mPreciseDataConnectionState[phoneId] = new PreciseDataConnectionState(-1, 0, str2, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, str, null, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            for (Record record : this.mRecords) {
                if (record.matchPhoneStateListenerEvent(4096) && idMatchEx(record.subId, i, record.phoneId, phoneId)) {
                    try {
                        record.callback.onPreciseDataConnectionStateChanged(this.mPreciseDataConnectionState[phoneId]);
                    } catch (RemoteException e) {
                        this.mRemoveList.add(record.binder);
                    }
                }
            }
            handleRemoveListLocked();
        }
        broadcastDataConnectionFailed(str, str2, i);
        broadcastPreciseDataConnectionStateChanged(-1, 0, str2, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, str, null, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, i);
    }

    public void notifyCellLocation(Bundle bundle) {
        notifyCellLocationForSubscriber(Integer.MAX_VALUE, bundle);
    }

    public void notifyCellLocationForSubscriber(int i, Bundle bundle) {
        log("notifyCellLocationForSubscriber: subId=" + i + " cellLocation=" + bundle);
        if (!checkNotifyPermission("notifyCellLocation()")) {
            return;
        }
        int phoneId = SubscriptionManager.getPhoneId(i);
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mCellLocation[phoneId] = bundle;
                for (Record record : this.mRecords) {
                    if (validateEventsAndUserLocked(record, 16) && idMatch(record.subId, i, phoneId) && checkLocationAccess(record)) {
                        try {
                            record.callback.onCellLocationChanged(new Bundle(bundle));
                        } catch (RemoteException e) {
                            this.mRemoveList.add(record.binder);
                        }
                    }
                }
                handleRemoveListLocked();
            } else {
                handleRemoveListLocked();
            }
        }
    }

    public void notifyOtaspChanged(int i) {
        if (!checkNotifyPermission("notifyOtaspChanged()")) {
            return;
        }
        synchronized (this.mRecords) {
            this.mOtaspMode = i;
            for (Record record : this.mRecords) {
                if (record.matchPhoneStateListenerEvent(512)) {
                    try {
                        record.callback.onOtaspChanged(i);
                    } catch (RemoteException e) {
                        this.mRemoveList.add(record.binder);
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyPreciseCallState(int i, int i2, int i3) {
        if (!checkNotifyPermission("notifyPreciseCallState()")) {
            return;
        }
        synchronized (this.mRecords) {
            this.mRingingCallState = i;
            this.mForegroundCallState = i2;
            this.mBackgroundCallState = i3;
            this.mPreciseCallState = new PreciseCallState(i, i2, i3, -1, -1);
            for (Record record : this.mRecords) {
                if (record.matchPhoneStateListenerEvent(2048)) {
                    try {
                        record.callback.onPreciseCallStateChanged(this.mPreciseCallState);
                    } catch (RemoteException e) {
                        this.mRemoveList.add(record.binder);
                    }
                }
            }
            handleRemoveListLocked();
        }
        broadcastPreciseCallStateChanged(i, i2, i3, -1, -1);
    }

    public void notifyDisconnectCause(int i, int i2) {
        if (!checkNotifyPermission("notifyDisconnectCause()")) {
            return;
        }
        synchronized (this.mRecords) {
            this.mPreciseCallState = new PreciseCallState(this.mRingingCallState, this.mForegroundCallState, this.mBackgroundCallState, i, i2);
            for (Record record : this.mRecords) {
                if (record.matchPhoneStateListenerEvent(2048)) {
                    try {
                        record.callback.onPreciseCallStateChanged(this.mPreciseCallState);
                    } catch (RemoteException e) {
                        this.mRemoveList.add(record.binder);
                    }
                }
            }
            handleRemoveListLocked();
        }
        broadcastPreciseCallStateChanged(this.mRingingCallState, this.mForegroundCallState, this.mBackgroundCallState, i, i2);
    }

    public void notifyPreciseDataConnectionFailed(String str, String str2, String str3, String str4) {
        notifyPreciseDataConnectionFailedForSubscriber(Integer.MAX_VALUE, str, str2, str3, str4);
    }

    public void notifyPreciseDataConnectionFailedForSubscriber(int i, String str, String str2, String str3, String str4) {
        if (!checkNotifyPermission("notifyPreciseDataConnectionFailed()")) {
            return;
        }
        synchronized (this.mRecords) {
            int phoneId = SubscriptionManager.getPhoneId(i);
            this.mPreciseDataConnectionState[phoneId] = new PreciseDataConnectionState(-1, 0, str2, str3, str, null, str4);
            for (Record record : this.mRecords) {
                if (record.matchPhoneStateListenerEvent(4096) && idMatchEx(record.subId, i, record.phoneId, phoneId)) {
                    try {
                        record.callback.onPreciseDataConnectionStateChanged(this.mPreciseDataConnectionState[phoneId]);
                    } catch (RemoteException e) {
                        this.mRemoveList.add(record.binder);
                    }
                }
            }
            handleRemoveListLocked();
        }
        broadcastPreciseDataConnectionStateChanged(-1, 0, str2, str3, str, null, str4, i);
    }

    public void notifyVoLteServiceStateChanged(VoLteServiceState voLteServiceState) {
        if (!checkNotifyPermission("notifyVoLteServiceStateChanged()")) {
            return;
        }
        synchronized (this.mRecords) {
            this.mVoLteServiceState = voLteServiceState;
            for (Record record : this.mRecords) {
                if (record.matchPhoneStateListenerEvent(16384)) {
                    try {
                        record.callback.onVoLteServiceStateChanged(new VoLteServiceState(this.mVoLteServiceState));
                    } catch (RemoteException e) {
                        this.mRemoveList.add(record.binder);
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyOemHookRawEventForSubscriber(int i, byte[] bArr) {
        if (!checkNotifyPermission("notifyOemHookRawEventForSubscriber")) {
            return;
        }
        synchronized (this.mRecords) {
            for (Record record : this.mRecords) {
                if (record.matchPhoneStateListenerEvent(32768) && (record.subId == i || record.subId == Integer.MAX_VALUE)) {
                    try {
                        record.callback.onOemHookRawEvent(bArr);
                    } catch (RemoteException e) {
                        this.mRemoveList.add(record.binder);
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, indentingPrintWriter)) {
            synchronized (this.mRecords) {
                int size = this.mRecords.size();
                indentingPrintWriter.println("last known state:");
                indentingPrintWriter.increaseIndent();
                for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
                    indentingPrintWriter.println("Phone Id=" + i);
                    indentingPrintWriter.increaseIndent();
                    indentingPrintWriter.println("mCallState=" + this.mCallState[i]);
                    indentingPrintWriter.println("mCallIncomingNumber=" + this.mCallIncomingNumber[i]);
                    indentingPrintWriter.println("mServiceState=" + this.mServiceState[i]);
                    indentingPrintWriter.println("mVoiceActivationState= " + this.mVoiceActivationState[i]);
                    indentingPrintWriter.println("mDataActivationState= " + this.mDataActivationState[i]);
                    indentingPrintWriter.println("mUserMobileDataState= " + this.mUserMobileDataState[i]);
                    indentingPrintWriter.println("mSignalStrength=" + this.mSignalStrength[i]);
                    indentingPrintWriter.println("mMessageWaiting=" + this.mMessageWaiting[i]);
                    indentingPrintWriter.println("mCallForwarding=" + this.mCallForwarding[i]);
                    indentingPrintWriter.println("mDataActivity=" + this.mDataActivity[i]);
                    indentingPrintWriter.println("mDataConnectionState=" + this.mDataConnectionState[i]);
                    indentingPrintWriter.println("mCellLocation=" + this.mCellLocation[i]);
                    indentingPrintWriter.println("mCellInfo=" + this.mCellInfo.get(i));
                    indentingPrintWriter.println("mPreciseDataConnectionState=" + this.mPreciseDataConnectionState[i]);
                    indentingPrintWriter.decreaseIndent();
                }
                indentingPrintWriter.println("mPreciseCallState=" + this.mPreciseCallState);
                indentingPrintWriter.println("mCarrierNetworkChangeState=" + this.mCarrierNetworkChangeState);
                indentingPrintWriter.println("mRingingCallState=" + this.mRingingCallState);
                indentingPrintWriter.println("mForegroundCallState=" + this.mForegroundCallState);
                indentingPrintWriter.println("mBackgroundCallState=" + this.mBackgroundCallState);
                indentingPrintWriter.println("mVoLteServiceState=" + this.mVoLteServiceState);
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println("local logs:");
                indentingPrintWriter.increaseIndent();
                this.mLocalLog.dump(fileDescriptor, indentingPrintWriter, strArr);
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println("registrations: count=" + size);
                indentingPrintWriter.increaseIndent();
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    indentingPrintWriter.println(it.next());
                }
                indentingPrintWriter.decreaseIndent();
            }
        }
    }

    private void broadcastServiceStateChanged(ServiceState serviceState, int i, int i2) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mBatteryStats.notePhoneState(serviceState.getState());
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
        Intent intent = new Intent("android.intent.action.SERVICE_STATE");
        intent.addFlags(DumpState.DUMP_SERVICE_PERMISSIONS);
        Bundle bundle = new Bundle();
        serviceState.fillInNotifierBundle(bundle);
        intent.putExtras(bundle);
        intent.putExtra("subscription", i2);
        intent.putExtra("slot", i);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void broadcastSignalStrengthChanged(SignalStrength signalStrength, int i, int i2) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mBatteryStats.notePhoneSignalStrength(signalStrength);
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
        Intent intent = new Intent("android.intent.action.SIG_STR");
        Bundle bundle = new Bundle();
        signalStrength.fillInNotifierBundle(bundle);
        intent.putExtras(bundle);
        intent.putExtra("subscription", i2);
        intent.putExtra("slot", i);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void broadcastCallStateChanged(int i, String str, int i2, int i3) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (i == 0) {
                this.mBatteryStats.notePhoneOff();
            } else {
                this.mBatteryStats.notePhoneOn();
            }
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
        Intent intent = new Intent("android.intent.action.PHONE_STATE");
        intent.putExtra(AudioService.CONNECT_INTENT_KEY_STATE, PhoneConstantConversions.convertCallState(i).toString());
        if (i3 != -1) {
            intent.setAction("android.intent.action.SUBSCRIPTION_PHONE_STATE");
            intent.putExtra("subscription", i3);
        }
        if (i2 != -1) {
            intent.putExtra("slot", i2);
        }
        intent.addFlags(DumpState.DUMP_SERVICE_PERMISSIONS);
        Intent intent2 = new Intent(intent);
        if (!TextUtils.isEmpty(str)) {
            intent2.putExtra("incoming_number", str);
        }
        this.mContext.sendBroadcastAsUser(intent2, UserHandle.ALL, "android.permission.READ_PRIVILEGED_PHONE_STATE");
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.READ_PHONE_STATE", 51);
        this.mContext.sendBroadcastAsUserMultiplePermissions(intent2, UserHandle.ALL, new String[]{"android.permission.READ_PHONE_STATE", "android.permission.READ_CALL_LOG"});
    }

    private void broadcastDataConnectionStateChanged(int i, boolean z, String str, String str2, String str3, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, boolean z2, int i2) {
        Intent intent = new Intent("android.intent.action.ANY_DATA_STATE");
        intent.putExtra(AudioService.CONNECT_INTENT_KEY_STATE, PhoneConstantConversions.convertDataState(i).toString());
        if (!z) {
            intent.putExtra("networkUnvailable", true);
        }
        if (str != null) {
            intent.putExtra(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, str);
        }
        if (linkProperties != null) {
            intent.putExtra("linkProperties", linkProperties);
            String interfaceName = linkProperties.getInterfaceName();
            if (interfaceName != null) {
                intent.putExtra("iface", interfaceName);
            }
        }
        if (networkCapabilities != null) {
            intent.putExtra("networkCapabilities", networkCapabilities);
        }
        if (z2) {
            intent.putExtra("networkRoaming", true);
        }
        intent.putExtra("apn", str2);
        intent.putExtra("apnType", str3);
        intent.putExtra("subscription", i2);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void broadcastDataConnectionFailed(String str, String str2, int i) {
        Intent intent = new Intent("android.intent.action.DATA_CONNECTION_FAILED");
        intent.putExtra(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, str);
        intent.putExtra("apnType", str2);
        intent.putExtra("subscription", i);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void broadcastPreciseCallStateChanged(int i, int i2, int i3, int i4, int i5) {
        Intent intent = new Intent("android.intent.action.PRECISE_CALL_STATE");
        intent.putExtra("ringing_state", i);
        intent.putExtra("foreground_state", i2);
        intent.putExtra("background_state", i3);
        intent.putExtra("disconnect_cause", i4);
        intent.putExtra("precise_disconnect_cause", i5);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.READ_PRECISE_PHONE_STATE");
    }

    private void broadcastPreciseDataConnectionStateChanged(int i, int i2, String str, String str2, String str3, LinkProperties linkProperties, String str4, int i3) {
        Intent intent = new Intent("android.intent.action.PRECISE_DATA_CONNECTION_STATE_CHANGED");
        intent.putExtra(AudioService.CONNECT_INTENT_KEY_STATE, i);
        intent.putExtra("networkType", i2);
        if (str3 != null) {
            intent.putExtra(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, str3);
        }
        if (str != null) {
            intent.putExtra("apnType", str);
        }
        if (str2 != null) {
            intent.putExtra("apn", str2);
        }
        if (linkProperties != null) {
            intent.putExtra("linkProperties", linkProperties);
        }
        if (str4 != null) {
            intent.putExtra("failCause", str4);
        }
        intent.putExtra("subscription", i3);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.READ_PRECISE_PHONE_STATE");
    }

    private void enforceNotifyPermissionOrCarrierPrivilege(String str) {
        if (checkNotifyPermission()) {
            return;
        }
        TelephonyPermissions.enforceCallingOrSelfCarrierPrivilege(SubscriptionManager.getDefaultSubscriptionId(), str);
    }

    private boolean checkNotifyPermission(String str) {
        if (checkNotifyPermission()) {
            return true;
        }
        String str2 = "Modify Phone State Permission Denial: " + str + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
        return false;
    }

    private boolean checkNotifyPermission() {
        return this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") == 0;
    }

    private boolean checkListenerPermission(int i, int i2, String str, String str2) {
        if ((i & ENFORCE_COARSE_LOCATION_PERMISSION_MASK) != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_COARSE_LOCATION", null);
            if (this.mAppOps.noteOp(0, Binder.getCallingUid(), str) != 0) {
                return false;
            }
        }
        if ((i & ENFORCE_PHONE_STATE_PERMISSION_MASK) != 0 && !TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, i2, str, str2)) {
            return false;
        }
        if ((i & PRECISE_PHONE_STATE_PERMISSION_MASK) != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRECISE_PHONE_STATE", null);
        }
        if ((i & 32768) != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", null);
            return true;
        }
        return true;
    }

    private void handleRemoveListLocked() {
        if (this.mRemoveList.size() > 0) {
            Iterator<IBinder> it = this.mRemoveList.iterator();
            while (it.hasNext()) {
                remove(it.next());
            }
            this.mRemoveList.clear();
        }
    }

    private boolean validateEventsAndUserLocked(Record record, int i) {
        boolean z;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (UserHandle.getUserId(record.callerUid) == ActivityManager.getCurrentUser()) {
                z = record.matchPhoneStateListenerEvent(i);
            }
            return z;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean validatePhoneId(int i) {
        return i >= 0 && i < this.mNumPhones;
    }

    private static void log(String str) {
        Rlog.d(TAG, str);
    }

    boolean idMatch(int i, int i2, int i3) {
        return i2 < 0 ? this.mDefaultPhoneId == i3 : i == Integer.MAX_VALUE ? i2 == this.mDefaultSubId : i == i2;
    }

    private boolean checkLocationAccess(Record record) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return LocationAccessPolicy.canAccessCellLocation(this.mContext, record.callingPackage, record.callerUid, record.callerPid, false);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void checkPossibleMissNotify(Record record, int i) {
        int i2 = record.events;
        if ((i2 & 1) != 0) {
            try {
                record.callback.onServiceStateChanged(makeServiceState(this.mServiceState[i]));
            } catch (RemoteException e) {
                this.mRemoveList.add(record.binder);
            }
        }
        if ((i2 & 256) != 0) {
            try {
                record.callback.onSignalStrengthsChanged(makeSignalStrength(i, this.mSignalStrength[i]));
            } catch (RemoteException e2) {
                this.mRemoveList.add(record.binder);
            }
        }
        if ((i2 & 2) != 0) {
            try {
                int gsmSignalStrength = this.mSignalStrength[i].getGsmSignalStrength();
                IPhoneStateListener iPhoneStateListener = record.callback;
                if (gsmSignalStrength == 99) {
                    gsmSignalStrength = -1;
                }
                iPhoneStateListener.onSignalStrengthChanged(gsmSignalStrength);
            } catch (RemoteException e3) {
                this.mRemoveList.add(record.binder);
            }
        }
        if (validateEventsAndUserLocked(record, 1024)) {
            try {
                if (checkLocationAccess(record)) {
                    record.callback.onCellInfoChanged(this.mCellInfo.get(i));
                }
            } catch (RemoteException e4) {
                this.mRemoveList.add(record.binder);
            }
        }
        if ((524288 & i2) != 0) {
            try {
                record.callback.onUserMobileDataStateChanged(this.mUserMobileDataState[i]);
            } catch (RemoteException e5) {
                this.mRemoveList.add(record.binder);
            }
        }
        if ((i2 & 4) != 0) {
            try {
                record.callback.onMessageWaitingIndicatorChanged(this.mMessageWaiting[i]);
            } catch (RemoteException e6) {
                this.mRemoveList.add(record.binder);
            }
        }
        if ((i2 & 8) != 0) {
            try {
                record.callback.onCallForwardingIndicatorChanged(this.mCallForwarding[i]);
            } catch (RemoteException e7) {
                this.mRemoveList.add(record.binder);
            }
        }
        if (validateEventsAndUserLocked(record, 16)) {
            try {
                if (checkLocationAccess(record)) {
                    record.callback.onCellLocationChanged(new Bundle(this.mCellLocation[i]));
                }
            } catch (RemoteException e8) {
                this.mRemoveList.add(record.binder);
            }
        }
        if ((i2 & 64) != 0) {
            try {
                record.callback.onDataConnectionStateChanged(this.mDataConnectionState[i], this.mDataConnectionNetworkType[i]);
            } catch (RemoteException e9) {
                this.mRemoveList.add(record.binder);
            }
        }
    }

    boolean idMatchEx(int i, int i2, int i3, int i4) {
        if (sMethodIdMatchEx != null) {
            try {
                return ((Boolean) sMethodIdMatchEx.invoke(null, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(this.mDefaultSubId), Integer.valueOf(i3), Integer.valueOf(i4))).booleanValue();
            } catch (Exception e) {
                e.printStackTrace();
                log(e.toString());
            }
        } else {
            log("sMethodIdMatchEx is null!");
        }
        return idMatch(i, i2, i4);
    }

    public SignalStrength makeSignalStrength(int i) {
        try {
            Constructor<?> constructor = Class.forName("mediatek.telephony.MtkSignalStrength").getConstructor(Integer.TYPE);
            constructor.setAccessible(true);
            return (SignalStrength) constructor.newInstance(new Integer(i));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Rlog.e(TAG, "MtkSignalStrength IllegalAccessException! Used AOSP instead!");
            return new SignalStrength();
        } catch (InstantiationException e2) {
            e2.printStackTrace();
            Rlog.e(TAG, "MtkSignalStrength InstantiationException! Used AOSP instead!");
            return new SignalStrength();
        } catch (InvocationTargetException e3) {
            e3.printStackTrace();
            Rlog.e(TAG, "MtkSignalStrength InvocationTargetException! Used AOSP instead!");
            return new SignalStrength();
        } catch (Exception e4) {
            e4.printStackTrace();
            Rlog.e(TAG, "No MtkSignalStrength! Used AOSP instead!");
            return new SignalStrength();
        }
    }

    public SignalStrength makeSignalStrength(int i, SignalStrength signalStrength) {
        try {
            Constructor<?> constructor = Class.forName("mediatek.telephony.MtkSignalStrength").getConstructor(Integer.TYPE, SignalStrength.class);
            constructor.setAccessible(true);
            return (SignalStrength) constructor.newInstance(new Integer(i), new SignalStrength(signalStrength));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Rlog.e(TAG, "MtkSignalStrength IllegalAccessException! Used AOSP instead!");
            return new SignalStrength(signalStrength);
        } catch (InstantiationException e2) {
            e2.printStackTrace();
            Rlog.e(TAG, "MtkSignalStrength InstantiationException! Used AOSP instead!");
            return new SignalStrength(signalStrength);
        } catch (InvocationTargetException e3) {
            e3.printStackTrace();
            Rlog.e(TAG, "MtkSignalStrength InvocationTargetException! Used AOSP instead!");
            return new SignalStrength(signalStrength);
        } catch (Exception e4) {
            Rlog.e(TAG, "No MtkSignalStrength! Used AOSP instead!");
            return new SignalStrength(signalStrength);
        }
    }

    public ServiceState makeServiceState() {
        try {
            Constructor<?> constructor = Class.forName("mediatek.telephony.MtkServiceState").getConstructor(new Class[0]);
            constructor.setAccessible(true);
            return (ServiceState) constructor.newInstance(new Object[0]);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Rlog.e(TAG, "MtkServiceState IllegalAccessException! Used AOSP instead!");
            return new ServiceState();
        } catch (InstantiationException e2) {
            e2.printStackTrace();
            Rlog.e(TAG, "MtkServiceState InstantiationException! Used AOSP instead!");
            return new ServiceState();
        } catch (InvocationTargetException e3) {
            e3.printStackTrace();
            Rlog.e(TAG, "MtkServiceState InvocationTargetException! Used AOSP instead!");
            return new ServiceState();
        } catch (Exception e4) {
            Rlog.e(TAG, "No MtkServiceState! Used AOSP instead!");
            return new ServiceState();
        }
    }

    public ServiceState makeServiceState(ServiceState serviceState) {
        try {
            Constructor<?> constructor = Class.forName("mediatek.telephony.MtkServiceState").getConstructor(ServiceState.class);
            constructor.setAccessible(true);
            return (ServiceState) constructor.newInstance(serviceState);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Rlog.e(TAG, "MtkServiceState IllegalAccessException! Used AOSP instead!");
            return new ServiceState(serviceState);
        } catch (InstantiationException e2) {
            e2.printStackTrace();
            Rlog.e(TAG, "MtkServiceState InstantiationException! Used AOSP instead!");
            return new ServiceState(serviceState);
        } catch (InvocationTargetException e3) {
            e3.printStackTrace();
            Rlog.e(TAG, "MtkServiceState InvocationTargetException! Used AOSP instead!");
            return new ServiceState(serviceState);
        } catch (Exception e4) {
            Rlog.e(TAG, "No MtkServiceState! Used AOSP instead!");
            return new ServiceState(serviceState);
        }
    }
}
