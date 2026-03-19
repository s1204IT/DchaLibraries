package com.mediatek.calendarimporter;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.mediatek.calendarimporter.BindServiceHelper;
import com.mediatek.calendarimporter.service.PreviewProcessor;
import com.mediatek.calendarimporter.service.VCalService;
import com.mediatek.calendarimporter.utils.LogUtils;
import com.mediatek.calendarimporter.utils.Utils;

public class ShowPreviewActivity extends Activity implements BindServiceHelper.ServiceConnectedOperation {
    static final int DURATION = 5000;
    private static final String TAG = "ShowPreviewActivity";
    private Button mCancelButton;
    private Button mErrorCertainButton;
    private Button mImportButton;
    private ImageView mImportErrorIcon;
    private Intent mIntent;
    private View mLoadingView;
    private View mMainPreviewView;
    private TextView mPreviewText;
    private PreviewProcessor mProcessor;
    private VCalService mService;
    private BindServiceHelper mServiceHelper;
    private TextView mTitleTextView;
    private Uri mUri;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        LogUtils.d(TAG, "onCreate.");
        setContentView(R.layout.view_calander);
        this.mIntent = getIntent();
        this.mUri = this.mIntent.getData();
        this.mMainPreviewView = findViewById(R.id.preview_activity);
        this.mServiceHelper = new BindServiceHelper(this);
        this.mServiceHelper.onBindService();
        this.mTitleTextView = (TextView) findViewById(R.id.preview_title);
        this.mPreviewText = (TextView) findViewById(R.id.calendar_value);
        int themeMainColor = Utils.getThemeMainColor(this, 17170450);
        if (themeMainColor != 17170450) {
            this.mTitleTextView.setTextColor(themeMainColor);
            findViewById(R.id.preview_divide_line).setBackgroundColor(themeMainColor);
        }
        this.mLoadingView = findViewById(R.id.preview_loading);
        this.mImportErrorIcon = (ImageView) findViewById(R.id.import_error_icon);
        this.mErrorCertainButton = (Button) findViewById(R.id.import_error_certain);
        this.mErrorCertainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShowPreviewActivity.this.finish();
            }
        });
        this.mImportButton = (Button) findViewById(R.id.button_ok);
        this.mImportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Utils.hasExchangeOrGoogleAccount(ShowPreviewActivity.this)) {
                    LogUtils.d(ShowPreviewActivity.TAG, "onResume,show SelectActivity... ");
                    ShowPreviewActivity.this.showSelectActivity();
                    ShowPreviewActivity.this.finish();
                } else {
                    if (BenesseExtension.getDchaState() != 0) {
                        return;
                    }
                    Toast.makeText(ShowPreviewActivity.this, R.string.no_access_toast, ShowPreviewActivity.DURATION).show();
                    Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
                    intent.putExtra("authorities", new String[]{"com.android.calendar"});
                    intent.addFlags(67108864);
                    LogUtils.d(ShowPreviewActivity.TAG, "onResume,Show Settings... ");
                    ShowPreviewActivity.this.startActivity(intent);
                }
            }
        });
        this.mCancelButton = (Button) findViewById(R.id.button_cancel);
        this.mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShowPreviewActivity.this.finish();
            }
        });
    }

    private void showSelectActivity() {
        Intent intent = new Intent(getIntent());
        intent.setClass(this, HandleProgressActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        LogUtils.d(TAG, "onDestroy() + mService:" + this.mService);
        if (this.mService != null) {
            this.mService.tryCancelProcessor(this.mProcessor);
        }
        if (this.mServiceHelper != null) {
            this.mServiceHelper.unBindService();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        LogUtils.d(TAG, "onBackPressed.");
        if (this.mProcessor != null && this.mService != null) {
            this.mService.tryCancelProcessor(this.mProcessor);
        }
        super.onBackPressed();
    }

    @Override
    public void serviceConnected(VCalService vCalService) {
        LogUtils.d(TAG, "serviceConnected.");
        this.mService = vCalService;
        this.mProcessor = new PreviewProcessor(this, this.mUri, new Handler() {
            @Override
            public void handleMessage(Message message) {
                int i = message.what;
                if (i == -1) {
                    LogUtils.d(ShowPreviewActivity.TAG, "serviceConnected,handlerMessage:EXCEPTION happened.");
                    ShowPreviewActivity.this.setImportErrorView(R.string.import_vcs_failed);
                    return;
                }
                if (i == 1) {
                    LogUtils.d(ShowPreviewActivity.TAG, "serviceConnected,handlerMessage : " + message.arg1 + "/" + message.arg2 + " " + message.obj);
                    if (message.arg2 > 1) {
                        ShowPreviewActivity.this.setImportErrorView(R.string.not_support_multi_events);
                        return;
                    }
                    ShowPreviewActivity.this.mLoadingView.setVisibility(8);
                    ShowPreviewActivity.this.mPreviewText.setText((String) message.obj);
                    ShowPreviewActivity.this.mPreviewText.setVisibility(0);
                    ShowPreviewActivity.this.mImportButton.setVisibility(0);
                    ShowPreviewActivity.this.mMainPreviewView.setVisibility(0);
                }
            }
        });
        this.mService.tryExecuteProcessor(this.mProcessor);
    }

    @Override
    public void serviceUnConnected() {
        LogUtils.d(TAG, "serviceUnConnected.");
        this.mService = null;
    }

    private void setImportErrorView(int i) {
        this.mPreviewText.setVisibility(0);
        this.mCancelButton.setVisibility(8);
        this.mImportButton.setVisibility(8);
        this.mTitleTextView.setText(android.R.string.dialog_alert_title);
        this.mPreviewText.setText(i);
        this.mErrorCertainButton.setVisibility(0);
        this.mImportErrorIcon.setVisibility(0);
        this.mLoadingView.setVisibility(8);
        this.mMainPreviewView.setVisibility(0);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        LogUtils.d(TAG, "onNewIntent, mServiceHelper = " + this.mServiceHelper);
        if (this.mServiceHelper == null) {
            return;
        }
        this.mServiceHelper.unBindService();
        this.mIntent = intent;
        this.mUri = intent.getData();
        this.mServiceHelper.onBindService();
    }
}
