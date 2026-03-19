package com.mediatek.internal.telephony.ims;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.ims.aidl.IImsMmTelFeature;
import android.util.Log;
import com.android.ims.internal.IImsFeatureStatusCallback;
import com.android.internal.telephony.ims.ImsConfigCompatAdapter;
import com.android.internal.telephony.ims.ImsRegistrationCompatAdapter;
import com.android.internal.telephony.ims.ImsServiceController;
import com.android.internal.telephony.ims.ImsServiceControllerStaticCompat;
import com.mediatek.ims.internal.IMtkImsService;

public class MtkImsServiceControllerCompat extends ImsServiceControllerStaticCompat {
    private static final String MTK_IMS_SERVICE_NAME = "mtkIms";
    private static final String TAG = "MtkImsSCStaticCompat";
    private IBinder.DeathRecipient mMtkImsDeathRecipient;
    private IBinder mMtkImsServiceBinder;
    private IMtkImsService mMtkImsServiceCompat;

    public MtkImsServiceControllerCompat(Context context, ComponentName componentName, ImsServiceController.ImsServiceControllerCallbacks imsServiceControllerCallbacks) {
        super(context, componentName, imsServiceControllerCallbacks);
        this.mMtkImsServiceCompat = null;
        this.mMtkImsServiceBinder = null;
        this.mMtkImsDeathRecipient = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                Log.e(MtkImsServiceControllerCompat.TAG, "ImsService(MtkImsServiceControllerCompat) died.");
            }
        };
    }

    public boolean startBindToService(Intent intent, ImsServiceController.ImsServiceConnection imsServiceConnection, int i) {
        Log.i(TAG, "startBindToService vendor");
        IBinder iBinderCheckService = ServiceManager.checkService("mtkIms");
        if (iBinderCheckService == null) {
            Log.i(TAG, "get binder null");
            return false;
        }
        try {
            this.mMtkImsServiceBinder = iBinderCheckService;
            iBinderCheckService.linkToDeath(this.mMtkImsDeathRecipient, 0);
            this.mMtkImsServiceCompat = IMtkImsService.Stub.asInterface(iBinderCheckService);
        } catch (RemoteException e) {
            Log.e(TAG, "ImsService(MtkImsServiceControllerCompat) RemoteException:" + e.getMessage());
            this.mMtkImsDeathRecipient.binderDied();
        }
        Log.i(TAG, "startBindToService default");
        return super.startBindToService(intent, imsServiceConnection, i);
    }

    protected void setServiceController(IBinder iBinder) {
        super.setServiceController(iBinder);
        if (iBinder == null) {
            this.mMtkImsServiceCompat = null;
        }
    }

    public void unbind() throws RemoteException {
        Log.i(TAG, "unbind");
        if (this.mMtkImsServiceBinder != null) {
            this.mMtkImsServiceBinder.unlinkToDeath(this.mMtkImsDeathRecipient, 0);
            this.mMtkImsServiceBinder = null;
        }
        super.unbind();
    }

    protected boolean isServiceControllerAvailable() {
        Log.d(TAG, "isServiceControllerAvailable-mImsServiceCompat: " + this.mImsServiceCompat + ", mMtkImsServiceCompat:" + this.mMtkImsServiceCompat);
        return (this.mImsServiceCompat == null || this.mMtkImsServiceCompat == null) ? false : true;
    }

    protected IImsMmTelFeature createMMTelCompat(int i, IImsFeatureStatusCallback iImsFeatureStatusCallback) throws RemoteException {
        MtkMmTelFeatureCompatAdapter mtkMmTelFeatureCompatAdapter = new MtkMmTelFeatureCompatAdapter(this.mContext, i, getInterface(i, iImsFeatureStatusCallback));
        this.mMmTelCompatAdapters.put(i, mtkMmTelFeatureCompatAdapter);
        ImsRegistrationCompatAdapter imsRegistrationCompatAdapter = new ImsRegistrationCompatAdapter();
        mtkMmTelFeatureCompatAdapter.addRegistrationAdapter(imsRegistrationCompatAdapter);
        this.mRegCompatAdapters.put(i, imsRegistrationCompatAdapter);
        this.mConfigCompatAdapters.put(i, new ImsConfigCompatAdapter(mtkMmTelFeatureCompatAdapter.getOldConfigInterface()));
        return mtkMmTelFeatureCompatAdapter.getBinder();
    }
}
