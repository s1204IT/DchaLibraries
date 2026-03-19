package com.android.internal.telephony.ims;

import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsMmTelFeature;
import android.telephony.ims.aidl.IImsRcsFeature;
import android.telephony.ims.aidl.IImsRegistration;
import android.util.Log;
import android.util.SparseArray;
import com.android.ims.internal.IImsFeatureStatusCallback;
import com.android.ims.internal.IImsMMTelFeature;
import com.android.ims.internal.IImsServiceController;
import com.android.internal.telephony.ims.ImsServiceController;

public class ImsServiceControllerCompat extends ImsServiceController {
    private static final String TAG = "ImsSCCompat";
    protected final SparseArray<ImsConfigCompatAdapter> mConfigCompatAdapters;
    protected final SparseArray<MmTelFeatureCompatAdapter> mMmTelCompatAdapters;
    protected final SparseArray<ImsRegistrationCompatAdapter> mRegCompatAdapters;
    private IImsServiceController mServiceController;

    public ImsServiceControllerCompat(Context context, ComponentName componentName, ImsServiceController.ImsServiceControllerCallbacks imsServiceControllerCallbacks) {
        super(context, componentName, imsServiceControllerCallbacks);
        this.mMmTelCompatAdapters = new SparseArray<>();
        this.mConfigCompatAdapters = new SparseArray<>();
        this.mRegCompatAdapters = new SparseArray<>();
    }

    @Override
    protected final String getServiceInterface() {
        return "android.telephony.ims.compat.ImsService";
    }

    @Override
    public final void enableIms(int i) {
        MmTelFeatureCompatAdapter mmTelFeatureCompatAdapter = this.mMmTelCompatAdapters.get(i);
        if (mmTelFeatureCompatAdapter == null) {
            Log.w(TAG, "enableIms: adapter null for slot :" + i);
            return;
        }
        try {
            mmTelFeatureCompatAdapter.enableIms();
        } catch (RemoteException e) {
            Log.w(TAG, "Couldn't enable IMS: " + e.getMessage());
        }
    }

    @Override
    public final void disableIms(int i) {
        MmTelFeatureCompatAdapter mmTelFeatureCompatAdapter = this.mMmTelCompatAdapters.get(i);
        if (mmTelFeatureCompatAdapter == null) {
            Log.w(TAG, "enableIms: adapter null for slot :" + i);
            return;
        }
        try {
            mmTelFeatureCompatAdapter.disableIms();
        } catch (RemoteException e) {
            Log.w(TAG, "Couldn't enable IMS: " + e.getMessage());
        }
    }

    @Override
    public final IImsRegistration getRegistration(int i) {
        ImsRegistrationCompatAdapter imsRegistrationCompatAdapter = this.mRegCompatAdapters.get(i);
        if (imsRegistrationCompatAdapter == null) {
            Log.w(TAG, "getRegistration: Registration does not exist for slot " + i);
            return null;
        }
        return imsRegistrationCompatAdapter.getBinder();
    }

    @Override
    public final IImsConfig getConfig(int i) {
        ImsConfigCompatAdapter imsConfigCompatAdapter = this.mConfigCompatAdapters.get(i);
        if (imsConfigCompatAdapter == null) {
            Log.w(TAG, "getConfig: Config does not exist for slot " + i);
            return null;
        }
        return imsConfigCompatAdapter.getIImsConfig();
    }

    @Override
    protected final void notifyImsServiceReady() {
        Log.d(TAG, "notifyImsServiceReady");
    }

    @Override
    protected final IInterface createImsFeature(int i, int i2, IImsFeatureStatusCallback iImsFeatureStatusCallback) throws RemoteException {
        switch (i2) {
            case 1:
                return createMMTelCompat(i, iImsFeatureStatusCallback);
            case 2:
                return createRcsFeature(i, iImsFeatureStatusCallback);
            default:
                return null;
        }
    }

    @Override
    protected final void removeImsFeature(int i, int i2, IImsFeatureStatusCallback iImsFeatureStatusCallback) throws RemoteException {
        if (i2 == 1) {
            this.mMmTelCompatAdapters.remove(i);
            this.mRegCompatAdapters.remove(i);
            this.mConfigCompatAdapters.remove(i);
        }
        if (this.mServiceController != null) {
            this.mServiceController.removeImsFeature(i, i2, iImsFeatureStatusCallback);
        }
    }

    @Override
    protected void setServiceController(IBinder iBinder) {
        this.mServiceController = IImsServiceController.Stub.asInterface(iBinder);
    }

    @Override
    protected boolean isServiceControllerAvailable() {
        return this.mServiceController != null;
    }

    protected MmTelInterfaceAdapter getInterface(int i, IImsFeatureStatusCallback iImsFeatureStatusCallback) throws RemoteException {
        IImsMMTelFeature iImsMMTelFeatureCreateMMTelFeature = this.mServiceController.createMMTelFeature(i, iImsFeatureStatusCallback);
        if (iImsMMTelFeatureCreateMMTelFeature == null) {
            Log.w(TAG, "createMMTelCompat: createMMTelFeature returned null.");
            return null;
        }
        return new MmTelInterfaceAdapter(i, iImsMMTelFeatureCreateMMTelFeature.asBinder());
    }

    protected IImsMmTelFeature createMMTelCompat(int i, IImsFeatureStatusCallback iImsFeatureStatusCallback) throws RemoteException {
        MmTelFeatureCompatAdapter mmTelFeatureCompatAdapter = new MmTelFeatureCompatAdapter(this.mContext, i, getInterface(i, iImsFeatureStatusCallback));
        this.mMmTelCompatAdapters.put(i, mmTelFeatureCompatAdapter);
        ImsRegistrationCompatAdapter imsRegistrationCompatAdapter = new ImsRegistrationCompatAdapter();
        mmTelFeatureCompatAdapter.addRegistrationAdapter(imsRegistrationCompatAdapter);
        this.mRegCompatAdapters.put(i, imsRegistrationCompatAdapter);
        this.mConfigCompatAdapters.put(i, new ImsConfigCompatAdapter(mmTelFeatureCompatAdapter.getOldConfigInterface()));
        return mmTelFeatureCompatAdapter.getBinder();
    }

    private IImsRcsFeature createRcsFeature(int i, IImsFeatureStatusCallback iImsFeatureStatusCallback) {
        return null;
    }
}
