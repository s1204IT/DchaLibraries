package com.android.settings.search;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.print.PrintManager;
import android.print.PrintServicesLoader;
import android.printservice.PrintServiceInfo;
import android.provider.UserDictionary;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import com.android.internal.content.PackageMonitor;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;
import com.android.settings.print.PrintSettingsFragment;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.ArrayList;
import java.util.List;

public final class DynamicIndexableContentMonitor extends PackageMonitor implements InputManager.InputDeviceListener, LoaderManager.LoaderCallbacks<List<PrintServiceInfo>> {
    private Context mContext;
    private boolean mHasFeatureIme;
    private boolean mRegistered;
    private final List<String> mAccessibilityServices = new ArrayList();
    private final List<String> mImeServices = new ArrayList();
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DefaultWfcSettingsExt.PAUSE:
                    String packageName = (String) msg.obj;
                    DynamicIndexableContentMonitor.this.handlePackageAvailable(packageName);
                    break;
                case DefaultWfcSettingsExt.CREATE:
                    String packageName2 = (String) msg.obj;
                    DynamicIndexableContentMonitor.this.handlePackageUnavailable(packageName2);
                    break;
            }
        }
    };
    private final ContentObserver mUserDictionaryContentObserver = new UserDictionaryContentObserver(this.mHandler);

    private static Intent getAccessibilityServiceIntent(String packageName) {
        Intent intent = new Intent("android.accessibilityservice.AccessibilityService");
        intent.setPackage(packageName);
        return intent;
    }

    private static Intent getIMEServiceIntent(String packageName) {
        Intent intent = new Intent("android.view.InputMethod");
        intent.setPackage(packageName);
        return intent;
    }

    public void register(Activity activity, int loaderId) {
        this.mContext = activity;
        if (!((UserManager) this.mContext.getSystemService(UserManager.class)).isUserUnlocked()) {
            Log.w("DynamicIndexableContentMonitor", "Skipping content monitoring because user is locked");
            this.mRegistered = false;
            return;
        }
        this.mRegistered = true;
        boolean hasFeaturePrinting = this.mContext.getPackageManager().hasSystemFeature("android.software.print");
        this.mHasFeatureIme = this.mContext.getPackageManager().hasSystemFeature("android.software.input_methods");
        AccessibilityManager accessibilityManager = (AccessibilityManager) this.mContext.getSystemService("accessibility");
        List<AccessibilityServiceInfo> accessibilityServices = accessibilityManager.getInstalledAccessibilityServiceList();
        int accessibilityServiceCount = accessibilityServices.size();
        for (int i = 0; i < accessibilityServiceCount; i++) {
            AccessibilityServiceInfo accessibilityService = accessibilityServices.get(i);
            ResolveInfo resolveInfo = accessibilityService.getResolveInfo();
            if (resolveInfo != null && resolveInfo.serviceInfo != null) {
                this.mAccessibilityServices.add(resolveInfo.serviceInfo.packageName);
            }
        }
        if (hasFeaturePrinting) {
            activity.getLoaderManager().initLoader(loaderId, null, this);
        }
        if (this.mHasFeatureIme) {
            InputMethodManager imeManager = (InputMethodManager) this.mContext.getSystemService("input_method");
            List<InputMethodInfo> inputMethods = imeManager.getInputMethodList();
            int inputMethodCount = inputMethods.size();
            for (int i2 = 0; i2 < inputMethodCount; i2++) {
                InputMethodInfo inputMethod = inputMethods.get(i2);
                ServiceInfo serviceInfo = inputMethod.getServiceInfo();
                if (serviceInfo != null) {
                    this.mImeServices.add(serviceInfo.packageName);
                }
            }
            this.mContext.getContentResolver().registerContentObserver(UserDictionary.Words.CONTENT_URI, true, this.mUserDictionaryContentObserver);
        }
        InputManager inputManager = (InputManager) activity.getSystemService("input");
        inputManager.registerInputDeviceListener(this, this.mHandler);
        register(activity, Looper.getMainLooper(), UserHandle.CURRENT, false);
    }

    public void unregister() {
        if (this.mRegistered) {
            super.unregister();
            InputManager inputManager = (InputManager) this.mContext.getSystemService("input");
            inputManager.unregisterInputDeviceListener(this);
            if (this.mHasFeatureIme) {
                this.mContext.getContentResolver().unregisterContentObserver(this.mUserDictionaryContentObserver);
            }
            this.mAccessibilityServices.clear();
            this.mImeServices.clear();
        }
    }

    public void onPackageAppeared(String packageName, int uid) {
        postMessage(1, packageName);
    }

    public void onPackageDisappeared(String packageName, int uid) {
        postMessage(2, packageName);
    }

    public void onPackageModified(String packageName) {
        super.onPackageModified(packageName);
        try {
            int state = this.mContext.getPackageManager().getApplicationEnabledSetting(packageName);
            if (state == 0 || state == 1) {
                postMessage(1, packageName);
            } else {
                postMessage(2, packageName);
            }
        } catch (IllegalArgumentException e) {
            Log.e("DynamicIndexableContentMonitor", "Package does not exist: " + packageName, e);
        }
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        Index.getInstance(this.mContext).updateFromClassNameResource(InputMethodAndLanguageSettings.class.getName(), false, true);
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        onInputDeviceChanged(deviceId);
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        Index.getInstance(this.mContext).updateFromClassNameResource(InputMethodAndLanguageSettings.class.getName(), true, true);
    }

    private void postMessage(int what, String packageName) {
        Message message = this.mHandler.obtainMessage(what, packageName);
        this.mHandler.sendMessageDelayed(message, 2000L);
    }

    public void handlePackageAvailable(String packageName) {
        if (!this.mAccessibilityServices.contains(packageName)) {
            Intent intent = getAccessibilityServiceIntent(packageName);
            List<?> services = this.mContext.getPackageManager().queryIntentServices(intent, 0);
            if (services != null && !services.isEmpty()) {
                this.mAccessibilityServices.add(packageName);
                Index.getInstance(this.mContext).updateFromClassNameResource(AccessibilitySettings.class.getName(), false, true);
            }
        }
        if (!this.mHasFeatureIme || this.mImeServices.contains(packageName)) {
            return;
        }
        Intent intent2 = getIMEServiceIntent(packageName);
        List<?> services2 = this.mContext.getPackageManager().queryIntentServices(intent2, 0);
        if (services2 == null || services2.isEmpty()) {
            return;
        }
        this.mImeServices.add(packageName);
        Index.getInstance(this.mContext).updateFromClassNameResource(InputMethodAndLanguageSettings.class.getName(), false, true);
    }

    public void handlePackageUnavailable(String packageName) {
        int imeIndex;
        int accessibilityIndex = this.mAccessibilityServices.indexOf(packageName);
        if (accessibilityIndex >= 0) {
            this.mAccessibilityServices.remove(accessibilityIndex);
            Index.getInstance(this.mContext).updateFromClassNameResource(AccessibilitySettings.class.getName(), true, true);
        }
        if (!this.mHasFeatureIme || (imeIndex = this.mImeServices.indexOf(packageName)) < 0) {
            return;
        }
        this.mImeServices.remove(imeIndex);
        Index.getInstance(this.mContext).updateFromClassNameResource(InputMethodAndLanguageSettings.class.getName(), true, true);
    }

    @Override
    public Loader<List<PrintServiceInfo>> onCreateLoader(int id, Bundle args) {
        return new PrintServicesLoader((PrintManager) this.mContext.getSystemService("print"), this.mContext, 3);
    }

    @Override
    public void onLoadFinished(Loader<List<PrintServiceInfo>> loader, List<PrintServiceInfo> services) {
        Index.getInstance(this.mContext).updateFromClassNameResource(PrintSettingsFragment.class.getName(), false, true);
    }

    @Override
    public void onLoaderReset(Loader<List<PrintServiceInfo>> loader) {
    }

    private final class UserDictionaryContentObserver extends ContentObserver {
        public UserDictionaryContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (!UserDictionary.Words.CONTENT_URI.equals(uri)) {
                return;
            }
            Index.getInstance(DynamicIndexableContentMonitor.this.mContext).updateFromClassNameResource(InputMethodAndLanguageSettings.class.getName(), true, true);
        }
    }
}
