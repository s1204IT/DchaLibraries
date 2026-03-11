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
        public void onClick(View v) {
            new StorageSettings.UnmountTask(PrivateVolumeUnmount.this.getActivity(), PrivateVolumeUnmount.this.mVolume).execute(new Void[0]);
            PrivateVolumeUnmount.this.getActivity().finish();
        }
    };
    private DiskInfo mDisk;
    private VolumeInfo mVolume;

    @Override
    protected int getMetricsCategory() {
        return 42;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        StorageManager storage = (StorageManager) getActivity().getSystemService(StorageManager.class);
        String volumeId = getArguments().getString("android.os.storage.extra.VOLUME_ID");
        this.mVolume = storage.findVolumeById(volumeId);
        this.mDisk = storage.findDiskById(this.mVolume.getDiskId());
        View view = inflater.inflate(R.layout.storage_internal_unmount, container, false);
        TextView body = (TextView) view.findViewById(R.id.body);
        Button confirm = (Button) view.findViewById(R.id.confirm);
        body.setText(TextUtils.expandTemplate(getText(R.string.storage_internal_unmount_details), this.mDisk.getDescription()));
        confirm.setOnClickListener(this.mConfirmListener);
        return view;
    }
}
