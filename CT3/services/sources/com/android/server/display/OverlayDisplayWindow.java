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
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import com.android.internal.util.DumpUtils;
import com.android.server.pm.PackageManagerService;
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
        public void onDisplayAdded(int displayId) {
        }

        @Override
        public void onDisplayChanged(int displayId) {
            if (displayId != OverlayDisplayWindow.this.mDefaultDisplay.getDisplayId()) {
                return;
            }
            if (OverlayDisplayWindow.this.updateDefaultDisplayInfo()) {
                OverlayDisplayWindow.this.relayout();
                OverlayDisplayWindow.this.mListener.onStateChanged(OverlayDisplayWindow.this.mDefaultDisplayInfo.state);
            } else {
                OverlayDisplayWindow.this.dismiss();
            }
        }

        @Override
        public void onDisplayRemoved(int displayId) {
            if (displayId != OverlayDisplayWindow.this.mDefaultDisplay.getDisplayId()) {
                return;
            }
            OverlayDisplayWindow.this.dismiss();
        }
    };
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            OverlayDisplayWindow.this.mListener.onWindowCreated(surfaceTexture, OverlayDisplayWindow.this.mDefaultDisplayInfo.getMode().getRefreshRate(), OverlayDisplayWindow.this.mDefaultDisplayInfo.presentationDeadlineNanos, OverlayDisplayWindow.this.mDefaultDisplayInfo.state);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            OverlayDisplayWindow.this.mListener.onWindowDestroyed();
            return true;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }
    };
    private final View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            float oldX = event.getX();
            float oldY = event.getY();
            event.setLocation(event.getRawX(), event.getRawY());
            OverlayDisplayWindow.this.mGestureDetector.onTouchEvent(event);
            OverlayDisplayWindow.this.mScaleGestureDetector.onTouchEvent(event);
            switch (event.getActionMasked()) {
                case 1:
                case 3:
                    OverlayDisplayWindow.this.saveWindowParams();
                    break;
            }
            event.setLocation(oldX, oldY);
            return true;
        }
    };
    private final GestureDetector.OnGestureListener mOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            OverlayDisplayWindow.this.mLiveTranslationX -= distanceX;
            OverlayDisplayWindow.this.mLiveTranslationY -= distanceY;
            OverlayDisplayWindow.this.relayout();
            return true;
        }
    };
    private final ScaleGestureDetector.OnScaleGestureListener mOnScaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            OverlayDisplayWindow.this.mLiveScale *= detector.getScaleFactor();
            OverlayDisplayWindow.this.relayout();
            return true;
        }
    };

    public interface Listener {
        void onStateChanged(int i);

        void onWindowCreated(SurfaceTexture surfaceTexture, float f, long j, int i);

        void onWindowDestroyed();
    }

    public OverlayDisplayWindow(Context context, String name, int width, int height, int densityDpi, int gravity, boolean secure, Listener listener) {
        this.mContext = context;
        this.mName = name;
        this.mGravity = gravity;
        this.mSecure = secure;
        this.mListener = listener;
        this.mDisplayManager = (DisplayManager) context.getSystemService("display");
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mDefaultDisplay = this.mWindowManager.getDefaultDisplay();
        updateDefaultDisplayInfo();
        resize(width, height, densityDpi, false);
        createWindow();
    }

    public void show() {
        if (this.mWindowVisible) {
            return;
        }
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

    public void dismiss() {
        if (!this.mWindowVisible) {
            return;
        }
        this.mDisplayManager.unregisterDisplayListener(this.mDisplayListener);
        this.mWindowManager.removeView(this.mWindowContent);
        this.mWindowVisible = false;
    }

    public void resize(int width, int height, int densityDpi) {
        resize(width, height, densityDpi, true);
    }

    private void resize(int width, int height, int densityDpi, boolean doLayout) {
        this.mWidth = width;
        this.mHeight = height;
        this.mDensityDpi = densityDpi;
        this.mTitle = this.mContext.getResources().getString(R.string.indeterminate_progress_35, this.mName, Integer.valueOf(this.mWidth), Integer.valueOf(this.mHeight), Integer.valueOf(this.mDensityDpi));
        if (this.mSecure) {
            this.mTitle += this.mContext.getResources().getString(R.string.indeterminate_progress_36);
        }
        if (!doLayout) {
            return;
        }
        relayout();
    }

    public void relayout() {
        if (!this.mWindowVisible) {
            return;
        }
        updateWindowParams();
        this.mWindowManager.updateViewLayout(this.mWindowContent, this.mWindowParams);
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.println("mWindowVisible=" + this.mWindowVisible);
        pw.println("mWindowX=" + this.mWindowX);
        pw.println("mWindowY=" + this.mWindowY);
        pw.println("mWindowScale=" + this.mWindowScale);
        pw.println("mWindowParams=" + this.mWindowParams);
        if (this.mTextureView != null) {
            pw.println("mTextureView.getScaleX()=" + this.mTextureView.getScaleX());
            pw.println("mTextureView.getScaleY()=" + this.mTextureView.getScaleY());
        }
        pw.println("mLiveTranslationX=" + this.mLiveTranslationX);
        pw.println("mLiveTranslationY=" + this.mLiveTranslationY);
        pw.println("mLiveScale=" + this.mLiveScale);
    }

    private boolean updateDefaultDisplayInfo() {
        if (!this.mDefaultDisplay.getDisplayInfo(this.mDefaultDisplayInfo)) {
            Slog.w(TAG, "Cannot show overlay display because there is no default display upon which to show it.");
            return false;
        }
        return true;
    }

    private void createWindow() {
        LayoutInflater inflater = LayoutInflater.from(this.mContext);
        this.mWindowContent = inflater.inflate(R.layout.input_method_switcher_list_layout, (ViewGroup) null);
        this.mWindowContent.setOnTouchListener(this.mOnTouchListener);
        this.mTextureView = (TextureView) this.mWindowContent.findViewById(R.id.list_item);
        this.mTextureView.setPivotX(0.0f);
        this.mTextureView.setPivotY(0.0f);
        this.mTextureView.getLayoutParams().width = this.mWidth;
        this.mTextureView.getLayoutParams().height = this.mHeight;
        this.mTextureView.setOpaque(false);
        this.mTextureView.setSurfaceTextureListener(this.mSurfaceTextureListener);
        this.mTitleTextView = (TextView) this.mWindowContent.findViewById(R.id.list_menu_presenter);
        this.mTitleTextView.setText(this.mTitle);
        this.mWindowParams = new WindowManager.LayoutParams(2026);
        this.mWindowParams.flags |= 16778024;
        if (this.mSecure) {
            this.mWindowParams.flags |= PackageManagerService.DumpState.DUMP_PREFERRED_XML;
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
        float scale = Math.max(0.3f, Math.min(1.0f, Math.min(Math.min(this.mWindowScale * this.mLiveScale, this.mDefaultDisplayInfo.logicalWidth / this.mWidth), this.mDefaultDisplayInfo.logicalHeight / this.mHeight)));
        float offsetScale = ((scale / this.mWindowScale) - 1.0f) * 0.5f;
        int width = (int) (this.mWidth * scale);
        int height = (int) (this.mHeight * scale);
        int x = (int) ((this.mWindowX + this.mLiveTranslationX) - (width * offsetScale));
        int y = (int) ((this.mWindowY + this.mLiveTranslationY) - (height * offsetScale));
        int x2 = Math.max(0, Math.min(x, this.mDefaultDisplayInfo.logicalWidth - width));
        int y2 = Math.max(0, Math.min(y, this.mDefaultDisplayInfo.logicalHeight - height));
        this.mTextureView.setScaleX(scale);
        this.mTextureView.setScaleY(scale);
        this.mWindowParams.x = x2;
        this.mWindowParams.y = y2;
        this.mWindowParams.width = width;
        this.mWindowParams.height = height;
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
