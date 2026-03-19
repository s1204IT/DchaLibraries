package com.android.packageinstaller.television;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.android.packageinstaller.PackageUtil;
import com.android.packageinstaller.R;
import com.android.packageinstaller.television.UninstallAppProgress;

public class UninstallAppProgressFragment extends Fragment implements View.OnClickListener, UninstallAppProgress.ProgressFragment {
    private Button mDeviceManagerButton;
    private Button mOkButton;
    private Button mUsersButton;

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate = layoutInflater.inflate(R.layout.uninstall_progress, viewGroup, false);
        PackageUtil.initSnippetForInstalledApp(getContext(), ((UninstallAppProgress) getActivity()).getAppInfo(), viewInflate.findViewById(R.id.app_snippet));
        this.mDeviceManagerButton = (Button) viewInflate.findViewById(R.id.device_manager_button);
        this.mUsersButton = (Button) viewInflate.findViewById(R.id.users_button);
        this.mDeviceManagerButton.setVisibility(8);
        this.mDeviceManagerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClassName("com.android.settings", "com.android.settings.Settings$DeviceAdminSettingsActivity");
                intent.setFlags(1342177280);
                UninstallAppProgressFragment.this.startActivity(intent);
                UninstallAppProgressFragment.this.getActivity().finish();
            }
        });
        this.mUsersButton.setVisibility(8);
        this.mUsersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent("android.settings.USER_SETTINGS");
                intent.setFlags(1342177280);
                UninstallAppProgressFragment.this.startActivity(intent);
                UninstallAppProgressFragment.this.getActivity().finish();
            }
        });
        this.mOkButton = (Button) viewInflate.findViewById(R.id.ok_button);
        this.mOkButton.setOnClickListener(this);
        return viewInflate;
    }

    @Override
    public void onClick(View view) {
        UninstallAppProgress uninstallAppProgress = (UninstallAppProgress) getActivity();
        if (view == this.mOkButton && uninstallAppProgress != null) {
            Log.i("UninstallAppProgressF", "Finished uninstalling pkg: " + uninstallAppProgress.getAppInfo().packageName);
            uninstallAppProgress.setResultAndFinish();
        }
    }

    @Override
    public void setUsersButtonVisible(boolean z) {
        this.mUsersButton.setVisibility(z ? 0 : 8);
    }

    @Override
    public void setDeviceManagerButtonVisible(boolean z) {
        this.mDeviceManagerButton.setVisibility(z ? 0 : 8);
    }

    @Override
    public void showCompletion(CharSequence charSequence) {
        View view = getView();
        view.findViewById(R.id.progress_view).setVisibility(8);
        view.findViewById(R.id.status_view).setVisibility(0);
        ((TextView) view.findViewById(R.id.status_text)).setText(charSequence);
        view.findViewById(R.id.ok_panel).setVisibility(0);
    }
}
