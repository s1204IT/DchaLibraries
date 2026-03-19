package com.android.internal.policy;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.view.ContextThemeWrapper;
import android.view.WindowManager;
import android.view.WindowManagerImpl;
import java.lang.ref.WeakReference;

class DecorContext extends ContextThemeWrapper {
    private WeakReference<Context> mActivityContext;
    private Resources mActivityResources;
    private PhoneWindow mPhoneWindow;
    private WindowManager mWindowManager;

    public DecorContext(Context context, Context context2) {
        super(context, (Resources.Theme) null);
        this.mActivityContext = new WeakReference<>(context2);
        this.mActivityResources = context2.getResources();
    }

    void setPhoneWindow(PhoneWindow phoneWindow) {
        this.mPhoneWindow = phoneWindow;
        this.mWindowManager = null;
    }

    @Override
    public Object getSystemService(String str) {
        if (Context.WINDOW_SERVICE.equals(str)) {
            if (this.mWindowManager == null) {
                this.mWindowManager = ((WindowManagerImpl) super.getSystemService(Context.WINDOW_SERVICE)).createLocalWindowManager(this.mPhoneWindow);
            }
            return this.mWindowManager;
        }
        return super.getSystemService(str);
    }

    @Override
    public Resources getResources() {
        Context context = this.mActivityContext.get();
        if (context != null) {
            this.mActivityResources = context.getResources();
        }
        return this.mActivityResources;
    }

    @Override
    public AssetManager getAssets() {
        return this.mActivityResources.getAssets();
    }
}
