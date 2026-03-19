package com.mediatek.plugin.component;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.mediatek.plugin.utils.Log;

public class ComponentSupport {
    public static final String KEY_TARGET_ACTVITY = "targetActivityName";
    public static final String KEY_TARGET_PLUGIN_ID = "targetTargetId";
    private static final String TAG = "PluginManager/ComponentSupport";
    public static final String VALUE_PROXY_ACTIVITY_NAME = "com.mediatek.plugin.component.PluginProxyActivity";

    public static void startActivity(Context context, Intent intent, String str) {
        startActivity(context, intent, null, str);
    }

    public static void startActivity(Context context, Intent intent, Bundle bundle, String str) {
        String className = intent.getComponent().getClassName();
        Log.d(TAG, "<startActivity> className = " + className + " pluginId = " + str);
        intent.putExtra(KEY_TARGET_ACTVITY, className);
        intent.putExtra(KEY_TARGET_PLUGIN_ID, str);
        intent.setClassName(context, VALUE_PROXY_ACTIVITY_NAME);
        context.startActivity(intent, bundle);
    }

    public static void startActivityForResult(Activity activity, Intent intent, int i, String str) {
        startActivityForResult(activity, intent, i, null, str);
    }

    public static void startActivityForResult(Activity activity, Intent intent, int i, Bundle bundle, String str) {
        String className = intent.getComponent().getClassName();
        Log.d(TAG, "<startActivity> className = " + className + " pluginId = " + str);
        intent.putExtra(KEY_TARGET_ACTVITY, className);
        intent.putExtra(KEY_TARGET_PLUGIN_ID, str);
        intent.setClassName(activity, VALUE_PROXY_ACTIVITY_NAME);
        activity.startActivityForResult(intent, i, bundle);
    }
}
