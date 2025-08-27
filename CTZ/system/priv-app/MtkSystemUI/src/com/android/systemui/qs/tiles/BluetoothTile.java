package com.android.systemui.qs.tiles;

import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.Utils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.graph.BluetoothDeviceLayerDrawable;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSDetailItems;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.BluetoothController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/* loaded from: classes.dex */
public class BluetoothTile extends QSTileImpl<QSTile.BooleanState> {
    private static final Intent BLUETOOTH_SETTINGS = new Intent("android.settings.BLUETOOTH_SETTINGS");
    private final ActivityStarter mActivityStarter;
    private final BluetoothController.Callback mCallback;
    private final BluetoothController mController;
    private final BluetoothDetailAdapter mDetailAdapter;

    public BluetoothTile(QSHost qSHost) {
        super(qSHost);
        this.mCallback = new BluetoothController.Callback() { // from class: com.android.systemui.qs.tiles.BluetoothTile.1
            @Override // com.android.systemui.statusbar.policy.BluetoothController.Callback
            public void onBluetoothStateChange(boolean z) {
                BluetoothTile.this.refreshState();
                if (!BluetoothTile.this.isShowingDetail()) {
                    return;
                }
                BluetoothTile.this.mDetailAdapter.updateItems();
                BluetoothTile.this.fireToggleStateChanged(BluetoothTile.this.mDetailAdapter.getToggleState().booleanValue());
            }

            @Override // com.android.systemui.statusbar.policy.BluetoothController.Callback
            public void onBluetoothDevicesChanged() {
                BluetoothTile.this.refreshState();
                if (!BluetoothTile.this.isShowingDetail()) {
                    return;
                }
                BluetoothTile.this.mDetailAdapter.updateItems();
            }
        };
        this.mController = (BluetoothController) Dependency.get(BluetoothController.class);
        this.mActivityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
        this.mDetailAdapter = (BluetoothDetailAdapter) createDetailAdapter();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
    }

