package com.android.settings.deviceinfo.firmwareversion;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class FirmwareVersionDialogFragment extends InstrumentedDialogFragment {
    private View mRootView;

    public static void show(Fragment fragment) {
        FragmentManager childFragmentManager = fragment.getChildFragmentManager();
        if (childFragmentManager.findFragmentByTag("firmwareVersionDialog") == null) {
            new FirmwareVersionDialogFragment().show(childFragmentManager, "firmwareVersionDialog");
        }
    }

    @Override
    public int getMetricsCategory() {
        return 1247;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        AlertDialog.Builder positiveButton = new AlertDialog.Builder(getActivity()).setTitle(R.string.firmware_title).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null);
        this.mRootView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_firmware_version, (ViewGroup) null);
        initializeControllers();
        return positiveButton.setView(this.mRootView).create();
    }

    public void setText(int i, CharSequence charSequence) {
        TextView textView = (TextView) this.mRootView.findViewById(i);
        if (textView != null) {
            textView.setText(charSequence);
        }
    }

    public void removeSettingFromScreen(int i) {
        View viewFindViewById = this.mRootView.findViewById(i);
        if (viewFindViewById != null) {
            viewFindViewById.setVisibility(8);
        }
    }

    public void registerClickListener(int i, View.OnClickListener onClickListener) {
        View viewFindViewById = this.mRootView.findViewById(i);
        if (viewFindViewById != null) {
            viewFindViewById.setOnClickListener(onClickListener);
        }
    }

    private void initializeControllers() {
        new FirmwareVersionDialogController(this).initialize();
        new SecurityPatchLevelDialogController(this).initialize();
        new BasebandVersionDialogController(this).initialize();
        new KernelVersionDialogController(this).initialize();
        new BuildNumberDialogController(this).initialize();
    }
}
