package com.android.internal.policy.impl;

import android.content.Context;
import android.util.Log;
import android.view.FallbackEventHandler;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManagerPolicy;
import com.android.internal.policy.IPolicy;

public class Policy implements IPolicy {
    private static final String TAG = "PhonePolicy";
    private static final String[] preload_classes = {"com.android.internal.policy.impl.PhoneLayoutInflater", "com.android.internal.policy.impl.PhoneWindow", "com.android.internal.policy.impl.PhoneWindow$1", "com.android.internal.policy.impl.PhoneWindow$DialogMenuCallback", "com.android.internal.policy.impl.PhoneWindow$DecorView", "com.android.internal.policy.impl.PhoneWindow$PanelFeatureState", "com.android.internal.policy.impl.PhoneWindow$PanelFeatureState$SavedState"};

    static {
        String[] arr$ = preload_classes;
        for (String s : arr$) {
            try {
                Class.forName(s);
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Could not preload class for phone policy: " + s);
            }
        }
    }

    public Window makeNewWindow(Context context) {
        return new PhoneWindow(context);
    }

    public LayoutInflater makeNewLayoutInflater(Context context) {
        return new PhoneLayoutInflater(context);
    }

    public WindowManagerPolicy makeNewWindowManager() {
        return new PhoneWindowManager();
    }

    public FallbackEventHandler makeNewFallbackEventHandler(Context context) {
        return new PhoneFallbackEventHandler(context);
    }
}
