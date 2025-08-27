package com.android.systemui.tuner;

import android.app.Fragment;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;

/* loaded from: classes.dex */
public class PowerNotificationControlsFragment extends Fragment {
    @Override // android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override // android.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return layoutInflater.inflate(R.layout.power_notification_controls_settings, viewGroup, false);
    }

    @Override // android.app.Fragment
    public void onViewCreated(View view, Bundle bundle) {
        String string;
        super.onViewCreated(view, bundle);
        View viewFindViewById = view.findViewById(R.id.switch_bar);
        final Switch r3 = (Switch) viewFindViewById.findViewById(android.R.id.switch_widget);
        final TextView textView = (TextView) viewFindViewById.findViewById(R.id.switch_text);
        r3.setChecked(isEnabled());
        if (isEnabled()) {
            string = getString(R.string.switch_bar_on);
        } else {
            string = getString(R.string.switch_bar_off);
        }
        textView.setText(string);
        r3.setOnClickListener(new View.OnClickListener() { // from class: com.android.systemui.tuner.PowerNotificationControlsFragment.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view2) {
                String string2;
                boolean z = !PowerNotificationControlsFragment.this.isEnabled();
                MetricsLogger.action(PowerNotificationControlsFragment.this.getContext(), 393, z);
                Settings.Secure.putInt(PowerNotificationControlsFragment.this.getContext().getContentResolver(), "show_importance_slider", z ? 1 : 0);
                r3.setChecked(z);
                TextView textView2 = textView;
                if (z) {
                    string2 = PowerNotificationControlsFragment.this.getString(R.string.switch_bar_on);
                } else {
                    string2 = PowerNotificationControlsFragment.this.getString(R.string.switch_bar_off);
                }
                textView2.setText(string2);
            }
        });
    }

    @Override // android.app.Fragment
    public void onResume() {
        super.onResume();
        MetricsLogger.visibility(getContext(), 392, true);
    }

    @Override // android.app.Fragment
    public void onPause() {
        super.onPause();
        MetricsLogger.visibility(getContext(), 392, false);
    }

    private boolean isEnabled() {
        return Settings.Secure.getInt(getContext().getContentResolver(), "show_importance_slider", 0) == 1;
    }
}
