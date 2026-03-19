package android.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import com.android.internal.app.WindowDecorActionBar;
import com.android.internal.policy.PhoneWindow;
import java.lang.ref.WeakReference;

public class Dialog implements DialogInterface, Window.Callback, KeyEvent.Callback, View.OnCreateContextMenuListener, Window.OnWindowDismissedCallback {
    private static final int CANCEL = 68;
    private static final String DIALOG_HIERARCHY_TAG = "android:dialogHierarchy";
    private static final String DIALOG_SHOWING_TAG = "android:dialogShowing";
    private static final int DISMISS = 67;
    private static final int SHOW = 69;
    private static final String TAG = "Dialog";
    private ActionBar mActionBar;
    private ActionMode mActionMode;
    private int mActionModeTypeStarting;
    private String mCancelAndDismissTaken;
    private Message mCancelMessage;
    protected boolean mCancelable;
    private boolean mCanceled;
    final Context mContext;
    private boolean mCreated;
    View mDecor;
    private final Runnable mDismissAction;
    private Message mDismissMessage;
    private final Handler mHandler;
    private final Handler mListenersHandler;
    private DialogInterface.OnKeyListener mOnKeyListener;
    private Activity mOwnerActivity;
    private SearchEvent mSearchEvent;
    private Message mShowMessage;
    private boolean mShowing;
    final Window mWindow;
    private final WindowManager mWindowManager;

    public Dialog(Context context) {
        this(context, 0, true);
    }

    public Dialog(Context context, int i) {
        this(context, i, true);
    }

    Dialog(Context context, int i, boolean z) {
        this.mCancelable = true;
        this.mCreated = false;
        this.mShowing = false;
        this.mCanceled = false;
        this.mHandler = new Handler();
        this.mActionModeTypeStarting = 0;
        this.mDismissAction = new Runnable() {
            @Override
            public final void run() {
                this.f$0.dismissDialog();
            }
        };
        if (z) {
            if (i == 0) {
                TypedValue typedValue = new TypedValue();
                context.getTheme().resolveAttribute(16843528, typedValue, true);
                i = typedValue.resourceId;
            }
            this.mContext = new ContextThemeWrapper(context, i);
        } else {
            this.mContext = context;
        }
        this.mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        PhoneWindow phoneWindow = new PhoneWindow(this.mContext);
        this.mWindow = phoneWindow;
        phoneWindow.setCallback(this);
        phoneWindow.setOnWindowDismissedCallback(this);
        phoneWindow.setOnWindowSwipeDismissedCallback(new Window.OnWindowSwipeDismissedCallback() {
            @Override
            public final void onWindowSwipeDismissed() {
                Dialog.lambda$new$0(this.f$0);
            }
        });
        phoneWindow.setWindowManager(this.mWindowManager, null, null);
        phoneWindow.setGravity(17);
        this.mListenersHandler = new ListenersHandler(this);
    }

    public static void lambda$new$0(Dialog dialog) {
        if (dialog.mCancelable) {
            dialog.cancel();
        }
    }

    @Deprecated
    protected Dialog(Context context, boolean z, Message message) {
        this(context);
        this.mCancelable = z;
        updateWindowForCancelable();
        this.mCancelMessage = message;
    }

    protected Dialog(Context context, boolean z, DialogInterface.OnCancelListener onCancelListener) {
        this(context);
        this.mCancelable = z;
        updateWindowForCancelable();
        setOnCancelListener(onCancelListener);
    }

    public final Context getContext() {
        return this.mContext;
    }

    public ActionBar getActionBar() {
        return this.mActionBar;
    }

    public final void setOwnerActivity(Activity activity) {
        this.mOwnerActivity = activity;
        getWindow().setVolumeControlStream(this.mOwnerActivity.getVolumeControlStream());
    }

    public final Activity getOwnerActivity() {
        return this.mOwnerActivity;
    }

    public boolean isShowing() {
        return this.mShowing;
    }

