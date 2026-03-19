package com.android.settings.fingerprint;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.fingerprint.FingerprintEnrollSidecar;
import com.android.settings.password.ChooseLockSettingsHelper;

public class FingerprintEnrollFindSensor extends FingerprintEnrollBase {
    private FingerprintFindSensorAnimation mAnimation;
    private boolean mLaunchedConfirmLock;
    private boolean mNextClicked;
    private FingerprintEnrollSidecar mSidecar;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(getContentView());
        ((Button) findViewById(R.id.skip_button)).setOnClickListener(this);
        setHeaderText(R.string.security_settings_fingerprint_enroll_find_sensor_title);
        if (bundle != null) {
            this.mLaunchedConfirmLock = bundle.getBoolean("launched_confirm_lock");
            this.mToken = bundle.getByteArray("hw_auth_token");
        }
        if (this.mToken == null && !this.mLaunchedConfirmLock) {
            launchConfirmLock();
        } else if (this.mToken != null) {
            startLookingForFingerprint();
        }
        KeyEvent.Callback callbackFindViewById = findViewById(R.id.fingerprint_sensor_location_animation);
        if (callbackFindViewById instanceof FingerprintFindSensorAnimation) {
            this.mAnimation = (FingerprintFindSensorAnimation) callbackFindViewById;
        } else {
            this.mAnimation = null;
        }
    }

    protected int getContentView() {
        return R.layout.fingerprint_enroll_find_sensor;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (this.mAnimation != null) {
            this.mAnimation.startAnimation();
        }
    }

    private void startLookingForFingerprint() {
        this.mSidecar = (FingerprintEnrollSidecar) getFragmentManager().findFragmentByTag("sidecar");
        if (this.mSidecar == null) {
            this.mSidecar = new FingerprintEnrollSidecar();
            getFragmentManager().beginTransaction().add(this.mSidecar, "sidecar").commit();
        }
        this.mSidecar.setListener(new FingerprintEnrollSidecar.Listener() {
            @Override
            public void onEnrollmentProgressChange(int i, int i2) {
                FingerprintEnrollFindSensor.this.mNextClicked = true;
                FingerprintEnrollFindSensor.this.proceedToEnrolling(true);
            }

            @Override
            public void onEnrollmentHelp(CharSequence charSequence) {
            }

            @Override
            public void onEnrollmentError(int i, CharSequence charSequence) {
                if (FingerprintEnrollFindSensor.this.mNextClicked && i == 5) {
                    FingerprintEnrollFindSensor.this.mNextClicked = false;
                    FingerprintEnrollFindSensor.this.proceedToEnrolling(false);
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (this.mAnimation != null) {
            this.mAnimation.pauseAnimation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.mAnimation != null) {
            this.mAnimation.stopAnimation();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("launched_confirm_lock", this.mLaunchedConfirmLock);
        bundle.putByteArray("hw_auth_token", this.mToken);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.skip_button) {
            onSkipButtonClick();
        } else {
            super.onClick(view);
        }
    }

    protected void onSkipButtonClick() {
        setResult(2);
        finish();
    }

    private void proceedToEnrolling(boolean z) {
        if (this.mSidecar != null) {
            if (z && this.mSidecar.cancelEnrollment()) {
                return;
            }
            getFragmentManager().beginTransaction().remove(this.mSidecar).commitAllowingStateLoss();
            this.mSidecar = null;
            startActivityForResult(getEnrollingIntent(), 2);
        }
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        if (i == 1) {
            if (i2 == -1) {
                this.mToken = intent.getByteArrayExtra("hw_auth_token");
                overridePendingTransition(R.anim.suw_slide_next_in, R.anim.suw_slide_next_out);
                getIntent().putExtra("hw_auth_token", this.mToken);
                startLookingForFingerprint();
                return;
            }
            finish();
            return;
        }
        if (i == 2) {
            if (i2 == 1) {
                setResult(1);
                finish();
                return;
            }
            if (i2 == 2) {
                setResult(2);
                finish();
                return;
            } else if (i2 == 3) {
                setResult(3);
                finish();
                return;
            } else if (Utils.getFingerprintManagerOrNull(this).getEnrolledFingerprints().size() >= getResources().getInteger(android.R.integer.config_cdma_3waycall_flash_delay)) {
                finish();
                return;
            } else {
                startLookingForFingerprint();
                return;
            }
        }
        super.onActivityResult(i, i2, intent);
    }

    private void launchConfirmLock() {
        boolean zLaunchConfirmationActivity;
        long jPreEnroll = Utils.getFingerprintManagerOrNull(this).preEnroll();
        ChooseLockSettingsHelper chooseLockSettingsHelper = new ChooseLockSettingsHelper(this);
        if (this.mUserId == -10000) {
            zLaunchConfirmationActivity = chooseLockSettingsHelper.launchConfirmationActivity(1, getString(R.string.security_settings_fingerprint_preference_title), null, null, jPreEnroll);
        } else {
            zLaunchConfirmationActivity = chooseLockSettingsHelper.launchConfirmationActivity(1, getString(R.string.security_settings_fingerprint_preference_title), (CharSequence) null, (CharSequence) null, jPreEnroll, this.mUserId);
        }
        if (!zLaunchConfirmationActivity) {
            finish();
        } else {
            this.mLaunchedConfirmLock = true;
        }
    }

    @Override
    public int getMetricsCategory() {
        return 241;
    }
}
