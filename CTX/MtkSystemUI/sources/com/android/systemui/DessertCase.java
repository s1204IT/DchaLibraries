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
        PackageManager packageManager = getPackageManager();
        ComponentName componentName = new ComponentName(this, (Class<?>) DessertCaseDream.class);
        if (packageManager.getComponentEnabledSetting(componentName) != 1) {
            Slog.v("DessertCase", "ACHIEVEMENT UNLOCKED");
            packageManager.setComponentEnabledSetting(componentName, 1, 1);
        }
        this.mView = new DessertCaseView(this);
        DessertCaseView.RescalingContainer rescalingContainer = new DessertCaseView.RescalingContainer(this);
        rescalingContainer.setView(this.mView);
        setContentView(rescalingContainer);
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
