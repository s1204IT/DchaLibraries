package com.android.gallery3d.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.widget.Toast;
import com.android.gallery3d.R;
import com.android.gallery3d.util.SaveVideoFileInfo;
import com.android.gallery3d.util.SaveVideoFileUtils;
import java.io.IOException;

public class MuteVideo {
    private Activity mActivity;
    private String mFilePath;
    private ProgressDialog mMuteProgress;
    private Uri mUri;
    private Uri mNewVideoUri = null;
    private SaveVideoFileInfo mDstFileInfo = null;
    private final Handler mHandler = new Handler();
    final String TIME_STAMP_NAME = "'MUTE'_yyyyMMdd_HHmmss";
    private final Runnable mShowErrorToastRunnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(MuteVideo.this.mActivity.getApplicationContext(), MuteVideo.this.mActivity.getString(R.string.video_mute_err), 0).show();
        }
    };

    public MuteVideo(String str, Uri uri, Activity activity) {
        this.mFilePath = null;
        this.mUri = null;
        this.mActivity = null;
        this.mUri = uri;
        this.mFilePath = str;
        this.mActivity = activity;
    }

    public void muteInBackground() {
        com.mediatek.gallery3d.util.Log.v("VP_MuteVideo", "[muteInBackground]...");
        this.mDstFileInfo = SaveVideoFileUtils.getDstMp4FileInfo("'MUTE'_yyyyMMdd_HHmmss", this.mActivity.getContentResolver(), this.mUri, null, false, this.mActivity.getString(R.string.folder_download));
        showProgressDialog();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (VideoUtils.startMute(MuteVideo.this.mFilePath, MuteVideo.this.mDstFileInfo, MuteVideo.this.mMuteProgress)) {
                    MuteVideo.this.mNewVideoUri = null;
                    MuteVideo.this.mNewVideoUri = SaveVideoFileUtils.insertContent(MuteVideo.this.mDstFileInfo, MuteVideo.this.mActivity.getContentResolver(), MuteVideo.this.mUri);
                    com.mediatek.gallery3d.util.Log.v("VP_MuteVideo", "mNewVideoUri = " + MuteVideo.this.mNewVideoUri);
                    com.mediatek.gallery3d.util.Log.v("VP_MuteVideo", "[muteInBackground] post mTriggerUiChangeRunnable");
                    MuteVideo.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MuteVideo.this.mActivity.getApplicationContext(), MuteVideo.this.mActivity.getString(R.string.save_into, MuteVideo.this.mDstFileInfo.mFolderName), 0).show();
                            if (MuteVideo.this.mMuteProgress != null) {
                                MuteVideo.this.mMuteProgress.dismiss();
                                MuteVideo.this.mMuteProgress = null;
                                if (MuteVideo.this.mNewVideoUri != null) {
                                    Intent intent = new Intent("android.intent.action.VIEW");
                                    intent.setDataAndType(MuteVideo.this.mNewVideoUri, "video/*");
                                    intent.putExtra("android.intent.extra.finishOnCompletion", false);
                                    MuteVideo.this.mActivity.startActivity(intent);
                                }
                            }
                        }
                    });
                    return;
                }
                com.mediatek.gallery3d.util.Log.v("VP_MuteVideo", "[muteInBackground] mute failed");
                MuteVideo.this.mHandler.removeCallbacks(MuteVideo.this.mShowErrorToastRunnable);
                MuteVideo.this.mHandler.post(MuteVideo.this.mShowErrorToastRunnable);
                if (MuteVideo.this.mDstFileInfo.mFile.exists()) {
                    MuteVideo.this.mDstFileInfo.mFile.delete();
                }
            }
        }).start();
    }

    public void cancelMute() {
        com.mediatek.gallery3d.util.Log.v("VP_MuteVideo", "[cancleMute] mMuteProgress = " + this.mMuteProgress);
        if (this.mMuteProgress != null) {
            this.mMuteProgress.dismiss();
            this.mMuteProgress = null;
        }
    }

    private void showProgressDialog() {
        this.mMuteProgress = new ProgressDialog(this.mActivity);
        this.mMuteProgress.setTitle(this.mActivity.getString(R.string.muting));
        this.mMuteProgress.setMessage(this.mActivity.getString(R.string.please_wait));
        this.mMuteProgress.setCancelable(false);
        this.mMuteProgress.setCanceledOnTouchOutside(false);
        this.mMuteProgress.show();
    }
}
