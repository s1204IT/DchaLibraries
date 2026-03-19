package com.android.bluetooth.opp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v4.content.FileProvider;
import android.util.Log;
import java.io.File;

public class BluetoothOppFileProvider extends FileProvider {
    private static final String TAG = "BluetoothOppFileProvider";
    private Context mContext = null;
    private ProviderInfo mProviderInfo = null;
    private boolean mRegisteredReceiver = false;
    private boolean mInitialized = false;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction())) {
                BluetoothOppFileProvider.this.attachInfo(BluetoothOppFileProvider.this.mContext, BluetoothOppFileProvider.this.mProviderInfo);
            }
        }
    };

    @Override
    public void attachInfo(Context context, ProviderInfo providerInfo) {
        synchronized (this) {
            this.mContext = context;
            this.mProviderInfo = providerInfo;
            if (!this.mRegisteredReceiver) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.intent.action.USER_UNLOCKED");
                this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.CURRENT, intentFilter, null, null);
                this.mRegisteredReceiver = true;
            }
            if (((UserManager) this.mContext.getSystemService("user")).isUserUnlocked()) {
                if (!this.mInitialized) {
                    if (Constants.DEBUG) {
                        Log.d(TAG, "Initialized");
                    }
                    super.attachInfo(this.mContext, this.mProviderInfo);
                    this.mInitialized = true;
                }
                if (this.mRegisteredReceiver) {
                    this.mContext.unregisterReceiver(this.mBroadcastReceiver);
                    this.mRegisteredReceiver = false;
                }
            }
        }
    }

    public static Uri getUriForFile(Context context, String str, File file) {
        if (!((UserManager) context.getSystemService("user")).isUserUnlocked()) {
            return null;
        }
        return FileProvider.getUriForFile(context.createCredentialProtectedStorageContext(), str, file);
    }
}
