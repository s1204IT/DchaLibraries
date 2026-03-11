package jp.co.benesse.dcha.setupwizard;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.ProgressBar;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import jp.co.benesse.dcha.dchaservice.IDchaService;
import jp.co.benesse.dcha.setupwizard.http.FileDownloadRequest;
import jp.co.benesse.dcha.setupwizard.http.FileDownloadResponse;
import jp.co.benesse.dcha.setupwizard.http.HttpThread;
import jp.co.benesse.dcha.setupwizard.http.Request;
import jp.co.benesse.dcha.setupwizard.http.Response;
import jp.co.benesse.dcha.util.Logger;
import jp.co.benesse.dcha.util.UrlUtil;

public class DownloadSettingActivity extends ParentSettingActivity {
    public static final String DOWNLOAD_APK_PATH = "open/TouchSetupLogin.apk";
    public static final int PROGRESS_MAX_VALUE = 100;
    private static final String TAG = DownloadSettingActivity.class.getSimpleName();
    protected IDchaService mDchaService;
    protected DownloadTask mDownloadTask;
    protected ProgressBar mProgressBar;
    protected ServiceConnection mDchaServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Logger.d(DownloadSettingActivity.TAG, "onServiceConnected 0001");
            DownloadSettingActivity.this.mDchaService = IDchaService.Stub.asInterface(iBinder);
            DownloadSettingActivity.this.startDownloadTask();
            Logger.d(DownloadSettingActivity.TAG, "onServiceConnected 0004");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Logger.d(DownloadSettingActivity.TAG, "onServiceDisconnected 0001");
            DownloadSettingActivity.this.mDchaService = null;
            Logger.d(DownloadSettingActivity.TAG, "onServiceDisconnected 0002");
        }
    };
    protected BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.d(DownloadSettingActivity.TAG, "onReceive 0001");
            if (!DchaNetworkUtil.isConnective(context)) {
                Logger.d(DownloadSettingActivity.TAG, "onReceive 0002");
                DownloadSettingActivity.this.callWifiErrorDialog();
                if (DownloadSettingActivity.this.mDownloadTask != null) {
                    Logger.d(DownloadSettingActivity.TAG, "onReceive 0003");
                    DownloadSettingActivity.this.mDownloadTask.cancel();
                }
            }
            Logger.d(DownloadSettingActivity.TAG, "onReceive 0004");
        }
    };
    protected BroadcastReceiver mInstallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.d(DownloadSettingActivity.TAG, "onReceive 0101");
            if (!"android.intent.action.PACKAGE_ADDED".equals(intent.getAction()) || intent.getExtras().getBoolean("android.intent.extra.REPLACING")) {
                return;
            }
            Logger.d(DownloadSettingActivity.TAG, "onReceive 0102");
            if (DownloadSettingActivity.this.mInstallReceiver != null) {
                DownloadSettingActivity downloadSettingActivity = DownloadSettingActivity.this;
                downloadSettingActivity.unregisterReceiver(downloadSettingActivity.mInstallReceiver);
                DownloadSettingActivity.this.mInstallReceiver = null;
            }
            DownloadSettingActivity.this.moveInstalledApk(intent.getData().getSchemeSpecificPart());
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        Logger.d(TAG, "onCreate 0001");
        super.onCreate(bundle);
        setContentView(R.layout.act_download);
        this.mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        this.mProgressBar.setMax(100);
        this.mProgressBar.setProgress(0);
        File file = new File(getFilesDir(), getString(R.string.path_download_file));
        file.mkdirs();
        file.setExecutable(true, false);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addDataScheme("package");
        registerReceiver(this.mInstallReceiver, intentFilter);
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
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        registerReceiver(this.mWifiReceiver, intentFilter);
        Logger.d(TAG, "onStart 0002");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Logger.d(TAG, "onStop 0001");
        if (this.mWifiReceiver != null) {
            Logger.d(TAG, "onStop 0002");
            unregisterReceiver(this.mWifiReceiver);
        }
        Logger.d(TAG, "onStop 0003");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger.d(TAG, "onDestroy 0001");
        this.mProgressBar = null;
        this.mWifiReceiver = null;
        if (this.mDownloadTask != null) {
            Logger.d(TAG, "onDestroy 0002");
            this.mDownloadTask.cancel();
            this.mDownloadTask = null;
        }
        if (this.mInstallReceiver != null) {
            Logger.d(TAG, "onDestroy 0003");
            unregisterReceiver(this.mInstallReceiver);
            this.mInstallReceiver = null;
        }
        if (this.mDchaServiceConnection != null) {
            Logger.d(TAG, "onDestroy 0004");
            unbindService(this.mDchaServiceConnection);
            this.mDchaServiceConnection = null;
            this.mDchaService = null;
        }
        Logger.d(TAG, "onDestroy 0005");
    }

    class DownloadTask extends AsyncTask<FileDownloadRequest, Integer, FileDownloadResponse> implements Request.ResponseListener {
        protected CountDownLatch mCountDownLatch = null;
        protected HttpThread mThread = new HttpThread();
        protected FileDownloadResponse mResponse = null;

        DownloadTask() {
        }

        @Override
        public FileDownloadResponse doInBackground(FileDownloadRequest... fileDownloadRequestArr) {
            Logger.d(DownloadSettingActivity.TAG, "doInBackground 0001");
            this.mCountDownLatch = new CountDownLatch(1);
            this.mThread.setResponseListener(this);
            this.mThread.postRequest(fileDownloadRequestArr[0]);
            try {
                try {
                    Logger.d(DownloadSettingActivity.TAG, "doInBackground 0002");
                    this.mThread.start();
                    this.mCountDownLatch.await();
                    Logger.d(DownloadSettingActivity.TAG, "doInBackground 0004");
                } catch (InterruptedException e) {
                    Logger.d(DownloadSettingActivity.TAG, "doInBackground 0003");
                    Logger.d(DownloadSettingActivity.TAG, "doInBackground InterruptedException", e);
                    DownloadSettingActivity.this.callSystemErrorDialog(DownloadSettingActivity.this.getString(R.string.error_code_fail_download));
                    cancel(true);
                    Logger.d(DownloadSettingActivity.TAG, "doInBackground 0004");
                }
                this.mThread.stopRunning();
                FileDownloadResponse fileDownloadResponse = this.mResponse;
                if (fileDownloadResponse != null && fileDownloadResponse.isSuccess()) {
                    Logger.d(DownloadSettingActivity.TAG, "doInBackground 0005");
                    try {
                        try {
                            Logger.d(DownloadSettingActivity.TAG, "doInBackground 0006");
                            this.mResponse.outFile.setReadable(true, false);
                            if (!DownloadSettingActivity.this.mDchaService.installApp(this.mResponse.outFile.getCanonicalPath(), 0)) {
                                Logger.d(DownloadSettingActivity.TAG, "doInBackground 0007");
                                DownloadSettingActivity.this.callSystemErrorDialog(DownloadSettingActivity.this.getString(R.string.error_code_fail_install));
                                cancel(true);
                            }
                            Logger.d(DownloadSettingActivity.TAG, "doInBackground 0009");
                        } catch (Exception e2) {
                            Logger.d(DownloadSettingActivity.TAG, "doInBackground 0008");
                            Logger.d(DownloadSettingActivity.TAG, "doInBackground Exception", e2);
                            DownloadSettingActivity.this.callSystemErrorDialog(DownloadSettingActivity.this.getString(R.string.error_code_fail_install));
                            cancel(true);
                            Logger.d(DownloadSettingActivity.TAG, "doInBackground 0009");
                        }
                        this.mResponse.outFile.delete();
                    } catch (Throwable th) {
                        Logger.d(DownloadSettingActivity.TAG, "doInBackground 0009");
                        this.mResponse.outFile.delete();
                        throw th;
                    }
                }
                Logger.d(DownloadSettingActivity.TAG, "doInBackground 0010");
                return this.mResponse;
            } catch (Throwable th2) {
                Logger.d(DownloadSettingActivity.TAG, "doInBackground 0004");
                this.mThread.stopRunning();
                throw th2;
            }
        }

        @Override
        public void onProgressUpdate(Integer... numArr) {
            Logger.d(DownloadSettingActivity.TAG, "onProgressUpdate 0001");
            DownloadSettingActivity.this.mProgressBar.setProgress(numArr[0].intValue());
            Logger.d(DownloadSettingActivity.TAG, "onProgressUpdate 0002");
        }

        @Override
        public void onPostExecute(FileDownloadResponse fileDownloadResponse) {
            Logger.d(DownloadSettingActivity.TAG, "onPostExecute 0001");
            DownloadSettingActivity.this.mProgressBar.setProgress(100);
            if (fileDownloadResponse == null) {
                Logger.d(DownloadSettingActivity.TAG, "onPostExecute 0002");
                DownloadSettingActivity.this.callNetworkErrorDialog();
            } else if (fileDownloadResponse.responseCode == 404 || fileDownloadResponse.responseCode == 403) {
                Logger.d(DownloadSettingActivity.TAG, "onPostExecute 0003");
                DownloadSettingActivity downloadSettingActivity = DownloadSettingActivity.this;
                downloadSettingActivity.callSystemErrorDialog(downloadSettingActivity.getString(R.string.error_code_before_open));
            } else if (!fileDownloadResponse.isSuccess()) {
                Logger.d(DownloadSettingActivity.TAG, "onPostExecute 0004");
                DownloadSettingActivity downloadSettingActivity2 = DownloadSettingActivity.this;
                downloadSettingActivity2.callSystemErrorDialog(downloadSettingActivity2.getString(R.string.error_code_fail_download));
            }
            Logger.d(DownloadSettingActivity.TAG, "onPostExecute 0005");
        }

        public void cancel() {
            Logger.d(DownloadSettingActivity.TAG, "cancel 0001");
            cancel(true);
            this.mThread.cancel();
            Logger.d(DownloadSettingActivity.TAG, "cancel 0002");
        }

        @Override
        public void onHttpResponse(Response response) {
            Logger.d(DownloadSettingActivity.TAG, "onHttpResponse 0001");
            Logger.d(DownloadSettingActivity.TAG, "onHttpResponse URL:", response.request.url);
            if (response instanceof FileDownloadResponse) {
                Logger.d(DownloadSettingActivity.TAG, "onHttpResponse 0002");
                this.mResponse = (FileDownloadResponse) response;
            }
            this.mCountDownLatch.countDown();
            Logger.d(DownloadSettingActivity.TAG, "onHttpResponse 0004");
        }

        @Override
        public void onHttpProgress(Response response) {
            Logger.d(DownloadSettingActivity.TAG, "onHttpProgress 0001");
            if (response.contentLength > 0) {
                Logger.d(DownloadSettingActivity.TAG, "onHttpProgress 0002");
                publishProgress(Integer.valueOf((int) ((response.receiveLength * 100) / response.contentLength)));
            }
            Logger.d(DownloadSettingActivity.TAG, "onHttpProgress 0003");
        }

        @Override
        public void onHttpError(Request request) {
            Logger.d(DownloadSettingActivity.TAG, "onHttpError 0001");
            Logger.d(DownloadSettingActivity.TAG, "onHttpError URL:", request.url);
            this.mCountDownLatch.countDown();
            Logger.d(DownloadSettingActivity.TAG, "onHttpError 0002");
        }

        @Override
        public void onHttpCancelled(Request request) {
            Logger.d(DownloadSettingActivity.TAG, "onHttpCancelled 0001");
            this.mCountDownLatch.countDown();
            Logger.d(DownloadSettingActivity.TAG, "onHttpCancelled 0002");
        }
    }

    protected void moveInstalledApk(String str) {
        try {
            Logger.d(TAG, "moveInstalledApk 0001");
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setPackage(str);
            intent.addCategory("android.intent.category.DEFAULT");
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        } catch (ActivityNotFoundException e) {
            Logger.d(TAG, "moveInstalledApk 0002");
            Logger.d(TAG, "moveInstalledApk ActivityNotFoundException", e);
            callSystemErrorDialog(getString(R.string.error_code_move_installed_apk));
        }
    }

    protected void startDownloadTask() {
        try {
            Logger.d(TAG, "onServiceConnected 0002");
            FileDownloadRequest fileDownloadRequest = new FileDownloadRequest();
            fileDownloadRequest.outPath = new File(getFilesDir(), getString(R.string.path_download_file));
            fileDownloadRequest.fileOverwrite = true;
            fileDownloadRequest.url = new URL(new UrlUtil().getUrlAkamai(this) + DOWNLOAD_APK_PATH);
            this.mDownloadTask = new DownloadTask();
            this.mDownloadTask.execute(fileDownloadRequest);
        } catch (MalformedURLException e) {
            Logger.d(TAG, "onServiceConnected 0003");
            Logger.d(TAG, "onServiceConnected MalformedURLException", e);
            callSystemErrorDialog(getString(R.string.error_code_get_file_path));
        }
    }
}
