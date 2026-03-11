package com.android.settings.fingerprint;

import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.android.settings.R;

public class FingerprintEnrollFinish extends FingerprintEnrollBase {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingerprint_enroll_finish);
        setHeaderText(R.string.security_settings_fingerprint_enroll_finish_title);
        Button addButton = (Button) findViewById(R.id.add_another_button);
        FingerprintManager fpm = (FingerprintManager) getSystemService("fingerprint");
        int enrolled = fpm.getEnrolledFingerprints(this.mUserId).size();
        int max = getResources().getInteger(android.R.integer.config_externalDisplayPeakHeight);
        if (enrolled >= max) {
            addButton.setVisibility(4);
        } else {
            addButton.setOnClickListener(this);
        }
    }

    @Override
    protected void onNextButtonClick() {
        setResult(1);
        finish();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.add_another_button) {
            Intent intent = getEnrollingIntent();
            intent.addFlags(33554432);
            startActivity(intent);
            finish();
        }
        super.onClick(v);
    }

    @Override
    protected int getMetricsCategory() {
        return 242;
    }
}
