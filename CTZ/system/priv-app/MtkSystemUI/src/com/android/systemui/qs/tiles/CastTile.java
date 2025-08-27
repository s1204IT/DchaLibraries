package com.android.systemui.qs.tiles;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.android.internal.app.MediaRouteDialogPresenter;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSDetailItems;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

/* loaded from: classes.dex */
public class CastTile extends QSTileImpl<QSTile.BooleanState> {
    private static final Intent CAST_SETTINGS = new Intent("android.settings.CAST_SETTINGS");
    private final ActivityStarter mActivityStarter;
    private final Callback mCallback;
    private final CastController mController;
    private final CastDetailAdapter mDetailAdapter;
    private Dialog mDialog;
    private final KeyguardMonitor mKeyguard;

    public CastTile(QSHost qSHost) {
        super(qSHost);
        this.mCallback = new Callback();
        this.mController = (CastController) Dependency.get(CastController.class);
        this.mDetailAdapter = new CastDetailAdapter();
        this.mKeyguard = (KeyguardMonitor) Dependency.get(KeyguardMonitor.class);
        this.mActivityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public boolean isAvailable() {
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.direct");
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
        if (DEBUG) {
            Log.d(this.TAG, "handleSetListening " + z);
        }
        if (z) {
            this.mController.addCallback(this.mCallback);
            this.mKeyguard.addCallback(this.mCallback);
        } else {
            this.mController.setDiscovering(false);
            this.mController.removeCallback(this.mCallback);
            this.mKeyguard.removeCallback(this.mCallback);
        }
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleUserSwitch(int i) {
        super.handleUserSwitch(i);
        this.mController.setCurrentUserId(i);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public Intent getLongClickIntent() {
        return new Intent("android.settings.CAST_SETTINGS");
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleSecondaryClick() {
        handleClick();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleClick() {
        if (this.mKeyguard.isSecure() && !this.mKeyguard.canSkipBouncer()) {
            this.mActivityStarter.postQSRunnableDismissingKeyguard(new Runnable() { // from class: com.android.systemui.qs.tiles.-$$Lambda$CastTile$0TU5SvbFGUs5F0udF1tvlhHVObs
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.showDetail(true);
                }
            });
        } else {
            showDetail(true);
        }
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void showDetail(boolean z) {
        this.mUiHandler.post(new Runnable() { // from class: com.android.systemui.qs.tiles.-$$Lambda$CastTile$WPXsuhhRJ1um-wt53q0kaFd3rzI
            @Override // java.lang.Runnable
            public final void run() {
                CastTile.lambda$showDetail$3(this.f$0);
            }
        });
    }

    public static /* synthetic */ void lambda$showDetail$3(final CastTile castTile) {
        castTile.mDialog = MediaRouteDialogPresenter.createDialog(castTile.mContext, 4, new View.OnClickListener() { // from class: com.android.systemui.qs.tiles.-$$Lambda$CastTile$4kXW6ECEqBpSUmuEtdBz8p9QY1w
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                CastTile.lambda$showDetail$1(this.f$0, view);
            }
        });
        castTile.mDialog.getWindow().setType(2009);
        SystemUIDialog.setShowForAllUsers(castTile.mDialog, true);
        SystemUIDialog.registerDismissListener(castTile.mDialog);
        SystemUIDialog.setWindowOnTop(castTile.mDialog);
        castTile.mUiHandler.post(new Runnable() { // from class: com.android.systemui.qs.tiles.-$$Lambda$CastTile$MhJepZXXVH2Vaj80AOmfpppL58s
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.mDialog.show();
            }
        });
        castTile.mHost.collapsePanels();
    }

    public static /* synthetic */ void lambda$showDetail$1(CastTile castTile, View view) {
        castTile.mDialog.dismiss();
        ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(castTile.getLongClickIntent(), 0);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_cast_title);
    }

    /* JADX DEBUG: Method merged with bridge method: handleUpdateState(Lcom/android/systemui/plugins/qs/QSTile$State;Ljava/lang/Object;)V */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleUpdateState(QSTile.BooleanState booleanState, Object obj) {
        booleanState.label = this.mContext.getString(R.string.quick_settings_cast_title);
        booleanState.contentDescription = booleanState.label;
        booleanState.value = false;
        Set<CastController.CastDevice> castDevices = this.mController.getCastDevices();
        Iterator<CastController.CastDevice> it = castDevices.iterator();
        boolean z = false;
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            CastController.CastDevice next = it.next();
            if (next.state == 2) {
                booleanState.value = true;
                booleanState.label = getDeviceName(next);
                booleanState.contentDescription = ((Object) booleanState.contentDescription) + "," + this.mContext.getString(R.string.accessibility_cast_name, booleanState.label);
            } else if (next.state == 1) {
                z = true;
            }
        }
        if (!booleanState.value && z) {
            booleanState.label = this.mContext.getString(R.string.quick_settings_connecting);
        }
        booleanState.state = booleanState.value ? 2 : 1;
        booleanState.icon = QSTileImpl.ResourceIcon.get(booleanState.value ? R.drawable.ic_qs_cast_on : R.drawable.ic_qs_cast_off);
        this.mDetailAdapter.updateItems(castDevices);
        booleanState.expandedAccessibilityClassName = Button.class.getName();
        booleanState.contentDescription = ((Object) booleanState.contentDescription) + "," + this.mContext.getString(R.string.accessibility_quick_settings_open_details);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public int getMetricsCategory() {
        return com.android.systemui.plugins.R.styleable.AppCompatTheme_windowFixedHeightMajor;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected String composeChangeAnnouncement() {
        if (!((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_casting_turned_off);
        }
        return null;
    }

    private String getDeviceName(CastController.CastDevice castDevice) {
        return castDevice.name != null ? castDevice.name : this.mContext.getString(R.string.quick_settings_cast_device_default_name);
    }

    private final class Callback implements CastController.Callback, KeyguardMonitor.Callback {
        private Callback() {
        }

        @Override // com.android.systemui.statusbar.policy.CastController.Callback
        public void onCastDevicesChanged() {
            CastTile.this.refreshState();
        }

        @Override // com.android.systemui.statusbar.policy.KeyguardMonitor.Callback
        public void onKeyguardShowingChanged() {
            CastTile.this.refreshState();
        }
    }

    private final class CastDetailAdapter implements DetailAdapter, QSDetailItems.Callback {
        private QSDetailItems mItems;
        private final LinkedHashMap<String, CastController.CastDevice> mVisibleOrder;

        private CastDetailAdapter() {
            this.mVisibleOrder = new LinkedHashMap<>();
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public CharSequence getTitle() {
            return CastTile.this.mContext.getString(R.string.quick_settings_cast_title);
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public Boolean getToggleState() {
            return null;
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public Intent getSettingsIntent() {
            return CastTile.CAST_SETTINGS;
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public void setToggleState(boolean z) {
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public int getMetricsCategory() {
            return 151;
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public View createDetailView(Context context, View view, ViewGroup viewGroup) {
            this.mItems = QSDetailItems.convertOrInflate(context, view, viewGroup);
            this.mItems.setTagSuffix("Cast");
            if (view == null) {
                if (CastTile.DEBUG) {
                    Log.d(CastTile.this.TAG, "addOnAttachStateChangeListener");
                }
                this.mItems.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() { // from class: com.android.systemui.qs.tiles.CastTile.CastDetailAdapter.1
                    @Override // android.view.View.OnAttachStateChangeListener
                    public void onViewAttachedToWindow(View view2) {
                        if (CastTile.DEBUG) {
                            Log.d(CastTile.this.TAG, "onViewAttachedToWindow");
                        }
                    }

                    @Override // android.view.View.OnAttachStateChangeListener
                    public void onViewDetachedFromWindow(View view2) {
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

        private void updateItems(Set<CastController.CastDevice> set) {
            int i;
            if (this.mItems == null) {
                return;
            }
            QSDetailItems.Item[] itemArr = null;
            if (set != null && !set.isEmpty()) {
                Iterator<CastController.CastDevice> it = set.iterator();
                while (true) {
                    i = 0;
                    if (!it.hasNext()) {
                        break;
                    }
                    CastController.CastDevice next = it.next();
                    if (next.state == 2) {
                        QSDetailItems.Item item = new QSDetailItems.Item();
                        item.iconResId = R.drawable.ic_qs_cast_on;
                        item.line1 = CastTile.this.getDeviceName(next);
                        item.line2 = CastTile.this.mContext.getString(R.string.quick_settings_connected);
                        item.tag = next;
                        item.canDisconnect = true;
                        itemArr = new QSDetailItems.Item[]{item};
                        break;
                    }
                }
                if (itemArr == null) {
                    for (CastController.CastDevice castDevice : set) {
                        this.mVisibleOrder.put(castDevice.id, castDevice);
                    }
                    itemArr = new QSDetailItems.Item[set.size()];
                    Iterator<String> it2 = this.mVisibleOrder.keySet().iterator();
                    while (it2.hasNext()) {
                        CastController.CastDevice castDevice2 = this.mVisibleOrder.get(it2.next());
                        if (set.contains(castDevice2)) {
                            QSDetailItems.Item item2 = new QSDetailItems.Item();
                            item2.iconResId = R.drawable.ic_qs_cast_off;
                            item2.line1 = CastTile.this.getDeviceName(castDevice2);
                            if (castDevice2.state == 1) {
                                item2.line2 = CastTile.this.mContext.getString(R.string.quick_settings_connecting);
                            }
                            item2.tag = castDevice2;
                            itemArr[i] = item2;
                            i++;
                        }
                    }
                }
            }
            this.mItems.setItems(itemArr);
        }

        @Override // com.android.systemui.qs.QSDetailItems.Callback
        public void onDetailItemClick(QSDetailItems.Item item) {
            if (item != null && item.tag != null) {
                MetricsLogger.action(CastTile.this.mContext, 157);
                CastTile.this.mController.startCasting((CastController.CastDevice) item.tag);
            }
        }

        @Override // com.android.systemui.qs.QSDetailItems.Callback
        public void onDetailItemDisconnect(QSDetailItems.Item item) {
            if (item != null && item.tag != null) {
                MetricsLogger.action(CastTile.this.mContext, 158);
                CastTile.this.mController.stopCasting((CastController.CastDevice) item.tag);
            }
        }
    }
}
