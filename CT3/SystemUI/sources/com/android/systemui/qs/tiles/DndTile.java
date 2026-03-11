package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BenesseExtension;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.SysUIToast;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.volume.ZenModePanel;

public class DndTile extends QSTile<QSTile.BooleanState> {
    private final ZenModeController mController;
    private final DndDetailAdapter mDetailAdapter;
    private final QSTile<QSTile.BooleanState>.AnimationIcon mDisable;
    private final QSTile<QSTile.BooleanState>.AnimationIcon mDisableTotalSilence;
    private boolean mListening;
    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefListener;
    private final BroadcastReceiver mReceiver;
    private boolean mShowingDetail;
    private final ZenModeController.Callback mZenCallback;
    private final ZenModePanel.Callback mZenModePanelCallback;
    private static final Intent ZEN_SETTINGS = new Intent("android.settings.ZEN_MODE_SETTINGS");
    private static final Intent ZEN_PRIORITY_SETTINGS = new Intent("android.settings.ZEN_MODE_PRIORITY_SETTINGS");
    private static final QSTile.Icon TOTAL_SILENCE = QSTile.ResourceIcon.get(R.drawable.ic_qs_dnd_on_total_silence);

    public DndTile(QSTile.Host host) {
        super(host);
        this.mDisable = new QSTile.AnimationIcon(R.drawable.ic_dnd_disable_animation, R.drawable.ic_qs_dnd_off);
        this.mDisableTotalSilence = new QSTile.AnimationIcon(R.drawable.ic_dnd_total_silence_disable_animation, R.drawable.ic_qs_dnd_off);
        this.mPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (!"DndTileCombinedIcon".equals(key) && !"DndTileVisible".equals(key)) {
                    return;
                }
                DndTile.this.refreshState();
            }
        };
        this.mZenCallback = new ZenModeController.Callback() {
            @Override
            public void onZenChanged(int zen) {
                DndTile.this.refreshState(Integer.valueOf(zen));
            }
        };
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean visible = intent.getBooleanExtra("visible", false);
                DndTile.setVisible(DndTile.this.mContext, visible);
                DndTile.this.refreshState();
            }
        };
        this.mZenModePanelCallback = new ZenModePanel.Callback() {
            @Override
            public void onPrioritySettings() {
                if (BenesseExtension.getDchaState() != 0) {
                    return;
                }
                DndTile.this.mHost.startActivityDismissingKeyguard(DndTile.ZEN_PRIORITY_SETTINGS);
            }

            @Override
            public void onInteraction() {
            }

            @Override
            public void onExpanded(boolean expanded) {
            }
        };
        this.mController = host.getZenModeController();
        this.mDetailAdapter = new DndDetailAdapter(this, null);
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter("com.android.systemui.dndtile.SET_VISIBLE"));
    }

    public static void setVisible(Context context, boolean visible) {
        Prefs.putBoolean(context, "DndTileVisible", visible);
    }

    public static boolean isVisible(Context context) {
        return Prefs.getBoolean(context, "DndTileVisible", false);
    }

    public static void setCombinedIcon(Context context, boolean combined) {
        Prefs.putBoolean(context, "DndTileCombinedIcon", combined);
    }

    public static boolean isCombinedIcon(Context context) {
        return Prefs.getBoolean(context, "DndTileCombinedIcon", false);
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
    public Intent getLongClickIntent() {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        return ZEN_SETTINGS;
    }

    @Override
    public void handleClick() {
        if (this.mController.isVolumeRestricted()) {
            this.mHost.collapsePanels();
            SysUIToast.makeText(this.mContext, this.mContext.getString(android.R.string.js_dialog_before_unload_positive_button), 1).show();
            return;
        }
        MetricsLogger.action(this.mContext, getMetricsCategory(), !((QSTile.BooleanState) this.mState).value);
        if (((QSTile.BooleanState) this.mState).value) {
            this.mController.setZen(0, null, this.TAG);
            return;
        }
        int zen = Prefs.getInt(this.mContext, "DndFavoriteZen", 3);
        this.mController.setZen(zen, null, this.TAG);
        showDetail(true);
    }

    @Override
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_dnd_label);
    }

    @Override
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        int zen = arg instanceof Integer ? ((Integer) arg).intValue() : this.mController.getZen();
        boolean newValue = zen != 0;
        boolean valueChanged = state.value != newValue;
        state.value = newValue;
        checkIfRestrictionEnforcedByAdminOnly(state, "no_adjust_volume");
        switch (zen) {
            case 1:
                state.icon = QSTile.ResourceIcon.get(R.drawable.ic_qs_dnd_on);
                state.label = this.mContext.getString(R.string.quick_settings_dnd_priority_label);
                state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_dnd_priority_on);
                break;
            case 2:
                state.icon = TOTAL_SILENCE;
                state.label = this.mContext.getString(R.string.quick_settings_dnd_none_label);
                state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_dnd_none_on);
                break;
            case 3:
                state.icon = QSTile.ResourceIcon.get(R.drawable.ic_qs_dnd_on);
                state.label = this.mContext.getString(R.string.quick_settings_dnd_alarms_label);
                state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_dnd_alarms_on);
                break;
            default:
                state.icon = TOTAL_SILENCE.equals(state.icon) ? this.mDisableTotalSilence : this.mDisable;
                state.label = this.mContext.getString(R.string.quick_settings_dnd_label);
                state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_dnd);
                break;
        }
        if (this.mShowingDetail && !state.value) {
            showDetail(false);
        }
        if (valueChanged) {
            fireToggleStateChanged(state.value);
        }
        String name = Switch.class.getName();
        state.expandedAccessibilityClassName = name;
        state.minimalAccessibilityClassName = name;
    }

    @Override
    public int getMetricsCategory() {
        return 118;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_dnd_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_dnd_changed_off);
    }

    @Override
    public void setListening(boolean listening) {
        if (this.mListening == listening) {
            return;
        }
        this.mListening = listening;
        if (this.mListening) {
            this.mController.addCallback(this.mZenCallback);
            Prefs.registerListener(this.mContext, this.mPrefListener);
        } else {
            this.mController.removeCallback(this.mZenCallback);
            Prefs.unregisterListener(this.mContext, this.mPrefListener);
        }
    }

    @Override
    public boolean isAvailable() {
        return isVisible(this.mContext);
    }

    private final class DndDetailAdapter implements QSTile.DetailAdapter, View.OnAttachStateChangeListener {
        DndDetailAdapter(DndTile this$0, DndDetailAdapter dndDetailAdapter) {
            this();
        }

        private DndDetailAdapter() {
        }

        @Override
        public CharSequence getTitle() {
            return DndTile.this.mContext.getString(R.string.quick_settings_dnd_label);
        }

        @Override
        public Boolean getToggleState() {
            return Boolean.valueOf(((QSTile.BooleanState) DndTile.this.mState).value);
        }

        @Override
        public Intent getSettingsIntent() {
            if (BenesseExtension.getDchaState() != 0) {
                return null;
            }
            return DndTile.ZEN_SETTINGS;
        }

        @Override
        public void setToggleState(boolean state) {
            MetricsLogger.action(DndTile.this.mContext, 166, state);
            if (state) {
                return;
            }
            DndTile.this.mController.setZen(0, null, DndTile.this.TAG);
            DndTile.this.showDetail(false);
        }

        @Override
        public int getMetricsCategory() {
            return 149;
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            ZenModePanel zmp = convertView != null ? (ZenModePanel) convertView : (ZenModePanel) LayoutInflater.from(context).inflate(R.layout.zen_mode_panel, parent, false);
            if (convertView == null) {
                zmp.init(DndTile.this.mController);
                zmp.addOnAttachStateChangeListener(this);
                zmp.setCallback(DndTile.this.mZenModePanelCallback);
            }
            return zmp;
        }

        @Override
        public void onViewAttachedToWindow(View v) {
            DndTile.this.mShowingDetail = true;
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            DndTile.this.mShowingDetail = false;
        }
    }
}
