package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.os.BenesseExtension;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.systemui.R;
import com.android.systemui.qs.QSDetailItems;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.BluetoothController;
import java.util.ArrayList;
import java.util.Collection;

public class BluetoothTile extends QSTile<QSTile.BooleanState> {
    private static final Intent BLUETOOTH_SETTINGS = new Intent("android.settings.BLUETOOTH_SETTINGS");
    private final BluetoothController.Callback mCallback;
    private final BluetoothController mController;
    private final BluetoothDetailAdapter mDetailAdapter;

    public BluetoothTile(QSTile.Host host) {
        super(host);
        this.mCallback = new BluetoothController.Callback() {
            @Override
            public void onBluetoothStateChange(boolean enabled) {
                BluetoothTile.this.refreshState();
            }

            @Override
            public void onBluetoothDevicesChanged() {
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
        this.mDetailAdapter = new BluetoothDetailAdapter(this, null);
    }

    @Override
    public QSTile.DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
    }

    @Override
    public QSTile.BooleanState newTileState() {
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
    protected void handleSecondaryClick() {
        boolean isEnabled = Boolean.valueOf(((QSTile.BooleanState) this.mState).value).booleanValue();
        MetricsLogger.action(this.mContext, getMetricsCategory(), !isEnabled);
        this.mController.setBluetoothEnabled(isEnabled ? false : true);
    }

    @Override
    public Intent getLongClickIntent() {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        return new Intent("android.settings.BLUETOOTH_SETTINGS");
    }

    @Override
    protected void handleClick() {
        if (!this.mController.canConfigBluetooth() && BenesseExtension.getDchaState() == 0) {
            this.mHost.startActivityDismissingKeyguard(new Intent("android.settings.BLUETOOTH_SETTINGS"));
            return;
        }
        if (!((QSTile.BooleanState) this.mState).value) {
            ((QSTile.BooleanState) this.mState).value = true;
            this.mController.setBluetoothEnabled(true);
        }
        showDetail(true);
    }

    @Override
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_bluetooth_label);
    }

