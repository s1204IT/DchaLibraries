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
import java.io.File;
import java.util.List;
import jp.co.benesse.dcha.dchaservice.IDchaService;
import jp.co.benesse.dcha.util.DchaUncaughtExceptionHandler;
import jp.co.benesse.dcha.util.FileUtils;
import jp.co.benesse.dcha.util.Logger;

/* loaded from: classes.dex */
public class ParentSettingActivity extends Activity {
    private IDchaService mDchaService;
    private ServiceConnection mDchaServiceConnection = new ServiceConnection() { // from class: jp.co.benesse.dcha.systemsettings.ParentSettingActivity.1
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Logger.d("ParentSettingActivity", "onServiceConnected 0001");
            ParentSettingActivity.this.mDchaService = IDchaService.Stub.asInterface(iBinder);
            Logger.d("ParentSettingActivity", "onServiceConnected 0002");
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName componentName) {
            Logger.d("ParentSettingActivity", "onServiceDisconnected 0001");
            ParentSettingActivity.this.mDchaService = null;
            Logger.d("ParentSettingActivity", "onServiceDisconnected 0002");
        }
    };
    protected boolean mIsFirstFlow;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        Logger.d("ParentSettingActivity", "onCreate 0001");
        super.onCreate(bundle);
        Thread.setDefaultUncaughtExceptionHandler(new DchaUncaughtExceptionHandler(this));
        Intent intent = new Intent("jp.co.benesse.dcha.dchaservice.DchaService");
        intent.setPackage("jp.co.benesse.dcha.dchaservice");
        bindService(intent, this.mDchaServiceConnection, 1);
        Logger.d("ParentSettingActivity", "onCreate 0002");
    }

    @Override // android.app.Activity
    protected void onStart() {
        Logger.d("ParentSettingActivity", "onStart 0001");
        super.onStart();
        Logger.d("ParentSettingActivity", "onStart 0002");
    }

    @Override // android.app.Activity
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

    @Override // android.app.Activity
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
            overridePendingTransition(0, 0);
            Logger.d("ParentSettingActivity", "moveSettingActivity 0003");
        } catch (ActivityNotFoundException e) {
            Logger.d("ParentSettingActivity", "moveSettingActivity 0004");
            Logger.e("ParentSettingActivity", "ActivityNotFoundException", e);
            finish();
        }
        Logger.d("ParentSettingActivity", "moveSettingActivity 0005");
    }

    protected boolean getFirstFlg() {
        Logger.d("ParentSettingActivity", "getFirstFlg 0001");
        boolean booleanExtra = getIntent().getBooleanExtra("first_flg", false);
        Logger.d("ParentSettingActivity", "getFirstFlg 0002");
        return booleanExtra;
    }

    public boolean isApplicationForeground() throws SecurityException {
        boolean z;
        Logger.d("ParentSettingActivity", "isApplicationForeground 0001");
        Logger.i("ParentSettingActivity", "thisPackageName :" + getApplicationInfo().packageName);
        ActivityManager activityManager = (ActivityManager) getSystemService("activity");
        try {
            Logger.d("ParentSettingActivity", "isApplicationForeground 0002");
            List<ActivityManager.RunningTaskInfo> runningTasks = activityManager.getRunningTasks(1);
            String packageName = runningTasks.get(0).baseActivity.getPackageName();
            Logger.i("ParentSettingActivity", "foregroundTaskName :" + packageName);
            runningTasks.clear();
            if (packageName == null || !packageName.startsWith("jp.co.benesse")) {
                z = false;
            } else {
                Logger.d("ParentSettingActivity", "isApplicationForeground 0003");
                z = true;
            }
            Logger.d("ParentSettingActivity", "isApplicationForeground 0004");
            Logger.d("ParentSettingActivity", "isApplicationForeground 0006");
            return z;
        } catch (SecurityException e) {
            Logger.d("ParentSettingActivity", "isApplicationForeground 0005");
            Logger.e("ParentSettingActivity", "SecurityException", e);
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
            Logger.e("ParentSettingActivity", "RemoteException", e);
        }
        Logger.d("ParentSettingActivity", "doCancelDigicharize 0005");
    }

    protected void hideNavigationBar(boolean z) {
        Logger.d("ParentSettingActivity", "hideNavigationBar 0001");
        try {
            Logger.d("ParentSettingActivity", "hideNavigationBar 0002");
            if (this.mDchaService != null) {
                Logger.d("ParentSettingActivity", "hideNavigationBar 0003");
                this.mDchaService.hideNavigationBar(z);
            }
        } catch (RemoteException e) {
            Logger.d("ParentSettingActivity", "hideNavigationBar 0004");
            Logger.e("ParentSettingActivity", "RemoteException", e);
        }
        Logger.d("ParentSettingActivity", "hideNavigationBar 0005");
    }

    protected void setFont(TextView textView) {
        Logger.d("ParentSettingActivity", "setFont 0001");
        if (canReadSystemFont()) {
            Logger.d("ParentSettingActivity", "setFont 0002");
            textView.setTypeface(Typeface.createFromFile("system/fonts/gjsgm.ttf"));
        }
        Logger.d("ParentSettingActivity", "setFont 0003");
    }

    public static boolean canReadSystemFont() {
        Logger.d("ParentSettingActivity", "canReadSystemFont 0001");
        File file = new File("system/fonts/gjsgm.ttf");
        Logger.d("ParentSettingActivity", "canReadSystemFont 0002");
        return FileUtils.canReadFile(file);
    }
}
