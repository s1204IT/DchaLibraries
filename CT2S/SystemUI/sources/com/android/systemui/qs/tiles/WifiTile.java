package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.qs.QSDetailItems;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTileView;
import com.android.systemui.qs.SignalTileView;
import com.android.systemui.statusbar.policy.NetworkController;

public class WifiTile extends QSTile<QSTile.SignalState> {
    private static final Intent WIFI_SETTINGS = new Intent("android.settings.WIFI_SETTINGS");
    private final NetworkController.NetworkSignalChangedCallback mCallback;
    private final NetworkController mController;
    private final WifiDetailAdapter mDetailAdapter;
    private final QSTile.SignalState mStateBeforeClick;
    private final NetworkController.AccessPointController mWifiController;

    public WifiTile(QSTile.Host host) {
        super(host);
        this.mStateBeforeClick = newTileState();
        this.mCallback = new NetworkController.NetworkSignalChangedCallback() {
            @Override
            public void onWifiSignalChanged(boolean enabled, boolean connected, int wifiSignalIconId, boolean activityIn, boolean activityOut, String wifiSignalContentDescriptionId, String description) {
                if (WifiTile.DEBUG) {
                    Log.d(WifiTile.this.TAG, "onWifiSignalChanged enabled=" + enabled);
                }
                CallbackInfo info = new CallbackInfo();
                info.enabled = enabled;
                info.connected = connected;
                info.wifiSignalIconId = wifiSignalIconId;
                info.enabledDesc = description;
                info.activityIn = activityIn;
                info.activityOut = activityOut;
                info.wifiSignalContentDescription = wifiSignalContentDescriptionId;
                WifiTile.this.refreshState(info);
            }

            @Override
            public void onMobileDataSignalChanged(boolean enabled, int mobileSignalIconId, String mobileSignalContentDescriptionId, int dataTypeIconId, boolean activityIn, boolean activityOut, String dataTypeContentDescriptionId, String description, boolean isDataTypeIconWide) {
            }

            @Override
            public void onNoSimVisibleChanged(boolean noSims) {
            }

            @Override
            public void onAirplaneModeChanged(boolean enabled) {
            }

            @Override
            public void onMobileDataEnabled(boolean enabled) {
            }
        };
        this.mController = host.getNetworkController();
        this.mWifiController = this.mController.getAccessPointController();
        this.mDetailAdapter = new WifiDetailAdapter();
    }

    @Override
    public boolean supportsDualTargets() {
        return true;
    }

