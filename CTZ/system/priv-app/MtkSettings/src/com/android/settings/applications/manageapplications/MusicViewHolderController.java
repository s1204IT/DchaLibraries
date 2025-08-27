package com.android.settings.applications.manageapplications;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.DocumentsContract;
import android.text.format.Formatter;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.applications.StorageStatsSource;
import java.io.IOException;

/* loaded from: classes.dex */
public class MusicViewHolderController implements FileViewHolderController {
    private Context mContext;
    private long mMusicSize;
    private StorageStatsSource mSource;
    private UserHandle mUser;
    private String mVolumeUuid;

    public MusicViewHolderController(Context context, StorageStatsSource storageStatsSource, String str, UserHandle userHandle) {
        this.mContext = context;
        this.mSource = storageStatsSource;
        this.mVolumeUuid = str;
        this.mUser = userHandle;
    }

    @Override // com.android.settings.applications.manageapplications.FileViewHolderController
    public void queryStats() {
        try {
            this.mMusicSize = this.mSource.getExternalStorageStats(this.mVolumeUuid, this.mUser).audioBytes;
        } catch (IOException e) {
            this.mMusicSize = 0L;
            Log.w("MusicViewHolderCtrl", e);
        }
    }

    @Override // com.android.settings.applications.manageapplications.FileViewHolderController
    public boolean shouldShow() {
        return true;
    }

    @Override // com.android.settings.applications.manageapplications.FileViewHolderController
    public void setupView(ApplicationViewHolder applicationViewHolder) {
        applicationViewHolder.setIcon(R.drawable.ic_headset_24dp);
        applicationViewHolder.setTitle(this.mContext.getText(R.string.audio_files_title));
        applicationViewHolder.setSummary(Formatter.formatFileSize(this.mContext, this.mMusicSize));
    }

    @Override // com.android.settings.applications.manageapplications.FileViewHolderController
    public void onClick(Fragment fragment) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndType(DocumentsContract.buildRootUri("com.android.providers.media.documents", "audio_root"), "vnd.android.document/root");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.putExtra("android.intent.extra.USER_ID", this.mUser);
        Utils.launchIntent(fragment, intent);
    }
}
