package com.android.managedprovisioning.preprovisioning;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.MdmPackageInfo;
import com.android.managedprovisioning.common.SettingsFacade;
import com.android.managedprovisioning.common.SimpleDialog;
import com.android.setupwizardlib.util.SystemBarHelper;

public class DeleteManagedProfileDialog extends SimpleDialog {
    private final SettingsFacade mSettingsFacade = new SettingsFacade();

    public static DeleteManagedProfileDialog newInstance(int i, ComponentName componentName, String str) {
        Bundle arguments = new SimpleDialog.Builder().setTitle(Integer.valueOf(R.string.delete_profile_title)).setPositiveButtonMessage(R.string.delete_profile).setNegativeButtonMessage(R.string.cancel_delete_profile).setCancelable(false).build().getArguments();
        arguments.putInt("user_profile_callback_id", i);
        if (componentName != null) {
            arguments.putString("mdm_package_name", componentName.getPackageName());
        }
        arguments.putString("profile_owner_domain", str);
        DeleteManagedProfileDialog deleteManagedProfileDialog = new DeleteManagedProfileDialog();
        deleteManagedProfileDialog.setArguments(arguments);
        return deleteManagedProfileDialog;
    }

    @Override
    public AlertDialog onCreateDialog(Bundle bundle) {
        AlertDialog alertDialogOnCreateDialog = super.onCreateDialog(bundle);
        alertDialogOnCreateDialog.setView(createContentView());
        if (!this.mSettingsFacade.isUserSetupCompleted(getActivity())) {
            SystemBarHelper.hideSystemBars(alertDialogOnCreateDialog);
        }
        return alertDialogOnCreateDialog;
    }

    private View createContentView() {
        MdmPackageInfo mdmPackageInfoCreateFromPackageName;
        String string;
        Drawable defaultActivityIcon;
        View viewInflate = getActivity().getLayoutInflater().inflate(R.layout.delete_managed_profile_dialog, (ViewGroup) getActivity().findViewById(android.R.id.content), false);
        String string2 = getArguments().getString("mdm_package_name");
        if (string2 != null) {
            mdmPackageInfoCreateFromPackageName = MdmPackageInfo.createFromPackageName(getActivity(), string2);
        } else {
            mdmPackageInfoCreateFromPackageName = null;
        }
        if (mdmPackageInfoCreateFromPackageName != null) {
            string = mdmPackageInfoCreateFromPackageName.appLabel;
            defaultActivityIcon = mdmPackageInfoCreateFromPackageName.packageIcon;
        } else {
            string = getResources().getString(android.R.string.unknownName);
            defaultActivityIcon = getActivity().getPackageManager().getDefaultActivityIcon();
        }
        ImageView imageView = (ImageView) viewInflate.findViewById(R.id.device_manager_icon_view);
        imageView.setImageDrawable(defaultActivityIcon);
        imageView.setContentDescription(getResources().getString(R.string.mdm_icon_label, string));
        ((TextView) viewInflate.findViewById(R.id.device_manager_name)).setText(string);
        return viewInflate;
    }

    public int getUserId() {
        return getArguments().getInt("user_profile_callback_id");
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        dialogInterface.dismiss();
        ((SimpleDialog.SimpleDialogListener) getActivity()).onNegativeButtonClick(this);
    }
}
