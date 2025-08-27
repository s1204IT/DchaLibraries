package com.android.systemui.qs.tiles;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.net.DataUsageController;
import com.android.systemui.Dependency;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.plugins.qs.QSIconView;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.CellTileView;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.SignalTileView;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.NetworkController;
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import com.mediatek.systemui.ext.OpSystemUICustomizationFactoryBase;

/* loaded from: classes.dex */
public class CellularTile extends QSTileImpl<QSTile.SignalState> {
    private final ActivityStarter mActivityStarter;
    private final NetworkController mController;
    private final DataUsageController mDataController;
    private final CellularDetailAdapter mDetailAdapter;
    private boolean mDisplayDataUsage;
    private QSTile.Icon mIcon;
    private final KeyguardMonitor mKeyguardMonitor;
    private IQuickSettingsPlugin mQuickSettingsPlugin;
    private final CellSignalCallback mSignalCallback;

    public CellularTile(QSHost qSHost) {
        super(qSHost);
        this.mSignalCallback = new CellSignalCallback();
        this.mController = (NetworkController) Dependency.get(NetworkController.class);
        this.mActivityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
        this.mKeyguardMonitor = (KeyguardMonitor) Dependency.get(KeyguardMonitor.class);
        this.mDataController = this.mController.getMobileDataController();
        this.mDetailAdapter = new CellularDetailAdapter();
        this.mQuickSettingsPlugin = OpSystemUICustomizationFactoryBase.getOpFactory(this.mContext).makeQuickSettings(this.mContext);
        this.mDisplayDataUsage = this.mQuickSettingsPlugin.customizeDisplayDataUsage(false);
        this.mIcon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_data_usage);
    }

    /* JADX DEBUG: Method merged with bridge method: newTileState()Lcom/android/systemui/plugins/qs/QSTile$State; */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public QSTile.SignalState newTileState() {
        return new QSTile.SignalState();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
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
    public QSIconView createTileView(Context context) {
        if (this.mDisplayDataUsage) {
            return new SignalTileView(context);
        }
        return new CellTileView(context);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public Intent getLongClickIntent() {
        return getCellularSettingIntent();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleClick() {
        if (getState().state == 0) {
            return;
        }
        if (this.mDataController.isMobileDataEnabled()) {
            if (this.mKeyguardMonitor.isSecure() && !this.mKeyguardMonitor.canSkipBouncer()) {
                this.mActivityStarter.postQSRunnableDismissingKeyguard(new Runnable() { // from class: com.android.systemui.qs.tiles.-$$Lambda$CellularTile$e5lJLa1KQT80zJILmE-4gKT4_rA
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.maybeShowDisableDialog();
                    }
                });
                return;
            } else {
                this.mUiHandler.post(new Runnable() { // from class: com.android.systemui.qs.tiles.-$$Lambda$CellularTile$e5lJLa1KQT80zJILmE-4gKT4_rA
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.maybeShowDisableDialog();
                    }
                });
                return;
            }
        }
        this.mDataController.setMobileDataEnabled(true);
        this.mQuickSettingsPlugin.disableDataForOtherSubscriptions();
    }

    private void maybeShowDisableDialog() {
        if (Prefs.getBoolean(this.mContext, "QsHasTurnedOffMobileData", false)) {
            this.mDataController.setMobileDataEnabled(false);
            return;
        }
        String mobileDataNetworkName = this.mController.getMobileDataNetworkName();
        if (TextUtils.isEmpty(mobileDataNetworkName)) {
            mobileDataNetworkName = this.mContext.getString(R.string.mobile_data_disable_message_default_carrier);
        }
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this.mContext).setTitle(R.string.mobile_data_disable_title).setMessage(this.mContext.getString(R.string.mobile_data_disable_message, mobileDataNetworkName)).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(android.R.string.PERSOSUBSTATE_RUIM_HRPD_PUK_ENTRY, new DialogInterface.OnClickListener() { // from class: com.android.systemui.qs.tiles.-$$Lambda$CellularTile$oLJGrvqAwKFs9wNM4MvnfZ_a1QQ
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                CellularTile.lambda$maybeShowDisableDialog$0(this.f$0, dialogInterface, i);
            }
        }).create();
        alertDialogCreate.getWindow().setType(2009);
        SystemUIDialog.setShowForAllUsers(alertDialogCreate, true);
        SystemUIDialog.registerDismissListener(alertDialogCreate);
        SystemUIDialog.setWindowOnTop(alertDialogCreate);
        alertDialogCreate.show();
    }

    public static /* synthetic */ void lambda$maybeShowDisableDialog$0(CellularTile cellularTile, DialogInterface dialogInterface, int i) {
        cellularTile.mDataController.setMobileDataEnabled(false);
        Prefs.putBoolean(cellularTile.mContext, "QsHasTurnedOffMobileData", true);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleSecondaryClick() {
        if (this.mDataController.isMobileDataSupported()) {
            showDetail(true);
        } else {
            this.mActivityStarter.postStartActivityDismissingKeyguard(getCellularSettingIntent(), 0);
        }
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public CharSequence getTileLabel() {
        if (this.mDisplayDataUsage) {
            return this.mContext.getString(R.string.data_usage);
        }
        return this.mContext.getString(R.string.quick_settings_cellular_detail_title);
    }

    /* JADX DEBUG: Method merged with bridge method: handleUpdateState(Lcom/android/systemui/plugins/qs/QSTile$State;Ljava/lang/Object;)V */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleUpdateState(QSTile.SignalState signalState, Object obj) throws Resources.NotFoundException {
        Object string;
        if (this.mDisplayDataUsage) {
            signalState.icon = this.mIcon;
            signalState.label = this.mContext.getString(R.string.data_usage);
            signalState.contentDescription = this.mContext.getString(R.string.data_usage);
            return;
        }
        CallbackInfo callbackInfo = (CallbackInfo) obj;
        if (callbackInfo == null) {
            callbackInfo = this.mSignalCallback.mInfo;
        }
        Resources resources = this.mContext.getResources();
        signalState.activityIn = callbackInfo.enabled && callbackInfo.activityIn;
        signalState.activityOut = callbackInfo.enabled && callbackInfo.activityOut;
        signalState.label = resources.getString(R.string.mobile_data);
        boolean z = this.mDataController.isMobileDataSupported() && this.mDataController.isMobileDataEnabled();
        signalState.value = z;
        signalState.expandedAccessibilityClassName = Switch.class.getName();
        if (callbackInfo.noSim) {
            signalState.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_no_sim);
        } else {
            signalState.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_swap_vert);
        }
        if (callbackInfo.noSim) {
            signalState.state = 0;
            signalState.secondaryLabel = resources.getString(R.string.keyguard_missing_sim_message_short);
        } else if (callbackInfo.airplaneModeEnabled) {
            signalState.state = 0;
            signalState.secondaryLabel = resources.getString(R.string.status_bar_airplane);
        } else if (z) {
            signalState.state = 2;
            signalState.secondaryLabel = getMobileDataDescription(callbackInfo);
        } else {
            signalState.state = 1;
            signalState.secondaryLabel = resources.getString(R.string.cell_data_off);
        }
        if (signalState.state == 1) {
            string = resources.getString(R.string.cell_data_off_content_description);
        } else {
            string = signalState.secondaryLabel;
        }
        signalState.contentDescription = ((Object) signalState.label) + ", " + string;
    }

    private CharSequence getMobileDataDescription(CallbackInfo callbackInfo) {
        if (callbackInfo.roaming && !TextUtils.isEmpty(callbackInfo.dataContentDescription)) {
            return this.mContext.getString(R.string.mobile_data_text_format, this.mContext.getString(R.string.data_connection_roaming), callbackInfo.dataContentDescription);
        }
        if (callbackInfo.roaming) {
            return this.mContext.getString(R.string.data_connection_roaming);
        }
        return callbackInfo.dataContentDescription;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public int getMetricsCategory() {
        return com.android.systemui.plugins.R.styleable.AppCompatTheme_windowFixedHeightMinor;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public boolean isAvailable() {
        return this.mController.hasMobileDataFeature();
    }

    private static final class CallbackInfo {
        boolean activityIn;
        boolean activityOut;
        boolean airplaneModeEnabled;
        String dataContentDescription;
        boolean enabled;
        boolean noSim;
        boolean roaming;

        private CallbackInfo() {
        }
    }

    private final class CellSignalCallback implements NetworkController.SignalCallback {
        private final CallbackInfo mInfo;

        private CellSignalCallback() {
            this.mInfo = new CallbackInfo();
        }

        @Override // com.android.systemui.statusbar.policy.NetworkController.SignalCallback
        public void setMobileDataIndicators(NetworkController.IconState iconState, NetworkController.IconState iconState2, int i, int i2, int i3, int i4, boolean z, boolean z2, String str, String str2, boolean z3, int i5, boolean z4, boolean z5) {
            if (iconState2 == null) {
                return;
            }
            this.mInfo.enabled = iconState2.visible;
            this.mInfo.dataContentDescription = str;
            this.mInfo.activityIn = z;
            this.mInfo.activityOut = z2;
            this.mInfo.roaming = z4;
            CellularTile.this.refreshState(this.mInfo);
        }

        @Override // com.android.systemui.statusbar.policy.NetworkController.SignalCallback
        public void setNoSims(boolean z, boolean z2) {
            this.mInfo.noSim = z;
            CellularTile.this.refreshState(this.mInfo);
        }

        @Override // com.android.systemui.statusbar.policy.NetworkController.SignalCallback
        public void setIsAirplaneMode(NetworkController.IconState iconState) {
            this.mInfo.airplaneModeEnabled = iconState.visible;
            CellularTile.this.refreshState(this.mInfo);
        }

        @Override // com.android.systemui.statusbar.policy.NetworkController.SignalCallback
        public void setMobileDataEnabled(boolean z) {
            CellularTile.this.mDetailAdapter.setMobileDataEnabled(z);
        }
    }

    static Intent getCellularSettingIntent() {
        return new Intent("android.settings.DATA_USAGE_SETTINGS");
    }

    private final class CellularDetailAdapter implements DetailAdapter {
        private CellularDetailAdapter() {
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public CharSequence getTitle() {
            return CellularTile.this.mContext.getString(R.string.quick_settings_cellular_detail_title);
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public Boolean getToggleState() {
            if (CellularTile.this.mDataController.isMobileDataSupported()) {
                return Boolean.valueOf(CellularTile.this.mDataController.isMobileDataEnabled());
            }
            return null;
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public Intent getSettingsIntent() {
            return CellularTile.getCellularSettingIntent();
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public void setToggleState(boolean z) {
            MetricsLogger.action(CellularTile.this.mContext, 155, z);
            CellularTile.this.mDataController.setMobileDataEnabled(z);
            if (z) {
                CellularTile.this.mQuickSettingsPlugin.disableDataForOtherSubscriptions();
            }
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public int getMetricsCategory() {
            return com.android.systemui.plugins.R.styleable.AppCompatTheme_windowFixedWidthMinor;
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public View createDetailView(Context context, View view, ViewGroup viewGroup) throws Resources.NotFoundException {
            if (view == null) {
                view = LayoutInflater.from(CellularTile.this.mContext).inflate(R.layout.data_usage, viewGroup, false);
            }
            DataUsageDetailView dataUsageDetailView = (DataUsageDetailView) view;
            DataUsageController.DataUsageInfo dataUsageInfo = CellularTile.this.mDataController.getDataUsageInfo();
            if (dataUsageInfo == null) {
                return dataUsageDetailView;
            }
            dataUsageDetailView.bind(dataUsageInfo);
            dataUsageDetailView.findViewById(R.id.roaming_text).setVisibility(CellularTile.this.mSignalCallback.mInfo.roaming ? 0 : 4);
            return dataUsageDetailView;
        }

        public void setMobileDataEnabled(boolean z) {
            CellularTile.this.fireToggleStateChanged(z);
        }
    }
}
