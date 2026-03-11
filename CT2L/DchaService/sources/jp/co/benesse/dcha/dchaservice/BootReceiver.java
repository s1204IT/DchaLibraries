package jp.co.benesse.dcha.dchaservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import jp.co.benesse.dcha.dchaservice.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("DchaService", "onReceive DigichalizedStatus 0001");
        if (action.equals("android.intent.action.BOOT_COMPLETED")) {
            Log.d("DchaService", "onReceive DigichalizedStatus 0002");
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            int status = sp.getInt("DigichalizedStatus", 0);
            Log.d("DchaService", "onReceive DigichalizedStatus:" + status);
            if (status == 1 || status == 2) {
                Log.d("DchaService", "onReceive DigichalizedStatus 0003");
                Intent startIntent = new Intent(context, (Class<?>) DchaService.class);
                startIntent.putExtra("REQ_COMMAND", 1);
                context.startService(startIntent);
            } else if (status == 0) {
                Log.d("DchaService", "onReceive DigichalizedStatus 0004");
                Intent startIntent2 = new Intent(context, (Class<?>) DchaService.class);
                startIntent2.putExtra("REQ_COMMAND", 2);
                context.startService(startIntent2);
            } else if (status == 3) {
                Log.d("DchaService", "onReceive DigichalizedStatus 0005");
                Intent startIntent3 = new Intent(context, (Class<?>) DchaService.class);
                startIntent3.putExtra("REQ_COMMAND", 3);
                context.startService(startIntent3);
            }
        }
        Log.d("DchaService", "onReceive DigichalizedStatus 0006");
    }
}
