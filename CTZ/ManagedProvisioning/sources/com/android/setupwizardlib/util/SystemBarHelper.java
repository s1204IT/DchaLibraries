package com.android.setupwizardlib.util;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class SystemBarHelper {

    private interface OnDecorViewInstalledListener {
        void onDecorViewInstalled(View view);
    }

    public static void hideSystemBars(Dialog dialog) {
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = dialog.getWindow();
            temporarilyDisableDialogFocus(window);
            addVisibilityFlag(window, 4098);
            addImmersiveFlagsToDecorView(window, 4098);
            window.setNavigationBarColor(0);
            window.setStatusBarColor(0);
        }
    }

    public static void addVisibilityFlag(View view, int i) {
        if (Build.VERSION.SDK_INT >= 11) {
            view.setSystemUiVisibility(i | view.getSystemUiVisibility());
        }
    }

    public static void addVisibilityFlag(Window window, int i) {
        if (Build.VERSION.SDK_INT >= 11) {
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.systemUiVisibility = i | attributes.systemUiVisibility;
            window.setAttributes(attributes);
        }
    }

    @TargetApi(11)
    private static void addImmersiveFlagsToDecorView(Window window, final int i) {
        getDecorView(window, new OnDecorViewInstalledListener() {
            @Override
            public void onDecorViewInstalled(View view) {
                SystemBarHelper.addVisibilityFlag(view, i);
            }
        });
    }

    private static void getDecorView(Window window, OnDecorViewInstalledListener onDecorViewInstalledListener) {
        new DecorViewFinder().getDecorView(window, onDecorViewInstalledListener, 3);
    }

    private static class DecorViewFinder {
        private OnDecorViewInstalledListener mCallback;
        private Runnable mCheckDecorViewRunnable;
        private final Handler mHandler;
        private int mRetries;
        private Window mWindow;

        private DecorViewFinder() {
            this.mHandler = new Handler();
            this.mCheckDecorViewRunnable = new Runnable() {
                @Override
                public void run() {
                    View viewPeekDecorView = DecorViewFinder.this.mWindow.peekDecorView();
                    if (viewPeekDecorView != null) {
                        DecorViewFinder.this.mCallback.onDecorViewInstalled(viewPeekDecorView);
                        return;
                    }
                    DecorViewFinder.access$410(DecorViewFinder.this);
                    if (DecorViewFinder.this.mRetries >= 0) {
                        DecorViewFinder.this.mHandler.post(DecorViewFinder.this.mCheckDecorViewRunnable);
                        return;
                    }
                    Log.w("SystemBarHelper", "Cannot get decor view of window: " + DecorViewFinder.this.mWindow);
                }
            };
        }

        static int access$410(DecorViewFinder decorViewFinder) {
            int i = decorViewFinder.mRetries;
            decorViewFinder.mRetries = i - 1;
            return i;
        }

        public void getDecorView(Window window, OnDecorViewInstalledListener onDecorViewInstalledListener, int i) {
            this.mWindow = window;
            this.mRetries = i;
            this.mCallback = onDecorViewInstalledListener;
            this.mCheckDecorViewRunnable.run();
        }
    }

    private static void temporarilyDisableDialogFocus(final Window window) {
        window.setFlags(8, 8);
        window.setSoftInputMode(256);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                window.clearFlags(8);
            }
        });
    }
}
