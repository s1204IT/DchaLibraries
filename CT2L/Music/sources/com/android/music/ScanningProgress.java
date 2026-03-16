package com.android.music;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;

public class ScanningProgress extends Activity {
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                String status = Environment.getExternalStorageState();
                if (!status.equals("mounted")) {
                    ScanningProgress.this.finish();
                    return;
                }
                Cursor c = MusicUtils.query(ScanningProgress.this, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, null, null, null, null);
                if (c != null) {
                    c.close();
                    ScanningProgress.this.setResult(-1);
                    ScanningProgress.this.finish();
                } else {
                    Message next = obtainMessage(0);
                    sendMessageDelayed(next, 3000L);
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setVolumeControlStream(3);
        requestWindowFeature(1);
        if (Environment.isExternalStorageRemovable()) {
            setContentView(R.layout.scanning);
        } else {
            setContentView(R.layout.scanning_nosdcard);
        }
        getWindow().setLayout(-2, -2);
        setResult(0);
        Message msg = this.mHandler.obtainMessage(0);
        this.mHandler.sendMessageDelayed(msg, 1000L);
    }

    @Override
    public void onDestroy() {
        this.mHandler.removeMessages(0);
        super.onDestroy();
    }
}