    @Override
    public QSTile.SignalState newTileState() {
        return new QSTile.SignalState();
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            this.mController.addNetworkSignalChangedCallback(this.mCallback);
            this.mWifiController.addAccessPointCallback(this.mDetailAdapter);
        } else {
            this.mController.removeNetworkSignalChangedCallback(this.mCallback);
            this.mWifiController.removeAccessPointCallback(this.mDetailAdapter);
        }
    }

    @Override
    public QSTile.DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
    }

    @Override
    public QSTileView createTileView(Context context) {
        return new SignalTileView(context);
    }

    @Override
    protected void handleClick() {
        ((QSTile.SignalState) this.mState).copyTo(this.mStateBeforeClick);
        this.mController.setWifiEnabled(!((QSTile.SignalState) this.mState).enabled);
    }

    @Override
    protected void handleSecondaryClick() {
        if (!this.mWifiController.canConfigWifi()) {
            this.mHost.startSettingsActivity(new Intent("android.settings.WIFI_SETTINGS"));
            return;
        }
        if (!((QSTile.SignalState) this.mState).enabled) {
            this.mController.setWifiEnabled(true);
            ((QSTile.SignalState) this.mState).enabled = true;
        }
        showDetail(true);
    }

    @Override
    public void handleUpdateState(QSTile.SignalState state, Object arg) {
        String signalContentDescription;
        state.visible = true;
        if (DEBUG) {
            Log.d(this.TAG, "handleUpdateState arg=" + arg);
        }
        if (arg != null) {
            CallbackInfo cb = (CallbackInfo) arg;
            boolean wifiConnected = cb.enabled && cb.wifiSignalIconId > 0 && cb.enabledDesc != null;
            boolean wifiNotConnected = cb.wifiSignalIconId > 0 && cb.enabledDesc == null;
            boolean enabledChanging = state.enabled != cb.enabled;
            if (enabledChanging) {
                this.mDetailAdapter.setItemsVisible(cb.enabled);
                fireToggleStateChanged(cb.enabled);
            }
            state.enabled = cb.enabled;
            state.connected = wifiConnected;
            state.activityIn = cb.enabled && cb.activityIn;
            state.activityOut = cb.enabled && cb.activityOut;
            state.filter = true;
            Resources r = this.mContext.getResources();
            if (!state.enabled) {
                state.icon = QSTile.ResourceIcon.get(R.drawable.ic_qs_wifi_disabled);
                state.label = r.getString(R.string.quick_settings_wifi_label);
                signalContentDescription = r.getString(R.string.accessibility_wifi_off);
            } else if (wifiConnected) {
                state.icon = QSTile.ResourceIcon.get(cb.wifiSignalIconId);
                state.label = removeDoubleQuotes(cb.enabledDesc);
                signalContentDescription = cb.wifiSignalContentDescription;
            } else if (wifiNotConnected) {
                state.icon = QSTile.ResourceIcon.get(R.drawable.ic_qs_wifi_0);
                state.label = r.getString(R.string.quick_settings_wifi_label);
                signalContentDescription = r.getString(R.string.accessibility_no_wifi);
            } else {
                state.icon = QSTile.ResourceIcon.get(R.drawable.ic_qs_wifi_no_network);
                state.label = r.getString(R.string.quick_settings_wifi_label);
                signalContentDescription = r.getString(R.string.accessibility_wifi_off);
            }
            state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_wifi, signalContentDescription);
            String wifiName = state.label;
            if (state.connected) {
                wifiName = r.getString(R.string.accessibility_wifi_name, state.label);
            }
            state.dualLabelContentDescription = wifiName;
        }
    }

    @Override
    protected boolean shouldAnnouncementBeDelayed() {
        return this.mStateBeforeClick.enabled == ((QSTile.SignalState) this.mState).enabled;
    }

    @Override
    protected String composeChangeAnnouncement() {
        return ((QSTile.SignalState) this.mState).enabled ? this.mContext.getString(R.string.accessibility_quick_settings_wifi_changed_on) : this.mContext.getString(R.string.accessibility_quick_settings_wifi_changed_off);
    }

    private static String removeDoubleQuotes(String string) {
        if (string == null) {
            return null;
        }
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }

    private static final class CallbackInfo {
        boolean activityIn;
        boolean activityOut;
        boolean connected;
        boolean enabled;
        String enabledDesc;
        String wifiSignalContentDescription;
        int wifiSignalIconId;

        private CallbackInfo() {
        }

        public String toString() {
            return "CallbackInfo[enabled=" + this.enabled + ",connected=" + this.connected + ",wifiSignalIconId=" + this.wifiSignalIconId + ",enabledDesc=" + this.enabledDesc + ",activityIn=" + this.activityIn + ",activityOut=" + this.activityOut + ",wifiSignalContentDescription=" + this.wifiSignalContentDescription + ']';
        }
    }

    private final class WifiDetailAdapter implements QSDetailItems.Callback, QSTile.DetailAdapter, NetworkController.AccessPointController.AccessPointCallback {
        private NetworkController.AccessPointController.AccessPoint[] mAccessPoints;
        private QSDetailItems mItems;

        private WifiDetailAdapter() {
        }

        @Override
        public int getTitle() {
            return R.string.quick_settings_wifi_label;
        }

        @Override
        public Intent getSettingsIntent() {
            return WifiTile.WIFI_SETTINGS;
        }

        @Override
        public Boolean getToggleState() {
            return Boolean.valueOf(((QSTile.SignalState) WifiTile.this.mState).enabled);
        }

        @Override
        public void setToggleState(boolean state) {
            if (WifiTile.DEBUG) {
                Log.d(WifiTile.this.TAG, "setToggleState " + state);
            }
            WifiTile.this.mController.setWifiEnabled(state);
            WifiTile.this.showDetail(false);
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            if (WifiTile.DEBUG) {
                Log.d(WifiTile.this.TAG, "createDetailView convertView=" + (convertView != null));
            }
            this.mAccessPoints = null;
            WifiTile.this.mWifiController.scanForAccessPoints();
            WifiTile.this.fireScanStateChanged(true);
            this.mItems = QSDetailItems.convertOrInflate(context, convertView, parent);
            this.mItems.setTagSuffix("Wifi");
            this.mItems.setCallback(this);
            this.mItems.setEmptyState(R.drawable.ic_qs_wifi_detail_empty, R.string.quick_settings_wifi_detail_empty_text);
            updateItems();
            setItemsVisible(((QSTile.SignalState) WifiTile.this.mState).enabled);
            return this.mItems;
        }

        @Override
        public void onAccessPointsChanged(NetworkController.AccessPointController.AccessPoint[] accessPoints) {
            this.mAccessPoints = accessPoints;
            updateItems();
            if (accessPoints != null && accessPoints.length > 0) {
                WifiTile.this.fireScanStateChanged(false);
            }
        }

        @Override
        public void onSettingsActivityTriggered(Intent settingsIntent) {
            WifiTile.this.mHost.startSettingsActivity(settingsIntent);
        }

        @Override
        public void onDetailItemClick(QSDetailItems.Item item) {
            if (item != null && item.tag != null) {
                NetworkController.AccessPointController.AccessPoint ap = (NetworkController.AccessPointController.AccessPoint) item.tag;
                if (!ap.isConnected && WifiTile.this.mWifiController.connect(ap)) {
                    WifiTile.this.mHost.collapsePanels();
                }
                WifiTile.this.showDetail(false);
            }
        }

        @Override
        public void onDetailItemDisconnect(QSDetailItems.Item item) {
        }

        public void setItemsVisible(boolean visible) {
            if (this.mItems != null) {
                this.mItems.setItemsVisible(visible);
            }
        }

        private void updateItems() {
            Drawable drawable;
            if (this.mItems != null) {
                QSDetailItems.Item[] items = null;
                if (this.mAccessPoints != null) {
                    items = new QSDetailItems.Item[this.mAccessPoints.length];
                    for (int i = 0; i < this.mAccessPoints.length; i++) {
                        NetworkController.AccessPointController.AccessPoint ap = this.mAccessPoints[i];
                        QSDetailItems.Item item = new QSDetailItems.Item();
                        item.tag = ap;
                        item.icon = ap.iconId;
                        item.line1 = ap.ssid;
                        if (ap.isConnected) {
                            item.line2 = WifiTile.this.mContext.getString(ap.isConfigured ? R.string.quick_settings_connected : R.string.quick_settings_connected_via_wfa);
                        } else if (ap.networkId >= 0) {
                        }
                        if (ap.hasSecurity) {
                            drawable = WifiTile.this.mContext.getDrawable(R.drawable.qs_ic_wifi_lock);
                        } else {
                            drawable = null;
                        }
                        item.overlay = drawable;
                        items[i] = item;
                    }
                }
                this.mItems.setItems(items);
            }
        }
    }
}
