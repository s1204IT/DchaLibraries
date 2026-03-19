package com.mediatek.camera.feature.setting.focus;

import android.app.Activity;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.mediatek.camera.R;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.widget.PreviewFrameLayout;
import com.mediatek.camera.feature.setting.focus.IFocusView;
import com.mediatek.camera.feature.setting.focus.MultiZoneAfView;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;

public class FocusViewController {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(FocusViewController.class.getSimpleName());
    private Activity mActivity;
    private IApp mApp;
    private PreviewFrameLayout mFeatureRootView;
    private FocusView mFocusView;
    private Handler mHandler;
    private MultiZoneAfView.MultiWindow[] mMultiAfWindows;
    private MultiZoneAfView mMultiZoneAfView;
    private RectF mPreviewRect = new RectF();

    public FocusViewController(IApp iApp, Focus focus) {
        this.mApp = iApp;
        this.mFeatureRootView = iApp.getAppUi().getPreviewFrameLayout();
        this.mActivity = iApp.getActivity();
        this.mHandler = new MainHandler(iApp.getActivity().getMainLooper());
        LogHelper.d(TAG, "[FocusViewController]");
    }

    public void showPassiveFocusAtCenter() {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (FocusViewController.this.mFocusView != null) {
                    FocusViewController.this.mFeatureRootView.unRegisterView(FocusViewController.this.mFocusView);
                    FocusViewController.this.mFeatureRootView.registerView(FocusViewController.this.mFocusView, 20);
                    FocusViewController.this.makeSureViewOnTree();
                    FocusViewController.this.setFocusLocation(FocusViewController.this.mFeatureRootView.getWidth() / 2, FocusViewController.this.mFeatureRootView.getHeight() / 2);
                    if (FocusViewController.this.hasMultiAFData(FocusViewController.this.mMultiAfWindows)) {
                        FocusViewController.this.handleMultiAfWindow(true);
                        FocusViewController.this.mFocusView.setFocusState(IFocusView.FocusViewState.STATE_PASSIVE_FOCUSING);
                    } else {
                        LogHelper.d(FocusViewController.TAG, "[showPassiveFocusAtCenter]");
                        FocusViewController.this.mFocusView.startPassiveFocus();
                        FocusViewController.this.mFocusView.centerFocusLocation();
                    }
                }
            }
        });
    }

    public void showActiveFocusAt(final int i, final int i2) {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (FocusViewController.this.mFocusView != null) {
                    FocusViewController.this.mFeatureRootView.unRegisterView(FocusViewController.this.mFocusView);
                    FocusViewController.this.mFeatureRootView.registerView(FocusViewController.this.mFocusView, 0);
                    LogHelper.d(FocusViewController.TAG, "[showActiveFocusAt] +");
                    FocusViewController.this.makeSureViewOnTree();
                    FocusViewController.this.setFocusLocation(i, i2);
                    FocusViewController.this.mFocusView.setFocusLocation(i, i2);
                    FocusViewController.this.mFocusView.startActiveFocus();
                }
            }
        });
    }

    public void stopFocusAnimations() {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (FocusViewController.this.mFocusView != null) {
                    FocusViewController.this.makeSureViewOnTree();
                    if (FocusViewController.this.mFocusView.isPassiveFocusRunning() && FocusViewController.this.hasMultiAFData(FocusViewController.this.mMultiAfWindows)) {
                        FocusViewController.this.handleMultiAfWindow(false);
                    }
                    FocusViewController.this.mFocusView.stopFocusAnimations();
                }
            }
        });
    }

    public void setAfData(byte[] bArr) {
        this.mMultiAfWindows = getMultiWindows(bArr);
    }

    protected void clearAfData() {
        this.mMultiAfWindows = null;
    }

    protected void clearFocusUi() {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (FocusViewController.this.mMultiZoneAfView != null) {
                    FocusViewController.this.mMultiZoneAfView.clear();
                }
                if (FocusViewController.this.mFocusView != null) {
                    FocusViewController.this.makeSureViewOnTree();
                    LogHelper.d(FocusViewController.TAG, "clearFocusUi");
                    FocusViewController.this.mFocusView.clearFocusUi();
                }
            }
        });
    }

    protected void highlightFocusView() {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (FocusViewController.this.mFocusView != null) {
                    FocusViewController.this.makeSureViewOnTree();
                    FocusViewController.this.mFocusView.highlightFocusView();
                }
            }
        });
    }

    protected void lowlightFocusView() {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (FocusViewController.this.mFocusView != null) {
                    FocusViewController.this.makeSureViewOnTree();
                    FocusViewController.this.mFocusView.lowlightFocusView();
                }
            }
        });
    }

    protected void addFocusView() {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FocusViewController.this.mFocusView = (FocusView) FocusViewController.this.mFeatureRootView.findViewById(R.id.focus_view);
                FocusViewController.this.mMultiZoneAfView = (MultiZoneAfView) FocusViewController.this.mFeatureRootView.findViewById(R.id.multi_focus_indicator);
                if (FocusViewController.this.mFocusView == null) {
                    FocusViewController.this.mFocusView = (FocusView) FocusViewController.this.mActivity.getLayoutInflater().inflate(R.layout.focus_view, (ViewGroup) FocusViewController.this.mFeatureRootView, false);
                    FocusViewController.this.mFeatureRootView.addView(FocusViewController.this.mFocusView);
                    LogHelper.i(FocusViewController.TAG, "[addFocusView] mFocusView = " + FocusViewController.this.mFocusView);
                }
                FocusViewController.this.mFocusView.setPreviewRect(FocusViewController.this.mPreviewRect);
                FocusViewController.this.addMultiZoneAfView();
                int gSensorOrientation = FocusViewController.this.mApp.getGSensorOrientation();
                if (gSensorOrientation == -1) {
                    LogHelper.d(FocusViewController.TAG, "[addFocusView] unknown orientation");
                } else {
                    CameraUtil.rotateViewOrientation(FocusViewController.this.mFocusView, gSensorOrientation + CameraUtil.getDisplayRotation(FocusViewController.this.mActivity), false);
                }
                FocusViewController.this.setFocusLocation(FocusViewController.this.mFeatureRootView.getWidth() / 2, FocusViewController.this.mFeatureRootView.getHeight() / 2);
            }
        });
    }

    protected void removeFocusView() {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (FocusViewController.this.mFocusView != null) {
                    FocusViewController.this.mFeatureRootView.unRegisterView(FocusViewController.this.mFocusView);
                    FocusViewController.this.mFeatureRootView.removeView(FocusViewController.this.mFocusView);
                    LogHelper.d(FocusViewController.TAG, "[removeFocusView]");
                    FocusViewController.this.mFocusView = null;
                }
                FocusViewController.this.removeMultiZoneAfView();
            }
        });
    }

    private void addMultiZoneAfView() {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (FocusViewController.this.mMultiZoneAfView == null) {
                    FocusViewController.this.mMultiZoneAfView = (MultiZoneAfView) FocusViewController.this.mActivity.getLayoutInflater().inflate(R.layout.multi_zone_af_view, (ViewGroup) FocusViewController.this.mFeatureRootView, false);
                    FocusViewController.this.mMultiZoneAfView.setPreviewSize((int) FocusViewController.this.mPreviewRect.width(), (int) FocusViewController.this.mPreviewRect.height());
                    FocusViewController.this.mFeatureRootView.addView(FocusViewController.this.mMultiZoneAfView);
                    FocusViewController.this.mFeatureRootView.registerView(FocusViewController.this.mMultiZoneAfView, 20);
                    LogHelper.d(FocusViewController.TAG, "[addMultiZoneAfView] mMultiZoneAfView = " + FocusViewController.this.mMultiZoneAfView);
                }
            }
        });
    }

    private void removeMultiZoneAfView() {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (FocusViewController.this.mMultiZoneAfView != null) {
                    FocusViewController.this.mFeatureRootView.unRegisterView(FocusViewController.this.mMultiZoneAfView);
                    FocusViewController.this.mFeatureRootView.removeView(FocusViewController.this.mMultiZoneAfView);
                    FocusViewController.this.mMultiZoneAfView = null;
                }
            }
        });
    }

    protected IFocusView.FocusViewState getFocusState() {
        if (this.mFocusView == null) {
            return IFocusView.FocusViewState.STATE_IDLE;
        }
        makeSureViewOnTree();
        return this.mFocusView.getFocusState();
    }

    protected void onPreviewChanged(final RectF rectF) {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FocusViewController.this.mPreviewRect = rectF;
                if (FocusViewController.this.mFocusView != null) {
                    FocusViewController.this.mFocusView.setPreviewRect(rectF);
                }
                if (FocusViewController.this.mMultiZoneAfView != null) {
                    FocusViewController.this.mMultiZoneAfView.setPreviewSize((int) rectF.width(), (int) rectF.height());
                }
            }
        });
    }

    protected boolean isReadyTodoFocus() {
        if (this.mFocusView == null) {
            LogHelper.w(TAG, "[isReadyTodoFocus]mFocusView is null");
            return false;
        }
        if (this.mFocusView.getWidth() == 0 || this.mFocusView.getHeight() == 0) {
            LogHelper.w(TAG, "[isReadyTodoFocus]width or height is 0");
            return false;
        }
        return true;
    }

    protected boolean isActiveFocusRunning() {
        if (this.mFocusView == null) {
            return false;
        }
        return this.mFocusView.isActiveFocusRunning();
    }

    protected void setOrientation(final int i) {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (FocusViewController.this.mFocusView != null) {
                    CameraUtil.rotateViewOrientation(FocusViewController.this.mFocusView, i + CameraUtil.getDisplayRotation(FocusViewController.this.mActivity), true);
                }
            }
        });
    }

    private void setFocusLocation(int i, int i2) {
        if (this.mFocusView == null) {
            return;
        }
        ((FrameLayout.LayoutParams) this.mFocusView.getLayoutParams()).setMargins(i - (this.mFocusView.getWidth() / 2), i2 - (this.mFocusView.getHeight() / 2), 0, 0);
        this.mFocusView.requestLayout();
    }

    private void makeSureViewOnTree() {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (FocusViewController.this.mFeatureRootView.findViewById(R.id.focus_view) == null && FocusViewController.this.mFocusView != null) {
                    LogHelper.w(FocusViewController.TAG, "[makeSureViewOnTree] mFocusView is not on view tree");
                    FocusViewController.this.mFeatureRootView.addView(FocusViewController.this.mFocusView);
                }
            }
        });
    }

    private void makeSureMultiZoneAfViewOnTree() {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (FocusViewController.this.mFeatureRootView.findViewById(R.id.multi_focus_indicator) == null && FocusViewController.this.mMultiZoneAfView != null) {
                    LogHelper.w(FocusViewController.TAG, "[makeSureMultiZoneAfViewOnTree]mMultiZoneAfView is not on view tree");
                    FocusViewController.this.mFeatureRootView.addView(FocusViewController.this.mMultiZoneAfView);
                }
            }
        });
    }

    private boolean hasMultiAFData(MultiZoneAfView.MultiWindow[] multiWindowArr) {
        boolean z = multiWindowArr != null && multiWindowArr.length > 0;
        LogHelper.d(TAG, "hasMultiAFData result = " + z);
        return z;
    }

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            LogHelper.d(FocusViewController.TAG, "[handleMessage] msg.what = " + message.what);
            if (message.what == 0 && FocusViewController.this.mMultiZoneAfView != null) {
                FocusViewController.this.mMultiZoneAfView.clear();
                FocusViewController.this.mMultiAfWindows = null;
            }
        }
    }

    private void handleMultiAfWindow(boolean z) {
        if (this.mMultiZoneAfView == null) {
            return;
        }
        this.mMultiZoneAfView.setVisibility(0);
        makeSureMultiZoneAfViewOnTree();
        int length = this.mMultiAfWindows.length;
        LogHelper.d(TAG, "[handleMultiAfWindow] length = " + length + ", moving = " + z);
        if (z) {
            for (int i = 0; i < length; i++) {
                this.mMultiAfWindows[i].mResult = 0;
            }
            this.mMultiZoneAfView.updateFocusWindows(this.mMultiAfWindows);
            this.mMultiZoneAfView.showWindows(true);
            return;
        }
        ArrayList arrayList = new ArrayList();
        for (int i2 = 0; i2 < length; i2++) {
            if (this.mMultiAfWindows[i2].mResult > 0) {
                arrayList.add(this.mMultiAfWindows[i2]);
            }
        }
        MultiZoneAfView.MultiWindow[] multiWindowArr = new MultiZoneAfView.MultiWindow[arrayList.size()];
        for (int i3 = 0; i3 < arrayList.size(); i3++) {
            multiWindowArr[i3] = (MultiZoneAfView.MultiWindow) arrayList.get(i3);
        }
        this.mMultiZoneAfView.updateFocusWindows(multiWindowArr);
        this.mMultiZoneAfView.showWindows(false);
        this.mHandler.sendEmptyMessageDelayed(0, 1000L);
    }

    private MultiZoneAfView.MultiWindow[] getMultiWindows(byte[] bArr) {
        LogHelper.d(TAG, "[getMultiWindows] original data size " + bArr.length);
        IntBuffer intBufferAsIntBuffer = ByteBuffer.wrap(bArr).order(ByteOrder.nativeOrder()).asIntBuffer();
        if (intBufferAsIntBuffer.limit() / 3 < 1) {
            LogHelper.w(TAG, "[getMultiWindows] intBuffer.limit() = " + intBufferAsIntBuffer.limit() + "the AF original data from framework is wrong.");
            return null;
        }
        int i = intBufferAsIntBuffer.get(0);
        int i2 = intBufferAsIntBuffer.get(1);
        int i3 = intBufferAsIntBuffer.get(2);
        IntBuffer intBufferAsIntBuffer2 = ByteBuffer.wrap(bArr, 12, bArr.length - 12).order(ByteOrder.nativeOrder()).asIntBuffer();
        int iLimit = intBufferAsIntBuffer2.limit();
        LogHelper.d(TAG, "[getMultiWindows] windowCount " + i + " ,single window (width,height ) from native (" + i2 + " ," + i3 + ")");
        if (iLimit != i * 3) {
            LogHelper.w(TAG, "[getMultiWindows] limit = " + iLimit + ", the window data number is not consistent with the common info");
        }
        MultiZoneAfView.MultiWindow[] multiWindowArr = new MultiZoneAfView.MultiWindow[i];
        for (int i4 = 0; i4 < iLimit; i4 += 3) {
            Rect rect = new Rect();
            int i5 = intBufferAsIntBuffer2.get(i4);
            int i6 = intBufferAsIntBuffer2.get(i4 + 1);
            int i7 = intBufferAsIntBuffer2.get(i4 + 2);
            int i8 = i2 / 2;
            rect.left = i5 - i8;
            int i9 = i3 / 2;
            rect.top = i6 - i9;
            rect.right = i5 + i8;
            rect.bottom = i6 + i9;
            multiWindowArr[i4 / 3] = new MultiZoneAfView.MultiWindow(rect, i7);
        }
        return multiWindowArr;
    }
}
