package com.example.tscalibration;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import com.panasonic.sanyo.ce.bej.hard.Touchpanel;
import java.util.Timer;
import java.util.TimerTask;

public class TsCalibration extends Activity {
    Calib TP;
    Timer timer = null;
    final String TAG = "TsCalibration";
    View rootView = null;
    boolean TpEnd = false;
    int SYSTEM_UI_FLAG_IMMERSIVE = 2048;

    @Override
    @SuppressLint({"NewApi", "InlinedApi"})
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.TP = new Calib(getApplication());
        getWindow().addFlags(128);
        this.rootView = getWindow().getDecorView();
        this.rootView.setSystemUiVisibility(this.SYSTEM_UI_FLAG_IMMERSIVE | 2);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);
        setContentView(this.TP);
        Log.v("TsCalibration", "onCreate()  ");
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == 1 && (event.getKeyCode() == 24 || event.getKeyCode() == 25)) {
            finish();
        }
        return true;
    }

    private boolean TsCalibration_end() {
        this.TpEnd = true;
        if (this.TP.Index != 99) {
            return true;
        }
        if (Touchpanel.coefficient_set(2) < 0) {
            this.TP.Index = 98;
            this.TP.invalidate();
            return false;
        }
        finish();
        return true;
    }

    @Override
    @SuppressLint({"NewApi"})
    public void onResume() {
        super.onResume();
        this.TpEnd = false;
        int mode = 1;
        if (Touchpanel.coefficient_init() < 0) {
            this.TP.Index = 97;
            mode = 0;
        }
        Navigate();
        if (mode == 1) {
            Display display = getWindowManager().getDefaultDisplay();
            Point p = new Point();
            display.getRealSize(p);
            Log.v("TsCalibration", "Display Width:" + p.x + " Height:" + p.y);
            this.TP.dispX = p.x;
            this.TP.dispY = p.y;
            this.TP.Init();
        }
    }

    @Override
    public void onPause() {
        Log.v("TsCalibration", "onPause()!!");
        this.TpEnd = true;
        if (this.timer != null) {
            this.timer.cancel();
        }
        super.onPause();
        if (this.TP.Index == 99) {
            Touchpanel.coefficient_set(2);
        } else {
            Touchpanel.coefficient_cancele();
            Log.v("TsCalibration", "Calibraton Back up Set!!");
        }
    }

    @SuppressLint({"NewApi"})
    private void Navigate() {
        this.rootView.setSystemUiVisibility(this.SYSTEM_UI_FLAG_IMMERSIVE | 2);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.TP.x = (int) event.getX();
        this.TP.y = (int) event.getY();
        this.TP.act = event.getAction() & 255;
        if (this.TP.act == 1) {
            if (this.TP.Index == 97) {
                finish();
            } else if (this.TP.Index == 98) {
                if (Touchpanel.coefficient_init() == 0) {
                    this.TP.Init();
                } else {
                    this.TP.Index = 97;
                }
            } else if (this.TP.Index == 4) {
                Start_Timer();
            }
        }
        this.TP.invalidate();
        return true;
    }

    private void Start_Timer() {
        this.timer = new Timer(true);
        final Handler handler = new Handler();
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (TsCalibration.this.TP.Index == 99 && !TsCalibration.this.TpEnd) {
                            TsCalibration.this.timer.cancel();
                            TsCalibration.this.timer = null;
                            TsCalibration.this.TsCalibration_end();
                        }
                    }
                });
            }
        }, 100L, 500L);
    }
}
