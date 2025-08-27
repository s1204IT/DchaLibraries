package com.mediatek.systemui.ext;

import android.view.View;
import android.view.ViewGroup;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;

/* loaded from: classes.dex */
public interface IQuickSettingsPlugin {
    String addOpTileSpecs(String str);

    void addOpViews(ViewGroup viewGroup);

    Object createTile(Object obj, String str);

    Object customizeAddQSTile(Object obj);

    String customizeApnSettingsTile(boolean z, IconIdWrapper iconIdWrapper, String str);

    String customizeDataConnectionTile(int i, IconIdWrapper iconIdWrapper, String str);

    boolean customizeDisplayDataUsage(boolean z);

    String customizeDualSimSettingsTile(boolean z, IconIdWrapper iconIdWrapper, String str);

    String customizeQuickSettingsTileOrder(String str);

    void customizeSimDataConnectionTile(int i, IconIdWrapper iconIdWrapper);

    void disableDataForOtherSubscriptions();

    boolean doOperatorSupportTile(String str);

    int getOpViewsHeight();

    View getPreviousView(View view);

    String getTileLabel(String str);

    void measureOpViews(int i);

    void registerCallbacks();

    void setHostAppInstance(Object obj);

    void setOpViewsLayout(int i);

    void setViewsVisibility(int i);

    void unregisterCallbacks();
}