    /* JADX DEBUG: Method merged with bridge method: newTileState()Lcom/android/systemui/plugins/qs/QSTile$State; */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleSetListening(boolean z) {
        if (z) {
            this.mController.addCallback(this.mCallback);
        } else {
            this.mController.removeCallback(this.mCallback);
        }
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleClick() {
        boolean z = ((QSTile.BooleanState) this.mState).value;
        refreshState(z ? null : ARG_SHOW_TRANSIENT_ENABLING);
        this.mController.setBluetoothEnabled(!z);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public Intent getLongClickIntent() {
        return new Intent("android.settings.BLUETOOTH_SETTINGS");
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleSecondaryClick() {
        if (!this.mController.canConfigBluetooth()) {
            this.mActivityStarter.postStartActivityDismissingKeyguard(new Intent("android.settings.BLUETOOTH_SETTINGS"), 0);
            return;
        }
        showDetail(true);
        if (!((QSTile.BooleanState) this.mState).value) {
            this.mController.setBluetoothEnabled(true);
        }
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_bluetooth_label);
    }

    /* JADX DEBUG: Method merged with bridge method: handleUpdateState(Lcom/android/systemui/plugins/qs/QSTile$State;Ljava/lang/Object;)V */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleUpdateState(QSTile.BooleanState booleanState, Object obj) {
        boolean z = obj == ARG_SHOW_TRANSIENT_ENABLING;
        boolean z2 = z || this.mController.isBluetoothEnabled();
        boolean zIsBluetoothConnected = this.mController.isBluetoothConnected();
        boolean zIsBluetoothConnecting = this.mController.isBluetoothConnecting();
        booleanState.isTransient = z || zIsBluetoothConnecting || this.mController.getBluetoothState() == 11;
        booleanState.dualTarget = true;
        booleanState.value = z2;
        if (booleanState.slash == null) {
            booleanState.slash = new QSTile.SlashState();
        }
        booleanState.slash.isSlashed = !z2;
        booleanState.label = this.mContext.getString(R.string.quick_settings_bluetooth_label);
        booleanState.secondaryLabel = TextUtils.emptyIfNull(getSecondaryLabel(z2, zIsBluetoothConnecting, zIsBluetoothConnected, booleanState.isTransient));
        if (z2) {
            if (zIsBluetoothConnected) {
                booleanState.icon = new BluetoothConnectedTileIcon();
                if (!TextUtils.isEmpty(this.mController.getConnectedDeviceName())) {
                    booleanState.label = this.mController.getConnectedDeviceName();
                }
                booleanState.contentDescription = this.mContext.getString(R.string.accessibility_bluetooth_name, booleanState.label) + ", " + ((Object) booleanState.secondaryLabel);
            } else if (booleanState.isTransient) {
                booleanState.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_bluetooth_transient_animation);
                booleanState.contentDescription = booleanState.secondaryLabel;
            } else {
                booleanState.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_bluetooth_on);
                booleanState.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_bluetooth) + "," + this.mContext.getString(R.string.accessibility_not_connected);
            }
            booleanState.state = 2;
        } else {
            booleanState.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_bluetooth_on);
            booleanState.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_bluetooth);
            booleanState.state = 1;
        }
        booleanState.dualLabelContentDescription = this.mContext.getResources().getString(R.string.accessibility_quick_settings_open_settings, getTileLabel());
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
    }

    private String getSecondaryLabel(boolean z, boolean z2, boolean z3, boolean z4) {
        if (z2) {
            return this.mContext.getString(R.string.quick_settings_connecting);
        }
        if (z4) {
            return this.mContext.getString(R.string.quick_settings_bluetooth_secondary_label_transient);
        }
        List<CachedBluetoothDevice> connectedDevices = this.mController.getConnectedDevices();
        if (z && z3 && !connectedDevices.isEmpty()) {
            if (connectedDevices.size() > 1) {
                return this.mContext.getResources().getQuantityString(R.plurals.quick_settings_hotspot_secondary_label_num_devices, connectedDevices.size(), Integer.valueOf(connectedDevices.size()));
            }
            CachedBluetoothDevice cachedBluetoothDevice = connectedDevices.get(0);
            int batteryLevel = cachedBluetoothDevice.getBatteryLevel();
            if (batteryLevel != -1) {
                return this.mContext.getString(R.string.quick_settings_bluetooth_secondary_label_battery_level, Utils.formatPercentage(batteryLevel));
            }
            BluetoothClass btClass = cachedBluetoothDevice.getBtClass();
            if (btClass != null) {
                if (btClass.doesClassMatch(1)) {
                    return this.mContext.getString(R.string.quick_settings_bluetooth_secondary_label_audio);
                }
                if (btClass.doesClassMatch(0)) {
                    return this.mContext.getString(R.string.quick_settings_bluetooth_secondary_label_headset);
                }
                if (btClass.doesClassMatch(3)) {
                    return this.mContext.getString(R.string.quick_settings_bluetooth_secondary_label_input);
                }
                return null;
            }
            return null;
        }
        return null;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public int getMetricsCategory() {
        return com.android.systemui.plugins.R.styleable.AppCompatTheme_windowActionModeOverlay;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_changed_off);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public boolean isAvailable() {
        return this.mController.isBluetoothSupported();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected DetailAdapter createDetailAdapter() {
        return new BluetoothDetailAdapter();
    }

    private class BluetoothBatteryTileIcon extends QSTile.Icon {
        private int mBatteryLevel;
        private float mIconScale;

        BluetoothBatteryTileIcon(int i, float f) {
            this.mBatteryLevel = i;
            this.mIconScale = f;
        }

        @Override // com.android.systemui.plugins.qs.QSTile.Icon
        public Drawable getDrawable(Context context) {
            return BluetoothDeviceLayerDrawable.createLayerDrawable(context, R.drawable.ic_qs_bluetooth_connected, this.mBatteryLevel, this.mIconScale);
        }
    }

    private class BluetoothConnectedTileIcon extends QSTile.Icon {
        BluetoothConnectedTileIcon() {
        }

        @Override // com.android.systemui.plugins.qs.QSTile.Icon
        public Drawable getDrawable(Context context) {
            return context.getDrawable(R.drawable.ic_qs_bluetooth_connected);
        }
    }

    protected class BluetoothDetailAdapter implements DetailAdapter, QSDetailItems.Callback {
        private QSDetailItems mItems;

        protected BluetoothDetailAdapter() {
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public CharSequence getTitle() {
            return BluetoothTile.this.mContext.getString(R.string.quick_settings_bluetooth_label);
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public Boolean getToggleState() {
            return Boolean.valueOf(((QSTile.BooleanState) BluetoothTile.this.mState).value);
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public boolean getToggleEnabled() {
            return BluetoothTile.this.mController.getBluetoothState() == 10 || BluetoothTile.this.mController.getBluetoothState() == 12;
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public Intent getSettingsIntent() {
            return BluetoothTile.BLUETOOTH_SETTINGS;
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public void setToggleState(boolean z) {
            MetricsLogger.action(BluetoothTile.this.mContext, 154, z);
            BluetoothTile.this.mController.setBluetoothEnabled(z);
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public int getMetricsCategory() {
            return 150;
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public View createDetailView(Context context, View view, ViewGroup viewGroup) {
            this.mItems = QSDetailItems.convertOrInflate(context, view, viewGroup);
            this.mItems.setTagSuffix("Bluetooth");
            this.mItems.setCallback(this);
            updateItems();
            setItemsVisible(((QSTile.BooleanState) BluetoothTile.this.mState).value);
            return this.mItems;
        }

        public void setItemsVisible(boolean z) {
            if (this.mItems == null) {
                return;
            }
            this.mItems.setItemsVisible(z);
        }

        private void updateItems() {
            if (this.mItems == null) {
                return;
            }
            if (BluetoothTile.this.mController.isBluetoothEnabled()) {
                this.mItems.setEmptyState(R.drawable.ic_qs_bluetooth_detail_empty, R.string.quick_settings_bluetooth_detail_empty_text);
            } else {
                this.mItems.setEmptyState(R.drawable.ic_qs_bluetooth_detail_empty, R.string.bt_is_off);
            }
            ArrayList arrayList = new ArrayList();
            Collection<CachedBluetoothDevice> devices = BluetoothTile.this.mController.getDevices();
            if (devices != null) {
                int i = 0;
                int i2 = 0;
                for (CachedBluetoothDevice cachedBluetoothDevice : devices) {
                    if (BluetoothTile.this.mController.getBondState(cachedBluetoothDevice) != 10) {
                        QSDetailItems.Item item = new QSDetailItems.Item();
                        item.iconResId = R.drawable.ic_qs_bluetooth_on;
                        item.line1 = cachedBluetoothDevice.getName();
                        item.tag = cachedBluetoothDevice;
                        int maxConnectionState = cachedBluetoothDevice.getMaxConnectionState();
                        if (maxConnectionState == 2) {
                            item.iconResId = R.drawable.ic_qs_bluetooth_connected;
                            int batteryLevel = cachedBluetoothDevice.getBatteryLevel();
                            if (batteryLevel == -1) {
                                item.line2 = BluetoothTile.this.mContext.getString(R.string.quick_settings_connected);
                            } else {
                                item.icon = BluetoothTile.this.new BluetoothBatteryTileIcon(batteryLevel, 1.0f);
                                item.line2 = BluetoothTile.this.mContext.getString(R.string.quick_settings_connected_battery_level, Utils.formatPercentage(batteryLevel));
                            }
                            item.canDisconnect = true;
                            arrayList.add(i, item);
                            i++;
                        } else if (maxConnectionState == 1) {
                            item.iconResId = R.drawable.ic_qs_bluetooth_connecting;
                            item.line2 = BluetoothTile.this.mContext.getString(R.string.quick_settings_connecting);
                            arrayList.add(i, item);
                        } else {
                            arrayList.add(item);
                        }
                        i2++;
                        if (i2 == 20) {
                            break;
                        }
                    }
                }
            }
            this.mItems.setItems((QSDetailItems.Item[]) arrayList.toArray(new QSDetailItems.Item[arrayList.size()]));
        }

        @Override // com.android.systemui.qs.QSDetailItems.Callback
        public void onDetailItemClick(QSDetailItems.Item item) {
            CachedBluetoothDevice cachedBluetoothDevice;
            if (item != null && item.tag != null && (cachedBluetoothDevice = (CachedBluetoothDevice) item.tag) != null && cachedBluetoothDevice.getMaxConnectionState() == 0) {
                BluetoothTile.this.mController.connect(cachedBluetoothDevice);
            }
        }

        @Override // com.android.systemui.qs.QSDetailItems.Callback
        public void onDetailItemDisconnect(QSDetailItems.Item item) {
            CachedBluetoothDevice cachedBluetoothDevice;
            if (item != null && item.tag != null && (cachedBluetoothDevice = (CachedBluetoothDevice) item.tag) != null) {
                BluetoothTile.this.mController.disconnect(cachedBluetoothDevice);
            }
        }
    }
}
