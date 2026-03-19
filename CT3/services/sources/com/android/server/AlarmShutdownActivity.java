package com.android.server;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Slog;
import com.android.server.power.ShutdownThread;

public class AlarmShutdownActivity extends Activity {
    private static final String TAG = "AlarmShutdownActivity";
    private boolean mConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mConfirm = getIntent().getBooleanExtra("android.intent.extra.KEY_CONFIRM", false);
        Slog.i(TAG, "confirm=" + this.mConfirm);
        Handler h = new Handler();
        h.post(new Runnable() {
            @Override
            public void run() {
                ShutdownThread.EnableAnimating(false);
                ShutdownThread.shutdown(AlarmShutdownActivity.this, "AlarmShutdownRequest", AlarmShutdownActivity.this.mConfirm);
                ShutdownThread.EnableAnimating(true);
                AlarmShutdownActivity.this.finish();
            }
        });
    }
}
