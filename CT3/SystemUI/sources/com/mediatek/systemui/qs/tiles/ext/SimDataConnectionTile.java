package com.mediatek.systemui.qs.tiles.ext;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.PhoneConstants;
import com.android.systemui.qs.QSTile;
import com.mediatek.systemui.PluginManager;
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;
import com.mediatek.systemui.statusbar.util.SIMHelper;
import java.util.List;

public class SimDataConnectionTile extends QSTile<QSTile.BooleanState> {
    private static final String TAG = "SimDataConnectionTile";
    private boolean mListening;
    private IconIdWrapper[] mSimConnectionIconWrapperArray;
    private SimDataSwitchStateMachine mSimDataSwitchStateMachine;
    private CharSequence mTileLabel;

    public SimDataConnectionTile(QSTile.Host host) {
        super(host);
        this.mSimConnectionIconWrapperArray = new IconIdWrapper[SIMConnState.valuesCustom().length];
        init();
    }

    @Override
    public CharSequence getTileLabel() {
        this.mTileLabel = PluginManager.getQuickSettingsPlugin(this.mContext).getTileLabel("simdataconnection");
        return this.mTileLabel;
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    private void init() {
        for (int i = 0; i < this.mSimConnectionIconWrapperArray.length; i++) {
            this.mSimConnectionIconWrapperArray[i] = new IconIdWrapper();
        }
        this.mSimDataSwitchStateMachine = new SimDataSwitchStateMachine();
    }

    @Override
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    protected void handleLongClick() {
        handleClick();
    }

    @Override
    protected void handleClick() {
        if (this.mSimDataSwitchStateMachine.isClickable()) {
            this.mSimDataSwitchStateMachine.toggleState(this.mContext);
        }
        refreshState();
    }

    @Override
    public void setListening(boolean listening) {
        if (this.mListening == listening) {
            return;
        }
        this.mListening = listening;
        if (listening) {
            this.mSimDataSwitchStateMachine.registerReceiver();
        } else {
            this.mSimDataSwitchStateMachine.unregisterReceiver();
        }
    }

    @Override
    public int getMetricsCategory() {
        return 111;
    }

    @Override
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        int simConnState = this.mSimDataSwitchStateMachine.getCurrentSimConnState().ordinal();
        IQuickSettingsPlugin quickSettingsPlugin = PluginManager.getQuickSettingsPlugin(this.mContext);
        quickSettingsPlugin.customizeSimDataConnectionTile(simConnState, this.mSimConnectionIconWrapperArray[simConnState]);
        state.icon = QsIconWrapper.get(this.mSimConnectionIconWrapperArray[simConnState].getIconId(), this.mSimConnectionIconWrapperArray[simConnState]);
        state.label = quickSettingsPlugin.getTileLabel("simdataconnection");
    }

    public enum SIMConnState {
        SIM1_E_D,
        SIM1_E_E,
        SIM1_D_D,
        SIM1_D_E,
        SIM2_E_D,
        SIM2_E_E,
        SIM2_D_D,
        SIM2_D_E,
        NO_SIM,
        SIM1_E_F,
        SIM1_D_F,
        SIM2_E_F,
        SIM2_D_F;

        public static SIMConnState[] valuesCustom() {
            return values();
        }
    }

    private class SimDataSwitchStateMachine {

