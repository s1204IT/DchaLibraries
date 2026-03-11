package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.WifiDisplayStatus;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.BenesseExtension;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
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
    private static final Intent WFD_SINK_SETTINGS = new Intent("mediatek.settings.WFD_SINK_SETTINGS");
    private final Callback mCallback;
    private final CastController mController;
    private final CastDetailAdapter mDetailAdapter;
    private final KeyguardMonitor mKeyguard;

    public CastTile(QSTile.Host host) {
        super(host);
        this.mCallback = new Callback(this, null);
        this.mController = host.getCastController();
        this.mDetailAdapter = new CastDetailAdapter(this, 0 == true ? 1 : 0);
        this.mKeyguard = host.getKeyguardMonitor();
        this.mController.setListening(true);
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
        if (this.mController == null) {
            return;
        }
        Log.d(this.TAG, "setListening " + listening);
        if (listening) {
            this.mController.addCallback(this.mCallback);
            this.mKeyguard.addCallback(this.mCallback);
        } else {
            this.mController.setDiscovering(false);
            this.mController.removeCallback(this.mCallback);
            this.mKeyguard.removeCallback(this.mCallback);
        }
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        if (this.mController == null) {
            return;
        }
        Log.d(this.TAG, "handle destroy");
        this.mController.setListening(false);
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
        super.handleUserSwitch(newUserId);
        if (this.mController == null) {
            return;
        }
        this.mController.setCurrentUserId(newUserId);
    }

    @Override
    public Intent getLongClickIntent() {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        return new Intent("android.settings.CAST_SETTINGS");
    }

    @Override
    protected void handleClick() {
        if (this.mKeyguard.isSecure() && !this.mKeyguard.canSkipBouncer()) {
            this.mHost.startRunnableDismissingKeyguard(new Runnable() {
                @Override
                public void run() {
                    MetricsLogger.action(CastTile.this.mContext, CastTile.this.getMetricsCategory());
                    CastTile.this.showDetail(true);
                    CastTile.this.mHost.openPanels();
                }
            });
        } else {
            MetricsLogger.action(this.mContext, getMetricsCategory());
            showDetail(true);
        }
    }

    @Override
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_cast_title);
    }

    @Override
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        state.label = this.mContext.getString(R.string.quick_settings_cast_title);
        state.contentDescription = state.label;
        state.value = false;
        state.autoMirrorDrawable = false;
        Set<CastController.CastDevice> devices = this.mController.getCastDevices();
        boolean connecting = false;
        for (CastController.CastDevice device : devices) {
            if (device.state == 2) {
                state.value = true;
                state.label = getDeviceName(device);
                state.contentDescription += "," + this.mContext.getString(R.string.accessibility_cast_name, state.label);
            } else if (device.state == 1) {
                connecting = true;
            }
        }
        if (!state.value && connecting) {
            state.label = this.mContext.getString(R.string.quick_settings_connecting);
        }
        state.icon = QSTile.ResourceIcon.get(state.value ? R.drawable.ic_qs_cast_on : R.drawable.ic_qs_cast_off);
        this.mDetailAdapter.updateItems(devices);
        String name = Button.class.getName();
        state.expandedAccessibilityClassName = name;
        state.minimalAccessibilityClassName = name;
        state.contentDescription += "," + this.mContext.getString(R.string.accessibility_quick_settings_open_details);
        this.mDetailAdapter.updateSinkView();
    }

    @Override
    public int getMetricsCategory() {
        return 114;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (!((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_casting_turned_off);
        }
        return null;
    }

    @Override
    public boolean isAvailable() {
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.direct");
    }

    public String getDeviceName(CastController.CastDevice device) {
        return device.name != null ? device.name : this.mContext.getString(R.string.quick_settings_cast_device_default_name);
    }

    private final class Callback implements CastController.Callback, KeyguardMonitor.Callback {
        Callback(CastTile this$0, Callback callback) {
            this();
        }

        private Callback() {
        }

        @Override
        public void onCastDevicesChanged() {
            Log.d(CastTile.this.TAG, "onCastDevicesChanged");
            CastTile.this.refreshState();
        }

        @Override
        public void onWfdStatusChanged(WifiDisplayStatus status, boolean sinkMode) {
            Log.d(CastTile.this.TAG, "onWfdStatusChanged: " + status.getActiveDisplayState());
            CastTile.this.mDetailAdapter.wfdStatusChanged(status, sinkMode);
            CastTile.this.refreshState();
        }

        @Override
        public void onWifiP2pDeviceChanged(WifiP2pDevice device) {
            Log.d(CastTile.this.TAG, "onWifiP2pDeviceChanged");
            CastTile.this.mDetailAdapter.updateDeviceName(device);
        }

        @Override
        public void onKeyguardChanged() {
            Log.d(CastTile.this.TAG, "onKeyguardChanged");
            CastTile.this.refreshState();
        }
    }

    private final class CastDetailAdapter implements QSTile.DetailAdapter, QSDetailItems.Callback {
        private LinearLayout mDetailView;
        private QSDetailItems mItems;
        private boolean mSinkViewEnabledBak;
        private final LinkedHashMap<String, CastController.CastDevice> mVisibleOrder;
        private View mWfdSinkView;

        CastDetailAdapter(CastTile this$0, CastDetailAdapter castDetailAdapter) {
            this();
        }

        private CastDetailAdapter() {
            this.mVisibleOrder = new LinkedHashMap<>();
            this.mSinkViewEnabledBak = true;
        }

        @Override
        public CharSequence getTitle() {
            return CastTile.this.mContext.getString(R.string.quick_settings_cast_title);
        }

        @Override
        public Boolean getToggleState() {
            return null;
        }

        @Override
        public Intent getSettingsIntent() {
            if (BenesseExtension.getDchaState() != 0) {
                return null;
            }
            return CastTile.CAST_SETTINGS;
        }

        @Override
        public void setToggleState(boolean state) {
        }

        @Override
        public int getMetricsCategory() {
            return 151;
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            if (CastTile.this.mController.isWfdSinkSupported()) {
                this.mItems = QSDetailItems.convertOrInflate(context, this.mItems, parent);
            } else {
                this.mItems = QSDetailItems.convertOrInflate(context, convertView, parent);
            }
            this.mItems.setTagSuffix("Cast");
            if (convertView == null) {
                Log.d(CastTile.this.TAG, "addOnAttachStateChangeListener");
                this.mItems.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View v) {
                        Log.d(CastTile.this.TAG, "onViewAttachedToWindow");
                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        Log.d(CastTile.this.TAG, "onViewDetachedFromWindow");
                        CastDetailAdapter.this.mVisibleOrder.clear();
                    }
                });
            }
            this.mItems.setEmptyState(R.drawable.ic_qs_cast_detail_empty, R.string.quick_settings_cast_detail_empty_text);
            this.mItems.setCallback(this);
            updateItems(CastTile.this.mController.getCastDevices());
            CastTile.this.mController.setDiscovering(true);
            if (CastTile.this.mController.isWfdSinkSupported()) {
                Log.d(CastTile.this.TAG, "add WFD sink view: " + (this.mWfdSinkView == null));
                if (this.mWfdSinkView == null) {
                    LayoutInflater layoutInflater = LayoutInflater.from(context);
                    this.mWfdSinkView = layoutInflater.inflate(R.layout.qs_wfd_prefrence_material, parent, false);
                    ViewGroup widgetFrame = (ViewGroup) this.mWfdSinkView.findViewById(android.R.id.widget_frame);
                    layoutInflater.inflate(R.layout.qs_wfd_widget_switch, widgetFrame);
                    ImageView view = (ImageView) this.mWfdSinkView.findViewById(android.R.id.icon);
                    if (context.getResources().getBoolean(android.R.^attr-private.frameDuration)) {
                        view.setImageResource(R.drawable.ic_wfd_cellphone);
                    } else {
                        view.setImageResource(R.drawable.ic_wfd_laptop);
                    }
                    TextView summary = (TextView) this.mWfdSinkView.findViewById(android.R.id.summary);
                    summary.setText(R.string.wfd_sink_summary);
                    this.mWfdSinkView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Switch swi = (Switch) v.findViewById(android.R.id.checkbox);
                            boolean checked = swi.isChecked();
                            if (!checked) {
                                CastTile.this.getHost().startActivityDismissingKeyguard(CastTile.WFD_SINK_SETTINGS);
                            }
                            swi.setChecked(!checked);
                        }
                    });
                }
                if (convertView instanceof LinearLayout) {
                    this.mDetailView = (LinearLayout) convertView;
                    updateSinkView();
                } else {
                    this.mDetailView = new LinearLayout(context);
                    this.mDetailView.setOrientation(1);
                    ViewGroup sinkViewParent = (ViewGroup) this.mWfdSinkView.getParent();
                    if (sinkViewParent != null) {
                        Log.d(CastTile.this.TAG, "mWfdSinkView needs remove from parent: " + sinkViewParent.toString());
                        sinkViewParent.removeView(this.mWfdSinkView);
                    }
                    ViewGroup itemParent = (ViewGroup) this.mItems.getParent();
                    if (itemParent != null) {
                        Log.d(CastTile.this.TAG, "mItems needs remove from parent: " + itemParent.toString());
                        itemParent.removeView(this.mItems);
                    }
                    this.mDetailView.addView(this.mWfdSinkView);
                    View devider = new View(context);
                    int dh = context.getResources().getDimensionPixelSize(R.dimen.qs_tile_divider_height);
                    devider.setLayoutParams(new ViewGroup.LayoutParams(-1, dh));
                    devider.setBackgroundColor(context.getResources().getColor(R.color.qs_tile_divider));
                    this.mDetailView.addView(devider);
                    this.mDetailView.addView(this.mItems);
                }
                updateDeviceName(CastTile.this.mController.getWifiP2pDev());
                setSinkViewVisible(CastTile.this.mController.isNeedShowWfdSink());
                setSinkViewEnabled(this.mSinkViewEnabledBak);
            }
            if (this.mDetailView != null) {
                return this.mDetailView;
            }
            return this.mItems;
        }

        public void updateItems(Set<CastController.CastDevice> devices) {
            Log.d(CastTile.this.TAG, "update items: " + devices.size());
            if (this.mItems == null) {
                return;
            }
            QSDetailItems.Item[] items = null;
            if (devices != null && !devices.isEmpty()) {
                Iterator device$iterator = devices.iterator();
                while (true) {
                    if (!device$iterator.hasNext()) {
                        break;
                    }
                    CastController.CastDevice device = (CastController.CastDevice) device$iterator.next();
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

        @Override
        public void onDetailItemClick(QSDetailItems.Item item) {
            if (item == null || item.tag == null) {
                return;
            }
            MetricsLogger.action(CastTile.this.mContext, 157);
            CastController.CastDevice device = (CastController.CastDevice) item.tag;
            Log.d(CastTile.this.TAG, "onDetailItemClick: " + device.name);
            CastTile.this.mController.startCasting(device);
            CastTile.this.mController.updateWfdFloatMenu(true);
        }

        @Override
        public void onDetailItemDisconnect(QSDetailItems.Item item) {
            if (item == null || item.tag == null) {
                return;
            }
            MetricsLogger.action(CastTile.this.mContext, 158);
            CastController.CastDevice device = (CastController.CastDevice) item.tag;
            Log.d(CastTile.this.TAG, "onDetailItemDisconnect: " + device.name);
            CastTile.this.mController.stopCasting(device);
            CastTile.this.mController.updateWfdFloatMenu(false);
        }

        public void wfdStatusChanged(WifiDisplayStatus status, boolean sinkMode) {
            boolean show = CastTile.this.mController.isNeedShowWfdSink();
            setSinkViewVisible(show);
            handleWfdStateChanged(show ? status.getActiveDisplayState() : 0, sinkMode);
        }

        private void handleWfdStateChanged(int wfdState, boolean sinkMode) {
            switch (wfdState) {
                case 0:
                    if (!sinkMode) {
                        setSinkViewEnabled(true);
                        setSinkViewChecked(false);
                        CastTile.this.mController.updateWfdFloatMenu(false);
                    }
                    break;
                case 1:
                    if (!sinkMode) {
                        setSinkViewEnabled(false);
                    }
                    break;
                case 2:
                    if (!sinkMode) {
                        setSinkViewEnabled(false);
                    }
                    break;
            }
        }

        public void updateDeviceName(WifiP2pDevice device) {
            if (device == null || this.mWfdSinkView == null) {
                return;
            }
            Log.d(CastTile.this.TAG, "updateDeviceName: " + device.deviceName);
            TextView textView = (TextView) this.mWfdSinkView.findViewById(android.R.id.title);
            if (TextUtils.isEmpty(device.deviceName)) {
                textView.setText(device.deviceAddress);
            } else {
                textView.setText(device.deviceName);
            }
        }

        private void setSinkViewVisible(boolean visible) {
            if (this.mWfdSinkView == null) {
                return;
            }
            Log.d(CastTile.this.TAG, "setSinkViewVisible: " + visible);
            if (visible) {
                if (this.mWfdSinkView.getVisibility() == 0) {
                    return;
                }
                updateDeviceName(CastTile.this.mController.getWifiP2pDev());
                this.mWfdSinkView.setVisibility(0);
                return;
            }
            this.mWfdSinkView.setVisibility(8);
        }

        private void setSinkViewEnabled(boolean enabled) {
            this.mSinkViewEnabledBak = enabled;
            if (this.mWfdSinkView == null) {
                return;
            }
            Log.d(CastTile.this.TAG, "setSinkViewEnabled: " + enabled);
            setEnabledStateOnViews(this.mWfdSinkView, enabled);
        }

        private void setEnabledStateOnViews(View v, boolean enabled) {
            v.setEnabled(enabled);
            if (!(v instanceof ViewGroup)) {
                return;
            }
            ViewGroup vg = (ViewGroup) v;
            for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                setEnabledStateOnViews(vg.getChildAt(i), enabled);
            }
        }

        private void setSinkViewChecked(boolean checked) {
            if (this.mWfdSinkView == null) {
                return;
            }
            Log.d(CastTile.this.TAG, "setSinkViewChecked: " + checked);
            Switch swi = (Switch) this.mWfdSinkView.findViewById(android.R.id.checkbox);
            swi.setChecked(checked);
        }

        public void updateSinkView() {
            if (this.mWfdSinkView == null) {
                return;
            }
            Log.d(CastTile.this.TAG, "updateSinkView summary");
            final TextView summary = (TextView) this.mWfdSinkView.findViewById(android.R.id.summary);
            summary.post(new Runnable() {
                @Override
                public void run() {
                    summary.setText(R.string.wfd_sink_summary);
                }
            });
        }
    }
}
