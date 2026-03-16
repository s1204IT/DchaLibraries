package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.qs.QSDetailItems;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.BluetoothController;
import java.util.Set;

public class BluetoothTile extends QSTile<QSTile.BooleanState> {
    private static final Intent BLUETOOTH_SETTINGS = new Intent("android.settings.BLUETOOTH_SETTINGS");
    private final BluetoothController.Callback mCallback;
    private final BluetoothController mController;
    private final BluetoothDetailAdapter mDetailAdapter;

    public BluetoothTile(QSTile.Host host) {
        super(host);
        this.mCallback = new BluetoothController.Callback() {
            @Override
            public void onBluetoothStateChange(boolean enabled, boolean connecting) {
                BluetoothTile.this.refreshState();
            }

            @Override
            public void onBluetoothPairedDevicesChanged() {
                BluetoothTile.this.mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        BluetoothTile.this.mDetailAdapter.updateItems();
                    }
                });
                BluetoothTile.this.refreshState();
            }
        };
        this.mController = host.getBluetoothController();
        this.mDetailAdapter = new BluetoothDetailAdapter();
    }

    @Override
    public boolean supportsDualTargets() {
        return true;
    }

    @Override
    public QSTile.DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
    }

    @Override
    protected QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            this.mController.addStateChangedCallback(this.mCallback);
        } else {
            this.mController.removeStateChangedCallback(this.mCallback);
        }
    }

    @Override
    protected void handleClick() {
        boolean isEnabled = Boolean.valueOf(((QSTile.BooleanState) this.mState).value).booleanValue();
        this.mController.setBluetoothEnabled(!isEnabled);
    }

    @Override
    protected void handleSecondaryClick() {
        if (!((QSTile.BooleanState) this.mState).value) {
            ((QSTile.BooleanState) this.mState).value = true;
            this.mController.setBluetoothEnabled(true);
        }
        showDetail(true);
    }

    @Override
    protected void handleUpdateState(QSTile.BooleanState state, Object arg) {
        boolean supported = this.mController.isBluetoothSupported();
        boolean enabled = this.mController.isBluetoothEnabled();
        boolean connected = this.mController.isBluetoothConnected();
        boolean connecting = this.mController.isBluetoothConnecting();
        state.visible = supported;
        state.value = enabled;
        state.autoMirrorDrawable = false;
        if (enabled) {
            state.label = null;
            if (connected) {
                state.icon = QSTile.ResourceIcon.get(R.drawable.ic_qs_bluetooth_connected);
                state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_connected);
                state.label = this.mController.getLastDeviceName();
            } else if (connecting) {
                state.icon = QSTile.ResourceIcon.get(R.drawable.ic_qs_bluetooth_connecting);
                state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_connecting);
                state.label = this.mContext.getString(R.string.quick_settings_bluetooth_label);
            } else {
                state.icon = QSTile.ResourceIcon.get(R.drawable.ic_qs_bluetooth_on);
                state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_on);
            }
            if (TextUtils.isEmpty(state.label)) {
                state.label = this.mContext.getString(R.string.quick_settings_bluetooth_label);
            }
        } else {
            state.icon = QSTile.ResourceIcon.get(R.drawable.ic_qs_bluetooth_off);
            state.label = this.mContext.getString(R.string.quick_settings_bluetooth_label);
            state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_off);
        }
        String bluetoothName = state.label;
        if (connected) {
            bluetoothName = this.mContext.getString(R.string.accessibility_bluetooth_name, state.label);
            state.dualLabelContentDescription = bluetoothName;
        }
        state.dualLabelContentDescription = bluetoothName;
    }

    @Override
    protected String composeChangeAnnouncement() {
        return ((QSTile.BooleanState) this.mState).value ? this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_changed_on) : this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_changed_off);
    }

    private final class BluetoothDetailAdapter implements QSDetailItems.Callback, QSTile.DetailAdapter {
        private QSDetailItems mItems;

        private BluetoothDetailAdapter() {
        }

        @Override
        public int getTitle() {
            return R.string.quick_settings_bluetooth_label;
        }

        @Override
        public Boolean getToggleState() {
            return Boolean.valueOf(((QSTile.BooleanState) BluetoothTile.this.mState).value);
        }

        @Override
        public Intent getSettingsIntent() {
            return BluetoothTile.BLUETOOTH_SETTINGS;
        }

        @Override
        public void setToggleState(boolean state) {
            BluetoothTile.this.mController.setBluetoothEnabled(state);
            BluetoothTile.this.showDetail(false);
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            this.mItems = QSDetailItems.convertOrInflate(context, convertView, parent);
            this.mItems.setTagSuffix("Bluetooth");
            this.mItems.setEmptyState(R.drawable.ic_qs_bluetooth_detail_empty, R.string.quick_settings_bluetooth_detail_empty_text);
            this.mItems.setCallback(this);
            this.mItems.setMinHeightInItems(0);
            updateItems();
            setItemsVisible(((QSTile.BooleanState) BluetoothTile.this.mState).value);
            return this.mItems;
        }

        public void setItemsVisible(boolean visible) {
            if (this.mItems != null) {
                this.mItems.setItemsVisible(visible);
            }
        }

        private void updateItems() {
            if (this.mItems != null) {
                QSDetailItems.Item[] items = null;
                Set<BluetoothController.PairedDevice> devices = BluetoothTile.this.mController.getPairedDevices();
                if (devices != null) {
                    items = new QSDetailItems.Item[devices.size()];
                    int i = 0;
                    for (BluetoothController.PairedDevice device : devices) {
                        QSDetailItems.Item item = new QSDetailItems.Item();
                        item.icon = R.drawable.ic_qs_bluetooth_on;
                        item.line1 = device.name;
                        if (device.state == BluetoothController.PairedDevice.STATE_CONNECTED) {
                            item.icon = R.drawable.ic_qs_bluetooth_connected;
                            item.line2 = BluetoothTile.this.mContext.getString(R.string.quick_settings_connected);
                            item.canDisconnect = true;
                        } else if (device.state == BluetoothController.PairedDevice.STATE_CONNECTING) {
                            item.icon = R.drawable.ic_qs_bluetooth_connecting;
                            item.line2 = BluetoothTile.this.mContext.getString(R.string.quick_settings_connecting);
                        }
                        item.tag = device;
                        items[i] = item;
                        i++;
                    }
                }
                this.mItems.setItems(items);
            }
        }

        @Override
        public void onDetailItemClick(QSDetailItems.Item item) {
            BluetoothController.PairedDevice device;
            if (item != null && item.tag != null && (device = (BluetoothController.PairedDevice) item.tag) != null && device.state == BluetoothController.PairedDevice.STATE_DISCONNECTED) {
                BluetoothTile.this.mController.connect(device);
            }
        }

        @Override
        public void onDetailItemDisconnect(QSDetailItems.Item item) {
            BluetoothController.PairedDevice device;
            if (item != null && item.tag != null && (device = (BluetoothController.PairedDevice) item.tag) != null) {
                BluetoothTile.this.mController.disconnect(device);
            }
        }
    }
}
