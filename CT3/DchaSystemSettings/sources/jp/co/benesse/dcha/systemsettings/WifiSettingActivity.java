package jp.co.benesse.dcha.systemsettings;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.ImageView;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import jp.co.benesse.dcha.dchaservice.IDchaService;
import jp.co.benesse.dcha.util.Logger;

public class WifiSettingActivity extends ParentSettingActivity implements View.OnClickListener {
    private ImageView mBackBtn;
    private ImageView mConnectBtn;
    private IDchaService mDchaService;
    private WpsDialog mDialog;
    private ImageView mInternetBtn;
    private ImageView mNearestNetBtn;
    private ImageView mNextBtn;
    private ImageView mPankuzu;
    private ImageView mWpsPinBtn;
    private ImageView mWpsPushBtn;
    private Timer timer;
    private Handler mHandler = new Handler();
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Logger.d("WiFiSettingActivity", "onServiceConnected 0001");
            WifiSettingActivity.this.mDchaService = IDchaService.Stub.asInterface(service);
            WifiSettingActivity.this.hideNavigationBar(true);
            Logger.d("WiFiSettingActivity", "onServiceConnected 0002");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Logger.d("WiFiSettingActivity", "onServiceDisconnected 0001");
            WifiSettingActivity.this.mDchaService = null;
            Logger.d("WiFiSettingActivity", "onServiceDisconnected 0002");
        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.d("WiFiSettingActivity", "onReceive 0001");
            String action = intent.getAction();
            if (action.equals("jp.co.benesse.dcha.allgrade.b001.ACTION_ACTIVATE")) {
                try {
                    Logger.d("WiFiSettingActivity", "onReceive 0002");
                    WifiSettingActivity.this.mDchaService.removeTask("jp.co.benesse.dcha.setupwizard");
                    WifiSettingActivity.this.mDchaService.removeTask("jp.co.benesse.dcha.allgrade.usersetting");
                    Logger.d("WiFiSettingActivity", "onReceive 0003");
                } catch (RemoteException e) {
                    Logger.d("WiFiSettingActivity", "onReceive 0004");
                    Logger.e("RemoteException", e);
                }
            } else if (action.equals("android.net.wifi.STATE_CHANGE")) {
                Logger.d("WiFiSettingActivity", "onReceive 0005");
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                if (WifiSettingActivity.this.mNextBtn != null) {
                    Logger.d("WiFiSettingActivity", "onReceive 0006");
                    if (info.isConnected()) {
                        Logger.d("WiFiSettingActivity", "onReceive 0007");
                        WifiSettingActivity.this.mNextBtn.setEnabled(true);
                        WifiSettingActivity.this.mConnectBtn.setVisibility(0);
                    } else {
                        Logger.d("WiFiSettingActivity", "onReceive 0008");
                        WifiSettingActivity.this.mNextBtn.setEnabled(false);
                        WifiSettingActivity.this.mConnectBtn.setVisibility(4);
                    }
                }
            }
            Logger.d("WiFiSettingActivity", "onReceive 0009");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.d("WiFiSettingActivity", "onCreate 0001");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_wifi);
        this.mIsFirstFlow = getFirstFlg();
        Intent intent = new Intent("jp.co.benesse.dcha.dchaservice.DchaService");
        intent.setPackage("jp.co.benesse.dcha.dchaservice");
        bindService(intent, this.mServiceConnection, 1);
        this.mNearestNetBtn = (ImageView) findViewById(R.id.nearestnet_btn);
        this.mWpsPushBtn = (ImageView) findViewById(R.id.wpspush_btn);
        this.mWpsPinBtn = (ImageView) findViewById(R.id.wpspin_btn);
        this.mConnectBtn = (ImageView) findViewById(R.id.connect_btn);
        this.mNextBtn = (ImageView) findViewById(R.id.next_btn);
        this.mBackBtn = (ImageView) findViewById(R.id.back_btn);
        this.mInternetBtn = (ImageView) findViewById(R.id.internet_btn);
        this.mPankuzu = (ImageView) findViewById(R.id.pankuzu);
        this.mNextBtn.setEnabled(false);
        this.mConnectBtn.setVisibility(4);
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (WifiSettingActivity.this.mNearestNetBtn != null) {
                    Logger.d("WiFiSettingActivity", "onCreate 0005");
                    WifiSettingActivity.this.mNearestNetBtn.setOnClickListener(WifiSettingActivity.this);
                }
                if (WifiSettingActivity.this.mWpsPushBtn != null) {
                    Logger.d("WiFiSettingActivity", "onCreate 0008");
                    WifiSettingActivity.this.mWpsPushBtn.setOnClickListener(WifiSettingActivity.this);
                }
                if (WifiSettingActivity.this.mWpsPinBtn != null) {
                    Logger.d("WiFiSettingActivity", "onCreate 0009");
                    WifiSettingActivity.this.mWpsPinBtn.setOnClickListener(WifiSettingActivity.this);
                }
                if (WifiSettingActivity.this.mNextBtn != null) {
                    Logger.d("WiFiSettingActivity", "onCreate 0010");
                    WifiSettingActivity.this.mNextBtn.setOnClickListener(WifiSettingActivity.this);
                }
                if (WifiSettingActivity.this.mBackBtn != null) {
                    Logger.d("WiFiSettingActivity", "onCreate 0011");
                    WifiSettingActivity.this.mBackBtn.setOnClickListener(WifiSettingActivity.this);
                }
                if (WifiSettingActivity.this.mInternetBtn == null) {
                    return;
                }
                Logger.d("WiFiSettingActivity", "onCreate 0012");
                WifiSettingActivity.this.mInternetBtn.setOnClickListener(WifiSettingActivity.this);
            }
        }, 750L);
        if (this.mIsFirstFlow) {
            Logger.d("WiFiSettingActivity", "onCreate 0002");
            this.mBackBtn.setVisibility(8);
            this.mInternetBtn.setVisibility(8);
            getWindow().addFlags(128);
        } else {
            Logger.d("WiFiSettingActivity", "onCreate 0003");
            this.mNextBtn.setVisibility(8);
            this.mPankuzu.setVisibility(4);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        intentFilter.addAction("jp.co.benesse.dcha.allgrade.b001.ACTION_ACTIVATE");
        registerReceiver(this.mReceiver, intentFilter);
        Logger.d("WiFiSettingActivity", "onCreate 0004");
    }

    @Override
    protected void onStart() {
        Logger.d("WiFiSettingActivity", "onStart 0001");
        super.onStart();
        changeBtnClickable(true);
        if (this.timer != null) {
            Logger.d("WiFiSettingActivity", "onStart 0002");
            this.timer.cancel();
            this.timer = null;
        }
        Logger.d("WiFiSettingActivity", "onStart 0003");
    }

    @Override
    protected void onResume() {
        Logger.d("WiFiSettingActivity", "onResume 0001");
        super.onResume();
        hideNavigationBar(true);
        changeBtnClickable(true);
        Logger.d("WiFiSettingActivity", "onResume 0002");
    }

    @Override
    protected void onStop() {
        Logger.d("WiFiSettingActivity", "onStop 0001");
        super.onStop();
        if (this.mDialog != null) {
            Logger.d("WiFiSettingActivity", "onStop 0002");
            this.mDialog.dismiss();
            this.mDialog = null;
        }
        if (this.mIsFirstFlow) {
            Logger.d("WiFiSettingActivity", "onStop 0003");
            startTimer();
        }
        Logger.d("WiFiSettingActivity", "onStop 0004");
    }

    @Override
    protected void onDestroy() {
        Logger.d("WiFiSettingActivity", "onDestroy 0001");
        super.onDestroy();
        if (getSetupStatus() == 3) {
            hideNavigationBar(true);
        }
        this.mNearestNetBtn.setOnClickListener(null);
        this.mWpsPushBtn.setOnClickListener(null);
        this.mWpsPinBtn.setOnClickListener(null);
        this.mInternetBtn.setOnClickListener(null);
        if (this.mServiceConnection != null) {
            Logger.d("WiFiSettingActivity", "onDestroy 0002");
            unbindService(this.mServiceConnection);
            this.mServiceConnection = null;
            this.mDchaService = null;
        }
        if (this.mReceiver != null) {
            Logger.d("WiFiSettingActivity", "onDestroy 0003");
            unregisterReceiver(this.mReceiver);
        }
        this.mNearestNetBtn = null;
        this.mWpsPushBtn = null;
        this.mWpsPinBtn = null;
        this.mConnectBtn = null;
        this.mNextBtn = null;
        this.mBackBtn = null;
        this.mInternetBtn = null;
        this.mPankuzu = null;
        this.mReceiver = null;
        this.mHandler = null;
        if (this.timer != null) {
            Logger.d("WiFiSettingActivity", "onDestroy 0004");
            this.timer.cancel();
            this.timer = null;
        }
        Logger.d("WiFiSettingActivity", "onDestroy 0005");
    }

    @Override
    public void onClick(View v) {
        Logger.d("WiFiSettingActivity", "onClick 0001");
        if (v.getId() == this.mNearestNetBtn.getId()) {
            Logger.d("WiFiSettingActivity", "onClick 0002");
            changeBtnClickable(false);
            moveNetworkSettingActivity();
        } else if (v.getId() == this.mNextBtn.getId()) {
            Logger.d("WiFiSettingActivity", "onClick 0005");
            changeBtnClickable(false);
            moveHealthCheckActivity();
        } else if (v.getId() == this.mBackBtn.getId()) {
            Logger.d("WiFiSettingActivity", "onClick 0006");
            changeBtnClickable(false);
            moveSettingActivity();
            finish();
        } else if (v.getId() == this.mWpsPushBtn.getId()) {
            Logger.d("WiFiSettingActivity", "onClick 0007");
            this.mDialog = new WpsDialog(this, 0);
            this.mDialog.show();
        } else if (v.getId() == this.mWpsPinBtn.getId()) {
            Logger.d("WiFiSettingActivity", "onClick 0008");
            this.mDialog = new WpsDialog(this, 1);
            this.mDialog.show();
        } else if (v.getId() == this.mInternetBtn.getId()) {
            Logger.d("WiFiSettingActivity", "onClick 0009");
            changeBtnClickable(false);
            moveHealthCheckActivity();
        }
        Logger.d("WiFiSettingActivity", "onClick 0010");
    }

    private void changeBtnClickable(boolean clickable) {
        Logger.d("WiFiSettingActivity", "changeBtnClickable 0001");
        if (clickable) {
            Logger.d("WiFiSettingActivity", "changeBtnClickable 0002");
            this.mNearestNetBtn.setClickable(true);
            this.mNextBtn.setClickable(true);
            this.mBackBtn.setClickable(true);
            this.mWpsPushBtn.setClickable(true);
            this.mWpsPinBtn.setClickable(true);
            this.mInternetBtn.setClickable(true);
            return;
        }
        Logger.d("WiFiSettingActivity", "changeBtnClickable 0003");
        this.mNearestNetBtn.setClickable(false);
        this.mNextBtn.setClickable(false);
        this.mBackBtn.setClickable(false);
        this.mWpsPushBtn.setClickable(false);
        this.mWpsPinBtn.setClickable(false);
        this.mInternetBtn.setClickable(false);
    }

    private void moveHealthCheckActivity() {
        Logger.d("WiFiSettingActivity", "moveHealthCheckActivity 0001");
        Intent intent = new Intent();
        if (this.mIsFirstFlow) {
            intent.setClassName("jp.co.benesse.dcha.systemsettings", "jp.co.benesse.dcha.systemsettings.HealthCheckActivity");
            Logger.d("WiFiSettingActivity", "moveHealthCheckActivity 0002");
        } else {
            intent.setClassName("jp.co.benesse.dcha.allgrade.usersetting", "jp.co.benesse.dcha.allgrade.usersetting.activity.HealthCheckActivity");
            Logger.d("WiFiSettingActivity", "moveHealthCheckActivity 0003");
        }
        intent.putExtra("first_flg", this.mIsFirstFlow);
        startActivity(intent);
        finish();
        Logger.d("WiFiSettingActivity", "moveHealthCheckActivity 0004");
    }

    private void moveNetworkSettingActivity() {
        Logger.d("WiFiSettingActivity", "moveNetworkSettingActivity 0001");
        Intent intent = new Intent(this, (Class<?>) NetworkSettingActivity.class);
        intent.putExtra("first_flg", this.mIsFirstFlow);
        startActivity(intent);
        finish();
        Logger.d("WiFiSettingActivity", "moveNetworkSettingActivity 0002");
    }

    private void startTimer() {
        Logger.d("WiFiSettingActivity", "startTimer 0001");
        this.timer = new Timer(true);
        final Handler handler = new Handler();
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Logger.d("WiFiSettingActivity", "run 0001");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Logger.d("WiFiSettingActivity", "run 0002");
                        ActivityManager manager = (ActivityManager) WifiSettingActivity.this.getSystemService("activity");
                        List<ActivityManager.RunningTaskInfo> appInfo = manager.getRunningTasks(1);
                        String foregroundTaskName = appInfo.get(0).baseActivity.getPackageName();
                        Logger.i("WiFiSettingActivity", "run foregroundTaskName = " + foregroundTaskName);
                        if (foregroundTaskName.equals("com.android.launcher")) {
                            Logger.d("WiFiSettingActivity", "run 0003");
                            try {
                                Logger.d("WiFiSettingActivity", "run 0004");
                                if (WifiSettingActivity.this.mDchaService != null) {
                                    Logger.d("WiFiSettingActivity", "run 0005");
                                    WifiSettingActivity.this.mDchaService.removeTask("jp.co.benesse.dcha.setupwizard");
                                }
                                Logger.d("WiFiSettingActivity", "run 0006");
                            } catch (RemoteException e) {
                                Logger.d("WiFiSettingActivity", "run 0007");
                                Logger.e("WiFiSettingActivity", "RemoteException", e);
                            }
                            WifiSettingActivity.this.doCancelDigicharize();
                            if (WifiSettingActivity.this.timer != null) {
                                WifiSettingActivity.this.timer.cancel();
                                WifiSettingActivity.this.timer = null;
                            }
                        }
                        Logger.d("WiFiSettingActivity", "run 0008");
                    }
                });
                Logger.d("WiFiSettingActivity", "run 0009");
            }
        }, 100L, 100L);
        Logger.d("WiFiSettingActivity", "startTimer 0002");
    }

    private int getSetupStatus() {
        Logger.d("WiFiSettingActivity", "getSetupStatus 0001");
        int status = -1;
        try {
            Logger.d("WiFiSettingActivity", "getSetupStatus 0002");
            if (this.mDchaService != null) {
                Logger.d("WiFiSettingActivity", "getSetupStatus 0003");
                status = this.mDchaService.getSetupStatus();
            }
        } catch (RemoteException e) {
            Logger.d("WiFiSettingActivity", "getSetupStatus 0004");
            Logger.e("WiFiSettingActivity", "getSetupStatus", e);
        }
        Logger.d("WiFiSettingActivity", "getSetupStatus 0005");
        return status;
    }
}
