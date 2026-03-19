package com.mediatek.mmsdk.callback;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.Surface;
import com.mediatek.mmsdk.BaseParameters;
import com.mediatek.mmsdk.BinderHolder;
import com.mediatek.mmsdk.CameraEffectHalException;
import com.mediatek.mmsdk.CameraEffectHalRuntimeException;
import com.mediatek.mmsdk.EffectHalVersion;
import com.mediatek.mmsdk.IEffectFactory;
import com.mediatek.mmsdk.IFeatureManager;
import com.mediatek.mmsdk.IMMSdkService;
import com.mediatek.mmsdk.callback.ICallbackClient;
import java.util.List;

public class MmsdkCallbackClient {
    public static final int CAMERA_MSG_COMPRESSED_IMAGE = 256;
    private static final String TAG = "MmsdkCallbackClient";
    private ICallbackClient mICallbackClient;
    private IEffectFactory mIEffectFactory;
    private IFeatureManager mIFeatureManager;
    private IMMSdkService mIMmsdkService;

    public MmsdkCallbackClient(Context context) {
    }

    public boolean isCallbackClientSupported() {
        try {
            if (getEffectFactory() != null) {
                return isCallbackSupported();
            }
            return false;
        } catch (CameraEffectHalException e) {
            Log.e(TAG, "Current not support Effect HAl", e);
            return false;
        }
    }

    public void start() throws CameraEffectHalException {
        init();
        try {
            this.mICallbackClient.start();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during start", e);
            throw new CameraEffectHalException(CameraEffectHalException.EFFECT_INITIAL_ERROR);
        }
    }

    public void stop() throws CameraEffectHalException {
        try {
            this.mICallbackClient.stop();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during stop", e);
            throw new CameraEffectHalException(CameraEffectHalException.EFFECT_HAL_ERROR);
        }
    }

    public void setOutputSurfaces(List<Surface> list, List<BaseParameters> list2) throws CameraEffectHalException {
        try {
            this.mICallbackClient.setOutputSurfaces(list, list2);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during set Listener", e);
            throw new CameraEffectHalRuntimeException(CameraEffectHalException.EFFECT_HAL_LISTENER_ERROR).asChecked();
        }
    }

    private void init() throws CameraEffectHalException {
        getMmSdkService();
        getFeatureManager();
        getEffectFactory();
        this.mICallbackClient = createCallbackClient(new EffectHalVersion());
    }

    private IMMSdkService getMmSdkService() throws CameraEffectHalException {
        if (this.mIMmsdkService == null) {
            IBinder service = ServiceManager.getService(BaseParameters.CAMERA_MM_SERVICE_BINDER_NAME);
            if (service == null) {
                throw new CameraEffectHalException(CameraEffectHalException.EFFECT_HAL_SERVICE_ERROR);
            }
            this.mIMmsdkService = IMMSdkService.Stub.asInterface(service);
        }
        return this.mIMmsdkService;
    }

    private boolean isCallbackSupported() throws CameraEffectHalException {
        getMmSdkService();
        try {
            return this.mIMmsdkService.existCallbackClient() == 1;
        } catch (RemoteException e) {
            throw new CameraEffectHalException(CameraEffectHalException.EFFECT_HAL_SERVICE_ERROR);
        }
    }

    private IFeatureManager getFeatureManager() throws CameraEffectHalException {
        getMmSdkService();
        if (this.mIFeatureManager == null) {
            BinderHolder binderHolder = new BinderHolder();
            try {
                this.mIMmsdkService.connectFeatureManager(binderHolder);
                this.mIFeatureManager = IFeatureManager.Stub.asInterface(binderHolder.getBinder());
            } catch (RemoteException e) {
                throw new CameraEffectHalException(CameraEffectHalException.EFFECT_HAL_FEATUREMANAGER_ERROR);
            }
        }
        return this.mIFeatureManager;
    }

    private IEffectFactory getEffectFactory() throws CameraEffectHalException {
        getFeatureManager();
        if (this.mIEffectFactory == null) {
            BinderHolder binderHolder = new BinderHolder();
            try {
                this.mIFeatureManager.getEffectFactory(binderHolder);
                this.mIEffectFactory = IEffectFactory.Stub.asInterface(binderHolder.getBinder());
            } catch (RemoteException e) {
                throw new CameraEffectHalException(CameraEffectHalException.EFFECT_HAL_FACTORY_ERROR);
            }
        }
        return this.mIEffectFactory;
    }

    private ICallbackClient createCallbackClient(EffectHalVersion effectHalVersion) throws CameraEffectHalException {
        getEffectFactory();
        BinderHolder binderHolder = new BinderHolder();
        try {
            this.mIEffectFactory.createCallbackClient(effectHalVersion, binderHolder);
            return ICallbackClient.Stub.asInterface(binderHolder.getBinder());
        } catch (RemoteException e) {
            throw new CameraEffectHalException(CameraEffectHalException.EFFECT_HAL_CLIENT_ERROR);
        }
    }
}
