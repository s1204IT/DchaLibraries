package com.mediatek.systemui.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;

public class DefaultQuickSettingsPlugin extends ContextWrapper implements IQuickSettingsPlugin {
    private static final String TAG = "DefaultQuickSettingsPlugin";
    protected Context mContext;

    public DefaultQuickSettingsPlugin(Context context) {
        super(context);
        this.mContext = context;
    }

    @Override
    public boolean customizeDisplayDataUsage(boolean z) {
        Log.i(TAG, "customizeDisplayDataUsage, return isDisplay = " + z);
        return z;
    }

    @Override
    public String customizeQuickSettingsTileOrder(String str) {
        return str;
    }

    @Override
    public Object customizeAddQSTile(Object obj) {
        return null;
    }

    @Override
    public String customizeDataConnectionTile(int i, IconIdWrapper iconIdWrapper, String str) {
        Log.i(TAG, "customizeDataConnectionTile, icon = " + iconIdWrapper + ", orgLabelStr=" + str);
        return str;
    }

    @Override
    public String customizeDualSimSettingsTile(boolean z, IconIdWrapper iconIdWrapper, String str) {
        Log.i(TAG, "customizeDualSimSettingsTile, enable = " + z + " icon=" + iconIdWrapper + " labelStr=" + str);
        return str;
    }

    @Override
    public void customizeSimDataConnectionTile(int i, IconIdWrapper iconIdWrapper) {
        Log.i(TAG, "customizeSimDataConnectionTile, state = " + i + " icon=" + iconIdWrapper);
    }

    @Override
    public void disableDataForOtherSubscriptions() {
    }

    @Override
    public String customizeApnSettingsTile(boolean z, IconIdWrapper iconIdWrapper, String str) {
        return str;
    }

    @Override
    public String addOpTileSpecs(String str) {
        return str;
    }

    @Override
    public boolean doOperatorSupportTile(String str) {
        return false;
    }

    @Override
    public Object createTile(Object obj, String str) {
        return null;
    }

    @Override
    public void addOpViews(ViewGroup viewGroup) {
    }

    @Override
    public void registerCallbacks() {
    }

    @Override
    public void unregisterCallbacks() {
    }

    @Override
    public void setViewsVisibility(int i) {
    }

    @Override
    public void measureOpViews(int i) {
    }

    @Override
    public View getPreviousView(View view) {
        return view;
    }

    @Override
    public int getOpViewsHeight() {
        return 0;
    }

    @Override
    public void setOpViewsLayout(int i) {
    }

    @Override
    public String getTileLabel(String str) {
        return "";
    }

    @Override
    public void setHostAppInstance(Object obj) {
    }
}
