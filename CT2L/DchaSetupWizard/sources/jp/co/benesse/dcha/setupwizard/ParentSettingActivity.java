package jp.co.benesse.dcha.setupwizard;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.TextView;
import jp.co.benesse.dcha.dchaservice.IDchaService;
import jp.co.benesse.dcha.util.DchaUncaughtExceptionHandler;
import jp.co.benesse.dcha.util.Logger;

public abstract class ParentSettingActivity extends Activity {
    public static final String CLASS_SYSTEM_WIFI_SETTING = "jp.co.benesse.dcha.systemsettings.WifiSettingActivity";
    public static final String FIRST_FLG = "first_flg";
    public static final String PACKAGE_ANDROID_LAUNCHER = "com.android.launcher";
    public static final String PACKAGE_SYSTEM_SETTING = "jp.co.benesse.dcha.systemsettings";
    private static final String TAG = "ParentSettingActivity";
    public static final long WAIT_TIME = 750;
    protected ServiceConnection mDchaServiceConnectionParent = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Logger.d(ParentSettingActivity.TAG, "onServiceConnected 0001");
            ParentSettingActivity.this.mDchaServiceParent = IDchaService.Stub.asInterface(service);
            ParentSettingActivity.this.hideNavigationBar(true);
            Logger.d(ParentSettingActivity.TAG, "onServiceConnected 0002");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Logger.d(ParentSettingActivity.TAG, "onServiceDisconnected 0001");
            ParentSettingActivity.this.mDchaServiceParent = null;
            Logger.d(ParentSettingActivity.TAG, "onServiceDisconnected 0002");
        }
    };
    protected IDchaService mDchaServiceParent;
    protected boolean mIsShowErrorDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.d(TAG, "onCreate 0001");
        super.onCreate(savedInstanceState);
        this.mIsShowErrorDialog = false;
        getWindow().addFlags(128);
        Thread.setDefaultUncaughtExceptionHandler(new DchaUncaughtExceptionHandler(this));
        Intent intent = new Intent("jp.co.benesse.dcha.dchaservice.DchaService");
        intent.setPackage("jp.co.benesse.dcha.dchaservice");
        bindService(intent, this.mDchaServiceConnectionParent, 1);
        Logger.d(TAG, "onCreate 0002");
    }

    @Override
    protected void onStart() {
        Logger.d(TAG, "onStart 0001");
        super.onStart();
        Logger.d(TAG, "onStart 0002");
    }

    @Override
    protected void onStop() {
        Logger.d(TAG, "onStop 0001");
        super.onStop();
        if (!isApplicationForeground()) {
            Logger.d(TAG, "onStop 0002");
            hideNavigationBar(false);
            doCancelDigicharize();
        }
        Logger.d(TAG, "onStop 0003");
    }

    @Override
    protected void onDestroy() {
        Logger.d(TAG, "onDestroy 0001");
        super.onDestroy();
        if (this.mDchaServiceConnectionParent != null) {
            Logger.d(TAG, "onDestroy 0002");
            unbindService(this.mDchaServiceConnectionParent);
            this.mDchaServiceConnectionParent = null;
            this.mDchaServiceParent = null;
        }
        Logger.d(TAG, "onDestroy 0003");
    }

    protected void moveIntroductionSettingActivity() {
        Logger.d(TAG, "moveIntroductionSettingActivity 0001");
        try {
            Logger.d(TAG, "moveIntroductionSettingActivity 0002");
            Intent intent = new Intent(this, (Class<?>) IntroductionSettingActivity.class);
            startActivity(intent);
            Logger.d(TAG, "moveIntroductionSettingActivity 0003");
        } catch (ActivityNotFoundException e) {
            Logger.d(TAG, "moveIntroductionSettingActivity 0004");
            Logger.d(TAG, "moveIntroductionSettingActivity", e);
            finish();
        }
        Logger.d(TAG, "moveIntroductionSettingActivity 0005");
    }

    public boolean isApplicationForeground() {
        Logger.d(TAG, "isApplicationForeground 0001");
        boolean ret = false;
        String thisPackageName = getApplicationInfo().packageName;
        Logger.d(TAG, "thisPackageName :" + thisPackageName);
        String foregroundPackageName = getForegroundPackageName();
        Logger.d(TAG, "foregroundPackageName :" + foregroundPackageName);
        if (foregroundPackageName != null && foregroundPackageName.equals(thisPackageName)) {
            Logger.d(TAG, "isApplicationForeground 0002");
            ret = true;
        }
        Logger.d(TAG, "isApplicationForeground 0003");
        return ret;
    }

    protected void doCancelDigicharize() {
        Logger.d(TAG, "doCancelDigicharize 0001");
        try {
            Logger.d(TAG, "doCancelDigicharize 0002");
            if (this.mDchaServiceParent != null) {
                Logger.d(TAG, "doCancelDigicharize 0003");
                this.mDchaServiceParent.cancelSetup();
            }
        } catch (RemoteException e) {
            Logger.d(TAG, "doCancelDigicharize 0004");
            Logger.d(TAG, "doCancelDigicharize", e);
        }
        Logger.d(TAG, "doCancelDigicharize 0005");
    }

    protected void setFont(TextView view) {
        Logger.d(TAG, "setFont 0001");
        Typeface typeFace = Typeface.createFromFile("system/fonts/gjsgm.ttf");
        view.setTypeface(typeFace);
        Logger.d(TAG, "setFont 0002");
    }

    protected void callWifiErrorDialog() {
        Logger.d(TAG, "callWifiErrorDialog 0001");
        if (this.mIsShowErrorDialog) {
            Logger.d(TAG, "callWifiErrorDialog 0002");
            return;
        }
        FragmentManager manager = getFragmentManager();
        DchaDialog dialog = new DchaDialog(this, 1);
        dialog.show(manager, "dialog");
        this.mIsShowErrorDialog = true;
        Logger.d(TAG, "callWifiErrorDialog 0004");
    }

    protected void callSystemErrorDialog(String msg) {
        Logger.d(TAG, "callSystemErrorDialog 0001");
        if (this.mIsShowErrorDialog) {
            Logger.d(TAG, "callSystemErrorDialog 0002");
            return;
        }
        FragmentManager manager = getFragmentManager();
        DchaDialog dialog = new DchaDialog(this, 2, msg);
        dialog.show(manager, "dialog");
        this.mIsShowErrorDialog = true;
        Logger.d(TAG, "callSystemErrorDialog 0004");
    }

    protected void callNetworkErrorDialog() {
        Logger.d(TAG, "callNetworkErrorDialog 0001");
        if (this.mIsShowErrorDialog) {
            Logger.d(TAG, "callNetworkErrorDialog 0002");
            return;
        }
        FragmentManager manager = getFragmentManager();
        DchaDialog dialog = new DchaDialog(this, 3);
        dialog.show(manager, "dialog");
        this.mIsShowErrorDialog = true;
        Logger.d(TAG, "callNetworkErrorDialog 0004");
    }

    protected int getSetupStatus() {
        try {
            Logger.d(TAG, "getSetupStatus 0001");
            int status = this.mDchaServiceParent.getSetupStatus();
            return status;
        } catch (RemoteException e) {
            Logger.d(TAG, "getSetupStatus 0002");
            Logger.d(TAG, "getSetupStatus", e);
            return 0;
        }
    }

    protected boolean setSetupStatus(int status) {
        try {
            Logger.d(TAG, "setSetupStatus 0001");
            this.mDchaServiceParent.setSetupStatus(status);
            return true;
        } catch (RemoteException e) {
            Logger.d(TAG, "setSetupStatus 0002");
            Logger.d(TAG, "setSetupStatus", e);
            return false;
        }
    }

    protected void hideNavigationBar(boolean hide) {
        try {
            Logger.d(TAG, "hideNavigationBar 0001");
            if (this.mDchaServiceParent != null) {
                Logger.d(TAG, "hideNavigationBar 0002");
                this.mDchaServiceParent.hideNavigationBar(hide);
            }
        } catch (RemoteException e) {
            Logger.d(TAG, "hideNavigationBar 0003");
            Logger.d(TAG, "RemoteException", e);
        }
    }

    protected String getForegroundPackageName() {
        try {
            Logger.d(TAG, "getForegroundPackageName 0001");
            if (this.mDchaServiceParent == null) {
                return null;
            }
            Logger.d(TAG, "getForegroundPackageName 0002");
            String packageName = this.mDchaServiceParent.getForegroundPackageName();
            return packageName;
        } catch (RemoteException e) {
            Logger.d(TAG, "getForegroundPackageName 0003");
            Logger.d(TAG, "getForegroundPackageName", e);
            return null;
        }
    }
}
