package com.android.systemui.qs.tileimpl;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.ContextThemeWrapper;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSFactory;
import com.android.systemui.plugins.qs.QSIconView;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.qs.tiles.AirplaneModeTile;
import com.android.systemui.qs.tiles.BatterySaverTile;
import com.android.systemui.qs.tiles.BluetoothTile;
import com.android.systemui.qs.tiles.CastTile;
import com.android.systemui.qs.tiles.CellularTile;
import com.android.systemui.qs.tiles.ColorInversionTile;
import com.android.systemui.qs.tiles.DataSaverTile;
import com.android.systemui.qs.tiles.DndTile;
import com.android.systemui.qs.tiles.FlashlightTile;
import com.android.systemui.qs.tiles.HotspotTile;
import com.android.systemui.qs.tiles.IntentTile;
import com.android.systemui.qs.tiles.LocationTile;
import com.android.systemui.qs.tiles.NfcTile;
import com.android.systemui.qs.tiles.NightDisplayTile;
import com.android.systemui.qs.tiles.RotationLockTile;
import com.android.systemui.qs.tiles.UserTile;
import com.android.systemui.qs.tiles.WifiTile;
import com.android.systemui.qs.tiles.WorkModeTile;
import com.android.systemui.util.leak.GarbageMonitor;
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import com.mediatek.systemui.ext.OpSystemUICustomizationFactoryBase;
import com.mediatek.systemui.qs.tiles.ext.ApnSettingsTile;
import com.mediatek.systemui.qs.tiles.ext.DualSimSettingsTile;
import com.mediatek.systemui.qs.tiles.ext.MobileDataTile;
import com.mediatek.systemui.qs.tiles.ext.SimDataConnectionTile;
import com.mediatek.systemui.statusbar.util.SIMHelper;

public class QSFactoryImpl implements QSFactory {
    private final QSTileHost mHost;

    public QSFactoryImpl(QSTileHost qSTileHost) {
        this.mHost = qSTileHost;
    }

    @Override
    public QSTile createTile(String str) {
        QSTileImpl qSTileImplCreateTileInternal = createTileInternal(str);
        if (qSTileImplCreateTileInternal != null) {
            qSTileImplCreateTileInternal.handleStale();
        }
        return qSTileImplCreateTileInternal;
    }

    private QSTileImpl createTileInternal(String str) {
        IQuickSettingsPlugin iQuickSettingsPluginMakeQuickSettings;
        Context context = this.mHost.getContext();
        iQuickSettingsPluginMakeQuickSettings = OpSystemUICustomizationFactoryBase.getOpFactory(context).makeQuickSettings(context);
        switch (str) {
            case "wifi":
                return new WifiTile(this.mHost);
            case "bt":
                return new BluetoothTile(this.mHost);
            case "cell":
                return new CellularTile(this.mHost);
            case "dnd":
                return new DndTile(this.mHost);
            case "inversion":
                return new ColorInversionTile(this.mHost);
            case "airplane":
                return new AirplaneModeTile(this.mHost);
            case "work":
                return new WorkModeTile(this.mHost);
            case "rotation":
                return new RotationLockTile(this.mHost);
            case "flashlight":
                return new FlashlightTile(this.mHost);
            case "location":
                return new LocationTile(this.mHost);
            case "cast":
                return new CastTile(this.mHost);
            case "hotspot":
                return new HotspotTile(this.mHost);
            case "user":
                return new UserTile(this.mHost);
            case "battery":
                return new BatterySaverTile(this.mHost);
            case "saver":
                return new DataSaverTile(this.mHost);
            case "night":
                return new NightDisplayTile(this.mHost);
            case "nfc":
                return new NfcTile(this.mHost);
            default:
                if (str.equals("dataconnection") && !SIMHelper.isWifiOnlyDevice()) {
                    return new MobileDataTile(this.mHost);
                }
                if (str.equals("simdataconnection") && !SIMHelper.isWifiOnlyDevice() && iQuickSettingsPluginMakeQuickSettings.customizeAddQSTile(new SimDataConnectionTile(this.mHost)) != null) {
                    return (SimDataConnectionTile) iQuickSettingsPluginMakeQuickSettings.customizeAddQSTile(new SimDataConnectionTile(this.mHost));
                }
                if (str.equals("dulsimsettings") && !SIMHelper.isWifiOnlyDevice() && iQuickSettingsPluginMakeQuickSettings.customizeAddQSTile(new DualSimSettingsTile(this.mHost)) != null) {
                    return (DualSimSettingsTile) iQuickSettingsPluginMakeQuickSettings.customizeAddQSTile(new DualSimSettingsTile(this.mHost));
                }
                if (str.equals("apnsettings") && !SIMHelper.isWifiOnlyDevice() && iQuickSettingsPluginMakeQuickSettings.customizeAddQSTile(new ApnSettingsTile(this.mHost)) != null) {
                    return (ApnSettingsTile) iQuickSettingsPluginMakeQuickSettings.customizeAddQSTile(new ApnSettingsTile(this.mHost));
                }
                if (str.startsWith("intent(")) {
                    return IntentTile.create(this.mHost, str);
                }
                if (str.startsWith("custom(")) {
                    return CustomTile.create(this.mHost, str);
                }
                if (Build.IS_DEBUGGABLE && str.equals("dbg:mem")) {
                    return new GarbageMonitor.MemoryTile(this.mHost);
                }
                Log.w("QSFactory", "Bad tile spec: " + str);
                return null;
        }
    }

    @Override
    public com.android.systemui.plugins.qs.QSTileView createTileView(QSTile qSTile, boolean z) {
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(this.mHost.getContext(), R.style.qs_theme);
        QSIconView qSIconViewCreateTileView = qSTile.createTileView(contextThemeWrapper);
        if (z) {
            return new QSTileBaseView(contextThemeWrapper, qSIconViewCreateTileView, z);
        }
        return new QSTileView(contextThemeWrapper, qSIconViewCreateTileView);
    }
}
