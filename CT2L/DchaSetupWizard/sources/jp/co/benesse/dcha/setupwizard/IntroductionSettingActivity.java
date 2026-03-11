package jp.co.benesse.dcha.setupwizard;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.ImageView;
import jp.co.benesse.dcha.dchaservice.IDchaService;
import jp.co.benesse.dcha.util.Logger;

public class IntroductionSettingActivity extends ParentSettingActivity implements View.OnClickListener {
    public static final int DIGICHALIZE_STATUS_DIGICHALIZED = 3;
    public static final int DIGICHALIZE_STATUS_DIGICHARIZING = 1;
    public static final int DIGICHALIZE_STATUS_DIGICHARIZING_DL_COMPLETE = 2;
    public static final int DIGICHALIZE_STATUS_UNDIGICHALIZE = 0;
    public static final int REQUIRED_BATTERY_AMOUNT = 50;
    private static final String TAG = IntroductionSettingActivity.class.getSimpleName();
    protected Handler mHandler = new Handler();
    protected ImageView mOkBtn = null;
    protected IDchaService mDchaService = null;
    protected ServiceConnection mDchaServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Logger.d(IntroductionSettingActivity.TAG, "onServiceConnected 0001");
            IntroductionSettingActivity.this.mDchaService = IDchaService.Stub.asInterface(service);
            int status = IntroductionSettingActivity.this.getSetupStatus();
            if (status == 1 || status == 2) {
                IntroductionSettingActivity.this.doCancelDigicharize();
            }
            IntroductionSettingActivity.this.setDefaultParam();
            IntroductionSettingActivity.this.hideNavigationBar(true);
            if (IntroductionSettingActivity.this.mOkBtn != null) {
                IntroductionSettingActivity.this.mOkBtn.setClickable(true);
            }
            Logger.d(IntroductionSettingActivity.TAG, "onServiceConnected 0002");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Logger.d(IntroductionSettingActivity.TAG, "onServiceDisconnected 0001");
            IntroductionSettingActivity.this.mDchaService = null;
            Logger.d(IntroductionSettingActivity.TAG, "onServiceDisconnected 0002");
        }
    };
    public BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.d(IntroductionSettingActivity.TAG, "onReceive 0001");
            int level = intent.getIntExtra("level", -1);
            int plugged = intent.getIntExtra("plugged", -1);
            if (level < 50 || (plugged != 1 && plugged != 2)) {
                Logger.d(IntroductionSettingActivity.TAG, "onReceive 0003");
                IntroductionSettingActivity.this.mOkBtn.setEnabled(false);
            } else {
                Logger.d(IntroductionSettingActivity.TAG, "onReceive 0002");
                IntroductionSettingActivity.this.mOkBtn.setEnabled(true);
            }
            Logger.d(IntroductionSettingActivity.TAG, "onReceive 0004");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.d(TAG, "onCreate 0001");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_introduction);
        this.mOkBtn = (ImageView) findViewById(R.id.ok_btn);
        this.mOkBtn.setClickable(false);
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (IntroductionSettingActivity.this.mOkBtn != null) {
                    IntroductionSettingActivity.this.mOkBtn.setOnClickListener(IntroductionSettingActivity.this);
                }
            }
        }, 750L);
        Intent intent = new Intent("jp.co.benesse.dcha.dchaservice.DchaService");
        intent.setPackage("jp.co.benesse.dcha.dchaservice");
        bindService(intent, this.mDchaServiceConnection, 1);
        Logger.d(TAG, "onCreate 0002");
    }

    @Override
    protected void onStart() {
        Logger.d(TAG, "onStart 0001");
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        registerReceiver(this.mBatteryReceiver, intentFilter);
        Logger.d(TAG, "onStart 0002");
    }

    @Override
    protected void onStop() {
        Logger.d(TAG, "onStop 0001");
        super.onStop();
        if (this.mBatteryReceiver != null) {
            Logger.d(TAG, "onStop 0002");
            unregisterReceiver(this.mBatteryReceiver);
        }
        Logger.d(TAG, "onStop 0003");
    }

    @Override
    protected void onDestroy() {
        Logger.d(TAG, "onDestroy 0001");
        super.onDestroy();
        this.mOkBtn.setClickable(false);
        this.mOkBtn.setOnClickListener(null);
        this.mHandler = null;
        this.mOkBtn = null;
        this.mBatteryReceiver = null;
        if (this.mDchaServiceConnection != null) {
            Logger.d(TAG, "onDestroy 0002");
            unbindService(this.mDchaServiceConnection);
            this.mDchaServiceConnection = null;
            this.mDchaService = null;
        }
        Logger.d(TAG, "onDestroy 0003");
    }

    protected boolean isRoot() {
        Logger.d(TAG, "isRoot 0001");
        try {
            Logger.d(TAG, "isRoot 0002");
            return this.mDchaService.checkPadRooted();
        } catch (RemoteException e) {
            Logger.d(TAG, "isRoot 0003");
            Logger.d(TAG, "isRoot", e);
            return false;
        }
    }

    protected boolean removeTask() {
        Logger.d(TAG, "removeTask 0001");
        try {
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
        Logger.d(TAG, "setDefaultParam 0001");
        try {
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
        Logger.d(TAG, "getUserCount 0001");
        try {
            Logger.d(TAG, "getUserCount 0002");
            return this.mDchaService.getUserCount();
        } catch (RemoteException e) {
            Logger.d(TAG, "getUserCount 0003");
            Logger.d(TAG, "getUserCount", e);
            return 0;
        }
    }

    protected boolean isDeviceEncryptionEnabled() {
        Logger.d(TAG, "isDeviceEncryptionEnabled 0001");
        try {
            Logger.d(TAG, "isDeviceEncryptionEnabled 0002");
            return this.mDchaService.isDeviceEncryptionEnabled();
        } catch (RemoteException e) {
            Logger.d(TAG, "isDeviceEncryptionEnabled 0003");
            Logger.d(TAG, "isDeviceEncryptionEnabled", e);
            return false;
        }
    }

    @Override
    public void onClick(View v) {
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
        Intent intent = new Intent(this, (Class<?>) TabletIntroductionSettingActivity.class);
        startActivity(intent);
        finish();
    }
}
