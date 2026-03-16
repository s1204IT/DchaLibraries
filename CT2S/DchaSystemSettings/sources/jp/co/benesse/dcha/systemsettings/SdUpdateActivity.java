package jp.co.benesse.dcha.systemsettings;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StatFs;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.text.DecimalFormat;
import jp.co.benesse.dcha.dchaservice.IDchaService;
import jp.co.benesse.dcha.util.Logger;

public class SdUpdateActivity extends ParentSettingActivity implements View.OnClickListener {
    private IDchaService dchaService;
    private TextView mSdMountText;
    private Button mUpdateBtn;
    public String mUpdatePath;
    private ProgressDialog progressDialog;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            Logger.d("SDSettingActivity", "onServiceConnected 0001");
            SdUpdateActivity.this.dchaService = IDchaService.Stub.asInterface(service);
            Logger.d("SDSettingActivity", "onServiceConnected 0002");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Logger.d("SDSettingActivity", "onServiceDisconnected 0001");
            SdUpdateActivity.this.dchaService = null;
            Logger.d("SDSettingActivity", "onServiceDisconnected 0002");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.d("SDSettingActivity", "onCreate 0001");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_sd_update);
        this.mUpdatePath = getResources().getString(R.string.path_sd_update_file);
        Intent intent = new Intent("jp.co.benesse.dcha.dchaservice.DchaService");
        intent.setPackage("jp.co.benesse.dcha.dchaservice");
        bindService(intent, this.serviceConnection, 1);
        this.mSdMountText = (TextView) findViewById(R.id.text_sd_mount);
        setFont(this.mSdMountText);
        this.mUpdateBtn = (Button) findViewById(R.id.btn_sd_update);
        this.mUpdateBtn.setVisibility(0);
        this.mSdMountText.setText(R.string.msg_sd_update_able);
        this.mUpdateBtn.setOnClickListener(this);
        Logger.d("SDSettingActivity", "onCreate 0002");
    }

    @Override
    protected void onPause() {
        Logger.d("SDSettingActivity", "onPause 0001");
        super.onPause();
        Logger.d("SDSettingActivity", "onPause 0002");
    }

    @Override
    protected void onDestroy() {
        Logger.d("SDSettingActivity", "onDestroy 0001");
        super.onDestroy();
        this.mUpdateBtn.setOnClickListener(null);
        if (this.progressDialog != null) {
            this.progressDialog.dismiss();
            this.progressDialog = null;
        }
        this.mSdMountText = null;
        this.mUpdateBtn = null;
        if (this.serviceConnection != null) {
            Logger.d("SDSettingActivity", "onDestroy 0002");
            unbindService(this.serviceConnection);
            this.serviceConnection = null;
        }
        Logger.d("SDSettingActivity", "onDestroy 0003");
    }

    private boolean isSdMounted() {
        Logger.d("SDSettingActivity", "isSdMounted 0001");
        File updateFile = new File(this.mUpdatePath);
        StatFs stat = new StatFs("/mnt/extsd");
        DecimalFormat df = new DecimalFormat("#,###");
        long blockSize = stat.getBlockSize();
        long total = blockSize * ((long) stat.getBlockCount());
        Logger.d("SDSettingActivity", "isSdMounted 0002");
        return !"0".equals(df.format(total)) && updateFile.exists();
    }

    @Override
    public void onClick(View v) {
        Logger.d("SDSettingActivity", "onClick 0001");
        if (v.getId() == this.mUpdateBtn.getId()) {
            Logger.d("SDSettingActivity", "onClick 0002");
            if (isSdMounted()) {
                Logger.d("SDSettingActivity", "onClick 0003");
                this.mUpdateBtn.setClickable(false);
                OsUploadTask task = new OsUploadTask();
                task.execute(new String[0]);
            } else {
                Logger.d("SDSettingActivity", "onClick 0004");
                Toast.makeText(this, "SDカードがマウントされていません。", 0).show();
            }
        }
        Logger.d("SDSettingActivity", "onClick 0005");
    }

    class OsUploadTask extends AsyncTask<String, Integer, String> {
        OsUploadTask() {
        }

        @Override
        protected void onPreExecute() {
            Logger.d("SDSettingActivity", "onPreExecute 0001");
            SdUpdateActivity.this.progressDialog = new ProgressDialog(SdUpdateActivity.this);
            SdUpdateActivity.this.progressDialog.setTitle(SdUpdateActivity.this.getString(R.string.msg_sd_update_dialog_title));
            SdUpdateActivity.this.progressDialog.setMessage(SdUpdateActivity.this.getString(R.string.msg_sd_update_dialog_content));
            SdUpdateActivity.this.progressDialog.setIndeterminate(false);
            SdUpdateActivity.this.progressDialog.setProgressStyle(0);
            SdUpdateActivity.this.progressDialog.setCancelable(false);
            SdUpdateActivity.this.progressDialog.show();
            Logger.d("SDSettingActivity", "onPreExecute 0002");
        }

        @Override
        protected String doInBackground(String... params) {
            String str = null;
            Logger.d("SDSettingActivity", "doInBackground 0001");
            try {
                Logger.d("SDSettingActivity", "doInBackground 0002");
                if (SdUpdateActivity.this.dchaService.copyUpdateImage(SdUpdateActivity.this.mUpdatePath, "/cache/update.zip")) {
                    Logger.d("SDSettingActivity", "doInBackground 0003");
                    SdUpdateActivity.this.dchaService.rebootPad(2, "/cache/update.zip");
                    Logger.d("SDSettingActivity", "doInBackground 0006");
                    str = "success";
                } else {
                    Logger.d("SDSettingActivity", "doInBackground 0004");
                }
            } catch (Exception e) {
                Logger.d("SDSettingActivity", "doInBackground 0005");
                Logger.e("SDSettingActivity", "onClick", e);
            }
            return str;
        }

        @Override
        protected void onCancelled(String result) {
            Logger.d("SDSettingActivity", "onCancelled 0001");
            super.onCancelled(result);
            SdUpdateActivity.this.progressDialog.dismiss();
            if (SdUpdateActivity.this.progressDialog != null) {
                SdUpdateActivity.this.progressDialog = null;
            }
            Toast.makeText(SdUpdateActivity.this, SdUpdateActivity.this.getString(R.string.msg_sd_update_cancel), 0).show();
            Logger.d("SDSettingActivity", "onCancelled 0002");
        }

        @Override
        protected void onPostExecute(String result) {
            Logger.d("SDSettingActivity", "onPostExecute 0001");
            if (result != null) {
                Logger.d("SDSettingActivity", "onPostExecute 0002");
            } else {
                Logger.d("SDSettingActivity", "onPostExecute 0003");
                Toast.makeText(SdUpdateActivity.this, SdUpdateActivity.this.getString(R.string.msg_sd_update_failed), 0).show();
            }
            SdUpdateActivity.this.mUpdateBtn.setClickable(true);
            if (SdUpdateActivity.this.progressDialog != null) {
                SdUpdateActivity.this.progressDialog.dismiss();
                SdUpdateActivity.this.progressDialog = null;
            }
            Logger.d("SDSettingActivity", "onPostExecute 0004");
        }
    }
}
