package com.mediatek.settings.inputmethod;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.android.settings.password.ChooseLockSettingsHelper;

/* loaded from: classes.dex */
public class VowKeyguardConfirm extends Activity {
    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.d("VowKeyguardConfirm", "onCreate");
        if (!runKeyguardConfirmation(55)) {
            setResult(-1);
            finish();
        }
    }

    @Override // android.app.Activity
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        Log.d("VowKeyguardConfirm", "onActivityResult: requestCode = " + i + ", resultCode = " + i2);
        if (i != 55) {
            return;
        }
        if (i2 == -1) {
            setResult(-1);
            finish();
        } else {
            setResult(0);
            finish();
        }
    }

    private boolean runKeyguardConfirmation(int i) {
        return new ChooseLockSettingsHelper(this).launchConfirmationActivity(i, getIntent().getCharSequenceExtra("title"), null, null, 0L);
    }
}
