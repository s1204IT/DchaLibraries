package jp.co.benesse.dcha.dchaservice;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import jp.co.benesse.dcha.dchaservice.util.Log;

public class ProxyService extends IntentService {
    public ProxyService() {
        super("ProxyService");
        Log.d("ProxyService", "ProxyService 0001");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("ProxyService", "onBind 0001");
        return null;
    }

    @Override
    public void onCreate() {
        Log.d("ProxyService", "onCreate 0001");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        Log.d("ProxyService", "onStartCommand 0001");
        return super.onStartCommand(intent, i, i2);
    }

    @Override
    public void onDestroy() {
        Log.d("ProxyService", "onDestroy 0001");
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("ProxyService", "onHandleIntent 0001");
        String action = intent.getAction();
        if ("jp.co.benesse.dcha.dchaservice.UpdateTime".equals(action)) {
            Log.d("ProxyService", "onHandleIntent 0002");
            Log.d("ProxyService", "UpdateLog write");
            UpdateLog.write();
        } else if ("jp.co.benesse.dcha.dchaservice.EmergencyLog".equals(action)) {
            Log.d("ProxyService", "onHandleIntent 0003");
            Log.d("ProxyService", "EmergencyLog write");
            String stringExtra = intent.getStringExtra("EXTRA_LOG_KIND");
            String stringExtra2 = intent.getStringExtra("EXTRA_LOG_DATE");
            String stringExtra3 = intent.getStringExtra("EXTRA_LOG_MESSAGE");
            if (TextUtils.isEmpty(stringExtra2)) {
                Log.d("ProxyService", "onHandleIntent 0004");
                Log.d("ProxyService", "EmergencyLog write without time");
                EmergencyLog.write(this, stringExtra, stringExtra3);
            } else {
                Log.d("ProxyService", "onHandleIntent 0005");
                Log.d("ProxyService", "EmergencyLog write with time");
                EmergencyLog.write(this, stringExtra2, stringExtra, stringExtra3);
            }
            if ("ELK010".equals(stringExtra) || "ELK011".equals(stringExtra)) {
                Log.d("ProxyService", "onHandleIntent 0006");
                TimeSetLogSender.send(this);
            }
        }
        Log.d("ProxyService", "onHandleIntent 0006");
    }
}
