package com.android.camera;

import android.app.Activity;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import com.android.camera.debug.Log;
import com.android.camera2.R;

public class OnScreenHint {
    static final Log.Tag TAG = new Log.Tag("OnScreenHint");
    View mNextView;
    View mView;
    private final WindowManager mWM;
    private final WindowManager.LayoutParams mParams = new WindowManager.LayoutParams();
    private final Handler mHandler = new Handler();
    private final Runnable mShow = new Runnable() {
        @Override
        public void run() {
            OnScreenHint.this.handleShow();
        }
    };
    private final Runnable mHide = new Runnable() {
        @Override
        public void run() {
            OnScreenHint.this.handleHide();
        }
    };

    private OnScreenHint(Activity activity) {
        this.mWM = (WindowManager) activity.getSystemService("window");
        this.mParams.height = -2;
        this.mParams.width = -2;
        this.mParams.flags = 24;
        this.mParams.format = -3;
        this.mParams.windowAnimations = R.style.Animation_OnScreenHint;
        this.mParams.type = 1000;
        this.mParams.setTitle("OnScreenHint");
    }

    public void show() {
        if (this.mNextView == null) {
            throw new RuntimeException("View is not initialized");
        }
        this.mHandler.post(this.mShow);
    }

    public void cancel() {
        this.mHandler.post(this.mHide);
    }

    public static OnScreenHint makeText(Activity activity, CharSequence text) {
        OnScreenHint result = new OnScreenHint(activity);
        LayoutInflater inflate = (LayoutInflater) activity.getSystemService("layout_inflater");
        View v = inflate.inflate(R.layout.on_screen_hint, (ViewGroup) null);
        TextView tv = (TextView) v.findViewById(R.id.message);
        tv.setText(text);
        result.mNextView = v;
        return result;
    }

    public void setText(CharSequence s) {
        if (this.mNextView == null) {
            throw new RuntimeException("This OnScreenHint was not created with OnScreenHint.makeText()");
        }
        TextView tv = (TextView) this.mNextView.findViewById(R.id.message);
        if (tv == null) {
            throw new RuntimeException("This OnScreenHint was not created with OnScreenHint.makeText()");
        }
        tv.setText(s);
    }

    private synchronized void handleShow() {
        if (this.mView != this.mNextView) {
            handleHide();
            this.mView = this.mNextView;
            if (this.mView.getParent() != null) {
                this.mWM.removeView(this.mView);
            }
            this.mWM.addView(this.mView, this.mParams);
        }
    }

    private synchronized void handleHide() {
        if (this.mView != null) {
            if (this.mView.getParent() != null) {
                this.mWM.removeView(this.mView);
            }
            this.mView = null;
        }
    }
}
