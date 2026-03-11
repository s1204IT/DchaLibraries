package com.mediatek.systemui;

import android.content.Context;
import com.mediatek.common.MPlugin;
import com.mediatek.systemui.ext.DefaultMobileIconExt;
import com.mediatek.systemui.ext.DefaultQuickSettingsPlugin;
import com.mediatek.systemui.ext.DefaultStatusBarPlmnPlugin;
import com.mediatek.systemui.ext.DefaultSystemUIStatusBarExt;
import com.mediatek.systemui.ext.IMobileIconExt;
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import com.mediatek.systemui.ext.IStatusBarPlmnPlugin;
import com.mediatek.systemui.ext.ISystemUIStatusBarExt;

public class PluginManager {
    private static IMobileIconExt sMobileIconExt = null;
    private static IQuickSettingsPlugin sQuickSettingsPlugin = null;
    private static IStatusBarPlmnPlugin sStatusBarPlmnPlugin = null;
    private static ISystemUIStatusBarExt sSystemUIStatusBarExt = null;

    public static synchronized IMobileIconExt getMobileIconExt(Context context) {
        if (sMobileIconExt == null) {
            sMobileIconExt = (IMobileIconExt) MPlugin.createInstance(IMobileIconExt.class.getName(), context);
            if (sMobileIconExt == null) {
                sMobileIconExt = new DefaultMobileIconExt();
            }
        }
        return sMobileIconExt;
    }

    public static synchronized IQuickSettingsPlugin getQuickSettingsPlugin(Context context) {
        if (sQuickSettingsPlugin == null) {
            sQuickSettingsPlugin = (IQuickSettingsPlugin) MPlugin.createInstance(IQuickSettingsPlugin.class.getName(), context);
            if (sQuickSettingsPlugin == null) {
                sQuickSettingsPlugin = new DefaultQuickSettingsPlugin(context);
            }
        }
        return sQuickSettingsPlugin;
    }

    public static synchronized IStatusBarPlmnPlugin getStatusBarPlmnPlugin(Context context) {
        if (sStatusBarPlmnPlugin == null) {
            sStatusBarPlmnPlugin = (IStatusBarPlmnPlugin) MPlugin.createInstance(IStatusBarPlmnPlugin.class.getName(), context);
            if (sStatusBarPlmnPlugin == null) {
                sStatusBarPlmnPlugin = new DefaultStatusBarPlmnPlugin(context);
            }
        }
        return sStatusBarPlmnPlugin;
    }

    public static synchronized ISystemUIStatusBarExt getSystemUIStatusBarExt(Context context) {
        ISystemUIStatusBarExt statusBarExt;
        if (sSystemUIStatusBarExt == null) {
            sSystemUIStatusBarExt = new DefaultSystemUIStatusBarExt(context);
        }
        statusBarExt = (ISystemUIStatusBarExt) MPlugin.createInstance(ISystemUIStatusBarExt.class.getName(), context);
        if (statusBarExt == null) {
            statusBarExt = sSystemUIStatusBarExt;
        }
        return statusBarExt;
    }
}
