package com.android.packageinstaller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.IPackageManager;
import android.content.pm.UserInfo;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class UninstallAppProgress extends Activity implements View.OnClickListener {
    private boolean mAllUsers;
    private ApplicationInfo mAppInfo;
    private IBinder mCallback;
    private Button mDeviceManagerButton;
    private Button mOkButton;
    private View mOkPanel;
    private ProgressBar mProgressBar;
    private TextView mStatusTextView;
    private UserHandle mUser;
    private final String TAG = "UninstallAppProgress";
    private boolean localLOGV = false;
    private volatile int mResultCode = -1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String statusText;
            switch (msg.what) {
                case 1:
                    UninstallAppProgress.this.mResultCode = msg.arg1;
                    String packageName = (String) msg.obj;
                    if (UninstallAppProgress.this.mCallback != null) {
                        IPackageDeleteObserver2 observer = IPackageDeleteObserver2.Stub.asInterface(UninstallAppProgress.this.mCallback);
                        try {
                            observer.onPackageDeleted(UninstallAppProgress.this.mAppInfo.packageName, UninstallAppProgress.this.mResultCode, packageName);
                            break;
                        } catch (RemoteException e) {
                        }
                        UninstallAppProgress.this.finish();
                    } else if (UninstallAppProgress.this.getIntent().getBooleanExtra("android.intent.extra.RETURN_RESULT", false)) {
                        Intent result = new Intent();
                        result.putExtra("android.intent.extra.INSTALL_RESULT", UninstallAppProgress.this.mResultCode);
                        UninstallAppProgress.this.setResult(UninstallAppProgress.this.mResultCode == 1 ? -1 : 1, result);
                        UninstallAppProgress.this.finish();
                    } else {
                        switch (msg.arg1) {
                            case -4:
                                UserManager userManager = (UserManager) UninstallAppProgress.this.getSystemService("user");
                                IPackageManager packageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
                                List<UserInfo> users = userManager.getUsers();
                                int blockingUserId = -10000;
                                for (int i = 0; i < users.size(); i++) {
                                    UserInfo user = users.get(i);
                                    try {
                                        if (packageManager.getBlockUninstallForUser(packageName, user.id)) {
                                            blockingUserId = user.id;
                                            UninstallAppProgress.this.mDeviceManagerButton.setVisibility(0);
                                            if (blockingUserId != 0) {
                                                statusText = UninstallAppProgress.this.getString(R.string.uninstall_blocked_device_owner);
                                            } else if (blockingUserId == -10000) {
                                                Log.d("UninstallAppProgress", "Uninstall failed for " + packageName + " with code " + msg.arg1 + " no blocking user");
                                                statusText = UninstallAppProgress.this.getString(R.string.uninstall_failed);
                                            } else {
                                                String userName = userManager.getUserInfo(blockingUserId).name;
                                                statusText = String.format(UninstallAppProgress.this.getString(R.string.uninstall_blocked_profile_owner), userName);
                                            }
                                        } else {
                                            continue;
                                        }
                                    } catch (RemoteException e2) {
                                        Log.e("UninstallAppProgress", "Failed to talk to package manager", e2);
                                    }
                                    break;
                                }
                                UninstallAppProgress.this.mDeviceManagerButton.setVisibility(0);
                                if (blockingUserId != 0) {
                                }
                                break;
                            case -3:
                            case -1:
                            case 0:
                            default:
                                Log.d("UninstallAppProgress", "Uninstall failed for " + packageName + " with code " + msg.arg1);
                                statusText = UninstallAppProgress.this.getString(R.string.uninstall_failed);
                                break;
                            case -2:
                                Log.d("UninstallAppProgress", "Uninstall failed because " + packageName + " is a device admin");
                                UninstallAppProgress.this.mDeviceManagerButton.setVisibility(0);
                                statusText = UninstallAppProgress.this.getString(R.string.uninstall_failed_device_policy_manager);
                                break;
                            case 1:
                                String statusText2 = UninstallAppProgress.this.getString(R.string.uninstall_done);
                                Context ctx = UninstallAppProgress.this.getBaseContext();
                                Toast.makeText(ctx, statusText2, 1).show();
                                UninstallAppProgress.this.setResultAndFinish(UninstallAppProgress.this.mResultCode);
                                break;
                        }
                        UninstallAppProgress.this.mStatusTextView.setText(statusText);
                        UninstallAppProgress.this.mProgressBar.setVisibility(4);
                        UninstallAppProgress.this.mOkPanel.setVisibility(0);
                    }
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent intent = getIntent();
        this.mAppInfo = (ApplicationInfo) intent.getParcelableExtra("com.android.packageinstaller.applicationInfo");
        this.mAllUsers = intent.getBooleanExtra("android.intent.extra.UNINSTALL_ALL_USERS", false);
        if (this.mAllUsers && UserHandle.myUserId() != 0) {
            throw new SecurityException("Only owner user can request uninstall for all users");
        }
        this.mUser = (UserHandle) intent.getParcelableExtra("android.intent.extra.USER");
        if (this.mUser == null) {
            this.mUser = Process.myUserHandle();
        } else {
            UserManager userManager = (UserManager) getSystemService("user");
            List<UserHandle> profiles = userManager.getUserProfiles();
            if (!profiles.contains(this.mUser)) {
                throw new SecurityException("User " + Process.myUserHandle() + " can't request uninstall for user " + this.mUser);
            }
        }
        this.mCallback = intent.getIBinderExtra("android.content.pm.extra.CALLBACK");
        initView();
    }

    class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        PackageDeleteObserver() {
        }

        public void packageDeleted(String packageName, int returnCode) {
            Message msg = UninstallAppProgress.this.mHandler.obtainMessage(1);
            msg.arg1 = returnCode;
            msg.obj = packageName;
            UninstallAppProgress.this.mHandler.sendMessage(msg);
        }
    }

    void setResultAndFinish(int retCode) {
        setResult(retCode);
        finish();
    }

    public void initView() {
        boolean isUpdate = (this.mAppInfo.flags & 128) != 0;
        setTitle(isUpdate ? R.string.uninstall_update_title : R.string.uninstall_application_title);
        setContentView(R.layout.uninstall_progress);
        View snippetView = findViewById(R.id.app_snippet);
        PackageUtil.initSnippetForInstalledApp(this, this.mAppInfo, snippetView);
        this.mStatusTextView = (TextView) findViewById(R.id.center_text);
        this.mStatusTextView.setText(R.string.uninstalling);
        this.mDeviceManagerButton = (Button) findViewById(R.id.device_manager_button);
        this.mDeviceManagerButton.setVisibility(8);
        this.mDeviceManagerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BenesseExtension.getDchaState() != 0) {
                    UninstallAppProgress.this.finish();
                    return;
                }
                Intent intent = new Intent();
                intent.setClassName("com.android.settings", "com.android.settings.Settings$DeviceAdminSettingsActivity");
                intent.setFlags(1342177280);
                UninstallAppProgress.this.startActivity(intent);
                UninstallAppProgress.this.finish();
            }
        });
        this.mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        this.mProgressBar.setIndeterminate(true);
        this.mOkPanel = findViewById(R.id.ok_panel);
        this.mOkButton = (Button) findViewById(R.id.ok_button);
        this.mOkButton.setOnClickListener(this);
        this.mOkPanel.setVisibility(4);
        IPackageManager packageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        PackageDeleteObserver observer = new PackageDeleteObserver();
        try {
            packageManager.deletePackageAsUser(this.mAppInfo.packageName, observer, this.mUser.getIdentifier(), this.mAllUsers ? 2 : 0);
        } catch (RemoteException e) {
            Log.e("UninstallAppProgress", "Failed to talk to package manager", e);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == this.mOkButton) {
            Log.i("UninstallAppProgress", "Finished uninstalling pkg: " + this.mAppInfo.packageName);
            setResultAndFinish(this.mResultCode);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent ev) {
        if (ev.getKeyCode() == 4) {
            if (this.mResultCode == -1) {
                return true;
            }
            setResult(this.mResultCode);
        }
        return super.dispatchKeyEvent(ev);
    }
}
