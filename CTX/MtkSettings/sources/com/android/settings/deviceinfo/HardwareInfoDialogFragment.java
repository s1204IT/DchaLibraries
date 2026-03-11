package com.android.settings.deviceinfo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IDeviceInfoSettingsExt;

public class HardwareInfoDialogFragment extends InstrumentedDialogFragment {
    private IDeviceInfoSettingsExt mExt;

    @Override
    public int getMetricsCategory() {
        return 862;
    }

    public static HardwareInfoDialogFragment newInstance() {
        return new HardwareInfoDialogFragment();
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        this.mExt = UtilsExt.getDeviceInfoSettingsExt(getActivity());
        AlertDialog.Builder positiveButton = new AlertDialog.Builder(getActivity()).setTitle(R.string.hardware_info).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null);
        View viewInflate = LayoutInflater.from(positiveButton.getContext()).inflate(R.layout.dialog_hardware_info, (ViewGroup) null);
        setText(viewInflate, R.id.model_label, R.id.model_value, this.mExt.customeModelInfo(DeviceModelPreferenceController.getDeviceModel()));
        setText(viewInflate, R.id.serial_number_label, R.id.serial_number_value, getSerialNumber());
        setText(viewInflate, R.id.hardware_rev_label, R.id.hardware_rev_value, SystemProperties.get("ro.boot.hardware.revision"));
        return positiveButton.setView(viewInflate).create();
    }

    void setText(View view, int i, int i2, String str) {
        if (view == null) {
            return;
        }
        View viewFindViewById = view.findViewById(i);
        TextView textView = (TextView) view.findViewById(i2);
        if (!TextUtils.isEmpty(str)) {
            viewFindViewById.setVisibility(0);
            textView.setVisibility(0);
            textView.setText(str);
        } else {
            viewFindViewById.setVisibility(8);
            textView.setVisibility(8);
        }
    }

    String getSerialNumber() {
        return Build.getSerial();
    }
}
