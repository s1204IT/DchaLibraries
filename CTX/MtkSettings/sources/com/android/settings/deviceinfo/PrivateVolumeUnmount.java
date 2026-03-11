package com.android.settings.deviceinfo;

import android.os.Bundle;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.deviceinfo.StorageSettings;

public class PrivateVolumeUnmount extends SettingsPreferenceFragment {
    private final View.OnClickListener mConfirmListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            new StorageSettings.UnmountTask(PrivateVolumeUnmount.this.getActivity(), PrivateVolumeUnmount.this.mVolume).execute(new Void[0]);
            PrivateVolumeUnmount.this.getActivity().finish();
        }
    };
    private DiskInfo mDisk;
    private VolumeInfo mVolume;

    @Override
    public int getMetricsCategory() {
        return 42;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        StorageManager storageManager = (StorageManager) getActivity().getSystemService(StorageManager.class);
        this.mVolume = storageManager.findVolumeById(getArguments().getString("android.os.storage.extra.VOLUME_ID"));
        this.mDisk = storageManager.findDiskById(this.mVolume.getDiskId());
        View viewInflate = layoutInflater.inflate(R.layout.storage_internal_unmount, viewGroup, false);
        TextView textView = (TextView) viewInflate.findViewById(R.id.body);
        Button button = (Button) viewInflate.findViewById(R.id.confirm);
        textView.setText(TextUtils.expandTemplate(getText(R.string.storage_internal_unmount_details), this.mDisk.getDescription()));
        button.setOnClickListener(this.mConfirmListener);
        return viewInflate;
    }
}
