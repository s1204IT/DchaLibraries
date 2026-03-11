package com.android.settings.applications;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;

public class ConvertToFbe extends SettingsPreferenceFragment {
    public boolean runKeyguardConfirmation(int request) {
        Resources res = getActivity().getResources();
        return new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivity(request, res.getText(R.string.convert_to_file_encryption));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.convert_fbe, (ViewGroup) null);
        Button button = (Button) rootView.findViewById(R.id.button_convert_fbe);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ConvertToFbe.this.runKeyguardConfirmation(55)) {
                    return;
                }
                ConvertToFbe.this.convert();
            }
        });
        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 55 || resultCode != -1) {
            return;
        }
        convert();
    }

    public void convert() {
        SettingsActivity sa = (SettingsActivity) getActivity();
        sa.startPreferencePanel(ConfirmConvertToFbe.class.getName(), null, R.string.convert_to_file_encryption, null, null, 0);
    }

    @Override
    protected int getMetricsCategory() {
        return 402;
    }
}
