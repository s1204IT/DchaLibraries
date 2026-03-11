package com.android.systemui.qs.tiles;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.BenesseExtension;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.net.DataUsageController;
import com.android.systemui.R;
import com.android.systemui.qs.QSIconView;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.SignalTileView;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.SignalCallbackAdapter;
import com.mediatek.systemui.PluginManager;
import com.mediatek.systemui.ext.IQuickSettingsPlugin;

public class CellularTile extends QSTile<QSTile.SignalState> {
    static final Intent CELLULAR_SETTINGS = new Intent().setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity"));
    private final NetworkController mController;
    private final DataUsageController mDataController;
    private final CellularDetailAdapter mDetailAdapter;
    private boolean mDisplayDataUsage;
    private QSTile.Icon mIcon;
    private IQuickSettingsPlugin mQuickSettingsPlugin;
    private final CellSignalCallback mSignalCallback;
    private TelephonyManager mTelephonyManager;

    public CellularTile(QSTile.Host host) {
        super(host);
        this.mSignalCallback = new CellSignalCallback(this, null);
        this.mController = host.getNetworkController();
        this.mDataController = this.mController.getMobileDataController();
        this.mDetailAdapter = new CellularDetailAdapter(this, 0 == true ? 1 : 0);
        this.mQuickSettingsPlugin = PluginManager.getQuickSettingsPlugin(this.mContext);
        this.mDisplayDataUsage = this.mQuickSettingsPlugin.customizeDisplayDataUsage(false);
        this.mIcon = QSTile.ResourceIcon.get(R.drawable.ic_qs_data_usage);
        this.mTelephonyManager = TelephonyManager.from(this.mContext);
    }

    @Override
    public QSTile.SignalState newTileState() {
        return new QSTile.SignalState();
    }