    @Override
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        boolean enabled = this.mController.isBluetoothEnabled();
        boolean connected = this.mController.isBluetoothConnected();
        boolean connecting = this.mController.isBluetoothConnecting();
        state.value = enabled;
        state.autoMirrorDrawable = false;
        state.minimalContentDescription = this.mContext.getString(R.string.accessibility_quick_settings_bluetooth);
        if (enabled) {
            state.label = null;
            if (connected) {
                state.icon = QSTile.ResourceIcon.get(R.drawable.ic_qs_bluetooth_connected);
                state.label = this.mController.getLastDeviceName();
                state.contentDescription = this.mContext.getString(R.string.accessibility_bluetooth_name, state.label);
                state.minimalContentDescription += "," + state.contentDescription;
            } else if (connecting) {
                state.icon = QSTile.ResourceIcon.get(R.drawable.ic_qs_bluetooth_connecting);
                state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_connecting);
                state.label = this.mContext.getString(R.string.quick_settings_bluetooth_label);
                state.minimalContentDescription += "," + state.contentDescription;
            } else {
                state.icon = QSTile.ResourceIcon.get(R.drawable.ic_qs_bluetooth_on);
                state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_on) + "," + this.mContext.getString(R.string.accessibility_not_connected);
                state.minimalContentDescription += "," + this.mContext.getString(R.string.accessibility_not_connected);
            }
            if (TextUtils.isEmpty(state.label)) {
                state.label = this.mContext.getString(R.string.quick_settings_bluetooth_label);
            }
        } else {
            state.icon = QSTile.ResourceIcon.get(R.drawable.ic_qs_bluetooth_off);
            state.label = this.mContext.getString(R.string.quick_settings_bluetooth_label);
            state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_off);
        }
        CharSequence bluetoothName = state.label;
        if (connected) {
            bluetoothName = this.mContext.getString(R.string.accessibility_bluetooth_name, state.label);
            state.dualLabelContentDescription = bluetoothName;
        }
        state.dualLabelContentDescription = bluetoothName;
        state.contentDescription += "," + this.mContext.getString(R.string.accessibility_quick_settings_open_settings, getTileLabel());
        state.expandedAccessibilityClassName = Button.class.getName();
        state.minimalAccessibilityClassName = Switch.class.getName();
    }

    @Override
    public int getMetricsCategory() {
        return 113;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_changed_off);
    }

    @Override
    public boolean isAvailable() {
        return this.mController.isBluetoothSupported();
    }

    private final class BluetoothDetailAdapter implements QSTile.DetailAdapter, QSDetailItems.Callback {
        private QSDetailItems mItems;

        BluetoothDetailAdapter(BluetoothTile this$0, BluetoothDetailAdapter bluetoothDetailAdapter) {
            this();
        }

        private BluetoothDetailAdapter() {
        }

        @Override
        public CharSequence getTitle() {
            return BluetoothTile.this.mContext.getString(R.string.quick_settings_bluetooth_label);
        }

        @Override
        public Boolean getToggleState() {
            return Boolean.valueOf(((QSTile.BooleanState) BluetoothTile.this.mState).value);
        }

        @Override
        public Intent getSettingsIntent() {
            if (BenesseExtension.getDchaState() != 0) {
                return null;
            }
            return BluetoothTile.BLUETOOTH_SETTINGS;
        }

        @Override
        public void setToggleState(boolean state) {
            MetricsLogger.action(BluetoothTile.this.mContext, 154, state);
            BluetoothTile.this.mController.setBluetoothEnabled(state);
            BluetoothTile.this.showDetail(false);
        }

        @Override
        public int getMetricsCategory() {
            return 150;
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            this.mItems = QSDetailItems.convertOrInflate(context, convertView, parent);
            this.mItems.setTagSuffix("Bluetooth");
            this.mItems.setEmptyState(R.drawable.ic_qs_bluetooth_detail_empty, R.string.quick_settings_bluetooth_detail_empty_text);
            this.mItems.setCallback(this);
            updateItems();
            setItemsVisible(((QSTile.BooleanState) BluetoothTile.this.mState).value);
            return this.mItems;
        }

        public void setItemsVisible(boolean visible) {
            if (this.mItems == null) {
                return;
            }
            this.mItems.setItemsVisible(visible);
        }

        public void updateItems() {
            if (this.mItems == null) {
                return;
            }
            ArrayList<QSDetailItems.Item> items = new ArrayList<>();
            Collection<CachedBluetoothDevice> devices = BluetoothTile.this.mController.getDevices();
            if (devices != null) {
                for (CachedBluetoothDevice device : devices) {
                    if (device.getBondState() != 10) {
                        QSDetailItems.Item item = new QSDetailItems.Item();
                        item.icon = R.drawable.ic_qs_bluetooth_on;
                        item.line1 = device.getName();
                        int state = device.getMaxConnectionState();
                        if (state == 2) {
                            item.icon = R.drawable.ic_qs_bluetooth_connected;
                            item.line2 = BluetoothTile.this.mContext.getString(R.string.quick_settings_connected);
                            item.canDisconnect = true;
                        } else if (state == 1) {
                            item.icon = R.drawable.ic_qs_bluetooth_connecting;
                            item.line2 = BluetoothTile.this.mContext.getString(R.string.quick_settings_connecting);
                        }
                        item.tag = device;
                        items.add(item);
                    }
                }
            }
            this.mItems.setItems((QSDetailItems.Item[]) items.toArray(new QSDetailItems.Item[items.size()]));
        }

        @Override
        public void onDetailItemClick(QSDetailItems.Item item) {
            CachedBluetoothDevice device;
            if (item == null || item.tag == null || (device = (CachedBluetoothDevice) item.tag) == null || device.getMaxConnectionState() != 0) {
                return;
            }
            BluetoothTile.this.mController.connect(device);
        }

        @Override
        public void onDetailItemDisconnect(QSDetailItems.Item item) {
            CachedBluetoothDevice device;
            if (item == null || item.tag == null || (device = (CachedBluetoothDevice) item.tag) == null) {
                return;
            }
            BluetoothTile.this.mController.disconnect(device);
        }
    }
}
