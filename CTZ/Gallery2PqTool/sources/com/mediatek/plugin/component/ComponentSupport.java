package com.mediatek.plugin.component;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.mediatek.plugin.utils.Log;

public class ComponentSupport {
    public static void startActivity(Context context, Intent intent, String str) {
        startActivity(context, intent, null, str);
    }

    public static void startActivity(Context context, Intent intent, Bundle bundle, String str) {
        String className = intent.getComponent().getClassName();
        Log.d("PluginManager/ComponentSupport", "<startActivity> className = " + className + " pluginId = " + str);
        intent.putExtra("targetActivityName", className);
        intent.putExtra("targetTargetId", str);
        intent.setClassName(context, "com.mediatek.plugin.component.PluginProxyActivity");
        context.startActivity(intent, bundle);
    }
}
