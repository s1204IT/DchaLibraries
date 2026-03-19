package com.mediatek.camera.common.thermal;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.widget.RotateLayout;

public class WarningDialog {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(WarningDialog.class.getSimpleName());
    private Activity mActivity;
    private IApp mApp;
    private Button mDialogButton;
    private IApp.OnOrientationChangeListener mOnOrientationChangeListener = new IApp.OnOrientationChangeListener() {
        @Override
        public void onOrientationChanged(int i) {
            CameraUtil.rotateRotateLayoutChildView(WarningDialog.this.mActivity, WarningDialog.this.mRoot, i, true);
        }
    };
    private RotateLayout mRoot;
    private TextView mWarningDialogTime;

    public WarningDialog(IApp iApp) {
        this.mApp = iApp;
        this.mActivity = iApp.getActivity();
    }

    public void show() {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WarningDialog.this.initView();
                if (WarningDialog.this.mRoot.getVisibility() != 0) {
                    WarningDialog.this.mRoot.setVisibility(0);
                }
            }
        });
    }

    public boolean isShowing() {
        return this.mRoot != null && this.mRoot.getVisibility() == 0;
    }

    public void hide() {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (WarningDialog.this.mRoot.getVisibility() == 0) {
                    WarningDialog.this.mRoot.setVisibility(8);
                }
            }
        });
    }

    public void uninitView() {
        if (this.mRoot != null) {
            this.mApp.unregisterOnOrientationChangeListener(this.mOnOrientationChangeListener);
            this.mApp.getAppUi().getModeRootView().removeView(this.mRoot);
        }
    }

    public void setCountDownTime(String str) {
        if (this.mWarningDialogTime != null) {
            this.mWarningDialogTime.setText(str);
        }
    }

    private void initView() {
        if (this.mRoot == null) {
            int identifier = this.mActivity.getResources().getIdentifier("warning_dialog", "layout", this.mActivity.getPackageName());
            int identifier2 = this.mActivity.getResources().getIdentifier("alert_dialog_time", "id", this.mActivity.getPackageName());
            int identifier3 = this.mActivity.getResources().getIdentifier("alert_dialog_button", "id", this.mActivity.getPackageName());
            this.mRoot = (RotateLayout) this.mActivity.getLayoutInflater().inflate(identifier, (ViewGroup) null);
            this.mApp.getAppUi().getModeRootView().addView(this.mRoot);
            this.mWarningDialogTime = (TextView) this.mRoot.findViewById(identifier2);
            this.mWarningDialogTime.setText("30");
            this.mDialogButton = (Button) this.mRoot.findViewById(identifier3);
            this.mDialogButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    WarningDialog.this.hide();
                }
            });
            this.mApp.registerOnOrientationChangeListener(this.mOnOrientationChangeListener);
        }
    }
}
