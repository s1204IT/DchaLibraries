package com.android.systemui.qs.tiles;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTileView;
import com.android.systemui.qs.SignalTileView;
import com.android.systemui.statusbar.policy.NetworkController;

public class CellularTile extends QSTile<QSTile.SignalState> {
    private static final Intent CELLULAR_SETTINGS = new Intent().setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity"));
    private final NetworkController.NetworkSignalChangedCallback mCallback;
    private final NetworkController mController;
    private final NetworkController.MobileDataController mDataController;
    private final CellularDetailAdapter mDetailAdapter;

    public CellularTile(QSTile.Host host) {
        super(host);
        this.mCallback = new NetworkController.NetworkSignalChangedCallback() {
            private final CallbackInfo mInfo = new CallbackInfo();

            @Override
            public void onWifiSignalChanged(boolean enabled, boolean connected, int wifiSignalIconId, boolean activityIn, boolean activityOut, String wifiSignalContentDescriptionId, String description) {
                this.mInfo.wifiEnabled = enabled;
                this.mInfo.wifiConnected = connected;
                CellularTile.this.refreshState(this.mInfo);
            }

            @Override
            public void onMobileDataSignalChanged(boolean enabled, int mobileSignalIconId, String mobileSignalContentDescriptionId, int dataTypeIconId, boolean activityIn, boolean activityOut, String dataTypeContentDescriptionId, String description, boolean isDataTypeIconWide) {
                this.mInfo.enabled = enabled;
                this.mInfo.mobileSignalIconId = mobileSignalIconId;
                this.mInfo.signalContentDescription = mobileSignalContentDescriptionId;
                this.mInfo.dataTypeIconId = dataTypeIconId;
                this.mInfo.dataContentDescription = dataTypeContentDescriptionId;
                this.mInfo.activityIn = activityIn;
                this.mInfo.activityOut = activityOut;
                this.mInfo.enabledDesc = description;
                this.mInfo.isDataTypeIconWide = isDataTypeIconWide;
                CellularTile.this.refreshState(this.mInfo);
            }

            @Override
            public void onNoSimVisibleChanged(boolean visible) {
                this.mInfo.noSim = visible;
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
            public void onAirplaneModeChanged(boolean enabled) {
                this.mInfo.airplaneModeEnabled = enabled;
                CellularTile.this.refreshState(this.mInfo);
            }

            @Override
            public void onMobileDataEnabled(boolean enabled) {
                CellularTile.this.mDetailAdapter.setMobileDataEnabled(enabled);
            }
        };
        this.mController = host.getNetworkController();
        this.mDataController = this.mController.getMobileDataController();
        this.mDetailAdapter = new CellularDetailAdapter();
    }

    @Override
    protected QSTile.SignalState newTileState() {
        return new QSTile.SignalState();
    }

    @Override
    public QSTile.DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            this.mController.addNetworkSignalChangedCallback(this.mCallback);
        } else {
            this.mController.removeNetworkSignalChangedCallback(this.mCallback);
        }
    }

    @Override
    public QSTileView createTileView(Context context) {
        return new SignalTileView(context);
    }

    @Override
    protected void handleClick() {
        if (this.mDataController.isMobileDataSupported()) {
            showDetail(true);
        } else {
            this.mHost.startSettingsActivity(CELLULAR_SETTINGS);
        }
    }

    @Override
    protected void handleUpdateState(QSTile.SignalState state, Object arg) {
        CallbackInfo cb;
        int iconId;
        state.visible = this.mController.hasMobileDataFeature();
        if (state.visible && (cb = (CallbackInfo) arg) != null) {
            Resources r = this.mContext.getResources();
            if (cb.noSim) {
                iconId = R.drawable.ic_qs_no_sim;
            } else {
                iconId = (!cb.enabled || cb.airplaneModeEnabled) ? R.drawable.ic_qs_signal_disabled : cb.mobileSignalIconId > 0 ? cb.mobileSignalIconId : R.drawable.ic_qs_signal_no_signal;
            }
            state.icon = QSTile.ResourceIcon.get(iconId);
            state.isOverlayIconWide = cb.isDataTypeIconWide;
            state.autoMirrorDrawable = !cb.noSim;
            state.overlayIconId = (!cb.enabled || cb.dataTypeIconId <= 0) ? 0 : cb.dataTypeIconId;
            state.filter = iconId != R.drawable.ic_qs_no_sim;
            state.activityIn = cb.enabled && cb.activityIn;
            state.activityOut = cb.enabled && cb.activityOut;
            state.label = cb.enabled ? removeTrailingPeriod(cb.enabledDesc) : r.getString(R.string.quick_settings_rssi_emergency_only);
            String signalContentDesc = (!cb.enabled || cb.mobileSignalIconId <= 0) ? r.getString(R.string.accessibility_no_signal) : cb.signalContentDescription;
            String dataContentDesc = (!cb.enabled || cb.dataTypeIconId <= 0 || cb.wifiEnabled) ? r.getString(R.string.accessibility_no_data) : cb.dataContentDescription;
            state.contentDescription = r.getString(R.string.accessibility_quick_settings_mobile, signalContentDesc, dataContentDesc, state.label);
        }
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
        boolean wifiConnected;
        boolean wifiEnabled;

        private CallbackInfo() {
        }
    }

    private final class CellularDetailAdapter implements QSTile.DetailAdapter {
        private CellularDetailAdapter() {
        }

        @Override
        public int getTitle() {
            return R.string.quick_settings_cellular_detail_title;
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
            return CellularTile.CELLULAR_SETTINGS;
        }

        @Override
        public void setToggleState(boolean state) {
            CellularTile.this.mDataController.setMobileDataEnabled(state);
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            DataUsageDetailView v = (DataUsageDetailView) (convertView != null ? convertView : LayoutInflater.from(CellularTile.this.mContext).inflate(R.layout.data_usage, parent, false));
            NetworkController.MobileDataController.DataUsageInfo info = CellularTile.this.mDataController.getDataUsageInfo();
            if (info != null) {
                v.bind(info);
            }
            return v;
        }

        public void setMobileDataEnabled(boolean enabled) {
            CellularTile.this.fireToggleStateChanged(enabled);
        }
    }
}