    public void create() {
        if (!this.mCreated) {
            dispatchOnCreate(null);
        }
    }

    public void show() {
        boolean z = false;
        if (this.mShowing) {
            if (this.mDecor != null) {
                if (this.mWindow.hasFeature(8)) {
                    this.mWindow.invalidatePanelMenu(8);
                }
                this.mDecor.setVisibility(0);
                return;
            }
            return;
        }
        this.mCanceled = false;
        if (!this.mCreated) {
            dispatchOnCreate(null);
        } else {
            this.mWindow.getDecorView().dispatchConfigurationChanged(this.mContext.getResources().getConfiguration());
        }
        onStart();
        this.mDecor = this.mWindow.getDecorView();
        if (this.mActionBar == null && this.mWindow.hasFeature(8)) {
            ApplicationInfo applicationInfo = this.mContext.getApplicationInfo();
            this.mWindow.setDefaultIcon(applicationInfo.icon);
            this.mWindow.setDefaultLogo(applicationInfo.logo);
            this.mActionBar = new WindowDecorActionBar(this);
        }
        WindowManager.LayoutParams attributes = this.mWindow.getAttributes();
        if ((attributes.softInputMode & 256) == 0) {
            attributes.softInputMode |= 256;
            z = true;
        }
        this.mWindowManager.addView(this.mDecor, attributes);
        if (z) {
            attributes.softInputMode &= -257;
        }
        this.mShowing = true;
        sendShowMessage();
    }

    public void hide() {
        if (this.mDecor != null) {
            this.mDecor.setVisibility(8);
        }
    }

    @Override
    public void dismiss() {
        if (Looper.myLooper() == this.mHandler.getLooper()) {
            dismissDialog();
        } else {
            this.mHandler.post(this.mDismissAction);
        }
    }

    void dismissDialog() {
        if (this.mDecor == null || !this.mShowing) {
            return;
        }
        if (this.mWindow.isDestroyed()) {
            Log.e(TAG, "Tried to dismissDialog() but the Dialog's window was already destroyed!");
            return;
        }
        try {
            this.mWindowManager.removeViewImmediate(this.mDecor);
        } finally {
            if (this.mActionMode != null) {
                this.mActionMode.finish();
            }
            this.mDecor = null;
            this.mWindow.closeAllPanels();
            onStop();
            this.mShowing = false;
            sendDismissMessage();
        }
    }

    private void sendDismissMessage() {
        if (this.mDismissMessage != null) {
            Message.obtain(this.mDismissMessage).sendToTarget();
        }
    }

    private void sendShowMessage() {
        if (this.mShowMessage != null) {
            Message.obtain(this.mShowMessage).sendToTarget();
        }
    }

    void dispatchOnCreate(Bundle bundle) {
        if (!this.mCreated) {
            onCreate(bundle);
            this.mCreated = true;
        }
    }

    protected void onCreate(Bundle bundle) {
    }

    protected void onStart() {
        if (this.mActionBar != null) {
            this.mActionBar.setShowHideAnimationEnabled(true);
        }
    }

    protected void onStop() {
        if (this.mActionBar != null) {
            this.mActionBar.setShowHideAnimationEnabled(false);
        }
    }

