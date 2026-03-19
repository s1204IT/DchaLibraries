package com.mediatek.camera.ui;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.utils.CameraUtil;
import java.util.LinkedList;

class OnScreenHintManager {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(OnScreenHintManager.class.getSimpleName());
    private final IApp mApp;
    private TextView mAutoHideHint;
    private TextView mBottomAlwaysHint;
    private Stack<IAppUi.HintInfo> mBottomStack;
    private IAppUi.HintInfo mCurrentAutoHideInfo;
    private IAppUi.HintInfo mCurrentBottomInfo;
    private IAppUi.HintInfo mCurrentTopInfo;
    private ViewGroup mHintRoot;
    private MainHandler mMainHandler;
    private final OnOrientationChangeListenerImpl mOrientationChangeListener;
    private TextView mTopAlwaysHint;
    private Stack<IAppUi.HintInfo> mTopStack;

    OnScreenHintManager(IApp iApp, ViewGroup viewGroup) {
        this.mApp = iApp;
        this.mHintRoot = (ViewGroup) viewGroup.findViewById(R.id.screen_hint_root);
        AnonymousClass1 anonymousClass1 = null;
        this.mOrientationChangeListener = new OnOrientationChangeListenerImpl(this, anonymousClass1);
        this.mApp.registerOnOrientationChangeListener(this.mOrientationChangeListener);
        this.mTopAlwaysHint = (TextView) this.mHintRoot.findViewById(R.id.top_always_hint);
        this.mAutoHideHint = (TextView) this.mHintRoot.findViewById(R.id.auto_hide_hint);
        this.mBottomAlwaysHint = (TextView) this.mHintRoot.findViewById(R.id.bottom_always_hint);
        this.mTopStack = new Stack<>(this, anonymousClass1);
        this.mBottomStack = new Stack<>(this, anonymousClass1);
        this.mMainHandler = new MainHandler(iApp.getActivity().getMainLooper());
    }

