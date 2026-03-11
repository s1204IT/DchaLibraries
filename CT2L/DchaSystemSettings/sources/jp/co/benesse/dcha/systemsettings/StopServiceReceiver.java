package jp.co.benesse.dcha.systemsettings;

import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import jp.co.benesse.dcha.util.Logger;

public class StopServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();
        Logger.d("StopServiceReceiver", "onReceive action:" + action);
        Logger.d("StopServiceReceiver", "onReceive 0001");
        if (action.equals("jp.co.benesse.dcha.systemsettings.intent.action.STOP")) {
            Logger.d("StopServiceReceiver", "onReceive 0002");
            try {
                String key = intent.getStringExtra("key");
                if ("ZygdP8j_px".equals(key)) {
                    Logger.d("StopServiceReceiver", "onReceive 0003");
                    int[] pids = intent.getIntArrayExtra("pids");
                    intent.getExtras().getStringArrayList("packages");
                    ActivityManagerNative.getDefault().killPids(pids, "digicharize", true);
                    for (int pid : pids) {
                        Process.killProcessQuiet(pid);
                    }
                }
            } catch (Exception e) {
                Logger.d("StopServiceReceiver", "onReceive 0004");
                Logger.e("StopServiceReceiver", "onReceive", e);
            }
        }
        Logger.d("StopServiceReceiver", "onReceive 0005");
    }
}
