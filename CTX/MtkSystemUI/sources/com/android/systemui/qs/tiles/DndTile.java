package com.android.systemui.qs.tiles;

import android.R;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.notification.EnableZenModeDialog;
import com.android.systemui.Dependency;
import com.android.systemui.Prefs;
import com.android.systemui.SysUIToast;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.volume.ZenModePanel;

public class DndTile extends QSTileImpl<QSTile.BooleanState> {
    private final ZenModeController mController;
    private final DndDetailAdapter mDetailAdapter;
    private boolean mListening;
    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefListener;
    private final BroadcastReceiver mReceiver;
    private boolean mReceiverRegistered;
    private boolean mShowingDetail;
    private final ZenModeController.Callback mZenCallback;
    private final ZenModePanel.Callback mZenModePanelCallback;
    private static final Intent ZEN_SETTINGS = new Intent("android.settings.ZEN_MODE_SETTINGS");
    private static final Intent ZEN_PRIORITY_SETTINGS = new Intent("android.settings.ZEN_MODE_PRIORITY_SETTINGS");

    public DndTile(QSHost qSHost) {
        super(qSHost);
        this.mPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
                if ("DndTileCombinedIcon".equals(str) || "DndTileVisible".equals(str)) {
                    DndTile.this.refreshState();
                }
            }
        };
        this.mZenCallback = new ZenModeController.Callback() {
            @Override
            public void onZenChanged(int i) {
                DndTile.this.refreshState(Integer.valueOf(i));
                if (!DndTile.this.isShowingDetail()) {
                    return;
                }
                DndTile.this.mDetailAdapter.updatePanel();
            }

            @Override
            public void onConfigChanged(ZenModeConfig zenModeConfig) {
                if (!DndTile.this.isShowingDetail()) {
                    return;
                }
                DndTile.this.mDetailAdapter.updatePanel();
            }
        };
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                DndTile.setVisible(DndTile.this.mContext, intent.getBooleanExtra("visible", false));
                DndTile.this.refreshState();
            }
        };
        this.mZenModePanelCallback = new ZenModePanel.Callback() {
            @Override
            public void onPrioritySettings() {
                ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(DndTile.ZEN_PRIORITY_SETTINGS, 0);
            }

            @Override
            public void onInteraction() {
            }

            @Override
            public void onExpanded(boolean z) {
            }
        };
        this.mController = (ZenModeController) Dependency.get(ZenModeController.class);
        this.mDetailAdapter = new DndDetailAdapter();
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter("com.android.systemui.dndtile.SET_VISIBLE"));
        this.mReceiverRegistered = true;
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        if (this.mReceiverRegistered) {
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mReceiverRegistered = false;
        }
    }

    public static void setVisible(Context context, boolean z) {
        Prefs.putBoolean(context, "DndTileVisible", z);
    }

    public static boolean isVisible(Context context) {
        return Prefs.getBoolean(context, "DndTileVisible", false);
    }

    public static void setCombinedIcon(Context context, boolean z) {
        Prefs.putBoolean(context, "DndTileCombinedIcon", z);
    }

    public static boolean isCombinedIcon(Context context) {
        return Prefs.getBoolean(context, "DndTileCombinedIcon", false);
    }

    @Override
    public DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
    }

    @Override
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    public Intent getLongClickIntent() {
        return ZEN_SETTINGS;
    }

    @Override
    protected void handleClick() {
        if (((QSTile.BooleanState) this.mState).value) {
            this.mController.setZen(0, null, this.TAG);
        } else {
            showDetail(true);
        }
    }

    @Override
    public void showDetail(boolean z) {
        int i = Settings.Global.getInt(this.mContext.getContentResolver(), "zen_duration", 0);
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "show_zen_upgrade_notification", 0) != 0) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "show_zen_upgrade_notification", 0);
            this.mController.setZen(1, null, this.TAG);
            Intent intent = new Intent("android.settings.ZEN_MODE_ONBOARDING");
            intent.addFlags(268468224);
            ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(intent, 0);
            return;
        }
        switch (i) {
            case -1:
                this.mUiHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        DndTile.lambda$showDetail$1(this.f$0);
                    }
                });
                break;
            case 0:
                this.mController.setZen(1, null, this.TAG);
                break;
            default:
                this.mController.setZen(1, ZenModeConfig.toTimeCondition(this.mContext, i, ActivityManager.getCurrentUser(), true).id, this.TAG);
                break;
        }
    }

    public static void lambda$showDetail$1(DndTile dndTile) {
        final Dialog dialogCreateDialog = new EnableZenModeDialog(dndTile.mContext).createDialog();
        dialogCreateDialog.getWindow().setType(2009);
        SystemUIDialog.setShowForAllUsers(dialogCreateDialog, true);
        SystemUIDialog.registerDismissListener(dialogCreateDialog);
        SystemUIDialog.setWindowOnTop(dialogCreateDialog);
        dndTile.mUiHandler.post(new Runnable() {
            @Override
            public final void run() {
                dialogCreateDialog.show();
            }
        });
        dndTile.mHost.collapsePanels();
    }

    @Override
    protected void handleSecondaryClick() {
        if (this.mController.isVolumeRestricted()) {
            this.mHost.collapsePanels();
            SysUIToast.makeText(this.mContext, this.mContext.getString(R.string.capability_title_canControlMagnification), 1).show();
        } else if (!((QSTile.BooleanState) this.mState).value) {
            this.mController.addCallback(new ZenModeController.Callback() {
                @Override
                public void onZenChanged(int i) {
                    DndTile.this.mController.removeCallback(this);
                    DndTile.this.showDetail(true);
                }
            });
            this.mController.setZen(1, null, this.TAG);
        } else {
            showDetail(true);
        }
    }

    @Override
    public CharSequence getTileLabel() {
        return this.mContext.getString(com.android.systemui.R.string.quick_settings_dnd_label);
    }

    @Override
    protected void handleUpdateState(QSTile.BooleanState booleanState, Object obj) {
        int iIntValue = obj instanceof Integer ? ((Integer) obj).intValue() : this.mController.getZen();
        boolean z = iIntValue != 0;
        boolean z2 = booleanState.value != z;
        if (booleanState.slash == null) {
            booleanState.slash = new QSTile.SlashState();
        }
        booleanState.dualTarget = true;
        booleanState.value = z;
        booleanState.state = booleanState.value ? 2 : 1;
        booleanState.slash.isSlashed = !booleanState.value;
        booleanState.label = getTileLabel();
        booleanState.secondaryLabel = TextUtils.emptyIfNull(ZenModeConfig.getDescription(this.mContext, iIntValue != 0, this.mController.getConfig(), false));
        booleanState.icon = QSTileImpl.ResourceIcon.get(com.android.systemui.R.drawable.ic_qs_dnd_on);
        checkIfRestrictionEnforcedByAdminOnly(booleanState, "no_adjust_volume");
        switch (iIntValue) {
            case 1:
                booleanState.contentDescription = this.mContext.getString(com.android.systemui.R.string.accessibility_quick_settings_dnd) + ", " + ((Object) booleanState.secondaryLabel);
                break;
            case 2:
                booleanState.contentDescription = this.mContext.getString(com.android.systemui.R.string.accessibility_quick_settings_dnd) + ", " + this.mContext.getString(com.android.systemui.R.string.accessibility_quick_settings_dnd_none_on) + ", " + ((Object) booleanState.secondaryLabel);
                break;
            case 3:
                booleanState.contentDescription = this.mContext.getString(com.android.systemui.R.string.accessibility_quick_settings_dnd) + ", " + this.mContext.getString(com.android.systemui.R.string.accessibility_quick_settings_dnd_alarms_on) + ", " + ((Object) booleanState.secondaryLabel);
                break;
            default:
                booleanState.contentDescription = this.mContext.getString(com.android.systemui.R.string.accessibility_quick_settings_dnd);
                break;
        }
        if (z2) {
            fireToggleStateChanged(booleanState.value);
        }
        booleanState.dualLabelContentDescription = this.mContext.getResources().getString(com.android.systemui.R.string.accessibility_quick_settings_open_settings, getTileLabel());
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
    }

    @Override
    public int getMetricsCategory() {
        return com.android.systemui.plugins.R.styleable.AppCompatTheme_windowMinWidthMajor;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(com.android.systemui.R.string.accessibility_quick_settings_dnd_changed_on);
        }
        return this.mContext.getString(com.android.systemui.R.string.accessibility_quick_settings_dnd_changed_off);
    }

    @Override
    public void handleSetListening(boolean z) {
        if (this.mListening == z) {
            return;
        }
        this.mListening = z;
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

    private final class DndDetailAdapter implements View.OnAttachStateChangeListener, DetailAdapter {
        private boolean mAuto;
        private ZenModePanel mZenPanel;

        private DndDetailAdapter() {
        }

        @Override
        public CharSequence getTitle() {
            return DndTile.this.mContext.getString(com.android.systemui.R.string.quick_settings_dnd_label);
        }

        @Override
        public Boolean getToggleState() {
            return Boolean.valueOf(((QSTile.BooleanState) DndTile.this.mState).value);
        }

        @Override
        public Intent getSettingsIntent() {
            return DndTile.ZEN_SETTINGS;
        }

        @Override
        public void setToggleState(boolean z) {
            MetricsLogger.action(DndTile.this.mContext, 166, z);
            if (!z) {
                DndTile.this.mController.setZen(0, null, DndTile.this.TAG);
                this.mAuto = false;
            } else {
                DndTile.this.mController.setZen(1, null, DndTile.this.TAG);
            }
        }

        @Override
        public int getMetricsCategory() {
            return 149;
        }

        @Override
        public View createDetailView(Context context, View view, ViewGroup viewGroup) {
            this.mZenPanel = view != null ? (ZenModePanel) view : (ZenModePanel) LayoutInflater.from(context).inflate(com.android.systemui.R.layout.zen_mode_panel, viewGroup, false);
            if (view == null) {
                this.mZenPanel.init(DndTile.this.mController);
                this.mZenPanel.addOnAttachStateChangeListener(this);
                this.mZenPanel.setCallback(DndTile.this.mZenModePanelCallback);
                this.mZenPanel.setEmptyState(com.android.systemui.R.drawable.ic_qs_dnd_detail_empty, com.android.systemui.R.string.dnd_is_off);
            }
            updatePanel();
            return this.mZenPanel;
        }

        private void updatePanel() {
            if (this.mZenPanel == null) {
                return;
            }
            this.mAuto = false;
            if (DndTile.this.mController.getZen() != 0) {
                ZenModeConfig config = DndTile.this.mController.getConfig();
                String string = "";
                if (config.manualRule != null && config.manualRule.enabler != null) {
                    string = getOwnerCaption(config.manualRule.enabler);
                }
                for (ZenModeConfig.ZenRule zenRule : config.automaticRules.values()) {
                    if (zenRule.isAutomaticActive()) {
                        string = string.isEmpty() ? DndTile.this.mContext.getString(com.android.systemui.R.string.qs_dnd_prompt_auto_rule, zenRule.name) : DndTile.this.mContext.getString(com.android.systemui.R.string.qs_dnd_prompt_auto_rule_app);
                    }
                }
                if (string.isEmpty()) {
                    this.mZenPanel.setState(0);
                    return;
                }
                this.mAuto = true;
                this.mZenPanel.setState(1);
                this.mZenPanel.setAutoText(string);
                return;
            }
            this.mZenPanel.setState(2);
        }

        private String getOwnerCaption(String str) {
            CharSequence charSequenceLoadLabel;
            PackageManager packageManager = DndTile.this.mContext.getPackageManager();
            try {
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(str, 0);
                return (applicationInfo == null || (charSequenceLoadLabel = applicationInfo.loadLabel(packageManager)) == null) ? "" : DndTile.this.mContext.getString(com.android.systemui.R.string.qs_dnd_prompt_app, charSequenceLoadLabel.toString().trim());
            } catch (Throwable th) {
                Slog.w(DndTile.this.TAG, "Error loading owner caption", th);
                return "";
            }
        }

        @Override
        public void onViewAttachedToWindow(View view) {
            DndTile.this.mShowingDetail = true;
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
            DndTile.this.mShowingDetail = false;
            this.mZenPanel = null;
        }
    }
}
