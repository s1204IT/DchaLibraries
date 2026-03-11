package com.mediatek.settings.wfd;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.StatusBarManager;
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

public final class WfdSinkSurfaceFragment extends DialogFragment implements SurfaceHolder.Callback, View.OnLongClickListener {
    private static final String TAG = WfdSinkSurfaceFragment.class.getSimpleName();
    private Activity mActivity;
    private Dialog mDialog;
    private WfdSinkExt mExt;
    private SurfaceView mSinkView;
    private WfdSinkLayout mSinkViewLayout;
    private StatusBarManager mStatusBar;
    private boolean mSurfaceShowing = false;
    private boolean mGuideShowing = false;
    private boolean mCountdownShowing = false;
    private int mOrientationBak = -100;
    private boolean mLatinCharTest = false;
    private int mTestLatinChar = 160;

    static int access$1308(WfdSinkSurfaceFragment wfdSinkSurfaceFragment) {
        int i = wfdSinkSurfaceFragment.mTestLatinChar;
        wfdSinkSurfaceFragment.mTestLatinChar = i + 1;
        return i;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle != null || !FeatureOption.MTK_WFD_SINK_SUPPORT) {
            Log.d("@M_" + TAG, "bundle is not null, recreate");
            dismissAllowingStateLoss();
            getActivity().finish();
            return;
        }
        this.mActivity = getActivity();
        this.mExt = new WfdSinkExt(this.mActivity);
        this.mExt.registerSinkFragment(this);
        this.mActivity.getActionBar().hide();
        setShowsDialog(true);
    }

    @Override
    public void onStart() {
        Log.d("@M_" + TAG, "onStart");
        super.onStart();
        this.mExt.onStart();
        this.mStatusBar = (StatusBarManager) this.mActivity.getSystemService("statusbar");
        this.mStatusBar.disable(52887552);
    }

    @Override
    public void onStop() {
        Log.d("@M_" + TAG, "onStop");
        this.mExt.onStop();
        dismissAllowingStateLoss();
        if (this.mStatusBar != null) {
            this.mStatusBar.disable(0);
        }
        this.mActivity.finish();
        super.onStop();
    }

    public void disconnect() {
        if (this.mSurfaceShowing) {
            this.mExt.disconnectWfdSinkConnection();
        }
        this.mSurfaceShowing = false;
        if (this.mGuideShowing) {
            removeWfdSinkGuide();
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        String str = "@M_" + TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("mDialog is null? ");
        sb.append(this.mDialog == null);
        Log.d(str, sb.toString());
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
        ViewGroup viewGroup = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.wfd_sink_guide, (ViewGroup) null);
        ((Button) viewGroup.findViewById(R.id.wfd_sink_guide_ok_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "ok button onClick");
                WfdSinkSurfaceFragment.this.removeWfdSinkGuide();
            }
        });
        ((TextView) viewGroup.findViewById(R.id.wfd_sink_guide_content)).setText(getActivity().getResources().getString(R.string.wfd_sink_guide_content, 3));
        this.mSinkViewLayout.addView(viewGroup);
        this.mSinkViewLayout.setTag(R.string.wfd_sink_guide_content, viewGroup);
        this.mSinkViewLayout.setCatchEvents(false);
        this.mGuideShowing = true;
    }

    public void removeWfdSinkGuide() {
        View view;
        if (this.mGuideShowing && (view = (View) this.mSinkViewLayout.getTag(R.string.wfd_sink_guide_content)) != null) {
            this.mSinkViewLayout.removeView(view);
            this.mSinkViewLayout.setTag(R.string.wfd_sink_guide_content, null);
        }
        this.mSinkViewLayout.setCatchEvents(true);
        this.mGuideShowing = false;
    }

    public void addCountdownView(String str) {
        if (this.mCountdownShowing) {
            return;
        }
        ViewGroup viewGroup = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.wfd_sink_countdown, (ViewGroup) null);
        ((TextView) viewGroup.findViewById(R.id.wfd_sink_countdown_num)).setText(str);
        this.mSinkViewLayout.addView(viewGroup);
        this.mSinkViewLayout.setTag(R.id.wfd_sink_countdown_num, viewGroup);
        this.mCountdownShowing = true;
    }

    public void removeCountDown() {
        View view;
        if (this.mCountdownShowing && (view = (View) this.mSinkViewLayout.getTag(R.id.wfd_sink_countdown_num)) != null) {
            this.mSinkViewLayout.removeView(view);
            this.mSinkViewLayout.setTag(R.id.wfd_sink_countdown_num, null);
        }
        this.mCountdownShowing = false;
    }

    public void requestOrientation(boolean z) {
        this.mOrientationBak = this.mActivity.getRequestedOrientation();
        this.mActivity.setRequestedOrientation(z ? 1 : 0);
    }

    public void restoreOrientation() {
        if (this.mOrientationBak != -100) {
            this.mActivity.setRequestedOrientation(this.mOrientationBak);
        }
    }

    private class FullScreenDialog extends Dialog {
        private Activity mActivity;
        private int mSystemUiBak;

        public FullScreenDialog(Activity activity) {
            super(activity, android.R.style.Theme.Translucent.NoTitleBar.Fullscreen);
            this.mActivity = activity;
        }

        @Override
        protected void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "dialog onCreate");
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(-1, -1);
            WfdSinkSurfaceFragment.this.mSinkViewLayout = WfdSinkSurfaceFragment.this.new WfdSinkLayout(this.mActivity);
            WfdSinkSurfaceFragment.this.mSinkViewLayout.setFocusableInTouchMode(true);
            setContentView(WfdSinkSurfaceFragment.this.mSinkViewLayout);
            WfdSinkSurfaceFragment.this.mSinkView = new SurfaceView(this.mActivity);
            WfdSinkSurfaceFragment.this.mSinkView.setFocusableInTouchMode(false);
            WfdSinkSurfaceFragment.this.mSinkView.setFocusable(false);
            WfdSinkSurfaceFragment.this.mSinkViewLayout.addView(WfdSinkSurfaceFragment.this.mSinkView, layoutParams);
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
                        if (WfdSinkSurfaceFragment.this.mSinkViewLayout.mHasFocus) {
                            WfdSinkSurfaceFragment.this.requestFullScreen(FullScreenDialog.this.mSystemUiBak);
                            return;
                        }
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

    public void requestFullScreen(final int i) {
        if (Build.VERSION.SDK_INT >= 14) {
            i |= 2;
        }
        if (Build.VERSION.SDK_INT >= 16) {
            i |= 4;
        }
        if (Build.VERSION.SDK_INT >= 18) {
            i |= 16781312;
        }
        this.mSinkViewLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "request full screen: " + Integer.toHexString(i));
                WfdSinkSurfaceFragment.this.mSinkViewLayout.setSystemUiVisibility(i);
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
        public boolean onTouchEvent(MotionEvent motionEvent) {
            if (!this.mCatchEvents) {
                return false;
            }
            int action = motionEvent.getAction();
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "onTouchEvent action=" + action);
            switch (action & 255) {
                case 0:
                    if (FeatureOption.MTK_WFD_SINK_UIBC_SUPPORT) {
                        sendUibcInputEvent(String.valueOf(0) + "," + getTouchEventDesc(motionEvent));
                    }
                    this.mInitX = motionEvent.getX();
                    this.mInitY = motionEvent.getY();
                    this.mHasPerformedLongPress = false;
                    checkForLongClick(0);
                    return true;
                case 1:
                    if (FeatureOption.MTK_WFD_SINK_UIBC_SUPPORT) {
                        sendUibcInputEvent(String.valueOf(1) + "," + getTouchEventDesc(motionEvent));
                    }
                    removePendingCallback();
                    return true;
                case 2:
                    if (FeatureOption.MTK_WFD_SINK_UIBC_SUPPORT) {
                        sendUibcInputEvent(String.valueOf(2) + "," + getTouchEventDesc(motionEvent));
                    }
                    if (Math.hypot(motionEvent.getX() - this.mInitX, motionEvent.getY() - this.mInitY) > this.mTouchSlop) {
                        removePendingCallback();
                    }
                    return true;
                case 3:
                    removePendingCallback();
                    return true;
                default:
                    return true;
            }
        }

        @Override
        public boolean onGenericMotionEvent(MotionEvent motionEvent) {
            if (!this.mCatchEvents) {
                return false;
            }
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "onGenericMotionEvent event.getSource()=" + motionEvent.getSource());
            if (FeatureOption.MTK_WFD_SINK_UIBC_SUPPORT && motionEvent.getSource() == 8194) {
                switch (motionEvent.getAction()) {
                    case 7:
                        sendUibcInputEvent(String.valueOf(2) + "," + getTouchEventDesc(motionEvent));
                        break;
                }
                return true;
            }
            return true;
        }

        @Override
        public boolean onKeyPreIme(int i, KeyEvent keyEvent) {
            if (!this.mCatchEvents || !this.mFullScreenFlag) {
                return false;
            }
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "onKeyPreIme keyCode=" + i + ", action=" + keyEvent.getAction());
            if (FeatureOption.MTK_WFD_SINK_UIBC_SUPPORT) {
                int unicodeChar = keyEvent.getUnicodeChar();
                if (unicodeChar == 0 || unicodeChar < 32) {
                    Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "Can't find unicode for keyCode=" + i);
                    unicodeChar = KeyCodeConverter.keyCodeToAscii(i);
                }
                boolean z = keyEvent.getAction() == 1;
                if (WfdSinkSurfaceFragment.this.mLatinCharTest && i == 131) {
                    Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "Latin Test Mode enabled");
                    unicodeChar = WfdSinkSurfaceFragment.this.mTestLatinChar;
                    if (z) {
                        if (WfdSinkSurfaceFragment.this.mTestLatinChar == 255) {
                            WfdSinkSurfaceFragment.this.mTestLatinChar = 160;
                        } else {
                            WfdSinkSurfaceFragment.access$1308(WfdSinkSurfaceFragment.this);
                        }
                    }
                }
                Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "onKeyPreIme asciiCode=" + unicodeChar);
                if (unicodeChar == 0) {
                    Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "Can't find control for keyCode=" + i);
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.valueOf(z ? 4 : 3));
                    sb.append(",");
                    sb.append(String.format("0x%04x", Integer.valueOf(unicodeChar)));
                    sb.append(", 0x0000");
                    sendUibcInputEvent(sb.toString());
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onWindowFocusChanged(boolean z) {
            super.onWindowFocusChanged(z);
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "onWindowFocusChanged: " + z);
            this.mHasFocus = z;
            if (z && this.mFocusGetCallback != null) {
                this.mFocusGetCallback.run();
            }
        }

        private String getTouchEventDesc(MotionEvent motionEvent) {
            int pointerCount = motionEvent.getPointerCount();
            StringBuilder sb = new StringBuilder();
            sb.append(String.valueOf(pointerCount));
            sb.append(",");
            for (int i = 0; i < pointerCount; i++) {
                sb.append(String.valueOf(motionEvent.getPointerId(i)));
                sb.append(",");
                sb.append(String.valueOf((int) (motionEvent.getXPrecision() * motionEvent.getX(i))));
                sb.append(",");
                sb.append(String.valueOf((int) (motionEvent.getYPrecision() * motionEvent.getY(i))));
                sb.append(",");
            }
            return sb.toString().substring(0, r7.length() - 1);
        }

        private void sendUibcInputEvent(String str) {
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "sendUibcInputEvent: " + str);
            WfdSinkSurfaceFragment.this.mExt.sendUibcEvent(str);
        }

        private void checkForLongClick(int i) {
            this.mHasPerformedLongPress = false;
            if (this.mCountDown == null) {
                this.mCountDown = new CountDown();
            }
            this.mCountDown.rememberWindowAttachCount();
            postDelayed(this.mCountDown, (1000 + ViewConfiguration.getLongPressTimeout()) - i);
        }

        private void removePendingCallback() {
            Log.v("@M_" + WfdSinkSurfaceFragment.TAG, "removePendingCallback");
            if (this.mCountDown != null && !this.mHasPerformedLongPress) {
                removeCallbacks(this.mCountDown);
                WfdSinkSurfaceFragment.this.removeCountDown();
            }
        }

        public void setCatchEvents(boolean z) {
            this.mCatchEvents = z;
        }

        public void setFullScreenFlag(boolean z) {
            this.mFullScreenFlag = z;
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
                ViewGroup viewGroup;
                TextView textView;
                if (!WfdSinkSurfaceFragment.this.mCountdownShowing) {
                    this.mCountDownNum = 3;
                    WfdSinkSurfaceFragment.this.addCountdownView(this.mCountDownNum + "");
                } else {
                    this.mCountDownNum--;
                    if (this.mCountDownNum > 0) {
                        if (WfdSinkSurfaceFragment.this.mCountdownShowing && (viewGroup = (ViewGroup) WfdSinkSurfaceFragment.this.mSinkViewLayout.getTag(R.id.wfd_sink_countdown_num)) != null && (textView = (TextView) viewGroup.findViewById(R.id.wfd_sink_countdown_num)) != null) {
                            textView.setText(this.mCountDownNum + "");
                            textView.postInvalidate();
                        }
                    } else {
                        if (WfdSinkLayout.this.mParent != null && this.mOriginalWindowAttachCount == WfdSinkLayout.this.getWindowAttachCount() && WfdSinkSurfaceFragment.this.onLongClick(WfdSinkSurfaceFragment.this.mSinkViewLayout)) {
                            WfdSinkLayout.this.mHasPerformedLongPress = true;
                            return;
                        }
                        return;
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
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        Log.d("@M_" + TAG, "surface changed: " + i2 + "x" + i3);
        int systemUiVisibility = this.mSinkViewLayout.getSystemUiVisibility();
        if (this.mSinkViewLayout.mHasFocus && (systemUiVisibility & 2) == 0) {
            requestFullScreen(systemUiVisibility);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d("@M_" + TAG, "surface created");
        if (!this.mSurfaceShowing) {
            this.mExt.setupWfdSinkConnection(surfaceHolder.getSurface());
        }
        this.mSurfaceShowing = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d("@M_" + TAG, "surface destroyed");
        disconnect();
    }

    @Override
    public boolean onLongClick(View view) {
        Log.d("@M_" + TAG, "onLongClick");
        dismissAllowingStateLoss();
        this.mActivity.finish();
        return true;
    }

    private static class KeyCodeConverter {
        private static final SparseIntArray KEYCODE_ASCII = new SparseIntArray();

        static {
            populateKeycodeAscii();
        }

        private static void populateKeycodeAscii() {
            SparseIntArray sparseIntArray = KEYCODE_ASCII;
            sparseIntArray.put(57, 18);
            sparseIntArray.put(58, 18);
            sparseIntArray.put(111, 27);
            sparseIntArray.put(59, 15);
            sparseIntArray.put(60, 15);
            sparseIntArray.put(123, 0);
            sparseIntArray.put(122, 0);
            sparseIntArray.put(113, 0);
            sparseIntArray.put(114, 0);
            sparseIntArray.put(115, 0);
            sparseIntArray.put(67, 8);
            sparseIntArray.put(93, 12);
            sparseIntArray.put(66, 13);
            sparseIntArray.put(112, 127);
            sparseIntArray.put(61, 9);
        }

        public static int keyCodeToAscii(int i) {
            return KEYCODE_ASCII.get(i);
        }
    }
}
