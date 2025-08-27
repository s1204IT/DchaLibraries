package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.wifi.AccessPoint;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.plugins.qs.QSIconView;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.AlphaControlledSignalTileView;
import com.android.systemui.qs.QSDetailItems;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.NetworkController;
import java.util.List;

/* loaded from: classes.dex */
public class WifiTile extends QSTileImpl<QSTile.SignalState> {
    private static final Intent WIFI_SETTINGS = new Intent("android.settings.WIFI_SETTINGS");
    private final ActivityStarter mActivityStarter;
    protected final NetworkController mController;
    private final WifiDetailAdapter mDetailAdapter;
    private boolean mExpectDisabled;
    protected final WifiSignalCallback mSignalCallback;
    private final QSTile.SignalState mStateBeforeClick;
    private final NetworkController.AccessPointController mWifiController;

    public WifiTile(QSHost qSHost) {
        super(qSHost);
        this.mStateBeforeClick = newTileState();
        this.mSignalCallback = new WifiSignalCallback();
        this.mController = (NetworkController) Dependency.get(NetworkController.class);
        this.mWifiController = this.mController.getAccessPointController();
        this.mDetailAdapter = (WifiDetailAdapter) createDetailAdapter();
        this.mActivityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
    }

    /* JADX DEBUG: Method merged with bridge method: newTileState()Lcom/android/systemui/plugins/qs/QSTile$State; */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public QSTile.SignalState newTileState() {
        return new QSTile.SignalState();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleSetListening(boolean z) {
        if (z) {
            this.mController.addCallback((NetworkController.SignalCallback) this.mSignalCallback);
        } else {
            this.mController.removeCallback((NetworkController.SignalCallback) this.mSignalCallback);
        }
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public void setDetailListening(boolean z) {
        if (z) {
            this.mWifiController.addAccessPointCallback(this.mDetailAdapter);
        } else {
            this.mWifiController.removeAccessPointCallback(this.mDetailAdapter);
        }
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected DetailAdapter createDetailAdapter() {
        return new WifiDetailAdapter();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public QSIconView createTileView(Context context) {
        return new AlphaControlledSignalTileView(context);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public Intent getLongClickIntent() {
        return WIFI_SETTINGS;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleClick() {
        ((QSTile.SignalState) this.mState).copyTo(this.mStateBeforeClick);
        boolean z = ((QSTile.SignalState) this.mState).value;
        refreshState(z ? null : ARG_SHOW_TRANSIENT_ENABLING);
        this.mController.setWifiEnabled(!z);
        this.mExpectDisabled = z;
        if (this.mExpectDisabled) {
            this.mHandler.postDelayed(new Runnable() { // from class: com.android.systemui.qs.tiles.-$$Lambda$WifiTile$FBMX-zj483F7uFPAUwutmnquiRU
                @Override // java.lang.Runnable
                public final void run() {
                    WifiTile.lambda$handleClick$0(this.f$0);
                }
            }, 350L);
        }
    }

    public static /* synthetic */ void lambda$handleClick$0(WifiTile wifiTile) {
        if (wifiTile.mExpectDisabled) {
            wifiTile.mExpectDisabled = false;
            wifiTile.refreshState();
        }
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleSecondaryClick() {
        if (!this.mWifiController.canConfigWifi()) {
            this.mActivityStarter.postStartActivityDismissingKeyguard(new Intent("android.settings.WIFI_SETTINGS"), 0);
            return;
        }
        showDetail(true);
        if (!((QSTile.SignalState) this.mState).value) {
            this.mController.setWifiEnabled(true);
        }
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_wifi_label);
    }

    /* JADX DEBUG: Method merged with bridge method: handleUpdateState(Lcom/android/systemui/plugins/qs/QSTile$State;Ljava/lang/Object;)V */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleUpdateState(QSTile.SignalState signalState, Object obj) {
        if (DEBUG) {
            Log.d(this.TAG, "handleUpdateState arg=" + obj);
        }
        CallbackInfo callbackInfo = this.mSignalCallback.mInfo;
        if (this.mExpectDisabled) {
            if (callbackInfo.enabled) {
                return;
            } else {
                this.mExpectDisabled = false;
            }
        }
        boolean z = obj == ARG_SHOW_TRANSIENT_ENABLING;
        boolean z2 = callbackInfo.enabled && callbackInfo.wifiSignalIconId > 0 && callbackInfo.ssid != null;
        boolean z3 = callbackInfo.wifiSignalIconId > 0 && callbackInfo.ssid == null;
        if (signalState.value != callbackInfo.enabled) {
            this.mDetailAdapter.setItemsVisible(callbackInfo.enabled);
            fireToggleStateChanged(callbackInfo.enabled);
        }
        if (signalState.slash == null) {
            signalState.slash = new QSTile.SlashState();
            signalState.slash.rotation = 6.0f;
        }
        signalState.slash.isSlashed = false;
        boolean z4 = z || callbackInfo.isTransient;
        signalState.secondaryLabel = getSecondaryLabel(z4, callbackInfo.statusLabel);
        signalState.state = 2;
        signalState.dualTarget = true;
        signalState.value = z || callbackInfo.enabled;
        signalState.activityIn = callbackInfo.enabled && callbackInfo.activityIn;
        signalState.activityOut = callbackInfo.enabled && callbackInfo.activityOut;
        StringBuffer stringBuffer = new StringBuffer();
        Resources resources = this.mContext.getResources();
        if (z4) {
            signalState.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_signal_wifi_transient_animation);
            signalState.label = resources.getString(R.string.quick_settings_wifi_label);
        } else if (!signalState.value) {
            signalState.slash.isSlashed = true;
            signalState.state = 1;
            signalState.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_wifi_disabled);
            signalState.label = resources.getString(R.string.quick_settings_wifi_label);
        } else if (z2) {
            signalState.icon = QSTileImpl.ResourceIcon.get(callbackInfo.wifiSignalIconId);
            signalState.label = removeDoubleQuotes(callbackInfo.ssid);
        } else if (z3) {
            signalState.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_wifi_disconnected);
            signalState.label = resources.getString(R.string.quick_settings_wifi_label);
        } else {
            signalState.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_wifi_no_network);
            signalState.label = resources.getString(R.string.quick_settings_wifi_label);
        }
        stringBuffer.append(this.mContext.getString(R.string.quick_settings_wifi_label));
        stringBuffer.append(",");
        if (signalState.value && z2) {
            stringBuffer.append(callbackInfo.wifiSignalContentDescription);
            stringBuffer.append(",");
            stringBuffer.append(removeDoubleQuotes(callbackInfo.ssid));
        }
        signalState.contentDescription = stringBuffer.toString();
        signalState.dualLabelContentDescription = resources.getString(R.string.accessibility_quick_settings_open_settings, getTileLabel());
        signalState.expandedAccessibilityClassName = Switch.class.getName();
    }

    private CharSequence getSecondaryLabel(boolean z, String str) {
        if (!z) {
            return str;
        }
        return this.mContext.getString(R.string.quick_settings_wifi_secondary_label_transient);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public int getMetricsCategory() {
        return 126;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected boolean shouldAnnouncementBeDelayed() {
        return this.mStateBeforeClick.value == ((QSTile.SignalState) this.mState).value;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected String composeChangeAnnouncement() {
        if (((QSTile.SignalState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_wifi_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_wifi_changed_off);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public boolean isAvailable() {
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi");
    }

    private static String removeDoubleQuotes(String str) {
        if (str == null) {
            return null;
        }
        int length = str.length();
        if (length > 1 && str.charAt(0) == '\"') {
            int i = length - 1;
            if (str.charAt(i) == '\"') {
                return str.substring(1, i);
            }
        }
        return str;
    }

    protected static final class CallbackInfo {
        boolean activityIn;
        boolean activityOut;
        boolean connected;
        boolean enabled;
        boolean isTransient;
        String ssid;
        public String statusLabel;
        String wifiSignalContentDescription;
        int wifiSignalIconId;

        protected CallbackInfo() {
        }

        public String toString() {
            return "CallbackInfo[enabled=" + this.enabled + ",connected=" + this.connected + ",wifiSignalIconId=" + this.wifiSignalIconId + ",ssid=" + this.ssid + ",activityIn=" + this.activityIn + ",activityOut=" + this.activityOut + ",wifiSignalContentDescription=" + this.wifiSignalContentDescription + ",isTransient=" + this.isTransient + ']';
        }
    }

    protected final class WifiSignalCallback implements NetworkController.SignalCallback {
        final CallbackInfo mInfo = new CallbackInfo();

        protected WifiSignalCallback() {
        }

        @Override // com.android.systemui.statusbar.policy.NetworkController.SignalCallback
        public void setWifiIndicators(boolean z, NetworkController.IconState iconState, NetworkController.IconState iconState2, boolean z2, boolean z3, String str, boolean z4, String str2) {
            if (WifiTile.DEBUG) {
                Log.d(WifiTile.this.TAG, "onWifiSignalChanged enabled=" + z);
            }
            this.mInfo.enabled = z;
            this.mInfo.connected = iconState2.visible;
            this.mInfo.wifiSignalIconId = iconState2.icon;
            this.mInfo.ssid = str;
            this.mInfo.activityIn = z2;
            this.mInfo.activityOut = z3;
            this.mInfo.wifiSignalContentDescription = iconState2.contentDescription;
            this.mInfo.isTransient = z4;
            this.mInfo.statusLabel = str2;
            if (WifiTile.this.isShowingDetail()) {
                WifiTile.this.mDetailAdapter.updateItems();
            }
            WifiTile.this.refreshState();
        }
    }

    protected class WifiDetailAdapter implements DetailAdapter, QSDetailItems.Callback, NetworkController.AccessPointController.AccessPointCallback {
        private AccessPoint[] mAccessPoints;
        private QSDetailItems mItems;

        protected WifiDetailAdapter() {
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public CharSequence getTitle() {
            return WifiTile.this.mContext.getString(R.string.quick_settings_wifi_label);
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public Intent getSettingsIntent() {
            return WifiTile.WIFI_SETTINGS;
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public Boolean getToggleState() {
            return Boolean.valueOf(((QSTile.SignalState) WifiTile.this.mState).value);
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public void setToggleState(boolean z) {
            if (WifiTile.DEBUG) {
                Log.d(WifiTile.this.TAG, "setToggleState " + z);
            }
            MetricsLogger.action(WifiTile.this.mContext, 153, z);
            WifiTile.this.mController.setWifiEnabled(z);
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public int getMetricsCategory() {
            return 152;
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public View createDetailView(Context context, View view, ViewGroup viewGroup) {
            if (WifiTile.DEBUG) {
                String str = WifiTile.this.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("createDetailView convertView=");
                sb.append(view != null);
                Log.d(str, sb.toString());
            }
            this.mAccessPoints = null;
            this.mItems = QSDetailItems.convertOrInflate(context, view, viewGroup);
            this.mItems.setTagSuffix("Wifi");
            this.mItems.setCallback(this);
            WifiTile.this.mWifiController.scanForAccessPoints();
            setItemsVisible(((QSTile.SignalState) WifiTile.this.mState).value);
            return this.mItems;
        }

        @Override // com.android.systemui.statusbar.policy.NetworkController.AccessPointController.AccessPointCallback
        public void onAccessPointsChanged(List<AccessPoint> list) {
            this.mAccessPoints = (AccessPoint[]) list.toArray(new AccessPoint[list.size()]);
            filterUnreachableAPs();
            updateItems();
        }

        private void filterUnreachableAPs() {
            int i = 0;
            for (AccessPoint accessPoint : this.mAccessPoints) {
                if (accessPoint.isReachable()) {
                    i++;
                }
            }
            if (i != this.mAccessPoints.length) {
                AccessPoint[] accessPointArr = this.mAccessPoints;
                this.mAccessPoints = new AccessPoint[i];
                int i2 = 0;
                for (AccessPoint accessPoint2 : accessPointArr) {
                    if (accessPoint2.isReachable()) {
                        this.mAccessPoints[i2] = accessPoint2;
                        i2++;
                    }
                }
            }
        }

        @Override // com.android.systemui.statusbar.policy.NetworkController.AccessPointController.AccessPointCallback
        public void onSettingsActivityTriggered(Intent intent) {
            WifiTile.this.mActivityStarter.postStartActivityDismissingKeyguard(intent, 0);
        }

        @Override // com.android.systemui.qs.QSDetailItems.Callback
        public void onDetailItemClick(QSDetailItems.Item item) {
            if (item == null || item.tag == null) {
                return;
            }
            AccessPoint accessPoint = (AccessPoint) item.tag;
            if (!accessPoint.isActive() && WifiTile.this.mWifiController.connect(accessPoint)) {
                WifiTile.this.mHost.collapsePanels();
            }
            WifiTile.this.showDetail(false);
        }

        @Override // com.android.systemui.qs.QSDetailItems.Callback
        public void onDetailItemDisconnect(QSDetailItems.Item item) {
        }

        public void setItemsVisible(boolean z) {
            if (this.mItems == null) {
                return;
            }
            this.mItems.setItemsVisible(z);
        }

        private void updateItems() {
            QSDetailItems.Item[] itemArr;
            int i;
            if (this.mItems == null) {
                return;
            }
            if ((this.mAccessPoints != null && this.mAccessPoints.length > 0) || !WifiTile.this.mSignalCallback.mInfo.enabled) {
                WifiTile.this.fireScanStateChanged(false);
            } else {
                WifiTile.this.fireScanStateChanged(true);
            }
            if (!WifiTile.this.mSignalCallback.mInfo.enabled) {
                this.mItems.setEmptyState(R.drawable.ic_qs_wifi_detail_empty, R.string.wifi_is_off);
                this.mItems.setItems(null);
                return;
            }
            this.mItems.setEmptyState(R.drawable.ic_qs_wifi_detail_empty, R.string.quick_settings_wifi_detail_empty_text);
            if (this.mAccessPoints != null) {
                itemArr = new QSDetailItems.Item[this.mAccessPoints.length];
                for (int i2 = 0; i2 < this.mAccessPoints.length; i2++) {
                    AccessPoint accessPoint = this.mAccessPoints[i2];
                    QSDetailItems.Item item = new QSDetailItems.Item();
                    item.tag = accessPoint;
                    item.iconResId = WifiTile.this.mWifiController.getIcon(accessPoint);
                    item.line1 = accessPoint.getSsid();
                    item.line2 = accessPoint.isActive() ? accessPoint.getSummary() : null;
                    if (accessPoint.getSecurity() != 0) {
                        i = R.drawable.qs_ic_wifi_lock;
                    } else {
                        i = -1;
                    }
                    item.icon2 = i;
                    itemArr[i2] = item;
                }
            } else {
                itemArr = null;
            }
            this.mItems.setItems(itemArr);
        }
    }
}
