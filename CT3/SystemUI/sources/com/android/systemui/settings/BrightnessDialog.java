package com.android.systemui.settings;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.ImageView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;

public class BrightnessDialog extends Activity {
    private BrightnessController mBrightnessController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setGravity(48);
        window.clearFlags(2);
        window.requestFeature(1);
        setContentView(R.layout.quick_settings_brightness_dialog);
        ImageView icon = (ImageView) findViewById(R.id.brightness_icon);
        ToggleSlider slider = (ToggleSlider) findViewById(R.id.brightness_slider);
        this.mBrightnessController = new BrightnessController(this, icon, slider);
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.mBrightnessController.registerCallbacks();
        MetricsLogger.visible(this, 220);
    }

    @Override
    protected void onStop() {
        super.onStop();
        MetricsLogger.hidden(this, 220);
        this.mBrightnessController.unregisterCallbacks();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 25 || keyCode == 24 || keyCode == 164) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}
