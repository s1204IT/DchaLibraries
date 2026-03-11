package com.android.systemui.plugins;

import android.text.TextUtils;
import com.android.systemui.plugins.annotations.ProvidesInterface;

public interface PluginManager {
    public static final String NOTIFICATION_CHANNEL_ID = "ALR";
    public static final String PLUGIN_CHANGED = "com.android.systemui.action.PLUGIN_CHANGED";

    <T extends Plugin> void addPluginListener(PluginListener<T> pluginListener, Class<?> cls);

    <T extends Plugin> void addPluginListener(PluginListener<T> pluginListener, Class<?> cls, boolean z);

    <T extends Plugin> void addPluginListener(String str, PluginListener<T> pluginListener, Class<?> cls);

    <T extends Plugin> void addPluginListener(String str, PluginListener<T> pluginListener, Class cls, boolean z);

    <T> boolean dependsOn(Plugin plugin, Class<T> cls);

    <T extends Plugin> T getOneShotPlugin(Class<T> cls);

    <T extends Plugin> T getOneShotPlugin(String str, Class<?> cls);

    void removePluginListener(PluginListener<?> pluginListener);

    static <P> String getAction(Class<P> cls) {
        ProvidesInterface providesInterface = (ProvidesInterface) cls.getDeclaredAnnotation(ProvidesInterface.class);
        if (providesInterface == null) {
            throw new RuntimeException(cls + " doesn't provide an interface");
        }
        if (TextUtils.isEmpty(providesInterface.action())) {
            throw new RuntimeException(cls + " doesn't provide an action");
        }
        return providesInterface.action();
    }
}
