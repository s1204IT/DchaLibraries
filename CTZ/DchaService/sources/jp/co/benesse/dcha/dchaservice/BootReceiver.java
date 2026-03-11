package jp.co.benesse.dcha.dchaservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import jp.co.benesse.dcha.dchaservice.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("DchaService", "onReceive DigichalizedStatus 0001");
        if (action.equals("android.intent.action.BOOT_COMPLETED")) {
            Log.d("DchaService", "onReceive DigichalizedStatus 0002");
            int i = PreferenceManager.getDefaultSharedPreferences(context).getInt("DigichalizedStatus", 0);
            Log.d("DchaService", "onReceive DigichalizedStatus:" + i);
            if (i == 1 || i == 2) {
                Log.d("DchaService", "onReceive DigichalizedStatus 0003");
                Intent intent2 = new Intent(context, (Class<?>) DchaService.class);
                intent2.putExtra("REQ_COMMAND", 1);
                context.startService(intent2);
            } else if (i == 0) {
                Log.d("DchaService", "onReceive DigichalizedStatus 0004");
                Intent intent3 = new Intent(context, (Class<?>) DchaService.class);
                intent3.putExtra("REQ_COMMAND", 2);
                context.startService(intent3);
            } else if (i == 3) {
                Log.d("DchaService", "onReceive DigichalizedStatus 0005");
                Intent intent4 = new Intent(context, (Class<?>) DchaService.class);
                intent4.putExtra("REQ_COMMAND", 3);
                context.startService(intent4);
            }
        }
        Log.d("DchaService", "onReceive DigichalizedStatus 0006");
    }
}
