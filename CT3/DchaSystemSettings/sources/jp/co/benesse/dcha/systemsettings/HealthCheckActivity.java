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

public class HealthCheckActivity extends ParentSettingActivity implements View.OnClickListener {
    private CheckNetworkTask checkNetworkTask;
    private HealthCheckDto healthCheckDto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.d("HealthCheckActivity", "onCreate 0001");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_health_check);
        this.checkNetworkTask = null;
        this.healthCheckDto = null;
        this.mIsFirstFlow = getFirstFlg();
        findViewById(R.id.details_btn).setOnClickListener(this);
        findViewById(R.id.details_btn).setEnabled(false);
        findViewById(R.id.next_btn).setOnClickListener(this);
        findViewById(R.id.back_btn).setOnClickListener(this);
        if (savedInstanceState != null) {
            Logger.d("HealthCheckActivity", "onCreate 0002");
            this.healthCheckDto = (HealthCheckDto) savedInstanceState.getSerializable(HealthCheckDto.class.getSimpleName());
            savedInstanceState.clear();
        }
        getWindow().addFlags(128);
        Logger.d("HealthCheckActivity", "onCreate 0003");
    }

    @Override
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

    @Override
    protected void onStop() {
        Logger.d("HealthCheckActivity", "onStop 0001");
        super.onStop();
        if (this.checkNetworkTask != null && this.checkNetworkTask.getStatus() != AsyncTask.Status.FINISHED) {
            Logger.d("HealthCheckActivity", "onStop 0002");
            this.checkNetworkTask.stop();
        }
        Logger.d("HealthCheckActivity", "onStop 0003");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Logger.d("HealthCheckActivity", "onSaveInstanceState 0001");
        super.onSaveInstanceState(outState);
        if (this.healthCheckDto != null && this.healthCheckDto.isHealthChecked != R.string.health_check_pending) {
            Logger.d("HealthCheckActivity", "onSaveInstanceState 0002");
            outState.putSerializable(HealthCheckDto.class.getSimpleName(), this.healthCheckDto);
        }
        Logger.d("HealthCheckActivity", "onSaveInstanceState 0003");
    }

    @Override
    protected void onDestroy() {
        Logger.d("HealthCheckActivity", "onDestroy 0001");
        super.onDestroy();
        View v = findViewById(R.id.details_btn);
        if (v != null) {
            Logger.d("HealthCheckActivity", "onDestroy 0002");
            v.setOnClickListener(null);
        }
        View v2 = findViewById(R.id.next_btn);
        if (v2 != null) {
            Logger.d("HealthCheckActivity", "onDestroy 0003");
            v2.setOnClickListener(null);
        }
        View v3 = findViewById(R.id.back_btn);
        if (v3 != null) {
            Logger.d("HealthCheckActivity", "onDestroy 0004");
            v3.setOnClickListener(null);
        }
        this.checkNetworkTask = null;
        this.healthCheckDto = null;
        Logger.d("HealthCheckActivity", "onDestroy 0005");
    }

    @Override
    public void onClick(View v) {
        Logger.d("HealthCheckActivity", "onClick 0001");
        int id = v.getId();
        switch (id) {
            case R.id.next_btn:
                Logger.d("HealthCheckActivity", "onClick 0003");
                changeBtnClickable(false);
                moveDownloadActivity();
                break;
            case R.id.back_btn:
                Logger.d("HealthCheckActivity", "onClick 0004");
                changeBtnClickable(false);
                moveWifiSettingActivity();
                finish();
                break;
            case R.id.details_btn:
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
        finish();
        Logger.d("HealthCheckActivity", "moveDownloadActivity 0002");
    }

    private void moveWifiSettingActivity() {
        Logger.d("HealthCheckActivity", "moveWifiSettingActivity 0001");
        Intent intent = new Intent();
        intent.setClassName("jp.co.benesse.dcha.systemsettings", "jp.co.benesse.dcha.systemsettings.WifiSettingActivity");
        intent.putExtra("first_flg", this.mIsFirstFlow);
        startActivity(intent);
        finish();
        Logger.d("HealthCheckActivity", "moveWifiSettingActivity 0002");
    }

    private void showDetailDialog() {
        Logger.d("HealthCheckActivity", "showDetailDialog 0001");
        HCheckDetailDialog dialog = new HCheckDetailDialog();
        Bundle bundle = new Bundle();
        bundle.putSerializable("healthCheckDto", this.healthCheckDto);
        dialog.setArguments(bundle);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog2) {
                HealthCheckActivity.this.changeBtnClickable(true);
            }
        });
        dialog.show(getFragmentManager(), "dialog");
        Logger.d("HealthCheckActivity", "showDetailDialog 0002");
    }

    public void changeBtnClickable(boolean clickable) {
        Logger.d("HealthCheckActivity", "changeBtnClickable 0001");
        View v = findViewById(R.id.details_btn);
        if (v != null) {
            Logger.d("HealthCheckActivity", "changeBtnClickable 0002");
            v.setClickable(clickable);
        }
        View v2 = findViewById(R.id.next_btn);
        if (v2 != null) {
            Logger.d("HealthCheckActivity", "changeBtnClickable 0003");
            v2.setClickable(clickable);
        }
        View v3 = findViewById(R.id.back_btn);
        if (v3 != null) {
            Logger.d("HealthCheckActivity", "changeBtnClickable 0004");
            v3.setClickable(clickable);
        }
        Logger.d("HealthCheckActivity", "changeBtnClickable 0005");
    }

    public void updateHealthCheckInfo(HealthCheckDto dto) {
        Logger.d("HealthCheckActivity", "updateHealthCheckInfo 0001");
        if (dto != null) {
            Logger.d("HealthCheckActivity", "updateHealthCheckInfo 0002");
            drawingHealthCheckProgress(dto);
            if (dto.isHealthChecked != R.string.health_check_pending) {
                Logger.d("HealthCheckActivity", "updateHealthCheckInfo 0003");
                drawingHealthCheckResult(dto);
                this.healthCheckDto = dto;
            }
        }
        Logger.d("HealthCheckActivity", "updateHealthCheckInfo 0004");
    }

    protected void drawingHealthCheckProgress(HealthCheckDto dto) {
        Logger.d("HealthCheckActivity", "onProgressUpdate 0001");
        drawingProgressView(dto.isCheckedSsid, R.id.hCheckRCheckSsid, R.id.hCheckResultSsid, dto.mySsid);
        drawingProgressView(dto.isCheckedWifi, R.id.hCheckRCheckWifi, R.id.hCheckResultWifi, getString(dto.isCheckedWifi));
        drawingProgressView(dto.isCheckedIpAddress, R.id.hCheckRCheckIpAddress, R.id.hCheckResultIpAddress, getString(dto.isCheckedIpAddress));
        drawingProgressView(dto.isCheckedNetConnection, R.id.hCheckRCheckNetConnection, R.id.hCheckResultNetConnection, getString(dto.isCheckedNetConnection));
        Logger.d("HealthCheckActivity", "onProgressUpdate 0002");
    }

    protected void drawingProgressView(int healthCheckResult, int checkMarkView, int hCheckTView, String hCResultText) {
        Logger.d("HealthCheckActivity", "drawingProgressView 0001");
        if (healthCheckResult != R.string.health_check_pending) {
            Logger.d("HealthCheckActivity", "drawingProgressView 0002");
            findViewById(checkMarkView).setVisibility(0);
            TextView resultTextView = (TextView) findViewById(hCheckTView);
            resultTextView.setText(hCResultText);
            if (healthCheckResult == R.string.health_check_ok) {
                Logger.d("HealthCheckActivity", "drawingProgressView 0003");
                resultTextView.setTextColor(getResources().getColor(R.color.text_black));
            } else {
                Logger.d("HealthCheckActivity", "drawingProgressView 0004");
                resultTextView.setTextColor(getResources().getColor(R.color.text_red_hc));
            }
            resultTextView.setVisibility(0);
        }
        Logger.d("HealthCheckActivity", "drawingProgressView 0005");
    }

    protected void drawingHealthCheckResult(HealthCheckDto dto) {
        Logger.d("HealthCheckActivity", "onPostExecute 0001");
        drawingPendingView(dto.isCheckedWifi, R.id.hCheckResultWifi);
        drawingPendingView(dto.isCheckedIpAddress, R.id.hCheckResultIpAddress);
        drawingPendingView(dto.isCheckedNetConnection, R.id.hCheckResultNetConnection);
        if (dto.isCheckedDSpeed == R.string.health_check_pending) {
            Logger.d("HealthCheckActivity", "onPostExecute 0002");
            findViewById(R.id.hCheckDSpeedPending).setVisibility(0);
        } else {
            Logger.d("HealthCheckActivity", "onPostExecute 0003");
            findViewById(R.id.hCheckRCheckDownloadSpeed).setVisibility(0);
            ImageView dSpeedImageView = (ImageView) findViewById(R.id.hCheckResultDSpeedImg);
            dSpeedImageView.setImageResource(dto.myDSpeedImage);
            dSpeedImageView.setVisibility(0);
            TextView dSpeedTextView = (TextView) findViewById(R.id.hCheckResultDSpeedText);
            dSpeedTextView.setText(dto.myDownloadSpeed);
            dSpeedTextView.setVisibility(0);
        }
        findViewById(R.id.details_btn).setEnabled(true);
        if (dto.isHealthChecked == R.string.health_check_ng) {
            Logger.d("HealthCheckActivity", "onPostExecute 0004");
            findViewById(R.id.checkNGResultText).setVisibility(0);
            findViewById(R.id.back_btn).setVisibility(0);
        } else {
            Logger.d("HealthCheckActivity", "onPostExecute 0005");
            findViewById(R.id.next_btn).setVisibility(0);
        }
        Logger.d("HealthCheckActivity", "onPostExecute 0006");
    }

    protected void drawingPendingView(int healthCheckResult, int hCheckTView) {
        Logger.d("HealthCheckActivity", "drawingPendingView 0001");
        if (healthCheckResult == R.string.health_check_pending) {
            Logger.d("HealthCheckActivity", "drawingPendingView 0002");
            TextView resultTextView = (TextView) findViewById(hCheckTView);
            resultTextView.setText(getString(R.string.health_check_pending));
            resultTextView.setTextColor(getResources().getColor(R.color.text_gray_hc));
            resultTextView.setVisibility(0);
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

        @Override
        protected void onPreExecute() {
            Logger.d("CheckNetworkTask", "onPreExecute 0001");
            this.healthCheckDto = new HealthCheckDto();
        }

        @Override
        public HealthCheckDto doInBackground(Void... params) throws Throwable {
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
                    RotateAsyncTask rotateTask = new RotateAsyncTask(activity, R.id.hCheckLoadingSsid);
                    try {
                        rotateTask.executeOnExecutor(THREAD_POOL_EXECUTOR, new Void[0]);
                        this.logic.getMacAddress(activity, wifiManager.getConnectionInfo(), this.healthCheckDto);
                        this.logic.checkSsid(activity, wifiManager.getConfiguredNetworks(), this.healthCheckDto);
                        rotateTask.cancel(true);
                        publishProgress(this.healthCheckDto);
                        if (isCancelled() || this.healthCheckDto.isCheckedSsid == R.string.health_check_ng) {
                            Logger.d("CheckNetworkTask", "doInBackground 0004");
                            this.healthCheckDto.isHealthChecked = R.string.health_check_ng;
                            HealthCheckDto healthCheckDto = this.healthCheckDto;
                            if (this.logic != null) {
                                this.logic = null;
                            }
                            return healthCheckDto;
                        }
                        RotateAsyncTask rotateTask2 = new RotateAsyncTask(activity, R.id.hCheckLoadingWifi);
                        rotateTask2.executeOnExecutor(THREAD_POOL_EXECUTOR, new Void[0]);
                        this.logic.checkWifi(wifiManager.getConnectionInfo(), this.healthCheckDto);
                        rotateTask2.cancel(true);
                        publishProgress(this.healthCheckDto);
                        if (isCancelled() || this.healthCheckDto.isCheckedWifi == R.string.health_check_ng) {
                            Logger.d("CheckNetworkTask", "doInBackground 0005");
                            this.healthCheckDto.isHealthChecked = R.string.health_check_ng;
                            HealthCheckDto healthCheckDto2 = this.healthCheckDto;
                            if (this.logic != null) {
                                this.logic = null;
                            }
                            return healthCheckDto2;
                        }
                        RotateAsyncTask rotateTask3 = new RotateAsyncTask(activity, R.id.hCheckLoadingIpAddress);
                        rotateTask3.executeOnExecutor(THREAD_POOL_EXECUTOR, new Void[0]);
                        this.logic.checkIpAddress(activity, wifiManager.getDhcpInfo(), this.healthCheckDto);
                        rotateTask3.cancel(true);
                        publishProgress(this.healthCheckDto);
                        if (isCancelled() || this.healthCheckDto.isCheckedIpAddress == R.string.health_check_ng) {
                            Logger.d("CheckNetworkTask", "doInBackground 0006");
                            this.healthCheckDto.isHealthChecked = R.string.health_check_ng;
                            HealthCheckDto healthCheckDto3 = this.healthCheckDto;
                            if (this.logic != null) {
                                this.logic = null;
                            }
                            return healthCheckDto3;
                        }
                        RotateAsyncTask rotateTask4 = new RotateAsyncTask(activity, R.id.hCheckLoadingNetConnectio);
                        rotateTask4.executeOnExecutor(THREAD_POOL_EXECUTOR, new Void[0]);
                        HealthChkMngDto healthChkMngDto = new HealthChkMngDto();
                        healthChkMngDto.url = "http://ctcds.benesse.ne.jp/network-check/connection.html";
                        healthChkMngDto.timeout = 30;
                        this.logic.checkNetConnection(healthChkMngDto, this.healthCheckDto);
                        rotateTask4.cancel(true);
                        publishProgress(this.healthCheckDto);
                        if (isCancelled() || this.healthCheckDto.isCheckedNetConnection == R.string.health_check_ng) {
                            Logger.d("CheckNetworkTask", "doInBackground 0007");
                            this.healthCheckDto.isHealthChecked = R.string.health_check_ng;
                            HealthCheckDto healthCheckDto4 = this.healthCheckDto;
                            if (this.logic != null) {
                                this.logic = null;
                            }
                            return healthCheckDto4;
                        }
                        RotateAsyncTask rotateTask5 = new RotateAsyncTask(activity, R.id.hCheckLoadingDSpeed);
                        rotateTask5.executeOnExecutor(THREAD_POOL_EXECUTOR, new Void[0]);
                        HealthChkMngDto healthChkMngDto2 = new HealthChkMngDto();
                        healthChkMngDto2.url = "http://ctcds.benesse.ne.jp/network-check/speedtest.list";
                        healthChkMngDto2.timeout = 30;
                        this.logic.checkDownloadSpeed(activity, healthChkMngDto2, this.healthCheckDto);
                        rotateTask5.cancel(true);
                        this.healthCheckDto.isHealthChecked = R.string.health_check_ok;
                    } catch (Throwable th) {
                        th = th;
                        if (this.logic != null) {
                            this.logic = null;
                        }
                        throw th;
                    }
                }
                if (this.logic != null) {
                    this.logic = null;
                }
                Logger.d("CheckNetworkTask", "doInBackground 0008");
                return this.healthCheckDto;
            } catch (Throwable th2) {
                th = th2;
            }
        }

        @Override
        public void onProgressUpdate(HealthCheckDto... item) {
            Logger.d("CheckNetworkTask", "onProgressUpdate 0001");
            HealthCheckDto dto = item[0];
            HealthCheckActivity activity = (HealthCheckActivity) this.owner.get();
            if (activity != null) {
                Logger.d("CheckNetworkTask", "onProgressUpdate 0002");
                activity.updateHealthCheckInfo(dto);
            }
            Logger.d("CheckNetworkTask", "onProgressUpdate 0003");
        }

        @Override
        public void onPostExecute(HealthCheckDto result) {
            Logger.d("CheckNetworkTask", "onPostExecute 0001");
            HealthCheckActivity activity = (HealthCheckActivity) this.owner.get();
            if (activity != null) {
                Logger.d("CheckNetworkTask", "onPostExecute 0002");
                activity.updateHealthCheckInfo(result);
            }
            Logger.d("CheckNetworkTask", "onPostExecute 0003");
        }
    }

    protected static class RotateAsyncTask extends AsyncTask<Void, Void, Void> {
        private final int id;
        private WeakReference<Activity> owner;
        private final String TAG = "RotateAsyncTask";
        private int rotation = 0;

        public RotateAsyncTask(Activity activity, int id) {
            Logger.d("RotateAsyncTask", "RotateAsyncTask 0001");
            this.owner = new WeakReference<>(activity);
            this.id = id;
        }

        @Override
        public Void doInBackground(Void... arg0) {
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

        @Override
        public void onProgressUpdate(Void... values) {
            Logger.d("RotateAsyncTask", "onProgressUpdate 0001");
            Activity activity = this.owner.get();
            if (activity != null && !isCancelled()) {
                Logger.d("RotateAsyncTask", "onProgressUpdate 0002");
                ImageView rotationImageView = (ImageView) activity.findViewById(this.id);
                if (rotationImageView.getVisibility() != 0) {
                    Logger.d("RotateAsyncTask", "onProgressUpdate 0003");
                    rotationImageView.setVisibility(0);
                }
                this.rotation = (this.rotation + 30) % 360;
                rotationImageView.setRotation(this.rotation);
            }
            Logger.d("RotateAsyncTask", "onProgressUpdate 0004");
        }

        @Override
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
