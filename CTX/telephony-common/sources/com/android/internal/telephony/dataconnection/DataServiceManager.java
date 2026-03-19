package com.android.internal.telephony.dataconnection;

import android.R;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.net.LinkProperties;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataProfile;
import android.telephony.data.IDataService;
import android.telephony.data.IDataServiceCallback;
import android.text.TextUtils;
import com.android.internal.telephony.Phone;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DataServiceManager {
    public static final String DATA_CALL_RESPONSE = "data_call_response";
    private static final boolean DBG = false;
    private static final String TAG = DataServiceManager.class.getSimpleName();
    private final AppOpsManager mAppOps;
    private final CarrierConfigManager mCarrierConfigManager;
    private ComponentName mComponentName;
    private DataServiceManagerDeathRecipient mDeathRecipient;
    private IDataService mIDataService;
    private final Phone mPhone;
    private final int mTransportType;
    private final RegistrantList mServiceBindingChangedRegistrants = new RegistrantList();
    private final Map<IBinder, Message> mMessageMap = new ConcurrentHashMap();
    private final RegistrantList mDataCallListChangedRegistrants = new RegistrantList();
    private boolean mBound = false;
    private final IPackageManager mPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));

    private class DataServiceManagerDeathRecipient implements IBinder.DeathRecipient {
        private DataServiceManagerDeathRecipient() {
        }

        @Override
        public void binderDied() {
            DataServiceManager.this.loge("DataService(" + DataServiceManager.this.mComponentName + " transport type " + DataServiceManager.this.mTransportType + ") died.");
        }
    }

    private void grantPermissionsToService(String str) {
        String[] strArr = {str};
        try {
            this.mPackageManager.grantDefaultPermissionsToEnabledTelephonyDataServices(strArr, this.mPhone.getContext().getUserId());
            this.mAppOps.setMode(75, this.mPhone.getContext().getUserId(), strArr[0], 0);
        } catch (RemoteException e) {
            loge("Binder to package manager died, permission grant for DataService failed.");
            throw e.rethrowAsRuntimeException();
        }
    }

    private void revokePermissionsFromUnusedDataServices() {
        Set<String> allDataServicePackageNames = getAllDataServicePackageNames();
        for (int i : new int[]{1, 2}) {
            allDataServicePackageNames.remove(getDataServicePackageName(i));
        }
        try {
            String[] strArr = new String[allDataServicePackageNames.size()];
            allDataServicePackageNames.toArray(strArr);
            this.mPackageManager.revokeDefaultPermissionsFromDisabledTelephonyDataServices(strArr, this.mPhone.getContext().getUserId());
            Iterator<String> it = allDataServicePackageNames.iterator();
            while (it.hasNext()) {
                this.mAppOps.setMode(75, this.mPhone.getContext().getUserId(), it.next(), 2);
            }
        } catch (RemoteException e) {
            loge("Binder to package manager died; failed to revoke DataService permissions.");
            throw e.rethrowAsRuntimeException();
        }
    }

    private final class CellularDataServiceConnection implements ServiceConnection {
        private CellularDataServiceConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            DataServiceManager.this.mComponentName = componentName;
            DataServiceManager.this.mIDataService = IDataService.Stub.asInterface(iBinder);
            DataServiceManager.this.mDeathRecipient = new DataServiceManagerDeathRecipient();
            DataServiceManager.this.mBound = true;
            try {
                iBinder.linkToDeath(DataServiceManager.this.mDeathRecipient, 0);
                DataServiceManager.this.mIDataService.createDataServiceProvider(DataServiceManager.this.mPhone.getPhoneId());
                DataServiceManager.this.mIDataService.registerForDataCallListChanged(DataServiceManager.this.mPhone.getPhoneId(), new CellularDataServiceCallback());
                DataServiceManager.this.mServiceBindingChangedRegistrants.notifyResult(true);
            } catch (RemoteException e) {
                DataServiceManager.this.mDeathRecipient.binderDied();
                DataServiceManager.this.loge("Remote exception. " + e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            DataServiceManager.this.mIDataService.asBinder().unlinkToDeath(DataServiceManager.this.mDeathRecipient, 0);
            DataServiceManager.this.mIDataService = null;
            DataServiceManager.this.mBound = false;
            DataServiceManager.this.mServiceBindingChangedRegistrants.notifyResult(false);
        }
    }

    private final class CellularDataServiceCallback extends IDataServiceCallback.Stub {
        private CellularDataServiceCallback() {
        }

        public void onSetupDataCallComplete(int i, DataCallResponse dataCallResponse) {
            Message message = (Message) DataServiceManager.this.mMessageMap.remove(asBinder());
            if (message == null) {
                DataServiceManager.this.loge("Unable to find the message for setup call response.");
            } else {
                message.getData().putParcelable(DataServiceManager.DATA_CALL_RESPONSE, dataCallResponse);
                DataServiceManager.this.sendCompleteMessage(message, i);
            }
        }

        public void onDeactivateDataCallComplete(int i) {
            DataServiceManager.this.sendCompleteMessage((Message) DataServiceManager.this.mMessageMap.remove(asBinder()), i);
        }

        public void onSetInitialAttachApnComplete(int i) {
            DataServiceManager.this.sendCompleteMessage((Message) DataServiceManager.this.mMessageMap.remove(asBinder()), i);
        }

        public void onSetDataProfileComplete(int i) {
            DataServiceManager.this.sendCompleteMessage((Message) DataServiceManager.this.mMessageMap.remove(asBinder()), i);
        }

        public void onGetDataCallListComplete(int i, List<DataCallResponse> list) {
            DataServiceManager.this.sendCompleteMessage((Message) DataServiceManager.this.mMessageMap.remove(asBinder()), i);
        }

        public void onDataCallListChanged(List<DataCallResponse> list) {
            DataServiceManager.this.mDataCallListChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, list, (Throwable) null));
        }
    }

    public DataServiceManager(Phone phone, int i) {
        this.mPhone = phone;
        this.mTransportType = i;
        this.mCarrierConfigManager = (CarrierConfigManager) phone.getContext().getSystemService("carrier_config");
        this.mAppOps = (AppOpsManager) phone.getContext().getSystemService("appops");
        bindDataService();
    }

    private void bindDataService() {
        revokePermissionsFromUnusedDataServices();
        String dataServicePackageName = getDataServicePackageName();
        if (TextUtils.isEmpty(dataServicePackageName)) {
            loge("Can't find the binding package");
            return;
        }
        grantPermissionsToService(dataServicePackageName);
        try {
            if (!this.mPhone.getContext().bindService(new Intent("android.telephony.data.DataService").setPackage(dataServicePackageName), new CellularDataServiceConnection(), 1)) {
                loge("Cannot bind to the data service.");
            }
        } catch (Exception e) {
            loge("Cannot bind to the data service. Exception: " + e);
        }
    }

    private Set<String> getAllDataServicePackageNames() {
        List<ResolveInfo> listQueryIntentServices = this.mPhone.getContext().getPackageManager().queryIntentServices(new Intent("android.telephony.data.DataService"), 1048576);
        HashSet hashSet = new HashSet();
        for (ResolveInfo resolveInfo : listQueryIntentServices) {
            if (resolveInfo.serviceInfo != null) {
                hashSet.add(resolveInfo.serviceInfo.packageName);
            }
        }
        return hashSet;
    }

    private String getDataServicePackageName() {
        return getDataServicePackageName(this.mTransportType);
    }

    private String getDataServicePackageName(int i) {
        int i2;
        String str;
        switch (i) {
            case 1:
                i2 = R.string.app_streaming_blocked_title;
                str = "carrier_data_service_wwan_package_override_string";
                break;
            case 2:
                i2 = R.string.app_streaming_blocked_message_for_permission_request;
                str = "carrier_data_service_wlan_package_override_string";
                break;
            default:
                throw new IllegalStateException("Transport type not WWAN or WLAN. type=" + this.mTransportType);
        }
        String string = this.mPhone.getContext().getResources().getString(i2);
        PersistableBundle configForSubId = this.mCarrierConfigManager.getConfigForSubId(this.mPhone.getSubId());
        if (configForSubId != null) {
            return configForSubId.getString(str, string);
        }
        return string;
    }

    private void sendCompleteMessage(Message message, int i) {
        if (message != null) {
            message.arg1 = i;
            message.sendToTarget();
        }
    }

    public void setupDataCall(int i, DataProfile dataProfile, boolean z, boolean z2, int i2, LinkProperties linkProperties, Message message) {
        CellularDataServiceCallback cellularDataServiceCallback;
        if (!this.mBound) {
            loge("Data service not bound.");
            sendCompleteMessage(message, 4);
            return;
        }
        if (message != null) {
            cellularDataServiceCallback = new CellularDataServiceCallback();
            this.mMessageMap.put(cellularDataServiceCallback.asBinder(), message);
        } else {
            cellularDataServiceCallback = null;
        }
        try {
            this.mIDataService.setupDataCall(this.mPhone.getPhoneId(), i, dataProfile, z, z2, i2, linkProperties, cellularDataServiceCallback);
        } catch (RemoteException e) {
            loge("Cannot invoke setupDataCall on data service.");
            if (cellularDataServiceCallback != null) {
                this.mMessageMap.remove(cellularDataServiceCallback.asBinder());
            }
            sendCompleteMessage(message, 4);
        }
    }

    public void deactivateDataCall(int i, int i2, Message message) {
        if (!this.mBound) {
            loge("Data service not bound.");
            sendCompleteMessage(message, 4);
            return;
        }
        CellularDataServiceCallback cellularDataServiceCallback = null;
        Object[] objArr = 0;
        if (message != null) {
            CellularDataServiceCallback cellularDataServiceCallback2 = new CellularDataServiceCallback();
            this.mMessageMap.put(cellularDataServiceCallback2.asBinder(), message);
            cellularDataServiceCallback = cellularDataServiceCallback2;
        }
        try {
            this.mIDataService.deactivateDataCall(this.mPhone.getPhoneId(), i, i2, cellularDataServiceCallback);
        } catch (RemoteException e) {
            loge("Cannot invoke deactivateDataCall on data service.");
            if (cellularDataServiceCallback != null) {
                this.mMessageMap.remove(cellularDataServiceCallback.asBinder());
            }
            sendCompleteMessage(message, 4);
        }
    }

    public void setInitialAttachApn(DataProfile dataProfile, boolean z, Message message) {
        if (!this.mBound) {
            loge("Data service not bound.");
            sendCompleteMessage(message, 4);
            return;
        }
        CellularDataServiceCallback cellularDataServiceCallback = null;
        Object[] objArr = 0;
        if (message != null) {
            CellularDataServiceCallback cellularDataServiceCallback2 = new CellularDataServiceCallback();
            this.mMessageMap.put(cellularDataServiceCallback2.asBinder(), message);
            cellularDataServiceCallback = cellularDataServiceCallback2;
        }
        try {
            this.mIDataService.setInitialAttachApn(this.mPhone.getPhoneId(), dataProfile, z, cellularDataServiceCallback);
        } catch (RemoteException e) {
            loge("Cannot invoke setInitialAttachApn on data service.");
            if (cellularDataServiceCallback != null) {
                this.mMessageMap.remove(cellularDataServiceCallback.asBinder());
            }
            sendCompleteMessage(message, 4);
        }
    }

    public void setDataProfile(List<DataProfile> list, boolean z, Message message) {
        if (!this.mBound) {
            loge("Data service not bound.");
            sendCompleteMessage(message, 4);
            return;
        }
        CellularDataServiceCallback cellularDataServiceCallback = null;
        Object[] objArr = 0;
        if (message != null) {
            CellularDataServiceCallback cellularDataServiceCallback2 = new CellularDataServiceCallback();
            this.mMessageMap.put(cellularDataServiceCallback2.asBinder(), message);
            cellularDataServiceCallback = cellularDataServiceCallback2;
        }
        try {
            this.mIDataService.setDataProfile(this.mPhone.getPhoneId(), list, z, cellularDataServiceCallback);
        } catch (RemoteException e) {
            loge("Cannot invoke setDataProfile on data service.");
            if (cellularDataServiceCallback != null) {
                this.mMessageMap.remove(cellularDataServiceCallback.asBinder());
            }
            sendCompleteMessage(message, 4);
        }
    }

    public void getDataCallList(Message message) {
        if (!this.mBound) {
            loge("Data service not bound.");
            sendCompleteMessage(message, 4);
            return;
        }
        CellularDataServiceCallback cellularDataServiceCallback = null;
        Object[] objArr = 0;
        if (message != null) {
            CellularDataServiceCallback cellularDataServiceCallback2 = new CellularDataServiceCallback();
            this.mMessageMap.put(cellularDataServiceCallback2.asBinder(), message);
            cellularDataServiceCallback = cellularDataServiceCallback2;
        }
        try {
            this.mIDataService.getDataCallList(this.mPhone.getPhoneId(), cellularDataServiceCallback);
        } catch (RemoteException e) {
            loge("Cannot invoke getDataCallList on data service.");
            if (cellularDataServiceCallback != null) {
                this.mMessageMap.remove(cellularDataServiceCallback.asBinder());
            }
            sendCompleteMessage(message, 4);
        }
    }

    public void registerForDataCallListChanged(Handler handler, int i) {
        if (handler != null) {
            this.mDataCallListChangedRegistrants.addUnique(handler, i, (Object) null);
        }
    }

    public void unregisterForDataCallListChanged(Handler handler) {
        if (handler != null) {
            this.mDataCallListChangedRegistrants.remove(handler);
        }
    }

    public void registerForServiceBindingChanged(Handler handler, int i, Object obj) {
        if (handler != null) {
            this.mServiceBindingChangedRegistrants.addUnique(handler, i, obj);
        }
    }

    public void unregisterForServiceBindingChanged(Handler handler) {
        if (handler != null) {
            this.mServiceBindingChangedRegistrants.remove(handler);
        }
    }

    public int getTransportType() {
        return this.mTransportType;
    }

    private void log(String str) {
        Rlog.d(TAG, str);
    }

    private void loge(String str) {
        Rlog.e(TAG, str);
    }
}
