package com.android.packageinstaller.television;

import android.app.Activity;
import android.app.admin.IDevicePolicyManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.IPackageManager;
import android.content.pm.UserInfo;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.widget.Toast;
import com.android.packageinstaller.R;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;

public class UninstallAppProgress extends Activity {
    private boolean mAllUsers;
    private ApplicationInfo mAppInfo;
    private IBinder mCallback;
    private boolean mIsViewInitialized;
    private volatile int mResultCode = -1;
    private Handler mHandler = new MessageHandler(this);

    public interface ProgressFragment {
        void setDeviceManagerButtonVisible(boolean z);

        void setUsersButtonVisible(boolean z);

        void showCompletion(CharSequence charSequence);
    }

    private static class MessageHandler extends Handler {
        private final WeakReference<UninstallAppProgress> mActivity;

        public MessageHandler(UninstallAppProgress uninstallAppProgress) {
            this.mActivity = new WeakReference<>(uninstallAppProgress);
        }

        @Override
        public void handleMessage(Message message) {
            UninstallAppProgress uninstallAppProgress = this.mActivity.get();
            if (uninstallAppProgress != null) {
                uninstallAppProgress.handleMessage(message);
            }
        }
    }

    private void handleMessage(Message message) {
        int i;
        String string;
        if (isFinishing() || isDestroyed()) {
            return;
        }
        switch (message.what) {
            case DialogFragment.STYLE_NO_TITLE:
                this.mHandler.removeMessages(2);
                if (message.arg1 != 1) {
                    initView();
                }
                this.mResultCode = message.arg1;
                String str = (String) message.obj;
                if (this.mCallback != null) {
                    try {
                        IPackageDeleteObserver2.Stub.asInterface(this.mCallback).onPackageDeleted(this.mAppInfo.packageName, this.mResultCode, str);
                        break;
                    } catch (RemoteException e) {
                    }
                    finish();
                } else if (getIntent().getBooleanExtra("android.intent.extra.RETURN_RESULT", false)) {
                    Intent intent = new Intent();
                    intent.putExtra("android.intent.extra.INSTALL_RESULT", this.mResultCode);
                    setResult(this.mResultCode == 1 ? -1 : 1, intent);
                    finish();
                } else {
                    int i2 = message.arg1;
                    if (i2 != -4) {
                        if (i2 != -2) {
                            if (i2 == 1) {
                                Toast.makeText(getBaseContext(), getString(R.string.uninstall_done), 1).show();
                                setResultAndFinish();
                            } else {
                                Log.d("UninstallAppProgress", "Uninstall failed for " + str + " with code " + message.arg1);
                                string = getString(R.string.uninstall_failed);
                            }
                            break;
                        } else {
                            UserManager userManager = (UserManager) getSystemService("user");
                            IDevicePolicyManager iDevicePolicyManagerAsInterface = IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"));
                            int iMyUserId = UserHandle.myUserId();
                            UserInfo userInfo = null;
                            Iterator it = userManager.getUsers().iterator();
                            while (true) {
                                if (it.hasNext()) {
                                    UserInfo userInfo2 = (UserInfo) it.next();
                                    if (!isProfileOfOrSame(userManager, iMyUserId, userInfo2.id)) {
                                        try {
                                            if (iDevicePolicyManagerAsInterface.packageHasActiveAdmins(str, userInfo2.id)) {
                                                userInfo = userInfo2;
                                            }
                                        } catch (RemoteException e2) {
                                            Log.e("UninstallAppProgress", "Failed to talk to package manager", e2);
                                        }
                                    }
                                }
                            }
                            if (userInfo == null) {
                                Log.d("UninstallAppProgress", "Uninstall failed because " + str + " is a device admin");
                                getProgressFragment().setDeviceManagerButtonVisible(true);
                                string = getString(R.string.uninstall_failed_device_policy_manager);
                            } else {
                                Log.d("UninstallAppProgress", "Uninstall failed because " + str + " is a device admin of user " + userInfo);
                                getProgressFragment().setDeviceManagerButtonVisible(false);
                                string = String.format(getString(R.string.uninstall_failed_device_policy_manager_of_user), userInfo.name);
                            }
                        }
                    } else {
                        UserManager userManager2 = (UserManager) getSystemService("user");
                        IPackageManager iPackageManagerAsInterface = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
                        List users = userManager2.getUsers();
                        int i3 = 0;
                        while (true) {
                            if (i3 < users.size()) {
                                UserInfo userInfo3 = (UserInfo) users.get(i3);
                                try {
                                    if (iPackageManagerAsInterface.getBlockUninstallForUser(str, userInfo3.id)) {
                                        i = userInfo3.id;
                                    } else {
                                        continue;
                                    }
                                } catch (RemoteException e3) {
                                    Log.e("UninstallAppProgress", "Failed to talk to package manager", e3);
                                }
                                i3++;
                            } else {
                                i = -10000;
                            }
                        }
                        if (isProfileOfOrSame(userManager2, UserHandle.myUserId(), i)) {
                            getProgressFragment().setDeviceManagerButtonVisible(true);
                        } else {
                            getProgressFragment().setDeviceManagerButtonVisible(false);
                            getProgressFragment().setUsersButtonVisible(true);
                        }
                        if (i == 0) {
                            string = getString(R.string.uninstall_blocked_device_owner);
                        } else if (i == -10000) {
                            Log.d("UninstallAppProgress", "Uninstall failed for " + str + " with code " + message.arg1 + " no blocking user");
                            string = getString(R.string.uninstall_failed);
                        } else if (this.mAllUsers) {
                            string = getString(R.string.uninstall_all_blocked_profile_owner);
                        } else {
                            string = getString(R.string.uninstall_blocked_profile_owner);
                        }
                    }
                    getProgressFragment().showCompletion(string);
                }
                break;
            case DialogFragment.STYLE_NO_FRAME:
                initView();
                break;
        }
    }

