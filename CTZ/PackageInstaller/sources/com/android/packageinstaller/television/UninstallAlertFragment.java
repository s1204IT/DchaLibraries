package com.android.packageinstaller.television;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
import android.os.UserManager;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import com.android.packageinstaller.R;
import com.android.packageinstaller.UninstallerActivity;
import java.util.List;

public class UninstallAlertFragment extends GuidedStepFragment {
    @Override
    public int onProvideTheme() {
        return R.style.Theme_Leanback_GuidedStep;
    }

    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle bundle) {
        PackageManager packageManager = getActivity().getPackageManager();
        UninstallerActivity.DialogInfo dialogInfo = ((UninstallerActivity) getActivity()).getDialogInfo();
        CharSequence charSequenceLoadSafeLabel = dialogInfo.appInfo.loadSafeLabel(packageManager);
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
        return new GuidanceStylist.Guidance(charSequenceLoadSafeLabel.toString(), sb.toString(), null, dialogInfo.appInfo.loadIcon(packageManager));
    }

    @Override
    public void onCreateActions(List<GuidedAction> list, Bundle bundle) {
        list.add(new GuidedAction.Builder(getContext()).clickAction(-4L).build());
        list.add(new GuidedAction.Builder(getContext()).clickAction(-5L).build());
    }

    @Override
    public void onGuidedActionClicked(GuidedAction guidedAction) {
        if (isAdded()) {
            if (guidedAction.getId() == -4) {
                ((UninstallerActivity) getActivity()).startUninstallProgress();
                getActivity().finish();
            } else {
                ((UninstallerActivity) getActivity()).dispatchAborted();
                getActivity().setResult(1);
                getActivity().finish();
            }
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
