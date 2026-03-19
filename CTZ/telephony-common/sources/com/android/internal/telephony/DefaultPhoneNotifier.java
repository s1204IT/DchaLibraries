package com.android.internal.telephony;

import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.CellInfo;
import android.telephony.PhysicalChannelConfig;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.VoLteServiceState;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneInternalInterface;
import java.util.List;

public class DefaultPhoneNotifier implements PhoneNotifier {
    private static final boolean DBG = false;
    private static final String LOG_TAG = "DefaultPhoneNotifier";
    protected ITelephonyRegistry mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));

    @Override
    public void notifyPhoneState(Phone phone) {
        Call ringingCall = phone.getRingingCall();
        int subId = phone.getSubId();
        int phoneId = phone.getPhoneId();
        String address = "";
        if (ringingCall != null && ringingCall.getEarliestConnection() != null) {
            address = ringingCall.getEarliestConnection().getAddress();
        }
        try {
            if (this.mRegistry != null) {
                this.mRegistry.notifyCallStateForPhoneId(phoneId, subId, PhoneConstantConversions.convertCallState(phone.getState()), address);
            }
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyServiceState(Phone phone) {
        ServiceState serviceState = phone.getServiceState();
        int phoneId = phone.getPhoneId();
        int subId = phone.getSubId();
        Rlog.d(LOG_TAG, "nofityServiceState: mRegistry=" + this.mRegistry + " ss=" + serviceState + " sender=" + phone + " phondId=" + phoneId + " subId=" + subId);
        if (serviceState == null) {
            serviceState = new ServiceState();
            serviceState.setStateOutOfService();
        }
        try {
            if (this.mRegistry != null) {
                this.mRegistry.notifyServiceStateForPhoneId(phoneId, subId, serviceState);
            }
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifySignalStrength(Phone phone) {
        int phoneId = phone.getPhoneId();
        int subId = phone.getSubId();
        try {
            if (this.mRegistry != null) {
                this.mRegistry.notifySignalStrengthForPhoneId(phoneId, subId, phone.getSignalStrength());
            }
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyMessageWaitingChanged(Phone phone) {
        int phoneId = phone.getPhoneId();
        int subId = phone.getSubId();
        try {
            if (this.mRegistry != null) {
                this.mRegistry.notifyMessageWaitingChangedForPhoneId(phoneId, subId, phone.getMessageWaitingIndicator());
            }
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyCallForwardingChanged(Phone phone) {
        int subId = phone.getSubId();
        try {
            if (this.mRegistry != null) {
                Rlog.d(LOG_TAG, "notifyCallForwardingChanged: subId=" + subId + ", isCFActive=" + phone.getCallForwardingIndicator());
                this.mRegistry.notifyCallForwardingChangedForSubscriber(subId, phone.getCallForwardingIndicator());
            }
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyDataActivity(Phone phone) {
        int subId = phone.getSubId();
        try {
            if (this.mRegistry != null) {
                this.mRegistry.notifyDataActivityForSubscriber(subId, convertDataActivityState(phone.getDataActivityState()));
            }
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyDataConnection(Phone phone, String str, String str2, PhoneConstants.DataState dataState) {
        doNotifyDataConnection(phone, str, str2, dataState);
    }

    private void doNotifyDataConnection(Phone phone, String str, String str2, PhoneConstants.DataState dataState) {
        LinkProperties linkProperties;
        NetworkCapabilities networkCapabilities;
        boolean dataRoaming;
        int subId = phone.getSubId();
        SubscriptionManager.getDefaultDataSubscriptionId();
        TelephonyManager telephonyManager = TelephonyManager.getDefault();
        if (dataState == PhoneConstants.DataState.CONNECTED) {
            linkProperties = phone.getLinkProperties(str2);
            networkCapabilities = phone.getNetworkCapabilities(str2);
        } else {
            linkProperties = null;
            networkCapabilities = null;
        }
        ServiceState serviceState = phone.getServiceState();
        if (serviceState == null) {
            dataRoaming = false;
        } else {
            dataRoaming = serviceState.getDataRoaming();
        }
        try {
            if (this.mRegistry != null) {
                this.mRegistry.notifyDataConnectionForSubscriber(subId, PhoneConstantConversions.convertDataState(dataState), phone.isDataAllowed(), str, phone.getActiveApnHost(str2), str2, linkProperties, networkCapabilities, telephonyManager != null ? telephonyManager.getDataNetworkType(subId) : 0, dataRoaming);
            }
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyDataConnectionFailed(Phone phone, String str, String str2) {
        int subId = phone.getSubId();
        try {
            if (this.mRegistry != null) {
                this.mRegistry.notifyDataConnectionFailedForSubscriber(subId, str, str2);
            }
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyCellLocation(Phone phone) {
        int subId = phone.getSubId();
        Bundle bundle = new Bundle();
        phone.getCellLocation().fillInNotifierBundle(bundle);
        try {
            if (this.mRegistry != null) {
                this.mRegistry.notifyCellLocationForSubscriber(subId, bundle);
            }
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyCellInfo(Phone phone, List<CellInfo> list) {
        int subId = phone.getSubId();
        try {
            if (this.mRegistry != null) {
                this.mRegistry.notifyCellInfoForSubscriber(subId, list);
            }
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyPhysicalChannelConfiguration(Phone phone, List<PhysicalChannelConfig> list) {
        int subId = phone.getSubId();
        try {
            if (this.mRegistry != null) {
                this.mRegistry.notifyPhysicalChannelConfigurationForSubscriber(subId, list);
            }
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyOtaspChanged(Phone phone, int i) {
        try {
            if (this.mRegistry != null) {
                this.mRegistry.notifyOtaspChanged(i);
            }
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyPreciseCallState(Phone phone) {
        Call ringingCall = phone.getRingingCall();
        Call foregroundCall = phone.getForegroundCall();
        Call backgroundCall = phone.getBackgroundCall();
        if (ringingCall != null && foregroundCall != null && backgroundCall != null) {
            try {
                this.mRegistry.notifyPreciseCallState(convertPreciseCallState(ringingCall.getState()), convertPreciseCallState(foregroundCall.getState()), convertPreciseCallState(backgroundCall.getState()));
            } catch (RemoteException e) {
            }
        }
    }

    @Override
    public void notifyDisconnectCause(int i, int i2) {
        try {
            this.mRegistry.notifyDisconnectCause(i, i2);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyPreciseDataConnectionFailed(Phone phone, String str, String str2, String str3, String str4) {
        try {
            this.mRegistry.notifyPreciseDataConnectionFailedForSubscriber(phone.getSubId(), str, str2, str3, str4);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyVoLteServiceStateChanged(Phone phone, VoLteServiceState voLteServiceState) {
        try {
            this.mRegistry.notifyVoLteServiceStateChanged(voLteServiceState);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyDataActivationStateChanged(Phone phone, int i) {
        try {
            this.mRegistry.notifySimActivationStateChangedForPhoneId(phone.getPhoneId(), phone.getSubId(), 1, i);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyVoiceActivationStateChanged(Phone phone, int i) {
        try {
            this.mRegistry.notifySimActivationStateChangedForPhoneId(phone.getPhoneId(), phone.getSubId(), 0, i);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyUserMobileDataStateChanged(Phone phone, boolean z) {
        try {
            this.mRegistry.notifyUserMobileDataStateChangedForPhoneId(phone.getPhoneId(), phone.getSubId(), z);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void notifyOemHookRawEventForSubscriber(int i, byte[] bArr) {
        try {
            this.mRegistry.notifyOemHookRawEventForSubscriber(i, bArr);
        } catch (RemoteException e) {
        }
    }

    public static int convertDataActivityState(PhoneInternalInterface.DataActivityState dataActivityState) {
        switch (dataActivityState) {
            case DATAIN:
                return 1;
            case DATAOUT:
                return 2;
            case DATAINANDOUT:
                return 3;
            case DORMANT:
                return 4;
            default:
                return 0;
        }
    }

    public static int convertPreciseCallState(Call.State state) {
        switch (state) {
            case ACTIVE:
                return 1;
            case HOLDING:
                return 2;
            case DIALING:
                return 3;
            case ALERTING:
                return 4;
            case INCOMING:
                return 5;
            case WAITING:
                return 6;
            case DISCONNECTED:
                return 7;
            case DISCONNECTING:
                return 8;
            default:
                return 0;
        }
    }

    private void log(String str) {
        Rlog.d(LOG_TAG, str);
    }
}