    private boolean isProfileOfOrSame(UserManager userManager, int i, int i2) {
        if (i == i2) {
            return true;
        }
        UserInfo profileParent = userManager.getProfileParent(i2);
        return profileParent != null && profileParent.id == i;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getIntent();
        this.mAppInfo = (ApplicationInfo) intent.getParcelableExtra("com.android.packageinstaller.applicationInfo");
        this.mCallback = intent.getIBinderExtra("android.content.pm.extra.CALLBACK");
        if (bundle != null) {
            this.mResultCode = -1;
            if (this.mCallback != null) {
                try {
                    IPackageDeleteObserver2.Stub.asInterface(this.mCallback).onPackageDeleted(this.mAppInfo.packageName, this.mResultCode, (String) null);
                } catch (RemoteException e) {
                }
                finish();
                return;
            } else {
                setResultAndFinish();
                return;
            }
        }
        this.mAllUsers = intent.getBooleanExtra("android.intent.extra.UNINSTALL_ALL_USERS", false);
        UserHandle userHandleMyUserHandle = (UserHandle) intent.getParcelableExtra("android.intent.extra.USER");
        if (userHandleMyUserHandle == null) {
            userHandleMyUserHandle = Process.myUserHandle();
        }
        PackageDeleteObserver packageDeleteObserver = new PackageDeleteObserver();
        getWindow().setBackgroundDrawable(new ColorDrawable(0));
        getWindow().setStatusBarColor(0);
        getWindow().setNavigationBarColor(0);
        try {
            getPackageManager().deletePackageAsUser(this.mAppInfo.packageName, packageDeleteObserver, this.mAllUsers ? 2 : 0, userHandleMyUserHandle.getIdentifier());
        } catch (IllegalArgumentException e2) {
            Log.w("UninstallAppProgress", "Could not find package, not deleting " + this.mAppInfo.packageName, e2);
        }
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2), 500L);
    }

    public ApplicationInfo getAppInfo() {
        return this.mAppInfo;
    }

    private class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        private PackageDeleteObserver() {
        }

        public void packageDeleted(String str, int i) {
            Message messageObtainMessage = UninstallAppProgress.this.mHandler.obtainMessage(1);
            messageObtainMessage.arg1 = i;
            messageObtainMessage.obj = str;
            UninstallAppProgress.this.mHandler.sendMessage(messageObtainMessage);
        }
    }

    public void setResultAndFinish() {
        setResult(this.mResultCode);
        finish();
    }

    private void initView() {
        if (this.mIsViewInitialized) {
            return;
        }
        this.mIsViewInitialized = true;
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true);
        if (typedValue.type >= 28 && typedValue.type <= 31) {
            getWindow().setBackgroundDrawable(new ColorDrawable(typedValue.data));
        } else {
            getWindow().setBackgroundDrawable(getResources().getDrawable(typedValue.resourceId, getTheme()));
        }
        getTheme().resolveAttribute(android.R.attr.navigationBarColor, typedValue, true);
        getWindow().setNavigationBarColor(typedValue.data);
        getTheme().resolveAttribute(android.R.attr.statusBarColor, typedValue, true);
        getWindow().setStatusBarColor(typedValue.data);
        setTitle((this.mAppInfo.flags & 128) != 0 ? R.string.uninstall_update_title : R.string.uninstall_application_title);
        getFragmentManager().beginTransaction().add(android.R.id.content, new UninstallAppProgressFragment(), "progress_fragment").commitNowAllowingStateLoss();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == 4) {
            if (this.mResultCode == -1) {
                return true;
            }
            setResult(this.mResultCode);
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    private ProgressFragment getProgressFragment() {
        return (ProgressFragment) getFragmentManager().findFragmentByTag("progress_fragment");
    }
}
