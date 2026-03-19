package com.android.packageinstaller.handheld;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
import android.os.UserManager;
import com.android.packageinstaller.R;
import com.android.packageinstaller.UninstallerActivity;

public class UninstallAlertDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        PackageManager packageManager = getActivity().getPackageManager();
        UninstallerActivity.DialogInfo dialogInfo = ((UninstallerActivity) getActivity()).getDialogInfo();
        CharSequence charSequenceLoadSafeLabel = dialogInfo.appInfo.loadSafeLabel(packageManager);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        StringBuilder sb = new StringBuilder();
        if (dialogInfo.activityInfo != null) {
            Object objLoadSafeLabel = dialogInfo.activityInfo.loadSafeLabel(packageManager);
            if (!objLoadSafeLabel.equals(charSequenceLoadSafeLabel)) {
                sb.append(getString(R.string.uninstall_activity_text, new Object[]{objLoadSafeLabel}));
                sb.append(" ");
                sb.append(charSequenceLoadSafeLabel);
                sb.append(".\n\n");
            }
        }
        boolean z = (dialogInfo.appInfo.flags & 128) != 0;
        UserManager userManager = UserManager.get(getActivity());
        if (z) {
            if (isSingleUser(userManager)) {
                sb.append(getString(R.string.uninstall_update_text));
            } else {
                sb.append(getString(R.string.uninstall_update_text_multiuser));
            }
        } else if (dialogInfo.allUsers && !isSingleUser(userManager)) {
            sb.append(getString(R.string.uninstall_application_text_all_users));
        } else if (!dialogInfo.user.equals(Process.myUserHandle())) {
            sb.append(getString(R.string.uninstall_application_text_user, new Object[]{userManager.getUserInfo(dialogInfo.user.getIdentifier()).name}));
        } else {
            sb.append(getString(R.string.uninstall_application_text));
        }
        builder.setTitle(charSequenceLoadSafeLabel);
        builder.setPositiveButton(android.R.string.ok, this);
        builder.setNegativeButton(android.R.string.cancel, this);
        builder.setMessage(sb.toString());
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -1) {
            ((UninstallerActivity) getActivity()).startUninstallProgress();
        } else {
            ((UninstallerActivity) getActivity()).dispatchAborted();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
        if (isAdded()) {
            getActivity().finish();
        }
    }

    private boolean isSingleUser(UserManager userManager) {
        int userCount = userManager.getUserCount();
        if (userCount != 1) {
            return UserManager.isSplitSystemUser() && userCount == 2;
        }
        return true;
    }
}
