package com.mediatek.settings.wfd;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.settings.R;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;

public final class WfdSinkSurfaceFragment extends DialogFragment implements SurfaceHolder.Callback, View.OnLongClickListener {
    private static final String TAG = WfdSinkSurfaceFragment.class.getSimpleName();
    private Activity mActivity;
    private Dialog mDialog;
    private WfdSinkExt mExt;
    private SurfaceView mSinkView;
    private WfdSinkLayout mSinkViewLayout;
    private boolean mSurfaceShowing = false;
    private boolean mGuideShowing = false;
    private boolean mCountdownShowing = false;
    private int mOrientationBak = -100;
    private boolean mLatinCharTest = false;
    private int mTestLatinChar = 160;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (icicle != null || !FeatureOption.MTK_WFD_SINK_SUPPORT) {
            Log.d("@M_" + TAG, "bundle is not null, recreate");
            dismissAllowingStateLoss();
            getActivity().finish();
        } else {
            this.mActivity = getActivity();
            this.mExt = new WfdSinkExt(this.mActivity);
            this.mExt.registerSinkFragment(this);
            this.mActivity.getActionBar().hide();
            setShowsDialog(true);
        }
    }

    @Override
    public void onStart() {
        Log.d("@M_" + TAG, "onStart");
        super.onStart();
        this.mExt.onStart();
    }

    @Override
    public void onStop() {
        Log.d("@M_" + TAG, "onStop");
        this.mExt.onStop();
        dismissAllowingStateLoss();
        this.mActivity.finish();
        super.onStop();
    }

    public void disconnect() {
        if (this.mSurfaceShowing) {
            this.mExt.disconnectWfdSinkConnection();
        }
        this.mSurfaceShowing = false;
        if (!this.mGuideShowing) {
            return;
        }
        removeWfdSinkGuide();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d("@M_" + TAG, "mDialog is null? " + (this.mDialog == null));
        this.mLatinCharTest = SystemProperties.get("wfd.uibc.latintest", "0").equals("1");
        if (this.mDialog == null) {
            this.mDialog = new FullScreenDialog(getActivity());
        }
        return this.mDialog;
    }

    public void addWfdSinkGuide() {
        if (this.mGuideShowing) {
            return;
        }
        ViewGroup guide = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.wfd_sink_guide, (ViewGroup) null);
        Button btn = (Button) guide.findViewById(R.id.wfd_sink_guide_ok_btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "ok button onClick");
                WfdSinkSurfaceFragment.this.removeWfdSinkGuide();
            }
        });
        TextView tv = (TextView) guide.findViewById(R.id.wfd_sink_guide_content);
        tv.setText(getActivity().getResources().getString(R.string.wfd_sink_guide_content, 3));
        this.mSinkViewLayout.addView(guide);
        this.mSinkViewLayout.setTag(R.string.wfd_sink_guide_content, guide);
        this.mSinkViewLayout.setCatchEvents(false);
        this.mGuideShowing = true;
    }

    public void removeWfdSinkGuide() {
        View guide;
        if (this.mGuideShowing && (guide = (View) this.mSinkViewLayout.getTag(R.string.wfd_sink_guide_content)) != null) {
            this.mSinkViewLayout.removeView(guide);
            this.mSinkViewLayout.setTag(R.string.wfd_sink_guide_content, null);
        }
        this.mSinkViewLayout.setCatchEvents(true);
        this.mGuideShowing = false;
    }

    public void addCountdownView(String countdownNum) {
        if (this.mCountdownShowing) {
            return;
        }
        ViewGroup countdownView = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.wfd_sink_countdown, (ViewGroup) null);
        TextView tv = (TextView) countdownView.findViewById(R.id.wfd_sink_countdown_num);
        tv.setText(countdownNum);
        this.mSinkViewLayout.addView(countdownView);
        this.mSinkViewLayout.setTag(R.id.wfd_sink_countdown_num, countdownView);
        this.mCountdownShowing = true;
    }

    public void removeCountDown() {
        View countdownView;
        if (this.mCountdownShowing && (countdownView = (View) this.mSinkViewLayout.getTag(R.id.wfd_sink_countdown_num)) != null) {
            this.mSinkViewLayout.removeView(countdownView);
            this.mSinkViewLayout.setTag(R.id.wfd_sink_countdown_num, null);
        }
        this.mCountdownShowing = false;
    }

    public void requestOrientation(boolean isPortrait) {
        this.mOrientationBak = this.mActivity.getRequestedOrientation();
        this.mActivity.setRequestedOrientation(isPortrait ? 1 : 0);
    }

    public void restoreOrientation() {
        if (this.mOrientationBak == -100) {
            return;
        }
        this.mActivity.setRequestedOrientation(this.mOrientationBak);
    }

    private class FullScreenDialog extends Dialog {
        private Activity mActivity;
        private int mSystemUiBak;

        public FullScreenDialog(Activity activity) {
            super(activity, android.R.style.Theme.Translucent.NoTitleBar.Fullscreen);
            this.mActivity = activity;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "dialog onCreate");
            ViewGroup.LayoutParams viewParams = new ViewGroup.LayoutParams(-1, -1);
            WfdSinkSurfaceFragment.this.mSinkViewLayout = WfdSinkSurfaceFragment.this.new WfdSinkLayout(this.mActivity);
            WfdSinkSurfaceFragment.this.mSinkViewLayout.setFocusableInTouchMode(true);
            setContentView(WfdSinkSurfaceFragment.this.mSinkViewLayout);
            WfdSinkSurfaceFragment.this.mSinkView = new SurfaceView(this.mActivity);
            WfdSinkSurfaceFragment.this.mSinkView.setFocusableInTouchMode(false);
            WfdSinkSurfaceFragment.this.mSinkView.setFocusable(false);
            WfdSinkSurfaceFragment.this.mSinkViewLayout.addView(WfdSinkSurfaceFragment.this.mSinkView, viewParams);
        }

        @Override
        protected void onStart() {
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "dialog onStart");
            super.onStart();
            this.mSystemUiBak = WfdSinkSurfaceFragment.this.mSinkViewLayout.getSystemUiVisibility();
            WfdSinkSurfaceFragment.this.mSinkViewLayout.setOnFocusGetCallback(new Runnable() {
                @Override
                public void run() {
                    WfdSinkSurfaceFragment.this.requestFullScreen(FullScreenDialog.this.mSystemUiBak);
                }
            });
            WfdSinkSurfaceFragment.this.mSinkViewLayout.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int i) {
                    Log.i("@M_" + WfdSinkSurfaceFragment.TAG, "onSystemUiVisibilityChange: " + i);
                    if (i == 0) {
                        WfdSinkSurfaceFragment.this.mSinkViewLayout.setFullScreenFlag(false);
                        if (!WfdSinkSurfaceFragment.this.mSinkViewLayout.mHasFocus) {
                            return;
                        }
                        WfdSinkSurfaceFragment.this.requestFullScreen(FullScreenDialog.this.mSystemUiBak);
                        return;
                    }
                    WfdSinkSurfaceFragment.this.mSinkViewLayout.setFullScreenFlag(true);
                }
            });
            WfdSinkSurfaceFragment.this.requestFullScreen(this.mSystemUiBak);
            this.mActivity.getWindow().addFlags(128);
            WfdSinkSurfaceFragment.this.mSinkView.getHolder().addCallback(WfdSinkSurfaceFragment.this);
        }

        @Override
        protected void onStop() {
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "dialog onStop");
            WfdSinkSurfaceFragment.this.mSinkViewLayout.setSystemUiVisibility(this.mSystemUiBak);
            this.mActivity.getWindow().clearFlags(128);
            WfdSinkSurfaceFragment.this.mSinkView.getHolder().removeCallback(WfdSinkSurfaceFragment.this);
            WfdSinkSurfaceFragment.this.restoreOrientation();
            super.onStop();
        }

        @Override
        public void dismiss() {
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "dialog dismiss");
            WfdSinkSurfaceFragment.this.disconnect();
            this.mActivity.finish();
            super.dismiss();
        }

        @Override
        public void onBackPressed() {
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "dialog onBackPressed");
            if (WfdSinkSurfaceFragment.this.mGuideShowing) {
                WfdSinkSurfaceFragment.this.removeWfdSinkGuide();
            } else {
                WfdSinkSurfaceFragment.this.disconnect();
                super.onBackPressed();
            }
        }
    }

    public void requestFullScreen(int systemUi) {
        if (Build.VERSION.SDK_INT >= 14) {
            systemUi |= 2;
        }
        if (Build.VERSION.SDK_INT >= 16) {
            systemUi |= 4;
        }
        if (Build.VERSION.SDK_INT >= 18) {
            systemUi |= 16781312;
        }
        final int newUiOptions = systemUi;
        this.mSinkViewLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "request full screen: " + Integer.toHexString(newUiOptions));
                WfdSinkSurfaceFragment.this.mSinkViewLayout.setSystemUiVisibility(newUiOptions);
            }
        }, 500L);
    }

    private class WfdSinkLayout extends FrameLayout {
        private boolean mCatchEvents;
        private CountDown mCountDown;
        private Runnable mFocusGetCallback;
        private boolean mFullScreenFlag;
        private boolean mHasFocus;
        private boolean mHasPerformedLongPress;
        private float mInitX;
        private float mInitY;
        private int mTouchSlop;

        public WfdSinkLayout(Context context) {
            super(context);
            this.mHasPerformedLongPress = false;
            this.mCatchEvents = true;
            this.mFullScreenFlag = false;
            this.mHasFocus = false;
            this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (!this.mCatchEvents) {
                return false;
            }
            int action = ev.getAction();
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "onTouchEvent action=" + action);
            switch (action & 255) {
                case DefaultWfcSettingsExt.RESUME:
                    if (FeatureOption.MTK_WFD_SINK_UIBC_SUPPORT) {
                        StringBuilder eventDesc = new StringBuilder();
                        eventDesc.append(String.valueOf(0)).append(",");
                        eventDesc.append(getTouchEventDesc(ev));
                        sendUibcInputEvent(eventDesc.toString());
                    }
                    this.mInitX = ev.getX();
                    this.mInitY = ev.getY();
                    this.mHasPerformedLongPress = false;
                    checkForLongClick(0);
                    return true;
                case DefaultWfcSettingsExt.PAUSE:
                    if (FeatureOption.MTK_WFD_SINK_UIBC_SUPPORT) {
                        StringBuilder eventDesc2 = new StringBuilder();
                        eventDesc2.append(String.valueOf(1)).append(",");
                        eventDesc2.append(getTouchEventDesc(ev));
                        sendUibcInputEvent(eventDesc2.toString());
                    }
                    removePendingCallback();
                    return true;
                case DefaultWfcSettingsExt.CREATE:
                    if (FeatureOption.MTK_WFD_SINK_UIBC_SUPPORT) {
                        StringBuilder eventDesc3 = new StringBuilder();
                        eventDesc3.append(String.valueOf(2)).append(",");
                        eventDesc3.append(getTouchEventDesc(ev));
                        sendUibcInputEvent(eventDesc3.toString());
                    }
                    if (Math.hypot(ev.getX() - this.mInitX, ev.getY() - this.mInitY) > this.mTouchSlop) {
                        removePendingCallback();
                    }
                    return true;
                case DefaultWfcSettingsExt.DESTROY:
                    removePendingCallback();
                    return true;
                default:
                    return true;
            }
        }

        @Override
        public boolean onGenericMotionEvent(MotionEvent event) {
            if (!this.mCatchEvents) {
                return false;
            }
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "onGenericMotionEvent event.getSource()=" + event.getSource());
            if (FeatureOption.MTK_WFD_SINK_UIBC_SUPPORT && event.getSource() == 8194) {
                switch (event.getAction()) {
                    case 7:
                        StringBuilder eventDesc = new StringBuilder();
                        eventDesc.append(String.valueOf(2)).append(",");
                        eventDesc.append(getTouchEventDesc(event));
                        sendUibcInputEvent(eventDesc.toString());
                        break;
                }
            }
            return true;
        }

        @Override
        public boolean onKeyPreIme(int keyCode, KeyEvent event) {
            if (!this.mCatchEvents || !this.mFullScreenFlag) {
                return false;
            }
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "onKeyPreIme keyCode=" + keyCode + ", action=" + event.getAction());
            if (FeatureOption.MTK_WFD_SINK_UIBC_SUPPORT) {
                int asciiCode = event.getUnicodeChar();
                if (asciiCode == 0 || asciiCode < 32) {
                    Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "Can't find unicode for keyCode=" + keyCode);
                    asciiCode = KeyCodeConverter.keyCodeToAscii(keyCode);
                }
                boolean onKeyUp = event.getAction() == 1;
                if (WfdSinkSurfaceFragment.this.mLatinCharTest && keyCode == 131) {
                    Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "Latin Test Mode enabled");
                    asciiCode = WfdSinkSurfaceFragment.this.mTestLatinChar;
                    if (onKeyUp) {
                        if (WfdSinkSurfaceFragment.this.mTestLatinChar == 255) {
                            WfdSinkSurfaceFragment.this.mTestLatinChar = 160;
                        } else {
                            WfdSinkSurfaceFragment.this.mTestLatinChar++;
                        }
                    }
                }
                Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "onKeyPreIme asciiCode=" + asciiCode);
                if (asciiCode != 0) {
                    StringBuilder eventDesc = new StringBuilder();
                    eventDesc.append(String.valueOf(onKeyUp ? 4 : 3)).append(",").append(String.format("0x%04x", Integer.valueOf(asciiCode))).append(", 0x0000");
                    sendUibcInputEvent(eventDesc.toString());
                    return true;
                }
                Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "Can't find control for keyCode=" + keyCode);
            }
            return false;
        }

        @Override
        public void onWindowFocusChanged(boolean hasWindowFocus) {
            super.onWindowFocusChanged(hasWindowFocus);
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "onWindowFocusChanged: " + hasWindowFocus);
            this.mHasFocus = hasWindowFocus;
            if (!hasWindowFocus || this.mFocusGetCallback == null) {
                return;
            }
            this.mFocusGetCallback.run();
        }

        private String getTouchEventDesc(MotionEvent ev) {
            int pointerCount = ev.getPointerCount();
            StringBuilder eventDesc = new StringBuilder();
            eventDesc.append(String.valueOf(pointerCount)).append(",");
            for (int p = 0; p < pointerCount; p++) {
                eventDesc.append(String.valueOf(ev.getPointerId(p))).append(",").append(String.valueOf((int) (ev.getXPrecision() * ev.getX(p)))).append(",").append(String.valueOf((int) (ev.getYPrecision() * ev.getY(p)))).append(",");
            }
            String ret = eventDesc.toString();
            return ret.substring(0, ret.length() - 1);
        }

        private void sendUibcInputEvent(String eventDesc) {
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "sendUibcInputEvent: " + eventDesc);
            WfdSinkSurfaceFragment.this.mExt.sendUibcEvent(eventDesc);
        }

        private void checkForLongClick(int delayOffset) {
            this.mHasPerformedLongPress = false;
            if (this.mCountDown == null) {
                this.mCountDown = new CountDown();
            }
            this.mCountDown.rememberWindowAttachCount();
            postDelayed(this.mCountDown, (ViewConfiguration.getLongPressTimeout() + 1000) - delayOffset);
        }

        private void removePendingCallback() {
            Log.v("@M_" + WfdSinkSurfaceFragment.TAG, "removePendingCallback");
            if (this.mCountDown == null || this.mHasPerformedLongPress) {
                return;
            }
            removeCallbacks(this.mCountDown);
            WfdSinkSurfaceFragment.this.removeCountDown();
        }

        public void setCatchEvents(boolean catched) {
            this.mCatchEvents = catched;
        }

        public void setFullScreenFlag(boolean fullScreen) {
            this.mFullScreenFlag = fullScreen;
        }

        public void setOnFocusGetCallback(Runnable runnable) {
            this.mFocusGetCallback = runnable;
        }

        @Override
        protected void onDetachedFromWindow() {
            removePendingCallback();
            super.onDetachedFromWindow();
        }

        class CountDown implements Runnable {
            private int mCountDownNum;
            private int mOriginalWindowAttachCount;

            CountDown() {
            }

            @Override
            public void run() {
                ViewGroup countdownView;
                TextView tv;
                if (!WfdSinkSurfaceFragment.this.mCountdownShowing) {
                    this.mCountDownNum = 3;
                    WfdSinkSurfaceFragment.this.addCountdownView(this.mCountDownNum + "");
                } else {
                    this.mCountDownNum--;
                    if (this.mCountDownNum <= 0) {
                        if (WfdSinkLayout.this.mParent != null && this.mOriginalWindowAttachCount == WfdSinkLayout.this.getWindowAttachCount() && WfdSinkSurfaceFragment.this.onLongClick(WfdSinkSurfaceFragment.this.mSinkViewLayout)) {
                            WfdSinkLayout.this.mHasPerformedLongPress = true;
                            return;
                        }
                        return;
                    }
                    if (WfdSinkSurfaceFragment.this.mCountdownShowing && (countdownView = (ViewGroup) WfdSinkSurfaceFragment.this.mSinkViewLayout.getTag(R.id.wfd_sink_countdown_num)) != null && (tv = (TextView) countdownView.findViewById(R.id.wfd_sink_countdown_num)) != null) {
                        tv.setText(this.mCountDownNum + "");
                        tv.postInvalidate();
                    }
                }
                WfdSinkLayout.this.postDelayed(this, 1000L);
            }

            public void rememberWindowAttachCount() {
                this.mOriginalWindowAttachCount = WfdSinkLayout.this.getWindowAttachCount();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("@M_" + TAG, "surface changed: " + width + "x" + height);
        int systemUiVis = this.mSinkViewLayout.getSystemUiVisibility();
        if (!this.mSinkViewLayout.mHasFocus || (systemUiVis & 2) != 0) {
            return;
        }
        requestFullScreen(systemUiVis);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("@M_" + TAG, "surface created");
        if (!this.mSurfaceShowing) {
            this.mExt.setupWfdSinkConnection(holder.getSurface());
        }
        this.mSurfaceShowing = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("@M_" + TAG, "surface destroyed");
        disconnect();
    }

    @Override
    public boolean onLongClick(View v) {
        Log.d("@M_" + TAG, "onLongClick");
        dismissAllowingStateLoss();
        this.mActivity.finish();
        return true;
    }

    private static class KeyCodeConverter {
        private static final SparseIntArray KEYCODE_ASCII = new SparseIntArray();

        private KeyCodeConverter() {
        }

        static {
            populateKeycodeAscii();
        }

        private static void populateKeycodeAscii() {
            SparseIntArray codes = KEYCODE_ASCII;
            codes.put(57, 18);
            codes.put(58, 18);
            codes.put(111, 27);
            codes.put(59, 15);
            codes.put(60, 15);
            codes.put(123, 0);
            codes.put(122, 0);
            codes.put(113, 0);
            codes.put(114, 0);
            codes.put(115, 0);
            codes.put(67, 8);
            codes.put(93, 12);
            codes.put(66, 13);
            codes.put(112, 127);
            codes.put(61, 9);
        }

        public static int keyCodeToAscii(int keyCode) {
            int asciiCode = KEYCODE_ASCII.get(keyCode);
            return asciiCode;
        }
    }
}
