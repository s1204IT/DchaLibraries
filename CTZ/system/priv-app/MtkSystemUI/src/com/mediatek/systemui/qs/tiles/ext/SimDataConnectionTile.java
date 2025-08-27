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
import com.android.systemui.plugins.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import com.mediatek.systemui.ext.OpSystemUICustomizationFactoryBase;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;
import com.mediatek.systemui.statusbar.util.SIMHelper;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class SimDataConnectionTile extends QSTileImpl<QSTile.BooleanState> {
    private boolean mListening;
    private IconIdWrapper[] mSimConnectionIconWrapperArray;
    private SimDataSwitchStateMachine mSimDataSwitchStateMachine;
    private CharSequence mTileLabel;

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
        SIM2_D_F
    }

    public SimDataConnectionTile(QSHost qSHost) {
        super(qSHost);
        this.mSimConnectionIconWrapperArray = new IconIdWrapper[SIMConnState.values().length];
        init();
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public CharSequence getTileLabel() {
        this.mTileLabel = OpSystemUICustomizationFactoryBase.getOpFactory(this.mContext).makeQuickSettings(this.mContext).getTileLabel("simdataconnection");
        return this.mTileLabel;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public Intent getLongClickIntent() {
        return null;
    }

    private void init() {
        for (int i = 0; i < this.mSimConnectionIconWrapperArray.length; i++) {
            this.mSimConnectionIconWrapperArray[i] = new IconIdWrapper();
        }
        this.mSimDataSwitchStateMachine = new SimDataSwitchStateMachine();
    }

    /* JADX DEBUG: Method merged with bridge method: newTileState()Lcom/android/systemui/plugins/qs/QSTile$State; */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleLongClick() {
        handleClick();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleClick() {
        if (this.mSimDataSwitchStateMachine.isClickable()) {
            this.mSimDataSwitchStateMachine.toggleState(this.mContext);
        }
        refreshState();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleSetListening(boolean z) {
        if (this.mListening == z) {
            return;
        }
        this.mListening = z;
        if (z) {
            this.mSimDataSwitchStateMachine.registerReceiver();
        } else {
            this.mSimDataSwitchStateMachine.unregisterReceiver();
        }
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public int getMetricsCategory() {
        return R.styleable.AppCompatTheme_windowActionBar;
    }

    /* JADX DEBUG: Method merged with bridge method: handleUpdateState(Lcom/android/systemui/plugins/qs/QSTile$State;Ljava/lang/Object;)V */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleUpdateState(QSTile.BooleanState booleanState, Object obj) {
        int iOrdinal = this.mSimDataSwitchStateMachine.getCurrentSimConnState().ordinal();
        IQuickSettingsPlugin iQuickSettingsPluginMakeQuickSettings = OpSystemUICustomizationFactoryBase.getOpFactory(this.mContext).makeQuickSettings(this.mContext);
        iQuickSettingsPluginMakeQuickSettings.customizeSimDataConnectionTile(iOrdinal, this.mSimConnectionIconWrapperArray[iOrdinal]);
        booleanState.icon = QsIconWrapper.get(this.mSimConnectionIconWrapperArray[iOrdinal].getIconId(), this.mSimConnectionIconWrapperArray[iOrdinal]);
        booleanState.label = iQuickSettingsPluginMakeQuickSettings.getTileLabel("simdataconnection");
    }

    private class SimDataSwitchStateMachine {
        private boolean mIsAirlineMode;
        protected boolean mIsUserSwitching;
        boolean mMmsOngoing;
        private PhoneStateListener[] mPhoneStateListener;
        boolean mSimConnStateTrackerReady;
        private int mSlotCount;
        TelephonyManager mTelephonyManager;
        private SIMConnState mCurrentSimConnState = SIMConnState.NO_SIM;
        private Handler mDataTimerHandler = new Handler() { // from class: com.mediatek.systemui.qs.tiles.ext.SimDataConnectionTile.SimDataSwitchStateMachine.1
            @Override // android.os.Handler
            public void handleMessage(Message message) {
                int i = message.arg1;
                int i2 = message.arg2;
                if (message.what == 2000) {
                    Log.d("SimDataConnectionTile", "switching time out..... switch from " + i + " to " + i2);
                    if (!SimDataConnectionTile.this.isWifiOnlyDevice()) {
                        SimDataSwitchStateMachine.this.refresh();
                    }
                }
            }
        };
        private BroadcastReceiver mSimStateIntentReceiver = new BroadcastReceiver() { // from class: com.mediatek.systemui.qs.tiles.ext.SimDataConnectionTile.SimDataSwitchStateMachine.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d("SimDataConnectionTile", "onReceive action is " + action);
                if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                    SimDataSwitchStateMachine.this.updateSimConnTile();
                    return;
                }
                boolean z = false;
                if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                    boolean booleanExtra = intent.getBooleanExtra("state", false);
                    Log.d("SimDataConnectionTile", "airline mode changed: state is " + booleanExtra);
                    if (SimDataSwitchStateMachine.this.mSimConnStateTrackerReady) {
                        SimDataSwitchStateMachine.this.setAirplaneMode(booleanExtra);
                    }
                    SimDataSwitchStateMachine.this.updateSimConnTile();
                    return;
                }
                if (action.equals("android.intent.action.ANY_DATA_STATE")) {
                    PhoneConstants.DataState mobileDataState = SimDataSwitchStateMachine.this.getMobileDataState(intent);
                    String stringExtra = intent.getStringExtra("apnType");
                    if (stringExtra != null) {
                        String[] strArrSplit = stringExtra.split(",");
                        int length = strArrSplit.length;
                        int i = 0;
                        while (true) {
                            if (i >= length) {
                                break;
                            }
                            if (!"default".equals(strArrSplit[i])) {
                                i++;
                            } else {
                                z = true;
                                break;
                            }
                        }
                    }
                    if (z) {
                        if ((mobileDataState == PhoneConstants.DataState.CONNECTED || mobileDataState == PhoneConstants.DataState.DISCONNECTED) && !SimDataSwitchStateMachine.this.isMmsOngoing()) {
                            SimDataSwitchStateMachine.this.updateSimConnTile();
                            return;
                        }
                        return;
                    }
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
                    if (action.equals("com.android.mms.transaction.START")) {
                        if (!SimDataConnectionTile.this.isWifiOnlyDevice() && SimDataSwitchStateMachine.this.mSimConnStateTrackerReady) {
                            SimDataSwitchStateMachine.this.setIsMmsOnging(true);
                            SimDataSwitchStateMachine.this.updateSimConnTile();
                            return;
                        }
                        return;
                    }
                    if (action.equals("com.android.mms.transaction.STOP") && !SimDataConnectionTile.this.isWifiOnlyDevice() && SimDataSwitchStateMachine.this.mSimConnStateTrackerReady) {
                        SimDataSwitchStateMachine.this.setIsMmsOnging(false);
                        SimDataSwitchStateMachine.this.updateSimConnTile();
                    }
                }
            }
        };

        public SIMConnState getCurrentSimConnState() {
            return this.mCurrentSimConnState;
        }

        public SimDataSwitchStateMachine() {
            this.mSlotCount = 0;
            this.mTelephonyManager = (TelephonyManager) SimDataConnectionTile.this.mContext.getSystemService("phone");
            this.mSlotCount = SIMHelper.getSlotCount();
            this.mPhoneStateListener = new PhoneStateListener[this.mSlotCount];
        }

        private void registerReceiver() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.AIRPLANE_MODE");
            intentFilter.addAction("com.android.mms.transaction.START");
            intentFilter.addAction("com.android.mms.transaction.STOP");
            intentFilter.addAction("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE");
            intentFilter.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
            intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
            intentFilter.addAction("android.intent.action.ANY_DATA_STATE");
            SimDataConnectionTile.this.mContext.registerReceiver(this.mSimStateIntentReceiver, intentFilter);
        }

        private void unregisterReceiver() {
            SimDataConnectionTile.this.mContext.unregisterReceiver(this.mSimStateIntentReceiver);
        }

        private void updateSimConnTile() {
            onActualStateChange(SimDataConnectionTile.this.mContext, null);
            SimDataConnectionTile.this.refreshState();
        }

        public void refresh() {
            onActualStateChange(SimDataConnectionTile.this.mContext, null);
            setUserSwitching(false);
        }

        public void onActualStateChange(Context context, Intent intent) {
            boolean z;
            boolean z2;
            List<SubscriptionInfo> activeSubscriptionInfoList = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
            boolean zIsSimEnable = isSimEnable(activeSubscriptionInfoList, 0);
            boolean zIsSimEnable2 = isSimEnable(activeSubscriptionInfoList, 1);
            int defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
            if (SubscriptionManager.getSlotIndex(defaultDataSubscriptionId) != 0) {
                if (SubscriptionManager.getSlotIndex(defaultDataSubscriptionId) != 1) {
                    z = false;
                    z2 = false;
                } else {
                    z = false;
                    z2 = true;
                }
            } else {
                z2 = false;
                z = true;
            }
            Log.d("SimDataConnectionTile", "SimConnStateTracker onActualStateChange sim1Enable = " + zIsSimEnable + ", sim2Enable = " + zIsSimEnable2);
            if (zIsSimEnable || zIsSimEnable2) {
                boolean zIsDataConnected = isDataConnected();
                Log.d("SimDataConnectionTile", "onActualStateChange, dataConnected = " + zIsDataConnected + ", sim1Enable = " + zIsSimEnable + ", sim2Enable = " + zIsSimEnable2 + ", sim1Conn = " + z + ", sim2Conn = " + z2);
                if (zIsDataConnected) {
                    if (zIsSimEnable && zIsSimEnable2) {
                        if (z) {
                            this.mCurrentSimConnState = SIMConnState.SIM1_E_E;
                        } else {
                            this.mCurrentSimConnState = SIMConnState.SIM2_E_E;
                        }
                    } else if (!zIsSimEnable && zIsSimEnable2) {
                        if (isSimInsertedWithUnAvaliable(activeSubscriptionInfoList, 0) && z) {
                            this.mCurrentSimConnState = SIMConnState.SIM1_E_F;
                        } else {
                            this.mCurrentSimConnState = SIMConnState.SIM2_D_E;
                        }
                    } else if (zIsSimEnable && !zIsSimEnable2) {
                        if (isSimInsertedWithUnAvaliable(activeSubscriptionInfoList, 1) && z2) {
                            this.mCurrentSimConnState = SIMConnState.SIM2_E_F;
                        } else {
                            this.mCurrentSimConnState = SIMConnState.SIM1_D_E;
                        }
                    }
                } else if (zIsSimEnable && zIsSimEnable2) {
                    if (z) {
                        this.mCurrentSimConnState = SIMConnState.SIM1_E_D;
                    } else {
                        this.mCurrentSimConnState = SIMConnState.SIM2_E_D;
                    }
                } else if (!zIsSimEnable && zIsSimEnable2) {
                    if (isSimInsertedWithUnAvaliable(activeSubscriptionInfoList, 0) && z) {
                        this.mCurrentSimConnState = SIMConnState.SIM1_E_F;
                    } else {
                        this.mCurrentSimConnState = SIMConnState.SIM2_D_D;
                    }
                } else if (zIsSimEnable && !zIsSimEnable2) {
                    if (isSimInsertedWithUnAvaliable(activeSubscriptionInfoList, 1) && z2) {
                        this.mCurrentSimConnState = SIMConnState.SIM2_E_F;
                    } else {
                        this.mCurrentSimConnState = SIMConnState.SIM1_D_D;
                    }
                }
            } else if (isSimInsertedWithUnAvaliable(activeSubscriptionInfoList, 0) && z) {
                this.mCurrentSimConnState = SIMConnState.SIM1_D_F;
            } else if (isSimInsertedWithUnAvaliable(activeSubscriptionInfoList, 1) && z2) {
                this.mCurrentSimConnState = SIMConnState.SIM2_D_F;
            } else {
                this.mCurrentSimConnState = SIMConnState.NO_SIM;
            }
            setUserSwitching(false);
        }

        private boolean isSimEnable(List<SubscriptionInfo> list, int i) {
            return SimDataConnectionTile.this.isSimInsertedBySlot(list, i) && !isAirplaneMode() && isRadioOn(i) && !isSimLocked(i);
        }

        private boolean isSimInsertedWithUnAvaliable(List<SubscriptionInfo> list, int i) {
            return SimDataConnectionTile.this.isSimInsertedBySlot(list, i) && (!isRadioOn(i) || isAirplaneMode() || isSimLocked(i));
        }

        private boolean isRadioOn(int i) {
            return SIMHelper.isRadioOn(SIMHelper.getFirstSubInSlot(i));
        }

        private boolean isSimLocked(int i) {
            int simState = TelephonyManager.getDefault().getSimState(i);
            boolean z = simState == 2 || simState == 3 || simState == 4;
            Log.d("SimDataConnectionTile", "isSimLocked, slotId=" + i + " simState=" + simState + " bSimLocked= " + z);
            return z;
        }

        public void toggleState(Context context) {
            enterNextState(this.mCurrentSimConnState);
        }

        private void enterNextState(SIMConnState sIMConnState) {
            Log.d("SimDataConnectionTile", "enterNextState state is " + sIMConnState);
            switch (sIMConnState) {
                case NO_SIM:
                case SIM1_D_D:
                case SIM1_D_E:
                case SIM2_D_D:
                case SIM2_D_E:
                case SIM1_D_F:
                case SIM2_D_F:
                    Log.d("SimDataConnectionTile", "No Sim or one Sim do nothing!");
                    break;
                case SIM1_E_D:
                    Log.d("SimDataConnectionTile", "Try to switch from Sim1 to Sim2! mSimCurrentCurrentState=" + this.mCurrentSimConnState);
                    this.mCurrentSimConnState = SIMConnState.SIM2_E_D;
                    switchDataDefaultSIM(1);
                    break;
                case SIM1_E_E:
                    Log.d("SimDataConnectionTile", "Try to switch from Sim1 to Sim2! mSimCurrentCurrentState=" + this.mCurrentSimConnState);
                    this.mCurrentSimConnState = SIMConnState.SIM2_E_E;
                    switchDataDefaultSIM(1);
                    break;
                case SIM2_E_D:
                    Log.d("SimDataConnectionTile", "Try to switch from Sim2 to Sim1! mSimCurrentCurrentState=" + this.mCurrentSimConnState);
                    this.mCurrentSimConnState = SIMConnState.SIM1_E_D;
                    switchDataDefaultSIM(0);
                    break;
                case SIM2_E_E:
                    Log.d("SimDataConnectionTile", "Try to switch from Sim2 to Sim1! mSimCurrentCurrentState=" + this.mCurrentSimConnState);
                    this.mCurrentSimConnState = SIMConnState.SIM1_E_E;
                    switchDataDefaultSIM(0);
                    break;
                case SIM1_E_F:
                    Log.d("SimDataConnectionTile", "Try to switch from Sim1 to Sim2! mSimCurrentCurrentState=" + this.mCurrentSimConnState);
                    switchDataDefaultSIM(1);
                    break;
                case SIM2_E_F:
                    Log.d("SimDataConnectionTile", "Try to switch from Sim2 to Sim1! mSimCurrentCurrentState=" + this.mCurrentSimConnState);
                    switchDataDefaultSIM(0);
                    break;
            }
        }

        private void switchDataDefaultSIM(int i) {
            if (!SimDataConnectionTile.this.isWifiOnlyDevice()) {
                setUserSwitching(true);
                handleDataConnectionChange(i);
            }
        }

        private void handleDataConnectionChange(int i) {
            Log.d("SimDataConnectionTile", "handleDataConnectionChange, newSlot=" + i);
            if (SubscriptionManager.getSlotIndex(SubscriptionManager.getDefaultDataSubscriptionId()) != i) {
                this.mDataTimerHandler.sendEmptyMessageDelayed(2000, 30000L);
                List<SubscriptionInfo> activeSubscriptionInfoList = SubscriptionManager.from(SimDataConnectionTile.this.mContext).getActiveSubscriptionInfoList();
                if (activeSubscriptionInfoList != null && activeSubscriptionInfoList.size() > 0) {
                    boolean dataEnabled = this.mTelephonyManager.getDataEnabled();
                    for (int i2 = 0; i2 < activeSubscriptionInfoList.size(); i2++) {
                        SubscriptionInfo subscriptionInfo = activeSubscriptionInfoList.get(i2);
                        int subscriptionId = subscriptionInfo.getSubscriptionId();
                        if (i == subscriptionInfo.getSimSlotIndex()) {
                            Log.d("SimDataConnectionTile", "handleDataConnectionChange. newSlot = " + i + " subId = " + subscriptionId);
                            SubscriptionManager.from(SimDataConnectionTile.this.mContext).setDefaultDataSubId(subscriptionId);
                            if (dataEnabled) {
                                this.mTelephonyManager.setDataEnabled(subscriptionId, true);
                            }
                        } else if (dataEnabled) {
                            this.mTelephonyManager.setDataEnabled(subscriptionId, false);
                        }
                    }
                }
            }
        }

        public boolean isClickable() {
            List<SubscriptionInfo> activeSubscriptionInfoList = SubscriptionManager.from(SimDataConnectionTile.this.mContext).getActiveSubscriptionInfoList();
            if (SimDataConnectionTile.this.isSimInsertedBySlot(activeSubscriptionInfoList, 0) || SimDataConnectionTile.this.isSimInsertedBySlot(activeSubscriptionInfoList, 1)) {
                return ((!isRadioOn(0) && !isRadioOn(1)) || isAirplaneMode() || isMmsOngoing() || isUserSwitching()) ? false : true;
            }
            return false;
        }

        private boolean isDataConnected() {
            return TelephonyManager.getDefault().getDataState() == 2;
        }

        private void setIsMmsOnging(boolean z) {
            this.mMmsOngoing = z;
        }

        private boolean isMmsOngoing() {
            return this.mMmsOngoing;
        }

        private void setAirplaneMode(boolean z) {
            this.mIsAirlineMode = z;
        }

        private boolean isAirplaneMode() {
            return this.mIsAirlineMode;
        }

        private void setUserSwitching(boolean z) {
            this.mIsUserSwitching = z;
        }

        private boolean isUserSwitching() {
            return this.mIsUserSwitching;
        }

        private PhoneConstants.DataState getMobileDataState(Intent intent) {
            String stringExtra = intent.getStringExtra("state");
            if (stringExtra != null) {
                return Enum.valueOf(PhoneConstants.DataState.class, stringExtra);
            }
            return PhoneConstants.DataState.DISCONNECTED;
        }

        private void registerPhoneStateListener() {
            for (int i = 0; i < this.mSlotCount; i++) {
                int firstSubInSlot = SIMHelper.getFirstSubInSlot(i);
                if (firstSubInSlot >= 0) {
                    this.mPhoneStateListener[i] = getPhoneStateListener(firstSubInSlot, i);
                    this.mTelephonyManager.listen(this.mPhoneStateListener[i], 1);
                } else {
                    this.mPhoneStateListener[i] = null;
                }
            }
        }

        private PhoneStateListener getPhoneStateListener(int i, final int i2) {
            return new PhoneStateListener(Integer.valueOf(i)) { // from class: com.mediatek.systemui.qs.tiles.ext.SimDataConnectionTile.SimDataSwitchStateMachine.3
                @Override // android.telephony.PhoneStateListener
                public void onServiceStateChanged(ServiceState serviceState) {
                    Log.d("SimDataConnectionTile", "PhoneStateListener:onServiceStateChanged, slot " + i2 + " servicestate = " + serviceState);
                    SimDataSwitchStateMachine.this.updateSimConnTile();
                }
            };
        }

        private void unRegisterPhoneStateListener() {
            for (int i = 0; i < this.mSlotCount; i++) {
                if (this.mPhoneStateListener[i] != null) {
                    this.mTelephonyManager.listen(this.mPhoneStateListener[i], 0);
                }
            }
        }
    }

    private boolean isWifiOnlyDevice() {
        Context context = this.mContext;
        Context context2 = this.mContext;
        return !((ConnectivityManager) context.getSystemService("connectivity")).isNetworkSupported(0);
    }

    private boolean isSimInsertedBySlot(List<SubscriptionInfo> list, int i) {
        if (i >= SIMHelper.getSlotCount()) {
            return false;
        }
        if (list != null && list.size() > 0) {
            Iterator<SubscriptionInfo> it = list.iterator();
            while (it.hasNext()) {
                if (it.next().getSimSlotIndex() == i) {
                    return true;
                }
            }
            return false;
        }
        Log.d("SimDataConnectionTile", "isSimInsertedBySlot, SubscriptionInfo is null");
        return false;
    }
}
