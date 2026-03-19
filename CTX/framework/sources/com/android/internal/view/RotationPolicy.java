package com.android.internal.view;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.R;

public final class RotationPolicy {
    private static final int CURRENT_ROTATION = -1;
    public static final int NATURAL_ROTATION = 0;
    private static final String TAG = "RotationPolicy";

    public static abstract class RotationPolicyListener {
        final ContentObserver mObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean z, Uri uri) {
                RotationPolicyListener.this.onChange();
            }
        };

        public abstract void onChange();
    }

    private RotationPolicy() {
    }

    public static boolean isRotationSupported(Context context) {
        PackageManager packageManager = context.getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER) && packageManager.hasSystemFeature(PackageManager.FEATURE_SCREEN_PORTRAIT) && packageManager.hasSystemFeature(PackageManager.FEATURE_SCREEN_LANDSCAPE) && context.getResources().getBoolean(R.bool.config_supportAutoRotation);
    }

    public static int getRotationLockOrientation(Context context) {
        if (!areAllRotationsAllowed(context)) {
            Point point = new Point();
            try {
                WindowManagerGlobal.getWindowManagerService().getInitialDisplaySize(0, point);
                return point.x < point.y ? 1 : 2;
            } catch (RemoteException e) {
                Log.w(TAG, "Unable to get the display size");
            }
        }
        return 0;
    }

    public static boolean isRotationLockToggleVisible(Context context) {
        return isRotationSupported(context) && Settings.System.getIntForUser(context.getContentResolver(), Settings.System.HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY, 0, -2) == 0;
    }

    public static boolean isRotationLocked(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0, -2) == 0;
    }

    public static void setRotationLock(Context context, boolean z) {
        setRotationLockAtAngle(context, z, areAllRotationsAllowed(context) ? -1 : 0);
    }

    public static void setRotationLockAtAngle(Context context, boolean z, int i) {
        Settings.System.putIntForUser(context.getContentResolver(), Settings.System.HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY, 0, -2);
        setRotationLock(z, i);
    }

    public static void setRotationLockForAccessibility(Context context, boolean z) {
        Settings.System.putIntForUser(context.getContentResolver(), Settings.System.HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY, z ? 1 : 0, -2);
        setRotationLock(z, 0);
    }

    private static boolean areAllRotationsAllowed(Context context) {
        return context.getResources().getBoolean(R.bool.config_allowAllRotations);
    }

    private static void setRotationLock(final boolean z, final int i) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    IWindowManager windowManagerService = WindowManagerGlobal.getWindowManagerService();
                    if (z) {
                        windowManagerService.freezeRotation(i);
                    } else {
                        windowManagerService.thawRotation();
                    }
                } catch (RemoteException e) {
                    Log.w(RotationPolicy.TAG, "Unable to save auto-rotate setting");
                }
            }
        });
    }

    public static void registerRotationPolicyListener(Context context, RotationPolicyListener rotationPolicyListener) {
        registerRotationPolicyListener(context, rotationPolicyListener, UserHandle.getCallingUserId());
    }

    public static void registerRotationPolicyListener(Context context, RotationPolicyListener rotationPolicyListener, int i) {
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), false, rotationPolicyListener.mObserver, i);
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY), false, rotationPolicyListener.mObserver, i);
    }

    public static void unregisterRotationPolicyListener(Context context, RotationPolicyListener rotationPolicyListener) {
        context.getContentResolver().unregisterContentObserver(rotationPolicyListener.mObserver);
    }
}
