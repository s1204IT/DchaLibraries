package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.BenesseExtension;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.wifi.AccessPoint;
import com.android.systemui.R;
import com.android.systemui.qs.QSDetailItems;
import com.android.systemui.qs.QSIconView;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.SignalTileView;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.SignalCallbackAdapter;
import java.util.List;

public class WifiTile extends QSTile<QSTile.SignalState> {
    private static final Intent WIFI_SETTINGS = new Intent("android.settings.WIFI_SETTINGS");
    private final NetworkController mController;
    private final WifiDetailAdapter mDetailAdapter;
    protected final WifiSignalCallback mSignalCallback;
    private final QSTile.SignalState mStateBeforeClick;
    private final NetworkController.AccessPointController mWifiController;

    public WifiTile(QSTile.Host host) {
        super(host);
        this.mStateBeforeClick = newTileState();
        this.mSignalCallback = new WifiSignalCallback();
        this.mController = host.getNetworkController();
        this.mWifiController = this.mController.getAccessPointController();
        this.mDetailAdapter = new WifiDetailAdapter(this, null);
    }

    @Override
    public QSTile.SignalState newTileState() {
        return new QSTile.SignalState();
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
    public void setDetailListening(boolean listening) {
        if (listening) {
            this.mWifiController.addAccessPointCallback(this.mDetailAdapter);
        } else {
            this.mWifiController.removeAccessPointCallback(this.mDetailAdapter);
        }
    }

    @Override
    public QSTile.DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
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
        return WIFI_SETTINGS;
    }

    @Override
    protected void handleSecondaryClick() {
        ((QSTile.SignalState) this.mState).copyTo(this.mStateBeforeClick);
        MetricsLogger.action(this.mContext, getMetricsCategory(), !((QSTile.SignalState) this.mState).value);
        this.mController.setWifiEnabled(((QSTile.SignalState) this.mState).value ? false : true);
    }

    @Override
    protected void handleClick() {
        if (!this.mWifiController.canConfigWifi() && BenesseExtension.getDchaState() == 0) {
            this.mHost.startActivityDismissingKeyguard(new Intent("android.settings.WIFI_SETTINGS"));
            return;
        }
        if (!((QSTile.SignalState) this.mState).value) {
            this.mController.setWifiEnabled(true);
            ((QSTile.SignalState) this.mState).value = true;
        }
        showDetail(true);
    }

