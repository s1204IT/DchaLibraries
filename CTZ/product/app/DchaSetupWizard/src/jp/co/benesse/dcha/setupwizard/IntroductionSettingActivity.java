package jp.co.benesse.dcha.setupwizard;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.ImageView;
import jp.co.benesse.dcha.dchaservice.IDchaService;
import jp.co.benesse.dcha.util.Logger;

/* loaded from: classes.dex */
public class IntroductionSettingActivity extends ParentSettingActivity implements View.OnClickListener {
    public static final int DIGICHALIZE_STATUS_DIGICHALIZED = 3;
    public static final int DIGICHALIZE_STATUS_DIGICHARIZING = 1;
    public static final int DIGICHALIZE_STATUS_DIGICHARIZING_DL_COMPLETE = 2;
    public static final int DIGICHALIZE_STATUS_UNDIGICHALIZE = 0;
    public static final int REQUIRED_BATTERY_AMOUNT = 50;
    private static final String TAG = IntroductionSettingActivity.class.getSimpleName();
    protected ImageView mOkBtn = null;
    protected IDchaService mDchaService = null;
    protected ServiceConnection mDchaServiceConnection = new ServiceConnection() { // from class: jp.co.benesse.dcha.setupwizard.IntroductionSettingActivity.1
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Logger.d(IntroductionSettingActivity.TAG, "onServiceConnected 0001");
            IntroductionSettingActivity.this.mDchaService = IDchaService.Stub.asInterface(iBinder);
            IntroductionSettingActivity introductionSettingActivity = IntroductionSettingActivity.this;
            introductionSettingActivity.hideNavigationBar(introductionSettingActivity.mDchaService, true);
            int setupStatus = IntroductionSettingActivity.this.getSetupStatus();
            if (setupStatus == 1 || setupStatus == 2) {
                IntroductionSettingActivity introductionSettingActivity2 = IntroductionSettingActivity.this;
                introductionSettingActivity2.doCancelDigicharize(introductionSettingActivity2.mDchaService);
            }
            IntroductionSettingActivity.this.setDefaultParam();
            if (IntroductionSettingActivity.this.mOkBtn != null) {
                IntroductionSettingActivity.this.mOkBtn.setClickable(true);
            }
            Logger.d(IntroductionSettingActivity.TAG, "onServiceConnected 0002");
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName componentName) {
            Logger.d(IntroductionSettingActivity.TAG, "onServiceDisconnected 0001");
            IntroductionSettingActivity.this.mDchaService = null;
            Logger.d(IntroductionSettingActivity.TAG, "onServiceDisconnected 0002");
        }
    };
    protected BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() { // from class: jp.co.benesse.dcha.setupwizard.IntroductionSettingActivity.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Logger.d(IntroductionSettingActivity.TAG, "onReceive 0001");
            int intExtra = intent.getIntExtra("level", -1);
            int intExtra2 = intent.getIntExtra("plugged", -1);
            if (intExtra < 50 || (intExtra2 != 1 && intExtra2 != 2)) {
                Logger.d(IntroductionSettingActivity.TAG, "onReceive 0003");
                if (IntroductionSettingActivity.this.mOkBtn != null) {
                    IntroductionSettingActivity.this.mOkBtn.setEnabled(false);
                }
            } else {
                Logger.d(IntroductionSettingActivity.TAG, "onReceive 0002");
                if (IntroductionSettingActivity.this.mOkBtn != null) {
                    IntroductionSettingActivity.this.mOkBtn.setEnabled(true);
                }
            }
            Logger.d(IntroductionSettingActivity.TAG, "onReceive 0004");
        }
    };

    @Override // jp.co.benesse.dcha.setupwizard.ParentSettingActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        Logger.d(TAG, "onCreate 0001");
        super.onCreate(bundle);
        setContentView(R.layout.act_introduction);
        this.mOkBtn = (ImageView) findViewById(R.id.ok_btn);
        this.mOkBtn.setClickable(false);
        this.mOkBtn.setOnClickListener(this);
        Logger.d(TAG, "onCreate 0002");
    }

    @Override // android.app.Activity
    protected void onResume() {
        Logger.d(TAG, "onResume 0001");
        super.onResume();
        this.mOkBtn.setClickable(false);
        Intent intent = new Intent("jp.co.benesse.dcha.dchaservice.DchaService");
        intent.setPackage("jp.co.benesse.dcha.dchaservice");
        bindService(intent, this.mDchaServiceConnection, 1);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        registerReceiver(this.mBatteryReceiver, intentFilter);
        Logger.d(TAG, "onResume 0002");
    }

    @Override // android.app.Activity
    protected void onPause() {
        Logger.d(TAG, "onPause 0001");
        super.onPause();
        unregisterReceiver(this.mBatteryReceiver);
        if (this.mDchaService != null) {
            Logger.d(TAG, "onPause 0002");
            unbindService(this.mDchaServiceConnection);
            this.mDchaService = null;
        }
        Logger.d(TAG, "onPause 0003");
    }

    @Override // jp.co.benesse.dcha.setupwizard.ParentSettingActivity, android.app.Activity
    protected void onDestroy() {
        Logger.d(TAG, "onDestroy 0001");
        super.onDestroy();
        this.mOkBtn.setClickable(false);
        this.mOkBtn.setOnClickListener(null);
        this.mOkBtn = null;
        Logger.d(TAG, "onDestroy 0002");
    }

    protected boolean isRoot() {
        try {
            Logger.d(TAG, "isRoot 0001");
            if (this.mDchaService == null) {
                return false;
            }
            Logger.d(TAG, "isRoot 0002");
            return this.mDchaService.checkPadRooted();
        } catch (RemoteException e) {
            Logger.d(TAG, "isRoot 0003");
            Logger.d(TAG, "isRoot", e);
            return false;
        }
    }

    protected boolean removeTask() {
        try {
            Logger.d(TAG, "removeTask 0001");
            if (this.mDchaService == null) {
                return false;
            }
            Logger.d(TAG, "removeTask 0002");
            this.mDchaService.removeTask(null);
            return true;
        } catch (RemoteException e) {
            Logger.d(TAG, "removeTask 0003");
            Logger.d(TAG, "removeTask", e);
            return false;
        }
    }

    protected boolean setDefaultParam() {
        try {
            Logger.d(TAG, "setDefaultParam 0001");
            if (this.mDchaService == null) {
                return false;
            }
            Logger.d(TAG, "setDefaultParam 0002");
            this.mDchaService.setDefaultParam();
            return true;
        } catch (RemoteException e) {
            Logger.d(TAG, "setDefaultParam 0003");
            Logger.d(TAG, "setDefaultParam", e);
            return false;
        }
    }

    protected int getUserCount() {
        try {
            Logger.d(TAG, "getUserCount 0001");
            if (this.mDchaService == null) {
                return 0;
            }
            Logger.d(TAG, "getUserCount 0002");
            return this.mDchaService.getUserCount();
        } catch (RemoteException e) {
            Logger.d(TAG, "getUserCount 0003");
            Logger.d(TAG, "getUserCount", e);
            return 0;
        }
    }

    protected boolean isDeviceEncryptionEnabled() {
        try {
            Logger.d(TAG, "isDeviceEncryptionEnabled 0001");
            if (this.mDchaService == null) {
                return false;
            }
            Logger.d(TAG, "isDeviceEncryptionEnabled 0002");
            return this.mDchaService.isDeviceEncryptionEnabled();
        } catch (RemoteException e) {
            Logger.d(TAG, "isDeviceEncryptionEnabled 0003");
            Logger.d(TAG, "isDeviceEncryptionEnabled", e);
            return false;
        }
    }

    protected int getSetupStatus() {
        try {
            Logger.d(TAG, "getSetupStatus 0001");
            if (this.mDchaService == null) {
                return 0;
            }
            Logger.d(TAG, "getSetupStatus 0002");
            return this.mDchaService.getSetupStatus();
        } catch (RemoteException e) {
            Logger.d(TAG, "getSetupStatus 0003");
            Logger.d(TAG, "getSetupStatus", e);
            return 0;
        }
    }

    protected boolean setSetupStatus(int i) {
        try {
            Logger.d(TAG, "setSetupStatus 0001");
            if (this.mDchaService == null) {
                return false;
            }
            Logger.d(TAG, "setSetupStatus 0002");
            this.mDchaService.setSetupStatus(i);
            return true;
        } catch (RemoteException e) {
            Logger.d(TAG, "setSetupStatus 0003");
            Logger.d(TAG, "setSetupStatus", e);
            return false;
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        Logger.d(TAG, "onClick 0001");
        this.mOkBtn.setClickable(false);
        if (getUserCount() != 1) {
            callSystemErrorDialog(getString(R.string.error_code_multi_users));
            return;
        }
        if (isDeviceEncryptionEnabled()) {
            callSystemErrorDialog(getString(R.string.error_code_device_encryption));
            return;
        }
        if (!setDefaultParam()) {
            callSystemErrorDialog(getString(R.string.error_code_initialize));
            return;
        }
        if (isRoot()) {
            callSystemErrorDialog(getString(R.string.error_code_root));
            return;
        }
        if (!removeTask()) {
            callSystemErrorDialog(getString(R.string.error_code_remove_task));
            return;
        }
        if (!setSetupStatus(1)) {
            callSystemErrorDialog(getString(R.string.error_code_set_status));
            return;
        }
        Logger.d(TAG, "onClick 0002");
        startActivity(new Intent(this, (Class<?>) TabletIntroductionSettingActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }
}
