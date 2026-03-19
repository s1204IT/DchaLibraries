package android.app;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerImpl;

public class Presentation extends Dialog {
    private static final int MSG_CANCEL = 1;
    private static final String TAG = "Presentation";
    private final Display mDisplay;
    private final DisplayManager.DisplayListener mDisplayListener;
    private final DisplayManager mDisplayManager;
    private final Handler mHandler;
    private final IBinder mToken;

    public Presentation(Context context, Display display) {
        this(context, display, 0);
    }

    public Presentation(Context context, Display display, int i) {
        super(createPresentationContext(context, display, i), i, false);
        this.mToken = new Binder();
        this.mDisplayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int i2) {
            }

            @Override
            public void onDisplayRemoved(int i2) {
                if (i2 == Presentation.this.mDisplay.getDisplayId()) {
                    Presentation.this.handleDisplayRemoved();
                }
            }

            @Override
            public void onDisplayChanged(int i2) {
                if (i2 == Presentation.this.mDisplay.getDisplayId()) {
                    Presentation.this.handleDisplayChanged();
                }
            }
        };
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 1) {
                    Presentation.this.cancel();
                }
            }
        };
        this.mDisplay = display;
        this.mDisplayManager = (DisplayManager) getContext().getSystemService(Context.DISPLAY_SERVICE);
        int i2 = (display.getFlags() & 4) != 0 ? WindowManager.LayoutParams.TYPE_PRIVATE_PRESENTATION : WindowManager.LayoutParams.TYPE_PRESENTATION;
        Window window = getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.token = this.mToken;
        window.setAttributes(attributes);
        window.setGravity(119);
        window.setType(i2);
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
            Log.i(TAG, "Presentation is being dismissed because the display metrics have changed since it was created.");
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
            Log.i(TAG, "Presentation is being dismissed because the display metrics have changed since it was created.");
            cancel();
        }
    }

    private boolean isConfigurationStillValid() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.mDisplay.getMetrics(displayMetrics);
        return displayMetrics.equalsPhysical(getResources().getDisplayMetrics());
    }

    private static Context createPresentationContext(Context context, Display display, int i) {
        if (context == null) {
            throw new IllegalArgumentException("outerContext must not be null");
        }
        if (display == null) {
            throw new IllegalArgumentException("display must not be null");
        }
        Context contextCreateDisplayContext = context.createDisplayContext(display);
        if (i == 0) {
            TypedValue typedValue = new TypedValue();
            contextCreateDisplayContext.getTheme().resolveAttribute(16843712, typedValue, true);
            i = typedValue.resourceId;
        }
        final WindowManagerImpl windowManagerImplCreatePresentationWindowManager = ((WindowManagerImpl) context.getSystemService(Context.WINDOW_SERVICE)).createPresentationWindowManager(contextCreateDisplayContext);
        return new ContextThemeWrapper(contextCreateDisplayContext, i) {
            @Override
            public Object getSystemService(String str) {
                if (Context.WINDOW_SERVICE.equals(str)) {
                    return windowManagerImplCreatePresentationWindowManager;
                }
                return super.getSystemService(str);
            }
        };
    }
}