    @Override
    public QSTile.DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            this.mController.addSignalCallback(this.mSignalCallback);
        } else {
            this.mController.removeSignalCallback(this.mSignalCallback);
        }
    }

    @Override
    public QSIconView createTileView(Context context) {
        return new SignalTileView(context);
    }

    @Override
    public Intent getLongClickIntent() {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        return CELLULAR_SETTINGS;
    }

    @Override
    protected void handleClick() {
        MetricsLogger.action(this.mContext, getMetricsCategory());
        if (this.mDataController.isMobileDataSupported() && isDefaultDataSimExist()) {
            showDetail(true);
        } else {
            if (BenesseExtension.getDchaState() != 0) {
                return;
            }
            this.mHost.startActivityDismissingKeyguard(CELLULAR_SETTINGS);
        }
    }

    @Override
    protected void handleSecondaryClick() {
        boolean zIsMobileDataEnabled;
        Log.d("CellularTile", "handleSecondaryClick()");
        if (this.mDisplayDataUsage) {
            handleClick();
            return;
        }
        if (isDefaultDataSimExist()) {
            if (!this.mDataController.isMobileDataSupported()) {
                zIsMobileDataEnabled = false;
            } else {
                zIsMobileDataEnabled = this.mDataController.isMobileDataEnabled();
            }
            MetricsLogger.action(this.mContext, 155, !zIsMobileDataEnabled);
            this.mDataController.setMobileDataEnabled(zIsMobileDataEnabled ? false : true);
            if (!zIsMobileDataEnabled) {
                disableDataForOtherSubscriptions();
            }
        }
    }

    @Override
    public CharSequence getTileLabel() {
        if (this.mDisplayDataUsage) {
            return this.mContext.getString(R.string.data_usage);
        }
        return this.mContext.getString(R.string.quick_settings_cellular_detail_title);
    }

    @Override
    public void handleUpdateState(QSTile.SignalState state, Object arg) {
        int iconId;
        String string;
        String signalContentDesc;
        if (this.mDisplayDataUsage) {
            Log.i("CellularTile", "customize datausage, displayDataUsage = " + this.mDisplayDataUsage);
            state.icon = this.mIcon;
            state.label = this.mContext.getString(R.string.data_usage);
            state.contentDescription = this.mContext.getString(R.string.data_usage);
            return;
        }
        CallbackInfo cb = (CallbackInfo) arg;
        if (cb == null) {
            cb = this.mSignalCallback.mInfo;
        }
        Resources r = this.mContext.getResources();
        if (cb.noSim) {
            iconId = R.drawable.ic_qs_no_sim;
        } else if (!cb.enabled || cb.airplaneModeEnabled) {
            iconId = R.drawable.ic_qs_signal_disabled;
        } else {
            iconId = cb.mobileSignalIconId > 0 ? cb.mobileSignalIconId : R.drawable.ic_qs_signal_no_signal;
        }
        state.icon = QSTile.ResourceIcon.get(iconId);
        state.isOverlayIconWide = cb.isDataTypeIconWide;
        state.autoMirrorDrawable = !cb.noSim;
        state.overlayIconId = (!cb.enabled || cb.dataTypeIconId <= 0 || cb.airplaneModeEnabled) ? 0 : cb.dataTypeIconId;
        state.filter = iconId != R.drawable.ic_qs_no_sim;
        state.activityIn = cb.enabled ? cb.activityIn : false;
        state.activityOut = cb.enabled ? cb.activityOut : false;
        if (cb.enabled) {
            string = removeTrailingPeriod(cb.enabledDesc);
        } else {
            string = r.getString(R.string.quick_settings_rssi_emergency_only);
        }
        state.label = string;
        if (cb.enabled && cb.mobileSignalIconId > 0) {
            signalContentDesc = cb.signalContentDescription;
        } else {
            signalContentDesc = r.getString(R.string.accessibility_no_signal);
        }
        if (cb.noSim) {
            state.contentDescription = state.label;
        } else {
            String enabledDesc = cb.enabled ? r.getString(R.string.accessibility_cell_data_on) : r.getString(R.string.accessibility_cell_data_off);
            state.contentDescription = r.getString(R.string.accessibility_quick_settings_mobile, enabledDesc, signalContentDesc, state.label);
            state.minimalContentDescription = r.getString(R.string.accessibility_quick_settings_mobile, r.getString(R.string.accessibility_cell_data), signalContentDesc, state.label);
        }
        state.contentDescription += "," + r.getString(R.string.accessibility_quick_settings_open_settings, getTileLabel());
        String name = Button.class.getName();
        state.expandedAccessibilityClassName = name;
        state.minimalAccessibilityClassName = name;
        state.value = this.mDataController.isMobileDataSupported() ? this.mDataController.isMobileDataEnabled() : false;
        if (this.mTelephonyManager.getNetworkOperator() == null || cb.noSim || isDefaultDataSimExist()) {
            return;
        }
        Log.d("CellularTile", "handleUpdateState(), default data sim not exist");
        state.icon = QSTile.ResourceIcon.get(R.drawable.ic_qs_data_sim_not_set);
        state.label = r.getString(R.string.quick_settings_data_sim_notset);
        state.overlayIconId = 0;
        state.filter = true;
        state.activityIn = false;
        state.activityOut = false;
    }

    @Override
    public int getMetricsCategory() {
        return 115;
    }

    @Override
    public boolean isAvailable() {
        return this.mController.hasMobileDataFeature();
    }

    public static String removeTrailingPeriod(String string) {
        if (string == null) {
            return null;
        }
        int length = string.length();
        if (string.endsWith(".")) {
            return string.substring(0, length - 1);
        }
        return string;
    }

    private static final class CallbackInfo {
        boolean activityIn;
        boolean activityOut;
        boolean airplaneModeEnabled;
        String dataContentDescription;
        int dataTypeIconId;
        boolean enabled;
        String enabledDesc;
        boolean isDataTypeIconWide;
        int mobileSignalIconId;
        boolean noSim;
        String signalContentDescription;
        boolean wifiEnabled;

        CallbackInfo(CallbackInfo callbackInfo) {
            this();
        }

        private CallbackInfo() {
        }
    }

    private final class CellSignalCallback extends SignalCallbackAdapter {
        private final CallbackInfo mInfo;

        CellSignalCallback(CellularTile this$0, CellSignalCallback cellSignalCallback) {
            this();
        }

        private CellSignalCallback() {
            this.mInfo = new CallbackInfo(null);
        }

        @Override
        public void setWifiIndicators(boolean enabled, NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, boolean activityIn, boolean activityOut, String description) {
            this.mInfo.wifiEnabled = enabled;
            CellularTile.this.refreshState(this.mInfo);
        }

        @Override
        public void setMobileDataIndicators(NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, int statusType, int networkIcon, int volteIcon, int qsType, boolean activityIn, boolean activityOut, String typeContentDescription, String description, boolean isWide, int subId) {
            if (qsIcon == null) {
                Log.d("CellularTile", "setMobileDataIndicator qsIcon = null, Not data sim, don't display");
                return;
            }
            this.mInfo.enabled = qsIcon.visible;
            this.mInfo.mobileSignalIconId = qsIcon.icon;
            this.mInfo.signalContentDescription = qsIcon.contentDescription;
            this.mInfo.dataTypeIconId = qsType;
            this.mInfo.dataContentDescription = typeContentDescription;
            this.mInfo.activityIn = activityIn;
            this.mInfo.activityOut = activityOut;
            this.mInfo.enabledDesc = description;
            CallbackInfo callbackInfo = this.mInfo;
            if (qsType == 0) {
                isWide = false;
            }
            callbackInfo.isDataTypeIconWide = isWide;
            Log.d("CellularTile", "setMobileDataIndicators info.enabled = " + this.mInfo.enabled + " mInfo.mobileSignalIconId = " + this.mInfo.mobileSignalIconId + " mInfo.signalContentDescription = " + this.mInfo.signalContentDescription + " mInfo.dataTypeIconId = " + this.mInfo.dataTypeIconId + " mInfo.dataContentDescription = " + this.mInfo.dataContentDescription + " mInfo.activityIn = " + this.mInfo.activityIn + " mInfo.activityOut = " + this.mInfo.activityOut + " mInfo.enabledDesc = " + this.mInfo.enabledDesc + " mInfo.isDataTypeIconWide = " + this.mInfo.isDataTypeIconWide);
            CellularTile.this.refreshState(this.mInfo);
        }

        @Override
        public void setNoSims(boolean show) {
            Log.d("CellularTile", "setNoSims, noSim = " + show);
            this.mInfo.noSim = show;
            if (this.mInfo.noSim) {
                this.mInfo.mobileSignalIconId = 0;
                this.mInfo.dataTypeIconId = 0;
                this.mInfo.enabled = true;
                this.mInfo.enabledDesc = CellularTile.this.mContext.getString(R.string.keyguard_missing_sim_message_short);
                this.mInfo.signalContentDescription = this.mInfo.enabledDesc;
            }
            CellularTile.this.refreshState(this.mInfo);
        }

        @Override
        public void setIsAirplaneMode(NetworkController.IconState icon) {
            this.mInfo.airplaneModeEnabled = icon.visible;
            CellularTile.this.refreshState(this.mInfo);
        }

        @Override
        public void setMobileDataEnabled(boolean enabled) {
            CellularTile.this.mDetailAdapter.setMobileDataEnabled(enabled);
        }
    }

    private final class CellularDetailAdapter implements QSTile.DetailAdapter {
        CellularDetailAdapter(CellularTile this$0, CellularDetailAdapter cellularDetailAdapter) {
            this();
        }

        private CellularDetailAdapter() {
        }

        @Override
        public CharSequence getTitle() {
            return CellularTile.this.mContext.getString(R.string.quick_settings_cellular_detail_title);
        }

        @Override
        public Boolean getToggleState() {
            if (CellularTile.this.mDataController.isMobileDataSupported()) {
                return Boolean.valueOf(CellularTile.this.mDataController.isMobileDataEnabled());
            }
            return null;
        }

        @Override
        public Intent getSettingsIntent() {
            if (BenesseExtension.getDchaState() != 0) {
                return null;
            }
            return CellularTile.CELLULAR_SETTINGS;
        }

        @Override
        public void setToggleState(boolean state) {
            MetricsLogger.action(CellularTile.this.mContext, 155, state);
            CellularTile.this.mDataController.setMobileDataEnabled(state);
            if (state) {
                CellularTile.this.disableDataForOtherSubscriptions();
            }
        }

        @Override
        public int getMetricsCategory() {
            return 117;
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            DataUsageDetailView v = (DataUsageDetailView) (convertView != null ? convertView : LayoutInflater.from(CellularTile.this.mContext).inflate(R.layout.data_usage, parent, false));
            DataUsageController.DataUsageInfo info = CellularTile.this.mDataController.getDataUsageInfo();
            if (info == null) {
                return v;
            }
            v.bind(info);
            return v;
        }

        public void setMobileDataEnabled(boolean enabled) {
            CellularTile.this.fireToggleStateChanged(enabled);
        }
    }

    public boolean isDefaultDataSimExist() {
        int[] subList = SubscriptionManager.from(this.mContext).getActiveSubscriptionIdList();
        int defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        Log.d("CellularTile", "isDefaultDataSimExist, Default data sub id : " + defaultDataSubId);
        for (int subId : subList) {
            if (subId == defaultDataSubId) {
                return true;
            }
        }
        return false;
    }

    public void disableDataForOtherSubscriptions() {
        int[] subList = SubscriptionManager.from(this.mContext).getActiveSubscriptionIdList();
        int defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        for (int subId : subList) {
            if (subId != defaultDataSubId && this.mTelephonyManager.getDataEnabled(subId)) {
                Log.d("CellularTile", "Disable other sub's data : " + subId);
                this.mTelephonyManager.setDataEnabled(subId, false);
            }
        }
    }
}
