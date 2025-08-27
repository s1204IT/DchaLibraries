package com.mediatek.systemui.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;

/* loaded from: classes.dex */
public class DefaultQuickSettingsPlugin extends ContextWrapper implements IQuickSettingsPlugin {
    private static final String TAG = "DefaultQuickSettingsPlugin";
    protected Context mContext;

    public DefaultQuickSettingsPlugin(Context context) {
        super(context);
        this.mContext = context;
    }

    @Override // com.mediatek.systemui.ext.IQuickSettingsPlugin
    public boolean customizeDisplayDataUsage(boolean z) {
        Log.i(TAG, "customizeDisplayDataUsage, return isDisplay = " + z);
        return z;
    }

    @Override // com.mediatek.systemui.ext.IQuickSettingsPlugin
    public String customizeQuickSettingsTileOrder(String str) {
        return str;
    }

    @Override // com.mediatek.systemui.ext.IQuickSettingsPlugin
    public Object customizeAddQSTile(Object obj) {
        return null;
    }

    @Override // com.mediatek.systemui.ext.IQuickSettingsPlugin
    public String customizeDataConnectionTile(int i, IconIdWrapper iconIdWrapper, String str) {
        Log.i(TAG, "customizeDataConnectionTile, icon = " + iconIdWrapper + ", orgLabelStr=" + str);
        return str;
    }

    @Override // com.mediatek.systemui.ext.IQuickSettingsPlugin
    public String customizeDualSimSettingsTile(boolean z, IconIdWrapper iconIdWrapper, String str) {
        Log.i(TAG, "customizeDualSimSettingsTile, enable = " + z + " icon=" + iconIdWrapper + " labelStr=" + str);
        return str;
    }

    @Override // com.mediatek.systemui.ext.IQuickSettingsPlugin
    public void customizeSimDataConnectionTile(int i, IconIdWrapper iconIdWrapper) {
        Log.i(TAG, "customizeSimDataConnectionTile, state = " + i + " icon=" + iconIdWrapper);
    }

    @Override // com.mediatek.systemui.ext.IQuickSettingsPlugin
    public void disableDataForOtherSubscriptions() {
    }

    @Override // com.mediatek.systemui.ext.IQuickSettingsPlugin
    public String customizeApnSettingsTile(boolean z, IconIdWrapper iconIdWrapper, String str) {
        return str;
    }

    @Override // com.mediatek.systemui.ext.IQuickSettingsPlugin
    public String addOpTileSpecs(String str) {
        return str;
    }

    @Override // com.mediatek.systemui.ext.IQuickSettingsPlugin
    public boolean doOperatorSupportTile(String str) {
        return false;
    }

    @Override // com.mediatek.systemui.ext.IQuickSettingsPlugin
    public Object createTile(Object obj, String str) {
        return null;
    }

    @Override // com.mediatek.systemui.ext.IQuickSettingsPlugin
    public void addOpViews(ViewGroup viewGroup) {
    }

    @Override // com.mediatek.systemui.ext.IQuickSettingsPlugin
    public void registerCallbacks() {
    }

    @Override // com.mediatek.systemui.ext.IQuickSettingsPlugin
    public void unregisterCallbacks() {
    }

    @Override // com.mediatek.systemui.ext.IQuickSettingsPlugin
    public void setViewsVisibility(int i) {
    }

    @Override // com.mediatek.systemui.ext.IQuickSettingsPlugin
    public void measureOpViews(int i) {
    }

    @Override // com.mediatek.systemui.ext.IQuickSettingsPlugin
    public View getPreviousView(View view) {
        return view;
    }

    @Override // com.mediatek.systemui.ext.IQuickSettingsPlugin
    public int getOpViewsHeight() {
        return 0;
    }

    @Override // com.mediatek.systemui.ext.IQuickSettingsPlugin
    public void setOpViewsLayout(int i) {
    }

    @Override // com.mediatek.systemui.ext.IQuickSettingsPlugin
    public String getTileLabel(String str) {
        return "";
    }

    @Override // com.mediatek.systemui.ext.IQuickSettingsPlugin
    public void setHostAppInstance(Object obj) {
    }
}
