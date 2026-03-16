package com.android.packageinstaller;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

public class UninstallerActivity extends Activity {
    private DialogInfo mDialogInfo;

    public static class UninstallAlertDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            PackageManager pm = getActivity().getPackageManager();
            DialogInfo dialogInfo = ((UninstallerActivity) getActivity()).mDialogInfo;
            CharSequence appLabel = dialogInfo.appInfo.loadLabel(pm);
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
            StringBuilder messageBuilder = new StringBuilder();
            if (dialogInfo.activityInfo != null) {
                Object activityLabel = dialogInfo.activityInfo.loadLabel(pm);
                if (!activityLabel.equals(appLabel)) {
                    messageBuilder.append(getString(R.string.uninstall_activity_text, new Object[]{activityLabel}));
                    messageBuilder.append(" ").append(appLabel).append(".\n\n");
                }
            }
            boolean isUpdate = (dialogInfo.appInfo.flags & 128) != 0;
            if (isUpdate) {
                messageBuilder.append(getString(R.string.uninstall_update_text));
            } else {
                UserManager userManager = UserManager.get(getActivity());
                if (dialogInfo.allUsers && userManager.getUserCount() >= 2) {
                    messageBuilder.append(getString(R.string.uninstall_application_text_all_users));
                } else if (!dialogInfo.user.equals(Process.myUserHandle())) {
                    UserInfo userInfo = userManager.getUserInfo(dialogInfo.user.getIdentifier());
                    messageBuilder.append(getString(R.string.uninstall_application_text_user, new Object[]{userInfo.name}));
                } else {
                    messageBuilder.append(getString(R.string.uninstall_application_text));
                }
            }
            dialogBuilder.setTitle(appLabel);
            dialogBuilder.setIcon(dialogInfo.appInfo.loadIcon(pm));
            dialogBuilder.setPositiveButton(android.R.string.ok, this);
            dialogBuilder.setNegativeButton(android.R.string.cancel, this);
            dialogBuilder.setMessage(messageBuilder.toString());
            return dialogBuilder.create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == -1) {
                ((UninstallerActivity) getActivity()).startUninstallProgress();
            } else {
                ((UninstallerActivity) getActivity()).dispatchAborted();
            }
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            getActivity().finish();
        }
    }

    public static class AppNotFoundDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity()).setTitle(R.string.app_not_found_dlg_title).setMessage(R.string.app_not_found_dlg_text).setNeutralButton(android.R.string.ok, (DialogInterface.OnClickListener) null).create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            ((UninstallerActivity) getActivity()).dispatchAborted();
            getActivity().setResult(1);
            getActivity().finish();
        }
    }

    static class DialogInfo {
        ActivityInfo activityInfo;
        boolean allUsers;
        ApplicationInfo appInfo;
        IBinder callback;
        UserHandle user;

        DialogInfo() {
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent intent = getIntent();
        Uri packageUri = intent.getData();
        if (packageUri == null) {
            Log.e("UninstallerActivity", "No package URI in intent");
            showAppNotFound();
            return;
        }
        String packageName = packageUri.getEncodedSchemeSpecificPart();
        if (packageName == null) {
            Log.e("UninstallerActivity", "Invalid package name in URI: " + packageUri);
            showAppNotFound();
            return;
        }
        IPackageManager pm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        this.mDialogInfo = new DialogInfo();
        this.mDialogInfo.user = (UserHandle) intent.getParcelableExtra("android.intent.extra.USER");
        if (this.mDialogInfo.user == null) {
            this.mDialogInfo.user = Process.myUserHandle();
        }
        this.mDialogInfo.allUsers = intent.getBooleanExtra("android.intent.extra.UNINSTALL_ALL_USERS", false);
        this.mDialogInfo.callback = intent.getIBinderExtra("android.content.pm.extra.CALLBACK");
        try {
            this.mDialogInfo.appInfo = pm.getApplicationInfo(packageName, 8192, this.mDialogInfo.user.getIdentifier());
        } catch (RemoteException e) {
            Log.e("UninstallerActivity", "Unable to get packageName. Package manager is dead?");
        }
        if (this.mDialogInfo.appInfo == null) {
            Log.e("UninstallerActivity", "Invalid packageName: " + packageName);
            showAppNotFound();
            return;
        }
        String className = packageUri.getFragment();
        if (className != null) {
            try {
                this.mDialogInfo.activityInfo = pm.getActivityInfo(new ComponentName(packageName, className), 0, this.mDialogInfo.user.getIdentifier());
            } catch (RemoteException e2) {
                Log.e("UninstallerActivity", "Unable to get className. Package manager is dead?");
            }
        }
        showConfirmationDialog();
    }

    private void showConfirmationDialog() {
        showDialogFragment(new UninstallAlertDialogFragment());
    }

    private void showAppNotFound() {
        showDialogFragment(new AppNotFoundDialogFragment());
    }

    private void showDialogFragment(DialogFragment fragment) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        fragment.show(ft, "dialog");
    }

    void startUninstallProgress() {
        Intent newIntent = new Intent("android.intent.action.VIEW");
        newIntent.putExtra("android.intent.extra.USER", this.mDialogInfo.user);
        newIntent.putExtra("android.intent.extra.UNINSTALL_ALL_USERS", this.mDialogInfo.allUsers);
        newIntent.putExtra("android.content.pm.extra.CALLBACK", this.mDialogInfo.callback);
        newIntent.putExtra("com.android.packageinstaller.applicationInfo", this.mDialogInfo.appInfo);
        if (getIntent().getBooleanExtra("android.intent.extra.RETURN_RESULT", false)) {
            newIntent.putExtra("android.intent.extra.RETURN_RESULT", true);
            newIntent.addFlags(33554432);
        }
        newIntent.setClass(this, UninstallAppProgress.class);
        startActivity(newIntent);
    }

    void dispatchAborted() {
        if (this.mDialogInfo != null && this.mDialogInfo.callback != null) {
            IPackageDeleteObserver2 observer = IPackageDeleteObserver2.Stub.asInterface(this.mDialogInfo.callback);
            try {
                observer.onPackageDeleted(this.mDialogInfo.appInfo.packageName, -5, "Cancelled by user");
            } catch (RemoteException e) {
            }
        }
    }
}
