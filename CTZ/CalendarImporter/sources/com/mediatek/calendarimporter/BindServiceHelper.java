package com.mediatek.calendarimporter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.mediatek.calendarimporter.service.VCalService;
import com.mediatek.calendarimporter.utils.LogUtils;

public class BindServiceHelper {
    private static final String TAG = "BindServiceHelper";
    private ServiceConnectedOperation mConnectedOperation;
    private final Context mContext;
    protected VCalService mService = null;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            BindServiceHelper.this.mService.disconnected(BindServiceHelper.this.mContext.getClass().getName());
            LogUtils.d(BindServiceHelper.TAG, "onServiceDisconnected");
            if (BindServiceHelper.this.mConnectedOperation != null) {
                BindServiceHelper.this.mConnectedOperation.serviceUnConnected();
            }
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            LogUtils.d(BindServiceHelper.TAG, "onServiceConnected");
            BindServiceHelper.this.mService = ((VCalService.MyBinder) iBinder).getService();
            if (BindServiceHelper.this.mConnectedOperation != null) {
                BindServiceHelper.this.mConnectedOperation.serviceConnected(BindServiceHelper.this.mService);
            }
        }
    };

    interface ServiceConnectedOperation {
        void serviceConnected(VCalService vCalService);

        void serviceUnConnected();
    }

    public BindServiceHelper(Context context) {
        this.mContext = context;
        if (context instanceof ServiceConnectedOperation) {
            this.mConnectedOperation = (ServiceConnectedOperation) context;
        }
    }

    public BindServiceHelper(Context context, ServiceConnectedOperation serviceConnectedOperation) {
        this.mContext = context;
        this.mConnectedOperation = serviceConnectedOperation;
    }

    public void onBindService() {
        this.mContext.bindService(new Intent(this.mContext.getApplicationContext(), (Class<?>) VCalService.class), this.mServiceConnection, 1);
    }

    public void unBindService() {
        LogUtils.d(TAG, "unBindService");
        if (this.mService != null) {
            this.mContext.unbindService(this.mServiceConnection);
        }
    }
}
