package com.android.systemui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.util.Slog;
import com.android.systemui.DessertCaseView;

public class DessertCase extends Activity {
    DessertCaseView mView;

    @Override
    public void onStart() {
        super.onStart();
        PackageManager pm = getPackageManager();
        ComponentName cn = new ComponentName(this, (Class<?>) DessertCaseDream.class);
        if (pm.getComponentEnabledSetting(cn) != 1) {
            Slog.v("DessertCase", "ACHIEVEMENT UNLOCKED");
            pm.setComponentEnabledSetting(cn, 1, 1);
        }
        this.mView = new DessertCaseView(this);
        DessertCaseView.RescalingContainer container = new DessertCaseView.RescalingContainer(this);
        container.setView(this.mView);
        setContentView(container);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mView.postDelayed(new Runnable() {
            @Override
            public void run() {
                DessertCase.this.mView.start();
            }
        }, 1000L);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mView.stop();
    }
}
