package com.android.simappdialog;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.android.setupwizardlib.util.WizardManagerHelper;

public class InstallCarrierAppActivity extends Activity implements View.OnClickListener {
    @Override
    protected void onCreate(Bundle bundle) {
        setTheme(WizardManagerHelper.getThemeRes(SystemProperties.get("setupwizard.theme"), R.style.SuwThemeGlif_Light));
        super.onCreate(bundle);
        setContentView(R.layout.install_carrier_app_activity);
        ((Button) findViewById(R.id.skip_button)).setOnClickListener(this);
        ((Button) findViewById(R.id.download_button)).setOnClickListener(this);
        Intent intent = getIntent();
        if (intent != null) {
            String stringExtra = intent.getStringExtra("carrier_name");
            if (!TextUtils.isEmpty(stringExtra)) {
                ((TextView) findViewById(R.id.install_carrier_app_description)).setText(getString(R.string.install_carrier_app_description, stringExtra));
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.skip_button:
                finish(1);
                break;
            case R.id.download_button:
                finish(2);
                break;
        }
    }

    private void finish(int i) {
        setResult(i);
        finish();
    }
}
