package com.android.pacprocessor;

import android.util.Log;

public class PacNative {
    private boolean mIsActive;

    private native boolean createV8ParserNativeLocked();

    private native boolean destroyV8ParserNativeLocked();

    private native String makeProxyRequestNativeLocked(String str, String str2);

    private native boolean setProxyScriptNativeLocked(String str);

    static {
        System.loadLibrary("jni_pacprocessor");
    }

    PacNative() {
    }

    public synchronized boolean startPacSupport() {
        if (createV8ParserNativeLocked()) {
            Log.e("PacProxy", "Unable to Create v8 Proxy Parser.");
            return true;
        }
        this.mIsActive = true;
        return false;
    }

    public synchronized boolean stopPacSupport() {
        if (this.mIsActive) {
            if (destroyV8ParserNativeLocked()) {
                Log.e("PacProxy", "Unable to Destroy v8 Proxy Parser.");
                return true;
            }
            this.mIsActive = false;
        }
        return false;
    }

    public synchronized boolean setCurrentProxyScript(String str) {
        if (setProxyScriptNativeLocked(str)) {
            Log.e("PacProxy", "Unable to parse proxy script.");
            return true;
        }
        return false;
    }

    public synchronized String makeProxyRequest(String str, String str2) {
        String strMakeProxyRequestNativeLocked;
        strMakeProxyRequestNativeLocked = makeProxyRequestNativeLocked(str, str2);
        if (strMakeProxyRequestNativeLocked == null || strMakeProxyRequestNativeLocked.length() == 0) {
            Log.e("PacProxy", "v8 Proxy request failed.");
            strMakeProxyRequestNativeLocked = null;
        }
        return strMakeProxyRequestNativeLocked;
    }
}
