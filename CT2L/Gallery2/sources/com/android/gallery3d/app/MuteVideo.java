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
    private SaveVideoFileInfo mDstFileInfo = null;
    private final Handler mHandler = new Handler();
    final String TIME_STAMP_NAME = "'MUTE'_yyyyMMdd_HHmmss";

    public MuteVideo(String filePath, Uri uri, Activity activity) {
        this.mFilePath = null;
        this.mUri = null;
        this.mActivity = null;
        this.mUri = uri;
        this.mFilePath = filePath;
        this.mActivity = activity;
    }

    public void muteInBackground() {
        this.mDstFileInfo = SaveVideoFileUtils.getDstMp4FileInfo("'MUTE'_yyyyMMdd_HHmmss", this.mActivity.getContentResolver(), this.mUri, this.mActivity.getString(R.string.folder_download));
        showProgressDialog();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    VideoUtils.startMute(MuteVideo.this.mFilePath, MuteVideo.this.mDstFileInfo);
                    SaveVideoFileUtils.insertContent(MuteVideo.this.mDstFileInfo, MuteVideo.this.mActivity.getContentResolver(), MuteVideo.this.mUri);
                    MuteVideo.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MuteVideo.this.mActivity.getApplicationContext(), MuteVideo.this.mActivity.getString(R.string.save_into, new Object[]{MuteVideo.this.mDstFileInfo.mFolderName}), 0).show();
                            if (MuteVideo.this.mMuteProgress != null) {
                                MuteVideo.this.mMuteProgress.dismiss();
                                MuteVideo.this.mMuteProgress = null;
                                Intent intent = new Intent("android.intent.action.VIEW");
                                intent.setDataAndType(Uri.fromFile(MuteVideo.this.mDstFileInfo.mFile), "video/*");
                                intent.putExtra("android.intent.extra.finishOnCompletion", false);
                                MuteVideo.this.mActivity.startActivity(intent);
                            }
                        }
                    });
                } catch (IOException e) {
                    Log.e("@@@@", "Can't mute file: " + MuteVideo.this.mFilePath, e);
                    if (MuteVideo.this.mMuteProgress != null) {
                        MuteVideo.this.mMuteProgress.dismiss();
                        MuteVideo.this.mMuteProgress = null;
                    }
                    MuteVideo.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MuteVideo.this.mActivity, MuteVideo.this.mActivity.getString(R.string.video_mute_err), 0).show();
                        }
                    });
                }
            }
        }).start();
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
