package com.android.internal.policy;

import android.content.Context;
import android.view.FallbackEventHandler;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManagerPolicy;

public final class PolicyManager {
    private static final String POLICY_IMPL_CLASS_NAME = "com.android.internal.policy.impl.Policy";
    private static final IPolicy sPolicy;

    static {
        try {
            sPolicy = (IPolicy) Class.forName(POLICY_IMPL_CLASS_NAME).newInstance();
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("com.android.internal.policy.impl.Policy could not be loaded", ex);
        } catch (IllegalAccessException ex2) {
            throw new RuntimeException("com.android.internal.policy.impl.Policy could not be instantiated", ex2);
        } catch (InstantiationException ex3) {
            throw new RuntimeException("com.android.internal.policy.impl.Policy could not be instantiated", ex3);
        }
    }

    private PolicyManager() {
    }

    public static Window makeNewWindow(Context context) {
        return sPolicy.makeNewWindow(context);
    }

    public static LayoutInflater makeNewLayoutInflater(Context context) {
        return sPolicy.makeNewLayoutInflater(context);
    }

    public static WindowManagerPolicy makeNewWindowManager() {
        return sPolicy.makeNewWindowManager();
    }

    public static FallbackEventHandler makeNewFallbackEventHandler(Context context) {
        return sPolicy.makeNewFallbackEventHandler(context);
    }
}
