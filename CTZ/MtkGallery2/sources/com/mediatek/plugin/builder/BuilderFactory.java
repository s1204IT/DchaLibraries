package com.mediatek.plugin.builder;

import java.util.HashMap;

public class BuilderFactory {
    public static HashMap<String, Builder> sBuilders = new HashMap<>();

    static {
        registerBuilder(new ParameterBuilder());
        registerBuilder(new ParameterDefBuilder());
        registerBuilder(new ExtensionBuilder());
        registerBuilder(new ExtensionPointBuilder());
        registerBuilder(new PluginDescriptorBuilder());
    }

    public static Builder getBuilder(String str) {
        return sBuilders.get(str);
    }

    private static void registerBuilder(Builder builder) {
        sBuilders.put(builder.getSupportedTag(), builder);
    }
}
