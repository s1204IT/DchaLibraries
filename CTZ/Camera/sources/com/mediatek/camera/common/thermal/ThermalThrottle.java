package com.mediatek.camera.common.thermal;

import android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ThermalThrottle {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ThermalThrottle.class.getSimpleName());
    private Activity mActivity;
    private WarningDialog mAlertDialog;
    protected final Handler mHandler;
    private HandlerThread mHandlerThread;
    private boolean mIsShowing;
    private Resources mRes;
    private int mWaitingTime;
    private WorkerHandler mWorkerHandler;
    private int mThermalStatus = -1;
    private boolean mIsResumed = false;

    public ThermalThrottle(IApp iApp) {
        this.mIsShowing = false;
        this.mActivity = iApp.getActivity();
        this.mAlertDialog = new WarningDialog(iApp);
        this.mRes = this.mActivity.getResources();
        int identifier = this.mRes.getIdentifier("pref_thermal_dialog_title", "string", this.mActivity.getPackageName());
        int identifier2 = this.mRes.getIdentifier("pref_thermal_dialog_content_launch", "string", this.mActivity.getPackageName());
        if (getTemperStatus() == 2 || this.mThermalStatus == 1) {
            this.mIsShowing = true;
            showThermalDlg(this.mActivity, identifier, identifier2);
        }
        this.mHandler = new MainHandler(iApp.getActivity().getMainLooper());
        this.mHandlerThread = new HandlerThread("ThermalThrottle-thread");
        this.mHandlerThread.start();
        this.mWorkerHandler = new WorkerHandler(this.mHandlerThread.getLooper());
        this.mWorkerHandler.sendEmptyMessageDelayed(0, 5000L);
        this.mWaitingTime = 30;
    }

    public void resume() {
        LogHelper.d(TAG, "[resume]...");
        this.mIsResumed = true;
        this.mWaitingTime = 30;
        if (this.mWorkerHandler != null) {
            this.mWorkerHandler.removeMessages(0);
            this.mWorkerHandler.sendEmptyMessageDelayed(0, 5000L);
        }
    }

    public void pause() {
        LogHelper.d(TAG, "[pause]...");
        this.mIsResumed = false;
        if (this.mWorkerHandler != null) {
            this.mWorkerHandler.removeCallbacksAndMessages(null);
        }
        if (this.mHandler != null) {
            this.mHandler.removeCallbacksAndMessages(null);
        }
        if (this.mAlertDialog.isShowing()) {
            this.mAlertDialog.hide();
        }
        this.mAlertDialog.setCountDownTime(String.valueOf(30));
        this.mWaitingTime = 30;
    }

    public void destroy() {
        LogHelper.d(TAG, "[destroy]...");
        this.mAlertDialog.uninitView();
        if (this.mWorkerHandler != null) {
            this.mWorkerHandler.getLooper().quit();
        }
        if (this.mHandlerThread != null) {
            this.mHandlerThread.quit();
        }
    }

    private void updateCountDownTime(final Activity activity) {
        LogHelper.d(TAG, "[updateCountDownTime]mCountDown = " + this.mWaitingTime + ",mIsResumed = " + this.mIsResumed);
        if (this.mThermalStatus == 1) {
            if (this.mWaitingTime > 0) {
                this.mWaitingTime--;
                this.mAlertDialog.setCountDownTime(String.valueOf(this.mWaitingTime));
                if (this.mIsResumed) {
                    this.mHandler.sendEmptyMessageDelayed(1, 1000L);
                    return;
                }
                return;
            }
            if (this.mWaitingTime == 0) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (activity.isFinishing() || activity.isDestroyed()) {
                            LogHelper.d(ThermalThrottle.TAG, "[updateCountDownTime] don't need finish activity");
                        } else {
                            activity.finish();
                        }
                    }
                });
                return;
            }
            return;
        }
        if (this.mAlertDialog.isShowing()) {
            this.mAlertDialog.hide();
        }
        this.mAlertDialog.setCountDownTime(String.valueOf(30));
        this.mWaitingTime = 30;
    }

    class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                ThermalThrottle.this.updateCountDownTime(ThermalThrottle.this.mActivity);
            }
        }
    }

    private class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                if (ThermalThrottle.this.getTemperStatus() == 2) {
                    if (!ThermalThrottle.this.mActivity.isFinishing()) {
                        ThermalThrottle.this.mActivity.finish();
                        return;
                    }
                    return;
                }
                if (ThermalThrottle.this.mThermalStatus == 1 && !ThermalThrottle.this.mAlertDialog.isShowing() && !ThermalThrottle.this.mIsShowing) {
                    LogHelper.d(ThermalThrottle.TAG, "[handleMessage]WorkerHandler, mCountDown = " + ThermalThrottle.this.mWaitingTime);
                    if (ThermalThrottle.this.mWaitingTime == 30) {
                        ThermalThrottle.this.mAlertDialog.show();
                        ThermalThrottle.this.mHandler.removeMessages(1);
                        ThermalThrottle.this.mHandler.sendEmptyMessageDelayed(1, 1000L);
                    }
                }
                if (ThermalThrottle.this.mThermalStatus == 1) {
                    ThermalThrottle.this.mWorkerHandler.sendEmptyMessageDelayed(0, 1000L);
                } else {
                    ThermalThrottle.this.mWorkerHandler.sendEmptyMessageDelayed(0, 5000L);
                }
            }
        }
    }

    private void showThermalDlg(final Activity activity, int i, int i2) {
        new AlertDialog.Builder(activity).setCancelable(false).setIconAttribute(R.attr.alertDialogIcon).setTitle(i).setMessage(i2).setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i3) {
                activity.finish();
            }
        }).show();
    }

    private int getTemperStatus() throws Throwable {
        FileReader fileReader;
        BufferedReader bufferedReader;
        int iIntValue;
        int i = 0;
        try {
            try {
                fileReader = new FileReader("/proc/driver/cl_cam_status");
                bufferedReader = new BufferedReader(fileReader);
                iIntValue = Integer.valueOf(bufferedReader.readLine()).intValue();
            } catch (IOException e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            bufferedReader.close();
            fileReader.close();
            if (this.mThermalStatus != iIntValue) {
                LogHelper.i(TAG, "Camera Thermal status :" + iIntValue);
            }
            i = iIntValue;
        } catch (IOException e2) {
            e = e2;
            i = iIntValue;
            System.out.println(e.toString());
            if (this.mThermalStatus != i) {
                LogHelper.i(TAG, "Camera Thermal status :" + i);
            }
        } catch (Throwable th2) {
            th = th2;
            i = iIntValue;
            if (this.mThermalStatus != i) {
                LogHelper.i(TAG, "Camera Thermal status :" + i);
            }
            throw th;
        }
        this.mThermalStatus = i;
        return this.mThermalStatus;
    }
}
