package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.qs.QSDetailItems;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

public class CastTile extends QSTile<QSTile.BooleanState> {
    private static final Intent CAST_SETTINGS = new Intent("android.settings.CAST_SETTINGS");
    private final Callback mCallback;
    private final CastController mController;
    private final CastDetailAdapter mDetailAdapter;
    private final KeyguardMonitor mKeyguard;

    public CastTile(QSTile.Host host) {
        super(host);
        this.mCallback = new Callback();
        this.mController = host.getCastController();
        this.mDetailAdapter = new CastDetailAdapter();
        this.mKeyguard = host.getKeyguardMonitor();
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
        if (this.mController != null) {
            if (DEBUG) {
                Log.d(this.TAG, "setListening " + listening);
            }
            if (listening) {
                this.mController.addCallback(this.mCallback);
                this.mKeyguard.addCallback(this.mCallback);
            } else {
                this.mController.setDiscovering(false);
                this.mController.removeCallback(this.mCallback);
                this.mKeyguard.removeCallback(this.mCallback);
            }
        }
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
        super.handleUserSwitch(newUserId);
        if (this.mController != null) {
            this.mController.setCurrentUserId(newUserId);
        }
    }

    @Override
    protected void handleClick() {
        showDetail(true);
    }

    @Override
    protected void handleUpdateState(QSTile.BooleanState state, Object arg) {
        state.visible = (this.mKeyguard.isSecure() && this.mKeyguard.isShowing()) ? false : true;
        state.label = this.mContext.getString(R.string.quick_settings_cast_title);
        state.value = false;
        state.autoMirrorDrawable = false;
        Set<CastController.CastDevice> devices = this.mController.getCastDevices();
        boolean connecting = false;
        for (CastController.CastDevice device : devices) {
            if (device.state == 2) {
                state.value = true;
                state.label = getDeviceName(device);
            } else if (device.state == 1) {
                connecting = true;
            }
        }
        if (!state.value && connecting) {
            state.label = this.mContext.getString(R.string.quick_settings_connecting);
        }
        state.icon = QSTile.ResourceIcon.get(state.value ? R.drawable.ic_qs_cast_on : R.drawable.ic_qs_cast_off);
        this.mDetailAdapter.updateItems(devices);
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return null;
        }
        return this.mContext.getString(R.string.accessibility_casting_turned_off);
    }

    private String getDeviceName(CastController.CastDevice device) {
        return device.name != null ? device.name : this.mContext.getString(R.string.quick_settings_cast_device_default_name);
    }

    private final class Callback implements CastController.Callback, KeyguardMonitor.Callback {
        private Callback() {
        }

        @Override
        public void onCastDevicesChanged() {
            CastTile.this.refreshState();
        }

        @Override
        public void onKeyguardChanged() {
            CastTile.this.refreshState();
        }
    }

    private final class CastDetailAdapter implements QSDetailItems.Callback, QSTile.DetailAdapter {
        private QSDetailItems mItems;
        private final LinkedHashMap<String, CastController.CastDevice> mVisibleOrder;

        private CastDetailAdapter() {
            this.mVisibleOrder = new LinkedHashMap<>();
        }

        @Override
        public int getTitle() {
            return R.string.quick_settings_cast_title;
        }

        @Override
        public Boolean getToggleState() {
            return null;
        }

        @Override
        public Intent getSettingsIntent() {
            return CastTile.CAST_SETTINGS;
        }

        @Override
        public void setToggleState(boolean state) {
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            this.mItems = QSDetailItems.convertOrInflate(context, convertView, parent);
            this.mItems.setTagSuffix("Cast");
            if (convertView == null) {
                if (CastTile.DEBUG) {
                    Log.d(CastTile.this.TAG, "addOnAttachStateChangeListener");
                }
                this.mItems.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View v) {
                        if (CastTile.DEBUG) {
                            Log.d(CastTile.this.TAG, "onViewAttachedToWindow");
                        }
                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        if (CastTile.DEBUG) {
                            Log.d(CastTile.this.TAG, "onViewDetachedFromWindow");
                        }
                        CastDetailAdapter.this.mVisibleOrder.clear();
                    }
                });
            }
            this.mItems.setEmptyState(R.drawable.ic_qs_cast_detail_empty, R.string.quick_settings_cast_detail_empty_text);
            this.mItems.setCallback(this);
            updateItems(CastTile.this.mController.getCastDevices());
            CastTile.this.mController.setDiscovering(true);
            return this.mItems;
        }

        private void updateItems(Set<CastController.CastDevice> devices) {
            if (this.mItems != null) {
                QSDetailItems.Item[] items = null;
                if (devices != null && !devices.isEmpty()) {
                    Iterator<CastController.CastDevice> it = devices.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        CastController.CastDevice device = it.next();
                        if (device.state == 2) {
                            QSDetailItems.Item item = new QSDetailItems.Item();
                            item.icon = R.drawable.ic_qs_cast_on;
                            item.line1 = CastTile.this.getDeviceName(device);
                            item.line2 = CastTile.this.mContext.getString(R.string.quick_settings_connected);
                            item.tag = device;
                            item.canDisconnect = true;
                            items = new QSDetailItems.Item[]{item};
                            break;
                        }
                    }
                    if (items == null) {
                        for (CastController.CastDevice device2 : devices) {
                            this.mVisibleOrder.put(device2.id, device2);
                        }
                        items = new QSDetailItems.Item[devices.size()];
                        int i = 0;
                        for (String id : this.mVisibleOrder.keySet()) {
                            CastController.CastDevice device3 = this.mVisibleOrder.get(id);
                            if (devices.contains(device3)) {
                                QSDetailItems.Item item2 = new QSDetailItems.Item();
                                item2.icon = R.drawable.ic_qs_cast_off;
                                item2.line1 = CastTile.this.getDeviceName(device3);
                                if (device3.state == 1) {
                                    item2.line2 = CastTile.this.mContext.getString(R.string.quick_settings_connecting);
                                }
                                item2.tag = device3;
                                items[i] = item2;
                                i++;
                            }
                        }
                    }
                }
                this.mItems.setItems(items);
            }
        }

        @Override
        public void onDetailItemClick(QSDetailItems.Item item) {
            if (item != null && item.tag != null) {
                CastController.CastDevice device = (CastController.CastDevice) item.tag;
                CastTile.this.mController.startCasting(device);
            }
        }

        @Override
        public void onDetailItemDisconnect(QSDetailItems.Item item) {
            if (item != null && item.tag != null) {
                CastController.CastDevice device = (CastController.CastDevice) item.tag;
                CastTile.this.mController.stopCasting(device);
            }
        }
    }
}
