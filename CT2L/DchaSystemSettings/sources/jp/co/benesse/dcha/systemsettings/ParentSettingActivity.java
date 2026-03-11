package jp.co.benesse.dcha.systemsettings;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.TextView;
import java.util.List;
import jp.co.benesse.dcha.dchaservice.IDchaService;
import jp.co.benesse.dcha.util.DchaUncaughtExceptionHandler;
import jp.co.benesse.dcha.util.Logger;

public class ParentSettingActivity extends Activity {
    private IDchaService mDchaService;
    private ServiceConnection mDchaServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Logger.d("ParentSettingActivity", "onServiceConnected 0001");
            ParentSettingActivity.this.mDchaService = IDchaService.Stub.asInterface(service);
            Logger.d("ParentSettingActivity", "onServiceConnected 0002");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Logger.d("ParentSettingActivity", "onServiceDisconnected 0001");
            ParentSettingActivity.this.mDchaService = null;
            Logger.d("ParentSettingActivity", "onServiceDisconnected 0002");
        }
    };
    protected boolean mIsFirstFlow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.d("ParentSettingActivity", "onCreate 0001");
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new DchaUncaughtExceptionHandler(this));
        Intent intent = new Intent("jp.co.benesse.dcha.dchaservice.DchaService");
        intent.setPackage("jp.co.benesse.dcha.dchaservice");
        bindService(intent, this.mDchaServiceConnection, 1);
        Logger.d("ParentSettingActivity", "onCreate 0002");
    }

    @Override
    protected void onStart() {
        Logger.d("ParentSettingActivity", "onStart 0001");
        super.onStart();
        Logger.d("ParentSettingActivity", "onStart 0002");
    }

    @Override
    protected void onStop() {
        Logger.d("ParentSettingActivity", "onStop 0001");
        super.onStop();
        if (!isApplicationForeground() && this.mIsFirstFlow) {
            Logger.d("ParentSettingActivity", "onStop 0002");
            hideNavigationBar(false);
            doCancelDigicharize();
        }
        Logger.d("ParentSettingActivity", "onStop 0003");
    }

    @Override
    protected void onDestroy() {
        Logger.d("ParentSettingActivity", "onDestroy 0001");
        super.onDestroy();
        if (this.mDchaServiceConnection != null) {
            Logger.d("ParentSettingActivity", "onDestroy 0002");
            unbindService(this.mDchaServiceConnection);
            this.mDchaServiceConnection = null;
            this.mDchaService = null;
        }
        Logger.d("ParentSettingActivity", "onDestroy 0003");
    }

    protected void moveSettingActivity() {
        Logger.d("ParentSettingActivity", "moveSettingActivity 0001");
        try {
            Logger.d("ParentSettingActivity", "moveSettingActivity 0002");
            Intent intent = new Intent();
            intent.setClassName("jp.co.benesse.dcha.allgrade.usersetting", "jp.co.benesse.dcha.allgrade.usersetting.activity.SettingMenuActivity");
            startActivity(intent);
            Logger.d("ParentSettingActivity", "moveSettingActivity 0003");
        } catch (ActivityNotFoundException e) {
            Logger.d("ParentSettingActivity", "moveSettingActivity 0004");
            Logger.d("ParentSettingActivity", "moveSettingActivity", e);
            finish();
        }
        Logger.d("ParentSettingActivity", "moveSettingActivity 0005");
    }

    protected boolean getFirstFlg() {
        Logger.d("ParentSettingActivity", "getFirstFlg 0001");
        Intent intent = getIntent();
        boolean isFirstFlow = intent.getBooleanExtra("first_flg", false);
        Logger.d("ParentSettingActivity", "getFirstFlg 0002");
        return isFirstFlow;
    }

    public boolean isApplicationForeground() {
        Logger.d("ParentSettingActivity", "isApplicationForeground 0001");
        boolean ret = false;
        String thisPackageName = getApplicationInfo().packageName;
        Logger.i("ParentSettingActivity", "thisPackageName :" + thisPackageName);
        ActivityManager manager = (ActivityManager) getSystemService("activity");
        try {
            Logger.d("ParentSettingActivity", "isApplicationForeground 0002");
            List<ActivityManager.RunningTaskInfo> appInfo = manager.getRunningTasks(1);
            String foregroundTaskName = appInfo.get(0).baseActivity.getPackageName();
            Logger.i("ParentSettingActivity", "foregroundTaskName :" + foregroundTaskName);
            appInfo.clear();
            if (foregroundTaskName.equals(thisPackageName) || foregroundTaskName.equals("jp.buffalo.aoss")) {
                Logger.d("ParentSettingActivity", "isApplicationForeground 0003");
                ret = true;
            }
            Logger.d("ParentSettingActivity", "isApplicationForeground 0004");
            Logger.d("ParentSettingActivity", "isApplicationForeground 0006");
            return ret;
        } catch (SecurityException e) {
            Logger.d("ParentSettingActivity", "isApplicationForeground 0005");
            Logger.d("ParentSettingActivity", "isApplicationForeground", e);
            return false;
        }
    }

    public void doCancelDigicharize() {
        Logger.d("ParentSettingActivity", "doCancelDigicharize 0001");
        try {
            Logger.d("ParentSettingActivity", "doCancelDigicharize 0002");
            this.mDchaService.cancelSetup();
            Logger.d("ParentSettingActivity", "doCancelDigicharize 0003");
        } catch (RemoteException e) {
            Logger.d("ParentSettingActivity", "doCancelDigicharize 0004");
            Logger.d("ParentSettingActivity", "doCancelDigicharize", e);
        }
        Logger.d("ParentSettingActivity", "doCancelDigicharize 0005");
    }

    protected void hideNavigationBar(boolean hide) {
        Logger.d("ParentSettingActivity", "hideNavigationBar 0001");
        try {
            Logger.d("ParentSettingActivity", "hideNavigationBar 0002");
            if (this.mDchaService != null) {
                Logger.d("ParentSettingActivity", "hideNavigationBar 0003");
                this.mDchaService.hideNavigationBar(hide);
            }
        } catch (RemoteException e) {
            Logger.d("ParentSettingActivity", "hideNavigationBar 0004");
            Logger.d("ParentSettingActivity", "RemoteException", e);
        }
        Logger.d("ParentSettingActivity", "hideNavigationBar 0005");
    }

    protected void setFont(TextView view) {
        Logger.d("ParentSettingActivity", "setFont 0001");
        Typeface typeFace = Typeface.createFromFile("system/fonts/gjsgm.ttf");
        view.setTypeface(typeFace);
        Logger.d("ParentSettingActivity", "setFont 0002");
    }
}