    public Bundle onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(DIALOG_SHOWING_TAG, this.mShowing);
        if (this.mCreated) {
            bundle.putBundle(DIALOG_HIERARCHY_TAG, this.mWindow.saveHierarchyState());
        }
        return bundle;
    }

    public void onRestoreInstanceState(Bundle bundle) {
        Bundle bundle2 = bundle.getBundle(DIALOG_HIERARCHY_TAG);
        if (bundle2 == null) {
            return;
        }
        dispatchOnCreate(bundle);
        this.mWindow.restoreHierarchyState(bundle2);
        if (bundle.getBoolean(DIALOG_SHOWING_TAG)) {
            show();
        }
    }

    public Window getWindow() {
        return this.mWindow;
    }

    public View getCurrentFocus() {
        if (this.mWindow != null) {
            return this.mWindow.getCurrentFocus();
        }
        return null;
    }

    public <T extends View> T findViewById(int i) {
        return (T) this.mWindow.findViewById(i);
    }

    public final <T extends View> T requireViewById(int i) {
        T t = (T) findViewById(i);
        if (t == null) {
            throw new IllegalArgumentException("ID does not reference a View inside this Dialog");
        }
        return t;
    }

    public void setContentView(int i) {
        this.mWindow.setContentView(i);
    }

    public void setContentView(View view) {
        this.mWindow.setContentView(view);
    }

    public void setContentView(View view, ViewGroup.LayoutParams layoutParams) {
        this.mWindow.setContentView(view, layoutParams);
    }

    public void addContentView(View view, ViewGroup.LayoutParams layoutParams) {
        this.mWindow.addContentView(view, layoutParams);
    }

    public void setTitle(CharSequence charSequence) {
        this.mWindow.setTitle(charSequence);
        this.mWindow.getAttributes().setTitle(charSequence);
    }

    public void setTitle(int i) {
        setTitle(this.mContext.getText(i));
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 4 || i == 111) {
            keyEvent.startTracking();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyLongPress(int i, KeyEvent keyEvent) {
        return false;
    }

    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if ((i == 4 || i == 111) && keyEvent.isTracking() && !keyEvent.isCanceled()) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyMultiple(int i, int i2, KeyEvent keyEvent) {
        return false;
    }

    public void onBackPressed() {
        if (this.mCancelable) {
            cancel();
        }
    }

    public boolean onKeyShortcut(int i, KeyEvent keyEvent) {
        return false;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mCancelable && this.mShowing && this.mWindow.shouldCloseOnTouch(this.mContext, motionEvent)) {
            cancel();
            return true;
        }
        return false;
    }

    public boolean onTrackballEvent(MotionEvent motionEvent) {
        return false;
    }

    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onWindowAttributesChanged(WindowManager.LayoutParams layoutParams) {
        if (this.mDecor != null) {
            this.mWindowManager.updateViewLayout(this.mDecor, layoutParams);
        }
    }

    @Override
    public void onContentChanged() {
    }

    @Override
    public void onWindowFocusChanged(boolean z) {
    }

    @Override
    public void onAttachedToWindow() {
    }

    @Override
    public void onDetachedFromWindow() {
    }

    @Override
    public void onWindowDismissed(boolean z, boolean z2) {
        dismiss();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if ((this.mOnKeyListener == null || !this.mOnKeyListener.onKey(this, keyEvent.getKeyCode(), keyEvent)) && !this.mWindow.superDispatchKeyEvent(keyEvent)) {
            return keyEvent.dispatch(this, this.mDecor != null ? this.mDecor.getKeyDispatcherState() : null, this);
        }
        return true;
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent keyEvent) {
        if (this.mWindow.superDispatchKeyShortcutEvent(keyEvent)) {
            return true;
        }
        return onKeyShortcut(keyEvent.getKeyCode(), keyEvent);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (this.mWindow.superDispatchTouchEvent(motionEvent)) {
            return true;
        }
        return onTouchEvent(motionEvent);
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent motionEvent) {
        if (this.mWindow.superDispatchTrackballEvent(motionEvent)) {
            return true;
        }
        return onTrackballEvent(motionEvent);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent motionEvent) {
        if (this.mWindow.superDispatchGenericMotionEvent(motionEvent)) {
            return true;
        }
        return onGenericMotionEvent(motionEvent);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        accessibilityEvent.setClassName(getClass().getName());
        accessibilityEvent.setPackageName(this.mContext.getPackageName());
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        accessibilityEvent.setFullScreen(attributes.width == -1 && attributes.height == -1);
        return false;
    }

    @Override
    public View onCreatePanelView(int i) {
        return null;
    }

    @Override
    public boolean onCreatePanelMenu(int i, Menu menu) {
        if (i == 0) {
            return onCreateOptionsMenu(menu);
        }
        return false;
    }

    @Override
    public boolean onPreparePanel(int i, View view, Menu menu) {
        if (i != 0 || menu == null) {
            return true;
        }
        return onPrepareOptionsMenu(menu) && menu.hasVisibleItems();
    }

    @Override
    public boolean onMenuOpened(int i, Menu menu) {
        if (i == 8) {
            this.mActionBar.dispatchMenuVisibilityChanged(true);
        }
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int i, MenuItem menuItem) {
        return false;
    }

    @Override
    public void onPanelClosed(int i, Menu menu) {
        if (i == 8) {
            this.mActionBar.dispatchMenuVisibilityChanged(false);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        return false;
    }

    public void onOptionsMenuClosed(Menu menu) {
    }

    public void openOptionsMenu() {
        if (this.mWindow.hasFeature(0)) {
            this.mWindow.openPanel(0, null);
        }
    }

    public void closeOptionsMenu() {
        if (this.mWindow.hasFeature(0)) {
            this.mWindow.closePanel(0);
        }
    }

    public void invalidateOptionsMenu() {
        if (this.mWindow.hasFeature(0)) {
            this.mWindow.invalidatePanelMenu(0);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
    }

    public void registerForContextMenu(View view) {
        view.setOnCreateContextMenuListener(this);
    }

    public void unregisterForContextMenu(View view) {
        view.setOnCreateContextMenuListener(null);
    }

    public void openContextMenu(View view) {
        view.showContextMenu();
    }

    public boolean onContextItemSelected(MenuItem menuItem) {
        return false;
    }

    public void onContextMenuClosed(Menu menu) {
    }

    @Override
    public boolean onSearchRequested(SearchEvent searchEvent) {
        this.mSearchEvent = searchEvent;
        return onSearchRequested();
    }

    @Override
    public boolean onSearchRequested() {
        SearchManager searchManager = (SearchManager) this.mContext.getSystemService("search");
        ComponentName associatedActivity = getAssociatedActivity();
        if (associatedActivity != null && searchManager.getSearchableInfo(associatedActivity) != null) {
            searchManager.startSearch(null, false, associatedActivity, null, false);
            dismiss();
            return true;
        }
        return false;
    }

    public final SearchEvent getSearchEvent() {
        return this.mSearchEvent;
    }

    @Override
    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
        if (this.mActionBar != null && this.mActionModeTypeStarting == 0) {
            return this.mActionBar.startActionMode(callback);
        }
        return null;
    }

    @Override
    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int i) {
        try {
            this.mActionModeTypeStarting = i;
            return onWindowStartingActionMode(callback);
        } finally {
            this.mActionModeTypeStarting = 0;
        }
    }

    @Override
    public void onActionModeStarted(ActionMode actionMode) {
        this.mActionMode = actionMode;
    }

    @Override
    public void onActionModeFinished(ActionMode actionMode) {
        if (actionMode == this.mActionMode) {
            this.mActionMode = null;
        }
    }

    private ComponentName getAssociatedActivity() {
        Activity activity = this.mOwnerActivity;
        Context context = getContext();
        while (activity == null && context != null) {
            if (context instanceof Activity) {
                activity = (Activity) context;
            } else if (!(context instanceof ContextWrapper)) {
                context = null;
            } else {
                context = ((ContextWrapper) context).getBaseContext();
            }
        }
        if (activity == null) {
            return null;
        }
        return activity.getComponentName();
    }

    public void takeKeyEvents(boolean z) {
        this.mWindow.takeKeyEvents(z);
    }

    public final boolean requestWindowFeature(int i) {
        return getWindow().requestFeature(i);
    }

    public final void setFeatureDrawableResource(int i, int i2) {
        getWindow().setFeatureDrawableResource(i, i2);
    }

    public final void setFeatureDrawableUri(int i, Uri uri) {
        getWindow().setFeatureDrawableUri(i, uri);
    }

    public final void setFeatureDrawable(int i, Drawable drawable) {
        getWindow().setFeatureDrawable(i, drawable);
    }

    public final void setFeatureDrawableAlpha(int i, int i2) {
        getWindow().setFeatureDrawableAlpha(i, i2);
    }

    public LayoutInflater getLayoutInflater() {
        return getWindow().getLayoutInflater();
    }

    public void setCancelable(boolean z) {
        this.mCancelable = z;
        updateWindowForCancelable();
    }

    public void setCanceledOnTouchOutside(boolean z) {
        if (z && !this.mCancelable) {
            this.mCancelable = true;
            updateWindowForCancelable();
        }
        this.mWindow.setCloseOnTouchOutside(z);
    }

    @Override
    public void cancel() {
        if (!this.mCanceled && this.mCancelMessage != null) {
            this.mCanceled = true;
            Message.obtain(this.mCancelMessage).sendToTarget();
        }
        dismiss();
    }

    public void setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
        if (this.mCancelAndDismissTaken != null) {
            throw new IllegalStateException("OnCancelListener is already taken by " + this.mCancelAndDismissTaken + " and can not be replaced.");
        }
        if (onCancelListener != null) {
            this.mCancelMessage = this.mListenersHandler.obtainMessage(68, onCancelListener);
        } else {
            this.mCancelMessage = null;
        }
    }

    public void setCancelMessage(Message message) {
        this.mCancelMessage = message;
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        if (this.mCancelAndDismissTaken != null) {
            throw new IllegalStateException("OnDismissListener is already taken by " + this.mCancelAndDismissTaken + " and can not be replaced.");
        }
        if (onDismissListener != null) {
            this.mDismissMessage = this.mListenersHandler.obtainMessage(67, onDismissListener);
        } else {
            this.mDismissMessage = null;
        }
    }

    public void setOnShowListener(DialogInterface.OnShowListener onShowListener) {
        if (onShowListener != null) {
            this.mShowMessage = this.mListenersHandler.obtainMessage(69, onShowListener);
        } else {
            this.mShowMessage = null;
        }
    }

    public void setDismissMessage(Message message) {
        this.mDismissMessage = message;
    }

    public boolean takeCancelAndDismissListeners(String str, DialogInterface.OnCancelListener onCancelListener, DialogInterface.OnDismissListener onDismissListener) {
        if (this.mCancelAndDismissTaken != null) {
            this.mCancelAndDismissTaken = null;
        } else if (this.mCancelMessage != null || this.mDismissMessage != null) {
            return false;
        }
        setOnCancelListener(onCancelListener);
        setOnDismissListener(onDismissListener);
        this.mCancelAndDismissTaken = str;
        return true;
    }

    public final void setVolumeControlStream(int i) {
        getWindow().setVolumeControlStream(i);
    }

    public final int getVolumeControlStream() {
        return getWindow().getVolumeControlStream();
    }

    public void setOnKeyListener(DialogInterface.OnKeyListener onKeyListener) {
        this.mOnKeyListener = onKeyListener;
    }

    private static final class ListenersHandler extends Handler {
        private final WeakReference<DialogInterface> mDialog;

        public ListenersHandler(Dialog dialog) {
            this.mDialog = new WeakReference<>(dialog);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 67:
                    ((DialogInterface.OnDismissListener) message.obj).onDismiss(this.mDialog.get());
                    break;
                case 68:
                    ((DialogInterface.OnCancelListener) message.obj).onCancel(this.mDialog.get());
                    break;
                case 69:
                    ((DialogInterface.OnShowListener) message.obj).onShow(this.mDialog.get());
                    break;
            }
        }
    }

    private void updateWindowForCancelable() {
        this.mWindow.setCloseOnSwipeEnabled(this.mCancelable);
    }
}
