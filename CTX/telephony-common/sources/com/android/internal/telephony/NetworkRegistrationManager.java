package com.android.internal.telephony;

import android.R;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.telephony.CarrierConfigManager;
import android.telephony.INetworkService;
import android.telephony.INetworkServiceCallback;
import android.telephony.NetworkRegistrationState;
import android.telephony.Rlog;
import java.util.Hashtable;
import java.util.Map;

public class NetworkRegistrationManager {
    private static final String TAG = NetworkRegistrationManager.class.getSimpleName();
    private final CarrierConfigManager mCarrierConfigManager;
    private RegManagerDeathRecipient mDeathRecipient;
    private final Phone mPhone;
    private INetworkService.Stub mServiceBinder;
    private final int mTransportType;
    private final RegistrantList mRegStateChangeRegistrants = new RegistrantList();
    private final Map<NetworkRegStateCallback, Message> mCallbackTable = new Hashtable();

    public NetworkRegistrationManager(int i, Phone phone) {
        this.mTransportType = i;
        this.mPhone = phone;
        this.mCarrierConfigManager = (CarrierConfigManager) phone.getContext().getSystemService("carrier_config");
        bindService();
    }

    public boolean isServiceConnected() {
        return this.mServiceBinder != null && this.mServiceBinder.isBinderAlive();
    }

    public void unregisterForNetworkRegistrationStateChanged(Handler handler) {
        this.mRegStateChangeRegistrants.remove(handler);
    }

    public void registerForNetworkRegistrationStateChanged(Handler handler, int i, Object obj) {
        logd("registerForNetworkRegistrationStateChanged");
        new Registrant(handler, i, obj);
        this.mRegStateChangeRegistrants.addUnique(handler, i, obj);
    }

    public void getNetworkRegistrationState(int i, Message message) {
        if (message == null) {
            return;
        }
        logd("getNetworkRegistrationState domain " + i);
        if (!isServiceConnected()) {
            logd("service not connected.");
            message.obj = new AsyncResult(message.obj, (Object) null, new IllegalStateException("Service not connected."));
            message.sendToTarget();
            return;
        }
        NetworkRegStateCallback networkRegStateCallback = new NetworkRegStateCallback();
        try {
            this.mCallbackTable.put(networkRegStateCallback, message);
            this.mServiceBinder.getNetworkRegistrationState(this.mPhone.getPhoneId(), i, networkRegStateCallback);
        } catch (RemoteException e) {
            Rlog.e(TAG, "getNetworkRegistrationState RemoteException " + e);
            this.mCallbackTable.remove(networkRegStateCallback);
            message.obj = new AsyncResult(message.obj, (Object) null, e);
            message.sendToTarget();
        }
    }

    private class RegManagerDeathRecipient implements IBinder.DeathRecipient {
        private final ComponentName mComponentName;

        RegManagerDeathRecipient(ComponentName componentName) {
            this.mComponentName = componentName;
        }

        @Override
        public void binderDied() {
            NetworkRegistrationManager.logd("NetworkService(" + this.mComponentName + " transport type " + NetworkRegistrationManager.this.mTransportType + ") died.");
        }
    }

    private class NetworkServiceConnection implements ServiceConnection {
        private NetworkServiceConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            NetworkRegistrationManager.logd("service connected.");
            NetworkRegistrationManager.this.mServiceBinder = (INetworkService.Stub) iBinder;
            NetworkRegistrationManager.this.mDeathRecipient = NetworkRegistrationManager.this.new RegManagerDeathRecipient(componentName);
            try {
                NetworkRegistrationManager.this.mServiceBinder.linkToDeath(NetworkRegistrationManager.this.mDeathRecipient, 0);
                NetworkRegistrationManager.this.mServiceBinder.createNetworkServiceProvider(NetworkRegistrationManager.this.mPhone.getPhoneId());
                NetworkRegistrationManager.this.mServiceBinder.registerForNetworkRegistrationStateChanged(NetworkRegistrationManager.this.mPhone.getPhoneId(), new NetworkRegStateCallback());
            } catch (RemoteException e) {
                NetworkRegistrationManager.this.mDeathRecipient.binderDied();
                NetworkRegistrationManager.logd("RemoteException " + e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            NetworkRegistrationManager.logd("onServiceDisconnected " + componentName);
            if (NetworkRegistrationManager.this.mServiceBinder != null) {
                NetworkRegistrationManager.this.mServiceBinder.unlinkToDeath(NetworkRegistrationManager.this.mDeathRecipient, 0);
            }
        }
    }

    private class NetworkRegStateCallback extends INetworkServiceCallback.Stub {
        private NetworkRegStateCallback() {
        }

        public void onGetNetworkRegistrationStateComplete(int i, NetworkRegistrationState networkRegistrationState) {
            NetworkRegistrationManager.logd("onGetNetworkRegistrationStateComplete result " + i + " state " + networkRegistrationState);
            Message message = (Message) NetworkRegistrationManager.this.mCallbackTable.remove(this);
            if (message == null) {
                NetworkRegistrationManager.loge("onCompleteMessage is null");
                return;
            }
            message.arg1 = i;
            message.obj = new AsyncResult(message.obj, networkRegistrationState, (Throwable) null);
            message.sendToTarget();
        }

        public void onNetworkStateChanged() {
            NetworkRegistrationManager.logd("onNetworkStateChanged");
            NetworkRegistrationManager.this.mRegStateChangeRegistrants.notifyRegistrants();
        }
    }

    private boolean bindService() {
        Intent intent = new Intent("android.telephony.NetworkService");
        intent.setPackage(getPackageName());
        try {
            return this.mPhone.getContext().bindService(intent, new NetworkServiceConnection(), 1);
        } catch (SecurityException e) {
            loge("bindService failed " + e);
            return false;
        }
    }

    private String getPackageName() {
        int i;
        String str;
        switch (this.mTransportType) {
            case 1:
                i = R.string.app_streaming_blocked_title_for_camera_dialog;
                str = "carrier_network_service_wwan_package_override_string";
                break;
            case 2:
                i = R.string.app_streaming_blocked_message_for_settings_dialog;
                str = "carrier_network_service_wlan_package_override_string";
                break;
            default:
                throw new IllegalStateException("Transport type not WWAN or WLAN. type=" + this.mTransportType);
        }
        String string = this.mPhone.getContext().getResources().getString(i);
        PersistableBundle configForSubId = this.mCarrierConfigManager.getConfigForSubId(this.mPhone.getSubId());
        if (configForSubId != null) {
            string = configForSubId.getString(str, string);
        }
        logd("Binding to packageName " + string + " for transport type" + this.mTransportType);
        return string;
    }

    private static int logd(String str) {
        return Rlog.d(TAG, str);
    }

    private static int loge(String str) {
        return Rlog.e(TAG, str);
    }
}
