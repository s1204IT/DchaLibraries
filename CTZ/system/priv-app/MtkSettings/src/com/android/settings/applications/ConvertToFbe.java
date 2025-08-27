package com.android.settings.applications;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.password.ChooseLockSettingsHelper;

/* loaded from: classes.dex */
public class ConvertToFbe extends InstrumentedFragment {
    private boolean runKeyguardConfirmation(int i) {
        return new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivity(i, getActivity().getResources().getText(R.string.convert_to_file_encryption));
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getActivity().setTitle(R.string.convert_to_file_encryption);
    }

    @Override // android.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate = layoutInflater.inflate(R.layout.convert_fbe, (ViewGroup) null);
        ((Button) viewInflate.findViewById(R.id.button_convert_fbe)).setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.applications.-$$Lambda$ConvertToFbe$cKWuNkHe-dkbg8HKJCoDk07_9og
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ConvertToFbe.lambda$onCreateView$0(this.f$0, view);
            }
        });
        return viewInflate;
    }

    public static /* synthetic */ void lambda$onCreateView$0(ConvertToFbe convertToFbe, View view) {
        if (!convertToFbe.runKeyguardConfirmation(55)) {
            convertToFbe.convert();
        }
    }

    @Override // android.app.Fragment
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == 55 && i2 == -1) {
            convert();
        }
    }

    private void convert() {
        new SubSettingLauncher(getContext()).setDestination(ConfirmConvertToFbe.class.getName()).setTitle(R.string.convert_to_file_encryption).setSourceMetricsCategory(getMetricsCategory()).launch();
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 402;
    }
}
