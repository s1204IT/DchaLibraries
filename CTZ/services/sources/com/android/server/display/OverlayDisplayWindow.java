package com.android.server.display;

import android.R;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.ThreadedRenderer;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import com.android.internal.util.DumpUtils;
import java.io.PrintWriter;

final class OverlayDisplayWindow implements DumpUtils.Dump {
    private static final boolean DEBUG = false;
    private static final String TAG = "OverlayDisplayWindow";
    private final Context mContext;
    private final Display mDefaultDisplay;
    private int mDensityDpi;
    private final DisplayManager mDisplayManager;
    private GestureDetector mGestureDetector;
    private final int mGravity;
    private int mHeight;
    private final Listener mListener;
    private float mLiveTranslationX;
    private float mLiveTranslationY;
    private final String mName;
    private ScaleGestureDetector mScaleGestureDetector;
    private final boolean mSecure;
    private TextureView mTextureView;
    private String mTitle;
    private TextView mTitleTextView;
    private int mWidth;
    private View mWindowContent;
    private final WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowParams;
    private float mWindowScale;
    private boolean mWindowVisible;
    private int mWindowX;
    private int mWindowY;
    private final float INITIAL_SCALE = 0.5f;
    private final float MIN_SCALE = 0.3f;
    private final float MAX_SCALE = 1.0f;
    private final float WINDOW_ALPHA = 0.8f;
    private final boolean DISABLE_MOVE_AND_RESIZE = false;
    private final DisplayInfo mDefaultDisplayInfo = new DisplayInfo();
    private float mLiveScale = 1.0f;
    private final DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int i) {
        }

        @Override
        public void onDisplayChanged(int i) {
            if (i == OverlayDisplayWindow.this.mDefaultDisplay.getDisplayId()) {
                if (OverlayDisplayWindow.this.updateDefaultDisplayInfo()) {
                    OverlayDisplayWindow.this.relayout();
                    OverlayDisplayWindow.this.mListener.onStateChanged(OverlayDisplayWindow.this.mDefaultDisplayInfo.state);
                } else {
                    OverlayDisplayWindow.this.dismiss();
                }
            }
        }

        @Override
        public void onDisplayRemoved(int i) {
            if (i == OverlayDisplayWindow.this.mDefaultDisplay.getDisplayId()) {
                OverlayDisplayWindow.this.dismiss();
            }
        }
    };
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
            OverlayDisplayWindow.this.mListener.onWindowCreated(surfaceTexture, OverlayDisplayWindow.this.mDefaultDisplayInfo.getMode().getRefreshRate(), OverlayDisplayWindow.this.mDefaultDisplayInfo.presentationDeadlineNanos, OverlayDisplayWindow.this.mDefaultDisplayInfo.state);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            OverlayDisplayWindow.this.mListener.onWindowDestroyed();
            return true;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }
    };
    private final View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            float x = motionEvent.getX();
            float y = motionEvent.getY();
            motionEvent.setLocation(motionEvent.getRawX(), motionEvent.getRawY());
            OverlayDisplayWindow.this.mGestureDetector.onTouchEvent(motionEvent);
            OverlayDisplayWindow.this.mScaleGestureDetector.onTouchEvent(motionEvent);
            int actionMasked = motionEvent.getActionMasked();
            if (actionMasked == 1 || actionMasked == 3) {
                OverlayDisplayWindow.this.saveWindowParams();
            }
            motionEvent.setLocation(x, y);
            return true;
        }
    };
    private final GestureDetector.OnGestureListener mOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            OverlayDisplayWindow.access$724(OverlayDisplayWindow.this, f);
            OverlayDisplayWindow.access$824(OverlayDisplayWindow.this, f2);
            OverlayDisplayWindow.this.relayout();
            return true;
        }
    };
    private final ScaleGestureDetector.OnScaleGestureListener mOnScaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            OverlayDisplayWindow.access$932(OverlayDisplayWindow.this, scaleGestureDetector.getScaleFactor());
            OverlayDisplayWindow.this.relayout();
            return true;
        }
    };

    public interface Listener {
        void onStateChanged(int i);

        void onWindowCreated(SurfaceTexture surfaceTexture, float f, long j, int i);

        void onWindowDestroyed();
    }

    static float access$724(OverlayDisplayWindow overlayDisplayWindow, float f) {
        float f2 = overlayDisplayWindow.mLiveTranslationX - f;
        overlayDisplayWindow.mLiveTranslationX = f2;
        return f2;
    }

    static float access$824(OverlayDisplayWindow overlayDisplayWindow, float f) {
        float f2 = overlayDisplayWindow.mLiveTranslationY - f;
        overlayDisplayWindow.mLiveTranslationY = f2;
        return f2;
    }

    static float access$932(OverlayDisplayWindow overlayDisplayWindow, float f) {
        float f2 = overlayDisplayWindow.mLiveScale * f;
        overlayDisplayWindow.mLiveScale = f2;
        return f2;
    }

    public OverlayDisplayWindow(Context context, String str, int i, int i2, int i3, int i4, boolean z, Listener listener) {
        ThreadedRenderer.disableVsync();
        this.mContext = context;
        this.mName = str;
        this.mGravity = i4;
        this.mSecure = z;
        this.mListener = listener;
        this.mDisplayManager = (DisplayManager) context.getSystemService("display");
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mDefaultDisplay = this.mWindowManager.getDefaultDisplay();
        updateDefaultDisplayInfo();
        resize(i, i2, i3, false);
        createWindow();
    }

    public void show() {
        if (!this.mWindowVisible) {
            this.mDisplayManager.registerDisplayListener(this.mDisplayListener, null);
            if (!updateDefaultDisplayInfo()) {
                this.mDisplayManager.unregisterDisplayListener(this.mDisplayListener);
                return;
            }
            clearLiveState();
            updateWindowParams();
            this.mWindowManager.addView(this.mWindowContent, this.mWindowParams);
            this.mWindowVisible = true;
        }
    }

    public void dismiss() {
        if (this.mWindowVisible) {
            this.mDisplayManager.unregisterDisplayListener(this.mDisplayListener);
            this.mWindowManager.removeView(this.mWindowContent);
            this.mWindowVisible = false;
        }
    }

    public void resize(int i, int i2, int i3) {
        resize(i, i2, i3, true);
    }

    private void resize(int i, int i2, int i3, boolean z) {
        this.mWidth = i;
        this.mHeight = i2;
        this.mDensityDpi = i3;
        this.mTitle = this.mContext.getResources().getString(R.string.bugreport_option_interactive_summary, this.mName, Integer.valueOf(this.mWidth), Integer.valueOf(this.mHeight), Integer.valueOf(this.mDensityDpi));
        if (this.mSecure) {
            this.mTitle += this.mContext.getResources().getString(R.string.bugreport_option_full_title);
        }
        if (z) {
            relayout();
        }
    }

    public void relayout() {
        if (this.mWindowVisible) {
            updateWindowParams();
            this.mWindowManager.updateViewLayout(this.mWindowContent, this.mWindowParams);
        }
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.println("mWindowVisible=" + this.mWindowVisible);
        printWriter.println("mWindowX=" + this.mWindowX);
        printWriter.println("mWindowY=" + this.mWindowY);
        printWriter.println("mWindowScale=" + this.mWindowScale);
        printWriter.println("mWindowParams=" + this.mWindowParams);
        if (this.mTextureView != null) {
            printWriter.println("mTextureView.getScaleX()=" + this.mTextureView.getScaleX());
            printWriter.println("mTextureView.getScaleY()=" + this.mTextureView.getScaleY());
        }
        printWriter.println("mLiveTranslationX=" + this.mLiveTranslationX);
        printWriter.println("mLiveTranslationY=" + this.mLiveTranslationY);
        printWriter.println("mLiveScale=" + this.mLiveScale);
    }

    private boolean updateDefaultDisplayInfo() {
        if (!this.mDefaultDisplay.getDisplayInfo(this.mDefaultDisplayInfo)) {
            Slog.w(TAG, "Cannot show overlay display because there is no default display upon which to show it.");
            return false;
        }
        return true;
    }

    private void createWindow() {
        this.mWindowContent = LayoutInflater.from(this.mContext).inflate(R.layout.list_menu_item_checkbox, (ViewGroup) null);
        this.mWindowContent.setOnTouchListener(this.mOnTouchListener);
        this.mTextureView = (TextureView) this.mWindowContent.findViewById(R.id.horizontal_double_arrow);
        this.mTextureView.setPivotX(0.0f);
        this.mTextureView.setPivotY(0.0f);
        this.mTextureView.getLayoutParams().width = this.mWidth;
        this.mTextureView.getLayoutParams().height = this.mHeight;
        this.mTextureView.setOpaque(false);
        this.mTextureView.setSurfaceTextureListener(this.mSurfaceTextureListener);
        this.mTitleTextView = (TextView) this.mWindowContent.findViewById(R.id.hour);
        this.mTitleTextView.setText(this.mTitle);
        this.mWindowParams = new WindowManager.LayoutParams(2026);
        this.mWindowParams.flags |= 16778024;
        if (this.mSecure) {
            this.mWindowParams.flags |= 8192;
        }
        this.mWindowParams.privateFlags |= 2;
        this.mWindowParams.alpha = 0.8f;
        this.mWindowParams.gravity = 51;
        this.mWindowParams.setTitle(this.mTitle);
        this.mGestureDetector = new GestureDetector(this.mContext, this.mOnGestureListener);
        this.mScaleGestureDetector = new ScaleGestureDetector(this.mContext, this.mOnScaleGestureListener);
        this.mWindowX = (this.mGravity & 3) == 3 ? 0 : this.mDefaultDisplayInfo.logicalWidth;
        this.mWindowY = (this.mGravity & 48) != 48 ? this.mDefaultDisplayInfo.logicalHeight : 0;
        this.mWindowScale = 0.5f;
    }

    private void updateWindowParams() {
        float fMax = Math.max(0.3f, Math.min(1.0f, Math.min(Math.min(this.mWindowScale * this.mLiveScale, this.mDefaultDisplayInfo.logicalWidth / this.mWidth), this.mDefaultDisplayInfo.logicalHeight / this.mHeight)));
        float f = ((fMax / this.mWindowScale) - 1.0f) * 0.5f;
        int i = (int) (this.mWidth * fMax);
        int i2 = (int) (this.mHeight * fMax);
        int i3 = (int) ((this.mWindowX + this.mLiveTranslationX) - (i * f));
        int i4 = (int) ((this.mWindowY + this.mLiveTranslationY) - (i2 * f));
        int iMax = Math.max(0, Math.min(i3, this.mDefaultDisplayInfo.logicalWidth - i));
        int iMax2 = Math.max(0, Math.min(i4, this.mDefaultDisplayInfo.logicalHeight - i2));
        this.mTextureView.setScaleX(fMax);
        this.mTextureView.setScaleY(fMax);
        this.mWindowParams.x = iMax;
        this.mWindowParams.y = iMax2;
        this.mWindowParams.width = i;
        this.mWindowParams.height = i2;
    }

    private void saveWindowParams() {
        this.mWindowX = this.mWindowParams.x;
        this.mWindowY = this.mWindowParams.y;
        this.mWindowScale = this.mTextureView.getScaleX();
        clearLiveState();
    }

    private void clearLiveState() {
        this.mLiveTranslationX = 0.0f;
        this.mLiveTranslationY = 0.0f;
        this.mLiveScale = 1.0f;
    }
}
