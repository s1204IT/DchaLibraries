package com.android.packageinstaller;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.ManifestDigest;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.VerificationParams;
import android.graphics.drawable.LevelListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.packageinstaller.PackageUtil;
import java.io.File;
import java.util.List;

public class InstallAppProgress extends Activity implements DialogInterface.OnCancelListener, View.OnClickListener {
    private ApplicationInfo mAppInfo;
    private Button mDoneButton;
    private TextView mExplanationTextView;
    private InstallFlowAnalytics mInstallFlowAnalytics;
    private CharSequence mLabel;
    private Button mLaunchButton;
    private Intent mLaunchIntent;
    private View mOkPanel;
    private Uri mPackageURI;
    private ProgressBar mProgressBar;
    private TextView mStatusTextView;
    private final String TAG = "InstallAppProgress";
    private boolean localLOGV = false;
    private final int INSTALL_COMPLETE = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int centerTextLabel;
            List<ResolveInfo> list;
            switch (msg.what) {
                case 1:
                    InstallAppProgress.this.mInstallFlowAnalytics.setFlowFinishedWithPackageManagerResult(msg.arg1);
                    if (!InstallAppProgress.this.getIntent().getBooleanExtra("android.intent.extra.RETURN_RESULT", false)) {
                        InstallAppProgress.this.mProgressBar.setVisibility(4);
                        int centerExplanationLabel = -1;
                        LevelListDrawable centerTextDrawable = (LevelListDrawable) InstallAppProgress.this.getDrawable(R.drawable.ic_result_status);
                        if (msg.arg1 == 1) {
                            InstallAppProgress.this.mLaunchButton.setVisibility(0);
                            centerTextDrawable.setLevel(0);
                            centerTextLabel = R.string.install_done;
                            InstallAppProgress.this.mLaunchIntent = InstallAppProgress.this.getPackageManager().getLaunchIntentForPackage(InstallAppProgress.this.mAppInfo.packageName);
                            boolean enabled = false;
                            if (InstallAppProgress.this.mLaunchIntent != null && (list = InstallAppProgress.this.getPackageManager().queryIntentActivities(InstallAppProgress.this.mLaunchIntent, 0)) != null && list.size() > 0) {
                                enabled = true;
                            }
                            if (enabled) {
                                InstallAppProgress.this.mLaunchButton.setOnClickListener(InstallAppProgress.this);
                            } else {
                                InstallAppProgress.this.mLaunchButton.setEnabled(false);
                            }
                        } else {
                            if (msg.arg1 == -4) {
                                InstallAppProgress.this.showDialogInner(1);
                            } else {
                                centerTextDrawable.setLevel(1);
                                centerExplanationLabel = InstallAppProgress.this.getExplanationFromErrorCode(msg.arg1);
                                centerTextLabel = R.string.install_failed;
                                InstallAppProgress.this.mLaunchButton.setVisibility(4);
                            }
                            break;
                        }
                        if (centerTextDrawable != null) {
                            centerTextDrawable.setBounds(0, 0, centerTextDrawable.getIntrinsicWidth(), centerTextDrawable.getIntrinsicHeight());
                            InstallAppProgress.this.mStatusTextView.setCompoundDrawablesRelative(centerTextDrawable, null, null, null);
                        }
                        InstallAppProgress.this.mStatusTextView.setText(centerTextLabel);
                        if (centerExplanationLabel != -1) {
                            InstallAppProgress.this.mExplanationTextView.setText(centerExplanationLabel);
                            InstallAppProgress.this.mExplanationTextView.setVisibility(0);
                        } else {
                            InstallAppProgress.this.mExplanationTextView.setVisibility(8);
                        }
                        InstallAppProgress.this.mDoneButton.setOnClickListener(InstallAppProgress.this);
                        InstallAppProgress.this.mOkPanel.setVisibility(0);
                    } else {
                        Intent result = new Intent();
                        result.putExtra("android.intent.extra.INSTALL_RESULT", msg.arg1);
                        InstallAppProgress.this.setResult(msg.arg1 != 1 ? 1 : -1, result);
                        InstallAppProgress.this.finish();
                    }
                    break;
            }
        }
    };

    private int getExplanationFromErrorCode(int errCode) {
        Log.d("InstallAppProgress", "Installation error code: " + errCode);
        switch (errCode) {
            case -104:
                return R.string.install_failed_inconsistent_certificates;
            case -16:
                return R.string.install_failed_cpu_abi_incompatible;
            case -12:
                return R.string.install_failed_older_sdk;
            case -2:
                return R.string.install_failed_invalid_apk;
            default:
                return -1;
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent intent = getIntent();
        this.mAppInfo = (ApplicationInfo) intent.getParcelableExtra("com.android.packageinstaller.applicationInfo");
        this.mInstallFlowAnalytics = (InstallFlowAnalytics) intent.getParcelableExtra("com.android.packageinstaller.extras.install_flow_analytics");
        this.mInstallFlowAnalytics.setContext(this);
        this.mPackageURI = intent.getData();
        String scheme = this.mPackageURI.getScheme();
        if (scheme != null && !"file".equals(scheme) && !"package".equals(scheme)) {
            this.mInstallFlowAnalytics.setFlowFinished((byte) 1);
            throw new IllegalArgumentException("unexpected scheme " + scheme);
        }
        initView();
    }

    @Override
    public Dialog onCreateDialog(int id, Bundle bundle) {
        switch (id) {
            case 1:
                String dlgText = getString(R.string.out_of_space_dlg_text, new Object[]{this.mLabel});
                return new AlertDialog.Builder(this).setTitle(R.string.out_of_space_dlg_title).setMessage(dlgText).setPositiveButton(R.string.manage_applications, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent("android.intent.action.MANAGE_PACKAGE_STORAGE");
                        InstallAppProgress.this.startActivity(intent);
                        InstallAppProgress.this.finish();
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("InstallAppProgress", "Canceling installation");
                        InstallAppProgress.this.finish();
                    }
                }).setOnCancelListener(this).create();
            default:
                return null;
        }
    }

    private void showDialogInner(int id) {
        removeDialog(id);
        showDialog(id);
    }

    class PackageInstallObserver extends IPackageInstallObserver.Stub {
        PackageInstallObserver() {
        }

        public void packageInstalled(String packageName, int returnCode) {
            Message msg = InstallAppProgress.this.mHandler.obtainMessage(1);
            msg.arg1 = returnCode;
            InstallAppProgress.this.mHandler.sendMessage(msg);
        }
    }

    public void initView() {
        PackageUtil.AppSnippet as;
        setContentView(R.layout.op_progress);
        int installFlags = 0;
        PackageManager pm = getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(this.mAppInfo.packageName, 8192);
            if (pi != null) {
                installFlags = 0 | 2;
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        if ((installFlags & 2) != 0) {
            Log.w("InstallAppProgress", "Replacing package:" + this.mAppInfo.packageName);
        }
        if ("package".equals(this.mPackageURI.getScheme())) {
            as = new PackageUtil.AppSnippet(pm.getApplicationLabel(this.mAppInfo), pm.getApplicationIcon(this.mAppInfo));
        } else {
            File sourceFile = new File(this.mPackageURI.getPath());
            as = PackageUtil.getAppSnippet(this, this.mAppInfo, sourceFile);
        }
        this.mLabel = as.label;
        PackageUtil.initSnippetForNewApp(this, as, R.id.app_snippet);
        this.mStatusTextView = (TextView) findViewById(R.id.center_text);
        this.mStatusTextView.setText(R.string.installing);
        this.mExplanationTextView = (TextView) findViewById(R.id.center_explanation);
        this.mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        this.mProgressBar.setIndeterminate(true);
        this.mOkPanel = findViewById(R.id.buttons_panel);
        this.mDoneButton = (Button) findViewById(R.id.done_button);
        this.mLaunchButton = (Button) findViewById(R.id.launch_button);
        this.mOkPanel.setVisibility(4);
        String installerPackageName = getIntent().getStringExtra("android.intent.extra.INSTALLER_PACKAGE_NAME");
        Uri originatingURI = (Uri) getIntent().getParcelableExtra("android.intent.extra.ORIGINATING_URI");
        Uri referrer = (Uri) getIntent().getParcelableExtra("android.intent.extra.REFERRER");
        int originatingUid = getIntent().getIntExtra("android.intent.extra.ORIGINATING_UID", -1);
        ManifestDigest manifestDigest = getIntent().getParcelableExtra("com.android.packageinstaller.extras.manifest_digest");
        VerificationParams verificationParams = new VerificationParams((Uri) null, originatingURI, referrer, originatingUid, manifestDigest);
        PackageInstallObserver observer = new PackageInstallObserver();
        if ("package".equals(this.mPackageURI.getScheme())) {
            try {
                pm.installExistingPackage(this.mAppInfo.packageName);
                observer.packageInstalled(this.mAppInfo.packageName, 1);
                return;
            } catch (PackageManager.NameNotFoundException e2) {
                observer.packageInstalled(this.mAppInfo.packageName, -2);
                return;
            }
        }
        pm.installPackageWithVerificationAndEncryption(this.mPackageURI, observer, installFlags, installerPackageName, verificationParams, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v == this.mDoneButton) {
            if (this.mAppInfo.packageName != null) {
                Log.i("InstallAppProgress", "Finished installing " + this.mAppInfo.packageName);
            }
            finish();
        } else if (v == this.mLaunchButton) {
            startActivity(this.mLaunchIntent);
            finish();
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }
}
