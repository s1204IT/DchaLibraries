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
    public boolean customizeDisplayDataUsage(boolean isDisplay) {
        Log.i(TAG, "customizeDisplayDataUsage, return isDisplay = " + isDisplay);
        return isDisplay;
    }

    @Override
    public String customizeQuickSettingsTileOrder(String defaultString) {
        return defaultString;
    }

    @Override
    public Object customizeAddQSTile(Object qsTile) {
        return null;
    }

    @Override
    public String customizeDataConnectionTile(int dataState, IconIdWrapper icon, String orgLabelStr) {
        Log.i(TAG, "customizeDataConnectionTile, icon = " + icon + ", orgLabelStr=" + orgLabelStr);
        return orgLabelStr;
    }

    @Override
    public String customizeDualSimSettingsTile(boolean enable, IconIdWrapper icon, String labelStr) {
        Log.i(TAG, "customizeDualSimSettingsTile, enable = " + enable + " icon=" + icon + " labelStr=" + labelStr);
        return labelStr;
    }

    @Override
    public void customizeSimDataConnectionTile(int state, IconIdWrapper icon) {
        Log.i(TAG, "customizeSimDataConnectionTile, state = " + state + " icon=" + icon);
    }

    @Override
    public String customizeApnSettingsTile(boolean enable, IconIdWrapper icon, String orgLabelStr) {
        return orgLabelStr;
    }

    @Override
    public String addOpTileSpecs(String defaultTileList) {
        return defaultTileList;
    }

    @Override
    public boolean doOperatorSupportTile(String tileSpec) {
        return false;
    }

    @Override
    public Object createTile(Object host, String tileSpec) {
        return null;
    }

    @Override
    public void addOpViews(ViewGroup vg) {
    }

    @Override
    public void registerCallbacks() {
    }

    @Override
    public void unregisterCallbacks() {
    }

    @Override
    public void setViewsVisibility(int visibility) {
    }

    @Override
    public void measureOpViews(int width) {
    }

    @Override
    public View getPreviousView(View v) {
        return v;
    }

    @Override
    public int getOpViewsHeight() {
        return 0;
    }

    @Override
    public void setOpViewsLayout(int aboveViewHeight) {
    }

    @Override
    public String getTileLabel(String tile) {
        return "";
    }
}
