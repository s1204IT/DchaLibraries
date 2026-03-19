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
        public void handleMessage(Message message) {
            if (message.what == 0) {
                if (!Environment.getExternalStorageState().equals("mounted")) {
                    ScanningProgress.this.finish();
                    return;
                }
                Cursor cursorQuery = MusicUtils.query(ScanningProgress.this, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, null, null, null, null);
                if (cursorQuery != null) {
                    cursorQuery.close();
                    ScanningProgress.this.setResult(-1);
                    ScanningProgress.this.finish();
                    return;
                }
                sendMessageDelayed(obtainMessage(0), 3000L);
            }
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setVolumeControlStream(3);
        requestWindowFeature(1);
        setContentView(R.layout.scanning);
        getWindow().setLayout(-2, -2);
        setResult(0);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(0), 1000L);
    }

    @Override
    public void onDestroy() {
        this.mHandler.removeMessages(0);
        super.onDestroy();
    }
}
