package com.mediatek.common;

public final class PluginInfo {
    private String apkName;
    private String implementationName;
    private String packageName;

    public PluginInfo(String str, String str2, String str3) {
        this.implementationName = "";
        this.packageName = "";
        this.apkName = "";
        this.packageName = str;
        this.implementationName = str2;
        this.apkName = str3;
    }

    public String getImplementationName() {
        return this.implementationName;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public String getApkName() {
        return this.apkName;
    }
}
