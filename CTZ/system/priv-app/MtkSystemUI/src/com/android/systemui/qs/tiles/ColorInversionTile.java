package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.widget.Switch;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.SecureSetting;
import com.android.systemui.qs.tileimpl.QSTileImpl;

/* loaded from: classes.dex */
public class ColorInversionTile extends QSTileImpl<QSTile.BooleanState> {
    private final QSTile.Icon mIcon;
    private final SecureSetting mSetting;

    public ColorInversionTile(QSHost qSHost) {
        super(qSHost);
        this.mIcon = QSTileImpl.ResourceIcon.get(R.drawable.ic_invert_colors);
        this.mSetting = new SecureSetting(this.mContext, this.mHandler, "accessibility_display_inversion_enabled") { // from class: com.android.systemui.qs.tiles.ColorInversionTile.1
            @Override // com.android.systemui.qs.SecureSetting
            protected void handleValueChanged(int i, boolean z) {
                ColorInversionTile.this.handleRefreshState(Integer.valueOf(i));
            }
        };
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleDestroy() {
        super.handleDestroy();
        this.mSetting.setListening(false);
    }

    /* JADX DEBUG: Method merged with bridge method: newTileState()Lcom/android/systemui/plugins/qs/QSTile$State; */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleSetListening(boolean z) {
        this.mSetting.setListening(z);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleUserSwitch(int i) {
        this.mSetting.setUserId(i);
        handleRefreshState(Integer.valueOf(this.mSetting.getValue()));
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public Intent getLongClickIntent() {
        return new Intent("android.settings.ACCESSIBILITY_SETTINGS");
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleClick() {
        this.mSetting.setValue(!((QSTile.BooleanState) this.mState).value ? 1 : 0);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_inversion_label);
    }

    /* JADX DEBUG: Method merged with bridge method: handleUpdateState(Lcom/android/systemui/plugins/qs/QSTile$State;Ljava/lang/Object;)V */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleUpdateState(QSTile.BooleanState booleanState, Object obj) {
        boolean z;
        if ((obj instanceof Integer ? ((Integer) obj).intValue() : this.mSetting.getValue()) == 0) {
            z = false;
        } else {
            z = true;
        }
        if (booleanState.slash == null) {
            booleanState.slash = new QSTile.SlashState();
        }
        booleanState.value = z;
        booleanState.slash.isSlashed = !booleanState.value;
        booleanState.state = booleanState.value ? 2 : 1;
        booleanState.label = this.mContext.getString(R.string.quick_settings_inversion_label);
        booleanState.icon = this.mIcon;
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
        booleanState.contentDescription = booleanState.label;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public int getMetricsCategory() {
        return com.android.systemui.plugins.R.styleable.AppCompatTheme_windowFixedWidthMajor;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_color_inversion_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_color_inversion_changed_off);
    }
}
