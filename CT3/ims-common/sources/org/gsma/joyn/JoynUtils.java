package org.gsma.joyn;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageItemInfo;
import android.content.pm.ResolveInfo;
import java.util.List;
import org.gsma.joyn.Intents;

public class JoynUtils {
    public static final String TAG = "TAPI-JoynUtils";

    public static List<ResolveInfo> getJoynClients(Context context) {
        Logger.d(TAG, "getJoynClients() entry " + context);
        Intent intent = new Intent(Intents.Client.ACTION_VIEW_SETTINGS);
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, 65536);
        Logger.d(TAG, "getJoynClients() exit " + list);
        return list;
    }

    public static void isJoynClientActivated(Context context, ResolveInfo appInfo, BroadcastReceiver receiverResult) {
        Logger.d(TAG, "isJoynClientActivated() entry " + context);
        Intent broadcastIntent = new Intent(((PackageItemInfo) appInfo.activityInfo).packageName + Intents.Client.ACTION_CLIENT_GET_STATUS);
        context.sendOrderedBroadcast(broadcastIntent, null, receiverResult, null, -1, null, null);
    }

    public static void loadJoynClientSettings(Context context, ResolveInfo appInfo) {
        Logger.d(TAG, "loadJoynClientSettings() entry " + context);
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setComponent(new ComponentName(((PackageItemInfo) appInfo.activityInfo).packageName, ((PackageItemInfo) appInfo.activityInfo).name));
        intent.setFlags(270532608);
        context.startActivity(intent);
    }
}