        private static final int[] f10xe858a61c = null;
        private static final int EVENT_SWITCH_TIME_OUT = 2000;
        private static final int SWITCH_TIME_OUT_LENGTH = 30000;
        private static final String TRANSACTION_START = "com.android.mms.transaction.START";
        private static final String TRANSACTION_STOP = "com.android.mms.transaction.STOP";
        final int[] $SWITCH_TABLE$com$mediatek$systemui$qs$tiles$ext$SimDataConnectionTile$SIMConnState;
        private boolean mIsAirlineMode;
        protected boolean mIsUserSwitching;
        boolean mMmsOngoing;
        private PhoneStateListener[] mPhoneStateListener;
        boolean mSimConnStateTrackerReady;
        private int mSlotCount;
        TelephonyManager mTelephonyManager;
        private SIMConnState mCurrentSimConnState = SIMConnState.NO_SIM;
        private Handler mDataTimerHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int simFrom = msg.arg1;
                int simTo = msg.arg2;
                switch (msg.what) {
                    case SimDataSwitchStateMachine.EVENT_SWITCH_TIME_OUT:
                        Log.d(SimDataConnectionTile.TAG, "switching time out..... switch from " + simFrom + " to " + simTo);
                        if (!SimDataConnectionTile.this.isWifiOnlyDevice()) {
                            SimDataSwitchStateMachine.this.refresh();
                        }
                        break;
                }
            }
        };
        private BroadcastReceiver mSimStateIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int i = 0;
                String action = intent.getAction();
                Log.d(SimDataConnectionTile.TAG, "onReceive action is " + action);
                if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                    SimDataSwitchStateMachine.this.updateSimConnTile();
                    return;
                }
                if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                    boolean enabled = intent.getBooleanExtra("state", false);
                    Log.d(SimDataConnectionTile.TAG, "airline mode changed: state is " + enabled);
                    if (SimDataSwitchStateMachine.this.mSimConnStateTrackerReady) {
                        SimDataSwitchStateMachine.this.setAirplaneMode(enabled);
                    }
                    SimDataSwitchStateMachine.this.updateSimConnTile();
                    return;
                }
                if (action.equals("android.intent.action.ANY_DATA_STATE")) {
                    PhoneConstants.DataState state = SimDataSwitchStateMachine.this.getMobileDataState(intent);
                    boolean isApnTypeChange = false;
                    String types = intent.getStringExtra("apnType");
                    if (types != null) {
                        String[] typeArray = types.split(",");
                        int length = typeArray.length;
                        while (true) {
                            if (i >= length) {
                                break;
                            }
                            String type = typeArray[i];
                            if (!"default".equals(type)) {
                                i++;
                            } else {
                                isApnTypeChange = true;
                                break;
                            }
                        }
                    }
                    if (!isApnTypeChange) {
                        return;
                    }
                    if ((state != PhoneConstants.DataState.CONNECTED && state != PhoneConstants.DataState.DISCONNECTED) || SimDataSwitchStateMachine.this.isMmsOngoing()) {
                        return;
                    }
                    SimDataSwitchStateMachine.this.updateSimConnTile();
                    return;
                }
                if (action.equals("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE")) {
                    SimDataSwitchStateMachine.this.updateSimConnTile();
                    return;
                }
                if (action.equals("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED")) {
                    SimDataSwitchStateMachine.this.unRegisterPhoneStateListener();
                    SimDataSwitchStateMachine.this.updateSimConnTile();
                    SimDataSwitchStateMachine.this.registerPhoneStateListener();
                } else {
                    if (action.equals(SimDataSwitchStateMachine.TRANSACTION_START)) {
                        if (SimDataConnectionTile.this.isWifiOnlyDevice() || !SimDataSwitchStateMachine.this.mSimConnStateTrackerReady) {
                            return;
                        }
                        SimDataSwitchStateMachine.this.setIsMmsOnging(true);
                        SimDataSwitchStateMachine.this.updateSimConnTile();
                        return;
                    }
                    if (!action.equals(SimDataSwitchStateMachine.TRANSACTION_STOP) || SimDataConnectionTile.this.isWifiOnlyDevice() || !SimDataSwitchStateMachine.this.mSimConnStateTrackerReady) {
                        return;
                    }
                    SimDataSwitchStateMachine.this.setIsMmsOnging(false);
                    SimDataSwitchStateMachine.this.updateSimConnTile();
                }
            }
        };

        private static int[] m2071xe212dcc0() {
            if (f10xe858a61c != null) {
                return f10xe858a61c;
            }
            int[] iArr = new int[SIMConnState.valuesCustom().length];
            try {
                iArr[SIMConnState.NO_SIM.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[SIMConnState.SIM1_D_D.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[SIMConnState.SIM1_D_E.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[SIMConnState.SIM1_D_F.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                iArr[SIMConnState.SIM1_E_D.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                iArr[SIMConnState.SIM1_E_E.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                iArr[SIMConnState.SIM1_E_F.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                iArr[SIMConnState.SIM2_D_D.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                iArr[SIMConnState.SIM2_D_E.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                iArr[SIMConnState.SIM2_D_F.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                iArr[SIMConnState.SIM2_E_D.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                iArr[SIMConnState.SIM2_E_E.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
            try {
                iArr[SIMConnState.SIM2_E_F.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
            f10xe858a61c = iArr;
            return iArr;
        }

        public SIMConnState getCurrentSimConnState() {
            return this.mCurrentSimConnState;
        }

        public SimDataSwitchStateMachine() {
            this.mSlotCount = 0;
            this.mTelephonyManager = (TelephonyManager) SimDataConnectionTile.this.mContext.getSystemService("phone");
            this.mSlotCount = SIMHelper.getSlotCount();
            this.mPhoneStateListener = new PhoneStateListener[this.mSlotCount];
        }

        public void registerReceiver() {
            IntentFilter simIntentFilter = new IntentFilter();
            simIntentFilter.addAction("android.intent.action.AIRPLANE_MODE");
            simIntentFilter.addAction(TRANSACTION_START);
            simIntentFilter.addAction(TRANSACTION_STOP);
            simIntentFilter.addAction("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE");
            simIntentFilter.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
            simIntentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
            simIntentFilter.addAction("android.intent.action.ANY_DATA_STATE");
            SimDataConnectionTile.this.mContext.registerReceiver(this.mSimStateIntentReceiver, simIntentFilter);
        }

        public void unregisterReceiver() {
            SimDataConnectionTile.this.mContext.unregisterReceiver(this.mSimStateIntentReceiver);
        }

        private void addConnTile() {
            this.mSimConnStateTrackerReady = true;
        }

        public void updateSimConnTile() {
            onActualStateChange(SimDataConnectionTile.this.mContext, null);
            SimDataConnectionTile.this.refreshState();
        }

        public void refresh() {
            onActualStateChange(SimDataConnectionTile.this.mContext, null);
            setUserSwitching(false);
        }

        public void onActualStateChange(Context context, Intent intent) {
            List<SubscriptionInfo> infos = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
            boolean sim1Enable = isSimEnable(infos, 0);
            boolean sim2Enable = isSimEnable(infos, 1);
            boolean sim1Conn = false;
            boolean sim2Conn = false;
            int dataConnectionId = SubscriptionManager.getDefaultDataSubscriptionId();
            if (SubscriptionManager.getSlotId(dataConnectionId) == 0) {
                sim1Conn = true;
                sim2Conn = false;
            } else if (SubscriptionManager.getSlotId(dataConnectionId) == 1) {
                sim1Conn = false;
                sim2Conn = true;
            }
            Log.d(SimDataConnectionTile.TAG, "SimConnStateTracker onActualStateChange sim1Enable = " + sim1Enable + ", sim2Enable = " + sim2Enable);
            if (sim1Enable || sim2Enable) {
                boolean dataConnected = isDataConnected();
                Log.d(SimDataConnectionTile.TAG, "onActualStateChange, dataConnected = " + dataConnected + ", sim1Enable = " + sim1Enable + ", sim2Enable = " + sim2Enable + ", sim1Conn = " + sim1Conn + ", sim2Conn = " + sim2Conn);
                if (dataConnected) {
                    if (sim1Enable && sim2Enable) {
                        if (sim1Conn) {
                            this.mCurrentSimConnState = SIMConnState.SIM1_E_E;
                        } else {
                            this.mCurrentSimConnState = SIMConnState.SIM2_E_E;
                        }
                    } else if (sim1Enable || !sim2Enable) {
                        if (sim1Enable && !sim2Enable) {
                            if (isSimInsertedWithUnAvaliable(infos, 1) && sim2Conn) {
                                this.mCurrentSimConnState = SIMConnState.SIM2_E_F;
                            } else {
                                this.mCurrentSimConnState = SIMConnState.SIM1_D_E;
                            }
                        }
                    } else if (isSimInsertedWithUnAvaliable(infos, 0) && sim1Conn) {
                        this.mCurrentSimConnState = SIMConnState.SIM1_E_F;
                    } else {
                        this.mCurrentSimConnState = SIMConnState.SIM2_D_E;
                    }
                } else if (sim1Enable && sim2Enable) {
                    if (sim1Conn) {
                        this.mCurrentSimConnState = SIMConnState.SIM1_E_D;
                    } else {
                        this.mCurrentSimConnState = SIMConnState.SIM2_E_D;
                    }
                } else if (sim1Enable || !sim2Enable) {
                    if (sim1Enable && !sim2Enable) {
                        if (isSimInsertedWithUnAvaliable(infos, 1) && sim2Conn) {
                            this.mCurrentSimConnState = SIMConnState.SIM2_E_F;
                        } else {
                            this.mCurrentSimConnState = SIMConnState.SIM1_D_D;
                        }
                    }
                } else if (isSimInsertedWithUnAvaliable(infos, 0) && sim1Conn) {
                    this.mCurrentSimConnState = SIMConnState.SIM1_E_F;
                } else {
                    this.mCurrentSimConnState = SIMConnState.SIM2_D_D;
                }
            } else if (isSimInsertedWithUnAvaliable(infos, 0) && sim1Conn) {
                this.mCurrentSimConnState = SIMConnState.SIM1_D_F;
            } else if (isSimInsertedWithUnAvaliable(infos, 1) && sim2Conn) {
                this.mCurrentSimConnState = SIMConnState.SIM2_D_F;
            } else {
                this.mCurrentSimConnState = SIMConnState.NO_SIM;
            }
            setUserSwitching(false);
        }

        private boolean isSimEnable(List<SubscriptionInfo> infos, int slotId) {
            return SimDataConnectionTile.this.isSimInsertedBySlot(infos, slotId) && !isAirplaneMode() && isRadioOn(slotId) && !isSimLocked(slotId);
        }

        private boolean isSimInsertedWithUnAvaliable(List<SubscriptionInfo> infos, int slotId) {
            if (!SimDataConnectionTile.this.isSimInsertedBySlot(infos, slotId)) {
                return false;
            }
            if (!isRadioOn(slotId) || isAirplaneMode()) {
                return true;
            }
            return isSimLocked(slotId);
        }

        private boolean isRadioOn(int slotId) {
            int subId1 = SIMHelper.getFirstSubInSlot(slotId);
            return SIMHelper.isRadioOn(subId1);
        }

        private boolean isSimLocked(int slotId) {
            int simState = TelephonyManager.getDefault().getSimState(slotId);
            boolean bSimLocked = simState == 2 || simState == 3 || simState == 4;
            Log.d(SimDataConnectionTile.TAG, "isSimLocked, slotId=" + slotId + " simState=" + simState + " bSimLocked= " + bSimLocked);
            return bSimLocked;
        }

        public void toggleState(Context context) {
            enterNextState(this.mCurrentSimConnState);
        }

        private void enterNextState(SIMConnState state) {
            Log.d(SimDataConnectionTile.TAG, "enterNextState state is " + state);
            switch (m2071xe212dcc0()[state.ordinal()]) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 8:
                case 9:
                case 10:
                    Log.d(SimDataConnectionTile.TAG, "No Sim or one Sim do nothing!");
                    break;
                case 5:
                    Log.d(SimDataConnectionTile.TAG, "Try to switch from Sim1 to Sim2! mSimCurrentCurrentState=" + this.mCurrentSimConnState);
                    this.mCurrentSimConnState = SIMConnState.SIM2_E_D;
                    switchDataDefaultSIM(1);
                    break;
                case 6:
                    Log.d(SimDataConnectionTile.TAG, "Try to switch from Sim1 to Sim2! mSimCurrentCurrentState=" + this.mCurrentSimConnState);
                    this.mCurrentSimConnState = SIMConnState.SIM2_E_E;
                    switchDataDefaultSIM(1);
                    break;
                case 7:
                    Log.d(SimDataConnectionTile.TAG, "Try to switch from Sim1 to Sim2! mSimCurrentCurrentState=" + this.mCurrentSimConnState);
                    switchDataDefaultSIM(1);
                    break;
                case 11:
                    Log.d(SimDataConnectionTile.TAG, "Try to switch from Sim2 to Sim1! mSimCurrentCurrentState=" + this.mCurrentSimConnState);
                    this.mCurrentSimConnState = SIMConnState.SIM1_E_D;
                    switchDataDefaultSIM(0);
                    break;
                case 12:
                    Log.d(SimDataConnectionTile.TAG, "Try to switch from Sim2 to Sim1! mSimCurrentCurrentState=" + this.mCurrentSimConnState);
                    this.mCurrentSimConnState = SIMConnState.SIM1_E_E;
                    switchDataDefaultSIM(0);
                    break;
                case 13:
                    Log.d(SimDataConnectionTile.TAG, "Try to switch from Sim2 to Sim1! mSimCurrentCurrentState=" + this.mCurrentSimConnState);
                    switchDataDefaultSIM(0);
                    break;
            }
        }

        private void switchDataDefaultSIM(int slotId) {
            if (SimDataConnectionTile.this.isWifiOnlyDevice()) {
                return;
            }
            setUserSwitching(true);
            handleDataConnectionChange(slotId);
        }

        private void handleDataConnectionChange(int newSlot) {
            Log.d(SimDataConnectionTile.TAG, "handleDataConnectionChange, newSlot=" + newSlot);
            if (SubscriptionManager.getSlotId(SubscriptionManager.getDefaultDataSubscriptionId()) == newSlot) {
                return;
            }
            this.mDataTimerHandler.sendEmptyMessageDelayed(EVENT_SWITCH_TIME_OUT, 30000L);
            List<SubscriptionInfo> si = SubscriptionManager.from(SimDataConnectionTile.this.mContext).getActiveSubscriptionInfoList();
            if (si == null || si.size() <= 0) {
                return;
            }
            boolean dataEnabled = this.mTelephonyManager.getDataEnabled();
            for (int i = 0; i < si.size(); i++) {
                SubscriptionInfo subInfo = si.get(i);
                int subId = subInfo.getSubscriptionId();
                if (newSlot == subInfo.getSimSlotIndex()) {
                    Log.d(SimDataConnectionTile.TAG, "handleDataConnectionChange. newSlot = " + newSlot + " subId = " + subId);
                    SubscriptionManager.from(SimDataConnectionTile.this.mContext).setDefaultDataSubId(subId);
                    if (dataEnabled) {
                        this.mTelephonyManager.setDataEnabled(subId, true);
                    }
                } else if (dataEnabled) {
                    this.mTelephonyManager.setDataEnabled(subId, false);
                }
            }
        }

        public boolean isClickable() {
            List<SubscriptionInfo> infos = SubscriptionManager.from(SimDataConnectionTile.this.mContext).getActiveSubscriptionInfoList();
            if (SimDataConnectionTile.this.isSimInsertedBySlot(infos, 0) || SimDataConnectionTile.this.isSimInsertedBySlot(infos, 1)) {
                return ((!isRadioOn(0) && !isRadioOn(1)) || isAirplaneMode() || isMmsOngoing() || isUserSwitching()) ? false : true;
            }
            return false;
        }

        private boolean isDataConnected() {
            return TelephonyManager.getDefault().getDataState() == 2;
        }

        public void setIsMmsOnging(boolean ongoing) {
            this.mMmsOngoing = ongoing;
        }

        public boolean isMmsOngoing() {
            return this.mMmsOngoing;
        }

        public void setAirplaneMode(boolean airplaneMode) {
            this.mIsAirlineMode = airplaneMode;
        }

        private boolean isAirplaneMode() {
            return this.mIsAirlineMode;
        }

        private void setUserSwitching(boolean userSwitching) {
            this.mIsUserSwitching = userSwitching;
        }

        private boolean isUserSwitching() {
            return this.mIsUserSwitching;
        }

        public PhoneConstants.DataState getMobileDataState(Intent intent) {
            String str = intent.getStringExtra("state");
            if (str != null) {
                return Enum.valueOf(PhoneConstants.DataState.class, str);
            }
            return PhoneConstants.DataState.DISCONNECTED;
        }

        public void registerPhoneStateListener() {
            for (int i = 0; i < this.mSlotCount; i++) {
                int subId = SIMHelper.getFirstSubInSlot(i);
                if (subId >= 0) {
                    this.mPhoneStateListener[i] = getPhoneStateListener(subId, i);
                    this.mTelephonyManager.listen(this.mPhoneStateListener[i], 1);
                } else {
                    this.mPhoneStateListener[i] = null;
                }
            }
        }

        private PhoneStateListener getPhoneStateListener(int subId, final int slotId) {
            return new PhoneStateListener(subId) {
                @Override
                public void onServiceStateChanged(ServiceState state) {
                    Log.d(SimDataConnectionTile.TAG, "PhoneStateListener:onServiceStateChanged, slot " + slotId + " servicestate = " + state);
                    SimDataSwitchStateMachine.this.updateSimConnTile();
                }
            };
        }

        public void unRegisterPhoneStateListener() {
            for (int i = 0; i < this.mSlotCount; i++) {
                if (this.mPhoneStateListener[i] != null) {
                    this.mTelephonyManager.listen(this.mPhoneStateListener[i], 0);
                }
            }
        }
    }

    public boolean isWifiOnlyDevice() {
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        return !cm.isNetworkSupported(0);
    }

    public boolean isSimInsertedBySlot(List<SubscriptionInfo> infos, int slotId) {
        if (slotId >= SIMHelper.getSlotCount()) {
            return false;
        }
        if (infos != null && infos.size() > 0) {
            for (SubscriptionInfo info : infos) {
                if (info.getSimSlotIndex() == slotId) {
                    return true;
                }
            }
            return false;
        }
        Log.d(TAG, "isSimInsertedBySlot, SubscriptionInfo is null");
        return false;
    }
}
