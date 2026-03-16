package com.android.phone;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import com.android.phone.HfaLogic;

public class HfaService extends Service {
    private static final String TAG = HfaService.class.getSimpleName();
    private HfaLogic mHfaLogic;

    @Override
    public void onCreate() {
        Log.i(TAG, "service started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PendingIntent otaResponseIntent = (PendingIntent) intent.getParcelableExtra("otasp_result_code_pending_intent");
        this.mHfaLogic = new HfaLogic(this, new HfaLogic.HfaLogicCallback() {
            @Override
            public void onSuccess() {
                Log.i(HfaService.TAG, "onSuccess");
                HfaService.this.onComplete();
            }

            @Override
            public void onError(String msg) {
                Log.i(HfaService.TAG, "onError: " + msg);
                HfaService.this.onComplete();
            }
        }, otaResponseIntent);
        this.mHfaLogic.start();
        return 3;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void onComplete() {
        stopSelf();
    }
}
