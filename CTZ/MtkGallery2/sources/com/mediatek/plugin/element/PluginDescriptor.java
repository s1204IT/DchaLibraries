package com.mediatek.plugin.element;

import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import com.mediatek.plugin.builder.PluginDescriptorBuilder;
import java.util.HashMap;
import mf.org.apache.xerces.dom3.as.ASContentModel;

public class PluginDescriptor extends Element {
    public String className;
    private String mArchivePath;
    private AssetManager mAssetManager;
    private PackageInfo mInfo;
    private HashMap<String, PluginDescriptor> mRequiredPluginDescriptor;
    private Resources mResources;
    public String name;
    public int requireMaxHostVersion = ASContentModel.AS_UNBOUNDED;
    public int requireMinHostVersion = Integer.MIN_VALUE;
    public String[] requiredPluginIds;
    public int version;

    public HashMap<String, PluginDescriptor> getRequirePluginDes() {
        return this.mRequiredPluginDescriptor;
    }

    public HashMap<String, ExtensionPoint> getExtensionPoints() {
        return (HashMap) this.mChilds.get(ExtensionPoint.class);
    }

    public HashMap<String, Extension> getExtension() {
        return (HashMap) this.mChilds.get(Extension.class);
    }

    public String getArchivePath() {
        return this.mArchivePath;
    }

    public void setArchivePath(String str) {
        this.mArchivePath = str;
    }

    public void setPackageInfo(PackageInfo packageInfo) {
        this.mInfo = packageInfo;
    }

    public PackageInfo getPackageInfo() {
        return this.mInfo;
    }

    public void setResource(Resources resources) {
        this.mResources = resources;
    }

    public Resources getResources() {
        return this.mResources;
    }

    public void setAssetManager(AssetManager assetManager) {
        this.mAssetManager = assetManager;
    }

    public AssetManager getAssetManager() {
        return this.mAssetManager;
    }

    public void setRequirePluginDes(HashMap<String, PluginDescriptor> map) {
        this.mRequiredPluginDescriptor = map;
    }

    public void release() {
    }

    @Override
    public void printAllKeyValue(String str) {
        super.printAllKeyValue(str);
        printKeyValue(str, PluginDescriptorBuilder.VALUE_NAME, String.valueOf(this.name));
        printKeyValue(str, PluginDescriptorBuilder.VALUE_VERSION, String.valueOf(this.version));
        printKeyValue(str, "requireMaxHostVersion", String.valueOf(this.requireMaxHostVersion));
        printKeyValue(str, "requireMinHostVersion", String.valueOf(this.requireMinHostVersion));
        printKeyValue(str, "className", String.valueOf(this.className));
        if (this.requiredPluginIds != null) {
            int length = this.requiredPluginIds.length;
            if (length == 0) {
                printKeyValue(str, "requiredPluginIds size ", String.valueOf(0));
            }
            for (int i = 0; i < length; i++) {
                printKeyValue(str, "requiredPluginIds " + i + " ", String.valueOf(this.requiredPluginIds[i]));
            }
        } else {
            printKeyValue(str, "requiredPluginIds size ", String.valueOf(0));
        }
        printKeyValue(str, "mArchivePath", String.valueOf(this.mArchivePath));
    }
}
