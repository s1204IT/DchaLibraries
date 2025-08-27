package com.android.settings.deviceinfo;

import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import com.android.settings.R;
import com.android.settings.deviceinfo.StorageSettings;
import com.android.settingslib.Utils;
import java.io.File;
import java.io.IOException;

/* loaded from: classes.dex */
public class StorageVolumePreference extends Preference {
    private static final String TAG = StorageVolumePreference.class.getSimpleName();
    private int mColor;
    private final StorageManager mStorageManager;
    private final View.OnClickListener mUnmountListener;
    private int mUsedPercent;
    private final VolumeInfo mVolume;

    /* JADX WARN: Removed duplicated region for block: B:27:0x00c3  */
    /* JADX WARN: Removed duplicated region for block: B:30:0x00d4  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public StorageVolumePreference(Context context, VolumeInfo volumeInfo, int i, long j) {
        Drawable drawable;
        long totalSpace;
        long freeSpace;
        long j2;
        super(context);
        this.mUsedPercent = -1;
        this.mUnmountListener = new View.OnClickListener() { // from class: com.android.settings.deviceinfo.StorageVolumePreference.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                new StorageSettings.UnmountTask(StorageVolumePreference.this.getContext(), StorageVolumePreference.this.mVolume).execute(new Void[0]);
            }
        };
        this.mStorageManager = (StorageManager) context.getSystemService(StorageManager.class);
        this.mVolume = volumeInfo;
        this.mColor = i;
        setLayoutResource(R.layout.storage_volume);
        setKey(volumeInfo.getId());
        setTitle(this.mStorageManager.getBestVolumeDescription(volumeInfo));
        if ("private".equals(volumeInfo.getId())) {
            drawable = context.getDrawable(R.drawable.ic_storage);
        } else {
            drawable = context.getDrawable(R.drawable.ic_sim_sd);
        }
        if (volumeInfo.isMountedReadable()) {
            File path = volumeInfo.getPath();
            if (volumeInfo.getType() == 1) {
                StorageStatsManager storageStatsManager = (StorageStatsManager) context.getSystemService(StorageStatsManager.class);
                try {
                    totalSpace = storageStatsManager.getTotalBytes(volumeInfo.getFsUuid());
                    try {
                        freeSpace = storageStatsManager.getFreeBytes(volumeInfo.getFsUuid());
                        j2 = totalSpace - freeSpace;
                    } catch (IOException e) {
                        e = e;
                        Log.w(TAG, e);
                        freeSpace = 0;
                        j2 = 0;
                        setSummary(context.getString(R.string.storage_volume_summary, Formatter.formatFileSize(context, j2), Formatter.formatFileSize(context, totalSpace)));
                        if (totalSpace > 0) {
                        }
                        if (freeSpace < this.mStorageManager.getStorageLowBytes(path)) {
                        }
                        drawable.mutate();
                        drawable.setTint(this.mColor);
                        setIcon(drawable);
                        if (volumeInfo.getType() != 0) {
                        } else {
                            return;
                        }
                    }
                } catch (IOException e2) {
                    e = e2;
                    totalSpace = j;
                }
            } else {
                if (j <= 0) {
                    totalSpace = path.getTotalSpace();
                } else {
                    totalSpace = j;
                }
                freeSpace = path.getFreeSpace();
                j2 = totalSpace - freeSpace;
            }
            setSummary(context.getString(R.string.storage_volume_summary, Formatter.formatFileSize(context, j2), Formatter.formatFileSize(context, totalSpace)));
            if (totalSpace > 0) {
                this.mUsedPercent = (int) ((j2 * 100) / totalSpace);
            }
            if (freeSpace < this.mStorageManager.getStorageLowBytes(path)) {
                this.mColor = Utils.getColorAttr(context, android.R.attr.colorError);
                drawable = context.getDrawable(R.drawable.ic_warning_24dp);
            }
        } else {
            setSummary(volumeInfo.getStateDescription());
            this.mUsedPercent = -1;
        }
        drawable.mutate();
        drawable.setTint(this.mColor);
        setIcon(drawable);
        if (volumeInfo.getType() != 0 && volumeInfo.isMountedReadable()) {
            setWidgetLayoutResource(R.layout.preference_storage_action);
        }
    }

    @Override // android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        ImageView imageView = (ImageView) preferenceViewHolder.findViewById(R.id.unmount);
        if (imageView != null) {
            imageView.setImageTintList(ColorStateList.valueOf(Color.parseColor("#8a000000")));
            imageView.setOnClickListener(this.mUnmountListener);
        }
        ProgressBar progressBar = (ProgressBar) preferenceViewHolder.findViewById(android.R.id.progress);
        if (this.mVolume.getType() == 1 && this.mUsedPercent != -1) {
            progressBar.setVisibility(0);
            progressBar.setProgress(this.mUsedPercent);
            progressBar.setProgressTintList(ColorStateList.valueOf(this.mColor));
        } else {
            progressBar.setVisibility(8);
        }
        super.onBindViewHolder(preferenceViewHolder);
    }
}
