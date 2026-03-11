package com.android.settings.deviceinfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeRecord;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class PrivateVolumeForget extends SettingsPreferenceFragment {
    private final View.OnClickListener mConfirmListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            ForgetConfirmFragment.show(PrivateVolumeForget.this, PrivateVolumeForget.this.mRecord.getFsUuid());
        }
    };
    private VolumeRecord mRecord;

    @Override
    public int getMetricsCategory() {
        return 42;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        StorageManager storageManager = (StorageManager) getActivity().getSystemService(StorageManager.class);
        String string = getArguments().getString("android.os.storage.extra.FS_UUID");
        if (string == null) {
            getActivity().finish();
            return null;
        }
        this.mRecord = storageManager.findRecordByUuid(string);
        if (this.mRecord == null) {
            getActivity().finish();
            return null;
        }
        View viewInflate = layoutInflater.inflate(R.layout.storage_internal_forget, viewGroup, false);
        TextView textView = (TextView) viewInflate.findViewById(R.id.body);
        Button button = (Button) viewInflate.findViewById(R.id.confirm);
        textView.setText(TextUtils.expandTemplate(getText(R.string.storage_internal_forget_details), this.mRecord.getNickname()));
        button.setOnClickListener(this.mConfirmListener);
        return viewInflate;
    }

    public static class ForgetConfirmFragment extends InstrumentedDialogFragment {
        @Override
        public int getMetricsCategory() {
            return 559;
        }

        public static void show(Fragment fragment, String str) {
            Bundle bundle = new Bundle();
            bundle.putString("android.os.storage.extra.FS_UUID", str);
            ForgetConfirmFragment forgetConfirmFragment = new ForgetConfirmFragment();
            forgetConfirmFragment.setArguments(bundle);
            forgetConfirmFragment.setTargetFragment(fragment, 0);
            forgetConfirmFragment.show(fragment.getFragmentManager(), "forget_confirm");
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            Activity activity = getActivity();
            final StorageManager storageManager = (StorageManager) activity.getSystemService(StorageManager.class);
            final String string = getArguments().getString("android.os.storage.extra.FS_UUID");
            VolumeRecord volumeRecordFindRecordByUuid = storageManager.findRecordByUuid(string);
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(TextUtils.expandTemplate(getText(R.string.storage_internal_forget_confirm_title), volumeRecordFindRecordByUuid.getNickname()));
            builder.setMessage(TextUtils.expandTemplate(getText(R.string.storage_internal_forget_confirm), volumeRecordFindRecordByUuid.getNickname()));
            builder.setPositiveButton(R.string.storage_menu_forget, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    storageManager.forgetVolume(string);
                    ForgetConfirmFragment.this.getActivity().finish();
                }
            });
            builder.setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null);
            return builder.create();
        }
    }
}
