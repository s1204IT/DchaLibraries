package com.mediatek.keyguard.VoiceWakeup;

import android.content.Context;
import android.util.Log;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.keyguard.ViewMediatorCallback;

public class VoiceWakeupManagerProxy {
    private static VoiceWakeupManagerProxy sInstance = null;
    private static Class<?> mVoiceWakeupClsObj = null;
    private static Object mVoiceWakeupInstance = null;

    public VoiceWakeupManagerProxy() {
        Log.d("VoiceWakeupManagerProxy", "constructor is called.");
    }

    public static VoiceWakeupManagerProxy getInstance() {
        if (sInstance == null) {
            Log.d("VoiceWakeupManagerProxy", "getInstance(...) create one.");
            sInstance = new VoiceWakeupManagerProxy();
            createVoiceWakeupManagerInstance();
        }
        return sInstance;
    }

    private static void createVoiceWakeupManagerInstance() {
        try {
            mVoiceWakeupClsObj = Class.forName("com.mediatek.keyguard.VoiceWakeup.VoiceWakeupManager");
            if (mVoiceWakeupClsObj != null) {
                mVoiceWakeupInstance = mVoiceWakeupClsObj.getDeclaredMethod("getInstance", new Class[0]).invoke(mVoiceWakeupClsObj, new Object[0]);
            }
        } catch (Exception e) {
            Log.e("VoiceWakeupManagerProxy", "createVoiceWakeupManagerInstance error: " + e);
        }
    }

    public boolean isDismissAndLaunchApp() {
        try {
            if (mVoiceWakeupInstance != null) {
                return ((Boolean) mVoiceWakeupClsObj.getDeclaredMethod("isDismissAndLaunchApp", new Class[0]).invoke(mVoiceWakeupInstance, new Object[0])).booleanValue();
            }
            return false;
        } catch (Exception e) {
            Log.e("VoiceWakeupManagerProxy", "reflect isDismissAndLaunchApp error");
            Log.e("VoiceWakeupManagerProxy", Log.getStackTraceString(e));
            return false;
        }
    }

    public boolean onDismiss() {
        try {
            if (mVoiceWakeupInstance != null) {
                return ((Boolean) mVoiceWakeupClsObj.getDeclaredMethod("onDismiss", new Class[0]).invoke(mVoiceWakeupInstance, new Object[0])).booleanValue();
            }
            return false;
        } catch (Exception e) {
            Log.e("VoiceWakeupManagerProxy", "reflect onDismiss error");
            Log.e("VoiceWakeupManagerProxy", Log.getStackTraceString(e));
            return false;
        }
    }

    public void notifySecurityModeChange(KeyguardSecurityModel.SecurityMode securityMode, KeyguardSecurityModel.SecurityMode securityMode2) {
        try {
            if (mVoiceWakeupInstance != null) {
                mVoiceWakeupClsObj.getDeclaredMethod("notifySecurityModeChange", KeyguardSecurityModel.SecurityMode.class, KeyguardSecurityModel.SecurityMode.class).invoke(mVoiceWakeupInstance, securityMode, securityMode2);
            }
        } catch (Exception e) {
            Log.e("VoiceWakeupManagerProxy", "reflect notifySecurityModeChange error");
            Log.e("VoiceWakeupManagerProxy", Log.getStackTraceString(e));
        }
    }

    public void notifyKeyguardIsGone() {
        try {
            if (mVoiceWakeupInstance != null) {
                mVoiceWakeupClsObj.getDeclaredMethod("notifyKeyguardIsGone", new Class[0]).invoke(mVoiceWakeupInstance, new Object[0]);
            }
        } catch (Exception e) {
            Log.e("VoiceWakeupManagerProxy", "reflect notifyKeyguardIsGone error");
            Log.e("VoiceWakeupManagerProxy", Log.getStackTraceString(e));
        }
    }

    public void init(Context context, ViewMediatorCallback viewMediatorCallback) {
        try {
            if (mVoiceWakeupInstance != null) {
                mVoiceWakeupClsObj.getDeclaredMethod("init", Context.class, ViewMediatorCallback.class).invoke(mVoiceWakeupInstance, context, viewMediatorCallback);
            }
        } catch (Exception e) {
            Log.e("VoiceWakeupManagerProxy", "reflect init error");
            Log.e("VoiceWakeupManagerProxy", Log.getStackTraceString(e));
        }
    }
}