    @Override
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_wifi_label);
    }

    @Override
    public void handleUpdateState(QSTile.SignalState state, Object arg) {
        if (DEBUG) {
            Log.d(this.TAG, "handleUpdateState arg=" + arg);
        }
        CallbackInfo cb = (CallbackInfo) arg;
        if (cb == null) {
            cb = this.mSignalCallback.mInfo;
        }
        boolean wifiConnected = cb.enabled && cb.wifiSignalIconId > 0 && cb.enabledDesc != null;
        boolean wifiNotConnected = cb.wifiSignalIconId > 0 && cb.enabledDesc == null;
        boolean enabledChanging = state.value != cb.enabled;
        if (enabledChanging) {
            this.mDetailAdapter.setItemsVisible(cb.enabled);
            fireToggleStateChanged(cb.enabled);
        }
        state.value = cb.enabled;
        state.connected = wifiConnected;
        state.activityIn = cb.enabled ? cb.activityIn : false;
        state.activityOut = cb.enabled ? cb.activityOut : false;
        state.filter = true;
        StringBuffer minimalContentDescription = new StringBuffer();
        StringBuffer expandedContentDescription = new StringBuffer();
        Resources r = this.mContext.getResources();
        if (!state.value) {
            state.icon = QSTile.ResourceIcon.get(R.drawable.ic_qs_wifi_disabled);
            state.label = r.getString(R.string.quick_settings_wifi_label);
        } else if (wifiConnected) {
            state.icon = QSTile.ResourceIcon.get(cb.wifiSignalIconId);
            state.label = removeDoubleQuotes(cb.enabledDesc);
        } else if (wifiNotConnected) {
            state.icon = QSTile.ResourceIcon.get(R.drawable.ic_qs_wifi_disconnected);
            state.label = r.getString(R.string.quick_settings_wifi_label);
        } else {
            state.icon = QSTile.ResourceIcon.get(R.drawable.ic_qs_wifi_no_network);
            state.label = r.getString(R.string.quick_settings_wifi_label);
        }
        minimalContentDescription.append(this.mContext.getString(R.string.quick_settings_wifi_label)).append(",");
        if (state.value) {
            expandedContentDescription.append(r.getString(R.string.quick_settings_wifi_on_label)).append(",");
            if (wifiConnected) {
                minimalContentDescription.append(cb.wifiSignalContentDescription).append(",");
                minimalContentDescription.append(removeDoubleQuotes(cb.enabledDesc));
                expandedContentDescription.append(cb.wifiSignalContentDescription).append(",");
                expandedContentDescription.append(removeDoubleQuotes(cb.enabledDesc));
            }
        } else {
            expandedContentDescription.append(r.getString(R.string.quick_settings_wifi_off_label));
        }
        state.minimalContentDescription = minimalContentDescription;
        expandedContentDescription.append(",").append(r.getString(R.string.accessibility_quick_settings_open_settings, getTileLabel()));
        state.contentDescription = expandedContentDescription;
        CharSequence wifiName = state.label;
        if (state.connected) {
            wifiName = r.getString(R.string.accessibility_wifi_name, state.label);
        }
        state.dualLabelContentDescription = wifiName;
        state.expandedAccessibilityClassName = Button.class.getName();
        state.minimalAccessibilityClassName = Switch.class.getName();
    }

    @Override
    public int getMetricsCategory() {
        return 126;
    }

    @Override
    protected boolean shouldAnnouncementBeDelayed() {
        return this.mStateBeforeClick.value == ((QSTile.SignalState) this.mState).value;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (((QSTile.SignalState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_wifi_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_wifi_changed_off);
    }

    @Override
    public boolean isAvailable() {
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi");
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

    protected static final class CallbackInfo {
        boolean activityIn;
        boolean activityOut;
        boolean connected;
        boolean enabled;
        String enabledDesc;
        String wifiSignalContentDescription;
        int wifiSignalIconId;

        protected CallbackInfo() {
        }

        public String toString() {
            return "CallbackInfo[enabled=" + this.enabled + ",connected=" + this.connected + ",wifiSignalIconId=" + this.wifiSignalIconId + ",enabledDesc=" + this.enabledDesc + ",activityIn=" + this.activityIn + ",activityOut=" + this.activityOut + ",wifiSignalContentDescription=" + this.wifiSignalContentDescription + ']';
        }
    }

    protected final class WifiSignalCallback extends SignalCallbackAdapter {
        final CallbackInfo mInfo = new CallbackInfo();

        protected WifiSignalCallback() {
        }

        @Override
        public void setWifiIndicators(boolean enabled, NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, boolean activityIn, boolean activityOut, String description) {
            if (WifiTile.DEBUG) {
                Log.d(WifiTile.this.TAG, "onWifiSignalChanged enabled=" + enabled);
            }
            this.mInfo.enabled = enabled;
            this.mInfo.connected = qsIcon.visible;
            this.mInfo.wifiSignalIconId = qsIcon.icon;
            this.mInfo.enabledDesc = description;
            this.mInfo.activityIn = activityIn;
            this.mInfo.activityOut = activityOut;
            this.mInfo.wifiSignalContentDescription = qsIcon.contentDescription;
            WifiTile.this.refreshState(this.mInfo);
        }
    }

    private final class WifiDetailAdapter implements QSTile.DetailAdapter, NetworkController.AccessPointController.AccessPointCallback, QSDetailItems.Callback {
        private AccessPoint[] mAccessPoints;
        private QSDetailItems mItems;

        WifiDetailAdapter(WifiTile this$0, WifiDetailAdapter wifiDetailAdapter) {
            this();
        }

        private WifiDetailAdapter() {
        }

        @Override
        public CharSequence getTitle() {
            return WifiTile.this.mContext.getString(R.string.quick_settings_wifi_label);
        }

        @Override
        public Intent getSettingsIntent() {
            if (BenesseExtension.getDchaState() != 0) {
                return null;
            }
            return WifiTile.WIFI_SETTINGS;
        }

        @Override
        public Boolean getToggleState() {
            return Boolean.valueOf(((QSTile.SignalState) WifiTile.this.mState).value);
        }

        @Override
        public void setToggleState(boolean state) {
            if (WifiTile.DEBUG) {
                Log.d(WifiTile.this.TAG, "setToggleState " + state);
            }
            MetricsLogger.action(WifiTile.this.mContext, 153, state);
            WifiTile.this.mController.setWifiEnabled(state);
            WifiTile.this.showDetail(false);
        }

        @Override
        public int getMetricsCategory() {
            return 152;
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
            setItemsVisible(((QSTile.SignalState) WifiTile.this.mState).value);
            return this.mItems;
        }

        @Override
        public void onAccessPointsChanged(List<AccessPoint> accessPoints) {
            this.mAccessPoints = (AccessPoint[]) accessPoints.toArray(new AccessPoint[accessPoints.size()]);
            updateItems();
            if (accessPoints == null || accessPoints.size() <= 0) {
                return;
            }
            WifiTile.this.fireScanStateChanged(false);
        }

        @Override
        public void onSettingsActivityTriggered(Intent settingsIntent) {
            WifiTile.this.mHost.startActivityDismissingKeyguard(settingsIntent);
        }

        @Override
        public void onDetailItemClick(QSDetailItems.Item item) {
            if (item == null || item.tag == null) {
                return;
            }
            AccessPoint ap = (AccessPoint) item.tag;
            if (!ap.isActive() && WifiTile.this.mWifiController.connect(ap)) {
                WifiTile.this.mHost.collapsePanels();
            }
            WifiTile.this.showDetail(false);
        }

        @Override
        public void onDetailItemDisconnect(QSDetailItems.Item item) {
        }

        public void setItemsVisible(boolean visible) {
            if (this.mItems == null) {
                return;
            }
            this.mItems.setItemsVisible(visible);
        }

        private void updateItems() {
            if (this.mItems == null) {
                return;
            }
            QSDetailItems.Item[] items = null;
            if (this.mAccessPoints != null) {
                items = new QSDetailItems.Item[this.mAccessPoints.length];
                for (int i = 0; i < this.mAccessPoints.length; i++) {
                    AccessPoint ap = this.mAccessPoints[i];
                    QSDetailItems.Item item = new QSDetailItems.Item();
                    item.tag = ap;
                    item.icon = WifiTile.this.mWifiController.getIcon(ap);
                    item.line1 = ap.getSsid();
                    item.line2 = ap.isActive() ? ap.getSummary() : null;
                    item.overlay = ap.getSecurity() != 0 ? WifiTile.this.mContext.getDrawable(R.drawable.qs_ic_wifi_lock) : null;
                    items[i] = item;
                }
            }
            this.mItems.setItems(items);
        }
    }
}
