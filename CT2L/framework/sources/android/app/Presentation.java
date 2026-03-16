package android.app;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.WindowManagerImpl;

public class Presentation extends Dialog {
    private static final int MSG_CANCEL = 1;
    private static final String TAG = "Presentation";
    private final Display mDisplay;
    private final DisplayManager.DisplayListener mDisplayListener;
    private final DisplayManager mDisplayManager;
    private final Handler mHandler;

    public Presentation(Context outerContext, Display display) {
        this(outerContext, display, 0);
    }

    public Presentation(Context outerContext, Display display, int theme) {
        super(createPresentationContext(outerContext, display, theme), theme, false);
        this.mDisplayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {
            }

            @Override
            public void onDisplayRemoved(int displayId) {
                if (displayId == Presentation.this.mDisplay.getDisplayId()) {
                    Presentation.this.handleDisplayRemoved();
                }
            }

            @Override
            public void onDisplayChanged(int displayId) {
                if (displayId == Presentation.this.mDisplay.getDisplayId()) {
                    Presentation.this.handleDisplayChanged();
                }
            }
        };
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        Presentation.this.cancel();
                        break;
                }
            }
        };
        this.mDisplay = display;
        this.mDisplayManager = (DisplayManager) getContext().getSystemService(Context.DISPLAY_SERVICE);
        getWindow().setGravity(119);
        setCanceledOnTouchOutside(false);
    }

    public Display getDisplay() {
        return this.mDisplay;
    }

    public Resources getResources() {
        return getContext().getResources();
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.mDisplayManager.registerDisplayListener(this.mDisplayListener, this.mHandler);
        if (!isConfigurationStillValid()) {
            Log.i(TAG, "Presentation is being immediately dismissed because the display metrics have changed since it was created.");
            this.mHandler.sendEmptyMessage(1);
        }
    }

    @Override
    protected void onStop() {
        this.mDisplayManager.unregisterDisplayListener(this.mDisplayListener);
        super.onStop();
    }

    @Override
    public void show() {
        super.show();
    }

    public void onDisplayRemoved() {
    }

    public void onDisplayChanged() {
    }

    private void handleDisplayRemoved() {
        onDisplayRemoved();
        cancel();
    }

    private void handleDisplayChanged() {
        onDisplayChanged();
        if (!isConfigurationStillValid()) {
            cancel();
        }
    }

    private boolean isConfigurationStillValid() {
        DisplayMetrics dm = new DisplayMetrics();
        this.mDisplay.getMetrics(dm);
        return dm.equalsPhysical(getResources().getDisplayMetrics());
    }

    private static Context createPresentationContext(Context outerContext, Display display, int theme) {
        if (outerContext == null) {
            throw new IllegalArgumentException("outerContext must not be null");
        }
        if (display == null) {
            throw new IllegalArgumentException("display must not be null");
        }
        Context displayContext = outerContext.createDisplayContext(display);
        if (theme == 0) {
            TypedValue outValue = new TypedValue();
            displayContext.getTheme().resolveAttribute(16843712, outValue, true);
            theme = outValue.resourceId;
        }
        WindowManagerImpl outerWindowManager = (WindowManagerImpl) outerContext.getSystemService(Context.WINDOW_SERVICE);
        final WindowManagerImpl displayWindowManager = outerWindowManager.createPresentationWindowManager(display);
        return new ContextThemeWrapper(displayContext, theme) {
            @Override
            public Object getSystemService(String name) {
                return Context.WINDOW_SERVICE.equals(name) ? displayWindowManager : super.getSystemService(name);
            }
        };
    }
}
