package com.android.systemui.tuner;

import android.R;
import android.content.Context;
import android.content.Intent;
import android.os.BenesseExtension;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.LinearLayout;
import com.android.systemui.Prefs;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.volume.ZenModePanel;

public class TunerZenModePanel extends LinearLayout implements View.OnClickListener {
    private View mButtons;
    private ZenModeController mController;
    private View mDone;
    private View.OnClickListener mDoneListener;
    private boolean mEditing;
    private View mHeaderSwitch;
    private View mMoreSettings;
    private final Runnable mUpdate;
    private int mZenMode;
    private ZenModePanel mZenModePanel;

    public TunerZenModePanel(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mUpdate = new Runnable() {
            @Override
            public void run() {
                TunerZenModePanel.this.updatePanel();
            }
        };
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mEditing = false;
    }

    @Override
    public void onClick(View view) {
        if (view == this.mHeaderSwitch) {
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
        if (view == this.mMoreSettings) {
            if (BenesseExtension.getDchaState() != 0) {
                return;
            }
            Intent intent = new Intent("android.settings.ZEN_MODE_SETTINGS");
            intent.addFlags(268435456);
            getContext().startActivity(intent);
            return;
        }
        if (view == this.mDone) {
            this.mEditing = false;
            setVisibility(8);
            this.mDoneListener.onClick(view);
        }
    }

    private void postUpdatePanel() {
        removeCallbacks(this.mUpdate);
        postDelayed(this.mUpdate, 40L);
    }

    private void updatePanel() {
        boolean z = this.mZenMode != 0;
        ((Checkable) this.mHeaderSwitch.findViewById(R.id.toggle)).setChecked(z);
        this.mZenModePanel.setVisibility(z ? 0 : 8);
        this.mButtons.setVisibility(z ? 0 : 8);
    }
}
