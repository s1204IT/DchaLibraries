package com.android.internal.telephony;

import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.CellInfo;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.VoLteServiceState;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneInternalInterface;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.List;

public class DefaultPhoneNotifier implements PhoneNotifier {

    private static final int[] f2comandroidinternaltelephonyCall$StateSwitchesValues = null;

    private static final int[] f3xce0d2696 = null;

    private static final int[] f4xb32e9020 = null;

    private static final int[] f5xb8401fb4 = null;
    private static final boolean DBG = false;
    private static final String LOG_TAG = "DefaultPhoneNotifier";
    protected ITelephonyRegistry mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));

    private static int[] m6getcomandroidinternaltelephonyCall$StateSwitchesValues() {
        if (f2comandroidinternaltelephonyCall$StateSwitchesValues != null) {
            return f2comandroidinternaltelephonyCall$StateSwitchesValues;
        }
        int[] iArr = new int[Call.State.valuesCustom().length];
        try {
            iArr[Call.State.ACTIVE.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Call.State.ALERTING.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[Call.State.DIALING.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[Call.State.DISCONNECTED.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[Call.State.DISCONNECTING.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[Call.State.HOLDING.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[Call.State.IDLE.ordinal()] = 18;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[Call.State.INCOMING.ordinal()] = 7;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[Call.State.WAITING.ordinal()] = 8;
        } catch (NoSuchFieldError e9) {
        }
        f2comandroidinternaltelephonyCall$StateSwitchesValues = iArr;
        return iArr;
    }

    private static int[] m7x9dac0c3a() {
        if (f3xce0d2696 != null) {
            return f3xce0d2696;
        }
        int[] iArr = new int[PhoneConstants.DataState.values().length];
        try {
            iArr[PhoneConstants.DataState.CONNECTED.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[PhoneConstants.DataState.CONNECTING.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[PhoneConstants.DataState.DISCONNECTED.ordinal()] = 18;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[PhoneConstants.DataState.SUSPENDED.ordinal()] = 3;
        } catch (NoSuchFieldError e4) {
        }
        f3xce0d2696 = iArr;
        return iArr;
    }

    private static int[] m8x3549e7c4() {
        if (f4xb32e9020 != null) {
            return f4xb32e9020;
        }
        int[] iArr = new int[PhoneConstants.State.values().length];
        try {
            iArr[PhoneConstants.State.IDLE.ordinal()] = 18;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[PhoneConstants.State.OFFHOOK.ordinal()] = 1;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[PhoneConstants.State.RINGING.ordinal()] = 2;
        } catch (NoSuchFieldError e3) {
        }
        f4xb32e9020 = iArr;
        return iArr;
    }

    private static int[] m9x2c473d58() {
        if (f5xb8401fb4 != null) {
            return f5xb8401fb4;
        }
        int[] iArr = new int[PhoneInternalInterface.DataActivityState.valuesCustom().length];
        try {
            iArr[PhoneInternalInterface.DataActivityState.DATAIN.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[PhoneInternalInterface.DataActivityState.DATAINANDOUT.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[PhoneInternalInterface.DataActivityState.DATAOUT.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[PhoneInternalInterface.DataActivityState.DORMANT.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[PhoneInternalInterface.DataActivityState.NONE.ordinal()] = 18;
        } catch (NoSuchFieldError e5) {
        }
        f5xb8401fb4 = iArr;
        return iArr;
    }

    @Override
    public void notifyPhoneState(Phone sender) {
        Call ringingCall = sender.getRingingCall();
        int subId = sender.getSubId();
        int phoneId = sender.getPhoneId();
        int phoneType = sender.getPhoneType();
        String incomingNumber = UsimPBMemInfo.STRING_NOT_SET;
        if (ringingCall != null && ringingCall.getEarliestConnection() != null) {
            incomingNumber = ringingCall.getEarliestConnection().getAddress();
        }
        try {
            if (this.mRegistry == null) {
                return;
            }
            this.mRegistry.notifyCallStateForPhoneInfo(phoneType, phoneId, subId, convertCallState(sender.getState()), incomingNumber);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyServiceState(Phone sender) {
        ServiceState ss = sender.getServiceState();
        int phoneId = sender.getPhoneId();
        int subId = sender.getSubId();
        Rlog.d(LOG_TAG, "nofityServiceState: mRegistry=" + this.mRegistry + " ss=" + ss + " sender=" + sender + " phondId=" + phoneId + " subId=" + subId);
        if (ss == null) {
            ss = new ServiceState();
            ss.setStateOutOfService();
        }
        try {
            if (this.mRegistry != null) {
                this.mRegistry.notifyServiceStateForPhoneId(phoneId, subId, ss);
            }
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifySignalStrength(Phone sender) {
        int phoneId = sender.getPhoneId();
        int subId = sender.getSubId();
        try {
            if (this.mRegistry == null) {
                return;
            }
            this.mRegistry.notifySignalStrengthForPhoneId(phoneId, subId, sender.getSignalStrength());
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyMessageWaitingChanged(Phone sender) {
        int phoneId = sender.getPhoneId();
        int subId = sender.getSubId();
        try {
            if (this.mRegistry == null) {
                return;
            }
            this.mRegistry.notifyMessageWaitingChangedForPhoneId(phoneId, subId, sender.getMessageWaitingIndicator());
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyCallForwardingChanged(Phone sender) {
        int subId = sender.getSubId();
        Rlog.d(LOG_TAG, "notifyCallForwardingChanged: " + subId + ",  mRegistry: " + this.mRegistry);
        try {
            if (this.mRegistry == null) {
                return;
            }
            this.mRegistry.notifyCallForwardingChangedForSubscriber(subId, sender.getCallForwardingIndicator());
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyDataActivity(Phone sender) {
        int subId = sender.getSubId();
        try {
            if (this.mRegistry == null) {
                return;
            }
            this.mRegistry.notifyDataActivityForSubscriber(subId, convertDataActivityState(sender.getDataActivityState()));
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyDataConnection(Phone sender, String reason, String apnType, PhoneConstants.DataState state) {
        doNotifyDataConnection(sender, reason, apnType, state);
    }

    private void doNotifyDataConnection(Phone sender, String reason, String apnType, PhoneConstants.DataState state) {
        int subId = sender.getSubId();
        SubscriptionManager.getDefaultDataSubscriptionId();
        TelephonyManager telephony = TelephonyManager.getDefault();
        LinkProperties linkProperties = null;
        NetworkCapabilities networkCapabilities = null;
        boolean roaming = DBG;
        if (state == PhoneConstants.DataState.CONNECTED) {
            linkProperties = sender.getLinkProperties(apnType);
            networkCapabilities = sender.getNetworkCapabilities(apnType);
        }
        ServiceState ss = sender.getServiceState();
        if (ss != null) {
            roaming = ss.getDataRoaming();
        }
        int networkType = telephony != null ? telephony.getDataNetworkType(subId) : 0;
        try {
            if (this.mRegistry == null) {
                return;
            }
            if (sender.getActiveApnHost(apnType) == null && !"default".equals(apnType) && !"emergency".equals(apnType)) {
                return;
            }
            this.mRegistry.notifyDataConnectionForSubscriber(subId, convertDataState(state), sender.isDataConnectivityPossible(apnType), reason, sender.getActiveApnHost(apnType), apnType, linkProperties, networkCapabilities, networkType, roaming);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyDataConnectionFailed(Phone sender, String reason, String apnType) {
        int subId = sender.getSubId();
        try {
            if (this.mRegistry == null) {
                return;
            }
            this.mRegistry.notifyDataConnectionFailedForSubscriber(subId, reason, apnType);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyCellLocation(Phone sender) {
        int subId = sender.getSubId();
        Bundle data = new Bundle();
        sender.getCellLocation().fillInNotifierBundle(data);
        try {
            if (this.mRegistry == null) {
                return;
            }
            this.mRegistry.notifyCellLocationForSubscriber(subId, data);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyCellInfo(Phone sender, List<CellInfo> cellInfo) {
        int subId = sender.getSubId();
        try {
            if (this.mRegistry == null) {
                return;
            }
            this.mRegistry.notifyCellInfoForSubscriber(subId, cellInfo);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyOtaspChanged(Phone sender, int otaspMode) {
        try {
            if (this.mRegistry == null) {
                return;
            }
            this.mRegistry.notifyOtaspChanged(otaspMode);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyPreciseCallState(Phone sender) {
        Call ringingCall = sender.getRingingCall();
        Call foregroundCall = sender.getForegroundCall();
        Call backgroundCall = sender.getBackgroundCall();
        if (ringingCall == null || foregroundCall == null || backgroundCall == null) {
            return;
        }
        try {
            this.mRegistry.notifyPreciseCallState(convertPreciseCallState(ringingCall.getState()), convertPreciseCallState(foregroundCall.getState()), convertPreciseCallState(backgroundCall.getState()));
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyDisconnectCause(int cause, int preciseCause) {
        try {
            this.mRegistry.notifyDisconnectCause(cause, preciseCause);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyPreciseDataConnectionFailed(Phone sender, String reason, String apnType, String apn, String failCause) {
        try {
            this.mRegistry.notifyPreciseDataConnectionFailed(reason, apnType, apn, failCause);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyVoLteServiceStateChanged(Phone sender, VoLteServiceState lteState) {
        try {
            this.mRegistry.notifyVoLteServiceStateChanged(lteState);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyOemHookRawEventForSubscriber(int subId, byte[] rawData) {
        try {
            this.mRegistry.notifyOemHookRawEventForSubscriber(subId, rawData);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyLteAccessStratumChanged(Phone sender, String state) {
        try {
            if (this.mRegistry == null) {
                return;
            }
            this.mRegistry.notifyLteAccessStratumChanged(state);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyPsNetworkTypeChanged(Phone sender, int nwType) {
        try {
            if (this.mRegistry == null) {
                return;
            }
            this.mRegistry.notifyPsNetworkTypeChanged(nwType);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifySharedDefaultApnStateChanged(Phone sender, boolean isSharedDefaultApn) {
        try {
            if (this.mRegistry == null) {
                return;
            }
            this.mRegistry.notifySharedDefaultApnStateChanged(isSharedDefaultApn);
        } catch (RemoteException e) {
        }
    }

    public static int convertCallState(PhoneConstants.State state) {
        switch (m8x3549e7c4()[state.ordinal()]) {
            case 1:
                return 2;
            case 2:
                return 1;
            default:
                return 0;
        }
    }

    public static PhoneConstants.State convertCallState(int state) {
        switch (state) {
            case 1:
                return PhoneConstants.State.RINGING;
            case 2:
                return PhoneConstants.State.OFFHOOK;
            default:
                return PhoneConstants.State.IDLE;
        }
    }

    public static int convertDataState(PhoneConstants.DataState state) {
        switch (m7x9dac0c3a()[state.ordinal()]) {
            case 1:
                return 2;
            case 2:
                return 1;
            case 3:
                return 3;
            default:
                return 0;
        }
    }

    public static PhoneConstants.DataState convertDataState(int state) {
        switch (state) {
            case 1:
                return PhoneConstants.DataState.CONNECTING;
            case 2:
                return PhoneConstants.DataState.CONNECTED;
            case 3:
                return PhoneConstants.DataState.SUSPENDED;
            default:
                return PhoneConstants.DataState.DISCONNECTED;
        }
    }

    public static int convertDataActivityState(PhoneInternalInterface.DataActivityState state) {
        switch (m9x2c473d58()[state.ordinal()]) {
            case 1:
                return 1;
            case 2:
                return 3;
            case 3:
                return 2;
            case 4:
                return 4;
            default:
                return 0;
        }
    }

    public static int convertPreciseCallState(Call.State state) {
        switch (m6getcomandroidinternaltelephonyCall$StateSwitchesValues()[state.ordinal()]) {
            case 1:
                return 1;
            case 2:
                return 4;
            case 3:
                return 3;
            case 4:
                return 7;
            case 5:
                return 8;
            case 6:
                return 2;
            case 7:
                return 5;
            case 8:
                return 6;
            default:
                return 0;
        }
    }

    private void log(String s) {
        Rlog.d(LOG_TAG, s);
    }
}
