package com.android.settings.search;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.print.PrintManager;
import android.printservice.PrintServiceInfo;
import android.provider.UserDictionary;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import com.android.internal.content.PackageMonitor;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;
import com.android.settings.print.PrintSettingsFragment;
import java.util.ArrayList;
import java.util.List;

public final class DynamicIndexableContentMonitor extends PackageMonitor implements InputManager.InputDeviceListener {
    private Context mContext;
    private boolean mHasFeatureIme;
    private boolean mHasFeaturePrinting;
    private final List<String> mAccessibilityServices = new ArrayList();
    private final List<String> mPrintServices = new ArrayList();
    private final List<String> mImeServices = new ArrayList();
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    String packageName = (String) msg.obj;
                    DynamicIndexableContentMonitor.this.handlePackageAvailable(packageName);
                    break;
                case 2:
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

    private static Intent getPrintServiceIntent(String packageName) {
        Intent intent = new Intent("android.printservice.PrintService");
        intent.setPackage(packageName);
        return intent;
    }

    private static Intent getIMEServiceIntent(String packageName) {
        Intent intent = new Intent("android.view.InputMethod");
        intent.setPackage(packageName);
        return intent;
    }

    public void register(Context context) {
        this.mContext = context;
        this.mHasFeaturePrinting = this.mContext.getPackageManager().hasSystemFeature("android.software.print");
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
        if (this.mHasFeaturePrinting) {
            PrintManager printManager = (PrintManager) this.mContext.getSystemService("print");
            List<PrintServiceInfo> printServices = printManager.getInstalledPrintServices();
            int serviceCount = printServices.size();
            for (int i2 = 0; i2 < serviceCount; i2++) {
                PrintServiceInfo printService = printServices.get(i2);
                ResolveInfo resolveInfo2 = printService.getResolveInfo();
                if (resolveInfo2 != null && resolveInfo2.serviceInfo != null) {
                    this.mPrintServices.add(resolveInfo2.serviceInfo.packageName);
                }
            }
        }
        if (this.mHasFeatureIme) {
            InputMethodManager imeManager = (InputMethodManager) this.mContext.getSystemService("input_method");
            List<InputMethodInfo> inputMethods = imeManager.getInputMethodList();
            int inputMethodCount = inputMethods.size();
            for (int i3 = 0; i3 < inputMethodCount; i3++) {
                InputMethodInfo inputMethod = inputMethods.get(i3);
                ServiceInfo serviceInfo = inputMethod.getServiceInfo();
                if (serviceInfo != null) {
                    this.mImeServices.add(serviceInfo.packageName);
                }
            }
            this.mContext.getContentResolver().registerContentObserver(UserDictionary.Words.CONTENT_URI, true, this.mUserDictionaryContentObserver);
        }
        InputManager inputManager = (InputManager) context.getSystemService("input");
        inputManager.registerInputDeviceListener(this, this.mHandler);
        register(context, Looper.getMainLooper(), UserHandle.CURRENT, false);
    }

    public void unregister() {
        super.unregister();
        InputManager inputManager = (InputManager) this.mContext.getSystemService("input");
        inputManager.unregisterInputDeviceListener(this);
        if (this.mHasFeatureIme) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mUserDictionaryContentObserver);
        }
        this.mAccessibilityServices.clear();
        this.mPrintServices.clear();
        this.mImeServices.clear();
    }

    public void onPackageAppeared(String packageName, int uid) {
        postMessage(1, packageName);
    }

    public void onPackageDisappeared(String packageName, int uid) {
        postMessage(2, packageName);
    }

    public void onPackageModified(String packageName) {
        super.onPackageModified(packageName);
        int state = this.mContext.getPackageManager().getApplicationEnabledSetting(packageName);
        if (state == 0 || state == 1) {
            postMessage(1, packageName);
        } else {
            postMessage(2, packageName);
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
            if (!this.mContext.getPackageManager().queryIntentServices(intent, 0).isEmpty()) {
                this.mAccessibilityServices.add(packageName);
                Index.getInstance(this.mContext).updateFromClassNameResource(AccessibilitySettings.class.getName(), false, true);
            }
        }
        if (this.mHasFeaturePrinting && !this.mPrintServices.contains(packageName)) {
            Intent intent2 = getPrintServiceIntent(packageName);
            if (!this.mContext.getPackageManager().queryIntentServices(intent2, 0).isEmpty()) {
                this.mPrintServices.add(packageName);
                Index.getInstance(this.mContext).updateFromClassNameResource(PrintSettingsFragment.class.getName(), false, true);
            }
        }
        if (this.mHasFeatureIme && !this.mImeServices.contains(packageName)) {
            Intent intent3 = getIMEServiceIntent(packageName);
            if (!this.mContext.getPackageManager().queryIntentServices(intent3, 0).isEmpty()) {
                this.mImeServices.add(packageName);
                Index.getInstance(this.mContext).updateFromClassNameResource(InputMethodAndLanguageSettings.class.getName(), false, true);
            }
        }
    }

    public void handlePackageUnavailable(String packageName) {
        int imeIndex;
        int printIndex;
        int accessibilityIndex = this.mAccessibilityServices.indexOf(packageName);
        if (accessibilityIndex >= 0) {
            this.mAccessibilityServices.remove(accessibilityIndex);
            Index.getInstance(this.mContext).updateFromClassNameResource(AccessibilitySettings.class.getName(), true, true);
        }
        if (this.mHasFeaturePrinting && (printIndex = this.mPrintServices.indexOf(packageName)) >= 0) {
            this.mPrintServices.remove(printIndex);
            Index.getInstance(this.mContext).updateFromClassNameResource(PrintSettingsFragment.class.getName(), true, true);
        }
        if (this.mHasFeatureIme && (imeIndex = this.mImeServices.indexOf(packageName)) >= 0) {
            this.mImeServices.remove(imeIndex);
            Index.getInstance(this.mContext).updateFromClassNameResource(InputMethodAndLanguageSettings.class.getName(), true, true);
        }
    }

    private final class UserDictionaryContentObserver extends ContentObserver {
        public UserDictionaryContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (UserDictionary.Words.CONTENT_URI.equals(uri)) {
                Index.getInstance(DynamicIndexableContentMonitor.this.mContext).updateFromClassNameResource(InputMethodAndLanguageSettings.class.getName(), true, true);
            }
        }
    }
}