    void showScreenHint(IAppUi.HintInfo hintInfo) {
        if (hintInfo == null) {
            LogHelper.e(TAG, "showScreenHint info is null!");
        }
        LogHelper.d(TAG, "showScreenHint type = " + hintInfo.mType + " string = " + hintInfo.mHintText);
        if (hintInfo.mHintText == null) {
            return;
        }
        switch (AnonymousClass1.$SwitchMap$com$mediatek$camera$common$IAppUi$HintType[hintInfo.mType.ordinal()]) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                if (this.mCurrentTopInfo != null) {
                    this.mTopStack.push(this.mCurrentTopInfo);
                }
                this.mCurrentTopInfo = hintInfo;
                this.mTopAlwaysHint.setText(this.mCurrentTopInfo.mHintText);
                this.mTopAlwaysHint.setBackground(this.mCurrentTopInfo.mBackground);
                this.mTopAlwaysHint.setVisibility(0);
                break;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                this.mAutoHideHint.setText(hintInfo.mHintText);
                this.mAutoHideHint.setBackground(hintInfo.mBackground);
                this.mBottomAlwaysHint.setVisibility(8);
                this.mAutoHideHint.setVisibility(0);
                this.mCurrentAutoHideInfo = hintInfo;
                this.mMainHandler.removeMessages(0);
                this.mMainHandler.sendEmptyMessageDelayed(0, hintInfo.mDelayTime);
                break;
            case Camera2Proxy.TEMPLATE_RECORD:
                if (this.mCurrentBottomInfo != null) {
                    this.mBottomStack.push(this.mCurrentBottomInfo);
                }
                this.mCurrentBottomInfo = hintInfo;
                this.mBottomAlwaysHint.setText(this.mCurrentBottomInfo.mHintText);
                this.mBottomAlwaysHint.setBackground(this.mCurrentBottomInfo.mBackground);
                if (this.mAutoHideHint.getVisibility() != 0) {
                    this.mBottomAlwaysHint.setVisibility(0);
                }
                break;
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$mediatek$camera$common$IAppUi$HintType = new int[IAppUi.HintType.values().length];

        static {
            try {
                $SwitchMap$com$mediatek$camera$common$IAppUi$HintType[IAppUi.HintType.TYPE_ALWAYS_TOP.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$IAppUi$HintType[IAppUi.HintType.TYPE_AUTO_HIDE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$IAppUi$HintType[IAppUi.HintType.TYPE_ALWAYS_BOTTOM.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    void hideScreenHint(IAppUi.HintInfo hintInfo) {
        if (hintInfo == null) {
            LogHelper.e(TAG, "hideScreenHint info is null!");
        }
        LogHelper.d(TAG, "hideScreenHint type = " + hintInfo.mType + " string = " + hintInfo.mHintText);
        switch (AnonymousClass1.$SwitchMap$com$mediatek$camera$common$IAppUi$HintType[hintInfo.mType.ordinal()]) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                if (hintInfo == this.mCurrentTopInfo) {
                    if (this.mTopStack.empty()) {
                        this.mCurrentTopInfo = null;
                        this.mTopAlwaysHint.setVisibility(8);
                    } else {
                        this.mCurrentTopInfo = this.mTopStack.pop();
                        this.mTopAlwaysHint.setText(this.mCurrentTopInfo.mHintText);
                        this.mTopAlwaysHint.setBackground(this.mCurrentTopInfo.mBackground);
                        this.mTopAlwaysHint.setVisibility(0);
                    }
                } else if (!this.mTopStack.empty()) {
                    this.mTopStack.remove(hintInfo);
                }
                break;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                if (hintInfo == this.mCurrentAutoHideInfo) {
                    this.mMainHandler.removeMessages(0);
                    this.mAutoHideHint.setVisibility(8);
                    if (this.mCurrentBottomInfo != null) {
                        this.mBottomAlwaysHint.setVisibility(0);
                    }
                }
                break;
            case Camera2Proxy.TEMPLATE_RECORD:
                if (hintInfo == this.mCurrentBottomInfo) {
                    if (this.mBottomStack.empty()) {
                        this.mCurrentBottomInfo = null;
                        this.mBottomAlwaysHint.setVisibility(8);
                    } else {
                        this.mCurrentBottomInfo = this.mBottomStack.pop();
                        this.mBottomAlwaysHint.setText(this.mCurrentBottomInfo.mHintText);
                        this.mBottomAlwaysHint.setBackground(this.mCurrentBottomInfo.mBackground);
                        this.mBottomAlwaysHint.setVisibility(0);
                    }
                } else if (!this.mBottomStack.empty()) {
                    this.mBottomStack.remove(hintInfo);
                }
                break;
        }
    }

    void setVisibility(int i) {
        if (this.mHintRoot != null) {
            this.mHintRoot.setVisibility(i);
        }
    }

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                OnScreenHintManager.this.mAutoHideHint.setVisibility(8);
                if (OnScreenHintManager.this.mCurrentBottomInfo != null) {
                    OnScreenHintManager.this.mBottomAlwaysHint.setVisibility(0);
                }
            }
        }
    }

    private class OnOrientationChangeListenerImpl implements IApp.OnOrientationChangeListener {
        private OnOrientationChangeListenerImpl() {
        }

        OnOrientationChangeListenerImpl(OnScreenHintManager onScreenHintManager, AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override
        public void onOrientationChanged(int i) {
            int i2;
            View viewFindViewById = OnScreenHintManager.this.mHintRoot.findViewById(R.id.hint_root);
            if (CameraUtil.getDisplayRotation(OnScreenHintManager.this.mApp.getActivity()) == 270) {
                i2 = (i + 180) % 360;
            } else {
                i2 = i;
            }
            if (i2 == 0) {
                viewFindViewById.setPadding(dpToPixel(11), dpToPixel(50), dpToPixel(11), 0);
            } else if (i2 == 90) {
                viewFindViewById.setPadding(dpToPixel(180), dpToPixel(10), dpToPixel(180), 0);
            } else if (i2 != 180) {
                if (i2 == 270) {
                }
            } else {
                viewFindViewById.setPadding(dpToPixel(11), dpToPixel(180), dpToPixel(11), 0);
            }
            if (OnScreenHintManager.this.mHintRoot != null) {
                CameraUtil.rotateRotateLayoutChildView(OnScreenHintManager.this.mApp.getActivity(), OnScreenHintManager.this.mHintRoot, i, true);
            }
        }

        private int dpToPixel(int i) {
            return (int) ((i * OnScreenHintManager.this.mApp.getActivity().getResources().getDisplayMetrics().density) + 0.5f);
        }
    }

    private class Stack<T> {
        private LinkedList<T> storage;

        private Stack() {
            this.storage = new LinkedList<>();
        }

        Stack(OnScreenHintManager onScreenHintManager, AnonymousClass1 anonymousClass1) {
            this();
        }

        public void push(T t) {
            this.storage.addFirst(t);
        }

        public T pop() {
            return this.storage.removeFirst();
        }

        public boolean empty() {
            return this.storage.isEmpty();
        }

        public boolean remove(T t) {
            return this.storage.remove(t);
        }

        public String toString() {
            return this.storage.toString();
        }
    }
}
