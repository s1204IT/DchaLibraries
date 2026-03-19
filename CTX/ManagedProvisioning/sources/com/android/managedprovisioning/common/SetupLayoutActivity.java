package com.android.managedprovisioning.common;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.analytics.TimeLogger;

public abstract class SetupLayoutActivity extends Activity {
    private TimeLogger mTimeLogger;
    protected final Utils mUtils;

    public SetupLayoutActivity() {
        this(new Utils());
    }

    protected SetupLayoutActivity(Utils utils) {
        this.mUtils = utils;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mTimeLogger = new TimeLogger(this, getMetricsCategory());
        this.mTimeLogger.start();
        if (getResources().getBoolean(R.bool.lock_to_portrait)) {
            setRequestedOrientation(1);
        }
    }

    @Override
    public void onDestroy() {
        this.mTimeLogger.stop();
        super.onDestroy();
    }

    protected int getMetricsCategory() {
        return 0;
    }

    protected Utils getUtils() {
        return this.mUtils;
    }

    protected void setStatusBarColor(int i) {
        int i2;
        int iIntValue = toSolidColor(Integer.valueOf(i)).intValue();
        Window window = getWindow();
        window.clearFlags(67108864);
        window.addFlags(Integer.MIN_VALUE);
        window.setStatusBarColor(iIntValue);
        View decorView = getWindow().getDecorView();
        int systemUiVisibility = decorView.getSystemUiVisibility();
        if (getUtils().isBrightColor(iIntValue)) {
            i2 = systemUiVisibility | 8192;
        } else {
            i2 = systemUiVisibility & (-8193);
        }
        decorView.setSystemUiVisibility(i2);
        setTaskDescription(new ActivityManager.TaskDescription((String) null, (Bitmap) null, iIntValue));
    }

    private Integer toSolidColor(Integer num) {
        return Integer.valueOf(Color.argb(255, Color.red(num.intValue()), Color.green(num.intValue()), Color.blue(num.intValue())));
    }

    protected void showDialog(DialogBuilder dialogBuilder, String str) {
        FragmentManager fragmentManager = getFragmentManager();
        if (!isDialogAdded(str)) {
            dialogBuilder.build().show(fragmentManager, str);
        }
    }

    protected boolean isDialogAdded(String str) {
        Fragment fragmentFindFragmentByTag = getFragmentManager().findFragmentByTag(str);
        return fragmentFindFragmentByTag != null && fragmentFindFragmentByTag.isAdded();
    }
}
