package jp.co.benesse.dcha.setupwizard;

import android.app.Activity;
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
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Logger.d(ParentSettingActivity.TAG, "onServiceConnected 0001");
            ParentSettingActivity.this.mDchaServiceParent = IDchaService.Stub.asInterface(iBinder);
            ParentSettingActivity parentSettingActivity = ParentSettingActivity.this;
            parentSettingActivity.hideNavigationBar(parentSettingActivity.mDchaServiceParent, true);
            Logger.d(ParentSettingActivity.TAG, "onServiceConnected 0002");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Logger.d(ParentSettingActivity.TAG, "onServiceDisconnected 0001");
            ParentSettingActivity.this.mDchaServiceParent = null;
            Logger.d(ParentSettingActivity.TAG, "onServiceDisconnected 0002");
        }
    };
    protected IDchaService mDchaServiceParent;
    protected boolean mIsShowErrorDialog;

    @Override
    protected void onCreate(Bundle bundle) {
        Logger.d(TAG, "onCreate 0001");
        super.onCreate(bundle);
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
            hideNavigationBar(this.mDchaServiceParent, false);
            doCancelDigicharize(this.mDchaServiceParent);
            finish();
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
            startActivity(new Intent(this, (Class<?>) IntroductionSettingActivity.class));
            overridePendingTransition(0, 0);
            Logger.d(TAG, "moveIntroductionSettingActivity 0003");
        } catch (ActivityNotFoundException e) {
            Logger.d(TAG, "moveIntroductionSettingActivity 0004");
            Logger.d(TAG, "moveIntroductionSettingActivity", e);
            finish();
        }
        Logger.d(TAG, "moveIntroductionSettingActivity 0005");
    }

    public boolean isApplicationForeground() {
        boolean z;
        Logger.d(TAG, "isApplicationForeground 0001");
        String foregroundPackageName = getForegroundPackageName(this.mDchaServiceParent);
        Logger.d(TAG, "foregroundPackageName :" + foregroundPackageName);
        if (foregroundPackageName == null || !foregroundPackageName.startsWith("jp.co.benesse")) {
            z = false;
        } else {
            Logger.d(TAG, "isApplicationForeground 0002");
            z = true;
        }
        Logger.d(TAG, "isApplicationForeground 0003");
        return z;
    }

    protected void doCancelDigicharize(IDchaService iDchaService) {
        Logger.d(TAG, "doCancelDigicharize 0001");
        try {
            Logger.d(TAG, "doCancelDigicharize 0002");
            if (iDchaService != null) {
                Logger.d(TAG, "doCancelDigicharize 0003");
                iDchaService.cancelSetup();
            }
        } catch (RemoteException e) {
            Logger.d(TAG, "doCancelDigicharize 0004");
            Logger.d(TAG, "doCancelDigicharize", e);
        }
        Logger.d(TAG, "doCancelDigicharize 0005");
    }

    protected void setFont(TextView textView) {
        Logger.d(TAG, "setFont 0001");
        try {
            textView.setTypeface(Typeface.createFromFile("system/fonts/gjsgm.ttf"));
        } catch (Exception e) {
            Logger.d(TAG, "setFont 0002");
            Logger.d(TAG, "setFont", e);
        }
        Logger.d(TAG, "setFont 0003");
    }

    protected void callWifiErrorDialog() {
        Logger.d(TAG, "callWifiErrorDialog 0001");
        if (this.mIsShowErrorDialog) {
            Logger.d(TAG, "callWifiErrorDialog 0002");
            return;
        }
        new DchaDialog(this, 1).show(getFragmentManager(), "dialog");
        this.mIsShowErrorDialog = true;
        Logger.d(TAG, "callWifiErrorDialog 0004");
    }

    protected void callSystemErrorDialog(String str) {
        Logger.d(TAG, "callSystemErrorDialog 0001");
        if (this.mIsShowErrorDialog) {
            Logger.d(TAG, "callSystemErrorDialog 0002");
            return;
        }
        new DchaDialog(this, 2, str).show(getFragmentManager(), "dialog");
        this.mIsShowErrorDialog = true;
        Logger.d(TAG, "callSystemErrorDialog 0004");
    }

    protected void callNetworkErrorDialog() {
        Logger.d(TAG, "callNetworkErrorDialog 0001");
        if (this.mIsShowErrorDialog) {
            Logger.d(TAG, "callNetworkErrorDialog 0002");
            return;
        }
        new DchaDialog(this, 3).show(getFragmentManager(), "dialog");
        this.mIsShowErrorDialog = true;
        Logger.d(TAG, "callNetworkErrorDialog 0004");
    }

    protected void hideNavigationBar(IDchaService iDchaService, boolean z) {
        try {
            Logger.d(TAG, "hideNavigationBar 0001");
            if (iDchaService != null) {
                Logger.d(TAG, "hideNavigationBar 0002");
                iDchaService.hideNavigationBar(z);
            }
        } catch (RemoteException e) {
            Logger.d(TAG, "hideNavigationBar 0003");
            Logger.d(TAG, "RemoteException", e);
        }
    }

    protected String getForegroundPackageName(IDchaService iDchaService) {
        try {
            Logger.d(TAG, "getForegroundPackageName 0001");
            if (iDchaService == null) {
                return null;
            }
            Logger.d(TAG, "getForegroundPackageName 0002");
            return iDchaService.getForegroundPackageName();
        } catch (RemoteException e) {
            Logger.d(TAG, "getForegroundPackageName 0003");
            Logger.d(TAG, "getForegroundPackageName", e);
            return null;
        }
    }
}
