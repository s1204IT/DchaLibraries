package com.android.internal.telephony.ims;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.ServiceManager;
import android.util.Log;
import com.android.ims.internal.IImsFeatureStatusCallback;
import com.android.ims.internal.IImsService;
import com.android.internal.telephony.ims.ImsServiceController;

public class ImsServiceControllerStaticCompat extends ImsServiceControllerCompat {
    private static final String IMS_SERVICE_NAME = "ims";
    private static final String TAG = "ImsSCStaticCompat";
    protected IImsService mImsServiceCompat;

    public ImsServiceControllerStaticCompat(Context context, ComponentName componentName, ImsServiceController.ImsServiceControllerCallbacks imsServiceControllerCallbacks) {
        super(context, componentName, imsServiceControllerCallbacks);
        this.mImsServiceCompat = null;
    }

    @Override
    public boolean startBindToService(Intent intent, ImsServiceController.ImsServiceConnection imsServiceConnection, int i) {
        IBinder iBinderCheckService = ServiceManager.checkService(IMS_SERVICE_NAME);
        if (iBinderCheckService == null) {
            return false;
        }
        imsServiceConnection.onServiceConnected(new ComponentName(this.mContext, (Class<?>) ImsServiceControllerStaticCompat.class), iBinderCheckService);
        return true;
    }

    @Override
    protected void setServiceController(IBinder iBinder) {
        this.mImsServiceCompat = IImsService.Stub.asInterface(iBinder);
    }

    @Override
    protected boolean isServiceControllerAvailable() {
        return this.mImsServiceCompat != null;
    }

    @Override
    protected MmTelInterfaceAdapter getInterface(int i, IImsFeatureStatusCallback iImsFeatureStatusCallback) {
        if (this.mImsServiceCompat == null) {
            Log.w(TAG, "getInterface: IImsService returned null.");
            return null;
        }
        return new ImsServiceInterfaceAdapter(i, this.mImsServiceCompat.asBinder());
    }
}
