package com.android.settings.applications;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class ConfirmConvertToFbe extends SettingsPreferenceFragment {
    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate = layoutInflater.inflate(R.layout.confirm_convert_fbe, (ViewGroup) null);
        ((Button) viewInflate.findViewById(R.id.button_confirm_convert_fbe)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent("android.intent.action.FACTORY_RESET");
                intent.addFlags(268435456);
                intent.setPackage("android");
                intent.putExtra("android.intent.extra.REASON", "convert_fbe");
                ConfirmConvertToFbe.this.getActivity().sendBroadcast(intent);
            }
        });
        return viewInflate;
    }

    @Override
    public int getMetricsCategory() {
        return 403;
    }
}
