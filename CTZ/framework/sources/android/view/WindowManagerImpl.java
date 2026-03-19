package android.view;

import android.content.Context;
import android.graphics.Region;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.ViewGroup;
import android.view.WindowManager;
import com.android.internal.os.IResultReceiver;

public final class WindowManagerImpl implements WindowManager {
    private final Context mContext;
    private IBinder mDefaultToken;
    private final WindowManagerGlobal mGlobal;
    private final Window mParentWindow;

    public WindowManagerImpl(Context context) {
        this(context, null);
    }

    private WindowManagerImpl(Context context, Window window) {
        this.mGlobal = WindowManagerGlobal.getInstance();
        this.mContext = context;
        this.mParentWindow = window;
    }

    public WindowManagerImpl createLocalWindowManager(Window window) {
        return new WindowManagerImpl(this.mContext, window);
    }

    public WindowManagerImpl createPresentationWindowManager(Context context) {
        return new WindowManagerImpl(context, this.mParentWindow);
    }

    public void setDefaultToken(IBinder iBinder) {
        this.mDefaultToken = iBinder;
    }

    @Override
    public void addView(View view, ViewGroup.LayoutParams layoutParams) {
        applyDefaultToken(layoutParams);
        this.mGlobal.addView(view, layoutParams, this.mContext.getDisplay(), this.mParentWindow);
    }

    @Override
    public void updateViewLayout(View view, ViewGroup.LayoutParams layoutParams) {
        applyDefaultToken(layoutParams);
        this.mGlobal.updateViewLayout(view, layoutParams);
    }

    private void applyDefaultToken(ViewGroup.LayoutParams layoutParams) {
        if (this.mDefaultToken != null && this.mParentWindow == null) {
            if (!(layoutParams instanceof WindowManager.LayoutParams)) {
                throw new IllegalArgumentException("Params must be WindowManager.LayoutParams");
            }
            WindowManager.LayoutParams layoutParams2 = (WindowManager.LayoutParams) layoutParams;
            if (layoutParams2.token == null) {
                layoutParams2.token = this.mDefaultToken;
            }
        }
    }

    @Override
    public void removeView(View view) {
        this.mGlobal.removeView(view, false);
    }

    @Override
    public void removeViewImmediate(View view) {
        this.mGlobal.removeView(view, true);
    }

    @Override
    public void requestAppKeyboardShortcuts(final WindowManager.KeyboardShortcutsReceiver keyboardShortcutsReceiver, int i) {
        try {
            WindowManagerGlobal.getWindowManagerService().requestAppKeyboardShortcuts(new IResultReceiver.Stub() {
                @Override
                public void send(int i2, Bundle bundle) throws RemoteException {
                    keyboardShortcutsReceiver.onKeyboardShortcutsReceived(bundle.getParcelableArrayList(WindowManager.PARCEL_KEY_SHORTCUTS_ARRAY));
                }
            }, i);
        } catch (RemoteException e) {
        }
    }

    @Override
    public Display getDefaultDisplay() {
        return this.mContext.getDisplay();
    }

    @Override
    public Region getCurrentImeTouchRegion() {
        try {
            return WindowManagerGlobal.getWindowManagerService().getCurrentImeTouchRegion();
        } catch (RemoteException e) {
            return null;
        }
    }
}
