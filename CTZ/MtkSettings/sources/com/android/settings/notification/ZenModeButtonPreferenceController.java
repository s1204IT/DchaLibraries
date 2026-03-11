package com.android.settings.notification;

import android.app.FragmentManager;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.view.View;
import android.widget.Button;
import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeButtonPreferenceController extends AbstractZenModePreferenceController implements PreferenceControllerMixin {
    private FragmentManager mFragment;
    private Button mZenButtonOff;
    private Button mZenButtonOn;

    public ZenModeButtonPreferenceController(Context context, Lifecycle lifecycle, FragmentManager fragmentManager) {
        super(context, "zen_mode_settings_button_container", lifecycle);
        this.mFragment = fragmentManager;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return "zen_mode_settings_button_container";
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (this.mZenButtonOn == null) {
            this.mZenButtonOn = (Button) ((LayoutPreference) preference).findViewById(R.id.zen_mode_settings_turn_on_button);
            updateZenButtonOnClickListener();
        }
        if (this.mZenButtonOff == null) {
            this.mZenButtonOff = (Button) ((LayoutPreference) preference).findViewById(R.id.zen_mode_settings_turn_off_button);
            this.mZenButtonOff.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    ZenModeButtonPreferenceController.lambda$updateState$0(this.f$0, view);
                }
            });
        }
        updateButtons();
    }

    public static void lambda$updateState$0(ZenModeButtonPreferenceController zenModeButtonPreferenceController, View view) {
        zenModeButtonPreferenceController.mMetricsFeatureProvider.action(zenModeButtonPreferenceController.mContext, 1268, false);
        zenModeButtonPreferenceController.mBackend.setZenMode(0);
    }

    private void updateButtons() {
        switch (getZenMode()) {
            case 1:
            case 2:
            case 3:
                this.mZenButtonOff.setVisibility(0);
                this.mZenButtonOn.setVisibility(8);
                break;
            default:
                this.mZenButtonOff.setVisibility(8);
                updateZenButtonOnClickListener();
                this.mZenButtonOn.setVisibility(0);
                break;
        }
    }

    private void updateZenButtonOnClickListener() {
        final int zenDuration = getZenDuration();
        switch (zenDuration) {
            case -1:
                this.mZenButtonOn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        ZenModeButtonPreferenceController.lambda$updateZenButtonOnClickListener$1(this.f$0, view);
                    }
                });
                break;
            case 0:
                this.mZenButtonOn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        ZenModeButtonPreferenceController.lambda$updateZenButtonOnClickListener$2(this.f$0, view);
                    }
                });
                break;
            default:
                this.mZenButtonOn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        ZenModeButtonPreferenceController.lambda$updateZenButtonOnClickListener$3(this.f$0, zenDuration, view);
                    }
                });
                break;
        }
    }

    public static void lambda$updateZenButtonOnClickListener$1(ZenModeButtonPreferenceController zenModeButtonPreferenceController, View view) {
        zenModeButtonPreferenceController.mMetricsFeatureProvider.action(zenModeButtonPreferenceController.mContext, 1268, false);
        new SettingsEnableZenModeDialog().show(zenModeButtonPreferenceController.mFragment, "EnableZenModeButton");
    }

    public static void lambda$updateZenButtonOnClickListener$2(ZenModeButtonPreferenceController zenModeButtonPreferenceController, View view) {
        zenModeButtonPreferenceController.mMetricsFeatureProvider.action(zenModeButtonPreferenceController.mContext, 1268, false);
        zenModeButtonPreferenceController.mBackend.setZenMode(1);
    }

    public static void lambda$updateZenButtonOnClickListener$3(ZenModeButtonPreferenceController zenModeButtonPreferenceController, int i, View view) {
        zenModeButtonPreferenceController.mMetricsFeatureProvider.action(zenModeButtonPreferenceController.mContext, 1268, false);
        zenModeButtonPreferenceController.mBackend.setZenModeForDuration(i);
    }
}
