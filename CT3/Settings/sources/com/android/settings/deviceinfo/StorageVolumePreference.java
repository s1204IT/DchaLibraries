package com.android.settings.deviceinfo;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.format.Formatter;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import com.android.settings.R;
import com.android.settings.deviceinfo.StorageSettings;
import java.io.File;

public class StorageVolumePreference extends Preference {
    private int mColor;
    private final StorageManager mStorageManager;
    private final View.OnClickListener mUnmountListener;
    private int mUsedPercent;
    private final VolumeInfo mVolume;

    public StorageVolumePreference(Context context, VolumeInfo volume, int color) {
        Drawable icon;
        super(context);
        this.mUsedPercent = -1;
        this.mUnmountListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new StorageSettings.UnmountTask(StorageVolumePreference.this.getContext(), StorageVolumePreference.this.mVolume).execute(new Void[0]);
            }
        };
        this.mStorageManager = (StorageManager) context.getSystemService(StorageManager.class);
        this.mVolume = volume;
        this.mColor = color;
        setLayoutResource(R.layout.storage_volume);
        setKey(volume.getId());
        setTitle(this.mStorageManager.getBestVolumeDescription(volume));
        if ("private".equals(volume.getId())) {
            icon = context.getDrawable(R.drawable.ic_settings_storage);
        } else {
            icon = context.getDrawable(R.drawable.ic_sim_sd);
        }
        if (volume.isMountedReadable()) {
            File path = volume.getPath();
            long freeBytes = path.getFreeSpace();
            long totalBytes = path.getTotalSpace();
            long usedBytes = totalBytes - freeBytes;
            String used = Formatter.formatFileSize(context, usedBytes);
            String total = Formatter.formatFileSize(context, totalBytes);
            setSummary(context.getString(R.string.storage_volume_summary, used, total));
            if (totalBytes > 0) {
                this.mUsedPercent = (int) ((100 * usedBytes) / totalBytes);
            }
            if (freeBytes < this.mStorageManager.getStorageLowBytes(path)) {
                this.mColor = StorageSettings.COLOR_WARNING;
                icon = context.getDrawable(R.drawable.ic_warning_24dp);
            }
        } else {
            setSummary(volume.getStateDescription());
            this.mUsedPercent = -1;
        }
        icon.mutate();
        icon.setTint(this.mColor);
        setIcon(icon);
        if (volume.getType() != 0 || !volume.isMountedReadable()) {
            return;
        }
        setWidgetLayoutResource(R.layout.preference_storage_action);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        ImageView unmount = (ImageView) view.findViewById(R.id.unmount);
        if (unmount != null) {
            unmount.setImageTintList(ColorStateList.valueOf(Color.parseColor("#8a000000")));
            unmount.setOnClickListener(this.mUnmountListener);
        }
        ProgressBar progress = (ProgressBar) view.findViewById(android.R.id.progress);
        if (this.mVolume.getType() == 1 && this.mUsedPercent != -1) {
            progress.setVisibility(0);
            progress.setProgress(this.mUsedPercent);
            progress.setProgressTintList(ColorStateList.valueOf(this.mColor));
        } else {
            progress.setVisibility(8);
        }
        super.onBindViewHolder(view);
    }

    public void setPercent(int percent) {
        this.mUsedPercent = percent;
    }
}
