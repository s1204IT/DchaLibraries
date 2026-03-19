package com.android.server.emergency;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.server.SystemService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EmergencyAffordanceService extends SystemService {
    private static final int CELL_INFO_STATE_CHANGED = 2;
    private static final String EMERGENCY_SIM_INSERTED_SETTING = "emergency_sim_inserted_before";
    private static final int INITIALIZE_STATE = 1;
    private static final int NUM_SCANS_UNTIL_ABORT = 4;
    private static final int SUBSCRIPTION_CHANGED = 3;
    private static final String TAG = "EmergencyAffordanceService";
    private BroadcastReceiver mAirplaneModeReceiver;
    private final Context mContext;
    private boolean mEmergencyAffordanceNeeded;
    private final ArrayList<Integer> mEmergencyCallMccNumbers;
    private MyHandler mHandler;
    private final Object mLock;
    private boolean mNetworkNeedsEmergencyAffordance;
    private PhoneStateListener mPhoneStateListener;
    private int mScansCompleted;
    private boolean mSimNeedsEmergencyAffordance;
    private SubscriptionManager.OnSubscriptionsChangedListener mSubscriptionChangedListener;
    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;
    private boolean mVoiceCapable;

    private void requestCellScan() {
        this.mHandler.obtainMessage(2).sendToTarget();
    }

    public EmergencyAffordanceService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onCellInfoChanged(List<CellInfo> list) {
                if (!EmergencyAffordanceService.this.isEmergencyAffordanceNeeded()) {
                    EmergencyAffordanceService.this.requestCellScan();
                }
            }

            @Override
            public void onCellLocationChanged(CellLocation cellLocation) {
                if (!EmergencyAffordanceService.this.isEmergencyAffordanceNeeded()) {
                    EmergencyAffordanceService.this.requestCellScan();
                }
            }
        };
        this.mAirplaneModeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (Settings.Global.getInt(context2.getContentResolver(), "airplane_mode_on", 0) == 0) {
                    EmergencyAffordanceService.this.startScanning();
                    EmergencyAffordanceService.this.requestCellScan();
                }
            }
        };
        this.mSubscriptionChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
            @Override
            public void onSubscriptionsChanged() {
                EmergencyAffordanceService.this.mHandler.obtainMessage(3).sendToTarget();
            }
        };
        this.mContext = context;
        int[] intArray = context.getResources().getIntArray(R.array.config_bg_current_drain_threshold_to_restricted_bucket);
        this.mEmergencyCallMccNumbers = new ArrayList<>(intArray.length);
        for (int i : intArray) {
            this.mEmergencyCallMccNumbers.add(Integer.valueOf(i));
        }
    }

    private void updateEmergencyAffordanceNeeded() {
        synchronized (this.mLock) {
            this.mEmergencyAffordanceNeeded = this.mVoiceCapable && (this.mSimNeedsEmergencyAffordance || this.mNetworkNeedsEmergencyAffordance);
            Settings.Global.putInt(this.mContext.getContentResolver(), "emergency_affordance_needed", this.mEmergencyAffordanceNeeded ? 1 : 0);
            if (this.mEmergencyAffordanceNeeded) {
                stopScanning();
            }
        }
    }

    private void stopScanning() {
        synchronized (this.mLock) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
            this.mScansCompleted = 0;
        }
    }

    private boolean isEmergencyAffordanceNeeded() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mEmergencyAffordanceNeeded;
        }
        return z;
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 600) {
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService(TelephonyManager.class);
            this.mVoiceCapable = this.mTelephonyManager.isVoiceCapable();
            if (!this.mVoiceCapable) {
                updateEmergencyAffordanceNeeded();
                return;
            }
            this.mSubscriptionManager = SubscriptionManager.from(this.mContext);
            HandlerThread handlerThread = new HandlerThread(TAG);
            handlerThread.start();
            this.mHandler = new MyHandler(handlerThread.getLooper());
            this.mHandler.obtainMessage(1).sendToTarget();
            startScanning();
            this.mContext.registerReceiver(this.mAirplaneModeReceiver, new IntentFilter("android.intent.action.AIRPLANE_MODE"));
            this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mSubscriptionChangedListener);
        }
    }

    private void startScanning() {
        this.mTelephonyManager.listen(this.mPhoneStateListener, 1040);
    }

    private class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    EmergencyAffordanceService.this.handleInitializeState();
                    break;
                case 2:
                    EmergencyAffordanceService.this.handleUpdateCellInfo();
                    break;
                case 3:
                    EmergencyAffordanceService.this.handleUpdateSimSubscriptionInfo();
                    break;
            }
        }
    }

    private void handleInitializeState() {
        if (handleUpdateSimSubscriptionInfo() || handleUpdateCellInfo()) {
            return;
        }
        updateEmergencyAffordanceNeeded();
    }

    private boolean handleUpdateSimSubscriptionInfo() {
        boolean z;
        int i;
        boolean zSimNeededAffordanceBefore = simNeededAffordanceBefore();
        List<SubscriptionInfo> activeSubscriptionInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList == null) {
            setSimNeedsEmergencyAffordance(zSimNeededAffordanceBefore);
            return zSimNeededAffordanceBefore;
        }
        Iterator<SubscriptionInfo> it = activeSubscriptionInfoList.iterator();
        while (true) {
            z = true;
            if (it.hasNext()) {
                SubscriptionInfo next = it.next();
                int mcc = next.getMcc();
                if (mccRequiresEmergencyAffordance(mcc)) {
                    break;
                }
                if (mcc != 0 && mcc != Integer.MAX_VALUE) {
                    zSimNeededAffordanceBefore = false;
                }
                String simOperator = this.mTelephonyManager.getSimOperator(next.getSubscriptionId());
                if (simOperator != null && simOperator.length() >= 3) {
                    i = Integer.parseInt(simOperator.substring(0, 3));
                } else {
                    i = 0;
                }
                if (i != 0) {
                    if (mccRequiresEmergencyAffordance(i)) {
                        break;
                    }
                    zSimNeededAffordanceBefore = false;
                }
            } else {
                z = zSimNeededAffordanceBefore;
                break;
            }
        }
        setSimNeedsEmergencyAffordance(z);
        return z;
    }

    private void setSimNeedsEmergencyAffordance(boolean z) {
        if (simNeededAffordanceBefore() != z) {
            Settings.Global.putInt(this.mContext.getContentResolver(), EMERGENCY_SIM_INSERTED_SETTING, z ? 1 : 0);
        }
        if (z != this.mSimNeedsEmergencyAffordance) {
            this.mSimNeedsEmergencyAffordance = z;
            updateEmergencyAffordanceNeeded();
        }
    }

    private boolean simNeededAffordanceBefore() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), EMERGENCY_SIM_INSERTED_SETTING, 0) != 0;
    }

    private boolean handleUpdateCellInfo() {
        int mcc;
        List<CellInfo> allCellInfo = this.mTelephonyManager.getAllCellInfo();
        if (allCellInfo == null) {
            return false;
        }
        boolean z = false;
        for (CellInfo cellInfo : allCellInfo) {
            if (cellInfo instanceof CellInfoGsm) {
                mcc = ((CellInfoGsm) cellInfo).getCellIdentity().getMcc();
            } else if (cellInfo instanceof CellInfoLte) {
                mcc = ((CellInfoLte) cellInfo).getCellIdentity().getMcc();
            } else if (cellInfo instanceof CellInfoWcdma) {
                mcc = ((CellInfoWcdma) cellInfo).getCellIdentity().getMcc();
            } else {
                mcc = 0;
            }
            if (mccRequiresEmergencyAffordance(mcc)) {
                setNetworkNeedsEmergencyAffordance(true);
                return true;
            }
            if (mcc != 0 && mcc != Integer.MAX_VALUE) {
                z = true;
            }
        }
        if (z) {
            stopScanning();
        } else {
            onCellScanFinishedUnsuccessful();
        }
        setNetworkNeedsEmergencyAffordance(false);
        return false;
    }

    private void setNetworkNeedsEmergencyAffordance(boolean z) {
        synchronized (this.mLock) {
            this.mNetworkNeedsEmergencyAffordance = z;
            updateEmergencyAffordanceNeeded();
        }
    }

    private void onCellScanFinishedUnsuccessful() {
        synchronized (this.mLock) {
            this.mScansCompleted++;
            if (this.mScansCompleted >= 4) {
                stopScanning();
            }
        }
    }

    private boolean mccRequiresEmergencyAffordance(int i) {
        return this.mEmergencyCallMccNumbers.contains(Integer.valueOf(i));
    }
}
