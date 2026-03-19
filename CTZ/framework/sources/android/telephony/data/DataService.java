package android.telephony.data;

import android.app.Service;
import android.content.Intent;
import android.net.LinkProperties;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.Rlog;
import android.telephony.data.IDataService;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class DataService extends Service {
    private static final int DATA_SERVICE_CREATE_DATA_SERVICE_PROVIDER = 1;
    public static final String DATA_SERVICE_EXTRA_SLOT_ID = "android.telephony.data.extra.SLOT_ID";
    private static final int DATA_SERVICE_INDICATION_DATA_CALL_LIST_CHANGED = 11;
    public static final String DATA_SERVICE_INTERFACE = "android.telephony.data.DataService";
    private static final int DATA_SERVICE_REMOVE_ALL_DATA_SERVICE_PROVIDERS = 3;
    private static final int DATA_SERVICE_REMOVE_DATA_SERVICE_PROVIDER = 2;
    private static final int DATA_SERVICE_REQUEST_DEACTIVATE_DATA_CALL = 5;
    private static final int DATA_SERVICE_REQUEST_GET_DATA_CALL_LIST = 8;
    private static final int DATA_SERVICE_REQUEST_REGISTER_DATA_CALL_LIST_CHANGED = 9;
    private static final int DATA_SERVICE_REQUEST_SETUP_DATA_CALL = 4;
    private static final int DATA_SERVICE_REQUEST_SET_DATA_PROFILE = 7;
    private static final int DATA_SERVICE_REQUEST_SET_INITIAL_ATTACH_APN = 6;
    private static final int DATA_SERVICE_REQUEST_UNREGISTER_DATA_CALL_LIST_CHANGED = 10;
    public static final int REQUEST_REASON_HANDOVER = 3;
    public static final int REQUEST_REASON_NORMAL = 1;
    public static final int REQUEST_REASON_SHUTDOWN = 2;
    private static final String TAG = DataService.class.getSimpleName();
    private final DataServiceHandler mHandler;
    private final SparseArray<DataServiceProvider> mServiceMap = new SparseArray<>();

    @VisibleForTesting
    public final IDataServiceWrapper mBinder = new IDataServiceWrapper();
    private final HandlerThread mHandlerThread = new HandlerThread(TAG);

    @Retention(RetentionPolicy.SOURCE)
    public @interface DeactivateDataReason {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SetupDataReason {
    }

    public abstract DataServiceProvider createDataServiceProvider(int i);

    public class DataServiceProvider {
        private final List<IDataServiceCallback> mDataCallListChangedCallbacks = new ArrayList();
        private final int mSlotId;

        public DataServiceProvider(int i) {
            this.mSlotId = i;
        }

        public final int getSlotId() {
            return this.mSlotId;
        }

        public void setupDataCall(int i, DataProfile dataProfile, boolean z, boolean z2, int i2, LinkProperties linkProperties, DataServiceCallback dataServiceCallback) {
            dataServiceCallback.onSetupDataCallComplete(1, null);
        }

        public void deactivateDataCall(int i, int i2, DataServiceCallback dataServiceCallback) {
            dataServiceCallback.onDeactivateDataCallComplete(1);
        }

        public void setInitialAttachApn(DataProfile dataProfile, boolean z, DataServiceCallback dataServiceCallback) {
            dataServiceCallback.onSetInitialAttachApnComplete(1);
        }

        public void setDataProfile(List<DataProfile> list, boolean z, DataServiceCallback dataServiceCallback) {
            dataServiceCallback.onSetDataProfileComplete(1);
        }

        public void getDataCallList(DataServiceCallback dataServiceCallback) {
            dataServiceCallback.onGetDataCallListComplete(1, null);
        }

        private void registerForDataCallListChanged(IDataServiceCallback iDataServiceCallback) {
            synchronized (this.mDataCallListChangedCallbacks) {
                this.mDataCallListChangedCallbacks.add(iDataServiceCallback);
            }
        }

        private void unregisterForDataCallListChanged(IDataServiceCallback iDataServiceCallback) {
            synchronized (this.mDataCallListChangedCallbacks) {
                this.mDataCallListChangedCallbacks.remove(iDataServiceCallback);
            }
        }

        public final void notifyDataCallListChanged(List<DataCallResponse> list) {
            synchronized (this.mDataCallListChangedCallbacks) {
                Iterator<IDataServiceCallback> it = this.mDataCallListChangedCallbacks.iterator();
                while (it.hasNext()) {
                    DataService.this.mHandler.obtainMessage(11, this.mSlotId, 0, new DataCallListChangedIndication(list, it.next())).sendToTarget();
                }
            }
        }

        protected void onDestroy() {
            this.mDataCallListChangedCallbacks.clear();
        }
    }

    private static final class SetupDataCallRequest {
        public final int accessNetworkType;
        public final boolean allowRoaming;
        public final IDataServiceCallback callback;
        public final DataProfile dataProfile;
        public final boolean isRoaming;
        public final LinkProperties linkProperties;
        public final int reason;

        SetupDataCallRequest(int i, DataProfile dataProfile, boolean z, boolean z2, int i2, LinkProperties linkProperties, IDataServiceCallback iDataServiceCallback) {
            this.accessNetworkType = i;
            this.dataProfile = dataProfile;
            this.isRoaming = z;
            this.allowRoaming = z2;
            this.linkProperties = linkProperties;
            this.reason = i2;
            this.callback = iDataServiceCallback;
        }
    }

    private static final class DeactivateDataCallRequest {
        public final IDataServiceCallback callback;
        public final int cid;
        public final int reason;

        DeactivateDataCallRequest(int i, int i2, IDataServiceCallback iDataServiceCallback) {
            this.cid = i;
            this.reason = i2;
            this.callback = iDataServiceCallback;
        }
    }

    private static final class SetInitialAttachApnRequest {
        public final IDataServiceCallback callback;
        public final DataProfile dataProfile;
        public final boolean isRoaming;

        SetInitialAttachApnRequest(DataProfile dataProfile, boolean z, IDataServiceCallback iDataServiceCallback) {
            this.dataProfile = dataProfile;
            this.isRoaming = z;
            this.callback = iDataServiceCallback;
        }
    }

    private static final class SetDataProfileRequest {
        public final IDataServiceCallback callback;
        public final List<DataProfile> dps;
        public final boolean isRoaming;

        SetDataProfileRequest(List<DataProfile> list, boolean z, IDataServiceCallback iDataServiceCallback) {
            this.dps = list;
            this.isRoaming = z;
            this.callback = iDataServiceCallback;
        }
    }

    private static final class DataCallListChangedIndication {
        public final IDataServiceCallback callback;
        public final List<DataCallResponse> dataCallList;

        DataCallListChangedIndication(List<DataCallResponse> list, IDataServiceCallback iDataServiceCallback) {
            this.dataCallList = list;
            this.callback = iDataServiceCallback;
        }
    }

    private class DataServiceHandler extends Handler {
        DataServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            DataServiceCallback dataServiceCallback;
            int i = message.arg1;
            DataServiceProvider dataServiceProvider = (DataServiceProvider) DataService.this.mServiceMap.get(i);
            switch (message.what) {
                case 1:
                    DataServiceProvider dataServiceProviderCreateDataServiceProvider = DataService.this.createDataServiceProvider(message.arg1);
                    if (dataServiceProviderCreateDataServiceProvider != null) {
                        DataService.this.mServiceMap.put(i, dataServiceProviderCreateDataServiceProvider);
                    }
                    break;
                case 2:
                    if (dataServiceProvider != null) {
                        dataServiceProvider.onDestroy();
                        DataService.this.mServiceMap.remove(i);
                    }
                    break;
                case 3:
                    for (int i2 = 0; i2 < DataService.this.mServiceMap.size(); i2++) {
                        DataServiceProvider dataServiceProvider2 = (DataServiceProvider) DataService.this.mServiceMap.get(i2);
                        if (dataServiceProvider2 != null) {
                            dataServiceProvider2.onDestroy();
                        }
                    }
                    DataService.this.mServiceMap.clear();
                    break;
                case 4:
                    if (dataServiceProvider != null) {
                        SetupDataCallRequest setupDataCallRequest = (SetupDataCallRequest) message.obj;
                        int i3 = setupDataCallRequest.accessNetworkType;
                        DataProfile dataProfile = setupDataCallRequest.dataProfile;
                        boolean z = setupDataCallRequest.isRoaming;
                        boolean z2 = setupDataCallRequest.allowRoaming;
                        int i4 = setupDataCallRequest.reason;
                        LinkProperties linkProperties = setupDataCallRequest.linkProperties;
                        if (setupDataCallRequest.callback == null) {
                            dataServiceCallback = null;
                        } else {
                            dataServiceCallback = new DataServiceCallback(setupDataCallRequest.callback);
                        }
                        dataServiceProvider.setupDataCall(i3, dataProfile, z, z2, i4, linkProperties, dataServiceCallback);
                        break;
                    }
                    break;
                case 5:
                    if (dataServiceProvider != null) {
                        DeactivateDataCallRequest deactivateDataCallRequest = (DeactivateDataCallRequest) message.obj;
                        dataServiceProvider.deactivateDataCall(deactivateDataCallRequest.cid, deactivateDataCallRequest.reason, deactivateDataCallRequest.callback != null ? new DataServiceCallback(deactivateDataCallRequest.callback) : null);
                        break;
                    }
                    break;
                case 6:
                    if (dataServiceProvider != null) {
                        SetInitialAttachApnRequest setInitialAttachApnRequest = (SetInitialAttachApnRequest) message.obj;
                        dataServiceProvider.setInitialAttachApn(setInitialAttachApnRequest.dataProfile, setInitialAttachApnRequest.isRoaming, setInitialAttachApnRequest.callback != null ? new DataServiceCallback(setInitialAttachApnRequest.callback) : null);
                        break;
                    }
                    break;
                case 7:
                    if (dataServiceProvider != null) {
                        SetDataProfileRequest setDataProfileRequest = (SetDataProfileRequest) message.obj;
                        dataServiceProvider.setDataProfile(setDataProfileRequest.dps, setDataProfileRequest.isRoaming, setDataProfileRequest.callback != null ? new DataServiceCallback(setDataProfileRequest.callback) : null);
                        break;
                    }
                    break;
                case 8:
                    if (dataServiceProvider != null) {
                        dataServiceProvider.getDataCallList(new DataServiceCallback((IDataServiceCallback) message.obj));
                        break;
                    }
                    break;
                case 9:
                    if (dataServiceProvider != null) {
                        dataServiceProvider.registerForDataCallListChanged((IDataServiceCallback) message.obj);
                        break;
                    }
                    break;
                case 10:
                    if (dataServiceProvider != null) {
                        dataServiceProvider.unregisterForDataCallListChanged((IDataServiceCallback) message.obj);
                        break;
                    }
                    break;
                case 11:
                    if (dataServiceProvider != null) {
                        DataCallListChangedIndication dataCallListChangedIndication = (DataCallListChangedIndication) message.obj;
                        try {
                            dataCallListChangedIndication.callback.onDataCallListChanged(dataCallListChangedIndication.dataCallList);
                        } catch (RemoteException e) {
                            DataService.this.loge("Failed to call onDataCallListChanged. " + e);
                            return;
                        }
                        break;
                    }
                    break;
            }
        }
    }

    public DataService() {
        this.mHandlerThread.start();
        this.mHandler = new DataServiceHandler(this.mHandlerThread.getLooper());
        log("Data service created");
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (intent == null || !DATA_SERVICE_INTERFACE.equals(intent.getAction())) {
            loge("Unexpected intent " + intent);
            return null;
        }
        return this.mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        this.mHandler.obtainMessage(3).sendToTarget();
        return false;
    }

    @Override
    public void onDestroy() {
        this.mHandlerThread.quit();
    }

    private class IDataServiceWrapper extends IDataService.Stub {
        private IDataServiceWrapper() {
        }

        @Override
        public void createDataServiceProvider(int i) {
            DataService.this.mHandler.obtainMessage(1, i, 0).sendToTarget();
        }

        @Override
        public void removeDataServiceProvider(int i) {
            DataService.this.mHandler.obtainMessage(2, i, 0).sendToTarget();
        }

        @Override
        public void setupDataCall(int i, int i2, DataProfile dataProfile, boolean z, boolean z2, int i3, LinkProperties linkProperties, IDataServiceCallback iDataServiceCallback) {
            DataService.this.mHandler.obtainMessage(4, i, 0, new SetupDataCallRequest(i2, dataProfile, z, z2, i3, linkProperties, iDataServiceCallback)).sendToTarget();
        }

        @Override
        public void deactivateDataCall(int i, int i2, int i3, IDataServiceCallback iDataServiceCallback) {
            DataService.this.mHandler.obtainMessage(5, i, 0, new DeactivateDataCallRequest(i2, i3, iDataServiceCallback)).sendToTarget();
        }

        @Override
        public void setInitialAttachApn(int i, DataProfile dataProfile, boolean z, IDataServiceCallback iDataServiceCallback) {
            DataService.this.mHandler.obtainMessage(6, i, 0, new SetInitialAttachApnRequest(dataProfile, z, iDataServiceCallback)).sendToTarget();
        }

        @Override
        public void setDataProfile(int i, List<DataProfile> list, boolean z, IDataServiceCallback iDataServiceCallback) {
            DataService.this.mHandler.obtainMessage(7, i, 0, new SetDataProfileRequest(list, z, iDataServiceCallback)).sendToTarget();
        }

        @Override
        public void getDataCallList(int i, IDataServiceCallback iDataServiceCallback) {
            if (iDataServiceCallback == null) {
                DataService.this.loge("getDataCallList: callback is null");
            } else {
                DataService.this.mHandler.obtainMessage(8, i, 0, iDataServiceCallback).sendToTarget();
            }
        }

        @Override
        public void registerForDataCallListChanged(int i, IDataServiceCallback iDataServiceCallback) {
            if (iDataServiceCallback == null) {
                DataService.this.loge("registerForDataCallListChanged: callback is null");
            } else {
                DataService.this.mHandler.obtainMessage(9, i, 0, iDataServiceCallback).sendToTarget();
            }
        }

        @Override
        public void unregisterForDataCallListChanged(int i, IDataServiceCallback iDataServiceCallback) {
            if (iDataServiceCallback == null) {
                DataService.this.loge("unregisterForDataCallListChanged: callback is null");
            } else {
                DataService.this.mHandler.obtainMessage(10, i, 0, iDataServiceCallback).sendToTarget();
            }
        }
    }

    private void log(String str) {
        Rlog.d(TAG, str);
    }

    private void loge(String str) {
        Rlog.e(TAG, str);
    }
}
