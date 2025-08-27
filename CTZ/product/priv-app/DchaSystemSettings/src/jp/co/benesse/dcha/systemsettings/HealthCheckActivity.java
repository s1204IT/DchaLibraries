package jp.co.benesse.dcha.systemsettings;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import jp.co.benesse.dcha.util.Logger;

/* loaded from: classes.dex */
public class HealthCheckActivity extends ParentSettingActivity implements View.OnClickListener {
    private CheckNetworkTask checkNetworkTask;
    private HealthCheckDto healthCheckDto;

    @Override // jp.co.benesse.dcha.systemsettings.ParentSettingActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        Logger.d("HealthCheckActivity", "onCreate 0001");
        super.onCreate(bundle);
        setContentView(R.layout.act_health_check);
        this.checkNetworkTask = null;
        this.healthCheckDto = null;
        this.mIsFirstFlow = getFirstFlg();
        findViewById(R.id.details_btn).setOnClickListener(this);
        findViewById(R.id.details_btn).setEnabled(false);
        findViewById(R.id.next_btn).setOnClickListener(this);
        findViewById(R.id.back_btn).setOnClickListener(this);
        if (bundle != null) {
            Logger.d("HealthCheckActivity", "onCreate 0002");
            this.healthCheckDto = (HealthCheckDto) bundle.getSerializable(HealthCheckDto.class.getSimpleName());
            bundle.clear();
        }
        getWindow().addFlags(128);
        Logger.d("HealthCheckActivity", "onCreate 0003");
    }

    @Override // jp.co.benesse.dcha.systemsettings.ParentSettingActivity, android.app.Activity
    protected void onStart() {
        Logger.d("HealthCheckActivity", "onStart 0001");
        super.onStart();
        findViewById(R.id.hCheckResultSsid).setVisibility(4);
        findViewById(R.id.hCheckLoadingSsid).setVisibility(4);
        findViewById(R.id.hCheckRCheckSsid).setVisibility(4);
        findViewById(R.id.hCheckResultWifi).setVisibility(4);
        findViewById(R.id.hCheckLoadingWifi).setVisibility(4);
        findViewById(R.id.hCheckRCheckWifi).setVisibility(4);
        findViewById(R.id.hCheckResultIpAddress).setVisibility(4);
        findViewById(R.id.hCheckLoadingIpAddress).setVisibility(4);
        findViewById(R.id.hCheckRCheckIpAddress).setVisibility(4);
        findViewById(R.id.hCheckResultNetConnection).setVisibility(4);
        findViewById(R.id.hCheckLoadingNetConnectio).setVisibility(4);
        findViewById(R.id.hCheckRCheckNetConnection).setVisibility(4);
        findViewById(R.id.hCheckDSpeedPending).setVisibility(4);
        findViewById(R.id.hCheckResultDSpeedImg).setVisibility(4);
        findViewById(R.id.hCheckResultDSpeedText).setVisibility(4);
        findViewById(R.id.hCheckLoadingDSpeed).setVisibility(4);
        findViewById(R.id.hCheckRCheckDownloadSpeed).setVisibility(4);
        findViewById(R.id.checkNGResultText).setVisibility(4);
        findViewById(R.id.next_btn).setVisibility(8);
        findViewById(R.id.back_btn).setVisibility(8);
        changeBtnClickable(true);
        if (this.healthCheckDto == null || this.healthCheckDto.isHealthChecked == R.string.health_check_pending) {
            Logger.d("HealthCheckActivity", "onStart 0003");
            this.checkNetworkTask = new CheckNetworkTask(this);
            this.checkNetworkTask.execute(new Void[0]);
        } else {
            Logger.d("HealthCheckActivity", "onStart 0004");
            updateHealthCheckInfo(this.healthCheckDto);
        }
        Logger.d("HealthCheckActivity", "onStart 0005");
    }

    @Override // jp.co.benesse.dcha.systemsettings.ParentSettingActivity, android.app.Activity
    protected void onStop() {
        Logger.d("HealthCheckActivity", "onStop 0001");
        super.onStop();
        if (this.checkNetworkTask != null && this.checkNetworkTask.getStatus() != AsyncTask.Status.FINISHED) {
            Logger.d("HealthCheckActivity", "onStop 0002");
            this.checkNetworkTask.stop();
        }
        Logger.d("HealthCheckActivity", "onStop 0003");
    }

    @Override // android.app.Activity
    protected void onSaveInstanceState(Bundle bundle) {
        Logger.d("HealthCheckActivity", "onSaveInstanceState 0001");
        super.onSaveInstanceState(bundle);
        if (this.healthCheckDto != null && this.healthCheckDto.isHealthChecked != R.string.health_check_pending) {
            Logger.d("HealthCheckActivity", "onSaveInstanceState 0002");
            bundle.putSerializable(HealthCheckDto.class.getSimpleName(), this.healthCheckDto);
        }
        Logger.d("HealthCheckActivity", "onSaveInstanceState 0003");
    }

    @Override // jp.co.benesse.dcha.systemsettings.ParentSettingActivity, android.app.Activity
    protected void onDestroy() {
        Logger.d("HealthCheckActivity", "onDestroy 0001");
        super.onDestroy();
        View viewFindViewById = findViewById(R.id.details_btn);
        if (viewFindViewById != null) {
            Logger.d("HealthCheckActivity", "onDestroy 0002");
            viewFindViewById.setOnClickListener(null);
        }
        View viewFindViewById2 = findViewById(R.id.next_btn);
        if (viewFindViewById2 != null) {
            Logger.d("HealthCheckActivity", "onDestroy 0003");
            viewFindViewById2.setOnClickListener(null);
        }
        View viewFindViewById3 = findViewById(R.id.back_btn);
        if (viewFindViewById3 != null) {
            Logger.d("HealthCheckActivity", "onDestroy 0004");
            viewFindViewById3.setOnClickListener(null);
        }
        this.checkNetworkTask = null;
        this.healthCheckDto = null;
        Logger.d("HealthCheckActivity", "onDestroy 0005");
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        Logger.d("HealthCheckActivity", "onClick 0001");
        switch (view.getId()) {
            case R.id.next_btn /* 2131361813 */:
                Logger.d("HealthCheckActivity", "onClick 0003");
                changeBtnClickable(false);
                moveDownloadActivity();
                break;
            case R.id.back_btn /* 2131361814 */:
                Logger.d("HealthCheckActivity", "onClick 0004");
                changeBtnClickable(false);
                moveWifiSettingActivity();
                finish();
                break;
            case R.id.details_btn /* 2131361815 */:
                Logger.d("HealthCheckActivity", "onClick 0002");
                changeBtnClickable(false);
                showDetailDialog();
                break;
        }
        Logger.d("HealthCheckActivity", "onClick 0005");
    }

    private void moveDownloadActivity() {
        Logger.d("HealthCheckActivity", "moveDownloadActivity 0001");
        Intent intent = new Intent();
        intent.setClassName("jp.co.benesse.dcha.setupwizard", "jp.co.benesse.dcha.setupwizard.DownloadSettingActivity");
        intent.putExtra("first_flg", this.mIsFirstFlow);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
        Logger.d("HealthCheckActivity", "moveDownloadActivity 0002");
    }

    private void moveWifiSettingActivity() {
        Logger.d("HealthCheckActivity", "moveWifiSettingActivity 0001");
        Intent intent = new Intent();
        intent.setClassName("jp.co.benesse.dcha.systemsettings", "jp.co.benesse.dcha.systemsettings.WifiSettingActivity");
        intent.putExtra("first_flg", this.mIsFirstFlow);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
        Logger.d("HealthCheckActivity", "moveWifiSettingActivity 0002");
    }

    private void showDetailDialog() {
        Logger.d("HealthCheckActivity", "showDetailDialog 0001");
        HCheckDetailDialog hCheckDetailDialog = new HCheckDetailDialog();
        Bundle bundle = new Bundle();
        bundle.putSerializable("healthCheckDto", this.healthCheckDto);
        hCheckDetailDialog.setArguments(bundle);
        hCheckDetailDialog.setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: jp.co.benesse.dcha.systemsettings.HealthCheckActivity.1
            @Override // android.content.DialogInterface.OnDismissListener
            public void onDismiss(DialogInterface dialogInterface) {
                HealthCheckActivity.this.changeBtnClickable(true);
            }
        });
        hCheckDetailDialog.show(getFragmentManager(), "dialog");
        Logger.d("HealthCheckActivity", "showDetailDialog 0002");
    }

    private void changeBtnClickable(boolean z) {
        Logger.d("HealthCheckActivity", "changeBtnClickable 0001");
        View viewFindViewById = findViewById(R.id.details_btn);
        if (viewFindViewById != null) {
            Logger.d("HealthCheckActivity", "changeBtnClickable 0002");
            viewFindViewById.setClickable(z);
        }
        View viewFindViewById2 = findViewById(R.id.next_btn);
        if (viewFindViewById2 != null) {
            Logger.d("HealthCheckActivity", "changeBtnClickable 0003");
            viewFindViewById2.setClickable(z);
        }
        View viewFindViewById3 = findViewById(R.id.back_btn);
        if (viewFindViewById3 != null) {
            Logger.d("HealthCheckActivity", "changeBtnClickable 0004");
            viewFindViewById3.setClickable(z);
        }
        Logger.d("HealthCheckActivity", "changeBtnClickable 0005");
    }

    public void updateHealthCheckInfo(HealthCheckDto healthCheckDto) {
        Logger.d("HealthCheckActivity", "updateHealthCheckInfo 0001");
        if (healthCheckDto != null) {
            Logger.d("HealthCheckActivity", "updateHealthCheckInfo 0002");
            drawingHealthCheckProgress(healthCheckDto);
            if (healthCheckDto.isHealthChecked != R.string.health_check_pending) {
                Logger.d("HealthCheckActivity", "updateHealthCheckInfo 0003");
                drawingHealthCheckResult(healthCheckDto);
                this.healthCheckDto = healthCheckDto;
            }
        }
        Logger.d("HealthCheckActivity", "updateHealthCheckInfo 0004");
    }

    protected void drawingHealthCheckProgress(HealthCheckDto healthCheckDto) {
        Logger.d("HealthCheckActivity", "onProgressUpdate 0001");
        drawingProgressView(healthCheckDto.isCheckedSsid, R.id.hCheckRCheckSsid, R.id.hCheckResultSsid, healthCheckDto.mySsid);
        drawingProgressView(healthCheckDto.isCheckedWifi, R.id.hCheckRCheckWifi, R.id.hCheckResultWifi, getString(healthCheckDto.isCheckedWifi));
        drawingProgressView(healthCheckDto.isCheckedIpAddress, R.id.hCheckRCheckIpAddress, R.id.hCheckResultIpAddress, getString(healthCheckDto.isCheckedIpAddress));
        drawingProgressView(healthCheckDto.isCheckedNetConnection, R.id.hCheckRCheckNetConnection, R.id.hCheckResultNetConnection, getString(healthCheckDto.isCheckedNetConnection));
        Logger.d("HealthCheckActivity", "onProgressUpdate 0002");
    }

    protected void drawingProgressView(int i, int i2, int i3, String str) {
        Logger.d("HealthCheckActivity", "drawingProgressView 0001");
        if (i != R.string.health_check_pending) {
            Logger.d("HealthCheckActivity", "drawingProgressView 0002");
            findViewById(i2).setVisibility(0);
            TextView textView = (TextView) findViewById(i3);
            textView.setText(str);
            if (i == R.string.health_check_ok) {
                Logger.d("HealthCheckActivity", "drawingProgressView 0003");
                textView.setTextColor(getResources().getColor(R.color.text_black));
            } else {
                Logger.d("HealthCheckActivity", "drawingProgressView 0004");
                textView.setTextColor(getResources().getColor(R.color.text_red_hc));
            }
            textView.setVisibility(0);
        }
        Logger.d("HealthCheckActivity", "drawingProgressView 0005");
    }

    protected void drawingHealthCheckResult(HealthCheckDto healthCheckDto) {
        Logger.d("HealthCheckActivity", "onPostExecute 0001");
        drawingPendingView(healthCheckDto.isCheckedWifi, R.id.hCheckResultWifi);
        drawingPendingView(healthCheckDto.isCheckedIpAddress, R.id.hCheckResultIpAddress);
        drawingPendingView(healthCheckDto.isCheckedNetConnection, R.id.hCheckResultNetConnection);
        if (healthCheckDto.isCheckedDSpeed == R.string.health_check_pending) {
            Logger.d("HealthCheckActivity", "onPostExecute 0002");
            findViewById(R.id.hCheckDSpeedPending).setVisibility(0);
        } else {
            Logger.d("HealthCheckActivity", "onPostExecute 0003");
            findViewById(R.id.hCheckRCheckDownloadSpeed).setVisibility(0);
            ImageView imageView = (ImageView) findViewById(R.id.hCheckResultDSpeedImg);
            imageView.setImageResource(healthCheckDto.myDSpeedImage);
            imageView.setVisibility(0);
            TextView textView = (TextView) findViewById(R.id.hCheckResultDSpeedText);
            textView.setText(healthCheckDto.myDownloadSpeed);
            textView.setVisibility(0);
        }
        findViewById(R.id.details_btn).setEnabled(true);
        if (healthCheckDto.isHealthChecked == R.string.health_check_ng) {
            Logger.d("HealthCheckActivity", "onPostExecute 0004");
            findViewById(R.id.checkNGResultText).setVisibility(0);
            findViewById(R.id.back_btn).setVisibility(0);
        } else {
            Logger.d("HealthCheckActivity", "onPostExecute 0005");
            findViewById(R.id.next_btn).setVisibility(0);
        }
        Logger.d("HealthCheckActivity", "onPostExecute 0006");
    }

    protected void drawingPendingView(int i, int i2) {
        Logger.d("HealthCheckActivity", "drawingPendingView 0001");
        if (i == R.string.health_check_pending) {
            Logger.d("HealthCheckActivity", "drawingPendingView 0002");
            TextView textView = (TextView) findViewById(i2);
            textView.setText(getString(R.string.health_check_pending));
            textView.setTextColor(getResources().getColor(R.color.text_gray_hc));
            textView.setVisibility(0);
        }
        Logger.d("HealthCheckActivity", "drawingPendingView 0003");
    }

    protected static class CheckNetworkTask extends AsyncTask<Void, HealthCheckDto, HealthCheckDto> {
        private final String TAG = "CheckNetworkTask";
        protected HealthCheckDto healthCheckDto = null;
        protected HealthCheckLogic logic;
        private WeakReference<Activity> owner;

        public CheckNetworkTask(Activity activity) {
            Logger.d("CheckNetworkTask", "CheckNetworkTask 0001");
            this.owner = new WeakReference<>(activity);
            this.logic = new HealthCheckLogic();
            Logger.d("CheckNetworkTask", "CheckNetworkTask 0002");
        }

        public void stop() {
            Logger.d("CheckNetworkTask", "stop 0001");
            if (this.healthCheckDto != null) {
                Logger.d("CheckNetworkTask", "stop 0002");
                this.healthCheckDto.cancel();
            }
            cancel(true);
            Logger.d("CheckNetworkTask", "stop 0003");
        }

        @Override // android.os.AsyncTask
        protected void onPreExecute() {
            Logger.d("CheckNetworkTask", "onPreExecute 0001");
            this.healthCheckDto = new HealthCheckDto();
        }

        /* JADX DEBUG: Another duplicated slice has different insns count: {[IGET]}, finally: {[IGET, IPUT, IF] complete} */
        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [738=6, 739=6] */
        /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.os.AsyncTask
        protected HealthCheckDto doInBackground(Void... voidArr) {
            Logger.d("CheckNetworkTask", "doInBackground 0001");
            if (isCancelled()) {
                Logger.d("CheckNetworkTask", "doInBackground 0002");
                return null;
            }
            try {
                Activity activity = this.owner.get();
                if (activity != null) {
                    Logger.d("CheckNetworkTask", "doInBackground 0003");
                    WifiManager wifiManager = (WifiManager) activity.getSystemService("wifi");
                    RotateAsyncTask rotateAsyncTask = new RotateAsyncTask(activity, R.id.hCheckLoadingSsid);
                    rotateAsyncTask.executeOnExecutor(THREAD_POOL_EXECUTOR, new Void[0]);
                    this.logic.getMacAddress(activity, wifiManager.getConnectionInfo(), this.healthCheckDto);
                    this.logic.checkSsid(activity, wifiManager.getConfiguredNetworks(), this.healthCheckDto);
                    rotateAsyncTask.cancel(true);
                    publishProgress(this.healthCheckDto);
                    if (!isCancelled() && this.healthCheckDto.isCheckedSsid != R.string.health_check_ng) {
                        RotateAsyncTask rotateAsyncTask2 = new RotateAsyncTask(activity, R.id.hCheckLoadingWifi);
                        rotateAsyncTask2.executeOnExecutor(THREAD_POOL_EXECUTOR, new Void[0]);
                        this.logic.checkWifi(wifiManager.getConnectionInfo(), this.healthCheckDto);
                        rotateAsyncTask2.cancel(true);
                        publishProgress(this.healthCheckDto);
                        if (!isCancelled() && this.healthCheckDto.isCheckedWifi != R.string.health_check_ng) {
                            RotateAsyncTask rotateAsyncTask3 = new RotateAsyncTask(activity, R.id.hCheckLoadingIpAddress);
                            rotateAsyncTask3.executeOnExecutor(THREAD_POOL_EXECUTOR, new Void[0]);
                            this.logic.checkIpAddress(activity, wifiManager.getDhcpInfo(), this.healthCheckDto);
                            rotateAsyncTask3.cancel(true);
                            publishProgress(this.healthCheckDto);
                            if (!isCancelled() && this.healthCheckDto.isCheckedIpAddress != R.string.health_check_ng) {
                                RotateAsyncTask rotateAsyncTask4 = new RotateAsyncTask(activity, R.id.hCheckLoadingNetConnectio);
                                rotateAsyncTask4.executeOnExecutor(THREAD_POOL_EXECUTOR, new Void[0]);
                                HealthChkMngDto healthChkMngDto = new HealthChkMngDto();
                                healthChkMngDto.url = "http://ctcds.benesse.ne.jp/network-check/connection.html";
                                healthChkMngDto.timeout = 30;
                                this.logic.checkNetConnection(healthChkMngDto, this.healthCheckDto);
                                rotateAsyncTask4.cancel(true);
                                publishProgress(this.healthCheckDto);
                                if (!isCancelled() && this.healthCheckDto.isCheckedNetConnection != R.string.health_check_ng) {
                                    RotateAsyncTask rotateAsyncTask5 = new RotateAsyncTask(activity, R.id.hCheckLoadingDSpeed);
                                    rotateAsyncTask5.executeOnExecutor(THREAD_POOL_EXECUTOR, new Void[0]);
                                    HealthChkMngDto healthChkMngDto2 = new HealthChkMngDto();
                                    healthChkMngDto2.url = "http://ctcds.benesse.ne.jp/network-check/speedtest.list";
                                    healthChkMngDto2.timeout = 30;
                                    this.logic.checkDownloadSpeed(activity, healthChkMngDto2, this.healthCheckDto);
                                    rotateAsyncTask5.cancel(true);
                                    this.healthCheckDto.isHealthChecked = R.string.health_check_ok;
                                }
                                Logger.d("CheckNetworkTask", "doInBackground 0007");
                                this.healthCheckDto.isHealthChecked = R.string.health_check_ng;
                                return this.healthCheckDto;
                            }
                            Logger.d("CheckNetworkTask", "doInBackground 0006");
                            this.healthCheckDto.isHealthChecked = R.string.health_check_ng;
                            HealthCheckDto healthCheckDto = this.healthCheckDto;
                            if (this.logic != null) {
                                this.logic = null;
                            }
                            return healthCheckDto;
                        }
                        Logger.d("CheckNetworkTask", "doInBackground 0005");
                        this.healthCheckDto.isHealthChecked = R.string.health_check_ng;
                        HealthCheckDto healthCheckDto2 = this.healthCheckDto;
                        if (this.logic != null) {
                            this.logic = null;
                        }
                        return healthCheckDto2;
                    }
                    Logger.d("CheckNetworkTask", "doInBackground 0004");
                    this.healthCheckDto.isHealthChecked = R.string.health_check_ng;
                    HealthCheckDto healthCheckDto3 = this.healthCheckDto;
                    if (this.logic != null) {
                        this.logic = null;
                    }
                    return healthCheckDto3;
                }
                if (this.logic != null) {
                    this.logic = null;
                }
                Logger.d("CheckNetworkTask", "doInBackground 0008");
                return this.healthCheckDto;
            } finally {
                if (this.logic != null) {
                    this.logic = null;
                }
            }
        }

        /* JADX DEBUG: Method merged with bridge method: onProgressUpdate([Ljava/lang/Object;)V */
        @Override // android.os.AsyncTask
        protected void onProgressUpdate(HealthCheckDto... healthCheckDtoArr) {
            Logger.d("CheckNetworkTask", "onProgressUpdate 0001");
            HealthCheckDto healthCheckDto = healthCheckDtoArr[0];
            HealthCheckActivity healthCheckActivity = (HealthCheckActivity) this.owner.get();
            if (healthCheckActivity != null) {
                Logger.d("CheckNetworkTask", "onProgressUpdate 0002");
                healthCheckActivity.updateHealthCheckInfo(healthCheckDto);
            }
            Logger.d("CheckNetworkTask", "onProgressUpdate 0003");
        }

        /* JADX DEBUG: Method merged with bridge method: onPostExecute(Ljava/lang/Object;)V */
        @Override // android.os.AsyncTask
        protected void onPostExecute(HealthCheckDto healthCheckDto) {
            Logger.d("CheckNetworkTask", "onPostExecute 0001");
            HealthCheckActivity healthCheckActivity = (HealthCheckActivity) this.owner.get();
            if (healthCheckActivity != null) {
                Logger.d("CheckNetworkTask", "onPostExecute 0002");
                healthCheckActivity.updateHealthCheckInfo(healthCheckDto);
            }
            Logger.d("CheckNetworkTask", "onPostExecute 0003");
        }
    }

    protected static class RotateAsyncTask extends AsyncTask<Void, Void, Void> {
        private final int id;
        private WeakReference<Activity> owner;
        private final String TAG = "RotateAsyncTask";
        private int rotation = 0;

        public RotateAsyncTask(Activity activity, int i) {
            Logger.d("RotateAsyncTask", "RotateAsyncTask 0001");
            this.owner = new WeakReference<>(activity);
            this.id = i;
        }

        /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.os.AsyncTask
        protected Void doInBackground(Void... voidArr) throws InterruptedException {
            Logger.d("RotateAsyncTask", "doInBackground 0001");
            while (!isCancelled()) {
                try {
                    Thread.sleep(100L);
                    publishProgress(new Void[0]);
                } catch (InterruptedException e) {
                    return null;
                }
            }
            return null;
        }

        /* JADX DEBUG: Method merged with bridge method: onProgressUpdate([Ljava/lang/Object;)V */
        @Override // android.os.AsyncTask
        protected void onProgressUpdate(Void... voidArr) {
            Logger.d("RotateAsyncTask", "onProgressUpdate 0001");
            Activity activity = this.owner.get();
            if (activity != null && !isCancelled()) {
                Logger.d("RotateAsyncTask", "onProgressUpdate 0002");
                ImageView imageView = (ImageView) activity.findViewById(this.id);
                if (imageView.getVisibility() != 0) {
                    Logger.d("RotateAsyncTask", "onProgressUpdate 0003");
                    imageView.setVisibility(0);
                }
                this.rotation = (this.rotation + 30) % 360;
                imageView.setRotation(this.rotation);
            }
            Logger.d("RotateAsyncTask", "onProgressUpdate 0004");
        }

        @Override // android.os.AsyncTask
        protected void onCancelled() {
            Logger.d("RotateAsyncTask", "onCancelled 0001");
            Activity activity = this.owner.get();
            if (activity != null) {
                Logger.d("RotateAsyncTask", "onCancelled 0002");
                activity.findViewById(this.id).setVisibility(4);
            }
            Logger.d("RotateAsyncTask", "onCancelled 0003");
            super.onCancelled();
        }
    }
}
