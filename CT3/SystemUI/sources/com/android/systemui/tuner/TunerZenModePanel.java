package com.android.systemui.tuner;

import android.content.Context;
import android.content.Intent;
import android.os.BenesseExtension;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.volume.ZenModePanel;

public class TunerZenModePanel extends LinearLayout implements View.OnClickListener {
    private View mButtons;
    private ZenModePanel.Callback mCallback;
    private ZenModeController mController;
    private View mDone;
    private View.OnClickListener mDoneListener;
    private boolean mEditing;
    private View mHeaderSwitch;
    private View mMoreSettings;
    private final Runnable mUpdate;
    private int mZenMode;
    private ZenModePanel mZenModePanel;

    public TunerZenModePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mUpdate = new Runnable() {
            @Override
            public void run() {
                TunerZenModePanel.this.updatePanel();
            }
        };
    }

    public void init(ZenModeController zenModeController) {
        this.mController = zenModeController;
        this.mHeaderSwitch = findViewById(R.id.tuner_zen_switch);
        this.mHeaderSwitch.setVisibility(0);
        this.mHeaderSwitch.setOnClickListener(this);
        this.mHeaderSwitch.findViewById(android.R.id.accessibilitySystemActionBack).setVisibility(8);
        ((TextView) this.mHeaderSwitch.findViewById(android.R.id.title)).setText(R.string.quick_settings_dnd_label);
        this.mZenModePanel = (ZenModePanel) findViewById(R.id.zen_mode_panel);
        this.mZenModePanel.init(zenModeController);
        this.mButtons = findViewById(R.id.tuner_zen_buttons);
        this.mMoreSettings = this.mButtons.findViewById(android.R.id.button2);
        this.mMoreSettings.setOnClickListener(this);
        ((TextView) this.mMoreSettings).setText(R.string.quick_settings_more_settings);
        this.mDone = this.mButtons.findViewById(android.R.id.button1);
        this.mDone.setOnClickListener(this);
        ((TextView) this.mDone).setText(R.string.quick_settings_done);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mEditing = false;
    }

    public void setCallback(ZenModePanel.Callback zenPanelCallback) {
        this.mCallback = zenPanelCallback;
        this.mZenModePanel.setCallback(zenPanelCallback);
    }

    @Override
    public void onClick(View v) {
        if (v == this.mHeaderSwitch) {
            this.mEditing = true;
            if (this.mZenMode == 0) {
                this.mZenMode = Prefs.getInt(this.mContext, "DndFavoriteZen", 3);
                this.mController.setZen(this.mZenMode, null, "TunerZenModePanel");
                postUpdatePanel();
                return;
            } else {
                this.mZenMode = 0;
                this.mController.setZen(0, null, "TunerZenModePanel");
                postUpdatePanel();
                return;
            }
        }
        if (v == this.mMoreSettings && BenesseExtension.getDchaState() == 0) {
            Intent intent = new Intent("android.settings.ZEN_MODE_SETTINGS");
            intent.addFlags(268435456);
            getContext().startActivity(intent);
        } else {
            if (v != this.mDone) {
                return;
            }
            this.mEditing = false;
            setVisibility(8);
            this.mDoneListener.onClick(v);
        }
    }

    public boolean isEditing() {
        return this.mEditing;
    }

    public void setZenState(int zenMode) {
        this.mZenMode = zenMode;
        postUpdatePanel();
    }

    private void postUpdatePanel() {
        removeCallbacks(this.mUpdate);
        postDelayed(this.mUpdate, 40L);
    }

    public void setDoneListener(View.OnClickListener onClickListener) {
        this.mDoneListener = onClickListener;
    }

    public void updatePanel() {
        boolean zenOn = this.mZenMode != 0;
        ((Checkable) this.mHeaderSwitch.findViewById(android.R.id.toggle)).setChecked(zenOn);
        this.mZenModePanel.setVisibility(zenOn ? 0 : 8);
        this.mButtons.setVisibility(zenOn ? 0 : 8);
    }
}
