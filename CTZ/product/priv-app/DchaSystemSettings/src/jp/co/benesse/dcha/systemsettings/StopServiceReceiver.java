package jp.co.benesse.dcha.systemsettings;

import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import jp.co.benesse.dcha.util.Logger;

/* loaded from: classes.dex */
public class StopServiceReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Logger.d("StopServiceReceiver", "onReceive action:" + action);
        Logger.d("StopServiceReceiver", "onReceive 0001");
        if (action.equals("jp.co.benesse.dcha.systemsettings.intent.action.STOP")) {
            Logger.d("StopServiceReceiver", "onReceive 0002");
            try {
                if ("ZygdP8j_px".equals(intent.getStringExtra("key"))) {
                    Logger.d("StopServiceReceiver", "onReceive 0003");
                    int[] intArrayExtra = intent.getIntArrayExtra("pids");
                    intent.getExtras().getStringArrayList("packages");
                    ActivityManagerNative.getDefault().killPids(intArrayExtra, "digicharize", true);
                    for (int i : intArrayExtra) {
                        Process.killProcessQuiet(i);
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
