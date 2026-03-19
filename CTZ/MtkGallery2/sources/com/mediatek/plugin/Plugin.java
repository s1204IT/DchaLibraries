package com.mediatek.plugin;

import com.mediatek.plugin.element.PluginDescriptor;

public class Plugin {
    private ClassLoader mClassLoader;
    private PluginDescriptor mDescriptor;

    public Plugin(PluginDescriptor pluginDescriptor, ClassLoader classLoader) {
        this.mDescriptor = pluginDescriptor;
        this.mClassLoader = classLoader;
    }

    public void start() {
        doStart();
    }

    public void stop() {
        doStop();
    }

    public ClassLoader getClassLoader() {
        return this.mClassLoader;
    }

    public PluginDescriptor getDescriptor() {
        return this.mDescriptor;
    }

    protected void doStart() {
    }

    protected void doStop() {
    }
}
